# CAMBIOS de la V3.6 frente a la base de 2020

**Estado: compilada y probada sólo en simulador (MPLAB SIM). Nada de esto se ha probado en un equipo.**
Lo que exige el equipo delante está en la sección 5 y queda pendiente.

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

- **Tabla de fábrica** (`:33-46`): los 48 coeficientes con **el mismo texto literal** que
  `ecuacionesCalibracion.c:3-42` de la base; un término restado en 2020 aparece con signo menos; el grado
  que falta es `0.0`.
- **Ecuación** (`aplicarEcuacion`, `:228-233`): `((c3·x·x·x + c2·x·x) + c1·x) + c0` en `double` de 32 bits,
  mismo orden de operaciones que 2020, no Horner; la conversión final a `unsigned int` es la misma
  asignación que en 2020. Resultado en simulador: bit a bit igual a 2020 (§3.1).
- **EEPROM** (`:52-222`): cabecera `V36`+versión 1 en 0x100; un registro de 18 bytes por código en
  `0x104 + 18·k` (4 `float` little endian + CRC-16/CCITT-FALSE, byte bajo primero) y otro en 0x1DC para
  `X_2, X_1, X_0` y el PIN. Cabecera mala: todo de fábrica. CRC malo: sólo ese registro de fábrica.
  Escritura sólo de los bytes que cambian, relectura completa y, si no cuadra, la RAM vuelve al valor
  anterior y se responde `#ERR,EEPROM#` (RF-FW-19). Nada por debajo de 0x100.
- **Máscara de `#V#`** (`:201-222`): bit *k* = coeficientes del código *k* distintos (bit a bit) de los de
  fábrica; bit 12 = factor de temperatura distinto del de `gui.c:41-44`. `CAL` si la máscara no es 0.
- **Órdenes** (`adminProcesarTrama`, `:424-575`): `#V#`, `#L#`, `#Q#`, `#G#`, `#E#`, `#S#`, `#F#`,
  `#GT#`, `#ST#`, `#P#`, `#K#`, `#KC#`. Ninguna mide. Números con `%.8E`. PIN de fábrica `2026`;
  5 fallos seguidos de `#L` o `#P` bloquean `#L` y `#P` hasta apagar; la sesión caduca a los 10 min sin
  tramas `#`. `#ST` rechaza `X_0` fuera de [0,5 ; 1,5] con `#ERR,FORMATO#`.
- **Registro STONE** (`:236-292`): últimas 8 tramas de UART2 (hasta 16 bytes cada una) y contador total
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

### 3.3 Formato numérico: **no cumple O-02** (ver §4)

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
4. **`#ST`:** sólo se limita `X_0`; `X_1` y `X_2` sólo se exigen finitos. Un `X_1` extremo sigue pudiendo
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

- `hex/RetroVertical_V3.6.hex`, su md5 y el informe de memoria en `hex/memoria.txt`.
- md5 de cada fuente en `hex/fuente.md5`.
