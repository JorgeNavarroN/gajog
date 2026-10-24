package com.utp.gajog.service;

import com.utp.gajog.config.TimeConfig;
import com.utp.gajog.dto.MatriculaWizardDTO;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private ApoderadoRepository apoderadoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private HorarioRepository horarioRepository;
    @Mock private CinturonRepository cinturonRepository;
    @Mock private HistorialCinturonesRepository historialCinturonesRepository;
    @Mock private TarifaRepository tarifaRepository;
    @Mock private PagoService pagoService;

    private Clock fixedClock;
    private MatriculaService matriculaService;

    private Categoria infantil;
    private Categoria juvenil;
    private Categoria adultos;

    @BeforeEach
    void setUp() {
        // Fecha fija de prueba: 21 de octubre de 2026
        fixedClock = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), TimeConfig.ZONA_LIMA);

        matriculaService = new MatriculaService(
                matriculaRepository,
                alumnoRepository,
                personaRepository,
                apoderadoRepository,
                categoriaRepository,
                horarioRepository,
                cinturonRepository,
                historialCinturonesRepository,
                tarifaRepository,
                pagoService,
                fixedClock
        );

        infantil = new Categoria();
        infantil.setId(1L);
        infantil.setNombre("Infantil");
        infantil.setEdadMinima(5);
        infantil.setEdadMaxima(11);
        infantil.setActivo(true);

        juvenil = new Categoria();
        juvenil.setId(2L);
        juvenil.setNombre("Juvenil");
        juvenil.setEdadMinima(12);
        juvenil.setEdadMaxima(17);
        juvenil.setActivo(true);

        adultos = new Categoria();
        adultos.setId(3L);
        adultos.setNombre("Adultos");
        adultos.setEdadMinima(18);
        adultos.setEdadMaxima(60);
        adultos.setActivo(true);
    }

    @Test
    @DisplayName("1. Cálculo de edad y categoría en el límite exacto de cumpleaños")
    void debeCalcularEdadYCategoriaEnLimiteDeCumpleanios() {
        // Nació el 22 de octubre de 2014 -> el 21 de octubre de 2026 aún tiene 11 años (Infantil)
        LocalDate cumpleManiana12 = LocalDate.of(2014, 10, 22);
        when(categoriaRepository.findActivaPorEdad(11)).thenReturn(Optional.of(infantil));

        assertEquals(11, matriculaService.calcularEdadExacta(cumpleManiana12));
        assertEquals("Infantil", matriculaService.calcularCategoriaAutomaticaPorFechaNacimiento(cumpleManiana12).getNombre());

        // Nació el 21 de octubre de 2014 -> hoy 21 de octubre de 2026 cumple exactamente 12 años (Juvenil)
        LocalDate cumpleHoy12 = LocalDate.of(2014, 10, 21);
        when(categoriaRepository.findActivaPorEdad(12)).thenReturn(Optional.of(juvenil));

        assertEquals(12, matriculaService.calcularEdadExacta(cumpleHoy12));
        assertEquals("Juvenil", matriculaService.calcularCategoriaAutomaticaPorFechaNacimiento(cumpleHoy12).getNombre());
    }

    @Test
    @DisplayName("2. Control de cupos: rechaza confirmar matrícula cuando el horario alcanzó su cupo máximo")
    void debeRechazarMatriculaSiCupoEstaLleno() {
        Horario horarioSinCupo = new Horario();
        horarioSinCupo.setId(50L);
        horarioSinCupo.setCategoria(infantil);
        horarioSinCupo.setDiaSemana(DiaSemana.LUNES);
        horarioSinCupo.setHoraInicio(LocalTime.of(16, 0));
        horarioSinCupo.setHoraFin(LocalTime.of(17, 30));
        horarioSinCupo.setCupoMaximo(15);

        MatriculaWizardDTO wizard = new MatriculaWizardDTO();
        wizard.setDniAlumno("74581236");
        wizard.setNombresAlumno("Valeria");
        wizard.setApellidosAlumno("Mendoza");
        wizard.setFechaNacimiento(LocalDate.of(2017, 5, 10)); // 9 años -> Infantil
        wizard.setTelefonoContacto("987654321");
        wizard.setDniApoderado("41256398");
        wizard.setNombresApoderado("Lucía");
        wizard.setApellidosApoderado("Mendoza");
        wizard.setHorarioSeleccionadoId(50L);

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.empty());
        when(personaRepository.existsByDni("74581236")).thenReturn(false);
        when(categoriaRepository.findActivaPorEdad(9)).thenReturn(Optional.of(infantil));
        when(tarifaRepository.findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(1L)).thenReturn(Optional.empty());
        when(horarioRepository.findByIdConBloqueoOptimista(50L)).thenReturn(Optional.of(horarioSinCupo));
        when(matriculaRepository.countByHorarioIdAndEstado(50L, EstadoMatricula.ACTIVA)).thenReturn(15L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                matriculaService.confirmarMatriculaWizard(wizard)
        );
        assertTrue(ex.getMessage().contains("cupos"));
    }

    @Test
    @DisplayName("3. DNI duplicado y reutilización de Persona: impide doble matrícula activa pero reutiliza Persona existente")
    void debeImpedirDobleMatriculaActivaYReutilizarPersonaExistente() {
        Alumno existenteConMatricula = new Alumno();
        existenteConMatricula.setId(99L);

        Matricula activa = new Matricula();
        activa.setCategoria(infantil);
        activa.setEstado(EstadoMatricula.ACTIVA);

        MatriculaWizardDTO wizardDuplicado = new MatriculaWizardDTO();
        wizardDuplicado.setDniAlumno("74581236");
        wizardDuplicado.setNombresAlumno("Valeria");
        wizardDuplicado.setApellidosAlumno("Mendoza");
        wizardDuplicado.setFechaNacimiento(LocalDate.of(2017, 5, 10));
        wizardDuplicado.setTelefonoContacto("987654321");
        wizardDuplicado.setDniApoderado("41256398");
        wizardDuplicado.setNombresApoderado("Lucía");
        wizardDuplicado.setApellidosApoderado("Mendoza");

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.of(existenteConMatricula));
        when(matriculaRepository.findByAlumnoIdAndEstado(99L, EstadoMatricula.ACTIVA)).thenReturn(List.of(activa));

        assertThrows(ReglaNegocioException.class, () ->
                matriculaService.validarPaso1YCalcularRevision(wizardDuplicado)
        );

        // Si la Persona existe pero no tiene matrícula activa, debe marcar personaAlumnoReutilizada = true
        when(alumnoRepository.findByPersonaDni("44556677")).thenReturn(Optional.empty());
        when(personaRepository.existsByDni("44556677")).thenReturn(true);
        when(categoriaRepository.findActivaPorEdad(20)).thenReturn(Optional.of(adultos));

        MatriculaWizardDTO wizardMayorExistente = new MatriculaWizardDTO();
        wizardMayorExistente.setDniAlumno("44556677");
        wizardMayorExistente.setNombresAlumno("Diego");
        wizardMayorExistente.setApellidosAlumno("Salazar");
        wizardMayorExistente.setFechaNacimiento(LocalDate.of(2006, 3, 15)); // 20 años
        wizardMayorExistente.setTelefonoContacto("999111222");

        matriculaService.validarPaso1YCalcularRevision(wizardMayorExistente);
        assertTrue(wizardMayorExistente.isPersonaAlumnoReutilizada());
        assertEquals("Adultos", wizardMayorExistente.getCategoriaAsignadaNombre());
    }

    @Test
    @DisplayName("4. Apoderado obligatorio para menores de 18 años y opcional para mayores de edad")
    void debeExigirApoderadoSoloParaMenoresDeEdad() {
        MatriculaWizardDTO menorSinApoderado = new MatriculaWizardDTO();
        menorSinApoderado.setDniAlumno("74581236");
        menorSinApoderado.setNombresAlumno("Valeria");
        menorSinApoderado.setApellidosAlumno("Mendoza");
        menorSinApoderado.setFechaNacimiento(LocalDate.of(2017, 5, 10)); // 9 años (menor)
        menorSinApoderado.setTelefonoContacto("987654321");

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.empty());
        when(personaRepository.existsByDni("74581236")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                matriculaService.validarPaso1YCalcularRevision(menorSinApoderado)
        );
        assertTrue(ex.getMessage().contains("apoderado"));

        // Mayor de 18 años sin apoderado pasa sin error
        MatriculaWizardDTO mayorSinApoderado = new MatriculaWizardDTO();
        mayorSinApoderado.setDniAlumno("71542019");
        mayorSinApoderado.setNombresAlumno("Carlos");
        mayorSinApoderado.setApellidosAlumno("Rojas");
        mayorSinApoderado.setFechaNacimiento(LocalDate.of(2005, 1, 10)); // 21 años (mayor)
        mayorSinApoderado.setTelefonoContacto("988777666");

        when(alumnoRepository.findByPersonaDni("71542019")).thenReturn(Optional.empty());
        when(personaRepository.existsByDni("71542019")).thenReturn(false);
        when(categoriaRepository.findActivaPorEdad(21)).thenReturn(Optional.of(adultos));

        assertDoesNotThrow(() -> matriculaService.validarPaso1YCalcularRevision(mayorSinApoderado));
        assertFalse(mayorSinApoderado.isMenorDeEdad());
        assertEquals("Adultos", mayorSinApoderado.getCategoriaAsignadaNombre());
    }
}