package genai.genaiprojectbackend.api.categoryitem.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateCategoryItemDTO {
    @NotBlank
    @Size(max = 32)
    private String name;

    private String description;

    @NotNull
    private Integer categoryId;
}
