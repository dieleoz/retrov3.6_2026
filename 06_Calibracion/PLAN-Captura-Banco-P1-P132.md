# Plan de captura del banco de patrones P1-P132

**Nada de este plan se ha ejecutado. Es la cola que tendrá que cargar la app 3.6.10, que todavía no
existe.** Los tiempos son estimaciones hechas con los tiempos de disparo medidos el 19-sep-2026, no
medidas de un banco. De los 133 patrones, 83 (P51-P132) **no se han medido nunca**: su `x` esperada es
una estimación y se dice en cada fila. La cobertura por código se ha calculado con la regla de la app
(`Asistente.coberturaValores`, `Asistente.java:75-98`). Que un código cumpla la regla no significa que
se pueda escribir: eso lo deciden el ajuste con lo medido y Diego.

- **Fuente del catálogo:** `06_Calibracion/patrones_certificados_P1-P132.csv` (md5
  `a74222c06ce7bf18d95bb3d3851a173d`), sacado de `Datos certificados papeles reflectivos sept 2026.xlsx`
  (commit `bba4dbe`). Son 133 patrones, porque P32 está repetido en la fuente (P32a azul 9 y P32b
  naranja 68).
- **Cola para la app:** `06_Calibracion/cola_banco_P1-P132.csv`, generada junto a este documento (md5
  `5ba9465852fd721751c496183f2dff95`). Tiene 180 filas: 133 patrones y 47 pasos de control. El acta
  del banco debe citar ese md5.
- **`x` medida:** media de la serie elegida en `resumen.txt` del ZIP
  `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip` (md5 `ce1f35fc…`), líneas
  14-118. Oscuro: `x` = 565,4, con s entre colocaciones de 2,0 (`resumen.txt:122`). A5: s_rep media
  del 2,24 % (`resumen.txt:186-191`).
- Las cifras de este documento se han calculado con un script de trabajo que no se versiona. Todas se
  pueden rehacer desde el CSV del catálogo y el `resumen.txt` citado.

---

## 0. Reglas del banco

1. **La captura siempre se hace con `e`.** El equipo devuelve la `x`, y la columna "Código" dice a qué
   curva va ese punto. En la re-medida de la Fase B se alternan `e` y el código (`AdminActivity.java:772-774`).
2. **Café y lila se miden con el código del rojo**: 4 si la lámina es intensa y b si es tipo I (dato
   de campo de Diego del 19-sep). En el catálogo nuevo no hay ningún café ni lila tipo I, así que los
   10 van al 4. **No entran en el ajuste del 4**: se verifican (PA-15, §3.9).
3. **Protocolo K × M con M = 4 fijo en todo el banco**, más 1 disparo de asentamiento por colocación.
   El M es el mismo en campaña, re-medida y verificación (P9-B3). Con M = 4 actúa el rechazo de
   descolgados (`Veredicto.N_MIN_DESCOLGADOS = 4`, `Veredicto.java:40`).
4. **K = 3 colocaciones** para los patrones de AJUSTE y de VERIFICACIÓN. **K = 5** para el OSCURO y
   la A5 del inicio del banco, para el patrón de RE-MEDIDA de cada código y para los cuatro patrones
   del código 8. §4 explica por qué.
5. **La A5 (P22, P28, P4) y el OSCURO van al principio y al final de cada sesión.** Los del principio
   dan la s_rep y el ancla. Los del final dan la deriva de la sesión (RF-CAL-36).
6. **La batería se lee con la orden `9`** al empezar la sesión y al cambiar de grupo de color
   (RF-CAL-41). La respuesta es `:<n>:`, y la tensión sale de V = 10 + (n + 30,09)/90,91 (`gui.c:329-356`,
   `measurement.c:137-146`; derivación en `SPEC-Calibracion-V3.6.md` §12.8). **Ninguna respuesta de
   `9` se ha registrado nunca con la V3.6**: la única petición de ese día quedó sin respuesta, con el
   equipo mudo (`07 pruebas/campana_103300/tramas/rtv36_20260919_095234.txt:15-16`).
7. **Orden de campo:** calentamiento y OSCURO al principio, y después **por color**, para no ir
   cambiando de caja. Dentro de cada color, de IV a IX, luego XI y por último tipo I, y de menor a
   mayor certificado. Tres sesiones de unos 60 min, con exportación al final de cada una.

**Uso** (columna de la cola):

| Uso | Qué significa |
| :--- | :--- |
| AJUSTE | Entra en el ajuste de su código (decisión C: todos los patrones del color y la clase) |
| RE-MEDIDA | Entra en el ajuste y además es el patrón que la Fase B vuelve a medir con el código escrito |
| VERIFICACIÓN | Se mide y se compara con la curva vigente de su código. No entra en ningún ajuste |
| control | OSCURO, A5 y batería. No entra en el ajuste |

**Columna "x esperada".** Sin marca: medida el 19-sep. **(o)**: estimada con una recta que pasa por el
oscuro (565,4 ; 0) y se ajusta a los patrones medidos del mismo color y clase. **(r)**: igual, pero con
la recta del rojo intenso, porque café y lila no tienen datos. **(f)**: curva de fábrica invertida,
sólo en el naranja intenso, que no tiene ningún patrón medido. §5.5 explica por qué no se usa la curva
de fábrica en todos, como pedía el encargo.

---

## 1. La cola

