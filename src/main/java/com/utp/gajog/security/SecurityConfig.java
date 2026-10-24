package com.utp.gajog.security;

import com.utp.gajog.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ForzarCambioPasswordFilter forzarCambioPasswordFilter;
    private final UsuarioService usuarioService;

    public SecurityConfig(
            ForzarCambioPasswordFilter forzarCambioPasswordFilter,
            @Lazy UsuarioService usuarioService
    ) {
        this.forzarCambioPasswordFilter = forzarCambioPasswordFilter;
        this.usuarioService = usuarioService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/cuenta/cambiar-password/**").authenticated()

                // 1. Solo ADMINISTRADOR: Usuarios, Instructores, Anulación de pagos, Retiro por mora y generación masiva
                .requestMatchers("/usuarios/**", "/instructores/**", "/personal/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/pagos/*/anular", "/pagos/mora/*/retirar", "/pagos/mensualidad/masivo").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/pagos/*/anular").hasRole("ADMINISTRADOR")

                // 2. ADMINISTRADOR y RECEPCION: Matrículas, Pagos, Apoderados, Estado/Creación de Alumnos
                .requestMatchers("/matriculas/**", "/api/matriculas/**").hasAnyRole("ADMINISTRADOR", "RECEPCION")
                .requestMatchers("/pagos/**", "/mensualidades/**", "/api/pagos/**").hasAnyRole("ADMINISTRADOR", "RECEPCION")
                .requestMatchers("/apoderados/**").hasAnyRole("ADMINISTRADOR", "RECEPCION")
                .requestMatchers("/alumnos/guardar", "/alumnos/*/estado", "/alumnos/*/reasignar-grupo").hasAnyRole("ADMINISTRADOR", "RECEPCION")

                // 3. ADMINISTRADOR e INSTRUCTOR: Exámenes y Observaciones técnicas
                .requestMatchers("/examenes/**", "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "INSTRUCTOR")
                .requestMatchers("/alumnos/*/observacion").hasAnyRole("ADMINISTRADOR", "INSTRUCTOR")

                // 4. ADMINISTRADOR, RECEPCION e INSTRUCTOR: Resumen, Consulta de Alumnos/Expediente, Horarios y Asistencias
                .requestMatchers("/", "/alumnos/**", "/horarios/**", "/asistencias/**", "/api/asistencias/**", "/api/alumnos/**")
                    .hasAnyRole("ADMINISTRADOR", "RECEPCION", "INSTRUCTOR")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    usuarioService.limpiarIntentosFallidosAlIngresar(authentication.getName());
                    response.sendRedirect(request.getContextPath() + "/");
                })
                .failureHandler((request, response, exception) -> {
                    String username = request.getParameter("username");
                    usuarioService.registrarIntentoFallido(username);
                    if (exception instanceof LockedException) {
                        response.sendRedirect(request.getContextPath() + "/login?locked=true");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login?error=true");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .addFilterAfter(forzarCambioPasswordFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}