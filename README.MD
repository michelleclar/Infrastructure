## component

| component name | effect          | dependencies                                                  |
|----------------|-----------------|---------------------------------------------------------------|
| dto            | 定义数据传输格式        | 无                                                             |
| authorization  | 提供身份验证和权限控制功能   | quarkus-oidc, quarkus-keycloak                                |
| broadcast      | 提供消息广播和事件总线功能   | quarkus-vertx                                                 |
| cache          | 提供本地和分布式缓存功能    | quarkus-cache, quarkus-redis                                  |
| discover       | 提供服务发现功能        | consul, consul-stork, consul-client                           |
| persistence    | 提供数据持久化和数据库操作功能 | quarkus-jdbc-postgresql, jdbi-core, quarkus-agroal, jooq等     |
| search         | 提供全文搜索功能        | quarkus-elasticsearch                                         |
| tool           | 提供通用工具类和辅助功能    | guava, commons-collections4等                                  |
| web            | 提供Web服务         | quarkus-rest, quarkus-jackson, quarkus-reactive-routes等       |
| pulsar         | 提供消息队列和事件流处理功能  | quarkus-pulsar                                                |
| metrics        | 提供应用监控和指标收集功能   | quarkus-observability-devservices-lgtm, quarkus-opentelemetry |
| workflow       | 提供工作流编排和执行功能    | quarkus-temporal                                              |
| statemachine   | 状态机功能           | 无                                                             |

## 完成度

- [x] dto
- [x] authorization
- [x] broadcast
- [x] cache
- [ ] discover
- [x] persistence
- [ ] search
- [ ] tool
- [x] web
- [ ] pulsar
- [x] metrics
- [x] workflow
- [ ] statemachine