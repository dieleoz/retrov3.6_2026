# CAMBIOS de la V3.6 frente a la base de 2020

**Estado: SLV-002 lleva la V3.6.1 (§7, md5 `8736c05d…`) desde el 19-sep-2026 a las 09:36** (fuente en
el commit `8860445`, acta de grabación en `869d3c6`). **La V3.6.2 (§8) está compilada y probada sólo en
simulador (MPLAB SIM); no se ha grabado en ningún equipo.** Lo que exige el equipo delante está en la
sección 5 y queda pendiente.

**Primero, lo que no cuadra:** la ida y vuelta de un coeficiente por `#S` → `#G` **puede superar los
5 ulp** que tolera la app 3.6.2 (`Ecuacion.ULP_S`): medido en simulador hasta 5 ulp, y la emulación
validada bit a bit contra el simulador llega a 7 (§7.2). La cifra "±2 ulp al imprimir" de §3.4 también
queda corta: son hasta 3.

- Base: `01_Firmware/base_2020_d089f962/RetroVertical1.X/`, `.hex` md5 `d089f9625090a1213291c090eae7ac01`.
- Compilador: XC8 **v2.10** (`C:\Program Files (x86)\Microchip\xc8\v2.10\bin\xc8-cc.exe`, build Jul 30 2019),
  con los flags del proyecto (`nbproject/Makefile-default.mk`), sin `-mdfp` (usa el soporte de dispositivo
  interno del compilador, como en 2020).
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`, **revisión 1.1 (§4 bis)**, que manda sobre la 1.0.

## 1. Reproducibilidad de la base

La base, copiada sin cambios a un temporal y compilada con XC8 2.10 y el makefile del proyecto, da un
`.hex` **idéntico**: 0 direcciones distintas en la imagen de memoria (31 166 bytes en
0x0-0x3, 0x8-0xB, 0x18-0x1D, 0xFEC8-0x1785D, IDLOC 0x200000-0x20000F, CONFIG 0x300000-0x300009), y el
propio texto del `.hex` tiene el mismo md5 `d089f962…`. Lo único cambiado en la copia fue
`nbproject/Makefile-local-default.mk` (rutas de MPLAB X 5.35 → 5.50), que no aporta flags de compilación.

## 2. Diff funcional, fichero a fichero

Números de línea de la V3.6 salvo que se diga "base".

| Fichero | Líneas | Cambio | Efecto |
| :--- | :--- | :--- | :--- |
| `calibracion_v36.c` | nuevo | Tabla de fábrica, EEPROM, ecuación general, órdenes `#...#`, registro STONE | Ver §2.1 |
| `calibracion_v36.h` | nuevo | Índices `EC_*` (orden `1`-`8`, `a`-`d`), prototipos | — |
| `ecuacionesCalibracion.c` | 3-42 | El cuerpo de cada una de las 12 funciones pasa a `aplicarEcuacion(EC_...)`. Nombres, firmas y llamadas (`:70-198`) iguales | Gatillo/STONE y Bluetooth usan la misma tabla en RAM (RF-FW-11) |
| `ecuacionesCalibracion.h` | 15 | `#include "calibracion_v36.h"` | — |
| `gui.c` | 41-44 | `X_2`, `X_1`, `X_0` dejan de ser `static`, con los mismos literales | Se pueden cargar de EEPROM; si no hay registro válido valen lo de 2020 |
| `gui.c` | 268 | `char nivel[2]` → `nivel[6]` | Defecto de memoria: `sprintf("%u")` con dos o más cifras (`9` con batería alta) escribía fuera. No cambia ningún byte emitido |
| `measurement.c` | 212 | `tempSamples[15]` → `[16]` | Defecto de memoria, ver §2.2. No cambia el cálculo |
| `main.c` | 11, 20 | `calibracionIniciar()` tras `SYSTEM_Initialize()` | Carga EEPROM antes del bucle |
| `uart_module.c` | 52-121 | Recepción: `#` abre trama en `tramaAdmin[100]` (máx. 96 bytes), cierre con `#`, caducidad 2 s, descarte silencioso de tramas largas; bytes sueltos en `bufferData[50]` con el límite comprobado (`:100`) | O-01, O-05, RF-FW-13/14 |
| `uart_module.c` | 169-170 | `atenderAdmin()` y `adminTick()` en `executeUartModule()` | Atiende la trama al completarse; caducidad de la sesión |
| `uart_stone.c` | 87, 89-101 | `readUartStr2()` copia cada byte al registro de `#K#` antes de guardarlo como en 2020 | `bufferPantalla`, `bufferindice` y los códigos 0x01-0x0E no cambian |
| `nbproject/*` | — | Proyecto `RetroVertical_V3.6`, artefacto `RetroVertical_V3.6.X.production.hex`, `calibracion_v36.c/.h` añadidos; rutas de MPLAB X 5.50 en `Makefile-local-default.mk` | Mismos flags de compilación |

