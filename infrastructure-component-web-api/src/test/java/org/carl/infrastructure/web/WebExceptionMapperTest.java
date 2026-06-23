package org.carl.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

class WebExceptionMapperTest {

    private final WebExceptionMapper<ResponseEnvelope<?>> mapper =
            new WebExceptionMapper<>(new DefaultResponseEnvelopeAdapter());

    @Test
    void mapsBusinessError() {
        MappedWebResponse<ResponseEnvelope<?>> response =
                mapper.map(
                        new BusinessWebException(409, "ORDER_CONFLICT", "Order already changed"),
                        WebRequestContext.of("req-1"));

        assertEquals(409, response.status());
        assertFalse(response.body().success());
        assertEquals(409, response.body().code());
        assertEquals("Order already changed", response.body().message());
        assertEquals("req-1", response.body().requestId());
    }

    @Test
    void mapsValidationError() {
        MappedWebResponse<ResponseEnvelope<?>> response =
                mapper.map(
                        new ValidationWebException(List.of(ValidationError.of("name", "must not be blank"))),
                        WebRequestContext.of("req-2"));

        assertEquals(400, response.status());
        assertEquals("Validation failed", response.body().message());
        assertEquals(1, response.body().errors().size());
        assertEquals("name", response.body().errors().getFirst().field());
    }

    @Test
    void mapsNotFound() {
        MappedWebResponse<ResponseEnvelope<?>> response =
                mapper.map(new NotFoundWebException("Missing order"), WebRequestContext.of("req-3"));

        assertEquals(404, response.status());
        assertEquals("Missing order", response.body().message());
    }

    @Test
    void mapsFallbackError() {
        MappedWebResponse<ResponseEnvelope<?>> response =
                mapper.map(new IllegalStateException("broken"), WebRequestContext.of("req-4"));

        assertEquals(500, response.status());
        assertEquals("broken", response.body().message());
    }

    @Test
    void supportsApplicationSpecificEnvelopeAdapter() {
        record ExistingApiResponse(String traceId, int status, String message) {}

        WebExceptionMapper<ExistingApiResponse> customMapper =
                new WebExceptionMapper<>(
                        new ResponseEnvelopeAdapter<>() {
                            @Override
                            public ExistingApiResponse success(Object data, WebRequestContext context) {
                                return new ExistingApiResponse(context.requestId(), 0, "ok");
                            }

                            @Override
                            public ExistingApiResponse failure(WebError error, WebRequestContext context) {
                                return new ExistingApiResponse(
                                        context.requestId(), error.status(), error.message());
                            }
                        });

        MappedWebResponse<ExistingApiResponse> response =
                customMapper.map(new BusinessWebException("denied"), WebRequestContext.of("req-5"));

        assertEquals("req-5", response.body().traceId());
        assertEquals(400, response.body().status());
        assertEquals("denied", response.body().message());
    }
}
