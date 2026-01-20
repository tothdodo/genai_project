schema = {
    "type": "object",
    "required": ["job_id", "category_item_id", "summaries", "flashcards"],
    "properties": {
        "job_id": {
            "type": "integer",
            "minimum": 0
        },
        "category_item_id": {
            "type": "integer",
            "minimum": 0
        },
        "summaries": {
            "type": "array",
            "items": {
                "type": "string"
            },
            "minItems": 1
        },
        "flashcards": {
            "type": "array",
            "items": {
                "type": "object",
                "required": ["question", "answer"],
                "properties": {
                    "question": {"type": "string"},
                    "answer": {"type": "string"}
                }
            }
        }
    }
}