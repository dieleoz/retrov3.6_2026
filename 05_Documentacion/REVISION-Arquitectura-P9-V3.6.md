# Revisión de arquitectura P9 — V3.6 (firmware 3.6.2, app 3.6.6)

**Nada de lo que aprueba este documento está medido en SLV-002 con la 3.6.2**: la 3.6.2 no está
grabada y la 3.6.1 que se supone grabada no se ha leído nunca con `#V#` (C-36). Todo lo que sigue sale
de leer el fuente, de las pruebas en simulador archivadas y de cálculos hechos en esta revisión, que se
indican como tales.

- **Fecha:** 19-sep-2026. Revisor adversario, puerta P9 (`ROADMAP.md`, tabla P9-P12).
- **Firmware revisado:** V3.6.2, commit `6a32ca3`, `hex/RetroVertical_V3.6.hex` md5
  `9d5d5e3951c8aa83d1465f16f3733d27` (recalculado aquí: coincide).
- **Firmware de comparación:** V3.6.1 (`8860445`, blob del `.hex` md5 `8736c05d0273fdda66d5988f472d41e1`,
  recalculado aquí), V3.6.0 (`f75ff88`) y base 2020 (`01_Firmware/base_2020_d089f962/`).
- **App revisada:** RTV 3.6.6, commit `70266b1`, `versionName "3.6.6"` (`app/build.gradle:15`).
  APK del árbol `03_App_Movil/RTV-V3.6.apk` md5 `1ad44849a51564d78af00618d34ee602` (recalculado); su
  `classes.dex` contiene las cadenas nuevas de la 3.6.6 ("Cerrar campaña", `EXPORTA`, `SHA-256`). No se
  ha recompilado el APK: la correspondencia binario-fuente es por contenido, no byte a byte.
- **Pruebas JVM:** **71 de 71 en verde**, compiladas aquí desde el fuente de `70266b1` con JDK 11 y
  `javac -sourcepath app/src/main/java` sobre `app/src/test/java` (no con los `.class` de `build/`).
- **Contra:** `SPEC-V3.6.md` r1.2, `TDD-V3.6.md` r1.2, `MATRIZ-SPEC-codigo-V3.6.md`, `PROTOCOLO-V3.6.md`,
  `SPEC-Calibracion-V3.6.md`, `SPEC-Registro-Indicador-Interventoria.md`, `CAMBIOS-V3.6.md` §6-8.

Rutas de firmware relativas a `01_Firmware/RetroVertical_V3.6.X/`; de app, a
`03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Líneas de `calibracion_v36.c` del commit
`6a32ca3`.

---

## 1. Veredictos

| Decisión | Veredicto | En una frase |
| :--- | :--- | :--- |
| **(a) Grabar la 3.6.2 en SLV-002** | **APROBADO CON CONDICIONES** (P9-A1 a P9-A5) | Por fuente, la ruta de medida es la de la 3.6.0/3.6.1 y la ecuación es la de 2020; la EEPROM nueva no solapa nada; configuración igual. Las condiciones son de orden y de comprobación tras grabar |
| **(b) Escribir la calibración con la app 3.6.6** | **APROBADO CON CONDICIONES** (P9-B1 a P9-B12). **Hoy no se puede escribir:** bloquean P9-B1 a P9-B7 | La herramienta sirve: escritura, relectura, `#E` y restauración son correctas y seguras. Lo que falta son datos y decisiones, no código |
| **(c) Pasar a la propuesta de app de producción (P10)** | **APROBADO CON CONDICIONES** (P9-P1 a P9-P9) | Se puede escribir la propuesta. La APK de producción (P11) no se entrega hasta cerrar P8 con la 3.6.2 verificada en SLV-002 |

**C1 / C-47:** **cerrada, modificada** (§4). La ida y vuelta exacta deja de exigirse; la sustituyen
la relectura a `ULP_S = 8` como filtro, `#E` en 5 puntos como aceptación y la regla P9-B9.

---

## 2. Lo que se desmiente o se corrige del encargo y de los documentos

Primero lo que no se sostiene, con su evidencia.

1. **"La ruta de medida no cambia respecto a 2020."** Es cierto **respecto al fuente de 2020**
   (`base_2020_d089f962`) y es **falso como afirmación sobre SLV-002**. El acta de SLV-002 midió, por
   pantalla, **−80 a −89 cuentas en x = 1700-2300** entre el firmware con que llegó el equipo y la V3.6
   (`06_Calibracion/SLV-002/ACTA-antes-y-despues-grabacion.md:16`; C-01, T-C08 "HECHA-FALLA",
   `TDD-V3.6.md:641`). El firmware original no era el fuente 2020, o tenía otra adquisición u otro factor
   de temperatura. Además **T-A20 sólo cubre la ecuación** (`pruebas/T-A20.md:13-15`: `reflectivityValue
   = x` → función), no la adquisición ni el factor de temperatura. Consecuencia: la 3.6.2 no cambia nada
   respecto a la 3.6.0/3.6.1, pero el "como llegó" de SLV-002 no es comparable con lo que mida ahora.
