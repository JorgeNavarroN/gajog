/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.GrupoForm;
import com.utp.gajog.model.Categoria;
import com.utp.gajog.model.Grupo;
import com.utp.gajog.model.Horario;
import com.utp.gajog.model.Instructor;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.repository.HorarioRepository;
import com.utp.gajog.repository.InstructorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JORGE
 */
@Service
public class GestionGrupoService {

    private final GrupoRepository grupoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InstructorRepository instructorRepository;
    private final HorarioRepository horarioRepository;

    public GestionGrupoService(
            GrupoRepository grupoRepository,
            CategoriaRepository categoriaRepository,
            InstructorRepository instructorRepository,
            HorarioRepository horarioRepository) {

        this.grupoRepository = grupoRepository;
        this.categoriaRepository = categoriaRepository;
        this.instructorRepository = instructorRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long crear(GrupoForm formulario) {

        validarFormulario(formulario);

        String nombre = formulario.getNombre().trim();

        Instructor instructor = instructorRepository
                .findByIdConBloqueo(formulario.getInstructorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El instructor seleccionado no existe."));

        Categoria categoria = categoriaRepository
                .findById(formulario.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría seleccionada no existe."));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalArgumentException(
                    "La categoría seleccionada está inactiva.");
        }

        if (grupoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException(
                    "Ya existe un grupo con ese nombre.");
        }

        for (GrupoForm.HorarioForm horario : formulario.getHorarios()) {

            long cruces = horarioRepository.contarCrucesInstructor(
                    instructor.getId(),
                    horario.getDiaSemana(),
                    horario.getHoraInicio(),
                    horario.getHoraFin());

            if (cruces > 0) {
                throw new IllegalArgumentException(
                        "El instructor ya tiene una clase el "
                        + horario.getDiaSemana()
                        + " dentro del horario "
                        + horario.getHoraInicio()
                        + "–" + horario.getHoraFin() + ".");
            }
        }

        Grupo grupo = new Grupo();
        grupo.setNombre(nombre);
        grupo.setCategoria(categoria);
        grupo.setInstructor(instructor);
        grupo.setCupoMaximo(formulario.getCupoMaximo());
        grupo.setActivo(true);

        grupoRepository.save(grupo);

        for (GrupoForm.HorarioForm detalle : formulario.getHorarios()) {

            Horario horario = new Horario();
            horario.setGrupo(grupo);
            horario.setDiaSemana(detalle.getDiaSemana());
            horario.setHoraInicio(detalle.getHoraInicio());
            horario.setHoraFin(detalle.getHoraFin());

            horarioRepository.save(horario);
            grupo.getHorarios().add(horario);
        }

        return grupo.getId();
    }

    private void validarFormulario(GrupoForm formulario) {

        if (formulario == null) {
            throw new IllegalArgumentException(
                    "Completa los datos del grupo.");
        }

        String nombre = formulario.getNombre() == null
                ? "" : formulario.getNombre().trim();

        if (nombre.isEmpty() || nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre debe tener entre 1 y 100 caracteres.");
        }

        if (formulario.getCategoriaId() == null
                || formulario.getInstructorId() == null) {
            throw new IllegalArgumentException(
                    "Selecciona categoría e instructor.");
        }

        if (formulario.getCupoMaximo() == null
                || formulario.getCupoMaximo() <= 0) {
            throw new IllegalArgumentException(
                    "El cupo máximo debe ser mayor que cero.");
        }

        List<GrupoForm.HorarioForm> horarios = formulario.getHorarios();

        if (horarios == null || horarios.isEmpty() || horarios.size() > 21) {
            throw new IllegalArgumentException(
                    "Agrega entre 1 y 21 horarios.");
        }

        for (GrupoForm.HorarioForm horario : horarios) {

            if (horario == null
                    || horario.getDiaSemana() == null
                    || horario.getHoraInicio() == null
                    || horario.getHoraFin() == null) {
                throw new IllegalArgumentException(
                        "Completa el día y las horas de cada horario.");
            }

            if (!horario.getHoraFin().isAfter(horario.getHoraInicio())) {
                throw new IllegalArgumentException(
                        "La hora final debe ser posterior a la inicial.");
            }

            if (horario.getHoraInicio().getSecond() != 0
                    || horario.getHoraInicio().getNano() != 0
                    || horario.getHoraFin().getSecond() != 0
                    || horario.getHoraFin().getNano() != 0) {
                throw new IllegalArgumentException(
                        "Los horarios deben expresarse en horas y minutos.");
            }
        }

        // Comprueba cruces entre las filas del propio formulario.
        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {

                var primero = horarios.get(i);
                var segundo = horarios.get(j);

                boolean mismoDia = primero.getDiaSemana()
                        == segundo.getDiaSemana();

                boolean seCruzan = primero.getHoraInicio()
                        .isBefore(segundo.getHoraFin())
                        && primero.getHoraFin()
                                .isAfter(segundo.getHoraInicio());

                if (mismoDia && seCruzan) {
                    throw new IllegalArgumentException(
                            "Hay horarios superpuestos dentro del grupo.");
                }
            }
        }
    }
}
