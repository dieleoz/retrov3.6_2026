# SPEC — App única: qué puede cada familia y qué ZIP de calibración le corresponde

**Nada de este documento está probado contra un equipo ni en teléfono.** Sale de leer el fuente y de
ejecutar la herramienta de coeficientes en esta máquina. Los dos únicos hechos medidos que se citan
llevan la orden con la que se midieron y vienen de `V4.6:05_Documentacion/CAMINO-del-ZIP-V4.6.md`.
Lo que aquí se **propone** no está implementado: se distingue en cada apartado.

---

## Fuentes, y cómo leer las citas

| Prefijo | Qué es |
| :--- | :--- |
| `RTV10:` | Árbol `D:\IT\wt_rtv10`, rama `rtv-1.0`, **fijado en el commit `d7d7b4d`** (*RTV 1.0.0-rc5*). Ruta real: `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` |
| `ASSET:` | `03_App_Movil/RetroV36/app/src/main/assets/` del mismo commit |
| `TOOL:` | `D:\IT\P_RetroVertical_V4.6\06_Calibracion\tools\` |
| `SPEC-U:` | `V3.6:05_Documentacion/SPEC-App-Unica-V36-V46.md` |
| `CAMINO:` | `V4.6:05_Documentacion/CAMINO-del-ZIP-V4.6.md` |

**Por qué se fija el commit.** Hay otro subagente escribiendo en ese árbol mientras se redacta esto.
Las citas se hicieron sobre una extracción `git archive d7d7b4d`, no sobre el árbol de trabajo, para
que las líneas no bailen. `CAMINO` cita contra `8b0fc9d` (rc3); **entre rc3 y rc5 se movieron cosas
de este documento** y se dice dónde. Quien lea contra rc6 tiene que releerlas.

Este documento **continúa** la numeración de `SPEC-U`, que llega a RF-APP-U27, C-U06 y R-U14. Aquí
empiezan RF-APP-U28, C-U07 y R-U15. No repite lo que `SPEC-U` ya dice: lo cita.

**Fuera de alcance, a propósito:** metrología (curvas, criterios, dispensas), el candado de
calibración de la V4.6 y la lista blanca de tramas. Ninguna de las tres se toca aquí.

---

## 1. Las cuatro familias, en lo que ve el operador

### 1.1 Cuáles son y de dónde sale la lista

Son cuatro implementaciones de una interfaz, no cuatro ramas de `if`: `RTV10:Protocolo.java:14-26`
declara el enum `Firmware` con `F2020`, `F36`, `V4_ORIGINAL`, `F46` y `DESCONOCIDO`, y cada una tiene
su clase (`ProtocoloV2020`, `ProtocoloV36`, `ProtocoloV4Original`, `ProtocoloV46`).

Lo que **es dato** y no sintaxis vive en `ASSET:firmwares.csv`, una fila por familia:

```
firmware,x_min,x_max,cola_banco,coherencia_def,bateria_unidad,nota
F36,600,4300,cola_banco_P1-P132.csv,G,n9,...
F2020,500,4300,cola_banco_P1-P132.csv,-,n9,...
V4_ORIGINAL,,,,-,-,solo @LEERV: medir y verificar (RF-APP-U07)
F46,0,4095,cola_banco_representativo_v46.csv,E,pct,...
```

### 1.2 La tabla que hoy no está escrita en ningún sitio — EXISTE HOY

Lo que sigue es el comportamiento **de la rc5**, traducido a lo que el operador ve en pantalla. Cada
casilla lleva su línea.

| Lo que el operador quiere hacer | **V3 2020** (`F2020`) | **V3.6** (`F36`) | **V4 original** (`V4_ORIGINAL`) | **V4.6** (`F46`) |
| :--- | :--- | :--- | :--- | :--- |
| **Conectar y que la app sepa qué es** | Sí, por los bytes `9` y `6` | Sí, por `#V#` | Sí, sólo por la sonda `@LEERV,BLA,2@` | Sí, por `#V#` |
| **2. Medida de patrones** | Sí | Sí | Sí, **el botón dice "(R con @LEERV)"** (`RTV10:ConexionActivity.java:165`) | Sí |
| **Ver la lectura interna `x`** | Sí, **calculada por la app** invirtiendo el código 6 (`ProtocoloV2020.java:34-38` deja `tramaX` en `null` y hereda `daX()` de `ProtocoloV36.java:63-65`) | Sí, con `e` (`ProtocoloV36.java:68-70`) | **No** (`ProtocoloV4Original.java:69-71`) | Sí, con `#X,k#` (`ProtocoloV46.java:79-81`) |
| **3. Campaña de calibración** | Sí | Sí | **Botón deshabilitado**, con el motivo escrito al lado (`ConexionActivity.java:160,164`) | Sí |
| **Tomar muestras (banco)** | Sí, banco completo `P1-P132` | Sí, banco completo `P1-P132` | **No.** El botón dice *"Equipo V4 sin firmware V4.6: sólo medir y verificar. Sin x no hay banco."* (`ProtocoloV4Original.java:13,146-148`) | Sí, **banco representativo V4.6** y sólo ése (`ProtocoloV46.java:145-147` + `firmwares.csv`) |
| **Calibrar este equipo** | **No** (`ProtocoloV2020.java:52-54`) | **Sí** (`ProtocoloV36.java:126-128`) | **No** (`ProtocoloV4Original.java:131-133`) | **No — candado**: *"Calibración V4.6: próxima versión"* (`ProtocoloV46.java:13,135-137`; `calibracion` es `false` fuera del simulador, `:28-31`) |
| **Modo administrador** (alta de serie `#SN`, PIN, `#Q`) | **No**: *"sin órdenes #...#"* (`ProtocoloV2020.java:48-50`) | Sí | **No** (`ProtocoloV4Original.java:126-128`) | **Sí desde rc5**, y el botón lo dice: *"(alta de serie #SN; la calibración, no)"* (`ConexionActivity.java:157-163`) |
| **Batería en pantalla** | Sí, con el byte `9` | Sí, con el byte `9` | **No** (`ProtocoloV4Original.java:111-113`) | Sí, con `#GB#`, **en %** (`ProtocoloV46.java:115-117`; `firmwares.csv` `bateria_unidad=pct`) |
| **Temperatura anotada en el diario** | **No**, y dice por qué: *"no tiene órdenes #...#"* (`ProtocoloV2020.java:42-45`) | **No**: `#GT#`/`#ST`/`#FT` son **coeficientes**, no el sensor (`ProtocoloV36.java:100-103`) | **No** (`ProtocoloV4Original.java:105-108`) | **Sí, y sólo ella**, con `#T#` (`ProtocoloV46.java:93-97`) |
| **Serie leída del equipo** | No | Sólo la 3.6.2 (`Sesion.java:270-275`) | No | Sí, si está grabada (`#GN#`) |
| **Acta de calibración** | No | Sí | No | No, mientras dure el candado |

