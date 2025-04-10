package org.carl.infrastructure.component.web.response;

import jakarta.ws.rs.core.Response;

public abstract class SingleResponse<T> extends Response {
    private T data;

    public SingleResponse<T> setData(T data) {
        this.data = data;
        return this;
    }

    public T getData() {
        return data;
    }

    public Response build() {
        return Response.ok(getData()).build();
    }
}
