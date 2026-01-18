package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.model.entities.CategoryItem;
import genai.genaiprojectbackend.model.entities.File;
import genai.genaiprojectbackend.model.entities.TextChunk;
import genai.genaiprojectbackend.repository.CategoryItemRepository;
import genai.genaiprojectbackend.repository.FileRepository;
import genai.genaiprojectbackend.repository.TextChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
            CategoryItemRepository categoryItemRepository) {
        this.textChunkRepository = textChunkRepository;
        this.fileRepository = fileRepository;
        this.workerStartService = workerStartService;
        this.categoryItemRepository = categoryItemRepository;
    }

    public void processTextExtractionResult(Map<String, Object> payload) {
        try {
            // Save chunked text
            Object rawValue = payload.get("textChunks");
            List<String> textChunks = new ArrayList<>();

            if (rawValue instanceof List<?>) {
                for (Object obj : (List<?>) rawValue) {
                    if (obj instanceof String) {
                        textChunks.add((String) obj);
                    }
                }
            }
            Long fileId = Long.valueOf(String.valueOf(payload.get("fileId")));
            Integer categoryItemId = Integer.valueOf(String.valueOf(payload.get("categoryItemId")));
            File file = fileRepository.getReferenceById(fileId);
            CategoryItem categoryItem = categoryItemRepository.getReferenceById(categoryItemId);

            List<TextChunk> newTextChunks = IntStream.range(0, textChunks.size())
                    .mapToObj(i -> new TextChunk(
                            file,
                            categoryItem,
                            i,
                            textChunks.get(i),
                            // Todo: Do we need these?
                            (Integer) payload.get("pageStart"),
                            (Integer) payload.get("pageEnd")))
                    .toList();

            textChunkRepository.saveAll(newTextChunks);
            // Todo: Start Summary Generation Job for each chunk
            // for all chunk in newTextChunks
            // workerStartService.startSummaryGenerationJob();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void processSummaryGenerationResult(Map<String, Object> result) {
    }

    public void processFlashcardGenerationResult(Map<String, Object> result) {
    }

    public void processAggregationResult(Map<String, Object> result) {
    }
}
