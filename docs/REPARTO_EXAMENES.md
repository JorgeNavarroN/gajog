# Módulo de exámenes de ascenso (BPMN 3) — guía de reparto

El BPMN 3 se corta en dos mitades. Cada integrante programa y expone la suya.
El diagrama dividido está en `docs/BPMN3_dividido.png`.

## Cómo funciona el flujo (lo que se ve en la pantalla `/examenes`)

1. **Pago** → el Administrador registra el pago del examen (S/ 80.00 por defecto).
2. **¿Pago registrado?** → si no, el sistema no deja evaluar ("No: pago pendiente").
3. **Evaluación** → el Instructor califica 4 criterios y deja observaciones.
4. **¿Aprobó?** → con 70 puntos o más sube el cinturón y queda en el historial;
   si no, conserva el cinturón.
5. **Resultado** → se consulta el resultado, las notas y el historial de ascensos.

## MITAD A — Pago del examen (carriles Administrador y Sistema web)

Casos de uso: **CU06** (cobrar examen), **CU15** (programar examen) y el rombo
"¿Pago registrado?".

| Archivo | Qué hace |
|---|---|
| `service/ExamenPagoService.java` | Tarifa, programar examen, cobrar, rombo del pago, indicadores (75 % asistencia, 3 meses) |
| `controller/ExamenPagoController.java` | `POST /examenes/{alumnoId}/pago` |
| `dto/PagoExamenForm.java` | Formulario del cobro |
| `templates/examenes/pago.html` | Pestaña "1. Pago" |

**De qué hablar:** el rombo del pago como regla de negocio (no se evalúa sin
pagar), validaciones (monto igual a la tarifa, fecha no futura, sin pago
duplicado, solo alumno ACTIVO con matrícula activa) y que cada examen apunta
al *siguiente* cinturón de la escala.

## MITAD B — Evaluación y resultado (carriles Instructor, Sistema web y Alumno)

Casos de uso: **CU16** (calificar por criterios), **CU17** (registrar resultado)
y **CU18** (consultar exámenes).

| Archivo | Qué hace |
|---|---|
| `service/ExamenEvaluacionService.java` | Califica, calcula el total, decide aprobado/reprobado, sube el cinturón y guarda el historial (una sola transacción) |
| `controller/ExamenEvaluacionController.java` | `POST /examenes/{examenId}/evaluacion` |
| `templates/examenes/evaluacion.html` | Pestaña "2. Evaluación" |
| `templates/examenes/resultado.html` | Pestaña "3. Resultado" (notas, historial, exámenes rendidos) |

**De qué hablar:** criterios (Básicos 25, Poomsae 30, Kyorugi 25, Rompimiento
y teoría 20 = 100), mínimo 70 para aprobar (configurable con
`gajog.examen.puntaje-minimo`), la transacción única (si algo falla no se guarda
nada), y las tablas `examenes`, `detalle_examen`, `criterios_examen` e
`historial_cinturones`.

## BASE COMPARTIDA (no la edite uno solo sin avisar)

- `controller/ExamenWebController.java` — pantalla `GET /examenes`
- `service/ExamenConsultaService.java` — lista de alumnos y estado de cada uno
- `templates/examenes.html` — armazón con las 3 pestañas
- `repository/ExamenRepository`, `DetalleExamenRepository`, `CriterioExamenRepository`, `HistorialCinturonesRepository`
- `config/ExamenDatosIniciales.java` — crea los 4 criterios y los cinturones que falten
- `config/ColoresCinturon.java` — colores de las insignias
- `templates/fragments/layout.html` — se agregó el enlace "Exámenes" al menú

## Cambios sobre el proyecto anterior

- `model/Examen.java`: columna nueva `observaciones` (opcional). Hibernate la crea sola (`ddl-auto=update`).
- `repository/PagoRepository.java`: método nuevo `findByExamen_IdAndEstado`.
- `application.properties`: la contraseña de MySQL ahora es `CAMBIAR_LOCALMENTE`.
  **Cada uno debe poner la suya y no subirla a GitHub.**

## Cómo probarlo

1. Arranca igual que antes: `./gradlew bootRun` (Java 27 + MySQL).
2. Al iniciar, el sistema crea solo los 4 criterios y los cinturones Amarillo, Verde, Azul, Rojo y Negro si faltan.
3. Registra un alumno en `/matriculas/nueva/paso-1` (queda con cinturón Blanco y matrícula activa).
4. Entra a `/examenes`, elige al alumno y recorre: Pago → Evaluación → Resultado.
   - Prueba reprobar (menos de 70) y aprobar (70 o más).
   - Prueba evaluar sin pagar: el sistema lo impide.

## Qué NO incluye (decirlo con honestidad al exponer)

- No hay login todavía: no se sabe qué instructor evaluó (la tabla `examen_evaluador` del informe no está implementada).
- La asistencia mínima (75 %) y la antigüedad (3 meses) se muestran como advertencia; no bloquean.
- Se calificó con puntajes numéricos por criterio (CU17), no con "Logrado / No logrado" como en el mockup.

## Trabajo en GitHub sin pisarse

```
git checkout -b feature/examen-pago          # mitad A
git checkout -b feature/examen-evaluacion    # mitad B
```
Cada uno sube solo los archivos de su mitad y hace Pull Request a `main`.
Quien suba primero la base compartida avisa al otro.
