package com.utp.gajog.controller;

import com.utp.gajog.model.EstadoMatricula;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.service.MatriculaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    public record RegistroMatriculaRequest(
            Long alumnoId,
            Long categoriaId,
            Long horarioId
    ) {}

    @PostMapping
    public ResponseEntity<Matricula> matricular(@RequestBody RegistroMatriculaRequest request) {
        Matricula matricula = matriculaService.registrarMatricula(
                request.alumnoId(),
                request.categoriaId(),
                request.horarioId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(matricula);
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<Matricula>> listarPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(matriculaService.listarPorAlumno(alumnoId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Matricula> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoMatricula nuevoEstado
    ) {
        return ResponseEntity.ok(matriculaService.cambiarEstado(id, nuevoEstado));
    }
}