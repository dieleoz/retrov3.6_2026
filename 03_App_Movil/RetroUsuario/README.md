# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus 108 tests JVM. Nada probado contra un equipo ni un teléfono**
(CLAUDE.md, cabecera). Cierra las condiciones de arquitecto-iot y qa-istqb sobre la 0.3.0
(QA-1 a QA-10: reintento de sonda operador-driven sin reenviar `#V#`, caso mixto de `exigir_362`
que conserva lo que sí contestó, pausa de 150 ms también entre `#V#` y `#GN#`, colisión de nombre en
`MediaStore` con `_2`/`_3`, guarda de M-1 sobre `SesionHolder.enlace()`, "Exportar" desde la pantalla
principal sin sesión, GPS con edad/precisión, B-1 a B-4, batería QA y reproducibilidad del `.apk`).
Incremento 1 "Medir y exportar" (`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0) completo; el
incremento 2 "Señal a señal" no está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 4`, `versionName "0.3.1"`.
- `minSdk 24`, `targetSdk 30`, `compileSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`, ubicación
  (opcional, sin pedirla en tiempo de ejecución: se mide igual sin ella, RF-USR-15/T-USR-23),
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
  configurable (RF-USR-01/02), caso mixto que conserva lo que sí contestó (QA-2); `SondaReintentable`
  (reintento de la sonda operador-driven, hasta 2, sin reenviar `#V#`, QA-1); `DeteccionYSonda`
  (detección + primera sonda con la pausa de 150 ms entre las dos, QA-3); `MapaColor` (RF-USR-03,
  incluye marrón = código del rojo); disparos `::<n>` con silencio confirmado ANTES del plazo (A3) y
  cuarentena (RF-USR-16); `SerieDisparos` (RF-USR-04, `n` fijado al empezar la serie, A1); `Diario`
  (persistencia por disparo, tolera una línea `FILA` cortada, B1); `RegistroTramas`/`CanalRegistrado`
  (`tramas.log`, con respaldo en fichero — M2 —, línea `SIN_RESPUESTA <trama>` cuando una trama `#`
  no llega y `SESION_NUEVA MAC=...` al abrir enlace, B-3); `CsvMedidas`/`FilaMedida`, `NombreZip`
  (colisión por sistema de ficheros y, para `MediaStore`, contra un conjunto de nombres ya existentes
  — QA-4 —), `ExportadorZip` (fichero u `OutputStream`, A4); `EstrategiaExportacion` (API → vía de
  exportar, A4); `EstadoGps` (descarta una posición de más de 2 min o con precisión peor que 50 m,
  QA-7); `SesionMedicion` (orquesta todo, `medir` con cerrojo — A1 —, `filasCortadasIgnoradas` para
  que el exportador avise, B-4).
- Capa Android: `MainActivity` (aviso previo una sola vez por proceso — M-1 —, detección con
  `CanalRegistrado` sobre el mismo `tramas.log` que los disparos — M2 —, cambio de equipo/reintento
  de sonda sobre `SesionHolder.enlace()`, no sobre un campo de la Activity — M-1/QA-5 —, "Exportar"
  desde la pantalla principal sin sesión viva — QA-6 —, ciclo de vida que no cierra un enlace con
  sesión viva — M7); `MedirActivity` (pantalla 4, botones deshabilitados mientras mide o exporta —
  A1 —, exportar por API — A4 —, `0` mostrado como "saturado o negativo" con las lecturas crudas —
  M3 —, hilo de medir captura cualquier excepción y la muestra — B-1); `AjustesActivity`
  (`lecturasPorColor`, persistido en `SharedPreferences` — B3); `EnlaceBluetooth` (`Canal` y
  `FuenteBytes`, `ahoraMs()` relativo a la apertura del enlace — M2 —, expone la MAC realmente
  conectada — A2); `UbicacionGps` (lee el `Location` del sistema, la decisión de si sirve vive en
  `dominio.EstadoGps`, QA-7); `ExportadorAndroid` (vía `MediaStore.Downloads`, con la colisión de
  nombre resuelta consultando el `ContentResolver` antes de insertar — QA-4 —, limpia `IS_PENDING`
  ante cualquier excepción, no sólo `IOException` — B-2); `SesionHolder` (sesión viva, invalidación
  al cambiar de equipo — A2 —, ajustes persistidos — B3 —, aviso previo aceptado una vez por proceso
  — M-1).
- **Reloj inyectable, sin `Thread.sleep` en pruebas del dominio:** toda espera pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador avanza un
  reloj propio. Excepción deliberada: `Sonda362`/`DeteccionYSonda` sí usan un `Thread.sleep` real de
  150 ms en producción (M5, QA-3, pausa entre `#V#`→`#GN#` y entre `#GN#`→`#GC#`); las pruebas pasan
  un consumidor de pausa que no duerme (overload de 3 argumentos, visible sólo para el paquete
  `dominio`).

## Tests JVM

108 tests, `dominio/*Test.java` (77 requisito / 31 comportamiento; los 80 de la 0.3.0, 62/18, más 28
de esta vuelta, 15/13 — la clasificación de las 28 nuevas es del analista de esta entrega, con el
mismo criterio de CLAUDE.md §7: cuenta como requisito lo que cita PROTOCOLO/SPEC/TDD/una decisión
escrita; cuenta como comportamiento lo que fija un diseño propio de este trabajo sin cita externa —
p. ej. los umbrales de `EstadoGps`, marcados `▸ propuesta` en el propio código, o la marca
`SESION_NUEVA`/`SIN_RESPUESTA` de `tramas.log`). Cada arreglo se vio en rojo contra 32c785d (revertido
con `git checkout -- <fichero>` en un ciclo aparte, o el fichero nuevo movido a `.bak`, nunca
`git stash`):

- QA-2 (`Sonda362Test.unMotivoDeErrorDistintoDeFormatoNoPideActualizarYConservaLaFechaQueSiContesto`):
  `expected:<OK> but was:<SIN_RESPUESTA_REINTENTAR>` contra 32c785d (la fecha de `#GC#` se descartaba).
- QA-1 (`SondaReintentableTest`), QA-3 (`DeteccionYSondaTest`), QA-7 (`EstadoGpsTest`): las tres
  clases no existían en 32c785d — error de compilación, "cannot find symbol", contra sus pruebas.
- QA-4 (`NombreZipTest`, casos de `resolverColisionEntreNombres`) y B-4/QA-4
  (`SesionMedicionFlujoTest`, `filasCortadasIgnoradasSeReflejanTrasExportar`,
  `nombreZipResuelto`): "cannot find symbol" contra `NombreZip`/`SesionMedicion` sin el método nuevo.
- B-3 (`CanalRegistradoTest.sesionNuevaDejaComentarioConLaMac`): "cannot find symbol" contra
  `CanalRegistrado` sin `sesionNueva`.

Los mismos que ya citaba la 0.3.0 siguen vigentes y no se repiten aquí (A3, A1 cerrojo, A1 `n` fijo,
B1, T-USR-01c): ver `git log -p` de esa entrega si hace falta el detalle.

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

`fuente.md5` (raíz de este proyecto) declara el md5 de cada fichero de `app/src/main`, generado sobre
el árbol limpio del commit final. `.gitattributes` (raíz del repositorio) fuerza
`03_App_Movil/RetroUsuario/** text eol=lf` para que ese md5 no cambie con el fin de línea de quien
clona en Windows (`core.autocrlf`); los binarios (`*.png`, `*.jar`, `*.apk`, `*.zip`, `*.jks`,
`*.keystore`) están excluidos explícitamente, después de la regla general (en `.gitattributes` manda
el último patrón que matchea la misma ruta).

## Lo que NO verifica esta corrida

Nada de esto sale de un verde de la JVM: que el firmware real responda como el simulador (incluida
la hipótesis "latencia < plazo+Q" de T-B06, sin medir); el Bluetooth de un equipo de verdad,
incluido el cambio de equipo real (A2/M-1) y el ciclo de vida real de las Activities (M7); la pantalla
(esta app no toca la STONE); GPS real (QA-7: los umbrales de `EstadoGps` se probaron con valores
sintéticos, no con un fix real envejeciendo); exportar de verdad a `Download/RetroUsuario/` o a
`MediaStore.Downloads` en un teléfono (A4/QA-4, las dos vías, incluida la consulta real al
`ContentResolver`); el permiso de almacenamiento pedido en tiempo de ejecución (A4); `SharedPreferences`
sobreviviendo a un reinicio real del proceso (B3); que el botón "Reintentar"/"Exportar" de
`MainActivity` se comporte como describe este README en un teléfono real (QA-1/QA-5/QA-6).
A2, M3, M6, M7, M-1, QA-1, QA-3, QA-4 (la parte de `ContentResolver`), QA-5, QA-6, QA-7 (la parte de
`UbicacionGps`) y B3 son cambios de la capa Android: se verificaron por lectura y por compilación
contra el SDK real (`compileDebugJavaWithJavac`), no con una prueba JVM (esa capa no tiene arnés de
pruebas en este árbol, "Tests JVM" arriba: sólo `dominio/`).
