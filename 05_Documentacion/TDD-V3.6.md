# TDD-V3.6 — Plan de pruebas ejecutable del firmware y la app V3.6

**Estado, 18-sep-2026, 20:20: ninguna prueba se ha ejecutado contra un equipo con V3.6, porque la
V3.6 no está grabada.** Hechas: las 45 pruebas JVM de la app (en verde; reejecutadas en este trabajo a
las 19:59), T-A25 (compilación reproducible), T-A20, T-A24 y T-A31 **declaradas** por el agente de
firmware en simulador, T-A23 hecha y **fallida** (condición C1), y dos pruebas de equipo anteriores a
este plan (T-B11, barrido; T-B12, lectura ICSP). Todo lo demás está pendiente.

Requisitos: [`SPEC-V3.6.md`](SPEC-V3.6.md), revisión 1.1. Contrato: [`PROTOCOLO-V3.6.md`](PROTOCOLO-V3.6.md),
revisión 1.1 (§4 bis). Este documento manda sobre las tablas de §5 de la SPEC.

**Recuento: 85 pruebas.** 37 de nivel A (sin equipo), 12 de nivel B (equipo sin grabar) y 36 de
nivel C (tras grabar).

---

## 0. Cómo se usa

- **El orden del documento es el orden de la campaña** (§2). No se pasa de fase sin cumplir la puerta
  de la anterior.
- Cada ficha dice: requisitos que cubre, nivel, **quién la ejecuta y con qué receta** (§1), estado,
  precondiciones, pasos, resultado esperado y criterio de pasa o falla.
- **Tramas.** Lo que se envía va entre comillas invertidas, en ASCII, **sin CR ni LF** (el V3 no los
  espera, y un CR suelto es un byte que dispara una medida). `::<n>` y `:<n>:` llegan **sin
  terminador**. `<n>` es un entero decimal.
- **Estados:** HECHA-PASA · HECHA-FALLA · DECLARADA (la dio por hecha otro agente; falta evidencia
  verificable) · PARCIAL · PENDIENTE · APLAZADA.
- **Registro de cada prueba:** fecha y hora, equipo y serie, versión detectada, commit y md5 del
  `.hex` grabado, md5 del APK, tramas en crudo (registro `rtv36_<fecha>.txt` de la app o el del
  terminal) y resultado. Se archiva junto al acta de la campaña.
- **Valores esperados calculados.** Los que llevan "(emulación)" salen de emular en `float` de 32 bits
  el orden de 2020 (`c3·x·x·x + c2·x·x + c1·x + c0`, truncado y módulo 2¹⁶), hecho en este trabajo con
  numpy. Coinciden con los dos casos que dio el simulador (`CAMBIOS-V3.6.md` §3.2 y §4.3), pero **son
  cálculo, no medida**: los confirman T-A26 y T-A27.

---

## 1. Recetas

### R-JVM — pruebas JVM de la app (quien: agente o técnico de app)

`./gradlew testDebugUnitTest` no arranca en esta máquina (la carpeta de usuario lleva `ñ`). Receta del
`README.md` de la app, comprobada el 18-sep a las 19:59:

```bash
export JAVA_HOME="D:/@Proyect/Baliza/7 sw apk/jdk-11/jdk-11.0.24+8"
cd D:/IT/P_RetroVertical_V3.6/03_App_Movil/RetroV36
./gradlew compileDebugUnitTestJavaWithJavac --offline
cd app && "$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../build/libtest/junit-4.13.2.jar;../build/libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest
```

Pasa: la última línea es `OK (<N> tests)`. Se ejecuta desde `app/` porque `FabricaTest` lee el fuente
de la base por ruta relativa. Una prueba nueva de este plan se añade a una de esas clases o a una clase
nueva que se añade a la línea de `JUnitCore`.

### R-MDB — firmware en MPLAB SIM (quien: agente o técnico de firmware)

Método de `CAMBIOS-V3.6.md` §3 y de `01_Firmware/RetroVertical_V3.6.X/pruebas/`:

1. **Arnés generado a partir del fuente real**, nunca copiado a mano (como hace
   `pruebas/T-A20_gen_equiv.py`). Para las órdenes `#...#`, el arnés incluye `calibracion_v36.c` y, si
   hace falta, `uart_module.c` tal cual, con dobles de `UART1_Write`/`UART1_Read` (colas en RAM),
   `DATAEE_ReadByte`/`DATAEE_WriteByte` (array de 1024 bytes iniciado a 0xFF) y `getMillis()` (una
   variable que el arnés avanza).
2. Compilar con **XC8 2.10** (`C:\Program Files (x86)\Microchip\xc8\v2.10\bin\xc8-cc.exe`),
   `-mcpu=18F47K42` y los flags de `nbproject/Makefile-default.mk` (en particular `double` de 32 bits).
3. Guion MDB como `pruebas/T-A20_mdb_ejemplo.txt`: `device PIC18F47K42`, `hwtool SIM`,
   `program "<elf>"`, `break <arnés>:<línea final>`, `run`, `wait <ms>`, `print <variable>`…, `quit`.
4. Ejecutar: `"C:\Program Files\Microchip\MPLABX\v5.50\mplab_platform\bin\mdb.bat" guion.txt > salida.txt`.
5. Archivar en `pruebas/`: arnés, generador, guion, salida y un `T-xx.md` con el resultado y el md5 de
   cada fuente incluido (debe coincidir con `hex/fuente.md5` del commit de G1).

### R-IPE — grabación (quien: técnico con el equipo; Diego autoriza, P6)

MPLAB IPE 5.50 (`C:\Program Files\Microchip\MPLABX\v5.50\mplab_ipe\mplab_ipe64.exe`), PICkit 3 en
modo MPLAB (si da "Connection Failed" sin placa, está en modo *scripting*: L-03 de la V4.6), dispositivo
PIC18F47K42, conector `PICKIT3` de 6 pines de la placa H-IoT. Antes de cargar el `.hex`, su md5 debe
ser el declarado en el commit de G1.

### R-APP — operador con la app RTV V3.6 (quien: técnico de campo)

APK compilado **del commit de G1** (md5 anotado). Equipo emparejado en los ajustes del teléfono
(`COVIANDINA_<serie>`). Al terminar cada fase: "Compartir registro y datos" y archivar lo exportado.

### R-TERM — operador con un terminal serie Bluetooth (quien: técnico)

Un terminal que envíe **exactamente** los bytes tecleados, **sin añadir CR ni LF**, y registre lo
recibido con hora de milisegundos (la app RTV Diag BT del repositorio V5, la del barrido del 18-sep, o
equivalente). Con cualquier V3: **nunca** una trama de más de 49 bytes antes de confirmar V3.6
(RF-APP-26), y **nunca** un `@` (RF-FW-25; en un V4.1 un `@` sin `LEERV` bloquea el Bluetooth).

---

## 2. Secuencia de la campaña

| Fase | Pruebas, en este orden | Puerta de salida |
| :--- | :--- | :--- |
| **F0 — App en JVM** | T-A01, T-A02, T-A03, T-A18, T-A10, T-A05, T-A06, T-A13, T-A14, T-A12, T-A11, T-A36, T-A37, T-A15, T-A16, T-A17, T-A07, T-A08, T-A19, T-A32, T-A34, T-A35, T-A04, T-A09 | Todas las no aplazadas en HECHA-PASA |
| **F1 — Firmware en MDB** | T-A25, T-A20, T-A21, T-A26, T-A27, T-A31, T-A24, T-A23, T-A30, T-A22, T-A33, T-A29 | T-A20 (G2) pasa; el resto, pasa o con decisión escrita |
| **F2 — G1** | T-A28 | G1 |
| **F3 — Línea base de SLV-002, firmware original** | T-B11, T-B12 (hechas); T-B01, T-B08, T-B06, T-B09, T-B07, T-B02, T-B04, T-B03, T-B10; T-B05 aplazada | **G3.** Con G1, G2, G3, G5 y P6: **se puede grabar** |
| **F4 — Grabación y verificación** | T-C01, T-C02, T-C03, T-C04 | **G4.** Si falla, no se sigue |
| **F5 — V3.6 sin escribir nada** | T-C06, T-C05, T-C07, T-C08, T-C09, T-C12, T-C13, T-C14, T-C15, T-C16, T-C17, T-C36, T-C18, T-C20, T-C34, T-C33, T-C11; T-C30 si hay app del cliente | Todas pasan o con decisión escrita. **T-C08 decide si vale el "como llegó"** (C-01) |
| **F6 — Escrituras de administración** | T-C21, T-C22, T-C31, T-C29, T-C24, T-C26 (con T-C35), T-C23, T-C19, T-C25; T-C32 sólo con C4 corregida | C1 (T-A23 pasa, o tolerancia decidida) y C4 antes de empezar; C3 y C5 al terminar |
| **F7 — Calibración (P8)** | T-C10, T-C28, T-C27 | Acta de antes y después |

