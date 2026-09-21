# SPEC — App de usuario (campo) V3.6: flujo y pantallas (r5)

**Nada de esto está medido contra un equipo ni un teléfono; no hay código propio de esta app todavía.**
**r5, 21-sep-2026: cierra las condiciones del `arquitecto-iot` (APTO CON CONDICIONES al incremento 1,
sobre `3f9e02d`).** Cambios de fondo sobre r4: (1) respuesta tardía (ALTO): tras un plazo vencido, una
**cuarentena de `Q` ms** descarta y anota en `tramas.log` todo lo recibido, antes de la pausa de
RF-APP-01; un solo valor de pausa, 600 ms desde el último byte (RF-USR-16, se retira el "500 ms" de la
r4); (2) columnas `latitud`/`longitud` (un solo nombre, ya no `lat`/`lon`) y formato completo de
`medidas.csv`: `;`, decimal `,`, UTF-8 con BOM, comillas donde haga falta, fechas ISO 8601 con zona,
lecturas como enteros sin `::`, y `estado_calibracion` con la rama `sin_fecha` (RF-USR-06); (3) la serie
de disparos con un disparo anulado por plazo se repite hasta 2 veces antes de anular la serie entera
(RF-USR-04, RF-USR-16); `lecturas_por_color` se fija en Ajustes, no en la pantalla de medir; (4)
T-USR-19(b), T-USR-20 y T-USR-21 se parten entre incremento 1 y 2; contenido y nombre del ZIP del
incremento 1 (RF-USR-15 bis); (5) fichas nuevas de persistencia entre reinicios, ritmo con borrado de
80 ms y exportación repetible (TDD §8); además, T-USR-01(a) queda acotada a antes de confirmar
`#V,3.6,`; plazo y cierre de las tramas `#` dentro de RF-USR-16; se retira "NO CUMPLE" de la ficha del
cero en el incremento 1 (no hay columna de cumple todavía); ficha de `#ERR,EEPROM#`; y `exigir_362`
como parámetro con sus dos ramas, probadas (RF-USR-01, M-3). Detalle numérico y de verificación en
`TDD-V3.6.md` §8. Fuentes: `PROTOCOLO-V3.6.md`, `SPEC-V3.6.md`, `calibracion_v36.c`,
`ecuacionesCalibracion.c`, `gui.c` (2020 y V3.6), `SPEC-Registro-Indicador-Interventoria.md` (SPEC-REG),
`08_Senales/senales.csv`, `rtv-1.0` (§3), `06_Calibracion/SLV-002/tramas/
rtv36_20260919_191255_HONOR.txt` (formato de `tramas.log`),
`Manual-Senalizacion-Vial-Extracto.md` (repositorio V4.1/V5, sólo consulta).

| Campo | Valor |
| :--- | :--- |
| Alcance | Va con el equipo, la usa el operador de campo. **Sólo firmware V3.6**: un APK por firmware y cliente |
| Incrementos | **1** "Medir y exportar" (primero). **2** "Señal a señal" (después, depende de Diego, §0) |
| Modos | **"Medir y exportar"** (por defecto, cero tecleo, inc. 1) y **"Señal a señal"** (opcional, inc. 2) |
| Mide | Sólo la **lámina retrorreflectiva** (fondo y orla); símbolo y lo demás son mantenimiento |
| Umbral | Mínimo del Manual (referencia, C-06 abierta) o instalación con el 80 %; nunca 70 % de SFT |

---

## 0. Incrementos

**Incremento 1 — "Medir y exportar".** Se programa primero: no depende de ninguna propuesta ▸
pendiente de confirmación de Diego más allá de los parámetros de plazo/silencio (RF-USR-16) y de la
exigencia de firmware 3.6.2 (M-3), que sólo cambian un número, no el diseño.
- **Requisitos:** RF-USR-01, RF-USR-02, RF-USR-03, RF-USR-04, RF-USR-05 (sólo el modo por defecto),
  RF-USR-06, RF-USR-15, RF-USR-15 bis, RF-USR-16.
- **Fichas (TDD §8):** T-USR-01, T-USR-01b, T-USR-01c, T-USR-02, T-USR-03, T-USR-03b, T-USR-04,
  T-USR-06, T-USR-06b, T-USR-07, T-USR-19a, T-USR-20a, T-USR-21a, T-USR-22, T-USR-23, T-USR-24,
  T-USR-25, T-USR-26, T-USR-27, T-USR-28.

**Incremento 2 — "Señal a señal".** Va después: depende de que Diego confirme o corrija el umbral
doble de la condición 3 (UMBRAL-LEY, nota 9), la geometría 0,2°/−4° que cierra C-06, y el umbral de
15 m de emparejamiento de RF-USR-13. Mientras no se confirmen, el incremento se programa con esas
propuestas ▸ marcadas como tales, y el dictamen queda `NO_DICTAMINABLE` por defecto (RF-USR-10).
- **Requisitos:** RF-USR-07 a RF-USR-14.
- **Fichas (TDD §8):** T-USR-05, T-USR-08 a T-USR-18, T-USR-19b, T-USR-20b, T-USR-21b.

## 1. Pantallas

**Incremento 1:**
1. **Aviso, antes de conectar.** "El equipo puede encender la luz y sonar" (RF-USR-01).
2. **Conectar y detectar.** Sólo `#V#`; si no empieza por `#V,3.6,`, "equipo no compatible" y nada más
   (RF-USR-01). Si empieza por `#V,3.6,` pero el equipo no responde a `#GN#`/`#GC#` con el formato
   esperado, "actualice el firmware del equipo" (RF-USR-01, M-3). Estado de calibración (RF-USR-02).
3. **Elegir modo.** "Medir y exportar" (por defecto) o "Señal a señal" (RF-USR-05); la segunda abre en
   el incremento 2.
4. **Medir y exportar.** Color (RF-USR-03), medir (RF-USR-04); cada disparo se guarda solo, **cero
   tecleo** (RF-USR-06); exportar ZIP (RF-USR-15 bis).
5. **Ajustes**, fuera del flujo de medir (RF-USR-04). Único campo: `lecturas_por_color` (por defecto
   3). Se llega desde un menú, nunca desde la pantalla 4: medir no tiene selector de disparos.

