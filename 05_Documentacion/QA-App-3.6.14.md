# QA de la app 3.6.14 (rehacer en el banco, arreglo de QA-App-3.6.13)

**Veredicto: APTO CON AVISOS para medir el banco con rehacer y para calibrar el 8 en SLV-002. No hay
bloqueante.** El rehacer hace lo que pidió Diego en el camino normal: la serie queda ANULADA con su motivo, sale del
ajuste, de la A5, de la s_rep y del ancla, y el paso vuelve a la cola. Al intentar romperlo aparecen **dos defectos de
severidad Alta**: una importación incremental pierde la anulación y deja elegida la serie equivocada (QA-3614-01), y
un motivo con salto de línea deja el ZIP sin importar (QA-3614-02). Los dos se rodean con un aviso de uso (§1).
Además, **rehacer no mira el acta**: con un acta en curso o ya aceptada se puede anular una serie que entró en la
curva escrita (QA-3614-03).

**Nada de esto se ha probado en Android ni contra el equipo.** Lo probado es la lógica (`Campana`, `BancoCola`,
`ImportadorCampana`, `FlujoCalibracion`) en la JVM. `BancoActivity` (botones, diálogos, preferencia) no la
recorre ninguna prueba: sus casos se juzgan leyendo el código. T-C41 y T-C43 en SLV-002 siguen pendientes.

- Revisado: `00f667e`, `RTV-V3.6.14.apk`, md5 `ebfcfad87a4ec9db81673f8ce2f338a2` (igual a `RTV-V3.6.14-3614.apk`).
- Fecha: 19-sep-2026. Método: ISTQB. Pruebas JVM en un export limpio (`git archive`), casos de aceptación por
  lectura de código y **9 pruebas de ruptura propias** fuera del repositorio (§3.3). **Sin cambios de código.**
- Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Escalas de severidad y
  prioridad de `QA-App-3.6.12.md:21-24`.

---

## 1. Avisos de uso para la sesión de campo

Siguen vigentes los avisos 2, 7 y 8 de `QA-App-3.6.13.md` §1 (C-3613-1 y C-3613-2, T-C41, registro de tramas).
Los demás se cierran o cambian con §4. Nuevos:

| # | Aviso | Por qué |
| :---: | :--- | :--- |
| A | **El motivo del rehacer, en una sola línea.** No pulsar Intro dentro del motivo | QA-3614-02 |
| B | **Para llevar la campaña a otro teléfono, importar sólo el último ZIP y en una campaña vacía.** No importar un ZIP encima de otro anterior de la misma campaña | QA-3614-01 |
| C | **Rehacer sólo antes de pulsar "Calibrar".** Con un acta en curso o aceptada, no rehacer: primero rechazar el acta o avisar a Diego | QA-3614-03 |
| D | **Tras rehacer y volver a medir, pulsar "Exportar ahora".** La cola no vuelve a pedir el ZIP si la sesión ya se exportó | QA-3614-04 |
| E | **No rehacer un OSCURO ni una A5 de una sesión ya terminada.** Se volvería a medir fuera de su sesión | QA-3614-05 |
| F | Todos los teléfonos que importen ZIP de esta campaña, en la 3.6.14. La 3.6.13 no entiende `ANULA` y rechaza el ZIP entero | §6 |
| G | **Rechazar en "Calibrar" cualquier acta en curso abierta con la 3.6.13 antes de seguir.** La 3.6.14 la aceptaría sin volver a mirar su tabla | QA-3614-09 |
| H | Si "Rechazar" responde "SIN RESTAURAR", **parar y avisar a Diego**: el código puede tener la curva nueva sin acta | QA-3613-01, parcial |

---

## 2. Pruebas JVM y APK

### 2.1 Resultado

| Árbol | Compilación | Resultado |
| :--- | :--- | :--- |
| `00f667e` exportado con `git archive` al scratchpad (repositorio entero) | JDK 11.0.24, `compileDebugUnitTestJavaWithJavac assembleDebug --offline`, JUnit 4.13.2 a mano (`03_App_Movil/RetroV36/README.md:36-46`) | **OK, 169 tests** (15 clases), 1,0 s |

- **El recuento coincide con el del desarrollador.** Por `@Test` en el fuente: Asistente 12, BancoRehacer 5,
  Calculo 31, Campana 15, CoherenciaReal 7, Fabrica 1, FlujoCalibracion 38, Receptor 5, TS00 6, Version3610 11,
  Version3611 8, Version3612 12, Version367 7, Version368 6, Version369 5 = 169.
