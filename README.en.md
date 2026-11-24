# OpsPilot

OpsPilot is an AIOps and incident response project with Phase 1 and Phase 2 verified locally. Java owns tenant-scoped alerts, incidents, audit events and the durable database task queue. Python runs asynchronous diagnosis over HTTP and persists run state independently in SQLite.

Integration tests cover lost submission responses, replay, late diagnosis, process restarts, tenant isolation, alert window eviction and Redis failure. The default deterministic provider returns investigation suggestions and reports insufficient evidence. Live model runs, RCA accuracy and performance benchmarks are pending.

See the [Windows guide](WINDOWS_QUICKSTART.md), [Linux guide](docs/linux-quickstart.md), [test evidence](TEST_REPORT.md) and [Chinese README](README.md).
