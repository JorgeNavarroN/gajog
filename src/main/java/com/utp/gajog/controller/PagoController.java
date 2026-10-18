package com.utp.gajog.controller;

import com.utp.gajog.model.MedioPago;
import com.utp.gajog.model.Mensualidad;
import com.utp.gajog.model.Pago;
import com.utp.gajog.model.TipoPago;
import com.utp.gajog.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    public record GenerarMensualidadRequest(
            Long alumnoId,
            Integer anio,
            Integer mes,
            BigDecimal monto,
            LocalDate fechaVencimiento
    ) {}

    public record PagoGeneralRequest(
            Long alumnoId,
            TipoPago tipoPago,
            BigDecimal monto,
            MedioPago medioPago,
            Long referenciaId
    ) {}

    @PostMapping("/mensualidades")
    public ResponseEntity<Mensualidad> generarMensualidad(@RequestBody GenerarMensualidadRequest request) {
        Mensualidad mensualidad = pagoService.generarMensualidad(
                request.alumnoId(),
                request.anio(),
                request.mes(),
                request.monto(),
                request.fechaVencimiento()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mensualidad);
    }

    @PostMapping("/mensualidades/{mensualidadId}/pagar")
    public ResponseEntity<Pago> pagarMensualidad(
            @PathVariable Long mensualidadId,
            @RequestParam MedioPago medioPago
    ) {
        return ResponseEntity.ok(pagoService.pagarMensualidad(mensualidadId, medioPago));
    }

    @PostMapping("/general")
    public ResponseEntity<Pago> registrarPagoGeneral(@RequestBody PagoGeneralRequest request) {
        Pago pago = pagoService.registrarPagoGeneral(
                request.alumnoId(),
                request.tipoPago(),
                request.monto(),
                request.medioPago(),
                request.referenciaId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @PatchMapping("/{pagoId}/anular")
    public ResponseEntity<Pago> anularPago(@PathVariable Long pagoId) {
        return ResponseEntity.ok(pagoService.anularPago(pagoId));
    }

    @GetMapping("/mensualidades/alumno/{alumnoId}")
    public ResponseEntity<List<Mensualidad>> listarMensualidadesPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(pagoService.listarMensualidadesPorAlumno(alumnoId));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<Pago>> listarPagosPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(pagoService.listarPagosPorAlumno(alumnoId));
    }
}