# Cotejo de las fórmulas de calibración de SLV-002 con las históricas del Vertical (19-sep-2026)

**Sin validar en hardware y sin escribir nada en el equipo.** Es una lectura de código y de hojas de
cálculo, más un cálculo en papel sobre las medidas del 19-sep-2026. Ninguna curva histórica se ha
medido hoy contra un patrón, y la hipótesis del LED (§4.3) sale de una hoja de pruebas de 2020, no de
una medida de SLV-002.

Cálculos: `06_Calibracion/SLV-002/tools/cotejo_formulas_historicas.py` (numpy y openpyxl). Imprime
por pantalla y no escribe nada. Las cifras de §2, §3 y §5 salen de él.

## 0. Resumen

- **Las 12 ecuaciones de fábrica de 2020 salen de una sola hoja de Excel**, la de «concesion sabana
  occidente» de `06_Calibracion/2020_calibraciones VERTICALES.xlsx`. El script las reproduce una por
  una (§2.1). Cada ajuste mete el oscuro del equipo de 2020 como punto `(488 ; 0)`, y **ocho de
  los nueve le añaden un punto inventado, `(3000 ; 800)` o `(4000 ; 800)`**, que ningún patrón
  respalda.
- **No hubo nunca una recta en la línea K42 de 2020**, ni para el rojo ni para el azul. El rojo
  intenso es una cúbica, el rojo tipo I una parábola y el azul una cúbica. Esa cúbica del azul es
  la misma para el intenso y para el tipo I (`ecuacionesCalibracion.c:16,38`). La V4.1 sí usa rectas,
  pero **el azul de la V4.1 no tiene datos de azul**: es la recta del verde menos 60
  (`Ecuaciones.c:66`). En el 18F4550 (2012-2018) sí hubo rectas, a veces en dos tramos. **Se
  calibraban equipo por equipo**, y su pendiente de rojo y de azul varía en dos órdenes de magnitud
  entre equipos (§2.3).
- **Por qué la fábrica del azul intenso (5) lee −55/−58 %:** la cúbica se ajustó con azules de
  **R 2 a 20,5**, sin ningún azul intenso. Por encima de x = 685 sólo la sostiene el punto inventado
  `(3000 ; 800)`. En x ≈ 820 su pendiente es 0,125 R por unidad de x, y los P17-P19 piden 0,327.
- **Por qué la fábrica del rojo tipo I (b) lee −71 %:** la parábola se ajustó con tres rojos tipo I
  de **R 10 a 17**, con sensibilidad k ≈ 0,06 R por unidad de x. Los P38, P39 y P49 de hoy dan
  k ≈ 0,27-0,35: **cinco veces más.** El LED no lo explica: iría en sentido contrario (§4.3). Queda
  como **contradicción abierta** (§4.4).
- **Las pendientes de hoy son del mismo orden que las de 2020 en blanco, amarillo y rojo intensos**
  (0,75 a 0,92 veces). **No lo son en azul intenso (3,1 veces) ni en rojo tipo I (5,1 veces).** El
  amarillo tipo I queda en medio, con 1,55 veces.
- **Código 5 con recta anclada en el oscuro (PA-14):** `c1 = 3.27318460E-01`,
  `c0 = -1.85051315E+02` (ancla x = 565,3556). Errores de +2,4, −3,6 y −1,2 % en P17-P19, y
  RMS 2,43 unidades de R. Coincide con la cifra de `REFORMULACION-y-Simulacion-2026-09-19.md` §3.5.
  **Sólo vale cerca de R ≈ 85:** los tres patrones están en 14 cuentas de x.

## 1. Fuentes encontradas

