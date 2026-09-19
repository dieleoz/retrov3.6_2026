# Revisión P15 y QA de la app 3.6.16 (arquitecto adversario + ISTQB)

**Veredicto: APTO CON CONDICIONES para entregar a Diego. No hay bloqueante de seguridad**: ningún corte, ninguna
muerte de la app y ningún equipo con otra MAC consiguió escribir una curva sin acta, duplicar un `#S` indebido
ni aceptar un acta dos veces. **Hay un defecto Alto nuevo (B-01)**: si el operador sale del Banco y vuelve a entrar
entre P81 y P127, la app da P127 (AJUSTE del 5) por medido con la serie de P81, que Diego excluyó. El 5 se calibra
entonces con 3 patrones en lugar de 4, **y el acta sale ACEPTADA**. Se rodea con la condición 1 de §1.

**Nada de esto se ha probado en un teléfono ni contra SLV-002.** Todo sale de leer el fuente, ejecutar la JVM
contra `EquipoSimulado` y usar el ZIP real de las 15:10. Un documento que cuadra en papel no es un documento medido.

- **Fecha:** 19-sep-2026, noche. Puerta **P15**.
- **Qué se revisa:** `5be257a`. HEAD está en `4c4c475`, que sólo cambia `.gitattributes` (`dde21fd`) y un documento
  del firmware.
- **APK:** `03_App_Movil/RTV-V3.6.16.apk`, md5 `4e342be735d5c62cc392d45a3e474e4e`. `RTV-V3.6.16-3616.apk` tiene el
  mismo md5.
- **Base:** `REVISION-Arquitectura-P14-V3.6.md`, `QA-App-3.6.15.md`, `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`
  (todas las filas) y `06_Calibracion/PLAN-Banco-Representativo.md`.
- **Rutas:** las de la app son relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`. Los tests
  son de `app/src/test/java/com/dpi/retrov36/`.
- **Cómo se ha hecho:**
  - Tres subagentes adversarios trabajaron sobre el mismo export: el flujo "Calibrar todo", la serie con la
    importación, y el banco con "Quedan N".
  - Sus hallazgos de gravedad media o alta los he vuelto a abrir en el código: F-01, F-02, F-04, S-01, S-02, S-04,
    B-01 y B-03.
  - El recuento de "Quedan 51" y el caso de principio a fin (§4) los he hecho yo aparte.
  - Las pruebas de ruptura (unos 35 casos en total) están en el scratchpad y **no se han añadido al árbol**.

---

## 1. Condiciones de campo (operador, 5 líneas)

1. Desde que mida P81 en el Banco, no salga de la pantalla hasta medir P19. En el acta del 5 tiene que poner
   "patrones 5: P18, P67, P112, P127"; si falta alguno, pulse Rechazar y avise a Diego.
2. Si sale "RESTAURACIÓN NO VERIFICADA" o "rechazo pendiente", no toque nada y llame a Diego: "Cerrar sin restaurar"
   solo lo escribe él.
3. Si "Calibrar todo" dice "El acta en curso no está lista" sin haber escrito nada (batería baja, apagado cancelado),
   pulse Rechazar, arregle la causa y vuelva a empezar.
4. Cambie la serie una sola vez, con el equipo cerca. Si dice "NO quedó cambiada", no calibre y avise a Diego. Antes
   de calibrar, Pruebas debe mostrar SLV-002-2026 como serie del equipo.
5. Al acabar, envíe el ZIP de soporte. No importe ZIP de otro equipo ni en un teléfono que no tenga la 3.6.16.

**Para Diego, fuera de campo:** la regla K ≥ 5 y M ≥ 4 (§5) necesita su fila en `decisiones.csv`, o limitarse a
series de antes de la 3.6.15.

---

## 2. Pruebas JVM y APK

| Comprobación | Resultado |
| :--- | :--- |
| Export limpio | `git archive 5be257a` al scratchpad. En la copia, y sólo allí, se añadieron `android.overridePathCheck=true` (la ruta del usuario lleva `ñ`), `local.properties` y los JAR de `libtest/`. JDK 11.0.24, `assembleDebug compileDebugUnitTestJavaWithJavac --offline`: BUILD SUCCESSFUL |
| Pruebas | **225 de 225 en verde**, en 17 clases. Coincide con `@Test`: FlujoCalibracion 65 y Version3616 11. Ninguna usa `@Ignore` ni `Assume`. T-S00 132/132 (`#G` 38/38, `#E` 70/70, `#SC` 1) |
| Ajuste necesario del export | La primera ejecución dio 2 fallos que **no son de la app**. `FabricaTest` lee `01_Firmware/…/ecuacionesCalibracion.c`, que no se había exportado. `laColaRepresentativaV2…` (`Version3616Test.java:127`) compara con `06_Calibracion/cola_banco_representativo_v2.csv`, y `git archive` con `core.autocrlf=true` lo saca con CRLF (md5 `78fa2940…`). Con `-c core.autocrlf=false` da `70ef3b86…`. Es la regla `-text` que añadió `dde21fd` y que **no está en 5be257a**. Quien repita la prueba desde `5be257a` necesita `core.autocrlf=false` |
| El APK sale del commit | `assembleDebug` del export da md5 **`4e342be735d5c62cc392d45a3e474e4e`**, idéntico byte a byte al entregado, firma incluida |
| `aapt dump badging` | `versionCode='3616' versionName='3.6.16'`, `sdkVersion:'24'` |
| Firma (`apksigner`) | `CN=Android Debug`, SHA-256 `c990adf6…075f`, la misma que la 3.6.11 y la 3.6.15: **se instala encima** |
| Assets | Cola completa `9ddb7882…`, representativa v2 `70ef3b86…`, anual `d86eddf7…`, `decisiones.csv` `21dd6138…`, grupos `1fa657c9…`. Coinciden con el árbol |

