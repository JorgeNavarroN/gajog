package com.utp.gajog.dto;

import com.utp.gajog.model.*;
import java.util.List;

public record ExpedienteAlumnoDTO(
        Alumno alumno,
        int edadActual,
        boolean menorDeEdad,
        Matricula matriculaActiva,
        Categoria categoriaSugeridaPorEdad,
        boolean requiereReasignacionCategoria,
        List<HistorialCinturones> historialCinturones,
        List<Mensualidad> mensualidades,
        List<Asistencia> ultimasAsistencias,
        int porcentajeAsistencia,
        long totalSesionesEvaluadas,
        long totalAsistenciasEfectivas,
        boolean elegibleExamenPorAsistencia,
        int asistenciaMinimaRequerida,
        long mesesAntiguedad,
        int mesesMinimosRequeridos,
        List<ObservacionProgreso> observaciones,
        List<Instructor> instructores,
        List<Horario> horariosReasignacion
) {}