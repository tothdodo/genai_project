package genai.genaiprojectbackend.api.categoryitem.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class CategoryItemDetailsDTO {
    private final Integer id;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final CategoryHeaderDTO category;
}
