package org.carl.infrastructure.resumeScript;

import io.quarkus.test.junit.QuarkusTest;
import org.carl.infrastructure.persistence.PersistenceStd;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class syncInterview extends PersistenceStd {
    @Test
    public void test() {
        String sql =
                """
                                                SELECT c.ACC200,c2.ACC676,c2.AAE052 FROM JY_HRM.CC20 c INNER JOIN JY_HRM.CC20_SEND cs ON c.ACC200 = cs.ACC200
                                                INNER JOIN JY_HRM.CC31 c31 ON c31.ACC200 = cs.ACC200S
                                                where c.ACC200 in (%s)
""";
    }
}
