# Revisión de arquitectura P12 — V3.6 (app 3.6.13, "Calibrar este equipo"; firmware 3.6.2 sin cambios)

**Nada de lo que revisa este documento se ha ejecutado contra un equipo.** El flujo ha corrido sólo en
la JVM, contra `EquipoSimulado`, y ese simulador **no pasa T-S00** (`TDD-V3.6.md:1201-1206`): se ha
reenviado aquí el registro real T4 y el `#G` coincide en 0 de 25 respuestas (§3). T-C41 y T-C43 siguen
PENDIENTES (`TDD-V3.6.md:1361`, `:1376`). Todo lo que sigue sale de leer el fuente, de ejecutar las
pruebas y de cotejar el APK. Los cálculos propios se marcan como tales.

- **Fecha:** 19-sep-2026, tarde. Revisor adversario. Es la puerta P12: si la 3.6.13 puede calibrar
  SLV-002 (equipo de cliente, firmware 3.6.2) empezando por el 8, el b y el 5, **después del banco**.
- **Repositorio:** `D:\IT\P_RetroVertical_V3.6`, HEAD `5b5bd54`. Delta `37f827d..5b5bd54`: 21 ficheros,
  +2825 y −695 líneas.
- **APK:** `03_App_Movil/RTV-V3.6.13.apk`, md5 `b423285ab5181bfd8152be064bd69b03`, recalculado.
  - `aapt`: `versionCode='3613' versionName='3.6.13'`.
  - `assembleDebug --offline` en un export limpio de `5b5bd54`: `classes.dex` (`9a5d877c…`) y
    `classes2.dex` (`959b9eb3…`) **idénticos** a los del APK.
  - Los tres assets, iguales al fuente. `decisiones.csv`, byte a byte (md5 `8eb5404f…`).
- **Pruebas JVM:** **146 de 146 en verde** con JDK 11.0.24. `FlujoCalibracionTest`: 26.
- **Contra:** P11 (M1-M7 y §5.2), P10, P9; `QA-App-3.6.12.md`; `SPEC-V3.6.md` r1.3;
  `SPEC-Calibracion-V3.6.md` §12; `TDD-V3.6.md` §3 ter; `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`
  (PA-24 y PA-14 = sí).

Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/`; las de `main/java/com/dpi/retrov36/` se
citan sólo con el nombre del fichero. Firmware en `01_Firmware/RetroVertical_V3.6.X/`.

---

## 1. Veredicto

**APTO CON CONDICIONES para calibrar el 8, el b y el 5 en SLV-002 después del banco**, con las nueve
condiciones de §6, y en ese orden: primero el 8 solo, hasta el acta ACEPTADA; después el b y el 5.

Por qué no es NO APTO:

- Los siete arreglos de P11 están hechos en el código. Seis tienen una prueba de extremo a extremo que
  sólo usa acciones del operador (§2). El séptimo (M7) está cerrado en la lógica, no en el teléfono.
- Los cinco P1 de la QA 3.6.12 que se declaran cerrados lo están (§2.2).
- La lógica del flujo ya no vive en la actividad: `CalibrarActivity` pinta y llama a
  `FlujoCalibracion`, y las 26 pruebas recorren exactamente esa clase.
- Ninguna secuencia encontrada deja una curva escrita bajo un acta aceptada sin que la verificación
  final la relea (`FlujoCalibracion.java:922-941`), ni graba `#SC` sin ella.

Por qué no es APTO a secas:

1. **El simulador no está validado** (T-S00 falla, §3). Las 26 pruebas valen como pruebas de la lógica,
   no como prueba de fidelidad al equipo.
2. **La capa Android del flujo no ha corrido nunca**: diálogos, hilos, reconexión de la persistencia y
   `Cliente`. La primera ejecución completa será en SLV-002. Por eso el 8 va solo y primero.
3. **Cuatro huecos nuevos** que el operador tiene que cubrir a mano (§5): la persistencia no comprueba
   que el enlace se cayó; la dispensa del b no tiene límite de clase ni de magnitud; el 5 es calibrable
   con un solo patrón medido; y la re-medida del b se juzga con un criterio que Diego no ha visto
   escrito.

---

## 2. ¿Está cerrado lo de P11 y lo de la QA?

