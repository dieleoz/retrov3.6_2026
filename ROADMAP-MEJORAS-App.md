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

---

## 8. Segunda pasada (18-sep-2026)

**Nada de esta sección está medido.** Sale de leer documentos y código. Completa la primera pasada
con los documentos del proyecto V5 que ésta no revisó. **Origen de todas las filas: proyecto V5**
(`D:\IT\P_RetroReflectometro_Vertical`, commit `f79daf6`), que es **otro proyecto** y aquí sólo se
consulta: nada de esta sección propone escribir en él ni versionar la V3.6 allí.

- **Fuentes nuevas (sólo lectura), en `05_Documentacion/` del V5:** `SPEC-08-APK-Cotejo-Equipos.md`
  (SPEC-08), `ENCARGO-Medida-Julio.md` (ENCARGO), `PROCEDIMIENTO-Identificar-Firmware.md` (PIF),
  `TABLA-Patrones-Derivada.md` (TABLA) y `SPEC-01` a `SPEC-04`, más `ROADMAP.md` del V5 (tarea 3 y
  tabla SLV-002) y `03_App_Movil/RetroDiagBT/.../Barrido.java`.
- **Contrato V3.6:** `PROTOCOLO-V3.6.md` revisión 1.1 (§4 bis manda) y `SPEC-V3.6.md`.
- Las citas de firmware sin ruta son de `01_Firmware/base_2020_d089f962/RetroVertical1.X/` de este
  repositorio. Todas se han abierto el 18-sep-2026.

### 8.0 Hecho nuevo: la app ya existe

`03_App_Movil/RetroV36/` contiene la app (`com.dpi.retrov36`, `versionCode 360`, sin permiso
`INTERNET`; `README.md:7-9`). Su `README.md` describe un modo de pruebas de seis pruebas
(`:57-69`), la medida de patrones (`:49-50`) y el asistente (`:99-107`). **Lo que sigue se contrasta
con ese `README.md`, no con el código Java**, que esta pasada no ha revisado. Donde se dice "ya lo
hace", significa "su `README` dice que lo hace".

### 8.1 Copia antigua `D:\@Proyect\IT\P_RetroReflectometro_Vertical\05_Documentacion\`

Comparados nombres y md5 de los dos `05_Documentacion/`. **No hay ningún documento que esté sólo en
la copia antigua.** La antigua tiene 14 de los 20. Los 13 comunes coinciden en md5 salvo
`TABLA-Patrones-Derivada.md`, que en la antigua es una versión anterior (102 líneas frente a 208)
cuyas líneas están todas en la nueva (`diff --strip-trailing-cr`). En la raíz, `MEJORAS.md` e
`INDICE_CRUZADO.md` coinciden en md5. `ROADMAP.md`, `README.md`, `ESTADO.md`, `CLAUDE.md` y
`ARQUITECTURA.map` difieren y no se han revisado aquí, porque no son documentos de mejoras ni SPEC.
Nada que añadir por esta vía.

### 8.2 Mejoras, una por fila

Mismas columnas que §3. Las filas que duplican una ya tratada se remiten a ella y **no se cuentan**.

**SPEC-08 (origen: proyecto V5)**