2. **"SLV-002 lleva hoy la 3.6.1."** Es probable, no está comprobado. El registro de IPE
   (`01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.1_2026-09-19.log`) dice *Program Succeeded* hasta
   `0x1d07f`, que cuadra con el tamaño de la 3.6.1 (último byte `0x1D009`, calculado aquí), pero nadie ha
   leído `#V#` después (`ROADMAP.md:26-27`, C-36).
3. **CAMBIOS §8.4: "Tras grabar, comprobar el md5 de la lectura ICSP."** **No se puede.** La
   protección de código está activa: `CONFIG5L = 0xFE` (bit CP a 0) en los tres `.hex` (extraído aquí de
   `0x300008`), y el propio registro de grabación lee ceros (`grabacion_V3.6.1_2026-09-19.log`:
   `Address: 0 Expected Value: ef0d Received Value: 0`). La única verificación posible tras grabar es la
   de IPE durante la programación, más `#V#`/`#GC#` y el resto de P9-A3.
4. **`hex/fuente.md5` "sin fallos".** En este checkout (`core.autocrlf = true`) `md5sum -c
   hex/fuente.md5` **falla en `calibracion_v36.c`** (57 de 58 OK). No es un cambio de contenido: el md5
   declarado `996b480c…` es el del blob de git (LF); el fichero del árbol está en CRLF y da `91d569d5…`
   (comprobado con `git show HEAD:… | md5sum` y con el fichero). Quien verifique G1 con el árbol de
   trabajo verá un falso fallo. Condición documental P9-A6.
5. **La MATRIZ (39/19/4/1) está desfasada frente a la 3.6.6.** Las cifras cuadran con su propia tabla
   (`MATRIZ-SPEC-codigo-V3.6.md:112-118`), pero describen la app 3.6.4 y el firmware 3.6.1. En la 3.6.6
   ya existen el cierre de campaña y las huellas del ZIP que la MATRIZ da por faltantes
   (`Campana.java:190-200`, `Campanas.java:184-197`), y **C-46 ha cambiado de regla**: umbral
   `max(5·1,4826·MAD ; 50)` (`Veredicto.java:38,82`), que no es la de la SPEC (suelo 30, RF-APP-29) ni la de
   la 3.6.5. Se fijó con dos series (P2 y P7, `CampanaTest.java:318`), no con las 17 de T-A39. C-46 sigue
   abierta, con otra cifra.
6. **La app 3.6.6 rotula la 3.6.2 como "3.6.1".** `Sesion.java:140` escribe `(3.6.1)` para cualquier
   fecha de compilación ≥ 2026-09-19 (`:159-160`), y la 3.6.2 también es del 19-sep (cadena `Sep 19 2026`
   en los dos `.hex`, comprobado aquí). Ese texto va a cada serie de la campaña
   (`CampanaActivity.java:361`) y a la exportación (`Campanas.java:214`). Con la 3.6.2 grabada, el ZIP
   dirá un firmware que no es. Condición P9-B11.
7. **"La curva C del código 2 en grado 2 da R(600) ≈ −60."** **No lo he recalculado**: los datos
   (`07 pruebas/19092026_0900/`) no están en ningún commit y P24 está sin decidir (C-40). La cifra sale
   de la MATRIZ (§5.1.4, "cálculo en `double`, no medida"). Lo que sí está comprobado es que la app no
   deja escribir una curva así: `criterioFirmwareS` la bloquea (`Asistente.java:222-241,350-353`) y
   propone la recta (`:354-366`), y aunque se enviara, el firmware la rechaza antes de tocar RAM o
   EEPROM (`calibracion_v36.c:698-699`).
8. **Menor.** CAMBIOS §6 da "ROM 57 009 B"; `hex/memoria.txt` da 56 996 B. Sin efecto.

---

## 3. Pregunta 1 — ¿Se puede grabar la 3.6.2 en SLV-002?

### 3.1 Ruta de medida

- **3.6.0 → 3.6.2:** el único fuente que cambia es `calibracion_v36.c` (`git diff --stat f75ff88 6a32ca3`
  sobre `*.c`, `*.h`, `mcc_generated_files/` y `configurations.xml`). Las 4 líneas borradas son el
  límite viejo de `#ST` (`TEMP_X0_MIN/MAX`). **3.6.1 → 3.6.2:** sólo inserciones (154 líneas) en
  `calibracion_v36.c`; `aplicarEcuacion` (`:229-234`) y `coefFabrica` (`:34-47`) no cambian.
- **Contra 2020:** `diff` del fuente de la V3.6.2 contra `base_2020_d089f962/RetroVertical1.X/` en todo
  lo que toca la medida: `measurement.c` sólo `:212` (`tempSamples[16]`; en 2020 se pisaba un bloque
  muerto, `CAMBIOS-V3.6.md:75-83`; la media sigue siendo la de las muestras 1-15, `measurement.c:234-237`);
  `gui.c` sólo quita `static` a `X_2`, `X_1`, `X_0` con los mismos literales (`:42-44`) y `nivel[6]`
  (`:268`); `ecuacionesCalibracion.c`, las 12 funciones pasan a `aplicarEcuacion` (`:3-41`);
  `arreglar_dato` igual (`:49-54` en el original). `eeprom_manager.c`, `io.c`, `timming.c`, `lcd.c`,
  `LCD1.c`, `binary_utils.c`: idénticos.
