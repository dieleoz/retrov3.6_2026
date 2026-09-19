# Revisión de arquitectura P16 y QA de la app 3.6.17 (arquitecto adversario)

**Veredicto: NO APTA para entregarse tal cual a Diego.** La 3.6.17 cierra de verdad casi todo lo que
declara —**B-01 por la ruta que P15 describió está cerrado y comprobado**, los 267 tests están en verde y el
APK sale del commit—, pero **el mismo defecto B-01 vuelve a estar abierto por donantes distintos**, y el
cambio de cola a mitad, que es lo nuevo, abre dos vías más para que un patrón del ajuste quede sin medir.
Son **ocho defectos Altos**, cuatro de ellos reproducidos ejecutando código. Con las condiciones de §1 se
puede usar igualmente para terminar SLV-002; sin ellas hay dos desenlaces malos y ninguno avisa:
**el código 5 sale ajustado sin sus patrones y el acta ACEPTADA** (A-01, que es lo que P15 marcó como Alto y
dio por cerrado), y **el cambio de serie a `SLV-002-2026` puede dejar el equipo sin poder calibrarse y sin
salida en la app** (A-08), justo el paso que hay que dar en campo.

**Nada de esto se ha probado en un teléfono ni contra SLV-002.** Todo sale de leer el fuente, ejecutar la
suite en la JVM y correr cuatro sondas propias contra las clases compiladas del commit. Un documento que
cuadra en papel no es un documento medido.

- **Fecha:** 19-sep-2026, noche. Puerta **P16**.
- **Qué se revisa:** `f5145ed` "App RTV 3.6.17: tomar muestras en Representativo (cambiable a mitad) y
  arreglos de P15". **Es HEAD** (`git rev-parse HEAD` = `f5145ed1ffe84f93c879b88634833614076be644`).
- **APK:** `03_App_Movil/RTV-V3.6.17.apk`, md5 `fc016afab69142b347dc33e106f44606`.
  `RTV-V3.6.17-3617.apk` tiene el mismo md5. Comprobado.
- **Base:** `REVISION-P15-QA-3.6.16.md`, `REVISION-Arquitectura-P14-V3.6.md`, `QA-App-3.6.15.md`,
  `SPEC-App-Unica-V36-V46.md`, `SPEC-Calibracion-V3.6.md`, `SPEC-Registro-Indicador-Interventoria.md`,
  `SPEC-V3.6.md`, `PROTOCOLO-V3.6.md`, `MATRIZ-SPEC-codigo-V3.6.md`,
  `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md` (todas las filas) y
  `06_Calibracion/PLAN-Banco-Representativo.md`.
- **Rutas:** las de la app son relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.
  Los tests son de `app/src/test/java/com/dpi/retrov36/`.
- **Cómo se ha hecho:** dos subagentes adversarios con alcances disjuntos (uno el flujo de calibración y la
  serie, otro el banco y las colas) y el trabajo propio de esta puerta. Todo hallazgo Alto de los subagentes
  se ha **reabierto en el código** antes de entrar aquí; uno de ellos no se reproducía como venía descrito y
  se corrige en §7 (A-05). Las sondas están en el scratchpad y **no se han añadido al árbol**.

---

## 1. Condiciones de campo (operador, 6 líneas)

1. **No toque el botón "Banco: … (cambiar)" mientras haya una medida en curso.** Si la pantalla dice "Serie
   en curso", espere a que acabe. (A-02)
2. **Elija el tipo de banco una sola vez, al principio, y no lo cambie más.** Si tiene que cambiarlo, avise a
   Diego: hay que revisar qué quedó dado por medido. (A-02, A-05)
3. **Si pulsó "Rehacer" en algún patrón, mídalo antes de cambiar de banco.** Un cambio de banco después de un
   Rehacer puede dar por buena la medida vieja. (A-05)
4. En el acta del código 5 tiene que poner **"patrones 5: P18, P67, P112, P127"**, los cuatro. Si falta
   alguno, o aparece otro nombre (P126, P82, P17, P111, P66), pulse Rechazar y avise a Diego. (A-01)
5. Si sale "RESTAURACIÓN NO VERIFICADA" o "rechazo pendiente", no toque nada y llame a Diego. Tenga a mano el
   **PIN del equipo**: sin él y sin enlace no se puede cerrar ni liberar. (A-04)
6. Instale **sólo** `RTV-V3.6.17.apk`. **No instale `RTV-V1.0.0-rc1.apk`**, que está en la misma carpeta: si
   entra, la 3.6.17 ya no se puede poner encima sin desinstalar, y desinstalar borra la campaña. (A-07)
7. **Antes de pulsar "Cambiar serie", Diego tiene que dar el visto bueno con el equipo delante.** Si después
   del cambio la app dice "no se pudo verificar", **pare y llame a Diego: no siga ni calibre**. (A-08)

**Para Diego, fuera de campo:** antes de que nadie toque "Cambiar serie", **compruebe con el equipo delante
que `#SN,SLV-002-2026#` y luego `#GN#` devuelven los doce caracteres íntegros** (A-08). La fila
`FRASE-DIEGO` **no existe** en el `decisiones.csv` que viaja en el APK (A-04), y la vara documental
(`MATRIZ-SPEC-codigo-V3.6.md`) sigue anclada a la app 3.6.9 (M-04).

---

## 2. Pruebas JVM y APK

