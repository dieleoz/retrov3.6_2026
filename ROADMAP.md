# ROADMAP — V3.6: qué se hace y en qué orden

**Actualizado:** 19-sep-2026, ~17:15 (hora de Diego; ver "Contradicciones abiertas" sobre el reloj).
Lo que no está aquí no está en ejecución. **SLV-002 está calibrado sólo en los códigos 1 y 2**, con
acta aceptada a las 12:23:26 y vencimiento el 2027-09-19. **El banco de patrones está a medias** (15:10:
tipo I completo, blanco 5/16, amarillo 1/24) y los códigos 8, b y 5 siguen con la curva de fábrica.
**No hay APK entregable hoy:** la 3.6.15 no se entregó y la 3.6.16 está en desarrollo.
Procedimiento paso a paso: [`RUNBOOK.md`](RUNBOOK.md). Reglas permanentes del repositorio:
[`CLAUDE.md`](CLAUDE.md), escrito el 19-sep-2026 por la noche. **Hasta entonces no existía**, y toda
sesión abierta aquí trabajaba con las reglas globales de la línea V5, que es otro equipo y otra línea.

## Objetivo

Dejar los equipos V3 con firmware **V3.6**: la calibración pasa a EEPROM y se ajusta **desde la app**,
sin volver a reprogramar, con serie y fecha de calibración guardadas en el propio equipo. La pantalla
STONE y la app del cliente siguen funcionando igual. Después, informe de ajuste y registros para la
interventoría.

**Por equipo, nunca por modelo (L-23).** Campaña, ajuste, coeficientes, fecha y acta son de **cada
equipo físico**. Entre equipos se reutilizan el catálogo de patrones, el método, el firmware y la app.

## DÓNDE QUEDAMOS — 19-sep-2026, ~17:15

### SLV-002 (Coviandina, Concesionaria Vial Andina; MAC 00:21:13:05:19:3B)

| Qué | Estado | Evidencia |
| :--- | :--- | :--- |
| Firmware | **3.6.2** grabado a las 10:37 | `.hex` md5 `9d5d5e39…`, commit `6a32ca3`; grabación `78924ae` |
| Códigos 1 y 2 | **Calibrados**, acta ACEPTADA a las 12:23:26, `#SC` 2026-09-19, vence 2027-09-19 | ZIP de las 12:27, md5 `ce1f35fc…`, `afdd700` |
| Persistencia en campo | **`#V#` = CAL máscara 0003 y `#GC` 2026-09-19 a las 15:10**: primera confirmación en campo de que los códigos 1 y 2 persisten (T-C41) | `campanas/HUELLAS.txt`, ZIP `…_151045.zip` md5 `f04fc014…`, `8c8c7de` |
| Banco de patrones | **Parcial a las 15:10**, con la app 3.6.11: **tipo I 20/20, blanco 5/16, amarillo 1/24**; el resto, pendiente | Mismo ZIP de las 15:10 |
| Códigos 8, b y 5 | **Sin escribir**: curva de fábrica | — |
| Códigos 3, 4 y 6 | Sin escribir; se ajustan sólo si el banco lo permite | — |
| Serie en EEPROM | Hoy **`SLV-002`**. Decidida **`SLV-002-2026`** (SERIE-2), **aún no grabada**: se grabará con la 3.6.16 | `DECISIONES-Diego-2026-09-19.md`, fila SERIE-2 |
| Nombre Bluetooth | `COVIANDINA_SLV-002`. **No se cambia** | Decisión de Diego |

### Apps

| Versión | Revisión de arquitectura | QA | Estado |
| :--- | :--- | :--- | :--- |
| 3.6.13 (`5b5bd54`) | **P12**: APTO CON CONDICIONES para 8, b y 5 tras el banco (`c20a30c`) | `QA-App-3.6.13.md`: apta con avisos (`d15a27d`) | Superada |
| 3.6.14 (`00f667e`) | **P13**: apta para banco y el 8; el b no; el 5 tras P81 (`b85ce4f`) | `QA-App-3.6.14.md`: apta con avisos (`9c970d3`) | Superada |
| 3.6.15 (`c7ef3c6`) | **P14**: APTO CON CONDICIONES (`0c319d9`) | `QA-App-3.6.15.md`: apta con avisos y **tres defectos Altos**, QA-3615-01 a 03 (`a5c47b0`) | **No entregada**, por esos Altos y las condiciones de P14 |
| **3.6.16** | Pendiente | Pendiente | **En desarrollo** (otro agente, `03_App_Movil/`) |

La última APK con la que Diego ha medido es la **3.6.11** (banco de las 15:10, según `HUELLAS.txt`).

**Qué trae la 3.6.16** (en desarrollo, sin commit a esta hora):

