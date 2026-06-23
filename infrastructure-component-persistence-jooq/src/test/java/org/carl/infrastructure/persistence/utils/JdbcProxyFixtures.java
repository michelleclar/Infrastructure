package org.carl.infrastructure.persistence.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class JdbcProxyFixtures {
    private JdbcProxyFixtures() {}

    static TrackingConnection trackingConnection(String productName, String productVersion, String schema) {
        AtomicBoolean closed = new AtomicBoolean();
        Connection connection =
                proxy(
                        Connection.class,
                        (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "getMetaData" -> metadata(productName, productVersion);
                                case "getSchema" -> schema;
                                case "createStatement" -> statement();
                                case "close" -> {
                                    closed.set(true);
                                    yield null;
                                }
                                case "isClosed" -> closed.get();
                                case "toString" -> "TrackingConnection";
                                default -> defaultValue(method.getReturnType());
                            };
                        });
        return new TrackingConnection(connection, closed);
    }

    static TrackingConnection metadataConnection() {
        AtomicBoolean closed = new AtomicBoolean();
        DatabaseMetaData metaData =
                proxy(
                        DatabaseMetaData.class,
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if ("getDatabaseProductName".equals(name)) {
                                return "PostgreSQL";
                            }
                            if ("getDatabaseProductVersion".equals(name)) {
                                return "16.2";
                            }
                            if ("getSchemas".equals(name)) {
                                return resultSet(
                                        List.of(
                                                Map.of(
                                                        "TABLE_CATALOG",
                                                        "catalog_a",
                                                        "TABLE_SCHEM",
                                                        "CamelSchema")));
                            }
                            if ("getTables".equals(name)) {
                                String schema = (String) args[1];
                                String table = (String) args[2];
                                if (!"CamelSchema".equals(schema)) {
                                    return resultSet(List.of());
                                }
                                if (table == null) {
                                    return resultSet(
                                            List.of(
                                                    Map.of(
                                                            "TABLE_CAT",
                                                            "catalog_a",
                                                            "TABLE_SCHEM",
                                                            "CamelSchema",
                                                            "TABLE_NAME",
                                                            "OrderLine",
                                                            "TABLE_TYPE",
                                                            "TABLE",
                                                            "REMARKS",
                                                            "order line table")));
                                }
                                if ("OrderLine".equals(table)) {
                                    return resultSet(
                                            List.of(
                                                    Map.of(
                                                            "TABLE_CAT",
                                                            "catalog_a",
                                                            "TABLE_SCHEM",
                                                            "CamelSchema",
                                                            "TABLE_NAME",
                                                            "OrderLine",
                                                            "TABLE_TYPE",
                                                            "TABLE",
                                                            "REMARKS",
                                                            "order line table")));
                                }
                                return resultSet(List.of());
                            }
                            if ("getColumns".equals(name)) {
                                return resultSet(
                                        List.of(
                                                row(
                                                        "COLUMN_NAME",
                                                        "ID",
                                                        "REMARKS",
                                                        "primary id",
                                                        "TYPE_NAME",
                                                        "bigint",
                                                        "DATA_TYPE",
                                                        -5,
                                                        "COLUMN_SIZE",
                                                        19,
                                                        "DECIMAL_DIGITS",
                                                        0,
                                                        "NULLABLE",
                                                        DatabaseMetaData.columnNoNulls,
                                                        "IS_GENERATEDCOLUMN",
                                                        "NO",
                                                        "IS_AUTOINCREMENT",
                                                        "YES",
                                                        "COLUMN_DEF",
                                                        null,
                                                        "ORDINAL_POSITION",
                                                        1),
                                                row(
                                                        "COLUMN_NAME",
                                                        "UserID",
                                                        "REMARKS",
                                                        "user id",
                                                        "TYPE_NAME",
                                                        "int4",
                                                        "DATA_TYPE",
                                                        4,
                                                        "COLUMN_SIZE",
                                                        10,
                                                        "DECIMAL_DIGITS",
                                                        0,
                                                        "NULLABLE",
                                                        DatabaseMetaData.columnNullable,
                                                        "IS_GENERATEDCOLUMN",
                                                        "NO",
                                                        "IS_AUTOINCREMENT",
                                                        "NO",
                                                        "COLUMN_DEF",
                                                        "0",
                                                        "ORDINAL_POSITION",
                                                        2)));
                            }
                            if ("getPrimaryKeys".equals(name)) {
                                return resultSet(
                                        List.of(
                                                row(
                                                        "PK_NAME",
                                                        "PK_OrderLine",
                                                        "COLUMN_NAME",
                                                        "ID",
                                                        "KEY_SEQ",
                                                        1)));
                            }
                            if ("getImportedKeys".equals(name)) {
                                return resultSet(
                                        List.of(
                                                row(
                                                        "FK_NAME",
                                                        "FK_OrderLine_User",
                                                        "FKCOLUMN_NAME",
                                                        "UserID",
                                                        "PKTABLE_CAT",
                                                        "catalog_a",
                                                        "PKTABLE_SCHEM",
                                                        "CamelSchema",
                                                        "PKTABLE_NAME",
                                                        "User",
                                                        "PKCOLUMN_NAME",
                                                        "ID",
                                                        "KEY_SEQ",
                                                        1)));
                            }
                            if ("getIndexInfo".equals(name)) {
                                return resultSet(
                                        List.of(
                                                row(
                                                        "INDEX_NAME",
                                                        "IX_OrderLine_UserID",
                                                        "NON_UNIQUE",
                                                        true,
                                                        "COLUMN_NAME",
                                                        "UserID",
                                                        "ORDINAL_POSITION",
                                                        1,
                                                        "ASC_OR_DESC",
                                                        "A")));
                            }
                            return defaultValue(method.getReturnType());
                        });

        Connection connection =
                proxy(
                        Connection.class,
                        (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "getMetaData" -> metaData;
                                case "getSchema" -> "CamelSchema";
                                case "close" -> {
                                    closed.set(true);
                                    yield null;
                                }
                                case "isClosed" -> closed.get();
                                case "toString" -> "MetadataConnection";
                                default -> defaultValue(method.getReturnType());
                            };
                        });
        return new TrackingConnection(connection, closed);
    }

    static TrackingConnection mysqlMetadataConnection() {
        AtomicBoolean closed = new AtomicBoolean();
        DatabaseMetaData metaData =
                proxy(
                        DatabaseMetaData.class,
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if ("getDatabaseProductName".equals(name)) {
                                return "MySQL";
                            }
                            if ("getDatabaseProductVersion".equals(name)) {
                                return "8.0";
                            }
                            if ("getSchemas".equals(name)) {
                                return resultSet(List.of());
                            }
                            if ("getCatalogs".equals(name)) {
                                return resultSet(List.of(Map.of("TABLE_CAT", "AppCatalog")));
                            }
                            if ("getTables".equals(name)) {
                                String catalog = (String) args[0];
                                String schema = (String) args[1];
                                String table = (String) args[2];
                                if (!"AppCatalog".equals(catalog) || schema != null) {
                                    return resultSet(List.of());
                                }
                                if (table == null || "OrderLine".equals(table)) {
                                    return resultSet(
                                            List.of(
                                                    Map.of(
                                                            "TABLE_CAT",
                                                            "AppCatalog",
                                                            "TABLE_SCHEM",
                                                            "",
                                                            "TABLE_NAME",
                                                            "OrderLine",
                                                            "TABLE_TYPE",
                                                            "TABLE",
                                                            "REMARKS",
                                                            "mysql table")));
                                }
                                return resultSet(List.of());
                            }
                            if ("getColumns".equals(name)) {
                                String catalog = (String) args[0];
                                if (!"AppCatalog".equals(catalog)) {
                                    return resultSet(List.of());
                                }
                                return resultSet(
                                        List.of(
                                                row(
                                                        "COLUMN_NAME",
                                                        "ID",
                                                        "REMARKS",
                                                        "id",
                                                        "TYPE_NAME",
                                                        "bigint",
                                                        "DATA_TYPE",
                                                        -5,
                                                        "COLUMN_SIZE",
                                                        20,
                                                        "DECIMAL_DIGITS",
                                                        0,
                                                        "NULLABLE",
                                                        DatabaseMetaData.columnNoNulls,
                                                        "IS_GENERATEDCOLUMN",
                                                        "NO",
                                                        "IS_AUTOINCREMENT",
                                                        "YES",
                                                        "COLUMN_DEF",
                                                        null,
                                                        "ORDINAL_POSITION",
                                                        1)));
                            }
                            if ("getPrimaryKeys".equals(name)) {
                                String catalog = (String) args[0];
                                if (!"AppCatalog".equals(catalog)) {
                                    return resultSet(List.of());
                                }
                                return resultSet(
                                        List.of(row("PK_NAME", "PRIMARY", "COLUMN_NAME", "ID", "KEY_SEQ", 1)));
                            }
                            if ("getImportedKeys".equals(name) || "getIndexInfo".equals(name)) {
                                return resultSet(List.of());
                            }
                            return defaultValue(method.getReturnType());
                        });

        Connection connection =
                proxy(
                        Connection.class,
                        (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "getMetaData" -> metaData;
                                case "getSchema" -> null;
                                case "close" -> {
                                    closed.set(true);
                                    yield null;
                                }
                                case "isClosed" -> closed.get();
                                case "toString" -> "MysqlMetadataConnection";
                                default -> defaultValue(method.getReturnType());
                            };
                        });
        return new TrackingConnection(connection, closed);
    }

    static DatabaseMetaData metadata(String productName, String productVersion) {
        return proxy(
                DatabaseMetaData.class,
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getDatabaseProductName" -> productName;
                        case "getDatabaseProductVersion" -> productVersion;
                        default -> defaultValue(method.getReturnType());
                    };
                });
    }

    static ResultSet resultSet(List<Map<String, Object>> rows) {
        List<Map<String, Object>> copy = new ArrayList<>(rows);
        class State {
            int index = -1;
            boolean lastWasNull;
        }
        State state = new State();
        return proxy(
                ResultSet.class,
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        state.index++;
                        return state.index < copy.size();
                    }
                    if ("getString".equals(name)) {
                        Object value = copy.get(state.index).get((String) args[0]);
                        state.lastWasNull = value == null;
                        return value == null ? null : value.toString();
                    }
                    if ("getInt".equals(name)) {
                        Object value = copy.get(state.index).get((String) args[0]);
                        state.lastWasNull = value == null;
                        if (value instanceof Number number) {
                            return number.intValue();
                        }
                        return value == null ? 0 : Integer.parseInt(value.toString());
                    }
                    if ("getBoolean".equals(name)) {
                        Object value = copy.get(state.index).get((String) args[0]);
                        state.lastWasNull = value == null;
                        if (value instanceof Boolean bool) {
                            return bool;
                        }
                        return value != null && Boolean.parseBoolean(value.toString());
                    }
                    if ("wasNull".equals(name)) {
                        return state.lastWasNull;
                    }
                    if ("close".equals(name)) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    static Statement statement() {
        return proxy(
                Statement.class,
                (proxy, method, args) -> switch (method.getName()) {
                    case "execute" -> true;
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    static Map<String, Object> row(Object... keyValues) {
        java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T)
                Proxy.newProxyInstance(
                        type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    static Object defaultValue(Class<?> type) throws SQLException {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == void.class) {
            return null;
        }
        if (type == String.class) {
            return null;
        }
        if (type == ResultSet.class) {
            return resultSet(List.of());
        }
        return null;
    }

    record TrackingConnection(Connection connection, AtomicBoolean closed) {}
}
