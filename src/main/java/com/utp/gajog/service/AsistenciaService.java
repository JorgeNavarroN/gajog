package com.utp.gajog.service;

import com.utp.gajog.config.NegocioProperties;
import com.utp.gajog.dto.*;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaService.class);
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SesionClaseRepository sesionClaseRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final HorarioRepository horarioRepository;
    private final InstructorRepository instructorRepository;
    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final NegocioProperties negocioProperties;
    private final Clock clock;

    public AsistenciaService(
            SesionClaseRepository sesionClaseRepository,
            AsistenciaRepository asistenciaRepository,
            HorarioRepository horarioRepository,
            InstructorRepository instructorRepository,
            AlumnoRepository alumnoRepository,
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository,
            NegocioProperties negocioProperties,
            Clock clock
    ) {
        this.sesionClaseRepository = sesionClaseRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.horarioRepository = horarioRepository;
        this.instructorRepository = instructorRepository;
        this.alumnoRepository = alumnoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.negocioProperties = negocioProperties;
        this.clock = clock;
    }

    // ==================== 1. REGISTRO RÁPIDO POR DNI ====================

    @Transactional
    public ConfirmacionIngresoDniDTO registrarIngresoRapidoPorDni(String dni, Long horarioSeleccionadoId) {
        if (dni == null || !dni.trim().matches("\\d{8}")) {
            throw new ReglaNegocioException("Ingrese un número de DNI válido de 8 dígitos.");
        }

        String dniLimpio = dni.trim();
        Alumno alumno = alumnoRepository.findByPersonaDni(dniLimpio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un alumno registrado con el DNI " + dniLimpio + "."));

        // Bloqueo si el alumno tiene retiro confirmado por mora o está inactivo
        if (alumno.getEstado() == EstadoAlumno.RETIRADO) {
            throw new ReglaNegocioException(
                    "Acceso bloqueado: El alumno " + alumno.getPersona().getNombres() + " " + alumno.getPersona().getApellidos()
                    + " se encuentra en estado RETIRADO por mora. Debe regularizar su pago en administración para reincorporarse."
            );
        }
        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new ReglaNegocioException(
                    "El alumno " + alumno.getPersona().getNombres() + " figura como " + alumno.getEstado() + " y no puede registrar asistencia."
            );
        }

        List<Matricula> matriculasActivas = matriculaRepository.findByAlumnoIdAndEstado(alumno.getId(), EstadoMatricula.ACTIVA);
        if (matriculasActivas.isEmpty()) {
            throw new ReglaNegocioException(
                    "El alumno " + alumno.getPersona().getNombres() + " " + alumno.getPersona().getApellidos()
                    + " no cuenta con una matrícula ACTIVA."
            );
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime ahora = LocalDateTime.now(clock);
        DiaSemana diaHoy = mapearDiaSemana(hoy.getDayOfWeek());

        // Determinar el horario de la sesión del día
        Horario horarioElegido = resolverHorarioParaIngresoDni(matriculasActivas, diaHoy, horarioSeleccionadoId);

        if (horarioElegido == null) {
            // Hay más de un horario candidato y el operador debe elegir cuál corresponde
            List<OpcionHorarioAsistenciaDTO> opciones = matriculasActivas.stream()
                    .map(Matricula::getHorario)
                    .map(h -> new OpcionHorarioAsistenciaDTO(
                            h.getId(),
                            h.getCategoria().getNombre(),
                            h.getDiaSemana(),
                            h.getHoraInicio(),
                            h.getHoraFin(),
                            h.getSala()
                    ))
                    .toList();

            return new ConfirmacionIngresoDniDTO(
                    true,
                    dniLimpio,
                    alumno.getId(),
                    alumno.getPersona().getNombres() + " " + alumno.getPersona().getApellidos(),
                    matriculasActivas.getFirst().getCategoria().getNombre(),
                    alumno.getCinturonActual() != null ? alumno.getCinturonActual().getNombre() : "Sin grado",
                    alumno.getCinturonActual() != null ? alumno.getCinturonActual().getColor() : "BLANCO",
                    "Seleccione el turno del alumno",
                    null,
                    null,
                    0,
                    "-",
                    "warning",
                    false,
                    opciones
            );
        }

        // Obtener la sesión del día o crearla automáticamente si aún no existe
        SesionClase sesion = obtenerOCrearSesionDeHorarioEnFecha(horarioElegido, hoy);

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new ReglaNegocioException("La sesión de hoy para el grupo " + horarioElegido.getCategoria().getNombre() + " se encuentra CANCELADA.");
        }

        // Evitar duplicados respetando la unicidad sesión-alumno
        Optional<Asistencia> registroPrevio = asistenciaRepository.findBySesionIdAndAlumnoId(sesion.getId(), alumno.getId());
        if (registroPrevio.isPresent()) {
            Asistencia prev = registroPrevio.get();
            if (prev.getHoraIngreso() != null && (prev.getEstado() == EstadoAsistencia.PRESENTE || prev.getEstado() == EstadoAsistencia.TARDANZA)) {
                String horaTexto = prev.getHoraIngreso().format(FORMATO_HORA);
                throw new ReglaNegocioException(
                        "El alumno " + alumno.getPersona().getNombres() + " " + alumno.getPersona().getApellidos()
                        + " ya registró ingreso a las " + horaTexto + "."
                );
            }
        }

        // Calcular PRESENTE o TARDANZA según horaInicio y tolerancia configurable
        int minutosTarde = calcularMinutosTarde(sesion, ahora.toLocalTime());
        int tolerancia = Math.max(0, negocioProperties.getMinutosToleranciaTardanza());
        EstadoAsistencia estadoCalculado = (minutosTarde > tolerancia)
                ? EstadoAsistencia.TARDANZA
                : EstadoAsistencia.PRESENTE;

        Asistencia asistencia = registroPrevio.orElseGet(() -> {
            Asistencia nueva = new Asistencia();
            nueva.setSesion(sesion);
            nueva.setAlumno(alumno);
            nueva.setFechaRegistro(ahora);
            return nueva;
        });

        asistencia.setEstado(estadoCalculado);
        asistencia.setHoraIngreso(ahora);
        asistencia.setMinutosTarde(minutosTarde);
        asistencia.setMetodoRegistro(MetodoRegistroAsistencia.DNI);
        asistenciaRepository.save(asistencia);

        if (sesion.getEstado() == EstadoSesionClase.PROGRAMADA) {
            sesion.setEstado(EstadoSesionClase.EN_CURSO);
            sesionClaseRepository.save(sesion);
        }

        EstadoMensualidadInfo infoMens = evaluarEstadoMensualidadAlumno(alumno.getId(), hoy);

        String grupoTexto = horarioElegido.getCategoria().getNombre() + " · "
                + horarioElegido.getDiaSemana() + " (" + horarioElegido.getHoraInicio() + " - " + horarioElegido.getHoraFin()
                + ") · " + horarioElegido.getSala();

        return new ConfirmacionIngresoDniDTO(
                false,
                dniLimpio,
                alumno.getId(),
                alumno.getPersona().getNombres() + " " + alumno.getPersona().getApellidos(),
                horarioElegido.getCategoria().getNombre(),
                alumno.getCinturonActual() != null ? alumno.getCinturonActual().getNombre() : "Sin grado",
                alumno.getCinturonActual() != null ? alumno.getCinturonActual().getColor() : "BLANCO",
                grupoTexto,
                ahora,
                estadoCalculado,
                minutosTarde,
                infoMens.etiqueta(),
                infoMens.variante(),
                infoMens.alerta(),
                List.of()
        );
    }

    // ==================== 2. PASE DE LISTA POR GRUPO / HORARIO ====================

    @Transactional
    public SesionClase abrirOCargarSesionPorGrupoYFecha(Long horarioId, Long instructorId, LocalDate fecha) {
        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado"));

        LocalDate fechaEfectiva = (fecha != null) ? fecha : LocalDate.now(clock);

        Optional<SesionClase> existente = sesionClaseRepository.findFirstByHorarioIdAndFechaOrderByIdDesc(horarioId, fechaEfectiva);
        if (existente.isPresent()) {
            return existente.get();
        }

        Instructor instructor = resolverInstructorParaSesion(horario, instructorId);

        SesionClase nueva = new SesionClase();
        nueva.setHorario(horario);
        nueva.setInstructor(instructor);
        nueva.setFecha(fechaEfectiva);
        nueva.setHoraInicio(horario.getHoraInicio());
        nueva.setHoraFin(horario.getHoraFin());
        nueva.setEstado(EstadoSesionClase.EN_CURSO);
        return sesionClaseRepository.save(nueva);
    }

    @Transactional(readOnly = true)
    public List<FilaPaseListaDTO> construirFilasPaseLista(Long sesionId) {
        SesionClase sesion = sesionClaseRepository.findByIdConDetalle(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));

        List<Matricula> matriculados = matriculaRepository.findByHorarioIdAndEstado(
                sesion.getHorario().getId(), EstadoMatricula.ACTIVA
        );

        Map<Long, Asistencia> asistenciasExistentes = asistenciaRepository.findBySesionId(sesionId)
                .stream()
                .collect(Collectors.toMap(a -> a.getAlumno().getId(), a -> a, (a, b) -> a));

        LocalDate hoy = LocalDate.now(clock);
        List<FilaPaseListaDTO> filas = new ArrayList<>();

        for (Matricula m : matriculados) {
            Alumno al = m.getAlumno();
            boolean esMenor = al.getPersona().getFechaNacimiento() != null
                    && Period.between(al.getPersona().getFechaNacimiento(), hoy).getYears() < 18;
            String dniMostrado = AlumnoPagoAutocompletadoDTO.enmascararDni(al.getPersona().getDni(), esMenor);

            EstadoMensualidadInfo infoMens = evaluarEstadoMensualidadAlumno(al.getId(), hoy);
            Asistencia reg = asistenciasExistentes.get(al.getId());

            EstadoAsistencia estadoFila = (reg != null) ? reg.getEstado() : EstadoAsistencia.PRESENTE;
            LocalDateTime horaIng = (reg != null) ? reg.getHoraIngreso() : null;
            int minTarde = (reg != null && reg.getMinutosTarde() != null) ? reg.getMinutosTarde() : 0;
            MetodoRegistroAsistencia metodo = (reg != null) ? reg.getMetodoRegistro() : MetodoRegistroAsistencia.MANUAL;
            String obs = (reg != null) ? reg.getObservacion() : null;

            filas.add(new FilaPaseListaDTO(
                    al.getId(),
                    al.getPersona().getNombres() + " " + al.getPersona().getApellidos(),
                    dniMostrado,
                    al.getCinturonActual() != null ? al.getCinturonActual().getNombre() : "-",
                    al.getCinturonActual() != null ? al.getCinturonActual().getColor() : "BLANCO",
                    infoMens.etiqueta(),
                    infoMens.variante(),
                    estadoFila,
                    horaIng,
                    minTarde,
                    metodo,
                    obs
            ));
        }
        return filas;
    }

    @Transactional
    public SesionClase guardarPaseListaDetallado(
            Long sesionId,
            List<Long> alumnoIds,
            List<EstadoAsistencia> estados,
            List<String> observaciones
    ) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new ReglaNegocioException("No se puede guardar asistencia en una sesión cancelada.");
        }

        if (alumnoIds != null && estados != null) {
            for (int i = 0; i < alumnoIds.size() && i < estados.size(); i++) {
                Long alumnoId = alumnoIds.get(i);
                EstadoAsistencia nuevoEstado = estados.get(i);
                String obs = (observaciones != null && i < observaciones.size()) ? limpiarTexto(observaciones.get(i)) : null;

                Optional<Asistencia> existenteOpt = asistenciaRepository.findBySesionIdAndAlumnoId(sesionId, alumnoId);
                if (existenteOpt.isPresent() && existenteOpt.get().getMetodoRegistro() == MetodoRegistroAsistencia.DNI) {
                    // Preservar la hora de ingreso y método DNI si sigue siendo PRESENTE o TARDANZA
                    Asistencia existente = existenteOpt.get();
                    existente.setEstado(nuevoEstado);
                    if (obs != null) {
                        existente.setObservacion(obs);
                    }
                    if (nuevoEstado == EstadoAsistencia.AUSENTE || nuevoEstado == EstadoAsistencia.JUSTIFICADO) {
                        existente.setHoraIngreso(null);
                        existente.setMinutosTarde(0);
                    }
                    asistenciaRepository.save(existente);
                } else {
                    registrarAsistencia(sesionId, alumnoId, nuevoEstado, MetodoRegistroAsistencia.MANUAL, obs);
                }
            }
        }

        sesion.setEstado(EstadoSesionClase.FINALIZADA);
        return sesionClaseRepository.save(sesion);
    }

    @Transactional
    public SesionClase programarSesion(Long horarioId, Long instructorId, LocalDate fecha) {
        return abrirOCargarSesionPorGrupoYFecha(horarioId, instructorId, fecha);
    }

    @Transactional
    public Asistencia registrarAsistencia(Long sesionId, Long alumnoId, EstadoAsistencia estadoAsistencia) {
        return registrarAsistencia(sesionId, alumnoId, estadoAsistencia, MetodoRegistroAsistencia.MANUAL, null);
    }

    @Transactional
    public Asistencia registrarAsistencia(
            Long sesionId,
            Long alumnoId,
            EstadoAsistencia estadoAsistencia,
            MetodoRegistroAsistencia metodoRegistro,
            String observacion
    ) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de clase no encontrada"));

        if (sesion.getEstado() == EstadoSesionClase.CANCELADA) {
            throw new ReglaNegocioException("No se puede registrar asistencia en una sesión cancelada");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        LocalDateTime ahora = LocalDateTime.now(clock);

        Asistencia asistencia = asistenciaRepository.findBySesionIdAndAlumnoId(sesionId, alumnoId)
                .orElseGet(() -> {
                    Asistencia nueva = new Asistencia();
                    nueva.setSesion(sesion);
                    nueva.setAlumno(alumno);
                    nueva.setFechaRegistro(ahora);
                    return nueva;
                });

        asistencia.setEstado(estadoAsistencia);
        asistencia.setMetodoRegistro(metodoRegistro != null ? metodoRegistro : MetodoRegistroAsistencia.MANUAL);
        asistencia.setObservacion(limpiarTexto(observacion));

        if (estadoAsistencia == EstadoAsistencia.PRESENTE || estadoAsistencia == EstadoAsistencia.TARDANZA) {
            if (asistencia.getHoraIngreso() == null) {
                asistencia.setHoraIngreso(ahora);
            }
            asistencia.setMinutosTarde(calcularMinutosTarde(sesion, asistencia.getHoraIngreso().toLocalTime()));
        } else {
            asistencia.setHoraIngreso(null);
            asistencia.setMinutosTarde(0);
        }

        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public void registrarAsistenciaMasiva(Long sesionId, Set<Long> alumnosPresentesIds) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión no encontrada"));

        List<Matricula> matriculados = matriculaRepository.findByHorarioIdAndEstado(
                sesion.getHorario().getId(), EstadoMatricula.ACTIVA
        );

        for (Matricula m : matriculados) {
            Long alumnoId = m.getAlumno().getId();
            EstadoAsistencia estado = (alumnosPresentesIds != null && alumnosPresentesIds.contains(alumnoId))
                    ? EstadoAsistencia.PRESENTE
                    : EstadoAsistencia.AUSENTE;
            registrarAsistencia(sesionId, alumnoId, estado, MetodoRegistroAsistencia.MANUAL, null);
        }

        sesion.setEstado(EstadoSesionClase.FINALIZADA);
        sesionClaseRepository.save(sesion);
    }

    // ==================== 3. HISTORIAL, BÚSQUEDA JSON Y AUTOMATIZACIÓN ====================

    @Transactional(readOnly = true)
    public Page<Asistencia> buscarHistorialPaginado(FiltroHistorialAsistenciaDTO filtro, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(5, size)));
        return asistenciaRepository.buscarHistorialFiltrado(
                filtro.fecha(),
                filtro.categoriaId(),
                filtro.horarioId(),
                filtro.q(),
                filtro.estado(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<AlumnoBusquedaAsistenciaDTO> buscarAlumnosPorDniONombreJson(String termino) {
        LocalDate hoy = LocalDate.now(clock);
        String q = (termino != null) ? termino.trim().toLowerCase() : "";

        return alumnoRepository.findAll().stream()
                .filter(a -> q.isEmpty()
                        || a.getPersona().getDni().contains(q)
                        || (a.getPersona().getNombres() + " " + a.getPersona().getApellidos()).toLowerCase().contains(q))
                .limit(15)
                .map(a -> {
                    boolean esMenor = a.getPersona().getFechaNacimiento() != null
                            && Period.between(a.getPersona().getFechaNacimiento(), hoy).getYears() < 18;
                    String dniMostrado = AlumnoPagoAutocompletadoDTO.enmascararDni(a.getPersona().getDni(), esMenor);

                    List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(a.getId(), EstadoMatricula.ACTIVA);
                    String cat = activas.isEmpty() ? "Sin matrícula" : activas.getFirst().getCategoria().getNombre();
                    String hor = activas.isEmpty() ? "-" : (activas.getFirst().getHorario().getDiaSemana() + " " + activas.getFirst().getHorario().getHoraInicio());

                    EstadoMensualidadInfo infoMens = evaluarEstadoMensualidadAlumno(a.getId(), hoy);
                    return new AlumnoBusquedaAsistenciaDTO(
                            a.getId(),
                            a.getPersona().getDni(),
                            dniMostrado,
                            a.getPersona().getNombres() + " " + a.getPersona().getApellidos(),
                            cat,
                            hor,
                            a.getCinturonActual() != null ? a.getCinturonActual().getNombre() : "-",
                            a.getEstado(),
                            infoMens.etiqueta()
                    );
                })
                .toList();
    }

    /**
     * Job semanal idempotente: genera las sesiones de los próximos 7 días según los horarios configurados.
     */
    @Transactional
    public int generarSesionesDeLaSemana() {
        LocalDate hoy = LocalDate.now(clock);
        List<Horario> horarios = horarioRepository.buscarHorariosConDetalle(null, null);
        int creadas = 0;

        for (int offset = 0; offset < 7; offset++) {
            LocalDate fecha = hoy.plusDays(offset);
            DiaSemana dia = mapearDiaSemana(fecha.getDayOfWeek());

            for (Horario h : horarios) {
                if (h.getDiaSemana() == dia && Boolean.TRUE.equals(h.getCategoria().getActivo())) {
                    boolean yaExiste = sesionClaseRepository
                            .findFirstByHorarioIdAndFechaOrderByIdDesc(h.getId(), fecha)
                            .isPresent();
                    if (!yaExiste) {
                        Instructor instructor = resolverInstructorParaSesion(h, null);
                        SesionClase nueva = new SesionClase();
                        nueva.setHorario(h);
                        nueva.setInstructor(instructor);
                        nueva.setFecha(fecha);
                        nueva.setHoraInicio(h.getHoraInicio());
                        nueva.setHoraFin(h.getHoraFin());
                        nueva.setEstado(EstadoSesionClase.PROGRAMADA);
                        sesionClaseRepository.save(nueva);
                        creadas++;
                    }
                }
            }
        }
        log.info("Generación semanal de sesiones completada: {} sesiones nuevas creadas.", creadas);
        return creadas;
    }

    /**
     * Job diario idempotente: cierra las sesiones anteriores a hoy que sigan PROGRAMADA o EN_CURSO,
     * marcando AUSENTE a los alumnos con matrícula ACTIVA que no registraron ingreso.
     */
    @Transactional
    public int cerrarSesionesPasadasYMarcarAusentes() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<SesionClase> abiertasPasadas = sesionClaseRepository.findSesionesPasadasAbiertas(
                hoy, List.of(EstadoSesionClase.PROGRAMADA, EstadoSesionClase.EN_CURSO)
        );

        int sesionesCerradas = 0;
        for (SesionClase sesion : abiertasPasadas) {
            List<Matricula> matriculados = matriculaRepository.findByHorarioIdAndEstado(
                    sesion.getHorario().getId(), EstadoMatricula.ACTIVA
            );
            for (Matricula m : matriculados) {
                Long alumnoId = m.getAlumno().getId();
                if (asistenciaRepository.findBySesionIdAndAlumnoId(sesion.getId(), alumnoId).isEmpty()) {
                    Asistencia ausente = new Asistencia();
                    ausente.setSesion(sesion);
                    ausente.setAlumno(m.getAlumno());
                    ausente.setEstado(EstadoAsistencia.AUSENTE);
                    ausente.setMetodoRegistro(MetodoRegistroAsistencia.MANUAL);
                    ausente.setMinutosTarde(0);
                    ausente.setFechaRegistro(ahora);
                    ausente.setObservacion("Ausencia registrada al cierre automático de sesión");
                    asistenciaRepository.save(ausente);
                }
            }
            sesion.setEstado(EstadoSesionClase.FINALIZADA);
            sesionClaseRepository.save(sesion);
            sesionesCerradas++;
        }
        log.info("Cierre de sesiones pasadas completado: {} sesiones finalizadas.", sesionesCerradas);
        return sesionesCerradas;
    }

    @Transactional
    public SesionClase cambiarEstadoSesion(Long sesionId, EstadoSesionClase nuevoEstado) {
        SesionClase sesion = sesionClaseRepository.findById(sesionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sesión de clase no encontrada"));
        sesion.setEstado(nuevoEstado);
        return sesionClaseRepository.save(sesion);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> listarAsistenciasPorSesion(Long sesionId) {
        return asistenciaRepository.findBySesionId(sesionId);
    }

    @Transactional(readOnly = true)
    public Optional<SesionClase> obtenerSesionConDetalle(Long sesionId) {
        return sesionClaseRepository.findByIdConDetalle(sesionId);
    }

    @Transactional(readOnly = true)
    public List<SesionClase> listarSesionesRecientes(LocalDate fecha, Long categoriaId, Long horarioId) {
        return sesionClaseRepository.buscarSesionesPorFiltro(fecha, categoriaId, horarioId)
                .stream()
                .limit(12)
                .toList();
    }

    // ==================== UTILITARIOS INTERNOS ====================

    private Horario resolverHorarioParaIngresoDni(
            List<Matricula> matriculasActivas,
            DiaSemana diaHoy,
            Long horarioSeleccionadoId
    ) {
        if (horarioSeleccionadoId != null) {
            return matriculasActivas.stream()
                    .map(Matricula::getHorario)
                    .filter(h -> h.getId().equals(horarioSeleccionadoId))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El alumno no está matriculado en el horario seleccionado."));
        }

        List<Horario> delDiaHoy = matriculasActivas.stream()
                .map(Matricula::getHorario)
                .filter(h -> h.getDiaSemana() == diaHoy)
                .toList();

        if (delDiaHoy.size() == 1) {
            return delDiaHoy.getFirst();
        }
        if (delDiaHoy.size() > 1) {
            return null; // Pide elegir
        }
        if (matriculasActivas.size() == 1) {
            return matriculasActivas.getFirst().getHorario();
        }
        return null; // Pide elegir
    }

    private SesionClase obtenerOCrearSesionDeHorarioEnFecha(Horario horario, LocalDate fecha) {
        return sesionClaseRepository.findFirstByHorarioIdAndFechaOrderByIdDesc(horario.getId(), fecha)
                .orElseGet(() -> {
                    Instructor instructor = resolverInstructorParaSesion(horario, null);
                    SesionClase nueva = new SesionClase();
                    nueva.setHorario(horario);
                    nueva.setInstructor(instructor);
                    nueva.setFecha(fecha);
                    nueva.setHoraInicio(horario.getHoraInicio());
                    nueva.setHoraFin(horario.getHoraFin());
                    nueva.setEstado(EstadoSesionClase.EN_CURSO);
                    return sesionClaseRepository.save(nueva);
                });
    }

    private Instructor resolverInstructorParaSesion(Horario horario, Long instructorIdExplicito) {
        if (instructorIdExplicito != null) {
            return instructorRepository.findById(instructorIdExplicito)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Instructor no encontrado"));
        }
        if (horario.getInstructor() != null) {
            return horario.getInstructor();
        }
        List<Instructor> todos = instructorRepository.findAll();
        if (todos.isEmpty()) {
            throw new ReglaNegocioException("No hay ningún instructor registrado en el sistema para abrir la sesión.");
        }
        return todos.getFirst();
    }

    private int calcularMinutosTarde(SesionClase sesion, LocalTime horaLlegada) {
        if (sesion.getHoraInicio() == null || !horaLlegada.isAfter(sesion.getHoraInicio())) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(sesion.getHoraInicio(), horaLlegada).toMinutes());
    }

    private DiaSemana mapearDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    private record EstadoMensualidadInfo(String etiqueta, String variante, boolean alerta) {}

    private EstadoMensualidadInfo evaluarEstadoMensualidadAlumno(Long alumnoId, LocalDate hoy) {
        List<Mensualidad> vencidas = mensualidadRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMensualidad.VENCIDA);
        if (!vencidas.isEmpty()) {
            return new EstadoMensualidadInfo(EstadoMensualidad.VENCIDA.getEtiqueta(), "danger", true);
        }
        Optional<Mensualidad> actual = mensualidadRepository.findByAlumnoIdAndAnioAndMes(
                alumnoId, hoy.getYear(), hoy.getMonthValue()
        );
        if (actual.isPresent()) {
            EstadoMensualidad est = actual.get().getEstado();
            if (est == EstadoMensualidad.PAGADA) {
                return new EstadoMensualidadInfo(est.getEtiqueta(), "success", false);
            }
            if (est == EstadoMensualidad.VENCIDA) {
                return new EstadoMensualidadInfo(est.getEtiqueta(), "danger", true);
            }
            return new EstadoMensualidadInfo(est.getEtiqueta(), "warning", true);
        }
        return new EstadoMensualidadInfo(EstadoMensualidad.POR_VENCER.getEtiqueta(), "warning", true);
    }

    private String limpiarTexto(String valor) {
        return (valor == null || valor.trim().isEmpty()) ? null : valor.trim();
    }
}