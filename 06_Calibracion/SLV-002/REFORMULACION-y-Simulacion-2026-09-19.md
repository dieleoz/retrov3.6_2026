# Reformulación de las curvas de SLV-002 y simulación antes / ahora / propuesta (19-sep-2026)

**Sin validar en hardware y sin escribir nada en el equipo.** Es un cálculo en papel sobre las medidas del 19-sep-2026. La reproducibilidad (s_rep ≈ 2,2 % por colocación) sale de **una** A5 de tres patrones, y P51-P132 **no están medidos**: su tabla (§5) es una **estimación**.

Generado por `06_Calibracion/SLV-002/tools/reformulacion_simulacion_slv002.py`, que llama al código de la app a través de `tools/ReformulacionSLV002.java`. No editar a mano: se regenera. La tabla completa está en `simulacion_antes_ahora_propuesta_SLV-002_20260919.csv`.

## 0. Resumen

| Código | Lámina | n | Fábrica: sesgo / RMS % | Acta de hoy: sesgo / RMS % | Propuesta: sesgo / RMS % |
| :---: | :--- | ---: | ---: | ---: | ---: |
| 1 | blanco intenso | 8 | +18,0 / 25,1 | -0,2 / 5,3 | -0,2 / 5,3 |
| 2 | amarillo intenso | 14 | +17,7 / 18,8 | -1,5 / 5,9 | -1,5 / 5,9 |
| 8 | amarillo tipo I | 4 | -17,2 / 19,4 | -17,2 / 19,4 | +0,7 / 5,7 |
| b | rojo tipo I | 3 | -70,9 / 70,9 | -70,9 / 70,9 | +3,2 / 10,8 |

Error = (respuesta float32 del firmware − certificado) / certificado, con la x combinada de todas las sesiones (§2.2). Los demás códigos no cambian en ningún estado (§4.2).

- **Códigos 1 y 2: no se reescriben.** Con las dos campañas y la A5, la curva nueva se separa de la del acta como mucho 5,3 unidades de R en el rango medido (código 1) y 2,5 (código 2): 0,89 y 0,55 veces la σ de colocación. No es significativo.
- **Código 8: escribir ya, con la recta anclada en el oscuro:** `c1 = 2.04279088E-01`, `c0 = -1.15490317E+02`. La de grado 1 sale casi igual, pero queda en el filo de `#S` (§3.3).
- **Código b: se puede escribir ya, también con la recta anclada:** `c1 = 3.13845140E-01`, `c0 = -1.77434093E+02`. Necesita la conformidad de Diego a RF-CAL-14. El banco P51-P132 no trae rojo tipo I: esperarlo no lo mejora.
- **Códigos 3, 4 y 6: esperan al banco**, que sí los cubre. **El código 5, ni con el banco**: el azul intenso sólo llega de R 83 a 102 y la regla de cobertura de la app lo impide.

## 1. Datos y trazabilidad

| Qué | Fuente | Uso |
| :--- | :--- | :--- |
| Campaña 10:33 | `campanas/campana_SLV-002_20260919_103300.zip`, md5 `4c50dbf63d53bd7f53cbd9856a0e5805` (= `HUELLAS.txt`) | Series **aceptadas** de P1-P50, 1 colocación × 9 disparos. P5 cuenta dos (0° y 90°). Las no aceptadas (S021 y S049, marcadas «repetida») no entran |
| Campaña 12:00 | `campana_SLV-002_20260919_120049.zip`, md5 `3e2c913bd88eb24cf922335d69c9aad9` | A5: S057 (P22), S058 (P28) y S059 (P4), 5 × 9. OSCURO: S060, 5 × 9. Sus S001-S056 son las de 10:33 sin cambios: el script lo comprueba serie a serie |
| Campaña 12:27 | `campana_SLV-002_20260919_122727.zip`, md5 `ce1f35fc64439cbb602d014b725fadfb` | `campana.csv` idéntico al de 12:00 (mismo md5). De `tramas/rtv36_20260919_114644.txt` salen las re-medidas conformes del acta |
| Sesión de la mañana | `03_App_Movil/RetroV36/app/src/test/resources/medidas_SLV-002_20260919_consolidado.csv` | 08:59-09:25, firmware 3.6.0. Primer disparo de cada serie descartado y el 3031 de P7 fuera. La serie de P24 de 09:04:43 (2445,5) queda fuera por identidad dudosa (`PROPUESTA-Ajuste-SLV-002-2026-09-19.md` §1.4) |
| Estado de hoy (acta) | `campanas/coeficientes_SLV-002_20260919_122047.csv` | Lectura `#G` de las 12 curvas después de escribir: los códigos 1 y 2 son nuevos, el resto es de fábrica |
| Curvas de fábrica | `Fabrica.java:62-88` (tabla de 2020 de `ecuacionesCalibracion.c`) | Estado «fábrica» |
| Catálogo | `06_Calibracion/patrones_certificados_P1-P132.csv` | 133 filas (P32 cuenta dos). Medidos: P1-P50. **El café y el lila se miden con el código del rojo**: el script les asigna el color rojo antes de `Asistente.deCodigo` |
| Código que calcula | App 3.6.9, commit `f52eeb1`, compilada con JDK 11 | `Asistente.proponer` (`Asistente.java:399`), `Ajuste.ajustar` (`Ajuste.java:41`), `Ajuste.anclada` (`Ajuste.java:99`), `criterioFirmwareS` (`Asistente.java:261`), `validarForma` (`:179`), `comprobarOscuro` (`:237`), `cobertura` (`:101`), `Ecuacion.respuestaFloat32` (`Ecuacion.java:56`) |
| Firmware | `calibracion_v36.c:229-233` (`aplicarEcuacion`); `:316-319` y `:404-420` (límites de `#S`); `ecuacionesCalibracion.c:49-54` (`arreglar_dato`) | Float32 en el orden de 2020, truncado a entero; más de 4000 da 0, y un negativo da la vuelta y también sale 0 (`calibracion_v36.c:309-313`) |

**El canal de cálculo reproduce el acta.** Con las series elegidas de 10:33 y el código de la app salen `c1 = 2.98471545E-01`, `c0 = -1.62263869E+02` para el código 1, y `c1 = 3.65483818E-01`, `c0 = -2.06628307E+02` para el 2, con la recta anclada en x = 565,36. Son exactamente los `#S` enviados (`tramas/rtv36_20260919_114644.txt:1660` y `:2520`). El equipo guarda `2.98471571E-01 / -1.62263885E+02` y `3.65483810E-01 / -2.06628295E+02` (relectura `#G` en float32), y esos son los coeficientes que usa aquí el estado «acta».

## 2. Reproducibilidad y combinación de las sesiones

### 2.1 A5, oscuro y re-medidas

| Patrón | Serie | Media de cada colocación | Media | s entre colocaciones | s_rep % | x 10:33 | A5 − 10:33 % |
| :--- | :---: | :--- | ---: | ---: | ---: | ---: | ---: |
| P4 | S059 | 3263,0 · 3337,7 · 3297,9 · 3358,6 · 3145,3 | 3280,5 | 84,0 | 2,56 | 3315,8 | -1,1 |
| P22 | S057 | 1409,3 · 1383,3 · 1440,8 · 1416,7 · 1419,7 | 1414,0 | 20,7 | 1,47 | 1497,0 | -5,5 |
| P28 | S058 | 2306,1 · 2163,6 · 2178,2 · 2270,7 · 2234,0 | 2230,5 | 60,3 | 2,70 | 2277,9 | -2,1 |