| Fuente | Qué trae | Uso aquí |
| :--- | :--- | :--- |
| `D:\IT\P_RetroVertical_V3.6\01_Firmware\base_2020_d089f962\RetroVertical1.X\ecuacionesCalibracion.c` | Las 12 ecuaciones de 2020 (9 distintas). Es la fábrica de SLV-002 (`Fabrica.java:62-88`) | §2 |
| `...\base_2020_d089f962\RetroVertical1.X\measurement.c`, `gui.c` | Cómo se forma x | §1.1 |
| `D:\IT\P_RetroVertical_V3.6\06_Calibracion\2020_calibraciones VERTICALES.xlsx` | Datos y líneas de tendencia de las ecuaciones de 2020, en dos equipos (hojas «concesion sabana occidente» y «coviandina») | §2, §3 |
| `D:\IT\P_RetroVertical_V3.6\06_Calibracion\2020_medidas.xlsx` | Pruebas de óptica de 2020: luz fría, cálida, láser y óptica gris (hoja `Hoja4`) | §4.3 |
| `D:\IT\P_RetroVertical_V3.6\06_Calibracion\2020_Hoja de referencia (posiblemente regulares).xlsx` | 48 hojas de clientes con medidas de campo. **No trae ecuaciones ni patrones certificados** (sólo se listaron las hojas) | No se usa |
| `D:\@Proyect\IT\P_RetroReflectometro_Vertical\01_Firmware\18f47k42_RetroV_V4.1.X\Ecuaciones.c` (md5 `8b0dbd79...`, igual a la copia de `D:\IT\P_RetroReflectometro_Vertical`) | Rectas de la V4.1 | §2.2 |
| `D:\@Proyect\IT\P_RetroReflectometro_Vertical\06_Calibracion\*_Curva.py` | Regresiones de la V4.1: blanco, amarillo, rojo/naranja y verde. **No hay de azul** | §2.2 |
| `D:\@Proyect\IT\old\VERTICAL\` (18F4550 CCS, V3.3 2020, V4.0 2021) | Ver §2.3 | §2.3 |

### 1.1 La x no es la misma variable en todas las versiones

| Versión | x que entra en la ecuación | Evidencia |
| :--- | :--- | :--- |
| 2020 (K42) y V3.6 (SLV-002) | `x = (unsigned int)((ADC filtrado + 200) · F(T))`, con `F(T) = 0,00043212·T + 0,90148651`. F vale 1 en T = 228 | Base 2020: `measurement.c:247`, `gui.c:41-43,300`. V3.6: `measurement.c:247`, `gui.c:301`, `calibracion_v36.c:312` |
| V4.1 | `VoltajeADC = ADC promedio × 0,1`, sin +200 ni F(T), menos `error`, que es la última lectura de tipo 1 menos 163 | `Aplicacion.c:245,256,270,276`; CLAUDE.md del V5 §4 |

**Normalización usada.** En la familia de 2020 y en la V3.6 la x tiene la misma definición y el
mismo firmware. Lo que cambia de un equipo a otro es el oscuro: 488 en «sabana» (celda `C2`), 450 en
«coviandina» (`F2`, con `C2 = $F$2`) y 565,36 en SLV-002 (S060). Por eso se compara la sensibilidad
**k = R / (x − x_oscuro)**, que es la pendiente de una recta anclada en el oscuro, y no la x en
bruto. F(T) no se puede quitar: no consta la T de 2020 ni la de hoy. En el rango de T de uso, F
mueve la x un pequeño porcentaje (F = 1 ± 0,05 para T de 110 a 340), pero **sin medir**.

La V4.1 lleva otra placa y otra ganancia: 1 unidad de VoltajeADC son 10 cuentas, y el blanco da
2,99 R por cuenta, frente a ~0,30 R por unidad de x en la V3.6. **Sus pendientes absolutas no se
comparan**, sólo las proporciones entre colores.

## 2. Inventario de ecuaciones históricas

### 2.1 Fábrica de 2020 (K42, `ecuacionesCalibracion.c`): origen reconstruido

«Filas» son las de la hoja «concesion sabana occidente»: columna `C` = x («valor lcd»), columna `D` =
R certificada («VALOR ESPERADO»). Las etiquetas de la línea de tendencia están en `A64-A85` y
coinciden con el firmware; el firmware las copia con 9 decimales. El script reajusta cada grupo con
`numpy.polyfit` y reproduce los coeficientes hasta el décimo dígito.

| Código V3.6 | Función (línea) | Forma | Filas | Puntos reales: n, R | Oscuro | Punto inventado |
| :---: | :--- | :--- | :--- | :--- | :---: | :---: |
| 1 | `blancoIntenso` (`:7`) | grado 2 | 20-26, «Blanco O.» | 5, R 271-747 | (488 ; 0) | (4000 ; 800) |
| 2 | `amarilloIntenso` (`:4`) | grado 3 | 9-14, «Amarillo O.» | 5, R 208-870 | (488 ; 0) | — |
| 3 y a | `verdeIntenso` / `verdeOpaco` (`:19,:41`, iguales) | grado 3 | 52-60, «Verde I.» y «Verde O.» juntos | 7, R 5-115 | (488 ; 0) | (3000 ; 800) |
| **4** | **`rojoIntenso` (`:13`)** | **grado 3** | 39-44, «Rojo O.» | **4, R 117-239** | (488 ; 0) | **(3000 ; 800)** |
| **5 y c** | **`azulIntenso` / `azulOpaco` (`:16,:38`, iguales)** | **grado 3** | 45-51, «Azul I.» y «Azul O.» juntos | **5, R 2-20,5** | (488 ; 0) | **(3000 ; 800)** |
| 6 y d | `naranjaIntenso` / `naranjaOpaco` (`:10,:32`, iguales) | grado 2 | 27-33, «Naranja I.» y «O.» juntos | 4, R 33-278 | (488 ; 0) | (3000 ; 800) |
| 7 | `blancoOpaco` (`:29`) | grado 2 | 16-19, «Blanco I.» | 2, R 88-95 | (488 ; 0) | (3000 ; 800) |
| 8 | `amarilloOpaco` (`:26`) | grado 3 | 2-8, «Amarillo I.» | 5, R 28-128 | (488 ; 0) | (3000 ; 800) |
| **b** | **`rojoOpaco` (`:35`)** | **grado 2** | 34-38, «Rojo I.» | **3, R 10-17** | (488 ; 0) | **(3000 ; 800)** |

Coeficientes, tal como están en el firmware:

- Rojo intenso (4): `R = 1,47E-07·x³ − 6,68624E-04·x² + 1,082394050·x − 393` (`ecuacionesCalibracion.c:13`).
- Azul, 5 y c: `R = 2,6E-08·x³ − 1,8402E-05·x² + 0,102152670·x − 49` (`:16` y `:38`).
- Rojo tipo I (b): `R = 1,13098E-04·x² − 0,076130418·x + 10` (`:35`).

Notas:

- En la hoja, **«I.» es tipo I** (R bajas) y **«O.» es la otra lámina** (R altas). El firmware llama
  «Opaco» al tipo I e «Intenso» a la otra. Lo confirma el reajuste: `amarilloOpaco` sale de las filas
  «Amarillo I.».
- **Los recortes** son `arreglar_dato` (`ecuacionesCalibracion.c:49-54`), que da 0 por encima de 4000,
  y el truncado a `unsigned int` (`gui.c:30`). **No hay ningún recorte por color.**
- El propio autor lo dejó escrito: «rojo muy regular» (`:87`, `:116`), «azul regular» (`:92`, `:120`),
  «naranja único que funciona bien» (`:97`). Y en `gui.c:302-305`: «algunas ecuaciones son de primer
  orden, otras de segundo e incluso de tercer orden».
- **Hay un segundo equipo de 2020.** Es la hoja «coviandina», con oscuro 450 (38 cuentas por debajo
  de «sabana») y los mismos patrones. Sus rectas ancladas coinciden con las de «sabana» dentro del
  ±12 % (§3).
- **La c3 de la cúbica tiene sólo 2 cifras significativas** (0,000000026 y 0,000000147). Con ese
  redondeo, la curva del firmware se separa hasta 6,2 unidades de R del ajuste de Excel en x ≤ 3000,
  tanto en el azul como en el rojo intenso.

### 2.2 V4.1 (`Ecuaciones.c`, 18F47K42, placa V4)

Todas son rectas **R = (V − error − b) / m + d**. Salen de regresiones de la lectura sobre la R
(`*_Curva.py:16-17`) que se invierten, y cada una lleva además una suma **`d` que no está en ningún
script**.

| Color | m (V por R) | b | d | Datos del script | Evidencia |
| :--- | ---: | ---: | ---: | :--- | :--- |
| Blanco | 0,033487 | 165,1383 | +150 | `Blanco_Curva.py:6-7`, R 0-748, V 167-195 | `Ecuaciones.c:28` |
| Amarillo | 0,034102 | 167,8763 | +150 | `Amarillo_Curva.py:6-7` | `:20` |
| **Rojo** | **0,080052** | **168,9455** | **+30** | `Rojo_Naranja_Curva.py:6-7`: R 0, 217, 146, 200, 65 frente a V 167, 192, 175, 181, 180. **No es monótona** (el de R 65 lee más que el de R 146) | `:38` |
| Naranja | 0,080052 | 168,9455 | +60 | La del rojo | `:48` |
| Verde | 0,244763 | 251,5204 | +260 | `Verde_Curva.py:6-7`: R 0-124 frente a V 250-278. **Su oscuro es 250**, frente a 167 en los demás scripts | `:57` |
| **Azul** | 0,244763 | 251,5204 | **−60 + 260** | **Ninguno: es la del verde menos 60** | `:66` |

- **El tipo 1 de la V4.1 no tiene ecuación**: devuelve V (`Ecuaciones.c:77-113`). Las fórmulas
  comentadas de `:79-114` son variantes de las del intenso: sin `error` ni la suma `d`, con las
  constantes del blanco en el rojo y el naranja (`:93`, `:100`) y divididas por 9,5 en el verde y el
  azul (`:107`, `:114`).
- El oscuro entra como un punto más del ajuste, el `(0 ; 167)` de cada script. **No se impone.** Es
  lo mismo que hacía 2020 con `(488 ; 0)`.
- Proporciones entre colores, en R por cuenta: rojo/blanco = 12,49 / 29,86 = **0,42**. En SLV-002
  es 0,168-0,190 / 0,2985 = **0,56-0,64**; en 2020, 0,238 / 0,351 = **0,68**. Para el azul no hay
  proporción que dar: la V4.1 no midió ningún azul.

### 2.3 Archivo histórico (`D:\@Proyect\IT\old\VERTICAL\`)

Lo levantó un subagente con `find`, Grep y openpyxl, sin modificar nada. Agrupó por texto de ecuación
los 487 `refle*.c` del 18F4550: salen **60 juegos distintos**. El principal comprobó dos citas a mano
(las marca «verificado»). `[RV]` = `...\1_V2-V3_18F4550_CCS\CALIBRACION RETROS VERTICALES V2 Y V3\Retros verticales\`.

**18F4550 CCS (V1-V3, 2012-2018).**

- **La x es otra variable.** Media de 60 lecturas de un ADC de 10 bits por 4, sin +200, **sin restar
  oscuro** y con corrección por humedad, no por temperatura (`Retro Plus Vial__E_Electronica_ing\Programas pic\refle_vert_2_3.c:121,164,720-772,1100-1137`).
  La 2_3_2 anula la humedad (`[RV]AIM INGENIEROS\SLV-007\14-03-2018\Programa PIC\refle_vert_2_3_2.c:1005`).
  La placa también es otra: **las pendientes no se comparan con las de la V3.6.**
- **Forma: siempre rectas, a veces en dos tramos cortados por un umbral de x o de R.** No hay
  parábolas. Ejemplo de la plantilla V3 (`[RV]MAB_ingenieria\Software pic\refle_vert_2_3.c`,
  verificado en `:1129-1142`):
  - rojo tipo I: `0,31x − 61`, y por encima de R = 10, `0,283x − 54,82`;
  - rojo «otros papeles»: `0,86x − 162`.
- **Las constantes son por equipo.** Se recompilaban por cliente y se ajustaban con la hoja
  `CL V3 FULL SLV-xxx.xlsx`, con SLOPE/INTERCEPT de la R de RoadVista 922 / ART 920 frente a la x del
  equipo. La hoja coincide con el firmware en SLV-007, 0300, 018, 023, 029, 030, 009 y 003. **La de
  SLV-024 no**: copia el rojo de Plusvial y el azul de SLV-018.
- **Dispersión entre equipos, en pendiente:**

  | Lámina | Pendiente, de … a … |
  | :--- | :--- |
  | Rojo tipo I | 0,083 (SLV-030) a 4,88 (Vía Pacífico) |
  | Rojo «otros» | 0,36 a 6,05 |
  | Azul tipo I | 0,007 a 2,7; una es **negativa** (Llanos 13-02-2018) |
  | Azul «otros» | 0,06 a 2,5 |

  Desde 2017-18, el rojo «otros» y el azul tipo I pasan a **dos tramos**, con el corte en
  x ≈ 130-678.
- **Defecto de código.** `r_y=0,01*res_med-0,0436;` usa coma decimal, que en C es el operador coma,
  y da R = 0 en el primer tramo del azul tipo I. Pasa en Coviandes, Llanos
  (`refle_vert_2_3_1.c:1219`) y Bucaramanga.

**V3.3 2020 (K42).** Tres copias con ecuaciones idénticas a la base 2020 de §2.1
(`2_V3.3_18F47K42_2020\RETRO VERTICAL\1_RetroVertical_2020(modificada)\RetroVertical1.X\ecuacionesCalibracion.c:13,16,35,38`).
Trae su propia copia de `calibraciones VERTICALES.xlsx`, con la misma hoja «concesion sabana
occidente». La calibración negro/blanco (`adcBlack`/`adcWhite`) existe, pero no entra en el valor
mostrado (`measurement.c:275-286`).

**V4.0 2021 (K42)** (`3_V4.0_18F47K42_2021\retrovertical\SOTFWARE\47k42_RV_v4.X\Aplicacion.c:324-439`).
Todo son rectas en V (voltios), salvo el naranja tipo I, que es cuadrático:

| Lámina | Ecuación | Línea y recortes |
| :--- | :--- | :--- |
| Rojo tipo I | `1456V − 144` | `:332` |
| Azul tipo I | `−623,4V + 63` | `:338`; **pendiente negativa** |
| Rojo «otros» | `4289V − 354` | `:381-393`; con corrección de T y recortes |
| Azul «otros» | `553,9V − 24` | `:398-403`; más de 300 da 0 |

La x se divide entre 3000 cuando debería dividirse entre 1500 (`Optical_Capture.c:88,100`, según el
subagente; sin verificar por el principal).

**V4.1: hay dos `Ecuaciones.c`.** La de `old\VERTICAL\5_V4.1_18F47K42\18f47k42_RetroV_V4.1.X\`
(md5 `01be826e`) resta 200 en amarillo y blanco (`:20,28`, verificado). La del repositorio V5
(md5 `8b0dbd79`) suma 150. **El rojo y el azul son iguales en las dos** (`:38`, `:66`). Además, la
V4.1 enciende **otro LED** para azul y verde (`App_Stone.c:178-228`, `Optical_Capture.c:40-82`, según
el subagente). Es el propio firmware reconociendo que el azul necesita otra luz, lo que cuadra con
§4.3.

**No encontrado:** hojas con las regresiones de la V4.0 o la V4.1, y ecuaciones para el café. En
ninguna familia se resta el oscuro dentro del cálculo activo.

## 3. Comparación con las propuestas de hoy

Sensibilidades k = R / (x − oscuro). Las de 2020 son la recta anclada en el oscuro de cada grupo,
sólo con los patrones reales (sin el punto inventado). Las de hoy son el `c1` de la curva escrita o
propuesta. Si no hay curva, se da el k de cada patrón (salida del script, §2 y §3).

| Lámina | 2020 «sabana» (oscuro 488) | 2020 «coviandina» (450) | SLV-002 hoy (565,36) | Hoy / 2020 |
| :--- | :--- | :--- | :--- | ---: |
| Blanco intenso (1) | 0,351 (n = 5, R 271-747) | 0,352 | acta `c1` 0,2985 (grado 1 libre; P1-P7 XI k 0,297-0,318) | 0,85 |
| Amarillo intenso (2) | 0,396 (n = 5) | 0,383 | acta `c1` 0,3655 (anclada) | 0,92 |
| **Rojo intenso (4)** | **0,238** (n = 4, R 117-239; por patrón 0,185-0,332) | 0,210 | **k 0,168 (P11) y 0,190 (P12)**; sin curva | **0,75** |
| **Azul intenso (5)** | **0,105** («Azul O.», n = 2, **R 20,4-20,5**) | 0,116 | **k 0,315 / 0,339 / 0,329 (P17-P19, R 83-85)**; propuesta `c1` 0,3273 | **3,1** |
| Verde intenso (3) | 0,436 (n = 5, R 50-115) | 0,471 | k 0,590-0,633 (P13-P16) | 1,39 |
| Amarillo tipo I (8) | 0,132 (n = 5, R 28-128) | 0,129 | propuesta `c1` 0,2043 (anclada) | 1,55 |
| Blanco tipo I (7) | 0,152 (n = 2) | 0,152 | k 0,175 (P35) | 1,15 |
| **Rojo tipo I (b)** | **0,062** (n = 3, **R 10,3-17,4**) | 0,062 | **propuesta `c1` 0,3138** (anclada); k 0,272-0,349 | **5,1** |
| Azul tipo I (c) | 0,099 (n = 3, R 2-10,7) | 0,103 | k 0,114-0,257 (P32a-P48, R 7-10; Δx de 27 a 79 cuentas) | ~1,8 |

**¿Las pendientes son del mismo orden?** Sí en blanco, amarillo y rojo intensos (0,75-0,92) y en el
blanco tipo I. **No en el azul intenso ni en el rojo tipo I**: son justo los dos códigos que la
fábrica lee tan bajo. En los dos, 2020 **no tenía patrones del rango de hoy**. El azul intenso se
ajustó con azules de R ≤ 20,5 y hoy se mide en R 83-85. El rojo tipo I se ajustó con rojos de R 10-17
y hoy se mide en R 46-81.

**¿Hubo antes rectas o parábolas para el rojo y el azul?** En la línea K42 de 2020, de la que sale
la fábrica de SLV-002, nunca hubo recta. Hubo una cúbica para el rojo intenso, una parábola para el
rojo tipo I y una cúbica, compartida, para los dos azules. Las tres se ajustaron con el oscuro como
punto y con el punto inventado `(3000 ; 800)`, **de 3 a 5 patrones reales cada una**. En la V4.1, el
rojo es una recta sobre 4 patrones no monótonos más el oscuro, y el azul no tiene ajuste propio.
**En el 18F4550 (2012-2018) siempre fueron rectas, calibradas equipo por equipo** y a menudo en dos
tramos para el rojo «otros» y el azul tipo I (§2.3). De equipo a equipo, la pendiente del rojo y del
azul cambia en dos órdenes de magnitud. Es otra x y otra placa, así que las cifras no se trasladan,
pero la práctica sí.

**Pendiente local de la fábrica en la x de hoy** (script, §3): el rojo intenso da 0,088-0,095 en
x ≈ 1740, el rojo tipo I 0,090-0,104 en x ≈ 734-797 y el azul 0,124-0,125 en x ≈ 813-828. Las tres
son **2 a 3,5 veces más planas** de lo que piden los patrones.

## 4. Por qué la fábrica lee tan bajo en el azul intenso y en el rojo tipo I

### 4.1 Azul intenso (código 5): −55 a −58 %

1. **La curva no se ajustó con azul intenso.** Los 5 azules reales de la hoja son
   `C46:D50`: tres «Azul I.» de R 2-10,7 y dos «Azul O.» de R 20,4-20,5. El más alto está en
   x = 685. Entre 685 y 3000 la cúbica sólo sigue al punto inventado `(3000 ; 800)` (fila 51).
2. **Es la misma curva para 5 y para c** (`ecuacionesCalibracion.c:16,38`). Con los tipo I de hoy
   (c), el error es de +22 a +78 %. Con los intensos (5), de −55 a −58 %. Una sola curva no puede
   servir a las dos láminas.
3. **La recta anclada del «Azul O.» de 2020 (0,105) es un tercio de la de hoy (0,327).** Lo explican
   dos cosas que no se pueden separar con los datos que hay: que la lámina es otra (R 20 frente a 84)
   y el LED (§4.3).
4. **El desplazamiento del oscuro no es la causa.** La fábrica tiene su cero cerca de x = 488, y el
   oscuro de SLV-002 está en 565: eso la hace leer **más** en SLV-002, no menos.

### 4.2 Rojo tipo I (código b): −71 %

1. **La parábola se ajustó con tres rojos de R 10,3, 11,9 y 17,4** (`D35:D37`) y el punto inventado
   `(3000 ; 800)` (fila 38). Su vértice está en x = 336,6: por debajo de ahí la curva decrece. De ahí
   el «C2 no creciente en x = 200» de `REFORMULACION-y-Simulacion-2026-09-19.md` §4.6.
2. En 2020, k = 0,057-0,064. Hoy los P38, P39 y P49 dan k = 0,291, 0,272 y 0,349: cinco veces más en
   el mismo tramo de Δx (170-275 cuentas). **Con la curva de 2020, a esa x le corresponde R ≈ 15-21**,
   que es lo que lee la fábrica.

### 4.3 La hipótesis del LED (hoja `2020_medidas.xlsx`, `Hoja4`)

En 2020 se midieron los mismos patrones con «luz fría» (columna `C`) y con «luz cálida» (columna `J`).
La razón fría/cálida de la señal depende mucho del color:

| Lámina (celdas) | Fría / cálida | Hoy / 2020 (k, §3) |
| :--- | ---: | ---: |
| Azul O. (`C38/J38`, `C39/J39`) | 0,243/0,08 = **3,0** · 0,232/0,08 = 2,9 | **3,1** |
| Verde O. (`C43:C47` / `J43:J47`) | 1,5-2,0 | 1,39 |
| Blanco O. (`C15:C19` / `J15:J19`) | 0,85-0,97 | 0,85 |
| Amarillo O. (`C7:C11` / `J7:J11`) | 0,80-0,83 | 0,92 |
| Rojo O. (`C30:C33` / `J30:J33`) | 0,64-0,69 | 0,75 |
| **Rojo I.** (`C27:C29` / `J27:J29`) | **0,76-0,81** | **5,1** |

k es R por unidad de señal. Si SLV-002 diera menos señal azul y más roja que el equipo de 2020, que
es lo que haría un LED más cálido, el cociente hoy/2020 seguiría la columna fría/cálida. **La sigue
en las cinco láminas «O.»**: azul, verde, blanco, amarillo y rojo intensos. Es una coincidencia de
órdenes de magnitud, no una medida. No se sabe qué LED lleva SLV-002 ni con cuál se hizo la hoja
«sabana». La cabecera `Hoja1!C1` de `2020_calibraciones VERTICALES.xlsx` dice «led cálido», pero es
de otra tabla.

**En el rojo tipo I no la sigue.** El LED predice 0,8 y se mide 5,1: hay otra cosa (§4.4).

### 4.4 Contradicción abierta: el rojo tipo I

- Los rojos tipo I de 2020 (R 10,3 y 11,9) están **por debajo del mínimo de la NTC 4739** para el
  tipo I rojo, R 14 a 0,2°/−4° (`D:\@Proyect\IT\P_RetroReflectometro_Vertical\04_Manuales\NTC-4739-Requisitos.md:135`).
- Los de hoy (P38 = 52, P39 = 46, P49 = 81; `patrones_certificados_P1-P132.csv:40,41,51`) están
  entre **3,3 y 5,8 veces ese mínimo**. En el amarillo tipo I, el mismo cociente es de 1,3 a 2,4
  (P34, P37, P43 y P44 frente a 50).
- En SLV-002 **el rojo tipo I tiene más k que el rojo intenso (0,31 frente a 0,18)**. En el blanco y
  el amarillo pasa lo contrario, y también en el rojo de 2020 (0,06 frente a 0,24).

Una de las dos series de rojos tipo I no es lo que dice su etiqueta, sea la clase, el certificado o
la geometría a la que se certificó. **No se elige aquí.** Se cierra midiendo: re-certificar P38, P39
y P49, o comprobar su clase. La decisión PA-24 (código b escrito con la recta anclada) sigue siendo
mucho mejor que la fábrica **con los certificados de hoy**. Si esos certificados cayeran, la recta
caería con ellos.

## 5. Código 5 (azul intenso): recta anclada en el oscuro (PA-14)

**Forma histórica:** cúbica compartida con el tipo I, ajustada sin azul intenso (§2.1, §4.1). No hay
ningún antecedente de recta para el azul en la línea K42. El de la V4.1 es el verde menos 60, y no se
puede comparar.

**Datos:** P17, P18 y P19 (XI; 83, 84 y 85), series S042, S043 y S044 de 10:33. Son 9 disparos
aceptados por patrón, sacados del `campana.csv` del ZIP de 12:27 (md5 `ce1f35fc...` comprobado).
Las x medias son 828,444, 813,444 y 823,556. Oscuro S060: 45 disparos, **x = 565,3556**. Fórmula de
`Ajuste.anclada` (`Ajuste.java:99`) con r0 = 0; respuesta en float32 y truncada, como en
`calibracion_v36.c:229-233`.

| Ancla | c1 | c0 | Respuesta P17 / P18 / P19 | Error % | RMS (R) | R(600) | R(4300) |
| :--- | :--- | :--- | :--- | :--- | ---: | ---: | ---: |
| **565,3556 (S060)** | **`3.27318460E-01`** | **`-1.85051315E+02`** | 85 / 81 / 84 | +2,4 / −3,6 / −1,2 | 2,43 | 11,3 | 1222 |
| 575 (`Asistente.java:218`) | `3.40091556E-01` | `-1.95552658E+02` | 86 / 80 / 84 | +3,6 / −4,8 / −1,2 | 2,51 | 8,5 | 1267 |

- Coincide con el `c1 = 3.27318464E-01` de `REFORMULACION-y-Simulacion-2026-09-19.md` §3.5, línea 190.
  La diferencia está en el último dígito del float32.
- **Pasa el criterio de `#S`:** R entre 0 y 4000 en x de 600 a 4300, porque es una recta creciente
  con R(600) = 11,3 y R(4300) = 1222. En el oscuro da −0,1, que el firmware saca como 0. **B13
  cumple.**
