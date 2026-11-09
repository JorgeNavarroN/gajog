package com.utp.gajog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraPoomsaeTest {

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void descartaLaNotaMasAltaYLaMasBajaYPromedia() {
        // Totales: 7.0, 8.0, 8.5, 9.0, 9.9  -> se quitan 7.0 y 9.9 -> (8.0+8.5+9.0)/3 = 8.50
        List<BigDecimal> totales = List.of(n("9.0"), n("7.0"), n("9.9"), n("8.0"), n("8.5"));
        assertEquals(n("8.50"), CalculadoraPoomsae.notaFinal(totales));
    }

    @Test
    void sumaExactitudYPresentacion() {
        assertEquals(n("8.5"), CalculadoraPoomsae.totalJuez(n("3.5"), n("5.0")));
    }

    @Test
    void rechazaExactitudMayorACuatro() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraPoomsae.totalJuez(n("4.1"), n("5.0")));
    }

    @Test
    void rechazaPresentacionNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraPoomsae.totalJuez(n("3.0"), n("-0.1")));
    }

    @Test
    void necesitaAlMenosTresJueces() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraPoomsae.notaFinal(List.of(n("8"), n("9"))));
    }
}