| Comprobación | Resultado |
| :--- | :--- |
| Export limpio | `git archive f5145ed` con `-c core.autocrlf=false`. Se añadieron, y sólo en la copia, `local.properties`, los JAR de `libtest/` y `android.overridePathCheck=true`. JDK 11.0.24+8 |
| Compilación | `assembleDebug`: **BUILD SUCCESSFUL**. `compileDebugUnitTestJavaWithJavac`: correcta |
| Pruebas | **267 de 267 en verde**, en 23 clases, `Time: 5,074`. **La afirmación del commit es cierta, verificada ejecutándolas.** Ninguna usa `@Ignore` ni `Assume` |
| Salvedad de método | El ejecutor de Gradle **no arranca en esta máquina**: `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`, por la `ñ` del `GRADLE_USER_HOME` (`C:\Users\Diego.Zuñiga\.gradle`). No es un fallo de la app. Los 267 se ejecutaron con `org.junit.runner.JUnitCore` sobre las clases que compiló Gradle, con el directorio de trabajo en `app/`, que es lo que los tests suponen (`Version3617Test.java:37`, `:43`) |
| El APK sale del commit | El `assembleDebug` del export da md5 `abe8ae9f934fd4c7a88e01768088494a`, **distinto** del entregado. Pero el contenido es **idéntico**: `diff -rq` de los dos APK descomprimidos no da ninguna diferencia, y `classes.dex` coincide (`9a5d877c546a2a96fac35639f0cd84f1`). La diferencia es sólo de metadatos del contenedor ZIP. **El APK entregado se corresponde con `f5145ed`** |
| `aapt dump badging` | `versionCode='3617' versionName='3.6.17'`, `sdkVersion:'24'`, `targetSdkVersion:'30'` |
| Firma (`apksigner`) | `CN=Android Debug`, SHA-256 `c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`: **la misma que la 3.6.15 y la 3.6.16. Se instala encima** |
| Assets | `decisiones.csv` `7b846506…` (cambió: trae `PRECISO-5x9`), cola completa `9ddb7882…`, representativa v2 `70ef3b86…`, anual `d86eddf7…`, grupos `1fa657c9…`. Los cuatro CSV de cola y grupos coinciden con los de `06_Calibracion/` |

---

## 3. Cierre real de lo declarado en P15

Leyenda: **C** cerrado con código y prueba · **Cc** cerrado en código, sin prueba propia · **P** parcial ·
**A** abierto · **Falso** el código no hace lo que se declara.

| ID de P15 | Declarado en la 3.6.17 | Estado | Evidencia |
| :--- | :--- | :---: | :--- |
| **B-01** (Alto) | "Equivalentes sólo de fuera de la cola y nunca de un excluido" | **P — el agujero se traslada** | El filtro existe y funciona: `BancoPrevio.java:131-136` construye `enCola` y `:145` exige `!enCola.contains(e) && !excluidos.contains(e)`; `excluidosDe` (`Decisiones.java:216-228`) devuelve `[P81]` desde `decisiones.csv:11`. El caso de P15 está cerrado y comprobado: `Version3617Test.java:161-178` sale y entra tres veces y los pasos 83 y 85 siguen pendientes, y `RupturaE2ETest` deja el acta con `patrones 5: P18 (n = 20), P67 (n = 20), P112 (n = 20), P127 (n = 20)`, ejecutado aquí. **Pero el filtro sólo tapa a los miembros del grupo que están en la cola**, y en REPRESENTATIVO casi ninguno lo está: ver **A-01** |
| **B-03** (Media) | "Los 7 tipo I ANULADOS con motivo" | **C** | `BancoPrevio.java:116-126` anula la serie fuera de protocolo del patrón que Diego manda repetir aunque su paso no estuviera HECHO, con el motivo `"repetir en preciso 5×4 (TIPO-I-REPETIR de Diego, 6048453)"` (`:97-98`). `Version3617Test.java:182-200` lo comprueba con el ZIP real de las 15:10: 7 anuladas, con motivo, e idempotente (la segunda pasada anula 0) |
| **F-01** (Media) | "Acta vacía se descarta" | **P** | `FlujoCalibracion.java:914-923`: sólo descarta si el acta no tiene códigos, no está escribiendo y no tiene rechazo pendiente (`:915-916`); se aplica al entrar y al salir de `calibrar` (`:926`, `:928`) y al retomar (`:1666`). Las guardas están bien puestas. **Pero sólo en "Calibrar"**: el Banco y "Cambiar serie" siguen bloqueando igual (M-11), y aparece una fuga nueva (M-12) |
| **F-02** (Media) | "Cerrar sin restaurar con el PIN o la frase de Diego" | **Falso en producción** | El código está (`FlujoCalibracion.java:1589-1602`), pero la fila `FRASE-DIEGO` **no viaja en el APK**: ver **A-04** |
| **F-03** (Media) | "Bloqueo hasta liberar" | **Cc** | `bloqueoSinRestaurar()` (`FlujoCalibracion.java:1605-1617`) entra en `previas()` (`:387-391`), y `calibrar` se para en `motivoPrevias()` (`:933`); "Calibrar todo" pasa por `calibrar` (`:1676`, `:1719`), así que también queda bloqueado. `Acta.sinRestaurarPendiente()` (`Acta.java:340-342`) exige `liberada == null`. El botón sólo aparece con el bloqueo puesto (`CalibrarActivity.java`, rama `btnLiberar`). **La liberación depende de la misma firma que F-02**, así que hereda A-04 |
| **F-04** (Baja) | "El `#SC` anula el atajo del T-C41" | **C** | `scEnUltimoAceptar` se pone en `FlujoCalibracion.java:1475` y se consume en `:1754-1755` (`encendidoReciente = sesionTodo && !scEnUltimoAceptar`) |
| **F-05** (Media) | "Se comparten todos los ZIP ligeros de la pulsación" | **Cc** | `ultimoZip` (un solo ZIP) se sustituye por la lista `ligeros`, que acumula uno por acta aceptada y se vuelca entera al compartir (`CalibrarActivity.java`, bloque de `ligeros`). Sin test propio |
| **S-01** (Media) | "`RENOMBRA_REVIERTE` viaja" | **C** | `Version3617Test.java:212-238` |
| **S-02** (Media) | "Un revierte falso se corrige al repetir el cambio" | **C** | `FlujoCalibracion.java:1836-1842`: la condición pasa de `!c.esSerie(n)` a `!n.equalsIgnoreCase(c.serieActual())`, que es lo que hacía falta. `Version3617Test.java:239-252` |
| **S-04** (Media) | "Si `#GN#` no es la serie actual de la campaña, no se calibra" | **C, y abre un Alto** | `FlujoCalibracion.java:380-390`. Comprobado en la salida de la suite: *"El equipo dice SLV-002-2026 (#GN#) y la campaña SLV-002: repita «Cambiar serie» en Avanzado hasta que coincidan"*. El arreglo es correcto, pero convierte en terminal el fallo de **A-08** |
| **S-03** (Media) | No se reclama en el commit | **A** | Estaba en el plan "Para la 3.6.17" de P15 §7. Sigue abierto y ahora hace más daño: **M-13** |
| **P14-B02** | "SHA-256 del soporte en el acta" | **P** | El ZIP de soporte se exporta antes de cerrar y su SHA-256 entra como dato del acta (`FlujoCalibracion.java:1484-1488`, `CalibrarActivity.java:176-188`). Si la exportación falla, devuelve `null`, el acta se cierra **sin la cita y sin decirlo** (sólo queda en `Registro.nota`, `CalibrarActivity.java:184`): **M-05** |
| **§5 de P15**, regla K ≥ 5 y M ≥ 4 | "Fila PRECISO-5x9 firmada" | **C** | `decisiones.csv:15` y `DECISIONES-Diego-2026-09-19.md:27`. La condición que P15 dejó abierta queda cerrada. Aparece un matiz nuevo en cómo se mide la M: **M-01** |
| P15 §7, resto | No se tocan | **A** | B-02, B-04 (`Anclas.java:122-126`, sigue cayendo a la A5 de toda la campaña sin avisar), B-05 (`BancoPrevio.java:151` mira `aceptada` y `anulada`, no el veredicto), S-03, S-05 a S-08, F-06 (no hay quién acepta), F-07, P14-B03, C-P14-2. El README de la app lo declara ("Sin hacer en esta: … S-03 y S-08"), salvo los demás, que calla |

