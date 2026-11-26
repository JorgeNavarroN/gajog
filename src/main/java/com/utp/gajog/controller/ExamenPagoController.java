package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoExamenForm;
import com.utp.gajog.service.ExamenPagoService;
import com.utp.gajog.service.AuditoriaExamenes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/examenes")
public class ExamenPagoController {

    private final ExamenPagoService pagoService;

    public ExamenPagoController(ExamenPagoService pagoService) {
        this.pagoService = pagoService;
    }

    @InitBinder("pagoExamenForm")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("monto", "medioPago", "fechaPago");
    }

    /**
     * A06 Diseño inseguro: separación de funciones. Solo recepción o el
     * administrador registran el cobro; el instructor que evalúa no puede.
     * A08 Integridad: solo se aceptan los campos monto, medioPago y fechaPago
     * (ver configurarFormulario) y el monto se compara con la tarifa del servidor.
     */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCION')")
    @PostMapping("/{alumnoId}/pago")
    public String registrarPago(
            @PathVariable("alumnoId") Long alumnoId,
            @ModelAttribute("pagoExamenForm") PagoExamenForm formulario,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("alumno", alumnoId);

        if (resultado.hasErrors()) {
            AuditoriaExamenes.rechazar("PAGO", alumnoId, null, "Formulario de pago inválido");
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Revisa el monto, la fecha y el medio de pago.");
            redirectAttributes.addAttribute("tab", "pago");

            return "redirect:/examenes";
        }

        try {
            pagoService.registrarPagoExamen(
                    alumnoId,
                    formulario.getMonto(),
                    formulario.getMedioPago(),
                    formulario.getFechaPago());

        } catch (IllegalArgumentException | IllegalStateException e) {
            AuditoriaExamenes.rechazar("PAGO", alumnoId, null, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("tab", "pago");

            return "redirect:/examenes";
        } catch (RuntimeException e) {
            AuditoriaExamenes.error("PAGO", alumnoId, null, e);
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar el pago. Revisa el estado del examen antes de reintentar.");
            redirectAttributes.addAttribute("tab", "pago");
            return "redirect:/examenes";
        }

        redirectAttributes.addFlashAttribute(
                "exito",
                "Pago del examen registrado. El instructor ya puede evaluar.");
        redirectAttributes.addAttribute("tab", "evaluacion");

        return "redirect:/examenes";
    }
}