- **`BancoRehacerTest` tiene 5 pruebas** y las 5 pasan. Cubren: anular la última, un paso intermedio, que la anulada
  no entre en el ajuste, la A5 ni la s_rep, la app matada entre `ANULA` y `PASO`, e importar un ZIP con una anulada
  (`BancoRehacerTest.java:81-192`). **No cubren:** rehacer dos veces, rehacer tras exportar, la ruta "¿Era Pxx? No,
  rehacer", la importación incremental, un motivo de varias líneas ni rehacer con un acta abierta.
- **Ajuste del export:** `android.overridePathCheck=true` en `gradle.properties` (en línea propia: el fichero no
  acaba en salto de línea) y en `local.properties`, sólo en la copia.

### 2.2 El APK sale del commit

| APK | `classes.dex` | `classes2.dex` | Assets |
| :--- | :--- | :--- | :--- |
| `RTV-V3.6.14.apk` entregado | `9a5d877c…` | `e9987fec…` | cola `9ddb7882…`, catálogo `07ab9cd8…`, `decisiones.csv` `ba2c1056…` |
| `assembleDebug` del export de `00f667e` | `9a5d877c…` | `e9987fec…` | Idénticos |

**Los 420 ficheros del APK fuera de `META-INF/` son idénticos byte a byte** (md5 de cada uno). El APK entero difiere
(`b27721b5…` frente a `ebfcfad8…`) sólo por la firma. `classes.dex` es el mismo que en la 3.6.13 (librerías); la
app va en `classes2.dex`. La cola y el catálogo no cambian desde la 3.6.13; `decisiones.csv` sí.

---

## 3. Banco con rehacer: casos de aceptación

### 3.1 Cómo funciona, según el código

- **Dos botones**, bajo OK y Saltar: "Rehacer el paso anterior (…)" y "Rehacer patrón…" (`BancoActivity.java:53-55`).
  Se habilitan con la campaña abierta, sin serie en curso y con algún paso rehacible (`:204-208`).
- **Rehacible** = paso HECHO con serie: PATRON, A5 y OSCURO. CALENTAMIENTO, BATERIA y EXPORTAR no, porque se
  anotan sin serie (`Campana.java:468-486`).
- **Motivo obligatorio.** Vacío: aviso y vuelve a pedirlo (`BancoActivity.java:109-130`).
- **`Campana.rehacer`** escribe dos eventos: `ANULA,id,fecha,motivo` y `PASO,orden,REHACER,,fecha,"rehacer: …"`
  (`Campana.java:454-465`, `:438-448`). La serie no se borra.
- **`pasos()`** trata como REHACER un paso HECHO cuya serie está anulada (`Campana.java:334-344`). Así, si la app
  muere entre los dos eventos, el paso ya vuelve a la cola.
- **`BancoCola.siguiente`** devuelve el primer paso sin estado o en REHACER, en el orden de la cola
  (`BancoCola.java:220-225`).
- **Anulada fuera de todo:** `seriesDe`, `seriesA5` y `elegida` la excluyen (`Campana.java:225-247`, `:513-528`);
  también `Anclas.seriePaso`, que alimenta el oscuro y la s_rep (`Anclas.java:31-35`). `anular` quita el ELIGE
  (`Campana.java:444-446`, `:992-1002`).
- **Calibrar se bloquea** hasta volver a medir: `calibrable` exige HECHO en todos los pasos AJUSTE y RE-MEDIDA del
  código (`BancoCola.java:250-262`; `FlujoCalibracion.java:339-342`), y REHACER no es HECHO.
- **Preferencia** "Confirmar cada patrón antes de pasar al siguiente", en SharedPreferences `rtv/confirmar_patron`,
  desactivada por defecto (`BancoActivity.java:56-61`, `:71-73`). Sólo actúa en pasos PATRON con veredicto OK
  (`:655-694`).

### 3.2 Resultado por caso

Precondición común: SLV-002 conectado, pruebas APTO, campaña abierta y cola `9ddb7882…`. Pasos 1-50 hechos.

