# OpsPilot GitHub 发布报告

发布日为 2026-11-24（北京时间）。

| 验收项 | 结果 |
| --- | --- |
| GitHub 仓库 | [w2557414742-collab/OpsPilot](https://github.com/w2557414742-collab/OpsPilot) |
| 归属与权限 | `w2557414742-collab`，发布时已核实 ADMIN 和 push 权限 |
| 可见性与分支 | Private，默认分支及推送分支均为 `main` |
| 同名库检查 | 已认证的查询返回 HTTP 404。创建后重新确认可见性、身份和空仓库状态 |
| Git 远程 | `origin` 指向 `https://github.com/w2557414742-collab/OpsPilot.git`，本地 `main` 跟踪 `origin/main` |
| 首次发布提交 | [4559a2f8e5033f3178e9707e35592aedf9167ac2](https://github.com/w2557414742-collab/OpsPilot/commit/4559a2f8e5033f3178e9707e35592aedf9167ac2)，本地 HEAD、Git 远程 ref 与 GitHub API 一致 |
| 文件完整性 | 比对 GitHub 递归树的全部 106 个文件路径、模式与对象 SHA。树 SHA 为 `014396dbd4775a51fa7bc6bf2d1dde9090f0173b`，最大 blob 为 59,811 B |
| README | GitHub 返回的 README blob 与本地一致，并成功返回渲染后的 HTML |
| 密钥与大文件 | 首次推送前扫描 5 个提交、141 个本地 Git blob 与 106 个文件。0 个凭证命中，0 个达到 50 MiB 的历史 blob。[安全核验](SECURITY_AUDIT.md) · [容量审计](PACKAGE_AUDIT.md) |
| 首次推送 Actions | [Verify OpsPilot #45419789126](https://github.com/w2557414742-collab/OpsPilot/actions/runs/45419789126) 于北京时间 22:59:13 完成，结论为 `success` |
| 当前发布记录 | [main 提交](https://github.com/w2557414742-collab/OpsPilot/commits/main/) · [main 的 Actions](https://github.com/w2557414742-collab/OpsPilot/actions/workflows/verify.yml?query=branch%3Amain) |

## 发布与复核记录

首次推送保留了原有 5 个提交。GitHub API 走已验证的直连。Git HTTPS 推送及 ref 核对使用本机代理、CLI 钥匙串凭据和有效的 TLS 校验。连接参数只作用于发布进程。

首次 Actions 的 Java 构建与 JUnit、Python 依赖安装与 pytest、MySQL／Redis 端到端测试、验收工件上传及容器停止步骤均成功。

首次推送后的记录以单独的文档提交归档。上表保存首次远程验收的固定 SHA。文档提交自己的 SHA 以 Git 的 `HEAD` 和远程 `main` 为准。

```powershell
git rev-parse HEAD
git ls-remote origin refs/heads/main
gh repo view w2557414742-collab/OpsPilot --json nameWithOwner,visibility,defaultBranchRef,url
gh run list --repo w2557414742-collab/OpsPilot --branch main --workflow verify.yml
```

项目能力、已知问题与后续阶段见 [项目状态](PROJECT_STATUS.md) 和 [ChatGPT 交接](CHATGPT_HANDOFF.md)。
