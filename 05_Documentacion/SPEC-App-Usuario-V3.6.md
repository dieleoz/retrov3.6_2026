# SPEC — App de usuario (campo) V3.6: flujo y pantallas (r3)

**Nada de esto está medido contra un equipo ni un teléfono; no hay código propio de esta app todavía.**
**r3, 21-sep-2026: cierra el NO APTO del `arquitecto-iot` sobre la r2** (H-A1 a H-A4, M-1 a M-8, B-1 a
B-5, verificados por el agente principal). r2 venía de cerrar el NO APTO sobre la r1 con USR-SIMPLE,
SERIE-USR, SENAL-FILTRO, USR-ALCANCE, UMBRAL-LEY (nota 9) y USR-MODOS (nota 10). **Las precisiones del
21-sep que la r2 daba por "sin registrar" (notas 11-15) están registradas**: todas caen bajo la **nota
11 (USR-DETALLE)** de `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`, puntos (a)-(f); no
existen notas 12-15 separadas, y la r2 se equivocaba al citarlas. Lo que no esté en esa nota 11, en
otra fila de la tabla de decisiones o en una fuente ajena al código (protocolo, firmware, catálogo,
SPEC-REG) se marca **"fija comportamiento"**, no "requisito de Diego", o **"▸ propuesta pendiente de
Diego"** si depende de una confirmación suya todavía sin dar. Detalle numérico y de verificación en
`TDD-V3.6.md` §8. Fuentes: `PROTOCOLO-V3.6.md`, `SPEC-V3.6.md`, `calibracion_v36.c`,
`ecuacionesCalibracion.c`, `SPEC-Registro-Indicador-Interventoria.md` (SPEC-REG), `08_Senales/
senales.csv`, `rtv-1.0` (§3).

| Campo | Valor |
| :--- | :--- |
| Alcance | Va con el equipo, la usa el operador de campo. **Sólo firmware V3.6**: un APK por firmware y cliente |
| Modos | **"Medir y exportar"** (por defecto, cero tecleo) y **"Señal a señal"** (opcional, USR-MODOS) |
| Mide | Sólo la **lámina retrorreflectiva** (fondo y orla); símbolo y lo demás son mantenimiento |
| Umbral | Mínimo del Manual (referencia, C-06 abierta) o instalación con el 80 %; nunca 70 % de SFT |

---

## 1. Pantallas

1. **Aviso, antes de conectar.** "El equipo puede encender la luz y sonar" (RF-USR-01).
2. **Conectar y detectar.** Sólo `#V#`; si no empieza por `#V,3.6,`, "equipo no compatible" y nada más
   (RF-USR-01). Si empieza por `#V,3.6,` pero el equipo no responde a `#GN#`/`#GC#` con el formato
   esperado, "actualice el firmware del equipo" (RF-USR-01, M-3). Estado de calibración (RF-USR-02).
3. **Elegir modo.** "Medir y exportar" (por defecto) o "Señal a señal" (RF-USR-05).
4. **Medir y exportar.** Color (RF-USR-03), medir (RF-USR-04); cada disparo se guarda solo, **cero
   tecleo** (RF-USR-06); exportar ZIP (RF-USR-15 bis).
5. **Señal a señal.** Proyecto (RF-USR-13); elegir o dar de alta señal con sustitución (RF-USR-07,
   RF-USR-12); identificador y estado, mantenimiento (RF-USR-08); medir y resultado (RF-USR-09, 10).

Nada de PIN, "Avanzado", banco, cola ni acta: eso es la app de EMPRESA.

## 2. Requisitos (RF-USR)

