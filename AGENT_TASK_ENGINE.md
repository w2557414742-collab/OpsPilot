# Phase 1 诊断任务引擎

1. IncidentService.requestDiagnosis 持有工单行锁，检查事务内的幂等键重放，更新工单并写入任务。
2. TaskWorker.tick 查询到期项，用条件更新取得 12 秒租约并增加 attempt。attempt 统计领取或轮询次数。
3. AgentClient.submit 在事务外提交。Python 使用同一租户与 UUID 主键，并用规范化请求摘要识别内容变化。
4. Python 工作者在 SQLite 中领取 30 秒租约，提交报告或持久化失败。确定性运行可以重新领取，可能已触发远程调用的过期运行进入 UNKNOWN_OUTCOME。
5. Java 遇到传输故障以有界退避复查，90 秒对账期限到达后进入 UNKNOWN_OUTCOME。已知 run ID 使用 GET 轮询。
6. Java 结算使用当前租约令牌与有效期，并验证工单版本。用户可以独立读任务和工单事件。
7. Java 未领取时本地取消，领取后等待 Python 确认。Python 允许取消占位，覆盖提交与取消乱序到达的情形。

Phase 1 的取消针对报告生成流程。远程模型可能已开始调用或计费，取消请求用于停止应用报告，需要按实际终态核对。人工财务对账、预算预留及面向长任务的心跳属于后续版本。