### 3.1 Lo que declara el desarrollador

| Afirmación (`RetroV36/README.md`) | Veredicto |
| :--- | :--- |
| "267 tests, todas en verde" | **Cierto**, ejecutadas aquí |
| "B-01. … P127 ya no se da por medido con P81, ni P19 con P18" | **Cierto tal como está escrito, y engañoso como cierre del defecto**: P127 sí puede darse por medido con **P126**, y P19 con **P17** (A-01) |
| "B-03. Las series de los 7 tipo I … quedan ANULADAS" | **Cierto** |
| "F-02. … o la frase de Diego (fila FRASE-DIEGO de `decisiones.csv`)" | **Falso para el APK entregado**: esa fila no existe en el asset (A-04) |
| "La cola se cambia en cualquier momento. Lo ya medido se conserva" | **Cierto para las series, falso para los controles y para un Rehacer**: M-03 y A-05 |
| "quedan 51 patrones, unos 82 min" (representativo) | **Cierto**, sale en la suite: `ZIP 15:10, banco REPRESENTATIVO: 58 patrones en la cola, 7 ya medidos cuentan, 0 a repetir; quedan 51 patrones, 82 min`. Coincide con el recuento a mano de P15 §6 |
| Banco completo | **Cambia respecto de P15 y el README no lo dice**: ahora `quedan 114 patrones, 119 min` (antes 106 y 116), porque el arreglo de B-01 quita 8 equivalentes que antes saltaban. Es el efecto correcto del arreglo, pero deja obsoleta la cifra de P15 §6 |

---

## 4. Decisiones de Diego, una por una

| Fila de `DECISIONES-Diego-2026-09-19.md` | En la app | Evidencia |
| :--- | :---: | :--- |
| D-C, D-B1, D-B2, D-A5 (códigos 1 y 2) | **Respetada** | Códigos ya escritos; `TablaCalibracion` no los reescribe |
| **PA-24** y **PA-24 ampliada / margen** | **Respetada** | `decisiones.csv:10`, alcance `RF-CAL-14 P39 +15 3; RF-CAL-14 P49 -10 3; RF-CAL-15 I 11.5 0` |
| **RF-CAL-15-b** (11,5 % estricto) | **Respetada** | Margen `0` en la misma línea |
| **PA-14** y **P81** (fuera del ajuste) | **Respetada al donar, NO al recibir** | `decisiones.csv:11` `EXCLUYE P81`; `patronesAjuste` lo quita del ajuste (`FlujoCalibracion.java:449`). Pero el paso de P81 en la cola **sí recibe** la serie de P126 y queda HECHO (A-01, variante) |
| **REMEDIDA-b** | **Respetada** | `decisiones.csv:12` |
| **D-FW**, **D-SFT** | Fuera del alcance de la app | — |
| **D-VENC** (avisa, no bloquea) | **Respetada** | `Calibracion.java:130,144-146`; `Sesion.java:195-198` |
| **SERIE / SERIE-2** (SLV-002-2026) | **Respetada, con riesgo** | `TablaCalibracion.java:35-66`: `canonico()` devuelve `SLV-002` para la MAC `00:21:13:05:19:3B` con cualquiera de las dos series en el historial, así que **las decisiones y la tabla no se pierden al renombrar**. Comprobado que `Base.java:200` usa `canonico` antes de `BancoPrevio.aplicar`. El riesgo es **A-08**: si el `#GN#` devuelve la serie truncada, `canonico` deja de reconocer el equipo y **estas mismas decisiones se pierden** |
| **PROTOCOLO-MIN** (1×4 por defecto) | **Respetada** | `Protocolo.java:7-13`, `:63` |
| **PROTOCOLO-AJUSTE** (5×4 en lo que se escribe) | **Respetada** | `decisiones.csv:13`; `Protocolo.java:85` |
| **TIPO-I-REPETIR** (los 7) | **Respetada** | `decisiones.csv:14`; `BancoPrevio.java:94-126` |
| **PRECISO-5x9** | **Respetada, con un matiz** | `decisiones.csv:15`; `Protocolo.java:97-103`. El matiz es M-01 |

---

## 5. Lo nuevo de la 3.6.17: el cambio de cola a mitad

