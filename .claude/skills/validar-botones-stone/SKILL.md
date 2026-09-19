---
name: validar-botones-stone
description: Cruza los botones y variables de un proyecto de pantalla STONE (.vt de 2.a generacion o .st de 3.a) contra lo que espera el firmware del Retrorreflectometro Vertical, e informa de lo que falta en cada lado. Usar cuando llegue o se cambie un proyecto de pantalla, antes de tocar el tratamiento de tramas de la STONE en el firmware, cuando un boton "no hace nada", o para preparar la comprobacion en el equipo con el registro #K# de la V3.6 o un sniffer.
---

# Validar botones y variables de la pantalla STONE

**Estado: el cruce de archivos esta probado (18-sep-2026); nada de esto se ha medido todavia en el
equipo.** Que el proyecto y el firmware cuadren en papel no dice que la pantalla cargada en un
equipo concreto sea ese proyecto.

La pantalla y el firmware se hablan por **codigos o nombres escritos a mano en los dos lados**. No
hay compilacion cruzada: un codigo o un nombre que no coincide **no da error, el boton simplemente
no hace nada** (o la variable no se pinta). Esta skill convierte ese silencio en una lista.

## Origen del metodo

Tomado del proyecto hermano `D:\@Proyect\Camion _G` (ESP32, STONE de 3.a generacion). **Se adapta
el metodo, no el codigo** (su protocolo y sus nombres no son los nuestros). Alli viven:

- `.claude/skills/verificar-pantalla-stone/SKILL.md`: el `.st` es un ZIP; todo esta en
  `manifest.json` -> `windows{}` -> `widgets[]`. Dos trampas: hay widgets guardados **fuera** de
  `widgets[]` (restos del editor), y `grep -c` sobre el binario no cuenta ocurrencias.
- `01_Firmware/simulacion_pc/banco/packs/contrato_01_widgets.py` y `banco/fuente.py`
  (`widgets_de_pantalla()`): el cruce automatico "todo lo que el firmware invoca existe en la
  pantalla", con **extraccion explicita** (no un grep ingenuo), **suelo de inventario** (si sale
  corto, ABORTA), **control negativo** (renombra un widget en una copia y exige que el cruce lo
  cace) y tres estados: `0 PASS`, `1 FALLA`, `2 ABORTADO`. ABORTADO no es PASS.
- `01_Firmware/camion_pintura/src/Stone.cpp` (`stoneDebugVolcar`, bajo `STONE_DEBUG_RX` de
  `include/config.h:43`): volcado en hex de cada trama recibida, **solo en banco**. Asi midieron
  que la pantalla entrega el toque siempre al mismo widget de un par apilado, cosa que ningun
  archivo podia decir.

## Las dos generaciones del Vertical

| | V3 (2020, V3.6) | V4.1 |
| :--- | :--- | :--- |
| Pantalla | STVA035WT, 2.a gen., binario `A5 5A` | STWA035WT, 3.a gen., JSON `ST<{...}>ET` |
| Proyecto | `.vt` (semitexto) | `.st` (ZIP con `manifest.json`); **el de 2022 no lo tenemos** |
| Boton -> PIC | `KeyCodeReturn`: valor 0x10NN; el PIC lee NN en `bufferPantalla[8]` | evento con el nombre del widget; el PIC busca `"<buttonN>"` |
| PIC -> pantalla | `enviarDatos2(valor, direccion)` a variables `Data variable` | `set_text` a `labelN`, `open_win` a `windowN` |
| UART2 | 9600 8N1 (`uart2.c:106,115,118` + `mcc.c:74` + `device_config.h:50`: 16 MHz, BRGS=1, BRG=416) | 115200 8N1 (`uart2.c:104,113,116`, BRGS=1, BRG=0x8A, Fosc 64 MHz) |

Las citas de la columna V3 son del fuente de 2020, `01_Firmware/base_2020_d089f962/RetroVertical1.X/`;
las de V4.1, de `01_Firmware/18f47k42_RetroV_V4.1.X/` del repositorio del Vertical. Ignorar
`build/` y `dist/`: no corresponden al fuente.

## 1. Cruce de archivos (script)

