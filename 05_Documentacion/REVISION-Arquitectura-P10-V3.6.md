# Revisión de arquitectura P10 — V3.6 (app 3.6.10 sobre SPEC r1.3; firmware 3.6.2 sin cambios)

**Nada de lo que aprueba este documento está implementado ni medido.** La 3.6.10 no existe. El banco
P1-P132 no se ha medido. Ninguna respuesta a la orden `9` se ha registrado nunca con la V3.6. Todo sale
de leer el fuente, los documentos y el ZIP de las 12:27. Los cálculos hechos en esta revisión se marcan
como tales.

- **Fecha:** 19-sep-2026, tarde. Revisor adversario. Es la fase 2 de 4 del ciclo: la decisión es si se
  desarrolla la app 3.6.10.
- **Repositorio:** `D:\IT\P_RetroVertical_V3.6`, HEAD `1127dfb` (tras `git pull`: "Already up to date").
- **App revisada:** RTV 3.6.9, `f52eeb1` (`app/build.gradle:14-15`: `versionCode 369`,
  `versionName "3.6.9"`). **Es la primera revisión del delta 3.6.7-3.6.9**, es decir, de `70266b1` a
  `f52eeb1`: 22 ficheros y +2499 líneas en `app/src`.
- **Pruebas JVM:** **89 de 89 en verde**, compiladas aquí desde el fuente con JDK 11, junit 4.13.2 y
  hamcrest 1.3, y ejecutadas con `app/` como directorio de trabajo. Desde otro directorio fallan 25,
  porque no encuentran `src/main/assets/…csv`.
- **Firmware:** 3.6.2, último cambio de fuente en `6a32ca3` (`git log -- 01_Firmware`; `78924ae` sólo
  añade un log de grabación).
- **Contra:**
  - `SPEC-V3.6.md` r1.3 §3.7 quater (RF-APP-33 a RF-APP-48);
  - `SPEC-Calibracion-V3.6.md` §12 (RF-CAL-35 a RF-CAL-43, PA-10 a PA-23, C-CAL-16 a C-CAL-22);
  - `TDD-V3.6.md` §3 ter;
  - `MATRIZ-SPEC-codigo-V3.6.md`;
  - `QA-Flujo-Calibracion-V3.6.md`;
  - `06_Calibracion/PLAN-Captura-Banco-P1-P132.md` y `cola_banco_P1-P132.csv`;
  - `06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md` (en adelante **REFORM**);
  - `REVISION-Arquitectura-P9-V3.6.md`.
- **Evidencia de campo:** `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip`. Su md5
  `ce1f35fc64439cbb602d014b725fadfb` se ha recalculado aquí y coincide. Su registro
  `tramas/rtv36_20260919_114644.txt` es **T4**; los tiempos van en ms desde las 11:46:44.

Rutas de app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Rutas de firmware
relativas a `01_Firmware/RetroVertical_V3.6.X/`.

---

## 1. Veredicto

**APROBADO CON CONDICIONES.** Se puede empezar a desarrollar la 3.6.10 con la SPEC r1.3, con dos
salvedades:

- **Tres puntos de la SPEC están mal y hay que corregirlos antes de codificarlos:** la comprobación 1
  de RF-CAL-39 (§2.1), el método del código 8 (§2.2) y la relación entre la cola y la decisión PA-14
  (§2.3).
- **El orden de construcción es el de §6:** primero la integridad del acta, después el banco y al final
  el botón.

### 1.1 Condiciones

