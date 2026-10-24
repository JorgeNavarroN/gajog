package com.utp.gajog.service;

import com.utp.gajog.dto.*;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.*;

@Service
public class MatriculaService {

    private static final Logger log = LoggerFactory.getLogger(MatriculaService.class);

    private final MatriculaRepository matriculaRepository;
    private final AlumnoRepository alumnoRepository;
    private final PersonaRepository personaRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioRepository horarioRepository;
    private final CinturonRepository cinturonRepository;
    private final HistorialCinturonesRepository historialCinturonesRepository;
    private final TarifaRepository tarifaRepository;
    private final PagoService pagoService;
    private final Clock clock;

    public MatriculaService(
            MatriculaRepository matriculaRepository,
            AlumnoRepository alumnoRepository,
            PersonaRepository personaRepository,
            ApoderadoRepository apoderadoRepository,
            CategoriaRepository categoriaRepository,
            HorarioRepository horarioRepository,
            CinturonRepository cinturonRepository,
            HistorialCinturonesRepository historialCinturonesRepository,
            TarifaRepository tarifaRepository,
            PagoService pagoService,
            Clock clock
    ) {
        this.matriculaRepository = matriculaRepository;
        this.alumnoRepository = alumnoRepository;
        this.personaRepository = personaRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.horarioRepository = horarioRepository;
        this.cinturonRepository = cinturonRepository;
        this.historialCinturonesRepository = historialCinturonesRepository;
        this.tarifaRepository = tarifaRepository;
        this.pagoService = pagoService;
        this.clock = clock;
    }

    // ==================== CÁLCULO DE EDAD Y CATEGORÍA AUTOMÁTICA ====================

