# Protocolo Bluetooth del firmware V3.6 — contrato entre firmware y app
Mapa: `ARQUITECTURA.map` §M1 (contrato entre firmware y app, EEPROM).
**Estado (19-sep-2026, revisión 1.2 de las notas):** contrato 1.1 (§4 bis) implementado en el
firmware V3.6.1 (commit `8860445`) y en la app RTV 3.6.4 (commit `090c84c`); **probado en simulador y,
en parte, en SLV-002** (`#V#`, `#G`, `#GT#`, `#E` 60/60, con la V3.6 del 18-sep). Ninguna escritura
(`#S`, `#ST`, `#F`, `#P`) se ha probado todavía en un equipo. Lo que el código hace y el texto no dice
está en §4 ter. Este documento manda sobre el firmware y sobre la app: si uno de los dos lo contradice,
se corrige ese, no este.
*Estado anterior:* «especificación, 18-sep-2026. Nada implementado ni probado todavía.»

**Objetivo de la V3.6:** que la calibración viva en **EEPROM** y se cambie **desde la app, en modo
administrador, sin reprogramar el PIC**. Todo lo demás se comporta igual que el firmware de 2020.

## 1. Compatibilidad: lo que NO cambia

Base: `RetroVertical1.X` de 2020 (`01_Firmware/base_2020_d089f962/`). Enlace UART1 9600 8N1.

| Petición | Respuesta | Igual que 2020 |
| :--- | :--- | :--- |
| `1`-`8`, `a`-`d` (un byte) | mide, aplica la ecuación de ese código, responde `::<entero>` sin terminador y pita | Sí |
| `9` | `:<n>:` nivel de batería | Sí (formato) |
| `e` | mide y responde `::<x>` **sin ecuación** (la lectura interna) | Sí en el fuente; **la variante de SLV-002 no lo tenía** |
| Pantalla STONE, códigos 0x01-0x0E | igual | Sí: **la STONE no se toca** |

La app del cliente y la pantalla actual tienen que seguir funcionando sin cambios.

## 2. Ecuaciones: forma general

Cada uno de los 12 códigos (`1`-`8`, `a`-`d`) usa `R = c3·x³ + c2·x² + c1·x + c0`, con `x` la lectura
interna (ADC promediado + 200, por el factor de temperatura; `measurement.c:208-250`, `gui.c:298-300`).
Las 12 ecuaciones de 2020 (`ecuacionesCalibracion.c:3-42`) caben en esa forma, con ceros donde falte
grado. **Son los valores de fábrica**: los que se cargan si la EEPROM no tiene calibración válida.

## 3. Órdenes nuevas (administración)

Tramas ASCII **entre `#` y `#`**, campos separados por `,`, **como máximo 48 bytes**.
- **Una trama que empieza por `#` nunca dispara una medida.**
- Una trama incompleta se descarta a los 2 s.

| Petición | Respuesta | Requiere admin |
| :--- | :--- | :---: |
| `#V#` | `#V,3.6,<AAAA-MM-DD>,<CAL\|DEF>#`: `CAL` si hay calibración válida en EEPROM, `DEF` si usa las de fábrica | No |
| `#L,<pin>#` | `#OK#` o `#ERR,PIN#`. Desbloquea el modo admin hasta `#Q#` o apagar | — |
| `#Q#` | `#OK#`. Bloquea | No |
| `#G,<k>#` (k = `1`-`8`, `a`-`d`) | `#G,<k>,<c3>,<c2>,<c1>,<c0>#` | No |
| `#S,<k>,<c3>,<c2>,<c1>,<c0>#` | `#OK#` o `#ERR,<motivo>#`. Escribe en RAM y EEPROM | **Sí** |
| `#F,<k>#` o `#F,*#` | `#OK#`. Restaura los coeficientes de fábrica de `k` o de todos | **Sí** |
| `#GT#` | `#GT,<x2>,<x1>,<x0>#`, el factor de temperatura (`gui.c:41-43`) | No |
| `#ST,<x2>,<x1>,<x0>#` | `#OK#` / `#ERR,...#` | **Sí** |
| `#FT#` | `#OK#` / `#ERR,BLOQUEADO#` / `#ERR,EEPROM#`. Firmware 3.6.2: repone el factor de temperatura de fábrica (`gui.c:42-44`) en RAM y EEPROM y limpia el bit 12 de la máscara de `#V#`; el PIN no cambia | **Sí** |
| `#SC,<AAAA-MM-DD>#` o `#SC,NONE#` | `#OK#` / `#ERR,FORMATO#` / `#ERR,BLOQUEADO#` / `#ERR,EEPROM#`. Firmware 3.6.2: graba la fecha de calibración en EEPROM (0x200, registro con CRC). Año 2020-2099, mes 1-12, día según el mes. `NONE` la borra. `#F` y `#FT` no la tocan. El vencimiento lo calcula la app | **Sí** |
| `#GC#` | `#GC,<AAAA-MM-DD>#` o `#GC,NONE#` (EEPROM en blanco, CRC mala o fecha fuera de rango). Firmware 3.6.2 | No |
| `#SN,<serie>#` | `#OK#` / `#ERR,FORMATO#` / `#ERR,BLOQUEADO#` / `#ERR,EEPROM#`. Firmware 3.6.2: graba la serie del equipo en EEPROM (0x1EE, registro con CRC). 1-12 caracteres ASCII 0x20-0x7E sin `#` ni `,`; `NONE` no se admite. La pantalla STONE sigue mostrando `SLH-046` (`gui.c:198`) | **Sí** |
| `#GN#` | `#GN,<serie>#` o `#GN,NONE#` (EEPROM en blanco o CRC mala). Firmware 3.6.2 | No |
| `#P,<pin_actual>,<pin_nuevo>#` | `#OK#` / `#ERR,PIN#`. PIN de 4 dígitos | **Sí** |

