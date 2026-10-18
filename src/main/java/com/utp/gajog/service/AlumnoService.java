package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final PersonaRepository personaRepository;
    private final CinturonRepository cinturonRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final HistorialCinturonesRepository historialCinturonesRepository;

    public AlumnoService(
            AlumnoRepository alumnoRepository,
            PersonaRepository personaRepository,
            CinturonRepository cinturonRepository,
            ApoderadoRepository apoderadoRepository,
            HistorialCinturonesRepository historialCinturonesRepository
    ) {
        this.alumnoRepository = alumnoRepository;
        this.personaRepository = personaRepository;
        this.cinturonRepository = cinturonRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.historialCinturonesRepository = historialCinturonesRepository;
    }

    @Transactional
    public Alumno registrarAlumno(Persona datosPersona, Long cinturonInicialId, Long apoderadoId) {
        if (alumnoRepository.findByPersonaDni(datosPersona.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un alumno registrado con el DNI: " + datosPersona.getDni());
        }

        Persona persona = personaRepository.findByDni(datosPersona.getDni())
                .orElseGet(() -> personaRepository.save(datosPersona));

        Cinturon cinturon = cinturonRepository.findById(cinturonInicialId)
                .orElseThrow(() -> new IllegalArgumentException("Cinturón no encontrado"));

        Alumno alumno = new Alumno();
        alumno.setPersona(persona);
        alumno.setCinturonActual(cinturon);
        alumno.setFechaIngreso(LocalDate.now());
        alumno.setEstado(EstadoAlumno.ACTIVO);

        if (apoderadoId != null) {
            Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                    .orElseThrow(() -> new IllegalArgumentException("Apoderado no encontrado"));
            alumno.setApoderado(apoderado);
        }

        Alumno alumnoGuardado = alumnoRepository.save(alumno);

        HistorialCinturones historial = new HistorialCinturones();
        historial.setAlumno(alumnoGuardado);
        historial.setCinturon(cinturon);
        historial.setFechaObtencion(LocalDate.now());
        historialCinturonesRepository.save(historial);

        return alumnoGuardado;
    }

    @Transactional(readOnly = true)
    public List<Alumno> listarTodos() {
        return alumnoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Alumno> listarPorEstado(EstadoAlumno estado) {
        return alumnoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public Alumno obtenerPorId(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado con ID: " + id));
    }

    @Transactional
    public Alumno cambiarEstado(Long alumnoId, EstadoAlumno nuevoEstado) {
        Alumno alumno = obtenerPorId(alumnoId);
        alumno.setEstado(nuevoEstado);
        return alumnoRepository.save(alumno);
    }
}