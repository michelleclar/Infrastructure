package org.carl.infrastructure.pulsar.test;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PulsarConfigTest {
    @Inject MsgArgsConfig msgArgsConfig;

    @Test
    public void testPulsarConfig() {
        System.out.println(msgArgsConfig);
    }
}
