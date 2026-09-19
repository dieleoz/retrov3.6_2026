# SPEC-Calibracion-V3.6 — Calibración contra patrones certificados

**Nada de esta especificación se ha ejecutado de principio a fin contra un equipo.** La campaña de
medida de SLV-002 del 19-sep-2026 está a medias. El ajuste, la escritura con `#S` y la re-medida no
se han hecho nunca. Los criterios de aceptación de §5 son **propuestas** sin norma que los fije. No
hay incertidumbre calculada (§9). Mientras eso siga así, lo que produce este proceso es un **ajuste
contra patrones**, no una calibración trazable.

**Revisión r2 (19-sep-2026, tarde): §12.** Añade el banco guiado de los 133 patrones P1-P132, el flujo
"Calibrar este equipo", RF-CAL-35 a RF-CAL-43, los defectos D-18 a D-20, las contradicciones C-CAL-16 a
C-CAL-22 y las decisiones PA-10 a PA-24. **Corregida tras la revisión P10** (`REVISION-Arquitectura-P10-V3.6.md`): métodos del 8 y del b, dispensa del 2, K de la cola, comprobación 1 de RF-CAL-39 y §12.8. **Tampoco está implementado ni medido.** Desde las 12:23 del
19-sep, SLV-002 tiene escritos y re-medidos los códigos 1 y 2, con el acta aceptada (§12.0): lo que
decía el párrafo anterior sobre la escritura queda superado para esos dos códigos, y se deja escrito.

Escrita el 19-sep-2026 a partir del código y de los datos de campo de ese día. El código citado es
el de la app en el commit `ff66f93` (app 3.6.5) y el del firmware V3.6.1 (`.hex` md5
`8736c05d…`, grabado en SLV-002 según el commit `869d3c6`). Otros agentes están cambiando la app, el
firmware y la SPEC mientras se escribe esto: una línea citada puede moverse, y **la referencia
estable es el nombre del método o de la constante** que se cita junto a ella.

**Prefijos de las citas:**

| Prefijo | Ruta |
| :--- | :--- |
| *(ninguno)*, `.java` | `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` |
| `assets/` | `03_App_Movil/RetroV36/app/src/main/assets/` |
| `fw:` | `01_Firmware/RetroVertical_V3.6.X/` |
| `C1` | `07 pruebas/19092026_0900/medidas_SLV-002_20260919_085924 (1).csv` (3 disparos por serie, 08:59-09:05) |
| `C2` | `07 pruebas/19092026_0900/p29_p24_p23_p5/medidas_SLV-002_20260919_091213 (3).csv` (9 disparos por serie, 09:12-09:25) |
| `V5:` | `D:\IT\P_RetroReflectometro_Vertical\` (repositorio V5) |
| `V4.6:` | `D:\IT\P_RetroVertical_V4.6\` |

`C1` y `C2` son las copias más largas de cada fichero. Las demás son prefijos suyos (L-21). La
carpeta `07 pruebas/` no está en git.

---

## 0. Qué existía y qué faltaba

Antes de escribir esto se buscó una especificación de calibración equivalente con Grep (`calibraci`
en todos los `.md` de los tres repositorios) y con listados de `05_Documentacion/` y
`06_Calibracion/`. No la hay. Lo que existe está repartido así:

| Documento | Qué cubre | Qué le falta para ser la especificación de calibración |
| :--- | :--- | :--- |
| `PROCEDIMIENTO-Calibracion-V3-K42.md` (idéntico en V3.6 y V5, md5 `9529c763…`) | El firmware de **2020**: calibrar consiste en editar `ecuacionesCalibracion.c`, recompilar y grabar por ICSP (§6). Captura con `e`, 10 lecturas + 3×5 recolocaciones (§4), ajuste "primero una recta" (§5) | No conoce la EEPROM ni `#S`/`#G`/`#E` de la V3.6. Tampoco los patrones tipo I ni la decisión C, y no tiene criterios de aceptación |
| `SPEC-V3.6.md` §3.4-3.7, §6 bis | RF-APP-13 a 22: sesión de patrones, avisos de cobertura, ajuste de grado 1-2, "como llegó", confirmación, restauración (C3), acta (RF-APP-21) | Criterios de aceptación del resultado, incertidumbre, modo superadministrador e informe PDF. Además **se ha quedado atrás del código** en cobertura y en método de medida (§10, C-CAL-07 y C-CAL-08) |
| `TDD-V3.6.md` F7 | T-C10 (sesión con `e`) y T-C28 (de extremo a extremo; tolerancia "± max(suelo, 2·s)") | El suelo no está fijado. No hay prueba de aceptación del ajuste |
| `V4.6:RUNBOOK-desde-V3.6.md` fases 9-11 | Orden de la campaña, del ajuste y de la escritura, con las lecciones L-16 a L-22 | Es un orden de trabajo, no una especificación. Su criterio de salida de la fase 9 (s ≤ 10) contradice al código (C-CAL-09) |
| `06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md` | Acta del cambio de firmware (antes y después de grabar) | No es un acta de calibración |

**Qué aporta este documento:** reúne en un solo sitio lo que ya estaba repartido, con las citas
comprobadas contra el código de hoy. Añade lo que faltaba: la tabla de cobertura del catálogo actual
(§2), los criterios de aceptación propuestos (§5), el contenido del acta (§7), el modo
superadministrador y el informe PDF (§8), lo que hace falta para declarar incertidumbre (§9) y las
contradicciones que siguen abiertas (§10). **No sustituye** a `SPEC-V3.6.md`. Donde las dos
difieran, se anota la contradicción en §10 y no se elige.

---

## 1. Alcance y trazabilidad

### 1.1 La magnitud medida y la salida

- **Entrada del modelo: la `x` interna.** Es el ADC filtrado más 200 (`fw:measurement.c:247`),
  multiplicado por el factor de temperatura `X_2·T² + X_1·T + X_0` (`fw:gui.c:301`). Los valores de
  fábrica son `X_2 = 0`, `X_1 = 0,00043212` y `X_0 = 0,90148651` (`fw:gui.c:42-44`). El resultado se
  guarda en `reflectivityValue`, que es `unsigned int` (`fw:gui.c:30`), así que **la `x` es entera**:
  su resolución es de 1 cuenta. La orden `e` la devuelve sin ecuación.
- **Salida: `R`, el coeficiente de retrorreflexión** en mcd·lx⁻¹·m⁻² (la unidad de la NTC 4739).
  Sale de `R = c3·x³ + c2·x² + c1·x + c0`, evaluado por `aplicarEcuacion()`
  (`fw:calibracion_v36.c:229-234`). Se trunca a entero y `arreglar_dato()` pone a 0 todo lo que pasa
  de 4000 (`fw:ecuacionesCalibracion.c:49-51`). **La resolución de la salida es 1 unidad de `R`.**
  Con patrones de valor 6-10, como los verdes y azules tipo I, eso es entre el 10 y el 17 % del valor.
- **`T` no se sabe qué es.** Se lee del canal `INREFLECT30`, y su sensor y sus unidades están sin
  verificar (`PROCEDIMIENTO-Calibracion-V3-K42.md` §1.6). Por eso la temperatura del informe es **la
  ambiente, anotada a mano** (§8.3), y no la `T` del equipo.

### 1.2 Los patrones como referencia

El catálogo que usa la app es `assets/patrones_certificados_P1-P31.csv`: **50 patrones en 51 filas**,
porque P32 aparece dos veces (P32a y P32b). Contiene:

| Grupo | Patrones | Tipos | Origen | Líneas |
| :--- | :--- | :--- | :--- | :--- |
| Intensos ("OTROS PAPELES") | P1-P31 | 17 XI, 9 IV, 5 IX | "Datos certificados", entregados por Diego el 18-sep-2026 | `assets/…csv:7-37` |
| Opacos ("PAPEL TIPO I") | P32a, P32b, P33-P50 | 20 tipo I | Lista de Diego del 19-sep-2026 | `assets/…csv:38-60` |

Lo que **no** se sabe, y así lo dice el propio catálogo:

- **El color no está certificado.** Diego lo aportó por inspección (`assets/…csv:3`).
- **La geometría del certificado no está identificada**: ni el ángulo de observación ni el de
  entrada. Tampoco las unidades ni el certificado de origen (`assets/…csv:4`).
- **La incertidumbre de los patrones no se conoce.** No figura en el catálogo ni en ningún documento
  de los tres repositorios. Sin ella no hay incertidumbre del equipo (§9).
- **Los patrones tipo I no traen ninguna nota de certificado.** Sólo patrón, valor, color y tipo
  (`assets/…csv:38-40`). Que su valor esté certificado se da por supuesto y **no está confirmado**.
- **No hay que confundirlos con los testigos P2-P40 de 2018**, que tienen los mismos nombres y otros
  valores (`assets/…csv:5`).

### 1.3 La norma

La referencia es la **NTC 4739** (clasificación de láminas por tipo, de I a XI, y coeficientes
mínimos por tipo, color y geometría). Esa norma remite a **ASTM D4956** y, para el método de medida,
a **ASTM E810**. Las transcripciones propias están en el repositorio V5. Aquí sólo se citan; **los
PDF no se copian** (son norma de pago, `V5:CLAUDE.md` §9):

- `V5:04_Manuales/NTC-4739-Requisitos.md`: Tabla 1 (tipo I, `:131`), Tabla 5 (tipo IV, `:160`),
  Tabla 9 (tipo IX, `:210`), Tabla 10 (tipo XI, `:225`), criterio de conformidad 7.3.1.1 (`:267`) y
  condiciones de ensayo 7.1 y 7.3 (`:422-450`).
- `V5:04_Manuales/NTC-4739-Estado-Actual.md`: la edición vigente es la NTC 4739:2020, y la que se
  transcribió es la segunda actualización. Además, no se encontró en Colombia oferta de calibración
  trazable en retrorreflexión (resumen ejecutivo, punto 5).

**Lo que la norma no da.** La NTC 4739 fija **mínimos de producto** para lámina nueva, no la
exactitud de un retrorreflectómetro. Tampoco fija la geometría de medida: la delega en ASTM E810
(`V5:04_Manuales/NTC-4739-Requisitos.md:436-441`). Por eso ningún criterio de §5 sale de la norma.
Todos van marcados como propuesta.

---

## 2. Qué código se calibra con qué patrones

### 2.1 La regla que aplica la app

`Asistente.cobertura()` decide, código a código, hasta qué grado se puede ajustar:

1. **Qué patrones sirven para cada código:** los de su color y su clase. Los códigos intensos (1-6)
   usan los tipos distintos de I, y los opacos (7, 8, a-d) usan tipo I (`Asistente.java:39-45`;
   correspondencia código → color en `Fabrica.java:32-46`).
2. **Grado + 2 niveles certificados distintos.** Con menos de 3 niveles no se ajusta ni una recta
   (`Asistente.java:88-91`). El grado máximo es `min(2, niveles − 2)` (`Asistente.java:97`).
   `Ajuste.ajustar()` sólo admite grado 1 o 2 (`Ajuste.java:42`) y exige grado + 2 valores de `x`
   distintos (`Ajuste.java:49`).
3. **Rango mínimo.** Si el rango certificado es menor de 20 unidades de `R` (`RANGO_MIN_ABS`,
   `Asistente.java:34`) o menor del 30 % del valor mayor (`RANGO_MIN_REL`, `Asistente.java:36`), el
   código **sólo se verifica**. Ajustarlo sería extrapolar a ciegas (`Asistente.java:92-96`).

