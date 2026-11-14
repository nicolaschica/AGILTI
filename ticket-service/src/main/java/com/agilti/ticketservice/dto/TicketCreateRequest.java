package com.agilti.ticketservice.dto;

import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.service.NormalizadorTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Coincide con components/schemas/TicketCreateRequest:
 * required: [titulo, descripcion, prioridad]
 *
 * Los setters normalizan el texto (ver NormalizadorTicket) antes de que
 * Bean Validation lo evalue, de modo que las longitudes se miden sobre el
 * valor que realmente se guarda. El frontend replica estas mismas reglas y
 * mensajes en src/validation/ticketRules.js.
 */
public class TicketCreateRequest {

    public static final int TITULO_MIN = 5;
    public static final int TITULO_MAX = 80;
    public static final int DESCRIPCION_MIN = 10;

    /** Al menos una letra; solo letras (con tildes, Ü/ü, Ñ/ñ), digitos, espacio y . , : ; - _ ( ) / # ¿ ? ¡ ! */
    public static final String TITULO_REGEX =
            "^(?=.*[A-Za-zÁÉÍÓÚÜáéíóúüÑñ])[A-Za-zÁÉÍÓÚÜáéíóúüÑñ0-9 .,:;\\-_()/#¿?¡!]+$";

    public static final String MSG_TITULO_VACIO = "El título es obligatorio.";
    public static final String MSG_TITULO_CORTO = "Escribe un título de al menos 5 caracteres.";
    public static final String MSG_TITULO_LARGO =
            "El título es muy largo. Resúmelo en pocas palabras (máximo 80 caracteres) y deja los detalles en la descripción.";
    public static final String MSG_TITULO_CARACTERES =
            "El título contiene caracteres no permitidos. Usa solo letras, números y signos básicos como . , - ( ).";
    public static final String MSG_DESCRIPCION_VACIA = "La descripción es obligatoria.";
    public static final String MSG_DESCRIPCION_CORTA =
            "La descripción debe tener al menos 10 caracteres. Cuéntanos un poco más sobre el problema.";

    @NotBlank(message = MSG_TITULO_VACIO)
    @Size(min = TITULO_MIN, message = MSG_TITULO_CORTO)
    @Size(max = TITULO_MAX, message = MSG_TITULO_LARGO)
    @Pattern(regexp = TITULO_REGEX, message = MSG_TITULO_CARACTERES)
    private String titulo;

    @NotBlank(message = MSG_DESCRIPCION_VACIA)
    @Size(min = DESCRIPCION_MIN, message = MSG_DESCRIPCION_CORTA)
    private String descripcion;

    @NotNull(message = "La prioridad es obligatoria")
    private Prioridad prioridad;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = NormalizadorTicket.normalizarTitulo(titulo);
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = NormalizadorTicket.normalizarDescripcion(descripcion);
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }
}