**Incremento 2:**
6. **Señal a señal.** Proyecto (RF-USR-13); elegir o dar de alta señal con sustitución (RF-USR-07,
   RF-USR-12); identificador y estado, mantenimiento (RF-USR-08); medir y resultado (RF-USR-09, 10).

Nada de PIN, "Avanzado", banco, cola ni acta: eso es la app de EMPRESA.

## 2. Requisitos (RF-USR)

### 2.1 Incremento 1 — "Medir y exportar"

**RF-USR-01 — Detección sólo V3.6, con aviso previo, y `exigir_362` configurable (M-3).** Aviso antes de
conectar. Al conectar, se envía **únicamente** `#V#`; **antes de confirmar que la respuesta empieza por
`#V,3.6,`, la app no envía ningún otro byte** (ni `#GN#`, ni `#GC#`, ni nada de la lista blanca de
RF-USR-15): hasta ese punto la secuencia enviada es exactamente `#V#`, sin excepción (T-USR-01(a)). Si
la respuesta no empieza por `#V,3.6,`, "equipo no compatible" y ningún otro byte (nunca `9`, `6`, `e`,
`@LEERV...`). Con `#V,3.6,` confirmado, la app prueba `#GN#` y `#GC#` (ninguna mide).
- **Parámetro `exigir_362`** (▸ propuesta pendiente de Diego, M-3, cierra C-USR-04; booleano,
  por defecto `true`, configurable como el resto de RF-USR-16). Rama `true`: se exige firmware
  **3.6.2 o posterior**, que responde a `#GN#`/`#GC#`; si cualquiera de las dos devuelve
  `#ERR,FORMATO#` (3.6 o 3.6.1, que no las tienen, `PROTOCOLO-V3.6.md:51-54`), "actualice el firmware
  del equipo" y **no se mide**. Rama `false`: la app **sí mide** aunque `#GN#`/`#GC#` devuelvan
  `#ERR,FORMATO#` o no contesten; la fila se guarda con `estado_calibracion = "sin_fecha"` (no hay
  `#GC#` que leer, RF-USR-06) y `serie_origen = "ninguna"` (no hay `#GN#` que leer); las dos ramas se
  prueban por separado (T-USR-01c).
- **Cualquier `#ERR,<motivo>#`, no sólo `FORMATO`.** Si `#GN#` o `#GC#` responden con otro motivo válido
  del protocolo (p. ej. `#ERR,EEPROM#`, `PROTOCOLO-V3.6.md:71-72`), el parser no falla ni lo interpreta
  como "actualice el firmware": lo registra en `tramas.log` y trata la sonda como **"sin respuesta
  útil"**, igual que un timeout (§3, T-USR-01b). El protocolo no documenta hoy `#ERR,EEPROM#` como
  respuesta de `#GN#`/`#GC#` (para esos dos, la EEPROM en blanco o con CRC mala da `NONE`, no `#ERR#`,
  `PROTOCOLO-V3.6.md:53-54`); esta regla es defensiva, para que el parser no asuma que sólo puede
  llegar `FORMATO`.
- Si `#GN#` o `#GC#` **no contestan nada** dentro del plazo (RF-USR-16), la app no concluye "3.6/3.6.1":
  muestra "el equipo no respondió, reintente" y permite reintentar la sonda antes de decidir; sólo un
  `#ERR,FORMATO#` explícito, con `exigir_362 = true`, dispara "actualice el firmware" (§3, parser). Esto
  es distinto de `#GN,NONE#` (equipo 3.6.2 sin serie grabada, RF-USR-06): ahí sí se mide, sin pedir nada
  al operador.
*Fuente:* `PROTOCOLO-V3.6.md:42` (formato de `#V#`; **cita corregida, r2 decía `:24`**); `:51-54`
(`#GN#`/`#GC#` de firmware 3.6.2, `NONE` para EEPROM en blanco/CRC mala, `#ERR,FORMATO#` genérico);
`:71-72` (motivos válidos de `#ERR#`, incluido `EEPROM`); en un V3 de 2020 el primer byte de cualquier
trama dispara la luz (`gui.c:294-296`, `01_Firmware/base_2020_d089f962/`); `CLAUDE.md` §4.

**RF-USR-02 — Estado de calibración, siempre visible, formato real de `#V#`.** El cuarto campo de
`#V,3.6,<fecha de compilación>,CAL|DEF,<máscara %04X>#` decide: `DEF` → **"sin calibración"**, **aunque
`#GC#` devuelva una fecha** (`#F` no borra `#SC`, `PROTOCOLO-V3.6.md:51`); `CAL` → se lee `#GC#`/`#GN#`;
vencimiento = fecha + 1 año, con el 29-feb de un año bisiesto venciendo el **28-feb** del siguiente
(**PA-02**, `SPEC-Registro-Indicador-Interventoria.md:567,609`); si `NONE`, vencida o sin registrar,
avisa y marca, no bloquea. El campo exportado `estado_calibracion` (RF-USR-06) tiene cuatro valores:
`CAL` (vigente), `vencida` (`CAL` con fecha a más de un año), `sin_fecha` (`CAL` pero `#GC#` devuelve
`NONE`: la máscara dice calibrado, pero no hay fecha que leer) y `DEF` (sin calibración; en este caso
`fecha_calibracion` y `vencimiento` van vacíos, RF-USR-06). *Fuente:* `calibracion_v36.c:635-642`
(respuesta de `#V#`) y `:458-473` (`enviarFecha`, `__DATE__` = fecha de compilación, no la de hoy);
`PROTOCOLO-V3.6.md:34,42,51`; SPEC-REG `:567-569` (RF-REG-02 a 04, PA-01, PA-02).

**RF-USR-03 — Mapa color → byte.** `blanco`→`1`, `amarillo`→`2`, `verde`→`3`, `rojo`→`4`, `azul`→`5`,
`anaranjado`→`6`; `marron` (café) usa el código del rojo, `4` (`CLAUDE.md` §10, RF-CAL-40). Son
**caracteres ASCII** (`0x31`-`0x36`), no los hex `0x01`-`0x0E` de la STONE: la r1 confundió `b` con
`0x0B`, pero el firmware compara contra `"b"` = `0x62` (`ecuacionesCalibracion.c:179`). Nunca código
opaco (`7`,`8`,`a`-`d`): ninguna fila pide tipo I. Fuera de medida: `negro`, fluorescentes y color
compuesto (§8.2 del TDD). *Fuente:* `ecuacionesCalibracion.c:141-164` (por carácter, no hex).

