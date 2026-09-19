# ROADMAP de mejoras — app V3.6

**Estado:** propuesta, 18-sep-2026. **Nada de esto está medido ni implementado.** La app V3.6 todavía
no existe: `03_App_Movil/` de este repositorio está vacío (comprobado). *[Corrección de la segunda
pasada, 18-sep-2026: ya no es cierto. Existe `03_App_Movil/RetroV36/`, que según su `README.md:3`
compila y pasa 42 tests JVM, sin probar contra ningún equipo. Ver §8.0.]* Este documento recoge las
mejoras ya identificadas para la app de campo en el repositorio V5 (`D:\IT\P_RetroReflectometro_Vertical`)
y propone cuáles aplican a la V3.6, cuáles se adaptan y cuáles no.

Según `ROADMAP.md` (AHORA, punto 4), estas mejoras **no bloquean la V3.6**. La sección 5 propone un
matiz: un núcleo pequeño debería entrar en P5 porque la app V3.6 va a **escribir coeficientes en la
EEPROM** de un equipo sin copia de su firmware. Es una propuesta y la decide el propietario.

---

## 1. Convenciones y método

- **Fuentes (sólo lectura), repositorio V5:** `MEJORAS.md`, `ROADMAP.md`,
  `05_Documentacion/SPEC-05`, `SPEC-06`, `SPEC-07`, `TDD-01`, `REVISION-Arquitectura-Mejoras.md`
  (en adelante REVISION), `REBATE-Spec-Funcional-V5.md` (REBATE) y `VALIDACION-Spec-Cliente-APK.md`
  (VALIDACION).
- **Rutas de código.** `medicion.java`, `dispositivos.java`, `crearproyecto.java`, `archivos.java` y
  `Enviarcorreo.java` están en
  `D:\IT\P_RetroReflectometro_Vertical\03_App_Movil\RetroVerticalP1\app\src\main\java\com\example\retrohorizontalp1\`.
  `AndroidManifest.xml` y `app/build.gradle` cuelgan de `...\RetroVerticalP1\app\`. Los ficheros de
  firmware 2020 (`gui.c`, `uart_module.c`, `ecuacionesCalibracion.c`) están en
  `01_Firmware/base_2020_d089f962/RetroVertical1.X/` de **este** repositorio. `Aplicacion.c` y
  `Serial.c` son del firmware V4.1, en `01_Firmware/18f47k42_RetroV_V4.1.X/` del repositorio V5.
- **Toda cita `archivo:línea` de este documento se ha abierto en el fichero el 18-sep-2026.** Cuando
  una cita viene sólo de un documento de origen y no la he comprobado, se dice.
- **Prioridad propuesta:**
  **antes de campo** = debe estar en la app antes de usarla contra un equipo en P7 (grabación) o
  P8 (calibración) · **después** = tras P8 · **no** = no se hace.

## 2. Qué cambia al pasar del protocolo V4 al V3

Casi todas las adaptaciones salen de esta tabla. El contrato V3.6 es `05_Documentacion/PROTOCOLO-V3.6.md`.

| Aspecto | App de campo (V4, `@LEERV`) | V3 / V3.6 | Evidencia V3 |
| :--- | :--- | :--- | :--- |
| Petición | `@LEERV,<COLOR>,<TIPO>@` | Un byte: `1`-`8`, `a`-`d` (12 ecuaciones), `9` batería, `e` lectura interna sin ecuación | `ecuacionesCalibracion.c:141-193`, `gui.c:328` |
| Respuesta | `@LEERV,<n>@/n/r` + `0x00` | `::<n>` **sin terminador**; batería `:<n>:` | `ecuacionesCalibracion.c:60-63`, `gui.c:336-339` |
| Fin de trama | El segundo `@` | **No hay delimitador de fin.** Sólo silencio o tiempo | Ídem |
| Byte nulo final | Sí (`Serial.c:31,33`) | **No**: `sendUartStr` envía `strlen` bytes | `uart_module.c:45-48` |
| Banner de arranque | Sí (`Aplicacion.c:94-99`) | **No**: sólo hay dos emisores por UART1 | `ecuacionesCalibracion.c:63`, `gui.c:339` |
| Byte no reconocido | Trama inválida | **Mide igualmente y no responde nada**: cualquier byte recibido dispara la adquisición | `gui.c:295-297` y cadena de `ecuacionesCalibracion.c:141-193` sin `else` |
| Valor `0` | Un valor | **Ambiguo**: el firmware pone a 0 todo valor > 4000, y el valor es `unsigned int`, de modo que un resultado negativo de la ecuación puede acabar ahí. **Sin medir** | `ecuacionesCalibracion.c:49-53`, `gui.c:30` |
| Velocidad | 115200 | 9600 8N1 | `PROTOCOLO-V3.6.md` §1; barrido SLV-002 (`ROADMAP.md` V5, tabla SLV-002) |
| Buffer de recepción | — | 50 bytes, **sin comprobar límite**; se vacía a los 2 s del primer byte. *[Corrección, segunda pasada: **no se vacía por tiempo**. La tarea de `:27-39` existe pero su llamada está comentada en `uart_module.c:109`; el buffer sólo se vacía tras cada medida (`gui.c:342`). `SPEC-V3.6.md:32` ya lo decía]* | `uart_module.c:19,51-57`, `:33-36`, `:109` |
| Pausa tras responder | — | 500 ms | `gui.c:345-346` |
| Administración | No existe | Tramas `#...#` | `PROTOCOLO-V3.6.md` §3 |

