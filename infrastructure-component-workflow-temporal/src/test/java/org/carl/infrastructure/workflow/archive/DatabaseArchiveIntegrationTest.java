package org.carl.infrastructure.workflow.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.BuiltInNodeType;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.definition.NodeStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.client.WorkflowClientOptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration test for workflow archival using real PostgreSQL and Temporal.
 *
 * <p>Requirements:
 *
 * <ul>
 *   <li>PostgreSQL: jdbc:postgresql://180.184.66.147:31432/db (user: root, password: root)
 *   <li>Temporal: 180.184.66.147:31733
 * </ul>
 *
 * <p>This test:
 *
 * <ol>
 *   <li>Creates workflow_instance and execution_record tables
 *   <li>Starts a Temporal worker connected to the remote server
 *   <li>Executes a sample workflow that terminates
 *   <li>Verifies the workflow data is persisted to PostgreSQL
 * </ol>
 */
class DatabaseArchiveIntegrationTest {

    private static final String TEMPORAL_HOST = "180.184.66.147";
    private static final int TEMPORAL_PORT = 31733;
    private static final String DB_URL = "jdbc:postgresql://180.184.66.147:31432/db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private static final String TASK_QUEUE = "DATABASE_ARCHIVE_INTEGRATION_TEST";

    private WorkflowServiceStubs service;
    private WorkerFactory workerFactory;
    private WorkflowClient client;
    private ObjectMapper mapper;

