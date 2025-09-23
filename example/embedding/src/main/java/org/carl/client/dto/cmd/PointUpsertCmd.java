package org.carl.client.dto.cmd;

import java.util.List;
import java.util.Map;

public class PointUpsertCmd {
    String collectionName;
    Map<String, Map<String, Object>> data;
    List<String> includeVectorFields;

    public List<String> getIncludeVectorFields() {
        return includeVectorFields;
    }

    public PointUpsertCmd setIncludeVectorFields(List<String> includeVectorFields) {
        this.includeVectorFields = includeVectorFields;
        return this;
    }

    public Map<String, Map<String, Object>> getData() {
        return data;
    }

    public PointUpsertCmd setData(Map<String, Map<String, Object>> data) {
        this.data = data;
        return this;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public PointUpsertCmd setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }
}