- **Mismo compilador y mismo modelo numérico:** XC8 2.10, `double` y `float` de 32 bits, `-std=c99` en la
  base y en la V3.6 (`nbproject/configurations.xml:104,184-187,688` y base `:102,182-185,686`).
- **T-A20** (`pruebas/T-A20.md`): 786 432 evaluaciones, 0 diferencias, con segunda fuente (emulación
  float32 con las mismas cuatro sumas). CAMBIOS §8.3 dice que el arnés regenerado con la 3.6.2 es idéntico
  byte a byte; es coherente con que `coefFabrica` y `aplicarEcuacion` no han cambiado (comprobado aquí
  por diff). **Límite:** T-A20 cubre la ecuación, no la adquisición (§2.1).
- **Las series de campaña hechas con la 3.6.0 y la 3.6.1 valen con la 3.6.2**, porque `e` devuelve la
  `x` ya compensada (`gui.c:301` → `ecuacionesCalibracion.c:191-193`) por un camino que no ha
  cambiado. Condición: que el factor de temperatura sea el de fábrica antes y después (P9-A3). Parte de
  la campaña del 19-sep se midió con la 3.6.0 (campo `firmware` = `V3.6 2026-09-18` en
  `07 pruebas/19092026_0900/medidas_SLV-002_20260919_091213*.csv`).

### 3.2 Registros nuevos de EEPROM

| Rango | Contenido | Evidencia |
| :--- | :--- | :--- |
| 0x000-0x049 | Bloque heredado (sin uso en la medida) | `eeprom_manager.h:9-23`; último byte 70+3 = 73 |
| 0x100-0x103 | Cabecera `V36` | `calibracion_v36.c:64,124` |
| 0x104-0x1ED | 12 códigos + temperatura/PIN, 18 bytes cada uno | `EE_REG_DIR(r) = 0x104 + 18·r` (`:67`); `r = 12` → 0x1DC-0x1ED |
| **0x1EE-0x1FF** | Serie | `EE_SERIE` (`:488`); 18 bytes |
| **0x200-0x211** | Fecha de calibración | `EE_FECHA` (`:489`); 18 bytes |

**No solapan.** `guardarEeprom` sólo recorre cabecera y 13 registros (`:132-139`), hasta 0x1ED;
`regExtraEscribir` sólo sus 18 bytes (`:506-520`). El driver enmascara a 10 bits
(`mcc_generated_files/memory.c:176`), y 0x211 < 0x3FF. T-SN-SC lo comprueba en simulador: 0 bytes de
0x000-0x1ED cambiados por `#SC`/`#SN`/`#SC,NONE#` (`pruebas/T-SN-SC/resultado_T-SN-SC.txt:1-2`). Un
registro borrado (0xFF) responde `NONE` por rango aunque la CRC cuadrase: año 0xFFFF fuera de 2020-2099
(`:527-533,561`) y longitud 0xFF > 12 (`:574`).

### 3.3 `#FT` y el PIN

`#FT` copia `tempFabrica` a `X_2..X_0` (`:722-725`) y llama a `guardarYResponder` (`:726`), que reescribe
el registro 12 desde RAM **con el PIN de RAM** (`byteRegistro`, `:102-108`) y su CRC nueva (`:136-138`),
y lo relee (`:144-152`). El PIN no se toca: es el que haya en RAM, que es el de EEPROM o, si el registro
tenía mala CRC al arrancar, `2026` (`:180,192-198`), el mismo con que se ha podido entrar. `tempFabrica`
se toma de los literales antes de cargar la EEPROM (`:179` frente a `:192-196`, llamada desde
`main.c:20` tras `SYSTEM_Initialize`). T-FT: tras `#P,2026,1234#`, `#ST` y `#FT#`, al rearrancar
`#L,1234#` → `#OK#` y `X_*` iguales a ROM bit a bit (`pruebas/T-FT/resultado_T-FT.txt`). Correcto.

Efecto lateral, no defecto: `#FT` (como cualquier escritura) reescribe también los 12 registros de
coeficientes desde RAM. Si alguno tenía mala CRC al arrancar, queda reescrito con fábrica.

### 3.4 Bits de configuración

Extraídos aquí de los tres `.hex` (2020, 3.6.1, 3.6.2): `EC FF F7 FF 9F FF FF DF FE FF` en
0x300000-0x300009 e IDLOC `FF 0F` ×8, **idénticos**. Ninguno de los tres lleva datos de EEPROM
(segmento 0x31 ausente): grabar no escribe EEPROM, pero el borrado previo de IPE (`-M`, `RUNBOOK.md:132`;
"Device Erased" en el registro) la deja a 0xFF.

### 3.5 Decisiones del agente de firmware