| ID | Qué es | V3.6 | Por qué | Prioridad | Depende de |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S8-1** (§2.1) | Encender de cero antes de la sesión, por el arrastre de estado | **Adaptada** | Con `e` no hay arrastre de estado en el V3. Pero la `x` lleva un factor de temperatura activo (`gui.c:300`, `X_1 = 0,00043212` en `gui.c:42`) y la app no puede leer la temperatura: ninguna orden de `PROTOCOLO-V3.6.md` §3 la devuelve (`#GT#` da los coeficientes, no T). Se registra la hora de encendido y la sesión se hace en un solo régimen térmico (`PROCEDIMIENTO-Calibracion-V3-K42.md:244`) | Antes de P8 | Ninguna |
| **S8-2** (§2.2) | Descartar las líneas de arranque | **No aplica** | El V3 no emite nada al arrancar (§2) | No | — |
| **S8-3** (§2.5) | Pedir naranja en lugar de rojo | **No aplica** | Es un defecto del V4.1 (M-20) | No | — |
| **S8-4** (§2.7) | Registrar todas las lecturas, no sólo la última | **Sí** | Ya lo hace el registro de tramas (`README.md:89`); el CSV sólo lleva las válidas (`:90`). Que se mantenga así: el estadístico (S8-7) cuenta las excluidas | Antes de P8 | RF-06 |
| — (§2.8) | Si la medida no es válida, no sale número ni veredicto | Se trata en M-03 | — | — | — |
| **S8-5** (§2.9) | Tanda de control sobre el equipo patrón en la misma jornada | **Adaptada** | En P8 no hay un equipo patrón contra el que cotejar. El control equivalente es un **patrón testigo** medido con `e` al principio y al final de la sesión, junto al oscuro que ya pide `SPEC-V3.6.md:289`. Su diferencia mide la deriva térmica de la sesión (S8-1) | Antes de P8 | RF-APP-13 |
| **S8-6** (§3) | Versión de la tabla de referencia en cada medida y en el acta | **Sí** | Un veredicto sin la tabla contra la que se emitió no vale. En V3.6: md5 del `patrones_certificados_P1-P31.csv` cargado, en cada fila y en el acta (RF-APP-21) | Antes de P8 | M-09 |
| — (§3) | Registrar qué se midió inmediatamente antes | Se trata en RF-06 / RF-APP-20 | El registro de tramas ya lo da en orden | — | — |
| **S8-7** (condición 1) | Fijar el estadístico que convierte N lecturas en un valor | **Adaptada** | Ver §8.3 | Antes de P8 | RF-APP-13, P-04 |
| **S8-8** (condición 2) | Cotejar en cuentas, no en unidades de R | **Sí** | En V3.6 ya es así en la práctica: `e` da la `x` y el asistente ajusta sobre `x` (`SPEC-V3.6.md:302`). Falta subirlo a regla: toda tolerancia y toda repetibilidad, en cuentas de `x` | Antes de P8 | Ninguna |
| **S8-9** (condición 3) | Suelo de resolución: paso observable | **Adaptada** | En V3 no son diez cuentas. Ver §8.4 | Antes de P8 | Ninguna |
| **S8-10** (condición 4) | Declarar la relación con SPEC-07 | **Adaptada** | En V3.6 hay que declarar qué certifica cada pantalla: el modo de pruebas dictamina aptitud; el asistente ajusta contra patrones; **ninguno coteja dos equipos**. Si alguna vez se comparan dos V3 (SLV-002 y otro), aplican las reglas de SPEC-08: mismos patrones físicos, misma jornada, en cuentas | Antes de P8 | Ninguna |
| — (condición 5, M6) | `getTypePaper()` puede no devolver nada y decide la rama | Se trata en M-04 corregida | El equivalente V3 es el gatillo con el color de pantalla (§8.3, punto 5) | — | — |
| **S8-11** (§5.2; ENCARGO §4d) | Qué geometría mide el equipo | **Sí** | Tampoco consta para el V3. Sin ella, el acta no puede decir contra qué fila de la norma se dictamina. `SPEC-V3.6.md:334` ya lo recoge en el acta | Después | Dato del fabricante |
| **S8-12** (§5.4; ENCARGO §4b) | N lecturas y desviación aceptable: criterio del propietario | **Sí** | Es P-04 (`SPEC-V3.6.md:575`). Los umbrales de `README.md:125` (10 y 15 cuentas) son provisionales | Antes de P8 | T-B04 medido |
| — (§5.3) | Azul sin datos de calibración | Se trata en RF-APP-14 | En P1-P31 verde, azul y rojo tienen un solo nivel | — | — |
| **S8-13** (§6) | Administrador con dos niveles y credenciales de correo cifradas | **No aplica** | La V3.6 no lleva credenciales (sin `INTERNET`, `README.md:9`) y su único nivel de escritura es el PIN del equipo | No | — |

**ENCARGO, PIF y TABLA (origen: proyecto V5)**

