package com.agilti.graphqlgateway.dto;

import com.agilti.graphqlgateway.model.Estado;
import com.agilti.graphqlgateway.model.Prioridad;

/**
 * Ticket tal como lo devuelve ticket-service (TicketResponse). fechaCreacion
 * se recibe y se expone como String ISO-8601, sin reinterpretarla, para que
 * GraphQL devuelva exactamente el mismo valor que REST.
 */
public record TicketDto(
        String id,
        String titulo,
        String descripcion,
        Prioridad prioridad,
        Estado estado,
        String tecnicoAsignado,
        String fechaCreacion) {
}
