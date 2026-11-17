package com.utp.gajog.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Deja disponible en todas las pantallas el usuario y su rol (para el menú). */
@ControllerAdvice
public class UsuarioActualAdvice {

    @ModelAttribute("usuarioActual")
    public String usuarioActual() {
        Authentication auth = autenticacion();
        return auth == null ? null : auth.getName();
    }

    @ModelAttribute("rolActual")
    public String rolActual() {
        Authentication auth = autenticacion();
        if (auth == null) {
            return null;
        }
        for (GrantedAuthority permiso : auth.getAuthorities()) {
            String nombre = permiso.getAuthority();
            if (nombre != null && nombre.startsWith("ROLE_")) {
                return nombre.substring(5);
            }
        }
        return null;
    }

    private static Authentication autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth;
    }
}
