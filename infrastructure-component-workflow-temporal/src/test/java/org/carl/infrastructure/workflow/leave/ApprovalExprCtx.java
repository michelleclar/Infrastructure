package org.carl.infrastructure.workflow.leave;

/** Minimal context for the expression-based approval tests. */
public class ApprovalExprCtx {

    private String id;

    public ApprovalExprCtx() {}

    public ApprovalExprCtx(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
