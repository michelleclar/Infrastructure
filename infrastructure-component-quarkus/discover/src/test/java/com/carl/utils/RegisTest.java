package com.carl.utils;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
@Disabled("requires live Consul")
public class RegisTest {
    @Test
    public void test() {}
}
