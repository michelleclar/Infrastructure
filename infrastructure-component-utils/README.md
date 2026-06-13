# infrastructure-component-utils

> 基础设施层通用工具库。提供字符串、集合、字段类型判断、日志截断、URL 解析、数据脱敏以及 DAG / 链式表等数据结构工具，供各模块无 Quarkus 依赖地直接引用。

---

## 依赖引入

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":infrastructure-component-utils"))
}
```

无任何第三方运行时依赖，仅需 JDK 21。

---

## 工具类一览

### 基础工具（包 `org.carl.infrastructure.utils`）

| 类 | 主要方法 | 用途 |
|---|---|---|
| `StringUtils` | `isEmpty(CharSequence)` | 判断字符串是否为 `null` 或空串（不 trim） |
| | `isNumeric(CharSequence)` | 判断是否全为 Unicode 数字 |
| | `isNumericSpace(CharSequence)` | 判断是否全为数字或空格 |
| `CollectionUtils` | `isEmpty(Collection<?>)` | 判断集合是否为 `null` 或空 |
| | `notEmpty(Collection<?>)` | 判断集合非空 |
| `FieldsUtils` | `isString(Object)` | 判断值是否为 `String` |
| | `isInteger(Object)` | 判断值是否为 `Integer` |
| | `isLong(Object)` | 判断值是否为 `Long` 或 `Integer` |
| | `isFloat(Object)` | 判断值是否为 `Float` |
| | `isDouble(Object)` | 判断值是否为 `Double` |
| | `isBoolean(Object)` | 判断值是否为 `Boolean` |
| | `isStringOrLong(Object)` | 判断值是否为 `String` 或 `Long`/`Integer` |
| | `isStringOrLongOrBoolean(Object)` | 判断值是否为字符串、长整型或布尔 |
| | `isNumber(Object)` | 判断值是否为 `Number` 子类 |
| | `isList(Object)` | 判断值是否为 `List` |
| | `isPrimitiveType(Object)` | 判断值是否为基础标量类型（String/int/long/float/double/boolean） |
| | `typeOf(Object)` | 返回值的简单类名，`null` 时返回 `"null"` |
| `LogUtils` | `toShortString(Object, int maxLength)` | 将对象 toString 后截断至 `maxLength`，超出部分追加 `"..."` |
| `UrlParser` | `UrlParser(String url)` 构造器 | 解析 URL，支持 HTTP/HTTPS/FTP/REDIS/JDBC/PULSAR/SMTP 协议 |
| | `getProtocol()` | 返回协议名（大写字符串） |
| | `getHost()` | 返回主机名 |
| | `getPort()` | 返回端口号 |
| | `getPath()` | 返回路径 |
| | `getQuery()` | 返回查询参数字符串 |

### 脱敏工具（包 `org.carl.infrastructure.utils.desensitization`）

| 类 | 类型 | 方法 | 行为 |
|---|---|---|---|
| `IDesensitizationAlgorithm` | 接口 | `desensitize(String source)` | 脱敏算法统一接口 |
| `MaskEmailAlgorithm` | 枚举单例 `INSTANCE` | `desensitize(String)` | 邮箱本地部分保留首尾字符，中间替换为 `*`（如 `a***z@example.com`） |
| `MaskMiddleAlgorithm` | 枚举单例 `INSTANCE` | `desensitize(String)` | 保留首尾字符，中间全部替换为 `*`（长度 ≤ 2 时原样返回） |
| `MaskPhoneNumberAlgorithm` | 枚举单例 `INSTANCE` | `desensitize(String)` | 11 位手机号保留前 3 后 4 位，中间 4 位替换为 `****` |

### 数据结构（包 `org.carl.infrastructure.utils.struct`）

| 类 | 主要方法 | 用途 |
|---|---|---|
| `DAG` | `addNode(String)` | 添加节点（重复时抛出异常） |
| | `addNodeIfNotExists(String)` | 幂等添加节点 |
| | `deleteNode(String)` | 删除节点及其所有关联边 |
| | `deleteNodeIfExists(String)` | 幂等删除节点 |
| | `addEdge(String indNode, String depNode)` | 添加有向边，自动检测环路 |
| | `deleteEdge(String indNode, String depNode)` | 删除有向边 |
| | `predecessors(String node)` | 返回指向该节点的所有前驱节点 |
| | `downstream(String node)` | 返回直接后继节点列表 |
| | `allDownstreams(String node)` | BFS 返回所有可达后继节点（含自身） |
| | `allLeaves()` | 返回所有叶节点（出度为 0） |
| | `independentNodes()` | 返回所有入度为 0 的节点 |
| | `topologicalSort()` | 返回拓扑排序结果，有环时抛出 `IllegalStateException` |
| | `fromMap(Map<String,List<String>>)` | 从邻接表批量构建图（先 reset） |
| | `resetGraph()` | 清空图 |
| | `size()` | 返回节点数量 |
| `LinkedTable<T>` | `LinkedTable(int capacity)` 构造器 | 固定容量的数组链表 |
| | `insert(T value)` | 在尾部追加节点，容量满时抛出异常 |

---

## 使用示例

### 字符串与集合判断

```java
import org.carl.infrastructure.utils.StringUtils;
import org.carl.infrastructure.utils.CollectionUtils;
import org.carl.infrastructure.utils.FieldsUtils;

