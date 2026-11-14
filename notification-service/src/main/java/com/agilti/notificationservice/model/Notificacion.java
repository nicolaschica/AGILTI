package com.agilti.notificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Coincide con components/schemas/Notificacion del contrato: id, ticketId,
 * mensaje, fechaEnvio.
 *
 * ticketId es una referencia LOGICA (columna UUID normal), NO una llave
 * foranea fisica hacia la tabla "ticket" de ticket-service — el propio
 * contrato lo aclara explicitamente en la descripcion del parametro
 * ticketId: "no es una llave foranea fisica, ya que ticket_db y
 * notification_db son bases de datos independientes".
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_envio", nullable = false)
    private OffsetDateTime fechaEnvio;

    protected Notificacion() {
        // requerido por JPA
    }

    public Notificacion(UUID ticketId, String mensaje) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.mensaje = mensaje;
        this.fechaEnvio = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }
}
