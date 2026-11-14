package com.agilti.ticketservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persistencia del schema "Ticket" del contrato: id (uuid), titulo,
 * descripcion, prioridad, estado, tecnicoAsignado (nullable), fechaCreacion.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Column(name = "tecnico_asignado")
    private String tecnicoAsignado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    protected Ticket() {
        // requerido por JPA
    }

    public Ticket(String titulo, String descripcion, Prioridad prioridad, String tecnicoAsignado) {
        this.id = UUID.randomUUID();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = Estado.ABIERTO;
        this.tecnicoAsignado = tecnicoAsignado;
        this.fechaCreacion = OffsetDateTime.now();
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

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
