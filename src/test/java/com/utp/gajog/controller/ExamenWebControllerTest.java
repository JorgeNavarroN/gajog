package com.utp.gajog.controller;

import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.*;
import com.utp.gajog.support.EscenarioExamen;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;

class ExamenWebControllerTest {
    EscenarioExamen datos;
    ExamenWebController controlador;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        controlador = new ExamenWebController(datos.consultaService, datos.pagoService, datos.evaluacionService);
    }

    @Test
    void sinSeleccionOAlumnoAjenoSoloPresentaListado() {
        for (Long id : Arrays.asList(null, 999L)) {
            ExtendedModelMap modelo = new ExtendedModelMap();
            assertEquals("examenes", controlador.mostrar(id, null, modelo));
            assertNull(modelo.get("seleccionado"));
            assertTrue(modelo.containsAttribute("filas"));
            assertTrue(modelo.containsAttribute("mediosPago"));
        }
        assertTrue(datos.pagos.guardados.isEmpty());
        assertTrue(datos.examenes.guardados.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"SIN,false,pago", "PENDIENTE,false,pago", "PENDIENTE,true,evaluacion", "APROBADO,true,resultado", "REPROBADO,true,resultado"})
    void seleccionCargaDatosYDecidePestana(String estado, boolean pagado, String pestana) {
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        datos.matricular(alumno);
        Examen examen = null;
        if (!estado.equals("SIN")) {
            examen = datos.agregarExamen(10L, alumno, null);
            examen.setResultado(ResultadoExamen.valueOf(estado));
            if (pagado) {
                datos.agregarPago(examen);
            }
        }
        ExtendedModelMap modelo = new ExtendedModelMap();
        assertEquals("examenes", controlador.mostrar(1L, "invalida", modelo));
        assertSame(alumno, modelo.get("seleccionado"));
        assertSame(examen, modelo.get("examen"));
        assertEquals(pestana, modelo.get("tab"));
        assertEquals(pagado, modelo.get("pagado"));
        assertEquals(new BigDecimal("80.00"), modelo.get("tarifaExamen"));
        assertTrue(modelo.containsAttribute("criterios"));
        assertTrue(modelo.containsAttribute("historial"));
        assertEquals(examen == null ? List.of() : List.of(examen), modelo.get("examenesAlumno"));
        assertEquals(List.of(), modelo.get("detalles"));
        if (estado.equals("PENDIENTE") && pagado) {
            assertSame(datos.pagos.findAll().getFirst(), modelo.get("pagoExamen"));
        } else {
            assertNull(modelo.get("pagoExamen"));
        }
    }

    @ParameterizedTest
    @CsvSource({"pago", "evaluacion", "resultado"})
    void respetaPestanaSolicitada(String pestana) {
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        datos.matricular(alumno);
        ExtendedModelMap modelo = new ExtendedModelMap();
        controlador.mostrar(1L, pestana, modelo);
        assertEquals(pestana, modelo.get("tab"));
    }
}
