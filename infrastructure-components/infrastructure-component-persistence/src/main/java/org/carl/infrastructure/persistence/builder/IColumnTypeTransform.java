package org.carl.infrastructure.persistence.builder;

@Deprecated
public interface IColumnTypeTransform {

    IColumnTypeTransform POSTGRES =
            type ->
                    switch (type) {
                        case VARCHAR_ARRAY -> "varchar[]";
                        case INT_ARRAY -> "int[]";
                        case JSON -> "jsonb";
                        default -> type.name();
                    };

    String transform(ColumnType type);
}
