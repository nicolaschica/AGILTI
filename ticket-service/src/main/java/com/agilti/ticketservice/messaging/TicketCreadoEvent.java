package com.agilti.ticketservice.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento publicado en el topico "ticket-creado" cada vez que se crea un
 * ticket. notification-service tiene su propia copia de esta clase (no hay
 * libreria compartida entre microservicios).
 */
public class TicketCreadoEvent {

    public static final String TIPO_EVENTO = "TICKET_CREADO";

    private UUID eventoId;
    private String tipoEvento;
    private OffsetDateTime fechaEvento;
    private UUID ticketId;
    private String titulo;
    private String prioridad;
    private String tecnicoAsignado;

    public TicketCreadoEvent() {
        // requerido para (de)serializacion JSON
    }

    public TicketCreadoEvent(UUID ticketId, String titulo, String prioridad, String tecnicoAsignado) {
        this.eventoId = UUID.randomUUID();
        this.tipoEvento = TIPO_EVENTO;
        this.fechaEvento = OffsetDateTime.now();
        this.ticketId = ticketId;
        this.titulo = titulo;
        this.prioridad = prioridad;
        this.tecnicoAsignado = tecnicoAsignado;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public void setEventoId(UUID eventoId) {
        this.eventoId = eventoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public OffsetDateTime getFechaEvento() {
        return fechaEvento;
    }

    public void setFechaEvento(OffsetDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public void setTecnicoAsignado(String tecnicoAsignado) {
        this.tecnicoAsignado = tecnicoAsignado;
    }
}
