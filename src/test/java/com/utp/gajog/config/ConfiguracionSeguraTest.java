package com.utp.gajog.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Pruebas del control A02: el repositorio no guarda contraseñas (Joaquín). */
class ConfiguracionSeguraTest {

    private static String leer(String ruta) throws Exception {
        return Files.readString(Path.of(ruta));
    }

    @Test
    void laContrasenaDeLaBaseSeLeeDeVariableDeEntorno() throws Exception {
        String propiedades = leer("src/main/resources/application.properties");
        assertTrue(propiedades.contains("spring.datasource.password=${DB_PASSWORD:}"));
        assertFalse(propiedades.contains("tidbcloud.com"), "No debe haber servidores reales en el repositorio");
    }

    @Test
    void lasPaginasDeErrorNoMuestranDetallesInternos() throws Exception {
        String propiedades = leer("src/main/resources/application.properties");
        assertTrue(propiedades.contains("server.error.include-stacktrace=never"));
        assertTrue(propiedades.contains("server.error.include-message=never"));
    }

    @Test
    void elArchivoLocalConCredencialesNoSeSubeAGitHub() throws Exception {
        assertTrue(leer(".gitignore").contains("application-local.properties"));
    }
}
