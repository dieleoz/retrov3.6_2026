# ROADMAP — V3.6: qué se hace y en qué orden

**Actualizado:** 19-sep-2026, 12:30. Lo que no está aquí no está en ejecución. **Ningún equipo tiene
todavía una calibración con acta aceptada.** SLV-002 lleva el firmware **3.6.2**; el código 1 está
escrito y comprobado con `#E`, pero **sin re-medida registrada**, y lo escrito después de las 12:04 no
tiene registro en el repositorio. Procedimiento paso a paso: [`RUNBOOK.md`](RUNBOOK.md).

## Objetivo

Dejar los **dos equipos V3** (SLV-002 y el segundo) con firmware **V3.6**: la calibración pasa a
EEPROM y se ajusta **desde la app**, sin volver a reprogramar, con serie y fecha de calibración
guardadas en el propio equipo. La pantalla STONE y la app del cliente siguen funcionando igual.
Después, app de producción, informe de ajuste y registros para la interventoría. Los dos V4, en su
propio repositorio, cuando se cierre la V3.6.

**Por equipo, nunca por modelo (L-23).** Campaña, ajuste, coeficientes, fecha y acta son de **cada
equipo físico**. Entre equipos se reutilizan el catálogo de patrones, el método, el firmware y la app.

## DÓNDE QUEDAMOS — 19-sep-2026, ~12:30

**Firmware.** SLV-002 lleva la **3.6.2** (`.hex` md5 `9d5d5e39…`, commit `6a32ca3`), grabada a las
10:37 (`78924ae`, registro en `01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.2_2026-09-19.log`).
Añade `#FT#`, serie (`#SN`/`#GN`) y fecha de calibración (`#SC`/`#GC`). La revisión P9 r2 la **mantiene
con condiciones** ([`REVISION-Arquitectura-P9-V3.6.md`](05_Documentacion/REVISION-Arquitectura-P9-V3.6.md) §1).
A las 11:20 la app 3.6.8 la confirmó en el equipo (`#GC,NONE#`, APTO dos veces; propuesta,
"Decisiones de Diego").

**App.** Vigente la **3.6.9** (`f52eeb1`, APK md5 `3fbb68f3…`, 89 tests JVM). La QA ISTQB
([`QA-Flujo-Calibracion-V3.6.md`](05_Documentacion/QA-Flujo-Calibracion-V3.6.md), `f45d2a8`) concluye
que **no está lista para un operador sin soporte**: 17 defectos, entre ellos el acta que se pierde al
reconectar (D-02), la serie que se sobrescribe con un toque (D-04), la identidad sacada del nombre
Bluetooth y no de la EEPROM (D-05) y la campaña que borra una desinstalación (D-09). Propone un
**modo banco** (medir) y un **"Calibrar este equipo"** de un solo botón.

**Campañas de SLV-002** (`06_Calibracion/SLV-002/campanas/`, huellas en `HUELLAS.txt`):

- **10:33**: 50 patrones P1-P50, 55 series, app 3.6.5 y firmware 3.6.1 (md5 `4c50dbf6…`).
- **12:00**: importa la de 10:33 y añade **A5** y **OSCURO** (md5 `3e2c913b…`), app 3.6.9 y firmware
  3.6.2. **A5 NO CONCLUYENTE**: s_rep media 2,24 %, P22 −5,6 % (fuera), P28 −2,1 %, P4 −1,1 %; no
  obliga a revertir la 3.6.2. **OSCURO: x = 565,4** (s 2,0). La batería del equipo se cambió hacia
  las 11:40, a mitad de sesión.
- En `07 pruebas/19092026_1210/` hay un ZIP de las **12:10** que no está en `HUELLAS.txt` ni
  versionado; la QA lo cita (D-02: "no lleva acta").

**Propuesta y decisiones** ([`PROPUESTA-Ajuste-SLV-002-2026-09-19.md`](06_Calibracion/SLV-002/PROPUESTA-Ajuste-SLV-002-2026-09-19.md),
decisiones de Diego de las 11:20):

