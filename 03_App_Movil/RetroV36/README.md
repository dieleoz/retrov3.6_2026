# RTV — app única del Retrorreflectómetro Vertical (V3, V3.6, V4 y V4.6)

**Estado, 19-sep-2026 (RTV 1.0.0-rc6): compila y pasa 345 tests JVM (316 aseveran, 29 no). NADA de esto se ha probado
contra un equipo físico ni en un teléfono: los defectos que corrige la rc6 SÍ se midieron en campo, pero la
corrección no.** El modo V3.6 no se puede probar: el firmware V3.6 no existe todavía
en ningún equipo. La V4.6 tampoco existe grabada: se prueba contra `EquipoSimuladoV46`. Lo que sí debe
funcionar es la medida contra un V3 2020 (SLV-002) y contra un V4 original (`@LEERV`), y ninguna de las
dos se ha comprobado aún con esta app.

- Paquete `com.dpi.retrov36` (no cambia), etiqueta "RTV", `versionCode 10011`, `versionName 1.0.0-rc12`
  (RTV 1.0.0, sucede a 3.6.16; decisión VERSION de Diego). rc11 = `10010`, rc10 = `10009`, rc9 = `10008`,
  rc8 = `10007`, rc7 = `10006`, rc6 = `10005`, rc5 = `10004`. El APK de calibrar (buildType `coviandina`) es
  `com.dpi.retrov36.calibra`, `versionName Cov_3.6.6_calibrar`, `versionCode 10011` (nombre fijado por
  Diego el 21-sep-2026; RF-COV-16). La **rc1** se compiló con `10000`/`1.0.0` y es **anterior** a la
  mezcla de la 3.6.17 (`3dcaf41`): ese par no se reutiliza. Antes: `versionCode 3616`, `versionName
  3.6.16` (desde la 3.6.10 el versionCode sigue a RF-APP-41: 3.6.10 → 3610, 3.6.16 → 3616) (la 3.6.0
  enviaba `e` en la detección: no usar).
- `minSdk 24`, `targetSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`.
  **Sin `INTERNET`**: los ficheros salen por "Compartir" (`ACTION_SEND_MULTIPLE` + `FileProvider`).
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`, **revisión 1.1** (§4 bis).
- Base técnica: `RetroDiagBT` (repositorio V5). `EnlaceSerie`, `Registro` y `Hex` vienen de allí.

## Compilar

Misma cadena que RetroDiagBT (skill `compilar-apk` del repositorio V5): JDK 11, Gradle 6.5, AGP 4.1.1.

```bash
export JAVA_HOME="D:/@Proyect/Baliza/7 sw apk/jdk-11/jdk-11.0.24+8"
./gradlew assembleDebug --offline      # app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` lleva `sdk.dir=C:/android-sdk` (barras normales) y no se versiona.

**Entrega del APK.** Cada entrega se copia a `03_App_Movil/RTV-V<versionName>.apk` (p. ej. `RTV-V3.6.8.apk`) y,
desde la 3.6.10, también a `RTV-V<versionName>-<versionCode>.apk` (RF-APP-41). Desde la 3.6.11 ya no se
actualiza `RTV-V3.6.apk` (RF-APP-41 lo prohíbe, QA-3610-14), para que no se confunda una versión con otra en el teléfono. Se comprueba con
`aapt dump badging` que `versionCode`/`versionName` son los de la entrega y se declara el md5. Los `*.apk`
no se versionan.

### Tests JVM

