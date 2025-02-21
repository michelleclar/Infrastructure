package org.carl.infrastructure.testcontainers;

import org.junit.jupiter.api.Test;

public class ContainerTest implements AllContainerTest {
    @Test
    public void testContainer() {
        compose.start();
    }
}
