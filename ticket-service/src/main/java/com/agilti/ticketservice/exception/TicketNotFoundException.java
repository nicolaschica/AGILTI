package com.agilti.ticketservice.exception;

import java.util.UUID;

/**
 * Produce el mismo formato de mensaje que el "example" de la respuesta 404
 * documentada en el contrato: "No se encontro el ticket con id {uuid}"
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(UUID id) {
        super("No se encontro el ticket con id " + id);
    }
}
