package org.carl.infrastructure.workflow.leave;

/** Process context; must be serializable by the Temporal DataConverter (Jackson). */
public class LeaveCtx {

    private String id;
    private String approver;

    public LeaveCtx() {}

    public LeaveCtx(String id, String approver) {
        this.id = id;
        this.approver = approver;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }
}