**Registro de la pantalla STONE (ingeniería inversa de botones), añadido el 18-sep-2026:**

| Petición | Respuesta | Requiere admin |
| :--- | :--- | :---: |
| `#K#` | `#K,<n>,<trama1>;<trama2>;...#`: las **últimas 8 tramas recibidas de la STONE por UART2**, en hex, de la más antigua a la más reciente, y `n` = total de tramas vistas desde el arranque | No |
| `#KC#` | `#OK#`. Vacía el registro | No |

Motivo: la pantalla de SLV-002 no es idéntica al proyecto STONE archivado. Pulsando cada botón y
leyendo `#K#` se obtiene el código real que envía, que llega a `bufferPantalla[8]`
(`ecuacionesCalibracion.c:73-130`). El registro **no altera** el tratamiento de las tramas. Si la
respuesta pasara de 96 bytes (revisión 1.1; antes decía 48), se envían sólo las más recientes que quepan, o el firmware usa un
límite mayor para respuestas; documentarlo.

**Números:** notación científica con 7 cifras significativas y punto decimal, por ejemplo
`-8.654100E-05`. El firmware acepta también decimal simple (`0.6196681`). `motivo` es texto corto sin
comas: `PIN`, `BLOQUEADO`, `FORMATO`, `EEPROM`.

**PIN de fábrica:** `2026`. Se guarda en EEPROM.

## 4. EEPROM

- Bloque propio desde **0x100**. No pisa las direcciones de `eeprom_manager.h:9-19` (0-52).
- Contenido: marca `V36`, versión, 12 × 4 `float` de 32 bits, 3 `float` de temperatura, PIN de 4
  bytes ASCII y CRC-16.
- **Al arrancar, si la marca o el CRC no cuadran, se cargan los valores de fábrica** y `#V#` responde
  `DEF`. Un chip recién grabado arranca en `DEF` y **mide igual que el de 2020**.

## 4 bis. Revisión 1.1 del contrato (18-sep-2026), tras la SPEC. MANDA sobre lo anterior

La SPEC (`SPEC-V3.6.md` §7) encontró defectos en la versión 1.0. Decisiones:

| Obs. | Problema | Decisión |
| :--- | :--- | :--- |
| **O-01** | `#S` con 4 coeficientes ocupa ~61 bytes > 48 | **Límite de trama `#...#` = 96 bytes. Buffer de recepción = 100 bytes**, con el límite comprobado. *La versión 1.0 decía 48 y 50: retirado* |
| **O-02** | 7 cifras no recuperan el mismo `float` | Coeficientes en **9 cifras significativas** (`%.8E`, p. ej. `-8.65410000E-05`). `#G` devuelve 9 cifras. La app compara con tolerancia de 1 ulp |
| **O-03** | `CAL`/`DEF` es global | `#V,3.6,<fecha>,<CAL\|DEF>,<máscara>#`: máscara hex de 16 bits, bit *i* = código *i* ajustado (orden `1`-`8`, `a`-`d`; bit 12 = temperatura) |
| **O-04** | Un solo CRC; CRC sin definir | **Un registro por código** (4 `float` + CRC) y otro para temperatura y PIN. **CRC-16/CCITT-FALSE** (poli 0x1021, inicio 0xFFFF). Si el CRC de un registro falla, sólo ese código vuelve a fábrica |
| **O-05** | Trama durante una medida | Las tramas `#...#` las analiza la tarea UART al completarse. **Una trama `#` nunca dispara medida ni la borra `clearBuffer()`**. Los bytes sueltos durante una medida se descartan, como en 2020 |
| **O-06** | PIN sin límite | **5 fallos seguidos bloquean `#L` hasta apagar**. El modo admin caduca a los **10 min sin tramas `#`** |
| **O-07** | Fecha de `#V#` | La de compilación (`__DATE__`), en formato `AAAA-MM-DD` |
| **O-08** | `e` pasa por el corte `>4000 → 0` | Se mantiene, igual que 2020. La app lo sabe |
| **O-10** | No hay forma exacta de probar la no regresión | **Orden nueva `#E,<k>,<x>#` → `#E,<k>,<R>#`**: evalúa la ecuación `k` en la `x` dada **sin medir** y devuelve el entero que se enviaría en `::` (con `arreglar_dato`). No requiere admin |

