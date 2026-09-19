# QA de la app 3.6.15 ("Calibrar todo", serie SLV-002-2026, banco 1×4, ZIP incremental)

**Veredicto: APTO CON AVISOS para que Diego calibre SLV-002 en campo de principio a fin. No hay bloqueante en
el camino normal.** El camino feliz funciona en la JVM: instalar encima de la 3.6.11, terminar el banco a 1×4,
rehacer, exportar, "Cambiar serie" y "Calibrar todo" 8 → b → 5 con tres actas aceptadas. Al intentar romperlo
salen **tres defectos de severidad Alta**:

- **QA-3615-01.** Un corte entre el `#SN` y el `RENOMBRA` deja el equipo en SLV-002-2026 y la campaña en
  SLV-002. Desde ese momento la app no calibra y "Cambiar serie" no deja ni avanzar ni volver. Es un bloqueo
  condicional, sin salida sencilla dentro de la app.
- **QA-3615-02.** En otro teléfono, tras renombrar, no se calibra.
- **QA-3615-03.** La importación no trae el tipo de banco.

Los tres se rodean con avisos de uso (§1).

**Nada de esto se ha probado en Android ni contra el equipo.** Lo probado es la lógica en la JVM, con
`EquipoSimulado` y el diario real de las 15:10 (3.6.11). Las pantallas (`CalibrarActivity`, `BancoActivity`,
`AdminActivity`, `CampanaActivity`) se juzgan leyendo el código.

- Revisado: `c7ef3c6` y `RTV-V3.6.15.apk`, md5 `d22727f0aacfed1c11f978dc37c63443`, igual que
  `RTV-V3.6.15-3615.apk`. HEAD ya es `8c8c7de`, que sólo añade ficheros de `06_Calibracion/SLV-002/campanas/`.
- Fecha: 19-sep-2026. Método: ISTQB. Pruebas JVM en un export limpio (`git archive`). Casos de aceptación
  recorridos con **11 pruebas de ruptura propias** fuera del repositorio (§3.3). **Sin cambios de código.**
- Dos subagentes hicieron la lectura para el cierre de QA-3614 (§4) y para la usabilidad (§5). Las
  contradicciones que encontraron se comprobaron en el código o con una prueba.
- Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Escalas de severidad y
  prioridad de `QA-App-3.6.12.md:21-24`.

---

## 1. Avisos de uso para la sesión de campo

Siguen vigentes los avisos 7 y 8 de `QA-App-3.6.13.md` §1: T-C41 y el registro de tramas. Los de
`QA-App-3.6.14.md` §1 quedan cerrados o sustituidos por estos (§4).

| # | Aviso | Por qué |
| :---: | :--- | :--- |
| A | **"Cambiar serie", con el equipo a menos de 1 m y la batería leída en verde, y sin tocar el teléfono hasta que salga "Serie cambiada".** Si sale otra cosa, no seguir: ver el rodeo de QA-3615-01 (§6). La otra opción es calibrar primero con SLV-002 y renombrar al final: así el corte ya no bloquea nada, pero las actas llevarán el nombre SLV-002 | QA-3615-01 |
| B | **En otro teléfono, tras renombrar: primero dar la serie manual `SLV-002-2026` en Campaña, después importar el ZIP de soporte, y sólo después abrir el Banco.** Sin la serie manual, ese teléfono no calibra | QA-3615-02, -03 |
| C | **Importar siempre el ZIP de soporte (`soporte_…zip`), nunca el ligero.** El ligero no trae el diario entero y la app responde "el ZIP no trae campana.csv" | QA-3615-07 |
| D | **Si "Calibrar todo" se corta, reanudar pulsándolo otra vez y leer el acta, no el mensaje.** Al reanudar, la app acepta el acta a medias pero puede contestar "El código 8 ya tiene un acta ACEPTADA: desmárquelo" o "Nada que calibrar". En ese caso no sale ZIP: pulsar "ZIP de soporte" en el Banco | QA-3615-04 |
| E | **Si un código se restaura tras dos re-medidas no conformes, pulsar "Rechazar" antes de seguir.** La app no lo dice | QA-3615-06 |
| F | **Tras aceptar el 8, no rehacer P34, P37, P43, P44 ni el OSCURO de la sesión 1.** Si hay que hacerlo, la app no deja volver a calibrar el 8 | QA-3615-05 |
| G | **Rechazar en "Calibrar" cualquier acta en curso de una versión anterior.** La 3.6.15 no la acepta: exige las mismas reglas | QA-3614-09 |
| H | Todos los teléfonos que importen ZIP de esta campaña, en la 3.6.15. La 3.6.14 no entiende `RENOMBRA` ni `COLA` y rechaza el diario entero | §7 |
| I | Banco: dejar "Modo rápido" marcado (1×4; es el valor por defecto). A5 y OSCURO siguen a K = 5 sin hacer nada | `Protocolo.java:22-27` |

