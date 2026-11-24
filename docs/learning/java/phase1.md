# Java 工单与队列源码练习

读 IncidentService.requestDiagnosis 和 OpsStore.byKey。分析为什么先查已有幂等键，再对比当前工单版本。随后用 scripts/verify_phase1.py 观察重复请求返回相同任务 UUID。

读 TaskWorker.finish 和 OpsStore.settle。比较 worker 的租约令牌与业务工单版本，分别说明它们约束的写入。对照迟到任务测试中的 TASK_RESULT_STALE。
