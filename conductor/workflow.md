# Workflow

## TDD Policy

**Strict** — 实现前必须先写测试。

- Write the test first. No implementation code is merged without a corresponding test written beforehand.
- 单元测试覆盖所有公共 API 方法
- Integration tests are required for any module that interacts with external systems (DB, Redis, Pulsar, etc.)
- A task is not considered complete until all tests pass (`./gradlew test`)

## Commit Strategy

**Emoji 前缀格式**（与仓库现有约定一致，见 `AGENTS.md`）：

```
:emoji: 简短描述
```

| Emoji | 场景 |
|-------|------|
| `:sparkles:` | 新功能 |
| `:bug:` | 修复 |
| `:recycle:` | 重构 |
| `:memo:` | 文档 |
| `:white_check_mark:` | 测试 |
| `:wrench:` | 构建/依赖/工具变更 |
| `:zap:` | 性能优化 |

## Code Review Requirements

**非 trivial 变更需要 Code Review**，包括：

- 新增公共 API 或接口变更
- 中间件集成逻辑
- 安全相关代码（认证、权限）
- 跨模块依赖变更
- Reviewer must verify: tests present, API boundaries respected, no unintended Quarkus dependency leakage into standalone modules

简单的文档修正、注释更新、版本号 bump 可自审合并。

## Verification Checkpoints

**每个 Phase 完成后**需人工验证：

1. Run `./gradlew build` — build must succeed with no errors
2. Run `./gradlew test` — all tests must pass
3. 模块可正常发布到本地 Maven（`./gradlew publishToMavenLocal`）
4. 若涉及 Quarkus 模块，验证 Quarkus dev mode 启动无异常

## Task Lifecycle

```
Todo → In Progress → Review → Verified → Done
```

- **In Progress**: 开发中，测试先行
- **Review**: 代码完成，等待 Code Review（非 trivial 变更）
- **Verified**: Phase 验证通过
- **Done**: 合并主干

## Branch Strategy

- Feature branches: `feat/<scope>-<short-description>`
- Bug fix branches: `fix/<scope>-<short-description>`
- Base branch for PRs: `main`