---

## 3. Cierre real de lo declarado

Leyenda:

| Clave | Significado |
| :---: | :--- |
| **C** | Cerrado con código y prueba |
| **Cc** | Cerrado en código, sin prueba propia |
| **P** | Parcial |
| **A** | Abierto |
| **Falso** | El código no hace lo que se declara |

### 3.1 P14

| ID | Declarado | Estado | Evidencia |
| :--- | :--- | :---: | :--- |
| **P14-04** Acta visible con "Aceptar", "Rechazar" y "Parar aquí" | Cerrado | **P** | El diálogo existe (`FlujoCalibracion.java:1633-1640`) y enseña `acta.texto()` entero. Si la pantalla muere, se trata como "Parar", nunca como aceptar (`Base.java:221-223`). "Parar aquí" deja el acta recuperable: `p1404PararAquiDejaElActaSinAceptar`. **Quedan dos cosas.** El texto que se enseña no lleva aún la verificación final ni la fecha, que se añaden después de Aceptar (`:1411`, `:1431`). Y la conformidad sigue siendo una sola, nombre y nota, para los tres códigos, escrita antes del T-C41 (`:938-944`), mientras que "Aceptar" no registra quién acepta (F-06). El test `p1404CadaActaSeEnsenaYSeConfirma` sólo comprueba los títulos |
| `#SC` una vez al día | Cerrado | **C** | `:1416-1430`; `laFechaSeGrabaUnaVezAlDia`. Un `#GC#` sin respuesta graba igual, del lado seguro. Con un corte del `#GC#` posterior, el acta atribuye la fecha "al equipo" (F-07, Baja) |
| **P14-05/-06/-10** Rechazo pendiente | Cerrado | **P** | Bloquea "Continuar", "Calibrar todo", la persistencia y "Aceptar", y sobrevive a matar la app (`:887-891`, `:1561-1563`; `Acta.java:298-300`, `:548-551`; `unRechazoPendienteBloqueaYSoloDiegoLoCierra`). **La "firma de Diego" es cualquier texto que contenga "diego"** (`FlujoCalibracion.java:1506`), sin PIN (`CalibrarActivity.java:480-487`): con "no soy diego" cierra (F-02). Tras el cierre nada impide calibrar enseguida (F-03) |
| **P14-01** Atajo del T-C41 | Cerrado | **P** | Sólo vale dentro de una pulsación (`:1539-1547`). Lo anulan un `#S` (`:1187`), una restauración (`:1216`) y un rechazo (`:1455`); `p1401ElAtajoDeTC41NoSobreviveAUnRechazo`. El acta dice qué apagado usó (`:1063-1064`, comprobado en §4). **El `#SC` del 8 no lo anula, y el acta del b afirma "sin escrituras en medio"** (F-04, Baja). La caída del enlace lo anula sólo de forma indirecta: la IOException termina la pulsación |
| **P14-02** Revalidar lo aceptado | Cerrado | **C** | Los heredados incluyen las actas ACEPTADAS (`:317-338`) y se comprueban con `#G` y `#E` (`:1080-1092`); `p1402ElOchoAceptadoSeRevalidaAntesDelB`. En la traza del T-C41 del b sale `#G,8#` y `#E,8,…` |
| P14-07 ZIP tras cada acta | Cerrado | **Cc** | `CalibrarActivity.java:191-195`, `:387-395`, `:442-468`, sin test. **Sólo se comparte el ZIP ligero del último código**: `ultimoZip` se sobrescribe, y el comentario "el último lleva todo lo anterior" (`:440`) es falso para el ligero, que es incremental (F-05). El de soporte sí va entero |
| P14-B02 SHA-256 del soporte en el acta | — | **A** | El acta se archiva antes de exportar (`Campanas.java:351-358`), así que no puede citarlo |
| P14-08 RF-CAL-15 del b | Cerrado | **C** | `decisiones.csv:8`: `RF-CAL-15 I 11.5 0`; `Version3616Test.java:93-94` (11,5 pasa, 11,6 no) |
| P14-11 `VERSION` sin la del APK | Cerrado | **C** | `TablaCalibracion.java:33`; `Version3616Test.java:97` |
| Hueco de P14 §4 (b con P39 a +15 %) | — | **A en la suite, cubierto aquí** | Ningún test del desarrollador usa el alcance real. Aquí, con `decisiones.csv` real: P39 ×1,14 → "residuo +10,8 % > 10 %", "RMS de I 6,6 %", b ACEPTADO. Otro subagente: con +15,1 % y +17,1 %, dispensado; con +19,1 % y RMS 11,9 %, "queda FUERA de lo dispensado … No se ha empezado la sesión" |
| **P14-S01** Nueva campaña tras renombrar | Cerrado | **Cc** | `Campanas.java:112`, `:122-144` (incluye las `_archivada_`). El test llama a `copiarRenombrados`, no a `sincronizarAlias`, que es Android. Queda S-01 |
| **P14-S02** Renombrado a medias | Cerrado | **Cc** | `FlujoCalibracion.java:1726-1731`, `:1740-1741`; `renombrarCortadoDespuesDelSNSeRecupera`. `RENOMBRA_REVIERTE` (`:1747-1750`) **no tiene ningún test**. Quedan S-02 y S-04 |
| P14-B03 md5 de la cola al cargar | — | **P** | Sólo queda una nota en el registro (`BancoActivity.java:354-357`), que el operador no ve |
| C-P14-2 s_rep 2,24 % o 1,18 % | — | **A, se cierra midiendo** | La A5 del banco nuevo la cierra. Si falta la A5 de todas las sesiones, la app cae **sin avisar** a la A5 de toda la campaña, que es la de las 15:10 (1,18 %) (`Anclas.java:122-124`; B-04) |
| C-P14-3 Tipo I a 3 × 3 | Cerrado | **C** | TIPO-I-REPETIR y `protocoloIncumplido` (`FlujoCalibracion.java:669-690`). En §4 las siete series salen a 5 × 4 y el acta dice "P34 5×4, … re-medida 5×4" |