**Sin tocar:** `mcc_generated_files/` completo (incluido `adcc.c:159` y `device_config.c`),
`measurement.c` salvo la línea 212, y el resto de `gui.c`. **Bits de configuración idénticos** en el `.hex`:
`EC FF F7 FF 9F FF FF DF FE FF` (0x300000-0x300009), igual que la base y que lo leído en SLV-002; IDLOC igual.

### 2.1 `calibracion_v36.c`

- **Tabla de fábrica** (`:34-47`): los 48 coeficientes con **el mismo texto literal** que
  `ecuacionesCalibracion.c:3-42` de la base; un término restado en 2020 aparece con signo menos; el grado
  que falta es `0.0`.
- **Ecuación** (`aplicarEcuacion`, `:229-234`): `((c3·x·x·x + c2·x·x) + c1·x) + c0` en `double` de 32 bits,
  mismo orden de operaciones que 2020, no Horner; la conversión final a `unsigned int` es la misma
  asignación que en 2020. Resultado en simulador: bit a bit igual a 2020 (§3.1).
- **EEPROM** (`:53-223`): cabecera `V36`+versión 1 en 0x100; un registro de 18 bytes por código en
  `0x104 + 18·k` (4 `float` little endian + CRC-16/CCITT-FALSE, byte bajo primero) y otro en 0x1DC para
  `X_2, X_1, X_0` y el PIN. Cabecera mala: todo de fábrica. CRC malo: sólo ese registro de fábrica.
  Escritura sólo de los bytes que cambian, relectura completa y, si no cuadra, la RAM vuelve al valor
  anterior y se responde `#ERR,EEPROM#` (RF-FW-19). Nada por debajo de 0x100.
- **Máscara de `#V#`** (`:202-223`): bit *k* = coeficientes del código *k* distintos (bit a bit) de los de
  fábrica; bit 12 = factor de temperatura distinto del de `gui.c:41-44`. `CAL` si la máscara no es 0.
- **Órdenes** (`adminProcesarTrama`, `:501-654`): `#V#`, `#L#`, `#Q#`, `#G#`, `#E#`, `#S#`, `#F#`,
  `#GT#`, `#ST#`, `#P#`, `#K#`, `#KC#`. Ninguna mide. Números con `%.8E`. PIN de fábrica `2026`;
  5 fallos seguidos de `#L` o `#P` bloquean `#L` y `#P` hasta apagar; la sesión caduca a los 10 min sin
  tramas `#`. `#ST` y `#S` rechazan con `#ERR,FORMATO#` lo que salga de los límites de §7.1 (en la V3.6
  `#ST` sólo miraba `X_0`).
- **Registro STONE** (`:237-292`): últimas 8 tramas de UART2 (hasta 16 bytes cada una) y contador total
  desde el arranque. Una "trama" es lo que lee una llamada a `readUartStr2()` (`uart_stone.c:89`), que
  espera 20 ms antes de leer (`uart_stone.c:107`, base `:100`). `#KC#` vacía las ranuras; el contador no.
  La respuesta `#K#` puede llegar a ~280 bytes: las respuestas no tienen límite de longitud.

### 2.2 `tempSamples[15]` (corrección de seguridad de memoria)

`measurement.c:212` (base) declara `static unsigned int tempSamples[15]`; el bucle de `:228-231` escribe
los índices 0-15 y el de `:234-236` lee hasta el 15. En el `.map`/`.lst` de la base reconstruida,
`acquireReflectivity15Adc@tempSamples` ocupa 0x352-0x36F (bssBANK3) y **0x370-0x371** son los dos primeros
bytes de un bloque `ds 20` **sin símbolo**: uno de los 14 arrays estáticos de 20 bytes declarados y nunca
usados en `taskUartRx` (`uart_module.c:76-86` base) y `taskPantallaRx` (`uart_stone.c:149-159` base).
En 2020 se pisaba memoria muerta; en la V3.6 cambia el reparto de RAM y podía pisar otra cosa. Con `[16]`
la media sigue siendo la de las muestras 1-15: el cálculo no cambia.

## 3. Qué se probó y cómo

Todo en MPLAB SIM (`mdb.bat` de MPLAB X 5.50, `hwtool SIM`), con programas de prueba compilados con
XC8 2.10 y los flags de float/double del proyecto. **Ninguna prueba toca el PICkit.**

### 3.1 T-A20: fábrica = 2020, bit a bit

Tabla y método en [`pruebas/T-A20.md`](pruebas/T-A20.md). Resumen: 12 códigos × x = 0-65535, las 12
funciones de 2020 copiadas literalmente de la base frente a `coefFabrica` + `aplicarEcuacion` copiados
literalmente del fuente V3.6: **0 diferencias** en el `unsigned int` resultante.

