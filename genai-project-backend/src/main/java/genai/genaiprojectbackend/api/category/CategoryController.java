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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO create(@Valid @RequestBody CreateCategoryDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public CategoryDTO getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/all")
    public List<CategoryDTO> getAll() {
        return service.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