- **Estabilidad:** quitando un patrón cada vez, c1 va de 0,3222 a 0,3337 (±2 %). **Pero esa cifra
  engaña:** los tres patrones están en 15 cuentas de x y en 2 unidades de R. La pendiente la fijan el
  ancla y un único racimo de puntos. Por encima de R ≈ 85 la recta es extrapolación, y los azules
  IV y IX del banco la pondrán a prueba (P66, P67 y P81, R 89-95; x esperada 836-854,
  `REFORMULACION-y-Simulacion-2026-09-19.md` §5).
- **Frente a la historia:** 3,1 veces la pendiente anclada del «Azul O.» de 2020, que era otra
  lámina. En proporción al blanco, hoy 0,327 / 0,2985 = **1,10**, frente a 0,105 / 0,351 = 0,30 en
  2020. El cambio de proporción es el que predice la columna fría/cálida de §4.3. En x = 828 la
  fábrica da 37,7 y la anclada 86,0; en x = 1000, 60,8 frente a 142,3.
- **El código c no cambia.** En la V3.6 cada código tiene su propia curva (`coefCal[EC_NUM][4]`,
  `calibracion_v36.c:49`, `EC_NUM = 12` en `calibracion_v36.h:31`). Escribir el 5 no toca el c,
  aunque en fábrica compartan ecuación.

