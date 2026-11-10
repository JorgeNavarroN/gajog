/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.model.Categoria;
import com.utp.gajog.repository.CategoriaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author JORGE
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @ParameterizedTest(name = "{0} años corresponde a {1}")
    @CsvSource({
        "7, Infantil",
        "11, Infantil",
        "12, Juvenil",
        "16, Juvenil",
        "17, Adultos",
        "120, Adultos"
    })
    @DisplayName("Asigna categoría en los límites de edad")
    void asignaCategoriaSegunEdad(int edad, String nombreEsperado) {
        prepararCategorias();

        LocalDate nacimiento = LocalDate.now().minusYears(edad);

        Categoria resultado = categoriaService
                .determinarPorFechaNacimiento(nacimiento);

        assertEquals(nombreEsperado, resultado.getNombre());
    }

    @Test
    @DisplayName("Mantiene Infantil hasta el día anterior a cumplir 12")
    void noAdelantaCategoriaAntesDelCumpleanos() {
        prepararCategorias();

        // Cumple 12 mañana: todavía tiene 11 años.
        LocalDate nacimiento = LocalDate.now()
                .minusYears(12)
                .plusDays(1);

        Categoria resultado = categoriaService
                .determinarPorFechaNacimiento(nacimiento);

        assertEquals("Infantil", resultado.getNombre());
    }

    @Test
    @DisplayName("Rechaza una fecha de nacimiento vacía")
    void rechazaFechaNula() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService
                        .determinarPorFechaNacimiento(null));

        assertEquals(
                "La fecha de nacimiento es obligatoria.",
                error.getMessage());

        verifyNoInteractions(categoriaRepository);
    }

    @Test
    @DisplayName("Rechaza una fecha de nacimiento futura")
    void rechazaFechaFutura() {
        LocalDate nacimiento = LocalDate.now().plusDays(1);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService
                        .determinarPorFechaNacimiento(nacimiento));

        assertEquals(
                "La fecha de nacimiento no puede ser futura.",
                error.getMessage());

        verifyNoInteractions(categoriaRepository);
    }

    @ParameterizedTest(name = "Rechaza edad fuera de rango: {0}")
    @CsvSource({"6", "121"})
    @DisplayName("Rechaza edades sin categoría disponible")
    void rechazaEdadFueraDeRango(int edad) {
        prepararCategorias();

        LocalDate nacimiento = LocalDate.now().minusYears(edad);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService
                        .determinarPorFechaNacimiento(nacimiento));

        assertEquals(
                "No existe una categoría activa para la edad de "
                + edad + " años.",
                error.getMessage());
    }

    @Test
    @DisplayName("Detecta categorías con rangos superpuestos")
    void rechazaCategoriasSuperpuestas() {
        when(categoriaRepository.findByActivoTrue())
                .thenReturn(List.of(
                        crearCategoria("Infantil", 7, 12),
                        crearCategoria("Juvenil", 12, 16)));

        LocalDate nacimiento = LocalDate.now().minusYears(12);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> categoriaService
                        .determinarPorFechaNacimiento(nacimiento));

        assertEquals(
                "Hay categorías activas con rangos de edad "
                + "superpuestos. Revisa su configuración.",
                error.getMessage());
    }

    private void prepararCategorias() {
        when(categoriaRepository.findByActivoTrue())
                .thenReturn(List.of(
                        crearCategoria("Infantil", 7, 11),
                        crearCategoria("Juvenil", 12, 16),
                        crearCategoria("Adultos", 17, 120)));
    }

    private Categoria crearCategoria(
            String nombre, int edadMinima, int edadMaxima) {

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setEdadMinima(edadMinima);
        categoria.setEdadMaxima(edadMaxima);
        categoria.setActivo(true);

        return categoria;
    }
}
