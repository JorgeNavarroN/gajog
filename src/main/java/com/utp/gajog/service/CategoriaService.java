package com.utp.gajog.service;

import com.utp.gajog.model.Categoria;
import com.utp.gajog.repository.CategoriaRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author JORGE
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Categoria determinarPorFechaNacimiento(
            LocalDate fechaNacimiento) {

        LocalDate hoy = LocalDate.now();

        if (fechaNacimiento == null) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento es obligatoria.");
        }

        if (fechaNacimiento.isAfter(hoy)) {
            throw new IllegalArgumentException(
                    "La fecha de nacimiento no puede ser futura.");
        }

        int edad = Period.between(fechaNacimiento, hoy).getYears();

        List<Categoria> coincidencias = categoriaRepository
                .findByActivoTrue()
                .stream()
                .filter(categoria
                        -> edad >= categoria.getEdadMinima()
                        && edad <= categoria.getEdadMaxima())
                .toList();

        if (coincidencias.isEmpty()) {
            throw new IllegalArgumentException(
                    "No existe una categoría activa para la edad de "
                    + edad + " años.");
        }

        if (coincidencias.size() > 1) {
            throw new IllegalStateException(
                    "Hay categorías activas con rangos de edad "
                    + "superpuestos. Revisa su configuración.");
        }

        return coincidencias.get(0);
    }
}
