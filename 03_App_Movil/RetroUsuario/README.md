# RTV Usuario — app de USUARIO del retrorreflectómetro V3.6

**Estado, 22-sep-2026: compila y pasa sus 152 tests JVM (checkout limpio). Nada probado contra un
equipo ni un teléfono** (CLAUDE.md, cabecera). Cierra **arq C1** (REVISIONES-Apps-V3.6.md, entrada
0.3.6): salir o cambiar de equipo con la pregunta CERO abierta guardaba una fila con `media = 0`
(`SerieDisparos.java:98-99` de la 0.3.6) — la SPEC §4 bis pide "sin fila". `PreguntaOperador.Decision`
gana un tercer valor, `ABANDONAR` (sólo lo emite `EstadoMedida.abandonar()`; ningún diálogo de operador
lo ofrece): `SerieDisparos` ya no lo trata como un "Saltar" real. Cierra **arq Medio**: el `Runnable` de
`handlerPrincipal.post()` (`MedirActivity.java:70`, mismo patrón en `MainActivity.java:79`) no pinta si
la Activity ya está `isFinishing()`/`isDestroyed()` cuando el `Handler` lo entrega (evita
`AlertDialog.show()` → `BadTokenException` sobre una Activity muerta). Cierra **arq Bajo**:
`EstadoMedida.marcarMidiendo()` (nuevo, mismo patrón que `EstadoDeteccion.iniciar()`) pone la fase en
MIDIENDO desde el hilo de la pantalla, ANTES de lanzar el hilo de fondo — cerraba la ventana en LIBRE
entre la pulsación y `EstadoMedida.ejecutar()` (`MedirActivity.java:174-183` frente a
`EstadoMedida.java:146-148` de la 0.3.6). **QA:** recuento corregido — la base heredada de la 0.3.5 es
**98 requisito / 43 comportamiento** (141), no 100/41 (`REVISIONES-Apps-V3.6.md:58-59`, ya lo decía);
el "107/41 (148)" de esta misma cabecera en la 0.3.6 arrastraba el número viejo. 4 pruebas nuevas de
esta vuelta (abajo): **107 requisito / 45 comportamiento** (152). `versionCode 10`, `versionName
"0.3.7"`.

Vuelta anterior (0.3.6, ROADMAP del principal): cierra **FABLE-USR** (revisor Fable, opción A,
`SPEC-App-Usuario-V3.6.md:617-625`, "Operaciones que sobreviven a la pantalla"): hasta la 0.3.5
`MedirActivity` era la dueña de la operación "medir" (hilo con una cola local alimentada por un
diálogo sobre `this`, `runOnUiThread` sobre `this` para pintar) — un giro de pantalla a mitad de la
pregunta "Repetir o Saltar" perdía el diálogo del sistema y dejaba el hilo de fondo bloqueado para
siempre, sin arnés, y la Activity nueva nacía con los botones habilitados aunque hubiera una medida en
curso. Ahora la dueña es `dominio.EstadoMedida` (fase LIBRE/MIDIENDO/PREGUNTANDO, implementa
`PreguntaOperador`, oyente registrado en `onResume`/retirado en `onPause`), mismo patrón que
`dominio.EstadoDeteccion` — cuyo `Oyente` se extrae a `dominio.Oyente`, compartido por las dos.
`SesionHolder.limpiar()` llama a `medida().abandonar()`: cambiar de equipo o salir con una medida en
curso ya no deja el hilo de medir colgado sobre un enlace cerrado (antes, `MainActivity.java:219-221`
de la 0.3.5 cerraba el socket sin avisar a nadie). `dominio.EstadoMedidaTest` (7 nuevas, todas
requisito, T1-T7 de la SPEC citada) recuento de esa vuelta: 107 requisito / 41 comportamiento (148) —
**corregido en la 0.3.7 (arriba): 105 requisito / 43 comportamiento (148)**, la base heredada de la
0.3.5 ya era 98/43, no 100/41. `versionCode 9`, `versionName "0.3.6"`.