| Decisión | Veredicto | Motivo |
| :--- | :--- | :--- |
| `#SN,NONE#` rechazado (`:751`) | **De acuerdo** | Evita la ambigüedad con la respuesta de `#GN#`. Consecuencia que hay que saber: la serie no se puede borrar, sólo sobrescribir |
| `#SC,NONE#` borra (`:738-741`) | **De acuerdo** | Exige sesión (`:737`). En producción, sólo desde el superadministrador (P9-P6) |
| La fecha se conserva al restaurar fábrica (`#F`, `#FT` no tocan 0x200) | **De acuerdo, con una corrección** | La regla de CAMBIOS §8.2 ("fecha presente y `DEF` = no calibrado") sólo cubre la restauración total. Un `#F,k#` parcial deja `CAL` con fecha y el código `k` en fábrica. **La fecha no prueba que los coeficientes sean los calibrados; sólo la huella lo prueba** (opción A de `SPEC-Registro-Indicador-Interventoria.md` §3.3). P9-P3 |
| `#V#` sigue diciendo `3.6` (`calibracion_v36.h:16`) | **No bloquea hoy; bloquea producción** | Hoy la 3.6.2 se distingue por `#GC#` (la 3.6.1 no tiene esa rama y cae en el `else` que responde `#ERR,FORMATO#`; CAMBIOS §8.4). C-37, P9-P2 |

---

## 4. Pregunta 2 — C1 / C-47: ¿la verificación de la app sustituye a la ida y vuelta exacta?

**Veredicto: C1 queda CERRADA, MODIFICADA.** El argumento es numérico.

**Cuánto mueve R un error de 8 ulp.** En float32 un ulp relativo es 2⁻²³ como mucho, así que 8 ulp en un
coeficiente `c_i` cambian su término en ≤ 8·ulp(c_i)·xⁱ. Calculado aquí (`numpy.spacing` sobre float32),
en x = 4400, el peor extremo del rango de uso:

| Caso | ΔR en x = 4400 | ΔR en x = 200 |
| :--- | ---: | ---: |
| `c3` = 1e-6 (tope del barrido de T-A23) | 0,078 | — |
| `c2` = 1e-3 | 0,018 | — |
| `c1` = 2 | 0,008 | — |
| `c0` = 1000 | 0,0005 | — |
| **Los cuatro a la vez con 8 ulp, en esos topes** | **≤ 0,105** | ≤ 0,001 |
| Los cuatro a la vez, magnitudes de las 12 curvas de fábrica | ≤ 0,023 (códigos 3 y 4) | ≤ 0,0005 |
| Grado 1 (`c3 = c2 = 0`), `c1 ≤ 2`, `c0 ≤ 1000` | ≤ 0,009 | — |

Aunque la relectura añada su propio error de impresión (hasta 3-4 ulp, CAMBIOS §7.2), el coeficiente
guardado dista de lo enviado a lo sumo ~12 ulp si pasa el filtro de `ULP_S`: **ΔR ≤ 0,16 cuentas** en
el peor caso de los rangos de T-A23. **Ningún error de 7-8 ulp en `c2` o `c3` cambia R en una cuenta
entera en ningún x de 200 a 4400.** Lo único visible es que el truncado a entero
(`calibracion_v36.c:230-233`) cambie de lado cuando la parte fraccionaria de R está a menos de ~0,16 de
un entero: ±1 cuenta, que es exactamente lo que tolera `#E` (`AdminActivity.java:507`). Para comparar:
la propia aritmética float32 del PIC, con términos de hasta ~14 000 (código 3 en x = 4400), redondea en
~0,001 por operación; el error de transporte es del mismo orden que el de evaluación.

**Qué cubre cada pieza de la app, y por qué basta:**

- **`#G` a `ULP_S = 8`** (`Ecuacion.java:93`, `AdminActivity.java:526,536`): no es una cota demostrada
  (7 es el máximo **observado** en 600 000 muestras, CAMBIOS §7.2), pero no hace falta que lo sea. Si
  algún coeficiente se pasa, la app restaura (`:544-545`): un fallo de más da una escritura rechazada,
  nunca una escritura mala. Es un parámetro de disponibilidad, no de seguridad.
- **`#E` en 5 puntos con ±1** (`:498-512`, `Pruebas.java:319`): compara enteros del propio firmware
  (`#E` usa `aplicarEcuacion` y `arreglar_dato`, `calibracion_v36.c:673-689`) con la curva enviada
  emulada en float32 (`Ecuacion.java:56-68`). Un polinomio de grado ≤ 3 queda fijado por 4 puntos; junto
  con la relectura, no hay sitio para una desviación oculta "entre los puntos" mayor que la de la tabla.
  Un falso fallo cerca de 0 o 4000 (truncado a 4001 → 0) sólo provoca restauración.
- Lo que el rodeo no hace es **probar la EEPROM tras un rearranque**. Eso lo da `guardarEeprom` (relee
  todo antes de `#OK#`, `:140-153`) y P9-B8.

**Qué queda de C1:** la conversión exacta en el firmware (P-10) **no se exige**. Se sustituye por:
`ULP_S = 8` como filtro, `#E` en 5 puntos ±1 como aceptación, restauración automática, y P9-B9 (el
acta certifica la curva **leída** con `#G`, no la calculada). Si algún día se exige transporte exacto,
el camino barato es transportar los `float` en hex (L-09), no reescribir `strtod`.