**RF-USR-04 — Disparos por color; un cero repite una vez, y si persiste se guarda con motivo.**
`lecturas_por_color` disparos, **3 por defecto** (Diego: "leído 3 veces promedio", DECISIONES nota
11a; distinto del 4 de RF-REG-08 general), configurable **desde Ajustes** (pantalla 5, §1), **nunca
desde la pantalla de medir**: la pantalla 4 no tiene selector de disparos; media, mínimo, trama cruda.
Un `0` se muestra **"0 (saturado o negativo)"**, nunca oculto ni nulo. **Regla del cero, corregida
(H-A3, sustituye a "repetir siempre"):** si la serie trae algún `0`, se repite **una vez**; si la
**segunda** serie también trae algún `0`, se guarda la fila con `media = 0`, `válido = NO` y `motivo =
"saturado_o_negativo"`, en vez de excluirse: "no decir null a todo sino indicar que no cumple con una
medida estándar" (DECISIONES nota 8, USR-ALCANCE). **En el incremento 1 esa fila no lleva juicio de
cumple/no cumple** (no hay columna de dictamen, RF-USR-06, RF-USR-10 es del incremento 2): se exporta
igual, con `valido = NO` y su `motivo`, y es la interventoría quien la lee en Excel. **Nunca se
promedia excluyendo sólo los ceros** (una serie `0, 0, 330` no da media 330 con n = 1: se repite entera,
y si la repetición también trae ceros, se guarda con el motivo). Con repetición por cero, `lecturas`
(RF-USR-06) lleva **sólo los valores de la segunda serie** (la que se guarda); la primera serie
descartada va únicamente a `tramas.log` (RF-USR-15 bis), no a `medidas.csv`, para no duplicar filas de
una misma medida. Motivo técnico del cero: `reflectivityValue` es sin signo; una lectura negativa da un
entero enorme, `> 4000`, y se fuerza a `0` (`ecuacionesCalibracion.c:49-54`, `arreglar_dato`, llamada en
`:59` de `conversionDatoEnviar`); `0` casi siempre es lámina muy degradada o mala lectura.
- **Disparo anulado por plazo (RF-USR-16), distinto del cero.** Si un disparo de la serie se anula por
  vencer el plazo (nunca cuenta como "0", RF-USR-16), **ese disparo se repite hasta 2 veces**; si las 2
  repeticiones también se anulan por plazo, **la serie entera de ese color se anula**: no se guarda fila
  en `medidas.csv`, la app lo dice en pantalla ("serie anulada: el equipo no respondió a 3 intentos") y
  los 3 intentos van a `tramas.log`. `n`, `lecturas`, `media` y `valido` sólo existen para una serie que
  sí se guarda (RF-USR-06); una serie anulada no produce ninguno de los cuatro. Ficha que fija
  comportamiento de este trabajo (▸ propuesta pendiente de Diego, sin cita: el límite de 2 repeticiones
  no está en `DECISIONES-Diego-2026-09-19.md`).
*Fuente:* `SPEC-V3.6.md:501-503`; `ecuacionesCalibracion.c:49-54`; DECISIONES nota 11a (disparos); nota
8 (motivo de no ocultar el 0); RF-USR-16 (plazo y anulación de un disparo).

