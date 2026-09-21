# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus tests JVM. Nada probado contra un equipo ni un teléfono**
(CLAUDE.md, cabecera). Incremento 1 "Medir y exportar" completo
(`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0): detección y sonda 3.6.2 (RF-USR-01/02), mapa
color→byte (RF-USR-03), serie de disparos con plazo/silencio/cuarentena/repetición
(RF-USR-04/RF-USR-16), modo por defecto sin tecleo (RF-USR-05/06), sin modo administrador
(RF-USR-15), exportación a ZIP (RF-USR-15 bis). El incremento 2 "Señal a señal" no está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 2`, `versionName "0.2.0"`.
- `minSdk 24`, `targetSdk 30`, `compileSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`, ubicación
  (opcional, sin pedirla en tiempo de ejecución: se mide igual sin ella, RF-USR-15/T-USR-23),
  `WRITE_EXTERNAL_STORAGE` (≤ API 28, con `requestLegacyExternalStorage` para 29-30).
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`. Catálogo de colores: `08_Senales/senales.csv`.

## Compilar

Misma cadena que `03_App_Movil/RetroV36` (CLAUDE.md §8): JDK 11 Temurin, Gradle 6.5, AGP 4.1.1.

```bash
export JAVA_HOME="D:/@Proyect/Baliza/7 sw apk/jdk-11/jdk-11.0.24+8"
./gradlew assembleDebug --offline      # app/build/outputs/apk/debug/app-debug.apk
```

## Estructura del código

- `dominio/` (**Java puro, sin Android**, probado en la JVM): detección y sonda (RF-USR-01/02),
  `MapaColor` (RF-USR-03); `FuenteBytes` + `LectorDisparo` + `EmisorRitmo` (RF-USR-16: silencio,
  plazo, ritmo 1500/600 ms, cuarentena) + `SerieDisparos` (RF-USR-04: repetición por disparo anulado
  y por cero persistente, media redondeada mitad hacia arriba); `Diario` (persistencia por disparo,
  RF-USR-06 C5); `CsvMedidas`/`FilaMedida` (`medidas.csv`, RFC 4180, BOM, CRLF), `InventarioCsv`,
  `NombreZip`, `ExportadorZip` (RF-USR-15 bis); `SesionMedicion` (orquesta todo lo anterior).
- Capa Android: `MainActivity` (aviso, detección, sonda), `MedirActivity` (pantalla 4: color →
  Medir, exportar, cero tecleo), `AjustesActivity` (pantalla 5: sólo `lecturasPorColor`),
  `EnlaceBluetooth` (implementa `Canal` **y** `FuenteBytes` sobre el mismo socket), `UbicacionGps`
  (última posición conocida, sin bloquear si no hay), `SesionHolder` (la `SesionMedicion` viva).
- **Reloj inyectable, sin `Thread.sleep` en pruebas:** toda espera del dominio pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador
  (`EquipoSimuladoDisparos`) avanza un reloj propio de forma determinista.
- **PA-02 sin `java.time`:** igual que en la primera parte (`FechaISO` propio); `fecha_hora` de
  `medidas.csv` usa `SimpleDateFormat` con patrón `XXX` (offset con dos puntos), disponible desde
  API 24.

## Tests JVM

58 tests, `dominio/*Test.java`, contra `EquipoSimulado` (tramas `#...#`) y
`EquipoSimuladoDisparos` (disparos `::<n>`, byte a byte, con reloj propio). **Recuento
requisito/comportamiento (CLAUDE.md §7):** ~46 aseveran un valor de fuente ajena (firmware,
protocolo, SPEC/TDD, decisión de Diego); ~12 fijan comportamiento de este trabajo sin cita externa
(formato del diario, nombre del ZIP, redondeo). Vistas en rojo a propósito, con su salida
comprobada: la cuarentena de RF-USR-16 (T-USR-24 b/d, `EmisorRitmo.cuarentena`), el redondeo mitad
hacia arriba y la regla "media = 0" del cero persistente (T-USR-06), y la regla de comillas de `;`
en `medidas.csv` (T-USR-21a).

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
cd app
CP="build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes"
CP="$CP;src/test/resources;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar"
"$JAVA_HOME/bin/java" -cp "$CP" org.junit.runner.JUnitCore \
  com.dpi.retrousuario.dominio.DetectorEquipoTest com.dpi.retrousuario.dominio.Sonda362Test \
  com.dpi.retrousuario.dominio.EstadoCalibracionTest com.dpi.retrousuario.dominio.MapaColorTest \
  com.dpi.retrousuario.dominio.RespuestaTramaTest com.dpi.retrousuario.dominio.SerieDisparosTest \
  com.dpi.retrousuario.dominio.RitmoYCuarentenaTest com.dpi.retrousuario.dominio.DiarioTest \
  com.dpi.retrousuario.dominio.RitmoDePacingTest com.dpi.retrousuario.dominio.CsvMedidasTest \
  com.dpi.retrousuario.dominio.NombreZipTest com.dpi.retrousuario.dominio.SesionMedicionFlujoTest \
  com.dpi.retrousuario.dominio.ListaBlancaYExportacionTest
```

## Lo que NO verifica esta corrida

Nada de esto sale de un verde de la JVM: que el firmware real responda como el simulador (incluida
la hipótesis "latencia < plazo+Q" de T-B06, sin medir); el Bluetooth de un equipo de verdad; la
pantalla (esta app no toca la STONE); GPS real (sólo última posición conocida, sin pedir un fix);
exportar a `Download/RetroUsuario/` en un teléfono real (permisos de almacenamiento con ámbito,
API 29-30); ni que `EnlaceBluetooth` reconecte o sobreviva un corte real del enlace.