### 3.2 QA-3615

| ID | Estado | Evidencia |
| :--- | :---: | :--- |
| -01 Corte entre `#SN` y `RENOMBRA` | **C** | El `RENOMBRA` va antes del `#SN` (`FlujoCalibracion.java:1740-1741`) y, tras el corte, se adopta la serie que ya tiene el equipo (`:1726-1731`). Tests `renombrarCortado…` (FCT:1253, :1273). Queda S-02 |
| -02 Otro teléfono tras renombrar | **Cc** | `ImportadorCampana.java:137`. El test abre la campaña como "SLV-002-2026"; el subagente lo repitió con "SLV-002", como la abre el teléfono, y **pasa**. Pero copia un `RENOMBRA` de otra MAC (S-03) |
| -03 Tipo de banco al importar | **C** | `ImportadorCampana.java:119-135`; `Version3616Test.importarConservaElTipoDeBancoOTraeSoloLasSeries`. Salvedad S-06 |
| -04 Retomar acepta y lo dice | **C** | `:1564-1579`; `calibrarTodoSeParaEnElPrimerFallo`. Excepción: un acta vacía (F-01) |
| -05 Recalibrar tras rehacer | **C** | `:293-314`, `:725-733`; `rehacerUnaSerieDelOchoAceptadoPermiteRecalibrarlo` |
| -06 "Pulse Rechazar" | **C** | `:1225-1226`, `:1574-1576`; `trasDosReMedidasNoConformesSeDiceRechazar` |
| -07 Textos de exportación | **Cc** | `CampanaActivity.java:109-114`, `:754`; `ImportadorCampana.java:53` |
| -08 Tamaño del ZIP ligero | **C** | `elResumenDelZipLigeroCabeEn10kB`: con las 15:10, `resumen.txt` de 2236 B frente a 21594 B |
| -09 Renombrar otra MAC | **Cc** | `FlujoCalibracion.java:1716-1720`. La rama "Alta" de Avanzado no mira la MAC (`AdminActivity.java:566-569`, `:603-604`; S-05, Baja) |
| -10 Banco por defecto | **A** | Declarado como no hecho (README:178) |

