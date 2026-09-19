# Matriz SPEC ↔ código de la V3.6

**Nada de esta matriz está medido salvo lo que cita el acta de SLV-002 o `07 pruebas/19092026_0900/`.**
"Cumple" quiere decir **que el código hace lo que pide el requisito**, no que se haya probado en un
equipo. Leída el 19-sep-2026 entre las 09:30 y las 10:30.

## 0. Qué se ha leído

| Pieza | Versión | Cómo se fija |
| :--- | :--- | :--- |
| Firmware | **V3.6.1**, commit `8860445`, `.hex` md5 `8736c05d0273fdda66d5988f472d41e1` | md5 del blob del commit = md5 declarado en `hex/RetroVertical_V3.6.hex.md5` (comprobado); `md5sum -c hex/fuente.md5` sin fallos |
| App | **RTV 3.6.4**, commit `090c84c` (`versionName "3.6.4"`, `app/build.gradle`) | Fuente exportado con `git archive 090c84c` |
| App, sólo RF-APP-29, 30 y 32 | **RTV 3.6.5**, commit `ff66f93` (llegó durante esta revisión) | `git show ff66f93:…`; citada por clase y método porque sigue cambiando. Sus 68 pruebas JVM **no se han ejecutado en este trabajo** |
| Pruebas JVM de la app | 56 (`AsistenteTest` 12, `CalculoTest` 31, `CoherenciaRealTest` 7, `FabricaTest` 1, `ReceptorTest` 5) | **Ejecutadas en este trabajo**: `OK (56 tests)`, receta R-JVM con el classpath `../libtest/` (no `../build/libtest/`, como decía la TDD) |

Rutas: firmware relativo a `01_Firmware/RetroVertical_V3.6.X/`; app relativo a
`03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Las citas de `calibracion_v36.c` son
**del commit `8860445`**: en el árbol de trabajo hay a esta hora un `#FT#` sin commit (V3.6.2, otro
agente) que desplaza +12 líneas todo lo que va detrás de `:605`. No se ha leído ni se cuenta aquí.

**Dos hechos del encargo que no se sostienen tal cual** (se dejan escritos, con la evidencia):

1. *"SLV-002 lleva grabada la V3.6 de 2026-09-18."* Era cierto hasta las 09:36 del 19-sep. El commit
   `869d3c6` y `01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.1_2026-09-19.log` registran
   *Program Succeeded* a las 09:36:15, con memoria de programa hasta `0x1d07f`. Eso cuadra con el `.hex`
   de la 3.6.1 (último byte de datos en `0x1d009`, calculado aquí sobre el blob de `8860445`) y **no**
   con el de la 3.6 (`0x1bcad`; su grabación del 18-sep registró `0x1bcff`). El registro no guarda el
   md5 del fichero cargado y **nadie ha leído aún `#V#` tras esa grabación**: queda como
   contradicción abierta C-36 hasta que `#V#` responda.
2. *"Sin el primer disparo, s ≈ 5."* El número es correcto (s de los disparos 2 a 9: de 3,2 a 7,7
   cuentas), pero **no es ruido puro**: hay una subida que dura 3-4 disparos. Media de
   `x − mediana(disparos 2-9)` por posición, 17 series: −17,4 · −5,1 · −3,5 · −0,8 · +1,9 · +3,4 · +1,5
   · +2,4 · +3,2. El segundo disparo sigue 5,8 cuentas por debajo de la mediana del 3 al 9 (16 de 17
   series). Descartar **un** disparo, que es lo que hace la app 3.6.4 por defecto
   (`Sesion.java:59`), quita la mayor parte del sesgo, no todo. Ver T-C38.

## 1. Firmware (RF-FW)