**RF-USR-01 — Detección sólo V3.6, con aviso previo, y exige 3.6.2.** Aviso antes de conectar. Al
conectar, se envía **únicamente** `#V#`; si la respuesta no empieza por `#V,3.6,`, "equipo no
compatible" y ningún otro byte (nunca `9`, `6`, `e`, `@LEERV...`). Con `#V,3.6,` confirmado, la app
prueba `#GN#` y `#GC#` (ninguna mide). ▸ **Propuesta pendiente de Diego (M-3, cierra C-USR-04):** se
exige firmware **3.6.2 o posterior**, que responde a ambas; si cualquiera de las dos devuelve
`#ERR,FORMATO#` (3.6 o 3.6.1, que no las tienen, `PROTOCOLO-V3.6.md:51-54`), "actualice el firmware del
equipo" y **no se mide**. Esto es distinto de `#GN,NONE#` (equipo 3.6.2 sin serie grabada, RF-USR-06
(b)): ahí sí se mide, con la serie declarada por el operador. *Fuente:* `PROTOCOLO-V3.6.md:42` (formato
de `#V#`; **cita corregida, r2 decía `:24`**); en un V3 de 2020 el primer byte de cualquier trama
dispara la luz (`gui.c:294-296`, `01_Firmware/base_2020_d089f962/`); `CLAUDE.md` §4.

**RF-USR-02 — Estado de calibración, siempre visible, formato real de `#V#`.** El cuarto campo de
`#V,3.6,<fecha de compilación>,CAL|DEF,<máscara %04X>#` decide: `DEF` → **"sin calibración"**, **aunque
`#GC#` devuelva una fecha** (`#F` no borra `#SC`, `PROTOCOLO-V3.6.md:51`); `CAL` → se lee `#GC#`/`#GN#`;
vencimiento = fecha + 1 año, con el 29-feb de un año bisiesto venciendo el **28-feb** del siguiente
(**PA-02**, `SPEC-Registro-Indicador-Interventoria.md:567,609`); si `NONE`, vencida o sin registrar,
avisa y marca, no bloquea. *Fuente:* `calibracion_v36.c:635-642` (respuesta de `#V#`) y `:458-473`
(`enviarFecha`, `__DATE__` = fecha de compilación, no la de hoy); `PROTOCOLO-V3.6.md:34,42,51`;
SPEC-REG `:567-569` (RF-REG-02 a 04, PA-01, PA-02).

**RF-USR-03 — Mapa color → byte.** `blanco`→`1`, `amarillo`→`2`, `verde`→`3`, `rojo`→`4`, `azul`→`5`,
`anaranjado`→`6`; `marron` (café) usa el código del rojo, `4` (`CLAUDE.md` §10, RF-CAL-40). Son
**caracteres ASCII** (`0x31`-`0x36`), no los hex `0x01`-`0x0E` de la STONE: la r1 confundió `b` con
`0x0B`, pero el firmware compara contra `"b"` = `0x62` (`ecuacionesCalibracion.c:179`). Nunca código
opaco (`7`,`8`,`a`-`d`): ninguna fila pide tipo I. Fuera de medida: `negro`, fluorescentes y color
compuesto (§8.2 del TDD). *Fuente:* `ecuacionesCalibracion.c:141-164` (por carácter, no hex).

**RF-USR-04 — Disparos por color; una serie con un cero se repite entera.** `lecturas_por_color`
disparos, **3 por defecto** (Diego: "leído 3 veces promedio", DECISIONES nota 11a; distinto del 4 de
RF-REG-08 general), configurable; media, mínimo, trama cruda. Un `0` se muestra **"0 (saturado o
negativo)"**, nunca oculto ni nulo. ▸ **Propuesta pendiente de Diego (H-A3):** si **cualquier** lectura
de la serie es `0`, la serie entera queda **"medida no válida, repetir"**; nunca se promedia excluyendo
sólo los ceros (una serie `0, 0, 330` no da media 330 con n = 1: se repite entera). Motivo:
`reflectivityValue` es sin signo; una lectura negativa da un entero enorme, `> 4000` y se fuerza a `0`
(`ecuacionesCalibracion.c:49-54`, `arreglar_dato`, llamada en `:59` de `conversionDatoEnviar`); `0` casi
siempre es lámina muy degradada o mala lectura, no un dato válido bajo. *Fuente:* `SPEC-V3.6.md:501-503`;
`ecuacionesCalibracion.c:44-58`; DECISIONES nota 11a.