- **código 1**, blanco intenso: grado 1 aceptado, aunque incumple RF-CAL-14, 15 y 16;
- **código 2**, amarillo intenso: recta anclada en oscuro;
- **reproducibilidad**: el criterio sale de la A5, no del "máx(3·s ; 1 %)".

**Escritura en SLV-002. Estado en parte sin confirmar:**

| Qué | Evidencia | Estado |
| :--- | :--- | :--- |
| Código 1, grado 1 | `#S,1,…,2.98471545E-01,-1.62263869E+02` → `#OK#` a las 12:04:34, relectura y `#E` conformes (QA §1, T4:1660-1698) | **Escrito y comprobado con `#E`. Sin re-medida registrada** |
| Re-medidas y códigos 8 y 2 | Sólo lo dicho por Diego: hechos hacia las 12:25 | **Sin ZIP ni registro de tramas en el repositorio. Sin confirmar** |
| Serie en EEPROM | `#SN,SLV-002#` a las 11:48:52 y `#SN,SLV-02#` a las 12:09:02 (QA §1, T4:593 y T4:2048) | Según Diego, **corregida después a `SLV-002`**. Sin registro que lo confirme |
| Fecha de calibración | — | Sin constancia de `#SC` |
| Acta | — | **Sin aceptar** |

**Catálogo nuevo P1-P132** (`06_Calibracion/patrones_certificados_P1-P132.csv`, `bba4dbe`): 133
entradas (P32 sigue duplicado como P32a azul y P32b naranja). Con él, **los códigos intensos 3 a 6**
(verde, rojo, azul y naranja) tienen niveles suficientes para ajustarse y dejan de ser "sólo
verificar". **Café y lila** se miden con el código del rojo (4 intenso, b tipo I); queda abierto si
entran en el ajuste del rojo o sólo se verifican (recomendación: **sólo verificar**). La app 3.6.9
**todavía no lee este catálogo**: carga `assets/patrones_certificados_P1-P31.csv` (59 entradas).

**Otros documentos del día:** [`ESTUDIO-Tecnologia-App-Produccion.md`](05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md)
(seguir en Android nativo; subir `targetSdk` desde 30),
[`SPEC-Registro-Indicador-Interventoria.md`](05_Documentacion/SPEC-Registro-Indicador-Interventoria.md)
(SFT: > 70 % del valor original, mensual, 95 %; umbral en contradicción C-01 con AT4, AT2 y Manual 2024),
[`08_Senales/`](08_Senales/CATALOGO-Senales-Manual-2024.md) (376 señales del Manual 2024) y
[`SPEC-Calibracion-V3.6.md`](05_Documentacion/SPEC-Calibracion-V3.6.md).

## AHORA — el ciclo acordado con Diego, en este orden

Ningún paso empieza sin cerrar el anterior.

| # | Paso | Quién | Sale | Estado |
| :---: | :--- | :--- | :--- | :--- |
| 1 | **SPEC + TDD del flujo nuevo**: modo banco, "Calibrar este equipo" con un botón, serie leída de `#GN#`, acta persistente, catálogo P1-P132 leído de CSV, comprobación de oscuro, A5 y colocaciones | Subagente | `SPEC-V3.6.md`, `SPEC-Calibracion-V3.6.md`, `TDD-V3.6.md`, matriz | **En curso** |
| 2 | **Arquitecto**: revisión del paso 1 y del delta de las apps 3.6.7-3.6.9, que la P9 r2 no revisó | Subagente, modelo capaz | Veredicto escrito | Tras 1 |
| 3 | **App** (y firmware, sólo si el paso 2 lo exige) | Subagente | APK atado a un commit, md5, tests en verde | Tras 2 |
| 4 | **QA ISTQB de la APK** y visto bueno | Subagente | Informe con los 22 casos de aceptación | Tras 3 |
| 5 | **Diego mide el banco completo** en SLV-002: 133 patrones más A5 y OSCURO, con la app guiada, en **un solo ZIP** | Diego | ZIP en `06_Calibracion/SLV-002/campanas/` con su huella | Tras 4 |
| 6 | **Calibrar con un botón** | Diego con la app | Códigos escritos, relectura, `#E` y re-medida en el registro | Tras 5 |
| 7 | **Acta y fecha** (`#SC` sólo con el acta aceptada) | Diego | Acta en `06_Calibracion/SLV-002/` | Tras 6. Cierra P8 |

