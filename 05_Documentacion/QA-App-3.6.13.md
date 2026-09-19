# QA de la app 3.6.13 ("Calibrar este equipo", arreglo de P11 y de QA-App-3.6.12)

**Veredicto: APTO CON AVISOS para calibrar SLV-002 en campo tras medir el banco. No hay bloqueante.**
Los cinco defectos P1 de la 3.6.12 están cerrados y el recorrido 8 + b + 5 termina en un acta ACEPTADA contra
el equipo simulado. Quedan tres defectos nuevos de severidad Alta, todos P2 y con un aviso de uso que los rodea
(§1). También queda una contradicción que tiene que cerrar Diego antes de escribir el b (C-3613-1).

**Nada de esto se ha probado contra el equipo ni en Android.** Lo probado es la lógica (`FlujoCalibracion`) en
la JVM contra `EquipoSimulado`. `CalibrarActivity`, `Cliente`, la reconexión real de la persistencia y el acta
en disco (`Campanas`) no los recorre ninguna prueba. **T-C41 y T-C43 en SLV-002 siguen pendientes.** Además,
**ninguna trama de campo muestra `#V#` después de escribir el 1 y el 2**: todas las respuestas a `#V#` de `07 pruebas/` son
`DEF,0000` y anteriores a `#S,1` (`07 pruebas/19092026_1201/x/tramas/rtv36_20260919_114644.txt:18`, `:1660`). Que SLV-002 conteste
`CAL,0003` sale del fuente (`calibracion_v36.c:202-222`, `:636-639`), no de una medida.

- Revisado: `5b5bd54`, `RTV-V3.6.13.apk`, md5 `b423285ab5181bfd8152be064bd69b03` (igual a `RTV-V3.6.13-3613.apk`).
- Fecha: 19-sep-2026. Método: ISTQB. Pruebas JVM en un export limpio (`git archive`), recorrido estático por
  casos de aceptación y **4 pruebas de ruptura propias**, fuera del repositorio (§4.2). **Sin cambios de código.**
- Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Escalas de severidad y
  prioridad de `QA-App-3.6.12.md:21-24`.

---

## 1. Avisos de uso para la sesión de campo

| # | Aviso | Por qué |
| :---: | :--- | :--- |
| 1 | **Usar la 3.6.13 sólo con SLV-002.** En otro equipo el b y el 5 salen escribibles con las dispensas de SLV-002 | QA-3613-03 |
| 2 | **Antes de marcar el b**, Diego confirma por escrito cómo se juzga su re-medida. **Antes de marcar el 5**, confirma que el ajuste use sus 12 patrones, P81 incluido. Si no, calibrar primero el 8 | C-3613-1, C-3613-2 |
| 3 | **Tras un corte, no pulsar "Rechazar"**: pulsar antes "Continuar la calibración a medias" hasta que no quede ningún `#S` sin resolver | QA-3613-01 |
| 4 | Si la persistencia responde "Reconectado, pero PIN rechazado" o "Falta el PIN del equipo", **salir de la pantalla y volver a entrar**. No teclear otro PIN: el firmware se bloquea tras 5 fallos | QA-3613-02 |
| 5 | Rechazar desde "Calibrar", no desde Avanzado (Avanzado no restaura). Rechazar también cualquier acta en curso que venga de la 3.6.12 | QA-3613-04, QA-3613-05 |
| 6 | No cerrar la campaña antes de calibrar. **Exportar el ZIP después de aceptar** (el flujo no lo exporta) | QA-3613-06; QA-3612-16 |
| 7 | Si T-C41 falla al pulsar "Calibrar" (`#V#` distinto de `CAL` con los bits 1 y 2, o `#G,1`/`#G,2` distintos del acta de las 12:23), **parar**: no se escribe nada y hay que avisar a Diego | §7, R-1 |
| 8 | Grabar el registro de tramas de la sesión entera: es la primera ejecución real del flujo (P11-M7 sólo se ha cumplido en la JVM) | §2.3 |

---

## 2. Pruebas JVM

### 2.1 Resultado