| ID | Caso | Pasos | Esperado | Resultado | Evidencia |
| :--- | :--- | :--- | :--- | :---: | :--- |
| CR-01 | Rehacer la última | Medir P44 y P37 (P37 con papel equivocado); "Rehacer el paso anterior"; motivo; "Rehacer" | P37 ANULADA con su motivo, fuera del ajuste; el paso 52 vuelve a ser el siguiente; al medirlo, la nueva queda elegida | **PASA** | `BancoRehacerTest.anularLaUltimaDevuelveElPasoALaCola` (`:81-101`) |
| CR-02 | Rehacer una intermedia | Medir P44, P37 y P34; "Rehacer patrón…"; elegir P44 | P44 vuelve antes que los pendientes; los demás no cambian | **PASA** | `rehacerUnPasoIntermedio` (`:103-122`). Se mide fuera de su lugar: ver CR-10 |
| CR-03 | Rehacer dos veces el mismo | P37 → rehacer → medir → rehacer → medir | Dos anuladas con su motivo; la tercera elegida; paso HECHO | **PASA** | Prueba propia R1: 2 anuladas, elegida S003, paso HECHO, rehacibles = [52] |
| CR-04 | Rehacer tras exportar | Banco hasta EXPORTAR; rehacer P37; medir | El ZIP entregado queda viejo; la app pide exportar otra vez | **FALLA (aviso)** | Prueba propia R2. Tras anular, `seriesSinExportar()` = 0: la anulación no cuenta como pendiente (`Campana.java:329-331`). Tras volver a medir, la cola dice "Banco completo" y no ofrece EXPORTAR, que ya estaba HECHO. **QA-3614-04** |
| CR-05 | App matada entre `ANULA` y `PASO` | Matar tras el `ANULA`; reabrir | Paso en la cola, serie anulada, sin elegida | **PASA** | `reanudarTrasCerrarLaAppAMitadDelRehacer` (`:158-177`) |
| CR-06 | App matada en "¿Era P37? No, rehacer" | Con la preferencia: "No, rehacer", motivo, "Rehacer"; matar tras el `PASO HECHO` y antes del `ANULA` | La serie equivocada no queda aceptada | **FALLA** (ventana corta) | La ruta escribe primero `VEREDICTO OK aceptada` y `PASO HECHO`, y después el `ANULA` (`BancoActivity.java:678-681`). Prueba propia R3: al reabrir, paso HECHO, **serie equivocada elegida y no anulada**, y la cola sigue adelante. **QA-3614-06** |
| CR-07 | La anulada no entra en el ajuste, la A5, la s_rep ni el ancla | Anular una A5-INICIO, un patrón y el OSCURO | s_rep con 2 patrones; `medidasElegidas` y `medias` sin él; sin oscuro de la sesión | **PASA** | `laAnuladaNoEntra…` (`:124-156`); prueba propia R7: `xOscuro` = NaN y "falta el OSCURO de la sesión 1". Sí entra en T-C38 (`Campana.java:846-849`, todas las series); es aceptable, porque mide el primer disparo y no el patrón |
| CR-08a | El ZIP muestra la anulada | Exportar tras rehacer | `campana.csv` con veredicto ANULADA, aceptada 0 y "ANULADA: motivo"; `resumen.txt` con su bloque | **PASA** | `Campana.java:736-742`, `:782-789`; test `:98-100` |
| CR-08b | Importar un ZIP con una anulada en una campaña vacía | Importar el diario | La trae anulada y con su motivo; el paso en REHACER | **PASA**, con dos pegas | Test `:179-192`; prueba propia R5 ("solo zip2"). **La fecha de la anulación se pierde**: se escribe "importada" (`ImportadorCampana.java:132-134`) |
| CR-08c | Importar ZIP sucesivos de la misma campaña | Importar el ZIP de la sesión 1 y después el de la sesión 2, con P37 rehecho entre medias | P37 anulada y la nueva elegida | **FALLA** | Prueba propia R5: "1 ya estaban", aviso "P37: se mantiene la serie elegida por el operador"; en el destino, **elegida la anulada (media 1502, no 994)**, 0 anuladas y paso HECHO. **QA-3614-01** |
| CR-08d | Motivo con salto de línea | Motivo "papel equivocado:⏎pedía P37, puse P40" | Se guarda entero | **FALLA** | Prueba propia R6. El motivo va entre comillas con el salto dentro (`Medida.java:64-65`), pero el diario se lee línea a línea (`Campana.java:899-912`): 2 líneas malas y el motivo cortado. **`importarDiario` rechaza el ZIP entero** ("2 líneas del diario no se entienden") (`ImportadorCampana.java:73-76`). **QA-3614-02** |
| CR-09 | Preferencia de confirmación | Activar la casilla; medir un patrón OK | "¿Era P45?" con cabecera, veredicto, color, tipo y certificado; "Sí" acepta; "No, rehacer" anula | **PASA** en el código, con pegas | `BancoActivity.java:655-694`. Persiste entre sesiones y actualizaciones (SharedPreferences). **No pregunta** en "Aceptar con nota" (DUDOSO/REPETIR), ni en A5 ni en OSCURO. **No queda en el diario ni en el registro** si se confirmó cada patrón (O-5). Motivo vacío en esta ruta: vuelve a "¿Era…?" en lugar de volver a pedir el motivo (`:673-676`) |
| CR-10 | Rehacer un OSCURO o una A5 de otra sesión | En la sesión 3, rehacer el OSCURO de inicio de la sesión 1 | Aviso o bloqueo | **FALLA (aviso)** | Se ofrece (R7: rehacibles = [3]) y la cola lo pone el primero (`BancoCola.java:221-224`). Se mediría horas después y se emparejaría con el OSCURO final de la sesión 1 (`Anclas.java:47-74`). Lo mismo con la A5-INICIO (s_rep). **QA-3614-05** |
| CR-11 | Rehacer con acta en curso o aceptada | Calibrar el 8 (escrito, falta la persistencia); ir al banco y rehacer P43 | Bloqueo, o que el acta no se pueda aceptar | **FALLA** | Ni `Campana.rehacer` ni `BancoActivity` consultan el acta. `aceptar()` no vuelve a mirar la campaña (`FlujoCalibracion.java:1070-1100`); sólo `plan()` pide `calibrable` (`:339`). La re-medida pasa a comparar con otra serie o con ninguna (`:935-938`). El acta guarda los patrones con su n, no los id de serie (`:884`, `:316-327`). **QA-3614-03** |
| CR-12 | Pasos sin serie | `rehacer(1)` (CALENTAMIENTO) o `rehacer(9999)` | Rechazo | No alcanzable desde la pantalla | R8: `Campana` lo acepta y marca REHACER. La pantalla sólo ofrece pasos HECHO con serie (`Campana.java:468-486`). Sin impacto (O-6) |