| ID | Qué es | V3.6 | Por qué | Prioridad | Depende de |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ENC-1** (ENCARGO §1) | Cambiar las claves por defecto antes de medir nada | **Adaptada** | El PIN de fábrica `2026` está escrito en `PROTOCOLO-V3.6.md:64`. La app debería exigir `#P` en la primera sesión de administrador si `#L,2026#` funciona | Antes de P8 | RF-APP-19 |
| **ENC-2** (ENCARGO §4f; TABLA §4ter) | Los sumandos K no salen de ninguna regresión | **No aplica** | Las ecuaciones del V3 son polinomios completos (`ecuacionesCalibracion.c:3-42`) y el asistente sustituye la curva entera. El origen de los coeficientes de 2020 tampoco está documentado, pero no hay ningún término suelto que conservar | No | — |
| **ENC-3** (ENCARGO §5; TABLA §4ter) | La señal útil ocupa el 6,8 % del ADC | **Adaptada** | La cifra es de la placa V4 y **no se traslada**. En SLV-002 un mismo patrón dio `x` = 599-634 en los 12 códigos (`ROADMAP.md` V5:38), y el blanco intenso tiene su techo en `x` ≈ 3580 (`SPEC-V3.6.md:73`): el recorrido del V3 puede ser mucho mayor. Hay que medirlo: oscuro y patrón más brillante con `e`. Se propone mostrarlo en el modo de pruebas como dato informativo (recorrido en cuentas y en % de 4096) | Después (dato de P8) | RF-APP-13 |
| **PIF-1** (vía A) | Identificar el firmware por lo que responde | **Adaptada** | Ver §8.5 | Antes de campo (reducida); barrido, después | Pruebas 2 a 4 del `README.md` |
| **PIF-2** (A.4) | El tiempo de respuesta como firma | **Adaptada** | Registrar por código el tiempo entre el envío y el primer byte de `::`. Cuesta poco y distingue variantes. El V3 espera 400 ms con la luz encendida (`measurement.c:225-226`); la duración del resto no está medida | Antes de campo | Registro de tramas |
| **PIF-3** (final) | La primera orden de un firmware nuevo: lectura en crudo con un decimal | **No aplica** | Con `e` el paso ya es de una cuenta de `x` (§8.4). Un decimal no añadiría resolución útil por debajo del ruido térmico | No | — |
| **TAB-1** (§1, §5) | Tabla derivada: `x` esperada por patrón, invirtiendo las ecuaciones del firmware | **Adaptada** | Junto al "como llegó" de RF-APP-16: por cada patrón, la `x` que haría que la ecuación de fábrica diera su valor certificado, y al lado la `x` medida, en cuentas. Es la comparación que no depende de la curva | Antes de P8 | RF-APP-10 |
| **TAB-2** (§4bis) | Residuos en cuentas y no sólo en R; ningún panel medido dos veces | **Adaptada** | Dividir por una pendiente pequeña infla el residuo en R. El asistente muestra residuos en R (`SPEC-V3.6.md:304`): añadir la columna en cuentas de `x`. La falta de repeticiones ya la cubre RF-APP-13 (10 + 3×5) | Antes de P8 | RF-APP-15 |

**Patrones de defecto de `SPEC-01` a `SPEC-04` que pueden repetirse (origen: proyecto V5)**

