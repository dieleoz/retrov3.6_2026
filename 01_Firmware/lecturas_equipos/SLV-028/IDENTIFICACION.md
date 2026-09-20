# SLV-028: identificación de firmware y veredicto de grabación

**NO se le puede grabar `RetroVertical_V4.6_8f626188.hex` tal como está.** La PPS sí es la de la
`Retro_smd_v1` (V4.1) —la alarma por el rótulo "SAT-LUX/IoT" es infundada—, pero la placa no es lo
único que decide: este equipo lleva **pantalla STONE de protocolo JSON** y **su propia calibración
por tramos**, y la V4.6 estándar lleva pantalla binaria `A5 5A` y las ecuaciones de la V4.1.

**Sin medir en hardware.** Todo lo de abajo sale de leer el `.hex` extraído por ICSP el 19-sep-2026
y cruzarlo con los fuentes y con la lectura de Nordeste; no se ha comprobado con polímetro ni con un
terminal serie. Análisis de sólo lectura: no se grabó nada.

- Lectura: `lectura_SLV-028_2026-09-19.hex`, md5 `d49560286465968463a6ee2ed2eeae8c` (recalculado con
  `md5sum`, coincide con el `.md5` de al lado). **Única copia de ese firmware.**
- Herramienta: `D:\IT\P_RetroVertical_V4.6\01_Firmware\lecturas_equipos\tools\identificar_lectura.py`,
  que antes de usarse reprodujo **36/36** comprobaciones de `V3-2/IDENTIFICACION.md`
  (`--verificar-v3-2`). Cada negativo se ha repetido además con un barrido de bytes crudos
  independiente del desensamblador, y ese barrido se validó contra un **control positivo** (el `.hex`
  v4.0, que sí es H-IoT).
- Datos físicos dados por Diego: rótulos **"SAT-LUX/V4"**, serie **SLV-028**, **"SAT-LUX/IoT"**;
  Revision ID `a003`; CONFIG `8C FF F7 FF 9F FF FF FF FF FF`. Los tres primeros son rótulos, no
  identifican nada (lección L-01); el Revision ID no es comprobable desde el `.hex`; el CONFIG sí, y
  se confirma.

## Resumen

| Pregunta | Respuesta |
| :--- | :--- |
| ¿Es placa H-IoT? | **No.** UART1 en RC4/RC5, UART2 en RD0/RD1: asignación `Retro_smd_v1` / V4.1 |
| ¿Coincide con alguna base del disco? | **No.** Ni con la de Nordeste. Lo más parecido, la V4.1: 7,02 % de bloques |
| Pantalla | **JSON `ST<{…}>ET`** (32 tramas, 19 `<buttonN>`), la de la V4.1. La V4.6 lleva `A5 5A` |
| Ecuaciones | **Propias, por tramos**, sin fuente en disco. No son las de la V4.1 ni las de Nordeste |
| Compensación por temperatura | **Ninguna.** Está cableada a cero, pese a las trazas `Dec Ajuste T` |
| Variable `error` (estado oculto) | **No la tiene.** La ecuación recibe 3 argumentos, no 4 |
| Baudios / reloj | 115 200 Bd en UART1 y UART2; 64 MHz. Igual que la V4.6 |
| ¿Grabar la V4.6 `8f626188`? | **No.** Tres bloqueos: pantalla, ecuaciones y `error` (§6) |

## 1. Protección de código y CONFIG

CONFIG 0x300000-0x300009 = `8C FF F7 FF 9F FF FF FF FF FF`, **idéntico** al de la V4.6 y al de la
lectura de Nordeste. `CONFIG5L` = 0xFF → **CP = OFF**: la lectura contiene el programa de verdad y
el original queda respaldado. `CONFIG1L` = 0x8C → `FEXTOSC` = OFF, `RSTOSC` = HFINTOSC 64 MHz.
Decodificado con el `.cfgdata` del DFP PIC18F-K 1.4.87.

**Corrección a los datos del encargo:** el programa **no llega a 0x18210**. El último byte distinto
de 0xFF está en **0x1820B**, y la primera dirección libre es **0x1820C** (98 828 bytes). De 0x1820C
en adelante todo es 0xFF. La diferencia son 4 bytes y no cambia ninguna conclusión, pero la cifra
que se publique debe ser 0x1820C.

