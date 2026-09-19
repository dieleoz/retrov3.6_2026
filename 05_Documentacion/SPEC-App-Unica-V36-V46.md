# SPEC — App única RTV para los firmwares V3.6 y V4.6 (y los V3/V4 sin grabar)

**Sin medir. Nada de esto está implementado.** Sale de leer el código de la app RTV, los fuentes de
firmware de 2020, v4.0 y V4.1, la identificación del equipo V3-2 y los borradores de la V4.6, el
19-sep-2026 por la tarde. Ninguna trama de esta especificación se ha visto en un terminal serie con un
V4 delante. El firmware V4.6 no existe todavía: lo que aquí se dice de él es lo que pide su borrador,
que otro agente está reescribiendo mientras se escribe este documento.

**Revisión de arquitectura P2 (19-sep-2026):** capa de protocolo CON CONDICIONES (A-1 a A-9; C-U01 → `BLA,2`, C-U02 → orden de esta SPEC, versionName `1.0.0`): `D:\IT\P_RetroVertical_V4.6\05_Documentacion\REVISION-Arquitectura-V46-P2.md`.

**Decisión de producto de Diego (19-sep-2026):** una sola app para los dos firmwares. Es la RTV de
`03_App_Movil/RetroV36` (3.6.13 publicada; 3.6.14 en desarrollo por otro agente, `versionName` y
`versionCode` en `app/build.gradle:14-15` del commit `00f667e`). Este documento **no toca código**.
Arquitectura: la del `ESTUDIO-Tecnologia-App-Produccion.md` §6 (`:156-190`), Android nativo con
`Enlace` y `Protocolo` como interfaces.

## Fuentes y prefijos

| Prefijo | Ruta | Nota |
| :--- | :--- | :--- |
| `RTV:` | `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` | Commit `00f667e`. Los ficheros citados sin más (`Tramas`, `Pruebas`, `Sesion`, `Cliente`, `Receptor`, `EnlaceSerie`, `ConexionActivity`, `LecturaX`) no tienen cambios sin confirmar; `Campana` y `Campanas` sí, y se citan en `HEAD` |
| `V4.1:` | `D:\IT\P_RetroVertical_V4.6\01_Firmware\base_v4.1_repositorio\` | md5 de `Serial.c` `e134f4d9…`, `Aplicacion.c` `3472ab9d…`, `Ecuaciones.c` `8b0dbd79…`, `App_Stone.c` `c7f641c6…`: idénticos a los del repositorio V5 (comprobado hoy) |
| `v4.0:` | `D:\IT\P_RetroVertical_V4.6\01_Firmware\base_v4.0_2021\` | |
| `2020:` | `01_Firmware/base_2020_d089f962/RetroVertical1.X/` | |
| `ID-V3-2` | `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md` | |
| `SPEC-V46` | `D:\IT\P_RetroVertical_V4.6\05_Documentacion\SPEC-V4.6-BORRADOR.md` | **En edición por otro agente, sin confirmar.** Se cita por identificador de requisito; las líneas son las de las 15:00 y pueden moverse |
| `PROT-V46` | `D:\IT\P_RetroVertical_V4.6\05_Documentacion\PROTOCOLO-V4.6-BORRADOR.md` | |

---

## 1. Los cinco firmwares que la app puede encontrar

| Id | Firmware | Fuente | Enlace PIC ↔ módulo BT | Protocolo de medida | Administración |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **F-2020** | V3 de 2020 (y la variante sin `e` de SLV-002) | `2020:` (la variante de SLV-002, sin fuente) | 9600 (`PROTOCOLO-V3.6.md:16`) | Un byte `1`-`8`, `a`-`d` → `::<n>` sin terminador; `9` → `:<n>:` (`2020:gui.c:328-340`, `2020:ecuacionesCalibracion.c:141-192`) | Ninguna |
| **F-36** | V3.6.x (3.6.0, 3.6.1, 3.6.2) | `01_Firmware/RetroVertical_V3.6.X/` | 9600 (`ESTUDIO` `:44`) | El de 2020 + `e` → `::<x>` | `#...#` rev. 1.1 (`PROTOCOLO-V3.6.md:36-98`) |
| **F-40** | v4.0 (placa H-IoT) | `v4.0:` | 9600 (`v4.0:mcc_generated_files/uart1.c:115,122,125`) | `@LEERV,<COLOR>,<TIPO>@` → `@LEERV,<n>@/n/r` | Ninguna |
| **F-41** | V4.1 y el firmware intermedio del **V3-2** | `V4.1:`; el del V3-2 **no tiene fuente** (`ID-V3-2` §2) | 115200 (`V4.1:mcc_generated_files/uart1.c:114,123,126`; V3-2: `ID-V3-2` §3) | El mismo que F-40, con el tipo 1 sin ecuación en el fuente V4.1 | Ninguna |
| **F-46** | V4.6 (rama B) | No existe | 115200 (`SPEC-V46` RF-FW-29) | `@LEERV` intacto (`SPEC-V46` RF-FW-34) + `#X,<k>#` en bruto | `#...#` **con el mismo contrato que la V3.6** (`SPEC-V46` RF-FW-31, RF-FW-32) |

