# Product Guidelines

## Voice and Tone

专业且技术性 — 面向工程师，精确简洁。文档和注释应直接表达意图，避免冗余描述。

## Design Principles

- **简单优于功能** — 优先提供最小可用接口，避免过度设计
- **开箱即用** — 合理默认配置，减少使用方的手动配置负担
- **框架隔离** — 独立库模块不依赖 Quarkus，可在任意 Java 项目中使用

## API Design Standards

- 公共 API 必须有 Javadoc 说明
- 模块间通过接口解耦，避免直接依赖具体实现
- 破坏性变更需要版本号升级并在 CHANGELOG 中记录