---

## 2. Pruebas JVM y APK

### 2.1 Resultado

| Árbol | Compilación | Resultado |
| :--- | :--- | :--- |
| `c7ef3c6` exportado con `git archive` al scratchpad (repositorio entero) | JDK 11.0.24, `compileDebugUnitTestJavaWithJavac assembleDebug --offline`, JUnit 4.13.2 a mano (`03_App_Movil/RetroV36/README.md:36-46`) | **OK, 198 tests** (16 clases), 1,5 s |

- **El recuento coincide con el del desarrollador.** Por `@Test` en el fuente: Asistente 12, BancoRehacer 5,
  Calculo 31, Campana 15, CoherenciaReal 7, Fabrica 1, FlujoCalibracion 49, Receptor 5, TS00 6, Version3610 11,
  Version3611 8, Version3612 12, Version3615 18, Version367 7, Version368 6, Version369 5 = 198.
- **T-S00 da 132/132**, impreso por `TS00Test`: `#V#` 1, `#GC#` 2, `#GN#` 4, `#GT#` 3, `#G` 38, `#E` 70, `#L` 4,
  `#SN` 3, `#Q#` 4, `#S` 2 y `#SC` 1.
- **Ajuste del export:** `android.overridePathCheck=true` en `gradle.properties` y en `local.properties`, sólo
  en la copia.

### 2.2 El APK sale del commit

| APK | md5 del APK | `classes2.dex` | Assets |
| :--- | :--- | :--- | :--- |
| `RTV-V3.6.15.apk` entregado | `d22727f0…` | `967d7afd…` | cola completa `9ddb7882…`, representativa `2e266bbf…`, anual `d86eddf7…`, `decisiones.csv` `d21be8b2…`, catálogo `07ab9cd8…` |
| `assembleDebug` del export de `c7ef3c6` | **`d22727f0…`** | `967d7afd…` | Idénticos |

**El APK compilado es idéntico byte a byte al entregado, firma incluida.** También son idénticos, uno a uno,
los 422 ficheros de fuera de `META-INF/`. `aapt dump badging` da `versionCode='3615'` y `versionName='3.6.15'`.
`classes.dex` es el de la 3.6.14 (`9a5d877c…`, librerías).

---

## 3. Casos de aceptación de principio a fin

### 3.1 Resultado por caso

Punto de partida: el diario real de `campana_SLV-002_20260919_151045.zip` (3.6.11): 38 series, 7 pasos del
banco hechos, protocolo 3×3. El banco se simula midiendo cada paso con `Protocolo.efectivo(p, true)`, igual que
`BancoActivity.java:532-539`.

