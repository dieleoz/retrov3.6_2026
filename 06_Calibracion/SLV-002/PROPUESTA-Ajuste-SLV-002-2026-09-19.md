# Propuesta de ajuste de SLV-002 — campaña del 19-sep-2026

**Sin validar en hardware y sin escribir nada en el equipo.** Es una propuesta en papel. Los coeficientes son **provisionales hasta P9-A5** (5 colocaciones de P22, P28 y P4, `05_Documentacion/REVISION-Arquitectura-P9-V3.6.md:454`), porque la reproducibilidad medida **entre sesiones** (2-5 %) es del mismo orden que los residuos del ajuste, y la que hay **entre colocaciones dentro de una sesión** está casi sin medir.

Generado por `06_Calibracion/SLV-002/tools/propuesta_ajuste_slv002.py`. No editar a mano: se regenera.

## 0. Datos y trazabilidad

| Qué | Valor |
| :--- | :--- |
| Campaña | `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_103300.zip`, md5 `4c50dbf63d53bd7f53cbd9856a0e5805` (= `HUELLAS.txt`) |
| Dentro del ZIP | `campana.csv` md5 `f9cb68323d5372a3c8835c046de9f96d`; diario md5 `82a24950192a399c0d4dcd31242b3586` (idénticos a los extraídos en `07 pruebas/campana_103300/`) |
| Equipo y firmware | SLV-002, MAC 00:21:13:05:19:3B; firmware 3.6.1 (`#V,3.6,2026-09-19,DEF,0000#`), app 3.6.5 |
| Sesión de la mañana | `03_App_Movil/RetroV36/app/src/test/resources/medidas_SLV-002_20260919_consolidado.csv`, 08:59-09:25, firmware 3.6.0 (`V3.6 2026-09-18`), app 3.6.2. **Primer disparo de cada serie descartado** |
| Código que calcula | Clases de la app en `f7b75c4` (con cambios sin confirmar en el árbol), compiladas con JDK 11 y llamadas desde `tools/CotejoAjusteSLV002.java`: `Campana.leerDiario` + `medidasElegidas` (`Campana.java:497`), `Asistente.puntos` (`Asistente.java:136`), `Ajuste.ajustar` (`Ajuste.java:41`), `Asistente.proponer` (`:280`), `criterioFirmwareS` (`:222`), `validarForma` (`:179`), `Ecuacion.respuestaFloat32` (`Ecuacion.java:56`), `Fabrica.ecuacion` (`Fabrica.java:62`). **El ajuste es el de la app, no una reimplementación** |
| Puntos del ajuste | Media de la serie elegida de cada patrón, como la app: sin evento `ELIGE` en el diario, la **última aceptada** (`Campana.elegida`, `Campana.java:304`). **Para P5 es S024, a 90°** (2459,3); a 0° (S023) da 2456,8. Se da el ajuste con las dos (§2.2) |
| Curva de fábrica | Tabla ROM `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:34-46`, igual a `Fabrica.java:62-88` |
| Criterio de `#S` | `calibracion_v36.c:316-319` (límites), `:407` (`curvaValida`), `:698` (llamada) en el fuente 3.6.2; en la 3.6.1 era `:587-588`. En la app, evaluación float32 en cada `x` entera de 600 a 4300 |
| Oscuro | x ≈ 575 (`ACTA-antes-y-despues-grabacion.md:40`). **Una sola observación, no una serie**: la cota "575-620" del encargo no tiene fuente en la V3.6 (599-634 es del firmware original, `TDD-V3.6.md:525`) |
| Criterios | `05_Documentacion/SPEC-Calibracion-V3.6.md` §5 (RF-CAL-13 `:345`, -14 `:356`, -15 `:362`, -16 `:370`, -17 `:373`), §4.4 `:294` (grado). **Todos propuestos** (P-CAL-01) |

Resultado del diario leído por la app: 56 series, 51 entradas del catálogo con serie elegida (P32 cuenta dos: P32a y P32b).

**Aviso de versión.** En el árbol de trabajo hay cambios **sin confirmar** en `Asistente.java` (otro agente): `comprobarOscuro` (P9-B13), que bloquea toda curva con R(575) > máx(fábrica + 10 ; 25). Este documento usa el código confirmado en `f7b75c4`; en §2 se dice qué curvas bloquearía esa regla si se confirma.

## 1. Reproducibilidad: mañana frente a campaña

Cada fila es una serie de la mañana (sin el primer disparo) frente a la serie aceptada a 0° de la campaña del mismo patrón. Δ = campaña − mañana. Las series de 08:59-09:05 son de 3 disparos (quedan 2).

| Patrón | Tipo | Cert. | Mañana | n | Media mañana | s | Serie campaña | Media campaña | s | Δ (cuentas) | Δ % |
| :--- | :---: | ---: | :---: | ---: | ---: | ---: | :---: | ---: | ---: | ---: | ---: |
| P5 | XI | 740 | 08:59:24 | 2 | 2526,0 | 7,1 | S023 | 2456,8 | 6,9 | -69,2 | -2,74 |
| P8 | XI | 721 | 09:02:18 | 2 | 2517,5 | 4,9 | S033 | 2532,9 | 5,8 | +15,4 | +0,61 |
| P21 | IV | 375 | 09:02:50 | 2 | 1559,0 | 2,8 | S046 | 1562,4 | 6,6 | +3,4 | +0,22 |
| P31 | IX | 573 | 09:03:21 | 2 | 2096,5 | 3,5 | S055 | 2085,8 | 3,5 | -10,7 | -0,51 |
| P9 | XI | 705 | 09:03:50 | 2 | 2556,0 | 9,9 | S034 | 2483,9 | 3,4 | -72,1 | -2,82 |
| P24 | IV | 593 | 09:04:12 | 2 | 2073,5 | 3,5 | S047 | 1982,7 | 5,7 | -90,8 | -4,38 |
| P24 | IV | 593 | 09:04:43 | 2 | 2445,5 | 10,6 | S047 | 1982,7 | 5,7 | -462,8 | -18,93 |
| P10 | XI | 680 | 09:05:04 | 2 | 2461,5 | 16,3 | S035 | 2504,3 | 9,8 | +42,8 | +1,74 |
| P30 | IV | 448 | 09:05:32 | 2 | 1664,0 | 2,8 | S054 | 1654,2 | 5,8 | -9,8 | -0,59 |
| P22 | IV | 334 | 09:12:13 | 8 | 1469,6 | 4,3 | S048 | 1497,0 | 6,2 | +27,4 | +1,86 |
| P21 | IV | 375 | 09:13:14 | 8 | 1552,0 | 3,2 | S046 | 1562,4 | 6,6 | +10,4 | +0,67 |
| P25 | IX | 576 | 09:13:49 | 8 | 2202,0 | 4,7 | S051 | 2157,4 | 5,4 | -44,6 | -2,02 |
| P26 | IX | 583 | 09:14:27 | 8 | 2148,9 | 4,6 | S052 | 2208,8 | 5,3 | +59,9 | +2,79 |
| P23 | XI | 714 | 09:14:57 | 8 | 2723,4 | 5,9 | S050 | 2735,4 | 6,0 | +12,1 | +0,44 |
| P20 | XI | 782 | 09:15:47 | 8 | 2798,1 | 6,0 | S045 | 2787,7 | 8,0 | -10,5 | -0,37 |
| P1 | XI | 762 | 09:16:45 | 8 | 3071,0 | 5,0 | S025 | 2960,9 | 6,7 | -110,1 | -3,59 |
| P2 | IV | 414 | 09:17:21 | 8 | 2015,0 | 5,1 | S026 | 2058,6 | 6,7 | +43,6 | +2,16 |
| P3 | IV | 378 | 09:17:50 | 8 | 1659,9 | 5,6 | S029 | 1674,0 | 4,1 | +14,1 | +0,85 |
| P4 | XI | 828 | 09:18:21 | 8 | 3361,1 | 5,0 | S030 | 3315,8 | 7,5 | -45,3 | -1,35 |
| P6 | XI | 772 | 09:18:59 | 8 | 3115,5 | 6,0 | S031 | 3168,4 | 5,2 | +52,9 | +1,70 |
| P7 | XI | 768 | 09:19:26 (sin el 3031) | 7 | 3230,9 | 4,8 | S032 | 3122,4 | 5,3 | -108,4 | -3,36 |
| P7 | XI | 768 | 09:19:47 | 8 | 3237,0 | 4,5 | S032 | 3122,4 | 5,3 | -114,6 | -3,54 |
| P29 | IV | 442 | 09:22:51 | 8 | 1724,5 | 3,2 | S053 | 1677,0 | 4,8 | -47,5 | -2,75 |
| P20 | XI | 782 | 09:23:40 | 8 | 2804,1 | 5,9 | S045 | 2787,7 | 8,0 | -16,5 | -0,59 |
| P23 | XI | 714 | 09:24:18 | 8 | 2757,2 | 7,3 | S050 | 2735,4 | 6,0 | -21,8 | -0,79 |
| P5 | XI | 740 | 09:25:00 | 8 | 2584,0 | 7,7 | S023 | 2456,8 | 6,9 | -127,2 | -4,92 |

