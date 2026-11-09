package com.utp.gajog.service;

import com.utp.gajog.dto.FilaExamenDTO;
import com.utp.gajog.enums.*;
import com.utp.gajog.model.*;
import com.utp.gajog.support.EscenarioExamen;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ExamenConsultaServiceTest {
    EscenarioExamen datos;
    ExamenConsultaService servicio;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        servicio = datos.consultaService;
    }

    @Test
    void sinExamenSeMuestraEstadoInicial() {
        assertEquals("Sin examen", servicio.describirEstado(null, false));
    }

    @ParameterizedTest
    @CsvSource({"APROBADO,false,Ascenso aprobado", "REPROBADO,true,Examen reprobado",
        "PENDIENTE,false,Pago pendiente", "PENDIENTE,true,Pago registrado · por evaluar"})
    void describeCadaEstado(ResultadoExamen resultado, boolean pagado, String esperado) {
        Examen examen = new Examen();
        examen.setResultado(resultado);
        assertEquals(esperado, servicio.describirEstado(examen, pagado));
    }

    @Test
    void consultaUltimoExamenEHistorialDelAlumno() {
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        Examen previo = datos.agregarExamen(9L, alumno, null);
        Examen examen = datos.agregarExamen(10L, alumno, null);
        assertSame(examen, servicio.ultimoExamen(1L).orElseThrow());
        assertEquals(List.of(examen, previo), servicio.listarExamenesDelAlumno(1L));
    }

    @Test
    void listadoEliminaDuplicadosOrdenaYRelacionaPago() {
        Alumno zeta = datos.agregarAlumno(1L, "Ana", "Zapata");
        Alumno primero = datos.agregarAlumno(2L, "Luis", "alvarez");
        datos.matricular(zeta);
        datos.matricular(primero);
        datos.matricular(zeta);
        Examen examen = datos.agregarExamen(10L, zeta, null);
        datos.agregarPago(examen);
        List<FilaExamenDTO> filas = servicio.listarFilas();
        assertEquals(2, filas.size());
        assertSame(primero, filas.get(0).getAlumno());
        assertNull(filas.get(0).getExamen());
        assertFalse(filas.get(0).isPagado());
        assertTrue(filas.get(1).isPagado());
        assertSame(examen, filas.get(1).getExamen());
    }

    @Test
    void listadoVacioNoGeneraRegistros() {
        assertTrue(servicio.listarFilas().isEmpty());
        assertTrue(datos.examenes.guardados.isEmpty());
        assertTrue(datos.pagos.guardados.isEmpty());
    }
}
