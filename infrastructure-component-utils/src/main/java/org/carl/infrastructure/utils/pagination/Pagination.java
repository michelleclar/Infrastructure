package org.carl.infrastructure.utils.pagination;

public final class Pagination {

    private Pagination() {
    }

    public static PageWindow normalize(int pageIndex, int pageSize) {
        return normalize(pageIndex, pageSize, Integer.MAX_VALUE);
    }

    public static PageWindow normalize(int pageIndex, int pageSize, int maxPageSize) {
        int normalizedMax = Math.max(1, maxPageSize);
        int normalizedIndex = Math.max(1, pageIndex);
        int normalizedSize = Math.min(Math.max(1, pageSize), normalizedMax);
        return new PageWindow(normalizedIndex, normalizedSize, 0);
    }
}
