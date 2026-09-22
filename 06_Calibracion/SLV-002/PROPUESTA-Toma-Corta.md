Simulación en papel: nada de esto se ha medido en el equipo ni se ha escrito en la EEPROM. Es
aritmética sobre el ZIP de soporte y las ecuaciones del firmware, sin comprobar nada en hardware.

# Propuesta de toma corta — SLV-002 (tarea A5a)

## 0. Fuente y método

- Banco completo: `06_Calibracion/SLV-002/campanas/soporte_SLV-002_20260919_181120.zip`, md5
  `79b23590c3a6b1877d55b5c4818b93ce` — **comprobado** (coincide con el dado en el encargo).
- `campana.csv` dentro del ZIP: 1741 filas, columnas `patron,valor_certificado,tipo_lamina,color,x,
  descartado,elegida,colocacion,nota`. Código de calibración de cada paso, tomado del texto de `nota`
  (`"banco paso N, código X, uso AJUSTE|RE-MEDIDA|VERIFICACION"`); sin ese texto, el código sale del
  censo ya verificado en `06_Calibracion/SLV-002/INFORME-Ajuste-SLV-002-20260919-1811.md:173-186`.
- Curva de fábrica por código: `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:34-46`
  (`coefFabrica`, orden c3,c2,c1,c0). Curva actual de los códigos 1 y 2 (ya escritos hoy a las 12:20):
  `06_Calibracion/SLV-002/campanas/coeficientes_SLV-002_20260919_122047.csv`. El resto de códigos
  sigue en fábrica (confirmado: ese CSV coincide byte a byte con `coefFabrica` en 3,4,5,6,7,8,a,b,c,d).
- Decisiones que mandan: `DECISIONES-Diego-2026-09-19.md`, notas 12-15 (líneas 108-123): TOMA-SEL,
  TOMA-FORMA, AJUSTE-POR-CURVA, CAFE-LILA.
- Método: de `campana.csv` del ZIP, por `serie_id`, la serie `elegida=1` y su media de `x` sin
  descartados (el script no está versionado). **Validación**: para
  los seis códigos con tabla propia en el INFORME (3,4,5,6,8,b), mi media por código reproduce
  **exactamente** la de `INFORME...:373-380` sumando los patrones de AJUSTE+RE-MEDIDA+VERIFICACION de
  cada código (antes de sumar VERIFICACION no coincidía: el INFORME cuenta también esos patrones).
- La herramienta `tools/propuesta_ajuste_slv002.py` apunta a otro ZIP (`campana_SLV-002_...103300`,
  md5 `4c50dbf6...`), no al banco completo de este encargo: no se pudo reutilizar tal cual (§6).

## 1. Error de la curva actual frente al certificado, banco completo, por código

| Código | Color/clase | n medidos | Media \|error\| | Peor caso | Patrón peor | ¿Ajustar? |
| :---: | :--- | ---: | ---: | ---: | :--- | :--- |
| 1 | blanco intenso | 7 | 5,6 % | −15,9 % | P51 | ya escrito hoy; no |
| 2 | amarillo intenso | 7 | 10,3 % | +16,6 % | P61 | ya escrito hoy; no |
| 3 | verde intenso | 7 | 88,6 % | +226,4 % | P109 | **no con este dato** (§1.1) |
| 4 | rojo intenso | 12 | 45,8 % | +178,9 % | P68 (café) | sí, tras resolver tipos (§1.1) |
| 5 | azul intenso | 6 | 71,2 % | −81,2 % | P81 | **no con este dato** (§1.1) |
| 6 | naranja intenso | 8 | 8,5 % | +17,3 % | P117 | **sí, primero** (§1.1) |
| 7 | blanco tipo I | 1 | 3,9 % | −3,9 % | P35 | no hace falta |
| 8 | amarillo tipo I | 4 | 17,8 % | −29,3 % | P43 | ya recomendado por el INFORME |
| a | verde tipo I | 1 | 964 % | +964 % | P40 | sin datos para ajustar (1 patrón) |
| b | rojo tipo I | 3 | 70,4 % | −71,7 % | P49 | ya recomendado por el INFORME |
| c | azul tipo I | 1 | 57,2 % | +57,2 % | P36 | sin datos para ajustar (1 patrón) |
| d | naranja tipo I | 1 | 25,1 % | −25,1 % | P41 | sin datos para ajustar (1 patrón) |

