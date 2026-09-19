# TDD-V3.6 — Plan de pruebas ejecutable del firmware y la app V3.6

**Estado, 19-sep-2026, 10:30: ninguna escritura de calibración se ha probado en un equipo, y C1 no se
cumple por la letra (T-A23 falla el criterio de 0 ulp).** SLV-002 corrió la V3.6 del 18-sep y con ella
pasó G4 (T-C01, T-C03, T-C04, y `#E` 60/60). A las 09:36 se regrabó con un `.hex` del tamaño de la
3.6.1, **sin `#V#` leído todavía** (T-C37). Hechas y en verde: las **56 pruebas JVM** de la app 3.6.4
(ejecutadas en este trabajo), T-A20 (con informe), T-A25 y T-A30. La línea base con el firmware
original (G3) **ya no se puede hacer**: se borró el 18-sep a las 20:24. Cada ficha que cambia lleva
una línea **r1.2** debajo de su cabecera con el estado nuevo y su evidencia; el estado anterior queda
a la izquierda de la flecha.

*Estado r1.1:* «18-sep-2026, 20:20: ninguna prueba se ha ejecutado contra un equipo con V3.6… Hechas:
las 45 pruebas JVM… T-A23 hecha y fallida…»

Requisitos: [`SPEC-V3.6.md`](SPEC-V3.6.md), **revisión 1.2**; estado de cada requisito frente al
código en [`MATRIZ-SPEC-codigo-V3.6.md`](MATRIZ-SPEC-codigo-V3.6.md). Contrato:
[`PROTOCOLO-V3.6.md`](PROTOCOLO-V3.6.md), revisión 1.1 (§4 bis) con las notas de §4 ter. La
calibración con lo medido (asistente, superadministrador, PDF) tiene su propia especificación,
[`SPEC-Calibracion-V3.6.md`](SPEC-Calibracion-V3.6.md), en redacción por otro agente. Este documento
manda sobre las tablas de §5 de la SPEC.

**Recuento: 101 pruebas.** 49 de nivel A (sin equipo), 12 de nivel B (equipo sin grabar) y 40 de
nivel C (tras grabar). Nuevas en r1.2 (§3 bis): T-A38 a T-A49 y T-C37 a T-C40. *Recuento r1.1: 85
(37 A, 12 B, 36 C).*

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

**[r1.2]** Receta corregida, comprobada el 19-sep a las 10:05 con la app 3.6.4 (`OK (56 tests)`): los
jar están en `../libtest/`, **no** en `../build/libtest/` (con la ruta de arriba, `JUnitCore` no se
encuentra), y hay una quinta clase:

```bash
"$JAVA_HOME/bin/java" -cp "build/intermediates/javac/debug/classes;build/intermediates/javac/debugUnitTest/classes;../libtest/junit-4.13.2.jar;../libtest/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore com.dpi.retrov36.CalculoTest com.dpi.retrov36.ReceptorTest \
  com.dpi.retrov36.AsistenteTest com.dpi.retrov36.FabricaTest com.dpi.retrov36.CoherenciaRealTest
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

**[r1.2] Secuencia vigente desde el 19-sep.** F3 ya no se puede hacer en SLV-002 (original borrado);
F4 se hizo con la V3.6. Lo que queda, en este orden:

| Fase | Pruebas | Puerta de salida |
| :--- | :--- | :--- |
| **F0 bis — App en JVM, nuevas** | T-A38, T-A39, T-A40, T-A41 (parte JVM), T-A42 a T-A49 | En verde, o con la contradicción (C-46) cerrada |
| **F1 bis — Firmware en MDB** | T-A41 (parte MDB) | Veredictos de `#S` iguales en app y firmware |
| **F4 bis — Regrabación 3.6.0 → 3.6.1** | **T-C37** | `#V#` con fecha `2026-09-19`, `DEF,0000`, PIN `2026`, `#E` 60/60 y `#S,2,<fábrica>#` rechazado. **Si falla, no se escribe nada** |
| **F5 bis — Método de medida** | **T-C38** (asentamiento), T-C09 con la 3.6.4 | N de asentamiento fijado |
| **F6** | Como arriba; T-C32 ya ejecutable tras T-C37 | Como arriba |
| **F7 — Calibración (P8)** | T-C40 (campaña), T-C10, T-C28 (códigos `1` y `2`, opción C), **T-C39** (opacas con tipo I), T-C27 | Acta de antes y después; huecos de `MATRIZ-SPEC-codigo-V3.6.md` §5.1 cerrados |

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

**T-A10 — Coherencia de las 12 fórmulas.** RF-APP-07 · A · R-JVM · **HECHA-PASA** (con una ampliación pendiente) → **[r1.2] HECHA-PASA**
- **r1.2 (19-sep-2026):** La ampliación está hecha con otro número: `ULP_G = 4`, no 2 (`Ecuacion.java:92`, app 3.6.4; prueba `CalculoTest.igualdadFloat32ConCuatroUlp`), porque XC8 imprime con hasta 3 ulp de error (`CAMBIOS` §7.2). `#E` esperado con la tabla de fábrica: `Pruebas.java:375-393`. Además `CoherenciaRealTest` (7) reproduce con las respuestas literales del 19-sep la prueba 4 con deriva e INVÁLIDA. 56/56 en verde, ejecutadas en este trabajo.
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

**T-A05 — Inversión de una respuesta.** RF-APP-10 · A · R-JVM · **PARCIAL** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Sin cambio en la 3.6.4: la inversión sigue en `double`/Horner (`Inversion.java:58`, C-21).
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

**T-A12 — Avisos de cobertura.** RF-APP-14 · A · R-JVM · **HECHA-PASA** → **[r1.2] HECHA-PASA**
- **r1.2 (19-sep-2026):** Con el catálogo tipo I (3.6.4): `AsistenteTest.coberturaDelCatalogoConLosTipoI`, `losTipoIVanASuCodigoOpaco`, `rangoEstrechoNoSeAjustaAunqueHayaPuntos`, `codigoNoAjustableBloquea`. Esperado ahora: ajustables `1`, `2` (grado 2), `8` (grado 2) y `b` (grado 1); `7`, `a`, `c`, `d` sólo verificar. El criterio "`7` bloqueado porque ningún patrón es de tipo I" queda retirado.
- Pasos: `AsistenteTest.soloBlancoYAmarilloIntensosSonAjustables`, `codigoNoAjustableBloquea`.
- Esperado: sólo `1` y `2` ajustables; `7` bloqueado por "ningún patrón es de tipo I".
- Pasa: en verde.

