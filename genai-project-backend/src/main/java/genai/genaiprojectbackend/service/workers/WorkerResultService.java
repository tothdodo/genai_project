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
    public void processTextExtractionResult(Map<String, Object> payload) {
        try {
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

        checkAndStartAggregation(job.getFileId(), job.getCategoryItemId());
    }

    private void checkAndStartAggregation(Long fileId, Integer categoryItemId) {
        // Check if there are any pending or in-progress jobs for this file
        long pendingJobs = jobRepository.countByFileIdAndStatusIn(
                fileId,
                List.of(JobStatus.PENDING, JobStatus.IN_PROGRESS)
        );

        if (pendingJobs == 0) {
            // All chunks are processed. Start Aggregation.
            List<SummaryChunk> summaries = summaryChunkRepository.findAllByTextChunk_File_Id(fileId);
            List<String> summaryTexts = summaries.stream().map(SummaryChunk::getSummaryText).toList();

            // Fetch Temporary Flashcards
            List<TemporaryFlashcard> tempFlashcards = temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_Id(fileId);
            List<Map<String, String>> flashcardMaps = tempFlashcards.stream()
                    .map(f -> Map.of("question", f.getQuestion(), "answer", f.getAnswer()))
                    .toList();

            Job aggJob = new Job(JobType.AGGREGATION, categoryItemId);
            aggJob.setFileId(fileId);
            aggJob = jobRepository.save(aggJob);

            StartAggregationJobDto dto = StartAggregationJobDto.builder()
                    .jobId(aggJob.getId())
                    .categoryItemId(categoryItemId)
                    .fileId(fileId)
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

        // Ensure we have the necessary IDs
        if (job.getFileId() == null || job.getCategoryItemId() == null) {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        File file = fileRepository.findById(job.getFileId()).orElse(null);
        CategoryItem categoryItem = categoryItemRepository.findById(job.getCategoryItemId()).orElse(null);

        if (file != null && categoryItem != null) {
            // 1. Save Final Summary
            FinalSummary finalSummary = new FinalSummary(finalSummaryText, categoryItem);
            finalSummaryRepository.save(finalSummary);

            // 2. Save Final Flashcards
            Object finalFlashcardsObj = payload.get("final_flashcards");
            if (finalFlashcardsObj instanceof List<?>) {
                List<?> list = (List<?>) finalFlashcardsObj;
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        String question = (String) map.get("question");
                        String answer = (String) map.get("answer");

                        if (question != null && answer != null) {
                            FinalFlashcard ff = new FinalFlashcard(file, question, answer, categoryItem);
                            finalFlashcardRepository.save(ff);
                        }
                    }
                }
            }

            // 3. Cleanup Temporary Tables
            temporaryFlashcardRepository.deleteAll(
                    temporaryFlashcardRepository.findAllBySummaryChunk_TextChunk_File_Id(file.getId())
            );
            summaryChunkRepository.deleteAll(
                    summaryChunkRepository.findAllByTextChunk_File_Id(file.getId())
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