---

## 3. Fichas

### F0 — App en JVM (receta R-JVM)

**T-A01 — Tabla de fábrica de la app (51 valores).** RF-APP-08, RF-FW-05 · A · R-JVM · **PARCIAL**
- Pre: fuente base en `01_Firmware/base_2020_d089f962/RetroVertical1.X/`.
- Pasos: `FabricaTest.lasDoceEcuacionesCoincidenConElFuente` extrae los 12 polinomios del texto de
  `ecuacionesCalibracion.c` y los compara con `Fabrica.ecuacion(k)` con tolerancia 0 en `double`. Falta
  añadir los 3 de temperatura (`gui.c:41-43` base: `0.0`, `0.00043212`, `0.90148651`), que no están en
  `Fabrica`.
- Esperado: 48 + 3 = 51 valores iguales al texto. Hoy: 48 de 48 (en verde).
- Pasa: 51/51. Falla: uno distinto o fuente no encontrado.

**T-A02 — Mapa código ↔ ecuación.** RF-APP-08, RF-APP-04 · A · R-JVM · **HECHA-PASA**
- Pasos: `FabricaTest` (tabla `FUNCION`: `1`→`blancoIntenso`, …, `d`→`naranjaOpaco`, la de
  `ecuacionesColoresApp`, `ecuacionesCalibracion.c:137-193`).
- Esperado y pasa: 12/12, incluidas las tres parejas idénticas (`3`=`a`, `5`=`c`, `6`=`d`).

**T-A03 — Análisis de respuestas sin terminador.** RF-APP-02 · A · R-JVM · **HECHA-PASA**
- Pasos: `ReceptorTest` (`medidaPartidaComoEnElRegistroReal`, `noCortaUnNumeroQueSigueLlegando`,
  `adminSeCierraPorSuAlmohadilla`, `tresDesenlaces`, `acotado`) y `CalculoTest.extraerMedidaEsperaElSilencio`,
  `CalculoTest.tramasAdmin`.
- Esperado: `::42` partido en dos lecturas se cierra 180 ms (`Receptor.SILENCIO_MS`) después del último
  byte y no antes; `::1234` que sigue llegando no se corta; `#V,3.6,2026-09-20,DEF#` se cierra en su `#`;
  `basura#V,3.6,x,DEF#` → `#V,3.6,x,DEF#`; desenlaces VALIDA, TIMEOUT e INESPERADA; entrada acotada a 512
  bytes. Nunca se espera `\r\n`.
- Pasa: todas en verde (lo están).

**T-A18 — Formato numérico y longitud de `#S`.** RF-APP-17, RF-FW-22 (lado app) · A · R-JVM · **HECHA-PASA**
- Pasos: `CalculoTest.numerosEnElFormatoDelContrato`, `CalculoTest.tramaSConNueveCifrasCabeEn96Bytes`.
- Esperado: `#S` de los 12 juegos de fábrica con `%.8E` cabe en 96 bytes y, releída, da el mismo
  `float`; el peor caso (cuatro coeficientes negativos de 9 cifras) ocupa **69 bytes** y empieza por
  `#S,d,-1.23456789E-07,-8.76543211E-05,`.
- Pasa: en verde. *Texto r1.0 del criterio (7 cifras, 48 bytes) retirado.*

**T-A10 — Coherencia de las 12 fórmulas.** RF-APP-07 · A · R-JVM · **HECHA-PASA** (con una ampliación pendiente)
- Pasos: `CalculoTest.coherenciaAptaConLas12DeFabricaHacia615`, `coherenciaUsaLaXDeE`,
  `rojoOpacoTienePocaResolucionYNoSeCastiga`, `coherenciaDetectaUnCodigoDesplazado`,
  `coherenciaSinRespuestaNoEsApta`, `coherenciaConPatronOscuroNoEsEvaluable`, `coherenciaEnVariosPuntos`,
  `igualdadFloat32ConUnUlp`.
- Esperado: las 12 respuestas de fábrica hacia `x` ≈ 615 son APTO; un código desplazado, NO APTO; sin
  respuesta, NO APTO; oscuro, no evaluable; 1 ulp igual, ×1,001 distinto.
- **Ampliación r1.1 (pendiente):** mientras T-A23 falle, un coeficiente de ROM leído a **2 ulp** tiene
  que dar "= fábrica" (RF-APP-07, C-12); y el `#E` esperado con la máscara a "fábrica" se calcula con
  la tabla de fábrica, no con lo leído.
- Pasa: las 8 actuales y las dos ampliaciones en verde.

**T-A05 — Inversión de una respuesta.** RF-APP-10 · A · R-JVM · **PARCIAL**
- Pasos hoy: `inversionDel1ReproduceLosDatosDeSLV002`, `el1NoEsMonotonoY780EsAmbiguo`,
  `inversionDel6RecuperaLaXDeSLV002`, `el6EsMonotonoYSinTechoDe500a4300`,
  `cadenaCompletaPantallaDel1AlCodigo6`, `ceroNoSeInvierte` (en verde). **Pero la inversión usa `double`
  y Horner** (`Inversion.java:52`; C-21).
- Pasos r1.1: para cada `x` entera de 475 a 4400, `n = respuestaFloat32('6', x)`; invertir `n` con la
  misma emulación `float`.
- Esperado: el conjunto devuelto **contiene `x`** en el 100 % de los casos; semianchura ≤ 3 cuentas
  para `x` de 1700 a 3000 (valor de `README.md` de la app).
- Pasa: 3926/3926. Falla: una `x` fuera de su conjunto.

**T-A06 — Forma de las curvas.** RF-APP-11, RF-APP-15 · A · R-JVM · **PARCIAL**
- Hecho: `el1NoEsMonotonoY780EsAmbiguo` (máximo del `1` en `x` ≈ 3580), `el6EsMonotonoYSinTechoDe500a4300`,
  `AsistenteTest.formaDeLaCurvaEnTodoElRango`.
- Falta: informe de los 12 códigos de fábrica (máximo, ceros, tramo creciente y no negativo).
  Esperado: `1` con máximo en `x` ≈ 3580 y `2` en `x` ≈ 2784; `6` creciente de 500 a 4300; las 12
  negativas por debajo de `x` ≈ 475-530.
- Pasa: el informe da esos valores (±5 cuentas) y no cambia el APTO.

**T-A13 — Ajuste por mínimos cuadrados.** RF-APP-15 (C2) · A · R-JVM · **HECHA-PASA**
- Pasos: `CalculoTest.grado2ConLosTresPuntosDeSLV002SeRechaza`, `ajusteGrado2ConLosDatosDeSLV002YUnCuartoPunto`,
  `ajusteGrado1ConResiduosCoherentes`, `ajusteRecuperaLaEcuacionDeFabricaDel1`,
  `ajusteRechazaPuntosInsuficientes`; `AsistenteTest.ajusteDelBlancoConCincoPatrones`.
- Esperado: recupera coeficientes exactos de puntos exactos; residuos y RMS coherentes; grado 2 con 3
  niveles distintos se rechaza (hacen falta grado + 2 = 4).
- Pasa: en verde.

**T-A14 — Validación del juego antes de escribir.** RF-APP-15 (C2) · A · R-JVM · **HECHA-PASA**
- Pasos: `AsistenteTest.formaDeLaCurvaEnTodoElRango`, `curvaDecrecienteBloquea`, `pocosPatronesBloquean`.
- Esperado: el `1` de fábrica se bloquea (techo dentro de 200-4400); `R = 0,2·x − 200` con patrones
  desde `x` = 1700 sólo avisa, y desde 800 bloquea; `R = x` bloquea (pasa de 4000).
- Pasa: en verde. Abierta C-27 (P-09).

**T-A12 — Avisos de cobertura.** RF-APP-14 · A · R-JVM · **HECHA-PASA**
- Pasos: `AsistenteTest.soloBlancoYAmarilloIntensosSonAjustables`, `codigoNoAjustableBloquea`.
- Esperado: sólo `1` y `2` ajustables; `7` bloqueado por "ningún patrón es de tipo I".
- Pasa: en verde.

**T-A11 — Repetibilidad.** RF-APP-09, RF-APP-27 · A · R-JVM · **PARCIAL**
- Hoy: `CalculoTest.repetibilidad` (5 lecturas; `{615, 618, 612, 616, 614}` APTO; `{600, 640, 610,
  630, 590}` NO APTO; una fallida, NO APTO).
- r1.1: `Repetibilidad.LECTURAS` = 10. Serie `{1834, 1835, 1833, 1834, 1834, 1835, 1833, 1834, 1834,
  1834}` → media 1834,0, `s` = 0,667, APTO; serie con `s` = 12 → NO APTO (avería); una fallida → NO APTO.
- Pasa: valores a 0,01 y veredictos correctos.