**RF-USR-05 — Dos modos (USR-MODOS).** "Medir y exportar" es el modo **por defecto**: mide muchas
señales sin escribir nada, el análisis lo hace el operador en Excel. "Señal a señal" es **opcional**,
para identificar cada señal y ver si cumple. *Fuente:* DECISIONES nota 10 ("no necesito que señal por
señal y son 1000 me tengas escribiendo en la app").

**RF-USR-06 — "Medir y exportar": cada medida se guarda sola, sin tecleo.** Fecha-hora, GPS (o vacío
con `gps_estado = sin_posicion`, RF-USR-15 bis), color, valor, MAC, serie (o "declarada"), `#GC#` y
vencimiento. **Ninguna pantalla pide un dato escrito.** CSV crudo, sin señal ni cumple/no cumple (eso
es de "Señal a señal"). *Fuente:* DECISIONES nota 10; SPEC-REG `:566` (RF-REG-01); SPEC-REG `:592`
(RF-REG-27, sin credenciales).

**RF-USR-07 — "Señal a señal": elegir o dar de alta señal.** Filtro por familia (15) y búsqueda
(SENAL-FILTRO); catálogo `08_Senales/senales.csv`, **376 filas**. El filtro y la búsqueda no dependen
de si la señal es medible: eso lo decide `color_fondo` en el momento de medir (RF-USR-03), no aquí. El
tipo de lámina no se pregunta. *Fuente:* `08_Senales/senales.csv`; recuento verificado con `python3`
(TDD §8.2, comando y salida).

**RF-USR-08 — "Señal a señal": identificador y estado, sólo mantenimiento.** Identificador de campo
**opcional** (serial de la señal, o lo asigna el operador; sin él, `codigo_catalogo` + GPS). Estado:
bien instalada, limpia, en condiciones (sí/no), observación. **No es parte del indicador**: columnas
propias del CSV, **nunca cambia** el resultado de RF-USR-10. *Fuente:* DECISIONES nota 10; nota 11b.

**RF-USR-09 — "Señal a señal": referencia y lámina por defecto.** Por color, el **mínimo absoluto del
Manual de Señalización Vial 2024** para esa lámina (Tabla 2-5 IV o 2-6 XI); cambiable por el **valor de
instalación, con su año** (opcional). Lámina: por defecto la del catálogo (`lamina_minima`), columna
`lamina_origen = catalogo | inventario` si el operador la corrige; las **102 filas "IV (VI si
enrollable)"** no tienen tabla propia de tipo VI en el Manual: **se juzgan como IV y la app lo dice**
("lámina VI juzgada como IV, sin tabla propia"). La fila de ángulo depende de C-06 (abierta, ▸
propuesta pendiente de Diego en RF-USR-10), sin verificar contra el original. *Fuente:* DECISIONES nota
10; nota 9 (UMBRAL-LEY); SPEC-REG `:128-160` (C-01, C-06), `:221`; `08_Senales/senales.csv`
(`lamina_minima`, recuento en TDD §8.2).

**RF-USR-10 — Resultado: números siempre visibles, una sola convención de porcentaje (M-7).** El
indicador muestra y exporta **siempre** `leído`, `umbral` y `Δ% = trunc((leído / umbral) × 100) − 100`,
**truncado hacia cero, nunca redondeado** (reproduce los "−50 %" y "−35 %" literales de DECISIONES nota
11a; el "−51 %" de la r2 venía de redondear). Si además se quiere la proporción `leído / umbral`, va en
una columna aparte, `proporcion_vs_umbral`, nombrada y explicada; nunca bajo el mismo rótulo "%".
**El dictamen se calcula con la división sin truncar**: con umbral 248 y leído 247, el texto muestra
"−0 %" pero el dictamen es **NO CUMPLE**, porque 247 < 248 exactos.

