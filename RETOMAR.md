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
  x = 565,4), en `06_Calibracion/SLV-002/campanas/` con `HUELLAS.txt`. Hay un ZIP de las 12:10 sólo en
  `07 pruebas/` (sin versionar, sin huella).
- **Escrito en el equipo:** código 1 en grado 1 con `#S` y `#E` conformes a las 12:04, **sin re-medida
  registrada**. Según Diego, a las 12:25 también re-medidas y códigos 8 y 2: **sin ZIP que lo
  confirme**. Serie: el registro deja `SLV-02` (12:09); según Diego se corrigió a `SLV-002`. Acta
  **sin aceptar**; fecha de calibración sin escribir.
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
   solo ZIP. Al empezar, `#GN#` y `#G` de los 12 códigos para saber qué quedó escrito hoy.
6. **Calibrar con un botón.**
7. **Acta y fecha** (`#SC` sólo con el acta aceptada). Cierra P8.

Después: P10-P12 (app de producción, informe PDF y registros), el segundo equipo V3 con `RUNBOOK.md`
y los dos V4 en `D:\IT\P_RetroVertical_V4.6`.

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Lee D:\IT\P_RetroVertical_V3.6\RETOMAR.md y
ROADMAP.md ("DÓNDE QUEDAMOS", "AHORA" y "Decisiones pendientes"). SLV-002 lleva el firmware 3.6.2
(hex 9d5d5e39, grabado el 19-sep 10:37). App vigente 3.6.9 (f52eeb1, md5 3fbb68f3), no apta sin
soporte según la QA. Código 1 escrito y comprobado con #E a las 12:04, sin re-medida registrada; los
códigos 8 y 2 y la corrección de la serie a SLV-002 los dice Diego, sin ZIP: no los des por hechos.
Acta sin aceptar. Estamos en el ciclo 1-7: SPEC+TDD, arquitecto, app, QA, banco completo de 133
patrones + A5 + OSCURO, calibrar con un botón, acta y fecha. Compruébalo todo con git log antes de
citarlo. Tú orquestas con subagentes; los repos V3.6 y V4.6 son independientes del V5, y la
calibración es por equipo, nunca compartida.
```
