# ROADMAP — V3.6: qué se hace y en qué orden

**Actualizado:** 19-sep-2026, 09:50. Lo que no está aquí no está en ejecución. SLV-002 lleva el
firmware **V3.6.1**, grabado a las 09:36 y **aún sin comprobar en el equipo**. **Ningún equipo está
calibrado todavía.** El procedimiento paso a paso está en [`RUNBOOK.md`](RUNBOOK.md).

## Objetivo

Dejar los **dos equipos V3** (SLV-002 y el segundo) con firmware **V3.6**: la calibración pasa a
EEPROM y se ajusta **desde la app, en modo administrador**, sin volver a reprogramar. La pantalla
STONE y la app del cliente siguen funcionando igual. Después, app de producción, informe de ajuste y
registros para la interventoría. Los dos V4, en su propio repositorio, cuando se cierre la V3.6.

**Por equipo, nunca por modelo (L-23).** Campaña, ajuste, coeficientes y acta son de **cada equipo
físico**. Entre equipos se reutilizan el catálogo de patrones, el método, el firmware y la app; los
coeficientes, nunca.

## DÓNDE QUEDAMOS — 19-sep-2026, 09:50

- **SLV-002 (Concesionaria Vial Andina):**
  - V3.6 grabada el 18-sep 20:24 (`.hex` `680b6a7d…`, commit `f75ff88`). **G4 cerrada el 19-sep**:
    `#V#` → `#V,3.6,2026-09-18,DEF,0000#`, `#E` 60/60 exacto, repetibilidad s = 4,0 cuentas
    ([acta](06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md)).
  - **Firmware V3.6.1 grabado a las 09:36** (`.hex` `8736c05d…`, commit `869d3c6`; registro
    `01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.1_2026-09-19.log`). IPE: *Program Succeeded*;
    el "Verify failed" posterior en ceros es la protección de código (L-14). **Sin comprobar en el
    equipo:** falta `#V#` con fecha `2026-09-19` y `#E` a fábrica.
- **Firmware 3.6.2 en curso:** añade `#FT#` (repone el factor de temperatura de fábrica; ya en
  `PROTOCOLO-V3.6.md` §3, sin commit a las 09:50). Se grabará con el PICkit, que sigue conectado.
- **App:** 3.6.3 (`d3025b2`, coherencia con deriva e INVÁLIDA); **3.6.4** (`090c84c`, APK md5
  `efb0386b…`): tipo I P32a-P50, disparo de asentamiento, `ULP_S` = 8, criterio de `#S` de la 3.6.1;
  **3.6.5** (`ff66f93`): campaña guiada por equipo (serie + MAC) con un solo ZIP, **sin probar en el
  equipo**. El APK del árbol a las 09:50 da md5 `f01531e7…`: no es el de la 3.6.4 y no está declarado
  en ningún commit (los APK no se versionan).
- **Campaña de SLV-002** (`07 pruebas/19092026_0900/`, **sin commit**):
  - medidos: los amarillos salvo P24; los blancos P1, P2, P3, P4, P6 y P7;
  - **faltan:** P27 y P28 (blancos IX, **imprescindibles**), P24 (dos series contradictorias: 2065 y
    2443), P30 ×9, los tipo I y la prueba de giro de los XI.
- **Hallazgos del día:** el primer disparo de cada serie sale bajo (17 de 17 series, L-16); los XI no
  siguen el orden de su certificado (L-22); P32 aparece duplicado en la lista de tipo I (la app lo
  carga como P32a azul y P32b naranja, **pendiente de confirmar**).

## Decisión de calibración (Diego, 19-sep-2026)

**Opción C: compromiso entre XI e IV/IX** para las curvas intensas de blanco (código 1) y amarillo
(código 2). Una sola curva no puede corregir a la vez el XI (+2-3 %) y el IV/IX (+25-50 %): se ajusta
por mínimos cuadrados con todos los patrones, sin ponderar por tipo, y el acta declara el error
residual de cada tipo. Verde, azul y rojo intensos (un solo nivel de patrón) sólo se comprueban. Con
tipo I, la app 3.6.4 permite ajustar además el 8 (amarillo opaco, hasta grado 2) y el b (rojo opaco,
grado 1).

