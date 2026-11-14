package com.agilti.ticketservice.exception;

import com.agilti.ticketservice.model.Estado;

/**
 * Corresponde a la respuesta 400 documentada en PATCH /api/tickets/{id}/estado:
 * "Transicion de estado no valida (ej. de CERRADO a ABIERTO)"
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(Estado actual, Estado nuevo) {
        super("No se puede cambiar el estado de " + actual + " a " + nuevo);
    }
}
