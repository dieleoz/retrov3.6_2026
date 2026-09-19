# Revisión de arquitectura P14 — V3.6 (app 3.6.15; firmware 3.6.2 sin cambios)

**Nada de esta revisión se ha ejecutado contra un equipo ni en un teléfono.** T-C41 y T-C43 siguen sin hacerse
con la app en SLV-002. "Calibrar todo" no ha corrido nunca fuera de la JVM. Todo lo que sigue sale de leer el
fuente, ejecutar las pruebas, recompilar el APK y rehacer las cuentas.

- **Fecha:** 19-sep-2026, tarde. Revisor adversario. Puerta **P14**: si Diego puede calibrar SLV-002
  (Coviandina, firmware 3.6.2) con la 3.6.15, **de una vez**, después del banco: primero el 8, luego el b y el 5,
  y el 3, el 4 y el 6 si procede.
- **Repositorio:** `D:\IT\P_RetroVertical_V3.6`. Se revisa `c7ef3c6`. HEAD está en `8c8c7de`, que sólo añade
  `06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip` y `HUELLAS.txt` (`git diff --stat`).
- **APK:** `03_App_Movil/RTV-V3.6.15.apk`, md5 `d22727f0aacfed1c11f978dc37c63443`, recalculado aquí.
  `RTV-V3.6.15-3615.apk` tiene el mismo md5.
  - `aapt`: `versionCode='3615' versionName='3.6.15'`.
  - `assembleDebug --offline` en un export limpio de `c7ef3c6`: los 422 ficheros fuera de `META-INF/` son
    **idénticos** a los del APK entregado. `classes2.dex` `967d7afd…`; `decisiones.csv` `d21be8b2…`.
  - Colas: completa `9ddb7882…`, representativa `2e266bbf…`, anual `d86eddf7…`. Son las tres que admite
    `BancoCola.java:28-31`.
  - `apksigner`: `CN=Android Debug`, SHA-256 `c990adf6…075f`. Es la misma firma que la 3.6.14, así que se
    instala encima.
- **Pruebas JVM:** **198 de 198 en verde**, repartidas en 16 clases. `FlujoCalibracionTest` tiene 49 y
  `Version3615Test` 18. Ninguna usa `@Ignore` ni `Assume`. **T-S00: 132/132** (`#G` 38/38, `#E` 70/70), según el
  informe que imprime `TS00Test`.
- **Contra:**
  - P13 (§5 y §6), P12 y P11;
  - `QA-App-3.6.14.md` §7;
  - `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`, todas sus filas (`:8-23`);
  - `06_Calibracion/PLAN-Banco-Representativo.md` §1, §3.1 y RF-APP-49 a 53 (`:443-447`);
  - `SPEC-App-Unica-V36-V46.md`, sólo RF-APP-U04, U05 y U12.

Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.

**Cómo se ha hecho.** Cuatro subagentes:

- uno de volcado: compilar, probar y cotejar el APK;
- tres adversarios: el flujo de calibración, el alias de serie, y el banco con el ZIP.

Sus hallazgos más graves los he vuelto a abrir yo en el código: P14-01, -02, -04, -B04, -B05 y -S01, con las líneas
que se citan. Las cifras de §3 son cálculo propio. Se hacen con la fórmula de
`PLAN-Banco-Representativo.md:60` y con la función `sigma_curva` de `06_Calibracion/tools/banco_representativo.py`
(`:341-363`), llamada con otro K.

---

## 1. Veredicto

**APTO CON CONDICIONES para que Diego calibre SLV-002 con la 3.6.15 después del banco.**

Sin las condiciones de §7 **no es apto**, por tres motivos que no son de código:

1. **Diego no ha decidido tres cosas que la app decide por él o que contradicen el plan:**
   - el protocolo del banco en los patrones del 8, del b y del 5 (§3, P14-B04);
   - el margen de RF-CAL-15 (P14-08);
   - la discrepancia de la s_rep (§3.4).
2. **"Calibrar todo" acepta las actas sin que nadie las mire** (P14-04). En la primera ejecución real del flujo
   eso no se puede consentir con el 8.
3. **El renombrado de la serie tiene dos caminos que bloquean la calibración** (P14-S01 y P14-S02). Si se hace
   en mal momento, deja a Diego sin poder calibrar ese día.

Nada de lo encontrado permite escribir una curva en otro equipo, ni aceptar un acta de otro equipo, ni dejar
el equipo con una curva sin acta. **Todos los defectos nuevos fallan hacia el bloqueo o hacia la pérdida de
trazabilidad.** Por eso el veredicto no es NO APTO.

---

## 2. Cierre de P13 y de QA-3614

Leyenda:

| Clave | Significado |
| :---: | :--- |
| **C** | Cerrado con código y prueba |
| **Cc** | Cerrado en código, sin prueba |
| **P** | Parcial |
| **A** | Abierto |
| **Op** | Se cumple en campo |

### 2.1 Condiciones C-1 a C-6 de P13

