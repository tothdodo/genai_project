import logging
import signal
import sys
import time
import json
import argparse
import fitz
import io
from jsonschema.exceptions import ValidationError
from jsonschema.validators import validate
from pika.exceptions import ChannelWrongStateError, ReentrancyError, StreamLostError
import util.file_handler as file_handler
import os
from pathlib import Path

from messaging.rabbit_config import get_rabbitmq_config
from messaging.rabbit_connect import create_rabbit_con_and_return_channel
from schemas.text_extraction import schema as text_extraction_schema
from messaging.message_model import BaseMessage
from messaging.result_publisher import ResultPublisher
from requests.exceptions import HTTPError

# Save chunks into data folder for local testing
BASE_DIR = Path("/data/text_chunks")

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
    publisher.publish_text_extraction_result(msg.payload)
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


def mk_success_msg(job_id: str, payload: dict):
    return BaseMessage(
        type = "metadata",
        job_id = job_id,
        status = "success",
        payload= payload
    )


def chunk_text(text):
    chunks = []
    # We use a list of separators from most desirable (paragraph) to least (space)
    separators = ["\n\n", "\n", ". ", " "]

    current_pos = 0
    text_len = len(text)

    min_chunk_size = int(os.environ.get("TEXT_CHUNK_CHARACTER_SIZE", "1000"))

    while current_pos < text_len:
        # If remaining text is smaller than limit, take it all
        if text_len - current_pos <= min_chunk_size:
            chunks.append(text[current_pos:].strip())
            break

        # Define the search window: start at the minimum limit
        search_start = current_pos + min_chunk_size
        chunk_end = -1

        # Try to find the best separator near the 1000 char mark
        for sep in separators:
            # We look for the first occurrence of a separator AFTER the min_chunk_size
            found_pos = text.find(sep, search_start)
            if found_pos != -1:
                # We add the length of the separator to include it in the current chunk
                chunk_end = found_pos + len(sep)
                break

        # Fallback: If no separator is found in the remaining text, take the rest
        if chunk_end == -1:
            chunks.append(text[current_pos:].strip())
            break

        chunks.append(text[current_pos:chunk_end].strip())
        current_pos = chunk_end

    return chunks


def extract_pdf_content(pdf_stream: io.BytesIO):
    text_content = []
    full_image_list = []

    doc = fitz.open(stream=pdf_stream.getvalue(), filetype="pdf")

    for page_num, page in enumerate(doc):
        # Extract text using "blocks" to better preserve paragraph structures
        page_text = page.get_text("text")
        text_content.append(page_text)

        # Image extraction
        image_list = page.get_images(full=True)
        for img_index, img in enumerate(image_list):
            xref = img[0]
            base_image = doc.extract_image(xref)
            full_image_list.append({
                "page": page_num + 1,
                "extension": base_image["ext"],
                "image_data": base_image["image"]
            })

    doc.close()

    # Join all text and then chunk it contextually
    complete_text = "\n".join(text_content)
    text_chunks = chunk_text(complete_text)

    return text_chunks, full_image_list


def process_req(ch, method, properties, body):
    start_time = time.time()

    req = json.loads(body)
    job_id = req["jobId"]
    if not validate_request(req):
        logging.warning("The text extraction job is cancelled because of a Validation Error")
        publish_response(ch, mk_error_msg(job_id, "Text extraction job is cancelled because job request is invalid"))
        return
    try:
        request = json.loads(body)
        logging.info(f"Start time: {start_time}")
        logging.info("Received request: {}".format(request))

        # 1. Access the list of files
        files_list = request.get("files", [])
        category_item_id = str(request.get("categoryItemId", "unknown_category"))

        # 2. Loop through each file in the list
        for file_entry in files_list:
            file_url = file_entry.get("url")
            file_id = str(file_entry.get("id", "unknown_file"))

            if not file_url:
                logging.warning("Skipping an entry with no URL.")
                continue

            try:
                # 3. Download and process the specific file
                local_file = file_handler.download_file_to_memory(file_url)

                if not local_file:
                    logging.warning(f"File at {file_url} is empty, skipping.")
                    continue

                # 4. Extract content
                text_chunks, images = extract_pdf_content(local_file)

                # Create the path: base/categoryId/fileId
                target_folder = BASE_DIR / str(category_item_id) / str(file_id)

                # Create the directories (exist_ok=True prevents errors if they already exist)
                # parents=True creates the category folder if it doesn't exist yet
                target_folder.mkdir(parents=True, exist_ok=True)

                # Loop through chunks and save as text files
                for index, chunk in enumerate(text_chunks):
                    file_path = target_folder / f"chunk_{index}.txt"

                    with open(file_path.absolute(), "w", encoding="utf-8") as f:
                        f.write(chunk)

                logging.info(f"Saved {len(text_chunks)} chunks to {target_folder.absolute()}")

                # --- Continue with your logic for saving/using 'text' and 'images' here ---
                publish_response(ch, BaseMessage(type="text_extraction", job_id=job_id, status="success",
                                                 payload={
                                                     "textChunks": text_chunks,
                                                     "fileId": file_id,
                                                     "categoryItemId": category_item_id,
                                                     # Todo: Do we need these?
                                                     "pageStart": 0,
                                                     "pageEnd": 0
                                                 }))
            except HTTPError as e:
                logging.warning("Couldn't download file from: {}, error: {}".format(file_url, e))
                # Depending on requirements, you might 'continue' to the next file
                # or 'return' to fail the whole job.
                publish_response(ch, mk_error_msg(job_id, f"Error downloading {file_url}: {e}"))
                continue

    except Exception as e:
        logging.error(f"Failed to process message: {e}")
        publish_response(ch, mk_error_msg(job_id, "An unexpected error occured, text_extraction job cancelled"))
    finally:
        processing_time = int((time.time() - start_time) * 1000)
        logging.info(f"Worker took {processing_time} ms to process the message.")


def main():
    # logging.info(f"Connected to RabbitMQ Listening on queue '{rabbitConfig.queue_comparison_job}'")
    logging.info("Text Extraction Worker Started!")
    channel = connect_rabbitmq()

    def callback(ch, method, properties, body):
        process_req(ch, method, properties, body)

    # Using the queue name from definitions.json
    queue_name = "worker.text.extraction.job"

    # Ensure this matches your rabbit_config.queue_summary_generation_job
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)

    logging.info(f"Waiting for messages on {queue_name}")
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
