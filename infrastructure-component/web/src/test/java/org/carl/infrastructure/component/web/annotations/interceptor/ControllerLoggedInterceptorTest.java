package org.carl.infrastructure.component.web.annotations.interceptor;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;

import model.BaseArgs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ControllerLoggedInterceptorTest {
    @Test
    @DisplayName("验证基础参数日志打印")
    void testBaseArgsLogged() {
        BaseArgs defualt = BaseArgs.DEFUALT;
        given().contentType("application/json")
                .body(defualt)
                .when()
                .post("/test/logger.base/args")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }
}
