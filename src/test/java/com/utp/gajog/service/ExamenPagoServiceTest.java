package com.utp.gajog.service;

import com.utp.gajog.enums.*;
import com.utp.gajog.model.*;
import com.utp.gajog.support.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ExamenPagoServiceTest {
    EscenarioExamen datos;
    RepositoriosExamen.Alumnos alumnos;
    RepositoriosExamen.Cinturones cinturones;
    RepositoriosExamen.Examenes examenes;
    RepositoriosExamen.Pagos pagos;
    RepositoriosExamen.Tarifas tarifas;
    RepositoriosExamen.Asistencias asistencias;
    ExamenPagoService servicio;
    final LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
    Alumno alumno;
    Cinturon blanco;
    Cinturon amarillo;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        alumnos = datos.alumnos;
        cinturones = datos.cinturones;
        examenes = datos.examenes;
        pagos = datos.pagos;
        tarifas = datos.tarifas;
        asistencias = datos.asistencias;
        servicio = datos.pagoService;
        blanco = new Cinturon(); blanco.setOrden(1);
        amarillo = new Cinturon(); amarillo.setOrden(2);
        alumno = new Alumno(); alumno.setId(1L); alumno.setCinturonActual(blanco);
    }

    private void alumnoApto() {
        alumnos.agregar(alumno);
        datos.matricular(alumno);
    }

    private Examen examenPendiente() {
        alumnoApto();
        Examen examen = new Examen(); examen.setId(10L); examen.setAlumno(alumno);
        examenes.agregar(examen);
        return examen;
    }

    @Test
    void tarifaPredeterminadaSinTarifasVigentes() {
        assertEquals(new BigDecimal("80.00"), servicio.tarifaExamen());
    }

    @Test
    void tarifaSeleccionaVigenteMasReciente() {
        Tarifa vieja = tarifa("90", hoy.minusMonths(2), null);
        Tarifa reciente = tarifa("100", hoy.minusDays(1), hoy);
        Tarifa futura = tarifa("200", hoy.plusDays(1), null);
        Tarifa vencida = tarifa("300", hoy.minusDays(3), hoy.minusDays(1));
        Tarifa invalida = tarifa("0", hoy, null);
        List.of(vieja, reciente, futura, vencida, invalida).forEach(tarifas::agregar);
        assertEquals(new BigDecimal("100"), servicio.tarifaExamen());
    }

    private Tarifa tarifa(String monto, LocalDate inicio, LocalDate fin) {
        Tarifa tarifa = new Tarifa(); tarifa.setTipoPago(TipoPago.EXAMEN); tarifa.setMonto(new BigDecimal(monto)); tarifa.setFechaInicio(inicio); tarifa.setFechaFin(fin);
        return tarifa;
    }

    @Test
    void siguienteCinturonEsElSuperiorInmediato() {
        alumnos.agregar(alumno);
        cinturones.agregar(blanco);
        cinturones.agregar(amarillo);
        assertSame(amarillo, servicio.proximoCinturon(1L).orElseThrow());
    }

    @Test
    void sinCinturonActualOSinSiguienteDevuelveVacio() {
        alumnos.agregar(alumno);
        assertTrue(servicio.proximoCinturon(1L).isEmpty());
        alumno.setCinturonActual(null);
        assertTrue(servicio.proximoCinturon(1L).isEmpty());
    }

    @Test
    void requisitosCalculanAsistenciaYAntiguedadEnElLimite() {
        alumnos.agregar(alumno);
        alumno.setFechaIngreso(hoy.minusMonths(3));
        List<Asistencia> registros = new ArrayList<>();
        for (int indice = 0; indice < 4; indice++) {
            Asistencia asistencia = new Asistencia();
            asistencia.setAlumno(alumno);
            asistencia.setEstado(indice < 3 ? EstadoAsistencia.ASISTIO : EstadoAsistencia.NO_ASISTIO);
            registros.add(asistencia);
        }
        registros.forEach(asistencias::agregar);
        ExamenPagoService.Requisitos requisitos = servicio.consultarRequisitos(1L);
        assertEquals(75, requisitos.getAsistenciaPorcentaje());
        assertEquals(3, requisitos.getMesesAntiguedad());
        assertTrue(requisitos.isCumpleAsistencia());
        assertTrue(requisitos.isCumpleAntiguedad());
    }

    @Test
    void requisitosSinDatosNoCumplen() {
        alumnos.agregar(alumno);
        ExamenPagoService.Requisitos requisitos = servicio.consultarRequisitos(1L);
        assertEquals(0, requisitos.getAsistenciaPorcentaje());
        assertEquals(0, requisitos.getMesesAntiguedad());
        assertFalse(requisitos.isCumpleAsistencia());
        assertFalse(requisitos.isCumpleAntiguedad());
    }

    @Test
    void consultaPagoNuloAusenteYRegistrado() {
        assertFalse(servicio.tienePagoRegistrado(null));
        assertTrue(servicio.pagoDelExamen(null).isEmpty());
        assertFalse(servicio.tienePagoRegistrado(10L));
        Pago pago = new Pago();
        Examen examen = new Examen();
        examen.setId(10L);
        pago.setExamen(examen);
        pagos.agregar(pago);
        assertTrue(servicio.tienePagoRegistrado(10L));
        assertSame(pago, servicio.pagoDelExamen(10L).orElseThrow());
    }

    @Test
    void programarReutilizaExamenPendiente() {
        Examen pendiente = examenPendiente();
        assertSame(pendiente, servicio.programarExamen(1L, hoy));
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void programarCreaExamenConAlumnoObjetivoYFecha() {
        alumnoApto();
        cinturones.agregar(blanco);
        cinturones.agregar(amarillo);
        Examen examen = servicio.programarExamen(1L, hoy);
        assertSame(alumno, examen.getAlumno());
        assertSame(amarillo, examen.getCinturonObjetivo());
        assertEquals(hoy, examen.getFechaExamen());
        assertEquals(ResultadoExamen.PENDIENTE, examen.getResultado());
    }

    @Test
    void programarSinFechaOSinAscensoSeRechaza() {
        alumnoApto();
        assertThrows(IllegalArgumentException.class, () -> servicio.programarExamen(1L, null));
        assertThrows(IllegalStateException.class, () -> servicio.programarExamen(1L, hoy));
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void alumnoNuloInexistenteInactivoOSinMatriculaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio.programarExamen(null, hoy));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarExamen(1L, hoy));
        alumnos.agregar(alumno);
        alumno.setEstado(EstadoAlumno.INACTIVO);
        assertThrows(IllegalStateException.class, () -> servicio.programarExamen(1L, hoy));
        alumno.setEstado(EstadoAlumno.ACTIVO);
        assertThrows(IllegalStateException.class, () -> servicio.programarExamen(1L, hoy));
        assertTrue(pagos.guardados.isEmpty());
        assertTrue(examenes.guardados.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void montoNoPositivoSeRechazaSinGuardar(String monto) {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, new BigDecimal(monto), MedioPago.EFECTIVO, hoy));
        assertTrue(pagos.guardados.isEmpty());
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void datosObligatoriosYFechaFuturaSeValidan() {
        BigDecimal monto = new BigDecimal("80");
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(null, monto, MedioPago.EFECTIVO, hoy));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, null, MedioPago.EFECTIVO, hoy));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, monto, null, hoy));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, monto, MedioPago.EFECTIVO, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, monto, MedioPago.EFECTIVO, hoy.plusDays(1)));
        assertTrue(pagos.guardados.isEmpty());
        assertTrue(examenes.guardados.isEmpty());
    }

    @Test
    void registrarPagoGuardaTodosLosDatos() {
        Examen examen = examenPendiente();

        Pago pago = servicio.registrarPagoExamen(1L, new BigDecimal("80"), MedioPago.EFECTIVO, hoy);
        assertSame(alumno, pago.getAlumno());
        assertSame(examen, pago.getExamen());
        assertEquals(TipoPago.EXAMEN, pago.getTipoPago());
        assertEquals(new BigDecimal("80"), pago.getMonto());
        assertEquals(MedioPago.EFECTIVO, pago.getMedioPago());
        assertEquals(hoy.atStartOfDay(), pago.getFechaPago());
        assertEquals(EstadoPago.REGISTRADO, pago.getEstado());
    }

    @Test
    void pagoDuplicadoSeRechazaSinGuardar() {
        Examen examen = examenPendiente();

        datos.agregarPago(examen);
        assertThrows(IllegalStateException.class, () -> servicio.registrarPagoExamen(1L, new BigDecimal("80"), MedioPago.EFECTIVO, hoy));
        assertTrue(pagos.guardados.isEmpty());
    }

    @Test
    void montoDistintoDeTarifaSeRechaza() {
        Examen examen = examenPendiente();

        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPagoExamen(1L, new BigDecimal("79.99"), MedioPago.EFECTIVO, hoy));
        assertTrue(pagos.guardados.isEmpty());
    }

    @Test
    void examenNoDisponibleParaBloqueoNoGuardaPago() {
        examenPendiente();
        examenes.disponibleParaLecturaConBloqueo = false;
        assertThrows(IllegalStateException.class, () -> servicio.registrarPagoExamen(1L, new BigDecimal("80"), MedioPago.EFECTIVO, hoy));
        assertTrue(pagos.guardados.isEmpty());
    }
}
