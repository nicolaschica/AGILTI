package com.agilti.graphqlgateway.controller;

import com.agilti.graphqlgateway.client.TicketServiceClient;
import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * Resuelve las mutations de schema.graphqls delegando por REST en
 * ticket-service. No valida ni aplica reglas de negocio: si ticket-service
 * rechaza los datos (400) o no encuentra el ticket (404), el error llega al
 * cliente como BAD_REQUEST / NOT_FOUND via GraphQlExceptionResolver.
 */
@Controller
public class TicketMutationController {

    private final TicketServiceClient ticketServiceClient;

    public TicketMutationController(TicketServiceClient ticketServiceClient) {
        this.ticketServiceClient = ticketServiceClient;
    }

    // crearTicket(titulo, descripcion, prioridad): Ticket!  -> POST /api/tickets
    @MutationMapping
    public TicketDto crearTicket(@Argument String titulo, @Argument String descripcion,
                                 @Argument Prioridad prioridad) {
        return ticketServiceClient.crearTicket(titulo, descripcion, prioridad);
    }

    // actualizarEstadoTicket(id, estado): Ticket!  -> PATCH /api/tickets/{id}/estado
    @MutationMapping
    public TicketDto actualizarEstadoTicket(@Argument String id, @Argument Estado estado) {
        return ticketServiceClient.actualizarEstado(id, estado);
    }
}
