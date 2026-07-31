package com.easy1auth.foundation.web;

import java.util.List;

public record PageData<T>(List<T> items, int page, int pageSize, long total) {
    public PageData {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> PageData<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageData<>(items, page, pageSize, total);
    }
}
