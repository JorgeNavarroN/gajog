package com.utp.gajog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CategoriaTorneoTest {

    @Test
    void asignaGrupoDeEdad() {
        assertEquals("Infantil", CategoriaTorneo.grupoEdad(11));
        assertEquals("Juvenil", CategoriaTorneo.grupoEdad(12));
        assertEquals("Juvenil", CategoriaTorneo.grupoEdad(16));
        assertEquals("Adulto", CategoriaTorneo.grupoEdad(17));
    }

    @Test
    void asignaNivelPorCinturon() {
        assertEquals("Principiante", CategoriaTorneo.nivelCinturon(1));
        assertEquals("Intermedio", CategoriaTorneo.nivelCinturon(5));
        assertEquals("Avanzado", CategoriaTorneo.nivelCinturon(8));
    }

    @Test
    void asignaCategoriaDePeso() {
        assertEquals("Ligero", CategoriaTorneo.categoriaPeso(new BigDecimal("44.9")));
        assertEquals("Medio", CategoriaTorneo.categoriaPeso(new BigDecimal("60")));
        assertEquals("Pesado", CategoriaTorneo.categoriaPeso(new BigDecimal("60.1")));
    }

    @Test
    void armaElTextoCompleto() {
        assertEquals("Juvenil · Intermedio · Medio",
                CategoriaTorneo.calcular(14, 5, new BigDecimal("52")));
    }

    @Test
    void rechazaPesoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> CategoriaTorneo.categoriaPeso(BigDecimal.ZERO));
    }
}
