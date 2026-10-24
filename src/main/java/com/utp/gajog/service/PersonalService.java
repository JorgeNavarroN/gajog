package com.utp.gajog.service;

import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonalService {

    private final PersonaRepository personaRepository;
    private final InstructorRepository instructorRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AlumnoRepository alumnoRepository;
    private final ObservacionProgresoRepository observacionRepository;
    private final Clock clock;

    public PersonalService(
            PersonaRepository personaRepository,
            InstructorRepository instructorRepository,
            ApoderadoRepository apoderadoRepository,
            AlumnoRepository alumnoRepository,
            ObservacionProgresoRepository observacionRepository,
            Clock clock
    ) {
        this.personaRepository = personaRepository;
        this.instructorRepository = instructorRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.alumnoRepository = alumnoRepository;
        this.observacionRepository = observacionRepository;
        this.clock = clock;
    }

    @Transactional
    public Instructor registrarInstructor(Persona datosPersona) {
        String dni = datosPersona.getDni().trim();
        if (instructorRepository.findByPersonaDni(dni).isPresent()) {
            throw new ReglaNegocioException("Ya existe un instructor con el DNI: " + dni);
        }
        Persona persona = personaRepository.findByDni(dni)
                .orElseGet(() -> {
                    datosPersona.setDni(dni);
                    return personaRepository.save(datosPersona);
                });

        Instructor instructor = new Instructor();
        instructor.setPersona(persona);
        return instructorRepository.save(instructor);
    }

    @Transactional
    public Apoderado registrarApoderado(Persona datosPersona) {
        String dni = datosPersona.getDni().trim();
        if (apoderadoRepository.findByPersonaDni(dni).isPresent()) {
            throw new ReglaNegocioException("Ya existe un apoderado con el DNI: " + dni);
        }
        Persona persona = personaRepository.findByDni(dni)
                .orElseGet(() -> {
                    datosPersona.setDni(dni);
                    return personaRepository.save(datosPersona);
                });

        Apoderado apoderado = new Apoderado();
        apoderado.setPersona(persona);
        return apoderadoRepository.save(apoderado);
    }

    @Transactional
    public ObservacionProgreso registrarObservacion(Long alumnoId, Long instructorId, String textoObservacion) {
        Alumno alumno = alumnoRepository.findByIdConDetalle(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Instructor no encontrado"));

        ObservacionProgreso obs = new ObservacionProgreso();
        obs.setAlumno(alumno);
        obs.setInstructor(instructor);
        obs.setFecha(LocalDate.now(clock));
        obs.setObservacion(textoObservacion.trim());

        return observacionRepository.save(obs);
    }

    @Transactional(readOnly = true)
    public List<Instructor> listarInstructores() {
        return instructorRepository.findAllConPersona();
    }

    @Transactional(readOnly = true)
    public List<Apoderado> listarApoderados() {
        return apoderadoRepository.findAllConPersona();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Alumno>> obtenerMapaHijosPorApoderado() {
        List<Apoderado> apoderados = apoderadoRepository.findAllConPersona();
        Map<Long, List<Alumno>> mapa = new HashMap<>();
        for (Apoderado ap : apoderados) {
            mapa.put(ap.getId(), new ArrayList<>());
        }
        for (Alumno al : alumnoRepository.findAllConApoderadoYPersona()) {
            if (al.getApoderado() != null) {
                mapa.computeIfAbsent(al.getApoderado().getId(), k -> new ArrayList<>()).add(al);
            }
        }
        return mapa;
    }
}