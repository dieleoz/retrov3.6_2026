# Banco representativo de patrones y ZIP más ligero

**Nada de esto se ha medido.** Es un cálculo en papel sobre el catálogo, la cola completa y los ZIP
del 19-sep-2026. De los 133 patrones, 83 no se han medido nunca: su `x` es una estimación
(`PLAN-Captura-Banco-P1-P132.md` §5.5), y en el naranja intenso sale de la curva de fábrica invertida,
que falla hasta un 40 %. **La app de hoy no carga ninguna de las dos colas nuevas**: rechaza todo md5
que no esté en su lista (§5). Para cargarlas hay que tocar el código.

Encargo de Diego: "son 133 patrones pero no todos son representativos; muchos son iguales, como P47 y
P48, idénticos, con P46; optimiza el ZIP de la app, pues son muchas muestras".

| | Banco completo | **Banco representativo** | Verificación anual |
| :--- | ---: | ---: | ---: |
| Patrones | 133 | **58** | 12 |
| Puntos de ajuste (AJUSTE + RE-MEDIDA) | 98 | **43** | 0 (no calibra) |
| Sesiones | 3 | **2** | 1 |
| Tiempo estimado | 196 min (el plan decía 195) | **105 min** (47 + 58) | 38 min |
| Disparos con `e` | 2575 | **1310** | 360 |
| Cola | `cola_banco_P1-P132.csv` (md5 `9ddb7882…`) | `cola_banco_representativo.csv` (md5 `2e266bbf89ec7dfb716e5ddd2d59d8f4`) | `cola_verificacion_anual.csv` (md5 `d86eddf7fbdfdd4ae11ee2080d220e6c`) |

md5 del fichero con LF, que es como queda el blob en git.

**Lo que se pierde:** en el centro del rango, la σ de la curva pasa del 1,3-2,2 % al 2,3-3,6 %. En los
extremos llega al 5-6,5 % en los códigos 1, 3, 4 y 6 (§4). Además, el banco representativo **no
resuelve** las contradicciones abiertas, que sólo cierra el completo (§4.3). **Recomendación:** medir el
banco completo **una vez** para caracterizar el catálogo, y usar el representativo en las
recalibraciones y la verificación anual entre una y otra.

**ZIP:** el de las 12:27 pesa 80,6 kB. El 44 % se repite byte a byte del ZIP anterior, y las tramas
son el 64 %. Con un ZIP incremental sin tramas ni `campana.csv` queda en **7,6 kB**, y todo lo demás va
en el ZIP de soporte (§6).

Scripts, que regeneran todo lo de este documento:

- `06_Calibracion/tools/banco_representativo.py`: grupos, colas, tiempos, cobertura e incertidumbre.
  Llama a `tools/CoberturaCola.java`, que compila el código de la app de `HEAD` (9c970d3, con
  `git archive`) con el JDK 11.
- `06_Calibracion/tools/medir_zip_campana.py`: tamaños y simulaciones del ZIP.
- `06_Calibracion/grupos_patrones_equivalentes.csv`: la tabla de §2, en CSV.

---

## 1. Datos y criterio

| Dato | Valor | Fuente |
| :--- | :--- | :--- |
| s_rep por colocación | 2,2 % (medida: 2,24 %) | A5 del 19-sep, `resumen.txt:191` del ZIP de las 12:27; REFORM §2.1 |
| Dispersión dentro de la serie | ~0,24 % | `PROPUESTA-Ajuste-SLV-002-2026-09-19.md` §6. No limita nada: domina la colocación |
| `x` del oscuro | 565,4 | `resumen.txt:122` |
| `x` de cada patrón | `x_esperada` de la cola completa: medida en P1-P50, estimada en P51-P132 | `cola_banco_P1-P132.csv`; PLAN §5.5 |
| Regla de cobertura | 3 niveles para grado 1; rango ≥ 20 unidades y ≥ 30 % del mayor; el ancla cuenta como un nivel | `Asistente.java:75-98` y `:101`; `Asistente.proponer` para la anclada |
| Patrones que entran en el ajuste | AJUSTE y RE-MEDIDA de la cola. Si el código no tiene ningún AJUSTE, todos los suyos | `FlujoCalibracion.java:287-302` (HEAD) |
| Método y re-medida por código | Tabla RF-CAL-37: 1 P28, 2 P25, 3 P123, 4 P11, 5 P81, 6 P86, 8 P43, b P49 | `TablaCalibracion.java:134-165` |
| P81 | Fuera del ajuste del 5; se queda sólo como verificación y re-medida | `SLV-002/DECISIONES-Diego-2026-09-19.md`, fila P81 (c836cae) |
| Tiempo | 1,5 s por disparo, 11 s por recolocación y 20 s por cambio de patrón; 10 s por lectura de batería y 20 s por exportación | PLAN §2. Con estas cifras, la cola completa da 196,2 min frente a los 195 del plan |

**Resolución útil del equipo en la zona de un patrón.** Es el cambio de R certificado que separa dos
medias de K = 3 colocaciones a 2σ:

  ΔR = máx(1 ; 2·√(2/K)·s_rep·b·x), con b = R/(x − 565,4)

