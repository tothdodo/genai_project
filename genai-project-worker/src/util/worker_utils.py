import logging
import signal
import sys
import time
import json
import threading
from typing import Callable, Set

from messaging.message_model import BaseMessage
from messaging.rabbit_config import get_rabbitmq_config
from messaging.rabbit_connect import create_rabbit_con_and_return_channel
from messaging.result_publisher import ResultPublisher

rabbitConfig = get_rabbitmq_config()

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


def start_cancellation_listener(cancelled_categories: Set[str]):
    """
    Starts a background thread that connects to the cancellation fanout exchange.
    Any received categoryItemIds are added to the provided cancelled_categories set.
    """
    def run_listener():
        while True:
            try:
                # Create a dedicated connection for this thread
                channel = create_rabbit_con_and_return_channel()

                # Ensure exchange exists (fanout)
                # Fallback string provided in case config isn't updated yet, but config update is recommended
                exchange_name = getattr(rabbitConfig, 'exchange_worker_cancellation', 'worker-cancellation')
                channel.exchange_declare(
                    exchange=exchange_name,
                    exchange_type='fanout',
                    durable=True
                )

                # Create a temporary, exclusive queue for this worker instance
                result = channel.queue_declare(queue='', exclusive=True)
                queue_name = result.method.queue

                # Bind to the fanout exchange
                channel.queue_bind(exchange=exchange_name, queue=queue_name)

                def on_cancel_message(ch, method, properties, body):
                    try:
                        data = json.loads(body)
                        cat_id = str(data.get("categoryItemId"))
                        if cat_id:
                            logging.warning(f"!!! RECEIVED CANCELLATION SIGNAL FOR CATEGORY: {cat_id} !!!")
                            cancelled_categories.add(cat_id)
                    except Exception as e:
                        logging.error(f"Error processing cancellation message: {e}")

                logging.info(f"Cancellation listener started on queue {queue_name}")
                channel.basic_consume(queue=queue_name, on_message_callback=on_cancel_message, auto_ack=True)
                channel.start_consuming()

            except Exception as e:
                logging.error(f"Cancellation listener connection lost: {e}. Retrying in 5s...")
                time.sleep(5)

    # Start as daemon thread so it closes when the main process exits
    listener_thread = threading.Thread(target=run_listener, daemon=True)
    listener_thread.start()