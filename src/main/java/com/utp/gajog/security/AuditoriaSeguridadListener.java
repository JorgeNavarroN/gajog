package com.utp.gajog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * A09 Fallos de registro y alertas: guarda en el log (archivo logs/gajog.log)
 * los inicios de sesión, los intentos fallidos, los bloqueos, los cierres de
 * sesión y los accesos denegados, con usuario e IP.
 */
@Component
public class AuditoriaSeguridadListener {

    private static final Logger log = LoggerFactory.getLogger("SEGURIDAD");

    private final IntentosLoginService intentos;

    public AuditoriaSeguridadListener(IntentosLoginService intentos) {
        this.intentos = intentos;
    }

    @EventListener
    public void alIniciarSesion(AuthenticationSuccessEvent evento) {
        Authentication auth = evento.getAuthentication();
        log.info("LOGIN_OK usuario={} ip={}", limpiar(auth.getName()), ip(auth));
    }

    @EventListener
    public void alFallarLogin(AbstractAuthenticationFailureEvent evento) {
        Authentication auth = evento.getAuthentication();
        String usuario = auth.getName();
        log.warn("LOGIN_FALLIDO usuario={} motivo={} ip={} intentos={}",
                limpiar(usuario), evento.getException().getClass().getSimpleName(),
                ip(auth), intentos.fallos(usuario));
        if (intentos.estaBloqueado(usuario)) {
            log.error("ALERTA cuenta bloqueada por intentos fallidos: usuario={} ip={}",
                    limpiar(usuario), ip(auth));
        }
    }

    @EventListener
    public void alCerrarSesion(LogoutSuccessEvent evento) {
        log.info("LOGOUT usuario={}", limpiar(evento.getAuthentication().getName()));
    }

    @EventListener
    public void alDenegarAcceso(AuthorizationDeniedEvent<?> evento) {
        Authentication auth = evento.getAuthentication().get();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return; // visitante sin sesión: solo se le pide iniciar sesión
        }
        String recurso = evento.getSource() instanceof HttpServletRequest req
                ? req.getMethod() + " " + req.getRequestURI()
                : String.valueOf(evento.getSource());
        log.warn("ACCESO_DENEGADO usuario={} recurso={}", limpiar(auth.getName()), limpiar(recurso));
    }

    /** Quita saltos de línea para que nadie pueda falsificar líneas del log. */
    static String limpiar(String texto) {
        if (texto == null) {
            return "-";
        }
        String limpio = texto.replaceAll("[\\r\\n\\t]", "_");
        return limpio.length() > 100 ? limpio.substring(0, 100) : limpio;
    }

    private static String ip(Authentication auth) {
        return auth != null && auth.getDetails() instanceof WebAuthenticationDetails d
                ? d.getRemoteAddress() : "-";
    }
}
