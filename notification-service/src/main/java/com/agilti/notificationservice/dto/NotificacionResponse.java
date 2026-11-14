package com.agilti.notificationservice.dto;

import com.agilti.notificationservice.model.Notificacion;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Coincide exactamente con components/schemas/Notificacion del contrato.
 */
public class NotificacionResponse {

    private UUID id;
    private UUID ticketId;
    private String mensaje;
    private OffsetDateTime fechaEnvio;

    public static NotificacionResponse from(Notificacion notificacion) {
        NotificacionResponse response = new NotificacionResponse();
        response.id = notificacion.getId();
        response.ticketId = notificacion.getTicketId();
        response.mensaje = notificacion.getMensaje();
        response.fechaEnvio = notificacion.getFechaEnvio();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }
}
