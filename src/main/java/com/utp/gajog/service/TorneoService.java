package com.utp.gajog.service;

import com.utp.gajog.dto.TorneoForm;
import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.TorneoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creación y consulta de torneos. */
@Service
public class TorneoService {

    private static final Logger log = LoggerFactory.getLogger(TorneoService.class);

    private final TorneoRepository torneoRepository;

    public TorneoService(TorneoRepository torneoRepository) {
        this.torneoRepository = torneoRepository;
    }

    @Transactional(readOnly = true)
    public List<Torneo> listar() {
        return torneoRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public Torneo obtener(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));
    }

    /** Valida los datos y crea el torneo en estado INSCRIPCIÓN ABIERTA. */
    @Transactional
    public Torneo crear(TorneoForm form) {
        if (form.getNombre() == null || form.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del torneo es obligatorio.");
        }
        if (form.getFecha() == null) {
            throw new IllegalArgumentException("La fecha del torneo es obligatoria.");
        }
        if (form.getSede() == null || form.getSede().isBlank()) {
            throw new IllegalArgumentException("La sede es obligatoria.");
        }
        if (form.getModalidad() == null) {
            throw new IllegalArgumentException("Seleccione la modalidad.");
        }
        BigDecimal cuota = form.getCuotaInscripcion();
        if (cuota == null || cuota.signum() < 0) {
            throw new IllegalArgumentException("La cuota de inscripción no puede ser negativa.");
        }

        Torneo torneo = new Torneo();
        torneo.setNombre(form.getNombre().trim());
        torneo.setFecha(form.getFecha());
        torneo.setSede(form.getSede().trim());
        torneo.setModalidad(form.getModalidad());
        torneo.setCuotaInscripcion(cuota);
        torneo.setEstado(EstadoTorneo.INSCRIPCION_ABIERTA);

        Torneo guardado = torneoRepository.save(torneo);
        log.info("Torneo creado: id={}, nombre={}, modalidad={}",
                guardado.getId(), guardado.getNombre(), guardado.getModalidad());
        return guardado;
    }
}
