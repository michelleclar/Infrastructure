package org.carl.infrastructure.component.web.runtime;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IRuntimeProviderTest {
    /** {@link org.carl.infrastructure.component.web.filter.RootFilter} */
    @Test
    @DisplayName("验证自定义过滤器")
    void test() {
        given().contentType("application/json")
                .when()
                .headers("Authorization", "adsdddddddaadads")
                .get("/test")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }
}