- actas visibles por código;
- renombrado de serie robusto (QA-3615-01 y 02: corte entre `#SN` y `RENOMBRA`, y otro teléfono);
- banco que salta lo ya medido y los grupos equivalentes (y la importación con su cola, QA-3615-03);
- controles (oscuro, A5) sólo al inicio y al final de la sesión;
- protocolo **1 × 4 por defecto** (PROTOCOLO-MIN) y **"preciso" 5 × 4** en los ajustes, P81 y las
  re-medidas del 8, el b y el 5 (PROTOCOLO-AJUSTE), en los ajustes de la app.

**Regla:** a Diego sólo se le entrega una APK con el visto bueno escrito **del arquitecto y de QA**.

### Decisiones de Diego del día

Todas en [`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`](06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md):

| ID | Qué dice, en corto |
| :--- | :--- |
| C | Códigos 1 y 2: compromiso entre XI e IV/IX |
| B1 | Código 1: grado 1 aceptado pese a RF-CAL-14/15/16 |
| B2 | Código 2: recta anclada en oscuro |
| A5 | El criterio de reproducibilidad sale de la A5 |
| PA-24 (ampliada y con techo) | Código b con recta anclada en oscuro; la dispensa cubre RF-CAL-15; P39 +15 % y P49 −10 % son techo, con ±3 puntos |
| PA-14 | Código 5 con recta anclada en oscuro y la regla de cobertura relajada |
| REMEDIDA-b | La re-medida del b se juzga con RF-CAL-18 (frente a la curva escrita) |
| P81 | Fuera del ajuste del 5; sólo verificación y re-medida |
| SERIE-2 | Coviandina pasa a SLV-002-2026; Nordeste, a SLV-003-2026 |
| PROTOCOLO-MIN | 1 colocación × 4 disparos por defecto; A5 y OSCURO a K = 5 |
| PROTOCOLO-AJUSTE | Ajuste de 8, b y 5, P81 y re-medidas en "preciso", 5 × 4 |
| RF-CAL-15-b | RMS de tipo I del b: 11,5 % estricto, sin el ±3 |
| TIPO-I-REPETIR | P34, P37, P43, P44, P38, P39 y P49 se repiten en "preciso"; el resto del tipo I de las 15:10 vale |

### Documentos nuevos del día

| Documento | Qué es |
| :--- | :--- |
| [`06_Calibracion/PLAN-Banco-Representativo.md`](06_Calibracion/PLAN-Banco-Representativo.md) | Banco completo (133), representativo (58, ~105 min) y verificación anual (12). En papel |
| [`06_Calibracion/SLV-002/COTEJO-Formulas-Historicas.md`](06_Calibracion/SLV-002/COTEJO-Formulas-Historicas.md) | Curvas de hoy frente a las históricas del Vertical |
| [`06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md`](06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md) | Fábrica, acta y propuesta simuladas por código |
| [`05_Documentacion/PROPUESTA-Diagnostico-y-Logs-RTV.md`](05_Documentacion/PROPUESTA-Diagnostico-y-Logs-RTV.md) | Diagnóstico y registros de la app. Propuesta sin construir |
| [`05_Documentacion/SPEC-App-Unica-V36-V46.md`](05_Documentacion/SPEC-App-Unica-V36-V46.md) | Una sola app RTV para V3.6 y V4.6 (RF-APP-U01 a U14; pruebas en `TDD-V3.6.md` §7) |
| [`05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md`](05_Documentacion/ESTUDIO-Tecnologia-App-Produccion.md) | Seguir en Android nativo; `targetSdk` (hoy 30) |
| [`05_Documentacion/SPEC-Registro-Indicador-Interventoria.md`](05_Documentacion/SPEC-Registro-Indicador-Interventoria.md) | Registro de medidas periódicas e indicador E11 |
| [`08_Senales/CATALOGO-Senales-Manual-2024.md`](08_Senales/CATALOGO-Senales-Manual-2024.md) | 376 señales verticales del Manual 2024 |

## AHORA — en este orden

Ningún paso empieza sin cerrar el anterior. Mientras tanto **no se escribe nada en SLV-002**.

