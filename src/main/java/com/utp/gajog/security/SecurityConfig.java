package com.utp.gajog.security;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuración de seguridad del sistema GAJOG TKD.
 *
 * <ul>
 *   <li>A07 Fallos de autenticación: login con contraseña cifrada (BCrypt).</li>
 *   <li>A01 Control de acceso roto: cada ruta exige un rol y lo demás
 *       requiere sesión iniciada. CSRF activo en todos los formularios.</li>
 *   <li>A02 Configuración de seguridad incorrecta: cabeceras HTTP seguras.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMINISTRADOR";
    private static final String RECEPCION = "RECEPCION";
    private static final String INSTRUCTOR = "INSTRUCTOR";

    /** A07: las contraseñas se guardan con BCrypt (hash con sal, costo 12). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** A09: publica los eventos de acceso denegado para registrarlos en el log. */
    @Bean
    public AuthorizationEventPublisher authorizationEventPublisher(
            ApplicationEventPublisher publicador) {
        return new SpringAuthorizationEventPublisher(publicador);
    }

    @Bean
    public SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
        http
            // ---------------- A01: permisos por rol (Jorge) ----------------
            .authorizeHttpRequests(acceso -> acceso
                .requestMatchers("/login", "/error", "/error/**", "/favicon.ico").permitAll()
                .requestMatchers("/matriculas/**", "/mensualidades/**", "/horarios/**")
                    .hasAnyRole(ADMIN, RECEPCION)
                .requestMatchers("/asistencias/**")
                    .hasAnyRole(ADMIN, INSTRUCTOR)
                .requestMatchers("/examenes/**")
                    .hasAnyRole(ADMIN, RECEPCION, INSTRUCTOR)
                .requestMatchers("/torneos/ranking").authenticated()
                .requestMatchers(HttpMethod.POST,
                        "/torneos/nuevo", "/torneos/*/inscribir", "/torneos/*/sortear")
                    .hasAnyRole(ADMIN, RECEPCION)
                .requestMatchers("/torneos/combates/**", "/torneos/*/poomsae/**",
                        "/torneos/*/rompimiento/**", "/torneos/*/iniciar-poomsae",
                        "/torneos/*/iniciar-rompimiento")
                    .hasAnyRole(ADMIN, INSTRUCTOR)
                .requestMatchers("/torneos/**")
                    .hasAnyRole(ADMIN, RECEPCION, INSTRUCTOR)
                // Todo lo que no está en la lista exige iniciar sesión.
                .anyRequest().authenticated())

            // ---------------- A07: inicio y cierre de sesión (Jorge) ----------------
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(salida -> salida
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salida")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            // Nueva sesión al iniciar sesión (evita fijación de sesión).
            .sessionManagement(sesion -> sesion
                .sessionFixation(fijacion -> fijacion.migrateSession()))

            // ---------------- A02: cabeceras HTTP seguras (Joaquín) ----------------
            .headers(cabeceras -> cabeceras
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                        + "script-src 'self' 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; font-src 'self' data:; "
                        + "object-src 'none'; base-uri 'self'; "
                        + "form-action 'self'; frame-ancestors 'none'"))
                .frameOptions(marco -> marco.deny())
                .referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));

        // CSRF queda activo por defecto: Thymeleaf agrega el token a cada
        // formulario con th:action y layout.html lo agrega a las llamadas fetch.
        return http.build();
    }
}