**T-A11 — Repetibilidad.** RF-APP-09, RF-APP-27 · A · R-JVM · **PARCIAL** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Sin cambio en la 3.6.4: `Repetibilidad.LECTURAS` = 5 (`Repetibilidad.java:13`).
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

**T-A37 — Límites de `#ST` en la app.** RF-APP-23 (C4) · A · R-JVM · **PARCIAL** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Sin cambio en la 3.6.4 (`Tramas.java:240-254` sólo limita `X_0`). El firmware 3.6.1 ya aplica C4 completa (T-A30), así que el riesgo es sólo de la app si algún día ofrece `#ST`.
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

**T-A20 — No regresión de las ecuaciones (G2).** RF-FW-04, RF-FW-05, RF-FW-06 · A · R-MDB · **DECLARADA** → **[r1.2] HECHA-PASA**
- **r1.2 (19-sep-2026):** `pruebas/T-A20.md`: 4 grupos, 786 432 comparaciones, 0 diferencias, con las sumas de control del simulador iguales a la emulación. Repetida con la 3.6.1 (`CAMBIOS` §7.4). Confirmada en hardware: `#E` 60/60 exacto en SLV-002 el 19-sep (acta, G4). C-19 cerrada.
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

**T-A27 — `#E` en simulador.** RF-FW-28 · A · R-MDB · **PARCIAL** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** En SLV-002, prueba 5 de la app 3.6.2: 60 de 60 exactos en `x` = 500, 1000, 2000, 3000 y 4000 para los 12 códigos, en dos pasadas (acta, G4). La tabla de arriba queda confirmada en hardware en esos 60 puntos. Faltan `x` = 0, 200, 4300, 65535 y los 5 rechazos.
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

**T-A23 — Conversión de números con 9 cifras (C1).** RF-FW-22 · A · R-MDB · **HECHA-FALLA** → **[r1.2] HECHA-FALLA (medida cerrada)**
- **r1.2 (19-sep-2026):** Cerrada **como medida** con la 3.6.1 (`CAMBIOS` §7.2, `pruebas/T-A23_T-A30/`): ida y vuelta `#S` → `#G` hasta 5 ulp en simulador (2267 valores) y hasta 7 con la emulación de `strtof`/`efgtoa` validada bit a bit; `%.8E` solo, hasta 3 ulp. **El criterio de 0 ulp (C1) falla.** Lo que se hace con eso: `ULP_G = 4`, `ULP_S = 8` en la app y `#E` tras `#S` (SPEC RF-APP-07 r1.2). Si eso sustituye a C1 **no se decide aquí**: contradicción abierta C-47, para la revisión de arquitectura. SPEC C-44.
- Pasos: 120 valores (los 35 literales distintos de fábrica y 85 aleatorios de 1e-9 a 1e5 de los dos
  signos): (a) `sprintf("%.8E")` de XC8 frente al texto de 9 cifras correctamente redondeado calculado
  fuera; (b) ese texto → `strtod` de XC8 frente al `float` exacto. Además, `abc`, `nan`, `inf`, `1e5x` y
  cadena vacía → rechazo.
- Esperado: **120/120 exactos en las dos direcciones**; los 5 rechazos.
- Resultado 18-sep (`CAMBIOS` §3.3): `sprintf` 60 exactos, 41 a 1 ulp, 19 a 2 ulp; `strtod` 68 exactos,
  49 a 1 ulp, 2 a 2 ulp, 1 a 3 ulp. Rechazos correctos.
- Falla ⇒ C1 no cumplida. Se repite tras la conversión exacta propia (RF-FW-22 r1.1, P-10).

**T-A30 — Límites de `#ST` (C4).** RF-FW-19 · A · R-MDB · **PARCIAL** → **[r1.2] HECHA-PASA**
- **r1.2 (19-sep-2026):** Con la 3.6.1 (`calibracion_v36.c:620-621`): los 5 casos de arriba dan lo esperado, más el vértice, los bordes y 240 casos aleatorios que coinciden con un barrido fuera (`pruebas/T-A23_T-A30/resultado_T-A30.txt`, `CAMBIOS` §7.3). **Aviso:** el paso 1 (`#ST` con el texto de fábrica) deja la máscara en `CAL,1000` y en la 3.6.1 no hay orden para volver (C-30): **no se repite en un equipo**.
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

**T-A28 — Fuente y `.hex` atados a un commit (G1).** RF-FW-27 · A · operador con git · **PARCIAL** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Para la 3.6.1 (`8860445`): árbol limpio en `RetroVertical_V3.6.X` en ese commit; `md5sum -c hex/fuente.md5` sin fallos; md5 del blob = `8736c05d…` = declarado; `git check-attr text` → `unset`. Falta el quinto punto: `CAMBIOS-V3.6.md` no cita `8860445` (sólo `f75ff88`, de la 3.6). 4 de 5.
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

**T-B01 — Detección.** RF-APP-03, RF-APP-26 · B · R-APP · **PENDIENTE** → **[r1.2] HECHA-FALLA (18-sep) y ya no realizable**
- **r1.2 (19-sep-2026):** Con la app 3.6.0, SLV-002 no respondió a `#V#`, `e`, `6` ni `@LEERV` (SPEC RF-APP-03). El firmware original se borró el 18-sep a las 20:24: la prueba no se puede repetir en SLV-002; vale para el segundo V3.
- Pasos: equipo sobre P1; Pruebas → Iniciar.
- Esperado en el registro: TX `#V#` → sin respuesta (el equipo enciende la luz y pita); TX `e` → sin
  respuesta; TX `6` → `::<n>` con n > 0 ⇒ "V3 2020 sin `e`". Después, la prueba 3 envía `1` y recibe
  `::<n>`.
- Pasa: clasificación correcta, ninguna TX con `@`, ninguna TX de más de 49 bytes.