**Resumen, una comparación por patrón** (la última serie de la mañana; 19 patrones; P24 fuera por identidad dudosa, §1.4):

- Δ medio **-0,58 %**, mediana -0,59 %, desviación típica **2,23 %**, de **-4,92 %** (P5) a **+2,79 %** (P26). |Δ| mediano 1,74 %.
- Dentro de la serie, en la campaña: s relativa mediana **0,24 %** (0,14-0,42 %). La dispersión entre sesiones es **9 veces** la de dentro de la serie.
- Signos mezclados en patrones vecinos: P1 -3,6 % y P6 +1,7 % (los dos XI blancos, x ≈ 3000-3170). **No es una deriva común de ganancia** que un `c0` pueda absorber: es un error por patrón.
- Si las dos sesiones pesan igual, cada medida lleva σ ≈ 2,23 / √2 = **1,58 %** (sesión + colocación + deriva). Es la σ que usa la simulación de §2.

### 1.1 Dentro de una misma sesión

| Par (campaña) | Serie A | Media A | Serie B | Media B | Δ % |
| :--- | :---: | ---: | :---: | ---: | ---: |
| P5 0° / 90° | S023 | 2456,8 | S024 | 2459,3 | +0,10 |
| P23 repetida | S049 | 2733,3 | S050 | 2735,4 | +0,08 |
| P37 repetida | S002 | 991,9 | S003 | 992,4 | +0,06 |
| P50 repetida | S021 | 898,7 | S022 | 907,1 | +0,94 |

| Par (mañana) | Hora A | Media A (n) | Hora B | Media B (n) | Δ % |
| :--- | :---: | ---: | :---: | ---: | ---: |
| P5 | 08:59:24 | 2526,0 (2) | 09:25:00 | 2584,0 (8) | +2,30 |
| P21 | 09:02:50 | 1559,0 (2) | 09:13:14 | 1552,0 (8) | -0,45 |
| P23 | 09:14:57 | 2723,4 (8) | 09:24:18 | 2757,2 (8) | +1,24 |
| P20 | 09:15:47 | 2798,1 (8) | 09:23:40 | 2804,1 (8) | +0,21 |
| P7 | 09:19:26 | 3230,9 (7) | 09:19:47 | 3237,0 (8) | +0,19 |

Dentro de la campaña, cuatro pares repiten a **0,1-0,9 %**. Sólo el de P5 es con seguridad una recolocación (el giro obliga a levantar el equipo); en los otros tres el CSV no dice si se levantó. Dentro de la mañana, P5 se movió **+2,3 %** en 26 min y P23 +1,2 % en 9 min. Es decir: **la reproducibilidad entre colocaciones dentro de una sesión está casi sin medir** (un par seguro), y lo que sí está medido es la variación **entre sesiones**, que mezcla colocación, tiempo, temperatura, un ciclo de apagado (`pruebas.txt`, 09:52: el equipo no respondía) y el cambio de firmware 3.6.0 → 3.6.1. El cambio de firmware no toca la ruta de medida (`01_Firmware/RetroVertical_V3.6.X/CAMBIOS-V3.6.md:181,317`), pero no se ha medido que no la toque. **P9-A5 es la medida que separa las dos cosas.**

Tercer dato de P1: a las 08:53 dio 3021,6 (5 disparos, `ACTA-antes-y-despues-grabacion.md:38`), a las 09:16 3071,0 y a las 10:15 2960,9: **3,6 % de recorrido en 80 min** sobre el mismo patrón.

### 1.2 Deriva dentro de la serie

Con `Campana.desvioPorPosicion()` (`Campana.java:601`), desvío medio frente a la mediana de su serie, por posición (el asentamiento ya descartado): `1:-4.0(55) 2:-1.6(55) 3:-0.6(55) 4:-0.9(55) 5:+0.5(55) 6:+1.3(55) 7:+1.2(55) 8:+1.0(55) 9:+3.3(55)`.

**Confirmado** −4,0 en la posición 1 y +3,3 en la 9. **No es monótona**: la 4 (−0,9) queda por debajo de la 3 (−0,6), y la 7 y la 8 por debajo de la 6. Es una rampa de ~7 cuentas (~0,3 % en x ≈ 2500), **diez veces menor** que la variación entre sesiones. Con el mismo número de disparos en campaña, verificación y campo, se la come `c0`.

### 1.3 Qué incertidumbre de colocación hay que asumir

- **Hoy hay que asumir σ ≈ 1,6 % por medida** (una serie de 9 disparos en una colocación), hasta que P9-A5 diga cuánto es colocación y cuánto sesión. Es una cota **por arriba** de la colocación pura.
- En R, con la recta propuesta del código 2 (pendiente 0,318 R/cuenta), un 1,6 % en x = 2500 son 13 unidades de R; en el blanco (0,298) y x = 3000, 14.
- **Criterio propuesto RF-CAL-13, "reproducibilidad ≤ máx(3·s ; 1 %)"** (`SPEC-Calibracion-V3.6.md:345-355`): con s ≈ 5-8 cuentas, 3·s es 0,6-1,2 % y manda el 1 %. **12 de 19** patrones lo incumplen entre sesiones. Dentro de la sesión, los cuatro pares de 1.1 lo cumplen. Por tanto el criterio **no mide la reproducibilidad que importa**: si las dos series son de la misma sesión, lo pasa casi todo; si son de sesiones distintas, rechaza casi todo. Propuesta: definirlo sobre **K colocaciones repartidas en al menos dos sesiones** y con umbral en función de la σ entre colocaciones que mida P9-A5, no de la s de disparo. El 3 % de la app 3.6.7 (`Veredicto.java:71`, `REPRO_MAX`) es un umbral de s **entre colocaciones**, no de diferencia entre dos medias: son magnitudes distintas.

