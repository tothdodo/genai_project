package genai.genaiprojectbackend.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StartSummaryGenerationJobDto {
    private String jobId;
    private String presignedUploadUrl;
    //private MinioObjectDto file;
    //private Long fileId;
}
