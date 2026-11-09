package com.utp.gajog.service;

import com.utp.gajog.enums.*;
import com.utp.gajog.model.*;
import com.utp.gajog.support.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ExamenEvaluacionServiceTest {
    EscenarioExamen datos;
    RepositoriosExamen.Examenes examenes;
    RepositoriosExamen.Criterios criterios;
    RepositoriosExamen.Detalles detalles;
    RepositoriosExamen.Historial historial;
    RepositoriosExamen.Alumnos alumnos;
    ExamenEvaluacionService servicio;
    Examen examen;
    Alumno alumno;
    Cinturon blanco;
    Cinturon amarillo;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        examenes = datos.examenes;
        criterios = datos.criterios;
        detalles = datos.detalles;
        historial = datos.historial;
        alumnos = datos.alumnos;
        servicio = datos.evaluacionService;
        blanco = new Cinturon(); blanco.setOrden(1);
        amarillo = new Cinturon(); amarillo.setOrden(2);
        alumno = new Alumno(); alumno.setId(2L); alumno.setCinturonActual(blanco);
        examen = new Examen(); examen.setId(1L); examen.setAlumno(alumno); examen.setCinturonObjetivo(amarillo);
    }

    private void examenPagado() {
        examenes.agregar(examen);
        datos.agregarPago(examen);
    }

    private void configurarCriterios() {
        CriterioExamen criterio = new CriterioExamen(); criterio.setId(1L);
        criterio.setNombre("Poomsae"); criterio.setPuntajeMaximo(new BigDecimal("100"));
        criterios.agregar(criterio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"70", "100"})
    void aprobarActualizaCinturonYGuardaHistorial(String puntaje) {
        examenPagado(); configurarCriterios();
        assertSame(examen, servicio.registrarResultado(1L, Map.of(1L, new BigDecimal(puntaje)), "  Buen desempeño  "));
        assertEquals(ResultadoExamen.APROBADO, examen.getResultado());
        assertSame(amarillo, alumno.getCinturonActual());
        assertEquals("Buen desempeño", examen.getObservaciones());
        assertEquals(1, historial.guardados.size());
        HistorialCinturones registro = historial.guardados.getFirst();
        assertSame(alumno, registro.getAlumno());
        assertSame(amarillo, registro.getCinturon());
        assertSame(examen, registro.getExamen());
        assertEquals(1, detalles.guardados.size());
        assertEquals(List.of(alumno), alumnos.guardados);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "69.99"})
    void reprobarConservaCinturonYNoCreaAscenso(String puntaje) {
        examenPagado(); configurarCriterios();
        servicio.registrarResultado(1L, Map.of(1L, new BigDecimal(puntaje)), " ");
        assertEquals(ResultadoExamen.REPROBADO, examen.getResultado());
        assertSame(blanco, alumno.getCinturonActual());
        assertNull(examen.getObservaciones());
        assertEquals(0, examen.getPuntajeTotal().compareTo(new BigDecimal(puntaje)));
        assertEquals(1, detalles.guardados.size());
        assertEquals(new BigDecimal(puntaje).setScale(2), detalles.guardados.getFirst().getPuntaje());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
        assertEquals(List.of(examen), examenes.guardados);
    }

    @Test
    void examenNuloOInexistenteNoGuarda() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(null, Map.of(), null));
        examenes.datos.clear();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADO", "REPROBADO"})
    void examenTerminadoNoAdmiteReevaluacion(String resultado) {
        examen.setResultado(ResultadoExamen.valueOf(resultado));
        examenes.agregar(examen);
        assertThrows(IllegalStateException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @Test
    void sinPagoNoPermiteEvaluar() {
        examenes.agregar(examen);
        assertThrows(IllegalStateException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @Test
    void alumnoInactivoNoSeEvalua() {
        examenPagado(); alumno.setEstado(EstadoAlumno.INACTIVO);
        assertThrows(IllegalStateException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @Test
    void objetivoIgualOInferiorSeRechaza() {
        examenPagado(); amarillo.setOrden(1);
        assertThrows(IllegalStateException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
    }

    @Test
    void observacionExcesivaSeRechaza() {
        examenPagado();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, Map.of(), "x".repeat(1001)));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @Test
    void sinCriteriosConfiguradosSeRechaza() {
        examenPagado();
        assertThrows(IllegalStateException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
    }

    @Test
    void puntajesVaciosNulosOIncompletosSeRechazan() {
        examenPagado(); configurarCriterios();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, null, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, Map.of(99L, BigDecimal.ONE), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01"})
    void puntajeFueraDelRangoNoGuarda(String valor) {
        examenPagado(); configurarCriterios();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarResultado(1L, Map.of(1L, new BigDecimal(valor)), null));
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void sumaTodosLosCriteriosYGuardaDetalles() {
        examenPagado();
        CriterioExamen primero = new CriterioExamen(); primero.setId(1L); primero.setPuntajeMaximo(new BigDecimal("30"));
        CriterioExamen segundo = new CriterioExamen(); segundo.setId(2L); segundo.setPuntajeMaximo(new BigDecimal("70"));
        criterios.agregar(primero);
        criterios.agregar(segundo);
        servicio.registrarResultado(1L, Map.of(1L, new BigDecimal("30"), 2L, new BigDecimal("40")), null);
        assertEquals(new BigDecimal("70.00"), examen.getPuntajeTotal());
        assertEquals(2, detalles.guardados.size());
    }

    @Test
    void masDeTreintaEnPoomsaeRechazaTodaLaEvaluacion() {
        examenPagado();
        CriterioExamen basicos = new CriterioExamen();
        basicos.setId(1L);
        basicos.setNombre("Básicos");
        basicos.setPuntajeMaximo(new BigDecimal("25"));
        CriterioExamen poomsae = new CriterioExamen();
        poomsae.setId(2L);
        poomsae.setNombre("Poomsae");
        poomsae.setPuntajeMaximo(new BigDecimal("30"));
        criterios.agregar(basicos);
        criterios.agregar(poomsae);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> servicio.registrarResultado(1L,
                Map.of(1L, new BigDecimal("20"), 2L, new BigDecimal("31")), null));

        assertTrue(error.getMessage().contains("Poomsae"));
        assertTrue(error.getMessage().contains("30"));
        assertEquals(ResultadoExamen.PENDIENTE, examen.getResultado());
        assertTrue(detalles.guardados.isEmpty());
        assertTrue(historial.guardados.isEmpty());
        assertTrue(alumnos.guardados.isEmpty());
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void consultasDevuelvenCriteriosMaximoDetalleEHistorial() {
        configurarCriterios();
        assertEquals(1, servicio.listarCriterios().size());
        assertEquals(new BigDecimal("100"), servicio.puntajeMaximoTotal());
        assertEquals(new BigDecimal("70"), servicio.getPuntajeMinimo());
        DetalleExamen detalle = new DetalleExamen();
        HistorialCinturones ascenso = new HistorialCinturones();
        detalle.setExamen(examen);
        detalle.setCriterio(criterios.findAll().getFirst());
        detalles.agregar(detalle);
        ascenso.setAlumno(alumno);
        ascenso.setFechaObtencion(datos.hoy);
        historial.agregar(ascenso);
        assertEquals(List.of(detalle), servicio.listarDetalle(1L));
        assertEquals(List.of(ascenso), servicio.listarHistorial(2L));
    }
}