### 3.2 Prueba funcional de `calibracion_v36.c`

El fuente real incluido tal cual, con dobles de UART, EEPROM (array RAM) y `getMillis()`. Resultados
comprobados: `#V#` → `#V,3.6,2026-09-18,DEF,0000#`; `#G`, `#GT`, `#E` (`#E,1,1000#` → 230,
`#E,2,4300#` → 0 por el corte de 4000); rechazos `FORMATO` (`abc`, `nan`, `k` inválido, x > 65535,
`X_0` fuera de rango); `BLOQUEADO` sin sesión; `#S` → `CAL` con máscara `0001`; persistencia tras
reinicio; CRC corrupto en un registro → sólo ese código a fábrica; cabecera borrada → `DEF` y PIN 2026;
bloqueo tras 5 fallos (de `#L` y de `#P`); caducidad a los 10 min. Los CRC del volcado de EEPROM se
recalcularon fuera (Python, CRC-16/CCITT-FALSE, `"123456789"` → 0x29B1) y cuadran los 13 registros.

### 3.3 Descarte de trama `#` incompleta y trama en la ventana de `clearBuffer()`

El descarte no depende de `taskUartTimeout` (sigue comentada en `uart_module.c:172`, como en 2020):
lo hace `atenderAdmin()` (`uart_module.c:108-117`), una función normal, no un protothread, llamada en
cada vuelta del bucle principal desde `executeUartModule()` (`uart_module.c:169`, `main.c:37`). Compara
`getMillis() - adminTiempo > 2000` (resta sin signo, segura ante el desbordamiento del contador).
Ritmo: una comprobación por vuelta del bucle; la vuelta se alarga en los tramos bloqueantes de 2020
(`__delay_ms(20)` de `uart_stone.c:107`, bucle de 600 conversiones de `measurement.c:242`).

Prueba en simulador con el `uart_module.c` real (`pruebas/T-RX_harness_rx.c`; UART1 y reloj simulados):
`#` suelto, a 1,9 s la trama sigue abierta (`adminIndex` = 1); a 2,5 s se ha descartado, y un `1`
posterior entra en `bufferData` (`bufferIndex` = 1, `bufferData[0]` = `'1'`), que es la condición de
medida de 2020 (`gui.c:295`, sin cambios). Trama `#V#` que llega partida alrededor de un
`clearBuffer()` con un byte suelto pendiente, y trama `#V#` en la ventana de 500 ms de `gui.c:342-346`:
en los dos casos llega entera a `adminProcesarTrama` y `bufferIndex` queda en 0 (no dispara medida).
Un byte suelto en esa ventana sigue disparando otra medida, como en 2020.

### 3.4 Formato numérico: **no cumple O-02** (ver §4.1)

120 valores en simulador: los 35 literales distintos de fábrica (coeficientes y temperatura) y 85 aleatorios entre 1e-9 y 1e5, de los dos signos:

| Conversión | Exacta | ±1 ulp | ±2 ulp | ±3 ulp |
| :--- | ---: | ---: | ---: | ---: |
| `sprintf("%.8E")` de XC8 → valor | 60 | 41 | 19 | 0 |
| texto exacto de 9 cifras → `strtod` de XC8 | 68 | 49 | 2 | 1 |

## 4. Desviaciones del contrato y observaciones

1. **O-02 no se cumple con la biblioteca de XC8 2.10.** `printf %.8E` se equivoca hasta en 2 ulp y
   `strtod` hasta en 3: ninguna de las dos redondea correctamente a 9 cifras. Consecuencia: un `#S`
   seguido de `#G` puede diferir más de 1 ulp y la comparación de la app fallaría. **No afecta a la
   medida con los valores de fábrica** (salen de la ROM, sin texto) ni a `#F`. Arreglo propuesto:
   conversión exacta propia (comparación entera con el punto medio entre floats vecinos), sólo en la ruta
   de administración.
2. **Trama `#` demasiado larga:** se descarta sin responder, como dice el contrato.
3. **Negativos (RF-FW-06):** no se ha hecho "explícita" la conversión; se conserva la misma asignación
   `double` → `unsigned int` que 2020 y el simulador da el mismo resultado (p. ej. `2` con x = 4300: 65360).
4. **`#ST` (corregido en la V3.6.1, §7.1):** en la V3.6 sólo se limitaba `X_0`; `X_1` y `X_2` sólo se exigían finitos. Un `X_1` extremo podía
   anular las medidas.