// 字符串
StringUtils.isEmpty(null);     // true
StringUtils.isEmpty("");       // true
StringUtils.isEmpty(" ");      // false
StringUtils.isNumeric("123");  // true
StringUtils.isNumeric("12.3"); // false

// 集合
List<String> list = List.of("a");
CollectionUtils.isEmpty(list);  // false
CollectionUtils.notEmpty(list); // true

// 字段类型判断
Object value = 42L;
FieldsUtils.isLong(value);          // true
FieldsUtils.isPrimitiveType(value); // true
FieldsUtils.typeOf(value);          // "Long"
```

### 日志截断

```java
import org.carl.infrastructure.utils.LogUtils;

String payload = "一段很长的 JSON 字符串...";
String short_ = LogUtils.toShortString(payload, 50);
// 超出 50 字符时尾部追加 "..."
```

### URL 解析

```java
import org.carl.infrastructure.utils.UrlParser;

UrlParser parser = new UrlParser("https://example.com:8080/api/v1?token=abc");
parser.getProtocol(); // "HTTPS"
parser.getHost();     // "example.com"
parser.getPort();     // 8080
parser.getPath();     // "/api/v1"
parser.getQuery();    // "token=abc"
```

### 数据脱敏

```java
import org.carl.infrastructure.utils.desensitization.MaskEmailAlgorithm;
import org.carl.infrastructure.utils.desensitization.MaskPhoneNumberAlgorithm;
import org.carl.infrastructure.utils.desensitization.MaskMiddleAlgorithm;

MaskEmailAlgorithm.INSTANCE.desensitize("alice@example.com");
// -> "a***e@example.com"

MaskPhoneNumberAlgorithm.INSTANCE.desensitize("15211117256");
// -> "152****7256"

MaskMiddleAlgorithm.INSTANCE.desensitize("张三丰");
// -> "张*丰"
```

也可以通过接口统一调用：

```java
import org.carl.infrastructure.utils.desensitization.IDesensitizationAlgorithm;

IDesensitizationAlgorithm algo = MaskPhoneNumberAlgorithm.INSTANCE;
String masked = algo.desensitize(rawPhone);
```

### DAG（有向无环图）

```java
import org.carl.infrastructure.utils.struct.DAG;

DAG dag = new DAG();
dag.addNode("A");
dag.addNode("B");
dag.addNode("C");
dag.addEdge("A", "B");
dag.addEdge("B", "C");

dag.topologicalSort();   // ["A", "B", "C"]
dag.predecessors("B");   // ["A"]
dag.downstream("B");     // ["C"]
dag.allLeaves();         // ["C"]

// 添加成环边会抛出 IllegalStateException
// dag.addEdge("C", "A"); // 抛出异常
```

### LinkedTable（固定容量链式表）

```java
import org.carl.infrastructure.utils.struct.LinkedTable;

LinkedTable<String> table = new LinkedTable<>(3);
table.insert("first");
table.insert("second");
table.insert("third");
System.out.println(table); // first -> second -> third
```

---

## 注意事项

- **`UrlParser`** 构造器声明了受检异常 `URISyntaxException`，调用方需捕获或向上抛出。仅支持 `HTTP / HTTPS / FTP / REDIS / JDBC / PULSAR / SMTP` 六种协议，其余协议会在构造阶段抛出 `UnsupportedOperationException`。
- **`MaskPhoneNumberAlgorithm`** 仅对长度恰好为 11 的字符串生效，不符合时原样返回，不抛异常。
- **`MaskEmailAlgorithm`** 本地部分长度 ≤ 1 时原样返回。
- **`DAG.addEdge`** 在插入前会对临时图做拓扑排序验证，成本为 O(V+E)，不适合在热路径上频繁调用。
- **`LinkedTable`** 容量在构造时固定，超出后抛出 `ArrayIndexOutOfBoundsException`，使用前需估算上限。
- 本模块无 Quarkus / CDI 依赖，可在任意 Java 21 项目中引用。
