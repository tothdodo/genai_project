package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.configuration.RabbitConfig;
import genai.genaiprojectbackend.model.dtos.StartAggregationJobDto;
import genai.genaiprojectbackend.model.dtos.StartFlashcardGenerationJobDto;
import genai.genaiprojectbackend.model.dtos.StartSummaryGenerationJobDto;
import genai.genaiprojectbackend.model.entities.*;
import genai.genaiprojectbackend.model.enums.CategoryItemStatus;
import genai.genaiprojectbackend.model.enums.JobStatus;
import genai.genaiprojectbackend.model.enums.JobType;
import genai.genaiprojectbackend.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
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
    private final RabbitTemplate rabbitTemplate;

    public WorkerResultService(
            TextChunkRepository textChunkRepository,
            SummaryChunkRepository summaryChunkRepository,
            TemporaryFlashcardRepository temporaryFlashcardRepository,
            WorkerStartService workerStartService,
            FileRepository fileRepository,
            CategoryItemRepository categoryItemRepository,
            JobRepository jobRepository,
            FinalSummaryRepository finalSummaryRepository,
            FinalFlashcardRepository finalFlashcardRepository,
            RabbitTemplate rabbitTemplate
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
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void processTextExtractionResult(Map<String, Object> result) {
        try {
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            Job job = getValidJobFromResult(result).orElse(null);
            if (job == null || !isResultSuccessful(result, job)) return;

            if (!isJobActive(job)) {
                log.warn("Backend: Received Text Extraction result for Job {} but status is {}. Ignoring (Zombie Result).", job.getId(), job.getStatus());
                return;
            }

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

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        workerStartService.startSummaryGenerationJob(jobDto);
                    }
                });
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing text extraction result", e);
        }
    }

    @Transactional
    public void processSummaryGenerationResult(Map<String, Object> result) {
        log.info("Backend: Received Summary Generation Result: {}", result);

        Job job = getValidJobFromResult(result).orElse(null);

        if (job == null) {
            log.error("Backend: Job NOT FOUND from result. Looking for keys 'job_id' or 'original_job_id'. Raw map: {}", result);
            return;
        }

        if (!isResultSuccessful(result, job)) {
            log.info("Backend: Job {} detected as FAILED via isResultSuccessful check. Returning...", job.getId());
            return;
        }

        if (!isJobActive(job)) {
            log.warn("Backend: Received Summary result for Job {} but status is {}. Ignoring (Zombie Result).", job.getId(), job.getStatus());
            return;
        }

        log.info("Backend: Job {} SUCCESS. Processing payload...", job.getId());

        try {
            Map<String, Object> payload = getPayload(result);
            String summaryText = (String) payload.get("summary");
            Integer chunkNumber = (Integer) payload.get("chunk_number");

            Optional<TextChunk> textChunkOpt = textChunkRepository.findByFile_IdAndChunkIndex(job.getFileId(), chunkNumber);

            if (textChunkOpt.isPresent() && StringUtils.hasText(summaryText)) {
                SummaryChunk summaryChunk = new SummaryChunk(textChunkOpt.get(), summaryText);
                summaryChunk = summaryChunkRepository.save(summaryChunk);

                job.setStatus(JobStatus.FINISHED);
                jobRepository.save(job);

                Job flashcardJob = new Job(JobType.FLASHCARD_GENERATION, job.getCategoryItemId());
                flashcardJob.setFileId(job.getFileId());
                flashcardJob = jobRepository.save(flashcardJob);

                StartFlashcardGenerationJobDto startDto = StartFlashcardGenerationJobDto.builder()
                        .jobId(flashcardJob.getId())
                        .summaryChunkId(summaryChunk.getId())
                        .text(summaryText)
                        .categoryItemId(job.getCategoryItemId())
                        .build();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        workerStartService.startFlashcardGenerationJob(startDto);
                    }
                });

            } else {
                log.error("Backend: Summary logic failed (Chunk found: {}, Text valid: {}). Triggering handleJobFailure.",
                        textChunkOpt.isPresent(), StringUtils.hasText(summaryText));
                handleJobFailure(job);
            }
        } catch (Exception e) {
            log.error("Backend: Exception in processSummaryGenerationResult: ", e);
            handleJobFailure(job);
        }
    }

    @Transactional
    public void processFlashcardGenerationResult(Map<String, Object> result) {
        Job job = getValidJobFromResult(result).orElse(null);
        if (job == null || !isResultSuccessful(result, job)) return;

        if (!isJobActive(job)) {
            log.warn("Backend: Received Flashcard result for Job {} but status is {}. Ignoring (Zombie Result).", job.getId(), job.getStatus());
            return;
        }

        Map<String, Object> payload = getPayload(result);

        Integer summaryChunkId = (Integer) payload.get("summary_chunk_id");
        Optional<SummaryChunk> summaryChunkOpt = summaryChunkRepository.findById(summaryChunkId);

        if (summaryChunkOpt.isEmpty()) {
            handleJobFailure(job);
            return;
        }

        SummaryChunk summaryChunk = summaryChunkOpt.get();

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

        job.setStatus(JobStatus.FINISHED);
        jobRepository.save(job);

        checkAndStartAggregation(job.getCategoryItemId());
    }

    /**
     * Checks if ALL jobs for a specific CategoryItem are finished.
     * If so, starts the Aggregation Job for the entire CategoryItem.
     */
    private void checkAndStartAggregation(Integer categoryItemId) {
        long pendingJobs = jobRepository.countByCategoryItemIdAndStatusIn(
                categoryItemId,
                List.of(JobStatus.PENDING, JobStatus.IN_PROGRESS)
        );

        if (pendingJobs == 0) {
            List<SummaryChunk> summaries = summaryChunkRepository.findAllByTextChunk_File_CategoryItem_Id(categoryItemId);
            List<String> summaryTexts = summaries.stream().map(SummaryChunk::getSummaryText).toList();

            List<TemporaryFlashcard> tempFlashcards = temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(categoryItemId);
            List<Map<String, String>> flashcardMaps = tempFlashcards.stream()
                    .map(f -> Map.of("question", f.getQuestion(), "answer", f.getAnswer()))
                    .toList();

            if (summaryTexts.isEmpty() && flashcardMaps.isEmpty()) {
                return;
            }

            Job aggJob = new Job(JobType.AGGREGATION, categoryItemId);
            aggJob.setFileId(null);
            aggJob = jobRepository.save(aggJob);

            StartAggregationJobDto dto = StartAggregationJobDto.builder()
                    .jobId(aggJob.getId())
                    .categoryItemId(categoryItemId)
                    .summaries(summaryTexts)
                    .flashcards(flashcardMaps)
                    .build();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workerStartService.startAggregationJob(dto);
                }
            });
        }
    }

    @Transactional
    public void processAggregationResult(Map<String, Object> result) {
        Job job = getValidJobFromResult(result).orElse(null);
        if (job == null || !isResultSuccessful(result, job)) return;

        if (!isJobActive(job)) {
            log.warn("Backend: Received Aggregation result for Job {} but status is {}. Ignoring (Zombie Result).", job.getId(), job.getStatus());
            return;
        }

        Map<String, Object> payload = getPayload(result);
        String finalSummaryText = (String) payload.get("final_summary");

        if (job.getCategoryItemId() == null) {
            handleJobFailure(job);
            return;
        }

        CategoryItem categoryItem = categoryItemRepository.findById(job.getCategoryItemId()).orElse(null);

        if (categoryItem != null) {
            FinalSummary finalSummary = new FinalSummary(finalSummaryText, categoryItem);
            finalSummaryRepository.save(finalSummary);

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

            temporaryFlashcardRepository.deleteAll(
                    temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(categoryItem.getId())
            );
            summaryChunkRepository.deleteAll(
                    summaryChunkRepository.findAllByTextChunk_File_CategoryItem_Id(categoryItem.getId())
            );

            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);

            categoryItem.setStatus(CategoryItemStatus.COMPLETED);
            categoryItemRepository.save(categoryItem);
        } else {
            handleJobFailure(job);
        }
    }

    /**
     * Determines if a job is in an "Active" state (Pending or In Progress).
     * If a job is Cancelled, Failed, or already Finished, it is considered inactive.
     */
    private boolean isJobActive(Job job) {
        return job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.IN_PROGRESS;
    }

    /**
     * Handles job failure by updating statuses and cleaning up all temporary data
     * (Flashcards, SummaryChunks, TextChunks) for the associated CategoryItem.
     */
    private void handleJobFailure(Job job) {
        log.warn("Backend: handleJobFailure called for Job ID: {}", job.getId());

        if (job.getStatus() == JobStatus.CANCELLED) {
            log.info("Backend: Job {} already CANCELLED. Skipping failure logic.", job.getId());
            return;
        }

        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);

        Integer categoryItemId = job.getCategoryItemId();
        if (categoryItemId != null) {
            Optional<CategoryItem> categoryItemOpt = categoryItemRepository.findById(categoryItemId);
            if (categoryItemOpt.isPresent()) {
                CategoryItem categoryItem = categoryItemOpt.get();
                log.info("Backend: Marking CategoryItem {} as FAILED.", categoryItemId);

                categoryItem.setStatus(CategoryItemStatus.FAILED);
                categoryItem.setFailedJobType(job.getJobType());
                categoryItemRepository.save(categoryItem);

                log.info("Backend: Cancelling remaining jobs for category {}", categoryItemId);
                jobRepository.cancelRemainingJobs(categoryItemId);

                Map<String, Object> abortMessage = Map.of("categoryItemId", categoryItemId);

                log.info("Backend: Sending broadcast to '{}': {}", RabbitConfig.WORKER_CANCELLATION_EXCHANGE, abortMessage);

                rabbitTemplate.convertAndSend(
                        RabbitConfig.WORKER_CANCELLATION_EXCHANGE,
                        "",
                        abortMessage
                );

                log.info("Backend: Broadcast sent (buffered in transaction). Cleaning up data...");
                cleanupFailedCategoryData(categoryItemId);
            } else {
                log.error("Backend: CategoryItem {} not found in DB.", categoryItemId);
            }
        } else {
            log.error("Backend: Job {} has NULL categoryItemId.", job.getId());
        }
    }

    private void cleanupFailedCategoryData(Integer categoryItemId) {
        List<TemporaryFlashcard> tempFlashcards = temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_CategoryItem_Id(categoryItemId);
        if (!tempFlashcards.isEmpty()) {
            temporaryFlashcardRepository.deleteAll(tempFlashcards);
        }

        List<SummaryChunk> summaryChunks = summaryChunkRepository.findAllByTextChunk_File_CategoryItem_Id(categoryItemId);
        if (!summaryChunks.isEmpty()) {
            summaryChunkRepository.deleteAll(summaryChunks);
        }

        List<TextChunk> textChunks = textChunkRepository.findAllByCategoryItem_Id(categoryItemId);
        if (!textChunks.isEmpty()) {
            textChunkRepository.deleteAll(textChunks);
        }
    }

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
        log.debug("Backend: Checking status for Job {}: '{}'", job.getId(), statusStr);

        if ("success".equalsIgnoreCase(statusStr)) {
            return true;
        }

        log.warn("Backend: Status is NOT success. Triggering handleJobFailure for Job {}", job.getId());
        handleJobFailure(job);
        return false;
    }
}