| Árbol | Compilación | Resultado |
| :--- | :--- | :--- |
| `5b5bd54`, exportado con `git archive` al scratchpad, más `ecuacionesCalibracion.c` y `06_Calibracion/SLV-002/campanas/` | JDK 11.0.24, `compileDebugUnitTestJavaWithJavac assembleDebug --offline`, y JUnit 4.13.2 a mano (`03_App_Movil/RetroV36/README.md:36-44`) | **OK, 146 tests** (13 clases), 0,54 s |

- **El recuento coincide con el del desarrollador:** 120 de la 3.6.12 más 26 de `FlujoCalibracionTest`. Contado
  también por `@Test` en el fuente: 12 + 31 + 15 + 7 + 1 + 26 + 5 + 11 + 8 + 12 + 7 + 6 + 5 = 146.
- **`FlujoCalibracionTest` y `EquipoSimulado` están de verdad en el export**: son parte de `5b5bd54`, y sus
  clases compiladas aparecen en `debugUnitTest/classes` (`EquipoSimulado`, `$Falla`, `$Inyeccion`;
  `FlujoCalibracionTest`, `$Almacen`, `$AppMatada`, `$Operador`). Las 26 pruebas corren y pasan.
- **Ajuste del export:** `android.overridePathCheck=true` en `gradle.properties` y `local.properties`, sólo en la
  copia. La primera vez se pegó la línea al final de la anterior (`enableJetifier=trueandroid...`) y Gradle
  falló. Corregido en la copia, sin ningún efecto en el árbol.

### 2.2 El APK sale del commit

| APK | `classes.dex` | `classes2.dex` | Assets |
| :--- | :--- | :--- | :--- |
| `RTV-V3.6.13.apk` entregado | `9a5d877c…` | `959b9eb3…` | cola `9ddb7882…`, catálogo `07ab9cd8…`, `decisiones.csv` `8eb5404f…` |
| `assembleDebug` del export de `5b5bd54` | `9a5d877c…` | `959b9eb3…` | Idénticos |

### 2.3 Cobertura, y lo que no cubre

- **Lo que sí cambia frente a la 3.6.12:** la lógica ya no está en la actividad. `FlujoCalibracionTest` la
  recorre sólo con los botones (`calibrar`, `leerBateria`, `persistencia`, `aceptar`, `rechazar`) y con
  `fabricaDesdeAvanzado` (`FlujoCalibracionTest.java:31-37`). La prueba que no vio QA-3612-01 ya no llama a
  mano a la verificación final: `caminoFelizDel8` comprueba `puedeAceptar()` antes y después de la persistencia
  (`:286-290`).
- **Sin prueba:**
  - `CalibrarActivity`: estado de los botones, PIN, casillas e hilos. Por ejemplo, `:265` usa `puedeAceptar()`,
    pero eso sólo se ve leyendo el código.
  - `Cliente` con sus timeouts reales.
  - La reconexión real de la persistencia (`CalibrarActivity.java:175-197`, bucle de 45 s y margen de 2,5 s
    "provisional, sin medir"). En el simulador vuelve al instante (`FlujoCalibracionTest.java:147-150`).
  - El acta en disco. La prueba usa un diario en memoria (`FlujoCalibracionTest.java:58-100`); la app,
    `Campanas.actaEnCurso` (`Campanas.java:235-256`).
- **Lo que el simulador no emula:**
  - la caducidad de 10 min del modo administrador;
  - la máscara calculada por bytes frente a fábrica (`EquipoSimulado.java:182` pone el bit con cualquier `#S`;
    el firmware compara, `calibracion_v36.c:202-222`);
  - el ruido entre disparos (x fija, `EquipoSimulado.java:45`, `:135`);
  - la batería que cambia sola;
  - una respuesta a `#S` perdida con la escritura aplicada (`MUDO` no aplica la trama, `:120-121`).

---

## 3. Cierre de QA-3612-01..17

