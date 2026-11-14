package com.agilti.graphqlgateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * agilti.notification-service.url     -> URL base de notification-service (NOTIFICATION_SERVICE_URL)
 * agilti.notification-service.timeout -> timeout de conexion y de lectura
 */
@ConfigurationProperties(prefix = "agilti.notification-service")
public record NotificationServiceProperties(String url, Duration timeout) {
}
