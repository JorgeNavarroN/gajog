package com.utp.gajog.config;

import com.utp.gajog.model.*;
import com.utp.gajog.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
            UsuarioRepository usuarioRepository,
            TarifaRepository tarifaRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        return args -> {
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

            Instructor instructorPorDefecto;
            if (instructorRepo.count() == 0) {
                Persona pInstructor = personaRepo.findByDni("10000001").orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setDni("10000001");
                    nueva.setNombres("Sabonim Carlos");
                    nueva.setApellidos("Mendoza");
                    nueva.setTelefono("999888777");
                    nueva.setCorreo("carlos.mendoza@gajogtkd.com");
                    return personaRepo.save(nueva);
                });

                Instructor ins = new Instructor();
                ins.setPersona(pInstructor);
                instructorPorDefecto = instructorRepo.save(ins);
            } else {
                instructorPorDefecto = instructorRepo.findAllConPersona().getFirst();
            }

            if (categoriaRepo.count() == 0) {
                Categoria infantil = crearCategoria("Infantil", 5, 11);
                Categoria juvenil = crearCategoria("Juvenil", 12, 17);
                Categoria adultos = crearCategoria("Adultos", 18, 60);
                categoriaRepo.saveAll(List.of(infantil, juvenil, adultos));

                horarioRepo.saveAll(List.of(
                        crearHorario(infantil, DiaSemana.LUNES, LocalTime.of(16, 0), LocalTime.of(17, 30), 20, "Sala A", instructorPorDefecto),
                        crearHorario(infantil, DiaSemana.MIERCOLES, LocalTime.of(16, 0), LocalTime.of(17, 30), 20, "Sala A", instructorPorDefecto),
                        crearHorario(juvenil, DiaSemana.MARTES, LocalTime.of(17, 30), LocalTime.of(19, 0), 20, "Sala A", instructorPorDefecto),
                        crearHorario(juvenil, DiaSemana.JUEVES, LocalTime.of(17, 30), LocalTime.of(19, 0), 20, "Sala A", instructorPorDefecto),
                        crearHorario(adultos, DiaSemana.VIERNES, LocalTime.of(19, 0), LocalTime.of(20, 30), 25, "Sala B", instructorPorDefecto),
                        crearHorario(adultos, DiaSemana.SABADO, LocalTime.of(9, 0), LocalTime.of(11, 0), 25, "Sala B", instructorPorDefecto)
                ));
            }

            LocalDate hoy = LocalDate.now(clock);
            for (Categoria cat : categoriaRepo.findAll()) {
                if (tarifaRepository.findFirstByCategoriaIdAndActivoTrueOrderByFechaDesdeDesc(cat.getId()).isEmpty()) {
                    BigDecimal montoInicial = switch (cat.getNombre().toUpperCase()) {
                        case "INFANTIL" -> new BigDecimal("100.00");
                        case "JUVENIL" -> new BigDecimal("120.00");
                        default -> new BigDecimal("140.00");
                    };
                    Tarifa tarifa = new Tarifa();
                    tarifa.setCategoria(cat);
                    tarifa.setMontoVigente(montoInicial);
                    tarifa.setFechaDesde(hoy);
                    tarifa.setActivo(true);
                    tarifaRepository.save(tarifa);
                }
            }

            if (criterioRepo.count() == 0) {
                criterioRepo.saveAll(List.of(
                        crearCriterio("Básicos y Técnicas de Mano/Patada", new BigDecimal("25.00")),
                        crearCriterio("Poomsae (Formas)", new BigDecimal("30.00")),
                        crearCriterio("Kyorugi (Combate)", new BigDecimal("25.00")),
                        crearCriterio("Rompimiento y Teoría", new BigDecimal("20.00"))
                ));
            }

            // 1. Usuario Admin inicial (con cambio obligatorio de contraseña)
            if (!usuarioRepository.existsByNombreUsuario("admin")) {
                Persona pAdmin = personaRepo.findByDni("00000000").orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setDni("00000000");
                    nueva.setNombres("Administrador");
                    nueva.setApellidos("General");
                    return personaRepo.save(nueva);
                });

                Usuario admin = new Usuario(
                        pAdmin,
                        "admin",
                        passwordEncoder.encode("admin123"),
                        RolUsuario.ADMINISTRADOR
                );
                admin.setRequiereCambioPassword(true);
                usuarioRepository.save(admin);
            }

            // 2. Usuario Recepción inicial
            if (!usuarioRepository.existsByNombreUsuario("recepcion")) {
                Persona pRec = personaRepo.findByDni("10000002").orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setDni("10000002");
                    nueva.setNombres("Recepción");
                    nueva.setApellidos("Academia");
                    return personaRepo.save(nueva);
                });

                Usuario recepcion = new Usuario(
                        pRec,
                        "recepcion",
                        passwordEncoder.encode("recep1234"),
                        RolUsuario.RECEPCION
                );
                usuarioRepository.save(recepcion);
            }

            // 3. Usuario Instructor inicial
            if (!usuarioRepository.existsByNombreUsuario("instructor")) {
                Usuario uInstructor = new Usuario(
                        instructorPorDefecto.getPersona(),
                        "instructor",
                        passwordEncoder.encode("sabonim1234"),
                        RolUsuario.INSTRUCTOR
                );
                usuarioRepository.save(uInstructor);
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

    private Horario crearHorario(
            Categoria cat,
            DiaSemana dia,
            LocalTime inicio,
            LocalTime fin,
            int cupoMaximo,
            String sala,
            Instructor instructor
    ) {
        Horario h = new Horario();
        h.setCategoria(cat);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        h.setCupoMaximo(cupoMaximo);
        h.setSala(sala);
        h.setInstructor(instructor);
        return h;
    }

    private CriterioExamen crearCriterio(String nombre, BigDecimal puntajeMaximo) {
        CriterioExamen crit = new CriterioExamen();
        crit.setNombre(nombre);
        crit.setPuntajeMaximo(puntajeMaximo);
        return crit;
    }
}