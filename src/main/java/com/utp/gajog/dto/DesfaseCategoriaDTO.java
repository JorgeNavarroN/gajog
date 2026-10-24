package com.utp.gajog.dto;

public record DesfaseCategoriaDTO(
        Long alumnoId,
        Long matriculaId,
        String nombreAlumno,
        int edadActual,
        String categoriaActualNombre,
        int edadMinActual,
        int edadMaxActual,
        Long categoriaSugeridaId,
        String categoriaSugeridaNombre
) {}