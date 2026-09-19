# Matriz SPEC ↔ código de la app RTV — rama `rtv-1.0`

**Nada de esta matriz está probado en un teléfono ni contra un equipo físico.** Sale de leer el
fuente Java del árbol `D:\IT\wt_rtv10` y las SPEC de la V3.6 y de la V4.6. Ni una sola de las filas
de abajo se ha visto funcionar en un Android real ni con un V3, un V4 o una V4.6 delante. "Implementado"
quiere decir **que el código hace lo que pide el requisito al leerlo**, no que alguien lo haya medido.

**Lo que impide usarla mañana: nada que haya encontrado en este censo.** No hay ningún camino que
rompa la app al abrirla ni que envíe una trama prohibida por sí solo. Hay **tres trampas** que un
operador puede pisar y que estropean una sesión o un registro, y están en §3.1, §3.2 y §3.3; la
primera es la única que puede contaminar un acta.

## 0. Con qué se ha trabajado

| Cosa | Valor | Cómo se fija |
| :--- | :--- | :--- |
| Árbol | `D:\IT\wt_rtv10`, rama `rtv-1.0` | `git branch --show-current` |
| Commit | `d0aba636a53d13b960971931831a50262c61fb5d`, 19-sep-2026 17:47, "Revision y QA de la RTV 1.0.0-rc2: apta con condiciones" | `git log -1` |
| Versión de la app | `versionCode 10001`, `versionName "1.0.0-rc2"` | `app/build.gradle:15,16` |
| Fuente | `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`, **de 66 a 67 ficheros `.java` durante el propio censo** | `find` y `Get-ChildItem`, dos pasadas |

**El árbol estaba cambiando mientras se leía.** Otro agente compilaba la rc3 en él: 16 ficheros de
`main/java` y 5 de `test/java` tenían marca de tiempo posterior al commit citado, entre ellos
`Protocolo.java`, `Tramas.java`, `ProtocoloV46.java`, `FlujoCalibracion.java` y dos ficheros que no
existían al empezar (`AjusteTemperatura.java`, `TemperaturaRtv10Test.java`). Por eso **cada cita de
abajo lleva el símbolo además del número de línea**: el número puede haberse movido; el símbolo, no.

Prefijos: `RTV:` = `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` del árbol
`D:\IT\wt_rtv10`. `SPEC-U` = `05_Documentacion/SPEC-App-Unica-V36-V46.md` de la V3.6.
`SPEC-V46` = `D:\IT\P_RetroVertical_V4.6\05_Documentacion\SPEC-V4.6-BORRADOR.md`.
`PROT-V46` = `D:\IT\P_RetroVertical_V4.6\05_Documentacion\PROTOCOLO-V4.6-BORRADOR.md`.

---

## 1. Sentido 1 — requisitos con código y sin código

### 1.1 La cifra vieja no vale, y ésta es la buena

Circula un **"2 de 14 RF-APP-U implementados"**. Esa medida se hizo contra la **3.6.17 de `main`**,
antes de que existiera la capa de protocolo. **En `rtv-1.0` es falsa.** Recontado desde cero, fila por
fila, abriendo cada fichero:

| Estado | Cuántos | Cuáles |
| :--- | ---: | :--- |
| **Implementado** (el código hace lo que pide el requisito) | **8** | U01, U03, U04, U05, U06, U11, U12, U14 |
| **Parcial** (parte hecha, parte no; el requisito **no** se puede dar por cerrado) | **5** | U02, U07, U08, U09, U13 |
| **Sin código** | **1** | U10 |

De los 5 parciales, **U02 es el más engañoso**: el código está entero y bien, pero **el dato que lo
alimenta está vacío**. Un recuento que mire sólo clases lo daría por hecho.

### 1.2 RF-APP-U01 a U14, uno por uno