- b es la pendiente local de la recta que pasa por el oscuro.
- 1 unidad es la resolución de salida del firmware.
- Queda ΔR ≈ 0,051·R·x/(x − 565,4): cuanto más cerca del oscuro, peor.
  - Blanco XI, en x ≈ 3100: ΔR ≈ 34 unidades.
  - Azul tipo I, en x ≈ 606: ΔR ≈ 5,5 unidades con R = 9. Un certificado de 7 a 10 no se distingue.

**Dos patrones son equivalentes** si son del mismo código, color y tipo, y su R difiere menos que ΔR.
Café y lila se miden con el 4, pero forman grupos propios.

**Distinguir no es lo mismo que sobrar.** Dos patrones equivalentes dan la misma información de
*nivel*. Cada uno aporta, sin embargo, una muestra más de la dispersión entre láminas, que es lo que
de verdad manda en la curva (REFORM §3: σ_pat es de 3 a 4 veces σ_col). Ese es el precio de §4.

## 2. Patrones equivalentes: 133 patrones en 76 grupos

"—": el grupo no tiene representante en el banco representativo.

| Grupo | Cód. | Color | Tipo | Patrones (R cert.) | ΔR útil | Representante(s) |
| :--- | :---: | :--- | :---: | :--- | ---: | :--- |
| G01 | 1 | blanco | IV | P56 (347) | 18.6 | P56 (AJUSTE) |
| G02 | 1 | blanco | IV | P3 (378) | 20.5 | — |
| G03 | 1 | blanco | IV | P2 (414), P52 (421), P58 (432) | 20.5 | — |
| G04 | 1 | blanco | IV | P51 (439) | 21.9 | P51 (AJUSTE) |
| G05 | 1 | blanco | IX | P27 (471), P28 (484) | 23.1 | P28 (RE-MEDIDA) |
| G06 | 1 | blanco | IX | P118 (508) | 24.4 | P118 (VERIFICACION) |
| G07 | 1 | blanco | IX | P73 (614) | 28.2 | P73 (AJUSTE) |
| G08 | 1 | blanco | XI | P1 (762), P103 (763), P7 (768), P6 (772), P88 (773) | 33.8 | P7 (AJUSTE) |
| G09 | 1 | blanco | XI | P4 (828) | 35.9 | P4 (AJUSTE) |
| G10 | 2 | amarillo | IV | P61 (207) | 14.9 | P61 (AJUSTE) |
| G11 | 2 | amarillo | IV | P62 (280) | 17.5 | — |
| G12 | 2 | amarillo | IV | P22 (334) | 19.3 | — (se mide en la A5) |
| G13 | 2 | amarillo | IV | P21 (375) | 21.1 | P21 (VERIFICACION) |
| G14 | 2 | amarillo | IV | P29 (442), P30 (448) | 24.0 | P30 (AJUSTE) |
| G15 | 2 | amarillo | IV | P24 (593) | 29.8 | — (C-40) |
| G16 | 2 | amarillo | IX | P121 (320) | 18.9 | P121 (AJUSTE) |
| G17 | 2 | amarillo | IX | P122 (352) | 20.1 | — |
| G18 | 2 | amarillo | IX | P77 (391) | 21.5 | — |
| G19 | 2 | amarillo | IX | P76 (433) | 23.0 | — |
| G20 | 2 | amarillo | IX | P31 (573), P25 (576), P26 (583) | 28.2 | P25 (RE-MEDIDA) |
| G21 | 2 | amarillo | XI | P107 (478), P106 (493) | 24.6 | P106 (AJUSTE) |
| G22 | 2 | amarillo | XI | P91 (516), P92 (522) | 26.0 | — |
| G23 | 2 | amarillo | XI | P10 (680), P9 (705) | 31.6 | — |
| G24 | 2 | amarillo | XI | P23 (714), P8 (721), P5 (740) | 32.3 | — (C-39) |
| G25 | 2 | amarillo | XI | P20 (782) | 35.2 | P20 (AJUSTE) |
| G26 | 3 | verde | IV | P65 (51), P64 (54) | 14.2 | P64 (AJUSTE) |
| G27 | 3 | verde | IV | P63 (121) | 16.7 | P63 (AJUSTE) |
| G28 | 3 | verde | IX | P124 (51), P125 (51), P79 (51), P80 (51) | 14.2 | P124 (AJUSTE) |
| G29 | 3 | verde | IX | P78 (97) | 15.8 | P78 (VERIFICACION) |
| G30 | 3 | verde | IX | P123 (115) | 16.5 | P123 (RE-MEDIDA) |
| G31 | 3 | verde | XI | P109 (51), P110 (51), P94 (51), P95 (51) | 14.2 | P109 (AJUSTE) |
| G32 | 3 | verde | XI | P93 (85), P108 (96) | 15.4 | — |
| G33 | 3 | verde | XI | P15 (164), P13 (168), P16 (170), P14 (173) | 17.9 | P16 (AJUSTE) |
| G34 | 4 | café | IV | P68 (36) | 4.9 | P68 (VERIFICACION) |
| G35 | 4 | café | IX | P128 (38) | 5.0 | — |
| G36 | 4 | café | IX | P83 (55) | 5.6 | — |
| G37 | 4 | café | XI | P113 (60) | 5.8 | — |
| G38 | 4 | café | XI | P98 (68) | 6.1 | P98 (VERIFICACION) |
| G39 | 4 | lila | IV | P69 (139) | 8.6 | P69 (VERIFICACION) |
| G40 | 4 | lila | IX | P129 (157) | 9.3 | — |
| G41 | 4 | lila | IX | P84 (177) | 10.0 | — |
| G42 | 4 | lila | XI | P99 (188) | 10.4 | — |
| G43 | 4 | lila | XI | P114 (202) | 10.9 | P114 (VERIFICACION) |
| G44 | 4 | rojo | IV | P60 (68) | 6.1 | P60 (AJUSTE) |
| G45 | 4 | rojo | IV | P54 (121), P59 (122) | 8.0 | — |
| G46 | 4 | rojo | IV | P11 (194) | 10.4 | P11 (RE-MEDIDA) |
| G47 | 4 | rojo | IV | P57 (218), P12 (227) | 11.5 | P12 (AJUSTE) |
| G48 | 4 | rojo | IX | P120 (110) | 7.6 | P120 (AJUSTE) |
| G49 | 4 | rojo | IX | P119 (138), P75 (138) | 8.6 | — |
| G50 | 4 | rojo | IX | P74 (169) | 9.7 | P74 (AJUSTE) |
| G51 | 4 | rojo | XI | P55 (146) | 8.9 | P55 (AJUSTE) |
| G52 | 4 | rojo | XI | P90 (162), P105 (164) | 9.5 | — |
| G53 | 4 | rojo | XI | P104 (173) | 9.9 | P104 (VERIFICACION) |
| G54 | 4 | rojo | XI | P89 (183) | 10.2 | — |
| G55 | 4 | rojo | XI | P53 (279) | 13.7 | P53 (AJUSTE) |
| G56 | 5 | azul | IV | P66 (89), P67 (95) | 9.9 | P67 (AJUSTE) |
| G57 | 5 | azul | IX | P126 (91), P81 (92), P127 (97), P82 (98) | 9.9 | P127 (AJUSTE), P81 (VERIFICACION, re-medida) |
| G58 | 5 | azul | XI | P17 (83), P18 (84), P19 (85) | 9.4 | P18 (AJUSTE), P19 (VERIFICACION) |
| G59 | 5 | azul | XI | P111 (93), P96 (95), P112 (101), P97 (102) | 10.0 | P112 (AJUSTE) |
| G60 | 6 | naranja | IV | P70 (80), P71 (84) | 6.2 | P71 (AJUSTE) |
| G61 | 6 | naranja | IV | P72 (93) | 6.8 | P72 (AJUSTE) |
| G62 | 6 | naranja | IX | P130 (98), P131 (102) | 7.0 | P131 (AJUSTE) |
| G63 | 6 | naranja | IX | P132 (117), P85 (118), P86 (124) | 7.8 | P86 (RE-MEDIDA) |
| G64 | 6 | naranja | IX | P87 (131) | 8.4 | P87 (AJUSTE) |
| G65 | 6 | naranja | XI | P100 (143), P101 (146), P115 (147) | 8.9 | P101 (AJUSTE) |
| G66 | 6 | naranja | XI | P117 (154), P116 (158) | 9.4 | P117 (VERIFICACION) |
| G67 | 6 | naranja | XI | P102 (173) | 10.2 | P102 (AJUSTE) |
| G68 | 7 | blanco | I | P35 (96) | 7.0 | P35 (VERIFICACION) |
| G69 | 8 | amarillo | I | P44 (64) | 6.2 | P44 (AJUSTE) |
| G70 | 8 | amarillo | I | P37 (82), P34 (86) | 6.8 | P37 (AJUSTE), P34 (AJUSTE) |
| G71 | 8 | amarillo | I | P43 (122) | 8.6 | P43 (RE-MEDIDA) |
| G72 | a | verde | I | P40 (6), P45 (7) | 2.3 | P40 (VERIFICACION) |
| G73 | b | rojo | I | P39 (46), P38 (52) | 7.2 | P39 (AJUSTE), P38 (AJUSTE) |
| G74 | b | rojo | I | P49 (81) | 10.0 | P49 (RE-MEDIDA) |
| G75 | c | azul | I | P42 (7), P32a (9), P46 (9), P47 (9), P48 (9), P36 (10) | 5.5 | P36 (VERIFICACION) |
| G76 | d | naranja | I | P32b (68), P50 (71), P33 (72), P41 (73) | 8.3 | P41 (VERIFICACION) |

