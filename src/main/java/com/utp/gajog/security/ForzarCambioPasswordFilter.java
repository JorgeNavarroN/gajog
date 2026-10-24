package com.utp.gajog.security;

import com.utp.gajog.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ForzarCambioPasswordFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;

    public ForzarCambioPasswordFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!esRutaExenta(path)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                boolean debeCambiar = usuarioRepository.findByNombreUsuario(auth.getName())
                        .map(u -> Boolean.TRUE.equals(u.getRequiereCambioPassword()))
                        .orElse(false);

                if (debeCambiar) {
                    response.sendRedirect(request.getContextPath() + "/cuenta/cambiar-password?forzado=true");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean esRutaExenta(String path) {
        return path.startsWith("/cuenta/cambiar-password")
                || path.startsWith("/login")
                || path.startsWith("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/");
    }
}