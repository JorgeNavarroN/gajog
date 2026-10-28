/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.AsistenciaForm;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.enums.EstadoSesionClase;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Asistencia;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.model.SesionClase;
import com.utp.gajog.repository.AsistenciaRepository;
import com.utp.gajog.repository.MatriculaRepository;
import com.utp.gajog.repository.SesionClaseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JOAQUIN
 */
@Service
public class AsistenciaService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final SesionClaseRepository sesionClaseRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaRepository asistenciaRepository;

    public AsistenciaService(
            SesionClaseRepository sesionClaseRepository,
            MatriculaRepository matriculaRepository,
            AsistenciaRepository asistenciaRepository) {

        this.sesionClaseRepository = sesionClaseRepository;
        this.matriculaRepository = matriculaRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    @Transactional
    public void guardar(Long sesionId, AsistenciaForm formulario) {

        if (sesionId == null) {
            throw new IllegalArgumentException(
                    "Selecciona una sesión.");
        }

        SesionClase sesion = sesionClaseRepository
                .findByIdConBloqueo(sesionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La sesión no existe."));

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new IllegalStateException(
                    "No se puede registrar asistencia en una sesión cancelada.");
        }

        if (sesion.getFecha().isAfter(LocalDate.now(ZONA))) {
            throw new IllegalArgumentException(
                    "No se puede registrar asistencia de una fecha futura.");
        }

        if (formulario == null
                || formulario.getDetalles() == null
                || formulario.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "La lista de asistencia está vacía.");
        }

        Long grupoId = sesion.getHorario().getGrupo().getId();

        List<Matricula> matriculas = matriculaRepository
                .findByGrupo_IdAndEstado(
                        grupoId,
                        EstadoMatricula.ACTIVA);

        Map<Long, Alumno> alumnosPermitidos = new HashMap<>();

        for (Matricula matricula : matriculas) {
            // Excluye alumnos matriculados después de esta clase.
            if (!matricula.getFechaMatricula().isAfter(sesion.getFecha())) {
                Alumno alumno = matricula.getAlumno();
                alumnosPermitidos.put(alumno.getId(), alumno);
            }
        }

        if (alumnosPermitidos.isEmpty()) {
            throw new IllegalStateException(
                    "No hay alumnos habilitados para esta sesión.");
        }

        Set<Long> alumnosRecibidos = new HashSet<>();

        // Primero validamos toda la lista.
        for (AsistenciaForm.Detalle detalle : formulario.getDetalles()) {

            if (detalle == null
                    || detalle.getAlumnoId() == null
                    || detalle.getEstado() == null) {

                throw new IllegalArgumentException(
                        "Marca Presente o Ausente para cada alumno.");
            }

            Long alumnoId = detalle.getAlumnoId();

            if (!alumnosRecibidos.add(alumnoId)) {
                throw new IllegalArgumentException(
                        "La lista contiene alumnos duplicados.");
            }

            if (!alumnosPermitidos.containsKey(alumnoId)) {
                throw new IllegalArgumentException(
                        "La lista contiene un alumno que no está habilitado "
                        + "para esta sesión.");
            }
        }

        if (!alumnosRecibidos.equals(alumnosPermitidos.keySet())) {
            throw new IllegalArgumentException(
                    "La lista de alumnos cambió. "
                    + "Recarga la sesión y completa la asistencia.");
        }

        Map<Long, Asistencia> existentes = new HashMap<>();

        for (Asistencia asistencia
                : asistenciaRepository.findBySesion_Id(sesionId)) {

            existentes.put(
                    asistencia.getAlumno().getId(),
                    asistencia);
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA);

        for (AsistenciaForm.Detalle detalle : formulario.getDetalles()) {

            Long alumnoId = detalle.getAlumnoId();
            Asistencia asistencia = existentes.get(alumnoId);

            if (asistencia == null) {
                asistencia = new Asistencia();
                asistencia.setSesion(sesion);
                asistencia.setAlumno(alumnosPermitidos.get(alumnoId));
            }

            asistencia.setEstado(detalle.getEstado());
            asistencia.setFechaRegistro(ahora);

            asistenciaRepository.save(asistencia);
        }
    }

    @Transactional(readOnly = true)
    public AsistenciaForm prepararFormulario(Long sesionId) {

        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La sesión no existe."));

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new IllegalStateException(
                    "La sesión está cancelada.");
        }

        Long grupoId = sesion.getHorario().getGrupo().getId();

        List<Matricula> matriculas = matriculaRepository
                .findByGrupo_IdAndEstado(grupoId, EstadoMatricula.ACTIVA);

        Map<Long, Asistencia> existentes = new HashMap<>();

        for (Asistencia asistencia
                : asistenciaRepository.findBySesion_Id(sesionId)) {

            existentes.put(asistencia.getAlumno().getId(), asistencia);
        }

        AsistenciaForm formulario = new AsistenciaForm();
        Set<Long> incluidos = new HashSet<>();

        for (Matricula matricula : matriculas) {

            if (matricula.getFechaMatricula().isAfter(sesion.getFecha())) {
                continue;
            }

            Alumno alumno = matricula.getAlumno();

            if (!incluidos.add(alumno.getId())) {
                continue;
            }

            AsistenciaForm.Detalle detalle = new AsistenciaForm.Detalle();
            detalle.setAlumnoId(alumno.getId());
            detalle.setNombreAlumno(
                    alumno.getPersona().getApellidos()
                    + ", " + alumno.getPersona().getNombres());
            detalle.setDniAlumno(alumno.getPersona().getDni());

            Asistencia existente = existentes.get(alumno.getId());

            if (existente != null) {
                detalle.setEstado(existente.getEstado());
            }

            formulario.getDetalles().add(detalle);
        }

        formulario.getDetalles().sort(
                java.util.Comparator.comparing(
                        AsistenciaForm.Detalle::getNombreAlumno));

        return formulario;
    }
}
