# SPEC — App única RTV para los firmwares V3.6 y V4.6 (y los V3/V4 sin grabar)

**Sin medir. Nada de lo que hay aquí se ha probado en un teléfono ni contra un equipo físico.** Lo
que este documento marca como IMPLEMENTADO lo está *en el código de la rama `rtv-1.0`*, y se ha
ejecutado, como mucho, en la JVM contra `EquipoSimulado` / `EquipoSimuladoV46`: eso no es una medida.
Ninguna trama de esta especificación se ha visto en un terminal serie con un V4 ni con una V4.6
delante, y ninguna APK de la RTV 1.0 se ha instalado en un teléfono para escribir esto.

Sale de leer el código de la app RTV, los fuentes de firmware de 2020, v4.0 y V4.1, el **candidato**
del firmware V4.6, la identificación del equipo V3-2 y los borradores de la V4.6. El firmware V4.6
**no está grabado en ningún equipo**.

**Estado de la especificación (19-sep-2026, noche).** El documento nació describiendo lo que había que
hacer; buena parte ya está hecha en `rtv-1.0` (rc2 publicada en `d0aba63`, rc3 en el árbol de trabajo,
sin confirmar). Desde hoy cada requisito lleva su etiqueta —**IMPLEMENTADO**, **DECIDIDO, SIN
IMPLEMENTAR** o **PENDIENTE**— y el resumen está en §6. Lo que la rc3 implementa y aquí no estaba
escrito (capa de protocolo por familias, lista blanca por perfil, sonda de tipo 2, firma del acta y
temperatura en el registro) se especifica en §2.2 bis, §2.6 y §2.7.

**Revisión de arquitectura P2 (19-sep-2026):** capa de protocolo CON CONDICIONES (A-1 a A-9; C-U01 → `BLA,2`, C-U02 → orden de esta SPEC, versionName `1.0.0`): `D:\IT\P_RetroVertical_V4.6\05_Documentacion\REVISION-Arquitectura-V46-P2.md`.

**Decisión de producto de Diego (19-sep-2026):** una sola app para los dos firmwares. Es la RTV de
`03_App_Movil/RetroV36` (3.6.13 publicada; 3.6.14 en desarrollo por otro agente, `versionName` y
`versionCode` en `app/build.gradle:14-15` del commit `00f667e`). Este documento **no toca código**.
Arquitectura: la del `ESTUDIO-Tecnologia-App-Produccion.md` §6 (`:156-190`), Android nativo con
`Enlace` y `Protocolo` como interfaces.

## Fuentes y prefijos

| Prefijo | Ruta | Nota |
| :--- | :--- | :--- |
| `RTV:` | `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` | Commit `00f667e`, rama `main`. Es el estado **anterior** a la RTV 1.0: se conserva porque las citas de §1 y §2.1 explican de dónde viene cada regla |
| `RTV10:` | `D:\IT\wt_rtv10\03_App_Movil\RetroV36\app\src\main\java\com\dpi\retrov36\` | Rama **`rtv-1.0`**, HEAD `d0aba63` (rc2) **más los cambios de la rc3 sin confirmar** en `Acta`, `BancoActivity`, `CalibrarActivity`, `Campana`, `Cliente`, `FlujoCalibracion`, `LecturaX`, `MedidaActivity`, `Ops`, `Protocolo*`, `Tramas`. **Los números de línea de la rc3 se mueven hasta que se confirme:** por eso cada cita lleva además el símbolo (método o constante), que es lo que hay que buscar si la línea no cuadra |
| `RTV10T:` | `D:\IT\wt_rtv10\03_App_Movil\RetroV36\app\src\test\java\com\dpi\retrov36\` | Las pruebas de la misma rama |
| `V4.6:` | `D:\IT\P_RetroVertical_V4.6\01_Firmware\RetroVertical_V4.6.X\` | **Candidato** del firmware V4.6 (`4927ad55` según `firmwares.csv`). No está grabado en ningún equipo |
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

**RF-APP-U01 — Secuencia de detección, en este orden y sólo este. [IMPLEMENTADO]** Tras cada conexión (la sesión se
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

**RF-APP-U02 — Perfil por MAC para los firmwares sin fuente. [IMPLEMENTADO]** Un CSV versionado en los assets,
`equipos.csv` (mismo régimen que `decisiones.csv`: una línea por equipo, fecha, firmante y documento),
asigna a una MAC un perfil. Obligatorio para el **V3-2** (F-41 sin fuente): su perfil dice "sólo la
sonda", y la app no le envía `#V#`, `9` ni `6`, cuyo efecto en ese firmware no se conoce (§1.2). El
perfil nunca sustituye a la detección: la confirma. Si el equipo responde algo que contradice su
perfil, la app para y lo dice.

CA: con la MAC del V3-2 en `equipos.csv`, el registro contiene sólo la sonda. Con un perfil F-36 y un
equipo que responde `@LEERV`, la app no opera y muestra "el equipo no responde como su perfil".
Pruebas: T-U07, AT-U05.

**RF-APP-U03 — Desconocido y sin respuesta. [IMPLEMENTADO]** Si ninguna trama tiene respuesta válida, la app dice:
"Sin respuesta válida. Puede ser un firmware que esta app no conoce, o el módulo Bluetooth del equipo
a otra velocidad que el equipo (la app no puede verlo)". No ofrece reintentar con otras tramas ni
barrido (`SPEC-V46` RF-APP-05). CA: 0 tramas tras el paso 4. Prueba: T-U05.

**RF-APP-U04 — Sonda V4: es la de tipo 2, `@LEERV,BLA,2@`. [IMPLEMENTADO]** La sonda es **una de las
12 tramas `@LEERV` válidas, byte a byte**, y se espera su respuesta **5 s** (`SPEC-V46` RF-APP-02; el
V4.1 añade un segundo de espera antes de medir, `V4.1:Serial.c:182-188`). En la 3.6.x la espera era de
3000 ms más 180 de silencio (`RTV:Pruebas.java:321`, `RTV:Receptor.java:14`), corta: riesgo R-U04,
cerrado.

**C-U01 cerrada: gana la opción B.** La sonda es `@LEERV,BLA,2@` y no `@LEERV,BLA,1@`.

| Opción | Trama | A favor | En contra |
| :--- | :--- | :--- | :--- |
| A (3.6.x) | `@LEERV,BLA,1@` | Era la única trama que `ROADMAP.md:89` de la V4.6 permitía con un V4 sin identificar; su respuesta servía de indicio v4.0/V4.1 en P0 | **Reescribe `error`** (`V4.1:Aplicacion.c:297-298`): la siguiente medida de otros papeles depende de lo que hubiera bajo el cabezal durante la sonda |
| **B (vigente)** | **`@LEERV,BLA,2@`** | **No toca `error`**: la rama de otros papeles sólo lo lee (`V4.1:Aplicacion.c:303-309`). Detectar deja de alterar la medida siguiente | No sirve de indicio v4.0/V4.1 (para eso está el perfil de `equipos.csv`, RF-APP-U02); nadie la ha enviado a un V4 físico |

Implementación: `RTV10:Tramas.java:29` (`SONDA_V4 = "@LEERV,BLA,2@"`), espera en
`RTV10:Tramas.java:31` (`TIMEOUT_LEERV_MS = 5000`), uso en `RTV10:Deteccion.java:152` (`sonda`). La
sonda **deja la pantalla del equipo en BLANCO, otros papeles**, y eso se anota en el detalle de la
detección (`RTV10:Deteccion.java:156`).

