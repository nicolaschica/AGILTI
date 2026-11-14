package com.agilti.graphqlgateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.dto.TicketPageDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Verifica las llamadas REST reales que arma el cliente (metodo, URL, query
 * params y cuerpo JSON) y el mapeo de las respuestas, con ticket-service
 * simulado por MockRestServiceServer.
 */
class TicketServiceClientTest {

    private static final String TICKET_JSON = """
            {"id":"550e8400-e29b-41d4-a716-446655440000","titulo":"T","descripcion":"D",
             "prioridad":"BAJA","estado":"%s","tecnicoAsignado":"Ana Gomez",
             "fechaCreacion":"2026-03-10T14:32:00-05:00"}
            """;

    private MockRestServiceServer server;
    private TicketServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ticket-service:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TicketServiceClient(builder.build());
    }

    @Test
    void listarTicketsNoEnviaFiltrosNulosYMapeaLaPagina() {
        server.expect(requestTo("http://ticket-service:8080/api/tickets?estado=CERRADO&page=1&size=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"content":[{"id":"550e8400-e29b-41d4-a716-446655440000","titulo":"T",
                          "descripcion":"D","prioridad":"BAJA","estado":"CERRADO",
                          "tecnicoAsignado":null,"fechaCreacion":"2026-03-10T14:32:00-05:00"}],
                         "page":1,"size":2,"totalElements":3,"totalPages":2,
                         "nextPage":false,"previousPage":true}
                        """, MediaType.APPLICATION_JSON));

        TicketPageDto pagina = client.listarTickets(Estado.CERRADO, null, 1, 2);

        server.verify();
        assertThat(pagina.content()).hasSize(1);
        assertThat(pagina.content().get(0).fechaCreacion()).isEqualTo("2026-03-10T14:32:00-05:00");
        assertThat(pagina.content().get(0).tecnicoAsignado()).isNull();
        assertThat(pagina.totalElements()).isEqualTo(3);
        assertThat(pagina.previousPage()).isTrue();
    }

    @Test
    void crearTicketHacePostConElCuerpoDelContrato() {
        server.expect(requestTo("http://ticket-service:8080/api/tickets"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"titulo\":\"T\",\"descripcion\":\"D\",\"prioridad\":\"BAJA\"}", true))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(TICKET_JSON.formatted("ABIERTO")));

        TicketDto creado = client.crearTicket("T", "D", Prioridad.BAJA);

        server.verify();
        assertThat(creado.estado()).isEqualTo(Estado.ABIERTO);
        assertThat(creado.tecnicoAsignado()).isEqualTo("Ana Gomez");
    }

    @Test
    void actualizarEstadoHacePatchAlEndpointDeEstado() {
        server.expect(requestTo("http://ticket-service:8080/api/tickets/550e8400-e29b-41d4-a716-446655440000/estado"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"estado\":\"EN_ATENCION\"}", true))
                .andRespond(withSuccess(TICKET_JSON.formatted("EN_ATENCION"), MediaType.APPLICATION_JSON));

        TicketDto actualizado = client.actualizarEstado("550e8400-e29b-41d4-a716-446655440000", Estado.EN_ATENCION);

        server.verify();
        assertThat(actualizado.estado()).isEqualTo(Estado.EN_ATENCION);
    }

    @Test
    void errorHttpSeEnvuelveIndicandoElServicio() {
        server.expect(requestTo("http://ticket-service:8080/api/tickets/x"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.obtenerTicket("x"))
                .isInstanceOf(ServicioRemotoException.class)
                .hasCauseInstanceOf(HttpClientErrorException.NotFound.class)
                .extracting(ex -> ((ServicioRemotoException) ex).getServicio())
                .isEqualTo("ticket-service");
    }
}