**Lo que esta tabla corrige de la lectura rápida:** con la V4.6 el operador **puede** medir el banco
y **no puede** calibrar, y eso no es un fallo ni un equipo mal detectado. `CAMINO:§3` lo midió contra
el equipo real: las pruebas 3-6 salen `NO_APLICA` y **aun así el veredicto es APTO**.

### 1.3 La regla de redacción que falta — PROPUESTO

**RF-APP-U28 — Ningún botón deshabilitado sin motivo, y el motivo en lengua de operador.
[PROPUESTO]** Hoy ya se cumple en los cinco botones de la pantalla de conexión
(`ConexionActivity.java:145,147,161-164`), porque cada `Protocolo` trae su `motivoNoCalibra()` y su
`motivoNoBanco()`. Lo que falta es **escribirlo como regla**: toda puerta nueva que dependa de la
familia expone su motivo por el `Protocolo`, nunca con un `if` sobre la versión, y el texto nombra
**lo que el operador ve** ("sólo medir y verificar"), no la clase.

CA: con un V4 original conectado, ningún botón queda gris y mudo. Prueba: recorrer la pantalla de
conexión con los cuatro protocolos y exigir que `isEnabled()==false` implique texto con paréntesis.

---

## 2. El ZIP de calibración por familia

### 2.1 Lo que hay hoy: dos ZIP, y el que se descarga solo es el que no vale — EXISTE HOY

Los dos salen de la misma lista de piezas, `Campanas.piezas(Campana, boolean completo)`
(`RTV10:Campanas.java:564-593`). Las líneas son **idénticas en rc3 y rc5**.

| | **ZIP ligero** | **ZIP de soporte** |
| :--- | :--- | :--- |
| Método | `Campanas.exportar` (`:607-650`) | `Campanas.exportarSoporte` (`:656-689`) |
| Nombre | `campana_<serie>_<sello>.zip` (`:614`) | `soporte_<serie>_<sello>.zip` (`:663`) |
| `completo` | `false` (`:628`) | `true` (`:664`) |
| `diario_campana_….csv` | sí, **sólo el tramo nuevo** | sí, entero |
| `actas/`, `pruebas.txt` | sí (`:566-578`) | sí |
| **`campana.csv`** | **NO** | **SÍ** (`:580`) |
| **`tramas/`** | **NO** | **SÍ** (`:588`) |
| Índices | `indice.sha256` (`:630`) | `indices/…` (`:670`) |
| `resumen.txt` | corto (`:631-633`) | largo (`:673-675`) |
| Copia en `Download/RTV/` | sólo vía `exportarConHuellas` (`:545`) | siempre (`:683`) |

**`campana.csv` es el único fichero del que salen coeficientes**: `Campana.exportarCsv`
(`RTV10:Campana.java:1052-1068`), una fila por disparo, con la cabecera de `cabeceraCsv()`
(`:1047-1049`).

**Cómo se distingue de un vistazo, hoy:** por el **prefijo del nombre**. `soporte_` calibra;
`campana_` no. Nada más del fichero lo dice. Esta noche se confundieron, y el procedimiento
(`V4.6:05_Documentacion/PROCEDIMIENTO-Calibrar-un-V4.6.md:197,255`) cita `campana_…zip`, que es justo
el que la herramienta rechaza.

**Está medido, no deducido** (`CAMINO:§6.1`): con un ZIP armado con las piezas exactas del ligero,

```
python calibrar.py --zip campana_FAKE_20260919_200000.zip --serie SLV-003-2026 --solo-calculo
RECHAZADO: el ZIP no lleva campana.csv (...)
```

**Por qué engaña el nombre:** el ZIP con el que se validó la herramienta,
`V3.6:06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip`, **sí lleva `campana.csv`**,
porque es de una app anterior al reparto en ligero + soporte. Entonces `campana_` era el completo.

### 2.2 Qué columnas exige la herramienta, y qué escribe la app

`TOOL:rtv/campana.py:75-77` aborta si no hay `campana.csv`. `:83-89` exige **once columnas**:

`patron, valor_certificado, tipo_lamina, color, x, descartado, elegida, colocacion, serie, mac, firmware`

`Campana.cabeceraCsv()` (`RTV10:Campana.java:1047-1049`) escribe **veintidós**, y **las once están**:

```
serie_id,fecha_hora,serie,mac,firmware,patron,patron_original,valor_certificado,tipo_lamina,color,
orientacion,codigo,disparo,respuesta_bruta,x,descartado,motivo_descarte,veredicto,nota,aceptada,
elegida,colocacion
```

De esas once, la herramienta sólo **filtra** por tres: `elegida == 1`, `descartado == 0` y `x` no
vacía (`TOOL:rtv/campana.py:120-129`). **No mira `aceptada` ni `veredicto`**, aunque el comentario de
cabecera del módulo los nombra (`:17`). No es un fallo — `Campana.exportarCsv:1064` escribe
`elegida` sólo para la serie elegida, y `aceptada` ya está implícita — pero conviene saberlo antes de
tocar el formato: **quitar `elegida` rompe la herramienta; quitar `aceptada`, no.**

**El CSV alternativo no sirve, y por columnas.** `medidas_<serie>.csv` de "Compartir CSV y registro"
tiene catorce columnas (`RTV10:Medida.java:43-45`) y **le faltan tres de las once**: `descartado`,
`elegida` y `colocacion`.

### 2.3 Por qué el ZIP tiene que ser distinto por familia — EXISTE HOY, sin escribir

No es una preferencia de formato: es que **cada familia produce datos distintos**, y las columnas que
para una son obligatorias, para otra no pueden existir.

| Dato | V3 2020 | V3.6 | V4 original | V4.6 |
| :--- | :--- | :--- | :--- | :--- |
| `x` | **calculada por la app** (inversión del código 6) | leída con `e` | **no existe** | leída con `#X,k#` |
| Escala de esa `x` | ADC + 200 | ADC + 200 | — | cuenta de ADC por F(T) |
| `to` / `tc` / `t_estado` | vacías, con motivo | vacías, con motivo | vacías, con motivo | **con valor** (`#T#`) |
| `codigo` de la fila | el byte de la cola | el byte de la cola | — | la clave `k` de `#X,k#` |
| Banco disponible | completo `P1-P132` | completo `P1-P132` | **ninguno** | representativo V4.6 |
| ¿Produce `.hex`? | no (no calibra) | sí, familia `v36` | **no** | sí, familia `v46` |

Las escalas no son intercambiables y la herramienta ya lo dice por escrito: *"La x de un V3.6 es
ADC + 200 y la de un V4.6 es la cuenta de ADC por F(T): los ajustes no son intercambiables"*
(`TOOL:calibrar.py:425-427`).

De ahí la consecuencia que hay que escribir:

- Un **V4 original no genera ZIP de calibración en absoluto**. Genera, como mucho, un ZIP de
  **verificación**: sin `x`, sin banco y sin acta. Que hoy salga un `campana_…zip` de una campaña de
  un V4 original no lo convierte en un ZIP de calibración, y la herramienta lo rechazará por `x`
  vacía aunque lleve `campana.csv`.
- Un **V3.6** genera ZIP con `x`, sin temperatura. La herramienta lo acepta **sólo con
  `--solo-calculo`**: `--familia v36` no produce `.hex` (`TOOL:calibrar.py:432-434`).
- Una **V4.6** genera ZIP con `x` y con temperatura. Es la única que puede producir `.hex`
  (`TOOL:calibrar.py:383` sobre `fabrica.FAMILIAS`, `TOOL:rtv/fabrica.py:72`).

### 2.4 El agujero: la familia se declara FUERA del ZIP — EXISTE HOY

Hoy la familia se declara **en la línea de órdenes**, con `--familia v46` (por omisión) o `v36`
(`TOOL:calibrar.py:383`). Lo único que el ZIP aporta es la columna `firmware`, que es **texto libre**
escrito por `Sesion.firmware()` (`RTV10:Sesion.java:246-262`), del tipo
`"V4.6 2026-09-19c DEF mascara 0000"`.

Y la comprobación cruzada es esta línea:

```python
TOOL:calibrar.py:423    es_v46 = bool(re.search(r'V?4\.6', fw))
```

**Una expresión regular sobre una cadena de presentación.** Basta que alguien cambie el texto de
`Sesion.firmware()` —una cadena pensada para cabeceras, no para máquinas— para que la guarda deje de
disparar o dispare de más. Y con `--solo-calculo` **ni siquiera bloquea**: sólo escribe un aviso por
`stderr` (`:431`).

**Comprobado dos veces que no hay manifiesto en el ZIP:** (1) `grep` de `manifiesto`, `.json` y
`meta.` sobre `RTV10:Campanas.java` y `RTV10:PaquetesZip.java` no devuelve nada; (2) la lista de
piezas está enumerada entera en `Campanas.java:564-593` más `:630,633` (ligero) y `:670,675`
(soporte), y son todas: `diario_…`, `actas/`, `pruebas.txt`, `campana.csv`, `tramas/`,
`indice.sha256` / `indices/` y `resumen.txt`. **Ninguna es legible por máquina salvo los dos CSV.**

### 2.5 Cómo se declara la familia dentro del ZIP — PROPUESTO

**RF-APP-U29 — El ZIP declara su familia, y la declara donde una máquina la lea. [PROPUESTO]**

