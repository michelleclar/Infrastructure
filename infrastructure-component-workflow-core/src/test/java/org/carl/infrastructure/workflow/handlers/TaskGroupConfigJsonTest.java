package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.JoinRule;
import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.TaskGroupChild;
import org.junit.jupiter.api.Test;

import java.util.List;

class TaskGroupConfigJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void joinRuleSerializesToLowercaseWireName() throws Exception {
        String json = mapper.writeValueAsString(JoinRule.ALL);
        assertEquals("\"all\"", json);
        assertEquals("\"any\"", mapper.writeValueAsString(JoinRule.ANY));
    }

    @Test
    void joinRuleAcceptsLowercaseAndUppercaseOnRead() throws Exception {
        assertEquals(JoinRule.ALL, mapper.readValue("\"all\"", JoinRule.class));
        assertEquals(JoinRule.ANY, mapper.readValue("\"ANY\"", JoinRule.class));
    }

    @Test
    void unknownJoinRuleFailsLoudly() {
        assertThrows(Exception.class, () -> mapper.readValue("\"foo\"", JoinRule.class));
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        String json = "{\"join\":\"all\",\"tasks\":[],\"extraField\":\"x\"}";
        TaskGroupConfig cfg = mapper.readValue(json, TaskGroupConfig.class);
        assertEquals(JoinRule.ALL, cfg.join());
        assertTrue(cfg.tasks().isEmpty());
    }

    @Test
    void roundTripPreservesChildren() throws Exception {
        TaskGroupConfig original =
                new TaskGroupConfig(
                        JoinRule.ANY,
                        List.of(new TaskGroupChild("hr", "HR", "approvalTask", null)));
        String json = mapper.writeValueAsString(original);
        TaskGroupConfig back = mapper.readValue(json, TaskGroupConfig.class);
        assertEquals(original, back);
    }

    @Test
    void taskGroupChildConfigSurvivesAsJsonNode() throws Exception {
        String json =
                "{\"join\":\"all\",\"tasks\":[{\"id\":\"hr\",\"type\":\"approvalTask\",\"config\":{\"assignee\":\"alice\"}}]}";
        TaskGroupConfig cfg = mapper.readValue(json, TaskGroupConfig.class);
        TaskGroupChild child = cfg.tasks().get(0);
        JsonNode innerCfg = child.config();
        assertEquals("alice", innerCfg.path("assignee").asText());
        assertNull(child.label());
    }
}