| ID | Caso | Resultado | Evidencia |
| :--- | :--- | :---: | :--- |
| C1 | Instalar encima de la 3.6.11 con el banco a medias | **PASA** | Misma firma, SHA-256 `c990adf6…` en la 3.6.11, la 3.6.14 y la 3.6.15. `versionCode` 3611 → 3615. La cola completa ya era `9ddb7882…` en `fda734e`. La 3.6.15 lee el diario real con **0 líneas malas**. Sin evento `COLA` y con pasos hechos, se queda en COMPLETO (`BancoActivity.java:324-335`), y el siguiente paso es el 8 (P3, sesión 1). R1 |
| C2 | Terminar el banco a 1×4, con A5 y OSCURO a K = 5 | **PASA** | 152 medidas nuevas: 132 de 1 colocación y el resto a K = 5. Al acabar, `siguiente` = null, y el 8, el b y el 5 son calibrables. El modo rápido viene activo por defecto, también al actualizar (`BancoActivity.java:84-87`). El acta anota "protocolo banco 8: P34 1×4, P37 1×4, P43 1×4, P44 1×4; re-medida 5×4" (R6). **Con K = 1 no se evalúa la reproducibilidad** (`Veredicto.java:106-108`): una colocación mala sólo la detectan el orden y el parecido. Es la decisión PROTOCOLO-MIN, no un defecto |
| C3 | Rehacer un patrón equivocado | **PASA** | Rehacer P37 (paso 52) con el banco terminado: vuelve el 52; el 8 deja de ser calibrable; `seriesSinExportar` = 1. Tras volver a medirlo, la cola pide el EXPORTAR 60 (`RehacerBanco.java:63-77`). El motivo con salto de línea se guarda en una línea. R1 |
| C3b | Rehacer P43 con el 8 ya ACEPTADO | **FALLA** | Avisa "habrá que volver a calibrar el 8" (`FlujoCalibracion.java:1391-1393`), pero después no deja hacerlo. **QA-3615-05**. R6 |
| C4 | Exportar | **PASA**, con pega | ZIP ligero e incremental con `indice.sha256` y `resumen.txt` (`Campanas.java:509-554`). Tamaños en §3.2. El texto de Campaña promete "un solo ZIP con todo", y eso ya no es cierto. **QA-3615-07** |
| C5 | "Cambiar serie" a SLV-002-2026 | **PASA** en el camino normal | `#GN` → `#SN` → `#GN` y `RENOMBRA` (`FlujoCalibracion.java:1405-1439`). La campaña, las decisiones y los heredados siguen (`renombrarYCalibrarConLaSerieNueva`). **Con un corte tras el `#SN`: bloqueo.** QA-3615-01. R2 |
| C6 | "Calibrar todo": 8 → acta aceptada → b → 5 | **PASA** | Con el banco 1×4: "Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO". 3 actas, 4 apagados (T-C41 y 3 persistencias), 3 `#SC`, 15 diálogos de re-medida. El 8 se acepta antes de tocar el b (`FlujoCalibracion.java:1352-1366`). R6 |
| C7 | ZIP de soporte | **PASA** | Todo entero, con los índices de los incrementales, y copia en `Download/RTV/` (`Campanas.java:558-590`). Sale solo al aceptar (`CalibrarActivity.java:418-433`) |
| C8 | Importar ese ZIP en otro teléfono | **PASA** la importación; **FALLA** calibrar allí tras renombrar | Sin renombrar: 191 series, 180 pasos, 1 anulada, P37 con la serie buena, el 8 calibrable. R1. Tras renombrar, el otro teléfono no calibra: **QA-3615-02**. R3 |

**Negativos:**

| ID | Caso | Resultado | Evidencia |
| :--- | :--- | :---: | :--- |
| N1 | Corte de Bluetooth en cada fase de "Calibrar todo": `#L`, `#S,8`, `#SC`, `#S,b`, `#S,5` | **PASA** la seguridad, **FALLA** el mensaje | En todos los casos se retoma, y con otra pulsación sobre lo que falta se llega a las 3 actas. La que se acepta al retomar no sale en el mensaje ni exporta ZIP: **QA-3615-04**. R5 |
| N1b | Corte tras el `#SN` de "Cambiar serie" | **FALLA** | Equipo `SLV-002-2026`, historial de la campaña `[SLV-002]`. La app dice "La serie #GN# (SLV-002-2026) no coincide con la campaña (SLV-002): no se calibra". Volver a cambiar la serie, a SLV-002-2026 o a SLV-002: "no es de la campaña abierta: no se cambia". **QA-3615-01**. R2. Con el `#SN` sin respuesta y sin aplicar, la app dice "NO quedó cambiada" y todo queda coherente |
| N2 | App matada en la re-medida del 8, del b y del 5 | **PASA**, mismo mensaje confuso | Se retoma desde el disco. El acta a medias se acepta en la pulsación siguiente, que después contesta "El código 8 ya tiene un acta ACEPTADA: desmárquelo". R5 |
| N3 | Re-medida no conforme ×2, en el 5 (P81 colocado a 0,85) | **PASA** la seguridad, **FALLA** la guía | "Hecho: 8 ACEPTADO; b ACEPTADO; . Código 5: … restaurado tras dos re-medidas no conformes … La secuencia se detiene". Tiene 1 `#F,5`, y el acta queda abierta sin código conforme. Pulsar otra vez da "El acta en curso no está lista: … ningún código ha quedado conforme" y no dice que hay que rechazar. **QA-3615-06**. R6 |
| N4 | Batería a 0 | **PASA** | `leerBateria` → "bloqueadas las escrituras". "Calibrar todo" → "No se calibra: Batería…", ninguna trama de escritura. Tras cambiarla, sin bloqueo. Si el equipo se apaga antes del `#S,b`, el 8 queda aceptado y el b se retoma como en N1 |
| N5 | Otro equipo con la serie antigua SLV-002 y otra MAC | **PASA**, con pega | Ninguna casilla, y "Calibrar todo" no envía ningún `#S` (`TablaCalibracion.java:46-66`). La importación del ZIP de SLV-002 lo rechaza (`conOtraMacLaSerieAntiguaSeRechaza`). **Pero se deja renombrar a SLV-002-2026**, y quedan dos equipos con la misma serie. QA-3615-09 |
| N6 | ZIP de la 3.6.11 importado tras el renombrado | **PASA** en el mismo teléfono y **FALLA** con el rodeo del aviso B | En el teléfono que renombró: 38 series (lleva `RENOMBRA`). En otro teléfono con campaña "SLV-002", la del nombre Bluetooth: 38 series. En otro teléfono con la serie manual SLV-002-2026 (aviso B): "la campaña es de otro equipo (SLV-002 …): no se importa nada". Hay que importar primero el soporte, que trae el `RENOMBRA`, pero la importación no lo copia al destino (QA-3615-02). R3 |
| N7 | ZIP de banco representativo importado en una campaña vacía | **FALLA** | La campaña destino queda en COMPLETO. **24 de 31 pasos** apuntan a otro patrón (orden 8: P51 en la representativa, P3 en la completa), y no se puede cambiar: "el banco ya empezó con COMPLETO". **QA-3615-03**. R4 |
| N8 | "Calibrar todo" con b y 5 sin el 8 aceptado | **PASA** | Ninguna trama. "Código b: … va después de un acta ACEPTADA del código 8". El texto no dice "No se ha empezado la sesión", como sí dice la comprobación previa |

