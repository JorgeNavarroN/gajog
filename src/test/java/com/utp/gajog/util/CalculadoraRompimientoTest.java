package com.utp.gajog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraRompimientoTest {

    private static BigDecimal n(String v) {
        return new BigDecimal(v);
    }

    @Test
    void puntosSonTablasRotasPorCoeficiente() {
        // 2 de 3 tablas con patada lateral (1.5) -> 3.00, sin bonus
        assertEquals(n("3.00"), CalculadoraRompimiento.puntosIntento(n("1.5"), 3, 2));
    }

    @Test
    void rompimientoLimpioSumaBonus() {
        // 3 de 3 con patada de salto (2.0) -> 6.0 + bonus 1.5 = 7.50
        assertEquals(n("7.50"), CalculadoraRompimiento.puntosIntento(n("2.0"), 3, 3));
        assertTrue(CalculadoraRompimiento.esLimpio(3, 3));
        assertFalse(CalculadoraRompimiento.esLimpio(3, 2));
    }

    @Test
    void ningunaTablaRotaDaCero() {
        assertEquals(n("0.00"), CalculadoraRompimiento.puntosIntento(n("2.5"), 4, 0));
    }

    @Test
    void sumaElTotalDeLosIntentos() {
        assertEquals(n("11.50"), CalculadoraRompimiento.total(List.of(n("3.00"), n("7.50"), n("1.00"))));
    }

    @Test
    void validaQueLasRotasNoSuperenLasIntentadas() {
        assertThrows(IllegalArgumentException.class, () -> CalculadoraRompimiento.validar(3, 4));
        assertThrows(IllegalArgumentException.class, () -> CalculadoraRompimiento.validar(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CalculadoraRompimiento.validar(11, 5));
    }
}