La razón de fondo, que no se pierde: la sonda de tipo 1 **escribía** el estado oculto del V4.1, así
que el mero hecho de identificar el equipo cambiaba la medida siguiente. La de tipo 2 sólo lo lee.

**RF-APP-U05 — Respuesta `@LEERV` estricta. [IMPLEMENTADO]** Se acepta sólo `@LEERV,-?[0-9]{1,6}@`
(`RTV10:Tramas.java:264` `P_LEERV`, `RTV10:Tramas.java:35` `P_LEERV_VALOR`, lectura en
`RTV10:Tramas.java:67` `valorLeerv`). Se toleran antes
cualquier texto (el saludo `***** RETROREFLECTOMETRO VERTICAL *****` y `OFFSET= …` del arranque,
`V4.1:Aplicacion.c:94-99`) y después `/n/r` literal y un `0x00` (§1.2), que **se descartan antes de la
siguiente petición**. Hoy el patrón es `@LEERV,[^@]*@` (`RTV:Tramas.java:40`): acepta el **eco** de
la propia sonda (`@LEERV,BLA,1@`) como si fuera una respuesta. Riesgo R-U05.

CA: `@LEERV,BLA,1@` recibido → no es respuesta. `@LEERV,127@/n/r\0` → 127, y el `/n/r\0` no aparece
al principio de la siguiente respuesta. `@LEERV,127,45@` → rechazada (no se lee 127,45; memoria del
V5, trampa del protocolo). Un parser que espere `\r\n` no existe en la app. Pruebas: T-U08, T-U09,
AT-U06.

### 2.2 Capa de protocolo intercambiable

**RF-APP-U06 — Interfaz `Protocolo`. [IMPLEMENTADO]** Sustituye al booleano `esV36`
(`RTV:Tramas.java:46`, `RTV:Cliente.java:118`). Cada firmware es una implementación; ninguna pantalla
pregunta "¿es V4?". Lo que ofrece cada una:

| Función de la interfaz | F-2020 | F-36 | V4 original (F-40, F-41) | F-46 |
| :--- | :--- | :--- | :--- | :--- |
| `permitida(trama)` | Bytes `1`-`8`, `a`-`d`, `9`, `6` | + `e` y `#...#` sin `@` | **Sólo las 12 `@LEERV` válidas** | Las 12 `@LEERV` y `#...#` sin `@` |
| `medir(clave)` → R | `::<n>` | `::<n>` | `@LEERV,<n>@`, **entero** (`V4.1:Aplicacion.c:312`), ≥ 0 (`V4.1:Ecuaciones.c:120-122`) | `@LEERV,<n>@` |
| `leerX(clave)` → x | Invertir el código 6 (`RTV:LecturaX.java:54`) | `e` | **No hay `x`.** Vía tipo 1 (RF-APP-U07) | `#X,<k>#` (`SPEC-V46` RF-FW-19, RF-FW-34) |
| `tramaTemperatura()` | **No hay** (sin `#`) | **No hay**: `#GT`/`#ST`/`#FT` son los COEFICIENTES, no el sensor | **No hay** (sólo `@LEERV`) | **`#T#`** (§2.7) |
| `bateria()` | `9` | `9` | **No hay** (el V4.1 sólo atiende `LEERV`, `V4.1:Serial.c:86-105`) | **`#GB#`**, en % (cerrada, ver abajo) |
| `version()`, `identidad()` | — | `#V#`, `#GC#`, `#GN#` | — | `#V#`, `#GC#`, `#GN#` (RF-FW-27, 28, 31) |
| `curvas()`, `escribir()`, `evaluar()` | — | `#G`, `#S`, `#E` | — | Los mismos (RF-FW-31, 33) |
| Catálogo de claves | `1`-`8`, `a`-`d` | Ídem (`RTV:Fabrica.java:16`) | 6 colores × tipo {1, 2} | `1`-`8`, `a`-`d` (RF-FW-32) |

**La batería de la V4.6 ya no es pregunta abierta.** Es `#GB#` → `#GB,<pct>#`, el mismo porcentaje que
va a la pantalla STONE: `RTV10:ProtocoloV46.java:115` (`tramaBateria`) y
`RTV10:ProtocoloV46.java:125` (`valorBateria` → `Tramas.parsearGB`), contra
`V4.6:Calibracion.c:991-995`. La unidad va en el perfil (`firmwares.csv`, columna `bateria_unidad`:
`n9` para la V3.6 y el 2020, `pct` para la V4.6), no en código de pantalla. Con eso RF-APP-43
(batería antes de escribir) funciona igual en las dos familias.

CA: una búsqueda de `esV36` y de `Version.V4` fuera de las implementaciones de `Protocolo` da 0
resultados. `permitida()` de V4 original rechaza `#V#`, `9`, `e`, `@VERS@`, `@LEERV@`, `@LEERV,BL,1@`
y `@LEERV,BLA,3@`. Pruebas: T-U10, T-U11 (`RTV10T:RtvUnicaTest.java:265`).

**RF-APP-U07 — Lo que la app hace con un V4 original: medir y verificar, nunca calibrar. [IMPLEMENTADO]**

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

**RF-APP-U08 — Diario del estado oculto (V4 original). [IMPLEMENTADO]** La app guarda, en cada medida de otros
papeles hecha con un V4 original, **la última medida de tipo 1 enviada por la app en esa conexión**
(clave y valor), o "estado previo desconocido" si no hubo ninguna desde que se conectó. Motivo: el
resultado depende del orden de los disparos (`V4.1:Aplicacion.c:27,270-271,297-298`), y una medida
de tipo 1 hecha desde la pantalla también lo cambia sin que la app lo sepa. El resumen de verificación
avisa si hay medidas con "estado previo desconocido". Cómo fijar `error` antes de verificar (por
ejemplo, una tipo 1 sobre el oscuro) es una **decisión abierta**, no se inventa aquí.

**Con la sonda de tipo 2 (RF-APP-U04) el diario no queda vacío ni sobra: cambia lo que anota.** La
sonda ya no escribe `error`, así que ya **no** se registra como una medida de tipo 1; sí se sigue
anotando que dejó la pantalla en BLANCO, otros papeles. Lo que el diario cuenta sigue siendo
necesario, porque una medida de tipo 1 disparada **desde la pantalla del equipo** cambia `error` sin
que la app se entere: por eso la primera medida de otros papeles de una conexión sigue saliendo con
"estado previo desconocido" y el resumen lo cuenta.

Implementación: `RTV10:DiarioEstadoOculto.java:29` (`anotar`), aviso en `:44` (`aviso`), llamada desde
la pantalla de medida en `RTV10:MedidaActivity.java:315`.

CA: una medida de tipo 1 seguida de otra de otros papeles → la segunda lleva "previa: BLA,1 = <n>".
Una medida de otros papeles como primera trama tras el perfil → "estado previo desconocido".
Prueba: T-U14.

### 2.2 bis Lo que la rama `rtv-1.0` ya implementa de la capa de protocolo

Esto no es nuevo trabajo: es lo que ya existe en el código y **no estaba escrito aquí**. Se escribe
para que la siguiente persona sepa qué hay antes de proponer cambiarlo.

