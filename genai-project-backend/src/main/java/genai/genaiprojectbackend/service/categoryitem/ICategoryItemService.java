package genai.genaiprojectbackend.service.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.*;

import java.util.List;

public interface ICategoryItemService {
    CategoryItemDTO create(CreateCategoryItemDTO dto);

    CategoryItemDetailsDTO getById(Integer id);

    List<CategoryItemDTO> getAllByCategory(Integer categoryId);

    void delete(Integer id);

    void startGeneration(Integer id);

    StatusInfo getStatusById(Integer id);

    Generation getGenerationById(Integer id);
}