### 3.2 Tamaño del ZIP incremental

Se continúa el diario real de las 15:10 hasta el final de la cola completa a 1×4. En cada EXPORTAR se aplican
`PaquetesZip.incremental` y `PaquetesZip.zip` (DEFLATE 9), como hace `Campanas.exportar`, con `resumen.txt`
completo e `indice.sha256`. Las actas no entran: son unos pocos kB.

| ZIP | Contenido nuevo | Tamaño | Frente a las 12:27 (80,6 kB) |
| :--- | :--- | ---: | ---: |
| 1.º incremental tras actualizar (sesión 1, paso 60) | diario entero (80,8 kB) y `pruebas.txt` | **20,6 kB** | 26 % |
| Sesión 2 (paso 125) | tramo del diario de 38,7 kB | **13,0 kB** | 16 % |
| Sesión 3 (paso 180) | tramo de 34,4 kB | **13,1 kB** | 16 % |
| Tras rehacer P37 | tramo de 0,8 kB | **9,9 kB** | 12 % |
| Soporte sin tramas (diario, `campana.csv`, pruebas) | — | 40,3 kB | — |
| Soporte con 3 registros de tramas de 137 kB (el real de las 14:11 como modelo) | — | 152,1 kB | — |
| El mismo soporte con la compresión por defecto (la de la 3.6.14) | — | 157,0 kB | DEFLATE 9 ahorra el 3 % |

- **El incremental de cada sesión ocupa unas 6 veces menos que el ZIP de las 12:27.**
- **No cumple el "≤ 10 kB" de RF-APP-50** (`PLAN-Banco-Representativo.md:444`). Lo que pesa es `resumen.txt`,
  que va entero cada vez y crece con el banco: 29 kB → 47 kB sin comprimir. Un incremental con 0,8 kB de datos
  nuevos ocupa 9,9 kB. QA-3615-08, Baja.
- **Un incremental no se puede importar.** Desde el segundo, el diario va como `…csv.desde_N`:
  `diarioDeZip` = null y `csvDeZip` = null (`ImportadorCampana.java:52-63`). La app contesta "el ZIP no trae
  campana.csv" (`CampanaActivity.java:743`). Aviso C.

### 3.3 Pruebas de ruptura (fuera del repositorio)

`Qa3615RomperTest.java` (11 pruebas) está en el scratchpad y corre sobre las clases del export. Usa
`EquipoSimulado`, y los andamios `Almacen` y `Operador` de `FlujoCalibracionTest`. **No se ha añadido al
árbol.** Imprime lo observado sin afirmar nada.

