# 实际调用流程

## 告警接收

```mermaid
sequenceDiagram
    participant S as 告警来源
    participant A as AlertApi.accept
    participant N as AlertNormalization
    participant I as AlertIngestion.accept
    participant D as MySQL
    participant R as AlertCache.project
    S->>A: POST /ingest/v1/tenant/source/format
    A->>A: 校验来源令牌与请求字节数
    A->>N: standard 或 alertmanager
    A->>I: 传入已校验事件和原始请求
    I->>D: 写原始回执并锁定分组
    I->>D: 写入或复用告警事件
    I->>D: 写逐项处理结果并推进窗口或候选
    D-->>I: 提交事务
    A->>R: 可选 Lua 计数投影
    A-->>S: 202 和回执
```

## MySQL 聚合与 Redis 投影

```mermaid
flowchart TD
    G[读取并锁定 SQL 分组] --> E[来源键去重与事件入库]
    E -->|已处理或过了窗口| A[记录回执关联]
    E -->|新事件在窗口内| W[按水位扣减过期区间并更新严重度计数]
    W --> C[检查候选阈值与冷却]
    A --> T[事务提交]
    C --> T
    T -->|配置启用| R[Redis Lua 原子更新水位和有序集合]
    R -->|不可用| M[增加缓存失败度量]
```

事件时间窗口及 SQL 查询边界见 [告警设计](docs/alerts.md)。

## 故障工单与诊断

```mermaid
sequenceDiagram
    actor S as 操作员
    participant J as OperationsApi 与 IncidentService
    participant M as MySQL
    participant W as TaskWorker 与 AgentClient
    participant P as FastAPI
    participant Q as SQLite
    S->>J: POST incidents
    J->>M: 检查服务归属并创建 OPEN
    S->>J: 带版本请求 TRIAGING
    J->>M: 条件更新工单并追加审计
    S->>J: POST diagnoses 带版本和幂等键
    J->>M: 写 DIAGNOSING、任务与流水
    J-->>S: 202 Task ID
    W->>M: 领取任务租约
    W->>P: PUT run 使用 Task ID
    P->>Q: 提交请求摘要和 QUEUED 流水
    P-->>W: 202 Run ID
    P->>Q: 后台 worker 提交结构化建议
    W->>P: GET run 查询状态
    W->>M: 按租约和工单版本结算
    S->>J: GET task 和 incident events
```

第一次 PUT 响应丢失时，后续请求仍携带原租户、UUID 和请求体。取消请求根据 Java 任务状态在本地确认或发送 Python 取消端点。Python 确认后落库终态。[工单状态机](INCIDENT_STATE_MACHINE.md) · [任务恢复机制](AGENT_TASK_ENGINE.md)

## Docker 部署关系

```mermaid
flowchart LR
    subgraph Host[本机进程]
        J[Java 服务]
        P[Python Agent]
        DB[(Python SQLite 本地文件)]
    end
    subgraph Docker[Compose 项目]
        M[(MySQL 容器与持久卷)]
        R[(可选 alert-cache Redis 容器)]
    end
    J -->|HTTP 8000| P
    P --> DB
    J -->|本机端口 13316| M
    J -. 本机端口 16379 .-> R
```

## 后续流程

| 需求 | 状态 | 当前可追踪入口 |
| --- | --- | --- |
| RocketMQ、Outbox 与 Inbox | NOT_IMPLEMENTED | TaskWorker 当前从 MySQL 调度 |
| LangGraph 工作流及 MCP | NOT_IMPLEMENTED | Python work_once 调用固定 provider |
| 根因排序算法 | NOT_IMPLEMENTED | Report 返回证据不足和待检查项 |
| 审批、处置与恢复验证 | NOT_IMPLEMENTED | 状态枚举包含后续转换，公开入口只执行目前实现的流程 |
