# infrastructure-component-quarkus / authorization

基于 [OpenFGA](https://openfga.dev/) 的关系型细粒度授权模块。提供两套正交的权限模型：

- **模块权限**：按模块（`module`）和动作（`action`）定义用户可执行的操作，内置标准动作级别枚举。
- **资源权限**：以 `user / relation / object` 三元组描述主体与资源的关系，委托 OpenFGA 进行关系求值（PDP）。

两套模型统一通过 `AuthProvider` 接口暴露，业务模块实现 `IModuleAuthorizationServiceAbility` 即可获得完整的权限检查能力。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-quarkus:authorization"))
}
```

本模块通过 `libs.bundles.auth` 引入以下三个传递依赖：

| 依赖 | 坐标 | 版本 |
|------|------|------|
| Quarkus OIDC | `io.quarkus:quarkus-oidc` | 3.30.4 |
| Quarkus Keycloak Authorization | `io.quarkus:quarkus-keycloak-authorization` | 3.30.4 |
| OpenFGA Java SDK | `dev.openfga:openfga-sdk` | 0.9.3 |

> **注意**：`build.gradle.kts` 中有一行被注释掉的 `// implementation(libs.openfga)`。实际上 `openfga-sdk` 已通过 `libs.bundles.auth` 包含其中，注释行属于冗余遗留，不影响功能。

---

## 配置项

`FgaClientProvider` 通过 MicroProfile Config 读取以下属性，并生产 `@ApplicationScoped` 的 `OpenFgaClient` CDI bean：

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `fga.api-url` | （必填，无默认值） | OpenFGA 服务地址，如 `http://localhost:8080` |
| `fga.store-id` | `""` | OpenFGA Store ID |
| `fga.authorization-model-id` | `""` | 授权模型 ID；为空时使用 Store 内最新模型 |
| `fga.api-token` | `""` | API Token；非空时自动附加 `ApiToken` 凭证，为空时不配置认证 |

`application.properties` 示例：

```properties
fga.api-url=http://localhost:8080
fga.store-id=01HXYZ
fga.authorization-model-id=01HABC
fga.api-token=my-token
```

---

## 核心抽象

### `IUserIdentity`

用户身份上下文接口，代表已认证（或匿名）用户的完整身份信息。

```java
public interface IUserIdentity {
    String USER_ATTRIBUTE = "auth.user.identity";   // 身份存储属性键

    Boolean isAnonymous();
    Map<String, Set<Permission>> getPermissions();   // key = 模块名
    Set<UserGroup> getGroups();
    Set<UserOrganize> getOrganizes();
    Set<String> getRoles();
    Boolean hasRole(String role);
    Object getAttribute(String name);
    Map<String, Object> getAttributes();
}
```

`UserIdentity` 是其默认实现，可通过 `UserIdentity.UserIdentityBuilder` 链式构造，也可通过 `UserIdentity(SecurityIdentity)` 构造函数直接从 Quarkus `SecurityIdentity` 初始化。

---

### `IModuleEnum`

模块动作（权限项）的抽象描述，用于将枚举值作为结构化权限标识传入检查方法。

```java
public interface IModuleEnum {
    String getName();
    int getLevel();
}
```

---

### `IPermission`

单条权限规则的核心接口，`ModulePermission` 和 `ResourceIPermission` 均实现此接口。

```java
public interface IPermission {
    String getName();
    String getDescription();
    Boolean hasPermission(IUserIdentity identity, @Nonnull String requiredAction);
}
```

---

### `IModel`

模块树节点描述接口，用于表述业务模块的元数据及其权限列表。

```java
public interface IModel {
    String getUrl();
    String getName();
    String getCode();
    String getDescription();
    boolean isVisible();
    boolean isRoot();
    List<ModulePermission> getPermissions();
    List<IModel> getSubModules();
}
```

---

### `AuthProvider`

授权能力的顶层接口，所有授权服务均实现或继承此接口。

```java
public interface AuthProvider {
    IUserIdentity getIdentity(String token);
    default IUserIdentity getIdentity();                   // 从 SecurityIdentity 构造
    SecurityIdentity getSecurityIdentity();
    default boolean hasModulePermission(ModulePermission permission);
    default boolean canAccess(ResourceIPermission permission);
}
```

---

### `IModuleAuthorizationServiceAbility`

