---
name: leer-planos-pcb
description: Lee esquematicos y placas (Proteus .pdsprj de la placa del V3, IPC-D-356, Eagle XML .sch/.brd, KiCad, PDF de placa) y levanta el mapeo pin -> red -> componente cruzado con el firmware V3.6 (mcc_generated_files/pin_manager.c/.h del PIC18F47K42). Usar cuando haya que saber a que va un pin del PIC, que hay en un conector (ICSP, UART1 del Bluetooth, UART2 de la STONE), si la placa de un equipo es la SATLUX H-IoT o una Retro_smd_v1 antes de grabarle la V3.6, si una afirmacion de un documento de hardware es cierta, o antes de escribir un documento de hardware. No sirve para decir que la placa fisica es asi; eso solo lo cierra una medida.
---

# Leer planos de PCB y mapear pin -> red -> componente
Mapa: `ARQUITECTURA.map` §M1 (equipo) y §M6 (hardware).
Todo lo que sale de aqui es **papel contra papel**: fichero de diseno contra firmware. Ningun
resultado dice como es la placa montada. Cada tabla escrita con esta skill lleva **NO VERIFICADO EN
PLACA** hasta que alguien la mida con la placa delante.

## 0. Antes de empezar

1. **Arbol correcto** (`CLAUDE.md` §1). Este repositorio es `D:\IT\P_RetroVertical_V3.6`. El de la
   V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`) y el de la V4.6 se **consultan en solo lectura**:
   nada de aqui se escribe alli. `D:\@Proyect\IT\P_RetroReflectometro_Vertical` es una copia
   desfasada. `git ls-remote --heads origin` y `git log --oneline -1` deben apuntar al mismo commit.
2. **Que hay en este arbol** (comprobado con dos vias el 21-sep-2026: `git ls-files` y `find`/`Glob`):
   - **Ningun `.sch`, `.brd`, `.kicad_*` ni `.IPC`.**
   - Un solo diseno: `02_Hardware/PCB_Proteus_3.3/Retro Básico Horizontal v3.2 ..pdsprj`. Por su
     nombre es el diseno del **Horizontal**; `ARQUITECTURA.map` §M6 (nodo HW0_PCB) lo registra asi, con la cita de
     `LEAME_original_2020.txt` («el circuito ... es el mismo practicamente»). Que sea exactamente la
     placa «SATLUX H-IoT» de los equipos V3 **no esta comprobado**: tratarlo como lectura heuristica
     y marcarlo.
   - El mapeo de la placa H-IoT (`HARDWARE-V3-SATLUX-H-IoT.md`) y el de la `Retro_smd_v1` viven en
     el repositorio V5 (`README.md:79`), no aqui.
3. **Herramientas, comprobadas con dos vias en cada sesion** (p. ej. `which kicad-cli` en Bash **y**
   `Get-Command kicad-cli` / `Test-Path "C:\Program Files\KiCad"` en PowerShell). Un «no esta» con
   una sola herramienta no vale (`CLAUDE.md` §2). A 18-sep-2026 ni KiCad ni Proteus estaban
   instalados en esta maquina; repetir la comprobacion antes de afirmarlo.

## 1. El metodo

- **Conectividad pura, cero proximidad de coordenadas.** Coordenadas solo para *situar* algo, nunca
  para deducir que dos cosas estan unidas. Cruzar rotulos y pads por su altura dio **dos tablas
  falsas** en otro proyecto.
- **Dos derivaciones independientes que coincidan red por red**: netlist del esquematico contra
  netlist de la placa. Los dos ficheros deben tener el mismo juego de componentes.
- **Control con respuesta conocida, que no salga del propio pleito**: validar el extractor sobre una
  placa ya comprobada antes de creer lo que dice de la placa en duda (seccion 6).
- **Cadenas trazadas de extremo a extremo**, conector -> componentes -> pin del PIC, pad a pad.
- **El nombre del pin en el simbolo puede mentir**: el emparejado fisico lo manda el datasheet y lo
  cierra la medida.
- **El cuarto nombre es la serigrafia**: se cruza con la red **por el componente al que pertenece**,
  no por posicion. Y la serigrafia tampoco identifica la placa: «v4.0» en la serigrafia no quiere
  decir «placa H-IoT» (`01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md:30-31`).
- **Lo no medido va a una tabla de huecos**, no a la de hechos.

## 2. Reglas

- **Cada afirmacion con su evidencia**: `archivo:linea` del fichero de diseno o del fuente, o el
  elemento (`net MCLR`, `pinref part="JP1" pin="1"`). Los scripts imprimen la linea.
- **Los negativos se verifican con dos herramientas.** «El pin 2 no tiene conexion» exige que el
  script no lo liste **y** que un `grep` directo tampoco. Un listado vacio es «mi listado no lo ve».
- **Contradicciones marcadas, sin elegir** (`CLAUDE.md` §2): las dos lecturas en «Contradicciones
  abiertas» de `ROADMAP.md` y, si es del sistema, en `ARQUITECTURA.map`. Se cierra midiendo.
- **Que cada equipo de campo lleve esta revision de placa tampoco esta comprobado**: se dice.
- **`build/` y `dist/` no se usan** para deducir hardware ni firmware (`CLAUDE.md` §10).

## 3. Identificar la placa antes de grabar la V3.6

Es el uso que mas importa en este repositorio, porque **grabar es irreversible** (`CLAUDE.md` §3).
La V3.6 saca UART1 (Bluetooth) por **RC1 (TX) y RC0 (RX)**
(`01_Firmware/RetroVertical_V3.6.X/mcc_generated_files/pin_manager.c:92,95`); la `Retro_smd_v1` de
la V4.1 lleva UART1 en **RC4/RC5**. Grabar la V3.6 en una placa V4 **dejaria el Bluetooth sin
conexion** (`01_Firmware/lecturas_equipos/V3-2/LEEME.md:7`). Caso real: el segundo equipo «V3»
resulto ser un V4 y va a la V4.6 (`HISTORIA.md`, errores).

Identificar empieza por **micro, placa y pantalla** con el equipo delante (`RUNBOOK.md`, fase 0),
nunca por los textos de la pantalla. Lo que sale de los planos ayuda a saber **que medir** (que pin
del PIC va al modulo Bluetooth), no sustituye la medida con polimetro.

## 4. Formatos

**Proteus `.pdsprj`** (el unico diseno de este arbol): un zip (`PROJECT.XML`, `ROOT.DSN` esquema,
`ROOT.LYT` placa, `ROOT.CDB`) con binarios propietarios. Se extrae **al scratchpad, nunca dentro del
repositorio**:

```bash
mkdir -p "$SCRATCH/pds" && unzip -o -q "02_Hardware/PCB_Proteus_3.3/Retro Básico Horizontal v3.2 ..pdsprj" -d "$SCRATCH/pds"
strings -n 3 "$SCRATCH/pds/ROOT.LYT" | grep -n -A8 PICKIT3     # registro del conector: ref, encapsulado, redes
```

Lo que sale es **heuristico** (registros desplazados, valores con basura, redes anonimas `#00103`):
toda columna asi va marcada **(heuristico)** y no vale como esquematico. Si existe un `.IPC` de la
misma revision, manda el `.IPC`. Cerrarlo exige Proteus 8.6+ o medir la placa.