| # | Ses. | Paso | Patrón | Color | Tipo | Cert. | Código | Uso | K × M | x esperada | Nota |
| ---: | :---: | :--- | :--- | :--- | :---: | ---: | :---: | :--- | :---: | ---: | :--- |
| 1 | 1 | CALENTAMIENTO |  |  |  |  |  |  |  |  | 10 min encendido antes de la primera serie (RF-CAL-01) |
| 2 | 1 | BATERIA |  |  |  |  | 9 | control |  |  | orden 9: tensión y nivel; aviso si baja (RF-CAL-41) |
| 3 | 1 | OSCURO inicio | OSCURO |  |  | 0 | e | control | 5 × 4 | 565 | superficie negra mate; ancla de la recta del código 2 (y del 5 si PA-14) |
| 4 | 1 | A5-INICIO | P22 (A5) | amarillo | IV | 334 | e | control | 5 × 4 | 1497 | puente y s_rep (P9-A5); no entra en el ajuste |
| 5 | 1 | A5-INICIO | P28 (A5) | blanco | IX | 484 | e | control | 5 × 4 | 2277,9 | puente y s_rep (P9-A5); no entra en el ajuste |
| 6 | 1 | A5-INICIO | P4 (A5) | blanco | XI | 828 | e | control | 5 × 4 | 3315,8 | puente y s_rep (P9-A5); no entra en el ajuste |
| 7 | 1 | blanco | P56 | blanco | IV | 347 | 1 | AJUSTE | 3 × 4 | ~1717 (o) |  |
| 8 | 1 | blanco | P3 | blanco | IV | 378 | 1 | AJUSTE | 3 × 4 | 1674 | P2 y P3 se contradicen entre sí: leer etiqueta |
| 9 | 1 | blanco | P2 | blanco | IV | 414 | 1 | AJUSTE | 3 × 4 | 2058,6 | P2 y P3 se contradicen entre sí: leer etiqueta |
| 10 | 1 | blanco | P52 | blanco | IV | 421 | 1 | AJUSTE | 3 × 4 | ~1962 (o) |  |
| 11 | 1 | blanco | P58 | blanco | IV | 432 | 1 | AJUSTE | 3 × 4 | ~1999 (o) |  |
| 12 | 1 | blanco | P51 | blanco | IV | 439 | 1 | AJUSTE | 3 × 4 | ~2022 (o) |  |
| 13 | 1 | blanco | P27 | blanco | IX | 471 | 1 | AJUSTE | 3 × 4 | 2111,1 |  |
| 14 | 1 | blanco | P28 | blanco | IX | 484 | 1 | RE-MEDIDA | 5 × 4 | 2277,9 |  |
| 15 | 1 | blanco | P118 | blanco | IX | 508 | 1 | AJUSTE | 3 × 4 | ~2251 (o) |  |
| 16 | 1 | blanco | P73 | blanco | IX | 614 | 1 | AJUSTE | 3 × 4 | ~2603 (o) |  |
| 17 | 1 | blanco | P1 | blanco | XI | 762 | 1 | AJUSTE | 3 × 4 | 2960,9 |  |
| 18 | 1 | blanco | P103 | blanco | XI | 763 | 1 | AJUSTE | 3 × 4 | ~3097 (o) |  |
| 19 | 1 | blanco | P7 | blanco | XI | 768 | 1 | AJUSTE | 3 × 4 | 3122,4 |  |
| 20 | 1 | blanco | P6 | blanco | XI | 772 | 1 | AJUSTE | 3 × 4 | 3168,4 |  |
| 21 | 1 | blanco | P88 | blanco | XI | 773 | 1 | AJUSTE | 3 × 4 | ~3130 (o) |  |
| 22 | 1 | blanco | P4 | blanco | XI | 828 | 1 | AJUSTE | 3 × 4 | 3315,8 |  |
| 23 | 1 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 24 | 1 | blanco-I | P35 | blanco | I | 96 | 7 | VERIFICACIÓN | 3 × 4 | 1114,7 |  |
| 25 | 1 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 26 | 1 | amarillo | P61 | amarillo | IV | 207 | 2 | AJUSTE | 3 × 4 | ~1132 (o) |  |
| 27 | 1 | amarillo | P62 | amarillo | IV | 280 | 2 | AJUSTE | 3 × 4 | ~1331 (o) |  |
| 28 | 1 | amarillo | P22 | amarillo | IV | 334 | 2 | AJUSTE | 3 × 4 | 1497 | FUERA en la A5 del 19-sep (-5,6 %) |
| 29 | 1 | amarillo | P21 | amarillo | IV | 375 | 2 | AJUSTE | 3 × 4 | 1562,4 |  |
| 30 | 1 | amarillo | P29 | amarillo | IV | 442 | 2 | AJUSTE | 3 × 4 | 1677 |  |
| 31 | 1 | amarillo | P30 | amarillo | IV | 448 | 2 | AJUSTE | 3 × 4 | 1654,2 |  |
| 32 | 1 | amarillo | P24 | amarillo | IV | 593 | 2 | AJUSTE | 3 × 4 | 1982,7 | identidad sin confirmar (C-40) |
| 33 | 1 | amarillo | P121 | amarillo | IX | 320 | 2 | AJUSTE | 3 × 4 | ~1441 (o) |  |
| 34 | 1 | amarillo | P122 | amarillo | IX | 352 | 2 | AJUSTE | 3 × 4 | ~1528 (o) |  |
| 35 | 1 | amarillo | P77 | amarillo | IX | 391 | 2 | AJUSTE | 3 × 4 | ~1635 (o) |  |
| 36 | 1 | amarillo | P76 | amarillo | IX | 433 | 2 | AJUSTE | 3 × 4 | ~1750 (o) |  |
| 37 | 1 | amarillo | P31 | amarillo | IX | 573 | 2 | AJUSTE | 3 × 4 | 2085,8 |  |
| 38 | 1 | amarillo | P25 | amarillo | IX | 576 | 2 | RE-MEDIDA | 5 × 4 | 2157,4 |  |
| 39 | 1 | amarillo | P26 | amarillo | IX | 583 | 2 | AJUSTE | 3 × 4 | 2208,8 |  |
| 40 | 1 | amarillo | P107 | amarillo | XI | 478 | 2 | AJUSTE | 3 × 4 | ~1873 (o) |  |
| 41 | 1 | amarillo | P106 | amarillo | XI | 493 | 2 | AJUSTE | 3 × 4 | ~1914 (o) |  |
| 42 | 1 | amarillo | P91 | amarillo | XI | 516 | 2 | AJUSTE | 3 × 4 | ~1977 (o) |  |
| 43 | 1 | amarillo | P92 | amarillo | XI | 522 | 2 | AJUSTE | 3 × 4 | ~1994 (o) |  |
| 44 | 1 | amarillo | P10 | amarillo | XI | 680 | 2 | AJUSTE | 3 × 4 | 2504,3 |  |
| 45 | 1 | amarillo | P9 | amarillo | XI | 705 | 2 | AJUSTE | 3 × 4 | 2483,9 |  |
| 46 | 1 | amarillo | P23 | amarillo | XI | 714 | 2 | AJUSTE | 3 × 4 | 2735,4 | P23 lee más que P5 con menos certificado (C-39) |
| 47 | 1 | amarillo | P8 | amarillo | XI | 721 | 2 | AJUSTE | 3 × 4 | 2532,9 |  |
| 48 | 1 | amarillo | P5 | amarillo | XI | 740 | 2 | AJUSTE | 3 × 4 | 2459,3 | P23 lee más que P5 con menos certificado (C-39) |
| 49 | 1 | amarillo | P20 | amarillo | XI | 782 | 2 | AJUSTE | 3 × 4 | 2787,7 |  |
| 50 | 1 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 51 | 1 | amarillo-I | P44 | amarillo | I | 64 | 8 | AJUSTE | 5 × 4 | 898,6 |  |
| 52 | 1 | amarillo-I | P37 | amarillo | I | 82 | 8 | AJUSTE | 5 × 4 | 992,4 |  |
| 53 | 1 | amarillo-I | P34 | amarillo | I | 86 | 8 | AJUSTE | 5 × 4 | 958,3 |  |
| 54 | 1 | amarillo-I | P43 | amarillo | I | 122 | 8 | RE-MEDIDA | 5 × 4 | 1151,3 |  |
| 55 | 1 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 56 | 1 | A5-FIN | P22 (A5) | amarillo | IV | 334 | e | control | 3 × 4 | 1497 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 57 | 1 | A5-FIN | P28 (A5) | blanco | IX | 484 | e | control | 3 × 4 | 2277,9 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 58 | 1 | A5-FIN | P4 (A5) | blanco | XI | 828 | e | control | 3 × 4 | 3315,8 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 59 | 1 | OSCURO fin | OSCURO |  |  | 0 | e | control | 3 × 4 | 565 | deriva del oscuro |
| 60 | 1 | EXPORTAR |  |  |  |  |  | control |  |  | ZIP de la sesión; copia en `Download/RTV/` |
| 61 | 1 | PAUSA |  |  |  |  |  | control |  |  | si se apaga el equipo o se cambia la batería: 10 min de calentamiento al volver |
| 62 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | orden 9: tensión y nivel; aviso si baja (RF-CAL-41) |
| 63 | 2 | OSCURO inicio | OSCURO |  |  | 0 | e | control | 3 × 4 | 565 | superficie negra mate; ancla de la recta del código 2 (y del 5 si PA-14) |
| 64 | 2 | A5-INICIO | P22 (A5) | amarillo | IV | 334 | e | control | 3 × 4 | 1497 | puente y s_rep (P9-A5); no entra en el ajuste |
| 65 | 2 | A5-INICIO | P28 (A5) | blanco | IX | 484 | e | control | 3 × 4 | 2277,9 | puente y s_rep (P9-A5); no entra en el ajuste |
| 66 | 2 | A5-INICIO | P4 (A5) | blanco | XI | 828 | e | control | 3 × 4 | 3315,8 | puente y s_rep (P9-A5); no entra en el ajuste |
| 67 | 2 | rojo | P60 | rojo | IV | 68 | 4 | AJUSTE | 3 × 4 | ~945 (o) |  |
| 68 | 2 | rojo | P54 | rojo | IV | 121 | 4 | AJUSTE | 3 × 4 | ~1241 (o) |  |
| 69 | 2 | rojo | P59 | rojo | IV | 122 | 4 | AJUSTE | 3 × 4 | ~1247 (o) |  |
| 70 | 2 | rojo | P11 | rojo | IV | 194 | 4 | RE-MEDIDA | 5 × 4 | 1722,9 |  |
| 71 | 2 | rojo | P57 | rojo | IV | 218 | 4 | AJUSTE | 3 × 4 | ~1783 (o) |  |
| 72 | 2 | rojo | P12 | rojo | IV | 227 | 4 | AJUSTE | 3 × 4 | 1761,9 |  |
| 73 | 2 | rojo | P120 | rojo | IX | 110 | 4 | AJUSTE | 3 × 4 | ~1180 (o) |  |
| 74 | 2 | rojo | P119 | rojo | IX | 138 | 4 | AJUSTE | 3 × 4 | ~1336 (o) |  |
| 75 | 2 | rojo | P75 | rojo | IX | 138 | 4 | AJUSTE | 3 × 4 | ~1336 (o) |  |
| 76 | 2 | rojo | P74 | rojo | IX | 169 | 4 | AJUSTE | 3 × 4 | ~1510 (o) |  |
| 77 | 2 | rojo | P55 | rojo | XI | 146 | 4 | AJUSTE | 3 × 4 | ~1381 (o) |  |
| 78 | 2 | rojo | P90 | rojo | XI | 162 | 4 | AJUSTE | 3 × 4 | ~1470 (o) |  |
| 79 | 2 | rojo | P105 | rojo | XI | 164 | 4 | AJUSTE | 3 × 4 | ~1482 (o) |  |
| 80 | 2 | rojo | P104 | rojo | XI | 173 | 4 | AJUSTE | 3 × 4 | ~1532 (o) |  |
| 81 | 2 | rojo | P89 | rojo | XI | 183 | 4 | AJUSTE | 3 × 4 | ~1588 (o) |  |
| 82 | 2 | rojo | P53 | rojo | XI | 279 | 4 | AJUSTE | 3 × 4 | ~2124 (o) |  |
| 83 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 84 | 2 | cafe | P68 | café | IV | 36 | 4 | VERIFICACIÓN | 3 × 4 | ~767 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 85 | 2 | cafe | P128 | café | IX | 38 | 4 | VERIFICACIÓN | 3 × 4 | ~778 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 86 | 2 | cafe | P83 | café | IX | 55 | 4 | VERIFICACIÓN | 3 × 4 | ~873 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 87 | 2 | cafe | P113 | café | XI | 60 | 4 | VERIFICACIÓN | 3 × 4 | ~901 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 88 | 2 | cafe | P98 | café | XI | 68 | 4 | VERIFICACIÓN | 3 × 4 | ~945 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 89 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 90 | 2 | lila | P69 | lila | IV | 139 | 4 | VERIFICACIÓN | 3 × 4 | ~1342 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 91 | 2 | lila | P129 | lila | IX | 157 | 4 | VERIFICACIÓN | 3 × 4 | ~1442 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 92 | 2 | lila | P84 | lila | IX | 177 | 4 | VERIFICACIÓN | 3 × 4 | ~1554 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 93 | 2 | lila | P99 | lila | XI | 188 | 4 | VERIFICACIÓN | 3 × 4 | ~1616 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 94 | 2 | lila | P114 | lila | XI | 202 | 4 | VERIFICACIÓN | 3 × 4 | ~1694 (r) | sin código propio: se mide con el del rojo (4); no entra en el ajuste (PA-15) |
| 95 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 96 | 2 | rojo-I | P39 | rojo | I | 46 | b | VERIFICACIÓN | 3 × 4 | 734,3 | verificar; ajuste sólo con recta anclada (PA-14) |
| 97 | 2 | rojo-I | P38 | rojo | I | 52 | b | VERIFICACIÓN | 3 × 4 | 743,9 | verificar; ajuste sólo con recta anclada (PA-14) |
| 98 | 2 | rojo-I | P49 | rojo | I | 81 | b | VERIFICACIÓN | 3 × 4 | 797,2 | verificar; ajuste sólo con recta anclada (PA-14) |
| 99 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 100 | 2 | naranja | P70 | naranja | IV | 80 | 6 | AJUSTE | 3 × 4 | ~1050 (f) |  |
| 101 | 2 | naranja | P71 | naranja | IV | 84 | 6 | AJUSTE | 3 × 4 | ~1070 (f) |  |
| 102 | 2 | naranja | P72 | naranja | IV | 93 | 6 | AJUSTE | 3 × 4 | ~1116 (f) |  |
| 103 | 2 | naranja | P130 | naranja | IX | 98 | 6 | AJUSTE | 3 × 4 | ~1140 (f) |  |
| 104 | 2 | naranja | P131 | naranja | IX | 102 | 6 | AJUSTE | 3 × 4 | ~1159 (f) |  |
| 105 | 2 | naranja | P132 | naranja | IX | 117 | 6 | AJUSTE | 3 × 4 | ~1228 (f) |  |
| 106 | 2 | naranja | P85 | naranja | IX | 118 | 6 | AJUSTE | 3 × 4 | ~1232 (f) |  |
| 107 | 2 | naranja | P86 | naranja | IX | 124 | 6 | RE-MEDIDA | 5 × 4 | ~1259 (f) |  |
| 108 | 2 | naranja | P87 | naranja | IX | 131 | 6 | AJUSTE | 3 × 4 | ~1289 (f) |  |
| 109 | 2 | naranja | P100 | naranja | XI | 143 | 6 | AJUSTE | 3 × 4 | ~1339 (f) |  |
| 110 | 2 | naranja | P101 | naranja | XI | 146 | 6 | AJUSTE | 3 × 4 | ~1351 (f) |  |
| 111 | 2 | naranja | P115 | naranja | XI | 147 | 6 | AJUSTE | 3 × 4 | ~1355 (f) |  |
| 112 | 2 | naranja | P117 | naranja | XI | 154 | 6 | AJUSTE | 3 × 4 | ~1383 (f) |  |
| 113 | 2 | naranja | P116 | naranja | XI | 158 | 6 | AJUSTE | 3 × 4 | ~1399 (f) |  |
| 114 | 2 | naranja | P102 | naranja | XI | 173 | 6 | AJUSTE | 3 × 4 | ~1457 (f) |  |
| 115 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 116 | 2 | naranja-I | P32b | naranja | I | 68 | d | VERIFICACIÓN | 3 × 4 | 802,8 | P32 duplicado en la fuente: leer etiqueta y color en voz alta (C-41) |
| 117 | 2 | naranja-I | P50 | naranja | I | 71 | d | VERIFICACIÓN | 3 × 4 | 907,1 |  |
| 118 | 2 | naranja-I | P33 | naranja | I | 72 | d | VERIFICACIÓN | 3 × 4 | 849,8 |  |
| 119 | 2 | naranja-I | P41 | naranja | I | 73 | d | VERIFICACIÓN | 3 × 4 | 898,7 |  |
| 120 | 2 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 121 | 2 | A5-FIN | P22 (A5) | amarillo | IV | 334 | e | control | 3 × 4 | 1497 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 122 | 2 | A5-FIN | P28 (A5) | blanco | IX | 484 | e | control | 3 × 4 | 2277,9 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 123 | 2 | A5-FIN | P4 (A5) | blanco | XI | 828 | e | control | 3 × 4 | 3315,8 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 124 | 2 | OSCURO fin | OSCURO |  |  | 0 | e | control | 3 × 4 | 565 | deriva del oscuro |
| 125 | 2 | EXPORTAR |  |  |  |  |  | control |  |  | ZIP de la sesión; copia en `Download/RTV/` |
| 126 | 2 | PAUSA |  |  |  |  |  | control |  |  | si se apaga el equipo o se cambia la batería: 10 min de calentamiento al volver |
| 127 | 3 | BATERIA |  |  |  |  | 9 | control |  |  | orden 9: tensión y nivel; aviso si baja (RF-CAL-41) |
| 128 | 3 | OSCURO inicio | OSCURO |  |  | 0 | e | control | 3 × 4 | 565 | superficie negra mate; ancla de la recta del código 2 (y del 5 si PA-14) |
| 129 | 3 | A5-INICIO | P22 (A5) | amarillo | IV | 334 | e | control | 3 × 4 | 1497 | puente y s_rep (P9-A5); no entra en el ajuste |
| 130 | 3 | A5-INICIO | P28 (A5) | blanco | IX | 484 | e | control | 3 × 4 | 2277,9 | puente y s_rep (P9-A5); no entra en el ajuste |
| 131 | 3 | A5-INICIO | P4 (A5) | blanco | XI | 828 | e | control | 3 × 4 | 3315,8 | puente y s_rep (P9-A5); no entra en el ajuste |
| 132 | 3 | verde | P65 | verde | IV | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 133 | 3 | verde | P64 | verde | IV | 54 | 3 | AJUSTE | 3 × 4 | ~654 (o) |  |
| 134 | 3 | verde | P63 | verde | IV | 121 | 3 | AJUSTE | 3 × 4 | ~764 (o) |  |
| 135 | 3 | verde | P124 | verde | IX | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 136 | 3 | verde | P125 | verde | IX | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 137 | 3 | verde | P79 | verde | IX | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 138 | 3 | verde | P80 | verde | IX | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 139 | 3 | verde | P78 | verde | IX | 97 | 3 | AJUSTE | 3 × 4 | ~725 (o) |  |
| 140 | 3 | verde | P123 | verde | IX | 115 | 3 | RE-MEDIDA | 5 × 4 | ~754 (o) |  |
| 141 | 3 | verde | P109 | verde | XI | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 142 | 3 | verde | P110 | verde | XI | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 143 | 3 | verde | P94 | verde | XI | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 144 | 3 | verde | P95 | verde | XI | 51 | 3 | AJUSTE | 3 × 4 | ~649 (o) | uno de los 9 verdes certificados en 51 exacto: confirmar certificado |
| 145 | 3 | verde | P93 | verde | XI | 85 | 3 | AJUSTE | 3 × 4 | ~705 (o) |  |
| 146 | 3 | verde | P108 | verde | XI | 96 | 3 | AJUSTE | 3 × 4 | ~723 (o) |  |
| 147 | 3 | verde | P15 | verde | XI | 164 | 3 | AJUSTE | 3 × 4 | 843,2 |  |
| 148 | 3 | verde | P13 | verde | XI | 168 | 3 | AJUSTE | 3 × 4 | 830,9 |  |
| 149 | 3 | verde | P16 | verde | XI | 170 | 3 | AJUSTE | 3 × 4 | 846,7 |  |
| 150 | 3 | verde | P14 | verde | XI | 173 | 3 | AJUSTE | 3 × 4 | 849,1 |  |
| 151 | 3 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 152 | 3 | verde-I | P40 | verde | I | 6 | a | VERIFICACIÓN | 3 × 4 | 624,7 | a menos de 80 cuentas del oscuro |
| 153 | 3 | verde-I | P45 | verde | I | 7 | a | VERIFICACIÓN | 3 × 4 | 615,6 | a menos de 80 cuentas del oscuro |
| 154 | 3 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 155 | 3 | azul | P66 | azul | IV | 89 | 5 | VERIFICACIÓN | 3 × 4 | ~837 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 156 | 3 | azul | P67 | azul | IV | 95 | 5 | VERIFICACIÓN | 3 × 4 | ~856 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 157 | 3 | azul | P126 | azul | IX | 91 | 5 | VERIFICACIÓN | 3 × 4 | ~843 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 158 | 3 | azul | P81 | azul | IX | 92 | 5 | VERIFICACIÓN | 3 × 4 | ~846 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 159 | 3 | azul | P127 | azul | IX | 97 | 5 | VERIFICACIÓN | 3 × 4 | ~862 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 160 | 3 | azul | P82 | azul | IX | 98 | 5 | VERIFICACIÓN | 3 × 4 | ~865 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 161 | 3 | azul | P17 | azul | XI | 83 | 5 | VERIFICACIÓN | 3 × 4 | 828,4 | AJUSTE sólo si PA-14 = recta anclada |
| 162 | 3 | azul | P18 | azul | XI | 84 | 5 | VERIFICACIÓN | 3 × 4 | 813,4 | AJUSTE sólo si PA-14 = recta anclada |
| 163 | 3 | azul | P19 | azul | XI | 85 | 5 | VERIFICACIÓN | 3 × 4 | 823,6 | AJUSTE sólo si PA-14 = recta anclada |
| 164 | 3 | azul | P111 | azul | XI | 93 | 5 | VERIFICACIÓN | 3 × 4 | ~849 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 165 | 3 | azul | P96 | azul | XI | 95 | 5 | VERIFICACIÓN | 3 × 4 | ~856 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 166 | 3 | azul | P112 | azul | XI | 101 | 5 | VERIFICACIÓN | 3 × 4 | ~874 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 167 | 3 | azul | P97 | azul | XI | 102 | 5 | VERIFICACIÓN | 3 × 4 | ~877 (o) | AJUSTE sólo si PA-14 = recta anclada |
| 168 | 3 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 169 | 3 | azul-I | P42 | azul | I | 7 | c | VERIFICACIÓN | 3 × 4 | 592,6 | a menos de 80 cuentas del oscuro |
| 170 | 3 | azul-I | P32a | azul | I | 9 | c | VERIFICACIÓN | 3 × 4 | 630,9 | P32 duplicado en la fuente: leer etiqueta y color en voz alta (C-41); a menos de 80 cuentas del oscuro |
| 171 | 3 | azul-I | P46 | azul | I | 9 | c | VERIFICACIÓN | 3 × 4 | 644,3 | a menos de 80 cuentas del oscuro |
| 172 | 3 | azul-I | P47 | azul | I | 9 | c | VERIFICACIÓN | 3 × 4 | 606,3 | a menos de 80 cuentas del oscuro |
| 173 | 3 | azul-I | P48 | azul | I | 9 | c | VERIFICACIÓN | 3 × 4 | 605,4 | a menos de 80 cuentas del oscuro |
| 174 | 3 | azul-I | P36 | azul | I | 10 | c | VERIFICACIÓN | 3 × 4 | 638,8 | a menos de 80 cuentas del oscuro |
| 175 | 3 | BATERIA |  |  |  |  | 9 | control |  |  | control al cambiar de grupo |
| 176 | 3 | A5-FIN | P22 (A5) | amarillo | IV | 334 | e | control | 3 × 4 | 1497 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 177 | 3 | A5-FIN | P28 (A5) | blanco | IX | 484 | e | control | 3 × 4 | 2277,9 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 178 | 3 | A5-FIN | P4 (A5) | blanco | XI | 828 | e | control | 3 × 4 | 3315,8 | deriva de la sesión frente a A5-INICIO (RF-CAL-36) |
| 179 | 3 | OSCURO fin | OSCURO |  |  | 0 | e | control | 3 × 4 | 565 | deriva del oscuro |
| 180 | 3 | EXPORTAR |  |  |  |  |  | control |  |  | ZIP de la sesión; copia en `Download/RTV/` |

