-- 1. Nueva tabla de tarifas por categoría
CREATE TABLE IF NOT EXISTS tarifas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id BIGINT NOT NULL,
    monto_vigente DECIMAL(10,2) NOT NULL,
    fecha_desde DATE NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    CONSTRAINT fk_tarifa_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Alteraciones en horarios (compatibles con registros existentes)
ALTER TABLE horarios
    ADD COLUMN cupo_maximo INT NOT NULL DEFAULT 20,
    ADD COLUMN sala VARCHAR(60) NOT NULL DEFAULT 'Sala Principal',
    ADD COLUMN instructor_id BIGINT NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_horario_instructor FOREIGN KEY (instructor_id) REFERENCES instructores(id);

-- 3. Alteraciones en asistencias
ALTER TABLE asistencias
    MODIFY COLUMN estado VARCHAR(20) NOT NULL,
    ADD COLUMN hora_ingreso DATETIME(6) NULL,
    ADD COLUMN minutos_tarde INT NOT NULL DEFAULT 0,
    ADD COLUMN metodo_registro VARCHAR(15) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN observacion VARCHAR(500) NULL;

-- 4. Alteraciones en mensualidades
ALTER TABLE mensualidades
    MODIFY COLUMN estado VARCHAR(20) NOT NULL;

-- 5. Alteraciones en pagos (auditoría, operación, observación y concurrencia)
ALTER TABLE pagos
    ADD COLUMN numero_operacion VARCHAR(60) NULL,
    ADD COLUMN observacion VARCHAR(500) NULL,
    ADD COLUMN registrado_por VARCHAR(60) NOT NULL DEFAULT 'SISTEMA',
    ADD COLUMN fecha_registro DATETIME(6) NULL,
    ADD COLUMN modificado_por VARCHAR(60) NULL,
    ADD COLUMN fecha_modificacion DATETIME(6) NULL,
    ADD COLUMN anulado_por VARCHAR(60) NULL,
    ADD COLUMN motivo_anulacion VARCHAR(255) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE pagos SET fecha_registro = fecha_pago WHERE fecha_registro IS NULL;
ALTER TABLE pagos MODIFY COLUMN fecha_registro DATETIME(6) NOT NULL;

-- 6. Migración de datos de Mensualidades (PENDIENTE -> POR_VENCER o VENCIDA)
UPDATE mensualidades
SET estado = CASE
    WHEN fecha_vencimiento < CURDATE() THEN 'VENCIDA'
    ELSE 'POR_VENCER'
END
WHERE estado = 'PENDIENTE';

-- 7. Migración de datos de Asistencias (ASISTIO -> PRESENTE, NO_ASISTIO -> AUSENTE)
UPDATE asistencias SET estado = 'PRESENTE' WHERE estado = 'ASISTIO';
UPDATE asistencias SET estado = 'AUSENTE' WHERE estado = 'NO_ASISTIO';
UPDATE asistencias SET hora_ingreso = fecha_registro WHERE estado = 'PRESENTE' AND hora_ingreso IS NULL;

-- 8. Índices de rendimiento y búsqueda
CREATE INDEX idx_alumnos_estado ON alumnos(estado);
CREATE INDEX idx_mensualidades_estado_vencimiento ON mensualidades(estado, fecha_vencimiento);
CREATE INDEX idx_sesiones_fecha_estado ON sesiones_clase(fecha, estado);
CREATE INDEX idx_pagos_estado_fecha ON pagos(estado, fecha_pago);
CREATE INDEX idx_matriculas_alumno_estado ON matriculas(alumno_id, estado);