package genai.genaiprojectbackend.api.categoryitem.dtos;

import genai.genaiprojectbackend.model.enums.CategoryItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CategoryItemDetailsDTO {
    private final Integer id;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final CategoryItemStatus status;
    private final CategoryHeaderDTO category;
    private List<String> filenames;
}
