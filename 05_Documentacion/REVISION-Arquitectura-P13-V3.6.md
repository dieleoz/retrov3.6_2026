# Revisión de arquitectura P13 — V3.6 (app 3.6.14; firmware 3.6.2 sin cambios)

**Nada de esta revisión se ha ejecutado contra un equipo ni en un teléfono.** T-C41 y T-C43 siguen sin
hacerse en SLV-002. Lo que cambia frente a P12 es que el simulador **ya pasa T-S00**: reproduce byte a byte
las 132 respuestas deterministas del registro real T4 (§3). Todo lo demás sale de leer el fuente, de
ejecutar las pruebas y de cotejar el APK.

- **Fecha:** 19-sep-2026, tarde. Revisor adversario. Es la puerta P13: si la 3.6.14 sirve en SLV-002 para
  el banco con "rehacer", para calibrar el 8 y, después, el b y el 5.
- **Repositorio:** `D:\IT\P_RetroVertical_V3.6`, HEAD `00f667e`. Delta `5b5bd54..00f667e`: 22 ficheros,
  +2442 y −807 líneas.
- **APK:** `03_App_Movil/RTV-V3.6.14.apk`, md5 `ebfcfad87a4ec9db81673f8ce2f338a2`, recalculado aquí.
  `RTV-V3.6.14-3614.apk` tiene el mismo md5.
  - `aapt`: `versionCode='3614' versionName='3.6.14'`.
  - `assembleDebug --offline` en un export limpio de `00f667e`: el contenido desempaquetado es
    **idéntico** al del APK entregado (`diff -rq` vacío). `classes.dex` `9a5d877c…` y `classes2.dex`
    `e9987fec…`. `decisiones.csv` `ba2c1056…`. Cola `9ddb7882…` y catálogo `07ab9cd8…`, sin cambios.
  - `apksigner`: v2, `CN=Android Debug`, SHA-256 del certificado `c990adf6…075f`. Es la misma firma que
    la 3.6.13, así que la 3.6.14 se instala encima.
- **Pruebas JVM:** **169 de 169 en verde** (JDK 11.0.24, 15 clases). `FlujoCalibracionTest` tiene 38,
  `BancoRehacerTest` 5 y `TS00Test` 6. Ninguna usa `Assume`.
- **Contra:** P12 (§6 condiciones y "Para la 3.6.14", §5 y §7), P11; `QA-App-3.6.13.md`; `SPEC-V3.6.md`,
  `SPEC-Calibracion-V3.6.md` §12 y `TDD-V3.6.md` §3 ter; `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`.

Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/`. Las de `main/java/com/dpi/retrov36/` se
citan sólo con el nombre del fichero.

**Nota sobre el encargo.** El encargo cita "P12 §10". **P12 no tiene §10.** Sus condiciones están en §6
(las nueve de campo y la lista "Para la 3.6.14", `REVISION-Arquitectura-P12-V3.6.md:297-344`) y sus
contradicciones en §7. Aquí se revisan esas.

---

## 1. Veredicto

| Uso | Veredicto |
| :--- | :--- |
| **(a) Banco con "Rehacer"** | **APTO**, con dos avisos de uso (§6.1) |
| **(b) Calibrar el 8** | **APTO CON CONDICIONES**: C-1 a C-6 (§6.2). Es la primera vez que la capa Android ejecuta el flujo |
| **(c) El b** | **NO APTO en este APK, por diseño.** Falta REMEDIDA-b. Hay además un bloqueo que el desarrollador no anuncia como tal: aunque Diego añadiera REMEDIDA-b, el b **tampoco se escribiría**, porque el alcance de PA-24 no incluye RF-CAL-15 (RMS de tipo I de 11,5 %, §4.1) |
| **(c) El 5** | **APTO CON CONDICIONES sólo si Diego decide antes sobre P81** (QA-3613-07). Si no decide, no se marca |

**Sobre el simulador.** El desarrollador tiene razón en una cosa y no en la otra (§3).

- **Tiene razón en que** P12 usó la copia de `07 pruebas/19092026_1210/`, y no el ZIP de las 12:27 que
  exige `TDD-V3.6.md:1204`.
- **No tiene razón en que** eso explique el 0/25. **El 0/25 lo causó el simulador viejo.** Con el registro
  completo, el simulador de la 3.6.13 da `#G` **0/38**. Con el registro corto, el simulador nuevo da **25/25**.
