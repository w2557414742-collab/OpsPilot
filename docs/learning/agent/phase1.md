# Python 诊断服务源码练习

从 api.create_app 的 lifespan 追到 work_once。沿着 Store.submit、claim、finish 找到原子提交的事件流。用 test_runs.py 的过期租约用例比较可重放确定性步骤和远程调用结果未知时的处理。
