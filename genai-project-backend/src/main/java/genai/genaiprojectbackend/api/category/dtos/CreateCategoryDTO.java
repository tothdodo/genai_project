package genai.genaiprojectbackend.api.category.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateCategoryDTO {

    @NotBlank
    @Size(max = 32)
    private String name;

    private String description;
}