**RF-APP-U15 — Detección en una sola clase, sin estado de pantalla. [IMPLEMENTADO]**
`RTV10:Deteccion.java` es Java puro y habla con el equipo por un `Canal`: no toca Android, y por eso
la detección entera se prueba en la JVM. Devuelve un `Resultado` (`:46`) con el `Protocolo` con el que
operar, el firmware, la variante de `#GC#`, la serie de `#GN#`, la huella y el detalle trama a trama.
**`protocolo == null` significa "no se opera"** (`:62`, `opera()`), y es la única puerta: ninguna
pantalla decide por su cuenta.

- Orden vigente, y sólo éste: `#V#` → `9` → `6` → sonda (`RTV10:Deteccion.java:102,122,131,140`). Es
  el de RF-APP-U01, no el de `SPEC-V46` R.6 (C-U02).
- Con `#V,3.6,…#` o `#V,4.6,…#` se leen además `#GC#` y, si la variante tiene serie, `#GN#`
  (`:162`, `identidad`).
- Cualquier otra versión en `#V#` es **desconocida y no se envía nada más** (`:117-120`), que es
  C-U03 aplicada.
- Perfil `SOLO_SONDA` de `equipos.csv`: sólo la sonda, sin `#V#`, `9` ni `6` (`:83-94`). Se puede
  forzar la detección completa tras regrabar el equipo (`completa`, `:73`), y el perfil **caduca solo**
  si la última detección vio una V4.6 (`:76-82`).
- Si el equipo responde algo que contradice su perfil firmado, **se anula el protocolo y no se opera**
  (`:194-197`, `confirmar`).

**RF-APP-U16 — Lista blanca por perfil, comprobada en el único punto de salida. [IMPLEMENTADO]** Es
lo que impide colgar un equipo, y por eso no vive en las pantallas sino en el cliente:
`RTV10:Cliente.java:126` rechaza con `IllegalArgumentException` toda petición que no esté en la lista
del protocolo detectado y, **antes de detectar**, en la de la detección
(`RTV10:Deteccion.java:24`, `PERMITIDAS` = `#V#`, `#GC#`, `#GN#`, `9`, `6` y la sonda, y nada más).

| Perfil | `permitida()` acepta | Dónde |
| :--- | :--- | :--- |
| Sin detectar | `#V#`, `#GC#`, `#GN#`, `9`, `6`, `@LEERV,BLA,2@` | `RTV10:Deteccion.java:24,42` |
| V4 original | **Sólo las 12 `@LEERV` válidas, byte a byte** | `RTV10:ProtocoloV4Original.java:44` → `RTV10:Tramas.java:38` (`esLeervValida`, patrón `@LEERV,(BLA\|AMA\|VER\|ROJ\|AZU\|NAR),([12])@`) |
| V3 de 2020 | Un byte: `1`-`8`, `a`-`d` o `9`. **Nunca `e`, nunca `#`** | `RTV10:ProtocoloV2020.java:26` |
| V3.6 | Un byte (`1`-`8`, `a`-`d`, `9`, `e`) o `#…#` de 3 a 96 bytes, **sin `@`** | `RTV10:ProtocoloV36.java:31` |
| V4.6 | Las 12 `@LEERV` y `#…#` sin `@` | `RTV10:ProtocoloV46.java:45` |

El motivo de que la de V4 original sea cerrada byte a byte está medido en el fuente: en el V4.1 una
`@` sin `LEERV` deja el equipo **sin Bluetooth hasta apagarlo** (`V4.1:Serial.c:87-100`, con
`WDTE = OFF`), y `@LEERV` sin comas recorre memoria desde un puntero nulo (`V4.1:Serial.c:95,254-259`).
La regla general de la app —ninguna trama con `@` salvo la sonda literal— está además en
`RTV10:Tramas.java:267` (`peticionPermitida`).

CA: una prueba que genere todas las tramas que cada pantalla puede producir no consigue sacar ninguna
fuera de la lista de su perfil; `Cliente.pedir` lanza antes de escribir en el enlace. Pruebas: T-U10,
T-U11, y la de ruptura de `RTV10T:RupturaRtv10Test.java`.

### 2.3 Flujo idéntico para V3.6 y V4.6

**RF-APP-U09 — Campaña, banco, "Calibrar este equipo", acta, serie y fecha: un solo flujo. [IMPLEMENTADO]** Con F-36
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

**RF-APP-U10 — Banco con un V4 original: sólo de verificación. [IMPLEMENTADO]** Sin `x` no hay ajuste. El banco se
ofrece como **"Verificación y línea base"**: cola propia, sin pasos OSCURO con `e` ni A5 en `x`; A5 en
R entera de otros papeles; resultado por patrón = R frente a certificado; nunca alimenta `Ajuste`.
Sirve para la línea base "como llegó" que la V4.6 exige antes de grabar (`SPEC-V46` R.7, `:268-272`).
CA: la cola V4 no tiene ninguna fila con `codigo_equipo` = `e` ni uso AJUSTE; el ZIP resultante no
puede importarse como campaña de ajuste. Prueba: T-U17.

### 2.4 Identidad del equipo

**RF-APP-U11 — Identidad por firmware. [IMPLEMENTADO]**

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

**RF-APP-U12 — Cambio de equipo a mitad de campaña. [IMPLEMENTADO]** Al reconectar se repite la detección
(RF-APP-U01) y se comparan MAC, serie y firmware con los de la campaña abierta. Si alguno difiere, no
se mide para esa campaña, y se ofrece abrir o retomar la del equipo nuevo. **Una campaña sin MAC no
casa con ningún equipo**: hoy `Campana.esDeEsteEquipo()` devuelve `true` si la MAC de la campaña está
vacía (`Campana.java:186-188` en `HEAD`), riesgo R-U06. CA: campaña de SLV-002 abierta, reconexión con
otro equipo → 0 medidas añadidas a la campaña de SLV-002. Pruebas: T-U19, AT-U09.

**RF-APP-U13 — V4.6 con la EEPROM en blanco. [IMPLEMENTADO]** `#V,4.6,<fecha>,DEF,0000#`, `#GN,NONE#` y `#GC,NONE#`
(`SPEC-V46` RF-FW-27, RF-FW-28). La app: (1) lo trata como F-46 en `DEF`, con la fábrica del perfil;
(2) no abre campaña hasta dar de alta la serie por el flujo de RF-APP-35 (`Campanas.java:59-61` en
`HEAD` ya se niega sin serie); (3) marca "sin fecha de calibración"; (4) la prueba de coherencia en
`DEF` usa `#E` contra el literal, no `#G` (RF-APP-U09). CA: 0 tramas `#S`, `#SC`, `#F` antes del alta
de serie; la serie de los registros es la dada de alta y releída con `#GN#`. Pruebas: T-U20, AT-U10.

### 2.5 Versionado

**RF-APP-U14 — Esquema de versión. Decisión de Diego. [IMPLEMENTADO: `1.0.0`, opción 1]** Propuesta:

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

### 2.6 Firma del acta

Decisión **FIRMA-ACTA** de Diego, 19-sep-2026 noche
(`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md:28`): *"si tienen pass de admin firman, ya no
lo compliques"* / *"Firmado por \" nombre\" ITVIAl SAS + fecha de ese dia"*.