Leyenda: **C** cerrado con código y prueba; **Cc** cerrado en código, sin prueba; **P** parcial.

**Sobre la afirmación "solo con acciones del operador".** Se ha leído `FlujoCalibracionTest.java`
entero. Las 26 llaman a `calibrar`, `leerBateria`, `persistencia`, `aceptar`, `rechazar` y
`fabricaDesdeAvanzado`, y leen `tarjetas`, `previas`, `puede*` y `acta()`. **Es cierta, con tres
matices:**

- `el5ConPa14…` llama a `f.plan('5', null)` (`FlujoCalibracionTest.java:695`). Es de sólo lectura y
  la prueba sigue después por `calibrar`, pero no es una acción del operador.
- El `Contexto` (APTO, 3.6.2, `#GN#`) se rellena a mano (`:186-195`). La correspondencia real
  (`CalibrarActivity.java:82-93`) no la prueba nada.
- El operador simulado contesta siempre la opción 0 (`:121-140`). "Parar aquí" y "No repetir" no se
  prueban. `apagarYEncender` no corta el enlace (`:147-150`): la persistencia pasa por construcción.

Las manipulaciones directas del simulador (`sim.curvas.put`, `sim.admin = true`, `:468-470`) son
inyección de fallos externos, no pasos internos de la app. Son legítimas.

### 2.1 P11-M1 a M7

| N.º | Estado | Código | Prueba |
| :--- | :---: | :--- | :--- |
| **M1** restauración no verificada | **C** | `Ops.restaurar` devuelve `ok` sólo si la relectura `#G` coincide (`Ops.java:98-119`). Si no, `restauracionFallida` deja el código sin resolver (`Acta.java:518-527`) y el acta no se acepta (`:632`). En `escribir`, si la restauración falla, `ESCRIBIENDO` se conserva y el siguiente "Calibrar" la resuelve (`FlujoCalibracion.java:741-745`) | `restauracionNoVerificadaNoResuelveYSeReintenta` (`OK_SIN_HACER` en `#F,8#`). **El equipo mudo** (timeout en `#F`) no se prueba; el código lo trata igual (`relectura` nula ⇒ no `ok`) |
| **M2** escribir o restaurar anula la persistencia | **C** | `anularVerificaciones` en `escrito`, `sinEscribir`, `restaurado` y `restauracionFallida` (`Acta.java:438-527`) | `escribirOtroCodigoAnulaLaPersistencia…` |
| **M3** oscuro por código | **C** | El `Plan` lleva su `xOscuro` y su origen (`FlujoCalibracion.java:94-127`, `:294-298`); `dato("oscuro k")` antes del `#S` (`:723-725`); `ESCRIBIENDO` guarda método, oscuro y conformidad (`Acta.java:412-423`). Sin OSCURO de la sesión no se escribe, y ya no se cae al de la campaña (`Anclas.java:74-78`) | `…CadaUnoLlevaSuOscuro` (8 en x = 565, b en x = 571); `corteDuranteSQueEntro…` (método y oscuro tras el corte) |
| **M4** no reescribir; Avanzado no escribe | **C** en "Calibrar"; **Cc** en Avanzado | `motivoNoEscribir` bloquea conforme, restaurado, restauración fallida y re-medida válida pendiente (`Acta.java:354-386`). En Avanzado, `confirmarEscritura` vuelve siempre antes de escribir (`AdminActivity.java:535-539`), y el botón abre "Calibrar" (`:121-122`) | `unCodigoConformeNoSeReescribe`, `dosReMedidasNoConformes…` (`motivoNoEscribir('8')` ≠ null). Avanzado, sin prueba: el código de escritura queda muerto, no borrado |
| **M5** b y 5 sólo con la decisión de Diego | **C** | `TablaCalibracion.tabla(Decisiones)` (`TablaCalibracion.java:92-116`); las decisiones sólo del asset (`CalibrarActivity.java:151-152`; `Decisiones.java:202-230`) | `elBSinPa24NoSeEscribe`, `lasDecisionesDelApkSonPa24YPa14DeDiego` |
| **M6** `#V#` en CAL, máscara y heredados | **C** en la lógica | Al empezar: `#V#`, `#G` y `#E` del 1 y el 2 frente al texto del acta de las 12:23 (`FlujoCalibracion.java:615-648`). En la persistencia y antes de `#SC`: máscara {1, 2} ∪ certificados y `#G` de los heredados (`:875-891`, `:922-936`). Los textos de `TablaCalibracion.java:136-137` son los de `06_Calibracion/SLV-002/campanas/coeficientes_SLV-002_20260919_122047.csv:2-3` y de la relectura real (`07 pruebas/19092026_1210/x/tramas/rtv36_20260919_114644.txt:1676`) | `unHeredadoDevueltoAFabricaSinActaImpideCalibrar`, `unHeredadoTocadoDespuesDeLaPersistencia…`. **T-C41 en el equipo sigue sin hacer** |
| **M7** el flujo corre fuera del equipo del cliente | **P** | T-S07, T-S10 y T-S12 corren en la JVM: `caminoFelizDel8`, `appMatada…`, `dosReMedidas…` | **Contra un simulador que no pasa T-S00** (§3). La actividad, `Base.preguntar`, la reconexión de la persistencia y `Cliente` no han ejecutado el flujo nunca. No hay registro archivado |