### 2.2 Tabla resultante con los patrones de hoy

Se calculó reproduciendo la regla anterior sobre `assets/…csv`, y se cotejó con la prueba JVM
`AsistenteTest.coberturaDelCatalogoConLosTipoI` (`app/src/test/.../AsistenteTest.java:60-78`), que
comprueba los mismos grados. **Coinciden.**

| Código | Ecuación | Patrones | Niveles | Rango de R | Rango / máx. | Resultado |
| :---: | :--- | :--- | :---: | :--- | :---: | :--- |
| **1** | blanco intenso | P1, P2, P3, P4, P6, P7, P27, P28 (XI, IV, IX) | 8 | 378-828 | 54 % | **Ajustable hasta grado 2** |
| **2** | amarillo intenso | P5, P8, P9, P10, P20-P26, P29-P31 (XI, IV, IX) | 14 | 334-782 | 57 % | **Ajustable hasta grado 2** |
| 3 | verde intenso | P13-P16 (XI) | 4 | 164-173 | 5 % | Sólo verificar: rango estrecho |
| 4 | rojo intenso | P11, P12 (IV) | 2 | 194-227 | 15 % | Sólo verificar: 2 niveles |
| 5 | azul intenso | P17-P19 (XI) | 3 | 83-85 | 2 % | Sólo verificar: rango estrecho |
| 6 | naranja intenso | — | 0 | — | — | **Sin patrones**: se queda la curva de fábrica sin verificar |
| 7 | blanco opaco | P35 (I) | 1 | 96 | — | Sólo verificar: 1 nivel |
| **8** | amarillo opaco | P34, P37, P43, P44 (I) | 4 | 64-122 | 48 % | **Ajustable hasta grado 2** (con 1 grado de libertad) |
| a | verde opaco | P40, P45 (I) | 2 | 6-7 | 14 % | Sólo verificar: 2 niveles |
| **b** | rojo opaco | P38, P39, P49 (I) | 3 | 46-81 | 43 % | **Ajustable hasta grado 1** (con 1 grado de libertad) |
| c | azul opaco | P32a, P36, P42, P46-P48 (I) | 3 | 7-10 | 30 % | Sólo verificar: rango de 3 unidades (< 20) |
| d | naranja opaco | P32b, P33, P41, P50 (I) | 4 | 68-73 | 7 % | Sólo verificar: rango estrecho |

**Comprobado contra el código: la tabla del encargo es correcta.** 1 y 2 son ajustables hasta grado
2, 8 hasta grado 2, b hasta grado 1, y 7, a, c y d sólo se verifican. De 3, 4, 5 y 6, **ninguno** se
puede ajustar con el catálogo de hoy.

**Advertencias sobre la tabla:**

- En **8** y **b** el ajuste máximo deja **un solo grado de libertad**. Un patrón mal medido basta
  para mover la curva sin que el residuo lo delate. Propuesta (§4.4): ajustar **grado 1** en los dos
  salvo que el grado 2 esté justificado.
- La **c** pasa por la regla relativa (3/10 = 30 %, justo en el límite) y se bloquea por la
  absoluta. Con valores de 7-10 y resolución de salida de 1 unidad, verificarla es ya difícil (§1.1).
- La columna "patrones" es el **catálogo**. Lo que se ajusta es lo **medido**: `Asistente.proponer()`
  vuelve a aplicar la regla sobre los patrones que tengan serie elegida (`Asistente.java:295-305`).
  Un patrón sin medir baja la cobertura real.

---

## 3. Procedimiento de medida (campaña)

La campaña se hace con la app 3.6.5, en la pantalla "Campaña de calibración" (`CampanaActivity`),
que lee la `x` con `e`. Cada paso lleva la lección de la que sale (L-xx, en
`V4.6:APRENDIDO-DE-V3.6.md`).

**RF-CAL-01 — Calentamiento.** El equipo lleva al menos **10 min** encendido antes de la primera
serie, y toda la campaña se hace en el mismo régimen térmico. La temperatura ambiente se anota al
principio y al final. *La cifra de 10 min viene de `PROCEDIMIENTO-Calibracion-V3-K42.md:243` y **no
está medida** en SLV-002.* **La app no la hace cumplir**: no hay temporizador de calentamiento
(búsqueda de "calent" con Grep en el código de la app: sin resultados). Criterio de aceptación: la
hora de encendido y la de la primera serie constan en el acta.

**RF-CAL-02 — Disparo de asentamiento.** Antes de cada serie se hace **1 disparo que se descarta**.
Va al registro de tramas, pero no a la media ni al CSV (`Sesion.java:59`, `disparosAsentamiento = 1`;
`LecturaX.java:40-46`). Motivo (**L-16**): en 17 de 17 series de C2 el primer disparo quedó por
debajo de la mediana de los otros ocho. **La causa no está medida** (C-CAL-04).

**RF-CAL-03 — 9 disparos por serie**, sin mover el equipo ni tocar el gatillo (valor por defecto,
`CampanaActivity.java:62-63`). El equipo no se levanta hasta que la app da la serie por terminada
(**L-17**).

**RF-CAL-04 — Rechazo de descolgados.** Un disparo que se aparta de la mediana de su serie más de
**5 sigmas robustas** se descarta. La sigma robusta es `1,4826·MAD`, con un suelo de 1,5 cuentas, y
sólo se aplica a partir de 4 disparos (`Veredicto.java:29,34,36,67-86`). Caso real: P7, un disparo
de 3031 entre valores de ~3230 (C2:109). El descarte queda en el diario como evento `DESCARTE`: no se
borra nada (`Campana.java:197-202`).

**RF-CAL-05 — s ≤ 15 cuentas o repetir.** Si la desviación típica de la serie, ya sin descolgados,
pasa de 15 cuentas, el veredicto es `REPETIR` (`Veredicto.java:30,107,126`). Referencia medida: en
C2 las series limpias dan s de 3,2 a 7,7 cuentas (medias de los disparos 2-9, calculadas para este
documento). **Hay otro umbral de 10 en el código** (C-CAL-09).

**RF-CAL-06 — Orientación de los XI.** Cada serie registra su orientación, 0 o 90°, y el diario y
el CSV la guardan (`Campana.java:59`, evento `SERIE`). La cola incluye una prueba de giro de **P5 a
0 y 90°** (`Cola.java:24,79-89`). Motivo (**L-22**): los XI no siguen el orden de su certificado.
Propuesta de `L-22`, **no implementada**: medir un XI en cuatro orientaciones (0, 90, 180 y 270°).

**RF-CAL-07 — "¿Es este el patrón?"** Antes de aceptar una serie, la app la compara con lo que ya
se ha medido en la campaña (`Veredicto.evaluar()`, `Veredicto.java:91-128`):

- **Orden:** dentro del mismo color y tipo, un patrón de certificado mayor no puede dar claramente
  menos `x`, con una tolerancia del 3 % (`TOL_ORDEN`, `Veredicto.java:31,130-148`).
- **Parecido:** si la media está a ±1 % de la de otro patrón ya medido, de certificado distinto, y
  no se acerca más a la `x` esperada, la app lo avisa (`Veredicto.java:32,177-200`).

Una serie dudosa se repite o se reasigna ("era otro patrón"), y el original queda en el diario
(`Campana.java:211-219`). **Nunca se reetiqueta a mano después** (**L-18**). Además, antes de cada
serie el operador lee en voz alta la etiqueta física del patrón (L-18; **procedimiento, no lo hace
cumplir la app**).

**RF-CAL-08 — Segunda pasada y conflictos.** Si dos series aceptadas de un mismo patrón difieren más
de un 3 % (`CONFLICTO_REL`, `Campana.java:347`), el patrón pasa a "prioritario" y la cola lo pide de
nuevo (`Campana.prioritarios()`, `Campana.java:371-419`). Caso real: P24, con 2065 frente a 2443
(C-CAL-01). Se mide una segunda pasada de todos los patrones de los códigos ajustables
(`V4.6:RUNBOOK-desde-V3.6.md`, fase 9, acción 7).

**RF-CAL-09 — Una campaña por equipo.** La campaña va atada a **serie y MAC**. Nunca se mezclan
series de dos equipos: `nuevaSerie()` rechaza las de otra MAC (`Campana.java:124-126,178-183`), y el
diario las salta al leerlo (`Campana.java:544-546`). Los coeficientes de un equipo **no se copian a
otro** (**L-23**).

**RF-CAL-10 — Un solo ZIP.** Al final se comparte **un único ZIP** con `campana.csv`, `resumen.txt`,
el diario, las pruebas del equipo y los registros de tramas de la campaña (`Campanas.exportar()`,
`Campanas.java:167-200`). Se comparte una vez, no patrón a patrón (**L-21**). El SHA-256 de ese ZIP
ata el informe a los datos crudos (§8.3).

**RF-CAL-11 — Estado "como llegó".** Antes de ajustar se leen con `#G` los 12 juegos de
coeficientes y los 3 de temperatura (`#GT#`), y se guarda una copia (RF-APP-16 y RF-APP-22 de
`SPEC-V3.6.md`). Con esas curvas, la app evalúa las `x` medidas y anota la desviación de cada
patrón frente a su certificado (`Asistente.java:340-343`).

---

## 4. Modelo y ajuste

### 4.1 El modelo

`R = c3·x³ + c2·x² + c1·x + c0`, con los 4 coeficientes en EEPROM, uno de 12 juegos por código. El
firmware lo evalúa **en float de 32 bits y en el orden de 2020**: potencias de izquierda a derecha,
sin Horner (`fw:calibracion_v36.c:226-234`). La app emula ese cálculo en
`Ecuacion.respuestaFloat32()` (`Ecuacion.java:51-68`). Esa emulación supone que la biblioteca de
XC8 redondea como IEEE-754: es el riesgo R-05 de `SPEC-V3.6.md`, y la contradice en parte la medida
de T-A23 (**L-20**). El asistente sólo ajusta grado 1 o 2, así que **c3 = 0** en toda curva nueva
(`Ajuste.java:86`).

### 4.2 El ajuste: decisión C

**Decisión C de Diego (19-sep-2026, `ROADMAP.md:42-47`)**, para los códigos 1 y 2: una sola curva
de compromiso entre XI e IV/IX. Se ajusta por **mínimos cuadrados con todos los patrones, sin
ponderar por tipo**, y el acta **declara el error residual de cada tipo**. Motivo: una curva no
corrige a la vez el XI (+2-3 %) y el IV/IX (+25-50 %) (**L-07**).

Cómo lo hace el código:

- **Los puntos del ajuste son medias**, una por patrón, de sus disparos válidos de la serie elegida
  (`Asistente.puntos()`, `Asistente.java:136-158`; `Campana.medidasElegidas()`, `Campana.java:422-437`).
- **Sin ponderar:** las ecuaciones normales suman todos los puntos con el mismo peso
  (`Ajuste.java:64-77`), sobre `u = (x − media)/escala` para no perder precisión (`Ajuste.java:54-63`).
- **Aviso de mezcla de tipos:** si una curva junta IV, IX y XI, la app lo avisa (`Asistente.java:306-312`)
  y no lo bloquea. Es lo que la decisión C pide.
- **El residuo por tipo lo tiene que calcular el acta.** El informe del asistente da el residuo por
  patrón, con su tipo, y el error máximo y el RMS **globales** (`Asistente.java:332-345`). **No da el
  resumen por tipo.** Falta, y lo exige §5 (RF-CAL-15).

### 4.3 Validación de la curva

Una curva nueva tiene que pasar **dos controles**. Si falla cualquiera, no se escribe.

