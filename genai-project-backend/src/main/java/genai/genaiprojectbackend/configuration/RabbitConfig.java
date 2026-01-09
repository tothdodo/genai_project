package genai.genaiprojectbackend.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    //TEST EXCHANGE, QUEUE, RK
    public static final String TEST_EXCHANGE = "test-exchange";
    public static final String TEST_QUEUE = "test.queue";
    public static final String TEST_RK = "test";

    // ============================
    // EXCHANGES
    // ============================
    public static final String WORKER_JOB_EXCHANGE = "worker-job";
    public static final String PYTHON_WORKER_RESULTS_EXCHANGE = "worker-results";


    // ============================
    // JOB QUEUES (Java → Python)
    // ============================
    public static final String WORKER_AGGREGATION_JOB_QUEUE = "worker.aggregation.job";
    public static final String WORKER_FLASHCARD_GENERATION_JOB_QUEUE = "worker.flashcard.generation.job";
    public static final String WORKER_SUMMARY_GENERATION_JOB_QUEUE = "worker.summary.generation.job";
    public static final String WORKER_TEXT_EXTRACTION_JOB_QUEUE = "worker.text.extraction.job";



    // ============================
    // RESULT QUEUES (Python → Java)
    // ============================
    public static final String WORKER_AGGREGATION_RESULT_QUEUE = "worker.aggregation.result";
    public static final String WORKER_FLASHCARD_GENERATION_RESULT_QUEUE = "worker.flashcard.generation.result";
    public static final String WORKER_SUMMARY_GENERATION_RESULT_QUEUE = "worker.summary.generation.result";
    public static final String WORKER_TEXT_EXTRACTION_RESULT_QUEUE = "worker.text.extraction.result";




    // ============================
    // ROUTING KEYS — Jobs (Start)
    // ============================
    public static final String WORKER_AGGREGATION_START_ROUTING_KEY = "worker.aggregation.job.start";
    public static final String WORKER_FLASHCARD_GENERATION_START_ROUTING_KEY = "worker.flashcard.generation.job.start";
    public static final String WORKER_SUMMARY_GENERATION_START_ROUTING_KEY = "worker.summary.generation.job.start";
    public static final String WORKER_TEXT_EXTRACTION_START_ROUTING_KEY = "worker.text.extraction.job.start";



    // ============================
    // ROUTING KEYS — Results
    // (Used by Python Workers)
    // ============================
    public static final String WORKER_AGGREGATION_RESULT_ROUTING_KEY = "worker.aggregation.result.start";
    public static final String WORKER_FLASHCARD_GENERATION_RESULT_ROUTING_KEY = "worker.flashcard.generation.result";
    public static final String WORKER_SUMMARY_GENERATION_RESULT_ROUTING_KEY = "worker.summary.generation.result";
    public static final String WORKER_TEXT_EXTRACTION_RESULT_ROUTING_KEY = "worker.text.extraction.result";

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

}