Dos consecuencias que no están en ninguna fuente de origen y que condicionan todo lo demás:

1. **La lectura de `::<n>` sólo puede cerrarse por silencio.** Un lector que espere un terminador se
   cuelga. Un lector que corte en el primer paquete recibido puede cortar `::12` de `::1234`.
2. **Con firmware 2020 (SLV-002 hoy), sondear `#V#` dispara una medida y no devuelve nada**
   (`gui.c:295-297`). La app debe interpretar el silencio ante `#V#` como "firmware sin V3.6" y no
   como un fallo del enlace. En V3.6 una trama con `#` ya no mide (`PROTOCOLO-V3.6.md` §3).

---

## 3. Mejoras, una por fila

### 3.1 `MEJORAS.md` (M-xx)

| ID | Qué es | V3.6 | Por qué | Prioridad | Depende de |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **M-01** (`medicion.java:1606`) | Bucle sin cota buscando el `@` de cierre: una trama incompleta cierra la app | **Adaptada** | No hay `@` en V3, pero el requisito es el mismo: ninguna entrada, por mala que sea, cierra la app. El lector de `::<n>` y el de `#...#` se acotan por bytes leídos y por tamaño | Antes de campo | Analizador con pruebas (tramo 1) |
| **M-02** (`app/build.gradle:12`) | Permisos `BLUETOOTH_CONNECT`/`SCAN`, obligatorios con `targetSdk` ≥ 31 | **Sí** | Mismo Android. Con `targetSdk` 30 no hacen nada (REVISION, M-02) | Antes de campo **sólo si** la app nace con `targetSdk` ≥ 31; si no, después | M-17 (decisión de cadena de compilación) |
| **M-03** (`medicion.java:1601-1613`) | Fallo silencioso: no se distingue "no midió" de "midió cero" | **Sí** | Con más razón en V3: un byte desconocido mide sin responder, y `0` es ambiguo (§2). Tres desenlaces: válida, sin respuesta, no interpretable | Antes de campo | Analizador; textos de los tres mensajes acordados antes (M-03, riesgo funcional) |
| **M-04** (`Aplicacion.c:27`) | Arrastre de la global `error` entre mediciones | **No aplica** *[Corrección, segunda pasada: **Adaptada**; ver §8.2, PAT-0]* | Es del firmware V4.1. En 2020 cada medida parte de `getReflectivity15Adc()` (`gui.c:298`); no he encontrado estado equivalente que pase a la ecuación. *[Sí lo hay, condicionado al gatillo: el color de pantalla no se borra y, con el gatillo pulsado, `gui.c:311-315` aplica su ecuación antes que la del código Bluetooth (`gui.c:325-327`); con `e` la respuesta deja de ser la `x`. Documentado en `SPEC-V3.6.md:46`]* | No *[Antes de P8: instrucción de no tocar el gatillo y la prueba 4 de coherencia como detector]* | — |
| **M-05** | Rojo en tipo 1 (absorbida por M-20) | **No aplica** | Ya retirada en origen, y V3 no usa códigos de color de tres letras | No | — |
| **M-06** (`medicion.java:1592-1594`) | Espera fija de 3250 ms y una sola lectura | **Adaptada** | Partida en dos, como pide REVISION: **(a)** leer acumulando con fin por silencio y tiempo límite explícito; **(b)** E/S fuera del hilo de interfaz. En una app nueva (b) es diseño de partida, no una reestructuración de riesgo | Antes de campo | Medir el ciclo real de V3 (tramo 4) para fijar el tiempo límite |
| **M-07** (`medicion.java:1562`) | Fecha dependiente del idioma; ampliada por REVISION a "formato de registro estable" | **Sí** | Mismo problema de datos. Incluye juego de caracteres declarado, escape del separador, cabecera y versión de esquema | Antes de campo | Ninguna |
| **M-08** (`Ecuaciones.c:77-113`, V4) | Registrar la cuenta de ADC con papel tipo 1 | **Adaptada** | En V3 la lectura interna sin ecuación es el comando `e` (`ecuacionesCalibracion.c:191-193`). El registro debe decir que `x` **no es** retrorreflexión. **SLV-002 no tiene `e`** (`ROADMAP.md` V5, tabla SLV-002): hasta grabar V3.6, `x` se deduce invirtiendo la ecuación | Antes de P8 | P7; M-19 (saber si el equipo tiene `e`) |
| **M-09** | Tabla de patrones cargable, trazable, con tolerancia por patrón | **Adaptada** | La tabla existe: `06_Calibracion/patrones_certificados_P1-P31.csv`. Aplica íntegro el requisito 1 (dato cargable, nunca constantes en la app) y el de tolerancia por encima del suelo de resolución del equipo, que en V3 hay que medir | Antes de P8 | Repetibilidad medida (tramo 4) |
| **M-10** | Medido, referencia, desviación y veredicto en una vista | **Adaptada** | En V3 sirve para el ajuste de P8: comparar la lectura de cada patrón con su valor certificado, antes y después de escribir coeficientes | Antes de P8 | M-03, M-09 |
| **M-11** | Exportar el acta de cotejo | **Adaptada** | P8 exige "acta de antes y después" (`ROADMAP.md` P8). Debe llevar equipo, versión de firmware (`#V#`), coeficientes leídos (`#G`), tabla usada y medidas | Antes de P8 | M-19, M-12 |
| **M-12** (`Enviarcorreo.java:54-55`) | Credencial SMTP en el fuente; vía de entrega por decidir | **Sí** | La V3.6 no debe llevar credencial alguna. Se propone la opción A de M-12 (Intent de compartir) | Después (antes de P8 si el acta tiene que salir del teléfono) | Decisión escrita (RF-55) |
| **M-13** (`medicion.java:1603`) | Array `p3` de 10 caracteres sin uso | **No aplica** | Código de la app vieja; queda en la sección 6 como defecto a no repetir | No | — |
| **M-14** (`medicion.java:69`) | El socket nunca se cierra; REVISION añade `dispositivos.java:53,112` | **Sí** | Mismo patrón de conexión. Cerrar al salir y al perder el enlace; reintentar sin reiniciar la app | Antes de campo | Capa de enlace (tramo 2) |
| **M-15** | Carpeta `RetroHorizontalP1` anidada | **No aplica** | Asunto del repositorio V5 | No | — |
| **M-16** (`applicationId com.example.retrohorizontalp1`) | Identificador de paquete equivocado; no se cambia sin migración | **Adaptada** | La V3.6 nace sin datos previos: se fija un identificador propio desde el primer APK (como hacen `RetroDiagBT` y `RetroCotejoV1`) y **no se cambia después**. Si la V3.6 tuviera que leer proyectos de la app de campo, no puede: `archivos.java:19` usa almacenamiento privado (`ROADMAP.md` V5, punto 8) | Antes de campo (es una decisión del tramo 0) | Ninguna |
| **M-17** (`gradle-wrapper.properties:6`, `app/build.gradle:13,14`) | Cadena de compilación antigua; `versionCode` fijo en 1 | **Adaptada** | Proyecto nuevo: que compile sin `jcenter()`, **sin subir `minSdk` de 24** y con `versionCode`/`versionName` que identifiquen el binario. El resto de la modernización, después | `versionCode`: antes de campo. Resto: después | JDK 11 si se parte de Gradle 6.5 (`ROADMAP.md` V5, tramo 0) |
| **M-18** | Los APK eran de otra revisión del fuente | **Adaptada** | Sin tarea propia en origen; en la V3.6 se cumple con M-17: cada APK entregado lleva versión y se verifica contra el fuente (skill `compilar-apk` del repositorio V5) | Antes de campo | M-17 |
| **M-19** (`crearproyecto.java:53`, `dispositivos.java:65`) | El registro no dice qué equipo midió | **Adaptada** | Imprescindible con dos V3. En V3.6 la identidad se completa con la respuesta de `#V#` (versión, fecha, `CAL`/`DEF`), y "sin respuesta a `#V#`" = firmware 2020 | Antes de campo | Analizador `#...#` |
| **M-20** (`Serial.c:142`, V4) | Con `ROJ` en tipo 1 la iluminación queda heredada; se envía `NAR` | **No aplica** | Defecto del firmware V4.1. V3 no tiene ese reparto | No | — |
| **M-21** (`Serial.c:31,33`, V4) | Un `0x00` tras cada cadena | **No aplica** | El firmware 2020 no lo envía (`uart_module.c:45-48`). La V3.6 debe mantenerlo así; si lo cambiara, el lector lo toleraría igual | No | — |
| **M-22** | Reglas del fabricante: verde limón en amarillo, revisión contra patrón, apoyo sin luz | **Adaptada** | El manual transcrito es el del equipo IoT (`04_Manuales/Manual-Uso-Retro-Vertical-IoT.md` del repositorio V5). **Falta confirmar que las reglas valgan para V3.** La revisión contra patrón sí encaja con el modo de pruebas de P5 | Después | Confirmación con el fabricante o con el manual de V3 |
| **M-23** | ¿Uno o varios parámetros por medición (R_A, Y%)? | **Sí** | Decisión de modelo de datos: abrir el registro a N parámetros cuesta poco en una app nueva y mucho después | Después (la decisión); el modelo abierto, antes de campo | Decisión del propietario, pendiente en origen |