### 3.3 Pruebas de ruptura (fuera del repositorio)

`QaRehacerRomperTest.java` (8 pruebas) y `QaDiarioRealTest.java` (1), en el scratchpad y sobre las clases del
export. **No se han añadido al árbol.** Resultado: 9 de 9 ejecutadas. Imprimen lo observado sin afirmar nada:

| # | Secuencia | Salida |
| :---: | :--- | :--- |
| R1 | Rehacer dos veces P37 | `anuladas=2 elegida=S003 (esperada S003) paso=HECHO rehacibles=[52]` |
| R2 | Exportar, rehacer y volver a medir | Tras anular: `seriesSinExportar=0`. Tras medir: `siguiente=null (Banco completo, no pide exportar)` |
| R3 | Matar tras `VEREDICTO OK` y `PASO HECHO` de "No, rehacer" | `paso=HECHO elegida=S001 anulada=null` y la cola sigue en el 51 |
| R4 | Matar tras `VEREDICTO OK` | Paso pendiente; elegida la equivocada hasta volver a medir (la nueva la sustituye) |
| R5 | Importar zip1 y luego zip2 | `1 ya estaban. Avisos: P37: se mantiene la serie elegida por el operador`; `elegida P37 = S001 media 1502 anulada=null` |
| R6 | Motivo con salto de línea | `2 lineas malas`, motivo `[papel equivocado:]`, importar: `2 líneas del diario no se entienden: no se importa nada` |
| R7 | Rehacer el OSCURO de inicio | Se ofrece; `oscuro(8)=no calculable: falta el OSCURO de la sesión 1`; `siguiente=3` |
| R8 | `rehacer` de un paso sin serie | Sin error; estado REHACER |
| R9 | Diario real del 19-sep (`campana_SLV-002_20260919_122727.zip`) con la 3.6.14 | `malas=0 series=60 anuladas=0 rehacibles=[] elegidas=51` |

---

## 4. Regresión y cierre de QA-3613-01..09

### 4.1 Regresión de los casos de la 3.6.13

Todos pasan en el export (§2.1).

