package com.agilti.notificationservice.controller;

import com.agilti.notificationservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejo centralizado de errores (requisito general de la entrega Q10).
 * Cubre el unico caso de error posible en este servicio: un ticketId con
 * formato invalido en la URL (no es un UUID).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = "Valor invalido para '" + ex.getName() + "': " + ex.getValue()
                + ". Se esperaba un UUID valido.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(mensaje));
    }
}