继承 `AuthProvider`，业务模块授权服务的标准能力接口。实现类需声明自己负责的模块标识，并提供枚举驱动的权限检查。

```java
public interface IModuleAuthorizationServiceAbility extends AuthProvider {
    <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission);

    default <T extends Enum<T> & IModuleEnum> boolean check(
            String permission, T requiredPermission) {
        return requiredPermission.getName().equalsIgnoreCase(permission);
    }

    String getModule();
}
```

---

## 权限模型

### `ModuleStandardPermission`（枚举）

内置标准模块动作，实现 `IModuleEnum`，通过 `level` 字段表示操作敏感度。

| 枚举值 | level |
|--------|-------|
| `VIEW` | 1 |
| `LIST` | 1 |
| `CREATE` | 2 |
| `EDIT` | 3 |
| `UPDATE` | 3 |
| `EXPORT` | 3 |
| `IMPORT` | 3 |
| `DELETE` | 4 |
| `ASSIGN` | 4 |
| `PUBLISH` | 5 |
| `APPROVE` | 5 |
| `MANAGE` | 5 |
| `ARCHIVE` | 6 |

静态工具方法：

```java
// 按名称（大小写不敏感）查找枚举值，不存在则返回 null
static ModuleStandardPermission fromName(String name)

// 判断 currentAction 的 level 是否 >= requiredAction 的 level
static boolean hasPermission(String currentAction, String requiredAction)
```

---

### `ModulePermission`

基于模块名 + 动作的权限对象，从 `IUserIdentity.getPermissions()` 中按模块名查找对应的 `Permission` 集合进行校验。

通过 `ModulePermission.ModulePermissionBuilder` 构造：

```java
ModulePermission permission = ModulePermission.ModulePermissionBuilder.create()
        .name("user.order")          // 模块名（对应 getPermissions() 的 key）
        .description("订单操作")
        .action("VIEW")              // 要检查的动作
        .build();
```

主要方法：

```java
// 检查 identity 在该模块下是否具有指定动作
Boolean hasPermission(IUserIdentity identity, @Nonnull String requiredAction)

// 检查 identity 在该模块下是否具有构造时指定的 action
Boolean hasPermission(IUserIdentity identity)
```

---

### `ResourceIPermission`

抽象类，描述资源级别的权限。持有 `resourceId`、`resourceType`、`relation` 等字段，供外部扩展实现与 OpenFGA 对接。默认 `hasPermission` 实现均返回 `false`，子类需覆写以集成实际 FGA 检查逻辑。

通过 `ResourceIPermission.ResourcePermissionBuilder` 构造：

```java
ResourceIPermission permission = ResourceIPermission.ResourcePermissionBuilder.create()
        .name("document-reader")
        .description("文档读取权限")
        .resourceId("doc-42")
        .resourceType("document")
        .relation("reader")
        .build();
```

字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 权限名称 |
| `resourceId` | `String` | 资源实例 ID |
| `resourceType` | `String` | 资源类型 |
| `relation` | `String` | 关系类型（对应 FGA relation） |
| `description` | `String` | 描述 |

---

### `Permission`

`IUserIdentity.getPermissions()` 值的元素类型，代表某用户在某模块下的全部动作集合。

| 方法 | 说明 |
|------|------|
| `Boolean hasPermission(String requirePermission)` | 按动作名（大小写不敏感，内部统一转大写）检查；disabled 优先于 enabled |
| `Boolean hasPermission(Integer permissionLevel)` | 按 level 检查，`permissionLevel >= maxLevel` 时为 true |
| `<T extends Enum<T> & IModuleEnum> Boolean hasPermission(T requiredPermission)` | 默认按枚举名（忽略 level）检查 |
| `<T extends Enum<T> & IModuleEnum> Boolean hasPermission(T requiredPermission, Boolean ignoreLevel)` | `ignoreLevel=true` 按名称，`ignoreLevel=false` 按 level |
| `Set<String> getEnabledActions()` | 已启用的动作名集合（大写） |
| `Set<String> getDisabledActions()` | 已禁用的动作名集合（大写） |
| `Integer getMaxLevel()` | 当前 Permission 包含的最大动作 level |

通过 `Permission.PermissionBuilder` 构造：

