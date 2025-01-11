package org.carl.infrastructure.persistence.engine.runtime;

import io.agroal.api.AgroalDataSource;
import java.sql.SQLException;
import org.carl.infrastructure.persistence.engine.core.DSLContextX;
import org.jboss.logging.Logger;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;

/**
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public class DslContextFactory {
    private static final Logger log = Logger.getLogger(DslContextFactory.class);

    static {
        System.setProperty("org.jooq.no-logo", String.valueOf(true)); // -Dorg.jooq.no-logo=true
    }

    public static DSLContextX create(AgroalDataSource ds) throws SQLException {
        DSLContextX context;
        SQLDialect dialect = JDBCUtils.dialect(ds.getConnection());
        context = DSLContextX.create(ds, dialect);
        return context;
    }

    public static DSLContextX create(
            String sqlDialect, AgroalDataSource ds, JooqCustomContext customContext) {
        DSLContextX context;
        if ("PostgreSQL".equalsIgnoreCase(sqlDialect) || "Postgres".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.POSTGRES);
        } else if ("MySQL".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.MYSQL);
        } else if ("MARIADB".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.MARIADB);
        } else if ("Oracle".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.DEFAULT);
        } else if ("SQLServer".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.DEFAULT);
        } else if ("DB2".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.DEFAULT);
        } else if ("Derby".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.DERBY);
        } else if ("HSQLDB".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.HSQLDB);
        } else if ("H2".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.H2);
        } else if ("Firebird".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.FIREBIRD);
        } else if ("SQLite".equalsIgnoreCase(sqlDialect)) {
            context = DSLContextX.create(ds, SQLDialect.SQLITE);
        } else {
            log.warnv("Undefined sqlDialect: {0}", sqlDialect);
            context = DSLContextX.create(ds, SQLDialect.DEFAULT);
        }
        customContext.apply(context.configuration());
        return context;
    }
}
