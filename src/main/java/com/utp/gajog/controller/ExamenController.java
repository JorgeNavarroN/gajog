package com.utp.gajog.controller;

import com.utp.gajog.model.DetalleExamen;
import com.utp.gajog.model.Examen;
import com.utp.gajog.service.ExamenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/examenes")
public class ExamenController {

    private final ExamenService examenService;

    public ExamenController(ExamenService examenService) {
        this.examenService = examenService;
    }

    public record ProgramarExamenRequest(
            Long alumnoId,
            Long cinturonObjetivoId,
            LocalDate fechaExamen
    ) {}

    public record CalificarCriterioRequest(
            Long criterioId,
            BigDecimal puntaje,
            String observacion
    ) {}

    @PostMapping
    public ResponseEntity<Examen> programarExamen(@RequestBody ProgramarExamenRequest request) {
        Examen examen = examenService.programarExamen(
                request.alumnoId(),
                request.cinturonObjetivoId(),
                request.fechaExamen()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(examen);
    }

    @PostMapping("/{examenId}/calificar")
    public ResponseEntity<DetalleExamen> calificarCriterio(
            @PathVariable Long examenId,
            @RequestBody CalificarCriterioRequest request
    ) {
        DetalleExamen detalle = examenService.calificarCriterio(
                examenId,
                request.criterioId(),
                request.puntaje(),
                request.observacion()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @PostMapping("/{examenId}/finalizar")
    public ResponseEntity<Examen> finalizarEvaluacion(
            @PathVariable Long examenId,
            @RequestParam(defaultValue = "70.00") BigDecimal puntajeMinimo
    ) {
        return ResponseEntity.ok(examenService.finalizarEvaluacion(examenId, puntajeMinimo));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<Examen>> listarPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(examenService.listarPorAlumno(alumnoId));
    }
}