Mientras tanto, **no se escribe nada más en SLV-002**. Lo escrito hoy se reconcilia en el paso 5: la
campaña completa lee con los coeficientes que el equipo tenga (`#G` de los 12 códigos al empezar).

## Puertas P1-P12

| Puerta | Qué tiene que cumplirse | Estado a las 12:30 |
| :--- | :--- | :--- |
| **P1 — Especificación** | `SPEC-V3.6` con RF de firmware y app, paridad y pruebas | Cerrada en r1.1 (18-sep) y reconciliada en r1.2 (`b7783fd`). **Reabierta** por el flujo nuevo (ciclo, paso 1) |
| **P2 — Arquitectura** | Revisión adversaria de SPEC, PROTOCOLO y código | **Cerrada**, 18-sep 19:48 (APROBADO CON CONDICIONES) |
| **P3 — Compilación reproducible** | XC8 2.10 compila la base 2020 idéntica a `d089f962` | **Cerrada**, 18-sep |
| **P4 — Firmware** | `.hex` atado a un commit; fábrica = ecuaciones de 2020 (T-A20) | **Cerrada la 3.6.2** (`6a32ca3`, `9d5d5e39…`; simulador: T-A20 sin diferencias) |
| **P5 — App** | APK con tests en verde y modo de pruebas | 3.6.9 compilada (`f52eeb1`, 89 tests JVM). **Reabierta:** la QA la da por no apta sin soporte (ciclo, pasos 3-4) |
| **P6 — Autorización** | Visto bueno del propietario a reprogramar | **Cerrada**, 18-sep (Diego) |
| **P7 — Grabación** | Verificación tras grabar | **Cerrada para la 3.6.2**: grabada a las 10:37, confirmada por la app 3.6.8 a las 11:20 |
| **P8 — Calibración de SLV-002** | Campaña, ajuste, escritura con `#S`, `#E`, re-medida y acta | **En curso, con estado sin confirmar**: ver "DÓNDE QUEDAMOS". Se cierra en el ciclo, pasos 5-7 |
| **P9 — Validación del arquitecto** | Firmware 3.6.x y APK: aprobado, o aprobado con condiciones resueltas | Hecha r1/r2 sobre firmware 3.6.2 y app 3.6.6: (a) la 3.6.2 se mantiene; (b) escribir, con P9-B1 a B13. **Las apps 3.6.7-3.6.9 sin revisar** (ciclo, paso 2) |
| **P10 — Propuesta de app de producción** | Propuesta escrita, revisando también la app de campo (`RetroVerticalP1`, V5) y `ROADMAP-MEJORAS-App.md` | Estudio de tecnología hecho (`d295547`). Propuesta, tras P9 |
| **P11 — APK de producción** | Atada a un commit, md5, tests en verde, probada en SLV-002 | Tras P10 |
| **P12 — Informe y registros** | PDF de ajuste y verificación y registros para la interventoría con serie, fecha y vencimiento = calibración + 1 año | SPEC de registros escrita (`7111024`, decisiones PA-01, 07 y 08 en `2069ab5`). Informe PDF, pendiente |

## Segundo equipo V3

Tramo propio, **después de cerrar P8 en SLV-002** y con la app que salga del ciclo. Mismo firmware
(3.6.2) y misma app; **campaña, ajuste, acta y fecha propios** (L-23). Se sigue
[`RUNBOOK.md`](RUNBOOK.md) de la fase 0 a la 11.

