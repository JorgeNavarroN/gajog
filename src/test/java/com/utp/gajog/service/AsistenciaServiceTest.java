package com.utp.gajog.service;

import com.utp.gajog.config.NegocioProperties;
import com.utp.gajog.config.TimeConfig;
import com.utp.gajog.dto.ConfirmacionIngresoDniDTO;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock private SesionClaseRepository sesionClaseRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private HorarioRepository horarioRepository;
    @Mock private InstructorRepository instructorRepository;
    @Mock private AlumnoRepository alumnoRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private MensualidadRepository mensualidadRepository;

    private NegocioProperties negocioProperties;
    private Alumno alumnoMatriculado;
    private Categoria categoriaInfantil;
    private Horario horarioMiercoles16h;
    private Instructor instructor;
    private Matricula matriculaActiva;

    @BeforeEach
    void setUp() {
        negocioProperties = new NegocioProperties();
        negocioProperties.setMinutosToleranciaTardanza(10);

        Persona pAlumno = new Persona();
        pAlumno.setId(1L);
        pAlumno.setDni("74581236");
        pAlumno.setNombres("Valeria");
        pAlumno.setApellidos("Mendoza");
        pAlumno.setFechaNacimiento(LocalDate.of(2017, 5, 10));

        Cinturon cinturon = new Cinturon();
        cinturon.setNombre("Amarillo (8° Gup)");
        cinturon.setColor("AMARILLO");

        alumnoMatriculado = new Alumno();
        alumnoMatriculado.setId(10L);
        alumnoMatriculado.setPersona(pAlumno);
        alumnoMatriculado.setCinturonActual(cinturon);
        alumnoMatriculado.setEstado(EstadoAlumno.ACTIVO);

        Persona pInst = new Persona();
        pInst.setNombres("Sabonim Carlos");
        pInst.setApellidos("Mendoza");
        instructor = new Instructor();
        instructor.setId(1L);
        instructor.setPersona(pInst);

        categoriaInfantil = new Categoria();
        categoriaInfantil.setId(1L);
        categoriaInfantil.setNombre("Infantil");
        categoriaInfantil.setActivo(true);

        horarioMiercoles16h = new Horario();
        horarioMiercoles16h.setId(5L);
        horarioMiercoles16h.setCategoria(categoriaInfantil);
        horarioMiercoles16h.setDiaSemana(DiaSemana.MIERCOLES);
        horarioMiercoles16h.setHoraInicio(LocalTime.of(16, 0));
        horarioMiercoles16h.setHoraFin(LocalTime.of(17, 30));
        horarioMiercoles16h.setSala("Sala A");
        horarioMiercoles16h.setInstructor(instructor);

        Grupo grupo = new Grupo();
        grupo.setId(44L);
        grupo.setCategoria(categoriaInfantil);
        grupo.setNombre("Infantil A");
        grupo.setInstructor(instructor);
        grupo.getHorarios().add(horarioMiercoles16h);
        horarioMiercoles16h.setGrupo(grupo);

        matriculaActiva = new Matricula();
        matriculaActiva.setId(20L);
        matriculaActiva.setAlumno(alumnoMatriculado);
        matriculaActiva.setCategoria(categoriaInfantil);
        matriculaActiva.setGrupo(grupo);
        matriculaActiva.setEstado(EstadoMatricula.ACTIVA);
    }

    @Test
    @DisplayName("1. Duplicado: rechaza segundo ingreso por DNI en la misma sesión indicando la hora previa")
    void debeRechazarIngresoDuplicadoPorDniEnMismaSesion() {
        // Miércoles 21/10/2026 a las 16:05 Lima (21:05 UTC)
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T21:05:00Z"), TimeConfig.ZONA_LIMA);
        AsistenciaService service = crearServicioConClock(clock);

        SesionClase sesionHoy = new SesionClase();
        sesionHoy.setId(99L);
        sesionHoy.setHorario(horarioMiercoles16h);
        sesionHoy.setFecha(LocalDate.of(2026, 10, 21));
        sesionHoy.setHoraInicio(LocalTime.of(16, 0));
        sesionHoy.setEstado(EstadoSesionClase.EN_CURSO);

        Asistencia yaRegistrada = new Asistencia();
        yaRegistrada.setId(500L);
        yaRegistrada.setSesion(sesionHoy);
        yaRegistrada.setAlumno(alumnoMatriculado);
        yaRegistrada.setEstado(EstadoAsistencia.PRESENTE);
        yaRegistrada.setHoraIngreso(LocalDateTime.of(2026, 10, 21, 16, 1));

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.of(alumnoMatriculado));
        when(matriculaRepository.findByAlumnoIdAndEstado(10L, EstadoMatricula.ACTIVA)).thenReturn(List.of(matriculaActiva));
        when(sesionClaseRepository.findFirstByHorarioIdAndFechaOrderByIdDesc(5L, LocalDate.of(2026, 10, 21)))
                .thenReturn(Optional.of(sesionHoy));
        when(asistenciaRepository.findBySesionIdAndAlumnoId(99L, 10L)).thenReturn(Optional.of(yaRegistrada));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                service.registrarIngresoRapidoPorDni("74581236", null)
        );
        assertTrue(ex.getMessage().contains("ya registró ingreso a las 16:01"));
    }

    @Test
    @DisplayName("2. Tardanza: marca TARDANZA cuando supera la tolerancia respecto a horaInicio")
    void debeCalcularTardanzaCuandoSuperaTolerancia() {
        // Miércoles 21/10/2026 a las 16:18 Lima (21:18 UTC) -> 18 min tarde (> 10 min de tolerancia)
        Clock clockTarde = Clock.fixed(Instant.parse("2026-10-21T21:18:00Z"), TimeConfig.ZONA_LIMA);
        AsistenciaService service = crearServicioConClock(clockTarde);

        SesionClase sesionHoy = new SesionClase();
        sesionHoy.setId(99L);
        sesionHoy.setHorario(horarioMiercoles16h);
        sesionHoy.setFecha(LocalDate.of(2026, 10, 21));
        sesionHoy.setHoraInicio(LocalTime.of(16, 0));
        sesionHoy.setEstado(EstadoSesionClase.EN_CURSO);

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.of(alumnoMatriculado));
        when(matriculaRepository.findByAlumnoIdAndEstado(10L, EstadoMatricula.ACTIVA)).thenReturn(List.of(matriculaActiva));
        when(sesionClaseRepository.findFirstByHorarioIdAndFechaOrderByIdDesc(5L, LocalDate.of(2026, 10, 21)))
                .thenReturn(Optional.of(sesionHoy));
        when(asistenciaRepository.findBySesionIdAndAlumnoId(99L, 10L)).thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacionIngresoDniDTO resultado = service.registrarIngresoRapidoPorDni("74581236", null);

        assertEquals(EstadoAsistencia.TARDANZA, resultado.estadoAsistencia());
        assertEquals(18, resultado.minutosTarde());
    }

    @Test
    @DisplayName("3. Sesión inexistente: crea automáticamente la sesión del día a partir del horario del alumno")
    void debeCrearSesionAutomaticamenteSiNoExisteEnElDia() {
        // Miércoles 21/10/2026 a las 16:04 Lima (21:04 UTC) -> Dentro de tolerancia (PRESENTE)
        Clock clockPuntual = Clock.fixed(Instant.parse("2026-10-21T21:04:00Z"), TimeConfig.ZONA_LIMA);
        AsistenciaService service = crearServicioConClock(clockPuntual);

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.of(alumnoMatriculado));
        when(matriculaRepository.findByAlumnoIdAndEstado(10L, EstadoMatricula.ACTIVA)).thenReturn(List.of(matriculaActiva));
        when(sesionClaseRepository.findFirstByHorarioIdAndFechaOrderByIdDesc(5L, LocalDate.of(2026, 10, 21)))
                .thenReturn(Optional.empty());
        when(sesionClaseRepository.save(any(SesionClase.class))).thenAnswer(inv -> {
            SesionClase s = inv.getArgument(0);
            s.setId(777L);
            return s;
        });
        when(asistenciaRepository.findBySesionIdAndAlumnoId(777L, 10L)).thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacionIngresoDniDTO resultado = service.registrarIngresoRapidoPorDni("74581236", null);

        assertEquals(EstadoAsistencia.PRESENTE, resultado.estadoAsistencia());
        assertEquals(4, resultado.minutosTarde());
        verify(sesionClaseRepository, atLeastOnce()).save(any(SesionClase.class));
    }

    @Test
    @DisplayName("4. Alumno no matriculado o retirado por mora: rechaza el registro de asistencia")
    void debeRechazarAlumnoSinMatriculaActivaORetiradoPorMora() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T21:00:00Z"), TimeConfig.ZONA_LIMA);
        AsistenciaService service = crearServicioConClock(clock);

        when(alumnoRepository.findByPersonaDni("74581236")).thenReturn(Optional.of(alumnoMatriculado));
        when(matriculaRepository.findByAlumnoIdAndEstado(10L, EstadoMatricula.ACTIVA)).thenReturn(List.of());

        ReglaNegocioException exNoMatriculado = assertThrows(ReglaNegocioException.class, () ->
                service.registrarIngresoRapidoPorDni("74581236", null)
        );
        assertTrue(exNoMatriculado.getMessage().contains("matrícula ACTIVA"));

        // Caso alumno retirado por mora
        alumnoMatriculado.setEstado(EstadoAlumno.RETIRADO);
        ReglaNegocioException exRetirado = assertThrows(ReglaNegocioException.class, () ->
                service.registrarIngresoRapidoPorDni("74581236", null)
        );
        assertTrue(exRetirado.getMessage().contains("RETIRADO por mora"));
    }

    private AsistenciaService crearServicioConClock(Clock clock) {
        return new AsistenciaService(
                sesionClaseRepository,
                asistenciaRepository,
                horarioRepository,
                instructorRepository,
                alumnoRepository,
                matriculaRepository,
                mensualidadRepository,
                negocioProperties,
                clock
        );
    }
}