```java
Permission p = Permission.PermissionBuilder.create("user.order")
        .addAction(b -> b.action("VIEW").level(1).enable(true).build())
        .addAction(b -> b.action("DELETE").level(4).enable(false).build())
        .build();
```

---

## 用户身份模型

### `UserIdentity`

`IUserIdentity` 的标准实现。支持两种构造方式：

```java
// 方式一：从 Quarkus SecurityIdentity 构造（自动填充 roles、attributes，permissions/groups/organizes 为空集合）
new UserIdentity(SecurityIdentity identity)

// 方式二：Builder 链式构造
UserIdentity user = UserIdentity.UserIdentityBuilder.create()
        .setAnonymous(false)
        .setRoles(Set.of("manager"))
        .setUserGroups(Set.of(...))
        .setUserOrganizes(Set.of(...))
        .addPermission("user.order", p -> p.addAction(b -> b.action("VIEW").level(1).enable(true).build()).build())
        .build();
```

---

### `UserGroup`

```java
public class UserGroup {
    String name;
}
```

用户所属团队/组，仅持有名称字段。

---

### `UserOrganize`

```java
public class UserOrganize {
    Long organizeId;
    String name;
    Set<UserGroup> groups;
}
```

用户所属组织，一个组织可包含多个 `UserGroup`。

---

### `ModuleAction`

`Permission` 中单条动作的内部结构，通过 `ModuleAction.ModuleActionBuilder` 构造：

```java
ModuleAction action = ModuleAction.ModuleActionBuilder.create()
        .action("VIEW")
        .level(1)
        .enable(true)
        .build();

// 也可从 IModuleEnum 枚举直接填充 action 和 level
ModuleAction action2 = ModuleAction.ModuleActionBuilder.create()
        .addStandardAction(ModuleStandardPermission.VIEW)
        .enable(true)
        .build();
```

字段：`action`（`String`）、`level`（`Integer`）、`enable`（`Boolean`）。

---

## FGA 客户端

`FgaClientProvider` 通过 `@Produces` 生产 `OpenFgaClient` CDI bean。`FGAClient` 和 `FGASyncClient` 均注入该 bean，两者提供**完全相同的方法签名**，差异仅在 `check` 方法的实现细节上（见下文说明）。

### `FGASyncClient`（推荐）

`@ApplicationScoped` bean，所有方法调用 `openFgaClient.xxx(request).get()` 阻塞等待结果，适合同步请求处理路径。

| 方法 | 签名 | 说明 |
|------|------|------|
| `check` | `boolean check(String user, String relation, String object)` | 检查 user 对 object 是否具有 relation 关系；返回 `response.getAllowed()` |
| `checkWithContext` | `boolean checkWithContext(String user, String relation, String object, List<ClientTupleKey> contextualTuples)` | 携带临时上下文元组的权限检查 |
| `write` | `ClientWriteResponse write(List<ClientTupleKey> writes)` | 写入关系元组（授予权限） |
| `delete` | `ClientWriteResponse delete(List<ClientTupleKeyWithoutCondition> deletes)` | 删除关系元组（撤销权限） |
| `writeAndDelete` | `ClientWriteResponse writeAndDelete(List<ClientTupleKey> writes, List<ClientTupleKeyWithoutCondition> deletes)` | 批量写入 + 删除 |
| `read` | `ClientReadResponse read(String user, String relation, String object)` | 查询关系元组，三个参数均可为 `null`（作为过滤条件） |
| `listObjects` | `List<String> listObjects(String user, String relation, String type)` | 列出 user 对某 type 下所有具有 relation 关系的对象 ID |
| `expand` | `ClientExpandResponse expand(String relation, String object)` | 展开 object 的权限关系树 |
| `listStores` | `ClientListStoresResponse listStores()` | 列出所有 Store |
| `getStore` | `ClientGetStoreResponse getStore()` | 获取当前 Store 信息 |
| `deleteStore` | `ClientDeleteStoreResponse deleteStore()` | 删除当前 Store |
| `readAuthorizationModels` | `ClientReadAuthorizationModelsResponse readAuthorizationModels()` | 读取所有授权模型 |
| `readAuthorizationModel` | `ClientReadAuthorizationModelResponse readAuthorizationModel()` | 读取当前授权模型 |
| `readChanges` | `ClientReadChangesResponse readChanges(String type)` | 读取关系元组变更日志，`type` 为 `null` 时不过滤 |
| `createTupleKey` | `ClientTupleKey createTupleKey(String user, String relation, String object)` | 辅助方法：构造写入用元组 |
| `createTupleKeyWithoutCondition` | `ClientTupleKeyWithoutCondition createTupleKeyWithoutCondition(String user, String relation, String object)` | 辅助方法：构造删除用元组 |

