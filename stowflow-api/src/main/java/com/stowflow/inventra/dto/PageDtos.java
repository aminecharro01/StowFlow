package com.stowflow.inventra.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public final class PageDtos {

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {
        public static <T> PageResponse<T> from(Page<T> springPage) {
            return new PageResponse<>(
                    springPage.getContent(),
                    springPage.getTotalElements(),
                    springPage.getTotalPages(),
                    springPage.getNumber(),
                    springPage.getSize());
        }

        public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
            int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
            return new PageResponse<>(content, totalElements, totalPages, page, size);
        }
    }

    private PageDtos() {}
}
