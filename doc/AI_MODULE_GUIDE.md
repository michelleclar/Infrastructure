# Infrastructure 模块使用指南

> 提供各模块的 API 签名、用法示例和编码约定。
> 模块总览和完成度见 `README.MD`。

---

## 项目概况

- **构建系统**: Gradle (Kotlin DSL)，Java 21
- **基础包名**: `org.carl.infrastructure.*`
- **仓库地址**: Aliyun Maven

### 架构分层

```
infrastructure-component-quarkus/     # Quarkus 集成模块 — 依赖 Quarkus 框架
 ├── authorization/                   # OIDC/Keycloak 认证
 ├── web/                             # RESTful 服务
 ├── persistence/                     # 数据持久化（Quarkus 集成）
 ├── mq/                              # 消息队列（Quarkus 集成）
 ├── cache/                           # 缓存
 ├── discover/                        # 服务发现
 ├── workflow/                        # 工作流 (Temporal)
 ├── metrics/                         # 监控 (OpenTelemetry)
 ├── broadcast/                       # 消息广播
 ├── search/                          # 全文搜索
 ├── approval/                        # 审批流程
 └── user/                            # 用户管理

infrastructure-component-dto/         # 数据传输对象基类（无依赖）
infrastructure-component-log/         # 统一日志接口（无 Quarkus 依赖）
infrastructure-component-utils/       # 通用工具类（无 Quarkus 依赖）
infrastructure-component-persistence-jooq/  # JOOQ 数据库操作（无 Quarkus 依赖）
infrastructure-component-redis/       # Redis 客户端（无 Quarkus 依赖）
infrastructure-component-mq-api/      # 消息队列抽象接口（无 Quarkus 依赖）
infrastructure-component-mq-pulsar/   # Pulsar 实现（无 Quarkus 依赖）
infrastructure-component-statemachine/ # 状态机（无依赖）
infrastructure-component-rule-engine/ # 规则引擎（无依赖）
infrastructure-component-pdp/         # 策略决策点（无依赖）
infrastructure-component-qdrant-grpc/ # Qdrant 向量库 gRPC
infrastructure-component-embedding-grpc/ # Embedding gRPC
infrastructure-component-audit/       # 审计日志
```

> **重要**: 独立库模块**不依赖 Quarkus**，可在任意 Java 项目中使用。
> 只有 `infrastructure-component-quarkus/` 下的模块才依赖 Quarkus 框架。

---

## 独立库模块（无 Quarkus 依赖）

### 1. DTO — 数据传输对象

**包**: `org.carl.infrastructure.dto`
**依赖**: 无

#### 基类体系

```
DTO (abstract, Serializable)
 └── Command (abstract, 用于 CUD 操作)
      └── Query (abstract, 用于查询操作)
```

#### 响应类型

```java
// 单条数据响应
SingleEntityResponse<User> resp = SingleEntityResponse.of(user);
resp.setData(user);
resp.isSuccess();

// 多条数据响应
MultiEntityResponse<User> resp = MultiEntityResponse.of(userList);
resp.isEmpty();
resp.isNotEmpty();

// 分页响应
PageEntityResponse<User> resp = PageEntityResponse.of(data, totalCount, pageSize, pageIndex);
resp.getTotalPages();
```

#### 分页查询

```java
public class UserQuery extends PageQuery {
    private String name;
    // pageSize, pageIndex, orderBy, orderDirection 已内置
    // getOffset() 自动计算偏移量
}
```

#### 使用约定

- 所有 Command/Query **必须继承** `Command` 或 `Query`
- 所有 API 响应**必须使用** `SingleEntityResponse` / `MultiEntityResponse` / `PageEntityResponse`
- 构建成功响应: `XxxResponse.buildSuccess()`
- 构建失败响应: `XxxResponse.buildFailure(errCode, errMessage)`

---

### 2. Log — 统一日志

**包**: `org.carl.infrastructure.logging`
**依赖**: SLF4J, JBoss Logging