### 1.4 P24

P24 (IV, 593) da ahora **1982,7** (S047). Frente a cada candidata:

| Candidata | Origen | Δ campaña frente a ella | ¿Compatible con −4,9…+2,8 %? |
| :--- | :--- | ---: | :--- |
| 2073,5 | mañana 09:04:12 (2071, 2076; sin el 2048) | -4,4 % | sí, en el borde |
| 2065,0 | cifra citada en `SPEC-Calibracion-V3.6.md:603` (media de los 3 disparos) | -4,0 % | sí, en el borde |
| 2445,5 | mañana 09:04:43 (2438, 2453) | -18,9 % | no |
| 2804,0 | **no es una medida**: caso sintético de `Veredicto.java:26` y `CampanaTest.java:74` | -29,3 % | no |

**La compatible es la de ~2065-2074.** La de 2443 (y la 2804) caen en la zona de los XI amarillos (P5/P9/P10 a 2457-2504; P20 a 2788-2804), a −19 % y −29 %, fuera de toda reproducibilidad medida. Además 1982,7 encaja entre los IV amarillos: P29 (442) 1677, P30 (448) 1654. **P24 entra en el ajuste**, pero su identidad (etiqueta de S047) la confirma Diego, como pide C-40.

## 2. Ajustes propuestos (códigos 1, 2, 8 y b)

Sesgo = media de (R curva − cert)/cert; RMS igual, en %. Residuo = cert − R (convención de `Ajuste.java:17`). "Firmware float32" es `Ecuacion.respuestaFloat32(round(x))`, lo que respondería el equipo. Ninguna curva nueva tiene c3 (`Ajuste.java:86`).

### 2.1 Código 1, blanco intenso (P1-P4, P6, P7, P27, P28)

#### Grado 1 (recomendado)

`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 2.98471545E-01`, `c0 = -1.62263869E+02`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P1 | XI | 762 | 2960,9 | 721,5 | +40,5 | +5,3 | 721 | 773,1 | +1,5 |
| P2 | IV | 414 | 2058,6 | 452,2 | -38,2 | -9,2 | 452 | 605,9 | +46,4 |
| P3 | IV | 378 | 1674,0 | 337,4 | +40,6 | +10,7 | 337 | 491,8 | +30,1 |
| P4 | XI | 828 | 3315,8 | 827,4 | +0,6 | +0,1 | 827 | 800,2 | -3,4 |
| P6 | XI | 772 | 3168,4 | 783,4 | -11,4 | -1,5 | 783 | 791,6 | +2,5 |
| P7 | XI | 768 | 3122,4 | 769,7 | -1,7 | -0,2 | 769 | 788,1 | +2,6 |
| P27 | IX | 471 | 2111,1 | 467,8 | +3,2 | +0,7 | 467 | 619,5 | +31,5 |
| P28 | IX | 484 | 2277,9 | 517,6 | -33,6 | -6,9 | 517 | 659,5 | +36,3 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| IV | 2 | -0,8 / 10,0 | +38,2 / 39,1 | **no cumple** | cumple |
| IX | 2 | +3,1 / 4,9 | +33,9 / 34,0 | cumple | cumple |
| XI | 4 | -0,9 / 2,8 | +0,8 / 2,6 | cumple | **no cumple** |

- RMS 27,44 y error máximo 40,6 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): **no cumplen P3**.
- Criterio de `#S`: **pasa**. R(600) = 16,8.
- C2: **pasa**. Aviso: la curva es negativa hasta x = 543 (por debajo de los patrones): ahí el equipo responderá 0.
- **Oscuro** (x = 575): nueva 9,4 → responde 9; fábrica 24,7. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 35): pasa.
- `Asistente.proponer().escribible()`: **sí**.
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±11 / ±5 / ±6. La curva re-ajustada falla `#S` en **170 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | 9 | 17 | 136 | 285 | 435 | 584 | 733 | 882 | 1032 | 1121 |
| R fábrica | 25 | 38 | 230 | 432 | 590 | 705 | 777 | 806 | 791 | 761 |

#### Grado 2

`c3 = 0.00000000E+00`, `c2 = 5.94632602E-05`, `c1 = -3.65650043E-03`, `c0 = 2.01088395E+02`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P1 | XI | 762 | 2960,9 | 711,6 | +50,4 | +6,6 | 711 | 773,1 | +1,5 |
| P2 | IV | 414 | 2058,6 | 445,5 | -31,5 | -7,6 | 445 | 605,9 | +46,4 |
| P3 | IV | 378 | 1674,0 | 361,6 | +16,4 | +4,3 | 361 | 491,8 | +30,1 |
| P4 | XI | 828 | 3315,8 | 842,7 | -14,7 | -1,8 | 842 | 800,2 | -3,4 |
| P6 | XI | 772 | 3168,4 | 786,5 | -14,5 | -1,9 | 786 | 791,6 | +2,5 |
| P7 | XI | 768 | 3122,4 | 769,4 | -1,4 | -0,2 | 769 | 788,1 | +2,6 |
| P27 | IX | 471 | 2111,1 | 458,4 | +12,6 | +2,7 | 458 | 619,5 | +31,5 |
| P28 | IX | 484 | 2277,9 | 501,3 | -17,3 | -3,6 | 501 | 659,5 | +36,3 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| IV | 2 | +1,6 / 6,2 | +38,2 / 39,1 | **no cumple** | cumple |
| IX | 2 | +0,4 / 3,2 | +33,9 / 34,0 | cumple | cumple |
| XI | 4 | -0,7 / 3,6 | +0,8 / 2,6 | cumple | **no cumple** |

- RMS 24,22 y error máximo 50,4 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): cumplen todos.
- Criterio de `#S`: **pasa**. R(600) = 220,3.
- C2: **pasa**.
- **Oscuro** (x = 575): nueva 218,6 → responde 218; fábrica 24,7. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 35): **la bloquearía**.
- `Asistente.proponer().escribible()`: **sí**.
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±45 / ±5 / ±8. La curva re-ajustada falla `#S` en **12 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | 219 | 220 | 257 | 329 | 432 | 564 | 725 | 917 | 1138 | 1285 |
| R fábrica | 25 | 38 | 230 | 432 | 590 | 705 | 777 | 806 | 791 | 761 |

**Recomendación: grado 1.** El grado 2 baja el RMS de 27,4 a 24,2 (−12 %), por debajo del 30 % que exige la condición 3 de §4.4 (`SPEC-Calibracion-V3.6.md:302-305`); empeora la validación cruzada (P3: +127 frente a +71); con la colocación simulada, R(1000) baila ±45 frente a ±11; y en el oscuro da 219 (B13 del arquitecto, confirmado). El grado 1 da 9 en el oscuro, por debajo de los 25 de fábrica. Pero ojo: la recta pasa `#S` con R(600) = 16,8, y en la simulación falla `#S` en 170 de 2000: **una re-medida puede dar una recta que el firmware rechace**.

