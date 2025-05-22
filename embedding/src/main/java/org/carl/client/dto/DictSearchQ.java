package org.carl.client.dto;

import org.carl.component.dto.Query;

public class DictSearchQ extends Query {
    String text;
    Integer limit;
    Integer level;
    String dictType;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
