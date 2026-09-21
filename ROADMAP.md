# ROADMAP — V3.6: qué falta para acabar y en qué orden

**Los códigos 8 y b siguen sin escribir en el equipo; ninguna APK tiene hoy arquitecto y QA para
hacerlo.** Este fichero dice qué falta, en qué orden y en qué sesión; no es una bitácora. Las cifras del día (versiones, md5, recuentos) van en
[`RETOMAR.md`](RETOMAR.md); lo que pasó, en `git log` y [`HISTORIA.md`](HISTORIA.md). Reglas en
[`CLAUDE.md`](CLAUDE.md); mapa en `ARQUITECTURA.map` §M2-§M3. Lo que no está aquí no está en ejecución.

## Objetivo y cuándo está acabado

Dejar **SLV-002 (Coviandina)** con firmware V3.6, **calibrado por códigos con acta aceptada**, serie
grabada, ZIP de soporte archivado e **informe PDF y registros** para la interventoría. Después, una
**app de producción** revisada. El proyecto se cierra con las puertas P8, P11 y P12 cerradas.

La calibración es **por equipo** (L-23). Nordeste es de la V4.6 y Ruta al Mar (SLV-028) tiene su
propio proyecto: no se trabajan desde aquí.

## Orden para acabar (prioridad de arriba abajo)

| # | Tarea | Quién | Sale | Depende de |
| :---: | :--- | :--- | :--- | :--- |
| 1 | **Decidir versión de la app para el HONOR** (recomendada rc4; ver `RETOMAR.md`) | Diego | Decisión escrita en `DECISIONES-Diego-*.md` | — |
| 2 | **Arquitecto y QA** del salto rc2 → versión elegida; confirmar que su APK es la del commit | Subagentes adversarios, alcances disjuntos | Dos veredictos escritos | 1 |
| 3 | **Procedimiento del 8 y la b al día** (once cambios del análisis del 21-sep) | Principal | `PROCEDIMIENTO-Escribir-8-y-b.md` | 1 |
| 4 | **Calibrar el 8** y aceptar su acta; **después la b**. Batería nueva, campaña nueva con el ZIP de las 18:11, sin ICSP | Diego con la app | Códigos escritos, relectura, re-medida, actas | 2, 3 |
| 5 | **Grabar la serie `SLV-002-2026`** | Diego | Tramas en el ZIP | 4 |
| 6 | **ZIP de soporte** al repositorio con su huella | Diego y principal | ZIP + `HUELLAS.txt` | 5. **Cierra P8** para 8 y b |
| 7 | **Informe PDF y registros** para la interventoría (serie, fecha, vencimiento) | Subagente + revisión | PDF y registros | 6. **Cierra P12** |
| 8 | **App corta "RTV Calibra"**: versión propia, arquitecto y QA, prueba en teléfono | Subagentes; Diego | APK entregable | Tras 4 (no bloquea) |
| 9 | **App de producción** (P10, P11) sobre la app única RTV | Subagentes | Propuesta y APK revisada | 6 |
| 10 | Banco representativo y verificación anual para recalibrar | Diego | Colas medidas | 6 |

Los códigos 1 y 2 no se reescriben. **Los códigos 3 y 5 no se arreglan midiendo**: su dato está
invertido (informe de las 18:11). Los 3, 4 y 6 esperan la decisión D-3.

## Sesiones

Sólo la sesión en curso y la siguiente. Lo hecho se borra de aquí: está en `git log`.

| Sesión | Tareas |
| :--- | :--- |
| En curso (21-sep) | Skills del repositorio, `ARQUITECTURA.map` y referencias cruzadas; tareas 1 a 3 si Diego decide |
| Siguiente | Tareas 2 a 6: revisiones, procedimiento, calibrar 8 y b, serie, ZIP |

## Puertas P1-P12

| Puerta | Estado |
| :--- | :--- |
| P1 Especificación · P2 Arquitectura · P3 Compilación reproducible · P6 Autorización | Cerradas (18-sep) |
| P4 Firmware · P7 Grabación | Cerradas para la 3.6.2 |
| P5 App · P9 Validación del arquitecto | **Abiertas**: ninguna APK con arquitecto y QA para calibrar el 8 y la b |
| P8 Calibración de SLV-002 | Cerrada para 1 y 2; **abierta** para 8 y b (y 3, 4, 5, 6) |
| P10 Propuesta de producción · P11 APK de producción · P12 Informe y registros | Pendientes, tras P8 |

Las revisiones de arquitectura de la app (P9 a P16) y las QA están en `05_Documentacion/`; en la rama
`rtv-1.0`, `REVISION-QA-RTV-1.0.0-rc2.md`.

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir |
| :--- | :--- |
| D-3 | Qué códigos intensos 3, 4 y 6 se ajustan y con qué grado |
| D-4 | Café y lila: dentro del ajuste del rojo o sólo verificar (recomendación: sólo verificar) |
| D-5 | P32a/P32b: identidad del P32 duplicado |
| D-8 | Clave de firma propia del APK (una desinstalación borra la campaña) |
| D-9 | Umbral de la interventoría (C-01): bloquea el indicador E11 |
| D-10 | Cuándo subir `targetSdk` (hoy 30) |
| D-11 | Versión de la app para calibrar el 8 y la b en el HONOR (tarea 1) |
| D-12 | Versión propia de la app corta (hoy repite la de la rc6 con otro binario) |

Las decididas están en `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`.

## Contradicciones abiertas

No se eligen: se cierran midiendo o con registro. Las cerradas están en `HISTORIA.md`.

- **Reloj:** la hora de Diego y la de los commits difirieron ~1 h 30 min el 19-sep.
- **Numeración:** "P10-P14" nombra puertas del proceso y revisiones de arquitectura de la app.
- **`targetSdk`:** urgente según el encargo del 19-sep; sólo obligatorio en Play según el estudio.
- **Serie en la V4.6:** su `CERTIFICADO-ANTERIOR.md` dice que Coviandina "conserva `SLV-002`";
  SERIE-2 lo cambia a `SLV-002-2026`.
- **Modelo de pantalla:** STA035WT-01 frente a STVA035WT(-01) (`HISTORIA.md`). Se mira la etiqueta.
- **Borrado de datos del HONOR** entre las 12:27 y las 19:12 del 19-sep: hipótesis sin registro.
