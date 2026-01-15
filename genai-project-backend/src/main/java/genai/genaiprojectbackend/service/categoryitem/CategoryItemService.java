package genai.genaiprojectbackend.service.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryHeaderDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CategoryItemDetailsDTO;
import genai.genaiprojectbackend.api.categoryitem.dtos.CreateCategoryItemDTO;
import genai.genaiprojectbackend.api.exceptions.BadRequestException;
import genai.genaiprojectbackend.api.exceptions.NotFoundException;
import genai.genaiprojectbackend.mapper.CategoryItemMapper;
import genai.genaiprojectbackend.model.dtos.StartTextExtractionJobDto;
import genai.genaiprojectbackend.model.entities.Category;
import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.repository.CategoryItemRepository;
import genai.genaiprojectbackend.repository.CategoryRepository;
import genai.genaiprojectbackend.repository.projection.StatusOnly;
import genai.genaiprojectbackend.service.workers.WorkerStartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CategoryItemService implements ICategoryItemService {
    private final CategoryItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final WorkerStartService workerStartService;
    private final CategoryItemMapper mapper;

    public CategoryItemService(
            CategoryItemRepository itemRepository,
            CategoryRepository categoryRepository,
            WorkerStartService workerStartService,
            CategoryItemMapper mapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.workerStartService = workerStartService;
        this.mapper = mapper;
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
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryItemDetailsDTO getById(Integer id) {
        CategoryItem item = itemRepository.findByIdWithCategoryAndFiles(id)
                .orElseThrow(() -> new NotFoundException(
                        "Category item not found with id: " + id
                ));
        return mapper.toDetailsDTO(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryItemDTO> getAllByCategory(Integer categoryId) {
        return itemRepository.findAllByCategoryIdOrderByCreatedAtDesc(categoryId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public void delete(Integer id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Category item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    @Override
    @Transactional // Ensures the status update is persisted correctly
    public void startGeneration(Integer id) {
        CategoryItem item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category item not found with id: " + id));

        item.setStatus("PROCESSING");
        itemRepository.save(item);

        StartTextExtractionJobDto jobDto = new StartTextExtractionJobDto("77", id);
        workerStartService.startTextExtractionJob(jobDto);
    }

    @Override
    public String getStatusById(Integer id) {
        return itemRepository.findProjectedById(id)
                .map(StatusOnly::getStatus)
                .orElseThrow(() -> new NotFoundException("Category item not found with id: " + id));
    }
}
