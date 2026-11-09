/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.GrupoDisponibleDTO;
import com.utp.gajog.dto.GrupoListadoDTO;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.model.Grupo;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.repository.MatriculaRepository;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JORGE
 */
@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final MatriculaRepository matriculaRepository;

    public GrupoService(
            GrupoRepository grupoRepository,
            MatriculaRepository matriculaRepository) {
        this.grupoRepository = grupoRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional(readOnly = true)
    public List<Grupo> listarDisponibles(Long categoriaId) {
        return grupoRepository
                .findByCategoria_IdAndActivoTrue(categoriaId)
                .stream()
                .filter(grupo -> !grupo.getHorarios().isEmpty())
                .filter(this::tieneVacantes)
                .toList();
    }

    private boolean tieneVacantes(Grupo grupo) {
        long ocupados = matriculaRepository.countByGrupo_IdAndEstado(
                grupo.getId(),
                EstadoMatricula.ACTIVA
        );
        return ocupados < grupo.getCupoMaximo();
    }

    @Transactional(readOnly = true)
    public List<GrupoDisponibleDTO> listarOpciones(Long categoriaId) {

        return grupoRepository
                .findByCategoria_IdAndActivoTrue(categoriaId)
                .stream()
                .filter(grupo -> !grupo.getHorarios().isEmpty())
                .map(grupo -> {

                    long ocupados = matriculaRepository
                            .countByGrupo_IdAndEstado(
                                    grupo.getId(),
                                    EstadoMatricula.ACTIVA
                            );

                    long vacantes = Math.max(
                            0L,
                            grupo.getCupoMaximo() - ocupados
                    );

                    List<GrupoDisponibleDTO.HorarioDTO> horarios
                            = grupo.getHorarios()
                                    .stream()
                                    .map(horario
                                            -> new GrupoDisponibleDTO.HorarioDTO(
                                                    horario.getDiaSemana().name(),
                                                    horario.getHoraInicio(),
                                                    horario.getHoraFin()
                                            ))
                                    .toList();

                    return new GrupoDisponibleDTO(
                            grupo.getId(),
                            grupo.getNombre(),
                            vacantes,
                            horarios
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrupoListadoDTO> listarParaGestion() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");

        return grupoRepository.listarParaGestion().stream()
                .map(grupo -> {
                    long ocupados = matriculaRepository
                            .countByGrupo_IdAndEstado(
                                    grupo.getId(),
                                    EstadoMatricula.ACTIVA);

                    String instructor = "Sin asignar";

                    if (grupo.getInstructor() != null) {
                        var persona = grupo.getInstructor().getPersona();

                        instructor = persona.getNombres()
                                + " " + persona.getApellidos();
                    }

                    List<String> horarios = grupo.getHorarios().stream()
                            .sorted(Comparator
                                    .comparingInt(
                                            (com.utp.gajog.model.Horario h)
                                            -> h.getDiaSemana().ordinal())
                                    .thenComparing(
                                            com.utp.gajog.model.Horario::getHoraInicio))
                            .map(h -> h.getDiaSemana()
                                    + " · "
                                    + h.getHoraInicio().format(formato)
                                    + "–"
                                    + h.getHoraFin().format(formato))
                            .toList();

                    return new GrupoListadoDTO(
                            grupo.getId(),
                            grupo.getNombre(),
                            grupo.getCategoria().getNombre(),
                            instructor,
                            grupo.getCupoMaximo(),
                            ocupados,
                            Math.max(0L, grupo.getCupoMaximo() - ocupados),
                            Boolean.TRUE.equals(grupo.getActivo()),
                            horarios);
                })
                .toList();
    }
}