5. **No bloqueantes, anotados:** el anillo RX de `uart1.c` es de 64 bytes (`UART1_RX_BUFFER_SIZE`,
   `uart1.c:57`) y una trama de 96 puede desbordarlo si el bucle principal está bloqueado más de ~65 ms
   (bucle de 600 conversiones de `measurement.c:242`, `__delay_ms(20)` de `uart_stone.c:107`, escrituras de
   EEPROM de ~4 ms/byte); y un `#` suelto se traga los bytes que lleguen durante 2 s.
6. **Escritura de EEPROM:** cada byte deja las interrupciones apagadas hasta acabar
   (`memory.c:181-191`). El primer `#S` sobre un chip en `DEF` escribe cabecera y los 13 registros
   (~238 bytes): del orden de 1 s sin interrupciones. Sin medir.

## 5. Lo que no se ha podido probar (requiere el equipo)

- Nada ha corrido en un PIC real: tiempos, UART física, Bluetooth, pantalla STONE.
- La `x` de la adquisición (sólo se razona: `measurement.c` sólo cambia en la línea 212).
- Escritura real de la EEPROM de datos, su duración y su efecto sobre `getMillis()` y la UART.
- Tramas `#` de 96 bytes por Bluetooth real (anillo de 64 B, §4.5).
- `#K#` con la pantalla de SLV-002.
- Comportamiento con `CP = ON` al grabar: el borrado previo deja la EEPROM a 0xFF, es decir en `DEF`.

## 6. Entregable

**V3.6.2 (19-sep-2026, vigente, sin grabar):** `hex/RetroVertical_V3.6.hex`, md5
`9d5d5e3951c8aa83d1465f16f3733d27`. Añade `#FT#`, `#SC#`/`#GC#` y `#SN#`/`#GN#` (§8). ROM 57 009 B
de 131 072 (43,5 %), RAM 2 464 B de 8 192 (30,1 %), pila 11/16. Configuración e IDLOC iguales. Detalle en
`hex/memoria.txt`; md5 de cada fuente en `hex/fuente.md5` (sólo cambia `calibracion_v36.c`).

*V3.6.1 (19-sep-2026, la que lleva SLV-002 desde las 09:36; fuente `8860445`, acta `869d3c6`):* md5
`8736c05d0273fdda66d5988f472d41e1`. ROM 54 020 B de 131 072 (41,2 %), RAM 2 418 B de 8 192 (29,5 %), pila
hardware estimada 11 niveles en `main` y 16 con interrupción (igual que la base). Bits de configuración
e IDLOC idénticos a la V3.6 (`EC FF F7 FF 9F FF FF DF FE FF`). `#V#` sigue respondiendo `3.6`
(`FW_VERSION_STR`, `calibracion_v36.h:16`); lo que distingue la 3.6.1 es la fecha de compilación
(`#V,3.6,2026-09-19,...#`).

*V3.6 (18-sep-2026; SLV-002 la llevó hasta el 19-sep):* md5 `680b6a7d3a387ccddf066a2ebc0899d1`, commit `f75ff88`.
ROM 49 060 B (37,4 %), RAM 2 373 B (29,0 %). Se recupera de git.

## 7. V3.6.1: límites de `#ST` y `#S` (C4) y medida de la ida y vuelta (C1)

Sólo cambia `calibracion_v36.c`. Nada más del firmware se ha tocado: ni la tabla de fábrica, ni
`aplicarEcuacion`, ni la EEPROM, ni el formato numérico. Probado sólo en simulador.

### 7.1 Qué cambia

| Líneas | Cambio |
| :--- | :--- |
| `:20` | `#include <math.h>` (para `sqrt`) |
| `:301-319` | Constantes de los límites, con su origen en el código |
| `:366-422` | `esFinito`, `factorEnRango`, `temperaturaValida`, `respuestaEnRango`, `criticoEnRango`, `curvaValida` |
| `:587-588` | `#S`: `curvaValida(v)` antes de tocar RAM o EEPROM; si falla, `#ERR,FORMATO#` |
| `:620-621` | `#ST`: `temperaturaValida(v)` sustituye a la comprobación de sólo `X_0` |

**`#ST` (C4).** Se rechaza todo `(X_2, X_1, X_0)` con `F(T) = X_2·T² + X_1·T + X_0` fuera de
[0,5 ; 1,5] o no finito en algún `T` de 0 a 831. Se evalúa en `T = 0`, `T = 831` y en el vértice
`-X_1/(2·X_2)` si cae dentro; una parábola no tiene otros extremos. La forma de la evaluación es la de
`gui.c:301`. Origen de 831: `T` = filtro del ADC / 4,928 (`measurement.c:72-73`), con ADC de 12 bits,
4095/4,928 = 830,97.

