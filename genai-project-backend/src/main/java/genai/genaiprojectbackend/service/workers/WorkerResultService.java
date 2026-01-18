package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.model.dtos.StartFlashcardGenerationJobDto;
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
import java.util.Optional;

@Service
public class WorkerResultService {
    private final WorkerStartService workerStartService;
    private final TextChunkRepository textChunkRepository;
    private final SummaryChunkRepository summaryChunkRepository;
    private final TemporaryFlashcardRepository temporaryFlashcardRepository; // NEW
    private final FileRepository fileRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final JobRepository jobRepository;

    public WorkerResultService(
            TextChunkRepository textChunkRepository,
            SummaryChunkRepository summaryChunkRepository,
            TemporaryFlashcardRepository temporaryFlashcardRepository, // NEW
            WorkerStartService workerStartService,
            FileRepository fileRepository,
            CategoryItemRepository categoryItemRepository,
            JobRepository jobRepository
    ) {
        this.textChunkRepository = textChunkRepository;
        this.summaryChunkRepository = summaryChunkRepository;
        this.temporaryFlashcardRepository = temporaryFlashcardRepository; // NEW
        this.fileRepository = fileRepository;
        this.workerStartService = workerStartService;
        this.categoryItemRepository = categoryItemRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void processTextExtractionResult(Map<String, Object> payload) {
        try {
            Long fileId = ((Number) payload.get("fileId")).longValue();
            Integer categoryItemId = ((Number) payload.get("categoryItemId")).intValue();

            Object rawValue = payload.get("textChunks");
            List<String> textChunks = new ArrayList<>();

            if (rawValue instanceof List<?>) {
                for (Object obj : (List<?>) rawValue) {
                    if (obj instanceof String) {
                        textChunks.add((String) obj);
                    }
                }
            } else if (payload.containsKey("textContent")) {
                textChunks.add((String) payload.get("textContent"));
            }

            File fileReference = fileRepository.getReferenceById(fileId);
            CategoryItem categoryReference = categoryItemRepository.getReferenceById(categoryItemId);

            for (int i = 0; i < textChunks.size(); i++) {
                String content = textChunks.get(i);

                TextChunk textChunk = new TextChunk(
                        fileReference,
                        categoryReference,
                        i,
                        content,
                        (Integer) payload.get("pageStart"),
                        (Integer) payload.get("pageEnd")
                );
                textChunkRepository.save(textChunk);

                Job summaryJob = new Job(JobType.SUMMARY_GENERATION, categoryItemId);
                summaryJob.setFileId(fileId);
                Job savedJob = jobRepository.save(summaryJob);

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
        Job job = getValidJobFromResult(result).orElse(null);
        if (job == null || !isResultSuccessful(result, job)) return;

        Map<String, Object> payload = getPayload(result);
        String summaryText = (String) payload.get("summary");
        Integer chunkNumber = (Integer) payload.get("chunk_number");

        Optional<TextChunk> textChunkOpt = textChunkRepository.findByFile_IdAndChunkIndex(job.getFileId(), chunkNumber);

        if (textChunkOpt.isPresent()) {
            // 1. Save the Summary
            SummaryChunk summaryChunk = new SummaryChunk(textChunkOpt.get(), summaryText);
            summaryChunk = summaryChunkRepository.save(summaryChunk);

            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);

            // 2. TRIGGER FLASHCARD GENERATION
            // Create a new job for flashcard generation
            Job flashcardJob = new Job(JobType.FLASHCARD_GENERATION, job.getCategoryItemId());
            flashcardJob.setFileId(job.getFileId());
            flashcardJob = jobRepository.save(flashcardJob);

            // Prepare DTO
            StartFlashcardGenerationJobDto startDto = StartFlashcardGenerationJobDto.builder()
                    .jobId(flashcardJob.getId())
                    .summaryChunkId(summaryChunk.getId())
                    .text(summaryText) // We generate flashcards from the summary
                    .build();

            // Send to RabbitMQ
            workerStartService.startFlashcardGenerationJob(startDto);

        } else {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
        }
    }

    @Transactional
    public void processFlashcardGenerationResult(Map<String, Object> result) {
        Job job = getValidJobFromResult(result).orElse(null);
        if (job == null || !isResultSuccessful(result, job)) return;

        Map<String, Object> payload = getPayload(result);

        // 1. Get Summary Chunk
        Integer summaryChunkId = (Integer) payload.get("summary_chunk_id");
        Optional<SummaryChunk> summaryChunkOpt = summaryChunkRepository.findById(summaryChunkId);

        if (summaryChunkOpt.isEmpty()) {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        SummaryChunk summaryChunk = summaryChunkOpt.get();

        // 2. Parse Flashcards List
        Object flashcardsObj = payload.get("flashcards");
        if (flashcardsObj instanceof List<?>) {
            List<?> list = (List<?>) flashcardsObj;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    String question = (String) map.get("question");
                    String answer = (String) map.get("answer");

                    if (question != null && answer != null) {
                        TemporaryFlashcard tf = new TemporaryFlashcard(summaryChunk, question, answer);
                        temporaryFlashcardRepository.save(tf);
                    }
                }
            }
        }

        // 3. Finish Job
        job.setStatus(JobStatus.FINISHED);
        jobRepository.save(job);

        // Note: Aggregation Trigger logic would go here if needed later
    }

    public void processAggregationResult(Map<String, Object> result) {
        // Implementation for aggregation result
    }

    // Helper methods to handle inconsistent map keys if necessary
    private Integer getJobId(Map<String, Object> result) {
        if (result.get("original_job_id") != null) {
            return (Integer) result.get("original_job_id");
        } else if (result.get("job_id") != null) {
            return (Integer) result.get("job_id");
        }
        return null;
    }

    private Map<String, Object> getPayload(Map<String, Object> result) {
        if (result.containsKey("payload") && result.get("payload") instanceof Map) {
            return (Map<String, Object>) result.get("payload");
        }
        return result;
    }

    private Optional<Job> getValidJobFromResult(Map<String, Object> result) {
        return Optional.ofNullable(getJobId(result))
                .flatMap(jobRepository::findById);
    }

    private boolean isResultSuccessful(Map<String, Object> result, Job job) {
        String statusStr = (String) result.get("status");
        if ("success".equalsIgnoreCase(statusStr)) {
            return true;
        }

        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);
        return false;
    }
}