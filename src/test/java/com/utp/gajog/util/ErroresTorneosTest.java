package com.utp.gajog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de casos limite y datos invalidos del modulo de Torneos.
 * Cada prueba describe el comportamiento CORRECTO esperado.
 */
class ErroresTorneosTest {

    @Test
    void error1_cinturonConOrdenCeroONegativoDebeRechazarse() {
        assertThrows(IllegalArgumentException.class, () -> CategoriaTorneo.nivelCinturon(0));
        assertThrows(IllegalArgumentException.class, () -> CategoriaTorneo.nivelCinturon(-3));
    }

    @Test
    void error2_edadIrrealDebeRechazarse() {
        assertThrows(IllegalArgumentException.class, () -> CategoriaTorneo.grupoEdad(150));
    }

    @Test
    void error3_notaDeJuezNulaDebeDarMensajeControlado() {
        assertThrows(IllegalArgumentException.class, () -> CalculadoraPoomsae.notaFinal(
                Arrays.asList(new BigDecimal("8"), null, new BigDecimal("7"),
                        new BigDecimal("9"), new BigDecimal("6"))));
    }

    @Test
    void error4_maximoDeFaltasCeroNoDebeDescalificarAtodos() {
        assertFalse(ReglasCombate.descalificadoPorFaltas(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasCombate.haySuperioridad(5, 5, 0));
    }

    @Test
    void error5_puntosNegativosDebenRechazarse() {
        assertThrows(IllegalArgumentException.class, () -> ReglasCombate.puntosRound(-3, 0));
        assertThrows(IllegalArgumentException.class, () -> ReglasCombate.puntosRound(2, -1));
    }

    @Test
    void error6_coeficienteNuloDebeDarMensajeControlado() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraRompimiento.puntosIntento(null, 3, 2));
    }

    @Test
    void error7_empateDeRoundsImposibleNoDebeDarGanador() {
        assertEquals(0, ReglasCombate.ganadorPorRounds(2, 2));
    }
}
