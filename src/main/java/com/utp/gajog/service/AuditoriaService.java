package com.utp.gajog.service;

import com.utp.gajog.model.AuditoriaEvento;
import com.utp.gajog.repository.AuditoriaEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaEventoRepository auditoriaRepository;
    private final Clock clock;

    public AuditoriaService(AuditoriaEventoRepository auditoriaRepository, Clock clock) {
        this.auditoriaRepository = auditoriaRepository;
        this.clock = clock;
    }

    @Transactional
    public void registrarEvento(String accion, String entidad, Long entidadId, String detalle) {
        String actor = obtenerUsuarioActual();
        AuditoriaEvento ev = new AuditoriaEvento();
        ev.setFechaHora(LocalDateTime.now(clock));
        ev.setUsuario(actor);
        ev.setAccion(accion);
        ev.setEntidad(entidad);
        ev.setEntidadId(entidadId);
        ev.setDetalle(detalle != null && detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
        auditoriaRepository.save(ev);

        log.info("[AUDITORÍA] usuario={} | accion={} | entidad={}({}) | {}",
                actor, accion, entidad, entidadId, detalle);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> listarUltimosEventos() {
        return auditoriaRepository.findTop50ByOrderByFechaHoraDesc();
    }

    public String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SISTEMA";
    }
}