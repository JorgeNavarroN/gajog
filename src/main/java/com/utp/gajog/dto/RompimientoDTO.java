package com.utp.gajog.dto;

import java.math.BigDecimal;
import java.util.List;

/** Estado de un competidor de rompimiento (formato JSON para el panel). */
public record RompimientoDTO(
        Long inscripcionId,
        String nombre,
        String categoria,
        List<IntentoDTO> intentos,
        BigDecimal total,
        int tablasRotas,
        int intentosRestantes,
        Integer puesto) {

    public record IntentoDTO(
            int numero,
            String tecnica,
            int tablasIntentadas,
            int tablasRotas,
            boolean limpio,
            BigDecimal puntos) {
    }
}