**T-A36 — Analizador de `#K#`.** RF-APP-24 · A · R-JVM · **HECHA-PASA**
- Pasos: `CalculoTest.registroDeLaPantallaStone`, `AsistenteTest.codigosDePantallaStone`.
- Esperado: `#K,17,A55A06830010010001;A5 5A 06 83 00 10 01 00 07#` → total 17, dos tramas, byte 8 =
  0x01 y 0x07; `#K,0,#` → vacío; `#K,3,ZZ#` → inválida; 0x01 → "blanco intenso".
- Pasa: en verde.

**T-A37 — Límites de `#ST` en la app.** RF-APP-23 (C4) · A · R-JVM · **PARCIAL**
- Hoy: `CalculoTest.tramaSTConLimitesDeX0` (`X_0` = 0,49 y 1,51 rechazados; `NaN` rechazado; fábrica
  aceptada) en verde.
- r1.1: criterio `F(T)` en [0,5 ; 1,5] para `T` de 0 a 831. Casos: `(0, 1.0E-03, 0.9)` → `F(831)` =
  1,731 → rechazar; `(0, -5.0E-04, 0.9)` → `F(831)` = 0,4845 → rechazar; `(1.0E-06, 0, 0.9)` →
  `F(831)` = 1,5906 → rechazar; fábrica `(0, 4.3212E-04, 0.90148651)` → `F(831)` = 1,2606 → aceptar.
- Pasa: los cuatro casos.

**T-A15 — Estado "como llegó".** RF-APP-16 · A · R-JVM · **PENDIENTE**
- Pasos: `Asistente.proponer('1', 1, medidas, Fabrica.ecuacion('1'))` con dos patrones sintéticos en
  `x` = 1739 y 3271.
- Esperado: "R actual" = 512,9 y 798,0 (evaluación en `double` de la ecuación de fábrica del `1`), y
  "desvío actual" = R actual − certificado.
- Pasa: a 0,1.

**T-A16 — Registro técnico.** RF-APP-20 · A · R-JVM · **PARCIAL**
- Hecho: `AsistenteTest.elPinNoLlegaAlRegistro` (`#L,2026#` → `#L,****#`).
- Falta: sesión simulada (detección, 12 códigos, `#G`) → todas las tramas TX y RX con hora, en orden,
  en hex y ASCII, cada una con su desenlace.
- Pasa: el registro reproduce la secuencia sin huecos.

**T-A17 — Acta y CSV.** RF-APP-21, PAR-02, PAR-11 · A · R-JVM · **PARCIAL**
- Hecho: `CalculoTest.medidaEnCsv` (CSV técnico de 14 columnas con la respuesta literal y la `x`).
- Falta: acta de antes y después (RF-APP-21, no existe). CSV de proyecto: **APLAZADO** con PAR.
- Pasa: el acta lleva por código coeficientes antes y después, `x` y `R` por patrón, residuos, versión,
  serie y la frase "ajuste contra patrones, no calibración trazable".

**T-A07 — Ritmo de peticiones.** RF-APP-01 · A · R-JVM · **PENDIENTE**
- Pre: extraer la espera de `Cliente.esperarPausa` a una clase con reloj inyectable (hoy usa
  `SystemClock` de Android y no se puede probar en la JVM).
- Pasos: envío en t = 0; último byte de la respuesta en t = 1900 ms; pedir la siguiente.
- Esperado: no sale antes de t = max(0 + 1500, 1900 + 600) = **2500 ms**. Entre dos tramas `#` con
  V3.6 detectado: 150 ms.
- Pasa: ninguna petición antes de su plazo en 1000 secuencias aleatorias.

**T-A08 — Detección de versión.** RF-APP-03, RF-APP-26 · A · R-JVM · **PARCIAL**
- Hecho: `CalculoTest.soloLaSondaDeV4LlevaArroba` (`@` y `@LEERV,BLA,2@` prohibidos; la sonda
  `@LEERV,BLA,1@` permitida).
- Falta: transporte simulado con los cinco casos:
  1. `#V#` → `#V,3.6,2026-09-18,DEF,0000#` ⇒ V3.6;
  2. `#V#` → nada; `e` → `::1834` ⇒ V3 2020 con `e`;
  3. `#V#` → nada; `e` → nada; `6` → `::344` ⇒ V3 2020 sin `e`;
  4. tres silencios; `@LEERV,BLA,1@` → `@LEERV,512@/n/r` ⇒ V4, no se opera;
  5. cuatro silencios ⇒ desconocido.
- Esperado además: la sonda con `@` sólo en el caso 4 y en el paso 4; ninguna petición de más de 49
  bytes en los casos 2-5.
- Pasa: 5/5.

**T-A19 — PIN.** RF-APP-19 · A · R-JVM · **PENDIENTE**
- Pasos: validar las entradas `123`, `1234`, `12345`, `12a4`, `    `.
- Esperado: sólo `1234` se acepta; ninguna llega al registro en claro.
- Pasa: 5/5.

**T-A32 — Restauración automática tras `#S` (C3).** RF-APP-17 · A · R-JVM · **PENDIENTE**
- Pre: extraer de `AdminActivity.escribir`/`restaurar` (`AdminActivity.java:442-495`) la decisión a una
  clase pura con el enlace simulado.
- Pasos y esperado:
  1. `#S` → `#OK#`, `#G` = lo enviado ⇒ no restaura;
  2. `#OK#`, `#G` distinto, anterior = fábrica ⇒ envía `#F,1#` y relee;
  3. `#OK#`, `#G` distinto, anterior ≠ fábrica ⇒ envía `#S,1,<anteriores>#` y relee;
  4. `#ERR,EEPROM#` y `#G` = anterior ⇒ no restaura;
  5. sin respuesta y `#G` ≠ anterior ⇒ restaura.
- Pasa: 5/5, y nunca `#S` con los valores de fábrica.

**T-A34 — Longitud de petición según la versión.** RF-APP-26 · A · R-JVM · **PENDIENTE**
- Pasos: con la versión DESCONOCIDA o V3 2020, pedir una trama de 50 bytes; con V3.6, una de 96 y
  otra de 97.
- Esperado: la de 50 se rechaza **antes de enviarse** en las dos primeras versiones; con V3.6, 96 sale
  y 97 se rechaza.
- Pasa: 4/4.

**T-A35 — Estadístico y tolerancias.** RF-APP-27 · A · R-JVM · **PENDIENTE**
- Pasos: medidas de P1 con `e` = {1834, 1835, 1833} y con `6` invertido = {1840, 1842}.
- Esperado: el punto del ajuste usa sólo las de `e`: media 1834,0, n = 3, `s` = 1,0; tolerancia
  asociada = max(1, 2·1,0) = 2 cuentas. Las de `6` se publican aparte.
- Pasa: valores exactos.

**T-A04 — Emulación `float` frente al firmware (lado app).** RF-APP-10 · A · R-JVM · **PENDIENTE** (depende de T-A26)
- Pasos: con la tabla de CRC por bloques que produce T-A26, la app calcula, para cada código y cada
  bloque de 256 `x`, el CRC-16/CCITT-FALSE de las 256 salidas (`uint16`, byte bajo primero) de su
  emulación (`respuestaFloat32` antes del corte, con el modelo de negativos de T-A21).
- Esperado: bloques 0 a 17 (`x` de 0 a 4607, que cubre el rango de uso) **iguales** en los 12
  códigos. Fuera de ese rango, las diferencias se anotan.
- Pasa: 216/216.

**T-A09 — Mapa color × tipo → código.** RF-APP-04 · A · R-JVM · **APLAZADA** (fase de campo)
- Cubierto en parte por `CalculoTest.csvDePatronesEmbebido` (color de los 31 patrones → código
  intenso).
- Pasa (cuando se retome): 6 colores × 11 tipos; tipo I → `7`, `8`, `a`-`d`; II-XI → `1`-`6`.

### F1 — Firmware en MDB (receta R-MDB)

**T-A25 — Compilación reproducible de la base.** RF-FW-27 · A · R-MDB (compilación) · **HECHA-PASA**
- Evidencia: `CAMBIOS-V3.6.md` §1: la base compilada con XC8 2.10 da un `.hex` con md5
  `d089f962…` (0 direcciones distintas). P3 cerrada.

**T-A20 — No regresión de las ecuaciones (G2).** RF-FW-04, RF-FW-05, RF-FW-06 · A · R-MDB · **DECLARADA**
- Evidencia: `CAMBIOS-V3.6.md` §3.1 ("0 diferencias"); `pruebas/T-A20_harness_eq.c`,
  `T-A20_gen_equiv.py`, `T-A20_mdb_ejemplo.txt` (commit `319345f`). La tabla y `aplicarEcuacion` del
  arnés coinciden con `calibracion_v36.c:33-46,228-233` actuales (comparado a mano el 18-sep).
  **Falta el informe** `pruebas/T-A20.md` que `CAMBIOS` cita (C-19).
