# Workflow

## TDD Policy

**Strict — tests required before implementation.**

- Write the test first. No implementation code is merged without a corresponding test written beforehand.
- Unit tests cover all public API methods.
- Integration tests are required for any module that interacts with external systems (DB, Redis, Pulsar, etc.).
- A task is not considered complete until all tests pass (`./gradlew test`).

## Commit Strategy

**Conventional Commits** — all commit messages must follow the [Conventional Commits 1.0.0](https://www.conventionalcommits.org/) specification.

Common types used in this project:

| Type | When to use |
|------|-------------|
| `feat` | New module or public API addition |
| `fix` | Bug fix in an existing module |
| `refactor` | Code restructuring without behaviour change |
| `test` | Adding or fixing tests |
| `docs` | Documentation only |
| `build` | Gradle build scripts, dependency updates |
| `chore` | Maintenance (CI, tooling, formatting) |
| `perf` | Performance improvement |

Scope should reference the affected module, e.g. `feat(redis): add distributed lock timeout option`.

Breaking changes must include `BREAKING CHANGE:` in the commit footer.

## Code Review

**Required for all changes.**

- Every change must go through a pull request; direct pushes to `main` are not permitted.
- At least one approval is required before merge.
- Reviewer must verify: tests present, API boundaries respected, no unintended Quarkus dependency leakage into standalone modules.

## Verification Checkpoints

**After each task completion.**

Before marking a task done, the implementer must:

1. Run `./gradlew build` — build must succeed with no errors.
2. Run `./gradlew test` — all tests must pass.
3. Manually verify the change behaves as specified in the task description.
4. Confirm no regressions in dependent modules (`./gradlew :affected-module:test`).

## Task Lifecycle

```
pending → in_progress → [manual verification] → completed
```

A task moves to `in_progress` when work begins. It moves to `completed` only after verification passes. If verification fails, it stays `in_progress` until resolved.

## Branch Strategy

- Feature branches: `feat/<scope>-<short-description>`
- Bug fix branches: `fix/<scope>-<short-description>`
- Base branch for PRs: `main`
