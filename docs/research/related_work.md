# 参考项目与源码核查

冻结于 2026-11-24，用于设计参考。表中链接指向本次读过的源码文件。RCAEval 的数据及第三方基线需按各自条款另行核对。

| 项目 | 参考版本与完整 SHA | 上游许可 | 观察到的机制和 OpsPilot 实现方向 |
| --- | --- | --- | --- |
| OpenTelemetry Demo | 2.0.2 · 63649d6d6a59de88fb421b88c3c3a6185b6d21ad | Apache-2.0 | 服务经 Collector 转发遥测，部分服务从 flagd 读故障开关。OpsPilot 将自行实现文档处理实验系统与独立故障控制。 [Compose](https://github.com/open-telemetry/opentelemetry-demo/blob/63649d6d6a59de88fb421b88c3c3a6185b6d21ad/docker-compose.yml) |
| RCAEval | 1.12.3 · 3c3e25f3f50a511beb777117b63ead4acb8fdf5a | MIT，第三方算法见项目说明 | 运行器按案例分发基线，读取排名并调用评估器。OpsPilot 将独立实现盲评样本入口和本地实验排序。 [main.py](https://github.com/phamquiluan/RCAEval/blob/3c3e25f3f50a511beb777117b63ead4acb8fdf5a/main.py) |
| Apache SkyWalking | v10.3.0 · 05c5b245ad39efc3910fef6d0fc5f21fd9653afd | Apache-2.0 | ServiceRelation 保存有向源和目标服务标识。OpsPilot 将基于 Trace span 父子关系计算调用边。 [ServiceRelation.java](https://github.com/apache/skywalking/blob/05c5b245ad39efc3910fef6d0fc5f21fd9653afd/oap-server/server-core/src/main/java/org/apache/skywalking/oap/server/core/source/ServiceRelation.java) |
| Prometheus | v3.5.0 · 8be3a9560fbdd18a94dedec4b747c35178177202 | Apache-2.0 | queryRange 解析 start、end、step，使用范围查询引擎。OpsPilot 将限制查询窗口、样本量和允许的服务标签。 [api.go](https://github.com/prometheus/prometheus/blob/8be3a9560fbdd18a94dedec4b747c35178177202/web/api/v1/api.go) |
| Apache RocketMQ | rocketmq-all-5.3.3 · 29d90a4ff96c04c094039685a6106870c58a7e40 | Apache-2.0 | 并发消费回调处理重投。Phase 3 将独立落库业务幂等、消息版本、租约和重放记录。 [ConsumeMessageConcurrentlyService.java](https://github.com/apache/rocketmq/blob/29d90a4ff96c04c094039685a6106870c58a7e40/client/src/main/java/org/apache/rocketmq/client/impl/consumer/ConsumeMessageConcurrentlyService.java) |
| Toxiproxy | v2.12.0 · 3ccd6a79cbc6c6a72b884d295ad314b75cdf3962 | MIT | Timeout toxic阻塞 TCP 数据流。Phase 1 自有 HTTP 测试代理在收到 Python 的 202 后丢弃响应，后续可用 Toxiproxy 验证网络层故障。 [timeout.go](https://github.com/Shopify/toxiproxy/blob/3ccd6a79cbc6c6a72b884d295ad314b75cdf3962/toxics/timeout.go) |
| Testcontainers for Java | 1.21.4 · d509c81e3395215fad43971e968e638afd65f463 | MIT | MySQLContainer 形成基于映射端口的 JDBC URL。Phase 1 跨进程验收通过独立 Docker MySQL 和脚本启动器，JUnit MySQL 边界可在后续采用 Testcontainers。 [MySQLContainer.java](https://github.com/testcontainers/testcontainers-java/blob/d509c81e3395215fad43971e968e638afd65f463/modules/mysql/src/main/java/org/testcontainers/containers/MySQLContainer.java) |
| Google SRE Book | 2016 年版网络章节 · 无发布 Commit SHA | 出版物。项目仅引用，未复制段落 | 监控章节区分观测信号、告警和根因，复盘章节要求保存影响、响应及后续行动。OpsPilot 将保存可追踪的试验输入和恢复证据。 [目录](https://sre.google/sre-book/table-of-contents/) · [Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/) · [Postmortems](https://sre.google/sre-book/postmortem-culture/) |