El diseño es correcto en su núcleo y conviene decirlo antes de los defectos. **Los números de orden de las
dos colas no son intercambiables**: comprobado cruzando los CSV, el orden 12 es `P51` en
`cola_banco_P1-P132.csv` y `P7` en `cola_banco_representativo_v2.csv`, y **73 de los 80 órdenes de la
representativa designan otro patrón en la completa**. La app lo sabe y lo resuelve bien: `elegirCola`
(`Campana.java:534-546`) limpia `pasos`, `seriePaso` e `historialPasos` cuando el tipo cambia, la
reproducción del diario hace lo mismo (`Campana.java:1264-1274`), y `BancoActivity.cargar()` vuelve a contar
desde las **series**, que sí están atadas al patrón, llamando a `aplicarBancoPrevio` en
`BancoActivity.java:425`. Es la decisión de diseño acertada.

Lo que falla es lo que queda fuera de esa limpieza: una medida en vuelo (**A-02**), un Rehacer pendiente
(**A-05**) y los pasos de control (**M-03**).

---

## 6. Defectos Altos

### A-01 — B-01 sigue abierto por un donante de fuera de la cola (Alta)

`BancoPrevio.java:145` descarta como donante al equivalente que **está en la cola** o que **una decisión
excluye**. En la cola REPRESENTATIVA sólo está el representante de cada grupo, así que **los demás miembros
del grupo pasan el filtro y pueden donar su serie**.

Reproducido ejecutando contra las clases del commit, con `decisiones.csv` y los CSV reales:

```
G57 = [P126, P81, P127, P82]
Medido P126 -> S001. P127 y P81 no se han medido.
paso 83 (P127, AJUSTE del 5) = HECHO, serie S001
paso 82 (P81, EXCLUIDO del ajuste) = HECHO, serie S001
excluidos de SLV-002 = [P81]
pasos de la REPRESENTATIVA sustituibles por un donante de fuera de la cola: 24
```

Entre esos 24 están **los cuatro patrones del ajuste del código 5** (`P67←P66`, `P127←P126`, `P18←P17`,
`P112←P111`) y sus dos verificaciones (`P81←P126`, `P19←P17`). En la cola COMPLETA no ocurre: allí todos los
miembros del grupo son pasos.

**Por qué acaba en un ajuste con 3 patrones de 4.** El ajuste no usa el paso: usa las series cuyo patrón
está en `patronesAjuste`, que sale de la cola (`FlujoCalibracion.java:433-451`, `medidasDe` en `:453-462`,
filtro en `:457`). La serie de P126 **no entra** en el ajuste porque P126 no es patrón de la cola. Y nada
aguas abajo lo detiene: `BancoCola.calibrable` (`BancoCola.java:298-310`) mira **sólo los estados de los
pasos**, así que el 5 sale "calibrable"; y `Protocolo.incumple` (`Protocolo.java:98-101`) **devuelve `null`
cuando la serie es `null`**, así que un patrón sin serie no incumple ningún protocolo. El resultado es el
mismo que P15 marcó como Alto: el 5 se ajusta con 3 patrones y **el acta sale ACEPTADA**.

**Agravante de la variante P81.** La exclusión de `decisiones.csv:11` impide que P81 **done**, no que
**reciba**: en la traza, la misma serie `S001` satisface **dos pasos de la cola**, el de P127 y el de P81.
Diego sacó P81 del ajuste (fila `P81`, ~15:40); esto lo devuelve como paso satisfecho sin haberlo medido.

**Contexto que agrava el juicio:** dentro de un mismo grupo los valores certificados **no son iguales** —
G57: P126 = 91, P81 = 92, P127 = 97, P82 = 98 (`grupos_patrones_equivalentes.csv:89-92`), un 6,6 % entre
P126 y P127 — y su `origen_x` es `"estimada: recta por el oscuro"`, no medida. Que un miembro del grupo
sustituya a otro **no es una equivalencia comprobada**, y menos para elegir el "extremo bajo del tipo IX",
que es el motivo por el que P127 está en la cola.

**Cómo se reproduce:** banco en REPRESENTATIVO; medir P126 (o P66, P17, P111) en cualquier momento —por
ejemplo con la cola COMPLETA y cambiando luego, o en otra campaña de la misma MAC—; salir y volver a entrar
al Banco; el paso de P127 (P67, P18, P112) queda HECHO.

**El camino más probable, y el peor, es el que la propia 3.6.17 propone.** En la cola COMPLETA el bloque
azul del código 5 es, por orden: 155 P66, 156 P67, 157 P126, 158 P81, 159 P127, 160 P82, 161 P17, 162 P18,
163 P19, 164 P111, 165 P96, 166 P112, 167 P97. Si el operador mide P66, P126, P17 y P111 —que es lo que esa
cola le pide— y luego acepta el paso a REPRESENTATIVO que la app le ofrece al entrar al Banco, **los seis
pasos del código 5 quedan HECHO, los seis con la serie de otro patrón**, `calibrable("5")` da `true` y
**ninguno de los cuatro patrones de AJUSTE tiene serie propia**. El ajuste del 5 se quedaría sin un solo
punto suyo. La lista "patrones 5" del acta sería la única señal, y es la que la condición 4 de §1 manda
mirar.

**Condición si se entrega igual:** condición 4 de §1, leer la lista "patrones 5" del acta y rechazar si no
son los cuatro. **Arreglo:** el filtro debe ser por **grupo**, no por pertenencia a la cola: un paso cuyo
patrón es representante de un grupo no admite donante de ese grupo.

### A-02 — Cambiar la cola con una medida en curso marca HECHO un paso de otro patrón (Alta)

El botón del selector de cola **nunca se deshabilita**: en `pintar()` se ajustan `btnOk`, `btnSaltar`,
`btnRehacerAnterior` y `btnRehacerPatron` con `libre = !ocupado && enCurso == null`
(`BancoActivity.java:495-503`), pero a `btnBanco` sólo se le cambia el texto (`:420`). `elegirBanco()`
(`:284-310`) no mira `ocupado` ni `enCurso`, y `cambiarCola` (`:337-350`) tampoco.

