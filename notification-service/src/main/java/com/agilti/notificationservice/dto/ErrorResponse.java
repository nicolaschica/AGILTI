package com.agilti.notificationservice.dto;

/**
 * El contrato OpenAPI de este servicio no define ninguna respuesta de
 * error explicita. Este DTO se agrega para cumplir con el requisito
 * general de la entrega Q10 ("manejo centralizado de errores"), cubriendo
 * el unico caso de error real posible en este endpoint: que "ticketId" en
 * la URL no sea un UUID valido (por ejemplo /api/notificaciones/ticket/abc).
 */
public class ErrorResponse {

    private String mensaje;

    public ErrorResponse() {
    }

    public ErrorResponse(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
