package org.carl.client.dto.clientobject;

import java.util.Map;

public class ScoredPointCO extends AbstractMetricCO{

    public Long id;
    public String uuid;
    public float score;
    public Map<String, Object> payload;

    public Map<String, Object> getPayload() {
        return payload;
    }

    public ScoredPointCO setPayload(Map<String, Object> payload) {
        this.payload = payload;
        return this;
    }

    public float getScore() {
        return score;
    }

    public ScoredPointCO setScore(float score) {
        this.score = score;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public ScoredPointCO setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public Long getId() {
        return id;
    }

    public ScoredPointCO setId(Long id) {
        this.id = id;
        return this;
    }
}
