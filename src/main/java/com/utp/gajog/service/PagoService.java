package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PagoService {

    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;
    private final AlumnoRepository alumnoRepository;
    private final ExamenRepository examenRepository;
    private final MatriculaRepository matriculaRepository;

    public PagoService(
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository,
            AlumnoRepository alumnoRepository,
            ExamenRepository examenRepository,
            MatriculaRepository matriculaRepository
    ) {
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
        this.alumnoRepository = alumnoRepository;
        this.examenRepository = examenRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional
    public Mensualidad generarMensualidad(Long alumnoId, Integer anio, Integer mes, BigDecimal monto, LocalDate fechaVencimiento) {
        if (mensualidadRepository.findByAlumnoIdAndAnioAndMes(alumnoId, anio, mes).isPresent()) {
            throw new IllegalArgumentException("El alumno ya tiene una mensualidad generada para el periodo " + mes + "/" + anio);
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        Mensualidad mensualidad = new Mensualidad();
        mensualidad.setAlumno(alumno);
        mensualidad.setAnio(anio);
        mensualidad.setMes(mes);
        mensualidad.setMonto(monto);
        mensualidad.setFechaVencimiento(fechaVencimiento);
        mensualidad.setEstado(fechaVencimiento.isBefore(LocalDate.now())
                ? EstadoMensualidad.VENCIDA
                : EstadoMensualidad.PENDIENTE);

        return mensualidadRepository.save(mensualidad);
    }

    @Transactional
    public int generarMensualidadesMasivas(Integer anio, Integer mes, BigDecimal monto, LocalDate fechaVencimiento) {
        List<Alumno> alumnosActivos = alumnoRepository.findByEstado(EstadoAlumno.ACTIVO);
        int generadas = 0;

        for (Alumno alumno : alumnosActivos) {
            boolean yaExiste = mensualidadRepository
                    .findByAlumnoIdAndAnioAndMes(alumno.getId(), anio, mes)
                    .isPresent();

            if (!yaExiste) {
                Mensualidad m = new Mensualidad();
                m.setAlumno(alumno);
                m.setAnio(anio);
                m.setMes(mes);
                m.setMonto(monto);
                m.setFechaVencimiento(fechaVencimiento);
                m.setEstado(fechaVencimiento.isBefore(LocalDate.now())
                        ? EstadoMensualidad.VENCIDA
                        : EstadoMensualidad.PENDIENTE);
                mensualidadRepository.save(m);
                generadas++;
            }
        }
        return generadas;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // Se ejecuta automáticamente todos los días a medianoche
    public int actualizarMensualidadesVencidas() {
        List<Mensualidad> pendientes = mensualidadRepository.findByEstado(EstadoMensualidad.PENDIENTE);
        LocalDate hoy = LocalDate.now();
        int actualizadas = 0;

        for (Mensualidad m : pendientes) {
            if (m.getFechaVencimiento().isBefore(hoy)) {
                m.setEstado(EstadoMensualidad.VENCIDA);
                mensualidadRepository.save(m);
                actualizadas++;
            }
        }
        return actualizadas;
    }

    @Transactional
    public Pago pagarMensualidad(Long mensualidadId, MedioPago medioPago) {
        Mensualidad mensualidad = mensualidadRepository.findById(mensualidadId)
                .orElseThrow(() -> new IllegalArgumentException("Mensualidad no encontrada"));

        if (mensualidad.getEstado() == EstadoMensualidad.PAGADA) {
            throw new IllegalStateException("Esta mensualidad ya se encuentra pagada");
        }

        mensualidad.setEstado(EstadoMensualidad.PAGADA);
        mensualidadRepository.save(mensualidad);

        Pago pago = new Pago();
        pago.setAlumno(mensualidad.getAlumno());
        pago.setMensualidad(mensualidad);
        pago.setTipoPago(TipoPago.MENSUALIDAD);
        pago.setMonto(mensualidad.getMonto());
        pago.setMedioPago(medioPago);
        pago.setEstado(EstadoPago.REGISTRADO);

        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago registrarPagoGeneral(Long alumnoId, TipoPago tipoPago, BigDecimal monto, MedioPago medioPago, Long referenciaId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        Pago pago = new Pago();
        pago.setAlumno(alumno);
        pago.setTipoPago(tipoPago);
        pago.setMonto(monto);
        pago.setMedioPago(medioPago);
        pago.setEstado(EstadoPago.REGISTRADO);

        if (tipoPago == TipoPago.EXAMEN && referenciaId != null) {
            Examen examen = examenRepository.findById(referenciaId)
                    .orElseThrow(() -> new IllegalArgumentException("Examen no encontrado"));
            pago.setExamen(examen);
        } else if (tipoPago == TipoPago.MATRICULA && referenciaId != null) {
            Matricula matricula = matriculaRepository.findById(referenciaId)
                    .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada"));
            pago.setMatricula(matricula);
        }

        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago anularPago(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        pago.setEstado(EstadoPago.ANULADO);

        if (pago.getMensualidad() != null) {
            Mensualidad mensualidad = pago.getMensualidad();
            mensualidad.setEstado(mensualidad.getFechaVencimiento().isBefore(LocalDate.now())
                    ? EstadoMensualidad.VENCIDA
                    : EstadoMensualidad.PENDIENTE);
            mensualidadRepository.save(mensualidad);
        }

        return pagoRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public Pago obtenerPagoPorId(Long pagoId) {
        return pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado con ID: " + pagoId));
    }

    @Transactional(readOnly = true)
    public List<Mensualidad> listarMensualidadesPorAlumno(Long alumnoId) {
        return mensualidadRepository.findByAlumnoIdOrderByAnioDescMesDesc(alumnoId);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPagosPorAlumno(Long alumnoId) {
        return pagoRepository.findByAlumnoIdOrderByFechaPagoDesc(alumnoId);
    }
}