package com.utp.gajog.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** A07: cuenta los intentos fallidos y los reinicia al entrar bien. */
@Component
public class ControlIntentosListener {

    private final IntentosLoginService intentos;

    public ControlIntentosListener(IntentosLoginService intentos) {
        this.intentos = intentos;
    }

    @EventListener
    public void alFallarContrasena(AuthenticationFailureBadCredentialsEvent evento) {
        intentos.registrarFallo(evento.getAuthentication().getName());
    }

    @EventListener
    public void alIniciarSesion(AuthenticationSuccessEvent evento) {
        intentos.registrarExito(evento.getAuthentication().getName());
    }
}
