package org.carl.infrastructure.component.web.annotations.interceptor;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;
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
                .post("/test/logger/base")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }
}

@ControllerLogged
@Path("/test/logger")
class TestLoggedController {
    @Path("/base")
    @POST
    public Response base(BaseArgs b) {
        return Response.ok(b).build();
    }
}

class BaseArgs implements Serializable {
    Integer i = 1;
    String s = "a";
    Short aShort = 1;
    Byte aByte = Byte.valueOf("1");
    Double aDouble = 1.2;
    public static BaseArgs DEFUALT = new BaseArgs();

    public Double getaDouble() {
        return aDouble;
    }

    public BaseArgs setaDouble(Double aDouble) {
        this.aDouble = aDouble;
        return this;
    }

    public Byte getaByte() {
        return aByte;
    }

    public BaseArgs setaByte(Byte aByte) {
        this.aByte = aByte;
        return this;
    }

    public Short getaShort() {
        return aShort;
    }

    public BaseArgs setaShort(Short aShort) {
        this.aShort = aShort;
        return this;
    }

    public String getS() {
        return s;
    }

    public BaseArgs setS(String s) {
        this.s = s;
        return this;
    }

    public Integer getI() {
        return i;
    }

    public BaseArgs setI(Integer i) {
        this.i = i;
        return this;
    }
}
