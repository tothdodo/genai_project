package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.model.dtos.StartSummaryGenerationJobDto;
import genai.genaiprojectbackend.model.entities.*;
import genai.genaiprojectbackend.model.enums.JobStatus;
import genai.genaiprojectbackend.model.enums.JobType;
import genai.genaiprojectbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.Optional;

@Service
public class WorkerResultService {
    private final WorkerStartService workerStartService;
    private final TextChunkRepository textChunkRepository;
    private final SummaryChunkRepository summaryChunkRepository;
    private final FileRepository fileRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final JobRepository jobRepository;

    public WorkerResultService(
            TextChunkRepository textChunkRepository,
            SummaryChunkRepository summaryChunkRepository,
            WorkerStartService workerStartService,
            FileRepository fileRepository,
            CategoryItemRepository categoryItemRepository,
            JobRepository jobRepository
    ) {
        this.textChunkRepository = textChunkRepository;
        this.summaryChunkRepository = summaryChunkRepository;
        this.fileRepository = fileRepository;
        this.workerStartService = workerStartService;
        this.categoryItemRepository = categoryItemRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void processTextExtractionResult(Map<String, Object> payload) {
        try {
            // 1. Extract common IDs (Using safer casting logic)
            // Handling generic Number helps avoid casting errors if payload sends Integer vs Long
            Long fileId = ((Number) payload.get("fileId")).longValue();
            Integer categoryItemId = ((Number) payload.get("categoryItemId")).intValue();

            // 2. Extract Text Chunks (Preserving HEAD's list handling)
            Object rawValue = payload.get("textChunks");
            List<String> textChunks = new ArrayList<>();

            if (rawValue instanceof List<?>) {
                for (Object obj : (List<?>) rawValue) {
                    if (obj instanceof String) {
                        textChunks.add((String) obj);
                    }
                }
            } else if (payload.containsKey("textContent")) {
                // Fallback: If payload matches the Incoming branch format (single string)
                textChunks.add((String) payload.get("textContent"));
            }

            File fileReference = fileRepository.getReferenceById(fileId);
            CategoryItem categoryReference = categoryItemRepository.getReferenceById(categoryItemId);

            // 3. Iterate through chunks to Save and Start Jobs
            for (int i = 0; i < textChunks.size(); i++) {
                String content = textChunks.get(i);

                // A. Save Text Chunk
                TextChunk textChunk = new TextChunk(
                        fileReference,
                        categoryReference,
                        i, // Using loop index as chunkIndex
                        content,
                        (Integer) payload.get("pageStart"),
                        (Integer) payload.get("pageEnd")
                );
                textChunkRepository.save(textChunk);

                // B. Create and Save Job for Summary Generation (From Incoming)
                Job summaryJob = new Job(JobType.SUMMARY_GENERATION, categoryItemId);
                summaryJob.setFileId(fileId);
                Job savedJob = jobRepository.save(summaryJob);

                // C. Start Summary Generation Worker (From Incoming)
                StartSummaryGenerationJobDto jobDto = StartSummaryGenerationJobDto.builder()
                        .jobId(savedJob.getId())
                        .text(content)
                        .categoryId(categoryItemId)
                        .chunkNumber(i)
                        .build();

                workerStartService.startSummaryGenerationJob(jobDto);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing text extraction result", e);
        }
    }

    @Transactional
    public void processSummaryGenerationResult(Map<String, Object> result) {
        // 1. Identify Job
        Integer jobId = null;
        // Handle potential naming differences in result map
        if (result.get("original_job_id") != null) {
            jobId = (Integer) result.get("original_job_id");
        } else if (result.get("job_id") != null) {
            jobId = (Integer) result.get("job_id");
        }

        if (jobId == null) return;

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        // 2. Check Status
        String statusStr = (String) result.get("status");
        if (!"success".equalsIgnoreCase(statusStr)) {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        // 3. Extract Payload Data
        Map<String, Object> payload = result;
        if (result.containsKey("payload") && result.get("payload") instanceof Map) {
            payload = (Map<String, Object>) result.get("payload");
        }

        String summaryText = (String) payload.get("summary");
        Integer chunkNumber = (Integer) payload.get("chunk_number");

        // 4. Find Corresponding TextChunk
        Optional<TextChunk> textChunkOpt = textChunkRepository.findByFile_IdAndChunkIndex(job.getFileId(), chunkNumber);

        if (textChunkOpt.isPresent()) {
            // 5. Create and Save SummaryChunk
            SummaryChunk summaryChunk = new SummaryChunk(textChunkOpt.get(), summaryText);
            summaryChunkRepository.save(summaryChunk);
            job.setStatus(JobStatus.FINISHED);
        } else {
            job.setStatus(JobStatus.FAILED); // Could not link to text chunk
        }

        jobRepository.save(job);
    }

    public void processFlashcardGenerationResult(Map<String, Object> result) {
    }

    public void processAggregationResult(Map<String, Object> result) {
    }
}