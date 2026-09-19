# Matriz SPEC ↔ código de la V3.6

**Nada de esta matriz está medido salvo lo que cita un registro de tramas, el acta de SLV-002 o el ZIP
`06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip`.** "Cumple" quiere decir **que el
código hace lo que pide el requisito**, no que se haya probado en un equipo. Revisión del 19-sep-2026 por
la tarde contra la **app 3.6.9** y el **firmware 3.6.2**. La revisión anterior (09:30-10:30, app 3.6.4 y
firmware 3.6.1) queda como histórico en §0.3 y en las columnas "antes".

## 0. Qué se ha leído

### 0.1 Versiones

| Pieza | Versión | Cómo se fija |
| :--- | :--- | :--- |
| Firmware | **V3.6.2**, commit `6a32ca3`, `.hex` md5 `9d5d5e3951c8aa83d1465f16f3733d27` | md5 del fichero = md5 del blob de `6a32ca3` = `hex/RetroVertical_V3.6.hex.md5` (comprobado). Frente a `8860445` sólo cambia `calibracion_v36.c` (`git diff 8860445 6a32ca3`: dos bloques, +110 líneas tras `:476` y +44 tras la antigua `:605`) |
| App | **RTV 3.6.9**, commit `f52eeb1` (`versionName "3.6.9"`, `versionCode 369`: `app/build.gradle:14-15`) | El árbol de trabajo de `app/src` no difiere de `f52eeb1` (`git diff --stat f52eeb1`, vacío) |
| Pruebas JVM de la app | **89**, en 9 clases | **Ejecutadas en este trabajo: `OK (89 tests)`** (§0.2) |
| Evidencia de campo | ZIP `campana_SLV-002_20260919_122727.zip`, md5 `ce1f35fc64439cbb602d014b725fadfb` (comprobado) | Descomprimido fuera del repositorio. Sus registros de tramas llevan `# app: com.dpi.retrov36 3.6.9 (369)` (`tramas/rtv36_20260919_113200.txt:5` y siguientes); el de las 11:16, 3.6.8. **Que el APK instalado sea `f52eeb1` no está comprobado** (C-16) |