### 2.2 QA-3612 declarados cerrados

| ID | Estado | Evidencia |
| :--- | :---: | :--- |
| **01** Aceptar nunca se habilita | **C** | `puedeAceptar` usa `motivoNoAceptableSalvoVerificacionFinal` (`FlujoCalibracion.java:418-421`; `Acta.java:612-614`). La verificación final va dentro de `aceptar` (`FlujoCalibracion.java:922-941`). Prueba: `caminoFelizDel8` (`:286-292`) |
| **02** calibra con NO APTO | **C** | `FlujoCalibracion.java:195-198`. Prueba: `conPruebasNoAptoNoSeCalibra`, con 0 tramas enviadas |
| **03** restauración no verificada | **C** | Como M1 |
| **04** casillas marcadas y reescritura | **C** | Desmarcadas tras "Calibrar" (`CalibrarActivity.java:324-329`); conforme no reescribible; persistencia anulada |
| **05** batería sin salida | **C** | "Leer batería" (`CalibrarActivity.java:66`; `FlujoCalibracion.java:430-433`). Prueba: `bateriaACeroBloqueaYTrasCambiarlaSeSigue` |
| 07 dispensa del operador | **C** | Como M5. Pero ver §5.2: la dispensa no tiene límite |
| 08 Avanzado se salta la tabla | **Cc** | Como M4. Queda "Rechazar acta" en Avanzado, que no restaura (`AdminActivity.java:147`, `:859`) |
| 09 "no entró" no ofrece repetir | **C** | `FlujoCalibracion.java:674-689`. Prueba: `corteDuranteSQueNoEntroOfreceRepetirlo`. La respuesta "No" no se prueba |
| 10 oscuro de otro código | **C** | Como M3 |
| 11 diálogo sin pantalla | **Cc** | `Base.preguntar` devuelve −1 y `onDestroy` libera las esperas (`Base.java:169-171`, `:177-180`, `:201-209`). −1 lleva a "Parado" en la re-medida (`FlujoCalibracion.java:795-797`) |
| 13 caducidad de `#L` | **Cc** | `entrar()` antes de cada `#S`, restauración, persistencia, aceptación y rechazo (`FlujoCalibracion.java:711`, `:754`, `:871`, `:914`, `:967`). El simulador no caduca: T-S15 sigue PENDIENTE |
| 14 cinco entradas de texto | **P** | PIN una vez por conexión (`CalibrarActivity.java:288-297`) y nombre recordado (`:62-63`, `:302-304`). Quedan la nota y el PIN: 2 frente a 1 de RF-APP-34 |
| 15 README y pruebas tras persistencia | No revisado a fondo | Baja |
| 17 Rechazar no restaura | **C** | `FlujoCalibracion.java:961-988`, restauración verificada. Prueba: `rechazarRestauraLoEscrito` |

**No declarados:** QA-3612-06 (§4.2), -12 (RF-CAL-43, sigue abierto) y -16. El -16 está casi cerrado:
la batería se lee antes de `#SC` (`FlujoCalibracion.java:918-921`) y el aviso con n baja se muestra
(`:453-455`). Falta exportar el ZIP al aceptar.

