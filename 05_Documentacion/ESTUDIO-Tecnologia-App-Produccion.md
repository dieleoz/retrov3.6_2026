# Estudio de viabilidad tecnológica — app de producción del Retrorreflectómetro Vertical

**Sin medir.** Este estudio sale de leer código y documentación oficial a fecha 19-sep-2026. Nada de lo
que dice sobre Web Serial en Android, ni sobre ningún plugin multiplataforma, se ha probado contra el
módulo Bluetooth del equipo. Las cifras de esfuerzo son estimaciones, no mediciones.

## Resumen (una página)

**Recomendación: seguir en Android nativo, sin reescribir.** Separar el código Java puro que ya existe
(19 clases, 3085 líneas, con sus 6 ficheros de test JVM) en un módulo de dominio, poner el Bluetooth y
el protocolo detrás de interfaces, y construir las funciones nuevas encima. Kotlin se admite para el
código nuevo, pero no es condición ni motivo de migración.

Por qué, en cinco líneas:

1. Lo que obliga es **que sea nativo Android**, no Java: el SPP se usa con la misma API
   (`BluetoothDevice.createRfcommSocketToServiceRecord`) desde Java y desde Kotlin.
2. La web **sí** puede hablar SPP en Android desde Chrome 138 (Web Serial sobre RFCOMM), pero es
   soporte parcial, sólo en Chrome, no en WebView ni en iOS, y sin eventos de conexión en Android.
3. Flutter, React Native y Capacitor dependen de plugins SPP de un solo mantenedor, varios abandonados;
   el riesgo cae justo sobre la pieza que no puede fallar.
4. Cualquier cambio de lenguaje obliga a reescribir el dominio probado (tramas, Inversión, Ajuste,
   Coherencia, Campaña) y sus tests; quedarse lo reutiliza tal cual.
5. iOS no aporta nada: sin MFi no hay SPP clásico en iPhone, y el equipo no es MFi. El multiplataforma
   no tiene aquí su principal ventaja.

**Lo que sí hay que hacer, con o sin cambio de tecnología:** subir `targetSdk` (hoy 30) y pedir
`BLUETOOTH_CONNECT` en ejecución. Eso exige cambiar la cadena de compilación entera (Gradle 6.5 + AGP
4.1.1 + JDK 11 → Gradle/AGP 8.x o 9.x + JDK 17). Es el trabajo más urgente y no depende de esta
decisión.

**Visor para la interventoría:** si hace falta, es un **producto aparte** — una web estática que lee los
CSV/XLSX/ZIP exportados, sin Bluetooth. No condiciona la app de medida.

---

## 1. Hechos del sistema, verificados

| Hecho | Evidencia | Estado |
| :--- | :--- | :--- |
| El enlace es SPP clásico (RFCOMM), UUID `00001101-0000-1000-8000-00805F9B34FB` | `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/EnlaceSerie.java:33` | Confirmado |
| La app V3.6 abre primero socket inseguro y cae a seguro | `EnlaceSerie.java:157` (`createInsecureRfcommSocketToServiceRecord`) y `:168` (`createRfcommSocketToServiceRecord`) | Confirmado |
| La app de campo V4.1 también usa socket inseguro | `D:\IT\P_RetroReflectometro_Vertical\03_App_Movil\RetroVerticalP1\app\src\main\java\com\example\retrohorizontalp1\medicion.java:1661` y `dispositivos.java:128` | Confirmado |
| V3.6 a 9600 baudios | `01_Firmware/RetroVertical_V3.6.X/mcc_generated_files/uart1.c:114,124,127` (`U1CON0 = 0xB0`, BRGS = 1; `U1BRG = 0x01A0` = 416) con `mcc.c:74` (`OSCFRQ = 0x05`, 16 MHz) y `device_config.h:50`. 16 000 000 / (4 × 417) = 9592 baud, −0,08 % de 9600 | Confirmado por cálculo |
| V4.1 a 115200 y `@LEERV,...@` | `CLAUDE.md` del repositorio V5, §5 (`uart1.c:114,123,126` + `mcc.c:65-71`) | Tomado de ese repositorio, no recalculado aquí |
| `targetSdk 30`, `minSdk 24`, AGP 4.1.1, Java 8 | `03_App_Movil/RetroV36/app/build.gradle:7,12,13` y comentario de cabecera | Confirmado |
| Permisos: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`; sin `INTERNET` | `app/src/main/AndroidManifest.xml:9-11` | Confirmado |
| El protocolo ya distingue V3.6 y V4 en una sola clase | `Tramas.java:24` (`SONDA_V4 = "@LEERV,BLA,1@"`), `:46` (`peticionPermitida(String p, boolean esV36)`); `Receptor.java` cierra `#...#`, `:n:`, `::n` y `@LEERV,...@` | Confirmado: hay un booleano, no una interfaz |

