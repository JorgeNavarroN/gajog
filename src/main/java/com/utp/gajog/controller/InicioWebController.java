/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * @author JOAQUIN
 */
@Controller
public class InicioWebController {

    /** Después de iniciar sesión, cada rol entra a la pantalla que puede usar. */
    @GetMapping("/")
    public String inicio(Authentication auth) {
        if (tieneRol(auth, "ROLE_ADMINISTRADOR") || tieneRol(auth, "ROLE_RECEPCION")) {
            return "redirect:/matriculas";
        }
        if (tieneRol(auth, "ROLE_INSTRUCTOR")) {
            return "redirect:/asistencias";
        }
        return "redirect:/torneos/ranking";
    }

    private static boolean tieneRol(Authentication auth, String rol) {
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority permiso : auth.getAuthorities()) {
            if (rol.equals(permiso.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
