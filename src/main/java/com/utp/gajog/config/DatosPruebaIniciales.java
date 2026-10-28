/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.gajog.config;

import com.utp.gajog.enums.DiaSemana;
import com.utp.gajog.enums.TipoPago;
import com.utp.gajog.model.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JORGE
 */
@Component
@ConditionalOnProperty(
        name = "gajog.datos-prueba.enabled",
        havingValue = "true")
public class DatosPruebaIniciales implements ApplicationRunner {

    private final EntityManager em;

    public DatosPruebaIniciales(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        Categoria infantil = categoria("Infantil", 7, 11);
        Categoria juvenil = categoria("Juvenil", 12, 16);
        Categoria adultos = categoria("Adultos", 17, 120);

        cinturonBlanco();

        tarifa(TipoPago.MATRICULA, "30.00");
        tarifa(TipoPago.MENSUALIDAD, "100.00");

        Instructor instructor = instructorPrueba();

        crearGrupo(
                "Infantil A", infantil, instructor, 20,
                "16:00", "17:00",
                DiaSemana.LUNES, DiaSemana.MIERCOLES);

        crearGrupo(
                "Infantil B", infantil, instructor, 15,
                "16:00", "17:00",
                DiaSemana.MARTES, DiaSemana.JUEVES);

        crearGrupo(
                "Infantil C", infantil, instructor, 20,
                "09:00", "11:00",
                DiaSemana.SABADO);

        crearGrupo(
                "Juvenil A", juvenil, instructor, 20,
                "17:00", "18:00",
                DiaSemana.MARTES, DiaSemana.JUEVES);

        crearGrupo(
                "Juvenil B", juvenil, instructor, 15,
                "17:00", "18:00",
                DiaSemana.LUNES, DiaSemana.MIERCOLES);

        crearGrupo(
                "Juvenil C", juvenil, instructor, 20,
                "11:00", "13:00",
                DiaSemana.SABADO);

        crearGrupo(
                "Adultos noche", adultos, instructor, 20,
                "19:00", "20:00",
                DiaSemana.MARTES, DiaSemana.JUEVES);

        crearGrupo(
                "Adultos mañana", adultos, instructor, 15,
                "07:00", "08:00",
                DiaSemana.LUNES, DiaSemana.MIERCOLES);

        crearGrupo(
                "Adultos sábado", adultos, instructor, 20,
                "18:00", "20:00",
                DiaSemana.SABADO);

        System.out.println(
                "GAJOG: datos de prueba iniciales verificados.");
    }

    private Categoria categoria(String nombre, int minima, int maxima) {
        List<Categoria> existentes = em.createQuery(
                "SELECT c FROM Categoria c "
                + "WHERE LOWER(c.nombre) = LOWER(:nombre)",
                Categoria.class)
                .setParameter("nombre", nombre)
                .getResultList();

        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setEdadMinima(minima);
        categoria.setEdadMaxima(maxima);
        categoria.setActivo(true);

        em.persist(categoria);
        return categoria;
    }

    private void cinturonBlanco() {
        Long cantidad = em.createQuery(
                "SELECT COUNT(c) FROM Cinturon c "
                + "WHERE LOWER(c.nombre) = 'blanco'",
                Long.class)
                .getSingleResult();

        if (cantidad > 0) {
            return;
        }

        Cinturon cinturon = new Cinturon();
        cinturon.setNombre("Blanco");
        cinturon.setColor("#FFFFFF");
        cinturon.setOrden(1);

        em.persist(cinturon);
    }

    private void tarifa(TipoPago tipo, String monto) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));

        Long cantidad = em.createQuery(
                "SELECT COUNT(t) FROM Tarifa t "
                + "WHERE t.tipoPago = :tipo "
                + "AND t.activo = true "
                + "AND t.fechaInicio <= :hoy "
                + "AND (t.fechaFin IS NULL OR t.fechaFin >= :hoy)",
                Long.class)
                .setParameter("tipo", tipo)
                .setParameter("hoy", hoy)
                .getSingleResult();

        if (cantidad > 0) {
            return;
        }

        Tarifa tarifa = new Tarifa();
        tarifa.setTipoPago(tipo);
        tarifa.setMonto(new BigDecimal(monto));
        tarifa.setFechaInicio(hoy);
        tarifa.setFechaFin(null);
        tarifa.setActivo(true);

        em.persist(tarifa);
    }

    private Instructor instructorPrueba() {
        String documento = "DEMO-INSTRUCTOR";

        List<Persona> personas = em.createQuery(
                "SELECT p FROM Persona p WHERE p.dni = :dni",
                Persona.class)
                .setParameter("dni", documento)
                .getResultList();

        Persona persona;

        if (personas.isEmpty()) {
            persona = new Persona();
            persona.setDni(documento);
            persona.setNombres("Carlos");
            persona.setApellidos("Instructor de prueba");

            em.persist(persona);
        } else {
            persona = personas.get(0);
        }

        List<Instructor> instructores = em.createQuery(
                "SELECT i FROM Instructor i WHERE i.persona.id = :personaId",
                Instructor.class)
                .setParameter("personaId", persona.getId())
                .getResultList();

        if (!instructores.isEmpty()) {
            return instructores.get(0);
        }

        Instructor instructor = new Instructor();
        instructor.setPersona(persona);

        em.persist(instructor);
        return instructor;
    }

    private void crearGrupo(
            String nombre,
            Categoria categoria,
            Instructor instructor,
            int cupo,
            String inicio,
            String fin,
            DiaSemana... dias) {

        List<Grupo> existentes = em.createQuery(
                "SELECT g FROM Grupo g "
                + "WHERE LOWER(g.nombre) = LOWER(:nombre)",
                Grupo.class)
                .setParameter("nombre", nombre)
                .getResultList();

        Grupo grupo;

        if (existentes.isEmpty()) {
            grupo = new Grupo();
            grupo.setNombre(nombre);
            grupo.setCategoria(categoria);
            grupo.setInstructor(instructor);
            grupo.setCupoMaximo(cupo);
            grupo.setActivo(true);

            em.persist(grupo);
        } else {
            grupo = existentes.get(0);

            // Completa la asignación si un registro antiguo no la tiene.
            if (grupo.getInstructor() == null) {
                grupo.setInstructor(instructor);
            }
        }

        LocalTime horaInicio = LocalTime.parse(inicio);
        LocalTime horaFin = LocalTime.parse(fin);

        for (DiaSemana dia : dias) {
            Long cantidad = em.createQuery(
                    "SELECT COUNT(h) FROM Horario h "
                    + "WHERE h.grupo.id = :grupoId "
                    + "AND h.diaSemana = :dia",
                    Long.class)
                    .setParameter("grupoId", grupo.getId())
                    .setParameter("dia", dia)
                    .getSingleResult();

            // Conserva cualquier horario existente para ese día.
            if (cantidad > 0) {
                continue;
            }

            Horario horario = new Horario();
            horario.setGrupo(grupo);
            horario.setDiaSemana(dia);
            horario.setHoraInicio(horaInicio);
            horario.setHoraFin(horaFin);

            em.persist(horario);
            grupo.getHorarios().add(horario);
        }
    }
}
