package com.agilti.notificationservice.config;

import com.agilti.notificationservice.messaging.TicketCreadoEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Consumidor Kafka: clave String y valor JSON deserializado a la copia local
 * de TicketCreadoEvent. ErrorHandlingDeserializer evita que un mensaje mal
 * formado deje al consumidor atascado reintentando el mismo registro.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, TicketCreadoEvent> ticketCreadoConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Se ignoran las cabeceras de tipo: siempre se deserializa a la clase
        // local, sin importar el nombre de clase que use el productor.
        JsonDeserializer<TicketCreadoEvent> jsonDeserializer = new JsonDeserializer<>(TicketCreadoEvent.class, false);
        jsonDeserializer.addTrustedPackages("com.agilti.notificationservice.messaging");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketCreadoEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TicketCreadoEvent> ticketCreadoConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, TicketCreadoEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ticketCreadoConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