| ID | Estado | Evidencia |
| :--- | :--- | :--- |
| **QA-3612-01** Aceptar nunca se habilita | **Verificado** | `puedeAceptar()` usa `motivoNoAceptableSalvoVerificacionFinal()` (`FlujoCalibracion.java:418-421`; `Acta.java:612-614`); la verificación final va dentro de `aceptar()` (`FlujoCalibracion.java:922-941`); botón en `CalibrarActivity.java:265`. Test `caminoFelizDel8` (`FlujoCalibracionTest.java:286-292`) |
| **QA-3612-02** Calibra con NO APTO | **Verificado** | La previa exige `apto == TRUE` (`FlujoCalibracion.java:195-198`). Test `conPruebasNoAptoNoSeCalibra`: ninguna trama enviada |
| **QA-3612-03** Restauración no verificada da por buena | **Verificado** | `Ops.restaurar` devuelve `ok` sólo si `#G` relee la anterior (`Ops.java:98-119`); `restauracionFallida` deja el código sin resolver y el acta no aceptable (`Acta.java:518-526`, `:631-633`); un `#S` fallido con restauración sin verificar conserva `ESCRIBIENDO` (`FlujoCalibracion.java:741-745`, `:666-670`, `:690-694`). Test `restauracionNoVerificadaNoResuelveYSeReintenta` |
| **QA-3612-04** Reescribe conformes; persistencia vieja vale | **Verificado** | `motivoNoEscribir` rechaza conforme, restaurado, restauración fallida y re-medida válida pendiente (`Acta.java:364-379`); `anularVerificaciones()` en `ESCRITO`, `SIN_ESCRIBIR`, `RESTAURADO` y `RESTAURA_FALLA`, también al releer el diario (`Acta.java:438-444`, `:255`, `:260`, `:268`, `:284`); casillas desmarcadas (`CalibrarActivity.java:324-329`). Tests `unCodigoConformeNoSeReescribe`, `escribirOtroCodigoAnulaLaPersistencia…` |
| **QA-3612-05** Batería sin salida | **Verificado**, salvo campaña cerrada (QA-3613-06) | Botón "Leer batería (9)" habilitado con enlace (`CalibrarActivity.java:66`, `:263`); la lectura nueva queda en la campaña (`FlujoCalibracion.java:435-439`; `Campana.java:343-353`). Test `bateriaACeroBloqueaYTrasCambiarlaSeSigue` |
| **QA-3612-06** El b no pasa su re-medida | **No verificado: cerrado por interpretación.** Pasa a C-3613-1 | Con PA-24, la re-medida del b deja de juzgarse frente al certificado y pasa a RF-CAL-18 frente a la curva escrita (`FlujoCalibracion.java:846-852`; `Remedida3611.java:83-115`). **Esa regla no está en las palabras de Diego** (`DECISIONES-Diego-2026-09-19.md:12`: "Sí, escribirla") |
| **QA-3612-07** La casilla concede PA-24 | **Verificado** | El b y el 5 sólo son escribibles con una línea válida en `assets/decisiones.csv` (`TablaCalibracion.java:98-112`; `Decisiones.java:76-81`). La casilla ya no decide (`FlujoCalibracion.java:282-286`). Tests `elBSinPa24NoSeEscribe`, `lasDecisionesDelApkSonPa24YPa14DeDiego`. Fuga nueva: QA-3613-03 |
| **QA-3612-08** Avanzado se salta la tabla | **Verificado** | "Escribir" abre "Calibrar" (`AdminActivity.java:119-122`); `confirmarEscritura()` sale antes de escribir (`:535-539`). Los métodos `escribir()`, `remedida()` y `aceptarActa()` de Avanzado quedan sin ningún llamador (QA-3613-09). `#F` invalida el acta leída de disco (`:994-1016`; `FlujoCalibracion.java:996-1011`) |
| QA-3612-09 No entró: no ofrece repetir | **Verificado** | Pregunta "Repetir" con la misma conformidad (`FlujoCalibracion.java:674-688`). Test `corteDuranteSQueNoEntroOfreceRepetirlo` |
| QA-3612-10 Oscuro de otro código | **Verificado** | `Plan` lleva su oscuro (`FlujoCalibracion.java:94-127`, `:294-298`); `"oscuro " + k` en el acta (`:723-725`); sin OSCURO de la sesión no se escribe, tampoco el 3, el 4 y el 6 (`:295-298`); ya no cae al OSCURO de la campaña (`Anclas.java:77-79`). Test `escribirOtroCodigo…CadaUnoLlevaSuOscuro` |
| QA-3612-11 Diálogo sin pantalla | **Verificado en el código**, sin prueba | `preguntar` devuelve −1 si la pantalla no existe (`Base.java:169-171`, `:177-179`); `onDestroy` suelta las esperas (`:200-209`); el flujo para con −1 (`FlujoCalibracion.java:795-797`) |
| QA-3612-12 RF-CAL-43 incompleto | **No verificado** (parcial) | Nuevos: "batería al inicio", "batería mínima (n)", "heredados (T-C41)", "decisiones", "código 5", "código b" y "oscuro k" (`FlujoCalibracion.java:440-451`, `:563-576`). Siguen faltando el SHA-256 del ZIP, la A5 y la deriva por sesión, y el PIN de fábrica (`Acta.java:54-55`; README `:166-167`) |
| QA-3612-13 Modo administrador caduca | **Verificado en el código** | `entrar()` antes de cada `#S`, de cada restauración, de la persistencia, de aceptar y de rechazar (`FlujoCalibracion.java:711`, `:754`, `:871`, `:914`, `:967`). El simulador no emula la caducidad |
| QA-3612-14 5 entradas de texto | **Parcial**: 2 (3 la primera vez) | §5 |
| QA-3612-15 README sobre repetir pruebas | **Verificado** | README `:159` coincide con el código (la reconexión de la persistencia no pasa por `ConexionActivity.java:80-85`) |
| QA-3612-16 Batería antes de `#SC`, aviso n < 19, ZIP | **Parcial** | Batería antes de `#SC` (`FlujoCalibracion.java:918-921`); aviso en pantalla (`:453-455`). **El ZIP no se exporta al aceptar** (`:942-954`) |
| QA-3612-17 Rechazar no restaura | **Parcial** | "Calibrar" ofrece restaurar, verificado (`CalibrarActivity.java:351-358`; `FlujoCalibracion.java:966-983`). **No restaura el código en `ESCRIBIENDO`** (QA-3613-01), y **el "Rechazar" de Avanzado sigue sin restaurar** (QA-3613-04) |