| Caso 3.6.13 | Prueba en la 3.6.14 | Resultado |
| :--- | :--- | :---: |
| Calibrar el 8 (camino feliz) | `FlujoCalibracionTest.caminoFelizDel8` (`:317-349`): un `#S,8`, ningún `#S,1`/`#S,2`, `#SC` una vez, acta ACEPTADA | **PASA** |
| Restaurar | `dosReMedidasNoConformesRestauranYNoSeAcepta`, `restauracionNoVerificadaNoResuelveYSeReintenta`, `rechazarRestauraSiempre` | **PASA** |
| Corte durante `#S` | `corteDuranteSQueNoEntroYNoSeRepite`, `…QueEntroSeResuelveAlVolver`, `…QueNoEntroOfreceRepetirlo`, y **`rechazarConUnCorteSinResolverRestaura`** (QA-3613-01, `:598-617`: un `#F,8#` y el 8 a fábrica) | **PASA** |
| Batería | `bateriaACeroBloqueaYTrasCambiarlaSeSigue`, `bateriaEnCampanaCerradaTambienLevantaElBloqueo` (QA-3613-06) | **PASA** |
| Otra serie | `tS03EquipoDeOtraSerieNoSeToca` (ninguna trama), `laTablaYLasDecisionesSonDeSlv002` (QA-3613-03: con SLV-003 ninguna casilla) | **PASA** |
| `#F` desde Avanzado | `fDesdeAvanzadoConElActaAbiertaLaInvalida` | **PASA** |

**Interacción nueva con el rehacer:** un paso AJUSTE o RE-MEDIDA en REHACER deja el código "no calibrable" hasta
volver a medirlo (§3.1). Es seguro antes de calibrar; después, no (QA-3614-03).

### 4.2 Cierre de QA-3613-01..09

Un subagente hizo la lectura y yo contrasté sus líneas con el código de `00f667e`. FCT = `FlujoCalibracionTest`.

| ID | Estado | Evidencia | Test |
| :--- | :--- | :--- | :--- |
| **QA-3613-01** Rechazar con `#S` sin resolver | **Parcial** | `rechazar()` restaura el código en ESCRIBIENDO y lo verifica con `#G` (`FlujoCalibracion.java:1134-1145`). **Si esa restauración falla, el acta se cierra RECHAZADA igualmente**, sólo con el texto "SIN RESTAURAR" (`:1142-1144`, `:1159-1161`): el código no queda anotado como `restauracionFallida`, que es lo que sí se hace con los códigos ya escritos (`:1152-1153`). Es el caso de doble fallo (corte más restauración fallida) | `rechazarConUnCorteSinResolverRestaura` (sólo el caso bueno), `rechazarRestauraSiempre` |
| **QA-3613-02** PIN tras un `#L` fallido | **Cerrado** | Cualquier fallo de `#L` borra el PIN del flujo (`FlujoCalibracion.java:583-589`), y la pantalla borra el de la sesión con `necesitaPin()` (`CalibrarActivity.java:345-347`). Un PIN vacío pasa a null (`:321-322`), pero la acción se lanza igual (`:324`) y falla después pidiendo el PIN: es un paso de más, no un bloqueo | `unLSinRespuestaOlvidaElPinParaVolverAPedirlo` (sólo el flujo) |
| **QA-3613-03** Tabla y decisiones por serie | **Cerrado** | Columna `equipo` en `assets/decisiones.csv:8-10`, con lectura por decisión y equipo (`Decisiones.java:117-170`). En otro equipo todo queda en "Sólo verificar", también el b, el 5, el 1 y el 2 (`TablaCalibracion.java:124-133`) | `laTablaYLasDecisionesSonDeSlv002`, `lasDecisionesDelApkSonDeSlv002YSinReglaDelB`, `tS03…` |
| QA-3613-04 "Rechazar" de Avanzado | **Cerrado**, sin test | Los botones de Avanzado sólo abren "Calibrar" (`AdminActivity.java:145-148`); "Para aceptar" usa `motivoNoAceptableSalvoVerificacionFinal()` (`:228-231`) | — (pantalla) |
| QA-3613-05 Acta de la 3.6.12 | **Cerrado**, sin test. Ver QA-3614-09 | `completarDatos()` rellena "código 5" y "decisiones" (`FlujoCalibracion.java:592-603`) desde calibrar, persistencia y aceptar (`:653`, `:1071`) | ninguno con un acta sin "código 5" |
| QA-3613-06 Batería en campaña cerrada | **Cerrado**, con residuo | La lectura nueva del flujo manda sobre la de la campaña (`FlujoCalibracion.java:261`). Sólo vive en memoria: al volver a crear la pantalla con la campaña cerrada, vuelve el bloqueo | `bateriaEnCampanaCerradaTambienLevantaElBloqueo` |
| QA-3613-07 / C-3613-2 Patrones del ajuste del 5 | **Abierto**, sin cambios | Sin pasos AJUSTE, `patronesAjuste` usa todos los del código, P81 incluido (`FlujoCalibracion.java:288-302`). `decisiones.csv:10`: PA-14 sin alcance. El README lo da por abierto (`README.md:149`) | `el5ExigeSus13PasosYDeclaraElRango` (lo afirma, no lo decide) |
| QA-3613-08 Tarjeta y conformidad | **Cerrado** | Una conformidad por código, con su dispensa y su origen (`FlujoCalibracion.java:655-661`) | `elBConPa24DentroDelAlcance` |
| QA-3613-09 Código muerto en Avanzado | **Cerrado**, sin test | Ni `escribir()`, ni `remedida()`, ni `aceptarActa()`, ni la condición siempre cierta aparecen en `AdminActivity.java` (grep: 0) | — |
| C-3613-1 Re-medida del b | **Cerrado como bloqueo**; la decisión sigue siendo de Diego | REMEDIDA-b no está en el CSV (`decisiones.csv:6-7`). Sin ella el b queda en "Sólo verificar" (`TablaCalibracion.java:151-162`) | `elBSinReglaDeReMedidaNoSeEscribe` |
| QA-3612-16 ZIP al aceptar | **Cerrado**, sin test | `exportarTrasAceptar()` tras "Acta ACEPTADA" (`CalibrarActivity.java:348-349`, `:393-401`). **Si falla, sólo deja una nota en el registro**, sin aviso al operador (`:399`) | — |

