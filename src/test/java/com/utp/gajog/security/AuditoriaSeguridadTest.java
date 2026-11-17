package com.utp.gajog.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** Pruebas del control A09: los datos del log no se pueden falsificar (Joaquín). */
class AuditoriaSeguridadTest {

    @Test
    void quitaSaltosDeLineaParaEvitarLineasFalsas() {
        String limpio = AuditoriaSeguridadListener.limpiar("ana\nLOGIN_OK usuario=admin");
        assertFalse(limpio.contains("\n"));
        assertEquals("ana_LOGIN_OK usuario=admin", limpio);
    }

    @Test
    void recortaTextosMuyLargos() {
        assertEquals(100, AuditoriaSeguridadListener.limpiar("x".repeat(500)).length());
    }

    @Test
    void usaGuionSiNoHayUsuario() {
        assertEquals("-", AuditoriaSeguridadListener.limpiar(null));
    }
}
