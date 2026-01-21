package genai.genaiprojectbackend.api.categoryitem.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StatusInfo {
    @NotBlank
    private final String status;
    private final String failedJobType;
}
