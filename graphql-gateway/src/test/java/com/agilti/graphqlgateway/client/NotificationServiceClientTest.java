package com.agilti.graphqlgateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.agilti.graphqlgateway.dto.NotificacionDto;
import com.agilti.graphqlgateway.dto.NotificacionPageDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Verifica las URLs que arma el cliente hacia notification-service y el
 * mapeo de NotificacionResponse / PageResponse, con el servicio simulado.
 */
class NotificationServiceClientTest {

    private static final String NOTIF_JSON = """
            {"id":"7c9e6679-7425-40de-944b-e07fc1f90ae7",
             "ticketId":"550e8400-e29b-41d4-a716-446655440000",
             "mensaje":"Se creo el ticket","fechaEnvio":"2026-03-10T14:32:05-05:00"}
            """;

    private MockRestServiceServer server;
    private NotificationServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notification-service:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NotificationServiceClient(builder.build());
    }

    @Test
    void listarNotificacionesEnviaPageYSizeYMapeaLaPagina() {
        server.expect(requestTo("http://notification-service:8081/api/notificaciones?page=1&size=5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"content\":[" + NOTIF_JSON + "],\"page\":1,\"size\":5,"
                        + "\"totalElements\":6,\"totalPages\":2,\"nextPage\":false,\"previousPage\":true}",
                        MediaType.APPLICATION_JSON));

        NotificacionPageDto pagina = client.listarNotificaciones(1, 5);

        server.verify();
        assertThat(pagina.content()).hasSize(1);
        assertThat(pagina.content().get(0).fechaEnvio()).isEqualTo("2026-03-10T14:32:05-05:00");
        assertThat(pagina.totalElements()).isEqualTo(6);
        assertThat(pagina.previousPage()).isTrue();
    }

    @Test
    void historialPorTicketLlamaAlEndpointDelTicket() {
        server.expect(requestTo(
                        "http://notification-service:8081/api/notificaciones/ticket/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[" + NOTIF_JSON + "]", MediaType.APPLICATION_JSON));

        List<NotificacionDto> historial = client.historialPorTicket("550e8400-e29b-41d4-a716-446655440000");

        server.verify();
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).ticketId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(historial.get(0).mensaje()).isEqualTo("Se creo el ticket");
    }

    @Test
    void errorHttpSeEnvuelveIndicandoElServicio() {
        server.expect(requestTo("http://notification-service:8081/api/notificaciones/ticket/abc"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.historialPorTicket("abc"))
                .isInstanceOf(ServicioRemotoException.class)
                .hasCauseInstanceOf(HttpClientErrorException.BadRequest.class)
                .extracting(ex -> ((ServicioRemotoException) ex).getServicio())
                .isEqualTo("notification-service");
    }
}
