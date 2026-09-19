# Software para editar las pantallas STONE del Retrorreflectómetro Vertical

**Nada de esto se ha probado contra una pantalla.** Sale de leer ficheros del disco, los manuales
de STONE archivados y la web de STONE (consultada el 18-sep-2026). No se ha ejecutado ningún `.exe`,
no se ha instalado nada y no se ha cargado ningún proyecto en ningún equipo.

Copia idéntica en `D:\IT\P_RetroVertical_V3.6\04_Pantalla_STONE\` y
`D:\IT\P_RetroVertical_V4.6\04_Pantalla_STONE\`.

---

## 1. Por modelo

| | **STVA035WT** (V3, 2020) | **STWA035WT** (V4.1, 2022) |
| :--- | :--- | :--- |
| Generación | 2.ª ("V"). La herramienta la llama familia `STV` (`ScreenProperties20190717.txt`: `P2=STV`) | 3.ª ("W" = *Third Generation*, "A" = *Advanced*; `STW Serial Model Description.pdf`, regla de nombres) |
| Herramienta | **VGUS**. El proyecto de 2020 se hizo con VGUS 4.3; en el disco está **VGUS2019** ("TOOL 2019") | **STONE Designer** |
| Versión | VGUS4.3 `20180403` (`lcd_RetroVertical_2020/CompileInfomation.txt`, primera cabecera) y VGUS2019 `20190325` (cadena dentro de `TOOL 2019.exe`) | En disco: **3.0.12** (recurso de versión del instalador). Con qué versión se hizo la pantalla de 2022: **no se sabe** |
| Formato del proyecto | Carpeta con `*.vt` (texto), `VT_SET\` (salida compilada: `VGUSII.bin`, `22.bin`, `CheckSum.bin`, `CONFIG.txt`, `N.JPG`, `0.ICO`), `IMAGE\`, `ICON\`, `FONT\`, `CompileInfomation.txt` | Un único `*.st`, que es un **ZIP** con `manifest.json`, `data/app_conf.json`, `fonts/`, `images/`, `styles/`, `ui/` (comprobado con `unzip -l` sobre `retro_v.st`) |
| Protocolo con el PIC | Binario `A5 5A 05 82 00 <dir> …` (`01_Firmware/base_2020_d089f962/RetroVertical1.X/uart_stone.c:109-128`) | JSON `ST<{"cmd_code":…}>ET` (`18f47k42_RetroV_V4.1.X/App_Stone.c:45`; widgets `label1`…`label14` en `App_Stone.c:271-372`) |
| Cómo se carga | Por **USB**: el menú de la herramienta tiene "Online Download" (Ctrl+D), que copia a la carpeta `VGUS_USER` de la pantalla montada como unidad, y "Download to U-disk" (cadenas de `TOOL 2019.exe`). La herramienta genera la carpeta `VT_SET` | Dos vías (manual *STONE Designer User Manual V1.7*, §2.2): **cable USB A-A** con el conmutador K600 en "PC" (la pantalla aparece como unidad; hay que borrarla o formatearla antes) o **pendrive FAT32** con el K600 en "U-disk" y la ruta exacta `STONE\project\default`. Las unidades anteriores a enero de 2022 pueden necesitar actualizar el firmware de la pantalla para admitir el pendrive (§2.2.2) |
| Dónde conseguirla | Ya en disco (sección 2). STONE ya no la publica | Ya en disco la 3.0.12 (sección 2). La oficial, sólo por formulario (sección 3) |

Datos que sostienen la tabla:

- **VGUS2019 abre los proyectos de VGUS 4.3.** El mismo proyecto de la v4.0 se compiló con
  `VGUS4.3 … 20180403` por última vez el 12-ago-2020 15:30 y con
  `VGUS2019 development tool version number: 20190325` desde el 07-dic-2020 14:41
  (`lcd_RetroVertical_v4.0/CompileInfomation.txt`, líneas 1700-1721 tras pasarlo de GBK a UTF-8).
  Es decir, TOOL 2019 abrió y recompiló un proyecto de VGUS 4.3 sin rehacerlo.
- **`TOOL 2019.exe`** declara `FileDescription: Development Tools`, `FileVersion: 4.3`; en sus
  cadenas aparecen `VGUS2019 development tool version number: 20190325` y
  `Beijing STONE Technology Co.,Ltd.`. **No está firmado** (Authenticode `NotSigned`).
  SHA-256 `736EA771…7BEB6`, idéntico al de `D:\@Proyect\IT\old\VERTICAL\3_V4.0_18F47K42_2021\retrovertical\SOTFWARE\20200607-TOOL 2019\`.
- La herramienta **espera estar en `C:\VGUS2019 Development Tool\`**: esa ruta está cableada en el
  ejecutable para las fuentes, el proyecto de ejemplo, `viewtech.rsa` y
  `ScreenProperties20190717.txt`. Instalarla en otra carpeta puede dejarla sin esos ficheros.
- Para el simulador y el asistente serie necesita registrar `mscomm32.ocx` como administrador
  (`Instruction manual.txt`, `Register.cmd`).
- **STONE Designer no abre `.vt`**, hasta donde dicen los papeles: el manual V1.7 no nombra VGUS ni
  habla de importar proyectos antiguos (se buscó "vgus", "import", ".st"). Sólo abre `.st`
  (§1, "open project: select the corresponding UI (.st) file"). No se ha comprobado con el programa.
  Un proyecto de 2.ª generación no sirve para la 3.ª: hay que rehacerlo.
- **La STWA035WT ya no figura en la web de STONE.** La serie Advanced empieza en 4,3" y
  `stoneitech.com/product/stwa035wt-01/` redirige a la **STWI035WT-01** (Industrial, 3,5", 320×240,
  Cortex A8, 256 MB). Si hay que sustituir una pantalla del V4.1, el recambio probable es la STWI;
  que acepte el mismo proyecto no está comprobado.

## 2. Lo que ya está en disco

| Qué | Ruta | Estado |
| :--- | :--- | :--- |
| VGUS2019 ("TOOL 2019") y su kit | `D:\IT\P_RetroVertical_V3.6\04_Pantalla_STONE\herramienta_STONE_TOOL_2019\` | Completo: `TOOL 2019.exe`, `Command Assistant 20190421.exe`, `mscomm32.ocx`, `Register.cmd`, fuentes, proyectos de ejemplo. Sin firma |
| Instalador STONE Designer **3.0.12** (Windows) | `D:\@Proyect\Camion\04_Manuales\STONE Product Detail 2025\02-STONE Designer GUI Software\Stone Designer Setup 3.0.12.exe` | 140 071 512 bytes, 31-mar-2025. `CompanyName: XYH`. **No está firmado.** SHA-256 `91742DFB…AC64126`. Viene del paquete de documentación de STONE del proyecto Camión; en `D:\@Proyect\Camion _G\` la misma carpeta no lleva el `.exe` |
| Versión macOS | misma carpeta, `Stone Desginer-3.0.12.dmg` | — |
| Manuales de 3.ª generación | `D:\@Proyect\Camion\04_Manuales\STONE Product Detail 2025\` | *STONE Designer User Manual V1.7*, *JSON Instruction Sets V2.8RC*, *Download Project Files & Update Logo File.pdf*, *Common Issues & Solutions.pdf* |

Negativos comprobados con dos herramientas (`find` de Git Bash y `Get-ChildItem` de PowerShell):
no hay STONE Designer ni VGUS instalados (`C:\Program Files*`, `%LOCALAPPDATA%\Programs`,
`%APPDATA%`, claves `Uninstall` de HKLM y HKCU); fuera de `Camion` no hay instaladores de Designer
en `D:`. `D:\@Proyect\IT\old\instaladores_stone\` no se ha creado porque no se descargó nada.

## 3. Lo que hay que pedir a STONE

**STONE Designer no se descarga directamente.** `https://www.stoneitech.com/support/software/` sólo
ofrece un formulario; el enlace llega por correo en 24 h. Lo único de descarga directa es
*UART Communication Tool - SSCOM32*. El formulario **no se ha rellenado**. Pide:

- Name, **Email** (el único obligatorio), Company, Job Title, Country, Mobile phone;
- *Interested display size* (desplegable: 3.5", 4.3", … Others);
- Project, *Comments & Questions*.

Qué conviene preguntar en *Comments*: la versión vigente de Designer; si un proyecto hecho con la
3.0.12 carga en una **STWA035WT de 2022**; si esa pantalla necesita actualizar firmware para cargar
por pendrive (el manual avisa de las anteriores a enero de 2022); y si hay algún conversor de `.vt`.

VGUS / TOOL 2019 **no hay que pedirlo**: ya lo tenemos, y la web actual no lo ofrece.

## 4. Bases de proyecto de pantalla del Vertical

Búsqueda de `*.vt`, `VT_SET`, `*.st` en todo `D:` (incluidos `D:\@Proyect\IT\old\`, `legacy\` y
`D:\@Proyect\legacy a revisar\`). Duplicados descartados comparando md5 del `.vt`/`.st` y del
contenido de `VT_SET`. No se ha encontrado **ningún proyecto STONE del Horizontal ni de la H-IoT**:
los `.st` que hay en `D:` fuera de estas rutas son del Camión (`Controladora.st`, `Vel_Odom.st`,
`conductor.st`) y no se han copiado.

| Ruta (relativa al repositorio) | Gen. | Fecha | Para qué equipo | Se abre con | md5 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| V3.6 `04_Pantalla_STONE\lcd_RetroVertical_2020\` | 2.ª | `.vt` 14-ago-2020, compilado 12-ago-2020 | V3 2020 (STVA035WT, firmware `uart_stone.c`) | VGUS 4.3 / TOOL 2019 | `.vt` `b0b3b427…`; `VT_SET` idéntico al de `2_V3.3…\RETRO VERTICAL\` y sus copias |
| V3.6 `04_Pantalla_STONE\bases\old_V4.0_retrovertical_2021-03-09\` | 2.ª | `.vt` 09-mar-2021 14:11 | **V4.0 2021**, que también hablaba el protocolo binario `0xA5` (`3_V4.0…\SOTFWARE\47k42_RV_v4.X\Stone.c:27`). **No sirve para la STWA del V4.1** | TOOL 2019 (última compilación con VGUS2019) | `.vt` `f7fd66e0…`, 158 ficheros, igual que `V4.6\04_Pantalla_STONE\lcd_RetroVertical_v4.0\` |
| V3.6 `04_Pantalla_STONE\disenos_pantalla_2020\320x240 Test Project\` | 2.ª | 21-ene-2020 | Plantilla vacía de STONE, no es la pantalla del equipo | VGUS | `.vt` `5419afff…` |
| V4.6 `04_Pantalla_STONE\bases\old_V4.1_Retro_Vert_2025-11-24\retro_v.st` | 3.ª | 24-nov-2025 | **Borrador**, no la pantalla de 2022: 320×240, 115200 baudios, 3 ventanas (`home_page`, `Amarillo_Otros`, `Blanco_Otros`), widgets `label_medida_Ot_Am`, `label_temp_Ot_Bl`, `progress_battery`… Sus nombres **no coinciden** con los `label1`…`label14` que escribe el firmware V4.1 | STONE Designer | `6a5c8884…`; origen `old\VERTICAL\5_V4.1_18F47K42\Retro_Vert\` (igual en `D:\@Proyect\legacy a revisar\Retro_Vert\`). Se copió al lado `Linterna.png`, de la misma fecha |

Descartadas por duplicadas o incompletas: las copias de `_copias_E\`, `RETRO VERTICAL__E_Electronica_proyectos`
(idénticas a la de 2020) y `RETRO VERTICAL__old_old` (mismo `.vt`, `VT_SET` a medias). Los tres
proyectos de ejemplo de la carpeta de TOOL 2019 (`STONE`, `PruebaSlider`, `800x480_Test_Project`)
son demos de la herramienta.

**No existe en disco el proyecto de pantalla del V4.1 de 2022 para la STWA035WT.** Si hace falta,
o se rehace en STONE Designer partiendo de `retro_v.st` y del firmware (`App_Stone.c`), o se pide a
quien lo hizo.

## 5. Advertencias

- **Una STONE no deja sacar copia de lo que lleva cargado.** No hay forma documentada de leer el
  proyecto de vuelta a un `.vt` o `.st` editable. En la 3.ª generación, con el K600 en "PC", la
  pantalla aparece como unidad USB, pero lo que hay dentro es el proyecto compilado (`default`), no
  el `.st`. No se ha probado.
- **Cargar un proyecto sustituye el que tiene el equipo**, y en la 3.ª generación el manual pide
  borrar o formatear la memoria antes de cargar por cable. Antes de tocar una pantalla en campo hay
  que tener la base que la reemplaza y saber que casa con los nombres de widget (3.ª gen.) o las
  direcciones de variable (2.ª gen.) que usa ese firmware; si no casan, la pantalla arranca pero no
  muestra medidas.
- **La pantalla de propietario (logo de arranque) es de cada cliente.** En la 3.ª generación va
  aparte, en `STONE\logo\logo.bmp` (BMP de 8 o 24 bits, pendrive FAT32). Una base genérica no lleva
  el logo del cliente: hay que conservarlo o pedírselo.
- Los ejecutables de disco (`TOOL 2019.exe`, `Stone Designer Setup 3.0.12.exe`) **no están
  firmados**. Su procedencia de STONE es probable pero no está verificada.
- Pantalla vacía o color púrpura con "home_page is missing" tras cargar por pendrive: *Common Issues
  & Solutions.pdf* indica formatear en modo PC, copiar `default` a mano y reintentar; nombres de
  fichero sólo en minúsculas, números y guion bajo.

## 6. Sin confirmar

- Con qué versión de STONE Designer y con qué firmware de pantalla se hizo la STWA035WT de 2022.
- Si la 3.0.12 carga en esa pantalla sin actualizar su firmware.
- Si la pantalla de campo del V3 coincide con `lcd_RetroVertical_2020` y la del V4.1 con algo de
  lo que hay en disco.
- Que STONE Designer no importe `.vt`: sale del manual, no del programa.