Vuelta anterior (0.3.5, presupuesto del incremento 1, ROADMAP del principal): cierra **C2** de
`REVISIONES-Apps-V3.6.md` (entrada 0.3.4, Alto) —
`runOnUiThread(this::restaurarInterfaz)` (`MainActivity.java:259` de la 0.3.4) corría sobre la
Activity destruida tras un giro, consumía el resultado de un solo uso sin que la nueva se enterara, y
se encolaba antes del `finally` que bajaba `detectando`; el camino de error tampoco publicaba. Ahora
`dominio.EstadoDeteccion` baja `detectando` y publica (resultado, error o sonda de
`reintentarSonda`, anotado por el arquitecto en la misma entrada) en el mismo bloque `synchronized`,
ANTES de avisar a un `EstadoDeteccion.Oyente` registrado en `onResume()`/retirado en `onPause()`
(nunca `runOnUiThread` sobre `this`) — con pruebas JVM que reproducen la carrera (`EstadoDeteccionTest`,
5 nuevas). Cierra también **RF-USR-04 r7** (REPETIR-PREGUNTA, DECISIONES nota 18,
`SPEC-App-Usuario-V3.6.md:167-197`): la app **ya no repite sola** — ni la serie con un cero (antes, un
auto-repetir de una vez) ni un disparo anulado por plazo/doble `::` (antes, tope fijo de 2 reintentos)
— `dominio.PreguntaOperador` (nueva interfaz) bloquea `SerieDisparos` hasta que el operador conteste
"Repetir" o "Saltar", sin límite; `MedirActivity` la implementa con un diálogo modal que bloquea el
HILO DE FONDO de medir (nunca el principal). `SerieDisparosTest` reescrita contra r7 (12 tests, 2
nuevos) y `SesionMedicion.medir`/`MedirActivity.medir` reciben la pregunta como parámetro. `versionCode
8`, `versionName "0.3.5"`.
Cierra las condiciones de arquitecto-iot sobre la
0.3.3, registradas en `05_Documentacion/REVISIONES-Apps-V3.6.md` **C1** (`MainActivity.java:192-194`
de la 0.3.3: la salida anticipada de `hiloConectarYDetectar` durante "Conectando" — Atrás a mitad de
la conexión — no ponía `detectando = false`, ni tampoco `SesionHolder.limpiar()`: la app quedaba en
"Conectando" para siempre. Se saca la máquina de estados a `dominio.EstadoDeteccion`, dominio puro
con pruebas JVM propias, y `hiloConectarYDetectar` la termina con un `try/finally` — TODO camino de
salida del hilo pasa por ahí, sin repetir la llamada en cada rama) y **C2** (`MainActivity.java:94-97`
y `:211` de la 0.3.3: el resultado de una detección que termina con la Activity ya destruida — giro
de pantalla — se pintaba con `runOnUiThread` sobre esa instancia vieja, capturando `canalRegistrado`/
`resultado` en el cierre; la Activity nueva nunca se enteraba. Ahora el resultado se publica en
`SesionHolder` (`EstadoDeteccion.publicar`) y CUALQUIER Activity viva lo recoge, una sola vez, en su
propio `onResume()` → `restaurarInterfaz()`; corregido el comentario que describía mal ese mecanismo).
Bajos de la misma revisión: el camino de reintento con conexión nueva (arq ALTO de la 0.3.2) llama a
`SesionHolder.limpiar()` antes de reconectar, para no dejar la sesión/enlace viejos sobre un socket ya
cerrado; la receta del hash por CONTENIDO del `.apk` (más abajo) fija el orden de `sort` con
`LC_ALL=C`, y `fuente.md5` ahora también cubre `app/proguard-rules.pro`. Añadido de QA sobre la 0.3.3
(obligatorio en esta entrega, no citado en ninguna revisión archivada todavía): el bucle de lectura de
`EnlaceBluetooth.leerSinParar` no tenía ningún arnés de pruebas — quitar la llamada a
`EstadoEnlace.marcarCaido()` del `catch` dejaba las 126 pruebas de la 0.3.3 en verde igual. Se saca a
`dominio.LectorDeFlujo` (java.io.InputStream puro), con pruebas JVM de los tres casos (excepción, EOF,
bytes en camino) vistas en rojo quitando esas llamadas del código de producción (no invirtiendo
aserciones) — la salida real de las dos demostraciones de rojo está pegada en el Javadoc de
`LectorDeFlujoTest` y `EstadoDeteccionTest`. Las condiciones de arquitecto-iot y qa-istqb sobre la
0.3.2 (`REVISIONES-Apps-V3.6.md:33-37`, ambas APTO CON CONDICIONES) siguen cerradas, sin repetirse
aquí: **arq ALTO** (`EnlaceBluetooth.vivo()` ya no es sólo `socket.isConnected()` — un equipo caído se
reutilizaba como si siguiera vivo, "no compatible" en bucle; ahora `dominio.EstadoEnlace` lleva una
bandera "caído" aparte, y una detección sobre un enlace REUTILIZADO que no recibe nada a `#V#` se
reintenta UNA vez con conexión nueva antes de concluir "no compatible"), **arq M-1** (`detectando` en
`SesionHolder`, no en un campo de `MainActivity`), **arq M-2** (salir con Atrás desde la pantalla
principal libera el equipo), **arq B-3** (si la Activity terminó de verdad mientras `connect()` estaba
en curso, el hilo cierra ese socket en vez de dejarlo huérfano), **arq B-1** (`fuente.md5` cubre
también `build.gradle`/`settings.gradle`/`gradle.properties` de la raíz de este proyecto), **QA D-1**
y **QA D-2** (recuento requisito/comportamiento honesto, abajo). Las condiciones de la 0.3.0/0.3.1
(QA-1 a QA-10, arq C1, C2, B-1 a B-5 de esas vueltas — **distintas** de las C1/C2 de esta entrega,
mismo nombre corto, revisión distinta) siguen cerradas, sin repetirse aquí. Incremento 1 "Medir y
exportar" (`05_Documentacion/SPEC-App-Usuario-V3.6.md` r6, §0) completo; el incremento 2 "Señal a
señal" no está aquí.

