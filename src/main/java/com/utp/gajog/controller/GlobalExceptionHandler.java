package com.utp.gajog.controller;

import com.utp.gajog.config.TimeConfig;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(TimeConfig.ZONA_LIMA).toString(),
                "estado", HttpStatus.NOT_FOUND.value(),
                "error", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler({ReglaNegocioException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> manejarErroresDeNegocio(RuntimeException ex) {
        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(TimeConfig.ZONA_LIMA).toString(),
                "estado", HttpStatus.BAD_REQUEST.value(),
                "error", ex.getMessage()
        );
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarErroresDeValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(TimeConfig.ZONA_LIMA).toString(),
                "estado", HttpStatus.BAD_REQUEST.value(),
                "error", detalle.isBlank() ? "Datos de entrada inválidos." : detalle
        );
        return ResponseEntity.badRequest().body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorTecnico(Exception ex) {
        log.error("Error técnico inesperado en API REST", ex);
        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(TimeConfig.ZONA_LIMA).toString(),
                "estado", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Ocurrió un error interno al procesar la solicitud. Intente nuevamente."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }
}