---

## 2. Tiempo estimado

**Modelo**, con lo medido el 19-sep-2026 en las series S053-S060 de `campana.csv` (ZIP de las 12:00):
cada colocación de 9 disparos tardó 12-13 s (≈ 1,5 s por disparo), entre colocaciones pasaron 8-13 s
y entre patrones, 10-23 s. Se toman 1,5 s por disparo, 11 s por recolocación y 20 s por cambio de
patrón.

- Un patrón a K × M: K·(M + 1)·1,5 + (K − 1)·11 + 20 s. **3 × 4: 64,5 s. 5 × 4: 101,5 s.**
- Comprobación del modelo: la campaña de las 09:57-10:33 midió 55 series de 1 × 9 en 36 min, 39 s por
  serie. El modelo da 35 s.

| Variante | Qué mide | Patrones | Estimación |
| :--- | :--- | ---: | ---: |
| **Banco completo (esta cola)** | Los 133, con OSCURO y A5 al inicio y al final de cada sesión | 133 | **≈ 190 min en 3 sesiones: 74 (con 10 de calentamiento), 63 y 53** |
| Banco completo a 5 × 4 (referencia) | Los 133 a K = 5 | 133 | ≈ 225 min sólo de patrones, más ≈ 40 de control: **≈ 4 h 25 min** |
| Sólo ajuste, códigos 1, 2 y 8 (los decididos) | Blanco y amarillo intensos y amarillo tipo I | 44 | 51 min de patrones. **≈ 72 min en una sesión, u ≈ 81 min en dos** (recomendado: sesión 1 blanco y amarillo tipo I, sesión 2 amarillo intenso) |
| Sólo ajuste, todos los ajustables (1, 2, 3, 4, 6, 8, y 5 si PA-14) | Sin los de sólo verificar | 107 | 121 min de patrones. **≈ 160 min en 3 sesiones** |
| Verificación rápida | OSCURO, A5 y un patrón por código (los de RE-MEDIDA, más P35, P40, P49, P36 y P41), a 3 × 4 | 12 | **≈ 27 min**, calentamiento incluido |
| Fase B (escritura y re-medida) | Por código: `#S`, `#G`, `#E` y re-medida a 5 × 4 con `e` y código alternados, más persistencia | — | ≈ 2,4 min por código. **≈ 17 min** para 6 códigos |

