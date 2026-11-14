package com.agilti.notificationservice.controller;

import com.agilti.notificationservice.dto.NotificacionResponse;
import com.agilti.notificationservice.dto.PageResponse;
import com.agilti.notificationservice.repository.NotificacionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementa los endpoints de lectura de notification-service-openapi.yaml:
 * GET /api/notificaciones  (listado paginado, que el frontend consume via graphql-gateway) y
 * GET /api/notificaciones/ticket/{ticketId}.
 *
 * No se agrego ningun endpoint de escritura (POST/PUT/DELETE): el contrato
 * solo define lectura, ya que en la arquitectura real las notificaciones
 * se crean al consumir eventos de Kafka, no via REST. Esa parte queda
 * fuera de este microservicio REST tal como se pidio explicitamente
 * ("no agregar funcionalidades que no esten definidas en el contrato").
 *
 * El contrato no define respuestas de error para el caso normal (un
 * ticketId sin notificaciones simplemente devuelve 200 con un arreglo
 * vacio). Se agrego un GlobalExceptionHandler solo para el caso de
 * formato invalido de ticketId, como buena practica general de la
 * entrega (ver GlobalExceptionHandler.java).
 */
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Historial de notificaciones simuladas por ticket")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    // GET /api/notificaciones?page=&size= -> operationId: listarNotificaciones
    // Agregado para el frontend (lo consume via graphql-gateway): listado general paginado, mas reciente primero.
    @GetMapping
    @Operation(summary = "Listar todas las notificaciones, con paginacion",
            operationId = "listarNotificaciones")
    public PageResponse<NotificacionResponse> listarNotificaciones(
            @Parameter(description = "Numero de pagina, empieza en 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de resultados por pagina")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaEnvio").descending());
        return PageResponse.from(notificacionRepository.findAll(pageable).map(NotificacionResponse::from));
    }

    // GET /api/notificaciones/ticket/{ticketId} -> operationId: historialPorTicket
    @GetMapping("/ticket/{ticketId}")
    @Operation(summary = "Consultar el historial de notificaciones de un ticket",
            operationId = "historialPorTicket")
    public List<NotificacionResponse> historialPorTicket(@PathVariable UUID ticketId) {
        return notificacionRepository.findByTicketIdOrderByFechaEnvioDesc(ticketId).stream()
                .map(NotificacionResponse::from)
                .toList();
    }
}
