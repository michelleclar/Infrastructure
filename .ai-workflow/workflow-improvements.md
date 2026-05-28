# 工作流缺陷与改进计划

> 整理自 Phase 4 执行过程中发现的问题，优先级从高到低排列。

---

## 缺陷列表

### D1 — 子任务未关联父任务 【高】

**现象**：`/aw-create` 创建子任务时，没有在 YouTrack 写入 Subtask 链接。
打开 INFRA-17 时看不到 INFRA-18/19/20，审查者需要手动翻任务列表才能找到相关工作。

**根因**：`/aw-create` 只调用了 issue 创建接口，没有后续调用 `links/subtask` API。

**修复时机**：创建子任务后立即执行。

**改进方案**：
```
创建子任务 → POST /api/issues/{child}/links/{subtask-inward-id}
             body: {"issues": [{"idReadable": "INFRA-XX"}]}
```

---

### D2 — 子任务无提交记录 【高】

**现象**：INFRA-18/19/20 没有任何提交引用，审查者无法从任务跳转到代码。

**根因**：编排器在 `Test PASS → Fixed` 阶段没有向任务写评论。

**修复时机**：每次 Dev agent 提交代码后 + Test PASS 时各写一次。

**改进方案**：
```
Dev commit    → POST comment: 提交 SHA + 变更文件摘要 + 测试数量
Test PASS     → POST comment: 测试通过 N/N，BUILD SUCCESSFUL
Review PASS   → POST comment: 代码审查通过，进入测试阶段
```

---

### D3 — 平台状态与工作流状态语义不匹配 【高】

**现象**：YouTrack 没有 "Awaiting Review" / "Awaiting Test" 状态，
全程用 "To be discussed" 代替，审查者看到的状态无法区分"待审查"和"待测试"。

**根因**：`youtrack.json` 平台配置没有做状态映射。

**改进方案**：
在 `.ai-workflow/platforms/youtrack.json` 中增加状态映射表：

```json
"stateMap": {
  "Todo":             "Open",
  "In Progress":      "In Progress",
  "Awaiting Review":  "To be discussed",
  "Awaiting Test":    "In Progress",
  "Done":             "Fixed",
  "Verified":         "Verified",
  "Blocked":          "Incomplete"
}
```

并在评论中标注实际语义，例如：
`状态变更为 "To be discussed"（语义：Awaiting Review）`

---

### D4 — Review FAIL 后未创建 finding 子任务 【中】

**现象**：Review 失败时直接将原任务打回 Dev，没有创建独立的 finding issue。
多轮 review 的修复历史混在原任务评论里，无法单独追踪每个问题的修复状态。

**根因**：编排器 Review FAIL 分支只做了状态回退，没有走 finding 创建流程。

**改进方案**：
```
Review FAIL → 对每个 severity=error 的 finding:
              创建子任务 [Finding] <summary>，类型 Bug，关联父任务
              原任务打回 Dev，附带 finding 任务 ID 列表
Finding 修复后 → 子任务转 Fixed，父任务重新进入 Review
```

---

### D5 — 所有提交直接落到 main，无 PR 流程 【中】

**现象**：Dev agent 的每次提交都直接 commit 到 main，跳过了代码审查的标准 git 流程。
审查者无法在 PR 层面留评论，review 只存在于 blackboard 和 YouTrack 评论中。

**根因**：`codex-host` 和 `linear-autopilot:dev` 默认提交到当前分支。

**改进方案**：
```
Dev agent 在独立分支提交 → 编排器创建 PR → Review agent 审查 PR diff
Review PASS → 编排器合并 PR → 关闭分支
```
短期替代方案：保持当前模式，但在 YouTrack 评论中附上 `git diff <base>..<sha>` 链接。

---

### D6 — 缺陷任务未关联到父特性任务 【中】

**现象**：INFRA-32/33/34/35（缺陷）创建时没有挂在对应特性任务下，
在 INFRA-17/18/19/20 的任务视图里看不到已知缺陷。

**根因**：`/aw-create` 创建缺陷时只设置了 `parent` 字段（见 blackboard），
但没有调用 YouTrack links API 真正建立关联。

**改进方案**：同 D1，创建缺陷任务后立即调用 links API 挂父任务。

---

### D7 — Review 简报不够完整导致多轮循环 【低】

**现象**：INFRA-18 经历 3 轮 review，INFRA-19 经历 3 轮，
每轮都发现上一轮没有提到的问题（ObjectMapper 注入方式、toUserId 校验等）。

**根因**：Review agent 的 prompt 每轮只检查"上一轮遗留项"，
没有对整个文件做全量扫描。

**改进方案**：
- Review R1 prompt 要求全量扫描，不只看 diff
- 建立 review checklist 模板，覆盖：空值校验、ThreadLocal 清理、CDI 模式、测试覆盖度、反射注入
- 限制最大 review 轮次（建议 2 轮），超过后升级 runner 到 strong 模式

---

## 优先级汇总

| ID | 缺陷 | 优先级 | 修复位置 |
|----|------|--------|---------|
| D1 | 子任务未关联父任务 | 高 | `/aw-create` |
| D2 | 子任务无提交记录 | 高 | `/aw-work` Dev/Test 阶段 |
| D3 | 状态语义不匹配 | 高 | `youtrack.json` + 编排器状态转换 |
| D4 | Review FAIL 未创建 finding 任务 | 中 | `/aw-work` Review FAIL 分支 |
| D5 | 无 PR 流程 | 中 | `agents.json` runner 配置 |
| D6 | 缺陷未关联父任务 | 中 | `/aw-create` 缺陷创建路径 |
| D7 | Review 简报不完整 | 低 | Review agent prompt 模板 |

---

## 建议实施顺序

1. **立即可做（配置层）**：D3 — 在 `youtrack.json` 补充 stateMap，成本极低
2. **下一个 Phase 前（编排器层）**：D1 + D2 + D6 — 在 `/aw-work` 和 `/aw-create` 的平台写入步骤里加 API 调用
3. **专项优化**：D4 — finding 子任务流程，需要修改 Review FAIL 分支逻辑
4. **长期**：D5 — PR 流程，需要 git workflow 改造；D7 — review checklist 模板

---

_最后更新：2026-05-26_