### 3.3 Lo que declara el desarrollador

| Afirmación | Veredicto |
| :--- | :--- |
| Actas visibles, con "Aceptar", "Rechazar" y "Parar aquí" | **Cierto.** Conformidad todavía única (F-06) |
| `#SC` una vez al día | **Cierto** |
| Rechazo pendiente persistente y "Cerrar sin restaurar" firmado | **Persistente, cierto. "Firmado", falso** (F-02) |
| T-C41 revalidado | **Cierto** (P14-02). "Cualquier escritura anula el atajo": **falso para el `#SC`** (F-04) |
| Serie robusta (QA-3615-01/02, S01, S02) | **Cierto en el camino normal y tras un corte.** Quedan S-01, S-02 y S-04 si el `#SN` falla o se mata la app |
| `BancoPrevio` cuenta lo medido y los equivalentes | **Cierto, con un defecto Alto** (B-01): la equivalencia actúa también entre pasos de la misma cola |
| "Los 7 tipo I vuelven a la cola **con su serie ANULADA** y el motivo" | **Falso en el flujo real** (B-03). Con el ZIP de las 15:10 vuelven, pero sin ANULA: repetir = 0 y S007-S013 siguen elegidas. Sin efecto en el ajuste, porque la serie nueva a 5 × 4 pasa a ser la elegida (§4). La decisión de Diego no queda anotada en el diario |
| Cola v2 con "sólo A5 y OSCURO de control" | md5 cierto. **La frase es inexacta**: la v2 trae también calentamiento, baterías de inicio de sesión, exportación y pausa. Lo que desapareció es la batería por color |
| "Quedan N patrones, M min"; 51 y unos 82 min | **Cierto**, recontado a mano (§6) |
| 225 tests | **Cierto** |
| K ≥ 5 y M ≥ 4 | **Correcta con condición** (§5) |

---

## 4. Casos de principio a fin con el ZIP de las 15:10

