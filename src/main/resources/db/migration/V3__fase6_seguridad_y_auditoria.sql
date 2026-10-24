ALTER TABLE usuarios
    ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1',
    ADD COLUMN requiere_cambio_password BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta DATETIME(6) NULL;

-- Forzar cambio de contraseña al usuario 'admin' inicial si aún conserva la clave por defecto
UPDATE usuarios SET requiere_cambio_password = b'1' WHERE usuario = 'admin';

CREATE TABLE IF NOT EXISTS auditoria_eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora DATETIME(6) NOT NULL,
    usuario VARCHAR(60) NOT NULL,
    accion VARCHAR(60) NOT NULL,
    entidad VARCHAR(60) NOT NULL,
    entidad_id BIGINT NULL,
    detalle VARCHAR(1000) NOT NULL,
    INDEX idx_auditoria_fecha (fecha_hora),
    INDEX idx_auditoria_accion (accion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;