Rutas: firmware relativo a `01_Firmware/RetroVertical_V3.6.X/`; app relativo a
`03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Las citas `acta:N` son líneas de
`actas/acta_SLV-002_00211305193B_20260919_122326.txt` dentro del ZIP; `resumen:N`, de su `resumen.txt`;
`T4:N`, de su `tramas/rtv36_20260919_114644.txt`.

### 0.2 Pruebas JVM, resultado real

JDK 17.0.12 (`D:/@Proyect/Baliza/7 sw apk/jdk-17/`), `javac -encoding UTF-8 -sourcepath app/src/main/java`
sobre `app/src/test/java/com/dpi/retrov36/*.java`, classpath `libtest/junit-4.13.2.jar` y
`libtest/hamcrest-core-1.3.jar`, `JUnitCore` con las nueve clases:

| Clase | Pruebas | | Clase | Pruebas |
| :--- | ---: | :--- | :--- | ---: |
| `AsistenteTest` | 12 | | `ReceptorTest` | 5 |
| `CalculoTest` | 31 | | `Version367Test` | 7 |
| `CampanaTest` | 15 | | `Version368Test` | 6 |
| `CoherenciaRealTest` | 7 | | `Version369Test` | 5 |
| `FabricaTest` | 1 | | **Total** | **89, OK** |

**Hay que ejecutarlas con el directorio de trabajo en `app/`.** Desde `03_App_Movil/RetroV36/` salen
`Tests run: 89, Failures: 25`: `Version369Test.java:26` abre `src/main/assets/patrones_certificados_P1-P31.csv`
y `:23` el ZIP de las 10:33 con rutas relativas a `app/`. La receta de
`REVISION-Arquitectura-P9-V3.6.md:504-505` ("seis clases: `OK (71 tests)`") no lo dice y está desfasada.

### 0.3 Histórico (revisión de las 09:30-10:30, no se borra)

- Firmware leído entonces: V3.6.1, `8860445`, md5 `8736c05d0273fdda66d5988f472d41e1`. App: 3.6.4, `090c84c`
  (RF-APP-29, 30 y 32 sobre la 3.6.5, `ff66f93`). Pruebas JVM ejecutadas entonces: 56, `OK`.
- *"SLV-002 lleva grabada la V3.6 de 2026-09-18"*: falso desde las 09:36 del 19-sep (V3.6.1, `869d3c6`).
  Desde entonces volvió a cambiar: el commit `78924ae` (10:38) y
  `01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.2_2026-09-19.log:20` registran la **V3.6.2**
  (*Program Succeeded*). Y el equipo lo confirma: a las 11:16 `#V#` responde
  `#V,3.6,2026-09-19,DEF,0000#` y `#GC#` responde `#GC,NONE#`, que sólo sabe la 3.6.2
  (`tramas/rtv36_20260919_111628.txt:13-27` del ZIP). **C-36 queda cerrada: SLV-002 lleva la 3.6.2.**
- *"Sin el primer disparo, s ≈ 5"*: correcto en número pero con subida de 3-4 disparos (−17,4 · −5,1 · −3,5
  · −0,8 … cuentas por posición, 17 series del 19-sep por la mañana). Sigue vigente: C-38.

## 1. Firmware (RF-FW)

Citas de `calibracion_v36.c` **en `6a32ca3`**. Todo lo anterior a la línea 477 no se ha movido; lo que
estaba entre 477 y 605 baja 110 líneas y lo que estaba tras 605 baja 154. Las citas desplazadas se han
comprobado leyendo el fichero, no sumando.

| RF | Estado | Evidencia (`6a32ca3`) | Prueba que lo cubre y estado |
| :--- | :--- | :--- | :--- |
| 01 Enlace 9600 | Cumple | `mcc_generated_files/` sin cambios desde la base (la 3.6.2 no los toca: `git diff --stat 8860445 6a32ca3`) | T-C05 parcial: el enlace funciona en SLV-002 (registros del 19-sep) |
| 02 Adquisición igual al fuente 2020 | Cumple (código) | `measurement.c:222-247`; único cambio `:212`; factor `gui.c:299-301` | T-C08 **falla por pantalla** en x = 1700-2300 (acta de la mañana). C-01 |
| 03 Códigos de medida | Cumple | `ecuacionesCalibracion.c:49-67,137-193` | T-C05 parcial |
| 04 Evaluación bit a bit | Cumple | `calibracion_v36.c:229-234` | T-A20 pasa (repetida con 3.6.2 en simulador, `CAMBIOS-V3.6.md` §8) |
| 05 Fábrica en ROM | Cumple | `calibracion_v36.c:34-47`; `gui.c:42-44`; `#F` copia `coefFabrica`: `:703-715` (antes `:593-605`) | T-A20, T-C04 pasan |
| 06 Negativos y saturación | Cumple | `calibracion_v36.c:230-233` | T-A21 parcial |
| 07 `e` | Cumple | `ecuacionesCalibracion.c:191-193` | T-C06 parcial (238 `e` en T4) |
| 08 `9` | Cumple (como en 2020) | `gui.c:268` (`nivel[6]`), `:329-340`. **Detalle de la respuesta en §1.1** | T-C12 pendiente: **ninguna respuesta `:n:` registrada con la V3.6** |
| 09 Byte no código | Cumple | `gui.c:296` | T-C13 pendiente |
| 10 STONE sin cambios | Cumple | `ecuacionesCalibracion.c:73-128`; `uart_stone.c:91-100` | T-C14 parcial |
| 11 Una tabla | Cumple | `ecuacionesCalibracion.c:3-41` → `aplicarEcuacion()` | T-C19 pendiente |
| 12 Defectos que se conservan | Cumple | `gui.c:296-343` | T-C15 pendiente |
| 13 Tramas `#` | Cumple | `uart_module.c:61-117` | T-C16, T-C17, T-C36 pendientes |
| 14 Límite de recepción | **Parcial** | UART1 `uart_module.c:87-100`; UART2 sin límite `uart_stone.c:95-98` (O-11) | T-A22, T-C18, T-C34 pendientes |
| 15 Trama durante medida | Cumple | `uart_module.c:94-99,108-117` | T-C20 pendiente |
| 16 `#V#` | Cumple | `calibracion_v36.c:635-643` (antes `:525-533`), máscara `:202-223`, fecha `:458-475`; versión `"3.6"` fija: `calibracion_v36.h:16` | T-C03 pasa con la 3.6.2 (`#V,3.6,2026-09-19,DEF,0000#`, T4:13-18). C-37 |
| 17 Sesión y PIN | Cumple | `:73-75,181-182`; bloqueo `:644-657,782-790`; caducidad `:298,337-341` | T-C21, T-C22, T-C31 pendientes. En campo: `#L` con el PIN enmascarado en el registro (T4:588) |
| 18 Lectura `#G`, `#GT` | Cumple | `:662-672,760-768` | T-C04 pasa |
| 19 Escritura y C4 | Cumple | `#S` `:690-702`; `#ST` con C4 `:769-781`, `temperaturaValida` `:381-389`; copia y deshacer `:587-605` | T-A30 pasa. **En equipo: `#S` de los códigos 1 y 2 → `#OK#`** (T4:1660-1664 y `:2520`) |
| 20 `#F` | Cumple | `:703-715`. La temperatura tiene ahora su orden: `#FT#` (§1, fila 3.6.2-a) | T-C25 pendiente |
| 21 `#P` | Cumple | `:782-796`, `pinValido` `:442-449` | T-C26 pendiente |
| 22 Números exactos (C1) | **No** | `strtod` `:352-363`; `%.8E` `:451-455`. En campo: se envió `2.98471545E-01` (T4:1660) y `#G` devolvió `2.98471571E-01` (acta:10) | T-A23 falla el criterio de 0 ulp |
| 23 EEPROM y comunicaciones | Cumple (código) | `guardarEeprom` `:128-154`; `#OK#` después: `:595-598` | T-C35 pendiente |
| 24 Bloque EEPROM | Cumple | `:53-69,128-199`; registros nuevos de la 3.6.2 fuera de la cabecera: `0x1EE` y `0x200` (`:477-491`) | T-C23 pendiente |
| 25 Nunca `@` | Cumple | Sin `@` en `calibracion_v36.c` 3.6.2 (Grep y `grep -c`: 0) | T-C27 pendiente |
| 26 Configuración | Cumple | `device_config.c` sin cambios | T-C01 pasa |
| 27 Cadena de compilación (G1) | **Parcial** | Blob = md5 declarado. `CAMBIOS-V3.6.md:163-165` cita el md5 de la 3.6.2 pero no el commit `6a32ca3` (Grep: sin resultados). `md5sum -c hex/fuente.md5` **falla en el árbol de trabajo** para `calibracion_v36.c` (`91d569d5…` frente a `996b480c…`): `core.autocrlf = true` lo deja con CRLF; el blob de `6a32ca3` sí da `996b480c…` | T-A28 parcial |
| 28 `#E` | Cumple | `:673-689`, `leerEntero` `:425-436` | En campo: `#E` en 5 puntos tras cada `#S` (acta:12,19) |
| 29 `#K#`, `#KC#` | Cumple | `:236-292,797-804` | T-A33, T-C33 pendientes |
| 30 Defectos de memoria | Cumple | `measurement.c:212`, `gui.c:268`, `uart_module.c:100` | T-A29 pendiente |
| 31 Límites de `#S` [r1.2] | Cumple | `:309-319`, `curvaValida` `:404-422`, aplicado en `:697-698` (antes `:587-588`) | T-A30 pasa; T-A41 |
| **3.6.2-a `#FT#`** (sin RF en la SPEC) | Cumple (código) | `:716-727`: repone `tempFabrica` (copiado al arrancar, `:179`) en RAM y EEPROM vía `guardarYResponder` `:595-605`; exige modo admin `:721`; el PIN no cambia | Simulador: `pruebas/T-FT/resultado_T-FT.txt`. **En equipo: nunca enviado** (ninguna `#FT#` en `07 pruebas/` ni en el ZIP) |
| **3.6.2-b `#SN` / `#GN#`** (sin RF) | Cumple (código) | `#SN` `:745-759` (1-12 caracteres, sin `#` ni `,`, `NONE` rechazado `:751`; validación `:522-524`); `#GN#` `:731-733` → `responderSerie` `:571-585`; registro `0x1EE` `:488`; lectura con CRC `:493-502`, escritura con relectura `:506-520` | Simulador: `pruebas/T-SN-SC/resultado_T-SN-SC.txt`. **En equipo**: `#SN,SLV-002#` → `#OK#`, `#GN#` → `#GN,SLV-002#` (T4:593-600); también `#SN,SLV-02#` → `#OK#` (T4:2048-2056), corregido en T4:2069-2077 (ver RF-APP-35) |
| **3.6.2-c `#SC` / `#GC#`** (sin RF) | Cumple (código) | `#SC` `:734-744` (`NONE` borra `:738-740`); `#GC#` `:728-730` → `responderFecha` `:554-569`; `leerFecha` `:536-552`, `fechaValida` `:527-534` (2020-2099; bisiesto `a % 4`, válido en ese rango); registro `0x200` `:489` | **En equipo**: `#SC,2026-09-19#` → `#OK#`, `#GC#` → `#GC,2026-09-19#` (T4:2763-2772). `#GC,NONE#` antes de grabar (`tramas/rtv36_20260919_111628.txt:19-22`) |

### 1.1 La orden `9` (batería), leída en el fuente

Qué responde el equipo (`gui.c`, función `measure()`, verificado leyendo el fichero):

- `gui.c:329` detecta `"9"` dentro del mismo bloque que atiende cualquier byte de la app (`:296`), así que
  un `9` **también dispara una adquisición** (`:298-301`) y pasa por `ecuacionesColoresApp` (`:327`).
- `gui.c:332`: n' = 2·bv − 21, con `bv` la variable `static float batteryVoltage` de `gui.c:19` (distinta de
  la de `measurement.c:18`). Pinzas: si 5 < n' < 10, n' = 5 (`:333`); si n' < 0, n' = 0 (`:334`). **No hay
  tope por arriba**: el comentario de `:332` ("entre cero a cinco") no se cumple con la batería cargada.
  `:335` trunca a `int`; `:336-340` envía `:<n>:`.
- `bv` se recalcula en cada vuelta del bucle, después de atender la trama: `bv = 45,455·(V − 10) − 4,5455`
  (`gui.c:349-351`), con `V = getBatteryVoltage()` (`measurement.c:109-111`). `V` sale de
  `acquireBatteryVoltage`: 5 muestras del ADC, ordenadas de menor a mayor (`binary_utils.c:35-45`), media
  de las tres mayores (`measurement.c:140-141`), × 0,001 × 4,3 (`:143-144`). El 0,001 supone 1 mV por
  cuenta (comentario de `:143`); no se ha medido.

Juntando: **n = trunc(90,91·(V − 10) − 30,09)**. Es invertible salvo pinzas:
**V ∈ [10 + (n + 30,09)/90,91 ; 10 + (n + 31,09)/90,91)**, un intervalo de 11 mV (la cuenta del ADC son
4,3 mV de batería). La fórmula del encargo, V = 10 + (n + 30,09)/90,91, es el extremo inferior. Con 12 V,
n ≈ 151; con 13 V, n ≈ 242 (cálculo, no medida). Lo que la rompe:

1. `n = 0`: cualquier V < 10,342 V (y todo `bv < 0`). No distingue una batería de 10,3 V de una muerta.
2. `n = 5`: V en [10,386 ; 10,441) V, por la pinza de `:333`. **n de 6 a 9 no puede salir nunca.**
3. La pinza [0 ; 99] de `:353-355` **no se aplica en cada vuelta**, contra lo que da a entender el encargo:
   sólo cuando `contadorBateria > 200` (`:352-353`), es decir, una vuelta de cada 201, y una vez al entrar
   por `taskGui` (`:512-516`) antes de lanzar `measure()` (`:519`). Si el `9` llega justo en la vuelta
   siguiente a una de ésas, `bv ≤ 99` y **n ≤ 177**: una respuesta 177 no se puede invertir (puede ser
   cualquier V ≥ 12,28 V). En las demás vueltas `bv` va sin pinza.

**No hay ninguna respuesta `:n:` registrada con la V3.6.** La única petición `9` a un firmware V3.6 está en
`07 pruebas/campana_103300/tramas/rtv36_20260919_095234.txt:15-16`, sin respuesta (a esa hora el equipo
tampoco respondió a `#V#` ni a `6`: `:13-20`). En los cinco registros del ZIP de las 12:27 no hay ningún `9`
(`grep ";TX;39;9"`: nada). La app 3.6.9 sólo envía `9` para detectar un V3 2020 cuando `#V#` no responde
(`Pruebas.java:305`) y en la línea base (`LineaBase.java:71`); no interpreta `n` como tensión en ningún sitio.

## 2. App (RF-APP)

"Antes" = estado en la 3.6.4 (o 3.6.5 donde lo diga la revisión anterior). Estado y evidencia, en la 3.6.9.

| RF | Antes | Estado 3.6.9 | Evidencia (`f52eeb1`) | Prueba que lo cubre y estado |
| :--- | :--- | :--- | :--- | :--- |
| 01 Enlace y pausas | Cumple | Cumple | `EnlaceSerie.java:33,157`; `Cliente.java:24-25,33,196-197` (sin cambios) | T-A07 pendiente |
| 02 Fin por silencio | Cumple | Cumple | `Receptor.java:14-15,59`; órdenes de escritura a 5000 ms: `AdminActivity.java:626,677,871,898,918,941` | T-A03 pasa |
| 03 Detección | Cumple | Cumple | `#V#` `Pruebas.java:256`; 3.6.1/3.6.2 por `#GC#` `:279-293`; `9` `:305`, `6` `:313`, `@LEERV` `:321`; nunca `e` | T-C03 pasa (T4:13-28); `Version367Test.deteccion361Frente362` |
| 04 Mapa color × tipo | Parcial | Parcial | Sólo para patrones: `Asistente.java:39-45` | Aplazado |
| 05 Medir en campo | No (aplazado) | No (aplazado) | — | T-B05 aplazada |
| 06 Registro de `x` | **Contradice** | **Contradice** | `LecturaX.java:62-64` sigue rotulando `::0` "saturado o negativo" | T-C06 parcial |
| 07 Coherencia | Parcial | Parcial | `ULP_G = 4` `Ecuacion.java:92`, `#G` `Pruebas.java:363`; `#E` exacto `:387-420`; `Coherencia.java:136-241` | T-A10 pasa; `CoherenciaRealTest` 7/7 |
| 08 Tabla de fábrica | Parcial | Parcial | Faltan los 3 de temperatura (Grep y `grep -rn` de `43212`, `90148`: nada) | T-A01 parcial |
| 09 Repetibilidad, 10 lecturas | **No** | **No** | `Repetibilidad.java:13` (`LECTURAS = 5`), `:14`. La campaña mide K × M (`CampanaActivity.java:73-76`, por defecto 3 × 3), que es otro requisito | T-A11 parcial |
| 10 Emulación e inversión en `float` | Parcial | Parcial | `Ecuacion.java:56-68`; inversión en `double` `Inversion.java:58` (C-21) | T-A05 parcial |
| 11 Forma de las curvas | Parcial | Parcial | `Asistente.validarForma` `:179-208` | T-A06 parcial |
| 12 Otras comprobaciones | Parcial | Parcial | Hay oscuro guiado: preajuste OSCURO `CampanaActivity.java:97,236-243`, `Campana.OSCURO` `:184`. `@` sólo en lo enviado: `Tramas.java:53-55` (C-31) | T-C27 pendiente; `Version369Test.oscuroYA5` |
| 13 Sesión de patrones | Parcial | Parcial | K colocaciones × M disparos con recolocación guiada `CampanaActivity.java:440-517`; oscuro (fila 12); **sin temperatura** | T-C10 parcial; `Version367Test.colocacionesKPorMAgregacion` |
| 14 Cobertura | Cumple | Cumple | `Asistente.java:75-118`, rango estrecho `:33-36,93-95`. Con el asset actual: 1, 2 y 8 hasta grado 2, b grado 1 | T-A12 pasa |
| 15 Ajuste | Parcial | Parcial | `Asistente.proponer` `:399-530`; criterio de `#S` `:261-280,478-481`; recta alternativa `:493-505`; **recta anclada en oscuro**: `GRADO_ANCLADA` `:220`, `Ajuste.anclada` `Ajuste.java:99-122`, ancla = media de la serie OSCURO elegida `AdminActivity.java:487-500`; RF-CAL-14/15/16 avisan y piden conformidad `Asistente.java:310-345`, `AdminActivity.java:565-597`. Falta: método fijado por código (RF-CAL-37) | T-A13, T-A14 pasan; `Version369Test.rectaAncladaReproduceLaPropuesta`, `blancoGrado1AvisaPeroNoBloquea` |
| 16 "Como llegó" | Cumple | Cumple | `Asistente.java:460-471` (R actual y desvío con la vigente); exige leer antes `AdminActivity.java:502-504` | T-A15 pendiente |
| 17 Escritura con restauración (C3) | Cumple | Cumple | `AdminActivity.java:660-717`: copia `:671`, `#S` `:677`, `#G` a `ULP_S` `:681`, `#E` en 5 puntos `:644-658`, restauración `:614-636`. **Sólo si el equipo contesta**: un corte del enlace no restaura (RF-APP-37) | T-A32 pendiente. En campo: 2 escrituras sin restauración (acta:10-12,17-19) |
| 18 `#F` | Cumple | Cumple | `AdminActivity.java:932-949` | T-C25 pendiente |
| 19 PIN | Cumple | Cumple | `AdminActivity.java:33-34,182-189,294-364,393-407` | T-A19 pendiente |
| 20 Registro técnico | Cumple | Cumple | `Registro.java` | T-A16 parcial |
| 21 Acta de calibración | **No** | **Parcial** | `Acta.java` (nuevo): protocolo fijo `:27-29,210-211`, un código cada vez `:106-117`, curva certificada = `#G` `:219`, re-medida `:141-162`, aceptar/rechazar `:175-203`; se guarda al cerrarla `AdminActivity.java:813-854` y va en el ZIP `Campanas.java:238-245`. Falta lo que pide la SPEC: **antes** por código (coeficientes, `x` y `R` por patrón), residuos (sólo salen dentro del texto de conformidad, acta:11,18); y el firmware que imprime es rancio (RF-APP-44) | T-A17 parcial; `Version368Test.laFechaSoloSeGrabaAlAceptarElActa`, `actaRechazadaNoLlevaFecha`, `b3ProtocoloDeLaCampanaEnElActa`. **Acta real aceptada** (acta:23-24) |
| 22 Copia de coeficientes | Cumple | Cumple | `Sesion.java:312-331` (la cita antigua `:229-230` es ahora la carga del catálogo); `AdminActivity.java:440,671,710,940,944` | T-C29 pendiente |
| 23 La app no escribe `#ST` | Parcial | Parcial | No se ofrece; `Tramas.tramaST` `:240-254`. La temperatura sólo se repone con `#FT#` (`AdminActivity.java:911-928`) | T-A37 parcial |
| 24 Botones de pantalla | Cumple | Cumple | `BotonesActivity.java`; `Tramas.java` | T-A36 pasa |
| 25 Línea base previa | Parcial, no realizable | Parcial, no realizable | `LineaBase.java:27` | T-B03, T-B07, T-B09 no realizables |
| 26 Longitud según versión | Parcial | Parcial | `Cliente.pedir` no mide longitud | T-A34 pendiente |
| 27 Estadístico y tolerancias | Parcial | Parcial | `Asistente.puntos` `:136-158`; `Coherencia.java:36`, `Repetibilidad.java:14`, `Veredicto.SD_MAX` `:34` | T-A35 pendiente |
| 28 Asentamiento [r1.2] | Parcial | Parcial | `Sesion.java:65` (1 por defecto); `MedidaActivity.java:49-51,142-151`, fijo con acta `:84-87`; `LecturaX.java:43-49`; prueba 3 `Pruebas.java:458-461`; campaña `CampanaActivity.java:485-488`; re-medida `AdminActivity.java:765-768` | T-A38, T-C38 |
| 29 Disparo descolgado [r1.2] | Parcial | Parcial | `Veredicto.descolgados` `:176-195`: umbral máx(5 · 1,4826 · MAD ; **50**) `:38,187`, sólo con ≥ 4 disparos `:40`; se descartan en la campaña `CampanaActivity.java:554-560` y no entran en el ajuste `Campana.java:574-575`. La SPEC pide suelo **30** (C-46). Por "Medida de patrones" no hay filtro | `CampanaTest.disparoDescolgadoDeP7`, `descolgadoC46P2RealNoSaltaP7Si`; T-A39 |
| 30 ¿Es este el patrón? [r1.2] | Parcial | Parcial | `Veredicto` orden (3 %) y parecido (±1 %) `:19-36`, sólo en la campaña; el ajuste avisa si no usa series de campaña `Sesion.java:86-92,241-250` | `CampanaTest.p24MedidoEnRealidadSobreP20`, `ordenPorCertificado`, `avisoDeXINoSeSuprime`; T-A40 |
| 31 Patrones tipo I [r1.2] | Parcial | Parcial | Asset sin cambios desde `090c84c` (`git diff --stat`, vacío): `assets/patrones_certificados_P1-P31.csv`, 51 patrones, 20 tipo I, P32 duplicado (`:41-42`). `Sesion.java:228-229` lo carga por nombre fijo. **El catálogo `06_Calibracion/patrones_certificados_P1-P132.csv` (133 filas) no está en la app** (Grep y `grep -rn` de `P1-P132`/`P132` en `app/src`: nada) | T-A12 pasa; T-C39 |
| 32 Modo Campaña [r1.2] | Parcial | **Cumple (código)** | Faltaban cierre y md5: **cierre** `Campanas.cerrarCampana` `:202-205`, `Campana.cerrarCampana` `:294-299`, sólo lectura `:288-292`; **md5 y sha256** `Campanas.exportarConHuellas` `:207-216` (`Resumen.hex`, `MessageDigest`), en el texto del envío `CampanaActivity.java:658-673`. Importa también el ZIP exportado `:683-757` | `CampanaTest` 15/15, `Version369Test.importarElZipRealDaLas56SeriesTalCual`, `laCampanaDeOtroEquipoNoSeImporta`. **ZIP real de las 12:27** con acta, diario, pruebas y tramas |

### 2.1 Requisitos nuevos de la r1.3 (lo que hace hoy la 3.6.9)

Los ID son los de `SPEC-V3.6.md` r1.3 y `SPEC-Calibracion-V3.6.md`, en redacción por otro agente; aquí sólo
se juzga el código. Cada "no existe" se ha comprobado con dos herramientas (Grep y `grep -rn`/lectura).

| RF | Estado 3.6.9 | Qué hay hoy, con evidencia | Prueba que lo cubrirá |
| :--- | :--- | :--- | :--- |
| APP-33 Banco guiado de los 133 patrones | **No** (piezas sueltas) | La cola sale del catálogo de la campaña (`Cola.construir` `Cola.java:54-108`), que es el asset P1-P31 (`Campanas.java:69`, `Sesion.java:229`). A5 y OSCURO son **preajustes aparte**, no pasos de la cola (`CampanaActivity.java:96-97,225-243`; `Campana.java:179-184`: OSCURO "no entra en la cola"). K × M a mano (`CampanaActivity.java:73-76`). El ZIP sí existe (APP-32). `06_Calibracion/cola_banco_P1-P132.csv` apareció durante esta revisión (fichero sin commit, 181 líneas, con INICIO/BATERIA/OSCURO/A5-INICIO/…/A5-FIN); **la app no lo lee** (Grep y `grep -rn` de `cola_banco` en `app/src`: nada) | T-A50, T-S01, T-C42 (`TDD-V3.6.md` §3 ter) |
| APP-34 "Calibrar este equipo" con un botón | **No** | El flujo son pasos manuales en `AdminActivity`: código `:95-101`, grado `:102-117`, Ajustar `:118`, Escribir `:119`, re-medida `:139`, aceptar `:140` | T-S07, T-S16, T-S18, T-C43 (`TDD-V3.6.md` §3 ter) |
| APP-35 Identidad por `#GN#` | **No** | La identidad sale del nombre Bluetooth o de una serie tecleada: `Sesion.serie()` `:157-164`; la campaña se abre con ella al conectar (`ConexionActivity.java:85-87`). `#GN#` se lee (`Pruebas.java:286-287`) y **sólo se muestra** (`Sesion.java:193`). Alta de serie con **una sola entrada** (`AdminActivity.java:148-149,887-909`). En campo: `#SN,SLV-02#` se grabó con `#OK#` (T4:2048-2056) y hubo que regrabar (T4:2069-2077) | T-A59, T-S03, T-S04, T-S05, T-C48 (`TDD-V3.6.md` §3 ter) |
| APP-36 Acta persistida y reanudable | **No** | El acta vive en memoria (`Sesion.java:78`); al reconectar `reiniciar` la pone a `null` (`Sesion.java:130`, llamado en `ConexionActivity.java:81`); sólo se escribe a disco al aceptar o rechazar (`AdminActivity.java:823,841`) | T-A60, T-S09, T-S10 (`TDD-V3.6.md` §3 ter) |
| APP-37 Recuperación de un corte durante `#S` (P9-B12) | **No** | `Cliente.pedir` lanza `IOException` si se pierde el enlace (`Cliente.java:112-139`); `escribir` no la captura y `op` sólo la convierte en texto (`AdminActivity.java:260-265`): ni relectura ni restauración. La restauración sólo corre si el equipo contesta algo distinto de `#OK#` o vence el plazo (`:685-690`) | T-A61, T-S08, T-C46 (`TDD-V3.6.md` §3 ter) |
| APP-38 Re-medida guiada, patrón de la app, K × M del protocolo, s_rep medida | **Parcial** | Re-medida guiada colocación a colocación con el protocolo del acta (`AdminActivity.java:736-811`), pero **el patrón lo elige el operador** (`:228-243,743-748`) y con K = 1 la s_rep es **supuesta**: `S_REP_REL · R` (`Acta.java:153,159`). En campo: "s_rep 14.9 (supuesta)" y "20.8 (supuesta)" (acta:14,21), que son 0,03 × 497,8 y 0,03 × 693,2 Además compara con el propio equipo y no con el certificado (D-19, `Acta.java:141-154`), y `Acta.remedida()` sobrescribe el intento anterior (D-20, `Acta.java:133`; T4:2156 NO CONFORME y :2234 CONFORME, sólo el segundo en el acta) | T-A53, T-A54, T-A55, T-S12, T-S23 (`TDD-V3.6.md` §3 ter) |
| APP-39 Importar visible arriba | **Parcial** | Existe, pero en la sección "Enviar", debajo del modo guiado y de los preajustes (`CampanaActivity.java:86-107`), y exige equipo conectado con MAC para abrir campaña (`:123-130,683-686`) | T-A68, T-S02 (`TDD-V3.6.md` §3 ter) |
| APP-40 Aviso de no desinstalar y copia en `Download/RTV/` | **No** | El ZIP va a `getFilesDir()/registros` (`Campanas.java:220-225`), que se borra al desinstalar. Grep y `grep -rniE` de `Download`, `DIRECTORY_DOWNLOADS`, `MediaStore`, `getExternal`, `desinstal` en `app/src/main`: nada | T-A69, T-A71 (`TDD-V3.6.md` §3 ter) |
| APP-41 Versión en el nombre del APK y en la cabecera | **Parcial** | `BuildConfig.VERSION_NAME` sólo en el texto de la pantalla principal (`ConexionActivity.java:43`); las demás pantallas llevan etiqueta fija (`AndroidManifest.xml:30-34`). Nombre del APK sin versión: `build.gradle` no tiene `outputFileName` ni `archivesBaseName` (Grep y `grep`: nada) | T-A62, T-A70 (`TDD-V3.6.md` §3 ter) |
| APP-42 Códigos 3, 4 y 6 ajustables con P1-P132 | **No** | Con el asset: 3 rango estrecho (164-173), 4 dos niveles, 6 sin patrones, 5 estrecho (83-85). La regla ya está (`Asistente.coberturaValores` `:75-98`): aplicada aquí a `patrones_certificados_P1-P132.csv` con un script que la reproduce, daría 3, 4 y 6 hasta grado 2 y **5 seguiría estrecho** (83-102, 19 % < 30 %). Cálculo, no medida. Falta cargar el catálogo (APP-31) | T-A56 (`TDD-V3.6.md` §3 ter) |
| APP-43 Aviso de batería baja con `9` | **No** | No se pide `9` salvo en detección y línea base (`Pruebas.java:305`, `LineaBase.java:71`); `Tramas` sólo extrae `:n:` (`Tramas.java:38,77-78`). Firmware: §1.1 | T-A57, T-S14, T-C44, T-C49 (`TDD-V3.6.md` §3 ter) |
| APP-44 `#V#` fresco tras `#S`, `#F`, `#SC` y antes de resumen y acta | **No** | `#V#` sólo se pide en `Pruebas.java:256`; `marca` y `mascara` sólo se escriben en `:262-263`. El acta copia `s.firmware()` al abrirse (`AdminActivity.java:729`) en un campo final (`Acta.java:26,40`). `Calibracion.estado` deduce "No calibrado" de esa marca (`Calibracion.java:141-142`). **En campo**: último `#V#` a las 11:46:56 (T4:13-18); `#S` código 1 en T4:1660, código 2 en `:2520`, `#SC` en `:2763`; ningún `#V#` después. Resultado: acta:3 "DEF mascara 0000" con los códigos 1 y 2 escritos, y resumen:3-4 "No calibrado (fecha 2026-09-19 pero #V# dice DEF …)" | T-A66, T-S20, T-C41 (`TDD-V3.6.md` §3 ter) |
| APP-45 Modo Avanzado separado | **No** | Pruebas, Medida, Campaña, Botones y Administrador en la misma lista (`ConexionActivity.java:45-51`). Grep de `Avanzado`/`avanzado`: nada | T-S07 (interfaz) (`TDD-V3.6.md` §3 ter) |
| APP-46 Persistencia dentro del flujo | **No** | No hay paso de apagar/encender. Reconectar vacía la sesión y el acta (APP-36). A mano se puede repetir "Pruebas" (`#V#`, `#G`, `#E`: `Pruebas.java:256,355,404`), fuera del acta | T-S07, T-C41 (`TDD-V3.6.md` §3 ter) |
| APP-47 Café y lila con el código 4, marcados | **No** | No existen en la app: 10 filas en P1-P132 (P68, P69, P83, P84, P98, P99, P113, P114, P128, P129), ninguna en el asset; `Fabrica.color` no tiene esos colores (`Fabrica.java:32-42`), así que `Asistente.deCodigo` (`:39-45`) no las casaría con ningún código. Grep y `grep` de `cafe`/`lila`: nada | T-A64 (`TDD-V3.6.md` §3 ter) |
| APP-48 x de oscuro y s_rep de la campaña, no tecleadas | **No** | Tecleadas: `X_OSCURO` `AdminActivity.java:127-128,468` (defecto 575, `Asistente.java:218`); `S_REP_REL` `:134-136,471` (defecto 0,03, `Asistente.java:234`); son las únicas asignaciones (Grep y `grep`). La campaña tiene `xOscuro()` (`Campana.java:191-194`) y sólo se usa para el ancla (`AdminActivity.java:489-499`); `A5.Resultado.sRepRel` (`A5.java:94`) no se usa. **En campo**: acta:13 y :20 "Oscuro (x = 575)" mientras acta:16 ancla en "x = 565.4" medida (serie S060) | T-A65, T-S06 (`TDD-V3.6.md` §3 ter) |

| RF | Estado 3.6.9 | Qué hay hoy, con evidencia | Prueba que lo cubrirá |
| :--- | :--- | :--- | :--- |
| CAL-35 Protocolo del banco | **Parcial** | K × M a mano, 1-10 × 1-30 (`CampanaActivity.java:440-451`), por defecto 3 × 3 (`:73-76`), fijo mientras hay acta (`:268-275`). El acta toma el K × M más frecuente de las series elegidas o 1 × 9 (`Campana.protocolo` `:233-265`, `AdminActivity.java:724-728`). En campo: acta:5 "1 colocaciones × 9" | T-A50, T-A67, T-S01, T-S17, T-S19, T-C42 (`TDD-V3.6.md` §3 ter) |
| CAL-36 Deriva A5 al inicio y al final | **No** | `A5.evaluar` compara la **última** serie A5 con la serie de campaña (`A5.java:43-49,59-92`): un puente, no inicio frente a fin | T-A51, T-S21, T-C42 (`TDD-V3.6.md` §3 ter) |
| CAL-37 Método por código fijado en configuración | **No** | El operador elige grado 1, 2 o recta anclada en cada ajuste (`AdminActivity.java:102-117,484-486`) | T-A52, T-S06 (`TDD-V3.6.md` §3 ter) |
| CAL-38 s_rep medida en los criterios | **No** | Criterios con `S_REP_REL` tecleada (`Asistente.java:234,301`; `AdminActivity.java:471`). Ver APP-48 | T-A53 (`TDD-V3.6.md` §3 ter) |
| CAL-39 Re-medida guiada | **Parcial** | Ver APP-38 | T-A54, T-S12, T-S23, T-C43, T-C47 (`TDD-V3.6.md` §3 ter) |
| CAL-40 Café y lila sólo verificar | **No** | Ver APP-47 | T-A64 (`TDD-V3.6.md` §3 ter) |
| CAL-41 Umbral de batería | **No** | Ver APP-43 y §1.1 | T-A57, T-S14, T-C44, T-C45, T-C49 (`TDD-V3.6.md` §3 ter) |
| CAL-42 Vencimiento que avisa y no bloquea | **Parcial** | `Calibracion.vencimiento` `:109-120`, estado "vencida" `:144-147`, mostrado como texto (`AdminActivity.java:218`, `CampanaActivity.java:222`, `Pruebas.java:288`, `Campanas.java:232`); nada bloquea (Grep de `vencid`: sólo `Calibracion.java`). No hay aviso explícito, y el estado queda falseado por el `#V#` rancio (APP-44) | T-A58, T-S22 (`TDD-V3.6.md` §3 ter) |
| CAL-43 Contenido añadido del acta | **Parcial** | Hoy trae: equipo y MAC, firmware (rancio), protocolo, método, curva `#G`, conformidad con nota, `#E`, oscuro (x tecleada), re-medida, estado y fecha `#SC` (`Acta.java:205-235`). No trae: serie `#GN#`, `#V#` final, x de oscuro medida, s_rep medida, batería, estado "antes" | T-A63, T-S07 (`TDD-V3.6.md` §3 ter) |

## 3. Recuento

| | Cumple | Parcial | No | Contradice | Total |
| :--- | ---: | ---: | ---: | ---: | ---: |
| RF-FW-01…30 (r1.1) | 27 | 2 | 1 | 0 | 30 |
| RF-APP-01…27 (r1.1) | 11 | 13 | 2 | 1 | 27 |
| **Subtotal r1.1** | **38** | **15** | **3** | **1** | **57** |
| r1.2 (RF-FW-31, RF-APP-28…32) | 2 | 4 | 0 | 0 | 6 |
| Firmware 3.6.2 sin RF (`#FT#`, `#SN/#GN`, `#SC/#GC`) | 3 | 0 | 0 | 0 | 3 |
| r1.3: RF-APP-33…48 | 0 | 3 | 13 | 0 | 16 |
| r1.3: RF-CAL-35…43 | 0 | 4 | 5 | 0 | 9 |
| **Total** | **43** | **26** | **21** | **1** | **91** |

Cambios de estado frente a la 3.6.4: **RF-APP-21** No → Parcial (existe `Acta.java`); **RF-APP-32**
Parcial → Cumple (cierre y huellas). Ninguno empeora. El resto de las filas antiguas conserva el estado
con las citas al día. "No" en r1.1: RF-FW-22, RF-APP-05 (aplazado), RF-APP-09. "Contradice": RF-APP-06.

## 4. Contradicciones

### 4.1 Abiertas (no se eligen)

| ID | Qué | Evidencia |
| :--- | :--- | :--- |
| C-01 | La `x` de la V3.6 no es la del firmware original | Sin cambios; con el original borrado no se cierra con `e` |
| C-30 | Fábrica para la temperatura | **Cerrada en código** por `#FT#` (`calibracion_v36.c:716-727`); **abierta en equipo**: nunca enviada |
| C-37 | Versiones indistinguibles por `#V#` | La 3.6.1 y la 3.6.2 dicen `"3.6"` (`calibracion_v36.h:16`) **y la misma fecha** (2026-09-19). La app las separa con `#GC#` (`Pruebas.java:279-293`, `Calibracion.variante` `:28-40`) |
| C-38 | Primer disparo bajo | Sin cambios |
| C-39 | Los XI no se ordenan por su certificado | Sin cambios |
| C-40 | P24 tiene tres candidatas | Sin cambios |
| C-41 | P32 duplicado | Sigue en el asset (`:41-42`) y también en P1-P132 (`:33-34`) |
| C-42 | Tres rangos de `x` "de uso" | `Asistente.java:161-162`; `calibracion_v36.c:316-317`; `Inversion.java:20,26`. El oscuro medido en campo es x = 565,4 (acta:16) |
| C-46 | Regla del disparo descolgado | **Cambió de forma**: la 3.6.9 usa máx(5 · 1,4826 · MAD ; **50**) (`Veredicto.java:38,187`); la SPEC pide suelo **30** (`SPEC-V3.6.md:752`), y su texto sobre C-46 (`:1319-1321`) describe todavía la regla de la 3.6.5 (suelo 1,5) |
| C-47 | C1 frente al rodeo de la app | Sin cambios (`ULP_S = 8`, `Ecuacion.java:93`) |

Cerrada en esta revisión: **C-36** (SLV-002 lleva la 3.6.2, §0.3).

### 4.2 Documento frente a código, encontradas en esta revisión

1. `CAMBIOS-V3.6.md:3-6` y `:163` dicen que SLV-002 lleva la 3.6.1 y que la 3.6.2 está "sin grabar"; el
   commit `78924ae`, `grabacion_V3.6.2_2026-09-19.log:20` y las respuestas `#GC#`/`#GN#` del equipo desde
   las 11:16 dicen lo contrario.
2. `SPEC-V3.6.md:289` sigue diciendo que `#FT#` está "en obra … sin commit"; está en `6a32ca3`.
3. `SPEC-V3.6.md:787-790` (RF-APP-32) dice que no hay cierre de campaña y que el ZIP va sin md5; los dos
   existen desde la 3.6.6 (§2, fila 32).
4. `REVISION-Arquitectura-P9-V3.6.md:504-505`: "seis clases: `OK (71 tests)`"; son nueve y 89, y sólo pasan
   con el directorio de trabajo en `app/` (§0.2).
5. `06_Calibracion/cola_banco_P1-P132.csv`, que el encargo da como fuente de la cola del banco, no existía
   al empezar esta revisión (Glob, `find`, `git log --all`: nada) y apareció después, sin commit. No está en
   ningún commit a la hora de cerrar esta matriz: no es todavía una fuente citable.
6. El encargo describe la pinza [0 ; 99] de `gui.c:353-355` como algo que "se arrastra a la vuelta
   siguiente": sólo pasa una vuelta de cada 201 (§1.1, punto 3).
7. Dentro del código: `CampanaActivity.java:42` dice "5 colocaciones x 3 disparos" para A5; la constante
   es 9 (`A5.java:23`). `Asistente.java:249` cita `calibracion_v36.c:588` para los límites de `#S`; en la
   3.6.2 están en `:316-317` y se aplican en `:697-698`.

## 5. Huecos

### 5.1 Bloquean que un acta diga la verdad (P8, calibración de otros equipos)

SLV-002 tiene acta aceptada (acta:23-24), pero con tres datos que no son del equipo:

1. **Firmware y estado rancios (RF-APP-44).** Acta:3 y resumen:3-4 dicen DEF y "No calibrado" con los
   códigos 1 y 2 escritos. Hay que releer `#V#` tras `#S`/`#F`/`#SC` y abrir el acta con ese dato.
2. **Oscuro tecleado (RF-APP-48).** El acta certifica "x = 575" junto a una recta anclada en x = 565,4.
3. **s_rep supuesta (RF-APP-38, RF-CAL-38).** La tolerancia de la re-medida sale de 0,03 · R, no de una
   medida (acta:14,21). Con K = 1 no puede ser de otra forma: falta fijar el protocolo (RF-CAL-35).
4. **Identidad (RF-APP-35).** La serie de campaña y acta sale del nombre Bluetooth; `#GN#` se lee y no se
   usa, y el alta de serie ya se equivocó una vez en campo.
5. **Acta volátil (RF-APP-36, 37).** Un corte o una reconexión a mitad de calibración pierde el acta, y un
   corte durante `#S` no restaura.
6. **Catálogo (RF-APP-31, 42, 47).** La app sigue con P1-P31; con P1-P132 se podrían ajustar 3, 4 y 6.
7. Siguen: C-38 (asentamiento), C-40 (P24), C-39 (orden de los XI), RF-FW-22 / C-47 (C1).

### 5.2 Bloquean una app de producción (no bloquean P8)

1. Flujo de campo: RF-APP-05 y las PAR aplazadas.
2. Batería (RF-APP-43, RF-CAL-41): fórmula en §1.1, sin una sola respuesta `:n:` medida.
3. Usabilidad: RF-APP-34, 39, 45, 46.
4. Datos que se pierden al desinstalar (RF-APP-40) y APK sin versión en el nombre (RF-APP-41, C-16).
5. `@` en lo recibido (C-31), longitud en `Cliente.pedir` (RF-APP-26), texto de `::0` (RF-APP-06).
6. Pruebas JVM que faltan: T-A07, T-A19, T-A32, T-A34, T-A35 y las de las filas r1.3.
7. Temperatura: `#FT#` sin probar en equipo (C-30) y sin sensor identificado (CA3).
