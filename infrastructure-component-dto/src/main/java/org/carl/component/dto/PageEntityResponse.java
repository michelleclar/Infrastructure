package org.carl.component.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PageEntityResponse<T> extends EntityResponse {

    private int totalCount = 0;

    private int pageSize = 1;

    private int pageIndex = 1;

    private Collection<T> data;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageSize() {
        return Math.max(pageSize, 1);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
    }

    public int getPageIndex() {
        return Math.max(pageIndex, 1);
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = Math.max(pageIndex, 1);
    }

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

    public int getTotalPages() {
        return this.totalCount % this.pageSize == 0
                ? this.totalCount / this.pageSize
                : (this.totalCount / this.pageSize) + 1;
    }

    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public static PageEntityResponse<?> buildSuccess() {
        PageEntityResponse<?> response = new PageEntityResponse<>();
        response.setSuccess(true);
        return response;
    }

    public static PageEntityResponse<?> buildFailure(String errCode, String errMessage) {
        PageEntityResponse<?> response = new PageEntityResponse<>();
        response.setSuccess(false);
        response.setErrCode(errCode);
        response.setErrMessage(errMessage);
        return response;
    }

    public static <T> PageEntityResponse<T> of(int pageSize, int pageIndex) {
        PageEntityResponse<T> response = new PageEntityResponse<>();
        response.setSuccess(true);
        response.setData(Collections.emptyList());
        response.setTotalCount(0);
        response.setPageSize(pageSize);
        response.setPageIndex(pageIndex);
        return response;
    }

    public static <T> PageEntityResponse<T> of(
            Collection<T> data, int totalCount, int pageSize, int pageIndex) {
        PageEntityResponse<T> response = new PageEntityResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTotalCount(totalCount);
        response.setPageSize(pageSize);
        response.setPageIndex(pageIndex);
        return response;
    }
}
