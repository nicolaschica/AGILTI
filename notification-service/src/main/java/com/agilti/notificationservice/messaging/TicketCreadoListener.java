package com.agilti.notificationservice.messaging;

import com.agilti.notificationservice.model.Notificacion;
import com.agilti.notificationservice.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consume el topico "ticket-creado" y registra una Notificacion por cada
 * ticket nuevo. Es la unica forma en que se crean notificaciones (el
 * contrato REST solo expone lectura).
 */
@Component
public class TicketCreadoListener {

    private static final Logger log = LoggerFactory.getLogger(TicketCreadoListener.class);

    private final NotificacionRepository notificacionRepository;

    public TicketCreadoListener(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @KafkaListener(topics = "${agilti.kafka.topic-ticket-creado}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTicketCreado(TicketCreadoEvent evento) {
        log.info("Evento {} recibido: ticket {} creado", evento.getEventoId(), evento.getTicketId());

        String mensaje = construirMensaje(evento);

        // Idempotencia basica: si Kafka reentrega el mismo evento, el mensaje
        // generado es identico y no se vuelve a guardar.
        if (notificacionRepository.existsByTicketIdAndMensaje(evento.getTicketId(), mensaje)) {
            log.warn("Evento {} duplicado: ya existe una notificacion para el ticket {}, se ignora",
                    evento.getEventoId(), evento.getTicketId());
            return;
        }

        Notificacion notificacion = notificacionRepository.save(new Notificacion(evento.getTicketId(), mensaje));
        log.info("Notificacion {} guardada para el ticket {}", notificacion.getId(), notificacion.getTicketId());
    }

    private String construirMensaje(TicketCreadoEvent evento) {
        String tecnico = evento.getTecnicoAsignado() != null ? evento.getTecnicoAsignado() : "ningun tecnico";
        return String.format("Nuevo ticket \"%s\" (prioridad %s) asignado a %s.",
                evento.getTitulo(), evento.getPrioridad(), tecnico);
    }
}