No confundir con `03_App_Movil/RetroV36` (app de EMPRESA: DPI, por USB/Bluetooth, PIN, banco y
calibración). Esta app va **con el equipo** y la usa el operador de campo, sin modo administrador.

- `applicationId com.dpi.retrousuario.coviandina`, `versionCode 10`, `versionName "0.3.7"`.
- Etiqueta en teléfono: `"Retro Coviandina"` (`manifestPlaceholders = [etiqueta: "Retro Coviandina"]`).
- Fichero entregable: `RETRO-COVIANDINA-usuario-0.3.7-10.apk`.
- `minSdk 24`, `targetSdk 30`, `compileSdk 30`. Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`,
  `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` (arq C2: pedido en tiempo de ejecución al entrar a
  medir, una vez por proceso — `MedirActivity`/`dominio.PermisoUbicacion`; se conceda o no, se mide
  igual, `gps_estado = sin_posicion` si no hay posición, RF-USR-15/T-USR-23),
  `WRITE_EXTERNAL_STORAGE` (≤ API 28, pedido en tiempo de ejecución, A4). API 29+ exporta por
  `MediaStore.Downloads` (A4), sin ese permiso.
- Contrato: `05_Documentacion/PROTOCOLO-V3.6.md`. Catálogo de colores: `08_Senales/senales.csv`.

## Reglas para no tener que desinstalar nunca (firma y datos)

1. **El `versionCode` sólo sube:** Android rechaza con `INSTALL_FAILED_VERSION_DOWNGRADE` cualquier
   intento de instalar un `versionCode` inferior o igual.
2. **El `applicationId` NO se cambia en una app ya instalada:** en este teléfono ya está instalada
   `com.dpi.retrousuario.coviandina`. Cambiar el paquete crearía una segunda aplicación (dos iconos) y
   los datos/campañas quedarían en la partición de la app anterior. La versión tampoco se incluye en el
   paquete.
3. **NUNCA se desinstala para instalar:** desinstalar borra los datos de la aplicación y destruye la
   campaña del cliente en campo. Si una instalación falla, parar y avisar (A-01, A-10).

## Nombrado por dueño

- El nombre del dueño va en la **ETIQUETA** que se ve en el teléfono (`manifestPlaceholders` en
  `app/build.gradle` → `android:label="${etiqueta}"` en `AndroidManifest.xml`) y en el **NOMBRE DEL
  FICHERO** que se envía por chat/correo.
  - App de usuario (cliente): etiqueta `"Retro Coviandina"`, fichero
    `RETRO-COVIANDINA-usuario-<versionName>-<versionCode>.apk`.
  - App de calibrar (DPI): etiqueta `"Retro Coviandina Calibrar"`, fichero
    `RETRO-COVIANDINA-calibrar-<versionName>-<versionCode>.apk`.
- Si en el futuro hay otro dueño (ej. Nordeste): lo que cambia es la etiqueta visible y el nombre del
  fichero. El `applicationId` SOLO se modifica si se trata de un teléfono que NO tenga instalada la app
  previamente.

## Clave de firma y certificados

- Diagnóstico: las APKs se firman con la clave de depuración (`~/.android/debug.keystore`). Si la clave
  cambia entre máquinas de desarrollo, Android rechaza la actualización (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)
  y sólo permite desinstalar.
- Huella del certificado de compilación en esta máquina:
  `Signer #1 certificate SHA-256 digest: c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`.
- Propuesta para producción: generar una keystore estable (`~/.android/dpi_release.jks`) fuera del
  repositorio, compartida entre máquinas autorizadas, sin versionar el archivo `.jks` ni sus credenciales.

## Anotación del Arquitecto (`EstadoMedida.java:172`)

- El arquitecto anotó (`REVISIONES-Apps-V3.6.md:58`): abandonar entre pulsar Medir y `ejecutar()`
  (lectura del GPS) se pierde porque `marcarMidiendo()` en línea 172 resetea `abandonado = false`.