| N.º | Estado | Evidencia |
| :---: | :---: | :--- |
| C-1 A5 y deriva, a mano | **A → Op, y peor** | La app sigue sin calcular la deriva por A5. Además, "Calibrar todo" encadena los códigos sin detenerse entre sesiones del banco (`FlujoCalibracion.java:1356-1367`), así que la revisión manual queda fuera del flujo (P14-04) |
| C-2 T-C41 | **P** | El 8 tiene apagado propio (`FlujoCalibracion.java:903`). Los siguientes reutilizan el apagado de la persistencia anterior (`:901-902`) con una bandera mal acotada (P14-01) |
| C-3 El 8 solo, hasta ACEPTADA | **C en código; se pierde la pausa humana** | El 8 se acepta antes del b (`:1356-1366`; prueba `calibrarTodoEnUnaSesion`). Pero se acepta **sin que nadie vea el acta** (P14-04) |
| C-4 Persistencia | **Cc** | Una por código, en `persistirYAceptar` (`:1372-1379`). Exige ver caer el enlace, como en la 3.6.14 |
| C-5 "SIN RESTAURAR" | **P** | El acta ya no se cierra con una restauración fallida (`:1295-1300`). Quedan P14-05, -06 y -10 (§5) |
| C-6 ZIP al aceptar | **P** | Se exporta al final de la sesión y se avisa si falla. No se exporta en tres caminos (P14-07). El acta no cita el SHA-256 del ZIP de soporte (P14-B02) |

### 2.2 Defectos P13-01 a P13-05

| ID | Estado | Evidencia |
| :--- | :---: | :--- |
| P13-01 RF-CAL-15 en el alcance | **C, con un margen que no puso Diego** | `decisiones.csv:8`: `RF-CAL-15 I 11.5 3`. **El margen de ±3 sobre el RMS (hasta 14,5 %) lo generaliza la app.** Diego fijó el techo con ±3 sólo para P39 y P49 (`DECISIONES…:19`). Ver P14-08 |
| P13-02 Techo | **C** | `Decisiones.java:53-58`: mismo signo y \|dev\| ≤ \|cifra\| + margen. También en `Remedida3611.java:110`. Cifras en §4 |
| P13-03 RF de "rehacer" | **A** | No se ha tocado la SPEC ni el TDD. Fuera del alcance de esta puerta |
| P13-04 Comentario de `TS00Test` | No revisado | Sin efecto en campo |
| P13-05 Rechazo con restauración fallida | **P, mejor que antes** | §5 |

### 2.3 QA-3614

| ID | Estado | Evidencia y lo que queda |
| :--- | :---: | :--- |
| -01 Importar encima pierde la anulación | **C para los diarios** | `ImportadorCampana.java:114-119`, `:145-176`; `Version3615Test.java:204`. **Pero ahora el tipo de cola no viaja al importar** (P14-B01) |
| -02 Salto de línea en el motivo | **C** | `Csv.java:48-76`; `setSingleLine` en `BancoActivity.java:220`, `:778`, `:872` y `:958`; `Version3615Test.java:232` |
| -03 Rehacer no mira el acta | **P** | Bloquea con un acta en curso, avisa con un acta aceptada y el acta guarda los id de serie (`FlujoCalibracion.java:336-350`, `:375-404`, `:1385-1396`). **No cubre el OSCURO ni la A5**, que son el ancla y la s_rep, y da falsos avisos (P14-B08) |
| -04 Rehacer tras exportar | **C** | `Version3615Test.java:257` |
| -05 OSCURO o A5 de otra sesión | **C** | `RehacerBanco.java:25-35`; la prueba sólo cubre el OSCURO |
| -06 "No, rehacer" | **C** | `BancoActivity.java:883-892`. La prueba copia la secuencia y no llama a la pantalla |
| -07 Fecha de la anulación | **C** | `ImportadorCampana.java:117`, `:140` |
| -09 Aceptar con otra tabla | **C** | `FlujoCalibracion.java:400-403`, `:659`; `unActaAbiertaConOtrasReglasNoSeAcepta`. Efecto colateral en P14-11 |
| -10 Fallo al exportar | **Cc** | `CalibrarActivity.java:418-431`. Queda P14-B15 |

**Lo que declara el desarrollador, contrastado.**

| Afirmación | Veredicto |
| :--- | :--- |
| 198 pruebas en verde y T-S00 132/132 | Cierto |
| Una sesión, un acta por código y el 8 aceptado antes | Cierto |
| "Todo se comprueba antes de empezar" | **Exagerado.** No se comprueban T-C41, PIN ni batería, que no se pueden comprobar antes. Tampoco que el 8 vaya a quedar aceptado: con b y 5 marcados y el 8 sin aceptar, la sesión falla ya dentro del b (`:1349` frente a `:449-453`), sin escribir nada |
| `decisiones.csv` con todas las decisiones | **Casi.** Salvo el margen de RF-CAL-15 (P14-08) |
| Selector de banco y ZIP | Cumplen en lo esencial, con los defectos de §6 |

---

## 3. Pregunta 2: ¿el protocolo 1×4 de Diego es compatible con P10-P13?

### 3.1 Qué aplica de verdad la app

Con el modo "rápido", que viene activo por defecto (`BancoActivity.java:86`), `Protocolo.efectivo`
(`Protocolo.java:21-25`) pone **1 × 4 en todos los pasos PATRON**. Sólo el OSCURO y la A5 van a K ≥ 5. Eso
incluye tres grupos que el plan y P10 querían a K = 5:

- **los AJUSTE del 8 y del b:** P44, P37, P34, P39 y P38 (`PLAN:183`; cola `cola_banco_P1-P132.csv:52-54`,
  `:97-98`: K = 5);
- **los RE-MEDIDA del banco:** P43, P49 y los demás (`PLAN:179`);
- **P81:** P10-C3 lo fuerza a K = 5 al cargar la cola (`BancoCola.java:200`), pero `efectivo` lo baja a 1.

La re-medida del acta sigue a 5 × 4 (`Remedida3611.java:21-22`). Al banco se le aplica la K real de la serie
(`FlujoCalibracion.java:1074`).