EEPROM: 4 bytes distintos de 0xFF — `10 FF FF 64 00 00 …`. Encaja con el mapa de la V4.1
(`V4.1:Aplicacion.c:59-86`): marca de inicialización 0x10, offset LSB 0x64 = 100, offset MSB 0x00,
`EN_DEBUG` 0x00 → el equipo anuncia `OFFSET= 1.00` al arrancar. Ese offset **no interviene en la
medida**: `ap.fOffSet` se sobrescribe con `fOpticaCap × 100` antes de usarse (V4.1:`Aplicacion.c:245`;
en la lectura, el literal 100.0 de 0x12440).

## 2. Patas: la PPS del código leído

Inicialización de patas en 0x1736E-0x173FC. Las cuatro escrituras de PPS, en 0x173F0-0x173FC:

| Registro | Lectura SLV-028 | V4.6 `8f626188` | V4.1 fuente | Nordeste (V3-2) | v4.0 / H-IoT |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `U1RXPPS` | 0x15 = **RC5** (0x173FC) | 0x15 RC5 (0x1C314) | 0x15 RC5 (`pin_manager.c:140`) | 0x15 RC5 | 0x10 RC0 |
| UART1 TX | `RC4PPS` = 0x13 → **RC4** (0x173F4) | `RC4PPS` (0x1C30C) | `RC4PPS` (`:138`) | `RC4PPS` | `RC1PPS` → RC1 |
| `U2RXPPS` | 0x19 = **RD1** (0x173F0) | 0x19 RD1 (0x1C308) | 0x19 RD1 (`:137`) | 0x19 RD1 | 0x09 RB1 |
| UART2 TX | `RD0PPS` = 0x16 → **RD0** (0x173F8) | `RD0PPS` (0x1C310) | `RD0PPS` (`:139`) | `RD0PPS` | `RB0PPS` → RB0 |
| Otras PPS | ninguna | ninguna | ninguna | ninguna | ninguna |

**Negativo verificado dos veces y con dos herramientas.** (1) El barrido lineal de
`identificar_lectura.py` no encuentra **ninguna** escritura a `RC0PPS`, `RC1PPS`, `RB0PPS` ni
`RB1PPS`. (2) Un barrido de bytes crudos escrito aparte —que sigue `MOVLB`/`MOVLW`/`MOVWF` sobre el
`.hex` sin desensamblar, y que a propósito acepta falsos positivos— da el mismo resultado: 0
escrituras a patas H-IoT, y las mismas 4 direcciones de arriba. Ese mismo barrido, aplicado al `.hex`
v4.0 como **control positivo**, sí detecta las 2 escrituras H-IoT (`RB0PPS` en 0x09A18, `RC1PPS` en
0x09A1C). El método distingue los dos casos.

**El rótulo "SAT-LUX/IoT" no dice nada del hardware.** Es la segunda vez que pasa: con el equipo de
Nordeste la placa decía "v4.0" y por dentro tenía las patas de la V4.1.

Resto de la configuración de puertos (valores que escribe el código):

| Registro | SLV-028 | V4.6 `8f626188` | V4.1 | Nordeste (V3-2) | v4.0 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| TRISA / ANSELA | **0xCE** (0x17372/0x17394) | **0xCF** | 0xCE = | 0xCF | 0xFF |
| TRISB / ANSELB | 0xDF / 0xDF | 0xDF = | 0xDF = | 0xDF = | 0xFE / 0xFC |
| TRISC / ANSELC | 0x2C / 0x04 | 0x2C = / 0x04 = | = | = | 0xB1 / 0x30 |
| TRISD / ANSELD | 0x0E / 0x00 | = | = | = | 0x01 / 0x01 |
| TRISE / ANSELE | 0x04 / 0x04 | = | = | = | 0x07 / 0x04 |
| WPUC / WPUD | 0x08 / 0x0C | = | = | = | 0x80 / 0x00 |
| `ADREF` | 0x12 (0x178E8) | 0x12 = | 0x12 = | 0x12 = | 0x12 = |

