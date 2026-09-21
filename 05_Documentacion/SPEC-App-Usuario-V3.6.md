# SPEC — App de usuario (campo) V3.6: flujo y pantallas (r2)

**Nada de esto está medido contra un equipo ni un teléfono; no hay código propio de esta app todavía.**
**r2, 21-sep-2026: reescrita completa** tras NO APTO del `arquitecto-iot` sobre la r1 y las decisiones
de Diego en `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`: USR-SIMPLE, SERIE-USR,
SENAL-FILTRO, USR-ALCANCE, UMBRAL-LEY (nota 9) y **USR-MODOS (nota 10), que manda sobre las
anteriores**. Precisiones del 21-sep, **todavía sin registrar** ahí (pendientes notas 11-15: números y
estado sólo mantenimiento, inventario con sustitución, carga tolerante, carga que nunca borra medidas,
retiro en bloque por CSV). Detalle numérico y de verificación en `TDD-V3.6.md` §8. Fuentes:
`PROTOCOLO-V3.6.md`, `SPEC-V3.6.md`, `SPEC-Registro-Indicador-Interventoria.md` (SPEC-REG),
`08_Senales/senales.csv`, `rtv-1.0` (§3).

| Campo | Valor |
| :--- | :--- |
| Alcance | Va con el equipo, la usa el operador de campo. **Sólo firmware V3.6**: un APK por firmware y cliente |
| Modos | **"Medir y exportar"** (por defecto, cero tecleo) y **"Señal a señal"** (opcional, USR-MODOS) |
| Mide | Sólo la **lámina retrorreflectiva** (fondo y orla); símbolo y lo demás son mantenimiento |
| Umbral | Mínimo del Manual 2024 u 80 % de instalación, nunca 70 % de SFT; sólo en "Señal a señal" |

---

## 1. Pantallas

1. **Aviso, antes de conectar.** "El equipo puede encender la luz y sonar" (RF-USR-01).
2. **Conectar y detectar.** Sólo `#V#`; si no empieza por `#V,3.6,`, "equipo no compatible" y nada más
   (RF-USR-01). Estado de calibración (RF-USR-02).
3. **Elegir modo.** "Medir y exportar" (por defecto) o "Señal a señal" (RF-USR-05).
4. **Medir y exportar.** Color (RF-USR-03), medir (RF-USR-04); cada disparo se guarda solo, **cero
   tecleo** (RF-USR-06); exportar CSV.
5. **Señal a señal.** Proyecto (RF-USR-13); elegir o dar de alta señal con sustitución (RF-USR-07,
   RF-USR-12); identificador y estado, mantenimiento (RF-USR-08); medir y resultado (RF-USR-09, 10).

Nada de PIN, "Avanzado", banco, cola ni acta: eso es la app de EMPRESA.

## 2. Requisitos (RF-USR)

**RF-USR-01 — Detección sólo V3.6, con aviso previo.** Aviso antes de conectar. Al conectar, se envía
**únicamente** `#V#`; si la respuesta no empieza por `#V,3.6,`, "equipo no compatible" y ningún otro
byte (nunca `9`, `6`, `e`, `@LEERV...`). *Fuente:* `PROTOCOLO-V3.6.md:24`; en un V3 de 2020 el primer
byte de cualquier trama dispara la luz (`gui.c:294-296`, `01_Firmware/base_2020_d089f962/`); `CLAUDE.md`
§4.

**RF-USR-02 — Estado de calibración, siempre visible.** `#GC#`/`#GN#`; vencimiento = fecha + 1 año; si
`NONE`, vencida o sin registrar, avisa y marca, no bloquea. *Fuente:* `PROTOCOLO-V3.6.md:34,42`;
SPEC-REG `:567-569` (RF-REG-02 a 04, PA-01).

**RF-USR-03 — Mapa color → byte.** `blanco`→`1`, `amarillo`→`2`, `verde`→`3`, `rojo`→`4`, `azul`→`5`,
`anaranjado`→`6`; `marron` (café) usa el código del rojo, `4` (`CLAUDE.md` §10, RF-CAL-40). Son
**caracteres ASCII** (`0x31`-`0x36`), no los hex `0x01`-`0x0E` de la STONE: la r1 confundió `b` con
`0x0B`, pero el firmware compara contra `"b"` = `0x62` (`ecuacionesCalibracion.c:179`). Nunca código
opaco (`7`,`8`,`a`-`d`): ninguna fila pide tipo I. Fuera de medida: `negro`, fluorescentes y color
compuesto (SPEC-REG `:221`; TDD §8). *Fuente:* `ecuacionesCalibracion.c:141-164` (por carácter, no hex).