---

## 3. `EquipoSimulado` frente a la 3.6.2

**Método, propio de esta revisión.** Un programa fuera del repositorio crea un `EquipoSimulado` de
fábrica y le reenvía las 106 peticiones `#…#` del registro T4
(`07 pruebas/19092026_1210/x/tramas/rtv36_20260919_114644.txt`), comparando su respuesta con la RX real.
Es el procedimiento de T-S00 (`TDD-V3.6.md:1202-1206`).

| Orden | Iguales | Diferencia |
| :--- | ---: | :--- |
| `#E` | **65/65** | — |
| `#L`, `#Q#`, `#S`, `#V#` | 3/3, 3/3, 1/1, 1/1 | — |
| **`#G`** | **0/25** | **Última cifra de cada coeficiente.** Fábrica: real `#G,8,8.60000084E-08,-2.99026029E-04,4.46572328E-01,…`, simulado `8.60000000E-08,-2.99026000E-04,4.46572329E-01`. Tras `#S,1`: real `2.98471571E-01,-1.62263885E+02`, simulado `2.98471540E-01,-1.62263870E+02` |
| `#GC#`, `#GN#` | 0/1, 1/3 | El simulador no tiene `#GC,NONE#` ni `#SN` |
| `#SN`, `#GT#` | 0/2, 0/2 | No implementadas: `#ERR,FORMATO#` |

**Por qué el `#G` no coincide.** El simulador guarda la fábrica como `double` de la app
(`EquipoSimulado.java:49-53`), no como `float32`. Tras `#S` redondea a `float` con un `strtod` exacto
(`:179-180`), mientras que el `strtod` y el `sprintf("%.8E")` de XC8 se equivocan en 1-3 ulp
(`Ecuacion.java:70-93`, con la medida de `CAMBIOS-V3.6.md`). **Es decir, el simulador es más exacto que
el equipo.**

**Qué reproduce bien, comprobado contra el firmware:**

- **Formatos:** `::R` (`ecuacionesCalibracion.c:56-63`), `:n:` (`gui.c:329-340`), `#E,k,v#`, `#OK#`,
  `#ERR,BLOQUEADO#` sin modo administrador y `#ERR,EEPROM#` con la RAM intacta
  (`calibracion_v36.c:595-605`, `:690-702`).
- **La marca CAL o DEF** y la máscara de 4 cifras hex (`calibracion_v36.c:635-643`).
- **La sustitución de negativos por 0 y de más de 4000 por 0.**
  - En el firmware: `arreglar_dato`, en `ecuacionesCalibracion.c:49-54`, y `calibracion_v36.c:309-315`.
  - En el simulador: `Ecuacion.java:56-68`.
  - El `#E` en x = 500 da 0 en 10 de los 12 códigos, en el equipo real y en el simulador (T4).
- **`#F` vuelve a fábrica** (`calibracion_v36.c:703-715`).

**Qué no reproduce:**

| Hueco | Firmware | ¿Afecta a las 26? |
| :--- | :--- | :--- |
| `#S` fuera de [0 ; 4000] en x de 600 a 4300 ⇒ `#ERR,FORMATO#` | `calibracion_v36.c:404-422`, `:698` | No: la app lo filtra antes (`FlujoCalibracion.java:716`), pero el rechazo del firmware no se prueba |
| 5 PIN fallidos bloquean `#L` hasta apagar | `:644-657` | No. En el equipo, la app lo presentaría como "PIN rechazado" |
| Modo administrador caduca a los 10 min | `:298`, `:337-341` | No, por el `#L` antes de cada escritura. Sin probar (T-S15) |
| Tiempos: `#S` tarda 913 ms y `#G` unos 220 ms (T4:1663, :1676); cada orden de medida adquiere y espera 500 ms (`gui.c:346-347`) | — | No para la lógica. Los timeouts (`Ops.java:14-16`: 2000 y 5000 ms) cubren lo medido |
| EEPROM y RAM son una sola cosa; apagar no recarga nada; ningún registro con CRC malo | `:173-199` | **Sí:** la persistencia no puede fallar en el simulador |
| Máscara por contabilidad (`\|=` en `#S`) y no comparando con fábrica | `:202-223` | No en los casos probados |
| `e` y el código se leen sobre la misma x, sin ruido | — | Los pares siempre son coherentes: el filtro de P10-C1 sólo salta con "patrón ausente" |
| Comparte `Tramas.tramaG`, `Ecuacion.respuestaFloat32` y `Fabrica` con la app | — | **Sí:** el cotejo `#E` de la app contra el `#E` del simulador es tautológico. Lo salva el 65/65 de T4, que es independiente |