```bash
S=.claude/skills/validar-botones-stone/scripts
# V3: .vt contra el firmware de 2020
python $S/validar_stone.py v3 \
  --vt 04_Pantalla_STONE/lcd_RetroVertical_2020/lcd_RetroVertical.vt \
  --fw 01_Firmware/base_2020_d089f962/RetroVertical1.X
# V4.1: sin proyecto de pantalla lista lo que la pantalla TIENE que tener y sale ABORTADO (2)
python $S/validar_stone.py v4 --fw <repo Vertical>/01_Firmware/18f47k42_RetroV_V4.1.X
# V4.1 con proyecto: --st <proyecto.st>  o  --manifest <manifest.json ya extraido>
```

En Windows, si la consola no es UTF-8, anteponer `PYTHONIOENCODING=utf-8`. El script solo lee.

**Que extrae del `.vt`** (cada registro va hasta `;`; es lo que se ve con `strings | tr ';' '\n'`):

| Registro | Campos (tras `x1,y1,x2,y2,nombre,tipo,0`) |
| :--- | :--- |
| `KeyCodeReturn` | `DESTINO,-1,65029,PAGINA,0,VALOR,...`; codigo = byte bajo de VALOR |
| `basic` (boton de pagina) | `DESTINO,-1,0,PAGINA,...`; navegacion local, **no manda nada al PIC** |
| `ASCII` (Data variable) | `PAGINA,23056,65535,13,DIRECCION,...` |
| `IncrementAdjust` | `-1,55,65026,PAGINA,0,DIRECCION,...`; +/- local sobre una variable |

El byte que precede al tipo es binario y a veces se pega al nombre (`Ybasic`, `\basic`): el script
normaliza por sufijo. Suelo: si el numero de `KeyCodeReturn:` del archivo no coincide con los
leidos, ABORTA. Los nombres de pagina salen de `IMAGE/` (prefijo numerico = pagina).

**Que extrae del firmware V3:** cada `bufferPantalla[n] == codigo` y cada
`enviarDatos2(..., direccion)` con `archivo:linea` y la funcion que lo contiene; marca
**MUERTA** la llamada cuya funcion no se referencia en ningun otro sitio.

**Que extrae del firmware V4:** los `"widget":"..."` de cada `ST<{...}>ET` (separando `open_win`,
que nombra **ventanas**, de `set_text`, que nombra labels; la misma clave `widget` sirve para las dos
cosas, y confundirlas fue el primer error del pack de Camion _G) y los `strstr(..., "<nombre>")`.
Solo entran los nombres entre `<>`: `Serial2.c:52-58` descarta los bytes `<= 0x10` de la trama
(orden, longitud, valor), de modo que un toque queda como `ST<button1>ET`. Las busquedas sin `<>`
son de otro enlace (`LEERV`, Bluetooth).

**Cruce e informe:** codigo que la pantalla envia y el firmware no atiende (FALLA), codigo que el
firmware espera y ningun boton envia (FALLA), direccion escrita sin variable en la pantalla (FALLA,
o AVISO si es codigo muerto), variable que nadie escribe (AVISO; OK si la gobierna un
`IncrementAdjust`), botones de la pantalla que el firmware no escucha (AVISO). Siempre corre un
**control negativo**: quita un codigo o nombre de una copia del inventario y exige que el cruce lo
acuse; si no lo acusa, el resultado es ABORTADO.

## 2. Resultado de referencia (V3, 18-sep-2026)

`lcd_RetroVertical_2020/lcd_RetroVertical.vt` contra el firmware de 2020: **PASS**, 15 botones de
codigo, 22 de navegacion, 66 registros de variable en 6 direcciones. Si una corrida nueva sobre los
mismos archivos no da esto, se ha roto el script, no el equipo.

| Pagina -> destino | Codigo | Firmware |
| :--- | :--- | :--- |
| 3 -> 23 (entrada OTROS PAPELES) | 0x02 | `ecuacionesCalibracion.c:77` |
| 23 -> 6, 6 -> 8, 8 -> 5, 5 -> 9, 9 -> 7, 7 -> 23 | 0x01, 0x04, 0x05, 0x03, 0x06, 0x02 | `:73,87,92,82,97,77` |
| 3 -> 10 (entrada PAPEL TIPO I) | 0x08 | `:108` |
| 10 -> 12, 12 -> 14, 14 -> 11, 11 -> 15, 15 -> 13, 13 -> 10 | 0x07, 0x0B, 0x0C, 0x0A, 0x0D, 0x08 | `:103,116,120,112,124,108` |
| 19 -> 18 (PRUEBA ADC), zona (0,0)-(26,20) | 0x0E | `:128` (solo `presentarDato()`, sin ecuacion) |