| RF | Estado | Evidencia (`8860445`) | Prueba que lo cubre y estado |
| :--- | :--- | :--- | :--- |
| 01 Enlace 9600 | Cumple | `mcc_generated_files/` idéntico a la base (`diff -rq`, 19-sep, sin diferencias) | T-C05 parcial: el enlace funciona en SLV-002 (registros del 19-sep) |
| 02 Adquisición igual al fuente 2020 | Cumple (código) | `measurement.c:222-247`; único cambio `:212`; factor `gui.c:299-301` | T-C08: **falla por pantalla** en x = 1700-2300 (acta). Ver C-01 |
| 03 Códigos de medida | Cumple | `ecuacionesCalibracion.c:49-67,137-193` | T-C05 parcial (los 12 responden, acta G4) |
| 04 Evaluación bit a bit | Cumple | `calibracion_v36.c:229-234` (orden de 2020, no Horner) | T-A20 **pasa** (`pruebas/T-A20.md`, 786 432 casos, 0 diferencias, repetida con 3.6.1); `#E` 60/60 en SLV-002 |
| 05 Fábrica en ROM | Cumple | `calibracion_v36.c:34-47`; `gui.c:42-44`; `#F` copia de `coefFabrica`: `:593-605` | T-A20, T-C04 pasa (a ≤ 4 ulp) |
| 06 Negativos y saturación | Cumple (misma asignación que 2020) | `calibracion_v36.c:230-233` | T-A21 parcial |
| 07 `e` | Cumple | `ecuacionesCalibracion.c:191-193` | T-C06 parcial (funciona; 153 lecturas del 19-sep) |
| 08 `9` | Cumple | `gui.c:268` (`nivel[6]`) | T-C12 pendiente |
| 09 Byte no código | Cumple | `gui.c:296` | T-C13 pendiente |
| 10 STONE sin cambios | Cumple | `ecuacionesCalibracion.c:73-128`; `uart_stone.c:91-100` sólo añade el registro | T-C14 parcial (acta) |
| 11 Una tabla | Cumple | Las 12 funciones llaman a `aplicarEcuacion()`: `ecuacionesCalibracion.c:3-41` | T-C19 pendiente |
| 12 Defectos que se conservan | Cumple | `gui.c:296-343` | T-C15 pendiente |
| 13 Tramas `#` | Cumple | `uart_module.c:61-117` (96 bytes `:61`, descarte a 2 s `:113-114`) | T-C16, T-C17, T-C36 pendientes |
| 14 Límite de recepción | **Parcial** | UART1: `uart_module.c:87-100`. UART2 sin límite: `uart_stone.c:95-98` (O-11) | T-A22, T-C18, T-C34 pendientes |
| 15 Trama durante medida | Cumple | `uart_module.c:94-99,108-117` | T-C20 pendiente |
| 16 `#V#` | Cumple | `calibracion_v36.c:525-533`, máscara `:202-223`, fecha `:458-475`; versión `"3.6"` fija: `calibracion_v36.h:16` | T-C03 pasa (`#V,3.6,2026-09-18,DEF,0000#`). C-11 y **C-37** abiertas |
| 17 Sesión y PIN | Cumple | `:73-75,181-182`; bloqueo `:534-547,628-636`; caducidad `:298,337-341` | T-A31 declarada; T-C21, T-C22, T-C31 pendientes |
| 18 Lectura `#G`, `#GT` | Cumple | `:552-562,606-614` | T-C04 pasa |
| 19 Escritura y C4 | **Cumple (3.6.1; sin verificar en equipo)** | `#S` `:580-592`; `#ST` con C4 completa `:615-627`, `temperaturaValida` `:381-389`; deshacer `:485-495` | T-A30 **pasa** (5/5 y 240 aleatorios, `pruebas/T-A23_T-A30/resultado_T-A30.txt`); T-C24, T-C32 pendientes |
| 20 `#F` | Cumple | `:593-605`. No toca la temperatura: C-30 | T-C25 pendiente |
| 21 `#P` | Cumple | `:628-642`, `pinValido` `:442-449` | T-C26 pendiente |
| 22 Números exactos (C1) | **No** | `strtod` `:354-364`; `%.8E` `:451-455`. Ida y vuelta medida: hasta 5 ulp en simulador y 7 con la emulación (`CAMBIOS-V3.6.md` §7.2) | T-A23 cerrada como medida, **falla** el criterio de 0 ulp |
| 23 EEPROM y comunicaciones | Cumple (código) | `guardarEeprom` `:128-154`; `#OK#` después: `:485-488` | T-C35 pendiente |
| 24 Bloque EEPROM | Cumple | `:53-69,128-199` | T-A24 declarada; T-C23 pendiente |
| 25 Nunca `@` | Cumple | Sin `@` en `calibracion_v36.c`, `ecuacionesCalibracion.c`, `gui.c`, `uart_module.c` (Grep, dos herramientas) | T-C27 pendiente |
| 26 Configuración | Cumple | `device_config.c` idéntico (dentro de `mcc_generated_files/`) | T-C01 pasa (18 y 19-sep) |
| 27 Cadena de compilación (G1) | **Parcial** | `.gitattributes` con `*.hex -text`; blob = md5 declarado. `CAMBIOS-V3.6.md` no cita `8860445` (sólo `f75ff88`, de la 3.6) | T-A28 parcial |
| 28 `#E` | Cumple | `:563-579`, `leerEntero` `:425-436` | T-A27 parcial; 60/60 en SLV-002 |
| 29 `#K#`, `#KC#` | Cumple | `:236-292,643-650`; `uart_stone.c:91-100` | T-A33, T-C33 pendientes |
| 30 Defectos de memoria | Cumple | `measurement.c:212`, `gui.c:268`, `uart_module.c:100` | T-A29 pendiente |
| **31 Límites de `#S`** [r1.2] | Cumple | `:309-319` (600, 4300, 0, 4000), `curvaValida` `:404-422`, aplicado en `:587-588` | T-A30 pasa (casos `#S` de `CAMBIOS` §7.3); **T-A41** nueva |