| # | Secuencia | Salida |
| :---: | :--- | :--- |
| R1 | Diario real de la 3.6.11 → 3.6.15; banco a 1×4; rehacer P37; exportar; importar el soporte en otro teléfono | `malas=0 series=38 pasos=7 colaTipo=COMPLETO siguiente=8 PATRON P3`; `calibrable 8/b/5 = true/true/true`; `191 series … 180 pasos … 1 anuladas` |
| R2 | `#SN` con corte después; reconectar; volver a cambiar la serie | `serie en el equipo=SLV-002-2026 historial=[SLV-002]`; `no coincide con la campaña (SLV-002): no se calibra`; los dos reintentos: `no es de la campaña abierta: no se cambia` |
| R3 | Renombrar en A; importar el soporte de A en B; calibrar en B | B: `historial=[SLV-002]`, previas `no coincide con la campaña`. Con la serie manual SLV-002-2026: `previas=null`, `canonico=SLV-002`. El ZIP de la 3.6.11 en esa campaña: `otro equipo … no se importa nada` |
| R4 | Banco representativo (31 pasos) importado en una campaña vacía | `colaTipo=COMPLETO colaElegida=false`; `24 de 31` pasos en otro patrón; `elegirCola(REPRESENTATIVO)`: `el banco ya empezó con COMPLETO` |
| R5 | Corte en `#L`, `#S,8`, `#SC`, `#S,b` y `#S,5`, y app matada en cada re-medida; retomar | 2.ª pulsación: `El código 8 ya tiene un acta ACEPTADA: desmárquelo.` (el acta a medias queda aceptada); 3.ª con lo que falta: `Sesión completa`. Tras `#S,5`: `Nada que calibrar.` |
| R6 | "Calibrar todo" feliz; rehacer P43 tras aceptar; re-medida del 5 a 0,85 | `Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO`. Tarjeta del 8 `casilla=false`, `calibrarTodo(8)` → `desmárquelo`. En el 5: `restaurado … La secuencia se detiene`, acta abierta, `ningún código ha quedado conforme` |

---

## 4. Cierre real de QA-3614-01..10 y QA-3613-01

Un subagente hizo la lectura. Sus líneas se contrastaron con el código y dos de sus huecos se reprodujeron
(R5, R6). V15 = `Version3615Test`, FCT = `FlujoCalibracionTest`.

| ID | Declarado | Estado real | Evidencia | Test |
| :--- | :---: | :--- | :--- | :--- |
| QA-3614-01 Importación incremental | Cerrado | **Cerrado por el diario**; residuo por `campana.csv` | La serie que ya estaba se anula (`ImportadorCampana.java:114-119`), y la elegida se decide después (`:145-161`). R1 lo confirma con el soporte. Por la vía `campana.csv` (un CSV suelto) la serie existente se salta sin anular (`:367-369`) | V15 `importarDosZipSucesivosConservaLaAnulacion` |
| QA-3614-02 Salto de línea | Cerrado | **Cerrado**, con residuo | `Csv.unir` quita los saltos (`Csv.java:48-57`), y el lector une las comillas abiertas (`:63-87`). Quedan sin `setSingleLine` "Aceptar con nota" y la serie manual (`CampanaActivity.java:141`, `:605`), pero no rompe nada. Una comilla huérfana haría que `lineaLogica` se tragara el resto del fichero (`Csv.java:69-75`) | V15 `unMotivoConSaltoDeLineaNoRompeElDiario` |
| QA-3614-03 Rehacer y acta | Cerrado | **Parcial** | Con el acta en curso, BLOQUEO (`FlujoCalibracion.java:1385-1389`, `BancoActivity.java:169-184`). El acta guarda sus series y no se acepta con una anulada (`:338-351`, `:657-659`). Con el acta ACEPTADA sólo avisa, y luego no deja recalibrar (QA-3615-05). El aviso mira el código del paso: el OSCURO y la A5 son código `e`, así que rehacer el OSCURO que ancló el 8 no avisa | FCT `rehacerConActaEnCursoSeBloqueaYConActaAceptadaSeAvisa`, `unaSerieAnuladaEnElBancoImpideAceptar` |
| QA-3614-04 Volver a exportar | Cerrado | **Cerrado** | La anulación cuenta como pendiente (`Campana.java:433-435`), y el EXPORTAR vuelve a REHACER (`RehacerBanco.java:70-76`). R1 | V15 `trasExportarRehacerVuelveAPedirElZip` |
| QA-3614-05 OSCURO o A5 de otra sesión | Cerrado | **Parcial** | Bloqueado si la sesión terminó (`RehacerBanco.java:39-56`). Pero `sesionTerminada` no cuenta un EXPORTAR en REHACER (`:28`), y el propio rehacer lo pone así (`:73-74`). Rehacer un patrón de la última sesión vuelve a abrir su OSCURO y su A5. La A5 no tiene test | V15 `noSeRehaceUnOscuroDeUnaSesionTerminada` (sólo el OSCURO) |
| QA-3614-06 "No, rehacer" | Cerrado | **Cerrado** | Se cierra no aceptada, sin HECHO, y se anula después (`BancoActivity.java:883-892`) | V15 `noRehacerNoDejaLaSerieAceptada` |
| QA-3614-07 Fecha de la anulación | Cerrado | **Cerrado** | `ImportadorCampana.java:117`, `:140` | V15 (sólo la serie que ya estaba) |
| QA-3614-08 Documentación | No declarado | **Parcial** | La cabecera de `Campana` ya lista ANULA, REHACER, RENOMBRA y COLA (`Campana.java:34-40`). En `TDD-V3.6.md` sigue sin haber ningún caso de rehacer (`grep -ci rehac` = 0), ni de renombrar ni de "Calibrar todo" | — |
| QA-3614-09 Reglas del acta | Cerrado | **Cerrado**, con residuo | No se acepta con otra `VERSION` (`FlujoCalibracion.java:400-403`). `VERSION` lleva "APK 3.6.15" (`TablaCalibracion.java:33`), aunque el javadoc dice "no del APK" (`:30`), así que cualquier versión nueva deja sin aceptar las actas abiertas. Aviso G | FCT `unActaAbiertaConOtrasReglasNoSeAcepta` |
| QA-3614-10 Aviso si falla el ZIP | Cerrado | **Parcial** | El error se añade al texto (`CalibrarActivity.java:367-372`). Pero al retomar con "Calibrar todo" se acepta sin exportar y sin avisar (QA-3615-04). Reabre QA-3612-16 en ese camino | — (R5 lo reproduce) |
| QA-3613-01 Rechazo con restauración fallida | "No se cierra" | **Cerrado** en el código: el acta sigue abierta (`FlujoCalibracion.java:1295-1300`) | "No se cierra" describe el arreglo, no el estado. Residuo: con el `#S` sin resolver, el fallo sólo queda en el registro | FCT `unRechazoConLaRestauracionFallidaNoSeCierra`, `unRechazoConCorteYRestauracionFallidaNoSeCierra` |

