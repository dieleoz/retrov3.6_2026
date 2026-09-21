# Particularidades de la V3.6 para `orquestador:leer-planos-pcb`

Rutas, inventario del árbol, control con respuesta conocida y el uso que más importa aquí: identificar la
placa antes de grabar. El método está en la común. Mapa: `ARQUITECTURA.map` §M1 (equipo) y §M6
(hardware). Sin cifras vigentes.

## 0. Árbol y repositorios

- El árbol bueno es `D:\IT\P_RetroVertical_V3.6`. El de la V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`)
  y el de la V4.6 (`D:\IT\P_RetroVertical_V4.6`) se consultan en **solo lectura**: nada de aquí se escribe
  allí. `D:\@Proyect\IT\P_RetroReflectometro_Vertical` es una copia desfasada (`CLAUDE.md` §1).
- **Qué hay en este árbol** (comprobar con dos vías, `git ls-files` y `find`/`Glob`, antes de afirmarlo):
  - Ningún `.sch`, `.brd`, `.kicad_*` ni `.IPC`.
  - Un solo diseño: `02_Hardware/PCB_Proteus_3.3/Retro Básico Horizontal v3.2 ..pdsprj`. Por su nombre es
    el del **Horizontal**; `ARQUITECTURA.map` §M6 (nodo HW0_PCB) lo registra así, con la cita de
    `LEAME_original_2020.txt` («el circuito ... es el mismo prácticamente»). Que sea la placa
    «SATLUX H-IoT» de los V3 **no está comprobado**: lectura heurística, y marcada como tal.
  - El mapeo de la placa H-IoT (`HARDWARE-V3-SATLUX-H-IoT.md`) y el de la `Retro_smd_v1` viven en el
    repositorio V5 (`README.md:79` de aquel), no aquí. El `.IPC` de 2019 de la placa V3 también está fuera
    de este árbol (ruta en `HARDWARE-V3-SATLUX-H-IoT.md:18`, V5); se descomprime en el scratchpad.

## 1. Identificar la placa antes de grabar la V3.6

Grabar es irreversible (`CLAUDE.md` §3). La V3.6 saca UART1 (Bluetooth) por **RC1 (TX) y RC0 (RX)**
(`01_Firmware/RetroVertical_V3.6.X/mcc_generated_files/pin_manager.c:92,95`); la `Retro_smd_v1` de la
V4.1 lleva UART1 en **RC4/RC5**. Grabar la V3.6 en una placa V4 dejaría el Bluetooth sin conexión
(`01_Firmware/lecturas_equipos/V3-2/LEEME.md:7`). Caso real: el segundo equipo «V3» resultó ser un V4 y
va a la V4.6 (`HISTORIA.md`, tabla de errores).

Identificar empieza por micro, placa y pantalla con el equipo delante (`RUNBOOK.md`, fase 0), nunca por
los textos de la pantalla. Los planos dicen **qué medir** (qué pin del PIC va al módulo Bluetooth); no
sustituyen el polímetro. La serigrafía tampoco identifica la placa: un «v4.0» serigrafiado no quiere
decir placa H-IoT (`01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md:30-31`).

## 2. El `.pdsprj` de este árbol

Con `SCRATCH` apuntando al scratchpad de la sesión (nunca dentro del repositorio):

```bash
D="02_Hardware/PCB_Proteus_3.3/Retro Básico Horizontal v3.2 ..pdsprj"
mkdir -p "$SCRATCH/pds" && unzip -o -q "$D" -d "$SCRATCH/pds"
strings -n 3 "$SCRATCH/pds/ROOT.LYT" | grep -n -A8 PICKIT3   # conector ICSP: ref, encapsulado, redes
```

Además de `PROJECT.XML`, `ROOT.DSN` y `ROOT.LYT` trae `ROOT.CDB`. Lo que sale es heurístico, como dice
la común §4; cerrarlo exige Proteus 8.6 o posterior, o medir la placa.

## 3. Cruce con el firmware

Sólo hay PIC. Carpetas: V3.6 en `01_Firmware/RetroVertical_V3.6.X/mcc_generated_files/`; base 2020 en
`01_Firmware/base_2020_d089f962/RetroVertical1.X/mcc_generated_files/`.

```bash
python "$S/pin_manager_pic.py" 01_Firmware/RetroVertical_V3.6.X/mcc_generated_files [--pin RC0]
```

`S` es el `scripts/` de la común (`${CLAUDE_SKILL_DIR}/scripts`); fuera de la skill, el clon
`D:/IT/Arquitec_Orquestador/skills/leer-planos-pcb/scripts`. Si la V3.6 cambia `pin_manager.c` respecto
a la base 2020, el `diff` de los dos va en el documento: la placa es la misma. Las contradicciones van
también a «Contradicciones abiertas» de `ROADMAP.md` (`CLAUDE.md` §2), no sólo al documento y al mapa.

## 4. Control con respuesta conocida

- `eagle_netlist.py`: como en este árbol no hay Eagle, el control es la `Retro_smd_v1` del repositorio
  V5, en solo lectura: `D:\IT\P_RetroReflectometro_Vertical\02_Hardware\Retro2025\Retro_smd_v1.sch`. Los
  conteos esperados son los que da la común §6; no se copian aquí.
- `pin_manager_pic.py` sobre la V3.6: UART1 TX en RC1 (`pin_manager.c:92`), RX en RC0 (`:95`); UART2 RX
  en RB1 (`:90`), TX en RB0 (`:91`). Si cambia sin que haya cambiado el fichero, el script está roto.

## Diferencias con la común

- Inventario: un solo `.pdsprj`, del Horizontal por nombre; sin `.sch`, `.brd`, `.IPC` ni `.kicad_*`.
  Eagle y KiCad de la común se usan aquí sólo como control o si llega un fichero (§0).
- Identificar la placa antes de grabar (UART1 RC0/RC1 frente a RC4/RC5) es propio de esta línea (§1).
- Cruce ESP32/Arduino: no aplica (§3).
- El control de `eagle_netlist.py` está fuera del árbol (V5); se añade el de `pin_manager_pic.py` sobre
  la V3.6 (§4).
- Las contradicciones van también a `ROADMAP.md` (§3).
- Ya no hay copia de los scripts en el repositorio: se usan los de la común.
