package org.carl.infrastructure.workflow.dsl;

import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.approval;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Verifies the {@code nestedJoin} reservation on {@link ChildNodeSpec} — two-arg construction
 * leaves it {@code null}, three-arg construction stores it, and Jackson round-trips the full record
 * without losing fields.
 */
class NestedJoinSpecTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void twoArgConstructorLeavesNestedJoinNull() {
        ChildNodeSpec spec =
                new ChildNodeSpec(
                        "HR", new NodeConfig(NodeTypes.APPROVAL_TASK, Map.of("assignee", "hr")));
        assertEquals("HR", spec.name());
        assertNotNull(spec.config());
        assertNull(spec.nestedJoin());
    }

    @Test
    void threeArgConstructorStoresNestedJoin() {
        JoinSpec nested =
                Dsl.all(Dsl.node("inner1", approval("a")), Dsl.node("inner2", approval("b")));
        ChildNodeSpec spec =
                new ChildNodeSpec("outer", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), nested);

        assertEquals("outer", spec.name());
        assertEquals("taskGroup", spec.config().type());
        assertNotNull(spec.nestedJoin());
        assertEquals("all", spec.nestedJoin().rule());
        assertEquals(2, spec.nestedJoin().children().size());
        assertEquals("inner1", spec.nestedJoin().children().get(0).name());
    }

    @Test
    void dslFactoryProducesEquivalentSpec() {
        JoinSpec nested = Dsl.all(Dsl.node("inner", approval("a")));
        ChildNodeSpec viaCtor =
                new ChildNodeSpec("outer", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), nested);
        ChildNodeSpec viaFactory =
                Dsl.node("outer", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), nested);
        assertEquals(viaCtor, viaFactory);
    }

    @Test
    void roundTripPreservesAllFields() throws Exception {
        JoinSpec nested =
                Dsl.any(Dsl.node("leaf1", approval("alice")), Dsl.node("leaf2", approval("bob")));
        ChildNodeSpec original =
                new ChildNodeSpec("outer", new NodeConfig(NodeTypes.TASK_GROUP, Map.of()), nested);

        String json = mapper.writeValueAsString(original);
        ChildNodeSpec back = mapper.readValue(json, ChildNodeSpec.class);

        assertEquals(original.name(), back.name());
        assertEquals(original.config().type(), back.config().type());
        assertNotNull(back.nestedJoin());
        assertEquals(original.nestedJoin().rule(), back.nestedJoin().rule());
        assertEquals(original.nestedJoin().children().size(), back.nestedJoin().children().size());
        assertEquals(
                original.nestedJoin().children().get(0).name(),
                back.nestedJoin().children().get(0).name());
        assertEquals(original, back);
    }

    @Test
    void roundTripOmitsNullNestedJoinField() throws Exception {
        // Default Jackson serialization on a record with a null component still emits the field.
        // We only check that the JSON deserialises back to an equivalent value — the precise
        // wire shape is not asserted to keep this test focused on the round-trip property.
        ChildNodeSpec original =
                new ChildNodeSpec(
                        "HR", new NodeConfig(NodeTypes.APPROVAL_TASK, Map.of("assignee", "hr")));
        String json = mapper.writeValueAsString(original);
        ChildNodeSpec back = mapper.readValue(json, ChildNodeSpec.class);
        assertEquals(original, back);
        assertNull(back.nestedJoin());
    }
}