| Req | Qué pide | Estado | Evidencia (`símbolo` — `archivo:línea`) |
| :--- | :--- | :--- | :--- |
| **U01** | Detección `#V#` → `9` → `6` → sonda, y sólo ese orden | **Implementado** | `Deteccion.detectarCompleta` — `RTV:Deteccion.java:100-148`: `#V#` en `:102`, `9` en `:122`, `6` en `:131`, sonda en `:140`. Lista cerrada previa: `Deteccion.PERMITIDAS` — `:24`, aplicada en `Cliente.pedir` — `RTV:Cliente.java:121`. Versión distinta de `3.6`/`4.6` → no se envía nada más (`:117-120`, C-U03) |
| **U02** | Perfil por MAC en `equipos.csv`; el V3-2 sólo recibe la sonda | **Parcial: código sí, dato no** | Código completo: `PerfilesEquipo.leerFirmados` — `RTV:PerfilesEquipo.java:80-114`; rama `soloSonda` — `Deteccion.detectar:83-94`; carga — `PerfilesApp.iniciar` — `RTV:PerfilesApp.java:43`. **`app/src/main/assets/equipos.csv` tiene 7 líneas y ninguna es una fila de datos**: 6 de comentario y la cabecera. El propio fichero lo dice en `:6` ("Sin filas: el perfil del V3-2 espera a M-3"). Ver §3.2 |
| **U03** | Mensaje de "sin respuesta válida", sin reintentos | **Implementado** | `Deteccion.SIN_RESPUESTA` — `RTV:Deteccion.java:26-27`, emitido en `detectarCompleta:147`. Tras la sonda no se envía nada |
| **U04** | Sonda `@LEERV` válida, espera ≥ 5 s, búfer vaciado antes de cada petición | **Implementado** | `Tramas.TIMEOUT_LEERV_MS = 5000` — `RTV:Tramas.java:31`, usado en `Deteccion.sonda` — `:152`. Vaciado: `receptor.vaciar()` en `Cliente.pedir` — `RTV:Cliente.java:130`. **Desviación de la SPEC escrita**, ver §1.3 |
| **U05** | Sólo `@LEERV,-?[0-9]{1,6}@`; el eco de la sonda no es respuesta | **Implementado** | `Tramas.P_LEERV_VALOR` — `RTV:Tramas.java:35`; `Tramas.P_LEERV` (extracción) — `:258`; `Tramas.valorLeerv` — `:67-80`. `@LEERV,BLA,2@` no casa (no son dígitos) y `@LEERV,127,45@` tampoco (tras `127` va `,`, no `@`) |
| **U06** | Interfaz `Protocolo` en vez del booleano `esV36`; lista cerrada por firmware | **Implementado** | `Protocolo` — `RTV:Protocolo.java:11-103`, con `ProtocoloV2020`, `ProtocoloV36`, `ProtocoloV46`, `ProtocoloV4Original`. Puerta única: `Cliente.pedir` — `RTV:Cliente.java:119-124`. CA cumplida: **0 apariciones de `esV36` y de `Version.V4` en `main/java`** (comprobado con `grep -rn` y con `Select-String` de PowerShell) |
| **U07** | Con un V4 original: medir y verificar; nunca calibrar | **Parcial** | Medir ✔ (`ProtocoloV4Original.tramaMedida` — `RTV:ProtocoloV4Original.java:48-51`). Etiqueta del tipo 1 ✔ (`etiquetaMedida` — `:151-157`, constantes `:14-15`). No comparar con certificado en tipo 1 ✔ (`MedidaActivity.medir` — `RTV:MedidaActivity.java:317-318`). No calibrar ✔ (`ConexionActivity.refrescar` — `RTV:ConexionActivity.java:144-159`; `AdminActivity:305`). **Falta "verificar"**: no hay banco de verificación (→ U10) |
| **U08** | Diario del estado oculto en toda medida de otros papeles | **Parcial** | Clase completa: `DiarioEstadoOculto` — `RTV:DiarioEstadoOculto.java:12-47`; se reinicia al conectar (`Sesion.reiniciar:114`) y al detectar (`Pruebas.detectar:282`). **Se anota en un solo sitio**: `MedidaActivity.medir` — `RTV:MedidaActivity.java:315`; aviso en `:339`. **Ni el banco ni la campaña anotan nada**, y **la sonda tampoco**: `anotar(k, v, sonda)` no se llama nunca con `sonda = true` (verificado con `grep -rn "anotar("` y con `Select-String`). El CA escrito del requisito ("previa: BLA,1 = *n* (sonda)") **no es alcanzable hoy** |
| **U09** | Un solo flujo para F-36 y F-46; lo que difiere, en `Protocolo` y en CSV | **Parcial** | Cola por firmware ✔: `BancoCola.Tipo.REPRESENTATIVO_V46` — `RTV:BancoCola.java:50`, md5 en `MD5_PERMITIDOS:33`, asset `cola_banco_representativo_v46.csv` (85 líneas). Elección ✔: `BancoActivity:409-412`. Batería por perfil ✔: `Ops` — `RTV:Ops.java:139`. **Pero `PerfilFirmware.xMin`, `xMax` y `coherenciaDef` no los lee nadie en producción**: sólo los asevera `RtvUnicaTest:224-226`. El dominio de `x` sigue cableado a la V3.6 (`Asistente.S_X_MAX = 4300` — `RTV:Asistente.java:251`). Verificado con dos herramientas |
| **U10** | Banco de **verificación** con un V4 original ("Verificación y línea base") | **Sin código** | `ProtocoloV4Original.mideBanco` devuelve `false` — `RTV:ProtocoloV4Original.java:140-143`; `firmwares.csv:8` deja la columna `cola_banco` **vacía** para `V4_ORIGINAL`. No existe ninguna cola de verificación de V4, ni informe de verificación. Con un V4 conectado, "Tomar muestras" y "Campaña" salen deshabilitados con el motivo |
| **U11** | Marca "serie declarada, no leída del equipo" según firmware | **Implementado** | `Deteccion.marcaSerie` — `RTV:Deteccion.java:36-40`, constante `:30`; `Sesion.marcaSerie` — `RTV:Sesion.java:224-226`; llega a `Sesion.identidad` — `:246`, que es lo que encabeza registros y exportaciones. Salvedad: el **informe de verificación** del V4 original que el requisito describe no existe (depende de U10) |
| **U12** | Al reconectar se repite la detección; una campaña sin MAC no casa con nadie | **Implementado; R-U06 cerrado** | `Sesion.reiniciar` al conectar — `RTV:Sesion.java:108-140` (pone `protocolo = null`), llamado en `ConexionActivity.estado:112`; re-detección en `Pruebas.detectar` — `RTV:Pruebas.java:274-300`. **`Campana.esDeEsteEquipo` ya no acepta la MAC vacía**: `RTV:Campana.java:364-369` exige `!mac.isEmpty()` salvo en el lector sin atar |
| **U13** | V4.6 con la EEPROM en blanco | **Parcial** | (1) Se trata como F-46 en `DEF` ✔ (`Deteccion.detectarCompleta:108-112`). (3) Serie de `#GN#` y tolerancia a `NONE` ✔ (`Sesion.serieDeV46` — `RTV:Sesion.java:147-150`; `Sesion.serie` — `:165-180`). **(2) relajado a propósito**: `ConexionActivity.estado:116-122` abre campaña al conectar con la serie **declarada** del nombre BT, antes de detectar nada; el comentario de `Sesion.java:166-167` lo justifica ("para poder medir el banco antes de dar de alta la serie"). **(4) no implementado**: la coherencia en `DEF` con `#E` depende de `coherenciaDef`, que nadie lee (→ U09) |
| **U14** | `versionName` `1.0.0`, `versionCode` ≥ 10000, `applicationId` sin tocar | **Implementado** | `app/build.gradle:15-16` (`10001` / `"1.0.0-rc2"`), `applicationId "com.dpi.retrov36"` en `:11`. Es la opción 1 de la tabla del requisito |

