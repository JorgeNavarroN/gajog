package com.utp.gajog.service;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final AlumnoRepository alumnoRepository;
    private final CategoriaRepository categoriaRepository;
    private final HorarioRepository horarioRepository;

    public MatriculaService(
            MatriculaRepository matriculaRepository,
            AlumnoRepository alumnoRepository,
            CategoriaRepository categoriaRepository,
            HorarioRepository horarioRepository
    ) {
        this.matriculaRepository = matriculaRepository;
        this.alumnoRepository = alumnoRepository;
        this.categoriaRepository = categoriaRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional
    public Matricula registrarMatricula(Long alumnoId, Long categoriaId, Long horarioId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new IllegalArgumentException("Alumno no encontrado"));

        if (alumno.getEstado() != EstadoAlumno.ACTIVO) {
            throw new IllegalStateException("Solo se pueden matricular alumnos en estado ACTIVO");
        }

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalStateException("La categoría seleccionada no está activa");
        }

        Horario horario = horarioRepository.findById(horarioId)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado"));

        if (!horario.getCategoria().getId().equals(categoria.getId())) {
            throw new IllegalArgumentException("El horario seleccionado no pertenece a la categoría indicada");
        }

        LocalDate fechaNacimiento = alumno.getPersona().getFechaNacimiento();
        if (fechaNacimiento != null) {
            int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
            if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) {
                throw new IllegalArgumentException(
                        "La edad del alumno (" + edad + " años) no está en el rango de la categoría "
                        + categoria.getNombre() + " (" + categoria.getEdadMinima() + " - " + categoria.getEdadMaxima() + " años)"
                );
            }
        }

        Matricula matricula = new Matricula();
        matricula.setAlumno(alumno);
        matricula.setCategoria(categoria);
        matricula.setHorario(horario);
        matricula.setFechaMatricula(LocalDate.now());
        matricula.setEstado(EstadoMatricula.ACTIVA);

        return matriculaRepository.save(matricula);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarPorAlumno(Long alumnoId) {
        return matriculaRepository.findByAlumnoId(alumnoId);
    }

    @Transactional
    public Matricula cambiarEstado(Long matriculaId, EstadoMatricula nuevoEstado) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula no encontrada"));
        matricula.setEstado(nuevoEstado);
        return matriculaRepository.save(matricula);
    }
}