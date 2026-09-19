# Retrorreflectómetro Vertical — línea V3.6 (2026)

Firmware, app y documentación de la **generación V3** del retrorreflectómetro vertical SAT-LUX/V3,
con **PIC18F47K42**, placa **"SATLUX H-IoT"** y pantalla **STONE STA035WT-01**, reconstruida en 2026.

> **SLV-002 (Concesionaria Vial Andina) está calibrado sólo en los códigos 1 y 2**, y sin validar
> con el banco completo. Lleva el firmware **3.6.2** desde el 19-sep-2026 10:37. El acta se aceptó a
> las 12:23:26. Los códigos 1 y 2 se verificaron con `#E` y con re-medida CONFORME (P28 y P5). El
> código 8 no se escribió. Serie `SLV-002`; `#SC` 2026-09-19, que vence el 2027-09-19. Evidencia:
> ZIP de las 12:27, md5 `ce1f35fc…`, commit `afdd700`. El resto de códigos sigue de fábrica. La QA de
> la app 3.6.9 dice que **no está lista para un operador sin soporte**. El firmware original de SLV-002 se perdió
> al grabar (estaba protegido; pérdida autorizada por Diego).

## Por qué existe

El firmware de 2020 lleva las fórmulas de calibración **escritas en el código**: recalibrar exigía
recompilar y reprogramar. La **V3.6** las pasa a **EEPROM** y las deja cambiar **desde una app, en
modo administrador**, sin volver a tocar el PIC. Todo lo demás se mantiene como en 2020, para que la
pantalla y la app del cliente sigan funcionando. Contrato: [`05_Documentacion/PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md).

**La calibración es por equipo, nunca por modelo** (L-23): campaña, ajuste, coeficientes, fecha y
acta son de cada equipo físico. Entre equipos se reutilizan catálogo, método, firmware y app.

## Versiones vigentes — 19-sep-2026, 12:30

Comprobado con `git log` y `md5sum` a esa hora.

| Pieza | Versión | Huella | Estado |
| :--- | :--- | :--- | :--- |
| Firmware | **3.6.2**: `#FT#`, serie `#SN`/`#GN` (EEPROM 0x1EE) y fecha de calibración `#SC`/`#GC` (0x200) | `.hex` md5 `9d5d5e39…`, commit `6a32ca3`; grabado en SLV-002 a las 10:37 (`78924ae`) | Mantenido por la revisión P9 **con condiciones** (P9-A3 y P9-A5) |
| App `RetroV36` | **3.6.9** (`versionName` en `app/build.gradle:15`) | commit `f52eeb1`; `03_App_Movil/RTV-V3.6.9.apk` md5 `3fbb68f3…` (igual a `RTV-V3.6.apk`) | QA ISTQB: **no apta sin soporte** (17 defectos) |
| Catálogo de patrones | **P1-P132** (133 entradas: P32 duplicado como P32a/P32b) | `06_Calibracion/patrones_certificados_P1-P132.csv`, commit `bba4dbe` | **La app 3.6.9 todavía carga el P1-P31 + tipo I** (`app/src/main/assets/patrones_certificados_P1-P31.csv`, 59 entradas) |

Lo que trajeron las apps del día (3.6.3 a 3.6.9): patrones tipo I y disparo de asentamiento (3.6.4);
campaña guiada por equipo con un solo ZIP (3.6.5); medida por K × M colocaciones, serie y fecha de
calibración (3.6.7); comprobación de oscuro, condición P9-B13 (3.6.8); acta; importar el ZIP de una
campaña exportada, recta anclada en oscuro y preajustes A5 y OSCURO (3.6.9). Cada APK se entrega
también con la versión en el nombre (`RTV-V<versión>.apk`, `85d949b`).

**Café y lila** (nuevos en el P1-P132) se miden con el código del rojo: 4 en intenso y b en tipo I.
Queda abierto si entran en el ajuste del rojo o sólo se verifican; la recomendación es **sólo
verificar**.

## Documentos

| Documento | Qué es |
| :--- | :--- |
| [`ROADMAP.md`](ROADMAP.md) | **Qué se hace y en qué orden.** Puertas P1-P12 y el ciclo en curso. Empezar por aquí |
| [`RETOMAR.md`](RETOMAR.md) | Dónde se quedó el trabajo y el prompt para retomarlo |
| [`RUNBOOK.md`](RUNBOOK.md) | Procedimiento paso a paso para calibrar un equipo V3 con la V3.6 |
| [`05_Documentacion/PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md) | Contrato de órdenes `#...#`, con las de la 3.6.2 |
| [`05_Documentacion/SPEC-V3.6.md`](05_Documentacion/SPEC-V3.6.md), [`TDD-V3.6.md`](05_Documentacion/TDD-V3.6.md), [`MATRIZ-SPEC-codigo-V3.6.md`](05_Documentacion/MATRIZ-SPEC-codigo-V3.6.md) | Requisitos, pruebas y su cruce con el código. **En revisión** para el flujo nuevo (ciclo, paso 1) |
| [`05_Documentacion/SPEC-Calibracion-V3.6.md`](05_Documentacion/SPEC-Calibracion-V3.6.md) | Calibración contra patrones: cobertura por código, campaña, ajuste, criterios **propuestos**, acta, superadministrador e informe. En revisión |
| [`05_Documentacion/REVISION-Arquitectura-P9-V3.6.md`](05_Documentacion/REVISION-Arquitectura-P9-V3.6.md) | Revisión P9 (r2): la 3.6.2 se mantiene; condiciones P9-A1 a A5 y P9-B1 a B13 para escribir |
| [`05_Documentacion/QA-Flujo-Calibracion-V3.6.md`](05_Documentacion/QA-Flujo-Calibracion-V3.6.md) | QA ISTQB de la app 3.6.9: 17 defectos, flujo propuesto "modo banco" + "Calibrar este equipo" y 22 casos de aceptación |
| [`05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md`](05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md) | Viabilidad de la app de producción: seguir en Android nativo; subir `targetSdk` (hoy 30) |
| [`05_Documentacion/SPEC-Registro-Indicador-Interventoria.md`](05_Documentacion/SPEC-Registro-Indicador-Interventoria.md) | Registro de medidas periódicas e indicador E11 (SFT: > 70 % del valor original). Umbral en contradicción abierta con AT4, AT2 y Manual 2024 (C-01) |
| [`05_Documentacion/PROCEDIMIENTO-Calibracion-V3-K42.md`](05_Documentacion/PROCEDIMIENTO-Calibracion-V3-K42.md) | Procedimiento de calibración del V3 K42 |
| [`06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md`](06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md) | Propuesta de ajuste de SLV-002 con las decisiones de Diego de las 11:20 |
| [`06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md`](06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md) | Acta de la grabación (G4) |
| `06_Calibracion/SLV-002/campanas/` + `HUELLAS.txt` | ZIP de las 10:33, 12:00, 12:10 (estado intermedio) y 12:27 (acta aceptada), con sus huellas |
| [`08_Senales/CATALOGO-Senales-Manual-2024.md`](08_Senales/CATALOGO-Senales-Manual-2024.md) | 376 señales verticales del Manual 2024 con iconos propios (`senales.csv`, `indice.html`) |
| [`ROADMAP-MEJORAS-App.md`](ROADMAP-MEJORAS-App.md) | 44 mejoras de la app de campo evaluadas para la V3.6 |
| [`ARQUITECTURA.map`](ARQUITECTURA.map), [`HISTORIA.md`](HISTORIA.md) | Grafo del sistema y cómo se llegó aquí |

## Estructura

| Carpeta | Qué hay |
| :--- | :--- |
| `01_Firmware/base_2020_d089f962/` | Fuente de 2020 **sin tocar** y su `.hex` (md5 `d089f962…`). Referencia de todas las comparaciones |
| `01_Firmware/RetroVertical_V3.6.X/` | Proyecto MPLAB X de la V3.6 (XC8 2.10) y `CAMBIOS-V3.6.md` |
| `01_Firmware/lecturas_equipos/SLV-002/` | Lectura ICSP (protegido) y registros de grabación de la 3.6, 3.6.1 y 3.6.2 |
| `02_Hardware/` | PCB Proteus 3.3. El mapeo de la placa H-IoT está en el repositorio V5 (`HARDWARE-V3-SATLUX-H-IoT.md`) |
| `03_App_Movil/` | App `RetroV36` y los APK entregados (`*.apk` no se versiona) |
| `04_Pantalla_STONE/` | Proyecto STONE de 2020. **La STONE no se modifica** |
| `05_Documentacion/` | Protocolo, SPEC, TDD, matriz, revisiones, QA y estudios |
| `06_Calibracion/` | Catálogos de patrones (P1-P31 y P1-P132), hojas de 2020 y una carpeta por equipo |
| `07 pruebas/` | Material en bruto que comparte el teléfono. **Sin versionar**; lo que vale pasa a `06_Calibracion/<equipo>/` con su huella |
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
- La serie del equipo se lee de la EEPROM (`#GN#`), no se teclea.
- Documentación en español, sin emojis.
