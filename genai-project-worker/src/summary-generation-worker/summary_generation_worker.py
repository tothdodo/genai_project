import argparse
import json
import logging
import time

from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError, AMQPError

from messaging.message_model import BaseMessage
from messaging.rabbit_config import get_rabbitmq_config
from schemas.summary_generation import schema as summary_generation_schema
from util.gemini_client import GeminiClient
from util.worker_utils import (
    setup_sigterm,
    connect_rabbitmq,
    mk_error_msg,
    publish_response_with_connection,
    start_cancellation_listener
)

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

cancelled_categories = set()
setup_sigterm()


def validate_request(json_req):
    try:
        validate(instance=json_req, schema=summary_generation_schema)
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

    # Helper to publish results (Success or Error)
    def publish_response(msg: BaseMessage):
        publish_response_with_connection(
            msg,
            lambda pub, m: pub.publish_summary_result(
                payload=m.payload,
                original_job_id=m.job_id,
                status=m.status
            )
        )

    try:
        request = json.loads(body)

        job_id = request.get('job_id')
        if not job_id:
            logging.error("Missing 'job_id' in payload. Cannot report error.")
            safe_ack()
            return

        if not validate_request(request):
            logging.error("Validation failed. Cancelling Job.")
            publish_response(mk_error_msg(job_id, "Summary generation job cancelled because request is invalid"))
            safe_ack()
            return

        input_text = request.get('text')
        category_id = request.get('category_id')
        chunk_number = request.get('chunk_number')

        if str(category_id) in cancelled_categories:
            logging.info(f"Job {job_id} cancelled for category {category_id} before starting.")
            safe_ack()
            return

        logging.info(f"Generating summary for Job {job_id}...")
        status = "unknown"
        summary_text = ""

        try:
            gemini_client = GeminiClient()
        except Exception as e:
            logging.error(f"Failed to instantiate GeminiClient: {e}")
            publish_response(mk_error_msg(job_id, f"Failed to initialize AI client: {e}"))
            safe_ack()
            return

        max_attempts = 5
        last_error = None

        for attempt in range(max_attempts):
            if str(category_id) in cancelled_categories:
                logging.info(f"Job {job_id} cancelled for category {category_id} during attempt {attempt + 1}.")
                safe_ack()
                return

            try:
                current_model = DEFAULT_MODEL
                if attempt == 3:
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
                    publish_response(mk_error_msg(job_id, "Failed to generate summary after multiple attempts."))
                    safe_ack()
                    return
                else:
                    time.sleep(5)

        result_payload = {
            "summary": summary_text,
            "category_id": category_id,
            "chunk_number": chunk_number,
            "duration": time.time() - start_time
        }

        publish_response(BaseMessage(
            type="summary_generation",
            job_id=job_id,
            status=status,
            payload=result_payload
        ))

        safe_ack()

    except json.JSONDecodeError:
        logging.error("Failed to decode JSON body")
        safe_ack()
    except Exception as e:
        logging.error(f"Unexpected error in process_req: {e}")
        try:
            req_dict = json.loads(body)
            jid = req_dict.get("job_id")
            if jid:
                publish_response(mk_error_msg(jid, "An unexpected error occurred, summary job cancelled."))
        except:
            pass
        safe_ack()


def main():
    logging.info("Summary Generation Worker Started!")
    start_cancellation_listener(cancelled_categories)
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    queue_name = "worker.summary.generation.job"

    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=False)

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
    logging.getLogger("pika").setLevel(logging.CRITICAL)
    main()