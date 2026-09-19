# RETOMAR — dónde se quedó el trabajo

**Puesto al día:** 19-sep-2026, 09:50. Repositorio `D:\IT\P_RetroVertical_V3.6` (remoto
`github.com/dieleoz/retrov3.6_2026`). Detalle en `ROADMAP.md` → "DÓNDE QUEDAMOS"; procedimiento en
`RUNBOOK.md`.

## Estado

- **SLV-002 (Concesionaria Vial Andina)** lleva el firmware **V3.6.1**, grabado a las 09:36 (`.hex`
  md5 `8736c05d…`, commit `869d3c6`, log en `01_Firmware/lecturas_equipos/SLV-002/`). **Aún no se ha
  comprobado en el equipo tras grabarlo.** La V3.6 anterior (18-sep) pasó G4 el 19-sep (`#E` 60/60).
  El firmware original se perdió (autorizado).
- **Firmware 3.6.2 pendiente:** añade `#FT#` (temperatura a fábrica). Se grabará con el PICkit, que
  sigue conectado.
- **App vigente: 3.6.4** (commit `090c84c`, APK md5 `efb0386b…`). La 3.6.5 (campaña guiada por equipo,
  un solo ZIP, `ff66f93`) no se ha probado en el equipo.
- **Calibración:** decisión C de Diego para los códigos 1 y 2. Ningún coeficiente escrito todavía.
- **Falta medir en SLV-002:** P27 y P28 (blancos IX, imprescindibles), P24 (series 2065 y 2443
  contradictorias), P30 ×9, los tipo I y el giro de los XI. Datos del 19-sep en
  `07 pruebas/19092026_0900/` (sin commit).
- **Documentos en curso** (otros agentes): SPEC/TDD reconciliados y matriz; `SPEC-Calibracion-V3.6.md`;
  `SPEC-Registro-Indicador-Interventoria.md`; catálogo `08_Senales/`.

## Lo siguiente

1. Grabar la 3.6.2, desconectar el PICkit, apagar y encender, y G4 abreviada (`#V#`, `#GT#`, `#G`,
   `#E`) con la app.
2. Terminar la campaña con la app 3.6.5 (modo guiado) y exportar un solo ZIP.
3. Ajuste con la opción C, escritura con `#S`, `#E` y acta (P8).
4. Tubería P9-P12: validación del arquitecto, propuesta de app de producción, APK de producción,
   informe PDF y registros para la interventoría.
5. Segundo equipo V3, con `RUNBOOK.md`.

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Lee D:\IT\P_RetroVertical_V3.6\RETOMAR.md,
ROADMAP.md ("DÓNDE QUEDAMOS") y RUNBOOK.md. SLV-002 lleva el firmware 3.6.1 (hex 8736c05d, grabado
el 19-sep 09:36) sin comprobar en el equipo; la 3.6.2 (#FT#) está pendiente de grabar. App vigente
3.6.4; la 3.6.5 (campaña guiada) sin probar. Falta medir P27, P28, P24, P30 x9, tipo I y giro de los
XI; después, ajuste con la opción C y acta. Compruébalo todo con git log antes de citarlo. Tú
orquestas con subagentes; los repos V3.6 y V4.6 son independientes del V5, y la calibración es por
equipo, nunca compartida.
```