- s_rep media **2,24 %** (1,47 / 2,70 / 2,56). Se usa **2,2 %** por colocación, como fija el método. La A5 se midió después de cambiar la batería (`HUELLAS.txt`), y los tres patrones leen **por debajo** de 10:33. Con tres patrones no se distingue un efecto de sesión de la colocación.
- **Oscuro (S060):** medias por colocación 566,2 · 566,3 · 562,1 · 564,9 · 567,2; x = **565,36** (45 disparos); s entre colocaciones 2,0 cuentas. Es el ancla de las rectas ancladas. La regla B13 se evalúa aquí en esa x y en x = 575, que es la que usa la app (`Asistente.java:218`).
- **Re-medida del acta, P28 (código 1):** R = 497, 495, 499, 498, 498, 497, 501, 497, 498 (`tramas:2234`). La x de cada disparo es (R + 0,5 − c0) / c1 con la curva `#G`, porque el firmware trunca. Media **2213,1**. Entra como una colocación más.
- **Re-medida del acta, P5 (código 2):** R = 692, 692, 693, 695, 695, 693, 692, 690, 697 (`tramas:2762`). La x de cada disparo es (R + 0,5 − c0) / c1 con la curva `#G`, porque el firmware trunca. Media **2463,4**. Entra como una colocación más.
- La primera re-medida de P28 (`tramas:2156`, R = 227,3, NO CONFORME) tiene cinco disparos de 13-18, es decir, sin patrón debajo. No entra.

### 2.2 Combinación

Cada colocación (una serie de la mañana, una de 10:33, cada colocación de la A5, cada re-medida) pesa `1 / ((0,022·x)² + s²/n)`, y la media combinada tiene σ = `1/√Σw`. Como s_rep domina, una serie de 2 disparos pesa casi lo mismo que una de 9: lo que cuenta es el número de colocaciones K.

| Patrón | Cód. | Tipo | Cert. | K | x combinada | σ | Dispersión entre colocaciones % | Colocaciones |
| :--- | :---: | :---: | ---: | ---: | ---: | ---: | ---: | :--- |
| P1 | 1 | XI | 762 | 2 | 3013,9 | 46,9 | 2,6 | 10:33 S025: 2960,9 · mañana 09:16:45: 3071,0 |
| P2 | 1 | IV | 414 | 2 | 2036,3 | 31,7 | 1,5 | 10:33 S026: 2058,6 · mañana 09:17:21: 2015,0 |
| P3 | 1 | IV | 378 | 2 | 1666,9 | 26,0 | 0,6 | 10:33 S029: 1674,0 · mañana 09:17:50: 1659,9 |
| P4 | 1 | XI | 828 | 7 | 3294,0 | 27,4 | 2,3 | 10:33 S030: 3315,8 · mañana 09:18:21: 3361,1 · A5 S059 col 1: 3263,0 · A5 S059 col 2: 3337,7 · A5 S059 col 3: 3297,9 · A5 S059 col 4: 3358,6 · A5 S059 col 5: 3145,3 |
| P5 | 2 | XI | 740 | 5 | 2495,9 | 24,6 | 2,2 | 10:33 S023: 2456,8 · 10:33 S024 90°: 2459,3 · mañana 08:59:24: 2526,0 · mañana 09:25:00: 2584,0 · re-medida cód. 2 (tr:2762): 2463,4 |
| P6 | 1 | XI | 772 | 2 | 3141,5 | 48,9 | 1,2 | 10:33 S031: 3168,4 · mañana 09:18:59: 3115,5 |
| P7 | 1 | XI | 768 | 3 | 3195,0 | 40,6 | 2,0 | 10:33 S032: 3122,4 · mañana 09:19:26: 3230,9 · mañana 09:19:47: 3237,0 |
| P8 | 2 | XI | 721 | 2 | 2525,2 | 39,3 | 0,4 | 10:33 S033: 2532,9 · mañana 09:02:18: 2517,5 |
| P9 | 2 | XI | 705 | 2 | 2518,6 | 39,3 | 2,0 | 10:33 S034: 2483,9 · mañana 09:03:50: 2556,0 |
| P10 | 2 | XI | 680 | 2 | 2483,0 | 39,1 | 1,2 | 10:33 S035: 2504,3 · mañana 09:05:04: 2461,5 |
| P20 | 2 | XI | 782 | 3 | 2796,6 | 35,5 | 0,3 | 10:33 S045: 2787,7 · mañana 09:15:47: 2798,1 · mañana 09:23:40: 2804,1 |
| P21 | 2 | IV | 375 | 3 | 1557,8 | 19,8 | 0,3 | 10:33 S046: 1562,4 · mañana 09:02:50: 1559,0 · mañana 09:13:14: 1552,0 |
| P22 | 2 | IV | 334 | 7 | 1432,0 | 11,9 | 2,7 | 10:33 S048: 1497,0 · mañana 09:12:13: 1469,6 · A5 S057 col 1: 1409,3 · A5 S057 col 2: 1383,3 · A5 S057 col 3: 1440,8 · A5 S057 col 4: 1416,7 · A5 S057 col 5: 1419,7 |
| P23 | 2 | XI | 714 | 3 | 2738,5 | 34,8 | 0,6 | 10:33 S050: 2735,4 · mañana 09:14:57: 2723,4 · mañana 09:24:18: 2757,2 |
| P24 | 2 | IV | 593 | 2 | 2026,0 | 31,6 | 3,2 | 10:33 S047: 1982,7 · mañana 09:04:12: 2073,5 |
| P25 | 2 | IX | 576 | 2 | 2179,3 | 33,9 | 1,4 | 10:33 S051: 2157,4 · mañana 09:13:49: 2202,0 |
| P26 | 2 | IX | 583 | 2 | 2178,0 | 33,9 | 1,9 | 10:33 S052: 2208,8 · mañana 09:14:27: 2148,9 |
| P27 | 1 | IX | 471 | 1 | 2111,1 | 46,5 | — | 10:33 S027: 2111,1 |
| P28 | 1 | IX | 484 | 7 | 2232,6 | 18,6 | 2,4 | 10:33 S028: 2277,9 · A5 S058 col 1: 2306,1 · A5 S058 col 2: 2163,6 · A5 S058 col 3: 2178,2 · A5 S058 col 4: 2270,7 · A5 S058 col 5: 2234,0 · re-medida cód. 1 (tr:2234): 2213,1 |
| P29 | 2 | IV | 442 | 2 | 1700,1 | 26,5 | 2,0 | 10:33 S053: 1677,0 · mañana 09:22:51: 1724,5 |
| P30 | 2 | IV | 448 | 2 | 1659,1 | 25,8 | 0,4 | 10:33 S054: 1654,2 · mañana 09:05:32: 1664,0 |
| P31 | 2 | IX | 573 | 2 | 2091,1 | 32,6 | 0,4 | 10:33 S055: 2085,8 · mañana 09:03:21: 2096,5 |

Los demás patrones (P11-P19 y todo el tipo I) sólo tienen la colocación de 10:33: K = 1 y σ = 2,2 % de x.

## 3. Reformulación por código

**Criterio de «mejora significativa».** En cada x de la rejilla dentro del rango medido del código se compara la diferencia entre la curva nueva y la vigente con la σ de la curva nueva, obtenida de dos formas con el mismo código de ajuste de la app (4000 repeticiones, semilla fija):

- **σ_col (Monte Carlo de colocación):** cada x combinada se mueve N(0, σ) con su σ de §2.2, es decir, s_rep = 2,2 % por colocación dividido por √K. Es el criterio del encargo.
- **σ_pat (remuestreo de patrones):** se remuestrean los patrones con reposición. Recoge además la dispersión entre láminas, que es la que de verdad manda aquí.

