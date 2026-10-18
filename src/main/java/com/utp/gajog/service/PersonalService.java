package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PersonalService {

    private final PersonaRepository personaRepository;
    private final InstructorRepository instructorRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final AlumnoRepository alumnoRepository;
    private final ObservacionProgresoRepository observacionRepository;

    public PersonalService(
            PersonaRepository personaRepository,
            InstructorRepository instructorRepository,
            ApoderadoRepository apoderadoRepository,
            AlumnoRepository alumnoRepository,
            ObservacionProgresoRepository observacionRepository
    ) {
        this.personaRepository = personaRepository;
        this.instructorRepository = instructorRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.alumnoRepository = alumnoRepository;
        this.observacionRepository = observacionRepository;
    }

    @Transactional
    public Instructor registrarInstructor(Persona datosPersona) {
        if (instructorRepository.findByPersonaDni(datosPersona.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un instructor con el DNI: " + datosPersona.getDni());
        }
        Persona persona = personaRepository.findByDni(datosPersona.getDni())
                .orElseGet(() -> personaRepository.save(datosPersona));

        Instructor instructor = new Instructor();
        instructor.setPersona(persona);
        return instructorRepository.save(instructor);
    }

    @Transactional
    public Apoderado registrarApoderado(Persona datosPersona) {
        if (apoderadoRepository.findByPersonaDni(datosPersona.getDni()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un apoderado con el DNI: " + datosPersona.getDni());
        }
        Persona persona = personaRepository.findByDni(datosPersona.getDni())
                .orElseGet(() -> personaRepository.save(datosPersona));

        Apoderado apoderado = new Apoderado();
        apoderado.setPersona(persona);
        return apoderadoRepository.save(apoderado);
    }

    @Transactional
    public ObservacionProgreso registrarObservacion(Long alumnoId, Long instructorId, String textoObservacion) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor no encontrado"));

        ObservacionProgreso obs = new ObservacionProgreso();
        obs.setAlumno(alumno);
        obs.setInstructor(instructor);
        obs.setFecha(LocalDate.now());
        obs.setObservacion(textoObservacion);

        return observacionRepository.save(obs);
    }

    @Transactional(readOnly = true)
    public List<Instructor> listarInstructores() {
        return instructorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Apoderado> listarApoderados() {
        return apoderadoRepository.findAll();
    }
}