| ID | Patrón V4.1 | V3.6 | En el V3 | Prioridad | Depende de |
| :--- | :--- | :--- | :--- | :--- | :--- |
| PAT-0 | Estado oculto entre medidas (`error`, `SPEC-02` §G.2) | Se trata en M-04 corregida | Color de pantalla y gatillo (`gui.c:311-315`, `SPEC-V3.6.md:46`). Con `e` no hay otro estado que llegue a la `x`: el filtro se reinicia en cada medida (`measurement.c:241`) | — | — |
| **PAT-1** | Esperas bloqueantes | **Adaptada** | El V3 las tiene: 600 conversiones sin ceder (`measurement.c:242-245`), 200 en la temperatura (`:67-70`) y `__delay_ms(20)` en la STONE (`SPEC-V3.6.md:47`). La app no debe suponer que el equipo atiende mientras mide. El firmware V3.6 no debe añadir ninguna | Antes de campo | RF-APP-01 |
| **PAT-2** | Buffer de recepción sin límite | **Sí** | `bufferData[50]` se llena sin comprobar (`uart_module.c:19,52-56`) y en 2020 no se vacía por tiempo (`:109`). **Regla nueva para la app:** ninguna trama de más de 49 bytes (un `#S` completo) sale hacia un equipo que no haya respondido `#V,3.6,...#`. En un V3 2020 desbordaría el buffer | Antes de campo | RF-APP-03 |
| **PAT-3** | Trama que deja el equipo sin Bluetooth, sin perro guardián (`@` sin `LEERV`, PIF vía A) | **Sí** | La V3.6 conserva `WDTE = OFF` (`PROTOCOLO-V3.6.md:104-105`; `device_config.c:77`). Su descarte a los 2 s de una trama `#` incompleta (`PROTOCOLO-V3.6.md:33`) **necesita una tarea de tiempo que en 2020 está desconectada** (`uart_module.c:109`). Si no se activa, un `#` suelto deja el analizador esperando para siempre. La app no envía nunca un `#` suelto. Se cierra con T-C16/T-C17 | Antes de P7 | Firmware V3.6 |
| **PAT-4** | EEPROM usada como variable temporal (`SPEC-04:42`) | **Sí** | En 2020 **nada escribe la EEPROM**: `writeFunction15Params` y las demás funciones de escritura no tienen ninguna llamada (búsqueda en todo el proyecto). La V3.6 será la primera en escribirla, y sólo con `#S`, `#F`, `#ST` y `#P`. **Prueba en la app:** `#G` de los 12 antes y después de una tanda de medidas, iguales | Antes de P8 | P7 |
| **PAT-5** | Análisis por subcadena (`SPEC-03:135`: un `1` en cualquier posición) | **Sí** | El analizador `#...#` del firmware y el de la app separan por campos y validan cada uno. Nunca `strstr` | Antes de P7 | Firmware V3.6 |
| **PAT-6** | Filtro IIR truncado a entero en cada paso (`SPEC-02` §G.1) | **No aplica** | En el V3 el acumulador es `float` (`measurement.c:20,244`). Es global y lo comparten la temperatura y la reflexión (`:66,241`): la V3.6 debe seguir reiniciándolo en cada rutina | No | — |

### 8.3 Las condiciones de SPEC-08 aplicadas a la V3.6

La tarea 3 del `ROADMAP.md` V5 (`:91`) pide cinco cosas. Traducidas a la V3.6:

1. **Estadístico (S8-7).** Hoy la V3.6 usa tres distintos sin declararlo: la **media** por patrón en el
   asistente (`SPEC-V3.6.md:302`; `README.md:50,102`), la **mediana** como referencia en la prueba de
   coherencia (`README.md:67`) y la **desviación** de 5 lecturas en la de repetibilidad (`:69`).
   Propuesta, a decidir con P-04:
   - **Asistente:** el valor de cada patrón es la media de las lecturas válidas de `x` con `e`, y se
     publican n, s y cuántas se excluyeron. `::0` y los tiempos agotados no entran. **Nunca se
     mezclan** lecturas de `e` con lecturas invertidas de `6`: éstas son intervalos, y el acta lleva
     su semiancho.
   - Si queda fuera más de una lectura de las 10, el patrón no se usa en el ajuste.
   - **Modo de pruebas:** se declara la mediana como referencia de la prueba 4, que resiste un código
     mal invertido. La prueba 6 pasa a 10 lecturas, como pide RF-APP-09.
   - Con 3 recolocaciones de 5, el valor sale de las 15 lecturas, y las 10 seguidas sólo dan el
     error puro de repetibilidad. Esto también es propuesta.
2. **Cotejar en cuentas (S8-8).** Aplica íntegra. Tolerancias, repetibilidad, el "como llegó" (TAB-1)
   y el antes/después de RF-APP-17 se miden en cuentas de `x`. La R sólo aparece como resultado de la
   curva. El acta dice que `x` no es retrorreflexión (M-08).
3. **Suelo de resolución (S8-9).** Con `e` es **una cuenta**, no diez. Con inversión depende del
   código y de `x`. El cálculo está en §8.4. Consecuencia: ninguna tolerancia por debajo de
   `max(suelo, 2·s)`, donde `s` es la repetibilidad medida en T-B04. El umbral de 10 y la tolerancia
   de 15 (`README.md:125`) quedan por encima del suelo de `e`. Con `6` invertido, la app ya suma la
   resolución (`README.md:67`).
4. **Relación con SPEC-07 (S8-10).** Se escribe en `SPEC-V3.6.md` qué certifica cada pantalla
   (tabla §8.2). El acta ya dice "ajuste contra patrones, no calibración trazable"
   (`SPEC-V3.6.md:334`).
