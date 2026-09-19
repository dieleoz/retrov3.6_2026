# Retrorreflectómetro Vertical — línea V3.6 (2026)

Firmware, app y documentación de la **generación V3** del retrorreflectómetro vertical SAT-LUX/V3,
con **PIC18F47K42**, placa **"SATLUX H-IoT"** y pantalla **STONE STA035WT-01**, reconstruida en 2026.

> **SLV-002 (Coviandina, Concesionaria Vial Andina) está calibrado sólo en los códigos 1 y 2.** El
> banco de patrones está a medias y los códigos 8, b y 5 siguen de fábrica. Firmware **3.6.2** desde
> el 19-sep-2026 10:37. Acta aceptada a las 12:23:26 (ZIP de las 12:27, `afdd700`), vence el
> 2027-09-19; a las 15:10 el equipo respondió `#V#` = CAL 0003 (`8c8c7de`): los códigos 1 y 2
> persisten. **No hay APK entregable hoy**: la 3.6.15 no se entregó y la **3.6.16** está en desarrollo.
> El firmware original de SLV-002 se perdió al grabar (estaba protegido; pérdida autorizada por Diego).

## Por qué existe

El firmware de 2020 lleva las fórmulas de calibración **escritas en el código**: recalibrar exigía
recompilar y reprogramar. La **V3.6** las pasa a **EEPROM** y las deja cambiar **desde una app, en
modo administrador**, sin volver a tocar el PIC. Todo lo demás se mantiene como en 2020, para que la
pantalla y la app del cliente sigan funcionando. Contrato: [`05_Documentacion/PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md).

**La calibración es por equipo, nunca por modelo** (L-23): campaña, ajuste, coeficientes, fecha y
acta son de cada equipo físico. Entre equipos se reutilizan catálogo, método, firmware y app.

## Versiones vigentes — 19-sep-2026, ~17:15

| Pieza | Versión | Huella | Estado |
| :--- | :--- | :--- | :--- |
| Firmware | **3.6.2**: `#FT#`, serie `#SN`/`#GN` (EEPROM 0x1EE) y fecha de calibración `#SC`/`#GC` (0x200) | `.hex` md5 `9d5d5e39…`, commit `6a32ca3`; grabado en SLV-002 a las 10:37 (`78924ae`) | Mantenido por las revisiones P9 y P10, con condiciones |
| App `RetroV36` en manos de Diego | **3.6.11** (`fda734e`) | Con ella se midió el banco de las 15:10 (`HUELLAS.txt`) | Sólo para medir el banco |
| App revisada más reciente | **3.6.15** (`c7ef3c6`) | Revisión P14 (`0c319d9`) y QA (`a5c47b0`) | **No entregada**: tres defectos Altos (QA-3615-01 a 03) y condiciones de P14 |
| App en desarrollo | **3.6.16** | Sin commit a esta hora | Pendiente de arquitecto y QA |
| Serie en EEPROM | `SLV-002`; decidida **`SLV-002-2026`** (SERIE-2) | `DECISIONES-Diego-2026-09-19.md` | Se grabará con la 3.6.16. El nombre Bluetooth `COVIANDINA_SLV-002` no cambia |
| Catálogo de patrones | **P1-P132** (133 entradas: P32 duplicado como P32a/P32b) | `06_Calibracion/patrones_certificados_P1-P132.csv`, commit `bba4dbe` | Colas: completa, representativa (58) y anual (12), `PLAN-Banco-Representativo.md` |

**Regla de entrega:** a Diego sólo se le da una APK con el visto bueno escrito del arquitecto y de QA.
Cada APK se entrega con la versión en el nombre (`RTV-V<versión>.apk`); `*.apk` no se versiona.

**Café y lila** se miden con el código del rojo: 4 en intenso y b en tipo I. Queda abierto si entran
en el ajuste del rojo o sólo se verifican; la recomendación es **sólo verificar**.

**Segundo equipo:** resultó un **V4** (placa itvial v4.0 con la PPS de la V4.1; lectura ICSP en
`01_Firmware/lecturas_equipos/V3-2/`, con `IDENTIFICACION.md`). Es de Autopistas del Nordeste, serie
**SLV-003-2026**, y va a la V4.6 con la misma app.

## Documentos

| Documento | Qué es |
| :--- | :--- |
| [`ROADMAP.md`](ROADMAP.md) | **Qué se hace y en qué orden.** Puertas P1-P12, revisiones, el paso en curso. Empezar por aquí |
| [`RETOMAR.md`](RETOMAR.md) | Dónde se quedó el trabajo y el prompt para retomarlo |
| [`RUNBOOK.md`](RUNBOOK.md) | Procedimiento paso a paso para calibrar un equipo V3 con la V3.6 |
| [`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`](06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md) | Registro de decisiones de Diego (la app exige que las dispensas estén aquí) |
| [`05_Documentacion/PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md) | Contrato de órdenes `#...#` |
| [`05_Documentacion/SPEC-V3.6.md`](05_Documentacion/SPEC-V3.6.md), [`TDD-V3.6.md`](05_Documentacion/TDD-V3.6.md), [`MATRIZ-SPEC-codigo-V3.6.md`](05_Documentacion/MATRIZ-SPEC-codigo-V3.6.md) | Requisitos, pruebas y su cruce con el código |
| [`05_Documentacion/SPEC-Calibracion-V3.6.md`](05_Documentacion/SPEC-Calibracion-V3.6.md) | Calibración contra patrones: cobertura, campaña, ajuste, criterios, acta e informe |
| [`05_Documentacion/SPEC-App-Unica-V36-V46.md`](05_Documentacion/SPEC-App-Unica-V36-V46.md) | Una sola app RTV para V3.6 y V4.6 |
| `05_Documentacion/REVISION-Arquitectura-P9` a `P14-V3.6.md` | Revisiones de arquitectura del firmware y de las apps 3.6.6 a 3.6.15 |
| `05_Documentacion/QA-App-3.6.*.md`, [`QA-Flujo-Calibracion-V3.6.md`](05_Documentacion/QA-Flujo-Calibracion-V3.6.md) | QA de cada app y del flujo de calibración |
| [`05_Documentacion/PROPUESTA-Diagnostico-y-Logs-RTV.md`](05_Documentacion/PROPUESTA-Diagnostico-y-Logs-RTV.md) | Diagnóstico y registros de la app. Propuesta sin construir |
| [`05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md`](05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md) | App de producción: seguir en Android nativo; `targetSdk` (hoy 30) |
| [`05_Documentacion/SPEC-Registro-Indicador-Interventoria.md`](05_Documentacion/SPEC-Registro-Indicador-Interventoria.md) | Registro de medidas periódicas e indicador E11. Umbral en contradicción abierta (C-01) |
| [`05_Documentacion/PROCEDIMIENTO-Calibracion-V3-K42.md`](05_Documentacion/PROCEDIMIENTO-Calibracion-V3-K42.md) | Procedimiento de calibración del V3 K42 |
| [`06_Calibracion/PLAN-Banco-Representativo.md`](06_Calibracion/PLAN-Banco-Representativo.md) | Banco completo, representativo y anual. En papel |
| [`06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md`](06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md) | Propuesta de ajuste de los códigos 1 y 2 |
| [`06_Calibracion/SLV-002/COTEJO-Formulas-Historicas.md`](06_Calibracion/SLV-002/COTEJO-Formulas-Historicas.md) | Curvas de hoy frente a las históricas |
| [`06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md`](06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md) | Simulación fábrica / acta / propuesta por código |
| [`06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md`](06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md) | Acta de la grabación (G4) |
| `06_Calibracion/SLV-002/campanas/` + `HUELLAS.txt` | ZIP de las 10:33, 12:00, 12:10 (intermedio), 12:27 (acta aceptada) y 15:10 (banco parcial), con sus huellas |
| [`08_Senales/CATALOGO-Senales-Manual-2024.md`](08_Senales/CATALOGO-Senales-Manual-2024.md) | 376 señales verticales del Manual 2024 |
| [`ROADMAP-MEJORAS-App.md`](ROADMAP-MEJORAS-App.md) | 44 mejoras de la app de campo evaluadas para la V3.6 |
| [`ARQUITECTURA.map`](ARQUITECTURA.map), [`HISTORIA.md`](HISTORIA.md) | Grafo del sistema y cómo se llegó aquí |

## Estructura

| Carpeta | Qué hay |
| :--- | :--- |
| `01_Firmware/base_2020_d089f962/` | Fuente de 2020 **sin tocar** y su `.hex` (md5 `d089f962…`). Referencia de todas las comparaciones |
| `01_Firmware/RetroVertical_V3.6.X/` | Proyecto MPLAB X de la V3.6 (XC8 2.10) y `CAMBIOS-V3.6.md` |
| `01_Firmware/lecturas_equipos/` | `SLV-002/`: lectura ICSP y registros de grabación. `V3-2/`: lectura ICSP del segundo equipo (un V4) |
| `02_Hardware/` | PCB Proteus 3.3. El mapeo de la placa H-IoT está en el repositorio V5 (`HARDWARE-V3-SATLUX-H-IoT.md`) |
| `03_App_Movil/` | App `RetroV36` (RTV) y los APK entregados (`*.apk` no se versiona) |
| `04_Pantalla_STONE/` | Proyecto STONE de 2020. **La STONE no se modifica** |
| `05_Documentacion/` | Protocolo, SPEC, TDD, matriz, revisiones, QA y estudios |
| `06_Calibracion/` | Catálogos y colas de patrones, hojas de 2020 y una carpeta por equipo |
| `07 pruebas/` | Material en bruto del teléfono. **Sin versionar**; lo que vale pasa a `06_Calibracion/<equipo>/` con su huella |
| `08_Senales/` | Catálogo de señales del Manual 2024 |

## Procedencia

Todo lo copiado sale de `D:\@Proyect\IT\old\VERTICAL\`, que se conserva intacto. **Proyecto
independiente, con remoto privado propio: `github.com/dieleoz/retrov3.6_2026`.** El repositorio de la
línea V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`, `dieleoz/Retro_Vertical_2026_v1`) y el de la
V4.6 (`D:\IT\P_RetroVertical_V4.6`, `dieleoz/retrov4.6_2026`) son otros proyectos. Lo aprendido aquí
pasa a la V4.6 en su `APRENDIDO-DE-V3.6.md`.

## Reglas

- Toda afirmación sobre el código va con `archivo:línea`.
- **Nada se graba en un equipo** sin compilación verificada contra la base, revisión del cambio,
  prueba previa en otra placa si la hay y autorización del propietario del equipo.
- **Nada se da por escrito en un equipo sin su registro** (tramas o ZIP) en el repositorio.
- **Ninguna APK llega a Diego** sin el visto bueno del arquitecto y de QA.
- La serie del equipo se lee de la EEPROM (`#GN#`), no se teclea.
- Documentación en español, sin emojis.