| N.º | Condición | Comprobación | Bloquea |
| :--- | :--- | :--- | :--- |
| **P10-C1** | Rehacer la **comprobación 1 de RF-CAL-39** (§2.1): por par y con detección de patrón ausente, no con la media d̄ frente a 3·s_d/√n | T-A54 (a), con los pares de T4:2083-2156, sale "colocación no válida" | Codificar RF-APP-38 |
| **P10-C2** | **Cerrar el método del código 8** entre SPEC-Cal:775 (grado 1) y REFORM:19, :159 y :446 (recta anclada, "escribir ya"). También el b (§2.2) y la dispensa del código 2 | Tabla RF-CAL-37 única, citada por PLAN, REFORM, T-A52 y AT-07/T-S06 | Codificar RF-CAL-37 |
| **P10-C3** | **Separar la cola del método** (§2.3). La cola dice qué se mide (K, M y orden). El uso (AJUSTE o VERIFICACIÓN) y el patrón de re-medida salen de la tabla RF-CAL-37 del APK. En la cola, **K = 5 en P81 y en todos los OSCURO**, sea cual sea PA-14 | Cola nueva con su md5. Una PA-14 distinta no cambia la cola | Medir el banco |
| **P10-C4** | **D-20:** el acta guarda todos los intentos, incluidas las colocaciones no válidas. Hay como máximo una repetición válida. Reescribir un código anula su entrada anterior, y hoy la deja bloqueando para siempre (§5, hallazgo 2) | T-A55 y T-S12, y el caso del hallazgo 2 como prueba nueva | Cualquier acta nueva |
| **P10-C5** | **El acta sólo es aceptable si lo que certifica está en el equipo.** Justo antes de aceptar: `#V#` y `#G,k#` de cada código del acta, iguales a lo certificado. `#F` desde Avanzado con un acta abierta la invalida. Si `#SC` falla, el acta no se cierra (§5, hallazgos 6 y 7) | Pruebas nuevas en T-A (JVM): `#F` tras una re-medida conforme, y `#SC` sin respuesta | Cualquier acta nueva |
| **P10-C6** | **s_rep no se teclea ni se supone** en Operación (RF-CAL-38). Hoy el campo `edSrep` lo abre hasta dar todo CONFORME (`AdminActivity.java:134-136`, `:471`; `Asistente.java:234`) | T-A53 | RF-APP-38 |
| **P10-C7** | **Catálogo P1-P132 dentro del APK** (PA-21). Hoy sólo está P1-P31 (`Sesion.java:229`): con la 3.6.9 no se puede medir P32-P132 | T-A56; `assets/` con md5 `07ab9cd8…` | Medir el banco |
| **P10-C8** | **Importar de forma atómica:** o todo o nada, sin pisar lo que eligió el operador, y comparando el firmware de las series (§5, hallazgo 12) | Prueba JVM: un ZIP con un patrón desconocido en la fila 40 deja el diario igual | Medir el banco en 3 sesiones |
| **P10-C9** | **Corregir `SPEC-Calibracion-V3.6.md` §12.8** con lo que sale del código: 177 sólo con V > 12,28 V; n puede pasar de 177; la V es la de antes del destello; y los bytes que no empiezan por `#` se pierden mientras se atiende un `9` (§4) | Revisión del documento | No |
| **P10-C10** | **Anotar el acta del 19-sep sin reescribirla** (§3): primer intento NO CONFORME de P28, "x = 575" frente a 565,4 y `DEF` frente a `CAL`. **T-C41** (persistencia y `#V#`) en el próximo contacto con SLV-002 | Anexo en `06_Calibracion/SLV-002/` y registro de T-C41 | No bloquea desarrollar. Sí bloquea dar la P8 por cerrada |
| **P10-C11** | La 3.6.10 se entrega en **dos cortes** (§6): **A**, para medir el banco, con su validación en simulador; **B**, "Calibrar este equipo". Diego puede empezar el banco con el corte A | Dos APK con nombre versionado (RF-APP-41) | Organización |
| **P10-C12** | P9-B3 (el mismo protocolo en campaña, re-medida y campo) **cambia de 1 + 9 a 1 + 4**. Las curvas 1 y 2 de SLV-002 se ajustaron con 1 + 9. Se declara en el acta y se comprueba con la A5 del banco (C-CAL-22) | Nota de Diego y RF-CAL-43 | No |

---

## 2. Lo que esta revisión desmiente de la SPEC r1.3

### 2.1 La comprobación 1 de RF-CAL-39 no hace lo que sus pruebas esperan (nuevo)

RF-CAL-39.1 (`SPEC-Calibracion-V3.6.md:795-798`) declara no válida una colocación si
|d̄| > máx(2 ; 3·s_d/√n). T-A54 (a) (`TDD-V3.6.md:1064-1073`) y el criterio de RF-APP-38
(`SPEC-V3.6.md`, fila RF-APP-38) esperan que el intento de las 12:17:02 salga "colocación no válida".
**Con la fórmula de la SPEC no sale así.** Cálculo de esta revisión, con la curva `#G` del código 1 y
los 9 pares de T4:2084-2155:

- d = −3,1 · +0,8 · +0,8 · +1,5 · −3,5 · **+462,3** · −1,8 · −3,3 · −1,3;
- d̄ = +50,3 y s_d = 154,5, así que el umbral es 3·154,5/√9 = **154,5**;
- como |d̄| = 50,3 < 154,5, **la colocación pasa la comprobación 1**. Es el par atípico el que infla
  s_d.

Ese intento sale NO CONFORME por las comprobaciones 2 (x̄ = 1136,9 frente a 2277,9) y 3 (R̄ = 227,3
frente a 484). Con PA-12, eso **gasta la única repetición por un error de colocación**. Una prueba que
implemente fielmente la SPEC fallará T-A54 (a).

**Qué se propone (P10-C1).** Dos filtros, antes de contar la colocación:

- **Patrón presente.** El disparo de asentamiento tiene que caer en [0,8 ; 1,2]·x̄_banco del patrón. Si
  no, se muestra "Coloque P28" y no se gasta ningún disparo. El 12:17:02, el asentamiento dio `::592`
  (T4:2081-2083), a 27 cuentas del oscuro: el caso se habría detectado antes de medir.