## 2. App (RF-APP)

| RF | Estado | Evidencia (`090c84c`) | Prueba que lo cubre y estado |
| :--- | :--- | :--- | :--- |
| 01 Enlace y pausas | Cumple | `EnlaceSerie.java:33,157`; `Cliente.java:24-25,33,196-197` | T-A07 pendiente (sin reloj inyectable) |
| 02 Fin por silencio | Cumple | `Receptor.java:14-15,59`; `Cliente.java:27-28`; `#S`/`#F` a 5000 ms: `AdminActivity.java:480,522,567` | T-A03 pasa |
| 03 Detección | Cumple | `Pruebas.java:247,281,289,297` (`#V#`, `9`, `6`, `@LEERV`; nunca `e`) | T-A08 parcial; T-C03 pasa |
| 04 Mapa color × tipo | Parcial | Sólo para patrones: `Asistente.java:39-45` (tipo I → opaca) | Aplazado con PAR |
| 05 Medir en campo | No (aplazado) | — | T-B05 aplazada |
| 06 Registro de `x` | **Contradice** | Con `e`, `::0` se rotula "saturado o negativo": `LecturaX.java:64`. C-33 decidió "sólo saturación" | T-C06 parcial |
| 07 Coherencia | Parcial | `#G` a `ULP_G = 4`: `Ecuacion.java:92`, `Pruebas.java:339`; `#E` exacto contra la tabla de fábrica: `Pruebas.java:375-393`; deriva e INVÁLIDA: `Coherencia.java:136-241`. Falta la referencia "último juego escrito" en `CAL` | T-A10 pasa; `CoherenciaRealTest` 7/7; T-C09 a repetir con 3.6.4 |
| 08 Tabla de fábrica | Parcial | 48 coeficientes en `Fabrica.java`; **los 3 de temperatura no están** (Grep de `43212`, `90148`, `X_1`: sin resultados; comprobado con dos herramientas) | T-A01 parcial |
| 09 Repetibilidad, 10 lecturas | **No** | `Repetibilidad.java:13` (`LECTURAS = 5`), `:14` (umbral 10) | T-A11 parcial |
| 10 Emulación e inversión en `float` | Parcial | Emulación: `Ecuacion.java:56-68`. La inversión sigue en `double`/Horner: `Inversion.java:58` → `Ecuacion.java:26-28,41-48` (C-21) | T-A05 parcial |
| 11 Forma de las curvas | Parcial | `Asistente.validarForma` `:179-208`; sin informe de las 12 de fábrica | T-A06 parcial |
| 12 Otras comprobaciones | Parcial | `@` sólo en lo enviado: `Tramas.java:53-55` (C-31); sin oscuro guiado | T-C27 pendiente |
| 13 Sesión de patrones | Parcial | N lecturas y asentamiento: `MedidaActivity.java:49-51,134-162`; sin 3 × 5 recolocaciones, oscuro ni temperatura | T-C10 parcial (sesión del 19-sep) |
| 14 Cobertura | Cumple (3.6.4) | Por catálogo: `Asistente.java:74-118`; grado máximo = niveles − 2; rango estrecho no se ajusta `:33-36,93-95` | T-A12 pasa (`coberturaDelCatalogoConLosTipoI`, `rangoEstrechoNoSeAjustaAunqueHayaPuntos`) |
| 15 Ajuste | Parcial | `Asistente.java:280-384`: C2 `:347-349`, criterio de `#S` del firmware `:222-241,350-353`, recta alternativa `:354-366`. Falta "con y sin oscuro" y no mezclar métodos | T-A13, T-A14 pasan; T-A41 nueva |
| 16 "Como llegó" | Cumple | `Asistente.java:339-343` | T-A15 pendiente |
| 17 Escritura con restauración (C3) | Cumple | `AdminActivity.java:514-556`: `#S`, `#G` a `ULP_S = 8` (`Ecuacion.java:93`), `#E` en 5 puntos `:498-512`, restauración `:468-490` | T-A32 pendiente (lógica en la actividad) |
| 18 `#F` | Cumple | `AdminActivity.java:558-570` | T-C25 pendiente |
| 19 PIN | Cumple | `AdminActivity.java:33-34,138,226-290,295` | T-A19 pendiente |
| 20 Registro técnico | Cumple | `Registro.java` | T-A16 parcial |
| 21 Acta de calibración | **No** | No hay generador de acta (Grep de `acta` en `app/src/main/java`: sólo comentarios) | T-A17 parcial |
| 22 Copia de coeficientes | Cumple | `Sesion.java:229-230`; `AdminActivity.java:366,547` | T-C29 pendiente |
| 23 La app no escribe `#ST` | Parcial | No se ofrece; `Tramas.tramaST` limita sólo `X_0`: `Tramas.java:240-254` (el firmware 3.6.1 ya aplica C4 completa) | T-A37 parcial |
| 24 Botones de pantalla | Cumple | `BotonesActivity.java`; `Tramas.java` (analizador `#K`) | T-A36 pasa |
| 25 Línea base previa | Parcial y **ya no realizable** | `LineaBase.java:27` (`9` × 3, no 10). El firmware original de SLV-002 se borró el 18-sep | T-B03, T-B07, T-B09: no realizables |
| 26 Longitud según versión | Parcial | Por construcción; `Cliente.pedir` no mide longitud | T-A34 pendiente |
| 27 Estadístico y tolerancias | Parcial | `Asistente.puntos` `:136-158` filtra por código, no por método; tolerancias fijas `Coherencia.java:36`, `Repetibilidad.java:14` | T-A35 pendiente |
| **28 Asentamiento** [r1.2] | Parcial | `LecturaX.java:43-48`; `Sesion.java:59` (1 por defecto, 0-5: `MedidaActivity.java:134-144`); `e` inicial de la prueba 3: `Pruebas.java:434-438` | T-A38, T-C38 nuevas |
| **29 Disparo descolgado** [r1.2] | Parcial | 3.6.4: nada (`Asistente.puntos` promedia todo). 3.6.5 (`ff66f93`): `Veredicto.descolgados` con 5 · max(1,4826 · MAD ; 1,5), sin el suelo de 30 que pide la SPEC (C-46) | T-A39 nueva |
| **30 ¿Es este el patrón?** [r1.2] | Parcial | 3.6.4: nada. 3.6.5: `Veredicto` por orden de certificados (3 %) y parecido (±1 %) con otro patrón; `CampanaTest.p24MedidoEnRealidadSobreP20`, `lasDosSeriesRealesDeP24` | T-A40 nueva |
| **31 Patrones tipo I** [r1.2] | Parcial | 20 filas P32a-P50 en `assets/patrones_certificados_P1-P31.csv` (commit `090c84c`), P32 duplicado | T-A12 pasa; T-C39 nueva |
| **32 Modo Campaña** [r1.2] | Parcial | 3.6.5 (`ff66f93`), por clase: identificación serie + MAC (`Campanas.abrir`), diario de sólo añadir (`Campana`), ZIP único (`Campanas.exportar`), `ACTION_SEND` con un adjunto (`CampanaActivity`), importación por MAC (`Importador`). **Faltan el cierre explícito de campaña y el md5 del ZIP** (Grep de `md5`/`MessageDigest`: nada, dos herramientas) | T-A42 a T-A49, T-C40 nuevas |

