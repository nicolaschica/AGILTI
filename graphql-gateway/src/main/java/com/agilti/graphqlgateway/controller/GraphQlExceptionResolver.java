package com.agilti.graphqlgateway.controller;

import com.agilti.graphqlgateway.client.ServicioRemotoException;
import com.agilti.graphqlgateway.dto.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Traduce los errores de ticket-service y notification-service (queries y
 * mutations) a errores GraphQL con extensions.code: NOT_FOUND (HTTP 404),
 * BAD_REQUEST (HTTP 400), SERVICE_UNAVAILABLE (servicio caido o sin
 * respuesta a tiempo) e INTERNAL_ERROR (otro error HTTP). Nunca expone
 * stack traces: solo un mensaje legible para el cliente.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionResolver.class);

    private final ObjectMapper objectMapper;

    public GraphQlExceptionResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        // Los clientes REST envuelven el error indicando que servicio fallo
        String servicio = "el servicio de backend";
        Throwable causa = ex;
        if (ex instanceof ServicioRemotoException remoto) {
            servicio = remoto.getServicio();
            causa = remoto.getCause();
        }

        if (causa instanceof HttpClientErrorException.NotFound notFound) {
            return error(env, ErrorType.NOT_FOUND, "NOT_FOUND",
                    mensajeDe(notFound, "No se encontro el recurso solicitado en " + servicio));
        }
        if (causa instanceof HttpClientErrorException.BadRequest badRequest) {
            return error(env, ErrorType.BAD_REQUEST, "BAD_REQUEST",
                    mensajeDe(badRequest, "Datos invalidos para " + servicio));
        }
        if (causa instanceof ResourceAccessException) {
            log.warn("{} no disponible: {}", servicio, causa.getMessage());
            return error(env, ErrorType.INTERNAL_ERROR, "SERVICE_UNAVAILABLE",
                    servicio + " no esta disponible o no respondio a tiempo");
        }
        if (causa instanceof RestClientResponseException responseEx) {
            log.warn("{} respondio {}: {}", servicio, responseEx.getStatusCode(), causa.getMessage());
            return error(env, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR",
                    servicio + " respondio con un error inesperado");
        }
        // Cualquier otra excepcion: Spring GraphQL devuelve un INTERNAL_ERROR
        // generico, tambien sin stack trace.
        return null;
    }

    private static GraphQLError error(DataFetchingEnvironment env, ErrorType type, String code, String message) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }

    // Reusa el campo "mensaje" del ErrorResponse del servicio si viene
    private String mensajeDe(RestClientResponseException ex, String porDefecto) {
        try {
            ErrorResponse body = objectMapper.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
            if (body != null && body.mensaje() != null && !body.mensaje().isBlank()) {
                return body.mensaje();
            }
        } catch (JsonProcessingException ignored) {
            // cuerpo vacio o no JSON: se usa el mensaje por defecto
        }
        return porDefecto;
    }
}
