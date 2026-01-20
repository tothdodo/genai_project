package genai.genaiprojectbackend.configuration;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    // ============================
    // EXCHANGES
    // ============================
    public static final String WORKER_JOB_EXCHANGE = "worker-job";
    public static final String WORKER_CANCELLATION_EXCHANGE = "worker-cancellation";

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

    @Bean
    public FanoutExchange cancellationExchange() {
        return new FanoutExchange(WORKER_CANCELLATION_EXCHANGE);
    }

}

