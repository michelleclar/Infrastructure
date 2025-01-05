package org.carl.infrastructure.persistence.engine.core;

import java.util.Arrays;
import javax.sql.DataSource;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.TableImpl;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DSLContextX extends DefaultDSLContext {
    public DSLContextX(SQLDialect dialect) {
        super(dialect);
    }

    public DSLContextX(DataSource datasource, SQLDialect dialect) {
        super(datasource, dialect, null);
    }

    public DSLContextX(DefaultConfiguration configuration) {
        super(configuration);
    }

    public SelectSelectStep<Record> selectPage(SelectFieldOrAsterisk... fields) {
        Field<Integer> total = DSL.count().over().as("total");
        if (fields == null || fields.length == 0) { // FIXME: check design
            fields = new SelectFieldOrAsterisk[1];
        }
        if (fields.length == 1 && fields[0] instanceof TableImpl<?> ta) {
            Field<?>[] _f = ta.fields();
            fields = Arrays.copyOf((SelectFieldOrAsterisk[]) _f, _f.length + 1);
        }
        fields[fields.length - 1] = total;
        return super.select(fields);
    }
}
