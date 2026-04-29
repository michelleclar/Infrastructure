package org.carl.infrastructure.persistence.engine.runtime;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DslContextFactory {
    private static final ILogger logger = LoggerFactory.getLogger(DslContextFactory.class);

    static {
        System.setProperty("org.jooq.no-logo", String.valueOf(true)); // -Dorg.jooq.no-logo=true
    }

    public static DSLContext create(DataSource ds) {
        return create(dialect(ds), ds, new JooqCustomContext() {});
    }

    public static DSLContext create(
            SQLDialect dialect, DataSource ds, JooqCustomContext customContext) {
        DSLContext context = DSL.using(ds, dialect);
        customContext.apply(context.configuration());
        return context;
    }

    public static DSLContext create(
            String sqlDialect, DataSource ds, JooqCustomContext customContext) {
        DSLContext context;
        if ("PostgreSQL".equalsIgnoreCase(sqlDialect) || "Postgres".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.POSTGRES);
        } else if ("MySQL".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.MYSQL);
        } else if ("MARIADB".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.MARIADB);
        } else if ("Oracle".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.DEFAULT);
        } else if ("SQLServer".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.DEFAULT);
        } else if ("DB2".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.DEFAULT);
        } else if ("Derby".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.DERBY);
        } else if ("HSQLDB".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.HSQLDB);
        } else if ("H2".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.H2);
        } else if ("Firebird".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.FIREBIRD);
        } else if ("SQLite".equalsIgnoreCase(sqlDialect)) {
            context = DSL.using(ds, SQLDialect.SQLITE);
        } else {
            logger.warn("Undefined sqlDialect: {}", sqlDialect);
            context = DSL.using(ds, SQLDialect.DEFAULT);
        }
        customContext.apply(context.configuration());
        return context;
    }

    private static SQLDialect dialect(DataSource ds) {
        try (Connection connection = new ConnectionProvider(ds).get()) {
            return JDBCUtils.dialect(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
