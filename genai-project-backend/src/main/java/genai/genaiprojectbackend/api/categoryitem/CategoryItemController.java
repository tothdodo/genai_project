package genai.genaiprojectbackend.api.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDetailsDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CreateCategoryItemDTO;
import genai.genaiprojectbackend.service.categoryitem.ICategoryItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/category-item")
public class CategoryItemController {
    private final ICategoryItemService service;

    public CategoryItemController(ICategoryItemService service) {
        this.service = service;
    }

    // Create category item
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryItemDTO create(@Valid @RequestBody CreateCategoryItemDTO dto) {
        return service.create(dto);
    }

    // Read one item by ID
    @GetMapping("/{id}")
    public CategoryItemDetailsDTO getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    // Delete item
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