**T-B08 — `#V#` es inocuo en 2020.** RF-FW-09, RF-APP-03 · B · R-TERM · **PENDIENTE** → **[r1.2] NO REALIZABLE en SLV-002**
- **r1.2 (19-sep-2026):** Firmware original borrado. Queda para el segundo V3.
- Pasos: `#V#`; esperar 3 s; `6`.
- Esperado: a `#V#`, luz y pitido y **0 bytes** recibidos en 3 s; a `6`, `::<n>` en < 2,5 s.
- Pasa: las dos cosas.

**T-B06 — Tiempos.** RF-APP-01, RF-APP-02; §1 r1.1 · B · R-TERM · **PENDIENTE** → **[r1.2] NO REALIZABLE en SLV-002**
- **r1.2 (19-sep-2026):** Queda para el segundo V3. Los plazos provisionales de RF-APP-02 se sostienen con los registros del 19-sep (respuestas `#` en 93-128 ms; un disparo cada ~1,6 s).
- Pasos: (a) 50 × `6` sobre P1 con pausa de 1500 ms desde el envío y 600 ms desde el último byte;
  anotar envío → primer byte, envío → último byte, hueco máximo entre bytes de una respuesta.
  (b) `6`; en cuanto llegue `::<n>`, esperar **30 ms** y enviar `6`. (c) `6`; al llegar `::<n>`,
  esperar **300 ms** y enviar `6`.
- Esperado: (a) 50/50 respuestas; (b) **una** respuesta (el segundo `6` cae antes de `clearBuffer()`
  y se pierde); (c) **dos** respuestas (el segundo `6` cae en la pausa de 500 ms y mide).
- Pasa: (a) sin pérdidas, y se fijan plazo = 1,5 × máximo y silencio ≥ 3 × hueco máximo (mínimo 50 ms);
  (b) y (c) como se esperan. Si (b) o (c) no, se corrige §1 y RF-APP-01.

**T-B09 — Batería (G3).** RF-FW-08, RF-APP-25 · B · R-TERM o R-APP (Línea base) · **PENDIENTE** → **[r1.2] NO REALIZABLE (G3)**
- **r1.2 (19-sep-2026):** Firmware original borrado el 18-sep a las 20:24 sin hacerla.
- Pasos: `9` diez veces, con la pausa de RF-APP-01.
- Esperado: diez respuestas `:<n>:` con `<n>` entero; se anotan los literales.
- Pasa: 10/10 con ese formato. (La línea base de la app hace hoy 3 repeticiones, C-18: poner 10.)

**T-B07 — Negativos y saturación (G3).** RF-FW-06 · B · R-TERM o R-APP (Línea base) · **PENDIENTE** → **[r1.2] NO REALIZABLE (G3)**
- **r1.2 (19-sep-2026):** Firmware original borrado. Lo único que queda: el barrido de 255 bytes en oscuro (T-B11, x ≈ 599-634 por inversión). Con la V3.6, el oscuro sale en x ≈ 575 (acta, G4).
- Pasos: óptica tapada con una tapa opaca: los 12 códigos. Sobre el patrón más alto (P4, 828,
  blanco XI, "en el techo"): `1` y `2`, cinco veces cada uno.
- Esperado (emulación, **referencia, no criterio**): en oscuro `x` ≈ 180-200 y los 12 dan `::0`; sobre
  P4, con `x` ≈ 3271 (dato del 18-sep), `1` ≈ `::797` y `2` ≈ `::743`.
- Pasa: 12 + 10 respuestas registradas. Si alguna en oscuro no es `::0`, el modelo de negativos no es
  el de T-A21 y se revisa RF-APP-10.

**T-B02 — Coherencia por inversión.** RF-APP-07, RF-APP-25 · B · R-APP (Pruebas y Línea base) · **PENDIENTE** → **[r1.2] NO REALIZABLE en SLV-002**
- **r1.2 (19-sep-2026):** Firmware original borrado; T-B11 hizo la parte esencial (12 códigos en oscuro, x de 599 a 634).
- Pasos: sobre P1, los 12 códigos (la línea base de la app lo etiqueta hoy "T-B03", C-18).
- Esperado: los 12 intervalos de `x` se solapan dentro de max(suelo, 2·s) (como el 18-sep: 599-634).
- Pasa: prueba 4 de la app en verde (≥ 6 evaluables).

**T-B04 — Repetibilidad con Bluetooth.** RF-APP-09, RF-APP-27 · B · R-APP · **PARCIAL** → **[r1.2] PARCIAL, ya no realizable con el original**
- **r1.2 (19-sep-2026):** Hecho con la V3.6 y `e` en lugar de `6`: ver T-C06 y la sesión de campo del 19-sep (s de 2,7 a 7,7 cuentas en 17 series de 9 sin el primer disparo).
- Hecho: 8 blancos por pantalla (`x` de 1739 a 3271, ±1).
- Pasos: 10 × `6` sin mover el equipo sobre P2 (414), P1 (762) y P4 (828).
- Esperado: por patrón, media de `x`, `s` y CV; se espera `s` ≤ 3 (semianchura de la inversión).
- Pasa: 30/30 lecturas válidas. Con esto se fija el umbral metrológico (P-04).

**T-B03 — Patrones antes de grabar: "como llegó" (G3).** RF-APP-13, RF-APP-27 · B · R-APP (Medida de patrones) · **PENDIENTE** → **[r1.2] NO REALIZABLE (G3)**
- **r1.2 (19-sep-2026):** Firmware original borrado. El "como llegó" que queda son los 8 blancos por pantalla del acta (T-B10).
- Pasos: oscuro al principio (anotar); por cada P1-P31: N = 10 sin mover; después 3 recolocaciones
  con N = 5; oscuro al final; hora y temperatura ambiente a mano.
- Esperado: `medidas_<serie>_<fecha>.csv` con cada lectura (respuesta literal, código `6`, `x` y
  semianchura, método "6 invertido"); por patrón, media, `s`, `n`.
- Pasa: 31 patrones con ≥ 10 lecturas válidas (una lectura `::0` no es válida y se anota como tal).

