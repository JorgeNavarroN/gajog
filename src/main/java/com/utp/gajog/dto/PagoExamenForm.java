package com.utp.gajog.dto;

import com.utp.gajog.enums.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;


public class PagoExamenForm {

    private BigDecimal monto;

    private MedioPago medioPago;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaPago;

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }
}
