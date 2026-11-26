package com.utp.gajog.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import com.utp.gajog.controller.ExamenEvaluacionController;
import com.utp.gajog.controller.ExamenPagoController;
import com.utp.gajog.dto.PagoExamenForm;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.support.EscenarioExamen;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

class AuditoriaExamenesTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> captura;
    private EscenarioExamen datos;

    @BeforeEach
    void preparar() {
        logger = (Logger) LoggerFactory.getLogger("EXAMENES");
        captura = new ListAppender<>();
        captura.start();
        logger.addAppender(captura);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("instructor", "", List.of()));
        datos = new EscenarioExamen();
        Alumno alumno = datos.agregarAlumno(1L, "Alumno", "Prueba");
        Cinturon blanco = new Cinturon();
        blanco.setId(1L);
        blanco.setOrden(1);
        Cinturon amarillo = new Cinturon();
        amarillo.setId(2L);
        amarillo.setOrden(2);
        alumno.setCinturonActual(blanco);
        datos.matricular(alumno);
        datos.agregarPago(datos.agregarExamen(10L, alumno, amarillo));
        datos.agregarCriterio(1L, "Poomsae", "30");
        datos.agregarCriterio(2L, "Técnica", "70");
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        SecurityContextHolder.clearContext();
        logger.detachAppender(captura);
        captura.stop();
    }

    private void iniciarTransaccion() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void registraEvaluacionYCriteriosYAscensoSoloDespuesDeConfirmar() {
        iniciarTransaccion();
        datos.evaluacionService.registrarResultado(10L, 1L,
                Map.of(1L, new BigDecimal("30"), 2L, new BigDecimal("40")), "Comentario privado");
        assertTrue(captura.list.isEmpty());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        List<String> mensajes = captura.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertEquals(4, mensajes.size());
        assertTrue(mensajes.stream().anyMatch(m -> m.contains("EVALUACION_REGISTRADA") && m.contains("total=70.00") && m.contains("usuario=instructor")));
        assertEquals(2, mensajes.stream().filter(m -> m.contains("CALIFICACION_REGISTRADA")).count());
        assertTrue(mensajes.stream().anyMatch(m -> m.contains("ASCENSO_REGISTRADO") && m.contains("cinturonAnterior=1") && m.contains("cinturonNuevo=2")));
        assertTrue(mensajes.stream().noneMatch(m -> m.contains("Comentario privado")));
    }

    @Test
    void rollbackNoAnunciaPagoGuardado() {
        datos.pagos.datos.clear();
        iniciarTransaccion();
        datos.pagoService.registrarPagoExamen(1L, new BigDecimal("80"),
                com.utp.gajog.enums.MedioPago.EFECTIVO, datos.hoy);
        assertTrue(captura.list.isEmpty());
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertTrue(captura.list.isEmpty());
    }

    @Test
    void pagoConfirmadoIncluyeMontoYExamen() {
        datos.pagos.datos.clear();
        iniciarTransaccion();
        datos.pagoService.registrarPagoExamen(1L, new BigDecimal("80"),
                com.utp.gajog.enums.MedioPago.EFECTIVO, datos.hoy);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, captura.list.size());
        assertTrue(captura.list.getFirst().getFormattedMessage().contains("monto=80 medio=EFECTIVO"));
        assertTrue(captura.list.getFirst().getFormattedMessage().contains("examen=10"));
    }

    @Test
    void desaprobarRegistraResultadoSinAnunciarAscenso() {
        datos.evaluacionService.registrarResultado(10L, 1L,
                Map.of(1L, new BigDecimal("10"), 2L, new BigDecimal("20")), null);
        assertTrue(captura.list.stream().anyMatch(e -> e.getFormattedMessage().contains("resultado=REPROBADO")));
        assertTrue(captura.list.stream().noneMatch(e -> e.getFormattedMessage().contains("ASCENSO_REGISTRADO")));
    }

    @Test
    void puntajeInvalidoGeneraAdvertenciaSinExito() {
        new ExamenEvaluacionController(datos.evaluacionService).registrarResultado(10L, 1L, null,
                Map.of("puntaje-1", "31", "puntaje-2", "40"), new RedirectAttributesModelMap());
        assertEquals(1, captura.list.size());
        assertEquals(Level.WARN, captura.list.getFirst().getLevel());
        assertTrue(captura.list.getFirst().getFormattedMessage().contains("OPERACION_RECHAZADA"));
    }

    @Test
    void formularioPagoInvalidoGeneraAdvertencia() {
        PagoExamenForm formulario = new PagoExamenForm();
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(formulario, "pagoExamenForm");
        errores.reject("conversion");
        new ExamenPagoController(datos.pagoService).registrarPago(1L, formulario, errores,
                new RedirectAttributesModelMap());
        assertEquals(Level.WARN, captura.list.getFirst().getLevel());
        assertTrue(captura.list.getFirst().getFormattedMessage().contains("operacion=PAGO"));
    }

    @Test
    void erroresInesperadosSeRegistranSinMensajeSensible() {
        AuditoriaExamenes.error("PAGO", 1L, 10L, new IllegalStateException("password=secreto"));
        assertEquals(Level.ERROR, captura.list.getFirst().getLevel());
        assertTrue(captura.list.getFirst().getFormattedMessage().contains("tipo=IllegalStateException"));
        assertFalse(captura.list.getFirst().getFormattedMessage().contains("secreto"));
    }

    @Test
    void eliminaSaltosDeLineaParaEvitarRegistrosFalsificados() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario\nFALSO", "", List.of()));
        AuditoriaExamenes.rechazar("EVALUACION", 1L, 10L, "motivo\r\nFALSO");
        String mensaje = captura.list.getFirst().getFormattedMessage();
        assertFalse(mensaje.contains("\n"));
        assertFalse(mensaje.contains("\r"));
    }

    @Test
    void escribeConsultaEnArchivoConFechaUsuarioYAlumno(@TempDir Path temporal) throws Exception {
        Path archivo = temporal.resolve("gajog.log");
        PatternLayoutEncoder formato = new PatternLayoutEncoder();
        formato.setContext(logger.getLoggerContext());
        formato.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %logger - %msg%n");
        formato.start();
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setContext(logger.getLoggerContext());
        appender.setFile(archivo.toString());
        appender.setEncoder(formato);
        appender.start();
        logger.addAppender(appender);
        try {
            AuditoriaExamenes.consultar("PANEL_CONSULTADO", 1L, "pestana=evaluacion");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            formato.stop();
        }
        String registro = Files.readString(archivo);
        assertTrue(registro.matches("(?s)\\d{4}-\\d{2}-\\d{2} .*"));
        assertTrue(registro.contains("EXAMENES - evento=PANEL_CONSULTADO usuario=instructor alumno=1"));
    }
}