"V4 original" en este documento = F-40 o F-41: un V4 sin grabar con la V4.6.

### 1.1 El baudio no lo elige la app

**Comprobado en el código de la app:** ninguna clase fija una velocidad. La búsqueda de `9600` y
`115200` en `app/src/main/java` no da resultados, y el enlace es un socket RFCOMM con el UUID SPP
(`RTV:EnlaceSerie.java:33`, `:157`, `:168`). La API `BluetoothSocket` de Android no tiene ningún
parámetro de velocidad: el teléfono entrega bytes al módulo por RFCOMM, y **es el módulo del equipo
quien los pasa al PIC por su UART a la velocidad con la que esté configurado** (en los HC-0x, por
órdenes AT guardadas en el propio módulo; qué módulo lleva cada equipo es una contradicción abierta,
`ESTUDIO` `:52-59`).

Consecuencias, y ninguna más:

1. La app **no puede** distinguir 9600 de 115200, ni lo necesita: distingue por la trama.
2. Si el módulo y el PIC no están a la misma velocidad, la app ve **silencio o basura**, igual que ante
   un firmware desconocido. El mensaje al operador lo dice así (RF-APP-U03), sin probar más tramas.
3. El "115200" del V4 es un dato del **equipo** (PIC y módulo), no de la app. Cambiar un equipo de
   F-40 (9600) a F-46 (115200) exige reconfigurar su módulo BT: es trabajo de banco, fuera de la app
   (`SPEC-V46` R.4 punto 4, `:150-152`).

### 1.2 Lo que hace cada firmware con las tramas de la detección

Es la tabla que justifica el orden de RF-APP-U01. "Sin fuente" = no se sabe, y se dice.

| Trama | F-2020 (fuente) | F-36 | F-40 | F-41 fuente V4.1 | F-41 del V3-2 | F-46 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `#V#` | Sólo mira el primer byte (`2020:ecuacionesCalibracion.c:141-192`, `gui.c:328`): `#` no es orden; la app anota que dispara una medida sin responder (`RTV:Pruebas.java:303-304`) | `#V,3.6,…#` | Sin `@`: se ignora (`v4.0:Serial.c:112-121`) | Sin `@`: vuelve a reposo (`V4.1:Serial.c:101-104`) | **Sin fuente** | `#V,4.6,…#` (`SPEC-V46` RF-FW-14 revisado, RF-FW-31) |
| `9` | `:<n>:` (`2020:gui.c:328-340`) | `:<n>:` | Se ignora | Vuelve a reposo | **Sin fuente** | No adopta los bytes sueltos (`SPEC-V46` RF-FW-34) |
| `6` | Mide con el código 6 → `::<n>` | Igual | Se ignora | Vuelve a reposo | **Sin fuente** | Como `9` |
| `@LEERV,BLA,1@` | Primer byte `@`: no es orden | Igual que 2020 | Mide y responde | Mide, responde **y reescribe `error`** (`V4.1:Aplicacion.c:297-298`) | **Sin fuente**; el `.hex` contiene la cadena `@LEERV,%d@/n/r` (`ID-V3-2` §2) | Mide y responde |
| `@` sin `LEERV` | Primer byte `@`: no es orden | Igual | Se ignora (`v4.0:Serial.c:112-121`) | **Bloquea el Bluetooth hasta apagar**: `ST_INIT_ANAT` no tiene salida (`V4.1:Serial.c:87-100`) y `WDTE = OFF` (`V4.1:…/device_config.c:77`) | **Sin fuente** | Corregido en firmware (`SPEC-V46` RF-FW-B02), pero la app no lo envía nunca |
| `@LEERV` sin comas | — | — | `extraerColor` recorre memoria desde un puntero nulo + 1 (`v4.0:Serial.c:116`) | Igual (`V4.1:Serial.c:95,254-259`) | **Sin fuente** | — |
| `e` | Sin orden en la variante de SLV-002; tras `e` el equipo dejó de responder (`RTV:Tramas.java:42-45`, hipótesis) | `::<x>` | Se ignora | Vuelve a reposo | **Sin fuente** | Como `9` |

