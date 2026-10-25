package com.utp.gajog.service;

import com.utp.gajog.config.NegocioProperties;
import com.utp.gajog.dto.*;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final PersonaRepository personaRepository;
    private final CinturonRepository cinturonRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioRepository horarioRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final HistorialCinturonesRepository historialCinturonesRepository;
    private final ObservacionProgresoRepository observacionProgresoRepository;
    private final InstructorRepository instructorRepository;
    private final NegocioProperties negocioProperties;
    private final Clock clock;

    public AlumnoService(
            AlumnoRepository alumnoRepository,
            PersonaRepository personaRepository,
            CinturonRepository cinturonRepository,
            CategoriaRepository categoriaRepository,
            HorarioRepository horarioRepository,
            ApoderadoRepository apoderadoRepository,
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository,
            AsistenciaRepository asistenciaRepository,
            HistorialCinturonesRepository historialCinturonesRepository,
            ObservacionProgresoRepository observacionProgresoRepository,
            InstructorRepository instructorRepository,
            NegocioProperties negocioProperties,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.personaRepository = personaRepository;
        this.cinturonRepository = cinturonRepository;
        this.categoriaRepository = categoriaRepository;
        this.horarioRepository = horarioRepository;
        this.apoderadoRepository = apoderadoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.historialCinturonesRepository = historialCinturonesRepository;
        this.observacionProgresoRepository = observacionProgresoRepository;
        this.instructorRepository = instructorRepository;
        this.negocioProperties = negocioProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<AlumnoResumenFilaDTO> buscarAlumnosPaginados(FiltroAlumnoDTO filtro, int page, int size) {
        Sort.Direction dir = "desc".equalsIgnoreCase(filtro.direccionOrden()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propiedadOrden = switch (filtro.campoOrden()) {
            case "nombres" -> "persona.nombres";
            case "fechaIngreso" -> "fechaIngreso";
            case "estado" -> "estado";
            default -> "persona.apellidos";
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(5, size)), Sort.by(dir, propiedadOrden));
        LocalDate hoy = LocalDate.now(clock);

        Page<Alumno> paginaEntidades = alumnoRepository.buscarAlumnosFiltrados(
                filtro.q(),
                filtro.categoriaId(),
                filtro.horarioId(),
                filtro.estado(),
                filtro.cinturonId(),
                filtro.estadoMensualidad(),
                hoy.getYear(),
                hoy.getMonthValue(),
                pageable
        );

        List<AlumnoResumenFilaDTO> filas = new ArrayList<>();
        for (Alumno a : paginaEntidades.getContent()) {
            filas.add(mapearAFilaResumen(a, hoy));
        }

        return new PageImpl<>(filas, pageable, paginaEntidades.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ExpedienteAlumnoDTO obtenerExpedienteCompleto(Long alumnoId) {
        Alumno alumno = obtenerPorId(alumnoId);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaNac = alumno.getPersona().getFechaNacimiento();
        int edad = calcularEdadExacta(fechaNac, hoy);
        boolean esMenor = edad < 18;

        List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMatricula.ACTIVA);
        Matricula matriculaActiva = activas.isEmpty() ? null : activas.getFirst();

        Categoria categoriaSugerida = (fechaNac != null)
                ? categoriaRepository.findActivaPorEdad(edad).orElse(null)
                : null;

        boolean requiereCambio = false;
        if (matriculaActiva != null && fechaNac != null) {
            Categoria catActual = matriculaActiva.getCategoria();
            requiereCambio = edad < catActual.getEdadMinima() || edad > catActual.getEdadMaxima();
        }

        List<Asistencia> todasAsistencias = asistenciaRepository.findByAlumnoIdOrderByFechaRegistroDesc(alumnoId);
        long totalSesiones = todasAsistencias.size();
        long asistenciasEfectivas = todasAsistencias.stream()
                .filter(as -> as.getEstado() == EstadoAsistencia.PRESENTE || as.getEstado() == EstadoAsistencia.TARDANZA)
                .count();
        int porcentajeAsistencia = totalSesiones > 0
                ? (int) Math.round((asistenciasEfectivas * 100.0) / totalSesiones)
                : 0;

        List<HistorialCinturones> historial = historialCinturonesRepository.findByAlumnoIdOrderByFechaObtencionDesc(alumnoId);
        LocalDate fechaUltimoGrado = !historial.isEmpty()
                ? historial.getFirst().getFechaObtencion()
                : alumno.getFechaIngreso();
        long mesesAntiguedad = (fechaUltimoGrado != null && !fechaUltimoGrado.isAfter(hoy))
                ? ChronoUnit.MONTHS.between(fechaUltimoGrado, hoy)
                : 0;

        int minAsistencia = negocioProperties.getAsistenciaMinimaExamen();
        int minMeses = negocioProperties.getMesesMinimosExamen();
        boolean elegiblePorAsistencia = totalSesiones > 0 && porcentajeAsistencia >= minAsistencia;

        List<Asistencia> ultimas10 = todasAsistencias.stream().limit(10).toList();

        List<Horario> horariosReasignacion = (categoriaSugerida != null)
                ? horarioRepository.buscarHorariosConDetalle(categoriaSugerida.getId(), null)
                : horarioRepository.buscarHorariosConDetalle(null, null);

        return new ExpedienteAlumnoDTO(
                alumno,
                edad,
                esMenor,
                matriculaActiva,
                categoriaSugerida,
                requiereCambio,
                historial,
                mensualidadRepository.findByAlumnoIdOrderByAnioDescMesDesc(alumnoId),
                ultimas10,
                porcentajeAsistencia,
                totalSesiones,
                asistenciasEfectivas,
                elegiblePorAsistencia,
                minAsistencia,
                mesesAntiguedad,
                minMeses,
                observacionProgresoRepository.findByAlumnoIdOrderByFechaDesc(alumnoId),
                instructorRepository.findAll(),
                horariosReasignacion
        );
    }

    @Transactional
    public Alumno registrarAlumno(Persona datosPersona, Long cinturonInicialId, Long apoderadoId) {
        if (datosPersona.getDni() == null || !datosPersona.getDni().trim().matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI del alumno debe tener exactamente 8 dígitos numéricos");
        }

        String dniLimpio = datosPersona.getDni().trim();
        if (alumnoRepository.findByPersonaDni(dniLimpio).isPresent()) {
            throw new ReglaNegocioException("Ya existe un alumno registrado con el DNI: " + dniLimpio);
        }

        LocalDate hoy = LocalDate.now(clock);
        int edad = calcularEdadExacta(datosPersona.getFechaNacimiento(), hoy);
        if (edad < 18 && apoderadoId == null) {
            throw new ReglaNegocioException("El registro de un apoderado es obligatorio para alumnos menores de 18 años");
        }

        Persona persona = personaRepository.findByDni(dniLimpio)
                .map(existente -> {
                    existente.setNombres(datosPersona.getNombres().trim());
                    existente.setApellidos(datosPersona.getApellidos().trim());
                    if (datosPersona.getFechaNacimiento() != null) {
                        existente.setFechaNacimiento(datosPersona.getFechaNacimiento());
                    }
                    if (datosPersona.getTelefono() != null && !datosPersona.getTelefono().isBlank()) {
                        existente.setTelefono(datosPersona.getTelefono().trim());
                    }
                    if (datosPersona.getCorreo() != null && !datosPersona.getCorreo().isBlank()) {
                        existente.setCorreo(datosPersona.getCorreo().trim());
                    }
                    return personaRepository.save(existente);
                })
                .orElseGet(() -> {
                    datosPersona.setDni(dniLimpio);
                    return personaRepository.save(datosPersona);
                });

        Cinturon cinturon = (cinturonInicialId != null)
                ? cinturonRepository.findById(cinturonInicialId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cinturón no encontrado"))
                : cinturonRepository.findByOrden(1)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cinturón inicial no configurado"));

        Alumno alumno = new Alumno();
        alumno.setPersona(persona);
        alumno.setCinturonActual(cinturon);
        alumno.setFechaIngreso(hoy);
        alumno.setEstado(EstadoAlumno.ACTIVO);

        if (apoderadoId != null) {
            Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Apoderado no encontrado"));
            alumno.setApoderado(apoderado);
        }

        Alumno alumnoGuardado = alumnoRepository.save(alumno);

        HistorialCinturones historial = new HistorialCinturones();
        historial.setAlumno(alumnoGuardado);
        historial.setCinturon(cinturon);
        historial.setFechaObtencion(hoy);
        historialCinturonesRepository.save(historial);

        return alumnoGuardado;
    }

    @Transactional(readOnly = true)
    public List<Alumno> listarTodos() {
        return alumnoRepository.findAllConDetalle();
    }

    @Transactional(readOnly = true)
    public List<Alumno> listarPorEstado(EstadoAlumno estado) {
        return alumnoRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<Cinturon> listarCinturonesOrdenados() {
        return cinturonRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategoriasActivas() {
        return categoriaRepository.findByActivoTrueOrderByEdadMinimaAsc();
    }

    @Transactional(readOnly = true)
    public List<Horario> listarTodosLosHorarios() {
        return horarioRepository.buscarHorariosConDetalle(null, null);
    }

    @Transactional(readOnly = true)
    public Alumno obtenerPorId(Long id) {
        return alumnoRepository.findByIdConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado con ID: " + id));
    }

    @Transactional
    public Alumno cambiarEstado(Long alumnoId, EstadoAlumno nuevoEstado) {
        Alumno alumno = obtenerPorId(alumnoId);
        alumno.setEstado(nuevoEstado);
        return alumnoRepository.save(alumno);
    }

    public int calcularEdadExacta(LocalDate fechaNacimiento, LocalDate fechaReferencia) {
        if (fechaNacimiento == null) {
            return 0;
        }
        return Period.between(fechaNacimiento, fechaReferencia).getYears();
    }

    private AlumnoResumenFilaDTO mapearAFilaResumen(Alumno a, LocalDate hoy) {
        LocalDate fn = a.getPersona().getFechaNacimiento();
        int edad = calcularEdadExacta(fn, hoy);
        boolean esMenor = edad < 18;
        String dniMostrado = AlumnoPagoAutocompletadoDTO.enmascararDni(a.getPersona().getDni(), esMenor);

        List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(a.getId(), EstadoMatricula.ACTIVA);
        String catNombre = "Sin matrícula";
        String horTexto = "-";
        Long horId = null;
        boolean desfaseEdad = false;

        if (!activas.isEmpty()) {
            Matricula m = activas.getFirst();
            Categoria c = m.getCategoria();
            Horario h = m.getHorario();
            catNombre = c.getNombre();
            horTexto = m.getGrupo().getHorarios().stream()
                    .map(turno -> turno.getDiaSemana() + " " + turno.getHoraInicio()
                            + " (" + turno.getSala() + ")")
                    .collect(java.util.stream.Collectors.joining(" · "));
            horId = h.getId();
            if (fn != null) {
                desfaseEdad = (edad < c.getEdadMinima() || edad > c.getEdadMaxima());
            }
        }

        Optional<Mensualidad> mensActual = mensualidadRepository.findByAlumnoIdAndAnioAndMes(
                a.getId(), hoy.getYear(), hoy.getMonthValue()
        );
        List<Mensualidad> vencidasPrevias = mensualidadRepository.findByAlumnoIdAndEstado(a.getId(), EstadoMensualidad.VENCIDA);

        String etiquetaMens;
        String varianteMens;
        if (!vencidasPrevias.isEmpty()) {
            etiquetaMens = EstadoMensualidad.VENCIDA.getEtiqueta();
            varianteMens = "danger";
        } else if (mensActual.isPresent()) {
            EstadoMensualidad est = mensActual.get().getEstado();
            etiquetaMens = est.getEtiqueta();
            varianteMens = (est == EstadoMensualidad.PAGADA) ? "success" : (est == EstadoMensualidad.VENCIDA ? "danger" : "warning");
        } else {
            etiquetaMens = "Por vencer";
            varianteMens = "warning";
        }

        return new AlumnoResumenFilaDTO(
                a.getId(),
                a.getPersona().getNombres(),
                a.getPersona().getApellidos(),
                dniMostrado,
                esMenor,
                edad,
                a.getFechaIngreso(),
                a.getCinturonActual(),
                a.getEstado(),
                catNombre,
                horTexto,
                horId,
                etiquetaMens,
                varianteMens,
                desfaseEdad
        );
    }
}