package com.utp.gajog.controller;

import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.Persona;
import com.utp.gajog.service.AlumnoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    public record RegistroAlumnoRequest(
            String dni,
            String nombres,
            String apellidos,
            String correo,
            String telefono,
            LocalDate fechaNacimiento,
            Long cinturonInicialId,
            Long apoderadoId
    ) {}

    @PostMapping
    public ResponseEntity<Alumno> registrar(@RequestBody RegistroAlumnoRequest request) {
        Persona persona = new Persona();
        persona.setDni(request.dni());
        persona.setNombres(request.nombres());
        persona.setApellidos(request.apellidos());
        persona.setCorreo(request.correo());
        persona.setTelefono(request.telefono());
        persona.setFechaNacimiento(request.fechaNacimiento());

        Alumno nuevoAlumno = alumnoService.registrarAlumno(
                persona,
                request.cinturonInicialId(),
                request.apoderadoId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoAlumno);
    }

    @GetMapping
    public ResponseEntity<List<Alumno>> listarTodos(
            @RequestParam(required = false) EstadoAlumno estado
    ) {
        if (estado != null) {
            return ResponseEntity.ok(alumnoService.listarPorEstado(estado));
        }
        return ResponseEntity.ok(alumnoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alumno> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Alumno> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoAlumno nuevoEstado
    ) {
        return ResponseEntity.ok(alumnoService.cambiarEstado(id, nuevoEstado));
    }
}