---

## 5. Pregunta 3 — ¿Está lista la app 3.6.6 para escribir la calibración (P8)?

### 5.1 Lo revisado en el código

| Pieza | Estado | Evidencia |
| :--- | :--- | :--- |
| Ajuste | Correcto. Mínimos cuadrados en `u` centrada y escalada, Gauss con pivote, grado 1-2, grado + 2 niveles distintos | `Ajuste.java:41-92` |
| Compromiso C | Correcto: todos los patrones del color y la clase, sin ponderar; avisa de la mezcla de tipos | `Asistente.java:136-158,306-312` |
| Cobertura | Correcta y conservadora (niveles ≥ 3, rango ≥ 20 y ≥ 30 %) | `Asistente.java:75-98`; umbrales sin medida (C-43) |
| Forma C2 | Creciente, ≤ 4000 en 200-4400, sin negativos desde la `x` mínima medida | `Asistente.java:179-208` |
| Criterio de `#S` | **Distinto del firmware, pero seguro** (C-CAL-10). La app barre cada `x` entera 600-4300 en float32 (`:222-241`); el firmware evalúa bordes y puntos críticos reales (`calibracion_v36.c:393-422`). Discrepan sólo en el margen de redondeo o si el extremo cae entre dos enteros; en ese caso el firmware rechaza **antes** de copiar nada (`:698-699`) y la app, al no ver `#OK#` y releer igual que antes, no restaura nada que no haga falta (`AdminActivity.java:530-535`). No hay ninguna curva que el firmware acepte fuera de [0 ; 4000] en 600-4300 | Código |
| Escritura | Copia previa de los 12 + temperatura, trama con `%.8E` en `Locale.US`, ≤ 96 bytes | `AdminActivity.java:514-522`; `Tramas.java:201-203,350-361` |
| Restauración (C3) | Correcta por lectura: `#F,k#` si lo anterior era fábrica, `#S` con lo anterior si no, y relectura. Ante `#ERR` o silencio restaura aunque la escritura hubiera entrado. **Sin prueba automática** (T-A32) | `AdminActivity.java:468-490,514-553` |
| Campaña por equipo | Por MAC: las medidas de otro equipo nunca entran | `Sesion.java:191-200`; `Campana.java:132` |
| ZIP | Único, con md5 y SHA-256 en pantalla, envío, registro y diario; campaña cerrable a sólo lectura | `Campanas.java:184-197`; `Campana.java:180-200` |
| Descolgado | Umbral 50 (C-46 sigue abierta, §2.5) | `Veredicto.java:38,82` |
| Pruebas | 71/71 JVM (ejecutadas aquí) | §0 |

### 5.2 Lo que bloquea escribir

1. **Firmware del equipo sin identificar** (C-36): P9-A3 hecha y en verde.
2. **Campaña incompleta**: P27 y P28 (blancos IX), P24 (C-40), P30 ×9, y P25/P26/P29/P31 en la misma
   sesión (C-CAL-03). Y la campaña **cerrada y exportada**: `medidasParaAjuste` cae a las medidas sueltas
   de la sesión si la campaña no tiene series elegidas (`Sesion.java:191-199`), y el informe del ajuste no
   dice de dónde salen los puntos.
3. **Asentamiento** (C-38/C-CAL-04): N de disparos descartados fijado con datos.
4. **Descolgado** (C-46): aplicar las dos reglas (30 y 50) a las series elegidas.
5. **Aceptación** (SPEC-Calibracion §5, P-CAL-01): la app no calcula el residuo por tipo (RF-CAL-15), el
   "no empeorar" (RF-CAL-16) ni la validación cruzada (RF-CAL-17). Se calculan fuera y Diego los firma
   **antes** del primer `#S`.
6. **Método de medida** (C-CAL-08): decisión de Diego.
7. **Grado del código 2** (P-16): se acepta el que deje pasar la app con los datos definitivos; si es 1,
   se escribe en el acta por qué.

### 5.3 Lo que no bloquea (con su sustituto para P8)

- Acta (RF-APP-21) y re-medida guiada (RF-CAL-23): se hacen a mano desde el ZIP, con P9-B8.
- Superadministrador (SPEC-Calibracion §6 y §8, "no implementada"): en P8 lo sustituye la presencia y la
  firma de Diego en el acta. Contradice la letra de SPEC-Calibracion §6; se deja escrito aquí y bloquea
  producción (P9-P6).
- T-A32 sin prueba automática: la lógica se ha revisado aquí; la primera escritura real es T-C23.
- Rótulo de firmware erróneo (§2.6): se anota a mano (P9-B11).
- Uso de `#SC`/`#SN`/`#FT`: la app 3.6.6 no los envía (Grep de `#GC`, `#SN`, `#FT` en el fuente: sin
  resultados; confirmado con la lista de `pedir(` de todas las clases). P8 no los necesita.

---

## 6. Pregunta 4 — Contradicciones abiertas

"Se cierra con" es una medida o una decisión con nombre; aquí no se elige ninguna que se cierre midiendo.