Una curva nueva **sólo se propone si en algún punto |Δ| > 2·σ_col** y la app la deja escribir.

### 3.1 Código 1, blanco intenso (grado 1, decisión de Diego)

Curva nueva con las x combinadas: `c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 2.92574631E-01`, `c0 = -1.47096857E+02`. RMS 23,32 unidades de R; `#S` pasa; incumplimientos RF-CAL-14/15/16: 1 (RF-CAL-15: RMS de IV 9.2 % > 6 %).

| x | R acta | R nueva | Δ | σ_col | σ_pat | |Δ| / σ_col |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1667 | 335,3 | 340,6 | +5,3 | 6,0 | 22,3 | 0,89 |
| 1700 | 345,1 | 350,3 | +5,1 | 5,8 | 21,7 | 0,88 |
| 2000 | 434,7 | 438,1 | +3,4 | 4,6 | 16,8 | 0,73 |
| 2300 | 524,2 | 525,8 | +1,6 | 3,9 | 12,7 | 0,41 |
| 2500 | 583,9 | 584,3 | +0,4 | 3,8 | 10,7 | 0,11 |
| 2800 | 673,5 | 672,1 | -1,3 | 4,4 | 10,0 | 0,31 |
| 3000 | 733,2 | 730,6 | -2,5 | 5,1 | 11,3 | 0,49 |
| 3294 | 820,9 | 816,6 | -4,3 | 6,5 | 14,8 | 0,66 |

| Tipo | n | Fábrica | Acta de hoy | Nueva (combinada) | Nueva (una fila por colocación) |
| :---: | ---: | ---: | ---: | ---: | ---: |
| IV | 2 | +37,0 / 37,8 | -1,9 / 9,6 | -0,9 / 9,2 | -0,9 / 9,2 |
| IX | 2 | +32,8 / 32,8 | +1,6 / 3,0 | +2,2 / 3,2 | +2,1 / 3,1 |
| XI | 4 | +1,0 / 2,8 | -0,2 / 2,3 | -0,7 / 2,3 | -0,7 / 2,3 |

Sesgo / RMS en %, sobre las x combinadas. La variante «una fila por colocación» (`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 2.92237998E-01`, `c0 = -1.46590104E+02`) pesa cada patrón por su K. Monte Carlo de colocación: la curva re-ajustada falla `#S` en 25 de 4000 repeticiones y B13 (en x = 575) en 615: la recta libre del blanco está cerca del límite del oscuro.

**Veredicto código 1: se queda el acta.** |Δ| llega a 0,89·σ_col, lejos del umbral de 2; la mejora de RMS es de décimas de punto y está dentro de lo que mueve una colocación.

### 3.2 Código 2, amarillo intenso (recta anclada en el oscuro, decisión de Diego)

Curva nueva: `c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 3.64376405E-01`, `c0 = -2.06002225E+02`, anclada en x = 565,36. RMS 34,57; `#S` pasa; incumplimientos: 4 (RF-CAL-14: P23 (XI) residuo +10.9 % > 10 % / RF-CAL-14: P24 (IV) residuo -10.2 % > 10 % / RF-CAL-14: P30 (IV) residuo -11.0 % > 10 % / RF-CAL-15: RMS de IV 7.9 % > 6 %).

| x | R acta | R nueva | Δ | σ_col | σ_pat | |Δ| / σ_col |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1432 | 316,8 | 315,8 | -1,0 | 1,8 | 5,3 | 0,55 |
| 1500 | 341,6 | 340,6 | -1,0 | 1,9 | 5,7 | 0,55 |
| 1700 | 414,7 | 413,4 | -1,3 | 2,3 | 7,0 | 0,55 |
| 2000 | 524,3 | 522,8 | -1,6 | 2,9 | 8,8 | 0,55 |
| 2300 | 634,0 | 632,1 | -1,9 | 3,5 | 10,7 | 0,55 |
| 2500 | 707,1 | 704,9 | -2,1 | 3,9 | 11,9 | 0,55 |
| 2797 | 815,5 | 813,0 | -2,5 | 4,5 | 13,7 | 0,55 |

| Tipo | n | Fábrica | Acta de hoy | Nueva anclada | Anclada, una fila por colocación | Grado 1 libre (control) |
| :---: | ---: | ---: | ---: | ---: | ---: | ---: |
| IV | 5 | +20,1 / 20,6 | -7,2 / 7,8 | -7,5 / 8,0 | -7,5 / 8,0 | +0,1 / 5,8 |
| IX | 3 | +24,1 / 24,1 | +0,2 / 2,2 | -0,2 / 2,2 | -0,2 / 2,2 | +1,0 / 2,1 |
| XI | 6 | +12,6 / 13,3 | +2,4 / 5,4 | +2,1 / 5,3 | +2,0 / 5,2 | -0,0 / 4,3 |

El grado 1 libre (`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 3.15029239E-01`, `c0 = -9.32122398E+01`) sigue bloqueado por B13: en oscuro (x = 575) la curva nueva da R = 88 frente a 21 de fábrica; límite máx(fábrica + 10 ; 25) = 31. Una lámina degradada, o nada, leería 88 (P9-B13).

**Veredicto código 2: se queda el acta.** |Δ| llega a 0,55·σ_col, lejos del umbral de 2. La pendiente cambia un -0,30 %.

### 3.3 Código 8, amarillo tipo I (P34, P37, P43, P44; sólo 10:33)

| Curva | c1 / c0 | Sesgo / RMS % | Máx. |e| % | R(600) | MC: falla `#S` | Escribible | Incumple RF-CAL |
| :--- | :--- | ---: | ---: | ---: | ---: | :---: | ---: |
| Grado 1 | `2.20327236E-01` / `-1.31863958E+02` | -0,2 / 5,0 | 8,1 | 0,33 | 1603 de 4000 | sí | 0 |
| Recta anclada | `2.04279088E-01` / `-1.15490317E+02` | +0,7 / 5,7 | 7,0 | 7,08 | 0 de 4000 | sí | 0 |
| Fábrica | cúbica de 2020 | -17,2 / 19,4 | 28,7 | 17,87 | — | — | — |

| x | R fábrica | R nueva | Δ | σ_col | σ_pat | |Δ| / σ_col |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 899 | 61,2 | 68,1 | +6,8 | 1,7 | 1,7 | 3,94 |
| 900 | 61,4 | 68,4 | +7,0 | 1,7 | 1,7 | 3,99 |
| 1000 | 72,5 | 88,8 | +16,2 | 2,3 | 2,2 | 7,17 |
| 1151 | 88,0 | 119,7 | +31,7 | 3,1 | 2,9 | 10,38 |

**Veredicto código 8: escribir ya la recta anclada.** Frente a la fábrica, la diferencia llega a 10,4·σ_col: es significativa. El grado 1 da casi lo mismo en los patrones, pero corta el cero en x ≈ 598 y pasa `#S` por 0,33 unidades en x = 600: una re-medida lo dejaría sin poder escribir en 1603 de 4000 casos. La anclada no depende de eso (0 de 4000) y usa el mismo método que el código 2. **Queda con K = 1 por patrón:** su re-medida de verificación (RF-CAL-18) es la que confirma.

### 3.4 Código b, rojo tipo I (P38, P39, P49)

| Curva | c1 / c0 | Error por patrón % | Sesgo / RMS % | Escribible |
| :--- | :--- | :--- | ---: | :--- |
| Grado 1 | `5.52204417E-01` / `-3.59170158E+02` | P38 -1,9 · P39 +0,0 · P49 -1,2 | -1,1 / 1,3 | no: la curva se hace negativa en x = 600 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#) |
| Recta anclada | `3.13845140E-01` / `-1.77434093E+02` | P38 +7,7 · P39 +13,0 · P49 -11,1 | +3,2 / 10,8 | sí |
| Fábrica | — | P38 -71,2 · P39 -67,4 · P49 -74,1 | -70,9 / 70,9 | — |

