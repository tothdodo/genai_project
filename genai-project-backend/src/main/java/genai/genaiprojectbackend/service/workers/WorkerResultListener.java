package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.configuration.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WorkerResultListener {

    private final WorkerResultService workerResultService;


    public WorkerResultListener(WorkerResultService workerResultService) {
        this.workerResultService = workerResultService;
    }

    @RabbitListener(queues = RabbitConfig.WORKER_TEXT_EXTRACTION_RESULT_QUEUE)
    public void handleTextExtractionResult(Map<String, Object> result) {
        workerResultService.processTextExtractionResult(result);
    }

    @RabbitListener(queues = RabbitConfig.WORKER_SUMMARY_GENERATION_RESULT_QUEUE)
    public void handleSummaryGenerationResult(Map<String, Object> result) {
        workerResultService.processSummaryGenerationResult(result);
    }

    @RabbitListener(queues = RabbitConfig.WORKER_FLASHCARD_GENERATION_RESULT_QUEUE)
    public void handleFlashcardGenerationResult(Map<String, Object> result) {
        workerResultService.processFlashcardGenerationResult(result);
    }

    @RabbitListener(queues = RabbitConfig.WORKER_AGGREGATION_RESULT_QUEUE)
    public void handleAggregationGenerationResult(Map<String, Object> result) {
        workerResultService.processAggregationResult(result);
    }
}