**`#S` (nuevo).** Se rechaza toda curva `R(x) = c3·x³ + c2·x² + c1·x + c0` con algún valor no finito,
negativo o mayor que 4000 en algún `x` de 600 a 4300. Se evalúa en los dos bordes y en las raíces reales
de `R'(x) = 3·c3·x² + 2·c2·x + c1` que caen dentro (forma estable de la raíz, sin cancelación); un
polinomio de grado 3 no tiene otros extremos. El orden de operaciones es el de `aplicarEcuacion`. De
dónde sale cada cifra:

- **4000:** `arreglar_dato()` pone a 0 todo resultado mayor que 4000 (`ecuacionesCalibracion.c:49-54`).
- **0:** un `R` negativo, al convertirse a `unsigned int` en `aplicarEcuacion`, da la vuelta a más de 4000
  y también sale 0 (T-A20: código `2` con x = 4300 da 65360).
- **4300:** `x = (ADC filtrado + 200) · F(T)` (`measurement.c:247`, `gui.c:301`); con el ADC a fondo,
  4095 + 200 = 4295.
- **600:** **no sale del código**. Es el límite inferior que fijó el coordinador el 19-sep-2026.

Los coeficientes ya se exigían finitos (`leerNumero`, `:354-364`). `#F` no pasa por estos límites:
repone la ROM.

**Consecuencia que hay que conocer:** la curva de fábrica del código `2` (amarillo intenso) **no pasa**
el límite de `#S`: es negativa desde x = 4175 (emulación `float32`, barrido entero de 600 a 4300; en el
simulador, `#S,2,<fábrica>#` → `#ERR,FORMATO#`). Las otras once pasan. No afecta a `#F,2#` ni a la ROM,
y la app ya repone fábrica con `#F` y no con `#S` (RF-APP-18). Pero una calibración nueva del `2` con
la misma forma que la de fábrica sería rechazada.

### 7.2 T-A23: ida y vuelta `#S` → `#G` (C1)

Método en `pruebas/T-A23_T-A30/`. Texto `%.8E` de la app → `leerNumero()` (`strtod`) → `float` →
`enviarNumero()` (`%.8E`) → texto que lee la app, con el `calibracion_v36.c` real en MPLAB SIM y XC8 2.10
con los flags del proyecto. Error con la métrica de la app: `|leído − enviado| / Math.ulp(leído)` en
`float32` (`Ecuacion.java:96-104`).

| Lote | Valores | 0 ulp | 1 | 2 | 3 | 4 | 5 | Máx. |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1: 51 de fábrica (35 distintos) + 200 aleatorios | 251 | 90 | 96 | 52 | 13 | 0 | 0 | **3** |
| 2: 51 de fábrica + 1965 aleatorios | 2016 | 648 | 928 | 356 | 77 | 6 | 1 | **5** |

Aleatorios en el rango de calibración de grado 1 y 2: `c2` uniforme en ±1e-3 y log-uniforme de 1e-7 a
1e-3, `c1` de 0 a 2, `c0` de ±1000. Por separado, sobre los 2267 valores: `strtod` hasta **3 ulp** y
`%.8E` hasta **3 ulp** (un caso; §3.4 decía 2). `#G` de los valores de ROM (sin `strtod`): hasta 2 ulp.

**Emulación, segunda fuente.** El código de `strtof` y de `efgtoa` de la biblioteca de XC8 2.10
(`pic/sources/c99/common/strtof.c` y `doprnt.c`) reproducido en Python con `float32` (`emul.py`) da **los
mismos bits que el simulador en 2267 de 2267 valores, en las dos direcciones**. Para cuadrar hizo falta
un detalle: la conversión `uint32` → `float` de XC8 redondea el medio hacia arriba, no al par (con
redondeo al par discrepan 26 de 2267). Con la emulación, 600 000 valores de 1e-9 a 1e4 y 60 000 por
rango:

| Rango | Máx. ida y vuelta (ulp) | Casos > 5 |
| :--- | ---: | ---: |
| `c3` log-uniforme 1e-9 a 1e-6 | **7** | 73 de 240 000 |
| `c2` log-uniforme 1e-7 a 1e-3 | **6** | 5 de 240 000 |
| `c2` uniforme ±1e-3 | 5 | 0 |
| `c1` de 0 a 2 | 5 | 0 |
| `c0` de ±1000 | 4 | 0 |
| Sólo `strtod` / sólo `%.8E`, barrido de 600 000 | 4 / 3 | — |

Por qué: `strtof` junta 9 cifras en un entero y luego divide por 10 una vez por cada posición decimal
(hasta 9 divisiones en `float32`, variante `SMALLCODE`), y `efgtoa` busca la potencia de 10 multiplicando
o dividiendo por 10 y vuelve a dividir por 10 en cada cifra. El error crece con el número de pasos, sobre
todo con exponentes de -6 a -9.

