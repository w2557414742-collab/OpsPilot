# 跨语言接口源码练习

对照 AgentClient.Request 和 DiagnosisRequest，检查名称、类型与长度上限。对照 AgentProxy.forward 的第一次响应丢弃实验及 Java TaskWorker 的重试路径，定位两边记录的关联 ID。
