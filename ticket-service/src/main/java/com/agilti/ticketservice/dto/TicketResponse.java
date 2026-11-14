package com.agilti.ticketservice.dto;

import com.agilti.ticketservice.model.Estado;
import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.model.Ticket;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Coincide exactamente con components/schemas/Ticket del contrato:
 * id, titulo, descripcion, prioridad, estado, tecnicoAsignado (nullable),
 * fechaCreacion.
 */
public class TicketResponse {

    private UUID id;
    private String titulo;
    private String descripcion;
    private Prioridad prioridad;
    private Estado estado;
    private String tecnicoAsignado;
    private OffsetDateTime fechaCreacion;

    public static TicketResponse from(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.id = ticket.getId();
        response.titulo = ticket.getTitulo();
        response.descripcion = ticket.getDescripcion();
        response.prioridad = ticket.getPrioridad();
        response.estado = ticket.getEstado();
        response.tecnicoAsignado = ticket.getTecnicoAsignado();
        response.fechaCreacion = ticket.getFechaCreacion();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
