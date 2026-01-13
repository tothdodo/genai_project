package genai.genaiprojectbackend.service.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryHeaderDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDetailsDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CreateCategoryItemDTO;
import genai.genaiprojectbackend.api.exceptions.BadRequestException;
import genai.genaiprojectbackend.api.exceptions.NotFoundException;
import genai.genaiprojectbackend.model.entities.Category;
import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.repository.CategoryItemRepository;
import genai.genaiprojectbackend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryItemService implements ICategoryItemService {
    private final CategoryItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public CategoryItemService(
            CategoryItemRepository itemRepository,
            CategoryRepository categoryRepository
    ) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryItemDTO create(CreateCategoryItemDTO dto) {
        if (itemRepository.existsByCategoryIdAndName(dto.getCategoryId(), dto.getName())) {
            throw new BadRequestException(
                    "Category item with name '" + dto.getName() +
                            "' already exists in this category"
            );
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + dto.getCategoryId()
                ));

        CategoryItem item = new CategoryItem(
                dto.getName(),
                dto.getDescription(),
                category
        );

        CategoryItem saved = itemRepository.save(item);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryItemDetailsDTO getById(Integer id) {
        CategoryItem item = itemRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException(
                        "Category item not found with id: " + id
                ));
        return toDTOWithFolder(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryItemDTO> getAllByCategory(Integer categoryId) {
        return itemRepository.findAllByCategoryIdOrderByCreatedAtDesc(categoryId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void delete(Integer id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Category item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    private CategoryItemDTO toDTO(CategoryItem item) {
        return new CategoryItemDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCreatedAt(),
                item.getCategory().getId()
        );
    }

    private CategoryItemDetailsDTO toDTOWithFolder(CategoryItem item) {
        Category category = item.getCategory();

        CategoryHeaderDTO categoryHeader = new CategoryHeaderDTO(
                category.getId(),
                category.getName()
        );

        return new CategoryItemDetailsDTO(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCreatedAt(),
                categoryHeader
        );
    }
}
