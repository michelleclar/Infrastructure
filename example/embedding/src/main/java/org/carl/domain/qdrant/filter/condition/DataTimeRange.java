package org.carl.domain.qdrant.filter.condition;

import java.sql.Timestamp;

// TODO: need change filed
public class DataTimeRange {

    private String field;
    private Timestamp gt;
    private Timestamp gte;
    private Timestamp lt;
    private Timestamp lte;

    public Timestamp getGt() {
        return gt;
    }

    public DataTimeRange setGt(Timestamp gt) {
        this.gt = gt;
        return this;
    }

    public Timestamp getGte() {
        return gte;
    }

    public DataTimeRange setGte(Timestamp gte) {
        this.gte = gte;
        return this;
    }

    public Timestamp getLt() {
        return lt;
    }

    public DataTimeRange setLt(Timestamp lt) {
        this.lt = lt;
        return this;
    }

    public Timestamp getLte() {
        return lte;
    }

    public DataTimeRange setLte(Timestamp lte) {
        this.lte = lte;
        return this;
    }

    public String getField() {
        return field;
    }

    public DataTimeRange setField(String field) {
        this.field = field;
        return this;
    }
}
