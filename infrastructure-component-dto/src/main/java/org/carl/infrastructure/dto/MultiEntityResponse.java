package org.carl.infrastructure.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MultiEntityResponse<T> extends EntityResponse {

    private Collection<T> data;

    public List<T> getData() {
        if (null == data) {
            return Collections.emptyList();
        }
        if (data instanceof List<T> list) {
            return list;
        }
        return new ArrayList<>(data);
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public static MultiEntityResponse<?> buildSuccess() {
        MultiEntityResponse<?> response = new MultiEntityResponse<>();
        response.setSuccess(true);
        return response;
    }

    public static MultiEntityResponse<?> buildFailure(String errCode, String errMessage) {
        MultiEntityResponse<?> response = new MultiEntityResponse<>();
        response.setSuccess(false);
        response.setErrCode(errCode);
        response.setErrMessage(errMessage);
        return response;
    }

    public static <T> MultiEntityResponse<T> of(Collection<T> data) {
        MultiEntityResponse<T> response = new MultiEntityResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
}
