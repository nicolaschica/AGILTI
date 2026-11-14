package com.agilti.ticketservice.dto;

import com.agilti.ticketservice.model.Estado;
import jakarta.validation.constraints.NotNull;

/**
 * Coincide con components/schemas/EstadoUpdateRequest: required: [estado]
 */
public class EstadoUpdateRequest {

    @NotNull(message = "El estado es obligatorio")
    private Estado estado;

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
