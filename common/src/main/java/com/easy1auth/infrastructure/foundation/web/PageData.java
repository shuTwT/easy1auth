package com.easy1auth.infrastructure.foundation.web;

import java.util.List;

/**
 * 分页数据包装（面向接口层的只读 DTO）。
 *
 * @param <T>      列表元素类型
 * @param items    当前页的数据列表（自动做不可变拷贝，避免外部篡改）
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 * @param total    符合条件的总条数
 */
public record PageData<T>(List<T> items, int page, int pageSize, long total) {
    /** 紧凑构造器：对 items 做空值与不可变保护 */
    public PageData {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** 便捷构造：按参数创建分页数据。 */
    public static <T> PageData<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageData<>(items, page, pageSize, total);
    }
}
