package org.carl.infrastructure.persistence.core;

import org.junit.jupiter.api.Test;

public class TestUtils {
    long BYTE_TO_MB = 1024 * 1024;

    @Test
    public void test() {
        Runtime runtime = Runtime.getRuntime();
        long vmFree = runtime.freeMemory();
        System.out.println(vmFree / BYTE_TO_MB);
    }
}
