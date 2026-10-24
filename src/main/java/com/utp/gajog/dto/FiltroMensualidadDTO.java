package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.model.MedioPago;

public record FiltroMensualidadDTO(
        Integer anio,
        Integer mes,
        String q,
        Long categoriaId,
        Long horarioId,
        EstadoMensualidad estado,
        MedioPago medioPago,
        String tab
) {
    public String tabActiva() {
        return (tab == null || tab.isBlank()) ? "por-vencer" : tab;
    }
}