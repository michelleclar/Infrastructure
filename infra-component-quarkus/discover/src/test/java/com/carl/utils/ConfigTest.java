package com.carl.utils;

import io.quarkus.test.junit.QuarkusTest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Disabled("requires live Consul")
public class ConfigTest {
    @ConfigProperty(name = "greeting.message", defaultValue = "Hello from default")
    String message;

    @Test
    public void test() {
        System.out.println(message);
    }
}
