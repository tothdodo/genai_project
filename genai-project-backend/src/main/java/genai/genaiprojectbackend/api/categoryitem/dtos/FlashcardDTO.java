package genai.genaiprojectbackend.api.categoryitem.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FlashcardDTO {
    @NotBlank
    private String question;

    @NotBlank
    private String answer;
}