**Forma de evaluar (de la SPEC):** para medir igual que 2020, la ecuación se evalúa **en el mismo
orden que 2020**: `c3·x·x·x + c2·x·x + c1·x + c0`, en `float` de 32 bits, no por Horner. La
conversión final a `unsigned int` se hace como en 2020.

**Defectos de 2020 que se conservan o se corrigen (P-02):** se corrigen sólo los de **memoria**
(`tempSamples[15]` escrito con 16; `nivel[2]` desbordado en `9`; recepción sin límite), sin cambiar
ningún resultado. **Se conservan** los de comportamiento: doble ecuación con el gatillo pulsado, `9`
con su formato y el código 0x09 sin rama. Así la app del cliente ve lo mismo que antes.

## 5. Reglas de seguridad del firmware

- Recepción con límite comprobado (en 2020 no lo había: `uart_module.c:51-57` de la base). Según la
  revisión 1.1 (§4 bis, O-01): tramas `#...#` de **hasta 96 bytes** en un buffer propio de 100; los
  bytes sueltos siguen en el `bufferData[50]` de 2020, con el límite comprobado. *El "50 bytes" de la
  versión 1.0 queda retirado.*
- Las tramas `#...#` se atienden sin medir. Los bytes sueltos siguen el comportamiento de 2020.
- **No se envía nunca nada con `@`**, por coherencia con el resto del producto.
- Bits de configuración **idénticos** a 2020 (`device_config.c`), leídos en SLV-002 como
  `EC FF F7 FF 9F FF FF DF FE FF`, incluida la protección de código. Cambiarlos es una decisión aparte.

## 4 ter. Revisión 1.2 (19-sep-2026): lo que hace el firmware 3.6.1. Nota, no cambio de contrato

Esta sección **no cambia el contrato**: deja escrito lo que el código hace hoy donde el texto anterior
se quedó corto, para que la app y quien lea el contrato no trabajen con cifras viejas. Las decisiones
que sí cambiarían el contrato (P-10, P-11, P-12, P-15 de `SPEC-V3.6.md` §10) siguen siendo del
propietario. Citas del commit `8860445` (firmware V3.6.1, `.hex` md5 `8736c05d…`).

| Punto | Texto vigente del contrato | Lo que hace la 3.6.1 | Evidencia |
| :--- | :--- | :--- | :--- |
| Límite de trama | §3 dice 48; §4 bis, 96 | 96 bytes con los dos `#`, en un buffer propio de 100 | `uart_module.c:61-62,87-88` |
| Números | §3 dice 7 cifras; §4 bis, 9 y "1 ulp" | 9 cifras (`%.8E`), pero la conversión de XC8 2.10 no es exacta: `%.8E` hasta 3 ulp; ida y vuelta `#S` → `#G` hasta 5 ulp en simulador y 7 con la emulación validada. **1 ulp no es alcanzable con esta biblioteca.** La app compara `#G` a 4 ulp y `#G` tras `#S` a 8, y decide por `#E` | `calibracion_v36.c:354-364,451-455`; `CAMBIOS-V3.6.md` §7.2; `Ecuacion.java:92-93` (app 3.6.4) |
| `#ST` | "`#OK#` / `#ERR,...#`" | `#ERR,FORMATO#` si `F(T) = X_2·T² + X_1·T + X_0` no es finito o sale de [0,5 ; 1,5] en algún `T` de 0 a 831 (extremos y vértice) | `calibracion_v36.c:301-307,373-389,620-621` |
| `#S` | "`#OK#` o `#ERR,<motivo>#`" | `#ERR,FORMATO#`, sin tocar RAM ni EEPROM, si `R(x)` no es finito o sale de [0 ; 4000] en algún `x` de 600 a 4300 (bordes y puntos críticos). **La curva de fábrica del código `2` no pasa** (negativa desde x = 4175); `#F,2#` sí la repone | `calibracion_v36.c:309-319,404-422,587-588`; `CAMBIOS` §7.1 |
| `#ST` con valores de fábrica | — | No devuelve `DEF`: `strtod` no recupera los `float` de ROM y la máscara queda en `CAL,1000`. En la 3.6.1 no hay orden que devuelva la temperatura a fábrica | `CAMBIOS` §7.3; `calibracion_v36.c:215-220` |
| `#V#` | `#V,3.6,<fecha>,<CAL\|DEF>,<máscara>#` | La 3.6 y la 3.6.1 responden `3.6`; sólo las distingue la fecha de compilación (`2026-09-18` / `2026-09-19`). Abierta en `SPEC-V3.6.md` C-37 | `calibracion_v36.h:16`; `calibracion_v36.c:458-475` |