**T-B10 — Pantalla STONE, línea base (G3).** RF-FW-10 · B · operador con cámara · **PARCIAL** → **[r1.2] PARCIAL (definitiva)**
- **r1.2 (19-sep-2026):** Acta de SLV-002: P3, P2, P28, P1 y P7 por pantalla, antes y después. No se puede completar: el original se borró.
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

**T-C01 — Grabación.** RF-FW-26, RF-FW-27 · C · R-IPE · **PENDIENTE** → **[r1.2] HECHA-PASA**
- **r1.2 (19-sep-2026):** 18-sep 20:24:38 (V3.6, `grabacion_V3.6_2026-09-18.log`) y 19-sep 09:36:15 (`.hex` del tamaño de la 3.6.1, `grabacion_V3.6.1_2026-09-19.log`, commit `869d3c6`): *Program Succeeded*. El *Verify* posterior lee ceros por `CP = ON`, como avisaba la ficha: confirmado dos veces, no es fallo.
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

**T-C03 — Versión (G4).** RF-FW-16, RF-APP-03 · C · R-TERM y R-APP · **PENDIENTE** → **[r1.2] HECHA-PASA (con la V3.6)**
- **r1.2 (19-sep-2026):** 19-sep 08:53, app 3.6.2: `#V,3.6,2026-09-18,DEF,0000#` en 93 ms (registro `rtv36_20260919_090703.txt`). Luz y pitido no anotados. **Con la 3.6.1 grabada a las 09:36, pendiente: T-C37.**
- Pasos: `#V#`. Después, Pruebas de la app (prueba 2).
- Esperado: `#V,3.6,2026-09-18,DEF,0000#` en < 2 s (la fecha es la de compilación del `.hex` de G1),
  sin luz ni pitido; la app: "V3.6, marca DEF, ningún código ajustado; temperatura de fábrica".
- Pasa: literal exacto.

**T-C04 — Coeficientes de fábrica (G4).** RF-FW-18, RF-FW-22, RF-FW-05 · C · R-TERM y R-APP · **PENDIENTE** → **[r1.2] HECHA-PASA (con la V3.6, a 4 ulp)**
- **r1.2 (19-sep-2026):** 19-sep, app 3.6.2: 12 `#G` y `#GT#` "= fábrica" con `ULP_G = 4`. Literal no exacto, como se preveía con XC8: `#GT` → `4.32120039E-04` (esperado `4.32119996E-04`) y `9.01486480E-01` (esperado `9.01486516E-01`). Con la 3.6.1: T-C37.
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

**T-C06 — `e`.** RF-FW-07, RF-APP-06 · C · R-TERM · **PENDIENTE** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** `e` responde en V3.6: 5 lecturas sobre P1 (3016-3027, s = 4,0; acta) y 180 lecturas de campo el 19-sep. Falta la concordancia con `6` invertido. **Criterio nuevo:** las lecturas de una serie se toman tras el asentamiento de RF-APP-28.
- Pasos: sobre P1, 10 × `e`; después 10 × `6`.
- Esperado: `::<x>` con 200 < x ≤ 4000; la media de `x` cae dentro del intervalo de inversión de las
  respuestas a `6` ± max(1, 2·s).
- Pasa: 10/10 y concordancia.

**T-C05 — No regresión por Bluetooth (C5).** RF-FW-03, RF-FW-04, RF-FW-28 · C · R-TERM · **PENDIENTE** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** (a) cubierta por la prueba 5 de la app: `#E` 60/60 exacto (acta, G4). (b) pendiente: el emparejado `e` → código → `e` sobre P2, P1 y P4.
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

**T-C08 — Continuidad de la `x` (C-01).** RF-FW-02, RF-FW-30 · C · R-APP · **PENDIENTE** → **[r1.2] HECHA-FALLA por pantalla (sustituta)**
- **r1.2 (19-sep-2026):** No se puede hacer como está escrita: el original se borró sin T-B03. Sustituto (acta): mismos patrones por pantalla antes y después, **−80 a −89 cuentas de `x` en x = 1700-2300** (P3, P2, P28), no concluyente en la zona alta. El "como llegó" deja de ser comparable en la zona media. C-01 sigue abierta con esta evidencia.
- Pasos: repetir T-B03 con `e` sobre al menos 5 patrones que cubran el rango (P17, P13, P2, P1, P4).
- Esperado: `x` media dentro del intervalo de T-B03 ± (max(1, 2·s) + deriva térmica: 13 cuentas/°C ×
  |ΔT ambiente|, cota de RF-APP-27).
- Pasa: 5/5. **Falla ⇒ la variante de SLV-002 tenía otra adquisición u otro factor de temperatura; el
  "como llegó" deja de ser comparable** (C-01, R-02) y se dice en el acta.

**T-C09 — Modo de pruebas.** RF-APP-07, RF-APP-09, RF-APP-12, RF-APP-03 · C · R-APP · **PENDIENTE** → **[r1.2] HECHA-FALLA (criterio de la app 3.6.2); repetir con la 3.6.4**
- **r1.2 (19-sep-2026):** Prueba 4 en FALLO el 19-sep (acta): primera pasada con el equipo apoyado a mitad; segunda, 11/12 dentro y deriva 3004 → 3027. Era el criterio. La 3.6.3/3.6.4 cambian la prueba 4 (deriva, resolución local, INVÁLIDA, asentamiento antes de la `e` inicial) y `CoherenciaRealTest` da APTO con las respuestas literales de la segunda pasada. Falta repetirla en el equipo.
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

**T-C14 — Pantalla STONE, no regresión.** RF-FW-10 · C · operador con cámara · **PENDIENTE** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Acta: pantalla y Bluetooth coinciden sobre P1 dentro del ruido cerca del techo del blanco (776 por BT, 780-781 por pantalla). Faltan los 12 colores × 3.
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

**T-C32 — Límites de `#ST` en el equipo (C4).** RF-FW-19 · C · R-TERM · **PENDIENTE, sólo con T-A30 en verde** → **[r1.2] PENDIENTE, ejecutable sólo con la 3.6.1 confirmada**
- **r1.2 (19-sep-2026):** T-A30 pasa. Sólo después de T-C37 (que `#V#` confirme la 3.6.1): con la V3.6 del 18-sep, las dos tramas **se escribirían** en EEPROM. No enviar nunca `#ST` con los valores de fábrica (C-30).
- **Peligro:** con el firmware de hoy, las tramas de rechazo darían `#OK#` y **escribirían en EEPROM
  un factor que anula las medidas**. No se ejecuta hasta que T-A30 pase.