- Pre: el `calibracion_v36.c` del arnés tiene el md5 que figura en `hex/fuente.md5` del commit de G1.
- Pasos: `python T-A20_gen_equiv.py`; compilar el arnés 12 veces con `-DK_DESDE=k -DK_HASTA=k`
  (k = 0-11) o las veces que quepa en el tiempo de simulación; ejecutar el guion MDB de cada una.
- Esperado por código: `nComparados` = 65536, `nDistintos` = 0, `primerK` = 255, `primerX` = 65535.
- Pasa: los 12 códigos así, con el informe archivado. Falla: cualquier `nDistintos` ≠ 0 ⇒ **no se graba**.

**T-A21 — Negativos en 2020.** RF-FW-06 · A · R-MDB · **PARCIAL**
- Hecho: un caso (`2` con `x` = 4300 → 65360 antes del corte; `CAMBIOS` §4.3).
- Pasos: en el arnés de T-A20, imprimir la salida de las funciones de 2020, **antes** de
  `arreglar_dato()`, para `x` = 180 y 200 en los códigos `1`, `2`, `3`, `4`, `5`, `7`.
- Esperado (emulación, truncado y módulo 2¹⁶): `x` = 200 → `1`: 65354, `2`: 65442, `3`: 65308,
  `4`: 65334, `5`: 65507, `7`: 65516; `x` = 180 → `1`: 65342, `2`: 65438, `3`: 65289, `4`: 65318,
  `5`: 65505, `7`: 65515. Todos > 4000, así que tras el corte, 0.
- Pasa: 12/12 ⇒ el modelo de la app queda confirmado. Falla ⇒ se anota el valor real y RF-APP-10 se
  ajusta a él.

**T-A26 — Redondeo de XC8 frente a la JVM (lado firmware).** RF-APP-10 (R-05) · A · R-MDB · **PENDIENTE**
- Pasos: variante del arnés de T-A20 que, por código y por bloque de 256 `x`, calcula el
  CRC-16/CCITT-FALSE de las 256 salidas de la función de 2020 (antes del corte) y las guarda en un
  array de 12 × 256 `uint16` que se imprime con MDB.
- Esperado: la tabla que consume T-A04.
- Pasa: se genera y se archiva; el veredicto lo da T-A04.

**T-A27 — `#E` en simulador.** RF-FW-28 · A · R-MDB · **PARCIAL**
- Hecho (`CAMBIOS` §3.2): `#E,1,1000#` → `#E,1,230#`; `#E,2,4300#` → `#E,2,0#`.
- Pasos: con el fuente real y dobles de UART, para los 12 códigos y `x` ∈ {0, 200, 500, 1000, 2000,
  3000, 4000, 4300, 65535}, enviar `#E,<k>,<x>#` y comparar con la función de 2020 + `arreglar_dato()`
  en el mismo arnés. Además `#E,1,65536#`, `#E,1,-1#`, `#E,z,100#`, `#E,1,1e3#`, `#E,1#`.
- Esperado (emulación) para `x` = 500 / 1000 / 2000 / 3000 / 4000:

  | Código | 500 | 1000 | 2000 | 3000 | 4000 |
  | :---: | ---: | ---: | ---: | ---: | ---: |
  | `1` | 0 | 230 | 590 | 777 | 791 |
  | `2` | 0 | 203 | 664 | 813 | 214 |
  | `3`, `a` | 0 | 177 | 261 | 788 | 2735 |
  | `4` | 0 | 167 | 273 | 805 | 2646 |
  | `5`, `c` | 0 | 60 | 289 | 793 | 1729 |
  | `6`, `d` | 2 | 70 | 344 | 798 | 1434 |
  | `7` | 1 | 74 | 349 | 800 | 1425 |
  | `8` | 0 | 72 | 224 | 809 | 2344 |
  | `b` | 0 | 46 | 310 | 799 | 1515 |

  Las cinco tramas inválidas → `#ERR,FORMATO#`. `reflectivityValue` no cambia.
- Pasa: 108/108 iguales a la función de 2020 y 5/5 rechazos.

**T-A31 — Órdenes de administración en simulador.** RF-FW-16, 17, 18, 19, 20, 21 · A · R-MDB · **DECLARADA**
- Evidencia: `CAMBIOS` §3.2 (sin salida archivada).
- Pasos y esperado, en este orden, EEPROM a 0xFF:
  1. `#V#` → `#V,3.6,<fecha de compilación>,DEF,0000#`;
  2. `#F,1#` → `#ERR,BLOQUEADO#`;
  3. `#L,0000#` → `#ERR,PIN#`; `#L,2026#` → `#OK#`;
  4. `#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#` (67 bytes) → `#OK#`;
  5. `#V#` → `…,CAL,0001#`; `#E,1,2000#` → `#E,1,591#` (con fábrica, 590);
  6. reinicio simulado (llamar a `calibracionIniciar()`) → `#G,1#` igual que en 4; `#V#` → `CAL,0001`;
  7. `#F,1#` (sesión cerrada por el reinicio) → `#ERR,BLOQUEADO#`; `#L,2026#` → `#OK#`; `#F,1#` →
     `#OK#`; `#V#` → `DEF,0000`;
  8. `#Q#` → `#OK#`; cinco `#L,1111#` → cinco `#ERR,PIN#`; `#L,2026#` → `#ERR,BLOQUEADO#`;
  9. reinicio; `#L,2026#` → `#OK#`; avanzar `getMillis()` 600 001 ms sin tramas `#`; `#F,1#` →
     `#ERR,BLOQUEADO#`.
- Pasa: 9/9 con la salida archivada.

**T-A24 — EEPROM (C1).** RF-FW-24 · A · R-MDB · **DECLARADA** (pasa según `CAMBIOS` §3.2)
- Pasos: (a) cabecera borrada → `#V#` `DEF,0000` y PIN `2026`; (b) un byte de datos del registro del
  código `3` alterado → sólo `3` de fábrica, el resto como estaba; (c) bloque válido con el `1` escrito
  → `CAL,0001`; (d) CRC de `"123456789"` → 0x29B1; CRC de los 13 registros recalculados fuera.
- Pasa: 4/4 con la salida archivada.

**T-A23 — Conversión de números con 9 cifras (C1).** RF-FW-22 · A · R-MDB · **HECHA-FALLA**
- Pasos: 120 valores (los 35 literales distintos de fábrica y 85 aleatorios de 1e-9 a 1e5 de los dos
  signos): (a) `sprintf("%.8E")` de XC8 frente al texto de 9 cifras correctamente redondeado calculado
  fuera; (b) ese texto → `strtod` de XC8 frente al `float` exacto. Además, `abc`, `nan`, `inf`, `1e5x` y
  cadena vacía → rechazo.
- Esperado: **120/120 exactos en las dos direcciones**; los 5 rechazos.
- Resultado 18-sep (`CAMBIOS` §3.3): `sprintf` 60 exactos, 41 a 1 ulp, 19 a 2 ulp; `strtod` 68 exactos,
  49 a 1 ulp, 2 a 2 ulp, 1 a 3 ulp. Rechazos correctos.
- Falla ⇒ C1 no cumplida. Se repite tras la conversión exacta propia (RF-FW-22 r1.1, P-10).

**T-A30 — Límites de `#ST` (C4).** RF-FW-19 · A · R-MDB · **PARCIAL**
- Hecho: rechazo de `X_0` fuera de [0,5 ; 1,5] (`CAMBIOS` §3.2).
- Pasos, con sesión abierta: `#ST,0.00000000E+00,4.32119996E-04,9.01486516E-01#` → `#OK#`;
  `#ST,0,4.3E-04,4.9E-01#` → `#ERR,FORMATO#`; `#ST,0,1.0E-03,9.0E-01#` → `#ERR,FORMATO#`;
  `#ST,0,-5.0E-04,9.0E-01#` → `#ERR,FORMATO#`; `#ST,1.0E-06,0,9.0E-01#` → `#ERR,FORMATO#`.
- Hoy los tres últimos darían `#OK#` (`calibracion_v36.c:536-548` sólo mira `X_0`).
- Pasa: 5/5 ⇒ C4 cumplida en firmware.

**T-A22 — Desbordamiento de recepción.** RF-FW-14 · A · R-MDB · **PENDIENTE**
- Pasos: con `uart_module.c` real y cola UART simulada: 200 bytes `z` seguidos; tramas que empiezan
  por `#` de 49, 60, 96 (con `#` final) y 97 y 255 bytes (sin `#` hasta el final); después `#V#`.
- Esperado: `bufferIndex` ≤ 49; `adminIndex` ≤ 96; la trama de 96 se procesa (`#ERR,FORMATO#`); las de
  97 y 255, descartadas sin respuesta; `#V#` responde. Las variables vecinas en el `.map` no cambian.
- Pasa: todo lo anterior.

