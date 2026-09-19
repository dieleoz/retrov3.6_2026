# RTV V3.6 — app Android del Retrorreflectómetro Vertical V3 / V3.6

**Estado, 18-sep-2026: compila y pasa sus tests JVM. No se ha probado contra ningún equipo.** El modo
V3.6 no se puede probar: el firmware V3.6 no existe todavía en ningún equipo. Lo que sí debe funcionar
es la medida contra un V3 2020 (SLV-002), y eso tampoco se ha comprobado aún con esta app.

- Paquete `com.dpi.retrov36`, etiqueta "RTV V3.6", `versionCode 360`, `versionName 3.6.0`.
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

### Tests JVM

`app/src/test/`: `CalculoTest`, `ReceptorTest`, `AsistenteTest`, `FabricaTest` (45 tests).
`./gradlew testDebugUnitTest` **no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra su
clase `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se compilan
con Gradle y se ejecutan con JUnit a mano:

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
mkdir -p build/libtest   # copiar aquí junit-4.13.2.jar y hamcrest-core-1.3.jar de ~/.gradle/caches
cd app && "$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../build/libtest/junit-4.13.2.jar;../build/libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest
```

`FabricaTest` compara las 12 ecuaciones de la app con el **texto** de
`01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c`: si alguien cambia una
cifra en la app, falla (comprobado alterando una a propósito).

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
| 2 | Versión | Orden de la SPEC (RF-APP-03), con ≥ 1500 ms entre envíos: `#V#` → si `#V,3.6,...#`, V3.6. Si no, `e` → si `::n`, V3 2020 con `e`. Si no, `6` → si `::n`, V3 2020 sin `e` (SLV-002). Si no, `@LEERV,BLA,1@` → si `@LEERV,...@`, V4 (la app no aplica). **Ninguna otra trama con `@`** | V3.6 o V3 2020 |
| 3 | Códigos | Envía `1`-`8`, `a`-`d` y, si el equipo la tiene, `e` | Los 12 responden `::n` en < 2,5 s. En V3.6 `e` es obligatoria |
| 4 | Coherencia | Invierte las 12 respuestas a `x` con las ecuaciones vigentes (fábrica en V3 2020; las leídas con `#G` en V3.6) | Cada código a ±tolerancia (15 por defecto, editable) **más su resolución** de la referencia: la `x` de `e` si la hay, si no la mediana. Así `b` (rojo opaco, ~15 cuentas por unidad de R hacia x ≈ 600) no se castiga. Un 0 es "no evaluable". Hacen falta 6 evaluables |
| 5 | Sólo V3.6 | `#GT#`; `#G,k#` de los 12; marca `CAL`/`DEF` y máscara de `#V#`; `#E,k,x#` en x = 500, 1000, 2000, 3000, 4000 | Todo se analiza. Un código que la máscara da por "fábrica" debe coincidir con la tabla de fábrica a 1 ulp de float32. Cada `#E` debe dar **exactamente** lo que la app calcula en float32, en el orden de 2020 |
| 6 | Repetibilidad | 5 lecturas seguidas con `e` (o `6` invertido) | Ninguna fallida y desviación ≤ 10 cuentas. **Umbral provisional**, sin medir |

La app no puede comprobar el CRC de la EEPROM por sí misma: se fía de la marca y de la máscara que
calcula el firmware, y lo cruza con los coeficientes leídos.

## Línea base previa a grabar (G3)

En la pantalla de Pruebas, botón **"Línea base"**. Pensada para el firmware ORIGINAL (V3 2020 de
SLV-002), antes de grabar la V3.6. Si la versión aún no se ha detectado, la detecta primero.

1. **T-B03:** con el equipo sobre el patrón elegido, los 12 códigos.
2. **T-B09:** la respuesta a `9`, tres veces.
3. **T-B07:** la app pide tapar el cabezal (oscuro) y repite los 12 códigos. Se puede omitir.

Cada petición va al registro de tramas y a `lineabase_<serie>_<fecha>.csv`, con la etiqueta
`LINEA_BASE_PRE_GRABACION`, la respuesta literal, el desenlace, el tiempo y la `x` invertida con la
ecuación de fábrica.

## Lectura de la `x`

- Con `e` (V3.6, o V3 2020 que responda a `e`): `x` directa. Un 0 es saturado o negativo, no se guarda.
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
  comparación a 1 ulp. **C3:** si la relectura no coincide con lo enviado, o si `#S` da `#ERR` o no
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