La medida en vuelo guarda su propio `Paso` (`Medicion.p`, `BancoActivity.java:618-619`) y al terminar anota
con **el número de orden de la cola vieja**: `anotar(p, "HECHO", s.id, "OK")` en `:1013`, y `anotar` hace
`campana.anotarPaso(p.orden, …)` en `:531`. No hay ninguna comprobación de que ese paso pertenezca a la cola
actual. Lo mismo ocurre con la batería (`:601`) y el calentamiento (`:554`).

Reproducido:

```
COMPLETO paso 82 = P53 (PATRON, codigo 4, uso AJUSTE)
REPRESENTATIVO paso 82 = P81 (PATRON, codigo 5, uso VERIFICACION)
>>> Tras el cambio, el paso 82 de la cola NUEVA queda HECHO y es P81, pero lo medido fue P53
```

**Alcance:** **13 pasos de los códigos 8, b y 5** —los tres que se van a escribir— ocupan en la cola completa
un orden con otro patrón. Entre ellos el **orden 83 = P127 (5/AJUSTE)**, el **84 = P18 (5/AJUSTE)**, el
**81 = P67 (5/AJUSTE)**, el **86 = P112 (5/AJUSTE)** y los cuatro del 8 (órdenes 25 a 28). Un paso de ajuste
marcado HECHO sin medir es, otra vez, un ajuste con un patrón de menos.

**Cómo se reproduce:** empezar un paso (pulsar OK), y mientras la app mide, pulsar "Banco: … (cambiar)" y
elegir la otra cola.

**Condición si se entrega igual:** condiciones 1 y 2 de §1. **Arreglo:** `btnBanco.setEnabled(libre)`, y que
`anotarPaso` rechace un orden que no venga de la cola en curso.

### A-03 — El B-01 ya escrito por la 3.6.16 no se sanea si la cola no cambia (Alta)

`BancoPrevio.aplicar` sólo toca los pasos **sin estado**: `BancoPrevio.java:139` salta con
`est.containsKey(p.orden)`. Un `PASO … HECHO` que la 3.6.16 escribió con el defecto B-01 se hereda tal cual.

Reproducido:

```
Diario heredado de la 3.6.16: paso 83 (P127) = HECHO con la serie de P81
Tras abrir el Banco con la 3.6.17 (misma cola): paso 83 = HECHO
   serie del paso 83 = S001 (es la de P81)
```

**Mitigación comprobada:** cambiar de cola y volver limpia los estados y recuenta desde las series, y
entonces sí se sanea:

```
Tras cambiar de cola y volver: paso 83 (P127) = null  (vuelve a la cola, saneado)
```

**Riesgo real en SLV-002:** bajo. El banco parcial es de las 15:10 y su diario viene de la 3.6.11, que no
escribe evento `COLA`; con `colaElegida()` falso la 3.6.17 cambia sola a REPRESENTATIVO
(`BancoActivity.java:402-409`), lo que limpia los pasos y sanea de paso. El riesgo aparece si el teléfono
llegó a abrir el Banco con la 3.6.16.

**Condición:** antes de calibrar, con la 3.6.17 ya instalada, cambiar el banco a la otra cola y volver una
vez, y comprobar que "quedan 51 patrones".

### A-04 — "Cerrar sin restaurar con el PIN o la frase" es, en el APK entregado, sólo el PIN (Alta)

`firmaDeDiego` (`FlujoCalibracion.java:1589-1602`) admite dos firmas: la frase cuyo SHA-256 esté en la fila
`FRASE-DIEGO` de `decisiones.csv`, o el PIN del equipo comprobado con `#L`. **La fila `FRASE-DIEGO` no
existe en el asset que viaja en el APK.** Verificado dos veces: el `decisiones.csv` de
`app/src/main/assets/` tiene 6 filas de datos (`PA-24`, `PA-14`, `REMEDIDA-b`, `PROTOCOLO-AJUSTE`,
`TIPO-I-REPETIR`, `PRECISO-5x9`), y `FRASE-DIEGO` aparece **sólo como comentario** en su línea 7; una
búsqueda en todo el repositorio la encuentra además en `FlujoCalibracionTest.java:1405`, que **la inyecta a
mano** para su prueba, y en el README. La prueba que da F-02 por cerrado valida una fila que no se entrega.

Consecuencias:

- `decisiones.valor("FRASE-DIEGO", equipo())` devuelve `null` siempre, así que la única firma es el PIN, que
  se comprueba con `Ops.entrar` (`Ops.java:49-65`), **y exige que el equipo responda**: sin enlace devuelve
  *"El equipo no respondió a #L"* (`Ops.java:55`) y no hay firma posible.
- "Cerrar sin restaurar" se usa justo cuando la restauración no entró, que es el escenario en que el enlace
  suele estar caído. Y **F-03 bloquea toda calibración hasta liberar**, con la misma firma. Un fallo de
  restauración con el enlace perdido deja el equipo sin poder calibrarse y **sin salida en campo**: el
  bloqueo se lee de la última acta archivada (`Campanas.java:410-418`), así que sólo se levanta con el PIN o
  borrando los datos de la app, que se lleva por delante la campaña.
- El bloqueo es del lado seguro (no escribe nada malo), pero es un callejón sin salida operativo.

**Condición si se entrega igual:** condición 5 de §1, llevar el PIN del equipo a campo. **Arreglo:** añadir
la fila `FRASE-DIEGO` firmada al `decisiones.csv` del APK, o quitar de la interfaz y del README la mención a
la frase.

### A-05 — Cambiar de cola resucita una serie que el operador mandó Rehacer (Alta)

Corrige el hallazgo tal como llegó: **no se reproduce siempre**, sólo cuando existe una serie anterior
aceptada del mismo patrón — que es exactamente el caso de SLV-002, con su banco parcial de las 15:10.

Con una sola serie, `RehacerBanco.rehacer` la deja sin elegir y el cambio de cola deja el paso pendiente,
que es lo correcto:

```
Tras pedir Rehacer: paso 86 (P112) = REHACER, serie elegida de P112 = null
Tras cambiar de cola y volver: paso 86 (P112) = null, serie del paso = null
```