**T-A33 — `#K#` en simulador.** RF-FW-29 · A · R-MDB · **PENDIENTE**
- Pasos: cola UART2 simulada; una trama `A5 5A 06 83 00 10 01 00 01`; `#K#`; ocho tramas más; `#K#`;
  una trama de 20 bytes; `#K#`; `#KC#`; `#K#`.
- Esperado: `#K,1,A55A06830010010001#`; después `#K,9,<8 tramas>#` sin la primera; la de 20 bytes
  sale con sus 16 primeros (32 cifras hex); `#KC#` → `#OK#`; `#K,10,#`.
- Pasa: 4/4.

**T-A29 — Memoria de la V3.6.** RF-FW-30 · A · revisión del `.map` · **PENDIENTE**
- Pasos: en el `.map` y el `.lst` del `.hex` de G1, localizar `acquireReflectivity15Adc@tempSamples`
  (32 bytes), `measure@nivel` (6), `bufferData` (50), `tramaAdmin` (100), `bufferPantalla` (50) y
  comprobar que no se solapan; profundidad de pila estimada (`hex/memoria.txt`: 16) frente a los 31
  niveles de la pila hardware.
- Pasa: sin solapes y pila ≤ 31.

### F2 — G1

**T-A28 — Fuente y `.hex` atados a un commit (G1).** RF-FW-27 · A · operador con git · **PARCIAL**
- Pasos:
  ```bash
  cd D:/IT/P_RetroVertical_V3.6
  git status --porcelain 01_Firmware/RetroVertical_V3.6.X          # vacío
  C=$(git log -1 --format=%H -- 01_Firmware/RetroVertical_V3.6.X)
  cd 01_Firmware/RetroVertical_V3.6.X
  md5sum -c hex/fuente.md5 && md5sum -c hex/RetroVertical_V3.6.hex.md5
  git cat-file blob $C:01_Firmware/RetroVertical_V3.6.X/hex/RetroVertical_V3.6.hex | md5sum
  git check-attr text -- hex/RetroVertical_V3.6.hex                 # text: unset
  grep -n "${C:0:7}" CAMBIOS-V3.6.md
  ```
- Esperado: árbol limpio; los dos `md5sum -c` sin fallos; **md5 del blob = md5 declarado**;
  `.gitattributes` con `*.hex -text`; el hash citado en `CAMBIOS`.
- Estado 20:05: commit `319345f`; `md5sum -c` sin fallos a las 19:58; md5 del blob `32d636f8…` ≠
  declarado `680b6a7d…` (C-32); sin `.gitattributes`; hash no citado. **Falla hoy.**
- Pasa: los cinco puntos.

### F3 — Línea base de SLV-002 con su firmware original (G3)

Precondiciones de la fase: SLV-002 **sin grabar**, batería cargada, patrones P1-P31 limpios, sin
tocar el gatillo, temperatura ambiente anotada al principio y al final. En este firmware `e` no
existe: la `x` se obtiene con `6` invertido.

**T-B11 — Barrido de 255 bytes.** RF-APP-03, RF-FW-07 (R-02) · B · R-TERM (RTV Diag BT) · **HECHA-PASA** (18-sep)
- Resultado (`HISTORIA.md`): `1`-`8` y `a`-`d` → `::n`; `9` → `:n:`; `e` sin respuesta. Las 12
  respuestas invierten a `x` de 599 a 634 (misma `x`: fórmulas de 2020).

**T-B12 — Lectura ICSP.** RF-FW-26 · B · R-IPE · **HECHA-PASA** (18-sep)
- Resultado: chip protegido (`CP = ON`), sin copia; configuración `EC FF F7 FF 9F FF FF DF FE FF`.
  Archivo: `01_Firmware/lecturas_equipos/SLV-002/SLV-002_lectura_2026-09-18.hex`.

**T-B01 — Detección.** RF-APP-03, RF-APP-26 · B · R-APP · **PENDIENTE**
- Pasos: equipo sobre P1; Pruebas → Iniciar.
- Esperado en el registro: TX `#V#` → sin respuesta (el equipo enciende la luz y pita); TX `e` → sin
  respuesta; TX `6` → `::<n>` con n > 0 ⇒ "V3 2020 sin `e`". Después, la prueba 3 envía `1` y recibe
  `::<n>`.
- Pasa: clasificación correcta, ninguna TX con `@`, ninguna TX de más de 49 bytes.

**T-B08 — `#V#` es inocuo en 2020.** RF-FW-09, RF-APP-03 · B · R-TERM · **PENDIENTE**
- Pasos: `#V#`; esperar 3 s; `6`.
- Esperado: a `#V#`, luz y pitido y **0 bytes** recibidos en 3 s; a `6`, `::<n>` en < 2,5 s.
- Pasa: las dos cosas.

**T-B06 — Tiempos.** RF-APP-01, RF-APP-02; §1 r1.1 · B · R-TERM · **PENDIENTE**
- Pasos: (a) 50 × `6` sobre P1 con pausa de 1500 ms desde el envío y 600 ms desde el último byte;
  anotar envío → primer byte, envío → último byte, hueco máximo entre bytes de una respuesta.
  (b) `6`; en cuanto llegue `::<n>`, esperar **30 ms** y enviar `6`. (c) `6`; al llegar `::<n>`,
  esperar **300 ms** y enviar `6`.
- Esperado: (a) 50/50 respuestas; (b) **una** respuesta (el segundo `6` cae antes de `clearBuffer()`
  y se pierde); (c) **dos** respuestas (el segundo `6` cae en la pausa de 500 ms y mide).
- Pasa: (a) sin pérdidas, y se fijan plazo = 1,5 × máximo y silencio ≥ 3 × hueco máximo (mínimo 50 ms);
  (b) y (c) como se esperan. Si (b) o (c) no, se corrige §1 y RF-APP-01.

**T-B09 — Batería (G3).** RF-FW-08, RF-APP-25 · B · R-TERM o R-APP (Línea base) · **PENDIENTE**
- Pasos: `9` diez veces, con la pausa de RF-APP-01.
- Esperado: diez respuestas `:<n>:` con `<n>` entero; se anotan los literales.
- Pasa: 10/10 con ese formato. (La línea base de la app hace hoy 3 repeticiones, C-18: poner 10.)

**T-B07 — Negativos y saturación (G3).** RF-FW-06 · B · R-TERM o R-APP (Línea base) · **PENDIENTE**
- Pasos: óptica tapada con una tapa opaca: los 12 códigos. Sobre el patrón más alto (P4, 828,
  blanco XI, "en el techo"): `1` y `2`, cinco veces cada uno.
- Esperado (emulación, **referencia, no criterio**): en oscuro `x` ≈ 180-200 y los 12 dan `::0`; sobre
  P4, con `x` ≈ 3271 (dato del 18-sep), `1` ≈ `::797` y `2` ≈ `::743`.
- Pasa: 12 + 10 respuestas registradas. Si alguna en oscuro no es `::0`, el modelo de negativos no es
  el de T-A21 y se revisa RF-APP-10.

**T-B02 — Coherencia por inversión.** RF-APP-07, RF-APP-25 · B · R-APP (Pruebas y Línea base) · **PENDIENTE**
- Pasos: sobre P1, los 12 códigos (la línea base de la app lo etiqueta hoy "T-B03", C-18).
- Esperado: los 12 intervalos de `x` se solapan dentro de max(suelo, 2·s) (como el 18-sep: 599-634).
- Pasa: prueba 4 de la app en verde (≥ 6 evaluables).

**T-B04 — Repetibilidad con Bluetooth.** RF-APP-09, RF-APP-27 · B · R-APP · **PARCIAL**
- Hecho: 8 blancos por pantalla (`x` de 1739 a 3271, ±1).
- Pasos: 10 × `6` sin mover el equipo sobre P2 (414), P1 (762) y P4 (828).
- Esperado: por patrón, media de `x`, `s` y CV; se espera `s` ≤ 3 (semianchura de la inversión).
- Pasa: 30/30 lecturas válidas. Con esto se fija el umbral metrológico (P-04).

**T-B03 — Patrones antes de grabar: "como llegó" (G3).** RF-APP-13, RF-APP-27 · B · R-APP (Medida de patrones) · **PENDIENTE**
- Pasos: oscuro al principio (anotar); por cada P1-P31: N = 10 sin mover; después 3 recolocaciones
  con N = 5; oscuro al final; hora y temperatura ambiente a mano.
- Esperado: `medidas_<serie>_<fecha>.csv` con cada lectura (respuesta literal, código `6`, `x` y
  semianchura, método "6 invertido"); por patrón, media, `s`, `n`.
- Pasa: 31 patrones con ≥ 10 lecturas válidas (una lectura `::0` no es válida y se anota como tal).

