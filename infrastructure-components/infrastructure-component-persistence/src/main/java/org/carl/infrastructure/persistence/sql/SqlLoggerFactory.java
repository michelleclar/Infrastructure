package org.carl.infrastructure.persistence.sql;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import org.jdbi.v3.core.statement.ParsedSql;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlLoggerFactory {
    public static SqlLogger DEFAULT =
            new SqlLogger() {
                private final Logger log = LoggerFactory.getLogger(SqlLoggerFactory.class);

                @Override
                public void logAfterExecution(StatementContext context) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug(
                                "Executed in {} '{}' with parameters '{}'",
                                format(
                                        Duration.between(
                                                Objects.requireNonNull(
                                                        context.getExecutionMoment()),
                                                context.getCompletionMoment())),
                                getSql(context),
                                context.getBinding());
                    }
                }

                @Override
                public void logException(StatementContext context, SQLException ex) {
                    if (this.log.isErrorEnabled()) {
                        this.log.error(
                                "Exception while executing '{}' with parameters '{}'",
                                getSql(context),
                                context.getBinding(),
                                ex);
                    }
                }

                private static String getSql(StatementContext context) {
                    ParsedSql parsedSql = context.getParsedSql();
                    return parsedSql != null ? parsedSql.getSql() : "<not available>";
                }

                private static String format(Duration duration) {
                    long totalSeconds = duration.getSeconds();
                    long h = totalSeconds / 3600L;
                    long m = totalSeconds % 3600L / 60L;
                    long s = totalSeconds % 60L;
                    long ms = duration.toMillis() % 1000L;
                    return String.format("%d:%02d:%02d.%03d", h, m, s, ms);
                }
            };
}
