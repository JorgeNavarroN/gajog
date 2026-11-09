package com.utp.gajog.util;

import java.math.BigDecimal;

/**
 * Reglas para asignar la categoría de un competidor según su edad,
 * su cinturón y su peso. Es una clase sin dependencias de Spring para
 * poder probarla fácilmente con JUnit.
 */
public final class CategoriaTorneo {

    private CategoriaTorneo() {
    }

    /** Grupo de edad: coincide con las categorías de la academia. */
    public static String grupoEdad(int edad) {
        if (edad < 4) {
            throw new IllegalArgumentException("Edad mínima para competir: 4 años.");
        }
        if (edad <= 11) {
            return "Infantil";
        }
        if (edad <= 16) {
            return "Juvenil";
        }
        return "Adulto";
    }

    /** Nivel según el orden del cinturón (1 = blanco, el más bajo). */
    public static String nivelCinturon(int ordenCinturon) {
        if (ordenCinturon <= 3) {
            return "Principiante";
        }
        if (ordenCinturon <= 6) {
            return "Intermedio";
        }
        return "Avanzado";
    }

    /** Categoría de peso en kilogramos. */
    public static String categoriaPeso(BigDecimal pesoKg) {
        if (pesoKg == null || pesoKg.signum() <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero.");
        }
        if (pesoKg.compareTo(new BigDecimal("45")) < 0) {
            return "Ligero";
        }
        if (pesoKg.compareTo(new BigDecimal("60")) <= 0) {
            return "Medio";
        }
        return "Pesado";
    }

    /** Texto final de categoría, por ejemplo "Juvenil · Intermedio · Medio". */
    public static String calcular(int edad, int ordenCinturon, BigDecimal pesoKg) {
        return grupoEdad(edad) + " · " + nivelCinturon(ordenCinturon)
                + " · " + categoriaPeso(pesoKg);
    }
}
