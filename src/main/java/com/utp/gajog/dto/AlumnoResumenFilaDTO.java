package com.utp.gajog.dto;

import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.EstadoAlumno;
import java.time.LocalDate;

public record AlumnoResumenFilaDTO(
        Long id,
        String nombres,
        String apellidos,
        String dniMostrado,
        boolean menorDeEdad,
        int edad,
        LocalDate fechaIngreso,
        Cinturon cinturonActual,
        EstadoAlumno estado,
        String categoriaActual,
        String horarioActual,
        Long horarioActualId,
        String estadoMensualidadEtiqueta,
        String estadoMensualidadVariante,
        boolean requiereCambioCategoria
) {
    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}