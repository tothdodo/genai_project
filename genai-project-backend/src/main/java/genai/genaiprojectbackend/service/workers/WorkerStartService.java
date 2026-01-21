package genai.genaiprojectbackend.service.workers;

import genai.genaiprojectbackend.configuration.RabbitConfig;
import genai.genaiprojectbackend.model.dtos.StartAggregationJobDto;
import genai.genaiprojectbackend.model.dtos.StartFlashcardGenerationJobDto;
import genai.genaiprojectbackend.model.dtos.StartSummaryGenerationJobDto;
import genai.genaiprojectbackend.model.dtos.StartTextExtractionJobDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkerStartService {

    private final RabbitTemplate rabbitTemplate;


    public WorkerStartService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }


    public void startAggregationJob(StartAggregationJobDto startMetadataJobDto) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.WORKER_JOB_EXCHANGE,
                RabbitConfig.WORKER_AGGREGATION_START_ROUTING_KEY,
                startMetadataJobDto
        );
    }

    public void startFlashcardGenerationJob(StartFlashcardGenerationJobDto startPreProcessJobDto) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.WORKER_JOB_EXCHANGE,
                RabbitConfig.WORKER_FLASHCARD_GENERATION_START_ROUTING_KEY,
                startPreProcessJobDto
        );
    }

    public void startSummaryGenerationJob(StartSummaryGenerationJobDto startComparisonJobDto) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.WORKER_JOB_EXCHANGE,
                RabbitConfig.WORKER_SUMMARY_GENERATION_START_ROUTING_KEY,
                startComparisonJobDto
        );
    }

    public void startTextExtractionJob(StartTextExtractionJobDto startChunkingJobDto) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.WORKER_JOB_EXCHANGE,
                RabbitConfig.WORKER_TEXT_EXTRACTION_START_ROUTING_KEY,
                startChunkingJobDto
        );
    }
}