Dos hechos que pesan en todo lo demás:

- **La respuesta del V4.1 lleva un `0x00` detrás de `/n/r`.** `transmitUart1` recorre `x <= strlen`
  (`V4.1:Serial.c:31-40`) y envía también el nulo. Y `/n/r` son cuatro caracteres literales
  (`V4.1:Aplicacion.c:312`), no CR/LF.
- **El tipo 1 escribe el estado oculto.** En el V4.1, toda medida por la rama de tipo 1 hace
  `error = R − 163` (`V4.1:Aplicacion.c:270-271` desde la pantalla, `:297-298` por Bluetooth), y ese
  `error` entra en la siguiente medida de otros papeles (`V4.1:Ecuaciones.c:20,28,38,48,57,66`). La
  sonda actual `@LEERV,BLA,1@` (`RTV:Tramas.java:24`) **cambia la medida siguiente** del equipo.

---

## 2. Requisitos

Cada requisito lleva su criterio de aceptación (CA) y las pruebas que lo cierran en
`TDD-V3.6.md` §7 (T-U = prueba de nivel A, S o C; AT-U = aceptación ISTQB).

### 2.1 Detección del firmware

**RF-APP-U01 — Secuencia de detección, en este orden y sólo este.** Tras cada conexión (la sesión se
reinicia al conectar, `RTV:ConexionActivity.java:84`, `RTV:Sesion.java:114-118`), y respetando las
pausas de `RTV:Cliente.java:24-28`:

| Paso | Envía | Respuesta | Conclusión | Siguiente |
| :--- | :--- | :--- | :--- | :--- |
| 0 | — | Perfil del equipo registrado por MAC (RF-APP-U02) = F-41 sin fuente | — | Salta a 4 |
| 1 | `#V#` | `#V,3.6,…#` | F-36 (y variante por `#GC#`, como hoy `RTV:Pruebas.java:259-294`) | Fin |
| 1 | ídem | `#V,4.6,…#` con 4 o 5 campos | F-46 | Fin |
| 1 | ídem | `#V,<otra>,…#` | Desconocido que habla `#`: **no se envía nada más** (`RTV:Pruebas.java:296-299`) | Fin |
| 2 | `9` | `:<n>:` | F-2020 | Fin |
| 3 | `6` | `::<n>` | F-2020 | Fin |
| 4 | Sonda V4 (RF-APP-U04) | `@LEERV,<entero>@` | V4 original | Fin |
| — | — | Nada en ninguno | Desconocido: no se opera (RF-APP-U03) | Fin |

- **Nunca** se envía `e`, un `@` sin `LEERV`, ni un byte suelto distinto de `9` y `6`, a un equipo
  sin identificar. Es la regla de hoy (`RTV:Tramas.java:43-56`, `RTV:Pruebas.java:301-304`) y no
  se relaja.
- `9` y `6` van **antes** de la sonda porque en todos los fuentes V4 son inocuos (tabla §1.2) y en
  F-2020 contestan; la sonda, en cambio, en F-41 reescribe `error`. Así un V3 de 2020 se identifica
  sin que le llegue ninguna `@`, y un V4 sólo recibe la sonda.
- El orden de `SPEC-V46` R.6 (`:258-259`: `#V#` y, si calla, la sonda) **no reconoce un F-2020**: con
  él, un V3 de 2020 acaba en "desconocido". Contradicción C-U02 (§5).

CA: el registro de tramas de cada caso de la tabla contiene exactamente las tramas de su fila y nada
más; 0 tramas `e` y 0 tramas con `@` distintas de la sonda en todos los casos. Pruebas: T-U01 a T-U06,
AT-U01 a AT-U04.

