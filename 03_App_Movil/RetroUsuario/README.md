# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 21-sep-2026: compila y pasa sus tests JVM. Nada probado contra un equipo ni un teléfono**
(CLAUDE.md, cabecera). Primera parte del incremento 1 "Medir y exportar"
(`05_Documentacion/SPEC-App-Usuario-V3.6.md` r4, §0): RF-USR-01 (detección + sonda 3.6.2), RF-USR-02
(estado de calibración) y RF-USR-03 (mapa color → byte), con el parser de §3. **No** incluye la serie
de disparos (`::<n>`, RF-USR-04), el modo "Medir y exportar" en pantalla (RF-USR-05/06) ni la
exportación (RF-USR-15 bis): quedan para la siguiente parte de este incremento.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador
(RF-USR-15).

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 1`, `versionName "0.1.0"`.
- `minSdk 24`, `targetSdk 30`, `compileSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN` (nada de
  ubicación todavía: esta pantalla sólo lista equipos ya emparejados).
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`. Catálogo de colores: `08_Senales/senales.csv`.

## Compilar

Misma cadena que `03_App_Movil/RetroV36` (CLAUDE.md §8): JDK 11 Temurin, Gradle 6.5, AGP 4.1.1.

```bash
export JAVA_HOME="D:/@Proyect/Baliza/7 sw apk/jdk-11/jdk-11.0.24+8"
./gradlew assembleDebug --offline      # app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` lleva `sdk.dir=C:/android-sdk` y no se versiona.

## Estructura del código

- `dominio/` (**Java puro, sin Android**, probado en la JVM): `Canal` (interfaz del enlace),
  `RespuestaTrama` (parser §3), `RespuestaV` (respuesta de `#V#`), `DetectorEquipo` (RF-USR-01),
  `Sonda362` (sonda `#GN#`/`#GC#`, M-3), `MapaColor` (RF-USR-03), `FechaISO` y `EstadoCalibracion`
  (RF-USR-02, PA-02).
- `EnlaceBluetooth` / `MainActivity` (Android): capa mínima, reescrita de
  `rtv-1.0:.../EnlaceSerie.java` **sin `Registro.*`** (SPEC §3): un `enviar(trama, plazoMs)` síncrono
  que acumula bytes hasta formar `"#...#"` o vencer el plazo. Sin la pacing fina de RF-USR-16
  (silencio 180 ms, `Ritmo`) ni `tramas.log` (RF-USR-15 bis): no hacen falta para esta parte y quedan
  para cuando se programe la serie de disparos.
- **PA-02 sin `java.time`:** `minSdkVersion 24` (API < 26) no tiene `java.time`, y activar
  desugaring de biblioteca añade una dependencia (`desugar_jdk_libs`) sin verificar en la caché
  offline de esta máquina (README de RetroV36, "Sin INTERNET"). `FechaISO` hace la aritmética a
  mano; la regla del 29-feb tiene su propia prueba de bisiesto.

## Tests JVM

24 tests, `dominio/*Test.java`, contra `EquipoSimulado` (doble de pruebas, registra lo que RECIBE,
no lo que la app dice enviar). **Recuento requisito/comportamiento (CLAUDE.md §7):** 19 aseveran un
valor de fuente ajena (firmware, protocolo, catálogo, decisión); 5 fijan comportamiento de este
trabajo sin cita externa (integridad del fixture de prueba, robustez del parser ante entradas raras,
la regla general del calendario bisiesto). Cuatro pruebas se vieron en rojo a propósito, rompiendo el
código y volviéndolo a dejar como estaba (sin rastro en el commit): confundir silencio con
`#ERR,FORMATO#` en `Sonda362`, quitar el ajuste del 29-feb de `FechaISO`, mapear marrón a un código
opaco en `MapaColor`, y enviar `9` tras un rechazo en `DetectorEquipo`.

`./gradlew testDebugUnitTest` **no arranca en esta máquina** (`ñ` de `C:\Users\Diego.Zuñiga`,
`GradleWorkerMain`; CLAUDE.md §8). Se compila con Gradle y se ejecuta con JUnit a mano:

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
mkdir -p libtest   # copiar aquí junit-4.13.2.jar y hamcrest-core-1.3.jar de ~/.gradle/caches
cd app
CP="build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes"
CP="$CP;src/test/resources;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar"
"$JAVA_HOME/bin/java" -cp "$CP" \
  org.junit.runner.JUnitCore com.dpi.retrousuario.dominio.DetectorEquipoTest \
  com.dpi.retrousuario.dominio.Sonda362Test com.dpi.retrousuario.dominio.EstadoCalibracionTest \
  com.dpi.retrousuario.dominio.MapaColorTest com.dpi.retrousuario.dominio.RespuestaTramaTest
```

## Lo que NO verifica esta corrida (particularidades/verificar.md §7)

Nada de esto sale de un verde de la JVM: que el firmware real responda como `EquipoSimulado`; el
Bluetooth de un equipo de verdad; el efecto de un byte no probado en campo; la pantalla (esta app no
toca la STONE); ni que `EnlaceBluetooth` reconecte o sobreviva un corte real del enlace (no probado,
ni en la JVM ni en un teléfono).