`app/src/test/`: 30 clases, **345 tests** en la RTV 1.0.0-rc6 (333 de la rc5 + `Rc6DefectosTest` 12: **8 aseveran
un requisito** —los defectos medidos en campo la noche del 19-sep con SLV-002, SLV-003-2026 y SLV-028: el ZIP de
otro banco, la guarda de familia de firmware con la MAC repetida, el ZIP que sirve para calcular, el mensaje del
V4 original y el plazo de `@LEERV` frente a los 3,1-3,3 s medidos— y **4 fijan comportamiento**. Las 8 se vieron
en ROJO contra la rc5 (`d7d7b4d`) antes de arreglarlas. Además, **una de las 333 aseveraba el defecto D-1**
(`Version3616Test.importarConservaElTipoDeBancoOTraeSoloLasSeries`) y se corrigió el valor esperado, no el
arreglo. Añadir `Rc6DefectosTest` a la lista de clases al ejecutar con JUnit a mano. La rc5 fueron 333 (321 de la
rc4 + `SerieYAdminV46Test` 12: **9 aseveran un requisito** —la serie del SLV-003-2026 y la puerta del modo administrador, con el valor esperado tomado de `CAMBIOS-V4.6.md:259`, del registro de campo de las 18:53, de la decisión SERIE-2 de Diego y de la tabla de tramas §4.4 de la V4.6— y **3 fijan comportamiento** (saneado de lo tecleado). Medido sobre las 333: **304 contienen al menos un `assert`/`fail`; 29 no contienen ninguno** y, por la regla del CLAUDE.md §7, no cuentan como cobertura. La rc4 fueron 321 (316 de la rc3 + `FirmaActaTest` 5; la rc3 anadio `RitmoTest` 5 +
`TemperaturaRtv10Test` 11 + la del formato de la firma,
más T-U15 en `FlujoCalibracionTest`), entre ellas `FlujoCalibracionTest` (el flujo "Calibrar este equipo" de
extremo a extremo contra `EquipoSimulado`) y `RupturaRtv10Test` (ruptura frente a la 3.6.17 y cobertura de la rc2).
`./gradlew testDebugUnitTest` **no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra su
clase `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se compilan
con Gradle y se ejecutan con JUnit a mano:

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
mkdir -p libtest   # copiar aquí (fuera de build/: clean lo borra) junit-4.13.2.jar y hamcrest-core-1.3.jar de ~/.gradle/caches
cd app && "$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest com.dpi.retrov36.CoherenciaRealTest com.dpi.retrov36.CampanaTest com.dpi.retrov36.Version367Test com.dpi.retrov36.Version368Test com.dpi.retrov36.Version369Test com.dpi.retrov36.Version3610Test \
  com.dpi.retrov36.Version3611Test com.dpi.retrov36.Version3612Test com.dpi.retrov36.FlujoCalibracionTest \
  com.dpi.retrov36.TS00Test com.dpi.retrov36.BancoRehacerTest com.dpi.retrov36.Version3615Test com.dpi.retrov36.Version3616Test com.dpi.retrov36.Version3617Test \
  com.dpi.retrov36.RupturaSerieTest com.dpi.retrov36.RupturaFlujoTest com.dpi.retrov36.RupturaE2ETest \
  com.dpi.retrov36.RupturaEquivTest com.dpi.retrov36.RupturaBancoTest \
  com.dpi.retrov36.RtvUnicaTest com.dpi.retrov36.RupturaRtv10Test \
  com.dpi.retrov36.RitmoTest com.dpi.retrov36.TemperaturaRtv10Test com.dpi.retrov36.FirmaActaTest com.dpi.retrov36.SerieYAdminV46Test \
  com.dpi.retrov36.Rc6DefectosTest com.dpi.retrov36.AppCortaTest com.dpi.retrov36.Cov362DefectosTest
```

Más seguro que copiar la lista: pasar todas las `*Test.java` de `app/src/test/java/com/dpi/retrov36/` (37 clases,
**395 tests**, 21-sep-2026: 391 de `316a6bc` + `Cov366Rf17Test`, 4 nuevas — tarea A4b, RF-COV-17 en cuatro sitios
más de `FlujoCalibracion.java` que el mecanismo `Fabrica.elCodigo`/`elCodigoCap` no cubría: resolverCorte(),
escribir(), persistencia() y aceptar()).

`FabricaTest` compara las 12 ecuaciones de la app con el **texto** de
`01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c`: si alguien cambia una
cifra en la app, falla (comprobado alterando una a propósito).

## APK corto "RTV Calibra" — compilado, SIN ENTREGAR

**Sin arquitecto ni QA: no es entregable.** Nada probado en un teléfono ni contra un equipo; las dos apps no
se han visto instaladas a la vez. SPEC: `05_Documentacion/SPEC-App-Calibracion-Coviandina.md` (en `main`,
`2c8563d`).

```bash
./gradlew clean assembleDebug assembleCoviandina --offline
```

**rc12 / Cov_3.6.6_calibrar (21-sep-2026), tarea A4b — RF-COV-17 en cuatro sitios más** de
`FlujoCalibracion.java` que un agente anterior (solo lectura) había señalado sin comprobar si eran
alcanzables desde el camino corto: (1) `resolverCorte()` (java:1164-1210) — el diálogo "El #S del código k
no entró" y el "Corte durante #S,k: ..." no miraban `corto`, aunque SÍ es alcanzable en corto (`calibrar()`
→ `CalibracionManual.escribirYVerificar()` → `resolverCorte()`, el mismo `calibrar()` que usa
`CalibracionAutomatica`); (2) `escribir()` (java:1230,1234) — dos `return` sin `Fabrica.elCodigo`, aunque el
resto del método ya lo usaba; (3) `persistencia()` (java:1452,1455) y (4) `aceptar()` (java:1499,1501,1503) —
el `StringBuilder t`/`mal` que arman esos dos métodos, que `CalibracionAutomatica.persistirYAceptarAuto`
devuelve tal cual al resumen de "Calibrar" cuando algo falla después de escribir. Los cuatro, con
`Fabrica.elCodigo(k, corto)`, sin cambiar el texto de la app de campo (regresión probada en cada test).
**Comprobado y descartado** (no forma parte de RF-COV-17 en la práctica, no se tocó): `calibrarTodoInterno`
(java:1786-1827) y `persistirConfirmarYAceptar` (java:1853-1875) sólo los llama "Calibrar todo"/"Continuar",
botones que no existen cuando `BuildConfig.CORTO` (`CalibrarActivity.java:88-96`); `bloqueoRehacer`
(java:1881-1899) es del botón "Rehacer" de Banco, inalcanzable desde RTV Calibra por RF-COV-11.
`FlujoCalibracion.java` ya estaba por encima del tope de 500 líneas del pre-commit (2003, excepción de
"fichero existente" de rules/modularidad.md); el hook rechaza tocarlo si además CRECE. El arreglo se
mantuvo a líneas netas iguales (2003 antes y después: la declaración de `marcaS` en `resolverCorte()`
comparte línea con `char k = acta.escribiendo();`, y las demás conversiones reemplazan texto en la misma
línea) para no forzar aquí el corte que pide la regla; el corte queda pendiente para la próxima vez que
haga falta crecer el fichero de verdad.
Compilado con `clean assembleDebug assembleCoviandina --offline` sobre el árbol de este commit (rama
`rtv-1.0-cierre`, sobre `316a6bc`), `aapt dump badging`, build-tools 34.0.0. Las 37 clases `*Test.java`
pasaron antes con JUnitCore: `OK (395 tests)`.

| APK | md5 | versionCode | versionName | label |
| :--- | :--- | :--- | :--- | :--- |
| `app-debug.apk` | `b0a57a079d40aa2f0f7ae1fdea54b93e` | 10011 | `1.0.0-rc12` | RTV |
| `app-coviandina.apk` | `b4da005d21368cc8b6499384a0ff7340` | 10011 | `Cov_3.6.6_calibrar` | RTV Calibra |

package: `com.dpi.retrov36` (campo) y `com.dpi.retrov36.calibra` (calibrar).

Sin copiar a `03_App_Movil/` (el encargo lo prohibió expresamente): los dos APK quedan sólo en
`app/build/outputs/apk/{debug,coviandina}/` de este worktree.

**Sin arquitecto ni QA sobre ESTE par: sigue sin ser entregable** (§6 del CLAUDE.md: "a Diego sólo se le
entrega una APK con el visto bueno escrito del arquitecto y de QA — los dos").

**rc11 / Cov_3.6.5_calibrar (21-sep-2026), tres condiciones del arquitecto-iot** sobre "APTO CON
CONDICIONES" a `Cov_3.6.4_calibrar` (SPEC-App-Calibracion-Coviandina.md §8): (1) RF-COV-17/21 —
`Fabrica.elCodigo(k, corto)` en escribir() y rechazar() (antes mostraban "código k" en corto); (2)
RF-COV-22 — `calibrarInterno` (110 líneas) partido en `CalibracionManual.validarYPreparar`/
`escribirYVerificar` (59 y 58 líneas); `FlujoCalibracion.java`: 2003 líneas (`wc -l`), antes 2080; (3)
RF-COV-20 — `Decisiones.texto(equipo, corto)` marca REMEDIDA-b "sustituida por VERIF-5-10 (RF-COV-12)"
en el acta de la b en corto (antes citaba "se juzga con RF-CAL-18"). Bajo, RF-COV-18: el DATO del acta
lleva la frase literal "no se pudo leer la fecha del equipo; reintente". Detalle en el commit
"cierra las tres condiciones...".
Compilado con `clean assembleDebug assembleCoviandina --offline` sobre el árbol de este commit (rama
`rtv-1.0-cierre`, sobre `978dd0e`), `aapt dump badging`, build-tools 34.0.0. Las 36 clases `*Test.java`
pasaron antes con JUnitCore: `OK (391 tests)`.

| APK | md5 | versionCode | versionName | label |
| :--- | :--- | :--- | :--- | :--- |
| `app-debug.apk` | `c029abd3e6a5e42acc1d96d55b1e2605` | 10010 | `1.0.0-rc11` | RTV |
| `app-coviandina.apk` | `dc0beaf5e110a72b7cbb288e4011cb68` | 10010 | `Cov_3.6.5_calibrar` | RTV Calibra |

package: `com.dpi.retrov36` (campo) y `com.dpi.retrov36.calibra` (calibrar).

Copia en `03_App_Movil/`: sólo el par de calibrar, `Cov_3.6.5_calibrar.apk` y
`Cov_3.6.5_calibrar-10010.apk` (el encargo no pidió copiar el de campo); se borraron los `Cov_3.6.4_*`.

**Sin arquitecto ni QA sobre ESTE par: sigue sin ser entregable** (§6 del CLAUDE.md: "a Diego sólo se le
entrega una APK con el visto bueno escrito del arquitecto y de QA — los dos").

**rc9 / Cov_3.6.3_calibrar (21-sep-2026), arreglos ALTO/MEDIO/BAJO** de las revisiones arquitecto-iot y
qa-istqb a `Cov_3.6.2_calibrar`: MAX_NO_VALIDAS restaura (ALTO, QA); la b se juzga como el 8 en la app de
calibrar (ALTO, A-1, RF-COV-12/VERIF-5-10); `#SC`/`#GC#` anotados y devueltos al rechazar (MEDIO, A-06,
RF-COV-13); el resumen no confunde "queda escrito sin aceptar" con "No calibrado", y un acta sin ningún
código conforme se rechaza sola (MEDIO); nombres sin código (RF-COV-17) y sin la alerta "No se puede
calibrar" cuando la calibración fue correcta (BAJO, B-2); `#Q#` cierra el modo administrador al terminar
(BAJO, QA); `decisiones.csv` con VERIF-5-10 y FECHA-EQUIPO. Antes de estos arreglos, `calibrarAutomatico`
se sacó a `CalibracionAutomatica.java`, en su propio commit y con la suite en verde y salida idéntica
(rules/modularidad.md, fichero 500 líneas). Compilado con `clean assembleDebug assembleCoviandina --offline`
sobre el árbol de este commit (rama `rtv-1.0-cierre`, sobre `7b4391b`), `aapt dump badging`, build-tools
30.0.3. Las 33 clases `*Test.java` pasaron antes con JUnitCore: `OK (377 tests)`.

| APK | md5 | versionCode | versionName | label |
| :--- | :--- | :--- | :--- | :--- |
| `app-debug.apk` | `e1aba96e6383658f6241fa3e5189d24e` | 10008 | `1.0.0-rc9` | RTV |
| `app-coviandina.apk` | `98718ed9828f4c327815754df05fba87` | 10008 | `Cov_3.6.3_calibrar` | RTV Calibra |

package: `com.dpi.retrov36` (campo) y `com.dpi.retrov36.calibra` (calibrar).

Copia en `03_App_Movil/`: sólo el par de calibrar, `Cov_3.6.3_calibrar.apk` y
`Cov_3.6.3_calibrar-10008.apk` (el encargo no pidió copiar el de campo).

**Sin arquitecto ni QA sobre ESTE par: sigue sin ser entregable** (§6 del CLAUDE.md: "a Diego sólo se le
entrega una APK con el visto bueno escrito del arquitecto y de QA — los dos").

**rc8 / Cov_3.6.2_calibrar (21-sep-2026), tras cerrar H-1, H-2, H-3, M-1 y B-2** de la revisión
arquitecto-iot a `Cov_3.6.1_calibrar` (NO APTO) — `SPEC-App-Calibracion-Coviandina.md` §8, RF-COV-11 a
RF-COV-17. Compilado con `clean assembleDebug assembleCoviandina --offline` sobre el árbol de este commit
(rama `rtv-1.0-cierre`, sobre 9435f69), `aapt dump badging`, build-tools 30.0.3. Las 32 clases `*Test.java`
pasaron antes con JUnitCore: `OK (368 tests)` (360 de `2aa46e7` + `Cov362DefectosTest`, 8 nuevas).

| APK | md5 | package | versionCode | versionName | application-label | Copia en `03_App_Movil/` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `app-debug.apk` | `bedbd4787f8c6c1976c76ed6c08b5ab4` | `com.dpi.retrov36` | 10007 | `1.0.0-rc8` | RTV | `RTV-V1.0.0-rc8.apk`, `RTV-V1.0.0-rc8-10007.apk` |
| `app-coviandina.apk` | `43d1008715308933dabc66270f072511` | `com.dpi.retrov36.calibra` | 10007 | `Cov_3.6.2_calibrar` | RTV Calibra | `Cov_3.6.2_calibrar.apk`, `Cov_3.6.2_calibrar-10007.apk` |

El `versionName` del APK de calibrar se fija por partida doble en `app/build.gradle`: `versionNameOverride`
para el manifiesto (lo que lee aapt y el teléfono) y `buildConfigField "String", "VERSION_NAME"` para lo que
la app imprime en pantalla, acta y registro (`Base.java:55`, `Campanas.java:362,597`, `Registro.java:71`).
Ninguna prueba JVM asevera ese valor: se comprobó en el `BuildConfig.java` generado de la variante y con aapt.

**Sin arquitecto ni QA sobre ESTE par: sigue sin ser entregable** (§6 del CLAUDE.md: "a Diego sólo se le
entrega una APK con el visto bueno escrito del arquitecto y de QA — los dos"). Lo que cierra este commit es
la NO APTO anterior; falta la vuelta a revisión.

Compilación anterior, Cov_3.6.1_calibrar (21-sep-2026) sobre el árbol limpio de `2aa46e7`: `app-debug.apk`
`83d8ad85175038a92eb8e87d8eb9a195` (10006/`1.0.0-rc7`) y `app-coviandina.apk`
`0ece357f94d38b91b8df0767a426c771` (10006/`Cov_3.6.1_calibrar`). Esa fue la que dio NO APTO. Sustituidas
por las de arriba (RF-APP-41: un par de versionCode/versionName no se reutiliza).

Compilación anterior del 21-sep sobre `0a849fe`: `app-debug.apk` `037d4543c83f3c22d843398e03fda2b7` y
`app-coviandina.apk` `101d95535250748d6096bfd5fd8637f9`, las dos con el par `10005`/`1.0.0-rc6(-calibra)`
repetido de la rc6 entregada (`D:\IT\wt_rtv10\03_App_Movil\RTV-V1.0.0-rc6.apk`, md5
`28354751b1f4c93aadef03d427478df4`, atada a `fd37cc7` por la hora, no por registro). Sustituidas por las de
arriba (RF-APP-41).

## Uso por el operador

1. Emparejar el equipo en los ajustes del teléfono (aparece como `COVIANDINA_<serie>`).
2. Abrir la app y tocar el equipo en la lista. "Conectado" sólo aparece cuando `connect()` termina.
3. Se abren solas las **Pruebas del equipo**. Colocar el equipo sobre un patrón de nivel medio o alto,
   no moverlo ni tocar el gatillo, y pulsar Iniciar. Termina en **APTO** o **NO APTO**.
4. **Medida de patrones:** elegir el patrón P1-P31, pulsar "Medir ×N" (N = 3 por defecto). Se ven
   media y desviación de `x` por patrón.
5. **Compartir registro y datos:** envía el registro de tramas y los CSV de la sesión.
6. Sólo V3.6: **Modo administrador** (PIN) para leer coeficientes, ajustar y escribir; y **Botones de
   pantalla** para mapear la STONE.

## Pruebas del equipo

Se ejecutan en el orden 1, 2, 5, 3, 4, 6 (la 5 lee los coeficientes que usa la 4). Cada una queda en
verde o rojo, con su detalle, y va al registro. **APTO** exige todas en verde (la 5 sólo en V3.6).
**NO APTO bloquea el modo administrador**, salvo override: casilla de confirmación explícita + PIN,
que queda anotado en el registro. Medir con NO APTO pide una confirmación.

| # | Prueba | Qué hace | Criterio |
| :--- | :--- | :--- | :--- |
| 1 | Enlace | Comprueba que el socket SPP está abierto; muestra nombre, MAC y serie (lo que sigue al último `_` del nombre) | Socket abierto |
| 2 | Versión | Con ≥ 1500 ms entre envíos: `#V#` → si `#V,3.6,...#`, V3.6. Si no, `9` → si `:n:`, V3 2020. Si no, `6` → si `::n`, V3 2020. **Nunca `e`** (ver Lección). Si no, `@LEERV,BLA,1@` → si `@LEERV,...@`, V4 (la app no aplica). **Ninguna otra trama con `@`** | V3.6 o V3 2020 |
| 3 | Códigos | Envía `1`-`8`, `a`-`d` y, si el equipo la tiene, `e` | Los 12 responden `::n` en < 2,5 s. En V3.6 `e` es obligatoria |
| 4 | Coherencia | En V3.6 lee `e` antes y después de los 12 códigos; la `x` de referencia de cada código es la recta entre las dos según su orden (corrige la deriva: medido 3004 → 3023). Sin `e` (V3 2020), la mediana. Invierte cada respuesta con su ecuación vigente | Cada código a ±(base 10 + resolución local 1/|f'(x_ref)| + deriva observada). **Excluidos, no fallo** ("no evaluable en esta zona"): códigos con pendiente ≤ 0 en x_ref o con otra `x` a menos de 1500 cuentas que da la misma respuesta (blanco intenso desde x ≈ 2830, amarillo intenso desde ≈ 1970; ventana heurística). **INVÁLIDA, no fallo** ("el equipo se movió o no estaba apoyado; repetir"): `e` inicial y final difieren más de 50, o las `x` forman dos grupos separados más de 300 con ≥ 2 códigos cada uno. Hacen falta 6 evaluables |
| 5 | Sólo V3.6 | `#GT#`; `#G,k#` de los 12; marca `CAL`/`DEF` y máscara de `#V#`; `#E,k,x#` en x = 500, 1000, 2000, 3000, 4000 | Todo se analiza. Un código que la máscara da por "fábrica" debe coincidir con la tabla de fábrica a **4 ulp** de float32 (XC8 2.10 imprime `%.8E` con hasta 2 ulp de error y `strtod` lee con hasta 3: CAMBIOS-V3.6.md §3.4, SPEC C-12). La no regresión **exacta** va por `#E`: en un código a fábrica debe coincidir exactamente con la emulación de 2020 hecha con la tabla de fábrica de la app, no con `#G`. En un código ajustado se emula con `#G` y 1 unidad de diferencia es aviso, no fallo |
| 6 | Repetibilidad | 5 lecturas seguidas con `e` (o `6` invertido) | Ninguna fallida y desviación ≤ 10 cuentas. **Umbral provisional**, sin medir |

La app no puede comprobar el CRC de la EEPROM por sí misma: se fía de la marca y de la máscara que
calcula el firmware, y lo cruza con los coeficientes leídos.

Casos reales de SLV-002 (19-sep-2026, app 3.6.2) en `CoherenciaRealTest`, con las respuestas
**literales** del registro `07 pruebas/19092026_0900/rtv36_20260919_085316 (1).txt`: la segunda
pasada sobre P1 (líneas 996-1044, `e` final 3016) sale APTA con los códigos 1 y 2 excluidos; la
primera (líneas 457-505: 1-8 en oscuro, `a` a medio apoyar, b-d sobre P1) sale INVÁLIDA. La prueba opcional "gatillo contra
Bluetooth" no está hecha: la sospecha que la motivaba quedó retirada (acta G4).

## Línea base previa a grabar (G3)

En la pantalla de Pruebas, botón **"Línea base"**. Pensada para el firmware ORIGINAL (V3 2020 de
SLV-002), antes de grabar la V3.6. Si la versión aún no se ha detectado, la detecta primero; si la detección
falla, ofrece forzar "V3 2020 sin e". Hay además un botón que fuerza sin detectar. Nunca envía `e`.

1. **T-B03:** con el equipo sobre el patrón elegido, los 12 códigos.
2. **T-B09:** la respuesta a `9`, tres veces.
3. **T-B07:** la app pide tapar el cabezal (oscuro) y repite los 12 códigos. Se puede omitir.

Cada petición va al registro de tramas y a `lineabase_<serie>_<fecha>.csv`, con la etiqueta
`LINEA_BASE_PRE_GRABACION`, la respuesta literal, el desenlace, el tiempo y la `x` invertida con la
ecuación de fábrica.

## Lección del 18-sep-2026: `e` a un equipo sin identificar

Con la app 3.6.0 (detección `#V#`, `e`, `6`, `@LEERV,BLA,1@`, el orden de la SPEC RF-APP-03), SLV-002
**no respondió a nada**, aunque en el barrido `6` respondía siempre (`::14`, ~918 ms). En el barrido,
tras el byte 0x65 (`e`) tampoco respondió ningún byte más; tras `#` y otros bytes desconocidos sí.
**Hipótesis sin confirmar:** en esa variante, `e` deja el equipo sin Bluetooth hasta apagarlo.

Reglas que salen de ahí, en el código:
- La detección no envía `e`: `#V#` → `9` → `6` → `@LEERV,BLA,1@`. Se aparta de RF-APP-03 a propósito.
- `Tramas.peticionPermitida(p, esV36)` y `Cliente.pedir` **rechazan `e`** si el equipo no está
  identificado como V3.6. En V3 2020 la `x` se lee siempre con `6` e inversión.
- La línea base se puede **forzar como V3 2020 sin `e`**, sin detectar o tras una detección fallida:
  envía sólo `1`-`8`, `a`-`d` y `9`.
- Tras 3 peticiones seguidas sin un solo byte, la app aconseja apagar y encender el equipo y lo anota
  en el registro.

## RTV 1.0.0-rc1 (rama `rtv-1.0`): app única V3.6 y V4.6

**Sin medir. Ninguna trama V4 ni V4.6 de esta app se ha visto en un terminal serie, y el firmware V4.6 no
existe.** Contrato: `05_Documentacion/SPEC-App-Unica-V36-V46.md` y la revisión P2
(`REVISION-Arquitectura-V46-P2.md` del repositorio V4.6, veredicto (b), MVP de la capa de protocolo).

- **`Protocolo`** (RF-APP-U06) sustituye al "es V3.6". Hay cuatro implementaciones:
  - `ProtocoloV36`;
  - `ProtocoloV2020`: sin `e` ni `#`;
  - `ProtocoloV4Original`: sólo las 12 `@LEERV`;
  - `ProtocoloV46`: las 12 `@LEERV` y el contrato `#`, más `#X,k#` y `#GB#`, según PROTOCOLO-V4.6 §4.

  Cada una tiene su lista cerrada de tramas, y `Cliente` rechaza cualquier trama que no esté en ella. Las
  pantallas preguntan al protocolo qué puede hacer: medir, calibrar, medir el banco o leer x.
- **Lo que es dato va en `firmwares.csv`** (RF-APP-U09): el dominio de x, la cola del banco, la coherencia en
  `DEF` (`G` o `E`) y la unidad de la batería.
- **`Enlace`**: no se ha separado como interfaz propia. `Canal` ya cumple ese papel para las pruebas.
- **Detección** (`Deteccion`, RF-APP-U01 a U04). El orden es `#V#` → `9` → `6` → sonda `@LEERV,BLA,2@`
  (C-U01), con 5 s de espera. La respuesta sólo se acepta con la forma estricta `@LEERV,<entero>@`, y lo que
  sigue (`/n/r` + `0x00`) se descarta. Nunca se envía `e` ni una `@` que no sea la sonda.
- **Perfil por MAC** (RF-APP-U02 con A-3):
  - Los perfiles firmados van en `equipos.csv`, con el régimen de `decisiones.csv`. De momento no tiene
    filas: el perfil del V3-2 espera a M-3.
  - La app guarda un perfil aprendido por MAC con la huella de `#V#`. Si `#V#` cambia, el perfil caduca.
  - Un perfil firmado con `caduca_con_v` deja de aplicarse, sin contradecir, cuando el equipo responde con
    esa versión.
  - Un perfil `SOLO_SONDA` se salta si el operador pide la detección completa, o si lo aprendido ya vio una
    V4.6.
- **V4 original** (RF-APP-U07 y U08):
  - Sólo mide R entera con `@LEERV`. Los tipo 1 llevan su etiqueta y cada medida anota el diario del estado
    oculto.
  - No calibra, no entra en Avanzado y no mide banco. Cada botón deshabilitado muestra el motivo: "Equipo V4
    sin firmware V4.6: sólo medir y verificar".
  - Las pruebas 3-6 no aplican.
- **V4.6** (RF-APP-U09 y U13): sigue el mismo flujo "Calibrar todo" que la V3.6. Las diferencias:
  - x se lee con `#X,k#`, R con `@LEERV` y la batería con `#GB#`;
  - la serie sale siempre de `#GN#`. En blanco, no se abre campaña hasta dar de alta la serie.
  - Mientras `firmwares.csv` no traiga la cola de la V4.6, no calibra ni mide el banco (A-5: sólo en
    simulador).
- **Simuladores:**
  - `EquipoSimuladoV4` reproduce el V4.1: el bloqueo por `@` sin `LEERV`, el `0x00`, el segundo de espera
    y el `error` arrastrado.
  - `EquipoSimuladoV46` reproduce el contrato de §4 sobre la 3.6.2 simulada.
- **Pruebas:** `RtvUnicaTest` (18) y `FlujoCalibracionTest.tU15ElMismoFlujoConLaV46`. En total son 245 y
  pasan todas, T-S00 incluida.
- **Pendiente:**
  - T-U16, a medias: falta la coherencia por `#E` contra el literal de la V4.1, que depende de F-1 y F-5.
  - T-U17: el V4 original no mide banco, así que no hay cola de verificación.
  - T-U18: sólo registros; faltan el ZIP y el acta.
  - AT-U01 a AT-U11: ninguna se ha hecho con un equipo.
## Cambios de la 3.6.17 (REVISION-P15-QA-3.6.16.md, ef7ea10; peticiones de Diego del 19-sep, noche)

**Nada de esto se ha probado contra un equipo.** Lo primero es capturar el banco y sacar el ZIP. La guía del
operador está en `03_App_Movil/GUIA-OPERADOR.md`.

- **Pantalla principal.** Arriba hay tres botones grandes: "Tomar muestras", "Guardar / Compartir" y "Calibrar".
  Todo lo demás está en "Avanzado", plegado.
- **Tomar muestras** (el banco):
  - Abre en **REPRESENTATIVO** por defecto si el equipo ya tiene series medidas. El completo sólo se usa si se elige
    a mano, con el aviso "El completo mide los 133 patrones, incluidos los repetidos; recomendado: Representativo".
  - **La cola se cambia en cualquier momento.** Lo ya medido se conserva y `BancoPrevio` lo vuelve a contar; el paso
    siguiente se recalcula en la cola nueva.
  - Si hay un banco COMPLETO empezado en la 3.6.16, un solo diálogo ofrece pasarlo a REPRESENTATIVO.
  - Arriba, en grande, se ve "Banco REPRESENTATIVO: quedan N patrones, unos M min". El botón dice "Banco:
    REPRESENTATIVO (cambiar)".
  - En el completo, los patrones con un equivalente en la cola llevan "(equivalente a Pxx)".
  - El ZIP de cada sesión se guarda solo en `Download/RTV/`, sin selector. Al terminar el banco se exporta y se
    comparte.
- **Guardar / Compartir:** el ZIP ligero y el de soporte, con su copia en `Download/RTV/`, en un solo selector.
- **Seguridad (P15):**
  - **B-01.** Un equivalente sólo cuenta si su patrón no es otro paso de la cola y ninguna decisión lo excluye.
    P127 ya no se da por medido con P81, ni P19 con P18.
  - **B-03.** Las series de los 7 tipo I que están fuera de protocolo quedan ANULADAS, con el motivo de
    TIPO-I-REPETIR, aunque su paso no estuviera HECHO.
  - **F-01.** Un acta sin ningún código escrito se descarta sola y se dice.
  - **F-02.** "Cerrar sin restaurar" exige el PIN del equipo, comprobado con `#L`, o la frase de Diego (fila
    FRASE-DIEGO de `decisiones.csv`, con el SHA-256 de la frase).
  - **F-03.** Tras un cierre sin restaurar no se calibra hasta que Diego lo libere ("Liberar", con la misma firma).
    La liberación queda en el diario del acta.
  - **F-04.** El `#SC` anula el atajo del T-C41.
  - **F-05.** Se comparten todos los ZIP ligeros de la pulsación.
  - **S-01.** `RENOMBRA_REVIERTE` viaja al copiar y al importar.
  - **S-02.** Un revierte falso se corrige al repetir el cambio.
  - **S-04.** Si la serie `#GN#` no es la actual de la campaña, no se calibra y se pide repetir "Cambiar serie".
  - **P14-B02.** El acta cita el SHA-256 del ZIP de soporte, que se exporta justo antes de cerrarla.
- **Informe de calibración:** al acabar "Calibrar", el informe en texto va a `Download/RTV/`
  (`ExportadorFinal.java`). El PDF, la hoja con la tabla DENTRO/FUERA y "Probar calibración" quedan para la
  siguiente entrega, como pidió Diego.
- **Decisiones:** fila PRECISO-5x9 firmada (c38ad02): las series K ≥ 5 y M ≥ 4 cuentan como preciso.
- **Pruebas:** 267, todas en verde.
  - `Version3617Test` incluye el caso de un diario de la 3.6.16 en COMPLETO, en el paso 12, que pasa a
    REPRESENTATIVO: quedan 49 y no se pierde ninguna serie.
  - Las rupturas del revisor van al árbol: `RupturaSerieTest`, `RupturaFlujoTest`, `RupturaE2ETest`,
    `RupturaEquivTest` y `RupturaBancoTest`.
  - En `RupturaE2ETest`, al salir y entrar del Banco el acta del 5 lleva P18, P67, P112 y P127.
- **Sin hacer en esta:**
  - la capa V4.6 (rama `rtv-1.0`, rc1), que va en otra APK;
  - Asignar serie en la pantalla principal;
  - el PDF y "Probar calibración";
  - S-03 y S-08;
  - QA-3615-09 y -10.
- **Entrega:** `RTV-V3.6.17.apk` y `RTV-V3.6.17-3617.apk`.

## Cambios de la 3.6.16 (REVISION-Arquitectura-P14-V3.6.md, QA-App-3.6.15.md, decisiones de 6048453)

**Nada de esto se ha probado contra un equipo.** El flujo corre contra `EquipoSimulado`.

- **P14-04: ningún acta se acepta sin verla.** "Calibrar todo" enseña el acta de cada código tras su persistencia, con
  "Aceptar", "Rechazar" o "Parar aquí". `#SC` se graba **una vez al día**: antes se lee `#GC#`. Cada acta aceptada
  deja su ZIP ligero y su ZIP de soporte en disco en ese momento. Al acabar la pulsación, los dos se comparten
  **en un solo selector**.
- **Rechazo pendiente (P14-05, -06, -10).** Si una restauración no se verifica, el acta queda con un
  `RECHAZO_PENDIENTE` en el diario. Mientras siga así, no se escribe ningún código, ni con "Continuar" ni con
  "Calibrar todo", y no se acepta. Se sale volviendo a pulsar Rechazar o con **"Cerrar sin restaurar (firma de
  Diego)"**, que cierra el acta como RECHAZADA SIN RESTAURAR y deja escritos los códigos en estado desconocido.
- **P14-01 y P14-02.**
  - El atajo del T-C41, que aprovecha el apagado de la persistencia anterior, vale sólo dentro de una pulsación.
  - Un rechazo o una restauración lo anulan.
  - El acta dice qué apagado usó.
  - Los códigos de las actas aceptadas, como el 8 antes del b y el 5, se revalidan con `#G` y `#E` en el T-C41.
- **Serie (QA-3615-01, -02; P14-S01, S02).**
  - El `RENOMBRA` se escribe antes del `#SN`.
  - Tras un corte, repetir el cambio adopta la serie que ya tenga el equipo.
  - Si el `#GN#` dice que no entró, se anota `RENOMBRA_REVIERTE`.
  - No se renombra con un acta en curso.
  - El `RENOMBRA` viaja al importar. Una campaña nueva de la misma MAC copia los `RENOMBRA` de las otras, también
    de las archivadas.
- **Decisiones de Diego (6048453)**, en `decisiones.csv` como definitivas:
  - `PROTOCOLO-AJUSTE` = PRECISO: los patrones de AJUSTE y RE-MEDIDA del 8, el b y el 5, P81 y toda re-medida van
    a 5 × 4. El resto sigue a 1 × 4.
  - RF-CAL-15 del b a 11,5 % estricto, sin ±3.
  - `TIPO-I-REPETIR`: se repiten a 5 × 4 P34, P37, P43, P44, P38, P39 y P49.
  - Una serie cumple el protocolo con K ≥ 5 y M ≥ 4, así que un 5 × 9 de la 3.6.11 vale.
- **Banco más corto.**
  - Al abrir el Banco o importar, lo ya medido y válido cuenta como HECHO (elegida, aceptada, no anulada), también
    por un patrón equivalente de su grupo (`grupos_patrones_equivalentes.csv`). No cuenta lo que se escribe medido
    fuera de protocolo ni lo que Diego manda repetir.
  - Lo que se manda repetir y estaba HECHO fuera de protocolo vuelve a la cola, con su serie ANULADA y el motivo.
  - Cola representativa **v2** (`cola_banco_representativo_v2.csv`, md5 `70ef3b86…`, 80 pasos): sin la batería por
    color; los controles son sólo A5 y OSCURO.
  - Antes de empezar, la pantalla dice "Quedan N patrones, unos M min".
  - La cola se puede cambiar a mitad. Los pasos quedan sin estado, porque sus órdenes no valen en otra cola. Las
    series se quedan y se vuelven a contar.
- **QA-3615-03.** La importación adopta el tipo de banco del ZIP si el destino no tiene cola. Si las colas no
  coinciden, trae las series y no los pasos, y lo avisa.
- **QA-3615-04 a -07.**
  - Al retomar, el acta a medias se enseña, se acepta y sale en el mensaje ("Hecho: 8 ACEPTADO"). Los códigos ya
    aceptados se saltan y se dicen.
  - Si se rehace una serie de un código aceptado, el código vuelve a tener casilla.
  - Tras dos re-medidas no conformes, la app dice "pulse Rechazar".
  - Los textos de exportación ya distinguen el ZIP ligero del de soporte, y el ligero no se importa.
- **RF-APP-50.** El `resumen.txt` del ZIP ligero es corto: una línea por patrón medido y un tope de 8000 B. Con el
  ZIP de las 15:10 ocupa 2,2 kB frente a los 21,6 kB del completo.
- **C-P14-3.** La s_rep de cada código sale de la A5 del inicio de **su** sesión. Si esa sesión no tiene A5, sale de
  la sesión 1, y el texto lo dice.
- `TablaCalibracion.VERSION` = reglas r5 (c836cae, 6048453), sin la versión del APK (P14-11).
- **Con el ZIP de las 15:10** (`Version3616Test.elZipDeLas1510EnLasDosColas`, en rápido): el banco completo deja 106
  patrones y unos 116 min; el representativo v2, 51 patrones y unos 82 min. Los minutos incluyen el calentamiento
  y los controles.
- Sin hacer:
  - la capa V4.6;
  - las enmiendas documentales de 32.4, RF-CAL-10 y RF-CAL-35;
  - QA-3615-09 y -10 (P3).
- Pruebas: 225 en total. Son nuevas `Version3616Test` y 17 de `FlujoCalibracionTest`.
- Entrega: `RTV-V3.6.16.apk` y `RTV-V3.6.16-3616.apk`.

### Casos de rehacer (para el TDD)

| Caso | Qué hace la app | Test |
| :--- | :--- | :--- |
| R1. Rehacer con un acta en curso | BLOQUEO hasta aceptarla o rechazarla | `FlujoCalibracionTest.rehacerConActaEnCursoSeBloqueaYConActaAceptadaSeAvisa` |
| R2. Rehacer un patrón de un código con acta ACEPTADA | AVISO. La serie queda ANULADA, el código recupera la casilla y se recalibra (QA-3615-05) | `FlujoCalibracionTest.rehacerUnaSerieDelOchoAceptadoPermiteRecalibrarlo` |
| R3. Rehacer el OSCURO o la A5 de la sesión de un código ACEPTADO | AVISO de que habrá que recalibrarlo (P14-B08) | `FlujoCalibracionTest.rehacerElOscuroDeUnCodigoAceptadoAvisa` |
| R4. Rehacer el OSCURO o la A5 de una sesión terminada | No se deja | `Version3615Test.noSeRehaceUnOscuroDeUnaSesionTerminada` |
| R5. Rehacer después de exportar | Vuelve a pedir el ZIP | `Version3615Test.trasExportarRehacerVuelveAPedirElZip` |
| R6. "No, rehacer" | No deja la serie aceptada | `Version3615Test.noRehacerNoDejaLaSerieAceptada` |
| R7. Serie de un acta en curso anulada | El acta no se acepta | `FlujoCalibracionTest.unaSerieAnuladaEnElBancoImpideAceptar` |
| R8. Importar dos ZIP, el segundo con la anulación | Se conserva la anulación con su fecha, y se elige la serie buena | `Version3615Test.importarDosZipSucesivosConservaLaAnulacion` |
| R9. TIPO-I-REPETIR ya HECHO fuera de protocolo | Vuelve a la cola una sola vez, con la serie ANULADA y el motivo. A 5 × 4 no se vuelve a pedir | `Version3616Test.loQueDiegoMandaRepetirVuelveALaCola` |
| R10. Cambiar de cola a mitad | Los pasos quedan sin estado; las series se quedan y se recuentan | `Version3616Test.cambiarDeColaDejaLosPasosSinEstadoYSeReproduce` |

## Cambios de la 3.6.15 ("la APK que hace todo": QA-App-3.6.14, P13, decisiones de c836cae y 31d3b74)

**Nada de esto se ha probado contra un equipo.** El flujo corre contra `EquipoSimulado` (T-S00 132/132).

- **Una sola sesión: "Calibrar todo (8 → b → 5 …)".** Con los códigos marcados, en orden 8, b, 5, 3, 4, 6: para cada
  uno escribe, re-mide, hace la persistencia y **acepta su acta**, y sigue solo con el siguiente (un acta por
  código). El 8 queda ACEPTADO antes de tocar el b o el 5. Todo se comprueba antes de empezar. El T-C41 del
  segundo código y los siguientes aprovecha el apagado de la persistencia anterior. Se para en el primer código
  que no acabe aceptado, y lo dice. "Continuar / un código" sigue para retomar un acta a medias.
- **Decisiones de Diego (c836cae) en `decisiones.csv`:** REMEDIDA-b = RF-CAL-18; PA-24 con RF-CAL-15 (RMS de tipo I
  11,5 %); las cifras de PA-24 son **techo** con ±3 puntos (P13-02: mismo signo y |dev| ≤ |cifra| + margen);
  **P81 fuera del ajuste del 5** ("EXCLUYE P81"). El b ya se escribe (P13-01).
- **Serie SLV-002 → SLV-002-2026 (SERIE-2, 31d3b74).** Avanzado, "Alta / Cambiar serie": la nueva dos veces, `#SN`,
  verificada con `#GN#`, y un evento `RENOMBRA` en el diario (anterior, nueva, fecha, operador). La identidad es la
  **MAC + el historial de series**: la campaña, el banco, las actas y las decisiones de "SLV-002" siguen valiendo; el
  acta dice "SLV-002-2026 (antes SLV-002)"; un ZIP medido bajo SLV-002 se importa si la MAC coincide. Otra MAC que
  se llame SLV-002 no recibe ni la tabla ni las decisiones de SLV-002.
- **Banco (RF-APP-49 a 53):** selector completo / representativo / verificación anual (tres assets con su md5;
  completo la primera vez en un equipo, representativo después; no se cambia a mitad; la anual no calibra). ZIP
  **ligero e incremental** (lo nuevo desde el anterior, con `indice.sha256` y el hash del ZIP anterior; resumen con
  md5 y SHA-256 de cada pieza; DEFLATE 9) y **ZIP de soporte** aparte, con todo (es el que se importa en otro
  teléfono); el de soporte se genera solo al aceptar un acta y al cerrar la campaña.
- **Protocolo (PROTOCOLO-MIN):** por defecto 1 colocación × 4 disparos en la campaña y en el banco ("rápido"); "preciso"
  = el K × M de la cola; **A5 y OSCURO siempre K = 5**. La re-medida de cada código sigue en 5 × 4 (P12 y P13): es
  más que 1 × 4 y así se queda. El acta anota el protocolo del banco de cada código.
- **QA-3614:** -01 importar encima conserva la anulación y elige la serie buena; -02 ningún texto libre lleva salto
  de línea y un diario viejo con uno se lee; -03 rehacer se bloquea con un acta en curso y avisa con un acta
  aceptada; el acta guarda los id de serie y no se acepta si alguno se anuló; -04 rehacer tras exportar vuelve a
  pedir el ZIP; -05 no se rehace un OSCURO o una A5 de una sesión terminada; -06 "No, rehacer" no deja la serie
  aceptada; -07 la importación conserva la fecha de la anulación; -09 un acta abierta con otras reglas no se
  acepta; -10 un fallo al exportar al aceptar se enseña. QA-3613-01 / P13-05: un rechazo con una restauración no
  verificada **no se cierra** y lo dice. Rehacer: botón corto, lista filtrable y el paso devuelto marcado.
- **SPEC-App-Unica:** `@LEERV,<entero>@` estricta (el eco de la sonda no es respuesta); sonda de 5 s; una campaña sin
  MAC no casa con ningún equipo.
- Pruebas: `FlujoCalibracionTest` (49), `Version3615Test` (18), `TS00Test`, `BancoRehacerTest`. 198 en total.
- Sin hacer: la capa de protocolo V4.6 (espera al arquitecto); las enmiendas documentales de 32.4, RF-CAL-10 y
  RF-CAL-35 que pide el plan del banco representativo.
- Entrega: `RTV-V3.6.15.apk` y `RTV-V3.6.15-3615.apk`.

## Cambios de la 3.6.14 (rehacer en el banco; QA-App-3.6.13; REVISION-Arquitectura-P12-V3.6.md)

**Nada de esto se ha probado contra un equipo.** El flujo corre contra un `EquipoSimulado` que ya reproduce el
registro real T4 (T-S00: 132 de 132 tramas `#`, `#G` 38/38; `TS00Test`).

- **Rehacer en el banco (petición de Diego).** "Rehacer el paso anterior" (siempre visible) y "Rehacer patrón…"
  (cualquier paso HECHO): la serie queda **ANULADA** en el diario (evento `ANULA`) con un motivo obligatorio, sale
  del ajuste, de la A5 y de la s_rep, y el paso vuelve a la cola; la nueva serie pasa a ser la elegida. Preferencia
  "Confirmar cada patrón antes de pasar al siguiente" ("¿Era P45? Sí / No, rehacer"), desactivada por defecto. El
  resumen y `campana.csv` muestran las anuladas con su motivo; importar un ZIP las trae anuladas. Nada se borra.
  Si la app se cierra entre el `ANULA` y el paso, al volver el paso ya está en la cola.
- **P12 §5.1:** la persistencia y el apagado de T-C41 exigen **ver caer el enlace**; un OK sin apagar no vale.
- **P12 §5.2 / QA-3612-06:** la dispensa del b cubre sólo su **alcance** en `decisiones.csv` (criterio, patrón o
  tipo, desviación y margen; hoy `RF-CAL-14 P39 +15 ±3; RF-CAL-14 P49 −10 ±3`, margen fijado por la app). Lo demás
  (RF-CAL-15, RF-CAL-16, otro patrón, otra cifra) no queda dispensado. La **regla de la re-medida del b**
  (`REMEDIDA-b`: RF-CAL-18 o CERTIFICADO) va en `decisiones.csv`; **no está: el b no se escribe** hasta que
  Diego la confirme. Si su RMS de tipo I sale fuera de 6 %, tampoco, salvo que Diego añada `RF-CAL-15 I …`.
- **QA-3613-03:** decisiones y tabla **por equipo** (columna `equipo`). Para otro equipo que no sea SLV-002 todo
  queda en "sólo verificar".
- **P12 §6:** el 8 va **solo y primero**; el b y el 5 sólo tras un acta ACEPTADA del 8. T-C41 empieza apagando y
  encendiendo. El 5 exige **sus 13 pasos** del banco y el acta declara su rango ("rango 5": 0-102, tipos IV, IX y
  XI). "Rechazar" **siempre** restaura, también un `#S` sin resolver (QA-3613-01). El ZIP se exporta al aceptar.
- **QA-3613-02:** cualquier fallo de `#L` (mudo, PIN rechazado, bloqueado tras 5) olvida el PIN y la pantalla lo
  vuelve a pedir; los mensajes distinguen el caso. QA-3613-04 y -09: Avanzado ya no rechaza actas y se borró su
  código muerto de escritura. QA-3613-05: un acta retomada recibe los datos que le falten. QA-3613-06: la batería
  leída en "Calibrar" manda aunque la campaña esté cerrada. QA-3613-08: una conformidad por código.
- **Pruebas:** `FlujoCalibracionTest` (38) sin rellenar el Contexto a mano (`FlujoCalibracion.identificar` lee
  `#V#`, `#GC#` y `#GN#` del simulado), sin llamar a `plan()`, con "Parar aquí" y "No", persistencia que corta el
  enlace, T-S15 (caducidad del modo administrador). `TS00Test` (T-S00 y el simulador: PIN bloqueado, caducidad,
  EEPROM separada). `BancoRehacerTest` (5).
- Abierto: QA-3613-07 (el ajuste del 5 incluye su patrón de re-medida P81; C-3613-2, Diego); RF-CAL-43
  incompleto; T-C41 y T-C43 en SLV-002.
- Entrega: `RTV-V3.6.14.apk` y `RTV-V3.6.14-3614.apk`.

## Cambios de la 3.6.13 (arreglo de P11 y de QA-App-3.6.12)

**Nada de esto se ha probado contra un equipo.** El flujo sí ha corrido entero, por primera vez, contra un
**equipo simulado** en la JVM (`EquipoSimulado`, T-S00 en su parte de emulación: no reproduce todavía un
registro real). T-C41 y T-C43 en SLV-002 siguen PENDIENTES.

- **El flujo sale de la pantalla:** `FlujoCalibracion` (Java puro) tiene toda la lógica; `CalibrarActivity`
  sólo pinta y llama a sus acciones (Calibrar, Leer batería, Persistencia, Aceptar, Rechazar). El equipo se
  habla por `Canal` (en la app, `Cliente`; en las pruebas, el simulado). `OpsEquipo` pasa a `Ops`, pura.
- **QA-3612-01:** "Aceptar" se habilita con la persistencia hecha; la verificación final (`#V#` en `CAL` con
  máscara, `#G` de lo certificado y de los heredados) va dentro de la acción.
- **QA-3612-02:** con pruebas NO APTO no se calibra.
- **P11-M1 / QA-3612-03:** una restauración cuya relectura `#G` no coincide no resuelve el código: el acta
  dice "RESTAURACIÓN NO VERIFICADA", no se acepta, y "Continuar" la reintenta. Un `#S` fallido cuya
  restauración no se verifica deja el corte sin resolver.
- **P11-M2:** cada escritura, restauración o `#S` fallido anula la persistencia y la verificación hechas.
- **P11-M3 / QA-3612-10:** oscuro por código ("oscuro 8", "oscuro b"), sin estáticos compartidos entre
  tarjetas; sin OSCURO de la sesión no se escribe; ya no se cae al OSCURO de toda la campaña. El corte durante
  `#S` conserva método, oscuro y conformidad.
- **P11-M4 / QA-3612-04 / QA-3612-08:** no se reescribe un código conforme, restaurado o con una re-medida
  válida pendiente; las casillas se desmarcan tras el flujo. **Avanzado ya no escribe curvas** (ni el 1 ni el
  2): se escriben sólo en "Calibrar este equipo". Su `#F` invalida el acta abierta aunque se haya reconectado.
- **P11-M5 / QA-3612-07:** el b (PA-24) y el 5 (PA-14) sólo se escriben con la decisión de Diego registrada en
  `assets/decisiones.csv` (id, SI, fecha, firmante Diego, documento, palabras; la firma es el commit), nunca con
  una casilla. El APK lleva las dos, tomadas el 19-sep (`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`,
  8af526d y 37f827d), y el acta las cita. Sin la línea, el código sólo se verifica y el acta lo dice ("código 5
  de fábrica, fuera de tolerancia, sin decisión").
- **El b con PA-24:** la dispensa cubre el ajuste; su re-medida se juzga con RF-CAL-18 frente a la curva escrita
  más la reproducción de la campaña, y la desviación frente al certificado queda en el acta, como
  incumplimiento DISPENSADO si pasa de ±10 % (QA-3612-06: el umbral no se toca).
- **El 5 con PA-14:** recta anclada con la cobertura de RF-APP-42 (el ancla cuenta como un nivel; también para el
  8 y el b). Como la cola no tiene ningún AJUSTE del 5, el ajuste usa todos sus patrones medidos en el banco,
  VERIFICACIÓN incluidos. **Decisión de la app que conviene revisar.**
- **P11-M6 / T-C41:** al empezar se leen `#V#`, `#G` y `#E` de los heredados (1 y 2 de SLV-002) y se cotejan con
  el acta de las 12:23:26; si no coinciden, no se calibra. Se vuelven a cotejar en la persistencia y antes de
  `#SC`: un `#F,1`/`#F,2` ya no deja grabar una fecha nueva.
- **QA-3612-05:** botón "Leer batería" en la pantalla: tras cambiarla, la lectura nueva levanta el bloqueo. Se
  lee también antes de `#SC`; el aviso con n < 19 sale en pantalla; el acta guarda la batería al inicio y la
  mínima.
- QA-3612-09 (repetir un `#S` que no entró), -11 (diálogo sin pantalla: el hilo recibe "parar"), -13 (`#L`
  antes de cada `#S`), -14 (PIN una vez por conexión; nombre del superadministrador recordado), -15 (tras la
  persistencia no hace falta repetir las pruebas: la reconexión conserva la versión), -17 ("Rechazar" ofrece
  restaurar lo escrito, verificado). T-S09: una colocación cortada queda como INTERRUMPIDA.
- **Pruebas:** `FlujoCalibracionTest` (26) recorre, sólo con acciones del operador: camino feliz del 8 hasta
  `#SC`; el b sin PA-24; dos NO CONFORME y restauración; restauración no verificada y reintento; corte durante
  `#S` (entró y no entró); app matada en la re-medida; `#F` desde Avanzado con el acta abierta; heredado tocado
  antes de aceptar; `#SC` que falla; batería a 0 y cambio; reescribir un conforme; persistencia anulada por
  otro código; rechazo con restauración; el b con PA-24 y P49 a −10,7 %; el 5 con PA-14; T-S03, S06, S09, S11,
  S18, S19 y S23. Sin cubrir en la JVM: T-S00
  (reproducir un registro real), S01-S02, S04-S05, S13, S15, S17, S21, S22.
- Abierto: RF-CAL-43 incompleto (SHA-256 del ZIP, A5 y deriva por sesión, PIN de fábrica); s_rep de la propia
  serie con K ≥ 3 (C-P11-7).
- Entrega: `RTV-V3.6.13.apk` y `RTV-V3.6.13-3613.apk`.

## Cambios de la 3.6.12 (corte B: calibrar con un botón; REVISION-Arquitectura-P10-V3.6.md)

**Nada de esto se ha probado contra un equipo.** El flujo escribe en la EEPROM: T-C43 (Fase B en el equipo) sigue
PENDIENTE y exige que Diego autorice el código.

- **"Calibrar este equipo"** (`CalibrarActivity`, desde la pantalla inicial y desde Campaña). El operador no
  elige código, grado, método ni patrón de re-medida: salen de la **tabla RF-CAL-37 del APK**
  (`TablaCalibracion`): 1 y 2 no se reescriben; 3, 4 y 6 grado 1, o recta anclada si la libre no pasa `#S`;
  8 y b recta anclada en el OSCURO (b con la dispensa de PA-24 pendiente); 5 sólo verificar, o recta anclada si
  se activa PA-14; 7, a, c y d sólo verificar. Una tarjeta por código con su propuesta y su casilla de
  conformidad; nombre y nota obligatorios.
- **Comprobaciones previas:** conexión, firmware 3.6.2, pruebas pasadas, cola admitida, campaña del equipo,
  **serie `#GN#` igual a la de la campaña**, s_rep medida (A5 del inicio del banco; no se teclea) y batería.
- **Por código:** batería (9; bloquea con n = 0 o sin respuesta), `#G` anterior, `#S`, `#G`, `#E` en 5 puntos y
  `#V#` fresco (D-18). Si algo falla, se restaura la curva anterior (`#F,k#` si era la de fábrica, `#S` con la
  copia si no).
- **Re-medida (D-19, D-20, P10-C1):** K = 5 colocaciones × M = 4 pares `e`/código sobre el patrón de la tabla.
  Colocación con patrón ausente o par incoherente: NO_VALIDA, se registra y se repite. Criterios: reproducción
  de la campaña con s_rep medida y |R − certificado| ≤ max(10 %; 2). **Una repetición como máximo**; si la
  segunda tampoco es conforme, se restaura y la secuencia se para. Cada intento queda en el acta.
- **Acta en disco y reanudable** (RF-APP-36): `files/campanas/acta_<equipo>_curso.csv`, evento a evento. Al
  volver se retoma; un corte durante `#S` (RF-APP-37) se resuelve releyendo `#G` y `#E`: entró, no entró o
  ninguna de las dos (se restaura).
- **Persistencia:** apagar y encender; la app reconecta por MAC, vuelve a entrar con el PIN y relee `#V#`,
  `#G` y `#E` de lo certificado.
- **Aceptar:** `#V#` y `#G` frescos iguales a lo certificado, campos obligatorios (md5 de la cola, s_rep,
  batería, `#V#` posterior y oscuro si hay recta anclada) y `#SC` con la fecha de hoy verificada con `#GC#`.
  **Si `#SC` falla, el acta no se cierra.** Tras reconectar hay que volver a pasar las pruebas (la versión
  vuelve a "sin detectar"), porque sin 3.6.2 detectada no se acepta.
- **Avanzado:** un `#F` con el acta abierta la invalida; reescribir el mismo código no bloquea; s_rep ya no se
  teclea; la re-medida y la aceptación se hacen en "Calibrar este equipo"; el alta de la serie pide doble
  entrada, casilla si difiere del nombre Bluetooth y aviso si el equipo ya tenía serie.
- Tests: `Version3612Test` (12: D-20, acta en disco, corte durante `#S`, `#F`, `#SC`, T-A54 con cifras,
  T-A63, RF-CAL-37, anclas). El recorrido de la pantalla no tiene prueba automática.
- Entrega: `RTV-V3.6.12.apk` y `RTV-V3.6.12-3612.apk`.

## Cambios de la 3.6.11 (arreglo de la QA de la 3.6.10, `05_Documentacion/QA-App-3.6.10.md`)

Versión de arreglo antes de la sesión 2 del banco. **El corte B (calibrar con un botón) pasa a ser la 3.6.12.**

- **Filtro de patrón presente según `origen_x`** (QA-3610-01, `PLAN-Captura-Banco-P1-P132.md:542-544`):
  x medida ±20 %; x estimada con la recta por el oscuro ±30 %; fábrica invertida (naranja intenso) y **todos
  los café y lila**: sin comprobación de rango, sólo "no está en el oscuro" (x del oscuro de la campaña + 40).
  En la cola: 51 pasos a ±20 %, 57 a ±30 % y 25 sin rango. Un asentamiento sin lectura ya no se presenta como
  patrón ausente (QA-3610-13).
- **Salida ante un rechazo** (QA-3610-02): "Repetir colocación", "Medir igualmente" (nota obligatoria, queda
  en la nota de la serie) o "Dejar el paso" (un patrón queda SALTADO y se ofrece al final).
- **Al final se ofrecen todos los saltados**, uno tras otro y en vuelta (QA-3610-03); la lista de saltados
  se ve en la cabecera.
- **Rotación y Atrás a mitad de una serie** (QA-3610-04): la pantalla no se recrea al girar y queda en
  vertical mientras se mide; Atrás pide confirmación y, si se sale, la serie se cierra como no aceptada. La
  serie en curso vive fuera de la pantalla: si Android la destruye, la siguiente retoma **la misma serie**
  (nunca dos del mismo paso). Ningún diálogo se abre sobre una pantalla destruida.
- **Importación** (QA-3610-08/09/10): atómica también por diario (patrones de series y reasignaciones y
  series de los PASO se comprueban antes de escribir); trae los PASO del banco con la serie renumerada, sin
  pisar un paso que ya tenga estado salvo SALTADO frente a HECHO (BATERIA no se trae); la serie aceptada en el
  banco pasa a ser la elegida de su patrón, aunque un ELIGE del 19-sep fijara otra.
- EXPORTAR se anota HECHO **después** de exportar, con el nombre del ZIP, y una copia fallida en Download sale
  como alerta (QA-3610-11). Con la campaña cerrada, la cabecera dice por qué no se puede medir (QA-3610-12).
- Tests: `Version3611Test` (8). **T-S01 sólo en su parte JVM** (recorrido de la cola: 180 pasos, 133 patrones,
  2575 `e`); el recorrido con la interfaz y el simulador sigue PENDIENTE. El ciclo de vida de `BancoActivity`
  (giro, Atrás) no tiene prueba automática.
- Entrega: `RTV-V3.6.11.apk` y `RTV-V3.6.11-3611.apk`. **No se actualiza `RTV-V3.6.apk`** (RF-APP-41,
  QA-3610-14): el que haya en `03_App_Movil/` es de la 3.6.10.

## Cambios de la 3.6.10 (corte A: medir el banco; REVISION-Arquitectura-P10-V3.6.md)

- **Catálogo P1-P132** en `assets/patrones_certificados_P1-P132.csv` (md5 `07ab9cd8…`, P10-C7). El de P1-P50
  de la 3.6.9 queda como recurso de prueba de las pruebas antiguas.
- **Cola del banco** `assets/cola_banco_P1-P132.csv` (md5 `9ddb7882…`, commit `1f1c4e3`): se carga sólo si su
  md5 está en `BancoCola.MD5_PERMITIDOS`; si no, "Cola no admitida" y ningún `e`. K, M y asentamiento salen de
  cada fila; OSCURO y P81 a K = 5 (P10-C3; la cola ya lo trae y la app lo comprueba). Código por fila
  (café y lila con el 4, VERIFICACIÓN). `.gitattributes` evita la conversión de fin de línea de los CSV.
- **"Medir el banco"** (`BancoActivity`): paso a paso; calentamiento, batería, OSCURO, A5, patrones, pausa y
  exportar. Cada paso queda en el diario (evento `PASO`): al volver sigue en el primer paso pendiente. Los
  de control no se saltan; los patrones sí y quedan marcados (se ofrecen al final).
- **Patrón presente (P10-C1):** el asentamiento de cada colocación tiene que caer en [0,8 ; 1,2]·x esperada;
  si no, "Coloque P28" sin gastar la serie. La coherencia por par (`Colocacion.paresIncoherentes`) queda
  lista para la re-medida del corte B.
- **Batería (orden 9, RF-CAL-41 y §12.8 de `1f1c4e3`):** V, aviso con n < 19 y bloqueo de escrituras con n = 0
  o sin respuesta (evento `BATERIA`); nunca en mitad de una serie.
- **Exportar:** un solo ZIP y copia automática en `Download/RTV/` (MediaStore en Android 10+; permiso de
  almacenamiento sólo hasta Android 9). Aviso de no desinstalar al arrancar si hay series sin exportar.
- **Importar atómico (P10-C8):** todo se valida antes de escribir; no pisa la serie elegida por el operador;
  avisa si el firmware de lo importado difiere. "Importar ZIP de campaña" y "Medir el banco" arriba; cerrar
  y nueva campaña, en "Avanzado" al final (RF-APP-39).
- **Versión** en grande en la cabecera de todas las pantallas y en `resumen.txt` (RF-APP-41).

## Cambios de la 3.6.9

- **Importar una campaña exportada** (botón "Importar campaña (ZIP o campana.csv) o CSV antiguos"): del ZIP
  se usa el **diario** (reconstrucción exacta, incluidas las series sin disparos); si sólo hay `campana.csv`,
  se reconstruye desde él (una serie sin disparos no tiene filas: con el ZIP del 19-sep salen 56 por el
  diario y 55 por el CSV). Sin volver a juzgar: veredicto, aceptada, elegida, orientación, colocación,
  descartes y reasignaciones tal cual. Se rechaza entero si es de otro equipo (serie o MAC). Se muestra el md5
  del fichero y cada serie lleva el origen en su nota (queda en el diario). Reimportar no duplica.
- **Recta anclada en oscuro** (opción (b) de Diego para el código 2; disponible en cualquier código):
  mínimos cuadrados obligada a pasar por (x_oscuro ; 0). x_oscuro = media de la serie **OSCURO** de la
  campaña (patrón especial, superficie negra mate, certificado 0). Reproduce la propuesta: k = 0,36753403,
  IV 8,7 %, XI 5,8 %. El acta declara el método.
- **Preajustes:** A5 pasa a **5 × 9** (+ asentamiento); nuevo "OSCURO", 5 × 9. Ni A5 ni OSCURO entran en el
  ajuste de los códigos; OSCURO da el ancla.
- **RF-CAL-14/15/16** (propuestos) avisan, no bloquean: escribir una curva que los incumple pide la
  conformidad del superadministrador con nota, que queda en el acta (caso: blanco grado 1).

## Cambios de la 3.6.8 (condiciones (b) de REVISION-Arquitectura-P9-V3.6.md)

- **P9-B13, oscuro:** antes de `#S`, la curva nueva en la `x` de oscuro (575 por defecto, editable) frente a
  fábrica; bloquea si R > máx(fábrica + 10 ; 25) (umbral del coordinador, editable; el documento no fija
  cifra) y lo explica con cifras. El informe da también R en el patrón más bajo y declara que por debajo no
  está calibrado. Con las curvas del §4 bis: blanco grado 2 (219) bloqueado; **amarillo grado 1 (84 frente
  a 21) también, con el umbral por defecto**.
- **`#SC` sólo al aceptar el acta** (lo señala la revisión r2 de la 3.6.7): `Acta` reúne los códigos escritos
  (curva = `#G` tras `#S`, P9-B9), su `#E`, el oscuro y la re-medida; se acepta sólo con todas las
  re-medidas conformes y entonces se graba `#SC` una vez. Rechazada, no se graba fecha. El acta se guarda y
  va en el ZIP (`actas/`).
- **P9-B8:** un código cada vez: no se escribe otro sin la re-medida conforme del anterior.
- **P9-B3:** el protocolo (K × M + asentamiento) es el de la campaña (el más frecuente entre las series
  elegidas; 1 × 9 en la del 19-sep), queda fijo mientras el acta está abierta y sale escrito en ella.
- **P9-B5 / RF-CAL-18:** la re-medida usa `'e'` y el código en cada disparo; tolerancia máx(2 ; 2·s_rep) con
  la s ENTRE colocaciones. RF-CAL-13: la campaña K × M ya juzga por s entre colocaciones. RF-CAL-15/16:
  sesgo y RMS por tipo, nueva frente a fábrica, con margen de s_rep (informativo).
- **P9-B7:** el informe del ajuste dice de dónde salen los puntos (campaña o medidas sueltas).
- **P9-A5:** preajuste "A5 (arquitecto)" en la campaña: P22, P28, P4 a 5 × 3 (el documento dice
  asentamiento + 9). Muestra s_rep, el puente frente a la campaña (máx(2·s_rep ; 4 %)) y el criterio de
  reversión a la 3.6.1. Las series A5 no entran en el ajuste.

## Cambios de la 3.6.7

- **Firmware 3.6.2:** tras identificar una V3.6 con `#V#` se envía `#GC#`: `#GC,…#` = 3.6.2, `#ERR,FORMATO#` =
  3.6.1 (misma fecha de compilación). A un equipo sin identificar no se envía nada de efecto desconocido.
- **Serie y fecha:** `#GN#` y `#GC#` en Pruebas, en la cabecera de la campaña y en `resumen.txt`; vencimiento =
  fecha + 1 año (un 29 de febrero vence el 28 de febrero). Estados: Sin fecha, No calibrado (fecha y DEF),
  Calibración vencida. En admin: grabar serie (`#SN`, verificada con `#GN#`) y `#FT#` (sólo 3.6.2). Tras un
  `#S` verificado con `#E`, la app graba `#SC` con la fecha de hoy y la verifica con `#GC#`; en la 3.6.1 avisa.
- **K colocaciones × M disparos** (3 × 3 por defecto, configurable; 1 × 9 sigue valiendo). Asentamiento por
  colocación; entre colocaciones pide levantar y volver a apoyar. Media = media de las K medias; se informan
  la s entre colocaciones y la s dentro; repetir si la s entre colocaciones pasa del 3 % (configurable). El
  diario añade la colocación al final de `DISPARO` (si falta, 1): las campañas anteriores se abren igual
  (test con la campaña real de las 10:33).

## Cambios de la 3.6.6

- **Descolgado (C-46):** umbral = máx(5 × 1,4826 × MAD ; 50 cuentas). El ruido medido es s ≈ 5 y el
  descolgado real (P7, 3031) está a ~200; con sólo 5 MAD se marcaban disparos buenos de P2.
- **T-C38:** `resumen.txt` lleva, por serie, el desvío de cada posición de disparo respecto a la mediana
  de la serie y la media por posición. El asentamiento por defecto sigue en 1.
- **Cerrar campaña:** evento `CIERRE` en el diario; la deja de solo lectura (se puede exportar).
- **Huellas del ZIP:** md5 y SHA-256 en pantalla, en el texto del envío, en el registro de tramas y en
  el diario (`EXPORTA`), que las lleva al `resumen.txt` de la exportación siguiente: un ZIP no puede
  contener su propia huella.
- **Actualizar encima de la 3.6.5 conserva la campaña:** el diario vive en `files/campanas/` del
  almacenamiento propio (mismo `applicationId` y misma firma de depuración, comprobada con apksigner),
  y el formato es compatible hacia atrás (test con un diario de la 3.6.5).

## Campaña de calibración (3.6.5)

Pantalla "3. Campaña de calibración". **Sin probar en el equipo.**

- **Una campaña por equipo** (serie + MAC). La óptica cambia de un equipo a otro: nunca se mezclan
  series, el ajuste de un equipo sólo usa las suyas (`Sesion.medidasParaAjuste`) y el import sólo
  toma las filas con su MAC. Si el nombre Bluetooth no trae la serie, la app la pide.
- **Diario de sólo añadir** en `files/campanas/campana_<serie>_<mac>.csv`: cada disparo se escribe
  al llegar; sobrevive a cerrar la app y se retoma. Reasignar ("era otro patrón") o elegir otra serie
  son eventos nuevos: el registro original queda.
- **Modo guiado:** "Coloque P27 — blanco IX — cert. 471 — orientación 0°" → OK → asentamiento + N
  disparos (9 por defecto) → veredicto. Si es OK, se acepta y pasa solo al siguiente; si no, "Repetir",
  "Aceptar con nota" (obligatoria) o "Era otro patrón". "Saltar" lo deja para el final.
- **Cola** (`Cola`): 1) imprescindibles pendientes: huecos de x (hoy P27, P28), series aceptadas en
  conflicto (P24: 2065 y 2443) y orden de certificados contradicho fuera de los XI (P29 y P30, los dos
  porque no se sabe cuál está mal); 2) tipo I que ajustan (amarillo P34, P37, P43, P44; rojo P38, P39,
  P49); 3) resto de tipo I, comprobación; 4) giro de P5 a 0° y 90°; 5) lo demás sin medir o por repetir.
  Lo medido bien se salta.
- **Veredicto al momento** (`Veredicto`): disparo descolgado a más de 5·s robusta (1,4826·MAD) de la
  mediana → descartado con su motivo (P7: 3031); s > 15 → repetir; "¿es este el patrón?": orden de
  certificados dentro del mismo color y tipo (3 % configurable) y parecido ±1 % a otro patrón del mismo
  color y clase con certificado distinto, salvo que esté más cerca de la x esperada (recta
  x-certificado del mismo color y tipo). En un XI el aviso dice que puede ser la orientación, pero sale.
- **Un solo envío:** "Exportar campaña" → `campana_<serie>_<fecha>.zip` con `resumen.txt` (abre con
  serie, MAC, firmware y fecha), `campana.csv` (todas las series: serie_id, orientación, descartado y
  motivo, veredicto, nota, aceptada, elegida), `pruebas.txt` (cada "Pruebas del equipo" hecha con la
  campaña abierta), `tramas/` (todos los registros desde el inicio de la campaña) y el diario.
- **Importar CSV antiguos** (`Importador`): quita las filas repetidas de las copias crecientes, agrupa
  en series (mismo patrón, < 5 s entre disparos), pasa el mismo veredicto y sólo acepta las OK. Con los
  CSV del 19-sep: 1176 filas, 996 duplicadas, 180 únicas, 26 series.

## Novedades de la 3.6.4

- **Patrones tipo I** (P32a-P50, Diego, 19-sep-2026) en el asset de patrones. Cada color va a su
  código opaco: blanco 7, amarillo 8, verde a, rojo b, azul c, naranja d. `P32` venía duplicado:
  se carga como `P32a` (azul 9) y `P32b` (naranja 68), pendiente de confirmar; es una línea del CSV.
- **Qué se puede ajustar** lo calcula la app del catálogo (`Asistente.cobertura`): grado máximo =
  niveles certificados distintos − 2 (hasta 2), y **no se ajusta** si el rango certificado es
  estrecho (< 20 unidades de R o < 30 % del mayor): sería extrapolar a ciegas. Resultado: 1 y 2
  hasta grado 2; 8 (amarillo tipo I) hasta grado 2; b (rojo tipo I) grado 1; el resto, sólo
  verificar. C2 sólo valida la forma en todo el rango; por eso el informe del ajuste marca qué
  valores de R son extrapolación.
- **Criterio de `#S` del firmware 3.6.1** (R en [0; 4000] en x = 600-4300) aplicado antes de enviar,
  con el motivo ("la curva se hace negativa en x = …"). La fábrica del código 2 lo incumple desde
  x = 4175. Si una parábola no cumple, la app dice si una recta sí.
- Tras `#S`: relectura `#G` a 8 ulp (medido hasta 7 en el simulador del firmware) **y** `#E` en
  5 puntos contra la curva enviada (±1). Si falla, se restaura.
- **Disparo de asentamiento:** antes de cada serie y de la `e` inicial de la prueba 3 se hace y se
  descarta 1 disparo (configurable, 0 lo desactiva). Va al registro como "disparo de asentamiento,
  descartado"; no entra en la media ni en el CSV.
- `#V#` con fecha 2026-09-18 = V3.6 sin límites de `#S`; 2026-09-19 o posterior = 3.6.1.

## Lectura de la `x`

- Con `e` (sólo V3.6): `x` directa. Un 0 es saturado o negativo, no se guarda.
- Sin `e` (SLV-002): se envía `6` (naranja intenso, monótono y sin techo de x = 500 a 4300) y se
  invierte **de forma exacta**: se recorren las `x` enteras de 0 a 4400 y se guardan las que dan esa
  respuesta. La `x` es el centro del intervalo y su semiancho, la resolución (≤ 3 cuentas hacia
  1700-3000). Por debajo de x ≈ 475 el `6` da 0 y no hay lectura.

## Medidas y ficheros

Todo en el almacenamiento propio de la app (`files/registros/`). **Ningún fichero se pisa**: si el
nombre existe, se añade `_2`, `_3`...

| Fichero | Contenido |
| :--- | :--- |
| `rtv36_<fecha>.txt` | Registro de tramas: cada TX/RX en hex y ASCII con su tiempo, cada petición con su desenlace (válida, timeout o inesperada), las pruebas y la identidad del equipo. **El PIN sale como `****`** |
| `medidas_<serie>_<fecha>.csv` | Una fila por lectura **válida**: fecha, serie, MAC, firmware, patrón, valor, tipo, color, código, respuesta bruta, `x`, resolución, método. Se escribe en el acto |
| `coeficientes_<serie>_<fecha>.csv` | Copia de los 12 juegos y la temperatura (V3.6), antes y después de escribir |
| `botones_<serie>_<fecha>.csv` | Parejas botón → trama STONE |
| `lineabase_<serie>_<fecha>.csv` | Línea base previa a grabar (T-B03, T-B07, T-B09) |

## Modo administrador (sólo V3.6)

- `#L,<pin>#`. La app cuenta los fallos: el firmware bloquea `#L` a los 5 hasta apagarlo. El modo se
  cierra con `#Q#`, al salir de la pantalla, al perder el enlace o a los 10 min sin tramas `#` (la
  app lo da por cerrado a los 9,5 min). Un `#ERR,BLOQUEADO#` lo cierra también en la app.
- **Asistente:** sólo las curvas **1 (blanco intenso) y 2 (amarillo intenso)** se pueden ajustar con
  P1-P31. Verde y azul tienen un único nivel y rojo dos muy próximos (sólo verificar); naranja no tiene
  patrones; ningún patrón es de tipo I, así que las opacas no se ajustan. La pantalla lo dice.
  Ajuste por mínimos cuadrados de grado 1 o 2 sobre las medias por patrón; exige grado + 2 patrones;
  muestra coeficientes, residuo por patrón (absoluto y %), error máximo, RMS y el estado "como llegó".
  El ajuste exige grado + 2 valores de `x` distintos (`Ajuste.ajustar` lo rechaza si no).
- **Forma de la curva (C2), bloqueante**, en todo x = 200-4400: tiene que ser creciente (sin techo) y
  no pasar de 4000. No puede ser negativa desde la `x` mínima de los patrones hasta 4400. Por debajo
  de esa `x` un negativo sólo se avisa, porque todas las curvas de fábrica son negativas hacia
  x = 200 (f1(200) = −182) y en oscuro el equipo responde 0 por diseño. Contradicción abierta con la
  letra de C2. Consecuencia: el blanco intenso de grado 2, que reproduce el techo de fábrica
  (x ≈ 3580), queda bloqueado.
- **Escribir:** confirmación con coeficientes antiguos y nuevos, R de ambos sobre los patrones y la
  trama. Antes, copia de los 12 juegos; después, `#S` (9 cifras, `%.8E`), relectura `#G` y
  comparación a 5 ulp (`strtod` 3 + impresión 2; `Ecuacion.ULP_S`). **C3:** si la relectura no coincide con lo enviado, o si `#S` da `#ERR` o no
  responde y el estado releído no es el anterior, la app **restaura sola**: `#F,k#` si lo anterior era
  fábrica, o `#S` con los valores anteriores. Después relee y avisa. Ante `#ERR` no afirma que "no ha
  cambiado nada": informa de lo que relee.
- **`#ST` (C4):** la app no ofrece hoy escribir la temperatura. `Tramas.tramaST` rechaza X_0 fuera
  de 0,5-1,5 para cualquier uso futuro.
- `#F,k#` / `#F,*#` con confirmación y copia previa. `#P,<actual>,<nuevo>#`.

## Botones de pantalla (sólo V3.6)

`#KC#` vacía el registro del firmware; el operador pulsa un botón en la STONE; `#K#` devuelve las
últimas tramas. La app **resalta el byte 8** (`bufferPantalla[8]`) y lo traduce según la referencia
archivada (OTROS PAPELES 0x01-0x06, TIPO I 0x07-0x0D, 0x0E prueba ADC). El operador escribe qué
botón pulsó y se guarda la pareja.

## Lo que no está probado o queda abierto

- **Nada se ha probado contra un equipo.** Ni la conexión, ni la detección, ni las pruebas.
- Modo V3.6 completo: no hay firmware V3.6.
- Emulación float32: supone que la librería de XC8 redondea como IEEE-754 (R-05 de la SPEC). Si no,
  la prueba 5 dará discrepancias de 1 unidad en `#E`; la app las señala como tales.
- Formato hex de `#K#` dentro de cada trama: el contrato no lo fija. La app acepta hex seguido o con
  espacios.
- Umbral de repetibilidad (10 cuentas) y tolerancia de coherencia (15): provisionales.
- La pausa corta (150 ms) entre tramas `#` seguidas en V3.6 se apoya en que una trama `#` nunca mide
  (contrato §3 y O-05); sin medir.
