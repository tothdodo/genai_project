from dataclasses import dataclass
from typing import Any, Dict
import json
import uuid
from datetime import datetime, timezone

@dataclass
class BaseMessage:
    type: str
    status: str
    job_id: str
    status: str
    payload: Dict[str, Any]

    def to_json(self) -> bytes:
        return json.dumps({
            "type": self.type,
            "job_id": self.job_id,
            "status": self.status,
            "payload": self.payload,
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