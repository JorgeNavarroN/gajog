package com.utp.gajog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class AuditoriaExamenes {
    private static final Logger log = LoggerFactory.getLogger("EXAMENES");

    private AuditoriaExamenes() { }

    public static void confirmar(String evento, String datos) {
        String usuario = usuario();
        String detalle = limpiar(datos);
        Runnable registrar = () -> log.info("evento={} usuario={} {}", evento, usuario, detalle);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    registrar.run();
                }
            });
        } else {
            registrar.run();
        }
    }

    public static void consultar(String evento, Long alumnoId, String detalle) {
        log.info("evento={} usuario={} alumno={} detalle={}",
                evento, usuario(), alumnoId, limpiar(detalle));
    }

    public static void rechazar(String operacion, Long alumnoId, Long examenId, String motivo) {
        log.warn("evento=OPERACION_RECHAZADA usuario={} operacion={} alumno={} examen={} motivo={}",
                usuario(), operacion, alumnoId, examenId, limpiar(motivo));
    }

    public static void error(String operacion, Long alumnoId, Long examenId, RuntimeException causa) {
        StackTraceElement[] traza = causa.getStackTrace();
        String origen = traza.length == 0 ? "-" : traza[0].toString();
        log.error("evento=ERROR_OPERACION usuario={} operacion={} alumno={} examen={} tipo={} origen={}",
                usuario(), operacion, alumnoId, examenId, causa.getClass().getSimpleName(), origen);
    }

    private static String usuario() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null ? "sin_sesion" : limpiar(autenticacion.getName());
    }

    private static String limpiar(String texto) {
        if (texto == null) return "-";
        String limpio = texto.replaceAll("[\\r\\n\\t]", "_");
        return limpio.substring(0, Math.min(limpio.length(), 300));
    }
}