Afecta a las dos salidas del flujo que hasta la rc2 eran "de Diego": **cerrar sin restaurar** un acta
con rechazo pendiente (P14-10) y **liberar** el equipo tras ese cierre (F-03).

**RF-APP-U17 — La llave es el PIN de administrador del equipo, y nada más. [IMPLEMENTADO]** La
autorización de "Cerrar sin restaurar" y de "Liberar tras el cierre" es el **PIN de administrador,
comprobado contra el equipo con `#L`**. No hay ninguna barrera adicional.

- **Se retira `FRASE-DIEGO`.** Hasta la rc2 valía también una "frase registrada" cuyo SHA-256 debía
  estar en una fila `FRASE-DIEGO` de `decisiones.csv` (`d0aba63:FlujoCalibracion.java:1598-1611`).
  **Esa fila nunca existió en el asset**: en `d0aba63` la cadena `FRASE` sólo aparece en un
  comentario de cabecera (`decisiones.csv:7`), no en ninguna línea de datos. Una barrera que el
  código promete y el dato no sostiene es peor que no tenerla, porque quien la lea creerá que
  protege algo. **No se añade esa fila**: se quita la comprobación.
- Cualquier fallo de `#L` hace que el flujo olvide el PIN y la pantalla lo vuelva a pedir
  (QA-3613-02, `RTV10:FlujoCalibracion.java:896`).

Implementación: `RTV10:FlujoCalibracion.java:1623` (`firma(clave, nombre)`), que devuelve `null` si
`ops.entrar(clave)` falla. Nota para quien lea el CSV: la cabecera de
`app/src/main/assets/decisiones.csv:7-9` explica por qué la fila no está.

CA: con un PIN que no entra, `cerrarSinRestaurar` y `liberarTrasCierre` devuelven "…lo firma quien
tenga el PIN de administrador del equipo" y **no escriben nada**; un texto cualquiera en el campo del
PIN no firma. Pruebas: `RTV10T:FlujoCalibracionTest.java:1192-1194`,
`RTV10T:RupturaFlujoTest.java:352-354`.

**RF-APP-U18 — Texto de la firma, literal, y nombre obligatorio. [IMPLEMENTADO]** La línea que el
acta cita es, **exactamente**:

```
Firmado por "<nombre>", ITVIAL SAS, <AAAA-MM-DD>
```

- `<nombre>` es lo que se teclee, recortado de espacios y pasado por `Csv.unaLinea` para que no rompa
  el CSV del acta.
- `ITVIAL SAS` es constante (`RTV10:FlujoCalibracion.java:1610`, `EMPRESA`). **No es una errata**
  aunque el encabezado del repositorio diga "DPI Ingeniería & Consultoría": es la empresa que Diego
  dictó para la firma del acta.
- La fecha es la del día, del **mismo reloj que el resto del acta**
  (`RTV10:FlujoCalibracion.java:1634`, `hoyDe`), no la del teléfono por otro camino.