Incumplimientos de la anclada: RF-CAL-14: P39 (I) residuo +15.3 % > 10 % / RF-CAL-14: P49 (I) residuo -10.2 % > 10 % / RF-CAL-15: RMS de I 11.5 % > 6 %. MC de colocación: σ de R en x = 800 = 3,7; falla `#S` en 0 de 4000.

**Veredicto código b: se puede escribir ya, con la recta anclada; el banco no lo mejora.** P51-P132 no trae ningún rojo tipo I (el catálogo tiene tipo I sólo en P32a-P50), así que esperar al banco no aporta nada. La fábrica lee un 70 % por debajo; la anclada deja los tres patrones en ±15 %. La de grado 1 no se puede escribir: los tres patrones cubren sólo 63 cuentas de x. Incumple RF-CAL-14 en dos patrones: necesita la conformidad de Diego, como la tuvieron los códigos 1 y 2.

### 3.5 Códigos 3, 4, 5, 6, 7, a, c y d: ¿ahora o con el banco?

Cobertura según `Asistente.cobertura` (`Asistente.java:101`): hacen falta 3 niveles certificados y un rango de al menos 20 unidades de R y el 30 % del mayor (`:34-36`, `:93`). La recta anclada pasa por el mismo filtro (`Asistente.proponer`, `:416-432`).

| Código | Lámina | Hoy (P1-P50) | Con el banco (P1-P132) | Fábrica hoy: sesgo / RMS % | Veredicto |
| :---: | :--- | :--- | :--- | ---: | :--- |
| 3 | verde intenso | 4 patrones, 4 niveles, R 164-173 -> NO ajustable: rango certificado estrecho (164-173): ajustar sería extrapolar a ciegas; sólo verificar | 19 patrones, 11 niveles, R 51-173 -> ajustable hasta grado 2 | -17,0 / 17,1 | **Esperar al banco** |
| 4 | rojo intenso | 2 patrones, 2 niveles, R 194-227 -> NO ajustable: 2 niveles certificados: hacen falta 3 para grado 1; sólo verificar | 26 patrones, 24 niveles, R 36-279 -> ajustable hasta grado 2 | +14,6 / 16,7 | **Esperar al banco** |
| 5 | azul intenso | 3 patrones, 3 niveles, R 83-85 -> NO ajustable: rango certificado estrecho (83-85): ajustar sería extrapolar a ciegas; sólo verificar | 13 patrones, 12 niveles, R 83-102 -> NO ajustable: rango certificado estrecho (83-102): ajustar sería extrapolar a ciegas; sólo verificar | -56,7 / 56,8 | **Ni con el banco**: sólo verificar |
| 6 | naranja intenso | 0 patrones -> NO ajustable: no hay patrones de este color y clase | 15 patrones, 15 niveles, R 80-173 -> ajustable hasta grado 2 | sin patrones | **Esperar al banco** |
| 7 | blanco tipo I | 1 patrones, 1 niveles, R 96-96 -> NO ajustable: 1 nivel certificados: hacen falta 3 para grado 1; sólo verificar | 1 patrones, 1 niveles, R 96-96 -> NO ajustable: 1 nivel certificados: hacen falta 3 para grado 1; sólo verificar | +0,0 / 0,0 | Sólo verificar; el banco no aporta (no trae tipo I) |
| a | verde tipo I | 2 patrones, 2 niveles, R 6-7 -> NO ajustable: 2 niveles certificados: hacen falta 3 para grado 1; sólo verificar | 2 patrones, 2 niveles, R 6-7 -> NO ajustable: 2 niveles certificados: hacen falta 3 para grado 1; sólo verificar | +800,0 / 806,2 | Sólo verificar; el banco no aporta (no trae tipo I) |
| c | azul tipo I | 6 patrones, 3 niveles, R 7-10 -> NO ajustable: rango certificado estrecho (7-10): ajustar sería extrapolar a ciegas; sólo verificar | 6 patrones, 3 niveles, R 7-10 -> NO ajustable: rango certificado estrecho (7-10): ajustar sería extrapolar a ciegas; sólo verificar | +45,1 / 49,1 | Sólo verificar; el banco no aporta (no trae tipo I) |
| d | naranja tipo I | 4 patrones, 4 niveles, R 68-73 -> NO ajustable: rango certificado estrecho (68-73): ajustar sería extrapolar a ciegas; sólo verificar | 4 patrones, 4 niveles, R 68-73 -> NO ajustable: rango certificado estrecho (68-73): ajustar sería extrapolar a ciegas; sólo verificar | -33,2 / 34,2 | Sólo verificar; el banco no aporta (no trae tipo I) |

- **3 (verde):** hoy, 4 patrones de 164-173: rango estrecho. El banco añade de 51 a 121. La fábrica lee entre −14 y −18 %, y en el oscuro da R = 31,5: el oscuro leería como una lámina verde de R ≈ 31.
- **4 (rojo):** hoy, 2 niveles (194 y 227). El banco añade 24 niveles de 36 a 279, contando café y lila. La curva de fábrica es casi plana en esa zona (0,09 R por cuenta en x ≈ 1740), así que invertirla predice mal la x: véase §5.
- **5 (azul):** hoy de 83 a 85; con el banco, de 83 a 102 (19 % del mayor, por debajo del 30 %). **La app no lo va a dejar ajustar ni con el banco.** La fábrica lee un 55-58 % por debajo (P17-P19). La única salida es una recta anclada con dispensa de la regla de cobertura; la anclada de hoy daría `c1 = 3.27318464E-01`, RMS 2,43 unidades, pero la app la bloquea por cobertura. Lo decide Diego.
- **6 (naranja intenso):** ningún patrón hoy; el banco trae 15, de 80 a 173.
- **7, a, c y d (tipo I):** el banco no trae tipo I. Siguen como están: 7 lee el certificado (P35, un solo patrón), a lee 56-60 para 6-7, y c y d tienen rango estrecho.

## 4. Simulación antes / ahora / propuesta

«Antes» es la fábrica, «ahora» el acta de las 12:23 y «propuesta» el acta más los códigos 8 y b de §3. R es la respuesta del firmware: `Ecuacion.respuestaFloat32(round(x))`, con x la media combinada de §2.2.

### 4.1 Los 50 patrones medidos

