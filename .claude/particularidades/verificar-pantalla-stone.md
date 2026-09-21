# Particularidades de la V3.6 para `orquestador:verificar-pantalla-stone`

Sólo 2.ª generación (`.vt`, binario `A5 5A`): en este repositorio **no hay ningún `.st`**. La STONE no se
toca en la V3.6 (`CLAUDE.md` §1): la skill sirve para comprobar que un cambio del firmware no rompe el
contrato con la pantalla y para leer lo que la pantalla envía, no para editar el `.vt`. Mapa:
`ARQUITECTURA.map` §M1 (pantalla) y §M6 (STONE de 2020). Sin cifras vigentes.

## 0. Rutas, baudios y modelo

- Proyecto del firmware de 2020: `04_Pantalla_STONE/lcd_RetroVertical_2020/lcd_RetroVertical.vt`; otros
  `.vt` en `04_Pantalla_STONE/bases/` y `disenos_pantalla_2020/`. Los nombres de página salen de `IMAGE/`
  (prefijo numérico = página).
- Firmware: base 2020 en `01_Firmware/base_2020_d089f962/RetroVertical1.X/`; V3.6 en
  `01_Firmware/RetroVertical_V3.6.X/`. `build/` y `dist/` no corresponden al fuente.
- UART2 (STONE) a **9600 8N1**: `mcc_generated_files/uart2.c:106,115,118` (`U2CON0 = 0xB0`, BRGS=1;
  BRG = 0x01A0 = 416), `mcc.c:74` y `device_config.h:50` (16 MHz). Igual en la base y en la V3.6. No
  confundir con el Bluetooth (UART1, también 9600) ni con la V4.1/V5 (115200).
- Modelo de pantalla: **contradicción abierta** STA035WT-01 frente a STVA035WT(-01) (`CLAUDE.md` §10;
  `ARQUITECTURA.map` §M7). No se elige: se mira la etiqueta del equipo.
- La pantalla cargada en SLV-002 **no es idéntica** al proyecto archivado
  (`05_Documentacion/PROTOCOLO-V3.6.md:64`).

## 1. Cómo se hablan aquí la pantalla y el PIC

- Botón -> PIC: `KeyCodeReturn` con valor `0x10NN`; el PIC compara `bufferPantalla[8]` con `NN`
  (`ecuacionesCalibracion.c:73-128`, base 2020).
- PIC -> pantalla: `enviarDatos2(valor, direccion)` a variables `Data variable` (`uart_stone.c:109-134`);
  manda sólo el **byte bajo** de la dirección.
- Del firmware, el script extrae cada `bufferPantalla[n] == codigo` y cada `enviarDatos2(..., direccion)`
  con `archivo:línea` y la función que lo contiene, y marca **MUERTA** la llamada cuya función no se
  referencia en ningún otro sitio.
- Registros del `.vt` que lee el script (tras `x1,y1,x2,y2,nombre,tipo,0`): `KeyCodeReturn` =
  `DESTINO,-1,65029,PAGINA,0,VALOR,...` (código = byte bajo de VALOR); `basic` = `DESTINO,-1,0,PAGINA,...`
  (navegación local, no manda nada al PIC); `ASCII` (Data variable) = `PAGINA,23056,65535,13,DIRECCION,...`;
  `IncrementAdjust` = `-1,55,65026,PAGINA,0,DIRECCION,...` (+/- local sobre una variable). El byte
  binario que precede al tipo a veces se pega al nombre (`Ybasic`, `\basic`): el script normaliza por
  sufijo. Suelo: si el número de `KeyCodeReturn:` del archivo no coincide con los leídos, ABORTA. (Esto
  es del formato `.vt`, no de la V3.6: candidato a subir a la común.)

## 2. El cruce, aquí

```bash
VT=04_Pantalla_STONE/lcd_RetroVertical_2020/lcd_RetroVertical.vt
python "$S/validar_stone.py" v3 --vt $VT --fw 01_Firmware/base_2020_d089f962/RetroVertical1.X
python "$S/validar_stone.py" v3 --vt $VT --fw 01_Firmware/RetroVertical_V3.6.X
```

`S` es el `scripts/` de la común (`${CLAUDE_SKILL_DIR}/scripts`); fuera de la skill, el clon
`D:/IT/Arquitec_Orquestador/skills/verificar-pantalla-stone/scripts`. Tres estados y control negativo
como dice la común §5.

**Cuando cambie el firmware V3.6, las dos corridas tienen que dar el mismo cruce**; sólo pueden moverse
números de línea. Cualquier otra diferencia es un cambio de contrato con la pantalla, y la STONE no se
toca. `validar_stone.py v4` es para el firmware V4.1 con `.st`: de otras líneas, no de ésta.

## 3. Resultado de referencia (control con respuesta conocida)

Sobre ficheros que no cambian — el `.vt` de 2020 y la base 2020 —, reproducido en dos sesiones distintas
y con los dos firmware; por eso no es una cifra del día. Si una corrida nueva sobre los mismos archivos
no da esto, se ha roto el script, no el equipo.