Tres piezas, en orden de coste creciente. **La primera sola ya cierra el encargo**; las otras dos son
para cuando haya que discutirlas.

**(a) Evento `FAMILIA` en el diario — el camino barato.** El diario ya lleva eventos con este papel:
`COLA,tipo` guarda el tipo de banco de la campaña antes del primer paso
(`RTV10:Campana.java:47,595`) y **viaja en los dos ZIP** (el diario está en `piezas()` siempre,
`Campanas.java:566`). Se añade un evento gemelo, escrito al abrir la campaña:

```
FAMILIA,<F2020|F36|V4_ORIGINAL|F46>,<x_min>,<x_max>,<escala_x>
```

donde `escala_x` es el identificador de escala, no prosa: `ADC+200` o `ADC/F(T)`. El valor sale de
`Protocolo.firmware().name()` y de `PerfilFirmware`, no de `Sesion.firmware()`.

**(b) Columna `familia` en `campana.csv` — la que la herramienta puede exigir.** Se añade **al final**
de `cabeceraCsv()` (`Campana.java:1047-1049`), que es la regla ya establecida por RF-APP-U24 para
que un CSV anterior siga siendo legible. Una fila = un disparo = una familia, sin ambigüedad.

**(c) `TOOL:calibrar.py` deja de adivinar.** `--familia` pasa de **obligatorio con omisión** a
**comprobación**: si el ZIP declara familia, la del ZIP manda; si la orden dice otra, **rechazo**,
no aviso. La regex de `:423` se retira. Si el ZIP **no** declara familia (los de hoy), la herramienta
lo dice —*"ZIP sin declaración de familia: anterior a RF-APP-U29"*— y exige `--familia` explícita,
sin valor por omisión.

**Por qué no basta con renombrar el fichero.** Se podría pedir `soporte_v46_<serie>_….zip`. Es un
parche útil para el ojo, y se propone abajo (RF-APP-U30), pero **el nombre de un fichero se cambia
al copiarlo** y no sobrevive a un reenvío por mensajería. La declaración tiene que ir dentro.

CA: un `soporte_…zip` de un V3.6 pasado con `--familia v46` se rechaza **por la declaración**, no por
la regex; y el mismo ZIP con la columna `familia` borrada se rechaza por falta de declaración, no se
acepta por omisión.

**RF-APP-U30 — El nombre del ZIP dice para qué sirve. [PROPUESTO]** Hoy el prefijo `soporte_` es la
única pista y no significa nada para quien no ha leído el código. Propuesta:

| Hoy | Propuesto | Qué es |
| :--- | :--- | :--- |
| `campana_<serie>_<sello>.zip` | `parcial_<serie>_<sello>.zip` | envío incremental; **no calibra** |
| `soporte_<serie>_<sello>.zip` | `calibracion_<familia>_<serie>_<sello>.zip` | el completo; **es el que calibra** |

Y el botón "Exportar (ZIP ligero)" (`RTV10:CampanaActivity.java:111`) pasa a decir **"Enviar avance
(no calibra)"**, y "ZIP de soporte" (`:112`) a **"ZIP de calibración (el bueno)"**.

**Contradicción que esto abre:** cambiar el prefijo rompe a quien ya tenga ZIP en `Download/RTV/` y
rompe el `PROCEDIMIENTO`. Si se adopta, la app debe **seguir aceptando** los dos prefijos al importar
—cosa que hoy hace, porque el importador no mira el nombre sino el contenido
(`RTV10:CampanaActivity.java:754-767`)— y el `PROCEDIMIENTO` se corrige en el mismo movimiento.

**RF-APP-U31 — El ZIP automático del banco deja de ser el inútil. [PROPUESTO]** Hoy el paso
`EXPORTAR` de la cola y el fin del banco sacan **sólo el ligero** (`CAMINO:§2`, contra rc3). El
operador que acaba el banco y cierra la app se queda con el ZIP que no calibra, en `Download/RTV/`,
con nombre `campana_…`, y sin ningún aviso. Propuesta: al **terminar** el banco (no en cada paso
`EXPORTAR`, que es incremental a propósito) se saca **también** el de soporte, como ya hace
"Guardar / Compartir" (`RTV10:ConexionActivity.java:183-184`).

### 2.6 El ZIP por familia, resumido — PROPUESTO sobre lo que EXISTE

| Familia | ¿Qué ZIP saca? | ¿Calibra? | Columnas obligatorias además de las once | Columnas que van **vacías, con motivo** |
| :--- | :--- | :--- | :--- | :--- |
| **V3 2020** | soporte, con `x` calculada | **No** (no hay `#S`) | `codigo`, `colocacion` | `to`, `tc`, `t_estado` |
| **V3.6** | soporte, con `x` de `e` | Sí, `--familia v36 --solo-calculo` | `codigo`, `colocacion` | `to`, `tc`, `t_estado` |
| **V4 original** | **ZIP de verificación**, sin `x` | **No** | — (no entra en la herramienta) | `x`, `to`, `tc`, `t_estado` |
| **V4.6** | soporte, con `x` de `#X,k#` | **Sí, cuando se abra el candado**; `--familia v46` | `codigo` (= clave `k`), `colocacion`, `to`, `t_estado` | `tc` (vale siempre 0 en el candidato, R-U12) |

