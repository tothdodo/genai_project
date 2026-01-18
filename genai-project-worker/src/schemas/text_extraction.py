schema = {
    "type": "object",
    "required": ["jobId", "categoryItemId"],
    "properties": {
        "jobId": {
            "type": "number"
        },
        "categoryItemId": {
            "type": "number"
        },
        "files": {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "required": ["id", "url"],
                "properties": {
                    "id": {
                        "type": "number",
                        "minLength": 1
                    },
                    "url": {
                        "type": "string",
                        "minLength": 1
                    }
                },
                "additionalProperties": False
            }
        }

    },
    "additionalProperties": False
}
