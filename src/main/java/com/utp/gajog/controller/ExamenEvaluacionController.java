package com.utp.gajog.controller;

import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Examen;
import com.utp.gajog.service.ExamenEvaluacionService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/examenes")
public class ExamenEvaluacionController {

    private static final String PREFIJO_PUNTAJE = "puntaje-";

    private final ExamenEvaluacionService evaluacionService;

    public ExamenEvaluacionController(
            ExamenEvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @PostMapping("/{examenId}/evaluacion")
    public String registrarResultado(
            @PathVariable("examenId") Long examenId,
            @RequestParam("alumnoId") Long alumnoId,
            @RequestParam(name = "observaciones", required = false)
            String observaciones,
            @RequestParam Map<String, String> parametros,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("alumno", alumnoId);

        try {
            Map<Long, BigDecimal> puntajes = leerPuntajes(parametros);

            Examen examen = evaluacionService.registrarResultado(
                    examenId, puntajes, observaciones);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    examen.getResultado() == ResultadoExamen.APROBADO
                    ? "Ascenso aprobado y cinturón actualizado."
                    : "Resultado registrado: el alumno conserva su cinturón.");
            redirectAttributes.addAttribute("tab", "resultado");

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("tab", "evaluacion");
        }

        return "redirect:/examenes";
    }

    /** Lee los campos "puntaje-{idCriterio}" del formulario. */
    private Map<Long, BigDecimal> leerPuntajes(Map<String, String> parametros) {
        Map<Long, BigDecimal> puntajes = new LinkedHashMap<>();

        for (Map.Entry<String, String> entrada : parametros.entrySet()) {
            if (!entrada.getKey().startsWith(PREFIJO_PUNTAJE)) {
                continue;
            }

            String valor = entrada.getValue() == null
                    ? "" : entrada.getValue().trim().replace(',', '.');

            if (valor.isEmpty()) {
                continue;
            }

            try {
                Long criterioId = Long.valueOf(
                        entrada.getKey().substring(PREFIJO_PUNTAJE.length()));

                puntajes.put(criterioId, new BigDecimal(valor));

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Los puntajes deben ser números válidos.");
            }
        }

        return puntajes;
    }
}
