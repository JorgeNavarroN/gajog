package com.utp.gajog.dto;

import com.utp.gajog.model.EstadoAsistencia;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record FiltroHistorialAsistenciaDTO(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        Long categoriaId,
        Long horarioId,
        String q,
        EstadoAsistencia estado
) {}