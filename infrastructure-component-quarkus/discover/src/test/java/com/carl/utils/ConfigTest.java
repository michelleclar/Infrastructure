package com.carl.utils;

import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ConfigTest {
    @ConfigProperty(name = "greeting.message", defaultValue = "Hello from default")
    String message;

    @Test
    public void test() {
        System.out.println(message);
    }
}
