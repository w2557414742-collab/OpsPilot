# Python 核心函数索引

| 函数 | 职责 |
| --- | --- |
| create_app | 按 SQLite 路径、服务 token 和 provider 构建 FastAPI 应用 |
| work_once | 领取最多一项 run，调用固定 provider，尝试结算 |
| Store.submit | 通过 tenant 与 run UUID 去重，拒绝改变已有请求摘要 |
| Store.cancel | 在同一事务写取消状态及事件，处理提交迟到 |
| Store.claim | 处理过期租约，再领取最早 QUEUED run |
| Store.finish | 用租约结算结果、错误或 UNKNOWN_OUTCOME |
| configured_provider | 显式选择本地测试或外部模型兼容适配器 |
