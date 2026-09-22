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
  - #ERR,FORMATO: **es transitorio y la causa sigue sin identificar.** La misma trama de 66 B, con los mismos
    coeficientes, fue rechazada 3 veces (11:41, 13:35 y 13:55) y aceptada a las 14:32; el 19-sep otras dos
    iguales entraron a la primera (`campanas/HUELLAS.txt`). Descartadas la notación, el signo y la longitud;
    el desbordamiento del anillo RX de 64 B (`uart_module.c:57`) es hipótesis **sin medir**, no la conclusión.
- **App de calibrar en rama `rtv-1.0-simple` (`6d307a2`), sin entregar:** pacing UART en bloques ≤24 B con 30 ms
  en `EnlaceSerie`, reintento con trama corta de 58 B (`tramaSCorta`) y diálogo T-C41 con cuenta atrás.
  - **Dos defectos antes de tocarla:** sale de `31e6214` (la 3.6.8 que QA dejó NO APTO) y repite su mismo par
    `versionCode 10013` / `Cov_3.6.8_calibrar` (CLAUDE.md §6). Sin arquitecto ni QA; el "414/414" está sin
    comprobar. Ya no bloquea a nadie: el equipo está calibrado.
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