**Control por sesión:** OSCURO y A5 al inicio y al final suman 8,6 min por sesión (3 × 4). En la
primera son 5 × 4 al inicio y suman 11,1 min, más 10 de calentamiento.

---

## 3. Por código

Regla de la app: patrones del color y la clase del código, grado máximo = mín(2 ; niveles distintos − 2),
y "sólo verificar" si el rango certificado es menor de 20 unidades o del 30 % del mayor
(`Asistente.java:33-36`, `:75-98`). El grado que se propone es otra cosa: sale de las decisiones de
Diego y de `SPEC-Calibracion-V3.6.md` §4.4.

### 3.0 Resumen

| Código | Patrones | Niveles | R cert. | x (medida o estimada) | Regla de la app | Método propuesto | RE-MEDIDA |
| :---: | ---: | ---: | :--- | :--- | :--- | :--- | :--- |
| **1** blanco intenso | 16 | 16 | 347-828 | 1674-3316 (8 medidos) | Hasta grado 2 | **Grado 1 (decidido, escrito el 19-sep)** | **P28** (IX 484) |
| **2** amarillo intenso | 24 | 24 | 207-782 | ~1132-2788 (14 medidos) | Hasta grado 2 | **Recta anclada en el oscuro (decidido, escrito el 19-sep)** | **P25** (IX 576) |
| **3** verde intenso | 19 | 11 | 51-173 | ~649-849 (4 medidos) | Hasta grado 2 | Grado 1 (PA-16) | **P123** (IX 115) |
| **4** rojo intenso | 16 | 15 | 68-279 | ~945-2124 (2 medidos) | Hasta grado 2 | Grado 1 (PA-16). Sin café ni lila (PA-15) | **P11** (IV 194) |
| 5 azul intenso | 13 | 12 | 83-102 | 813-877 (3 medidos) | **No ajustable: rango 19 % < 30 %** | Sólo verificar. Ajustable sólo con recta anclada (PA-14) | P81 (IX 92), si PA-14 |
| **6** naranja intenso | 15 | 15 | 80-173 | ~1050-1457 (0 medidos) | Hasta grado 2 | Grado 1 (PA-16) | **P86** (IX 124) |
| 7 blanco tipo I | 1 | 1 | 96 | 1115 | No: 1 nivel | Sólo verificar | — |
| **8** amarillo tipo I | 4 | 4 | 64-122 | 899-1151 (4 medidos) | Hasta grado 2 | Grado 1 (P-CAL-05), con C-CAL-15 | **P43** (I 122) |
| a verde tipo I | 2 | 2 | 6-7 | 616-625 | No: 2 niveles | Sólo verificar | — |
| b rojo tipo I | 3 | 3 | 46-81 | 734-797 | Hasta grado 1 | Sólo verificar: la recta libre no pasa `#S` (propuesta del 19-sep, §2.4). Recta anclada si PA-14 | — |
| c azul tipo I | 6 | 3 | 7-10 | 593-644 | No: rango de 3 | Sólo verificar | — |
| d naranja tipo I | 4 | 4 | 68-73 | 803-907 | No: rango 7 % | Sólo verificar | — |
| (4) café y lila | 10 | 10 | 36-202 | ~767-1694 (r) | No se cuentan (otro color) | Verificar con el 4 (PA-15) | — |