### 1.3 Dónde la SPEC escrita ya no describe al código

Tres sitios en que **el código adelantó a la SPEC**. No son defectos del código; son deuda del
documento, y quien lea la SPEC sin leer el código se llevará una idea falsa.

| Dónde | Lo que dice la SPEC | Lo que hace el código |
| :--- | :--- | :--- |
| `SPEC-U` §2.1 RF-APP-U04 y §5 C-U01 | "Mientras no decida [Diego], se mantiene A" (`@LEERV,BLA,1@`), y C-U01 figura como contradicción **abierta** | La sonda es la **opción B**, `@LEERV,BLA,2@` — `Tramas.SONDA_V4` — `RTV:Tramas.java:29`, usada en `Deteccion.sonda:152`. La cabecera de la propia SPEC (`:9`) ya dice que la revisión P2 cerró C-U01 → `BLA,2`, pero **el cuerpo del documento no se actualizó** |
| `SPEC-U` §2.2 RF-APP-U08, CA | "la sonda A seguida de `@LEERV,BLA,2@` → la segunda medida lleva *previa: BLA,1 = n (sonda)*" | Con la sonda B, la sonda **no** es de tipo 1, así que nunca escribe `ultimaTipo1`: ese CA es **inalcanzable**. El parámetro `sonda` de `DiarioEstadoOculto.anotar` — `RTV:DiarioEstadoOculto.java:29` — **queda sin ningún llamante que lo ponga a `true`** |
| `SPEC-U` §2.3 RF-APP-U09, fila "Dominio de `x`" | "Perfil de firmware (CSV)" | El CSV trae el dato (`firmwares.csv:9`, `0`-`4095`) y `PerfilFirmware` lo parsea, pero **ningún código de producción lo lee** |

### 1.4 Los otros documentos

- **`SPEC-V3.6.md` RF-APP-01 a RF-APP-48.** No se recorren aquí uno por uno: los cubre
  `MATRIZ-SPEC-codigo-V3.6.md`, hecha contra la **app 3.6.9**. Lo que `rtv-1.0` cambia de ese bloque
  y hay que volver a comprobar cuando alguien la actualice: RF-APP-03 (detección, sustituida por
  RF-APP-U01), RF-APP-05/06 (medida y `x`, ahora por `Protocolo.tramaMedida` / `tramaX`),
  RF-APP-25 (línea base, ver §3.1) y RF-APP-43 (batería antes de escribir, ahora por
  `Protocolo.tramaBateria`).