La regla de las vacías no es nueva: `SPEC-U` RF-APP-U23 ya la fija —*"Sólo la V4.6 da temperatura;
las demás, vacío con motivo"*— y está implementada (`ProtocoloV36.java:100-103`,
`ProtocoloV2020.java:42-45`, `ProtocoloV4Original.java:105-108`). **Nunca 0, nunca la columna
omitida.**

---

## 3. Importar un ZIP en otra campaña

### 3.1 Lo que hoy pasa de verdad — EXISTE HOY

Aquí es donde se fue la hora de esta noche, y el diagnóstico corriente —"la app no comprueba nada"—
**es falso**. La app comprueba, y bastante. Lo que hace es **elegir mal qué bloquea y qué avisa**.

Al importar (`RTV10:CampanaActivity.java:726-796` → `ImportadorCampana.importarDiario`,
`:84`), en este orden:

| # | Comprobación | Línea | Qué hace hoy |
| :-- | :--- | :--- | :--- |
| 1 | **¿Es el ZIP ligero?** | `CampanaActivity.java:754-757` | **Bloquea** y dice *"Importe el ZIP de soporte (soporte_…zip)"* |
| 2 | **¿Es de este equipo?** (serie y MAC) | `ImportadorCampana.java:93-95` | **Bloquea todo**: *"la campaña es de otro equipo (…); no se importa nada"* |
| 3 | **¿Los patrones están en el catálogo?** | `:100-104` | **Bloquea todo** |
| 4 | **¿Los pasos apuntan a series que existen?** | `:109-113` | **Bloquea todo** |
| 5 | **¿El banco es el mismo?** | `:115-135` | **NO bloquea**: trae las series, descarta los **pasos** y avisa |
| 6 | **¿El firmware es el mismo?** | `:246-257` | **NO bloquea**: sólo añade un aviso a la lista |

El paso 5 está bien pensado y bien escrito: los pasos de una cola no valen en otra porque sus órdenes
son de otra cola, pero **las series sí valen**, y lo ya medido se recupera después con
`aplicarBancoPrevio` (`CampanaActivity.java:777`). Ésa es la respuesta correcta al conflicto de colas
y **no hay que cambiarla**.

### 3.2 El agujero de verdad no es la cola: es el firmware — EXISTE HOY

El paso 6 **sólo avisa**:

```java
ImportadorCampana.java:249-252
    for (String f : importados) {
        if (!actuales.isEmpty() && !actuales.contains(f)) {
            r.avisos.add("firmware distinto en lo importado (" + f + ") frente a " + actuales);
```

Ese aviso se acumula en `r.avisos` y se imprime junto a todo lo demás en un `TextView`
(`CampanaActivity.java:792`). **Nada se detiene.**

Y hay un caso real, no hipotético, donde eso mezcla escalas incompatibles: **el SLV-003-2026**. Se le
grabó la V4.6 esta noche. **La MAC no cambió** por la regrabación (es del módulo, no del PIC) y la
serie es la misma. De modo que un ZIP de sus medidas **de antes de grabar** —cuando era un V4
original— pasa la comprobación 2 sin objeción, pasa la 5 con un aviso de cola, y entra en la campaña
de la V4.6 con un aviso de firmware que nadie lee. Las `x` de las dos épocas **no están en la misma
escala**, y la herramienta, que sí sabe rechazar la mezcla (`TOOL:calibrar.py:424-430`), ya no puede:
recibe un único `campana.csv` y su guarda mira `camp.firmwares`, que es un conjunto y no un criterio
de rechazo.

**Esto es más grave que el conflicto de colas**, que sólo cuesta repetir pasos.

### 3.3 Comportamiento correcto — PROPUESTO

**RF-APP-U32 — Un ZIP de otra familia no entra, aunque sea del mismo equipo. [PROPUESTO]**
La comprobación de familia sube al nivel de la de equipo (paso 2): **bloquea, y no importa nada**.

```
La campaña abierta es de un equipo V4.6 y este ZIP trae medidas de un V4 original
(SLV-003-2026, misma MAC). La lectura x no está en la misma escala. No se ha
importado nada.
Si quiere conservar esas medidas, abra una campaña aparte para la etapa anterior.
```

Se compara la **familia declarada** (RF-APP-U29), no la cadena `firmware`, para que un cambio de
fecha de compilación o de máscara —que sí cambia `Sesion.firmware()`— no dispare un falso rechazo.

**RF-APP-U33 — Dentro de la misma familia, el firmware distinto avisa y se anota. [PROPUESTO]**
Un `V4.6 2026-09-19c` y un `V4.6 2026-09-20a` son la misma familia: la `x` es comparable. Se importa,
el aviso de `:251` se mantiene **y además se escribe en el diario** como evento, para que salga en
`resumen.txt` y en el acta. Hoy el aviso sólo vive en un `TextView` que desaparece al girar el
teléfono.

**RF-APP-U34 — El conflicto de colas se queda como está, y se explica mejor. [PROPUESTO]**
La lógica de `:115-135` no se toca. Lo que cambia es el texto: hoy dice *"se traen las series, no los
pasos; lo ya medido cuenta al abrir el Banco"* (`:133-134`), que es exacto y **no se entiende sin
saber qué es un paso**. Propuesta:

```
Este ZIP es de un banco REPRESENTATIVO_V46 y la campaña abierta es de un banco
COMPLETO. Las medidas entran todas (37 series); el ORDEN de la cola no, porque
es de otro banco. Al abrir "Tomar muestras" verá los pasos ya cubiertos marcados
como hechos.
```

