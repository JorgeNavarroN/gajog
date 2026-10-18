/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.MatriculaRepository;
import com.utp.gajog.repository.MensualidadRepository;
import com.utp.gajog.repository.PagoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AlumnoRepository alumnoRepository;
    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;
    private final PagoRepository pagoRepository;

    public HomeController(
            AlumnoRepository alumnoRepository,
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository,
            PagoRepository pagoRepository
    ) {
        this.alumnoRepository = alumnoRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.pagoRepository = pagoRepository;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("totalAlumnos", alumnoRepository.count());
        model.addAttribute("totalMatriculas", matriculaRepository.count());
        model.addAttribute("mensualidadesPendientes",
                mensualidadRepository.findByEstado(EstadoMensualidad.PENDIENTE).size());
        model.addAttribute("totalPagos", pagoRepository.count());
        return "index";
    }

    // NUEVO: Método indispensable para renderizar la pantalla de inicio de sesión
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}