**P11-M1..M7:**

- M1 a M5: cerrados como arriba.
- M6: cerrado en el código (`FlujoCalibracion.java:578-648`, `:875-891`, `:922-936`), **sin medir en SLV-002**.
- M7: cumplido **sólo en la JVM**. La capa Android no ha corrido nunca.

---

## 4. Casos de aceptación contra la 3.6.13

### 4.1 Resultado por caso

Precondición común, la de `QA-App-3.6.12.md:103-105`.

| ID | Caso | Resultado | Evidencia |
| :--- | :--- | :---: | :--- |
| CA-01 | Camino feliz con 8, b y 5 | **PASA** (simulador) | Una pulsación con las tres casillas y las decisiones del APK: un `#S` por código, persistencia OK (`#V# CAL,0293` = 1, 2, 5, 8 y b), `#SC` una vez y acta ACEPTADA (prueba propia `caminoFelizOchoBeCinco`, §4.2). Orden: 5, 8 y b (`Fabrica.CODIGOS`, `FlujoCalibracion.java:481`); un código cada vez (`Acta.java:380-384`). Con el b, C-3613-1; con el 5, C-3613-2 |
| CA-02 | Re-medida no conforme ×2 y restaurar | **PASA** | Aviso "Queda una repetición" y restauración verificada (`FlujoCalibracion.java:786-791`, `:857-859`, `:750-768`). Test `dosReMedidasNoConformes…`: dos válidas, `#F,8#` una vez, el 8 no se reescribe |
| CA-03 | Restauración fallida | **PASA** | Código sin resolver, acta no aceptable y "Continuar" reintenta. Test `restauracionNoVerificadaNoResuelveYSeReintenta`. Si falla siempre, la única salida es rechazar: el acta se cierra con "SIN RESTAURAR" (`:978-981`) y el equipo queda con una curva desconocida en k (O-2) |
| CA-04 | Corte durante `#S` | **PASA**; **FALLA si se rechaza en vez de continuar** | "Entró": se anota con método, oscuro y conformidad (`FlujoCalibracion.java:658-665`); "no entró": ofrece repetir (`:674-688`); ninguna de las dos: restaura verificado (`:690-696`). Tests de corte antes y después. **Rechazar con el corte pendiente deja la curva enviada en el equipo** (QA-3613-01) |
| CA-05 | App matada y reanudada | **PASA** en la lógica | Diario evento a evento (`Acta.java:190-196`) y reanudación (`Campanas.java:235-256`). Test `appMatadaEnLaReMedida…`: un solo `#S,8`. En el teléfono, la reanudación pasa por reconectar y repetir las pruebas (`ConexionActivity.java:80-96`) |
| CA-06 | Batería 0 y cambio | **PASA** | Bloquea antes de cualquier `#S` (`FlujoCalibracion.java:519-523`) y "Leer batería" lo levanta (test). Cambiar la batería corta el enlace: reconectar, pruebas y "Leer batería". **En una campaña cerrada no se levanta** (QA-3613-06) |
| CA-07 | Otra serie | **PASA** | `#GN#` distinto de la campaña: ninguna trama (`FlujoCalibracion.java:202-208`; test `tS03…`). **Pero la tabla y las decisiones no son por serie** (QA-3613-03) |
| CA-08 | Campaña sin OSCURO | **PASA** | Sin OSCURO de la sesión, ningún código escribible, tampoco el 3, el 4 y el 6 en grado 1 (`FlujoCalibracion.java:294-298`; `Anclas.java:74-79`). Test `tS06…`. Más estricto que la SPEC (C-P11-3), y seguro |
| CA-09 | `#SC` que falla | **PASA** | El acta no se cierra y se reintenta (`FlujoCalibracion.java:943-949`). Test `siSCFallaElActaNoSeCierraYSeReintenta` |
| CA-10 | Reescribir el 1 o el 2 | **PASA** | `NO_REESCRIBIR` sin casilla (`TablaCalibracion.java:94-95`); "Calibrar" no los escribe (`FlujoCalibracion.java:485-488`); Avanzado no escribe (`AdminActivity.java:535-539`). Un `#F,1`/`#F,2` hace fallar T-C41 al empezar, en la persistencia y al aceptar (tests `unHeredado…`). Test del camino feliz: 0 tramas `#S,1`/`#S,2` |
| CA-11 | `#F` desde Avanzado con el acta abierta | **PASA** | `fabricaDesdeAvanzado` lee el acta con `Campanas.actaEnCurso` (la de `Sesion` o la de disco) y la invalida aunque `#F` falle (`FlujoCalibracion.java:1004-1009`; `AdminActivity.java:1000-1013`). "Calibrar", persistencia y aceptar quedan bloqueados, y sólo queda rechazar (test `fDesdeAvanzado…`) |

