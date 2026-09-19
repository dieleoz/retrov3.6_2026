# Revisión QA — RTV 1.0.0-rc2

**VEREDICTO: APTO CON CONDICIONES**, tanto para medir el banco de un V4.6 recién grabado (SLV-003-2026)
como para seguir con SLV-002. Las condiciones de campo del §6 no son recomendaciones: sin ellas el
veredicto es NO APTO.

> **Nada de lo que sigue se ha probado nunca contra un equipo físico ni en un teléfono.** Todo sale de
> leer el fuente y de ejecutar las pruebas JVM en esta máquina. La V4.6 no existe grabada en ningún
> equipo: se prueba contra `EquipoSimuladoV46`.

- Árbol: `D:\IT\wt_rtv10`, rama `rtv-1.0`, commit `a1fbc63`.
- APK: `03_App_Movil/RTV-V1.0.0-rc2.apk`, md5 `fd44bd558f23e228ff7989ec6571b393`. La copia
  `RTV-V1.0.0-rc2-10001.apk` tiene el **mismo md5** (comprobado con `md5sum` sobre los dos ficheros).
- `aapt dump badging`: `package name='com.dpi.retrov36' versionCode='10001' versionName='1.0.0-rc2'`,
  `sdkVersion:'24'`, `targetSdkVersion:'30'`, `compileSdkVersion='30'`. Permisos: `BLUETOOTH`,
  `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`, `WRITE_EXTERNAL_STORAGE` (maxSdk 28). **Sin `INTERNET`.**
  Coincide con lo declarado en `03_App_Movil/RetroV36/README.md:11-13`.
- Correspondencia APK↔fuente: el asset `cola_banco_representativo_v46.csv` tiene el mismo md5
  `c46154fa4559f8e8f2df5b230537eb66` dentro del APK y en el árbol, y ese md5 está en
  `BancoCola.MD5_PERMITIDOS` (`BancoCola.java:28-33`).

---

## 1. Recuento real de pruebas

Compiladas con `./gradlew compileDebugUnitTestJavaWithJavac --offline` (JDK 11.0.24+8) y ejecutadas con
`JUnitCore` sobre las 25 clases que lista `03_App_Movil/RetroV36/README.md:44-54`.

```
Time: 5,383
OK (299 tests)
```

**Las 299 están en verde. De esas 299, sólo 274 aseveran algo; 25 no aseveran nada.**

El recuento de las 25 mudas se obtuvo analizando cada método `@Test` y comprobando si su cuerpo
contiene alguna llamada `assert*(` / `fail(` o un `@Test(expected=…)`. Reparto:

| Clase | Tests | Aseveran | Mudos |
| :--- | ---: | ---: | ---: |
| `RupturaSerieTest` | 15 | 3 | **12** |
| `RupturaFlujoTest` | 9 | 1 | **8** |
| `RupturaE2ETest` | 3 | 0 | **3** |
| `RupturaEquivTest` | 1 | 0 | **1** |
| `RupturaBancoTest` | 1 | 0 | **1** |
| `RupturaRtv10Test` | 12 | 12 | 0 |
| `RtvUnicaTest` | 19 | 19 | 0 |
| `FlujoCalibracionTest` | 71 | 71 | 0 |
| Las otras 17 clases | 168 | 168 | 0 |
| **Total** | **299** | **274** | **25** |

Lo que esto significa, dicho sin adorno: **los 25 mudos son exactamente los recorridos de extremo a
extremo del flujo V3.6 / banco**, es decir, la cobertura narrativa del equipo que ya tiene acta. No son
inútiles —detectan que el recorrido no lanza excepción, que es un humo real— pero **no comprueban ni un
solo valor**. Tres ficheros llevan en su cabecera una frase que afirma lo contrario:

- `RupturaEquivTest.java:4` — «*Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige el defecto, lo
  comprueba con assert*». Su único `@Test`
  (`RupturaEquivTest.java:17-31`, `verificacionYAjusteDelMismoGrupo`) sólo hace `System.out.println`.
  La única aparición de la palabra «assert» en el fichero es esa línea de comentario.
- `RupturaBancoTest.java:4`, misma frase; su único `@Test` (`RupturaBancoTest.java:141-144`) es
  `public void recorrido() { main(new String[0]); }`, y el javadoc de `:140` ya avisa: «*(imprime lo
  observado)*».
- `RupturaE2ETest`: 3 tests, cero asserts.

Además, en `RupturaSerieTest.java:324` el helper `calibrarYAceptar8()` **no lo llama nadie dentro de esa
clase** (verificado con `grep` y con la herramienta Grep: las 13 llamadas están todas en
`FlujoCalibracionTest`, que tiene su propia copia en `:339`). Es código muerto dentro del fichero.

**Conclusión del recuento:** el «299 en verde» es cierto pero no vale como argumento de entrega tal
cual. La cifra honesta es **274**, y las 25 que faltan están concentradas justo donde más se necesitaría
cobertura de comportamiento: el recorrido completo de una campaña de banco.

### 1.1 Lo que sí es cobertura de verdad: los 12 nuevos de `RupturaRtv10Test`

Los 12 casos nuevos (R-01 a R-10, con R-01 bis y R-01 ter) **aseveran todos**: 99 apariciones de
`assert`/`fail` en el fichero. En particular **R-05** (`RupturaRtv10Test.java:186-250`) tiene 17
sentencias `assert`, enumera 66 literales de trama y **no imprime nada**: no hay ni un caso hostil que
pase sin aseverar. Responde al punto 4 del encargo: ese agujero no existe.

### 1.2 R-01 se ha visto en rojo — comprobado, no razonado

El autor afirma que R-01 falla contra el `Tramas.java` de `f5145ed`. **Confirmado por ejecución.**
`main` (`f5145ed`) define `Tramas.java:24 SONDA_V4 = "@LEERV,BLA,1@"`; `rtv-1.0` define
`Tramas.java:29 SONDA_V4 = "@LEERV,BLA,2@"`. Compilando la aserción de `RupturaRtv10Test.java:57-58`
contra el valor de `main` y ejecutándola con JUnit:

```
org.junit.ComparisonFailure: la sonda de deteccion debe ser de tipo 2 (no escribe el error oculto del
V4.1) expected:<@LEERV,BLA,[2]@> but was:<@LEERV,BLA,[1]@>
```

Es **exactamente** el mensaje que se declaró. Esa prueba separa sondear de alterar la siguiente medida y
ha estado en rojo de verdad.

---

## 2. Los cuatro puntos donde se pidió apretar

### 2.1 ¿Se puede colgar un equipo? — No por ningún camino de la app. La lista blanca es real.

**Hay un solo camino de bytes hacia el equipo.** `EnlaceSerie.enviar(byte[])`
(`EnlaceSerie.java:248-268`) es el único método que escribe en el socket, y su campo `salida` es
`private volatile OutputStream` (`EnlaceSerie.java:53`). Su **único llamador en todo `src/main`** es
`Cliente.java:132`. Verificado dos veces, con `grep` por Bash y con la herramienta Grep sobre
`app/src/main/java` buscando `enviar(`, `getOutputStream`, `write(` y `flush(`: no hay ningún otro.
Tampoco hay ningún envío en un `onDestroy`, un `onPause` ni un cierre: `desconectar()`
(`EnlaceSerie.java:270-276`) sólo cierra el socket.

**La puerta es `Cliente.pedir`** (`Cliente.java:116-124`):

```java
Protocolo proto = Sesion.get().protocolo;
if (proto == null ? !Deteccion.permitida(peticion) : !proto.permitida(peticion)) {
    throw new IllegalArgumentException("peticion no permitida: " + peticion + …);
}
```

La sustitución de la lista negra por la lista por perfil es real: el `git diff main...HEAD` de
`Cliente.java` muestra que antes era `Tramas.peticionPermitida(peticion, version == V36)`.
`Tramas.peticionPermitida(String)` (`Tramas.java:129-131`) **queda como código muerto**: sus únicos
llamadores son `CalculoTest.java:275-279`.

**Una trama de más de 64 bytes hacia un V4 original es imposible por construcción.**
`ProtocoloV4Original.permitida` (`:128-131`) es `Tramas.esLeervValida`, que exige
`matches()` completo contra `@LEERV,(BLA|AMA|VER|ROJ|AZU|NAR),([12])@` (`Tramas.java:34,38-40`): **13
bytes exactos, 12 tramas posibles y ninguna otra.** Ningún `#…#` llega jamás a un V4 original.

> **Contradicción con la premisa del encargo (1).** Se me dijo que el anillo del V4 original «es de 64
> bytes». **No lo es: son 80.**
> `D:\IT\P_RetroVertical_V4.6\01_Firmware\base_v4.1_repositorio\mcc_generated_files\uart1.c:57`
> `#define UART1_RX_BUFFER_SIZE 80`, y `Serial.c:18` `extern uint8_t uart1RxBuffer[80];`. El anillo
> además **da la vuelta** en vez de desbordar: `uart1.c:235-239`
> `uart1RxBuffer[uart1RxHead++] = U1RXB; if(sizeof(uart1RxBuffer) <= uart1RxHead) { uart1RxHead = 0; }`.
> No corrige el veredicto (la app no puede mandar más de 13 bytes a ese firmware), pero la cifra no debe
> seguir circulando.

> **Contradicción con la premisa del encargo (2).** Se me dijo que **«un `<` suelto»** deja sordo al V4
> original. **No hay ningún tratamiento de `<` en ese firmware.** Verificado dos veces sobre
> `base_v4.1_repositorio\Serial.c`: `grep -n "'<'\|\"<\""` no devuelve nada, y el fichero entero contiene
> un solo carácter `<`. Lo que sí deja sordo al equipo es únicamente una `'@'` sin `LEERV`, y el
> mecanismo está en `Serial.c:86-105`:
> ```c
> case ST_INIT_ANAT:
>     if(strstr(bufferRx1, (char*) "@")) {
>         if(strstr(bufferRx1, (char*)"LEERV")) { … stateAnaTrama1 = ST_COLOR_ANAT; }
>     }
>     else { stateAnaTrama1 = ST_ESPERA_ANAT; }
> ```
> El `if` interior **no tiene `else`**: con `'@'` y sin `LEERV` la máquina se queda en `ST_INIT_ANAT`,
> que nunca vuelve a leer el puerto. **Corolario que importa esta noche:** cualquier trama **sin `'@'`**
> cae en el `else` y devuelve la máquina a `ST_ESPERA_ANAT`. Por eso `#V#`, `9` y `6` son **inofensivas**
> para un V4 original, y por eso la detección completa se le puede mandar a V4-B y V4-C sin miedo.

**Un V4 original sí recibe `#V#`, `9` y `6` antes de la sonda** (`Deteccion.java:102,122,131,152`). El
único atajo es el perfil `SOLO_SONDA` de `equipos.csv` (`Deteccion.java:76-94`), y hoy ese asset no
tiene ni una fila. Es seguro por lo anterior, pero conviene decirlo tal cual: **no** es cierto que «se
identifique sin escribirle nada»; se le escriben tres tramas, y son inocuas.

### 2.2 La sonda — el literal de tipo 1 no está en el dex, pero **sí se reconstruye**

**Lo que es cierto.** `Tramas.java:29` fija `SONDA_V4 = "@LEERV,BLA,2@"`, y el literal de tipo 1 **no
está en el APK**. Verificado con dos métodos sobre `classes.dex` y `classes2.dex` extraídos del APK
entregado: (a) `strings` lista `@LEERV,BLA,2@` y no lista ningún `@LEERV,BLA,1@`; (b) `grep -a -F
'@LEERV,BLA,1@'` sobre los dos ficheros binarios → **ausente en ambos**. El rótulo de la prueba 2, que
lo llevaba cableado, ahora sale de la constante (`Pruebas.java:56`), y `RupturaRtv10Test.java:69-88`
(R-01 ter) lo fija barriendo los fuentes.

