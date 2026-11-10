/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.enums.EstadoMatricula;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Categoria;
import com.utp.gajog.model.Grupo;
import com.utp.gajog.model.Horario;
import com.utp.gajog.model.Matricula;
import com.utp.gajog.model.Persona;
import com.utp.gajog.repository.AlumnoRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.repository.MatriculaRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MatriculaServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private MatriculaService matriculaService;

    private Alumno alumno;
    private Grupo grupo;

    @BeforeEach
    void prepararDatos() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Infantil");

        Persona persona = new Persona();
        persona.setFechaNacimiento(LocalDate.of(2016, 5, 10));

        alumno = new Alumno();
        alumno.setId(10L);
        alumno.setPersona(persona);
        alumno.setEstado(EstadoAlumno.ACTIVO);

        grupo = new Grupo();
        grupo.setId(20L);
        grupo.setCategoria(categoria);
        grupo.setActivo(true);
        grupo.setCupoMaximo(15);

        Horario horario = new Horario();
        horario.setGrupo(grupo);
        grupo.getHorarios().add(horario);

        when(grupoRepository.findByIdConBloqueo(20L))
                .thenReturn(Optional.of(grupo));

        when(alumnoRepository.findById(10L))
                .thenReturn(Optional.of(alumno));
    }

    @Test
    @DisplayName("Registra una matrícula activa cuando hay cupos")
    void registraMatriculaConCupoDisponible() {
        prepararCategoriaCompatible();
        // Preparar: quedan cinco vacantes.
        when(matriculaRepository.existsByAlumno_IdAndGrupo_IdAndEstado(
                10L, 20L, EstadoMatricula.ACTIVA))
                .thenReturn(false);

        when(matriculaRepository.countByGrupo_IdAndEstado(
                20L, EstadoMatricula.ACTIVA))
                .thenReturn(10L);

        when(matriculaRepository.save(any(Matricula.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Ejecutar.
        Matricula resultado = matriculaService.registrar(10L, 20L);

        // Comprobar los datos enviados a guardar.
        assertNotNull(resultado);
        assertSame(alumno, resultado.getAlumno());
        assertSame(grupo, resultado.getGrupo());
        assertEquals(EstadoMatricula.ACTIVA, resultado.getEstado());
        assertNotNull(resultado.getFechaMatricula());

        verify(matriculaRepository).save(resultado);
    }

    @Test
    @DisplayName("Rechaza la matrícula cuando el grupo está lleno")
    void rechazaMatriculaCuandoNoHayCupos() {
        prepararCategoriaCompatible();
        // Preparar: todos los cupos están ocupados.
        when(matriculaRepository.existsByAlumno_IdAndGrupo_IdAndEstado(
                10L, 20L, EstadoMatricula.ACTIVA))
                .thenReturn(false);

        when(matriculaRepository.countByGrupo_IdAndEstado(
                20L, EstadoMatricula.ACTIVA))
                .thenReturn(15L);

        // Ejecutar y comprobar la excepción.
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> matriculaService.registrar(10L, 20L));

        assertEquals(
                "El grupo ya no tiene vacantes disponibles.",
                error.getMessage());

        // Una matrícula rechazada no debe guardarse.
        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    @DisplayName("Rechaza una matrícula activa duplicada en el grupo")
    void rechazaMatriculaDuplicada() {
        prepararCategoriaCompatible();
        // Preparar: el alumno ya está matriculado en este grupo.
        when(matriculaRepository.existsByAlumno_IdAndGrupo_IdAndEstado(
                10L, 20L, EstadoMatricula.ACTIVA))
                .thenReturn(true);

        // Ejecutar y comprobar.
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> matriculaService.registrar(10L, 20L));

        assertEquals(
                "El alumno ya tiene una matrícula activa en este grupo.",
                error.getMessage());

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    @DisplayName("Rechaza un grupo incompatible con la categoría del alumno")
    void rechazaCategoriaIncompatible() {
        // El alumno pertenece a otra categoría.
        Categoria categoriaAlumno = new Categoria();
        categoriaAlumno.setId(2L);
        categoriaAlumno.setNombre("Juvenil");

        when(categoriaService.determinarPorFechaNacimiento(
                alumno.getPersona().getFechaNacimiento()))
                .thenReturn(categoriaAlumno);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> matriculaService.registrar(10L, 20L));

        assertEquals(
                "El grupo no corresponde a la categoría del alumno.",
                error.getMessage());

        verify(matriculaRepository, never()).save(any(Matricula.class));
    }

    @Test
    @DisplayName("Rechaza la matrícula en un grupo inactivo")
    void rechazaGrupoInactivo() {
        grupo.setActivo(false);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> matriculaService.registrar(10L, 20L));

        assertEquals(
                "El grupo no está activo.",
                error.getMessage());

        verifyNoInteractions(categoriaService, matriculaRepository);
    }

    @Test
    @DisplayName("Rechaza la matrícula en un grupo sin horarios")
    void rechazaGrupoSinHorarios() {
        grupo.getHorarios().clear();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> matriculaService.registrar(10L, 20L));

        assertEquals(
                "El grupo no tiene horarios asignados.",
                error.getMessage());

        verifyNoInteractions(categoriaService, matriculaRepository);
    }

    private void prepararCategoriaCompatible() {
        when(categoriaService.determinarPorFechaNacimiento(
                alumno.getPersona().getFechaNacimiento()))
                .thenReturn(grupo.getCategoria());
    }
}
