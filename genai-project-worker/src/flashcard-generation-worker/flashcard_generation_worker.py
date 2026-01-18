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
from messaging.result_publisher import ResultPublisher
from schemas.flashcard_generation import schema as flashcard_generation_schema
from util.gemini_client import GeminiClient

FLASHCARD_PROMPT = """
You are a flashcard generator. Create a list of flashcards based on the provided text.
The output must be a valid JSON list of objects.
Each object must have exactly two keys: "question" and "answer".
Do not include markdown formatting (like ```json). Just return the raw JSON string.

TEXT:
{text}
"""

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


def validate_request(json_req):
    try:
        validate(instance=json_req, schema=flashcard_generation_schema)
    except ValidationError as e:
        logging.warning(f"The job request is invalid: {e}")
        return False
    return True


def clean_json_response(text):
    if text.startswith("```json"):
        text = text[7:]
    if text.endswith("```"):
        text = text[:-3]
    return text.strip()


def process_req(ch, method, properties, body):
    start_time = time.time()

    try:
        request = json.loads(body)
        logging.info(f"Received request: {request}")

        if not validate_request(request):
            logging.error("Validation failed. Dropping message.")
            return

        job_id = request.get('job_id')
        summary_chunk_id = request.get('summary_chunk_id')
        input_text = request.get('text')

        logging.info(f"Generating flashcards for Job {job_id}...")
        status = "unknown"
        flashcards_list = []

        try:
            gemini_client = GeminiClient()
            formatted_prompt = FLASHCARD_PROMPT.format(text=input_text)

            # Call Gemini
            raw_response = gemini_client.generate_content(formatted_prompt)

            # Clean and Parse JSON
            cleaned_response = clean_json_response(raw_response)
            flashcards_list = json.loads(cleaned_response)

            if isinstance(flashcards_list, list):
                status = "success"
            else:
                logging.error("Gemini response was not a JSON list")
                status = "failed"
                flashcards_list = []

        except Exception as api_error:
            logging.error(f"Gemini/Parsing Error: {api_error}")
            status = "failed"

        result_payload = {
            "flashcards": flashcards_list,
            "summary_chunk_id": summary_chunk_id,
            "duration": time.time() - start_time
        }

        publisher = ResultPublisher(ch)
        publisher.publish_flashcard_result(
            payload=result_payload,
            original_job_id=job_id,
            status=status
        )

        logging.info(f"Job {job_id} completed. Status: {status}")

    except json.JSONDecodeError:
        logging.error("Failed to decode JSON body")
    except Exception as e:
        logging.error(f"Unexpected error: {e}")


def main():
    logging.info("Flashcard Generation Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    queue_name = rabbitConfig.queue_flashcard_generation_job
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)

    logging.info(f"Waiting for messages on {queue_name}")
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logging.warning("Interrupted")
    except (ReentrancyError, ChannelWrongStateError, StreamLostError) as e:
        logging.error(f"RabbitMQ Connection Error: {e}")


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    main()