**Dictamen del punto 2.** El simulador **no miente en lo que decide el flujo**: respuestas de control,
errores, `#E` y medidas. **Sí difiere del equipo en el `#G`**, y en la dirección cómoda. Todas las
comparaciones del flujo que usan `#G` tienen tolerancia:

- `ULP_S` = 8 tras `#S` (`FlujoCalibracion.java:733`);
- `ULP_G` = 4 contra otra lectura `#G`, que en el equipo repite el mismo texto;
- `ULP_S + ULP_G` en la restauración por `#S` (`Ops.java:115`).

Son holguras mayores que el error medido. El caso de T4 queda a 1 ulp en c1 y en c0 (cálculo propio).
Por eso **las 26 pruebas no quedan invalidadas como pruebas de la lógica**.

**Pero T-S00 falla.** El TDD dice "si falla, no se usa el simulador para nada más" (`:1206`). Es la
contradicción C-P12-1 (§7). Se cierra con T-C41 y T-C43 en el equipo, que son de todos modos las
condiciones 2 y 3.

---

## 4. Las decisiones que se aplican

### 4.1 El código 5 (PA-14): la decisión propia del desarrollador

**Qué hace.** Sin pasos de AJUSTE para el código, el ajuste usa **todos** los patrones del código que
tengan serie elegida, VERIFICACIÓN incluidos (`FlujoCalibracion.java:233-254`). Con la cola actual es el
caso del 5 y de nadie más que sea escribible. Cuentas propias sobre `assets/cola_banco_P1-P132.csv`:

- 5: 12 pasos de VERIFICACIÓN y 1 de RE-MEDIDA;
- 7, a, c y d: sólo VERIFICACIÓN, pero están en `SOLO_VERIFICAR`.

Para el 5 son 13 patrones de tres tipos (IV, IX y XI), con valores de 83 a 102, en la sesión 3 (filas
156-168).

**Juicio: aceptable, con tres límites.**

- **A favor:**
  - La propia cola lo anticipa. Las 13 filas del 5 dicen "uso según la tabla RF-CAL-37 del APK
    (PA-14)" (`cola_banco_P1-P132.csv:156-168`).
  - Diego pidió ajustarlo "como el amarillo". El 8 también se ajusta con todos sus patrones,
    incluido el de re-medida (P9-B7).
  - Sin los de VERIFICACIÓN, el 5 sólo tendría el P81 y el ancla: dos niveles, que no llegan a la
    cobertura (`Asistente.java:88-91`).
- **Límite 1. No queda ningún patrón independiente del 5.** El P81 de la re-medida entra en el ajuste,
  como el P43 en el 8 y el P49 en el b. En el 5 pesa más: no hay otro patrón que lo verifique.
- **Límite 2. Mezcla tres tipos de lámina en una recta.**
  - Sólo lo avisa (`Asistente.java:452-455`, P-06).
  - RF-CAL-14/15 sí bloquean, porque el 5 no tiene dispensa (`FlujoCalibracion.java:325-327`).
  - Es previsible que el 5 salga "no se escribe" si los tipos se separan. **Eso es seguro.**
  - De los 13 x esperados, 10 son estimados, no medidos (cola, columna `origen_x`).
- **Límite 3. El 5 es calibrable con un solo patrón medido.**
  - `BancoCola.calibrable` sólo exige que estén HECHO los pasos de AJUSTE y RE-MEDIDA
    (`BancoCola.java:250-262`), y para el 5 eso es sólo el P81.
  - Con el P81 y otro patrón elegido, más el ancla, hay tres niveles y la cobertura pasa.
  - Da una recta que descansa en dos puntos del mismo grupo. **Condición 6.**

**La cobertura con el ancla.**

- **Es la SPEC, no una invención:** RF-APP-42 dice que "para la recta anclada cuenta el ancla como un
  nivel más" (`SPEC-V3.6.md:865`). Está en `Asistente.java:416-428` y `:440-442`.
