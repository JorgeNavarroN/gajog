/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.GrupoForm;
import com.utp.gajog.enums.DiaSemana;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.InstructorRepository;
import com.utp.gajog.service.GestionGrupoService;
import com.utp.gajog.service.GrupoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author JORGE
 */
@Controller
@RequestMapping("/horarios")
public class HorarioWebController {

    private final GrupoService grupoService;
    private final GestionGrupoService gestionGrupoService;
    private final CategoriaRepository categoriaRepository;
    private final InstructorRepository instructorRepository;

    public HorarioWebController(
            GrupoService grupoService,
            GestionGrupoService gestionGrupoService,
            CategoriaRepository categoriaRepository,
            InstructorRepository instructorRepository) {

        this.grupoService = grupoService;
        this.gestionGrupoService = gestionGrupoService;
        this.categoriaRepository = categoriaRepository;
        this.instructorRepository = instructorRepository;
    }

    @InitBinder("grupoForm")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(21);

        binder.setAllowedFields(
                "nombre",
                "categoriaId",
                "instructorId",
                "cupoMaximo",
                "horarios[*].diaSemana",
                "horarios[*].horaInicio",
                "horarios[*].horaFin");
    }

    @GetMapping
    public String mostrar(Model model) {
        model.addAttribute("grupos", grupoService.listarParaGestion());
        return "horarios";
    }

    @GetMapping("/nuevo")
    public String mostrarNuevo(Model model) {
        model.addAttribute("grupoForm", new GrupoForm());
        cargarOpciones(model);

        return "horarios/nuevo";
    }

    @PostMapping
    public String crear(
            @ModelAttribute("grupoForm") GrupoForm formulario,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (resultado.hasErrors()) {
            model.addAttribute(
                    "error",
                    "Revisa los datos y las horas ingresadas.");

            cargarOpciones(model);
            return "horarios/nuevo";
        }

        try {
            gestionGrupoService.crear(formulario);

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());

            cargarOpciones(model);
            return "horarios/nuevo";
        }

        redirectAttributes.addFlashAttribute(
                "exito", "Grupo y horarios creados correctamente.");

        return "redirect:/horarios";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute(
                "categorias",
                categoriaRepository.findByActivoTrue());

        model.addAttribute(
                "instructores",
                instructorRepository.findAllByOrderByPersona_ApellidosAsc());

        model.addAttribute("dias", DiaSemana.values());
    }
}