**La lectura es idéntica a la V4.1 en los 15 registros de puerto, y a la V4.6 en 14 de 15.** El único
que difiere es `TRISA`/`ANSELA`: la lectura pone **0xCE** (RA0 salida digital, como la V4.1) y la
V4.6 pone **0xCF** (RA0 entrada analógica). La V4.6 lo cambió a propósito para parecerse a la lectura
de Nordeste (`CAMBIOS-V4.6.md` §2, RF-FW-29). En este equipo el cambio va **en contra**: RA0 se usa
(§5).

Canales de ADC, por las llamadas a la función de conversión (0x17FC6): **0x12 = ANC2** batería
(0x12FB4), **0x06 = ANA6** óptica (0x13BC2 y 0x13C64, dos llamadas como en
`V4.1:Optical_Capture.c:110,124`), **0x07 = ANA7** temperatura óptica (0x14D2C), **0x01 = ANA1**
temperatura de circuito (0x157D8). Son exactamente los cuatro de la V4.1, la V4.6 y Nordeste.
`ADREF` = 0x12: VREF+ y VREF− externas, una cuenta = 1 mV.

**Conclusión de la tarea 1: la placa es de asignación `Retro_smd_v1` (V4.1).** Por patas, la V4.6 es
compatible. El problema está en otra parte.

## 3. Reloj y baudios

`OSCCON1` = 0x60 (0x17E80), `OSCFRQ` = 0x08 (0x17E8C), `RSTOSC` = HFINTOSC 64 MHz → **Fosc = 64 MHz**.

- UART1: `U1CON0` = 0xB0 (0x1757C, `BRGS` = 1), `U1BRGL` = 0x8A (0x17588), `U1BRGH` = 0x00 (0x1758C)
  → 64 000 000 / (4 × 139) = **115 108 Bd → 115 200**.
- UART2: idéntico (0x1760A, 0x17616, 0x1761A).

Los mismos valores, byte a byte, que la V4.6 `8f626188` (0x1C3B6/0x1C3C0 y 0x1C516/0x1C520). **En
baudios y reloj no hay ningún obstáculo**, y confirma lo que Diego midió por Bluetooth a 115200.

## 4. Pantalla: aquí está el primer bloqueo

| Binario | cadenas `ST<{` | cadenas `<button` | `MOVLW 0xA5` | `MOVLW 0x5A` | Protocolo |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SLV-028** | **32** | **19** | 6 | **0** | **JSON `ST<{…}>ET`** |
| V4.1 | 32 | 19 | 0 | 0 | JSON |
| **V4.6 `8f626188`** | **0** | **0** | 3 | 4 | **binario `A5 5A`** |
| Nordeste (V3-2) | 0 | 0 | 15 | 15 | binario `A5 5A` |
| v4.0 | 0 | 0 | 18 | 18 | binario `A5 5A` |

Las 32 tramas JSON de la lectura están en 0x0F3C5-0x0FCA2 y son literalmente las de la V4.1
(`set_text` sobre `label1`…`label14`, `open_win` sobre `window1`…`window10`, `set_brightness`,
`back_home`). Los 19 `<buttonN>` están en 0x0FEAC-0x0FF6A y se leen en 0x13580-0x1395E: es la
pantalla la que manda ese texto al PIC. Es decir, **el enlace con la pantalla es de texto en los dos
sentidos**.

**Los 6 `MOVLW 0xA5` de la lectura no son protocolo STONE: son bytes de constantes de calibración.**
Están en 0x11C1E, 0x11C8C, 0x11CFA, 0x11D9C, 0x11E0A y 0x11E44, todos dentro de los bloques de
ecuación de tipo 1 (§5), y 0xA5 es el tercer byte de `1.29` (float32 `b8 1e a5 3f`) y de `−20.64`
(`b8 1e a5 c1`). Alineados a palabra o sin alinear salen los mismos 6, y **`MOVLW 0x5A` sale 0 en las
dos cuentas**: no hay ni una trama `A5 5A` que construir. Es exactamente el falso positivo contra el
que avisaba el encargo, y esta vez se ha cerrado mirando qué constante contenía el byte.

