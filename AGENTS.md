# OpsPilot 工作区约定

- 先完成可运行的垂直切片，在 PROJECT_STATUS.md 区分实现、验证与未运行。
- Java 持有业务真相。Python 只通过版本化 HTTP 接口接受任务，并使用独立存储。
- 数据库变更追加 Flyway 迁移。租户作用域来自认证凭据，状态变化检查版本并写审计。
- 集成测试使用隔离的本机容器或临时进程，默认 provider 为 DETERMINISTIC_TEST。
- 新实验结果保存命令、代码版本、配置与原始输出。测试夹具不可作为真实故障样本或性能指标发布。
- 复杂事务及算法使用中文 Javadoc 或 Docstring，解释恢复条件和边界。
