package genai.genaiprojectbackend.api.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDetailsDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CreateCategoryItemDTO;
import genai.genaiprojectbackend.service.categoryitem.ICategoryItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/category-item")
public class CategoryItemController {
    private final ICategoryItemService service;

    public CategoryItemController(ICategoryItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryItemDTO create(@Valid @RequestBody CreateCategoryItemDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public CategoryItemDetailsDTO getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> getStatusById(@PathVariable Integer id) {
        return Collections.singletonMap("status", service.getStatusById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @PostMapping("/{id}/start-generation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startGeneration(@PathVariable Integer id){
        service.startGeneration(id);
    }
}