Los ejemplos del encargo se confirman:

- **P46, P47, P48, P32a, P36 y P42 son un solo grupo (G75).** El equipo no distingue certificados de
  7 a 10 a esa distancia del oscuro.
- **Los nueve verdes de 51** quedan en un grupo por tipo: G26 (con P64, de 54), G28 y G31.
- En el blanco, **P1, P103, P7, P6 y P88** (762-773) son un grupo.

En cambio, **el blanco IV no es redundante**: P3 y P2 caen en grupos distintos (G02 y G03), que es
precisamente la contradicción de PLAN §5.4.

## 3. Banco representativo

### 3.1 Reglas

1. **Ajuste, por código y por tipo (decisión C):** el grupo más bajo y el más alto de cada tipo IV, IX
   y XI. Así se conserva el rango de R de cada tipo y del código.
   - Dentro del grupo manda el patrón de re-medida. Si no está, uno con `x` medida y sin contradicción
     abierta, lo más cerca de la mediana del grupo.
   - Los patrones con contradicción abierta (P2, P3, P5, P23, P24, P22, P32a y P32b) no se eligen nunca
     para el ajuste. Si uno es el extremo de su tipo, el extremo pasa al grupo siguiente: en el
     amarillo IV, el alto es P30 (448) y no P24 (593, C-40).
