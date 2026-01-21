schema = {
    "type": "object",
    "required": ["job_id", "category_id", "text"],
    "properties": {
        "job_id": {
            "type": "integer",
            "minimum": 0
        },
        "category_id": {
            "type": "integer",
            "minimum": 0
        },
        "text": {
            "type": "string",
            "minLength": 1
        },
        "chunk_number": {
            "type": "integer",
            "minimum": 0
        }
    }
}