**RF-APP-U35 — Una campaña sin MAC no recibe nada. [PROPUESTO, ya abierto como R-U06]**
`SPEC-U` R-U06 lo tiene anotado: `Campana.esDeEsteEquipo()` devuelve `true` con la MAC vacía. Con la
comprobación 2 apoyada en serie **y** MAC (`ImportadorCampana.java:93`), una campaña sin MAC acepta
medidas de cualquier equipo. Se cierra aquí porque el camino del ZIP lo atraviesa: **campaña sin MAC
= importación rechazada**, con el motivo escrito.

---

## 4. La identidad del equipo

### 4.1 Las cuatro fuentes y el orden que hoy manda — EXISTE HOY

`Sesion.serie()` (`RTV10:Sesion.java:171-186`), en este orden y sin excepciones:

| Orden | Fuente | Línea | Cuándo gana |
| :-- | :--- | :--- | :--- |
| 1 | **`#GN#` del equipo** | `:174-176` | Sólo con V4.6 (`serieDeV46()`, `:153-156`) **y** si no es `NONE` |
| 2 | **Tecleada por el operador** | `:177-179` | Si hay algo tecleado |
| 3 | **`equipos.csv`** (`serie_declarada` por MAC) | `:180-182` | Si hay fila firmada para esa MAC |
| 4 | **Nombre Bluetooth**, lo que sigue al último `_` | `:183-185` | Siempre que el nombre tenga un `_` |

`serieConocida()` (`:159-169`) usa el mismo orden para decidir si hay serie o hay que pedirla.

**Ojo con la fuente 1:** sólo vale para la V4.6. Un **V3.6.2** también tiene `#GN#` y lo lee
(`Sesion.java:270-275` lo reconoce como equipo "con serie y fecha en EEPROM"), pero `serieDeV46()`
(`:153-156`) devuelve `true` **únicamente** para `F46`, así que en un V3.6.2 la serie de `serie()`
sale de la fuente 2, 3 o 4 aunque el equipo la tenga grabada. Es una asimetría que conviene mirar
cuando se implemente RF-APP-U36.

**Los tres casos de esta noche, contra ese orden:**

| Equipo | Qué pasa | Por qué |
| :--- | :--- | :--- |
| **SLV-003-2026**, recién grabado | Las **cuatro** fuentes fallan | `#GN,NONE#` (fuente 1 descartada por `:174`); `equipos.csv` **no tiene ni una fila de datos**, sólo cabecera y comentarios (`ASSET:equipos.csv`, verificado abriendo el asset); y el módulo Bluetooth de repuesto se anuncia **`HC-06`**, sin `_` (`Sesion.java:193-196`, que documenta el caso). Queda la 2: **teclearla** |
| **SLV-028** | Serie **declarada**, de las fuentes 2 o 4 | Es un V4 original: no tiene `#GN#` en absoluto (`ProtocoloV4Original.java:126-128`) |
| **SLV-002** | Serie de la fuente 4, `COVIANDINA_SLV-002` | V3.6; y aunque fuera 3.6.2, la fuente 1 no se le aplica (ver la asimetría de arriba) |

**Lo que rc5 arregló, y conviene no volver a romper:** la serie tecleada **sobrevive a una
reconexión al mismo equipo** (`Sesion.java:108-117`: `reiniciar()` sólo borra `serieManual` si la MAC
cambió). En rc3 se borraba siempre, y una caída de Bluetooth a mitad de banco obligaba a teclearla de
nuevo — y la campaña se abría con otro nombre. Y la serie ya se puede teclear **donde se mide**, no
sólo en Campaña: `hayQuePedirSerie()` (`:198-200`) lo destapa y `BancoActivity.java:412` lo usa.

### 4.2 La regla de identidad — PROPUESTO, y contradice a `SPEC-U`

**RF-APP-U36 — Cuál manda, y cuándo. [PROPUESTO]**

1. **Si el equipo la tiene grabada y la dice, manda el equipo.** `#GN#` distinto de `NONE`, en V4.6 y
   en V3.6.2 (hoy sólo en V4.6; ver la asimetría de §4.1). Ninguna otra fuente la pisa, ni la
   tecleada. Si el operador teclea otra, la app **no la acepta y dice por qué**: la serie del acta
   tiene que ser la del equipo.
2. **Si el equipo no la tiene** (`NONE`, o firmware sin `#GN#`), la serie es **declarada**, y en este
   orden: tecleada > `equipos.csv` > nombre Bluetooth. La tecleada va primera **a propósito**: el
   operador que tiene el equipo delante sabe más que un nombre de módulo que puede ser el de repuesto.
3. **El nombre Bluetooth es la fuente más débil y hay que decirlo.** Un `HC-06` no tiene serie, y un
   `COVIANDINA_SLV-002` la tiene sólo porque alguien lo nombró así. **Nunca se usa para un acta sin
   que el operador la confirme.**
4. **La MAC no es identidad del equipo: es del módulo.** Ya está en `SPEC-U:349-351`. Si la MAC cambia
   con la misma serie, se pide nota. Esta noche pasó, y es exactamente por qué la fuente 4 falló.

**RF-APP-U37 — Qué se escribe en el acta y en el ZIP cuando la serie es declarada. [PROPUESTO]**

La marca ya existe: `Deteccion.MARCA_DECLARADA = " (serie declarada, no leída del equipo)"`
(`RTV10:Deteccion.java:30`), decidida por `Deteccion.marcaSerie()` (`:36-40`).