> **Contradicción con la premisa del encargo (3).** Se me pidió confirmar que «ninguna ruta lo
> reconstruye». **Sí lo reconstruye, y a propósito.** `Tramas.tramaLeerv(char)` (`Tramas.java:43-49`):
> ```java
> return "@LEERV," + COLORES_V4[i % 6] + "," + (i < 6 ? 2 : 1) + "@";
> ```
> Con `k = '7'` → `Fabrica.indice('7') = 6` (`Fabrica.java:16`) → `COLORES_V4[0] = "BLA"` y tipo `1`:
> la cadena resultante **es** `@LEERV,BLA,1@`. Las claves `7`, `8`, `a`, `b`, `c` y `d` miden en tipo 1
> y la propia `RupturaRtv10Test.java:155-162` (R-04) recorre las 12 claves contra un V4 original.
>
> Esto **no es un defecto**: medir en tipo 1 es una función documentada, el fragmento `"@LEERV,"` está en
> el dex y la app lo compone. Y el efecto está gestionado: `DiarioEstadoOculto` (`:29-41`) anota en cada
> medida de otros papeles cuál fue la última de tipo 1, está **cableado de verdad** en el bucle de medida
> (`MedidaActivity.java:280` `String nota = s.diario.anotar(k, v, false);` y `:304` el aviso del
> resumen), se reinicia al conectar (`Sesion.java:114`, `Pruebas.java:282`) y las medidas de tipo 1 no se
> comparan contra el certificado (`MedidaActivity.java:282-284`).
>
> Pero la afirmación «ninguna ruta lo reconstruye» es falsa y conviene no repetirla: **lo que se arregló
> es la sonda, no el tipo 1.** Quien mida una clave 7-d en un V4 original sigue reescribiendo el estado
> oculto y alterando la medida siguiente.

*(Nota de método: estuve a punto de dar `DiarioEstadoOculto` por muerto, porque un `grep` del nombre de
la clase sólo devuelve `Sesion.java:36`. Está vivo: se usa por la referencia `s.diario`. Un negativo
comprobado con una sola búsqueda no es un negativo.)*

### 2.3 Detección — correcta, y sin cambios para SLV-002

- **`#V,4.6,…#` no cae en DESCONOCIDA**: `Deteccion.java:108-112`
  `if ("3.6".equals(iv.version) || "4.6".equals(iv.version)) { … r.firmware = v46 ? F46 : F36; }`.
  Cubierto por R-02 (`RupturaRtv10Test.java:119-127`), que asevera.
- **Un V3 2020 / V3.6 se detecta exactamente igual que en `main`**: mismo orden (`#V#` → `#GC#`/`#GN#`
  si es 3.6/4.6 → `9` → `6` → sonda) y **mismos timeouts** (2500 ms en los tres casos;
  `Deteccion.java:21,102,122,131` con `Ops.TIMEOUT_ADMIN_MS`/`TIMEOUT_MEDIDA_MS` = los mismos valores que
  `Cliente.java:28-29`, frente a `main:Pruebas.java:256,302,310`). A un V3 la sonda **nunca llega**,
  porque `9` responde antes.
- **La medida de SLV-002 no puede llevar una `'e'` a un equipo que no la tiene**:
  `ProtocoloV2020.permitida` (`ProtocoloV2020.java:26-32`) admite sólo los 12 códigos y `'9'` —
  `'e'` fuera. Es **más estricta** que la guarda de `main`.

### 2.4 El botón de calibrar la V4.6 — está cerrado de verdad

`ProtocoloV46.calibra()` (`:113-116`) es `return calibracion && mideBanco();`, y `calibracion` sólo
vale `true` por el constructor de tres argumentos (`:28-31`). En producción la V4.6 se construye en
**dos** sitios y los dos usan el constructor sin argumentos → `calibracion = false`:
`Deteccion.java:112` y `FlujoCalibracion.java:269`. Las únicas llamadas al constructor de tres
argumentos están en pruebas (`FlujoCalibracionTest.java:1396`, `RupturaRtv10Test.java:355`).
Aguas arriba, los botones lo consultan: `ConexionActivity.java:152,157,158` y `AdminActivity.java:305`.
**Confirmado: con una V4.6 detectada, "4. Calibrar este equipo" y "Modo administrador" salen
deshabilitados y con el motivo escrito.**

---

## 3. Hallazgos

### ALTOS