**RF-USR-05 — Dos modos (USR-MODOS); en el incremento 1 sólo el modo por defecto.** "Medir y exportar"
es el modo **por defecto** y **único que se entrega en el incremento 1**: mide muchas señales sin
escribir nada, el análisis lo hace el operador en Excel. La pantalla de elegir modo existe desde el
incremento 1 (deja el sitio para "Señal a señal"), pero "Señal a señal" **no está disponible hasta el
incremento 2** (§0): depende de las propuestas ▸ pendientes de Diego de RF-USR-09/RF-USR-10 y
RF-USR-13. *Fuente:* DECISIONES nota 10 ("no necesito que señal por señal y son 1000 me tengas
escribiendo en la app").

**RF-USR-06 — "Medir y exportar": cada medida se guarda sola, sin tecleo, con serie de origen
explícita (M-2).** Fecha-hora, GPS (o vacío con `gps_estado = sin_posicion`, RF-USR-15 bis), color,
valor, MAC, serie leída del equipo (o vacía) y su origen, `#GC#` y vencimiento. **Ninguna pantalla pide
un dato escrito, tampoco la serie**: cuando el equipo responde `#GN,NONE#` (3.6.2 sin serie grabada en
EEPROM), la fila se guarda igual, con `serie_equipo` vacío y `serie_origen = "ninguna"` (texto en
pantalla y en CSV: **"SIN SERIE"**); la app **no ofrece teclear una serie** en este modo. La serie
tecleada por el operador (SERIE-USR) existe **sólo en "Señal a señal"** (incremento 2, RF-USR-07),
nunca aquí. CSV crudo, sin señal ni cumple/no cumple (eso es de "Señal a señal"). Una fila por serie de
disparos y color, salvo una serie anulada por plazo (RF-USR-04), que no produce fila.

**Formato del fichero `medidas.csv` (M-6).** Separador `;`, decimal `,` (Excel en español: sin fuente
que mande otra cosa — ▸ propuesta pendiente de Diego, se corrige si Interventoría exige otra). UTF-8
**con BOM** (para que Excel en Windows abra los acentos sin preguntar codificación). Comillas dobles
alrededor de un campo que contenga `;` o comillas — en la práctica, el único campo de texto libre de
esta lista es `firmware_v`; el resto son enteros, fechas o un catálogo cerrado de palabras, ninguno
necesita comillas hoy. `fecha_hora` en **ISO 8601 con zona**, p. ej. `2026-09-21T14:32:07-05:00` (no
sólo la hora local sin zona). `lecturas`: los valores **ya enteros** de la serie, separados por `|`
(p. ej. `100|110|120`), **sin el prefijo `::`** de la trama cruda (eso es `tramas.log`, RF-USR-15 bis;
`medidas.csv` es para Excel, no para depurar el enlace).

**Columnas fijas de `medidas.csv` para este incremento, en este orden:** `fecha_hora`, `latitud`,
`longitud` (nombre único: nunca `lat`/`lon`), `gps_estado`, `color`, `codigo_bt` (el byte `1`-`6`
enviado, RF-USR-03), `n` (disparos de la serie), `lecturas` (enteros separados por `|`, formato arriba),
`media` (entera; **redondeo estándar, mitad hacia arriba**, del promedio de las lecturas válidas —
convención distinta de la de truncado hacia cero del Δ%, M-7, porque aquí no hay umbral que truncar
contra cero: ficha que fija comportamiento de este trabajo, sin cita de Diego), `minimo`, `valido`
(`SI`/`NO`; `NO` sólo en el caso de cero persistente de RF-USR-04, sin columna de cumple en este
incremento), `motivo` (vacío o `"saturado_o_negativo"`), `serie_equipo`, `serie_origen` (`"leida"` o
`"ninguna"`), `mac`, `firmware_v` (respuesta completa de `#V#`, entre comillas si el motor de CSV usado
las exige por llevar comas), `fecha_calibracion`, `vencimiento` (los dos vacíos si `estado_calibracion
= DEF`), `estado_calibracion` (`CAL`/`DEF`/`vencida`/`sin_fecha`, RF-USR-02). *Fuente:* DECISIONES nota
10; SPEC-REG `:566` (RF-REG-01); SPEC-REG `:592` (RF-REG-27, sin credenciales); `PROTOCOLO-V3.6.md:54`
(`#GN,NONE#`); `CLAUDE.md` §5 (L-23, serie/MAC/vencimiento en todo registro exportado); el separador
`;`/decimal `,`, el BOM y el formato ISO 8601 con zona son ficha que fija comportamiento de este
trabajo, no cita de Diego.

**RF-USR-15 — Sin modo administrador ni red.** Ninguna pantalla ofrece `#L`, `#S`, `#ST`, `#FT`, `#SC`,
`#SN`, `#P`, `#F`. Lista blanca de transmisión: `#V#`, `#GC#`, `#GN#`, y `1`-`6` (RF-USR-03); nunca `e`
(`#K#`/`#KC#` no requieren admin pero tampoco entran, son diagnóstico de pantalla). Los dos modos,
completos, funcionan en modo avión; **sin permiso o sin posición GPS, se mide igual**: columnas
`latitud`/`longitud` vacías y `gps_estado = sin_posicion` (M-8), la app nunca bloquea una medida por
falta de GPS. *Fuente:* `PROTOCOLO-V3.6.md` §3 ("Requiere admin"); SPEC-REG `:588` (RF-REG-23).

**RF-USR-15 bis — ZIP exportado: contenido, nombre, y registro de tramas (M-6).** Exportar da un ZIP
con, en el incremento 1: `medidas.csv` (RF-USR-06), `tramas.log` (formato abajo) e `inventario.csv`
**sólo con la fila de encabezados** (RF-USR-13 es del incremento 2; el fichero se reserva vacío para no
cambiar el formato del ZIP entre incrementos). Nombre: `RTVU_<serie|SIN_SERIE>_<AAAAMMDD-HHMMSS>.zip`
(la serie leída del equipo, RF-USR-06; si no hay, el literal `SIN_SERIE`), en `Download/RetroUsuario/`
del teléfono. Ficha que fija comportamiento de este trabajo (nombre y ruta), no cita de Diego.

`tramas.log`: toda petición y respuesta Bluetooth de la sesión (incluidas las de detección) se anota,
en el mismo formato de columnas que ya usa la app RTV de calibración: `t_ms;TX|RX;hex;ascii`
(milisegundos desde la apertura del enlace; TX o RX; bytes en hex separados por espacio; el mismo texto
en ASCII, con `.` para lo no imprimible o para `;`), una línea por trama, con cabecera de comentario
(`#`) igual que ese registro. Un disparo anulado por plazo (RF-USR-16) y lo descartado en la cuarentena
(RF-USR-16) se anotan igual, marcados como tales. **Este registro es una salida para auditoría del
operador y de interventoría, no la fuente de verdad de la prueba de la lista blanca**: la ficha que
cierra RF-USR-15 (T-USR-19a) asevera contra lo que **recibe el simulador** directamente, no contra este
fichero, porque un fallo en el propio volcado a `tramas.log` daría un falso verde si se aseverara sólo
contra él (TDD T-USR-19a). *Fuente:* CLAUDE.md §2 (verificación medida, no de código); M-6;
`06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt:1-6` (formato `t_ms;TX|RX;hex;ascii` de
la app RTV, mismo formato de columnas que reutiliza esta ficha; el contenido de esas líneas es de la
app de calibración, no de esta app de usuario, que no existe todavía).

**RF-USR-16 — Lectura de tramas `::<n>`, sin terminador, con plazo y silencio (H-A4, corrige r2/r3).**
`::<n>` no lleva terminador: la trama se da por completa **por silencio**, nunca por un carácter de
cierre. El búfer de recepción se **vacía antes de cada envío**; el firmware descarta lo que llega
mientras mide y limpia el búfer al terminar (`clearBuffer()`, `bufferIndex = 0`, V3.6 `gui.c:342-346`).
- **Parámetros de plazo y silencio, con su fuente.** Silencio de fin de respuesta de `::<n>`: **180 ms**
  (`SPEC-V3.6.md:453-457`, RF-APP-02 `[MOD r1.1]`; sustituye al valor inicial de 300 ms de r1.0,
  `SPEC-V3.6.md:449-451`); los dos siguen **"a fijar en T-B06"**, sin medida propia todavía: se dejan
  como parámetros configurables con esta referencia, no como constantes. Plazo máximo de espera por
  disparo: **2500 ms** (`SPEC-V3.6.md:453-456`, mismo `[MOD r1.1]`), también provisional hasta T-B06.
  **Las tramas `#...#` (`#V#`, `#GC#`, `#GN#`) no terminan por silencio, sino en el `#` de cierre**, y su
  plazo máximo es **2000 ms** (`SPEC-V3.6.md:455`), distinto del plazo de 2500 ms de un disparo `::<n>`:
  el parser de esta app sabe cuál de los dos plazos aplica según si espera un `#` de cierre o un
  silencio (§3). Pausa mínima antes de un nuevo envío (RF-APP-01, fuera de una cuarentena, ver abajo):
  1500 ms desde el envío anterior y **600 ms desde el último byte recibido**, 150 ms entre dos tramas
  `#` con el equipo ya identificado (`SPEC-V3.6.md:441-446`). **Un solo valor de pausa**: la r4 citaba
  además un "500 ms" (`delayTimeout` de `gui.c:346`) como silencio mínimo tras un plazo vencido; se
  retira, sustituido por la cuarentena de abajo, que ya cubre ese caso con su propio parámetro.
- **Cuarentena tras vencer el plazo de un disparo (ALTO, corrige r4).** Si vence el plazo de un disparo,
  esa lectura se anula: no se cuenta como "0" ni se descarta en silencio; se marca "sin respuesta" en el
  registro. A partir de ahí empieza una **cuarentena de `Q` ms** (`Q ≥` plazo máximo del disparo, **2500
  ms por defecto** hasta T-B06, configurable como el resto de esta ficha): durante la cuarentena, **todo
  lo que llegue por el enlace se descarta sin asignarlo a ningún disparo**, y se anota en `tramas.log`
  como "descartado en cuarentena" (RF-USR-15 bis); la app **no envía ninguna otra trama** mientras dura.
  Sólo cuando la cuarentena termina se aplica la pausa normal de RF-APP-01 (600 ms desde el último byte,
  arriba) antes de enviar la trama siguiente: la cuarentena y la pausa de RF-APP-01 **no se solapan**, se
  sirven una detrás de otra. Motivo: el firmware puede seguir "midiendo" bastante después de vencido el
  plazo de la app (el `delayTimeout` de 500 ms de `gui.c:346` es sólo el mínimo del propio firmware;
  `Q ≥` 2500 ms da margen para el caso peor, sin asumir que el firmware ya terminó); enviar antes
  arriesga que el firmware trate el byte de la app como el disparo `N+1` mientras aún procesa `N`
  (`gui.c:342-346`, la limpieza de búfer sólo corre **después** de terminar de procesar).
- **Respuesta tardía (condición 4).** `::<n>` no lleva identificador: lo que llega **fuera de plazo**
  (tras el tiempo máximo de espera de la petición, incluido lo que llega durante la cuarentena de
  arriba) se descarta y **no se asigna al disparo siguiente**: una respuesta tardía de la medida N no se
  cuenta como respuesta de la medida N+1, ni se promedia con ella, tanto si llega durante la cuarentena
  como si llega después.
- **Dos `::` en la misma ventana de espera.** Si llegan dos secuencias `::<n>` completas antes de que
  la app haya podido separar cuál pertenece a qué disparo (dos respuestas se solapan por temporización),
  **se descartan las dos**: no se adivina cuál es la buena.
- Ficha de trama partida: `::1` llega, no hay silencio todavía (`Tramas.extraer` no la da por completa,
  sigue esperando dígitos, `Tramas.java:294-296`); llega `23` más tarde, junta a `::123`, silencio
  confirmado → se lee **123**.
- Ficha de respuesta tardía: se envía el disparo N+1 tras el plazo de N; la respuesta de N llega
  después de ese envío → se descarta por fuera de plazo, no se suma a N+1 ni a la media. Con la
  cuarentena, en la práctica la app no llega a enviar N+1 tan pronto: el caso de prueba (T-USR-24(b))
  fuerza igualmente el envío de N+1 antes de que llegue la respuesta de N, para probar la regla de
  descarte aunque el ritmo normal la haga menos frecuente.
- **Prueba vista en rojo.** T-USR-24 se ejecuta primero contra un borrador sin esta regla (que asigna
  cualquier `::<n>` recibido al disparo pendiente en ese momento, sin mirar el plazo) montado sobre
  `EquipoSimulado`: el simulador se configura para entregar la respuesta del disparo N **después** de
  que la app ya haya enviado el disparo N+1; contra el borrador, la ficha falla (la respuesta tardía se
  suma a N+1); con la regla de descarte por plazo implementada, la misma ficha pasa. Esto se hace al
  escribir el código de esta app (B-3): no está ejecutado todavía, porque no hay código propio de esta
  app hasta ahora (encabezado de este documento).
- *Fuente:* `rtv-1.0:03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/Tramas.java:266,286-298`
  (`P_MEDIDA = Pattern.compile("::(\\d+)")` en `:266`; `!silencio && m.end() == rx.length()` → sigue
  esperando dígitos, en `:295-297`; **cita corregida, r3 decía `:256`, que es el Javadoc del enum
  `Tipo`, no el patrón**); `ecuacionesCalibracion.c:60-63` (`sprintf`/`strcpy`/`strcat`/`sendUartStr`
  que arman y mandan `::<entero>` sin terminador; **cita corregida, r3 decía `:55-58`, que es el inicio
  de la función y la llamada a `arreglar_dato`, no el envío**); `SPEC-V3.6.md:441-457` (Ritmo, silencio
  y plazo) y `:455` (2000 ms de plazo por trama `#`); V3.6 `gui.c:342-346` (limpieza de búfer, y el
  `delayTimeout` de 500 ms de `:346` que motiva `Q ≥` 2500 ms, no lo sustituye).

### 2.2 Incremento 2 — "Señal a señal" (depende de propuestas pendientes de Diego)

**RF-USR-07 — "Señal a señal": elegir o dar de alta señal.** Filtro por familia (15) y búsqueda
(SENAL-FILTRO); catálogo `08_Senales/senales.csv`, **376 filas**. El filtro y la búsqueda no dependen
de si la señal es medible: eso lo decide `color_fondo` en el momento de medir (RF-USR-03), no aquí. El
tipo de lámina no se pregunta. *Fuente:* `08_Senales/senales.csv`; recuento verificado con `python3`
(TDD §8.2, comando y salida).

**RF-USR-08 — "Señal a señal": identificador y estado, sólo mantenimiento.** Identificador de campo
**opcional** (serial de la señal, o lo asigna el operador; sin él, `codigo_catalogo` + GPS). Estado:
bien instalada, limpia, en condiciones (sí/no), observación. **No es parte del indicador**: columnas
propias del CSV, **nunca cambia** el resultado de RF-USR-10. *Fuente:* DECISIONES nota 10; nota 11,
punto (b) — "es para mantenimiento y no es tema de indicador y no multa" (literal).

**RF-USR-09 — "Señal a señal": referencia y lámina por defecto.** Por color, el **mínimo absoluto del
Manual de Señalización Vial 2024** para esa lámina (Tabla 2-5 IV o 2-6 XI); cambiable por el **valor de
instalación, con su año** (opcional). Lámina: por defecto la del catálogo (`lamina_minima`), columna
`lamina_origen = catalogo | inventario` si el operador la corrige; las **102 filas "IV (VI si
enrollable)"** no tienen tabla propia de tipo VI en el Manual: **se juzgan como IV y la app lo dice**
("lámina VI juzgada como IV, sin tabla propia"). La fila de ángulo depende de C-06 (abierta, ▸
propuesta pendiente de Diego en RF-USR-10), sin verificar contra el original. *Fuente:* DECISIONES nota
10; nota 9 (UMBRAL-LEY); SPEC-REG `:128-160` (C-01, C-06), `:221`; `08_Senales/senales.csv`
(`lamina_minima`, recuento en TDD §8.2).