| Patrón | Cód. | Tipo | Color | Cert. | K | x | R fábrica | e % | R acta | e % | R propuesta | e % |
| :--- | :---: | :---: | :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P1 | 1 | XI | blanco | 762 | 2 | 3013,9 | 778 | +2,1 | 737 | -3,3 | 737 | -3,3 |
| P2 | 1 | IV | blanco | 414 | 2 | 2036,3 | 599 | +44,7 | 445 | +7,5 | 445 | +7,5 |
| P3 | 1 | IV | blanco | 378 | 2 | 1666,9 | 489 | +29,4 | 335 | -11,4 | 335 | -11,4 |
| P4 | 1 | XI | blanco | 828 | 7 | 3294,0 | 799 | -3,5 | 820 | -1,0 | 820 | -1,0 |
| P5 | 2 | XI | amarillo | 740 | 5 | 2495,9 | 804 | +8,6 | 705 | -4,7 | 705 | -4,7 |
| P6 | 1 | XI | blanco | 772 | 2 | 3141,5 | 789 | +2,2 | 775 | +0,4 | 775 | +0,4 |
| P7 | 1 | XI | blanco | 768 | 3 | 3195,0 | 793 | +3,3 | 791 | +3,0 | 791 | +3,0 |
| P8 | 2 | XI | amarillo | 721 | 2 | 2525,2 | 809 | +12,2 | 716 | -0,7 | 716 | -0,7 |
| P9 | 2 | XI | amarillo | 705 | 2 | 2518,6 | 808 | +14,6 | 714 | +1,3 | 714 | +1,3 |
| P10 | 2 | XI | amarillo | 680 | 2 | 2483,0 | 802 | +17,9 | 700 | +2,9 | 700 | +2,9 |
| P11 | 4 | IV | rojo | 194 | 1 | 1722,9 | 238 | +22,7 | 238 | +22,7 | 238 | +22,7 |
| P12 | 4 | IV | rojo | 227 | 1 | 1761,9 | 242 | +6,6 | 242 | +6,6 | 242 | +6,6 |
| P13 | 3 | XI | verde | 168 | 1 | 830,9 | 137 | -18,5 | 137 | -18,5 | 137 | -18,5 |
| P14 | 3 | XI | verde | 173 | 1 | 849,1 | 142 | -17,9 | 142 | -17,9 | 142 | -17,9 |
| P15 | 3 | XI | verde | 164 | 1 | 843,2 | 140 | -14,6 | 140 | -14,6 | 140 | -14,6 |
| P16 | 3 | XI | verde | 170 | 1 | 846,7 | 141 | -17,1 | 141 | -17,1 | 141 | -17,1 |
| P17 | 5 | XI | azul | 83 | 1 | 828,4 | 37 | -55,4 | 37 | -55,4 | 37 | -55,4 |
| P18 | 5 | XI | azul | 84 | 1 | 813,4 | 35 | -58,3 | 35 | -58,3 | 35 | -58,3 |
| P19 | 5 | XI | azul | 85 | 1 | 823,6 | 37 | -56,5 | 37 | -56,5 | 37 | -56,5 |
| P20 | 2 | XI | amarillo | 782 | 3 | 2796,6 | 829 | +6,0 | 815 | +4,2 | 815 | +4,2 |
| P21 | 2 | IV | amarillo | 375 | 3 | 1557,8 | 472 | +25,9 | 362 | -3,5 | 362 | -3,5 |
| P22 | 2 | IV | amarillo | 334 | 7 | 1432,0 | 412 | +23,4 | 316 | -5,4 | 316 | -5,4 |
| P23 | 2 | XI | amarillo | 714 | 3 | 2738,5 | 829 | +16,1 | 794 | +11,2 | 794 | +11,2 |
| P24 | 2 | IV | amarillo | 593 | 2 | 2026,0 | 673 | +13,5 | 533 | -10,1 | 533 | -10,1 |
| P25 | 2 | IX | amarillo | 576 | 2 | 2179,3 | 726 | +26,0 | 589 | +2,3 | 589 | +2,3 |
| P26 | 2 | IX | amarillo | 583 | 2 | 2178,0 | 726 | +24,5 | 589 | +1,0 | 589 | +1,0 |
| P27 | 1 | IX | blanco | 471 | 1 | 2111,1 | 619 | +31,4 | 467 | -0,8 | 467 | -0,8 |
| P28 | 1 | IX | blanco | 484 | 7 | 2232,6 | 649 | +34,1 | 504 | +4,1 | 504 | +4,1 |
| P29 | 2 | IV | amarillo | 442 | 2 | 1700,1 | 538 | +21,7 | 414 | -6,3 | 414 | -6,3 |
| P30 | 2 | IV | amarillo | 448 | 2 | 1659,1 | 520 | +16,1 | 399 | -10,9 | 399 | -10,9 |
| P31 | 2 | IX | amarillo | 573 | 2 | 2091,1 | 697 | +21,6 | 557 | -2,8 | 557 | -2,8 |
| P32a | c | I | azul | 9 | 1 | 630,9 | 14 | +55,6 | 14 | +55,6 | 14 | +55,6 |
| P32b | d | I | naranja | 68 | 1 | 802,8 | 38 | -44,1 | 38 | -44,1 | 38 | -44,1 |
| P33 | d | I | naranja | 72 | 1 | 849,8 | 45 | -37,5 | 45 | -37,5 | 45 | -37,5 |
| P34 | 8 | I | amarillo | 86 | 1 | 958,3 | 67 | -22,1 | 67 | -22,1 | 80 | -7,0 |
| P35 | 7 | I | blanco | 96 | 1 | 1114,7 | 96 | +0,0 | 96 | +0,0 | 96 | +0,0 |
| P36 | c | I | azul | 10 | 1 | 638,8 | 15 | +50,0 | 15 | +50,0 | 15 | +50,0 |
| P37 | 8 | I | amarillo | 82 | 1 | 992,4 | 71 | -13,4 | 71 | -13,4 | 87 | +6,1 |
| P38 | b | I | rojo | 52 | 1 | 743,9 | 15 | -71,2 | 15 | -71,2 | 56 | +7,7 |
| P39 | b | I | rojo | 46 | 1 | 734,3 | 15 | -67,4 | 15 | -67,4 | 52 | +13,0 |
| P40 | a | I | verde | 6 | 1 | 624,7 | 60 | +900,0 | 60 | +900,0 | 60 | +900,0 |
| P41 | d | I | naranja | 73 | 1 | 898,7 | 53 | -27,4 | 53 | -27,4 | 53 | -27,4 |
| P42 | c | I | azul | 7 | 1 | 592,6 | 10 | +42,9 | 10 | +42,9 | 10 | +42,9 |
| P43 | 8 | I | amarillo | 122 | 1 | 1151,3 | 87 | -28,7 | 87 | -28,7 | 119 | -2,5 |
| P44 | 8 | I | amarillo | 64 | 1 | 898,6 | 61 | -4,7 | 61 | -4,7 | 68 | +6,2 |
| P45 | a | I | verde | 7 | 1 | 615,6 | 56 | +700,0 | 56 | +700,0 | 56 | +700,0 |
| P46 | c | I | azul | 9 | 1 | 644,3 | 16 | +77,8 | 16 | +77,8 | 16 | +77,8 |
| P47 | c | I | azul | 9 | 1 | 606,3 | 11 | +22,2 | 11 | +22,2 | 11 | +22,2 |
| P48 | c | I | azul | 9 | 1 | 605,4 | 11 | +22,2 | 11 | +22,2 | 11 | +22,2 |
| P49 | b | I | rojo | 81 | 1 | 797,2 | 21 | -74,1 | 21 | -74,1 | 72 | -11,1 |
| P50 | d | I | naranja | 71 | 1 | 907,1 | 54 | -23,9 | 54 | -23,9 | 54 | -23,9 |

### 4.2 Resumen por código