- **Lo que P12 diagnosticó era correcto:** el simulador guardaba `double` y no imprimía como XC8
  (P12 §3). Eso es lo que la 3.6.14 ha arreglado.

---

## 2. Cierre de las condiciones de P12 y de la QA 3.6.13

Leyenda: **C** cerrado con código y prueba; **Cc** cerrado en código, sin prueba; **P** parcial;
**A** abierto; **Op** operativo, se cumple en campo.

### 2.1 Las nueve condiciones de campo (P12 §6)

| N.º | Condición | Estado | Evidencia |
| :---: | :--- | :---: | :--- |
| 1 | Banco completo y A5 revisada a mano (deriva) | **A → Op** | La app sigue sin calcular la deriva por A5. Sólo la del OSCURO bloquea el ancla (`Anclas.java:58-69`). **La condición sigue en pie** |
| 2 | T-C41 antes del primer `#S` | **Cc** | `heredados()` empieza apagando y encendiendo el equipo, y exige ver caer el enlace (`FlujoCalibracion.java:773-777`). Después hace `#L`, `#V#`, máscara, `#G` y `#E` de los heredados (`:778-800`). **Sin medir en SLV-002** |
| 3 | El 8 solo, primero, hasta ACEPTADA | **C** | `solo = true` para el 8 (`TablaCalibracion.java:147-148`) y el rechazo si se mezcla (`FlujoCalibracion.java:630-642`). El b y el 5 llevan `requiereAceptado = '8'` (`TablaCalibracion.java:140`, `:154`). Se busca en las actas archivadas (`Campanas.aceptadoAntes`, diff de `Campanas.java`) desde `FlujoCalibracion.java:350-358` |
| 4 | Persistencia de verdad | **Cc** | Un vigía mira el enlace mientras el diálogo está abierto. Si no lo vio caer y sigue conectado, devuelve `false` (`CalibrarActivity.java:180-209`). Falla del lado seguro. El retardo real de Android para detectar la caída **no está medido** (R-2 de la QA) |
| 5 | El b, sólo con lo que Diego dispensó | **C**, con un defecto nuevo | El alcance se limita a lo que dice `decisiones.csv` (`FlujoCalibracion.java:407-423`; `Decisiones.java:49-51`). RF-CAL-16 no se dispensa nunca. Defecto: la ventana penaliza la mejora (P13-02, §5) |
| 6 | El 5 con sus 13 pasos HECHO y el rango en el acta | **C** | `todosLosPasos` (`TablaCalibracion.java:140`) y `pasosSinHacer` (`FlujoCalibracion.java:343-349`, `:425-435`). El rango y los tipos se calculan en `:437-450`. Prueba: `el5ExigeSus13PasosYDeclaraElRango` |
| 7 | El 8 con la nota de C-CAL-15 | **Op** | Sigue siendo texto de la nota. La app no lo exige |
| 8 | Rechazar con restaurar | **C** | Rechazar restaura siempre, también el `#S` sin resolver (`FlujoCalibracion.java:1123-1161`). El "Rechazar" de Avanzado lleva a "Calibrar" (`AdminActivity.java:147-148`). **Queda O-2:** si la restauración falla, el acta se cierra igual como RECHAZADA con "SIN RESTAURAR" (`:1141-1142`, `:1153`, `:1158`). En el campo se sigue leyendo el resultado |
| 9 | Anexo RF-CAL-43 a mano | **A → Op** | Siguen faltando el SHA-256 del ZIP, la A5, la deriva por sesión y el PIN de fábrica |

