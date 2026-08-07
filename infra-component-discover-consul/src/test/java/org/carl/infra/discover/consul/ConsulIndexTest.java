package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConsulIndexTest {

    @Test
    void protectsBlockingLoopFromZeroAndRollback() {
        assertEquals(1, ConsulIndex.next(0, 0));
        assertEquals(12, ConsulIndex.next(10, 12));
        assertEquals(12, ConsulIndex.next(12, 12));
        assertEquals(0, ConsulIndex.next(12, 8));
        assertEquals(8, ConsulIndex.next(0, 8));
    }
}
