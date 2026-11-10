/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.service;

import com.utp.gajog.dto.GrupoForm;
import com.utp.gajog.enums.DiaSemana;
import com.utp.gajog.model.Categoria;
import com.utp.gajog.model.Grupo;
import com.utp.gajog.model.Horario;
import com.utp.gajog.model.Instructor;
import com.utp.gajog.repository.CategoriaRepository;
import com.utp.gajog.repository.GrupoRepository;
import com.utp.gajog.repository.HorarioRepository;
import com.utp.gajog.repository.InstructorRepository;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class GestionGrupoServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @InjectMocks
    private GestionGrupoService gestionGrupoService;

    private GrupoForm formulario;
    private Categoria categoria;
    private Instructor instructor;

    @BeforeEach
    void prepararDatos() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setActivo(true);

        instructor = new Instructor();
        instructor.setId(2L);

        formulario = new GrupoForm();
        formulario.setNombre("Infantil A");
        formulario.setCategoriaId(1L);
        formulario.setInstructorId(2L);
        formulario.setCupoMaximo(15);

        // GrupoForm ya crea una fila de horario vacía.
        GrupoForm.HorarioForm horario = formulario.getHorarios().get(0);
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(16, 0));
        horario.setHoraFin(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("Crea un grupo con instructor y horario válido")
    void creaGrupoConHorarioValido() {
        // Preparar dependencias.
        prepararConsultasValidas();

        when(horarioRepository.contarCrucesInstructor(
                2L, DiaSemana.LUNES,
                LocalTime.of(16, 0), LocalTime.of(17, 0)))
                .thenReturn(0L);

        // Simula el ID que asignaría la base de datos.
        when(grupoRepository.save(any(Grupo.class)))
                .thenAnswer(invocacion -> {
                    Grupo grupo = invocacion.getArgument(0);
                    grupo.setId(30L);
                    return grupo;
                });

        // Ejecutar.
        Long resultado = gestionGrupoService.crear(formulario);

        // Capturar los objetos enviados a guardar.
        ArgumentCaptor<Grupo> grupoCaptor
                = ArgumentCaptor.forClass(Grupo.class);

        ArgumentCaptor<Horario> horarioCaptor
                = ArgumentCaptor.forClass(Horario.class);

        verify(grupoRepository).save(grupoCaptor.capture());
        verify(horarioRepository).save(horarioCaptor.capture());

        Grupo grupoGuardado = grupoCaptor.getValue();
        Horario horarioGuardado = horarioCaptor.getValue();

        assertEquals(Long.valueOf(30L), resultado);
        assertEquals("Infantil A", grupoGuardado.getNombre());
        assertEquals(Integer.valueOf(15), grupoGuardado.getCupoMaximo());
        assertEquals(Boolean.TRUE, grupoGuardado.getActivo());
        assertSame(categoria, grupoGuardado.getCategoria());
        assertSame(instructor, grupoGuardado.getInstructor());

        assertSame(grupoGuardado, horarioGuardado.getGrupo());
        assertEquals(DiaSemana.LUNES, horarioGuardado.getDiaSemana());
        assertEquals(LocalTime.of(16, 0), horarioGuardado.getHoraInicio());
        assertEquals(LocalTime.of(17, 0), horarioGuardado.getHoraFin());
        assertEquals(1, grupoGuardado.getHorarios().size());
        assertSame(horarioGuardado, grupoGuardado.getHorarios().get(0));
    }

    @Test
    @DisplayName("Rechaza una hora final igual o anterior a la inicial")
    void rechazaHoraFinalInvalida() {
        for (LocalTime fin : new LocalTime[]{
            LocalTime.of(16, 0),
            LocalTime.of(15, 0)
        }) {
            formulario.getHorarios().get(0).setHoraFin(fin);

            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> gestionGrupoService.crear(formulario));

            assertEquals(
                    "La hora final debe ser posterior a la inicial.",
                    error.getMessage());
        }

        // La validación debe detenerse antes de consultar o guardar.
        verifyNoInteractions(
                grupoRepository,
                categoriaRepository,
                instructorRepository,
                horarioRepository);
    }

    @Test
    @DisplayName("Rechaza horarios superpuestos dentro del nuevo grupo")
    void rechazaHorariosSuperpuestos() {
        GrupoForm.HorarioForm segundo = new GrupoForm.HorarioForm();
        segundo.setDiaSemana(DiaSemana.LUNES);
        segundo.setHoraInicio(LocalTime.of(16, 30));
        segundo.setHoraFin(LocalTime.of(17, 30));

        formulario.getHorarios().add(segundo);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> gestionGrupoService.crear(formulario));

        assertEquals(
                "Hay horarios superpuestos dentro del grupo.",
                error.getMessage());

        verifyNoInteractions(
                grupoRepository,
                categoriaRepository,
                instructorRepository,
                horarioRepository);
    }

    @Test
    @DisplayName("Rechaza un horario ocupado por otra clase del instructor")
    void rechazaCruceConOtraClaseDelInstructor() {
        prepararConsultasValidas();

        when(horarioRepository.contarCrucesInstructor(
                2L, DiaSemana.LUNES,
                LocalTime.of(16, 0), LocalTime.of(17, 0)))
                .thenReturn(1L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> gestionGrupoService.crear(formulario));

        assertTrue(error.getMessage().contains(
                "El instructor ya tiene una clase"));

        verify(grupoRepository, never()).save(any(Grupo.class));
        verify(horarioRepository, never()).save(any(Horario.class));
    }

    private void prepararConsultasValidas() {
        when(instructorRepository.findByIdConBloqueo(2L))
                .thenReturn(Optional.of(instructor));

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(grupoRepository.existsByNombreIgnoreCase("Infantil A"))
                .thenReturn(false);
    }
}
