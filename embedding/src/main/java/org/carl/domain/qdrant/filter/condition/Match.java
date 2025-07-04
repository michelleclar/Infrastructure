package org.carl.domain.qdrant.filter.condition;

import org.carl.utils.Assert;

import java.util.List;

public class Match {
    private String field;
    // String, Long, Boolean
    private Object value;
    // string long
    private List<Object> any;
    private String text;
    // string long
    private List<Object> except;

    public List<Object> getExcept() {
        return except;
    }

    public Match setExcept(List<Object> except) {
        Assert.assertAllStringOrLong(except, "except");
        this.except = except;
        return this;
    }

    public String getText() {
        return text;
    }

    public Match setText(String text) {
        this.text = text;
        return this;
    }

    public List<Object> getAny() {
        return any;
    }

    public Match setAny(List<Object> any) {
        Assert.assertAllStringOrLong(any, "any");
        this.any = any;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public Match setValue(Object value) {
        Assert.assertStringOrLongOrBoolean(value, "value");
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "{"
                + "        \"value\":"
                + value
                + ",         \"any\":"
                + any
                + ",         \"text\":\""
                + text
                + "\""
                + ",         \"except\":"
                + except
                + "}";
    }
}
