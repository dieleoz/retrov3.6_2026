# TDD-V3.6 — Plan de pruebas ejecutable del firmware y la app V3.6
Mapa: `ARQUITECTURA.map` §M4 (compilación y pruebas) y §M3 (cadena documental).
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

**Recuento r1.3 (19-sep-2026, tarde): 156 pruebas.** 71 de nivel A, 12 de nivel B, 49 de nivel C y
24 de nivel S (app contra el simulador del equipo, receta nueva R-SIM). Las nuevas son T-A50 a T-A71,
T-S00 a T-S23 y T-C41 a T-C49 (§3 ter): el banco guiado y "Calibrar este equipo", con AT-01 a AT-22 del
QA incorporados. **Ninguna existe todavía.** Pruebas JVM de la app 3.6.9: 89, en verde según la matriz
al día (`MATRIZ-SPEC-codigo-V3.6.md`), ejecutadas desde `app/`.

*Recuento r1.2:* **101 pruebas.** 49 de nivel A (sin equipo), 12 de nivel B (equipo sin grabar) y 40 de
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

## 3 ter. Fichas nuevas de la revisión 1.3: banco y "Calibrar este equipo"

**Ninguna de estas pruebas existe todavía, y todas están PENDIENTES.** Cubren RF-APP-33 a RF-APP-48
(`SPEC-V3.6.md` §3.7 quater) y RF-CAL-35 a RF-CAL-43 (`SPEC-Calibracion-V3.6.md` §12). Incorporan los
casos de aceptación AT-01 a AT-22 de `QA-Flujo-Calibracion-V3.6.md` §4 (correspondencia en §3 ter.4) y
los negativos que pidió el encargo: corte de Bluetooth durante `#S`, re-medida no conforme, otra serie,
sin OSCURO y batería baja. Formato ISTQB: ID, requisitos, nivel, receta, estado, precondición, pasos,
resultado esperado y criterio de pasa o falla. **Evidencia obligatoria en todas:** el registro de tramas
de la ejecución y, si hay ZIP o acta, su md5.

Niveles: **A** sin equipo (JVM o teléfono solo), **S** app contra el simulador del equipo, **C** con
SLV-002 u otro V3 con la 3.6.2.

### 3 ter.1 Recetas nuevas

**R-SIM — app contra el simulador del equipo** (quien: agente o técnico de app). Hace falta una clase
`EnlaceSimulado`, que es parte de la implementación (fase 3) y no existe hoy. Implementa en memoria el
protocolo de la 3.6.2 (`PROTOCOLO-V3.6.md` §3):

- `#V#`, `#G`, `#GT#`, `#S` con el criterio de `curvaValida`, `#E`, `#F`, `#FT#`, `#L` (5 intentos),
  `#Q#`, caducidad de 10 min, `#SN`/`#GN#`, `#SC`/`#GC#`, `e`, los 12 códigos en float32 y `9`;
- un "patrón colocado" que fija la `x` verdadera; cada colocación suma un desvío N(0 ; 2,24 %) y cada
  disparo, N(0 ; 0,25 %);
- **inyección de fallos**: cortar el enlace tras la trama N o entre `#S` y `#OK#`, alterar la respuesta
  de `#E`, no responder, fijar la tensión que devuelve `9`, y desplazar la `x` a mitad de sesión.

Se inyecta en una variante de depuración del APK (nunca en la de campo) y se usa en emulador o en
teléfono. **Antes de usarlo, T-S00 lo valida contra un registro real.**

**R-APK — teléfono sin equipo** (quien: técnico). Instalar, actualizar y desinstalar el APK, y mirar
nombres de fichero y pantallas. Evidencia: capturas y listado de `Download/RTV/`.

### 3 ter.2 Nivel A

**T-A50 — Cola del banco.** RF-APP-33, RF-CAL-35 · A · R-JVM · **PENDIENTE**
- Pre: `06_Calibracion/cola_banco_P1-P132.csv` (md5 `9ddb7882…`) en los recursos de prueba.
- Pasos: (a) cargar la cola; (b) cargarla con un byte cambiado; (c) recorrerla simulando OK en cada
  paso, saltando P56 (AJUSTE) y P35 (VERIFICACIÓN); (d) intentar saltar un paso OSCURO y uno A5.
- Esperado: (a) 180 pasos, 133 `PATRON` con los 133 IDs del catálogo, 13 con K = 5 y 120 con K = 3 (P10-C3: P81 incluido), los 6 OSCURO a K = 5,
  todos con M = 4 y asentamiento 1; (b) "Cola no admitida", 0 pasos; (c) Σ K·(M + 1) `e` de los pasos
  medidos, y P56 y P35 marcados como saltados; (d) no hay salto posible.
- Pasa: todo. Además, `calibrable(1)` = falso mientras P56 esté saltado.

**T-A51 — Deriva de la sesión.** RF-CAL-36 · A · R-JVM · **PENDIENTE**
- Pasos: A5 del inicio a 5 × 4 y del final a 3 × 4 con x sintéticas: (a) desplazamiento del +1,0 % en
  los tres patrones; (b) del +2,5 %; (c) +3 %, −3 % y 0 % (sin desplazamiento común); (d) OSCURO de
  565 a 574.
- Esperado: umbral de D = ±1,9 % con s_rep = 2,24 %; (a) sin deriva; (b) **sesión con deriva**, y sus
  series fuera del ajuste mientras no haya nota; (c) D = 0, sin deriva, y cada patrón con su d en el
  resumen; (d) OSCURO con deriva (9 > 5 cuentas).
- Pasa: los cuatro.

**T-A52 — Método por código fijado.** RF-CAL-37 · A · R-JVM · **PENDIENTE**
- Pasos: leer la tabla del APK; pedir la propuesta de cada código con la campaña del 19-sep.
- Esperado (tabla RF-CAL-37 corregida tras P10-C2, que sigue a
  `06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md` §6): 1 → grado 1 con dispensa
  RF-CAL-14/15/16, sin reescribir; 2 → recta anclada con dispensa RF-CAL-14/15 y re-medida P25, sin
  reescribir; 3, 4 y 6 → grado 1; 5 → sólo verificar (o recta anclada si la tabla lo dice, PA-14);
  8 → **recta anclada** con re-medida P43 y sin dispensa; b → **recta anclada** con re-medida P49 si la
  tabla trae la conformidad de PA-24, y sólo verificar si no; 7, a, c y d → sólo verificar. La interfaz de Operación no expone ningún método (0 controles, revisión de código).
- Pasa: la tabla manda en los 12 códigos.

**T-A53 — s_rep medida, nunca supuesta.** RF-CAL-38, RF-APP-38 · A · R-JVM · **PENDIENTE**
- Pasos: evaluar una re-medida (a) con K = 1 y sin A5 en la campaña; (b) con K = 1 y la A5 del 19-sep;
  (c) con K = 5.
- Esperado: (a) **bloqueada**: "falta s_rep medida"; (b) s_rep = 2,24 % (A5); (c) s_rep de las 5
  colocaciones. En ningún caso aparece "supuesta" en el texto.
- Pasa: los tres. Con la 3.6.9 falla (a): `Acta.java:153` usa `sRepRelPorDefecto`.

**T-A54 — Re-medida: tres comprobaciones con los datos del 19-sep.** RF-CAL-39, RF-APP-38 · A · R-JVM · **PENDIENTE**
- Pre: curvas `#G` del acta del 19-sep (código 1: `2.98471571E-01`, `-1.62263885E+02`; código 2:
  `3.65483810E-01`, `-2.06628295E+02`) y los pares `e`/código de T4 (líneas 2156, 2234 y 2762 y sus
  disparos).
- Pasos: evaluar (a) el intento de las 12:16:32-12:17:02 sobre "P28" (T4:2080-2156); (b) el de las 12:17:36; (c) P5; (d) P28
  con R desplazada al +12 % del certificado; (e) P28 con la `x` al −8 % de la del banco.
