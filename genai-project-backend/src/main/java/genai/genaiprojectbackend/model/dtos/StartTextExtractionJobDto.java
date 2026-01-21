package genai.genaiprojectbackend.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StartTextExtractionJobDto {
    private final Integer jobId;
    private final Integer categoryItemId;
    private final List<WorkerFile> files;
}