La V4.6 `8f626188`, en cambio, escribe `UART2_Write(0xA5); UART2_Write(0x5A);`
(`compilacion_V4.6/proyecto/Stone.c:21-22,34-35`) y sólo reconoce tramas que empiecen por `A5 5A`
(`Serial2.c:131`). No tiene **ni una** cadena `ST<{` ni **ni un** `<button`.

**Consecuencia:** grabada la V4.6, este equipo se queda **sin pantalla y sin los botones táctiles**.
No es un ajuste fino: son dos familias de pantalla STONE distintas que no se entienden entre sí.

*Condición a confirmar con el equipo delante, y es barata:* que la pantalla de SLV-028 **funcione
hoy** (que muestre la medida y que los botones respondan). Si funciona, es una pantalla de las de
JSON y lo anterior queda cerrado. Si no funcionara, habría que mirarlo aparte antes de concluir nada.

## 5. Qué firmware lleva

**No coincide con ninguna base del disco.** Comparados los **81** `.hex` de
`D:\IT\P_RetroVertical_V4.6\` y `D:\IT\P_RetroVertical_V3.6\` (excluidos `build/` y `nbproject/`),
**ningún md5 coincide** con `d4956028…`. Lo más parecido, por bloques de 16 bytes no vacíos:

| Base | md5 | Cima | Bloques comunes | UART | TRIS/ANSEL/WPU |
| :--- | :--- | :--- | :--- | :--- | :--- |
| V4.1 (XC8 3.10) | `512b93c4` | 0x17724 | **7,02 %** | RC5, RC4, RD1, RD0 | **15/15** |
| V4.1 (XC8 2.36) | `fc24991e` | 0x18874 | 6,10 % | RC5, RC4, RD1, RD0 | 15/15 |
| V4.6 `4927ad55` | `4927ad55` | 0x1D24E | 2,28 % | RC5, RC4, RD1, RD0 | 14/15 |
| Nordeste (V3-2) | `a1c91038` | 0x192A2 | **1,36 %** | RC5, RC4, RD1, RD0 | 13/15 |
| V3.6 | `9d5d5e39` | 0x1DB68 | 0,44 % | RC0, RC1, RB1, RB0 | 4/15 |
| v4.0 | `610bd9c3` | 0x0A832 | 0,18 % | RC0, RC1, RB1, RB0 | 3/15 |
| **esta lectura** | `d4956028` | **0x1820C** | | RC5, RC4, RD1, RD0 | |

**SLV-028 y el equipo de Nordeste no llevan el mismo firmware**, ni de lejos: 1,36 % de bloques
comunes, distinta pantalla, distintas ecuaciones y distinto ajuste por temperatura. Son dos equipos
independientes.

Lo que sí es común con la V4.1 es el esqueleto: los 15 registros de puerto, los 4 canales de ADC, la
capa de pantalla JSON, la cadena de captura óptica (`/1000` en modo captura y `/100` en modo luz —
literales 1000.0 en 0x13DD0 y 100.0 en 0x13E0E—, después `× 0,001` en 0x13E46, igual que
`V4.1:Optical_Capture.c:148,156,161`), el arranque con `***** RETROREFLECTOMETRO VERTICAL *****`
(0x0FCD1) y `OFFSET= %0.2f` (0x0FDB2), y la trama `@LEERV,%d@/n/r` (0x0FE14). Es **la V4.1 modificada**,
con 2,7 KB más de código.

Lo que le han añadido, y no está en ningún fuente del disco:

- **Trazas de depuración** que se emiten por UART1 tras cada medida, gobernadas por el byte
  `EN_DEBUG` de la EEPROM y por las tramas `ENADEBUG` (0x0FF86) / `DISDEBUG` (0x0FF8F):
  `***** NEW SAMPLE *****` (0x0FD6B), `%0.3f ADC ORIG` (0x0FDC4), `%0.3f ADC RED * OFFSET` (0x0FD84),
  `%0.3f Dec Ajuste T` (0x0FD9D), `%0.3f ADC + Dec Ajuste T` (0x0FD35), `TIPO PAPEL: %s` (0x0FDD5),
  `Color: %s` (0x0FE8A), `%0.2f Cand` (0x0FE59) y
  `%0.2f TO, %0.2f TC, %0.1f VBat, %0.2f Offset` (0x0FC71). Se imprimen en 0x12524-0x126F4. Las de
  Nordeste eran parecidas pero **no iguales** (allí `DecimasAjuste` y `Adc Ajuste T` iban agrupadas
  en dos cadenas; aquí van sueltas): son dos ramas de desarrollo distintas.
- **Umbrales de luz distintos.** La estructura de selección de luz es la de
  `V4.1:Optical_Capture.c:40-80`, pero con los umbrales de temperatura óptica cambiados de 30,0 °C a
  **55,0 °C** (papel reflectivo, 0x13AC4) y **45,0 °C** (no reflectivo, 0x13B10). Por debajo de esos
  umbrales enciende RE1 (`ON_LED_OPT`) en reflectivo y RE0 (`ON_LED_H_OPT`) en no reflectivo; por
  encima, RA5 (`ON_AUX_LIGHT`, 0x13B02) y **RA0** (`ON_AUX2_LIGHT`, 0x13B4E) respectivamente.
  **Este equipo usa RA0 como salida** — por eso su `TRISA` es 0xCE y no 0xCF.
- **Sus propias ecuaciones** (§5.1).

### 5.1 Ecuaciones de calibración

La función de ecuaciones está en **0x114A0** y recibe **tres** argumentos: el valor óptico en float
(0x03A-0x03D), el color (0x03E) y el tipo de papel (0x03F, resultado de la llamada de 0x124C4).
Despacho en 0x114B0 (tipo) y 0x114C0/0x1174E/0x11918/0x11A22/0x11A90/0x11B9A (color, códigos 1-6 =
AMA, BLA, ROJ, AZU, VER, NAR, los de `V4.1:APP_Stone.h:36-41`).

La entrada es `V = ADC_medio × 0,1`: el promedio se multiplica por `0,001` (0x13E46) y después por
`100,0` (0x12440). Es la misma escala que en la V4.1, la que `CLAUDE.md` §4 llama `VoltajeADC`.

**Otros papeles** (tipo ≠ 1), rectas por tramos:

| Color | Ecuación | Direcciones de las constantes |
| :--- | :--- | :--- |
| AMARILLO | `V<92`: 2,31·V − 46,38 · `92≤V<108`: 8,31·V − 597,75 · `108≤V<133`: 4,92·V − 231,36 · `V≥133`: 3,28·V − 14,21 | 0x114DE, 0x11510, 0x11544, 0x1159E, 0x115D0, 0x11604, 0x1165E, 0x11690, 0x116C4, 0x116EC, 0x11720 |
| BLANCO | `V<97`: 3,06·V − 61,29 · `97≤V<140`: 9,79·V − 713,69 · `V≥140`: 0,87·V + 535,20 | 0x1176C, 0x1179E, 0x117D2, 0x1182C, 0x1185E, 0x11892, 0x118BA, 0x118EE |
| ROJO | `V<80`: 1,82·V − 36,50 · `V≥80`: 1,07·V + 38,80 | 0x11936, 0x11968, 0x1199C, 0x119C4, 0x119F8 |
| AZUL | 1,97·V − 306,55 (recta única) | 0x11A32, 0x11A66 |
| VERDE | `V<180`: 7,16·V − 1109,80 · `V≥180`: 0,25·V + 131,75 | 0x11AAE, 0x11AE0, 0x11B14, 0x11B3C, 0x11B70 |
| NARANJA | 1,36·V − 27,31 (recta única) | 0x11BAA, 0x11BDE |

**Papel tipo 1** (rama de 0x11C06), una recta por color:

| Color | Ecuación | Direcciones |
| :--- | :--- | :--- |
| AMARILLO, BLANCO, ROJO, NARANJA | 1,29·V − 25,84 | 0x11C16/0x11C4A, 0x11C84/0x11CB8, 0x11CF2/0x11D26, 0x11E3C/0x11E70 |
| AZUL, VERDE | 0,12·V − 20,64 | 0x11D60/0x11D94, 0x11DCE/0x11E02 |

**Continuidad en los cortes**, evaluada con las constantes de arriba (es una comprobación interna del
ajuste, no una medida):

| Color | Corte | Izquierda | Derecha | Salto |
| :--- | :--- | :--- | :--- | :--- |
| AMARILLO | 92 / 108 / 133 | 166,14 / 299,73 / 423,00 | 166,77 / 300,00 / 422,03 | +0,4 % / +0,1 % / −0,2 % |
| BLANCO | 97 / 140 | 235,53 / 656,91 | 235,94 / 657,00 | +0,2 % / +0,0 % |
| VERDE | 180 | 179,00 | 176,75 | −1,3 % |
| **ROJO** | **80** | **109,10** | **124,40** | **+14,0 %** |

AMARILLO y BLANCO son continuas dentro del redondeo de dos decimales de sus coeficientes; VERDE casi.
**ROJO tiene un escalón real de 15,3 unidades (+14 %) en V = 80.** No es un error de extracción —las
dos rectas y el umbral están en 0x11936, 0x11968, 0x1199C, 0x119C4 y 0x119F8—: es una discontinuidad
del propio ajuste del equipo, en la zona baja de ROJO. Conviene saberlo antes de comparar medidas de
ROJO cerca de ese valor, y **no se toca**: el firmware no se reprograma (`CLAUDE.md` §3).

**Dos reglas de `CLAUDE.md` §4 no valen para este equipo:**

1. *"Con papel TIPO 1 el equipo devuelve la señal del ADC sin transformar"*. **Falso aquí.** Es cierto
   en la V4.1 y en la V4.6 (`Ecuaciones.c:77,85,91,99,106,113`), pero SLV-028 **sí calcula** en tipo 1:
   1,29·V − 25,84 o 0,12·V − 20,64. La vía de "registrar cuentas de ADC pidiendo tipo 1" **no
   funciona en este equipo**.
2. *"Hay estado oculto entre mediciones: la variable `error`"*. **Falso aquí.** La ecuación de
   SLV-028 recibe 3 argumentos. Comparado con el **control positivo** de la V4.1, cuya llamada
   (0x11A36) pasa cuatro —float en 0x060-0x063, color en 0x064, tipo en 0x065 y **el `error` global
   desde 0x0D0-0x0D3 en 0x066-0x069**—, la llamada de SLV-028 (0x124CA) carga sólo 0x03A-0x03D,
   0x03E y 0x03F. **No hay `error`, y por tanto no hay dependencia del orden de los disparos.** Las
   tres lecturas repetibles de Diego (716/715/715) son coherentes con esto.

**Comprobación numérica con el dato de campo.** Diego midió `@LEERV,BLA,2@` → `@LEERV,716@`. Tipo "2"
no contiene "1", luego es "otros papeles" (`V4.1:Serial.c:115`). Con la recta de BLANCO para `V≥140`:
716 = 0,87·V + 535,20 → **V = 207,8**, es decir **ADC medio ≈ 2078 cuentas ≈ 2,078 V**, perfectamente
plausible con referencia externa de 4,096 V. Las ecuaciones extraídas reproducen el número observado.

### 5.2 Compensación por temperatura: no tiene

Las trazas `%0.3f Dec Ajuste T` y `%0.3f ADC + Dec Ajuste T` invitan a pensar que sí. **No la tiene.**

- Ninguna de las **siete** constantes del ajuste de Nordeste que la V4.6 lleva cableadas
  (`ST_VA` 0,18 · `ST_T1` 27,0 · `ST_T2` 37,5 · `ST_M1` 0,0113 · `ST_B1` −0,2939 · `ST_M2` 0,0226 ·
  `ST_B2` −0,72, `compilacion_V4.6/proyecto/Optical_Capture.c:63-69`) aparece en la lectura, ni como
  literal `MOVLW`/`MOVWF` ni en bytes crudos. Las siete **sí** aparecen en la lectura de Nordeste
  (0x16A8A, 0x16ABC, 0x16B34, 0x16B66, 0x16B9A, 0x16BF4, 0x16C28) y en la V4.6 (0x19524-0x19672):
  **control positivo superado**, el método las encuentra cuando están.
- La variable que se imprime como `Dec Ajuste T` vive en 0x2AE-0x2B1, y **la única escritura en todo
  el binario** la pone a `0.0` (0x1247E, 0x12482, 0x12486, 0x1248A).
- La que se imprime como `ADC + Dec Ajuste T` vive en 0x2B2-0x2B5, y **su única escritura en todo el
  binario** es una copia directa del valor ya escalado (`MOVFF 0x2BF→0x2B2` y siguientes, 0x1248C,
  0x12490, 0x12494, 0x12498). No hay suma.

Es el mismo patrón que la V4.1 bajo `DEVELOPTMEN` (`V4.1:Aplicacion.c:253-260`: `ap.fAjusteT = 0;
ap.fAjusteTemp = ap.fOpticalCapOffset;`). **Hoy el equipo entrega el óptico sin corregir.**

## 6. Veredicto: no se le graba la V4.6 estándar

Por patas y por reloj, sí. Por lo que hace el firmware, no. Tres bloqueos, en orden de gravedad:

**(1) Pantalla — bloqueo duro.** SLV-028 habla JSON en los dos sentidos; la V4.6 habla binario
`A5 5A` y no lleva ni una cadena JSON ni un `<button` (§4). Se queda sin pantalla y sin botones
táctiles. Es irreversible sólo en el sentido de que hay que volver a grabar: la lectura md5
`d4956028…` permite volver atrás, y sin CP.

**(2) Ecuaciones — bloqueo metrológico.** La V4.6 en `DEF` ejecuta las de la V4.1
(`CAMBIOS-V4.6.md` §1: "las ecuaciones no se han tocado"), cuyos divisores y términos aditivos
(0,034102 · 0,033487 · 0,080052 · 0,244763 · +150 · +30 · +60 · +260) **están en la V4.1 y en la
V4.6 y no están en SLV-028** (búsqueda por literal y por bytes crudos; control positivo superado).
Con el mismo óptico de la medida de campo (V = 207,8):

| | SLV-028 hoy | V4.6 `8f626188` en `DEF` |
| :--- | :--- | :--- |
| BLANCO, otros papeles | **716** | **≈ 1424** (primer disparo tras arrancar, `error` = 0) |
| el mismo, tras un disparo de tipo 1 | 716 (no depende del orden) | **≈ 86** (`error` = V − 163 = 44,8) |

Es decir: donde el equipo hoy lee 716 con un −8,6 % frente al certificado de P20, la V4.6 leería
**el doble**, o **la octava parte**, según lo que se haya disparado antes. La calibración de este
equipo se pierde entera.

**(3) `error` — se reintroduce un defecto que este equipo no tiene.** Hoy SLV-028 no arrastra estado
entre mediciones (§5.1). La V4.6 sí (`Aplicacion.c:307-366`, `error = max(V,0) − 163`). Sería un
retroceso medible sobre un equipo que hoy es repetible.

Y además, aunque no bloqueen por sí solos:

- **Compensación por temperatura que hoy no existe.** La V4.6 aplica el ajuste de Nordeste como
  fábrica: +4,5 unidades de `VoltajeADC` a 30 °C, +13,0 a 37,5 °C, +18,4 a 40 °C, +41,0 a 50 °C.
  Sobre un equipo calibrado sin ninguna corrección, eso es una desviación añadida que crece con la
  temperatura. **Es un hallazgo de primer orden**, como avisaba el encargo: la respuesta a "¿igual,
  otra o ninguna?" es **ninguna**.
- **Luz de ROJO y NARANJA en tipo 1: sí cambia.** SLV-028 reparte la luz **sólo por color**
  (AMA, BLA, ROJ, NAR → RE1; AZU, VER → RE0), en las seis escrituras de `flagReflPaper` de
  0x10E10, 0x10E4E, 0x10E8C (alta) y 0x10ECA, 0x10F08 (baja) y 0x10F46 (alta) — el mismo orden y
  número que `V4.1:App_Stone.c:183,192,200,209,218,227`. La V4.6 (H-A) baja **ROJ y NAR de tipo 1**
  al grupo de RE0. Sobre SLV-028 eso cambia la luz con la que se mide esas dos láminas en tipo 1, y
  por tanto el ADC.
- **RA0.** La V4.6 pone `TRISA`/`ANSELA` = 0xCF (RA0 entrada analógica); SLV-028 usa RA0 como salida
  `EN_AUX2_LIGHT` por encima de 45 °C (0x13B4E). No hay contención —una entrada no pelea con nada—,
  pero se pierde esa luz auxiliar. Poco probable en uso normal; conviene anotarlo.
- **EEPROM.** La grabación la borra (IPE sin "Preserve EEPROM"); se pierde el `10 … 64 00 00` actual.
  No es grave: la V4.6 la reinicializa (`CAMBIOS-V4.6.md` F-9) y ese offset no entra en la medida
  (§1). Pero el respaldo de la EEPROM actual ya está dentro de la lectura.

### Qué haría falta para poder grabarle algo de la línea V4.6

1. **Devolver la capa de pantalla JSON**: partir del `App_Stone.c`/`Serial2.c`/`Stone.c` de la V4.1,
   que es lo que este equipo tiene, en vez del port de la v4.0 que la V4.6 hizo para Nordeste. Es una
   variante de compilación, no un rediseño.
2. **Meter la calibración de este equipo**, no la de la V4.1. Las ecuaciones de §5.1 están extraídas
   y pueden cablearse. Ojo: **el modo `CAL` de la V4.6 usa un solo polinomio cúbico por código**
   (`Calibracion.c:329-334`), y AMARILLO tiene 4 tramos, BLANCO 3, ROJO y VERDE 2. Un cúbico puede
   aproximarlos, pero **no reproducirlos**; si se va por `#S`, hay que acotar el error tramo a tramo
   y dejarlo escrito.