**El encargo decía que "los códigos 3, 4, 5 y 6 pasan a ser ajustables". Con la regla de la app, 3, 4
y 6 sí; el 5 no.** El azul intenso va de 83 a 102: 19 unidades, el 19 % del mayor, por debajo de los
dos umbrales (`Asistente.java:34`, `:36`, `:93`). Hoy la fábrica lee el azul intenso un 55-57 % por
debajo del certificado (`PROPUESTA-Ajuste-SLV-002-2026-09-19.md`, §5), así que dejarlo en "sólo
verificar" deja el código peor calibrado del equipo tal como está. La salida que respeta la regla es la
misma que se decidió para el 2, la recta anclada en el oscuro. Con el ancla, el rango pasa a ser de 0
a 102 y la pendiente la fija la distancia al oscuro (unas 250 cuentas), no el ancho del grupo de
azules. **Pero la app de hoy también la bloquea**: `Asistente.proponer` comprueba la cobertura del
catálogo con el grado 1 antes de ajustar, también para la recta anclada (`Asistente.java:406-422`).
Hay que cambiar esa regla (RF-APP-42) y que Diego lo decida (PA-14).

### 3.1 Código 1, blanco intenso (grado 1, escrito el 19-sep)

- **Ajuste:** P56, P3, P2, P52, P58, P51 (IV); P27, P28, P118, P73 (IX); P1, P103, P7, P6, P88, P4 (XI).
- **Rango:** R 347-828. `x` medida de 1674 (P3) a 3316 (P4). Los 8 nuevos se estiman dentro: 1717-3130 (o).
- **Extrapolación:** por encima de 828 (x > 3316) hasta x = 4300, donde la curva escrita da R ≈ 1121.
  Por debajo de 347, la curva no tiene patrones hasta el oscuro. La recta escrita da R = 6,5 en
  x = 565,4 (cálculo sobre `#G,1,…,2.98471571E-01,-1.62263885E+02#`, acta de las 12:23, línea 10), así
  que entre el oscuro y P56 interpola, pero **no está calibrada** (P9-B13).
