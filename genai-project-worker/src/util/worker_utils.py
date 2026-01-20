import logging
import signal
import sys
import time
from typing import Callable

from messaging.message_model import BaseMessage
from messaging.rabbit_connect import create_rabbit_con_and_return_channel
from messaging.result_publisher import ResultPublisher


def handle_sigterm(signum, frame):
    logging.info("SIGTERM received")
    sys.exit(0)


def setup_sigterm():
    signal.signal(signal.SIGTERM, handle_sigterm)


def connect_rabbitmq():
    while True:
        try:
            return create_rabbit_con_and_return_channel()
        except Exception as e:
            logging.error("RabbitMQ connection failed, Retrying in 5s... Error: {}".format(e))
            time.sleep(5)


def mk_error_msg(job_id: str, error_msg: str):
    return BaseMessage(
        type="metadata",
        job_id=job_id,
        status="error",
        payload={
            "msg": error_msg
        }
    )


def clean_json_response(text):
    if text.startswith("```json"):
        text = text[7:]
    if text.endswith("```"):
        text = text[:-3]
    return text.strip()


def publish_response_with_connection(msg: BaseMessage, publish_func: Callable[[ResultPublisher, BaseMessage], None]):
    """
    Creates a new RabbitMQ connection, publishes the message using the provided callback,
    and closes the connection.
    """
    try:
        pub_channel = create_rabbit_con_and_return_channel()
        publisher = ResultPublisher(pub_channel)

        publish_func(publisher, msg)

        if pub_channel.connection and pub_channel.connection.is_open:
            pub_channel.connection.close()

        logging.info(f"Sent response for Job {msg.job_id} with status: {msg.status}")
    except Exception as pub_error:
        logging.error(f"Failed to publish response for Job {msg.job_id}: {pub_error}")