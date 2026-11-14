package com.agilti.graphqlgateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.agilti.graphqlgateway.client.ServicioRemotoException;
import com.agilti.graphqlgateway.client.TicketServiceClient;
import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Prueba las mutations crearTicket y actualizarEstadoTicket con el cliente
 * REST de ticket-service simulado: argumentos que se delegan, mapeo de la
 * respuesta y traduccion de 400 / 404 / servicio caido a extensions.code.
 */
@GraphQlTest(TicketMutationController.class)
class TicketMutationControllerTest {

    private static final TicketDto TICKET = TicketQueryControllerTest.TICKET;

    private static final String CREAR = """
            mutation($titulo: String!, $descripcion: String!, $prioridad: Prioridad!) {
              crearTicket(titulo: $titulo, descripcion: $descripcion, prioridad: $prioridad) {
                id titulo descripcion prioridad estado tecnicoAsignado fechaCreacion
              }
            }
            """;

    private static final String ACTUALIZAR = """
            mutation($id: ID!, $estado: Estado!) {
              actualizarEstadoTicket(id: $id, estado: $estado) { id estado }
            }
            """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private TicketServiceClient ticketServiceClient;

    @Test
    void crearTicketDelegaEnTicketServiceYDevuelveElTicketCreado() {
        when(ticketServiceClient.crearTicket(TICKET.titulo(), TICKET.descripcion(), Prioridad.ALTA))
                .thenReturn(TICKET);

        graphQlTester.document(CREAR)
                .variable("titulo", TICKET.titulo())
                .variable("descripcion", TICKET.descripcion())
                .variable("prioridad", "ALTA")
                .execute()
                .errors().verify()
                .path("crearTicket.id").entity(String.class).isEqualTo(TICKET.id())
                .path("crearTicket.estado").entity(String.class).isEqualTo("ABIERTO")
                .path("crearTicket.tecnicoAsignado").entity(String.class).isEqualTo("Carlos Perez");

        verify(ticketServiceClient).crearTicket(TICKET.titulo(), TICKET.descripcion(), Prioridad.ALTA);
    }

    @Test
    void crearTicketRechazadoPorTicketServiceDevuelveBadRequest() {
        String body = "{\"mensaje\":\"titulo: El titulo es obligatorio\"}";
        when(ticketServiceClient.crearTicket(any(), any(), any())).thenThrow(new ServicioRemotoException(
                "ticket-service", HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        graphQlTester.document(CREAR)
                .variable("titulo", " ")
                .variable("descripcion", "d")
                .variable("prioridad", "BAJA")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "BAD_REQUEST");
                    assertThat(errors.get(0).getMessage()).isEqualTo("titulo: El titulo es obligatorio");
                });
    }

    @Test
    void crearTicketSinArgumentoObligatorioLoRechazaElEsquema() {
        graphQlTester.document("mutation { crearTicket(titulo: \"x\") { id } }")
                .execute()
                .errors().satisfy(errors -> assertThat(errors).isNotEmpty());

        verifyNoInteractions(ticketServiceClient);
    }

    @Test
    void actualizarEstadoDelegaEnTicketService() {
        TicketDto enAtencion = new TicketDto(TICKET.id(), TICKET.titulo(), TICKET.descripcion(),
                TICKET.prioridad(), Estado.EN_ATENCION, TICKET.tecnicoAsignado(), TICKET.fechaCreacion());
        when(ticketServiceClient.actualizarEstado(TICKET.id(), Estado.EN_ATENCION)).thenReturn(enAtencion);

        graphQlTester.document(ACTUALIZAR)
                .variable("id", TICKET.id())
                .variable("estado", "EN_ATENCION")
                .execute()
                .errors().verify()
                .path("actualizarEstadoTicket.id").entity(String.class).isEqualTo(TICKET.id())
                .path("actualizarEstadoTicket.estado").entity(String.class).isEqualTo("EN_ATENCION");

        verify(ticketServiceClient).actualizarEstado(TICKET.id(), Estado.EN_ATENCION);
    }

    @Test
    void transicionInvalidaDevuelveBadRequestConElMensajeDeTicketService() {
        String body = "{\"mensaje\":\"Transicion invalida: CERRADO -> ABIERTO\"}";
        when(ticketServiceClient.actualizarEstado(TICKET.id(), Estado.ABIERTO)).thenThrow(
                new ServicioRemotoException("ticket-service", HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", null,
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        graphQlTester.document(ACTUALIZAR)
                .variable("id", TICKET.id())
                .variable("estado", "ABIERTO")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "BAD_REQUEST");
                    assertThat(errors.get(0).getMessage()).isEqualTo("Transicion invalida: CERRADO -> ABIERTO");
                })
                .path("actualizarEstadoTicket").pathDoesNotExist();
    }

    @Test
    void actualizarEstadoDeTicketInexistenteDevuelveNotFound() {
        String body = "{\"mensaje\":\"No se encontro el ticket con id 00000000-0000-0000-0000-000000000000\"}";
        when(ticketServiceClient.actualizarEstado(anyString(), any())).thenThrow(new ServicioRemotoException(
                "ticket-service", HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null,
                        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        graphQlTester.document(ACTUALIZAR)
                .variable("id", "00000000-0000-0000-0000-000000000000")
                .variable("estado", "CERRADO")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "NOT_FOUND");
                    assertThat(errors.get(0).getMessage()).startsWith("No se encontro el ticket");
                });
    }

    @Test
    void ticketServiceCaidoEnMutationDevuelveServiceUnavailable() {
        when(ticketServiceClient.crearTicket(any(), any(), any())).thenThrow(new ServicioRemotoException(
                "ticket-service", new ResourceAccessException("Connection refused")));

        graphQlTester.document(CREAR)
                .variable("titulo", "t")
                .variable("descripcion", "d")
                .variable("prioridad", "MEDIA")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getExtensions()).containsEntry("code", "SERVICE_UNAVAILABLE");
                    assertThat(errors.get(0).getMessage()).doesNotContain("Exception");
                });
    }
}