**RF-USR-10 — Resultado: números siempre visibles, una sola convención de porcentaje, un solo
truncado (M-3, M-7).** El indicador muestra y exporta **siempre** `leído`, `umbral` y
`Δ% = trunc(100 · (leído − umbral) / umbral)`, **una sola división entera, truncada hacia cero al
final** (Java: división de `long`). **Corrige r3:** la fórmula anterior, `trunc((leído/umbral) × 100) −
100`, trunca primero y resta 100 después; para valores negativos eso **no** da el mismo resultado que
truncar una sola vez al final (`trunc(x) − 100 ≠ trunc(x − 100)` cuando `x − 100` cruza el cero); por
eso `TDD-V3.6.md` §8, T-USR-10 de la r3, escribía "= 49 − 100 = −50 %", una cuenta que en realidad da
−51 %, no −50 %. Verificado con `python3`
(división entera al estilo Java, TDD §8.2): `(160, 325) → −50`; `(160, 248) → −35`; `(160, 200) → −20`;
`(240, 310) → −22`; `(200, 240) → −16`; `(247, 248) → 0`. Si además se quiere la proporción
`leído / umbral`, va en una columna aparte, `proporcion_vs_umbral`, nombrada y explicada; nunca bajo el
mismo rótulo "%". **El dictamen se calcula con la división sin truncar**: con umbral 248 y leído 247,
el texto muestra "−0 %" pero el dictamen es **NO CUMPLE**, porque 247 < 248 exactos.

