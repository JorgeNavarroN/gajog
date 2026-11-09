/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.dto;

import com.utp.gajog.enums.DiaSemana;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author JORGE
 */
public class GrupoForm {

    private String nombre;
    private Long categoriaId;
    private Long instructorId;
    private Integer cupoMaximo;

    private List<HorarioForm> horarios = new ArrayList<>();

    public GrupoForm() {
        horarios.add(new HorarioForm());
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public List<HorarioForm> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<HorarioForm> horarios) {
        this.horarios = horarios;
    }

    public static class HorarioForm {

        private DiaSemana diaSemana;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime horaInicio;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime horaFin;

        public DiaSemana getDiaSemana() {
            return diaSemana;
        }

        public void setDiaSemana(DiaSemana diaSemana) {
            this.diaSemana = diaSemana;
        }

        public LocalTime getHoraInicio() {
            return horaInicio;
        }

        public void setHoraInicio(LocalTime horaInicio) {
            this.horaInicio = horaInicio;
        }

        public LocalTime getHoraFin() {
            return horaFin;
        }

        public void setHoraFin(LocalTime horaFin) {
            this.horaFin = horaFin;
        }
    }
}
