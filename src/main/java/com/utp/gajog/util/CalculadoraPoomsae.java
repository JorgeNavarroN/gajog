package com.utp.gajog.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calcula la nota final de poomsae: cada juez da exactitud (0-4) y
 * presentación (0-6). Se descarta la nota total más alta y la más baja
 * y se promedia el resto.
 */
public final class CalculadoraPoomsae {

    public static final BigDecimal MAX_EXACTITUD = new BigDecimal("4.0");
    public static final BigDecimal MAX_PRESENTACION = new BigDecimal("6.0");

    private CalculadoraPoomsae() {
    }

    /** Valida los rangos de una calificación individual. */
    public static void validar(BigDecimal exactitud, BigDecimal presentacion) {
        if (exactitud == null || presentacion == null) {
            throw new IllegalArgumentException("Debe indicar exactitud y presentación.");
        }
        if (exactitud.signum() < 0 || exactitud.compareTo(MAX_EXACTITUD) > 0) {
            throw new IllegalArgumentException("La exactitud debe estar entre 0 y 4.0.");
        }
        if (presentacion.signum() < 0 || presentacion.compareTo(MAX_PRESENTACION) > 0) {
            throw new IllegalArgumentException("La presentación debe estar entre 0 y 6.0.");
        }
    }

    /** Nota total de un juez = exactitud + presentación. */
    public static BigDecimal totalJuez(BigDecimal exactitud, BigDecimal presentacion) {
        validar(exactitud, presentacion);
        return exactitud.add(presentacion);
    }

    /**
     * Promedio sin la nota más alta ni la más baja.
     * Se necesitan al menos 3 jueces para poder descartar los extremos.
     */
    public static BigDecimal notaFinal(List<BigDecimal> totales) {
        if (totales == null || totales.size() < 3) {
            throw new IllegalArgumentException("Se necesitan al menos 3 jueces.");
        }
        List<BigDecimal> ordenadas = new ArrayList<>(totales);
        Collections.sort(ordenadas);
        List<BigDecimal> centrales = ordenadas.subList(1, ordenadas.size() - 1);
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal nota : centrales) {
            suma = suma.add(nota);
        }
        return suma.divide(BigDecimal.valueOf(centrales.size()), 2, RoundingMode.HALF_UP);
    }
}
