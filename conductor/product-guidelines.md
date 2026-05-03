# Product Guidelines

## Voice and Tone

专业且技术性 — 面向工程师，精确简洁。文档和注释应直接表达意图，避免冗余描述。

Documentation, API Javadoc, error messages, and log output should be precise and unambiguous. Prefer exact terminology over casual language. Assume the reader is a competent backend engineer.

## Design Principles

- **简单优于功能** — 优先提供最小可用接口，避免过度设计
- **开箱即用** — 合理默认配置，减少使用方的手动配置负担；modules work out-of-the-box with zero mandatory configuration where possible
- **框架隔离** — 独立库模块不依赖 Quarkus，可在任意 Java 项目中使用
- **Fail fast** — 配置错误应在启动时暴露，而非在运行时才触发
- **Consistent patterns** — standalone and Quarkus modules should feel like the same library; share interfaces and DTOs where possible

## Module Boundary Rules

- **Quarkus modules** may depend on standalone modules, never the reverse.
- Standalone modules must not introduce a transitive Quarkus dependency (even optional).
- Each module should have a single, clearly stated responsibility.

## API Design Standards

- 公共 API 必须有 Javadoc 说明
- Prefer interface-based APIs over concrete class exposure
- Mark implementation classes `@Internal` (or package-private) where they are not intended for direct use
- 破坏性变更需要版本号升级并在 CHANGELOG 中记录
