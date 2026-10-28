/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.SesionClaseForm;
import com.utp.gajog.enums.EstadoSesionClase;
import com.utp.gajog.model.Horario;
import com.utp.gajog.model.Instructor;
import com.utp.gajog.model.SesionClase;
import com.utp.gajog.repository.HorarioRepository;
import com.utp.gajog.repository.SesionClaseRepository;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JOAQUIN
 */
@Service
public class SesionClaseService {

    private final HorarioRepository horarioRepository;
    private final SesionClaseRepository sesionClaseRepository;

    public SesionClaseService(
            HorarioRepository horarioRepository,
            SesionClaseRepository sesionClaseRepository) {

        this.horarioRepository = horarioRepository;
        this.sesionClaseRepository = sesionClaseRepository;
    }

    @Transactional
    public Long abrirSesion(SesionClaseForm formulario) {

        validarFormulario(formulario);

        Horario horario = horarioRepository
                .findByIdConBloqueo(formulario.getHorarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El horario seleccionado no existe."));

        if (!Boolean.TRUE.equals(horario.getGrupo().getActivo())) {
            throw new IllegalStateException(
                    "El grupo seleccionado está inactivo.");
        }

        if (horario.getDiaSemana() == null
                || horario.getHoraInicio() == null
                || horario.getHoraFin() == null
                || !horario.getHoraFin().isAfter(horario.getHoraInicio())) {

            throw new IllegalStateException(
                    "El horario tiene datos incompletos o inválidos.");
        }

        DayOfWeek diaEsperado = convertirDia(
                horario.getDiaSemana().name());

        if (formulario.getFecha().getDayOfWeek() != diaEsperado) {
            throw new IllegalArgumentException(
                    "La fecha no corresponde al día del horario: "
                    + horario.getDiaSemana());
        }

        List<SesionClase> existentes = sesionClaseRepository
                .findByHorario_IdAndFecha(
                        horario.getId(),
                        formulario.getFecha());

        if (existentes.size() > 1) {
            throw new IllegalStateException(
                    "Existen sesiones duplicadas para ese horario y fecha.");
        }

        if (!existentes.isEmpty()) {
            SesionClase existente = existentes.get(0);

            if (existente.getEstado() == EstadoSesionClase.CANCELADA) {
                throw new IllegalStateException(
                        "La sesión de ese horario y fecha está cancelada.");
            }

            // Abre la sesión existente sin crear otra.
            return existente.getId();
        }

        Instructor instructor = horario.getGrupo().getInstructor();

        if (instructor == null) {
            throw new IllegalStateException(
                    "El grupo no tiene un instructor asignado.");
        }

        SesionClase sesion = new SesionClase();
        sesion.setHorario(horario);
        sesion.setInstructor(instructor);
        sesion.setFecha(formulario.getFecha());
        sesion.setHoraInicio(horario.getHoraInicio());
        sesion.setHoraFin(horario.getHoraFin());
        sesion.setEstado(EstadoSesionClase.PROGRAMADA);

        return sesionClaseRepository.save(sesion).getId();
    }

    private void validarFormulario(SesionClaseForm formulario) {
        if (formulario == null
                || formulario.getHorarioId() == null
                || formulario.getFecha() == null) {

            throw new IllegalArgumentException(
                    "Selecciona horario y fecha.");
        }
    }

    private DayOfWeek convertirDia(String dia) {
        return switch (dia) {
            case "LUNES" ->
                DayOfWeek.MONDAY;
            case "MARTES" ->
                DayOfWeek.TUESDAY;
            case "MIERCOLES" ->
                DayOfWeek.WEDNESDAY;
            case "JUEVES" ->
                DayOfWeek.THURSDAY;
            case "VIERNES" ->
                DayOfWeek.FRIDAY;
            case "SABADO" ->
                DayOfWeek.SATURDAY;
            case "DOMINGO" ->
                DayOfWeek.SUNDAY;
            default ->
                throw new IllegalStateException(
                        "El día del horario no es válido.");
        };
    }
}
