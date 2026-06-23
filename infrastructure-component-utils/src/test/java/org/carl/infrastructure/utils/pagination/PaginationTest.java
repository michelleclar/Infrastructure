package org.carl.infrastructure.utils.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaginationTest {

    @Test
    void normalizesPageInputs() {
        PageWindow window = Pagination.normalize(0, 500, 100);

        assertEquals(1, window.pageIndex());
        assertEquals(100, window.pageSize());
        assertEquals(0, window.offset());
    }

    @Test
    void computesOffset() {
        PageWindow window = Pagination.normalize(3, 20, 100);

        assertEquals(40, window.offset());
    }
}