**T-B10 — Pantalla STONE, línea base (G3).** RF-FW-10 · B · operador con cámara · **PARCIAL**
- Hecho: 8 blancos por pantalla.
- Pasos: fotografiar cada pantalla; sobre P1, tres gatillazos con cada color de OTROS PAPELES (6) y
  de PAPEL TIPO I (6); anotar las cuatro variables (199, 205, 210, 215); buscar la página "PRUEBA ADC"
  (CA6).
- Pasa: 12 × 3 valores y las fotos archivadas.

**T-B05 — Flujo de campo.** PAR · B · R-APP · **APLAZADA** (fase de campo; SPEC §4 r1.1).

**Puerta de grabación.** G1 (T-A28), G2 (T-A20 con informe), G3 (T-B03, T-B07, T-B09, T-B10), G5
(documentos) y P6 (cerrada el 18-sep). Recomendado además: decidir P-10 (conversión exacta) **antes** de
G1, para no grabar dos veces.

### F4 — Grabación y verificación (G4)

**T-C01 — Grabación.** RF-FW-26, RF-FW-27 · C · R-IPE · **PENDIENTE**
- Pasos: comprobar el md5 del `.hex` (el de G1); IPE: *Program*; leer la configuración.
- Esperado: "Programming complete" con la verificación que IPE hace al programar; configuración
  `EC FF F7 FF 9F FF FF DF FE FF`.
- **Aviso sin verificar:** con `CP = ON`, un *Verify* separado después de programar lee la memoria
  protegida (como en T-B12) y puede dar fallos que no lo son. Vale la verificación durante *Program*;
  confirmarlo en la primera grabación y anotarlo.
- Pasa: programación verificada y configuración igual. Falla: no se sigue.

**T-C02 — Arranque.** RF-FW-01, RF-FW-10 · C · operador · **PENDIENTE**
- Pasos: encender; terminal abierto 5 s.
- Esperado: pantalla igual que en T-B10; **0 bytes** por Bluetooth sin petición.
- Pasa: las dos.

**T-C03 — Versión (G4).** RF-FW-16, RF-APP-03 · C · R-TERM y R-APP · **PENDIENTE**
- Pasos: `#V#`. Después, Pruebas de la app (prueba 2).
- Esperado: `#V,3.6,2026-09-18,DEF,0000#` en < 2 s (la fecha es la de compilación del `.hex` de G1),
  sin luz ni pitido; la app: "V3.6, marca DEF, ningún código ajustado; temperatura de fábrica".
- Pasa: literal exacto.

**T-C04 — Coeficientes de fábrica (G4).** RF-FW-18, RF-FW-22, RF-FW-05 · C · R-TERM y R-APP · **PENDIENTE**
- Pasos: `#G,1#` … `#G,d#` (12) y `#GT#`.
- Esperado con conversión exacta (T-A23 pasa), literal:
  ```
  #G,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.03000000E+02#
  #G,2,-7.30000025E-08,2.82508001E-04,1.24075353E-01,-1.30000000E+02#
  #G,3,1.62999996E-07,-7.56694993E-04,1.21314275E+00,-4.42000000E+02#
  #G,4,1.46999994E-07,-6.68624009E-04,1.08239400E+00,-3.93000000E+02#
  #G,5,2.60000004E-08,-1.84019991E-05,1.02152668E-01,-4.90000000E+01#
  #G,6,0.00000000E+00,9.07310023E-05,1.06432894E-03,-2.10000000E+01#
  #G,7,0.00000000E+00,8.74039979E-05,1.36176823E-02,-2.70000000E+01#
  #G,8,8.60000000E-08,-2.99025996E-04,4.46572334E-01,-1.61000000E+02#
  #G,a,1.62999996E-07,-7.56694993E-04,1.21314275E+00,-4.42000000E+02#
  #G,b,0.00000000E+00,1.13098002E-04,-7.61304200E-02,1.00000000E+01#
  #G,c,2.60000004E-08,-1.84019991E-05,1.02152668E-01,-4.90000000E+01#
  #G,d,0.00000000E+00,9.07310023E-05,1.06432894E-03,-2.10000000E+01#
  #GT,0.00000000E+00,4.32119996E-04,9.01486516E-01#
  ```
  (texto de 9 cifras correctamente redondeado del `float` de cada literal de fábrica; calculado en
  este trabajo).
- Con la biblioteca de XC8 (hoy): cada número, leído como `float`, a **≤ 2 ulp** del de la lista.
- Pasa: 13/13 con el criterio que corresponda; la prueba 5 de la app en verde (requiere RF-APP-07
  r1.1, C-12). Falla ⇒ G4 no cumplida.

### F5 — V3.6 sin escribir nada

**T-C06 — `e`.** RF-FW-07, RF-APP-06 · C · R-TERM · **PENDIENTE**
- Pasos: sobre P1, 10 × `e`; después 10 × `6`.
- Esperado: `::<x>` con 200 < x ≤ 4000; la media de `x` cae dentro del intervalo de inversión de las
  respuestas a `6` ± max(1, 2·s).
- Pasa: 10/10 y concordancia.

**T-C05 — No regresión por Bluetooth (C5).** RF-FW-03, RF-FW-04, RF-FW-28 · C · R-TERM · **PENDIENTE**
- Pre: `DEF,0000`; gatillo sin tocar.
- Pasos: sobre P2, P1 y P4; para cada código `k` de los 12: `e` → `::<x>`; `#E,<k>,<x>#` →
  `#E,<k>,<R_E>#`; `<k>` → `::<R_k>`; `e` → `::<x'>`.
- Esperado: (a) `R_E` = emulación `float` de la ecuación de fábrica de `k` en `x`, **exacto**;
  (b) `R_k` = `f_k(x*)` para algún `x*` entre min(x, x') − max(1, 2·s) y max(x, x') + max(1, 2·s).
- Pasa: (a) 36/36 y (b) 36/36.

**T-C07 — Negativos y saturación.** RF-FW-06 · C · R-TERM · **PENDIENTE**
- Pasos: repetir T-B07.
- Esperado: en oscuro, las mismas respuestas que T-B07 (`::0`); sobre P4, dentro de la repetibilidad
  de T-B04 propagada por la pendiente de `f_1` y `f_2`.
- Pasa: las dos.

**T-C08 — Continuidad de la `x` (C-01).** RF-FW-02, RF-FW-30 · C · R-APP · **PENDIENTE**
- Pasos: repetir T-B03 con `e` sobre al menos 5 patrones que cubran el rango (P17, P13, P2, P1, P4).
- Esperado: `x` media dentro del intervalo de T-B03 ± (max(1, 2·s) + deriva térmica: 13 cuentas/°C ×
  |ΔT ambiente|, cota de RF-APP-27).
- Pasa: 5/5. **Falla ⇒ la variante de SLV-002 tenía otra adquisición u otro factor de temperatura; el
  "como llegó" deja de ser comparable** (C-01, R-02) y se dice en el acta.

**T-C09 — Modo de pruebas.** RF-APP-07, RF-APP-09, RF-APP-12, RF-APP-03 · C · R-APP · **PENDIENTE**
- Pasos: Pruebas → Iniciar sobre P1.
- Esperado: pruebas 1-6 en verde; prueba 5: 12 `#G` "= fábrica", máscara `0000`, 60 `#E` exactos.
- Pasa: APTO.

**T-C12 — Batería.** RF-FW-08 · C · R-TERM · **PENDIENTE**
- Pasos: `9` diez veces.
- Esperado: `:<n>:` como en T-B09, valores coherentes con el mismo estado de batería (±1).
- Pasa: 10/10.

**T-C13 — Byte que no es código.** RF-FW-09 · C · R-TERM · **PENDIENTE**
- Pasos: `z`; esperar 3 s; `6`.
- Esperado: luz y pitido, 0 bytes; después `::<n>`.
- Pasa: las dos.

**T-C14 — Pantalla STONE, no regresión.** RF-FW-10 · C · operador con cámara · **PENDIENTE**
- Pasos: repetir T-B10.
- Esperado: mismas pantallas; mismos valores dentro de la repetibilidad; "PRUEBA ADC", si existe, da la `x`.
- Pasa: 12/12 colores.

**T-C15 — Gatillo pulsado con Bluetooth.** RF-FW-12; RF-APP-06 · C · R-TERM + operador · **PENDIENTE**
- Pasos: en pantalla, OTROS PAPELES blanco (0x01); sobre P1 sin gatillo, `e` → `x`; con el gatillo
  pulsado, `e`.
- Esperado: con gatillo, la respuesta es ≈ `f_1(x)` (p. ej. `x` ≈ 2000 → ≈ 590), **no** ≈ `x`; la
  pantalla también se actualiza. Igual que 2020.
- Pasa: respuesta dentro de la repetibilidad de `f_1(x)`.

**T-C16 — Trama sin medida.** RF-FW-13 · C · R-TERM · **PENDIENTE**
- Pasos: `#G,1#`.
- Esperado: `#G,1,…#` (T-C04); ni luz ni pitido.
- Pasa: las dos.

