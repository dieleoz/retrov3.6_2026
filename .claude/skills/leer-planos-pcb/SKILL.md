---
name: leer-planos-pcb
description: Lee esquematicos y placas del retrorreflectometro Vertical (Eagle XML .sch/.brd, IPC-D-356, Proteus .pdsprj, KiCad si esta instalado, PDF de placa) y levanta el mapeo pin -> red -> componente cruzado con el firmware (pin_manager.c/.h). Usar cuando haya que saber a que va conectado un pin del PIC, que hay en un conector (ICSP, UART, cabezal), si el esquematico y la placa coinciden, si una afirmacion de un documento de hardware es cierta, o antes de escribir o corregir un HARDWARE-*.md. No sirve para decir que la placa fisica es asi: eso solo lo cierra una medida.
---

# Leer planos de PCB y mapear pin -> red -> componente

Todo lo que sale de aqui es **papel contra papel**: fichero de diseno contra firmware. Ningun
resultado de esta skill dice como es la placa montada. Cada tabla que se escriba con ella lleva
**NO VERIFICADO EN PLACA** hasta que alguien la mida con la placa delante.

## 0. Antes de empezar

1. **Arbol correcto.** El repositorio es `D:\IT\P_RetroReflectometro_Vertical` (y la linea V3.6,
   `D:\IT\P_RetroVertical_V3.6`). `D:\@Proyect\IT\...` es una copia: no se edita.
   `git ls-remote --heads origin` y `git log --oneline -1` deben apuntar al mismo commit.
2. **KiCad no esta instalado** en esta maquina (comprobado el 18-sep-2026 con dos herramientas):
   - Bash: `which kicad-cli kicad` no los encuentra; `ls "/c/Program Files/KiCad"` no existe.
   - PowerShell: `Get-Command kicad-cli` vacio; `Test-Path "C:\Program Files\KiCad"` y
     `"C:\Program Files (x86)\KiCad"` dan `False`; las claves `Uninstall` de HKLM/HKCU no listan
     KiCad ni Eagle.
   Repetir las dos comprobaciones en cada sesion antes de afirmar que sigue sin estar. Un "no esta"
   con una sola herramienta no vale (`CLAUDE.md` §2, ultimo caso).
3. **Proteus tampoco** esta instalado: los `.pdsprj` solo se leen de forma heuristica (§4).

## 1. De donde sale el metodo

Se tomo del proyecto hermano `D:\@Proyect\Camion _G` (ESP32, KiCad). Alli **no hay scripts
guardados ni se usa `kicad-cli`**: la conectividad se extrajo leyendo el texto de los ficheros
KiCad. Lo que se hereda es el metodo, no el codigo (`CLAUDE.md` §11):

- **Netlist pad -> red leido del `.kicad_pcb`** y componentes del `.kicad_sch`; los dos ficheros
  deben tener el mismo juego de componentes (`Camion _G\VERIFICACION_ENTRADAS_TARJETA.md:6-9`).
- **Conectividad pura, cero proximidad de coordenadas.** Un extractor union-find sobre extremos de
  segmentos del `.kicad_sch`, contrastado con el netlist del `.kicad_pcb`: dos derivaciones
  independientes que coinciden red por red (`Camion _G\06 conductor\MAPEO_TARJETA_VELOCIMETRO.md:198-211`).
  Cruzar rotulos y pads por su altura dio **dos tablas falsas** (`Camion _G\MAPEO_TARJETA_KICAD.md:217-222`).
- **Control con respuesta conocida y que no salga del propio pleito.** El extractor se valido sobre
  otra tarjeta ya certificada contra `pins.h` antes de creer lo que decia de la tarjeta en duda
  (`MAPEO_TARJETA_VELOCIMETRO.md:207-211`).
- **Cadenas trazadas de extremo a extremo**, bornera -> componentes -> GPIO, pad a pad, en tabla
  (`VERIFICACION_ENTRADAS_TARJETA.md:63-116`).
- **El nombre del pin en el simbolo puede mentir** (simbolo espejado frente al datasheet): el
  emparejado fisico lo manda el datasheet y lo cierra el banco (`VERIFICACION_ENTRADAS_TARJETA.md:48-60`).
- **El cuarto nombre es la serigrafia**: lo que se lee con la placa delante y lo que llega en los
  reportes de campo (`Camion _G\CLAUDE.md:243-253`).
