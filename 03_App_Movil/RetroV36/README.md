# RTV V3.6 — app Android del Retrorreflectómetro Vertical V3 / V3.6

**Estado, 18-sep-2026: compila y pasa sus tests JVM. No se ha probado contra ningún equipo.** El modo
V3.6 no se puede probar: el firmware V3.6 no existe todavía en ningún equipo. Lo que sí debe funcionar
es la medida contra un V3 2020 (SLV-002), y eso tampoco se ha comprobado aún con esta app.

- Paquete `com.dpi.retrov36`, etiqueta "RTV V3.6", `versionCode 365`, `versionName 3.6.5` (la 3.6.0 enviaba `e` en la detección: no usar).
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

`app/src/test/`: `CalculoTest`, `ReceptorTest`, `AsistenteTest`, `FabricaTest` (68 tests).
`./gradlew testDebugUnitTest` **no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra su
clase `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se compilan
con Gradle y se ejecutan con JUnit a mano:

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
mkdir -p libtest   # copiar aquí (fuera de build/: clean lo borra) junit-4.13.2.jar y hamcrest-core-1.3.jar de ~/.gradle/caches
cd app && "$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest com.dpi.retrov36.CoherenciaRealTest com.dpi.retrov36.CampanaTest
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