**A-1 · El OSCURO de la cola V4.6 se mide con dos luces y el resto de la app sólo conoce uno; el filtro
de colocación de los patrones de luz alta se compara contra el oscuro de luz baja.**
La cola lo hace a propósito: `assets/cola_banco_representativo_v46.csv:4-5` (órdenes 3 y 4) traen
`codigo_equipo` `1` y `3` con las notas «*oscuro con la luz alta (#X,1#; AMA, BLA, ROJ, NAR)*» y
«*…luz baja (#X,3#; AZU, VER)*»; ídem órdenes 30-31 de la sesión 2. Pero las dos series se graban con el
**mismo nombre de patrón** (`BancoActivity.java:661`
`this.nombre = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;`) y se recuperan sin
distinguir la clave (`Campana.java:378-381` `xOscuro()` → `elegida(OSCURO.nombre)`, que devuelve **la
última aceptada**, es decir la de luz baja). Ese valor es el único filtro de colocación que queda:
`BancoActivity.java:776-779`
```java
if (!"OSCURO".equals(p.tipo)) {
    double xo = m.campana.xOscuro();
    ausente = Colocacion.patronAusente(m.nombre, xa, p.xEsperada, p.toleranciaX(),
            Double.isNaN(xo) ? Asistente.X_OSCURO : xo);
}
```
y, como la cola V4.6 deja `x_esperada` **vacía en todas sus filas**, `toleranciaX()` devuelve NaN
(`BancoCola.java:136-138`) y `Colocacion.java:38-44` cae siempre a `lim = xOscuro + MARGEN_OSCURO`
(`MARGEN_OSCURO = 40`, `Colocacion.java:23`). Resultado: **la guarda que detecta un patrón mal apoyado o
ausente queda comparada contra un cero óptico de otra luz, y por tanto es demasiado permisiva para los
patrones de luz alta (blanco, amarillo, rojo, naranja).** El respaldo cuando no hay oscuro es peor:
`Asistente.java:218 public static volatile double X_OSCURO = 575;`, una cifra de la V3.6.
Mismo problema, sin filtro por clave, en el ancla de calibración (`Anclas.java:51-73`: recoge los cuatro
OSCURO de la sesión, trata la diferencia entre luces como **deriva** y promedia dos luces distintas);
ahí está mitigado porque la calibración V4.6 está apagada (`ProtocoloV46.java:113-116`). **El camino de
`xOscuro()` en el banco está vivo hoy.**

**A-2 · Cambiar de cola deshace un "rehacer" y resucita la serie anterior.**
`Campana.java:537-539` vacía `pasos`, `seriePaso` e `historialPasos` al cambiar de cola. Con los estados
borrados, `BancoPrevio.java:139` deja de saltar el paso (`est.containsKey(p.orden)` pasa a false) y
`:150` consulta `c.elegida(pat)`, cuyo fallback (`Campana.java:735-741`) devuelve **la anterior aceptada
y no anulada** del mismo patrón. `BancoPrevio.java:158` la escribe como `HECHO`. El "rehacer" queda
deshecho y el patrón no se vuelve a medir. Sólo ocurre si el patrón tenía más de una serie aceptada.
La única prueba del cambio de cola (`Version3616Test.java:252-269`) usa una sola serie y ningún rehacer.

**A-3 · El botón de cola nunca se deshabilita, y el estado se persiste por número de orden.**
`BancoActivity.anotar` → `campana.anotarPaso(p.orden, …)` (`BancoActivity.java:546-552`,
`Campana.java:563-569`) guarda por **orden**, sin referencia a la cola —y el propio comentario de
`Campana.java:525-527` avisa: «*Los ordenes de una cola no valen en otra*». `btnBanco` se crea en
`BancoActivity.java:60` y sus otras dos apariciones son `:93` y `:437`: **ningún `setEnabled`**. Todos
los `setEnabled` del fichero (`:396,397,433,434,513,515,517,520`) son de otros botones y sí respetan
`libre = !ocupado && enCurso == null` (`:512`). `elegirBanco()` (`:284-322`) tampoco mira `enCurso`.
Contraejemplo contado sobre los assets: el **orden 8** es `PATRON P51` en
`cola_banco_representativo_v2.csv` y `PATRON P3` en `cola_banco_P1-P132.csv`. Cambiar de cola con una
medida en vuelo escribe `HECHO` en el orden 8 de la cola nueva, con la serie del patrón de la vieja
(`BancoActivity.java:934,964,1039,1061,1106`). Nada aguas abajo compara el patrón de la serie con el del
paso.

**A-4 · Fecha de calibración huérfana si el enlace cae entre `#SC` y `#GC#` — y no hay try/catch.**
`FlujoCalibracion.aceptar()` escribe la fecha en `:1484` (`ops.escribir("#SC," + hoy + "#")`) y la
relee en `:1487`. **No existe ningún `try` en todo el método** (`:1442-1505`; comprobado con `awk` sobre
ese rango: cero líneas con `try` o `catch`). Una `IOException` en `:1487` atraviesa el método hasta
`CalibrarActivity.java:417-418`. Entre `:1484` y `:1487` **no hay ningún `acta.dato(...)`**, y el diario
del acta se escribe en vivo (`Campanas.java:340-353`), así que del intento no queda rastro en disco.
El caso serio no es «fecha sin acta» (se recupera reintentando: `:1479-1482` detecta `hoy.equals(antes)`)
sino que tras el fallo el operador pulse **Rechazar** (`:1511-1563`), que restaura cada curva
(`:1527,:1540`) **pero no toca la fecha**: `#SC` se escribe en un solo sitio de toda la app
(`grep -rn 'escribir("#SC'` → única coincidencia `FlujoCalibracion.java:1484`) y nada la borra.
Queda un equipo con **curvas viejas y fecha de hoy**. El código conoce la regla que aquí se rompe
(`Acta.java:23` y `:727`), pero sólo cubre el `#SC` que responde mal, no el que no llega a responder.

**A-5 · Una aserción de seguridad de R-03 es código muerto, y su comentario es falso.**
`RupturaRtv10Test.java:136-142`:
```java
Deteccion.Resultado v4 = Deteccion.detectar(new EquipoSimuladoV4(), null, null, false);
…
// y el V4 original se identifica sin haber enviado jamas 'e' ni una '@' que no sea la sonda
for (String t : new EquipoSimuladoV4().recibidas) {
    fail("no deberia haber tramas aqui: " + t);
}
```
El bucle recorre una **instancia nueva y vacía**: el canal de `:136` no se guarda en ninguna variable
(`v4` es el `Resultado`). El bucle nunca itera y el `fail` nunca se alcanza. Y si se corrigiera usando el
canal correcto, **la prueba fallaría**, porque `EquipoSimuladoV4.java:85` (`recibidas.add(t)`) registra
todo y la detección le manda `#V#`, `9`, `6` y la sonda. Es decir: la prueba que parece garantizar la
propiedad más importante del encargo no garantiza nada, y además afirma algo que no es cierto. La
comprobación real sí existe, pero en otro sitio: `RtvUnicaTest.java:143-148` (`tU06`), que conserva el
canal. **Gravedad alta por lo que induce a creer, no por el riesgo directo.**

### MEDIOS

**M-1 · `REPRESENTATIVO_V46` hereda la donación por equivalencia, y en la V4.6 puede donar entre
firmwares distintos.** `BancoPrevio.java:145` filtra los equivalentes con
`!candidatos.contains(e) && !enCola.contains(e) && !excluidos.contains(e)`, y `excluidos`
(`:137` → `Decisiones.java:216-228`) sólo trae `P81` (`assets/decisiones.csv:11`, única fila con
`EXCLUYE`; negativo verificado dos veces). La línea es **byte a byte idéntica a `main`**
(`git show main:…/BancoPrevio.java`; el diff del fichero es sólo el renombrado
`Protocolo.`→`ProtocoloDisparos.`) y **la cola V4.6 pasa por ahí sin filtro alguno**
(`Base.java:195,202`, llamado siempre desde `BancoActivity.java:442`). Alcance contado cruzando la cola
con `grupos_patrones_equivalentes.csv`: de **58 pasos PATRON, 24 admiten donante** (15 AJUSTE, 3
RE-MEDIDA, 6 VERIFICACION). Agravante propio de la V4.6: `Campana.elegida` (`:727-742`) **no mira
`s.firmware`**, así que una serie medida con la V3.6 (x ≈ 600-4300) puede dar por hecho un paso de la
cola V4.6 (x ≈ 0-4095, **otra magnitud**, como dice el propio asset y `BancoCola.java:48`).
*Matiz frente a lo que se me dijo:* «*pasos de AJUSTE marcados HECHO **sin serie propia***» es engañoso.
El paso queda con la serie del equivalente, **anotada y trazable** (`BancoPrevio.java:158-159` escribe
el id y el grupo en la nota, que va al diario), y hay dos guardas omitidas en el enunciado
(`:139` y `:154`). Lo grave no es la falta de serie: es que la serie es de **otra lámina** y puede ser de
**otro firmware**.

**M-2 · Al conectar una V4.6, la app cambia de cola en silencio y borra los estados del banco.**
`BancoActivity.java:410-416`: si el protocolo es F46 se llama `cambiarCola(def, false)` **sin preguntar**,
y eso vacía `pasos`/`seriePaso`/`historialPasos` (`Campana.java:537-539`). El diálogo que la 3.6.17 añadió
para esto queda inalcanzable: `empezado` se calcula en `:417`, **después** del cambio, y la condición de
`:419` ya es falsa. Se pierden sin aviso los estados `SALTADO` y `REHACER` y el vínculo paso↔serie; los
`HECHO` se recuperan en parte por el camino de M-1.

**M-3 · El botón "Línea base forzando V3 2020" sustituye un perfil ya detectado, sin comprobarlo.**
`PruebasActivity.java:275` habilita `btnLineaBaseForzada` sólo con `!curso && !lineaBaseEnCurso &&
estaConectado()`: **no consulta el protocolo**. `lineaBaseForzada()` (`:118-132`) tampoco. Al aceptar,
`:150-151` hace `s.protocolo = new ProtocoloV2020(); s.detectado = true;`, **pisando una V4.6 o un V4
original correctamente detectados**, y el resto de la sesión los trata como V3 2020. Como
`ProtocoloV2020 extends ProtocoloV36` (`ProtocoloV2020.java:8`) hereda `daX()` y `mideBanco()` en `true`
(`ProtocoloV36.java:63-65,114-116`), «Tomar muestras» queda habilitado **con la cola de la V3.6**. No hay
riesgo para el equipo (los 13 bytes que emite no llevan `'@'`), y calibrar sigue cerrado porque
`ProtocoloV2020.calibra()` está sobrescrito a `false` (`:45-48`), pero el banco resultante llevaría la
cola equivocada y la sesión quedaría marcada como «detectada».

**M-4 · "Línea base" no está gateada cuando el firmware ya se detectó como V4.6 o V4 original.**
La guarda de `PruebasActivity.java:158` (`if (!s.versionMedible() || !s.protocolo.permitida("1"))`) vive
dentro de la rama `else if (s.protocolo == null)` de `:154`: sólo actúa si **no** se ha detectado nada.
Con una V4.6 ya detectada se cae a `:182` (`LineaBase.codigos`), que manda bytes sueltos
(`LineaBase.java:44-45`) y `"9"` (`:71`). La lista blanca los rechaza antes de tocar el cable y la
`IllegalArgumentException` se captura en `:184`, así que **falla seguro**, pero el operador ve «Interrumpida:
IllegalArgumentException…» en vez del aviso claro que sí existe para el otro camino (`:168-170`).

**M-5 · `#GB#`: no se comprueba el rango 0-100 que el propio contrato declara.**
`Tramas.parsearGB` (`:96-106`) sólo hace `Integer.parseInt`; `Bateria.java:72-79` sólo bloquea con
`n == 0` y avisa con `n < AVISO_PCT` (`=20`, `:66`). Un `#GB,250#` se muestra como «Batería 250 %» sin
aviso ni bloqueo; un `#GB,-5#` avisa pero **no** bloquea. El contrato dice 0-100
(`assets/firmwares.csv:3`, `Bateria.java:67`). El camino de "sin respuesta" sí está bien resuelto
(`Ops.java:138` → `interpretarPorcentaje(null,…)` → bloquea). Además, el paso BATERIA del banco queda
`HECHO` aunque no haya habido respuesta (`BancoActivity.java:618`).

**M-6 · Los límites de x declarados para la V4.6 son dato muerto.**
`firmwares.csv:9` fija `0,4095` para F46, pero `PerfilFirmware.xMin/xMax` (`:21-22`) **no los lee nadie
en producción** (verificado dos veces; los únicos usos son la declaración, un parámetro homónimo en
`Asistente.java:179` y una aserción en `RtvUnicaTest.java:226`). `Tramas.parsearX` (`:83-93`) sólo
rechaza NaN e infinito (`num()`, `:312-318`): **acepta negativos y valores por encima de 4095**. Y un
disparo inválido se descarta **en silencio para el operador** (`BancoActivity.java:791-793`, `continue`
con nota sólo en el registro): si fallan algunos, la colocación se cierra con menos de M disparos sin
decirlo en pantalla.

**M-7 · Un `#V,4.6,…#` con un número de campos distinto de 4 ó 5 hace que la app envíe `'9'` y `'6'` a
una V4.6.** `Tramas.parsearVersion` (`:245`) devuelve `null` si `c.length != 4 && c.length != 5`; con
`iv == null` la detección continúa a `Deteccion.java:122` y `:131`. Esos bytes están en
`Deteccion.PERMITIDAS` (`:24`), así que `Cliente` los deja pasar, pero `ProtocoloV46.permitida`
(`:43-51`) los prohíbe expresamente para ese firmware. Contradicción interna del diseño. R-02 sólo cubre
el formato de 5 campos que produce `EquipoSimuladoV46.java:81`.

**M-8 · Se perdió la única prueba JVM que cubría la decisión "nunca `'e'` a quien no la tiene".**
En `main` la guarda vivía en `Tramas` (Java puro) y `CalculoTest` la ejecutaba de verdad. En `rtv-1.0` la
decisión está en `Cliente.java:121`, que depende de `EnlaceSerie`/Android y **ninguna prueba JVM
ejecuta**. Las pruebas llaman a `Protocolo.permitida` suelto (`CalculoTest.java:281-284`,
`RtvUnicaTest.java:284-285`, `RupturaRtv10Test.java:189-197`). La decisión sigue siendo correcta y más
estricta; lo que se perdió es su cobertura.

**M-9 · La detección de un V3.6 es ahora ~3× más lenta.**
`main:Pruebas.java:260` fijaba `s.version = V36` **antes** de `#GC#` (`:281`), con lo que
`corta = true` → 150 ms entre almohadillas. En `rtv-1.0`, `Pruebas.java:278` pone `s.protocolo = null` y
sólo lo asigna en `:281`, tras terminar la detección: durante toda ella `proto == null`, así que
`Cliente.java:127` da `corta = false` → 1500 ms + 600 ms entre `#V#`→`#GC#` y `#GC#`→`#GN#`. Es un cambio
hacia el lado seguro, pero es observable en el arranque automático de las pruebas.

**M-10 · El informe de la prueba 2 pierde el desglose de la máscara, y ese texto va al expediente.**
`main:Pruebas.java:266-293` imprimía los códigos ajustados, «temperatura ajustada / de fábrica» y la
etiqueta de variante («firmware 3.6.2», «firmware 3.6.1 (no tiene serie…)»). `Deteccion.java:113-114`
imprime sólo versión, fecha, marca y la máscara en hexadecimal. Verificado dos veces que el desglose no
se imprime en ningún otro sitio (`codigoAjustado`/`temperaturaAjustada` sólo aparecen en
`Tramas.java:230,234`, `FlujoCalibracion.java:1084` y `CalculoTest.java:355-358`). No es cosmético de
pantalla: `Pruebas.java:252-260` vuelca el detalle a `Campanas.anotarPruebas`, que acompaña al acta.

### BAJOS

**B-1 · El acta atribuye a Diego por su nombre una firma que pudo poner cualquiera con el PIN.**
*(Con la decisión de Diego —«si tienen pass de admin firman, ya no lo compliques»— la ausencia de la
fila `FRASE-DIEGO` en `decisiones.csv` **ya no es un defecto** y no hay que añadirla. Queda sólo esto, de
redacción.)* `FlujoCalibracion.java:1611` devuelve el literal `"Diego (PIN del equipo, verificado con
#L)"`, y `:1588` lo incrusta como `"RECHAZADA SIN RESTAURAR, firmado por " + firmante.trim()`, con el
mismo texto en el mensaje de `:1593`. El acta archivada dice entonces, literalmente, «*firmado por Diego
(PIN del equipo, verificado con #L)*» aunque quien tecleó el PIN fuese el operador.
**Texto propuesto** (no lo he cambiado, sólo lo señalo): que `:1611` devuelva
`"el PIN de admin del equipo (verificado con #L)"` y que `:1588` y `:1593` digan **«firmado con »** en
lugar de «firmado por », de modo que el acta lea «*RECHAZADA SIN RESTAURAR, firmado con el PIN de admin
del equipo (verificado con #L)*». Ojo: `FlujoCalibracionTest.java:1197` fija la cadena actual y habría
que actualizarlo.

**B-2 · `claveX()` cae a `'1'` en silencio.** `BancoCola.java:130-133`:
`return codigo.length() == 1 && Fabrica.esCodigo(codigo.charAt(0)) ? codigo.charAt(0) : '1';`.
*Contradicción con lo que se me pidió verificar:* devuelve un `char` primitivo, así que **no puede ser
null ni indefinido** y no hay ningún uso sin comprobar. El defecto real es que el fallback es mudo: una
fila con `codigo_equipo` inválido mediría con la luz blanca sin decirlo, y `BancoCola.cargar`
(`:195-231`) no valida esa columna. Hoy el asset no lo dispara (lo fija `RupturaRtv10Test.java:271-277`).

**B-3 · `SLV-002-2026` cabe: la premisa del truncamiento es falsa.** Conté los caracteres dos veces
(`printf | wc -c` y `awk length`): **12**, correcto. Pero el límite es `> 12`, no `>= 12`
(`Calibracion.java:64-71`), y es un **rechazo con motivo, no un recorte**. No hay ningún `substring`
sobre la serie, y no hay `android:maxLength` porque **no hay layouts XML** (negativo verificado dos veces).
El firmware coincide: `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:490` `#define SERIE_MAX 12`,
con byte de longitud (`:480`) y `if (n < 1 || n > SERIE_MAX …)` en `:751`. Lo fijan
`Version367Test.java:67-68` y `FlujoCalibracionTest.java:1103`. Queda sólo la observación de diseño: el
margen es cero, y un `SLV-002-2026B` sería rechazado (con mensaje claro, sin equipo inutilizable).

**B-4 · `exportar()` revienta si el índice quedó truncado sin salto de línea.**
`Campanas.java:622-623`: si `indice_<clave>.txt` empieza por `"zip "` y no contiene `'\n'`,
`indexOf('\n')` devuelve -1 y `substring(0,-1)` lanza `StringIndexOutOfBoundsException`. Sólo tras un
corte a mitad de escritura.

**B-5 · Renombrado ciego en comentarios.** Un `sed` de `Protocolo`→`ProtocoloDisparos` pasó por encima de
la prosa: `Campana.java:417` («*ProtocoloDisparos de disparos de la campana*»),
`ProtocoloDisparos.java:7` y `BancoActivity.java:653`. Sólo comentarios; ningún código afectado
(verificado: el único cambio de `Campana.java` frente a `main` es esa línea de javadoc).

**B-6 · `PerfilFirmware.porDefecto` contradice `firmwares.csv` para F2020** (600 en `:44` frente a 500 en
`firmwares.csv:7`) y documenta `bateria_unidad` como `"n9"`/`"por_fijar"` (`:14`) cuando el CSV usa `pct`
(`:9`). Sin efecto hoy, porque esos campos no gobiernan nada (M-6).

---

## 4. Estado de los defectos heredados, uno por uno

| # | Enunciado heredado | Veredicto | Dónde |
| :-- | :--- | :--- | :--- |
| 1 | `BancoPrevio.java:145` filtra con `!enCola && !excluidos`; `excluidos` sólo trae P81 | **CONFIRMADO** | `BancoPrevio.java:137,145`; `decisiones.csv:11` |
| 1b | Esa línea es idéntica a `main` | **CONFIRMADO** (byte a byte) | `git show main:…/BancoPrevio.java` |
| 1c | `REPRESENTATIVO_V46` hereda el defecto | **CONFIRMADO**; alcance 24 de 58 pasos PATRON (15 AJUSTE, 3 RE-MEDIDA, 6 VERIFICACION) | `Base.java:195,202`; `BancoActivity.java:442` |
| 1d | «pasos de AJUSTE marcados HECHO **sin serie propia**» | **PARCIALMENTE FALSO**: quedan HECHO **con la serie del equivalente**, anotada con id y grupo en el diario, y con dos guardas omitidas en el enunciado | `BancoPrevio.java:139,154,158-159` |
| 2 | No existe la fila `FRASE-DIEGO`; la firma cae al PIN | **CIERTO, pero ya no es defecto** (decisión de Diego). Queda sólo B-1, de redacción. Y es **FALSO** que el acta diga «firmado por Diego» a secas: dice «firmado por Diego (PIN del equipo, verificado con #L)» | `decisiones.csv:7-8`; `FlujoCalibracion.java:1588,1606-1611` |
| 3 | Fecha de calibración huérfana entre `#SC` y `#GC#` | **CONFIRMADO — ALTO** (A-4). No hay ningún `try` en `aceptar()`; el caso peor es fecha de hoy + curvas restauradas tras Rechazar | `FlujoCalibracion.java:1484-1492,1511-1563` |
| 4 | Cambiar de cola deshace un "rehacer" y resucita la serie anterior | **CONFIRMADO — ALTO** (A-2), condicionado a que el patrón tenga otra serie aceptada | `Campana.java:537-539,735-741`; `BancoPrevio.java:150,158` |
| 4b | Con una medida en curso puede marcar HECHO un paso de otro patrón | **CONFIRMADO — ALTO** (A-3), pero **el mecanismo del enunciado es falso**: la medida en vuelo sí conserva su paso (`BancoActivity.java:565,953`). Lo que falla es que el estado se persiste por **número de orden**, y `btnBanco` nunca se deshabilita | `Campana.java:525-527,563-569`; `BancoActivity.java:60,437,546-552` |
| 5 | `SLV-002-2026` mide 12, el límite; un truncamiento dejaría el equipo sin calibrar | **FALSO** (B-3): son 12, el límite es `> 12`, y es rechazo con motivo, no truncamiento | `Calibracion.java:64-71`; `calibracion_v36.c:490,751` |

---

## 5. Lo que hay que corregir en lo que se me dijo

1. El anillo del V4 original **no es de 64 bytes: son 80** (`uart1.c:57`), y da la vuelta en vez de
   desbordar (`uart1.c:235-239`).
2. **Un `'<'` suelto no deja sordo al V4 original.** Ese carácter no se trata en su `Serial.c`. Lo único
   que lo deja sordo es una `'@'` sin `LEERV` (`Serial.c:86-105`).
3. El literal de tipo 1 no está en el dex, pero **sí se reconstruye**: `Tramas.tramaLeerv('7')` produce
   `@LEERV,BLA,1@` (`Tramas.java:43-49`). Es intencionado; lo que se arregló es la sonda, no el tipo 1.
4. El V4 original **no se identifica «sin escribirle nada»**: recibe `#V#`, `9` y `6` antes de la sonda
   (`Deteccion.java:102,122,131`). Son inofensivas, pero conviene decirlo bien.
5. `claveX()` **no puede devolver un valor nulo ni indefinido** (`BancoCola.java:130-133`).
6. «Pasos de AJUSTE HECHO **sin serie propia**» es inexacto: llevan la serie del equivalente, trazada.
7. El truncamiento de `SLV-002-2026` **no existe**.
8. **SLV-002 no lleva firmware «V3 2020»**: el árbol dice que lleva **3.6.2**, una variante de la V3.6
   (`RETOMAR.md:10`, `README.md:27`), y sus simuladores de prueba lo tratan así
   (`RtvUnicaTest.java:71-77`, `EquipoSimulado.java:10-12`). Es decir, su campaña recorre el camino
   **F36**, no el F2020. Hay una fuente que dice lo contrario (`03_App_Movil/RetroV36/README.md:6`), así
   que **queda como contradicción abierta: se cierra leyendo `#V#` del equipo, no discutiéndolo.**
9. El «299 en verde» es cierto, pero **sólo 274 aseveran**.

---

## 6. Condiciones de uso en campo

Para leer con el equipo delante. Si alguna no se puede cumplir, **pare y avise**.

### Antes de tocar nada

1. **Lo primero de todo, siempre: "1. Pruebas" (detección).** Hasta que la detección termine, la app no
   sabe con qué equipo habla y sólo deja salir seis tramas. Si empieza por "Tomar muestras" o por
   "Calibrar", verá errores raros y perderá tiempo. Compruebe en pantalla que dice **V4.6** (SLV-003) o
   **V3.6** (SLV-002) antes de seguir.
2. **No toque nunca el botón "Línea base forzando V3 2020 sin e (sin detectar)".** No está bloqueado y,
   si lo pulsa con un equipo ya detectado, la app pasa a tratarlo como si fuera un V3 de 2020 y le
   ofrecerá el banco con la cola equivocada. No rompe el equipo; arruina la sesión. Si lo pulsa por
   error: desconecte, vuelva a conectar y repita la detección.
3. **"Línea base" (la normal) no sirve con un V4.6 ni con un V4 original.** Si la pulsa, saldrá
   «Interrumpida: IllegalArgumentException…». No es una avería: es la lista blanca haciendo su trabajo.
   Ignórelo y use "2. Medida de patrones".

### Con un V4 original (V4-B, V4-C) — medir sin grabarles nada

4. **Puede conectarlos con confianza.** La app sólo les manda `#V#`, `9`, `6` y `@LEERV,BLA,2@`, y
   ninguna los deja sordos. La trama que sí los dejaría sordos —una `@` sin `LEERV`— la app no puede
   emitirla por ningún camino.
5. **Ojo con las claves 7, 8, a, b, c y d: son de tipo 1 y alteran la medida siguiente.** No es un fallo
   de la app, es cómo es ese firmware. Si va a medir claves de tipo 1 y de otros papeles en la misma
   sesión, **mida primero todas las de otros papeles (1-6)** y deje las de tipo 1 para el final. La app
   se lo anota en cada línea y le avisa al terminar; léalo.
6. **Ni banco ni calibración con estos equipos**, y los botones se lo dirán. Es correcto: sin x no hay
   banco.

### Con SLV-003-2026 recién grabado con la V4.6 — el banco de esta noche

7. **Abra la campaña DESPUÉS de conectar y detectar, no antes.** Al conectar una V4.6 la app cambia a su
   cola sin preguntar y **borra los estados de paso** de lo que hubiera. Si ya había pasos hechos, se
   pierden los SALTADO y los REHACER.
8. **Elija la cola una sola vez y no vuelva a tocar el botón de banco durante la sesión.** Ese botón no
   se bloquea nunca, ni siquiera con una medida en curso, y cambiar de cola con una medida en vuelo puede
   dar por medido un patrón con la lectura de otro. Es el riesgo más fácil de provocar sin querer.
9. **El aviso de "patrón ausente / mal apoyado" es poco fiable en esta cola.** La cola mide dos oscuros
   (luz alta y luz baja) y la app sólo se queda con el último, el de luz baja; los patrones claros
   (blanco, amarillo, rojo, naranja) se comparan contra ese. **Compruebe usted a ojo, en cada colocación,
   que el patrón está puesto y bien apoyado.** No delegue eso en la app.
10. **Empiece con una campaña nueva y vacía para este equipo.** Si reabre una campaña que ya tenga series
    de otro equipo o de la V3.6, la app puede dar pasos por hechos con medidas ajenas (M-1). En un equipo
    nuevo, sin series previas, ese riesgo no existe.
11. **La batería: no se fíe del número.** La app no comprueba que esté entre 0 y 100 %. Si ve un valor
    absurdo, tómelo como "no sé", no como bueno. Sólo bloquea escrituras con 0 % o sin respuesta.
12. **Cuente los disparos.** Si un disparo no da lectura, la app lo descarta **sin decírselo en
    pantalla**. Si una colocación termina antes de lo que esperaba, repítala.
13. **No use "Calibrar" con la V4.6.** Está deshabilitado y así debe quedarse; los coeficientes de este
    equipo van al `.hex` por ICSP, no por la app. Si en algún momento apareciera habilitado, **pare**: es
    señal de que la sesión ya no cree estar hablando con una V4.6 (véase el punto 2).
14. **Al terminar, guarde y comparta el ZIP antes de desconectar.** Y quédese con el **diario**, no sólo
    con `campana.csv`: el `campana.csv` **no lleva columna de orden ni de paso**, así que por sí solo no
    reconstruye la cola. El diario sí, y lleva la clave de luz de cada serie en el campo `codigo`.

### Con SLV-002 (Coviandina), que ya tiene acta

15. **No hay regresión funcional respecto de la 3.6.17.** Mismas tramas, mismos timeouts, mismas
    fórmulas; y la lista blanca es **más estricta** que antes: ahora es imposible que le llegue una `'e'`
    (`ProtocoloV2020.permitida`, `ProtocoloV36`), que fue lo que lo dejó mudo el 18-sep.
16. **Pero antes de medir nada, confirme el firmware en pantalla.** El árbol dice que lleva **3.6.2**, no
    un V3 de 2020, y la documentación se contradice. Lo que diga `#V#` en la detección manda.
17. **La detección tardará más que antes** (unos segundos de más entre `#V#`, `#GC#` y `#GN#`). Es
    normal, no es que se haya colgado.
18. **El informe de las pruebas ya no le dirá qué códigos están ajustados ni si la temperatura lo está.**
    Si necesita ese desglose para el expediente, sáquelo de las actas anteriores.
19. **Si va a recalibrarlo y el enlace se cae mientras se graba la fecha (entre `#SC` y `#GC#`):
    NO pulse "Rechazar".** Reconecte y vuelva a pulsar "Aceptar": la app detecta que la fecha ya está
    puesta y cierra el acta bien. Si pulsa "Rechazar", el equipo se queda con las **curvas viejas y la
    fecha de hoy**, es decir, presentándose como calibrado hoy sin estarlo. Si ya ocurrió, **avise antes
    de entregar el equipo**.
20. **Si "rehace" un patrón, no cambie de cola después.** Cambiar de cola deshace el rehacer y resucita
    la medida anterior, sin avisar.

### Siempre

21. **Nada de esto se ha probado contra un equipo físico ni en un teléfono.** La primera sesión con la
    rc2 es, de hecho, la primera prueba de campo. Vaya despacio, mire el registro y, ante cualquier cosa
    que no cuadre, pare y anote la hora: el registro de tramas guarda todo lo enviado y recibido.