Con una serie anterior aceptada del mismo patrón, `c.elegida(pat)` vuelve a devolverla y `BancoPrevio` la da
por buena:

```
Tras pedir Rehacer: paso 86 (P112) = REHACER, serie elegida de P112 = S001 (NO anulada)
Tras cambiar de cola y volver: paso 86 (P112) = HECHO, serie del paso = S001
```

El mecanismo: `Campana.elegirCola` borra los estados (`Campana.java:536-540`) y `BancoPrevio.java:149-162`
reconstruye desde `c.elegida(pat)`, que devuelve la última serie aceptada y no anulada. El `REHACER` vive en
el estado del paso, que se acaba de borrar; la serie vieja no está anulada, así que vuelve. Y persiste al
releer el diario, porque lo que se reproduce es el `PASO … HECHO` nuevo.

**Por qué importa:** el operador pulsa Rehacer porque la medida estaba mal (lámina mal colocada). El cambio
de cola —que la 3.6.17 **ofrece por iniciativa propia** nada más entrar al Banco, `BancoActivity.java:403-406`—
da por buena la medida que quería descartar, en silencio.

**Condición:** condición 3 de §1. **Arreglo:** `RehacerBanco.rehacer` debe dejar la serie fuera del recuento
(anularla, o anotar el rehacer por patrón y no por orden).

### A-06 — Fecha de calibración huérfana si el enlace cae entre el `#SC` y el `#GC#` (Alta)

En `aceptar()`, `FlujoCalibracion.java:1474-1483`: se escribe `#SC,<hoy>#` y **después** se pide `#GC#` para
releerla. Si el enlace cae en medio, `ops.pedir("#GC#")` lanza `IOException`, que sale del método; la fecha
**ya está grabada en el equipo** y el acta no se cierra. Si después se rechaza, `rechazar()` no toca la
fecha. Queda un equipo que responde `#GC#` con fecha de calibración y vencimiento **sin ningún acta aceptada
detrás**, y esa fecha se propaga a lo que la app enseña y escribe en la cabecera de los ZIP
(`Sesion.java:186-198`). P15 dio PASA a los cortes de `#SC` (§4, fila de negativos) porque sólo comprobó que
nada se aceptase dos veces.

**Condición:** tras cualquier corte durante "Calibrar", leer `#GC#` en Pruebas y contrastar la fecha con las
actas aceptadas antes de dar por buena la calibración. **Arreglo:** dejar constancia del `#SC` emitido en un
registro que sobreviva al rechazo, y avisar al arrancar si hay fecha en el equipo sin acta que la sostenga.

### A-07 — Un segundo APK con el mismo paquete y `versionCode` mayor, en la misma carpeta de entrega (Alta)

`03_App_Movil/` contiene, además del entregable, `RTV-V1.0.0-rc1.apk` (16:16, veinte minutos antes que el
3.6.17). Comprobado: mismo paquete `com.dpi.retrov36`, `versionCode='10000' versionName='1.0.0'`, **misma
firma** `c990adf6…075f`, y contenido distinto (`classes2.dex` y `decisiones.csv` difieren, y trae dos assets
que la 3.6.17 no tiene, `equipos.csv` y `firmwares.csv`). Sale de la rama `rtv-1.0`, cuyo
`app/build.gradle` declara el mismo `applicationId` y `versionCode 10000`.

La `SPEC-App-Unica-V36-V46.md:295-298` fija esa decisión a propósito: *"`applicationId` no cambia … porque
cambiarlo instala otra app al lado y obliga a desinstalar, contra RF-APP-40; `versionCode` siempre mayor que
el último publicado, porque Android no instala encima una versión con código menor"*. La consecuencia es que
**si el rc1 entra en el teléfono de campo, la 3.6.17 (3617) ya no se puede instalar encima**, y volver a ella
exige desinstalar, lo que borra campaña, actas y diario — justo lo que la app avisa con RF-APP-40.

**Condición:** condición 6 de §1; sacar el rc1 de la carpeta de entrega antes de que el teléfono lo vea.

### A-08 — El cambio de serie puede dejar el equipo sin poder calibrarse y sin salida en la app (Alta)

Es el paso que hay que dar en campo (fila **SERIE-2** de Diego), y el arreglo de S-04 lo ha vuelto terminal.

Secuencia, toda en `FlujoCalibracion.java:1845-1866`:

1. `:1852` escribe el `RENOMBRA` **antes** del `#SN` (es el arreglo de QA-3615-01): la campaña ya tiene
   `SLV-002-2026` como serie actual.
2. `:1853-1855` escribe `#SN,SLV-002-2026#` y relee `#GN#`. Si el equipo devuelve **un tercer valor** —ni la
   serie nueva ni la anterior— no entra en `:1856` ni en `:1859`, y cae en `:1864-1865`: *"no se pudo
   verificar. La campaña reconoce las dos series"*, que además es un mensaje engañoso, porque las dos que
   reconoce son la vieja y la nueva, no la que acaba de leer.
3. Al repetir "Cambiar serie", `anterior` es ya ese tercer valor, que no está en el historial de la campaña,
   y la guarda de `:1845-1846` corta: *"La serie actual del equipo (X) no es de la campaña abierta: no se
   cambia."* **La única vía de la app que añade una serie al historial es `renombrar`, y está detrás de esa
   guarda.**
4. Y `previas()` (`:381-390`) bloquea la calibración, porque `campana.esSerie(gn)` es falso.

**Por qué no es hipotético aquí.** `Calibracion.motivoSerieInvalida` admite hasta **12 caracteres**
(`Calibracion.java:69-70`), y la serie que Diego decidió, **`SLV-002-2026`, mide exactamente 12**: está en
el borde del límite. Un buffer de 12 con terminador en el firmware devolvería `SLV-002-202`, que es
precisamente el "tercer valor". **Esto no está medido contra el equipo**, y la fila SERIE-2 dice que se
graba con esta app.

