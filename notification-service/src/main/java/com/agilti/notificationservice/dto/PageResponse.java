package com.agilti.notificationservice.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltorio de paginacion para GET /api/notificaciones. Mismo formato que
 * el PageResponse de ticket-service, para que el graphql-gateway (y el
 * frontend detras de el) los trate igual.
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean nextPage;
    private boolean previousPage;

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.content = page.getContent();
        response.page = page.getNumber();
        response.size = page.getSize();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.nextPage = !page.isLast();
        response.previousPage = !page.isFirst();
        return response;
    }

    public List<T> getContent() {
        return content;
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

    public boolean isNextPage() {
        return nextPage;
    }

    public boolean isPreviousPage() {
        return previousPage;
    }
}
