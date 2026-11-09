package com.utp.gajog.controller;

import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.*;
import com.utp.gajog.support.EscenarioExamen;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

class ExamenEvaluacionControllerTest {
    EscenarioExamen datos;
    ExamenEvaluacionController controlador;
    Examen examen;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        controlador = new ExamenEvaluacionController(datos.evaluacionService);
        Alumno alumno = datos.agregarAlumno(4L, "Ana", "Pérez");
        Cinturon blanco = new Cinturon();
        blanco.setOrden(1);
        alumno.setCinturonActual(blanco);
        Cinturon amarillo = new Cinturon();
        amarillo.setOrden(2);
        examen = datos.agregarExamen(1L, alumno, amarillo);
        datos.agregarPago(examen);
        datos.agregarCriterio(2L, "Poomsae", "100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADO", "REPROBADO"})
    void conviertePuntajesYRedirigeAlResultado(String estado) {
        String valor = estado.equals("APROBADO") ? " 70,50 " : " 25,50 ";
        RedirectAttributesModelMap salida = new RedirectAttributesModelMap();
        Map<String, String> entrada = Map.of("puntaje-2", valor, "puntaje-3", "", "alumnoId", "4");
        assertEquals("redirect:/examenes", controlador.registrarResultado(1L, 4L, "Observación", entrada, salida));
        assertEquals(ResultadoExamen.valueOf(estado), examen.getResultado());
        assertEquals(new BigDecimal(estado.equals("APROBADO") ? "70.50" : "25.50"), examen.getPuntajeTotal());
        assertEquals("Observación", examen.getObservaciones());
        assertEquals("resultado", salida.get("tab"));
        assertEquals("4", salida.get("alumno"));
        String mensaje = salida.getFlashAttributes().get("exito").toString();
        assertTrue(mensaje.contains(estado.equals("APROBADO") ? "actualizado" : "conserva"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "NaN"})
    void puntajeNoNumericoMuestraErrorSinGuardar(String valor) {
        RedirectAttributesModelMap salida = new RedirectAttributesModelMap();
        controlador.registrarResultado(1L, 4L, null, Map.of("puntaje-2", valor), salida);
        assertEquals("evaluacion", salida.get("tab"));
        assertTrue(salida.getFlashAttributes().containsKey("error"));
        assertEquals(ResultadoExamen.PENDIENTE, examen.getResultado());
        assertTrue(datos.detalles.guardados.isEmpty());
        assertTrue(datos.examenes.guardados.isEmpty());
    }

    @Test
    void errorDelServicioRegresaAEvaluacion() {
        RedirectAttributesModelMap salida = new RedirectAttributesModelMap();
        controlador.registrarResultado(1L, 4L, null, Map.of("puntaje-2", "101"), salida);
        assertEquals("evaluacion", salida.get("tab"));
        assertTrue(salida.getFlashAttributes().get("error").toString().contains("entre 0 y 100"));
        assertTrue(datos.detalles.guardados.isEmpty());
        assertTrue(datos.historial.guardados.isEmpty());
    }
}
