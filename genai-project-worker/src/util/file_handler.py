import json
import os
import logging
from io import BytesIO

import requests
from minio import Minio
from minio.error import S3Error
from requests.adapters import HTTPAdapter, Retry
from urllib.parse import urlparse
import io

BUCKET_NAME = os.environ.get("BUCKET_NAME", "basebucket")
BASE_URL = os.environ.get("BASE_URL", "http://localhost:9000/")


def minio_client():
    endpoint_url = os.environ.get("MINIO_ENDPOINT", "localhost:9000")
    access_key = os.environ.get("MINIO_ACCESS_KEY")
    secret_key = os.environ.get("MINIO_SECRET_KEY")

    logging.info(f"Connecting to Minio at {endpoint_url}")
    logging.info(f"Using access key {access_key}")
    logging.info(f"Using secret key {secret_key}")

    if not access_key or not secret_key:
        logging.warning("MINIO_ACCESS_KEY or MINIO_SECRET_KEY not set!")
        raise EnvironmentError("MINIO_ACCESS_KEY or MINIO_SECRET_KEY not set!")

    # Reads from the env vars defined in your .env file
    client = Minio(
        endpoint_url,
        access_key,
        secret_key,
        secure=False  # Set to True if using SSL/TLS
    )
    return client


def upload_file(source_file):
    client = minio_client()
    destination_file = source_file.split("/")[-1]
    found = client.bucket_exists(BUCKET_NAME)
    if not found:
        logging.warning(f"Bucket {BUCKET_NAME} does not exist")
    else:
        logging.info(f"Bucket {BUCKET_NAME} exists, proceeding to upload!")

    client.fput_object(
        BUCKET_NAME, destination_file, source_file
    )
    logging.info(f"Uploaded {source_file} to {BUCKET_NAME} as {destination_file}")


def upload_file_by_type(destination_file, data):
    ext = os.path.splitext(destination_file)[1].lower()

    handlers = {
        ".csv": lambda: upload_df_as_csv(destination_file, data),
        ".json": lambda: (
            upload_df_as_json(destination_file, data)
            if hasattr(data, "to_json")
            else upload_json(destination_file, data)
        )
    }

    if ext not in handlers:
        logging.warning(f"File type {ext} is not supported")

    return handlers[ext]()


def upload_csv(destination_file, data_buf, length):
    client = minio_client()
    client.put_object(BUCKET_NAME,
                      destination_file,
                      data_buf,
                      length=length,
                      content_type="application/csv")

    return {
        "bucket": BUCKET_NAME,
        "objectKey": destination_file,
    }


def upload_df_as_csv(destination_file, df):
    csv_bytes = df.to_csv().encode('utf-8')
    csv_buffer = BytesIO(csv_bytes)
    return upload_csv(destination_file, csv_buffer, len(csv_bytes))


def upload_json(destination_file, json_obj):
    client = minio_client()
    if isinstance(json_obj, str):
        json_bytes = json_obj.encode('utf-8')
    else:
        json_bytes = json.dumps(json_obj).encode('utf-8')

    json_buffer = BytesIO(json_bytes)
    client.put_object(
        BUCKET_NAME,
        destination_file,
        json_buffer,
        length=len(json_bytes),
        content_type="application/json"
    )
    return {
        "bucket": BUCKET_NAME,
        "objectKey": destination_file
    }


def upload_df_as_json(destination_file, df):
    client = minio_client()
    json_bytes = df.to_json(orient="records").encode('utf-8')
    json_buffer = BytesIO(json_bytes)

    client.put_object(
        BUCKET_NAME,
        destination_file,
        json_buffer,
        length=len(json_bytes),
        content_type="application/json"
    )
    return BASE_URL + destination_file


def download_file_to_memory(url: str) -> io.BytesIO:
    session = requests.Session()
    retries = Retry(
        total=5,
        backoff_factor=1,
        status_forcelist=[500, 502, 503, 504],
        allowed_methods=["GET"],
    )
    session.mount("https://", HTTPAdapter(max_retries=retries))
    session.mount("http://", HTTPAdapter(max_retries=retries))

    try:
        with session.get(url, stream=True) as r:
            r.raise_for_status()
            # Read the entire content into an in-memory byte buffer
            return io.BytesIO(r.content)
    except Exception as e:
        raise RuntimeError(f"Download failed for {url}: {e}") from e

def download_file(url: str, dest_dir: str = ".", chunk_size: int = 10 * 1024) -> str:
    os.makedirs(dest_dir, exist_ok=True)
    parsed = urlparse(url)
    local_filename = os.path.join(dest_dir, os.path.basename(parsed.path))

    session = requests.Session()
    retries = Retry(
        total=5,
        backoff_factor=1,
        status_forcelist=[500, 502, 503, 504],
        allowed_methods=["GET"],
    )
    session.mount("https://", HTTPAdapter(max_retries=retries))
    session.mount("http://", HTTPAdapter(max_retries=retries))
    try:
        with requests.get(url, stream=True) as r:
            r.raise_for_status()
            with open(local_filename, 'wb') as f:
                for chunk in r.iter_content(chunk_size=chunk_size):
                    f.write(chunk)
        return local_filename
    except Exception as e:
        if os.path.exists(local_filename):
            os.remove(local_filename)
        raise RuntimeError(f"Download failed for {url}: {e}") from e


def fetch_file(file, dest_dir: str = ".") -> str:
    os.makedirs(dest_dir, exist_ok=True)
    bucket_name = file.get("bucket")
    object_key = file.get("objectKey")

    if not bucket_name or not object_key:
        raise ValueError("File object must contain 'bucket' and 'objectKey'")
    local_filename = os.path.join(dest_dir, os.path.basename(object_key))
    client = minio_client()
    try:
        client.fget_object(
            bucket_name=bucket_name,
            object_name=object_key,
            file_path=local_filename
        )
        return local_filename
    except Exception as e:
        if os.path.exists(local_filename):
            os.remove(local_filename)
        raise RuntimeError(f"MinIO download failed for {bucket_name}/{object_key}: {e}") from e


def main():
    test_file = os.path.join(os.getcwd(), "../Pre_Process_Job_0001_output.csv")
    try:
        upload_file(test_file)
    except S3Error as e:
        logging.error("Error occurred while uploading file to Minio:  {}".format(e))


if __name__ == "__main__":
    try:
        main()
    except S3Error as e:
        print("error occurred: ", e)
