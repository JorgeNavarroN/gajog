package com.utp.gajog.dto;

import java.math.BigDecimal;

public record ResumenMensualidadDTO(
        BigDecimal totalRecaudado,
        long cantidadPagados,
        long cantidadPorVencer,
        long cantidadVencidas,
        int porcentajeCobranza,
        long cantidadEnRiesgoRetiro
) {}