- **Se aplica a toda recta anclada** (8, b y el paso a anclada de 3, 4 y 6), no sólo al 5. Eso es lo
  que dice RF-APP-42.
- **La prueba no la comprueba:** `el5ConPa14…LaCoberturaDeRfApp42` no afirma nada de la cobertura
  (`FlujoCalibracionTest.java:691-704`). Con datos perfectos, pasaría igual sin la regla.
- **Consecuencia para el acta:** el rango certificado del 5 es 0-102. Por encima, la curva extrapola.

### 4.2 El b (PA-24) y la re-medida con dispensa

- **Qué hace la app.** Con dispensa, la re-medida se juzga con dos cosas:
  - la reproducción de la campaña (comprobación 2);
  - R medida frente a la curva **escrita** (primera parte de RF-CAL-18).

  La desviación frente al certificado se anota, y si pasa del 10 %, como "INCUMPLE, dispensado"
  (`Remedida3611.java:76-115`; `FlujoCalibracion.java:846-852`).
- **Juicio: es una derivación correcta, no una regla nueva.**
  - La comprobación 3 de RF-CAL-39 **es** RF-CAL-14 (`SPEC-Calibracion-V3.6.md:827`), y PA-24
    dispensa RF-CAL-14.
  - RF-CAL-18 se compone de "R frente a predicha" más RF-CAL-14 (`:391-396`).
  - Así se deshace QA-3612-06 sin tocar el umbral.
- **Pero hay que decirlo claro:** para el b, la re-medida ya no comprueba nada metrológico. La primera
  parte de RF-CAL-18 es "lo mismo que `#E`, pero con señal real" (D-19, `SPEC-Calibracion-V3.6.md:679-684`).
  Lo que queda es la comprobación 2, que detecta un patrón cambiado o mal apoyado.
- **Diego lo tiene que ver escrito** (condición 5). Su decisión habla de escribir, no de cómo se
  re-mide.

---

## 5. Huecos nuevos

### 5.1 La persistencia no comprueba que el equipo se apagó

`apagarYEncender` espera a que el enlace esté conectado (`CalibrarActivity.java:182-196`). Si nunca se
cayó, sale enseguida y devuelve `true`. **Una persistencia sin apagar da "Persistencia OK".**

- **Lo acota el firmware:** `#S` sólo responde `#OK#` tras releer la EEPROM
  (`calibracion_v36.c:128-154`, `:595-599`).
- **Lo que no cubre:** la carga desde EEPROM al arrancar, con su CRC (`:184-198`), que es lo que
  RF-APP-46 quiere probar.
- **El simulador tampoco puede verlo** (§3). Condición 4.

### 5.2 La dispensa del b no tiene límite

- **En el ajuste.** `plan` deja pasar **cualquier** incumplimiento si la fila tiene dispensa
  (`FlujoCalibracion.java:325`). Eso incluye:
  - RF-CAL-16, "la curva nueva no mejora a la de fábrica" (`Asistente.java:339`), que contradiría
    justo el motivo de Diego ("mucho mejor que fábrica");
  - un P39 al +40 %.
- **En la re-medida**, "dispensado" tampoco tiene techo (`Remedida3611.java:106-107`, `:113`).
- **Diego dispensó unas cifras concretas:** P39 +15 %, P49 −10 % y RMS 11,5 %
  (`DECISIONES-Diego-2026-09-19.md:12`; `SPEC-Calibracion-V3.6.md:786`). Condición 5.

### 5.3 Otras

- **Código muerto de escritura en Avanzado.** El cuerpo de `confirmarEscritura`, `conformidadYEscribir`,
  `escribir` y `aceptarActa` (`AdminActivity.java:535-620`, `:682-760`, `:843-857`) sigue compilado
  tras un `if` siempre cierto (`:536`). Hoy es inofensivo. Borrarlo.
- **"Rechazar acta" de Avanzado cierra el acta sin restaurar** (`AdminActivity.java:147`, `:859-870`).
  Es la vía que el flujo arregló (QA-3612-17).
