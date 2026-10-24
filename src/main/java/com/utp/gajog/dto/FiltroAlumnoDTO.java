package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAlumno;
import com.utp.gajog.model.EstadoMensualidad;

public record FiltroAlumnoDTO(
        String q,
        Long categoriaId,
        Long horarioId,
        EstadoAlumno estado,
        Long cinturonId,
        EstadoMensualidad estadoMensualidad,
        String sortBy,
        String sortDir
) {
    public String campoOrden() {
        return (sortBy == null || sortBy.isBlank()) ? "apellidos" : sortBy;
    }

    public String direccionOrden() {
        return "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
    }
}