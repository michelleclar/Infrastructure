# infrastructure-component-qdrant-grpc

基于 Vert.x gRPC 的 Qdrant 向量数据库客户端封装。提供 CDI 可注入的 `QdrantGrpcClient`，以及一套工厂类，将原始 protobuf Builder 调用简化为一行静态方法，屏蔽 gRPC 传输细节，返回 Vert.x `Future<T>` 供响应式链式调用。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-qdrant-grpc"))
}
```

---

## 配置

在 `application.properties`（或 `application.yaml`）中配置 Qdrant gRPC 端口。两项均有默认值，不配置时自动连 `localhost:6334`。

| 配置键                  | 类型      | 默认值        | 说明                        |
|------------------------|---------|-------------|---------------------------|
| `quarkus.qdrant.host`  | String  | `localhost` | Qdrant gRPC 服务主机          |
| `quarkus.qdrant.port`  | Integer | `6334`      | Qdrant gRPC 端口（非 HTTP 端口） |

---

## 核心类

### 连接与客户端

| 类 / 接口                   | 包                                     | 说明                                                                                 |
|---------------------------|---------------------------------------|------------------------------------------------------------------------------------|
| `QdrantGrpcClient`        | `org.carl.infrastructure.qdrant`      | 核心客户端，持有 Vert.x gRPC 连接，暴露 `getCollectionsGrpcClient()` / `getPointsGrpcClient()` |
| `QdrantGrpcClientProvider`| `org.carl.infrastructure.qdrant`      | CDI `@Produces` bean，读配置自动构造 `QdrantGrpcClient`，Quarkus 环境无需手动实例化               |
| `IQdrantAbility`          | `org.carl.infrastructure.qdrant`      | 可选接口，业务 bean 实现后直接调 `getPoints()` / `getCollections()`，省去注入步骤                   |

### 操作客户端

| 类                        | 关键方法                                                                                                                                 | 返回值                                          |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `CollectionsGrpcClient`  | `get(Function<GetCollectionInfoRequest.Builder, GetCollectionInfoRequest>)`                                                           | `Future<GetCollectionInfoResponse>`          |
|                          | `create(Function<CreateCollection.Builder, CreateCollection>)`                                                                        | `Future<CollectionOperationResponse>`        |
|                          | `collectionExists(Function<CollectionExistsRequest.Builder, CollectionExistsRequest>)`                                               | `Future<CollectionExistsResponse>`           |
| `PointsGrpcClient`       | `upsert(Function<UpsertPoints.Builder, UpsertPoints>)`                                                                               | `Future<PointsOperationResponse>`            |
|                          | `get(Function<GetPoints.Builder, GetPoints>)`                                                                                         | `Future<GetResponse>`                        |
|                          | `query(Function<QueryPoints.Builder, QueryPoints>)`                                                                                   | `Future<QueryResponse>`                      |

所有方法接受一个 **lambda**，参数是对应的 protobuf Builder，返回 Builder 构建好的请求对象。这种设计使调用点无需导入大量 Builder 类型，只操作一个 lambda 参数即可。

### 工厂类（Factory）

工厂类全部是 `final` 静态工具类，无需实例化。

| 工厂类                      | 用途                                                           |
|---------------------------|--------------------------------------------------------------|
| `PointIdFactory`          | `id(long)` / `id(UUID)` / `id(String)` — 构造 `PointId`       |
| `VectorFactory`           | `vector(List<Float>)` / `vector(float...)` / `multiVector(...)` — 构造单向量 / 多向量 / 稀疏向量 |
| `VectorsFactory`          | `vectors(List<Float>)` / `namedVectors(Map<String, Vector>)` — 构造 `Vectors`（upsert 时用）|
| `VectorInputFactory`      | `vectorInput(List<Float>)` / `vectorInput(long)` / `multiVectorInput(...)` — 构造查询用 `VectorInput` |
| `VectorsConfigFactory`    | `build(long size, Distance distance)` — 构造 collection 向量配置   |
| `PointStructFactory`      | `buildPointStruct(UUID, List<Float>)` / `buildPointStruct(long, List<Float>, Map<String,Object>)` 等多重载 — 构造 `PointStruct`，自动处理 payload Java 类型到 `JsonWithInt.Value` 的转换（支持 String / Integer / Long / Double / Boolean / List / Map） |
| `QueryFactory`            | `nearest(List<Float>)` / `nearest(float...)` / `nearest(UUID)` / `recommend(...)` / `discover(...)` / `fusion(...)` / `orderBy(...)` / `formula(...)` / `sample(...)` — 构造 `Query` |
| `ConditionFactory`        | `matchKeyword` / `matchText` / `match` / `range` / `hasId` / `isEmpty` / `isNull` / `nested` / `geoRadius` / `geoBoundingBox` / `geoPolygon` / `valuesCount` / `hasVector` — 构造过滤 `Condition` |
| `ValueFactory`            | `value(String/long/double/boolean/List/Map)` / `nullValue()` — 构造 payload 值 `JsonWithInt.Value` |
| `WithPayloadSelectorFactory` | `enable(boolean)` / `include(List<String>)` / `exclude(List<String>)` — 控制响应中的 payload 字段 |
| `ShardKeyFactory`         | `shardKey(String)` / `shardKey(long)` — 构造分片键                |
| `ExpressionFactory`       | `constant` / `variable` / `condition` / `sum` / `mult` / `div` / `pow` / `sqrt` / `exp` / `log10` / `ln` / `expDecay` / `gaussDecay` / `linDecay` 等 — 构造评分公式表达式 |

---

## 使用示例

### Quarkus CDI 环境（推荐）

```java
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import org.carl.infrastructure.qdrant.QdrantGrpcClient;
import org.carl.infrastructure.qdrant.factory.*;
import io.qdrant.client.grpc.Collections.Distance;