- **El error de PIN bloqueado se presenta como "PIN rechazado"** (`FlujoCalibracion.java:463-465`).
  Tras 5 fallos, la 3.6.2 exige apagar (`calibracion_v36.c:645-647`).
- **La deriva por A5 sigue sin calcularse** (QA-3612-12, T-S21). Sólo la deriva del OSCURO bloquea el
  ancla (`Anclas.java:58-69`). Una sesión con deriva entra en el ajuste. Condición 1.

---

## 6. Los abiertos, y las condiciones

**¿Bloquea alguno de los abiertos?**

| Abierto | ¿Bloquea tras el banco? | Por qué |
| :--- | :---: | :--- |
| T-S00 | No, con las condiciones 2 y 3 | Falla (§3). Afecta a la confianza en el simulador, no a una decisión del flujo |
| T-S01, S02, S04, S05, S17, S22 | No | Banco, importación, alta de serie, protocolo y vencimiento: fuera del flujo, o ya hechos en SLV-002 |
| T-S13 | No | La comprobación existe (`Asistente.java:497-501`, llamada en `:498`). En la anclada, R = 0 en el oscuro por construcción |
| T-S15 | No | `#L` antes de cada escritura (§2.2, 13) |
| T-S21 | **Sí, a mano** | Condición 1 |
| T-C41 | **Sí** | El flujo coteja el 1 y el 2 al empezar, pero no apaga antes. Condición 2 |
| T-C43 | Es lo que se autoriza | Condición 3 |
| RF-CAL-43 incompleto | No | Anexo a mano. Condición 9 |
| C-P11-7 | No | Hay s_rep de la A5 del inicio |
| QA-3610-06, 07 y 15 | No | Tiempo y trazabilidad |

### Condiciones (antes y durante la calibración en SLV-002)

1. **Banco completo y exportado, y A5 revisada a mano.**
   - Diego compara la A5 del inicio y la del final de cada sesión con el ZIP, porque la app no calcula
     la deriva (QA-3612-12).
   - Una sesión con deriva no calibra sus códigos: la 1 lleva el 8, la 2 el b y la 3 el 5.
   - La A5 del inicio decide también si se reabren el 1 y el 2 (P11 §5.2.2).
2. **T-C41 antes del primer `#S`.** Apagar SLV-002, esperar 5 s y encender. El primer "Calibrar" hace
   `#V#`, `#G` y `#E` del 1 y el 2 antes de escribir (`FlujoCalibracion.java:615-648`). Si el acta dice
   "T-C41 FALLA", no se calibra y se reabre el acta de las 12:23.
3. **El 8 solo, primero, hasta ACEPTADA.**
   - Es T-C43 y la primera vez que la actividad corre el flujo.
   - Registro de tramas y acta, archivados en `06_Calibracion/SLV-002/`.
   - Ante cualquier conducta no explicada de la app (un diálogo que no aparece, un cuelgue, un texto que
     no cuadra con lo hecho), se para: "Parar aquí" o Rechazar con restaurar.
   - El b y el 5 van en un acta posterior.
4. **Persistencia de verdad.** Quien pulsa OK ve caer la conexión en la app al apagar y volver al
   encender. Si no la vio caer, repite. La app no lo comprueba (§5.1).
5. **El b, sólo con lo que Diego dispensó.**
   - Antes de marcar la casilla, Diego lee los "Incumple" de la tarjeta.
   - Sólo pueden ser de RF-CAL-14/15 y del orden de lo que decidió: P39 ≈ +15 %, P49 ≈ −10 %, RMS ≈ 11,5 %.
   - Si aparece RF-CAL-16 u otro patrón, no se escribe y vuelve a Diego.
   - La nota de la conformidad dice que la re-medida del b sólo comprueba la reproducción y la curva
     escrita, no el certificado (§4.2).
6. **El 5, con sus 13 pasos HECHO.**
   - No basta el P81. Antes de escribir, se lee "patrones 5" en la tarjeta: los 13, con su n.
   - La nota de la conformidad declara el rango certificado (0-102) y la mezcla de tipos IV, IX y XI.
   - Si el 5 sale "incumple … sin dispensa", se acepta como resultado: sigue de fábrica y el acta lo
     dice.