| ID | Clasificación | Se cierra con |
| :--- | :--- | :--- |
| C-01 `x` distinta del firmware original | No bloquea | **No se puede cerrar**: el original se borró el 18-sep. Queda escrito que el "como llegó" no es comparable (§2.1) |
| C-30 sin fábrica para la temperatura | No bloquea | Grabar la 3.6.2 y P9-A3 (`#FT#` → `#OK#`, `DEF,0000`) |
| C-36 qué firmware lleva SLV-002 | **Bloquea P8** | P9-A1 (antes de grabar) y P9-A3 (después) |
| C-37 `#V#` no distingue versiones | Bloquea producción | Hoy, sonda `#GC#`. Decisión P-15 para el próximo firmware |
| C-38 / C-CAL-04 primer disparo | **Bloquea P8** | T-C38: desvío por posición del `resumen.txt` de la 3.6.6 en la campaña completa; N = primera posición cuyo desvío medio frente a la mediana de las posiciones siguientes es menor que la mitad de la `s` típica |
| C-39 / C-CAL-02 XI desordenados | No bloquea P8 si el acta declara el residuo XI; bloquea producción (procedimiento de campo) | Prueba de giro (RF-CAL-06): P5, P6, P7, P23 en 4 orientaciones |
| C-40 / C-CAL-01 P24 | **Bloquea P8** | Nueva serie de P24 con la etiqueta leída en voz alta, y P20 en la misma sesión |
| C-41 / C-CAL-05 P32 duplicado | No bloquea (P32a/b son azul y naranja tipo I: códigos c y d, que no se ajustan) | Diego lee la etiqueta física |
| C-42 / C-CAL-11 tres rangos de `x` | No bloquea P8 (la app aplica la intersección y el firmware manda); bloquea producción | Decisión P-12, con dos medidas: `x` en oscuro con la 3.6.2 y `x` del patrón más alto. Ojo: `e` devuelve 0 por encima de 4000 (`arreglar_dato` en la rama de `e`, `ecuacionesCalibracion.c:191-193`) |
| C-43 umbrales de cobertura | No bloquea (sólo impiden ajustar) | T-C39 con los tipo I |
| C-44 T-A23 "cerrada" | Cerrada por esta revisión (§4) | — |
| C-45 RF-APP-03 | Cerrada (SPEC r1.2) | — |
| C-46 descolgado | **Bloquea P8** en forma débil | T-A39 con las 17 series literales y las de la campaña: listar disparos entre 30 y 50 cuentas de la mediana en las series elegidas. Si no hay ninguno, la diferencia de reglas no afecta a P8 |
| C-47 / C-CAL-12 C1 | Cerrada, modificada (§4) | — |
| C-CAL-03 IX/IV desordenados | **Bloquea P8** | P25, P26, P29, P30, P31 en la misma sesión |
| C-CAL-06 catálogo maestro | No bloquea P8: P1-P31 son idénticos en los dos CSV (4 primeros campos, comparado aquí). El acta cita el md5 del asset (`bc4604b0…`). Bloquea producción | Decisión: un maestro |
| C-CAL-07 SPEC dice sólo 1 y 2 | No bloquea (documental) | Corregir la SPEC |
| C-CAL-08 método de medida | **Bloquea P8** | Decisión de Diego |
| C-CAL-09 dos umbrales de `s` | No bloquea si todas las series elegidas tienen `s` ≤ 10 (en C2, ≤ 7,7) | Comprobación sobre los datos |
| C-CAL-10 criterio de `#S` doble | No bloquea (§5.1) | T-A41 en MDB con una curva cuyo mínimo caiga entre dos enteros |
| C-CAL-13 runbook dice 3.6.1 sin grabar | No bloquea (documental) | Actualizar el runbook a la 3.6.2 |
| C-CAL-14 reproducibilidad entre sesiones | No bloquea la escritura; bloquea declarar incertidumbre | Campaña de otro día (§9 de SPEC-Calibracion) |
| C-CAL-15 tipo I sin nota de certificado | **Bloquea ajustar 8 y b**; no 1 ni 2 | Certificado de los tipo I, o 8 y b sólo se verifican |

---

## 7. Pregunta 5 — ¿Puede una orden dejar el equipo inservible o sin recuperación por Bluetooth?

**No hay ninguna vía que deje el equipo sin medir.** Hay **una** que deja sin administración por
Bluetooth.