La prueba es `MioE2ETest` (scratchpad). Parte del diario real de `campana_SLV-002_20260919_151045.zip`, que tiene 38
series. El banco se mide con `Protocolo.efectivo(p, true, "PRECISO", REPETIR)`, como hace `BancoActivity`.
"Calibrar todo" corre contra `EquipoSimulado.slv002()` con el `decisiones.csv` real. El operador simulado es el de
`FlujoCalibracionTest`.

| # | Paso | Resultado | Salida literal |
| :---: | :--- | :---: | :--- |
| 1 | Instalar encima de la 3.6.11 | **PASA** | Misma firma y `versionCode` 3611 → 3616. `lineasMalas=0 series=38 pasos=7 colaElegida=false colaTipo=COMPLETO` |
| 2 | Importar | **PASA** | El subagente de serie: el ZIP de las 15:10 da "38 series importadas (785 disparos)", también en una campaña ya renombrada. Con la cola REPRESENTATIVO: "se traen las series, no los pasos" |
| 3 | "Quedan N" | **PASA** | Al abrir el Banco (cola COMPLETO, porque el diario de la 3.6.11 no trae evento `COLA`): `Quedan 106 patrones, unos 116 min`. **El operador tiene que cambiar a mano** a Representativo con el botón "Banco:": `+7 … Quedan 51 patrones, unos 82 min` |
| 4 | Terminar el banco representativo | **PASA** sin salir de la pantalla | 67 series nuevas: `A5 5x4=12, OSCURO 5x4=4, PATRON 1x4=34, PATRON 5x4=17`. Queda `Quedan 0 patrones`, con `calibrable 8/b/5=true/true/true`. P34 a P49, todos a 5×4, con series nuevas |
| 4b | Lo mismo, saliendo y entrando del Banco tras cada paso | **FALLA** | `seriePaso 83 (P127)=S097 -> P81; 85 (P19)=S098 -> P18`. Sólo 65 series. El acta del 5 dice `patrones 5: P18 (n = 20), P67 (n = 20), P112 (n = 20)` **y queda ACEPTADA**. Es B-01 |
| 5 | Cambiar la serie | **PASA** | `Serie cambiada: SLV-002 -> SLV-002-2026 (verificada con #GN#). La campaña sigue siendo la misma.` `historial=[SLV-002, SLV-002-2026]` |
| 6 | "Calibrar todo" 8 → b → 5, con el acta visible | **PASA** | `Sesión completa: 8 ACEPTADO; b ACEPTADO; 5 ACEPTADO;` Tres diálogos "Acta del código k", en orden. 4 apagados, `#SC`=1, máscara `0x293`. En el acta: `Equipo: SLV-002-2026 (antes SLV-002)`. Para el b, `T-C41 apagado: el de la persistencia del acta del código 8`. `protocolo banco 8: P34 5×4, P37 5×4, P43 5×4, P44 5×4; re-medida 5×4`. `patrones 5: P18, P67, P112, P127` |

**Negativos (intentos de romperlo).**

