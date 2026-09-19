# Retrorreflectómetro Vertical — línea V3.6 (2026)

Firmware, app y documentación de la **generación V3** del retrorreflectómetro vertical SAT-LUX/V3,
con **PIC18F47K42**, placa **"SATLUX H-IoT"** y pantalla **STONE STA035WT-01**, reconstruida en 2026.

> **Nada de este repositorio se ha grabado ni probado todavía en un equipo.** La V3.6 está en
> construcción. El único equipo estudiado es SLV-002 (Concesionaria Vial Andina), y su firmware
> original **no se puede leer**: el chip está protegido. Grabar la V3.6 **borra el original sin
> vuelta atrás**.

## Por qué existe

El firmware de 2020 lleva las fórmulas de calibración **escritas en el código**: recalibrar exigía
recompilar y reprogramar. La **V3.6** las pasa a **EEPROM** y las deja cambiar **desde una app, en
modo administrador**, sin volver a tocar el PIC. Todo lo demás se mantiene como en 2020, para que la
pantalla y la app del cliente sigan funcionando. Contrato: [`05_Documentacion/PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md).

## Estado — 18-sep-2026, 20:00

| Pieza | Estado |
| :--- | :--- |
| Especificación (`SPEC-V3.6.md`) | **Escrita**: 27 RF de firmware, 22 de app, 23 de paridad y 66 pruebas. Sus 10 observaciones al protocolo se resolvieron en la **revisión 1.1** del PROTOCOLO |
| Revisión de arquitectura (puerta P2) | En curso |
| Firmware V3.6 | En construcción. **XC8 2.10 instalado** (`C:\Program Files (x86)\Microchip\xc8\v2.10`). Pendiente: reproducibilidad de la base y verificación de las fórmulas de fábrica |
| App V3.6 (`RetroV36`) | En construcción: modo de pruebas, medida P1-P31, visor de botones STONE y modo administrador |
| Grabación en SLV-002 | **GRABADO el 18-sep-2026 20:24** con el `.hex` `680b6a7d` (PICkit 3, IPE 5.50). Pendiente de probar con la app 3.6.2 (G4). Original perdido: estaba protegido |
| Software y bases de pantalla STONE | En curso: `04_Pantalla_STONE/SOFTWARE-STONE.md` |

## Estructura

| Carpeta o fichero | Qué hay |
| :--- | :--- |
| `ROADMAP.md` | Puertas P1-P12 y orden de ejecución. **Empezar por aquí** |
| [`RUNBOOK.md`](RUNBOOK.md) | Procedimiento paso a paso para calibrar un equipo V3 con la V3.6 (pensado para el segundo equipo) |
| `ROADMAP-MEJORAS-App.md` | 44 mejoras de la app de campo evaluadas para la V3.6 (18 aplican, 17 adaptadas, 9 no) y los defectos que no se deben repetir |
| `ARQUITECTURA.map` | Grafo del sistema: 85 nodos, contradicciones abiertas CA1-CA7 |
| `HISTORIA.md` | Cómo se llegó aquí y los errores que no deben repetirse |
| `01_Firmware/base_2020_d089f962/` | Fuente de 2020 **sin tocar** y su `.hex` (md5 `d089f962…`). Es la referencia de todas las comparaciones |
| `01_Firmware/RetroVertical_V3.6.X/` | Proyecto MPLAB X de la V3.6 (XC8 **2.10**). `CAMBIOS-V3.6.md` al terminar |
| `01_Firmware/lecturas_equipos/` | Lo leído por ICSP de cada equipo (SLV-002: protegido, sólo configuración) |
| `02_Hardware/` | PCB Proteus 3.3 (del Horizontal). El mapeo de la placa H-IoT está en el repositorio V5: `05_Documentacion/HARDWARE-V3-SATLUX-H-IoT.md`. ICSP en el conector `PICKIT3` de 6 pines |
| `03_App_Movil/` | App V3.6: medida, registro, modo de pruebas y modo administrador de calibración |
| `04_Pantalla_STONE/` | Proyecto STONE de 2020 (STVA035WT, 2.ª generación, `.vt`), diseños y la herramienta TOOL 2019. **La STONE no se modifica** |
| `05_Documentacion/` | `PROTOCOLO-V3.6.md` (**rev. 1.1**), `SPEC-V3.6.md`, procedimiento de calibración |
| `06_Calibracion/` | Patrones certificados P1-P31 (17 XI, 9 IV y 5 IX; sin tipo I) y las hojas de calibración de 2020 |
| `.claude/skills/` | `leer-planos-pcb` (Eagle, IPC-D-356, Proteus, cruce con el firmware) y `validar-botones-stone`, en curso |

## Procedencia

Todo lo copiado sale de `D:\@Proyect\IT\old\VERTICAL\`, que se conserva intacto como archivo con su
`INDICE.md` y los `VERSION.md`. La historia de cómo se llegó aquí está en
[`HISTORIA.md`](HISTORIA.md), y el grafo del sistema en [`ARQUITECTURA.map`](ARQUITECTURA.map).
**Es un proyecto independiente, con remoto privado propio: `github.com/dieleoz/retrov3.6_2026`.**
*(Antes decía "con su propio git local y sin remoto"; corregido el 18-sep-2026.)* El repositorio de la
línea V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`, GitHub `dieleoz/Retro_Vertical_2026_v1`) **es otro
proyecto**: aquí sólo se consulta, y la V3.6 no se sube a ese GitHub.

## Reglas

- Toda afirmación sobre el código va con `archivo:línea`.
- **Nada se graba en un equipo** sin: compilación verificada contra la base, revisión del cambio,
  prueba previa en otra placa si la hay, y autorización del propietario del equipo.
- Documentación en español, sin emojis.
