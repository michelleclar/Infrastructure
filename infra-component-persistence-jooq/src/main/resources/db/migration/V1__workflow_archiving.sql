-- Workflow instance archival tables for Temporal workflow engine
-- Supports: workflow_instance + execution_record

-- Main workflow instance table
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
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_workflow_instance_definition ON workflow_instance(definition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_business_key ON workflow_instance(business_key);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_status ON workflow_instance(status);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_started_at ON workflow_instance(started_at);

-- Detailed execution record table (optional, for audit)
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
);

-- Indexes for execution record queries
CREATE INDEX IF NOT EXISTS idx_execution_record_workflow ON execution_record(workflow_id);
CREATE INDEX IF NOT EXISTS idx_execution_record_node ON execution_record(node_id);

-- Comment documentation
COMMENT ON TABLE workflow_instance IS 'Archived workflow instances from Temporal';
COMMENT ON TABLE execution_record IS 'Detailed node execution history for audit';
