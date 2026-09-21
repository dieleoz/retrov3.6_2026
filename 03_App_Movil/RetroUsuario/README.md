# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus 80 tests JVM. Nada probado contra un equipo ni un teléfono**
(CLAUDE.md, cabecera). Cierra los hallazgos ALTO de las revisiones arquitecto-iot y qa-istqb de la
0.2.0 (A1-A4, exigir_362/T-USR-01c, D-1/M1) y los hallazgos M/B/D de esa misma vuelta (M2-M7, B1, B3,
D-3, D-4). Incremento 1 "Medir y exportar" (`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0)
completo; el incremento 2 "Señal a señal" no está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 3`, `versionName "0.3.0"`.
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
  configurable (RF-USR-01/02); `MapaColor` (RF-USR-03, incluye marrón = código del rojo); disparos
  `::<n>` con silencio confirmado ANTES del plazo (A3) y cuarentena (RF-USR-16); `SerieDisparos`
  (RF-USR-04, `n` fijado al empezar la serie, A1); `Diario` (persistencia por disparo, tolera una
  línea `FILA` cortada, B1); `RegistroTramas` (`tramas.log`, con respaldo en fichero, M2);
  `CsvMedidas`/`FilaMedida`, `NombreZip`, `ExportadorZip` (fichero u `OutputStream`, A4);
  `EstrategiaExportacion` (API → vía de exportar, A4); `SesionMedicion` (orquesta todo, `medir`
  con cerrojo, A1).
- Capa Android: `MainActivity` (aviso, detección con `CanalRegistrado` sobre el mismo `tramas.log`
  que los disparos — M2 —, cambio de equipo con reconexión — A2 —, ciclo de vida que no cierra un
  enlace con sesión viva — M7); `MedirActivity` (pantalla 4, botones deshabilitados mientras mide o
  exporta — A1 —, exportar por API — A4 —, `0` mostrado como "saturado o negativo" con las lecturas
  crudas — M3); `AjustesActivity` (`lecturasPorColor`, persistido en `SharedPreferences` — B3);
  `EnlaceBluetooth` (`Canal` y `FuenteBytes`, `ahoraMs()` relativo a la apertura del enlace — M2 —,
  expone la MAC realmente conectada — A2); `ExportadorAndroid` (vía `MediaStore.Downloads`, A4);
  `SesionHolder` (sesión viva, invalidación al cambiar de equipo — A2 —, ajustes persistidos — B3).
- **Reloj inyectable, sin `Thread.sleep` en pruebas del dominio:** toda espera pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador avanza un
  reloj propio. Excepción deliberada: `Sonda362` sí usa un `Thread.sleep` real de 150 ms en
  producción (M5, pausa entre `#GN#` y `#GC#`); las pruebas pasan un consumidor de pausa que no
  duerme (overload de 3 argumentos, visible sólo para el paquete `dominio`).

## Tests JVM

80 tests, `dominio/*Test.java` (62 requisito / 18 comportamiento; los 58 de la entrega anterior, 48/10,
más 22 de esta vuelta, 14/6 salvo por dos fichas fronterizas que se cuentan del lado de comportamiento
— ver el detalle en el informe de esta entrega, no repetido aquí por espacio). Cada arreglo se vio en
rojo contra el código sin el arreglo (revertido con `git checkout -- <fichero>` en un ciclo aparte, no
`git stash`): A3 (`RitmoYCuarentenaTest`, "expected:<ANULADO_PLAZO> but was:<VALOR>"), A1 cerrojo
(`SesionMedicionConcurrenciaTest`, `ConcurrentModificationException`/timeout sin el `synchronized`),
A1 `n` fijo (`SerieDisparosTest`, `guardada=false` con el `n` cambiando a mitad), B1
(`DiarioTest`, `ArrayIndexOutOfBoundsException` sin el manejo de línea cortada), T-USR-01c
(`Sonda362ExigirTest`, error de compilación: la rama `exigir_362`/el overload de 3 argumentos no
existían).

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
cd app
CP="build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes"
CP="$CP;src/test/resources;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar"
CLASSES=$(find build/intermediates/javac/debugUnitTest/classes -name "*Test.class" \
  | sed 's#build/intermediates/javac/debugUnitTest/classes/##; s#\.class$##; s#/#.#g')
"$JAVA_HOME/bin/java" -cp "$CP" org.junit.runner.JUnitCore $CLASSES
```

## Lo que NO verifica esta corrida

Nada de esto sale de un verde de la JVM: que el firmware real responda como el simulador (incluida
la hipótesis "latencia < plazo+Q" de T-B06, sin medir); el Bluetooth de un equipo de verdad,
incluido el cambio de equipo real (A2) y el ciclo de vida real de las Activities (M7); la pantalla
(esta app no toca la STONE); GPS real; exportar de verdad a `Download/RetroUsuario/` o a
`MediaStore.Downloads` en un teléfono (A4, las dos vías); el permiso de almacenamiento pedido en
tiempo de ejecución (A4); `SharedPreferences` sobreviviendo a un reinicio real del proceso (B3).
A2, M3, M6, M7 y B3 son cambios de la capa Android: se verificaron por lectura y por compilación
contra el SDK real (`compileDebugJavaWithJavac`), no con una prueba JVM (esa capa no tiene arnés de
pruebas en este árbol, `03_App_Movil/RetroUsuario/README.md`, "Tests JVM": sólo `dominio/`).
