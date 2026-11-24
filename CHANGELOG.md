# 更新记录

## GitHub 发布准备 · 2026-11-24

- 保留 Phase 0–2 的原有提交、源码、测试和验收证据。
- 扩充 Git 忽略规则、配置模板、容量审计、授权状态及第三方许可记录。
- 增补 Windows、Linux 启动脚本及 GitHub Actions。远程 CI 为 NOT_RUN。
- 完善双语 README、目录树、Java 与 Python 方法参考、调用图及 ChatGPT 交接。
- 复跑 Java 和 Python 本地单元测试，扫描当前文件与 Git 历史。

## Phase 2

- 实现告警接收、逐项回执、去重、事件时间聚合和告警风暴抑制。
- 在真实 MySQL 和 Redis 上验收。源码 `86c401f`，证据 `44f5fb2`。

## Phase 0–1

- 建立跨语言独立存储、JWT 和租户隔离、工单状态机与异步诊断队列。
- 验证 HTTP 丢响应、迟到结果、取消与重启。源码 `b82dd56`。