### 2.2 "Para la 3.6.14" (P12 §6) y huecos de P12 §5

| Punto | Estado | Evidencia |
| :--- | :---: | :--- |
| Simulador en float32 con XC8, `#SN`, `#GT#`, `#GC,NONE#`, PIN, caducidad, EEPROM separada, T-S00 | **C** | §3. `TS00Test` (6 pruebas) |
| La persistencia exige ver caer el enlace (§5.1) | **Cc** | Como la condición 4 |
| Dispensa limitada (§5.2) | **C** | Como la condición 5 |
| El 5 sólo con todos sus pasos | **C** | Como la condición 6 |
| Pruebas de "Parar aquí", "No" y cobertura con ancla | **C** | `pararAquiDejaElActaAMediasYSeContinua` (`FlujoCalibracionTest.java:522`) y `laCoberturaDel5DependeDelAncla` (`:470-481`): sin el ancla da grado 0, con el ancla ≥ 1. **Cierra C-P12-4** |
| Borrar el código muerto de Avanzado y su "Rechazar acta" (§5.3) | **C** | −400 líneas en `AdminActivity.java`. Ya no aparecen `escribir(`, `aceptarActa` ni `propuesta == null`. "Rechazar" redirige (`:147-148`) |
| PIN bloqueado presentado como "PIN rechazado" (§5.3) | **C** | `Ops.entrar` distingue el equipo mudo, el PIN rechazado y el bloqueo (`Ops.java:49-65`) |
| Exportar el ZIP al aceptar | **Cc** | `exportarTrasAceptar` (`CalibrarActivity.java:348-350`, `:392-401`). Si falla, sólo queda en el registro (`:399`): el operador tiene que ver el diálogo de compartir |
| Deriva por A5 (§5.3, T-S21) | **A** | Condición 1 |

### 2.3 QA-3613

| ID | Estado | Evidencia |
| :--- | :---: | :--- |
| **01** Rechazar con un `#S` sin resolver | **C** | `FlujoCalibracion.java:1134-1144` restaura `acta.escribiendo()` con verificación |
| **02** PIN tras un fallo de `#L` | **Cc** | El flujo olvida el PIN ante cualquier fallo (`FlujoCalibracion.java:583-589`). La pantalla lo borra si `necesitaPin()` (`CalibrarActivity.java:345-347`). Un PIN vacío pasa a `null` (`:322`) |
| **03** Tabla y decisiones por equipo | **C** | Otra serie, todo "sólo verificar" (`TablaCalibracion.java:126-133`). `Decisiones` usa la clave id\|equipo (`Decisiones.java:117-119`, `:164-176`). Prueba: `FlujoCalibracionTest.java:885-887` |
| **04** "Rechazar" de Avanzado | **C** | Como en §2.2 |
| **05** Acta de la 3.6.12 | **Cc** | `completarDatos()` (`FlujoCalibracion.java:591-603`), llamada también desde `aceptar()` (`:1071`) |
| **06** Batería con la campaña cerrada | **Cc** | `ultimaBateria` manda (`FlujoCalibracion.java:161`, `:261`, `:560-563`) |
| **07** P81 en el ajuste del 5 | **A** (el desarrollador lo declara así) | `patronesAjuste`: si no hay AJUSTE, entran todos los patrones del código, P81 de RE-MEDIDA incluido (`FlujoCalibracion.java:282-303`). §4.3 |
| **08** Una conformidad por código | **C** | `FlujoCalibracion.java:657-661` |
| **09** Código muerto | **C** | Como en §2.2 |

**Lo que declara el desarrollador, contrastado:** QA-3613-01..06, 08 y 09 y P12 §5-§6 cerrados, y el 07
abierto. **Es cierto**, con estos matices:

- 02, 05 y 06 están cerrados en el código, sin una prueba que lo recorra desde la pantalla.
- Las condiciones de campo 1, 7 y 9 no las cierra la app y siguen vigentes.

### 2.4 El banco con "Rehacer" (petición de Diego)

**Qué hace, comprobado:**

