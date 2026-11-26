package com.utp.gajog.service;

import com.utp.gajog.controller.ExamenEvaluacionController;
import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.Examen;
import com.utp.gajog.support.EscenarioExamen;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

class ExamenLimitesPuntajeTest {
    private EscenarioExamen datos;
    private Examen examen;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        Alumno alumno = datos.agregarAlumno(1L, "Alumno", "Prueba");
        Cinturon objetivo = new Cinturon();
        objetivo.setOrden(2);
        examen = datos.agregarExamen(10L, alumno, objetivo);
        datos.agregarPago(examen);
        datos.agregarCriterio(1L, "Poomsae", "30");
        datos.agregarCriterio(2L, "Técnica", "70");
    }

    @ParameterizedTest
    @ValueSource(strings = {"31", "30.001", "-0.001", "29.999"})
    void rechazaFueraDeRangoOPrecisionSinGuardar(String puntaje) {
        assertThrows(IllegalArgumentException.class, () -> datos.evaluacionService.registrarResultado(
                10L, 1L, Map.of(1L, new BigDecimal(puntaje), 2L, new BigDecimal("40")), null));
        assertEquals(ResultadoExamen.PENDIENTE, examen.getResultado());
        assertTrue(datos.detalles.guardados.isEmpty());
        assertTrue(datos.historial.guardados.isEmpty());
        assertTrue(datos.examenes.guardados.isEmpty());
    }

    @Test
    void aceptaLimitesYApruebaExactamenteConSetenta() {
        datos.evaluacionService.registrarResultado(10L, 1L,
                Map.of(1L, new BigDecimal("30.00"), 2L, new BigDecimal("40.00")), null);
        assertEquals(ResultadoExamen.APROBADO, examen.getResultado());
        assertEquals(new BigDecimal("70.00"), examen.getPuntajeTotal());
        assertEquals(1, datos.historial.guardados.size());
    }

    @Test
    void errorConservaValoresYObservacionesSinGuardar() {
        ExamenEvaluacionController controlador = new ExamenEvaluacionController(datos.evaluacionService);
        RedirectAttributesModelMap salida = new RedirectAttributesModelMap();
        Map<String, String> campos = Map.of("puntaje-1", "31", "puntaje-2", "40", "_csrf", "token");
        assertEquals("redirect:/examenes", controlador.registrarResultado(10L, 1L, "Comentario", campos, salida));
        assertEquals("evaluacion", salida.get("tab"));
        assertEquals(Map.of("puntaje-1", "31", "puntaje-2", "40"), salida.getFlashAttributes().get("puntajesIngresados"));
        assertEquals("Comentario", salida.getFlashAttributes().get("observacionesIngresadas"));
        assertTrue(datos.detalles.guardados.isEmpty());
    }
}