- **Sin instalación, mientras C-06 esté abierta.** El mínimo del Manual es **referencia, no dictamen**.
  Si `leído` < mínimo del Manual: texto **"POR DEBAJO del mínimo del Manual (referencia indicativa)"**,
  CSV con `dictamen = NO_DICTAMINABLE` (RF-REG-09, SPEC-REG `:574`) y `bajo_referencia = SI`. Si
  `leído` ≥ mínimo del Manual, no se dictamina CUMPLE tampoco: sigue `NO_DICTAMINABLE`,
  `bajo_referencia = NO`. ▸ **Propuesta pendiente de Diego:** la geometría del equipo es **0,2° de
  observación y −4° de entrada**, la misma fila de la Tabla 2-5 que da blanco IV = 360
  (`Manual-Senalizacion-Vial-Extracto.md:230`, repositorio V4.1/V5, sólo consulta). *Fuente del
  Manual de 2009 del equipo, corregida (r3 decía "confirmado en este árbol", y no lo está):*
  "Manual Reflectometro vertical.pdf" pág. 5, en `D:\@Proyect\IT\old\VERTICAL\1_V2-V3_18F4550_CCS\
  PROYECTO REFLECTOMETRO VERTICAL\Manuales del reflectometro vertical\` — **archivo confirmado en esa
  ruta antigua, no en este árbol**; contenido de la página 5 sin renderizar todavía en este entorno. El
  arquitecto observa que ese manual de 2009, aunque se renderice, **no basta por sí solo** para cerrar
  C-06 en el V3: **C-06 se cierra con el certificado de calibración de los patrones usados (que declara
  la geometría a la que están certificados) más una comprobación en el equipo**, no con una lectura de
  PDF. Sigue **▸ pendiente**; mientras tanto el mínimo del Manual queda como referencia, no dictamen.
- **Con instalación, medida con este mismo equipo.** Dictamen válido **sólo si** la fila de instalación
  declara `serie_equipo_instalacion` (RF-USR-13; la **serie**, no la MAC: la serie es el identificador
  que lee `#GN#` y sobrevive a un cambio de adaptador Bluetooth) y coincide con la serie del equipo
  conectado; si no consta o no coincide, `dictamen = NO_DICTAMINABLE` (A-3). ▸ **Propuesta pendiente de
  Diego (condición 3, umbral doble):** `NO CUMPLE` si `leído` baja del mínimo del Manual **o** del
  80 % de instalación (DECISIONES nota 9, "UMBRAL-LEY"), no sólo del 80 %. Prueba: blanco IV,
  instalación 310 (umbral 80 % = 248), mínimo Manual 360, leído 300 → cumple el 80 % (300 ≥ 248) pero
  **no cumple** el Manual (300 < 360): con la regla "o", el resultado es **NO CUMPLE**.

