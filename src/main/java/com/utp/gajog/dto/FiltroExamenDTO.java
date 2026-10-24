package com.utp.gajog.dto;

import com.utp.gajog.model.ResultadoExamen;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record FiltroExamenDTO(
        String q,
        Long cinturonObjetivoId,
        ResultadoExamen resultado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
) {}