### 2.2 Código 2, amarillo intenso (P5, P8-P10, P20-P26, P29-P31)

#### Grado 1 (recomendado)

`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 3.18077266E-01`, `c0 = -9.86378603E+01`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P5 | XI | 740 | 2459,3 | 683,6 | +56,4 | +7,6 | 683 | 798,0 | +7,8 |
| P8 | XI | 721 | 2532,9 | 707,0 | +14,0 | +1,9 | 707 | 810,5 | +12,4 |
| P9 | XI | 705 | 2483,9 | 691,4 | +13,6 | +1,9 | 691 | 802,5 | +13,8 |
| P10 | XI | 680 | 2504,3 | 697,9 | -17,9 | -2,6 | 697 | 806,0 | +18,5 |
| P20 | XI | 782 | 2787,7 | 788,1 | -6,1 | -0,8 | 788 | 829,9 | +6,1 |
| P21 | IV | 375 | 1562,4 | 398,3 | -23,3 | -6,2 | 398 | 475,1 | +26,7 |
| P22 | IV | 334 | 1497,0 | 377,5 | -43,5 | -13,0 | 377 | 443,9 | +32,9 |
| P23 | XI | 714 | 2735,4 | 771,4 | -57,4 | -8,0 | 771 | 829,1 | +16,1 |
| P24 | IV | 593 | 1982,7 | 532,0 | +61,0 | +10,3 | 532 | 657,6 | +10,9 |
| P25 | IX | 576 | 2157,4 | 587,6 | -11,6 | -2,0 | 587 | 719,6 | +24,9 |
| P26 | IX | 583 | 2208,8 | 603,9 | -20,9 | -3,6 | 603 | 735,7 | +26,2 |
| P29 | IV | 442 | 1677,0 | 434,8 | +7,2 | +1,6 | 434 | 528,3 | +19,5 |
| P30 | IV | 448 | 1654,2 | 427,5 | +20,5 | +4,6 | 427 | 517,9 | +15,6 |
| P31 | IX | 573 | 2085,8 | 564,8 | +8,2 | +1,4 | 564 | 695,4 | +21,4 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| IV | 5 | +0,6 / 8,2 | +21,1 / 22,5 | **no cumple** | cumple |
| IX | 3 | +1,4 / 2,5 | +24,2 / 24,2 | cumple | cumple |
| XI | 6 | -0,0 / 4,8 | +12,5 / 13,2 | cumple | cumple |

- RMS 32,17 y error máximo 61,0 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): **no cumplen P22, P24**.
- Criterio de `#S`: **pasa**. R(600) = 92,2.
- C2: **pasa**. Aviso: la curva es negativa hasta x = 310 (por debajo de los patrones): ahí el equipo responderá 0.
- **Oscuro** (x = 575): nueva 84,3 → responde 84; fábrica 20,9. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 31): **la bloquearía**.
- `Asistente.proponer().escribible()`: **sí**.
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±8 / ±3 / ±8. La curva re-ajustada falla `#S` en **0 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | 84 | 92 | 219 | 378 | 538 | 697 | 856 | 1015 | 1174 | 1269 |
| R fábrica | 21 | 30 | 204 | 445 | 664 | 805 | 814 | 635 | 214 | -177 |

#### Grado 2

`c3 = 0.00000000E+00`, `c2 = -1.12835500E-04`, `c1 = 7.97528883E-01`, `c0 = -5.87589520E+02`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P5 | XI | 740 | 2459,3 | 691,3 | +48,7 | +6,6 | 691 | 798,0 | +7,8 |
| P8 | XI | 721 | 2532,9 | 708,6 | +12,4 | +1,7 | 708 | 810,5 | +12,4 |
| P9 | XI | 705 | 2483,9 | 697,2 | +7,8 | +1,1 | 697 | 802,5 | +13,8 |
| P10 | XI | 680 | 2504,3 | 702,0 | -22,0 | -3,2 | 701 | 806,0 | +18,5 |
| P20 | XI | 782 | 2787,7 | 758,8 | +23,2 | +3,0 | 758 | 829,9 | +6,1 |
| P21 | IV | 375 | 1562,4 | 383,0 | -8,0 | -2,1 | 382 | 475,1 | +26,7 |
| P22 | IV | 334 | 1497,0 | 353,4 | -19,4 | -5,8 | 353 | 443,9 | +32,9 |
| P23 | XI | 714 | 2735,4 | 749,7 | -35,7 | -5,0 | 749 | 829,1 | +16,1 |
| P24 | IV | 593 | 1982,7 | 550,1 | +42,9 | +7,2 | 550 | 657,6 | +10,9 |
| P25 | IX | 576 | 2157,4 | 607,8 | -31,8 | -5,5 | 607 | 719,6 | +24,9 |
| P26 | IX | 583 | 2208,8 | 623,5 | -40,5 | -6,9 | 623 | 735,7 | +26,2 |
| P29 | IV | 442 | 1677,0 | 432,5 | +9,5 | +2,1 | 432 | 528,3 | +19,5 |
| P30 | IV | 448 | 1654,2 | 422,9 | +25,1 | +5,6 | 422 | 517,9 | +15,6 |
| P31 | IX | 573 | 2085,8 | 585,0 | -12,0 | -2,1 | 585 | 695,4 | +21,4 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| IV | 5 | -1,4 / 5,0 | +21,1 / 22,5 | cumple | cumple |
| IX | 3 | +4,9 / 5,3 | +24,2 / 24,2 | cumple | cumple |
| XI | 6 | -0,7 / 3,9 | +12,5 / 13,2 | cumple | cumple |

- RMS 27,62 y error máximo 48,7 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): cumplen todos.
- Criterio de `#S`: **NO pasa — la curva se hace negativa en x = 600 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)**. R(600) = -149,7.
- C2: **NO pasa — la curva no es creciente en x = 3535 (techo dentro de 200-4400)**. Aviso: la curva es negativa hasta x = 835 (por debajo de los patrones): ahí el equipo responderá 0.
- **Oscuro** (x = 575): nueva -166,3 → responde 0; fábrica 20,9. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 31): pasa.
- `Asistente.proponer().escribible()`: **no** (la curva no es creciente en x = 3535 (techo dentro de 200-4400) / la curva se hace negativa en x = 600 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)).
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±22 / ±4 / ±12. La curva re-ajustada falla `#S` en **2000 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | -166 | -150 | 97 | 355 | 556 | 701 | 789 | 822 | 797 | 755 |
| R fábrica | 21 | 30 | 204 | 445 | 664 | 805 | 814 | 635 | 214 | -177 |

**Recomendación: grado 1.** El grado 2 no se puede escribir: R(600) = -150 (`#S` lo rechaza) y no es creciente desde x ≈ 3535 (C2). Es la forma de la curva de fábrica, que tampoco pasa `#S` (la curva se hace negativa en x = 4175 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)). **Pega del grado 1 en el oscuro:** 84 frente a 21 de fábrica (confirma B13): una lámina amarilla degradada o el oscuro leerían ~84. Es consecuencia de que el patrón amarillo más bajo esté en x = 1497: por debajo, la recta es extrapolación. **Si se confirma la regla P9-B13 en curso, esta recta tampoco se podrá escribir** (84 > 31).

