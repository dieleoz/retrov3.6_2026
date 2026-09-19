# QA de la app 3.6.10 (corte A: medir el banco)

**Veredicto: APTO CON AVISOS para medir el banco hoy. No hay un bloqueante del banco entero, pero sí uno
parcial en la sesión 2:** el filtro de patrón presente puede rechazar colocaciones buenas de **lila**
(los 5 pasos, si su pendiente no es la del rojo) y de parte del **naranja intenso** (15 pasos con la x
sacada de la curva de fábrica invertida). Ante ese rechazo la app no ofrece "medir igualmente": el paso
no se puede completar dentro de "Medir el banco". Hay un rodeo, que se da en §1. La sesión 1 (blanco y
amarillo) no corre ese riesgo.

**Nada de esto se ha probado contra el equipo ni en el simulador.** T-S01, la condición para que Diego
empiece el banco (`REVISION-Arquitectura-P10-V3.6.md:320`), sigue **PENDIENTE** (`TDD-V3.6.md:1208`).
`BancoActivity` no tiene ni una prueba automática. La sesión de hoy hace de T-C42 (`TDD-V3.6.md:1369`).

- Revisado: commit `ec068d8`, APK `03_App_Movil/RTV-V3.6.10.apk`, md5 `83c591c3428b14ccdc79852922f32bdb`.
- Fecha: 19-sep-2026. Método: pruebas JVM, revisión estática del recorrido y cálculo con los datos del 19-sep.
- Sin cambios de código.

---

## 1. Qué tiene que hacer Diego hoy (avisos operativos)

| # | Aviso | Por qué (defecto) |
| :---: | :--- | :--- |
| 1 | **Antes de actualizar, exportar el ZIP desde la 3.6.9.** Instalar la 3.6.10 **encima** y no desinstalar | Si Android rechaza la actualización, la única salida es desinstalar, y eso borra la campaña (§4) |
| 2 | **Bloquear la rotación de la pantalla. No pulsar Atrás ni salir de "Medir el banco" mientras se mide una serie** | QA-3610-04: previsible cierre de la app y serie duplicada |
| 3 | Si sale "¿Está el patrón?" con el patrón bien colocado, **no insistir con "Ya está colocado"**: el rechazo se repite igual. Pulsar "Dejar el paso" y después "Saltar este patrón". Al final, medirlo en **Campaña**, en modo lista, con la K × M de la cola (3 × 4, o 5 × 4 en P81 y en las RE-MEDIDA). Anotar en la nota "paso N del banco" | QA-3610-01 y QA-3610-02 |
| 4 | **Saltar sólo lo imprescindible.** Al final, la app ofrece únicamente el primer patrón saltado. Si ese tampoco se puede medir, los demás saltados no vuelven a salir | QA-3610-03 |
| 5 | En los **22 pasos cercanos al oscuro** (verdes con cert. ≤ 97, P39, y todos los verde y azul tipo I), mirar que el patrón está debajo: ahí el filtro no detecta su ausencia | QA-3610-05 |
| 6 | Tras exportar, comprobar que la línea "Copia:" no dice "SIN COPIA" | QA-3610-11 |
| 7 | Si se cambia la batería o se apaga el equipo, esperar los 10 min de calentamiento. La app no los pide | Observación O-3 |
| 8 | Si se corta el Bluetooth: volver a la pantalla inicial, reconectar y pasar las Pruebas. El paso se repite entero, sin perder lo ya medido | §3, escenario 5 |
| 9 | Contar con **más de 3 h**. El plan estima unos 195 min sin repeticiones. Con las repeticiones y las notas que previsiblemente pedirá la app, **la sesión 3 (verde y azul) no cabrá hoy** | QA-3610-06 y QA-3610-07 |

---

## 2. Pruebas JVM

| Árbol | Compilación | Resultado |
| :--- | :--- | :--- |
| `ec068d8` limpio (exportado con `git archive` fuera del repositorio) | `./gradlew assembleDebug compileDebugUnitTestJavaWithJavac --offline` | **OK, 100 tests** (10 clases), 0,32 s |
| Árbol de trabajo del repositorio | Igual | OK, 100 tests |

