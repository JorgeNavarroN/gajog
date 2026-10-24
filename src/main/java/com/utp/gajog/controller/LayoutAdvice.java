package com.utp.gajog.controller;

import com.utp.gajog.model.EstadoMatricula;
import com.utp.gajog.model.EstadoMensualidad;
import com.utp.gajog.repository.MatriculaRepository;
import com.utp.gajog.repository.MensualidadRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class LayoutAdvice {

    private final MatriculaRepository matriculaRepository;
    private final MensualidadRepository mensualidadRepository;

    public LayoutAdvice(
            MatriculaRepository matriculaRepository,
            MensualidadRepository mensualidadRepository
    ) {
        this.matriculaRepository = matriculaRepository;
        this.mensualidadRepository = mensualidadRepository;
    }

    @ModelAttribute("badgeMatriculasProceso")
    public long badgeMatriculasProceso() {
        return matriculaRepository.countByEstado(EstadoMatricula.PENDIENTE);
    }

    @ModelAttribute("badgeMensualidadesAlerta")
    public long badgeMensualidadesAlerta() {
        return mensualidadRepository.countByEstadoIn(
                List.of(EstadoMensualidad.POR_VENCER, EstadoMensualidad.VENCIDA)
        );
    }

    @ModelAttribute("usuarioActualNombre")
    public String usuarioActualNombre(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "Invitado";
    }

    @ModelAttribute("usuarioActualRol")
    public String usuarioActualRol(Authentication auth) {
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r.replace("ROLE_", ""))
                    .findFirst()
                    .orElse("ADMINISTRACIÓN");
        }
        return "ADMINISTRACIÓN";
    }

    @ModelAttribute("usuarioActualIniciales")
    public String usuarioActualIniciales(Authentication auth) {
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            String n = auth.getName().trim().toUpperCase();
            return n.length() >= 2 ? n.substring(0, 2) : n;
        }
        return "AD";
    }
}