#### 核心 API

```java
// 获取 Logger
ILogger logger = LoggerFactory.getLogger(MyService.class);
ILogger logger = LoggerFactory.getLogger("module.name");

// 日志输出
logger.trace("message");
logger.debug("value: {}", value);
logger.info("message");
logger.warn("warning: {}", reason);
logger.error("error occurred", exception);

// 条件检查
if (logger.isDebugEnabled()) { ... }
```

#### 使用约定

- **禁止**直接使用 `org.slf4j.Logger` 或 `org.jboss.logging.Logger`
- 统一通过 `LoggerFactory.getLogger()` 获取 `ILogger` 实例
- 框架自动检测 SLF4J / JBoss Logging，无需手动配置

---

### 3. Persistence (JOOQ) — 数据库操作

**包**: `org.carl.infrastructure.persistence`
**依赖**: JOOQ, PostgreSQL

#### 核心 API

```java
// 创建上下文
PersistenceContext ctx = PersistenceContext.create(dataSource);

// 查询（返回结果）
List<Record> records = ctx.get(dsl -> dsl.selectFrom(TABLE).fetch());

// 执行（无返回）
ctx.run(dsl -> dsl.insertInto(TABLE).columns(...).values(...).execute());

// 带连接的操作
ctx.connection(conn -> { /* JDBC 操作 */ });
User user = ctx.connectionResult(conn -> { return ...; });
```

#### 实现 IPersistenceOperations

```java
public class UserRepository implements IPersistenceOperations {

    // 通过 dsl() 获取 PersistenceContext
    public List<User> findAll() {
        return dsl().get(dsl -> dsl.selectFrom(USERS).fetchInto(User.class));
    }

    public void insert(User user) {
        dsl().run(dsl -> dsl.insertInto(USERS)
            .columns(USERS.NAME, USERS.EMAIL)
            .values(user.getName(), user.getEmail())
            .execute());
    }

    // 获取表结构元数据
    public Map<String, DBColumn> getColumns() {
        return getColumnMap("public", "users");
    }
}
```

#### 使用约定

- 使用 `PersistenceContext` 包装所有 JOOQ 操作
- 通过 `dsl().get()` 执行查询，`dsl().run()` 执行更新
- 不直接使用 `DSL.using()`，统一使用 `DslContextFactory.create()`
- 数据库方言: PostgreSQL

---

### 4. Redis — 缓存与分布式锁

**包**: `org.carl.infrastructure.redis.factory`
**依赖**: Vert.x Redis

#### 创建客户端

```java
// 默认配置（从环境变量读取）
RedisClient client = RedisClientFactory.create();

// 自定义配置
RedisConfigOptions options = new RedisConfigOptions()
    .setConnectionString("redis://localhost:6379")
    .setPassword("secret")
    .setMaxPoolSize(16);
RedisClient client = RedisClientFactory.create(options);

// Sentinel 配置
RedisConfigOptions options = new RedisConfigOptions()
    .setConnectType(SentinelType.SENTINEL)
    .setSentinelRole(SentinelRole.MASTER)
    .setSentinelMasterName("mymaster")
    .addConnectionString("redis://sentinel1:26379")
    .addConnectionString("redis://sentinel2:26379");

// Cluster 配置
RedisConfigOptions options = new RedisConfigOptions()
    .setConnectType(SentinelType.CLUSTER)
    .addConnectionString("redis://node1:6379")
    .addConnectionString("redis://node2:6379");
```

#### 基本 CRUD

```java
// Sync（阻塞直到完成）
client.setSync("key", "value");
client.setSync("key", "value", Duration.ofSeconds(60));  // 带过期
String val = client.getSync("key");
client.delSync("key");

// Async（返回 CompletableFuture）
CompletableFuture<Void> f = client.set("key", "value");
CompletableFuture<String> f = client.get("key");
CompletableFuture<Response> f = client.del("key");

// 批量删除
client.delSync(List.of("k1", "k2", "k3"));

// TTL
Long ttl = client.pttlSync("key");  // -2=不存在, -1=无过期, >0=剩余毫秒
```

