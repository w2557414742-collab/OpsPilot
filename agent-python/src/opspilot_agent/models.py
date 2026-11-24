"""Java 到 Python 的版本化请求及结构化诊断结果。"""

from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, StringConstraints
from uuid import UUID

Identifier = Annotated[str, StringConstraints(pattern=r"^[a-z0-9][a-z0-9_-]{0,47}$")]
ProviderName = Literal["DETERMINISTIC_TEST", "OPENAI_COMPATIBLE"]
Status = Literal["QUEUED", "RUNNING", "CANCEL_REQUESTED", "CANCELLED", "SUCCEEDED", "FAILED", "UNKNOWN_OUTCOME"]
Check = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=300)]


class DiagnosisRequest(BaseModel):
    """工单症状与关联 ID，来自 Java 服务。"""

    model_config = ConfigDict(extra="forbid")
    incidentId: UUID
    serviceId: Identifier
    title: str = Field(min_length=1, max_length=200)
    description: str = Field(min_length=1, max_length=2000)
    traceId: str = Field(pattern=r"^[0-9a-f]{32}$")


class Report(BaseModel):
    """缺少遥测阶段的结构化调查建议。"""

    model_config = ConfigDict(extra="forbid")
    provider: ProviderName
    conclusion: Literal["INSUFFICIENT_EVIDENCE"] = "INSUFFICIENT_EVIDENCE"
    evidence: list[str] = Field(default_factory=list, max_length=0)
    nextChecks: list[Check] = Field(min_length=1, max_length=5)


class Run(BaseModel):
    """SQLite 中已知的 run 状态。"""

    runId: UUID
    status: Status
    result: Report | None = None
    errorCode: str | None = None


class RunEvent(BaseModel):
    """附带单调序号的状态变更。"""

    id: int
    status: Status
    at: float