La decisión PROTOCOLO-MIN (`DECISIONES…:23`) sólo exceptúa la A5 y el OSCURO. **La app la transcribe al pie de la
letra, y al hacerlo contradice P10-C3 y el plan §3.1 regla 4.** Queda como contradicción abierta C-P14-1 (§8).
Esta revisión no elige entre las dos.

### 3.2 Lo que se conserva

| Garantía | ¿Se conserva con 1×4? | Por qué |
| :--- | :---: | :--- |
| **s_rep** (RF-CAL-38) | **Sí** | Sale sólo de la A5-INICIO de la sesión 1 con ≥ 2 colocaciones (`Anclas.java:89-104`). La A5 está a K ≥ 5 (`Protocolo.java:22-23`). Nunca sale de un patrón |
| **Ancla** | **Sí** | OSCURO a K ≥ 5 |
| **Deriva** (RF-CAL-36) | **Sí, y algo mejor** | La A5 del final también pasa a 5. El plan pedía 3 (P14-B06) |
| **Cobertura** | **Sí** | La regla cuenta niveles (patrones), no colocaciones (`PLAN:51`; `Asistente.java:75-101`) |
| **B3** (P9-B3 / P10-C12: el mismo protocolo por colocación en banco, re-medida y campo) | **Sí** | Es 1 asentamiento + 4 disparos por colocación en el banco (M = 4) y en la re-medida (5 × 4). Cambia el número de colocaciones, no el protocolo de cada colocación |

### 3.3 Lo que se pierde, en cifras (s_rep = 2,2 %)

El ruido de colocación de un patrón, pasado a R, es σ_R/R = s_rep · x/(x − 565,4)/√K. Cuanto más cerca del
oscuro, mayor. Los tipo I y el azul están muy cerca: x de 734 a 1151 (`cola_banco_P1-P132.csv:52-55`,
`:97-99`, `:159`).

| Patrón | x/(x − x_osc) | σ_R de un punto, K = 1 | K = 3 | K = 5 |
| :--- | ---: | ---: | ---: | ---: |
| P39 (b, AJUSTE) | 4,35 | **9,6 %** | 5,5 % | 4,3 % |
| P49 (b, RE-MEDIDA) | 3,44 | **7,6 %** | 4,4 % | 3,4 % |
| P44 (8, AJUSTE) | 2,70 | **5,9 %** | 3,4 % | 2,7 % |
| P43 (8, RE-MEDIDA) | 1,97 | **4,3 %** | 2,5 % | 1,9 % |
| P81 (5) | 3,01 | **6,6 %** | 3,8 % | 3,0 % |

**σ de la curva.** Es la columna (a) de `PLAN:261-270`: sólo colocación, relativa a R, en la x mínima, el centro y
la x máxima. Se recalcula con la K de rápido.

| Cód. | Cola, K de la cola | Rápido, con RE-MEDIDA a 5 | **Rápido tal cual (todo a 1)** |
| :---: | :--- | :--- | :--- |
| 8 | 1,14 % | 1,65 % | **2,54 %** (×2,2) |
| b | 2,24 % | 3,03 % | **5,01 %** (×2,2) |
| 5 (completa) | 1,10 % | 1,90 % | **1,90 %** (×1,7) |
| 5 (representativa) | 1,89 % | 3,28 % | **3,28 %** |
| 3 (representativa) | 5,57 / 2,20 / 2,79 % | 9,50 / 2,99 / 4,07 % | **9,71 / 4,08 / 5,09 %** |
| 1, 2, 4, 6 | de 0,4 a 2,6 % | ×1,4 a ×1,7 | ×1,5 a ×1,8 |

Sumando la dispersión entre láminas, columna (b) del plan:

- el b pasa de 6,67 % a **8,07 %**;
- el 8, de 3,08 % a **3,85 %**;
- el 5, de 1,93 % a **2,48 %** en la cola completa.

**Re-medida, comprobación 2 de RF-CAL-39** (`Remedida3611.java:65`). El límite es 2·s_rep·√(1/5 + 1/K_banco):

| K_banco | Límite en x |
| ---: | ---: |
| 5 | ±2,78 % |
| 3 | ±3,21 % |
| 1 | **±4,82 %** (×1,73) |

Pasado a R en el patrón de re-medida:

| Patrón | Con K_banco = 5 | Con K_banco = 1 | Qué deja pasar con K = 1 |
| :--- | ---: | ---: | :--- |
| P49 (b) | ±9,5 % | **±16,5 %** | Un patrón cambiado o mal apoyado que mueva la R un 15 % |
| P43 (8) | ±5,5 % | **±9,5 %** | — |

La prueba no da más falsas alarmas, porque usa la K real, **pero pierde casi la mitad de su poder**.

**Resolución útil** (`PLAN:60`). Con K = 1, en lugar de 3, es √3 = 1,73 veces peor. El azul tipo I pasa de ΔR ≈ 5,5
a ≈ 9,5 unidades con R ≈ 9. El equipo deja de distinguir un certificado de 7 de uno de 16.

**Dispensa del b (PA-24).** La cifra que Diego aceptó, P39 +15 %, salió de series a K = 3 o 5. Con K = 1, el ruido
de P39 **por sí solo** es de 9,6 %. Es tres veces el margen de 3 puntos. Orden de magnitud, sin contar la
correlación con el ajuste:

- con K = 1, P39 cae por encima del techo de +18 % en ≈ 38 % de los casos; con K = 5, en ≈ 24 %;
- con K = 1, cae por debajo del 10 % por azar en ≈ 30 % de los casos, y entonces no hace falta ninguna dispensa.

**El juicio del b pasa a depender de la suerte de una colocación.**