2. **Re-medida:** el patrón de la tabla RF-CAL-37, a K = 5.
3. **Verificación independiente:** un patrón del código que no entra en el ajuste, lo más cerca del
   centro del rango, a K = 3 y con uso VERIFICACION. Así no entra en el ajuste
   (`FlujoCalibracion.java:287-302`).
4. **Códigos 8 y b, sin reducir:** 4 y 3 patrones, todos a K = 5, como en la cola completa. Con 3-4
   puntos, cada uno pesa del 25 al 33 % (PLAN §4), y la dispensa PA-24 nombra a P39 y a P49. **No les
   queda ningún patrón para una verificación independiente:** el catálogo no tiene más amarillos ni
   rojos tipo I. Los verifica su re-medida.
5. **Código 5:** P81 va como VERIFICACION a K = 5 (decisión P81), y el tipo IX se representa con P127.
6. **Sólo verificables:** un patrón en 7 (P35), a (P40), c (P36) y d (P41), los mismos de la
   "verificación rápida" de PLAN §2. Dos en café (P68 y P98) y dos en lila (P69 y P114), en los dos
   extremos, para ver el residuo frente a la curva del 4 (PA-15).
7. **Control, igual que en la cola completa:** calentamiento, batería, OSCURO a 5 × 4 al inicio y al
   final de cada sesión, y A5 al inicio y al final (5 × 4 la del inicio del banco y 3 × 4 las demás).
   - La sesión 1 lleva blanco, amarillo y sus tipo I.
   - La sesión 2 lleva rojo, café, lila, rojo tipo I, naranja, verde, azul y sus tipo I.
   - Cada color tiene el OSCURO de su propia sesión, que es su ancla (`Anclas.java:38-80`).
8. **K = 3 en AJUSTE, como en la cola completa.** Con 6-8 puntos por código, el ruido de colocación
   que queda en la curva es de 1,29 %/√7 ≈ 0,5 %. Pasarlos a K = 5 lo bajaría al 0,4 % a cambio de
   unos 20 min: no compensa frente al 1,5-3,5 % de la dispersión entre láminas (§4).

### 3.2 Por código

| Cód. | Método (RF-CAL-37) | AJUSTE | RE-MEDIDA (K = 5) | VERIFICACIÓN | Completa → repr. |
| :---: | :--- | :--- | :--- | :--- | :---: |
| 1 | Grado 1, escrito | P56 (IV 347), P51 (IV 439), P73 (IX 614), P7 (XI 768), P4 (XI 828) | P28 (IX 484) | P118 (IX 508) | 16 → 7 |
| 2 | Anclada, escrita | P61 (IV 207), P30 (IV 448), P121 (IX 320), P106 (XI 493), P20 (XI 782) | P25 (IX 576) | P21 (IV 375) | 24 → 7 |
| 3 | Grado 1 o anclada (PA-16) | P64 (IV 54), P63 (IV 121), P124 (IX 51), P109 (XI 51), P16 (XI 170) | P123 (IX 115) | P78 (IX 97) | 19 → 7 |
| 4 | Grado 1 o anclada (PA-16) | P60 (IV 68), P12 (IV 227), P120 (IX 110), P74 (IX 169), P55 (XI 146), P53 (XI 279) | P11 (IV 194) | P104 (XI 173); café P68 y P98; lila P69 y P114 | 26 → 12 |
| 5 | Anclada (PA-14) | P67 (IV 95), P127 (IX 97), P18 (XI 84), P112 (XI 101) | — (P81 fuera del ajuste) | P81 (IX 92, K = 5, re-medida), P19 (XI 85) | 13 → 6 |
| 6 | Grado 1 o anclada (PA-16) | P71 (IV 84), P72 (IV 93), P131 (IX 102), P87 (IX 131), P101 (XI 146), P102 (XI 173) | P86 (IX 124) | P117 (XI 154) | 15 → 8 |
| 8 | Anclada | P44, P37, P34 (K = 5) | P43 | — (no hay otro amarillo I) | 4 → 4 |
| b | Anclada (PA-24) | P39, P38 (K = 5) | P49 | — (no hay otro rojo I) | 3 → 3 |
| 7, a, c, d | Sólo verificar | — | — | P35, P40, P36, P41 | 13 → 4 |

**Total: 58 patrones.** Hay 36 AJUSTE, 7 RE-MEDIDA y 15 VERIFICACIÓN, más 2 sesiones de control.

- En el verde, P124 y P109 son verdes de 51. Si PA-16 los invalida, el extremo bajo de IX pasa a P78
  (97) y el de XI a P93 (85). La verificación del 3 pasaría entonces a P108.
- En el naranja, los grupos salen de una `x` estimada con la fábrica invertida, que falla hasta un
  40 %. Hasta que se mida, son grupos por certificado más que por lectura.

### 3.3 Tiempo

| Variante | Patrones | Sesiones | Tiempo |
| :--- | ---: | :---: | ---: |
| Banco completo (`cola_banco_P1-P132.csv`) | 133 | 3 (75 + 66 + 55) | **196 min** |
| **Banco representativo** (`cola_banco_representativo.csv`) | 58 | 2 (47 + 58) | **105 min** (−47 %) |
| El mismo, en una sola sesión | 58 | 1 | 95 min. Ahorra 10 min de control, pero es una sesión de 95 min con una sola comprobación de deriva (RF-CAL-36). No se recomienda |
| **Verificación anual** (`cola_verificacion_anual.csv`) | 12 | 1 | **38 min** |

