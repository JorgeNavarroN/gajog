package com.utp.gajog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class MensualidadScheduler {

    private static final Logger log = LoggerFactory.getLogger(MensualidadScheduler.class);

    private final PagoService pagoService;
    private final Clock clock;

    public MensualidadScheduler(PagoService pagoService, Clock clock) {
        this.pagoService = pagoService;
        this.clock = clock;
    }

    /**
     * Job mensual: corre el día 1 de cada mes a las 00:05 AM (hora de Lima) y genera
     * de forma idempotente las mensualidades para alumnos con matrícula ACTIVA.
     */
    @Scheduled(cron = "0 5 0 1 * *", zone = "America/Lima")
    public void ejecutarGeneracionMensualAutomatica() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate vencimiento = hoy.withDayOfMonth(Math.min(10, hoy.lengthOfMonth()));
        int creadas = pagoService.generarMensualidadesMasivas(hoy.getYear(), hoy.getMonthValue(), null, vencimiento);
        log.info("[JOB MENSUAL] Periodo {}/{}: {} mensualidades generadas automáticamente.",
                hoy.getMonthValue(), hoy.getYear(), creadas);
    }

    /**
     * Job diario: corre todos los días a las 00:10 AM (hora de Lima), marca como VENCIDA
     * las cuotas cuya fecha límite expiró y evalúa el plazo de gracia de mora.
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "America/Lima")
    public void ejecutarRevisionDiariaDeMora() {
        int retirados = pagoService.procesarMoraDiaria();
        log.info("[JOB DIARIO MORA] Revisión completada. Retiros automáticos aplicados: {}", retirados);
    }
}