import logging
import signal
import sys
import time
import json
import argparse
from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError

from messaging.rabbit_config import get_rabbitmq_config
from messaging.rabbit_connect import create_rabbit_con_and_return_channel
from schemas.text_extraction import schema as text_extraction_schema
from messaging.message_model import BaseMessage
from messaging.result_publisher import ResultPublisher

rabbitConfig = get_rabbitmq_config()


def handle_sigterm(signum, frame):
    logging.info("SIGTERM received")
    sys.exit(0)


signal.signal(signal.SIGTERM, handle_sigterm)


def connect_rabbitmq():
    while True:
        try:
            return create_rabbit_con_and_return_channel()
        except Exception as e:
            logging.error("RabbitMQ connection failed, Retrying in 5s... Error: {}".format(e))
            time.sleep(5)


def publish_response(ch, msg: BaseMessage):
    publisher = ResultPublisher(ch)
    publisher.publish_metadata_result(msg)
    logging.debug(f"Sent response to exchange: {rabbitConfig.exchange_worker_results}")


def validate_request(json_req):
    try:
        validate(instance=json_req, schema=text_extraction_schema)
    except ValidationError as e:
        logging.warning(f"The text extraction job request is invalid, ValidationError error: {e}")
        return False  # TODO: Might be an exception here as well
    return True


def mk_error_msg(job_id: str, error_msg: str):
    return BaseMessage(
        type="metadata",
        job_id=job_id,
        status="error",
        payload={
            "msg": error_msg
        }
    )


def mk_extract_text():
    return "Extract Text"


def process_req(ch, method, properties, body):
    start_time = time.time()
    try:
        req = json.loads(body)
        if not req["jobId"]:
            logging.warning("The metadata job is cancelled because there is no job id")
            publish_response(ch,
                             mk_error_msg(job_id="", error_msg="Metadata job is cancelled because job has no job id"))

        job_id = req["jobId"]

        if not validate_request(req):
            logging.warning("The metadata job is cancelled because of a Validation Error")
            publish_response(ch, mk_error_msg(job_id, "Metadata job is cancelled because job request is invalid"))
            return

        request = json.loads(body)
        logging.info(start_time)
        logging.info("Received request: {}".format(request))
    except Exception as e:
        logging.error(f"Failed to process message: {e}")
        publish_response(ch, mk_error_msg(job_id, "An unexpected error occured, metadata job cancelled"))
    finally:
        processing_time = int((time.time() - start_time) * 1000)
        logging.info(f"Worker took {processing_time} ms to process the message.")


def main():
    # logging.info(f"Connected to RabbitMQ Listening on queue '{rabbitConfig.queue_comparison_job}'")
    logging.info("Text Extraction Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    channel.basic_consume(queue=rabbitConfig.queue_text_extraction_job, on_message_callback=callback, auto_ack=True)

    logging.info("Waiting for messages")
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logging.warning("Worker interrupted by KeyboardInterrupt")
    except ReentrancyError as e:
        logging.error("Reentrancy error for start consuming on the channel: {}".format(e))
    except ChannelWrongStateError as e:
        logging.error("Channel error: {}".format(e))
    except StreamLostError as e:
        logging.error("Stream lost error: {}".format(e))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--verbose", default=False, action="store_true", help="Enable debug logging")
    args = parser.parse_args()

    logging.basicConfig(level=(logging.DEBUG if args.verbose else logging.INFO))
    main()