| Control | Qué exige | Dónde |
| :--- | :--- | :--- |
| **C2 de la revisión de arquitectura** | Creciente en todo `x` = 200-4400; nunca por encima de 4000 en ese rango; no negativa desde la `x` mínima medida hasta 4400. Por debajo de la `x` mínima, un negativo sólo se avisa | `Asistente.validarForma()`, `Asistente.java:161-208` |
| **Criterio de `#S` del firmware 3.6.1** | `R(x)` finita y en [0 ; 4000] en `x` = 600 y en `x` = 4300 y en los puntos críticos que caen entre los dos (donde `R'(x) = 0`) | `fw:calibracion_v36.c:316-319` (límites), `:391-396` (`respuestaEnRango`), `:404-423` (`curvaValida`) y `:587-588` (llamada en `#S`) |
| El mismo criterio, en la app | Se evalúa en float32 en cada `x` entera de 600 a 4300. Si falla, no se envía nada | `Asistente.criterioFirmwareS()`, `Asistente.java:211-241`; `AdminActivity.java:421-426` |

Origen de cada límite: **4000** sale de `arreglar_dato()`. **4300** sale del ADC a fondo más 200.
**600 no sale del código**: lo fijó el coordinador (`fw:calibracion_v36.c:314-315`, **L-19**). La
curva de fábrica del código 2 no pasa este criterio, porque es negativa desde `x` = 4175 (L-19): el
código 2 sólo vuelve a fábrica con `#F,2#`, nunca con `#S`.

Además, el asistente enseña la curva actual y la nueva en `x` = 500, 1000, 2000, 3000 y 4000, y marca
como **extrapolación** lo que cae fuera del rango medido (`Asistente.java:369-375`).

### 4.4 Cuándo bajar a grado 1

Se usa grado 1 (una recta) cuando se da **cualquiera** de estas condiciones:

1. **La parábola no pasa C2 o el criterio de `#S` y la recta sí.** La app lo detecta y lo propone
   (`Asistente.java:354-366`).
2. **La parábola deja 1 grado de libertad o menos** (códigos 8 y b, §2.2). *Propuesta de este
   documento.*
3. **El residuo de la parábola no supera claramente el error puro** de repetibilidad y colocación
   (`PROCEDIMIENTO-Calibracion-V3-K42.md:262-268`). *Criterio numérico propuesto:* se pasa a grado 2
   sólo si el RMS del residuo baja al menos un 30 % y la mejora es mayor que la `s` de los patrones
   convertida a `R`. **Sin validar.**
4. **El vértice cae a menos de 500 cuentas del borde del rango de uso.** *Propuesta:* una parábola
   casi en su techo convierte el ruido en una `R` ambigua, que es el caso del blanco y el amarillo de
   fábrica (`Coherencia.java:44-52`).

### 4.5 Ejercicio en papel con los datos de C1 y C2 (ilustrativo, no es un ajuste)

Para dimensionar §5 se calculó, fuera de la app, el ajuste de la decisión C con las medias de los
disparos 2-9 de la última serie de cada patrón en C2 y, para P8, P9, P10, P30 y P31, con los disparos
2-3 de C1. **No es una propuesta de escritura**: mezcla dos campañas, las series no pasaron por el
veredicto de la app, P24 está excluido (C-CAL-01) y faltan P27 y P28.

| Curva | XI (RMS del residuo) | IV (RMS) | IX (RMS) | Peor patrón | ¿Pasa `#S` 3.6.1? |
| :--- | :---: | :---: | :---: | :--- | :--- |
| 1, grado 1 (6 patrones) | 2,1 % | 8,0 % | — | P2, −8,9 % | Sí (R(600) = 57) |
| 1, grado 2 | 2,7 % | 5,2 % | — | P2, −6,2 % | Sí (R(600) = 202) |
| 2, grado 1 (13 patrones) | 3,8 % | 5,1 % | 2,3 % | P22, −7,9 % | Sí (R(600) = 85) |
| 2, grado 2 | 3,2 % | 3,4 % | 3,1 % | P30, +6,0 % | **No**: negativa por debajo de `x` ≈ 720 (R(600) ≈ −62) |

Qué enseña, **sin darlo por medido**:

- La decisión C deja residuos de entre el 2 y el 9 %, muy por debajo del +25-50 % que tienen hoy
  las curvas de fábrica en IV/IX (**L-07**).
- **El amarillo de grado 2 repite la forma de la curva de fábrica y `#S` lo rechazaría (L-19).** Con
  los datos de hoy, el código 2 se escribiría en grado 1, por la condición 1 de §4.4.

---

## 5. Criterios de aceptación

**Todos los umbrales de esta sección son PROPUESTA.** No los fija ninguna norma (§1.3). Salen de
los datos de SLV-002 del 19-sep-2026 y del ejercicio de §4.5. Diego los aprueba o los cambia antes
de la primera escritura (P-CAL-01). Donde el código ya aplica un umbral, se dice.

**RF-CAL-12 — Aceptación de una serie** (lo aplica el código):

- n ≥ 8 disparos válidos, contando el asentamiento aparte.
- s ≤ 15 cuentas (`Veredicto.java:30`).
- Veredicto `OK`, o `DUDOSO` aceptado con nota (`Campana.java:204-209`).

**RF-CAL-13 — Repetibilidad y reproducibilidad por patrón:**

- **Repetibilidad:** la `s` de la serie elegida, en cuentas. Va al acta sin umbral adicional al de
  RF-CAL-12.
- **Reproducibilidad (propuesta):** para cada patrón de un código ajustable, **dos series aceptadas
  con recolocación**, cuyas medias difieran menos de `max(3·s, 1 %)`. El código sólo detecta
  conflicto a partir del 3 % (`Campana.java:347`), que es más laxo. Referencia medida: P5 pasó de 2521
  (C1, mediana de 3 disparos) a 2584 (C2): **+2,5 %**, y lo aceptarían los dos criterios. P23 pasó de
  2721 a 2757 en 9 min (C2:38-46 y C2:137-145): **+1,3 %**, y 3·s ≈ 22 cuentas. **Con el criterio
  propuesto P23 se repetiría; con el del código, no.**

**RF-CAL-14 — Residuo por patrón** de la curva nueva, para los códigos ajustados (propuesta):

- |`R_cert − R_curva`| ≤ **máx(10 % del valor certificado ; 2 unidades de R)**.
- El suelo de 2 unidades existe porque la salida es entera (§1.1) y hay patrones tipo I de 46 a 122.
- Un patrón que no cumple **no se quita del ajuste sin dejarlo escrito en el acta** (L-22).

**RF-CAL-15 — Residuo por tipo** (lo pide la decisión C; propuesta de umbral):

- Para cada tipo de lámina presente en la curva: residuo medio (sesgo) y RMS, en % del certificado.
- Se acepta si el **sesgo de cada tipo** es ≤ 5 % y el **RMS de cada tipo** es ≤ 6 %.
- El acta los declara **siempre**, cumplan o no.
- Hoy el informe del asistente no hace este resumen (§4.2). Hasta que lo haga, lo calcula quien
  firma el acta, con `campana.csv`.

**RF-CAL-16 — No empeorar (propuesta).** Para cada tipo, el RMS de la curva nueva sobre los patrones
medidos tiene que ser menor que el de la curva "como llegó" (RF-CAL-11). Si no lo es, no se escribe.

**RF-CAL-17 — Verificación con patrones no usados en el ajuste:**

- **En los códigos 1 y 2 no hay patrones libres**: la decisión C los usa todos. Se sustituye por
  **validación cruzada dejando uno fuera**: se reajusta sin cada patrón y se predice ese patrón. El
  error de predicción de cada uno cumple RF-CAL-14 con el umbral multiplicado por 1,5 (propuesta).
  **La app no lo hace**: se calcula aparte.
- **En los códigos 8 y b** no tiene sentido con 3-4 niveles.
- **Los códigos que sólo se verifican** (3, 4, 5, 7, a, c, d) se miden con su propio código
  **después** de escribir los demás. No se ajustan: el acta anota su desviación frente al
  certificado con la curva de fábrica. *Propuesta:* se marca "fuera" una desviación mayor que la
  tolerancia de RF-CAL-14, para que Diego decida si hace falta un patrón nuevo.

**RF-CAL-18 — Re-medida tras escribir** (§6, RF-CAL-22): con el código escrito, no con `e`, cada
patrón del código cumple

- |`R_medida − R_predicha`| ≤ **máx(2 unidades ; 2·s_R)**, donde `s_R` es la `s` de la serie
  convertida a `R` con la pendiente local de la curva. Es la forma de T-C28 (`TDD-V3.6.md:732-736`),
  con el suelo de 2 unidades como **propuesta**.
- Y, frente al certificado, RF-CAL-14.

---

## 6. Escritura y verificación

Todo en **modo administrador** (`#L,<pin>#`). Sale de `AdminActivity` (app 3.6.5) y de
`PROTOCOLO-V3.6.md`. La escritura exige además la sesión de superadministrador de §8
(**no implementada**).

**RF-CAL-19 — Copia previa.** Antes de escribir se leen y guardan los 12 juegos y la temperatura
(`AdminActivity.escribir()`, `AdminActivity.java:514-518`). Si no se puede leer el estado anterior
del código, no se escribe nada.

**RF-CAL-20 — `#S` y relectura con `#G`.**

- La trama `#S` lleva 9 cifras significativas y cabe en 96 bytes (`Tramas.java:23`).
- Tras `#S`, `#G,k#` tiene que coincidir con lo enviado en **8 ulp** de float32 (`ULP_S`,
  `Ecuacion.java:93`). El origen de esa cifra es T-A23 del firmware 3.6.1: hasta 7 ulp en `c3` de
  1e-9 a 1e-6, más 1 de margen (**L-20**, `Ecuacion.java:82-91`).
- **8 ulp no es la condición C1** de `SPEC-V3.6.md` §6 bis, que pedía ida y vuelta exacta
  (C-CAL-12).

**RF-CAL-21 — `#E` en 5 puntos, la comprobación que manda.** `#E,k,x#` en `x` = 500, 1000, 2000,
3000 y 4000 (`Pruebas.java:319`) tiene que dar lo mismo que la curva enviada emulada en float32,
**con ±1 unidad** (`AdminActivity.comprobarPorE()`, `AdminActivity.java:498-512`).

**RF-CAL-22 — Restauración automática (condición C3).** Si `#S` responde `#ERR` o no responde, si la
relectura no coincide o si `#E` no reproduce la curva, la app restaura el estado anterior. Usa
`#F,k#` si era el de fábrica y `#S` con los coeficientes anteriores si no, y después relee
(`AdminActivity.restaurar()`, `AdminActivity.java:468-490`). **Sin prueba automática**: la lógica
vive en la actividad (T-A32 pendiente, `SPEC-V3.6.md` §6 bis).

**RF-CAL-23 — Re-medida de los patrones tras escribir.** Hoy la app **sólo lo pide con un texto**:
"Mida ahora al menos un patrón con el código k" (`AdminActivity.java:540`). **Requisito nuevo:** la
campaña ofrece una fase de verificación en la que se mide **cada patrón del código escrito** con ese
código (no con `e`), con asentamiento y 9 disparos, y se evalúa RF-CAL-18. El resultado va al ZIP y
al acta. Hasta que exista, se hace a mano con la pantalla de medida y se anota.

**RF-CAL-24 — Orden de escritura.** Primero un código, después su verificación y después el
siguiente. Nunca se escriben todos los códigos y se verifica al final. *Propuesta*: así una curva
mala se detecta antes de tocar las demás.

