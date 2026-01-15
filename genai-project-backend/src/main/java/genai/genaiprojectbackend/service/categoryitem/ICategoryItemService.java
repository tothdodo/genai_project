package genai.genaiprojectbackend.service.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDetailsDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CreateCategoryItemDTO;

import java.util.List;

public interface ICategoryItemService {
    CategoryItemDTO create(CreateCategoryItemDTO dto);

    CategoryItemDetailsDTO getById(Integer id);

    List<CategoryItemDTO> getAllByCategory(Integer categoryId);

    void delete(Integer id);

    void startGeneration(Integer id);

    String getStatusById(Integer id);
}
