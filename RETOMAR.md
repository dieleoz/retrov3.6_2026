# RETOMAR — dónde se quedó el trabajo

**Puesto al día:** 19-sep-2026, ~12:30. Repositorio `D:\IT\P_RetroVertical_V3.6` (remoto
`github.com/dieleoz/retrov3.6_2026`). Detalle en `ROADMAP.md` → "DÓNDE QUEDAMOS" y "AHORA";
procedimiento en `RUNBOOK.md`.

## Estado

- **SLV-002 (Concesionaria Vial Andina)** lleva el firmware **3.6.2** (`.hex` md5 `9d5d5e39…`, commit
  `6a32ca3`), grabado a las 10:37 (`78924ae`) y confirmado por la app a las 11:20. La revisión P9 r2 lo
  mantiene con condiciones.
- **App vigente: 3.6.9** (`f52eeb1`, APK md5 `3fbb68f3…`). La QA ISTQB (`f45d2a8`) la da por **no
  apta para un operador sin soporte** (17 defectos) y propone modo banco más "Calibrar este equipo".
  Las apps 3.6.7-3.6.9 no las ha revisado el arquitecto.
- **Campañas:** 10:33 (50 patrones) y 12:00 (A5 NO CONCLUYENTE, s_rep 2,24 %, P22 −5,6 %; OSCURO
  x = 565,4), 12:10 (estado intermedio) y 12:27 (acta aceptada), en `06_Calibracion/SLV-002/campanas/`
  con `HUELLAS.txt`.
- **SLV-002 calibrado en los códigos 1 y 2** (ZIP de las 12:27, md5 `ce1f35fc…`, `afdd700`). Acta
  ACEPTADA a las 12:23:26. Código 1 en grado 1 y código 2 en recta anclada en oscuro, los dos
  verificados con `#E` y con re-medida CONFORME (P28 y P5). **El código 8 no se escribió.** `#GN` =
  `SLV-002`. `#SC` 2026-09-19, que vence el 2027-09-19. Defecto de la app: la cabecera "DEF 0000 / No
  calibrado" del acta sale de un `#V#` de las 12:04.
- **Catálogo P1-P132** (`bba4dbe`): los intensos 3-6 pasan a ser ajustables; café y lila se miden
  con el código del rojo (ajuste o sólo verificación, abierto; recomendación: sólo verificar). La app
  3.6.9 aún carga el catálogo P1-P31.

## Lo siguiente — el ciclo acordado con Diego

1. **SPEC + TDD del flujo nuevo** (en curso, otro agente: no tocar `SPEC-V3.6.md`,
   `SPEC-Calibracion-V3.6.md`, `TDD-V3.6.md`, la matriz ni el `RUNBOOK` mientras tanto).
2. **Arquitecto**, sobre el paso 1 y el delta de las apps 3.6.7-3.6.9.
3. **App** (y firmware, sólo si hace falta).
4. **QA ISTQB de la APK** y visto bueno.
5. **Diego mide el banco completo** en SLV-002 (133 patrones más A5 y OSCURO) con la app guiada, en un
   solo ZIP, con las curvas 1 y 2 ya escritas (`#G` de los 12 códigos al empezar).
6. **Calibrar con un botón** los códigos que el P1-P132 permita ajustar, y revisar el 1 y el 2.
7. **Acta y fecha** (`#SC` sólo con el acta aceptada). Cierra P8 para el resto de códigos.

Después: P10-P12 (app de producción, informe PDF y registros), el segundo equipo V3 con `RUNBOOK.md`
y los dos V4 en `D:\IT\P_RetroVertical_V4.6`.

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Lee D:\IT\P_RetroVertical_V3.6\RETOMAR.md y
ROADMAP.md ("DÓNDE QUEDAMOS", "AHORA" y "Decisiones pendientes"). SLV-002 lleva el firmware 3.6.2
(hex 9d5d5e39, grabado el 19-sep 10:37). App vigente 3.6.9 (f52eeb1, md5 3fbb68f3), no apta sin
soporte según la QA. SLV-002 calibrado sólo en los códigos 1 y 2: acta aceptada a las 12:23, ZIP de
las 12:27 (afdd700), serie SLV-002, vence el 2027-09-19. El código 8 no se escribió. Estamos en el ciclo 1-7: SPEC+TDD, arquitecto, app, QA, banco completo de 133
patrones + A5 + OSCURO, calibrar con un botón, acta y fecha. Compruébalo todo con git log antes de
citarlo. Tú orquestas con subagentes; los repos V3.6 y V4.6 son independientes del V5, y la
calibración es por equipo, nunca compartida.
```