**RF-APP-U02 — Perfil por MAC para los firmwares sin fuente.** Un CSV versionado en los assets,
`equipos.csv` (mismo régimen que `decisiones.csv`: una línea por equipo, fecha, firmante y documento),
asigna a una MAC un perfil. Obligatorio para el **V3-2** (F-41 sin fuente): su perfil dice "sólo la
sonda", y la app no le envía `#V#`, `9` ni `6`, cuyo efecto en ese firmware no se conoce (§1.2). El
perfil nunca sustituye a la detección: la confirma. Si el equipo responde algo que contradice su
perfil, la app para y lo dice.

CA: con la MAC del V3-2 en `equipos.csv`, el registro contiene sólo la sonda. Con un perfil F-36 y un
equipo que responde `@LEERV`, la app no opera y muestra "el equipo no responde como su perfil".
Pruebas: T-U07, AT-U05.

**RF-APP-U03 — Desconocido y sin respuesta.** Si ninguna trama tiene respuesta válida, la app dice:
"Sin respuesta válida. Puede ser un firmware que esta app no conoce, o el módulo Bluetooth del equipo
a otra velocidad que el equipo (la app no puede verlo)". No ofrece reintentar con otras tramas ni
barrido (`SPEC-V46` RF-APP-05). CA: 0 tramas tras el paso 4. Prueba: T-U05.

**RF-APP-U04 — Sonda V4.** La sonda es **una de las 12 tramas `@LEERV` válidas, byte a byte**, y se
espera su respuesta al menos **5 s** (`SPEC-V46` RF-APP-02; el V4.1 añade un segundo de espera antes
de medir, `V4.1:Serial.c:182-188`). Hoy la espera es de 3000 ms más 180 de silencio
(`RTV:Pruebas.java:321`, `RTV:Receptor.java:14`): corta, riesgo R-U04.
La trama de la sonda **es una decisión abierta** (C-U01):

| Opción | Trama | A favor | En contra |
| :--- | :--- | :--- | :--- |
| A (hoy) | `@LEERV,BLA,1@` | Es la única trama que `ROADMAP.md:89` de la V4.6 permite con un V4 sin identificar; su respuesta sirve de indicio v4.0/V4.1 en P0 | Reescribe `error` (`V4.1:Aplicacion.c:297-298`): la siguiente medida de otros papeles depende de lo que hubiera bajo el cabezal durante la sonda |
| B | `@LEERV,BLA,2@` | No toca `error`: la rama de otros papeles sólo lo lee (`V4.1:Aplicacion.c:303-309`) | No sirve de indicio v4.0/V4.1; nadie la ha enviado a un V4 |

Decide Diego con el arquitecto. Mientras no decida, se mantiene A y la app **registra la sonda como
una medida de tipo 1** en el diario del estado oculto (RF-APP-U08).

**RF-APP-U05 — Respuesta `@LEERV` estricta.** Se acepta sólo `@LEERV,-?[0-9]{1,6}@`. Se toleran antes
cualquier texto (el saludo `***** RETROREFLECTOMETRO VERTICAL *****` y `OFFSET= …` del arranque,
`V4.1:Aplicacion.c:94-99`) y después `/n/r` literal y un `0x00` (§1.2), que **se descartan antes de la
siguiente petición**. Hoy el patrón es `@LEERV,[^@]*@` (`RTV:Tramas.java:40`): acepta el **eco** de
la propia sonda (`@LEERV,BLA,1@`) como si fuera una respuesta. Riesgo R-U05.

CA: `@LEERV,BLA,1@` recibido → no es respuesta. `@LEERV,127@/n/r\0` → 127, y el `/n/r\0` no aparece
al principio de la siguiente respuesta. `@LEERV,127,45@` → rechazada (no se lee 127,45; memoria del
V5, trampa del protocolo). Un parser que espere `\r\n` no existe en la app. Pruebas: T-U08, T-U09,
AT-U06.

### 2.2 Capa de protocolo intercambiable

**RF-APP-U06 — Interfaz `Protocolo`.** Sustituye al booleano `esV36`
(`RTV:Tramas.java:46`, `RTV:Cliente.java:118`). Cada firmware es una implementación; ninguna pantalla
pregunta "¿es V4?". Lo que ofrece cada una:

| Función de la interfaz | F-2020 | F-36 | V4 original (F-40, F-41) | F-46 |
| :--- | :--- | :--- | :--- | :--- |
| `permitida(trama)` | Bytes `1`-`8`, `a`-`d`, `9`, `6` | + `e` y `#...#` sin `@` | **Sólo las 12 `@LEERV` válidas** | Las 12 `@LEERV` y `#...#` sin `@` |
| `medir(clave)` → R | `::<n>` | `::<n>` | `@LEERV,<n>@`, **entero** (`V4.1:Aplicacion.c:312`), ≥ 0 (`V4.1:Ecuaciones.c:120-122`) | `@LEERV,<n>@` |
| `leerX(clave)` → x | Invertir el código 6 (`RTV:LecturaX.java:54`) | `e` | **No hay `x`.** Vía tipo 1 (RF-APP-U07) | `#X,<k>#` (`SPEC-V46` RF-FW-19, RF-FW-34) |
| `bateria()` | `9` | `9` | **No hay** (el V4.1 sólo atiende `LEERV`, `V4.1:Serial.c:86-105`) | Sin definir en `SPEC-V46` (pregunta abierta) |
| `version()`, `identidad()` | — | `#V#`, `#GC#`, `#GN#` | — | `#V#`, `#GC#`, `#GN#` (RF-FW-27, 28, 31) |
| `curvas()`, `escribir()`, `evaluar()` | — | `#G`, `#S`, `#E` | — | Los mismos (RF-FW-31, 33) |
| Catálogo de claves | `1`-`8`, `a`-`d` | Ídem (`RTV:Fabrica.java:16`) | 6 colores × tipo {1, 2} | `1`-`8`, `a`-`d` (RF-FW-32) |

CA: una búsqueda de `esV36` y de `Version.V4` fuera de las implementaciones de `Protocolo` da 0
resultados. `permitida()` de V4 original rechaza `#V#`, `9`, `e`, `@VERS@`, `@LEERV@`, `@LEERV,BL,1@`
y `@LEERV,BLA,3@`. Pruebas: T-U10, T-U11.

**RF-APP-U07 — Lo que la app hace con un V4 original: medir y verificar, nunca calibrar.**

- **Medir:** `@LEERV,<COLOR>,<1|2>@`. El entero se guarda como entero, sin decimales inventados.
- **Tipo 1 en F-41:** el fuente V4.1 devuelve la señal sin ecuación (`V4.1:Ecuaciones.c:77,85,91,99,106,113`),
  `VoltajeADC` = cuentas / 10 (CLAUDE.md del V5, §4), truncada a entero: resolución de 10 cuentas. La
  app la etiqueta **"señal tipo 1, cuentas/10, no es retrorreflexión"** y nunca la compara con un
  certificado. En F-40 el tipo 1 lleva ecuación (`PROT-V46` `:42`). En el V3-2 **no se sabe**: se
  etiqueta "tipo 1 sin interpretar" hasta que P0 lo mida.
- **`ROJ,1` no marca color** (`V4.1:Serial.c:140-143`): `getColorPaper()` cae en naranja
  (`V4.1:App_Stone.c:434-441`). En tipo 1 da igual para el número (todas las ramas devuelven
  `VoltajeADC`), pero la luz la decide el color (`SPEC-V46` §1.3). La app lo anota en el registro.
- **Verificar:** banco de verificación (RF-APP-U10) con los patrones que no son de tipo I, contra su
  certificado, en R entera.
- **Calibrar: no.** Las constantes están en `Ecuaciones.c` (CLAUDE.md del V5, §4) y el firmware no
  se toca (§3 del mismo). "Calibrar este equipo", Avanzado y toda trama `#` quedan deshabilitados con
  el motivo en pantalla: "Equipo V4 sin firmware V4.6: sólo medir y verificar".

CA: con un V4 original simulado, 0 tramas `#`, `e` y `9` en toda una sesión de medida y banco; el
botón "Calibrar este equipo" deshabilitado con el texto anterior. Pruebas: T-U12, T-U13, AT-U07.

**RF-APP-U08 — Diario del estado oculto (V4 original).** La app guarda, en cada medida de otros
papeles hecha con un V4 original, **la última medida de tipo 1 enviada por la app en esa conexión**
(clave y valor), o "estado previo desconocido" si no hubo ninguna desde que se conectó. Motivo: el
resultado depende del orden de los disparos (`V4.1:Aplicacion.c:27,270-271,297-298`), y una medida
de tipo 1 hecha desde la pantalla también lo cambia sin que la app lo sepa. El resumen de verificación
avisa si hay medidas con "estado previo desconocido". Cómo fijar `error` antes de verificar (por
ejemplo, una tipo 1 sobre el oscuro) es una **decisión abierta**, no se inventa aquí.