- Pasos: `#ST,0,1.0E-03,9.0E-01#`; `#ST,0,-5.0E-04,9.0E-01#`; `#GT#`.
- Esperado: dos `#ERR,FORMATO#`; `#GT` igual que en T-C04.

### F7 — Calibración (P8)

**T-C10 — Sesión de patrones con `e`.** RF-APP-13, RF-APP-27 · C · R-APP · **PENDIENTE** → **[r1.2] PARCIAL**
- **r1.2 (19-sep-2026):** Sesión de campo del 19-sep con `e` (V3.6): 17 series de 9 (09:12-09:25) y 9 series de 3 (08:59-09:05) sobre 20 patrones blancos y amarillos. Sin 3 × 5 recolocaciones, sin oscuro, sin temperatura. Hallazgos que cambian el método: primer disparo bajo (RF-APP-28), un descolgado (RF-APP-29), P24 con tres candidatas (RF-APP-30), XI desordenados (SPEC C-39).
- Pasos: T-B03 con `e`.
- Esperado: registro completo por patrón; media, `n`, `s` sólo con lecturas de `e`.

**T-C28 — Calibración de extremo a extremo.** RF-APP-15, 16, 17, 21 · C · R-APP · **PENDIENTE** → **[r1.2] PENDIENTE**
- **r1.2 (19-sep-2026):** Pre adicional r1.2: P-06 decidida (opción C); faltan T-C38 (asentamiento), C-40 (P24) y T-A41 (grado del código `2`).
- Pre: C1-C5 cumplidas; P-06 y P-09 decididas.
- Pasos: asistente sobre `1` y `2`; escribir; volver a medir los patrones de esos colores.
- Esperado: acta de antes y después; residuos tras escribir dentro de lo que mostró el asistente ±
  max(suelo, 2·s).

**T-C27 — Ningún `@`.** RF-FW-25, RF-APP-12 · C · revisión de registros · **PENDIENTE**
- Pasos: buscar `@` (0x40) en todo lo recibido de T-C01 a T-C36.
- Esperado: 0 apariciones.

---

## 3 bis. Fichas nuevas de la revisión 1.2

Datos de referencia de estas fichas: `07 pruebas/19092026_0900/p29_p24_p23_p5/medidas_SLV-002_20260919_091213 (3).csv`
(17 series de 9 disparos con `e`, 09:12-09:25) y `medidas_SLV-002_20260919_085924 (1).csv` (9 series de
3, 08:59-09:05), ambos de SLV-002 con la V3.6 del 18-sep. **Son los dos ficheros más largos; los demás
de la carpeta son prefijos suyos** (L-21). Las cifras "esperadas" que salen de ellos las calculó este
trabajo con Python; son cálculo sobre medidas, no medidas nuevas.

### F0 bis — App en JVM (receta R-JVM)

**T-A38 — Asentamiento.** RF-APP-28 · A · R-JVM · **PENDIENTE**
- Pre: transporte simulado que responde `::<x>` a cada `e`.
- Pasos: con N = 0, 1 y 3 (`Sesion.disparosAsentamiento`), (a) una serie de 9 en Medida; (b) la
  prueba 3 en V3.6.
- Esperado: (a) se envían N + 9 `e`; el CSV y la media tienen **9** lecturas; el registro tiene N
  líneas "disparo de asentamiento, descartado"; (b) la `e` inicial que usa la prueba 4 es la N + 1-ésima.
  Con N fuera de 0-5, la interfaz no deja empezar.
- Pasa: los 3 valores de N así. Falla: una lectura de asentamiento en el CSV o en la media.

**T-A39 — Disparo descolgado.** RF-APP-29 · A · R-JVM · **PENDIENTE** (cierra C-46)
- Pasos: aplicar la regla a las 17 series literales (disparos 2-9 de cada una) y a dos sintéticas:
  `{1000 × 8, 1100}` y `{1000, 1001, 999, 1000, 1002, 1000, 1001, 999, 1000}`.
- Esperado con la regla de RF-APP-29, max(30, 5 · 1,4826 · MAD): **1** descolgado en las 17 (el 3031
  de P7, serie de las 09:19:26); **P2 de las 09:17:21 sin descolgados** (su 2004 está a 12 de la
  mediana 2016); 1 en la primera sintética; 0 en la segunda. La media de P7 sin el 3031 = 3230,9.
- Pasa: esos cuatro resultados. **Con la regla de `Veredicto` de la 3.6.5 (`ff66f93`: 5 · max(1,4826 ·
  MAD ; 1,5)) salen 2 descolgados, no 1 (también el 2004 de P2): fallaría.** `CampanaTest.disparoDescolgadoDeP7`
  no lo ve porque usa la serie de P7 **con** su primer disparo. Se corrige la regla o se decide por
  escrito (C-46).

**T-A40 — ¿Es este el patrón?** RF-APP-30 · A · R-JVM · **PENDIENTE**
- Pasos: alimentar la sesión con las series literales en su orden y etiqueta: P24 (2048, 2071, 2076)
  y después P24 (2439, 2438, 2453); y P20 a las 09:15:47 y otra "P20" a las 09:23:40. Además, P23
  09:14:57 (mediana 2721,5) y P23 09:24:18 (2756,5).
- Esperado: aviso "¿es este el patrón?" en la segunda P24 (Δ mediana ≈ 370 > 100); **ningún aviso**
  en la segunda P23 (Δ = 35) ni en la segunda P20 (Δ = 8,5) por el criterio de 100 cuentas. Si la app
  añade el criterio de parecido con otro patrón (`Veredicto`, 3.6.5), la serie de las 09:23:40
  etiquetada P24 debe avisar de que se parece a P20.
- Pasa: 1 aviso donde se espera y 0 donde no. Las salidas Repetir / Aceptar con nota / Era otro patrón
  se prueban en T-A43.