### 3.2 Hallazgos de REVISION, REBATE y VALIDACION que no tienen M propia

| ID | Qué es | V3.6 | Por qué | Prioridad | Depende de |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **REV-P4.2** (`crearproyecto.java:54-57`, `archivos.java:19,59`) | Crear un proyecto con nombre repetido trunca el anterior; `VerificaArch()` existe y nunca se llama | **Sí** | Pérdida de datos, independiente del protocolo | Antes de campo | Ninguna |
| **REV-P4.3** (`medicion.java:1548,1584-1589`) | El registro guarda "Tipo 1..11" y al equipo se le envía `1` o `2` | **Adaptada** | En V3 el código enviado (`1`-`8`, `a`-`d`) combina color e intensidad. El registro guarda **el byte enviado** y su significado, no la etiqueta de la interfaz | Antes de campo | RF-06 |
| **REV-P4.4** (`medicion.java:1591-1594`) | No se vacía la entrada antes de pedir: se puede leer una trama antigua bien formada | **Adaptada** | En V3 no hay banner, pero sí respuestas tardías tras un tiempo agotado y medidas disparadas por otro byte. Vaciar antes de enviar y **una sola petición en vuelo** (RF-12b) | Antes de campo | Capa de enlace |
| **REV-P4.7** (`Ecuaciones.c:66`, V4) | Azul sin calibración propia | **No aplica** | Es de las ecuaciones V4. V3 tiene ecuación propia de azul (`ecuacionesCalibracion.c:38` para azul opaco) | No | — |
| **RF-04** (REBATE §3; `medicion.java:109-114`) | Bluetooth apagado: se pide activarlo y no se espera la respuesta | **Sí** | Mismo Android | Antes de campo | Capa de enlace |
| **RF-05** (REBATE §3; `medicion.java:1700,1709-1716`) | Denegar la localización no produce aviso; `grantResults[0]` sin comprobar longitud | **Sí** | Si la V3.6 guarda posición | Antes de campo | Ninguna |
| **RF-06** (REBATE §3) | Registrar la trama enviada y la recibida, literales | **Sí** | Más necesario que en V4: con `#S` la app **escribe** en la EEPROM, y el registro literal es la única prueba de qué se escribió | Antes de campo | Ninguna |
| **RF-08** (REBATE O-5; `archivos.java:16-28`) | Cada guardado reescribe el fichero entero y lo trunca | **Sí** | Una excepción a mitad pierde el proyecto | Antes de campo | Ninguna |
| **RF-09** (`AndroidManifest.xml:7`) | `allowBackup="true"`: los registros salen del teléfono en la copia de Android | **Sí** | Decidirlo expresamente | Después | Ninguna |
| **RNF-07** (REBATE §3) | Ninguna actividad declara `android:exported`; con `targetSdk` ≥ 31 no compila | **Sí** | Mismo Android | Con M-17 | M-17 |
| **RF-24** (SPEC-07 §4bis) | Ciclar la alimentación antes de cada sesión | **No aplica** | Su motivo es `error` y la iluminación del V4 | No | — |
| **RF-26** (SPEC-07 §3.3) | Forzar tipo 1 en la sesión de cotejo | **Adaptada** | Equivalente V3: la sesión de calibración de P8 mide **sólo con `e`**, y la app no deja enviar otro código en esa sesión | Antes de P8 | P7 (V3.6 grabado; SLV-002 no tiene `e`) |
| **RNF-03** (SPEC-07 §4) | Tiempo límite ≥ 6000 ms, sobre un ciclo de ~3015 ms | **Adaptada** | La cifra es del V4 y no está medida ni allí. En V3 **no hay cifra**: se mide el ciclo y el límite es al menos el doble (REBATE O-3) | Antes de campo | Tramo 4 |
| **VAL-2.4** (`medicion.java:1562`) | La hora es la del reloj del teléfono y se puede alterar | **Sí** | Pesa en un acta de calibración | Después | M-07 |
| **VAL-2.3** (VALIDACION §2.3) | Dictamen contra el Manual 2024, no "cumple NTC 4739"; SIT-01 a SIT-04 no se miden; catálogo incompleto | **Sí** | Sólo si la V3.6 incluye inspección de campo, no sólo calibración | Después | Alcance de la app V3.6, por decidir |

