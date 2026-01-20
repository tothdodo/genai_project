import json
import logging
import time

from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError, AMQPError

from messaging.rabbit_config import get_rabbitmq_config
from messaging.message_model import BaseMessage
from schemas.aggregation import schema as aggregation_schema
from util.gemini_client import GeminiClient
from util.worker_utils import (
    setup_sigterm,
    connect_rabbitmq,
    mk_error_msg,
    clean_json_response,
    publish_response_with_connection
)

AGGREGATION_PROMPT = r"""
You are an expert summarizer. 
Below is a list of partial summaries extracted from a document. 
Your task is to combine them into one cohesive, comprehensive, and well-structured final summary.
Ensure the flow is logical and avoid redundancy.
IMPORTANT: The text may contain LaTeX formatting.
IMPORTANT: Return the output in a markdown formatted string.

{feedback}

PARTIAL SUMMARIES:
{text}
"""

FLASHCARD_AGGREGATION_PROMPT = r"""
You are an expert study aid creator. 
Below is a list of raw flashcards (Question/Answer pairs) generated from chunks of a document.
Your task is to deduplicate them and refine the questions and answers to be concise, clear, and accurate.
Merge similar cards if necessary.
The output must be a valid JSON list of objects.
Each object must have exactly two keys: "question" and "answer".
IMPORTANT: The text may contain LaTeX formatting. You must escape all backslashes in your JSON output (e.g., write "\\pi" instead of "\pi").
Do not include markdown formatting (like ```json). Just return the raw JSON string.

{feedback}

FLASHCARDS:
{text}
"""

rabbitConfig = get_rabbitmq_config()

DEFAULT_MODEL = "gemini-flash-latest"
FALLBACK_MODEL = "gemini-2.0-flash"

setup_sigterm()


def validate_request(json_req):
    try:
        validate(instance=json_req, schema=aggregation_schema)
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

    # Helper to publish results
    def publish_response(msg: BaseMessage):
        publish_response_with_connection(
            msg,
            lambda pub, m: pub.publish_aggregation_result(
                payload=m.payload,
                original_job_id=m.job_id,
                status=m.status
            )
        )

    try:
        request = json.loads(body)
        logging.info(f"Received request: {request}")

        job_id = request.get('job_id')
        if not job_id:
            logging.error("Missing 'job_id'. Dropping message.")
            safe_ack()
            return

        if not validate_request(request):
            logging.error("Validation failed. Cancelling job.")
            publish_response(mk_error_msg(job_id, "Aggregation job cancelled because request is invalid"))
            safe_ack()
            return

        summaries = request.get('summaries', [])
        flashcards = request.get('flashcards', [])
        combined_text = "\n\n".join(summaries)

        logging.info(f"Generating final summary and flashcards for Job {job_id}...")
        status = "unknown"
        final_summary = ""
        final_flashcards = []

        try:
            gemini_client = GeminiClient()
        except Exception as e:
            logging.error(f"Failed to instantiate GeminiClient: {e}")
            publish_response(mk_error_msg(job_id, "Failed to initialize AI client"))
            safe_ack()
            return

        # 1. Generate Final Summary
        max_attempts = 5
        last_error = None
        summary_success = False

        for attempt in range(max_attempts):
            try:
                current_model = DEFAULT_MODEL
                if attempt == 3:
                    current_model = FALLBACK_MODEL

                feedback_str = ""
                if last_error:
                    feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE OUTPUT."
                    logging.info(f"Summary Aggregation Retry with error: {last_error}")

                formatted_prompt = AGGREGATION_PROMPT.format(text=combined_text, feedback=feedback_str)
                final_summary = gemini_client.generate_content(formatted_prompt, model_name=current_model)

                if final_summary and final_summary.startswith("Error"):
                    raise Exception(final_summary)

                summary_success = True
                break

            except Exception as api_error:
                last_error = str(api_error)
                logging.error(f"Gemini API Error during Summary Aggregation (Attempt {attempt + 1}): {last_error}")
                if attempt == max_attempts - 1:
                    publish_response(mk_error_msg(job_id, "Failed to aggregate summaries after multiple attempts."))
                    safe_ack()
                    return
                else:
                    time.sleep(5)

        # 2. Generate Final Flashcards (Only if summary succeeded)
        if summary_success and flashcards:
            flashcards_json_str = json.dumps(flashcards)
            last_error = None

            for attempt in range(max_attempts):
                try:
                    current_model = DEFAULT_MODEL
                    if attempt == 3:
                        current_model = FALLBACK_MODEL

                    feedback_str = ""
                    if last_error:
                        feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE JSON STRUCTURE."
                        logging.info(f"Flashcard Aggregation Retry with error: {last_error}")

                    formatted_prompt = FLASHCARD_AGGREGATION_PROMPT.format(text=flashcards_json_str,
                                                                           feedback=feedback_str)
                    raw_response = gemini_client.generate_content(formatted_prompt, model_name=current_model)

                    cleaned_response = clean_json_response(raw_response)
                    parsed_flashcards = json.loads(cleaned_response)

                    if isinstance(parsed_flashcards, list):
                        final_flashcards = parsed_flashcards
                        break
                    else:
                        error_msg = "Output parsed as valid JSON but was not a list."
                        logging.warning(f"Gemini response for flashcards was not a list (Attempt {attempt + 1}).")
                        last_error = error_msg

                        if attempt == max_attempts - 1:
                            logging.error("Failed to parse aggregated flashcards.")
                            publish_response(mk_error_msg(job_id, "Failed to aggregate flashcards."))
                            return
                except Exception as api_or_json_error:
                    last_error = str(api_or_json_error)
                    logging.error(
                        f"Gemini API/Parsing Error during Flashcard Aggregation (Attempt {attempt + 1}): {last_error}")
                    time.sleep(5)

        status = "success"
        result_payload = {
            "final_summary": final_summary,
            "final_flashcards": final_flashcards,
            "duration": time.time() - start_time
        }

        # 3. Publish Success
        publish_response(BaseMessage(
            type="aggregation",
            job_id=job_id,
            status=status,
            payload=result_payload
        ))

        # 4. Acknowledge Original Message
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
                publish_response(mk_error_msg(jid, "An unexpected error occurred during aggregation."))
        except:
            pass
        safe_ack()


def main():
    logging.info("Aggregation Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    queue_name = rabbitConfig.queue_aggregation_job

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
    main()