# ROADMAP — V3.6: qué falta hasta certificar SLV-002, y en qué orden

**SLV-002: calibrados 1, 2, 8 y b (máscara 0283) y certificado emitido. Faltan 3, 4, 5 y 6.**
Este fichero dice qué falta, en qué orden y quién lo hace; no es una bitácora. Las cifras del día van en
[`RETOMAR.md`](RETOMAR.md); lo que pasó, en `git log` y [`HISTORIA.md`](HISTORIA.md). Reglas en
[`CLAUDE.md`](CLAUDE.md); mapa en `ARQUITECTURA.map` §M2-§M3; decisiones en
`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`.

## Objetivo

**SLV-002 (Coviandina) certificado** (8 y b escritos, verificados y con certificado) y las **dos apps
de esta línea terminadas**, cada una sólo para el firmware V3.6 (USR-ALCANCE):

- **Calibrar** (`Cov_<v>_calibrar`), de DPI y por USB (APPS-DPI): cargar el ZIP, un botón "Calibrar",
  verificar ±5/±10 %, certificado (`SPEC-App-Calibracion-Coviandina.md` §8).
- **Usuario** (`RetroUsuario`), va con el equipo: medir y exportar; señal a señal opcional; indicador por
  norma (`SPEC-App-Usuario-V3.6.md`).

La toma (TOMA-50) y el certificado en PDF van detrás, en A5b: hoy el certificado se hace **a mano** desde
las actas `.txt`, en el formato de ITVIAL (RF-CAL-30).

## Orquestación de subagentes

Un escritor por árbol, cada uno en su worktree y con su rama. Modelos: **opus sólo para `arquitecto-iot`;
el resto sonnet o haiku; Fable sólo con autorización de Diego.** Un Alto se reabre en el código antes de
darlo por bueno. **Hasta el 25-sep (límite semanal):** sólo la entrega al cliente; revisiones con sonnet y
primera pasada en `orquestador:segunda-opinion`.

**Entrega a Coviandina (cierra P12 para SLV-002).** **E1:** firmar
`06_Calibracion/SLV-002/actas/CERTIFICADO-Calibracion-SLV-002-20260922.pdf` (faltan ciudad, cédula y cargo
del firmante, y datos del cliente) y mandarlo **con** `campanas/soporte_SLV-002_20260922_144415.zip`, que
lleva las actas, el diario y las tramas. **E2:** la APK de usuario, tras B7.

**Carril A — app de calibrar.** La entregada es `Cov_3.6.5_calibrar` (rama `rtv-1.0-cierre`, `316a6bc`,
md5 en `RETOMAR.md`); es la única con arquitecto y QA, y la que usa el funcional.

1. **A4b.** `Cov_3.6.8_calibrar` (`31e6214`): QA **NO APTO** (filtro fuera de la frontera). La rama
   `rtv-1.0-simple` (`6d307a2`) añade pacing UART, trama corta y T-C41 guiado, pero sale de ese mismo
   `31e6214` y repite su par `versionCode 10013` / `Cov_3.6.8_calibrar` (defecto, `CLAUDE.md` §6):
   antes de tocarla, rebasar sobre `316a6bc` y darle versión propia.
2. **A4c.** Defectos vistos por el funcional que van a la SPEC antes que al
   código: la app vuelve a ofrecer códigos ya aceptados; no dice en qué paso va y el operador acaba
   forzando el cierre (pierde el acta, `Base.java:254-262`); T-C41 depende de cuándo se pulse OK y manda
   a reconectar a mano; el `pruebas.txt` del ZIP es el de la tanda anterior. Va también la etiqueta
   `Retro Coviandina Calibrar` y el fichero `RETRO-COVIANDINA-calibrar-<versionName>-<versionCode>.apk`,
   que la 3.6.5 entregada todavía no lleva.
3. **A5a. Aprobada** (TOMA-SEL-LISTA): 46 patrones, en `06_Calibracion/SLV-002/PROPUESTA-Toma-Corta.md`.
4. **A5b (al cerrar A4b).** Certificado PDF, verificación de 10 patrones y toma corta, con SPEC,
   arquitecto, código y QA. Repetir o Saltar con su motivo (REPETIR-PREGUNTA), firma DPI (FIRMA-DPI) y
   elección del **tipo de lámina**, que el certificado dice por tipo y color.