| Paso | Qué | Estado |
| :--- | :--- | :--- |
| Identificación | Serie, cliente, micro, placa, pantalla, nombre Bluetooth y MAC; acta en `06_Calibracion/<serie>/` | Sin empezar: equipo no identificado |
| Línea base y lectura ICSP | Antes de conectar el PICkit, por pantalla y por Bluetooth. **Desconectar el PICkit antes de medir** | Sin empezar |
| Autorización | Visto bueno del propietario de **ese** equipo | Sin pedir |
| Grabación y G4 | `.hex` 3.6.2, atado a su commit; `#SN` con la serie **una sola vez**, verificada con `#GN#` | Sin empezar |
| Banco completo, calibración y acta | Como SLV-002 en el ciclo, pasos 5-7: 133 patrones más A5 y OSCURO, un ZIP | Sin empezar |

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir | Por qué ahora |
| :--- | :--- | :--- |
| D-1 | Qué se hace con lo escrito en SLV-002 después de las 12:04: se reconstruye su registro o se reescribe en el ciclo | No hay ZIP que lo confirme y el acta no está aceptada |
| D-2 | Criterio de reproducibilidad con la A5 NO CONCLUYENTE (s_rep 2,24 %, P22 −5,6 %): umbral y número de colocaciones K × M | Sustituye a RF-CAL-13 y entra en la SPEC (paso 1) |
| D-3 | Qué códigos intensos 3-6 se ajustan con el P1-P132 y con qué grado | Hasta hoy sólo se verificaban |
| D-4 | Café y lila: dentro del ajuste del rojo o sólo verificar (recomendación: sólo verificar) | El catálogo los trae |
| D-5 | P32a/P32b: identidad del P32 duplicado en la fuente | Sigue sin confirmar |
| D-6 | Ancla del código 2: la serie OSCURO (x = 565,4) o la observación única de la propuesta (x ≈ 575) | La propuesta se calculó con 575 |
| D-7 | Adoptar el flujo de la QA: modo banco más "Calibrar este equipo" | Condiciona la SPEC (paso 1) |
| D-8 | Clave de firma propia del APK, para instalar siempre encima y no desinstalar (QA §3.3) | Una desinstalación borra la campaña |
| D-9 | Umbral de la interventoría (C-01: SFT 70 %, AT4 y AT2 80 %, Manual 100 %) | Bloquea el indicador E11 |
| D-10 | Cuándo subir `targetSdk` (hoy 30) a 35/36 | El estudio lo liga a publicar en Play; ver contradicción abajo |

## Contradicciones abiertas (no se eligen: se cierran midiendo o con registro)

- **Serie de SLV-002:** el registro de tramas deja `SLV-02` a las 12:09:02 (QA §1); según Diego se
  corrigió a `SLV-002` después. Se cierra con `#GN#` en el paso 5.
- **Códigos 8 y 2 y re-medidas de las 12:25:** dichos por Diego, sin ZIP ni tramas. Se cierra con `#G`
  de los 12 códigos en el paso 5.
- **Café y lila:** el commit `bba4dbe` dice "sin código en el firmware"; Diego dice que se miden con el
  código del rojo.
- **`targetSdk`:** el encargo del 19-sep lo llama urgente; el estudio (`ESTUDIO-…:111`) dice que sólo
  obliga si se publica en Play.
- **"Código 1 verificado":** comprobado con `#E` (relectura conforme), pero la QA lo da por "escrito y
  sin verificar" porque falta la re-medida de patrones.

## Especificaciones

**Las SPEC son por versión de hardware** (Diego, 19-sep-2026): la V3.6 lleva las suyas; la V4.6 las
duplica y adapta en su repositorio.

## Después de la V3.6

- **Los dos V4:** repositorio `D:\IT\P_RetroVertical_V4.6` (remoto `dieleoz/retrov4.6_2026`), en
  espera. Lo aprendido hoy está en su `APRENDIDO-DE-V3.6.md` (L-24 en adelante).
- **Registro de medidas periódicas para la interventoría** (P12), dentro de la app de producción (P10).