| Código | Lámina | n | Fábrica: sesgo / RMS / máx % | Acta | Propuesta | Curva propuesta |
| :---: | :--- | ---: | ---: | ---: | ---: | :--- |
| 1 | blanco intenso | 8 | +18,0 / 25,1 / 44,7 | -0,2 / 5,3 / 11,4 | -0,2 / 5,3 / 11,4 | acta |
| 2 | amarillo intenso | 14 | +17,7 / 18,8 / 26,0 | -1,5 / 5,9 / 11,2 | -1,5 / 5,9 / 11,2 | acta |
| 3 | verde intenso | 4 | -17,0 / 17,1 / 18,5 | -17,0 / 17,1 / 18,5 | -17,0 / 17,1 / 18,5 | fábrica |
| 4 | rojo intenso | 2 | +14,6 / 16,7 / 22,7 | +14,6 / 16,7 / 22,7 | +14,6 / 16,7 / 22,7 | fábrica |
| 5 | azul intenso | 3 | -56,7 / 56,8 / 58,3 | -56,7 / 56,8 / 58,3 | -56,7 / 56,8 / 58,3 | fábrica |
| 6 | naranja intenso | 0 | — | — | — | sin patrones |
| 7 | blanco tipo I | 1 | +0,0 / 0,0 / 0,0 | +0,0 / 0,0 / 0,0 | +0,0 / 0,0 / 0,0 | fábrica |
| 8 | amarillo tipo I | 4 | -17,2 / 19,4 / 28,7 | -17,2 / 19,4 / 28,7 | +0,7 / 5,7 / 7,0 | nueva |
| a | verde tipo I | 2 | +800,0 / 806,2 / 900,0 | +800,0 / 806,2 / 900,0 | +800,0 / 806,2 / 900,0 | fábrica |
| b | rojo tipo I | 3 | -70,9 / 70,9 / 74,1 | -70,9 / 70,9 / 74,1 | +3,2 / 10,8 / 13,0 | nueva |
| c | azul tipo I | 6 | +45,1 / 49,1 / 77,8 | +45,1 / 49,1 / 77,8 | +45,1 / 49,1 / 77,8 | fábrica |
| d | naranja tipo I | 4 | -33,2 / 34,2 / 44,1 | -33,2 / 34,2 / 44,1 | -33,2 / 34,2 / 44,1 | fábrica |

### 4.3 Resumen por tipo de lámina

| Tipo | Códigos | n | Fábrica: sesgo / RMS % | Acta | Propuesta |
| :---: | :--- | ---: | ---: | ---: | ---: |
| IV | intensas 1-6 | 9 | +22,6 / 24,8 | -1,2 / 10,8 | -1,2 / 10,8 |
| IV | 1 y 2 | 7 | +24,9 / 26,7 | -5,7 / 8,4 | -5,7 / 8,4 |
| IX | intensas 1-6 | 5 | +27,5 / 27,9 | +0,8 / 2,5 | +0,8 / 2,5 |
| IX | 1 y 2 | 5 | +27,5 / 27,9 | +0,8 / 2,5 | +0,8 / 2,5 |
| XI | intensas 1-6 | 17 | -9,3 / 26,5 | -13,2 / 25,5 | -13,2 / 25,5 |
| XI | 1 y 2 | 10 | +8,0 / 10,4 | +1,3 / 4,4 | +1,3 / 4,4 |
| I | tipo I 7-d | 20 | +72,8 / 258,4 | +72,8 / 258,4 | +87,5 / 256,9 |
| I | 8 y b | 7 | -40,2 / 48,7 | -40,2 / 48,7 | +1,8 / 8,3 |

En las intensas 3-5 los tres estados coinciden (fábrica): es donde queda el error grande, y es lo que el banco tiene que resolver.

### 4.4 ¿El acta predice lo que no usó?

El acta se ajustó sólo con 10:33. Con la mañana, la A5 y las re-medidas, que no usó, se ve cómo predice fuera de su muestra:

| Código | x de | n | Fábrica: sesgo / RMS % | Acta: sesgo / RMS % |
| :---: | :--- | ---: | ---: | ---: |
| 1 | 10:33 (en muestra) | 7 | +16,5 / 25,0 | +0,2 / 6,3 |
| 1 | mañana + A5 + re-medidas (fuera) | 7 | +15,8 / 23,6 | -0,1 / 5,5 |
| 2 | 10:33 (en muestra) | 14 | +17,9 / 19,4 | -1,5 / 6,4 |
| 2 | mañana + A5 + re-medidas (fuera) | 14 | +18,1 / 19,0 | -1,1 / 5,5 |

Mismos patrones en las dos filas: los que tienen alguna colocación fuera de 10:33.

### 4.5 Oscuro y regla B13

B13: R en el oscuro ≤ máx(fábrica + 10 ; 25) (`Asistente.java:237-247`). R es la respuesta float32.

| Código | Lámina | Fábrica: R(565,4) / R(575) | Acta | Propuesta | B13 de la propuesta |
| :---: | :--- | ---: | ---: | ---: | :---: |
| 1 | blanco intenso | 19 / 24 | 6 / 9 | 6 / 9 | cumple |
| 2 | amarillo intenso | 17 / 20 | 0 / 3 | 0 / 3 | cumple |
| 3 | verde intenso | 31 / 36 | 31 / 36 | 31 / 36 | cumple |
| 4 | rojo intenso | 31 / 36 | 31 / 36 | 31 / 36 | cumple |
| 5 | azul intenso | 7 / 8 | 7 / 8 | 7 / 8 | cumple |
| 6 | naranja intenso | 8 / 9 | 8 / 9 | 8 / 9 | cumple |
| 7 | blanco tipo I | 8 / 9 | 8 / 9 | 8 / 9 | cumple |
| 8 | amarillo tipo I | 11 / 13 | 11 / 13 | 0 / 1 | cumple |
| a | verde tipo I | 31 / 36 | 31 / 36 | 31 / 36 | cumple |
| b | rojo tipo I | 3 / 3 | 3 / 3 | 0 / 3 | cumple |
| c | azul tipo I | 7 / 8 | 7 / 8 | 7 / 8 | cumple |
| d | naranja tipo I | 8 / 9 | 8 / 9 | 8 / 9 | cumple |

### 4.6 Criterio de `#S` y forma (C2)

`#S`: R finito y en [0 ; 4000] en toda x de 600 a 4300, en float32, lo que cubre los bordes y los extremos interiores (`Asistente.criterioFirmwareS`, igual en efecto a `calibracion_v36.c:404-420`). C2: creciente, sin negativos desde el patrón más bajo y ≤ 4000 en 200-4400 (`Asistente.validarForma`).

| Código | Fábrica | Acta | Propuesta | Propuesta: R(600) / R(4300) |
| :---: | :--- | :--- | :--- | ---: |
| 1 | `#S` sí; C2 no (la curva no es creciente en x = 3581 (techo dentro de 200-4400)) | `#S` sí; C2 sí | `#S` sí; C2 sí | 16 / 1121 |
| 2 | `#S` **no** (la curva se hace negativa en x = 4175); C2 no (la curva no es creciente en x = 2784 (techo dentro de 200-4400)) | `#S` sí; C2 sí | `#S` sí; C2 sí | 12 / 1364 |
| 3 | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | 48 / 3742 |
| 4 | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 47 / 3585 |
| 5 | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 11 / 2117 |
| 6 | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 12 / 1661 |
| 7 | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 12 / 1647 |
| 8 | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 7 / 762 |
| a | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | `#S` sí; C2 no (la curva pasa de 4000 en x = 4367 (el firmware lo pondría a 0)) | 48 / 3742 |
| b | `#S` sí; C2 no (la curva no es creciente en x = 200 (techo dentro de 200-4400)) | `#S` sí; C2 no (la curva no es creciente en x = 200 (techo dentro de 200-4400)) | `#S` sí; C2 sí | 10 / 1172 |
| c | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 11 / 2117 |
| d | `#S` sí; C2 sí | `#S` sí; C2 sí | `#S` sí; C2 sí | 12 / 1661 |

Las curvas de fábrica que no pasan `#S` o C2 están en ROM y no se escriben con `#S`: el dato es informativo. **Todas las curvas escritas o propuestas pasan `#S`.**

## 5. Predicción para P51-P132 (ESTIMACIÓN, sin medir)

**Nada de esta tabla está medido.** Sirve para que, en el banco, un patrón que caiga fuera de su banda se levante y se vuelva a colocar en el momento.