## Puertas P1-P8: firmware, app y calibración de SLV-002

| Puerta | Qué tiene que cumplirse | Estado a las 09:50 |
| :--- | :--- | :--- |
| **P1 — Especificación** | `SPEC-V3.6` con RF de firmware y app, paridad y pruebas | **Cerrada** (r1.1, 18-sep). Reconciliación con el código en curso: SPEC, [`TDD-V3.6.md`](05_Documentacion/TDD-V3.6.md) y [`MATRIZ-SPEC-codigo-V3.6.md`](05_Documentacion/MATRIZ-SPEC-codigo-V3.6.md) (sin commit) |
| **P2 — Arquitectura** | Revisión adversaria de SPEC, PROTOCOLO y código | **Cerrada**: APROBADO CON CONDICIONES, 18-sep 19:48 (G1-G5 para grabar, C1-C5 para calibrar; `SPEC-V3.6.md` §6 bis) |
| **P3 — Compilación reproducible** | XC8 2.10 compila la base 2020 idéntica a `d089f962` | **Cerrada**, 18-sep: `d089f9625090a1213291c090eae7ac01` idéntico |
| **P4 — Firmware** | `.hex` atado a un commit; fábrica = ecuaciones de 2020 (T-A20) | **Cerrada** la V3.6 (`680b6a7d`) y la **V3.6.1** (`8736c05d`, commit `8860445`: límites de `#S` y `#ST`). **3.6.2 en curso** (`#FT#`) |
| **P5 — App** | APK con tests en verde y modo de pruebas | **Cerrada con la 3.6.4** (`090c84c`, `efb0386b`). 3.6.5 compilada, sin probar en equipo |
| **P6 — Autorización** | Visto bueno del propietario a reprogramar | **Cerrada** el 18-sep por Diego: acepta perder el original. La placa es de un cliente (Concesionaria Vial Andina) |
| **P7 — Grabación** | G1-G5 y verificación tras grabar | **Cerrada** para la V3.6 (G4 el 19-sep, `#E` 60/60). **V3.6.1 grabada sin comprobar**: repetir la G4 abreviada (`#V#`, `#GT#`, `#G`, `#E`) tras la próxima grabación |
| **P7-bis — Condiciones para calibrar** | C1-C5 de P2 | **C1:** T-A23 medida en simulador (hasta 7 ulp; `CAMBIOS-V3.6.md` §7.2); la app 3.6.4 tolera 8 y comprueba con `#E`. **C2, C3:** en la app. **C4:** cumplida en firmware 3.6.1 (T-A30 en simulador). **C5:** `#E` 60/60 hecho; T-C05 completa y T-C23 en equipo, pendientes |
| **P8 — Calibración de SLV-002** | Campaña, ajuste con la opción C, escritura con `#S`, `#E` y acta de antes y después | **En curso: campaña a medias** (ver "DÓNDE QUEDAMOS"). Ajuste, escritura y acta, pendientes |

## Puertas P9-P12: de la calibración a la producción

Tubería acordada con Diego, en este orden. Ninguna se abre sin la anterior.

| Puerta | Qué tiene que cumplirse | Estado a las 09:50 |
| :--- | :--- | :--- |
| **P9 — Validación del arquitecto** | Con SPEC y TDD reconciliados con el código (P1), el arquitecto valida **firmware 3.6.x y APK**: aprobado, o aprobado con condiciones resueltas | Espera a la reconciliación de SPEC/TDD |
| **P10 — Propuesta de app de producción** | Con el visto bueno de P9: propuesta escrita de la app de producción, **revisando también la app de campo existente** (`RetroVerticalP1`, repositorio V5, `CLAUDE.md` §6 del V5) y [`ROADMAP-MEJORAS-App.md`](ROADMAP-MEJORAS-App.md). Primero se especifica qué hace la de campo, después qué se mejora | Tras P9 |
| **P11 — APK de producción** | Un subagente construye la APK según P10; atada a un commit, con md5 y tests en verde; probada en SLV-002 | Tras P10 |
| **P12 — Informe y registros** | PDF de "informe de ajuste y verificación" (el título lo decide Diego) desde el modo superadministrador, y registros periódicos para la interventoría por vía y tipo de señal, con serie o MAC, fecha de calibración y **vencimiento = calibración + 1 año** | SPEC en curso (ver abajo) |

