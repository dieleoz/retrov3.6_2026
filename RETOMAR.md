# RETOMAR — dónde se quedó el trabajo

**Puesto al día:** 19-sep-2026, ~17:15 (hora de Diego; el reloj de la máquina y los commits van ~1 h
30 min por detrás, ver `ROADMAP.md` → "Contradicciones abiertas"). Repositorio
`D:\IT\P_RetroVertical_V3.6` (remoto `github.com/dieleoz/retrov3.6_2026`). Detalle en `ROADMAP.md` →
"DÓNDE QUEDAMOS" y "AHORA"; procedimiento en `RUNBOOK.md`.

## Estado

- **SLV-002 (Coviandina)** lleva el firmware **3.6.2** (`.hex` md5 `9d5d5e39…`, `6a32ca3`), grabado a
  las 10:37 (`78924ae`).
- **Códigos 1 y 2 calibrados**, acta ACEPTADA a las 12:23:26 (ZIP de las 12:27, `afdd700`), vence el
  2027-09-19. **`#V#` = CAL 0003 confirmado en campo a las 15:10** (T-C41; `8c8c7de`).
- **Banco parcial de las 15:10** (app 3.6.11): tipo I 20/20, blanco 5/16, amarillo 1/24. Faltan los
  patrones de 8, b y 5 (y de 3, 4, 6 si procede) y repetir en "preciso" P34, P37, P43, P44, P38, P39 y
  P49 (TIPO-I-REPETIR).
- **Serie decidida `SLV-002-2026`** (SERIE-2), **sin grabar**: se hará con la 3.6.16. El nombre
  Bluetooth `COVIANDINA_SLV-002` no se cambia.
- **Apps:** 3.6.13, 3.6.14 y 3.6.15 revisadas (arquitecto P12, P13, P14; QA-App-3.6.13/14/15). **La
  3.6.15 no se entregó** (QA-3615-01 a 03, Altos, y condiciones de P14). **La 3.6.16 está en
  desarrollo** en `03_App_Movil/` por otro agente: actas visibles por código, renombrado de serie
  robusto, banco que salta lo medido y los grupos equivalentes, controles sólo al inicio y al final,
  1 × 4 por defecto y "preciso" 5 × 4 en ajustes y re-medidas del 8, el b y el 5.
- **Decisiones del día:** todas en `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`.
- **Segundo equipo:** es un V4 (Autopistas del Nordeste, **SLV-003-2026**); va a la V4.6.

## Lo siguiente

1. Terminar la **3.6.16**.
2. **Arquitecto y QA** sobre la 3.6.16. Sin los dos vistos buenos no se entrega.
3. Entregar a Diego; **grabar `SLV-002-2026`** con "Cambiar serie".
4. **Diego mide lo que queda del banco** (8, b, 5; 3, 4, 6 si procede; tipo I en "preciso").
5. **"Calibrar todo"**: 8 → b → 5, un acta aceptada por código; `#SC` sólo tras aceptar.
6. **ZIP de soporte** al repositorio con su huella en `HUELLAS.txt`.

Después: informe PDF y registros (P12), app de producción (P10, P11) y la V4.6.

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Lee D:\IT\P_RetroVertical_V3.6\RETOMAR.md y
ROADMAP.md ("DÓNDE QUEDAMOS", "AHORA", "Contradicciones abiertas") y
06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md. SLV-002 (Coviandina) lleva el firmware 3.6.2;
códigos 1 y 2 calibrados (acta 12:23, afdd700, vence 2027-09-19; CAL 0003 visto a las 15:10, 8c8c7de).
Banco parcial de las 15:10: tipo I 20/20, blanco 5/16, amarillo 1/24. Serie decidida SLV-002-2026,
sin grabar; el nombre Bluetooth no cambia. La 3.6.15 no se entregó; la 3.6.16 está en desarrollo en
03_App_Movil/. Orden: 3.6.16 -> arquitecto y QA -> entregar -> grabar serie -> Diego mide lo que queda
(8, b, 5) -> "Calibrar todo" -> actas -> ZIP de soporte. A Diego sólo se le entrega una APK con el visto
bueno del arquitecto y de QA. Compruébalo todo con git log antes de citarlo. Tú orquestas con
subagentes; los repos V3.6 y V4.6 son independientes del V5, y la calibración es por equipo.
```