    @BeforeAll
    static void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                Statement stmt = conn.createStatement()) {

            // Drop tables if they exist (clean state)
            stmt.execute("DROP TABLE IF EXISTS execution_record CASCADE");
            stmt.execute("DROP TABLE IF EXISTS workflow_instance CASCADE");

            // Create tables
            stmt.execute(
                    """
                CREATE TABLE IF NOT EXISTS workflow_instance (
                    workflow_id       VARCHAR(255) PRIMARY KEY,
                    definition_id     VARCHAR(255) NOT NULL,
                    business_key      VARCHAR(255),
                    status            VARCHAR(50)  NOT NULL,
                    started_at        TIMESTAMP    NOT NULL,
                    ended_at          TIMESTAMP,
                    final_node_id     VARCHAR(255),
                    final_outcome     VARCHAR(100),
                    final_status      VARCHAR(50),
                    business_data_json JSON,
                    final_variables_json JSON
                )
            """);

            stmt.execute(
                    """
                CREATE TABLE IF NOT EXISTS execution_record (
                    id              BIGSERIAL PRIMARY KEY,
                    workflow_id     VARCHAR(255) NOT NULL,
                    node_id         VARCHAR(255) NOT NULL,
                    visit_no        INT          NOT NULL,
                    outcome         VARCHAR(100),
                    status          VARCHAR(50)  NOT NULL,
                    result_json     JSONB,
                    executed_at     TIMESTAMP    NOT NULL,
                    FOREIGN KEY (workflow_id) REFERENCES workflow_instance(workflow_id) ON DELETE CASCADE
                )
            """);

            // Create indexes
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_workflow_instance_definition ON workflow_instance(definition_id)");
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_workflow_instance_business_key ON workflow_instance(business_key)");
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_workflow_instance_status ON workflow_instance(status)");
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_execution_record_workflow ON execution_record(workflow_id)");

            System.out.println("Database tables created successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to create database tables", e);
        }
    }

    @BeforeEach
    void setUp() {
        // Connect to remote Temporal server
        WorkflowServiceStubsOptions serviceOptions =
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(TEMPORAL_HOST + ":" + TEMPORAL_PORT)
                        .build();

        service = WorkflowServiceStubs.newInstance(serviceOptions);

        // Create workflow client
        WorkflowClientOptions clientOptions =
                WorkflowClientOptions.newBuilder().setNamespace("default").build();

        client = WorkflowClient.newInstance(service, clientOptions);

        // Create worker factory
        workerFactory = WorkerFactory.newInstance(client);

        // Create worker
        Worker worker = workerFactory.newWorker(TASK_QUEUE);

        // Setup workflow runtime
        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        BusinessActivityRegistry activityRegistry = buildTestRegistry();

        // Use JOOQ-based archival to write to database
        DatabaseArchiveActivities archiveActivities =
                new DatabaseArchiveActivities(DB_URL, DB_USER, DB_PASSWORD);
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, archiveActivities);

        // Start worker
        workerFactory.start();

        mapper = ObjectMapperHolder.mapper();
    }

    @AfterEach
    void tearDown() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            workerFactory.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    @Timeout(60)
    void workflowWithDatabaseArchivesSuccessfully() throws Exception {
        WorkflowDefinition def = buildSimpleFlow();
        WorkflowInput input = newInput(def);

        String workflowId = "db-archive-test-" + UUID.randomUUID();

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId(workflowId)
                                .build());

        // Start workflow
        io.temporal.client.WorkflowClient.start(workflow::execute, input);

        // Let the workflow reach the approval waiting state
        Thread.sleep(2000);

        // Signal approval
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved")));

        // Wait for workflow completion
        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());

        // Give archival time to complete
        Thread.sleep(2000);

        // Verify archival in PostgreSQL
        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs =
                        stmt.executeQuery(
                                "SELECT workflow_id, status, final_node_id FROM workflow_instance WHERE workflow_id = '"
                                        + workflowId
                                        + "'")) {

            assertTrue(rs.next(), "Workflow should be archived in database");
            assertEquals(workflowId, rs.getString("workflow_id"));
            assertEquals("COMPLETED", rs.getString("status"));
            assertEquals("completed", rs.getString("final_node_id"));

            // Check execution records
            try (java.sql.Statement stmt2 = conn.createStatement();
                    java.sql.ResultSet rs2 =
                            stmt2.executeQuery(
                                    "SELECT COUNT(*) as cnt FROM execution_record WHERE workflow_id = '"
                                            + workflowId
                                            + "'")) {
                assertTrue(rs2.next());
                int recordCount = rs2.getInt("cnt");
                assertTrue(recordCount >= 2, "Should have at least 2 execution records");
                System.out.println("Found " + recordCount + " execution records in database");
            }
        }

        System.out.println("✅ Workflow completed and archived to PostgreSQL: " + workflowId);
        System.out.println("Final status: " + result.finalStatus());
        System.out.println("Final node: " + result.finalNodeId());
    }

    // ---- helpers -----------------------------------------------------------------

    private static WorkflowDefinition buildSimpleFlow() {
        FlowDef flow = Flow.define("leaveDB", "请假数据库归档测试");
        flow.start("requestLeave");
        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node("leaveApproval", BuiltInNodes.approval("manager"));
        flow.node("completed", b -> b.type(BuiltInNodeType.END_TASK));
        flow.node("rejected", b -> b.type(BuiltInNodeType.END_TASK));

        flow.from("requestLeave").on("SUCCESS").to("leaveApproval");
        flow.from("leaveApproval").on("APPROVED").to("completed");
        flow.from("leaveApproval").on("REJECTED").to("rejected");

        return flow.build();
    }

    private WorkflowInput newInput(WorkflowDefinition def) throws Exception {
        return WorkflowInput.from(def, Map.of("employee", "alice", "days", 3))
                .withArchive(true);
    }

    private static ObjectNode decisionPayload(String decision) {
        return JsonNodeFactory.instance.objectNode().put("decision", decision);
    }

    private static BusinessActivityRegistry buildTestRegistry() {
        BusinessActivityRegistry registry = new BusinessActivityRegistry();
        registry.register(
                "createLeaveRequest",
                input -> {
                    Map<String, Object> out = new java.util.LinkedHashMap<>();
                    Object employeeId = input == null ? null : input.get("employeeId");
                    out.put("requestId", "REQ-" + employeeId);
                    out.put("employeeId", employeeId);
                    return out;
                });
        return registry;
    }
}
