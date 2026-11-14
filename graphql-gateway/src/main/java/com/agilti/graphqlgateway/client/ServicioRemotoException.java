package com.agilti.graphqlgateway.client;

import org.springframework.web.client.RestClientException;

/**
 * Envuelve cualquier error de RestClient indicando que servicio de backend
 * fallo, para que GraphQlExceptionResolver lo nombre en el mensaje (por
 * ejemplo "notification-service no esta disponible...").
 */
public class ServicioRemotoException extends RuntimeException {

    private final String servicio;

    public ServicioRemotoException(String servicio, RestClientException cause) {
        super(servicio + ": " + cause.getMessage(), cause);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
