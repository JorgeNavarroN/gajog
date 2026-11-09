package com.utp.gajog.support;

import com.utp.gajog.enums.*;
import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import java.util.*;

/** Implementaciones en memoria con datos reales del modelo, usadas únicamente en src/test. */
public final class RepositoriosExamen {
    private RepositoriosExamen() { }

    public static class Alumnos extends RepositorioEnMemoria<Alumno> implements AlumnoRepository {
        public Alumnos() { super(Alumno::getId, Alumno::setId); }
        @Override public Optional<Alumno> findByPersona_Dni(String dni) {
            return datos.values().stream().filter(a -> a.getPersona().getDni().equals(dni)).findFirst();
        }
        @Override public List<Alumno> findByEstado(EstadoAlumno estado) {
            return datos.values().stream().filter(a -> a.getEstado() == estado).toList();
        }
    }

    public static class Cinturones extends RepositorioEnMemoria<Cinturon> implements CinturonRepository {
        public Cinturones() { super(Cinturon::getId, Cinturon::setId); }
        @Override public List<Cinturon> findAllByOrderByOrdenAsc() {
            return datos.values().stream().sorted(Comparator.comparing(Cinturon::getOrden)).toList();
        }
        @Override public Optional<Cinturon> findByNombreIgnoreCase(String nombre) {
            return datos.values().stream().filter(c -> c.getNombre().equalsIgnoreCase(nombre)).findFirst();
        }
    }

    public static class Examenes extends RepositorioEnMemoria<Examen> implements ExamenRepository {
        public boolean disponibleParaLecturaConBloqueo = true;
        public Examenes() { super(Examen::getId, Examen::setId); }
        @Override public List<Examen> findByAlumno_IdOrderByIdDesc(Long alumnoId) {
            return datos.values().stream().filter(e -> e.getAlumno().getId().equals(alumnoId))
                .sorted(Comparator.comparing(Examen::getId).reversed()).toList();
        }
        @Override public Optional<Examen> findFirstByAlumno_IdOrderByIdDesc(Long alumnoId) {
            return findByAlumno_IdOrderByIdDesc(alumnoId).stream().findFirst();
        }
        @Override public Optional<Examen> findFirstByAlumno_IdAndResultadoOrderByIdDesc(Long alumnoId, ResultadoExamen resultado) {
            return findByAlumno_IdOrderByIdDesc(alumnoId).stream().filter(e -> e.getResultado() == resultado).findFirst();
        }
        @Override public Optional<Examen> findByIdConBloqueo(Long id) {
            return disponibleParaLecturaConBloqueo ? findById(id) : Optional.empty();
        }
    }

    public static class Matriculas extends RepositorioEnMemoria<Matricula> implements MatriculaRepository {
        public Matriculas() { super(Matricula::getId, Matricula::setId); }
        @Override public List<Matricula> findByAlumno_IdAndEstado(Long alumnoId, EstadoMatricula estado) {
            return datos.values().stream().filter(m -> m.getAlumno().getId().equals(alumnoId) && m.getEstado() == estado).toList();
        }
        @Override public List<Matricula> buscarParaListado(String busqueda, Long categoriaId, Long grupoId, EstadoMatricula estado) {
            if (!busqueda.isEmpty() || categoriaId != null || grupoId != null) {
                throw new UnsupportedOperationException("Estos filtros no se utilizan en la pantalla Exámenes");
            }
            return datos.values().stream().filter(m -> m.getEstado() == estado).toList();
        }
        @Override public List<Matricula> findByGrupo_IdAndEstado(Long grupoId, EstadoMatricula estado) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
        @Override public long countByGrupo_IdAndEstado(Long grupoId, EstadoMatricula estado) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
        @Override public boolean existsByAlumno_IdAndGrupo_IdAndEstado(Long alumnoId, Long grupoId, EstadoMatricula estado) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
    }

    public static class Pagos extends RepositorioEnMemoria<Pago> implements PagoRepository {
        public Pagos() { super(Pago::getId, Pago::setId); }
        @Override public List<Pago> findByExamen_IdAndEstado(Long examenId, EstadoPago estado) {
            return datos.values().stream().filter(p -> p.getExamen() != null
                && p.getExamen().getId().equals(examenId) && p.getEstado() == estado).toList();
        }
        @Override public List<Pago> findByAlumno_IdOrderByFechaPagoDesc(Long alumnoId) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
        @Override public List<Pago> findByMensualidad_IdAndEstado(Long mensualidadId, EstadoPago estado) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
        @Override public List<Pago> findByMensualidad_AnioAndMensualidad_MesAndTipoPagoAndEstadoOrderByFechaPagoDesc(
                Integer anio, Integer mes, TipoPago tipo, EstadoPago estado) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
    }

    public static class Tarifas extends RepositorioEnMemoria<Tarifa> implements TarifaRepository {
        public Tarifas() { super(Tarifa::getId, Tarifa::setId); }
        @Override public List<Tarifa> findByTipoPagoAndActivoTrue(TipoPago tipo) {
            return datos.values().stream().filter(t -> t.getTipoPago() == tipo && Boolean.TRUE.equals(t.getActivo())).toList();
        }
    }

    public static class Asistencias extends RepositorioEnMemoria<Asistencia> implements AsistenciaRepository {
        public Asistencias() { super(Asistencia::getId, Asistencia::setId); }
        @Override public List<Asistencia> findByAlumno_IdOrderByFechaRegistroDesc(Long alumnoId) {
            return datos.values().stream().filter(a -> a.getAlumno().getId().equals(alumnoId)).toList();
        }
        @Override public Optional<Asistencia> findBySesion_IdAndAlumno_Id(Long sesionId, Long alumnoId) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
        @Override public List<Asistencia> findBySesion_Id(Long sesionId) {
            throw new UnsupportedOperationException("Consulta ajena a Exámenes");
        }
    }

    public static class Criterios extends RepositorioEnMemoria<CriterioExamen> implements CriterioExamenRepository {
        public Criterios() { super(CriterioExamen::getId, CriterioExamen::setId); }
        @Override public List<CriterioExamen> findAllByOrderByIdAsc() {
            return datos.values().stream().sorted(Comparator.comparing(CriterioExamen::getId)).toList();
        }
    }

    public static class Detalles extends RepositorioEnMemoria<DetalleExamen> implements DetalleExamenRepository {
        public Detalles() { super(DetalleExamen::getId, DetalleExamen::setId); }
        @Override public List<DetalleExamen> findByExamen_IdOrderByCriterio_IdAsc(Long examenId) {
            return datos.values().stream().filter(d -> d.getExamen().getId().equals(examenId))
                .sorted(Comparator.comparing(d -> d.getCriterio().getId())).toList();
        }
    }

    public static class Historial extends RepositorioEnMemoria<HistorialCinturones> implements HistorialCinturonesRepository {
        public Historial() { super(HistorialCinturones::getId, HistorialCinturones::setId); }
        @Override public List<HistorialCinturones> findByAlumno_IdOrderByFechaObtencionDescIdDesc(Long alumnoId) {
            return datos.values().stream().filter(h -> h.getAlumno().getId().equals(alumnoId))
                .sorted(Comparator.comparing(HistorialCinturones::getFechaObtencion)
                    .thenComparing(HistorialCinturones::getId).reversed()).toList();
        }
    }
}
