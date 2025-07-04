package org.carl.client.dto.cmd;

import java.util.List;
import java.util.Map;

public class PointQueryCmd {
    String collectionName;
    private List<Float> vector;

    private int limit = 10;

    private Float scoreThreshold;

    private boolean withPayload = true;

    private boolean withVector = false;

    private Map<String, Object> filter;

    private Boolean useRerank;

    private String vectorName;

    public String getCollectionName() {
        return collectionName;
    }

    public PointQueryCmd setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }
}