**T-A41 — Curva C frente al límite de `#S`.** RF-FW-31, RF-APP-15, P-16 · A · R-JVM y R-MDB · **PENDIENTE**
- Pre: puntos por patrón = media de los disparos 2-9 (2-3 en la sesión de las 08:59), sin el 3031 de
  P7, media de las series repetidas; **P24 fuera** (C-40) y la serie de las 09:23:40 contada como P20.
  Blanco (código `1`): P1 3071,0; P2 2015,0; P3 1659,9; P4 3361,1; P6 3115,5; P7 3234,0. Amarillo
  (código `2`): P5 2555,0; P8 2517,5; P9 2556,0; P10 2461,5; P20 2801,1; P21 1555,5; P22 1469,6; P23
  2740,3; P25 2202,0; P26 2148,9; P29 1724,5; P30 1664,0; P31 2096,5.
- Pasos (JVM): `Asistente.proponer` para `1` y `2`, grados 1 y 2, y `criterioFirmwareS` de cada
  ecuación. Pasos (MDB): con el `calibracion_v36.c` de `8860445` y sesión abierta, `#S,<k>,<los cuatro
  coeficientes que dio la app>#` para los cuatro casos.
- Esperado (cálculo en `double` de este trabajo; los coeficientes de la app deben coincidir a 10⁻³
  relativo):

  | Código, grado | c2 | c1 | c0 | R(600) | Criterio de `#S` |
  | :--- | ---: | ---: | ---: | ---: | :--- |
  | `1`, 1 | 0 | 0,27854 | −110,29 | 57 | cumple |
  | `1`, 2 | 4,6642E-05 | 0,044678 | 160,26 | 204 | cumple |
  | `2`, 1 | 0 | 0,31996 | −111,04 | 81 | cumple |
  | `2`, 2 | −6,6249E-05 | 0,60147 | −396,80 | **−60** | **no cumple** |

- Pasa: los 4 veredictos de la app iguales a la tabla; el firmware responde `#OK#` en los tres que
  cumplen y `#ERR,FORMATO#` en `2` grado 2 (4/4 iguales), y en `2` grado 2 la app ofrece la recta. **No
  se ejecuta en SLV-002**: en el simulador, sin escribir en un equipo.

### F0 bis — Modo Campaña (RF-APP-32; receta R-JVM, clases puras con almacenamiento simulado)

**T-A42 — Flujo guiado.** RF-APP-32 · A · R-JVM · **PENDIENTE**
- Pasos: campaña simulada de 5 patrones del catálogo, OK en cada uno, el 3.º saltado, N = 1.
- Esperado: 5 peticiones "Coloque …" en el orden del catálogo; 4 × (1 + 9) = 40 `e` enviadas; 4
  series guardadas y 1 marca "saltado"; ningún `e` sin un OK previo.
- Pasa: exactamente eso.

**T-A43 — Veredicto de la serie.** RF-APP-32, RF-APP-29, RF-APP-30 · A · R-JVM · **PENDIENTE**
- Pasos: las 17 series literales (con su asentamiento, disparo 1) y una sintética con s = 20.
- Esperado: 0 REPETIR por ruido en las 17 (s sin descolgados de 2,7 a 7,7); REPETIR en la sintética
  (s > 15); las tres salidas escriben en el diario un evento de veredicto con su nota (Aceptar con nota
  sin nota → no se acepta) y "Era otro patrón" escribe un evento de reasignación sin borrar la serie.
- Pasa: todo.

**T-A44 — Identificación de la campaña.** RF-APP-32.1 · A · R-JVM · **PENDIENTE**
- Pasos: (a) abrir con serie SLV-002 y MAC `00:21:13:05:19:3B`, 1 serie, cerrar la app, reabrir con
  las mismas; (b) serie SLV-002 y MAC `00:21:13:05:19:3C`; (c) MAC `…:3B` y serie SLV-003, sin
  confirmar y confirmando.
- Esperado: (a) se retoma la misma campaña; (b) el diario de la campaña de `…:3B` no cambia de tamaño
  y hay 1 aviso; (c) sin confirmar, 0 eventos; confirmando, 2 diarios distintos.
- Pasa: los tres casos.

**T-A45 — ZIP y md5.** RF-APP-32.4 · A · R-JVM · **PENDIENTE**
- Pasos: campaña con 2 conexiones y 3 series; exportar.
- Esperado: `campana_SLV-002_<AAAAMMDD-HHMMSS>.zip` con 5 entradas (`campana.csv`, diario,
  `pruebas_<serie>_<mac>.txt`, registro de tramas, `resumen.txt`); los md5 de `resumen.txt` = md5 de las
  piezas extraídas; el registro contiene todas las TX/RX de las 2 conexiones; el md5 anotado = md5 del
  fichero.
- Pasa: todo.

**T-A46 — Compartir una vez.** RF-APP-32.5 · A · R-JVM + revisión de la interfaz · **PENDIENTE**
- Pasos: exportar dos veces seguidas; construir el `Intent`.
- Esperado: `ACTION_SEND` con **1** URI `.zip` (no `ACTION_SEND_MULTIPLE`); los dos ZIP con nombres
  distintos (sufijo `_2`); en la interfaz de campaña, 1 sola acción de compartir.
- Pasa: todo. Cierra P-03 (SPEC §10) en la práctica.

**T-A47 — Diario de sólo añadir.** RF-APP-32.2 · A · R-JVM · **PENDIENTE**
- Pasos: 100 disparos simulados, copiando el diario tras cada uno; después, truncar una copia en un
  byte al azar y leerla (20 veces).
- Esperado: cada copia es prefijo byte a byte de la siguiente (100/100); al leer una truncada se pierde
  como mucho el último evento y `lineasMalas()` ≤ 1 (20/20).
- Pasa: todo. Es la prueba de que no se repite RF-08 (truncar al guardar).

**T-A48 — Retomar y cerrar.** RF-APP-32.3 · A · R-JVM · **PENDIENTE** (falta el evento de cierre)
- Pasos: 5 patrones; matar tras la serie 3; reabrir; terminar; "Cerrar campaña"; reconectar.
- Esperado: reanuda en el patrón 4 con las 3 series (diario idéntico byte a byte hasta ese punto); tras
  cerrar, el diario tiene un evento de cierre y la reconexión abre campaña nueva sin añadir nada a la
  cerrada.
