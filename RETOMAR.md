# RETOMAR — dónde se quedó el trabajo

**A4 CERRADO (22-sep): SLV-002 calibrado (códigos 1, 2, 8 y b; máscara 0283; fecha 2026-09-22). Actas aceptadas.**
Estado vigente; se reescribe en cada sesión. Reglas en `CLAUDE.md`; orden en `ROADMAP.md`; decisiones en
`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`. Mapa: `ARQUITECTURA.map` §M2 y §M1.

## Estado (22-sep-2026, tarde)

- **SLV-002 (Coviandina) — CALIBRADO Y ACEPTADO:**
  - Código 8 (#S,8) escrito a las 14:32 (333 ms); P43 medido 118,5 (-2,9 % vs 122 cert, conforme ±5 %).
  - Código b (#S,b) escrito a las 14:36 (309 ms); P49 medido 75,7 (-6,5 % vs 81 cert, conforme ±10 % RF-COV-12).
  - Fecha #SC,2026-09-22# grabada en EEPROM (vence 2027-09-22). Máscara final en #V#: 0283 (1, 2, 8 y b ajustados).
  - Actas de calibración aceptadas y archivadas en `06_Calibracion/SLV-002/actas/`:
    `acta_SLV-002_00211305193B_20260922_143520.txt` y `144317.txt`.
  - Acta formal ITVIAL con membrete y firmas emitida: `ACTA-Calibracion-SLV-002-20260922.pdf` y `.docx`.
  - Contradicción de #ERR,FORMATO cerrada: la misma trama de 66 B falló 3 veces y entró a las 14:32; era desborde
    transitorio del buffer UART circular (64 B) al recibir ráfagas continuas con el bucle del PIC ocupado.
- **App de calibrar en rama `rtv-1.0-simple` (`6d307a2`):**
  - `Cov_3.6.8_calibrar.apk` (versionCode 10013, 414/414 tests OK). Pacing UART en bloques ≤24 B con 30 ms de pausa
    en `EnlaceSerie`, reintento con trama corta de 58 B (`tramaSCorta`) y diálogo T-C41 guiado con cuenta atrás.
- **Usuario 0.3.7** (`b8a299d`, rama `retro-usuario`): arquitecto APTO; QA sin hacer.
- **A5a:** lista de 46 patrones aprobada (TOMA-SEL-LISTA). A5b siguiente.

## Pendiente de Diego / Siguiente

1. Probar la app de usuario 0.3.5 / 0.3.7 en dos teléfonos (Android ≤9 y ≥10).
2. Revisar la propuesta de toma corta (A5a / A5b) cuando se retome tras el límite semanal (25-sep).
3. Entregar el acta formal ITVIAL (PDF) a Coviandina / cliente.

## Prompt para retomar

```
Retomamos la V3.6 (D:\IT\P_RetroVertical_V3.6, github.com/dieleoz/retrov3.6_2026). Lee CLAUDE.md, RETOMAR.md y
ROADMAP.md. SLV-002 quedó calibrado (A4 cerrado, máscara 0283, actas aceptadas y PDF emitido). Siguiente:
probar app de usuario 0.3.7 y preparar A5b (toma corta / certificado en app de calibrar tras el 25-sep).
```
