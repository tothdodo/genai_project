# worker_python/messaging/result_publisher.py
import uuid
import pika

from .message_model import BaseMessage
from .rabbit_connect import create_rabbit_con_and_return_channel
from .rabbit_config import get_rabbitmq_config

rabbitConfig = get_rabbitmq_config()

class ResultPublisher:
    def __init__(self, ch=None):
        # check if external connection exist --> testcases
        # Kein externes Objekt übergeben → selber alles erstellen
        if ch is None:
            self._ch = create_rabbit_con_and_return_channel()
        else:
            self._ch = ch

    def _publish(self, routing_key: str, payload, msg_type: str, status: str = "unknown", job_id_override: str = None):
        if isinstance(payload, BaseMessage):
            msg = payload
        else:
            final_job_id = job_id_override if job_id_override else str(uuid.uuid4())

            msg = BaseMessage(
                type=msg_type,
                status=status,
                job_id=final_job_id,
                payload=payload,
            )

        self._ch.basic_publish(
            exchange="worker-results",
            routing_key=routing_key,
            body=msg.to_json(),
            properties=pika.BasicProperties(
                content_type="application/json",
                delivery_mode=2,  # persistent
            ),
        )

    # def publish_preprocessing_result(self, payload, status: str = "unknown"):
    #     self._publish(routing_key=rabbitConfig.routing_preprocessing_result, payload=payload, msg_type=rabbitConfig.routing_preprocessing_result, status=status)
    #
    # def publish_comparison_result(self, payload: dict, status: str = "unknown"):
    #     self._publish(rabbitConfig.routing_comparison_result, payload, rabbitConfig.routing_comparison_result,status=status)

    def publish_text_extraction_result(self, payload: dict, status: str = "unknown"):
        self._publish(rabbitConfig.routing_text_extraction_result, payload, rabbitConfig.routing_text_extraction_result,status=status)

    # def publish_chunking_comparison_result(self, payload: dict, status: str = "unknown"):
    #     self._publish(rabbitConfig.routing_chunking_comparison_result, payload, rabbitConfig.routing_chunking_comparison_result,status=status)

    def publish_summary_result(self, payload: dict, original_job_id: str, status: str = "unknown"):
        routing_key = rabbitConfig.routing_summary_generation_result

        self._publish(
            routing_key=routing_key,
            payload=payload,
            msg_type=routing_key,
            status=status,
            job_id_override=original_job_id
        )

    def publish_flashcard_result(self, payload: dict, original_job_id: str, status: str = "unknown"):
        routing_key = rabbitConfig.routing_flashcard_generation_result
        self._publish(
            routing_key=routing_key,
            payload=payload,
            msg_type=routing_key,
            status=status,
            job_id_override=original_job_id
        )

    def publish_aggregation_result(self, payload: dict, original_job_id: str, status: str = "unknown"):
        routing_key = rabbitConfig.routing_aggregation_result
        self._publish(
            routing_key=routing_key,
            payload=payload,
            msg_type=routing_key,
            status=status,
            job_id_override=original_job_id
        )

    def close(self):
        # if connection is done externally, connection is not closed here
        if not self._external_conn:
            if self._ch and self._ch.is_open:
                self._ch.close()
            if self._conn and self._conn.is_open:
                self._conn.close()
