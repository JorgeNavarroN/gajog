package com.utp.gajog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.CriterioExamen;
import com.utp.gajog.model.DetalleExamen;
import com.utp.gajog.model.Examen;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.CriterioExamenRepository;
import com.utp.gajog.repository.DetalleExamenRepository;
import com.utp.gajog.repository.ExamenRepository;
import com.utp.gajog.repository.HistorialCinturonesRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas del control A08: integridad de los datos del examen (Alex). */
class ExamenIntegridadTest {

    private ExamenRepository examenes;
    private CriterioExamenRepository criterios;
    private DetalleExamenRepository detalles;
    private ExamenPagoService pagos;
    private ExamenEvaluacionService servicio;

    @BeforeEach
    void preparar() {
        examenes = mock(ExamenRepository.class);
        criterios = mock(CriterioExamenRepository.class);
        detalles = mock(DetalleExamenRepository.class);
        pagos = mock(ExamenPagoService.class);
        servicio = new ExamenEvaluacionService(examenes, criterios, detalles,
                mock(HistorialCinturonesRepository.class), mock(AlumnoRepository.class),
                pagos, new BigDecimal("70"));

        Alumno alumno = new Alumno();
        alumno.setId(5L);
        Cinturon objetivo = new Cinturon();
        objetivo.setOrden(2);
        objetivo.setId(2L);
        alumno.setCinturonActual(null);

        Examen examen = new Examen();
        examen.setId(1L);
        examen.setAlumno(alumno);
        examen.setCinturonObjetivo(objetivo);

        CriterioExamen tecnica = new CriterioExamen();
        tecnica.setId(1L);

        when(examenes.findByIdConBloqueo(1L)).thenReturn(Optional.of(examen));
        when(pagos.tienePagoRegistrado(1L)).thenReturn(true);
        when(criterios.findAllByOrderByIdAsc()).thenReturn(List.of(tecnica));
    }

    @Test
    void rechazaEvaluarElExamenConOtroAlumno() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarResultado(1L, 9L,
                        Map.of(1L, new BigDecimal("80")), null));
        assertEquals("El examen no corresponde al alumno seleccionado.", error.getMessage());
        verify(detalles, never()).save(any(DetalleExamen.class));
    }

    @Test
    void rechazaCriteriosQueNoExisten() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarResultado(1L, 5L,
                        Map.of(99L, new BigDecimal("80")), null));
        assertEquals("Se recibió un criterio de evaluación que no existe.", error.getMessage());
        verify(detalles, never()).save(any(DetalleExamen.class));
    }
}