## 3. Recuento

| | Cumple | Parcial | No | Contradice | Total |
| :--- | ---: | ---: | ---: | ---: | ---: |
| RF-FW-01…30 (r1.1) | 27 | 2 | 1 | 0 | 30 |
| RF-APP-01…27 (r1.1) | 11 | 12 | 3 | 1 | 27 |
| **Subtotal r1.1** | **38** | **14** | **4** | **1** | **57** |
| Nuevos r1.2 (RF-FW-31, RF-APP-28…32) | 1 | 5 | 0 | 0 | 6 |
| **Total** | **39** | **19** | **4** | **1** | **63** |

"No" en r1.1: RF-FW-22, RF-APP-05 (aplazado a propósito), RF-APP-09, RF-APP-21. "Contradice":
RF-APP-06.

**La SPEC r1.1 contradecía al código en cuatro sitios, y en los cuatro manda la evidencia**, así que
se corrige la SPEC (r1.2), sin contradicción abierta:

| r1.1 decía | El código hace | Por qué manda el código |
| :--- | :--- | :--- |
| RF-FW-19: "`#ST` sólo limita `X_0`" | C4 completa (`calibracion_v36.c:615-627`) | Es la 3.6.1; T-A30 pasa |
| RF-APP-07: aceptar hasta 2 ulp en `#G` | `ULP_G = 4`, `ULP_S = 8` (`Ecuacion.java:92-93`) | Medido: `%.8E` hasta 3 ulp, ida y vuelta hasta 7 (`CAMBIOS` §7.2). Con 2 ulp un chip recién grabado saldría NO APTO |
| RF-APP-14: "sólo 1 y 2 ajustables; ningún tipo I" | Ajustables 1, 2, 8 y b (`Asistente.java:74-118` con el catálogo nuevo) | Hay 20 patrones tipo I desde el 19-sep |
| RF-APP-03, párrafo "Decisión r1.1": "se mantiene `#V#`, `e`, `6`" | `#V#`, `9`, `6`, `@LEERV` (`Pruebas.java:247-297`) | El propio RF-APP-03 [MOD 18-sep] lo cambió por dato de campo; el párrafo quedó sin retirar |

