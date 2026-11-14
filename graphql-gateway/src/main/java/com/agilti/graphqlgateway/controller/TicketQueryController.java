package com.agilti.graphqlgateway.controller;

import com.agilti.graphqlgateway.client.TicketServiceClient;
import com.agilti.graphqlgateway.dto.TicketDto;
import com.agilti.graphqlgateway.dto.TicketPageDto;
import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Resuelve las queries de tickets de schema.graphqls delegando en
 * ticket-service. Las mutations estan en TicketMutationController y las
 * notificaciones en NotificacionQueryController.
 */
@Controller
public class TicketQueryController {

    private final TicketServiceClient ticketServiceClient;

    public TicketQueryController(TicketServiceClient ticketServiceClient) {
        this.ticketServiceClient = ticketServiceClient;
    }

    // tickets(estado, prioridad, page = 0, size = 10): TicketPage!
    @QueryMapping
    public TicketPageDto tickets(@Argument Estado estado, @Argument Prioridad prioridad,
                                 @Argument int page, @Argument int size) {
        return ticketServiceClient.listarTickets(estado, prioridad, page, size);
    }

    // ticket(id: ID!): Ticket
    @QueryMapping
    public TicketDto ticket(@Argument String id) {
        return ticketServiceClient.obtenerTicket(id);
    }
}
