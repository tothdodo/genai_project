package genai.genaiprojectbackend.api.category;

import genai.genaiprojectbackend.api.category.dtos.CategoryDTO;
import genai.genaiprojectbackend.api.category.dtos.CreateCategoryDTO;
import genai.genaiprojectbackend.service.category.ICategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category")
public class CategoryController {

    private final ICategoryService service;

    public CategoryController(ICategoryService service) {
        this.service = service;
    }

    // Create one category
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO create(@Valid @RequestBody CreateCategoryDTO dto) {
        return service.create(dto);
    }

    // Read one category by ID
    @GetMapping("/{id}")
    public CategoryDTO getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    // Read all categories
    @GetMapping("/all")
    public List<CategoryDTO> getAll() {
        return service.getAll();
    }

    // Delete one category
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