### 3.4 Verificación anual

Contenido:

- OSCURO a 5 × 4 al inicio y al final;
- A5 al inicio (5 × 4, que da la s_rep de RF-CAL-38) y al final (deriva);
- un patrón por código a 3 × 4: el de re-medida de cada código ajustable (P28, P25, P123, P11, P81,
  P86, P43 y P49) y P35, P40, P36 y P41. P81 va a 5 × 4, porque `BancoCola` lo fuerza igualmente
  (P10-C3).

Todos van como VERIFICACION, así que ningún código es calibrable con esta cola. `BancoCola.calibrable`
exige algún AJUSTE o RE-MEDIDA (`BancoCola.java:250`, HEAD). Si un código sale de tolerancia, el
siguiente paso es el banco representativo, no un ajuste con 1 punto.

## 4. Qué se pierde

### 4.1 Incertidumbre de la curva, en cifras

La σ de la curva ajustada se expresa relativa a R, en la x mínima, el centro y la x máxima del rango
del código. Se calcula con mínimos cuadrados ponderados, sobre las `x` esperadas y con la K de la cola:

- **grado 1 libre** en 1, 3, 4 y 6;
- **anclada** en 2, 5, 8 y b, sin el error del ancla, que es igual en las dos colas.

Se dan dos σ por punto:

- **(a) sólo colocación:** σ_R = b·2,2 %·x/√K;
- **(b) colocación más dispersión entre láminas:** el RMS de residuos por patrón. Es el medido en
  REFORM (5,3 % en el 1, 5,9 % en el 2, 5,7 % en el 8 y 10,8 % en el b) y un **5,5 % supuesto** en
  3, 4, 5 y 6, que no tienen datos.

| Cód. | Método | n: completa → repr. | (a) Sólo colocación, % | (b) Colocación + láminas, % |
| :---: | :--- | :---: | :--- | :--- |
| 1 | grado 1 | 16 → 6 | 0,99 / 0,42 / 0,64 → 1,61 / 0,65 / 0,98 | 3,06 / 1,46 / 2,24 → **5,03 / 2,33 / 3,46** |
| 2 | anclada | 24 → 6 | 0,37 → 0,72 | 1,26 → **2,53** |
| 3 | grado 1 | 19 → 6 | 2,97 / 1,35 / 1,68 → 5,57 / 2,21 / 2,81 | 3,47 / 2,17 / 2,80 → **6,53 / 3,59 / 4,65** |
| 4 | grado 1 | 16 → 7 | 2,21 / 0,52 / 0,83 → 2,60 / 0,74 / 1,00 | 5,13 / 1,58 / 2,41 → **5,64 / 2,35 / 3,12** |
| 5 | anclada | 12 → 4 | 1,10 → 1,89 | 1,93 → **3,34** |
| 6 | grado 1 | 15 → 7 | 1,55 / 0,59 / 0,91 → 2,17 / 0,85 / 1,37 | 3,63 / 1,57 / 2,47 → **5,13 / 2,34 / 3,79** |
| 8 | anclada | 4 → 4 | 1,14 (igual) | 3,08 (igual) |
| b | anclada | 3 → 3 | 2,24 (igual) | 6,67 (igual) |

En la anclada, la σ relativa es la misma en todo el rango.

Lectura:

- **Sólo con la colocación, la pérdida es pequeña:** en el centro del rango, de décimas de punto a
  0,9 puntos. La colocación no justifica medir 133 patrones.
- **Con la dispersión entre láminas, la σ de la curva crece por ≈ √(n_completa/n_repr.):** 1,6 a 2
  veces. En el centro pasa del 1,3-2,2 % al 2,3-3,6 %. En los extremos de los códigos de grado 1 llega
  al 5-6,5 %. Todo sigue por debajo del 10 % de RF-CAL-14, pero **se come margen**:
  - frente al 6 % de RMS de RF-CAL-15;
  - y en la prueba de "mejora significativa" de REFORM §3 (|Δ| > 2·σ_col). Con σ_col 1,6-2 veces
    mayor, un cambio real de la curva tarda más en ser significativo.
- **Con 3 niveles por tipo en lugar de 2** (unos 9-10 puntos por código y ~15 min más), la σ bajaría
  a ≈ 1,3 veces la del banco completo. Queda como opción, si Diego prefiere margen a tiempo.

### 4.2 Cobertura: la regla de la app se cumple

Ejecutada con el código de la app: `CoberturaCola.java` sobre `Asistente.cobertura` de `HEAD` 9c970d3,
JDK 11. El script la reproduce además en Python y da lo mismo.