| Caso | Resultado | Salida y evidencia |
| :--- | :---: | :--- |
| Corte de Bluetooth en `#L`, `#S,8`, `#SC`, `#GC#` antes y después, `#G,8` de la persistencia, `#V#` del T-C41 del b, `#S,b` y `#S,5` | **PASA** | Siempre IOException, y a la segunda pulsación "Sesión completa". Nada se acepta dos veces. Con el `#S,8` cortado se repite el `#S` (2), igual que con el `#SC` cortado antes de entrar |
| Corte tras `#SN` y `#GN#` sin respuesta | **PASA** | "El equipo ya tiene la serie SLV-002-2026: queda anotada en la campaña" |
| App matada en la re-medida, en el diálogo del acta del 8 y del b, y en la persistencia del 8 y del b | **PASA** | Al reabrir, "Sesión completa: … Ya aceptados antes: 8", con un solo `#S` por código |
| App matada entre `RENOMBRA` y `#SN` | **FALLA la trazabilidad** | La campaña dice SLV-002-2026 y el equipo sigue en SLV-002. Las previas pasan y el acta sale "Equipo: SLV-002-2026 (antes SLV-002)" con `#GN#` = SLV-002 (S-04). El equipo es el mismo (la identidad es la MAC); lo que queda mal es la serie escrita en el acta |
| Re-medida no conforme ×2 en el b y restauración que no entra | **PASA la seguridad, FALLA la firma** | "RESTAURACIÓN NO VERIFICADA del código b"; Rechazar ×3: "NO se rechaza … rechazo pendiente". "no soy diego" produce "Acta cerrada SIN RESTAURAR (firmado por no soy diego)". Justo después, "Calibrar todo b,5" da "Sesión completa: b ACEPTADO; 5 ACEPTADO" (F-02, F-03) |
| Batería a 0 antes de empezar | **PASA la seguridad, FALLA la salida** | "Batería < 10,34 V (n = 0) … No se escribe nada." Queda un acta abierta vacía. Después, en cada pulsación, "El acta en curso no está lista: … no se ha escrito ningún código", sin decir que hay que rechazar. Se sale con Rechazar (F-01) |
| Batería a 0 tras aceptar el 8 | Igual | El 8 queda ACEPTADO y el b bloquea como en el caso anterior |
| Otra MAC con la serie SLV-002 | **PASA** | `canonico` no da SLV-002. Renombrar a SLV-002-2026: "es de otro equipo … no se graba". Importar el ZIP de SLV-002: "la campaña es de otro equipo … no se importa nada" |
| Diario sin series de otra MAC (Nordeste) importado en Coviandina | **FALLA** | `serieActual=SLV-003-2026`: se copia un `RENOMBRA` ajeno (S-03) |
| El 8 un día y el b y el 5 al siguiente | **PASA** | `#SC`=2, "T-C41 apagado: propio" |
| Un diario de la 3.6.16 importado en la 3.6.15 | **FALLA** | "1 líneas del diario no se entienden: no se importa nada" por `RENOMBRA_REVIERTE` (c7ef3c6 `ImportadorCampana.java:74-76`; S-08) |

---

## 5. La decisión propia: K ≥ 5 y M ≥ 4 (`Protocolo.java:103`)

**Juicio: correcta con condición.**

- **A qué afecta hoy:** con el ZIP de las 15:10, a una sola serie: **S038 (P28, 5 × 9)**, la RE-MEDIDA del código 1.
  - El código 1 ya está escrito y no se recalibra.
  - Ninguna serie del 8, del b ni del 5 está a 5 × 9: las del tipo I son 3 × 3 y vuelven a medirse.
  - Con M = 4 exacto quedarían 52 patrones y unos 84 min, en lugar de 51 y 82.
- **Efecto numérico:** comparando las once series 5 × 9 con sus primeros 4 disparos por colocación, la media se mueve
  como mucho un 0,19 % (P48) y la s entre colocaciones cambia en unas pocas cuentas. Frente a la s_rep (1,18 % o
  2,24 %), es despreciable. El límite de la re-medida, 2·s_rep·√(1/5 + 1/K), sólo usa K.
- **Por qué no es "correcta" sin más:**
  - Se aparta de la letra de P9-B3 / P10-C12 (el mismo protocolo por colocación en banco y re-medida) y de la frase
    de Diego ("5 × 4"). La 3.6.11 no descartaba el asentamiento: el primer disparo de cada colocación sale de media
    3,5 cuentas más bajo y va dentro de la media.
  - La única serie que entra gracias a la regla, S038, tiene veredicto **REPETIR** aceptado a mano ("Ok"), con una s
    entre colocaciones de 67,6 cuentas (3,2 %). `BancoPrevio` no mira el veredicto (`BancoPrevio.java:125`; B-05).
- **Condición:** que Diego la firme como fila propia en `decisiones.csv`, o que M > 4 sólo valga para series de
  antes de la 3.6.15 y nunca para el ajuste o la re-medida de un código que se va a escribir.

---

## 6. "Quedan 51": recuento a mano

Hecho en Python sobre el diario del ZIP, la cola v2 y `grupos_patrones_equivalentes.csv`, antes de mirar la cifra de
la app. Las reglas son las de `DECISIONES-Diego`: 5 × 4 para lo que se escribe, los 7 tipo I se repiten y P81 queda
fuera del ajuste pero se mide.

