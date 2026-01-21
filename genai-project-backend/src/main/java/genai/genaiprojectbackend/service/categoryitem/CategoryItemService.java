package genai.genaiprojectbackend.service.categoryitem;

import genai.genaiprojectbackend.api.categoryitem.dtos.*;
import genai.genaiprojectbackend.api.exceptions.BadRequestException;
import genai.genaiprojectbackend.api.exceptions.NotFoundException;
import genai.genaiprojectbackend.mapper.CategoryItemMapper;
import genai.genaiprojectbackend.model.dtos.StartTextExtractionJobDto;
import genai.genaiprojectbackend.model.dtos.WorkerFile;
import genai.genaiprojectbackend.model.entities.*;
import genai.genaiprojectbackend.model.enums.CategoryItemStatus;
import genai.genaiprojectbackend.model.enums.JobType;
import genai.genaiprojectbackend.repository.*;
import genai.genaiprojectbackend.repository.projection.StatusOnly;
import genai.genaiprojectbackend.service.workers.WorkerStartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class CategoryItemService implements ICategoryItemService {
    private final CategoryItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final WorkerStartService workerStartService;
    private final JobRepository jobRepository;
    private final FileRepository fileRepository;
    private final FinalFlashcardRepository flashcardRepository;
    private final FinalSummaryRepository summaryRepository;
    private final CategoryItemMapper mapper;

    public CategoryItemService(
            CategoryItemRepository itemRepository,
            CategoryRepository categoryRepository,
            WorkerStartService workerStartService,
            JobRepository jobRepository,
            FileRepository fileRepository,
            FinalFlashcardRepository flashcardRepository,
            FinalSummaryRepository summaryRepository,
            CategoryItemMapper mapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.workerStartService = workerStartService;
        this.jobRepository = jobRepository;
        this.fileRepository = fileRepository;
        this.flashcardRepository = flashcardRepository;
        this.summaryRepository = summaryRepository;
        this.mapper = mapper;
    }

    @Override
    public CategoryItemDTO create(CreateCategoryItemDTO dto) {
        if (itemRepository.existsByCategoryIdAndName(dto.getCategoryId(), dto.getName())) {
            throw new BadRequestException(
                    "Category item with name '" + dto.getName() +
                            "' already exists in this category");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + dto.getCategoryId()));

        CategoryItem item = new CategoryItem(
                dto.getName(),
                dto.getDescription(),
                category);

        CategoryItem saved = itemRepository.save(item);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryItemDetailsDTO getById(Integer id) {
        CategoryItem item = itemRepository.findByIdWithCategoryAndFiles(id)
                .orElseThrow(() -> new NotFoundException(
                        "Category item not found with id: " + id));

        List<FinalFlashcard> flashcards = flashcardRepository.findAllByCategoryItemId(id);

        List<FlashcardDTO> flashcardDTOs = flashcards.stream().map(flashcard -> new FlashcardDTO(
                        flashcard.getId(),
                        flashcard.getQuestion(),
                        flashcard.getAnswer()
                ))
                .collect(Collectors.toList());

        String summary = summaryRepository.findSummaryTextByCategoryItemId(id)
                .orElse("");

        return mapper.toDetailsDTO(item, flashcardDTOs, summary);
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
    @Transactional
    public void startGeneration(Integer categoryItemId) {
        // Change CategoryItem Status
        CategoryItem item = itemRepository.findById(categoryItemId)
                .orElseThrow(() -> new NotFoundException(
                        "Category item not found with id: " + categoryItemId));

        item.setStatus(CategoryItemStatus.PROCESSING);
        itemRepository.save(item);

        // Get FileIds and Urls for them
        List<WorkerFile> files = fileRepository.findByCategoryItemId(categoryItemId);

        if (files.isEmpty()) {
            throw new BadRequestException("No files found for this category item");
        }

        // Create new job
        Job job = new Job(
                JobType.TEXT_EXTRACTION,
                categoryItemId);
        Job savedJob = jobRepository.save(job);

        // Start Text Extraction Job
        StartTextExtractionJobDto jobDto = new StartTextExtractionJobDto(
                savedJob.getId(),
                categoryItemId,
                files);
        workerStartService.startTextExtractionJob(jobDto);
    }

    @Override
    public StatusInfo getStatusById(Integer id) {
        return itemRepository.findProjectedById(id)
                .map(projection -> new StatusInfo(
                        projection.getStatus(),
                        projection.getFailedJobType()
                ))
                .orElseThrow(() -> new NotFoundException("Category item not found with id: " + id));
    }

    @Override
    public Generation getGenerationById(Integer id) {
        boolean exists = itemRepository.existsById(id) ;
        if(!exists) {
            throw new NotFoundException("Category item not found with id: " + id);
        }
        List<FinalFlashcard> flashcards = flashcardRepository.findAllByCategoryItemId(id);

        List<FlashcardDTO> flashcardDTOs = flashcards.stream().map(flashcard -> new FlashcardDTO(
                        flashcard.getId(),
                        flashcard.getQuestion(),
                        flashcard.getAnswer()
                ))
                .collect(Collectors.toList());

        String summary = summaryRepository.findSummaryTextByCategoryItemId(id)
                .orElse("");

        return new Generation(
                summary,
                flashcardDTOs
        );
    }
}
