/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.dto;

import com.utp.gajog.enums.EstadoAsistencia;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JOAQUIN
 */
public class AsistenciaForm {

    private List<Detalle> detalles = new ArrayList<>();

    public List<Detalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<Detalle> detalles) {
        this.detalles = detalles;
    }

    public static class Detalle {

        private Long alumnoId;
        private EstadoAsistencia estado;
        private String nombreAlumno;
        private String dniAlumno;

        public Long getAlumnoId() {
            return alumnoId;
        }

        public void setAlumnoId(Long alumnoId) {
            this.alumnoId = alumnoId;
        }

        public EstadoAsistencia getEstado() {
            return estado;
        }

        public void setEstado(EstadoAsistencia estado) {
            this.estado = estado;
        }

        public String getNombreAlumno() {
            return nombreAlumno;
        }

        public void setNombreAlumno(String nombreAlumno) {
            this.nombreAlumno = nombreAlumno;
        }

        public String getDniAlumno() {
            return dniAlumno;
        }

        public void setDniAlumno(String dniAlumno) {
            this.dniAlumno = dniAlumno;
        }
    }
}
