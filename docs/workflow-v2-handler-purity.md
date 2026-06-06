# Workflow V2 — NodeHandler Purity Contract

`NodeHandler` is the open SPI plug-point in workflow v2. Built-in handlers ship with the
infrastructure module; business teams will write their own. This note explains why a custom
handler must be deterministic, what that excludes, how to do IO safely, and how to verify it.

## Why purity matters

V2 runs on Temporal. Temporal persists every workflow decision and *replays* the workflow code
during recovery (new worker pickup, history compaction, version upgrade). Replay must produce
the same decisions as the original execution; otherwise Temporal's state machine diverges and the
workflow instance corrupts.

`NodeHandler.run(...)`, `onEvent(...)`, `canAccept(...)`, `compensate(...)` and the metadata
methods (`type()`, `configType()`, `outcomes()`, `compensable()`) all execute inside the
workflow thread. They are part of the replayed decision logic. **They must be deterministic.**

## Forbidden inside a handler

Any of the following will eventually break replay:

- wall-clock time — `System.currentTimeMillis`, `System.nanoTime`, `Instant.now`, `new Date()`,
  `LocalDateTime.now`, `Clock.systemDefaultZone`;
- I/O — HTTP, file system, JDBC, Kafka, gRPC, anything that crosses a process boundary;
- non-deterministic randomness — `Math.random`, `UUID.randomUUID`, `SecureRandom`;
- blocking — `Thread.sleep`, `CountDownLatch.await`, `Future.get` with timeouts;
- mutable shared state — mutable statics, environment variables, system properties;
- throwing from `run()`/`onEvent()` for non-fatal cases — return `NodeResult.failed(message)`
  instead.

## Recommended pattern: delegate side-effects to the runtime

When a node genuinely needs IO, time, or randomness, the handler must remain pure and *describe*
the side-effect via `NodeResult.payload()` keyed by `RuntimeIntents`. The runtime invokes the
intent **outside** the deterministic replay boundary (as a Temporal activity or runtime call) and
delivers the result back via an internal event.

### Anti-pattern: handler performs IO inline

```java
public NodeResult run(NodeExecutionContext ctx, FetchUserConfig cfg) {
    // BAD: HTTP call on the workflow thread. Breaks replay.
    UserDto user = httpClient.get("/users/" + cfg.userId());
    ctx.variables().put("user", user);          // BAD: mutating context directly.
    return NodeResult.completed(Outcomes.SUCCESS);
}
```

### Correct: describe the side-effect, let the runtime do it

```java
public NodeResult run(NodeExecutionContext ctx, FetchUserConfig cfg) {
    Map<String, Object> intent = new LinkedHashMap<>();
    intent.put(RuntimeIntents.ACTIVITY, "fetchUserInfo");
    intent.put(RuntimeIntents.ACTIVITY_INPUT, Map.of("userId", cfg.userId()));
    return new NodeResult(NodeStatus.WAITING, null, intent, null);
}

@Override
public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, FetchUserConfig cfg) {
    return event != null && ServiceTaskHandler.ACTIVITY_RESULT_EVENT.equals(event.name());
}

@Override
public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, FetchUserConfig cfg) {
    String status = event.payload().path("status").asText(null);
    return "success".equalsIgnoreCase(status)
            ? NodeResult.completed(Outcomes.SUCCESS)
            : NodeResult.failed(event.payload().path("message").asText("fetch failed"));
}
```

The business team registers the activity implementation (`fetchUserInfo`) once on the Temporal
worker (see `V2BusinessActivityRegistry`). The handler stays a pure function of `(ctx, config)`.

`ServiceTaskHandler` and `ApprovalTaskHandler` follow this pattern — read them as references.

## Static lint: `DeterminismGuard`

`DeterminismGuard` is a best-effort bytecode lint that scans the handler's constant pool for
references to forbidden JDK types and methods (`System`, `UUID`, `Random`, `HttpClient`, `File`,
`Thread.sleep`, `Math.random`, etc.).

`NodeHandlerRegistry.register()` always calls `DeterminismGuard.assertPure()` — no opt-in
needed. Any user-registered handler that references a forbidden symbol throws
`IllegalStateException` at registration time:

```java
NodeHandlerRegistry registry = new NodeHandlerRegistry();
registry.register(new MyServiceHandler()); // always checked; throws on violation
```

Built-in handlers bypass the scan via `registerBuiltIn()`, which is the trusted path reserved
for handlers shipped with the infrastructure module.

`NodeHandlerRegistry.strict()` and `isStrict()` are kept only for backward compatibility;
`isStrict()` always returns `true`.

Or scan manually for diagnostics:

```java
List<String> violations = DeterminismGuard.staticScan(MyServiceHandler.class);
```

**Limits.** The lint is intentionally simple and has both false positives (e.g. a `Class`
reference to `java.lang.System` for a benign reason will be flagged) and false negatives
(reflection, JDK calls hidden inside helper classes, lambda bodies in other compilation units,
injected services hidden behind interfaces). It complements — not replaces — code review.

## Testing a custom handler

Unit-test the handler against a hand-rolled `NodeExecutionContext` test double:

- inject deterministic `businessData()` / `variables()`;
- assert on the returned `NodeResult.status()`, `outcome()`, and `payload()` intents;
- do NOT spin up Temporal — handlers are pure functions, not workflows.

For end-to-end replay safety, run your workflow under Temporal's `WorkflowReplayer` against
recorded histories in your integration suite.

## Quick checklist

- [ ] Handler reads only from `NodeExecutionContext` and `config`.
- [ ] No `now()`, no `random*`, no `Thread.sleep`, no IO inline.
- [ ] Variable mutations expressed as payload intents, not direct `ctx.variables().put(...)`.
- [ ] `type()`, `configType()`, `outcomes()` return stable constants.
- [ ] `NodeHandlerRegistry.register()` always runs `DeterminismGuard.assertPure()` — no extra setup needed.
