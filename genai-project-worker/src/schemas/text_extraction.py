schema = {
    "type": "object",
    "required": ["jobId", "categoryItemId"],
    "properties": {
        "jobId": {
            "type": "string",
            "minLength": 1
        },
        "categoryItemId": {
            "type": "number"
        }
    },
    "additionalProperties": False
}