### 3.3 Defectos de `SPEC-05` (D-x, S-x)

Casi todos duplican una M. Se remiten a ella y **no se cuentan dos veces**.

| ID | Qué es | Se trata en | V3.6 |
| :--- | :--- | :--- | :--- |
| D1 | Faltan permisos de Android 12+ | M-02. **La afirmación de que la app "no funciona" en Android 12+ está retirada** (`MEJORAS.md` M-02) | — |
| D2 | Lectura en el hilo de interfaz | M-06 (b) | — |
| D3 | Sin acumulación de fragmentos | M-06 (a) | — |
| **D4** | Sin reintentos de conexión | Propia | **Sí**, antes de campo |
| D5 | `catch` silencioso al conectar | M-14 y sección 6 | — |
| D6 | Sin tiempo límite de lectura | M-06 (a) | — |
| D7 | Paquete "horizontal" | M-16 | — |
| **D8** | No se valida color ni tipo antes de enviar | Propia | **Adaptada**: sólo pueden salir bytes del conjunto del protocolo (`1`-`9`, `a`-`e`) y tramas `#...#` bien formadas, porque en V3 cualquier otro byte **mide** (`gui.c:295-297`). Antes de campo |
| D9, D13, D14 | CSV sin escape, fecha local, sin cabecera | M-07 | — |
| **D10** | No escanea equipos sin emparejar | Propia | **No**: el HC-06 se empareja una vez; no justifica el permiso de escaneo |
| **D11** | Sin indicación durante la conexión | Propia (SPEC-07 RF-01) | **Sí**, antes de campo |
| D12 | No valida que el valor sea numérico | M-03 | — |
| **S1** | Registros en texto plano | Propia | **Sí**, después |
| **S2** | No se verifica que el emparejado sea el equipo | Propia | **Adaptada**: `#V#` identifica un V3.6; con firmware 2020 no hay forma por protocolo. Antes de campo |
| S3 | Credencial SMTP | M-12 | — |