所有方法均声明抛出 `FgaInvalidParameterException`、`ExecutionException`、`InterruptedException`。

### `FGAClient`

与 `FGASyncClient` 方法集完全一致，但 **`check` 方法存在实现缺陷**：当前版本的 `check(String, String, String)` 始终返回 `false`（`Boolean.TRUE.equals(false)`），调用结果不反映实际 FGA 判断。`checkWithContext` 方法实现正常。生产环境如需无上下文的权限检查，应使用 `FGASyncClient.check`。

---

## 使用示例

### 1. 模块权限检查

```java
@ApplicationScoped
public class OrderAuthorizationService implements IModuleAuthorizationServiceAbility {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public String getModule() {
        return "user.order";
    }

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

    @Override
    public IUserIdentity getIdentity(String token) {
        // 根据 token 解析身份，此处按需实现
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Enum<T> & IModuleEnum> boolean check(T requiredPermission) {
        ModulePermission permission = ModulePermission.ModulePermissionBuilder.create()
                .name(getModule())
                .action(requiredPermission.getName())
                .build();
        return hasModulePermission(permission);
    }
}
```

调用侧：

```java
@Inject
OrderAuthorizationService authService;

// 检查当前用户是否具有 VIEW 权限
boolean canView = authService.check(ModuleStandardPermission.VIEW);
```

---

### 2. OpenFGA 关系权限检查（`FGASyncClient`）

```java
@Inject
FGASyncClient fgaClient;

// 检查 user:alice 是否对 document:report-42 具有 reader 关系
boolean allowed = fgaClient.check("user:alice", "reader", "document:report-42");

// 授予权限（写入关系元组）
ClientTupleKey tuple = fgaClient.createTupleKey("user:alice", "reader", "document:report-42");
fgaClient.write(List.of(tuple));

// 撤销权限（删除关系元组）
ClientTupleKeyWithoutCondition del =
        fgaClient.createTupleKeyWithoutCondition("user:alice", "reader", "document:report-42");
fgaClient.delete(List.of(del));

// 列出 alice 可读取的所有文档
List<String> docs = fgaClient.listObjects("user:alice", "reader", "document");
```

---

### 3. 构造 `UserIdentity` 并进行模块权限校验

```java
UserIdentity user = UserIdentity.UserIdentityBuilder.create()
        .setAnonymous(false)
        .setRoles(Set.of("manager"))
        .addPermission("user.order", p ->
                p.addAction(b -> b.addStandardAction(ModuleStandardPermission.VIEW).enable(true).build())
                 .addAction(b -> b.addStandardAction(ModuleStandardPermission.DELETE).enable(false).build())
                 .build())
        .build();

ModulePermission perm = ModulePermission.ModulePermissionBuilder.create()
        .name("user.order")
        .action("VIEW")
        .build();

boolean result = perm.hasPermission(user); // true
```

---

## 注意事项

- `FGAClient.check(String, String, String)` 存在实现 bug，始终返回 `false`，请勿在生产环境使用；应改用 `FGASyncClient.check` 或 `FGAClient.checkWithContext`。
- `fga.api-url` 无默认值，应用启动时若未配置该属性，`FgaClientProvider` 将因 `@ConfigProperty` 注入失败而导致 CDI 部署异常。
- `ResourceIPermission` 的 `hasPermission` 默认实现均返回 `false`，子类必须覆写才能实现实际的资源授权逻辑。
- `Permission.hasPermission(Integer permissionLevel)` 的语义是 `permissionLevel >= maxLevel`（即调用方 level 不低于所需最大 level），与 `ModuleStandardPermission.hasPermission` 中 `current.level >= required.level` 的方向一致，使用前需确认含义符合业务预期。
- `FgaClientProvider` 中有一条注释 `// TODO: 可以配置租户Id`，当前尚未实现多租户 header 注入（`X-telnet-id`）。
- `beans.xml` 文件内容为空，CDI bean 发现模式依赖 Quarkus 默认扫描策略。