**Contradicciones con el commit y el README (entregable):**

- **"Rechazar siempre restaura"** (mensaje de `00f667e`; `README.md:140`) no es cierto con doble fallo: el acta se
  cierra RECHAZADA y la curva puede quedar en el equipo (`FlujoCalibracion.java:1142-1144`, `:1159-1161`).
- **C-3614-1. El margen de 3 puntos del alcance de PA-24 lo fijó la app, en el fichero de decisiones de Diego**
  (`decisiones.csv:4-5`, `:9`). Es el mismo patrón que C-3613-1. Hoy no tiene efecto, porque el b no se escribe
  sin REMEDIDA-b, pero Diego debe confirmarlo antes de añadirla.
- Los avisos 3, 4, 5 y 6 de `QA-App-3.6.13.md` §1 quedan superados en el código. El 5 cambia de motivo: ver
  QA-3614-09.

---

## 5. Usabilidad del rehacer

Mismo criterio que `QA-App-3.6.13.md` §5: un toque por botón o elemento, y una entrada de texto por cada valor
tecleado. No se cuenta la nueva medida del paso, que cuesta lo mismo que la primera.

| Acción | Toques | Texto | Código |
| :--- | ---: | ---: | :--- |
| "Rehacer el paso anterior", motivo, "Rehacer" | 2 | 1 | `BancoActivity.java:83-88`, `:109-130` |
| "Rehacer patrón…", elegir en la lista, motivo, "Rehacer" | 3 | 1 | `:90-107` |
| Con la preferencia: "Sí" en cada patrón OK | +1 por patrón (≈ +100 en el banco) | | `:655-664` |
| Con la preferencia: "No, rehacer", motivo, "Rehacer" | 2 | 1 | `:665-687` |
| Exportar otra vez tras rehacer (el flujo no lo pide) | ≥ 1 | | QA-3614-04 |

- **Pocos toques.** Rehacer la última cuesta 2 toques y un texto, y la lista pone la más reciente arriba (`:101`).
- **El texto del botón es largo:** "Rehacer el paso anterior (Paso 52 (PATRON P37), serie S005 P37, x = 1500.0)",
  en una fila compartida con "Rehacer patrón…" (`:55`, `:206-207`). En un móvil ocupará 3-4 líneas. Repite el patrón
  dos veces y el OSCURO sale como "(OSCURO OSCURO)" (`:75-81`).
- **La lista no filtra ni agrupa.** Al final de la sesión 3 tiene ≈ 150 elementos (todos los HECHO con serie). Cada
  uno lleva paso, patrón y x, pero no la hora ni el color.
- **El mensaje del diálogo dice "sale del ajuste"** (`:113-114`), también para un OSCURO o una A5, que no entran en
  el ajuste: de lo que salen es del ancla o de la s_rep.
- **El paso que vuelve no se distingue:** la pantalla marca "(saltado antes)" para un SALTADO, pero nada para uno en
  REHACER (`:190`). El avance sí dice "por rehacer n" (`:180-182`).
- **La confirmación "¿Era Pxx?" enseña lo que hace falta** (patrón, color, tipo, certificado y veredicto). Nombra el
  patrón pedido, no el medido, y deja al operador la comprobación del papel.

---

## 6. Instalación encima de la 3.6.11 o la 3.6.13