| Cód. | Completa | Representativa | Representativa con el ancla (anclada) |
| :---: | :--- | :--- | :--- |
| 1 | 16 niveles, R 347-828 → grado ≤ 2 | 6 niveles, R 347-828 → grado ≤ 2 | 7, R 0-828 → grado ≤ 2 |
| 2 | 24, R 207-782 → ≤ 2 | 6, R 207-782 → ≤ 2 | 7, R 0-782 → ≤ 2 |
| 3 | 11, R 51-173 → ≤ 2 | 5, R 51-170 → ≤ 2 | 6, R 0-170 → ≤ 2 |
| 4 | 15, R 68-279 → ≤ 2 | 7, R 68-279 → ≤ 2 | 8, R 0-279 → ≤ 2 |
| 5 | 12, R 83-102 → no ajustable (rango estrecho) | 4, R 84-101 → no ajustable | **5, R 0-101 → ≤ 2** (RF-APP-42, PA-14) |
| 6 | 15, R 80-173 → ≤ 2 | 7, R 84-173 → ≤ 2 | 8, R 0-173 → ≤ 2 |
| 8 | 4, R 64-122 → ≤ 2 | 4 (igual) | 5 → ≤ 2 |
| b | 3, R 46-81 → ≤ 1 | 3 (igual) | 4 → ≤ 2 |

**Todos los códigos ajustables cumplen con la cola representativa** para su método de RF-CAL-37:

- grado 1 en 1, 3, 4 y 6, con 5-7 niveles;
- anclada en 2, 5, 8 y b.

El rango de R sólo cambia en el 3 (173 → 170) y en el 6 (80 → 84), porque el extremo es un grupo
equivalente. La verificación anual no tiene AJUSTE: no se calibra con ella.

**Hallazgo de paso.** Con la cola completa y el código de `HEAD`, el 5 no tiene ningún AJUSTE, y
`patronesAjuste` devuelve entonces *todos* sus patrones, P81 incluido (`FlujoCalibracion.java:287-302`).
Eso contradice la decisión P81. En el árbol de trabajo hay un cambio sin confirmar de otro agente que lo
corrige (`f.excluidos`). La cola representativa lo evita por construcción: declara AJUSTE en el 5 y
deja P81 en VERIFICACION.

### 4.3 Lo que sólo resuelve el banco completo

El banco representativo deja sin medir 75 patrones. Con ellos se va la posibilidad de cerrar:

- **P2/P3** (PLAN §5.4): los cuatro IV nuevos del blanco iban a decir cuál se sale de la fila. En el
  representativo sólo entran P56 y P51.
- **C-39 (P23/P5)**: faltan los XI amarillos P91, P92 y P107 como árbitros. Sólo entra P106.
- **C-40 (P24)** y el **FUERA de P22**: no entran en el ajuste.
- **PA-16, los nueve verdes de 51**: el aviso era "si los nueve dan `x` muy distintas, es una señal".
  Con dos de ellos no hay señal.
- **Café y lila (PA-15)**: 4 residuos frente al 4, en lugar de 10.
- **Las `x` estimadas de los 83 sin medir**, sobre todo el naranja intenso, que no tiene ninguno medido.
  Los grupos de §2 para P51-P132 son tan buenos como esas estimaciones.

**Por eso: el completo una vez, y el representativo después.** Con el completo medido, los grupos de §2
se recalculan con `x` medidas y el representativo se elige sobre datos. El script lo hace si se le pasa
una cola con las `x` del banco.

## 5. La app de hoy no carga las colas nuevas

El encargo pedía "el mismo formato que la actual para que la app lo cargue sin tocar código". **El
formato es el mismo**: mismas 17 columnas, mismo orden, ASCII en `nota`, LF. **Cargarlo sin tocar
código no es posible:**

- `BancoCola.cargar` rechaza cualquier md5 que no esté en `MD5_PERMITIDOS`, que hoy sólo lleva
  `9ddb7882…` (`BancoCola.java:28-29` y `:150`, HEAD).
- El nombre del asset es fijo: `ASSET = "cola_banco_P1-P132.csv"` (`:30`).

Ejecutado con el código de `HEAD`:

- la representativa da `Cola no admitida (md5 2e266bbf89ec7dfb716e5ddd2d59d8f4)`;
- la anual da `Cola no admitida (md5 d86eddf7fbdfdd4ae11ee2080d220e6c)`.

Es lo que exige RF-APP-33 (`SPEC-V3.6.md:856`). Sustituir el fichero del asset por el nuevo con su
nombre tampoco valdría: el md5 no cuadra.

**En el árbol de trabajo hay un cambio sin confirmar de otro agente (app 3.6.15):** un `enum Tipo`
con `COMPLETO`, `REPRESENTATIVO` y `VERIFICACION_ANUAL`, y los mismos nombres de fichero que aquí. Le
faltan sólo el asset y los dos md5 de este documento. Es RF-APP-49.

## 6. ZIP más ligero

### 6.1 Qué ocupa cada parte (bytes: sin comprimir / comprimido)

Medido con `medir_zip_campana.py` sobre los ZIP de `SLV-002/campanas/`:

| ZIP | Total | campana.csv | resumen.txt | diario | pruebas.txt | actas/ | tramas/ |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 10:33 | 50 196 | 77 058 / 6 742 | 10 383 / 2 298 | 35 230 / 6 183 | 7 436 / 1 940 | — | 112 452 / 32 031 |
| 12:00 | 63 400 | 164 675 / 9 788 | 23 022 / 4 960 | 54 277 / 8 730 | 15 857 / 2 794 | — | 123 667 / 35 814 |
| 12:10 | 71 939 | 164 675 / 9 788 | 23 263 / 5 108 | 54 446 / 8 854 | 15 857 / 2 794 | — | 164 644 / 44 081 |
| **12:27** | **80 559** | 164 675 / **9 788** | 23 525 / 5 229 | 54 615 / 8 944 | 15 857 / 2 794 | 1 977 / 949 | 203 070 / **51 347** |