## 4. Contradicciones abiertas (no se eligen)

Detalle en `SPEC-V3.6.md` §9 y en `ARQUITECTURA.map` (CA8-CA16).

| ID | Qué | Evidencia |
| :--- | :--- | :--- |
| C-01 | La `x` de la V3.6 no es la del firmware original | Acta: −80 a −89 cuentas en x = 1700-2300 por pantalla; en la zona alta, no concluyente. Con el original borrado no se puede cerrar con `e` |
| C-30 | No hay "fábrica" para la temperatura en la 3.6.1 | `#F` sólo coeficientes (`:593-605`); `#ST` con el texto de fábrica deja `CAL,1000` (`CAMBIOS` §7.3: `strtod` lee `4,32120039E-04` y `9,01486659E-01`). Un `#FT#` está en obra (3.6.2, sin commit) |
| C-36 | ¿Qué firmware lleva SLV-002 ahora? | §0, punto 1 |
| C-37 | La 3.6 y la 3.6.1 responden la misma versión | `calibracion_v36.h:16` = `"3.6"`; la app distingue por fecha de compilación ≥ 2026-09-19 (`Sesion.java`, `limitesS()`). Recompilar la 3.6.0 hoy la haría pasar por 3.6.1 |
| C-38 | Primer disparo bajo: ¿deriva o asentamiento? | 17 de 17 series; −17,4 de media en el primero, −5,1 en el segundo (§0). La prueba 4 de la 3.6.3 leía la diferencia `e` inicial-final como deriva (`Coherencia.java:16-18,140,217`; acta: 3004 → 3027). Causa sin medir: el filtro se reinicia en cada medida (`measurement.c:241`), así que no es estado arrastrado por el IIR |
| C-39 | Los XI no se ordenan por su certificado | Blanco: P7 (768) → x 3231-3237 > P6 (772) → 3115; amarillo: P23 (714) → 2723-2757 > P5 (740) → 2526-2584 y P8 (721) → 2518. Hipótesis de orientación, sin medir |
| C-40 | P24 tiene tres candidatas | Sesión 08:59: 2048-2076 y, 30 s después, 2438-2453 (Δ ≈ 370); la carpeta `p29_p24_p23_p5` sugiere que la serie de las 09:23:40, etiquetada P20 (2805), era P24. La recolocación normal del mismo patrón mueve 6-58 cuentas (P20, P21, P23, P5, P7) |
| C-41 | P32 duplicado | `P32a,9,I,azul` y `P32b,68,I,naranja` en el asset de `090c84c` |
| C-42 | Tres rangos de `x` "de uso" | App C2 200-4400 (`Asistente.java:161-162`); `#S` del firmware 600-4300 (`calibracion_v36.c:316-317`, el 600 "no sale del código"); inversión 180-4400 (`Inversion.java:20,26`). El oscuro de la V3.6 está en x ≈ 575 (acta, G4) |
| C-47 | C1 (ida y vuelta exacta) frente al rodeo de la app | T-A23: hasta 7 ulp. App: `ULP_S = 8` y `#E` en 5 puntos ±1 tras `#S`, con restauración. Lo decide la revisión de arquitectura |
| C-46 | Regla del disparo descolgado | RF-APP-29: max(30, 5 · 1,4826 · MAD). La de `Veredicto` (3.6.5, `ff66f93`): 5 · max(1,4826 · MAD ; 1,5), que marca el 2004 de P2 (09:17:21, umbral 11,1), un efecto de asentamiento y no un descolgado |

