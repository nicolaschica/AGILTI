package com.agilti.graphqlgateway.client;

import com.agilti.graphqlgateway.dto.NotificacionDto;
import com.agilti.graphqlgateway.dto.NotificacionPageDto;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unico punto de contacto con notification-service. Usa sus dos endpoints
 * de lectura (NotificacionController). Las notificaciones solo se crean al
 * consumir el evento Kafka ticket-creado, por eso aqui no hay escritura.
 */
@Component
public class NotificationServiceClient {

    static final String SERVICIO = "notification-service";

    private static final ParameterizedTypeReference<List<NotificacionDto>> LISTA =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public NotificationServiceClient(@Qualifier("notificationServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // GET /api/notificaciones?page=&size=  (mas reciente primero)
    public NotificacionPageDto listarNotificaciones(int page, int size) {
        return llamar(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/notificaciones")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(NotificacionPageDto.class));
    }

    // GET /api/notificaciones/ticket/{ticketId}  (mas reciente primero)
    public List<NotificacionDto> historialPorTicket(String ticketId) {
        return llamar(() -> restClient.get()
                .uri("/api/notificaciones/ticket/{ticketId}", ticketId)
                .retrieve()
                .body(LISTA));
    }

    private static <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientException ex) {
            throw new ServicioRemotoException(SERVICIO, ex);
        }
    }
}