- **Justificación técnica para no tocar código en la 0.3.7:** la versión 0.3.7 ya cuenta con veredicto
  **APTO del Arquitecto**. Alterar código de producción Java invalidaría el APTO y exigiría una nueva
  ronda de revisión de arquitectura. La ventana entre `marcarMidiendo()` en el hilo principal y
  `ejecutar()` en el hilo de fondo es de sólo ~1 a 5 ms (lectura de última posición GPS en caché del
  sistema). Además, pulsar Atrás en `MedirActivity` no llama a `abandonar()` (sólo lo hace
  `SesionHolder.limpiar()` al salir de la raíz o cambiar de equipo). Queda documentado y programado
  como mejora de robustez para la versión 0.3.8 / Incremento 2.

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
  0.3.0); `EstadoDeteccion` (arq C1/C2 sobre la 0.3.3: junta "hay una detección en curso" y "qué
  resultado hay que pintar" en dominio puro — `iniciar`/`salir`/`publicar`/`recogerResultadoPendiente`
  — para que `SesionHolder` no dependa de un `runOnUiThread` de la Activity que lanzó el hilo);
  `LectorDeFlujo` (QA sobre la 0.3.3: el bucle de lectura del socket, java.io.InputStream puro, para
  poder romperlo en la JVM con un flujo doble — antes vivía sólo en `EnlaceBluetooth`, sin pruebas);
  `EstadoMedida` (FABLE-USR sobre la 0.3.5, `SPEC-App-Usuario-V3.6.md:617-625`: dueña de "medir" —
  fase LIBRE/MIDIENDO/PREGUNTANDO, implementa `PreguntaOperador`, `ejecutar`/`responder`/`abandonar` —
  mismo patrón que `EstadoDeteccion`, para que `MedirActivity` deje de serlo); `Oyente` (extraída de
  `EstadoDeteccion.Oyente` sobre la 0.3.5, la comparten `EstadoDeteccion` y `EstadoMedida`).
