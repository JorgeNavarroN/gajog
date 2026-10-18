package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class AsistenciaService {

    private final SesionClaseRepository sesionClaseRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final HorarioRepository horarioRepository;
    private final InstructorRepository instructorRepository;
    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;

    public AsistenciaService(
            SesionClaseRepository sesionClaseRepository,
            AsistenciaRepository asistenciaRepository,
            HorarioRepository horarioRepository,
            InstructorRepository instructorRepository,
            AlumnoRepository alumnoRepository,
            MatriculaRepository matriculaRepository
    ) {
        this.sesionClaseRepository = sesionClaseRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.horarioRepository = horarioRepository;
        this.instructorRepository = instructorRepository;
        this.alumnoRepository = alumnoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional
    public SesionClase programarSesion(Long horarioId, Long instructorId, LocalDate fecha) {
        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor no encontrado"));

        SesionClase sesion = new SesionClase();
        sesion.setHorario(horario);
        sesion.setInstructor(instructor);
        sesion.setFecha(fecha);
        sesion.setHoraInicio(horario.getHoraInicio());
        sesion.setHoraFin(horario.getHoraFin());
        sesion.setEstado(EstadoSesionClase.PROGRAMADA);

        return sesionClaseRepository.save(sesion);
    }

    @Transactional
    public Asistencia registrarAsistencia(Long sesionId, Long alumnoId, EstadoAsistencia estadoAsistencia) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión de clase no encontrada"));

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new IllegalStateException("No se puede registrar asistencia en una sesión cancelada");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        Asistencia asistencia = asistenciaRepository.findBySesionIdAndAlumnoId(sesionId, alumnoId)
                .orElseGet(() -> {
                    Asistencia nueva = new Asistencia();
                    nueva.setSesion(sesion);
                    nueva.setAlumno(alumno);
                    return nueva;
                });

        asistencia.setEstado(estadoAsistencia);
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public void registrarAsistenciaMasiva(Long sesionId, Set<Long> alumnosPresentesIds) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));

        List<Matricula> matriculados = matriculaRepository.findByHorarioIdAndEstado(
                sesion.getHorario().getId(), EstadoMatricula.ACTIVA
        );

        for (Matricula m : matriculados) {
            Long alumnoId = m.getAlumno().getId();
            EstadoAsistencia estado = (alumnosPresentesIds != null && alumnosPresentesIds.contains(alumnoId))
                    ? EstadoAsistencia.ASISTIO
                    : EstadoAsistencia.NO_ASISTIO;
            registrarAsistencia(sesionId, alumnoId, estado);
        }

        sesion.setEstado(EstadoSesionClase.FINALIZADA);
        sesionClaseRepository.save(sesion);
    }

    @Transactional
    public SesionClase cambiarEstadoSesion(Long sesionId, EstadoSesionClase nuevoEstado) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión de clase no encontrada"));
        sesion.setEstado(nuevoEstado);
        return sesionClaseRepository.save(sesion);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> listarAsistenciasPorSesion(Long sesionId) {
        return asistenciaRepository.findBySesionId(sesionId);
    }
}