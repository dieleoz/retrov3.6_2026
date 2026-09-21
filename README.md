# Retrorreflectómetro Vertical — línea V3.6 (2026)

Firmware, app y documentación de la **generación V3** del retrorreflectómetro vertical SAT-LUX/V3
K42 de DPI Ingeniería & Consultoría: **PIC18F47K42**, placa **"SATLUX H-IoT"**, pantalla STONE de
2.ª generación, Bluetooth a **9600 8N1**. Nada está validado en un equipo salvo lo que respalda un ZIP
de campaña con su huella.

**El estado del día no está aquí**: está en [`RETOMAR.md`](RETOMAR.md). El orden de trabajo, en
[`ROADMAP.md`](ROADMAP.md). Las reglas, en [`CLAUDE.md`](CLAUDE.md). El mapa del sistema y el índice
de referencias cruzadas, en [`ARQUITECTURA.map`](ARQUITECTURA.map) §M1-§M5.

## Por qué existe

El firmware de 2020 lleva las fórmulas de calibración **escritas en el código**: recalibrar exigía
recompilar y reprogramar. La **V3.6** las pasa a **EEPROM** y las deja ajustar **desde la app, en modo
administrador**, sin volver a tocar el PIC. Todo lo demás se comporta como en 2020, para que la
pantalla y la app del cliente sigan funcionando. **La STONE no se toca.**

**La calibración es por equipo, nunca por modelo** (L-23): campaña, ajuste, coeficientes, fecha y
acta son de cada equipo físico. Entre equipos se reutilizan catálogo, método, firmware y app.

## Piezas

| Pieza | Dónde | Cómo se construye o se usa |
| :--- | :--- | :--- |
| Firmware V3.6 | `01_Firmware/RetroVertical_V3.6.X/` | MPLAB X, XC8 2.10; cambios en `CAMBIOS-V3.6.md`; pruebas en MPLAB SIM (`pruebas/`, `TDD-V3.6.md`) |
| Base de 2020 | `01_Firmware/base_2020_d089f962/` | Sin tocar; referencia de toda comparación |
| App RTV | `03_App_Movil/RetroV36/`; la de desarrollo en la rama **`rtv-1.0`** | JDK 11, Gradle 6.5; pruebas con JUnit a mano (`03_App_Movil/RetroV36/README.md`) |
| Contrato de órdenes `#...#` | `05_Documentacion/PROTOCOLO-V3.6.md` | Manda sobre firmware y app |
| Banco de patrones | `06_Calibracion/` (catálogo, colas) | Una carpeta por equipo con campañas, tramas, actas y `HUELLAS.txt` |
| Pantalla STONE | `04_Pantalla_STONE/` | Proyecto de 2020; sólo se cruza con el firmware |

## Documentos

| Documento | Qué es |
| :--- | :--- |
| [`RETOMAR.md`](RETOMAR.md) | **Estado vigente** y prompt para retomar |
| [`ROADMAP.md`](ROADMAP.md) | Qué falta, en qué orden y en qué sesión; puertas y decisiones pendientes |
| [`CLAUDE.md`](CLAUDE.md) | Reglas permanentes del repositorio |
| [`ARQUITECTURA.map`](ARQUITECTURA.map) | Mapa del sistema y referencias cruzadas entre documentos y código |
| [`RUNBOOK.md`](RUNBOOK.md) | Calibrar un equipo V3 con la V3.6, fase a fase |
| [`HISTORIA.md`](HISTORIA.md) | Cómo se llegó aquí y qué se concluyó mal |
| `05_Documentacion/SPEC-*.md` | Requisitos: firmware (`SPEC-V3.6`), calibración, app única, app corta |
| `05_Documentacion/TDD-V3.6.md`, `MATRIZ-SPEC-codigo-V3.6.md` | Pruebas y cruce SPEC ↔ código |
| `05_Documentacion/REVISION-*.md`, `QA-*.md` | Revisiones de arquitectura y QA de cada versión |
| `06_Calibracion/SLV-002/` | Procedimiento del 8 y la b, informe de ajuste, decisiones de Diego, actas |
| [`ROADMAP-MEJORAS-App.md`](ROADMAP-MEJORAS-App.md) | Mejoras de la app evaluadas para esta línea |

## Estructura

| Carpeta | Qué hay |
| :--- | :--- |
| `01_Firmware/` | Base 2020, proyecto V3.6 y lecturas ICSP de equipos (`lecturas_equipos/`) |
| `02_Hardware/` | PCB Proteus 3.3 |
| `03_App_Movil/` | App RTV y APK entregadas (`*.apk` no se versiona) |
| `04_Pantalla_STONE/` | Proyecto STONE de 2020 |
| `05_Documentacion/` | Protocolo, SPEC, TDD, matriz, revisiones, QA y estudios |
| `06_Calibracion/` | Catálogo y colas de patrones; una carpeta por equipo |
| `07 pruebas/` | Material en bruto del teléfono, **sin versionar**; lo que vale pasa a `06_Calibracion/<equipo>/` |
| `08_Senales/` | Catálogo de señales del Manual 2024 |
| `.claude/particularidades/` | Lo propio de cada skill `orquestador:*` (entregar, verificar, STONE, planos PCB) |

## Procedencia y proyectos vecinos

Todo lo copiado sale de `D:\@Proyect\IT\old\VERTICAL\`, que se conserva intacto. Remoto privado
propio: `github.com/dieleoz/retrov3.6_2026`. Son **otros proyectos**, que se consultan pero no se
mezclan: la V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`), la V4.6 (`D:\IT\P_RetroVertical_V4.6`,
equipos V4 como Nordeste) y Ruta al Mar (`D:\IT\P_RetroVertical_RutaAlMar`, SLV-028). Lo aprendido
aquí pasa a la V4.6 en su `APRENDIDO-DE-V3.6.md`.

## Reglas en corto

- Toda afirmación sobre el código va con `archivo:línea` verificado.
- **Nada se graba en un equipo** sin las cuatro puertas de `CLAUDE.md` §3. Grabar es irreversible.
- **Nada se da por escrito en un equipo sin su registro** (tramas o ZIP) en el repositorio.
- **Ninguna APK llega a Diego** sin el visto bueno escrito del arquitecto y de QA.
- Nunca se envía `e` a un equipo no identificado como V3.6.
- Documentación en español, sin emojis.