---

## 7. Acta de calibración

**RF-CAL-25 — Contenido mínimo del acta** (amplía RF-APP-21 de `SPEC-V3.6.md`). Se genera por
equipo y va en `06_Calibracion/<equipo>/` y en el informe PDF (§8).

| Bloque | Contenido |
| :--- | :--- |
| Equipo | Serie, MAC, cliente o propietario, placa, firmware (`#V#` completo con máscara), md5 del `.hex` grabado |
| App | versionName y versionCode, md5 del APK |
| Sesión | Fecha, hora de encendido, hora de la primera serie y de la última, temperatura ambiente al principio y al final, operador y superadministrador (§8) |
| Patrones | Por patrón: ID, tipo, color, valor, origen y certificado si se conoce (§1.2). Los no medidos, con el motivo |
| Medidas | Por patrón: serie elegida, media de `x`, s, n, descartados y su motivo, orientación, veredicto y nota. Las series no elegidas se listan con su media |
| Antes | Coeficientes "como llegó" de los 12 códigos y de la temperatura, y desviación de cada patrón con ellos (RF-CAL-11) |
| Ajuste | Por código: grado y motivo (§4.4), coeficientes nuevos, residuo por patrón (absoluto y %), **residuo por tipo** (sesgo y RMS), resultado de C2 y del criterio de `#S`, validación cruzada si procede (RF-CAL-17) |
| Decisión C | Transcrita: compromiso XI–IV/IX, mínimos cuadrados sin ponderar, fecha y quién decidió |
| Escritura | Por código: trama `#S` enviada, respuesta, relectura `#G` (ulp máximos), `#E` en 5 puntos, restauraciones si las hubo |
| Después | Coeficientes leídos tras escribir, re-medida de los patrones (RF-CAL-18) y verificación de los códigos que no se ajustaron |
| Aceptación | Criterio por criterio de §5, cumple o no cumple, con el umbral usado y la marca "propuesto" mientras lo sea |
| Contradicciones | Las de §10 que sigan abiertas ese día |
| Declaración | "Ajuste contra patrones, no calibración trazable", mientras falten la geometría, el certificado de origen de los patrones y la incertidumbre (§9) |
| Datos | Nombre del ZIP de campaña y su SHA-256 |

---

## 8. Modo superadministrador e informe de ajuste en PDF

**Nada de esta sección está implementado.** Es un requisito nuevo, pedido por Diego el 19-sep-2026.

### 8.1 Dos llaves distintas

| | Modo administrador (existe) | Superadministrador (nuevo) |
| :--- | :--- | :--- |
| Qué protege | El **firmware**: desbloquea `#S`, `#F`, `#FT`, `#P` | El **flujo de calibración en la app**: campaña → ajuste → escritura → verificación → informe |
| Dónde vive | En el equipo: PIN de 4 dígitos en EEPROM, de fábrica `2026` (`fw:calibracion_v36.c:180`, `PROTOCOLO-V3.6.md`, "PIN de fábrica") | En el móvil |
| Límite de intentos | 5 fallos bloquean `#L` hasta apagar (`fw:calibracion_v36.c:299`); caduca a los 10 min sin tramas `#` (`:298`) | Propuesto en RF-CAL-27 |
| Quién lo conoce | Quien tenga el PIN del equipo | Diego o quien él designe |

Hoy **cualquiera con la app 3.6.x y el PIN puede escribir coeficientes**. La app no registra quién
lo hizo: no existe ningún campo de operador (búsqueda de "operador", "tecnico" y "usuario" con Grep:
sólo aparece en comentarios y en un texto del registro, `AdminActivity.java:261`). Además, el PIN de fábrica está escrito en la documentación y **viaja
en claro por el SPP**.

### 8.2 Qué se puede sostener sin servidor

**La app no tiene permiso de INTERNET** (`AndroidManifest.xml:5-11`): los ficheros salen por
`ACTION_SEND` y los manda otra app. Sin servidor no hay forma de autenticar a una persona ni de
revocar una credencial a distancia. Quien tenga el APK y un móvil con acceso root puede saltarse
cualquier control local. **Lo que sigue es un control de procedimiento, contra el uso accidental o
no autorizado por un operador de campo. No es seguridad frente a un atacante, y no se debe
presentar como tal.**

**RF-CAL-26 — Separación por distribución (la medida más fuerte que cabe sin servidor).** El flujo
de calibración (campaña, asistente, escritura, informe) **no viaja en el APK de campo**. Va en una
variante de compilación "calibración" (otro `applicationId`, por ejemplo `com.dpi.retrov36.cal`) que
sólo tiene el superadministrador. Criterio de aceptación: en el APK de campo, las clases del
asistente y de la escritura no están en el `.dex` (comprobable con `apkanalyzer` o listando el `.dex`).

**RF-CAL-27 — Frase de superadministrador local.**

- En el primer uso del APK de calibración se crea una frase de al menos 8 caracteres.
- Se guarda **sólo su hash con sal** (PBKDF2-HMAC-SHA256, con ≥ 100 000 iteraciones y 16 bytes de
  sal) en el almacenamiento privado de la app. `allowBackup` ya es `false`
  (`AndroidManifest.xml:14`).
- La frase abre una **sesión de superadministrador** que caduca a los 30 min sin actividad o al
  cerrar la app.
- 5 fallos seguidos bloquean la entrada durante 15 min.
- Cambiarla exige la frase actual. **Si se olvida, se reinstala** y se pierden las campañas del
  móvil. Por eso la campaña sale siempre en ZIP (RF-CAL-10).

**RF-CAL-28 — Quién calibró.**

- Al abrir la sesión se teclean el **nombre del superadministrador** y el **del operador** (pueden
  ser la misma persona).
- Los dos nombres y un identificador del móvil (`Settings.Secure.ANDROID_ID` más el modelo) van al
  diario en un evento `SESION` y a cada evento `SERIE` y de escritura. Salen en el ZIP y en el
  informe.
- **Es una declaración, no una prueba de identidad.** Así lo dice el informe.

**RF-CAL-29 — PIN del equipo.** Antes de la primera escritura, el flujo de calibración **exige
cambiar el PIN de fábrica** (`#P`) si el equipo sigue con `2026`. El PIN nuevo no se guarda en la app
(RF-APP-19) y en el registro sale como `****`.

### 8.3 El informe en PDF

**RF-CAL-30 — Título.** Mientras el laboratorio no esté acreditado (ISO/IEC 17025) y no haya
incertidumbre calculada (§9), el documento **no puede llamarse "certificado de calibración"** ni
usar "calibración" en sentido metrológico en su título.

- **Título propuesto:** *"Informe de ajuste y verificación contra patrones certificados"*.
- **Decisión abierta de Diego (P-CAL-02).**
- El texto de la declaración de §7 va en la primera página, bajo el título.

**RF-CAL-31 — Generación.**

- La app genera el PDF al terminar la verificación (RF-CAL-23), **sin conexión**, con
  `android.graphics.pdf.PdfDocument`. Esa clase está disponible desde el API 19, y la app pide
  `minSdkVersion 24` (`app/build.gradle:12`), así que no hace falta ninguna biblioteca.
- El SHA-256 se calcula con `java.security.MessageDigest`, que viene con Android.
- El PDF se comparte con el mismo mecanismo que el ZIP: `FileProvider` más `ACTION_SEND`
  (`AndroidManifest.xml:36-44`; `CampanaActivity.java:480-500`).

**RF-CAL-32 — Contenido mínimo.** Lo derivado del acta (§7), y en particular:

1. **Identificador único:** `RTV-<serie>-<AAAAMMDD-HHMMSS>-<8 primeros hex del SHA-256 del ZIP>`.
2. Equipo (serie, MAC, firmware con `#V#` completo), cliente o propietario, fecha, operador y
   superadministrador (RF-CAL-28).
3. Patrones usados: ID, tipo, color, valor certificado y trazabilidad de su certificado. Donde no se
   conozca, el texto literal "no conocida" (§1.2), nunca un hueco.