#### 泛型操作（自动 JSON 序列化）

```java
// 对象序列化存储
User user = new User("Carl");
client.setSync("user:1", user);
User result = client.getSync("user:1", User.class);

// TypeReference（复杂泛型）
List<String> list = client.getSync("key", new TypeReference<List<String>>() {});
Map<String, Object> map = client.getSync("key", new TypeReference<Map<String, Object>>() {});

// 带 Function 的 get（原始值转换）
CompletableFuture<T> f = client.get("key", raw -> parse(raw));
```

#### 原子操作

```java
// GET-OR-SET：不存在时才设置
String val = client.getOrSet("key", "default", Duration.ofMinutes(5));

// INCR with init：先初始化再递增
Long count = client.incr("counter", 1, 0);
```

#### 分布式锁

```java
RedisLock lock = client.getLock("resource:lock");
try {
    if (lock.tryLock(Duration.ofSeconds(10), Duration.ofMinutes(1))) {
        // 持有锁，执行临界区代码
    }
} finally {
    lock.unlock();
}
```

#### 前缀扫描

```java
CompletableFuture<List<String>> keys = client.keys("user:*");
```

#### 使用约定

- 所有 sync 方法内部调用 `.join()`，在异步上下文中优先使用 async 方法
- 泛型操作依赖 Jackson，可通过 `RedisConfigOptions.registerModules()` 注册自定义序列化模块
- `RedisClient` 实现 `AutoCloseable`，使用完毕需关闭
- 连接类型枚举: `SentinelType.{STANDALONE, SENTINEL, CLUSTER, REPLICATION}`
- Sentinel 角色: `SentinelRole.{MASTER, REPLICA, SENTINEL}`

---

### 5. MQ (Message Queue) — 消息队列

**抽象接口包**: `org.carl.infrastructure.mq`（`infrastructure-component-mq-api`，无依赖）
**Pulsar 实现**: `infrastructure-component-mq-pulsar`（依赖 Apache Pulsar）

#### IProducer — 消息生产者

```java
IProducer<String> producer = ...;

// 基本发送
producer.sendMessage("payload");

// 带 key 和属性
producer.sendMessage("payload", builder -> {
    builder.key("order:123")
           .properties(Map.of("priority", "high", "source", "web"));
});

// 异步发送
CompletableFuture<Message<String>> future = producer.sendMessageAsync("payload");

// 延迟发送
producer.sendDelayedMessage(
    MessageBuilder.<String>create().value("delayed payload"),
    5000  // 5 秒后投递
);

// 批量发送
producer.sendMessages(messageBuilders);

// 事务发送
producer.sendMessageInTransaction(builder);
```

#### IConsumer — 消息消费者

```java
IConsumer<String> consumer = ...;

// 同步接收
Message<String> msg = consumer.receive();
Message<String> msg = consumer.receive(5, TimeUnit.SECONDS);

// 异步接收
CompletableFuture<Message<String>> future = consumer.receiveAsync();

// 确认
consumer.acknowledge(msg);
consumer.acknowledgeAsync(msg);

// 控制
consumer.pause();
consumer.resume();
consumer.seek(timestamp);
```

#### MessageBuilder 配置项

```java
MessageBuilder.<Order>create()
    .value(order)
    .key("order:" + order.getId())
    .properties(Map.of("type", "created"))
    .eventTime(System.currentTimeMillis())
    .deliverAfter(5000);  // 延迟 5s
```

#### 使用约定

- 消息体统一使用泛型 `IProducer<T>`
- Pulsar 是当前唯一实现（`infrastructure-component-mq-pulsar`）
- 所有消息确认必须显式调用 `acknowledge`
- MQ API 模块本身无依赖，可与任意 MQ 实现对接

---

### 6. StateMachine — 状态机

**包**: `org.carl.infrastructure.statemachine`
**依赖**: 无

