package com.agilti.graphqlgateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agilti.graphqlgateway.client.ServicioRemotoException;
import com.agilti.graphqlgateway.client.TicketServiceClient;
import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.dto.TicketPageDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
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
 * Prueba las queries de tickets (esquema + controller + resolver de
 * errores) con el cliente REST de ticket-service simulado: no necesita
 * ningun servicio levantado.
 */
@GraphQlTest(TicketQueryController.class)
class TicketQueryControllerTest {

    static final TicketDto TICKET = new TicketDto(
            "550e8400-e29b-41d4-a716-446655440000", "No enciende el computador",
            "El equipo del area contable no enciende", Prioridad.ALTA, Estado.ABIERTO,
            "Carlos Perez", "2026-03-10T14:32:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private TicketServiceClient ticketServiceClient;

    @Test
    void ticketsMapeaPageResponseYPasaFiltros() {
        when(ticketServiceClient.listarTickets(Estado.ABIERTO, Prioridad.ALTA, 0, 5))
                .thenReturn(new TicketPageDto(List.of(TICKET), 0, 5, 1, 1, false, false));

        graphQlTester.document("""
                        { tickets(estado: ABIERTO, prioridad: ALTA, size: 5) {
                            content { id titulo prioridad estado tecnicoAsignado fechaCreacion }
                            page size totalElements totalPages nextPage previousPage } }
                        """)
                .execute()
                .path("tickets.content[0].id").entity(String.class).isEqualTo(TICKET.id())
                .path("tickets.content[0].prioridad").entity(String.class).isEqualTo("ALTA")
                .path("tickets.content[0].fechaCreacion").entity(String.class).isEqualTo("2026-03-10T14:32:00Z")
                .path("tickets.size").entity(Integer.class).isEqualTo(5)
                .path("tickets.totalElements").entity(Integer.class).isEqualTo(1)
                .path("tickets.nextPage").entity(Boolean.class).isEqualTo(false);

        verify(ticketServiceClient).listarTickets(Estado.ABIERTO, Prioridad.ALTA, 0, 5);
    }

    @Test
    void ticketPorIdDevuelveElTicket() {
        when(ticketServiceClient.obtenerTicket(TICKET.id())).thenReturn(TICKET);

        graphQlTester.document("query($id: ID!) { ticket(id: $id) { id titulo estado } }")
                .variable("id", TICKET.id())
                .execute()
                .path("ticket.titulo").entity(String.class).isEqualTo(TICKET.titulo())
                .path("ticket.estado").entity(String.class).isEqualTo("ABIERTO");
    }

    @Test
    void ticketInexistenteDevuelveNotFound() {
        String body = "{\"mensaje\":\"No se encontro el ticket con id x\"}";
        when(ticketServiceClient.obtenerTicket("x")).thenThrow(new ServicioRemotoException("ticket-service",
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null,
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        graphQlTester.document("{ ticket(id: \"x\") { id } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "NOT_FOUND");
                    assertThat(errors.get(0).getMessage()).isEqualTo("No se encontro el ticket con id x");
                })
                .path("ticket").valueIsNull();
    }

    @Test
    void ticketServiceCaidoDevuelveServiceUnavailable() {
        when(ticketServiceClient.listarTickets(any(), any(), anyInt(), anyInt()))
                .thenThrow(new ServicioRemotoException("ticket-service",
                        new ResourceAccessException("Connection refused")));

        graphQlTester.document("{ tickets { totalElements } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "SERVICE_UNAVAILABLE");
                    assertThat(errors.get(0).getMessage())
                            .isEqualTo("ticket-service no esta disponible o no respondio a tiempo")
                            .doesNotContain("Exception");
                });
    }
}
