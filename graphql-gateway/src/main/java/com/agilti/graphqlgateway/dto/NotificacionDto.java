package com.agilti.graphqlgateway.dto;

/**
 * Notificacion tal como la devuelve notification-service
 * (NotificacionResponse): id, ticketId, mensaje, fechaEnvio. fechaEnvio se
 * expone como String ISO-8601 sin reinterpretarla, igual que en REST.
 */
public record NotificacionDto(
        String id,
        String ticketId,
        String mensaje,
        String fechaEnvio) {
}