**Texto exacto, único en SPEC y TDD (B-2):**
- Sin instalación, con el mínimo real de la Tabla 2-5 (blanco IV = 360, no 325):
  `"Mínimo Manual 2024: 360 · Leído: 160 · −55 % · POR DEBAJO del mínimo del Manual (referencia
  indicativa)"` (con C-06 abierta) o `"... · −55 % · NO CUMPLE"` (si C-06 ya cerrada). El "325" de
  DECISIONES nota 11a es **la aritmética del propio ejemplo de Diego** ("valor mínimo del manual 325,
  valor leído 160 = −50 %"), no el valor de la Tabla 2-5 para blanco IV: se conserva como cita literal
  en TDD §8.2, pero no se usa como umbral de un caso de prueba de esta app.
- Con instalación: `"Instalación 2024: 310 · umbral 80 % = 248 · Leído: 160 · −35 % · NO CUMPLE"`.
- Con instalación y serie coincidente, cumple los dos mínimos: `"Instalación 2024: 310 · umbral 80 % =
  248 · Leído: 380 · +53 % · CUMPLE"` (380 ≥ 248 y 380 ≥ 360 del Manual; serie del equipo conectado
  igual a `serie_equipo_instalacion`).

*Fuente:* DECISIONES nota 11a (números y redondeo literales, sólo para la cita, no para el umbral de
prueba); nota 9 (UMBRAL-LEY, condición 3); SPEC-REG `:574` (RF-REG-09, NO_DICTAMINABLE);
`Manual-Senalizacion-Vial-Extracto.md:230` (blanco IV = 360, Tabla 2-5, 0,20°/−4°).

**RF-USR-11 — Historial, sólo en "Señal a señal" y opcional.** Por identificador y color, **de sólo
añadir**: fila de instalación (valor y año tecleados, o el mínimo del Manual si no hay) y luego cada
medida, con fecha, valor, `pct_vs_referencia = trunc(100 · leído / referencia)` y
`pct_vs_anterior = trunc(100 · (leído − anterior) / anterior)`, la misma convención de una sola
división entera truncada de RF-USR-10 (M-7, M-3); se muestra al medir. El ZIP de RF-USR-13 lleva
`medidas.csv` en **formato largo** (una fila por medida y color, para sacar la curva en Excel), con
mantenimiento (RF-USR-08) en columnas separadas del resultado (RF-USR-10); lista de columnas en TDD
§8. *Fuente:* DECISIONES nota 10; nota 11, punto (f) — el concepto ("instalación, medida, siguiente
medida") es de esa nota; los valores de ejemplo de TDD §8 son un cálculo de este trabajo, **no una cita
de Diego**; SPEC-REG `:588` (RF-REG-23).

**RF-USR-12 — Inventario: ACTIVA o RETIRADA, nunca desaparece.** Estado ACTIVA o RETIRADA ("sustituida
por `<id>`, fecha"). Al dar de alta con GPS cercano y mismo código que una ACTIVA, pregunta "¿sustituye
a `<id>`?", **con "no" por defecto** (B-3); si sí, la vieja pasa a RETIRADA. Dar de alta (o cargar por
CSV) sobre un identificador que está RETIRADA **la reactiva** (vuelve a ACTIVA) y la vista previa lo
dice explícitamente: "`<id>` estaba RETIRADA, se reactiva" (M-4e). Las RETIRADAS no salen en la lista de
trabajo, sí en histórico y exportación: **por eso el inventario visible no crece sin fin**, no porque
se borre nada (fija comportamiento de este trabajo, a partir de RF-USR-12: DECISIONES nota 11, punto
(c), no contiene esta frase). *Fuente:* DECISIONES nota 11, punto (c) — concepto de señal repuesta y
RETIRADA, no cita literal de "sustituye/reactiva".

**RF-USR-13 — Inventario de trabajo por CSV, tolerante; proyecto = concesionaria + vía.** Sin
plantilla aparte: exportar da un ZIP con `medidas.csv`, `inventario.csv` y `tramas.log` (RF-USR-15
bis); **`inventario.csv` es exactamente el formato que se carga de vuelta** (ficha que fija
comportamiento de este trabajo, a partir del principio de ida y vuelta de DECISIONES nota 11, punto
(d)); con el proyecto vacío sale sólo con encabezados (la plantilla). Columnas: `identificador`,
`codigo` (de `08_Senales/`), `latitud`, `longitud`, `lamina`, `valor_instalacion_<color>`,
`anio_instalacion`, `serie_equipo_instalacion` (la **serie** del equipo con que se midió la
instalación, no su MAC; A-3, base del dictamen "con instalación" de RF-USR-10), `estado` (`A` activa /
`R` retirada, retira en bloque, p. ej. 100 de 1500), `sustituida_por` (opcional con `R`). **Clave de
emparejamiento sin identificador declarado: `codigo` + GPS a menos de 15 m** (▸ propuesta pendiente de
Diego, umbral sin confirmar); a 15 m o más no empareja: se da de alta aparte, sin preguntar sustitución
(M-5); si hay **dos señales ACTIVAS del mismo `codigo` a menos de 15 m** (dos candidatas), la app no
elige por sí sola: lista las dos y pregunta cuál sustituir, o "ninguna, dar de alta aparte". Cargar el
mismo fichero dos veces seguidas **no cambia nada** (idempotente, M-4b). **Excel:** `identificador` y
`codigo` se leen **como texto**; si un valor parece número, fecha o notación científica (`"0042"`
truncado a `42`, `"1-2"` leído como fecha, `"1.23E+05"`), la app avisa antes de cargar (M-4c). **Carga
tolerante, no atómica**: detecta separador `;`/`,`, decimal con coma o punto, UTF-8 con o sin BOM y
Windows-1252, espacios, mayúsculas/acentos y comillas. Carga las filas buenas; lista las malas con
**fila, columna, valor y motivo en español** (ejemplo de este trabajo, no cita de Diego: "Fila 12,
latitud: '4,65.3' no es un número"; "Fila 30: el código 'SR-1' no está, ¿SR-01?"), exportable. Un
proyecto por concesionaria y vía; un equipo prestado abre otro. *Fuente:* DECISIONES nota 11, punto (d)
(tolerancia a los separadores `;`/`,`, carga o rechazo con motivo); el resto de la tolerancia de
formato (decimal coma/punto, BOM, Windows-1252) y los mensajes de ejemplo son ficha que fija
comportamiento de este trabajo.

**RF-USR-14 — Carga de inventario: nunca borra medidas, vista previa, valores congelados y deshacer.**
Las medidas son **de sólo añadir**: ninguna carga las borra ni las modifica (DECISIONES nota 11, punto
(e): "cargar un inventario nunca toca las medidas; vista previa y deshacer"); `valor_umbral`, `Δ%` y
`cumple`/`dictamen` de cada medida ya guardada **se congelan al medir**: corregir `valor_instalacion_
<color>` por una carga posterior de CSV **no recalcula** medidas pasadas (M-4d, ficha que fija
comportamiento de este trabajo), sólo afecta a las medidas siguientes. Una carga **añade, corrige
atributos, retira** (`R` de RF-USR-13, RF-USR-12) o **reactiva** (M-4e, ficha de este trabajo); una
señal ausente del fichero **sigue igual**, ni se retira ni se borra (misma nota 11e). Vista previa: "se
añaden N, se corrigen M, se reactivan Q, se retiran R, K con error" → Confirmar/Cancelar. **Deshacer**
revierte **sólo los cambios de esa carga** (altas, correcciones, reactivaciones y retiros; ficha de
este trabajo a partir de la nota 11e); si tras esa carga hubo altas o sustituciones hechas en campo,
deshacer **se bloquea** y la app lo dice: "no se puede deshacer: hay cambios posteriores en `<lista de
identificadores>`" (M-4a, ficha de este trabajo). Identificador existente con otro código es **error de
esa fila, no se sobrescribe** (ficha de este trabajo, no cita literal de Diego). Uso previsto: el
fichero lo prepara DPI en oficina; el operador sólo carga y mide. *Fuente:* DECISIONES nota 11, puntos
(d)-(e) (principio de sólo-añadir y de vista previa/deshacer); el resto de esta ficha (mensajes
exactos, bloqueo de deshacer, colisión de identificador) fija comportamiento de este trabajo.

## 3. Parser, y qué reutiliza de `rtv-1.0`, y qué no

**Parser propio (B-3):** sólo `#V,3.6,...#`, `#GC,...#`, `#GN,...#`, `#ERR,<motivo>#` y `::<n>` sin
terminador (RF-USR-16). Las tramas `#...#` se dan por completas en el **`#` de cierre**, con un plazo
máximo de **2000 ms** (`SPEC-V3.6.md:455`, RF-USR-16); si no llega el `#` de cierre dentro de ese plazo,
la trama se trata como "sin respuesta útil", igual que cualquier otro timeout de esta sección. Cualquier
`#ERR,<motivo>#` recibido en respuesta a `#GN#`/`#GC#` se reconoce genéricamente (el `motivo` es texto
corto sin comas, `PROTOCOLO-V3.6.md:71-72`: `PIN`, `BLOQUEADO`, `FORMATO`, `EEPROM`); en esta app sólo
se espera `FORMATO` con `exigir_362 = true` (equipo sin esas órdenes, RF-USR-01), pero el parser no
debe fallar si llega otro motivo (p. ej. `EEPROM`): lo registra en `tramas.log` y trata la sonda como
**"sin respuesta útil"**, igual que un timeout. **Si `#GN#` o `#GC#` no contestan nada** dentro de su
plazo (ni `#GN,...#` ni `#ERR,...#`), la app **no** concluye "actualice el firmware": eso es sólo para
un `#ERR,FORMATO#` explícito con `exigir_362 = true`. Un silencio puede ser el enlace, no la versión: se
ofrece reintentar la sonda (RF-USR-01). *Fuente:* `PROTOCOLO-V3.6.md:70-72` (formato de `motivo`);
`SPEC-V3.6.md:455` (plazo de trama `#`); RF-USR-01, RF-USR-16.

Prefijo `RTV10:` = `git show rtv-1.0:.../com/dpi/retrov36/`. Se tocan `EnlaceSerie.java` (308 líneas,
sin `Registro.*`, 9 llamadas `:188-272`), `Receptor.java` (sin `Tramas.extraer()` completo: se copia la
lógica de silencio de RF-USR-16, no la clase entera) y `Cliente.java` (221 líneas, sin
`Sesion`/`Deteccion`, `:125-126`); `Ritmo.java` (3 constantes) se copia literal.

**No se copian:** `Deteccion.java`, `Ops.java`, `Calibracion*.java`, `PerfilesEquipo.java`,
`Protocolo*.java`, `Tramas.java` entero (sólo el patrón de RF-USR-16, reescrito), `Bateria.java`: fuera
de USR-ALCANCE. Sin `Protocolo.java` desaparece la colisión de nombre de la r1.

## 4. Empaquetado (M-5)

`applicationId` propio por cliente: `com.dpi.retrousuario.<cliente>` (p. ej. `com.dpi.retrousuario.
coviandina`). `versionCode` empieza en 1, independiente de las apps de calibración. Firma del APK:
**pendiente, D-8** (sin decisión de Diego registrada todavía).

## 5. Contradicciones, puntos abiertos y qué no se toca

- **C-06.** El mínimo de RF-USR-09/10 depende de la geometría del equipo y de una tabla transcrita sin
  verificar contra el original. ▸ Propuesta pendiente de Diego: 0,2°/−4° (Manual del equipo de 2009,
  pág. 5, en `D:\@Proyect\IT\old\VERTICAL\1_V2-V3_18F4550_CCS\PROYECTO REFLECTOMETRO VERTICAL\
  Manuales del reflectometro vertical\` — **no está en este árbol**; página sin renderizar todavía).
  **Plan de cierre, del arquitecto:** el manual de 2009 no basta por sí solo; C-06 se cierra con el
  certificado de calibración de los patrones (que declara su geometría) más una comprobación en el
  equipo, no con la lectura del PDF sola. Sigue abierta.
- **C-USR-04, cerrada como propuesta.** M-3 (RF-USR-01) fija: con `exigir_362 = true` (el valor por
  defecto), `#ERR,FORMATO#` a `#GN#`/`#GC#` (3.6, 3.6.1) da "actualice el firmware" y no se mide; un
  timeout sin `#ERR#` no cuenta como eso (§3, parser). Con `exigir_362 = false`, se mide igual, con
  `estado_calibracion = sin_fecha` y `serie_origen = ninguna`. Pendiente de confirmación de Diego, como
  el resto de las propuestas de esta r5.
- **Emparejamiento de inventario por GPS.** ▸ Propuesta pendiente de Diego: 15 m (RF-USR-13); sin
  confirmar.
- **Cuarentena `Q` y repetición de disparo anulado, sin cita de Diego.** El valor por defecto de `Q`
  (2500 ms, RF-USR-16) y el límite de 2 repeticiones de un disparo anulado por plazo antes de anular la
  serie (RF-USR-04) son diseño de este trabajo: no están en `DECISIONES-Diego-2026-09-19.md`. Quedan
  como parámetros, a confirmar o corregir en T-B06.
- **Separador y decimal de `medidas.csv`.** ▸ Propuesta pendiente de Diego: `;` y `,` (Excel en
  español), sin fuente que lo mande; se corrige si Interventoría exige otro formato de exportación.
- **Qué no se toca.** Firmware y pantalla STONE, sin excepción; ninguna trama con "Requiere admin" =
  Sí. Los iconos `.svg` se convierten a `VectorDrawable`, tarea de desarrollo.