**Tolerancias de la app (`Ecuacion.java:87-88`, sin cambios en la 3.6.3):**

- `ULP_G = 4` (`#G` frente a fábrica): **vale.** El error sólo de impresión no ha pasado de 3 ulp en
  600 000 casos.
- `ULP_S = 5` (`#G` tras `#S`): **no vale como cota.** Basta para `c2` uniforme, `c1` y `c0` (máx. 5, justo
  en el límite), pero se supera con `c2` del orden de 1e-7 a 1e-6 (6) y con `c3` (7). Superarlo no
  corrompe nada: la app da el `#S` por fallido y restaura lo anterior (`AdminActivity.java:471-497`).
  Propuesta, a decidir fuera de este documento: `ULP_S = 8`, o conversión exacta en el firmware (P-10).
- La restauración con `ULP_S + ULP_G` = 9 queda por encima de 7.

### 7.3 T-A30 y límites de `#S` en simulador

`calibracion_v36.c` real, tramas por `adminProcesarTrama`, EEPROM simulada en RAM
(`pruebas/T-A23_T-A30/resultado_T-A30.txt`):

| Trama | Caso | Respuesta |
| :--- | :--- | :--- |
| `#ST,0.00000000E+00,4.32119996E-04,9.01486516E-01#` | fábrica, dentro | `#OK#` |
| `#ST,0,4.3E-04,4.9E-01#` | `X_0` fuera | `#ERR,FORMATO#` |
| `#ST,0,1.0E-03,9.0E-01#` | `X_1` extremo, F(831) = 1,731 | `#ERR,FORMATO#` |
| `#ST,0,-5.0E-04,9.0E-01#` | `X_1` extremo, F(831) = 0,485 | `#ERR,FORMATO#` |
| `#ST,1.0E-06,0,9.0E-01#` | `X_2` extremo, F(831) = 1,591 | `#ERR,FORMATO#` |
| `#ST,-4.0E-06,3.4E-03,9.0E-01#` | sólo el vértice (1,6225) se sale | `#ERR,FORMATO#` |
| `#ST,-2.0E-06,1.7E-03,9.0E-01#` / `#ST,0,0,1.5#` | vértice 1,261 / borde | `#OK#` / `#OK#` |
| `#S,1,…,-3.02000000E+02#` (T-C23) | dentro | `#OK#`; después `#E,1,1000#` → 231 |
| `#S,1,0,0,1,0#` | R(4300) = 4300 | `#ERR,FORMATO#` |
| `#S,1,0,0,1,-700#` | R(600) = -100 | `#ERR,FORMATO#` |
| `#S,1,0,-1.0E-03,5.0,-2000#` | máximo interior 4250 | `#ERR,FORMATO#` |
| `#S,1,0,1.0E-03,-5.0,6200#` / `…,6500#` | mínimo interior -50 / 250 | `#ERR,FORMATO#` / `#OK#` |
| `#S,1,1.0E-07,-1.0E-03,3.0,1300#` / `…,-1000#` | cúbica, máximo local 4127 / 1827 | `#ERR,FORMATO#` / `#OK#` |
| `#S,1,1.0E+30,0,0,0#`, `#S,1,0,0,1.0E+38,0#`, `nan` | desbordamiento, NaN | `#ERR,FORMATO#` |
| `#S,2,<fábrica>#` / `#S,3,<fábrica>#` | ver §7.1 | `#ERR,FORMATO#` / `#OK#` |

Además, 120 curvas aleatorias de grado 1, 2 y 3 y 120 factores de temperatura aleatorios: la decisión del
firmware coincide en 240 de 240 con un barrido fuera (x entero de 600 a 4300, y T de 0 a 831 con paso
0,05, en `float32`).

**Observado de paso:** escribir con `#ST` el texto de fábrica que da la TDD (T-A30, paso 1) deja el
equipo en `CAL` con máscara `1000` (bit 12): `strtod` lee `4,32120039E-04` y `9,01486659E-01`, que no son
los `float` de ROM. Con `#S` de valores de fábrica pasaría lo mismo en su bit. Para volver a `DEF` en
temperatura no hay orden: `#F` sólo repone coeficientes.

### 7.4 No regresión

- **T-A20** repetida con el fuente de la V3.6.1: `pruebas/T-A20_gen_equiv.py` regenera un arnés
  **idéntico byte a byte** al archivado (`coefFabrica` y `aplicarEcuacion` no cambian). 4 grupos,
  786 432 comparaciones, **0 diferencias**, y las cuatro sumas iguales a las de `pruebas/T-A20.md`
  (2079971309, 1460313976, 1484936099, 1283016283).
- `#V#` en simulador: `#V,3.6,2026-09-19,DEF,0000#`. La cadena `Sep 19 2026` está en el `.hex`.
- Bits de configuración e IDLOC iguales a los de la V3.6.

