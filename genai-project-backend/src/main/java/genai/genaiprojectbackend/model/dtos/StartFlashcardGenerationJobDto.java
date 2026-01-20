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
public class StartFlashcardGenerationJobDto {
    @JsonProperty("job_id")
    private Integer jobId;

    @JsonProperty("summary_chunk_id")
    private Integer summaryChunkId;

    @JsonProperty("text")
    private String text;

    @JsonProperty("category_item_id")
    private Integer categoryItemId;
}