### 4.2 Intentos de romperlo (fuera del repositorio)

Son 4 pruebas en `QaRomperTest.java`, en el scratchpad y sobre el andamiaje de `FlujoCalibracionTest`. **No se
han añadido al árbol.** Resultado: 4 de 4 ejecutadas, con esta salida:

| Secuencia | Qué pasa | Defecto |
| :--- | :--- | :--- |
| 8, b y 5 en una pulsación, persistencia y aceptar | "Códigos escritos y verificados", "Persistencia OK: `#V,3.6,2026-09-19,CAL,0293#`…", "Acta ACEPTADA". Tramas: 6 `#L`, 5 `9` y 1 `#SC` | — |
| Corte tras `#S,8`, reconectar y "Rechazar" con restaurar | "Acta rechazada." (sin más), **0 tramas `#F,8#` y el 8 sigue con la curva nueva** | QA-3613-01 |
| `#L` sin respuesta en la persistencia (enlace que parece vivo tras apagar) | "Reconectado, pero PIN rechazado", después "Reconectado, pero Falta el PIN del equipo" y "No aceptable: falta la persistencia" | QA-3613-02 |
| Decisiones del APK en otro equipo | `fila('b')` = ANCLADA, `fila('5')` = ANCLADA; `heredados("SLV-003")` = {} | QA-3613-03 |

