package genai.genaiprojectbackend.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StartTextExtractionJobDto {
    private final Integer jobId;
    private final Integer categoryItemId;
    private final WorkerFile file; // Changed from List<WorkerFile> to single WorkerFile
}