| Vía | Efecto | Recuperación |
| :--- | :--- | :--- |
| **`#P` con un PIN que luego nadie recuerda** (`calibracion_v36.c:782-796`; la app lo ofrece, `AdminActivity.java:292-309`, "la app no lo guarda") | Sin `#S`, `#F`, `#FT`, `#SC`, `#SN` para siempre. La medida sigue | **Sólo ICSP**, y con CP activo el borrado completo pierde también la calibración. P9-B10 |
| 5 fallos de PIN (`:645-656,784-790`) | `#L` y `#P` bloqueados | Apagar y encender |
| `#S` con una curva válida pero equivocada | Medidas malas en ese código | `#F,k#` o `#S` con la copia (`Sesion.guardarCoeficientes`) |
| `#F,*#` | Se pierde la calibración | Reescribir desde la copia de la app |
| `#ST` | Factor de temperatura cambiado. La app no lo envía (RF-APP-23) | `#FT#` (sólo 3.6.2) |
| Corte de alimentación durante una escritura | El registro a medias falla la CRC y vuelve a fábrica al arrancar; si es el de temperatura/PIN, también el PIN vuelve a `2026` (`:187-198`) | Reescribir. Nunca deja el equipo bloqueado |
| Escritura de EEPROM con interrupciones apagadas, ~4 ms por byte, hasta ~1 s en la primera (`memory.c:172-190`; CAMBIOS §4.6) | Pueden perderse bytes que lleguen en ese tiempo | La app espera la respuesta de `#S`/`#F` hasta 5 s (`AdminActivity.java:480,522,567`) antes de enviar nada más |
| Trama `#` sin cerrar o de más de 96 bytes (`uart_module.c:61,87-91,113-115`) | Se descarta | Sola, a los 2 s o en el siguiente `#` |
| Caída del enlace a mitad de `escribir()` | La excepción corta la secuencia (`AdminActivity.java:190-191`): puede quedar escrito sin `#E` ni restauración | P9-B12 |
| `e` a un equipo sin identificar | En la V3 2020 dejó el Bluetooth mudo hasta apagar (`RUNBOOK.md:91`). La app no lo envía antes de identificar (`Pruebas.java:286-289`) | Apagar y encender |
| **Regrabar un equipo ya calibrado** | El borrado de IPE deja la EEPROM a 0xFF: se pierden coeficientes, serie, fecha y PIN | No es una orden de la app, pero es el riesgo mayor para el cliente. P9-A4 y P9-P7 |

---

## 8. Condiciones

### 8.1 Para grabar la 3.6.2 (a)

| N.º | Condición | Comprobación |
| :--- | :--- | :--- |
| **P9-A1** | Antes de conectar el PICkit: `#V#`, 12 `#G`, `#GT#` y `#GC#` con la app o un terminal, archivados en `01_Firmware/lecturas_equipos/SLV-002/` | `#V#` con fecha `2026-09-19` y `#GC#` → `#ERR,FORMATO#` confirman que llevaba la 3.6.1 (cierra C-36 hacia atrás); fecha `2026-09-18`, la 3.6.0 |
| **P9-A2** | Se graba sólo el `.hex` de md5 `9d5d5e39…`, con su md5 recalculado en la misma sesión y el registro de IPE guardado | Registro con *Program Succeeded* y memoria de programa hasta ≈ `0x1DB7F` (último byte `0x1DB67`), no `0x1d07f` (3.6.1). El "Verify failed" posterior es CP (§2.3) |
| **P9-A3** | Tras apagar y encender, G4 abreviada (T-C37 adaptada). **Si algo falla, no se escribe nada** | `#V,3.6,2026-09-19,DEF,0000#`; `#GC,NONE#` y `#GN,NONE#` (esto distingue la 3.6.2); 12 `#G` = fábrica a `ULP_G`; `#GT#` = fábrica; `#E` 60/60 contra la tabla de fábrica; `#L,2026#` → `#OK#`; `#FT#` → `#OK#` y `#V#` sigue en `DEF,0000`; `#S,2,<fábrica>#` → `#ERR,FORMATO#` (rechazada antes de tocar RAM, `:698`); `#Q#` |
| **P9-A4** | **Orden:** la 3.6.2 se graba **antes** del primer `#S`. Grabar después de calibrar borra la calibración | Acta: hora de grabación anterior a la de la primera trama `#S` del registro de la app |
| **P9-A5** | Puente de medida: con `e`, asentamiento y 9 disparos, tres patrones ya medidos en la campaña (uno por debajo de x ≈ 1600, uno hacia 2100, uno por encima de 3000; p. ej. P22, P28 y P4) | Media dentro del 3 % de la media de campaña (la reproducibilidad observada entre sesiones llega a 2,5 %, C-CAL-14). Fuera: se para y se investiga antes de seguir |
| P9-A6 | Documental: CAMBIOS §8.4 retira "md5 de la lectura ICSP"; `fuente.md5` se verifica contra el blob (`git show <commit>:<ruta> \| md5sum`) o se declara el md5 del fichero con el fin de línea del árbol | Revisión del documento. No bloquea grabar |

### 8.2 Para escribir la calibración con la app 3.6.6 (b)