### Contradicciones con el encargo

- **"El OUI 00:21:13 apunta a HC-05/HC-06": falso según el registro.** El registro IEEE MA-L asigna
  `00:21:13` a **Padtec S/A** (Campinas, Brasil), comprobado en dos servicios que replican el registro
  (`api.macvendors.com` y `macvendorlookup.com`). Que los módulos HC-0x chinos aparezcan con ese prefijo
  es una observación de comunidad —esos módulos usan prefijos que no son suyos—, no una asignación. El
  OUI **no identifica** el módulo. Además, `ROADMAP-MEJORAS-App.md:128` lo llama **HC-06** y el encargo
  dice HC-05/HC-06. **Contradicción abierta:** se cierra leyendo el nombre Bluetooth anunciado o con
  comandos AT sobre el módulo, no razonando. Para este estudio da igual: HC-05 y HC-06 son ambos SPP
  clásico, sin BLE.
- **"Si en Android la web no puede, queda descartada": la premisa no se cumple.** Web Serial sobre
  RFCOMM existe en Chrome para Android (sección 2). La web no queda descartada por imposibilidad; queda
  por detrás por riesgo y coste.

## 2. ¿Puede una web o PWA hablar con un SPP clásico en Android?

**Sí, desde Chrome 138 para Android, con soporte parcial.** No, desde una WebView ni desde iOS.

