# Segundo equipo "V3" (V3-2): identificación de firmware y placa

**Sin medir en hardware.** Todo lo de abajo sale de leer el `.hex` extraído por ICSP el 19-sep-2026
y cruzarlo con los fuentes; no se ha comprobado con polímetro ni con un terminal serie. Análisis de
sólo lectura: no se usó el PICkit ni se grabó nada.

- Lectura: `lectura_V3-2_2026-09-19.hex`, md5 `a1c91038f4989b844a8164b36723e3f9` (recalculado).
- Datos físicos dados por Diego: pantalla **STONE STVA035WT-01** (2.ª generación), arranque con la
  imagen "SAT-LUX/V3" (es una imagen de la pantalla, no identifica firmware: L-01), placa con la
  serigrafía **"www.itvial.com v4.0"** y **"19 de noviembre de 2021"**.
- Direcciones de SFR: `pic18f47k42.h` del DFP PIC18F-K 1.4.87 (`U1RXPPS` 0x3AE5, `U2RXPPS` 0x3AE8,
  `RB0PPS` 0x3A08, `RC1PPS` 0x3A11, `RC4PPS` 0x3A14, `RD0PPS` 0x3A18, `U1BRGL/H` 0x3DF5/6,
  `OSCFRQ` 0x39DF). El código se recorrió siguiendo `MOVLB` + `MOVWF` (el patrón de MCC).

## Resumen

| Pregunta | Respuesta |
| :--- | :--- |
| ¿Coincide con la base v4.0? | **No.** Ni byte a byte ni en la configuración de patas |
| ¿Para qué patas se compiló? | Las de la **V4.1 / `Retro_smd_v1`**: UART1 en RC4/RC5, UART2 en RD0/RD1 |
| Baudios UART1 | **115 200** (no 9600) |
| Versión | Intermedia **sin fuente en disco**: patas y reloj de V4.1, protocolo STONE binario `A5 5A` de la v4.0 |
| ¿Sirve la V3.6? | **No, tal como está.** Casi ninguna pata coincide (tabla de §3) |
| Recomendación | **(B)** tratarlo como V4 dentro de la línea V4.6 |