- **Lo no medido va a una tabla de huecos**, no a la de hechos; un "verificado" sobre algo no medido
  es peor que un hueco (`MAPEO_TARJETA_VELOCIMETRO.md:216-236`).

## 2. Reglas de este repositorio que aplican aqui

- **Cada afirmacion con su evidencia**: `archivo:linea` del `.sch`, `.brd`, `.IPC` o del `.c`, o el
  elemento del esquematico (`net MCLR`, `pinref part="JP1" pin="1"`). Los scripts imprimen la linea.
- **Los negativos se verifican con dos herramientas.** "El pin 2 no tiene conexion" exige: el script
  no lo lista **y** `grep 'pinref part="MCU1"'` / `grep 'element="MCU1" pad="2"'` tampoco. Un listado
  vacio es "mi listado no lo ve", no "no existe".
- **Contradicciones marcadas, sin elegir.** Si `.sch` y `.brd`, o placa y firmware, o este mapeo y un
  documento anterior no casan, se escribe la contradiccion con las dos lecturas en la seccion de
  contradicciones abiertas del documento y en `ARQUITECTURA.map`. Se cierra midiendo.
- **NO VERIFICADO** en toda afirmacion sobre la placa fisica que no se haya medido. Que cada equipo de
  campo lleve esta revision de placa tampoco esta comprobado.
- **Nunca por proximidad de coordenadas.** Coordenadas solo para *situar* algo (donde esta JP1 en la
  placa), nunca para deducir que dos cosas estan conectadas.
- **`build/` y `dist/` no se usan** para deducir hardware ni firmware (`CLAUDE.md` §9).

## 3. Eagle XML (`02_Hardware/Retro2025/Retro_smd_v1.sch` y `.brd`, Eagle 9.6.2)

Estructura que importa:

| Fichero | Donde esta la conectividad |
| :-- | :-- |
| `.sch` | `<parts><part name library deviceset device>`; `<nets><net name><segment><pinref part gate pin/>` |
| `.sch` | Pin -> pad: `<libraries>..<deviceset><devices><device package><connects><connect gate pin pad/>` |
| `.brd` | `<elements><element name package>`; `<signals><signal name><contactref element pad/>` |

Todas las `<segment>` de un mismo `<net>` son la misma red (Eagle une por nombre). Los simbolos de
alimentacion (`P+1`, `GND1`...) son partes sin encapsulado: estan en el `.sch` y no en el `.brd`.

Script: `scripts/eagle_netlist.py` (solo biblioteca estandar; el `.brd` se busca junto al `.sch`).

```bash
S=.claude/skills/leer-planos-pcb/scripts
H=02_Hardware/Retro2025/Retro_smd_v1.sch
FW=01_Firmware/18f47k42_RetroV_V4.1.X/mcc_generated_files
python $S/eagle_netlist.py $H --resumen --cruce          # inventario y .sch contra .brd pad a pad
python $S/eagle_netlist.py $H --parte JP1 --parte X4     # tabla pad | pin | red sch | red brd
python $S/eagle_netlist.py $H --red MCU_TX1              # miembros de una red, con lineas
python $S/eagle_netlist.py $H --parte MCU1 --pin-manager $FW   # los 40 pines del PIC con TRIS/ANSEL/WPU/PPS
```

Trampas de Eagle ya vistas en este diseno:

- El pin 40 del PIC se llama `RB7PGD` (sin barra) en la libreria; el 39, `RB6/PGC`. Buscar por pad,
  no por nombre.
- Los conectores Molex (`X4`, `X5`...) tienen **todos los pines llamados `S`**; lo que distingue el pad
  es el *gate* (`-1`, `-2`...). El script lo resuelve por `<connect gate pin pad>`; un `grep pin="S"`
  no sirve.
- Pines con sufijo `@1` (`VDD@1`, `VSS@1`) son el segundo pin del mismo nombre.
- Un pad con red en el `.brd` y sin `pinref` en el `.sch` suele ser un **pin de alimentacion
  implicito** (gate de potencia no invocado). `--cruce` lo lista; se cita como tal, no como error.
- `connect pad="1 2"` (varios pads por pin) existe en Eagle 9; el script los separa.

## 4. Otros formatos