**RF-USR-04 — Disparos por color.** `lecturas_por_color` disparos, **3 por defecto** (Diego: "leído 3
veces promedio"; distinto del 4 de RF-REG-08 general), configurable; media, mínimo, trama cruda. Un
`0` se muestra **"0 (saturado o negativo)"**, nunca oculto ni nulo, y **no entra en la media**.
*Fuente:* `SPEC-V3.6.md:501-503`; `ecuacionesCalibracion.c:49-54`; precisión de Diego (nota 11).

**RF-USR-05 — Dos modos (USR-MODOS).** "Medir y exportar" es el modo **por defecto**: mide muchas
señales sin escribir nada, el análisis lo hace el operador en Excel. "Señal a señal" es **opcional**,
para identificar cada señal y ver si cumple. *Fuente:* nota 10 ("no necesito que señal por señal y son
1000 me tengas escribiendo en la app").

**RF-USR-06 — "Medir y exportar": cada medida se guarda sola, sin tecleo.** Fecha-hora, GPS, color,
valor, MAC, serie (o "declarada"), `#GC#` y vencimiento. **Ninguna pantalla pide un dato escrito.** CSV
crudo, sin señal ni cumple/no cumple (eso es de "Señal a señal"). *Fuente:* nota 10; SPEC-REG `:566`
(RF-REG-01); SPEC-REG `:592` (RF-REG-27, sin credenciales).

**RF-USR-07 — "Señal a señal": elegir o dar de alta señal.** Filtro por familia (15) y búsqueda
(SENAL-FILTRO); catálogo `08_Senales/senales.csv`, 376 filas, 363 piden IV o XI (código 1-6), 13 fuera
de esta app (verificado con `python3`, ficha de TDD §8). El tipo de lámina no se pregunta. *Fuente:*
`08_Senales/senales.csv`.

**RF-USR-08 — "Señal a señal": identificador y estado, sólo mantenimiento.** Identificador de campo
**opcional** (serial de la señal, o lo asigna el operador; sin él, `codigo_catalogo` + GPS). Estado:
bien instalada, limpia, en condiciones (sí/no), observación. **No es parte del indicador**: columnas
propias del CSV, **nunca cambia** el resultado de RF-USR-10. *Fuente:* nota 10; nota 11.

**RF-USR-09 — "Señal a señal": referencia por defecto = mínimo del Manual.** Por color, el **mínimo
absoluto del Manual de Señalización Vial 2024** para esa lámina (Tabla 2-5 IV o 2-6 XI); cambiable por
el **valor de instalación, con su año** (opcional), que añade el 80 % (ANI AT4). La fila de ángulo
depende de C-06 (abierta), sin verificar contra el original. *Fuente:* nota 10; nota 9 (UMBRAL-LEY);
SPEC-REG `:128-160`; `P_RetroReflectometro_Vertical/04_Manuales/Manual-...-Extracto.md:223-249`.

**RF-USR-10 — Resultado, con números.** Umbral = mínimo del Manual o, con instalación, el 80 % de ese
valor. Se muestra **leído, umbral y % = (leído/umbral) − 1**, redondeado, con CUMPLE/NO CUMPLE. Caso
de prueba (TDD §8): `"Mínimo Manual 2024: 325 · Leído: 160 · 49 % → −51 % · NO CUMPLE"`; con
instalación: `"Instalación 2024: 310 · umbral 80 % = 248 · Leído 160 → 65 % → −35 % · NO CUMPLE"`.
*Fuente:* precisión de Diego (nota 11); SPEC-REG `:574` (RF-REG-09, NO_DICTAMINABLE si falta referencia).

**RF-USR-11 — Historial, sólo en "Señal a señal" y opcional.** Por identificador y color, **de sólo
añadir**: fila de instalación (valor y año tecleados, o el mínimo del Manual si no hay) y luego cada
medida, con fecha, valor, % frente a instalación y % frente a la anterior; se muestra al medir. El ZIP
de RF-USR-13 lleva `medidas.csv` en **formato largo** (una fila por medida y color, para sacar la
curva en Excel), con mantenimiento (RF-USR-08) en columnas separadas del resultado (RF-USR-10); lista
de columnas en TDD §8. *Fuente:* nota 10; nota 11 (histórico); SPEC-REG `:588` (RF-REG-23).

**RF-USR-12 — Inventario: ACTIVA o RETIRADA, nunca desaparece.** Estado ACTIVA o RETIRADA ("sustituida
por `<id>`, fecha"). Al dar de alta con GPS cercano y mismo código que una ACTIVA, pregunta "¿sustituye
a `<id>`?"; si sí, la vieja pasa a RETIRADA. Las RETIRADAS no salen en la lista de trabajo, sí en
histórico y exportación. *Fuente:* precisión de Diego (nota 12).

**RF-USR-13 — Inventario de trabajo por CSV, tolerante; proyecto = concesionaria + vía.** Sin
plantilla aparte: exportar da un ZIP con `medidas.csv` e `inventario.csv`; **`inventario.csv` es
exactamente el formato que se carga de vuelta**, así se carga también el de la concesionaria; con el
proyecto vacío sale sólo con encabezados (la plantilla). Columnas: `identificador`, `codigo` (de
`08_Senales/`), `latitud`, `longitud`, `lamina`, `valor_instalacion_<color>`, `anio_instalacion`,
`estado` (`A` activa / `R` retirada, retira en bloque, p. ej. 100 de 1500), `sustituida_por` (opcional
con `R`). **Carga tolerante, no atómica**: detecta separador `;`/`,`, decimal con coma o punto, UTF-8
con o sin BOM y Windows-1252, espacios, mayúsculas/acentos y comillas. Carga las filas buenas; lista
las malas con **fila, columna, valor y motivo en español** ("Fila 12, latitud: '4,65.3' no es un
número"; "Fila 30: el código 'SR-1' no está, ¿SR-01?"), exportable. Un proyecto por concesionaria y
vía; un equipo prestado abre otro. *Fuente:* precisión de Diego (nota 13).

**RF-USR-14 — Carga de inventario: nunca borra medidas, vista previa y deshacer.** Las medidas son
**de sólo añadir**: ninguna carga las borra ni las modifica. Una carga **añade, corrige atributos o
retira** (`R` de RF-USR-13, RF-USR-12); **retirar es sólo marcar, nunca borra la señal ni sus
medidas**. Una señal ausente del fichero **sigue igual**, ni se retira ni se borra. Vista previa:
"se añaden N, se corrigen M, se retiran R, K con error" → Confirmar/Cancelar. **Deshacer** revierte
altas, correcciones y retiros, vuelve exactamente al inventario anterior. Identificador existente con
otro código es **error de esa fila**, no se sobrescribe. Uso previsto: el fichero lo prepara DPI en
oficina; el operador sólo carga y mide. *Fuente:* precisión de Diego (notas 14-15).

**RF-USR-15 — Sin modo administrador ni red.** Ninguna pantalla ofrece `#L`, `#S`, `#ST`, `#FT`, `#SC`,
`#SN`, `#P`, `#F`. Lista blanca de transmisión: `#V#`, `#GC#`, `#GN#`, y `1`-`6` (RF-USR-03); nunca `e`
(`#K#`/`#KC#` no requieren admin pero tampoco entran, son diagnóstico de pantalla). Los dos modos,
completos, funcionan en modo avión. *Fuente:* `PROTOCOLO-V3.6.md` §3 ("Requiere admin"); SPEC-REG
`:588` (RF-REG-23).

## 3. Qué reutiliza de `rtv-1.0`, y qué no

Prefijo `RTV10:` = `git show rtv-1.0:.../com/dpi/retrov36/`. Se tocan `EnlaceSerie.java` (308 líneas,
sin `Registro.*`, 9 llamadas `:188-272`), `Receptor.java` (sin `Tramas.extraer()`) y `Cliente.java`
(221 líneas, sin `Sesion`/`Deteccion`, `:125-126`); `Ritmo.java` (3 constantes) se copia literal.

**No se copian:** `Deteccion.java`, `Ops.java`, `Calibracion*.java`, `PerfilesEquipo.java`,
`Protocolo*.java`, `Tramas.java` entero, `Bateria.java`: fuera de USR-ALCANCE. **Parser propio**: sólo
`#V,3.6,...#`, `#GC,...#`, `#GN,...#` y `::<n>` sin terminador (`ecuacionesCalibracion.c:60-63`); sin
`Protocolo.java` desaparece la colisión de nombre de la r1.

## 4. Contradicciones, puntos abiertos y qué no se toca

- **C-USR-04.** Con un equipo sin `#GN#` (pre-3.6.2), RF-USR-06 exige serie declarada; no se dice si
  eso bloquea o basta "SIN SERIE" (heredada de r1, sin cerrar).
- **C-06.** El mínimo de RF-USR-09 depende de la geometría del equipo (no documentada) y de una tabla
  transcrita sin verificar contra el original.
- **Qué no se toca.** Firmware y pantalla STONE, sin excepción; ninguna trama con "Requiere admin" =
  Sí. Los iconos `.svg` se convierten a `VectorDrawable`, tarea de desarrollo.