CA: la sonda A seguida de `@LEERV,BLA,2@` → la segunda medida lleva "previa: BLA,1 = <n> (sonda)".
Una medida de otros papeles como primera trama tras el perfil → "estado previo desconocido".
Prueba: T-U14.

### 2.3 Flujo idéntico para V3.6 y V4.6

**RF-APP-U09 — Campaña, banco, "Calibrar este equipo", acta, serie y fecha: un solo flujo.** Con F-36
y F-46 la app usa las mismas pantallas y las mismas reglas: RF-APP-33 a RF-APP-48 de `SPEC-V3.6.md`
(`:856-871`). Es posible porque la V4.6 adopta **el mismo contrato `#...#`, las mismas claves y la
misma forma de curva** (`SPEC-V46` RF-FW-31, RF-FW-32, RF-FW-33). Lo que cambia va en la
implementación de `Protocolo` y en datos, **no en código de pantalla**:

| Qué difiere | F-36 | F-46 | Dónde se configura |
| :--- | :--- | :--- | :--- |
| Trama de medida | Byte `k` → `::<n>` | `@LEERV,<COLOR>,<1|2>@` → `@LEERV,<n>@` | `Protocolo` |
| Lectura en bruto | `e` | `#X,<k>#` | `Protocolo` |
| Clave → color y tipo | `1`-`6` intenso, `7`, `8`, `a`-`d` opaco (`2020:ecuacionesCalibracion.c:141-188`) | `1`-`6` = color con tipo 2, `7`, `8`, `a`-`d` = color con tipo 1 (`SPEC-V46` RF-FW-32) | Catálogo por firmware (CSV) |
| Dominio de `x` y límites de `#S` | 600-4300 (`RTV:Sesion.java:207-214`) | 0-4095 y R sin techo (`SPEC-V46` RF-FW-37, L-19) | Perfil de firmware (CSV) |
| Fábrica en `DEF` | Polinomio de 2020 | **Literal de la V4.1**; `#G` devuelve un polinomio **equivalente, no bit a bit** (`SPEC-V46` RF-FW-33) | Perfil: la coherencia en `DEF` se comprueba con `#E` contra el literal, no con `#G` a 1 ulp |
| Cola del banco y tabla de métodos | `cola_banco_P1-P132.csv`, columna `codigo_equipo` (RF-APP-47) | Otra cola con su columna `codigo_equipo` | Asset por firmware, con su md5 en la lista del APK (RF-APP-33) |
| OSCURO | Uno con `e` | **Uno por fuente de luz**: la luz la elige el color (`SPEC-V46` §1.3; `ROADMAP.md` de la V4.6, fila "Comprobación de oscuro") | Cola |
| Batería antes de escribir (RF-APP-43) | `9` | Sin orden definida: **pregunta abierta a la V4.6** | Perfil; sin orden, las escrituras piden confirmación con la batería leída en la pantalla |

Que "intenso/opaco" de la V3.6 y "otros/tipo 1" del V4 sean las mismas láminas **no está verificado**
(`SPEC-V46` RF-FW-32): el catálogo del V4.6 no hereda la columna `codigo_equipo` de la V3.6, se
escribe aparte.

CA: el mismo test de interfaz del banco pasa con `ProtocoloV36` y con `ProtocoloV46` simulados sin
cambiar una línea de pantalla; `git diff` de una V4.6 añadida después sólo toca `Protocolo`, CSV y
tests. Pruebas: T-U15, T-U16, AT-U08.

**RF-APP-U10 — Banco con un V4 original: sólo de verificación.** Sin `x` no hay ajuste. El banco se
ofrece como **"Verificación y línea base"**: cola propia, sin pasos OSCURO con `e` ni A5 en `x`; A5 en
R entera de otros papeles; resultado por patrón = R frente a certificado; nunca alimenta `Ajuste`.
Sirve para la línea base "como llegó" que la V4.6 exige antes de grabar (`SPEC-V46` R.7, `:268-272`).
CA: la cola V4 no tiene ninguna fila con `codigo_equipo` = `e` ni uso AJUSTE; el ZIP resultante no
puede importarse como campaña de ajuste. Prueba: T-U17.

### 2.4 Identidad del equipo

**RF-APP-U11 — Identidad por firmware.**

