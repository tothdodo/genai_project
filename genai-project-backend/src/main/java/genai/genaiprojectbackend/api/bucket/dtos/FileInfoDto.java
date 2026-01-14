package genai.genaiprojectbackend.api.bucket.dtos;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryHeaderDTO;
import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.model.entities.Url;
import genai.genaiprojectbackend.validator.FileNameValid;
import lombok.*;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
public class FileInfoDto {

    @JsonProperty("fileName")
    @NonNull
    @FileNameValid
    private String fileName;

    @JsonProperty("originalFileName")
    private String originalFileName;

    @JsonProperty("presignedURL")
    private String presignedURL;

    @JsonProperty("uploaded")
    private Boolean uploaded;

    @JsonProperty("urlExpiresAt")
    private Instant urlExpiresAt;

    @JsonProperty("categoryItemId")
    private Integer categoryItemId;

    public FileInfoDto() {
    }

    public FileInfoDto(File file) {
        this.fileName = file.getFilename();
        this.presignedURL = null;
        this.uploaded = file.getUploaded();
        this.urlExpiresAt = null;
        this.originalFileName = file.getOriginalFilename();
    }

    public FileInfoDto(String fileName) {
        this.fileName = fileName;
    }

    FileInfoDto(String fileName, String presignedURL) {
        this.fileName = fileName;
        this.presignedURL = presignedURL;
    }

    FileInfoDto(String fileName, String presignedURL, Boolean uploaded) {
        this.fileName = fileName;
        this.presignedURL = presignedURL;
        this.uploaded = uploaded;
    }


    public FileInfoDto(String fileName, String presignedURL, Boolean uploaded, Instant urlExpiresAt) {
        this.fileName = fileName;
        this.presignedURL = presignedURL;
        this.uploaded = uploaded;
        this.urlExpiresAt = urlExpiresAt;
    }

    FileInfoDto(String fileName, String originalFileName, String presignedURL, Boolean uploaded, Instant urlExpiresAt) {
        this.fileName = fileName;
        this.presignedURL = presignedURL;
        this.uploaded = uploaded;
        this.urlExpiresAt = urlExpiresAt;
        this.originalFileName = originalFileName;
    }

    FileInfoDto(String fileName, String originalFileName, String presignedURL, Boolean uploaded, Instant urlExpiresAt, Integer categoryItemId) {
        this.fileName = fileName;
        this.presignedURL = presignedURL;
        this.uploaded = uploaded;
        this.urlExpiresAt = urlExpiresAt;
        this.originalFileName = originalFileName;
        this.categoryItemId = categoryItemId;
    }
}



