package org.carl.infrastructure.workflow.leave;

import java.util.ArrayList;
import java.util.List;

/** Context for the team-leave (multi-approver) process. */
public class TeamLeaveCtx {

    private String id;
    private List<String> approvers = new ArrayList<>();
    /** Selects the gather policy at registration time via TeamLeaveProcess. */
    private String policyName = "allApprove"; // or "kOfN:2", "anyApprove", "majority"

    public TeamLeaveCtx() {}

    public TeamLeaveCtx(String id, List<String> approvers) {
        this.id = id;
        this.approvers = approvers;
    }

    public TeamLeaveCtx(String id, List<String> approvers, String policyName) {
        this.id = id;
        this.approvers = approvers;
        this.policyName = policyName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getApprovers() {
        return approvers;
    }

    public void setApprovers(List<String> approvers) {
        this.approvers = approvers;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
}
