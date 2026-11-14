package com.agilti.graphqlgateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * agilti.ticket-service.url     -> URL base de ticket-service (TICKET_SERVICE_URL)
 * agilti.ticket-service.timeout -> timeout de conexion y de lectura
 */
@ConfigurationProperties(prefix = "agilti.ticket-service")
public record TicketServiceProperties(String url, Duration timeout) {
}