5. **M6 en §2.4.** El equivalente V3 es el gatillo. Si está pulsado, la respuesta a `e` es la
   ecuación del color de pantalla aplicada a la `x`, y no la `x`: `gui.c:311-315` aplica esa ecuación
   y `ecuacionesCalibracion.c:191-192` no aplica ninguna encima. La app no lo puede detectar en una
   lectura suelta. La prueba 4 sí lo delata, porque los 12 códigos dejan de invertir a la misma `x`
   (doble ecuación, `SPEC-V3.6.md:46`).

**Qué no pasa a la V3.6:**
- encender de cero por el arrastre (S8-1 lo sustituye por el control térmico);
- el descarte del banner;
- pedir naranja en lugar de rojo;
- los dos niveles de administrador;
- la cifra de "30 unidades de R en blanco", que es de la placa V4 y de su ecuación.

### 8.4 Suelo de resolución del V3

**Por `e` (la `x`):**

| Paso | Qué hace | Cita |
| :--- | :--- | :--- |
| 1 | ADC de 12 bits: el resultado justificado a la izquierda se reconvierte a 12 bits | `adcc.c:108,159` |
| 2 | Referencia externa en VREF+ (`ADREF = 0x02`); MCP1541 de 4,096 V nominal, **no medida en esta placa**: 1 cuenta ≈ 1 mV | `adcc.c:101-102`; `HARDWARE-V3-SATLUX-H-IoT.md:211-212` (V5) |
| 3 | Media de 15 muestras y filtro exponencial en `float` (α = 0,995, 599 pasos) | `measurement.c:228-245` |
| 4 | **Truncado** a entero y suma de 200 | `measurement.c:247` |
| 5 | Producto por el factor de temperatura `k = X_1·T + X_0` (`X_2 = 0`) | `gui.c:41-43,300` |
| 6 | **Truncado** a `unsigned int` | `gui.c:30,300` |
| 7 | `e` envía ese entero con `%u`, sin ecuación; si pasa de 4000, envía 0 | `ecuacionesCalibracion.c:51-52,60,191-192` |

**Resultado: el paso de la `x` es de 1 cuenta** (1/k cuentas de ADC, en torno a 1 mV). **Es diez
veces más fino que el del V4.1**, donde el entero va en unidades de diez cuentas (SPEC-08 §4,
"Segundo"). Los dos truncados sesgan hacia abajo menos de 2 cuentas en total. Como el sesgo va
siempre en el mismo sentido, se cancela al comparar dos lecturas, pero no al dar un valor absoluto.

**Pero el suelo práctico lo pone la temperatura, no el entero.** ∂x/∂T = (ADC + 200)·X_1 ≈
x·4,3·10⁻⁴ por unidad de T. Eso da 0,43 cuentas a x = 1000, 0,86 a x = 2000 y 1,3 a x = 3000. Según
el comentario de `gui.c:503`, T va en décimas de grado; eso **está sin verificar**
(`PROCEDIMIENTO-Calibracion-V3-K42.md:172-175`). Si es así, **un grado mueve la `x` entre 4 y 13
cuentas**. La app no ve T; de ahí S8-1 y S8-5, el control térmico y el patrón testigo.

**Un `::0` de `e`** sólo puede ser saturación (x > 4000). Negativo no puede ser, porque
x ≥ trunc(200·k) > 0 (`measurement.c:247`). Según `PROCEDIMIENTO-Calibracion-V3-K42.md:160-163`,
la saturación empieza hacia ADC ≈ 3760 a 25 °C (sin verificar).

**Por un código `k` e inversión (SLV-002, sin `e`):** la respuesta es un entero de R. Cada valor
recibido corresponde a un intervalo de `x` de ancho 1/f′ₖ(x). Calculado sobre
`ecuacionesCalibracion.c:3-42`, en cuentas de `x` por unidad de R:

| Código | x = 500 | 1000 | 1500 | 2000 | 3000 | Peor caso |
| :--- | ---: | ---: | ---: | ---: | ---: | :--- |
| `6` naranja intenso (el que usa la app) | 10,9 | 5,5 | 3,7 | 2,7 | 1,8 | x < 475: sin inversión (da 0) |
| `1` blanco intenso | 1,9 | 2,2 | 2,8 | 3,6 | 10 | pendiente nula en x ≈ 3580: sin inversión |
| `2` amarillo intenso | 2,8 | 2,1 | 2,1 | 2,6 | — | decreciente desde x ≈ 2780 |
| `3` / `a` verde | 1,7 | 5,3 | 23 | 7,0 | 0,9 | **23,7** en x ≈ 1550 |
| `4` rojo intenso | 1,9 | 5,4 | 14 | 5,8 | 1,0 | 14,6 en x ≈ 1520 |
| `b` rojo opaco | 27 | 6,7 | 3,8 | 2,7 | 1,7 | 16,8 en x ≈ 600 |

