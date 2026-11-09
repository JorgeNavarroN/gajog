package com.utp.gajog.service;

import com.utp.gajog.dto.InscripcionTorneoForm;
import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.EstadoTorneo;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.InscripcionTorneo;
import com.utp.gajog.model.Torneo;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.InscripcionTorneoRepository;
import com.utp.gajog.repository.TorneoRepository;
import com.utp.gajog.util.CategoriaTorneo;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscribe alumnos a un torneo. Valida estado del torneo, que no esté ya
 * inscrito, su edad y cinturón, y registra la cuota pagada.
 */
@Service
public class InscripcionTorneoService {

    private static final Logger log = LoggerFactory.getLogger(InscripcionTorneoService.class);

    private final TorneoRepository torneoRepository;
    private final AlumnoRepository alumnoRepository;
    private final InscripcionTorneoRepository inscripcionRepository;

    public InscripcionTorneoService(
            TorneoRepository torneoRepository,
            AlumnoRepository alumnoRepository,
            InscripcionTorneoRepository inscripcionRepository) {
        this.torneoRepository = torneoRepository;
        this.alumnoRepository = alumnoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public List<InscripcionTorneo> listar(Long torneoId) {
        return inscripcionRepository.findByTorneo_IdOrderByCategoriaAscIdAsc(torneoId);
    }

    /** Alumnos activos que se pueden inscribir (los demás aparecen deshabilitados). */
    @Transactional(readOnly = true)
    public List<Alumno> alumnosActivos() {
        return alumnoRepository.findByEstado(EstadoAlumno.ACTIVO);
    }

    @Transactional
    public InscripcionTorneo inscribir(Long torneoId, InscripcionTorneoForm form) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException("El torneo no existe."));

        if (torneo.getEstado() != EstadoTorneo.INSCRIPCION_ABIERTA) {
            throw new IllegalArgumentException("Las inscripciones de este torneo están cerradas.");
        }
        if (form.getAlumnoId() == null) {
            throw new IllegalArgumentException("Seleccione un alumno.");
        }
        if (form.getMedioPago() == null) {
            throw new IllegalArgumentException("Seleccione el medio de pago.");
        }

        Alumno alumno = alumnoRepository.findById(form.getAlumnoId())
                .orElseThrow(() -> new IllegalArgumentException("El alumno no existe."));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new IllegalArgumentException("Solo pueden competir alumnos activos.");
        }
        if (inscripcionRepository.existsByTorneo_IdAndAlumno_Id(torneoId, alumno.getId())) {
            throw new IllegalArgumentException("El alumno ya está inscrito en este torneo.");
        }
        if (alumno.getPersona().getFechaNacimiento() == null) {
            throw new IllegalArgumentException(
                    "El alumno no tiene fecha de nacimiento registrada, no se puede asignar categoría.");
        }
        if (alumno.getCinturonActual() == null) {
            throw new IllegalArgumentException("El alumno no tiene cinturón registrado.");
        }

        int edad = Period.between(
                alumno.getPersona().getFechaNacimiento(), LocalDate.now()).getYears();

        String categoria = CategoriaTorneo.calcular(
                edad, alumno.getCinturonActual().getOrden(), form.getPesoKg());

        InscripcionTorneo inscripcion = new InscripcionTorneo();
        inscripcion.setTorneo(torneo);
        inscripcion.setAlumno(alumno);
        inscripcion.setCategoria(categoria);
        inscripcion.setPesoKg(form.getPesoKg());
        inscripcion.setMontoPagado(torneo.getCuotaInscripcion());
        inscripcion.setMedioPago(form.getMedioPago());

        InscripcionTorneo guardada = inscripcionRepository.save(inscripcion);
        log.info("Inscripción registrada: torneo={}, alumno={}, categoria={}, monto={}",
                torneoId, alumno.getId(), categoria, guardada.getMontoPagado());
        return guardada;
    }
}