- **Las tramas son el 64 % del ZIP de las 12:27** (51,3 kB). El registro T4
  (`rtv36_20260919_114644.txt`) ocupa 125,7 kB sin comprimir y 28,7 kB dentro del ZIP.
- **`campana.csv` es el 12 %.** Es un derivado del diario, con los datos del equipo repetidos en cada
  disparo: 244 B por disparo frente a 81 B.
- **Repetición:** de 12:10 a 12:27 viajan otra vez **35,2 kB comprimidos idénticos byte a byte**, el
  44 % del ZIP. Lo nuevo fueron 25,5 kB (el resumen y el acta) y 38,6 kB añadidos al final de T4. El
  diario sólo creció 169 B.
- **Compresión:** `Campanas.exportar` usa DEFLATE con el nivel por defecto (`Campanas.java:419-451`).
  Reconstruido con el nivel 6 da 80,4 kB (control del método), y con el 9, **78,1 kB (−3 %)**. Un solo
  flujo xz daría 44,5 kB, pero `java.util.zip` no tiene xz y pediría una dependencia. **La compresión no
  es el problema: el problema es la repetición.**

### 6.2 Propuestas y su efecto sobre el ZIP de las 12:27

| Variante | Tamaño | Reducción |
| :--- | ---: | ---: |
| Hoy | 80,6 kB | — |
| Nivel 9 | 78,1 kB | −3 % |
| Diario compacto (`D,n,s,x`: segundos desde la línea SERIE; la respuesta bruta sólo si no es `::x`) | 76,3 kB | −5 %. El diario baja de 54,6 a 24,9 kB sin comprimir, pero DEFLATE ya se comía esa redundancia |
| Sin `campana.csv`, que se regenera del diario (el importador prefiere el diario: `ImportadorCampana.java:50-67`) | 70,5 kB | −12 % |
| Sin `campana.csv` y sin tramas, con un índice SHA-256 de las tramas | 18,9 kB | −77 % |
| Lo anterior, con diario compacto | 14,8 kB | −82 % |
| **Incremental** frente a la exportación de las 12:10: lo nuevo, lo añadido desde el byte en que acababa cada fichero y un `indice.sha256` | 17,1 kB | −79 % |
| **Incremental sin tramas ni `campana.csv`** (van al ZIP de soporte) | **7,6 kB** | **−91 %** |

**A escala de banco.** Por disparo, dentro del ZIP: tramas ≈ 26 B, diario ≈ 10 B y `campana.csv` ≈
14 B, unos 50 B en total. Lo miden las líneas de disparo de T4 y de los ficheros de las 12:27. Con una
exportación al final de cada sesión (RF-APP-33):

| Banco (disparos) | Hoy: suma de los ZIP de la campaña | Incremental, con tramas | Incremental sin tramas ni `campana.csv` |
| :--- | ---: | ---: | ---: |
| Completo (2575, en 3 sesiones) | ≈ 265 kB (45 + 91 + 129) | ≈ 129 kB | ≈ 26 kB |
| Representativo (1310, en 2) | ≈ 91 kB (26 + 65) | ≈ 65 kB | ≈ 13 kB |
| Verificación anual (360, en 1) | ≈ 18 kB | ≈ 18 kB | ≈ 4 kB |

Sólo cuenta los disparos. Cada conexión añade además su registro de pruebas del equipo, unos 11 kB
comprimidos (`rtv36_20260919_111628.txt`), más el resumen y `pruebas.txt`, unos 8 kB. Hoy eso se
repite en cada ZIP. Con la propuesta, va una vez al ZIP de soporte.

### 6.3 Lo que la SPEC obliga a llevar, y cómo se conserva la trazabilidad

| Pieza | Exigencia | ZIP incremental (el de cada sesión) | ZIP de soporte |
| :--- | :--- | :--- | :--- |
| `diario` | SPEC-V3.6 32.2 y 32.4 (`SPEC-V3.6.md:825`); RF-CAL-28: quién calibró; RF-APP-36: acta en curso | **Sí**, el tramo nuevo desde el último byte exportado, con el SHA-256 del diario completo | Completo |
| `campana.csv` | 32.4 ("con exactamente"); RF-CAL-10 (`SPEC-Calibracion-V3.6.md:239-242`) | No: se regenera del diario | Sí |
| `resumen.txt` | 32.4: "lleva el md5 de cada una de las otras piezas" | Sí | Sí |
| `pruebas.txt` | 32.4 | Sí, si cambió | Sí |
| `tramas/` | 32.4 ("registro de tramas de todas las conexiones"); RF-CAL-10. Además son la prueba de las escrituras: REFORM §1 lee `#S` en `tramas:1660` y `:2520`, y la re-medida en `:2234` y `:2762` | **No**: sólo su SHA-256 y su tamaño en el índice | **Sí**, completas |
| `actas/` | RF-APP-36 | Sí, las nuevas | Sí |
| Cadena de hashes | RF-CAL-10 y §8.3: el SHA-256 del ZIP ata el informe a los datos (`SPEC-Calibracion-V3.6.md:545-563`) | `indice.sha256`, con el SHA-256 del ZIP anterior | El índice de todos los incrementales |

