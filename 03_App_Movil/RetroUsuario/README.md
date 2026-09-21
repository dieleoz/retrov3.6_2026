# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus 126 tests JVM (checkout limpio). Nada probado contra un
equipo ni un teléfono** (CLAUDE.md, cabecera). Cierra las condiciones de arquitecto-iot y qa-istqb
sobre la 0.3.2, registradas en `05_Documentacion/REVISIONES-Apps-V3.6.md:33-37` (ambas APTO CON
CONDICIONES): **arq ALTO** (`EnlaceBluetooth.vivo()` ya no es sólo `socket.isConnected()` — un
equipo caído se reutilizaba como si siguiera vivo, "no compatible" en bucle; ahora
`dominio.EstadoEnlace` lleva una bandera "caído" aparte, y una detección sobre un enlace REUTILIZADO
que no recibe nada a `#V#` se reintenta UNA vez con conexión nueva antes de concluir "no
compatible"), **arq M-1** (`detectando` en `SesionHolder`, no en un campo de `MainActivity`: una
Activity recreada a mitad de una detección restaura la interfaz, no arranca "en blanco"), **arq M-2**
(salir con Atrás desde la pantalla principal libera el equipo — cierra el enlace y limpia la sesión —
para que otro teléfono pueda conectar), **arq B-3** (si la Activity terminó de verdad mientras
`connect()` estaba en curso, el hilo cierra ese socket en vez de dejarlo huérfano en `SesionHolder`),
**arq B-1** (`fuente.md5` cubre también `build.gradle`/`settings.gradle`/`gradle.properties` de la
raíz de este proyecto, no sólo `app/build.gradle`), **QA D-1** (`PermisoUbicacion.java` citaba una
sección de la SPEC sin `archivo:línea`; ahora cita `SPEC-App-Usuario-V3.6.md:608`) y **QA D-2**
(pruebas que citan una condición de arquitecto/QA como fuente citan
`05_Documentacion/REVISIONES-Apps-V3.6.md` con su línea; recuento requisito/comportamiento honesto
abajo). Además, B-2 (QA): `GestorEnlaceTest` tenía 3 pares de métodos que repetían literalmente los
mismos tres argumentos bajo un nombre de escenario distinto — deduplicados. Las condiciones de la
0.3.0/0.3.1 (QA-1 a QA-10, arq C1, C2, B-1 a B-5 de esas vueltas) siguen cerradas, sin repetirse
aquí. Incremento 1 "Medir y exportar" (`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0) completo;
el incremento 2 "Señal a señal" no está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 6`, `versionName "0.3.3"`.
- `minSdk 24`, `targetSdk 30`, `compileSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`,
  `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` (arq C2: pedido en tiempo de ejecución al entrar a
  medir, una vez por proceso — `MedirActivity`/`dominio.PermisoUbicacion`; se conceda o no, se mide
  igual, `gps_estado = sin_posicion` si no hay posición, RF-USR-15/T-USR-23),
  `WRITE_EXTERNAL_STORAGE` (≤ API 28, pedido en tiempo de ejecución, A4). API 29+ exporta por
  `MediaStore.Downloads` (A4), sin ese permiso.
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`. Catálogo de colores: `08_Senales/senales.csv`.

## Compilar

Misma cadena que `03_App_Movil/RetroV36` (CLAUDE.md §8): JDK 11 Temurin, Gradle 6.5, AGP 4.1.1.

```bash
export JAVA_HOME="D:/@Proyect/Baliza/7 sw apk/jdk-11/jdk-11.0.24+8"
./gradlew clean assembleDebug --offline   # app/build/outputs/apk/debug/app-debug.apk
```

## Estructura del código

- `dominio/` (**Java puro, sin Android**, probado en la JVM): detección y sonda con `exigir_362`
  configurable (RF-USR-01/02), `DetectorEquipo.ResultadoDeteccion.sinRespuesta()` distingue silencio
  real (nada llegó) de una trama que sí llegó pero no es compatible (arq ALTO, la usa
  `GestorEnlace.debeReintentarConexionNueva`), caso mixto que conserva lo que sí contestó (QA-2); silencio total en
  `#GN#`/`#GC#` ofrece reintentar con cualquier valor de `exigir_362` (arq B-4), y `Sonda362.vacio()`
  da el resultado degradado tras agotar los reintentos con `exigir_362 = false`; `SondaReintentable`
  (reintento de la sonda operador-driven, hasta 2, sin reenviar `#V#`, QA-1); `DeteccionYSonda`
  (detección + primera sonda con la pausa de 150 ms entre las dos, QA-3); `GestorEnlace` (arq C1:
  decide reutilizar/cerrar/conectar un enlace anterior — sólo se reutiliza con la misma MAC y vivo —;
  arq ALTO: `debeReintentarConexionNueva` decide si, tras un silencio a `#V#` sobre un enlace
  REUTILIZADO, toca cerrar y reintentar con una conexión nueva antes de concluir "no compatible" —
  "UNA vez" lo impone el propio parámetro `sobreEnlaceReutilizado`, sin contador aparte);
  `EstadoEnlace` (arq ALTO: la bandera "caído" de un enlace, separada de `socket.isConnected()`,
  para poder probarla en la JVM sin `BluetoothSocket`); `PermisoUbicacion` (arq C2: decide si toca
  pedir `ACCESS_FINE_LOCATION` ahora); `MapaColor`
  (RF-USR-03, incluye marrón = código del rojo); disparos `::<n>` con silencio confirmado ANTES del
  plazo (A3) y cuarentena (RF-USR-16); `SerieDisparos` (RF-USR-04, `n` fijado al empezar la serie,
  A1); `Diario` (persistencia por disparo, tolera una línea `FILA` cortada, B1);
  `RegistroTramas`/`CanalRegistrado` (`tramas.log`, con respaldo en fichero — M2 —, línea
  `SIN_RESPUESTA <trama>` cuando una trama `#` no llega y `SESION_NUEVA MAC=...` al abrir enlace,
  B-3 de la 0.3.0); `CsvMedidas`/`FilaMedida`, `NombreZip` (colisión por sistema de ficheros y, para
  `MediaStore`, contra un conjunto de nombres ya existentes — QA-4 —), `ExportadorZip` (fichero u
  `OutputStream`, A4); `EstrategiaExportacion` (API → vía de exportar, A4); `EstadoGps` (descarta una
  posición de más de 2 min o con precisión peor que 50 m, QA-7); `SesionMedicion` (orquesta todo,
  `medir` con cerrojo — A1 —, `filasCortadasIgnoradas` para que el exportador avise, B-4 de la
  0.3.0).
- Capa Android: `MainActivity` (aviso previo una sola vez por proceso — M-1 —, `restaurarInterfaz()`
  en `onCreate` pinta lo que `SesionHolder` ya sabe — lista deshabilitada si `detectando()`,
  `btnReintentar` visible si `canalSondaEnCurso()`, `btnContinuar` si `sesion()` — arq M-1 —,
  detección con `CanalRegistrado` sobre el mismo `tramas.log` que los disparos — M2 —, cambio de
  equipo decidido por `dominio.GestorEnlace` sobre `SesionHolder.enlace()` — arq C1, corrige la fuga
  de socket de la 0.3.1 —, un enlace REUTILIZADO sin respuesta a `#V#` se reintenta con conexión
  nueva antes de "no compatible" — arq ALTO —, un socket recién abierto se cierra en vez de
  registrarse si la Activity ya terminó de verdad mientras `connect()` estaba en curso — arq B-3 —,
  estado de "Reintente" en `SesionHolder`, no en un campo de la Activity — arq B-1 de la 0.3.1 —,
  "Exportar" desde la pantalla principal sin sesión viva — QA-6 —, `onDestroy` libera el equipo
  (cierra el enlace y limpia la sesión) al salir de verdad con Atrás desde la Activity raíz — arq M-2,
  corrige M7/arq B-1 de la 0.3.1, que no lo hacía si había sesión viva); `MedirActivity` (pantalla 4,
  botones deshabilitados mientras mide o exporta — A1 —, exportar por API — A4 —, pide
  `ACCESS_FINE_LOCATION` al entrar, una vez por proceso — arq C2 —, `0` mostrado como "saturado o
  negativo" con las lecturas crudas — M3 —, hilo de medir captura cualquier excepción y la muestra —
  B-1 de la 0.3.0); `AjustesActivity` (`lecturasPorColor`, persistido en `SharedPreferences` — B3);
  `EnlaceBluetooth` (`Canal` y `FuenteBytes`, `ahoraMs()` relativo a la apertura del enlace — M2 —,
  expone la MAC realmente conectada — A2 —, `vivo()` delega en `dominio.EstadoEnlace` — arq ALTO,
  ya no es sólo `socket.isConnected()` — para que `GestorEnlace` decida si reutilizar); `UbicacionGps`
  (lee el `Location` del sistema, la decisión de si sirve vive en `dominio.EstadoGps` — QA-7 —, toma
  la posición MÁS RECIENTE de todos los proveedores antes de clasificar — arq B-3 de la 0.3.1 —, no
  pide el permiso, eso lo hace `MedirActivity` — arq C2); `ExportadorAndroid` (vía
  `MediaStore.Downloads`, con la colisión de nombre resuelta consultando el `ContentResolver` antes
  de insertar — QA-4 —, limpia `IS_PENDING` ante cualquier excepción, no sólo `IOException` — B-2);
  `SesionHolder` (única fuente de verdad del enlace desde que se abre el socket, no sólo si la sonda
  tiene éxito — arq C1 —, `detectando()`/`marcarDetectando()` en vez de un campo de `MainActivity` —
  arq M-1 —, estado de "Reintente" de la sonda en curso — arq B-1 de la 0.3.1 —, invalidación al
  cambiar de equipo — A2 —, ajustes persistidos — B3 —, aviso previo y permiso de ubicación
  aceptados una vez por proceso — M-1/arq C2).
- **Reloj inyectable, sin `Thread.sleep` en pruebas del dominio:** toda espera pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador avanza un
  reloj propio. Excepción deliberada: `Sonda362`/`DeteccionYSonda` sí usan un `Thread.sleep` real de
  150 ms en producción (M5, QA-3, pausa entre `#V#`→`#GN#` y entre `#GN#`→`#GC#`); las pruebas pasan
  un consumidor de pausa que no duerme (overload de 3 argumentos, visible sólo para el paquete
  `dominio`).

## Tests JVM

126 tests, `dominio/*Test.java` (88 requisito / 38 comportamiento; los 120 de la 0.3.2, 83/37, menos
3 duplicados de `GestorEnlaceTest` que ya contaban como requisito de arq C1 — mismos tres argumentos
bajo un nombre de escenario distinto, B-2 — más 9 nuevos de esta vuelta, 8 requisito / 1
comportamiento). **QA D-2:** desde esta entrega, una prueba cuenta como requisito sólo si la
condición que cita está escrita en `05_Documentacion/REVISIONES-Apps-V3.6.md` (regla del propio
fichero, `:39-42`) — no basta con que el analista recuerde el informe del arquitecto o QA, si no
quedó archivado. Las 9 nuevas:

- **`GestorEnlaceTest.enlaceAnteriorMuertoConDistintaMac...`** (1, requisito, arq C1 — la quinta
  entrada de `decidir` que las 7 pruebas viejas nunca cubrían: enlace muerto Y otra MAC a la vez).
- **`GestorEnlaceTest.debeReintentarConexionNueva*`** (4, requisito, arq ALTO —
  `REVISIONES-Apps-V3.6.md:34-35` — tabla de verdad completa de
  `GestorEnlace.debeReintentarConexionNueva`).
- **`EstadoEnlaceTest`** (4: 3 requisito arq ALTO — prueban la fórmula exacta que pide la condición,
  `!caido && socketConectado`, en sus tres combinaciones — y 1 comportamiento —
  `marcarCaidoEsIrreversibleParaLaVidaDeEsteEnlace`, que llamar `marcarCaido()` dos veces no "revive"
  el enlace es diseño de esta entrega, no algo que la condición pida por sí misma).

`EstadoEnlace` y `GestorEnlace.debeReintentarConexionNueva` no existen en 385fc92 (`GestorEnlaceTest`
antiguo/`EstadoEnlaceTest` darían "cannot find symbol" si se copiaran solos contra ese commit); la
condición arq ALTO pide explícitamente no usar eso como demostración de rojo. En su lugar: se corrió
`unEnlaceCaidoNoEstaVivoAunqueElSocketSigaConectado` y `reutilizadoYSinRespuestaReintenta` con la
aserción INVERTIDA contra el código ya corregido (`assertTrue`/`assertFalse` cambiados), con
`JUnitCore`, y de verdad fallaron por comportamiento (no por compilación) — la traza exacta de cada
corrida está pegada en el Javadoc de `EstadoEnlaceTest` y en el comentario de
`GestorEnlaceTest.reutilizadoYSinRespuestaReintenta`; luego se corrigió la aserción y quedó en verde.
`DetectorEquipoTest` no ganó métodos nuevos: sus 3 casos existentes se reforzaron con una aserción de
`sinRespuesta()` cada uno (arq ALTO), sin cambiar el recuento de pruebas.

Lo ya vigente de 766e6f2 (0.3.1) y de antes sigue igual, sin repetirse aquí — **arq C1**
(`GestorEnlaceTest`, "cannot find symbol" contra 766e6f2), **arq C2** (`PermisoUbicacionTest`, mismo
motivo), **arq B-4** (`Sonda362ExigirTest`, rojo por comportamiento) — y los que ya citaban la
0.3.0/0.3.1 (QA-1 a QA-10, A3, A1 cerrojo, A1 `n` fijo, B1, T-USR-01c): ver `git log -p` de esas
entregas si hace falta el detalle.

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
cd app
CP="build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes"
CP="$CP;src/test/resources;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar"
CLASSES=$(find build/intermediates/javac/debugUnitTest/classes -name "*Test.class" \
  | sed 's#build/intermediates/javac/debugUnitTest/classes/##; s#\.class$##; s#/#.#g')
"$JAVA_HOME/bin/java" -cp "$CP" org.junit.runner.JUnitCore $CLASSES
```

## Verificación del APK — por CONTENIDO, no por md5 del fichero entero

**AGP empaqueta con zipflinger**, que puede reordenar las entradas del ZIP entre dos builds
idénticos en código: el md5 del `.apk` completo **no es estable** aunque nada haya cambiado, así que
comparar dos entregas por el md5 del fichero da falsos "distintos". Lo que sí es estable es el
**contenido** — versión, certificado y cada entrada del ZIP, con independencia del orden en que
zipflinger las escribió. Comando exacto (build-tools de la SDK, `aapt`/`apksigner`/`unzip` en PATH):

```bash
APK=app/build/outputs/apk/debug/app-debug.apk

# 1) version y applicationId declarados en el manifiesto empaquetado.
aapt dump badging "$APK" | grep -E "package: name|versionCode|versionName"

# 2) certificado de firma (compara el fingerprint, no el .apk entero).
apksigner verify --print-certs "$APK"

# 3) contenido: hash de CADA entrada del ZIP (dex, recursos, manifiesto...), en orden de nombre —
#    no el hash del .apk como blob, que zipflinger puede reordenar entre builds identicos.
for f in $(unzip -Z1 "$APK" | sort); do
  printf '%s  %s\n' "$(unzip -p "$APK" "$f" | sha256sum | cut -d' ' -f1)" "$f"
done | sha256sum
```

El paso 3 da UN sha256 que resume el contenido entero del ZIP, estable entre builds bit-idénticos
aunque zipflinger cambie el orden físico de las entradas. Dos `.apk` con el mismo paso 3 tienen el
mismo `classes.dex`, los mismos recursos y el mismo `AndroidManifest.xml` compilado, byte a byte.

## Reproducibilidad de la fuente

`fuente.md5` (raíz de este proyecto) declara el md5 de cada fichero de `app/src/main`, de
`app/build.gradle` (arq B-5: `applicationId`, `versionCode`/`versionName`, `compileSdk`... afectan
la reproducibilidad del build igual que cualquier fuente) y, desde esta entrega, de
**`build.gradle`, `settings.gradle` y `gradle.properties` de la raíz de este proyecto** (arq B-1
sobre 0.3.2: `classpath 'com.android.tools.build:gradle:4.1.1'` del `build.gradle` raíz, el
`rootProject.name` de `settings.gradle` y `android.useAndroidX`/`android.enableJetifier` de
`gradle.properties` condicionan el build exactamente igual que `app/build.gradle` — quedaban fuera
sin ninguna razón para excluirlos). `.gitattributes` (raíz del repositorio)
fuerza `03_App_Movil/RetroUsuario/** text eol=lf`, y desde esta entrega también
`08_Senales/senales.csv text eol=lf` (QA Medio), para que el md5 no cambie con el fin de línea de
quien clona en Windows (`core.autocrlf`); los binarios (`*.png`, `*.jar`, `*.apk`, `*.zip`, `*.jks`,
`*.keystore`) están excluidos explícitamente, después de la regla general (en `.gitattributes` manda
el último patrón que matchea la misma ruta).

**QA Alto (condición sobre 0.3.1): `fuente.md5` se genera desde los BLOBS de git, no del árbol de
trabajo** — generarlo sobre el árbol de trabajo de quien lo escribe cuela CRLF residual de ficheros
que `.gitattributes` ya declara `eol=lf` pero que ese checkout nunca renormalizó (así fallaban 3
entradas de la 0.3.1). Receta, sobre el commit ya hecho:

```bash
git ls-files app/src/main app/build.gradle build.gradle settings.gradle gradle.properties \
    | sort | while IFS= read -r f; do
  h=$(git cat-file -p "HEAD:03_App_Movil/RetroUsuario/$f" | md5sum | cut -d' ' -f1)
  printf '%s *%s\n' "$h" "$f"
done > fuente.md5
```

Verificado en esta entrega con `git worktree add --detach <ruta> <commit>` (checkout limpio, sin
nada del árbol de quien lo generó) + `md5sum -c fuente.md5`: 65/65 `OK` (61 de la 0.3.2 más
`dominio/EstadoEnlace.java`, `build.gradle`, `settings.gradle` y `gradle.properties` de la raíz de
este proyecto, arq ALTO/B-1).

## Lo que NO verifica esta corrida

Nada de esto sale de un verde de la JVM: que el firmware real responda como el simulador (incluida
la hipótesis "latencia < plazo+Q" de T-B06, sin medir); el Bluetooth de un equipo de verdad,
incluido el cambio de equipo real (A2/M-1/arq C1 — `GestorEnlace` sólo se probó con MACs y booleanos
de "vivo" sintéticos, nunca con un `BluetoothSocket` real reutilizándose o cayéndose) y el ciclo de
vida real de las Activities (M7/arq B-1, incluida la carrera del giro que `isChangingConfigurations()`
dice resolver); la pantalla (esta app no toca la STONE); GPS real (QA-7/arq B-3: los umbrales de
`EstadoGps` y "elegir el más reciente de varios proveedores" se probaron con valores sintéticos, no
con dos proveedores reales dando posiciones distintas); el diálogo real de
`ACCESS_FINE_LOCATION` (arq C2: que se pida una sola vez por proceso, y que denegarlo de verdad deje
`gps_estado = sin_posicion` sin bloquear, no se vio en un teléfono); exportar de verdad a
`Download/RetroUsuario/` o a `MediaStore.Downloads` en un teléfono (A4/QA-4, las dos vías, incluida
la consulta real al `ContentResolver`); el permiso de almacenamiento pedido en tiempo de ejecución
(A4); `SharedPreferences` sobreviviendo a un reinicio real del proceso (B3); que el botón
"Reintentar"/"Exportar" de `MainActivity` se comporte como describe este README en un teléfono real
(QA-1/QA-5/QA-6).
Tampoco: que un `BluetoothSocket` real se comporte como `EstadoEnlace` asume (que `read()` de verdad
lance `IOException` o dé EOF cuando el equipo se cae, y no se quede bloqueado para siempre) — arq
ALTO, la fórmula `!caido && socketConectado` sí tiene prueba JVM (`EstadoEnlaceTest`), el cableado en
`EnlaceBluetooth`/`MainActivity.hiloConectarYDetectar` (incluido el reintento con conexión nueva) no;
que `isFinishing()`/`isChangingConfigurations()` distingan de verdad un giro de pantalla de una
salida real y de la destrucción por memoria en un teléfono (arq M-1/M-2/B-3, sólo verificado por
lectura); que "salir con Atrás" en un teléfono real deje al equipo disponible para que otro conecte
en el sentido Bluetooth del sistema operativo (M-2, más allá de que este proceso cierre su socket).

A2, M3, M6, M7, QA-1, QA-3, QA-4 (la parte de `ContentResolver`), QA-5, QA-6, QA-7 (la parte de
`UbicacionGps`), B3, arq C1 (el cableado en `MainActivity`/`SesionHolder`, no la decisión de
`GestorEnlace`, que sí tiene prueba JVM), arq C2 (el cableado en `MedirActivity`, no la decisión de
`PermisoUbicacion`) y arq B-3 de la 0.3.1 (el bucle sobre `LocationManager` en `UbicacionGps`) son
cambios de la capa Android: se verificaron por lectura y por compilación contra el SDK real
(`compileDebugJavaWithJavac`/`assembleDebug`), no con una prueba JVM (esa capa no tiene arnés de
pruebas en este árbol, "Tests JVM" arriba: sólo `dominio/`).
