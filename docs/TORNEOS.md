# Módulo de Torneos y Competencias (BPMN 5)

## Qué hace
Organiza torneos de **combate (kyorugi)**, **formas (poomsae)** y **rompimiento de tablas (kyokpa)**: crea el torneo, inscribe alumnos
(con categoría automática y pago de cuota), sortea las llaves, registra los puntos en un
**marcador de combate** y asigna medallas. Incluye ranking anual de medallas, exportación a Excel
(Apache POI) y constancia imprimible.

## Pantallas (menú "Torneos")
1. `/torneos` — lista de torneos.
2. `/torneos/nuevo` — formulario de nuevo torneo.
3. `/torneos/{id}` — inscripciones, cierre/sorteo y llaves.
4. `/torneos/combates/{id}` — **marcador de combate** (reloj, puntos, faltas, rounds, deshacer, W.O.).
5. `/torneos/{id}/poomsae` — panel de 5 jueces con cálculo en vivo (tacha la nota más alta y la más baja) y tablero de clasificación.
5b. `/torneos/{id}/rompimiento` — **panel de rompimiento**: elegir técnica, tablas intentadas y rotas, ver los puntos antes de registrar, hasta 3 intentos por competidor, deshacer y tablero de posiciones.
6. `/torneos/{id}/resultados` — medallistas, Excel y constancias.
7. `/torneos/ranking` — ranking de medallas de la academia.

## Reglas
- Categoría = grupo de edad (Infantil ≤11, Juvenil 12-16, Adulto ≥17) · nivel de cinturón
  (orden 1-3 Principiante, 4-6 Intermedio, 7+ Avanzado) · peso (<45 Ligero, 45-60 Medio, >60 Pesado).
- Sin pago de la cuota no hay inscripción. Un alumno no puede inscribirse dos veces.
- Combate: 3 rounds de 2 min, gana quien gane 2 rounds. Puntos: +1 puño, +2 patada al tronco,
  +3 patada a la cabeza. Cada falta (gam-jeom) da 1 punto al rival.
- Superioridad técnica: diferencia de 12 puntos termina el combate (`gajog.torneo.diferencia-superioridad`).
- 10 faltas descalifican (`gajog.torneo.maximo-faltas`).
- Si tras 3 rounds hay empate: round de oro, gana quien anote primero.
- W.O.: si un competidor no se presenta, gana el rival.
- Poomsae: exactitud 0-4 + presentación 0-6 por juez; se descarta la nota total más alta y la más baja
  y se promedia el resto.
- Rompimiento: hasta 3 intentos por competidor. Puntos = tablas rotas × dificultad de la técnica
  (puño 1.0, canto de mano 1.2, patada frontal/lateral 1.5, patada con salto 2.0, giratoria con salto 2.5).
  Rompimiento limpio (rompe todas las intentadas): +0.5 por tabla. Máximo 10 tablas por intento.
  Desempate: más tablas rotas.
- Medallas: oro (ganador de la final), plata, y 2 bronces (perdedores de semifinales).
  Ranking: oro 5, plata 3, bronce 1.

## Flujo BPMN (para dibujar)
Carriles: **Administrador/Instructor**, **Sistema**, **Juez**.

1. (Inicio) Administrador crea el torneo → Sistema lo deja en *Inscripción abierta*.
2. Administrador inscribe a un alumno y registra el pago de la cuota.
3. Sistema: ◇ ¿alumno activo, con fecha de nacimiento y cinturón? No → rechaza con mensaje.
4. Sistema: ◇ ¿ya está inscrito? Sí → rechaza.
5. Sistema calcula la categoría (edad, cinturón, peso) y guarda la inscripción.
6. Administrador cierra inscripciones.
7. ◇ ¿Modalidad? 
   - **Combate:** Sistema sortea y crea la llave (pases directos si el número es impar).
   - **Poomsae:** Sistema habilita la calificación.
8. **Combate:** Juez anota puntos/faltas → Sistema calcula el round.
   ◇ ¿Superioridad o faltas? Sí → termina. No → ◇ ¿terminó el round? → ◇ ¿alguien tiene 2 rounds?
   No y empate tras 3 rounds → round de oro.
9. Sistema avanza al ganador en la llave. ◇ ¿Era la final? No → siguiente combate.
10. **Poomsae:** 5 jueces ingresan notas → Sistema descarta extremos y promedia.
11. Sistema asigna oro, plata y bronce, y finaliza el torneo.
12. (Fin) Administrador exporta Excel / imprime constancias. El ranking se actualiza.

## Flujo BPMN del rompimiento
1. Administrador inscribe y cobra la cuota → cierra inscripciones e inicia.
2. Juez elige al competidor, la técnica y las tablas intentadas.
3. Juez marca las tablas rotas. ◇ ¿Rotas ≤ intentadas? No → el sistema rechaza.
4. Sistema calcula: rotas × dificultad. ◇ ¿Rompió todas? Sí → suma bonus de limpio.
5. ◇ ¿Lleva 3 intentos? No → vuelve al paso 2. Sí → el competidor termina.
6. Cuando todos terminaron, el administrador finaliza: el sistema ordena por puntos (desempate: tablas rotas) y da oro, plata y bronce.

## Dónde mirar en el código
- Reglas puras (con pruebas JUnit): `util/ReglasCombate`, `util/CalculadoraPoomsae`, `util/CategoriaTorneo`.
- Marcador y llaves: `service/CompetenciaService`.
- Inscripción: `service/InscripcionTorneoService`.
- Poomsae: `service/PoomsaeService`.
- Rompimiento: `service/RompimientoService` y `util/CalculadoraRompimiento`.
- Excel (Apache POI): `service/ExportarTorneoService`.
- Registro de eventos (Logback/SLF4J): `log.info(...)` en cada servicio.
- Datos de ejemplo: `config/TorneoDatosIniciales` (8 alumnos con DNI 70000001 a 70000008).
