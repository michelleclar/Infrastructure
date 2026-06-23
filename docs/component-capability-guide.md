# 基础设施组件能力总览

更新时间：2026-06-15

本文档用于快速判断本仓库每个组件能做什么、如何使用、使用时要注意什么。组件清单以 `settings.gradle.kts` 中的 `include(...)` 为准；`infrastructure-component-quarkus` 是 Quarkus 适配层父项目，不作为单独业务能力组件使用。

## 依据来源

- `settings.gradle.kts`
- `README.MD`
- `docs/components/README.md`
- `docs/components/modules/*.md`
- `docs/quarkus-decoupling/README.md`
- `docs/quarkus-decoupling/modules/*.md`
- `infrastructure-component-workflow-core/README.md`
- `infrastructure-component-workflow-temporal/README.md`

## 通用规则

- 项目是 Java 21 多模块 Gradle 基础设施组件库，不是可运行应用。
- 组件能力通过 ability/mixin 接口暴露，例如 `IPersistenceAbility`、`ILogAbility`、`ISearchAbility`、`IBroadcastAbility`。
- Quarkus 子模块负责 CDI Bean、配置、生命周期、事务、JAX-RS、Quarkus 扩展 API 等适配；可复用 core/API/implementation 应放在顶层 `infrastructure-component-*` 模块。
- 顶层 core 模块不得依赖 Quarkus、CDI、JAX-RS、MicroProfile Config、SmallRye Config。
- 日志统一使用 `org.carl.infrastructure.logging.ILogger` 和 `LoggerFactory`。
- `src/main/gen/` 或生成源码目录禁止手动编辑。
- Redis、数据库、Temporal、Pulsar、Elasticsearch、Consul、Qdrant、Embedding 等外部服务相关测试需要本地或远端服务；测试应通过环境变量或 `assumeTrue()` 处理缺失环境。

## 顶层组件