### 7.5 Riesgos para regrabar SLV-002

- Grabar con borrado completo deja la EEPROM de datos a 0xFF (§5): SLV-002 volvería a `DEF` y PIN `2026`.
  Hoy está en `DEF`; si alguien ha escrito algo desde el 19-sep, se perdería. **Antes de grabar: `#V#`,
  `#G` de los 12 códigos y `#GT#`.**
- `#S` es más estricto: una calibración del código `2` con forma parecida a la de fábrica, o cualquier
  curva que dé más de 4000 o menos de 0 entre x = 600 y 4300, será rechazada con `#ERR,FORMATO#`.
- Nada nuevo en la ruta de medida: T-A20 da 0 diferencias. El código añadido sólo corre con `#S` y `#ST`.

## 8. V3.6.2: `#FT#`, serie y fecha de calibración

Sólo cambia `calibracion_v36.c`. No se tocan la ruta de medida, la pantalla STONE (sigue mostrando la
constante `SLH-046`, `gui.c:189,198`) ni el formato de `#V#`. Probado sólo en simulador. Filas nuevas
en `05_Documentacion/PROTOCOLO-V3.6.md`, tabla de órdenes.

| Líneas | Cambio |
| :--- | :--- |
| `:477-585` | Registros de serie y fecha: `regExtraLeer`, `regExtraEscribir`, validación y respuestas |
| `:716-727` | `#FT#` |
| `:728-759` | `#GC#`, `#GN#`, `#SC#`, `#SN#` |

### 8.1 `#FT#`: factor de temperatura de fábrica

**El hueco que cierra:** tras un `#ST`, ninguna orden devolvía la temperatura a fábrica; `#F` sólo repone
coeficientes, así que `#V#` se quedaba en `CAL` con el bit 12 (§7.3).

Hace lo mismo que `#F,k#` para un código. Exige sesión de administrador (`#ERR,BLOQUEADO#` sin ella),
copia en `X_2`, `X_1`, `X_0` los valores de ROM (`tempFabrica`, que `calibracionIniciar()` toma de los
literales de `gui.c:42-44` al arrancar) y guarda con `guardarYResponder()`: reescribe el registro de
temperatura (0x1DC) con esos valores y su CRC, lo relee y responde `#OK#`, o `#ERR,EEPROM#` con la RAM
como estaba. **El registro no se invalida** porque comparte CRC con el PIN (§2.1): invalidarlo devolvería
el PIN a `2026`. Con los valores de ROM en RAM, la máscara pierde el bit 12 (`mascaraAjustes`,
comparación bit a bit); si ningún código está tocado, `#V#` vuelve a `DEF,0000`. `#FT` con argumentos da
`#ERR,FORMATO#`.

### 8.2 Serie (`#SN`/`#GN`) y fecha de calibración (`#SC`/`#GC`)

Dos registros nuevos en la zona libre de la EEPROM (0x1EE-0x3FF según
`05_Documentacion/SPEC-Registro-Indicador-Interventoria.md` §3.2; el bloque heredado de
`eeprom_manager.h:9-23` acaba en el byte 73), con el formato de los demás (16 bytes + CRC-16/CCITT-FALSE,
byte bajo primero):

| Dirección | Registro | Datos |
| :--- | :--- | :--- |
| 0x1EE-0x1FF | Serie | longitud (1-12), 12 caracteres, 3 ceros |
| 0x200-0x211 | Fecha de calibración | año (2 bytes, byte bajo primero), mes, día, 12 ceros |

- **Órdenes:** las propuestas, sin cambios de nombre: no chocan con ninguna existente (`S`, `ST`, `G`,
  `GT`, `F` y `FT` se comparan enteras con `strcmp`). `#SC` y `#SN` exigen sesión de administrador;
  `#GC` y `#GN` son libres, como `#G`.
- **Fecha:** `AAAA-MM-DD` exacto (10 caracteres), año 2020-2099, mes 1-12, día según el mes (29 de
  febrero sólo si el año es múltiplo de 4, que en 2020-2099 es la regla completa). Cualquier otra cosa da
  `#ERR,FORMATO#` sin tocar la EEPROM. El firmware no calcula vencimientos.
- **Serie:** 1-12 caracteres ASCII 0x20-0x7E sin `#` ni `,`. **Ajuste sobre la propuesta:** `#SN,NONE#`
  se rechaza, porque `NONE` es lo que responde `#GN#` cuando no hay serie y sería ambiguo. No hay orden
  para borrar la serie. El bloque heredado de serie (`eeprom_manager.h:11`, `writeSerial` sin llamadas)
  ni se usa ni se toca.