*Opción fuera del método de la app, sólo para que Diego la valore:* recta anclada en el oscuro, R = k·(x − 575), k = 0,3675 (`c1 = 3.67534027E-01`, `c0 = -2.11332065E+02`). Error por tipo (sesgo / RMS): IV -6,9 / 8,7 %; IX +0,3 / 2,5 %; XI +2,1 / 5,8 %. Peor patrón: P24 -12,8 %, P30 -11,5 %, P23 +11,2 %. R(600) = 9,2: pasaría `#S`, C2 y la regla del oscuro. Empeora el IV y el XI frente a la recta libre; se apoya en un oscuro medido una vez. Es la pregunta que tiene que contestar Diego: **¿qué pesa más, el residuo en los patrones o no leer ~84 en una lámina amarilla muerta?**

**Sensibilidad a P5 (0° frente a 90°):** con S023 (0°) la recta queda `c1 = 3.18114120E-01`, `c0 = -9.86596318E+01`; la diferencia en R es < 0,1 unidades en todo el rango. **Da igual cuál se elija.**

**Sensibilidad a P24 (no es propuesta):** sin P24, `c1 = 3.22971343E-01`, `c0 = -1.14001343E+02`, RMS 28,3 (con P24, 32,2). En x = 2000 la curva cambia de 537,5 a 531,9. P24 se queda: su valor actual es compatible (§1.4).

### 2.3 Código 8, amarillo tipo I (P34, P37, P43, P44)

#### Grado 1 (recomendado)

`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 2.20327236E-01`, `c0 = -1.31863958E+02`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P34 | I | 86 | 958,3 | 79,3 | +6,7 | +7,8 | 79 | 68,0 | -20,9 |
| P37 | I | 82 | 992,4 | 86,8 | -4,8 | -5,9 | 86 | 71,7 | -12,5 |
| P43 | I | 122 | 1151,3 | 121,8 | +0,2 | +0,2 | 121 | 88,0 | -27,8 |
| P44 | I | 64 | 898,6 | 66,1 | -2,1 | -3,3 | 66 | 61,2 | -4,3 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| I | 4 | +0,3 / 5,2 | -16,4 / 18,6 | cumple | cumple |

- RMS 4,26 y error máximo 6,7 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): cumplen todos.
- Criterio de `#S`: **pasa**. R(600) = 0,3.
- C2: **pasa**. Aviso: la curva es negativa hasta x = 598 (por debajo de los patrones): ahí el equipo responderá 0.
- **Oscuro** (x = 575): nueva -5,2 → responde 0; fábrica 13,3. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 25): pasa.
- `Asistente.proponer().escribible()`: **sí**.
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±2 / ±21 / ±41. La curva re-ajustada falla `#S` en **840 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | -5 | 0 | 88 | 199 | 309 | 419 | 529 | 639 | 749 | 816 |
| R fábrica | 13 | 18 | 73 | 126 | 224 | 430 | 809 | 1426 | 2345 | 3068 |

El grado 2 se calculó sólo como control: no pasa `#S` (R(600) = -5,8) ni C2, y deja un grado de libertad. **El grado 1 pasa `#S` por 0,3 unidades** (R(600) = 0,33): la recta corta el cero en x ≈ 598, casi en el oscuro. Es coherente con la física (señal = x − oscuro), pero deja el criterio de `#S` en el filo: con la colocación simulada falla en **840 de 2000**. En el oscuro responde 0, como la fábrica (~0). P37 (82) lee más que P34 (86): 992 frente a 958, un 3,5 %, dentro de la reproducibilidad; nota de campaña de S003: "papel de baja calidad".

### 2.4 Código b, rojo tipo I (P38, P39, P49)

#### Grado 1 (NO escribible)

`c3 = 0.00000000E+00`, `c2 = 0.00000000E+00`, `c1 = 5.52204417E-01`, `c0 = -3.59170158E+02`

| Patrón | Tipo | Cert. | x | R nueva | Residuo (cert − R) | Residuo % | Firmware float32 | R fábrica | Error fábrica % |
| :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| P38 | I | 52 | 743,9 | 51,6 | +0,4 | +0,8 | 51 | 16,0 | -69,3 |
| P39 | I | 46 | 734,3 | 46,3 | -0,3 | -0,7 | 46 | 15,1 | -67,2 |
| P49 | I | 81 | 797,2 | 81,1 | -0,1 | -0,1 | 80 | 21,2 | -73,8 |

| Tipo | n | Nueva: sesgo / RMS (%) | Fábrica: sesgo / RMS (%) | RF-CAL-15 (sesgo ≤ 5, RMS ≤ 6) | RF-CAL-16 (RMS nueva < fábrica) |
| :---: | ---: | :--- | :--- | :---: | :---: |
| I | 3 | +0,0 / 0,6 | -70,1 / 70,2 | cumple | cumple |

