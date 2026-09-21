# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus 120 tests JVM (checkout limpio). Nada probado contra un
equipo ni un teléfono** (CLAUDE.md, cabecera). Cierra las condiciones de arquitecto-iot y qa-istqb
sobre la 0.3.1, ambas APTO CON CONDICIONES: **arq C1** (fuga de socket al cambiar de equipo, ahora
en `dominio.GestorEnlace`), **arq C2** (`ACCESS_FINE_LOCATION` sí se pide, al entrar a medir),
**arq B-1** (estado de "Reintente" en `SesionHolder`, no en la Activity; carrera del giro en
`onDestroy`), **arq B-3** (GPS toma la posición más reciente de todos los proveedores, no la del
primero), **arq B-4** (con `exigir_362 = false`, el silencio también ofrece reintentar antes de
medir degradado), **arq B-5** (`fuente.md5` incluye `app/build.gradle`), **QA Alto** (`fuente.md5`
regenerado desde los blobs de git, no del árbol de trabajo) y **QA Medio**
(`MapaColorTest.MD5_CATALOGO` fijo al md5 del blob LF de `08_Senales/senales.csv`). Las condiciones
de la 0.3.0 (QA-1 a QA-10) siguen cerradas, sin repetirse aquí. Incremento 1 "Medir y exportar"
(`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0) completo; el incremento 2 "Señal a señal" no
está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 5`, `versionName "0.3.2"`.
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
  configurable (RF-USR-01/02), caso mixto que conserva lo que sí contestó (QA-2); silencio total en
  `#GN#`/`#GC#` ofrece reintentar con cualquier valor de `exigir_362` (arq B-4), y `Sonda362.vacio()`
  da el resultado degradado tras agotar los reintentos con `exigir_362 = false`; `SondaReintentable`
  (reintento de la sonda operador-driven, hasta 2, sin reenviar `#V#`, QA-1); `DeteccionYSonda`
  (detección + primera sonda con la pausa de 150 ms entre las dos, QA-3); `GestorEnlace` (arq C1:
  decide reutilizar/cerrar/conectar un enlace anterior — sólo se reutiliza con la misma MAC y vivo);
  `PermisoUbicacion` (arq C2: decide si toca pedir `ACCESS_FINE_LOCATION` ahora); `MapaColor`
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
- Capa Android: `MainActivity` (aviso previo una sola vez por proceso — M-1 —, detección con
  `CanalRegistrado` sobre el mismo `tramas.log` que los disparos — M2 —, cambio de equipo decidido
  por `dominio.GestorEnlace` sobre `SesionHolder.enlace()` — arq C1, corrige la fuga de socket de la
  0.3.1 —, estado de "Reintente" en `SesionHolder`, no en un campo de la Activity — arq B-1 —,
  "Exportar" desde la pantalla principal sin sesión viva — QA-6 —, `onDestroy` no cierra un enlace
  que un giro o `MedirActivity` siguen usando — M7/arq B-1); `MedirActivity` (pantalla 4, botones
  deshabilitados mientras mide o exporta — A1 —, exportar por API — A4 —, pide
  `ACCESS_FINE_LOCATION` al entrar, una vez por proceso — arq C2 —, `0` mostrado como "saturado o
  negativo" con las lecturas crudas — M3 —, hilo de medir captura cualquier excepción y la muestra —
  B-1 de la 0.3.0); `AjustesActivity` (`lecturasPorColor`, persistido en `SharedPreferences` — B3);
  `EnlaceBluetooth` (`Canal` y `FuenteBytes`, `ahoraMs()` relativo a la apertura del enlace — M2 —,
  expone la MAC realmente conectada — A2 —, `vivo()` para que `GestorEnlace` decida si reutilizar —
  arq C1); `UbicacionGps` (lee el `Location` del sistema, la decisión de si sirve vive en
  `dominio.EstadoGps` — QA-7 —, toma la posición MÁS RECIENTE de todos los proveedores antes de
  clasificar — arq B-3 —, no pide el permiso, eso lo hace `MedirActivity` — arq C2);
  `ExportadorAndroid` (vía `MediaStore.Downloads`, con la colisión de nombre resuelta consultando el
  `ContentResolver` antes de insertar — QA-4 —, limpia `IS_PENDING` ante cualquier excepción, no sólo
  `IOException` — B-2); `SesionHolder` (única fuente de verdad del enlace desde que se abre el
  socket, no sólo si la sonda tiene éxito — arq C1 —, estado de "Reintente" de la sonda en curso —
  arq B-1 —, invalidación al cambiar de equipo — A2 —, ajustes persistidos — B3 —, aviso previo y
  permiso de ubicación aceptados una vez por proceso — M-1/arq C2).
- **Reloj inyectable, sin `Thread.sleep` en pruebas del dominio:** toda espera pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador avanza un
  reloj propio. Excepción deliberada: `Sonda362`/`DeteccionYSonda` sí usan un `Thread.sleep` real de
  150 ms en producción (M5, QA-3, pausa entre `#V#`→`#GN#` y entre `#GN#`→`#GC#`); las pruebas pasan
  un consumidor de pausa que no duerme (overload de 3 argumentos, visible sólo para el paquete
  `dominio`).

## Tests JVM

120 tests, `dominio/*Test.java` (83 requisito / 37 comportamiento; los 108 de la 0.3.1, 77/31, más 12
de esta vuelta, 6/6 — la clasificación de las 12 nuevas es del analista de esta entrega, con el mismo
criterio de CLAUDE.md §7: cuenta como requisito lo que cita PROTOCOLO/SPEC/una condición escrita del
arquitecto o QA; cuenta como comportamiento lo que fija un diseño propio de este trabajo sin cita
externa — p. ej. cuántas veces se pide el permiso de ubicación por proceso, o los tres casos de
`GestorEnlace` que no nombra la condición C1 palabra por palabra). Cada arreglo se vio en rojo contra
766e6f2 (verificado en un `git worktree add` aparte, con sólo los ficheros de prueba nuevos/tocados
copiados encima de ese commit, nunca `git stash`):

- **arq C1** (`GestorEnlaceTest`, 7 tests): la clase no existía en 766e6f2 — "cannot find symbol"
  contra las 7 pruebas.
- **arq C2** (`PermisoUbicacionTest`, 4 tests): mismo motivo, `dominio.PermisoUbicacion` no existía.
- **arq B-4** (`Sonda362ExigirTest.exigir362FalsoConSilencioTambienOfreceReintentarAntesDeMedirDegradado`):
  `expected:<SIN_RESPUESTA_REINTENTAR> but was:<OK>` contra 766e6f2 (exigir_362 = false medía
  degradado de inmediato, sin ofrecer reintentar); `Sonda362ExigirTest.vacioNoLlevaSerieNiFecha` y
  `SesionMedicionFlujoTest.tUsr01cD_...`: "cannot find symbol" contra `Sonda362` sin `vacio()`.
- **QA Medio** (`MapaColorTest`): `MD5_CATALOGO` no es una prueba nueva, es una constante corregida
  (era el md5 CRLF de este checkout, `75bfb0f0...`; pasa al md5 LF del blob, `9f94297f...`) — se
  verifica con el `git worktree add` limpio de "Reproducibilidad de la fuente", no en rojo/verde.

Los mismos que ya citaban la 0.3.0/0.3.1 siguen vigentes y no se repiten aquí (QA-1 a QA-10, A3, A1
cerrojo, A1 `n` fijo, B1, T-USR-01c): ver `git log -p` de esas entregas si hace falta el detalle.

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

`fuente.md5` (raíz de este proyecto) declara el md5 de cada fichero de `app/src/main` **y de
`app/build.gradle`** (arq B-5: `applicationId`, `versionCode`/`versionName`, `compileSdk`... afectan
la reproducibilidad del build igual que cualquier fuente). `.gitattributes` (raíz del repositorio)
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
git ls-files app/src/main app/build.gradle | sort | while IFS= read -r f; do
  h=$(git cat-file -p "HEAD:03_App_Movil/RetroUsuario/$f" | md5sum | cut -d' ' -f1)
  printf '%s *%s\n' "$h" "$f"
done > fuente.md5
```

Verificado en esta entrega con `git worktree add --detach <ruta> <commit>` (checkout limpio, sin
nada del árbol de quien lo generó) + `md5sum -c fuente.md5`: 61/61 `OK`.

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
A2, M3, M6, M7, M-1, QA-1, QA-3, QA-4 (la parte de `ContentResolver`), QA-5, QA-6, QA-7 (la parte de
`UbicacionGps`), B3, arq C1 (el cableado en `MainActivity`/`SesionHolder`/`EnlaceBluetooth.vivo()`,
no la decisión de `GestorEnlace`, que sí tiene prueba JVM), arq C2 (el cableado en `MedirActivity`,
no la decisión de `PermisoUbicacion`) y arq B-3 (el bucle sobre `LocationManager` en `UbicacionGps`)
son cambios de la capa Android: se verificaron por lectura y por compilación contra el SDK real
(`compileDebugJavaWithJavac`/`assembleDebug`), no con una prueba JVM (esa capa no tiene arnés de
pruebas en este árbol, "Tests JVM" arriba: sólo `dominio/`).