| 组件 | 能力 | 如何使用 | 注意事项 |
|------|------|----------|----------|
| `infrastructure-component-dto` | 提供 `Command`、`Query`、`DTO`、`PageQuery`、`SingleEntityResponse`、`MultiEntityResponse`、`PageEntityResponse` 等基础 DTO 和响应包装。 | 业务命令继承 `Command`，查询对象继承 `Query` 或 `PageQuery`，接口返回值使用 `SingleEntityResponse.of(data)`、`MultiEntityResponse.of(list)`、`PageEntityResponse.of(...)`。 | 不引入 Web、JSON、数据库、MQ 或业务字段；分页默认值、`orderBy`、`getOffset()` 是对外契约。 |
| `infrastructure-component-log` | 提供统一日志接口 `ILogger`，适配 SLF4J 与 JBoss Logging。 | 使用 `LoggerFactory.getLogger(MyClass.class)` 获取 `ILogger`，通过 `info`、`warn`、`error` 等方法输出日志；能力类可实现 `ILogAbility`。 | 禁止业务和组件直接使用 SLF4J/JBoss Logger；新增日志方法时同步两个 adapter。 |
| `infrastructure-component-utils` | 提供字符串、集合、字段反射、URL 解析、日志辅助、脱敏算法、`DAG`、`LinkedTable`、AES-GCM、SHA-256、JSONB/Jackson、UTC 时间和分页归一化。 | 直接调用 `StringUtils`、`CollectionUtils`、`FieldsUtils`、`UrlParser`；脱敏使用 `IDesensitizationAlgorithm`；加密用 `AesGcmStringCipher`；hash 用 `Sha256`；JSONB 用 `JsonbConverter`；分页用 `Pagination.normalize(...)`。 | 工具类保持小而稳定；新增工具前确认是否属于更具体组件；AES key 长度必须是 16/24/32 bytes；JSONB 转换使用共享 ObjectMapper 配置。 |
| `infrastructure-component-http` | 提供独立 HTTP client 抽象、请求/响应模型、拦截器上下文和 Apache async HTTP 实现。 | 通过 `HttpClientFactory.create()` 或 `create(HttpClientOptions)` 创建 client，构造 `HttpRequest` 后调用 `HttpClient#execute(...)`，返回 `CompletionStage<HttpResponse>`。 | 调用方依赖 `HttpClient` 接口，不依赖 Apache 实现；关注资源关闭、超时、拦截器异常传播和响应 body 消费。 |
| `infrastructure-component-web-api` | 提供 Web 响应信封、请求 ID、验证错误、业务异常、404 异常、fallback 异常映射和响应适配接口。 | 用 `WebExceptionMapper` 把异常转成 `MappedWebResponse`；默认响应使用 `DefaultResponseEnvelopeAdapter`，已有响应结构通过实现 `ResponseEnvelopeAdapter` 保持兼容。 | 纯 Java API，不注册 JAX-RS provider，因此不会覆盖应用自定义 mapper；Quarkus adapter 只负责接线。 |
| `infrastructure-component-artifact-storage` | 提供应用中立 artifact 存储 API 和本地文件 provider，包含路径穿越防护、metadata sidecar、content type 与字节数。 | 通过 `ArtifactStorage` 接口写入、读取和查询 metadata；本地实现使用 `new LocalArtifactStorage(root)`。 | key 必须是相对规范路径；本地 provider 会创建父目录并拒绝绝对路径和 `..` 片段；后续 MinIO/OSS 实现应保持同一接口。 |
| `infrastructure-component-mq-api` | 定义 MQ client、producer、consumer、message、processor、transaction、配置和异常抽象。 | 业务代码接收 `MQClient`，通过 `newProducer()`、`newConsumer()` 创建 producer/consumer，用 `IProducer#sendMessage(...)` 发消息。 | 不依赖 Pulsar、Kafka、RabbitMQ 或 Quarkus；`MQClient.builder()` 当前返回 `null`，维护时需明确该入口的处理方式。 |
| `infrastructure-component-mq-pulsar` | 实现 `mq-api` 到 Apache Pulsar Client 的映射，包含 client、producer、consumer、message builder、配置校验和资源管理。 | 构造 `PulsarConfig`，通过 `MQClientBuilder` 或 `PulsarClientFactory` 创建 `MQClient`；Quarkus 应用通常通过 `infrastructure-component-quarkus:mq` 的 `msg.*` 配置创建。 | 不读取 Quarkus 配置，不添加 CDI 注解；重点检查资源关闭幂等、Pulsar 异常转换、泛型消息序列化。 |
| `infrastructure-component-persistence-jooq` | 提供 jOOQ 持久化 core：`PersistenceContext`、`IPersistenceOperations`、metadata reader、table builder、DSLContext 工厂和 SQL 日志。 | 查询用 `ctx.get(dsl -> ...)`，变更用 `ctx.run(dsl -> ...)`，异步用 `fetchAsync(...)` 或 `executeAsync(...)`；schema 元数据用 `DatabaseMetadataReader`。 | 不能依赖 Quarkus datasource/Agroal/CDI；SQL 日志使用 `ILogger`；集成测试无 `JDBC_URL`、`JDBC_USER`、`JDBC_PASSWORD` 时应跳过。 |
| `infrastructure-component-redis` | 基于 Vert.x Redis client 提供异步/同步 API、泛型序列化、TTL、key 操作、计数器和连接配置。 | 通过 `RedisClientFactory.create(...)` 创建 `RedisClient`，调用 `get/set/del/pttl/keys/incr` 或同步方法 `getSync/setSync/delSync`。 | `keys(prefix)` 使用 Redis `KEYS` 命令，大 keyspace 生产环境有阻塞风险；同步方法基于 `join()`，异常会包装为 unchecked。 |
| `infrastructure-component-rule-engine` | 提供轻量规则引擎：`RuleEngine`、`Rule`、`Condition`、`Action`、`Fact`、`Facts`、组合规则和默认执行器。 | 使用 `RuleBuilder` 构造规则，准备 `Facts`，通过 `new DefaultRuleEngine().fire(rule, facts)` 执行；组合规则使用 `AllRules`、`AnyRules`、`CompositeRule`。 | 规则执行顺序、组合规则短路语义、动作异常策略是核心契约；当前 `DefaultRuleEngine` 对 null rule 使用 `System.err`，维护时应改为 `ILogger` 或明确异常。 |
| `infrastructure-component-pdp` | 提供策略决策点：`Pdp`、`Policy`、`PolicyRequest`、`PolicyDecision`、`DefaultPdp`、`IPdpAbility`。 | 实现若干 `Policy`，创建 `new DefaultPdp(policies)`，传入 `PolicyRequest` 调用 `evaluate(...)`。 | 默认语义是任一策略 DENY 则 DENY，否则有 PERMIT 则 PERMIT，没有 PERMIT 则 DENY；DENY 优先是安全敏感契约。 |
| `infrastructure-component-statemachine` | 提供状态机 builder DSL、状态、事件、上下文、transition、action、condition、运行时执行、PlantUML visitor 和调试能力。 | 通过 `StateMachineBuilderFactory.create()` 创建 builder，定义 state、external/internal transition、condition、action，`build()` 得到 `StateMachine`，运行时触发事件。 | 状态机是 workflow 的下层能力，不反向依赖 workflow；关注 builder DSL 兼容性、`TransitionFailException` 语义、action wrapper 执行顺序。 |
| `infrastructure-component-qdrant-grpc` | 提供 Qdrant gRPC client、points/collections client、`IQdrantAbility` 和 request factory。 | Quarkus 场景配置 `quarkus.qdrant.host`、`quarkus.qdrant.port`，由 provider 创建 client；直接使用时手动创建 Vert.x `GrpcClient` 和 `SocketAddress` 后实例化 `QdrantGrpcClient`。 | 当前模块混入 Quarkus provider，不是纯 client；`QdrantGrpcClientProvider` 应迁到 adapter；`clents` 包名修正需兼容迁移。 |
| `infrastructure-component-embedding-grpc` | 提供 embedding 服务 gRPC client、`IEmbeddingAbility` 和 protobuf 生成。 | Quarkus 场景配置 `quarkus.embedding.host`、`quarkus.embedding.port`，由 provider 创建 `EmbeddingGrpcClient`；直接使用时手动创建 Vert.x `GrpcClient` 和 `SocketAddress`。 | 当前模块混入 Quarkus provider，不是纯 client；protobuf 更新后要验证生成源码与 client API；`clents` 包名修正需兼容迁移。 |
| `infrastructure-component-workflow-core` | 提供纯 Java 工作流核心：`WorkflowDefinition`、Flow DSL、NodeHandler SPI、9 个内置 handler、图校验、路由、config codec、interceptor。 | 用 `Flow.define(id, name)` 定义流程，声明节点与边，`flow.build()` 得到 `WorkflowDefinition`；用 `NodeHandlerRegistry` 注册内置或自定义 handler；用 `GraphValidator` 校验定义。 | 零 Temporal 依赖；路由 key 大小写敏感；出边未匹配时流程终止；业务 handler 必须通过 `register` 触发 `DeterminismGuard`；DeterminismGuard 是 lint，不是运行时沙箱。 |
| `infrastructure-component-workflow-temporal` | 提供基于 Temporal 的配置驱动工作流运行时：`GenericWorkflowImpl`、`WorkflowEngine`、worker setup、业务 Activity registry、signal、查询、归档和示例。 | 推荐使用 `WorkflowEngine.connect(EngineConfig.of(target, queue)).withWorker(handlers, activities)` 启动 worker，通过 `engine.start(definition, businessData)` 发起流程，`WorkflowHandle#signal(...)` 发送事件。 | 运行需要 Temporal Server；业务 Activity 通过 `BusinessActivityRegistry` 按名字注册；`ServiceTask` 的 `activityInput` 是字面 Map，不做占位符替换；自定义 handler 必须保持 Temporal replay 确定性；归档开关在 `WorkflowInput`。 |