**Carril B — app de usuario.** Incremento 1 "Medir y exportar". Regla que dejó el revisor Fable
(FABLE-USR, A-09): la medida, la pregunta y el resultado viven en `dominio/EstadoMedida`, no en la pantalla.

1. **B7 (siguiente).** APK lista: `RETRO-COVIANDINA-usuario-0.3.7-10.apk` (`418991f`, arquitecto y QA
   APTO, misma firma que la instalada: se actualiza sin desinstalar). Prueba de Diego en dos teléfonos
   (Android ≤9 y ≥10): giro con la pregunta abierta, giro midiendo, Atrás desde Medir, apagar el equipo,
   exportar; después, a Julio.
2. **B8.** Incremento 2 "señal a señal" (histórico e inventario por CSV; sin dictamen, UMBRAL-CSV).

## Puertas P1-P12

| Puerta | Estado |
| :--- | :--- |
| P1 Especificación · P2 Arquitectura · P3 Compilación reproducible · P6 Autorización | Cerradas |
| P4 Firmware · P7 Grabación | Cerradas para la 3.6.2 |
| P5 App · P9 Validación del arquitecto | **Abiertas** (carriles A y B) |
| P8 Calibración de SLV-002 | Cerrada para 1, 2, 8 y b (máscara 0283); **abierta** para 3, 4, 5 y 6 |
| P10-P11 Producción · P12 Informe y registros | Pendientes (A5, B) |

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir |
| :--- | :--- |
| D-5 | P32a/P32b: identidad del P32 duplicado |
| D-11 | Clave de firma estable para las dos apps, fuera del repositorio. Hoy se firma con la de depuración: si cambia de máquina, hay que desinstalar y se pierde la campaña. Propuesta hecha y sin crear |
| D-12 | Las cuatro propuestas abiertas de `SPEC-App-Usuario-V3.6.md` §5 (GPS a 15 m, separador y decimal del CSV, nombre del ZIP con varios equipos, cuarentena con reintentos): bloquean el incremento 2 |
| D-13 | `campanas/salida/`: el pre-commit rechaza su informe generado por tres líneas de más de 120 caracteres. O se arregla el generador, o queda fuera del repositorio |
| D-14 | Etiquetas y origen de los certificados de **P112** y **P124**: cierran C-08 y dicen si el azul y el verde se pueden calibrar. Las trae el funcional |

Los códigos 1 y 2 no se reescriben; los certificados del 3 y el 5 se cotejan antes de ajustarlos.

## Contradicciones abiertas

No se eligen: se cierran midiendo o con registro. Las cerradas están en `HISTORIA.md`.

- **Catálogo de patrones azules y verdes (C-08).** Bloquea los códigos 3, 4, 5 y 6. Medido: el equipo
  repite (9 patrones, ±2 cuentas) y correlaciona +0,98 en amarillo y +0,96 en rojo, pero en azul y verde
  con certificado ≥ 40 la correlación es **negativa** (−0,68 y −0,42): P18 (XI, 84) lee 36 y P112 (XI,
  101) lee 25. Ni una curva por color y tipo lo arregla (azul XI −37 %, verde IX +103 %). Se cierra con
  D-14. Hasta entonces no se ajusta nada: forzarlo sería escribir una calibración falsa.
- **Geometría del equipo (C-06)** sin documentar: el mínimo del Manual 2024 queda sin dictamen hasta
  saber qué fila de ángulo aplica.
- **Manual 2024 frente a NTC 4739 (C-07)** en una celda de la tabla.
- **Serie en la V4.6:** su `CERTIFICADO-ANTERIOR.md` dice que Coviandina "conserva `SLV-002`";
  SERIE-2 lo cambia a `SLV-002-2026`.
- **Modelo de pantalla:** STA035WT-01 frente a STVA035WT(-01) (`HISTORIA.md`). Se mira la etiqueta.
- **Numeración:** "P10-P14" nombra puertas del proceso y revisiones de arquitectura de la app.
- **Formato de `#V#`:** `PROTOCOLO-V3.6.md:42` da cuatro campos; el firmware manda cinco, con la máscara
  (`calibracion_v36.c:636-641`). Manda el protocolo (`CLAUDE.md` §5): o se corrige el protocolo o el firmware.
