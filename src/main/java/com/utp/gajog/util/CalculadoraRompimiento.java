package com.utp.gajog.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Puntaje de rompimiento de tablas.
 * Puntos = tablas rotas x coeficiente de la técnica.
 * Bonus de rompimiento limpio: si rompe TODAS las tablas intentadas,
 * suma 0.5 por cada tabla intentada.
 */
public final class CalculadoraRompimiento {

    public static final int MAX_TABLAS = 10;
    public static final int MAX_INTENTOS = 3;
    private static final BigDecimal BONUS_POR_TABLA = new BigDecimal("0.5");

    private CalculadoraRompimiento() {
    }

    public static void validar(int tablasIntentadas, int tablasRotas) {
        if (tablasIntentadas < 1 || tablasIntentadas > MAX_TABLAS) {
            throw new IllegalArgumentException(
                    "Las tablas intentadas deben estar entre 1 y " + MAX_TABLAS + ".");
        }
        if (tablasRotas < 0 || tablasRotas > tablasIntentadas) {
            throw new IllegalArgumentException(
                    "Las tablas rotas deben estar entre 0 y las tablas intentadas.");
        }
    }

    public static boolean esLimpio(int tablasIntentadas, int tablasRotas) {
        return tablasIntentadas >= 1 && tablasRotas == tablasIntentadas;
    }

    /** Puntos de un intento. */
    public static BigDecimal puntosIntento(BigDecimal coeficiente, int tablasIntentadas, int tablasRotas) {
        validar(tablasIntentadas, tablasRotas);
        BigDecimal puntos = coeficiente.multiply(BigDecimal.valueOf(tablasRotas));
        if (esLimpio(tablasIntentadas, tablasRotas)) {
            puntos = puntos.add(BONUS_POR_TABLA.multiply(BigDecimal.valueOf(tablasIntentadas)));
        }
        return puntos.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal total(List<BigDecimal> puntosPorIntento) {
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal p : puntosPorIntento) {
            suma = suma.add(p);
        }
        return suma.setScale(2, RoundingMode.HALF_UP);
    }
}