- `Campana.rehacer` exige un motivo. Anula la serie, que queda en el diario como evento `ANULA` y nunca se
  borra, y anota el paso como `REHACER` (diff de `Campana.java`, métodos `anular` y `rehacer`).
- Una serie anulada queda fuera de:
  - `seriesDe` y `seriesA5` (`Campana.java:228`, `:242`);
  - `elegida`, y por tanto de `medidasElegidas` y del ajuste;
  - `Anclas.seriePaso` (`Anclas.java:33-34`), y por tanto del OSCURO y de la s_rep.
- `pasos()` convierte en `REHACER` un HECHO cuya serie esté anulada (`Campana.java:335-343`). Por eso un
  cierre de la app entre los dos eventos no deja un paso "hecho" con la serie anulada.
- Consecuencias:
  - `BancoCola.siguiente` lo vuelve a ofrecer (diff de `BancoCola.java:222`);
  - `calibrable` y `pasosSinHacer` no lo cuentan como HECHO (`BancoCola.java:250-262`; `FlujoCalibracion.java:430`);
  - un 8 o un 5 con un paso por rehacer no se calibra, y eso es seguro.
- La importación trae la anulada como anulada (diff de `ImportadorCampana.java:132-134`). El CSV y el
  resumen la muestran.
- La confirmación opcional "¿Era Pxx?" viene desactivada por defecto (`BancoActivity.java`,
  `prefConfirmar`). "No, rehacer" cierra la serie y la anula en el acto.
- Pruebas: las 5 de `BancoRehacerTest`. Cubren la última, una intermedia, la A5 y la s_rep, la reanudación y
  la importación.

**Observaciones (no bloquean):**

- **Rehacer no tiene requisito escrito.** No hay RF de "rehacer" ni "anular" en `SPEC-V3.6.md` ni en el TDD
  (grep sin resultados). Es una función pedida por Diego y sin especificar. Hay que añadir el RF y el caso
  de prueba del TDD.
- **Nada impide rehacer un patrón que ya entró en un acta ACEPTADA.** El acta conserva lo que usó, y la
  calibración siguiente usará la serie nueva. Es trazable, pero no se avisa.
- Rehacer una serie de la A5 del inicio cambia la s_rep de las actas posteriores. Es lo correcto; queda
  anotado en el diario.

---

## 3. ¿El simulador es fiel? ¿T-S00 pasa?

**Sí, pasa.** Un subagente de volcado exportó `00f667e` y `5b5bd54` con `git archive` al scratchpad. Ejecutó
una variante de `TS00Test`, con el mismo parseo y la misma comparación (`TS00Test.java:108-128`, `:146-160`),
en cuatro combinaciones. Las cifras de (a) coinciden con la salida del propio `TS00Test`.

| Orden | (a) nuevo + completo | (b) nuevo + corto | (c) viejo + completo | (d) viejo + corto |
| :--- | ---: | ---: | ---: | ---: |
| `#G` | **38/38** | **25/25** | **0/38** | **0/25** |
| `#E` | 70/70 | 65/65 | 70/70 | 65/65 |
| `#GC#`, `#GN#` | 2/2, 4/4 | 1/1, 3/3 | 1/2, 2/4 | 0/1, 1/3 |
| `#GT#`, `#SN` | 3/3, 3/3 | 2/2, 2/2 | 0/3, 0/3 | 0/2, 0/2 |
| `#V#`, `#L`, `#Q#`, `#S`, `#SC` | todas | todas | todas | todas |
| **Total** | **132/132** | **106/106** | 85/132 | 74/106 |

- **Los dos registros.** El registro completo es la entrada `tramas/rtv36_20260919_114644.txt` del ZIP de
  las 12:27 (md5 del ZIP `ce1f35fc…`, versionado en `06_Calibracion/SLV-002/campanas/`). Tiene 125 681 bytes.
  El corto, `07 pruebas/19092026_1210/x/tramas/rtv36_20260919_114644.txt`, es un **prefijo exacto** del
  completo: `cmp -n 87255`, comprobado aquí y por el subagente. Termina en una línea completa. **No está
  "cortado" en el sentido de dañado: es el mismo registro, a las 12:10.**
