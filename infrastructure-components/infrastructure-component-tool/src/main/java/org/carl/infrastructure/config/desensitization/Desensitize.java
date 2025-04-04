package org.carl.infrastructure.config.desensitization;

import org.jooq.Field;

import java.util.HashMap;
import java.util.Map;

/**
 * Use database column names to desensitize
 * If not registered, the original data will be returned
 */
public class Desensitize {
    private final Map<String, IDesensitizationAlgorithm> columnAlgorithmMap = new HashMap<>();

    public Desensitize registerAlgorithm(String columnName, IDesensitizationAlgorithm algorithm) {
        columnAlgorithmMap.put(columnName, algorithm);
        return this;
    }

    public Desensitize registerAlgorithm(Field<String> field, IDesensitizationAlgorithm algorithm) {
        columnAlgorithmMap.put(field.getName(), algorithm);
        return this;
    }

    public IDesensitizationAlgorithm getAlgorithm(String columnName) {
        return columnAlgorithmMap.get(columnName);
    }

    public IDesensitizationAlgorithm getAlgorithm(Field<String> field) {
        return columnAlgorithmMap.get(field.getName());
    }

    public String desensitize(String columnName, String source) {
        IDesensitizationAlgorithm algorithm = getAlgorithm(columnName);
        if (algorithm != null) {
            return algorithm.desensitize(source);
        }
        return source;
    }

    public String desensitize(Field<String> field, String source) {
        IDesensitizationAlgorithm algorithm = getAlgorithm(field.getName());
        if (algorithm != null) {
            return algorithm.desensitize(source);
        }
        return source; // 没有注册脱敏算法则返回原始数据
    }
}
