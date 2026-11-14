package com.agilti.ticketservice.repository;

import com.agilti.ticketservice.model.Estado;
import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.model.Ticket;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /**
     * Filtros opcionales por estado y/o prioridad (null = no filtrar por
     * ese campo), con paginacion. Usado por
     * GET /api/tickets?estado=..&prioridad=..&page=..&size=..
     */
    @Query("SELECT t FROM Ticket t "
            + "WHERE (:estado IS NULL OR t.estado = :estado) "
            + "AND (:prioridad IS NULL OR t.prioridad = :prioridad)")
    Page<Ticket> buscar(@Param("estado") Estado estado,
                         @Param("prioridad") Prioridad prioridad,
                         Pageable pageable);
}
