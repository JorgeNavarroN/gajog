package com.utp.gajog.controller;

import com.utp.gajog.dto.FilaExamenDTO;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.support.EscenarioExamen;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;

class ExamenNavegacionTest {
    private EscenarioExamen datos;
    private ExamenWebController controlador;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        controlador = new ExamenWebController(datos.consultaService, datos.pagoService, datos.evaluacionService);
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        alumno.getPersona().setDni("12345678");
        datos.matricular(alumno);
        Alumno segundo = datos.agregarAlumno(2L, "Luis", "Castro");
        segundo.getPersona().setDni("87654321");
        datos.matricular(segundo);
        datos.agregarPago(datos.agregarExamen(10L, segundo, null));
    }

    @Test
    void entradaPresentaListadoYEstados() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        assertEquals("examenes", controlador.mostrar(null, null, "", "", modelo));
        assertEquals(2, ((List<?>) modelo.get("filas")).size());
        assertEquals(5, ((List<?>) modelo.get("estadosExamen")).size());
    }

    @Test
    void combinaBusquedaPorDniConEstado() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        controlador.mostrar(null, null, "8765", "Pago registrado · por evaluar", modelo);
        List<?> filas = (List<?>) modelo.get("filas");
        assertEquals(1, filas.size());
        assertEquals(2L, ((FilaExamenDTO) filas.getFirst()).getAlumno().getId());
    }

    @Test
    void busquedaIgnoraMayusculasYEspacios() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        controlador.mostrar(null, null, "  ANA  ", "", modelo);
        assertEquals(1, ((List<?>) modelo.get("filas")).size());
    }

    @Test
    void seleccionAbreGestionSinDependerDelFiltro() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        assertEquals("examenes/alumno", controlador.mostrar(1L, null, "no coincide", "", modelo));
        assertEquals("pago", modelo.get("tab"));
    }

    @Test
    void alumnoInexistenteRegresaAlListadoConAviso() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        assertEquals("examenes", controlador.mostrar(999L, null, "", "", modelo));
        assertNotNull(modelo.get("error"));
    }
}
