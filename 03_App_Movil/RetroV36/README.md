# RTV V3.6 — app Android del Retrorreflectómetro Vertical V3 / V3.6

**Estado, 18-sep-2026: compila y pasa sus tests JVM. No se ha probado contra ningún equipo.** El modo
V3.6 no se puede probar: el firmware V3.6 no existe todavía en ningún equipo. Lo que sí debe funcionar
es la medida contra un V3 2020 (SLV-002), y eso tampoco se ha comprobado aún con esta app.

- Paquete `com.dpi.retrov36`, etiqueta "RTV V3.6", `versionCode 3613`, `versionName 3.6.13` (desde la 3.6.10 el versionCode sigue a RF-APP-41: 3.6.10 → 3610, 3.6.13 → 3613) (la 3.6.0 enviaba `e` en la detección: no usar).
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

`app/src/test/`: 13 clases, 146 tests en la 3.6.13, entre ellas `FlujoCalibracionTest` (el flujo "Calibrar este equipo" de
extremo a extremo contra `EquipoSimulado`).
`./gradlew testDebugUnitTest` **no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra su
clase `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se compilan
con Gradle y se ejecutan con JUnit a mano:

```bash
./gradlew compileDebugUnitTestJavaWithJavac --offline
mkdir -p libtest   # copiar aquí (fuera de build/: clean lo borra) junit-4.13.2.jar y hamcrest-core-1.3.jar de ~/.gradle/caches
cd app && "$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest com.dpi.retrov36.CoherenciaRealTest com.dpi.retrov36.CampanaTest com.dpi.retrov36.Version367Test com.dpi.retrov36.Version368Test com.dpi.retrov36.Version369Test com.dpi.retrov36.Version3610Test \
  com.dpi.retrov36.Version3611Test com.dpi.retrov36.Version3612Test com.dpi.retrov36.FlujoCalibracionTest
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