4. Medidas por patrón (media, s, n), coeficientes antes y después, residuos por patrón y por tipo.
5. Verificación posterior con los patrones re-medidos (RF-CAL-18) y códigos sólo verificados.
6. Decisión de ajuste (decisión C), condiciones (temperatura ambiente si se anotó; si no, "no
   registrada"), contradicciones abiertas (§10) y **validez propuesta** (P-CAL-03).
7. **Nombre y SHA-256 completo (64 hex) del ZIP de campaña**, más una línea que diga cómo
   comprobarlo: `sha256sum <zip>` en Linux, `certutil -hashfile <zip> SHA256` en Windows.
8. Estado de cada criterio de §5, con la marca "propuesto" mientras lo sea.

**RF-CAL-33 — Atadura a los datos crudos.**

- El PDF **no va dentro** del ZIP cuyo hash cita, porque la referencia sería circular.
- Se entregan los dos ficheros juntos o en un segundo ZIP de entrega.
- El ZIP de campaña **no se regenera** después de emitir el PDF: `ZipOutputStream` graba la fecha de
  cada entrada, así que otro ZIP con el mismo contenido tendría otro hash.
- El PDF cita el fichero concreto que se compartió.

**RF-CAL-34 — Criterios de aceptación del PDF:**

- Genera el PDF con el móvil en modo avión.
- No lo genera si falta un campo obligatorio de RF-CAL-32 (1-8). Un dato desconocido va escrito
  como "no conocido", no como campo vacío.
- El SHA-256 impreso coincide con `sha256sum` del ZIP compartido, y el identificador contiene sus 8
  primeros caracteres.
- Se comparte por el menú de Android: aparece en el selector de `ACTION_SEND` con tipo
  `application/pdf`.
- El título es el de RF-CAL-30 mientras P-CAL-02 no se resuelva de otro modo.
- La prueba JVM que genere el contenido (sin la maquetación) comprueba los campos y el hash con un
  ZIP de ejemplo.

---

## 9. Incertidumbre: qué haría falta para declararla

**PENDIENTE. Hoy no se puede declarar.** Estas son las componentes que harían falta, con lo que hay
de cada una:

| Componente | Qué se necesita | Lo que hay hoy |
| :--- | :--- | :--- |
| **Patrones** | Incertidumbre expandida del valor certificado y su factor k, y geometría del certificado | **No conocida** (`assets/…csv:4`). Bloquea todo lo demás |
| **Repetibilidad** | `s` por patrón, convertida a `R` con la pendiente local | Medida: s = 3,2-7,7 cuentas en C2 |
| **Reproducibilidad** | Dispersión entre colocaciones, entre sesiones y entre días | Sólo indicios: P5 +2,5 % (C1→C2) y P23 +1,3 % en 9 min. No hay campaña de otro día |
| **Resolución** | De la `x` (1 cuenta) y de la salida (1 unidad de `R`, truncada): u = 1/√12, con sesgo de −0,5 por truncar | Calculable. Pesa en los tipo I de valor bajo (§1.1) |
| **Deriva** | Cambio de `x` en la sesión, sobre un patrón fijo | Indicio: `e` pasó de 3004 a 3027 durante la prueba de coherencia del G4 (`06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md`); no separa deriva de colocación |
| **Residuo del modelo** | El sesgo por tipo que deja la decisión C (RF-CAL-15) | Es sistemático: se declara por tipo y no se trata como ruido |
| **Temperatura** | Sensibilidad de la `x` a la temperatura ambiente | Sin medir. `T` del equipo, de unidades desconocidas (§1.1) |
| **Geometría y orientación** | Diferencia entre la geometría del equipo y la del certificado, y efecto de la orientación en los XI | Sin medir (L-22, C-CAL-02) |

Método propuesto cuando existan los datos: GUM, con suma cuadrática de las componentes
independientes convertidas a `R`, y el residuo del modelo declarado aparte por tipo, no sumado.
**Quién lo aprueba:** Diego (P-CAL-04).

---

## 10. Contradicciones abiertas

**No se elige ninguna.** Se cierran midiendo o las decide el propietario.

| ID | Contradicción | Fuentes | Cómo se cierra |
| :--- | :--- | :--- | :--- |
| **C-CAL-01** | **P24 (IV, 593):** dos series seguidas dan 2065 (2048/2071/2076) y 2443 (2439/2438/2453). La segunda se parece a un XI amarillo. **No hay ninguna serie válida de P24 en C2** | C1:17-22; L-18 | Medir P24 de nuevo, con la etiqueta leída en voz alta |
| **C-CAL-02** | **XI desordenados:** P7 (768) da 3237 y P6 (772) da 3115,5. P23 (714) da 2721-2757 y P5 (740) da 2584. P8 (721) da 2514 en C1. Causa sin medir: orientación, estado del patrón o certificado | C2:92-118, C2:38-46, C2:137-154, C1:5-7; L-22 | Prueba de giro (RF-CAL-06) y medida en 4 orientaciones |
| **C-CAL-03** | **IX e IV también desordenados** (hallado aquí): P26 (583) da 2148,9 y P25 (576) da 2202 (−2,4 %, dentro del 3 % de `TOL_ORDEN`, así que la app no avisa). P30 (448) da 1662 en C1 y P29 (442) da 1724,5 en C2 | C2:20-37, C1:26-28, C2:119-127 | Medir P25, P26, P29, P30 y P31 en la misma sesión |
| **C-CAL-04** | **El primer disparo sale bajo** (17 de 17 series; entre 6 y 40,5 cuentas). Causa sin medir. Se descarta por diseño (RF-CAL-02) sin saber por qué | L-16; `Sesion.java:52-59` | Medir la `x` en función del tiempo desde el disparo anterior |
| **C-CAL-05** | **P32 duplicado:** P32 azul (9) y P32 naranja (68). La app los separa como P32a y P32b | `assets/…csv:40-42` | Que Diego confirme la etiqueta física |
| **C-CAL-06** | **El catálogo del repositorio no es el de la app** (hallado aquí): `06_Calibracion/patrones_certificados_P1-P31.csv` tiene 37 líneas, sin tipo I (md5 `f254c46f…`, igual que el del V5); el de `assets/` tiene 60 (md5 `bc4604b0…`). Los tipo I sólo existen dentro de la app | `wc -l` y `md5sum`, 19-sep-2026 | Decidir cuál es el maestro y copiar el otro |
| **C-CAL-07** | **SPEC-V3.6 dice que sólo 1 y 2 son ajustables** y que "ninguno es de tipo I" (RF-APP-14, MOD r1.1). El código ajusta también 8 y b | `SPEC-V3.6.md` RF-APP-14; `AsistenteTest.java:71-72` | Actualizar la SPEC (la están editando otros agentes) |
| **C-CAL-08** | **Método de medida:** RF-APP-13 y el PROCEDIMIENTO §4 piden 10 lecturas más 3 recolocaciones de 5. La campaña hace asentamiento más 9, y recoloca sólo en la segunda pasada | `SPEC-V3.6.md` RF-APP-13; `CampanaActivity.java:63` | Diego decide qué método vale para el acta |
| **C-CAL-09** | **Dos umbrales de s:** 10 cuentas en la prueba 6 (`Repetibilidad.java:14`, "provisional") y en el criterio de la fase 9 del runbook; 15 en el veredicto de la campaña (`Veredicto.java:30`) | Código y `V4.6:RUNBOOK-desde-V3.6.md` fase 9 | Fijarlo con datos: en C2, s ≤ 7,7 |
| **C-CAL-10** | **El criterio de `#S` se comprueba de dos maneras:** el firmware evalúa en los bordes y en los puntos críticos reales, también en `x` no enteras (`fw:calibracion_v36.c:404-423`); la app, en cada `x` entera (`Asistente.java:227-239`). Una curva que toque 0 o 4000 entre dos enteros puede pasar en la app y dar `#ERR,FORMATO#` | Código | T-A de frontera; la app ya informa del `#ERR` |
| **C-CAL-11** | **Rango de validación distinto:** C2 exige creciente y ≤ 4000 en 200-4400; `#S`, R en [0 ; 4000] en 600-4300. El 600 no sale del código. Negativos por debajo de la `x` mínima: la app avisa (C-27 de la SPEC) y el firmware, desde 600, rechaza | `Asistente.java:161-208`; `fw:calibracion_v36.c:314-319` | Decisión del propietario (C-27 y P-09 de la SPEC) |
| **C-CAL-12** | **C1 pedía ida y vuelta exacta (0 ulp).** El firmware llega a 7 ulp y la app tolera 8 y comprueba con `#E` | `SPEC-V3.6.md` §6 bis; L-20; `Ecuacion.java:93` | Diego acepta la sustitución o se transportan los `float` en hex (L-09) |
| **C-CAL-13** | **El runbook dice que la V3.6.1 está "compilada y no grabada"**; el commit `869d3c6` registra SLV-002 grabado con la V3.6.1 el 19-sep-2026 a las 09:36 | `V4.6:RUNBOOK-desde-V3.6.md` fase 10; `git log` | Actualizar el runbook |
| **C-CAL-14** | **Reproducibilidad entre sesiones sin separar:** P5 +2,5 % (C1→C2) y P23 +1,3 % en 9 min. No se sabe si es deriva, colocación u orientación | C1:2-4; C2:38-46, C2:137-154 | Campaña de otro día (§9) |
| **C-CAL-15** | **Los tipo I no traen nota de certificado** y el color de todos los patrones es por inspección | `assets/…csv:3,38-40` | Que Diego aporte el certificado |

---

## 11. Preguntas para Diego

- **P-CAL-01:** ¿Aprueba los umbrales propuestos de §5 (10 % o 2 unidades por patrón; sesgo del 5 %
  y RMS del 6 % por tipo; no empeorar; 2·s en la re-medida)? ¿O los cambia?
- **P-CAL-02:** ¿Título del PDF? Propuesta: "Informe de ajuste y verificación contra patrones
  certificados".
- **P-CAL-03:** ¿Qué validez propone el informe? Sin datos de deriva a largo plazo no hay base. La
  propuesta es **no declarar validez** y fijar una **re-verificación a los 6 meses** o tras un golpe o
  una reparación.
- **P-CAL-04:** ¿Hay forma de obtener la incertidumbre y la geometría de los certificados de los
  patrones? Sin eso, §9 no avanza.
- **P-CAL-05:** Para los códigos 8 y b, ¿grado 1 aunque el catálogo permita 2 en el 8 (§4.4)?
- **P-CAL-06:** ¿APK de calibración separado del de campo (RF-CAL-26), o una sola app con la frase
  local (RF-CAL-27)? Recomendado: las dos cosas.

---

## 12. Revisión r2 (19-sep-2026, tarde): banco guiado y "Calibrar este equipo"

**Nada de esta sección está implementado ni medido.** Es la especificación que recibe el arquitecto
(fase 2 del ciclo acordado con Diego) y que implementarán la app 3.6.10 y, si hiciera falta, el
firmware. Parte de `QA-Flujo-Calibracion-V3.6.md` (commit `f45d2a8`, defectos D-01 a D-17 y AT-01 a
AT-22), de `REVISION-Arquitectura-P9-V3.6.md` (P9-A1 a A6 y P9-B1 a B13), del catálogo nuevo P1-P132
(commit `bba4dbe`) y de la cola de captura `06_Calibracion/PLAN-Captura-Banco-P1-P132.md`. Los
requisitos de la app que salen de aquí están en `SPEC-V3.6.md` §3.7 quater (RF-APP-33 a RF-APP-48), y
sus pruebas en `TDD-V3.6.md` §3 ter.

### 12.0 Estado de SLV-002 del que parte esta revisión

Confirmado con el ZIP `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip` (md5
`ce1f35fc64439cbb602d014b725fadfb`, commit `afdd700`). El acta es
`actas/acta_SLV-002_00211305193B_20260919_122326.txt` y el registro de tramas,
`tramas/rtv36_20260919_114644.txt` (en adelante **T4**; tiempos en ms desde las 11:46:44).

| Qué | Estado | Evidencia |
| :--- | :--- | :--- |
| Código 1 | **Escrito y verificado**, grado 1. `#S,1` a las 12:04:34; re-medida de P28 conforme a las 12:17:36 | T4:1660; T4:2234; acta, líneas 8-14 |
| Código 2 | **Escrito y verificado**, recta anclada en el oscuro S060 (x = 565,4). `#S,2` a las 12:20:46; re-medida de P5 conforme a las 12:23:13 | T4:2520; T4:2762; acta, líneas 15-21 |
| Código 8 | **No escrito**: sigue la curva de fábrica | Acta: sólo los códigos 1 y 2 |
| Acta | **ACEPTADA** a las 12:23:26 | Acta, línea 23; T4:2798 |
| Fecha | `#SC,2026-09-19#` y `#GC,2026-09-19#`: vence el 2027-09-19 | T4:2763-2798 |
| Serie | `SLV-002`, grabada de nuevo a las 12:14:04 tras el `SLV-02` de las 12:09:02 | T4:2069-2078; T4:2048-2057 |
| Persistencia (final de P9-B8) | **Sin comprobar.** No hubo apagado ni ningún `#V#` después del de las 11:46:56 | Único `#V#` de T4: línea 13 |
| Cabecera del acta y del resumen | **Falsa.** Dicen "DEF mascara 0000" y "No calibrado" porque salen del `#V#` de las 11:46:56, anterior a escribir | Acta, línea 3; `resumen.txt:3-4`; C-CAL-20 |

**Tres defectos nuevos que deja ver ese ZIP**, además de D-01 a D-17 del QA:

- **D-18. La app no vuelve a leer `#V#` después de escribir.** `s.marca` y `s.mascara` sólo se asignan
  en las pruebas (`Pruebas.java:262-263`; comprobado con Grep y con `grep -n`). El acta guarda
  `s.firmware()` al abrirse (`AdminActivity.java:729`), y el estado del resumen sale de
  `Calibracion.estado(f, marca, …)` (`Sesion.java:196`), que con `DEF` da "No calibrado"
  (`Calibracion.java:141-142`). **El acta aceptada certifica un firmware en `DEF` que ya no lo está.**
- **D-19. La re-medida no compara con el patrón, sino con el propio equipo.** `Acta.evaluarRemedida`
  compara la media de R leída con el código contra la curva `#G` evaluada en la `x` que se lee con `e`
  en la misma colocación (`Acta.java:141-154`; `AdminActivity.java:772-774`). Eso comprueba que el
  equipo evalúa bien la curva escrita: es lo mismo que `#E`, pero con señal real. **No comprueba la
  curva frente al certificado** (RF-CAL-18, segunda parte). Además, la tolerancia usa una s entre
  colocaciones (supuesta, el 3 % de R: `Acta.java:153`; `AdminActivity.java:804`), que no es la
  dispersión de esa comparación, porque `e` y el código se leen en la misma colocación.
- **D-20. La re-medida se puede repetir hasta que pase, y el acta sólo guarda la última.**
  `Acta.remedida()` sobrescribe (`Acta.java:133`). El 19-sep, la primera re-medida del código 1 dio NO
  CONFORME (R 227,3 frente a 176,7 predicha; T4:2156, 12:17:02). La segunda, 34 s después, dio
  CONFORME (T4:2234). El acta sólo trae la segunda (línea 14).

**Lo que la re-medida del 19-sep sí permite afirmar**, calculado aquí con la curva `#G` del acta:

| Código | Patrón | R leída | Certificado | Frente al certificado | `x` implícita | `x` de la campaña | Diferencia |
| :---: | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | P28 | 497,8 | 484 | **+2,9 %** | 2211,5 | 2277,9 (S028) | −2,9 % |
| 2 | P5 | 693,2 | 740 | **−6,3 %** | 2462,0 | 2459,3 (S024) | +0,1 % |

Las dos cumplen RF-CAL-14 (≤ 10 %) frente al certificado y reproducen la `x` de la campaña dentro de
2·s_rep·√2 = 6,3 %. **La conclusión "CONFORME" del acta se sostiene, pero por estas cuentas, no por la
que hizo la app.**

### 12.1 Catálogo P1-P132 y cobertura por código

El catálogo nuevo (`06_Calibracion/patrones_certificados_P1-P132.csv`, md5 del blob `07ab9cd8…`) tiene 133
patrones y añade, por primera vez, IV, IX y XI de todos los colores intensos (IV/IX/XI): rojo 6/4/6,
verde 3/6/10, azul 2/4/7 y naranja 3/6/6, más café 1/2/2 y lila 1/2/2. **La app no lo lleva**: carga
`assets/patrones_certificados_P1-P31.csv` por nombre fijo (`Sesion.java:229`). Esta tabla sustituye a
la del §2.2 (con 50 patrones) y se ha calculado con la misma regla (`Asistente.java:75-98`):

| Código | Patrones | Niveles | R | Rango / máx. | Regla | Antes (§2.2) |
| :---: | ---: | ---: | :--- | ---: | :--- | :--- |
| 1 | 16 | 16 | 347-828 | 58 % | Hasta grado 2 | Grado 2 |
| 2 | 24 | 24 | 207-782 | 74 % | Hasta grado 2 | Grado 2 |
| **3** | 19 | 11 | 51-173 | 71 % | **Hasta grado 2** | Sólo verificar |
| **4** | 16 | 15 | 68-279 | 76 % | **Hasta grado 2** | Sólo verificar |
| 5 | 13 | 12 | 83-102 | **19 %** | **Sólo verificar: rango estrecho** | Sólo verificar |
| **6** | 15 | 15 | 80-173 | 54 % | **Hasta grado 2** | Sin patrones |
| 7 | 1 | 1 | 96 | — | Sólo verificar | Igual |
| 8 | 4 | 4 | 64-122 | 48 % | Hasta grado 2 | Igual |
| a | 2 | 2 | 6-7 | 14 % | Sólo verificar | Igual |
| b | 3 | 3 | 46-81 | 43 % | Hasta grado 1 | Igual |
| c | 6 | 3 | 7-10 | 30 % | Sólo verificar (rango < 20) | Igual |
| d | 4 | 4 | 68-73 | 7 % | Sólo verificar | Igual |

**Contradicción con el encargo (C-CAL-16):** "los códigos 3, 4, 5 y 6 pasan a ser ajustables". Con la
regla, 3, 4 y 6 sí; **el 5 no**. La salida que se propone es la recta anclada en el oscuro (PA-14), y
exige cambiar la regla de la app (RF-APP-42). El detalle por código (patrones, rangos de `x`,
extrapolación y patrón de re-medida) está en `PLAN-Captura-Banco-P1-P132.md` §3.

### 12.2 Requisitos nuevos (RF-CAL-35 a RF-CAL-43)

Todos llevan criterio medible. Los umbrales nuevos son **propuesta** mientras Diego no los apruebe
(§12.11).

**RF-CAL-35 — Banco guiado de los 133 patrones.** La captura sigue, en su orden, la cola
`06_Calibracion/cola_banco_P1-P132.csv` (md5 `9ddb7882…`):

- calentamiento de 10 min (RF-CAL-01);
- en cada sesión, batería (`9`), OSCURO y A5 (P22, P28, P4) al inicio, y A5 y OSCURO al final;
- los patrones, agrupados por color;
- todo con `e` y M = 4, más 1 disparo de asentamiento por colocación;
- **K = 5** en **todos los OSCURO** (inicio y fin de cada sesión), en la A5 del inicio del banco, en el
  patrón de re-medida de cada código (**también P81, decida lo que decida PA-14**) y en los patrones de
  los códigos de 3-4 puntos (8 y b);
- **K = 3** en todo lo demás, también en la A5 del final y en las A5 de las sesiones 2 y 3.

**[Corregido tras P10-C3]** Antes, los OSCURO del final y de las sesiones 2 y 3, y P81, iban a K = 3
(`REVISION-Arquitectura-P10-V3.6.md` §2.3). **La cola dice qué se mide, no cómo se ajusta:** su columna
`uso` es informativa, y el uso, el método y el patrón de re-medida salen de la tabla RF-CAL-37. Una
decisión de Diego sobre PA-14 no cambia la cola. **Ancla de la recta anclada:** la media de los OSCURO de
inicio y de fin de la sesión en que se midió ese color. Si el OSCURO tiene deriva (RF-CAL-36), no hay
ancla. El acta del 5 y del b declara la sensibilidad al ancla: 5 cuentas de error son del 1,6 al 2,0 %
de R en el 5 y del 2,2 al 2,9 % en el b (P10 §2.3).

*Criterio:* el diario de la campaña contiene K colocaciones aceptadas por cada fila `PATRON` de la cola
(o una marca de saltado), y las cuatro de control por cada sesión. Si falta un patrón de uso AJUSTE o
RE-MEDIDA, su código no se puede calibrar hasta medirlo.

*Por qué K = 3 y no 5:* con s_rep = 2,24 % (A5 del 19-sep), la media de K colocaciones lleva un 1,29 %
con K = 3 y un 1,00 % con K = 5. En códigos de 15 a 24 patrones, el ruido de colocación que queda en la
curva es de ~0,3 %, frente a residuos por tipo del 2,8 al 10 %. Tiempo estimado: **≈ 195 min en 3
sesiones**, frente a ≈ 4 h 26 min a 5 × 4. Modelo y variantes en `PLAN-Captura-Banco-P1-P132.md` §2 y
§4.

**RF-CAL-36 — Deriva de la sesión.** Con la A5 del inicio y la del final de cada sesión, se calcula
para cada patrón de la A5 d = (x̄_fin − x̄_ini)/x̄_ini, y D = media de las tres d.

- **Sesión con deriva** si |D| > 2·s_rep·√(1/K_ini + 1/K_fin)/√3. Con s_rep = 2,24 %, K_ini = 5 y
  K_fin = 3, el umbral es ±1,9 %. Con 3 y 3, ±2,1 %.
- **OSCURO con deriva** si |x̄_fin − x̄_ini| > máx(5 cuentas ; 3·s_osc·√(1/K_ini + 1/K_fin)).
- Una sesión con deriva **no entra en ningún ajuste** sin una nota de Diego. Sus series se conservan.
- *Criterio:* el resumen y el acta muestran, por sesión, D, el umbral y el veredicto.

**RF-CAL-37 — Método por código, fijado en configuración.** Una tabla versionada dentro del APK dice,
por código, el método (grado 1, grado 2, recta anclada o sólo verificar), el patrón de re-medida y si
hay dispensa de RF-CAL-14/15/16. El operador no elige grado (P9-B6). **La tabla sigue a `06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md` (commit `66a7649`, **REFORM**)**, que
es el cálculo con las dos campañas y la A5. Tabla de partida **[corregida tras P10-C2]**:

| Código | Método | Re-medida | Dispensa | Origen |
| :---: | :--- | :--- | :--- | :--- |
| 1 | Grado 1. **Escrito; no se reescribe** (REFORM §3.1 y §6) | P28 | RF-CAL-14/15/16 (acta, línea 11) | Decisión de Diego, 19-sep 11:20 |
| 2 | Recta anclada en el OSCURO. **Escrito; no se reescribe** (REFORM §3.2 y §6) | P25 | **RF-CAL-14/15**: Diego dio la conformidad en el acta (línea 18: P23, P24 y P30 fuera del 10 %; RMS de IV 8,5 %) | Decisión de Diego (opción b) |
| 3, 4, 6 | Grado 1; recta anclada si la libre no pasa `#S` | P123, P11, P86 | Ninguna | PA-16 |
| 5 | Sólo verificar; recta anclada si PA-14 (REFORM §6: "ni con el banco; sólo con dispensa") | P81 | — | PA-14 |
| 8 | **Recta anclada en el OSCURO** (REFORM §3.3 y §6: `c1 = 2.04279088E-01`, `c0 = -1.15490317E+02`). La de grado 1 pasa `#S` por 0,33 en x = 600 | P43 | Ninguna: cumple RF-CAL-14/15/16 | REFORM; C-CAL-15 abierta |
| b | **Recta anclada en el OSCURO** (REFORM §3.4 y §6: `c1 = 3.13845140E-01`, `c0 = -1.77434093E+02`) | P49 | **RF-CAL-14/15, pendiente de Diego** (P39 +15,3 %, P49 −10,2 %, RMS 11,5 %). Sin ella, sólo verificar | REFORM; PA-24 |
| 7, a, c, d | Sólo verificar | — | — | §12.1 |

*Texto de `80a0a97`, retirado:* "8: Grado 1 | Ninguna"; "2: Dispensa: Ninguna" (**era falso**: C-P10-5);
"b: Sólo verificar". Con el 8 y el b anclados, sin serie OSCURO **sólo el código 1** se puede calibrar
(AT-07 y T-S06 corregidos).

*Criterio:* cambiar la tabla exige compilar, y el acta cita su versión. Con la tabla del APK, la
pantalla de calibración no tiene ningún control de grado ni de método.

**RF-CAL-38 — s_rep medida, nunca supuesta.** Todo criterio que dependa de la dispersión entre
colocaciones usa una s_rep **medida**:

- la de la A5 del inicio de la campaña (decisión de Diego: el criterio de la A5, con s ≈ 2,2 %);
- o la de las K colocaciones de la propia serie, si K ≥ 3.

Si no hay ninguna de las dos, **el criterio no se evalúa**: se bloquea, no se supone. *Criterio:* en
el acta no aparece la palabra "supuesta". Hoy aparece en las líneas 14 y 21 del acta del 19-sep
(`Acta.java:159`).

**RF-CAL-39 — Re-medida guiada, con tres comprobaciones.** Tras `#S`, `#G` y `#E` de un código, la app
pide "Coloque <patrón de la tabla RF-CAL-37> y pulse OK". Mide K = 5 colocaciones con M = 4, alternando
`e` y el código en cada disparo. Es conforme si se cumplen las tres comprobaciones:

1. **Colocación válida.** Dos filtros por colocación, antes de contarla **[P10-C1]**:
   - **Patrón presente:** el disparo de asentamiento tiene que caer en [0,8 ; 1,2]·x̄_banco del patrón.
     Si no, la app repite "Coloque P28" sin gastar ningún disparo. El 19-sep a las 12:16:32, el
     asentamiento dio `::592`, a 27 cuentas del oscuro (T4:2081-2083): no había patrón debajo.
   - **Coherencia por par:** con d_i = R_k,i − R_#G(x_e,i), cualquier par con
     |d_i| > máx(3 ; 2 %·R_#G(x_e,i)) invalida la colocación. El 19-sep, el par 6 dio d = +462,3 (se
     apoyó P28 entre `e` y el código); en el segundo intento, el mayor |d_i| fue 4,5 frente a un umbral
     de ~10.

   Una colocación no válida se registra y se repite; nunca cuenta como re-medida, ni conforme ni no
   conforme. *Texto de `80a0a97`, retirado:* "|d̄| ≤ máx(2 ; 3·s_d/√n)". **No invalidaba el intento de
   las 12:17:02:** el par atípico inflaba s_d (154,5) por encima de d̄ (50,3) (C-P10-1). Queda como
   control de sesgo, sin decidir: con los pares del segundo intento, d̄ = −1,44 frente a 2,49, un margen
   de 0,5 que cuadra con el truncado a entero (−0,5 de media). Se fija con un caso real en T-S12.
2. **Reproducción de la campaña:** |x̄_rem − x̄_banco| ≤ 2·s_rep·x̄_banco·√(1/K_rem + 1/K_banco), con la
   s_rep de RF-CAL-38 y la serie del banco de ese patrón con uso RE-MEDIDA (no la de la A5, P10 §2.4).
   Detecta un patrón cambiado o mal apoyado.
3. **Frente al certificado:** |R̄_k − R_cert| ≤ máx(10 % ; 2 unidades) (RF-CAL-14). Es la comprobación
   metrológica que hoy falta (D-19).

**No conforme** (falla la 2 o la 3): se ofrece **una** repetición. Si también falla, se restaura el
código a su estado anterior (RF-CAL-22), se relee y se detiene la secuencia (PA-12). *Criterio:* el
acta lista **todos** los intentos, válidos o no, con su resultado (D-20), y ningún código tiene más de
dos re-medidas válidas.

**RF-CAL-40 — Café y lila.** Se miden con el código del rojo: 4 si son intensos y b si son tipo I
(dato de campo de Diego). **Se verifican y no entran en el ajuste del 4** (PA-15). El acta declara el
residuo de cada uno frente a la curva vigente del 4. *Criterio:* en la cola, los 10 llevan código 4 y
uso VERIFICACIÓN, y el ajuste del 4 sólo contiene patrones rojos.

**RF-CAL-41 — Batería.** La app lee la batería con la orden `9` al empezar cada sesión, en cada cambio
de grupo de color, antes de la Fase B y antes de cada `#S`. La tensión se calcula así (derivación en
§12.8):

- n ≥ 10: V = 10 + (n + 30,09)/90,91;
- n = 5: V entre 10,39 y 10,44 V;
- n = 0: V < 10,34 V;
- n = 177: puede ser el recorte a 99, pero sólo si V > 12,28 V;
- n > 177 es posible, porque sin recorte n no tiene tope (13 V da 242);
- la V es la de la vuelta anterior, **antes del destello** de la lámpara, no en carga.

Umbrales propuestos (PA-17):

- **Aviso** si n < 19 (V < 10,54 V, el 20 % de la escala de la pantalla STONE).
- **Bloqueo de escrituras** (`#S`, `#F`, `#SC`, `#SN`, `#FT`) si n = 0 o si `9` no responde, porque un
  corte de alimentación durante una escritura devuelve ese registro a fábrica al arrancar
  (`REVISION-Arquitectura-P9-V3.6.md` §7).
- La medida no se bloquea.

*Criterio:* cada serie del diario lleva la última V leída, y el acta lleva la V mínima y la máxima de
la sesión. **Queda por medir** cuánto cambia la `x` con la tensión (T-C45). Hasta entonces, los umbrales
son de seguridad eléctrica, no metrológicos.

**RF-CAL-42 — Vencimiento.** La calibración vence al año de `#SC` (`Calibracion.vencimiento`,
`Calibracion.java:109-120`). **Vencida, avisa y no bloquea** (decisión de Diego): se puede medir,
verificar y volver a calibrar. *Criterio:* con `#GC,2025-09-18#` y la fecha de hoy 2026-09-19, la
cabecera dice "Calibración vencida (calibrado 2025-09-18, venció 2026-09-18)" y todas las acciones de
medida siguen activas.

**RF-CAL-43 — Lo que se añade al acta** (amplía RF-CAL-25):

- nombre y SHA-256 del ZIP de entrada, y md5 de la cola;
- A5 del inicio y del final, D y veredicto de deriva por sesión (RF-CAL-36);
- s_rep usada y su origen (RF-CAL-38);
- `x` del OSCURO **medida**. Hoy pone "x = 575", el valor por defecto de `Asistente.X_OSCURO`
  (`Asistente.java:218`; acta del 19-sep, líneas 13 y 20);
- batería: V al inicio, V mínima y V antes de cada `#S` (RF-CAL-41);
- **`#V#` leído después de la última escritura**, con marca y máscara (D-18);
- todos los intentos de re-medida (D-20);
- resultado de la persistencia: apagar, encender, `#V#`, `#G` y `#E` (P9-B8);
- "PIN de fábrica: sí/no" (PA-13);
- método y dispensas, sacados de la tabla RF-CAL-37, con su versión.

*Criterio:* el acta no se puede aceptar si falta cualquiera de estos campos. Un dato desconocido se
escribe "no conocido", nunca se deja vacío.

### 12.3 El flujo, en dos fases

Es el del QA (§2.2 y §2.4), con estos cambios:

- **Fase A, medir:** la cola de RF-CAL-35 sustituye a los "patrones mínimos" del QA (§2.3). Se miden
  todos, y con K = 3 caben en ≈ 195 min. **El conflicto 5 desaparece**: la decisión C se cumple
  entera.
- **Fase B, "Calibrar este equipo":** comprobaciones previas; cálculo con la campaña (nunca con medidas
  de sesión, P9-B7); una pantalla de resumen con una tarjeta por código; una casilla de conformidad por
  código; secuencia código a código (`#S`, `#G`, `#E` y re-medida RF-CAL-39); persistencia; `#V#`
  fresco; acta; `#SC` y ZIP. Los requisitos de la app son RF-APP-34 a RF-APP-38, RF-APP-44 y
  RF-APP-46.

### 12.4 Protecciones en conflicto (QA §2.5): propuesta

No se elimina ninguna protección.

| # | Conflicto | Propuesta | ¿Decide Diego? |
| :---: | :--- | :--- | :--- |
| 1 | "Sólo Coloque P28 → OK" frente a P9-B3/B5 | Las 5 colocaciones de la re-medida se quedan, **sin decisiones**: botón a media pantalla, contador "3 de 5" y aviso sonoro. K = 5 da una s_rep propia, y RF-CAL-38 prohíbe suponerla | No: lo exigen P9-B3 y RF-CAL-38 |
| 2 | "Si falla, restaura a fábrica" frente a RF-CAL-22 | Se restaura **al estado anterior**, que en una primera calibración es fábrica. Es lo que hace hoy `restaurar()` | No |
| 3 | Re-medida no conforme | RF-CAL-39: las colocaciones inválidas (comprobación 1) se repiten y se registran. Si falla la 2 o la 3, hay **una** repetición; si vuelve a fallar, se restaura el estado anterior y se detiene. Todo queda en el acta (D-20) | **Sí, PA-12** |
| 4 | RF-CAL-29 (cambiar el PIN) frente a P9-B10 | El flujo no cambia el PIN. El acta anota "PIN de fábrica: sí". RF-CAL-29 queda en suspenso hasta que exista el superadministrador (P9-P6) | **Sí, PA-13** |
| 5 | Patrones mínimos frente a la decisión C | **Resuelto sin subconjunto:** el banco mide todos a K = 3 (RF-CAL-35). No hace falta aprobar la regla de mínimos del QA §2.3 | Sí: aprobar el protocolo (PA-10) |
| 6 | Re-medida con un patrón frente a RF-CAL-18/23 ("cada patrón") | En la Fase B se re-mide un patrón por código (RF-CAL-39). **"Cada patrón" se cumple con el banco de verificación** que va después, con `e` y la curva `#G` en float32. La comprobación 1 de RF-CAL-39 demuestra que eso equivale a medir con el código. El acta lo declara | **Sí, PA-11** |
| 7 | Superadministrador sin implementar | La conformidad por código es un **control de procedimiento**, no una identidad, y el acta lo dice. RF-CAL-26/27 siguen pendientes para producción (P9-P6) | No |

### 12.5 Café y lila: por qué sólo verificar

El detalle y la evidencia están en `PLAN-Captura-Banco-P1-P132.md` §3.9. En resumen:

- ninguno se ha medido;
- la pendiente de la recta por el oscuro cambia con el color, de 0,18 a 0,61 R por cuenta, así que no
  se puede suponer que caigan sobre la del rojo;
- serían 10 puntos frente a 16;
- y la decisión C es por color.

**Se cierra midiendo:** si tras el banco los 10 cumplen RF-CAL-14 frente a la curva del rojo, pueden
entrar en otra calibración.

### 12.6 Códigos 3, 4, 5 y 6: qué cambia

- **3, 4 y 6** cumplen la regla con el catálogo nuevo, y se ajustan en grado 1 (PA-16). **En los tres
  la parte baja está sin medir:** del 3 hay 4 medidos (todos XI, 164-173), del 4 hay 2 y del 6 ninguno.
  La cobertura real la dirá el banco: `Asistente.proponer` repite la regla con lo **medido**
  (`Asistente.java:423-433`).
- **Riesgo con `#S` en el 3:** los verdes de 51 caen, estimados, a unas 84 cuentas del oscuro. Una recta
  libre que corte el cero por encima de x = 600 da R(600) < 0, y `#S` la rechaza (RF-FW-31,
  `calibracion_v36.c:404-422`). Si pasa, recta anclada.
- **El 5** no cumple la regla: recta anclada sólo con PA-14, y cambiando la regla (RF-APP-42).
- **Nueve verdes certificados en 51 exacto** (P65, P79, P80, P94, P95, P109, P110, P124, P125): C-CAL-21.

### 12.7 La re-medida del 19-sep, revisada

Ver §12.0, D-19 y D-20. **No se reabre la calibración de SLV-002**: hechas las cuentas bien, las dos
re-medidas cumplen las comprobaciones 2 y 3 de RF-CAL-39. Lo que sí falta es la persistencia (final de
P9-B8) y un `#V#` que diga `CAL`. Se recomienda hacerlos en el próximo contacto con el equipo (T-C41).

### 12.8 La orden `9`: lo que el firmware devuelve de verdad

**El QA (D-17) y `PROCEDIMIENTO-Calibracion-V3-K42.md:135` dicen que la respuesta a `9` está rota. En
la 3.6.2 lo está sólo en parte, y se puede invertir** (C-CAL-17):

1. `acquireBatteryVoltage` toma 5 muestras del canal `BATERIA` y promedia las 3 mayores. Después,
   V = ADC·0,001·4,3, por el divisor de 10k y 33k (`measurement.c:124-146`).
2. En cada vuelta del bucle de medida, bv = 45,455·(V − 10) − 4,5455. Cada 201 vueltas se recorta a
   [0 ; 99] y se manda a la variable 207 de la pantalla (`gui.c:349-356`). bv es una `static` de
   fichero (`gui.c:19`), así que el recorte dura hasta la vuelta siguiente.
3. Con `9`, en la vuelta siguiente: bv' = 2·bv − 21; si 5 < bv' < 10, bv' = 5; si bv' < 0, bv' = 0;
   n = (int) bv', y la respuesta es `:<n>:` (`gui.c:329-340`). El `nivel[2]` que desbordaba es
   `nivel[6]` desde la V3.6 (`gui.c:268`).
4. Por tanto, n = trunc(90,91·(V − 10) − 30,09): V = 10 + (n + 30,09)/90,91 es el extremo inferior de
   un intervalo de 11 mV. Los valores 6 a 9 no salen nunca. Casos especiales **[corregidos tras P10-C9]**:
   - n = 5: V de 10,39 a 10,44;
   - n = 0 ⇔ V < 10,342 (el recorte a 0 da lo mismo, así que no es una excepción);
   - **n = 177 sólo aparece con V > 12,28 V**, y sólo si la `9` cae en la vuelta siguiente a un recorte a
     99 (`gui.c:352-356`);
   - **n puede pasar de 177**: sin recorte no hay tope (con 13 V, n = 242).

   *Texto de `80a0a97`, retirado:* "n = 177 si coincide con el recorte a 99", sin la condición de V y
   sin decir que n pasa de 177.
4 bis. **La V es la de antes del destello.** Se mide en la vuelta anterior (`gui.c:349-351`), sin la
   carga de la lámpara; bv' no persiste, porque `gui.c:350` lo sobrescribe. No es la tensión en carga.
4 ter. **Mientras se atiende una `9` se pierde todo byte que no empiece por `#`** (`clearBuffer`,
   `uart_module.c:23-26`; `gui.c:343`). Además, cualquier trama que empiece por `9` cuenta como batería
   (`strncmp(...,1)`, `gui.c:329`). La app no envía nada detrás de una `9` hasta tener `:n:` o agotar la
   espera.
5. `9` entra en la rama de medida (`gui.c:296`): **dispara la lámpara**, como cualquier código, y tarda
   lo mismo. Por eso no se envía en mitad de una serie.
6. **Ninguna respuesta `:n:` se ha registrado nunca con la V3.6.** La única petición, del 19-sep a las
   09:52, quedó sin respuesta porque el equipo estaba mudo
   (`07 pruebas/campana_103300/tramas/rtv36_20260919_095234.txt:15-16`). La fórmula sale de leer el
   código: T-C44 la contrasta con un polímetro.
7. **Tres dudas de hardware que el código no resuelve** (P10 §4). Se cierran con T-C44, no reprogramando:
   - que la referencia del ADC valga 4,096 V: `ADREF` elige VREF+ externo (`adcc.c:101-102`), y el valor
     depende del componente;
   - que la resistencia de 10k sea la inferior del divisor: lo dice un comentario
     (`measurement.c:144`), sin cotejar con el esquemático;
   - que el TAD esté dentro de especificación con `ADCLK = 0x00` (Fosc/2) y `ADACQ = 0`
     (`adcc.c:83-86`, `:105-106`).

### 12.9 Sólo app o también firmware

| Requisito | ¿Firmware? | Motivo |
| :--- | :--- | :--- |
| Banco, Calibrar con un botón, re-medida, reanudación, B12, Importar, aviso de desinstalar, APK con versión, `#V#` fresco y vencimiento | **No** | Todo usa órdenes que ya existen en la 3.6.2 |
| Serie y fecha en EEPROM | **No** | Ya están en la 3.6.2: `#SN`/`#GN#` y `#SC`/`#GC#` (commit `6a32ca3`) |
| Códigos 3, 4, 5 y 6 ajustables | **No** | `#S` admite los 12 códigos (`calibracion_v36.c:51`, `:343-350`, `:690-697`). Cambia la regla de cobertura de la app (RF-APP-42) |
| Aviso de batería | **No** | La orden `9` existe y se puede invertir (§12.8). Tiene dos salvedades: dispara una medida y da tres valores ambiguos |
| Café y lila con el código del rojo | **No** | Es una tabla de la cola y de la app (RF-APP-47) |

**Lo que sí exigiría cambiar el firmware** (fuera de alcance salvo que Diego decida otra cosa):

| ID | Cambio | Qué toca | Por qué no se propone ahora |
| :--- | :--- | :--- | :--- |
| F-1 | **Códigos propios para café y lila** | Firmware: 2 ecuaciones más; bloque EEPROM de 12 a 14 juegos, con su CRC; `#G`, `#S`, `#F` y `#E`; y la máscara de `#V#`, que hoy es de 12 bits. **Pantalla STONE:** botones, imágenes y códigos nuevos (`04_Pantalla_STONE/`). **App de campo:** códigos nuevos | Diego mide café y lila con el rojo. Además, es un cambio de contrato (`PROTOCOLO-V3.6.md`) |
| F-2 | Orden de batería limpia (p. ej. `#GB#` → V en mV), sin disparar la lámpara y sin recortes | `gui.c` y `calibracion_v36.c` | `9` basta para avisar. Sería una 3.6.3 |
| F-3 | Cadena de versión propia en `#V#` (C-37) | `calibracion_v36.h:16` | Hoy basta la sonda `#GC#` |
| F-4 | Bajar el límite de 600 de `#S`, para poder ajustar a y c | `calibracion_v36.c:309-319` | Con certificados de 6-10 y `x` a 27-79 cuentas del oscuro, no hay nada que ajustar |

### 12.10 Contradicciones nuevas

| ID | Contradicción | Fuentes | Cómo se cierra |
| :--- | :--- | :--- | :--- |
| **C-CAL-16** | "3, 4, 5 y 6 pasan a ser ajustables" frente a la regla, que deja fuera al 5 (rango del 19 %) | Encargo del 19-sep; `Asistente.java:93-96`; §12.1 | Diego: PA-14 |
| **C-CAL-17** | "La respuesta a `9` está rota" (QA D-17; PROCEDIMIENTO:135) frente al código de la 3.6.2, que la hace invertible salvo tres valores | `gui.c:268,329-356`; `measurement.c:124-146` | Medir: T-C44 |
| **C-CAL-18** | RF-CAL-18 pide comparar con el patrón; la app compara con el propio equipo (D-19) | `Acta.java:141-154`; §5 de este documento | Implementar RF-CAL-39 |
| **C-CAL-19** | El encargo pedía estimar la `x` de P51-P132 con la curva de fábrica invertida; en los 50 medidos, esa estimación falla del −29 % al +50 % | `PLAN-Captura…` §5.5 | La cola usa la recta por el oscuro y marca el origen de cada `x` |
| **C-CAL-20** | Acta aceptada con "DEF mascara 0000" y "Oscuro (x = 575)", cuando el equipo está en `CAL` y el oscuro medido es 565,4 | Acta del 19-sep, líneas 3, 13 y 20; T4 | `#V#` y persistencia en el próximo contacto (T-C41). **El acta no se reescribe**: se anota al lado |
| **C-CAL-21** | Nueve verdes certificados en 51 exacto, en tres tipos | Catálogo P1-P132; xlsx, Hoja1 | Diego, con el certificado; el banco dirá si sus `x` coinciden |
| **C-CAL-22** | La A5 del 19-sep fue "NO CONCLUYENTE" (P22 FUERA) y la propuesta decía "provisional hasta la A5", pero el acta se aceptó | `resumen.txt:187-191`; `PROPUESTA…:535-537`; acta, línea 23 | La A5 del inicio del banco (RF-CAL-35). Si sale conforme, se cierra |

### 12.11 Decisiones para Diego (PA-xx)

Cada una lleva la recomendación de esta revisión. La numeración sigue a la de `SPEC-Registro-Indicador-Interventoria.md` §8 (PA-01 a PA-09, ya usadas) para que ningún ID se repita.

| ID | Pregunta | Recomendación |
| :--- | :--- | :--- |
| **PA-10** | ¿Protocolo del banco con M = 4 en todo, K = 3 en general y K = 5 en todos los OSCURO, la A5 inicial, la re-medida y los códigos 8 y b? | **Sí.** ≈ 195 min frente a ≈ 4 h 26 min a 5 × 4, y con la decisión C entera (RF-CAL-35). Cambia P9-B3 de 1 + 9 a 1 + 4 (P10-C12): Diego lo firma |
| **PA-11** | ¿El "cada patrón" de RF-CAL-18/23 se cumple con el banco de verificación después de escribir, calculando R con `#G`? | **Sí**, con la comprobación 1 de RF-CAL-39 como prueba de la equivalencia |
| **PA-12** | ¿Re-medida no conforme: una repetición y, si falla, restaurar y detener? | **Sí.** Nunca "repetir hasta que pase" (D-20) |
| **PA-13** | ¿El flujo no cambia el PIN, y RF-CAL-29 queda en suspenso hasta que exista el superadministrador? | **Sí** (P9-B10) |
| **PA-14** | ¿Azul intenso (5) con recta anclada en el oscuro, cambiando la regla de cobertura de la app para la recta anclada? Esa regla haría ajustable también el **d** (0-73, 5 niveles; C-P10-7) | **Sí para el 5**: la fábrica lee −55 % y la recta libre no tiene pendiente. El d, sólo verificar (su rango certificado es de 68 a 73). REFORM §6 dice "ni con el banco; sólo con dispensa": es la misma decisión. *Retirado:* "el b, después del banco"; el b pasa a PA-24 |
| **PA-15** | ¿Café y lila entran en el ajuste del 4 o sólo se verifican? | **Sólo verificar**, con el error declarado (§12.5) |
| **PA-16** | ¿Códigos 3, 4 y 6 en grado 1, con la recta anclada si la libre no pasa `#S`? ¿Qué se hace con los nueve verdes de 51? | **Grado 1, con la anclada de reserva.** Los de 51 entran sólo si Diego confirma el certificado |
| **PA-17** | ¿Umbrales de batería: aviso con n < 19 (V < 10,54 V) y bloqueo de escrituras con n = 0 o sin respuesta? | **Sí, provisionales**, hasta medir la `x` frente a V (T-C45) |
| **PA-18** | ¿Una sesión con deriva (RF-CAL-36) queda fuera del ajuste salvo nota de Diego? | **Sí** |
| **PA-19** | ¿Patrón de re-medida del 2: P25 en lugar del P5 del 19-sep? | **Sí.** P5 es un XI desordenado (C-39), y P25 queda a +1,0 % con la curva escrita y la `x` de la campaña de las 10:33 (REFORM:226 da +2,3 % con la `x` combinada de todas las sesiones: otra base) |
| **PA-20** | ¿P22 se queda en la A5 aunque saliera FUERA? | **Sí**, para no perder la continuidad. Si vuelve a salir FUERA en el banco, se cambia por P21 y se anota |
| **PA-21** | ¿Catálogo maestro = `06_Calibracion/patrones_certificados_P1-P132.csv`, copiado al APK en la compilación con su md5? | **Sí.** Cierra C-CAL-06 |
| **PA-22** | ¿Clave de firma propia del proyecto, para poder instalar siempre encima (D-09, P9-P8)? | **Sí.** Sin ella, el aviso de no desinstalar no basta |
| **PA-23** | ¿Se completan en SLV-002 la persistencia y un `#V#` que diga `CAL` en el próximo contacto? | **Sí** (T-C41). No hace falta reescribir nada |
| **PA-24** | ¿Conformidad a RF-CAL-14/15 para la recta anclada del **b** (P39 +15,3 %, P49 −10,2 %, RMS 11,5 %; REFORM §3.4)? El 8 anclado no la necesita | **Decide Diego con las cifras delante.** Sin la conformidad, el b se verifica. Recomendación de P10 §7: escribirlo después del banco, cuando esté medida la deriva del OSCURO |
