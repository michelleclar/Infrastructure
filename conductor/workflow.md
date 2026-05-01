# Workflow

## TDD Policy

**Strict** — 实现前必须先写测试。

- 新功能和 bug fix 均需先编写失败测试，再编写实现
- PR 不得合并未通过测试的代码
- 单元测试覆盖核心逻辑；集成测试覆盖中间件交互

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

简单的文档修正、注释更新、版本号 bump 可自审合并。

## Verification Checkpoints

**每个 Phase 完成后**需人工验证：

- 所有测试通过（`./gradlew test`）
- 模块可正常发布到本地 Maven（`./gradlew publishToMavenLocal`）
- 若涉及 Quarkus 模块，验证 Quarkus dev mode 启动无异常

## Task Lifecycle

```
Todo → In Progress → Review → Verified → Done
```

- **In Progress**: 开发中，测试先行
- **Review**: 代码完成，等待 Code Review（非 trivial 变更）
- **Verified**: Phase 验证通过
- **Done**: 合并主干
