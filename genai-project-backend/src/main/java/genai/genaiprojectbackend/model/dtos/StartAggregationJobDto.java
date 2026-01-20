package genai.genaiprojectbackend.model.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StartAggregationJobDto {
    @JsonProperty("job_id")
    private Integer jobId;

    @JsonProperty("category_item_id")
    private Integer categoryItemId;

    @JsonProperty("file_id")
    private Long fileId;

    @JsonProperty("summaries")
    private List<String> summaries;

    @JsonProperty("flashcards")
    private List<Map<String, String>> flashcards;
}