- La cola v2 tiene **58 pasos PATRON**. Las A5 son control y no cuentan, aunque P28 y P4 también figuren como PATRON.
- Una serie cuenta si es la elegida (el ELIGE; si no hay, la última aceptada) y no está anulada. Las A5 S002-S004
  tienen aceptada = 0.

| Orden | Patrón (código, uso) | Cuenta con | K × M |
| :---: | :--- | :--- | :---: |
| 7 | P56 (1, AJUSTE) | S005 | 3×4 |
| 9 | P28 (1, RE-MEDIDA) | S038, **sólo por M ≥ 4** | 5×9 |
| 12 | P7 (1, AJUSTE) | S034 de **P1**, equivalente del grupo G08 | 5×9 |
| 15 | P35 (7, VERIFICACION) | S017 | 3×3 |
| 69 | P41 (d, VERIFICACION) | S023 | 3×3 |
| 79 | P40 (a, VERIFICACION) | S022 | 3×3 |
| 88 | P36 (c, VERIFICACION) | S021 (DUDOSO, aceptada) | 3×3 |

- **Resultado: 58 − 7 = 51.** Coincide con la app ("7 ya medidos cuentan, 0 a repetir; quedan 51 patrones, 82 min").
  También coincide el completo: 133 − 1 − 26 = 106 y 115,7 min, que la app redondea a 116.
- **Los minutos:**
  - 17 pasos a 5 × 4 × 101,5 s y 34 a 1 × 4 × 27,5 s dan 2660,5 s.
  - Los 17 son el 8 y el b (7, que son los que se repiten), los seis del 5 con P19, y las cuatro RE-MEDIDA: P25, P11,
    P86 y P123.
  - Los controles suman 2284 s: calentamiento 600, 2 baterías 20, 4 OSCURO 406, 12 A5 1218 y 2 exportaciones 40.
  - **Total: 4944,5 s = 82,4 min** (`BancoCola.segundos`, `Version3616Test.java:344-353`).
  - Un 46 % del tiempo es de controles. `Protocolo.java:57` sube a K = 5 las A5 que la cola da a K = 3 (P14-B06);
    con la K de la cola saldrían unos 77,5 min.
- **Salvedades:** P28 cuenta sólo por la regla de §5, y P7 cuenta con una serie de otro patrón (B-02 si se rehace).

---

## 7. Defectos nuevos