**Y hoy no llega ni al ZIP ni al acta.** Verificado dos veces: (1) `grep -rn "marcaSerie()"` sobre
todo el paquete devuelve **dos líneas y sólo dos**, `Sesion.java:278` (la definición) y
`Sesion.java:300`, dentro de `identidad()`; (2) `identidad()` se consume en
`Base.java:421-422` (asunto y cuerpo del correo), `ConexionActivity.java:114`, `LineaBase.java:39,67`,
`MedidaActivity.java:183,300,322` y `Pruebas.java:251-252` — **registro y correo, nada más**. La
columna `serie` de `campana.csv` sale de `s.equipo` a secas (`Campana.java:1058`), y
`cabeceraResumen` (`Campanas.java:596-601`) arma `resumen.txt` con `serieConHistoria()`,
`firmware()` y `datosCalibracion()`, ninguna de las cuales incluye la marca. (`datosCalibracion()`
sí imprime el `#GN#` literal, `Sesion.java:292`, así que un `NONE` se ve en `resumen.txt`; pero eso
es el dato crudo, no la marca, y sólo aparece si el firmware administra.)

**Esto contradice a `SPEC-U:352-353`**, que afirma: *"Toda serie declarada lleva en registros, ZIP y
acta la marca"*. En registros, sí. **En ZIP y acta, no.** `SPEC-U` da RF-APP-U11 por IMPLEMENTADO
(`:790`, citando `Deteccion.java:30,36`): está implementada **la marca**, no **su propagación**.

Propuesta, en tres sitios y ninguno caro:

| Dónde | Qué se escribe |
| :--- | :--- |
| `campana.csv` | columna nueva **al final** (regla de RF-APP-U24): `serie_origen` con `GN`, `TECLEADA`, `EQUIPOS_CSV` o `NOMBRE_BT`. **Un dato, no una frase** |
| `resumen.txt` | la línea `Equipo: serie …` de `Campanas.java:598` lleva `Sesion.get().marcaSerie()` detrás |
| acta | la misma marca, y **si la serie es declarada, el acta lo dice en su primera línea**, no en un pie |

El acta de calibración sólo existe hoy donde `calibra()` es cierto, es decir en V3.6
(`ConexionActivity.java:159`), y el camino de calibración ya exige que `#GN#` case con la campaña
(`FlujoCalibracion.java:387-394`). Cuando se abra el candado de la V4.6, **ese requisito hace que una
serie declarada no pueda firmar un acta de calibración** — lo cual es correcto y hay que dejarlo
escrito antes de que alguien lo "arregle".

**RF-APP-U38 — Un equipo con `#GN,NONE#` puede medir el banco, pero su ZIP lleva la marca.
[PROPUESTO, confirma lo que rc5 ya hace]** No se bloquea el banco por falta de serie grabada: sería
dejar al SLV-003-2026 sin poder medir nada hasta que alguien le grabe la serie. Se mide, y el ZIP
sale **marcado**. Quien luego calcule coeficientes con él sabrá que la trazabilidad de la serie es
declarada, y decidirá.

---

## 5. Contradicciones abiertas, y una que dejo dicha

**C-U07 — "Con la V4.6 la serie es la de `#GN#`, nunca tecleada".** `SPEC-U:345` lo pone en la tabla
de RF-APP-U11, y remite a RF-APP-35. **El código de rc5 hace lo contrario, a propósito y con el
motivo escrito**: `Sesion.java:172-173` documenta que *"recién grabada (`#GN,NONE#`) vale la declarada
(nombre BT o tecleada), marcada 'serie declarada, no leída del equipo', para poder medir el banco
antes de dar de alta la serie"*, y `:177` la devuelve. **Gana el código**, porque es lo que permitió
medir esta noche. `SPEC-U:345` hay que corregirlo a: *"`#GN#` cuando lo hay; con `NONE`, declarada y
marcada"*. Se deja escrita la frase vieja para que no vuelva a circular.

**C-U08 — El oscuro de dos luces, heredada de `CAMINO:§6.3`.** La cola de la V4.6 mide el oscuro con
`#X,1#` y con `#X,3#`, pero las ocho series se llaman `OSCURO`, y `TOOL:rtv/campana.py:159-188` toma
**una sola** y no mira la columna `codigo`. Esta SPEC **no la cierra** y no la toca: es metrología,
fuera de alcance. Pero la nombra porque **el formato del ZIP es donde se arreglaría**: bastaría que
la serie llevara la clave en el nombre (`OSCURO-1`, `OSCURO-3`). **Se cierra midiendo**, no aquí.

**C-U09 — Prefijo del ZIP frente a lo ya repartido.** RF-APP-U30 propone renombrar
`soporte_` → `calibracion_`. Contra: hay ZIP en `Download/RTV/` y en manos del cliente con el nombre
viejo, y el `PROCEDIMIENTO` los cita. A favor: **esta noche se perdió una hora por ese nombre**. No
se elige aquí; lo decide Diego con una frase.

### Donde esta SPEC puede chocar con la del camino corto de calibración

El encargo dice que otra SPEC está en marcha con el camino *"cargar, calibrar, nombre, nota"*. **Dos
puntos de roce**, y los digo en vez de resolverlos:

1. **RF-APP-U32 mete un rechazo duro en "cargar".** Si el camino corto asume que cargar un ZIP es un
   paso sin fricción, mi guarda de familia introduce un diálogo que puede detenerlo en seco. Creo que
   debe detenerlo —una mezcla de escalas produce coeficientes que parecen buenos— pero **es una
   decisión de flujo, no mía**.
