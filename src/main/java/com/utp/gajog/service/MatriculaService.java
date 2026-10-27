/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Categoria;
import com.utp.gajog.model.Grupo;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.repository.MatriculaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JORGE
 */
@Service
public class MatriculaService {

    private final AlumnoRepository alumnoRepository;
    private final GrupoRepository grupoRepository;
    private final MatriculaRepository matriculaRepository;
    private final CategoriaService categoriaService;

    public MatriculaService(
            AlumnoRepository alumnoRepository,
            GrupoRepository grupoRepository,
            MatriculaRepository matriculaRepository,
            CategoriaService categoriaService) {

        this.alumnoRepository = alumnoRepository;
        this.grupoRepository = grupoRepository;
        this.matriculaRepository = matriculaRepository;
        this.categoriaService = categoriaService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Matricula registrar(Long alumnoId, Long grupoId) {

        Grupo grupo = grupoRepository.findByIdConBloqueo(grupoId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Grupo no encontrado."));

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(()
                        -> new IllegalArgumentException("Alumno no encontrado."));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new IllegalStateException(
                    "El alumno debe estar activo para matricularse.");
        }

        if (!Boolean.TRUE.equals(grupo.getActivo())) {
            throw new IllegalStateException("El grupo no está activo.");
        }

        if (grupo.getHorarios().isEmpty()) {
            throw new IllegalStateException(
                    "El grupo no tiene horarios asignados.");
        }

        Categoria categoria = categoriaService
                .determinarPorFechaNacimiento(
                        alumno.getPersona().getFechaNacimiento());

        if (!categoria.getId().equals(grupo.getCategoria().getId())) {
            throw new IllegalArgumentException(
                    "El grupo no corresponde a la categoría del alumno.");
        }

        boolean yaMatriculado = matriculaRepository
                .existsByAlumno_IdAndGrupo_IdAndEstado(
                        alumnoId, grupoId, EstadoMatricula.ACTIVA);

        if (yaMatriculado) {
            throw new IllegalStateException(
                    "El alumno ya tiene una matrícula activa en este grupo.");
        }

        long ocupados = matriculaRepository.countByGrupo_IdAndEstado(
                grupoId, EstadoMatricula.ACTIVA);

        if (ocupados >= grupo.getCupoMaximo()) {
            throw new IllegalStateException(
                    "El grupo ya no tiene vacantes disponibles.");
        }

        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setGrupo(grupo);
        matricula.setFechaMatricula(LocalDate.now());
        matricula.setEstado(EstadoMatricula.ACTIVA);

        return matriculaRepository.save(matricula);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listar(
            String busqueda,
            Long categoriaId,
            Long grupoId,
            EstadoMatricula estado) {

        String texto = busqueda == null ? "" : busqueda.trim();

        return matriculaRepository.buscarParaListado(
                texto,
                categoriaId,
                grupoId,
                estado
        );
    }
}
