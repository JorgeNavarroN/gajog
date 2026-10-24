package com.utp.gajog.service;

import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.Persona;
import com.utp.gajog.model.RolUsuario;
import com.utp.gajog.model.Usuario;
import com.utp.gajog.repository.PersonaRepository;
import com.utp.gajog.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final int MINUTOS_BLOQUEO_TEMPORAL = 15;

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PersonaRepository personaRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodosConPersona() {
        return usuarioRepository.findAllConPersona();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));
    }

    @Transactional
    public Usuario crearUsuarioSistema(
            String dni,
            String nombres,
            String apellidos,
            String username,
            String passwordInicial,
            RolUsuario rol
    ) {
        if (dni == null || !dni.trim().matches("\\d{8}")) {
            throw new ReglaNegocioException("El DNI debe contener exactamente 8 dígitos.");
        }
        String userLimpio = username != null ? username.trim().toLowerCase() : "";
        if (userLimpio.length() < 4) {
            throw new ReglaNegocioException("El nombre de usuario debe tener al menos 4 caracteres.");
        }
        if (usuarioRepository.existsByNombreUsuario(userLimpio)) {
            throw new ReglaNegocioException("El nombre de usuario '" + userLimpio + "' ya está en uso.");
        }
        validarPoliticaPassword(passwordInicial);

        String dniLimpio = dni.trim();
        Persona persona = personaRepository.findByDni(dniLimpio)
                .orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setDni(dniLimpio);
                    nueva.setNombres(nombres.trim());
                    nueva.setApellidos(apellidos.trim());
                    return personaRepository.save(nueva);
                });

        Usuario nuevo = new Usuario(
                persona,
                userLimpio,
                passwordEncoder.encode(passwordInicial),
                rol
        );
        nuevo.setActivo(true);
        nuevo.setRequiereCambioPassword(true);
        Usuario guardado = usuarioRepository.save(nuevo);

        auditoriaService.registrarEvento(
                "CREAR_USUARIO",
                "Usuario",
                guardado.getId(),
                "Usuario '" + userLimpio + "' creado con rol " + rol
        );
        return guardado;
    }

    @Transactional
    public void cambiarRolUsuario(Long usuarioId, RolUsuario nuevoRol) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        RolUsuario rolAnterior = u.getRol();
        u.setRol(nuevoRol);
        usuarioRepository.save(u);

        auditoriaService.registrarEvento(
                "CAMBIO_ROL",
                "Usuario",
                u.getId(),
                "Usuario '" + u.getNombreUsuario() + "' cambió de rol " + rolAnterior + " a " + nuevoRol
        );
    }

    @Transactional
    public void cambiarEstadoActivo(Long usuarioId, boolean activo) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        u.setActivo(activo);
        if (activo) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
        usuarioRepository.save(u);

        auditoriaService.registrarEvento(
                activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "Usuario",
                u.getId(),
                "Estado de cuenta '" + u.getNombreUsuario() + "' actualizado a activo=" + activo
        );
    }

    @Transactional
    public void cambiarPasswordPropia(String username, String passwordActual, String passwordNueva, String confirmacion) {
        Usuario u = buscarPorUsername(username);

        if (!passwordEncoder.matches(passwordActual, u.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual ingresada es incorrecta.");
        }
        if (passwordNueva == null || !passwordNueva.equals(confirmacion)) {
            throw new ReglaNegocioException("La nueva contraseña y su confirmación no coinciden.");
        }
        if (passwordEncoder.matches(passwordNueva, u.getPasswordHash())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente a la contraseña actual.");
        }
        validarPoliticaPassword(passwordNueva);

        u.setPasswordHash(passwordEncoder.encode(passwordNueva));
        u.setRequiereCambioPassword(false);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        usuarioRepository.save(u);

        auditoriaService.registrarEvento(
                "CAMBIO_PASSWORD",
                "Usuario",
                u.getId(),
                "El usuario '" + username + "' actualizó su contraseña exitosamente."
        );
    }

    @Transactional
    public void registrarIntentoFallido(String username) {
        if (username == null || username.isBlank()) return;
        usuarioRepository.findByNombreUsuario(username.trim()).ifPresent(u -> {
            int intentos = (u.getIntentosFallidos() != null ? u.getIntentosFallidos() : 0) + 1;
            u.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS_FALLIDOS) {
                LocalDateTime desbloqueo = LocalDateTime.now(clock).plusMinutes(MINUTOS_BLOQUEO_TEMPORAL);
                u.setBloqueadoHasta(desbloqueo);
                auditoriaService.registrarEvento(
                        "BLOQUEO_CUENTA",
                        "Usuario",
                        u.getId(),
                        "Cuenta '" + u.getNombreUsuario() + "' bloqueada temporalmente por " + intentos + " intentos fallidos."
                );
            }
            usuarioRepository.save(u);
        });
    }

    @Transactional
    public void limpiarIntentosFallidosAlIngresar(String username) {
        if (username == null || username.isBlank()) return;
        usuarioRepository.findByNombreUsuario(username.trim()).ifPresent(u -> {
            if ((u.getIntentosFallidos() != null && u.getIntentosFallidos() > 0) || u.getBloqueadoHasta() != null) {
                u.setIntentosFallidos(0);
                u.setBloqueadoHasta(null);
                usuarioRepository.save(u);
            }
        });
    }

    public void validarPoliticaPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ReglaNegocioException("La contraseña debe tener como mínimo 8 caracteres.");
        }
        boolean tieneLetra = password.chars().anyMatch(Character::isLetter);
        boolean tieneDigito = password.chars().anyMatch(Character::isDigit);
        if (!tieneLetra || !tieneDigito) {
            throw new ReglaNegocioException("La contraseña debe incluir al menos una letra y un número.");
        }
    }
}