| Firmware | Serie | MAC | Fecha de calibración |
| :--- | :--- | :--- | :--- |
| F-36 3.6.2 y F-46 | `#GN#`, nunca tecleada (RF-APP-35) | La del enlace | `#GC#` |
| F-36 3.6.0/3.6.1, F-2020 | **Declarada**: nombre SPP `…_<serie>` o tecleada (`RTV:Sesion.java:146-167`) | La del enlace | No está en el equipo |
| V4 original | **Declarada**, igual; o la de `equipos.csv` (RF-APP-U02) | La del enlace | No está en el equipo |

- La MAC es **del módulo Bluetooth, no del equipo**: si se cambia el módulo, cambia. Por eso la clave
  de campaña es serie + MAC (`Campanas.java:26-30` en `HEAD`) y un cambio de MAC con la misma serie
  pide una nota.
- Toda serie declarada lleva en registros, ZIP y acta la marca **"serie declarada, no leída del
  equipo"**. Toda fecha que no sale de `#GC#` lleva "fecha no leída del equipo".
- Con un V4 original no hay acta de calibración (RF-APP-U07); el informe de verificación lleva serie
  declarada, MAC, firmware "V4 original (F-40/F-41), sin identificar la versión exacta" y la
  respuesta de la sonda.

CA: con F-36 3.6.1 la serie de los registros lleva la marca; con F-46, no. Prueba: T-U18.

**RF-APP-U12 — Cambio de equipo a mitad de campaña.** Al reconectar se repite la detección
(RF-APP-U01) y se comparan MAC, serie y firmware con los de la campaña abierta. Si alguno difiere, no
se mide para esa campaña, y se ofrece abrir o retomar la del equipo nuevo. **Una campaña sin MAC no
casa con ningún equipo**: hoy `Campana.esDeEsteEquipo()` devuelve `true` si la MAC de la campaña está
vacía (`Campana.java:186-188` en `HEAD`), riesgo R-U06. CA: campaña de SLV-002 abierta, reconexión con
otro equipo → 0 medidas añadidas a la campaña de SLV-002. Pruebas: T-U19, AT-U09.

**RF-APP-U13 — V4.6 con la EEPROM en blanco.** `#V,4.6,<fecha>,DEF,0000#`, `#GN,NONE#` y `#GC,NONE#`
(`SPEC-V46` RF-FW-27, RF-FW-28). La app: (1) lo trata como F-46 en `DEF`, con la fábrica del perfil;
(2) no abre campaña hasta dar de alta la serie por el flujo de RF-APP-35 (`Campanas.java:59-61` en
`HEAD` ya se niega sin serie); (3) marca "sin fecha de calibración"; (4) la prueba de coherencia en
`DEF` usa `#E` contra el literal, no `#G` (RF-APP-U09). CA: 0 tramas `#S`, `#SC`, `#F` antes del alta
de serie; la serie de los registros es la dada de alta y releída con `#GN#`. Pruebas: T-U20, AT-U10.

### 2.5 Versionado

**RF-APP-U14 — Esquema de versión. Decisión de Diego.** Propuesta:

| Opción | `versionName` | `versionCode` | A favor | En contra |
| :--- | :--- | :--- | :--- | :--- |
| **1 (propuesta)** | `RTV 1.0.0` en la primera versión con `Protocolo` y F-46; luego `1.x.y` | `10000` y creciente (hoy `3614`, `app/build.gradle:14`) | Separa la versión de la app de la del firmware: "3.6" deja de confundirse con el firmware 3.6 | Rompe la costumbre del nombre `RTV-V3.6.x` |
| 2 | Seguir `3.6.x` | Creciente | Nada cambia | Una app "3.6" que habla con un firmware 4.6 |
| 3 | `4.6.0` | `4600` | Marca el salto | Confunde app y firmware otra vez |

En las tres: **`applicationId` no cambia** (`com.dpi.retrov36`, `app/build.gradle:11`), porque
cambiarlo instala otra app al lado y obliga a desinstalar, contra RF-APP-40; `versionCode` siempre
mayor que el último publicado, porque Android no instala encima una versión con código menor; el APK
sigue `RTV-V<versionName>-<versionCode>.apk` (RF-APP-41); el acta y el ZIP dicen "RTV 1.0.0 (sucede a
3.6.x)" en la primera entrega.

---

## 3. Matriz firmware × función

S = sí; N = no; V = sólo verificar, sin ajuste; ? = por verificar en equipo.