**Contradicciones con el commit (entregable):**

- QA-3614-03, -05 y -10 se declaran cerrados y quedan parciales.
- QA-3614-01 sólo está cerrado por la vía del diario.
- **Los tests del renombrado modelan una campaña llamada "SLV-002-2026"** (`Version3615Test.java:352-400`). En
  el teléfono, la campaña se abre con la serie del nombre Bluetooth, `COVIANDINA_SLV-002` (`Sesion.java:160-167`,
  `Campanas.java:70-77`), y `#SN` no cambia ese nombre (`PROTOCOLO-V3.6.md:53`). Así que
  `Campanas.claveRenombrada` (`Campanas.java:137-155`) sólo actúa con una serie manual. Es el origen de
  QA-3615-02.

---

## 5. Usabilidad: calibrarlo todo, frente a la 3.6.14

Mismo criterio que `QA-App-3.6.14.md` §5. Conteo del subagente por lectura, contrastado con R6: 15 diálogos de
re-medida, 4 apagados y 3 `#SC`.

| Paso (8, b y 5) | 3.6.14 (actas [8] y [b+5]) | 3.6.15 (actas [8], [b] y [5]) |
| :--- | ---: | ---: |
| Casillas, nota, botón, PIN | 8 toques, 2 textos | 5 toques, 1 texto |
| T-C41 "Apagar y encender" | 2 | 1 (b y 5 aprovechan la persistencia anterior, `FlujoCalibracion.java:901-903`) |
| Re-medidas (colocar y 4 "Levante y apoye", ×3) | 15 | 15 |
| Persistencia y aceptar | 8 | 3 (automáticas; sólo el OK de apagar) |
| Compartir ZIP | 2 | 2 |
| **Total** | **33 toques, 2 textos** | **26 toques, 2 textos** |

**Mejora:** una sola pulsación arranca la sesión, se ahorran 7 toques, se ve el error de exportación, y
"Cambiar serie" conserva la campaña (6 toques y 4 textos).

**Empeora o confunde:**

- **"Calibrar todo" acepta sin la confirmación explícita** que "Aceptar" enseña (`CalibrarActivity.java:394-398`).
  La conformidad se da antes, con el nombre y la nota. Graba `#SC` tres veces en lugar de dos.
- **Con 3, 4 y 6 salen un acta, una persistencia y un `#SC` por código:** 7 apagados frente a 4.
- **Un código marcado que no se puede escribir impide empezar la sesión entera.** Es correcto, pero obliga a
  desmarcarlo (`FlujoCalibracion.java:1343-1351`).