- **Sin instalación, mientras C-06 esté abierta.** El mínimo del Manual es **referencia, no dictamen**.
  Si `leído` < mínimo del Manual: texto **"POR DEBAJO del mínimo del Manual (referencia indicativa)"**,
  CSV con `dictamen = NO_DICTAMINABLE` (RF-REG-09, SPEC-REG `:574`) y `bajo_referencia = SI`. Si
  `leído` ≥ mínimo del Manual, no se dictamina CUMPLE tampoco: sigue `NO_DICTAMINABLE`,
  `bajo_referencia = NO`. ▸ **Propuesta pendiente de Diego:** la geometría del equipo es **0,2° de
  observación y −4° de entrada** (Manual del equipo de 2009, "Manual Reflectometro vertical.pdf" pág.
  5, `D:\@Proyect\IT\old\VERTICAL\1_V2-V3_18F4550_CCS\PROYECTO REFLECTOMETRO VERTICAL\Manuales del
  reflectometro vertical\`; archivo confirmado en este árbol, contenido de la página sin renderizar
  todavía en este entorno — pendiente de una segunda comprobación antes de cerrar C-06); si Diego la
  confirma, el mínimo del Manual pasa a dictamen (deja de ser `NO_DICTAMINABLE` por sí solo).
- **Con instalación, medida con este mismo equipo.** Dictamen válido **sólo si** la fila de instalación
  declara la serie/MAC del equipo con que se midió y coincide con la del equipo conectado; si no consta
  o no coincide, `dictamen = NO_DICTAMINABLE`. ▸ **Propuesta pendiente de Diego (condición 3, umbral
  doble):** `NO CUMPLE` si `leído` baja del mínimo del Manual **o** del 80 % de instalación
  (DECISIONES nota 9, "UMBRAL-LEY"), no sólo del 80 %. Prueba: blanco IV, instalación 310 (umbral 80 %
  = 248), mínimo Manual 360, leído 300 → cumple el 80 % (300 ≥ 248) pero **no cumple** el Manual
  (300 < 360): con la regla "o", el resultado es **NO CUMPLE**.

**Texto exacto, único en SPEC y TDD (B-2):**
- Sin instalación: `"Mínimo Manual 2024: 325 · Leído: 160 · −50 % · POR DEBAJO del mínimo del Manual
  (referencia indicativa)"` (con C-06 abierta) o `"... · −50 % · NO CUMPLE"` (si C-06 ya cerrada).
- Con instalación: `"Instalación 2024: 310 · umbral 80 % = 248 · Leído: 160 · −35 % · NO CUMPLE"`.

*Fuente:* DECISIONES nota 11a (números y redondeo literales); nota 9 (UMBRAL-LEY, condición 3);
SPEC-REG `:574` (RF-REG-09, NO_DICTAMINABLE).

**RF-USR-11 — Historial, sólo en "Señal a señal" y opcional.** Por identificador y color, **de sólo
añadir**: fila de instalación (valor y año tecleados, o el mínimo del Manual si no hay) y luego cada
medida, con fecha, valor, `pct_vs_referencia = trunc(leído / referencia × 100)` y
`pct_vs_anterior = trunc((leído / anterior − 1) × 100)`, la misma convención de truncado de RF-USR-10
(M-7); se muestra al medir. El ZIP de RF-USR-13 lleva `medidas.csv` en **formato largo** (una fila por
medida y color, para sacar la curva en Excel), con mantenimiento (RF-USR-08) en columnas separadas del
resultado (RF-USR-10); lista de columnas en TDD §8. *Fuente:* DECISIONES nota 10; nota 11f (concepto:
"el histórico es la curva: instalación, medida, siguiente medida" — los valores de ejemplo de TDD §8
son un cálculo de este trabajo, **no una cita de Diego**); SPEC-REG `:588` (RF-REG-23).

**RF-USR-12 — Inventario: ACTIVA o RETIRADA, nunca desaparece.** Estado ACTIVA o RETIRADA ("sustituida
por `<id>`, fecha"). Al dar de alta con GPS cercano y mismo código que una ACTIVA, pregunta "¿sustituye
a `<id>`?", **con "no" por defecto** (B-3); si sí, la vieja pasa a RETIRADA. Dar de alta (o cargar por
CSV) sobre un identificador que está RETIRADA **la reactiva** (vuelve a ACTIVA) y la vista previa lo
dice explícitamente: "`<id>` estaba RETIRADA, se reactiva" (M-4e). Las RETIRADAS no salen en la lista de
trabajo, sí en histórico y exportación: **por eso el inventario visible no crece sin fin**, no porque
se borre nada. *Fuente:* DECISIONES nota 11c.

**RF-USR-13 — Inventario de trabajo por CSV, tolerante; proyecto = concesionaria + vía.** Sin
plantilla aparte: exportar da un ZIP con `medidas.csv`, `inventario.csv` y `tramas.log` (RF-USR-15
bis); **`inventario.csv` es exactamente el formato que se carga de vuelta**, así se carga también el de
la concesionaria; con el proyecto vacío sale sólo con encabezados (la plantilla). Columnas:
`identificador`, `codigo` (de `08_Senales/`), `latitud`, `longitud`, `lamina`, `valor_instalacion_<color>`,
`anio_instalacion`, `estado` (`A` activa / `R` retirada, retira en bloque, p. ej. 100 de 1500),
`sustituida_por` (opcional con `R`). **Clave de emparejamiento sin identificador declarado: `codigo` +
GPS a menos de 15 m** (▸ propuesta pendiente de Diego, umbral a confirmar); cargar el mismo fichero dos
veces seguidas **no cambia nada** (idempotente, M-4b). **Excel:** `identificador` y `codigo` se leen
**como texto**; si un valor parece número, fecha o notación científica (`"0042"` truncado a `42`,
`"1-2"` leído como fecha, `"1.23E+05"`), la app avisa antes de cargar (M-4c). **Carga tolerante, no
atómica**: detecta separador `;`/`,`, decimal con coma o punto, UTF-8 con o sin BOM y Windows-1252,
espacios, mayúsculas/acentos y comillas. Carga las filas buenas; lista las malas con **fila, columna,
valor y motivo en español** ("Fila 12, latitud: '4,65.3' no es un número"; "Fila 30: el código 'SR-1'
no está, ¿SR-01?"), exportable. Un proyecto por concesionaria y vía; un equipo prestado abre otro.
*Fuente:* DECISIONES nota 11d.

**RF-USR-14 — Carga de inventario: nunca borra medidas, vista previa, valores congelados y deshacer.**
Las medidas son **de sólo añadir**: ninguna carga las borra ni las modifica; `valor_umbral`, `Δ%` y
`cumple`/`dictamen` de cada medida ya guardada **se congelan al medir**: corregir `valor_instalacion_
<color>` por una carga posterior de CSV **no recalcula** medidas pasadas (M-4d), sólo afecta a las
medidas siguientes. Una carga **añade, corrige atributos, retira** (`R` de RF-USR-13, RF-USR-12) o
**reactiva** (M-4e); una señal ausente del fichero **sigue igual**, ni se retira ni se borra. Vista
previa: "se añaden N, se corrigen M, se reactivan Q, se retiran R, K con error" → Confirmar/Cancelar.
**Deshacer** revierte **sólo los cambios de esa carga** (altas, correcciones, reactivaciones y
retiros); si tras esa carga hubo altas o sustituciones hechas en campo, deshacer **se bloquea** y la app
lo dice: "no se puede deshacer: hay cambios posteriores en `<lista de identificadores>`" (M-4a).
Identificador existente con otro código es **error de esa fila**, no se sobrescribe. Uso previsto: el
fichero lo prepara DPI en oficina; el operador sólo carga y mide. *Fuente:* DECISIONES nota 11d-e.

**RF-USR-15 — Sin modo administrador ni red.** Ninguna pantalla ofrece `#L`, `#S`, `#ST`, `#FT`, `#SC`,
`#SN`, `#P`, `#F`. Lista blanca de transmisión: `#V#`, `#GC#`, `#GN#`, y `1`-`6` (RF-USR-03); nunca `e`
(`#K#`/`#KC#` no requieren admin pero tampoco entran, son diagnóstico de pantalla). Los dos modos,
completos, funcionan en modo avión; **sin permiso o sin posición GPS, se mide igual**: columnas
`latitud`/`longitud` vacías y `gps_estado = sin_posicion` (M-8), la app nunca bloquea una medida por
falta de GPS. *Fuente:* `PROTOCOLO-V3.6.md` §3 ("Requiere admin"); SPEC-REG `:588` (RF-REG-23).

**RF-USR-15 bis — Registro de tramas dentro del ZIP exportado (M-6).** Toda petición y respuesta
Bluetooth de la sesión (incluidas las de detección) se anota en `tramas.log`, dentro del mismo ZIP de
RF-USR-13: hora, trama enviada, trama recibida (o "timeout"), byte a byte. La lista blanca de RF-USR-15
se comprueba **en ejecución, contra este registro** (qué se envió de verdad), no sólo por `grep` sobre
el código fuente (TDD T-USR-19). *Fuente:* CLAUDE.md §2 (verificación medida, no de código); M-6.

**RF-USR-16 — Lectura de tramas `::<n>`, sin terminador (H-A4).** `::<n>` no lleva terminador: la
trama se da por completa **por silencio** (Ritmo: 1500 ms desde el envío anterior, 600 ms desde el
último byte, 150 ms entre dos tramas `#` con el equipo ya identificado; `SPEC-V3.6.md:441-446`), nunca
por un carácter de cierre. El búfer de recepción se **vacía antes de cada envío**; lo que llega **fuera
de plazo** (tras el tiempo máximo de espera de la petición) se descarta y **no se asigna al disparo
siguiente**: una respuesta tardía de la medida N no se cuenta como respuesta de la medida N+1. Ficha de
trama partida: `::1` llega, no hay silencio todavía (`Tramas.extraer` no la da por completa, sigue
esperando dígitos, `Tramas.java:294-296`); llega `23` más tarde, junta a `::123`, silencio confirmado →
se lee **123**. Ficha de respuesta tardía: se envía el disparo N+1 tras el plazo de N; la respuesta de N
llega después de ese envío → se descarta por fuera de plazo, no se suma a N+1 ni a la media. *Fuente:*
`rtv-1.0:03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/Tramas.java:256,286-295` (patrón
`::(\d+)`, `m.end() == rx.length()` sin silencio → sigue esperando); `ecuacionesCalibracion.c:55-58`
(el firmware manda `::<entero>` sin terminador); `SPEC-V3.6.md:441-446` (Ritmo).

## 3. Qué reutiliza de `rtv-1.0`, y qué no

Prefijo `RTV10:` = `git show rtv-1.0:.../com/dpi/retrov36/`. Se tocan `EnlaceSerie.java` (308 líneas,
sin `Registro.*`, 9 llamadas `:188-272`), `Receptor.java` (sin `Tramas.extraer()` completo: se copia la
lógica de silencio de RF-USR-16, no la clase entera) y `Cliente.java` (221 líneas, sin
`Sesion`/`Deteccion`, `:125-126`); `Ritmo.java` (3 constantes) se copia literal.

**No se copian:** `Deteccion.java`, `Ops.java`, `Calibracion*.java`, `PerfilesEquipo.java`,
`Protocolo*.java`, `Tramas.java` entero (sólo el patrón de RF-USR-16, reescrito), `Bateria.java`: fuera
de USR-ALCANCE. **Parser propio**: sólo `#V,3.6,...#`, `#GC,...#`, `#GN,...#` y `::<n>` sin terminador
(RF-USR-16); sin `Protocolo.java` desaparece la colisión de nombre de la r1.

## 4. Empaquetado (M-5)

`applicationId` propio por cliente: `com.dpi.retrousuario.<cliente>` (p. ej. `com.dpi.retrousuario.
coviandina`). `versionCode` empieza en 1, independiente de las apps de calibración. Firma del APK:
**pendiente, D-8** (sin decisión de Diego registrada todavía).

## 5. Contradicciones, puntos abiertos y qué no se toca

- **C-06.** El mínimo de RF-USR-09/10 depende de la geometría del equipo y de una tabla transcrita sin
  verificar contra el original. ▸ Propuesta pendiente de Diego: 0,2°/−4° (Manual del equipo de 2009,
  pág. 5, citado en RF-USR-10); archivo confirmado en este árbol, página sin renderizar en este entorno.
- **C-USR-04, cerrada como propuesta.** M-3 (RF-USR-01) fija: se exige firmware 3.6.2; con
  `#ERR,FORMATO#` a `#GN#`/`#GC#` (3.6, 3.6.1), "actualice el firmware" y no se mide. Pendiente de
  confirmación de Diego, como el resto de las propuestas de esta r3.
- **Qué no se toca.** Firmware y pantalla STONE, sin excepción; ninguna trama con "Requiere admin" =
  Sí. Los iconos `.svg` se convierten a `VectorDrawable`, tarea de desarrollo.
