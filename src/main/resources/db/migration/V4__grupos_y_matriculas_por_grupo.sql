-- Migrar cada horario preexistente a un grupo propio para preservar matrículas y sesiones.
-- Cada horario conserva su ID: sesiones_clase y asistencias no pierden referencias.
CREATE TABLE grupos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  categoria_id BIGINT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  instructor_id BIGINT NULL,
  cupo_maximo INT NOT NULL DEFAULT 20,
  activo BIT(1) NOT NULL DEFAULT b'1',
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_grupos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
  CONSTRAINT fk_grupos_instructor FOREIGN KEY (instructor_id) REFERENCES instructores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO grupos(id, categoria_id, nombre, instructor_id, cupo_maximo, activo, version)
SELECT h.id, h.categoria_id,
       CONCAT(c.nombre, ' - ', h.dia_semana, ' ', TIME_FORMAT(h.hora_inicio, '%H:%i')),
       h.instructor_id, h.cupo_maximo, b'1', 0
FROM horarios h JOIN categorias c ON c.id = h.categoria_id;

ALTER TABLE horarios
  ADD COLUMN grupo_id BIGINT NULL,
  ADD COLUMN activo BIT(1) NOT NULL DEFAULT b'1';
UPDATE horarios SET grupo_id = id;
ALTER TABLE horarios MODIFY COLUMN grupo_id BIGINT NOT NULL;
ALTER TABLE horarios ADD CONSTRAINT fk_horarios_grupo FOREIGN KEY(grupo_id) REFERENCES grupos(id);

ALTER TABLE matriculas ADD COLUMN grupo_id BIGINT NULL;
UPDATE matriculas m JOIN horarios h ON m.horario_id = h.id SET m.grupo_id = h.grupo_id;
ALTER TABLE matriculas MODIFY COLUMN grupo_id BIGINT NOT NULL;
ALTER TABLE matriculas ADD CONSTRAINT fk_matriculas_grupo FOREIGN KEY(grupo_id) REFERENCES grupos(id);
CREATE INDEX idx_matriculas_grupo_estado ON matriculas(grupo_id, estado);

-- Retirar las claves antiguas por nombre real (en instalaciones anteriores
-- Hibernate pudo crearlas con nombres distintos a los de Flyway).
SET @fk1 = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='horarios' AND COLUMN_NAME='categoria_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql1 = IF(@fk1 IS NULL, 'DO 0', CONCAT('ALTER TABLE horarios DROP FOREIGN KEY `', @fk1, '`'));
PREPARE p1 FROM @sql1;
EXECUTE p1;
DEALLOCATE PREPARE p1;
SET @fk2 = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='horarios' AND COLUMN_NAME='instructor_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql2 = IF(@fk2 IS NULL, 'DO 0', CONCAT('ALTER TABLE horarios DROP FOREIGN KEY `', @fk2, '`'));
PREPARE p2 FROM @sql2;
EXECUTE p2;
DEALLOCATE PREPARE p2;
SET @fk3 = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='matriculas' AND COLUMN_NAME='categoria_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql3 = IF(@fk3 IS NULL, 'DO 0', CONCAT('ALTER TABLE matriculas DROP FOREIGN KEY `', @fk3, '`'));
PREPARE p3 FROM @sql3;
EXECUTE p3;
DEALLOCATE PREPARE p3;
SET @fk4 = (SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='matriculas' AND COLUMN_NAME='horario_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql4 = IF(@fk4 IS NULL, 'DO 0', CONCAT('ALTER TABLE matriculas DROP FOREIGN KEY `', @fk4, '`'));
PREPARE p4 FROM @sql4;
EXECUTE p4;
DEALLOCATE PREPARE p4;

ALTER TABLE horarios DROP COLUMN categoria_id, DROP COLUMN instructor_id, DROP COLUMN cupo_maximo;
ALTER TABLE matriculas DROP COLUMN categoria_id, DROP COLUMN horario_id;
