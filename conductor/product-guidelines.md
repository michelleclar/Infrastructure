# Product Guidelines

## Voice and Tone

**Professional and technical.**

Documentation, API Javadoc, error messages, and log output should be precise and unambiguous. Prefer exact terminology over casual language. Assume the reader is a competent backend engineer.

## Design Principles

### Developer Experience Focused

Every API surface, configuration option, and error message must reduce friction for the consuming developer:

- Sensible defaults — modules work out-of-the-box with zero mandatory configuration where possible.
- Explicit over implicit — when behaviour deviates from defaults, make it obvious via clear naming and documentation.
- Fail fast and loudly — misconfiguration should surface at startup, not at runtime under load.
- Consistent patterns — standalone and Quarkus modules should feel like the same library; share interfaces and DTOs where possible.

## Module Boundary Rules

- **Quarkus modules** may depend on standalone modules, never the reverse.
- Standalone modules must not introduce a transitive Quarkus dependency (even optional).
- Each module should have a single, clearly stated responsibility.

## API Design Standards

- Prefer interface-based APIs over concrete class exposure.
- Mark implementation classes `@Internal` (or package-private) where they are not intended for direct use.
- Breaking changes to public APIs require a version bump and a migration note in the module's changelog.