3. **Decidir por escrito qué se hace con el ajuste por temperatura**: el de Nordeste no es de este
   equipo, y este equipo no tiene ninguno. Lo honesto de partida es dejarlo en `F(T) = 1`.
4. **Decidir qué se hace con `error`** y con la luz de ROJ/NAR en tipo 1: los dos cambian el número
   que sale por Bluetooth.

Nada de esto es urgente ni bloquea al equipo: **SLV-028 funciona hoy y está respaldado.** Lo que no
se puede es grabarle el `.hex` que se hizo para Nordeste.

## 7. Contradicciones con los datos del encargo

| Dato entregado | Lo que dice el `.hex` |
| :--- | :--- |
| "Programa no vacío hasta 0x18210" | Último byte ≠ 0xFF en **0x1820B**; primera libre **0x1820C** |
| "Si la PPS es la de la V4.1 → sí [grabar]" | La PPS **es** la de la V4.1 y la respuesta sigue siendo **no**: decide la pantalla y la calibración |
| Alarma por el rótulo "SAT-LUX/IoT" | **Infundada.** Cero escrituras PPS a patas H-IoT, verificado dos veces y con control positivo |
| "En Nordeste eran dos rectas sobre la temperatura óptica; mira si aquí es igual, otra, o ninguna" | **Ninguna**: cableada a cero (0x1247E-0x1248A), pese a las trazas `Dec Ajuste T` |
| `CLAUDE.md` §4: tipo 1 devuelve el ADC sin transformar | Cierto en V4.1/V4.6, **falso en SLV-028**: tipo 1 tiene ecuación propia |
| `CLAUDE.md` §4: hay estado oculto (`error`) entre mediciones | **Falso en SLV-028**: la ecuación recibe 3 argumentos, no 4 |

## 8. Lo que sigue sin medir

- Que la pantalla de SLV-028 **funcione hoy** (muestra la medida, responden los botones). Es la única
  condición que cerraría del todo §4, y se ve en treinta segundos con el equipo encendido.
- Las ecuaciones de §5.1 salen de leer el binario. Se confirman disparando por Bluetooth con dos o
  tres láminas por color y comprobando que el número cae en la recta del tramo. La de BLANCO ya
  cuadra con 716.
- Los umbrales de luz de 55 °C y 45 °C no se han visto actuar.
- El Revision ID `a003` no es comprobable desde el `.hex`.
- Nada de este documento se ha medido con polímetro. La continuidad de RC4/RC5 al Bluetooth y de
  RD0/RD1 al MAX3232 sigue siendo la comprobación física que cierra §2, aunque el binario ya no deja
  mucho margen de duda.
