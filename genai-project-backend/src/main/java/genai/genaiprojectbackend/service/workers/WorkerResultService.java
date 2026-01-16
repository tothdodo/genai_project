package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.model.entities.TextChunk;
import genai.genaiprojectbackend.repository.CategoryItemRepository;
import genai.genaiprojectbackend.repository.FileRepository;
import genai.genaiprojectbackend.repository.TextChunkRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkerResultService {
    private final WorkerStartService workerStartService;
    private final TextChunkRepository textChunkRepository;
    private final FileRepository fileRepository;
    private final CategoryItemRepository categoryItemRepository;

    public WorkerResultService(
            TextChunkRepository textChunkRepository,
            WorkerStartService workerStartService,
            FileRepository fileRepository,
            CategoryItemRepository categoryItemRepository
    ) {
        this.textChunkRepository = textChunkRepository;
        this.fileRepository = fileRepository;
        this.workerStartService = workerStartService;
        this.categoryItemRepository = categoryItemRepository;
    }

    public void processTextExtractionResult(Map<String, Object> result) {
        // Save chunked text
        TextChunk textChunk = new TextChunk(
                fileRepository.getReferenceById((Long) result.get("fileId")),
                categoryItemRepository.getReferenceById((Integer) result.get("categoryItemId")),
                (Integer) result.get("chunkIndex"),
                (String) result.get("textContent"),
                (Integer) result.get("pageStart"),
                (Integer) result.get("pageEnd")
        );
        textChunkRepository.save(textChunk);

        // Start Summary Generation Job
        //workerStartService.startSummaryGenerationJob();
    }

    public void processSummaryGenerationResult(Map<String, Object> result) {
    }

    public void processFlashcardGenerationResult(Map<String, Object> result) {
    }

    public void processAggregationResult(Map<String, Object> result) {
    }
}
