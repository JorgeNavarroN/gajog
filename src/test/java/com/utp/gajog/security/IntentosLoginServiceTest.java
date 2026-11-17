package com.utp.gajog.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Pruebas del control A07: bloqueo por intentos fallidos (Jorge). */
class IntentosLoginServiceTest {

    /** Reloj que se puede adelantar a mano en la prueba. */
    static class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-14T10:00:00Z");
        void adelantar(Duration d) { ahora = ahora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void bloqueaDespuesDeCincoIntentosFallidos() {
        IntentosLoginService intentos = new IntentosLoginService(new RelojManual());
        for (int i = 0; i < 4; i++) {
            intentos.registrarFallo("recepcion");
        }
        assertFalse(intentos.estaBloqueado("recepcion"));
        intentos.registrarFallo("recepcion");
        assertTrue(intentos.estaBloqueado("recepcion"));
        assertEquals(5, intentos.fallos("recepcion"));
    }

    @Test
    void elBloqueoNoDistingueMayusculasNiEspacios() {
        IntentosLoginService intentos = new IntentosLoginService(new RelojManual());
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo(i % 2 == 0 ? "Admin" : " admin ");
        }
        assertTrue(intentos.estaBloqueado("admin"));
    }

    @Test
    void seDesbloqueaPasadosQuinceMinutos() {
        RelojManual reloj = new RelojManual();
        IntentosLoginService intentos = new IntentosLoginService(reloj);
        for (int i = 0; i < 5; i++) {
            intentos.registrarFallo("instructor");
        }
        reloj.adelantar(Duration.ofMinutes(14));
        assertTrue(intentos.estaBloqueado("instructor"));
        reloj.adelantar(Duration.ofMinutes(1));
        assertFalse(intentos.estaBloqueado("instructor"));
    }

    @Test
    void unIngresoCorrectoReiniciaElContador() {
        IntentosLoginService intentos = new IntentosLoginService(new RelojManual());
        intentos.registrarFallo("admin");
        intentos.registrarFallo("admin");
        intentos.registrarExito("admin");
        assertEquals(0, intentos.fallos("admin"));
    }
}
