package com.utp.gajog.dto;

import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Examen;


public class FilaExamenDTO {

    private final Alumno alumno;
    private final Examen examen;
    private final boolean pagado;
    private final String estado;

    public FilaExamenDTO(
            Alumno alumno,
            Examen examen,
            boolean pagado,
            String estado) {
        this.alumno = alumno;
        this.examen = examen;
        this.pagado = pagado;
        this.estado = estado;
    }

    public Alumno getAlumno() {
        return alumno;
    }

    public Examen getExamen() {
        return examen;
    }

    public boolean isPagado() {
        return pagado;
    }

    public String getEstado() {
        return estado;
    }

    public String getInicial() {
        String nombres = alumno.getPersona().getNombres();

        return nombres == null || nombres.isBlank()
                ? "?"
                : nombres.substring(0, 1).toUpperCase();
    }
}
