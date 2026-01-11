package org.carl.infrastructure.approval.api;

import org.carl.infrastructure.approval.model.ApprovalNode;

import java.util.List;

public class ApprovalStartRequest {
    private String bizKey;
    private List<ApprovalNode> nodes;

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public List<ApprovalNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ApprovalNode> nodes) {
        this.nodes = nodes;
    }
}