**Criterios que desaparecen sin avisar con K = 1.**

- **La reproducibilidad por patrón (REPRO_MAX 3 %).** Con una sola colocación, `evaluarColocaciones` salta a
  `evaluar` (`Veredicto.java:107-108`) y la s entre colocaciones no se calcula. Una colocación mala sólo se
  detecta si la s dentro pasa de 15 cuentas (`Veredicto.java:34`) o si salta el aviso de orden. Con 1 × 4, un
  patrón mal apoyado pero estable entra en el ajuste. Es P14-B05.
- **"La s_rep de la propia serie si K ≥ 3"** (RF-CAL-38, `SPEC-Calibracion-V3.6.md:796-800`). Deja de existir. Sin
  la A5-INICIO, la re-medida queda NO_EVALUABLE (`Remedida3611.java:58-60`): falla hacia el bloqueo.

**Lectura.**

- Para los códigos de 12 a 24 patrones (1, 2, 4, 6 y el 5 de la cola completa), 1 × 4 es defendible. El ruido
  de colocación sigue por debajo de la dispersión entre láminas, que es la que manda (`PLAN:71-73`).
- **Para el 8 y el b, que tienen 3 o 4 puntos, cada uno a 4,3 × x del oscuro, no lo es.** La σ de la curva del b
  por colocación sube del 2,2 % al 5,0 %. Eso es la mitad del 10 % de RF-CAL-14. Y la dispensa de Diego se juzgaría
  con un ruido mayor que su propio margen.

**Coste de medir en "preciso" sólo lo que se va a escribir.** Con 11 s por recolocación y 6 s de disparos
(`PLAN:55`), cada colocación de más cuesta unos 17 s. Si el 8, el b y el 5 van en preciso:

- el 8 y el b: 7 patrones × 4 colocaciones de más, unos 8 min;
- el 5 de la cola completa: 12 patrones × 2 colocaciones de más, unos 7 min, y P81 a 5.

**Unos 15 minutos.**

### 3.4 Dos hechos que cambian la lectura (no se eligen aquí)

- **La s_rep que dará la A5 no tiene por qué ser 2,2 %.** El ZIP de las 15:10 (`8c8c7de`), dentro de su
  `resumen.txt:224-229`, da P22 1,74 %, P28 1,35 % y P4 0,44 %, con una **media de 1,18 %**. Frente a 2,24 %
  de la mañana (`PLAN:47`) es la mitad.
  - Si esa es la buena, todas las pérdidas de §3.3 se reducen a ≈ 0,54 veces. La de 1 × 4 queda cerca de lo que
    daba K = 3 con 2,2 %.
  - Es la contradicción abierta C-P14-2, y se cierra con la A5 del banco que haga Diego.
- **El banco del tipo I que ya existe no está ni en 5 × 4 ni en 1 × 4.** En ese mismo ZIP, S006-S013 (P34, P37,
  P38, P39, P43, P44 y P49) tienen **3 colocaciones × 3 disparos**: 9 disparos en 3 colocaciones
  (`diario_campana_SLV-002_00211305193B.csv:159-169` para P39 y `:192-200` para P49). Se midieron con la 3.6.11.
  - La cola pedía 5 × 4 (`cola_banco_P1-P132.csv:52-55`, `:97-99`), y la re-medida irá a 5 × 4.
  - **Si Diego calibra el 8 y el b sobre esa campaña, el banco y la re-medida no tienen el mismo número de
    disparos por colocación.** Es justo lo que P9-B3 / P10-C12 prohíbe.
  - No he comprobado si la 3.6.15 da esos pasos por HECHO. Es la contradicción C-P14-3.

---

## 4. Decisiones de Diego transcritas: cifras del techo

| Criterio | Pasa | Bloquea | Fuente |
| :--- | :--- | :--- | :--- |
| P39 (RF-CAL-14) | de +10 % a **+18,0 %** | más de +18 %, y **cualquier valor negativo por debajo de −10 %** (por ejemplo −12 %, aunque sea mejor en valor absoluto) | `Decisiones.java:57-58` |
| P49 (RF-CAL-14) | de −10 % a **−13,0 %** | por debajo de −13 %, o positivo por encima de +10 % | ídem |
| RMS de tipo I (RF-CAL-15) | hasta **14,5 %** | más de 14,5 % | `decisiones.csv:8` |

- **P39 y P49** coinciden con la fila "PA-24 margen" de Diego (`DECISIONES…:19`). El bloqueo por signo contrario
  falla del lado seguro. Se aparta de la letra ("un valor mejor que el aceptado siempre pasa"), pero con el b a
  +15 % no es un caso realista.
- **RF-CAL-15 hasta 14,5 %: no lo dijo Diego** (P14-08). La fila "PA-24 ampliada" habla de un RMS "previsto en
  11,5 %" (`:18`). El ±3 de la fila siguiente nombra sólo P39 y P49 (`:19`).
- La tarjeta del b sigue rotulada "RF-CAL-14 (PA-24)" (`TablaCalibracion.java:196`) y no menciona RF-CAL-15.
- REMEDIDA-b = RF-CAL-18 (`decisiones.csv:10`) y la exclusión de P81 (`:9`) coinciden con las filas de Diego
  (`DECISIONES…:17`, `:20`).
- La app no nombra ningún patrón que Diego no haya nombrado.
- **Hueco de pruebas:** ninguna prueba de flujo usa el alcance real del APK. El b simulado es una recta perfecta
  por el oscuro (`FlujoCalibracionTest.java:260-269`), así que la dispensa no se ejerce nunca de extremo a extremo.

---

