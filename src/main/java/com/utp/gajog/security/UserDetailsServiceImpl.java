package com.utp.gajog.security;

import com.utp.gajog.model.Usuario;
import com.utp.gajog.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        boolean habilitado = Boolean.TRUE.equals(usuario.getActivo());
        boolean cuentaNoBloqueada = usuario.getBloqueadoHasta() == null
                || !usuario.getBloqueadoHasta().isAfter(LocalDateTime.now(clock));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());

        return User.builder()
                .username(usuario.getNombreUsuario())
                .password(usuario.getPasswordHash())
                .disabled(!habilitado)
                .accountLocked(!cuentaNoBloqueada)
                .authorities(Collections.singletonList(authority))
                .build();
    }
}