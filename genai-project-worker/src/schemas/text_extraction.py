schema = {
    "type": "object",
    "required": ["jobId", "categoryItemId", "file"],
    "properties": {
        "jobId": {
            "type": "number"
        },
        "categoryItemId": {
            "type": "number"
        },
        "file": {
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
    },
    "additionalProperties": False
}