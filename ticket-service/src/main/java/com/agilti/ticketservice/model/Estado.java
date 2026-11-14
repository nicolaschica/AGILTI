package com.agilti.ticketservice.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Valores exactos del schema "Estado" del contrato OpenAPI:
 * enum: [ABIERTO, EN_ATENCION, CERRADO]
 *
 * El contrato especifica en su descripcion: "Transiciones permitidas:
 * ABIERTO -> EN_ATENCION, EN_ATENCION -> CERRADO. No se permiten
 * retrocesos en el MVP." Esa regla se implementa aqui.
 */
public enum Estado {
    ABIERTO,
    EN_ATENCION,
    CERRADO;

    private static final Map<Estado, Set<Estado>> TRANSICIONES_VALIDAS = new EnumMap<>(Estado.class);

    static {
        TRANSICIONES_VALIDAS.put(ABIERTO, Set.of(EN_ATENCION));
        TRANSICIONES_VALIDAS.put(EN_ATENCION, Set.of(CERRADO));
        TRANSICIONES_VALIDAS.put(CERRADO, Set.of());
    }

    public boolean puedeTransicionarA(Estado nuevoEstado) {
        return TRANSICIONES_VALIDAS.getOrDefault(this, Set.of()).contains(nuevoEstado);
    }
}