| # | Paso | Quién | Sale | Estado |
| :---: | :--- | :--- | :--- | :--- |
| 1 | **App 3.6.16** | Subagente de desarrollo | APK atado a un commit, md5, tests en verde | **En curso** |
| 2 | **Arquitecto y QA** de la 3.6.16 | Subagentes, modelo capaz | Veredictos escritos; cierre de QA-3615-01 a 03 y de las condiciones de P14 | Tras 1 |
| 3 | **Entrega a Diego** | Principal | APK con los dos vistos buenos | Tras 2 |
| 4 | **Grabar la serie `SLV-002-2026`** con la 3.6.16 ("Cambiar serie": `#SN`, `#GN`, `RENOMBRA`) | Diego | Tramas en el ZIP | Tras 3 |
| 5 | **Diego mide lo que queda del banco**: 8, b y 5 (y 3, 4, 6 si procede); repite en "preciso" los siete tipo I de TIPO-I-REPETIR; controles al inicio y al final | Diego | ZIP en `06_Calibracion/SLV-002/campanas/` con su huella | Tras 4 |
| 6 | **"Calibrar todo"**: 8 → b → 5 (y 3, 4, 6 si procede), un acta aceptada por código; re-medidas en "preciso" | Diego con la app | Códigos escritos, relectura, `#E` y re-medida | Tras 5 |
| 7 | **Actas** aceptadas; `#SC` sólo tras aceptar | Diego | Actas en `06_Calibracion/SLV-002/` | Tras 6. Cierra P8 |
| 8 | **ZIP de soporte** (completo, con tramas) al repositorio con su huella | Diego y principal | ZIP + `HUELLAS.txt` | Tras 7 |

Los códigos 1 y 2 no se reescriben (la app no lo permite: `NO_REESCRIBIR`). Se revisan con el banco,
y un acta nueva sólo sustituye a la de las 12:23 si Diego la acepta.

## Puertas P1-P12

Las puertas son del proceso; las **revisiones de arquitectura** P9 a P14 llevan otra numeración (ver
"Contradicciones abiertas").

| Puerta | Qué tiene que cumplirse | Estado a las ~17:15 |
| :--- | :--- | :--- |
| **P1 — Especificación** | `SPEC-V3.6` con RF de firmware y app, paridad y pruebas | Cerrada en r1.2; ampliada en el día con el flujo "Calibrar este equipo" y la SPEC de app única |
| **P2 — Arquitectura** | Revisión adversaria de SPEC, PROTOCOLO y código | **Cerrada**, 18-sep (aprobado con condiciones) |
| **P3 — Compilación reproducible** | XC8 2.10 compila la base 2020 idéntica a `d089f962` | **Cerrada**, 18-sep |
| **P4 — Firmware** | `.hex` atado a un commit; fábrica = ecuaciones de 2020 (T-A20) | **Cerrada la 3.6.2** (`6a32ca3`) |
| **P5 — App** | APK con tests en verde y modo de pruebas | **Abierta**: 3.6.15 no entregada; 3.6.16 en curso |
| **P6 — Autorización** | Visto bueno del propietario a reprogramar | **Cerrada**, 18-sep |
| **P7 — Grabación** | Verificación tras grabar | **Cerrada para la 3.6.2** (10:37, confirmada a las 11:20) |
| **P8 — Calibración de SLV-002** | Campaña, ajuste, `#S`, `#E`, re-medida y acta | **Cerrada para 1 y 2** (acta 12:23:26; persistencia vista a las 15:10). **Abierta** para 8, b y 5 (y 3, 4, 6) |
| **P9 — Validación del arquitecto** | Firmware y APK aprobados, o con condiciones resueltas | Firmware 3.6.2: se mantiene (P9 r2, P10). APK: revisiones P10 a P14 hechas; **la 3.6.16, sin revisar** |
| **P10 — Propuesta de app de producción** | Propuesta escrita | Estudio de tecnología hecho. Propuesta, tras cerrar P8 |
| **P11 — APK de producción** | Atada a un commit, md5, tests, probada en SLV-002 | Tras P10 (P9-P9: no antes de cerrar P8) |
| **P12 — Informe y registros** | PDF de ajuste y verificación; registros para la interventoría | SPEC de registros escrita. Informe PDF, pendiente |

### Revisiones de arquitectura de la app

| Revisión | Sobre | Veredicto | Documento en `05_Documentacion/` |
| :--- | :--- | :--- | :--- |
| P9 r1/r2 | Firmware 3.6.2 y app 3.6.6 | 3.6.2 se mantiene con condiciones | `REVISION-Arquitectura-P9-V3.6.md` |
| P10 | App 3.6.10 | Aprobada con condiciones; firmware sin cambios | `REVISION-Arquitectura-P10-V3.6.md` |
| P11 | App 3.6.12 | NO APTA; siete arreglos y cuatro condiciones | `REVISION-Arquitectura-P11-V3.6.md` |
| P12 | App 3.6.13 | APTO CON CONDICIONES (8, b y 5 tras el banco) | `REVISION-Arquitectura-P12-V3.6.md` |
| P13 | App 3.6.14 | Apta para banco y el 8; el b no; el 5 tras P81 | `REVISION-Arquitectura-P13-V3.6.md` |
| P14 | App 3.6.15 | APTO CON CONDICIONES | `REVISION-Arquitectura-P14-V3.6.md` |