## 5. Pregunta 4: la decisión propia del desarrollador sobre "Rechazar"

**Qué hace.** Si al rechazar falla alguna restauración, sea la de un código en ESCRIBIENDO o la de uno ya escrito,
el acta **no se cierra**. Devuelve "NO se rechaza: RESTAURACIÓN NO VERIFICADA… El acta sigue abierta"
(`FlujoCalibracion.java:1295-1300`). `acta.rechazar` sólo se llama cuando todo se ha restaurado (`:1301`).

**Juicio: es la decisión correcta, y más segura que la de la 3.6.14** (cerrar como RECHAZADA con "SIN
RESTAURAR", P13-05).

- Con el acta abierta no se puede aceptar: `motivoNoAceptable` exige que la restauración esté verificada
  (`Acta.java:627-638`).
- "Calibrar todo" no arranca con ella (`FlujoCalibracion.java:1334-1335`).
- El estado desconocido del código queda **visible**, y no enterrado en un acta cerrada que la siguiente no
  coteja.
- En SLV-002, además, el 8, el b y el 5 están hoy **en fábrica**: la máscara es 0003 (`HUELLAS.txt`, entrada de las
  15:10). "Restaurar" es por tanto `#F,k` (`Ops.java:124-127`), y desde Avanzado hay siempre una salida.

**Huecos (no bloquean, se rodean en campo):**

| ID | Hueco | Evidencia |
| :--- | :--- | :--- |
| **P14-05** | Con el acta abierta por un rechazo fallido **se pueden escribir otros códigos en ella**. Un código CONFORME con `restauracionFallida` cuenta como resuelto, y "Continuar / un código" deja marcar el 5 con el b en estado desconocido | `Acta.java:161-164`, `:384-388`; `FlujoCalibracion.java:567-570` |
| **P14-06** | El mensaje dice "pulse Rechazar o **Continuar** para reintentarlo". Pero "Continuar", con un corte durante `#S`, entra en `resolverCorte`: **puede dar el código por escrito y seguir con la re-medida, u ofrecer repetir la escritura**. Continúa la calibración que el operador quería rechazar. Además, el motivo del rechazo no se anota si la restauración falla | `:1298-1299`; `:945-974`; `:1301` |
| **P14-10** | No hay salida terminal. Si la restauración falla siempre, el acta queda abierta sin fin y bloquea también "Rehacer" en el banco. No existe el cierre "RECHAZADA SIN RESTAURAR, firmado por Diego" | `:1385-1389` |

**Con "Calibrar todo", un rechazo del b no toca el acta ACEPTADA del 8**: `rechazar` sólo recorre el acta en curso.
Pero nadie vuelve a leer el 8 (P14-02).

---

## 6. Hallazgos nuevos

### 6.1 Flujo de calibración

| ID | Hallazgo | Evidencia | Gravedad |
| :--- | :--- | :--- | :---: |
| **P14-01** | **El atajo de T-C41 no se anula cuando debe.** La bandera `encendidoReciente` se pone a true en la persistencia y sólo vuelve a false al escribir o al restaurar dentro del flujo. **`rechazar()` restaura (escribe `#F` o `#S`) sin anularla.** Tampoco caduca con el tiempo ni con una caída del enlace, no está limitada a "Calibrar todo" y no queda en el acta: el acta del b no dice que su apagado fue el de la persistencia de otra acta. Contradice su propio javadoc ("sin escribir nada después") | `FlujoCalibracion.java:163-167`, `:901-902`, `:1020`, `:1049`, `:1187`; `rechazar` en `:1268-1294` | Media |
| **P14-02** | **Ningún acta posterior vuelve a comprobar los códigos aceptados en esta app.** `heredados()` es fijo: sólo el 1 y el 2 (`TablaCalibracion.java:234-242`). Tras aceptar el 8, ni el T-C41 ni la persistencia del b o del 5 leen el 8. Si el `#SC` del 8 o los `#S` del b y del 5 dañaran su EEPROM, la app no lo vería | `FlujoCalibracion.java:1193-1201` | **Media-Alta** |
| **P14-03** | O-10 sigue abierto y se agrava. `aceptadoAntes` da por bueno el 8 aunque después se haya hecho `#F,8`. Y ahora la tarjeta de un código aceptado no tiene casilla, y "Calibrar todo" exige desmarcarlo: **el 8 no se puede recalibrar desde la pantalla** | `Campanas.java:336-357`; `FlujoCalibracion.java:589-592`, `:1346-1348`, `:1392-1393` | Media |
| **P14-04** | **"Calibrar todo" acepta cada acta sin que nadie la vea** (`persistirYAceptar`, `:1372-1379`) y sigue sola con el siguiente código. Pierde la pausa de C-3 ("ante cualquier conducta no explicada, Parar aquí o Rechazar"). La conformidad (nombre y nota) se escribe **antes** de ver los resultados, y una sola nota vale para los tres códigos (`:780-787`) | `:1356-1367` | **Media-Alta** para la primera ejecución real |
| P14-07 | El ZIP no se exporta en tres caminos: el acta a medias que acepta el preámbulo; un corte en el b con el 8 ya ACEPTADO, que acaba en "Calibrar interrumpido…" sin decir que el 8 se aceptó; y siempre, porque el ZIP va al final de la sesión y no tras cada acta | `CalibrarActivity.java:361-373`; `FlujoCalibracion.java:1326-1338` | Media |
| P14-08 | El margen de ±3 sobre RF-CAL-15 (hasta 14,5 %) lo pone la app | §4 | Media (Diego) |
| P14-11 | `TablaCalibracion.VERSION` incluye "APK 3.6.15" (`TablaCalibracion.java:33`). **Cualquier 3.6.16 deja sin aceptar un acta abierta con las mismas reglas.** No se mira hasta aceptar, así que el preámbulo hace el apagado antes de descubrirlo | `FlujoCalibracion.java:400-403`, `:652-654` | Baja; condición de campo |
| P14-12 | La comprobación previa no ve que el 8 quedará sin aceptar (§2.3) | `:1349`; `:449-453` | Baja |

**Juicio sobre los 4 apagados en lugar de 6 (T-C41).**

- **Dentro de una misma pulsación de "Calibrar todo", sin rechazos por medio, no debilita T-C41 de forma
  material.** El T-C41 que importa, el del primer `#S`, es el del 8, y tiene apagado propio. Entre la
  persistencia del 8 y el T-C41 del b no hay más escritura de curva que el `#SC`. El T-C41 del b repite `#V#`
  (máscara), `#G` y `#E` de los heredados 1 y 2.
- **Sí lo debilita en tres puntos:**
  1. el 8 recién aceptado no entra en la comprobación del b ni del 5 (P14-02);
  2. la bandera sobrevive a un rechazo con restauración, a una reconexión y al paso del tiempo (P14-01);
  3. el acta del b no dice de dónde salió su apagado.
- **Aceptable con la condición 5 de §7:** apagado y Pruebas al final. La corrección, para la 3.6.16, es que la
  bandera viva sólo dentro de `calibrarTodo`, que se anule con cualquier escritura y con cualquier caída del
  enlace, y que se anote en el acta.

### 6.2 Banco, protocolo y ZIP

| ID | Hallazgo | Evidencia | Gravedad |
| :--- | :--- | :--- | :---: |
| **P14-B01** | **Al importar no viaja el tipo de cola.** El destino se queda en COMPLETO y lee los PASO de una campaña representativa con los órdenes de la cola completa. Así, el orden 33, que es el OSCURO del final en la representativa, pasa a ser P121 AJUSTE. Quedan mal emparejados el ancla, `calibrable` y la s_rep | `ImportadorCampana.java:162-176`; `Campana.java:165`; `BancoActivity.java:324-336` | **Alta** si se importa; nula si no |
| P14-B02 | El acta aceptada no cita el SHA-256 del ZIP de soporte (RF-APP-51). El soporte se genera después de aceptar y su hash sólo queda en el registro | `Campanas.java:558-595`; `CalibrarActivity.java:418-426` | Media |
| P14-B03 | El md5 de la cola se guarda en la campaña, pero nunca se compara al cargarla | `Campana.java:476`; `BancoActivity.java:337-338`; `CalibrarActivity.java:121-122` | Media |
| **P14-B04** | El modo rápido baja a 1 × 4 los AJUSTE del 8 y del b, los RE-MEDIDA del banco y P81, frente a P10-C3 y `PLAN:179-187` | §3.1 | **Media-Alta** (Diego) |
| **P14-B05** | Con K = 1 la reproducibilidad por patrón no se evalúa, y la app no lo dice | `Veredicto.java:107-108` | Media |
| P14-B06 | La A5 va a K = 5 donde el plan dice 3, también en modo "preciso" | `Protocolo.java:22-23`; `PLAN:191-193` | Baja (a favor) |
| P14-B08 | QA-3614-03 no cubre el OSCURO ni la A5 del acta, y da falsos avisos | `FlujoCalibracion.java:336-350`, `:1391` | Media |
| P14-B09 | "Primera vez en el equipo" significa sólo que hay otro fichero de campaña de esa MAC **en ese teléfono** | `Campanas.java:120-134` | Media |
| P14-B10 | Un ZIP incremental suelto: el primero se importa como si fuera la campaña entera; los siguientes dan un mensaje que no explica que son incrementales. **El aviso B de QA-3614 ("importar sólo el último ZIP") ya es falso: sólo sirve el de soporte** | `ImportadorCampana.java:52-63`; `CampanaActivity.java:735-744` | Media |
| P14-B11 | RF-APP-50 se cumple a medias: el estado avanza aunque el ZIP no se comparta, no hay script de reconstrucción y el tope de 10 kB no se ha medido con los ZIP reales | `Campanas.java:539-542`; `PaquetesZip.java:150-179` | Media |
| P14-B07, B12-B15 | Protocolo mal rotulado en `resumen.txt` y en el acta; T-C38 mezcla posiciones; `AdminActivity` usa la cola completa para la s_rep; textos de RF-APP-49; el ZIP de soporte filtra las tramas por hora y no por MAC | Informe del subagente, contrastado por muestreo | Baja |

### 6.3 Alias de serie (pregunta 3)

**¿Hay alguna vía por la que se mezclen equipos o se acepte un acta de otro equipo? No.**

La identidad es la **MAC**, y las dos protecciones fallan hacia el bloqueo:

- **La campaña.** El fichero lleva la MAC en su nombre (`Campanas.java:57-58`). Una campaña sin MAC no se abre
  (`:62-63`). Una serie o un ZIP de otra MAC se rechazan (`Campana.java:515-517`; `ImportadorCampana.java:78-82`).
- **La tabla y las decisiones.** `TablaCalibracion.canonico(historial, mac)` devuelve "SLV-002" sólo con la MAC
  `00:21:13:05:19:3B` (`TablaCalibracion.java:37-65`; `FlujoCalibracion.java:199-204`).
  - El Nordeste, con otra MAC, abre su propio fichero y no recibe ni la tabla ni las decisiones
    (`FlujoCalibracionTest.java:1068`).
  - `aceptadoAntes` sólo mira las actas cuya clave lleva esa MAC (`Campanas.java:336-355`).

**Vías que bloquean o que pierden trazabilidad, todas dentro del mismo equipo:**

| ID | Hallazgo | Evidencia | Gravedad |
| :--- | :--- | :--- | :---: |
| **P14-S01** | **"Nueva campaña" después de renombrar deja el equipo sin poder calibrar.** La nueva se abre con la serie del nombre Bluetooth y sin RENOMBRA. `claveRenombrada` no ve las campañas archivadas, porque terminan en `_archivada_…`. `#GN#` = SLV-002-2026 ya no está en el historial, así que la previa bloquea (`FlujoCalibracion.java:261-267`) y Avanzado se niega a renombrar (`:1424-1425`) | `CampanaActivity.java:799`; `Campanas.java:137-156` | **Alta** (bloqueo) |
| **P14-S02** | **Renombrado a medias.** El equipo acepta `#SN`, pero se pierde el `#GN#`, o la campaña está cerrada, o la app muere antes de RENOMBRA. Resultado: el equipo queda en SLV-002-2026 y el diario no lo sabe. Mismo bloqueo que S01, y el mensaje "la serie NO quedó cambiada" es falso | `FlujoCalibracion.java:1418-1437`; `Campana.java:411-413` | **Alta** (bloqueo) |
| P14-S03 | La rama "Alta" de Avanzado envía `#SN` sin RENOMBRA y sin mirar la campaña si `#GN#` no se leyó en las Pruebas | `AdminActivity.java:600-611` | Media |
| P14-S04 | Si el nombre Bluetooth se cambia a `…_SLV-002-2026`, la campaña se parte en dos ficheros y el ZIP de soporte al cerrar exporta la equivocada | `Campanas.java:70-77`; `CampanaActivity.java:780` | Media |
| P14-S06 | Se puede renombrar con un acta en curso. El acta sale con la serie antigua | `FlujoCalibracion.java:838` | Media |
| P14-S05, S07-S11 | El acta en texto no viaja en el ZIP con el nombre Bluetooth nuevo; un diario importado sin líneas SERIE se salta la comprobación de MAC al copiar sus PASO; la atadura por MAC es una constante del código (`TablaCalibracion.java:37-39`); no se comprueba si la serie ya la usa otro equipo; los ZIP no llevan la MAC en el nombre (`Campanas.java:516`) | Informe del subagente | Baja o latente |

---

## 7. Condiciones

### 7.1 Antes de ir al equipo (Diego, una línea cada una en `DECISIONES-Diego-2026-09-19.md`)

1. **Protocolo del banco para lo que se escribe** (C-P14-1). O bien "preciso" (K × M de la cola) en los patrones
   del 8, del b y del 5, que cuesta unos 15 minutos más (§3.3). O bien Diego acepta por escrito 1 × 4 en ellos,
   sabiendo que:
   - la σ de la curva del b pasa del 2,2 % al 5,0 %;
   - la re-medida del b deja pasar hasta un ±16 %.

   **Recomendación: "preciso".**
2. **Margen de RF-CAL-15:** si el RMS de tipo I del b se dispensa hasta 14,5 % o sólo hasta 11,5 % (P14-08).
3. **Qué banco del tipo I vale** (C-P14-3): el de las 15:10, a 3 × 3 con la 3.6.11, o uno nuevo con la 3.6.15.
   Si se usa el de las 15:10, Diego acepta por escrito que el banco y la re-medida no llevan los mismos disparos
   por colocación (P9-B3).

### 7.2 En el campo (operador)

1. **Banco:** si Diego decidió "preciso", **desmarcar "rápido"** antes de medir el amarillo tipo I, el rojo tipo I y
   el azul. Medir el banco entero en el mismo teléfono y **no importar ZIP de otro teléfono** (P14-B01).
2. **Mirar la A5 del inicio y la del final de cada sesión** antes de calibrar. Si cambia más del 2 %, no calibrar y
   avisar a Diego (C-1).
3. **Un código cada vez.** En "Calibrar todo", marcar **sólo el 8**. Al acabar, leer su acta. Después marcar sólo el b,
   y después sólo el 5 (P14-04). **No salir de la pantalla entre uno y otro**: así se aprovecha el apagado anterior y
   siguen siendo 4 apagados.
4. **Apagar y encender cuando la app lo pida y esperar a ver en la app que se cae la conexión.** Si sale "T-C41
   FALLA", parar y avisar a Diego.
5. **Al terminar los tres:** apagar, encender, pasar "Pruebas del equipo" y comprobar que la prueba 5 marca el 8, el
   b y el 5 como calibrados. Si alguno sale "fábrica" o distinto de su acta, avisar a Diego (P14-02).
6. **Si "Rechazar" dice "NO se rechaza: RESTAURACIÓN NO VERIFICADA":**
   - no pulsar "Continuar" y no marcar ningún otro código;
   - volver a pulsar "Rechazar";
   - si sigue igual, en Avanzado hacer `#F,<código>` (el 8, el b y el 5 estaban en fábrica), anotarlo y avisar a
     Diego;
   - después de cualquier rechazo, **salir de la pantalla "Calibrar" y volver a entrar**, para que el siguiente
     código vuelva a pedir el apagado (P14-01, -05 y -06).
7. **Después de cada acta aceptada, pulsar "ZIP de soporte" y comprobar que se abre el diálogo de compartir.**
   Apuntar a mano en el acta el SHA-256 que muestra (P14-07 y -B02).
8. **Renombrar la serie a SLV-002-2026 al final**, con las tres actas ACEPTADAS y el ZIP de soporte ya compartido.
   Nunca con un acta en curso.
   - Tras renombrar: **no pulsar "Nueva campaña"** y **no cambiar el nombre Bluetooth** del equipo.
   - Si el renombrado da error, no reintentar: avisar a Diego (P14-S01, -S02, -S04 y -S06).
9. **No instalar otra versión de la app a mitad de la sesión** (P14-11).
10. Anexo RF-CAL-43 a mano, como en P13: SHA-256 del ZIP, A5, deriva y nota de C-CAL-15 en el acta del 8.

### 7.3 Para la 3.6.16 (no bloquea esta sesión)

| Qué | Resuelve |
| :--- | :--- |
| Heredados = el 1 y el 2 más las curvas de las actas ACEPTADAS del equipo | P14-02, P14-03 |
| Bandera de T-C41 local a `calibrarTodo`, anulada con cualquier escritura o caída del enlace, y anotada en el acta | P14-01 |
| Pausa de confirmación tras cada acta en "Calibrar todo" | P14-04 |
| Bloquear otras escrituras con un acta de rechazo fallido; cierre terminal "SIN RESTAURAR" firmado; quitar "Continuar" del mensaje | P14-05, -06, -10 |
| Exportar el soporte tras cada acta y citar su SHA-256 | P14-07, -B02 |
| El tipo de cola viaja al importar, y se compara el md5 al cargar | P14-B01, -B03 |
| Aviso visible cuando K = 1 deja un criterio sin evaluar | P14-B05 |
| `claveRenombrada` sobre las campañas archivadas; recuperar un renombrado a medias; bloquear el renombrado con un acta en curso | P14-S01, -S02, -S06 |
| `VERSION` sin la versión del APK | P14-11 |
| Pruebas de flujo con el alcance real de `decisiones.csv` y un b con P39 a +15 % | §4 |

---

## 8. Contradicciones

| ID | Contradicción | Fuentes | Estado |
| :--- | :--- | :--- | :--- |
| **C-P14-1** | PROTOCOLO-MIN (1 × 4 salvo A5 y OSCURO) frente a P10-C3 y `PLAN:179-187` (8, b, re-medidas y P81 a K = 5) | `DECISIONES…:23`; `Protocolo.java:21-25`; `BancoCola.java:200` | **Abierta. Diego** (§7.1-1) |
| **C-P14-2** | s_rep 2,24 % (A5 de la mañana) frente a 1,18 % (A5 de las 15:10) | `PLAN:47`; `resumen.txt:229` del ZIP de las 15:10 | **Abierta. Se cierra midiendo** la A5 del banco |
| **C-P14-3** | Banco del tipo I de las 15:10 a 3 × 3, frente a 5 × 4 en la cola y en la re-medida | diario de las 15:10, `:159-200`; `cola_banco_P1-P132.csv:52-55`, `:97-99` | **Abierta. Diego** (§7.1-3) |
| **C-P14-4** | El margen de RF-CAL-15 en `decisiones.csv` frente a las palabras de Diego | `decisiones.csv:8`; `DECISIONES…:18-19` | **Abierta. Diego** (§7.1-2) |
| C-P14-5 | Aviso B de QA-3614 ("importar el último ZIP") frente a los ZIP incrementales | `QA-App-3.6.14.md:31`; P14-B10 | Cerrada aquí: **sólo se importa el de soporte** |
| C-P14-6 | Las colas de `06_Calibracion/` en el disco llevan CRLF, y sus md5 no son los de `BancoCola.java:28-31`. Sin `\r`, son idénticas byte a byte | Volcado del subagente | Observación: el md5 se calcula sobre el blob, no sobre la copia de Windows |
| C-P12-2 | El flujo, fuera del equipo del cliente | P11-M7 | Abierta. Se cierra con esta sesión |

---

## 9. Cómo se ha comprobado

**Aquí, en el código:**

- `Protocolo`;
- `Decisiones.cubre`;
- `decisiones.csv`;
- de `FlujoCalibracion`:
  - `calibrarTodo` y `persistirYAceptar` (`:1320-1379`);
  - `heredados` (`:895-905`);
  - la bandera de T-C41 (`:160-167`, `:1020`, `:1049`, `:1187`);
  - `rechazar` (`:1268-1302`);
  - `renombrarSerie` (`:1418-1437`);
  - la previa de identidad (`:255-267`);
  - `kBanco` (`:1068-1075`);
- `TablaCalibracion.heredados` y `VERSION`;
- `Veredicto.evaluarColocaciones`;
- `Anclas.sRep`;
- `Remedida3611.criterios`;
- `Campanas.abrir`, `claveRenombrada` y `hayOtraCampanaDeEsteEquipo`;
- `CampanaActivity.nueva`.

**Aquí, con datos:**

- md5 del APK;
- el ZIP de las 15:10 descomprimido: series S006-S013 y `resumen.txt:224-229`;
- las cifras de §3 con la fórmula del plan y `sigma_curva`, con K modificada.

**Subagentes:**

- uno de volcado (Sonnet): export con `git archive`, compilación, 198 pruebas, T-S00, comparación del APK
  desempaquetado, `aapt` y `apksigner`;
- tres de juicio: flujo, alias y banco. Sus hallazgos de gravedad media-alta o alta se han vuelto a abrir en el
  código. Los de gravedad baja se recogen como los dieron, marcados como tales.

**No hecho:**

- no se ha medido nada;
- no se ha ejecutado la app en un teléfono;
- no se ha comprobado si la 3.6.15 da por HECHO el banco del tipo I de las 15:10 (C-P14-3).