- **x fábrica:** la curva de fábrica invertida, primera x ≥ oscuro con R(x) = certificado. Es lo que pide el encargo, pero en 3, 4 y 5 la fábrica se equivoca mucho (§4.2), y en el rojo es casi plana: esa x no sirve para vigilar.
- **x esperada:** la que hay que usar. En 1 y 2, la curva del acta invertida, corregida por la mediana medida en P1-P50 (dispersión 5,7 % y 5,9 %). En 3, 4 y 5, recta por el oscuro con la sensibilidad medida, k = cert / (x − oscuro): verde 0,6070, rojo 0,1787 y azul 0,3292 R/cuenta (n = 4, 2 y 3). En 6 no hay ningún patrón medido: sólo la fábrica.
- **Banda (±):** 2·√((s_modelo·(x − oscuro))² + (0,022·x)²). s_modelo es la dispersión relativa medida en el código (1: 5,7 %, 2: 5,9 %, 3: 2,9 %, 4: 8,8 %, 5: 3,5 %). En el 6 es el error máximo de fábrica del naranja tipo I (d, que tiene la misma ecuación): 44 %, una **heurística**.
- **R de cada estado:** respuesta float32 en la x esperada. En 3-6 los tres estados son la fábrica. En 1 y 2 el acta da el certificado por construcción: lo informativo es cuánto leería la fábrica.

| Patrón | Cód. | Tipo | Color | Cert. | x fábrica | x esperada | Banda | Aceptar x en | R fábrica | R acta | R propuesta |
| :--- | :---: | :---: | :--- | ---: | ---: | ---: | ---: | :---: | ---: | ---: | ---: |
| P51 | 1 | IV | blanco | 439 | 1520 | 2013 | ±189 | 1824-2201 | 593 | 438 | 438 |
| P52 | 1 | IV | blanco | 421 | 1470 | 1953 | ±181 | 1771-2134 | 577 | 420 | 420 |
| P53 | 4 | XI | rojo | 279 | 2032 | 2127 | ±289 | 1838-2416 | 298 | 298 | 298 |
| P54 | 4 | IV | rojo | 121 | 803 | 1243 | ±131 | 1112-1373 | 201 | 201 | 201 |
| P55 | 4 | XI | rojo | 146 | 897 | 1383 | ±155 | 1227-1538 | 213 | 213 | 213 |
| P56 | 1 | IV | blanco | 347 | 1277 | 1705 | ±151 | 1554-1856 | 501 | 346 | 346 |
| P57 | 4 | IV | rojo | 218 | 1438 | 1786 | ±228 | 1558-2013 | 244 | 244 | 244 |
| P58 | 1 | IV | blanco | 432 | 1501 | 1989 | ±186 | 1804-2175 | 587 | 431 | 431 |
| P59 | 4 | IV | rojo | 122 | 806 | 1248 | ±132 | 1117-1380 | 202 | 202 | 202 |
| P60 | 4 | IV | rojo | 68 | 649 | 946 | ±79 | 867-1025 | 157 | 157 | 157 |
| P61 | 2 | IV | amarillo | 207 | 1007 | 1122 | ±82 | 1040-1205 | 261 | 203 | 203 |
| P62 | 2 | IV | amarillo | 280 | 1160 | 1319 | ±106 | 1212-1425 | 357 | 275 | 275 |
| P63 | 3 | IV | verde | 121 | 779 | 765 | ±36 | 729-800 | 116 | 116 | 116 |
| P64 | 3 | IV | verde | 54 | 611 | 654 | ±29 | 625-684 | 73 | 73 | 73 |
| P65 | 3 | IV | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P66 | 5 | IV | azul | 89 | 1182 | 836 | ±41 | 794-877 | 38 | 38 | 38 |
| P67 | 5 | IV | azul | 95 | 1217 | 854 | ±43 | 811-897 | 41 | 41 | 41 |
| P68 | 4 | IV | cafe | 36 | 574 | 767 | ±49 | 718-816 | 110 | 110 | 110 |
| P69 | 4 | IV | lila | 139 | 868 | 1343 | ±148 | 1195-1492 | 210 | 210 | 210 |
| P70 | 6 | IV | naranja | 80 | 1049 | 1049 | ±425 | 624-1474 | 79 | 79 | 79 |
| P71 | 6 | IV | naranja | 84 | 1070 | 1070 | ±443 | 627-1513 | 84 | 84 | 84 |
| P72 | 6 | IV | naranja | 93 | 1115 | 1115 | ±482 | 633-1597 | 92 | 92 | 92 |
| P73 | 1 | IX | blanco | 614 | 2090 | 2598 | ±260 | 2338-2859 | 722 | 613 | 613 |
| P74 | 4 | IX | rojo | 169 | 1007 | 1511 | ±178 | 1333-1690 | 223 | 223 | 223 |
| P75 | 4 | IX | rojo | 138 | 864 | 1338 | ±147 | 1190-1485 | 210 | 210 | 210 |
| P76 | 2 | IX | amarillo | 433 | 1474 | 1730 | ±157 | 1573-1888 | 552 | 425 | 425 |
| P77 | 2 | IX | amarillo | 391 | 1387 | 1617 | ±143 | 1474-1761 | 500 | 384 | 384 |
| P78 | 3 | IX | verde | 97 | 712 | 725 | ±33 | 692-758 | 101 | 101 | 101 |
| P79 | 3 | IX | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P80 | 3 | IX | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P81 | 5 | IX | azul | 92 | 1200 | 845 | ±42 | 803-887 | 39 | 39 | 39 |
| P82 | 5 | IX | azul | 98 | 1235 | 863 | ±43 | 820-906 | 42 | 42 | 42 |
| P83 | 4 | IX | cafe | 55 | 617 | 873 | ±66 | 807-939 | 140 | 140 | 140 |
| P84 | 4 | IX | lila | 177 | 1053 | 1556 | ±186 | 1370-1743 | 226 | 226 | 226 |
| P85 | 6 | IX | naranja | 118 | 1232 | 1232 | ±584 | 648-1816 | 118 | 118 | 118 |
| P86 | 6 | IX | naranja | 124 | 1258 | 1258 | ±607 | 651-1866 | 123 | 123 | 123 |
| P87 | 6 | IX | naranja | 131 | 1288 | 1288 | ±634 | 655-1922 | 130 | 130 | 130 |
| P88 | 1 | XI | blanco | 773 | 2960 | 3130 | ±325 | 2805-3456 | 788 | 771 | 771 |
| P89 | 4 | XI | rojo | 183 | 1091 | 1590 | ±192 | 1397-1782 | 228 | 228 | 228 |
| P90 | 4 | XI | rojo | 162 | 970 | 1472 | ±171 | 1301-1644 | 220 | 220 | 220 |
| P91 | 2 | XI | amarillo | 516 | 1650 | 1954 | ±185 | 1768-2139 | 646 | 507 | 507 |
| P92 | 2 | XI | amarillo | 522 | 1663 | 1970 | ±187 | 1782-2157 | 652 | 513 | 513 |
| P93 | 3 | XI | verde | 85 | 682 | 705 | ±32 | 673-737 | 94 | 94 | 94 |
| P94 | 3 | XI | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P95 | 3 | XI | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P96 | 5 | XI | azul | 95 | 1217 | 854 | ±43 | 811-897 | 41 | 41 | 41 |
| P97 | 5 | XI | azul | 102 | 1257 | 875 | ±44 | 831-919 | 43 | 43 | 43 |
| P98 | 4 | XI | cafe | 68 | 649 | 946 | ±79 | 867-1025 | 157 | 157 | 157 |
| P99 | 4 | XI | lila | 188 | 1127 | 1618 | ±198 | 1420-1815 | 230 | 230 | 230 |
| P100 | 6 | XI | naranja | 143 | 1339 | 1339 | ±677 | 661-2016 | 143 | 143 | 143 |
| P101 | 6 | XI | naranja | 146 | 1351 | 1351 | ±688 | 663-2039 | 146 | 146 | 146 |
| P102 | 6 | XI | naranja | 173 | 1456 | 1456 | ±780 | 676-2237 | 172 | 172 | 172 |
| P103 | 1 | XI | blanco | 763 | 2873 | 3097 | ±321 | 2776-3418 | 786 | 762 | 762 |
| P104 | 4 | XI | rojo | 173 | 1029 | 1534 | ±182 | 1351-1716 | 224 | 224 | 224 |
| P105 | 4 | XI | rojo | 164 | 980 | 1483 | ±173 | 1310-1657 | 221 | 221 | 221 |
| P106 | 2 | XI | amarillo | 493 | 1601 | 1892 | ±178 | 1714-2069 | 621 | 484 | 484 |
| P107 | 2 | XI | amarillo | 478 | 1569 | 1851 | ±173 | 1679-2024 | 604 | 469 | 469 |
| P108 | 3 | XI | verde | 96 | 709 | 724 | ±33 | 690-757 | 101 | 101 | 101 |
| P109 | 3 | XI | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P110 | 3 | XI | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P111 | 5 | XI | azul | 93 | 1206 | 848 | ±42 | 806-890 | 40 | 40 | 40 |
| P112 | 5 | XI | azul | 101 | 1252 | 872 | ±44 | 828-916 | 43 | 43 | 43 |
| P113 | 4 | XI | cafe | 60 | 629 | 901 | ±71 | 830-972 | 146 | 146 | 146 |
| P114 | 4 | XI | lila | 202 | 1246 | 1696 | ±212 | 1484-1908 | 236 | 236 | 236 |
| P115 | 6 | XI | naranja | 147 | 1355 | 1355 | ±692 | 663-2047 | 147 | 147 | 147 |
| P116 | 6 | XI | naranja | 158 | 1399 | 1399 | ±730 | 669-2129 | 158 | 158 | 158 |
| P117 | 6 | XI | naranja | 154 | 1383 | 1383 | ±716 | 667-2099 | 154 | 154 | 154 |
| P118 | 1 | IX | blanco | 508 | 1724 | 2244 | ±217 | 2027-2460 | 651 | 507 | 507 |
| P119 | 4 | IX | rojo | 138 | 864 | 1338 | ±147 | 1190-1485 | 210 | 210 | 210 |
| P120 | 4 | IX | rojo | 110 | 766 | 1181 | ±120 | 1061-1301 | 194 | 194 | 194 |
| P121 | 2 | IX | amarillo | 320 | 1242 | 1426 | ±120 | 1307-1546 | 409 | 314 | 314 |
| P122 | 2 | IX | amarillo | 352 | 1307 | 1512 | ±130 | 1382-1643 | 451 | 345 | 345 |
| P123 | 3 | IX | verde | 115 | 762 | 755 | ±35 | 720-790 | 112 | 112 | 112 |
| P124 | 3 | IX | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P125 | 3 | IX | verde | 51 | 605 | 649 | ±29 | 620-678 | 71 | 71 | 71 |
| P126 | 5 | IX | azul | 91 | 1194 | 842 | ±42 | 800-884 | 39 | 39 | 39 |
| P127 | 5 | IX | azul | 97 | 1229 | 860 | ±43 | 817-903 | 41 | 41 | 41 |
| P128 | 4 | IX | cafe | 38 | 579 | 778 | ±51 | 727-829 | 113 | 113 | 113 |
| P129 | 4 | IX | lila | 157 | 946 | 1444 | ±166 | 1278-1611 | 218 | 218 | 218 |
| P130 | 6 | IX | naranja | 98 | 1139 | 1139 | ±503 | 636-1643 | 97 | 97 | 97 |
| P131 | 6 | IX | naranja | 102 | 1158 | 1158 | ±520 | 638-1679 | 101 | 101 | 101 |
| P132 | 6 | IX | naranja | 117 | 1227 | 1227 | ±580 | 647-1808 | 116 | 116 | 116 |

