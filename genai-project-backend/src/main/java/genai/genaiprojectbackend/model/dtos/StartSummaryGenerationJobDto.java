package genai.genaiprojectbackend.model.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StartSummaryGenerationJobDto {

    @JsonProperty("job_id")
    private Integer jobId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("category_id")
    private Integer categoryId;

    @JsonProperty("chunk_number")
    private Integer chunkNumber;
}