package org.carl.infrastructure.component.web.response;

public class SingleEntityResponse<T> extends EntityResponse {
    T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static SingleEntityResponse<?> buildSuccess() {
        SingleEntityResponse<?> response = new SingleEntityResponse<>();
        response.setSuccess(true);
        return response;
    }

    public static SingleEntityResponse<?> buildFailure(String errCode, String errMessage) {
        SingleEntityResponse<?> response = new SingleEntityResponse<>();
        response.setSuccess(false);
        response.setErrCode(errCode);
        response.setErrMessage(errMessage);
        return response;
    }

    public static <T> SingleEntityResponse<T> of(T data) {
        SingleEntityResponse<T> response = new SingleEntityResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
}
