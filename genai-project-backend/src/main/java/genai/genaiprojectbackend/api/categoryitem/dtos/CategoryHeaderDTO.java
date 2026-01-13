package genai.genaiprojectbackend.api.categoryitem.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategoryHeaderDTO {
    private final Integer id;
    private final String name;
}