### 1.1 Por qué

**6 primero.** El que más cerca está de pasar: ajustado, media 4,7 %, peor caso +11,1 % (P87), ya en
el INFORME (`:263-277`); falta repetir P87 en preciso o medir un naranja más entre P87 y P101. **4,
con condición.** Mejora de 45,8 % a 20,7 %, pero mezcla tipo IV, IX y XI en una sola recta (aviso
P-06, `INFORME...:298`) y el café sale muy mal (§4). **3 y 5, no.** El error no baja al ajustar (3:
88,6→69,8 %; 5: 71,2→42,6 %) porque el dato está invertido frente al certificado, no por falta de
ajuste (`INFORME...:301-361`). **a, c, d.** Un patrón cada uno: no hay con qué ajustar una recta.

## 2. Selección de referencias (TOMA-SEL): 46 patrones, hasta 50

Criterio: los patrones de uso AJUSTE/RE-MEDIDA del censo del INFORME (el conjunto que de hecho entra
en la curva de cada código), quitando los que repiten color y valor de certificado exactos.

**Quitado por TOMA-SEL:** P109 (verde, cert. 51, tipo XI) — duplica a P124 (verde, cert. 51, tipo IX).
Es además el patrón con peor residuo de todo el banco (+226,4 %, tabla §1): la propia cola ya sospechó
del certificado (`INFORME...:320-322`, nota de campo "el retro patrón da 51"). Se queda P124 en la
lista; los dos necesitan recertificación (§5), no un ajuste de curva.

| Código | Patrones (id, tipo, cert.) |
| :---: | :--- |
| 1 | P56(IV,347) P51(IV,439) P28(IX,484) P73(IX,614) P7(XI,768) P4(XI,828) |
| 2 | P61(IV,207) P30(IV,448) P121(IX,320) P25(IX,576) P106(XI,493) P20(XI,782) |
| 3 | P64(IV,54) P63(IV,121) P124(IX,51) P123(IX,115) P16(XI,170) |
| 4 | P60(IV,68) P11(IV,194) P12(IV,227) P120(IX,110) P74(IX,169) P55(XI,146) P53(XI,279) |
| 5 | P67(IV,95) P127(IX,97) P18(XI,84) P112(XI,101) |
| 6 | P71(IV,84) P72(IV,93) P131(IX,102) P86(IX,124) P87(IX,131) P101(XI,146) P102(XI,173) |
| 7 | P35(I,96) |
| 8 | P44(I,64) P37(I,82) P34(I,86) P43(I,122) |
| a | P40(I,6) |
| b | P39(I,46) P38(I,52) P49(I,81) |
| c | P36(I,10) |
| d | P41(I,73) |

Color: 1/7 blanco, 2 amarillo, 3/a verde, 4/b rojo, 5/c azul, 6/d naranja (el color va con el código,
no se pide aparte). Todos son "medido" en el banco, ninguno es "estimación" (columna `origen` de
`06_Calibracion/SLV-002/simulacion_antes_ahora_propuesta_SLV-002_20260919.csv`).

## 3. Prueba: la selección basta (1 colocación × 4 tomas)

Se recalculó la curva de cada código dos veces, con el mismo método del INFORME (anclada para 3, 5, 8,
b; libre grado 1 para 4, 6): una con **toda** la campaña (hasta 5 colocaciones × 4) y otra sacando del
banco **sólo la primera colocación** de cada patrón (lo que daría una toma corta 1 × 4). Las dos se
evalúan sobre **todos** los patrones medidos de ese código, incluidos los de VERIFICACIÓN que no
entraron en el ajuste.

| Código | n evaluado | Media, banco completo | Peor, completo | Media, 1×4 | Peor, 1×4 | Diferencia |
| :---: | ---: | ---: | :--- | ---: | :--- | :--- |
| 3 | 7 | 69,8 % | P109 +134,7 % | 69,8 % | P109 +134,7 % | ninguna (ya era 1×4) |
| 4 | 12 | 20,7 % | P68 +97,7 % | 20,9 % | P68 +99,2 % | +0,2 pp media |
| 5 | 6 | 42,6 % | P19 +58,3 % | 42,6 % | P19 +58,4 % | +0,1 pp peor caso |
| 6 | 8 | 4,7 % | P87 +11,1 % | 4,7 % | P87 +11,2 % | +0,1 pp peor caso |
| 8 | 4 | 4,5 % | P37 +5,8 % | 4,5 % | P37 −5,4 % | ninguna |
| b | 3 | 4,8 % | P39 +6,1 % | 4,7 % | P39 +5,9 % | mejora |