- **Quién tiene razón.** La combinación (d) reproduce exactamente las 106 peticiones y el 0/25 de P12. La
  (c) demuestra que con el registro "bueno" el simulador viejo daba 0/38. La (b) demuestra que con el
  registro "malo" el simulador nuevo da 25/25. **La causa fue el simulador, como dijo P12 §3.** El registro
  sólo cambia el denominador.
  - El comentario de `TS00Test.java:34-37` ("la copia de 07 pruebas no sirve… es la que dio las 106
    peticiones") **es cierto en el hecho y engañoso en la causa**. Debe corregirse (P13-04).
  - **P12 se equivocó en el procedimiento:** usó una fuente distinta de la que fija `TDD-V3.6.md:1204`. Se
    anota.
- **No es tautológico.**
  - La comparación es `String.equals`, byte a byte y sin tolerancia (`TS00Test.java:155`, `:167-168`).
  - El simulador imprime con su propio `Xc8.efmt` y lee con `Xc8.strtofCampo` (`EquipoSimulado.java:704-883`).
    Es un port de `01_Firmware/RetroVertical_V3.6.X/pruebas/T-A23_T-A30/emul.py:1-79`, validado allí
    contra el simulador de MPLAB.
  - Su `tramaG` es propio (`:548-555`), y también su `evaluar` (`:624-632`). No usa `Tramas.tramaG` ni
    `Ecuacion.respuestaFloat32` (`:34-37`; grep del subagente).
- **Límites de la validación:**
  - Las 38 `#G` son **14 respuestas distintas**: los 12 códigos de fábrica y los dos escritos.
  - La única elección libre, "la constante se redondea al float más próximo" (`EquipoSimulado.java:94-106`),
    se hizo mirando T4. Es lo que hace un compilador de C por norma, así que no se considera un ajuste
    a medida. Aun así, T4 no es una muestra independiente para esa elección.
  - Se excluyen 277 peticiones de medida (`e` y los códigos), como manda el TDD.
- **Contradicción C-P12-1: cerrada.** Queda a cargo del desarrollador pasar T-S00 a PASA en
  `TDD-V3.6.md:1201`, que sigue diciendo PENDIENTE.

---

## 4. Decisiones de Diego que faltan para el b y el 5

### 4.1 El b: tres decisiones, no una

1. **REMEDIDA-b (QA-3612-06 / C-3613-1).** Hoy no está en `decisiones.csv` (`:6-7`, `:9-10`), y sin ella el b
   queda en "sólo verificar" (`TablaCalibracion.java:150-163`).
   - **Recomendación: RF-CAL-18.** La otra opción, CERTIFICADO, equivale a no escribir el b. Con la recta
     anclada, P49 sale del certificado a −10,2 % (SPEC) o −11,1 % (REFORM), frente a un límite de ±10 %
     (`TablaCalibracion.java:115-117`; `Remedida3611.java:106-107`). La re-medida saldría NO CONFORME por
     construcción, y a la segunda se restauraría la curva.
   - Diego tiene que firmar sabiendo lo que implica: **la re-medida del b sólo comprueba que el patrón se
     reproduce y que el equipo aplica la curva escrita, no el certificado** (P12 §4.2).
2. **RF-CAL-15 de tipo I, que falta en el alcance.** Es el hallazgo principal de esta revisión.
   - La recta anclada del b incumple tres criterios: RF-CAL-14 en P39 (+15,3 %), RF-CAL-14 en P49
     (−10,2 %) y **RF-CAL-15, con un RMS de tipo I del 11,5 % frente al 6 %**
     (`06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md:169`; `SPEC-Calibracion-V3.6.md:786`;
     umbral en `Asistente.java:336`).
   - El alcance de PA-24 en el APK sólo trae los dos RF-CAL-14 (`decisiones.csv:9`). `noDispensados` deja
     fuera cualquier RF-CAL-15 que no figure en él (`FlujoCalibracion.java:411-423`).
   - **Con esos datos, el b saldría "FUERA de lo dispensado" aunque se añadiera REMEDIDA-b.** La única prueba
     en que el b se escribe usa un alcance inventado con `RF-CAL-15 I 10 5` y P38 (`FlujoCalibracionTest.java:416-417`).
     No es el del APK.
   - El README lo menciona como condicional ("si su RMS de tipo I sale fuera de 6 %",
     `03_App_Movil/RetroV36/README.md:135`). **Según REFORM, sale fuera.**
   - **Las palabras de Diego sólo nombran RF-CAL-14** (`DECISIONES-Diego-2026-09-19.md:12`). Por eso la app no
     se equivoca al transcribirlas: lo que falta es la decisión.
   - **Recomendación:** preguntárselo a Diego explícitamente ("¿dispensa también el RMS de tipo I, 11,5 %?").
     Si dice sí, `RF-CAL-15 I +11.5 3`. Si dice no, el b no se escribe.
3. **El margen de ±3 puntos de PA-24.** Lo fijó la app (`decisiones.csv:4-5`), no Diego.
   - **La cifra es razonable** (cálculo propio). La diferencia entre las dos cuentas del mismo P39 (SPEC
     +15,3 %, REFORM +13,0 %) ya es de 2,3 puntos. La s_rep de la A5 es de alrededor del 2,2 %.
   - **La forma no lo es: es una ventana, no un techo.** `cubre` acepta `|dev − cifra| ≤ margen`
     (`Decisiones.java:49-51`). Un P39 a +11 % (mejor que lo que Diego aceptó, pero por encima del 10 %) cae
     fuera de [+12 ; +18] y **bloquea el b** (P13-02). Es un fallo del lado seguro, pero absurdo.
   - **Recomendación:** que Diego fije el margen, y que la 3.6.15 lo interprete como techo: mismo signo y
     `|dev| ≤ |cifra| + margen`.

**Las tres van en una sola línea de `decisiones.csv`**, y ese fichero viaja dentro del APK. **Hace falta una
3.6.15 en cualquier caso.**

### 4.2 El margen de PA-24 en la re-medida

Es la misma regla, en `Remedida3611.java:110`, y tiene el mismo defecto de ventana. Con la ventana de P49,
[−13 ; −7], el caso real queda cubierto (incumple sólo por debajo del −10 %). El defecto no muerde aquí,
pero debe corregirse igual.

### 4.3 El 5: P81 dentro del ajuste (QA-3613-07 / C-3613-2)

**Hechos:**

- La cola no tiene ningún AJUSTE del 5. Por eso el ajuste usa **todos** sus patrones, 12 de VERIFICACIÓN
  más P81, que es también su patrón de re-medida (`FlujoCalibracion.java:282-303`).
- PA-14 no nombra patrones (`DECISIONES-Diego-2026-09-19.md:13`).

**Por qué importa más en el 5 que en el 8 o el b:**

- Mezcla tres tipos de lámina (IV, IX y XI).
- 10 de sus 13 x esperados son estimados, no medidos (P12 §4.1).
- Tiene 12 patrones más, así que quitar P81 no le cuesta cobertura.

Con P81 dentro, la re-medida comprueba la reproducción, no la capacidad de la recta para predecir un
patrón que no la formó.

**Recomendación:** que Diego decida **excluir P81 del ajuste del 5** en la 3.6.15. El 5 se ajustaría con 12
patrones y el ancla, y P81 quedaría como verificación independiente.

**Alternativa aceptable:** que Diego acepte por escrito que P81 entre, como P43 en el 8 y P49 en el b
(O-1 de la QA, P9-B7), y que la conformidad lo diga.

**Sin ninguna de las dos, el 5 no se marca.**

---

## 5. Defectos y observaciones nuevos

| ID | Defecto | Evidencia | Gravedad |
| :--- | :--- | :--- | :---: |
| **P13-01** | El alcance de PA-24 del APK no incluye RF-CAL-15 (RMS de tipo I 11,5 %). Con los datos de REFORM, el b no se escribe aunque se añada REMEDIDA-b. El README lo presenta como una eventualidad | `decisiones.csv:9`; `FlujoCalibracion.java:411-423`; REFORM `:169`; README `:135` | Alta para el b (bloquea); nula para la seguridad |
| **P13-02** | La dispensa es una ventana `\|dev − cifra\| ≤ margen`, no un techo: un resultado mejor que el dispensado, pero por encima del 10 %, bloquea | `Decisiones.java:49-51`; `Remedida3611.java:110` | Media (fallo seguro) |
| P13-03 | "Rehacer" no tiene RF ni caso de TDD | `SPEC-V3.6.md`, `TDD-V3.6.md` (grep de "rehacer" y "anula" vacío) | Baja |
| P13-04 | El comentario de `TS00Test` atribuye el 0/25 de P12 a la copia corta: es falso en la causa (§3). `TDD-V3.6.md:1201` sigue diciendo PENDIENTE | `TS00Test.java:34-37` | Baja |
| P13-05 | Un rechazo con una restauración fallida cierra el acta como RECHAZADA, con el código k en un estado desconocido. La siguiente acta no lo coteja (O-2 de la QA, sigue) | `FlujoCalibracion.java:1141-1158`; `:1056-1066` sólo coteja los heredados 1 y 2 y los certificados | Media (operativa: C-5) |
| P13-06 | Si falla la exportación del ZIP al aceptar, sólo queda en el registro de la app | `CalibrarActivity.java:398-400` | Baja (C-6) |

---

## 6. Condiciones

### 6.1 Banco con "Rehacer": avisos de uso

1. El motivo del rehacer se escribe con el patrón pedido y el puesto ("pedía P45, puse P40"). Queda en el
   diario y en el ZIP.
2. Se rehace **antes de calibrar** los códigos que usan ese patrón. Si el patrón ya está en un acta
   ACEPTADA, se avisa a Diego: la app no lo hace.

### 6.2 Calibrar el 8 (la primera ejecución real del flujo)

- **C-1. A5 y deriva, a mano** (P12, condición 1). Sesión 1 sin deriva, o el 8 no se calibra.
- **C-2. T-C41 al pulsar "Calibrar".**
  - Apagar, esperar hasta **ver en la app** que la conexión cae, y encender.
  - Si sale "no se vio caer el enlace", repetir esperando más con el equipo apagado.
  - Si sale "T-C41 FALLA", parar y avisar a Diego.
- **C-3. El 8 solo, hasta ACEPTADA**, con el registro de tramas y el acta archivados en
  `06_Calibracion/SLV-002/`.
  - Ante cualquier conducta no explicada, "Parar aquí" o Rechazar.
  - Nota de C-CAL-15 en la conformidad (P12, condición 7).
- **C-4. Persistencia:** lo mismo que en C-2. La app ya lo exige; el operador sólo tiene que dejar el
  equipo apagado el tiempo suficiente.
- **C-5. Si un rechazo o una re-medida dicen "SIN RESTAURAR" o "NO VERIFICADA", no se deja SLV-002 así.**
  - Se resuelve con "Continuar" antes de salir.
  - Si no se resuelve, se usa `#F,8#` desde Avanzado (para el 8, la curva anterior es la de fábrica) y se
    anota en el acta.
- **C-6. El ZIP al aceptar:** comprobar que aparece el diálogo de compartir. Si no, se exporta a mano.
  También se prepara el anexo RF-CAL-43 a mano (P12, condición 9).

### 6.3 El b y el 5

- **El b:** no se marca con la 3.6.14 (la app no lo deja). Hace falta una 3.6.15 con:
  - REMEDIDA-b;
  - el alcance de PA-24 con RF-CAL-15 de tipo I o sin él, según decida Diego;
  - el margen fijado por Diego y leído como techo (§4.1).
- **El 5:** sólo tras la decisión de Diego sobre P81 (§4.3).
  - Si Diego acepta que P81 entre, se puede hacer con la 3.6.14. Se aplican las condiciones 6 de P12 y
    C-1 a C-6, con la sesión 3 sin deriva.
  - Si decide excluirlo, hace falta la 3.6.15.
  - Recomiendo esperar a la 3.6.15 y hacer el b y el 5 en la misma acta, después del 8.

---

## 7. Contradicciones

| ID | Contradicción | Fuentes | Estado |
| :--- | :--- | :--- | :--- |
| C-P12-1 | T-S00 falla y el simulador se usa igual | `TDD-V3.6.md:1206` | **Cerrada**: 132/132 (§3). La causa fue el simulador, no la copia del registro |
| C-P12-3 | La dispensa nombra cifras y la app dispensaba cualquier incumplimiento | P12 §5.2 | **Cerrada** en el código. Nace P13-02 |
| C-P12-4 | La prueba del 5 no comprobaba la cobertura | `FlujoCalibracionTest.java:470-481` | **Cerrada** |
| C-3613-1 | Regla de la re-medida del b | `decisiones.csv:6-7` | **Abierta. Diego** (§4.1.1) |
| C-3613-2 | P81 en el ajuste del 5 | `FlujoCalibracion.java:282-303` | **Abierta. Diego** (§4.3) |
| **C-P13-1** | PA-24 en palabras de Diego: sólo RF-CAL-14. En SPEC y REFORM: RF-CAL-14 y 15 | `DECISIONES-Diego-2026-09-19.md:12`; `SPEC-Calibracion-V3.6.md:786`; REFORM `:169` | **Abierta. Diego** (§4.1.2) |
| **C-P13-2** | El margen de ±3 de PA-24 lo fijó la app | `decisiones.csv:4-5` | **Abierta. Diego** (§4.1.3) |
| C-P12-2 | El flujo, fuera del equipo del cliente | P11-M7 | Abierta. Se cierra con C-3 |

---

## 8. Cómo se ha comprobado

**Leído aquí:**

- `TablaCalibracion`, `Decisiones`, `decisiones.csv` y `TS00Test`, completos;
- de `FlujoCalibracion`: `plan`, `noDispensados`, `patronesAjuste`, `pasosSinHacer`, `calibrar`,
  `heredados`, `entrar`, `aceptar` y `rechazar`;
- de `CalibrarActivity`: la persistencia, las acciones, el rechazo y la exportación;
- de `Ops`: `entrar`;
- de `Remedida3611`: `evaluarConDispensa`;
- el diff de `Campana`, `BancoCola`, `Anclas`, `Campanas`, `ImportadorCampana` y `BancoActivity`;
- `AdminActivity`, en los botones de acta;
- la cabecera y las constantes de `EquipoSimulado`;
- las pruebas del b y del 5 de `FlujoCalibracionTest`.

**Comprobado aquí:**

- el md5 del APK;
- que el registro de las 12:10 es un prefijo exacto del de las 12:27, y el md5 de los ZIP;
- el recuento de los patrones del b en la cola (P38, P39 y P49, tipo I, `cola_banco_P1-P132.csv:97-99`).

**Un subagente, con un encargo de volcado.**

- Exportó `00f667e` y `5b5bd54`, compiló y ejecutó las 169 pruebas.
- Hizo `assembleDebug` y comparó el APK desempaquetado. También `aapt` y `apksigner`.
- Ejecutó las cuatro combinaciones de T-S00 y leyó la emulación de XC8.

Sus cifras de (a) coinciden con el informe que imprime `TS00Test`. Las de (d) coinciden con P12 §3
(106 peticiones, `#G` 0/25, `#E` 65/65).

**No hecho:**

- no se ha medido nada;
- no se ha ejecutado la app en un teléfono;
- no se ha medido cuánto tarda Android en ver caer el enlace.
