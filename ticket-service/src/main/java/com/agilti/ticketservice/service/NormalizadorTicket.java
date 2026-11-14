package com.agilti.ticketservice.service;

import java.text.Normalizer;

/**
 * Normalizacion de los textos de un ticket antes de validar y de guardar.
 * Es idempotente: se aplica en los setters de TicketCreateRequest (para que
 * Bean Validation mida el valor ya normalizado) y otra vez en TicketService
 * al construir la entidad.
 *
 * Titulo: forma Unicode NFC (una tilde escrita como letra + acento
 * combinable cuenta como un solo caracter), recorta espacios al inicio y al
 * final y reduce espacios repetidos a uno. Solo se tocan espacios: tabs,
 * saltos de linea u otros caracteres invisibles se conservan para que la
 * lista blanca los rechace.
 *
 * Descripcion: solo se recorta (strip) al inicio y al final.
 */
public final class NormalizadorTicket {

    private NormalizadorTicket() {
    }

    public static String normalizarTitulo(String titulo) {
        if (titulo == null) {
            return null;
        }
        String nfc = Normalizer.normalize(titulo, Normalizer.Form.NFC);
        return nfc.replaceAll(" {2,}", " ").replaceAll("^ +| +$", "");
    }

    public static String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.strip();
    }
}
