package com.utp.gajog.service;

import com.utp.gajog.dto.FilaExamenDTO;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.enums.ResultadoExamen;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Examen;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.repository.ExamenRepository;
import com.utp.gajog.repository.MatriculaRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ExamenConsultaService {

    private final MatriculaRepository matriculaRepository;
    private final ExamenRepository examenRepository;
    private final ExamenPagoService pagoService;

    public ExamenConsultaService(
            MatriculaRepository matriculaRepository,
            ExamenRepository examenRepository,
            ExamenPagoService pagoService) {
        this.matriculaRepository = matriculaRepository;
        this.examenRepository = examenRepository;
        this.pagoService = pagoService;
    }

    /** Alumnos con matrícula ACTIVA y el estado de su último examen. */
    @Transactional(readOnly = true)
    public List<FilaExamenDTO> listarFilas() {
        Map<Long, Alumno> alumnos = new LinkedHashMap<>();

        for (Matricula matricula : matriculaRepository.buscarParaListado(
                "", null, null, EstadoMatricula.ACTIVA)) {
            alumnos.putIfAbsent(
                    matricula.getAlumno().getId(), matricula.getAlumno());
        }

        List<Alumno> ordenados = new ArrayList<>(alumnos.values());
        ordenados.sort(Comparator
                .comparing((Alumno a) -> a.getPersona().getApellidos(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(a -> a.getPersona().getNombres(),
                        String.CASE_INSENSITIVE_ORDER));

        List<FilaExamenDTO> filas = new ArrayList<>();

        for (Alumno alumno : ordenados) {
            Optional<Examen> ultimo = ultimoExamen(alumno.getId());
            boolean pagado = ultimo
                    .map(e -> pagoService.tienePagoRegistrado(e.getId()))
                    .orElse(false);

            filas.add(new FilaExamenDTO(
                    alumno,
                    ultimo.orElse(null),
                    pagado,
                    describirEstado(ultimo.orElse(null), pagado)));
        }

        return filas;
    }

    @Transactional(readOnly = true)
    public Optional<Examen> ultimoExamen(Long alumnoId) {
        return examenRepository.findFirstByAlumno_IdOrderByIdDesc(alumnoId);
    }


    @Transactional(readOnly = true)
    public List<Examen> listarExamenesDelAlumno(Long alumnoId) {
        return examenRepository.findByAlumno_IdOrderByIdDesc(alumnoId);
    }

    public String describirEstado(Examen examen, boolean pagado) {
        if (examen == null) {
            return "Sin examen";
        }

        if (examen.getResultado() == ResultadoExamen.APROBADO) {
            return "Ascenso aprobado";
        }

        if (examen.getResultado() == ResultadoExamen.REPROBADO) {
            return "Examen reprobado";
        }

        return pagado ? "Pago registrado · por evaluar" : "Pago pendiente";
    }
}
