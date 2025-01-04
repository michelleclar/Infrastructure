package org.carl.persistence.engine.runtime;

import java.sql.Types;
import java.util.Date;
import org.jooq.DataType;
import org.jooq.impl.DefaultDataType;

/**
 * org.jooq.impl.SQLDataType.TIMESTAMP(XXX) to io.quarkiverse.jooq.UTILDATE(XXX)
 *
 * @see org.jooq.impl.SQLDataType
 * @see Types
 */
public final class SQLDataTypeExt {
    /**
     * The {@link Types#TIMESTAMP} type.
     */
    public static final DataType<Date> UTILDATE =
            new DefaultDataType<Date>(null, Date.class, "timestamp");

    /**
     * The {@link Types#TIMESTAMP_WITH_TIMEZONE} type.
     */
    public static final DataType<Date> UTILDATEWITHTIMEZONE =
            new DefaultDataType<Date>(null, Date.class, "timestamptz");
}
