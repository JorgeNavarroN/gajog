package com.utp.gajog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReglasCombateTest {

    @Test
    void lasFaltasDelRivalSumanPuntos() {
        // anotó 5 puntos y el rival cometió 2 faltas -> 7
        assertEquals(7, ReglasCombate.puntosRound(5, 2));
    }

    @Test
    void decideElGanadorDelRound() {
        assertEquals(1, ReglasCombate.ganadorRound(6, 3));
        assertEquals(2, ReglasCombate.ganadorRound(2, 3));
        assertEquals(0, ReglasCombate.ganadorRound(4, 4));
    }

    @Test
    void detectaSuperioridadTecnica() {
        assertTrue(ReglasCombate.haySuperioridad(15, 3, 12));
        assertFalse(ReglasCombate.haySuperioridad(10, 3, 12));
    }

    @Test
    void descalificaPorFaltas() {
        assertTrue(ReglasCombate.descalificadoPorFaltas(10, 10));
        assertFalse(ReglasCombate.descalificadoPorFaltas(9, 10));
    }

    @Test
    void ganaQuienLlegaADosRounds() {
        assertEquals(1, ReglasCombate.ganadorPorRounds(2, 0));
        assertEquals(2, ReglasCombate.ganadorPorRounds(1, 2));
        assertEquals(0, ReglasCombate.ganadorPorRounds(1, 1));
    }
}