- RMS 0,30 y error máximo 0,4 unidades de R (`Ajuste.Resultado`).
- RF-CAL-14 (|residuo| ≤ máx(10 % ; 2)): cumplen todos.
- Criterio de `#S`: **NO pasa — la curva se hace negativa en x = 600 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)**. R(600) = -27,8.
- C2: **pasa**. Aviso: la curva es negativa hasta x = 650 (por debajo de los patrones): ahí el equipo responderá 0.
- **Oscuro** (x = 575): nueva -41,7 → responde 0; fábrica 3,6. Con la regla P9-B13 en curso (R ≤ máx(fábrica + 10 ; 25) = 25): pasa.
- `Asistente.proponer().escribible()`: **no** (la curva se hace negativa en x = 600 (el firmware 3.6.1 rechaza #S con #ERR,FORMATO#)).
- Colocación simulada (σ = 1,58 % en cada x, 2000 repeticiones, `Ajuste.ajustar`): dispersión de R en x = 1000 / 2000 / 3000: ±46 / ±232 / ±419. La curva re-ajustada falla `#S` en **1869 de 2000**.

| x | 575 | 600 | 1000 | 1500 | 2000 | 2500 | 3000 | 3500 | 4000 | 4300 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| R nueva | -42 | -28 | 193 | 469 | 745 | 1021 | 1297 | 1574 | 1850 | 2015 |
| R fábrica | 4 | 5 | 47 | 150 | 310 | 527 | 799 | 1129 | 1515 | 1774 |

**No se puede escribir.** R(600) = -27,8: `#S` lo rechaza y `Asistente.proponer` lo bloquea. El motivo de fondo: los tres patrones ocupan sólo **63 cuentas de x** (734-797), unas 5 veces la σ de una medida en esa zona (±12 cuentas). La pendiente queda mal determinada: con la colocación simulada, R(1000) = 194 ± 46, y la recta re-ajustada falla `#S` en 1869 de 2000. El residuo casi nulo (RMS 0,30) no significa nada con 3 puntos y 2 parámetros.

*Opción fuera del método de la app, sólo para que Diego la valore:* recta anclada en el oscuro, R = k·(x − 575), k por mínimos cuadrados = 0,3302 (`c1 = 3.30239821E-01`, `c0 = -1.89887897E+02`). Error: P38 +7 %, P39 +14 %, P49 -9 %. R(600) = 8,3: pasaría `#S`. Depende de un oscuro medido una sola vez; **no se propone escribirla** sin medir el oscuro en serie.

## 3. Códigos que sólo se verifican (curva de fábrica, sin ajustar)

x − oscuro con oscuro = 575. σ_x = 1,6 % de x (§1.3). σ_R = pendiente de fábrica × σ_x.

| Código | Patrón | Tipo | Cert. | x | x − oscuro | R fábrica | Firmware | Error % | σ_R |
| :---: | :--- | :---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | P13 | XI | 168 | 830,9 | 256 | 137,1 | 137 | -18 | 3,8 |
| 3 | P14 | XI | 173 | 849,1 | 274 | 142,3 | 142 | -18 | 3,8 |
| 3 | P15 | XI | 164 | 843,2 | 268 | 140,6 | 140 | -14 | 3,8 |
| 3 | P16 | XI | 170 | 846,7 | 272 | 141,6 | 141 | -17 | 3,8 |
| 4 | P11 | IV | 194 | 1722,9 | 1148 | 238,9 | 238 | +23 | 2,4 |
| 4 | P12 | IV | 227 | 1761,9 | 1187 | 242,5 | 242 | +7 | 2,6 |
| 5 | P17 | XI | 83 | 828,4 | 253 | 37,8 | 37 | -54 | 1,6 |
| 5 | P18 | XI | 84 | 813,4 | 238 | 35,9 | 35 | -57 | 1,6 |
| 5 | P19 | XI | 85 | 823,6 | 249 | 37,2 | 37 | -56 | 1,6 |
| 7 | P35 | I | 96 | 1114,7 | 540 | 96,8 | 96 | +1 | 3,7 |
| a | P40 | I | 6 | 624,7 | 50 | 60,3 | 60 | +905 | 4,5 |
| a | P45 | I | 7 | 615,6 | 41 | 56,1 | 56 | +701 | 4,5 |
| c | P32a | I | 9 | 630,9 | 56 | 14,7 | 14 | +63 | 1,1 |
| c | P36 | I | 10 | 638,8 | 64 | 15,5 | 15 | +55 | 1,1 |
| c | P42 | I | 7 | 592,6 | 18 | 10,5 | 10 | +50 | 1,0 |
| c | P46 | I | 9 | 644,3 | 69 | 16,1 | 16 | +79 | 1,1 |
| c | P47 | I | 9 | 606,3 | 31 | 12,0 | 11 | +33 | 1,0 |
| c | P48 | I | 9 | 605,4 | 30 | 11,9 | 11 | +32 | 1,0 |
| d | P32b | I | 68 | 802,8 | 228 | 38,3 | 38 | -44 | 1,9 |
| d | P33 | I | 72 | 849,8 | 275 | 45,4 | 45 | -37 | 2,1 |
| d | P41 | I | 73 | 898,7 | 324 | 53,2 | 53 | -27 | 2,3 |
| d | P50 | I | 71 | 907,1 | 332 | 54,6 | 54 | -23 | 2,4 |

¿Distingue el equipo un patrón de otro dentro de cada código?

- **3.** Verde intenso: x 831-849 para 164-173. Separación máxima de 18 cuentas frente a σ_x ≈ 13 (σ_x·√2 ≈ 19 para una diferencia): **no distingue** 164 de 173, y P14 (173) y P15 (164) salen al revés. Sí está claramente fuera del oscuro (~260 cuentas). La fábrica lee −14/−18 %.
- **4.** Rojo intenso: P11 (194) 1723 y P12 (227) 1762. Separación de 39 cuentas frente a σ_x·√2 ≈ 39: **no se distinguen con una sola medida de cada uno**. La fábrica lee +23 % en P11 y +7 % en P12.
- **5.** Azul intenso: x 813-828 para 83-85, un nivel en la práctica: **no distingue** y P18 (84) sale el más bajo. ~240 cuentas sobre el oscuro. La fábrica lee −55/−57 %: **fuera** con cualquier tolerancia.
- **7.** Blanco tipo I, P35: 96 → fábrica 96,8 (+1 %). Un solo patrón.
- **a.** Verde tipo I, P40 (6) 625 y P45 (7) 616: 40-50 cuentas sobre el oscuro, ~5 σ_x. La fábrica lee **56-60 donde el certificado dice 6-7**: la curva del verde (idéntica a la del intenso, `Fabrica.java:69-70`) en esta zona no sirve. Señal insuficiente para calibrar.
- **c.** Azul tipo I: x 593-644 para 7-10, **18-70 cuentas sobre un oscuro medido una vez**. P46, P47 y P48, los tres de 9, dan 644, 606 y 605: 39 cuentas de dispersión entre patrones iguales, más que lo que separa 7 de 10 (P42 593, P36 639). **No distingue un valor de otro**; apenas distingue el patrón del oscuro. La fábrica lee +32/+79 %, que en unidades son 3-7.
- **d.** Naranja tipo I: x 803-907 para 68-73, 230-330 cuentas sobre el oscuro; P50 (71) lee más que P41 (73) y P33 (72) menos que ambos. **No distingue** 68 de 73: 104 cuentas de dispersión para 5 unidades. La fábrica lee −23/−44 %.

Conclusión: con la reproducibilidad medida, verde, azul y los opacos bajos **tienen señal para decir "hay lámina"** (salvo c, al límite), **pero no para separar valores de 1-9 unidades**. Verificar sí; ajustar, no, y la app ya lo impide por cobertura (`Asistente.java:88-96`).

## 4. Validación cruzada dejando uno fuera (códigos 1 y 2)

Con `Ajuste.ajustar` sobre los demás patrones; error = cert − predicción. Umbral RF-CAL-17: 1,5 × máx(10 % ; 2) = 15 %.

**Código 1, grado 1:** P1 +6,5 %, P2 -11,9 %, P3 +18,8 %, P4 +0,1 %, P6 -2,0 %, P7 -0,3 %, P27 +0,8 %, P28 -8,3 %. RMS de predicción 8,7 %. **Fuera: P3.**

**Código 1, grado 2:** P1 +8,8 %, P2 -10,3 %, P3 +33,7 %, P4 -3,5 %, P6 -2,5 %, P7 -0,2 %, P27 +3,7 %, P28 -5,6 %. RMS de predicción 13,2 %. **Fuera: P3.**

**Código 2, grado 1:** P5 +8,5 %, P8 +2,2 %, P9 +2,2 %, P10 -3,0 %, P20 -1,0 %, P21 -8,0 %, P22 -17,4 %, P23 -10,1 %, P24 +11,2 %, P25 -2,2 %, P26 -3,9 %, P29 +2,0 %, P30 +5,5 %, P31 +1,5 %. RMS de predicción 7,3 %. **Fuera: P22.**

**Código 2, grado 2:** P5 +7,5 %, P8 +2,0 %, P9 +1,3 %, P10 -3,7 %, P20 +5,4 %, P21 -3,0 %, P22 -9,7 %, P23 -7,4 %, P24 +8,7 %, P25 -6,7 %, P26 -8,4 %, P29 +2,6 %, P30 +6,8 %, P31 -2,6 %. RMS de predicción 6,1 %. Todos dentro. Predice mejor que la recta, pero no se puede escribir (§2.2).

Para 8 y b la SPEC dice que no tiene sentido (`SPEC-Calibracion-V3.6.md:379`). Como dato: en el 8, grado 1, P34 +11 %, P37 -8 %, P43 +2 %, P44 -7 %.

## 5. Veredicto por código

Con los criterios **propuestos** de la SPEC §5, que Diego no ha aprobado (P-CAL-01).

| Código | Veredicto | Por qué | Patrones anómalos (ninguno quitado) |
| :---: | :--- | :--- | :--- |
| 1 | **Ajustar, grado 1, tras P9-A5 y con dispensa expresa de Diego** | Pasa `#S` (R(600) = 17), C2 y el oscuro (9 frente a 25 de fábrica). Baja el RMS de IV de 39 a 10 % y el de IX de 34 a 5 %. **Incumple tres criterios propuestos:** RF-CAL-14 (P3, +10,7 %), RF-CAL-15 (RMS de IV 10,0 %) y RF-CAL-16 en XI (2,8 % frente a 2,6 % de fábrica, diferencia muy por debajo de la reproducibilidad). Con la letra de RF-CAL-16, "no se escribe" | **P2 y P3** (IV) se contradicen entre sí: 36 unidades de certificado y 385 cuentas de x, cuando la recta da 0,3 R/cuenta; P2 (414) lee casi lo que P27 (471). **P28**, −6,9 % |
| 2 | **Ajustar, grado 1, tras P9-A5; antes, decisión de Diego sobre el oscuro** | Pasa `#S` y C2; mejora a la fábrica en los tres tipos (sesgo IV +21 → +0,6 %, IX +24 → +1,4 %, XI +13 → 0 %). **Incumple RF-CAL-14 en P22 (−13 %) y P24 (+10,3 %), RF-CAL-15 en IV (RMS 8,2 %) y RF-CAL-17 en P22.** Lee **84 en el oscuro** frente a 21 (B13): la regla en curso la bloquearía. Alternativa: la recta anclada (§2.2) | **P22**, **P24** (identidad sin confirmar), **P23** (−8 %) y **P5** (+7,6 %): los XI amarillos no se ordenan por certificado |
| 8 | **Ajustar, grado 1, tras P9-A5** | Cumple RF-CAL-14/15/16 (tipo I, RMS 5,2 % frente a 18,6 % de fábrica). Pasa `#S` por 0,3 unidades: una re-medida puede dar una recta que el firmware rechace | P37 (lee más que P34 con menos certificado; dentro de la reproducibilidad) |
| b | **Dejar fábrica y marcar "fuera"** | La recta no pasa `#S` y su pendiente está mal determinada (63 cuentas de rango). La fábrica lee −67/−74 %. Hace falta un rojo tipo I de R más alto, o decidir la recta anclada en el oscuro tras medir el oscuro | — |
| 3, 4, 5 | Sólo verificar | Rango o niveles insuficientes (`Asistente.cobertura`). Fábrica: verde −14/−18 %, rojo +7/+23 %, **azul −55/−57 %: fuera** | P14/P15 invertidos; P18 |
| 7 | Sólo verificar | Fábrica +1 % | — |
| a | Sólo verificar; **fuera** | Fábrica lee 56-60 para 6-7 | — |
| c | Sólo verificar | Señal apenas por encima del oscuro; fábrica +32/+79 % (3-7 unidades) | P46 (644 frente a 605 de sus iguales) |
| d | Sólo verificar; **fuera** | Fábrica −23/−44 % | P32b (identidad de P32 pendiente, `ROADMAP.md:40-41`) |
| 6 | Sin patrones | Queda la fábrica sin verificar | — |

## 6. Recomendación de método para la medida final

**K = 5 colocaciones por patrón (levantar y volver a apoyar), repartidas en 2 sesiones (3 + 2, con apagado y calentamiento entre ellas), M = 4 disparos por colocación tras el de asentamiento.** Para los códigos 1, 2 y 8: 26 patrones × 5 = 130 colocaciones × 5 disparos = 650 disparos. La campaña del 19-sep hizo 55 series de 10 disparos (550) en 36 min (09:57-10:33): del orden de **1 h en total**, más el manejo entre colocaciones.

- **Por qué K y no M:** la s de disparo es ~0,24 % y la variación entre medidas ~1,6 %. Con M = 4 en vez de 9, el error de disparo de la media de una colocación pasa de 0,08 a 0,12 %, invisible al lado de la colocación; cada colocación nueva, en cambio, divide la parte grande. σ de la media: K = 3 → 0,91 %; **K = 5 → 0,71 %**; K = 9 → 0,53 %. K = 5 deja la incertidumbre de cada punto por debajo del 1 %, por debajo del RMS de XI que el ajuste quiere juzgar (2,8-4,8 %); K = 9 casi dobla el trabajo para ganar 0,2 puntos. Con K = 5 la misma cuenta de la app (s entre colocaciones, `Veredicto.sEntre`) tiene 4 grados de libertad, el mínimo razonable para que su umbral signifique algo.
- **Por qué en dos sesiones:** lo medido es variación entre sesiones; si todas las colocaciones son de la misma, la media hereda el error de esa sesión entero y la s entre colocaciones lo esconde (§1.1: 0,1-0,9 % dentro, 2-5 % entre). Si P9-A5 demuestra que la colocación sola ya da el 2-4 %, basta una sesión con K = 5.
- **M = 4 y no 3:** el rechazo de descolgados sólo actúa desde 4 disparos (RF-CAL-04, `SPEC-Calibracion-V3.6.md`, §3). Con el 3 × 3 por defecto de la 3.6.7 un disparo como el 3031 de P7 entraría en la media. Y el mismo M en campaña, verificación y campo, por la rampa de §1.2.
- Encaja con la app 3.6.7 (K × M configurables, 3 × 3 por defecto, `CampanaActivity.java:66-76`) cambiando a 5 × 4. **La 3.6.7 no está revisada** (nota de f7b75c4 en `REVISION-Arquitectura-P9-V3.6.md`): hasta que lo esté, esto es procedimiento, no una propiedad de la app.
- **Medir el oscuro en serie** (K = 5, tapa opaca) en cada sesión: lo necesita la decisión de B13 y la del código b.
- **Leer en voz alta la etiqueta** de P24, P32a/P32b y de los XI amarillos (L-18).
- El criterio de aceptación de cada patrón, en lugar de RF-CAL-13 actual: s entre colocaciones ≤ 3 % (el `REPRO_MAX` de la app) **y** diferencia entre las medias de las dos sesiones ≤ 2·√2·σ_col con la σ que dé P9-A5.

## 7. Hallazgos del principal: confirmados y desmentidos

| Afirmación | Resultado | Evidencia |
| :--- | :--- | :--- |
| P5 a 0° 2456,8 y a 90° 2459,3 (0,1 %); orientación refutada para P5 | **Confirmado**, con matiz: la diferencia (2,6 cuentas) es 0,8 veces su error típico. Refuta 0/90° en P5, no 180/270° ni los demás XI | S023, S024 |
| Reproducibilidad entre sesiones de −3,6 a +2,8 % | **Desmentido en el extremo:** llega a **-4,9 %** (P5, serie de 09:25) y a −4,4 % en P24 frente a su serie de 2065-2074 | §1 |
| Ejemplos P1 3071→2961, P7 3234→3122, P26 2149→2209, P29 1725→1677 | **Confirmados** (P7 3234 es la media de sus dos series de la mañana, 3230,9 y 3237,0) | §1 |
| Mucho mayor que la s de serie (0,2-0,3 %) | **Confirmado**, ~9 veces | §1 |
| Deriva −4 en la posición 1 y +3,3 en la 9, **monótona** | Cifras **confirmadas**; **monótona, no** (la 4 < la 3; la 7 y la 8 < la 6) | `Campana.desvioPorPosicion` |
| P24 = 1982,7; candidatas 2065, 2443 y 2804 | Compatible **2065** (−4,0 %). **2804 no es una medida de P24**: es el caso sintético de `Veredicto.java:26` y `CampanaTest.java:74` | §1.4 |
| XI amarillos sin ordenar; blancos XI ordenados | **Confirmado** los dos. En amarillo, P23 (714) lee 2735, un 11 % más que P5 (740): fuera de la reproducibilidad, así que no es sólo colocación | §2.2 |
| Reproducibilidad entre colocaciones 2-4 % (arquitecto, r2) | Lo medido es **entre sesiones**. Dentro de la sesión hay un solo par seguro (P5, 0,1 %) | §1.1 |
| Código b ajustable en grado 1 (`ROADMAP.md:49-50`, `SPEC-Calibracion-V3.6.md` §2.2) | **Desmentido en la práctica:** la cobertura lo permite, pero la recta que sale da R(600) = −28 y la app no la deja escribir | §2.4 |

## Anexo. Informe del asistente de la app, tal cual (grado 1 de los códigos 1, 2 y 8)

```
Código 1 (blanco intenso), grado 1
Catálogo: 1 blanco intenso: 8 patrones, 8 niveles, R 378-828 -> ajustable hasta grado 2
Nuevos: c3=0.00000000E+00 c2=0.00000000E+00 c1=2.98471545E-01 c0=-1.62263869E+02
patrón  tipo  cert      x     n  R nueva  resid   resid%  R actual  desvío actual
P1     XI     762  2960.9   9   721.5  +40.5    +5.3%    773.1    +11.1
P2     IV     414  2058.6   9   452.2  -38.2    -9.2%    605.9   +191.9
P3     IV     378  1674.0   9   337.4  +40.6   +10.7%    491.8   +113.8
P4     XI     828  3315.8   9   827.4   +0.6    +0.1%    800.2    -27.8
P6     XI     772  3168.4   9   783.4  -11.4    -1.5%    791.6    +19.6
P7     XI     768  3122.4   9   769.7   -1.7    -0.2%    788.1    +20.1
P27    IX     471  2111.1   9   467.8   +3.2    +0.7%    619.5   +148.5
P28    IX     484  2277.9   9   517.6  -33.6    -6.9%    659.5   +175.5
Error máximo 40.6, RMS 27.44 (unidades de R)
Rango medido: x 1674-3316. Fuera de él la curva es EXTRAPOLACIÓN:
     x  R actual  R nueva
   500     -14.8    -13.0 extrapolado
  1000     230.1    136.2 extrapolado
  2000     590.2    434.7 
  3000     777.1    733.2 
  4000     791.0   1031.6 extrapolado
Aviso: mezcla tipos de lámina [XI, IV, IX] en una sola curva (P-06)
Aviso: la curva es negativa hasta x = 543 (por debajo de los patrones): ahí el equipo responderá 0
```

```
Código 2 (amarillo intenso), grado 1
Catálogo: 2 amarillo intenso: 14 patrones, 14 niveles, R 334-782 -> ajustable hasta grado 2
Nuevos: c3=0.00000000E+00 c2=0.00000000E+00 c1=3.18077266E-01 c0=-9.86378603E+01
patrón  tipo  cert      x     n  R nueva  resid   resid%  R actual  desvío actual
P5     XI     740  2459.3   9   683.6  +56.4    +7.6%    798.0    +58.0
P8     XI     721  2532.9   9   707.0  +14.0    +1.9%    810.5    +89.5
P9     XI     705  2483.9   9   691.4  +13.6    +1.9%    802.5    +97.5
P10    XI     680  2504.3   9   697.9  -17.9    -2.6%    806.0   +126.0
P20    XI     782  2787.7   9   788.1   -6.1    -0.8%    829.9    +47.9
P21    IV     375  1562.4   9   398.3  -23.3    -6.2%    475.1   +100.1
P22    IV     334  1497.0   9   377.5  -43.5   -13.0%    443.9   +109.9
P23    XI     714  2735.4   9   771.4  -57.4    -8.0%    829.1   +115.1
P24    IV     593  1982.7   9   532.0  +61.0   +10.3%    657.6    +64.6
P25    IX     576  2157.4   9   587.6  -11.6    -2.0%    719.6   +143.6
P26    IX     583  2208.8   9   603.9  -20.9    -3.6%    735.7   +152.7
P29    IV     442  1677.0   9   434.8   +7.2    +1.6%    528.3    +86.3
P30    IV     448  1654.2   9   427.5  +20.5    +4.6%    517.9    +69.9
P31    IX     573  2085.8   9   564.8   +8.2    +1.4%    695.4   +122.4
Error máximo 61.0, RMS 32.17 (unidades de R)
Rango medido: x 1497-2788. Fuera de él la curva es EXTRAPOLACIÓN:
     x  R actual  R nueva
   500      -6.5     60.4 extrapolado
  1000     203.6    219.4 extrapolado
  2000     664.2    537.5 
  3000     813.8    855.6 extrapolado
  4000     214.4   1173.7 extrapolado
Aviso: mezcla tipos de lámina [XI, IV, IX] en una sola curva (P-06)
Aviso: la curva es negativa hasta x = 310 (por debajo de los patrones): ahí el equipo responderá 0
```

```
Código 8 (amarillo opaco), grado 1
Catálogo: 8 amarillo opaco: 4 patrones, 4 niveles, R 64-122 -> ajustable hasta grado 2
Nuevos: c3=0.00000000E+00 c2=0.00000000E+00 c1=2.20327236E-01 c0=-1.31863958E+02
patrón  tipo  cert      x     n  R nueva  resid   resid%  R actual  desvío actual
P34    I       86   958.3   9    79.3   +6.7    +7.8%     68.0    -18.0
P37    I       82   992.4   9    86.8   -4.8    -5.9%     71.7    -10.3
P43    I      122  1151.3   9   121.8   +0.2    +0.2%     88.0    -34.0
P44    I       64   898.6   9    66.1   -2.1    -3.3%     61.2     -2.8
Error máximo 6.7, RMS 4.26 (unidades de R)
Rango medido: x 899-1151. Fuera de él la curva es EXTRAPOLACIÓN:
     x  R actual  R nueva
   500      -1.7    -21.7 extrapolado
  1000      72.5     88.5 
  2000     224.0    308.8 extrapolado
  3000     809.5    529.1 extrapolado
  4000    2344.9    749.4 extrapolado
Aviso: la curva es negativa hasta x = 598 (por debajo de los patrones): ahí el equipo responderá 0
```
