package genai.genaiprojectbackend.service.files;

import genai.genaiprojectbackend.api.bucket.dtos.FileInfoDto;
import io.minio.BucketExistsArgs;
import io.minio.MinioAsyncClient;
import io.minio.errors.MinioException;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IPresignedUrlService {

    static CompletableFuture<Boolean> bucketExists(MinioAsyncClient client, String bucketName) throws MinioException, GeneralSecurityException, IOException {
        BucketExistsArgs args = BucketExistsArgs.builder().bucket(bucketName).build();
        return client.bucketExists(args);
    }

    /**
     * Fetch file information for the given file name.
     * <p>
     * Contract:
     * - Input: non-null, non-empty fileName.
     * - Output: Optional containing a populated FileInfoDto (fileName, presignedURL, uploaded)
     * or Optional.empty() if the file isn't available.
     * - Error modes: do not throw for not-found; throw IllegalArgumentException for invalid input.
     *
     * @param fileName         the name of the file to look up as sha-256 hash of the file concatenated with original file name
     * @param originalFileName original  name of the file to look up
     * @return Optional with FileInfoDto if present
     */
    Optional<FileInfoDto> fetchFileInfo(String fileName, String originalFileName);

    /**
     * Fetches a presigned URL for uploading a file with the given name.
     * <p>
     * Contract:
     * - Input: non-null, non-empty fileName.
     * - Output: Optional containing a populated FileInfoDto (fileName, presignedURL, uploaded=false)
     * or Optional.empty() if the upload URL could not be generated.
     * - Error modes: do not throw for not-found; throw IllegalArgumentException for invalid input.
     *
     * @param fileName         the name of the file to be uploaded as SHA-256 hash
     * @param originalFileName the original name of the file to be uploaded
     * @param categoryItemId         of the corresponding categoryItemId that we want to connect this file to
     * @return Optional with FileInfoDto if upload URL is generated
     */
    Optional<FileInfoDto> fetchUploadUrl(String fileName, String originalFileName, Integer categoryItemId);

    /**
     * Marks the upload as finished for the given file name.
     * <p>
     * Contract:
     * - Input: non-null, non-empty hash.
     * - Output: Optional containing a populated FileInfoDto (hash, originalFileName, presignedURL, uploaded=true)
     * or Optional.empty() if the file upload could not be confirmed.
     * - Error modes: throw IllegalArgumentException for invalid input e.g. file already uploaded.
     *
     * @param hash the name of the file that has been uploaded
     * @return Optional with FileInfoDto if upload is confirmed
     */
    Optional<FileInfoDto> uploadFinished(@NonNull @NotBlank FileInfoDto hash);

}

