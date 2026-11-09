package com.utp.gajog.config;

import com.utp.gajog.enums.EstadoAlumno;
import com.utp.gajog.model.Alumno;
import com.utp.gajog.model.Cinturon;
import com.utp.gajog.model.Persona;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea 8 alumnos de ejemplo para poder probar un torneo (solo si no existen).
 * Se activa con gajog.datos-prueba.enabled=true, igual que los demás datos de prueba.
 */
@Component
@Order(200)
@ConditionalOnProperty(name = "gajog.datos-prueba.enabled", havingValue = "true")
public class TorneoDatosIniciales implements ApplicationRunner {

    private static final String[][] DEMO = {
        {"70000001", "Carlos", "Quispe Huamán", "2012-03-10"},
        {"70000002", "Diego", "Ramos Soto", "2012-07-22"},
        {"70000003", "Mateo", "Flores Paredes", "2011-11-05"},
        {"70000004", "Luis", "Vargas Rojas", "2012-01-18"},
        {"70000005", "Andrea", "Salazar Cruz", "2010-05-30"},
        {"70000006", "Valeria", "Mendoza Lara", "2010-09-14"},
        {"70000007", "Jorge", "Castillo Ríos", "2009-12-02"},
        {"70000008", "Sofía", "Torres Vega", "2009-04-27"}
    };

    private final EntityManager em;

    public TorneoDatosIniciales(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Cinturon blanco = cinturonBase();
        List<Cinturon> cinturones = em.createQuery(
                "SELECT c FROM Cinturon c ORDER BY c.orden", Cinturon.class).getResultList();

        int creados = 0;
        for (int i = 0; i < DEMO.length; i++) {
            String dni = DEMO[i][0];
            Long existe = em.createQuery(
                    "SELECT COUNT(p) FROM Persona p WHERE p.dni = :dni", Long.class)
                    .setParameter("dni", dni).getSingleResult();
            if (existe > 0) {
                continue;
            }

            Persona persona = new Persona();
            persona.setDni(dni);
            persona.setNombres(DEMO[i][1]);
            persona.setApellidos(DEMO[i][2]);
            persona.setFechaNacimiento(LocalDate.parse(DEMO[i][3]));
            em.persist(persona);

            Alumno alumno = new Alumno();
            alumno.setPersona(persona);
            alumno.setCinturonActual(cinturones.isEmpty()
                    ? blanco : cinturones.get(i % cinturones.size()));
            alumno.setFechaIngreso(LocalDate.now().minusMonths(6));
            alumno.setEstado(EstadoAlumno.ACTIVO);
            em.persist(alumno);
            creados++;
        }
        if (creados > 0) {
            System.out.println("GAJOG: " + creados + " alumnos de ejemplo para torneos creados.");
        }
    }

    /** Si todavía no hay cinturones, crea el blanco para poder asignarlo. */
    private Cinturon cinturonBase() {
        List<Cinturon> existentes = em.createQuery("SELECT c FROM Cinturon c", Cinturon.class)
                .setMaxResults(1).getResultList();
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        Cinturon blanco = new Cinturon();
        blanco.setNombre("Blanco");
        blanco.setColor("#FFFFFF");
        blanco.setOrden(1);
        em.persist(blanco);
        return blanco;
    }
}