@ApplicationScoped
public class MyVectorService {

    @Inject
    QdrantGrpcClient qdrant;

    // 1. 检查 collection 是否存在
    public void ensureCollection(String name) {
        qdrant.getCollectionsGrpcClient()
              .collectionExists(b -> b.setCollectionName(name).build())
              .compose(resp -> {
                  if (resp.getResult().getExists()) {
                      return io.vertx.core.Future.succeededFuture(null);
                  }
                  // 2. 不存在则创建（384 维，余弦距离）
                  return qdrant.getCollectionsGrpcClient().create(b ->
                      b.setCollectionName(name)
                       .setVectorsConfig(VectorsConfigFactory.build(384, Distance.Cosine))
                       .build()
                  );
              })
              .onSuccess(r -> System.out.println("collection ready"))
              .onFailure(Throwable::printStackTrace);
    }

    // 3. 写入向量（带 payload）
    public void upsertPoint(String collection, long pointId,
                            List<Float> vector, Map<String, Object> payload) {
        var point = PointStructFactory.buildPointStruct(pointId, vector, payload);
        qdrant.getPointsGrpcClient()
              .upsert(b -> b.setCollectionName(collection).addPoints(point).build())
              .onSuccess(r -> System.out.println("upsert ok"));
    }

    // 4. 向量相似搜索（Top-5，带 payload 过滤）
    public void search(String collection, List<Float> queryVector) {
        qdrant.getPointsGrpcClient()
              .query(b -> b
                  .setCollectionName(collection)
                  .setQuery(QueryFactory.nearest(queryVector))
                  .setFilter(io.qdrant.client.grpc.Points.Filter.newBuilder()
                      .addMust(ConditionFactory.matchKeyword("category", "article"))
                      .build())
                  .setWithPayload(WithPayloadSelectorFactory.enable(true))
                  .setLimit(5)
                  .build())
              .onSuccess(resp -> resp.getResultList()
                  .forEach(pt -> System.out.println(pt.getId() + " score=" + pt.getScore())))
              .onFailure(Throwable::printStackTrace);
    }
}
```

### 非 CDI 环境（手动构造）

```java
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import org.carl.infrastructure.qdrant.QdrantGrpcClient;

Vertx vertx = Vertx.vertx();
QdrantGrpcClient client = new QdrantGrpcClient(
    vertx,
    SocketAddress.inetSocketAddress(6334, "localhost")
);

// 获取 collection 信息
client.getCollectionsGrpcClient()
      .get(b -> b.setCollectionName("my_collection").build())
      .onSuccess(info -> System.out.println(info.getResult().getStatus()))
      .onFailure(Throwable::printStackTrace);
```

### 实现 `IQdrantAbility`（可选门面）

```java
@ApplicationScoped
public class DocumentService implements IQdrantAbility {

    @Inject
    QdrantGrpcClient qdrantClient;

    @Override
    public QdrantGrpcClient getQdrantClient() {
        return qdrantClient;
    }

    public void run() {
        // 直接用 getPoints() / getCollections() 省去中间变量
        getPoints().upsert(b -> b.setCollectionName("docs")
                                 .addPoints(PointStructFactory.buildPointStruct(1L, List.of(0.1f, 0.2f)))
                                 .build());
    }
}
```

---

## 注意事项

- **gRPC 端口**：Qdrant 默认 gRPC 端口是 `6334`，REST 端口是 `6333`，本模块只对接 gRPC，配置时不要混淆。
- **返回值均为 `Future<T>`**：所有操作异步返回 Vert.x `Future`，需通过 `.onSuccess` / `.onFailure` / `.compose` 处理，不要阻塞调用。
- **PointStructFactory payload 类型**：`buildPointStruct` 的 `Map<String, Object>` payload 支持 `String`、`Integer`、`Long`、`Double`、`Boolean`、`List`、`Map` 和 `JsonWithInt.Value`；其他类型会被转为 null 并打出警告日志。
- **多向量 Collection**：如果 collection 使用命名向量（Named Vectors），upsert 时需使用 `PointStructFactory.buildPointStruct(UUID, Map<String, Points.Vector>, Map<String, Object>)` 重载，配合 `VectorFactory.vector(...)` 按名字组装。
- **Proto 文件位置**：项目将 Qdrant 官方 proto 放在 `dil/protos/`，通过 `protobuf { protobuf(files("dil/protos")) }` 参与编译，生成的 stub 类不属于本模块手写 API，不直接使用生成类名进行业务调用（只通过上面的工厂类和客户端封装访问）。