| Comprobación | Resultado |
| :--- | :--- |
| `aapt dump badging` | `versionCode='3614'`, `versionName='3.6.14'`; la 3.6.11 es 3611 y la 3.6.13, 3613: **Android acepta instalar encima** |
| Firma (`apksigner verify --print-certs`) | La 3.6.11, la 3.6.13 y la 3.6.14, v2, `CN=Android Debug`, SHA-256 `c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`: **la misma** |
| Banco en curso | **Se conserva.** `Campana.java` no cambia entre `fda734e` (3.6.11) y `5b5bd54` (3.6.13) (`git diff --stat`). La 3.6.14 sólo añade el evento `ANULA` y el estado REHACER, y lee los diarios anteriores igual. El diario real del 19-sep se lee con 0 líneas malas (R9). La cola y el catálogo son los mismos (md5 en §2.2), así que los `PASO` anteriores siguen apuntando a los mismos pasos |
| Rehacer lo medido con la versión anterior | Sí: los `PASO` del diario viejo llenan `historialPasos` (`Campana.java:988-990`) |
| Serie a medias al actualizar | El proceso muere: la serie queda PENDIENTE y no aceptada, y el paso se repite (`BancoActivity.java:316-344`). Igual que antes |
| Hacia atrás | Un ZIP de la 3.6.14 con una anulada **no se importa en la 3.6.13**: `ANULA` es una línea que no entiende, y `importarDiario` es atómico. Aviso F |
| Riesgo que queda | El de siempre: con otra clave de depuración habría que desinstalar, y eso borra la campaña. **Exportar antes de actualizar** |

---

## 7. Defectos nuevos

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :---: | :---: |
| **QA-3614-01** | **Una importación incremental pierde la anulación y deja elegida la serie equivocada.** La serie que ya estaba en el destino se salta ("ya estaban") sin aplicar su `ANULA`; el `PASO` no se pisa si ya es HECHO; y la nueva buena no se elige porque "se mantiene la serie elegida por el operador". En el destino, el ajuste usaría la serie del papel equivocado | `ImportadorCampana.java:110-115`, `:135-139`, `:150-158`. Reproducido (R5) | **Alta** (ajuste con un dato anulado, sin aviso) | P2 (aviso B) |
| **QA-3614-02** | **Un motivo con salto de línea rompe el diario.** El `EditText` del motivo no limita a una línea (`BancoActivity.java:110`, `:667`); el CSV lo guarda entre comillas con el salto dentro, pero el diario se lee línea a línea. En el teléfono, el motivo queda cortado y 2 líneas malas; **el ZIP ya no se importa en ningún sitio**. Afecta también a las otras notas libres del banco (medir igualmente, aceptar con nota), que ya existían; el rehacer añade una más, con una pista que invita a escribir una frase larga | `Medida.java:64-65`; `Campana.java:899-912`; `ImportadorCampana.java:73-76`. Reproducido (R6). Que Intro inserte un salto sale del comportamiento por defecto de `EditText`; no se ha visto en el teléfono | **Alta** (el ZIP es la entrega) | P2 (aviso A) |
| **QA-3614-03** | **Rehacer no mira el acta.** Con un acta en curso, anular una serie del ajuste o de la re-medida cambia el juicio de la re-medida, y `aceptar()` no lo detecta. Con un acta aceptada, la curva escrita queda apoyada en una serie anulada, y el acta no dice qué series usó | `Campana.java:454-465`; `BancoActivity.java:109-130`; `FlujoCalibracion.java:339`, `:884`, `:935-938`, `:1070-1100` | **Alta** | P2 (aviso C) |
| QA-3614-04 | **Tras exportar, rehacer no vuelve a pedir el ZIP.** Una anulación sin nueva medida no cuenta como "sin exportar", y el paso EXPORTAR ya está HECHO | `Campana.java:321-331`; `BancoCola.java:220-233`. Reproducido (R2) | Media | P2 (aviso D) |
| QA-3614-05 | **Se puede rehacer un OSCURO o una A5 de una sesión ya terminada.** La cola lo pone el primero, y se mide fuera de su sesión: el ancla empareja un OSCURO de hoy con uno de hace horas, y la s_rep mezcla sesiones (RF-CAL-36) | `Campana.java:468-486`; `BancoCola.java:221-224`; `Anclas.java:47-74`, `:89-104`. R7 | Media | P2 (aviso E) |
| QA-3614-06 | **"No, rehacer" escribe primero la serie como OK y aceptada, con el paso HECHO, y anula después.** Si la app muere entre los dos, la serie equivocada queda aceptada y elegida sin marca, y el banco sigue. Ventana corta: son escrituras seguidas en el hilo de la interfaz | `BancoActivity.java:678-681`. Reproducido (R3) | Media | P3 |
| QA-3614-07 | **La importación cambia la fecha de la anulación por "importada"** | `ImportadorCampana.java:133` | Baja | P3 |
| QA-3614-09 | **Aceptar no vuelve a mirar la tabla con la que se escribió.** `puedeAceptar()` sólo mira el estado del acta, y `acta.tabla` sólo se muestra, sin compararse con `TablaCalibracion.VERSION`. Un acta en curso de la 3.6.13 con el b o el 5 escritos, que la 3.6.14 ya no dejaría escribir, se puede aceptar. `completarDatos()` le pone "código 5" con la tabla de hoy, no con la de entonces | `FlujoCalibracion.java:541-544`, `:596-599`; `Acta.java:37-38`, `:705-706`; `TablaCalibracion.java:29` | Media | P2 (aviso G) |
| QA-3614-10 | **Si falla la exportación al aceptar, el operador no se entera.** Sólo queda una nota en el registro | `CalibrarActivity.java:393-401` | Baja | P3 |
| QA-3614-08 | **Documentación del diario sin actualizar.** La cabecera de `Campana` no lista `ANULA` ni el estado REHACER. `TDD-V3.6.md` no tiene ningún caso de rehacer | `Campana.java:22-34`; `grep -n rehacer 05_Documentacion/TDD-V3.6.md`, sin resultados | Baja | P3 |