2. **RF-APP-U37 pone la marca de serie declarada en la primera línea del acta.** Si el camino corto
   pide "nombre y nota" al final y arma el acta ahí, la marca tiene que estar disponible en ese punto.
   Hoy lo está (`Sesion.marcaSerie()`), pero no se pasa.

---

## 6. Estado, requisito a requisito

| Id | Qué pide | Estado | Evidencia / dónde tocaría |
| :--- | :--- | :--- | :--- |
| RF-APP-U28 | Ningún botón gris sin motivo, en lengua de operador | **YA SE CUMPLE, sin escribir como regla** | `RTV10:ConexionActivity.java:145,147,161-164` |
| RF-APP-U29 | El ZIP declara su familia (evento `FAMILIA` + columna `familia`), y la herramienta la exige | **PROPUESTO** | `RTV10:Campana.java:595` (patrón `COLA`), `:1047-1049`; `TOOL:calibrar.py:421-434` |
| RF-APP-U30 | El nombre del ZIP dice para qué sirve | **PROPUESTO, con C-U09 abierta** | `RTV10:Campanas.java:614,663` |
| RF-APP-U31 | Al terminar el banco sale también el ZIP que calibra | **PROPUESTO** | `RTV10:ConexionActivity.java:183-184` como modelo |
| RF-APP-U32 | ZIP de otra familia: rechazo duro, aunque sea el mismo equipo | **PROPUESTO** | `RTV10:ImportadorCampana.java:246-257` (hoy sólo avisa) |
| RF-APP-U33 | Misma familia, distinto firmware: avisa y se anota en el diario | **PROPUESTO** | `RTV10:ImportadorCampana.java:249-252` |
| RF-APP-U34 | El conflicto de colas se queda igual; cambia el texto | **EXISTE, texto PROPUESTO** | `RTV10:ImportadorCampana.java:115-135` |
| RF-APP-U35 | Campaña sin MAC: importación rechazada | **PROPUESTO** (cierra R-U06) | `RTV10:ImportadorCampana.java:93` |
| RF-APP-U36 | Orden de la identidad: `#GN#` > tecleada > `equipos.csv` > nombre BT | **EXISTE EN CÓDIGO, sin escribir; contradice `SPEC-U:345`** | `RTV10:Sesion.java:171-186` |
| RF-APP-U37 | La marca "serie declarada" llega al ZIP y al acta | **NO IMPLEMENTADO** (hoy sólo al registro y al correo) | `RTV10:Sesion.java:278-280,299-300`; `Campana.java:1058`; `Campanas.java:596-601` |
| RF-APP-U38 | Con `#GN,NONE#` se mide el banco; el ZIP va marcado | **LA MITAD EXISTE** (se mide, rc5); falta la marca | `RTV10:Sesion.java:172-179,198-200`; `BancoActivity.java:412` |

### Riesgos nuevos

| Id | Riesgo | Evidencia | Mitigación |
| :--- | :--- | :--- | :--- |
| R-U15 | **Se calibra un equipo con medidas de otra familia del mismo equipo** (antes y después de grabar la V4.6: misma MAC, misma serie) | `RTV10:ImportadorCampana.java:93,249-252`; escalas en `TOOL:calibrar.py:425-427` | RF-APP-U32 |
| R-U16 | **Se calcula con el ZIP que no calibra** y se cree que falta código | `CAMINO:§6.1` (medido); `PROCEDIMIENTO-Calibrar-un-V4.6.md:197,255` | RF-APP-U30, RF-APP-U31 |
| R-U17 | **La guarda de familia de la herramienta se cae sola** el día que alguien retoque el texto de `Sesion.firmware()`, que es una cadena de presentación | `TOOL:calibrar.py:423` (`re.search(r'V?4\.6', fw)`) contra `RTV10:Sesion.java:246-262` | RF-APP-U29(c) |
| R-U18 | **Un acta con serie declarada pasa por acta con serie leída** | `RTV10:Sesion.java:300` es el único consumidor de la marca | RF-APP-U37 |
| R-U19 | Se quita `elegida` o `colocacion` de `campana.csv` al reordenar columnas y la herramienta deja de leer el ZIP | `TOOL:rtv/campana.py:83-89,120-129` | Las once columnas son **contrato**: sólo se añade, y al final (RF-APP-U24) |

---

## 7. Lo que este documento NO ha comprobado

Se dice para que nadie lo dé por cerrado:

1. **Nada en teléfono.** Ni un botón pulsado, ni un ZIP real de un V4.6 abierto.
2. **Ningún ZIP de un V4 original examinado.** Que un `campana.csv` de un V4 original salga con la
   columna `x` vacía es deducción del `daX()` en `ProtocoloV4Original.java:69-71`, no un fichero
   abierto. Hay que verlo.
3. **Si `#X,k#` responde de verdad** en un V4.6 con equipo delante (`CAMINO:§8.5`).
4. **Que el aviso de firmware distinto se vea realmente** en la pantalla de importación: está en
   `r.avisos` y se imprime, pero no se ha visto la pantalla.
5. **El nombre Bluetooth exacto** de cada uno de los tres equipos. Los de esta SPEC vienen del
   registro de campo citado en `RTV10:Sesion.java:193-196`, no de la lista de emparejados.
6. **Ninguna de las cifras de este documento sostiene una entrega.** Esto es una especificación; lo
   que se instala sale de una validación en hardware que todavía no se ha hecho.
