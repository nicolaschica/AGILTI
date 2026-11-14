package com.agilti.graphqlgateway.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient por servicio de backend (ticket-service y
 * notification-service), cada uno con su URL base y timeout configurables.
 * Si el servicio esta caido o no responde a tiempo se lanza
 * ResourceAccessException, que GraphQlExceptionResolver traduce a
 * SERVICE_UNAVAILABLE.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient ticketServiceRestClient(RestClient.Builder builder, TicketServiceProperties properties) {
        return crear(builder, properties.url(), properties.timeout());
    }

    @Bean
    public RestClient notificationServiceRestClient(RestClient.Builder builder,
                                                    NotificationServiceProperties properties) {
        return crear(builder, properties.url(), properties.timeout());
    }

    // RestClient.Builder es mutable: se clona para que cada bean tenga su propia URL base
    private static RestClient crear(RestClient.Builder builder, String url, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return builder.clone()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();
    }
}