- **Coherencia por par.** Cualquier par con |d_i| > máx(3 ; 2 %·R_#G(x_e,i)) invalida la colocación.
  En el segundo intento (T4:2162-2233) el mayor |d_i| es 4,5 (cálculo de esta revisión), frente a un umbral de unos 10: pasa.

**Nota de margen.** En ese segundo intento, d̄ = −1,44 frente al umbral de la SPEC, máx(2 ; 2,49) =
2,49. El sesgo negativo cuadra con el truncado a entero del firmware (−0,5 de media) más el ruido entre
el disparo de `e` y el del código. Con n = 20 (K = 5, M = 4), 3·s_d/√n baja a unos 1,7 y el umbral queda
en 2. El margen es de 0,5 unidades. T-S12 tiene que incluir un caso real de este tamaño antes de
fijarlo.

### 2.2 Tres documentos con tres métodos distintos para los mismos códigos

Recuento cruzado del subagente documental, comprobado aquí en la cola y en REFORM:157-160.

| Código | SPEC-Cal (RF-CAL-37, PA-14) | PLAN y cola | REFORM | Quién lo cierra |
| :---: | :--- | :--- | :--- | :--- |
| **8** | Grado 1 (`:775`); T-A52 espera grado 1 (`TDD:1053`) | Grado 1 (`PLAN:298`, `:384`) | **Recta anclada, escribir ya**, c1 `2.04279088E-01` (`REFORM:19`, `:159`, `:446`). El grado 1 "pasa `#S` por 0,33" | **Diego.** Se recomienda la anclada: es la opción que no queda en el filo de `#S` (RF-FW-31) |
| **b** | Sólo verificar; "después del banco" (`:776`, `:980`) | Sólo verificar (`PLAN:300`, `:391`; cola, filas 96-98) | **Se puede escribir ya**, anclada; "el banco no trae rojo tipo I" (`REFORM:20`, `:171`, `:447`) | **Diego.** La condición de PA-14 (x ≥ 10·s_osc = 20 cuentas) ya se cumple hoy (734-797 frente a 565,4). Así planteada, no decide nada |
| **5** | Anclada si PA-14, con cambio de regla (RF-APP-42) | VERIFICACIÓN; P81 con K = 3 (cola, línea 159) | "Ni con el banco; sólo con dispensa" (`REFORM:21`, `:449`) | Diego (PA-14). Además, la regla de RF-APP-42 hace ajustable también el **d** (0-73, 5 niveles), y T-A56 no dice qué esperar del d |
| **2** | "Dispensa: ninguna" (`:772`) | — | 4 incumplimientos en el reajuste (`REFORM:122`) | Agente de SPEC. **Es falso:** el acta registra conformidad pese a RF-CAL-14 y 15 (acta, línea 18) |

**Causa:** ningún documento del alcance cita REFORM. Se ha comprobado con `grep -i "reformulaci"` y
con Grep: sólo aparece en la propia REFORM. La SPEC (`80a0a97`, 12:54) es posterior a REFORM
(`66a7649`, 12:45).

**Arrastra un problema más:** si el 8 va anclado, es falso lo que dicen AT-07 (`QA:425`) y T-S06
(`TDD:1236`), que sin OSCURO "1 y 8 se pueden calibrar".

### 2.3 La cola no puede depender de una decisión pendiente

PA-14 cambia el uso de 13 azules y el K de P81: RF-CAL-35 pide K = 5 en el patrón de re-medida. Eso
obliga a cambiar la cola, su md5 y RF-APP-33. Además:

- el ancla del 5 (sesión 3) y la anclada de reserva del 3, 4 y 6 salen de OSCURO a **K = 3** (cola,
  filas 63 y 128), cuando PLAN:429 justifica K = 5 "porque es el ancla";
- RF-APP-48 dice "el OSCURO de la sesión" sin decir cuál.

**Propuesta (P10-C3):** medir P81 y todos los OSCURO a 5 × 4 cuesta unos minutos. A cambio, la cola se
congela y PA-14 pasa a ser una fila de la tabla RF-CAL-37.

**Regla para el ancla:** el ancla de cada código es la media de los OSCURO de inicio y de fin de la
sesión en que se midió ese color. Si hay deriva del OSCURO (RF-CAL-36), no hay ancla.

**Sensibilidad (cálculo de esta revisión).** En el 5, la `x` está a 250-310 cuentas del oscuro
(`PLAN:363-371`), así que 5 cuentas de error en el ancla son del 1,6 al 2,0 % en R. En el b (170-230
cuentas) son del 2,2 al 2,9 %. El acta del 5 y del b tiene que declarar esa sensibilidad. La deriva del
oscuro a lo largo del año **no está medida**.

### 2.4 Otras que corrige el agente de SPEC (no bloquean)

- "La serie del banco de ese patrón" (RF-CAL-39.2) queda sin definir cuando P28 sale 7 veces en la
  cola. **Propuesta:** la fila `RE-MEDIDA`, no la A5.
- Hay cifras iguales calculadas con bases distintas y sin decirlo:
  - P25: +1,0 % (SPEC-Cal:985) frente a +2,3 % (REFORM:226);
  - P5: −6,3 %, −6,5 % y −4,7 %;
  - x implícita de P28: 2211,5 frente a 2213,1, por el +0,5 del truncado. La `x` medida con `e` en el
    segundo intento es **2216,2** (cálculo de esta revisión, T4:2162-2229).
- PLAN:426: el paso a K = 5 afecta a 85 patrones, no a 88, y cuesta unos 52 min, no 54.
- RF-APP-34: su columna de pruebas omite T-S16 y T-S18. El criterio de "≤ 24 toques más uno por
  colocación" cuenta dos veces las colocaciones (QA:283).
- QA-Flujo no marca como superados su 5 × 4, su lista "se verificarán sin ajustar: 3, 4, 5", D-17 y
  AT-15, ni su "ninguna re-medida en todo T4" de D-01, que era verdad en el ZIP de las 12:10 y no en el
  de las 12:27.

**Lo que se sostiene, comprobado por el subagente documental con Python sobre HEAD:**

- md5 de la cola `5ba94658…`;
- 180 filas: 133 patrones distintos y 47 de control; 45, 48 y 40 patrones por sesión;
- 9 patrones a K = 5 y 124 a K = 3, todos con M = 4;
- 88 AJUSTE, 6 RE-MEDIDA y 39 VERIFICACIÓN;
- café y lila: 10 con código 4 y VERIFICACIÓN, ninguno de tipo I;
- sesiones de unos 73, 61 y 52 min;
- umbrales de deriva de 1,889 % y 2,11 %;
- MATRIZ: C 43, P 26, N 21, X 1, igual que su cabecera (`MATRIZ:212-221`);
- **todos** los RF-APP-33 a 48 y RF-CAL-35 a 43 tienen prueba en `TDD:1431-1459` y fila en la MATRIZ.

---

## 3. D-20 y el acta del 19-sep: ¿sigue siendo válida la calibración de los códigos 1 y 2?

**Sí. Los códigos 1 y 2 de SLV-002 siguen siendo válidos, y no hay que repetir ninguna re-medida por
D-20.** El acta sí queda incompleta: hay que anotarla, sin reescribirla.

### 3.1 Qué pasó en el intento NO CONFORME (T4:2080-2156, de 12:16:32 a 12:17:02)

| Disparos | `e` | Código 1 | Lectura |
| :--- | :--- | :--- | :--- |
| Asentamiento (T4:2081) | 592 | — | **Sin patrón**: a 27 cuentas del oscuro de las 11:58 (S060, 565,4) |
| Pares 1-5 (T4:2084-2123) | 611, 598, 598, 599, 599 | 17, 17, 17, 18, 13 | Sin patrón. El equipo evalúa bien su curva: R_#G(598) = 16,2 |
| Par 6 (T4:2124-2131) | 603 | **480** | **Se apoyó P28 entre los dos disparos** |
| Pares 7-9 (T4:2132-2155) | 2198, 2213, 2213 | 492, 495, 497 | P28 colocado |

R̄ = 2046/9 = 227,3, y el valor predicho es 176,7 (T4:2156). **La diferencia sale entera del par 6:**
d = +462,3, que repartido entre 9 da +51. **No es un fallo de la curva, es una colocación no válida.**
La app de hoy no tiene forma de distinguirlo, y por eso la repetición de las 12:17:36 fue legítima: 9
pares con P28 colocado, d̄ = −1,44, x̄ = 2216,2 (T4:2162-2234).

### 3.2 Por qué se sostiene la conformidad

- **Código 1, P28:** R = 497,8 frente a 484 certificado, **+2,9 %**. La `x` es un −2,7 % frente a S028
  (2277,9), dentro de 2·2,24 %·√2 = 6,3 %.
- **Código 2, P5:** R = 693,2 frente a 740, **−6,3 %**. La `x` es 2463,9 frente a 2459,3 (S024), un
  +0,2 %.
- Las dos cumplen RF-CAL-14 frente al certificado. Coincide con SPEC-Cal §12.0 y §12.7, con los datos
  recalculados aquí.
- **Entre las 12:04 y las 12:23 no hubo ninguna `#F`, `#P` ni otra `#S`** que las de T4:1660 y T4:2520.
  Búsqueda de esta revisión en los cinco registros del ZIP. El hallazgo 7 de §5 no afecta a este acta.

### 3.3 Gravedad de D-20

- **Como defecto de diseño: grave, BLOQUEA cualquier acta nueva (P10-C4).** La app deja repetir sin
  límite (`AdminActivity.java:225`, `:736-759`) y sobrescribe el intento anterior (`Acta.java:133`). El
  subagente de código lo reprodujo con una prueba JVM propia: tras NO CONFORME y CONFORME, el acta no
  contiene "NO CONFORME" y queda aceptable. Con un patrón mal certificado o una curva mala, bastaría
  repetir hasta que el ruido diera CONFORME. Con la tolerancia de hoy (2·3 % de R supuesto), eso no es
  improbable.
- **Para el acta de hoy: menor.** El intento omitido no es válido por construcción: no había patrón en
  6 de 9 pares. Y el registro bruto sobrevive en el ZIP versionado (T4:2156, md5 `ce1f35fc…`). Lo que
  falla es la integridad del documento, no la calibración.

### 3.4 Lo que sigue pendiente en SLV-002 (P10-C10)

1. **Anexo al acta, sin tocar el original:**
   - el intento de las 12:17:02 y su causa (§3.1);
   - "x = 575" (`Asistente.java:218`), cuando el ancla medida del 2 es 565,4;
   - "DEF mascara 0000" (acta, línea 3), cuando el equipo está en `CAL` (D-18, C-CAL-20);
   - "s_rep (supuesta)" (acta, líneas 14 y 21);
   - la conformidad a RF-CAL-14 y 15 con la nota "Ok" (acta, líneas 11 y 18). Queda como decisión de
     Diego, pero el texto no dice nada.
2. **T-C41:** apagar y encender; después `#V#` (tiene que decir `CAL,0003`), `#G,1#`, `#G,2#` y `#E` en
   5 puntos. **Sin esto la P8 no está cerrada para 1 y 2** (P9-B8, final).
3. **C-CAL-22:** la A5 de las 12:00 dio "no concluyente", y los tres patrones leyeron por debajo de
   10:33 tras cambiar la batería (REFORM:47-49; −1,1, −5,5 y −2,1 %). La A5 del inicio del banco la
   cierra. **Si esa A5 sale con los tres desplazados en el mismo sentido más allá de 2·s_rep, se
   reabre el 1 y el 2.** No antes.

---

## 4. Firmware: ¿cambia? No

**Se confirma que el firmware 3.6.2 basta.** Todas las órdenes que usan RF-CAL-35 a 43 y RF-APP-33 a 48
existen:

| Orden | Dónde |
| :--- | :--- |
| `#V#` | `calibracion_v36.c:635` |
| `#L` | `:644` |
| `#Q` | `:658` |
| `#G` | `:662` |
| `#E` | `:673` |
| `#S` | `:690` |
| `#F` y `#F,*#` | `:703` y `:705` |
| `#FT` | `:716` |
| `#GC` y `#GN` | `:728` y `:731` |
| `#SC` | `:734` |
| `#SN` | `:745` |
| `e` | `ecuacionesCalibracion.c:191` |
| códigos 1-8 y a-d | `ecuacionesCalibracion.c:141-190` |
| `9` | `gui.c:329` |

Ningún requisito pide una trama nueva. RF-APP-46 pide apagar el equipo, que es una acción sobre el
hardware.

**La orden `9` (`gui.c:329-357`, `measurement.c:124-146`, comprobado aquí en `gui.c`).** Los puntos 1 a
5 de SPEC-Cal §12.8 se confirman:

- se promedian las 3 mayores de 5 muestras (`measurement.c:133-141`; `orderArray` ordena de menor a
  mayor, `binary_utils.c:41`);
- V = ADC·0,001·4,3 (`:143-144`);
- bv' = 2·bv − 21 y `(int)` trunca (C99, `-std=c99`);
- `:<n>:` sin terminador, que la app acepta con `:([^:]{1,8}):` (`Tramas.java:38`, `:77-79`);
- la orden dispara la lámpara (`measurement.c:222-225`).

Lo que hay que precisar (P10-C9):

- **El 177 sólo sale con V > 12,28 V** y si la `9` cae en la vuelta siguiente a un recorte. Sin
  recorte, **n puede pasar de 177**: con 13 V da 242. La SPEC no lo dice.
- n = 0 ⇔ V < 10,342 V. El recorte a 0 da el mismo resultado, así que no es una excepción.
- **La V es la de la vuelta anterior, sin la carga de la lámpara.** bv' no persiste, porque `gui.c:350`
  lo sobrescribe.
- **Mientras se atiende un `9`, se pierde todo byte que no empiece por `#`** (`clearBuffer`,
  `uart_module.c:23-26`, `gui.c:343`). Además, cualquier trama que empiece por `9` cuenta como batería
  (`strncmp(...,1)`, `gui.c:329`).
  - Consecuencia para la app: nada detrás de un `9` hasta tener `:n:` o agotar el tiempo de espera, y
    nunca un `9` en mitad de una serie. RF-APP-43 ya lo pide.
- **Dudas de hardware que no se resuelven con código:**
  - que la referencia sea de 4,096 V: `ADREF = 0x02` es VREF+ externo (`adcc.c:101-102`), y el valor
    depende del componente;
  - que la resistencia de 10k sea la inferior del divisor (comentario de `measurement.c:144`, no cotejado
    con el esquemático);
  - `ADCLK = 0x00` (Fosc/2) con `ADACQ = 0` (`adcc.c:83-86`, `:105-106`) podría dejar el TAD fuera de
    especificación. **No se ha comprobado aquí** con la hoja de datos.
  - Las tres se cierran con T-C44 (polímetro), no reprogramando.

---

## 5. Apps 3.6.7-3.6.9: lo que no se había revisado

Subagente de código, con evidencia `archivo:línea`. Los hallazgos 1, 2 y 7 se han comprobado aquí
(`Acta.java:104-135`; `AdminActivity.java:815-826`, `:930-948`).

| # | Hallazgo | Evidencia | Gravedad |
| :---: | :--- | :--- | :--- |
| 1 | La re-medida NO CONFORME desaparece del acta, y se puede repetir sin límite (D-20) | `Acta.java:133`; `AdminActivity.java:225`, `:736-759` | **Bloquea** (P10-C4) |
| 2 | Reescribir el **mismo** código tras un NO CONFORME deja la entrada vieja: `motivoNoEscribir` y `motivoNoAceptable` quedan bloqueados para siempre, y la única salida es rechazar el acta | `Acta.java:110-113`, `:125`, `:182-188` | Corregir (P10-C4). **Nuevo** |
| 3 | La re-medida no compara con el certificado. `p.valor` no interviene | `Acta.java:141-155`; `AdminActivity.java:772-774`, `:804` | Corregir (RF-APP-38) |
| 4 | El operador puede teclear s_rep, y la tolerancia se abre a voluntad; el acta no dice que se tecleó | `AdminActivity.java:134-136`, `:471`, `:724`; `Asistente.java:234` | Corregir (P10-C6). **Nuevo en su alcance** |
| 5 | `#SC` **sólo al aceptar** en la 3.6.9: el riesgo de la 3.6.7 que señaló P9 r2 está cerrado. Al rechazar, lo escrito se queda en EEPROM y sólo lo dice un aviso | `AdminActivity.java:821`, `:837-840` (3.6.7: `9ea7b6e`, `:554`) | Cerrado |
| 6 | El acta se acepta aunque `#SC` falle, e imprime "firmware sin #SC", que es falso en una 3.6.2 | `AdminActivity.java:821-822`; `Acta.java:232` | Corregir (P10-C5). **Nuevo** |
| 7 | `#F` no consulta el acta, y al aceptar no se relee `#G`. Escribir, re-medida conforme, `#F` y aceptar deja un acta aceptada de una curva que no está en el equipo | `AdminActivity.java:813-826`, `:932-948` | Corregir (P10-C5). **Nuevo** |
| 8 | El acta pendiente vive sólo en memoria y se borra al reconectar (D-02) | `Sesion.java:130`; `ConexionActivity.java:81` | Corregir (RF-APP-36) |
| 9 | Una restauración fallida sólo queda en el texto: ni detiene ni se anota en el acta | `AdminActivity.java:614-636`, `:685-709` | Corregir (RF-APP-37) |
| 10 | D-18 confirmado: marca y máscara sólo se asignan en las pruebas | `Pruebas.java:262-263`; `Sesion.java:118-119`, `:170-171`, `:196`; `AdminActivity.java:729` | Corregir (RF-APP-44) |
| 11 | Recta anclada: el ancla es la media del OSCURO de la campaña, pero el acta y B13 usan 575 | `AdminActivity.java:496`, `:698-699`; `Asistente.java:218`, `:483`, `:527` | Corregir (RF-APP-48) |
| 12 | Importar no es atómico, pisa lo elegido sin decirlo, no compara el firmware y acepta cualquier MAC con la campaña vacía; además muta la campaña fuera del hilo de interfaz | `ImportadorCampana.java:82`, `:102-104`, `:219-232`, `:255-258`; `CampanaActivity.java:713-755`; `Campana.java:166-168` | Corregir (P10-C8). **Nuevo** |
| 13 | "El firmware 3.6.1 rechaza #S" se muestra también con la 3.6.2 (T4:1839 y otras) | `Asistente.java:273`, `:276` | Menor |
| 14 | Catálogo P1-P31 solamente | `Sesion.java:229` | **Bloquea el banco** (P10-C7) |
| 15 | El selector de re-medida puede mostrar un patrón y usar otro | `AdminActivity.java:230-242` | Menor (desaparece con RF-CAL-37) |
| 16 | El PIN va enmascarado en el registro | `Registro.java:96`, `:103`; `Hex.java:72-80` | Sin defecto |

---

## 6. MVP ordenado para el desarrollador

El objetivo es que Diego mida el banco y calibre con un botón. Los tres bloques van en este orden y no
se solapan. **El corte A es lo imprescindible para medir. El corte B es lo imprescindible para
calibrar.**

### Corte A: medir el banco (3.6.10-a)

Diego puede empezar el banco en cuanto pase T-S01.

| Orden | Qué | RF | Por qué va aquí |
| :---: | :--- | :--- | :--- |
| A1 | Catálogo P1-P132 en `assets/` con su md5 | RF-APP-42 (sólo el catálogo), PA-21 | Sin esto no se mide nada nuevo (§5, hallazgo 14) |
| A2 | Cola congelada según P10-C3, leída con md5 permitido. **K y M salen de la cola**, no son constantes | RF-APP-33, RF-APP-47 | Si Diego cambia PA-10, cambia un fichero, no el código |
| A3 | Pasos de control que no se pueden saltar (OSCURO, A5, batería) y reanudación en el primer paso pendiente | RF-APP-33 | 190 min en 3 sesiones: sin reanudar no se acaba |
| A4 | Exportar al final de cada sesión, con copia en `Download/RTV/` y aviso de no desinstalar | RF-APP-40 | D-09: la campaña del teléfono ya se perdió una vez |
| A5 | Importar atómico | P10-C8, RF-APP-39 (sólo la lógica) | Las 3 sesiones se juntan por ZIP |
| A6 | Batería con `9`: aviso y V en el diario. El bloqueo de escrituras puede esperar al corte B, porque en A no hay escrituras | RF-APP-43, RF-CAL-41 | T-C44 se hace en la sesión 1 |
| A7 | APK con nombre versionado y la versión en la cabecera | RF-APP-41 | Es barato y evita D-10 |

### Corte B: calibrar con un botón (3.6.10-b)

| Orden | Qué | RF | Por qué va aquí |
| :---: | :--- | :--- | :--- |
| B1 | Acta y estado de la Fase B en disco, de sólo añadir, y P9-B8 evaluado sobre ese estado | RF-APP-36 | Es la base de todo lo demás (D-02) |
| B2 | Re-medida con P10-C1 + RF-CAL-39.2 y .3, todos los intentos en el acta, una repetición y restauración | RF-APP-38, P10-C4, P10-C6 | D-19 y D-20 |
| B3 | `#V#` fresco y relectura de `#G` antes de aceptar; con `#SC` fallido, el acta no se cierra; `#F` invalida el acta | RF-APP-44, P10-C5 | Hallazgos 6, 7 y 10 |
| B4 | Corte durante `#S`, automático | RF-APP-37 | D-03 |
| B5 | Tabla RF-CAL-37 en el APK, **cerrada tras P10-C2**; oscuro y s_rep de la campaña; deriva por sesión | RF-CAL-37, RF-APP-48, RF-CAL-36, RF-CAL-38 | Quita decisiones al operador |
| B6 | Persistencia dentro del flujo | RF-APP-46 | Final de P9-B8 |
| B7 | Identidad por `#GN#` y alta de serie con doble entrada | RF-APP-35 | D-04 y D-05 |
| B8 | Botón "Calibrar este equipo" con tarjetas por código, y bloqueo de escrituras por batería | RF-APP-34, RF-APP-43 (bloqueo) | Es la capa de interfaz: va la última, sobre B1-B7 |
| B9 | Campos del acta de RF-CAL-43 | RF-CAL-43 | Se completa a lo largo de B1-B8 |

### Puede esperar (3.6.11 o después)

- RF-APP-45 completo, con Avanzado detrás de confirmación tecleada. En B8 basta con que la pantalla de
  Operación no tenga controles destructivos.
- RF-APP-39 como cambio de disposición de pantalla.
- La regla de la anclada para el 5 y el d (RF-APP-42, segunda parte), si Diego no aprueba PA-14.
- El informe de café y lila frente a la curva del 4 (RF-CAL-40): se miden igual en el banco, y el
  informe puede hacerse con el script.
- F-1 a F-4 (firmware): fuera de alcance.

---

## 7. Decisiones de Diego: los supuestos de trabajo y cuáles bloquean

Se desarrolla con la recomendación del agente de SPEC como supuesto, salvo en PA-14, donde se propone
desacoplar.

| PA | Supuesto | Si Diego decide otra cosa | ¿Bloquea? |
| :--- | :--- | :--- | :--- |
| PA-10 | K = 3 / K = 5, con M = 4 | Cambia la cola, no el código (A2) | No |
| PA-12 | Una repetición y, si falla, restaurar | Es una constante de la tabla RF-CAL-37 | No, si se parametriza |
| PA-13 | PIN sin cambios en el flujo | Nada que programar en Operación | No |
| **PA-14** | Anclada para el 5, con la regla de RF-APP-42 | Con P10-C3 la cola no cambia. La regla de cobertura sí se programa, y también qué pasa con el d | **Bloquea B5 hasta decidirlo, junto con P10-C2 (8 y b)** |
| PA-15 | Café y lila sólo se verifican | Cambia el uso en la tabla | No |
| PA-16 | Grado 1 en 3, 4 y 6, con la anclada de reserva | Tabla | No. Los nueve verdes de 51 esperan a Diego (C-CAL-21) |
| PA-17 | Aviso con n < 19 y bloqueo con n = 0 o sin respuesta | Constantes | No, pero se dan por buenas sólo después de T-C44 y T-C45 |
| PA-19 | P25 como re-medida del 2 | Tabla | No |

**Decisión nueva que se pide a Diego (P10-C2):** método del 8 y del b con las cifras de REFORM delante.
Se recomienda:

- **8, recta anclada.** La de grado 1 queda a 0,33 unidades de que `#S` la rechace.
- **b, anclada después del banco.** Hay que medir la deriva del OSCURO antes de fiarle una recta que
  vive a 170-230 cuentas del ancla (§2.3).

---

## 8. Contradicciones abiertas y quién las cierra

| ID | Contradicción | Fuentes | Cierra |
| :--- | :--- | :--- | :--- |
| **C-P10-1** | La comprobación 1 de RF-CAL-39 no invalida el intento de las 12:17:02 que T-A54 (a) y RF-APP-38 dicen que invalida | `SPEC-Cal:795-798`; `TDD:1064-1073`; T4:2084-2155; §2.1 | Agente de SPEC (P10-C1) |
| **C-P10-2** | Método del 8: grado 1 frente a recta anclada | `SPEC-Cal:775`; `PLAN:298`, `:384`; `TDD:1053`; `REFORM:19`, `:159` | Diego |
| **C-P10-3** | Código b: sólo verificar frente a escribir ya | `SPEC-Cal:776`, `:980`; cola, filas 96-98; `REFORM:20`, `:171` | Diego |
| **C-P10-4** | P81 como re-medida del 5 con K = 3 y VERIFICACIÓN en la cola | `SPEC-Cal:742-743`, `:774`; cola, línea 159 | Agente de SPEC (P10-C3) |
| **C-P10-5** | "Dispensa: ninguna" en el código 2 frente a la conformidad del acta | `SPEC-Cal:772`; acta, línea 18 | Agente de SPEC |
| **C-P10-6** | El OSCURO que hace de ancla, a K = 3 en las sesiones 2 y 3 | `PLAN:429`; cola, filas 63 y 128 | Agente de SPEC (P10-C3) |
| **C-P10-7** | Con la regla de RF-APP-42, el d también sería ajustable | `SPEC-V3.6.md`, RF-APP-42; `TDD:1085-1087` | Agente de SPEC y Diego |
| **C-P10-8** | AT-07 y T-S06 ("sin OSCURO, 1 y 8 se calibran") frente a un 8 anclado | `QA:425`; `TDD:1236` | Se cierra con C-P10-2 |
| **C-P10-9** | §12.8 incompleto: el 177 sólo con V > 12,28 V, n > 177 posible y pérdida de bytes | `gui.c:329-357`; `uart_module.c:23-26` | Agente de SPEC (P10-C9); después, medir (T-C44) |
| **C-P10-10** | P9-B3 (1 + 9) frente al protocolo del banco (1 + 4) | `REVISION-P9:463`; `PLAN:34-35` | Diego (P10-C12) |
| C-CAL-17 | La orden `9`, "rota" frente a invertible | §4 | Medir: T-C44 |
| C-CAL-20 | Acta con `DEF` y x = 575 | Acta, líneas 3, 13 y 20 | T-C41 y anexo (P10-C10) |
| C-CAL-21 | Nueve verdes de 51 | Catálogo | Diego, con el certificado |
| C-CAL-22 | A5 no concluyente frente a acta aceptada | REFORM:47-49 | A5 del inicio del banco |
| — | Cifras con bases distintas: P25, P5, x de P28 y s_rep | §2.4 | Agente de SPEC (menor) |

---

## 9. Cómo se ha comprobado

**Pares de T4 y cuentas de §2.1 y §3.1.** ZIP de las 12:27 extraído en el scratchpad (md5 recalculado,
`ce1f35fc…`). Los pares se leyeron en T4:2080-2234 y T4:2686-2762. d_i, d̄, s_d y el umbral de
RF-CAL-39.1 se calcularon con Python y la curva `#G` del acta (línea 10). Búsqueda de `#F`, `#S` y `#P`
en los cinco registros de `tramas/`: sólo aparecen T4:1660 y T4:2520.

**Tres subagentes, con encargos acotados.**

- **Delta de la app `70266b1..f52eeb1`.** Ejecutó las 89 pruebas JVM y escribió una prueba propia
  (fuera del repositorio) que reproduce los hallazgos 1 y 2.
- **Firmware de la orden `9` y órdenes usadas.** Revisó `gui.c`, `measurement.c`, `adcc.c`,
  `uart_module.c` y `calibracion_v36.c`.
- **Coherencia documental.** Hizo los recuentos con Python sobre la cola y la MATRIZ en HEAD.

**Contraste de lo que dijeron los subagentes.**

- Los hallazgos que sostienen condiciones se han vuelto a abrir aquí:
  - `Acta.java:104-135`;
  - `AdminActivity.java:815-826` y `:930-948`;
  - `gui.c:326-357` y `uart_module.c:20-27`;
  - la cola, línea 159 y filas `RE-MEDIDA`;
  - `REFORM:157-160`.
- El subagente documental dio por buena la afirmación de §12.0 de que el intento de las 12:17:02 es "no
  válido". **Esta revisión la contradice (§2.1):** la SPEC acierta en el diagnóstico, pero su fórmula
  no lo produce.

**Lo que no se ha hecho.**

- No se ha medido nada.
- No se ha recompilado ningún APK.
- No se ha cotejado el divisor de batería con el esquemático.
- No se ha comprobado el TAD del ADC con la hoja de datos.
