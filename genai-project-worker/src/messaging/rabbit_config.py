import os
from dataclasses import dataclass

@dataclass
class RabbitMQConfig:
    host: str = None
    port: int = None
    username: str = None
    password: str = None
    vhost: str = None
    prefetch_count: int = None

    exchange_worker_job: str = "worker-job"
    exchange_worker_cancellation: str = "worker-cancellation"
    queue_aggregation_job: str = "worker.aggregation.job"
    queue_flashcard_generation_job: str = "worker.flashcard.generation.job"
    queue_summary_generation_job: str = "worker.summary.generation.job"
    queue_text_extraction_job: str = "worker.text.extraction.job"
    routing_aggregation_start: str = "worker.aggregation.job.start"
    routing_flashcard_generation_start: str = "worker.flashcard.generation.job.start"
    routing_summary_generation_start: str = "worker.summary.generation.job.start"
    routing_text_extraction_start: str = "worker.text.extraction.job.start"


    exchange_worker_results: str = "worker-results"
    queue_aggregation_result: str = "worker.aggregation.result"
    queue_flashcard_generation_result: str = "worker.flashcard.generation.result"
    queue_summary_generation_result: str = "worker.summary.generation.result"
    queue_text_extraction_result: str = "worker.text.extraction.result"

    routing_aggregation_result: str = "worker.aggregation.result"
    routing_flashcard_generation_result: str = "worker.flashcard.generation.result"
    routing_summary_generation_result: str = "worker.summary.generation.result"
    routing_text_extraction_result: str = "worker.text.extraction.result"

    def __post_init__(self):
        if self.host is None:
            self.host = os.getenv("RABBITMQ_HOST", "rabbitmq")
        if self.port is None:
            self.port = int(os.getenv("RABBITMQ_PORT", "5672"))
        if self.username is None:
            self.username = os.getenv("RABBITMQ_USER", "admin")
        if self.password is None:
            self.password = os.getenv("RABBITMQ_PASSWORD", "admin")
        if self.vhost is None:
            self.vhost = os.getenv("RABBITMQ_VHOST", "/")
        if self.prefetch_count is None:
            self.prefetch_count = int(os.getenv("RABBITMQ_PREFETCH", "0"))

def get_rabbitmq_config():
    return RabbitMQConfig()