### 3.4 Plan de pruebas `TDD-01` (T-xx)

Las pruebas no se cuentan como mejoras; se adaptan por bloques.

| Bloque | V3.6 | Adaptación |
| :--- | :--- | :--- |
| T-01 a T-18 (trama, N1) | **Adaptada** | Mismas particiones sobre `::<n>` y `:<n>:`: completa, partida en dos, carácter a carácter, vacía, sólo `::`, no numérica, 5 y 6 cifras, dos respuestas seguidas, ruido antes y después. Añadir **fin por silencio** y el caso `::0` (§2). T-12/T-13 (banner) se sustituyen por "respuesta tardía de una petición anterior" |
| T-20 a T-24 (petición) | **Adaptada** | Cada código produce su byte exacto; ningún otro byte sale; `#...#` ≤ 48 bytes y con los números en el formato de `PROTOCOLO-V3.6.md` §3. *[Corrección, segunda pasada: la revisión 1.1 manda, **≤ 96 bytes** y coeficientes con 9 cifras `%.8E` (`PROTOCOLO-V3.6.md:80-81`, O-01 y O-02)]*. T-24 no aplica |
| T-30 a T-38 (cotejo) | **Adaptada** | T-30/T-31 pasan a `e`; T-33 (borde de tolerancia) y T-35 (referencia 0) se mantienen: son los que hacen indefendible un acta |
| T-40 a T-49 (simulador, N2) | **Adaptada** | El simulador imita **al V3 con sus defectos**: sin terminador, silencio ante byte desconocido, `0` ambiguo, `#V#` mudo en firmware 2020, pausa de 500 ms. T-49 no aplica |
| T-50 a T-55 (registro) | **Sí** | T-51 (leer CSV de la versión anterior) sólo si la V3.6 debe leer proyectos de la app de campo |
| T-60 a T-62 (binario) | **Sí** | Sin credenciales; compila sin `jcenter()`; instala en `minSdk` 24 |
| T-70 a T-73 (banco) | **Adaptada** | Ver `::<n>` en un terminal serie a 9600; medir el ciclo; repetibilidad con el mismo patrón |
| T-74 | **No aplica** | Mide el arrastre de `error`, que es del V4 |

