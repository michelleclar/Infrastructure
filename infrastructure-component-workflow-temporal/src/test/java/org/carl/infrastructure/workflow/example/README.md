# 工作流引擎快速启动示例

本目录包含快速体验工作流引擎的示例代码。

## 🚀 快速开始

### 第一步：启动 Worker

在 IDEA 中右键运行 `QuickStartWorker.java`，或者在终端执行：

```bash
cd infrastructure-component-workflow-temporal
./gradlew test --tests QuickStartWorker
```

Worker 会持续运行并监听工作流任务。

### 第二步：启动客户端

在 IDEA 中右键运行 `QuickStartClient.java`（确保 Worker 已启动），或者在终端执行：

```bash
./gradlew test --tests QuickStartClient
```

客户端会：
1. 创建并启动一个工作流实例
2. 等待 2 秒让工作流到达审批节点
3. 发送审批信号
4. 等待工作流完成并显示结果

## 📊 查看执行状态

### Temporal UI
访问 http://180.184.66.147:31733 查看：
- 工作流执行历史
- 节点执行时间线
- 变量变化过程

### 数据库查询
```sql
-- 查看工作流实例
SELECT workflow_id, status, final_node_id, started_at, ended_at 
FROM workflow_instance 
WHERE definition_id = 'quickStart' 
ORDER BY started_at DESC;

-- 查看执行记录
SELECT node_id, visit_no, outcome, status, executed_at 
FROM execution_record 
WHERE workflow_id = 'your-workflow-id' 
ORDER BY executed_at;
```

## 📝 示例说明

### QuickStartWorker.java
- 连接到 Temporal 服务器
- 注册内置节点处理器
- 注册业务 Activities（createLeaveRequest, notifyManager）
- 启用数据库归档
- 启动 Worker 并监听任务队列

### QuickStartClient.java
- 创建工作流定义（使用简洁的 FlowDef DSL）
- 启动工作流实例
- 发送审批信号
- 等待并显示结果

## 🔧 自定义测试

你可以修改这些类来测试自己的业务逻辑：

### 修改工作流定义
编辑 `QuickStartClient.java` 中的 `createSimpleWorkflow()` 方法。

### 添加新的 Activities
编辑 `QuickStartWorker.java` 中的 activityRegistry.register() 调用。

### 修改数据库连接
修改两个类中的 DB_URL, DB_USER, DB_PASSWORD 常量。

## 💡 提示

- 确保 Temporal 和 PostgreSQL 服务已启动
- 先启动 Worker，再启动 Client
- Worker 可以处理多个工作流实例
- 可以运行多个 Client 来测试并发场景
