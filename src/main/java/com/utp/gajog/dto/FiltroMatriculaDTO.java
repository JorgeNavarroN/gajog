package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoMatricula;

public record FiltroMatriculaDTO(
        String tab,
        String q,
        Long categoriaId,
        Long horarioId
) {
    public String tabActiva() {
        return (tab == null || tab.isBlank()) ? "TODAS" : tab.toUpperCase();
    }

    public EstadoMatricula estadoFiltro() {
        return switch (tabActiva()) {
            case "PENDIENTES" -> EstadoMatricula.PENDIENTE;
            case "ACTIVAS" -> EstadoMatricula.ACTIVA;
            case "FINALIZADAS" -> EstadoMatricula.FINALIZADA;
            case "CANCELADAS" -> EstadoMatricula.CANCELADA;
            default -> null;
        };
    }
}