package org.carl.infrastructure.workflow.archive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.carl.infrastructure.workflow.definition.ExecutionRecord;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.runtime.WorkflowInstanceSnapshot;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.carl.ertool.infrastructure.jooq.generated.Tables.EXECUTION_RECORD;
import static org.carl.ertool.infrastructure.jooq.generated.Tables.WORKFLOW_INSTANCE;

/**
 * Database archival activities implementation using JOOQ.
 *
 * <p>Uses JOOQ-generated table classes for type-safe database operations. Maintains fast execution
 * to avoid blocking workflow termination.
 */
public class DatabaseArchiveActivities implements ArchiveActivities {

    private static final Logger log = LoggerFactory.getLogger(DatabaseArchiveActivities.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseArchiveActivities(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Override
    public void archive(WorkflowInstanceSnapshot snapshot) {
        log.info(
                "Archiving workflow to database using JOOQ: {} status: {}",
                snapshot.workflowId(),
                snapshot.status());

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            DSLContext dsl = DSL.using(conn, org.jooq.SQLDialect.POSTGRES);

            // Insert or update workflow instance
            upsertWorkflowInstance(dsl, snapshot);

            // Insert execution records
            insertExecutionRecords(dsl, snapshot);

            log.info(
                    "Successfully archived workflow: {} with {} execution records",
                    snapshot.workflowId(),
                    snapshot.executionRecords() != null ? snapshot.executionRecords().size() : 0);

        } catch (SQLException | JsonProcessingException e) {
            log.error("Failed to archive workflow: {}", snapshot.workflowId(), e);
            throw new RuntimeException("Archival failed for workflow: " + snapshot.workflowId(), e);
        }
    }

    private void upsertWorkflowInstance(DSLContext dsl, WorkflowInstanceSnapshot snapshot)
            throws SQLException, JsonProcessingException {

        // Convert JsonNode and Map to JSON strings
        JSON businessDataJson =
                snapshot.businessDataJson() != null
                        ? JSON.valueOf(snapshot.businessDataJson().toString())
                        : null;
        JSON finalVariablesJson =
                JSON.valueOf(MAPPER.writeValueAsString(snapshot.finalVariables()));

        dsl.insertInto(WORKFLOW_INSTANCE)
                .set(WORKFLOW_INSTANCE.WORKFLOW_ID, snapshot.workflowId())
                .set(WORKFLOW_INSTANCE.DEFINITION_ID, snapshot.definitionId())
                .set(WORKFLOW_INSTANCE.BUSINESS_KEY, snapshot.businessKey())
                .set(WORKFLOW_INSTANCE.STATUS, snapshot.status())
                .set(WORKFLOW_INSTANCE.STARTED_AT, toLocalDateTime(snapshot.startedAt()))
                .set(WORKFLOW_INSTANCE.ENDED_AT, toLocalDateTime(snapshot.endedAt()))
                .set(WORKFLOW_INSTANCE.FINAL_NODE_ID, snapshot.finalNodeId())
                .set(WORKFLOW_INSTANCE.FINAL_OUTCOME, snapshot.finalOutcome())
                .set(WORKFLOW_INSTANCE.FINAL_STATUS, snapshot.finalStatus())
                .set(WORKFLOW_INSTANCE.BUSINESS_DATA_JSON, businessDataJson)
                .set(WORKFLOW_INSTANCE.FINAL_VARIABLES_JSON, finalVariablesJson)
                .onConflict(WORKFLOW_INSTANCE.WORKFLOW_ID)
                .doUpdate()
                .set(WORKFLOW_INSTANCE.DEFINITION_ID, DSL.excluded(WORKFLOW_INSTANCE.DEFINITION_ID))
                .set(WORKFLOW_INSTANCE.BUSINESS_KEY, DSL.excluded(WORKFLOW_INSTANCE.BUSINESS_KEY))
                .set(WORKFLOW_INSTANCE.STATUS, DSL.excluded(WORKFLOW_INSTANCE.STATUS))
                .set(WORKFLOW_INSTANCE.ENDED_AT, DSL.excluded(WORKFLOW_INSTANCE.ENDED_AT))
                .set(WORKFLOW_INSTANCE.FINAL_NODE_ID, DSL.excluded(WORKFLOW_INSTANCE.FINAL_NODE_ID))
                .set(WORKFLOW_INSTANCE.FINAL_OUTCOME, DSL.excluded(WORKFLOW_INSTANCE.FINAL_OUTCOME))
                .set(WORKFLOW_INSTANCE.FINAL_STATUS, DSL.excluded(WORKFLOW_INSTANCE.FINAL_STATUS))
                .set(
                        WORKFLOW_INSTANCE.BUSINESS_DATA_JSON,
                        DSL.excluded(WORKFLOW_INSTANCE.BUSINESS_DATA_JSON))
                .set(
                        WORKFLOW_INSTANCE.FINAL_VARIABLES_JSON,
                        DSL.excluded(WORKFLOW_INSTANCE.FINAL_VARIABLES_JSON))
                .execute();
    }

    private void insertExecutionRecords(DSLContext dsl, WorkflowInstanceSnapshot snapshot)
            throws SQLException {
        if (snapshot.executionRecords() == null || snapshot.executionRecords().isEmpty()) {
            return;
        }

        for (ExecutionRecord record : snapshot.executionRecords()) {
            NodeResult result = record.result();

            String resultJson = null;
            try {
                resultJson =
                        result.payload() != null && !result.payload().isEmpty()
                                ? MAPPER.writeValueAsString(result.payload())
                                : null;
            } catch (JsonProcessingException e) {
                log.warn("Failed to convert payload to JSON for node: {}", record.nodeId(), e);
            }

            JSONB resultJsonB = null;
            if (resultJson != null && !resultJson.isEmpty()) {
                resultJsonB = JSONB.valueOf(resultJson);
            }

            dsl.insertInto(EXECUTION_RECORD)
                    .set(EXECUTION_RECORD.WORKFLOW_ID, snapshot.workflowId())
                    .set(EXECUTION_RECORD.NODE_ID, record.nodeId())
                    .set(EXECUTION_RECORD.VISIT_NO, record.visitNo())
                    .set(EXECUTION_RECORD.OUTCOME, result.outcome())
                    .set(EXECUTION_RECORD.STATUS, result.status().name())
                    .set(EXECUTION_RECORD.RESULT_JSON, resultJsonB)
                    .set(EXECUTION_RECORD.EXECUTED_AT, toLocalDateTime(snapshot.endedAt()))
                    .execute();
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
