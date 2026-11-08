package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.EstadoAsistencia;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.enums.EstadoPago;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Asistencia;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.Examen;
import com.utp.gajog.model.Pago;
import com.utp.gajog.model.Tarifa;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.AsistenciaRepository;
import com.utp.gajog.repository.CinturonRepository;
import com.utp.gajog.repository.ExamenRepository;
import com.utp.gajog.repository.MatriculaRepository;
import com.utp.gajog.repository.PagoRepository;
import com.utp.gajog.repository.TarifaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ExamenPagoService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    /** Tarifa usada si no hay una tarifa EXAMEN vigente en la tabla. */
    public static final BigDecimal TARIFA_POR_DEFECTO = new BigDecimal("80.00");


    public static final int ASISTENCIA_MINIMA_PORCENTAJE = 75;
    public static final int ANTIGUEDAD_MINIMA_MESES = 3;

    /** Resultado de revisar los indicadores de un alumno. */
    public static class Requisitos {

        private final int asistenciaPorcentaje;
        private final long mesesAntiguedad;

        public Requisitos(int asistenciaPorcentaje, long mesesAntiguedad) {
            this.asistenciaPorcentaje = asistenciaPorcentaje;
            this.mesesAntiguedad = mesesAntiguedad;
        }

        public int getAsistenciaPorcentaje() {
            return asistenciaPorcentaje;
        }

        public long getMesesAntiguedad() {
            return mesesAntiguedad;
        }

        public boolean isCumpleAsistencia() {
            return asistenciaPorcentaje >= ASISTENCIA_MINIMA_PORCENTAJE;
        }

        public boolean isCumpleAntiguedad() {
            return mesesAntiguedad >= ANTIGUEDAD_MINIMA_MESES;
        }
    }

    private final AlumnoRepository alumnoRepository;
    private final CinturonRepository cinturonRepository;
    private final ExamenRepository examenRepository;
    private final PagoRepository pagoRepository;
    private final TarifaRepository tarifaRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaRepository asistenciaRepository;

    public ExamenPagoService(
            AlumnoRepository alumnoRepository,
            CinturonRepository cinturonRepository,
            ExamenRepository examenRepository,
            PagoRepository pagoRepository,
            TarifaRepository tarifaRepository,
            MatriculaRepository matriculaRepository,
            AsistenciaRepository asistenciaRepository) {
        this.alumnoRepository = alumnoRepository;
        this.cinturonRepository = cinturonRepository;
        this.examenRepository = examenRepository;
        this.pagoRepository = pagoRepository;
        this.tarifaRepository = tarifaRepository;
        this.matriculaRepository = matriculaRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------
    /** Monto del examen: tarifa EXAMEN vigente hoy o S/ 80.00 por defecto. */
    @Transactional(readOnly = true)
    public BigDecimal tarifaExamen() {
        LocalDate hoy = LocalDate.now(ZONA);

        return tarifaRepository
                .findByTipoPagoAndActivoTrue(TipoPago.EXAMEN)
                .stream()
                .filter(t -> t.getMonto() != null && t.getMonto().signum() > 0)
                .filter(t -> t.getFechaInicio() != null
                && !t.getFechaInicio().isAfter(hoy))
                .filter(t -> t.getFechaFin() == null
                || !t.getFechaFin().isBefore(hoy))
                .max(Comparator.comparing(Tarifa::getFechaInicio))
                .map(Tarifa::getMonto)
                .orElse(TARIFA_POR_DEFECTO);
    }

    /** Cinturón al que aspira el alumno: el siguiente en la escala. */
    @Transactional(readOnly = true)
    public Optional<Cinturon> proximoCinturon(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                "El alumno seleccionado no existe."));

        Cinturon actual = alumno.getCinturonActual();

        if (actual == null) {
            return Optional.empty();
        }

        return cinturonRepository.findAllByOrderByOrdenAsc().stream()
                .filter(c -> c.getOrden() > actual.getOrden())
                .findFirst();
    }

    /** Asistencia mínima y antigüedad mínima para rendir (solo informativo). */
    @Transactional(readOnly = true)
    public Requisitos consultarRequisitos(Long alumnoId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                "El alumno seleccionado no existe."));

        List<Asistencia> asistencias = asistenciaRepository
                .findByAlumno_IdOrderByFechaRegistroDesc(alumnoId);

        long asistio = asistencias.stream()
                .filter(a -> a.getEstado() == EstadoAsistencia.ASISTIO)
                .count();

        int porcentaje = asistencias.isEmpty()
                ? 0
                : (int) Math.round(asistio * 100.0 / asistencias.size());

        long meses = alumno.getFechaIngreso() == null
                ? 0
                : Period.between(alumno.getFechaIngreso(),
                        LocalDate.now(ZONA)).toTotalMonths();

        return new Requisitos(porcentaje, meses);
    }


    @Transactional(readOnly = true)
    public boolean tienePagoRegistrado(Long examenId) {
        return examenId != null
                && !pagoRepository.findByExamen_IdAndEstado(
                        examenId, EstadoPago.REGISTRADO).isEmpty();
    }

    /** Pago registrado del examen, si existe. */
    @Transactional(readOnly = true)
    public Optional<Pago> pagoDelExamen(Long examenId) {
        if (examenId == null) {
            return Optional.empty();
        }

        return pagoRepository
                .findByExamen_IdAndEstado(examenId, EstadoPago.REGISTRADO)
                .stream()
                .findFirst();
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    /**
     * Devuelve el examen PENDIENTE del alumno o crea uno nuevo apuntando
     * al siguiente cinturón de la escala.
     */
    @Transactional
    public Examen programarExamen(Long alumnoId, LocalDate fecha) {
        Alumno alumno = obtenerAlumnoApto(alumnoId);

        Optional<Examen> pendiente = examenRepository
                .findFirstByAlumno_IdAndResultadoOrderByIdDesc(
                        alumnoId, ResultadoExamen.PENDIENTE);

        if (pendiente.isPresent()) {
            return pendiente.get();
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Indica la fecha del examen.");
        }

        Cinturon objetivo = proximoCinturon(alumnoId)
                .orElseThrow(() -> new IllegalStateException(
                "El alumno ya tiene el cinturón más alto de la escala "
                + "o no tiene un cinturón actual registrado."));

        Examen examen = new Examen();
        examen.setAlumno(alumno);
        examen.setCinturonObjetivo(objetivo);
        examen.setFechaExamen(fecha);
        examen.setResultado(ResultadoExamen.PENDIENTE);

        return examenRepository.save(examen);
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------

    @Transactional
    public Pago registrarPagoExamen(
            Long alumnoId,
            BigDecimal monto,
            MedioPago medioPago,
            LocalDate fechaPago) {

        validarPago(alumnoId, monto, medioPago, fechaPago);

        Examen examen = programarExamen(alumnoId, LocalDate.now(ZONA));

        // Bloquea el examen para evitar dos cobros simultáneos.
        examen = examenRepository.findByIdConBloqueo(examen.getId())
                .orElseThrow(() -> new IllegalStateException(
                "No se pudo bloquear el examen."));

        if (tienePagoRegistrado(examen.getId())) {
            throw new IllegalStateException(
                    "El examen ya tiene un pago registrado.");
        }

        BigDecimal tarifa = tarifaExamen();

        if (monto.compareTo(tarifa) != 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser igual a la tarifa del examen: S/ "
                    + tarifa.toPlainString());
        }

        Pago pago = new Pago();
        pago.setAlumno(examen.getAlumno());
        pago.setExamen(examen);
        pago.setTipoPago(TipoPago.EXAMEN);
        pago.setMonto(monto);
        pago.setMedioPago(medioPago);
        pago.setFechaPago(fechaPago.atStartOfDay());
        pago.setEstado(EstadoPago.REGISTRADO);

        return pagoRepository.save(pago);
    }

    // ------------------------------------------------------------------
    // Validaciones internas
    // ------------------------------------------------------------------
    private Alumno obtenerAlumnoApto(Long alumnoId) {
        if (alumnoId == null) {
            throw new IllegalArgumentException("Selecciona un alumno.");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException(
                "El alumno seleccionado no existe."));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new IllegalStateException(
                    "Solo un alumno ACTIVO puede rendir un examen.");
        }

        if (matriculaRepository.findByAlumno_IdAndEstado(
                alumnoId, EstadoMatricula.ACTIVA).isEmpty()) {
            throw new IllegalStateException(
                    "El alumno no tiene una matrícula activa.");
        }

        return alumno;
    }

    private void validarPago(
            Long alumnoId,
            BigDecimal monto,
            MedioPago medioPago,
            LocalDate fechaPago) {

        if (alumnoId == null) {
            throw new IllegalArgumentException("Selecciona un alumno.");
        }

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor a S/ 0.00.");
        }

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "Selecciona el medio de pago.");
        }

        if (fechaPago == null) {
            throw new IllegalArgumentException(
                    "Indica la fecha del pago.");
        }

        if (fechaPago.isAfter(LocalDate.now(ZONA))) {
            throw new IllegalArgumentException(
                    "La fecha de pago no puede ser futura.");
        }
    }
}
