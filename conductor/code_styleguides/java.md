# Java Style Guide

## Formatting

- Indent: 4 spaces（不使用 Tab）
- Line length: 最大 120 字符
- 大括号: Allman 风格（`{` 与声明同行）
- 空行: 方法间保留一个空行

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Class | UpperCamelCase | `RedisClient`, `MqProducer` |
| Method | lowerCamelCase | `sendMessage()`, `acquireLock()` |
| Field | lowerCamelCase | `connectionPool`, `retryCount` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT`, `MAX_RETRY` |
| Package | lowercase, dot-separated | `org.carl.infrastructure.redis` |

## Code Organization

- 每个文件只定义一个顶级类
- 字段声明在构造函数之前
- 静态成员在实例成员之前
- 接口优先于抽象类

## Comments & Documentation

- 公共 API（`public` 方法/类）必须有 Javadoc
- Javadoc 使用中文或英文均可，保持项目内一致
- 内部实现逻辑注释仅在 WHY 不明显时添加，不解释 WHAT
- 禁止提交注释掉的代码

```java
/**
 * 获取分布式锁，超时自动释放。
 *
 * @param key     锁的唯一标识
 * @param timeout 持锁超时时间
 * @return 是否成功获取锁
 */
public boolean acquireLock(String key, Duration timeout) { ... }
```

## Error Handling

- 业务异常使用自定义 unchecked exception，继承 `RuntimeException`
- 不吞掉异常（catch 后必须记录日志或重新抛出）
- 不使用异常控制正常业务流程

## Module Design

- 独立库模块：不得引入 Quarkus 依赖
- Quarkus 集成模块：通过 `@ApplicationScoped` 等 CDI 注解暴露 Bean
- 跨模块调用通过接口，不直接依赖实现类

## Testing

- 测试类命名：`XxxTest`（单元测试）、`XxxIT`（集成测试）
- 每个测试方法只验证一个行为
- 使用 JUnit 5 + Mockito；Quarkus 模块使用 `@QuarkusTest`
- 测试方法命名：`should_<expected>_when_<condition>`

```java
@Test
void should_return_empty_when_key_not_exists() { ... }
```
