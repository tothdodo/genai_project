package genai.genaiprojectbackend.api.bucket;

import genai.genaiprojectbackend.api.bucket.dtos.FileInfoDto;
import genai.genaiprojectbackend.service.files.IPresignedUrlService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BucketApiController implements IBucketApi {

    private final HttpServletRequest request;

    @Autowired
    private final IPresignedUrlService presignedUrlService;

    @Autowired
    public BucketApiController(HttpServletRequest request, IPresignedUrlService presignedUrlService) {
        this.request = request;
        this.presignedUrlService = presignedUrlService;
    }


    public ResponseEntity<FileInfoDto> fetchFile(
            @Parameter(in = ParameterIn.DEFAULT, description = "Fetch a presigned url for a specific file name from the bucket.", required = true, schema = @Schema())
            @Valid
            @RequestBody FileInfoDto body
    ) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            Optional<FileInfoDto> file = presignedUrlService.fetchFileInfo(body.getFileName(), body.getOriginalFileName());
            return file.map(fileInfo -> new ResponseEntity<>(fileInfo, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }

        return new ResponseEntity<FileInfoDto>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<FileInfoDto> fetchURLForUpload(
            @Parameter(in = ParameterIn.DEFAULT, description = "Fetch a presigned url for a specific file name from the bucket.", required = true, schema = @Schema())
            @Valid
            @RequestBody FileInfoDto body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            Optional<FileInfoDto> file = presignedUrlService.fetchUploadUrl(body.getFileName(), body.getOriginalFileName(), body.getCategoryItemId());
            return file.map(fileInfo -> new ResponseEntity<>(fileInfo, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<FileInfoDto>(new FileInfoDto(), HttpStatus.CONFLICT));
        }

        return new ResponseEntity<FileInfoDto>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<FileInfoDto> uploadFinished(
            @Parameter(in = ParameterIn.DEFAULT, description = "Signalizes that the upload of a file has finished, changes relevant metadata for that file.", required = true, schema = @Schema())
            @RequestBody FileInfoDto body
    ) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            Optional<FileInfoDto> file = presignedUrlService.uploadFinished(body);
            return file.map(fileInfo -> new ResponseEntity<>(fileInfo, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(new FileInfoDto(), HttpStatus.NOT_FOUND));
        }

        return new ResponseEntity<FileInfoDto>(HttpStatus.NOT_IMPLEMENTED);
    }


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler
    public void handleException(IllegalArgumentException ex) {
    }
}