Cuadra con lo que dice la app: `b` da unas 15 cuentas por unidad de R hacia x ≈ 600
(`README.md:67`), y con `6` el semiancho es ≤ 3 cuentas entre 1700 y 3000 (`:79`). También es
coherente con las `x` de 599 a 634 que dieron los 12 códigos en SLV-002 (`ROADMAP.md` V5:38): hacia
x ≈ 600, los códigos de pendiente baja tienen intervalos de 10 a 27 cuentas.

### 8.5 PROCEDIMIENTO-Identificar-Firmware: equivalente V3 y cómo entra en el modo de pruebas

La vía A del V4 (saludo, `86`/`7`, defecto del rojo, tiempos) no aplica: el V3 no saluda y no tiene
`error`. **Su equivalente V3 ya existe, en tres piezas:**

1. **Firma de órdenes:** qué bytes responden y con qué formato. Es lo que hizo el barrido de RTV Diag
   BT sobre SLV-002, que reveló la falta de `e` (`ROADMAP.md` V5:37). La prueba 3 de `README.md:66`
   ya es un barrido reducido a la lista blanca (`1`-`8`, `a`-`e`).
2. **Firma de fórmulas:** los 12 códigos invierten a la misma `x`. Es el equivalente exacto de A.2 y
   corresponde a la prueba 4 (`README.md:67`). Así se identificaron las fórmulas de SLV-002. En V3.6
   se añade la comprobación exacta con `#E,k,x#` (`PROTOCOLO-V3.6.md:88`), que es la prueba 5.
3. **Vía B (ICSP):** ya se sabe que el chip de SLV-002 está protegido (`ROADMAP.md` V5:40). Su papel
   lo cumple la línea base G3, tomada antes de grabar (`ROADMAP.md` P7).

**Propuesta de integración:**
- **En el flujo APTO**, sólo la firma reducida (pruebas 2 a 4), más el tiempo de respuesta por código
  (PIF-2), guardada como "firma" en el registro. La app la compara con las firmas conocidas: fuente
  2020 (responde a `e`), SLV-002 (sin `e`) y V3.6 (`#V#`). Si no coincide con ninguna, marca "variante
  desconocida" y no deja escribir.
- **El barrido completo va aparte**, como herramienta técnica con confirmación explícita, **nunca en
  el flujo APTO**. Motivo: contradice la regla D8, que prohíbe enviar un byte que no sea una orden.
  Cada byte enciende la luz y mide (`gui.c:295-297`), y son unas 255 medidas en más de 6 minutos
  (`Barrido.java:34,43`).
- **Condición para barrer un V3.6:** `Barrido.java` sólo excluye `0x40` (`:37`), así que envía `#`
  (`0x23`), y deja 1500 ms entre bytes (`:34`). En V3.6 un `#` abre una trama que se descarta a los
  2 s (`PROTOCOLO-V3.6.md:33`). El byte siguiente, enviado a los 1,5 s, **entra en esa trama y no
  mide**, y la firma saldría falsa. Hay que excluir `0x23` o esperar más de 2 s tras él.

### 8.6 Recuento actualizado

| | Sí | Adaptada | No aplica | Total |
| :--- | ---: | ---: | ---: | ---: |
| Primera pasada (§3.5) | 18 | 17 | 9 | 44 |
| Corrección de M-04 (de "No aplica" a "Adaptada") | 0 | +1 | −1 | 0 |
| SPEC-08 (S8-1 a S8-13) | 5 | 5 | 3 | 13 |
| ENCARGO, PIF y TABLA (ENC-1 a TAB-2) | 0 | 6 | 2 | 8 |
| Patrones de SPEC-01 a SPEC-04 (PAT-1 a PAT-6) | 4 | 1 | 1 | 6 |
| **Total** | **27** | **30** | **14** | **71** |

**Entradas nuevas: 27, de las que 21 aplican (Sí o Adaptada).** Las filas marcadas "Se trata en" no
se cuentan.

### 8.7 Contradicciones nuevas