#### 构建状态机

`StateMachineBuilderFactory.create()` 无参，每条迁移通过 `externalTransition()` / `externalTransitions()` 独立声明，最后 `build(machineId)` 注册到全局工厂：

```java
StateMachineBuilder<OrderStatus, OrderEvent, OrderCtx> builder =
    StateMachineBuilderFactory.create();

// 单条迁移
builder.externalTransition()
    .from(OrderStatus.NEW)
    .to(OrderStatus.PAID)
    .on(OrderEvent.PAY)
    .when(ctx -> ctx.hasPayment())   // 可选条件守卫
    .perform((from, to, event, ctx) -> log.info("paid"));

// 多源迁移（fromAmong）
builder.externalTransitions()
    .fromAmong(OrderStatus.NEW, OrderStatus.PAID)
    .to(OrderStatus.CANCELLED)
    .on(OrderEvent.CANCEL)
    .when(ctx -> true)
    .perform((from, to, event, ctx) -> {});

builder.build("OrderMachine");  // 注册到全局工厂
```

#### 获取与触发

```java
// 从全局工厂获取（build 之后）
StateMachine<OrderStatus, OrderEvent, OrderCtx> machine =
    StateMachineFactory.get("OrderMachine");

// 单状态触发
OrderStatus next = machine.fireEvent(OrderStatus.NEW, OrderEvent.PAY, context);

// 并行触发（externalParallelTransition 场景）
List<OrderStatus> targets = machine.fireParallelEvent(OrderStatus.NEW, OrderEvent.PAY, context);
```

#### 可视化

```java
machine.showStateMachine();    // 打印状态机结构
machine.generatePlantUML();    // 生成 PlantUML 图
```

#### 使用约定

- 泛型参数: `<S 状态枚举, E 事件枚举, C 上下文对象>`
- `when()` 是可选条件守卫；`perform()` 是可选动作
- 同一个 machineId 只能 `build` 一次；通过 `StateMachineFactory.get(id)` 复用

---

### 7. RuleEngine — 规则引擎

**包**: `org.carl.infrastructure.ruleengine`
**依赖**: 无

```java
// 构建规则
Rule rule = new RuleBuilder()
    .name("AdultEmployedRule")
    .when(facts -> facts.get("age") >= 18 && facts.get("employed"))   // condition
    .then(facts -> facts.put("qualified", true))                       // action
    .build();

// 执行
RuleEngine engine = new DefaultRuleEngine();
Facts facts = new Facts();
facts.put("age", 25);
facts.put("employed", true);
engine.fire(rule, facts);
```

---

### 8. Utils — 工具类

**包**: `org.carl.infrastructure.utils`
**依赖**: Guava, commons-collections4

| 类 | 用途 |
|----|------|
| `StringUtils` | 字符串操作 |
| `CollectionUtils` | 集合操作 |
| `FieldsUtils` | 反射字段操作 |
| `LogUtils` | 日志辅助 |
| `UrlParser` | URL 解析 |
| `DAG` | 有向无环图 |
| `LinkedTable` | 链表结构 |

---

## Quarkus 集成模块

以下模块位于 `infrastructure-component-quarkus/` 下，**依赖 Quarkus 框架**，
作为 Quarkus 扩展使用。每个模块通过 `@IfBuildProperty` 条件注册 Bean，
启用属性见各模块说明。

---

### Authorization — 认证授权

**包**: `org.carl.infrastructure.authorization`
**依赖**: quarkus-oidc, quarkus-keycloak

#### 核心接口

```java
public interface AuthProvider {
    IUserIdentity getIdentity(String token);   // 从 JWT token 解析身份
    IUserIdentity getIdentity();               // 从当前安全上下文获取身份
    SecurityIdentity getSecurityIdentity();
    boolean hasModulePermission(ModulePermission permission);
    boolean canAccess(ResourceIPermission permission);
}

public interface IUserIdentity {
    Boolean isAnonymous();
    Map<String, Set<Permission>> getPermissions(); // key=模块名
    Set<String> getRoles();
    Boolean hasRole(String role);
    Set<UserGroup> getGroups();
    Set<UserOrganize> getOrganizes();
    Object getAttribute(String name);
    Map<String, Object> getAttributes();
}
```

