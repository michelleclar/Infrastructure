package org.carl.infrastructure.workflow.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.api.WorkflowExecutionException;
import org.carl.infrastructure.workflow.testing.WorkflowTestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

/**
 * Runs the same engine against a <b>real Temporal server</b>. Opt-in: only enabled when {@code
 * WORKFLOWV2_TEMPORAL_TARGET} is set, e.g.
 *
 * <pre>
 *   WORKFLOWV2_TEMPORAL_TARGET=180.184.66.147:31733 \
 *   ./gradlew :infrastructure-component-quarkus:workflow:test --rerun-tasks
 * </pre>
 *
 * No time-skipping on a real server, so only the signal-driven paths are exercised (no 24h await).
 * Still temporal-free at the test level — everything goes through {@link WorkflowTestKit}.
 */
@EnabledIfEnvironmentVariable(named = "WORKFLOWV2_TEMPORAL_TARGET", matches = ".+")
class LeaveProcessRemoteTest {

    private static final String TASK_QUEUE = "workflow-tasks";

    private WorkflowTestKit kit;

    @BeforeEach
    void setUp() {
        Recorder.reset();
        String target = System.getenv("WORKFLOWV2_TEMPORAL_TARGET");
        String namespace = System.getenv().getOrDefault("WORKFLOWV2_TEMPORAL_NAMESPACE", "default");
        kit = WorkflowTestKit.remote(target, namespace, TASK_QUEUE, new LeaveProcess());
    }

    @AfterEach
    void tearDown() {
        if (kit != null) {
            kit.close();
        }
    }

    @Test
    void approves_and_completes_on_real_server() {
        String biz = "normal-" + UUID.randomUUID();
        kit.start("leave", biz, new LeaveCtx("1", "boss"));
        kit.signal("leave", biz, LeaveEvent.APPROVE);
        kit.signal("leave", biz, LeaveEvent.CONFIRM);

        assertEquals(LeaveStatus.DONE, kit.awaitResult("leave", biz, LeaveStatus.class));
        assertTrue(Recorder.EVENTS.contains("deductBalance"));
        assertTrue(Recorder.EVENTS.contains("writeRecord"));
    }

    @Test
    void compensates_on_real_server() {
        Recorder.failWriteRecord.set(true);
        String biz = "comp-" + UUID.randomUUID();
        kit.start("leave", biz, new LeaveCtx("3", "boss"));
        kit.signal("leave", biz, LeaveEvent.APPROVE);
        kit.signal("leave", biz, LeaveEvent.CONFIRM);

        assertThrows(
                WorkflowExecutionException.class,
                () -> kit.awaitResult("leave", biz, LeaveStatus.class));
        assertTrue(Recorder.EVENTS.contains("restoreBalance:APPROVED:APPROVE"));
    }
}
