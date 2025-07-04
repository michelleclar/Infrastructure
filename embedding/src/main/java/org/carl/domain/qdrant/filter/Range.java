package org.carl.domain.qdrant.filter;

public class Range {
    private Double gt; // 大于 (Greater Than)
    private Double gte; // 大于等于 (Greater Than or Equal to)
    private Double lt; // 小于 (Less Than)
    private Double lte; // 小于等于 (Less Than or Equal to)

    public Range() {}

    public Range(Double gt, Double gte, Double lt, Double lte) {
        this.gt = gt;
        this.gte = gte;
        this.lt = lt;
        this.lte = lte;
    }

    // Getters and Setters
    public Double getGt() {
        return gt;
    }

    public void setGt(Double gt) {
        this.gt = gt;
    }

    public Double getGte() {
        return gte;
    }

    public void setGte(Double gte) {
        this.gte = gte;
    }

    public Double getLt() {
        return lt;
    }

    public void setLt(Double lt) {
        this.lt = lt;
    }

    public Double getLte() {
        return lte;
    }

    public void setLte(Double lte) {
        this.lte = lte;
    }
}
