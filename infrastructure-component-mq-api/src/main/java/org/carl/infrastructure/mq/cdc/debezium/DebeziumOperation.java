package org.carl.infrastructure.mq.cdc.debezium;

public enum DebeziumOperation {
    CREATE("c"),
    UPDATE("u"),
    DELETE("d"),
    READ("r"),
    TRUNCATE("t"),
    MESSAGE("m");

    private final String code;

    DebeziumOperation(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DebeziumOperation fromCode(String code) {
        return switch (code) {
            case "c" -> CREATE;
            case "u" -> UPDATE;
            case "d" -> DELETE;
            case "r" -> READ;
            case "t" -> TRUNCATE;
            case "m" -> MESSAGE;
            default -> throw new IllegalArgumentException("Unsupported Debezium op: " + code);
        };
    }
}
