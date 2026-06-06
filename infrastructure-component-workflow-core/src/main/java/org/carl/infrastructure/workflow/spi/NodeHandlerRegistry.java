package org.carl.infrastructure.workflow.spi;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry of {@link NodeHandler}s keyed by {@link NodeHandler#type()}.
 *
 * <p>Lookup semantics:
 *
 * <ul>
 *   <li>{@link #register(NodeHandler)}: always calls {@link DeterminismGuard#assertPure(Class)}
 *       before storing the handler. Use {@link #registerBuiltIn(NodeHandler)} to bypass the guard
 *       for trusted built-in handlers.
 *   <li>{@link #registerBuiltIn(NodeHandler)}: trusted path — skips the determinism guard. Used by
 *       {@link org.carl.infrastructure.workflow.handlers.BuiltInHandlers}.
 *   <li>{@link #lookup(String)}: throws {@link IllegalArgumentException} if absent.
 *   <li>{@link #find(String)}: returns {@link Optional#empty()} if absent.
 * </ul>
 *
 * <p>The {@link #strict()} method is retained for backward compatibility but is now a no-op: {@link
 * #register(NodeHandler)} is always strict.
 */
public final class NodeHandlerRegistry {

    private final ConcurrentMap<String, NodeHandler<?>> handlers = new ConcurrentHashMap<>();
    private volatile boolean strictDeterminismCheck = true;

    /**
     * Retained for backward compatibility. Has no effect: {@link #register(NodeHandler)} is always
     * strict.
     *
     * @return this registry for chaining.
     */
    public NodeHandlerRegistry strict() {
        this.strictDeterminismCheck = true;
        return this;
    }

    /**
     * @return whether {@link #strict()} has been enabled on this registry (always {@code true}).
     */
    public boolean isStrict() {
        return strictDeterminismCheck;
    }

    /**
     * Register a user-provided handler. Always calls {@link DeterminismGuard#assertPure(Class)}
     * before storing. Throws {@link IllegalStateException} if the guard finds violations or if the
     * type is already registered.
     */
    @SuppressWarnings("unchecked")
    public void register(NodeHandler<?> handler) {
        Objects.requireNonNull(handler, "handler");
        String type = validateType(handler);
        DeterminismGuard.assertPure((Class<? extends NodeHandler<?>>) handler.getClass());
        putHandler(type, handler);
    }

    /**
     * Trusted path for built-in handlers. Skips the {@link DeterminismGuard} scan. Throws {@link
     * IllegalStateException} if the type is already registered.
     */
    public void registerBuiltIn(NodeHandler<?> handler) {
        Objects.requireNonNull(handler, "handler");
        String type = validateType(handler);
        putHandler(type, handler);
    }

    public NodeHandler<?> lookup(String type) {
        Objects.requireNonNull(type, "type");
        NodeHandler<?> handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("No NodeHandler registered for type: " + type);
        }
        return handler;
    }

    public Optional<NodeHandler<?>> find(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(type));
    }

    public Set<String> registeredTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    private static String validateType(NodeHandler<?> handler) {
        String type = handler.type();
        Objects.requireNonNull(type, "handler.type()");
        if (type.isBlank()) {
            throw new IllegalArgumentException("handler.type() must not be blank");
        }
        return type;
    }

    private void putHandler(String type, NodeHandler<?> handler) {
        NodeHandler<?> previous = handlers.putIfAbsent(type, handler);
        if (previous != null) {
            throw new IllegalStateException("NodeHandler already registered for type: " + type);
        }
    }
}