- **RE-MEDIDA: P28.** Es un patrón de la A5, así que su s_rep está medida (2,70 %, `resumen.txt:188`).
  Además, repetirlo da continuidad con la re-medida del 19-sep.

### 3.2 Código 2, amarillo intenso (recta anclada, escrito el 19-sep)

- **Ajuste:** P61, P62, P22, P21, P29, P30, P24 (IV); P121, P122, P77, P76, P31, P25, P26 (IX); P107,
  P106, P91, P92, P10, P9, P23, P8, P5, P20 (XI).
- **Rango:** R 207-782. `x` medida de 1497 (P22) a 2788 (P20); los nuevos bajan a ~1132 (P61, o).
- **Extrapolación:** por encima de 782 (x > 2788). Entre el oscuro y 207 la recta pasa por el ancla,
  que está medida: es interpolación.
- **RE-MEDIDA: P25** (IX, 576, a media altura). El 19-sep se re-midió P5, que es un XI desordenado
  frente a P23 (C-39). Con la curva escrita, P25 queda a +1,0 % del certificado y P5 a −6,5 %.
  P5 se sigue midiendo en el banco como AJUSTE.

### 3.3 Código 3, verde intenso

- **Ajuste:** P65, P64, P63 (IV); P79, P80, P124, P125, P78, P123 (IX); P94, P95, P109, P110, P93, P108,
  P15, P13, P16, P14 (XI).
- **Rango:** R 51-173, pero 9 de los 19 están en **51 exacto** (§5.3). `x` medida sólo en P13-P16
  (831-849). Las demás se estiman en 649-764 (o): **toda la parte baja está sin medir**.
- **Grado admisible:** 2 por la regla (11 niveles). Propuesto: 1.
- **Extrapolación:** por encima de 173 (x > 849). **Riesgo con `#S`:** los verdes de 51 caen, estimados,
  a ~84 cuentas del oscuro. Si la recta libre corta el cero por encima de x = 600, `#S` la rechaza
  (R(600) < 0; `calibracion_v36.c`, límites de RF-FW-31). Entonces hace falta la recta anclada
  (PA-16).
- **RE-MEDIDA: P123** (IX, 115, a media altura del rango; sin medir todavía).

### 3.4 Código 4, rojo intenso

- **Ajuste:** P60, P54, P59, P57, P11, P12 (IV); P120, P75, P119, P74 (IX); P55, P90, P105, P104, P89,
  P53 (XI).
- **Rango:** R 68-279. `x` medida sólo en P11 y P12 (1723 y 1762). El resto, estimado en 945-2124 (o),
  con una pendiente que sale de sólo dos patrones.
- **Grado admisible:** 2 por la regla. Propuesto: 1.
- **Extrapolación:** por encima de 279 (x > ~2124) y por debajo de 68 (x < ~945) hasta el oscuro.
- **RE-MEDIDA: P11** (IV, 194). Está medido y queda a media altura.
- **Café y lila (P68, P69, P83, P84, P98, P99, P113, P114, P128, P129)** se miden con el 4 y **no entran**
  (PA-15).

