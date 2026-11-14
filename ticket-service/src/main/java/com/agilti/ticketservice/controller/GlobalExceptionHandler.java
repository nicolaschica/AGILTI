package com.agilti.ticketservice.controller;

import com.agilti.ticketservice.dto.ErrorResponse;
import com.agilti.ticketservice.exception.TicketNotFoundException;
import com.agilti.ticketservice.exception.TransicionInvalidaException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce cada excepcion al codigo HTTP y al schema ErrorResponse
 * documentados en el contrato (400 y 404 son los unicos codigos de error
 * que el contrato define).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicionInvalida(TransicionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // Orden fijo para elegir UN mensaje cuando fallan varias reglas a la vez
    // (Bean Validation no garantiza orden): primero el campo segun el
    // formulario y, dentro del campo, obligatorio > caracteres > longitud.
    // El frontend aplica el mismo orden (src/validation/ticketRules.js).
    private static final List<String> ORDEN_CAMPOS = List.of("titulo", "descripcion", "prioridad");
    private static final List<String> ORDEN_REGLAS = List.of("NotBlank", "NotNull", "Pattern", "Size");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt((FieldError e) -> posicion(ORDEN_CAMPOS, e.getField()))
                        .thenComparingInt(e -> posicion(ORDEN_REGLAS, e.getCode())))
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Datos de entrada invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(mensaje));
    }

    // Se dispara, por ejemplo, si "id" en la URL no es un UUID valido, o si
    // "prioridad"/"estado" traen un valor fuera del enum del contrato.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = "Valor invalido para '" + ex.getName() + "': " + ex.getValue();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(mensaje));
    }

    // Body ilegible: JSON mal formado, o un valor que no encaja en el tipo
    // del campo (por ejemplo "prioridad" fuera del enum del contrato).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String mensaje = "JSON mal formado en el cuerpo de la peticion";
        if (ex.getCause() instanceof InvalidFormatException invalidFormat && !invalidFormat.getPath().isEmpty()) {
            String campo = invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            mensaje = "Valor invalido para '" + campo + "': " + invalidFormat.getValue();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(mensaje));
    }

    private static int posicion(List<String> orden, String valor) {
        int i = orden.indexOf(valor);
        return i < 0 ? orden.size() : i;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