**Contradicción abierta:** la placa dice "v4.0", y el fuente v4.0 (abr-2021) usa las patas de la
H-IoT; el firmware leído usa las de la `Retro_smd_v1`. La explicación más probable —**no
comprobada**— es que la placa itvial "v4.0" de nov-2021 ya tenía la asignación de patas que después
heredó la `Retro_smd_v1` (cuya serigrafía es "www.itvial.com / TC. RETROREFLECTOMETRO V4.5 / 30 de
Mayo del 2025", `02_Hardware/Retro2025/Retro_smd_v1.brd:1282-1284` del repositorio V5). Es decir:
**"v4.0" en la serigrafía no quiere decir "placa H-IoT"**. No hay en disco ningún diseño de placa de
nov-2021 (búsqueda de `.brd/.sch/.kicad_pcb/.pdsprj` en `old\` y `02_Hardware\`: sólo aparecen la
`Retro_smd_v1` y placas 18F4550). Se cierra con polímetro (§5).

## 1. Placa: la PPS del código leído

Inicialización de patas (`PIN_MANAGER_Initialize`) en la lectura, 0x183ac-0x1843a. Bytes en
0x18428: `0F 0E 84 6F 19 0E E8 6F 13 0E 14 6F 16 0E 18 6F 15 0E E5 6F` = `MOVLB 0x3A;
MOVLW 0x19; MOVWF 0xE8,b; MOVLW 0x13; MOVWF 0x14,b; MOVLW 0x16; MOVWF 0x18,b; MOVLW 0x15;
MOVWF 0xE5,b`.

| Registro (dirección) | Lectura V3-2 | V4.1 fuente (`base_v4.1_repositorio`) | v4.0 fuente y `.hex` (`base_v4.0_2021`) | V3.6 (`RetroVertical_V3.6.X`) |
| :--- | :--- | :--- | :--- | :--- |
| `U1RXPPS` (0x3AE5) | 0x15 = **RC5** (0x1843a) | 0x15 RC5 (`pin_manager.c:140`) | 0x10 RC0 (`pin_manager.c:140`; `.hex` 0x9a20) | 0x10 RC0 (`pin_manager.c:95`) |
| UART1 TX | `RC4PPS` = 0x13 → **RC4** (0x18432) | `RC4PPS` 0x13 (`:138`) | `RC1PPS` 0x13 → RC1 (`:139`; `.hex` 0x9a1c) | `RC1PPS` 0x13 (`:92`) |
| `U2RXPPS` (0x3AE8) | 0x19 = **RD1** (0x1842e) | 0x19 RD1 (`:137`) | 0x09 RB1 (`:137`) | 0x09 RB1 (`:90`) |
| UART2 TX | `RD0PPS` = 0x16 → **RD0** (0x18436) | `RD0PPS` 0x16 (`:139`) | `RB0PPS` 0x16 → RB0 (`:138`) | `RB0PPS` 0x16 (`:91`) |
| Otras PPS | ninguna | ninguna | ninguna | `RC4PPS`=PWM7, `RC5PPS`=PWM5 (`:93-94`) |

Resto de la configuración de puertos, lectura (0x183ac-0x183e6) frente a fuentes:

| Registro | Lectura V3-2 | V4.1 (`pin_manager.c:69-91`) | v4.0 (`pin_manager.c:69-91`) | V3.6 (`pin_manager.c:22-44`) |
| :--- | :--- | :--- | :--- | :--- |
| TRISA / ANSELA | 0xCF / 0xCF | **0xCE / 0xCE** | 0xFF / 0xFF | 0xFF / 0xFF |
| TRISB / ANSELB | 0xDF / 0xDF | 0xDF / 0xDF | 0xFE / 0xFC | 0xFE / 0xFC |
| TRISC / ANSELC | 0x2C / 0x04 | 0x2C / 0x04 | 0xB1 / 0x30 | 0x81 / 0x30 |
| TRISD / ANSELD | 0x0E / 0x00 | 0x0E / 0x00 | 0x01 / 0x01 | 0x00 / 0x00 |
| TRISE / ANSELE | 0x04 / 0x04 | 0x04 / 0x04 | 0x07 / 0x04 | 0x03 / 0x04 |
| WPUA-E | C=0x08, D=0x0C, resto 0 | idéntico | C=0x80, E=0x03 | C=0x80, D=0x01, E=0x03 |

La lectura es **idéntica a la V4.1 en todo salvo RA0**, que en la lectura es entrada analógica y en
el fuente V4.1 es la salida `EN_AUX2_LIGHT` (sin conexión en la `Retro_smd_v1`, según
`HARDWARE-V4-Retro_smd_v1.md:78`). Apunta a un firmware anterior a que se añadiera esa salida.

Canales de ADC: la función de conversión (0x1907e) se llama con `MOVLW` 0x12 (ANC2, 0x13bf0),
0x06 (ANA6, 0x14b8a y 0x14c2c), 0x07 (ANA7, 0x15aee) y 0x01 (ANA1, 0x15e7c). Son los de la V4.1:
batería ANC2 (`Bateria.c:34`), óptica ANA6 (`Optical_Capture.c:110,124`), temperatura óptica ANA7
(`Temp_Optica.c:32`), temperatura de circuito ANA1 (`Temp_Circuit.c:42`). `ADREF` = 0x12
(0x18964), igual que en la V4.1 (VREF+ y VREF- externas); la V3.6 pone 0x02.

**Conclusión de la tarea 1:** el firmware se compiló para la asignación de patas de la
`Retro_smd_v1` (V4.1), **no para la H-IoT**. La PPS de la v4.0 sí coincide con la H-IoT de la V3.6
en las UART (RC0/RC1, RB0/RB1), pero **eso es la v4.0 del disco, no lo que lleva este equipo**.

## 2. Versión

- **Coincidencia exacta: ninguna.** Comparados todos los `.hex` del Vertical en `old\VERTICAL\`,
  `D:\IT\P_RetroVertical_V4.6\` y el repositorio V5. Diferencias byte a byte sobre el tramo común:
  v4.0 (md5 `610bd9c3`, las tres copias de `old` son el mismo fichero) 34 409 bytes, V4.1 `dist`
  31 349, 2020 29 133, V3.6 36 041. Ninguno comparte más del 4 % de sus bloques de 16 bytes con la
  lectura. Tamaños: lectura 0x192a2, v4.0 0xa832, V4.1 `dist` 0x17724 (los `dist` del V4.1 no
  corresponden a su fuente, regla §9 del repositorio V5: sólo sirven de pista).
- **Cadenas.** La lectura tiene `***** RETROREFLECTOMETRO VERTICAL *****` (0xff5e),
  `***** NEW SAMPLE *****` (0xff88), `OFFSET= %0.2f` (0xffa1), `@LEERV,%d@/n/r` (0xffb3) y dos
  de depuración: `%0.2f TO, %0.1f VBat, %0.3f DecimasAjuste, %0.2f Offset` (0xfee7) y
  `%0.3f ADC ORIG, %0.2f TC, %0.2f Cand, %0.3f Adc Ajuste T` (0xff23).
  - `RETROREFLECTOMETRO VERTICAL`, `OFFSET=` y `@LEERV` están en el V4.1 (`Aplicacion.c:94,98,312`).
    La v4.0 no tiene ni el saludo ni `OFFSET=` (su `.hex` sólo trae `Valor temperatura`,
    `Valor ADC` y `@LEERV`, 0x2001-0x2029).
  - `NEW SAMPLE`, `DecimasAjuste` y `Adc Ajuste T` **no están en ningún fuente del disco**:
    `grep` en `*.c` de las bases y de `old\VERTICAL`, y segunda búsqueda con ripgrep en
    `D:\@Proyect\IT` (`*.c,h,txt,md,i,p1,lst`) y en todo `D:\IT`: sin resultados.
- **Pantalla.** La lectura construye tramas binarias STONE: 15 `MOVLW 0xA5` y 17 `MOVLW 0x5A`,
  como la v4.0 (`Stone.c:27,30`; su `.hex` tiene 18 y 20). **No tiene ninguna trama JSON `ST<{...}>ET`**,
  y el `.hex` V4.1 tiene 32 (`App_Stone.c:271-372`). Cuadra con la STVA035WT-01 de 2.ª generación.
- **Oscilador.** CONFIG1L = 0x8C: `RSTOSC` = HFINTOSC 64 MHz, `FEXTOSC` = OFF; `OSCFRQ` = 0x08
  (0x18f60). Igual que v4.0 y V4.1 (`device_config.c:50-51`, `mcc.c:65,71`). SLV-002 y la V3.6
  llevan CONFIG1L = 0xEC (`RSTOSC = HFINTOSC_1MHZ`, `OSCFRQ = 0x05` = 16 MHz, `mcc.c:74` de la V3.6).

**Conclusión de la tarea 2:** es una versión **intermedia entre la v4.0 y la V4.1**, sin fuente en
disco: patas, reloj y baudios de la V4.1; driver de pantalla binario de la v4.0; mensajes de
depuración de ajuste por temperatura que no aparecen en ningún fuente. Por la fecha de la placa
(nov-2021), probablemente de finales de 2021 o 2022. **La lectura ICSP es la única copia de ese
firmware**: no se borra.

## 3. Baudios de UART1

`U1CON0` = 0xB0 (0x1850e, `BRGS` = 1), `U1BRGL` = 0x8A (0x1851a), `U1BRGH` = 0x00 (0x1851e), con
Fosc = 64 MHz: 64 000 000 / (4 × 139) = **115 108 Bd → 115 200**. UART2 igual (0x185aa-0x185ae).
La v4.0 pone `U1BRG` = 0x0682 (`uart1.c:122,125`; `.hex` 0x9b00-0x9b04) → 9598 Bd, y la V3.6
`U1BRG` = 0x01A0 a 16 MHz (`uart1.c:124,127`) → 9592 Bd.

## 4. ¿Se puede grabar la V3.6? Patas que usa la V3.6 frente a lo que configura el firmware leído

Uso de patas de la V3.6: `pin_manager.h:25-489` y `measurement.c:68-324`. Uso en la placa del
equipo: lo que configura la lectura, y el significado según la `Retro_smd_v1`
(`HARDWARE-V4-Retro_smd_v1.md:56-109`), que **se toma como aproximación, no como dato**, porque la
placa es otra revisión.

| Pata | V3.6 la usa como | Lectura V3-2 la configura como | Riesgo si se graba la V3.6 |
| :--- | :--- | :--- | :--- |
| RC0 / RC1 | UART1 Bluetooth RX / TX | salida / salida (sin conexión en `Retro_smd_v1`) | **Sin Bluetooth** |
| RC5 | salida PWM5 | UART1 RX (llega el TX del módulo BT) | **Dos salidas enfrentadas** (PIC contra módulo BT) |
| RC4 | salida PWM7 | UART1 TX | Basura hacia el BT |
| RB0 / RB1 | UART2 STONE TX / RX | entradas analógicas las dos (`INTERRUPT`, `SDA` en `Retro_smd_v1`) | **Pantalla sin comunicación** |
| RD1 | salida `BUZZER` | UART2 RX (llega R2OUT del MAX3232) | **Dos salidas enfrentadas** (PIC contra MAX3232) |
| RD0 | salida `DHT11` | UART2 TX | Basura hacia la pantalla |
| RD2 / RD3 | salidas `LCD_RS` / `LCD_E` | entradas con pull-up: pulsadores | **Salida en alto cortocircuitada a masa al pulsar** |
| RE0 / RE1 | entradas: pulsadores | salidas: `EN_H_LIGHT` / `EN_L_LIGHT` (ULN2003) | Los LED del cabezal no se encienden; entradas del ULN al aire |
| RC2 / RC3 | salidas: luz 15 / 30 | RC2 entrada analógica de batería; RC3 entrada | RC2 empuja el divisor de batería; luces sin mando |
| RA0 / RA1 | canales ópticos (ANA0 / ANA1) | RA0 analógica sin uso; RA1 temperatura de circuito | **La medida óptica lee otra señal** |
| RA6 | batería (ANA6) | óptica (ANA6) | La batería se calcula con la señal óptica |
| RA4 / RA5 | temperaturas (analógicas) | RA4 salida zumbador (ULN I7) | Zumbador mudo |
| RC6 | LED rojo | salida (sin uso conocido) | LED de vida (RB5) apagado |
| RE2 | salida `PIN_CALOR` | entrada analógica | Sin efecto conocido |
| Reloj | 16 MHz | 64 MHz | Ninguno (interno en los dos) |

Ni una sola función de la V3.6 cae en la pata que usa esta placa, y hay **al menos tres puntos de
contención eléctrica** (RC5, RD1, RD2/RD3). La V3.6 **no es compatible pata por pata**.

## 5. Recomendación (la decisión es de Diego)

**(A) Grabar la V3.6 tal como está: no.** Lo que se pierde: Bluetooth, pantalla, pulsadores,
luces del cabezal y medida óptica, es decir, el equipo entero; y se arriesgan las salidas de RC5,
RD1, RD2 y RD3 contra el módulo BT, el MAX3232 y los pulsadores. Lo que se gana: nada utilizable.
Que el original esté respaldado (sin CP, md5 `a1c91038`) hace la operación reversible en firmware,
**no en hardware** si una contención llega a dañar una pata.

Variante A' (la única en que la V3.6 tendría sentido): una compilación de la V3.6 **reasignada a
estas patas** (PPS, TRIS, ANSEL, canales ADC, 64 MHz, 115 200 Bd, driver STONE). Eso ya no es la
V3.6 validada en SLV-002: es un port con sus propias puertas de validación, y en la práctica es el
trabajo de la línea V4.6.

**(B) Tratarlo como V4 dentro de la línea V4.6: sí.** Motivos: su firmware es de la familia V4
(`@LEERV`, 115 200 Bd, patas de V4.1). Riesgos de B: (1) no hay fuente de la versión exacta que
lleva, así que P3 (compilación reproducible) no puede dar "idéntico a lo leído" y habrá que decidir
por escrito la base (V4.1 más el driver STONE binario de la v4.0); (2) su pantalla es de 2.ª
generación (`A5 5A`), no la STWA JSON de la V4.1, y el proyecto de pantalla cargado ("SAT-LUX/V3")
no se sabe si casa con las direcciones VGUS que escribe este firmware; (3) el ajuste por temperatura
de este firmware (cadenas `Adc Ajuste T`, `DecimasAjuste`) puede no ser el de la V4.1, donde está
anulado (`Aplicacion.c:253-260`), de modo que sus lecturas pueden no ser comparables con un V4.1.

**Antes de decidir, con polímetro y sin alimentar** (cierra la contradicción de la placa):
continuidad de las patas 23/24 (RC4/RC5) al conector del Bluetooth, 19/20 (RD0/RD1) al MAX3232 y
15/16 (RC0/RC1) a cualquier sitio. Si RC4/RC5 van al BT, la placa es de asignación `Retro_smd_v1` y
la opción A queda descartada del todo.
