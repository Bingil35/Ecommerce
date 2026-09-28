package com.example.backend.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PageResponse() {
    }

    public static <E, T> PageResponse<T> from(
            Page<E> page,
            Function<E, T> mapper
    ) {
        PageResponse<T> response = new PageResponse<>();

        response.items = page.getContent()
                .stream()
                .map(mapper)
                .toList();
        response.page = page.getNumber();
        response.size = page.getSize();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.last = page.isLast();

        return response;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}