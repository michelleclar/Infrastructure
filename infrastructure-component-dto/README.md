# infrastructure-component-dto

跨模块共享的 DTO 基类与响应包装类库。提供统一的请求分层基类（`Command` / `Query` / `PageQuery`）和响应封装（`EntityResponse` / `SingleEntityResponse` / `MultiEntityResponse` / `PageEntityResponse`），供各业务模块继承或直接使用，避免重复定义。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-dto"))
}
```

无额外传递依赖，纯 Java 21，无框架绑定。

---

## 类型一览

### 请求侧基类（继承链：`DTO` → `Command` → `Query` → `PageQuery`）

| 类型 | 性质 | 字段 / 方法 | 用途 |
|------|------|------------|------|
| `DTO` | `abstract`，实现 `Serializable` | `serialVersionUID` | 所有 DTO 的根基类 |
| `Command` | `abstract extends DTO` | — | 写操作（insert / update / delete）入参基类 |
| `Query` | `abstract extends Command` | — | 查询操作入参基类 |
| `PageQuery` | `abstract extends Query` | `pageSize`（默认 10）、`pageIndex`（默认 1）、`orderBy`、`orderDirection`（默认 `DESC`）、`groupBy`、`needTotalCount`（默认 `true`） | 分页查询入参基类；`getOffset()` 自动计算偏移量 |
| `Scope` | `abstract extends DTO` | `includes: List<String>`、`excludes: List<String>` | 字段可见范围裁剪基类（按需继承） |

常量：`PageQuery.ASC = "ASC"`，`PageQuery.DESC = "DESC"`。

### 客户端扩展基类

| 类型 | 性质 | 字段 / 方法 | 用途 |
|------|------|------------|------|
| `ClientObject` | `abstract`，实现 `Serializable` | `extValues: Map<String, Object>`；`getExtField(key)`、`putExtField(fieldName, value)`、`getExtValues()`、`setExtValues(map)` | 供客户端侧 DTO 继承，支持携带任意扩展键值对 |

### 响应封装类

| 类型 | 泛型 | 核心字段 | 静态工厂方法 | 用途 |
|------|------|---------|-------------|------|
| `EntityResponse` | 无 | `success: boolean`、`errCode: String`、`errMessage: String` | `buildSuccess()`、`buildFailure(errCode, errMessage)` | 不含业务数据的操作结果（如写操作响应） |
| `SingleEntityResponse<T>` | `T` | 继承 `EntityResponse` + `data: T` | `buildSuccess()`、`buildFailure(errCode, errMessage)`、`of(T data)` | 单条记录响应 |
| `MultiEntityResponse<T>` | `T` | 继承 `EntityResponse` + `data: Collection<T>` | `buildSuccess()`、`buildFailure(errCode, errMessage)`、`of(Collection<T> data)` | 列表响应（非分页）；`isEmpty()` / `isNotEmpty()` 判空辅助 |
| `PageEntityResponse<T>` | `T` | 继承 `EntityResponse` + `data: Collection<T>`、`totalCount: int`、`pageSize: int`（下限 1）、`pageIndex: int`（下限 1） | `buildSuccess()`、`buildFailure(errCode, errMessage)`、`of(pageSize, pageIndex)`、`of(data, totalCount, pageSize, pageIndex)` | 分页响应；`getTotalPages()` 自动计算总页数 |

---

## 使用示例

### 定义业务入参

```java
// 写操作入参
public class CreateOrderCommand extends Command {
    private String productId;
    private int quantity;
    // getters / setters ...
}

// 分页查询入参
public class OrderPageQuery extends PageQuery {
    private String status;
    // getters / setters ...
}

// 使用分页参数
OrderPageQuery q = new OrderPageQuery();
q.setPageIndex(2).setPageSize(20).setOrderBy("createdAt").setOrderDirection(PageQuery.DESC);
int offset = q.getOffset(); // (2-1) * 20 = 20
```

### 响应封装

```java
// 写操作成功/失败
EntityResponse ok  = EntityResponse.buildSuccess();
EntityResponse err = EntityResponse.buildFailure("ORDER_NOT_FOUND", "订单不存在");

// 单条记录
SingleEntityResponse<OrderDTO> single = SingleEntityResponse.of(orderDTO);

// 列表（非分页）
MultiEntityResponse<OrderDTO> multi = MultiEntityResponse.of(orderList);
if (multi.isNotEmpty()) { ... }

// 分页
PageEntityResponse<OrderDTO> page =
    PageEntityResponse.of(orderList, totalCount, pageSize, pageIndex);
int totalPages = page.getTotalPages();
```

### 扩展 `ClientObject`（携带扩展字段）

```java
public class OrderClientVO extends ClientObject {
    private String orderId;
    // getters / setters ...
}

OrderClientVO vo = new OrderClientVO();
vo.setExtValues(new HashMap<>());
vo.putExtField("traceId", "abc-123");
Object traceId = vo.getExtField("traceId");
```

---

## 注意事项

- `PageQuery.setPageSize` / `setPageIndex` 均有下限保护（最小值为 1），无需在调用方做额外校验。
- `MultiEntityResponse.getData()` 和 `PageEntityResponse.getData()` 在 `data` 为 `null` 时返回空 `List`，不抛 NPE。
- `ClientObject.putExtField` 在 `extValues` 未初始化时会抛 `NullPointerException`，调用前须先 `setExtValues(new HashMap<>())` 完成初始化。
- 所有类均实现 `Serializable`（通过 `DTO` 根基类或 `ClientObject`），适合跨进程序列化场景。
- 本模块无任何框架依赖，可在 Quarkus / Spring / 纯 Java 环境中使用。