- Capa Android: `MainActivity` (aviso previo una sola vez por proceso — M-1 —, `restaurarInterfaz()`
  en `onResume()` (arq C2 sobre la 0.3.3: antes en `onCreate`, que no cubre volver al primer plano
  sin recrear la Activity) pinta lo que `SesionHolder` ya sabe — lista deshabilitada si
  `detectando()`, `btnReintentar` visible si `canalSondaEnCurso()`, `btnContinuar` si `sesion()`, y un
  resultado de detección pendiente (arq C2) se recoge y se pinta ahí, una sola vez —, detección con
  `CanalRegistrado` sobre el mismo `tramas.log` que los disparos — M2 —, `hiloConectarYDetectar`
  termina la detección en un `try/finally` (arq C1 sobre la 0.3.3: antes el `return` de "Atrás a
  mitad de Conectando" no lo hacía, y la app se quedaba en "Conectando" para siempre), cambio de
  equipo decidido por `dominio.GestorEnlace` sobre `SesionHolder.enlace()` — arq C1, corrige la fuga
  de socket de la 0.3.1 —, un enlace REUTILIZADO sin respuesta a `#V#` se reintenta con conexión
  nueva antes de "no compatible" — arq ALTO —, un socket recién abierto se cierra en vez de
  registrarse si la Activity ya terminó de verdad mientras `connect()` estaba en curso — arq B-3 —,
  estado de "Reintente" en `SesionHolder`, no en un campo de la Activity — arq B-1 de la 0.3.1 —,
  "Exportar" desde la pantalla principal sin sesión viva — QA-6 —, `onDestroy` libera el equipo
  (cierra el enlace y limpia la sesión) al salir de verdad con Atrás desde la Activity raíz — arq M-2,
  corrige M7/arq B-1 de la 0.3.1, que no lo hacía si había sesión viva); `MedirActivity` (pantalla 4,
  FABLE-USR sobre la 0.3.5: ya no es la dueña de "medir" — el hilo de fondo sólo referencia
  `SesionHolder.medida()` y `getApplicationContext()`, `restaurarInterfaz()` en `onResume()` y en el
  aviso del oyente de `EstadoMedida` pinta desde `fase()`/`preguntaPendiente()`/`recoger*()`: botones
  deshabilitados mientras MIDIENDO/PREGUNTANDO — A1 —, el diálogo "Repetir o Saltar" se re-muestra si
  hay una pregunta pendiente (un solo diálogo vivo, cerrado en `onPause()`), exportar por API — A4 —,
  pide `ACCESS_FINE_LOCATION` al entrar, una vez por proceso — arq C2 —, `0` mostrado como "saturado o
  negativo" con las lecturas crudas — M3 —, cualquier excepción de `sesion.medir()` la captura y
  publica `EstadoMedida.ejecutar` — B-1 de la 0.3.0); `AjustesActivity` (`lecturasPorColor`,
  persistido en `SharedPreferences` — B3);
  `EnlaceBluetooth` (`Canal` y `FuenteBytes`, `ahoraMs()` relativo a la apertura del enlace — M2 —,
  expone la MAC realmente conectada — A2 —, `vivo()` delega en `dominio.EstadoEnlace` — arq ALTO,
  ya no es sólo `socket.isConnected()` — para que `GestorEnlace` decida si reutilizar —,
  `leerSinParar()` delega el bucle en `dominio.LectorDeFlujo` — QA sobre la 0.3.3); `UbicacionGps`
  (lee el `Location` del sistema, la decisión de si sirve vive en `dominio.EstadoGps` — QA-7 —, toma
  la posición MÁS RECIENTE de todos los proveedores antes de clasificar — arq B-3 de la 0.3.1 —, no
  pide el permiso, eso lo hace `MedirActivity` — arq C2); `ExportadorAndroid` (vía
  `MediaStore.Downloads`, con la colisión de nombre resuelta consultando el `ContentResolver` antes
  de insertar — QA-4 —, limpia `IS_PENDING` ante cualquier excepción, no sólo `IOException` — B-2);
  `SesionHolder` (única fuente de verdad del enlace desde que se abre el socket, no sólo si la sonda
  tiene éxito — arq C1 de la 0.3.1 —, `detectando()`/`marcarDetectando()`/`publicarResultadoDeteccion()`/
  `recogerResultadoDeteccionPendiente()` delegan en `dominio.EstadoDeteccion` en vez de un campo de
  `MainActivity` — arq M-1, y arq C1/C2 sobre la 0.3.3 —, `limpiar()` también termina una detección
  en curso, defensivo — arq C1 sobre la 0.3.3 —, estado de "Reintente" de la sonda en curso — arq
  B-1 de la 0.3.1 —, invalidación al cambiar de equipo — A2 —, ajustes persistidos — B3 —, aviso
  previo y permiso de ubicación aceptados una vez por proceso — M-1/arq C2 de la 0.3.1 —, `medida()`
  expone la única instancia de `dominio.EstadoMedida` del proceso, y `limpiar()` le llama a
  `abandonar()` — FABLE-USR sobre la 0.3.5, antes cerraba el socket con una medida en vuelo).
- **Reloj inyectable, sin `Thread.sleep` en pruebas del dominio:** toda espera pasa por
  `FuenteBytes.leer(limiteMs)`; en producción bloquea de verdad, en pruebas un simulador avanza un
  reloj propio. Excepción deliberada: `Sonda362`/`DeteccionYSonda` sí usan un `Thread.sleep` real de
  150 ms en producción (M5, QA-3, pausa entre `#V#`→`#GN#` y entre `#GN#`→`#GC#`); las pruebas pasan
  un consumidor de pausa que no duerme (overload de 3 argumentos, visible sólo para el paquete
  `dominio`).

## Tests JVM

152 tests, `dominio/*Test.java` (**107 requisito / 45 comportamiento**). Las 4 nuevas de esta entrega
(0.3.7, abajo) son 2 requisito / 2 comportamiento. Base heredada de la 0.3.6: 105 requisito / 43
comportamiento (148) — las 7 nuevas de esa entrega eran todas requisito, pero la base que arrastraban
de la 0.3.5 estaba mal (100/41): **QA sobre la 0.3.7:** `REVISIONES-Apps-V3.6.md:58-59` (entrada 0.3.5,
ya archivada) declara la cifra correcta desde entonces, **98 requisito / 43 comportamiento (141)** — el
"100/41" que este README repetía en la cabecera y aquí no se había corregido contra el propio archivo
que lo desmentía. Base tras la corrección de la 0.3.7: 105 requisito / 43 comportamiento (148),
recuento recomputado contra
`REVISIONES-Apps-V3.6.md` vigente (entrada 0.3.4, ya archivada): de las 8 nuevas de la 0.3.4 que este
mismo README contaba como comportamiento a falta de archivo, QA dijo que **5 pasan a requisito** —
`LectorDeFlujoTest` completa (3: la entrada 0.3.4 archiva literalmente "roturas de LectorDeFlujo...
vistas en rojo") y, de `EstadoDeteccionTest`, las 2 que citan C1/C2 por su nombre
(`salirDuranteConexionDejaDetectandoEnFalso`, `resultadoPublicadoTrasDestruirLaActivityLoMuestraLaNueva`
— la entrada archiva "C1 cerrada" y describe C2 con detalle); las otras 3 de `EstadoDeteccionTest`
(`alCrearloNoHayDeteccionEnCurso`, `iniciarPoneDetectandoEnCurso`,
`salirSinPublicarNoDejaResultadoPendiente`) siguen comportamiento: fijan el contrato de la clase, no
citan una condición. Base tras la corrección: 93 requisito / 41 comportamiento (134). **QA D-2:**
desde la 0.3.3, una prueba cuenta como requisito sólo si la condición que cita está escrita en
`05_Documentacion/REVISIONES-Apps-V3.6.md` (regla del propio fichero, `:58-59`) — no basta con que el
analista recuerde el informe del arquitecto o QA, si no quedó archivado.

**Las 4 nuevas de esta entrega (0.3.7):**

- **`EstadoMedidaTest`** (2 nuevas): T8 `marcarMidiendoPoneFaseMidiendoDeInmediato` (requisito, cita
  arq Bajo, `REVISIONES-Apps-V3.6.md:56-57`, "fase LIBRE entre la pulsación y `ejecutar()`"): llamar a
  `marcarMidiendo()` deja la fase en MIDIENDO sin arrancar ningún hilo. Rojo real: el método no existía
  contra 6e59f7b ("cannot find symbol", `EstadoMedidaTest.java:371`); demostrado también sin ese "no
  compila" quitando la llamada de `ejecutar()` y dejando la fase en LIBRE hasta que el hilo de fondo
  arranca — mismo síntoma que describe la condición. T9
  `oyenteEsAvisadoConFaseYaLibreAlTerminarSinPregunta` (comportamiento: fija el orden interno "fase
  baja ANTES de avisar", ya exigido por el Javadoc de `ejecutar()`, sin condición archivada propia).
  QA: T7 (0.3.5) sólo cuenta avisos, así que mutar el orden a "avisar antes de bajar la fase" seguía en
  verde (7/7); T9 lee `medida.fase()` DESDE DENTRO del propio callback del oyente y falla con esa
  mutación — capturado forzándola en una copia aislada del árbol (`ejecutar()` reordenado a
  avisar→bajar fase): 1/9 caída (`oyenteEsAvisadoConFaseYaLibreAlTerminarSinPregunta`); T7 sigue en
  verde con la misma mutación, confirmando el hueco que cierra T9.
- **`SerieDisparosTest`** (2 nuevas, de 14): `abandonarAntePreguntaCeroAnulaLaSerieSinFila` (requisito,
  cita arq C1, `REVISIONES-Apps-V3.6.md:54-55`): `Decision.ABANDONAR` ante la pregunta CERO anula la
  serie sin fila (`r.guardada == false`), a diferencia de SALTAR. Rojo real, en una copia aislada del
  árbol con `SerieDisparos` revertido a distinguir sólo `== SALTAR`: `IllegalStateException:
  preguntarDisparoAnulado() llamado sin respuesta programada` (la serie, al no anularse, intentaba
  medir un segundo disparo no programado). `abandonarAntePreguntaDisparoAnuladoAnulaLaSerieIgualQueSaltar`
  (comportamiento, robustez: ABANDONAR y SALTAR se comportan igual ante un disparo anulado — RF-USR-16
  no distingue el motivo). Misma reversión: `IllegalStateException` igual.
  `EstadoMedidaTest.abandonarConPreguntaPendienteTerminaMedirYNoCondenaAlSiguiente` (T5, ya existente)
  gana la aserción `assertNull(resultadoAbandonado.fila())`/`assertTrue(sesion.filas().isEmpty())`:
  rojo real revirtiendo sólo `EstadoMedida.abandonar()`/`preguntar()` a `Decision.SALTAR` (el estado de
  6e59f7b) — `AssertionError: expected null, but was: FilaMedida@...` (1/23 de la clase).

Producción, sin prueba nueva propia (cubiertas por lectura, capa Android sin arnés — README "Lo que NO
verifica esta corrida"): **arq Medio**, el `Runnable` de `oyenteMedida`/`oyenteDeteccion` no pinta si
`isFinishing()`/`isDestroyed()` (`MedirActivity.java:70`, `MainActivity.java:79`).

**Las 7 nuevas de esta entrega (0.3.6), todas requisito** (el esperado de cada una sale de
`SPEC-App-Usuario-V3.6.md:617-625`, "Operaciones que sobreviven a la pantalla", FABLE-USR — documento
escrito fuera del código que prueban, CLAUDE.md §7):

- **`EstadoMedidaTest`** (7 nuevas, clase nueva): T1 `serieConCeroDejaPreguntaCeroPendienteYSaltarGuardaLaFila`
  (una serie con un 0 deja CERO pendiente, visible desde otro hilo; "Saltar" guarda media=0,
  válido=NO, motivo `saturado_o_negativo`, entregado una sola vez); T2
  `disparoAnuladoDejaPreguntaAnuladoPendienteYSaltarAnulaLaSerie` (disparo anulado → ANULADO pendiente;
  "Saltar" anula la serie entera, sin fila); T3 `faseEsMidiendoMientrasMideYLibreAlTerminar` (fase
  MIDIENDO desde que arranca hasta que publica, LIBRE después, confirmado con concurrencia real —
  `CountDownLatch`, no temporización supuesta); T4 `excepcionEnMedirDejaErrorPendienteYFaseLibre`
  (B-1 de la 0.3.0: una excepción de `sesion.medir()` queda como error pendiente, fase LIBRE); T5
  `abandonarConPreguntaPendienteTerminaMedirYNoCondenaAlSiguiente` (`abandonar()` contestaba SALTAR en
  la 0.3.6 — desde la 0.3.7 contesta `Decision.ABANDONAR`, arq C1, ver arriba — y
  suelta el monitor de `SesionMedicion.java:100`; un medir posterior, desde otro hilo, no queda
  condenado a auto-saltar — confirma que `ejecutar()` resetea el abandono de la medida anterior); T6
  `responderSinPreguntaPendienteSeDescartaYNoContestaLaSiguiente` (una respuesta sin pregunta
  pendiente se descarta, no se cuela en la pregunta que viene después); T7
  `trasResponderNoQuedaPreguntaPendienteYElOyenteFueAvisado` (tras `responder()`, pendiente `null` y
  el oyente avisado del cambio de fase). Rojo por comportamiento: se anuló el cuerpo de `responder()`
  (no hace nada) y de `abandonar()` (no contesta nada), se recompiló y se corrió sólo esta clase con
  `JUnitCore` bajo `timeout` (los hilos de medir de las pruebas afectadas quedan bloqueados para
  siempre en su `ArrayBlockingQueue.take()`; `JUnitCore` los mata igual al llamar a `System.exit()` al
  terminar) — 5 de 7 caídas (T3 y T4 siguen en verde, correcto: no dependen de esos dos métodos); traza
  completa en el Javadoc de la clase.

**Las 7 nuevas de la 0.3.5 (contexto, no se repiten aquí; recuento requisito/comportamiento ya
corregido arriba):**

- **`EstadoDeteccionTest`** (5 nuevas): citan la entrada 0.3.4 de `REVISIONES-Apps-V3.6.md` ("Alto",
  C2 sigue abierta) — `elOyenteSeAvisaConDetectandoYaEnFalso` reproduce en dominio puro la carrera que
  describe la condición ("esa llamada se encola antes del finally que baja detectando"): el oyente ve
  `detectando() == false` en el instante del aviso; `publicarErrorDejaDetectandoEnFalsoYAvisaUnaVez` y
  `publicarSondaDejaDetectandoEnFalsoYSeEntregaUnaVez` cubren el camino de error y `reintentarSonda`
  (anotado por el arquitecto en la misma entrada); `quitarOyenteConOtraInstanciaNoQuitaElVigente` y
  `publicarSinOyenteRegistradoNoRevientaYDejaElResultadoPendiente` fijan el resto del contrato del
  oyente. Rojo por comportamiento capturado forzando temporalmente a `publicar()` a avisar ANTES de
  bajar `detectando` (un cambio de una línea, revertido después), corrido con `JUnitCore` sólo contra
  esta clase — 1 caída (`elOyenteSeAvisaConDetectandoYaEnFalso`, el síntoma exacto de la condición);
  traza completa en el Javadoc de la clase.
- **`SerieDisparosTest`** (2 nuevas, de 12): `disparoAnuladoConSaltarAnulaLaSerieEntera` (renombrada de
  `disparoAnuladoDosVecesAnulaLaSerieEntera`, reescrita: "Saltar" anula tras el PRIMER disparo sin
  respuesta, sin tope fijo) y `disparoAnuladoSinLimiteDeRepeticionesSiElOperadorSigueRepitiendo` (r7,
  "sin límite de repeticiones: decide el operador", DECISIONES nota 18) citan
  `SPEC-App-Usuario-V3.6.md:167-197` (RF-USR-04 r7). Rojo por comportamiento: se restauró el
  auto-repetir de la 0.3.4 dentro de `SerieDisparos` (ignorando a propósito la respuesta de
  `PreguntaOperador`) y se corrió sólo esta clase con `JUnitCore` — 6 de 12 caídas (no sólo las 2
  nuevas: el resto de la clase también prueba r7 aunque no sean métodos nuevos); traza completa en el
  Javadoc de la clase.

Las 8 de la 0.3.4 (contexto, no se repiten aquí; recuento requisito/comportamiento YA corregido arriba):

- **`LectorDeFlujoTest`** (3, requisito tras la corrección de arriba): los tres casos del encargo de QA
  sobre `EnlaceBluetooth.leerSinParar` (rotura no detectada por las 126 pruebas de la 0.3.3): (a)
  `IOException` → caído, (b) EOF (`read() == -1`) → caído, (c) bytes entregados → quedan en la cola y
  el estado sigue vivo mientras el flujo no ha terminado (usa dos hilos y dos pestillos para
  observarlo a mitad del bucle bloqueante). Rojo por comportamiento capturado quitando las dos
  llamadas a `marcarCaido()` de `LectorDeFlujo` (código de producción): los tres caen — ver el Javadoc
  de la clase para la traza completa.
- **`EstadoDeteccionTest`** (5 de la 0.3.4; 2 requisito, 3 comportamiento tras la corrección de arriba):
  `salirDuranteConexionDejaDetectandoEnFalso` (C1: "salida durante conexión → detectando=false") y
  `resultadoPublicadoTrasDestruirLaActivityLoMuestraLaNueva` (C2: "resultado publicado tras destruir
  la Activity → la nueva lo muestra") son los dos casos que pide la condición literalmente (requisito);
  los otros tres (`alCrearloNoHayDeteccionEnCurso`, `iniciarPoneDetectandoEnCurso`,
  `salirSinPublicarNoDejaResultadoPendiente`) fijan el resto del contrato de la clase, sin citar una
  condición (comportamiento). Rojo por comportamiento capturado con `salir()` vacío (sin
  `detectando = false`, tal como estaba el camino de MainActivity.java:192-194 de la 0.3.3 antes de
  esta entrega): sólo cae `salirDuranteConexionDejaDetectandoEnFalso` — ver el Javadoc de la clase
  para la traza completa.

Las 9 nuevas de la 0.3.3 (siguen contando, sin repetirse aquí):

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
#    no el hash del .apk como blob, que zipflinger puede reordenar entre builds identicos. LC_ALL=C
#    fija el orden de "sort" (evita que el orden de "nombre" dependa del locale de quien lo corre:
#    con un locale distinto, mayusculas/minusculas o acentos pueden ordenar distinto y dar un sha256
#    final distinto sobre el MISMO contenido).
for f in $(unzip -Z1 "$APK" | LC_ALL=C sort); do
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
git ls-files app/src/main app/build.gradle app/proguard-rules.pro build.gradle settings.gradle \
    gradle.properties | LC_ALL=C sort | while IFS= read -r f; do
  h=$(git cat-file -p "HEAD:03_App_Movil/RetroUsuario/$f" | md5sum | cut -d' ' -f1)
  printf '%s *%s\n' "$h" "$f"
done > fuente.md5
```

Verificado en la 0.3.6 con `git worktree add --detach <ruta> <commit>` (checkout limpio, sin nada del
árbol de quien lo generó) + `md5sum -c fuente.md5`: 71/71 `OK` (69 de la 0.3.5 más
`dominio/EstadoMedida.java` y `dominio/Oyente.java`, nuevos de esa entrega, FABLE-USR).

**0.3.7 (esta entrega):** mismos 71 ficheros (ninguno nuevo; `manifestPlaceholders = [etiqueta: "Retro Coviandina"]`
en `app/build.gradle` y `${etiqueta}` en `AndroidManifest.xml`). Verificado en checkout limpio con
`md5sum -c fuente.md5`: **71/71 `OK`**. Compilación `assembleDebug` limpia:
`package: name='com.dpi.retrousuario.coviandina' versionCode='10' versionName='0.3.7'`,
`application-label:'Retro Coviandina'` (`aapt dump badging`); certificado de depuración
`Signer #1 certificate SHA-256 digest: c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`
(`apksigner verify --print-certs`); hash por CONTENIDO (receta de arriba, paso 3):
**`b7a3917cb7b3e2367a0efe841533d826b310537fc0c1f13d0ffcfbdd3a402882`**. md5 del `.apk` (declarado, no
comparable entre builds — trazabilidad del binario empaquetado):
**`6aecf3ac2251db29f78b74d324b920cc`**, entregado como `03_App_Movil/RETRO-COVIANDINA-usuario-0.3.7-10.apk`
(y copia de compatibilidad `03_App_Movil/RetroUsuario-0.3.7.apk`).

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

**De la 0.3.5, sin arnés (capa Android):** que el `Oyente` de `MainActivity` (registrado en
`onResume`, retirado en `onPause`) repinte de verdad en un giro de pantalla real sobre un
`Handler(Looper.getMainLooper())` real — la decisión "detectando baja antes de avisar" sí tiene
prueba JVM (`EstadoDeteccionTest`), el cableado con un `Handler`/ciclo de vida real de Android no.

**Nuevo de la 0.3.6 (FABLE-USR), sin arnés (capa Android), para la prueba de Diego en dos teléfonos:**
el bug de la 0.3.5 (un giro de pantalla A MITAD del diálogo "Repetir o Saltar" dejaba el hilo de medir
bloqueado para siempre) está corregido en dominio con prueba JVM (`EstadoMedidaTest`, T5:
`abandonar()`/`responder()` desde otro hilo, no una Activity real), pero el cableado con Android real
no se ha visto: que `MedirActivity` re-muestre de verdad el diálogo "Repetir o Saltar" tras un giro
real (Activity nueva, `EstadoMedida.preguntaPendiente()` no nulo, `onResume()` la re-muestra) y que
"Saltar"/"Repetir" tocado en la Activity NUEVA desbloquee de verdad el hilo de medir que arrancó la
Activity VIEJA; que los botones de color nazcan deshabilitados en la Activity nueva si el giro
ocurrió a mitad de una medida sin pregunta (fase MIDIENDO); que `onPause()` cierre el diálogo del
sistema sin que quede un segundo diálogo apilado al volver a `onResume()`; que cambiar de equipo o
salir con Atrás desde `MainActivity` mientras `MedirActivity` tiene una pregunta pendiente la
abandone de verdad (serie anulada) y libere el hilo de medir en un teléfono real, no sólo en la JVM;
que Atrás desde `MedirActivity` (sin cambiar de equipo) no interrumpa una medida en curso, y que al
volver se vea "Midiendo…" o el resultado, según toque.
