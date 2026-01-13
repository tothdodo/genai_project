package genai.genaiprojectbackend.api.category.dtos;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryListItemDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CategoryDTO {
    private final Integer id;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final List<CategoryListItemDTO> categoryItems;
}