**La toma corta basta.** En los seis códigos la diferencia entre ajustar con todo el banco y ajustar
con una sola colocación por patrón es menor de 0,3 puntos porcentuales, media y peor caso. Bajar de
5 × 4 (o de la mezcla actual) a 1 × 4 no empeora la curva que resulta; el error grande de 3, 4 y 5 no
viene de cuántas colocaciones se toman, viene de los patrones (§1.1, §5).

## 4. Café y lila con la curva del código del rojo (4)

Ya se miden así en el banco (`nota`: "código 4, uso VERIFICACION"), y confirma la duda de Diego: la
curva del rojo **no** vale igual para los dos.

| Patrón | Color | Cert. | Curva de fábrica | Curva ajustada (banco completo) |
| :--- | :--- | ---: | ---: | ---: |
| P68 | café | 36 | +178,9 % | **+97,7 %** |
| P98 | café | 68 | +89,8 % | **+27,7 %** |
| P69 | lila | 139 | +30,2 % | **−7,9 %** |
| P114 | lila | 202 | +8,6 % | **−5,3 %** |

**Lila entra bien** en la curva del rojo (−8 a −5 %, del orden del resto del código 4). **Café no**
(+28 a +98 % incluso ajustado): no es cuestión de ajustar, el café se comporta distinto del rojo. Con
sólo dos patrones de café no se puede decidir si necesita su propia curva o si el certificado está
mal (mismo patrón de sospecha que el verde de 51, §2); hace falta más dato antes de escribir nada
(§5). Mientras tanto, la app puede seguir sin pedir el color (CAFE-LILA), pero el informe de un café
medido con la curva del rojo debe avisar del error grande.

## 5. Muestras nuevas a pedir

- **Verde (código 3):** re-certificar P109 y P124 (los dos "51") contra el otro patrón, como ya
  empezó el operador (`INFORME...:320-322`); sin eso, cualquier verde nuevo hereda la misma duda.
- **Azul (código 5):** los 4-6 patrones actuales caben en 84-101 (un 20 % de recorrido) y no
  discriminan (`INFORME...:339-344`); pedir azules certificados fuera de ese rango, uno bajo (< 50) y
  uno alto (> 150), antes de insistir en ajustar.
- **Rojo (código 4):** un rojo IV bajo adicional cerca de P60 (68), para confirmar si es un patrón
  anómalo; y **café**, 3-4 patrones más en el rango 30-140, con certificado verificado, para saber si
  el café necesita curva propia.
- **Naranja (código 6):** repetir P87 en protocolo preciso y un naranja más entre certificados 131 y
  146 (huecos que hoy fuerza P87 y P101 casi al mismo x, §1).
- **Tipo I minoritarios (a, c, d):** un segundo patrón por código como mínimo; con uno solo no hay con
  qué distinguir una recta de un punto.

## 6. Contradicciones con documentos existentes

- **La herramienta indicada no aplica al banco de este encargo.** `tools/propuesta_ajuste_slv002.py`
  fija `ZIP` y `ZIP_MD5` a `campana_SLV-002_20260919_103300.zip` (línea 34-35 del script), no al
  `soporte_...181120.zip` que pide esta tarea. No se tocó el script; el cálculo de este documento es
  independiente, validado contra `INFORME-Ajuste-SLV-002-20260919-1811.md` (§0).
- **Ninguna cifra de este documento contradice al INFORME**: donde hay tabla comparable, coincide. Lo
  que añade es el "peor caso" por código (el INFORME no lo tabula) y la prueba de que 1×4 basta.
- **PROTOCOLO-AJUSTE (5×4 para 8, b, 5) queda sustituido por TOMA-FORMA en campañas nuevas**, tal como
  ya dice la propia nota 13 de `DECISIONES-Diego-2026-09-19.md:112-115`; no es una contradicción, es
  la sustitución que esta tarea debía comprobar, y el §3 la respalda con datos.
