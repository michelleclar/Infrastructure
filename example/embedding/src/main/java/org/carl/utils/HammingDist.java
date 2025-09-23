package org.carl.utils;

import org.jooq.Field;
import org.jooq.impl.DSL;

public class HammingDist {

    public static Field<Integer> searchBySimHash(Field<Long> sourceSimHash, long targetSimHash) {
        return DSL.field(
                "hamming_distance_sql({0}, {1})", Integer.class, sourceSimHash, DSL.val(targetSimHash));
    }
}