### 3.5 Código 5, azul intenso (no ajustable por la regla)

- **Patrones:** P66, P67 (IV); P126, P81, P127, P82 (IX); P17, P18, P19, P111, P96, P112, P97 (XI).
- **Rango:** R 83-102. `x` medida en P17-P19 (813-828); los nuevos se estiman en 837-877 (o).
- **Por qué no:** el rango de R es del 19 % y el de `x`, de unas 64 cuentas, del orden de 3 s_rep a
  esa altura. Una recta libre sobre ese grupo queda sin pendiente.
- **Con recta anclada (PA-14):** la pendiente la fija el oscuro, a 250-310 cuentas. RE-MEDIDA: **P81**
  (IX, 92).

### 3.6 Código 6, naranja intenso

- **Ajuste:** P70, P71, P72 (IV); P130, P131, P132, P85, P86, P87 (IX); P100, P101, P115, P117, P116, P102 (XI).
- **Rango:** R 80-173. **Ningún naranja intenso se ha medido nunca.** La `x` se estima con la curva de
  fábrica invertida (1050-1457, f), que en los medidos falla hasta un 40 % (§5.5).
- **Grado admisible:** 2 por la regla. Propuesto: 1. La cobertura real sólo se sabrá tras el banco.
- **RE-MEDIDA: P86** (IX, 124).

### 3.7 Código 8, amarillo tipo I

- **Ajuste:** P44 (64), P37 (82), P34 (86), P43 (122), todos a **5 × 4**: con 4 puntos, cada uno pesa.
- **Rango:** `x` medida 899-1151. Sin extrapolar hasta 122. Por debajo, hasta el oscuro.
- **Grado admisible:** 2 por la regla (4 niveles). Propuesto: 1 (P-CAL-05). Además sigue abierta
  C-CAL-15: no hay certificado de los tipo I.
- **RE-MEDIDA: P43**, el que más cuentas da sobre el oscuro. **El código 8 no se escribió el 19-sep.**

### 3.8 Código b, rojo tipo I

- **Patrones:** P39 (46), P38 (52), P49 (81). `x` 734-797: 63 cuentas de rango.
- **Regla:** hasta grado 1. **Propuesto: sólo verificar.** La recta libre da R(600) = −27,8 y `#S` la
  rechaza (`PROPUESTA-Ajuste-SLV-002-2026-09-19.md`, §2.4). Con recta anclada, la pendiente la fijaría
  el oscuro, a 170-230 cuentas (PA-14).

### 3.9 Café y lila (con el código 4)

**Recomendación: sólo verificar, con el error declarado.** Diego decide (PA-15). La evidencia es
indirecta, porque **ninguno se ha medido**:

1. La relación entre `x` y R depende del color. Con lo medido el 19-sep, la pendiente de la recta por el
   oscuro va de 0,18 R por cuenta (rojo intenso) a 0,61 (verde intenso). Café y lila no tienen ningún
   punto propio: no se sabe dónde caen frente a la recta del rojo.
2. Serían 10 puntos frente a los 16 del rojo. Si su pendiente fuera distinta, moverían la curva del
   rojo en todo el rango.
3. La decisión C es "todos los patrones **del color**". Meterlos sería otra decisión.
4. **Cómo se cierra, midiendo:** con el banco, se calcula el residuo de cada café y lila frente a la
   curva del rojo. Si los 10 cumplen RF-CAL-14 (10 %), se pueden añadir en otra calibración sin cambiar
   nada del flujo. Si no, el acta declara su error con el código 4.

`Asistente.deCodigo` los deja fuera del ajuste por construcción, porque compara el color
(`Asistente.java:39-45`). Para que "se midan con el 4", la app necesita la tabla color → código de la
cola, y no sólo `Fabrica.color()` (RF-APP-47). **Darles códigos propios exige cambiar el firmware y la
pantalla STONE**, y está fuera de alcance (`SPEC-Calibracion-V3.6.md` §12.9).

---

## 4. Por qué K = 3 y no K = 5 en casi todo

Con la s entre colocaciones medida en la A5 del 19-sep, **s_rep = 2,24 %** (media de P22, P28 y P4:
1,47 %, 2,70 % y 2,56 %; `resumen.txt:187-191`), la desviación de la media de K colocaciones es
2,24/√K: **2,24 % con K = 1; 1,29 % con K = 3; 1,00 % con K = 5; 0,75 % con K = 9.**

- **En los códigos con muchos patrones (1, 2, 3, 4 y 6, de 15 a 24), la curva la fija el conjunto.** Con
  16 puntos a K = 3, el ruido de colocación que queda en la curva es del orden de 1,29/√16 ≈ 0,3 %. Los
  residuos por tipo que se quieren juzgar son del 2,8 al 10 % (acta del 19-sep, líneas 11 y 18). Pasar
  los 88 de AJUSTE a K = 5 cuesta 37 s por patrón, unos 54 min más de banco, para bajar ese 0,3 % a 0,25 %.
- **Donde un solo punto manda, K = 5:**
  - el **OSCURO**, porque es el ancla del 2 y su error se traslada entero a la parte baja de la curva;
  - la **A5 del inicio**, porque es la s_rep que Diego fijó como criterio (4 grados de libertad por
    patrón, el mínimo razonable, `PROPUESTA-Ajuste-SLV-002-2026-09-19.md` §6);
  - el **patrón de RE-MEDIDA** de cada código, porque es la referencia con la que se compara la Fase B;
  - los **4 del código 8**, porque con 4 puntos cada uno pesa un 25 %.
- **K = 3 es el mínimo que detecta una mala colocación.** Con K = 1 no hay s entre colocaciones, y la
  app tiene que suponerla. Así se hizo el 19-sep: "s_rep 14,9 (supuesta)" en el acta, línea 14. Con
  K = 3, `Veredicto.sEntre` y `REPRO_MAX` = 3 % (`Veredicto.java:71`) marcan la serie para repetir.
- **Recolocar vale más que disparar.** La s de disparo dentro de una serie es de ~0,24 %
  (`PROPUESTA…` §6). Con M = 4 el error de disparo de una colocación es de ~0,12 %, invisible al
  lado del 2,24 %.

---

## 5. Patrones dudosos o especiales

### 5.1 P32a / P32b

La hoja fuente lleva **dos filas "p32"** (`Datos certificados…xlsx`, Hoja1, filas 33-34; y Hoja2,
filas 1-2): azul tipo I 9 y naranja tipo I 68. En la cola son **P32a (código c)** y **P32b (código d)**.
Se distinguen sin ambigüedad por color y por `x` (630,9 y 802,8), pero la identidad física sigue
abierta (C-41). **La cola pide leer la etiqueta y el color en voz alta.** Los dos son de VERIFICACIÓN:
el error no puede mover ninguna curva.

### 5.2 Azules y verdes tipo I, pegados al oscuro

El oscuro está en x = 565,4, con una s entre colocaciones de 2,0 cuentas.