#### 权限模型

```java
// 构建模块权限
ModulePermission perm = ModulePermission.ModulePermissionBuilder.create()
    .name("user.order")
    .action("view")
    .build();

// 检查权限
boolean allowed = authProvider.hasModulePermission(perm);
```

#### 模块级授权（枚举方式）

实现 `IModuleEnum` 枚举来表达权限层级，通过 `IModuleAuthorizationServiceAbility` 校验：

```java
public enum OrderPermission implements IModuleEnum {
    VIEW("order.view", 1), CREATE("order.create", 2);
    // ...
}

// 检查
boolean canView = authService.check(OrderPermission.VIEW);
```

---

### Web — RESTful 服务层

**包**: `org.carl.infrastructure.web`
**依赖**: quarkus-rest, quarkus-jackson

#### 配置

```properties
web.super-user-id=1          # 超级管理员 ID（默认 1）
web.session-timeout-hour=24  # 会话超时（默认 24h）
web.use-session=true         # 启用会话管理
```

#### 常用 API

```java
// 获取运行时用户上下文
public interface IRuntimeProvider {
    Optional<IRuntimeUser> getUser(RoutingContext context);
    ApiRequest apiRequest(RoutingContext context);
}

// 为 controller 添加自动日志和异常处理
@ControllerLogged
@Path("/orders")
public class OrderController { ... }
```

---

### Persistence — 数据持久化（Quarkus 集成）

**包**: `org.carl.infrastructure.persistence`
**依赖**: quarkus-jdbc-postgresql, quarkus-agroal
**启用**: `quarkus.plugins.persistence.enable=true`

底层使用 `infrastructure-component-persistence-jooq`，在 Quarkus 容器中通过 CDI 暴露：

```java
// 注入方式 1：直接注入 DSLContext
@Inject
DSLContext dsl;

dsl.selectFrom(USERS).where(USERS.ID.eq(id)).fetchOne();

// 注入方式 2：注入 PersistenceContext（带元数据能力）
@Inject
PersistenceContext ctx;

List<UserRecord> users = ctx.get(dsl -> dsl.selectFrom(USERS).fetch());
ctx.run(dsl -> dsl.insertInto(USERS).values(...).execute());
```

> 查询和更新的完整 API 见上方**独立库 persistence-jooq** 章节。

---

### Cache — 缓存

**包**: `org.carl.infrastructure.cache`
**依赖**: quarkus-cache, quarkus-redis
**启用**: `quarkus.cache.enable=true`

```java
public enum CacheTopic {
    SYSTEM("system", CacheType.MAP),
    // 按需扩展业务 topic
}

// 注入使用
@Inject
CacheService cacheService;

CacheContext ctx = cacheService.getCacheContext();
```

---

### Discover — 服务发现

**包**: `org.carl.infrastructure.discover`
**依赖**: consul, consul-stork

应用启停时自动注册/注销到 Consul，无需手动调用：

```properties
consul.host=localhost
consul.port=8500
quarkus.consul-config.enabled=true
quarkus.consul-config.agent.host-port=localhost:8500
quarkus.consul-config.properties-value-keys=config/${quarkus.application.name}
```

注册的服务 ID 格式：`{appName}-{httpPort}`。

---

### Workflow — 工作流（Temporal）

**包**: `org.carl.infrastructure.workflow`
**依赖**: quarkus-temporal

将状态机与 Temporal 绑定，实现持久化、可重试的状态流转：

```java
// 定义状态机
StateMachine<OrderStatus, OrderEvent, OrderCtx> machine =
    StateMachineBuilderFactory.create(...)...build();

// 通过 Temporal 触发状态流转
TXWorkflowBuilder.of(machine)
    .entityId(orderId)
    .fireEvent(currentStatus, OrderEvent.PAY, context);
// 返回 WorkflowExecution（包含 workflowId 和 runId）
```

