package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoExamenForm;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.model.*;
import com.utp.gajog.support.EscenarioExamen;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.junit.jupiter.api.Assertions.*;

class ExamenPagoControllerTest {
    EscenarioExamen datos;
    ExamenPagoController controlador;
    Examen examen;
    PagoExamenForm formulario;
    RedirectAttributesModelMap redireccion;
    BeanPropertyBindingResult errores;

    @BeforeEach
    void preparar() {
        datos = new EscenarioExamen();
        controlador = new ExamenPagoController(datos.pagoService);
        Alumno alumno = datos.agregarAlumno(1L, "Ana", "Pérez");
        datos.matricular(alumno);
        examen = datos.agregarExamen(10L, alumno, null);
        formulario = new PagoExamenForm(); formulario.setMonto(new BigDecimal("80"));
        formulario.setMedioPago(MedioPago.EFECTIVO); formulario.setFechaPago(datos.hoy);
        redireccion = new RedirectAttributesModelMap();
        errores = new BeanPropertyBindingResult(formulario, "pagoExamenForm");
    }

    @Test
    void binderPermiteSoloCamposDelPago() {
        WebDataBinder binder = new WebDataBinder(formulario);
        controlador.configurarFormulario(binder);
        assertArrayEquals(new String[]{"monto", "medioPago", "fechaPago"}, binder.getAllowedFields());
    }

    @Test
    void pagoValidoRedirigeAEvaluacionConConfirmacion() {
        assertEquals("redirect:/examenes", controlador.registrarPago(1L, formulario, errores, redireccion));
        assertEquals(1, datos.pagos.guardados.size());
        Pago pago = datos.pagos.guardados.getFirst();
        assertSame(examen, pago.getExamen());
        assertEquals(formulario.getMonto(), pago.getMonto());
        assertEquals(formulario.getMedioPago(), pago.getMedioPago());
        assertEquals(formulario.getFechaPago().atStartOfDay(), pago.getFechaPago());
        assertEquals("evaluacion", redireccion.get("tab"));
        assertEquals("1", redireccion.get("alumno"));
        assertTrue(redireccion.getFlashAttributes().containsKey("exito"));
    }

    @Test
    void errorDeConversionNoGuardaPago() {
        errores.rejectValue("monto", "typeMismatch");
        controlador.registrarPago(1L, formulario, errores, redireccion);
        assertTrue(datos.pagos.guardados.isEmpty());
        assertEquals("pago", redireccion.get("tab"));
        assertTrue(redireccion.getFlashAttributes().containsKey("error"));
    }

    @Test
    void errorDeNegocioVuelveAlPagoConMensaje() {
        Pago existente = datos.agregarPago(examen);
        controlador.registrarPago(1L, formulario, errores, redireccion);
        assertEquals("pago", redireccion.get("tab"));
        assertEquals("El examen ya tiene un pago registrado.", redireccion.getFlashAttributes().get("error"));
        assertEquals(List.of(existente), datos.pagos.findAll());
        assertTrue(datos.pagos.guardados.isEmpty());
    }
}
