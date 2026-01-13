package genai.genaiprojectbackend.service.category;

import genai.genaiprojectbackend.api.category.dtos.CategoryDTO;
import genai.genaiprojectbackend.api.category.dtos.CreateCategoryDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryListItemDTO;
import genai.genaiprojectbackend.api.exceptions.BadRequestException;
import genai.genaiprojectbackend.api.exceptions.NotFoundException;
import genai.genaiprojectbackend.model.entities.Category;
import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.repository.CategoryRepository;
import genai.genaiprojectbackend.service.categoryitem.ICategoryItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CategoryService implements ICategoryService {

    private final CategoryRepository repository;
    private final ICategoryItemService categoryItemService;

    public CategoryService(CategoryRepository repository, ICategoryItemService categoryItemService) {
        this.repository = repository;
        this.categoryItemService = categoryItemService;
    }

    @Override
    public CategoryDTO create(CreateCategoryDTO dto) {
        Boolean existsWithName = repository.existsByName(dto.getName());

        if (existsWithName) throw new BadRequestException("Category already exists with name: " + dto.getName());

        Category category = new Category(dto.getName(), dto.getDescription());
        Category saved = repository.save(category);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getById(Integer id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        return toDTO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAll() {
        return repository.findAllWithItemsOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Category not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private CategoryDTO toDTO(Category category) {
        List<CategoryListItemDTO> items = category.getItems()
                .stream()
                .sorted(Comparator.comparing(CategoryItem::getCreatedAt).reversed())
                .map(this::toItemDTO)
                .toList();

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                items
        );
    }

    private CategoryListItemDTO toItemDTO(CategoryItem item) {
        return new CategoryListItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCreatedAt()
        );
    }
}
