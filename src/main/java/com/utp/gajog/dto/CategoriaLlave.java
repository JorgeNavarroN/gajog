package com.utp.gajog.dto;

import com.utp.gajog.model.Combate;
import java.util.List;

/** Llave de una categoría: lista de rondas con sus combates. */
public record CategoriaLlave(String categoria, List<Ronda> rondas) {

    public record Ronda(String nombre, List<Combate> combates) {
    }
}