## Quarkus 适配组件

| 组件 | 能力 | 如何使用 | 注意事项 |
|------|------|----------|----------|
| `infrastructure-component-quarkus:authorization` | 提供 OIDC/Keycloak、用户身份、权限模型、模块权限、资源权限和 PDP/FGA provider 的 Quarkus 适配。 | Quarkus 应用依赖该模块，通过 `AuthProvider` 或 `IUserIdentity` 获取当前用户，通过 `IPermission`、`ModulePermission`、`ResourceIPermission` 表达权限；FGA 使用 `fga.api-url`、`fga.store-id`、`fga.authorization-model-id`、`fga.api-token`。 | 权限模型和判断规则应迁到 core；`SecurityIdentity` 解析、FGA 配置读取、Bean producer 留在 adapter；当前包名存在 `modle` 拼写问题，调整需兼容迁移。 |
| `infrastructure-component-quarkus:web` | 提供 Quarkus REST/Web 层通用能力：Jackson 配置、异常映射、请求上下文、路由过滤器、会话配置和 controller 拦截器。 | 依赖模块后使用 `web.*` 配置，例如 `web.super-user-id`、`web.session-timeout-hour`、`web.use-session`；业务 REST 层复用异常模型、Jackson producer、运行时用户/请求上下文。 | `ApiRequest`、通用异常基类、错误码不一定必须留在 adapter；依赖 `RoutingContext`、`RouteFilter`、JAX-RS `Response`、CDI 的代码留在 Quarkus adapter。 |
| `infrastructure-component-quarkus:persistence` | 把 Quarkus `AgroalDataSource` 装配为 jOOQ `DSLContext` 和 `PersistenceContext`。 | 配置 `quarkus.plugins.persistence.enable=true` 和 Quarkus datasource；业务注入 `PersistenceContext`、`DSLContext` 或实现 `IPersistenceAbility` 后使用 `ctx.get(...)`、`ctx.run(...)`。 | 不承载 repository 通用基类、metadata reader 或 SQL builder；这些能力属于 `persistence-jooq`。 |
| `infrastructure-component-quarkus:mq` | 把 `mq-api` 与 `mq-pulsar` 装配进 Quarkus 配置和生命周期，创建并关闭 Pulsar client、producer、consumer。 | 使用 `msg.*` 配置，例如 `msg.client.service-url`、`msg.producer.*`、`msg.consumer.*`、`msg.transaction.*`、`msg.monitoring.*`、`msg.retry.*`；业务通过 `MQClient` 创建 producer/consumer。 | 只做配置映射、Bean 暴露和 lifecycle 关闭；Pulsar 核心实现、资源管理、配置校验留在 `mq-pulsar`。 |
| `infrastructure-component-quarkus:cache` | 提供 Quarkus 本地缓存和远程 Redis 缓存访问入口，统一 get/set/delete/keys 等操作。 | 注入 `ICacheProvider`、`ICacheOperations` 或 `CacheContext`；本地缓存通过 Caffeine，远程缓存通过 Quarkus `ReactiveRedisDataSource`；远程 key 当前按 `quarkus.application.name` 作为前缀。 | `RemoteCacheContext.prefix` 是内部类字段上的 `@ConfigProperty`，普通 `new RemoteCacheContext(...)` 场景需要重点验证；抽象层不应暴露 Mutiny 或 Quarkus Redis 类型；生产环境慎用 `keys()`。 |
| `infrastructure-component-quarkus:discover` | 在 Quarkus 启动时注册服务到 Consul，关闭时注销，接入服务发现和健康检查。 | 配置 `consul.host`、`consul.port`、`quarkus.application.name`、`quarkus.http.port`；`ServiceLifecycle` 监听 `StartupEvent` 和 `ShutdownEvent`。 | 当前是 Quarkus 生命周期 adapter；不要加入通用服务发现策略；如支持非 Quarkus 应用，再抽 `discover-api`。 |
| `infrastructure-component-quarkus:workflow` | 提供 Temporal/状态机编排、事务工作流构建、事件、快照持久化、worker lifecycle 和 Quarkus 配置装配。 | 配置 Temporal/Quarkus 连接和 `workflow.enable.log`；业务使用 `TXWorkflowBuilder`、`TXWorkflowExecuter` 或 `IStateMachineAbility` 组合状态机和 Temporal workflow。 | 工作流模型、事件、构建器、执行器、repository 契约应脱离 Quarkus；lifecycle、ConfigMapping、worker 注册、CDI 注入留在 adapter。 |
| `infrastructure-component-quarkus:metrics` | 作为 Quarkus OpenTelemetry、Micrometer 或 SmallRye Metrics 的接入点，承载指标和链路追踪依赖与配置。 | 依赖模块后按 Quarkus 原生 metrics/tracing 配置启用 exporter、采样、resource attributes。 | 当前源码较少，更像依赖聚合和扩展占位；不要把业务指标计算逻辑写进该模块。 |
| `infrastructure-component-quarkus:broadcast` | 基于 Vert.x EventBus 提供广播、请求、订阅和注册管理能力。 | 配置 `quarkus.plugins.broadcast.enable=true` 后注入 `IBroadcastAbility` 或 `IBroadcastOperations`，通过 EventBus 地址发送、请求或订阅消息。 | 当前暴露 Vert.x 和 Mutiny 类型较多，属于强 adapter；若调用方被迫导入 `EventBus`、`MessageConsumer`、`Uni`，说明抽象边界还不干净。 |
| `infrastructure-component-quarkus:search` | 封装 Elasticsearch client、index/search/get/update/delete action、mapping/query builder 和搜索能力注册。 | 配置 `quarkus.plugins.search.enable=true`，并按 Quarkus Elasticsearch client 方式配置连接；业务通过 `ISearchAbility`、`IESOperations` 或 `ESContext` 执行 ES action。 | ES action、mapping/query builder 与 Quarkus 无关，应迁到 search core；如果 `co.elastic.clients.*` 和 `jakarta.inject.*` 在同一能力链路里混用，需要拆层。 |
| `infrastructure-component-quarkus:approval` | 提供审批 REST API、审批 DTO、审批模型、审批服务、jOOQ repository 和用户目录抽象。 | 配置 `quarkus.plugins.approval.enable=true` 后通过 `ApprovalResource` 暴露 HTTP API，业务也可注入或调用 `ApprovalService` 发起和推进审批；数据访问依赖 Quarkus persistence 的 jOOQ 能力。 | 当前超出 adapter 职责；审批模型、状态、流转规则、repository 接口应迁到独立 approval core；REST request/response、JAX-RS 异常、事务边界留在 adapter。 |
| `infrastructure-component-quarkus:user` | 把 Quarkus Security/Keycloak 当前登录用户适配为基础设施授权身份。 | 配置 `quarkus.plugins.user.enable=true`；业务通过 `KeycloakAuthProvider` 或 `UserAuthorizationService` 获取当前用户和权限相关信息。 | user 不应发展成权限 core；`SecurityIdentity`、Keycloak claims、当前请求用户读取留在 adapter；用户身份抽象和权限模型归入 authorization core。 |
| `infrastructure-component-quarkus:audit` | 提供 Quarkus 审计能力注册，当前包含审计事件模型、上下文、操作接口和 provider。 | 配置 `quarkus.plugins.audit.enable=true` 后注入 `IAuditAbility` 或 `IAuditOperations`。 | 顶层 `infrastructure-component-audit` 目录存在但未 include，且有迁移痕迹；不要维护两份审计事件模型；`AuditEvent`、`AuditContext`、`IAuditOperations` 应归入唯一 core。 |

## 历史目录

`infrastructure-component-audit` 目录存在，但 `settings.gradle.kts` 未 include，并有注释说明 audit 已迁到 `infrastructure-component-quarkus:audit`。因此本文不把它列为当前构建组件；维护时先决定是否恢复 include，再处理 Quarkus provider 迁移和审计 core 去重。

## 过模块检查清单

- 是否在 `settings.gradle.kts` 中 include。
- 是否属于顶层 core/API/implementation，还是 Quarkus adapter。
- 是否暴露 ability 接口，调用方是否依赖接口而非实现细节。
- 顶层组件源码是否出现 Quarkus、CDI、JAX-RS、MicroProfile Config、SmallRye Config import。
- Quarkus adapter 是否只做配置读取、Bean 注册、生命周期、事务和运行时装配。
- 外部服务测试是否有环境变量或跳过逻辑。
- 日志是否使用 `ILogger`。
- 生成源码目录是否保持只读。