## 5. Huecos

### 5.1 Bloquean la calibración (P8)

1. **Primer disparo y asentamiento (C-38).** Hay que fijar cuántos disparos se descartan con datos
   (T-C38), porque el segundo sigue sesgado unas 5 cuentas y la opción C ajusta sobre medias de `x`.
2. **Disparo descolgado.** Una lectura como la de P7 (3031 frente a una mediana de 3230) entra hoy en
   la media sin aviso. Falta la regla (RF-APP-29, T-A39).
3. **Identidad del patrón (C-40).** P24 tiene tres candidatas (2048-2076, 2438-2453 y, si la carpeta
   `p29_p24_p23_p5` dice bien, la serie de las 09:23:40 etiquetada P20, 2805). Ninguna entra en el
   ajuste sin decidir cuál es P24 (RF-APP-30, T-A40). El cálculo de la opción C de este documento dejó
   fuera las series de P24 y contó la de las 09:23:40 como P20.
3 bis. **Campaña (RF-APP-32).** La 3.6.5 (`ff66f93`) ya tiene diario de sólo añadir y ZIP único;
   faltan el cierre explícito de campaña y el md5 del ZIP.
4. **Curva C del código 2 con el límite de `#S`.** Con los datos del 19-sep (medias sin el primer
   disparo, P24 fuera), el ajuste de grado 2 del amarillo da R(600) ≈ −60 y el firmware 3.6.1 lo
   rechazaría. El de grado 1 pasa: R(600) ≈ 81. El blanco pasa en grado 1 y en grado 2. Cálculo de
   este trabajo en `double`, no medida. La app 3.6.4 ya lo detecta y propone la recta
   (`Asistente.java:350-366`). Queda **decidir el grado** con los datos definitivos (T-A41).