**IPC-D-356 / 356A** (netlist de prueba electrica, junto a los gerbers). Es el unico netlist en texto
de las placas Proteus. `scripts/ipc356_netlist.py FICHERO.IPC --resumen | --parte REF | --red NOMBRE`.
Columnas fijas: red 4-17, referencia 21-26, pin 28-31; `P  NNAMEnnnnn` son alias de nombres largos;
`N/C` es pad sin red; referencia en blanco es una via. Limite: la referencia se trunca a 6 caracteres
(`R16 (PRE..)` llega como `R16 (P`). Retro2025 **no trae** `.IPC` en `gerbers_retro2025/`.

**Proteus `.pdsprj`** (placa V3 "SATLUX H-IoT", `D:\IT\P_RetroVertical_V3.6\02_Hardware\PCB_Proteus_3.3\`).
Es un zip (`PROJECT.XML`, `ROOT.DSN` esquema, `ROOT.LYT` placa, `ROOT.CDB`) con binarios propietarios.

```bash
mkdir -p "$SCRATCH/pds" && unzip -o -q "RUTA.pdsprj" -d "$SCRATCH/pds"   # nunca descomprimir dentro del repo
strings -n 3 "$SCRATCH/pds/ROOT.LYT" | grep -n -A8 PICKIT3                 # registro del conector: ref, encapsulado, redes de sus pads
```

Lo que sale es **heuristico**: se ordenan cadenas por registro de componente (referencia, valor,
redes de los pads en orden). No hay estructura garantizada: registros desplazados, valores con basura
(`0MQ`, `10M?;`), redes anonimas `#00103`. Toda columna sacada asi va marcada **(heuristico)** y no
vale como esquematico (`05_Documentacion/HARDWARE-V3-SATLUX-H-IoT.md:257-262`). Si existe un `.IPC` de
la misma revision, manda el `.IPC`. Cerrarlo de verdad exige Proteus 8.6+ o medir la placa.

**KiCad** (si llega a instalarse). El metodo del camion no lo necesita: el `.kicad_pcb` es texto y
cada pad lleva su `(net N "NOMBRE")`; el `.kicad_sch` exige union-find sobre extremos de cables con
posiciones absolutas de pin (rotacion y espejo). Con KiCad instalado:

```bash
kicad-cli sch export netlist --format kicadxml -o out.xml Retro_smd_v1.kicad_sch
kicad-cli sch export pdf -o esquema.pdf Retro_smd_v1.kicad_sch
kicad-cli sch erc  Retro_smd_v1.kicad_sch       # y pcb drc sobre el .kicad_pcb
kicad-cli --help                                 # comprobar subcomandos: varian entre la 7, la 8 y la 9
```

La importacion de Eagle (`Archivo > Importar > Proyecto no KiCad`) es de la interfaz grafica;
`kicad-cli` no importa Eagle (comprobar con `--help` de la version instalada antes de afirmarlo). Tras
importar, **repetir el cruce**: el netlist del proyecto importado debe dar las mismas redes que
`eagle_netlist.py`; si no, es contradiccion abierta. En `02_Hardware/Retro2025/` ya hay restos de un
intento (`Retro_smd_v1-eagle-import.kicad_sym`, `.kicad_dru`, `sym-lib-table`), sin `.kicad_sch`.

**PDF** (`top pcb.pdf`, `bottom pcb.pdf`, `silkscreen pcb.pdf`: son capas de placa, no esquema).
`pdftotext` y `pypdf`/`pymupdf` estan disponibles. Sirven para **nombres** (serigrafia: `BLUETOOTH`,
`RX`, `TX`, `STONE`...), no para conectividad: el texto de un PDF no dice que esta unido a que. Para
verlo, renderizar la pagina a PNG (`pymupdf`) y leer la imagen. La serigrafia tambien esta en el
`.brd` como `<text>` en las capas 21/25; cruzar el rotulo con la red **por el componente al que
pertenece**, no por su posicion.

## 5. Cruce obligatorio con el firmware

Cada pin del MCU que entre en un documento lleva tambien lo que el firmware configura, de
`mcc_generated_files/pin_manager.c/.h`:

- `TRISx` (1 entrada / 0 salida), `ANSELx` (1 analogico), `WPUx`, `ODCONx`, `LATx`.
- **PPS**: `RxyPPS = 0xNN` saca un periferico por el pin; `<fn>PPS = 0xNN` lo lee desde el pin con
  puerto = `(NN >> 3) & 7` (A..E) y bit = `NN & 7`. El script decodifica el valor y **no se fia del
  comentario** de MCC (lo muestra al lado para que se vea si discrepa).
- Alias de `pin_manager.h` (`#define NOMBRE_TRIS TRISxbits.TRISxn`).

`scripts/pin_manager_pic.py CARPETA [--pin RC4]` da la tabla sola; `eagle_netlist.py --pin-manager`
la une a la de la placa. Luego, **a mano**, el uso real: `grep` del alias o del registro en los `.c`
propios (p. ej. `Serial.c` para UART1). Casos que se deben marcar:

- Pin que el firmware usa y la placa deja sin red (V4: RA0, RA1, RC0, RC1): contradiccion abierta.
- Pin con red en la placa y sin configuracion en el firmware (V4: RB0/RB1/RB2 de `X8`): se anota.
- `TRIS` dice `out` en RE3/MCLR: es el bit del registro; con `MCLRE` activo el pin es MCLR
  (`device_config.c:60` MCLRE, `:94` LVP = ON, que fija MCLR). No leer la columna sin mirar la configuracion.

## 6. Prueba de referencia (`Retro_smd_v1`, 18-sep-2026)

Salida esperada; si cambia sin que haya cambiado el fichero, el script esta roto.

```
--resumen: .sch Eagle 9.6.2, 176 partes (111 con encapsulado), 74 redes, 379 pinref
           .brd 111 elements, 74 signals, 315 contactref; ninguna parte de mas en uno u otro
--cruce:   315 pads con red en .sch, 315 contactref en .brd, 0 diferencias
```

| JP1 pad | Red | sch | brd | | X4 pad | Red | sch | brd |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| 1 | `MCLR` (+ MCU1.1, R1.2) | 24497 | 5922 | | 1 | `MCU_TX` (-> R34.1) | 25317 | 6722 |
| 2 | `+5V` | 23977 | 5358 | | 2 | `MCU_RX1` (MCU1.24 RC5, TP14) | 25323 | 6735 |
| 3 | `GND` | 24172 | 5565 | | 3 | `GND` | 24414 | 5603 |
| 4 | `ICSPDAT` (MCU1.40 `RB7PGD`) | 24509 | 5932 | | 4 | `+5V` | 24081 | 5383 |
| 5 | `ICSPCLK` (MCU1.39 `RB6/PGC`) | 24521 | 5939 | | | | | |

`MCU_TX1` = MCU1.23 (RC4), R34.2, TP15. Firmware: RC4 salida, `RC4PPS` UART1 TX
(`pin_manager.c:138`); RC5 entrada, `U1RXPPS` = RC5 (`pin_manager.c:140`). Coincide con
`05_Documentacion/HARDWARE-V4-Retro_smd_v1.md` §4 y §5, sin contradicciones.
Pines del PIC sin red en `.sch` ni `.brd`: 2, 3, 10, 15, 16, 36 (igual que ese documento §3).

Control del parser IPC: sobre el `.IPC` de 2019 (ruta en `HARDWARE-V3-SATLUX-H-IoT.md:18`,
descomprimido en el scratchpad) `--parte PICKIT` da MCLR, +5V, GND, PGD, PGC y N/C en las lineas
166-171, y `R3` en 172: lo mismo que cita ese documento en su §4.

## 7. Como se escribe el resultado

En `05_Documentacion/HARDWARE-*.md`, siguiendo los dos que ya existen:

1. Primera linea: **que no se ha medido nada sobre la placa**.
2. Inventario de ficheros con formato, version y fecha.
3. Tabla de los 40 pines: pin | puerto | uso en firmware (`archivo:linea`) | red | a donde va.
4. Una seccion por conector, con cadenas trazadas de extremo a extremo.
5. Contradicciones abiertas, numeradas, con las dos lecturas y como se cierran.
6. Huecos: lo que no se puede determinar leyendo, y que medida lo cierra.
7. Estado de las herramientas (KiCad, Proteus) con las dos comprobaciones.

La salida de los scripts es material de trabajo: no se versiona salvo que un documento la necesite.