**El escape por "Nueva campaña" degrada el equipo.** Con historial `[SLV-002-202]`,
`TablaCalibracion.canonico` (`TablaCalibracion.java:46-66`) no encuentra ninguna de `SERIES_SLV002` y
devuelve `SLV-002-202`; las decisiones están indexadas por `id|equipo` (`Decisiones.java:216-228`), así que
**se pierden PA-24, PA-14, REMEDIDA-b, PROTOCOLO-AJUSTE y TIPO-I-REPETIR**, y los códigos b y 5 dejan de ser
escribibles.

**Condición:** condición 7 de §1, y la comprobación previa con el equipo delante. **Arreglo:** ante un
`#GN#` con un valor desconocido, meterlo en el historial (`anadirSerie`) para no cerrar la puerta; y bajar
el límite de `motivoSerieInvalida` a lo que el firmware garantice.

---

## 7. Defectos Medios

| ID | Defecto | Evidencia | Severidad |
| :--- | :--- | :--- | :---: |
| **M-01** | `Protocolo.deSerie` calcula la M como **máximo** de disparos sobre las colocaciones, no como mínimo: una serie con una colocación larga y el resto cortas pasa por "preciso 5×4". La fila `PRECISO-5x9` que Diego firmó habla de series 5×9 homogéneas | `Protocolo.java:89-95`, usado por `incumple` en `:102` | Media |
| **M-02** | El paso a REPRESENTATIVO es **mudo** cuando el diario no trae evento `COLA` (los de la 3.6.11): `BancoActivity.java:402-409` sólo abre el diálogo si `colaElegida()`; si no, `cambiarCola(def, false)` cambia y borra los estados de paso sin preguntar. Es el caso del ZIP de las 15:10 | `BancoActivity.java:402-409` | Media |
| **M-03** | Al cambiar de cola se pierden los pasos de **control**: `BancoPrevio.java:139` sólo reconstruye los de tipo `PATRON`, así que calentamiento, batería, OSCURO y A5 vuelven a estar pendientes. Lo confirma el propio test del desarrollador, cuya salida es `3.6.16 COMPLETO paso 12 -> REPRESENTATIVO: … siguiente paso 1 CALENTAMIENTO`. No es sólo tiempo (21 pasos de control en la representativa): **el OSCURO que se vuelve a medir pasa a ser el elegido y cambia el ancla a mitad de banco**, que es justo lo que QA-3614-05 prohíbe al rehacer (`RehacerBanco.java:56-59`). Y el diálogo promete lo contrario: "Lo ya medido se conserva y cuenta" (`BancoActivity.java:371-372`) | `BancoPrevio.java:139`; `BancoCola.java:268-273`; salida de `Version3617Test` | Media |
| **M-04** | La vara documental no cubre lo que se entrega: `MATRIZ-SPEC-codigo-V3.6.md` está anclada a la **app 3.6.9** (commit `f52eeb1`, 89 tests) y **ninguna** de `SPEC-V3.6.md`, `SPEC-Calibracion-V3.6.md`, `PROTOCOLO-V3.6.md` ni la propia matriz menciona la 3.6.17 (`grep -c "3\.6\.17"` = 0 en las cuatro). No hay matriz SPEC↔código vigente para el APK que se entrega | `MATRIZ-SPEC-codigo-V3.6.md:1-20` | Media |
| **M-05** | Si la exportación del ZIP de soporte falla, `soporteSha256` devuelve `null` y el acta se cierra **sin la cita del SHA-256 y sin decirlo** en el acta; sólo queda en el registro | `CalibrarActivity.java:178-188`; `FlujoCalibracion.java:1484-1488` | Media |
| **M-06** | Quedan abiertos, sin mención en el commit, los defectos de P15 §7: B-02, B-04, B-05, S-03, S-05 a S-08, F-06, F-07, P14-B03 y C-P14-2. Comprobados tres al azar y siguen tal cual: `Anclas.java:122-126` (B-04), `BancoPrevio.java:151` (B-05), ningún registro de quién acepta (F-06) | — | Media |
| **M-07** | ~~El `#SN` que no entra deja un desacuerdo permanente de serie.~~ **Corregido tras reabrirlo: no es así.** Si `#GN#` devuelve la serie anterior, `FlujoCalibracion.java:1859-1862` hace `revertirRenombrado` y la campaña vuelve a `SLV-002`; se puede reintentar. El caso terminal es sólo el del **tercer valor**, y ése es A-08. Se deja escrito para que la afirmación errónea no vuelva a circular | `FlujoCalibracion.java:1859-1862` | — |
| **M-08** | El ZIP automático del final del banco y el de cada paso `EXPORTAR` se disparan desde `pintar()` con banderas de instancia (`autoExportado`, `exportadoAlFinal`), que se reinician al recrear la pantalla: salir y volver a entrar al Banco terminado vuelve a exportar. Sin daño, pero ensucia `Download/RTV/` y el diario con `EXPORTA` repetidos | `BancoActivity.java:460-474` | Baja-Media |
| **M-09** | **Las siete anulaciones de B-03 son mudas.** `BancoPrevio.Resultado.anuladas` se incrementa en `BancoPrevio.java:123` y **ningún código de producción lo lee**: `Base.java:203-205` compone el rótulo sólo con `r.hechos` y `r.repetir`, y `r.texto` no se usa en ninguna parte. Al abrir el Banco con el ZIP real se **anulan siete series aceptadas** y el operador sólo ve "7 pasos ya medidos cuentan como hechos; 0 a repetir". Además `anular` incrementa `anuladasDesdeExportar` (`Campana.java:657`), así que el ZIP ya entregado queda marcado como viejo sin explicación visible | `BancoPrevio.java:123`; `Base.java:203-205` | Media |
| **M-10** | **El ZIP publica un md5 de cola que no es el que se usó.** `cargar()` sólo reescribe el evento `COLA` si el tipo por defecto difiere (`BancoActivity.java:402-410`); con un diario que trae la v1 de la cola representativa y el APK cargando la v2, el tipo coincide y `colaMd5` se queda en el viejo. La discrepancia va sólo a `Registro.nota` (`:421-423`), invisible, mientras `Campana.java:976` y `:1014` imprimen ese md5 en el resumen del ZIP entregado | `BancoActivity.java:402-410`; `Campana.java:976`, `:1014` | Media |
| **M-11** | **F-01 sólo está cerrado en "Calibrar".** `descartarActaVacia` no se llama desde `bloqueoRehacer` (`FlujoCalibracion.java:1766-1770`, el "Rehacer" del Banco) ni desde `renombrarSerie` (`:1823-1825`), que siguen devolviendo "hay un acta en curso: acéptela o recházela" con sólo mirar si existe. Es el defecto F-01 original, mudado del Calibrar al Banco y a "Cambiar serie" | `FlujoCalibracion.java:1766-1770`, `:1823-1825` | Media |
| **M-12** | **"Calibrar todo" puede comerse un código sin decirlo.** Si al entrar hay un acta con `escribiendo() != 0` y el corte se resuelve como "el `#S` no entró" contestando **No**, el acta queda vacía, el `descartarActaVacia` de `:928` la cierra y `acta` pasa a `null`; de vuelta en `calibrarTodoInterno` las dos ramas exigen `acta != null` (`:1677`, `:1683`), **el resultado `r` se descarta sin mostrarse** y `:1688` quita el código de la lista. Con `{8}` el operador ve sólo "Nada que calibrar." | `FlujoCalibracion.java:1670-1698` | Media |
| **M-13** | **S-03 no se ha cerrado** y ahora hace más daño. `ImportadorCampana.java:92-97` comprueba la MAC recorriendo las series, así que un diario **sin series** (sólo `RENOMBRA`) no entra en el bucle y llega a `copiarRenombrados`, que no comprueba MAC (`Campana.java:327-345`). Con el arreglo de S-04, un `RENOMBRA` ajeno ya no sólo ensucia el acta: **bloquea la calibración** | `ImportadorCampana.java:92-97`, `:137`; `Campana.java:327-345` | Media |
| **M-14** | Dos cifras de minutos en la misma pantalla con "rápido" desmarcado: `txtModo` y `txtAvance` usan `prefRapido()` (`BancoActivity.java:449-450`, `:458`), pero `resumenPrevio` sale de `Base.java:204`, que pasa **`true` cableado**. El número de patrones sí coincide siempre. Es la Baja que P15 §7 ya listaba, sin arreglar | `Base.java:204` vs `BancoActivity.java:449-450`, `:458` | Baja |
| **M-15** | El informe de calibración se nombra con la serie del **nombre Bluetooth** (`Sesion.get().serie()`), mientras el acta lleva `campana.serieConHistoria()` y el ZIP de soporte `c.serieActual()`: tras grabar `SLV-002-2026` el entregable sale como `informe_calibracion_SLV-002_<fecha>.txt` junto a `soporte_SLV-002-2026_….zip` | `CalibrarActivity.java:518`, `:529`; `ExportadorFinal.java:46-49` | Baja |
| **M-16** | El bloqueo de F-03 vive **sólo** en los ficheros del teléfono; el equipo no lleva ninguna marca. Borrar los datos de la app, reinstalar o usar otro teléfono lo levanta por completo. Es inherente al diseño, pero conviene que el RUNBOOK lo diga: "cerrar sin restaurar" no inmoviliza el equipo, inmoviliza esta instalación | `Campanas.java:393-418` | Baja |

