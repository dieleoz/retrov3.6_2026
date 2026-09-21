---
name: verificar-pantalla-stone
description: Cruza el proyecto de pantalla STONE de 2.a generacion de los V3 (.vt, binario A5 5A, 04_Pantalla_STONE/) contra lo que espera el firmware del PIC (base 2020 o V3.6) — que codigo envia cada boton KeyCodeReturn y que rama lo atiende, que variables escribe el firmware y cuales tiene la pantalla — e interpreta el registro de tramas de la orden K de la V3.6 o un volcado de sniffer. Usar cuando llegue o se revise un proyecto de pantalla, antes de tocar el tratamiento de tramas de la STONE en el firmware, cuando un boton "no hace nada" o una variable no se pinta, o para preparar y leer la comprobacion boton a boton en el equipo. En la V3.6 la STONE no se toca.
---

# Verificar la pantalla STONE (V3, 2.a generacion)
Mapa: `ARQUITECTURA.map` §M1 (pantalla) y §M6 (STONE de 2020).
**Todo lo que sale del cruce de archivos es papel contra papel.** Que el `.vt` y el firmware cuadren
no dice que la pantalla **cargada** en un equipo concreto sea ese proyecto: la de SLV-002 ya se sabe
que no es identica al proyecto archivado (`05_Documentacion/PROTOCOLO-V3.6.md:64`). Eso solo lo dice
el equipo (seccion 5).

La pantalla y el firmware se hablan por **codigos escritos a mano en los dos lados**. No hay
compilacion cruzada: un codigo que no coincide **no da error** — el boton no hace nada, la variable
no se pinta. Esta skill convierte ese silencio en una lista.

## 0. Lo propio de esta linea

- **La STONE no se toca en la V3.6** (`CLAUDE.md` §1): la V3.6 mantiene el comportamiento de 2020
  para que la pantalla siga funcionando. Esta skill sirve para **comprobar** que un cambio del firmware
  no rompe el contrato con la pantalla, y para leer lo que la pantalla envia; no para editar el `.vt`.
- **Modelo de pantalla: contradiccion abierta, sin cerrar** (`HISTORIA.md`, errores): `HISTORIA.md:16`
  dice **STA035WT-01** (y el `README.md` lo decia hasta el 21-sep; `ARQUITECTURA.map` §M7 lo recoge como contradiccion);
  `04_Pantalla_STONE/SOFTWARE-STONE.md:14` y `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md:8`
  dicen **STVA035WT(-01)**. Esta skill no elige: se mira la etiqueta del equipo.
- **Proyectos**: `04_Pantalla_STONE/lcd_RetroVertical_2020/lcd_RetroVertical.vt` (el del firmware de
  2020); otros `.vt` en `04_Pantalla_STONE/bases/` y `disenos_pantalla_2020/`. En este repositorio
  **no hay `.st`** (3.a generacion).
- **Firmware**: base 2020 en `01_Firmware/base_2020_d089f962/RetroVertical1.X/`; V3.6 en
  `01_Firmware/RetroVertical_V3.6.X/`. Ignorar `build/` y `dist/`: no corresponden al fuente.
- **UART2 (STONE) a 9600 8N1**: `mcc_generated_files/uart2.c:106,115,118` (`U2CON0 = 0xB0`, BRGS=1;
  BRG = 0x01A0 = 416), `mcc.c:74` y `device_config.h:50` (16 MHz). Igual en la base y en la V3.6. No
  confundir con el Bluetooth (UART1, tambien 9600) ni con la V4.1/V5 (115200).

**Extraer siempre al scratchpad, nunca dentro del repositorio.**

## 1. La 2.a generacion

| | V3 (2020 y V3.6) |
| :--- | :--- |
| Proyecto | `.vt` (semitexto, registros hasta `;`; se ven con `strings` partiendo por `;`) |
| Boton -> PIC | `KeyCodeReturn`: valor 0x10NN; el PIC compara `bufferPantalla[8]` con NN (`ecuacionesCalibracion.c:73-128`) |
| PIC -> pantalla | `enviarDatos2(valor, direccion)` a variables `Data variable` (`uart_stone.c:109-134`) |

Que extrae el script del `.vt` (tras `x1,y1,x2,y2,nombre,tipo,0`):

| Registro | Campos |
| :--- | :--- |
| `KeyCodeReturn` | `DESTINO,-1,65029,PAGINA,0,VALOR,...`; codigo = byte bajo de VALOR |
| `basic` (boton de pagina) | `DESTINO,-1,0,PAGINA,...`; navegacion local, **no manda nada al PIC** |
| `ASCII` (Data variable) | `PAGINA,23056,65535,13,DIRECCION,...` |
| `IncrementAdjust` | `-1,55,65026,PAGINA,0,DIRECCION,...`; +/- local sobre una variable |

