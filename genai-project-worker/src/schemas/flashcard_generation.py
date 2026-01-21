schema = {
    "type": "object",
    "required": ["job_id", "summary_chunk_id", "text"],
    "properties": {
        "job_id": {
            "type": "integer",
            "minimum": 0
        },
        "summary_chunk_id": {
            "type": "integer",
            "minimum": 0
        },
        "text": {
            "type": "string",
            "minLength": 1
        }
    }
}