**T-C17 — Trama incompleta.** RF-FW-13 · C · R-TERM · **PENDIENTE**
- Pasos: `#G,1`; esperar 2,5 s; `e`.
- Esperado: nada tras `#G,1`; `e` → `::<x>`.
- Pasa: las dos.

**T-C36 — `#` suelto (descarte a los 2 s).** RF-FW-13 · C · R-TERM · **PENDIENTE**
- Pasos: (a) `#`; esperar 2,5 s; `1`. (b) `#`; esperar 0,5 s; `1`; esperar 2,5 s; `1`.
- Esperado: (a) `::<n>` (mide y responde). (b) el primer `1` **no** mide (se lo traga la trama
  abierta); el segundo `1` → `::<n>`.
- Pasa: las dos. Falla (a) ⇒ la caducidad no funciona sin `taskUartTimeout` ni perro guardián, y el
  equipo quedaría mudo a los códigos tras cualquier `#`.

**T-C18 — Desbordamiento.** RF-FW-14 · C · R-TERM · **PENDIENTE**
- Pasos: 200 bytes `z` seguidos; esperar 3 s; `#` y 119 bytes `A` sin cierre; esperar 2,5 s; `#V#`.
- Esperado: una medida sin respuesta; nada; `#V,3.6,…#`.
- Pasa: el equipo responde a `#V#` y después a `6`.

**T-C20 — Trama durante una medida.** RF-FW-15 · C · R-TERM · **PENDIENTE**
- Pasos: `6`; a los 200 ms, `#V#`.
- Esperado: `#V,3.6,…#` y `::<n>` **en cualquier orden**; un solo pitido; ninguna medida más.
- Pasa: las tres cosas.

**T-C34 — Trama larga con medida en curso (no bloqueante).** RF-FW-14 · C · R-TERM · **PENDIENTE**
- Pre: sesión cerrada.
- Pasos: 10 veces: `6`; a los 100 ms, la trama de 67 bytes
  `#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#`.
- Esperado: 10 × `#ERR,BLOQUEADO#` y 10 × `::<n>`.
- Pasa: 10/10. Un `#ERR,FORMATO#` o un silencio indican bytes perdidos en el anillo de 64 (O-11): se
  anota; no bloquea, pero la app no debe mandar tramas largas mientras mide.

**T-C33 — Registro de la pantalla.** RF-FW-29, RF-APP-24 · C · R-APP (Botones de pantalla) · **PENDIENTE**
- Pasos: por cada botón de color de la STONE: `#KC#`; pulsar; `#K#`.
- Esperado: `#OK#`; `#K,<n>,<hex>#` con una trama cuyo byte 8 está en 0x01-0x0E y coincide con la
  referencia archivada (o se documenta la diferencia, CA6).
- Pasa: 13 botones mapeados y archivados en `botones_<serie>_<fecha>.csv`.

**T-C11 — Ritmo sostenido.** RF-APP-01 · C · R-APP · **PENDIENTE**
- Pasos: `6` con la pausa de RF-APP-01 durante 30 min.
- Pasa: ninguna perdida.

**T-C30 — App del cliente.** PROTOCOLO §1 · C · operador con la app del cliente · **PENDIENTE sin fecha**
(sin la app no se puede hacer; R-09).

### F6 — Escrituras de administración

Precondición de la fase: C1 cumplida (T-A23 pasa) o decisión escrita de seguir con la tolerancia de
2 ulp; C4 cumplida en firmware para T-C32; copia de coeficientes hecha (T-C29).

**T-C21 — PIN.** RF-FW-17, RF-APP-19 · C · R-TERM · **PENDIENTE**
- Pasos: `#L,0000#`; `#L,2026#`.
- Esperado: `#ERR,PIN#`; `#OK#`.

**T-C22 — Cierre de sesión.** RF-FW-17 · C · R-TERM · **PENDIENTE**
- Pasos: `#Q#`; `#F,1#`; `#L,2026#`; apagar y encender; `#F,1#`.
- Esperado: `#OK#`; `#ERR,BLOQUEADO#`; `#OK#`; `#ERR,BLOQUEADO#`. (`#F,1#` en `DEF` no cambia nada si
  llegara a ejecutarse.)

**T-C31 — Bloqueo por fallos y caducidad.** RF-FW-17, RF-APP-19 · C · R-TERM · **PENDIENTE**
- Pasos: cinco `#L,1111#`; `#L,2026#`; apagar y encender; `#L,2026#`; 10 min 30 s sin tramas `#`;
  `#F,1#`.
- Esperado: cinco `#ERR,PIN#`; `#ERR,BLOQUEADO#`; `#OK#`; `#ERR,BLOQUEADO#`.

**T-C29 — Copia de coeficientes.** RF-APP-22 · C · R-APP (Admin → Leer coeficientes) · **PENDIENTE**
- Esperado: `coeficientes_<serie>_<fecha>.csv` con 12 filas de código y una `T`, iguales a T-C04.

**T-C24 — Formato inválido.** RF-FW-19 · C · R-TERM · **PENDIENTE**
- Pasos (con sesión): `#S,1,abc,0,0,0#`; `#S,1,nan,0,0,0#`; `#S,z,0,0,0,0#`; `#G,1#`.
- Esperado: tres `#ERR,FORMATO#`; `#G,1` igual que en T-C04.

**T-C26 — Cambio de PIN (primera escritura: medir T-C35).** RF-FW-21, RF-APP-19 · C · R-TERM · **PENDIENTE**
- Pasos: `#L,2026#`; `#P,2026,1234#`; `#Q#`; `#L,2026#`; `#L,1234#`; apagar y encender; `#L,1234#`;
  `#P,1234,2026#`; `#V#`.
- Esperado: `#OK#`; `#OK#`; `#OK#`; `#ERR,PIN#`; `#OK#`; `#OK#`; `#OK#`; `#V,3.6,…,DEF,0000#` (sólo cambió
  el PIN: O-12).

**T-C35 — Duración de la escritura de EEPROM.** RF-FW-23 · C · R-TERM · **PENDIENTE**
- Pasos: en el primer `#P` de T-C26 (chip en 0xFF: escribe cabecera y 13 registros) y en el `#S` de
  T-C23, medir del último byte enviado al `#` final de `#OK#`.
- Esperado: se anota (previsión: del orden de 1 s la primera, decenas de ms las siguientes).
- Pasa: respuesta antes del plazo de la app (5000 ms); si no, se sube el plazo.

**T-C23 — Escritura, persistencia y máscara (C3, C5).** RF-FW-19, RF-FW-16, RF-FW-24, RF-APP-17 · C · R-TERM · **PENDIENTE**
- Pasos: `#L,2026#`; `#S,1,0.00000000E+00,-8.65409966E-05,6.19668126E-01,-3.02000000E+02#` (fábrica
  con `c0` = −302: +1 en R); `#G,1#`; `#V#`; `#E,1,2000#`; apagar y encender; `#G,1#`; `#V#`.
- Esperado: `#OK#` (tiempo en T-C35); `#G,1` con esos valores (con la tolerancia de C1); `CAL,0001`;
  `#E,1,591#` (fábrica: 590); tras apagar, igual; `CAL,0001`.
- Pasa: todo. C3 no se puede provocar en el equipo: la cubre T-A32.

**T-C19 — Una sola tabla.** RF-FW-11 · C · operador · **PENDIENTE**
- Pre: el `1` escrito como en T-C23.
- Pasos: pantalla OTROS PAPELES blanco; tres gatillazos sobre P1; comparar con T-B10/T-C14.
- Esperado: el valor en pantalla es el de antes + 1 (dentro de la repetibilidad).

**T-C25 — Vuelta a fábrica.** RF-FW-20, RF-APP-18 · C · R-TERM y R-APP · **PENDIENTE**
- Pasos: `#F,1#`; `#F,*#`; `#V#`; `#G,1#`; T-C05 sólo con el código `1`.
- Esperado: `#OK#`; `#OK#`; `#V,3.6,…,DEF,0000#`; `#G,1` = T-C04; T-C05 da lo mismo que antes.

**T-C32 — Límites de `#ST` en el equipo (C4).** RF-FW-19 · C · R-TERM · **PENDIENTE, sólo con T-A30 en verde**
- **Peligro:** con el firmware de hoy, las tramas de rechazo darían `#OK#` y **escribirían en EEPROM
  un factor que anula las medidas**. No se ejecuta hasta que T-A30 pase.
- Pasos: `#ST,0,1.0E-03,9.0E-01#`; `#ST,0,-5.0E-04,9.0E-01#`; `#GT#`.
- Esperado: dos `#ERR,FORMATO#`; `#GT` igual que en T-C04.

### F7 — Calibración (P8)

**T-C10 — Sesión de patrones con `e`.** RF-APP-13, RF-APP-27 · C · R-APP · **PENDIENTE**
- Pasos: T-B03 con `e`.
- Esperado: registro completo por patrón; media, `n`, `s` sólo con lecturas de `e`.

