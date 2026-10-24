package com.utp.gajog;

import com.utp.gajog.dto.ConfirmacionIngresoDniDTO;
import com.utp.gajog.dto.MatriculaWizardDTO;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import com.utp.gajog.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GajogIntegrationTest {

    @Autowired private MatriculaService matriculaService;
    @Autowired private PagoService pagoService;
    @Autowired private AsistenciaService asistenciaService;
    @Autowired private ExamenService examenService;
    @Autowired private AlumnoService alumnoService;

    @Autowired private HorarioRepository horarioRepository;
    @Autowired private CinturonRepository cinturonRepository;
    @Autowired private CriterioExamenRepository criterioExamenRepository;
    @Autowired private MensualidadRepository mensualidadRepository;
    @Autowired private HistorialCinturonesRepository historialCinturonesRepository;

    @Test
    @DisplayName("Flujo integral E2E con open-in-view=false: Matrícula -> Mensualidad -> Pago y Anulación -> Asistencia DNI -> Examen >= 70 asciende cinturón")
    void flujoIntegralCompletoDeAcademia() {
        // 1. MATRÍCULA COMPLETA EN 3 PASOS
        Horario horarioInfantil = horarioRepository.buscarHorariosConDetalle(null, null).stream()
                .filter(h -> "Infantil".equalsIgnoreCase(h.getCategoria().getNombre()))
                .findFirst()
                .orElseThrow();

        MatriculaWizardDTO wizard = new MatriculaWizardDTO();
        wizard.setDniAlumno("74581236");
        wizard.setNombresAlumno("Valeria");
        wizard.setApellidosAlumno("Mendoza");
        wizard.setFechaNacimiento(LocalDate.now().minusYears(9)); // 9 años -> Infantil
        wizard.setTelefonoContacto("987654321");
        wizard.setDniApoderado("41256398");
        wizard.setNombresApoderado("Lucía");
        wizard.setApellidosApoderado("Mendoza");
        wizard.setHorarioSeleccionadoId(horarioInfantil.getId());

        Matricula matricula = matriculaService.confirmarMatriculaWizard(wizard);
        Long alumnoId = matricula.getAlumno().getId();

        assertNotNull(matricula.getId());
        assertEquals(EstadoMatricula.ACTIVA, matricula.getEstado());
        assertEquals("Infantil", matricula.getCategoria().getNombre());

        // Verificar que la primera mensualidad se creó automáticamente en estado POR_VENCER
        List<Mensualidad> cuotas = pagoService.listarMensualidadesPorAlumno(alumnoId);
        assertEquals(1, cuotas.size());
        Mensualidad primeraCuota = cuotas.getFirst();
        assertEquals(EstadoMensualidad.POR_VENCER, primeraCuota.getEstado());

        // 2. PAGO Y ANULACIÓN CON MOTIVO
        Pago pago = pagoService.pagarMensualidad(primeraCuota.getId(), MedioPago.YAPE);
        assertEquals(EstadoPago.REGISTRADO, pago.getEstado());
        assertEquals(EstadoMensualidad.PAGADA, pagoService.obtenerMensualidadPorId(primeraCuota.getId()).getEstado());

        Pago pagoAnulado = pagoService.anularPago(pago.getId(), "Corrección de operación en caja");
        assertEquals(EstadoPago.ANULADO, pagoAnulado.getEstado());
        assertEquals(EstadoMensualidad.POR_VENCER, pagoService.obtenerMensualidadPorId(primeraCuota.getId()).getEstado());

        // 3. GENERACIÓN MENSUAL IDEMPOTENTE Y MORA
        int generadasDuplicadas = pagoService.generarMensualidadesMasivas(
                primeraCuota.getAnio(), primeraCuota.getMes(), null, primeraCuota.getFechaVencimiento()
        );
        assertEquals(0, generadasDuplicadas);

        primeraCuota.setFechaVencimiento(LocalDate.now().minusDays(20));
        mensualidadRepository.save(primeraCuota);
        int pasadasAVencida = pagoService.actualizarMensualidadesVencidas();
        assertTrue(pasadasAVencida >= 1);
        assertEquals(EstadoMensualidad.VENCIDA, pagoService.obtenerMensualidadPorId(primeraCuota.getId()).getEstado());

        // 4. ASISTENCIA RÁPIDA POR DNI
        ConfirmacionIngresoDniDTO ingresoDni = asistenciaService.registrarIngresoRapidoPorDni("74581236", horarioInfantil.getId());
        assertFalse(ingresoDni.requiereSeleccionHorario());
        assertNotNull(ingresoDni.horaIngreso());
        assertTrue(ingresoDni.alertaMensualidad());

        // 5. EXAMEN DE ASCENSO (>= 70 PUNTOS ACTUALIZA CINTURÓN E HISTORIAL)
        Cinturon cinturonAmarillo = cinturonRepository.findByOrden(2).orElseThrow();
        Examen examen = examenService.programarExamen(alumnoId, cinturonAmarillo.getId(), LocalDate.now());

        List<CriterioExamen> criterios = criterioExamenRepository.findAll();
        List<Long> criterioIds = criterios.stream().map(CriterioExamen::getId).toList();
        // 22 + 25 + 20 + 15 = 82.00 puntos (>= 70.00 -> APROBADO)
        List<BigDecimal> puntajes = List.of(
                new BigDecimal("22.00"),
                new BigDecimal("25.00"),
                new BigDecimal("20.00"),
                new BigDecimal("15.00")
        );

        Examen evaluado = examenService.calificarYFinalizarExamen(
                examen.getId(), criterioIds, puntajes, List.of("Buen básico", "Buen poomsae", "Combate sólido", "Teoría correcta"), new BigDecimal("70.00")
        );

        assertEquals(ResultadoExamen.APROBADO, evaluado.getResultado());
        assertEquals(new BigDecimal("82.00"), evaluado.getPuntajeTotal());

        Alumno alumnoAscendido = alumnoService.obtenerPorId(alumnoId);
        assertEquals("Amarillo (8° Gup)", alumnoAscendido.getCinturonActual().getNombre());

        List<HistorialCinturones> historial = historialCinturonesRepository.findByAlumnoIdOrderByFechaObtencionDesc(alumnoId);
        assertEquals(2, historial.size()); // Blanco inicial + Amarillo por examen
    }
}