Otras secuencias, recorridas en el código:

- **Respuesta a `#S` perdida con la escritura aplicada.** `#G` coincide, pero sin `#OK#` se restaura una curva
  buena y se informa "fallida" (`FlujoCalibracion.java:736-747`). Es conservador: se pierde tiempo, no calidad (O-3).
- **Patrón equivocado en la re-medida dentro del ±20 %.** Por ejemplo, P37 (x ≈ 992) por P43 (x ≈ 1151): lo
  detecta la reproducción de la campaña (`Remedida3611.java:65-66`, `:101-102`). Da NO CONFORME y, dos veces
  seguidas, restauración. Seguro.
- **Acta en curso de la 3.6.12.** Se retoma, pero no se puede aceptar nunca (QA-3613-05).

---

## 5. Usabilidad

Mismo criterio que `QA-App-3.6.12.md:133`: un toque por botón o casilla, y una entrada de texto por cada valor
tecleado. Batería con n ≥ 19 y sin repeticiones.

| Paso | Toques | Texto | Código |
| :--- | ---: | ---: | :--- |
| Tocar el equipo, pruebas y volver | 2 | | `ConexionActivity.java:80-96` |
| "4. Calibrar este equipo" | 1 | | |
| Casillas 8, b y 5 | 3 | | `CalibrarActivity.java:241-249` |
| Nombre (se recuerda) y nota | | 1 (2 la primera vez) | `CalibrarActivity.java:62-64`, `:302-304` |
| "Calibrar", PIN y "Seguir" (una vez por conexión) | 2 | 1 | `CalibrarActivity.java:288-297` |
| Por código: "Coloque Pxx… OK" y 4 "Levante y apoye" | 3 × 5 = 15 | | `FlujoCalibracion.java:792-794`, `:839-840` |
| "Persistencia" y "Apague… OK" (sin PIN) | 2 | | `CalibrarActivity.java:70`, `:176-177` |
| "Aceptar y grabar fecha" y la confirmación (sin PIN) | 2 | | `CalibrarActivity.java:337-342` |
| Exportar el ZIP en Campaña (el flujo no exporta) | ≥ 3 | | |
| **Total 8, b y 5** | **≈ 30** | **2 (3 la primera vez)** | |
| **Total 8 y b** (comparable con la 3.6.12) | **≈ 24** | **2 (3)** | Antes: ≈ 25 y 5 |

- **El texto baja de 5 a 2:** el PIN se teclea una vez y el nombre se recuerda. RF-APP-34 pide 1; la nota
  obligatoria es la segunda (`FlujoCalibracion.java:495-497`).
- **Los toques casi no bajan:** el núcleo es el mismo, 5 por código. Cada código más suma 6 toques (5 más su
  casilla).
- **Con la batería baja (n < 19)**, un "Entendido" por lectura: 5 toques más con tres códigos
  (`FlujoCalibracion.java:453-455`).
- **Una re-medida NO CONFORME** suma 6 toques: el aviso, "Coloque" y 4 "Levante y apoye".
- **Decisiones técnicas del operador:** ninguna. La casilla ya no concede dispensas; sólo selecciona.

---

## 6. Instalación encima de la 3.6.11 o la 3.6.12

| Comprobación | Resultado |
| :--- | :--- |
| `aapt dump badging` | `versionCode='3613'`, `versionName='3.6.13'`, mayor que 3611 y 3612: **Android acepta instalar encima** |
| Firma (`apksigner verify --print-certs`) | La 3.6.11, la 3.6.12 y la 3.6.13, v2, `CN=Android Debug`, SHA-256 `c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`: **la misma** |
| Conserva campaña y banco | **Sí.** `Campana.java`, `Campanas.java`, `BancoActivity.java`, `BancoCola.java`, la cola y el catálogo no cambian entre `e159fdf` y `5b5bd54`: el único asset nuevo es `decisiones.csv` (`git diff --stat`). El diario vive en `files/campanas/` y se conserva al actualizar |
| Acta en curso | El formato admite los diarios de la 3.6.12 (campos opcionales, `Acta.java:243-245`). **Pero un acta abierta con la 3.6.12 no se puede aceptar** (QA-3613-05) |
| Riesgo que queda | El de siempre: firmada con otra clave de depuración habría que desinstalar, y desinstalar borra la campaña. **Exportar antes de actualizar** |

