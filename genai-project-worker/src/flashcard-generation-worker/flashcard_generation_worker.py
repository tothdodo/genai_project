import json
import logging
import time

from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError, AMQPError

from messaging.rabbit_config import get_rabbitmq_config
from messaging.message_model import BaseMessage
from schemas.flashcard_generation import schema as flashcard_generation_schema
from util.gemini_client import GeminiClient
from util.worker_utils import (
    setup_sigterm,
    connect_rabbitmq,
    mk_error_msg,
    clean_json_response,
    publish_response_with_connection,
    start_cancellation_listener
)

FLASHCARD_PROMPT = r"""
You are a flashcard generator. Create a list of flashcards based on the provided text.
The output must be a valid JSON list of objects.
Each object must have exactly two keys: "question" and "answer".
IMPORTANT: The text may contain LaTeX formatting. You must escape all backslashes in your JSON output (e.g., write "\\pi" instead of "\pi").
Do not include markdown formatting (like ```json). Just return the raw JSON string.

{feedback}

TEXT:
{text}
"""

rabbitConfig = get_rabbitmq_config()

DEFAULT_MODEL = "gemini-flash-latest"
FALLBACK_MODEL = "gemini-2.0-flash"

cancelled_categories = set()
setup_sigterm()


def validate_request(json_req):
    try:
        validate(instance=json_req, schema=flashcard_generation_schema)
    except ValidationError as e:
        logging.warning(f"The job request is invalid: {e}")
        return False
    return True


def process_req(ch, method, properties, body):
    start_time = time.time()

    def safe_ack():
        try:
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except (AMQPError, Exception) as ack_e:
            logging.warning(f"Could not ack message: {ack_e}")

    def publish_response(msg: BaseMessage):
        publish_response_with_connection(
            msg,
            lambda pub, m: pub.publish_flashcard_result(
                payload=m.payload,
                original_job_id=m.job_id,
                status=m.status
            )
        )

    try:
        request = json.loads(body)

        job_id = request.get('job_id')
        if not job_id:
            logging.error("Missing 'job_id' in payload.")
            safe_ack()
            return

        if not validate_request(request):
            logging.error("Validation failed. Cancelling job.")
            publish_response(mk_error_msg(job_id, "Flashcard job cancelled because request is invalid"))
            safe_ack()
            return

        summary_chunk_id = request.get('summary_chunk_id')
        input_text = request.get('text')
        category_item_id = request.get('category_item_id')

        if str(category_item_id) in cancelled_categories:
            logging.info(f"Job {job_id} cancelled for category {category_item_id} before starting.")
            safe_ack()
            return

        logging.info(f"Generating flashcards for Job {job_id}...")
        status = "unknown"
        flashcards_list = []

        max_attempts = 5
        last_error = None

        try:
            gemini_client = GeminiClient()
        except Exception as e:
            logging.error(f"Failed to instantiate GeminiClient: {e}")
            publish_response(mk_error_msg(job_id, "Failed to initialize AI client"))
            safe_ack()
            return

        for attempt in range(max_attempts):
            if str(category_item_id) in cancelled_categories:
                logging.info(f"Job {job_id} cancelled for category {category_item_id} during attempt {attempt + 1}.")

                safe_ack()
                return

            try:
                current_model = DEFAULT_MODEL
                if attempt == 3:
                    logging.info(f"Attempt {attempt + 1}: Retrying with fallback model {FALLBACK_MODEL}...")
                    current_model = FALLBACK_MODEL

                feedback_str = ""
                if last_error:
                    feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE JSON STRUCTURE."
                    logging.info(f"Retrying with error context: {last_error}")

                formatted_prompt = FLASHCARD_PROMPT.format(
                    text=input_text,
                    feedback=feedback_str
                )

                raw_response = gemini_client.generate_content(formatted_prompt, model_name=current_model)

                cleaned_response = clean_json_response(raw_response)
                flashcards_list = json.loads(cleaned_response)

                if isinstance(flashcards_list, list):
                    status = "success"
                    break
                else:
                    error_msg = "Output parsed as valid JSON but was not a list."
                    logging.error(f"{error_msg} (Attempt {attempt + 1})")

                    if attempt == max_attempts - 1:
                        publish_response(mk_error_msg(job_id, "Failed to generate valid flashcards JSON."))
                        safe_ack()
                        return
                    else:
                        raise ValueError(error_msg)

            except Exception as api_or_json_error:
                last_error = str(api_or_json_error)
                logging.error(f"Gemini/Parsing Error (Attempt {attempt + 1}): {last_error}")

                if attempt == max_attempts - 1:
                    publish_response(mk_error_msg(job_id, "An unexpected error occurred during generation."))
                    safe_ack()
                    return
                else:
                    time.sleep(5)

        result_payload = {
            "flashcards": flashcards_list,
            "summary_chunk_id": summary_chunk_id,
            "duration": time.time() - start_time
        }

        publish_response(BaseMessage(
            type="flashcard_generation",
            job_id=job_id,
            status=status,
            payload=result_payload
        ))

        safe_ack()

    except json.JSONDecodeError:
        logging.error("Failed to decode JSON body")
        safe_ack()
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
        try:
            req_dict = json.loads(body)
            jid = req_dict.get("job_id")
            if jid:
                publish_response(mk_error_msg(jid, "An unexpected error occurred, flashcard job cancelled."))
        except:
            pass
        safe_ack()


def main():
    logging.info("Flashcard Generation Worker Started!")
    start_cancellation_listener(cancelled_categories)
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    queue_name = rabbitConfig.queue_flashcard_generation_job
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=False)

    logging.info(f"Waiting for messages on {queue_name}")
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        logging.warning("Interrupted")
    except (ReentrancyError, ChannelWrongStateError, StreamLostError) as e:
        logging.error(f"RabbitMQ Connection Error: {e}")


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    logging.getLogger("pika").setLevel(logging.CRITICAL)
    main()