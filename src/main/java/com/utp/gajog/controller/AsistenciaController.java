package com.utp.gajog.controller;

import com.utp.gajog.model.Asistencia;
import com.utp.gajog.model.EstadoAsistencia;
import com.utp.gajog.model.EstadoSesionClase;
import com.utp.gajog.model.SesionClase;
import com.utp.gajog.service.AsistenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    public record ProgramarSesionRequest(
            Long horarioId,
            Long instructorId,
            LocalDate fecha
    ) {}

    public record RegistrarAsistenciaRequest(
            Long sesionId,
            Long alumnoId,
            EstadoAsistencia estado
    ) {}

    @PostMapping("/sesiones")
    public ResponseEntity<SesionClase> programarSesion(@RequestBody ProgramarSesionRequest request) {
        SesionClase sesion = asistenciaService.programarSesion(
                request.horarioId(),
                request.instructorId(),
                request.fecha()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(sesion);
    }

    @PostMapping("/marcar")
    public ResponseEntity<Asistencia> registrarAsistencia(@RequestBody RegistrarAsistenciaRequest request) {
        Asistencia asistencia = asistenciaService.registrarAsistencia(
                request.sesionId(),
                request.alumnoId(),
                request.estado()
        );
        return ResponseEntity.ok(asistencia);
    }

    @PatchMapping("/sesiones/{sesionId}/estado")
    public ResponseEntity<SesionClase> cambiarEstadoSesion(
            @PathVariable Long sesionId,
            @RequestParam EstadoSesionClase nuevoEstado
    ) {
        return ResponseEntity.ok(asistenciaService.cambiarEstadoSesion(sesionId, nuevoEstado));
    }

    @GetMapping("/sesiones/{sesionId}")
    public ResponseEntity<List<Asistencia>> listarPorSesion(@PathVariable Long sesionId) {
        return ResponseEntity.ok(asistenciaService.listarAsistenciasPorSesion(sesionId));
    }
}