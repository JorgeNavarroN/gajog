package com.utp.gajog.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * A07 Fallos de autenticación: protección contra fuerza bruta.
 * Después de {@value #MAXIMO_INTENTOS} contraseñas incorrectas seguidas,
 * la cuenta queda bloqueada durante 15 minutos.
 */
@Component
public class IntentosLoginService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final Duration TIEMPO_BLOQUEO = Duration.ofMinutes(15);

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public IntentosLoginService() {
        this(Clock.systemDefaultZone());
    }

    /** Constructor para las pruebas unitarias (permite controlar la hora). */
    IntentosLoginService(Clock reloj) {
        this.reloj = reloj;
    }

    public void registrarFallo(String usuario) {
        String clave = clave(usuario);
        if (clave.isEmpty()) {
            return;
        }
        registros.compute(clave, (k, actual) -> {
            int fallos = (actual == null || actual.vencido(reloj.instant()))
                    ? 1 : actual.fallos() + 1;
            Instant hasta = fallos >= MAXIMO_INTENTOS
                    ? reloj.instant().plus(TIEMPO_BLOQUEO) : null;
            return new Registro(fallos, hasta);
        });
    }

    public void registrarExito(String usuario) {
        registros.remove(clave(usuario));
    }

    public boolean estaBloqueado(String usuario) {
        Registro r = registros.get(clave(usuario));
        if (r == null || r.bloqueadoHasta() == null) {
            return false;
        }
        if (r.vencido(reloj.instant())) {
            registros.remove(clave(usuario));
            return false;
        }
        return true;
    }

    public int fallos(String usuario) {
        Registro r = registros.get(clave(usuario));
        return r == null ? 0 : r.fallos();
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);
    }

    private record Registro(int fallos, Instant bloqueadoHasta) {
        boolean vencido(Instant ahora) {
            return bloqueadoHasta != null && !ahora.isBefore(bloqueadoHasta);
        }
    }
}