- **Textos:**
  - "Continuar / un código" (`CalibrarActivity.java:71`) no dice cuándo se usa.
  - "Exportar campaña (un solo ZIP con todo)" (`CampanaActivity.java:109-111`) es falso.
  - "Hecho: 8 ACEPTADO; . Código b" tiene la puntuación rota (`FlujoCalibracion.java:1360`).
  - "…(QA-3613-03).. No se ha empezado la sesión." lleva doble punto (N5).
  - Hay identificadores internos a la vista: "RF-APP-49", "T-C41", "PA-12", "P12 §6.3".
  - "Campaña de verificación anual: no se calibra (RF-APP-49)" y "El banco ya empezó con …: no se cambia a mitad"
    no dicen cómo salir.
- **Tras aceptar salen dos selectores de compartir seguidos, con el mismo título** (`CalibrarActivity.java:424-427`).

---

## 6. Defectos nuevos

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :---: | :---: |
| **QA-3615-01** | **Un corte o la app muerta entre `#SN` y `RENOMBRA` bloquea la calibración, sin salida dentro del flujo.** El equipo queda en SLV-002-2026 y la campaña sin el alias. Las previas lo rechazan, y "Cambiar serie" exige que la serie actual (#GN) sea de la campaña, tanto para avanzar como para volver. Lo mismo pasa si se graba por "Alta de la serie" (`#SN` sin `RENOMBRA`) cuando `serieEquipo` es null (pruebas sin hacer o `#GN` fallido). **Rodeo, por lectura de código y sin probar:** reconectar sin pasar las pruebas (`serieEquipo` = null, `Sesion.java:114-123`). Entrar en Avanzado con el override, grabar `SLV-002` por "Alta de la serie" (`AdminActivity.java:564`, `:600-611`), pasar las pruebas y repetir "Cambiar serie" | `FlujoCalibracion.java:1423-1437`, `:261-267`. R2 | **Alta** | **P1** (aviso A) |
| **QA-3615-02** | **En otro teléfono no se calibra un equipo renombrado.** La importación no copia `RENOMBRA` (en `ImportadorCampana.java` sólo aparece en un comentario, `:181`; comprobado con grep y con Grep). La campaña del destino se abre por el nombre Bluetooth (SLV-002) y su historial no incluye SLV-002-2026. Rodeo probado: serie manual SLV-002-2026 antes de importar. Con ese rodeo, el ZIP de la 3.6.11 ya no entra | `Sesion.java:160-167`; `Campanas.java:70-77`; `FlujoCalibracion.java:261-267`. R3 | **Alta** | P2 (aviso B) |
| **QA-3615-03** | **La importación no trae el tipo de banco (`COLA`).** Un diario del banco representativo entra en COMPLETO, con los órdenes de otra cola: 24 de 31 pasos apuntan a otro patrón, y la cola ya no se puede cambiar. Al revés también: un teléfono con otra campaña abierta de esa MAC elige REPRESENTATIVO al abrir el Banco (`BancoActivity.java:324-331`), y un banco completo importado después se leería con la cola equivocada. `calibrable` y la cola quedan mal | `ImportadorCampana.java` (sin `COLA`); `Campana.java:460-469`. R4 | **Alta** | P2 (aviso B) |
| QA-3615-04 | **Al retomar, "Calibrar todo" acepta el acta a medias sin decirlo y sin ZIP.** No la apunta en `hecho` y devuelve "El código k ya tiene un acta ACEPTADA: desmárquelo" o "Nada que calibrar". El texto no lleva " ACEPTADO; ", así que no se exporta | `FlujoCalibracion.java:1326-1339`, `:1347`; `CalibrarActivity.java:367-368`. R5 | Media | P2 (aviso D) |
| QA-3615-05 | **Tras un acta ACEPTADA, rehacer avisa de que hay que recalibrar, pero la app no lo deja.** La tarjeta queda sin casilla ("no se vuelve a escribir en esta sesión") y "Calibrar todo" responde "desmárquelo". La curva escrita queda apoyada en una serie anulada | `FlujoCalibracion.java:1391-1393`, `:590`, `:1347`; `CalibrarActivity.java:284`. R6 | Media | P2 (aviso F) |
| QA-3615-06 | **Tras dos re-medidas no conformes, la app no dice que hay que rechazar.** El acta queda abierta sin código conforme, y "Calibrar todo" contesta "El acta en curso no está lista" | `FlujoCalibracion.java:1054`, `:1335`. R6 | Media | P2 (aviso E) |
| QA-3615-07 | **Textos de exportación falsos.** "Exportar campaña (un solo ZIP con todo) … no hace falta compartir nada más" entrega el ZIP ligero, sin tramas ni `campana.csv`, que no se puede importar. "Expórtelo a mano en Campaña" lleva a ese mismo botón. Importar un incremental responde "el ZIP no trae campana.csv" y no remite al de soporte | `CampanaActivity.java:109-111`, `:743`; `CalibrarActivity.java:371`; `Campanas.java:509-554` | Media | P2 (aviso C) |
| QA-3615-08 | El incremental de una sesión ocupa 13 kB, frente al ≤ 10 kB de RF-APP-50. Lo que pesa es `resumen.txt`, que va entero | §3.2 | Baja | P3 |
| QA-3615-09 | **Se puede renombrar a SLV-002-2026 un equipo con otra MAC.** No recibe tabla ni decisiones, pero quedan dos equipos con la misma serie | `FlujoCalibracion.java:1405-1439`; `TablaCalibracion.java:46-66`. R (N5) | Baja | P3 |
| QA-3615-10 | **El tipo de banco por defecto no detecta "el primer banco hecho".** Cuenta las campañas abiertas de otra serie con la misma MAC; las archivadas no cuentan. En el flujo normal siempre sale COMPLETO, y con la serie manual del aviso B sale REPRESENTATIVO | `Campanas.java:120-133`, `:403`; `BancoActivity.java:324-331` | Baja | P3 |

**Observaciones:**

- **O-12.** Tras renombrar, las series nuevas siguen anotando `equipo` = SLV-002, el del nombre Bluetooth
  (`BancoActivity.java:661`). La trazabilidad va por `RENOMBRA` y la MAC. El acta sí dice "SLV-002-2026 (antes
  SLV-002)".
