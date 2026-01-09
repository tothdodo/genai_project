schema = {
    "type": "object",
    "required": ["jobId", "file", "fileId"],
    "properties": {
        "jobId": {
            "type": "string",
            "minLength": 1
        },
        "presignedUploadUrl": {
            "type": "string",
            "minLength": 1
        }
        # "file": {
        #     "type": "object",
        #     "required": ["bucket", "objectKey"],
        #     "properties": {
        #         "bucket": {"type": "string"},
        #         "objectKey": {"type": "string"}
        #     }
        # },
        # "fileId": {
        #     "type": "integer",
        #     "minimum": 1
        # }
    },
    "additionalProperties": False
}