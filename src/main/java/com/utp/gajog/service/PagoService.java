package com.utp.gajog.service;

import com.utp.gajog.config.NegocioProperties;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.*;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;
    private final AlumnoRepository alumnoRepository;
    private final ExamenRepository examenRepository;
    private final MatriculaRepository matriculaRepository;
    private final TarifaRepository tarifaRepository;
    private final InstructorRepository instructorRepository;
    private final ObservacionProgresoRepository observacionProgresoRepository;
    private final NegocioProperties negocioProperties;
    private final Clock clock;

    public PagoService(
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository,
            AlumnoRepository alumnoRepository,
            ExamenRepository examenRepository,
            MatriculaRepository matriculaRepository,
            TarifaRepository tarifaRepository,
            InstructorRepository instructorRepository,
            ObservacionProgresoRepository observacionProgresoRepository,
            NegocioProperties negocioProperties,
            Clock clock
    ) {
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
        this.alumnoRepository = alumnoRepository;
        this.examenRepository = examenRepository;
        this.matriculaRepository = matriculaRepository;
        this.tarifaRepository = tarifaRepository;
        this.instructorRepository = instructorRepository;
        this.observacionProgresoRepository = observacionProgresoRepository;
        this.negocioProperties = negocioProperties;
        this.clock = clock;
    }

    // ==================== CONSULTAS Y KPIs ====================

    @Transactional(readOnly = true)
    public ResumenMensualidadDTO obtenerResumenPeriodo(Integer anio, Integer mes) {
        BigDecimal recaudado = pagoRepository.sumarRecaudacionPorPeriodoMensualidad(anio, mes);
        long pagados = mensualidadRepository.countByAnioAndMesAndEstado(anio, mes, EstadoMensualidad.PAGADA);
        long porVencer = mensualidadRepository.countByAnioAndMesAndEstado(anio, mes, EstadoMensualidad.POR_VENCER);
        long vencidas = mensualidadRepository.countByAnioAndMesAndEstado(anio, mes, EstadoMensualidad.VENCIDA);

        long totalCuotas = pagados + porVencer + vencidas;
        int porcentaje = totalCuotas > 0 ? (int) Math.round((pagados * 100.0) / totalCuotas) : 0;

        LocalDate limiteGracia = LocalDate.now(clock).minusDays(negocioProperties.getDiasGraciaMora());
        long enRiesgo = mensualidadRepository.findEnRiesgoDeRetiro(limiteGracia).size();

        return new ResumenMensualidadDTO(recaudado, pagados, porVencer, vencidas, porcentaje, enRiesgo);
    }

    @Transactional(readOnly = true)
    public Page<Mensualidad> buscarMensualidadesPorCobrar(FiltroMensualidadDTO filtro, Pageable pageable) {
        List<EstadoMensualidad> estados = (filtro.estado() != null && filtro.estado() != EstadoMensualidad.PAGADA)
                ? List.of(filtro.estado())
                : List.of(EstadoMensualidad.POR_VENCER, EstadoMensualidad.VENCIDA);

        return mensualidadRepository.buscarMensualidadesFiltradas(
                filtro.anio(),
                filtro.mes(),
                estados,
                filtro.q(),
                filtro.categoriaId(),
                filtro.horarioId(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Pago> buscarPagosRealizados(FiltroMensualidadDTO filtro, Pageable pageable) {
        return pagoRepository.buscarPagosDelPeriodo(
                filtro.anio(),
                filtro.mes(),
                filtro.medioPago(),
                filtro.q(),
                filtro.categoriaId(),
                filtro.horarioId(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<Mensualidad> listarAlumnosEnRiesgoRetiro() {
        LocalDate limiteGracia = LocalDate.now(clock).minusDays(negocioProperties.getDiasGraciaMora());
        return mensualidadRepository.findEnRiesgoDeRetiro(limiteGracia);
    }

    @Transactional(readOnly = true)
    public List<AlumnoPagoAutocompletadoDTO> listarAlumnosParaFormularioPago() {
        LocalDate hoy = LocalDate.now(clock);
        List<Alumno> alumnos = alumnoRepository.findAllConDetalle();
        List<AlumnoPagoAutocompletadoDTO> resultado = new ArrayList<>();

        for (Alumno a : alumnos) {
            boolean esMenor = esMenorDeEdad(a.getPersona().getFechaNacimiento(), hoy);
            String dniMostrado = AlumnoPagoAutocompletadoDTO.enmascararDni(a.getPersona().getDni(), esMenor);
            Optional<Categoria> catOpt = obtenerCategoriaActivaDeAlumno(a.getId());
            String nombreCat = catOpt.map(Categoria::getNombre).orElse("Sin categoría activa");
            BigDecimal tarifa = obtenerTarifaVigenteDeAlumno(a);

            resultado.add(new AlumnoPagoAutocompletadoDTO(
                    a.getId(),
                    a.getPersona().getNombres() + " " + a.getPersona().getApellidos(),
                    dniMostrado,
                    esMenor,
                    a.getEstado(),
                    nombreCat,
                    tarifa
            ));
        }
        return resultado;
    }

    public boolean esMenorDeEdad(LocalDate fechaNacimiento, LocalDate fechaReferencia) {
        if (fechaNacimiento == null) {
            return false;
        }
        return Period.between(fechaNacimiento, fechaReferencia).getYears() < 18;
    }

    // ==================== GENERACIÓN DE MENSUALIDADES ====================

    @Transactional
    public Mensualidad generarMensualidad(Long alumnoId, Integer anio, Integer mes, BigDecimal monto, LocalDate fechaVencimiento) {
        validarAnioMes(anio, mes);

        if (mensualidadRepository.findByAlumnoIdAndAnioAndMes(alumnoId, anio, mes).isPresent()) {
            throw new ReglaNegocioException("El alumno ya tiene una mensualidad generada para el periodo " + mes + "/" + anio);
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new ReglaNegocioException("Solo se pueden generar mensualidades para alumnos en estado ACTIVO");
        }

        BigDecimal montoFinal = resolverMontoMensualidad(alumno, monto);
        if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto de la mensualidad debe ser mayor a S/ 0.00");
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDate vencimientoFinal = fechaVencimiento != null ? fechaVencimiento : calcularFechaVencimientoPorDefecto(anio, mes);

        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setAlumno(alumno);
        mensualidad.setAnio(anio);
        mensualidad.setMes(mes);
        mensualidad.setMonto(montoFinal);
        mensualidad.setFechaVencimiento(vencimientoFinal);
        mensualidad.setEstado(vencimientoFinal.isBefore(hoy)
                ? EstadoMensualidad.VENCIDA
                : EstadoMensualidad.POR_VENCER);

        return mensualidadRepository.save(mensualidad);
    }

    /**
     * Genera de forma idempotente las mensualidades del periodo para todos los alumnos
     * activos con matrícula ACTIVA usando la Tarifa configurada de su categoría.
     */
    @Transactional
    public int generarMensualidadesMasivas(Integer anio, Integer mes, BigDecimal montoOpcional, LocalDate fechaVencimiento) {
        validarAnioMes(anio, mes);
        List<Alumno> alumnosActivos = alumnoRepository.findByEstado(EstadoAlumno.ACTIVO);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate vencimiento = fechaVencimiento != null ? fechaVencimiento : calcularFechaVencimientoPorDefecto(anio, mes);
        int generadas = 0;

        for (Alumno alumno : alumnosActivos) {
            List<Matricula> matriculasActivas = matriculaRepository.findByAlumnoIdAndEstado(alumno.getId(), EstadoMatricula.ACTIVA);
            if (matriculasActivas.isEmpty()) {
                continue;
            }

            boolean yaExiste = mensualidadRepository
                    .findByAlumnoIdAndAnioAndMes(alumno.getId(), anio, mes)
                    .isPresent();

            if (!yaExiste) {
                Mensualidad m = new Mensualidad();
                m.setAlumno(alumno);
                m.setAnio(anio);
                m.setMes(mes);
                m.setMonto(resolverMontoMensualidad(alumno, montoOpcional));
                m.setFechaVencimiento(vencimiento);
                m.setEstado(vencimiento.isBefore(hoy)
                        ? EstadoMensualidad.VENCIDA
                        : EstadoMensualidad.POR_VENCER);
                mensualidadRepository.save(m);
                generadas++;
            }
        }
        log.info("Generación masiva de mensualidades ({}/{}): {} cuotas nuevas creadas.", mes, anio, generadas);
        return generadas;
    }

    /**
     * Gancho para generar automáticamente la primera mensualidad al confirmar una matrícula.
     * Si ya existe la cuota del mes actual para el alumno, la reutiliza sin duplicar.
     */
    @Transactional
    public Mensualidad generarPrimeraMensualidadPorMatricula(Matricula matricula) {
        LocalDate hoy = LocalDate.now(clock);
        int anio = hoy.getYear();
        int mes = hoy.getMonthValue();

        Optional<Mensualidad> existente = mensualidadRepository.findByAlumnoIdAndAnioAndMes(
                matricula.getAlumno().getId(), anio, mes
        );
        if (existente.isPresent()) {
            return existente.get();
        }

        BigDecimal tarifaCategoria = tarifaRepository
                .findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(matricula.getCategoria().getId())
                .map(Tarifa::getMontoVigente)
                .orElse(new BigDecimal("100.00"));

        LocalDate vencimiento = hoy.plusDays(negocioProperties.getDiasAvisoPorVencer());

        Mensualidad nueva = new Mensualidad();
        nueva.setAlumno(matricula.getAlumno());
        nueva.setAnio(anio);
        nueva.setMes(mes);
        nueva.setMonto(tarifaCategoria);
        nueva.setFechaVencimiento(vencimiento);
        nueva.setEstado(EstadoMensualidad.POR_VENCER);
        return mensualidadRepository.save(nueva);
    }

    // ==================== REGISTRO, EDICIÓN Y ANULACIÓN DE PAGOS ====================

    @Transactional
    public Pago registrarPagoMensualidadDesdeFormulario(RegistrarPagoFormDTO form, boolean puedeEditarMonto) {
        validarAnioMes(form.getAnio(), form.getMes());

        if (form.getMonto() == null || form.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto a cobrar debe ser mayor a S/ 0.00");
        }

        Alumno alumno = alumnoRepository.findById(form.getAlumnoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            if (form.isReincorporarAlumno()) {
                alumno.setEstado(EstadoAlumno.ACTIVO);
                alumnoRepository.save(alumno);
                log.info("Alumno ID {} ({}) reincorporado a estado ACTIVO tras regularizar pago.",
                        alumno.getId(), alumno.getPersona().getDni());
            } else {
                throw new ReglaNegocioException(
                        "El alumno se encuentra en estado " + alumno.getEstado()
                        + ". Marque la opción 'Reincorporar alumno al registrar el pago' para activarlo."
                );
            }
        }

        Mensualidad mensualidad;
        if (form.getMensualidadId() != null) {
            mensualidad = mensualidadRepository.findById(form.getMensualidadId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Mensualidad no encontrada"));
        } else {
            mensualidad = mensualidadRepository
                    .findByAlumnoIdAndAnioAndMes(alumno.getId(), form.getAnio(), form.getMes())
                    .orElseGet(() -> {
                        Mensualidad nueva = new Mensualidad();
                        nueva.setAlumno(alumno);
                        nueva.setAnio(form.getAnio());
                        nueva.setMes(form.getMes());
                        nueva.setMonto(obtenerTarifaVigenteDeAlumno(alumno));
                        nueva.setFechaVencimiento(calcularFechaVencimientoPorDefecto(form.getAnio(), form.getMes()));
                        nueva.setEstado(EstadoMensualidad.POR_VENCER);
                        return mensualidadRepository.save(nueva);
                    });
        }

        if (mensualidad.getEstado() == EstadoMensualidad.PAGADA) {
            throw new ReglaNegocioException(
                    "La mensualidad del periodo " + mensualidad.getMes() + "/" + mensualidad.getAnio()
                    + " ya fue pagada anteriormente."
            );
        }

        BigDecimal montoTarifa = mensualidad.getMonto();
        BigDecimal montoFinal = puedeEditarMonto ? form.getMonto() : montoTarifa;
        if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto del pago debe ser mayor a S/ 0.00");
        }

        mensualidad.setMonto(montoFinal);
        mensualidad.setEstado(EstadoMensualidad.PAGADA);
        mensualidadRepository.save(mensualidad);

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime fechaPagoEfectiva = form.getFechaPago() != null
                ? LocalDateTime.of(form.getFechaPago(), ahora.toLocalTime())
                : ahora;

        String usuarioActual = obtenerUsuarioAutenticado();

        Pago pago = new Pago();
        pago.setAlumno(alumno);
        pago.setMensualidad(mensualidad);
        pago.setTipoPago(TipoPago.MENSUALIDAD);
        pago.setMonto(montoFinal);
        pago.setMedioPago(form.getMedioPago());
        pago.setFechaPago(fechaPagoEfectiva);
        pago.setFechaRegistro(ahora);
        pago.setRegistradoPor(usuarioActual);
        pago.setNumeroOperacion(limpiarTexto(form.getNumeroOperacion()));
        pago.setObservacion(limpiarTexto(form.getObservacion()));
        pago.setEstado(EstadoPago.REGISTRADO);

        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago pagarMensualidad(Long mensualidadId, MedioPago medioPago) {
        Mensualidad mensualidad = mensualidadRepository.findById(mensualidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensualidad no encontrada"));

        RegistrarPagoFormDTO dto = new RegistrarPagoFormDTO();
        dto.setMensualidadId(mensualidad.getId());
        dto.setAlumnoId(mensualidad.getAlumno().getId());
        dto.setAnio(mensualidad.getAnio());
        dto.setMes(mensualidad.getMes());
        dto.setMonto(mensualidad.getMonto());
        dto.setFechaPago(LocalDate.now(clock));
        dto.setMedioPago(medioPago != null ? medioPago : MedioPago.EFECTIVO);

        return registrarPagoMensualidadDesdeFormulario(dto, false);
    }

    @Transactional
    public Pago editarPago(EditarPagoFormDTO form, boolean puedeEditarMonto) {
        Pago pago = pagoRepository.findById(form.getPagoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));

        if (pago.getEstado() == EstadoPago.ANULADO) {
            throw new ReglaNegocioException("No se puede editar un pago que se encuentra ANULADO.");
        }

        if (!Objects.equals(pago.getVersion(), form.getVersion())) {
            throw new ReglaNegocioException(
                    "Este pago fue modificado por otro usuario simultáneamente. Recargue la página e intente de nuevo."
            );
        }

        if (form.getMonto() == null || form.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor a S/ 0.00");
        }

        try {
            if (puedeEditarMonto) {
                pago.setMonto(form.getMonto());
                if (pago.getMensualidad() != null) {
                    Mensualidad m = pago.getMensualidad();
                    m.setMonto(form.getMonto());
                    mensualidadRepository.save(m);
                }
            }

            LocalDateTime ahora = LocalDateTime.now(clock);
            if (form.getFechaPago() != null) {
                pago.setFechaPago(LocalDateTime.of(form.getFechaPago(), pago.getFechaPago().toLocalTime()));
            }
            pago.setMedioPago(form.getMedioPago());
            pago.setNumeroOperacion(limpiarTexto(form.getNumeroOperacion()));
            pago.setObservacion(limpiarTexto(form.getObservacion()));
            pago.setModificadoPor(obtenerUsuarioAutenticado());
            pago.setFechaModificacion(ahora);

            return pagoRepository.saveAndFlush(pago);
        } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
            throw new ReglaNegocioException(
                    "Conflicto de edición concurrente: otro usuario actualizó este pago al mismo tiempo. Recargue la vista."
            );
        }
    }

    @Transactional
    public Pago registrarPagoGeneral(Long alumnoId, TipoPago tipoPago, BigDecimal monto, MedioPago medioPago, Long referenciaId) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto del pago debe ser mayor a S/ 0.00");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new ReglaNegocioException("No se puede registrar un cobro a un alumno en estado " + alumno.getEstado());
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        String usuarioActual = obtenerUsuarioAutenticado();

        Pago pago = new Pago();
        pago.setAlumno(alumno);
        pago.setTipoPago(tipoPago);
        pago.setMonto(monto);
        pago.setMedioPago(medioPago);
        pago.setFechaPago(ahora);
        pago.setFechaRegistro(ahora);
        pago.setRegistradoPor(usuarioActual);
        pago.setEstado(EstadoPago.REGISTRADO);

        if (tipoPago == TipoPago.EXAMEN && referenciaId != null) {
            Examen examen = examenRepository.findById(referenciaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Examen no encontrado"));
            pago.setExamen(examen);
        } else if (tipoPago == TipoPago.MATRICULA && referenciaId != null) {
            Matricula matricula = matriculaRepository.findById(referenciaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada"));
            pago.setMatricula(matricula);
        }

        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago anularPago(Long pagoId) {
        return anularPago(pagoId, "Anulación manual solicitada en administración");
    }

    @Transactional
    public Pago anularPago(Long pagoId, String motivoAnulacion) {
        if (motivoAnulacion == null || motivoAnulacion.trim().isEmpty()) {
            throw new ReglaNegocioException("Es obligatorio especificar el motivo para anular un pago.");
        }

        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));

        if (pago.getEstado() == EstadoPago.ANULADO) {
            throw new ReglaNegocioException("El pago ya fue anulado previamente");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        String usuarioActual = obtenerUsuarioAutenticado();

        pago.setEstado(EstadoPago.ANULADO);
        pago.setAnuladoPor(usuarioActual);
        pago.setMotivoAnulacion(motivoAnulacion.trim());
        pago.setModificadoPor(usuarioActual);
        pago.setFechaModificacion(ahora);

        if (pago.getMensualidad() != null) {
            Mensualidad mensualidad = pago.getMensualidad();
            mensualidad.setEstado(mensualidad.getFechaVencimiento().isBefore(LocalDate.now(clock))
                    ? EstadoMensualidad.VENCIDA
                    : EstadoMensualidad.POR_VENCER);
            mensualidadRepository.save(mensualidad);
        }

        return pagoRepository.save(pago);
    }

    // ==================== GESTIÓN DE MORA Y RETIRO ====================

    @Transactional
    public int actualizarMensualidadesVencidas() {
        List<Mensualidad> porVencer = mensualidadRepository.findByEstado(EstadoMensualidad.POR_VENCER);
        LocalDate hoy = LocalDate.now(clock);
        int actualizadas = 0;

        for (Mensualidad m : porVencer) {
            if (m.getFechaVencimiento().isBefore(hoy)) {
                m.setEstado(EstadoMensualidad.VENCIDA);
                mensualidadRepository.save(m);
                actualizadas++;
            }
        }
        return actualizadas;
    }

    @Transactional
    public int procesarMoraDiaria() {
        int pasadasAVencida = actualizarMensualidadesVencidas();
        List<Mensualidad> enRiesgo = listarAlumnosEnRiesgoRetiro();

        if (!negocioProperties.isBajaAutomaticaMora()) {
            log.info("Revisión diaria de mora completada: {} cuotas pasaron a VENCIDA, {} alumnos en riesgo de retiro (baja automática desactivada).",
                    pasadasAVencida, enRiesgo.size());
            return 0;
        }

        int retiradosAutomaticamente = 0;
        Set<Long> alumnosProcesados = new HashSet<>();
        for (Mensualidad m : enRiesgo) {
            Alumno alumno = m.getAlumno();
            if (alumnosProcesados.add(alumno.getId()) && alumno.getEstado() == EstadoAlumno.ACTIVO) {
                confirmarRetiroPorMora(alumno.getId(),
                        "Retiro automático por mora tras superar los " + negocioProperties.getDiasGraciaMora()
                        + " días de gracia en la mensualidad " + m.getMes() + "/" + m.getAnio());
                retiradosAutomaticamente++;
            }
        }
        log.info("Baja automática por mora ejecutada: {} alumnos pasados a RETIRADO.", retiradosAutomaticamente);
        return retiradosAutomaticamente;
    }

    @Transactional
    public Alumno confirmarRetiroPorMora(Long alumnoId, String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ReglaNegocioException("Debe indicar el motivo del retiro por mora.");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        alumno.setEstado(EstadoAlumno.RETIRADO);
        Alumno guardado = alumnoRepository.save(alumno);

        List<Instructor> instructores = instructorRepository.findAll();
        if (!instructores.isEmpty()) {
            ObservacionProgreso bitacora = new ObservacionProgreso();
            bitacora.setAlumno(guardado);
            bitacora.setInstructor(instructores.getFirst());
            bitacora.setFecha(LocalDate.now(clock));
            bitacora.setObservacion("[RETIRO POR MORA - " + obtenerUsuarioAutenticado() + "]: " + motivo.trim());
            observacionProgresoRepository.save(bitacora);
        }

        log.info("Retiro por mora confirmado para alumno ID {} por usuario {}. Motivo: {}",
                alumnoId, obtenerUsuarioAutenticado(), motivo.trim());
        return guardado;
    }

    @Transactional
    public Alumno reincorporarAlumno(Long alumnoId, String observacionReincorporacion) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado"));

        alumno.setEstado(EstadoAlumno.ACTIVO);
        Alumno guardado = alumnoRepository.save(alumno);

        List<Instructor> instructores = instructorRepository.findAll();
        if (!instructores.isEmpty()) {
            ObservacionProgreso bitacora = new ObservacionProgreso();
            bitacora.setAlumno(guardado);
            bitacora.setInstructor(instructores.getFirst());
            bitacora.setFecha(LocalDate.now(clock));
            String detalle = (observacionReincorporacion != null && !observacionReincorporacion.isBlank())
                    ? observacionReincorporacion.trim()
                    : "Reincorporación aprobada tras regularización de deuda.";
            bitacora.setObservacion("[REINCORPORACIÓN - " + obtenerUsuarioAutenticado() + "]: " + detalle);
            observacionProgresoRepository.save(bitacora);
        }

        return guardado;
    }

    @Transactional(readOnly = true)
    public Pago obtenerPagoPorId(Long pagoId) {
        return pagoRepository.findByIdConDetalle(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado con ID: " + pagoId));
    }

    @Transactional(readOnly = true)
    public Mensualidad obtenerMensualidadPorId(Long mensualidadId) {
        return mensualidadRepository.findById(mensualidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensualidad no encontrada con ID: " + mensualidadId));
    }

    @Transactional(readOnly = true)
    public List<Mensualidad> listarMensualidadesPorAlumno(Long alumnoId) {
        return mensualidadRepository.findByAlumnoIdOrderByAnioDescMesDesc(alumnoId);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepository.findByAlumnoIdOrderByFechaPagoDesc(alumnoId);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerTarifaVigenteDeAlumno(Alumno alumno) {
        return obtenerCategoriaActivaDeAlumno(alumno.getId())
                .flatMap(cat -> tarifaRepository.findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(cat.getId()))
                .map(Tarifa::getMontoVigente)
                .orElse(new BigDecimal("100.00"));
    }

    public String obtenerNumeroCorrelativoRecibo(Pago pago) {
        int anio = pago.getFechaPago() != null ? pago.getFechaPago().getYear() : LocalDate.now(clock).getYear();
        long id = pago.getId() != null ? pago.getId() : 0L;
        return String.format("REC-%d-%06d", anio, id);
    }

    // ==================== UTILITARIOS INTERNOS ====================

    private Optional<Categoria> obtenerCategoriaActivaDeAlumno(Long alumnoId) {
        List<Matricula> activas = matriculaRepository.findByAlumnoIdAndEstado(alumnoId, EstadoMatricula.ACTIVA);
        if (!activas.isEmpty()) {
            return Optional.of(activas.getFirst().getCategoria());
        }
        List<Matricula> todas = matriculaRepository.findByAlumnoId(alumnoId);
        if (!todas.isEmpty()) {
            return Optional.of(todas.getLast().getCategoria());
        }
        return Optional.empty();
    }

    private BigDecimal resolverMontoMensualidad(Alumno alumno, BigDecimal montoSolicitado) {
        if (montoSolicitado != null && montoSolicitado.compareTo(BigDecimal.ZERO) > 0) {
            return montoSolicitado;
        }
        return obtenerTarifaVigenteDeAlumno(alumno);
    }

    private LocalDate calcularFechaVencimientoPorDefecto(Integer anio, Integer mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        int diaVencimiento = Math.min(10, ym.lengthOfMonth());
        return ym.atDay(diaVencimiento);
    }

    private void validarAnioMes(Integer anio, Integer mes) {
        if (anio == null || anio < 2020 || anio > 2100) {
            throw new ReglaNegocioException("El año indicado no es válido.");
        }
        if (mes == null || mes < 1 || mes > 12) {
            throw new ReglaNegocioException("El mes debe estar comprendido entre 1 y 12.");
        }
    }

    private String limpiarTexto(String valor) {
        return (valor == null || valor.trim().isEmpty()) ? null : valor.trim();
    }

    private String obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SISTEMA";
    }
}