A tener en cuenta en el banco:

- **Verdes de R 51-54** (P64, P65, P79, P80, P94, P95, P109, P110, P124, P125): x esperada ≈ 650, sólo unas 85 cuentas por encima del oscuro, con la fábrica leyendo ≈ 71. Son los patrones con menos señal del banco.
- **Café** (P68, P83, P98, P113, P128, R 36-68): con el código del rojo, la fábrica leería 110 / 140 / 157 / 146 / 113. La curva de fábrica del rojo ya da R ≈ 32 en el propio oscuro.
- **Rojo, café y lila:** la x esperada sale de dos patrones IV (P11, P12). Es la estimación más floja de la tabla. La k de los XI puede ser otra.
- **Naranja intenso:** sin ninguna referencia medida. La banda es ancha a propósito.

## 6. Veredicto

| Código | Qué hacer | Por qué | Banco P51-P132 |
| :---: | :--- | :--- | :--- |
| 1 | **No reescribir.** Se queda el acta | Δ máx 0,89·σ_col; mejora dentro de la reproducibilidad | Con el banco (P51, P52, P56, P58, P73, P88, P103, P118): 16 niveles de 347 a 828 |
| 2 | **No reescribir.** Se queda el acta | Δ máx 0,55·σ_col | Con el banco: baja hasta R 207 (P61) y da puntos por debajo de P22, donde hoy manda el ancla |
| 8 | **Escribir ya: recta anclada** `c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 2.04279088E-01`, `c0 = -1.15490317E+02` | Frente a fábrica, Δ hasta 10,4·σ_col; tipo I sesgo +0,7 % y RMS 5,7 % frente a -17,2 / 19,4 de fábrica; pasa `#S` (R(600) = 7,1) y B13; cumple RF-CAL-14/15/16 | El banco no trae tipo I |
| b | **Se puede escribir ya: recta anclada** `c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 3.13845140E-01`, `c0 = -1.77434093E+02`, con conformidad de Diego a RF-CAL-14 | De −70 % a sesgo +3,2 % y RMS 10,8 %; pasa `#S` y B13 | El banco no trae rojo tipo I: no lo mejora |
| 3, 4, 6 | **Esperar al banco** | Hoy la app no los deja ajustar (cobertura) | El banco los hace ajustables hasta grado 2 |
| 5 | **Ni ahora ni con el banco** | Rango 83-102: cobertura insuficiente | Recta anclada sólo con dispensa de Diego |
| 7, a, c, d | Sin cambios | Uno o dos niveles, o rango estrecho | El banco no trae tipo I |

**Orden sugerido, uno cada vez (P9-B8, `tramas:1698`):** 8 y, si Diego da la conformidad, b; cada uno con su re-medida de verificación (RF-CAL-18), P43 o P34 para el 8 y P49 para el b. Los códigos 1 y 2 no se tocan hasta el banco.

## 7. Qué no dice este documento

- Que s_rep = 2,2 % valga para todo el rango: salió de P22, P28 y P4 (x 1400-3300). Para el tipo I (x 600-1150) no hay A5, y 2,2 % de x allí son 13-25 cuentas de una señal de 30-600.
- Que las sesiones sean intercambiables: la mañana fue con firmware 3.6.0 y la A5 tras cambiar la batería. Se han combinado porque es lo que pide el encargo y porque, con s_rep 2,2 %, las diferencias entre sesiones de §2.1 (−1 a −6 %) caben en 2-3 colocaciones.
- Nada de P51-P132: la §5 es una estimación.

