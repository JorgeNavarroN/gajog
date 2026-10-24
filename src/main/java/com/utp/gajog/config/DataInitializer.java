package com.utp.gajog.config;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner inicializarCatalogos(
            CinturonRepository cinturonRepo,
            CategoriaRepository categoriaRepo,
            HorarioRepository horarioRepo,
            CriterioExamenRepository criterioRepo,
            InstructorRepository instructorRepo,
            PersonaRepository personaRepo,
            UsuarioRepository usuarioRepository
    ) {
        return args -> {
            // 1. Cinturones iniciales
            if (cinturonRepo.count() == 0) {
                cinturonRepo.saveAll(List.of(
                        crearCinturon("Blanco (10° Gup)", "BLANCO", 1),
                        crearCinturon("Amarillo (8° Gup)", "AMARILLO", 2),
                        crearCinturon("Verde (6° Gup)", "VERDE", 3),
                        crearCinturon("Azul (4° Gup)", "AZUL", 4),
                        crearCinturon("Rojo (2° Gup)", "ROJO", 5),
                        crearCinturon("Negro (1° Dan)", "NEGRO", 6)
                ));
            }

            // 2. Categorías y Horarios iniciales
            if (categoriaRepo.count() == 0) {
                Categoria infantil = crearCategoria("Infantil", 5, 11);
                Categoria juvenil = crearCategoria("Juvenil", 12, 17);
                Categoria adultos = crearCategoria("Adultos", 18, 60);
                categoriaRepo.saveAll(List.of(infantil, juvenil, adultos));

                horarioRepo.saveAll(List.of(
                        crearHorario(infantil, DiaSemana.LUNES, LocalTime.of(16, 0), LocalTime.of(17, 30)),
                        crearHorario(infantil, DiaSemana.MIERCOLES, LocalTime.of(16, 0), LocalTime.of(17, 30)),
                        crearHorario(juvenil, DiaSemana.MARTES, LocalTime.of(17, 30), LocalTime.of(19, 0)),
                        crearHorario(juvenil, DiaSemana.JUEVES, LocalTime.of(17, 30), LocalTime.of(19, 0)),
                        crearHorario(adultos, DiaSemana.VIERNES, LocalTime.of(19, 0), LocalTime.of(20, 30)),
                        crearHorario(adultos, DiaSemana.SABADO, LocalTime.of(9, 0), LocalTime.of(11, 0))
                ));
            }

            // 3. Criterios de Examen (Suman 100 puntos en total)
            if (criterioRepo.count() == 0) {
                criterioRepo.saveAll(List.of(
                        crearCriterio("Básicos y Técnicas de Mano/Patada", new BigDecimal("25.00")),
                        crearCriterio("Poomsae (Formas)", new BigDecimal("30.00")),
                        crearCriterio("Kyorugi (Combate)", new BigDecimal("25.00")),
                        crearCriterio("Rompimiento y Teoría", new BigDecimal("20.00"))
                ));
            }

            // 4. Instructor inicial por defecto
            if (instructorRepo.count() == 0) {

                Persona pInstructor = personaRepo
                        .findByDni("10000001")
                        .orElseGet(() -> {
                            Persona nueva = new Persona();
                            nueva.setDni("10000001");
                            nueva.setNombres("Sabonim Carlos");
                            nueva.setApellidos("Mendoza");
                            nueva.setTelefono("999888777");

                            return personaRepo.save(nueva);
                        });

                Instructor instructor = new Instructor();
                instructor.setPersona(pInstructor);

                instructorRepo.save(instructor);
            }

            // 5. Usuario Administrador por defecto
            if (!usuarioRepository.existsByNombreUsuario("admin")) {

                Persona pAdmin = personaRepo
                        .findByDni("00000000")
                        .orElseGet(() -> {
                            Persona nueva = new Persona();
                            nueva.setDni("00000000");
                            nueva.setNombres("Administrador");
                            nueva.setApellidos("General");

                            return personaRepo.save(nueva);
                        });

                BCryptPasswordEncoder encoder
                        = new BCryptPasswordEncoder();

                Usuario admin = new Usuario(
                        pAdmin,
                        "admin",
                        encoder.encode("admin123"),
                        RolUsuario.ADMINISTRADOR
                );

                usuarioRepository.save(admin);
            }

        };
    }

    private Cinturon crearCinturon(String nombre, String color, int orden) {
        Cinturon c = new Cinturon();
        c.setNombre(nombre);
        c.setColor(color);
        c.setOrden(orden);
        return c;
    }

    private Categoria crearCategoria(String nombre, int edadMin, int edadMax) {
        Categoria cat = new Categoria();
        cat.setNombre(nombre);
        cat.setEdadMinima(edadMin);
        cat.setEdadMaxima(edadMax);
        cat.setActivo(true);
        return cat;
    }

    private Horario crearHorario(Categoria cat, DiaSemana dia, LocalTime inicio, LocalTime fin) {
        Horario h = new Horario();
        h.setCategoria(cat);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        return h;
    }

    private CriterioExamen crearCriterio(String nombre, BigDecimal puntajeMaximo) {
        CriterioExamen crit = new CriterioExamen();
        crit.setNombre(nombre);
        crit.setPuntajeMaximo(puntajeMaximo);
        return crit;
    }
}