| Patrón | Código | Cert. | `x` medida | Sobre el oscuro | Observación |
| :--- | :---: | ---: | ---: | ---: | :--- |
| P42 | c | 7 | 592,6 | 27 | El más cercano al oscuro |
| P48 | c | 9 | 605,4 | 40 | **Indistinguible de P47** (Δ 0,9) |
| P47 | c | 9 | 606,3 | 41 | " |
| P45 | a | 7 | 615,6 | 50 | Lee **menos** que P40 con más certificado (Δ 9 cuentas) |
| P40 | a | 6 | 624,7 | 59 | " |
| P32a | c | 9 | 630,9 | 66 | |
| P36 | c | 10 | 638,8 | 73 | |
| P46 | c | 9 | 644,3 | 79 | Mismo certificado que P47 y P48 y 38 cuentas más |

- **Entre sí, en R, no se distinguen**: los certificados van de 6 a 10 y la salida del equipo tiene una
  resolución de 1 unidad (`SPEC-Calibracion-V3.6.md` §1.1). Los tres azules de 9 cubren 39 cuentas.
- **Casi todos caen por debajo de x = 600**, el límite inferior de `#S` (RF-FW-31), o en el borde. Por
  eso a y c sólo se verifican.
- **Intensos, en el mismo caso:** los azules XI P17-P19 (83-85) leen 813-828 **sin orden** (P18 da la
  `x` más baja con el certificado del medio). Los verdes XI P13-P16 (164-173) leen 831-849, también sin
  orden (P13 da menos que P15). Los azules nuevos (89-102) se estiman en 837-877 (o), mezclados con
  ellos. **Verdes y azules XI caen en la misma franja de `x`** (813-849): si se cambia la etiqueta
  entre un verde y un azul, la `x` no lo delata. Por eso la cola los separa en grupos distintos.

### 5.3 Nueve verdes certificados en 51 exacto

P65 (IV), P79, P80, P124, P125 (IX), P94, P95, P109, P110 (XI): **51 los nueve**, en los tres tipos. Es
la única cifra repetida tantas veces en todo el catálogo, y en tres tipos que en los demás colores dan
valores distintos. Puede ser un valor real o un relleno de la hoja. **No se decide aquí**: se mide, y
si los nueve dan `x` muy distintas entre sí, es una señal. Pesan en el ajuste del código 3: 9 de 19
puntos en el mismo nivel. Lo decide Diego con el certificado delante (PA-16).

### 5.4 P2/P3 y P23/P5

- **P2 (IV, 414) y P3 (IV, 378):** 36 unidades de certificado y 385 cuentas de `x` (2058,6 frente a
  1674,0). Con la pendiente de la curva escrita, 0,298 R por cuenta, 385 cuentas son 115 unidades. P2 lee
  casi lo mismo que P27 (IX, 471; 2111,1). Uno de los dos no es lo que dice su certificado, o no es el
  patrón que dice su etiqueta. **Los cuatro IV blancos nuevos (P51 439, P52 421, P56 347, P58 432)
  dirán cuál se sale de la fila.**
- **P23 (XI, 714) y P5 (XI, 740):** P23 lee 2735,4 y P5 2459,3: un 11 % más con menos certificado. P5
  girado de 0 a 90° da 2456,8 y 2459,3, así que la orientación no lo explica (C-39). Los XI amarillos
  nuevos (P91 516, P92 522, P106 493, P107 478) dirán si el desordenado es P5 o P23.
- **P24** sigue con la identidad sin confirmar (C-40), y **P22** salió FUERA en la A5 del 19-sep
  (−5,55 %, `resumen.txt:187`). Se quedan en la cola, con nota.

### 5.5 Por qué la `x` esperada no sale de la curva de fábrica

El encargo pedía estimar la `x` de P51-P132 con la curva de fábrica invertida. **En los 50 patrones
medidos, esa estimación falla mucho**: P2 da 1452 frente a 2058,6 medida (−29 %), P28 da 1651 frente a
2277,9 (−28 %), P17 da 1146 frente a 828,4 (+38 %) y P49 da 1198 frente a 797,2 (+50 %). Además, la
fábrica del blanco no llega a 828 (P4 no tiene inversa). Esto cuadra con que la fábrica lee IV/IX del
blanco un 25-50 % alto y el azul un 55 % bajo (L-07).

Por eso la cola usa, donde hay medidos del mismo color y clase, **la recta por el oscuro ajustada a
ellos**:

| Color y clase | Medidos | Pendiente (R por cuenta) |
| :--- | ---: | ---: |
| blanco intenso | 8 | 0,3014 |
| amarillo intenso | 14 | 0,3655 |
| rojo intenso | 2 | 0,1790 |
| verde intenso | 4 | 0,6088 |
| azul intenso | 3 | 0,3274 |
| blanco, amarillo, rojo, verde, azul y naranja tipo I | 1, 4, 3, 2, 6 y 4 | 0,1748 · 0,2043 · 0,3139 · 0,1172 · 0,1482 · 0,2334 |

La curva de fábrica sólo se usa en el naranja intenso, que no tiene ningún medido. La columna
`x_esperada` sirve para una cosa: que el aviso "¿es este el patrón?" (`Veredicto`) tenga una referencia
antes de medir. **No entra en ningún ajuste.** Si la app la usa para ese aviso, la tolerancia debe
ser amplia en las filas estimadas: ±30 % en (o) y (r), y sin aviso en (f).

---

## 6. Formato de la cola para la app

`cola_banco_P1-P132.csv`, UTF-8, separador coma, una cabecera y una fila por paso, en el orden de
ejecución:

| Columna | Contenido |
| :--- | :--- |
| `orden` | 1…180, orden de ejecución |
| `sesion` | 1, 2 o 3 |
| `bloque` | `INICIO`, `A5-INICIO`, `COLOR:<color>[-I]`, `A5-FIN`, `FIN` |
| `paso` | `CALENTAMIENTO`, `BATERIA`, `OSCURO`, `A5`, `PATRON`, `EXPORTAR`, `PAUSA` |
| `patron`, `color`, `tipo`, `valor_certificado` | Del catálogo. `OSCURO` con certificado 0 |
| `codigo_equipo` | La curva a la que va el punto (1-8, a-d). `e` en A5 y OSCURO. `9` en BATERIA |
| `uso` | `AJUSTE`, `RE-MEDIDA`, `VERIFICACION`, `CONTROL` |
| `K`, `M`, `asentamiento` | Colocaciones, disparos por colocación y disparos descartados por colocación |
| `remedida_de_codigo` | El código para el que este patrón es el de re-medida (vacío si no lo es) |
| `x_esperada`, `origen_x` | §5.5 |
| `nota` | Texto para el operador. En ASCII, sin tildes, para que no dependa de la codificación |

**Cambiar la cola no exige compilar**, pero sí un commit con su md5, y el acta cita ese md5 (P9-B2). La
app no admite una cola cuyo md5 no conozca: la lista de md5 admitidos va dentro del APK (RF-APP-33).
