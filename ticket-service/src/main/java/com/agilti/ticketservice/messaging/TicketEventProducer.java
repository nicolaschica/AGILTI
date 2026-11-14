package com.agilti.ticketservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica el evento TicketCreadoEvent en Kafka. El envio es asincrono: la
 * peticion HTTP no espera la confirmacion del broker, solo se loggea el
 * resultado cuando llega.
 */
@Component
public class TicketEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TicketEventProducer.class);

    private final KafkaTemplate<String, TicketCreadoEvent> kafkaTemplate;
    private final String topicTicketCreado;

    public TicketEventProducer(KafkaTemplate<String, TicketCreadoEvent> kafkaTemplate,
                               @Value("${agilti.kafka.topic-ticket-creado}") String topicTicketCreado) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicTicketCreado = topicTicketCreado;
    }

    public void publicarTicketCreado(TicketCreadoEvent evento) {
        // La clave es el ticketId para que todos los eventos de un mismo
        // ticket caigan en la misma particion (orden garantizado).
        String clave = evento.getTicketId().toString();

        kafkaTemplate.send(topicTicketCreado, clave, evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("Error publicando evento {} del ticket {} en el topico {}",
                                evento.getEventoId(), evento.getTicketId(), topicTicketCreado, error);
                    } else {
                        log.info("Evento {} del ticket {} publicado en {} (particion {}, offset {})",
                                evento.getEventoId(), evento.getTicketId(), topicTicketCreado,
                                resultado.getRecordMetadata().partition(),
                                resultado.getRecordMetadata().offset());
                    }
                });
    }
}
