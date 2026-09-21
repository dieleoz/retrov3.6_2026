# ROADMAP — V3.6: qué falta hasta certificar SLV-002, y en qué orden

**Los códigos 8 y b siguen sin escribir en el equipo, y la app que los calibra aún no emite el PDF.**
Este fichero dice qué falta, en qué orden y en qué sesión; no es una bitácora. Las cifras del día van
en [`RETOMAR.md`](RETOMAR.md); lo que pasó, en `git log` y [`HISTORIA.md`](HISTORIA.md). Reglas en
[`CLAUDE.md`](CLAUDE.md); mapa en `ARQUITECTURA.map` §M2-§M3.

## Objetivo y cuándo está acabado

**SLV-002 (Coviandina) certificado:** firmware V3.6, códigos calibrados con acta aceptada, serie
grabada, ZIP archivado y **PDF de calibración** entregado. Cierra con P8 y P12. Después, las apps de
DPI para los siguientes equipos. Nordeste es de la V4.6 y Ruta al Mar tiene su propio proyecto.

**Las apps de calibración son sólo de DPI** (decisión APPS-DPI): no se publican; van por USB, una por
proyecto. El cliente recibe el equipo y el PDF. **Calibrar = fecha, operador y exportar el PDF.**

## Dos apps, y no se mezclan

**App de empresa** (DPI, USB por proyecto) y **app de usuario** (va con el equipo; la usa el cliente).

### App de empresa: tres piezas

| Pieza | Qué hace, y nada más |
| :--- | :--- |
| **Toma** | Conectar, test, "Iniciar", **hasta 50 tomas** de 3-4 disparos (negro incluido), repetir, ZIP |
| **Calibra** | Cargar el ZIP, "Calibrar" con fecha y operador, escribir, verificar 10 patrones, PDF |
| **PDF** | Lo único que sale hacia el cliente (contenido: `SPEC-Calibracion-V3.6.md` RF-CAL-30 a 34) |

Las garantías (PIN del equipo, relectura, no mezclar equipos ni firmwares, ancla en negro) siguen
por dentro y sólo se muestran cuando bloquean.

## Orden hasta certificar (prioridad de arriba abajo)

| # | Tarea | Quién | Depende de |
| :---: | :--- | :--- | :--- |
| 0 | **Arreglar `Cov_3.6.1_calibrar`** (arquitecto: NO APTO, tres Altos) | Subagentes | — |
| 1 | **SPEC de Calibra con PDF y sus pruebas** (valor esperado de fuera del código) | Subagente | — |
| 2 | **`arquitecto-iot` sobre la SPEC y las pruebas**, antes del código | Subagente (opus) | 1 |
| 3 | **Desarrollo de Calibra** en worktree, versión propia, con `.githooks/` en su rama | Subagente | 2 |
| 4 | **`qa-istqb` y `arquitecto-iot` sobre el APK y su md5** que se entregan | Subagentes | 3 |
| 5 | **Calibrar el 8** con el ZIP archivado y aceptar su acta; **después la b**. Sin ICSP | Diego | 4 |
| 6 | Grabar la serie `SLV-002-2026`; ZIP y PDF al repositorio con su huella. **Cierra P8** | Diego y principal | 5 |
| 7 | Certificados de los códigos 3 y 5 (dato invertido), contra el otro patrón; luego el 5 | Subagente | En paralelo |
| 8 | Registros para la interventoría. **Cierra P12** | Subagente | 6 |
| 9 | **Toma** dentro de la app de empresa: SPEC, arquitecto, código, QA | Subagentes | Tras 6 |
| 10 | **App de usuario**: SPEC y TDD (en curso), arquitecto, código, QA | Subagentes | En paralelo |

Los códigos 1 y 2 no se reescriben. Los 3, 4 y 6 esperan D-3.

## Sesiones

Sólo la en curso y la siguiente; lo hecho se borra (está en `git log`).

| Sesión | Tareas |
| :--- | :--- |
| En curso | Tarea 0 (arreglar la app de calibrar); SPEC y TDD de la app de usuario (tarea 10) |
| Siguiente | Calibrar el 8 y la b con la app arreglada; tareas 1 a 4 (certificado) |

## Puertas P1-P12

| Puerta | Estado |
| :--- | :--- |
| P1 Especificación · P2 Arquitectura · P3 Compilación reproducible · P6 Autorización | Cerradas |
| P4 Firmware · P7 Grabación | Cerradas para la 3.6.2 |
| P5 App · P9 Validación del arquitecto | **Abiertas**: ninguna APK con arquitecto y QA para el 8 y la b |
| P8 Calibración de SLV-002 | Cerrada para 1 y 2; **abierta** para 8 y b (y 3, 4, 5, 6) |
| P10-P11 Producción · P12 Informe y registros | Pendientes |

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir |
| :--- | :--- |
| D-13 | Qué referencias entran en la Toma (tope de 50, TOMA-50); propuesta tras cruzar `old` y firmware |
| D-16 | ¿TOMA-50 anula PROTOCOLO-AJUSTE (5 × 4) y TIPO-I-REPETIR? ¿Curva propia para a, c y d? |
| D-3 | Qué códigos intensos 3, 4 y 6 se ajustan y con qué grado |
| D-4 | Café y lila: dentro del ajuste del rojo o sólo verificar (recomendación: sólo verificar) |
| D-5 | P32a/P32b: identidad del P32 duplicado |
| D-8 | Clave de firma propia del APK, interna de DPI |
| D-9 | Umbral de la interventoría (C-01): bloquea el indicador E11 |

Las decididas están en `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`. D-11 y D-12 (versión
para el HONOR y de la app corta) las absorbe la tarea 3: Calibra lleva versión propia.

## Contradicciones abiertas

No se eligen: se cierran midiendo o con registro. Las cerradas están en `HISTORIA.md`.

- **Reloj:** la hora de Diego y la de los commits difirieron hora y media en una sesión.
- **Numeración:** "P10-P14" nombra puertas del proceso y revisiones de arquitectura de la app.
- **Serie en la V4.6:** su `CERTIFICADO-ANTERIOR.md` dice que Coviandina "conserva `SLV-002`";
  SERIE-2 lo cambia a `SLV-002-2026`.
- **Modelo de pantalla:** STA035WT-01 frente a STVA035WT(-01) (`HISTORIA.md`). Se mira la etiqueta.
- **Borrado de datos del HONOR** antes del registro de la rc3: hipótesis sin registro.