- Esperado: (a) **colocación no válida** por los dos filtros de P10-C1: el asentamiento da 592, fuera de
  [0,8 ; 1,2]·2277,9 ("patrón presente"), y el par 6 da |d| = 462,3 > máx(3 ; 2 %·R) ("coherencia por
  par"); no cuenta como NO CONFORME ni gasta la repetición. *Texto de `80a0a97`, retirado:* "falla
  la comprobación 1 (d̄ = +50,7)"; con la fórmula de entonces pasaba (C-P10-1). Además, en (b) ningún
  |d_i| pasa de 4,5; (b)
  conforme: frente al certificado +2,9 % y `x` −2,9 % dentro de 2·s_rep·√(1/K_rem + 1/K_banco);
  (c) conforme: −6,3 % y +0,1 %; (d) NO CONFORME por la comprobación 3; (e) NO CONFORME por la 2.
- Pasa: los cinco.

**T-A55 — El acta guarda todos los intentos.** RF-APP-38 (D-20) · A · R-JVM · **PENDIENTE**
- Pasos: registrar en el acta del código 1 una colocación inválida, una re-medida NO CONFORME y una
  conforme; intentar una tercera re-medida válida.
- Esperado: el texto del acta lista los tres intentos con su hora y resultado; la tercera re-medida
  válida se rechaza ("máximo una repetición").
- Pasa: todo. Con la 3.6.9 falla: `Acta.java:133` sobrescribe.

**T-A56 — Cobertura con P1-P132 y recta anclada.** RF-APP-42 · A · R-JVM · **PENDIENTE**
- Pasos: `Asistente.cobertura` con el catálogo P1-P132; después, la cobertura de la recta anclada del 5
  y del b con el ancla en x = 565,4.
- Esperado: 1, 2, 3, 4, 6 y 8 hasta grado 2; b hasta grado 1; 5, 7, a, c y d "sólo verificar"
  (`SPEC-Calibracion-V3.6.md` §12.1). Con el ancla: 5 ajustable (rango 0-102), b ajustable (0-81) y **d ajustable (0-73, 5 niveles)**; que el d
  lo sea o no lo decide la tabla RF-CAL-37 (PA-14, C-P10-7).
  Café y lila no aparecen en la cobertura del 4 (16 patrones, no 26).
- Pasa: todo.

**T-A57 — Batería: de n a V, y umbrales.** RF-APP-43, RF-CAL-41 · A · R-JVM · **PENDIENTE**
- Pasos: convertir `:40:`, `:19:`, `:15:`, `:5:`, `:0:`, `:177:` y un tiempo de espera sin respuesta.
- Esperado: 40 → 10,77 V, sin aviso; 19 → 10,54 V, sin aviso; 15 → 10,50 V, aviso; 5 → "10,39-10,44 V",
  aviso; 0 → "< 10,34 V", aviso y **bloqueo de escrituras**; 177 → "≥ 12,28 V (posible recorte)"; sin
  respuesta → bloqueo de escrituras. En ningún caso se bloquea la medida.
- Pasa: los siete.

**T-A58 — Vencimiento.** RF-CAL-42 · A · R-JVM · **PENDIENTE**
- Pasos: `Calibracion.estado` con (`2025-09-18`, `CAL`, hoy `2026-09-19`), (`2026-09-19`, `CAL`, hoy
  `2026-09-19`) y (`2024-02-29`, `CAL`, hoy `2025-03-01`); y el estado de los botones con cada una.
- Esperado: "Calibración vencida (calibrado 2025-09-18, venció 2026-09-18)"; "Calibrado 2026-09-19,
  vence 2027-09-19"; vencida el 2025-02-28. **Todas las acciones de medida y de calibración siguen
  habilitadas.**
- Pasa: todo.

**T-A59 — Serie: cotejo y doble entrada.** RF-APP-35 (AT-06) · A · R-JVM · **PENDIENTE**
- Pasos: (a) `#GN#` = `SLV-002`, BT `COVIANDINA_SLV-002`, campaña SLV-002; (b) `#GN#` = `SLV-003`;
  (c) alta con `SLV-02` y `SLV-002`; (d) alta con `SLV-02` dos veces, sin casilla; (e) con casilla y sin
  nota; (f) con casilla y nota.
- Esperado: (a) identidad válida; (b) no válida, con el motivo; (c) 0 `#SN`; (d) y (e) 0 `#SN`; (f) 1
  `#SN,SLV-02#` y relectura `#GN#`, con la serie anterior y la nota en el diario.
- Pasa: los seis.

**T-A60 — Calibración en curso, persistente.** RF-APP-36 (D-02; AT-11) · A · R-JVM · **PENDIENTE**
- Pasos: estado "código 1 escrito, sin re-medida" con acta y conformidades; guardar; simular
  `reiniciar()`; volver a cargar; pedir `motivoNoEscribir('8')`.
- Esperado: el estado cargado es idéntico al guardado (igualdad de todos los campos); `reiniciar()` no lo
  borra; `motivoNoEscribir('8')` = "falta la re-medida del código 1". El fichero es de sólo añadir: cada
  versión es prefijo de la siguiente.
- Pasa: todo. Con la 3.6.9 falla: `Sesion.java:130`.

**T-A61 — P9-B12, la lógica.** RF-APP-37 (AT-09) · A · R-JVM · **PENDIENTE**
- Pasos: con enviado = E, anterior = A y leído = L por `#G`: (a) L = E en 8 ulp; (b) L = A; (c) L
  distinto de los dos; (d) sin respuesta a `#G`.
- Esperado: (a) `#E` en 5 puntos y re-medida pendiente; (b) "el `#S` no entró": ofrecer repetirlo; (c)
  restaurar A y releer; (d) no hacer nada más que reconectar, y bloquear cualquier `#S`.
- Pasa: las cuatro decisiones.

**T-A62 — Nombre del APK.** RF-APP-41 (AT-20) · A · compilación con la skill `compilar-apk` · **PENDIENTE**
- Pasos: compilar la 3.6.10 y listar la carpeta de entrega.
- Esperado: `RTV-V3.6.10-3610.apk`, con su md5 anotado; **no** existe `RTV-V3.6.apk`.
- Pasa: las dos cosas.

**T-A63 — Acta: campos obligatorios.** RF-CAL-43 · A · R-JVM · **PENDIENTE**
- Pasos: generar el acta con todos los campos; después, quitar uno a uno el SHA-256 del ZIP, el md5 de la
  cola, la A5, la s_rep, la `x` del OSCURO, la batería, el `#V#` posterior, la persistencia y el PIN.
- Esperado: completa → aceptable; a cada falta, "no aceptable: falta <campo>". Un dato desconocido sale
  como "no conocido", nunca como hueco.
- Pasa: 10 de 10.

**T-A64 — Código por fila; café y lila.** RF-APP-47, RF-CAL-40 · A · R-JVM · **PENDIENTE**
- Pasos: cargar la cola y construir los puntos del ajuste del 4 con una campaña sintética que mida todo.
- Esperado: P68, P69, P83, P84, P98, P99, P113, P114, P128 y P129 se evalúan con la curva del 4 y
  aparecen en la verificación del 4; en el ajuste del 4 hay 16 puntos, todos rojos.
- Pasa: todo.

**T-A65 — Oscuro y s_rep de la campaña.** RF-APP-48 (D-11) · A · R-JVM · **PENDIENTE**
- Pasos: construir la propuesta del código 2 con la campaña del 19-sep (S060 y A5 S057-S059).
- Esperado: el ancla es x = 565,4, no 575; la s_rep es 2,24 % "(A5)"; el texto del oscuro de las
  tarjetas usa 565,4.
- Pasa: todo.

**T-A66 — Estado con `#V#` fresco.** RF-APP-44 (D-18) · A · R-JVM · **PENDIENTE**
- Pasos: sesión con `#V,3.6,2026-09-19,DEF,0000#`; simular `#S,1` y `#S,2` y la respuesta posterior
  `#V,3.6,2026-09-19,CAL,0003#`; generar la cabecera y el acta.
- Esperado: la cabecera y el acta dicen `CAL mascara 0003` y "Calibrado 2026-09-19, vence 2027-09-19".
  Sin el `#V#` posterior, el acta no se puede aceptar.
- Pasa: todo.

**T-A67 — Protocolo homogéneo.** RF-CAL-35, P9-B3 (AT-18) · A · R-JVM · **PENDIENTE**
- Pasos: campaña con series 1 × 9 importadas y series 3 × 4 nuevas del mismo código.
- Esperado: "Protocolos distintos en la campaña: M = 9 y M = 4". El código no se puede calibrar. **K
  distinto con M igual (3 × 4 y 5 × 4) sí se admite**: es el diseño del banco.
- Pasa: las dos cosas.

**T-A68 — Importador.** RF-APP-39 (AT-02, AT-03) · A · R-JVM · **PENDIENTE**
- Pasos: (a) importar el ZIP de las 12:27 en una campaña vacía de SLV-002; (b) reimportarlo; (c)
  importarlo en una campaña de otra MAC.
- Esperado: (a) N series importadas; (b) "0 importadas, N ya estaban"; (c) "La campaña es de otro
  equipo… no se importa nada" y la campaña sin cambios (`ImportadorCampana.java:68-81`).
- Pasa: los tres.

**T-A69 — Actualizar la app instalando encima.** RF-APP-40 (AT-19) · A · R-APK · **PENDIENTE**
- Pre: la 3.6.9 con una campaña de 3 series; APK 3.6.10 firmado con la misma clave (PA-22).
- Pasos: instalar la 3.6.10 encima; abrir.
- Esperado: la campaña sigue, con las 3 series.
- Pasa: 3 de 3 series. Si la firma cambia, la prueba falla y se anota (D-09).

**T-A70 — Versión visible.** RF-APP-41 (AT-20) · A · R-APK · **PENDIENTE**
- Pasos: capturar las pantallas de Conexión, Pruebas, Campaña, Calibrar y Avanzado; exportar un ZIP y un
  acta.
- Esperado: `3.6.10` visible en las 5 capturas; `resumen.txt` y el acta la llevan.
- Pasa: 5 de 5, y los 2 ficheros.

**T-A71 — No desinstalar y copia fuera de la app.** RF-APP-40 · A · R-APK · **PENDIENTE**
- Pasos: (a) medir 3 series sin exportar y reabrir la app; (b) exportar; (c) desinstalar y reinstalar.
- Esperado: (a) el aviso "Hay 3 series sin exportar. No desinstale la app…"; (b)
  `Download/RTV/campana_SLV-002_<fecha>.zip` con el mismo SHA-256 que el compartido; (c) ese fichero
  sigue en `Download/RTV/`.
- Pasa: los tres.

### 3 ter.3 Nivel S (simulador)

**T-S00 — El simulador reproduce un registro real.** Requisito de R-SIM · S · R-SIM · **PENDIENTE**
- Pasos: reenviar al simulador las tramas TX de T4 (`tramas/rtv36_20260919_114644.txt` del ZIP de las
  12:27), con su estado inicial (`#V,…,DEF,0000#`, fábrica, `#GN,NONE#`).
- Esperado: las respuestas a `#V#`, `#GC#`, `#GN#`, `#L`, `#S`, `#G`, `#E`, `#SN`, `#SC` y `#Q#`
  coinciden byte a byte con las RX de T4. Las de `e` y códigos no se comparan: dependen del patrón.
- Pasa: 100 % de las respuestas deterministas. Si falla, no se usa el simulador para nada más.

**T-S01 — Fase A completa.** RF-APP-33, RF-CAL-35 (AT-01) · S · R-SIM · **PENDIENTE**
- Pre: `#GN#` = serie del nombre BT; sin campaña.
- Pasos: "Medir el banco" y seguir la cola hasta el final, pulsando OK en cada paso.
- Esperado: calentamiento, OSCURO y A5 al principio de cada sesión, y A5 y OSCURO al final; los 133
  patrones en el orden del CSV; `e` = Σ K·(M + 1); `9` sólo en los pasos BATERIA; al final de cada
  sesión, un ZIP con SHA-256 y copia en `Download/RTV/`. Toques = pasos de la cola + colocaciones.
- Pasa: todo, con el recuento de toques anotado.

**T-S02 — Importar, visible.** RF-APP-39 (AT-02, AT-03) · S · R-SIM · **PENDIENTE**
- Pasos: abrir Campaña con la campaña vacía; tocar "Importar ZIP" sin desplazarse; elegir el ZIP de las
  12:27; repetir con un ZIP de otra serie.
- Esperado: el botón es el primero y está visible; "N series importadas"; con el de otra serie, nada
  importado y el aviso.
- Pasa: todo.

**T-S03 — Equipo de otra serie (negativo).** RF-APP-35 (AT-04) · S · R-SIM · **PENDIENTE**
- Pre: campaña de SLV-002. Simulador con `#GN,SLV-003#` y, en otra pasada, con otra MAC.
- Pasos: conectar; pulsar "Calibrar este equipo".
- Esperado: botón deshabilitado con "Este equipo es SLV-003; la campaña es de SLV-002". **0 tramas
  `#L`, `#S`, `#SN`** en el registro.
- Pasa: 0 tramas en las dos pasadas.

**T-S04 — Serie sin grabar.** RF-APP-35 (AT-05) · S · R-SIM · **PENDIENTE**
- Pasos: conectar con `#GN,NONE#`.
- Esperado: "Dar de alta la serie"; "Calibrar" deshabilitado hasta darla.
- Pasa: las dos cosas.

**T-S05 — Error de tecleo en la serie.** RF-APP-35 (AT-06) · S · R-SIM · **PENDIENTE**
- Pasos: Avanzado → Serie: (a) `SLV-02` / `SLV-002`; (b) `SLV-02` dos veces.
- Esperado: (a) "Las dos entradas no coinciden", 0 `#SN`; (b) pide casilla (difiere del BT) y nota, y
  muestra "SLV-002 → SLV-02" en grande.
- Pasa: las dos.

**T-S06 — Campaña sin OSCURO (negativo).** RF-APP-48, RF-CAL-37 (AT-07) · S · R-SIM · **PENDIENTE**
- Pre: la campaña completa, sin ninguna serie OSCURO.
- Pasos: "Calibrar este equipo".
- Esperado: las tarjetas del 2, del 8 y del b (recta anclada) dicen "no calculable: falta OSCURO", sin
  casilla; sólo el 1 se puede calibrar (corregido tras P10: con el 8 anclado, AT-07 queda así);
  **0 tramas `#S,2`, `#S,8` ni `#S,b`**.
- Pasa: todo.

**T-S07 — Fase B completa.** RF-APP-34, 36, 44, 45, 46, RF-CAL-39, RF-CAL-43 (AT-08) · S · R-SIM · **PENDIENTE**
- Pre: campaña completa con OSCURO y A5; APTO; batería con n = 60.
- Pasos: "Calibrar" → casillas y nota → secuencia → "Apague y encienda" (el simulador reinicia sin
  perder la EEPROM) → "Aceptar y grabar fecha".
- Esperado, en este orden en el registro: `9`; `#L`; por código, `9`, `#S`, `#G`, 5 `#E`, `#V#` y la
  re-medida conforme, **antes del siguiente `#S`**; reconexión con `#V#`, un `#G` por código escrito y 5
  `#E` por código; `#SC` y `#GC#` **después** de Aceptar y una sola vez. Acta con `CAL` y la máscara,
  oscuro en la `x` medida, s_rep "(A5)", conformidad por código, SHA-256 del ZIP, A5, batería y todos
  los intentos. ZIP con el acta. Pantalla de Operación con ≤ 8 controles y 0 acciones destructivas.
- Pasa: todo.

**T-S08 — Corte de Bluetooth durante `#S` (negativo).** RF-APP-37 (AT-09) · S · R-SIM · **PENDIENTE**
- Pre: Fase B en el código 8, con el código 1 ya verificado.
- Pasos: cortar el enlace tras enviar `#S,8` y antes de `#OK#`, en tres variantes: (a) el simulador
  aplica el `#S`; (b) no lo aplica; (c) deja una curva que no es ni la enviada ni la anterior.
  Reconectar.
- Esperado: al reconectar, "Calibración a medias: código 8" y `#G,8#` antes que nada. (a) 5 `#E` y
  re-medida; (b) ofrece repetir `#S,8`; (c) restaura lo anterior y relee. **0 `#S` de otro código**
  entre el corte y la resolución. El acta conserva el código 1 y su re-medida.
- Pasa: las tres variantes.

**T-S09 — Corte durante la re-medida.** RF-APP-36, 38 (AT-10) · S · R-SIM · **PENDIENTE**
- Pasos: con el código 1 escrito y verificado por `#E`, cortar en la colocación 3 de 5 y reconectar.
- Esperado: la re-medida interrumpida no cuenta y queda anotada; se ofrece repetirla completa; `#S` de
  otro código bloqueado hasta entonces.
- Pasa: todo.

**T-S10 — Cierre forzado de la app.** RF-APP-36 (AT-11) · S · R-SIM · **PENDIENTE**
- Pasos: con el código 1 escrito y sin re-medida, forzar el cierre; abrir; conectar.
- Esperado: reanuda en la re-medida del código 1, con el acta y la conformidad intactas.
- Pasa: todo.

**T-S11 — `#E` no reproduce.** RF-CAL-22 (AT-12) · S · R-SIM · **PENDIENTE**
- Pasos: el simulador altera una de las 5 respuestas de `#E,1,…#` en +3 unidades.
- Esperado: "`#E` no reproduce la curva"; restauración del estado anterior con relectura; secuencia
  detenida; el código 1 no entra en el acta como escrito.
- Pasa: todo.

**T-S12 — Re-medida no conforme (negativo).** RF-APP-38, RF-CAL-39 (AT-13) · S · R-SIM · **PENDIENTE**
- Pasos: tras escribir el código 1, el simulador "coloca" un patrón con R verdadera al +15 % del
  certificado de P28, en la re-medida y en su repetición.
- Esperado: NO CONFORME (comprobación 3); se ofrece **una** repetición; al fallar la segunda, se
  restaura el estado anterior del código 1 (`#F,1#` si era fábrica) y se detiene. Las dos re-medidas
  están en el acta. "Aceptar" no admite el código 1. No se ofrece una tercera.
- Pasa: todo.

**T-S13 — Oscuro fuera de límite.** P9-B13 (AT-14) · S · R-SIM · **PENDIENTE**
- Pasos: campaña en la que la curva del 1 da R = 45 en el oscuro (límite 34,7).
- Esperado: la tarjeta del 1 dice "no escribible: oscuro 45 > 34,7 (P9-B13)", sin casilla; 0 `#S,1`.
- Pasa: todo.

**T-S14 — Batería baja (negativo).** RF-APP-43, RF-CAL-41 (AT-15, reescrita) · S · R-SIM · **PENDIENTE**
- Pasos: (a) `9` responde `:15:` al empezar la sesión; (b) `:0:` antes de la Fase B; (c) sin respuesta
  a `9` antes de un `#S`; (d) `:40:` de nuevo.
- Esperado: (a) aviso "Batería baja: 10,50 V", y la medida sigue; (b) y (c) "Calibrar" bloqueado y
  **0 tramas `#S`, `#F`, `#SC`, `#SN`, `#FT`**; (d) se desbloquea. Cada serie del diario lleva la V.
  Ningún `9` dentro de una serie.
- Pasa: los cuatro. **AT-15 del QA suponía que la app no podía leer la batería**; con la 3.6.2 sí puede
  (C-CAL-17).

**T-S15 — Caducidad del modo administrador.** RF-FW-17 (AT-16) · S · R-SIM · **PENDIENTE**
- Pasos: dejar la app 10 min en "Coloque P28" sin tramas `#` y pulsar OK.
- Esperado: la app renueva `#L` antes de medir, o pide el PIN. No se repite ningún `#S` ya verificado.
- Pasa: las dos cosas.

**T-S16 — Rechazo del acta.** RF-APP-34 (AT-17) · S · R-SIM · **PENDIENTE**
- Pasos: tras la Fase B, "Rechazar" con motivo.
- Esperado: 0 `#SC`; se ofrece restaurar lo escrito al estado anterior; acta RECHAZADA, guardada y en el
  ZIP.
- Pasa: todo.

**T-S17 — Protocolo mezclado.** RF-CAL-35 (AT-18) · S · R-SIM · **PENDIENTE**
- Pasos: importar la campaña 1 × 9 del 19-sep y medir el blanco a 3 × 4; "Calibrar".
- Esperado: "Protocolos distintos: M = 9 y M = 4"; no se sigue.
- Pasa: todo.

**T-S18 — Firmware 3.6.1.** RF-APP-34 (AT-21) · S · R-SIM · **PENDIENTE**
- Pasos: el simulador no responde a `#GC#`.
- Esperado: "Calibrar" deshabilitado: "Hace falta la 3.6.2 (serie y fecha de calibración)".
- Pasa: todo.

**T-S19 — Falta la A5.** RF-CAL-35 (AT-22) · S · R-SIM · **PENDIENTE**
- Pasos: campaña sin series A5.
- Esperado: "Calibrar" deshabilitado: "Falta la medida puente A5 (P9-A5)".
- Pasa: todo.

**T-S20 — `#V#` fresco.** RF-APP-44 (D-18) · S · R-SIM · **PENDIENTE**
- Pasos: Fase B de los códigos 1 y 2; exportar el ZIP.
- Esperado: `#V#` después de cada `#S` y antes del acta; acta y `resumen.txt` con `CAL mascara 0003` y
  "Calibrado 2026-09-19, vence 2027-09-19". Ninguna línea "No calibrado".
- Pasa: todo.

**T-S21 — Deriva detectada.** RF-CAL-36 · S · R-SIM · **PENDIENTE**
- Pasos: el simulador desplaza la `x` un +3 % entre la A5 del inicio y la del final de la sesión 2.
- Esperado: la sesión 2 queda "con deriva" (D ≈ +3 % > 2,1 %); sus series no entran en el ajuste sin la
  nota; el resumen lo dice.
- Pasa: todo.

**T-S22 — Calibración vencida.** RF-CAL-42 · S · R-SIM · **PENDIENTE**
- Pasos: el simulador responde `#GC,2025-09-18#`; hoy es 2026-09-19.
- Esperado: aviso de vencida en la cabecera; medir, banco y "Calibrar" siguen habilitados.
- Pasa: todo.

**T-S23 — Colocación inválida en la re-medida.** RF-CAL-39 · S · R-SIM · **PENDIENTE**
- Pasos: en la colocación 2 de la re-medida, el simulador da a `e` y al código `x` distintas en un 10 %
  (el equipo "se movió"), como a las 12:17:02 del 19-sep.
- Esperado: "colocación no válida: repítala"; se repite sólo esa colocación y queda registrada; la
  re-medida no cuenta como NO CONFORME.
- Pasa: todo.

### 3 ter.4 Nivel C (equipo)

**T-C41 — Persistencia y `#V#` en SLV-002.** RF-APP-46, RF-APP-44, P9-B8 (PA-23) · C · R-APP o R-TERM · **PENDIENTE**
- Pre: SLV-002 con el acta del 19-sep (códigos 1 y 2). **No se escribe nada.**
- Pasos: apagar, esperar 5 s y encender; `#V#`; `#G,1#`; `#G,2#`; `#E,1,x#` y `#E,2,x#` en x = 500,
  1000, 2000, 3000 y 4000; `#GN#`; `#GC#`.
- Esperado: `#V,3.6,2026-09-19,CAL,0003#`; `#G,1` y `#G,2` iguales al texto de las líneas 10 y 17 del
  acta; los 10 `#E` iguales a la curva emulada en float32 (±1); `#GN,SLV-002#`; `#GC,2026-09-19#`.
- Pasa: todo. Cierra la persistencia de P9-B8 y C-CAL-20 (al lado del acta, sin reescribirla).

**T-C42 — Sesión 1 del banco en SLV-002.** RF-APP-33, RF-CAL-35, 36 (AT-01) · C · R-APP (3.6.10) · **PENDIENTE**
- Pre: app 3.6.10; T-S01 pasa; batería cargada; superficie negra mate para el OSCURO.
- Pasos: sesión 1 de la cola (blanco y amarillo).
- Esperado: todas las series aceptadas o repetidas según `Veredicto`; A5 del inicio con s_rep; D de la
  sesión; ZIP. **Tiempo real anotado frente a los 74 min estimados** (`PLAN-Captura…` §2).
- Pasa: la sesión completa y exportada; el tiempo se anota, no es criterio.

**T-C43 — Fase B en el equipo.** RF-APP-34, RF-CAL-39 (AT-08) · C · R-APP (3.6.10) · **PENDIENTE**
- Pre: T-S07 pasa; banco completo con A5 conforme; Diego autoriza qué código se escribe (p. ej. el 8, que
  sigue de fábrica).
- Pasos: como T-S07, con el equipo real.
- Esperado: como T-S07. Re-medida con las tres comprobaciones y s_rep medida.
- Pasa: todo, con el registro y el acta archivados en `06_Calibracion/SLV-002/`.

**T-C44 — La orden `9` frente a un polímetro.** RF-APP-43, RF-CAL-41 (C-CAL-17) · C · R-TERM · **PENDIENTE**
- Pre: polímetro en bornes de la batería.
- Pasos: 5 veces `9`, con 3 s entre cada una, en tres estados de carga (recién cargada, a media sesión y
  al final).
- Esperado: `:<n>:` sin terminador; V_app = 10 + (n + 30,09)/90,91 y |V_app − V_polímetro| ≤ 0,1 V.
  Cada `9` dispara la lámpara.
- Pasa: 15 de 15 dentro de 0,1 V. Si no, se anota la diferencia y el umbral de PA-17 se corrige.

**T-C45 — La `x` frente a la tensión.** RF-CAL-41 (PA-17) · C · R-APP · **PENDIENTE**
- Pre: fuente de laboratorio en lugar de la batería, con autorización de Diego.
- Pasos: P28 a 3 × 4 con 12,5; 11,5; 11,0; 10,6 y 10,4 V; `9` en cada paso.
- Esperado: pendiente de la `x` en % por voltio, con su incertidumbre.
- Pasa: se fija el umbral de aviso de PA-17 como la V a la que la `x` se aparta más de s_rep/2 (1,1 %)
  del valor a 12,5 V.

**T-C46 — Corte real durante `#S` (negativo).** RF-APP-37 (AT-09) · C · R-APP (3.6.10) · **PENDIENTE**
- Pre: T-S08 pasa; Diego autoriza. Se usa el **código 8 con sus coeficientes de fábrica**: el peor caso
  lo deja en fábrica con el bit de la máscara a 1, y `#F,8#` lo devuelve a 0.
- Pasos: enviar `#S,8,<fábrica>#` y apagar el equipo antes de `#OK#`; encender y reconectar.
- Esperado: la app ejecuta P9-B12 (`#G,8#`, `#E`) antes que nada, y termina en fábrica con
  `#V#` coherente.
- Pasa: el código 8 queda en fábrica y la máscara vuelve a la de antes.

**T-C47 — Re-medida no conforme, en modo verificación (negativo).** RF-CAL-39 (AT-13) · C · R-APP (3.6.10) · **PENDIENTE**
- Pre: SLV-002 con el código 1 escrito. **No se escribe nada.**
- Pasos: verificación del código 1 con "Coloque P28", colocando P27 (IX, 471).
- Esperado: NO CONFORME por la comprobación 2 (`x` −7 %, fuera de 2·s_rep·√(1/5 + 1/5) ≈ 2,8 %). Al
  ser verificación, no se restaura nada: sólo se registra.
- Pasa: NO CONFORME detectado y registrado.

**T-C48 — Otra serie, con el equipo (negativo).** RF-APP-35 (AT-04) · C · R-APP (3.6.10) · **PENDIENTE**
- Pasos: importar un ZIP de campaña de otra serie (SLV-003, de prueba) y pulsar "Calibrar".
- Esperado: deshabilitado, con el motivo; 0 tramas `#L`, `#S`, `#SN` en el registro.
- Pasa: 0 tramas.

**T-C49 — Batería baja con fuente de laboratorio (negativo).** RF-APP-43 (AT-15) · C · R-APP (3.6.10) · **PENDIENTE, opcional**
- Pre: fuente de laboratorio; autorización de Diego (riesgo de corte durante una escritura).
- Pasos: bajar a una V con n < 19 y después a n = 0; intentar "Calibrar".
- Esperado: aviso con n < 19; con n = 0, 0 tramas de escritura.
- Pasa: las dos cosas. Si no hay fuente, basta T-S14.

### 3 ter.5 AT-01 a AT-22 del QA → pruebas

| AT | Pruebas | | AT | Pruebas |
| :--- | :--- | :--- | :--- | :--- |
| AT-01 | T-S01, T-C42 | | AT-12 | T-S11 |
| AT-02 | T-A68, T-S02 | | AT-13 | T-A54, T-S12, T-C47 |
| AT-03 | T-A68, T-S02 | | AT-14 | T-S13 |
| AT-04 | T-S03, T-C48 | | AT-15 | T-A57, T-S14, T-C44, T-C49 (reescrita: la app sí lee la batería) |
| AT-05 | T-S04 | | AT-16 | T-S15 |
| AT-06 | T-A59, T-S05 | | AT-17 | T-S16 |
| AT-07 | T-S06 | | AT-18 | T-A67, T-S17 |
| AT-08 | T-S07, T-C43 | | AT-19 | T-A69 |
| AT-09 | T-A61, T-S08, T-C46 | | AT-20 | T-A62, T-A70 |
| AT-10 | T-S09 | | AT-21 | T-S18 |
| AT-11 | T-A60, T-S10 | | AT-22 | T-S19 |

### 3 ter.6 Requisito → pruebas (r1.3)

| Requisito | Pruebas |
| :--- | :--- |
| RF-APP-33 | T-A50, T-S01, T-C42 |
| RF-APP-34 | T-S07, T-S16, T-S18, T-C43 |
| RF-APP-35 | T-A59, T-S03, T-S04, T-S05, T-C48 |
| RF-APP-36 | T-A60, T-S09, T-S10 |
| RF-APP-37 | T-A61, T-S08, T-C46 |
| RF-APP-38 | T-A53, T-A54, T-A55, T-S12, T-S23 |
| RF-APP-39 | T-A68, T-S02 |
| RF-APP-40 | T-A69, T-A71 |
| RF-APP-41 | T-A62, T-A70 |
| RF-APP-42 | T-A56 |
| RF-APP-43 | T-A57, T-S14, T-C44, T-C49 |
| RF-APP-44 | T-A66, T-S20, T-C41 |
| RF-APP-45 | T-S07 (revisión de interfaz) |
| RF-APP-46 | T-S07, T-C41 |
| RF-APP-47 | T-A64 |
| RF-APP-48 | T-A65, T-S06 |
| RF-CAL-35 | T-A50, T-A67, T-S01, T-S17, T-S19, T-C42 |
| RF-CAL-36 | T-A51, T-S21, T-C42 |
| RF-CAL-37 | T-A52, T-S06 |
| RF-CAL-38 | T-A53 |
| RF-CAL-39 | T-A54, T-S12, T-S23, T-C43, T-C47 |
| RF-CAL-40 | T-A64 |
| RF-CAL-41 | T-A57, T-S14, T-C44, T-C45, T-C49 |
| RF-CAL-42 | T-A58, T-S22 |
| RF-CAL-43 | T-A63, T-S07 |

**Cobertura:** los 16 RF-APP y los 9 RF-CAL nuevos tienen al menos una prueba de nivel A o S, que se
puede hacer sin equipo. Los que además necesitan equipo son la persistencia (T-C41), la batería real
(T-C44, T-C45) y el banco y la Fase B reales (T-C42, T-C43). **Orden para la fase 4 (QA de la APK):**
nivel A completo → T-S00 → nivel S → T-C41 → T-C44 → T-C42 → resto de C.

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

---

## 7. App única (V3.6 y V4.6)

**Añadido 19-sep-2026, tarde. Ninguna de estas pruebas existe todavía, y ninguna trama V4 se ha visto
en un terminal serie.** Requisitos: [`SPEC-App-Unica-V36-V46.md`](SPEC-App-Unica-V36-V46.md)
(RF-APP-U01 a U14). Firmwares F-2020, F-36, F-40, F-41 y F-46 como en su §1. Recetas: R-JVM (nivel A),
R-SIM (nivel S, simulador del equipo con un perfil por firmware) y R-TERM / R-APP (nivel C). En nivel
S, el simulador V4.1 debe reproducir **el bloqueo por `@` sin `LEERV`, el `0x00` tras `/n/r`, el
segundo de espera antes de medir y el `error` arrastrado** (`V4.1:Serial.c:31-40,87-100,182-188`,
`V4.1:Aplicacion.c:270-271,297-298`); si no, la prueba no vale.

### 7.1 Pruebas (T-U)

**T-U01 — Detección de F-36.** RF-APP-U01 · S · R-SIM · **PENDIENTE**
- Pasos: conectar al simulador F-36 3.6.2.
- Esperado: tramas `#V#`, `#GC#`, `#GN#` y ninguna más; versión V3.6 3.6.2.
- Pasa: registro idéntico a lo esperado.

**T-U02 — Detección de F-46.** RF-APP-U01 · S · R-SIM · **PENDIENTE**
- Pasos: simulador F-46 que responde `#V,4.6,2026-10-01,CAL,0003#`.
- Esperado: sólo las tramas `#` de identidad; ninguna `@`, `9` ni `6`; `ProtocoloV46`.

**T-U03 — Detección de F-2020.** RF-APP-U01 · S · R-SIM · **PENDIENTE**
- Pasos: simulador F-2020 que calla a `#V#` y responde `:3:` a `9`.
- Esperado: `#V#`, `9`; ninguna `@`, ninguna `e`. Variante con `9` mudo y `::512` a `6`: `#V#`, `9`, `6`.

**T-U04 — Detección de V4 original.** RF-APP-U01, U04, U05 · S · R-SIM · **PENDIENTE**
- Pasos: simulador V4.1 que calla a `#V#`, `9` y `6`, y responde a la sonda a los 3,2 s con
  `@LEERV,212@/n/r` + `0x00`.
- Esperado: V4 original; `ProtocoloV4`; la espera de la sonda es ≥ 5 s. Con la app 3.6.14 falla
  (espera 3000 + 180 ms, `RTV:Pruebas.java:321`, `RTV:Receptor.java:14`).

**T-U05 — Nadie contesta.** RF-APP-U01, U03 · S · R-SIM · **PENDIENTE**
- Esperado: `#V#`, `9`, `6`, sonda y **nada más**; mensaje de RF-APP-U03 con la mención al módulo BT.

**T-U06 — Lista de lo que nunca se envía en la detección.** RF-APP-U01 · A · R-JVM · **PENDIENTE**
- Pasos: recorrer las 5 ramas de la detección con un `Enlace` falso que registra.
- Esperado: en ninguna rama aparece `e`, un `@` sin `LEERV`, ni un byte distinto de `9` y `6`.

**T-U07 — Perfil por MAC.** RF-APP-U02 · S · R-SIM · **PENDIENTE**
- Pasos: (a) MAC del V3-2 con perfil "sólo sonda"; (b) MAC con perfil F-36 y simulador V4.1.
- Esperado: (a) registro = sólo la sonda; (b) sin operar, "el equipo no responde como su perfil".

**T-U08 — Parser `@LEERV` estricto.** RF-APP-U05 · A · R-JVM · **PENDIENTE**
- Entradas → esperado: `@LEERV,BLA,1@` → nada (eco); `@LEERV,127@/n/r` + `0x00` → 127;
  `***** RETROREFLECTOMETRO VERTICAL *****\r\nOFFSET= 12.30\r\n\r\n@LEERV,5@/n/r` → 5;
  `@LEERV,127,45@` → rechazada; `@LEERV,@` → rechazada.
- Con la 3.6.14 falla el primero: `RTV:Tramas.java:40` acepta `[^@]*`.

**T-U09 — Restos entre respuestas.** RF-APP-U05 · S · R-SIM · **PENDIENTE**
- Pasos: dos medidas seguidas con el simulador V4.1.
- Esperado: la segunda respuesta no empieza por `/n/r` ni por `0x00`; el valor es el de la segunda.

**T-U10 — `permitida()` del V4 original.** RF-APP-U06 · A · R-JVM · **PENDIENTE**
- Entradas: las 12 `@LEERV` válidas; y `#V#`, `9`, `6`, `e`, `@VERS@`, `@?@`, `@LEERV@`,
  `@LEERV,BL,1@`, `@LEERV,BLA,3@`, `@LEERV,BLA,1@@`, `@leerv,BLA,1@`, `@LEERV,BLA,1@\r\n`.
- Esperado: sólo las 12 pasan, byte a byte.

**T-U11 — Sin `esV36` fuera de `Protocolo`.** RF-APP-U06 · A · revisión de código · **PENDIENTE**
- Esperado: `grep` de `esV36` y de `Version.V4` fuera de las implementaciones: 0 resultados.

**T-U12 — V4 original: medir sin calibrar.** RF-APP-U07 · S · R-SIM · **PENDIENTE**
- Pasos: sesión completa con el simulador V4.1: 12 medidas, banco de verificación, intento de abrir
  "Calibrar este equipo" y Avanzado.
- Esperado: 0 tramas `#`, `9` y `e`; los dos botones deshabilitados con "Equipo V4 sin firmware V4.6:
  sólo medir y verificar".

**T-U13 — Etiqueta del tipo 1.** RF-APP-U07 · A · R-JVM · **PENDIENTE**
- Esperado: una medida `BLA,1` de V4.1 lleva "señal tipo 1, cuentas/10, no es retrorreflexión" y no
  entra en ninguna comparación con certificado; en el perfil del V3-2, "tipo 1 sin interpretar".

**T-U14 — Diario del estado oculto.** RF-APP-U08 · A · R-JVM · **PENDIENTE**
- Pasos: (a) sonda `BLA,1` y luego `AMA,2`; (b) `AMA,2` como primera medida.
- Esperado: (a) la de `AMA,2` lleva "previa: BLA,1 = <n> (sonda)"; (b) "estado previo desconocido", y
  el resumen lo avisa.

**T-U15 — Mismo banco con dos protocolos.** RF-APP-U09 · S · R-SIM · **PENDIENTE**
- Pasos: el banco de 3 patrones con `ProtocoloV36` (simulador F-36) y con `ProtocoloV46` (F-46).
- Esperado: las mismas pantallas y el mismo número de toques; F-36 usa `e` y bytes, F-46 `#X` y
  `@LEERV`; ninguna trama del otro protocolo.

**T-U16 — Catálogo y coherencia en `DEF` por perfil.** RF-APP-U09 · A · R-JVM · **PENDIENTE**
- Esperado: con F-46 en `DEF` la coherencia usa `#E` frente al literal de `V4.1:Ecuaciones.c`, no
  `#G` a 1 ulp; el catálogo F-46 no tiene la columna `codigo_equipo` de la V3.6.

**T-U17 — Cola de verificación V4.** RF-APP-U10 · A · R-JVM · **PENDIENTE**
- Esperado: ninguna fila con `e` ni uso AJUSTE; su ZIP no se importa como campaña de ajuste.

**T-U18 — Serie declarada.** RF-APP-U11 · A · R-JVM · **PENDIENTE**
- Esperado: con F-36 3.6.1 y con V4 original, registros, ZIP y acta dicen "serie declarada, no leída
  del equipo"; con F-46 y 3.6.2, no.

**T-U19 — Campaña sin MAC.** RF-APP-U12 · A · R-JVM · **PENDIENTE**
- Esperado: `esDeEsteEquipo("00:21:13:00:00:01")` de una campaña con MAC vacía → falso. Con la
  3.6.14 falla (`Campana.java:186-188` en `HEAD`).

**T-U20 — V4.6 en blanco.** RF-APP-U13 · S · R-SIM · **PENDIENTE**
- Pasos: simulador F-46 con `#V,4.6,2026-10-01,DEF,0000#`, `#GN,NONE#`, `#GC,NONE#`.
- Esperado: no abre campaña hasta el alta de serie; 0 `#S`, `#SC`, `#F` antes; "sin fecha de
  calibración".

### 7.2 Aceptación (ISTQB)

Nivel C salvo que se diga. Cada caso se registra como en §0 (fecha, equipo, versión detectada, md5 del
APK, tramas en crudo). **AT-U05 y AT-U06 son encargos de medida**: su resultado cambia la SPEC.

| ID | Requisito | Precondición | Pasos | Resultado esperado |
| :--- | :--- | :--- | :--- | :--- |
| **AT-U01** | U01 | SLV-002 con V3.6.2, emparejado | Conectar con la app | Detecta V3.6 3.6.2; registro: `#V#`, `#GC#`, `#GN#` y nada más |
| **AT-U02** | U01 | Equipo con V3 de 2020 (si queda alguno) o simulador F-2020 | Conectar | Detecta V3 2020; registro sin `@` ni `e` |
| **AT-U03 (negativo)** | U01, U04 | **V4 original sin identificar**, sin perfil en `equipos.csv`; terminal serie en paralelo si se puede | Conectar | Registro: `#V#`, `9`, `6`, sonda. Responde la sonda → V4 original. El equipo **sigue respondiendo** a una segunda medida `@LEERV,BLA,2@` (no se ha bloqueado) |
| **AT-U04 (negativo)** | U01, U03 | Equipo apagado o módulo BT sin equipo | Conectar | "Sin respuesta válida…" con la mención a la velocidad del módulo; ninguna trama tras la sonda |
| **AT-U05** | U02 | **V3-2** con su firmware original; **terminal serie** del PC por el BT; visto bueno de Diego | (1) Enviar a mano `@LEERV,BLA,2@`; (2) `@LEERV,BLA,1@`; (3) `@LEERV,BLA,2@`. **No** enviar `#V#`, `9`, `6` ni ninguna `@` sin `LEERV` | (1)-(3) responden `@LEERV,<n>@/n/r` con el `0x00`; el valor de (3) frente a (1) dice si el V3-2 arrastra `error`. Con eso se escribe su perfil en `equipos.csv` |
| **AT-U06 (negativo)** | U05 | Cualquier V4 original, terminal serie | Medir con la app y mirar los bytes en el terminal | Termina en `/n/r` literal (2F 6E 2F 72) y `00`, **no** en 0D 0A; la app muestra el entero y la siguiente medida no arrastra el resto |
| **AT-U07 (negativo)** | U06, U07 | V4 original conectado | Intentar "Calibrar este equipo", Avanzado y el terminal de diagnóstico con `@VERS@` | Los dos botones deshabilitados con su motivo; el terminal **rechaza** `@VERS@` antes de enviarlo (0 bytes en el enlace) |
| **AT-U08** | U09 | F-46 grabado (tras P7 de la V4.6) | Banco de 3 patrones y "Calibrar este equipo" de un código en simulador y luego en el equipo | Mismo flujo, pantallas y toques que con SLV-002 |
| **AT-U09 (negativo)** | U12 | Campaña de SLV-002 abierta con 2 series | Desconectar, conectar a otro equipo y pulsar Medir | 0 medidas añadidas a la campaña de SLV-002; la app ofrece la campaña del equipo nuevo |
| **AT-U10 (negativo)** | U13 | F-46 con la EEPROM en blanco (en simulador; en equipo tras P7) | Conectar y abrir el banco | Pide el alta de serie; hasta entonces 0 `#S`, `#SC`, `#F`; tras el alta, `#GN#` releído coincide |
| **AT-U11 (negativo)** | U01 | V4.1 **ya bloqueado** por una `@` sin `LEERV` enviada por otro medio (sólo en simulador) | Conectar | Sin respuesta a nada → RF-APP-U03; la app no intenta desbloquearlo con más tramas |

### 7.3 Requisito → pruebas

| Requisito | Pruebas |
| :--- | :--- |
| RF-APP-U01 | T-U01 a T-U06, AT-U01 a AT-U04, AT-U11 |
| RF-APP-U02 | T-U07, AT-U05 |
| RF-APP-U03 | T-U05, AT-U04 |
| RF-APP-U04 | T-U04, AT-U03 |
| RF-APP-U05 | T-U08, T-U09, AT-U06 |
| RF-APP-U06 | T-U10, T-U11, AT-U07 |
| RF-APP-U07 | T-U12, T-U13, AT-U07 |
| RF-APP-U08 | T-U14 |
| RF-APP-U09 | T-U15, T-U16, AT-U08 |
| RF-APP-U10 | T-U17 |
| RF-APP-U11 | T-U18 |
| RF-APP-U12 | T-U19, AT-U09 |
| RF-APP-U13 | T-U20, AT-U10 |
| RF-APP-U14 | Decisión de Diego; sin prueba |

---

## 8. App de usuario

**Añadido 21-sep-2026. Ninguna de estas pruebas existe todavía.** Requisitos:
[`SPEC-App-Usuario-V3.6.md`](SPEC-App-Usuario-V3.6.md) (RF-USR-01 a RF-USR-15). Ninguna ficha de esta
sección sustituye a `T-C30` (§3, "App del cliente", operador real): esa sigue `PENDIENTE sin fecha`
hasta que exista un operador con esta app delante de un equipo (R-09). El valor esperado de cada ficha
viene de una fuente ajena al código de esta app —protocolo, firmware, la app legacy o una decisión
escrita—, nunca de la salida de código todavía por escribir.

**T-USR-01 — Detección sin `e`.** RF-USR-01 · JVM contra `EquipoSimulado` (F-36) y un simulador F-2020
- Pasos: conectar contra F-36; conectar por separado contra un simulador F-2020 (sin `e`).
- Esperado: contra F-36, la secuencia enviada es exactamente `#V#`, y nada más (responde en el primer
  paso); contra F-2020, `#V#` (sin respuesta), `9`, `6` (responde), nunca `e`.
- Fuente del esperado: orden vigente de `SPEC-V3.6.md:464` (RF-APP-03, tabla de pasos 1-4) y la regla
  "nunca se envía `e` a un equipo no identificado" de `SPEC-V3.6.md:461-462`.

**T-USR-02 — Mapa color × tipo, las 12 combinaciones.** RF-USR-02 · JVM contra `EquipoSimulado`
- Pasos: para cada color (blanco, amarillo, verde, rojo, azul, naranja) × tipo (opaco, intenso),
  seleccionar y pulsar Medir.
- Esperado: opaco envía `7`,`8`,`a`,`b`,`c`,`d` en ese orden de color; intenso envía `1`-`6` en el
  mismo orden.
- Fuente del esperado: `SPEC-V3.6.md:496-500` (RF-APP-04, "tipo de lámina I → opaco (`7`,`8`,`a`-`d`);
  tipos II a XI → intenso (`1`-`6`)").

**T-USR-03 — Cero mostrado con leyenda.** RF-USR-03 · JVM contra `EquipoSimulado` forzado a `::0`
- Pasos: medir un código cualquiera con el simulador devolviendo `::0`.
- Esperado: la pantalla muestra "0 (saturado o sin señal)", no el dígito `0` solo.
- Fuente del esperado: `ecuacionesCalibracion.c:49-54` (2020, el `0` no distingue saturación de "sin
  señal") y `SPEC-V3.6.md:502-503` (RF-APP-05, el texto exacto de la leyenda).

**T-USR-04 — Repetición y estadístico.** RF-USR-04 · JVM contra `EquipoSimulado` con lecturas fijadas
- Pasos: fijar tres respuestas `::100`, `::110`, `::120` para el mismo código; medir tres veces.
- Esperado: media 110, mínimo 100, máximo 120, n = 3, guardar habilitado sólo tras la tercera.
- Fuente del esperado: aritmética elemental sobre los tres valores fijados por la prueba (no depende
  del código de la app); el conteo de 3 disparos viene de la app legacy Ionic,
  `medir.page.ts:54` (`medidasRealizar = 3`).

**T-USR-05 — Calibración vencida.** RF-USR-05 · JVM contra `EquipoSimulado` con `#GC#` y reloj fijados
- Pasos: `EquipoSimulado` responde `#GC,2025-09-18#`; reloj de la prueba en 2026-09-19.
- Esperado: aviso "CALIBRACIÓN VENCIDA" visible; la medida no se bloquea.
- Fuente del esperado: `SPEC-Calibracion-V3.6.md:863` (RF-CAL-42, vencimiento a un año de `#SC`/`#GC`)
  y `SPEC-Registro-Indicador-Interventoria.md:568` (RF-REG-03, "avisa, no bloquea", PA-01 de Diego).

**T-USR-06 — Sin calibración registrada.** RF-USR-05 · JVM contra `EquipoSimulado` con `#GC,NONE#`
- Pasos: `EquipoSimulado` responde `#GC,NONE#`.
- Esperado: aviso "SIN CALIBRACIÓN REGISTRADA"; la medida no se bloquea.
- Fuente del esperado: `PROTOCOLO-V3.6.md:52` ("`#GC#` → `#GC,<fecha>#` o `#GC,NONE#`").

**T-USR-07 — Serie ausente en la fila guardada.** RF-USR-06 · JVM contra `EquipoSimulado` con
`#GN,NONE#`
- Pasos: conectar, medir y guardar una fila con el simulador respondiendo `#GN,NONE#`.
- Esperado: la fila guardada lleva el texto "SIN SERIE" en el campo de serie, no un campo vacío.
- Fuente del esperado: `PROTOCOLO-V3.6.md:54` (`#GN#` → `#GN,<serie>#` o `#GN,NONE#`) y
  `SPEC-Registro-Indicador-Interventoria.md:566` (RF-REG-01, ninguna visita exportada con el campo
  vacío).

**T-USR-08 — Guardado sin ubicación, rechazado.** RF-USR-07 · JVM (analizador puro, sin equipo)
- Pasos: construir una medida sin campo de ubicación y llamar al guardado.
- Esperado: el guardado se rechaza y señala el campo "ubicación" como causa.
- Fuente del esperado: decisión de esta SPEC (`SPEC-App-Usuario-V3.6.md`, RF-USR-07); no hay fuente
  externa adicional, se anota como decisión propia de este documento, no del código.

**T-USR-09 — Fecha estable frente al idioma.** RF-USR-08 · JVM (analizador puro)
- Pasos: formatear la misma medida con el `Locale` de la JVM en `es-CO` y en `en-US`.
- Esperado: la cadena de fecha exportada es idéntica en los dos casos (ISO 8601 con zona).
- Fuente del esperado: `ROADMAP-MEJORAS-App.md`, M-07 (defecto de `medicion.java:1562`,
  `Date.toString()` "dependiente del idioma").

**T-USR-10 — Guardar dos medidas no pisa la primera.** RF-USR-09 · JVM (analizador puro)
- Pasos: guardar una medida con retrorreflectividad 150; guardar una segunda con 200.
- Esperado: dos filas en el almacenamiento, con 150 y 200 respectivamente; ninguna en `0`.
- Fuente del esperado: `SPEC-Registro-Indicador-Interventoria.md:490`, citando el defecto de
  `medicion.java:1563,1565` ("tras guardar el campo queda en 0 y un segundo toque guarda un cero").

**T-USR-11 — Exportación sin credenciales.** RF-USR-10 · Bash/grep sobre el código fuente de la app
- Pasos: `grep -ri "password\|smtp\|contraseñ"` sobre el árbol fuente de la app de usuario.
- Esperado: cero coincidencias.
- Fuente del esperado: `ROADMAP-MEJORAS-App.md`, M-12 (defecto de `Enviarcorreo.java:54-55`,
  "credencial SMTP en el fuente").

**T-USR-12 — CSV con el código real, no el nombre.** RF-USR-10 · JVM (analizador puro)
- Pasos: medir con color rojo, tipo intenso; exportar; leer la fila del CSV.
- Esperado: la fila lleva `4` (el código enviado, RF-APP-04), no la palabra "Rojo".
- Fuente del esperado: `SPEC-V3.6.md:496-500` (RF-APP-04) cruzado con el defecto ya documentado en
  `SPEC-Registro-Indicador-Interventoria.md:498-499` ("se guarda el nombre, no el código").

**T-USR-13 — Sin tramas de administración.** RF-USR-11 · Bash/grep sobre el código fuente de la app
- Pasos: `grep -E "#L,|#S,|#F,|#ST,|#FT,|#P,|#KC#|#K#"` sobre el árbol fuente de la app de usuario.
- Esperado: cero coincidencias.
- Fuente del esperado: `PROTOCOLO-V3.6.md` §3, columna "Requiere admin" (líneas 29-45): son
  exactamente las tramas que exigen sesión abierta con `#L`.

**T-USR-14 — Nunca `e`.** RF-USR-12 · Bash/grep sobre el código fuente de la app
- Pasos: revisar cada literal de trama de un byte enviado por la app de usuario.
- Esperado: ninguno es `e` ni `E` como carácter suelto.
- Fuente del esperado: `CLAUDE.md` §4 de este repositorio ("nunca se envía un byte cuyo efecto no se
  conozca"; regla vigente sobre `e` con SLV-002).

**T-USR-15 — Aviso de batería baja.** RF-USR-13 · JVM contra `EquipoSimulado` forzado a `n = 5`
- Pasos: abrir la pantalla de medir; el simulador responde `:5:` a `9`.
- Esperado: aviso de batería baja visible; la medida sigue disponible.
- Fuente del esperado: `Bateria.java` de `rtv-1.0` (`git show rtv-1.0:.../Bateria.java:8-16`,
  `AVISO_N = 19`, comentario "aviso con n < 19"), que a su vez cita `SPEC-Calibracion-V3.6.md` §12.8.

**T-USR-16 — Aviso antes de medir en V3 2020.** RF-USR-14 · JVM contra un simulador F-2020
- Pasos: detectar F-2020 (T-USR-01); observar si el aviso aparece antes o después de la primera sonda.
- Esperado: el aviso ("el equipo va a disparar la luz y a pitar") aparece **antes** de la sonda `9`,
  no después.
- Fuente del esperado: `SPEC-V3.6.md:484-486` ("En un V3 de 2020, `#V#`, `e` y `6` disparan una
  medida… Se avisa al usuario antes de empezar").

### 8.1 Requisito → pruebas

| Requisito | Pruebas |
| :--- | :--- |
| RF-USR-01 | T-USR-01 |
| RF-USR-02 | T-USR-02 |
| RF-USR-03 | T-USR-03 |
| RF-USR-04 | T-USR-04 |
| RF-USR-05 | T-USR-05, T-USR-06 |
| RF-USR-06 | T-USR-07 |
| RF-USR-07 | T-USR-08 |
| RF-USR-08 | T-USR-09 |
| RF-USR-09 | T-USR-10 |
| RF-USR-10 | T-USR-11, T-USR-12 |
| RF-USR-11 | T-USR-13 |
| RF-USR-12 | T-USR-14 |
| RF-USR-13 | T-USR-15 |
| RF-USR-14 | T-USR-16 |
| RF-USR-15 | Sin prueba propia: se cierra con `T-C30` (operador real), no antes |