---

## 7. Defectos nuevos

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :---: | :---: |
| **QA-3613-01** | **"Rechazar" con un corte durante `#S` sin resolver no restaura ese código.** La restauración sólo recorre `acta.codigos()`, y el código en `ESCRIBIENDO` no tiene `Codigo`. El acta se cierra RECHAZADA con el texto "Acta rechazada." y la curva enviada puede quedar en el equipo, bajo la fecha `#SC` del 19-sep. El texto del acta sí dice "#S … SIN RESOLVER" | `FlujoCalibracion.java:971-984`; `Acta.java:412-423`, `:749-751`; diálogo que promete restaurar, `CalibrarActivity.java:351-358`. Reproducido (§4.2) | **Alta** (curva escrita sin acta) | P2 (aviso 3) |
| **QA-3613-02** | **Un `#L` fallido en la persistencia deja el flujo sin PIN y la pantalla no lo vuelve a pedir.** `entrar()` borra el PIN del flujo, pero la pantalla sólo borra el de la sesión si el texto empieza por "PIN rechazado", y la persistencia lo antepone con "Reconectado, pero". Desde ahí, cada acción responde "Falta el PIN del equipo". Disparo verosímil: tras apagar, el enlace viejo parece vivo y `#L` no tiene respuesta, porque los timeouts no lanzan excepción. El mensaje culpa al PIN. Lo mismo pasa con un PIN vacío: `""` no es `null`, y el diálogo no vuelve hasta reconectar | `FlujoCalibracion.java:459-467`, `:871-874`; `CalibrarActivity.java:288`, `:315-317`, `:153`; `CalibrarActivity.java:183-196`. Reproducido (§4.2) | Media | P2 (aviso 4) |
| **QA-3613-03** | **La tabla RF-CAL-37 y las decisiones PA-24 y PA-14 no dependen de la serie.** `decisiones.csv` no lleva equipo, y `TablaCalibracion.tabla(d)` vale para cualquiera. En otro equipo, el b y el 5 saldrían escribibles con la dispensa de SLV-002, y el 1 y el 2 como "escritos el 19-sep", sin heredados que cotejar | `Decisiones.java:56-84`; `TablaCalibracion.java:92-115`, `:133-141`; `assets/decisiones.csv:6-7`. Reproducido (§4.2) | **Alta** | P2 (aviso 1; antes de otro equipo, P1) |
| QA-3613-04 | **El "Rechazar acta" de Avanzado sigue vivo.** No restaura y dice "restaure a fábrica". No archiva el diario con `Campanas.cerrarActaEnDisco`. Avanzado muestra además "Para aceptar: listo" con `motivoNoAceptable()` sin verificaciones | `AdminActivity.java:147`, `:232`, `:859-874`, `:227-229` | Media | P2 (aviso 5) |
| QA-3613-05 | **Un acta en curso de la 3.6.12 no se puede aceptar en la 3.6.13.** Falta el dato obligatorio "código 5", que sólo escribe `abrirActa()`, y un acta retomada no se reabre | `Acta.java:54-55`, `:645-648`; `FlujoCalibracion.java:502-504`, `:572-573` | Baja | P3 (aviso 5) |
| QA-3613-06 | **En una campaña cerrada, "Leer batería" no levanta el bloqueo:** la lectura nueva no se anota y la previa sigue leyendo la última de la campaña | `FlujoCalibracion.java:212-215`, `:437-439`; `Campana.java:351-353` | Baja | P3 (aviso 6) |
| QA-3613-07 | **El ajuste del 5 usa todos sus patrones, VERIFICACIÓN y P81 incluidos, y P81 es también su patrón de re-medida:** la re-medida no es independiente del ajuste. Es una decisión de la app, que el README pide revisar. PA-14 no nombra patrones | `FlujoCalibracion.java:233-254`; `cola_banco_P1-P132.csv` filas 155-164; README `:143-145`; `DECISIONES-Diego-2026-09-19.md:13` | Media | P2 (C-3613-2) |
| QA-3613-08 | **La tarjeta de un código con dispensa dice "decisión de Diego registrada" en la casilla**, pero el acta guarda una sola conformidad para todos los códigos de la pulsación | `FlujoCalibracion.java:385-386`, `:505-513` | Baja | P3 |
| QA-3613-09 | **Código muerto en Avanzado:** `if (propuesta != null \|\| propuesta == null)` y los métodos `escribir()`, `remedida()` y `aceptarActa()`, que siguen escribiendo en `Sesion.acta` y ya no tienen llamador. Si alguien reconecta un botón, vuelve QA-3612-08 | `AdminActivity.java:535-539`, `:682-745`, `:766`, `:843-857` | Baja | P3 |

