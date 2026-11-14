package com.agilti.notificationservice.repository;

import com.agilti.notificationservice.model.Notificacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    // El contrato pide: "ordenadas de la mas reciente a la mas antigua"
    List<Notificacion> findByTicketIdOrderByFechaEnvioDesc(UUID ticketId);

    // Usado por TicketCreadoListener para no duplicar notificaciones
    boolean existsByTicketIdAndMensaje(UUID ticketId, String mensaje);
}
