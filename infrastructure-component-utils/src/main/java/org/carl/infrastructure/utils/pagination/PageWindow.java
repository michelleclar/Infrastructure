package org.carl.infrastructure.utils.pagination;

public record PageWindow(int pageIndex, int pageSize, long offset) {

    public PageWindow {
        if (pageIndex < 1) {
            throw new IllegalArgumentException("pageIndex must be at least 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1");
        }
        offset = (long) (pageIndex - 1) * pageSize;
    }
}
