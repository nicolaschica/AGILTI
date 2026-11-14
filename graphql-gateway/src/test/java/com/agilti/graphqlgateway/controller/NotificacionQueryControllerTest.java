package com.agilti.graphqlgateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agilti.graphqlgateway.client.NotificationServiceClient;
import com.agilti.graphqlgateway.client.ServicioRemotoException;
import com.agilti.graphqlgateway.dto.NotificacionDto;
import com.agilti.graphqlgateway.dto.NotificacionPageDto;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Prueba las queries notificaciones y notificacionesPorTicket con el
 * cliente REST de notification-service simulado.
 */
@GraphQlTest(NotificacionQueryController.class)
class NotificacionQueryControllerTest {

    private static final String TICKET_ID = "550e8400-e29b-41d4-a716-446655440000";

    private static final NotificacionDto NOTIFICACION = new NotificacionDto(
            "7c9e6679-7425-40de-944b-e07fc1f90ae7", TICKET_ID,
            "Se creo el ticket 'No enciende el computador' (prioridad ALTA), asignado a Carlos Perez",
            "2026-03-10T14:32:05Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private NotificationServiceClient notificationServiceClient;

    @Test
    void notificacionesUsaValoresPorDefectoYMapeaLaPagina() {
        when(notificationServiceClient.listarNotificaciones(0, 20))
                .thenReturn(new NotificacionPageDto(List.of(NOTIFICACION), 0, 20, 1, 1, false, false));

        graphQlTester.document("""
                        { notificaciones {
                            content { id ticketId mensaje fechaEnvio }
                            page size totalElements totalPages nextPage previousPage } }
                        """)
                .execute()
                .errors().verify()
                .path("notificaciones.content[0].id").entity(String.class).isEqualTo(NOTIFICACION.id())
                .path("notificaciones.content[0].ticketId").entity(String.class).isEqualTo(TICKET_ID)
                .path("notificaciones.content[0].fechaEnvio").entity(String.class).isEqualTo("2026-03-10T14:32:05Z")
                .path("notificaciones.size").entity(Integer.class).isEqualTo(20)
                .path("notificaciones.totalElements").entity(Integer.class).isEqualTo(1);

        verify(notificationServiceClient).listarNotificaciones(0, 20);
    }

    @Test
    void notificacionesPasaPageYSize() {
        when(notificationServiceClient.listarNotificaciones(2, 5))
                .thenReturn(new NotificacionPageDto(List.of(), 2, 5, 11, 3, false, true));

        graphQlTester.document("{ notificaciones(page: 2, size: 5) { content { id } page size previousPage } }")
                .execute()
                .path("notificaciones.page").entity(Integer.class).isEqualTo(2)
                .path("notificaciones.previousPage").entity(Boolean.class).isEqualTo(true)
                .path("notificaciones.content").entityList(Object.class).hasSize(0);
    }

    @Test
    void notificacionesPorTicketDevuelveElHistorial() {
        when(notificationServiceClient.historialPorTicket(TICKET_ID)).thenReturn(List.of(NOTIFICACION));

        graphQlTester.document("query($id: ID!) { notificacionesPorTicket(ticketId: $id) { id ticketId mensaje } }")
                .variable("id", TICKET_ID)
                .execute()
                .path("notificacionesPorTicket").entityList(Object.class).hasSize(1)
                .path("notificacionesPorTicket[0].mensaje").entity(String.class).isEqualTo(NOTIFICACION.mensaje());
    }

    @Test
    void ticketIdInvalidoDevuelveBadRequest() {
        String body = "{\"mensaje\":\"Valor invalido para 'ticketId': abc. Se esperaba un UUID valido.\"}";
        when(notificationServiceClient.historialPorTicket("abc")).thenThrow(new ServicioRemotoException(
                "notification-service", HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                        null, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        graphQlTester.document("{ notificacionesPorTicket(ticketId: \"abc\") { id } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "BAD_REQUEST");
                    assertThat(errors.get(0).getMessage()).contains("Se esperaba un UUID valido");
                });
    }

    @Test
    void recursoInexistenteDevuelveNotFound() {
        when(notificationServiceClient.historialPorTicket("x")).thenThrow(new ServicioRemotoException(
                "notification-service", HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                        null, new byte[0], StandardCharsets.UTF_8)));

        graphQlTester.document("{ notificacionesPorTicket(ticketId: \"x\") { id } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "NOT_FOUND");
                    assertThat(errors.get(0).getMessage())
                            .isEqualTo("No se encontro el recurso solicitado en notification-service");
                });
    }

    @Test
    void notificationServiceCaidoDevuelveServiceUnavailable() {
        when(notificationServiceClient.listarNotificaciones(anyInt(), anyInt())).thenThrow(
                new ServicioRemotoException("notification-service",
                        new ResourceAccessException("Connection refused")));

        graphQlTester.document("{ notificaciones { totalElements } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "SERVICE_UNAVAILABLE");
                    assertThat(errors.get(0).getMessage())
                            .isEqualTo("notification-service no esta disponible o no respondio a tiempo");
                });
    }
}
