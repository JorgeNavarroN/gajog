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
public class InstructorWebController {

    private static final Logger log = LoggerFactory.getLogger(InstructorWebController.class);

    private final PersonalService personalService;

    public InstructorWebController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping("/instructores")
    public String vistaInstructores(Model model) {
        model.addAttribute("instructores", personalService.listarInstructores());
        model.addAttribute("formInstructor", new PersonaRegistroFormDTO());
        return "instructores";
    }

    @PostMapping("/instructores/guardar")
    public String guardarInstructor(
            @Valid @ModelAttribute("formInstructor") PersonaRegistroFormDTO form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/instructores";
        }

        try {
            Persona p = new Persona();
            p.setDni(form.getDni().trim());
            p.setNombres(form.getNombres().trim());
            p.setApellidos(form.getApellidos().trim());
            p.setTelefono(form.getTelefono());
            p.setCorreo(form.getCorreo());
            personalService.registrarInstructor(p);
            redirectAttributes.addFlashAttribute("exito", "Instructor (Sabonim) registrado correctamente.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al registrar instructor", e);
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al registrar al instructor.");
        }
        return "redirect:/instructores";
    }

    @GetMapping("/personal")
    public String redireccionarPersonal() {
        return "redirect:/instructores";
    }
}