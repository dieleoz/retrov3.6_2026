# Informe de ajuste — SLV-002, banco del 19-sep-2026 (export 18:11:20)

**Nada de lo que hay aquí lo he medido yo.** Son datos de campo de Diego, tomados con la app
RTV 3.6.17 sobre el equipo SLV-002 (Coviandina), y leídos del ZIP de soporte. Todo lo que sigue es
aritmética sobre ese ZIP más lectura del código de la app; no hay ninguna comprobación contra el
equipo físico.

**Conclusión en una línea: se pueden escribir dos códigos, el 8 y el b, y ninguno más. El 3, el 4,
el 5 y el 6 no pasan el criterio que la propia app aplica, y el 3 y el 5 no los salva ninguna
dispensa porque el dato está invertido.**

| Fuente | Identificación |
| :--- | :--- |
| ZIP de soporte | `07 pruebas/190920261812/soporte_SLV-002_20260919_181120.zip`, md5 `79b23590c3a6b1877d55b5c4818b93ce` — **comprobado** |
| Campaña | SLV-002, MAC `00:21:13:05:19:3B`, estado abierta, banco REPRESENTATIVO |
| Cola del banco | md5 `70ef3b868db85ef75743936a6218935e` = `app/src/main/assets/cola_banco_representativo_v2.csv` — **comprobado**. 80 pasos, los 80 HECHO |
| Firmware | `V3.6 2026-09-19 (3.6.2) CAL mascara 0003` |
| Código de la app leído | `D:\IT\wt_rtv10\03_App_Movil\RetroV36\app\src\main\java\com\dpi\retrov36\` (solo lectura) |

> La copia de la cola que hay en `06_Calibracion/cola_banco_representativo_v2.csv` tiene md5
> `78fa294056264e394262c5c688e77a38` y **no** es la que usó la app. La que usó es la de `assets/`.

---

## 1. Lo primero que se preguntó: ¿hubo donación de medidas?

**No. En estos datos concretos no hay ni una sola medida donada.** Es la mejor noticia posible y es
la respuesta a la pregunta que condicionaba todo el encargo.

### 1.1 El defecto existe, y está donde dicen las revisiones

`BancoPrevio.java:145` es literalmente

```java
if (!candidatos.contains(e) && !enCola.contains(e) && !excluidos.contains(e)) {
```

Un equivalente sólo queda descartado como donante si **está en la cola** o si **una decisión lo
excluye** (`excluidos` = `[P81]`). En la cola REPRESENTATIVA sólo está el representante de cada
grupo, así que los demás miembros pasan el filtro y pueden donar. El mecanismo es el que describen
`REVISION-Arquitectura-P16-V3.6.md:148-152` y `QA-App-3.6.17.md:175-186`. Confirmado, sin matices.

### 1.2 Pero en esta campaña no llegó a dispararse

Dos caminos independientes, y los dos dan lo mismo:

**Camino 1 — reconstruir el estado final del diario.** El diario trae 9 registros `PASO` con texto
`(equivalente de …)`, todos con fecha 16:09:35 (8) y 16:25:54 (1). Están **antes** del evento `COLA`
del diario (línea 980). Al reproducir el diario, `Campana.java:1393-1398` borra todos los pasos
cuando el tipo de cola cambia, y el tipo por defecto es `COMPLETO` (`Campana.java:215`):

```java
case "COLA":
    if (!c.get(1).equals(colaTipo)) {
        pasos.clear(); seriePaso.clear(); historialPasos.clear();
    }
```

Ese `COLA,REPRESENTATIVO` borra los **40** pasos anteriores, los 9 donados incluidos. Los 80 pasos
de la cola vigente se remarcaron HECHO entre las 16:41:12 y las 18:10, y **ninguno lleva
`(equivalente de …)`**. Donaciones en el estado final: **0**.

**Camino 2 — cotejar paso por paso contra el patrón real de la serie.** Para los 74 pasos de medida
de la cola (PATRON, OSCURO y A5) se toma la serie anotada en el paso y se comprueba su `patron` y su
`patron_original` en `campana.csv`. Resultado: **0 discrepancias, 0 pasos sin serie**. Y en las 1741
filas de `campana.csv`, `patron` es igual a `patron_original` en todas: **ninguna serie fue
reasignada a otro patrón**.

*(La primera vez que corrí el camino 2 me dio "0 pasos de medida en la cola" porque filtré por la
columna `tipo`, que en ese CSV es el tipo de lámina — la clase de paso está en la columna `paso`. El
cero era mío, no del dato. Corregido y vuelto a correr; lo anoto porque un negativo salido de una
columna equivocada es exactamente la trampa que avisa el CLAUDE.md.)*

### 1.3 Los cuatro del código 5: te contradigo

El encargo dice *"Se midieron 24 pasos sustituibles así, y entre ellos están los cuatro del código
5"*. **Los cuatro del código 5 no se midieron por donación: se midieron cada uno sobre su propio
patrón, y en 5 × 4 preciso.**

| Paso | Patrón | Uso | Serie | n | ¿donada? |
| :--- | :--- | :--- | :--- | ---: | :--- |
| 81 | P67 | AJUSTE | S114 | 20 | no — P67 |
| 83 | P127 | AJUSTE | S116 | 20 | no — P127 |
| 84 | P18 | AJUSTE | S117 | 20 | no — P18 |
| 86 | P112 | AJUSTE | S128 | 20 | no — P112 |
| 82 | P81 | VERIFICACIÓN (fuera del ajuste) | S115 | 20 | no — P81 |
| 85 | P19 | VERIFICACIÓN | S118 | 20 | no — P19 |

Y las revisiones **no dicen otra cosa**. `QA-App-3.6.17.md:217-218` ya lo deja escrito:
*"ninguno de los donantes del 5 tiene serie elegida (P66, P126, P82, P17, P111, P96, P97, todos «sin
serie»)"*, y `:313` remata *"Hoy no dispara con el diario de las 15:10"*. Los **24** de
`REVISION-...P16:162` son pasos **estructuralmente** sustituibles: tienen en su grupo algún miembro
fuera de la cola y no excluido. Es una cuenta de exposición, no de daño.

Contando además la condición que hace falta para que el daño ocurra —que ese miembro tenga de verdad
una serie aceptada— quedan **5 pasos**, y ninguno es del código 5:

| Paso | Patrón | Código | Uso | Donante posible |
| :--- | :--- | :--- | :--- | :--- |
| 9 | P28 | 1 | RE-MEDIDA | P27 |
| 12 | P7 | 1 | AJUSTE | P1, P6 |
| 69 | P41 | d | VERIFICACIÓN | P32b, P50, P33 |
| 79 | P40 | a | VERIFICACIÓN | P45 |
| 88 | P36 | c | VERIFICACIÓN | P42, P32a, P46, P47, P48 |

Los dos primeros son del código 1, que es `NO_REESCRIBIR` (`TablaCalibracion.java:171`); los otros
tres, de códigos `SOLO_VERIFICAR`. **La exposición real no toca ningún código que se vaya a
escribir.**

### 1.4 Lo que sí queda contaminado

La donación no puede ensuciar el número, porque `medidasDe(k)` (`FlujoCalibracion.java:462-471`)
busca las medidas por el **nombre del patrón**, y un paso donado no le da serie al patrón que lo
recibe. Lo que la donación sí falsea es **el recuento de pasos hechos**, y con él dos puertas:
`cola.calibrable()` (`BancoCola.java:316-328`) y `pasosSinHacer()` (`FlujoCalibracion.java:669-678`,
la puerta de los "todos los pasos" del código 5). Un código podría declararse calibrable con menos
puntos reales de los que aparenta. Aquí no ha pasado, pero la puerta sigue abierta.

### 1.5 P81

`DECISIONES-Diego-2026-09-19.md`, fila **P81**: fuera del ajuste del código 5, sólo verificación y
re-medida. **Respetado**: P81 no entra en el ajuste (`patronesAjuste` lo quita en
`FlujoCalibracion.java:458`) y tampoco donó a nadie. Se usa abajo como control, que es para lo que
Diego lo dejó — y es el dato que hunde al código 5.

---

## 2. Método: el de la app, reproducido, no reinventado

| Paso | Dónde está en la app | Qué hice |
| :--- | :--- | :--- |
| Puntos del ajuste | `Asistente.java:136-158` | un punto por patrón; `x` = **media plana de los disparos válidos** de la serie elegida, `R` = certificado |
| Qué patrones entran | `FlujoCalibracion.java:442-460` | los de uso AJUSTE y RE-MEDIDA del código, menos los excluidos (P81) |
| Serie elegida | `Campana.java:835-847` | la marcada, si no la última aceptada y no anulada |
| Recta anclada | `Ajuste.java:99-122` | `k = Σ(R−R₀)(x−x₀) / Σ(x−x₀)²`, con `R₀ = 0` |
| Mínimos cuadrados | `Ajuste.java:41-92` | grado 1 sobre `u = (x−x̄)/escala` y vuelta a `x` |
| Ancla de oscuro | `Anclas.java:47-87` | media de los OSCURO de inicio y fin de **la sesión de ese código**, con su prueba de deriva |
| Método por código | `TablaCalibracion.java:171-207` | 1 y 2 no se reescriben; 3/4/6 grado 1 o anclada si la libre no pasa `#S`; 8, b y 5 anclada; 7, a, c, d sólo verificar |
| Criterios | `Asistente.java:314-344` | RF-CAL-14 (residuo ≤ 10 %), RF-CAL-15 (RMS por tipo ≤ 6 %), RF-CAL-16 (no empeorar a fábrica) |
| Forma y `#S` | `Asistente.java:179-208`, `:261-280` | creciente, 0 ≤ R ≤ 4000 en x = 600..4300 |
| Oscuro (P9-B13) | `Asistente.java:237-247` | `R_nueva(x_osc) ≤ máx(fábrica + 10 ; 25)` |
| Dispensas | `FlujoCalibracion.java:654-666` | sólo lo que figure en el `alcance` de la decisión |

**Dónde me aparto: en nada del cálculo.** Dos anotaciones de método:

1. `Serie.media()` (`Campana.java:171-182`) es la media de las medias de colocación, y es la que
   imprime el `resumen.txt`; el ajuste, en cambio, usa la media plana de los disparos
   (`Asistente.java:149`). Aquí **coinciden en todas las series** porque todas las colocaciones
   tienen el mismo número de disparos. Lo comprobé una por una; si alguna vez un disparo se
   descarta, dejarían de coincidir.
2. La columna `codigo` de `campana.csv` vale `e` en las 1741 filas. **No es un fallo**: es el código
   de medida con el que se leyó el ADC (`Campana.java:1060` escribe `s.codigo`), no el código de
   calibración del patrón. El código de calibración sólo aparece en el texto libre de `nota`. Es una
   trampa para quien analice ese CSV sin leer el exportador.

### 2.1 Anclas y s_rep

| Sesión | OSCURO inicio | OSCURO fin | Deriva | Límite | Ancla x₀ | s_rep (A5 del inicio) |
| :--- | :--- | :--- | ---: | ---: | ---: | ---: |
| 1 (códigos 1, 2, 7, 8) | S051 568,10 | S075 570,05 | +1,95 | 5,00 | **569,08** | 0,149 % |
| 2 (códigos 3, 4, 5, 6, a, b, c, d) | S076 569,95 | S123 571,00 | +1,05 | 5,00 | **570,48** | 0,314 % |

Las dos anclas son válidas: ningún OSCURO deriva. El oscuro se mantuvo entre 568,1 y 571,0 cuentas
en las cuatro medidas del día, de las 16:41 a las 17:59 — 2,9 cuentas de recorrido en hora y cuarto.

---

## 3. Censo: patrones útiles por código

Sólo pasos de la cola vigente, sólo la serie elegida de cada patrón, sin donadas (no hay) y sin
excluidos (P81).

| Código | Color / clase | Pasos PATRÓN | **Patrones útiles en el ajuste** | Patrones | n por serie | Protocolo |
| :--- | :--- | ---: | ---: | :--- | :--- | :--- |
| 1 | blanco intenso | 7 | 6 | P56 P51 P28 P73 P7 P4 | 12/4/45/4/4/4 | mixto |
| 2 | amarillo intenso | 7 | 6 | P61 P30 P121 P25 P106 P20 | 4/4/4/20/4/4 | mixto |
| 3 | verde intenso | 7 | 6 | P64 P63 P124 P123 P109 P16 | 4/4/4/20/4/4 | 1 × 4 |
| 4 | rojo intenso | 12 | 7 | P60 P11 P12 P120 P74 P55 P53 | 4/20/4/4/4/4/4 | 1 × 4 |
| **5** | **azul intenso** | 6 | **4** | P67 P127 P18 P112 | 20/20/20/20 | **5 × 4** |
| 6 | naranja intenso | 8 | 7 | P71 P72 P131 P86 P87 P101 P102 | 4/4/4/20/4/4/4 | 1 × 4 |
| 7 | blanco tipo I | 1 | 1 | P35 | 9 | sólo verificar |
| **8** | **amarillo tipo I** | 4 | **4** | P44 P37 P34 P43 | 20/20/20/20 | **5 × 4** |
| a | verde tipo I | 1 | 1 | P40 | 9 | sólo verificar |
| **b** | **rojo tipo I** | 3 | **3** | P39 P38 P49 | 20/20/20 | **5 × 4** |
| c | azul tipo I | 1 | 1 | P36 | 9 | sólo verificar |
| d | naranja tipo I | 1 | 1 | P41 | 9 | sólo verificar |

**Series anuladas:** S007-S013 (7), las siete de tipo I medidas a 3 × 3 por la mañana, anuladas a las
16:41:12 por la decisión **TIPO-I-REPETIR** de Diego. Se repitieron todas en 5 × 4. Ninguna anulada
entra en ningún ajuste.

**Series repetidas y no elegidas:** 28 series de patrón que el operador no eligió (21 DUDOSO,
2 REPETIR, 2 OK y 3 de la A5 de la mañana), más las 12 series A5 del banco, que por diseño no entran
en ningún ajuste. Ninguna entra.

**PROTOCOLO-AJUSTE** (`ProtocoloDisparos.java:28`, `CODIGOS_A_ESCRIBIR = {8, b, 5}`): los tres
códigos que la app se plantea escribir, más P81 y todas las re-medidas, tienen que ir en 5 × 4. La
puerta (`FlujoCalibracion.java:708-730`) mira 12 patrones —los de AJUSTE y RE-MEDIDA de los tres
códigos, más P81— y **los 12 cumplen**: 20 disparos válidos en 5 colocaciones cada uno. De hecho
cumplen los 13 patrones de esos tres códigos, contando también P19, que sólo verifica. Los códigos
3, 4 y 6 van en 1 × 4, que es lo que manda PROTOCOLO-MIN y es correcto porque no son de los que se
escriben — aunque la tabla RF-CAL-37 sí los declare escribibles. Es una incoherencia menor de la app
(un código podría escribirse con dato 1 × 4); aquí no llega a importar porque esos tres fallan por
otro lado.

---

## 4. Las curvas, los residuos y el criterio

Signo de los residuos: el de RF-CAL-14, `(R_nueva − certificado) / certificado`
(`Asistente.java:317`).

### 4.1 Código 8 — amarillo tipo I. **PASA**

Recta anclada en x₀ = 569,08. `c1 = 2.11652640E-01`, `c0 = -1.20446226E+02`.

| Patrón | cert | x | R_nueva | residuo | R_hoy (fábrica) | desvío hoy |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| P44 | 64 | 887,30 | 67,4 | **+5,3 %** | 59,9 | −6,4 % |
| P34 | 86 | 955,25 | 81,7 | **−5,0 %** | 67,7 | −21,3 % |
| P37 | 82 | 979,15 | 86,8 | **+5,9 %** | 70,3 | −14,3 % |
| P43 (re-medida) | 122 | 1133,90 | 119,5 | **−2,0 %** | 86,3 | −29,3 % |

Error máximo 4,8 unidades de R, RMS 3,82. RMS relativo del tipo I: **4,8 %** (fábrica 19,7 %).
Oscuro: R_nueva(569) = 0,0 frente a 12,1 de fábrica, límite 25 → pasa. `criterioFirmwareS`: pasa.
Forma: creciente, sin techo; negativa sólo por debajo de x = 569, que es aviso, no bloqueo.

**Ningún incumplimiento.** No necesita dispensa de nadie. Se escribe solo y primero
(`TablaCalibracion.java:189-190`).

### 4.2 Código b — rojo tipo I. **PASA**

Recta anclada en x₀ = 570,48. `c1 = 3.20071157E-01`, `c0 = -1.82592593E+02`.

| Patrón | cert | x | R_nueva | residuo | R_hoy (fábrica) | desvío hoy |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| P39 | 46 | 723,00 | 48,8 | **+6,1 %** | 14,1 | −69,4 % |
| P38 | 52 | 739,70 | 54,2 | **+4,2 %** | 15,6 | −70,1 % |
| P49 (re-medida) | 81 | 813,30 | 77,7 | **−4,0 %** | 22,9 | −71,7 % |

Error máximo 3,3, RMS 2,79. RMS del tipo I: **4,9 %** (fábrica 70,4 %). Oscuro y `#S`: pasan.

**Ningún incumplimiento**, y aquí viene lo que hay que saber:

> **El código b ya no necesita la dispensa PA-24.** Esa dispensa se concedió sobre unas cifras de
> P39 +15 % y P49 −10 % (`DECISIONES-Diego-2026-09-19.md`, fila PA-24;
> `TablaCalibracion.java:152-154` habla de P49 a −10,2 % o −11,1 %). **Con el dato de este banco no
> reproduzco esas cifras por ningún camino**: ni con las series elegidas de la tarde (5 × 4:
> +6,1 %, +4,2 %, −4,0 %) ni con las anuladas de la mañana (3 × 3: +2,6 %, +7,2 %, −4,2 %). Las dos
> quedan holgadamente dentro del ±10 %.
>
> No digo que los documentos anteriores estuvieran mal: salían de otro conjunto de medidas (la
> campaña de las 11:21 / 12:20). Digo que **hoy ese código entra por derecho propio**, y que la
> dispensa PA-24, la ampliación a RF-CAL-15 y el límite RF-CAL-15-b de 11,5 % **no llegan a
> aplicarse** — el RMS real es 4,9 %. Conviene que Diego lo sepa antes de firmar un acta que
> invoque una dispensa que no hace falta.

**REMEDIDA-b** (juzgar la re-medida con RF-CAL-18 frente a la curva escrita, no frente al
certificado) sigue siendo la regla configurada, y la app la necesita para habilitar el b
(`TablaCalibracion.java:194-200`). Con estos números da igual por dónde se juzgue: P49 queda a
−4,0 % del certificado, dentro del ±10 % de `Remedida3611.java:67`.

### 4.3 Código 6 — naranja intenso. **NO PASA, por poco**

La recta libre de grado 1 pasa `#S`, así que el método es mínimos cuadrados:
`c1 = 1.85973278E-01`, `c0 = -1.05968093E+02`. Error máximo 14,5, RMS 7,08.

Incumplimientos: **RF-CAL-14 en P87 (+11,1 %)**, **RF-CAL-15 en tipo IX (RMS 7,5 % > 6 %)** y
**RF-CAL-16 en tipo XI** (la nueva da RMS 3,2 % donde la de fábrica da 0,2 %). El código 6 no tiene
dispensa en `TablaCalibracion.java:187`, así que `noDispensados` devuelve los tres y el plan queda
bloqueado.

Falla además fuera de muestra: **P117**, patrón de verificación independiente, quedaría a **+10,5 %**
del certificado. El de re-medida, P86, sí pasa (−6,8 %).

Es el único de los cuatro rechazados que está cerca. Un patrón más de naranja, o repetir P87 en
5 × 4, podría cambiarlo.

### 4.4 Código 4 — rojo intenso. **NO PASA**

Recta libre de grado 1 (pasa `#S`): `c1 = 1.66856982E-01`, `c0 = -5.18082434E+01`. Error máximo 34,9,
RMS 22,47.

| Patrón | cert | x | R_nueva | residuo |
| :--- | ---: | ---: | ---: | ---: |
| P60 | 68 | 879,25 | 94,9 | **+39,6 %** |
| P120 | 110 | 932,25 | 103,7 | −5,7 % |
| P55 | 146 | 1128,50 | 136,5 | −6,5 % |
| P74 | 169 | 1151,75 | 140,4 | **−16,9 %** |
| P11 (re-medida) | 194 | 1682,30 | 228,9 | **+18,0 %** |
| P12 | 227 | 1712,75 | 234,0 | +3,1 % |
| P53 | 279 | 1836,50 | 254,6 | −8,7 % |

Tres RF-CAL-14 fuera y los tres tipos de lámina fuera de RF-CAL-15 (IV 25,2 %, IX 12,6 %,
XI 7,7 %). Sin dispensa → bloqueado. Fuera de muestra fallan P11, P104, P68 y P98.

El problema de fondo se ve en P60 y en P11: dos patrones de tipo IV, certificados 68 y 194, que la
recta no puede conciliar con los de tipo IX y XI. **Mezcla tres tipos de lámina en una sola curva**
(el aviso P-06 de `Asistente.java:453-455`), y los tipos no se comportan igual.

### 4.5 Código 3 — verde intenso. **NO PASA, y no es cuestión de umbrales**

Aquí no hay un ajuste flojo: **hay un dato invertido**.

| Patrón | tipo | cert | x medida |
| :--- | :--- | ---: | ---: |
| P64 | IV | 54 | 828,25 |
| P63 | IV | 121 | **669,75** |
| P124 | IX | 51 | 823,50 |
| P123 | IX | 115 | **685,50** |
| P109 | XI | 51 | 946,00 |
| P16 | XI | 170 | **842,25** |

**En los tres tipos de lámina, el patrón de certificado más alto da menos cuentas que el de
certificado más bajo.** Eso no es ruido: la repetibilidad del día es del 1 al 5 % y estas
inversiones son del 15 al 20 %. Por eso la recta libre de mínimos cuadrados sale con **pendiente
negativa** (`c1 = −2.04140422E-01`) y es rechazada por `criterioFirmwareS` (se hace negativa en
x = 1259). La app cae entonces a la anclada, que da residuos de **+134,7 % a −68,1 %**.

La propia cola ya lo sospechaba: los pasos 73 y 76 llevan la nota *"uno de los 9 verdes certificados
en 51 exacto: confirmar certificado"*, y el operador anotó en S129 (P109) *"da 51 en el otro retro
patrón; El retro patrón da 51"*.

**Lo que hay que resolver aquí no es el ajuste, es el certificado de los verdes bajos (P109, P124,
P64).** Escribir cualquier curva con este dato sería peor que dejar la de fábrica en la zona alta.

### 4.6 Código 5 — azul intenso. **NO PASA. Y PA-14 no lo salva**

Recta anclada en x₀ = 570,48 con los cuatro patrones que quedan tras sacar P81:
`c1 = 5.32210312E-01`, `c0 = -3.03612678E+02`. Error máximo 44,0, RMS 35,56.

| Patrón | tipo | cert | x | R_nueva | residuo |
| :--- | :--- | ---: | ---: | ---: | ---: |
| P18 | XI | 84 | 811,05 | 128,0 | **+52,4 %** |
| P67 | IV | 95 | 671,65 | 53,8 | **−43,3 %** |
| P127 | IX | 97 | 687,25 | 62,1 | **−35,9 %** |
| P112 | XI | 101 | 732,95 | 86,5 | **−14,4 %** |

**El mismo mal que el verde, y peor.** Dentro del propio tipo XI: P18, certificado 84, da 811
cuentas; P112, certificado 101, da 733. Inversión de 78 cuentas donde la repetibilidad de P112 entre
sus dos series fue de 7,4 cuentas (1,01 %). Los cuatro certificados de azul intensa caben en
84-101 — un 17 % de recorrido — y las cuentas se reparten en 672-811, un 17 % **en sentido
contrario**. **Estos cuatro patrones no discriminan: no hay señal que ajustar.**

Dos cosas más, que son las que cierran el asunto:

1. **PA-14 no dispensa nada.** La decisión habilita el método (recta anclada) y la regla de
   cobertura relajada de RF-APP-42 — el ancla cuenta como un nivel, que es lo que permite pasar de
   un rango 84-101 a 0-101. Pero la `Fila` del 5 se construye con `dispensa = ""` y
   `alcance = Collections.emptyList()` (`TablaCalibracion.java:176-186`). En
   `FlujoCalibracion.java:654-666`, sin alcance **ningún** incumplimiento queda dispensado. Los
   cuatro RF-CAL-14 y los tres RF-CAL-15 salen fuera y el plan se bloquea con *"incumple … y la
   tabla no le da dispensa"*. Escribir el 5 hoy exigiría una decisión nueva de Diego, con alcance
   explícito, no la PA-14 que ya hay.
2. **P81 lo confirma desde fuera.** Diego lo dejó como patrón de verificación precisamente para
   esto. Con la curva nueva, P81 (certificado 92, x = 654,40) leería **44,7: un −51,5 %**. Hoy, con
   la de fábrica, lee 17,3 (−81,2 %). Se pasa de equivocarse por defecto un 81 % a equivocarse por
   defecto un 51 %. El otro control, **P19** (certificado 85), pasaría de −56,3 % a **+58,3 %**: se
   equivocaría igual de lejos, sólo que hacia el otro lado.

El código 5 no está listo. No por la app: por los patrones.

---

## 5. Qué cambiaría respecto a lo que el equipo lleva hoy

El SLV-002 responde `#V,3.6,2026-09-19,CAL,0003#`, y sus propias pruebas lo traducen
(`pruebas.txt:7`): *"máscara 0003: ajustados 1 2; temperatura de fábrica"`*. Es decir: **sólo el 1 y
el 2 están escritos** (`coeficientes_SLV-002_20260919_122047.csv`, filas de código 1 y 2). **Todo lo
demás que lleva dentro es de fábrica**, y por eso la comparación de abajo es contra
`ecuacionesCalibracion.c` (`Fabrica.java:62-88`), no contra nada que se haya escrito hoy.

| Código | Curva de hoy | Curva nueva | Error medio absoluto frente al certificado (todos los patrones medidos del código) | ¿Mejora? |
| :--- | :--- | :--- | :--- | :--- |
| **8** | fábrica, cúbica | `0,2116517 x − 120,4462` | **17,8 % → 4,5 %** | sí, mucho |
| **b** | fábrica, cuadrática | `0,3200712 x − 182,5926` | **70,4 % → 4,8 %** | sí, es otro instrumento |
| 6 | fábrica, cuadrática | `0,1859733 x − 105,9681` | 8,5 % → 4,7 % | sí, pero incumple |
| 4 | fábrica, cúbica | `0,1668570 x − 51,8082` | 45,8 % → 20,7 % | mejora y sigue siendo inservible |
| 5 | fábrica, cúbica | `0,5322103 x − 303,6127` | 71,2 % → 42,6 % | mejora y sigue siendo inservible |
| 3 | fábrica, cúbica | `0,3186920 x − 181,8058` | 88,6 % → 69,8 % | mejora y sigue siendo inservible |

**Ningún código nuevo es peor que el que lleva hoy**, que era la pregunta. Pero "mejor que fábrica"
no es el criterio: el 3, el 4 y el 5 siguen con errores del 20 al 70 %, muy por encima del 10 % de
RF-CAL-14.

Cuánto se mueve el resultado en el rango de trabajo, en unidades de R (lo que leería el operador):

| Código | Rango medido (x) | En el punto bajo | En el punto alto |
| :--- | :--- | :--- | :--- |
| 8 | 887 – 1134 | 59,9 → 67,4 (**+7,5**) | 86,3 → 119,5 (**+33,3**) |
| b | 723 – 813 | 14,1 → 48,8 (**+34,7**) | 22,9 → 77,7 (**+54,8**) |
| 6 | 1016 – 1458 | 73,8 → 83,0 (+9,2) | 173,3 → 165,1 (−8,2) |
| 4 | 879 – 1836 | 141,7 → 94,9 (−46,8) | 250,2 → 254,6 (+4,4) |
| 5 | 672 – 811 | 19,2 → 53,8 (+34,6) | 35,6 → 128,0 (+92,4) |
| 3 | 670 – 946 | 80,0 → 31,6 (−48,4) | 166,4 → 119,7 (−46,7) |

Del b, el cambio es el más grande en términos relativos: **el equipo multiplica por más de tres lo
que hoy lee en rojo tipo I**. Hoy una lámina roja opaca de 81 cd/lx/m² se lee como 23; con la curva
nueva se leería 78. Eso es lo que estaba roto y es lo que se arregla.

**Fuera del rango medido todo es extrapolación.** Las rectas nuevas se apartan mucho de las cúbicas
de fábrica por arriba: en el código 8, a x = 3000 la de fábrica da 810 y la nueva 515. Ninguna de
las dos está respaldada por un patrón; la diferencia es que la nueva al menos crece de forma
monótona y no revienta el `#S`. Conviene decirlo en el acta.

---

## 6. Repetibilidad: cuánto vale este dato

Es la parte que más condiciona lo demás, y la respuesta es incómoda.

**La s_rep que usa la app está medida dentro de una sola serie**, entre las 5 colocaciones seguidas
de la A5 del inicio (`Anclas.java:98-121`): 0,149 % en la sesión 1 y 0,314 % en la 2. **La
reproducibilidad real, volviendo a poner la misma lámina en otro momento del día, es entre cinco y
veinte veces peor.**

| Patrón | Repeticiones | Recorrido | % |
| :--- | ---: | ---: | ---: |
| P124 | 2 | 0,25 | 0,03 % |
| P101 | 3 | 2,50 | 0,19 % |
| P123 | 2 | 2,25 | 0,33 % |
| **P5 (0° vs 90°)** | 2 | 8,56 | **0,36 %** |
| P50 | 2 | 6,16 | 0,68 % |
| P36 | 4 | 6,33 | 0,99 % |
| P112 | 2 | 7,40 | 1,01 % |
| P118 | 2 | 27,00 | 1,25 % |
| P11 | 3 | 26,30 | 1,55 % |
| P78 | 5 | 11,00 | 1,55 % |
| P12 | 2 | 27,00 | 1,59 % |
| P4 | 6 | 55,20 | 1,68 % |
| P22 | 5 | 38,65 | 2,70 % |
| P37 | 2 | 34,50 | 3,59 % |
| P51 | 3 | 81,75 | 4,71 % |
| P28 | 7 | 103,31 | 4,83 % |
| P34 | 2 | 233,75 | 21,80 % |

Mediana 1,25 %, y una cola larga. Los patrones puente, repetidos cuatro veces entre las 16:42 y las
17:58 sin tocar nada más, dan **P22 2,48 %, P4 1,43 %, P28 0,75 %** — y esos son el mejor caso, papel
en buen estado y 5 × 4.

**P5, las dos orientaciones** (S032 a 0°, S033 a 90°, con dos minutos de diferencia): 2398,76 frente
a 2407,31, **+8,56 cuentas, +0,36 %**. La orientación de la lámina no introduce un sesgo apreciable
en este patrón. Es un buen dato y conviene repetirlo en más láminas antes de generalizarlo — con una
sola pareja no se puede afirmar nada del conjunto.

**P34, el 21,8 %,** no es dispersión: S006 es la serie con la anomalía que el propio `resumen.txt`
recoge en su desvío por posición (`1:+711 2:+708 3:+704`, los tres primeros disparos ~700 cuentas
por encima). Es una lámina mal apoyada o mal colocada, no una medida ruidosa. La serie buena es S070
(955,25, s 1,99). Lo mismo ocurre en S036/P27 (+95 en los 9 primeros), S038/P28 (−115 en los 18
primeros), S068/P37 (−85 en los 8 primeros) y S064/P25. **Hay un patrón recurrente de colocaciones
iniciales desviadas en bloque**, que el asentamiento de un disparo no siempre atrapa.

**Lo que esto significa para los coeficientes:**

- Los residuos del **8 (±5,9 %)** y del **b (±6,1 %)** son del mismo orden que la reproducibilidad
  de re-colocación (1-5 %). Son buenos ajustes, pero su holgura frente al 10 % de RF-CAL-14 no es
  tan grande como parece: repetir el banco otro día podría mover un patrón un par de puntos.
- Las inversiones del **3 (15-20 %)** y del **5 (10-17 %)** son **muy superiores** a esa
  reproducibilidad. No se explican midiendo otra vez. Son del certificado o de la lámina.
- La `s_rep` de 0,15-0,31 % que la app mete en los criterios de re-medida
  (`Remedida3611.java:65`) hace el límite 2 s_rep x √(1/K+1/K) demasiado estrecho: con
  x ≈ 800 y K = 5 sale ±1,6 cuentas, cuando el mismo patrón se mueve 7 cuentas entre dos series del
  mismo cuarto de hora. **Es previsible que la re-medida del 8 y del b falle el criterio 2 aunque
  todo esté bien.** No es un problema del ajuste, es del umbral; conviene anticiparlo.

---

## 7. Las decisiones de Diego, una por una

| Fila | Qué exige | Estado con este dato |
| :--- | :--- | :--- |
| **PA-24** (y sus tres ampliaciones) | dispensa RF-CAL-14/15 del código b; P39 +15 % y P49 −10 % como techo con ±3 | **No se necesita.** Residuos reales +6,1 %, +4,2 %, −4,0 %; todos mejores que el techo, y la regla de la fila "PA-24 margen" dice que un valor mejor que el aceptado siempre pasa |
| **PA-14** | código 5 con recta anclada y cobertura relajada | **Aplicada en el método, insuficiente en el criterio.** Habilita la anclada y RF-APP-42, pero no dispensa RF-CAL-14/15 (`TablaCalibracion.java:176-186`). El 5 queda bloqueado |
| **P81** | fuera del ajuste del 5, sólo verificación y re-medida | **Respetada.** No entra en el ajuste ni donó a nadie. Como control, delata al 5: −51,5 % |
| **REMEDIDA-b** | la re-medida del b se juzga con RF-CAL-18 frente a la curva escrita | **Configurada y necesaria** para habilitar el b (`TablaCalibracion.java:194-200`). Con estos números da igual la regla: P49 queda a −4,0 % del certificado |
| **PROTOCOLO-AJUSTE** | AJUSTE y RE-MEDIDA de 8, b y 5, más P81 y las re-medidas, en 5 × 4 | **Cumplida en los 12 patrones que mira la puerta** (y en los 13 del conjunto): 5 colocaciones × 4 disparos, 20 válidos |
| **TIPO-I-REPETIR** | P34, P37, P43, P44, P38, P39, P49 se repiten en preciso | **Cumplida.** S007-S013 anuladas a las 16:41:12; los siete remedidos en 5 × 4 (S067, S069, S070, S071, S092, S093, S094) |
| **RF-CAL-15-b** | RMS de tipo I del b ≤ 11,5 % estricto | **Cumplida con margen**: 4,9 % |
| **PRECISO-5x9** | las series de 5 × 9 de la mañana cuentan como preciso | **Aplicable pero no necesaria** para los códigos que se escriben: los doce patrones ya están en 5 × 4 propio. Sólo afecta a P28 (S038, 45 disparos) del código 1, que no se reescribe |

---

## 8. Lo que no se puede saber desde este ZIP

- **Si los certificados de los patrones son correctos.** Las inversiones del verde y del azul
  apuntan al certificado, no a la medida, pero el ZIP no trae los certificados originales: trae el
  catálogo `patrones_certificados_P1-P132.csv` de la app, que es una transcripción.
- **Si el equipo aceptaría estas curvas.** El `criterioFirmwareS` que reproduje es la emulación que
  la app hace del `#S` del firmware V3.6.1 (`Asistente.java:261-280`), no el firmware. Se cierra
  enviando `#S` y leyendo `#G` y `#E`.
- **La re-medida.** Es una medida posterior a escribir la curva, leyendo R con la curva ya dentro.
  Aquí sólo pude predecirla evaluando la curva nueva en la x ya medida. No es lo mismo.
- **Qué pasó en las dos colas anteriores.** El diario conserva los 40 `PASO` de antes de las 16:41,
  pero no conserva qué cola los numeraba. Que esos 9 donados se borraran es correcto por diseño,
  pero **no queda trazabilidad** de qué cola era.
- **La temperatura.** Máscara 0003 = "temperatura de fábrica" (`pruebas.txt:7`). El ZIP no trae
  lecturas de temperatura. Todo el ajuste es a temperatura no controlada ni registrada, en una tarde
  de cuatro horas.
- **Nada de esto está validado contra el equipo físico.** Regla 7 del CLAUDE.md.

---

## 9. Recomendación

**Escribir el 8 y el b. Nada más. Y seguir midiendo el 3, el 4, el 5 y el 6.**

1. **Código 8** — se escribe. Va solo y primero, y su acta se acepta antes de ofrecer el b
   (`TablaCalibracion.java:189-190`). Sin incumplimientos, sin dispensas, error máximo 5,9 %.
   `c3 = 0, c2 = 0, c1 = 2.11652640E-01, c0 = -1.20446226E+02`.
2. **Código b** — se escribe después, con el acta del 8 aceptada. Sin incumplimientos; la dispensa
   PA-24 queda sin usar y **eso hay que decirlo en el acta**, no dejar que la invoque de oficio.
   `c3 = 0, c2 = 0, c1 = 3.20071157E-01, c0 = -1.82592593E+02`.
3. **Código 6** — no se escribe hoy. Está a un patrón de conseguirlo: falla por P87 (+11,1 %) y por
   el RMS del tipo IX (7,5 %). Medir P77, P76 o P122 (naranjas que faltan) y repetir P87 en 5 × 4.
4. **Código 4** — no se escribe. Mezcla tres tipos de lámina que no se comportan igual. Antes de
   volver a intentarlo, decidir si el rojo intenso admite una sola curva para IV, IX y XI.
5. **Código 3 y código 5** — **no se escriben, y no es cuestión de aflojar umbrales.** El dato está
   invertido dentro del mismo tipo de lámina y por un margen quince veces mayor que la
   repetibilidad. Lo que toca es verificar los certificados de los verdes de 51 (P109, P124) y de
   los azules de 84-101 (P18, P112, P67, P127) contra el otro retrorreflectómetro patrón, como el
   operador ya empezó a hacer en S129. **Si se escribe el 5 hoy, P81 —el control que el propio Diego
   apartó para esto— pasaría a leer 45 contra un certificado de 92.**
6. **Antes de escribir**, dejar constancia de que la `s_rep` que va a juzgar la re-medida
   (0,149 % en la sesión 1) está medida dentro de una serie y es entre cinco y veinte veces menor
   que la reproducibilidad real de re-colocación. Es previsible que la re-medida del 8 y del b falle
   su criterio 2 sin que nada esté mal.

**Yo no escribo nada en el equipo y no propongo hacerlo yo: eso lo decide Diego.**

---

*Informe generado a partir del ZIP de soporte del 19-sep-2026 18:11:20 (md5 comprobado). El ZIP no
se modificó; se trabajó sobre copia en directorio temporal. No se tocó `D:\IT\wt_rtv10` (sólo
lectura) ni ningún `01_Firmware/`. No se hizo commit.*
