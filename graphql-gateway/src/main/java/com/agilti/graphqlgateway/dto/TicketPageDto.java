package com.agilti.graphqlgateway.dto;

import java.util.List;

/**
 * Espejo del PageResponse de ticket-service; se mapea 1 a 1 al tipo
 * GraphQL TicketPage.
 */
public record TicketPageDto(
        List<TicketDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean nextPage,
        boolean previousPage) {
}