## Especificaciones

Regla de Diego (19-sep-2026): **las SPEC son por versión de hardware.** La V3.6 lleva las suyas; la
V4.6 las duplica y adapta en su repositorio. No hay SPEC compartidas.

| Documento | Estado |
| :--- | :--- |
| [`SPEC-V3.6.md`](05_Documentacion/SPEC-V3.6.md), [`TDD-V3.6.md`](05_Documentacion/TDD-V3.6.md), [`PROTOCOLO-V3.6.md`](05_Documentacion/PROTOCOLO-V3.6.md) (rev. 1.1) | Vigentes; reconciliación con el código en curso |
| `05_Documentacion/SPEC-Calibracion-V3.6.md` | **En curso** (no existe a las 09:50): modo superadministrador y PDF del informe |
| `05_Documentacion/SPEC-Registro-Indicador-Interventoria.md` | **En curso** (no existe a las 09:50): registros por vía y tipo de señal |
| `08_Senales/` | **En curso** (no existe a las 09:50): catálogo de señales del Manual 2024 |
| SPEC de la app de producción | Por escribir en P10 |

## AHORA, en este orden

1. **Firmware 3.6.2** (`#FT#`) y grabación en SLV-002 con el PICkit conectado. Después, desconectar,
   apagar y encender, y **G4 abreviada** con la app: `#V#`, `#GT#`, los 12 `#G`, `#E` en 5 puntos por
   código. Si la 3.6.2 se graba antes de comprobar la 3.6.1, se anota que la 3.6.1 no llegó a
   comprobarse en el equipo.
2. **Terminar la campaña de SLV-002** con la app 3.6.5 (modo guiado): P27 y P28 primero, P24 (tercera
   serie para decidir entre 2065 y 2443), P30 ×9, los tipo I, el giro de P5 a 0° y 90°. Un solo ZIP.
3. **Ajuste** de los códigos 1 y 2 con la opción C; 8 y b si los tipo I lo permiten; el resto sólo se
   comprueba. Diego acepta la propuesta antes de escribir.
4. **Escritura** con `#S`, relectura `#G`, `#E` contra la curva enviada, nueva medida de patrones con
   el código de cada color y **acta** en `06_Calibracion/SLV-002/`. Cierra P8.
5. En paralelo, sin equipo: SPEC/TDD reconciliados → **P9**.

## Segundo equipo V3

Tramo propio, **después de cerrar P8 en SLV-002**. Mismo firmware y misma app; **campaña, ajuste y
acta propios** (L-23). Se sigue [`RUNBOOK.md`](RUNBOOK.md) de la fase 0 a la 11.

| Paso | Qué | Estado |
| :--- | :--- | :--- |
| Identificación | Serie, cliente, micro, placa, pantalla, nombre Bluetooth y MAC; acta en `06_Calibracion/<serie>/` | Sin empezar: equipo no identificado |
| Línea base y lectura ICSP | Antes de conectar el PICkit, por pantalla y por Bluetooth | Sin empezar |
| Autorización | Visto bueno del propietario de **ese** equipo | Sin pedir |
| Grabación y G4 | El `.hex` vigente de la V3.6.x, atado a su commit | Sin empezar |
| Campaña, ajuste, escritura y acta | Como SLV-002, con su propia campaña | Sin empezar |

## Después de la V3.6

- **Los dos V4:** repositorio `D:\IT\P_RetroVertical_V4.6` (remoto `dieleoz/retrov4.6_2026`), en
  espera. Lo aprendido aquí pasa a su `APRENDIDO-DE-V3.6.md` y a su runbook.