| N.º | Condición | Comprobación |
| :--- | :--- | :--- |
| **P9-B1** | P9-A1 a P9-A5 cumplidas | Registro de G4 en el acta |
| **P9-B2** | Campaña completa, cerrada y exportada: P27, P28, P24, P30 ×9 y P25/P26/P29/P31 en una sesión | Evento `CIERRE` y `EXPORTA` con SHA-256 en el diario; el acta cita ese SHA-256 |
| **P9-B3** | N de asentamiento fijado con T-C38 (C-38) | `resumen.txt` de la campaña, desvío por posición; firma de Diego |
| **P9-B4** | C-46 comprobada sobre los datos (T-A39) | Lista de disparos a 30-50 cuentas de su mediana en las series elegidas; vacía, o decidida por Diego a la vista |
| **P9-B5** | Criterios de SPEC-Calibracion §5 calculados desde `campana.csv` **antes** de escribir (RF-CAL-14 a 17), y umbrales aprobados (P-CAL-01) | Hoja de cálculo o script en `06_Calibracion/SLV-002/` y firma de Diego |
| **P9-B6** | Método de medida decidido (C-CAL-08) y P24 resuelto (C-40) | Decisión escrita |
| **P9-B7** | El ajuste se hace con la campaña (no con medidas sueltas de sesión) | El informe del asistente lista exactamente los patrones y `n` de las series elegidas del ZIP |
| P9-B8 | Un código cada vez (RF-CAL-24): `#S`, `#G`, `#E`, re-medida con el código escrito (RF-CAL-18), y sólo entonces el siguiente. Al final, apagar, encender y repetir `#V#`, `#G` y `#E` de lo escrito (persistencia real, T-C23) | Acta, bloque "Escritura" y "Después" |
| P9-B9 | La curva que certifica el acta es **la leída con `#G` tras `#S`**, y los residuos se recalculan con ella en float32 (`respuestaFloat32`), no con la ecuación en `double` del asistente | Acta: coeficientes = texto de `#G` |
| P9-B10 | No se cambia el PIN de SLV-002 en P8. Si Diego decide cambiarlo, el PIN nuevo se le entrega por un canal fuera de la app y el acta dice que se cambió | Acta; registro de la app sin `#P` |
| P9-B11 | El acta anota a mano el firmware (md5 del `.hex` y respuesta de `#GC#`), porque la app rotula la 3.6.2 como 3.6.1 | Acta, bloque "Equipo" |
| P9-B12 | Si la escritura de un código termina en error de enlace: reconectar, `#G,k#`, `#E` en 5 puntos, y restaurar con la copia si no coincide con lo enviado ni con lo anterior. Nunca se sigue con otro código antes | Registro técnico de la app |

8 y b, además: sólo con el certificado de los tipo I o la aprobación explícita de Diego de sus valores
(C-CAL-15). Si no, se verifican sin ajustar.

### 8.3 Para pasar a la propuesta de app de producción (c)

| N.º | Condición | Comprobación |
| :--- | :--- | :--- |
| P9-P1 | La propuesta empieza por especificar lo que hace la app de campo existente (`RetroVerticalP1`) y después lo que se mejora | Estructura del documento de P10 |
| P9-P2 | Identificación de firmware sin ambigüedad: sondear `#GC#`/`#GN#`, no inferir por fecha; pedir para el próximo firmware una cadena de versión propia (C-37, P-15) | Requisito en la SPEC de P10 |
| P9-P3 | La validez de una calibración la decide la huella (`#G` ×12 + `#GT#` frente al certificado); `#GC` es informativo. Detecta `#F,k#` parcial por la máscara de `#V#` código a código | Requisito con prueba JVM |
| P9-P4 | Restauración (C3) y PIN en clases Java puras, con T-A32 y T-A19 | Pruebas en verde |
| P9-P5 | Un único rango de `x` de uso (C-42, P-12) y tratamiento explícito de `e` = 0 por encima de 4000 | Decisión escrita y requisito |
| P9-P6 | Superadministrador (SPEC-Calibracion §8) antes de exponer `#S`, `#F`, `#FT`, `#SC`, `#SN` o `#P` a un operador | Requisito y prueba |
| P9-P7 | Procedimiento de regrabación de un equipo calibrado: copia de `#G` ×12, `#GT#`, `#GN#`, `#GC#` antes de grabar, y restauración y verificación después | Paso del runbook |
| P9-P8 | APK atada a un commit, con md5 declarado en un commit (C-16) | Commit |
| P9-P9 | P11 (APK de producción) no se entrega antes de cerrar P8 en SLV-002 con la 3.6.2 verificada | ROADMAP |

---

## 9. Cómo se ha comprobado

Reproducible desde `D:\IT\P_RetroVertical_V3.6` en `6a32ca3`:

- md5 de `.hex`: `md5sum hex/RetroVertical_V3.6.hex`; 3.6.1: `git show 8860445:…/hex/RetroVertical_V3.6.hex | md5sum`.
- Configuración, IDLOC, segmentos y fecha de compilación: lectura del Intel HEX (registros tipo 04/00) y
  búsqueda de `Sep 19 2026` en la memoria de programa de los dos `.hex`.
- Diferencias de fuente: `diff --strip-trailing-cr` contra `base_2020_d089f962/RetroVertical1.X/` y
  `git diff --stat f75ff88 6a32ca3`, `git diff 8860445 6a32ca3`.
- Sensibilidad de R a los ulp: `numpy.spacing(float32)` por coeficiente, en x = 200 y 4400.
- Pruebas JVM: `javac -encoding UTF-8 -sourcepath app/src/main/java app/src/test/java/…/*.java` y
  `JUnitCore` con las seis clases: `OK (71 tests)`.
- Catálogos: comparación de las filas P1-P31 (4 primeros campos) de los dos CSV.

Lo que **no** se ha hecho: grabar, medir, recompilar el `.hex` o el APK, ni rehacer el ajuste con los
datos del 19-sep.
