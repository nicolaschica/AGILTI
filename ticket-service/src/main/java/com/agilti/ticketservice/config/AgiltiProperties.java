package com.agilti.ticketservice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El contrato OpenAPI no especifica COMO se asigna tecnicoAsignado, solo
 * que el Ticket devuelto por POST /api/tickets ya lo trae (ver el ejemplo
 * de la respuesta 201). Esta lista fija es la implementacion minima de esa
 * regla de negocio (asignacion round-robin), sin introducir ningun
 * endpoint ni campo que no este en el contrato.
 */
@ConfigurationProperties(prefix = "agilti")
public class AgiltiProperties {

    private List<String> tecnicos = List.of();

    public List<String> getTecnicos() {
        return tecnicos;
    }

    public void setTecnicos(List<String> tecnicos) {
        this.tecnicos = tecnicos;
    }
}
