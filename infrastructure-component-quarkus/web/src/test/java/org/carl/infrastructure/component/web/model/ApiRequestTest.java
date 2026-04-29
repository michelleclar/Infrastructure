package org.carl.infrastructure.component.web.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApiRequestTest {
    @Test
    void testParse() {
        String url = "/api/v1/system.config/refresh";
        ApiRequest apiRequest = new ApiRequest(url);
        System.out.println(apiRequest);
        assertNotNull(apiRequest);
    }
}
