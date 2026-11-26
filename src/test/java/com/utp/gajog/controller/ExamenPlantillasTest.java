package com.utp.gajog.controller;

import com.utp.gajog.config.ColoresCinturon;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.Examen;
import com.utp.gajog.support.EscenarioExamen;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.junit.jupiter.api.Assertions.*;

class ExamenPlantillasTest {
    @Test
    void renderizaListadoYPanelesConPermisosYValoresRecuperados() throws Exception {
        EscenarioExamen datos = new EscenarioExamen();
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        alumno.getPersona().setDni("12345678");
        Cinturon blanco = new Cinturon();
        blanco.setNombre("Blanco");
        blanco.setColor("BLANCO");
        blanco.setOrden(1);
        Cinturon amarillo = new Cinturon();
        amarillo.setNombre("Amarillo");
        amarillo.setColor("AMARILLO");
        amarillo.setOrden(2);
        alumno.setCinturonActual(blanco);
        datos.cinturones.agregar(blanco);
        datos.cinturones.agregar(amarillo);
        datos.matricular(alumno);
        Examen examen = datos.agregarExamen(10L, alumno, amarillo);
        datos.agregarPago(examen);
        datos.agregarCriterio(1L, "Poomsae", "30");
        datos.agregarCriterio(2L, "Técnica", "70");
        ExamenWebController controlador = new ExamenWebController(datos.consultaService, datos.pagoService, datos.evaluacionService);
        ExtendedModelMap listado = new ExtendedModelMap();
        controlador.mostrar(null, null, "", "", listado);
        String html = renderizar("examenes", listado, "RECEPCION");
        assertTrue(html.contains("Gestionar examen"));
        assertTrue(html.contains("12345678"));

        ExtendedModelMap panel = new ExtendedModelMap();
        controlador.mostrar(1L, "evaluacion", "", "", panel);
        panel.addAttribute("puntajesIngresados", java.util.Map.of("puntaje-1", "31", "puntaje-2", "40"));
        panel.addAttribute("observacionesIngresadas", "Comentario recuperado");
        String evaluacion = renderizar("examenes/alumno", panel, "INSTRUCTOR");
        assertTrue(evaluacion.contains("value=\"31\""));
        assertTrue(evaluacion.contains("Comentario recuperado"));
        assertTrue(evaluacion.contains("examen-evaluacion.js"));
        assertFalse(renderizar("examenes/alumno", panel, "RECEPCION").contains("id=\"form-evaluacion\""));
        panel.addAttribute("tab", "resultado");
        assertTrue(renderizar("examenes/alumno", panel, "INSTRUCTOR").contains("Historial de ascensos"));
        datos.evaluacionService.registrarResultado(10L, 1L,
                java.util.Map.of(1L, new java.math.BigDecimal("30"), 2L, new java.math.BigDecimal("40")), "Aprobado");
        controlador.mostrar(1L, "resultado", "", "", panel);
        assertTrue(renderizar("examenes/alumno", panel, "INSTRUCTOR").contains("Ascenso aprobado"));
        examen.setResultado(com.utp.gajog.enums.ResultadoExamen.PENDIENTE);
        controlador.mostrar(1L, "pago", "", "", panel);
        panel.addAttribute("tab", "pago");
        assertTrue(renderizar("examenes/alumno", panel, "RECEPCION").contains("Pago del examen registrado"));
        datos.pagos.datos.clear();
        controlador.mostrar(1L, "pago", "", "", panel);
        alumno.setCinturonActual(blanco);
        controlador.mostrar(1L, "pago", "", "", panel);
        assertTrue(renderizar("examenes/alumno", panel, "RECEPCION").contains("Registrar pago del examen"));
        assertFalse(renderizar("examenes/alumno", panel, "INSTRUCTOR").contains("Registrar pago del examen"));
    }

    private String renderizar(String plantilla, ExtendedModelMap modelo, String rol) throws Exception {
        MockServletContext servlet = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servlet);
        request.setContextPath("");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var exchange = JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, response);
        WebContext context = new WebContext(exchange, Locale.forLanguageTag("es-PE"), modelo);
        context.setVariable("rolActual", rol);
        context.setVariable("usuarioActual", "Prueba local");
        StaticWebApplicationContext beans = new StaticWebApplicationContext();
        beans.setServletContext(servlet);
        beans.getBeanFactory().registerSingleton("coloresCinturon", new ColoresCinturon());
        beans.refresh();
        try {
            context.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                    new ThymeleafEvaluationContext(beans, null));
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            String html = engine.process(plantilla, context);
            Path destino = Path.of("build", "qa-examenes");
            Files.createDirectories(destino);
            String nombre = plantilla.equals("examenes") ? "listado" : modelo.get("tab").toString();
            Files.writeString(destino.resolve(nombre + "-" + rol + ".html"), html);
            return html;
        } finally {
            beans.close();
        }
    }
}
