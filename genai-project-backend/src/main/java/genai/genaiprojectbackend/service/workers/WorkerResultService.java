package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.model.dtos.StartAggregationJobDto;
import genai.genaiprojectbackend.model.dtos.StartFlashcardGenerationJobDto;
import genai.genaiprojectbackend.model.dtos.StartSummaryGenerationJobDto;
import genai.genaiprojectbackend.model.entities.*;
import genai.genaiprojectbackend.model.enums.CategoryItemStatus;
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
    private final TemporaryFlashcardRepository temporaryFlashcardRepository;
    private final FileRepository fileRepository;
    private final CategoryItemRepository categoryItemRepository;
    private final JobRepository jobRepository;
    private final FinalSummaryRepository finalSummaryRepository;
    private final FinalFlashcardRepository finalFlashcardRepository;

    public WorkerResultService(
            TextChunkRepository textChunkRepository,
            SummaryChunkRepository summaryChunkRepository,
            TemporaryFlashcardRepository temporaryFlashcardRepository,
            WorkerStartService workerStartService,
            FileRepository fileRepository,
            CategoryItemRepository categoryItemRepository,
            JobRepository jobRepository,
            FinalSummaryRepository finalSummaryRepository,
            FinalFlashcardRepository finalFlashcardRepository
    ) {
        this.textChunkRepository = textChunkRepository;
        this.summaryChunkRepository = summaryChunkRepository;
        this.temporaryFlashcardRepository = temporaryFlashcardRepository;
        this.fileRepository = fileRepository;
        this.workerStartService = workerStartService;
        this.categoryItemRepository = categoryItemRepository;
        this.jobRepository = jobRepository;
        this.finalSummaryRepository = finalSummaryRepository;
        this.finalFlashcardRepository = finalFlashcardRepository;
    }

    @Transactional
    public void processTextExtractionResult(Map<String, Object> result) {
        try {
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            Job job = getValidJobFromResult(result).orElse(null);
            if (job == null || !isResultSuccessful(result, job)) return;
            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);

            Long fileId = Long.valueOf(String.valueOf(payload.get("fileId")));
            Integer categoryItemId = Integer.valueOf(String.valueOf(payload.get("categoryItemId")));

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
        if (flashcardsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
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

        checkAndStartAggregation(job.getCategoryItemId());
    }

    /**
     * Checks if ALL jobs for a specific CategoryItem are finished.
     * If so, starts the Aggregation Job for the entire CategoryItem.
     */
    private void checkAndStartAggregation(Integer categoryItemId) {
        // Check if there are any pending or in-progress jobs for this Category Item (across ALL files)
        long pendingJobs = jobRepository.countByCategoryItemIdAndStatusIn(
                categoryItemId,
                List.of(JobStatus.PENDING, JobStatus.IN_PROGRESS)
        );

        if (pendingJobs == 0) {
            // 1. Fetch ALL summaries for this Category Item
            List<SummaryChunk> summaries = summaryChunkRepository.findAllByTextChunk_File_CategoryItem_Id(categoryItemId);
            List<String> summaryTexts = summaries.stream().map(SummaryChunk::getSummaryText).toList();

            // 2. Fetch ALL Temporary Flashcards for this Category Item
            List<TemporaryFlashcard> tempFlashcards = temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(categoryItemId);
            List<Map<String, String>> flashcardMaps = tempFlashcards.stream()
                    .map(f -> Map.of("question", f.getQuestion(), "answer", f.getAnswer()))
                    .toList();

            if (summaryTexts.isEmpty() && flashcardMaps.isEmpty()) {
                // Nothing to aggregate
                return;
            }

            Job aggJob = new Job(JobType.AGGREGATION, categoryItemId);
            aggJob.setFileId(null);
            aggJob = jobRepository.save(aggJob);

            StartAggregationJobDto dto = StartAggregationJobDto.builder()
                    .jobId(aggJob.getId())
                    .categoryItemId(categoryItemId)
                    .fileId(null)
                    .summaries(summaryTexts)
                    .flashcards(flashcardMaps)
                    .build();

            workerStartService.startAggregationJob(dto);
        }
    }

    @Transactional
    public void processAggregationResult(Map<String, Object> result) {
        Job job = getValidJobFromResult(result).orElse(null);
        if (job == null || !isResultSuccessful(result, job)) return;

        Map<String, Object> payload = getPayload(result);
        String finalSummaryText = (String) payload.get("final_summary");

        if (job.getCategoryItemId() == null) {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        CategoryItem categoryItem = categoryItemRepository.findById(job.getCategoryItemId()).orElse(null);

        if (categoryItem != null) {
            // 1. Save Final Summary
            FinalSummary finalSummary = new FinalSummary(finalSummaryText, categoryItem);
            finalSummaryRepository.save(finalSummary);

            // 2. Save Final Flashcards
            Object finalFlashcardsObj = payload.get("final_flashcards");
            if (finalFlashcardsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        String question = (String) map.get("question");
                        String answer = (String) map.get("answer");

                        if (question != null && answer != null) {
                            FinalFlashcard ff = new FinalFlashcard(question, answer, categoryItem);
                            finalFlashcardRepository.save(ff);
                        }
                    }
                }
            }

            // 3. Cleanup Temporary Tables for the WHOLE CategoryItem
            temporaryFlashcardRepository.deleteAll(
                    temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(categoryItem.getId())
            );
            summaryChunkRepository.deleteAll(
                    summaryChunkRepository.findAllByTextChunk_File_CategoryItem_Id(categoryItem.getId())
            );

            // Finishing Job
            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);

            // Mark CategoryItem as COMPLETED
            categoryItem.setStatus(CategoryItemStatus.COMPLETED);
            categoryItemRepository.save(categoryItem);
        } else {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
        }
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