核心工作流接口（Temporal 注解）：

```java
@WorkflowInterface
public interface ITransactionalWorkflow {
    @WorkflowMethod
    <S, E, C> S fireEvent(String machineId, S from, E event, C ctx);
}
```

---

### Broadcast — 消息广播（Vert.x EventBus）

**包**: `org.carl.infrastructure.broadcast`
**依赖**: quarkus-vertx
**启用**: `quarkus.message.enable=true`

```java
@Inject
BroadcastService broadcast;

// 发布（单向）
broadcast.publish("user.created", userData);

// 请求-响应
Uni<UserDto> result = broadcast.request("user.fetch", userId, UserDto.class);

// 订阅
broadcast.subscribe("user.created", UserData.class, data -> {
    // 处理事件
});

// 取消订阅
broadcast.unsubscribe("user.created");
```

---

### Search — 全文搜索（Elasticsearch）

**包**: `org.carl.infrastructure.search`
**依赖**: quarkus-elasticsearch

```java
@Inject
ESService esService;

ElasticsearchClient client = esService.getESContext().client;

// 索引文档
new Index<>(client, new IndexRequest.Builder<>())
    .index("users")
    .id("123")
    .document(userObj)
    .executor();

// 搜索文档
List<User> results = new Search(client, new SearchRequest.Builder())
    .index("users")
    .query(q -> q.term(t -> t.field("status").value("active")))
    .fetchOf(User.class);
```

---

### Approval — 审批流程

**包**: `org.carl.infrastructure.approval`

```java
@Inject
ApprovalService approvalService;

// 发起审批
List<ApprovalNode> nodes = List.of(
    new ApprovalNode("step1", "manager1"),
    new ApprovalNode("step2", "director")
);
long instanceId = approvalService.startProcess("biz-key-123", nodes);

// 审批通过
approvalService.approveTask(taskId, "同意");

// 驳回到上一步
approvalService.backTask(taskId, "需要修改");

// 转交他人
approvalService.transferTask(taskId, "manager2", "请代为处理");

// 查询待办/已办
List<ApprovalTask> todo = approvalService.listTodo();
List<ApprovalDoneItem> done = approvalService.listDone();
```

审批状态：`IN_PROGRESS`、`APPROVED`、`REJECTED`
任务状态：`PENDING`、`DONE`、`BACKED`

---

### User — 用户管理（Keycloak）

**包**: `org.carl.infrastructure.user`
**依赖**: quarkus-oidc, quarkus-keycloak

Keycloak 实现了 `AuthProvider`，并缓存用户身份：

```java
@Inject
KeycloakAuthProvider authProvider;

// 获取当前用户
IUserIdentity identity = authProvider.getIdentity();
boolean isAdmin = identity.hasRole("admin");
Map<String, Set<Permission>> perms = identity.getPermissions();

// 模块级鉴权
@Inject
UserAuthorizationService authService;

boolean canCreate = authService.check(OrderPermission.CREATE);
```

---

### MQ — 消息队列（Quarkus 集成）

**包**: `org.carl.infrastructure.mq`
**依赖**: quarkus-pulsar

Quarkus 应用启停时自动管理 `MQClient` 生命周期：

```java
@Inject
MQClient mqClient;

// 创建生产者
Producer<String> producer = mqClient.createProducer("topic-name");
producer.send("message");

// 创建消费者
Consumer<String> consumer = mqClient.subscribe("topic-name");
Message<String> msg = consumer.receive();
consumer.acknowledge(msg);
```

> 高层 `IProducer`/`IConsumer` API 见上方**独立库 mq-api** 章节。

---

### Metrics — 监控（占位）

**状态**: 结构存在，暂无实现。计划集成 OpenTelemetry + LGTM（Grafana/Loki/Tempo）栈。

---

