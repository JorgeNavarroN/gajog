package com.utp.gajog.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas del control A05: validación de entradas en Torneos (Amir). */
class TorneoFormValidacionTest {

    private final Validator validador =
            Validation.buildDefaultValidatorFactory().getValidator();

    private TorneoForm formularioValido() {
        TorneoForm f = new TorneoForm();
        f.setNombre("Copa Primavera 2026");
        f.setFecha(LocalDate.of(2026, 12, 5));
        f.setSede("Coliseo de Lince");
        f.setCuotaInscripcion(new BigDecimal("30.00"));
        return f;
    }

    @Test
    void aceptaUnTorneoNormalConTildes() {
        TorneoForm f = formularioValido();
        f.setNombre("Campeonato Nacional Juvenil - Perú");
        assertTrue(validador.validate(f).isEmpty());
    }

    @Test
    void rechazaEtiquetasHtmlOScripts() {
        TorneoForm f = formularioValido();
        f.setNombre("<script>alert('hack')</script>");
        assertFalse(validador.validate(f).isEmpty());
    }

    @Test
    void rechazaInyeccionSqlEnLaSede() {
        TorneoForm f = formularioValido();
        f.setSede("Lima'; DROP TABLE torneos; --");
        assertFalse(validador.validate(f).isEmpty());
    }

    @Test
    void rechazaCuotaNegativaOEnorme() {
        TorneoForm f = formularioValido();
        f.setCuotaInscripcion(new BigDecimal("-5"));
        assertFalse(validador.validate(f).isEmpty());
        f.setCuotaInscripcion(new BigDecimal("999999999"));
        assertFalse(validador.validate(f).isEmpty());
    }

    @Test
    void rechazaPesoFueraDeRangoEnLaInscripcion() {
        InscripcionTorneoForm i = new InscripcionTorneoForm();
        i.setAlumnoId(1L);
        i.setPesoKg(new BigDecimal("500"));
        assertFalse(validador.validate(i).isEmpty());
        i.setPesoKg(new BigDecimal("52.5"));
        assertTrue(validador.validate(i).isEmpty());
    }
}