| Función | F-2020 | F-36 3.6.0/3.6.1 | F-36 3.6.2 | V4 original F-40 | V4 original F-41 (V4.1) | V3-2 (F-41 sin fuente) | F-46 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Detección automática | S | S | S | S (sonda) | S (sonda) | Sólo por perfil (RF-APP-U02) | S |
| Medir R | S | S | S | S (entero) | S (entero) | ? | S |
| `x` en bruto | Invertida | `e` | `e` | N | Tipo 1 = cuentas/10 | ? | `#X` |
| Batería por BT | S | S | S | N | N | ? | ? |
| Pruebas APTO/NO APTO | S | S | S | Inversión (`SPEC-V46` RF-APP-08) | Ídem | N | S |
| Banco completo | S | S | S | N | N | N | S |
| Banco de verificación | S | S | S | V | V | V (tras AT-U05) | S |
| Calibrar este equipo | N | S | S | N | N | N | S |
| Acta | N | S | S | N | N | N | S |
| Serie leída del equipo | N | N | S | N | N | N | S |
| Fecha de calibración en el equipo | N | N | S | N | N | N | S |

---

## 4. Riesgos

| Id | Riesgo | Evidencia | Mitigación |
| :--- | :--- | :--- | :--- |
| R-U01 | Una trama de efecto desconocido llega a un firmware sin fuente (V3-2, variante de SLV-002) | §1.2, columna V3-2 | RF-APP-U02: perfil por MAC; AT-U05 con terminal serie antes de dejarlo en la app |
| R-U02 | Un V4.1 queda sin Bluetooth hasta apagarlo | `V4.1:Serial.c:87-100` | Lista cerrada de `permitida()` (RF-APP-U06); test de todas las tramas que la app puede generar |
| R-U03 | La sonda cambia la medida siguiente del V4.1 | `V4.1:Aplicacion.c:297-298` | C-U01; diario RF-APP-U08 |
| R-U04 | La sonda se da por perdida antes de que el V4.1 responda, y la respuesta tardía se cuela en la trama siguiente | `RTV:Pruebas.java:321` (3000 ms), `V4.1:Serial.c:182-188` | 5 s (RF-APP-U04) y vaciado del búfer antes de cada petición |
| R-U05 | El eco de la sonda se toma por respuesta V4 | `RTV:Tramas.java:40` | RF-APP-U05 |
| R-U06 | Una campaña sin MAC recibe medidas de cualquier equipo | `Campana.java:186-188` en `HEAD` | RF-APP-U12 |
| R-U07 | El tipo 1 de un V4.1 se lee como retrorreflexión | `V4.1:Ecuaciones.c:77-113` | Etiqueta de RF-APP-U07 |
| R-U08 | Módulo BT a otra velocidad que el PIC: parece "firmware desconocido" | §1.1 | Mensaje de RF-APP-U03; se resuelve en banco |
| R-U09 | Se asume que "intenso/opaco" = "otros/tipo 1" y se mezclan curvas | `SPEC-V46` RF-FW-32 | Catálogo V4.6 escrito aparte (RF-APP-U09) |
| R-U10 | La SPEC de la V4.6 cambia mientras se implementa esto | `SPEC-V46` en edición | Este documento cita por RF; se coteja antes de P2 de la V4.6 |

## 5. Contradicciones abiertas

- **C-U01 — Trama de la sonda.** `@LEERV,BLA,1@` (`RTV:Tramas.java:24`; `ROADMAP.md:89` de la V4.6)
  frente a `@LEERV,BLA,2@`, que no reescribe `error` (RF-APP-U04). Decide Diego.
- **C-U02 — Orden de la detección.** `SPEC-V46` R.6 (`:258-259`) va de `#V#` a la sonda; así un
  F-2020 no se reconoce. Esta SPEC mete `9` y `6` en medio, como la app de hoy
  (`RTV:Pruebas.java:305-320`). Se cierra cuando la V4.6 adopte uno de los dos órdenes.
- **C-U03 — Formato de `#V#` de la V4.6.** `PROT-V46` `:73` (`#V,4.6,…#`, 4 campos) y el RF-FW-14
  original (`4.6A`/`4.6B`) frente al RF-FW-14 revisado (`#V,4.6,…#` con máscara, formato de la V3.6).
  La app acepta sólo `4.6` exacto y trata cualquier otra cadena como desconocida.
- **C-U04 — Encargo frente a fuente, sobre "V4 original a 115200".** Vale para F-41; la v4.0 está a
  9600 (`v4.0:mcc_generated_files/uart1.c:115,122,125`). Para la app no cambia nada (§1.1).