- **`SPEC-Calibracion-V3.6.md` (RF-CAL-01 a RF-CAL-43).** Su flujo lo implementa `FlujoCalibracion` y
  `rtv-1.0` no le cambia el contrato: lo único que la rama mueve es **de dónde sale la trama**.
  RF-CAL-41 pide la batería "con la orden `9`", y ahora sale del protocolo
  (`Ops.bateria` — `RTV:Ops.java:131-145`, `Protocolo.tramaBateria`), que en la V4.6 devuelve `#GB#`:
  el requisito escrito se quedó corto, el código va bien. Un repaso fila a fila de los 43 **no se ha
  hecho** y no debe darse por hecho. Lo único que sí se comprobó, porque es visible y grande:
  **RF-CAL-31 a RF-CAL-34, el PDF del informe, no están implementados.** El informe sale en texto
  plano (`ExportadorFinal.informe` — `RTV:ExportadorFinal.java:32-43`; el propio fichero lo dice en
  `:13` y `CalibrarActivity:520`). RF-CAL-30 (el título, que no puede decir "certificado de
  calibración") **sí** se cumple: `ExportadorFinal.TITULO` — `:17`.
- **`SPEC-V46` §3.4 y `PROT-V46`.** Ver §4: de ahí sale la lista de lo que falta para calibrar un V4.6.

---

## 2. Sentido 2 — código sin requisito, código muerto y pruebas mudas

### 2.1 Código muerto

Todos los negativos de esta tabla están comprobados **dos veces y con dos herramientas distintas**:
`grep -rn` de Bash y `Select-String` de PowerShell, cada una sobre `main/java` y sobre `test/java` por
separado. "Sólo en pruebas" significa que en `main/java` no hay **ningún** llamante.

| Símbolo | Dónde | Estado | Por qué importa |
| :--- | :--- | :--- | :--- |
| `ExportadorFinal.exportar(...)` y su interfaz `ExportadorFinal.Descargas` | `RTV:ExportadorFinal.java:55-71`, interfaz en `:22-25` | **Confirmado: cero llamadas** a `.exportar(` en todo el árbol. `Descargas` no aparece fuera de su fichero | **Matiz importante: la clase `ExportadorFinal` NO está muerta.** `ExportadorFinal.informe` y `.nombreInforme` sí se llaman, desde `CalibrarActivity:530,533`. Lo huérfano es sólo el par `exportar()`/`Descargas`. Los "Descargas" que aparecen en `Campanas.java:235,240,545,683` son `copiarADescargas`, un homónimo parcial, no la interfaz |
| `ProtocoloV46(PerfilFirmware, boolean)` | `RTV:ProtocoloV46.java:28-31` | **Confirmado: sólo en pruebas** (`FlujoCalibracionTest:1404`, `RupturaRtv10Test:363`). Las dos construcciones de producción (`Deteccion.detectarCompleta:112`, `FlujoCalibracion.identificar:269`) usan los otros dos constructores | Es **la llave de la calibración V4.6** (§4). **Corrección al encargo: es de DOS argumentos** (`perfil` y `calibracion`), no de tres; comprobado leyendo la firma |
| `FlujoCalibracion.identificar(...)` | `RTV:FlujoCalibracion.java:255-279` | **Sólo en pruebas.** 6 llamadas, todas en `test/java` (`FlujoCalibracionTest:310,1110,1399`, `RupturaE2ETest:73`, `RupturaFlujoTest:208`, `RupturaSerieTest:295`) | **Es el hallazgo más incómodo del censo.** Las pruebas de extremo a extremo entran al flujo por `identificar()`, que **la app no ejecuta nunca**: la pantalla real toma el protocolo de la sesión, `CalibrarActivity:103` (`ctx.protocolo = s.protocolo != null ? s.protocolo : new ProtocoloV36()`). Es decir, **los recorridos E2E arrancan por una puerta que en el teléfono está tapiada** |
| `AjusteTemperatura` (clase entera: `evaluar`, `Decision`, `RECORRIDO_TO_MINIMO`, `SE_QUEDA_FABRICA`) | `RTV:AjusteTemperatura.java:30-112` | **Sólo en pruebas** (`TemperaturaRtv10Test:232-256`) | Se declara a sí misma "SITIO PREPARADO" (`:6`) y es honesta. Pero **la guarda que impide ajustar el término de temperatura con datos degenerados no está conectada a nada**: hoy no guarda (§4, punto 6) |
| `PerfilFirmware.xMin`, `.xMax`, `.coherenciaDef` | `RTV:PerfilFirmware.java:21,22,25` | **Se escriben y nunca se leen** en producción. Únicos lectores: `RtvUnicaTest:224-226` | Son el dato que RF-APP-U09 dice que gobierna el dominio de `x` y la comprobación en `DEF`. Ver §4, puntos 2 y 3 |
| `Tramas.tramaST(double, double, double)` | `RTV:Tramas.java:467` (símbolo `Tramas.tramaST`) | **Sólo en pruebas** (6 apariciones) | Deliberado y declarado en su javadoc: la app no escribe la temperatura en la V3.6 (RF-APP-23). Se deja en la lista porque, si algún día se activa RF-APP-28 de la V4.6, es el espejo de límites que hay que reutilizar |
| `Tramas.claveDeLeerv(String)` | `RTV:Tramas.java:52` | **Sólo en pruebas** (4 apariciones) | Es la inversa de `tramaLeerv`. Sin llamante: nada de la app necesita hoy sacar la clave de una petición `@LEERV` |
| La rama `sonda = true` de `DiarioEstadoOculto.anotar(char, Integer, boolean)` | `RTV:DiarioEstadoOculto.java:29-41` | **Ninguna llamada con `true`** en todo el árbol (`main` y `test`) | Es el CA de RF-APP-U08 que quedó inalcanzable al cambiar la sonda a `BLA,2` (§1.3) |

#### El barrido sistemático

Además de lo anterior, se recorrieron **513 declaraciones `public` con paréntesis** (métodos, sin
contar `@Override` ni constructores) y **unos 408 campos públicos**, con las mismas dos herramientas.
Resultado:

**Sin ningún llamante en absoluto, ni en producción ni en pruebas:**

| Símbolo | Dónde | Nota |
| :--- | :--- | :--- |
| `Ecuacion.diferenciaRelativa` | `RTV:Ecuacion.java:119` | — |
| `EnlaceSerie.quitarOyenteRx` | `RTV:EnlaceSerie.java:75` | Existe `agregarOyenteRx` y se notifica, pero **nunca se retira un oyente** |
| `Hex.byteHex` | `RTV:Hex.java:26` | — |
| `Registro.intentCompartir` | `RTV:Registro.java:148` | — |
| `Ritmo.esperaMs` | `RTV:Ritmo.java:64` | `Ritmo` sí se usa (`Cliente.java:27,28,39,131,132,205,206`), pero **este método no está entre las llamadas** |
| `Campana.T_ESTADO_DESC` (campo) | `RTV:Campana.java:674` | **El más significativo de los seis.** Sus hermanos `T_ESTADO_OK` y `T_ESTADO_SIN` (`:672,676`) se usan en 8 sitios. El comentario de `:673` dice para qué está ("un sensor muerto se parece a un día fresco"): detectar el estado `DESC` del sensor óptico. **Pero nada en el árbol produce ni lee ese valor**: la constante existe y el camino que la asignaría, no. Es el punto 4 de RF-APP-28 de la V4.6 ("un sensor averiado no puede pasar por día fresco") a medio cablear |

**Con llamante sólo en `test/java`:**

| Símbolo | Dónde | Nota |
| :--- | :--- | :--- |
| `Acta.evaluarRemedida` | `RTV:Acta.java:612` | `Version368Test` |
| `Fabrica.codigoIntenso` | `RTV:Fabrica.java:53` | `CalculoTest` |
| `Tramas.peticionPermitida` | `RTV:Tramas.java:261` | `CalculoTest`. Su papel lo asumió `Protocolo.permitida` vía `Cliente.pedir:121` |
| `Tramas.temperaturaAjustada` | `RTV:Tramas.java:366` | `CalculoTest`. **Relevante para §4 punto 7**: el bit 12 de la máscara de `#V#` se sabe leer y **no lo lee nadie en producción**; el bit 13 de la V4.6 ni siquiera existe |
| `Tramas.tramaST`, `Tramas.claveDeLeerv` | ya en la tabla de arriba | — |

**Lo que NO salió, y también es un dato:** ninguna clase del paquete está huérfana del todo, y las
**8 Activities del manifiesto cuadran exactamente con las 8 del código** (`AndroidManifest.xml:27-43`;
`ConexionActivity` es la de arranque, `MAIN`/`LAUNCHER`, y por eso nadie hace `startActivity` hacia
ella; `Base` es abstracta y no debe estar en el manifiesto).

**Un falso positivo, declarado para que nadie lo repita:** `LineaBase.Progreso`
(`RTV:LineaBase.java:23`) parecía sin uso —cero menciones del nombre fuera de su fichero— y **no lo
está**: `PruebasActivity:182-183` la implementa con `this::progresoLb`, una referencia a método que no
menciona el tipo en ningún sitio. Buscar interfaces funcionales por nombre de tipo tiene ese punto
ciego. Los métodos de las tablas de arriba se verificaron por **sintaxis de llamada** (`nombre(`), que
no lo tiene.

**Nota de volatilidad, medida:** durante el censo el paquete pasó de **66 a 67 ficheros** `.java`.
Apareció `Ritmo.java` (sin seguimiento en git), y `TemperaturaRtv10Test.java` no existía en una
primera pasada por `test/java` y sí en una segunda, segundos después. `Campana.T_ESTADO_DESC` cambió
de la línea 644 a la 674 entre dos lecturas. Ninguna cifra de esta sección es estable: es una
fotografía del árbol mientras se compilaba la rc3 encima.

### 2.2 Caminos que el operador puede tocar y no responden a ningún requisito

| Camino | Dónde | Qué requisito lo pide |
| :--- | :--- | :--- |
| **"Línea base forzando V3 2020 sin e (sin detectar)"** | `PruebasActivity.onCreate` — `RTV:PruebasActivity.java:75`; acción `lineaBaseForzada` — `:118-132`; efecto en `fase1` — `:149-153` | **Ninguno.** RF-APP-25 describe la línea base, no una variante que **salte la detección y fije el protocolo a mano**. Ver §3.1: es la trampa grave |
| **El diálogo "¿Forzar V3 2020 y enviar igualmente los 12 códigos y 9?"** que aparece cuando la detección no reconoce el equipo | `PruebasActivity.fase1` — `:172-177` | **Ninguno**, y además **contradice RF-APP-U03**, que dice que ante un desconocido "no se ofrece reintentar con otras tramas". Aquí se ofrece, en un diálogo, tras haber fallado la detección |
| **"Guardar / Compartir"** (los dos ZIP en un solo selector) | `ConexionActivity.onCreate:53`, `guardarCompartir` — `:168-184` | Ninguno escrito. El código lo atribuye a un principio de Diego de la 3.6.17 (`:165-167`), no a un RF. Es útil y no es peligroso, pero **no está especificado** |
| **Dos entradas distintas a la misma pantalla** (botón grande "Tomar muestras" y "3. Campaña…"; botón grande "Calibrar" y "4. Calibrar este equipo") | `ConexionActivity.onCreate:51,55` frente a `:68,70` | Ninguno. No es un defecto, pero **los dos pares se habilitan con condiciones distintas**: `btnMuestras`/`btnGrabar` en `:144-147` y `btnCampana`/`btnCalibrar` en `:152-159`. Hoy coinciden; si un día divergen, el operador tendrá un camino abierto y otro cerrado a lo mismo |
| **Compatibilidad del formato de diario entre versiones de la app** (leer un diario 3.6.11 o 3.6.16 con un lector más antiguo) | Ejercido por `RupturaSerieTest.i_instalarEncimaDe3611ConElDiarioReal:623` y `j_diario3616ParaEl3615:634` | **Ninguno.** Hay código y hay pruebas (mudas), pero no hay requisito que diga qué debe pasar. Sólo RF-APP-40 roza el tema ("instale la versión nueva encima") |
| **Campaña que cruza dos días naturales** | Ejercido por `RupturaFlujoTest.r07UnDiaDespues:450` | **Ninguno** en las tres SPEC |

Un camino menor que conviene tener anotado: **se puede entrar al banco sin haber detectado nada.**
El diálogo que abre las pruebas al conectar (`PruebasActivity.onCreate:79-88`) tiene un botón "Más
tarde"; si el operador lo pulsa, `Sesion.protocolo` sigue a `null` y "Tomar muestras" queda habilitado
(`ConexionActivity.refrescar:146`, que con `pr == null` habilita). El paso de medida está guardado
(`BancoActivity.medir:731-735`), pero **el paso BATERÍA no**: `BancoActivity.bateria:603` hace
`new Ops(Cliente.instancia(), Sesion.get().protocolo)` y `Ops` **se inventa un `ProtocoloV36` cuando
recibe `null`** (`RTV:Ops.java:28-31`). El daño es acotado —acabaría enviando `9`, que es una de las
tramas de la propia detección— pero el patrón "si no hay protocolo, supón V3.6" está ahí y no lo pide
ningún requisito.

Y dos divergencias entre lo escrito y lo que hace el código, que no son caminos nuevos pero tampoco
cumplen el requisito:

- **RF-APP-25 pide `9` diez veces** (`SPEC-V3.6.md:716`). El código lo hace **tres**:
  `LineaBase.REPETICIONES_9 = 3` — `RTV:LineaBase.java:27`, usado en `LineaBase.bateria:69`. El
  javadoc de la clase (`:14`) dice "varias veces", que no es lo que pide la SPEC.
- **RF-APP-26 se quedó viejo, y el código es más estricto que él.** El requisito
  (`SPEC-V3.6.md:719-723`) dice que antes de detectar "sólo salen `#V#`, `e`, `6` y la sonda
  `@LEERV,BLA,1@`". Hoy salen `#V#`, `#GC#`, `#GN#`, `9`, `6` y `@LEERV,BLA,2@`
  (`Deteccion.PERMITIDAS` — `RTV:Deteccion.java:24`), **nunca `e`**. El límite de 49 bytes que
  RF-APP-26 exige se cumple por construcción, porque antes de detectar la lista cerrada sólo deja
  pasar esas seis cadenas. **El requisito hay que reescribirlo; el código no.**

### 2.3 Qué cobertura se pierde por las 25 pruebas mudas

**El recuento no se ha vuelto a medir**: se da por bueno el dato de `CLAUDE.md` §7 y del encargo —de
299 pruebas, **274 aseveran**; las 25 mudas son `RupturaSerieTest` (12), `RupturaFlujoTest` (8),
`RupturaE2ETest` (3), `RupturaEquivTest` (1) y `RupturaBancoTest` (1)—. Lo que se ha hecho es la
pregunta que faltaba: **qué requisito se queda sin cobertura real por cada una.**

Primero, un dato que cambia cómo hay que leerlas: **ninguno de los 25 declara requisito**. Son
"pruebas de ruptura del revisor P15" y ninguna nombra un RF, un T-U ni un AT-U en su javadoc. Lo que
cubren se infiere del recorrido, y aquí se dice cuándo es inferencia.

Segundo: **casi todas son mudas pero redundantes.** El mecanismo que ejercen está aseverado en otro
sitio de la suite. Por ejemplo, los cortes de `#SN,` de `RupturaSerieTest.a2` los cubre
`FlujoCalibracionTest.renombrarCortadoDespuesDelSNSeRecupera` (`:1296-1314`) y
`…SeRepite` (`:1316-1334`), las dos con asertos; el renombrado que sí viaja al importar lo cubre
`FlujoCalibracionTest.elRenombradoViajaYOtroTelefonoCalibra` (`:1344-1358`); el rechazo por MAC
distinta, `Version3615Test.conOtraMacLaSerieAntiguaSeRechaza` (`:371-384`).

**Tres cosas se quedan con cero cobertura aseverada**, cada una cruzada con dos herramientas
(`Grep` y `grep` de Bash) sobre toda la suite:

| Qué se queda sin aseverar | Única prueba que lo toca | Requisito afectado |
| :--- | :--- | :--- |
| **`Campanas.claveRenombrada`** — localizar la campaña archivada tras un renombrado. El símbolo **no aparece en ninguna otra prueba de la suite**, ni siquiera en los 72 tests de `FlujoCalibracionTest` | `RupturaSerieTest.d2_claveRenombradaEnJvm` — `test/…/RupturaSerieTest.java:500` | RF-APP-35 / RF-APP-39 (inferido: ningún T-/AT- lo nombra) |
| **`#SN,` respondido con `Falla.OK_SIN_HACER`**: el equipo contesta OK y **no** aplica el cambio. No es un corte de enlace, es un equipo que miente. `FlujoCalibracionTest` inyecta `CORTE_ANTES` y `CORTE_DESPUES` en `#SN,` (`:1298`, `:1318`, aseverados) pero **nunca `OK_SIN_HACER`** | `RupturaSerieTest.b1_snNoEntraYRevierte` — `:422` | **RF-APP-35** (identidad por `#GN#`: si no coinciden, no se mide ni se calibra y se dice por qué) |
| **Corte del enlace durante `#S,b,`, `#S,5,` o `#V#` dentro de un "Calibrar todo" 8→b→5.** En `FlujoCalibracionTest` las únicas inyecciones son `#F,8#`, `#S,8,`, `#SC,`, `#L,`, `#GC#`, `#E,8,2000#` y `#SN,`: **el segundo y el tercer código de una misma sesión nunca se cortan** | `RupturaFlujoTest.r02CortesEnCadaFase` — `:276` | **RF-APP-37** ("nunca hay un `#S` de otro código antes de resolver *k*"). Hoy esa garantía sólo se ejerce con el **primer** código de la sesión |

El segundo y el tercero son los que importan. **RF-APP-35 y RF-APP-37 son requisitos de integridad de
la escritura en EEPROM**, y su caso más difícil —el equipo que dice OK sin hacer nada, y el corte a
mitad del segundo código de una tanda— está escrito en una prueba que pasa haga lo que haga el
código. Dicho de otro modo: **son exactamente los dos casos que la suite parece cubrir y no cubre.**

Salvedad de alcance, declarada por quien lo midió: no se cotejaron los 20 tests de `RupturaSerieTest`
y `RupturaFlujoTest` uno a uno contra los 299; se priorizaron los tres cuyo cruce con dos
herramientas salió negativo. Los casos `i`, `j` (compatibilidad del formato de diario entre versiones)
y `r07` (campaña que cruza dos días) **no tienen requisito escrito en ninguna de las tres SPEC**: no
son "cobertura perdida", son **recorridos sin requisito**, y van también a §2.2.

---

## 3. Las tres trampas

### 3.1 "Línea base forzando V3 2020 sin e (sin detectar)" deja el protocolo falseado toda la sesión

**Es la más grave de las tres, y es la única que puede contaminar un acta.** Matiza el enunciado con
que llegó el encargo: **no es que "no compruebe el protocolo"** — comprueba el de `ProtocoloV2020`, y
por eso nunca puede enviar `e`, `@` ni `#`. Lo peligroso es otra cosa.

- El botón: `PruebasActivity.onCreate` — `RTV:PruebasActivity.java:75`, acción `lineaBaseForzada` — `:118-132`.
- No comprueba nada salvo que haya enlace: `:119`. A diferencia del botón normal `lineaBase`, que sí
  rechaza los protocolos que no admiten el byte `1` (`RTV:PruebasActivity.java:100-104`).
- `PruebasActivity.fase1(p, true)` — `:149-153` — hace `s.protocolo = new ProtocoloV2020()` **y**
  `s.detectado = true`.
- **Nada lo deshace solo.** `Sesion.protocolo` vuelve a su sitio por dos caminos, y los dos exigen que
  el operador haga algo más: `Sesion.reiniciar` (`RTV:Sesion.java:112-113`), que **sólo** se llama al
  conectar —un único sitio en toda la app, `ConexionActivity.estado:112`, verificado con `grep`—, y
  `Pruebas.detectar` (`RTV:Pruebas.java:278-281`), es decir, volver a lanzar las pruebas del equipo.
  Si el operador no hace ninguna de las dos, **toda la app sigue creyendo que el equipo es un V3 de
  2020** durante el resto de la sesión. Y no hay nada en pantalla que se lo recuerde: el texto forzado
  queda en el cuadro de la línea base (`:152`) y en el registro (`:153`), no en la cabecera.

Lo que se sigue de ahí, con un V4 o una V4.6 conectados:

1. `Sesion.firmware()` — `RTV:Sesion.java:192-208` — devuelve "V3 2020 (sin e)", y ese texto entra en
   `Sesion.identidad()` (`:246`), que encabeza el registro de tramas, el CSV de línea base
   (`LineaBase.fila` — `RTV:LineaBase.java:80-84`) y las exportaciones. **Un registro de un V4
   quedaría archivado como de un V3 de 2020.**
2. "Tomar muestras" y "Campaña" se habilitan (`ProtocoloV2020` hereda `mideBanco() == true` de
   `ProtocoloV36:136-138`), y el banco mediría con bytes sueltos contra un equipo que los ignora:
   doce timeouts por paso.
3. Con un firmware **sin fuente** (el V3-2 de `ID-V3-2`), los 13 bytes que envía
   `LineaBase.codigos` / `LineaBase.bateria` (`RTV:LineaBase.java:42-59`, `:69-75`) tienen **efecto
   desconocido**. Eso choca de frente con `CLAUDE.md` §4 de este repositorio.

No es un bloqueo para mañana: hay que pulsar un botón que dice lo que hace, y confirmar un diálogo.
Pero **es el defecto que más barato sale de arreglar y más caro de descubrir después**, porque no
falla: contamina en silencio.

### 3.2 `equipos.csv` está vacío, así que el V3-2 no tiene protección

RF-APP-U02 existe para una cosa: que al V3-2 —un F-41 **sin fuente**— no le llegue ninguna trama de
efecto desconocido. El mecanismo está entero y funciona. **El fichero que lo activa no tiene ni una
fila** (`app/src/main/assets/equipos.csv`, 7 líneas: 6 de comentario y la cabecera; el comentario de
`:6` lo declara).

Consecuencia: si mañana alguien conecta el V3-2, la app ejecuta la detección completa y le envía
`#V#`, `9` y `6` antes de la sonda. En el fuente V4.1 esos tres son inocuos (`SPEC-U` §1.2), pero
**el V3-2 no es el fuente V4.1: es el firmware del que no hay fuente**. La regla de la casa dice que
de eso no se concluye "inocuo".

Se cierra con una línea en el CSV —MAC, `SOLO_SONDA`, variante `F41`, fecha, firmante Diego y
documento— o con la decisión escrita de que no hace falta. Hoy no está ninguna de las dos.

### 3.3 Con una V4.6 conectada, las pruebas dan APTO sin haber comprobado nada

`Pruebas` salta las pruebas 3 a 6 cuando el protocolo no admite el byte `1`, que es justo el caso de
la V4.6 (`ProtocoloV46.permitida` — `RTV:ProtocoloV46.java:44-51`, no acepta bytes sueltos):

- `RTV:Pruebas.java:153-161`: las pruebas 3-6 quedan `NO_APLICA` con el motivo "V4.6: pruebas 3-6
  pendientes de la tabla de tramas firmada (F-1) y de la x (F-5)" y **se llama a `terminar(true, …)`**,
  es decir, **APTO**.

El motivo está escrito y es honesto. El problema es lo que ese APTO abre: `FlujoCalibracion.previas`
exige `apto == TRUE` (`RTV:FlujoCalibracion.java:378-381`), y hoy sólo lo frena el candado aparte de
§4. **El día que se levante ese candado, una V4.6 podrá entrar a calibrar habiendo pasado cero
comprobaciones de coherencia.** Va en la lista de §4 por eso.

---

## 4. Sentido 3 — qué falta para habilitar la calibración de un V4.6

Hoy está deshabilitada a propósito y con una sola llave:

- `ProtocoloV46.calibra()` — `RTV:ProtocoloV46.java:134-137` — devuelve `calibracion && mideBanco()`.
- `calibracion` sólo se pone a `true` por el constructor de tres argumentos
  (`ProtocoloV46(PerfilFirmware, boolean)` — `:28-31`), y **ninguna de las dos construcciones de
  producción lo usa**: `Deteccion.detectarCompleta:112` y `FlujoCalibracion.identificar:269` llaman al
  constructor sin argumentos.
- El motivo que se ve en pantalla: `ProtocoloV46.MOTIVO_CALIBRAR = "Calibración V4.6: próxima versión"` — `:13`,
  pintado en `ConexionActivity.refrescar:157-158` y comprobado como previa en
  `FlujoCalibracion.previas:376-377`.

`mideBanco()` **ya vale `true`** (`:145-147`: la cola V4.6 existe, `firmwares.csv:9` +
`cola_banco_representativo_v46.csv`). Así que quitar el candado es una línea. **Lo que no es una
línea es lo que hay detrás.** Esto es lo que habría que escribir, cada punto con por qué:

| # | Qué falta | Por qué hace falta antes de quitar el candado |
| :--- | :--- | :--- |
| 1 | **Pruebas 3-6 para la V4.6** (coherencia de las 12 claves y repetibilidad con `#X`) | Hoy la V4.6 sale APTO sin comprobar nada (§3.3), y `FlujoCalibracion.previas:378-381` sólo mira `apto == TRUE`. Es `SPEC-V46` RF-APP-08 y RF-APP-11. Falta además la **tabla de fábrica de la V4.6** (`SPEC-V46` RF-APP-09): `Fabrica` — `RTV:Fabrica.java:3-9` — sólo lleva la de 2020 |
| 2 | **Leer `PerfilFirmware.coherenciaDef`** y comprobar `DEF` con `#E`, no con `#G` | El dato está en `firmwares.csv:9` (`E`) y **nadie lo lee** (verificado con `grep` y con `Select-String`: sólo lo toca `RtvUnicaTest:224`). En `DEF` la V4.6 devuelve un polinomio **equivalente, no bit a bit** (`PROT-V46:202`): comprobar con `#G` a 1 ulp daría NO APTO siempre. Es RF-APP-U09 y RF-APP-U13 punto (4) |
| 3 | **Leer `PerfilFirmware.xMin`/`xMax`** y usarlos donde hoy están cableados | `Asistente.S_X_MAX = 4300` — `RTV:Asistente.java:251` — y el 600 de la V3.6 valen para la V3.6. El dominio de la V4.6 es `0`-`4095` (`firmwares.csv:9`, `SPEC-V46` RF-FW-37). Sin esto, el asistente valida la forma de la curva en un rango que no es el del equipo |
| 4 | **Límites de `#S` de la V4.6** | `PROT-V46:203` los deja "por fijar con el código V4 (L-19)". `Sesion.limitesS()` — `RTV:Sesion.java:254-256` — decide por la **fecha de compilación del firmware V3.6**: no sirve para la V4.6. Mientras no estén fijados, la app puede enviar un `#S` que el equipo rechace, o —peor— que acepte y no deba |
| 5 | **`#GA,1#` / `#GA,2#` y `#SA,1,…#` / `#SA,2,…#`** (la mitad aditiva `S(TO)` del término de temperatura) | **Cero apariciones en toda la app** (verificado con `grep -rl` sobre `main/java`). `SPEC-V46` RF-APP-28 puntos 5 y 6 los exigen en la copia previa y en la escritura, con **relectura de las dos subtramas** porque `#SA` no es atómico (`PROT-V46:207-208`, RF-FW-B12) |
| 6 | **El umbral `RECORRIDO_TO_MINIMO`** | `AjusteTemperatura.RECORRIDO_TO_MINIMO` vale `NaN` a propósito — `RTV:AjusteTemperatura.java:40` — y con `NaN` la guarda **nunca** autoriza el ajuste (`evaluar:96-102`). Está bien puesto así: el número no está medido. Pero **`AjusteTemperatura` no la llama nadie en producción** (sólo `TemperaturaRtv10Test`), así que hoy la guarda no guarda nada: es un sitio preparado, no un freno |
| 7 | **Acta y máscara de la V4.6** | `#V#` de la V4.6 añade **bit 13 = `S(TO)` fuera de fábrica** (`PROT-V46:199`). `Tramas.InfoVersion.temperaturaAjustada` — `RTV:Tramas.java:366` (símbolo `Tramas.InfoVersion.temperaturaAjustada`) — sólo mira el bit 12, y **su único llamante está en `CalculoTest`**: en producción no lo lee nadie. Un equipo con `S(TO)` ajustado se leería como de fábrica. En la misma línea, `Campana.T_ESTADO_DESC` (`:674`) existe y **nadie lo escribe ni lo lee**: el punto 4 de RF-APP-28 ("un sensor averiado no puede pasar por día fresco") está a medio cablear |
| 8 | **Decidir qué pasa con `TablaCalibracion.heredados`** | `FlujoCalibracion.heredadosTodos` — `RTV:FlujoCalibracion.java` (método `heredadosTodos`) — arranca de `TablaCalibracion.heredados(equipo())`, que es el 1 y el 2 heredados **de SLV-002**. Con otro equipo y otro firmware eso hay que decidirlo por escrito, no heredarlo por omisión |
| 9 | **Cerrar C-U03 con el firmware real** | `Deteccion.detectarCompleta:108` acepta **sólo** la cadena `4.6` exacta. Si el firmware que se grabe responde `4.6A` o `4.6B`, la app lo dará por desconocido y no operará. Es una decisión ya tomada y bien documentada (`:117-119`), pero hay que comprobarla contra el `#V#` del firmware que se grabe, no contra el borrador |

**Y el requisito previo a todos:** `ProtocoloV46` dice de sí mismo que existe "sólo en simulador
hasta que exista una V4.6 grabada" (`RTV:ProtocoloV46.java:8-9`). No hay firmware V4.6. Ninguno de
los nueve puntos se puede cerrar midiendo hasta que lo haya.

---

## 5. Lo que este censo NO ha hecho

Se dice para que nadie lo dé por hecho:

- **Nada se ha ejecutado.** Ni la app en un teléfono, ni la suite de pruebas, ni una sola trama
  contra un equipo.
- **No se ha recorrido `SPEC-V3.6.md` RF-APP-01 a RF-APP-48** fila por fila contra `rtv-1.0`
  (§1.4). La matriz que los cubre es de la app **3.6.9**.
- **No se ha recorrido `SPEC-Calibracion-V3.6.md`** requisito por requisito.
- **No se han revisado los recursos** (`res/`, `values/`, `xml/`). Del manifiesto sólo se han cotejado
  las Activities (8/8) y los permisos declarados; el `FileProvider` y sus rutas, no.
- **El barrido de código muerto de §2.1 es de métodos y campos `public`.** Lo `private` y lo de
  paquete no se ha barrido, y una interfaz funcional implementada con `this::metodo` puede escaparse
  del barrido por nombre de tipo (hay un caso documentado en §2.1).
- **No se ha mirado el firmware.** Ese alcance es de otro subagente.