El byte que precede al tipo es binario y a veces se pega al nombre (`Ybasic`, `\basic`): el script
normaliza por sufijo. **Suelo**: si el numero de `KeyCodeReturn:` del archivo no coincide con los
leidos, ABORTA. Los nombres de pagina salen de `IMAGE/` (prefijo numerico = pagina).

Del firmware extrae cada `bufferPantalla[n] == codigo` y cada `enviarDatos2(..., direccion)` con
`archivo:linea` y la funcion que lo contiene, y marca **MUERTA** la llamada cuya funcion no se
referencia en ningun otro sitio.

## 2. Cruce de archivos (script)

```bash
S=.claude/skills/verificar-pantalla-stone/scripts
VT=04_Pantalla_STONE/lcd_RetroVertical_2020/lcd_RetroVertical.vt
python $S/validar_stone.py v3 --vt $VT --fw 01_Firmware/base_2020_d089f962/RetroVertical1.X
python $S/validar_stone.py v3 --vt $VT --fw 01_Firmware/RetroVertical_V3.6.X
```

En Windows, si la consola no es UTF-8, anteponer `PYTHONIOENCODING=utf-8`. El script solo lee.
Tres estados: `0` PASS, `1` FALLA, `2` ABORTADO; **ABORTADO no es PASS**.

Lo que acusa: codigo que la pantalla envia y el firmware no atiende (FALLA), codigo que el firmware
espera y ningun boton envia (FALLA), direccion escrita sin variable en la pantalla (FALLA, o AVISO si
la funcion que la escribe no se llama: codigo muerto), variable que nadie escribe (AVISO; OK si la
gobierna un `IncrementAdjust`), botones que el firmware no escucha (AVISO). Siempre corre un
**control negativo**: quita un codigo de una copia del inventario y exige que el cruce lo acuse; si
no, ABORTADO.

**Cuando cambie el firmware V3.6**, las dos corridas (base y V3.6) tienen que dar el mismo cruce; solo
pueden moverse numeros de linea. Cualquier otra diferencia es un cambio de contrato con la pantalla,
y la STONE no se toca.

(`validar_stone.py v4` existe para el firmware V4.1 con `.st`; es de otras lineas, no de esta.)

## 3. Resultado de referencia (control con respuesta conocida)

Sobre ficheros que no cambian — el `.vt` de 2020 y la base 2020 —, por eso no es una cifra del dia.
Obtenido el 18-sep-2026 y **reproducido el 21-sep-2026** con los dos firmware. Si una corrida nueva
sobre los mismos archivos no da esto, **se ha roto el script, no el equipo**.

`RESULTADO: PASS`, 15 botones `KeyCodeReturn`, 22 de navegacion, 66 registros de variable en 6
direcciones; 13 codigos presentes en los dos lados.

| Pagina -> destino | Codigo | Base 2020, `ecuacionesCalibracion.c` |
| :--- | :--- | :--- |
| 3 -> 23 (entrada OTROS PAPELES) | 0x02 | `:77` |
| 23 -> 6, 6 -> 8, 8 -> 5, 5 -> 9, 9 -> 7, 7 -> 23 | 0x01, 0x04, 0x05, 0x03, 0x06, 0x02 | `:73,87,92,82,97,77` |
| 3 -> 10 (entrada PAPEL TIPO I) | 0x08 | `:108` |
| 10 -> 12, 12 -> 14, 14 -> 11, 11 -> 15, 15 -> 13, 13 -> 10 | 0x07, 0x0B, 0x0C, 0x0A, 0x0D, 0x08 | `:103,116,120,112,124,108` |
| 19 -> 18 (PRUEBA ADC), zona (0,0)-(26,20) | 0x0E | `:128` (solo `presentarDato()`, sin ecuacion) |

- **Ningun boton envia 0x09 y el firmware no tiene rama para 0x09**: hueco coherente en los dos lados.
- Variables 199, 205, 210, 215 (medidas y promedio) y 207 (bateria), escritas por `gui.c`. La 28417
  (brillo) la gobiernan dos `IncrementAdjust` de la propia pantalla.
- `gui.c:376` (base; `:377` en la V3.6) escribe la direccion 21554 (0x5432) y la pantalla no la tiene;
  `enviarDatos2` manda solo el **byte bajo** (llegaria como 50). Es **codigo muerto**:
  `chooseUserGeometry()` (`gui.c:365` en la base) no se llama.

## 4. Las trampas

**a) No fiarse de los comentarios del firmware como significado de un codigo**: se copian de la rama
de al lado. `ecuacionesCalibracion.c:128` dice «naranja unico que funciona bien», copiado de `:124`
(0x0D): 0x0E **no** es naranja, es la entrada a PRUEBA ADC.

**b) `grep -c` sobre un binario no cuenta ocurrencias**: da 1 con diez coincidencias. Contar sobre el
texto extraido y con delimitadores.

