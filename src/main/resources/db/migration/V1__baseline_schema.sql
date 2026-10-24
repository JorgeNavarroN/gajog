CREATE TABLE IF NOT EXISTS personas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(15) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150),
    telefono VARCHAR(20),
    fecha_nacimiento DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cinturones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    color VARCHAR(30) NOT NULL,
    orden INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad_minima INT NOT NULL,
    edad_maxima INT NOT NULL,
    activo BIT(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS apoderados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_apoderado_persona FOREIGN KEY (persona_id) REFERENCES personas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS instructores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_instructor_persona FOREIGN KEY (persona_id) REFERENCES personas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL UNIQUE,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT fk_usuario_persona FOREIGN KEY (persona_id) REFERENCES personas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alumnos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    persona_id BIGINT NOT NULL UNIQUE,
    apoderado_id BIGINT,
    cinturon_actual_id BIGINT,
    fecha_ingreso DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT fk_alumno_persona FOREIGN KEY (persona_id) REFERENCES personas(id),
    CONSTRAINT fk_alumno_apoderado FOREIGN KEY (apoderado_id) REFERENCES apoderados(id),
    CONSTRAINT fk_alumno_cinturon FOREIGN KEY (cinturon_actual_id) REFERENCES cinturones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS horarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id BIGINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_horario_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS matriculas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    horario_id BIGINT NOT NULL,
    fecha_matricula DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT fk_matricula_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id),
    CONSTRAINT fk_matricula_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT fk_matricula_horario FOREIGN KEY (horario_id) REFERENCES horarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sesiones_clase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    horario_id BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(15) NOT NULL,
    CONSTRAINT fk_sesion_horario FOREIGN KEY (horario_id) REFERENCES horarios(id),
    CONSTRAINT fk_sesion_instructor FOREIGN KEY (instructor_id) REFERENCES instructores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asistencias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sesion_id BIGINT NOT NULL,
    alumno_id BIGINT NOT NULL,
    estado VARCHAR(15) NOT NULL,
    fecha_registro DATETIME(6) NOT NULL,
    CONSTRAINT uk_asistencia_sesion_alumno UNIQUE (sesion_id, alumno_id),
    CONSTRAINT fk_asistencia_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones_clase(id),
    CONSTRAINT fk_asistencia_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS criterios_examen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    puntaje_maximo DECIMAL(6,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS examenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    cinturon_objetivo_id BIGINT NOT NULL,
    fecha_examen DATE NOT NULL,
    resultado VARCHAR(15) NOT NULL,
    puntaje_total DECIMAL(6,2),
    CONSTRAINT fk_examen_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id),
    CONSTRAINT fk_examen_cinturon FOREIGN KEY (cinturon_objetivo_id) REFERENCES cinturones(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detalle_examen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    examen_id BIGINT NOT NULL,
    criterio_id BIGINT NOT NULL,
    puntaje DECIMAL(6,2) NOT NULL,
    observacion VARCHAR(500),
    CONSTRAINT fk_detalle_examen FOREIGN KEY (examen_id) REFERENCES examenes(id),
    CONSTRAINT fk_detalle_criterio FOREIGN KEY (criterio_id) REFERENCES criterios_examen(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historial_cinturones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    cinturon_id BIGINT NOT NULL,
    examen_id BIGINT,
    fecha_obtencion DATE NOT NULL,
    CONSTRAINT fk_historial_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id),
    CONSTRAINT fk_historial_cinturon FOREIGN KEY (cinturon_id) REFERENCES cinturones(id),
    CONSTRAINT fk_historial_examen FOREIGN KEY (examen_id) REFERENCES examenes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mensualidades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    anio INT NOT NULL,
    mes INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado VARCHAR(15) NOT NULL,
    CONSTRAINT uk_mensualidad_alumno_periodo UNIQUE (alumno_id, anio, mes),
    CONSTRAINT fk_mensualidad_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    mensualidad_id BIGINT,
    examen_id BIGINT,
    matricula_id BIGINT,
    tipo_pago VARCHAR(15) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    medio_pago VARCHAR(20) NOT NULL,
    fecha_pago DATETIME(6) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    CONSTRAINT fk_pago_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id),
    CONSTRAINT fk_pago_mensualidad FOREIGN KEY (mensualidad_id) REFERENCES mensualidades(id),
    CONSTRAINT fk_pago_examen FOREIGN KEY (examen_id) REFERENCES examenes(id),
    CONSTRAINT fk_pago_matricula FOREIGN KEY (matricula_id) REFERENCES matriculas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS observaciones_progreso (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alumno_id BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    observacion VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_obs_alumno FOREIGN KEY (alumno_id) REFERENCES alumnos(id),
    CONSTRAINT fk_obs_instructor FOREIGN KEY (instructor_id) REFERENCES instructores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;