package com.agilti.ticketservice.controller;

import com.agilti.ticketservice.dto.EstadoUpdateRequest;
import com.agilti.ticketservice.dto.PageResponse;
import com.agilti.ticketservice.dto.TicketCreateRequest;
import com.agilti.ticketservice.dto.TicketResponse;
import com.agilti.ticketservice.model.Estado;
import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementa los 4 endpoints de ticket-service-openapi.yaml. Se agrego
 * paginacion y filtros opcionales en el listado (estado, prioridad, page,
 * size), requeridos por la entrega Q10 "API REST y persistencia funcional"
 * ("Filtros y paginacion cuando correspondan al caso de uso") — un listado
 * de tickets es justamente un caso donde esto aplica, ya que puede crecer
 * sin limite con el tiempo.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Operaciones sobre el ciclo de vida de un ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // POST /api/tickets -> operationId: crearTicket
    @PostMapping
    @Operation(summary = "Crear un nuevo ticket", operationId = "crearTicket")
    public ResponseEntity<TicketResponse> crearTicket(@Valid @RequestBody TicketCreateRequest request) {
        TicketResponse creado = ticketService.crearTicket(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + creado.getId())).body(creado);
    }

    // GET /api/tickets?estado=&prioridad=&page=&size= -> operationId: listarTickets
    @GetMapping
    @Operation(summary = "Listar tickets, con filtros opcionales y paginacion",
            operationId = "listarTickets")
    public PageResponse<TicketResponse> listarTickets(
            @Parameter(description = "Filtrar por estado (opcional)")
            @RequestParam(required = false) Estado estado,
            @Parameter(description = "Filtrar por prioridad (opcional)")
            @RequestParam(required = false) Prioridad prioridad,
            @Parameter(description = "Numero de pagina, empieza en 0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de resultados por pagina")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        return ticketService.listarTickets(estado, prioridad, pageable);
    }

    // GET /api/tickets/{id} -> operationId: obtenerTicket
    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de un ticket", operationId = "obtenerTicket")
    public TicketResponse obtenerTicket(@PathVariable UUID id) {
        return ticketService.obtenerTicket(id);
    }

    // PATCH /api/tickets/{id}/estado -> operationId: actualizarEstadoTicket
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado de un ticket", operationId = "actualizarEstadoTicket")
    public TicketResponse actualizarEstadoTicket(@PathVariable UUID id,
                                                  @Valid @RequestBody EstadoUpdateRequest request) {
        return ticketService.actualizarEstado(id, request.getEstado());
    }
}
