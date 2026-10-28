package com.andregattermeyer.api_frota.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationSupport {

    private PaginationSupport() {
    }

    public static boolean requested(Integer page, Integer size) {
        return page != null || size != null;
    }

    public static Pageable pageable(Integer page, Integer size) {
        return PageRequest.of(
                page == null ? 0 : page,
                size == null ? 5 : size,
                Sort.by(Sort.Direction.ASC, "id"));
    }
}
