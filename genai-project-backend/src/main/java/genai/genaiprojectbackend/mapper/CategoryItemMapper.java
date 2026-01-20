package genai.genaiprojectbackend.mapper;

import genai.genaiprojectbackend.api.categoryitem.dtos.*;
import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.model.entities.File;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CategoryItemMapper {

        public CategoryItemDTO toDTO(CategoryItem item) {
                if (item == null)
                        return null;

                return new CategoryItemDTO(
                                item.getId(),
                                item.getName(),
                                item.getDescription(),
                                item.getCreatedAt(),
                                item.getCategory() != null ? item.getCategory().getId() : null);
        }

        public CategoryItemDetailsDTO toDetailsDTO(CategoryItem item,
                        List<FlashcardDTO> flashcards,
                        String summary) {
                if (item == null)
                        return null;

                CategoryHeaderDTO categoryHeader = null;
                if (item.getCategory() != null) {
                        categoryHeader = new CategoryHeaderDTO(
                                        item.getCategory().getId(),
                                        item.getCategory().getName());
                }

                List<String> filenames = item.getFiles() != null
                                ? item.getFiles().stream()
                                                .map(File::getOriginalFilename)
                                                .toList()
                                : List.of();

                return new CategoryItemDetailsDTO(
                                item.getId(),
                                item.getName(),
                                item.getDescription(),
                                item.getCreatedAt(),
                                item.getStatus(),
                                categoryHeader,
                                filenames,
                                summary,
                                flashcards);
        }
}