**Contradicciones abiertas** (no se eligen aquí; se cierran decidiendo o midiendo):

| ID | Contradicción | Fuentes | Cierra |
| :--- | :--- | :--- | :--- |
| **C-3613-1** | **El juicio de la re-medida del b lo decidió la app, no Diego.** PA-24 dice "escribirla"; la 3.6.13 deduce de ahí que la re-medida en P49 deje de compararse con el certificado. En su lugar, reproducción de la campaña más RF-CAL-18 frente a la curva escrita. Con el b, esto último es casi tautológico: el equipo aplica la curva que se acaba de escribir | `DECISIONES-Diego-2026-09-19.md:12`; `Remedida3611.java:83-115`; `TablaCalibracion.java:83-85` | Diego, por escrito, antes de marcar el b |
| C-3613-2 | Patrones del ajuste del 5 (QA-3613-07) | `FlujoCalibracion.java:233-254`; README `:143-145` | Diego |

**Riesgos no medidos** (no son defectos):

- **R-1.** `#V#` = `CAL,0003` en SLV-002 no se ha visto nunca: las capturas de campo son anteriores a las
  escrituras (§ cabecera). Si difiere, T-C41 bloquea la calibración. Es un fallo seguro.
- **R-2.** El margen de 2,5 s tras reconectar y el tiempo que tarda el móvil en detectar que el equipo se apagó
  (`CalibrarActivity.java:183-195`) están sin medir. Son el disparador verosímil de QA-3613-02.

**Observaciones:**

- **O-1.** Las re-medidas del 8 (P43) y del b (P49) usan patrones que también entran en el ajuste (usos
  AJUSTE y RE-MEDIDA, `FlujoCalibracion.java:247`). Viene de la 3.6.12 (`e159fdf`,
  `CalibrarActivity.java:195`).
- **O-2.** Una restauración que falla siempre sólo sale por rechazar. El acta dice "SIN RESTAURAR" y el
  código k queda en un estado desconocido. La siguiente acta no lo coteja: sólo coteja los heredados 1 y 2.
- **O-3.** Una respuesta a `#S` perdida con la trama aplicada se trata como fallo y se restaura (conservador).
- **O-4.** `Version3612Test` se ajustó a la tabla nueva sin debilitarse: el b sin decisión pasa a sólo
  verificar, y hay 4 casos de decisión no válida (`5b5bd54`, diff de `Version3612Test.java`).

---

## 8. Qué falta para APTO sin avisos

1. QA-3613-01: restaurar también el código en `ESCRIBIENDO` al rechazar, con la lógica de `resolverCorte`.
2. QA-3613-02: tratar cualquier fallo de `entrar()` igual (borrar el PIN de sesión y volver a pedirlo), y no
   admitir un PIN vacío.
3. QA-3613-03: decisiones y tabla por serie (una columna `equipo` en `decisiones.csv` y en la tabla).
4. C-3613-1 y C-3613-2 decididas por Diego y registradas en `DECISIONES-Diego-2026-09-19.md`.
5. T-C41 y T-C43 en SLV-002 con el registro archivado: es lo que convierte este APTO en medido.
