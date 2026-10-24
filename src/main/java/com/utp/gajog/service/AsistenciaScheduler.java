package com.utp.gajog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsistenciaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaScheduler.class);

    private final AsistenciaService asistenciaService;

    public AsistenciaScheduler(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    /**
     * Corre todos los lunes a las 00:02 AM (hora de Lima) y crea de forma idempotente
     * las sesiones de la semana según los horarios activos.
     */
    @Scheduled(cron = "0 2 0 * * MON", zone = "America/Lima")
    public void jobSemanalCrearSesiones() {
        int creadas = asistenciaService.generarSesionesDeLaSemana();
        log.info("[JOB SEMANAL ASISTENCIA] Sesiones creadas para la semana: {}", creadas);
    }

    /**
     * Corre todos los días a las 23:55 PM (hora de Lima), cierra las sesiones pasadas abiertas
     * y marca AUSENTE a quienes no registraron ingreso.
     */
    @Scheduled(cron = "0 55 23 * * *", zone = "America/Lima")
    public void jobDiarioCerrarSesionesPasadas() {
        int cerradas = asistenciaService.cerrarSesionesPasadasYMarcarAusentes();
        log.info("[JOB DIARIO ASISTENCIA] Sesiones pasadas cerradas automáticamente: {}", cerradas);
    }
}