`RESULTADO: PASS`; 15 botones `KeyCodeReturn`, 22 de navegación, 66 registros de variable en 6
direcciones; 13 códigos presentes en los dos lados.

| Página -> destino | Código | Base 2020, `ecuacionesCalibracion.c` |
| :--- | :--- | :--- |
| 3->23 (entrada OTROS PAPELES) | 0x02 | `:77` |
| 23->6, 6->8, 8->5, 5->9, 9->7, 7->23 | 0x01, 0x04, 0x05, 0x03, 0x06, 0x02 | `:73,87,92,82,97,77` |
| 3->10 (entrada PAPEL TIPO I) | 0x08 | `:108` |
| 10->12, 12->14, 14->11, 11->15, 15->13, 13->10 | 0x07, 0x0B, 0x0C, 0x0A, 0x0D, 0x08 | `:103,116,120,112,124,108` |
| 19->18 (PRUEBA ADC), zona (0,0)-(26,20) | 0x0E | `:128` (sólo `presentarDato()`, sin ecuación) |

- Ningún botón envía 0x09 y el firmware no tiene rama para 0x09: hueco coherente en los dos lados.
- Variables 199, 205, 210, 215 (medidas y promedio) y 207 (batería), escritas por `gui.c`. La 28417
  (brillo) la gobiernan dos `IncrementAdjust` de la propia pantalla.
- `gui.c:376` (base; `:377` en la V3.6) escribe la dirección 21554 (0x5432), que la pantalla no tiene;
  llegaría como 50. Es código muerto: `chooseUserGeometry()` (`gui.c:365` en la base) no se llama.

## 4. Trampas propias

- El comentario de `ecuacionesCalibracion.c:128`, «naranja único que funciona bien», está copiado de
  `:124` (0x0D): 0x0E **no** es naranja, es la entrada a PRUEBA ADC.
- El buffer de 2020 no tiene límite ni reinicio comprobado (`uart_stone.c:87-95`): que la trama llegue
  alineada en `bufferPantalla[0]` es una suposición del cruce, no un hecho.
- El formato esperado de la trama, `A5 5A 06 83 10 NN 01 10 NN`, sale de `AnalizarBufferUart2()` del
  V4.1 (repositorio V5), **que nadie llama**: es una segunda fuente de texto, no una medida. El equipo lo
  confirma o lo desmiente.

## 5. Validar en el equipo: la orden `#K#`

- `#K#` (`05_Documentacion/PROTOCOLO-V3.6.md:57-68`; atendida en
  `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:797,800`) devuelve las últimas 8 tramas recibidas
  por UART2 y el total desde el arranque; `#KC#` lo vacía. Sólo existe en un equipo con un firmware V3.6
  que lleve esa orden: qué lleva cada equipo lo dice `RETOMAR.md`, y se confirma con `#V#` antes.
  **Nunca se envía `#K#` a un equipo no identificado como V3.6** (`CLAUDE.md` §4; en un V3 de 2020 `#V#`
  ya dispara una medida).
- Protocolo: `#KC#`, pulsar **un** botón, `#K#`, anotar; recorrer las dos cadenas de §3 y la entrada
  oculta a PRUEBA ADC. Las tramas se registran en `06_Calibracion/<equipo>/tramas/` sin conversión de
  fin de línea.
- Decodificar: `python "$S/decodificar_k.py" "#K,<n>,<hex1>;<hex2>#" --fw 01_Firmware/RetroVertical_V3.6.X`
  imprime dirección, valor, el `bufferPantalla[8]` que resulta y la línea del firmware que lo atiende (o
  `SIN RAMA`). Criterios: una sola trama por toque; el byte 8 coincide con la tabla de §3; el `n` sube
  de uno en uno. Si llegan dos tramas juntas, `bufferPantalla[8]` sólo ve la primera: anotarlo.
- Equipo sin `#K#` (V3 de 2020): el sniffer pasivo de la común §6, a 9600; `decodificar_k.py` acepta el
  volcado. Aquí no hay volcado en banco: el firmware no se recompila para depurar la pantalla.

## 6. Lo que no se puede validar sin equipo

- Que la pantalla cargada en cada equipo sea el `.vt` archivado (en SLV-002 ya no lo es).
- Que la trama real tenga el formato de §4 y llegue alineada.
- El modelo exacto de la pantalla (contradicción abierta de §0).

## Diferencias con la común

- Sólo 2.ª generación: nada de `.st`, `manifest.json`, `inventario_st.py`, comparar entregas ni
  `--patron`. Si llega un `.st`, se usa la común tal cual.
- Sin volcado en banco: aquí se valida con `#K#` o con sniffer (§5).
- La STONE no se toca: el cruce doble base 2020 / V3.6 es una regresión del contrato, no una revisión
  de la pantalla (§2).
- Añadido: rutas y UART2 con `archivo:línea` (§0), extractores del lado firmware (§1), resultado de
  referencia (§3), trampas (§4), `#K#` y la regla de no enviarlo a ciegas (§5).
- El formato de los registros del `.vt` (§1) es general y no está en la común: candidato a subir.
- Ya no hay copia de los scripts en el repositorio: se usan los de la común.