- OTROS PAPELES envia 0x01-0x06; TIPO I envia 0x07, 0x08 y 0x0A-0x0D. **Ningun boton envia 0x09 y
  el firmware no tiene rama para 0x09**: hueco coherente en los dos lados.
- Variables 199, 205, 210, 215 (medidas y promedio) y 207 (bateria): escritas por `gui.c`. La 28417
  (pag. 17, brillo) la gobiernan dos `IncrementAdjust` de la propia pantalla.
- `gui.c:376` escribe la direccion 21554 (0x5432) y la pantalla no la tiene. Ademas
  `enviarDatos2` (`uart_stone.c:109-134`) manda `0x00` y solo el **byte bajo** de la direccion, asi
  que llegaria como 50. Es **codigo muerto**: `chooseUserGeometry()` (`gui.c:365`) no se llama.
- El comentario de `ecuacionesCalibracion.c:128` ("naranja unico que funciona bien") esta copiado
  del de 0x0D: 0x0E **no** es naranja. No fiarse de los comentarios del firmware como significado.

## 3. Validar en el equipo

El archivo dice lo que el proyecto **deberia** enviar; solo el equipo dice lo que envia la pantalla
**cargada**. La del SLV-002 ya se sabe que no es identica al proyecto archivado.

**V3.6: registro `#K#`** (`05_Documentacion/PROTOCOLO-V3.6.md`, "Registro de la pantalla STONE";
**especificado, no implementado todavia**). Por Bluetooth: `#KC#` para vaciar, pulsar **un** boton,
`#K#`, anotar; repetir boton a boton recorriendo las dos cadenas de la seccion 2 y la entrada oculta
a PRUEBA ADC. Decodificar:

```bash
python $S/decodificar_k.py "#K,<n>,<hex1>;<hex2>#" --fw 01_Firmware/base_2020_d089f962/RetroVertical1.X
```

Imprime direccion, valor, el `bufferPantalla[8]` que resulta y la linea del firmware que lo atiende
(o `SIN RAMA`). Criterios: **una sola trama por toque**; el byte 8 coincide con la tabla; el `n` del
registro sube de uno en uno. Si llegan dos tramas juntas, `bufferPantalla[8]` solo ve la primera:
anotarlo, es exactamente el defecto que Camion _G encontro con su volcado.

**Formato esperado de la trama, sin medir:** `A5 5A 06 83 10 NN 01 10 NN` (direccion y valor
0x10NN). Sale de `AnalizarBufferUart2()` en `Serial2.c:279-318` del V4.1, **que nadie llama**: es
una segunda fuente de texto, no una medida. El `#K#` lo confirma o lo desmiente.

**V4.1 y cualquier equipo sin `#K#`:** sniffer pasivo (adaptador USB-serie con solo RX) en la linea
TX de la STONE hacia el PIC, al baudio de la tabla de arriba, volcando en hex. El mismo
`decodificar_k.py` acepta un volcado binario 2.a gen. En 3.a gen buscar `ST<` + nombre + `>ET` y
comprobar que cada `buttonN` de la lista del script aparece al pulsar su boton. Ojo: `transmitUart2`
(`Serial2.c:36`) envia tambien el `\0` final de cada orden, con `x <= strlen`: en el volcado de la
linea PIC -> STONE aparece un `00` tras cada `>ET`, y no es ruido.

## 4. Lo que no se puede validar sin equipo o sin proyecto

- Que la pantalla cargada en cada equipo sea el `.vt` archivado (SLV-002 ya no lo es).
- Que la trama real tenga el formato y la direccion de arriba, y que llegue alineada en
  `bufferPantalla[0]` (el buffer de 2020 no tiene limite ni reinicio comprobado:
  `uart_stone.c:87-95`).
- **V4.1: el cruce entero.** Sin el `.st` de 2022 solo hay la lista de lo que el firmware usa (19
  `buttonN`, `label1`-`label14`, `window1`-`window10`); si la pantalla tiene esos nombres, cuales
  sobran y si algun boton que el operario ve no llega al firmware, solo se sabe con el proyecto o
  con el sniffer.
