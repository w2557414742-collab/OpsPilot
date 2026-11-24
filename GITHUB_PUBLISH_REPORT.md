# OpsPilot GitHub 发布报告

| 验收项 | 本轮结果 |
| --- | --- |
| 本地分支 | `main` |
| 原有历史 | 保留，整理前 HEAD 为 `44f5fb2ece09b45df8b0842ce94740675812055c` |
| GitHub CLI | 便携版 2.105.0，已按官方 SHA-256 校验 |
| 登录 | BLOCKED，`gh auth status --hostname github.com` 返回未登录 |
| 远程 URL、归属、可见性 | 待登录核对，目标为用户确认的 Private OpsPilot |
| Git 远程、远程分支和 SHA | 未配置、未推送。计划从本地 `main` 推送 |
| 远程文件、README 显示及大文件 | NOT_RUN |
| GitHub Actions | NOT_RUN，工作流已加入项目 |
| 本地大小和历史检查 | [容量审计](PACKAGE_AUDIT.md) |
| 本地发布安全检查 | 发布候选与历史通过。忽略目录内一条 JDK 配置命中已核实为误报。[安全核验](SECURITY_AUDIT.md) |
| 已实现范围和源码学习 | [项目状态](PROJECT_STATUS.md) · [ChatGPT 交接](CHATGPT_HANDOFF.md) |

本地工作区保留全部源码、测试、实验归档和现有 Git 提交。发布整理只修改项目归档、启动脚本和 CI 设置。最新本地提交使用 `git rev-parse HEAD` 获取，远程仓库 HEAD 须在推送后独立核对。

## 官方登录步骤

工作区已提供校验过的 Windows 便携版。从仓库根目录在自己可见的 Windows PowerShell 中运行。

```powershell
$ghExe = (Resolve-Path '.local/release-tools/gh.exe').Path
& $ghExe --version
& $ghExe auth login --hostname github.com --web --git-protocol https
& $ghExe auth status --hostname github.com
& $ghExe api user --jq .login
```

没有便携版时，可使用官方 Windows 安装程序；存在 WinGet 时可执行 `winget install --id GitHub.cli --source winget`。登录后只需告知账号或组织名与创建、推送授权。[官方 Windows 安装](https://github.com/cli/cli/blob/trunk/docs/install_windows.md) · [官方登录说明](https://cli.github.com/manual/gh_auth_login)

## 登录和明确授权后的发布验收

1. 用 `gh api user --jq .login` 确认账号。用 `gh repo view OWNER/OpsPilot --json nameWithOwner,visibility,defaultBranchRef,url,isEmpty` 核对同名仓库。将权限错误与仓库不存在区分。
2. 用户确认创建且同名库不存在时，执行 `gh repo create OWNER/OpsPilot --private`。重新核对归属与可见性。已有同名库先比较默认分支、远程分支与历史。若历史分叉则暂停。
3. 审计待推送提交、核对远程与分支后，仅在本地尚无 origin 时执行 `git remote add origin https://github.com/OWNER/OpsPilot.git`。可用 `gh auth setup-git --hostname github.com` 配置 Git 凭据助手，再执行 `git push -u origin main`。
4. 用 `git ls-remote origin refs/heads/main` 对比本地 HEAD。通过 GitHub API 核查树和 README，通过 `gh run list --repo OWNER/OpsPilot --branch main` 检查 CI。根据真实结果更新本报告及交接文件。

GitHub 凭据通过官方登录流程存储，勿将令牌粘贴到文档、命令参数或聊天。
