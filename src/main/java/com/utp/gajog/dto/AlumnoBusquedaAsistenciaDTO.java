package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAlumno;

public record AlumnoBusquedaAsistenciaDTO(
        Long id,
        String dni,
        String dniMostrado,
        String nombreCompleto,
        String categoriaActual,
        String horarioActual,
        String cinturonActual,
        EstadoAlumno estadoAlumno,
        String estadoMensualidad
) {}