# worker_python/messaging/message_model.py
from dataclasses import dataclass
from typing import Any, Dict
import json
import uuid
from datetime import datetime, timezone

#feel free to change this model as you wish/need
@dataclass
class BaseMessage:
    type: str # could be used to identify the message, with preprocessing, etc
    status: str # probably unnecessary, can be deleted later on
    job_id: str
    status: str
    payload: Dict[str, Any]

    def to_json(self) -> bytes:
        return json.dumps({
            "type": self.type,
            "job_id": self.job_id,
            "status": self.status,
            "payload": self.payload, #TODO payload bei workern abchecken
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }).encode("utf-8")

    @staticmethod
    def from_json(body: bytes) -> "BaseMessage":
        data = json.loads(body.decode("utf-8"))
        return BaseMessage(
            type=data.get("type", "unknown"),
            status=data.get("status", "unknown"),
            job_id=data.get("job_id", str(uuid.uuid4())),
            payload=data.get("payload", {}),
        )

    def to_dict(self) -> Dict[str, Any]:
        return {
            "type": self.type,
            "status": self.status,
            "job_id": self.job_id,
            "payload": self.payload,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }