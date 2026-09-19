# QA de la app 3.6.17 ("Tomar muestras" en Representativo, cola cambiable, arreglos de P15)

**Nada de esto se ha probado en un teléfono real ni contra el equipo físico SLV-002.** Todo sale de leer el
fuente, ejecutar la JVM contra `EquipoSimulado` y usar el ZIP real del banco de las 15:10. Un documento que
cuadra en papel no es un documento medido.

**Veredicto: NO APTO para entregar y calibrar SLV-002 tal como está.** El camino feliz funciona de principio a
fin y **el defecto Alto de la 3.6.16 (B-01) está cerrado en el caso que lo definía**, pero al intentar romperlo
salen **dos defectos de severidad Alta nuevos**:

- **QA-3617-01.** La puerta de los equivalentes sigue abierta por el otro lado. `BancoPrevio` sólo cierra los
  equivalentes **que son paso de la cola** y los **excluidos por decisión**; en la cola REPRESENTATIVA los demás
  miembros del grupo quedan fuera de la cola y **donan su medida a un paso de AJUSTE**. Reproducido: P126 dona a
  P127 (AJUSTE del 5) y el 5 se ajusta con 3 patrones de 4. Basta con pasar a COMPLETO, medir P126 y volver.
- **QA-3617-02.** Un corte del enlace entre el `#SC` y el `#GC#` deja la **fecha de calibración escrita en el
  equipo** aunque el acta acabe **RECHAZADA** y el código se restaure. La app enseña luego esa fecha como
  vigente. Nada dentro de la app la borra.

Y un tercero, Medio, que degrada un arreglo declarado: la fila `FRASE-DIEGO` **no existe** en el
`decisiones.csv` del APK, así que "Cerrar sin restaurar" (F-02) se firma **sólo con el PIN del equipo**, que es
el mismo que el operador ya ha tecleado para entrar en Avanzado.

**Los tres se rodean** (§1). Ninguno de los dos Altos dispara con los datos de hoy en el camino recto; los dos
están a un botón de distancia.

- **Revisado:** `f5145ed`, HEAD, árbol limpio en `03_App_Movil/` (`git diff --stat HEAD -- 03_App_Movil/RetroV36/app/src`
  vacío). APK `03_App_Movil/RTV-V3.6.17.apk`, md5 `fc016afab69142b347dc33e106f44606`; `RTV-V3.6.17-3617.apk`
  tiene el mismo md5. Confirmado con `git log` y `md5sum`.
- **Fecha:** 19-sep-2026, noche. Método ISTQB. **Sin cambios de código**: las pruebas propias están fuera del
  árbol (`D:\tmp\qa3617\src\com\dpi\retrov36\`), compiladas contra las clases del propio proyecto.
- **Escalas** de `QA-App-3.6.12.md:21-24`. **Alta**: bloquea al operador o deja un dato del acta erróneo.
  **Media**: cuesta tiempo o induce a error.
- **Rutas** de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`; las de prueba, a
  `03_App_Movil/RetroV36/app/src/test/java/com/dpi/retrov36/`; los assets, a
  `03_App_Movil/RetroV36/app/src/main/assets/`.
- **Dos correcciones al encargo, comprobadas:** el repositorio V3.6 **no tiene `CLAUDE.md`** (`ls`, `find` y
  `git ls-files | grep -i claude` coinciden: sólo hay `.claude/skills/`), así que rigen las reglas globales; y
  la 3.6.17 **no se entrega**: la sustituye la rama `rtv-1.0`, que la lleva dentro. Este documento deja
  constancia del estado de la 3.6.17 y de lo que la rama `rtv-1.0` tiene que cerrar.

---

## 1. Condiciones de uso mientras los defectos sigan abiertos

| # | Condición | Por qué |
| :---: | :--- | :--- |
| A | **No cambiar el banco a COMPLETO.** Dejar "Banco: REPRESENTATIVO". Si alguien lo cambió, medir ahí sólo lo que la app pida y, al volver a REPRESENTATIVO, comprobar que el título vuelve a decir los patrones que faltaban | QA-3617-01 |
| B | **En el acta del 5, leer la línea "patrones 5:".** Tiene que poner exactamente `P18, P67, P112, P127`. Si aparece otro nombre (P126, P82, P17, P111, P96, P97, P66), **Rechazar** y avisar a Diego | QA-3617-01 |
| C | **Si el enlace se cae durante "Calibrar", antes de seguir mirar la fecha del equipo en Pruebas.** Si dice la fecha de hoy y ninguna acta salió ACEPTADA, el equipo lleva una fecha que no corresponde: no entregar hasta cerrar una calibración de verdad | QA-3617-02 |
| D | **"Cerrar sin restaurar" y "Liberar" sólo los teclea Diego.** Hoy valen con el PIN del equipo, que el operador conoce: la app no distingue quién firma | QA-3617-03 |
| E | **Cambiar la serie una sola vez y antes de calibrar**, con el equipo cerca. Si dice que no coinciden `#GN#` y la campaña, no calibrar | S-04 (cerrado, pero el aviso sigue siendo el rodeo) |
| F | Siguen vigentes las condiciones 2, 3 y 5 de `REVISION-P15-QA-3.6.16.md` §1 | — |

---

## 2. Pruebas JVM y APK

### 2.1 El recuento del commit es cierto

| Comprobación | Resultado |
| :--- | :--- |
| Suite del árbol de trabajo | **OK (267 tests)**, 4,6 s. JDK 11.0.24, `compileDebugUnitTestJavaWithJavac --offline`, JUnit 4.13.2 a mano (`03_App_Movil/RetroV36/README.md:36-47`) |
| Suite desde un **export limpio** de `f5145ed` (`git -c core.autocrlf=false archive`, en `D:\tmp\exp3617`) | **OK (267 tests)**, 3,2 s. Segunda verificación, árbol independiente |
| Recuento por `@Test` en el fuente | 266 con `@Test` + 1 con `@org.junit.Test` (`RupturaBancoTest.java:141`) = **267**. Coincide |
| Clases | 23 (24 ficheros en `src/test/`, uno es `EquipoSimulado.java`). Coincide con `README.md:32` |
| `@Ignore` / `Assume` | Ninguno |

**El commit dice la verdad en la cifra.** Lo que no dice es de qué sirve una parte de ella: §2.2.

### 2.2 Las pruebas de ruptura llevadas al árbol **no comprueban nada**

Las cinco clases `Ruptura*` abren con el mismo comentario: *"Imprime lo observado (SALIDA) y, donde la 3.6.17
corrige el defecto, lo comprueba con assert."* **En tres de las cinco, esa frase es falsa.**

| Clase | Tests | Líneas | Líneas con `assert`/`fail` | Qué comprueba |
| :--- | :---: | :---: | :---: | :--- |
| `RupturaBancoTest` | 1 | 249 | **0** (la única coincidencia de "assert" es el comentario de la línea 4) | Nada. Sólo imprime |
| `RupturaE2ETest` | 3 | 129 | **0** (ídem) | Nada. Sólo imprime |
| `RupturaEquivTest` | 1 | 32 | **0** (ídem) | Nada. Sólo imprime |
| `RupturaFlujoTest` | 9 | 497 | 5, todas en `r04` (`:357-358, :361, :370-371`) | F-02 y F-03. Los otros 8 tests sólo imprimen |
| `RupturaSerieTest` | 15 | 663 | 3 `fail()` (`:388`, `:412`, `:467`), en `a1`, `a3` y `c`. Los 3 `assertTrue` de `:326-330` están en `calibrarYAceptar8()`, **un helper que no llama nadie** (`grep` de `calibrarYAceptar8()`: una sola coincidencia, su propia declaración) | 3 tests de 15 |

**De los 29 tests `Ruptura*`, 25 pasan hagan lo que hagan.** Sólo aseveran cuatro: `RupturaFlujoTest.r04` y `RupturaSerieTest.a1`, `.a3` y `.c`. En particular:

- `RupturaEquivTest.verificacionYAjusteDelMismoGrupo` es **el test directo de B-01**, el Alto de la 3.6.16, y no
  tiene ninguna aserción: imprime `c.pasos().get(83)` y sigue (`RupturaEquivTest.java:25-31`).
- `RupturaE2ETest.saliendoYEntrandoDelBanco` es el caso de extremo a extremo de B-01 que el `README.md:165`
  cita como prueba ("al salir y entrar del Banco el acta del 5 lleva P18, P67, P112 y P127"). **Tampoco
  comprueba nada**: filtra líneas y las imprime (`RupturaE2ETest.java:127`).

**Lo que salva a B-01** es `Version3617Test.b01LosEquivalentesNoSaltanEntrePasosDeLaCola`
(`Version3617Test.java:161-178`), que sí asevera (`assertEquals(null, c.pasos().get(83))`). Ese test es real y
protege la regresión del caso P81 → P127. No protege el caso de QA-3617-01.

Defecto **QA-3617-04** (Media): 25 de los 29 tests `Ruptura*` no aseveran nada, y el comentario de cabecera de
tres ficheros afirma lo contrario. Un "267 en verde" que incluye 25 pruebas que no pueden fallar no es la
red de seguridad que el commit anuncia.

Observación menor: `Version3617Test.java:136` asevera `quedan <= 51 && quedan >= 40`, un rango de once
patrones, mientras `README.md:162` afirma la cifra exacta 49. El test no sostiene la cifra del README.

### 2.3 El APK sale del commit

| Comprobación | Resultado |
| :--- | :--- |
| `aapt dump badging` | `versionCode='3617' versionName='3.6.17'`, `sdkVersion:'24'`, `targetSdkVersion:'30'` |
| md5 de las dos copias | `RTV-V3.6.17.apk` y `RTV-V3.6.17-3617.apk`: **`fc016afab6…`** las dos. El encargo lo daba bien |
| Assets dentro del APK contra el árbol | Los **seis** coinciden byte a byte: cola completa `9ddb7882…`, representativa v2 `70ef3b86…`, anual `d86eddf7…`, `decisiones.csv` `7b846506…`, grupos `1fa657c9…`, catálogo `07ab9cd8…` |
| Reconstrucción desde un export limpio de `f5145ed` | `assembleDebug --offline` da un APK cuyo **contenido es idéntico entrada por entrada** (`diff -r` de los dos descomprimidos: sin diferencias, `META-INF` incluido), con `classes.dex` `9a5d877c…` y `classes2.dex` `d7453bf3…` iguales. **El md5 del contenedor NO coincide** (`abe8ae9f…`): la diferencia está en las marcas de tiempo del ZIP, no en el contenido |

**Corrección a la forma de decirlo en P15.** `REVISION-P15-QA-3.6.16.md:55` afirma "idéntico byte a byte, firma
incluida" para la 3.6.16. Aquí, con el mismo procedimiento, el APK reconstruido **no** da el mismo md5; lo que
es idéntico es cada fichero de dentro. La afirmación correcta es "el contenido del APK sale del commit", no
"el md5 se reproduce".

---

## 3. Lo que tiene que aguantar: el camino de Diego, ejecutado

Pruebas propias, fuera del árbol, **con aserciones**. Total ejecutado: **51 casos, 2 fallos** (los dos son el
mismo defecto, QA-3617-01, y son fallos buscados: la prueba asevera lo que debería pasar).

| Fichero (scratchpad) | Casos | Qué recorre |
| :--- | :---: | :--- |
| `QaBancoTest` | 9 | Decisiones tras renombrar, cambio de cola en los dos sentidos, idempotencia, equivalentes y excluidos, patrones de ajuste del 5 |
| `QaFlujoTest` | 11 | Camino feliz con aserciones, acta del 5, SHA-256, aceptar dos veces, 11 cortes, 4 muertes de la app, batería a 0, S-04, nombre Bluetooth |
| `QaE2ETest` | 3 | ZIP real de las 15:10 → banco entero → renombrar → "Calibrar todo"; lo mismo saliendo y entrando del Banco en cada paso; el 5 sin P127 |
| `QaColaTest` | 5 | Cola ANUAL y COMPLETO después del banco; cambio de cola con la sesión a medias y con un acta en curso |
| `QaHuecoTest` | 3 | El hueco de `calibrable` en la cola COMPLETO; recuperación del estado atascado |
| `QaRegresionTest` | 3 | F-04, B-05, informe de calibración en texto |
| `QaMacTest` | 4 | Otra MAC: tabla, decisiones, renombrado, importación; `#S` repetido tras un corte |
| `QaAdversarioTest` | 8 | Los cuatro Altos comunicados y las dos Medias (§4) |
| `QaImpactoTest` + `QaViaTest` | 2 | Alcance real de QA-3617-01 con el diario de las 15:10 y por la vía del COMPLETO |

### 3.1 El camino recto: pasa

| Paso | Resultado | Salida literal |
| :--- | :---: | :--- |
| Importar el ZIP de las 15:10 y abrir el Banco | **PASA** | 38 series; `tipoPorDefecto` = REPRESENTATIVO; `Quedan 51 patrones, unos 82 min`. Coincide con el recuento a mano de P15 §6 |
| Los 7 tipo I de Diego | **PASA** | `BancoPrevio: hechos=7`, 7 series ANULADAS con el motivo TIPO-I-REPETIR, y la segunda pasada anula 0 (idempotente, comprobado 11 veces seguidas) |
| Terminar el banco representativo | **PASA** | `Quedan 0 patrones, unos 0 min`, `calibrable 8/b/5 = true/true/true` |
| Cambiar la serie | **PASA** | `Serie cambiada: SLV-002 -> SLV-002-2026 (verificada con #GN#)`. Tramas del renombrado: `#GN# #L,1234# #SN,SLV-002-2026# #GN#`. **Ninguna toca el nombre Bluetooth** |
| "Calibrar todo" 8 → b → 5 | **PASA** | `Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO;`, 3 actas, `#S8=1 #Sb=1 #S5=1 #SC=1`, sin acta abierta |
| Acta: serie, patrones y soporte | **PASA** | `Equipo: SLV-002-2026 (antes SLV-002) (MAC 00:21:13:05:19:3B)` en las tres; `patrones 5: P18 (n = 20), P67 (n = 20), P112 (n = 20), P127 (n = 20)`; `ZIP de soporte (SHA-256): …` (P14-B02) |
| Lo mismo **saliendo y entrando del Banco en cada paso** | **PASA** | `seriePaso 83 (P127) = S098 -> P127`, `seriePaso 85 (P19) = S100 -> P19`. El acta del 5 sigue llevando los cuatro. **B-01, el Alto de la 3.6.16, está cerrado** |
| El 5 con menos patrones de los decididos | **PASA** | Saltando P127: `calibrable(5)=false`, `El código 5 no se escribe: no calibrable: faltan patrones de AJUSTE o RE-MEDIDA del banco`; 0 actas, `#S5=0` |
| Aceptar dos veces | **PASA** | 2ª y 3ª pulsación: `Nada que calibrar. Ya aceptados antes: 8 b 5.` Ninguna curva se reescribe |
| Otra MAC | **PASA** | `canonico` da `SLV-002 (MAC distinta de SLV-002)`: sin tabla ni decisiones (`ninguna decisión registrada`). Renombrar: el equipo se queda en `SLV-002`. Importar un ZIP de SLV-002: 0 series |

### 3.2 Cortes y muertes: ninguna curva queda escrita sin acta

Once cortes (`#L,` `#S,8,` `#SC,` `#GC#`×2 `#G,8` `#S,b,` `#S,5,` `#V#` `#E,8,` `#F,`) y cuatro muertes de la
app (en los diálogos del acta del 8, del b, del 5 y en la re-medida del b). En **todos**:

- Siempre `IOException` y, al volver a pulsar, `Sesión completa` o `Ya aceptados antes: …`.
- Un solo `#S` por código al final, salvo el `#S` cortado que se repite **una vez** (`#S,8` = 2 con la primera
  escritura fallida; una sola acta cerrada, comprobado en `QaMacTest.m4`).
- Ninguna curva escrita sin acta: la invariante "escrituras ≤ códigos en acta" se cumple en los once cortes.
- **La excepción es la fecha, no la curva:** QA-3617-02 (§4.3).

### 3.3 Cambiar de cola: lo probado

| Caso | Resultado | Salida |
| :--- | :---: | :--- |
| REPRESENTATIVO → COMPLETO → REPRESENTATIVO → ANUAL → REPRESENTATIVO, con el ZIP de las 15:10 | **PASA** | `51 / 114 / 51 / 7 / 51` patrones; **las 38 series intactas** en todo el recorrido; ida y vuelta da la misma cuenta |
| Terminado el banco, pasar a ANUAL y calibrar | **PASA (seguridad)** | `calibrable 8/b/5 = false/false/false`; 0 escrituras, 0 actas. En la ANUAL `patronesAjuste(5)` queda **vacío** y el del 8 y el b se reduce a un patrón |
| Terminado el banco, pasar a COMPLETO y calibrar | **PASA (seguridad)** | `Quedan 70`, `calibrable 8/b/5 = false/false/true`; el flujo para en el 8 y no escribe nada |
| Cambiar de cola con la sesión a medias (el 8 ya aceptado) | **PASA** | El b y el 5 no se escriben |
| Cambiar de cola con un **acta en curso** ("Parar aquí") | **PASA con reserva** | El cambio **se permite** y deja el acta abierta con el 8 ya escrito; "Calibrar todo" responde `No aceptable: Campaña de verificación anual: no se calibra (RF-APP-49)` y `puedeAceptar=false` con `motivoNoAceptar()=null`. **Se sale**: `Rechazar` funciona y restaura el 8. Queda QA-3617-06 |

---

## 4. Los cuatro Altos comunicados: reproducción

### 4.1 A1 — equivalentes de fuera de la cola. **REPRODUCIDO. Alta. → QA-3617-01**

`BancoPrevio.java:142-148` construye los candidatos de un paso con su propio patrón y con los equivalentes que
**no estén en la cola** (`enCola`) y **no estén excluidos** (`excluidos`). En la cola REPRESENTATIVA sólo el
representante del grupo es paso; los demás miembros quedan fuera y **entran como donantes**.

```
A1 grupo de P127 = G57 -> [P126, P81, P127, P82]
A1 paso 83 (P127, AJUSTE del 5) = HECHO serie S001 -> patron P126
```

- El filtro de AJUSTE (`BancoPrevio.java:154`) **no lo para**: sólo exige que el donante cumpla 5 × 4, y P126
  medido a 5 × 4 lo cumple.
- **La vía operativa es un solo cambio de cola** (`QaViaTest.v1`): pasar a COMPLETO (donde P126 **sí** es paso,
  orden 157), medirlo, y volver a REPRESENTATIVO.

```
V1 medido P126 en el paso 157 del COMPLETO (serie S001, 5x4)
V1 de vuelta en REPRESENTATIVO: paso 83 (P127, AJUSTE del 5) = HECHO serie S001 -> patron P126
V1 quedan: Quedan 56 patrones, unos 83 min
```

**Alcance medido, y una corrección a la cifra comunicada.** Contando los pasos PATRON con al menos un
equivalente fuera de la cola y no excluido:

| Cola | Pasos sustituibles | De ellos, de AJUSTE o RE-MEDIDA |
| :--- | :---: | :---: |
| COMPLETO | **0** | 0 |
| REPRESENTATIVO | **24** | **9** |
| VERIFICACION_ANUAL | 7 | 1 |

Los nueve que importan: `P28 (1, RE-MEDIDA) ← P27`, `P25 (2, RE-MEDIDA) ← P31, P26`,
`P86 (6, RE-MEDIDA) ← P132, P85`, `P67 (5, AJUSTE) ← P66`, `P81 (5) ← P126, P82`,
`P127 (5, AJUSTE) ← P126, P82`, `P18 (5, AJUSTE) ← P17`, `P19 (5) ← P17`,
`P112 (5, AJUSTE) ← P111, P96, P97`.

**La cifra que me pasaron era 18 pasos sustituibles; yo cuento 24** con el criterio de arriba. Dejo las dos
escritas: si el otro recuento excluía los pasos de VERIFICACION o los de códigos que no se escriben, la
diferencia es de criterio, no de hecho. Lo que no cambia es la parte que decide: **9 pasos de AJUSTE o
RE-MEDIDA**, cuatro de ellos del código 5, que es el que Diego va a escribir.

**Severidad y urgencia.** Con el diario real de las 15:10 **el defecto no dispara hoy**: ninguno de los
donantes del 5 tiene serie elegida (`QaImpactoTest`: P66, P126, P82, P17, P111, P96, P97, todos "sin serie").
El único donante con serie es **P27 (S036, 5 × 9) para P28 (RE-MEDIDA del código 1)**, y ahí no se usa porque
P28 tiene la suya propia (S038) y el patrón propio va primero en la lista de candidatos. También se ve vivo el
mecanismo en **P7 ← P1 (S034)**, AJUSTE del código 1, que ya cuenta hoy; el código 1 no se reescribe, así que
hoy es inocuo. Es **Alta** porque afecta al ajuste de un código que se va a escribir y porque la 3.6.17 es
precisamente la versión que pone el cambio de cola a un botón.

### 4.2 A2 — `FRASE-DIEGO` no existe en el APK. **REPRODUCIDO. Media. → QA-3617-03**

```
A2 valor(FRASE-DIEGO, SLV-002) = null
A2 filas de datos FRASE-DIEGO en el CSV = 0 (menciones totales, comentario incluido = 1)
```

`decisiones.csv:7` es un **comentario** que explica el formato; `Decisiones.leer` salta las líneas que empiezan
por `#` (`Decisiones.java:172`). La única fila la fabrica el propio test
(`FlujoCalibracionTest.java:1405`), que concatena la fila al CSV antes de leerlo. Con el CSV real:

```
A2b con una frase: Cerrar sin restaurar lo firma Diego con el PIN del equipo o con su frase registrada
                   en decisiones.csv (FRASE-DIEGO). Lo tecleado no vale: no se cierra.
A2b con el PIN:    Acta cerrada SIN RESTAURAR (firmado por Diego (PIN del equipo, verificado con #L)).
A2b acta: RECHAZADA,... "RECHAZADA SIN RESTAURAR, firmado por Diego (PIN del equipo, verificado con #L)"
```

F-02 **mejora** respecto de la 3.6.16 (ya no vale cualquier texto que contenga "diego"), pero no es una firma:
el PIN del equipo es el mismo que el operador teclea para entrar en Avanzado, y el acta escribe "firmado por
Diego". `FlujoCalibracion.java:1596-1601`; `decisiones.csv:7`.

**Mi regresión de F-02 pasaba sólo porque el test se fabrica la fila.** Confirmado: el `assert` de
`RupturaFlujoTest.java:357-361` ejercita el camino del PIN, no el de la frase; el de la frase es
`FlujoCalibracionTest` con el CSV alterado.

### 4.3 A3 — fecha de calibración huérfana. **REPRODUCIDO. Alta. → QA-3617-02**

`FlujoCalibracion.java:1474-1483`: se envía `#SC,<hoy>#` y **después** se lee `#GC#` para verificar. Si el
enlace cae en ese `#GC#`, la `IOException` sale del método con la fecha **ya escrita** en el equipo.

```
A3 fecha antes=2026-09-19 -> tras el corte, fecha en el equipo=2026-09-20
A3 1a pulsacion: IOException: CORTE en #GC#
A3 #SC enviados=1 actas cerradas=0 acta en curso=true
A3 rechazar -> Acta rechazada. código 8 restaurado;
A3 fecha en el equipo tras rechazar = 2026-09-20; actas cerradas=1
A3 acta: Estado: RECHAZADA 2026-09-20T10:00:00-0500: QA: corte entre #SC y #GC# | código 8 restaurado;
A3 fecha huerfana (escrita y acta RECHAZADA) = true
```

`rechazar()` restaura la curva pero **no toca la fecha**, y ningún camino de la app la borra. Luego
`Sesion.datosCalibracion()` (`Sesion.java:187-199`) la presenta como fecha de calibración, con su vencimiento y
su estado, y eso va a las cabeceras y a las exportaciones.

**Esto corrige a P15.** `REVISION-P15-QA-3.6.16.md:148` da **PASA** a "corte de Bluetooth en … `#SC`, `#GC#`
antes y después", mirando sólo que no se escribiera una curva de más. La curva está bien; **la fecha no**.

### 4.4 A4 — cambiar de cola deshace un "rehacer". **NO REPRODUCIDO.** Contradigo el hallazgo

`Campana.elegirCola` (`Campana.java:536-540`) limpia `pasos`, `seriePaso` e `historialPasos`, y
`BancoPrevio.java:149-163` reconstruye desde `c.elegida(pat)` — hasta ahí, el razonamiento es correcto. Lo que
falla es el último paso: **`rehacer` ANULA la serie**, y el manejador de `ANULA` (`Campana.java:1275-1288`) la
saca de `elegidas`. Con la serie fuera de `elegidas`, `BancoPrevio` no tiene de dónde resucitarla.

```
A4 tras rehacer: paso 81 = REHACER serie S001 anulada=QA: mal apoyado elegida(P67)=null
A4 tras ir a COMPLETO y volver: paso 81 = null serie null elegida(P67)=null
A4 releido el diario: paso 81 = null elegida(P67)=null
```

El paso pasa de `REHACER` a `null`: en las dos formas está **pendiente** y la cola lo vuelve a pedir. **No hay
defecto.** Lo único que se pierde es el rastro del estado `REHACER` en la vista, no la decisión: el `ANULA` con
su motivo sigue en el diario y sobrevive a releerlo.

### 4.5 Las dos Medias comunicadas: las dos, reproducidas

- **QA-3617-05 (Media).** Al cambiar de cola se pierden **todos** los controles: `BancoPrevio.java:138-139`
  sólo reconstruye pasos `"PATRON"`. Medido: `M-A controles hechos = 22; tras ida y vuelta, controles que
  vuelven a pedirse = 22 de 22` — calentamiento de 10 min, 2 baterías, 4 OSCURO, 12 A5, 2 exportaciones y la
  pausa. Son 2284 s de los 4944 s del banco representativo (P15 §6): **casi la mitad del tiempo**. Las
  *series* de la A5 y del OSCURO no se pierden (`Anclas` las busca por serie, no por paso), así que la s_rep y
  el ancla siguen; lo que se pierde es el trabajo.
  Unido a esto, el paso a REPRESENTATIVO es **mudo** en el teléfono que midió: el diario de la 3.6.11 no trae
  evento `COLA`, así que `colaElegida()` es falso y `BancoActivity.java:402-408` entra por la rama
  `cambiarCola(def, false)`, sin diálogo. El operador ve el título nuevo, pero nadie le avisa de que los
  controles vuelven a la cola.
- **QA-3617-07 (Media).** `Protocolo.deSerie` (`Protocolo.java:88-95`) toma M como el **máximo** de disparos
  por colocación, no el mínimo. Medido: colocaciones de `8 1 1 1 1` dan `deSerie = 5x8` e
  `incumple(P34, s, 5×4) = null`: **una serie con cuatro colocaciones de un solo disparo pasa por "preciso
  5 × 4"**. La decisión `PRECISO-5x9` de Diego dice "K ≥ 5 y M ≥ 4", que en su contexto (series de 5 × 9) es lo
  mismo; con series desiguales, no.

---

## 5. Defectos nuevos

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :---: | :---: |
| **QA-3617-01** | **Un equivalente de fuera de la cola tapa un paso de AJUSTE.** En REPRESENTATIVO, 9 pasos de AJUSTE o RE-MEDIDA (4 del código 5) pueden darse por medidos con la serie de otro patrón del grupo. Reproducción: cambiar a COMPLETO, medir P126, volver a REPRESENTATIVO → P127 (AJUSTE del 5) queda HECHO con la serie de P126 y el 5 se ajusta con 3 patrones de 4. Hoy no dispara con el diario de las 15:10 | `BancoPrevio.java:142-148`, `:154`; `grupos_patrones_equivalentes.csv:89-95`; `cola_banco_representativo_v2.csv:71-74`. `QaAdversarioTest.a1`, `QaViaTest.v1` | **Alta** | **P1** (aviso A y B) |
| **QA-3617-02** | **Fecha de calibración huérfana.** Un corte entre el `#SC` y el `#GC#` deja la fecha escrita en el equipo; el acta acaba RECHAZADA y la curva se restaura, pero la fecha se queda y la app la enseña como vigente | `FlujoCalibracion.java:1474-1483`; `Sesion.java:187-199`. `QaAdversarioTest.a3` | **Alta** | **P1** (aviso C) |
| QA-3617-03 | **La "firma de Diego" es el PIN del equipo.** La fila `FRASE-DIEGO` no existe en el `decisiones.csv` del APK (la línea 7 es un comentario); sólo la fabrica el test. El acta escribe "firmado por Diego" con el PIN que el operador ya ha tecleado | `decisiones.csv:7`; `FlujoCalibracion.java:1596-1601`; `FlujoCalibracionTest.java:1405`. `QaAdversarioTest.a2`, `.a2b` | Media | P2 (aviso D) |
| QA-3617-04 | **25 de los 29 tests `Ruptura*` no aseveran nada**, y la cabecera de `RupturaBancoTest`, `RupturaE2ETest` y `RupturaEquivTest` afirma que sí. El test directo de B-01 (`RupturaEquivTest`) y el de extremo a extremo (`RupturaE2ETest`) sólo imprimen | §2.2 | Media | P2 |
| QA-3617-05 | **Cambiar de cola tira los 22 controles** (calentamiento, baterías, OSCURO, A5, exportaciones): `BancoPrevio` sólo reconstruye pasos PATRON. Y el paso a REPRESENTATIVO en el teléfono que midió es mudo | `BancoPrevio.java:138-139`; `BancoActivity.java:402-408`. `QaAdversarioTest.ma` | Media | P2 |
| QA-3617-06 | **Se puede cambiar de cola con un acta en curso.** Queda un acta abierta con el código ya escrito y un "Calibrar todo" que responde con el motivo de la cola nueva. Se sale con Rechazar, pero `motivoNoAceptar()` devuelve `null`: el botón queda inerte sin decir por qué | `Campana.java:534-546` (sólo `comprobarAbierta`); `FlujoCalibracion.java:406`. `QaColaTest.c5`, `QaHuecoTest.h3` | Media | P2 |
| QA-3617-07 | **M se toma como el máximo por colocación.** Una serie de `8 1 1 1 1` pasa por preciso 5 × 4 | `Protocolo.java:88-95`, `:97-104`. `QaAdversarioTest.mb` | Media | P3 |
| QA-3617-08 | **`calibrable("5")` es cierto en la cola COMPLETO con sólo P81 medido.** En esa cola el código 5 no tiene ningún paso de AJUSTE: su único paso AJUSTE/RE-MEDIDA es **P81**, el patrón que Diego excluyó (`cola_banco_P1-P132.csv:159`). Lo para una segunda puerta (`exige todos sus pasos del banco hechos`), no la primera | `BancoCola.java:298-310`; `FlujoCalibracion.java:575-582`. `QaHuecoTest.h1`, `.h2` | Baja (defensa en profundidad aguanta) | P3 |
| QA-3617-09 | **El informe de calibración en texto no tiene ninguna prueba** y la mitad de `ExportadorFinal` es código muerto: `exportar(...)` y la interfaz `Descargas` no las llama nadie; `CalibrarActivity` sólo usa `informe(...)` y `nombreInforme(...)`. Ejercitado aquí a mano: sale bien (23 007 caracteres, tres actas, `informe_calibracion_SLV-002-2026_2026-09-20.txt`) | `ExportadorFinal.java:53-70`; `CalibrarActivity.java:529-532`. `QaRegresionTest.rInforme` | Baja | P3 |

**Observaciones (sin severidad).**

- **O-1.** En la cola COMPLETO el mecanismo de equivalentes queda **inerte**: todos los patrones son paso, así
  que `enCola` los filtra a todos (medido: 0 sustituibles). Es coherente con "el completo lo mide todo", pero
  explica por qué la cuenta del completo sube de 106 (3.6.16) a **114** con el ZIP de las 15:10: +7 por los
  tipo I anulados (B-03) y +1 por P7, que en la 3.6.16 contaba con la serie de P1.
- **O-2.** `BancoActivity.quedanCon(t)` (`:358-365`) calcularía "Quedan N" de otra cola con los `orden` de la
  actual, que no significan lo mismo. **No es un defecto**: el único uso (`:370`) le pasa la cola actual, así
  que la rama del aviso "antes de contar lo ya medido" está muerta. Verificado dos veces.
- **O-3.** Las decisiones **sí sobreviven al renombrado**: `TablaCalibracion.canonico` (`:46-66`) mapea
  `SLV-002-2026` + MAC a `SLV-002`, y todos los caminos pasan por ahí (`Base.java:200`, `BancoActivity.java:272-281`,
  `FlujoCalibracion.java:233-238`). Sin esa función, `Decisiones.excluidosDe("SLV-002-2026")` devuelve la lista
  vacía y B-01 volvería entero. Comprobado: `QaBancoTest.q1`, `.q1b`.
- **O-4.** Con MAC vacía o distinta, `canonico` devuelve `"… (MAC distinta de SLV-002)"` y **se caen todas las
  decisiones**. No es alcanzable desde el Banco: `BancoActivity.cargar` (`:385-389`) exige `s.mac` no vacía
  antes de abrir la campaña. Negativo verificado dos veces.

---

## 6. Regresión de los defectos anteriores, uno por uno

Leyenda: **C** cerrado con ejecución propia · **Cc** cerrado en código, sin ejecución posible en la JVM ·
**P** parcial · **A** abierto.

### 6.1 P15 (`REVISION-P15-QA-3.6.16.md` §7)

| ID | Estado | Evidencia de esta revisión |
| :--- | :---: | :--- |
| **B-01** (Alta) equivalencia entre pasos de la misma cola | **C, en su caso; A por el otro lado** | P81 no cubre a P127 ni P18 a P19, en las **tres** colas y tras entrar y salir del Banco N veces (`QaBancoTest.q3`, `QaE2ETest.e2`). El acta del 5 lleva los cuatro. **Pero queda QA-3617-01**: la misma clase de defecto por los equivalentes de fuera de la cola |
| **B-03** los 7 tipo I ANULADOS con motivo | **C** | `BancoPrevio: hechos=7` y 7 ANULA con `TIPO-I-REPETIR`; la segunda pasada anula 0 |
| **F-01** acta vacía se descarta | **C** | Batería a 0: `El acta quedó vacía y se descartó sola: vuelva a pulsar cuando esté resuelto.` (`FlujoCalibracion.java:922`). Al cambiar la batería, `Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO;` |
| **F-02** firma para cerrar sin restaurar | **P** | Ya no vale "no soy diego". Pero la frase registrada no existe en el APK: **QA-3617-03** |
| **F-03** bloqueo tras cerrar sin restaurar | **C** | `bloqueoSinRestaurar()` no nulo y el `#S,b` no aumenta (`RupturaFlujoTest.java:370-371`, que sí asevera) |
| **F-04** el `#SC` anula el atajo del T-C41 | **C** | Acta del 8: `T-C41 apagado: propio`; acta del b: `T-C41 apagado: propio`. La del 5 sí reutiliza el apagado del b, correctamente (ya no hay `#SC` en medio) |
| **F-05** se comparten todos los ZIP ligeros | **Cc** | `CalibrarActivity.java:508`: `todos.addAll(ligeros)`. Android; sin prueba |
| **F-06** quién acepta el acta | **A** | No declarado en la 3.6.17 y no está: `grep aceptadaPor` sin resultados. La conformidad sigue siendo una sola |
| **S-01** el revierte viaja | **C** | `Version3617Test.s01ElRevierteViajaAOtraCampana` (asevera) |
| **S-02** revierte falso | **C** | `Version3617Test.s02UnRevierteFalsoSeCorrigeAlRepetirElCambio` (asevera) |
| **S-03** `RENOMBRA` de otra MAC | **A** | Declarado "sin hacer" (`README.md:170`) |
| **S-04** serie del acta contra `#GN#` | **C** | `El equipo dice SLV-002 (#GN#) y la campaña SLV-002-2026: repita "Cambiar serie"…`; 0 escrituras (`QaFlujoTest.qf5`) |
| **S-08** diario de la 3.6.16 en la 3.6.15 | **A** | Declarado "sin hacer" |
| **B-02** rehacer un paso que contó por equivalente | **A** | Sigue: la salida de `RupturaBancoTest` lo imprime (`tras rehacer: S034 (P1) anulada=P7 mal apoyado`). La corrección de B-01 no lo toca, porque el donante sigue existiendo cuando está fuera de la cola |
| **B-04** s_rep cae a la A5 de campaña sin avisar | **A** | `Anclas.java:122-126`: el `else` final devuelve `s_rep %.2f %% (A5)` sin decir de dónde sale |
| **B-05** `BancoPrevio` no mira el veredicto | **A** | Una serie de P67 a 5 × 4 cerrada con veredicto `REPETIR` deja el paso **HECHO** (`QaRegresionTest.rB05`) |
| **P14-B02** SHA-256 del soporte en el acta | **C** | `ZIP de soporte (SHA-256): abc123def456` en el acta (`QaFlujoTest.qf1c`) |
| F-07, S-05, S-06, S-07, B-06 a B-09 (Bajas) | **A** | No declaradas en la 3.6.17 |

### 6.2 QA-3.6.15 (`QA-App-3.6.15.md` §6)

| ID | Estado | Evidencia |
| :--- | :---: | :--- |
| QA-3615-01 corte entre `#SN` y `RENOMBRA` | **C** | Cerrado en la 3.6.16 y sostenido: el renombrado manda `#GN# #L #SN #GN#` y, tras un corte, adopta la serie del equipo |
| QA-3615-02 otro teléfono tras renombrar | **Cc** | `ImportadorCampana` copia `RENOMBRA`; `S-01` cerrado con test |
| QA-3615-03 tipo de banco al importar | **C** | El evento `COLA` viaja (`Campana.java:1264-1274`); la importación del ZIP de las 15:10 da 38 series y `colaElegida=false` (el diario de la 3.6.11 no lo trae), y `tipoPorDefecto` resuelve REPRESENTATIVO |
| QA-3615-04 retomar acepta y lo dice | **C** | `Nada que calibrar. Ya aceptados antes: 8 b 5.` |
| QA-3615-05 recalibrar tras rehacer | **C** | `FlujoCalibracionTest.rehacerUnaSerieDelOchoAceptadoPermiteRecalibrarlo` |
| QA-3615-06 "pulse Rechazar" | **C** | `trasDosReMedidasNoConformesSeDiceRechazar` |
| QA-3615-07 textos de exportación | **Cc** | Android |
| QA-3615-08 tamaño del ZIP ligero | **C** | `elResumenDelZipLigeroCabeEn10kB` |
| QA-3615-09 renombrar otra MAC | **C** | `QaMacTest.m2`: el equipo se queda en `SLV-002`, no se graba |
| **QA-3615-10 banco por defecto** | **C** | **Es el cambio grande de la 3.6.17.** `tipoPorDefecto` (`BancoPrevio.java:173-181`) y `Version3617Test` (asevera). Con el ZIP de las 15:10: REPRESENTATIVO y `Quedan 51` |

### 6.3 Lo que declara el commit `f5145ed`

| Afirmación | Veredicto |
| :--- | :--- |
| "REPRESENTATIVO por defecto si el equipo ya tiene series" | **Cierto** |
| "La cola se cambia en cualquier momento (lo medido se conserva y cuenta)" | **Cierto para las series** (las 38 intactas en cuatro cambios). **Inexacto para los controles**: los 22 vuelven a la cola (QA-3617-05) |
| "Título Banco REPRESENTATIVO: quedan N" | **Cierto** (`BancoActivity.java:449-450`) |
| "B-01 (equivalentes sólo de fuera de la cola y nunca de un excluido)" | **Cierto literalmente, y ese es el problema.** "Sólo de fuera de la cola" es exactamente el agujero de QA-3617-01 |
| "B-03 los 7 tipo I ANULADOS con motivo" | **Cierto** |
| "F-01 acta vacía se descarta" | **Cierto** |
| "F-02 cerrar sin restaurar con el PIN o la frase de Diego" | **Medio cierto**: la frase no existe en el APK (QA-3617-03) |
| "F-03 bloqueo hasta liberar", "F-04 el `#SC` anula el atajo" | **Ciertos** |
| "S-01 / S-02 / S-04" | **Ciertos**, con test que aseveran |
| "P14-B02 SHA-256 del soporte en el acta" | **Cierto** |
| "Informe de calibración en texto a Download/RTV/" | **Cierto en código**, sin ninguna prueba (QA-3617-09) |
| "Rupturas del revisor al árbol (`Ruptura*Test`)" | **Cierto que están; falso que comprueben.** 24 de 29 no aseveran nada (QA-3617-04) |
| "267 tests en verde" | **Cierto**, verificado dos veces en dos árboles |

---

## 7. Qué queda sin cubrir

1. **Nada se ha ejecutado en un teléfono ni contra SLV-002.** Todo lo de `BancoActivity`, `CalibrarActivity`,
   `AdminActivity` y `CampanaActivity` se juzga leyendo el código. En concreto quedan sin probar:
   el ZIP y el informe en `Download/RTV/` por MediaStore, los selectores de compartir, el diálogo de cambio de
   cola y el aviso del banco completo.
2. **El informe de calibración en texto** sólo se ha ejercitado llamando a `ExportadorFinal.informe` a mano
   (§5, QA-3617-09). El PDF no existe todavía.
3. **La instalación encima de la 3.6.16** no se ha comprobado con `apksigner` en esta revisión; el md5 del
   contenedor no se reproduce (§2.3), aunque el contenido sí.
4. **F-05, S-03, S-08, F-06, B-02, B-04 y B-05** siguen abiertos y sin prueba en el árbol.
5. **El alcance de QA-3617-01 con datos de campo distintos** de los de las 15:10 no se ha explorado más allá
   del recuento de §4.1: si en algún momento se midieron P66, P126, P82, P17, P111, P96 o P97, hay que
   comprobar a mano la línea "patrones 5:" del acta.
6. Siguen pendientes **T-C41 y T-C43 en SLV-002 con el registro archivado** (`QA-App-3.6.13.md` §8) y **la A5
   del banco nuevo**, que es la que cierra la contradicción C-P14-2 (s_rep 2,24 % o 1,18 %).

## 8. Qué hay que cerrar en la rama `rtv-1.0`

1. **QA-3617-01:** un equivalente no puede cubrir un paso de **AJUSTE o RE-MEDIDA** de un código que se va a
   escribir, esté o no en la cola. Para los pasos de VERIFICACION, la donación puede quedarse, pero el acta
   debe decir con qué patrón se cubrió cada uno.
2. **QA-3617-02:** leer `#GC#` **antes** de dar por buena la pulsación y, si el acta se rechaza o se pierde,
   devolver la fecha anterior con un `#SC` o dejar constancia visible de que la fecha del equipo no responde a
   ninguna acta aceptada.
3. **QA-3617-03:** añadir la fila `FRASE-DIEGO` real a `decisiones.csv`, o quitar la promesa del texto y de la
   documentación y llamar a las cosas por su nombre ("PIN del equipo").
4. **QA-3617-04:** poner aserciones a los 25 tests `Ruptura*` que no las tienen, y llamar o borrar `RupturaSerieTest.calibrarYAceptar8()`, empezando por
   `RupturaEquivTest` y `RupturaE2ETest`, y añadir el caso de QA-3617-01 a `Version3617Test`.
5. **QA-3617-05:** conservar los controles ya hechos al cambiar de cola, o avisar de que se pierden y de
   cuántos minutos cuesta.
6. **QA-3617-06 y -07:** bloquear el cambio de cola con un acta en curso; tomar M como el mínimo por
   colocación, o firmar la regla tal como está.
