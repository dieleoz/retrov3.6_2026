# SPEC — App de usuario (campo) V3.6: flujo y pantallas

**Nada de esto está medido contra un equipo ni un teléfono; no hay código propio de esta app todavía.**
Es flujo y pantallas sobre datos y reglas que **ya tienen ID en otro documento**: no se repite un
requisito que ya lo tenga, se cita. Sale de leer `PROTOCOLO-V3.6.md`, `SPEC-V3.6.md`,
`SPEC-Registro-Indicador-Interventoria.md` (SPEC-REG), el catálogo `08_Senales/`, el código de `main`
y de la rama `rtv-1.0`, y la app legacy en `D:\@Proyect\IT\old\VERTICAL\4_Apps\`.

| Campo | Valor |
| :--- | :--- |
| Alcance | La app que va **con el equipo**, la usa el operador de campo, "no sabe ni usar la app" |
| No es | La app de EMPRESA (DPI, USB por proyecto: tomar → ZIP → calibrar → verificar → certificado) |
| Datos, calibración, export | [SPEC-REG](SPEC-Registro-Indicador-Interventoria.md): §2 modelo, §3 equipo, §4 export |
| Catálogo de señal | `08_Senales/senales.csv` (377 filas), `iconos/*.svg`, `CATALOGO-Senales-Manual-2024.md` |
| No decide | Tecnología (nativa/híbrida/web), ni si esta app sustituye a una app legacy existente |

---

## 1. Pantallas

1. **Conectar.** Emparejados del teléfono, detectar firmware (RF-USR-01), mostrar estado de
   calibración apenas responde (RF-USR-02).
2. **Elegir señal.** Buscar por código del Manual 2024 o navegar el catálogo por icono; propone
   color de fondo y lámina mínima (RF-USR-03). Búsqueda manual si no hay GPS o la señal no está
   (RF-REG-19 de SPEC-REG). Señal nueva: alta mínima (código, familia, color); el resto del
   inventario georreferenciado de SPEC-REG §2.4 es opcional en esta versión (§6, C-USR-01).
3. **Medir.** Confirmar color y tipo (I / no-I), disparos por color con media y mínimo (RF-USR-04,
   RF-USR-05), botón único "Medir".
4. **Guardar visita.** Identidad de equipo, ubicación, observación (RF-USR-06).
5. **Ver datos / seguir midiendo.** Lista de la campaña abierta; consulta, no edición (RF-REG-13).
6. **Exportar.** Paquete de SPEC-REG §4.1 por "Compartir" del teléfono (RF-USR-07).

Nada de PIN, "Avanzado", banco, cola ni acta: eso es la app de EMPRESA (RF-APP de `SPEC-V3.6.md` §3).

## 2. Requisitos (RF-USR)

**RF-USR-01 — Detección sin escribir nada.** `#V#` → `9` → `6` → sonda `@LEERV,BLA,2@` si nada
respondió; nunca `e` a un equipo no identificado. Reutiliza `Deteccion.java` de `rtv-1.0` (§3).
*Fuente:* `SPEC-V3.6.md:459-494` (RF-APP-03); `PROTOCOLO-V3.6.md:16-22`; `CLAUDE.md` §4. *CA:* contra
`EquipoSimulado` en 3.6.2 y en F-2020, el registro de tramas es sólo `Deteccion.PERMITIDAS`, 0 veces `e`.

**RF-USR-02 — Estado de calibración, siempre visible.** Lee `#GC#`/`#GN#`; vencimiento = fecha + 1
año; si `NONE`, vencida o huella distinta, **avisa y marca, no bloquea** (PA-01). *Fuente:*
`PROTOCOLO-V3.6.md:51-54`; `SPEC-REG:370-382` (RF-REG-02 a RF-REG-04). *CA:* con `#GC#` de hace 400
días, "EQUIPO CON CALIBRACIÓN VENCIDA" aparece en pantalla y en cada exportación; la medida sigue.

**RF-USR-03 — Elegir señal por catálogo.** `08_Senales/senales.csv` da `color_fondo`, `color_orla`,
`color_simbolo` y `lamina_minima`; el operador confirma tipo I / no-I cuando el catálogo no lo
resuelve solo (p. ej. `"superior a IV; XI"`). *Fuente:* `08_Senales/CATALOGO-Senales-Manual-2024.md`.
*CA:* elegido `SR-01`, propone fondo rojo y pide confirmar tipo.

**RF-USR-04 — Código Bluetooth por RF-APP-04.** Color y tipo confirmados → un byte: tipo I → `7`,
`8`, `a`-`d`; otro tipo → `1`-`6`. *Fuente:* `SPEC-V3.6.md:496-497`, tabla `:121-131`. *CA:* fondo
rojo, tipo I → byte `b` (0x0B); nunca sale otro byte para esa combinación.

**RF-USR-05 — Disparos por color, nunca `e`.** 3 disparos por color (`medidasRealizar = 3` de la app
legacy Ionic, `medir.page.ts:54`), media, mínimo, trama cruda; nunca `e`, con equipo identificado o
no. Un `0` se muestra "0 (saturado o sin señal)". *Fuente:* `SPEC-REG:573` (RF-REG-08, media/mínimo);
`SPEC-V3.6.md:501-509` (RF-APP-05); `ecuacionesCalibracion.c:49-54`. *CA:* con lecturas simuladas
100, 110, 120 → media 110, mínimo 100, n = 3; `EquipoSimulado` respondiendo `::0` muestra la leyenda
completa; 0 apariciones de `e` en el registro de toda la app.

**RF-USR-06 — Guardar visita con identidad de equipo.** MAC, nombre Bluetooth, serie (o "SIN SERIE"
explícito si `#GN,NONE#`) y `#V#` completo en cada visita; nada se sobrescribe (una corrección es un
evento nuevo). *Fuente:* `SPEC-REG:566` (RF-REG-01); `SPEC-REG` §2.6 (RF-REG-13, diario);
`SPEC-REG:490` (defecto de `medicion.java:1563,1565`, "tras guardar el campo queda en 0", que no se
repite). *CA:* ninguna visita exportada tiene esos campos vacíos; guardar dos medidas seguidas deja
dos filas distintas, ninguna en cero; alterar un evento hace fallar la verificación de los siguientes.

**RF-USR-07 — Exportar sin SMTP ni credenciales.** El ZIP de SPEC-REG §4.1, por "Compartir" del
teléfono, reutilizando `PaquetesZip.java`/`Registro.java` de `main`. *Fuente:* `SPEC-REG:592`
(RF-REG-27); corrige PAR-16 de `SPEC-V3.6.md:926` y M-12 de `ROADMAP-MEJORAS-App.md` (credencial en
claro de `Enviarcorreo.java:54-55`). *CA:* 0 cadenas de credencial en el APK.

**RF-USR-08 — Sin modo administrador.** Ninguna pantalla ofrece `#L`, `#S`, `#F`, `#ST`, `#FT`,
`#SC`, `#SN`, `#P`, `#K#`, `#KC#`. *Fuente:* `PROTOCOLO-V3.6.md` §3 (columna "Requiere admin");
decisión de Diego (dos apps, no se mezclan). *CA:* `grep` del código de esta app, 0 resultados.

**RF-USR-09 — Colores sin ecuación, fuera de la medida.** Café, lila y fluorescentes salen como "no
medible con este equipo": el mapa de RF-APP-04 sólo cubre 6 colores. *Fuente:* `SPEC-REG:157`
(C-10); `SPEC-V3.6.md:121-131`. *CA:* esos colores no ofrecen botón "Medir".

**RF-USR-10 — Sin red.** El ciclo completo (medir, guardar, exportar) funciona en modo avión.
*Fuente:* `SPEC-REG:588` (RF-REG-23). *CA:* con el teléfono en modo avión, exportar produce el ZIP.

**RF-USR-11 — Aviso antes de la primera sonda con firmware 2020.** Con un V3 de 2020 (no V3.6),
`#V#`, `e` y `6` disparan una medida (luz y pitido): se avisa al operador antes de la primera sonda de
RF-USR-01. *Fuente:* `SPEC-V3.6.md:484-486`; `PROTOCOLO-V3.6.md:22`. *CA:* al detectar F-2020 en
`EquipoSimulado`, aparece el aviso antes de enviar la primera sonda, no después.

## 3. Qué reutiliza del código de `rtv-1.0` (capa Bluetooth / Protocolo / detección)

Prefijo `RTV10:` = `git show rtv-1.0:03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.

| Clase | Qué da | Se toca aquí |
| :--- | :--- | :--- |
| `EnlaceSerie.java` | Socket SPP, hilo lector permanente, bytes crudos | No |
| `Cliente.java`, `Canal.java` | Petición-respuesta con una en vuelo; pausas de RF-APP-01 | No |
| `Deteccion.java` | Orden `#V#`→`9`→`6`→sonda; `PERMITIDAS` antes de identificar | No |
| `Protocolo.java`, `ProtocoloV36.java`, `ProtocoloV2020.java` | Qué trama vale por firmware | No |
| `Tramas.java`, `Receptor.java` | Tramas y fin de `::n` por silencio (RF-APP-02) | No |
| `Bateria.java` (ya en `main`) | Orden `9`, aviso con `n < 19`, nunca bloquea medir | No |

**Colisión de nombre, sin resolver aquí (C-USR-02).** `main` ya tiene `Protocolo.java`
(`03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/Protocolo.java:7`), el "protocolo de
disparos" de la campaña de calibración (K de asentamiento, M de disparos): **sin relación** con la
interfaz `Protocolo` de `rtv-1.0` (familia de firmware). Fusionar exige renombrar una de las dos.

## 4. Legacy: qué app hablaba ya el protocolo V3, y cuál no

**Verificado con dos fuentes.** `RetroVerticalP1` (`medicion.java:1590-1594`) envía
`@LEERV,<color>,<tipo>@` y espera `@LEERV,<n>@/n/r` con `0x00`: es el protocolo **V4/V4.1**
(`SPEC-V3.6.md:906`: "la app de campo habla `@LEERV`… no el protocolo V3"), no el de un byte de
V3/V3.6. `RetroVertical_Ionic` (`medir.page.ts:225-261`) sí envía `'1'`-`'8'`, `'A'`-`'D'` y `'9'`, el
protocolo exacto de `PROTOCOLO-V3.6.md` §1, y ya tiene selector de señal (`ListasenalesPage`,
`senalSeleccionada`). **Es la base legacy más cercana a esta app**, no `RetroVerticalP1`; se reutiliza
como referencia de flujo, no como código (es Ionic/TypeScript, esta app es Java sobre `rtv-1.0`).

## 5. Qué no se toca

Firmware y pantalla STONE, sin excepción (`CLAUDE.md` §1, `PROTOCOLO-V3.6.md:25`). Ninguna trama con
"Requiere admin" = Sí (PROTOCOLO §3). El protocolo de la Ionic asumía un terminador `\n`
(`bluetoothGlobal.subscribe('\n')`, `medir.page.ts:612`) que el V3 no manda (RF-APP-02): ese defecto
no se replica.

## 6. Contradicciones abiertas (no se elige aquí)

- **C-USR-01.** Alcance de "elegir señal": el flujo simple de §1 (código + color propuesto) contra el
  inventario georreferenciado completo de SPEC-REG §2.4-§6 (`senal_id` propio, GPS, historial). Este
  documento trata lo segundo como capa posterior sobre el mismo guardado, sin fusionar RF.
- **C-USR-02.** Nombre `Protocolo` repetido entre `main` y `rtv-1.0` (§3): resolución pendiente.
- **C-USR-03.** `SPEC-REG:326-328` dice "ningún comando Bluetooth devuelve la serie"; `PROTOCOLO-V3.6.md:54`
  documenta `#GN#` (firmware 3.6.2) ya definido para leerla. Puede que SPEC-REG quedara desactualizada
  tras la 3.6.2, o que `#GN#` tenga un defecto de campo no registrado: no se ha medido en ningún equipo.
- **C-USR-04.** Con un equipo sin `#GN#` (pre-3.6.2), RF-REG-01 exige serie no vacía para guardar; esta
  SPEC no dice si eso **bloquea** guardar o si basta "SIN SERIE" explícito, como con la calibración.

## 7. Preguntas para Diego

1. ¿Esta app sustituye a `RetroVerticalP1`/Ionic para los equipos V3.6, o convive con ellas mientras
   no haya decisión de retirarlas (`T-C30` sigue `PENDIENTE` en `TDD-V3.6.md`)?
2. Con un equipo sin `#GN#` (pre-3.6.2), ¿guardar sin serie identificada **bloquea** o sólo **avisa y
   marca** "SIN SERIE", como se decidió para la calibración vencida (PA-01)? (C-USR-04)
3. La pantalla "Elegir señal", ¿empieza con el inventario georreferenciado completo de SPEC-REG desde
   la primera versión, o con el flujo simple de código + color propuesto? (C-USR-01)