- Lanzadas como indica `03_App_Movil/RetroV36/README.md:36-44`: Gradle compila y JUnit se ejecuta a mano.
- Recuento por clase: Calculo 31, Campana 15, Asistente 12, Version3610 11, Version367 7, CoherenciaReal 7,
  Version368 6, Receptor 5, Version369 5, Fabrica 1.
- **El árbol de trabajo no es `ec068d8`.** Mientras se hacía esta revisión había trabajo de otro agente sin
  commit: `Acta.java`, `Base.java` y `Campanas.java` modificados, y `Anclas.java`, `Remedida3611.java` y
  `TablaCalibracion.java` nuevos. Por eso la prueba de referencia es la del árbol limpio. Las líneas que
  cita este documento se han comprobado contra `ec068d8`.
- **Cobertura del corte A:** `Version3610Test` prueba la cola, el md5, los saltos, la reanudación, la
  batería, café y lila, P10-C1, P10-C8 y la importación. **Nada prueba `BancoActivity`**: ni el recorrido
  ni los diálogos, los hilos o el ciclo de vida.

---

## 3. Recorrido de Diego, simulado sobre el código

| # | Escenario | Qué hace el código | Resultado |
| :---: | :--- | :--- | :--- |
| 1 | Arranque | Versión en la cabecera de cada pantalla (`Base.java:54-57`). Aviso de no desinstalar si hay series sin exportar (`ConexionActivity.java:67`, `:117-126`) | Correcto |
| 2 | Aviso de desinstalar | Cuenta las series posteriores al último `EXPORTA` de cada diario (`Campana.java:321-323`, `:886-889`; `Campanas.java:199-219`) | Correcto. Sólo protege lo que se ha exportado: lo medido después de la última exportación vive sólo en el teléfono |
| 3 | Importar la campaña previa | "Importar ZIP" arriba, junto a "Medir el banco" (`CampanaActivity.java:73-77`). Con diario, `importarDiario` (`CampanaActivity.java:736-739`). No duplica lo que ya estaba (`ImportadorCampana.java:87-90`, `:152-158`). Comprueba el equipo antes de escribir (`:75-80`) | Correcto si el teléfono ya tiene la campaña ("N ya estaban"). Ver QA-3610-08, -09 y -10 |
| 4 | Banco paso a paso | Cola con md5 permitido (`BancoCola.java:452-453`, `:545-549`); el asset es el blob `9ddb7882…` (comprobado). K, M y asentamiento por fila (`:566-578`). Control sin "Saltar" (`:508-510`). Estado de cada paso en el diario (`Campana.java:335-340`) | Correcto. Total de la cola: **2575 `e`** (Σ K·(M + 1)) y **2060 disparos al diario** |
| 5 | Corte de Bluetooth a mitad de una colocación | `pedir` lanza `IOException` si cae el enlace (`Cliente.java:138-140`). La serie se cierra como REPETIR, no aceptada (`BancoActivity.java:268-275`, `:305-313`). El paso no se anota y se repite entero | **Sin pérdida ni duplicado**: los disparos ya escritos quedan en una serie no aceptada. Coste: se repiten las colocaciones buenas del paso, y hay que reconectar y pasar las Pruebas, porque reconectar pone la versión a "sin detectar" (`Sesion.java:116`, `:125`; `BancoActivity.java:209-211`) |
| 6 | App cerrada y reabierta | Cada evento se escribe y se vuelca al momento (`Campana.java:288-294`). Al volver, `siguiente()` da el primer paso sin estado (`BancoCola.java:609-614`) | **Reanuda en el paso exacto.** Un paso a medias deja una serie sin veredicto, que no cuenta como aceptada (`Campana.java:426-441`), y se repite con una serie nueva: sin duplicar la aceptada ni perder disparos. Se hace `flush` pero no `fsync`: un apagón del teléfono podría perder la última línea (riesgo bajo) |
| 7 | Batería baja | `9` sólo en los pasos BATERIA (`BancoActivity.java:169-203`). Aviso con n < 19 y bloqueo de escrituras con n = 0 o sin respuesta (`Bateria.java:746-767`). La medida nunca se bloquea | Correcto. Si el equipo se apaga a mitad de una serie, es el escenario 5 |
| 8 | Saltar un patrón | Sólo PATRON (`BancoCola.java:508-510`). Queda SALTADO y se ofrece al final (`:615-619`) | **Defecto QA-3610-03** |
| 9 | Pausa | Paso sin acción: OK y sigue (`BancoActivity.java:160-162`). Salir de la pantalla y volver reanuda en el mismo paso | Correcto. No pide calentamiento al volver (O-3) |
| 10 | Exportar con copia en Download | Un ZIP con diario, `campana.csv`, `resumen.txt`, actas, pruebas y tramas (`Campanas.java:298-345`). Copia por MediaStore en Android 10+ y en carpeta pública en 7-9 (`:160-188`). Huellas en el diario (`:281-296`) | Correcto. QA-3610-11 |