7. **El 8, con la nota de C-CAL-15** (certificado de los tipo I) en la conformidad (P11 §5.2.3).
8. **Si se rechaza, con "Restaurar" marcado.** Si una restauración sale "NO VERIFICADA", no se sale del
   flujo hasta resolverla con "Continuar". No se usa "Rechazar acta" de Avanzado.
9. **Anexo RF-CAL-43 a mano:** SHA-256 del ZIP, A5 y deriva por sesión, "PIN de fábrica sí/no" y estado
   de los 12 códigos.

### Para la 3.6.14 (no bloquean)

- Simulador:
  - guardar la fábrica y lo escrito en `float32`, con el error de impresión de XC8;
  - `#SN`, `#GT#` y `#GC,NONE#`;
  - bloqueo de PIN, caducidad del modo administrador y EEPROM separada de la RAM;
  - **pasar T-S00** con T4 y archivarlo.
- La persistencia exige ver caer el enlace.
- La dispensa, limitada a los criterios que nombra la fila.
- El 5 calibrable sólo con todos sus pasos HECHO.
- Pruebas de "Parar aquí", "No repetir" y de la cobertura con ancla.
- Borrar el código muerto de Avanzado y su "Rechazar acta".
- Exportar el ZIP al aceptar.

---

## 7. Contradicciones abiertas

| ID | Contradicción | Fuentes | Cierra |
| :--- | :--- | :--- | :--- |
| C-P12-1 | "Si T-S00 falla, no se usa el simulador" frente a 26 pruebas sobre un simulador que la falla en `#G` (0/25) | `TDD-V3.6.md:1206`; §3 aquí | T-C41 y T-C43 en el equipo; T-S00 con el simulador corregido |
| C-P12-2 | P11-M7 pedía que el flujo corriera fuera del equipo del cliente; lo ha hecho la lógica, no la app | P11 §5.1 (M7); §2.1 aquí | Condición 3, o un equipo propio con la 3.6.2 |
| C-P12-3 | La dispensa de PA-24 nombra cifras; la app dispensa cualquier incumplimiento del b | `DECISIONES-Diego-2026-09-19.md:12`; `FlujoCalibracion.java:325` | Condición 5; 3.6.14 |
| C-P12-4 | El nombre de la prueba del 5 promete la cobertura de RF-APP-42 y no la comprueba | `FlujoCalibracionTest.java:691-704` | 3.6.14 |
| QA-3612-06 | Sigue abierta en el documento de QA. La app la resuelve por derivación (§4.2) | `QA-App-3.6.12.md:180`; `Remedida3611.java:76-115` | Condición 5, con Diego |

---

## 8. Cómo se ha comprobado

**Leído aquí, línea a línea:**

- `FlujoCalibracion`, `EquipoSimulado`, `FlujoCalibracionTest`, `Ops`, `TablaCalibracion`,
  `Decisiones`, `Remedida3611` y `decisiones.csv`, completos;
- `Acta`: `Codigo`, `motivoNoEscribir`, las transiciones y `motivoNoAceptable`;
- `CalibrarActivity`, de la construcción a las acciones;
- `Asistente.proponer` y la cobertura;
- `Anclas.oscuro` y `sRep`;
- el diff de `AdminActivity` y de `Base`;
- `BancoCola.calibrable`;
- en el firmware: `calibracion_v36.c` completo, la medida y la batería de `gui.c`, y
  `ecuacionesCalibracion.c`.

**Un subagente, con un encargo de volcado.**

- Exportó `5b5bd54` a `D:\tmp\p12` con `git archive`.
- Compiló y ejecutó las 146 pruebas.
- Hizo `assembleDebug` y comparó los `.dex` y los assets con el APK.
- `aapt` y `cmp` de `decisiones.csv`.

Sus cifras se han contrastado con el `README` de la app (13 clases, 146) y con el md5 recalculado aquí.

**Propio:**

- La reproducción de T-S00: un programa en el scratchpad, compilado contra las clases del export,
  **fuera del repositorio**.
- La comprobación de que los heredados de la tabla son los del CSV de coeficientes y los de la
  relectura real.
- El recuento de pasos por código en la cola.

**Lo que no se ha hecho:**

- no se ha medido nada;
- no se ha ejecutado la app en un teléfono;
- no se ha probado la reconexión de la persistencia;
- no se ha cotejado el certificado de los tipo I.
