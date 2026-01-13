package genai.genaiprojectbackend.service.category;

import genai.genaiprojectbackend.api.category.dtos.CategoryDTO;
import genai.genaiprojectbackend.api.category.dtos.CreateCategoryDTO;

import java.util.List;

public interface ICategoryService {
    CategoryDTO create(CreateCategoryDTO dto);

    CategoryDTO getById(Integer id);

    List<CategoryDTO> getAll();

    void delete(Integer id);
}
