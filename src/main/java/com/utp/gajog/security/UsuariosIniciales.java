package com.utp.gajog.security;

import com.utp.gajog.enums.RolUsuario;
import com.utp.gajog.model.Persona;
import com.utp.gajog.model.Usuario;
import com.utp.gajog.repository.PersonaRepository;
import com.utp.gajog.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A07: crea los usuarios de prueba (admin, recepcion, instructor) con la
 * contraseña cifrada en BCrypt. La contraseña inicial NO está escrita en el
 * código: se lee de la propiedad gajog.seguridad.password-inicial.
 */
@Component
@Order(300)
@ConditionalOnProperty(name = "gajog.datos-prueba.enabled", havingValue = "true")
public class UsuariosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciales.class);

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordInicial;

    public UsuariosIniciales(UsuarioRepository usuarioRepository,
            PersonaRepository personaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${gajog.seguridad.password-inicial:}") String passwordInicial) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordInicial = passwordInicial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (passwordInicial == null || passwordInicial.length() < 8) {
            log.warn("No se crearon usuarios de prueba: falta gajog.seguridad.password-inicial (mínimo 8 caracteres).");
            return;
        }
        crear("admin", RolUsuario.ADMINISTRADOR, "99000001", "Administrador");
        crear("recepcion", RolUsuario.RECEPCION, "99000002", "Recepción");
        crear("instructor", RolUsuario.INSTRUCTOR, "99000003", "Instructor");
    }

    private void crear(String nombreUsuario, RolUsuario rol, String dni, String nombres) {
        if (usuarioRepository.findByNombreUsuario(nombreUsuario).isPresent()) {
            return;
        }
        Persona persona = personaRepository.findByDni(dni).orElseGet(() -> {
            Persona p = new Persona();
            p.setDni(dni);
            p.setNombres(nombres);
            p.setApellidos("GAJOG");
            return personaRepository.save(p);
        });
        usuarioRepository.save(new Usuario(
                persona, nombreUsuario, passwordEncoder.encode(passwordInicial), rol));
        log.info("Usuario de prueba creado: {} ({})", nombreUsuario, rol);
    }
}
