package com.utp.gajog.controller;

import com.utp.gajog.dto.PersonaRegistroFormDTO;
import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.Persona;
import com.utp.gajog.service.PersonalService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ApoderadoWebController {

    private static final Logger log = LoggerFactory.getLogger(ApoderadoWebController.class);

    private final PersonalService personalService;

    public ApoderadoWebController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping("/apoderados")
    public String vistaApoderados(Model model) {
        model.addAttribute("apoderados", personalService.listarApoderados());
        model.addAttribute("hijosPorApoderado", personalService.obtenerMapaHijosPorApoderado());
        model.addAttribute("formApoderado", new PersonaRegistroFormDTO());
        return "apoderados";
    }

    @PostMapping("/apoderados/guardar")
    public String guardarApoderado(
            @Valid @ModelAttribute("formApoderado") PersonaRegistroFormDTO form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/apoderados";
        }

        try {
            Persona p = new Persona();
            p.setDni(form.getDni().trim());
            p.setNombres(form.getNombres().trim());
            p.setApellidos(form.getApellidos().trim());
            p.setTelefono(form.getTelefono());
            p.setCorreo(form.getCorreo());
            personalService.registrarApoderado(p);
            redirectAttributes.addFlashAttribute("exito", "Apoderado registrado correctamente.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al registrar apoderado", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al registrar al apoderado.");
        }
        return "redirect:/apoderados";
    }
}