- Pasa: todo. Con la 3.6.5 no puede pasar: la lista de eventos de `Campana` no tiene cierre de campaña (SPEC RF-APP-32.3).

**T-A49 — Importar lo previo.** RF-APP-32.6 · A · R-JVM · **PENDIENTE**
- Pasos: importar los 16 CSV de `07 pruebas/19092026_0900/` (con subcarpeta) en una campaña de MAC
  `00:21:13:05:19:3B`; repetir con una campaña de otra MAC.
- Esperado: **180 lecturas** (153 + 27), no la suma de todos los ficheros; ninguna duplicada; con otra
  MAC, 0.
- Pasa: las dos cosas.

### F4 bis — Regrabación 3.6.0 → 3.6.1 (en SLV-002)

**T-C37 — Regrabación y vuelta a `DEF`.** RF-FW-16, 18, 24, 26, 28, 31 · C · R-IPE, R-TERM o R-APP · **PARCIAL**
- Hecho: grabación del 19-sep a las 09:36:15, *Program Succeeded*, memoria hasta `0x1d07f` (tamaño de
  la 3.6.1), commit `869d3c6`. El estado previo leído a las 08:53 era `DEF,0000` y no consta ninguna
  escritura entre las 08:53 y las 09:36.
- Pasos, en este orden: `#V#`; los 12 `#G` y `#GT#`; `#L,2026#`; Pruebas de la app 3.6.4 (prueba 5:
  60 `#E`); `#S,2,-7.30000025E-08,2.82508001E-04,1.24075353E-01,-1.30000000E+02#` (la curva de fábrica
  del `2`); `#V#`; `#Q#`.
- Esperado: `#V,3.6,2026-09-19,DEF,0000#` (la fecha es la que distingue la 3.6.1, C-37); 13 "=
  fábrica" a `ULP_G = 4`; `#OK#` con el PIN de fábrica (la EEPROM borrada por la grabación, a 0xFF,
  vuelve a fábrica, PIN incluido: `calibracion_v36.c:184-186`); 60/60 `#E`; **`#ERR,FORMATO#`** al `#S`
  (límite de RF-FW-31 en hardware, sin escribir); `#V#` sigue en `DEF,0000`.
- **Peligro:** si `#V#` responde `2026-09-18`, el equipo lleva la 3.6 y el `#S` **se escribiría**: no
  se envía. Ese caso cierra C-36 en contra y se para.
- Pasa: los 7 puntos. Cierra C-36.

### F5 bis — Método de medida

**T-C38 — Asentamiento: cuántos disparos se descartan.** RF-APP-28, P-13 · C · R-APP (Medida, N = 0) · **PENDIENTE**
- Pre: T-C37 pasa. Equipo apoyado, sin tocar el gatillo; temperatura ambiente anotada.
- Pasos: sobre P22 (~1470), P2 (~2016) y P4 (~3361): 5 series de **12** `e` con N = 0, con una pausa
  de 60 s entre series; y en P2, 3 series más con pausa de 10 s.
- Cálculo: por posición *k*, sesgo = media de `x_k − mediana(x_7…x_12)` en las series de la misma
  pausa.
- Esperado (referencia del 19-sep, no criterio): sesgo ≈ −17 en *k* = 1, ≈ −5 en *k* = 2, ≈ −3,5 en
  *k* = 3.
- Resultado: **N = el menor *k* tal que |sesgo(*k* + 1)| ≤ 2 cuentas** en los tres patrones y con las
  dos pausas. Se anota y se fija en la app.
- Pasa: N fijado y, repitiendo 3 series con ese N, el primer disparo que cuenta queda a ≤ 2 cuentas de
  la mediana. Si ningún N ≤ 5 lo cumple, se anota y la decisión es del propietario (P-13).

### F7 — Calibración, ampliación

**T-C39 — Opacas con los patrones tipo I.** RF-APP-31, RF-APP-14, RF-FW-31 · C · R-APP (campaña y asistente) · **PENDIENTE**
- Pre: T-C37 y T-C38 pasan; C-41 (P32) decidida; copia de coeficientes (T-C29).
- Pasos: (a) oscuro (tapa opaca): 9 `e` tras el asentamiento; (b) cada P32a-P50: 9 `e` tras el
  asentamiento; (c) para `8` y `b`, el asistente con el grado que permita la cobertura; `#S`; `#E` en
  5 puntos; volver a medir 3 patrones de ese color; (d) para `7`, `a`, `c`, `d`, sólo verificar: la
  respuesta del código frente al certificado.
- Criterio de "calibrable" por código, **medible**: (1) cada patrón con `x` media ≥ `x_oscuro` + 3 ·
  `s_oscuro`; (2) las `x` medias en el mismo orden que los certificados; (3) al menos 3 niveles
  distintos con rango ≥ 20 unidades de R y ≥ 30 % del mayor (umbrales de la app, sin medida: C-43).
- Esperado (**estimación con las curvas de fábrica**, que en las intensas ya se ha visto que no valen
  para SLV-002): verde (6-7) en x ≈ 518-520 y azul (7-10) en x ≈ 560-589, por debajo del 600 de `#S` y
  cerca del oscuro de la V3.6 (x ≈ 575, acta): **no calibrables**, sólo verificar. Rojo (46-81) en x ≈
  994-1198 y amarillo (64-122) en x ≈ 923-1467: calibrables si cumplen (1)-(3).
- Pasa: `8` y `b` escritos con `#OK#`, `#G` a `ULP_S = 8`, `#E` 5/5 a ±1, y residuo al volver a medir
  ≤ max(1, 2 · s) propagado por la pendiente de la curva nueva; o, si no cumplen (1)-(3), el motivo
  anotado y nada escrito. Los demás, verificados y anotados.

**T-C40 — Campaña de extremo a extremo.** RF-APP-32 · C · R-APP (campaña) · **PENDIENTE**
- Pre: app 3.6.5 o posterior (`ff66f93`); T-C37 pasa.
- Pasos: campaña de 5 patrones en SLV-002: el 2.º saltado; en el 3.º, cerrar la app a la fuerza a
  mitad de la serie; reabrir y retomar; en el 4.º, "Era otro patrón"; cerrar la campaña; exportar.