**T-C28 — Calibración de extremo a extremo.** RF-APP-15, 16, 17, 21 · C · R-APP · **PENDIENTE**
- Pre: C1-C5 cumplidas; P-06 y P-09 decididas.
- Pasos: asistente sobre `1` y `2`; escribir; volver a medir los patrones de esos colores.
- Esperado: acta de antes y después; residuos tras escribir dentro de lo que mostró el asistente ±
  max(suelo, 2·s).

**T-C27 — Ningún `@`.** RF-FW-25, RF-APP-12 · C · revisión de registros · **PENDIENTE**
- Pasos: buscar `@` (0x40) en todo lo recibido de T-C01 a T-C36.
- Esperado: 0 apariciones.

---

## 4. Trazabilidad

### 4.1 Requisito → pruebas

| Requisito | Pruebas |
| :--- | :--- |
| RF-FW-01 | T-C02, T-C05 |
| RF-FW-02 | T-C08 |
| RF-FW-03 | T-C05, T-C07 |
| RF-FW-04 | **T-A20**, T-C05 |
| RF-FW-05 | T-A01, T-A20, T-C04 |
| RF-FW-06 | T-A20, T-A21, T-B07, T-C07 |
| RF-FW-07 | T-B11, T-C06 |
| RF-FW-08 | T-B09, T-C12 |
| RF-FW-09 | T-B08, T-C13 |
| RF-FW-10 | T-B10, T-C02, T-C14 |
| RF-FW-11 | T-C19 |
| RF-FW-12 | T-C15 |
| RF-FW-13 | T-C16, T-C17, T-C36 |
| RF-FW-14 | T-A22, T-C18, T-C34 |
| RF-FW-15 | T-C20 |
| RF-FW-16 | T-A31, T-C03, T-C23 |
| RF-FW-17 | T-A31, T-C21, T-C22, T-C31 |
| RF-FW-18 | T-A31, T-C04 |
| RF-FW-19 | T-A30, T-A31, T-C23, T-C24, T-C32 |
| RF-FW-20 | T-A31, T-C25 |
| RF-FW-21 | T-A31, T-C26 |
| RF-FW-22 | **T-A23**, T-C04 |
| RF-FW-23 | T-C35 |
| RF-FW-24 | T-A24, T-C23 |
| RF-FW-25 | T-C27 |
| RF-FW-26 | T-B12, T-C01 |
| RF-FW-27 | T-A25, **T-A28**, T-C01 |
| RF-FW-28 | T-A27, T-C05 |
| RF-FW-29 | T-A33, T-C33 |
| RF-FW-30 | T-A29, T-C08 |
| RF-APP-01 | T-A07, T-B06, T-C11 |
| RF-APP-02 | T-A03, T-B06 |
| RF-APP-03 | T-A08, T-B01, T-B08, T-B11, T-C03, T-C09 |
| RF-APP-04 | T-A02, T-A09 (aplazada) |
| RF-APP-05 | T-B05 (aplazada); el envío de códigos: T-B01, T-C05 |
| RF-APP-06 | T-C06, T-C15 |
| RF-APP-07 | T-A10, T-B02, T-C09 |
| RF-APP-08 | T-A01, T-A02 |
| RF-APP-09 | T-A11, T-B04, T-C09 |
| RF-APP-10 | T-A04, T-A05, T-A26 |
| RF-APP-11 | T-A06 |
| RF-APP-12 | T-C09, T-C27 |
| RF-APP-13 | T-B03, T-C10 |
| RF-APP-14 | T-A12 |
| RF-APP-15 | T-A06, T-A13, T-A14, T-C28 |
| RF-APP-16 | T-A15, T-C28 |
| RF-APP-17 | T-A18, T-A32, T-C23, T-C28 |
| RF-APP-18 | T-C25 |
| RF-APP-19 | T-A19, T-C21, T-C26, T-C31 |
| RF-APP-20 | T-A16 |
| RF-APP-21 | T-A17, T-C28 |
| RF-APP-22 | T-C29 |
| RF-APP-23 | T-A37 |
| RF-APP-24 | T-A36, T-C33 |
| RF-APP-25 | T-B02, T-B07, T-B09 |
| RF-APP-26 | T-A08, T-A34, T-B01 |
| RF-APP-27 | T-A11, T-A35, T-B03, T-B04, T-C10 |
| PAR-01…PAR-23 | T-A17 (CSV), T-B05 — **aplazadas** con la fase de campo |
| G1 / G2 / G3 / G4 / G5 | T-A28 / T-A20 / T-B03, T-B07, T-B09, T-B10 / T-C01, T-C03, T-C04 / revisión |
| C1 / C2 / C3 / C4 / C5 | T-A23, T-A24 / T-A13, T-A14 / T-A32, T-C23 / T-A30, T-A37, T-C32 / T-C05, T-C23 |

**Cobertura:** los 30 RF-FW y los 27 RF-APP tienen al menos una prueba; las 23 PAR, sólo T-B05 y
T-A17, aplazadas. Requisitos cuya **única** prueba está aplazada: RF-APP-05 (salvo el envío de
códigos) y las PAR. Requisitos cuyas pruebas son todas de equipo y, por tanto, sin evidencia antes de
grabar: RF-FW-01, 02, 03 (parte), 07-13, 15, 23, 25; se aceptan porque su código **no cambia** frente a
2020 salvo lo revisado en `CAMBIOS-V3.6.md` §2.

### 4.2 Prueba → requisito

Cada ficha de §3 abre con sus requisitos. Ninguna prueba queda sin requisito: las de procedimiento
(T-A25, T-A28, T-A29) cubren RF-FW-27 y RF-FW-30; T-C30 cubre `PROTOCOLO-V3.6.md` §1 ("la app del
cliente tiene que seguir funcionando").

### 4.3 Las 45 pruebas JVM de la app, repartidas

| Clase (n.º) | Pruebas del plan |
| :--- | :--- |
| `FabricaTest` (1) | T-A01, T-A02 |
| `ReceptorTest` (5) | T-A03 |
| `CalculoTest` (31) | T-A03 (2), T-A05 (6), T-A06 (2, compartidas con T-A05), T-A10 (8), T-A11 (1), T-A13 (5), T-A17 (1), T-A18 (2), T-A36 (1), T-A37 (1), T-A08 (1), T-A09 (1), `emulacionFloat32ConcuerdaConDoubleSalvoRedondeo` (T-A04 en parte: `float` frente a `double`, no frente a XC8) y `versionConMascaraYOrdenE` (lógica de T-C03 y T-C09: análisis de `#V` con máscara y de `#E`) |
| `AsistenteTest` (8) | T-A12 (2), T-A13 (1), T-A14 (3), T-A36 (1), T-A16 (1) |

---

## 5. Lo que falta antes de grabar

1. **G1 (T-A28):** `.gitattributes` con `*.hex -text` y nuevo commit; md5 del blob = md5 declarado;
   hash del commit en `CAMBIOS-V3.6.md`. Recomendado: decidir antes P-10 (conversión exacta) para que
   el `.hex` de G1 sea el definitivo.
2. **G2 (T-A20):** archivar el informe `pruebas/T-A20.md` con los 12 recuentos y los md5; repetir si el
   `calibracion_v36.c` del commit de G1 cambia.
3. **G3:** T-B03, T-B07, T-B09 (10 repeticiones), T-B10 con SLV-002; y, aunque G3 no las exige, T-B01,
   T-B06 y T-B08, que no cuestan una grabación y fijan tiempos y comportamiento.
4. **G4 preparado:** que la app compare con 2 ulp mientras T-A23 falle (RF-APP-07 r1.1), o T-C04 y la
   prueba 5 pueden dar NO APTO con el chip recién grabado (C-12).
5. **G5:** corregir el `README.md` de la V3.6 (estado de P2 y del firmware) y el de la app (fila 2 de
   las pruebas), `ROADMAP.md` P1/P2/P7-bis y `ARQUITECTURA.map` CA4/CA7, según C-09, C-15, C-16, C-32 y
   C-34 de la SPEC. (C-22, "sin remoto", ya está corregida.)
6. **Deseable antes de grabar** (no son condición de G): T-A21, T-A22, T-A27 completa, T-A29, T-A33 en
   simulador, y T-A34 en la app. Cualquier fallo que obligue a cambiar el fuente cambia el `.hex` y
   devuelve a G1-G2.

## 6. Contradicciones

Están en `SPEC-V3.6.md` §9 (C-08 a C-35), con su decisión o como abiertas. Las que afectan a este plan:
C-12 (tolerancia; T-C04), C-18 (etiquetas y repeticiones de la línea base), C-19 (informe de T-A20),
C-27 (letra de C2), C-32 (md5 y fin de línea; T-A28) y C-34 (C4).
