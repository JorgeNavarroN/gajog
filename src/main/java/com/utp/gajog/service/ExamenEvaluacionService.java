package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.CriterioExamen;
import com.utp.gajog.model.DetalleExamen;
import com.utp.gajog.model.Examen;
import com.utp.gajog.model.HistorialCinturones;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.CriterioExamenRepository;
import com.utp.gajog.repository.DetalleExamenRepository;
import com.utp.gajog.repository.ExamenRepository;
import com.utp.gajog.repository.HistorialCinturonesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ExamenEvaluacionService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final ExamenRepository examenRepository;
    private final CriterioExamenRepository criterioRepository;
    private final DetalleExamenRepository detalleRepository;
    private final HistorialCinturonesRepository historialRepository;
    private final AlumnoRepository alumnoRepository;
    private final ExamenPagoService pagoService;
    private final BigDecimal puntajeMinimo;

    public ExamenEvaluacionService(
            ExamenRepository examenRepository,
            CriterioExamenRepository criterioRepository,
            DetalleExamenRepository detalleRepository,
            HistorialCinturonesRepository historialRepository,
            AlumnoRepository alumnoRepository,
            ExamenPagoService pagoService,
            @Value("${gajog.examen.puntaje-minimo:70}")
            BigDecimal puntajeMinimo) {
        this.examenRepository = examenRepository;
        this.criterioRepository = criterioRepository;
        this.detalleRepository = detalleRepository;
        this.historialRepository = historialRepository;
        this.alumnoRepository = alumnoRepository;
        this.pagoService = pagoService;
        this.puntajeMinimo = puntajeMinimo;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------
    public BigDecimal getPuntajeMinimo() {
        return puntajeMinimo;
    }

    @Transactional(readOnly = true)
    public List<CriterioExamen> listarCriterios() {
        return criterioRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public BigDecimal puntajeMaximoTotal() {
        return listarCriterios().stream()
                .map(CriterioExamen::getPuntajeMaximo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<DetalleExamen> listarDetalle(Long examenId) {
        return detalleRepository
                .findByExamen_IdOrderByCriterio_IdAsc(examenId);
    }

    @Transactional(readOnly = true)
    public List<HistorialCinturones> listarHistorial(Long alumnoId) {
        return historialRepository
                .findByAlumno_IdOrderByFechaObtencionDescIdDesc(alumnoId);
    }

    // ------------------------------------------------------------------
    // CU16 + CU17 - Calificar y registrar resultado
    // ------------------------------------------------------------------
    /**
     * @param examenId examen PENDIENTE con pago registrado
     * @param puntajes puntaje obtenido por cada criterio (id del criterio)
     * @param observaciones comentario general del instructor (opcional)
     */
    @Transactional
    public Examen registrarResultado(
            Long examenId,
            Map<Long, BigDecimal> puntajes,
            String observaciones) {

        if (examenId == null) {
            throw new IllegalArgumentException("Selecciona un examen.");
        }

        Examen examen = examenRepository.findByIdConBloqueo(examenId)
                .orElseThrow(() -> new IllegalArgumentException(
                "El examen seleccionado no existe."));

        // A2: un examen ya evaluado no admite más modificaciones.
        if (examen.getResultado() != ResultadoExamen.PENDIENTE) {
            throw new IllegalStateException(
                    "El examen ya fue evaluado y no admite más cambios.");
        }

        // Rombo "¿Pago registrado?": sin pago no se puede evaluar.
        if (!pagoService.tienePagoRegistrado(examenId)) {
            throw new IllegalStateException(
                    "El pago del examen está pendiente. "
                    + "Regístralo antes de evaluar.");
        }

        Alumno alumno = examen.getAlumno();

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new IllegalStateException(
                    "Solo un alumno ACTIVO puede ser evaluado.");
        }

        Cinturon objetivo = examen.getCinturonObjetivo();
        Cinturon actual = alumno.getCinturonActual();

        if (actual != null && objetivo.getOrden() <= actual.getOrden()) {
            throw new IllegalStateException(
                    "El cinturón objetivo debe ser superior al actual.");
        }

        String comentario = observaciones == null
                ? null : observaciones.trim();

        if (comentario != null && comentario.length() > 1000) {
            throw new IllegalArgumentException(
                    "Las observaciones no pueden pasar de 1000 caracteres.");
        }

        // A3 + A1: todos los criterios con puntaje entre 0 y su máximo.
        List<CriterioExamen> criterios = listarCriterios();

        if (criterios.isEmpty()) {
            throw new IllegalStateException(
                    "No hay criterios de evaluación configurados.");
        }

        if (puntajes == null || puntajes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Registra la calificación de cada criterio.");
        }

        // Primero se valida todo; recién después se guarda algo.
        Map<CriterioExamen, BigDecimal> validados = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CriterioExamen criterio : criterios) {
            BigDecimal puntaje = puntajes.get(criterio.getId());

            if (puntaje == null) {
                throw new IllegalArgumentException(
                        "Falta el puntaje del criterio: "
                        + criterio.getNombre() + ".");
            }

            puntaje = puntaje.setScale(2, RoundingMode.HALF_UP);

            if (puntaje.signum() < 0
                    || puntaje.compareTo(criterio.getPuntajeMaximo()) > 0) {
                throw new IllegalArgumentException(
                        "El puntaje de \"" + criterio.getNombre()
                        + "\" debe estar entre 0 y "
                        + criterio.getPuntajeMaximo().toPlainString() + ".");
            }

            validados.put(criterio, puntaje);
            total = total.add(puntaje);
        }

        for (Map.Entry<CriterioExamen, BigDecimal> par : validados.entrySet()) {
            DetalleExamen detalle = new DetalleExamen();
            detalle.setExamen(examen);
            detalle.setCriterio(par.getKey());
            detalle.setPuntaje(par.getValue());
            detalleRepository.save(detalle);
        }

        LocalDate hoy = LocalDate.now(ZONA);

        examen.setPuntajeTotal(total);
        examen.setFechaExamen(hoy);
        examen.setObservaciones(
                comentario == null || comentario.isEmpty() ? null : comentario);

        // Rombo "¿Aprobó?"
        if (total.compareTo(puntajeMinimo) >= 0) {
            examen.setResultado(ResultadoExamen.APROBADO);

            // Sistema web: actualiza cinturón e historial de ascensos.
            alumno.setCinturonActual(objetivo);
            alumnoRepository.save(alumno);

            HistorialCinturones historial = new HistorialCinturones();
            historial.setAlumno(alumno);
            historial.setCinturon(objetivo);
            historial.setExamen(examen);
            historial.setFechaObtencion(hoy);
            historialRepository.save(historial);

        } else {
            // "No: conserva cinturón"
            examen.setResultado(ResultadoExamen.REPROBADO);
        }

        return examenRepository.save(examen);
    }
}