## 6. Lecciones para el método

1. **Antes de fiarse de una curva de fábrica, hay que mirar con qué patrones se ajustó.** En 2020, el
   azul y el rojo tipo I se ajustaron con 3 a 5 patrones de un rango que no es el de hoy, y con un
   punto `(3000 ; 800)` inventado. Una curva así no es de fábrica en ningún sentido metrológico. Es
   una interpolación hacia un número elegido a mano.
2. **La recta anclada no es un invento de hoy.** 2020 y la V4.1 ya ponían el oscuro como punto
   `(488 ; 0)` o `(0 ; 167)`. Lo nuevo es **imponerlo** y no añadir puntos inventados. Eso es lo
   correcto: el oscuro está medido (S060) y el `(3000 ; 800)` no.
3. **El grado alto no ayudó.** Las cúbicas de 2020 con 4-5 patrones más dos anclas son casi
   interpolantes, y hacen cosas raras entre los puntos: el rojo intenso da una S con pendiente mínima
   0,069 en x ≈ 1516. Con la cobertura que hay por color, grado 1 anclado.
4. **Una curva por lámina, no por color.** 2020 compartió la curva del azul, la del verde y la del
   naranja entre intenso y tipo I, y el azul da −57 % en uno y +45 % en el otro. La V3.6 ya separa los
   12 códigos: hay que usarlo.
5. **El color depende del LED.** Si la hipótesis de §4.3 se confirma, **una curva de azul o de rojo no
   se puede trasladar de un equipo a otro**, aunque el blanco y el amarillo sí se trasladen al ±15 %.
   El 18F4550 ya lo hacía: una recta por equipo y por lámina (§2.3). La V4.1 enciende otro LED para
   el azul y el verde. Para el método: **cada equipo calibra el azul y el rojo con sus propios patrones**, y la curva de
   fábrica sólo sirve de punto de partida en blanco y amarillo. Se confirma midiendo el espectro del
   LED de SLV-002, o un blanco y un azul en dos equipos.
6. **Comparar en k = R / (x − oscuro), no en x.** Las x de equipos distintos no se comparan por el
   oscuro, que es 450, 488 o 565, y en la V4.1 ni siquiera es la misma variable.
7. **Un certificado que no encaja es un dato, no ruido.** El rojo tipo I de hoy tiene más k que el
   rojo intenso. Hay que resolverlo antes de extender la recta del código b a otros equipos (§4.4).
