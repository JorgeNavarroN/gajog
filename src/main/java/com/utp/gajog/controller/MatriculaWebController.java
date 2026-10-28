/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.dto.PagoMensualidadForm;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.enums.MedioPago;
import com.utp.gajog.model.Pago;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.service.MatriculaService;
import com.utp.gajog.service.MensualidadService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author JORGE
 */
@Controller
//@RequestMapping("/mensualidades")
public class MatriculaWebController {

    private final MatriculaService matriculaService;
    private final CategoriaRepository categoriaRepository;
    private final GrupoRepository grupoRepository;
    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private final MensualidadService mensualidadService;

    public MatriculaWebController(
            MatriculaService matriculaService,
            CategoriaRepository categoriaRepository,
            GrupoRepository grupoRepository,
            MensualidadService mensualidadService) {

        this.matriculaService = matriculaService;
        this.categoriaRepository = categoriaRepository;
        this.grupoRepository = grupoRepository;
        this.mensualidadService = mensualidadService;
    }

    @GetMapping("/matriculas")
    public String mostrarMatriculas(
            @RequestParam(name = "busqueda", defaultValue = "") String busqueda,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "grupoId", required = false) Long grupoId,
            @RequestParam(name = "estado", required = false) EstadoMatricula estado,
            Model model) {

        model.addAttribute(
                "matriculas",
                matriculaService.listar(
                        busqueda, categoriaId, grupoId, estado)
        );

        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("grupos", grupoRepository.findAll());
        model.addAttribute("estados", EstadoMatricula.values());

        // Conservan los filtros seleccionados al recargar la pantalla.
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("grupoId", grupoId);
        model.addAttribute("estadoSeleccionado", estado);

        return "matriculas";
    }

}
