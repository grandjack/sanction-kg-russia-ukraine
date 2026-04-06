package com.sanction.kg.util;

/**
 * Utility for pagination parameter normalization.
 */
public final class PageUtil {

    private PageUtil() {
        // utility class
    }

    /**
     * Normalize page number (1-based input, ensure >= 1).
     */
    public static int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    /**
     * Normalize page size (ensure between 1 and 100).
     */
    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        if (size > 100) {
            return 100;
        }
        return size;
    }

    /**
     * Convert 1-based page number to 0-based offset for Spring Data.
     */
    public static int toZeroBasedPage(int page) {
        return page - 1;
    }

    /**
     * Calculate total pages.
     */
    public static int totalPages(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }
}
