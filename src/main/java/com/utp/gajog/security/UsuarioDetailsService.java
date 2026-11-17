package com.utp.gajog.security;

import com.utp.gajog.model.Usuario;
import com.utp.gajog.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A07: carga el usuario desde la tabla usuarios. La contraseña se compara
 * con el hash BCrypt guardado y el rol se convierte en permiso (ROLE_...).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final IntentosLoginService intentos;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository,
            IntentosLoginService intentos) {
        this.usuarioRepository = usuarioRepository;
        this.intentos = intentos;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario) {
        String nombre = nombreUsuario == null ? "" : nombreUsuario.trim();
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombre)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        return User.withUsername(usuario.getNombreUsuario())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                .accountLocked(intentos.estaBloqueado(usuario.getNombreUsuario()))
                .build();
    }
}
