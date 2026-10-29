package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoExamenForm;
import com.utp.gajog.service.ExamenPagoService;
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

    @PostMapping("/{alumnoId}/pago")
    public String registrarPago(
            @PathVariable("alumnoId") Long alumnoId,
            @ModelAttribute("pagoExamenForm") PagoExamenForm formulario,
            BindingResult resultado,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("alumno", alumnoId);

        if (resultado.hasErrors()) {
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
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
