package com.utp.gajog.service;

import com.utp.gajog.config.NegocioProperties;
import com.utp.gajog.config.TimeConfig;
import com.utp.gajog.dto.RegistrarPagoFormDTO;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private MensualidadRepository mensualidadRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private ExamenRepository examenRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private TarifaRepository tarifaRepository;
    @Mock private InstructorRepository instructorRepository;
    @Mock private ObservacionProgresoRepository observacionProgresoRepository;

    private NegocioProperties negocioProperties;
    private Clock fixedClock;
    private PagoService pagoService;
    private Alumno alumnoActivo;

    @BeforeEach
    void setUp() {
        negocioProperties = new NegocioProperties();
        negocioProperties.setDiasAvisoPorVencer(5);
        negocioProperties.setDiasGraciaMora(15);
        negocioProperties.setBajaAutomaticaMora(false);

        // Fecha fija: 20 de octubre de 2026
        fixedClock = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), TimeConfig.ZONA_LIMA);

        pagoService = new PagoService(
                mensualidadRepository,
                pagoRepository,
                alumnoRepository,
                examenRepository,
                matriculaRepository,
                tarifaRepository,
                instructorRepository,
                observacionProgresoRepository,
                negocioProperties,
                fixedClock
        );

        Persona persona = new Persona();
        persona.setId(1L);
        persona.setDni("74581236");
        persona.setNombres("Valeria");
        persona.setApellidos("Mendoza");
        persona.setFechaNacimiento(LocalDate.of(2017, 5, 10));

        alumnoActivo = new Alumno();
        alumnoActivo.setId(10L);
        alumnoActivo.setPersona(persona);
        alumnoActivo.setEstado(EstadoAlumno.ACTIVO);
    }

    @Test
    @DisplayName("1. Unicidad de periodo: rechaza generar o cobrar dos veces el mismo año/mes")
    void debeRechazarDuplicidadDePeriodoYCobroDoble() {
        Mensualidad existente = new Mensualidad();
        existente.setId(100L);
        existente.setAlumno(alumnoActivo);
        existente.setAnio(2026);
        existente.setMes(10);
        existente.setMonto(new BigDecimal("100.00"));
        existente.setEstado(EstadoMensualidad.PAGADA);

        when(mensualidadRepository.findByAlumnoIdAndAnioAndMes(10L, 2026, 10))
                .thenReturn(Optional.of(existente));

        assertThrows(ReglaNegocioException.class, () ->
                pagoService.generarMensualidad(10L, 2026, 10, new BigDecimal("100.00"), LocalDate.of(2026, 10, 25))
        );

        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumnoActivo));
        when(mensualidadRepository.findById(100L)).thenReturn(Optional.of(existente));

        RegistrarPagoFormDTO form = new RegistrarPagoFormDTO();
        form.setMensualidadId(100L);
        form.setAlumnoId(10L);
        form.setAnio(2026);
        form.setMes(10);
        form.setMonto(new BigDecimal("100.00"));
        form.setMedioPago(MedioPago.YAPE);

        assertThrows(ReglaNegocioException.class, () ->
                pagoService.registrarPagoMensualidadDesdeFormulario(form, true)
        );
    }

    @Test
    @DisplayName("2. Anulación: exige motivo y devuelve la cuota a POR_VENCER o VENCIDA según la fecha")
    void debeAnularPagoYRestaurarEstadoDeMensualidadSegunFecha() {
        Mensualidad cuotaVencida = new Mensualidad();
        cuotaVencida.setId(200L);
        cuotaVencida.setAlumno(alumnoActivo);
        cuotaVencida.setAnio(2026);
        cuotaVencida.setMes(10);
        cuotaVencida.setFechaVencimiento(LocalDate.of(2026, 10, 10)); // Anterior al 20/10/2026
        cuotaVencida.setEstado(EstadoMensualidad.PAGADA);

        Pago pago = new Pago();
        pago.setId(500L);
        pago.setAlumno(alumnoActivo);
        pago.setMensualidad(cuotaVencida);
        pago.setMonto(new BigDecimal("100.00"));
        pago.setEstado(EstadoPago.REGISTRADO);

        assertThrows(ReglaNegocioException.class, () -> pagoService.anularPago(500L, "   "));

        when(pagoRepository.findById(500L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago anulado = pagoService.anularPago(500L, "Error de digitación en operación");

        assertEquals(EstadoPago.ANULADO, anulado.getEstado());
        assertEquals("Error de digitación en operación", anulado.getMotivoAnulacion());
        assertEquals(EstadoMensualidad.VENCIDA, cuotaVencida.getEstado());
    }

    @Test
    @DisplayName("3. Transición a VENCIDA: pasa cuotas POR_VENCER cuya fecha límite expiró a VENCIDA")
    void debeTransicionarCuotasPorVencerAVencidas() {
        Mensualidad expirada = new Mensualidad();
        expirada.setId(1L);
        expirada.setFechaVencimiento(LocalDate.of(2026, 10, 19));
        expirada.setEstado(EstadoMensualidad.POR_VENCER);

        Mensualidad vigente = new Mensualidad();
        vigente.setId(2L);
        vigente.setFechaVencimiento(LocalDate.of(2026, 10, 25));
        vigente.setEstado(EstadoMensualidad.POR_VENCER);

        when(mensualidadRepository.findByEstado(EstadoMensualidad.POR_VENCER))
                .thenReturn(List.of(expirada, vigente));

        int actualizadas = pagoService.actualizarMensualidadesVencidas();

        assertEquals(1, actualizadas);
        assertEquals(EstadoMensualidad.VENCIDA, expirada.getEstado());
        assertEquals(EstadoMensualidad.POR_VENCER, vigente.getEstado());
    }

    @Test
    @DisplayName("4. Gracia de mora: no da de baja automáticamente por defecto y permite retiro manual y reincorporación")
    void debeRespetarGraciaDeMoraYPermitirReincorporacion() {
        Mensualidad moraCritica = new Mensualidad();
        moraCritica.setId(300L);
        moraCritica.setAlumno(alumnoActivo);
        moraCritica.setAnio(2026);
        moraCritica.setMes(9);
        moraCritica.setFechaVencimiento(LocalDate.of(2026, 9, 30)); // > 15 días antes del 20/10/2026
        moraCritica.setEstado(EstadoMensualidad.VENCIDA);

        when(mensualidadRepository.findByEstado(EstadoMensualidad.POR_VENCER)).thenReturn(List.of());
        when(mensualidadRepository.findEnRiesgoDeRetiro(LocalDate.of(2026, 10, 5)))
                .thenReturn(List.of(moraCritica));

        // Con bajaAutomaticaMora = false no debe retirar automáticamente
        int retiradosAuto = pagoService.procesarMoraDiaria();
        assertEquals(0, retiradosAuto);
        assertEquals(EstadoAlumno.ACTIVO, alumnoActivo.getEstado());

        // Retiro manual confirmado por administrador
        when(alumnoRepository.findById(10L)).thenReturn(Optional.of(alumnoActivo));
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(inv -> inv.getArgument(0));
        when(instructorRepository.findAll()).thenReturn(List.of());

        Alumno retirado = pagoService.confirmarRetiroPorMora(10L, "Mora mayor a 15 días confirmada");
        assertEquals(EstadoAlumno.RETIRADO, retirado.getEstado());

        // Reincorporación al pagar
        Alumno reincorporado = pagoService.reincorporarAlumno(10L, "Deuda regularizada en recepción");
        assertEquals(EstadoAlumno.ACTIVO, reincorporado.getEstado());
    }

    @Test
    @DisplayName("5. Idempotencia del generador: ejecutar dos veces solo crea la mensualidad en la primera")
    void generadorMasivoDebeSerIdempotente() {
        Categoria infantil = new Categoria();
        infantil.setId(1L);
        infantil.setNombre("Infantil");

        Matricula matriculaActiva = new Matricula();
        matriculaActiva.setAlumno(alumnoActivo);
        matriculaActiva.setCategoria(infantil);
        matriculaActiva.setEstado(EstadoMatricula.ACTIVA);

        Tarifa tarifaInfantil = new Tarifa();
        tarifaInfantil.setCategoria(infantil);
        tarifaInfantil.setMontoVigente(new BigDecimal("100.00"));

        when(alumnoRepository.findByEstado(EstadoAlumno.ACTIVO)).thenReturn(List.of(alumnoActivo));
        when(matriculaRepository.findByAlumnoIdAndEstado(10L, EstadoMatricula.ACTIVA))
                .thenReturn(List.of(matriculaActiva));
        when(tarifaRepository.findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(1L))
                .thenReturn(Optional.of(tarifaInfantil));

        // Primera corrida: no existe aún -> devuelve empty; segunda corrida: ya existe
        when(mensualidadRepository.findByAlumnoIdAndAnioAndMes(10L, 2026, 10))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Mensualidad()));

        int primeraEjecucion = pagoService.generarMensualidadesMasivas(2026, 10, null, LocalDate.of(2026, 10, 10));
        int segundaEjecucion = pagoService.generarMensualidadesMasivas(2026, 10, null, LocalDate.of(2026, 10, 10));

        assertEquals(1, primeraEjecucion);
        assertEquals(0, segundaEjecucion);
        verify(mensualidadRepository, times(1)).save(any(Mensualidad.class));
    }
}