**Observaciones:**

- **O-5.** Si la preferencia de confirmación estaba activa no queda escrito en ningún sitio: ni en la nota de la
  serie, ni en el diario, ni en el registro (`BancoActivity.java:56-61`, `:655-664`).
- **O-6.** `Campana.rehacer` acepta pasos sin serie o fuera de la cola (R8). Desde la pantalla no se puede llegar.
- **O-7.** La anulada sigue en T-C38 (desvío por posición de disparo, `Campana.java:846-849`). Es coherente: ese
  análisis mide el disparo, no el patrón.
- **O-8.** La preferencia sólo pregunta con veredicto OK. Un papel equivocado suele dar orden o reproducibilidad
  fuera y acabar en "Repetir", que no necesita rehacer. El hueco es "Aceptar con nota", que no pregunta.
- **O-9.** Las conformidades se anotan en el acta antes del apagado de T-C41. Si T-C41 falla, quedan anotadas sin
  que se escriba nada (`FlujoCalibracion.java:654-661`, `:773-776`).
- **O-10.** `aceptadoAntes` da por bueno el 8 con cualquier acta ACEPTADA, aunque después se haya hecho un `#F,8`
  desde Avanzado (`Campanas.java:289-304`).
- **O-11.** El firmante de una decisión vale si contiene "diego" (`Decisiones.java:143`). El javadoc de
  `rechazar()` habla todavía de un parámetro `restaurar` que ya no existe (`FlujoCalibracion.java:1119-1121`).

**Riesgo no medido:**

- **R-3.** "Ver caer el enlace" depende de que el hilo lector detecte que el equipo se apagó. Si no lo detecta
  (QA-App-3.6.13 R-2), la persistencia y T-C41 se niegan siempre. Es un fallo seguro, pero bloquea la calibración.
  Ninguna prueba JVM lo cubre: el operador es simulado (`FlujoCalibracionTest.java:169-176`).

---

## 8. Qué falta para APTO sin avisos

1. QA-3614-01: al importar, aplicar `ANULA` también a las series que ya estaban, y elegir la nueva si la elegida del
   destino queda anulada.
2. QA-3614-02: una sola línea en todos los textos libres (`setSingleLine` o reemplazar `\n`), y un lector del diario
   que respete las comillas.
3. QA-3614-03: bloquear el rehacer con un acta en curso, o invalidarla, y avisar si hay un acta aceptada que usó el
   código del paso. Guardar en el acta los id de serie del ajuste y de la re-medida.
4. QA-3614-04 y -05: contar la anulación como pendiente de exportar y volver a ofrecer EXPORTAR; no ofrecer OSCURO
   ni A5 de una sesión cerrada, o avisar.
5. Casos de rehacer en `TDD-V3.6.md` y pruebas para CR-03, CR-04, CR-06, CR-08c y CR-08d.
6. Lo que queda de `QA-App-3.6.13.md` §8: T-C41 y T-C43 en SLV-002 con el registro archivado.
