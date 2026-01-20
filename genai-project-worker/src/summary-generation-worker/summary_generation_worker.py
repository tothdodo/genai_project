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
from schemas.summary_generation import schema as summary_generation_schema
from util.gemini_client import GeminiClient


SUMMARY_PROMPT = r"""
Summarize the following content clearly and concisely.
Focus on key ideas, definitions, and facts.
Avoid repetition.
IMPORTANT: The text may contain LaTeX formatting. You must escape all backslashes in your JSON output (e.g., write "\\pi" instead of "\pi").
Do not include markdown formatting (like ```json). Just return the raw JSON string.

{feedback}

TEXT:
{text}
"""

rabbitConfig = get_rabbitmq_config()

DEFAULT_MODEL = "gemini-flash-latest"
FALLBACK_MODEL = "gemini-2.0-flash"


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
        validate(instance=json_req, schema=summary_generation_schema)
    except ValidationError as e:
        logging.warning(f"The job request is invalid: {e}")
        return False
    return True


def process_req(ch, method, properties, body):
    start_time = time.time()

    try:
        # 1. Parse Body
        request = json.loads(body)
        logging.info(f"Received request: {request}")

        # 2. Validate Schema
        if not validate_request(request):
            logging.error("Validation failed. Dropping message.")
            return

        # 3. Extract Fields
        job_id = request.get('job_id')
        input_text = request.get('text')
        category_id = request.get('category_id')
        chunk_number = request.get('chunk_number')

        if not input_text or not job_id:
            logging.error("Missing 'text' or 'job_id' in payload.")
            return

        # 4. Generate Summary via Gemini
        logging.info(f"Generating summary for Job {job_id}...")
        status = "unknown"
        summary_text = ""

        try:
            gemini_client = GeminiClient()
        except Exception as e:
            logging.error(f"Failed to instantiate GeminiClient: {e}")
            return

        max_attempts = 3
        last_error = None

        for attempt in range(max_attempts):
            try:
                current_model = DEFAULT_MODEL
                if attempt == 2:
                    logging.info(f"Attempt {attempt + 1}: Retrying with fallback model {FALLBACK_MODEL}...")
                    current_model = FALLBACK_MODEL

                feedback_str = ""
                if last_error:
                    feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE OUTPUT FORMAT."
                    logging.info(f"Retrying with error context: {last_error}")

                formatted_prompt = SUMMARY_PROMPT.format(text=input_text, feedback=feedback_str)

                summary_text = gemini_client.generate_content(formatted_prompt, model_name=current_model)

                if summary_text and summary_text.startswith("Error"):
                    raise Exception(summary_text)

                status = "success"
                break

            except Exception as api_error:
                last_error = str(api_error)
                logging.error(f"Gemini API/Generation Error (Attempt {attempt + 1}): {last_error}")
                if attempt == max_attempts - 1:
                    status = "failed"
                    summary_text = "Error generating summary."
                else:
                    time.sleep(2)

        # 5. Prepare Result Payload
        result_payload = {
            "summary": summary_text,
            "category_id": category_id,
            "chunk_number": chunk_number,
            "duration": time.time() - start_time
        }

        # 6. Publish Result
        # Initialize publisher with the existing channel to reuse the connection
        publisher = ResultPublisher(ch)
        publisher.publish_summary_result(
            payload=result_payload,
            original_job_id=job_id,
            status=status
        )

        logging.info(f"Job {job_id} completed. Status: {status}. Time: {time.time() - start_time:.2f}s")

    except json.JSONDecodeError:
        logging.error("Failed to decode JSON body")
    except Exception as e:
        logging.error(f"Unexpected error in process_req: {e}")


def main():
    logging.info("Summary Generation Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    # Using the queue name from definitions.json
    queue_name = "worker.summary.generation.job"

    # Ensure this matches your rabbit_config.queue_summary_generation_job
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)

    logging.info(f"Waiting for messages on {queue_name}")
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logging.warning("Worker interrupted by KeyboardInterrupt")
    except (ReentrancyError, ChannelWrongStateError, StreamLostError) as e:
        logging.error(f"RabbitMQ Connection Error: {e}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--verbose", default=False, action="store_true", help="Enable debug logging")
    args = parser.parse_args()

    logging.basicConfig(level=(logging.DEBUG if args.verbose else logging.INFO))
    main()