- Esperado: la campaña retoma en el 3.º con los disparos ya hechos en el diario; el ZIP contiene las 5
  piezas de T-A45, con 3 series aceptadas, 1 saltada, 1 reasignada y el registro de las 2 conexiones;
  el md5 del ZIP coincide con el anotado en el registro; en el teléfono aparece **un** fichero, sin
  copias `(1)`.
- Pasa: todo.

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

**[r1.2] Filas nuevas y cambiadas:**

| Requisito | Pruebas |
| :--- | :--- |
| RF-FW-16 | + T-C37 |
| RF-FW-19 | T-A30 (**pasa**), T-A31, T-C23, T-C24, T-C32 |
| RF-FW-31 (nuevo) | T-A30, **T-A41**, **T-C37** |
| RF-APP-14 | T-A12 (con tipo I), T-C39 |
| RF-APP-15 | + **T-A41**, T-C39 |
| RF-APP-28 (nuevo) | **T-A38**, **T-C38** |
| RF-APP-29 (nuevo) | **T-A39**, T-A43 |
| RF-APP-30 (nuevo) | **T-A40**, T-A43 |
| RF-APP-31 (nuevo) | T-A12, **T-C39** |
| RF-APP-32 (nuevo) | **T-A42** a **T-A49**, **T-C40** |
| G4 con la 3.6.1 | **T-C37** |
| G3 | **ya no realizable** (original borrado): T-B03, T-B07, T-B09 |


### 4.2 Prueba → requisito

Cada ficha de §3 abre con sus requisitos. Ninguna prueba queda sin requisito: las de procedimiento
(T-A25, T-A28, T-A29) cubren RF-FW-27 y RF-FW-30; T-C30 cubre `PROTOCOLO-V3.6.md` §1 ("la app del
cliente tiene que seguir funcionando").

### 4.3 Las 45 pruebas JVM de la app, repartidas

**[r1.2]** Con la app 3.6.4 son **56** (`AsistenteTest` 12, `CalculoTest` 31, `CoherenciaRealTest` 7,
`FabricaTest` 1, `ReceptorTest` 5), en verde el 19-sep a las 10:05. Las nuevas de `AsistenteTest`
cubren T-A12 (cobertura con tipo I, rango estrecho), el criterio de `#S` del firmware
(`criterioDelFirmware361ParaS`, parte JVM de T-A41 con una curva sintética) y `ULP_S = 8`
(`ulpDeEscrituraCubreLaIdaYVueltaMedida`); `CoherenciaRealTest` cubre T-A10 con los datos reales.
Tabla r1.1 (45):

| Clase (n.º) | Pruebas del plan |
| :--- | :--- |
| `FabricaTest` (1) | T-A01, T-A02 |
| `ReceptorTest` (5) | T-A03 |
| `CalculoTest` (31) | T-A03 (2), T-A05 (6), T-A06 (2, compartidas con T-A05), T-A10 (8), T-A11 (1), T-A13 (5), T-A17 (1), T-A18 (2), T-A36 (1), T-A37 (1), T-A08 (1), T-A09 (1), `emulacionFloat32ConcuerdaConDoubleSalvoRedondeo` (T-A04 en parte: `float` frente a `double`, no frente a XC8) y `versionConMascaraYOrdenE` (lógica de T-C03 y T-C09: análisis de `#V` con máscara y de `#E`) |
| `AsistenteTest` (8) | T-A12 (2), T-A13 (1), T-A14 (3), T-A36 (1), T-A16 (1) |

---

## 5. Lo que falta (r1.2)

**La lista de antes de grabar (r1.1) ya no aplica: se grabó.** Quedan G1 punto 5 (citar `8860445` en
`CAMBIOS`), G3 imposible y G5 en curso. Los huecos que quedan, separados en los que **bloquean la
calibración (P8)** y los que **bloquean una app de producción**, están en
[`MATRIZ-SPEC-codigo-V3.6.md`](MATRIZ-SPEC-codigo-V3.6.md) §5. En pruebas, lo mínimo antes de escribir
un coeficiente en SLV-002:

1. **T-C37** (qué firmware lleva, y que vuelve a `DEF` con PIN `2026`).
2. **T-C38** (N de asentamiento) y **T-A39** (regla del descolgado, C-46).
3. **T-A41** en MDB (grado de la curva del código `2`, P-16).
4. C-40 decidida (qué serie es P24) y C-41 (P32).
5. T-A32 (restauración automática) y T-A17 (acta), que hoy no existen como prueba.
6. C-47 cerrada por la revisión de arquitectura: o el rodeo `ULP_S = 8` + `#E` sustituye a C1, o hace falta la conversión exacta (P-10).

*Texto r1.1 de esta sección, retirado:* «Lo que falta antes de grabar: G1 (T-A28)…; G2 (T-A20)…; G3:
T-B03, T-B07, T-B09 (10 repeticiones), T-B10…; G4 preparado: que la app compare con 2 ulp…; G5…;
deseable antes de grabar: T-A21, T-A22, T-A27 completa, T-A29, T-A33 y T-A34.»

## 6. Contradicciones

Están en `SPEC-V3.6.md` §9 (C-08 a C-47), con su decisión o como abiertas. Las que afectan a este plan:
C-12 (tolerancia; T-C04; **cerrada por medida en r1.2**), C-18 (línea base; moot, original borrado),
C-19 (**cerrada**), C-27 (letra de C2), C-32 (md5 y fin de línea; T-A28), C-34 (**cerrada en el
firmware 3.6.1**) y las nuevas: C-36 (qué lleva SLV-002; T-C37), C-37 (versión por fecha; T-C37),
C-38 (asentamiento; T-C38), C-39 (XI desordenados; T-C28), C-40 (P24; T-A40), C-41 (P32; T-C39), C-42
(rangos de `x`; T-A41, T-C39), C-43 (umbrales de cobertura; T-C39), C-44 (T-A23 "cerrada"), C-46
(regla del descolgado; T-A39) y C-47 (C1 frente al rodeo de la app; revisión de arquitectura).