## 模块启用属性速查

| 模块 | 启用属性 |
|------|----------|
| persistence | `quarkus.plugins.persistence.enable=true` |
| cache | `quarkus.cache.enable=true` |
| broadcast | `quarkus.message.enable=true` |
| 其余模块 | 默认启用（无需配置） |

---

## 编码约定速查

### 包命名

```
org.carl.infrastructure.{模块名}.{子包}
```

例如:
- `org.carl.infrastructure.dto`
- `org.carl.infrastructure.persistence.core`
- `org.carl.infrastructure.redis.factory`
- `org.carl.infrastructure.mq`

### 代码风格

- 4 空格缩进，LF 换行，UTF-8 编码
- 类名 PascalCase，方法名 camelCase
- 测试类以 `Test` 结尾，放于 `src/test/java` 同包路径下

### Commit 格式

```
:emoji: 简短描述
```

常用 emoji:
- `:sparkles:` 新功能
- `:bug:` 修复
- `:recycle:` 重构
- `:lock:` 安全相关
- `:memo:` 文档

### 依赖注入（仅 Quarkus 模块）

```java
@Inject
RedisClient redisClient;

@Inject
PersistenceContext persistenceContext;
```

> 独立库模块不使用 `@Inject`，通过工厂方法创建实例。

---

## 常见开发场景

### 场景 1：新增一个 Repository（独立库）

```java
package org.carl.infrastructure.xxx.repository;

import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.carl.infrastructure.persistence.core.PersistenceContext;

public class OrderRepository implements IPersistenceOperations {

    private PersistenceContext ctx;

    @Override
    public PersistenceContext dsl() { return ctx; }

    @Override
    public void setPersistenceContext(PersistenceContext ctx) { this.ctx = ctx; }

    public List<Order> findByUserId(Long userId) {
        return dsl().get(dsl ->
            dsl.selectFrom(ORDERS)
               .where(ORDERS.USER_ID.eq(userId))
               .fetchInto(Order.class)
        );
    }
}
```

### 场景 2：使用 Redis 缓存（独立库）

```java
// 工厂创建
RedisClient redisClient = RedisClientFactory.create();

// 缓存穿透保护
public User getUser(Long id) {
    String key = "user:" + id;
    User cached = redisClient.getSync(key, User.class);
    if (cached != null) return cached;

    User user = userRepository.findById(id);
    if (user != null) {
        redisClient.setSync(key, user, Duration.ofMinutes(30));
    }
    return user;
}
```

### 场景 3：发送 MQ 消息（独立库）

```java
IProducer<OrderCreatedEvent> producer = ...; // 通过 MQ API 创建

public void onOrderCreated(Order order) {
    producer.sendMessage(OrderCreatedEvent.from(order), builder -> {
        builder.key("order:" + order.getId())
               .properties(Map.of("eventType", "created"));
    });
}
```

### 场景 4：定义状态流转（独立库）

```java
StateMachine<String, String, Void> sm =
    StateMachineBuilderFactory.create(String.class, String.class, Void.class)
        .initial("DRAFT")
        .state("DRAFT")
            .transition().on("SUBMIT").to("REVIEW")
        .and()
        .state("REVIEW")
            .transition().on("APPROVE").to("APPROVED")
            .transition().on("REJECT").to("DRAFT")
        .and()
        .build();
```

### 场景 5：Quarkus 模块中使用 Redis（注入）

```java
@ApplicationScoped
public class UserService {

    @Inject
    RedisClient redisClient;

    public User getUser(Long id) {
        String key = "user:" + id;
        User cached = redisClient.getSync(key, User.class);
        if (cached != null) return cached;
        // ...
    }
}
```

---

## 测试约定

```java
// 使用 JUnit 5
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Redis 集成测试需要本地 Redis 服务
// 数据库测试使用 testcontainers 或本地 PostgreSQL
```

```bash
./gradlew test                                          # 测试全部
./gradlew :infrastructure-component-redis:test           # 测试单模块
```