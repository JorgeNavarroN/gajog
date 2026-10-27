/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.GrupoDisponibleDTO;
import com.utp.gajog.dto.RegistroMatriculaForm;
import com.utp.gajog.enums.Parentesco;
import com.utp.gajog.model.Categoria;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.service.CategoriaService;
import com.utp.gajog.service.GrupoService;
import com.utp.gajog.service.RegistroMatriculaService;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author JORGE
 */
@Controller
@RequestMapping("/matriculas/nueva")
@SessionAttributes("registroMatricula")
public class RegistroMatriculaController {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;
    private final GrupoService grupoService;
    private final RegistroMatriculaService registroMatriculaService;

    public RegistroMatriculaController(
            CategoriaRepository categoriaRepository,
            CategoriaService categoriaService,
            GrupoService grupoService,
            RegistroMatriculaService registroMatriculaService) {

        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
        this.grupoService = grupoService;
        this.registroMatriculaService = registroMatriculaService;
    }

    @ModelAttribute("registroMatricula")
    public RegistroMatriculaForm prepararFormulario() {
        return new RegistroMatriculaForm();
    }

    @InitBinder("registroMatricula")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields(
                "nombres",
                "apellidos",
                "dni",
                "telefono",
                "correo",
                "fechaNacimiento",
                "apoderadoNombre",
                "apoderadoApellidos",
                "apoderadoDni",
                "parentesco"
        );
    }

    @GetMapping("/paso-1")
    public String mostrarDatos(Model model) {
        cargarOpciones(model);
        return "matriculas/paso-1";
    }

    @PostMapping("/paso-1")
    public String procesarDatos(
            @ModelAttribute("registroMatricula") RegistroMatriculaForm formulario,
            BindingResult errores,
            Model model) {

        // La categoría se asigna en el servidor.
        formulario.setCategoriaId(null);

        if (!errores.hasErrors()) {
            validarDatos(formulario, errores);
        }

        if (!errores.hasErrors()) {
            try {
                Categoria categoria = categoriaService
                        .determinarPorFechaNacimiento(
                                formulario.getFechaNacimiento());

                formulario.setCategoriaId(categoria.getId());

            } catch (IllegalArgumentException | IllegalStateException e) {
                errores.reject("categoria", e.getMessage());
            }
        }

        if (errores.hasErrors()) {
            cargarOpciones(model);
            return "matriculas/paso-1";
        }

        return "redirect:/matriculas/nueva/paso-2";
    }

    @GetMapping("/paso-2")
    public String mostrarRevision(
            @ModelAttribute(
                    name = "registroMatricula",
                    binding = false
            ) RegistroMatriculaForm formulario,
            Model model) {

        if (formulario.getCategoriaId() == null) {
            return "redirect:/matriculas/nueva/paso-1";
        }

        Categoria categoria = categoriaRepository
                .findById(formulario.getCategoriaId())
                .orElse(null);

        if (categoria == null
                || !Boolean.TRUE.equals(categoria.getActivo())) {

            formulario.setCategoriaId(null);
            return "redirect:/matriculas/nueva/paso-1";
        }

        model.addAttribute("categoriaAsignada", categoria);

        model.addAttribute(
                "edad",
                Period.between(
                        formulario.getFechaNacimiento(),
                        LocalDate.now()
                ).getYears()
        );

        return "matriculas/paso-2";
    }

    @GetMapping("/paso-3")
    public String mostrarHorarios(
            @ModelAttribute(
                    name = "registroMatricula",
                    binding = false
            ) RegistroMatriculaForm formulario,
            Model model) {

        if (formulario.getCategoriaId() == null) {
            return "redirect:/matriculas/nueva/paso-1";
        }

        Categoria categoria = categoriaRepository
                .findById(formulario.getCategoriaId())
                .orElse(null);

        if (categoria == null
                || !Boolean.TRUE.equals(categoria.getActivo())) {

            formulario.setCategoriaId(null);
            return "redirect:/matriculas/nueva/paso-1";
        }

        List<GrupoDisponibleDTO> grupos
                = grupoService.listarOpciones(categoria.getId());
        model.addAttribute(
                "hayVacantes",
                grupos.stream().anyMatch(grupo -> grupo.vacantes() > 0)
        );
        model.addAttribute("categoriaAsignada", categoria);
        model.addAttribute("gruposDisponibles", grupos);

        return "matriculas/paso-3";
    }

    @PostMapping("/confirmar")
    public String confirmarMatricula(
            @ModelAttribute(
                    name = "registroMatricula",
                    binding = false
            ) RegistroMatriculaForm formulario,
            @RequestParam(name = "grupoId", required = false) Long grupoId,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes) {

        if (formulario.getCategoriaId() == null) {
            return "redirect:/matriculas/nueva/paso-1";
        }

        formulario.setGrupoId(grupoId);

        try {
            Matricula matricula = registroMatriculaService
                    .confirmar(formulario, grupoId);

            sessionStatus.setComplete();

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Matrícula N.º " + matricula.getId()
                    + " registrada sin pago de matrícula. "
                    + "La primera mensualidad quedó pendiente."
            );

            return "redirect:/matriculas";

        } catch (IllegalArgumentException | IllegalStateException e) {

            redirectAttributes.addFlashAttribute(
                    "errorConfirmacion",
                    e.getMessage()
            );

        } catch (DataIntegrityViolationException e) {

            redirectAttributes.addFlashAttribute(
                    "errorConfirmacion",
                    "No se pudo guardar por un conflicto con los datos "
                    + "existentes. Revisa si el DNI ya fue registrado."
            );
        }

        return "redirect:/matriculas/nueva/paso-3";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute(
                "categorias",
                categoriaRepository.findByActivoTrue()
        );

        model.addAttribute("parentescos", Parentesco.values());
        model.addAttribute("fechaActual", LocalDate.now());
    }

    private void validarDatos(
            RegistroMatriculaForm formulario,
            BindingResult errores) {

        if (texto(formulario.getNombres()).isEmpty()
                || texto(formulario.getApellidos()).isEmpty()) {

            errores.reject(
                    "nombre",
                    "Los nombres y apellidos son obligatorios.");
        }

        if (!texto(formulario.getDni()).matches("\\d{8}")) {
            errores.reject("dni", "El DNI debe tener 8 dígitos.");
        }

        if (!texto(formulario.getTelefono()).matches("\\d{9}")) {
            errores.reject(
                    "telefono",
                    "El teléfono debe tener 9 dígitos.");
        }

        LocalDate nacimiento = formulario.getFechaNacimiento();

        if (nacimiento == null || nacimiento.isAfter(LocalDate.now())) {
            errores.reject(
                    "nacimiento",
                    "Ingresa una fecha de nacimiento válida.");
            return;
        }

        int edad = Period.between(
                nacimiento, LocalDate.now()).getYears();

        boolean tieneDatosApoderado
                = !texto(formulario.getApoderadoNombre()).isEmpty()
                || !texto(formulario.getApoderadoApellidos()).isEmpty()
                || !texto(formulario.getApoderadoDni()).isEmpty()
                || formulario.getParentesco() != null;

        // Regla del proyecto: apoderado opcional desde los 17 años.
        if (edad < 17 || tieneDatosApoderado) {

            if (texto(formulario.getApoderadoNombre()).isEmpty()
                    || texto(formulario.getApoderadoApellidos()).isEmpty()
                    || !texto(formulario.getApoderadoDni()).matches("\\d{8}")
                    || formulario.getParentesco() == null) {

                errores.reject(
                        "apoderado",
                        "Completa nombres, apellidos, DNI y parentesco del apoderado.");
            }

            if (texto(formulario.getDni())
                    .equals(texto(formulario.getApoderadoDni()))) {

                errores.reject(
                        "apoderadoDni",
                        "El alumno y su apoderado deben tener DNI diferentes.");
            }
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