**IPC-D-356 / 356A** (netlist de prueba electrica, junto a los gerbers; a veces el unico netlist en
texto de una placa Proteus):
`python $S/ipc356_netlist.py FICHERO.IPC --resumen | --parte REF | --red NOMBRE`.
Columnas fijas: red 4-17, referencia 21-26, pin 28-31; `P  NNAMEnnnnn` son alias de nombres largos;
`N/C` es pad sin red; referencia en blanco es una via. La referencia se trunca a 6 caracteres. El
`.IPC` de 2019 de la placa V3 esta fuera de este arbol (ruta en `HARDWARE-V3-SATLUX-H-IoT.md:18`,
repositorio V5); se descomprime en el scratchpad.

**Eagle XML** (no hay en este arbol; si llega uno, o para el control de la seccion 6):

| Fichero | Donde esta la conectividad |
| :-- | :-- |
| `.sch` | `<parts><part>`; `<nets><net name><segment><pinref part gate pin/>`; pin -> pad en `<connect gate pin pad/>` |
| `.brd` | `<elements><element>`; `<signals><signal name><contactref element pad/>` |

```bash
S=.claude/skills/leer-planos-pcb/scripts
python $S/eagle_netlist.py <fichero.sch> --resumen --cruce          # inventario y .sch contra .brd
python $S/eagle_netlist.py <fichero.sch> --parte JP1 --parte X4     # pad | pin | red sch | red brd
python $S/eagle_netlist.py <fichero.sch> --red <NOMBRE>             # miembros de una red
python $S/eagle_netlist.py <fichero.sch> --parte MCU1 --pin-manager <carpeta mcc_generated_files>
```

Trampas de Eagle ya vistas: nombres de pin inconsistentes (`RB7PGD` sin barra, `RB6/PGC` con ella:
buscar por pad); conectores Molex con todos los pines llamados `S` (distingue el *gate*); sufijo `@1`
= segundo pin del mismo nombre; pad con red en `.brd` y sin `pinref` en `.sch` suele ser alimentacion
implicita; `connect pad="1 2"` existe en Eagle 9.

**KiCad** (no instalado a 18-sep-2026): el `.kicad_pcb` es texto y cada pad lleva
`(net N "NOMBRE")`; el `.kicad_sch` exige union-find sobre extremos de cable. Con KiCad instalado,
`kicad-cli sch export netlist --format kicadxml`; `kicad-cli` no importa Eagle (comprobar con
`--help`), y tras importar **se repite el cruce**.

**PDF de placa**: sirve para **nombres** (serigrafia), no para conectividad. `pdftotext` para el
texto; para verlo, renderizar a PNG (`pymupdf`) y leer la imagen.

## 5. Cruce obligatorio con el firmware