Las QA de cada versión están en `05_Documentacion/QA-App-3.6.*.md` (3.6.10, 12, 13, 14 y 15).

## Lo que sigue, cerrado P8

1. **Informe PDF y registros** para la interventoría (P12), con serie `SLV-002-2026`, fecha y
   vencimiento = calibración + 1 año.
2. **Propuesta y APK de producción** (P10, P11), sobre la app única RTV (`SPEC-App-Unica-V36-V46.md`).
3. **Banco representativo** (58 patrones) para recalibraciones, y verificación anual (12), una vez
   caracterizado el catálogo con el banco completo.
4. **La V4.6** en `D:\IT\P_RetroVertical_V4.6` (remoto `dieleoz/retrov4.6_2026`): el segundo equipo
   resultó un V4 (placa itvial v4.0 con la PPS de la V4.1, `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md`),
   serie **SLV-003-2026** (Autopistas del Nordeste). La rama A (grabarle la V3.6) queda descartada. Usa
   la misma app; su ROADMAP manda sobre ese equipo.

## Decisiones pendientes (Diego)

| ID | Qué hay que decidir | Por qué ahora |
| :--- | :--- | :--- |
| D-3 | Qué códigos intensos 3, 4 y 6 se ajustan con el banco y con qué grado | "Calibrar todo" los ofrece "si procede" |
| D-4 | Café y lila: dentro del ajuste del rojo o sólo verificar (recomendación: sólo verificar) | El catálogo los trae |
| D-5 | P32a/P32b: identidad del P32 duplicado | Sin confirmar en la fuente |
| D-8 | Clave de firma propia del APK | Una desinstalación borra la campaña |
| D-9 | Umbral de la interventoría (C-01: SFT 70 %, AT4 y AT2 80 %, Manual 100 %) | Bloquea el indicador E11 |
| D-10 | Cuándo subir `targetSdk` (hoy 30) | Ver contradicción abajo |

Resueltas en el día y fuera de esta tabla: D-1, D-2 (A5), D-6, D-7 (flujo de un botón) y todas las del
registro de decisiones.

## Contradicciones abiertas

No se eligen: se cierran midiendo o con registro.

- **Reloj.** El registro de decisiones fecha SERIE (~16:10), SERIE-2 (~16:15), PROTOCOLO-MIN (~16:40)
  y PROTOCOLO-AJUSTE (~17:10), pero sus commits son de las 15:03, 15:04, 15:11 y 15:40 (`git log`,
  zona −05:00), y este documento se escribe con el reloj de la máquina en ~15:45. La hora de Diego y la
  de la máquina difieren; no se sabe cuál es la buena.
- **3.6.15.** P14 la da por APTO CON CONDICIONES y la QA por apta con avisos; la decisión de no
  entregarla no está registrada en el repositorio, sólo en el encargo del coordinador.
- **Numeración.** "P10-P14" nombra a la vez puertas del proceso y revisiones de arquitectura.
- **`targetSdk`:** el encargo del 19-sep lo llama urgente; el estudio (`ESTUDIO-…:111`) dice que sólo
  obliga si se publica en Play.
- **Serie, en la V4.6:** `V4.6:06_Calibracion/EQ-v40-itvial/CERTIFICADO-ANTERIOR.md` dice que Coviandina
  "conserva `SLV-002`"; SERIE-2 lo sustituye por `SLV-002-2026`.

**Cerradas el 19-sep-2026** (se deja la errónea junto a la buena):

| Se dijo | Quedó | Cómo se cerró |
| :--- | :--- | :--- |
| Serie `SLV-02` en EEPROM (12:09:02) | `#GN` = **SLV-002** | ZIP de las 12:27 (`afdd700`) |
| Códigos 8 y 2 escritos hacia las 12:25 | **1 y 2 escritos; el 8 no** | ZIP de las 12:27 |
| `CAL,0003` "sale del fuente, no de una medida" (QA-3613 R-1) | `#V#` = CAL 0003 **visto en campo** a las 15:10 | ZIP de las 15:10 (`8c8c7de`) |
| Serie SLV-002 compartida con el equipo de Nordeste | Coviandina SLV-002-2026; Nordeste SLV-003-2026 | SERIE y SERIE-2 |
| El segundo equipo es un V3 y recibe la V3.6 | Es un V4 (PPS de la V4.1): va a la V4.6 | `IDENTIFICACION.md` |
| Café y lila "sin código en el firmware" (`bba4dbe`) | Se miden con el código del rojo | Decisión de Diego |

## Especificaciones

**Las SPEC son por versión de hardware** (Diego, 19-sep-2026): la V3.6 lleva las suyas; la V4.6 las
duplica y adapta. La excepción es la app: **una sola app** para las dos líneas
(`SPEC-App-Unica-V36-V46.md`).
