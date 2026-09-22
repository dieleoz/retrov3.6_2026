# ROADMAP — V3.6: qué falta hasta certificar SLV-002, y en qué orden

**Los códigos 8 y b siguen sin escribir en el equipo; ninguna APK tiene aún arquitecto y QA aptos.**
Este fichero dice qué falta, en qué orden y quién lo hace; no es una bitácora. Las cifras del día van en
[`RETOMAR.md`](RETOMAR.md); lo que pasó, en `git log` y [`HISTORIA.md`](HISTORIA.md). Reglas en
[`CLAUDE.md`](CLAUDE.md); mapa en `ARQUITECTURA.map` §M2-§M3; decisiones en
`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`.

## Objetivo

**SLV-002 (Coviandina) certificado** (8 y b escritos, verificados y con certificado) y las **dos apps
de esta línea terminadas**, cada una sólo para el firmware V3.6 (USR-ALCANCE):

- **Calibrar** (`Cov_<v>_calibrar`), de DPI y por USB (APPS-DPI): cargar el ZIP, un botón "Calibrar",
  verificar ±5/±10 %, certificado. SPEC: `SPEC-App-Calibracion-Coviandina.md` §8.
- **Usuario** (`RetroUsuario`), va con el equipo: medir y exportar; señal a señal opcional; indicador
  por norma (UMBRAL-LEY). SPEC: `SPEC-App-Usuario-V3.6.md`.

La toma de muestras (hasta 50, TOMA-50) y el PDF de certificado van detrás, en la app de calibrar.

## Orquestación de subagentes

Un escritor por árbol; el código, en el worktree de la rama `rtv-1.0-cierre` (calibrar) o en una rama
nueva (usuario). Modelos: **opus sólo para `arquitecto-iot`; el resto sonnet o haiku; Fable sólo con
autorización de Diego.** Cada paso empieza cuando llega el informe del anterior; un Alto se reabre en el
código antes de darlo por bueno.

**Primero, la certificación de SLV-002.** Lo que la bloquea es A4, en el banco y en manos de Diego; la
app de usuario no certifica nada y va como segundo carril, sin quitarle turno a A4.

**Carril A — app de calibrar.** `Cov_3.6.5_calibrar` (rama `rtv-1.0-cierre`, `316a6bc`, md5 en `RETOMAR.md`):
QA APTO y arquitecto APTO CON CONDICIONES; entregable a Diego para calibrar el 8 y la b.

1. **A4 (bloquea la certificación; en manos del funcional, se espera su ZIP).** Con la 3.6.5 y el ZIP del
   banco de `06_Calibracion/SLV-002/campanas/`, calibrar el 8 y la b; actas y ZIP resultante al repositorio.
2. **A4b (bucle roto con A4B-FILTRO).** `Cov_3.6.8_calibrar` (`3f72367`): filtro único `TextoOperador` y acta
   oculta en esta app; regresión de `Cov366Rf17Test` arreglada en `31e6214` (411/411). Arquitecto y QA en curso.
3. **A5a.** Propuesta en `06_Calibracion/SLV-002/PROPUESTA-Toma-Corta.md` (simulación, sin medir): 46
   patrones, ajustar 6 y 4; 3 y 5 con dato invertido; café fuera con la curva del rojo. Aprobada
   (TOMA-SEL-LISTA).
4. **A5b (siguiente, al cerrar A4b).** Certificado PDF, verificación de 10 patrones y toma corta: SPEC,
   arquitecto, código y QA. Toda repetición dice el motivo y ofrece Repetir o Saltar
   (REPETIR-PREGUNTA); firma DPI (FIRMA-DPI).

**Carril B — app de usuario (segundo carril).** Incremento 1 "Medir y exportar". Regla que dejó el
revisor Fable (FABLE-USR, A-09): la medida, la pregunta y el resultado viven en `dominio/EstadoMedida`, no
en la pantalla.

1. **B6 (CERRADO en rama `bb`).** `RetroUsuario` 0.3.7 (`com.dpi.retrousuario.coviandina`, versionCode 10,
   versionName 0.3.7): **arquitecto APTO y QA APTO** (152/152 pruebas JVM reproducidas: 107 requisito /
   45 comportamiento). Etiqueta visible `"Retro Coviandina"` en teléfono y binario entregable
   `RETRO-COVIANDINA-usuario-0.3.7-10.apk`. Misma firma (`c990adf6...`) para actualización directa
   sin desinstalar.
2. **B7 (Siguiente).** Prueba de Diego en dos teléfonos (Android ≤9 y ≥10): giro con la pregunta abierta, giro
   midiendo, Atrás desde Medir, apagar el equipo, exportar; después, entrega al funcional (Julio).
3. **B8.** Incremento 2 "señal a señal" (histórico e inventario por CSV; sin dictamen, UMBRAL-CSV).

## Puertas P1-P12

| Puerta | Estado |
| :--- | :--- |
| P1 Especificación · P2 Arquitectura · P3 Compilación reproducible · P6 Autorización | Cerradas |
| P4 Firmware · P7 Grabación | Cerradas para la 3.6.2 |
| P5 App · P9 Validación del arquitecto | **Abiertas** (carriles A y B) |
| P8 Calibración de SLV-002 | Cerrada para 1 y 2; **abierta** para 8 y b (y 3, 4, 5, 6) |
| P10-P11 Producción · P12 Informe y registros | Pendientes (A5, B) |

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir |
| :--- | :--- |
| D-5 | P32a/P32b: identidad del P32 duplicado |

Los códigos 1 y 2 no se reescriben. Los certificados de los códigos 3 y 5 (dato invertido) se cotejan
contra el otro patrón antes de ajustarlos.

## Contradicciones abiertas

No se eligen: se cierran midiendo o con registro. Las cerradas están en `HISTORIA.md`.

- **Geometría del equipo (C-06)** sin documentar: el mínimo del Manual 2024 queda sin dictamen hasta
  saber qué fila de ángulo aplica.
- **Manual 2024 frente a NTC 4739 (C-07)** en una celda de la tabla.
- **Serie en la V4.6:** su `CERTIFICADO-ANTERIOR.md` dice que Coviandina "conserva `SLV-002`";
  SERIE-2 lo cambia a `SLV-002-2026`.
- **Modelo de pantalla:** STA035WT-01 frente a STVA035WT(-01) (`HISTORIA.md`). Se mira la etiqueta.
- **Numeración:** "P10-P14" nombra puertas del proceso y revisiones de arquitectura de la app.
- **Formato de `#V#`:** `PROTOCOLO-V3.6.md:42` da cuatro campos; el firmware manda cinco, con la máscara
  (`calibracion_v36.c:636-641`). Manda el protocolo (`CLAUDE.md` §5): o se corrige el protocolo o el firmware.
