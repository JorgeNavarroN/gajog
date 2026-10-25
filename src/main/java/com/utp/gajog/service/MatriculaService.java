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
    private final GrupoRepository grupoRepository;
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
            GrupoRepository grupoRepository,
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
        this.grupoRepository = grupoRepository;
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
    public List<HorarioDisponibleDTO> buscarHorariosParaPaso3(Long categoriaId, DiaSemana diaSemana, String turno) {
        List<Horario> filtrados = horarioRepository.buscarHorariosConDetalle(categoriaId, diaSemana);
        Map<Long, Grupo> grupos = new LinkedHashMap<>();
        for (Horario horario : filtrados) {
            if (!Boolean.TRUE.equals(horario.getActivo()) || !Boolean.TRUE.equals(horario.getGrupo().getActivo())) continue;
            if (turno != null && !turno.isBlank()) {
                LocalTime hora = horario.getHoraInicio();
                boolean maniana = hora.isBefore(LocalTime.NOON);
                boolean tarde = !maniana && hora.isBefore(LocalTime.of(18, 0));
                if ("MANIANA".equalsIgnoreCase(turno) && !maniana) continue;
                if ("TARDE".equalsIgnoreCase(turno) && !tarde) continue;
                if ("NOCHE".equalsIgnoreCase(turno) && (maniana || tarde)) continue;
            }
            grupos.putIfAbsent(horario.getGrupo().getId(), horario.getGrupo());
        }

        List<HorarioDisponibleDTO> resultado = new ArrayList<>();
        for (Grupo grupo : grupos.values()) {
            List<Horario> horarios = grupo.getHorarios().stream()
                    .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                    .sorted(Comparator.comparing(Horario::getDiaSemana).thenComparing(Horario::getHoraInicio))
                    .toList();
            if (horarios.isEmpty()) continue;
            Horario primero = horarios.getFirst();
            long inscritos = matriculaRepository.countByGrupoIdAndEstado(grupo.getId(), EstadoMatricula.ACTIVA);
            int capacidad = grupo.getCupoMaximo();
            int disponibles = Math.max(0, capacidad - (int) inscritos);
            Instructor instructor = grupo.getInstructor();
            String nombreInstructor = instructor != null && instructor.getPersona() != null
                    ? instructor.getPersona().getNombres() + " " + instructor.getPersona().getApellidos()
                    : "Instructor por asignar";
            String resumenHorarios = horarios.stream()
                    .map(h -> h.getDiaSemana() + " " + h.getHoraInicio() + " - " + h.getHoraFin()
                            + " (" + h.getSala() + ")")
                    .collect(java.util.stream.Collectors.joining(" · "));
            resultado.add(new HorarioDisponibleDTO(
                    grupo.getId(), grupo.getCategoria().getId(), grupo.getCategoria().getNombre(),
                    primero.getDiaSemana(), primero.getHoraInicio(), primero.getHoraFin(),
                    primero.getSala(), nombreInstructor, capacidad, inscritos, disponibles,
                    grupo.getNombre(), resumenHorarios
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

        if (wizard.getGrupoSeleccionadoId() == null) {
            throw new ReglaNegocioException("Debe seleccionar un grupo disponible para confirmar la matrícula.");
        }

        try {
            Grupo grupo = grupoRepository.findByIdParaMatricula(wizard.getGrupoSeleccionadoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("El grupo seleccionado no existe"));
            if (!Boolean.TRUE.equals(grupo.getActivo()) || grupo.getHorarios().stream().noneMatch(h -> Boolean.TRUE.equals(h.getActivo()))) {
                throw new ReglaNegocioException("El grupo seleccionado no tiene horarios activos.");
            }

            if (!grupo.getCategoria().getId().equals(wizard.getCategoriaAsignadaId())) {
                throw new ReglaNegocioException(
                        "El grupo seleccionado (" + grupo.getCategoria().getNombre()
                        + ") no corresponde a la categoría asignada por edad (" + wizard.getCategoriaAsignadaNombre() + ")."
                );
            }

            long inscritosActivos = matriculaRepository.countByGrupoIdAndEstado(grupo.getId(), EstadoMatricula.ACTIVA);
            if (inscritosActivos >= grupo.getCupoMaximo()) {
                throw new ReglaNegocioException(
                        "El grupo seleccionado ya no cuenta con cupos disponibles ("
                        + inscritosActivos + "/" + grupo.getCupoMaximo() + ")."
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
            Categoria categoriaAsignada = grupo.getCategoria();
            Matricula matricula;
            if (wizard.getMatriculaPendienteId() != null) {
                matricula = matriculaRepository.findById(wizard.getMatriculaPendienteId())
                        .orElseGet(Matricula::new);
            } else {
                matricula = new Matricula();
            }

            matricula.setAlumno(alumno);
            matricula.setGrupo(grupo);
            matricula.setFechaMatricula(hoy);
            matricula.setEstado(EstadoMatricula.ACTIVA);

            Matricula matriculaGuardada = matriculaRepository.saveAndFlush(matricula);

            // 5. Generar automáticamente la primera mensualidad
            pagoService.generarPrimeraMensualidadPorMatricula(matriculaGuardada);

            log.info("Matrícula confirmada exitosamente (ID {}) para alumno {} en categoría {} / grupo {}.",
                    matriculaGuardada.getId(), personaAlumno.getDni(), categoriaAsignada.getNombre(), grupo.getId());
            return matriculaGuardada;

        } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
            throw new ReglaNegocioException(
                    "Otro usuario ocupó simultáneamente el último cupo de este grupo. Por favor, elija otro grupo."
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
    public Matricula reasignarGrupoPorEdad(Long alumnoId, Long nuevoGrupoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
        Grupo grupo = grupoRepository.findByIdParaMatricula(nuevoGrupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        if (!Boolean.TRUE.equals(grupo.getActivo()) || grupo.getHorarios().isEmpty()) {
            throw new ReglaNegocioException("El grupo seleccionado no está disponible.");
        }
        Categoria categoria = grupo.getCategoria();
        int edad = calcularEdadExacta(alumno.getPersona().getFechaNacimiento());
        if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) {
            throw new ReglaNegocioException("La edad del alumno no corresponde a la categoría del grupo seleccionado.");
        }
        long inscritos = matriculaRepository.countByGrupoIdAndEstado(grupo.getId(), EstadoMatricula.ACTIVA);
        if (inscritos >= grupo.getCupoMaximo()) {
            throw new ReglaNegocioException("El grupo seleccionado ya alcanzó su cupo máximo.");
        }
        for (Matricula anterior : matriculaRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMatricula.ACTIVA)) {
            anterior.setEstado(EstadoMatricula.FINALIZADA);
            matriculaRepository.save(anterior);
        }
        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setGrupo(grupo);
        matricula.setFechaMatricula(LocalDate.now(clock));
        matricula.setEstado(EstadoMatricula.ACTIVA);
        return matriculaRepository.save(matricula);
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
    public Matricula registrarMatricula(Long alumnoId, Long categoriaId, Long grupoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));
        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new ReglaNegocioException("Solo se pueden matricular alumnos ACTIVO.");
        }
        Grupo grupo = grupoRepository.findByIdParaMatricula(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        Categoria categoria = grupo.getCategoria();
        if (categoriaId != null && !categoria.getId().equals(categoriaId)) {
            throw new ReglaNegocioException("El grupo no pertenece a la categoría indicada.");
        }
        if (!Boolean.TRUE.equals(grupo.getActivo()) || !Boolean.TRUE.equals(categoria.getActivo())
                || grupo.getHorarios().stream().noneMatch(h -> Boolean.TRUE.equals(h.getActivo()))) {
            throw new ReglaNegocioException("El grupo no está disponible.");
        }
        if (matriculaRepository.countByGrupoIdAndEstado(grupoId, EstadoMatricula.ACTIVA) >= grupo.getCupoMaximo()) {
            throw new ReglaNegocioException("El grupo seleccionado no tiene cupos libres.");
        }
        if (!matriculaRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMatricula.ACTIVA).isEmpty()) {
            throw new ReglaNegocioException("El alumno ya tiene una matrícula activa.");
        }
        LocalDate nacimiento = alumno.getPersona().getFechaNacimiento();
        if (nacimiento != null) {
            int edad = calcularEdadExacta(nacimiento);
            if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) {
                throw new ReglaNegocioException("La edad del alumno no corresponde a la categoría del grupo.");
            }
        }
        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setGrupo(grupo);
        matricula.setFechaMatricula(LocalDate.now(clock));
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