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
  - **Certificado para el cliente emitido** (CERT-TITULO-2, cierra P-CAL-02 y deroga RF-CAL-30):
    `actas/CERTIFICADO-Calibracion-SLV-002-20260922.pdf` (md5 `fb5f7ad6…`) y `.docx` (md5 `fafd57fc…`),
    formato ITVIAL, con la declaración de alcance bajo el título. **Se entrega junto con
    `soporte_SLV-002_20260922_144415.zip`**, que lleva las actas, el diario y las tramas. Sin rellenar:
    ciudad, cédula y cargo del firmante, y datos del cliente.
  - #ERR,FORMATO: **es transitorio y la causa sigue sin identificar.** La misma trama de 66 B, con los mismos
    coeficientes, fue rechazada 3 veces (11:41, 13:35 y 13:55) y aceptada a las 14:32; el 19-sep otras dos
    iguales entraron a la primera (`campanas/HUELLAS.txt`). Descartadas la notación, el signo y la longitud;
    el desbordamiento del anillo RX de 64 B (`uart_module.c:57`) es hipótesis **sin medir**, no la conclusión.
- **App de calibrar en rama `rtv-1.0-simple` (`6d307a2`), sin entregar:** pacing UART en bloques ≤24 B con 30 ms
  en `EnlaceSerie`, reintento con trama corta de 58 B (`tramaSCorta`) y diálogo T-C41 con cuenta atrás.
  - **Dos defectos antes de tocarla:** sale de `31e6214` (la 3.6.8 que QA dejó NO APTO) y repite su mismo par
    `versionCode 10013` / `Cov_3.6.8_calibrar` (CLAUDE.md §6). Sin arquitecto ni QA; el "414/414" está sin
    comprobar. Ya no bloquea a nadie: el equipo está calibrado.
- **Usuario 0.3.7** (`418991f`, rama `retro-usuario`): **arquitecto APTO y QA APTO** (152/152).
  Etiqueta visible `"Retro Coviandina"`, entregable `RETRO-COVIANDINA-usuario-0.3.7-10.apk`, misma firma
  (`c990adf6...`) para actualización directa sin desinstalar.
  Firma verificada sobre el binario: el certificado es el mismo que el de la APK instalada hoy.
- **Azul y verde sin calibrar (C-08 del `ROADMAP.md`).** El equipo repite (9 patrones, ±2 cuentas) y en
  amarillo y rojo correlaciona a +0,98 y +0,96, pero en azul y verde la correlación es **negativa** en el
  rango de trabajo (−0,68 y −0,42). No hay curva posible mientras el catálogo diga lo que dice.
- **A5a:** lista de 46 patrones aprobada (TOMA-SEL-LISTA). A5b siguiente.

## Pendiente de Diego / Siguiente

1. **Probar la 0.3.7 en dos teléfonos** (Android ≤9 y ≥10), B7, y despacharla a Julio con la instrucción
   de instalar encima **sin desinstalar**.
2. **Rellenar y firmar el certificado** y mandarlo con el ZIP de soporte.
3. **Fotos de las etiquetas de P112 y P124** y de dónde salen sus valores: cierran C-08 y deciden si el
   azul y el verde se pueden calibrar.
4. **Clave de firma estable:** hoy se firma con la de depuración. Propuesta hecha y sin crear, a la
   espera de tu sí; si esa clave cambia de máquina, habría que desinstalar y se pierde la campaña.
5. **Las cuatro propuestas abiertas de `SPEC-App-Usuario-V3.6.md` §5** (GPS a 15 m, separador y decimal
   del CSV, nombre del ZIP con varios equipos, cuarentena con reintentos): bloquean el incremento 2.
6. **`campanas/salida/` sigue sin versionar:** el pre-commit rechaza su informe por tres líneas de más de
   120 caracteres, y es un fichero generado. O se arregla el generador o se deja fuera.
7. A5b (toma corta y certificado en la app) y A4c (defectos de campo), tras el límite del 25-sep.

## Prompt para retomar

```
Retomamos la V3.6 (D:\IT\P_RetroVertical_V3.6, github.com/dieleoz/retrov3.6_2026). Lee CLAUDE.md, RETOMAR.md y
ROADMAP.md. SLV-002 quedó calibrado (A4 cerrado, máscara 0283, actas aceptadas y PDF emitido). Siguiente:
la APK de cliente (RETRO-COVIANDINA-usuario-0.3.7-10.apk) esta lista y con QA APTO. Siguiente: B7 en dos
telefonos, certificado firmado al cliente con su ZIP, y las etiquetas de P112 y P124 para cerrar C-08.
```
