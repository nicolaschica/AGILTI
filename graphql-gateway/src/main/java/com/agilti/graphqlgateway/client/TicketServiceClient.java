package com.agilti.graphqlgateway.client;

import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.dto.TicketPageDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unico punto de contacto con ticket-service. Usa los 4 endpoints de su
 * contrato REST (lectura y escritura) sin agregar logica de negocio: las
 * validaciones, la asignacion de tecnico, la publicacion a Kafka y las
 * reglas de transicion de estado siguen viviendo en ticket-service. Los
 * errores HTTP (4xx/5xx) y de conexion se propagan como
 * ServicioRemotoException.
 */
@Component
public class TicketServiceClient {

    static final String SERVICIO = "ticket-service";

    private final RestClient restClient;

    public TicketServiceClient(@Qualifier("ticketServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // GET /api/tickets?estado=&prioridad=&page=&size= (los filtros nulos no se envian)
    public TicketPageDto listarTickets(Estado estado, Prioridad prioridad, int page, int size) {
        return llamar(() -> restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/tickets");
                    if (estado != null) {
                        uriBuilder.queryParam("estado", estado);
                    }
                    if (prioridad != null) {
                        uriBuilder.queryParam("prioridad", prioridad);
                    }
                    return uriBuilder
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build();
                })
                .retrieve()
                .body(TicketPageDto.class));
    }

    // GET /api/tickets/{id}
    public TicketDto obtenerTicket(String id) {
        return llamar(() -> restClient.get()
                .uri("/api/tickets/{id}", id)
                .retrieve()
                .body(TicketDto.class));
    }

    // POST /api/tickets  body: { titulo, descripcion, prioridad }
    public TicketDto crearTicket(String titulo, String descripcion, Prioridad prioridad) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", titulo);
        body.put("descripcion", descripcion);
        body.put("prioridad", prioridad);
        return llamar(() -> restClient.post()
                .uri("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TicketDto.class));
    }

    // PATCH /api/tickets/{id}/estado  body: { estado }
    public TicketDto actualizarEstado(String id, Estado estado) {
        return llamar(() -> restClient.patch()
                .uri("/api/tickets/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("estado", estado))
                .retrieve()
                .body(TicketDto.class));
    }

    private static <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientException ex) {
            throw new ServicioRemotoException(SERVICIO, ex);
        }
    }
}