- **Sin nombre no se firma.** Nombre vacío o en blanco → `FALTA_NOMBRE` ("Escriba el nombre de quien
  firma. El acta lo cita tal cual.", `RTV10:FlujoCalibracion.java:1607`) y **no se cierra ni se
  libera**. Se comprueba antes del PIN (`:1580` y `:1667`).

**La redacción vieja, junto a la nueva, para que no vuelva a circular:**

| Hasta la rc2 (`d0aba63`) | Desde la rc3 |
| :--- | :--- |
| `Diego (PIN del equipo, verificado con #L)` (`FlujoCalibracion.java:1611`) | `Firmado por "<nombre>", ITVIAL SAS, <AAAA-MM-DD>` |
| `Diego (frase registrada en decisiones.csv)` (`:1608`) | **No existe**: se retira `FRASE-DIEGO` |

CA: `cerrarSinRestaurar("1234", motivo, "  Ana Ruiz  ")` deja en el acta `Firmado por "Ana Ruiz",
ITVIAL SAS, <hoy>`; con nombre vacío devuelve `FALTA_NOMBRE` y el acta no se toca. Pruebas:
`RTV10T:FlujoCalibracionTest.java:1472-1480`.

**Defecto abierto D-U01, no corregido:** la línea que se escribe en el acta antepone otra vez
"firmado por", de modo que queda `RECHAZADA SIN RESTAURAR, firmado por Firmado por "Ana Ruiz", ITVIAL
SAS, <fecha>` (`RTV10:FlujoCalibracion.java:1597`, y lo mismo en el mensaje de `:1602` y en la línea
LIBERADA de `:1675`, que queda `liberado por Firmado por …`). **La prueba actual consagra la
duplicación** (`RTV10T:FlujoCalibracionTest.java:1200`). El texto que Diego dictó no la lleva. Se
arregla quitando el prefijo en esos tres puntos, o haciendo que `firma()` devuelva sólo
`"<nombre>", ITVIAL SAS, <fecha>`; **se decide al implementar, no aquí**.

**RF-APP-U19 — El nombre es el de quien firma en ese momento. [IMPLEMENTADO]** El nombre es
**parámetro explícito** de las dos entradas (`cerrarSinRestaurar(clave, motivo, nombre)`,
`liberarTrasCierre(clave, motivo, nombre)`) y **no un campo de la sesión ni el del operador que
calibró**. La razón está medida en el flujo:

- "Cerrar sin restaurar" puede abrirse en una sesión **en la que nadie ha calibrado**: firmar con el
  nombre de quien calibró sería una atribución falsa.
- **"Liberar" actúa sobre un acta ANTERIOR**, que pudo cerrarse **otro día y con otra persona
  delante**: la línea LIBERADA se añade al diario de esa acta ya cerrada
  (`RTV10:FlujoCalibracion.java:1675`, `almacen.anadirAUltimaCerrada`). Por eso **un acta liberada
  muestra dos nombres** —el de la conformidad de entonces y el de quien libera hoy— y **eso es lo
  correcto**: son dos actos distintos, de dos personas que pueden no ser la misma. Quien lea el acta
  tiene que poder distinguirlos.

La pantalla pide el nombre en un campo aparte del PIN, y lo dice: autorizar y firmar son dos cosas
distintas (`RTV10:CalibrarActivity.java:587-608` para cerrar, `:571-582` para liberar).

**Contradicción menor, anotada y no resuelta por escrito (C-U05).** La fila FIRMA-ACTA del documento
de decisiones dice "El nombre es el del **operador que calibra**, no un literal cableado"
(`DECISIONES-Diego-2026-09-19.md:28`), mientras que el encargo de esta SPEC y el código dicen "el de
**quien firma en ese momento**". No es lo mismo en el caso de liberar. **Manda lo implementado** —el
nombre de quien firma— porque es lo único compatible con que liberar actúe sobre un acta de otro día;
si Diego quiere lo otro, se cambia el código y esta línea.

### 2.7 La temperatura en el registro

Sin esto, las horas de banco **no sirven** para ajustar nunca la corrección térmica: es el dato que
hay que acumular ahora aunque el ajuste llegue después (§2.8).

> **Aviso de lectura.** Los ficheros de la rc3 se estaban editando mientras se escribía esta sección
> (18:00-18:15 del 19-sep-2026). Lo que sigue describe el árbol de trabajo a las **18:15**, y en esa
> franja la cadencia pasó de "por disparo" a "por serie". Si una línea no cuadra, búsquese el
> símbolo, que también se cita.

**RF-APP-U20 — Columnas de temperatura en el diario y en el ZIP. [IMPLEMENTADO]** Cada fila
`DISPARO` del diario de campaña lleva, **al final**, tres campos nuevos:

```
DISPARO,id,idx,fecha,respuesta_bruta,x[,colocacion][,to,tc,t_estado]
```

- **`to`** — temperatura **óptica**: la que entra en el término de la fórmula.
- **`tc`** — temperatura de **circuito**.
- **`t_estado`** — `OK`, `DESC` o `SIN` (RF-APP-U22).

`RTV10:Campana.java:24-28` (formato), `:68-75` (campos del `Disparo`), `:672,674,676`
(`T_ESTADO_OK`, `T_ESTADO_DESC`, `T_ESTADO_SIN`), `:688-698` (`agregarDisparo` con temperatura). El
ZIP de la campaña lleva ese mismo diario, así que las columnas viajan con él sin trabajo adicional.

Y **un evento por cada lectura de serie**, que es de donde sale el recorrido:

```
TEMP_SERIE,fecha,serie_id,APERTURA|CIERRE,to,tc,estado,motivo
```

`RTV10:Campana.java:707-712` (`anotarTemperaturaSerie`), momentos en `:700-701` (`T_APERTURA`,
`T_CIERRE`). La serie guarda además `temperaturaApertura`, `temperaturaCierre` y
`derivaTemperatura()` (`RTV10:Campana.java:110-123`).

**Vacío, nunca 0, nunca omitido.** Si no hay temperatura, las columnas van **vacías**, el estado es
`SIN`, y el motivo queda escrito **una sola vez por campaña** en un evento `TEMPERATURA`
(`RTV10:Campana.java:718`, `anotarTemperatura`; se escribe al abrir el banco,
`RTV10:BancoActivity.java:766`). Un 0 en esa columna es una cifra que alguien ajustaría mañana como
si fuese una medida; un hueco con motivo es un dato.

**RF-APP-U21 — Cadencia: por serie, no por disparo. [IMPLEMENTADO]** En el banco se lee la
temperatura **una vez al abrir la serie y otra al cerrarla**, y las columnas de sus disparos se
rellenan con **la de apertura**:

| Dónde | Cadencia | Qué queda escrito |
| :--- | :--- | :--- |
| **Banco** | `#T#` al **abrir** la serie (`RTV10:BancoActivity.java:867-868`) y al **cerrarla** (`:886`) | Un `TEMP_SERIE` por lectura, y las columnas de cada `DISPARO` con la de apertura (`:872-877`) |
| **Medida suelta de R** (`RTV10:MedidaActivity.java:307`, `temperaturaDeTanda` en `:249`) | Una por **tanda**, antes de la primera medida, con su marca de hora | Texto en cada línea del registro: `TO <n> (lectura de TANDA a las <hora>, NO de esta medida)` (`:263-264`). Esta pantalla **no alimenta el ajuste** |

**El motivo, que es lo que hay que escribir.** El canal de temperatura óptica se refresca **cada
10 s**: `TIME_TEMP_OPT = 1000` pasadas de `PERIOD_APLICACION = 10 ms` (`V4.6:Aplicacion.h:14,16`,
usados en `V4.6:Aplicacion.c:193`; los mismos valores en `v4.0:Aplicacion.h:22,33` y en
`V4.1:Aplicacion.h:14,16` — **las tres bases V4**). Y `#T#` **no mide**: devuelve el valor tal como
está en el firmware (`V4.6:Calibracion.c:983-984`). Con disparos cada ~2 s, **una lectura por disparo
devolvería el mismo número cinco veces seguidas**: no aporta dato y promete una resolución que no
existe.

Lo que el ajuste necesita **no es una temperatura por disparo, es el RECORRIDO de `TO`** (§2.8), y eso
lo da la pareja apertura/cierre de cada serie.

*(El coste no era el argumento: el comentario de `RTV10:BancoActivity.java:783-788` da 75 ms por
disparo y +1,6 % de banco para una `#T#` intercalada, medido —dice— contra `Ritmo`/`RitmoTest`. Se
recoge como lo que es, una cifra del código, no comprobada aparte en esta SPEC.)*

**Excepción, y es la buena:** si la respuesta de `#X` trae su propia `TO` (RF-FW-B13,
`#X,<k>,<x>,<TO>,<a>#`), **ésa manda** sobre la de apertura, porque sí es la del propio disparo y no
cuesta ningún viaje (`RTV10:BancoActivity.java:870-875`). **Con el candidato de hoy ese camino no se
toma**, porque manda tres campos (RF-APP-U25).

**C-U06 — abierta unas horas y cerrada el mismo día.** El diseño de la rc3 fue primero **por
disparo**, apoyado en `SPEC-V46` RF-APP-28 punto 1 ("columna `TO` por disparo, en el ZIP y en el
diario"), y pasó a **por serie** mientras se escribía esta sección. Se deja escrito el que se
descartó: por disparo, con una columna `origen_t` de valores `D` (de este disparo) y `T` (de la
tanda) en lugar de `t_estado`. **Queda una discrepancia viva con la SPEC de la V4.6**, que sigue
pidiendo `TO` por disparo; se resuelve sola cuando el firmware mande `TO` dentro de `#X`
(RF-APP-U25), que es el único modo de tener una `TO` por disparo que signifique algo. Mientras tanto
la discrepancia se anota aquí y **no se toca la SPEC de la V4.6 desde este documento**.

**RF-APP-U22 — Un sensor averiado no puede pasar por día fresco. [APP LISTA, FIRMWARE NO]** El
firmware fuerza `TO = 0` cuando la lectura cruda llega a 70 °C, que es como se manifiesta un sensor
óptico desconectado:

```c
if(top.fTempOpt >= 70.0)
{
    top.fTempOpt = 0.0;
}
```

`V4.6:Temp_Optica.c:63-66`, verificado abriendo el fichero. **Un sensor averiado se comporta
exactamente igual que un día fresco**, y un ajuste hecho con esos datos saldría plano y se leería
como "aquí no hay deriva". Por eso el estado **no es adorno**.

- **Valores del estado:** `OK` (lectura válida) y **`DESC`** (lectura cruda ≥ 70 °C, forzada a 0).
  Los define `SPEC-V46` RF-FW-B13 / `PROT-V46` `:215`, que amplían `#T#` a
  `#T,<Tcirc>,<TO>,<estado>#`.
- **Lo que la app hace ya:** si el estado es `DESC`, **la columna `to` va vacía con su motivo**, y el
  0 del centinela **no entra** en el registro (`RTV10:Tramas.java:197-222`, `parsearT`; constantes
  `T_DESC` en `:168` y `T_MOTIVO_DESC` en `:173`). La app acepta las dos formas de `#T#`, la de tres
  campos y la de cuatro (`:199-204`), y devuelve el estado `OK`/`DESC`/`SIN` que va a la columna
  (`:206,211,221`).
- **Lo que hoy NO se puede distinguir, y hay que decirlo:** el **candidato del firmware V4.6 emite
  sólo tres campos** —`responder("#T,")`, `enviarNumero(ap.fTempCircuit)`, `','`,
  `enviarNumero(opt.fTempOpt)`, `'#'`, en `V4.6:Calibracion.c:983-990`—, **sin el campo de estado**.
  Mientras eso no cambie, **un `TO = 0` y un sensor averiado son el mismo número para la app**. La
  protección existe en el parser y no se dispara nunca. Es una limitación del conjunto, no un defecto
  de la app, y se escribe en el acta como tal (`SPEC-V46` RF-APP-28 punto 4).
- **Orden de los campos: el circuito PRIMERO, la óptica SEGUNDA.** `V4.6:Calibracion.c:986`
  (`ap.fTempCircuit`) y `:988` (`opt.fTempOpt`), y `PROT-V46` `:215`. La rc3 lo tuvo invertido un
  rato; con la V4.6 real eso habría metido el 0 estructural del circuito en la columna óptica, que es
  la que se ajusta. **Un simulador simétrico (`#T,25.0,25.0#`) no lo habría visto nunca.** Prueba de
  ruptura: `RTV10T:TemperaturaRtv10Test.java:47` (t01).
- **`tc` vale siempre 0 en el candidato, y es estructural.** Se emite `ap.fTempCircuit`
  (`V4.6:Calibracion.c:986`) y el único sitio que lo asigna es `V4.6:Aplicacion.c:217`, dentro del
  estado `ST_TEMPCIRC_AP` (`:206`), **al que no se llega desde ningún `cambiarEstado()`**:
  verificado buscando `ST_TEMPCIRC_AP` en todo el candidato, que sólo aparece en su propio `case`,
  en su `inicioEstado` y en el `enum` (`V4.6:Aplicacion.h:92`). La app lo dice en el motivo en vez de
  callarlo (`RTV10:Tramas.java:180`, `T_CIRCUITO_ESTRUCTURAL`). **Con `tc` no se ajusta nada.**
  *(Nota de precisión: el comentario del código cita `Aplicacion.c:216` y `:205-216`; las líneas
  exactas del candidato son `:217` y `:206-217`.)*

CA: `#T,25.0,0,DESC#` → `to` vacía con el motivo de `DESC`, **no 0**; `#T,0,25.0#` → `tc = 0` con el
motivo estructural. Pruebas: `RTV10T:TemperaturaRtv10Test.java:62,75,88` (t02, t03, t04).

**RF-APP-U23 — Sólo la V4.6 da temperatura; las demás familias, vacío con motivo. [IMPLEMENTADO]**

| Firmware | Trama | Motivo si no hay |
| :--- | :--- | :--- |
| **F-46** | **`#T#`** (`RTV10:ProtocoloV46.java:95`) | — |
| F-36 | **Ninguna** | `#GT`, `#ST` y `#FT` leen o escriben los **coeficientes** del factor de temperatura, no el sensor (`PROTOCOLO-V3.6.md:48-50`; `#T#` aparece **0 veces** en ese documento, comprobado con `grep -c`). `RTV10:ProtocoloV36.java:100` |
| F-2020 | **Ninguna** | No habla `#…#`: no hay ni dónde pedirla (`RTV10:ProtocoloV2020.java:42`) |
| V4 original (F-40, F-41) | **Ninguna** | Sólo responde a `@LEERV`, que devuelve un entero y nada más (`RTV10:ProtocoloV4Original.java:105`) |

En esas tres familias las columnas van **vacías con su motivo, nunca a 0 y nunca omitidas**. El motivo
se escribe una vez por campaña, en el evento `TEMPERATURA`, y no se repite fila a fila.

CA: con cada protocolo, `tramaTemperatura()` y `motivoSinTemperatura()` son coherentes (o hay trama y
el motivo es `""`, o no la hay y el motivo explica por qué). Prueba:
`RTV10T:TemperaturaRtv10Test.java:157` (t07).

**RF-APP-U24 — Compatibilidad: los campos nuevos van al final. [IMPLEMENTADO]** Igual que se añadió
`colocacion` en la 3.6.7, `to`, `tc` y `t_estado` van **al final** del registro de disparo, y la
lectura los toma sólo si están (`RTV10:Campana.java:1326-1328`: `c.size() > 7`, `> 8`, `> 9`). **Un
diario escrito por la rc2 o anterior se sigue leyendo igual**, con la temperatura vacía. Una
temperatura vacía tiene que seguir vacía al releer, nunca convertirse en 0 (`RTV10:Campana.java:1262-1264`, `numONull`).

CA: un diario de la rc2 se relee sin error y con `to`/`tc` nulos; una temperatura escrita vuelve
idéntica. Pruebas: `RTV10T:TemperaturaRtv10Test.java:104,137` (t05, t06).

**RF-APP-U25 — `#X,<k>#` con la temperatura embebida. [PREVISTO, NO OBLIGATORIO TODAVÍA]** Es la vía
definitiva y **cuesta cero**, porque el dato ya está en el firmware: `#X,<k>#` pasaría a responder
`#X,<k>,<x>,<TO>,<a>#`, con `a` = ajuste aplicado en cuentas (`SPEC-V46` RF-FW-B13, `PROT-V46`
`:214`). Así `TO` es **la que entró en esa medida**, sin la carrera de pedir `#T#` después.

**El firmware de hoy no la trae:** el candidato responde tres campos
(`V4.6:Calibracion.c:774-780`, `calibracionResponderX`). **Por eso este requisito no es obligatorio y
no bloquea nada.**

La app **ya admite las dos formas a propósito** (`RTV10:Tramas.java:116`, `parsearXCompleta`: 3 o 5
campos) y usa la temperatura embebida si viene (`RTV10:BancoActivity.java:846-850`,
`LecturaX.traeTemperatura` en `RTV10:LecturaX.java:47`). Es decir: **el día que el firmware la mande,
la app no se toca**. Eso es lo que hay que conservar; convertirlo en obligatorio es decisión de la
SPEC de la V4.6, no de ésta.

### 2.8 Ajuste del término de temperatura

**RF-APP-U26 — El término de temperatura lo ajusta la app al pulsar Calibrar. [DECIDIDO, SIN
IMPLEMENTAR]** Decisión de Diego: la corrección **es un término de la fórmula**, no un procedimiento
aparte, y se ajusta **en el mismo momento y con el mismo botón que las curvas por color**: al
terminar el banco y pulsar "Calibrar", con los datos del ZIP. El modelo es

```
V_corregida = F(TO)·V + S(TO)
```

con la temperatura **óptica**, aditivo sobre `V` e **igual para todos los colores**
(`SPEC-V46` RF-FW-B10 y RF-FW-B11; `PROT-V46` `:206-207`, donde `F` ocupa el hueco multiplicativo de
`#GT`/`#ST` —fábrica `0, 0, 1`, neutra— y `S` va en `#GA,1#` y `#GA,2#`). Los coeficientes son **de
cada equipo físico**, como las curvas (L-23).

**Nada de esto está implementado en la app**: hoy no se escribe `#SA`, `#ST` ni `#GA` desde el flujo
de calibración, y el término se queda como venga de fábrica. El requisito queda escrito para que, al
implementarlo, no haya que volver a decidirlo: alcance completo en `SPEC-V46` RF-APP-28.

**RF-APP-U27 — Guarda de recorrido térmico: sin recorrido, no se ajusta. [GUARDA IMPLEMENTADA,
UMBRAL SIN MEDIR]** Si en los datos del banco **no hay recorrido suficiente de `TO`**, la app **no
ajusta**: deja los coeficientes **de fábrica** y **lo dice en el acta**, nombrando cuál de las
condiciones falló.

El motivo no es "sale con más error", es más grave: `F` y `S` entran **multiplicando y sumando la
misma `V`**. Con todas las medidas prácticamente a la misma `TO` el sistema es **degenerado**:
infinitas parejas `(F, S)` explican los mismos datos igual de bien, y una regresión devuelve una
cualquiera **con toda la apariencia de un buen ajuste**. El número que sale no significa nada. La
respuesta correcta no es ajustar con menos confianza, sino **negarse a ajustar**.

- **El umbral no se inventa aquí.** `SPEC-V46` RF-APP-28 punto 2 lo deja como **condición pendiente**
  (L-36): el criterio es "recorrido de `TO` suficiente para separar las dos mitades con los datos
  reales de deriva, y con puntos en los dos tramos de `S` si se van a ajustar los dos", y el valor lo
  fija quien esté midiendo esa deriva. **Mientras no esté fijado, la guarda actúa siempre.**
- **Implementación de la guarda, ya en el árbol:** `RTV10:AjusteTemperatura.java`. La constante
  `RECORRIDO_TO_MINIMO` vale **`Double.NaN` a propósito** (`:40`), y con `NaN` el método `evaluar`
  (`:74`) **nunca autoriza el ajuste**: devuelve la decisión con el recorrido observado, cuántas
  medidas traían temperatura y cuántas no, y el motivo para el acta (`:96-101`). El texto fijo para
  el acta es `SE_QUEDA_FABRICA` (`:65`). La clase **no ajusta nada**: sólo decide, porque esa
  decisión hay que tomarla **antes** de escribir en el equipo y no después.
- **Medidas sin temperatura: se cuentan aparte y se dicen**, no se rellenan con 0 (`:81-84`).
- **Sensor sospechoso también frena el ajuste** (`SPEC-V46` RF-APP-28 punto 4): si alguna serie trae
  `DESC`, o si `TO` sale constante e igual a 0 mientras la ambiente anotada a mano no lo es, no se
  ajusta. **Esa comprobación no está implementada** y hoy además no se puede hacer con `DESC`, por
  RF-APP-U22.

CA: con cualquier juego de temperaturas, mientras `RECORRIDO_TO_MINIMO` sea `NaN`, `evaluar` devuelve
`ajustar = false` y un motivo que dice que el umbral **no está medido**. Prueba:
`RTV10T:TemperaturaRtv10Test.java:240` (t11).

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
| R-U03 | ~~La sonda cambia la medida siguiente del V4.1~~ **CERRADO** | `V4.1:Aplicacion.c:297-298` | C-U01 cerrada con la opción B: la sonda es de tipo 2 y sólo lee `error` (`RTV10:Tramas.java:29`) |
| R-U04 | ~~La sonda se da por perdida antes de que el V4.1 responda~~ **CERRADO** | `RTV:Pruebas.java:321` (3000 ms), `V4.1:Serial.c:182-188` | 5 s (`RTV10:Tramas.java:31`) y vaciado del búfer antes de cada petición |
| R-U05 | ~~El eco de la sonda se toma por respuesta V4~~ **CERRADO** | `RTV:Tramas.java:40` | `RTV10:Tramas.java:264` (`P_LEERV`, sólo `@LEERV,<entero>@`) |
| R-U06 | Una campaña sin MAC recibe medidas de cualquier equipo | `Campana.java:186-188` en `HEAD` | RF-APP-U12 |
| R-U07 | El tipo 1 de un V4.1 se lee como retrorreflexión | `V4.1:Ecuaciones.c:77-113` | Etiqueta de RF-APP-U07 |
| R-U08 | Módulo BT a otra velocidad que el PIC: parece "firmware desconocido" | §1.1 | Mensaje de RF-APP-U03; se resuelve en banco |
| R-U09 | Se asume que "intenso/opaco" = "otros/tipo 1" y se mezclan curvas | `SPEC-V46` RF-FW-32 | Catálogo V4.6 escrito aparte (RF-APP-U09) |
| R-U10 | La SPEC de la V4.6 cambia mientras se implementa esto | `SPEC-V46` en edición | Este documento cita por RF; se coteja antes de P2 de la V4.6 |
| R-U11 | **Un sensor óptico averiado pasa por día fresco**: el candidato de la V4.6 manda `#T#` con tres campos, sin `estado`, y el 0 del centinela entra como si fuese una temperatura | `V4.6:Temp_Optica.c:63-66` + `V4.6:Calibracion.c:983-990` | La app ya trata `DESC` (`RTV10:Tramas.java:197-215`), pero **hasta que el firmware mande el cuarto campo la protección no se dispara**. No se ajusta el término (RF-APP-U27) y se dice en el acta |
| R-U12 | Alguien ajusta con `tc`, que en el candidato vale **siempre 0** por un estado inalcanzable | `V4.6:Aplicacion.c:206-217` + `V4.6:Calibracion.c:986` | Motivo escrito en la propia lectura (`RTV10:Tramas.java:180`); el término se ajusta con `TO`, nunca con `TC` (RF-APP-U26) |
| R-U13 | La columna `to` se lee como una medida independiente por disparo, cuando es la de **apertura de la serie** y el canal sólo se refresca **cada 10 s** | `V4.6:Aplicacion.h:14,16`; `#T#` no mide, `V4.6:Calibracion.c:983-984` | Cadencia por serie y `t_estado` (RF-APP-U20, U21); la vía buena es `TO` dentro de `#X` (RF-APP-U25) |
| R-U14 | La línea de firma del acta sale duplicada (`firmado por Firmado por …`) y la prueba la consagra | `RTV10:FlujoCalibracion.java:1597,1602,1675`; `RTV10T:FlujoCalibracionTest.java:1200` | Defecto D-U01 de RF-APP-U18, abierto |

## 5. Contradicciones abiertas

- **C-U01 — Trama de la sonda. CERRADA (19-sep-2026).** Era `@LEERV,BLA,1@` (`RTV:Tramas.java:24`;
  `ROADMAP.md:89` de la V4.6) frente a `@LEERV,BLA,2@`. **Gana la de tipo 2**, que no reescribe
  `error`: `RTV10:Tramas.java:29`, revisión P2 §4. Se deja escrita la que había, porque la de tipo 1
  alteraba la medida siguiente del equipo con sólo detectarlo.
- **C-U02 — Orden de la detección.** `SPEC-V46` R.6 (`:258-259`) va de `#V#` a la sonda; así un
  F-2020 no se reconoce. Esta SPEC mete `9` y `6` en medio, como la app de hoy
  (`RTV:Pruebas.java:305-320`). Se cierra cuando la V4.6 adopte uno de los dos órdenes.
- **C-U03 — Formato de `#V#` de la V4.6.** `PROT-V46` `:73` (`#V,4.6,…#`, 4 campos) y el RF-FW-14
  original (`4.6A`/`4.6B`) frente al RF-FW-14 revisado (`#V,4.6,…#` con máscara, formato de la V3.6).
  La app acepta sólo `4.6` exacto y trata cualquier otra cadena como desconocida.
- **C-U04 — Encargo frente a fuente, sobre "V4 original a 115200".** Vale para F-41; la v4.0 está a
  9600 (`v4.0:mcc_generated_files/uart1.c:115,122,125`). Para la app no cambia nada (§1.1).
- **C-U05 — De quién es el nombre que firma el acta.** "El del **operador que calibra**"
  (`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md:28`) frente a "el de **quien firma en ese
  momento**" (encargo de esta SPEC y código, `RTV10:FlujoCalibracion.java:1575,1661`). No es lo mismo
  al **liberar**, que actúa sobre un acta anterior. Vigente lo segundo (RF-APP-U19); lo cierra Diego
  con una frase.
- **C-U06 — Cadencia de la temperatura: por disparo o por serie. CERRADA el mismo día, a favor de
  POR SERIE** (`RTV10:BancoActivity.java:867-868,886`), por el refresco de 10 s del canal
  (`V4.6:Aplicacion.h:14,16`). Se deja escrita la que se descartó: por disparo, con columna
  `origen_t` (`D`/`T`). **Queda viva la discrepancia con `SPEC-V46` RF-APP-28 punto 1**, que pide
  `TO` por disparo; se cierra cuando el firmware mande `TO` dentro de `#X` (RF-APP-U25). Esta SPEC no
  toca la de la V4.6.

---

## 6. Estado de implementación

Tres etiquetas, y ninguna de ellas significa "probado en un equipo":

- **IMPLEMENTADO** — está en el código de `rtv-1.0` y se ejecuta al menos contra el simulador en la
  JVM. **No se ha probado en un teléfono ni contra un equipo físico.**
- **DECIDIDO, SIN IMPLEMENTAR** — la decisión está tomada y escrita; el código no lo hace.
- **PENDIENTE** — falta algo que no depende de la app (una cifra medida, una orden del firmware).

| Requisito | Qué | Estado | Dónde |
| :--- | :--- | :--- | :--- |
| RF-APP-U01 | Orden de la detección `#V#` → `9` → `6` → sonda | IMPLEMENTADO | `RTV10:Deteccion.java:102,122,131,140` |
| RF-APP-U02 | Perfil por MAC (`equipos.csv`) | IMPLEMENTADO | `RTV10:Deteccion.java:73-94,175-198`; asset `equipos.csv` |
| RF-APP-U03 | Desconocido y sin respuesta | IMPLEMENTADO | `RTV10:Deteccion.java:26` (`SIN_RESPUESTA`) |
| RF-APP-U04 | Sonda de **tipo 2**, espera de 5 s | IMPLEMENTADO | `RTV10:Tramas.java:29,31`; `RTV10:Deteccion.java:151` |
| RF-APP-U05 | Respuesta `@LEERV` estricta | IMPLEMENTADO | `RTV10:Tramas.java:264,67` |
| RF-APP-U06 | Interfaz `Protocolo` por familias | IMPLEMENTADO | `RTV10:Protocolo.java` y las cuatro implementaciones |
| RF-APP-U07 | Con un V4 original: medir y verificar, nunca calibrar | IMPLEMENTADO | `RTV10:ProtocoloV4Original.java:131,141,152` |
| RF-APP-U08 | Diario del estado oculto | IMPLEMENTADO | `RTV10:DiarioEstadoOculto.java:29,44` |
| RF-APP-U09 | Un solo flujo para F-36 y F-46; lo que difiere, en CSV | IMPLEMENTADO | `RTV10:PerfilFirmware.java`; asset `firmwares.csv` |
| RF-APP-U10 | Banco de verificación con V4 original | IMPLEMENTADO | `firmwares.csv` (V4_ORIGINAL sin cola); `RTV10:ProtocoloV4Original.java:141` |
| RF-APP-U11 | Identidad y marca "serie declarada" | IMPLEMENTADO | `RTV10:Deteccion.java:30,36` |
| RF-APP-U12 | Cambio de equipo a mitad de campaña | IMPLEMENTADO | `RTV10:Campana.esDeEsteEquipo`, `RTV10:Deteccion.java:175-198` |
| RF-APP-U13 | V4.6 con la EEPROM en blanco | IMPLEMENTADO | `RTV10T:RtvUnicaTest.java:390` (T-U20) |
| RF-APP-U14 | Versionado `1.0.0` / `applicationId` intacto | IMPLEMENTADO | `app/build.gradle` de `rtv-1.0` |
| RF-APP-U15 | Detección en una sola clase, Java puro | IMPLEMENTADO | `RTV10:Deteccion.java` |
| RF-APP-U16 | Lista blanca por perfil, en el único punto de salida | IMPLEMENTADO | `RTV10:Cliente.java:126` |
| RF-APP-U17 | Firma: el PIN de admin y nada más; se retira `FRASE-DIEGO` | IMPLEMENTADO | `RTV10:FlujoCalibracion.java:1623` |
| RF-APP-U18 | Texto literal de la firma; nombre obligatorio | IMPLEMENTADO, **con el defecto D-U01 abierto** | `RTV10:FlujoCalibracion.java:1607,1610,1630,1634` |
| RF-APP-U19 | El nombre es de quien firma ahora; dos nombres en un acta liberada | IMPLEMENTADO | `RTV10:FlujoCalibracion.java:1575,1661` |
| RF-APP-U20 | Columnas `to`, `tc`, `t_estado` y evento `TEMP_SERIE` | IMPLEMENTADO | `RTV10:Campana.java:24-28,672-712` |
| RF-APP-U21 | **Por serie** (apertura y cierre) en banco, por tanda en medida suelta | IMPLEMENTADO | `RTV10:BancoActivity.java:795,867-868,886`; `RTV10:MedidaActivity.java:249,307` |
| RF-APP-U22 | Sensor averiado distinguible (`DESC`) | **APP LISTA, FIRMWARE NO** (PENDIENTE de RF-FW-B13) | `RTV10:Tramas.java:197-222` vs. `V4.6:Calibracion.c:983-990` |
| RF-APP-U23 | Sólo la V4.6 da temperatura; las demás, vacío con motivo | IMPLEMENTADO | `RTV10:ProtocoloV46.java:95`, `ProtocoloV36.java:100`, `ProtocoloV2020.java:42`, `ProtocoloV4Original.java:105` |
| RF-APP-U24 | Campos nuevos al final; diario anterior legible | IMPLEMENTADO | `RTV10:Campana.java:1268-1271` |
| RF-APP-U25 | `#X,<k>,<x>,<TO>,<a>#` | **PREVISTO**: la app admite las dos formas; el firmware manda tres campos | `RTV10:Tramas.java:116` vs. `V4.6:Calibracion.c:774-780` |
| RF-APP-U26 | Ajuste del término de temperatura al pulsar Calibrar | **DECIDIDO, SIN IMPLEMENTAR** | `SPEC-V46` RF-APP-28 |
| RF-APP-U27 | Guarda de recorrido térmico | **GUARDA IMPLEMENTADA, UMBRAL SIN MEDIR** | `RTV10:AjusteTemperatura.java:40,74` |

**Lo que falta, en una línea cada cosa:**

1. El **umbral de recorrido de `TO`** (L-36): lo fija quien esté midiendo la deriva, no esta SPEC.
2. El **cuarto campo de `#T#`** y los **cinco de `#X`** en el firmware V4.6 (RF-FW-B13): sin ellos,
   ni se detecta el sensor averiado ni se sabe qué temperatura entró en cada medida.
3. El **ajuste** en sí (RF-APP-U26), que no empieza hasta que 1 y 2 estén.
4. El **defecto D-U01** de la línea de firma.
5. **Todo lo demás, en un equipo.** Nada de este documento se ha visto en un terminal serie.