---

## 8. Qué no se ha hecho

- **No se ha medido nada.** No se ha ejecutado la app en un teléfono ni contra SLV-002. Las cuatro pantallas
  (`CalibrarActivity`, `BancoActivity`, `AdminActivity`, `ConexionActivity`) se han juzgado leyendo el
  código.
- El ejecutor de Gradle no arranca en esta máquina (§2): los 267 tests se corrieron con JUnitCore. La
  reproducción byte a byte del APK no se ha conseguido (sí la del contenido).
- Las cuatro sondas de esta revisión (cambio de cola con medida en vuelo, B-01 heredado, donante de fuera de
  la cola, Rehacer resucitado) están en el scratchpad, **no en el árbol**. Se ejecutaron contra las clases
  que compiló Gradle desde el export de `f5145ed`, con `decisiones.csv` y los CSV reales.
- **A-06 y A-08 se han verificado leyendo el código, no provocando el corte ni el truncamiento.** A-08 en
  particular depende de cómo responda el firmware 3.6.2 a una serie de 12 caracteres, y **eso sólo se cierra
  midiendo**.
- Uno de los dos subagentes **no tenía JDK** y trabajó cruzando los CSV y el ZIP real con Python sobre una
  réplica de `BancoPrevio`, no ejecutando la app. Sus hallazgos Altos (A-01, A-02, A-03) se han vuelto a
  abrir aquí y se han reproducido con las clases compiladas; el resto de los suyos que entran en §7 se han
  comprobado leyendo el fichero. Su reproducción del "Quedan 51" coincide con la de la app y con el
  recuento a mano de P15 §6, lo que da crédito al resto de su simulación pero no la convierte en medida.
- **A-05 llegó descrito como un fallo general y no lo es**: sólo se da si hay una serie anterior aceptada del
  mismo patrón. Se corrige en §6 con las dos trazas.
- No se han revisado el firmware ni el hardware.
- Siguen pendientes T-C41 y T-C43 en SLV-002 con registro archivado, y la A5 del banco que cierra C-P14-2.

---

## 9. Nota sobre el encargo

Dos cosas del encargo de esta puerta que no eran ciertas y conviene dejar escritas, por la regla de §2 y §8
del método:

- **El repositorio V3.6 no tiene `CLAUDE.md`.** El encargo pedía leerlo y cumplirlo. Verificado con dos
  herramientas distintas: no existe en `D:\IT\P_RetroVertical_V3.6`, ni en la raíz ni en ningún
  subdirectorio. Rigen las reglas globales heredadas del V5.
- **La 3.6.17 no se entrega sola:** la sustituye la rama `rtv-1.0`, que la lleva dentro. Los siete defectos
  Altos de §6 **viajan con ella**, y A-07 deja de ser un riesgo de confusión para pasar a ser el camino
  previsto. Esta revisión es la que deja constancia de ellos.