5. **Orden de los XI (C-39).** Con la opción C, el error residual XI calculado aquí es de −7 a +5 % en
   el amarillo y de −3 a +3 % en el blanco. Si la causa es la orientación, se reduce midiendo, no
   ajustando.
6. **Firmware del equipo (C-36, C-37).** Antes de escribir, `#V#` tiene que decir qué lleva SLV-002, y
   hay que saber si es la 3.6.1 (límites de `#S`) o la 3.6.2 (`#FT#`).
7. **Acta de calibración (RF-APP-21).** No existe en la app; P8 exige acta de antes y después.
8. **Repetibilidad a 10 lecturas (RF-APP-09)** y estadístico sin mezclar métodos (RF-APP-27).
9. **Opacas con tipo I (T-C39).** Sin medir. Con las curvas de fábrica, verde (6-7) y azul (7-10) caen
   en x ≈ 518-589, por debajo del 600 de `#S` y en el oscuro de la V3.6 (x ≈ 575): no se pueden
   ajustar, sólo verificar. Rojo (46-81, x ≈ 994-1198) y amarillo (64-122, x ≈ 923-1467) sí tienen
   rango. Todo esto es estimación con las curvas de fábrica, que en las intensas ya se ha visto que no
   valen para SLV-002. Y P32 está duplicado (C-41).
10. **C1 sin cumplir por la letra (RF-FW-22), C-47.** La app lo rodea con `ULP_S = 8` y `#E` tras `#S`.
    Si eso sustituye a C1 lo decide la revisión de arquitectura, no este documento.

### 5.2 Bloquean una app de producción (no bloquean P8)

1. Flujo de campo entero: RF-APP-05 y las PAR aplazadas (proyectos, catálogo de señales, GPS, correo).
2. Detección de versión sin ambigüedad: C-37 (pedirle al firmware una cadena de versión distinta).
3. `@` en lo recibido (C-31) y límite de longitud explícito en `Cliente.pedir` (RF-APP-26).
4. Pruebas JVM que faltan: T-A07 (ritmo), T-A19 (PIN), T-A32 (restauración), T-A34, T-A35.
5. Texto de `::0` con `e` (RF-APP-06, contradice).
6. App del cliente de SLV-002 (T-C30): sin ella no se puede afirmar que "sigue funcionando".
7. Temperatura: sin sensor identificado (CA3) y sin `#FT#` en un firmware grabado (C-30).
8. APK sin atar a un commit (C-16): los APK no se versionan y ninguno lleva md5 en un commit.
