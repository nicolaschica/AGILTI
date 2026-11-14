package com.agilti.graphqlgateway.dto;

import java.util.List;

/**
 * Espejo del PageResponse de notification-service; se mapea 1 a 1 al tipo
 * GraphQL NotificacionPage.
 */
public record NotificacionPageDto(
        List<NotificacionDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean nextPage,
        boolean previousPage) {
}