| ID | Defecto | Evidencia | Severidad |
| :--- | :--- | :--- | :---: |
| **B-01** | **La equivalencia actúa entre dos pasos de la misma cola.** `aplicar` corre en cada `cargar()` del Banco (`BancoActivity.java:78`, `:358`). En la v2, G57 (P81 → P127) y G58 (P18 → P19) son del 5; G70 y G73 los protege TIPO-I-REPETIR. Si el operador vuelve a entrar al Banco después de medir P81, **P127 (AJUSTE) queda HECHO con la serie de P81**, que Diego excluyó (EXCLUYE P81), y P19 (verificación) con la de P18. El 5 se ajusta con 3 patrones, el acta queda ACEPTADA, y ningún criterio lo para, porque PA-14 relaja la cobertura | `BancoPrevio.java:113-137`; `FlujoCalibracion.java:404-422`; §4 caso 4b | **Alta** |
| F-01 | Un acta abierta sin códigos (batería, T-C41 que falla, apagado cancelado) bloquea "Calibrar todo" sin decir "pulse Rechazar". `abrirActa` va antes del T-C41 y de la batería | `FlujoCalibracion.java:933-957`, `:984-986`, `:1573-1576` | Media |
| F-02 | "Cerrar sin restaurar (firma de Diego)" acepta cualquier texto que contenga "diego", sin PIN | `FlujoCalibracion.java:1506`; `CalibrarActivity.java:480-487` | Media |
| F-03 | Tras el cierre SIN RESTAURAR se puede calibrar otra vez enseguida. La curva desconocida pasa a ser la "anterior" de la nueva acta | `:1523-1524`, `:1171`, `:1188` | Media |
| F-05 | Sólo se comparte el ZIP ligero del último código. Los del 8 y del b no salen en el selector, aunque su copia queda en `Download/RTV/` | `CalibrarActivity.java:440`; `Campanas.java:556-586` | Media |
| F-06 | La aceptación no deja quién acepta. La conformidad es una sola, escrita antes de los resultados | `:938-944`; `Acta.java:701-712` | Media |
| S-01 | Un `RENOMBRA_REVIERTE` no viaja: "Nueva campaña" o importar en otro teléfono dejan la serie nueva cuando el equipo sigue en la antigua | `Campana.java:287-291`, `:311-322` | Media |
| S-02 | Si el revierte era falso (el equipo sí tenía la serie nueva), "Cambiar serie" dice "queda anotada" y no anota nada | `FlujoCalibracion.java:1728` | Media |
| S-03 | Se copia un `RENOMBRA` de un diario de otra MAC sin series | `Campana.java:1222-1227`; `ImportadorCampana.java:93-98` | Media |
| S-04 | La serie del acta sale de la campaña, no del `#GN#`. No hay aviso si no coinciden | `FlujoCalibracion.java:359`, `:998` | Media |
| S-08 | Un diario de la 3.6.16 no se importa en la 3.6.15 (`RENOMBRA_REVIERTE`) | c7ef3c6 `ImportadorCampana.java:74-76` | Media |
| B-02 | Rehacer un paso que contó por equivalente anula la serie de otro patrón (P7 anula S034, de P1) y arrastra a todos los pasos que la comparten | `Campana.java:641-644` | Media |
| B-03 | TIPO-I-REPETIR no anula nada en el flujo real, así que la decisión no queda en el diario | `BancoPrevio.java:97` | Media (trazabilidad) |
| B-04 | Sin A5 de ninguna sesión, la s_rep cae sin avisar a la A5 de toda la campaña (1,18 %, la de las 15:10) | `Anclas.java:122-124` | Media |
| B-05 | `BancoPrevio` no mira el veredicto: cuenta S038 (REPETIR, 3,2 %) | `BancoPrevio.java:125` | Media-Baja |
| F-04, F-07, S-05, S-06, S-07, B-06 a B-09 | El `#SC` entre el apagado y el T-C41 del b; la fecha atribuida al equipo; "Alta" sin MAC; la cola por defecto impide adoptar la del ZIP; NONE en el historial; un corte a 5×3 que sólo se ve en Calibrar; dos cifras de "Quedan" con "rápido" desmarcado; equivalentes a 90° en el completo; la frase "sólo A5 y OSCURO" | Informes de los subagentes, contrastados por muestreo | Baja |

**Para la 3.6.17:**

- **B-01:** equivalencias sólo contra patrones que no estén en la cola, y nunca desde un patrón excluido.
- **F-02:** PIN o frase de Diego para cerrar sin restaurar, y bloquear después.
- **F-01:** un acta vacía se descarta sola.
- **S-01 a S-04:** el revierte viaja; se avisa cuando `#GN#` y la serie actual no coinciden; se filtra `RENOMBRA`
  por MAC.
- **B-03:** anotar la decisión aunque el paso no estuviera HECHO.
- **§5:** firmar la regla de M.
- **Pruebas:** tests de `RENOMBRA_REVIERTE` y de B-01.

---

## 8. Qué no se ha hecho

- No se ha medido nada.
- No se ha ejecutado la app en un teléfono. Las pantallas (`CalibrarActivity`, `BancoActivity`, `AdminActivity` y
  `CampanaActivity`) se han juzgado leyendo el código.
- Siguen pendientes T-C41 y T-C43 en SLV-002 con registro archivado (`QA-App-3.6.13.md` §8), y la A5 del banco que
  cierra C-P14-2.
- Las pruebas de ruptura (`MioE2ETest`, `MioEquivTest`, `RomperFlujoTest`, `RomperSerieTest` y `RomperBanco`) están
  en el scratchpad de la sesión, **no en el árbol**.