- **O-13.** Con 1×4, la reproducibilidad por patrón no se evalúa (`Veredicto.java:106-108`). Es la decisión
  PROTOCOLO-MIN. La s_rep sigue saliendo de la A5 a K = 5 (V15 `laSrepSigueSaliendoDeLaA5ConPatronesA1x4`).
- **O-14.** "Colocación no válida" en la re-medida no tiene límite de repeticiones. Si el operador pulsa OK
  siempre sin corregir, el bucle no acaba (`FlujoCalibracion.java:1116-1125`). Una prueba con el operador
  simulado lo agotó hasta quedarse sin memoria.

---

## 7. Instalación encima de la 3.6.11 o la 3.6.14

| Comprobación | Resultado |
| :--- | :--- |
| Versión y firma | 3611 / 3614 → 3615, el mismo certificado `c990adf6…` (`apksigner verify --print-certs`): **se instala encima** |
| Banco en curso | **Se conserva.** La cola completa es la misma (`9ddb7882…`). El diario real de las 15:10 se lee con 0 líneas malas y sigue en el paso 8 (R1) |
| Acta en curso de una versión anterior | No se acepta: `VERSION` distinta (QA-3614-09). Rechazarla (aviso G) |
| Hacia atrás | Un diario con `COLA` o `RENOMBRA` no se importa en la 3.6.14: una línea que no entiende y la importación es atómica. Aviso H |
| Riesgo que queda | El de siempre: exportar el soporte antes de actualizar |

---

## 8. Qué falta para APTO sin avisos

1. **QA-3615-01:** escribir `RENOMBRA` antes del `#SN`, o aceptar en "Cambiar serie" una serie actual que sea
   la nueva pendiente; y no grabar por "Alta" si la campaña ya tiene serie.
2. **QA-3615-02 y -03:** importar `RENOMBRA` y `COLA`, y rechazar un diario cuya cola no coincida con la del
   destino.
3. **QA-3615-04 a -06:** poner en `hecho` el acta retomada, permitir recalibrar tras un rehacer avisado, y
   decir "Rechazar" tras la restauración.
4. Corregir los textos de QA-3615-07, y añadir al TDD los casos de rehacer, renombrar y "Calibrar todo"
   (QA-3614-08).
5. Probar en el teléfono los cuatro caminos que aquí sólo se han leído: reanudar tras un corte real, "Cambiar
   serie", importar el soporte en otro teléfono y los dos selectores de compartir. Pendiente también lo de
   `QA-App-3.6.13.md` §8: T-C41 y T-C43 en SLV-002, con el registro archivado.