| API | Qué cubre | Android | Fuente |
| :--- | :--- | :--- | :--- |
| Web Bluetooth | Sólo BLE (GATT). No sirve para HC-05/HC-06 | Chrome 56+ (BLE) | [MDN — Web Bluetooth API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Bluetooth_API) |
| Web Serial sobre RFCOMM, escritorio | Puertos SPP de equipos emparejados | Chrome 117+ escritorio | [Chrome for Developers — Serial over Bluetooth on the web](https://developer.chrome.com/blog/serial-over-bluetooth): *"Starting in Chrome 117 on desktop, web developers can now reliably communicate with paired Bluetooth Classic devices through RFCOMM services using the Web Serial API."* |
| Web Serial sobre RFCOMM, Android | Sólo puertos emulados por RFCOMM | **Chrome 138** (parcial) | Datos de compatibilidad de MDN (`@mdn/browser-compat-data` 8.1.2, 17-sep-2026), `api.Serial`, `chrome_android`: *"Serial ports are only available if they're provided by Bluetooth RFCOMM serial port emulation"*, `version_added: 138`, `partial_implementation: true`. [Chrome Status 5139978918821888 — Web serial over Bluetooth on Android](https://chromestatus.com/feature/5139978918821888) |
| Web Serial completo (cable) en Android | Puertos serie por cable vía la Android Serial API | Chrome 148, pocos equipos | [Chrome Status 6043992171085824 — Web Serial API on Android](https://chromestatus.com/feature/6043992171085824); [PSA en blink-dev, 10-feb-2026](https://groups.google.com/a/chromium.org/g/blink-dev/c/yGhvQ6mEmcY) |
| Android WebView | — | **No implementado** | MDN BCD `webview_android: false`; [Intent to Ship en blink-dev](https://groups.google.com/a/chromium.org/g/blink-dev/c/BqUGCcurReE): *"the Webview embedder will not have it"* |
| Safari / iOS | — | No | MDN BCD `safari` y `safari_ios`: `false` |

Discrepancia menor entre fuentes, que no cambia la conclusión: el *Intent to Ship* habla de M137;
MDN y Chrome Status dan 138. Se toma **138**.

Limitaciones que pesan para un equipo de medida, según el *Intent to Ship*:

- En Android, *"the connected/disconnected events and SerialPort.connected property can't be fully
  supported on Android over Bluetooth yet due to lack of system support"*. Detectar que el equipo se
  ha apagado o alejado queda a cargo de temporizadores propios.
- Exige Chrome. Un teléfono HONOR con navegador propio por defecto necesita Chrome instalado.
- La selección del puerto requiere gesto del usuario (`requestPort()`); `getPorts()` recupera los ya
  autorizados.
- **No probado con el módulo del equipo a 9600 baud.** Hasta que se pruebe, es una posibilidad en papel.

Conclusión: **una PWA podría medir en Android**, pero sobre una API marcada como parcial, limitada a
un navegador, y reescribiendo todo el dominio en TypeScript. Cambiar el módulo del equipo a BLE para
usar Web Bluetooth sería un **cambio de hardware y está fuera de alcance**.

## 3. ¿Se usa Java "por la antigüedad del Bluetooth"?

No. El SPP clásico no tiene nada que ver con el lenguaje. `BluetoothAdapter`, `BluetoothDevice` y
`BluetoothSocket` son la misma API de Android, y la guía oficial muestra el mismo flujo en Java y en
Kotlin ([Connect Bluetooth devices](https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices)).
Kotlin llama a esas clases exactamente igual que Java, y puede convivir con las clases Java existentes
en el mismo módulo.

Lo que de verdad obliga es **que el código que abre el socket RFCOMM sea nativo Android**: Java,
Kotlin, o el código nativo de un plugin (Flutter, React Native, Capacitor), que por dentro llama a
esa misma API. Lo que sí es antiguo aquí es la **cadena de compilación** (Gradle 6.5, AGP 4.1.1,
JDK 11, `targetSdk 30`), y eso se arregla sin cambiar de lenguaje.

## 4. Requisitos de plataforma que aplican a cualquier opción

| Requisito | Qué dice la fuente | Consecuencia |
| :--- | :--- | :--- |
| Google Play, `targetSdk` | Desde el 31-ago-2026, *"New apps and app updates must target Android 16 (API level 36) or higher"*; prórroga solicitable hasta el 1-nov-2026. [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk) | Sólo si se publica en Play. Hoy el APK se instala a mano (`compilar-apk`); `targetSdk 30` no impide instalarlo, pero conviene no quedarse atrás |
| Permisos Android 12+ | Con `targetSdk` ≥ 31: `BLUETOOTH_CONNECT` para hablar con equipos emparejados y `BLUETOOTH_SCAN` para buscar; son permisos en tiempo de ejecución; los heredados, con `android:maxSdkVersion="30"`. [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) | Hoy la app funciona en Android 15 porque con `targetSdk 30` rige el modelo antiguo. Al subir, pedir `BLUETOOTH_CONNECT` en `ConexionActivity`. `BLUETOOTH_SCAN` no hace falta mientras sólo se usen equipos emparejados (`ROADMAP-MEJORAS-App.md:128`) |
| AGP mínimo por API | API 35 → AGP 8.6.0; API 36 → AGP 8.9.1. [About AGP](https://developer.android.com/build/releases/about-agp) | Con AGP 4.1.1 no se puede compilar contra API 35/36. AGP 8.x exige JDK 17, y Gradle 6.5 no arranca con JDK 17: se cambian los tres a la vez |

## 5. Comparativa

Base de código medida el 19-sep-2026 en `03_App_Movil/RetroV36/app/src/`:

- `main/java`: **33 clases, 7183 líneas**.
  - **19 clases sin ningún `import android`, 3085 líneas**: Ajuste, Asistente, Campana, Coherencia,
    Cola, Csv, Ecuacion, Estadistica, Fabrica, Hex, Importador, Inversion, LecturaX, Medida, Patron,
    Receptor, Repetibilidad, Tramas, Veredicto.
  - **14 clases con dependencias de Android, 4098 líneas**: actividades, `EnlaceSerie`, `Base`,
    `Registro`, `Sesion`, `Campanas`, `Cliente`, `LineaBase`, `Pruebas`.
- `test/java`: **6 ficheros de test JVM (JUnit 4), 1213 líneas**.

La app de campo V4.1 (`RetroVerticalP1`) tiene 10 clases y 2656 líneas.

| Criterio | Android Java (hoy) | Android Kotlin (± Compose) | Flutter | React Native | Capacitor/Ionic + plugin SPP | Nativo BT + WebView UI | PWA (Web Serial) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| SPP en Android 12-15 | API oficial, ya probada en HONOR Android 15 | La misma API | Vía plugin | Vía plugin | Vía plugin | API oficial (parte nativa) | Chrome 138+, parcial, sin eventos de conexión |
| Librería SPP y mantenimiento | No hace falta | No hace falta | `flutter_bluetooth_serial` 0.4.0, última versión 17-ago-2021, 102 incidencias abiertas: **abandonado**. Bifurcaciones de un solo autor: `flutter_bluetooth_serial_plus` 0.5.6 (ago-2026), `flutter_blue_classic` 0.1.1 (jun-2026), `flutter_bluetooth_classic_serial` 1.3.2 (oct-2025) | `react-native-bluetooth-classic`: última estable 0.10.12 (nov-2020); la 1.73 sigue en `rc` (nov-2025) | `@e-is/capacitor-bluetooth-serial` 6.0.3 (dic-2024), un mantenedor; `capacitor-bluetooth-serial` 0.0.4 (2020) abandonado; `@capacitor-community/bluetooth-le` es sólo BLE | No hace falta | No hace falta, pero API parcial |
| GPS | `LocationManager` / Fused | Igual | Plugin | Plugin | Plugin | Nativo o Geolocation web | Geolocation API |
| Mapa sin conexión | MapLibre Native (activo) o mapsforge (activo); osmdroid está **archivado** desde 2024 | Igual | `flutter_map` 8.3.2 (ago-2026) + teselas locales | MapLibre RN | Leaflet/MapLibre GL JS con teselas en caché | Leaflet/MapLibre GL JS en WebView | MapLibre GL JS + Service Worker (almacenamiento acotado por el navegador) |
| Base local | SQLite / Room (Room admite Java) | Room | `sqflite` / drift | SQLite por plugin | SQLite por plugin | Nativa | IndexedDB / OPFS |
| PDF | `android.graphics.pdf.PdfDocument` del sistema | Igual | Paquete `pdf` | Plugin | jsPDF | jsPDF o nativo | jsPDF |
| CSV/XLSX/ZIP y compartir | `java.util.zip` + `Intent.ACTION_SEND` (ya en uso) | Igual | Paquetes | Paquetes | Paquetes | Nativo | Web Share API / descarga |
| Reutiliza el dominio probado | **Tal cual (3085 líneas + tests)** | **Tal cual**: Kotlin llama a Java | No: reescribir en Dart | No: reescribir en TS o puentear | No: reescribir en TS | Sólo si el dominio queda en la parte nativa | No: reescribir en TS |
| Sirve para iOS | No | No | Interfaz sí, **SPP no** | Interfaz sí, **SPP no** | Interfaz sí, **SPP no** | No | No (Safari sin Web Serial) |
| Esfuerzo estimado de llegar a la app actual (sin mediciones) | 0 | 0 para el dominio; UI nueva a ritmo de las funciones nuevas | 6-10 semanas-persona | 6-10 semanas-persona | 5-8 semanas-persona | 3-5 semanas-persona | 5-8 semanas-persona |
| Riesgo principal | Cadena de compilación vieja | Dos lenguajes en el mismo proyecto | Plugin SPP de un autor | Plugin SPP sin estable reciente | Plugin SPP de un mantenedor | Dos pilas y un puente propio | API parcial, sólo Chrome |

Las semanas-persona son **estimaciones de orden de magnitud**, no mediciones: suponen reescribir el
dominio y los tests y rehacer las 14 clases de Android. Sirven para comparar, no para planificar.

Sobre iOS: en iPhone el Bluetooth clásico para accesorios sólo es accesible por el marco External
Accessory, reservado a accesorios del programa MFi de Apple; Core Bluetooth es sólo BLE.
[External Accessory](https://developer.apple.com/documentation/externalaccessory),
[Core Bluetooth](https://developer.apple.com/documentation/corebluetooth),
[programa MFi](https://mfi.apple.com/). El módulo HC-0x no es MFi. (La página de Apple se renderiza con
JavaScript y no se pudo citar literalmente con la herramienta usada; la conclusión es la conocida y no
depende de este estudio.)

## 6. Arquitectura recomendada

Un proyecto Android con tres módulos Gradle:

```
:dominio   (java-library, JVM puro, sin Android)
    protocolo/   Protocolo (interfaz), ProtocoloV36, ProtocoloV4, Receptor, Tramas
    calculo/     Inversion, Ajuste, Coherencia, Ecuacion, Estadistica, Repetibilidad, Veredicto
    campana/     Campana, Asistente, Fabrica, Patron, Medida, LecturaX
    exportar/    Csv, Importador (+ XLSX y ZIP, sin Android)
    indicador/   (nuevo) medidas periódicas e indicador para la interventoría
    tests JVM:   los 6 ficheros actuales + los nuevos
:enlace    (android-library)   Enlace (interfaz) + EnlaceSpp (hoy EnlaceSerie) + EnlaceSimulado
:app       (android-application) actividades, Room, GPS, mapa, PDF, compartir
```

Capas, de abajo arriba:

1. **Bluetooth (`:enlace`).** Una interfaz mínima: `conectar(mac)`, `enviar(bytes)`, oyente de bytes
   recibidos y de estado. `EnlaceSpp` es el `EnlaceSerie.java` actual casi sin tocar. Un
   `EnlaceSimulado` que reproduce tramas grabadas permite probar la app entera sin equipo.
2. **Protocolo (`:dominio`).** Interfaz `Protocolo` con lo que hoy hace `Tramas` con el booleano
   `esV36`: construir peticiones, decidir qué peticiones están permitidas, delimitar la respuesta y
   extraer el valor.
   - `ProtocoloV36`: tramas `#...#`, `:n:` y `::n` (cierre por silencio de `Receptor.SILENCIO_MS`),
     9600 baud.
   - `ProtocoloV4`: `@LEERV,<COLOR>,<TIPO>@`, color de exactamente 3 letras, respuesta entera, cierre en
     el `@` y **no** en `\r\n` (el terminador son los caracteres literales `/n/r`), 115200 baud.
   - Se elige por la sonda de versión que ya existe (`Tramas.SONDA_V4`) o por configuración del equipo.
     El baudio no lo ve la app —lo fija el módulo Bluetooth—; lo que cambia es la trama.
3. **Dominio.** Las clases de cálculo y campaña, tal cual, más el indicador nuevo. Sin Android, sin
   reloj propio (el `Receptor` ya recibe la hora de quien llama): todo se prueba en JVM.
4. **Datos locales.** Room sobre SQLite para campañas, medidas, inventario de señales con
   coordenadas y medidas periódicas. "Señales cercanas" es una consulta por caja de coordenadas; no
   necesita red. Todo el trabajo en carretera va sin conexión; la red sólo se usa al compartir.
5. **Exportación.** CSV, XLSX y ZIP generados en `:dominio`; PDF con `PdfDocument` en `:app`;
   salida por `Intent.ACTION_SEND` como hoy, sin permiso `INTERNET`.

Orden de trabajo propuesto:

1. Cadena de compilación nueva (Gradle + AGP 8.x/9.x + JDK 17) y `targetSdk` 35/36 con
   `BLUETOOTH_CONNECT`. **Validar en el HONOR Android 15 contra el equipo** antes de seguir.
2. Extraer `:dominio` moviendo las 19 clases puras sin cambiar su código; los 6 tests deben pasar
   igual.
3. Introducir `Protocolo` y `Enlace` detrás de lo que ya hay, sin cambio de comportamiento.
4. Funciones nuevas: inventario georreferenciado, medidas periódicas, PDF de calibración, XLSX.

**Visor para la interventoría (opcional, producto aparte).** Una web estática que abre los ZIP/CSV/XLSX
exportados y muestra registros, mapa e indicador. No mide, no usa Bluetooth, no comparte código con la
app salvo el formato de exportación, que hay que fijar por escrito. Podría reutilizar el dominio si se
compila a JavaScript, pero no merece la pena al principio: el visor sólo lee.

## 7. Qué queda por medir

- Nombre y modelo real del módulo Bluetooth (HC-05 o HC-06): el OUI no lo dice.
- La app con `targetSdk` 35/36 y `BLUETOOTH_CONNECT` conectando en el HONOR Android 15.
- Si se quiere mantener abierta la vía web: una página mínima con Web Serial en Chrome para Android
  138+ que abra el puerto RFCOMM del equipo y lea una trama `#...#` a 9600. Es una prueba de una tarde y
  cerraría la pregunta con el equipo delante.

## Fuentes

- Chrome for Developers, *Serial over Bluetooth on the web*: https://developer.chrome.com/blog/serial-over-bluetooth
- Chrome for Developers, *Bluetooth RFCOMM updates in Web Serial*: https://developer.chrome.com/blog/bluetooth-rfcomm-updates-web-serial
- Chrome Status, *Web Serial support for Bluetooth RFCOMM services* (escritorio 117): https://chromestatus.com/feature/5686596809523200
- Chrome Status, *Web serial over Bluetooth on Android* (Android 138): https://chromestatus.com/feature/5139978918821888
- Chrome Status, *Web Serial API on Android* (cable, Android 148): https://chromestatus.com/feature/6043992171085824
- blink-dev, *Intent to Ship: Web serial over Bluetooth on Android*: https://groups.google.com/a/chromium.org/g/blink-dev/c/BqUGCcurReE
- blink-dev, *PSA: Web Serial API on Android*: https://groups.google.com/a/chromium.org/g/blink-dev/c/yGhvQ6mEmcY
- MDN, *Web Serial API*: https://developer.mozilla.org/en-US/docs/Web/API/Web_Serial_API
- MDN, *Web Bluetooth API*: https://developer.mozilla.org/en-US/docs/Web/API/Web_Bluetooth_API
- MDN browser-compat-data 8.1.2 (17-sep-2026), `api.Serial`: https://github.com/mdn/browser-compat-data
- Android Developers, *Bluetooth permissions*: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- Android Developers, *Connect Bluetooth devices*: https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices
- Android Developers, *Target API level requirements for Google Play apps*: https://developer.android.com/google/play/requirements/target-sdk
- Android Developers, *About the Android Gradle Plugin*: https://developer.android.com/build/releases/about-agp
- Apple, *External Accessory*: https://developer.apple.com/documentation/externalaccessory
- Apple, *Core Bluetooth*: https://developer.apple.com/documentation/corebluetooth
- pub.dev y registro npm, consultados el 19-sep-2026 para las fechas de publicación de los plugins.
- Registro IEEE MA-L para `00:21:13`, vía https://api.macvendors.com y https://www.macvendorlookup.com.