Cada pin del PIC que entre en un documento lleva tambien lo que el firmware configura, de
`mcc_generated_files/pin_manager.c/.h` (V3.6: `01_Firmware/RetroVertical_V3.6.X/mcc_generated_files/`;
base 2020: `01_Firmware/base_2020_d089f962/RetroVertical1.X/mcc_generated_files/`):

```bash
python $S/pin_manager_pic.py 01_Firmware/RetroVertical_V3.6.X/mcc_generated_files [--pin RC0]
```

- `TRISx` (1 entrada / 0 salida), `ANSELx` (1 analogico), `WPUx`, `ODCONx`, `LATx`, alias de
  `pin_manager.h`.
- **PPS**: `RxyPPS = 0xNN` saca un periferico por el pin; `<fn>PPS = 0xNN` lo lee desde el pin con
  puerto `(NN >> 3) & 7` y bit `NN & 7`. El script decodifica el valor y **no se fia del comentario
  de MCC** (lo muestra al lado).
- `TRIS` = salida en el pin de MCLR es solo el bit del registro: con `MCLRE` activo el pin es MCLR.
  Mirar `device_config.c` antes de leer la columna.

Despues, **a mano**, el uso real: `grep` del alias o del registro en los `.c` propios. Se marcan
siempre: pin que el firmware usa y la placa deja sin red (**contradiccion abierta**); pin con red y
sin configuracion en el firmware (se anota); entrada configurada que ningun codigo lee (hueco, skill
`verificar`). Si la V3.6 cambia `pin_manager.c` respecto a la base 2020, el `diff` de los dos va en el
documento: la placa es la misma.

## 6. Control con respuesta conocida

Como en este arbol no hay Eagle, el control de `eagle_netlist.py` es la `Retro_smd_v1` del
repositorio V5, en solo lectura:
`D:\IT\P_RetroReflectometro_Vertical\02_Hardware\Retro2025\Retro_smd_v1.sch`. Referencia (18-sep-2026,
reproducida el 21-sep-2026): `.sch` Eagle 9.6.2, 176 partes (111 con encapsulado), 74 redes, 379
pinref; `.brd` 111 elements, 74 signals, 315 contactref; `--cruce` 0 diferencias. **Si cambia sin
que haya cambiado el fichero, el script esta roto.**

Control de `pin_manager_pic.py` sobre la V3.6: UART1 TX en RC1 (`pin_manager.c:92`), RX en RC0
(`:95`); UART2 RX en RB1 (`:90`), TX en RB0 (`:91`).

## 7. Como se escribe el resultado

1. Primera linea: **que no se ha medido nada sobre la placa**.
2. Inventario de ficheros con formato, version y fecha, y de que repositorio sale cada uno.
3. Tabla de pines del PIC: pin | puerto | uso en firmware (`archivo:linea`) | red | a donde va.
4. Una seccion por conector, con cadenas trazadas de extremo a extremo.
5. Contradicciones abiertas, numeradas, con las dos lecturas y como se cierran.
6. Huecos: lo que no se puede determinar leyendo, y que medida lo cierra.
7. Estado de las herramientas, con las dos comprobaciones.

Documentacion en espanol, sin emojis (`CLAUDE.md` §10). La salida de los scripts es material de
trabajo: no se versiona salvo que un documento la necesite.

## Diferencias con la comun

- Quitado: §0.1 `.claude/particularidades/leer-planos-pcb.md` — lo propio va aqui.
- Cambiado: §0.2 arbol correcto con los repositorios de esta maquina y la regla de no escribir en los otros — `CLAUDE.md` §1.
- Anadido: §0.2 inventario de lo que hay en este arbol (solo un `.pdsprj`, del Horizontal por nombre; sin `.sch`/`.brd`/`.IPC`), comprobado con dos vias.
- Anadido: §3 identificar la placa antes de grabar (UART1 RC0/RC1 frente a RC4/RC5, caso del segundo equipo) — `CLAUDE.md` §3 y §10, `V3-2/LEEME.md:7`.
- Cambiado: orden de §4 — Proteus e IPC primero (los de esta placa), Eagle y KiCad despues y abreviados.
- Quitado: cruce ESP32/Arduino de §5 — aqui solo hay PIC.
- Cambiado: §6 resultado de referencia — la `Retro_smd_v1` pasa a ser el control del script (fuera del arbol, solo lectura), y se anade el control de `pin_manager_pic.py` sobre la V3.6.
- Quitado: de la activa previa, la tabla JP1/X4 de `Retro_smd_v1`, las rutas `02_Hardware/Retro2025/` y `18f47k42_RetroV_V4.1.X` como si fueran de este arbol, y la cita a `HARDWARE-V3-SATLUX-H-IoT.md` como `05_Documentacion/` local — no existen aqui (son del repositorio V5).
- Cambiado: ruta de scripts `~/.claude/skills/...` -> `.claude/skills/leer-planos-pcb/scripts` (skill de proyecto).
- Anadido: contradicciones a `ROADMAP.md` («Contradicciones abiertas»), no solo al documento.