    public int calcularEdadExacta(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ReglaNegocioException("La fecha de nacimiento es obligatoria para calcular la categoría.");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (fechaNacimiento.isAfter(hoy)) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser una fecha futura.");
        }
        return Period.between(fechaNacimiento, hoy).getYears();
    }

    @Transactional(readOnly = true)
    public Categoria calcularCategoriaAutomaticaPorFechaNacimiento(LocalDate fechaNacimiento) {
        int edad = calcularEdadExacta(fechaNacimiento);
        return categoriaRepository.findActivaPorEdad(edad)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe una categoría activa para la edad calculada de " + edad + " años."
                ));
    }

    // ==================== WIZARD DE MATRÍCULA EN 3 PASOS ====================

    @Transactional(readOnly = true)
    public MatriculaWizardDTO inicializarWizardDesdeAlumnoExistente(Long alumnoId, Long matriculaPendienteId) {
        MatriculaWizardDTO wizard = new MatriculaWizardDTO();
        if (matriculaPendienteId != null) {
            Matricula m = matriculaRepository.findById(matriculaPendienteId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada"));
            wizard.setMatriculaPendienteId(m.getId());
            alumnoId = m.getAlumno().getId();
        }

        if (alumnoId != null) {
            Alumno a = alumnoRepository.findById(alumnoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
            Persona p = a.getPersona();
            wizard.setAlumnoExistenteId(a.getId());
            wizard.setDniAlumno(p.getDni());
            wizard.setNombresAlumno(p.getNombres());
            wizard.setApellidosAlumno(p.getApellidos());
            wizard.setFechaNacimiento(p.getFechaNacimiento());
            wizard.setTelefonoContacto(p.getTelefono() != null ? p.getTelefono() : "");
            wizard.setCorreoAlumno(p.getCorreo());
            if (a.getCinturonActual() != null) {
                wizard.setCinturonInicialId(a.getCinturonActual().getId());
            }
            if (a.getApoderado() != null) {
                Persona pa = a.getApoderado().getPersona();
                wizard.setApoderadoExistenteId(a.getApoderado().getId());
                wizard.setDniApoderado(pa.getDni());
                wizard.setNombresApoderado(pa.getNombres());
                wizard.setApellidosApoderado(pa.getApellidos());
                wizard.setTelefonoApoderado(pa.getTelefono());
            }
        }
        return wizard;
    }

    /**
     * Paso 1 -> Paso 2: Valida DNI (8 dígitos), verifica si ya tiene matrícula activa,
     * exige apoderado si es menor de 18 años, y calcula automáticamente edad y categoría.
     */
    @Transactional(readOnly = true)
    public void validarPaso1YCalcularRevision(MatriculaWizardDTO wizard) {
        if (wizard.getDniAlumno() == null || !wizard.getDniAlumno().trim().matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI del alumno debe contener exactamente 8 dígitos numéricos.");
        }

        String dniLimpio = wizard.getDniAlumno().trim();
        wizard.setDniAlumno(dniLimpio);

        Optional<Alumno> alumnoPorDni = alumnoRepository.findByPersonaDni(dniLimpio);
        if (alumnoPorDni.isPresent()) {
            Alumno existente = alumnoPorDni.get();
            if (wizard.getAlumnoExistenteId() == null) {
                wizard.setAlumnoExistenteId(existente.getId());
            }
            List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(existente.getId(), EstadoMatricula.ACTIVA);
            if (!activas.isEmpty() && wizard.getMatriculaPendienteId() == null) {
                throw new ReglaNegocioException(
                        "El alumno con DNI " + dniLimpio + " ya posee una matrícula ACTIVA en la categoría "
                        + activas.getFirst().getCategoria().getNombre() + "."
                );
            }
        }

        boolean personaExiste = personaRepository.existsByDni(dniLimpio);
        wizard.setPersonaAlumnoReutilizada(personaExiste);

        int edad = calcularEdadExacta(wizard.getFechaNacimiento());
        boolean esMenor = edad < 18;
        wizard.setEdadCalculada(edad);
        wizard.setMenorDeEdad(esMenor);

        if (esMenor) {
            boolean tieneApoderadoSeleccionado = wizard.getApoderadoExistenteId() != null;
            boolean tieneDatosNuevoApoderado = wizard.getDniApoderado() != null
                    && wizard.getDniApoderado().trim().matches("\\d{8}")
                    && wizard.getNombresApoderado() != null && !wizard.getNombresApoderado().isBlank()
                    && wizard.getApellidosApoderado() != null && !wizard.getApellidosApoderado().isBlank();

            if (!tieneApoderadoSeleccionado && !tieneDatosNuevoApoderado) {
                throw new ReglaNegocioException(
                        "El alumno tiene " + edad + " años (menor de edad). Es obligatorio registrar los datos del apoderado (DNI de 8 dígitos, nombres y apellidos)."
                );
            }
            if (!tieneApoderadoSeleccionado && dniLimpio.equals(wizard.getDniApoderado().trim())) {
                throw new ReglaNegocioException("El DNI del apoderado no puede ser igual al DNI del alumno menor de edad.");
            }
        }

        Categoria categoriaAutomatica = calcularCategoriaAutomaticaPorFechaNacimiento(wizard.getFechaNacimiento());
        wizard.setCategoriaAsignadaId(categoriaAutomatica.getId());
        wizard.setCategoriaAsignadaNombre(categoriaAutomatica.getNombre());
        wizard.setCategoriaRangoTexto(categoriaAutomatica.getEdadMinima() + " a " + categoriaAutomatica.getEdadMaxima() + " años");

        if (wizard.getFiltroCategoriaPaso3Id() == null) {
            wizard.setFiltroCategoriaPaso3Id(categoriaAutomatica.getId());
        }

        BigDecimal tarifa = tarifaRepository
                .findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(categoriaAutomatica.getId())
                .map(Tarifa::getMontoVigente)
                .orElse(new BigDecimal("100.00"));
        wizard.setTarifaMensualCategoria(tarifa);
        wizard.setPaso1Completado(true);
    }

    /**
     * Paso 3: Lista horarios disponibles con filtros por categoría, día de semana y rango/turno horario.
     */
    @Transactional(readOnly = true)
    public List<HorarioDisponibleDTO> buscarHorariosParaPaso3(
            Long categoriaId,
            DiaSemana diaSemana,
            String turno
    ) {
        List<Horario> horarios = horarioRepository.buscarHorariosConDetalle(categoriaId, diaSemana);
        List<HorarioDisponibleDTO> resultado = new ArrayList<>();

        for (Horario h : horarios) {
            if (turno != null && !turno.isBlank()) {
                boolean esManiana = h.getHoraInicio().isBefore(LocalTime.of(12, 0));
                boolean esTarde = !esManiana && h.getHoraInicio().isBefore(LocalTime.of(18, 0));
                boolean esNoche = !esManiana && !esTarde;

                if ("MANIANA".equalsIgnoreCase(turno) && !esManiana) continue;
                if ("TARDE".equalsIgnoreCase(turno) && !esTarde) continue;
                if ("NOCHE".equalsIgnoreCase(turno) && !esNoche) continue;
            }

            long inscritos = matriculaRepository.countByHorarioIdAndEstado(h.getId(), EstadoMatricula.ACTIVA);
            int cupoMax = h.getCupoMaximo() != null ? h.getCupoMaximo() : 20;
            int disponibles = Math.max(0, cupoMax - (int) inscritos);

            String nombreInstructor = (h.getInstructor() != null && h.getInstructor().getPersona() != null)
                    ? h.getInstructor().getPersona().getNombres() + " " + h.getInstructor().getPersona().getApellidos()
                    : "Sabonim por asignar";

            resultado.add(new HorarioDisponibleDTO(
                    h.getId(),
                    h.getCategoria().getId(),
                    h.getCategoria().getNombre(),
                    h.getDiaSemana(),
                    h.getHoraInicio(),
                    h.getHoraFin(),
                    h.getSala(),
                    nombreInstructor,
                    cupoMax,
                    inscritos,
                    disponibles
            ));
        }
        return resultado;
    }

    /**
     * Paso 3 Final: Confirma la matrícula en una sola transacción atómica creando o reutilizando
     * Persona, Apoderado, Alumno, Matrícula y la primera Mensualidad con validación de cupo y @Version.
     */
    @Transactional
    public Matricula confirmarMatriculaWizard(MatriculaWizardDTO wizard) {
        validarPaso1YCalcularRevision(wizard);

        if (wizard.getHorarioSeleccionadoId() == null) {
            throw new ReglaNegocioException("Debe seleccionar un horario disponible para confirmar la matrícula.");
        }

        try {
            Horario horario = horarioRepository.findByIdConBloqueoOptimista(wizard.getHorarioSeleccionadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El horario seleccionado no existe"));

            if (!horario.getCategoria().getId().equals(wizard.getCategoriaAsignadaId())) {
                throw new ReglaNegocioException(
                        "El horario seleccionado (" + horario.getCategoria().getNombre()
                        + ") no corresponde a la categoría asignada por edad (" + wizard.getCategoriaAsignadaNombre() + ")."
                );
            }

            long inscritosActivos = matriculaRepository.countByHorarioIdAndEstado(horario.getId(), EstadoMatricula.ACTIVA);
            if (horario.getCupoMaximo() != null && inscritosActivos >= horario.getCupoMaximo()) {
                throw new ReglaNegocioException(
                        "El horario seleccionado ya no cuenta con cupos disponibles ("
                        + inscritosActivos + "/" + horario.getCupoMaximo() + ")."
                );
            }

            // 1. Crear o reutilizar Apoderado (si corresponde)
            Apoderado apoderado = resolverApoderadoWizard(wizard);

            // 2. Crear o reutilizar Persona del Alumno
            String dniAlumno = wizard.getDniAlumno().trim();
            Persona personaAlumno = personaRepository.findByDni(dniAlumno)
                    .map(p -> {
                        p.setNombres(wizard.getNombresAlumno().trim());
                        p.setApellidos(wizard.getApellidosAlumno().trim());
                        p.setFechaNacimiento(wizard.getFechaNacimiento());
                        p.setTelefono(wizard.getTelefonoContacto().trim());
                        if (wizard.getCorreoAlumno() != null && !wizard.getCorreoAlumno().isBlank()) {
                            p.setCorreo(wizard.getCorreoAlumno().trim());
                        }
                        return personaRepository.save(p);
                    })
                    .orElseGet(() -> {
                        Persona nueva = new Persona();
                        nueva.setDni(dniAlumno);
                        nueva.setNombres(wizard.getNombresAlumno().trim());
                        nueva.setApellidos(wizard.getApellidosAlumno().trim());
                        nueva.setFechaNacimiento(wizard.getFechaNacimiento());
                        nueva.setTelefono(wizard.getTelefonoContacto().trim());
                        nueva.setCorreo(wizard.getCorreoAlumno());
                        return personaRepository.save(nueva);
                    });

            // 3. Crear o reutilizar Alumno
            LocalDate hoy = LocalDate.now(clock);
            Alumno alumno = alumnoRepository.findByPersonaDni(dniAlumno)
                    .map(a -> {
                        a.setEstado(EstadoAlumno.ACTIVO);
                        if (apoderado != null) {
                            a.setApoderado(apoderado);
                        }
                        return alumnoRepository.save(a);
                    })
                    .orElseGet(() -> {
                        Cinturon cinturonInicial = (wizard.getCinturonInicialId() != null)
                                ? cinturonRepository.findById(wizard.getCinturonInicialId())
                                        .orElseGet(() -> cinturonRepository.findByOrden(1).orElse(null))
                                : cinturonRepository.findByOrden(1).orElse(null);

                        Alumno nuevo = new Alumno();
                        nuevo.setPersona(personaAlumno);
                        nuevo.setApoderado(apoderado);
                        nuevo.setCinturonActual(cinturonInicial);
                        nuevo.setFechaIngreso(hoy);
                        nuevo.setEstado(EstadoAlumno.ACTIVO);
                        Alumno guardado = alumnoRepository.save(nuevo);

                        if (cinturonInicial != null) {
                            HistorialCinturones hist = new HistorialCinturones();
                            hist.setAlumno(guardado);
                            hist.setCinturon(cinturonInicial);
                            hist.setFechaObtencion(hoy);
                            historialCinturonesRepository.save(hist);
                        }
                        return guardado;
                    });

            // Cancelar cualquier otra matrícula activa previa si se está haciendo cambio de grupo
            List<Matricula> activasPrevias = matriculaRepository.findByAlumnoIdAndEstado(alumno.getId(), EstadoMatricula.ACTIVA);
            for (Matricula previa : activasPrevias) {
                previa.setEstado(EstadoMatricula.FINALIZADA);
                matriculaRepository.save(previa);
            }

            // 4. Crear o activar Matrícula
            Categoria categoriaAsignada = horario.getCategoria();
            Matricula matricula;
            if (wizard.getMatriculaPendienteId() != null) {
                matricula = matriculaRepository.findById(wizard.getMatriculaPendienteId())
                        .orElseGet(Matricula::new);
            } else {
                matricula = new Matricula();
            }

            matricula.setAlumno(alumno);
            matricula.setCategoria(categoriaAsignada);
            matricula.setHorario(horario);
            matricula.setFechaMatricula(hoy);
            matricula.setEstado(EstadoMatricula.ACTIVA);

            Matricula matriculaGuardada = matriculaRepository.saveAndFlush(matricula);

            // 5. Generar automáticamente la primera mensualidad
            pagoService.generarPrimeraMensualidadPorMatricula(matriculaGuardada);

            log.info("Matrícula confirmada exitosamente (ID {}) para alumno {} en categoría {} / horario {}.",
                    matriculaGuardada.getId(), personaAlumno.getDni(), categoriaAsignada.getNombre(), horario.getId());
            return matriculaGuardada;

        } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
            throw new ReglaNegocioException(
                    "Otro usuario ocupó simultáneamente el último cupo de este horario. Por favor, elija otro horario."
            );
        }
    }

    // ==================== DETECCIÓN Y REASIGNACIÓN DE CATEGORÍA POR EDAD ====================

    @Transactional(readOnly = true)
    public List<DesfaseCategoriaDTO> detectarAlumnosConDesfaseDeEdad() {
        List<Matricula> activas = matriculaRepository.findMatriculasActivasConAlumnoYCategoria(EstadoMatricula.ACTIVA);
        LocalDate hoy = LocalDate.now(clock);
        List<DesfaseCategoriaDTO> desfases = new ArrayList<>();

        for (Matricula m : activas) {
            LocalDate fn = m.getAlumno().getPersona().getFechaNacimiento();
            if (fn == null) continue;

            int edad = Period.between(fn, hoy).getYears();
            Categoria catActual = m.getCategoria();

            if (edad < catActual.getEdadMinima() || edad > catActual.getEdadMaxima()) {
                Optional<Categoria> sugerida = categoriaRepository.findActivaPorEdad(edad);
                desfases.add(new DesfaseCategoriaDTO(
                        m.getAlumno().getId(),
                        m.getId(),
                        m.getAlumno().getPersona().getNombres() + " " + m.getAlumno().getPersona().getApellidos(),
                        edad,
                        catActual.getNombre(),
                        catActual.getEdadMinima(),
                        catActual.getEdadMaxima(),
                        sugerida.map(Categoria::getId).orElse(null),
                        sugerida.map(Categoria::getNombre).orElse("Fuera de rango")
                ));
            }
        }
        return desfases;
    }

    @Transactional
    public Matricula reasignarGrupoPorEdad(Long alumnoId, Long nuevoHorarioId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        Horario nuevoHorario = horarioRepository.findByIdConBloqueoOptimista(nuevoHorarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado"));

        Categoria nuevaCategoria = nuevoHorario.getCategoria();
        int edad = calcularEdadExacta(alumno.getPersona().getFechaNacimiento());
        if (edad < nuevaCategoria.getEdadMinima() || edad > nuevaCategoria.getEdadMaxima()) {
            throw new ReglaNegocioException(
                    "La edad actual del alumno (" + edad + " años) no corresponde a la categoría del horario elegido ("
                    + nuevaCategoria.getNombre() + ": " + nuevaCategoria.getEdadMinima() + "-" + nuevaCategoria.getEdadMaxima() + " años)."
            );
        }

        long inscritos = matriculaRepository.countByHorarioIdAndEstado(nuevoHorario.getId(), EstadoMatricula.ACTIVA);
        if (nuevoHorario.getCupoMaximo() != null && inscritos >= nuevoHorario.getCupoMaximo()) {
            throw new ReglaNegocioException("El horario seleccionado ya alcanzó su cupo máximo.");
        }

        List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMatricula.ACTIVA);
        for (Matricula m : activas) {
            m.setEstado(EstadoMatricula.FINALIZADA);
            matriculaRepository.save(m);
        }

        Matricula nueva = new Matricula();
        nueva.setAlumno(alumno);
        nueva.setCategoria(nuevaCategoria);
        nueva.setHorario(nuevoHorario);
        nueva.setFechaMatricula(LocalDate.now(clock));
        nueva.setEstado(EstadoMatricula.ACTIVA);

        return matriculaRepository.save(nueva);
    }

    // ==================== LISTADO Y OPERACIONES ESTÁNDAR ====================

    @Transactional(readOnly = true)
    public Page<Matricula> buscarMatriculasPaginadas(FiltroMatriculaDTO filtro, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(5, size)));
        return matriculaRepository.buscarMatriculasFiltradas(
                filtro.estadoFiltro(),
                filtro.categoriaId(),
                filtro.horarioId(),
                filtro.q(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarMatriculasPorPestania() {
        Map<String, Long> conteos = new LinkedHashMap<>();
        conteos.put("TODAS", matriculaRepository.count());
        conteos.put("PENDIENTES", matriculaRepository.countByEstado(EstadoMatricula.PENDIENTE));
        conteos.put("ACTIVAS", matriculaRepository.countByEstado(EstadoMatricula.ACTIVA));
        conteos.put("FINALIZADAS", matriculaRepository.countByEstado(EstadoMatricula.FINALIZADA));
        conteos.put("CANCELADAS", matriculaRepository.countByEstado(EstadoMatricula.CANCELADA));
        return conteos;
    }

    @Transactional
    public Matricula registrarMatricula(Long alumnoId, Long categoriaId, Long horarioId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new ReglaNegocioException("Solo se pueden matricular alumnos en estado ACTIVO");
        }

        Horario horario = horarioRepository.findByIdConBloqueoOptimista(horarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado"));

        Categoria categoria;
        if (categoriaId != null) {
            categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));
            if (!horario.getCategoria().getId().equals(categoria.getId())) {
                throw new ReglaNegocioException("El horario seleccionado no pertenece a la categoría indicada");
            }
        } else {
            categoria = horario.getCategoria();
        }

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ReglaNegocioException("La categoría seleccionada no está activa");
        }

        long inscritosActivos = matriculaRepository.countByHorarioIdAndEstado(horario.getId(), EstadoMatricula.ACTIVA);
        if (horario.getCupoMaximo() != null && inscritosActivos >= horario.getCupoMaximo()) {
            throw new ReglaNegocioException("El horario seleccionado ya alcanzó su cupo máximo (" + horario.getCupoMaximo() + ")");
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaNacimiento = alumno.getPersona().getFechaNacimiento();
        if (fechaNacimiento != null) {
            int edad = Period.between(fechaNacimiento, hoy).getYears();
            if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) {
                throw new ReglaNegocioException(
                        "La edad del alumno (" + edad + " años) no está en el rango de la categoría "
                        + categoria.getNombre() + " (" + categoria.getEdadMinima() + " - " + categoria.getEdadMaxima() + " años)"
                );
            }
        }

        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setCategoria(categoria);
        matricula.setHorario(horario);
        matricula.setFechaMatricula(hoy);
        matricula.setEstado(EstadoMatricula.ACTIVA);

        Matricula guardada = matriculaRepository.save(matricula);
        pagoService.generarPrimeraMensualidadPorMatricula(guardada);

        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarPorAlumno(Long alumnoId) {
        return matriculaRepository.findByAlumnoId(alumnoId);
    }

    @Transactional
    public Matricula cambiarEstado(Long matriculaId, EstadoMatricula nuevoEstado) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada"));
        matricula.setEstado(nuevoEstado);
        return matriculaRepository.save(matricula);
    }

    private Apoderado resolverApoderadoWizard(MatriculaWizardDTO wizard) {
        if (wizard.getApoderadoExistenteId() != null) {
            return apoderadoRepository.findById(wizard.getApoderadoExistenteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Apoderado seleccionado no encontrado"));
        }
        if (wizard.getDniApoderado() == null || wizard.getDniApoderado().isBlank()) {
            return null;
        }

        String dniAp = wizard.getDniApoderado().trim();
        Optional<Apoderado> apExistente = apoderadoRepository.findByPersonaDni(dniAp);
        if (apExistente.isPresent()) {
            return apExistente.get();
        }

        Persona personaAp = personaRepository.findByDni(dniAp)
                .map(p -> {
                    if (wizard.getNombresApoderado() != null && !wizard.getNombresApoderado().isBlank()) {
                        p.setNombres(wizard.getNombresApoderado().trim());
                    }
                    if (wizard.getApellidosApoderado() != null && !wizard.getApellidosApoderado().isBlank()) {
                        p.setApellidos(wizard.getApellidosApoderado().trim());
                    }
                    if (wizard.getTelefonoApoderado() != null && !wizard.getTelefonoApoderado().isBlank()) {
                        p.setTelefono(wizard.getTelefonoApoderado().trim());
                    }
                    return personaRepository.save(p);
                })
                .orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setDni(dniAp);
                    nueva.setNombres(wizard.getNombresApoderado().trim());
                    nueva.setApellidos(wizard.getApellidosApoderado().trim());
                    nueva.setTelefono(wizard.getTelefonoApoderado() != null ? wizard.getTelefonoApoderado().trim() : wizard.getTelefonoContacto());
                    return personaRepository.save(nueva);
                });

        Apoderado nuevoAp = new Apoderado();
        nuevoAp.setPersona(personaAp);
        return apoderadoRepository.save(nuevoAp);
    }
}