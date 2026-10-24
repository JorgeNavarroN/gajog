# GAJOG TKD — Sistema de Gestión Integral para Academia de Taekwondo

Sistema web SSR construido con **Java 27**, **Spring Boot 4.1.1**, **Spring Security**, **Spring Data JPA (Hibernate)**, **Flyway**, **Thymeleaf** y **Bootstrap 5**.

## 1. Requisitos Previos
- **JDK 27** configurado en el sistema o Toolchain de Gradle.
- **MySQL 8.x** ejecutándose en el puerto `3306`.

## 2. Variables de Entorno
Configura las siguientes variables de entorno antes de ejecutar en desarrollo o producción:

| Variable | Descripción | Valor por defecto (`dev`) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de ejecución (`dev` o `prod`) | `dev` |
| `DB_URL` | Cadena JDBC de conexión a MySQL | `jdbc:mysql://localhost:3306/taekwondo_db?createDatabaseIfNotExist=true&serverTimezone=America/Lima&characterEncoding=UTF-8` |
| `DB_USERNAME` | Usuario de base de datos | `root` |
| `DB_PASSWORD` | Contraseña de base de datos | *(vacío o configurar localmente)* |
| `GAJOG_DIAS_GRACIA` | Días de gracia antes de marcar en riesgo de retiro | `15` |
| `GAJOG_BAJA_AUTO_MORA` | Baja automática por mora (`true`/`false`) | `false` |
| `GAJOG_TOLERANCIA_TARDANZA` | Minutos de tolerancia para asistencia por DNI | `10` |

## 3. Ejecución y Migraciones (Flyway)
Al iniciar la aplicación, Flyway ejecuta automáticamente en orden:
1. `V1__baseline_schema.sql`: Esquema base inicial.
2. `V2__fase1_modelo_y_migracion_datos.sql`: Tarifas, cupos, auditoría de pagos y migración de estados.
3. `V3__fase6_seguridad_y_auditoria.sql`: Bloqueo de cuentas, cambio forzado de contraseña y tabla `auditoria_eventos`.

Para compilar y arrancar el sistema:
```powershell
./gradlew clean bootRun