Se dejan marcadas, sin elegir, salvo donde el código ya decide. La numeración sigue la de §7.

| # | Contradicción | Dónde | Estado |
| :--- | :--- | :--- | :--- |
| 9 | Este documento decía que el buffer del V3 se vacía a los 2 s; la tarea que lo haría está desconectada | §2 de este documento · `uart_module.c:109` · `SPEC-V3.6.md:32` | **El código decide.** Corregido en §2, con la versión errónea al lado |
| 10 | Este documento pedía `#...#` ≤ 48 bytes; la revisión 1.1 fija 96 | §3.4 de este documento · `PROTOCOLO-V3.6.md:80` | **Manda la 1.1.** Corregido en §3.4 |
| 11 | M-04 "no aplica": sí hay estado que llega a la ecuación, con el gatillo pulsado | §3.1 · `gui.c:311-315` · `SPEC-V3.6.md:46` | **El código decide.** Corregido en §3.1 |
| 12 | `PROTOCOLO-V3.6.md` §5 dice "límite de 50 bytes"; su propio §4 bis (O-01) fija un buffer de 100. §5 no está marcado como superado | `PROTOCOLO-V3.6.md:80,101` | Manda §4 bis, por su título. Falta anotarlo en §5 |
| 13 | `SPEC-V3.6.md` no está al día con la revisión 1.1. `#V#` va sin máscara (RF-APP-03, `:222`, frente a O-03). RF-APP-07 y RF-APP-17 comparan con "≤ 1·10⁻⁶" y 7 cifras (`:252,313`), frente a 9 cifras y 1 ulp (O-02). RF-APP-17, C-06 y P-01 (`:315,558,569`) siguen bloqueados por O-01, que la 1.1 resuelve con 96 bytes | `SPEC-V3.6.md` · `PROTOCOLO-V3.6.md:80-83` | Manda la 1.1. La SPEC está desactualizada |
| 14 | "Lo recibido durante la medida se pierde" (RF-APP-01, `SPEC-V3.6.md:210`) y "los bytes sueltos durante una medida se descartan, como en 2020" (O-05). No del todo: `clearBuffer()` va **antes** de la pausa de 500 ms (`gui.c:342` frente a `:345-346`), y la tarea UART sigue corriendo durante esa pausa (`main.c:35`, `uart_module.c:92-93`). Un byte que llegue en ella sobrevive y dispara otra medida | `SPEC-V3.6.md:210` · `PROTOCOLO-V3.6.md:84` · `gui.c:342-346` | **Sin medir.** No tiene efecto si se respeta la espera de 1 s de RF-APP-01. Importa si la V3.6 debe "conservar el comportamiento de 2020" |
| 15 | La app y la SPEC no piden lo mismo. Repetibilidad: 5 lecturas (`README.md:69`) frente a 10 (RF-APP-09, `SPEC-V3.6.md:265`). "Medir ×N": N = 3 (`README.md:49`) frente a 10 + 3×5 (RF-APP-13, `:288`). Detección: `#V#`, `9` y `@LEERV` (`README.md:65`) frente a `#V#`, `e`, `6` y `@LEERV` (RF-APP-03, `:220-225`) | `README.md` de RetroV36 · `SPEC-V3.6.md` | Abierta. En principio la SPEC manda sobre la app, pero puede que la corregida sea la app |
| 16 | "Un 0 de `e` es saturado **o negativo**" (`README.md:76`; RF-APP-05). Con `e` no puede haber negativo: `x ≥ 200·k` | `measurement.c:247` · `ecuacionesCalibracion.c:191-192` | **El código decide:** con `e`, 0 = saturación. Con los códigos `1`-`d`, la frase sigue valiendo |
| 17 | La regla D8 (sólo bytes de orden) frente al barrido de RTV Diag BT, que envía 255 bytes | §3.3 D8 · `Barrido.java:13` | Se resuelve separando el barrido del flujo APTO (§8.5) |
| 18 | SPEC-08 §4 dice que "una cuenta equivale a 3,0 R en blanco" y, en el párrafo siguiente, que el paso observable es de diez cuentas. La tarea 3 del `ROADMAP.md` V5 (`:91`) da la corrección por pendiente | SPEC-08 §4 (V5) | Abierta **en el proyecto V5**. No afecta a la V3.6, cuyo suelo se calcula aparte (§8.4) |
