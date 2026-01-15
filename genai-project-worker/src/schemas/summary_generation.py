schema = {
    "type": "object",
    "required": ["job_id", "category_id", "text"],
    "properties": {
        "job_id": {
            "type": "string",
            "minLength": 1
        },
        "category_id": {
            "type": "integer",
            "minimum": 1
        },
        "text": {
            "type": "string",
            "minLength": 1
        },
        "chunk_number": {
            "type": "integer",
            "minimum": 1
        }
    }
}