- **Lectura:** `NONE` si la CRC no cuadra o el contenido está fuera de rango (fecha imposible, longitud
  fuera de 1-12, carácter no admitido). Con la EEPROM a 0xFF la CRC no cuadra, así que `NONE`. Los
  registros no dependen de la cabecera `V36`.
- **Escritura:** sólo de los bytes que cambian, relectura de los 18 y `#OK#`, o `#ERR,EEPROM#` si no
  cuadra (no hay estado en RAM que deshacer).
- **Restaurar a fábrica (propuesta):** `#F`, `#F,*` y `#FT` **no tocan** ni la serie ni la fecha. La
  fecha registra lo que hizo el superadmin, no describe los coeficientes; si el firmware la borrase al
  reponer fábrica, se perdería la huella de que hubo una calibración. La incoherencia la detecta la app:
  **fecha presente y `#V#` en `DEF`** significa que se repusieron los de fábrica después de calibrar, y
  la medida debe tratarse como no calibrada. Para borrarla a propósito está **`#SC,NONE#`** (admin), que
  deja el registro a 0xFF.

### 8.3 Pruebas en simulador

Todas con el `calibracion_v36.c` real, tramas por `adminProcesarTrama` y EEPROM simulada en RAM.

- **`#FT`** (`pruebas/T-FT/`): sin sesión, `#ERR,BLOQUEADO#`; `#ST,0,5.0E-04,9.5E-01#` da `#OK#`, `#GT` lo
  devuelve y `#V#` da `CAL,1000`; `#FT#` da `#OK#`, `X_2`, `X_1` y `X_0` quedan **iguales bit a bit a los
  literales de ROM** y `#V#` da `#V,3.6,2026-09-19,DEF,0000#`. Tras rearrancar desde la EEPROM, otra vez
  iguales a ROM y `DEF,0000`, con el PIN cambiado antes por `#P` conservado. Tras `#Q#`, `BLOQUEADO`.
- **Serie y fecha** (`pruebas/T-SN-SC/`, 56 tramas): EEPROM en blanco, `#GC,NONE#` y `#GN,NONE#`; sin
  sesión, `BLOQUEADO`; ida y vuelta de `2026-09-19`, `2028-02-29`, `SLV-002`, `ABCDEFGHIJKL` y `SLV 002`;
  rechazo (`FORMATO`, sin cambiar lo guardado) de `2026-02-29`, `2019-12-31`, `2100-01-01`, `2026-13-01`,
  `2026-04-31`, `2026-09-00`, `2026-9-19`, `2026/09/19`, fecha vacía, campo de más, serie vacía, de 13
  caracteres, `NONE`, con coma y con un carácter de control; `#F,*#` y `#FT#` no tocan serie ni fecha;
  las dos persisten tras rearrancar; un bit cambiado en cada registro da `NONE` sin colgarse, y se puede
  volver a escribir; `#SC,NONE#` deja `NONE`. **Los bytes 0x000-0x1ED (cabecera, 12 códigos y
  temperatura/PIN) no cambian en ninguna escritura de serie o fecha** (0 bytes distintos, con coeficientes
  y temperatura ya escritos).
- **T-A30:** mismas tramas que en §7.3; salida idéntica a `pruebas/T-A23_T-A30/resultado_T-A30.txt`.
- **T-A20:** el arnés regenerado con `pruebas/T-A20_gen_equiv.py` es idéntico byte a byte al archivado; 4 grupos,
  786 432 comparaciones, **0 diferencias**, y las cuatro sumas iguales a las de `pruebas/T-A20.md`.
- Bits de configuración (`EC FF F7 FF 9F FF FF DF FE FF`) e IDLOC iguales. `#V#` sigue en `3.6`; la
  cadena `Sep 19 2026` está en el `.hex`.

**Para quien lea `#GT` tras `#FT`:** imprime `4.32120039E-04` y `9.01486480E-01`, no el texto de la
fuente. Son los `float` de ROM, bit a bit, impresos con el error de `%.8E` de XC8 (§7.2); con `ULP_G = 4`
la app los da por iguales a fábrica.

### 8.4 Riesgos para grabar

- **`#V#` no distingue la 3.6.2 de la 3.6.1:** las dos se compilaron el 19-sep y dan
  `#V,3.6,2026-09-19,…#`. Para saber cuál lleva un equipo: `#GC#` responde `#GC,…#` en la 3.6.2 y
  `#ERR,FORMATO#` en la 3.6.1. Tras grabar, comprobar el md5 de la lectura ICSP.
- Si el borrado es completo, la EEPROM vuelve a 0xFF: `DEF`, PIN `2026`, serie y fecha `NONE`. Antes de
  grabar, leer `#V#`, `#G` de los 12 códigos y `#GT#`.
- Ninguna orden anterior cambia de comportamiento; la ruta de medida no se toca.
