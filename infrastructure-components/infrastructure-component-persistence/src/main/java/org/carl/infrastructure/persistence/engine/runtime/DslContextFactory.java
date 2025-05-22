package org.carl.infrastructure.persistence.engine.runtime;

import io.agroal.api.AgroalDataSource;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.JDBCUtils;

public class DslContextFactory {
    private static final Logger log = Logger.getLogger(DslContextFactory.class);

    static {
        System.setProperty("org.jooq.no-logo", String.valueOf(true)); // -Dorg.jooq.no-logo=true
    }

    public static DSLContext create(AgroalDataSource ds) {
        return create(
                JDBCUtils.dialect(new ConnectionProvider(ds).get()),
                ds,
                new JooqCustomContext() {});
    }

    public static DSLContext create(
            SQLDialect dialect, AgroalDataSource ds, JooqCustomContext customContext) {
        DSLContext context = DSL.using(ds, dialect);
        customContext.apply(context.configuration());
        return context;
    }

    public static DSLContext create(
            String sqlDialect, AgroalDataSource ds, JooqCustomContext customContext) {
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
            log.warnf("Undefined sqlDialect: {}", sqlDialect);
            context = DSL.using(ds, SQLDialect.DEFAULT);
        }
        customContext.apply(context.configuration());
        return context;
    }
}
