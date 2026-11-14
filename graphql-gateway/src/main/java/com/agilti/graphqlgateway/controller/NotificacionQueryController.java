package com.agilti.graphqlgateway.controller;

import com.agilti.graphqlgateway.client.NotificationServiceClient;
import com.agilti.graphqlgateway.dto.NotificacionDto;
import com.agilti.graphqlgateway.dto.NotificacionPageDto;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Resuelve las queries de notificaciones de schema.graphqls delegando por
 * REST en notification-service.
 */
@Controller
public class NotificacionQueryController {

    private final NotificationServiceClient notificationServiceClient;

    public NotificacionQueryController(NotificationServiceClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    // notificaciones(page = 0, size = 20): NotificacionPage!  -> GET /api/notificaciones
    @QueryMapping
    public NotificacionPageDto notificaciones(@Argument int page, @Argument int size) {
        return notificationServiceClient.listarNotificaciones(page, size);
    }

    // notificacionesPorTicket(ticketId: ID!): [Notificacion!]!  -> GET /api/notificaciones/ticket/{ticketId}
    @QueryMapping
    public List<NotificacionDto> notificacionesPorTicket(@Argument String ticketId) {
        return notificationServiceClient.historialPorTicket(ticketId);
    }
}