### 3.5 Recuento

| | Sí | Adaptada | No aplica | Total |
| :--- | ---: | ---: | ---: | ---: |
| `MEJORAS.md` (M-01 a M-23) | 6 | 11 | 6 | 23 |
| REVISION, REBATE, VALIDACION sin M propia | 9 | 4 | 2 | 15 |
| `SPEC-05` propias (D4, D8, D10, D11, S1, S2) | 3 | 2 | 1 | 6 |
| **Total** | **18** | **17** | **9** | **44** |

---

## 4. Las cinco prioritarias

1. **Analizador de tramas con pruebas en rojo primero** (M-01, M-06 a, RF-14). `::<n>` sin
   terminador se cierra por silencio; `#...#` por su `#` final; nada cierra la app.
2. **Tres desenlaces y sólo se guarda lo válido** (M-03, RF-13, RF-15). El valor registrado sale de la
   trama, nunca de lo que haya en pantalla.
3. **Enlace honesto** (M-14, D4, D11, RF-04). El socket sólo se da por bueno tras `connect()`
   correcto, el estado mostrado es el real, se reintenta y se cierra.
4. **Vaciar antes de pedir, una petición en vuelo, trama literal al registro** (REV-P4.4, RF-12b,
   RF-06, D8). Imprescindible antes de que la app pueda enviar `#S`.
5. **Identidad y registro estable** (M-19 con `#V#`, M-07, RF-08, REV-P4.2). Sin ellos el acta de
   antes y después de P8 no dice de qué equipo ni con qué firmware.

Un punto de partida que ya existe y evita varios de estos defectos: `EnlaceSerie.java` de
`RetroDiagBT` (repositorio V5, `03_App_Movil/RetroDiagBT/`). Da el socket por bueno sólo si
`connect()` termina (`EnlaceSerie.java:156-195`), mantiene un hilo lector y documenta que el firmware
2020 responde `::numero` sin terminador (`:20-22`). Ya se usó contra SLV-002. **Repite en cambio
`versionCode 1`** (`RetroDiagBT/app/build.gradle:15`).

---

## 5. Orden de ejecución, con puertas

Al estilo del `ROADMAP.md` V5, sección DESPUÉS. **Ninguna puerta se salta.**

| Tramo | Qué se hace | No empieza hasta que… |
| :--- | :--- | :--- |
| **0** | Decidir la base (proyecto nuevo o `RetroDiagBT`), fijar `applicationId` propio (M-16), `minSdk` 24, `versionCode` que identifique el binario (M-17, M-18). Comprobar que compila | — |
| **1** | Escribir las pruebas N1 del analizador y de la composición de peticiones (T-01 a T-24 adaptadas) y **verlas fallar** contra la implementación vacía | Tramo 0 compila |
| **2** | Analizador (M-01, M-06 a), capa de enlace (M-14, D4, D11, RF-04), vaciado y una petición en vuelo (REV-P4.4), lista blanca de bytes (D8), trama literal (RF-06). E/S fuera del hilo de interfaz (M-06 b) | **Cada prueba del tramo 1 se ha visto en rojo.** Una prueba que nunca falló no demuestra nada |
| **3** | Simulador del V3 con sus defectos (T-40 a T-48 adaptadas). Ciclo de medida con tres desenlaces (M-03), registro estable y atómico (M-07, RF-08, REV-P4.2, REV-P4.3), identidad (M-19), permisos de localización (RF-05) | Tramo 2 en verde |
| **4** | **Banco:** ver `::<n>` y `:<n>:` en un terminal serie a 9600; medir el ciclo y fijar el tiempo límite (RNF-03 adaptado); repetibilidad (T-73); confirmar qué devuelve `0` (§2). Contra SLV-002 con su firmware 2020 esto **no toca el equipo** | Tramo 3 en verde. Es la puerta que convierte lo anterior en algo medido |
| **5** | Modo administrador y calibración: tabla de patrones cargable (M-09), cotejo (M-10), sesión sólo con `e` (RF-26 adaptado), acta de antes y después (M-11). Tras cada `#S`, releer con `#G` y comparar | Tramo 4 aceptable **y P7 cerrada** (V3.6 grabado). Sin `e` en el equipo, este tramo no tiene sentido |
| **6** | Entrega de resultados (M-12, opción A), hora no alterable (VAL-2.4), `allowBackup` (RF-09), cifrado (S1), reglas del fabricante (M-22), dictamen y catálogo si entran en alcance (VAL-2.3) | Decisión escrita de M-12 y del alcance de la app |
| **7** | Subir `targetSdk` y **después** permisos nuevos (M-17, M-02, RNF-07) | Rama aparte, con la app midiendo bien antes y después. **M-02 no va antes que M-17** |
| **—** | Cambiar el `applicationId` una vez publicado | **No se hace** sin plan de migración de los datos guardados (M-16) |