**c) El buffer de 2020 no tiene limite ni reinicio comprobado** (`uart_stone.c:87-95`): que la trama
llegue alineada en `bufferPantalla[0]` es una suposicion del cruce, no un hecho.

**d) Una segunda fuente de texto no es una medida.** El formato esperado de la trama,
`A5 5A 06 83 10 NN 01 10 NN`, sale de `AnalizarBufferUart2()` del V4.1 (repositorio V5), **que nadie
llama**: el equipo lo confirma o lo desmiente.

## 5. Validar en el equipo

El archivo dice lo que el proyecto **deberia** enviar; solo el equipo dice lo que envia la pantalla
**cargada**.

**Registro `#K#` de la V3.6** (`PROTOCOLO-V3.6.md:57-68`; atendido en
`01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:797,800`). Devuelve las ultimas 8 tramas
recibidas por UART2 y el total visto desde el arranque; `#KC#` lo vacia. Solo existe en un equipo que
lleve un firmware V3.6 con esa orden: **que lleva cada equipo lo dice `RETOMAR.md`**, y se confirma
con `#V#` antes. **Nunca se envia `#K#` a un equipo no identificado como V3.6** (`CLAUDE.md` §4: nunca
un byte cuyo efecto no se conozca; en un V3 de 2020 `#V#` ya dispara una medida).

Protocolo: `#KC#`, pulsar **un** boton, `#K#`, anotar; repetir boton a boton recorriendo las dos
cadenas de la seccion 3 y la entrada oculta a PRUEBA ADC. Registrar las tramas en
`06_Calibracion/<equipo>/tramas/` sin conversion de fin de linea. Decodificar:

```bash
python $S/decodificar_k.py "#K,<n>,<hex1>;<hex2>#" --fw 01_Firmware/RetroVertical_V3.6.X
```

Imprime direccion, valor, el `bufferPantalla[8]` que resulta y la linea del firmware que lo atiende
(o `SIN RAMA`). Criterios: **una sola trama por toque**; el byte 8 coincide con la tabla; el `n` sube
de uno en uno. Si llegan dos tramas juntas, `bufferPantalla[8]` solo ve la primera: anotarlo.

**Equipo sin `#K#` (V3 de 2020):** sniffer pasivo (adaptador USB-serie **con solo RX**) en la linea TX
de la STONE hacia el PIC, a 9600, volcando en hex; `decodificar_k.py` acepta el volcado. No requiere
reprogramar ni enviar nada al equipo.

## 6. Lo que no se puede validar sin equipo

- Que la pantalla cargada en cada equipo sea el `.vt` archivado (SLV-002 ya no lo es).
- Que la trama real tenga el formato de §4 d y llegue alineada (§4 c).
- El modelo exacto de la pantalla (contradiccion abierta de §0).

## 7. Reportar

Una fila por codigo o direccion cruzada, con la **evidencia** al lado del veredicto (`archivo:linea`
del firmware, pagina y zona del `.vt`, trama del registro). Lo no comprobable sin equipo va a una
lista aparte, no a la de hechos. Si queda algo pendiente, un envio **nuevo** con solo lo que falta
(skill `entregar`).

## Diferencias con la comun

- Quitado: §0 `.claude/particularidades/verificar-pantalla-stone.md` — lo propio va aqui (§0).
- Quitado: 3.a generacion (`.st`, `manifest.json`, `inventario_st.py` inventario/comparar/cruzar, widgets fuera de `widgets[]`, `open_win`/`set_text`, legibilidad, pares superpuestos) — no hay ningun `.st` en este repo (`git ls-files`); si llega uno, traer esas secciones y el script de la comun. Por eso C6 no tiene objeto aqui.
- Quitado: `inventario_st.py` — mismo motivo.
- Quitado: el ejemplo de volcado en banco por UART de depuracion — aqui la STONE no se toca y el firmware no se recompila para depurar la pantalla; se usa `#K#` o sniffer.
- Anadido: «La STONE no se toca en la V3.6» y la contradiccion abierta del modelo — `CLAUDE.md` §1 y §10.
- Anadido: rutas, UART2 a 9600 con `archivo:linea`, y el cruce doble base 2020 / V3.6 como regresion del contrato.
- Anadido: resultado de referencia de la activa `validar-botones-stone`, reproducido el 21-sep-2026 (control con respuesta conocida sobre ficheros inmutables).
- Anadido: `#K#`/`#KC#` de la V3.6 (ya implementado en `calibracion_v36.c:797,800`; la activa decia «no implementado todavia») y la regla de no enviarlo a un equipo no identificado — `CLAUDE.md` §4.
- Cambiado: nombre `validar-botones-stone` -> `verificar-pantalla-stone` (C7).
- Cambiado: ruta de scripts `~/.claude/skills/...` -> `.claude/skills/verificar-pantalla-stone/scripts` (skill de proyecto).
