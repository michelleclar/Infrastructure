package org.carl.infrastructure.workflow.dsl;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Static factory helpers for the join-spec and {@link ChildNodeSpec} layer of the flow-first DSL.
 * Import statically:
 *
 * <pre>
 * import static org.carl.infrastructure.workflow.dsl.Dsl.*;
 * </pre>
 *
 * <p>Available factories:
 *
 * <ul>
 *   <li>{@link #node(String, NodeConfig)} — {@link ChildNodeSpec} factory (POJO form)
 *   <li>{@link #node(String, Consumer)} — {@link ChildNodeSpec} factory (builder form)
 *   <li>{@link #node(String, NodeConfig, JoinSpec)} — {@link ChildNodeSpec} with nested join (POJO
 *       form)
 *   <li>{@link #node(String, Consumer, JoinSpec)} — {@link ChildNodeSpec} with nested join (builder
 *       form)
 *   <li>{@link #all(ChildNodeSpec...)} — join spec requiring all children to complete
 *   <li>{@link #any(ChildNodeSpec...)} — join spec requiring any one child to complete
 * </ul>
 */
public final class Dsl {

    private Dsl() {
        throw new AssertionError("no instances");
    }

    /**
     * Creates a {@link ChildNodeSpec} for use inside {@link #all(ChildNodeSpec...)} or {@link
     * #any(ChildNodeSpec...)}.
     */
    public static ChildNodeSpec node(String name, NodeConfig config) {
        return new ChildNodeSpec(name, config);
    }

    /**
     * Creates a {@link ChildNodeSpec} with a nested join spec. The nested join is reserved for
     * future nested-taskGroup runtime support; see {@link ChildNodeSpec#nestedJoin()}.
     */
    public static ChildNodeSpec node(String name, NodeConfig config, JoinSpec nestedJoin) {
        return new ChildNodeSpec(name, config, nestedJoin);
    }

    /**
     * Builder-form factory for {@link ChildNodeSpec}. Lets taskGroup children be declared with the
     * same {@link NodeBuilder} pattern as top-level nodes.
     */
    public static ChildNodeSpec node(String name, Consumer<NodeBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer");
        NodeBuilder b = new NodeBuilder();
        configurer.accept(b);
        return new ChildNodeSpec(name, b.buildConfig(), null);
    }

    /** Builder-form factory for {@link ChildNodeSpec} with a nested join spec. */
    public static ChildNodeSpec node(
            String name, Consumer<NodeBuilder> configurer, JoinSpec nestedJoin) {
        Objects.requireNonNull(configurer, "configurer");
        NodeBuilder b = new NodeBuilder();
        configurer.accept(b);
        return new ChildNodeSpec(name, b.buildConfig(), nestedJoin);
    }

    /** Creates a {@link JoinSpec} requiring <em>all</em> child tasks to complete. */
    public static JoinSpec all(ChildNodeSpec... nodes) {
        return new JoinSpec("all", Arrays.asList(nodes));
    }

    /** Creates a {@link JoinSpec} requiring <em>any</em> child task to complete (short-circuit). */
    public static JoinSpec any(ChildNodeSpec... nodes) {
        return new JoinSpec("any", Arrays.asList(nodes));
    }

    /**
     * Creates a placeholder {@code taskGroup} {@link NodeConfig}. The actual join spec is supplied
     * via {@link FlowFrom#join(JoinSpec)} and overrides this placeholder at {@link FlowDef#build()}
     * time.
     *
     * <p>Package-private: only used internally by {@link FlowFrom#join}.
     */
    static NodeConfig taskGroup() {
        return new NodeConfig(
                org.carl.infrastructure.workflow.spi.NodeTypes.TASK_GROUP, java.util.Map.of());
    }
}