- **La cadena de hashes ya existe a medias.** `resumen.txt` lleva las exportaciones anteriores con su
  md5 y su SHA-256 (`resumen.txt:194-196` del ZIP de las 12:27).
- **Lo que no lleva es el md5 de cada pieza, que 32.4 exige.** El índice del incremental lo cubre.
- **La trazabilidad no se pierde.**
  - Cada incremental nombra el SHA-256 del anterior y el de cada fichero completo que resume.
  - Reconstruir la campaña es aplicar los tramos en orden y comprobar que cada SHA-256 cuadra.
  - Las tramas completas siguen existiendo en el móvil (`files/registros`) y en el ZIP de soporte, y su
    SHA-256 está en todos los incrementales, así que no pueden cambiar sin que se vea.
- **Cambios de SPEC que hacen falta** (este documento no los hace):
  - 32.4, porque dice "con exactamente";
  - RF-CAL-10, que dice "un único ZIP";
  - RF-CAL-35, que cita la cola `9ddb7882…`.
- **El acta y el informe PDF** citan el SHA-256 del ZIP que contiene la sesión del ajuste y el del ZIP
  de soporte (§8.3).

## 7. Requisitos para la próxima app

Siguen la numeración de `SPEC-V3.6.md`, cuyo último requisito es RF-APP-48. Son **propuesta**
mientras Diego no los apruebe.

| ID | Requisito | Criterio de aceptación |
| :--- | :--- | :--- |
| **RF-APP-49** | **Selector de banco.** Al entrar en "Medir el banco": "Banco completo (133, ≈ 196 min)", "Banco representativo (58, ≈ 105 min)" y "Verificación anual (12, ≈ 38 min)". Cada opción es un asset con su md5 en la lista del APK. La campaña guarda el tipo y el md5 de su cola, y **no se cambia de cola dentro de una campaña**. La verificación anual no ofrece "Calibrar" | Con los 3 assets: 180, 94 y 35 pasos, en el orden del CSV. md5 admitidos `9ddb7882…`, `2e266bbf…` y `d86eddf7…`; con otro md5, "Cola no admitida" y ningún `e`. Cambiar de tipo con la campaña abierta, "Cierre la campaña o abra otra". Con la anual, `calibrable` da false para los 12 códigos. El acta y el resumen dicen el tipo y el md5 |
| **RF-APP-50** | **ZIP incremental.** Cada exportación lleva sólo lo nuevo desde la anterior del mismo equipo. Los ficheros nuevos o cambiados van enteros; los que crecieron, sólo el tramo nuevo, como `nombre.desde_<byte>`. Lleva también un `indice.sha256` con SHA-256, tamaño y byte de inicio de cada pieza completa, y el nombre y el SHA-256 del ZIP anterior. `resumen.txt` va siempre. Sin tramas ni `campana.csv` (RF-APP-51) | Con la campaña del 19-sep, exportar a las 12:10 y a las 12:27: el segundo ZIP no contiene ningún byte que ya estuviera en el primero, salvo `resumen.txt` e `indice.sha256`, y ocupa ≤ 10 kB. Un script que aplica los tramos en orden reconstruye el diario, byte a byte igual al del móvil. Cada SHA-256 del índice coincide con `sha256sum` de la pieza reconstruida. Borrar un incremental intermedio: la reconstrucción falla y dice cuál falta |
| **RF-APP-51** | **ZIP de soporte aparte.** Un segundo botón, "ZIP de soporte", genera `soporte_<serie>_<fecha>.zip` con todo lo de hoy (tramas completas de todas las conexiones, `campana.csv`, diario, pruebas, actas y resumen) y los índices de todos los incrementales. Se comparte por separado (un adjunto, 32.5) y se copia en `Download/RTV/` (RF-APP-40). Al **cerrar la campaña**, o si el acta la acepta, se genera sin preguntar | Su contenido es el superconjunto de todos los incrementales: cada SHA-256 de cada `indice.sha256` se encuentra en él. El acta aceptada cita su SHA-256. Con el caso del 19-sep, las tramas del soporte son byte a byte las de `files/registros` |
| **RF-APP-52** | **Índice de piezas en `resumen.txt`.** Cumple lo que 32.4 ya pide y hoy no se hace: el md5 y el SHA-256 de cada pieza del ZIP | En el ZIP de las 12:27 regenerado, `resumen.txt` lista 10 piezas con hashes que coinciden con las extraídas. Hoy sólo lista los ZIP anteriores (`resumen.txt:194-196`) |
| **RF-APP-53** | **Compresión y diario.** Nivel 9 de DEFLATE (`ZipOutputStream.setLevel(9)`). El diario **no** se compacta: ahorra un 5 % del ZIP y obligaría a cambiar el formato de sólo añadir (32.2) y el importador | ZIP de las 12:27 regenerado: ≤ 78,2 kB. El diario exportado es idéntico al del móvil |

## 8. Qué no dice este documento

- Que el banco representativo dé una curva tan buena como el completo. Da una curva con 1,6-2 veces
  más σ, y eso con un RMS entre láminas **supuesto** en 3, 4, 5 y 6.
- Que las `x` esperadas sean buenas. En el naranja intenso sólo son la fábrica invertida.
- Que la app cargue las colas: hoy no las carga (§5).
- Nada sobre el tiempo real: el modelo es el del plan, comprobado con una sola campaña.