**Propuesta, no decisión:** los tramos 0 a 3 son lo que P5 necesita para que el "modo de pruebas que
comprueba el equipo antes de dejar calibrar" sea fiable. Si P5 se cierra sin ellos, la app que va a
escribir en la EEPROM arrastra los defectos de la sección 6. Contrasta con `ROADMAP.md` AHORA 4 ("no
bloquean la V3.6"); lo resuelve el propietario.

---

## 6. Lo que la app V3.6 NO debe repetir

Defectos de la app de campo, verificados en el fuente el 18-sep-2026. Ninguno es un reproche a la app
de campo, que funciona y está en uso (`CLAUDE.md` §6 del repositorio V5): es la lista de lo que no se
hereda.

**Conexión**

1. **Socket asignado antes de `connect()` y excepción tragada** (`medicion.java:1661-1664`), guardado
   luego en `btSocket` (`:1672`). Tras un fallo el socket no es nulo, `onItemClick` responde "Ya esta
   conectado" (`:1638-1644`) y no hay reintento sin reiniciar la app.
2. **Una sola instancia de `AsyncTask` reutilizada** (`dispositivos.java:53`, ejecutada en `:112`):
   el segundo intento lanza `IllegalStateException` (REVISION, M-14). Y **"Conectado" se anuncia
   antes de saberlo** (`dispositivos.java:113`).
3. **`isConnected()` falso sin rama `else`** (`medicion.java:1581`): pulsar Medir no hace nada y no
   avisa. El `else` de `:1628` es del `if` de `:1574`, no de éste.
4. **Ante una excepción se navega a otra actividad sin cerrar el socket** (`medicion.java:1616-1621`).
5. **Bluetooth apagado:** `startActivityForResult` sin `onActivityResult`, y `getBondedDevices()` se
   llama antes de la respuesta (`medicion.java:110-114`); `getDefaultAdapter()` no se comprueba contra
   nulo (`:109-110`).

**Lectura**

6. **Espera fija de 3250 ms en el hilo de interfaz y una sola lectura** (`medicion.java:1592-1594`).
   `numBytes` se asigna y no se usa; `new String(mmBuffer)` convierte los 1024 bytes (`:1595`).
7. **Bucle sin cota** (`medicion.java:1606`) sobre un array de 10 (`:1603`).
8. **No se vacía la entrada antes de pedir** (`medicion.java:1591-1594`). En V4 eso deja la app una
   trama por detrás tras el banner (`Aplicacion.c:94-99`); en V3 no hay banner, pero sí respuestas
   tardías de una petición ya vencida.

**Registro**

9. **El valor guardado se lee del `TextView`, no de la trama** (`medicion.java:1562`), y el campo se
   repone a `"0"` tras guardar (`:1565`). Se puede guardar dos veces el mismo valor o un `0` que nadie
   midió.
10. **Fecha con `new Date().toString()`** (`medicion.java:1562`): depende del idioma y del reloj del
    teléfono.
11. **Se registra lo que dice la interfaz, no lo que se envió** (`medicion.java:1548` frente a
    `:1584-1589`).
12. **Cada guardado reescribe y trunca el fichero** (`archivos.java:19-21`), sin juego de caracteres
    (`:19`, `:32`), sin escapar el separador (`crearproyecto.java:50-53`) y sin comprobar si el
    proyecto ya existe (`VerificaArch()`, `archivos.java:59`, nunca se llama).

**Permisos y empaquetado**

13. **Denegar la localización no produce nada, y `grantResults[0]` se lee sin comprobar la longitud**
    (`medicion.java:1709-1716`). Se comprueba `ACCESS_COARSE_LOCATION` (`:1700`) sin declararlo
    (`AndroidManifest.xml:29-32`).
14. **Ruta del adjunto escrita a mano** (`Enviarcorreo.java:101`), **éxito anunciado fuera de la rama
    que envía** (`:118` frente a `:108-116`) y **credencial en el fuente** (`:54-55`; no se transcribe).
15. **`versionCode 1` / `versionName "1.0"` fijos** (`app/build.gradle:13-14`), `allowBackup="true"`
    (`AndroidManifest.xml:7`) y `applicationId` con `com.example`.

**Propios del V3, sin equivalente en la app de campo**

16. **No enviar nunca un byte que no sea una orden**: en firmware 2020 cualquier byte dispara una
    medida (`gui.c:295-297`), y si su primer carácter no es un código conocido, mide sin responder.
17. **No esperar terminador tras `::<n>`**: no lo hay (`ecuacionesCalibracion.c:60-63`).
18. **No leer `::0` como un cero medido** sin más: puede ser un valor fuera de rango puesto a cero
    (`ecuacionesCalibracion.c:49-53`). Pendiente de medir.

---

## 7. Contradicciones entre los documentos de origen

Se dejan marcadas, sin elegir, salvo donde el código ya decide.

| # | Contradicción | Dónde | Estado |
| :--- | :--- | :--- | :--- |
| 1 | M-05 está absorbida por M-20, pero el `ROADMAP.md` V5 la programa ("M-05 aviso rojo+tipo 1", tramo 6), `SPEC-07` §6 la traza a RF-22 y `TDD-01` T-24 pide "se avisa; el registro lleva la marca", cuando RF-22 exige enviar `NAR` | `MEJORAS.md` M-05 · `ROADMAP.md` V5 tramo 6 · `SPEC-07` §6 · `TDD-01` §4.2 | Abierta en origen. No afecta a la V3.6 |
| 2 | Dos órdenes de ejecución distintos: `MEJORAS.md` §6 (tramos 0, A-G; M-07 en E, M-14 en G) y `ROADMAP.md` V5 DESPUÉS (tramos 0-9; M-07 en 6, M-14 en 9). REVISION pedía M-07 en el tramo A y subir M-14 a P0 | `MEJORAS.md` §6 · `ROADMAP.md` V5 · REVISION M-07, M-14 | Manda el `ROADMAP.md` por regla del repositorio V5; aquí se sigue el criterio de REVISION |
| 3 | M-12 y M-18 describen Gmail como el código activo (`Enviarcorreo.java:70,80`) e itvial sólo comentado (`:133-135`). **El fuente actual dice lo contrario**: itvial activo en `:70-73`; Gmail dentro del comentario `/* */` de `:77-87` (y otra copia comentada en `:142-147`); la copia comentada de itvial de `:133-136` sigue ahí. Cambió en el commit `a311d8f` del repositorio V5 | `MEJORAS.md` M-12, M-18 · `Enviarcorreo.java` actual | El código ya decide: los dos documentos están desactualizados |
| 4 | La guarda `if(Retroreflectividad.getText().equals(""))` (`medicion.java:1557`): REVISION dice que funciona hasta el segundo guardado; REBATE dice que no es cierta nunca (un `CharSequence` frente a un `String`) | REVISION M-03 · REBATE §2, RF-15 | Abierta. Las dos coinciden en que no protege; en la V3.6 no se hereda |
| 5 | `SPEC-05` mantiene que la app "no funciona" en Android 12+ (D1 y resumen) y recomienda cambiar el paquete y "descartar y rehacer" (§K), contra `MEJORAS.md` M-02 y M-16 y `CLAUDE.md` §6. `SPEC-05` se escribió para la app del Horizontal (su propósito, línea 5) | `SPEC-05` §B, §J, §K | Ya reconocida en `ROADMAP.md` V5, punto 4 |
| 6 | `SPEC-06` da como ejemplo de respuesta `@LEERV,255.3@`, con decimales; el firmware emite `%d` | `SPEC-06` §D · `Aplicacion.c:312` | El código decide: entero |
| 7 | Recuento del catálogo: 54/76/30 (`SPEC-06` §J, VALIDACION §1), 55/77/30 (REVISION E4) y "declara 160, hay 162, seis inalcanzables" (`ROADMAP.md` V5, punto 7) | Tres documentos | Abierta; sólo importa si la V3.6 reutiliza el catálogo |
| 8 | Tiempo de ciclo: `MEJORAS.md` M-06 da "~1000 + ~2000 ms" citando `SPEC-02` §B; REVISION E2 dice que `SPEC-02` da 2000 ms en total; `SPEC-07` RNF-03 da 3015 ms. Ninguna cifra está medida | `MEJORAS.md` M-06 · REVISION E2 · `SPEC-07` RNF-03 | Abierta en V4. En V3 no hay cifra: se mide (tramo 4) |
