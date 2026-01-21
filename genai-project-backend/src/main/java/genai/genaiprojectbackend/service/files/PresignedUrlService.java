package genai.genaiprojectbackend.service.files;

import genai.genaiprojectbackend.api.bucket.dtos.FileInfoDto;
import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.repository.CategoryItemRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioAsyncClient;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import genai.genaiprojectbackend.configuration.MinioProperties;
import genai.genaiprojectbackend.mapper.UrlMapper;
import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.model.entities.Url;
import genai.genaiprojectbackend.repository.FileRepository;
import genai.genaiprojectbackend.repository.UrlRepository;

@Slf4j
@Profile("!development")
@Service
@RequiredArgsConstructor
public class PresignedUrlService implements IPresignedUrlService {
    protected final MinioAsyncClient minioClient;
    protected final MinioProperties minioProperties;

    private final UrlRepository urlRepository;
    private final FileRepository fileRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final UrlMapper urlMapper;
    private final int minimumAddedTime = 20;
    @Value("${spring.rabbitmq.template.routing-key:metadata_trigger}")
    private String routingKey;

    @Scheduled(fixedDelayString = "${minio.defaultRefresh:2000}")
    @Transactional
    public void invalidateUrls() {
        urlRepository.getUrlsByExpiresAtBefore(Instant.now()).forEach(url -> {
            log.info("Deleting expired URL for file: {}",
                    url.getFile() != null ? url.getFile().getFilename() : "unknown");
            urlRepository.delete(url);
        });

    }

    @PostConstruct
    public void init() throws MinioException, GeneralSecurityException, IOException {
        if (IPresignedUrlService.bucketExists(minioClient, minioProperties.getBucket()).join()) {
            log.info("Bucket {} exists and is reachable", minioProperties.getBucket());
        } else {
            throw new MinioException("Bucket does not exist: " + minioProperties.getBucket());
        }
    }