### 3.1 Caso límite: el diario a miles de filas

Cifras escaladas desde el ZIP real de las 12:27 (`06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip`):
675 disparos ocupan 54,6 KB de diario (unos 81 B por disparo) y 164,7 KB de `campana.csv` (unos 244 B).
El registro de las 11:46 ocupa 125,7 KB para 409 peticiones (unos 307 B cada una). Los 464 KB sin comprimir
quedan en 80,6 KB de ZIP (5,8 : 1).

| Fichero | Banco completo (2060 disparos, unas 2600 peticiones) |
| :--- | :--- |
| Diario | ≈ 170 KB más lo que ya había |
| `campana.csv` | ≈ 500 KB |
| Tramas | ≈ 0,8 MB |
| **ZIP** | **≈ 0,3-0,4 MB** |

**Rendimiento: sin problema.** El diario se lee de una pasada al abrir la campaña (`Campanas.java:89-91`). Cada
veredicto recorre el catálogo por las series: 132 × unas 300, del orden de 10⁴ operaciones
(`Campana.java:453-460`). La exportación corre en el hilo de la interfaz (`BancoActivity.java:414`), pero con
estos tamaños tarda menos de un segundo; es una estimación, sin medir.

---

## 4. Actualización encima de la 3.6.9

| Comprobación | Resultado |
| :--- | :--- |
| `aapt dump badging` | `com.dpi.retrov36`, `versionCode='3610'`, `versionName='3.6.10'`. La 3.6.9: `versionCode='369'`. **3610 > 369: Android acepta la actualización** |
| Firma (`apksigner verify --print-certs`) | Las dos con esquema v2, `CN=Android Debug`, certificado SHA-256 `c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`. **Es la misma firma** |
| El APK sale de `ec068d8` | Compilado el árbol limpio: `classes.dex` `9a5d877c…` y `classes2.dex` `99cbdc70…`, **idénticos** a los del APK. Assets: cola `9ddb7882…` y catálogo `07ab9cd8…` |
| Conserva la campaña | El diario vive en `files/campanas/` (`Campanas.java:69-73`) y actualizar encima no lo toca. El formato añade `PASO` y `BATERIA` sin tocar lo anterior (`Campana.java:890-898`): un diario de la 3.6.9 se lee igual |
| Riesgo | Sólo si la 3.6.9 del teléfono **no** es `RTV-V3.6.9.apk` de este repositorio (md5 `3fbb68f3…`), sino una compilada con otra clave de depuración. Entonces falla con `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, y la salida sería desinstalar. De ahí el aviso 1 |
| RF-APP-41 | `RTV-V3.6.10-3610.apk` existe. Pero **`RTV-V3.6.apk` también, con el mismo md5**, y RF-APP-41 dice "nunca `RTV-V3.6.apk`" (`SPEC-V3.6.md:864`). El README lo sigue pidiendo (`03_App_Movil/RetroV36/README.md:24-25`). QA-3610-14 |

---

## 5. El filtro de patrón presente, con cifras

**Qué hace.** El disparo de asentamiento de cada colocación tiene que caer en [0,8 ; 1,2]·x_esperada; si no,
"¿Está el patrón?" (`Colocacion.java:663-674`, llamado desde `BancoActivity.java:237`). La x esperada es la de
la columna `x_esperada` de la cola (`BancoCola.java:572`, `:578`). **La columna `origen_x` no se lee**: el
mismo ±20 % vale para una x medida que para una estimada.

**Qué pedía el diseño:**

- P10-C1 define el filtro sobre **x̄_banco**, la x **medida** en el banco, para la **re-medida del corte B**
  (`REVISION-Arquitectura-P10-V3.6.md:88`, `:337`). En el corte A, para P51-P132, esa x todavía no existe.
- El plan de captura dice que, si la app usa la x esperada antes de medir, la tolerancia sea **±30 % en las
  filas (o) y (r), y sin aviso en las (f)** (`06_Calibracion/PLAN-Captura-Banco-P1-P132.md:542-544`).
- **La app aplica ±20 % a todas.**

### 5.1 Cifras por origen de la x esperada (133 pasos de patrón)

Datos: `06_Calibracion/SLV-002/simulacion_antes_ahora_propuesta_SLV-002_20260919.csv` (columnas `x`,
`x_fabrica_invertida`, `x_esperada`, `banda_x`) y la cola `9ddb7882`.

| Origen | Pasos | Error comprobable del método | `banda_x` de la simulación / x esperada | Riesgo de rechazar una colocación buena |
| :--- | ---: | :--- | :--- | :--- |
| Medida el 19-sep | 51 | Es la x medida. Deriva vista: P22 a −5,6 % en la A5 | — | **Bajo** |
| (o) Recta por el oscuro | 57 | Dejando fuera cada patrón medido, la recta de su color lo predice con x_medida/x_pred entre **0,912 (P3) y 1,098 (P23)**. Ninguno de los 31 intensos sale de [0,8 ; 1,2]. El rojo se apoya en sólo 2 puntos, y según cuál se use, P60 cambia de 924 a 971 y P53 de 2036 a 2230 | 4-14 % | **Bajo** (rojo, medio) |
| (r) Café y lila con la recta del rojo | 10 | **Sin ningún dato propio.** Si su pendiente fuera la de otro color intenso, x_real/x_esperada sería: café **0,72-0,89**; lila **0,53-0,76** (con blanco 0,73-0,76, azul 0,70-0,74, amarillo 0,66-0,70, verde 0,53-0,59) | 6-12 % | **Lila: alto. Los 5 pasos quedan fuera con cualquier pendiente que no sea la del rojo.** Café: medio (P83, P113 y P98 bajan de 0,8 con la del verde o la del amarillo) |
| (f) Fábrica invertida (naranja intenso) | 15 | Sobre los 30 intensos medidos, la fábrica invertida deja **12 fuera de [0,8 ; 1,2]**, con x_medida/x_fábrica de **0,71 (P18) a 1,47 (P11)**. En el naranja tipo I (código d), 0,82-0,91: dentro, pero con el más bajo (P32b, 0,815) a 1,5 % del borde | **40-54 %** | **Medio-alto: entre 0 y unos 6 de los 15**, según se parezca el naranja intenso al naranja tipo I o a la media de los intensos |

**Respuesta a la pregunta del encargo: sí, puede rechazar colocaciones buenas y bloquear a Diego**, en lila
casi con seguridad si su pendiente no es la del rojo y en parte del naranja. **No en la sesión 1**: blanco y
amarillo tienen la x medida o la de la recta, con error comprobado de ±10 % como mucho.

### 5.2 Por qué el rechazo bloquea

- "Ya está colocado" vuelve a hacer el asentamiento con la misma x esperada (`BancoActivity.java:280-281`). Si
  el patrón está bien puesto y la estimación es mala, **rechaza siempre**.
- "Dejar el paso" cierra la serie, pero **no anota el paso** (`BancoActivity.java:282-287`): la pantalla vuelve
  al mismo paso.
- No hay opción de medir igualmente con una nota. La única salida es "Saltar", y al final el saltado vuelve a
  salir con el mismo rechazo (QA-3610-03).

### 5.3 El caso contrario: el filtro no ve la ausencia cerca del oscuro

En **22 pasos** la ventana [0,8 ; 1,2]·x esperada contiene el oscuro (565, y 592 del caso de P10): P39, P65,
P64, P124, P125, P79, P80, P78, P109, P110, P94, P95, P93, P108, P40, P45, P42, P32a, P46, P47, P48 y P36. En ellos,
una colocación sin patrón pasa el filtro y se mide como buena (QA-3610-05).

---

## 6. Defectos

Severidad y prioridad según ISTQB. Severidad es el impacto en los datos o en el trabajo de Diego. Prioridad
es la urgencia: **P1**, antes de medir la sesión 2; **P2**, en el corte B; **P3**, cuando convenga.

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :--- | :---: |
| **QA-3610-01** | El filtro de patrón presente usa la x **estimada** con ±20 % en todas las filas. Contradice el plan (±30 % en (o) y (r), sin aviso en (f)) y extiende P10-C1 fuera de su alcance (x̄_banco, re-medida) | `Colocacion.java:657-674`; `BancoCola.java:572`, `:578` (no lee `origen_x`); `PLAN-Captura…:542-544`; `REVISION-P10:88`. Cifras en §5.1 | **Alta** | **P1** |
| **QA-3610-02** | Un rechazo del filtro no tiene salida dentro del paso: "Ya está colocado" repite el mismo rechazo y "Dejar el paso" deja el paso pendiente. No hay "medir con nota" | `BancoActivity.java:277-289` | **Alta** | **P1** |
| **QA-3610-03** | Con todo lo demás hecho, sólo se ofrece el **primer** saltado. Volver a saltarlo lo anota SALTADO otra vez y `siguiente()` lo devuelve de nuevo: los demás saltados no vuelven a salir. El test sólo comprueba que sale el primero | `BancoCola.java:609-621`; `BancoActivity.java:132-139`; `Version3610Test.java:117-120` | Media | **P1** |
| **QA-3610-04** | Girar el teléfono o pulsar Atrás durante una serie: el hilo sigue midiendo y después muestra diálogos (`AlertDialog.show()`) sobre una actividad destruida. Previsible `BadTokenException`, es decir, cierre de la app. Además, la actividad nueva nace con `ocupado = false` y deja pulsar OK: una segunda serie del mismo paso queda en cola detrás de la primera | `BancoActivity.java:34`, `:111-113`, `:218`, `:267-302`; `AndroidManifest.xml:40` (sin `configChanges` ni orientación fija); `Base.java:158-168` | **Alta** | **P1** (rodeo: aviso 2) |
| QA-3610-05 | El filtro no detecta la ausencia del patrón en 22 pasos, porque su ventana contiene el oscuro | §5.3; `Colocacion.java:667-668` | Media | P2 |
| QA-3610-06 | Muchos veredictos DUDOSOS, cada uno con diálogo y nota tecleada. Simulación con la lógica de `orden` y `parecido`, las x esperadas como valor real y las elegidas del 19-sep: **21 de 133 sin ruido y 33 con s_rep 2,24 %/√K**. Sobre todo azul (≈ 9 de 19), amarillo (≈ 8 de 28) y verde (≈ 6 de 21) | `Veredicto.java:239-309`; `BancoActivity.java:334-335`, `:360-375` | Media (tiempo) | P2 |
| QA-3610-07 | REPETIR por reproducibilidad con un 3 % fijo, que no se puede cambiar en el banco. Con la s_rep de la A5 del 19-sep (1,47 %, 2,70 % y 2,56 %; `resumen.txt:187-191` del ZIP de las 12:27), la probabilidad de REPETIR por azar con K = 3 es del **1,6 % (σ = 1,47 %), 16,6 % (σ = 2,24 %) y 29 % (σ = 2,7 %)**; con K = 5, del 0,2 %, 12,7 % y 29 %. Cálculo: χ² con K − 1 grados de libertad | `Veredicto.java:71`, `:150`; `BancoActivity.java:335` | Media (tiempo) | P2 |
| QA-3610-08 | El importador por diario, que es el camino del ZIP, **no valida el patrón contra el catálogo**, y un `REASIGNA` a un patrón desconocido lanza la excepción **después** de haber escrito series (importación a medias). El test de P10-C8 sólo prueba el camino de `campana.csv` | `ImportadorCampana.java:67-119`; `Campana.java:407-411`, `:825-832`; `Version3610Test.java:273-295` | Media | P2 |
| QA-3610-09 | La importación no trae `PASO`, `BATERIA` ni `EXPORTA`. Juntar sesiones por ZIP en **otro** teléfono reinicia el banco en el paso 1 | `ImportadorCampana.java:86-117` | Media (sólo si cambia de teléfono) | P2 |
| QA-3610-10 | Las `ELIGE` del 19-sep fijan la serie elegida de P1-P50 y del OSCURO: las series del banco no pasan a ser las elegidas, y el ajuste del corte B usaría las del 19-sep (1 × 9, otro día) salvo que se elijan a mano | `Campana.java:426-441`; `ImportadorCampana.java:106-117` | Media (corte B) | P2 |
| QA-3610-11 | El paso EXPORTAR se anota HECHO **antes** de exportar, y un fallo de la copia en Download sólo sale en el texto, sin alerta | `BancoActivity.java:409-411`, `:420`; `Campanas.java:289-293` | Baja | P3 |
| QA-3610-12 | Con la campaña cerrada, OK y Saltar se deshabilitan sin decir por qué | `BancoActivity.java:95-97`, `:110-113` | Baja | P2 |
| QA-3610-13 | Si el asentamiento no tiene respuesta, el aviso dice "Coloque P…: x = NaN": confunde un equipo mudo con un patrón ausente | `Colocacion.java:667-671`; `LecturaX.java:58-59` | Baja | P3 |
| QA-3610-14 | Se sigue entregando `RTV-V3.6.apk`, con el mismo md5 que la 3.6.10, contra RF-APP-41 | `SPEC-V3.6.md:864`; `03_App_Movil/RetroV36/README.md:24-25` | Baja | P3 |
| QA-3610-15 | El código de cada fila (RF-APP-47) sólo queda en el texto de la nota del veredicto. En la serie queda `e`, que es el código de lectura. El corte B tendrá que sacarlo del `PASO` o del catálogo | `BancoActivity.java:251-252`, `:352` | Baja | P2 |

**Observaciones que no son defectos:**

- **O-1.** El proceso se apartó de lo previsto: la 3.6.10 se entrega sin T-S01 (`TDD-V3.6.md:1208`), que era
  la condición para empezar el banco (`REVISION-P10:320`).
- **O-2.** La exportación corre en el hilo de la interfaz. Con los tamaños de §3.1 no es un problema.
- **O-3.** La cola sólo tiene calentamiento al principio (fila 1), y OSCURO y A5 sólo al inicio y al final de
  cada sesión. Un cambio de batería a media sesión, como el del 19-sep hacia las 11:40, deja sin control la
  deriva del tramo intermedio.

---

## 7. Los defectos D-01..D-20 frente al corte A

El corte A sólo tenía que cerrar los que afectan a medir (`REVISION-P10:320-331`). Los demás son del corte B.

| Defecto | Estado en la 3.6.10 | Evidencia |
| :--- | :--- | :--- |
| D-07 "Importar" escondido | **Cerrado** | `CampanaActivity.java:73-77`; "Nueva" y "Cerrar" en Avanzado, al final (`:121-124`) |
| D-09 Desinstalar borra la campaña | **Cerrado**, con el límite del escenario 2 | `ConexionActivity.java:117-126`; `Campanas.java:160-188` |
| D-10 Mismo nombre de APK | **Parcial** | `RTV-V3.6.10-3610.apk` existe y la versión se ve en la cabecera (`Base.java:54-57`), pero `RTV-V3.6.apk` sigue (QA-3610-14) |
| D-15 3 × 3 por defecto | **Cerrado en el banco** (K × M de la cola, M = 4 ≥ `N_MIN_DESCOLGADOS`); **abierto en Campaña** | `BancoCola.java:566-577`; `Veredicto.java:40`; `CampanaActivity.java:80-83` |
| D-17 Batería | **Cerrado en lo posible** (se decodifica n según §12.8) | `Bateria.java:703-767` |
| D-01..D-06, D-08, D-11..D-14, D-16, D-18..D-20 | Fuera del corte A (Fase B, acta, identidad, administrador) | `REVISION-P10:332-345` |

---

## 8. Resumen para quien decida

- **Se puede medir hoy**, con los avisos de §1.
- **Antes de la sesión 2 (P1)** conviene corregir QA-3610-01 a -04:
  - tolerancia por `origen_x`: ±30 % en (o) y (r), y sin filtro en (f), como pide el plan;
  - "medir igualmente con nota" ante un rechazo;
  - recorrer todos los saltados, no sólo el primero;
  - fijar la orientación, o que el hilo no toque una actividad destruida.
- **La sesión 1 (blanco y amarillo) no depende de esas correcciones.**
