/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoMensualidad;
import com.utp.gajog.enums.EstadoPago;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.Mensualidad;
import com.utp.gajog.model.Pago;
import com.utp.gajog.repository.MensualidadRepository;
import com.utp.gajog.repository.PagoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JOAQUIN
 */
@Service
public class MensualidadService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;

    public MensualidadService(
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository) {
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional(readOnly = true)
    public List<Mensualidad> listarMensualidades(int anio, int mes) {
        validarPeriodo(anio, mes);

        return mensualidadRepository
                .findByAnioAndMesOrderByAlumno_Persona_ApellidosAsc(
                        anio, mes);
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPagos(int anio, int mes) {
        validarPeriodo(anio, mes);

        return pagoRepository
                .findByMensualidad_AnioAndMensualidad_MesAndTipoPagoAndEstadoOrderByFechaPagoDesc(
                        anio,
                        mes,
                        TipoPago.MENSUALIDAD,
                        EstadoPago.REGISTRADO);
    }

    @Transactional
    public Pago registrarPago(
            Long mensualidadId,
            BigDecimal monto,
            MedioPago medioPago,
            LocalDate fechaPago) {

        validarPago(mensualidadId, monto, medioPago, fechaPago);

        Mensualidad mensualidad = mensualidadRepository
                .findByIdConBloqueo(mensualidadId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La mensualidad seleccionada no existe."));

        if (mensualidad.getEstado() == EstadoMensualidad.PAGADA
                || !pagoRepository.findByMensualidad_IdAndEstado(
                        mensualidadId, EstadoPago.REGISTRADO).isEmpty()) {
            throw new IllegalStateException(
                    "Esta mensualidad ya tiene un pago registrado.");
        }

        if (mensualidad.getMonto() == null
                || mensualidad.getMonto().signum() <= 0) {
            throw new IllegalStateException(
                    "La mensualidad no tiene un importe válido.");
        }

        if (monto.compareTo(mensualidad.getMonto()) != 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser igual al importe de la mensualidad: S/ "
                    + mensualidad.getMonto().toPlainString());
        }

        Pago pago = new Pago();
        pago.setAlumno(mensualidad.getAlumno());
        pago.setMensualidad(mensualidad);
        pago.setTipoPago(TipoPago.MENSUALIDAD);
        pago.setMonto(monto);
        pago.setMedioPago(medioPago);
        pago.setFechaPago(fechaPago.atStartOfDay());
        pago.setEstado(EstadoPago.REGISTRADO);

        pagoRepository.save(pago);

        mensualidad.setEstado(EstadoMensualidad.PAGADA);
        mensualidadRepository.save(mensualidad);

        return pago;
    }

    private void validarPeriodo(int anio, int mes) {
        if (anio < 1900 || anio > 9999 || mes < 1 || mes > 12) {
            throw new IllegalArgumentException(
                    "Selecciona un año y mes válidos.");
        }

        YearMonth.of(anio, mes);
    }

    private void validarPago(
            Long mensualidadId,
            BigDecimal monto,
            MedioPago medioPago,
            LocalDate fechaPago) {

        if (mensualidadId == null) {
            throw new IllegalArgumentException(
                    "Selecciona una mensualidad.");
        }

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Ingresa un monto mayor que cero.");
        }

        if (monto.stripTrailingZeros().scale() > 2
                || monto.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El monto debe tener como máximo dos decimales "
                    + "y no superar S/ 99999999.99.");
        }

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "Selecciona el medio de pago.");
        }

        if (fechaPago == null
                || fechaPago.isAfter(LocalDate.now(ZONA))) {
            throw new IllegalArgumentException(
                    "Selecciona una fecha de pago válida, "
                    + "que no sea futura.");
        }
    }

    @Transactional(readOnly = true)
    public Pago obtenerPagoMensualidad(Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pago no existe."));

        validarPagoEditable(pago);

        // Carga los datos necesarios mientras la transacción está abierta.
        pago.getAlumno().getPersona().getNombres();
        pago.getAlumno().getPersona().getApellidos();
        pago.getMensualidad().getAnio();
        pago.getMensualidad().getMes();

        return pago;
    }

    @Transactional
    public void editarPago(
            Long pagoId,
            MedioPago medioPago,
            LocalDate fechaPago) {

        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El pago no existe."));

        validarPagoEditable(pago);

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "Selecciona el medio de pago.");
        }

        if (fechaPago == null
                || fechaPago.isAfter(LocalDate.now(ZONA))) {
            throw new IllegalArgumentException(
                    "Selecciona una fecha de pago válida, que no sea futura.");
        }

        pago.setMedioPago(medioPago);

        // Conserva la hora registrada y corrige solamente la fecha.
        pago.setFechaPago(
                fechaPago.atTime(pago.getFechaPago().toLocalTime()));

        pagoRepository.save(pago);
    }

    private void validarPagoEditable(Pago pago) {
        if (pago.getTipoPago() != TipoPago.MENSUALIDAD
                || pago.getMensualidad() == null) {
            throw new IllegalArgumentException(
                    "El pago no corresponde a una mensualidad.");
        }

        if (pago.getEstado() != EstadoPago.REGISTRADO) {
            throw new IllegalStateException(
                    "No se puede editar un pago anulado.");
        }
    }
}