    @Override
    public Optional<FileInfoDto> fetchFileInfo(@NonNull @NotBlank String fileName, String originalFileName) {
        List<Url> presignedUrls = urlRepository.findByFile_Filename_AndMethod_AndExpiresAtAfter(fileName, Method.GET,
                Instant.now().plusSeconds(minimumAddedTime));
        if (!presignedUrls.isEmpty()) {
            Url url = presignedUrls.get(0);
            FileInfoDto fileInfoDto = urlMapper.urlToFileInfoDto(url);
            return Optional.of(fileInfoDto);
        }

        GetPresignedObjectUrlArgs presignedObjectUrlArgs = getPresignedObjectUrlArgs(fileName, Method.GET);
        if (presignedObjectUrlArgs == null) {
            return Optional.empty();
        }
        Instant expiresAt = Instant.now().plusSeconds(minioProperties.getDefaultExpiryTime());
        Optional<FileInfoDto> fileInfo = getUrl(presignedObjectUrlArgs, fileName, originalFileName);

        if (fileInfo.isPresent()) {
            FileInfoDto fileInfoDtoActual = fileInfo.get();
            Optional<File> f = fileRepository.findFileByFilenameAndUploaded(fileName, true);
            if (f.isEmpty()) {
                return Optional.empty();
            }
            Url url = new Url();
            url.setFile(f.get());
            url.setPresignedURL(fileInfoDtoActual.getPresignedURL());
            url.setMethod(Method.GET);
            url.setCreatedAt(Instant.now());
            url.setExpiresAt(expiresAt);

            fileInfoDtoActual = urlMapper.urlToFileInfoDto(urlRepository.save(url));

            return Optional.of(fileInfoDtoActual);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileInfoDto> fetchUploadUrl(String fileName, String originalFileName, Integer categoryItemId) {
        GetPresignedObjectUrlArgs presignedObjectUrlArgs = getPresignedObjectUrlArgs(fileName, Method.PUT);
        if (presignedObjectUrlArgs == null) {
            return Optional.empty();
        }
        if (fileRepository.findFileByFilenameAndUploaded(fileName, true).isPresent()) {
            return Optional.empty();
        }

        Instant expiresAt = Instant.now().plusSeconds(minioProperties.getDefaultExpiryTime());

        Optional<File> fileOpt = fileRepository.findFileByFilenameAndUploaded(fileName, false);
        File file;
        if (fileOpt.isPresent()) {
            List<Url> presignedUrls = urlRepository.findByFile_Filename_AndMethod_AndExpiresAtAfter(fileName,
                    Method.PUT, Instant.now());
            Optional<Url> createdUpload = presignedUrls.stream().findAny();
            if (createdUpload.isPresent()) {
                FileInfoDto fileInfo;
                fileInfo = new FileInfoDto(fileOpt.get());
                fileInfo.setPresignedURL(createdUpload.get().getPresignedURL());
                fileInfo.setUploaded(false);
                return Optional.of(fileInfo);
            }

            file = fileOpt.get();
        } else {
            file = new File();
            file.setFilename(fileName);
            file.setUploaded(false);
            file.setOriginalFilename(originalFileName);
            if (categoryItemId != null) {
                CategoryItem categoryItem = categoryItemRepository.findById(categoryItemId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Category not found with id: " + categoryItemId));
                file.setCategoryItem(categoryItem);
            }
            file = fileRepository.save(file);
        }

        Optional<FileInfoDto> fOpt = getUrl(presignedObjectUrlArgs, fileName, originalFileName);
        if (fOpt.isPresent()) {
            Url url = new Url();

            FileInfoDto fileInfo = new FileInfoDto();
            fileInfo.setFileName(fileName);
            fileInfo.setOriginalFileName(originalFileName);
            fileInfo = fOpt.get();
            fileInfo.setUrlExpiresAt(expiresAt);
            url.setFile(file);
            url.setPresignedURL(fOpt.get().getPresignedURL());
            url.setMethod(Method.PUT);
            url.setExpiresAt(expiresAt);
            url.setCreatedAt(Instant.now());
            url.setBucket(minioProperties.getBucket());
            urlRepository.save(url);
            return Optional.of(fileInfo);
        } else {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Optional<FileInfoDto> uploadFinished(@NonNull FileInfoDto body) {
        File file = fileRepository.findFileByFilenameAndUploaded(body.getFileName(), false)
                .orElse(null);
        if (file == null) {
            return Optional.empty();
        }
        GetPresignedObjectUrlArgs presignedObjectUrlArgs = getPresignedObjectUrlArgs(body.getFileName(), Method.GET);
        if (presignedObjectUrlArgs == null) {
            return Optional.empty();
        }
        Instant expiresAt = Instant.now().plusSeconds(minioProperties.getDefaultExpiryTime());
        Optional<FileInfoDto> fileInfoOpt = getUrl(presignedObjectUrlArgs, body.getFileName(),
                body.getOriginalFileName());
        if (fileInfoOpt.isEmpty()) {
            return Optional.empty();
        }

        if (!checkExistence(body)) {
            return Optional.empty();
        }

        file.setUrl(fileInfoOpt.get().getPresignedURL());
        file.setUploaded(true);
        file.setUploadedAt(Instant.now());
        file = fileRepository.save(file);
        FileInfoDto dto = new FileInfoDto(file);
        urlRepository.deleteByFileIdAndMethod(file.getId(), Method.PUT);

        dto.setUploaded(true);
        dto.setPresignedURL(fileInfoOpt.get().getPresignedURL());
        dto.setUrlExpiresAt(expiresAt);

        return Optional.of(dto);
    }

    private boolean checkExistence(FileInfoDto body) {
        StatObjectArgs statObjectArgs = getStatObjectArgs(body.getFileName());
        try {
            minioClient.statObject(statObjectArgs);
            return true;
        } catch (MinioException | GeneralSecurityException | IOException e) {
            log.info("Could not get stat object {} after upload finished call", body.getFileName(), e);
            return false;
        }
    }

    private Optional<FileInfoDto> getUrl(GetPresignedObjectUrlArgs presignedObjectUrlArgs, String fileName,
            String originalFileName) {
        try {
            String url = minioClient.getPresignedObjectUrl(presignedObjectUrlArgs);
            return Optional.of(FileInfoDto.builder().fileName(fileName).presignedURL(url)
                    .originalFileName(originalFileName).build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            log.info("Could not fetch presigned {} URL for file: {}", presignedObjectUrlArgs.method().toString(),
                    fileName, e);
            return Optional.empty();
        }
    }

    private StatObjectArgs getStatObjectArgs(String fileName) {
        return StatObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(fileName)
                .build();
    }

    private GetPresignedObjectUrlArgs getPresignedObjectUrlArgs(String fileName, Method method) {
        return GetPresignedObjectUrlArgs.builder()
                .method(method)
                .bucket(minioProperties.getBucket())
                .object(fileName)
                .expiry(minioProperties.getDefaultExpiryTime())
                .build();
    }

}
