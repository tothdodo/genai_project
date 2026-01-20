import json
import logging
import signal
import sys
import time

from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError, AMQPError

from messaging.rabbit_config import get_rabbitmq_config
from messaging.rabbit_connect import create_rabbit_con_and_return_channel
from messaging.result_publisher import ResultPublisher
from schemas.aggregation import schema as aggregation_schema
from util.gemini_client import GeminiClient

AGGREGATION_PROMPT = r"""
You are an expert summarizer. 
Below is a list of partial summaries extracted from a document. 
Your task is to combine them into one cohesive, comprehensive, and well-structured final summary.
Ensure the flow is logical and avoid redundancy.
IMPORTANT: The text may contain LaTeX formatting. You must escape all backslashes in your JSON output (e.g., write "\\pi" instead of "\pi").
Do not include markdown formatting (like ```json). Just return the raw JSON string.

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
        validate(instance=json_req, schema=aggregation_schema)
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

    def safe_ack():
        try:
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except (AMQPError, Exception) as ack_e:
            logging.warning(f"Could not ack message (Connection likely timed out during processing): {ack_e}")
            # Note: If ack fails, RabbitMQ will redeliver the message.
            # This is acceptable (at-least-once delivery) compared to losing the job.

    try:
        request = json.loads(body)
        logging.info(f"Received request: {request}")

        if not validate_request(request):
            logging.error("Validation failed. Dropping message.")
            # We ack here so we don't retry invalid messages forever
            safe_ack()
            return

        job_id = request.get('job_id')
        summaries = request.get('summaries', [])
        flashcards = request.get('flashcards', [])

        # Combine summaries into one text block
        combined_text = "\n\n".join(summaries)

        logging.info(f"Generating final summary and flashcards for Job {job_id}...")
        status = "unknown"
        final_summary = ""
        final_flashcards = []

        try:
            gemini_client = GeminiClient()
        except Exception as e:
            logging.error(f"Failed to instantiate GeminiClient: {e}")
            # If we can't start the client, we probably want to retry later (nack) or fail.
            # For now, we return and let the ack happen (or not), effectively dropping it to avoid loop.
            safe_ack()
            return

        # ---------------------------------------------------------
        # 1. Generate Final Summary
        # ---------------------------------------------------------
        max_attempts = 3
        last_error = None

        for attempt in range(max_attempts):
            try:
                current_model = DEFAULT_MODEL
                if attempt == 2:
                    current_model = FALLBACK_MODEL

                feedback_str = ""
                if last_error:
                    feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE OUTPUT."
                    logging.info(f"Summary Aggregation Retry with error: {last_error}")

                formatted_prompt = AGGREGATION_PROMPT.format(text=combined_text, feedback=feedback_str)
                final_summary = gemini_client.generate_content(formatted_prompt, model_name=current_model)

                if final_summary and final_summary.startswith("Error"):
                    raise Exception(final_summary)

                status = "success"
                break

            except Exception as api_error:
                last_error = str(api_error)
                logging.error(f"Gemini API Error during Summary Aggregation (Attempt {attempt + 1}): {last_error}")
                if attempt == max_attempts - 1:
                    status = "failed"
                    final_summary = "Error generating final summary."
                else:
                    time.sleep(2)

        # ---------------------------------------------------------
        # 2. Generate Final Flashcards
        # ---------------------------------------------------------
        if status == "success" and flashcards:
            flashcards_json_str = json.dumps(flashcards)
            last_error = None

            for attempt in range(max_attempts):
                try:
                    current_model = DEFAULT_MODEL
                    if attempt == 2:
                        current_model = FALLBACK_MODEL

                    feedback_str = ""
                    if last_error:
                        feedback_str = f"PREVIOUS ATTEMPT FAILED. ERROR: {last_error}. PLEASE FIX THE JSON STRUCTURE."
                        logging.info(f"Flashcard Aggregation Retry with error: {last_error}")

                    formatted_prompt = FLASHCARD_AGGREGATION_PROMPT.format(text=flashcards_json_str, feedback=feedback_str)
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
                            # We don't fail the whole job if just flashcards fail, but we log it
                            logging.error("Failed to parse aggregated flashcards.")

                except Exception as api_or_json_error:
                    last_error = str(api_or_json_error)
                    logging.error(f"Gemini API/Parsing Error during Flashcard Aggregation (Attempt {attempt + 1}): {last_error}")
                    time.sleep(2)

        result_payload = {
            "final_summary": final_summary,
            "final_flashcards": final_flashcards,
            "duration": time.time() - start_time
        }

        # ---------------------------------------------------------
        # 3. Publish Result (Using Fresh Connection)
        # ---------------------------------------------------------
        try:
            # We open a NEW connection just for publishing to avoid 'ConnectionResetError'
            # if the original consumer connection 'ch' timed out during the long AI processing.
            pub_channel = create_rabbit_con_and_return_channel()

            publisher = ResultPublisher(pub_channel)
            publisher.publish_aggregation_result(
                payload=result_payload,
                original_job_id=job_id,
                status=status
            )

            # Critical: Close the temporary connection
            if pub_channel.connection and pub_channel.connection.is_open:
                pub_channel.connection.close()

            logging.info(f"Job {job_id} completed. Status: {status}")

        except Exception as pub_error:
            logging.error(f"Failed to publish result for Job {job_id}: {pub_error}")
            # If publishing fails, we probably shouldn't Ack, so RabbitMQ retries the whole job.
            return

        # ---------------------------------------------------------
        # 4. Acknowledge Original Message
        # ---------------------------------------------------------
        safe_ack()

    except json.JSONDecodeError:
        logging.error("Failed to decode JSON body")
        safe_ack()  # Remove bad JSON from queue
    except Exception as e:
        logging.error(f"Unexpected error: {e}")
        # In case of unexpected crash, we generally do NOT ack so it retries.
        # But if you want to discard poison messages, you might Ack here.
        # safe_ack()


def main():
    logging.info("Aggregation Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    queue_name = rabbitConfig.queue_aggregation_job

    # CHANGE: auto_ack set to False so we don't lose jobs if the worker crashes
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