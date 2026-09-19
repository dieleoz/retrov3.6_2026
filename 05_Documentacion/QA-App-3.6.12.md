# QA de la app 3.6.12 ("Calibrar este equipo", corte B) y de la 3.6.11 (arreglo del banco)

**Veredicto: NO APTO para calibrar en campo. Hay un bloqueante: el botón "Aceptar y grabar fecha" no se
habilita nunca** (QA-3612-01). El botón exige una verificación final que sólo se hace al pulsarlo. Así, un
operador puede escribir y re-medir los códigos 8 y b, y hacer la persistencia, pero no puede cerrar el
acta ni grabar `#SC`. La única salida es "Rechazar", y deja en el equipo curvas escritas sin un acta
aceptada. Hay además un defecto crítico: la 3.6.12 **calibra con pruebas NO APTO** (QA-3612-02).

**La 3.6.11 sí es apta para medir el banco** (sesión 2 incluida). Cierra QA-3610-02, -03, -04 y los de
importación (§3).

**Nada de esto se ha probado contra el equipo ni en el simulador.** T-S07 y T-C43 siguen PENDIENTES
(`TDD-V3.6.md:1249`, `:1376`), y `CalibrarActivity` no tiene ninguna prueba automática. El bloqueante se
ha reproducido en la JVM con las clases compiladas del commit (§2.3).

- Revisado: `fda734e` (3.6.11, `RTV-V3.6.11.apk`, md5 `8f4c5d7bad5a0563666bedf624282641`) y `e159fdf`
  (3.6.12, `RTV-V3.6.12.apk`, md5 `6a7000e7c51b6709926211cb7a95a63e`).
- Fecha: 19-sep-2026. Método: ISTQB. Pruebas JVM en un export limpio de cada commit y recorrido estático
  del código por casos de aceptación. **Sin cambios de código.**
- Rutas de la app relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.
- Escalas de `QA-Flujo-Calibracion-V3.6.md:12-15`. Severidad **Crítica**: puede dejar el equipo mal
  calibrado sin que nadie lo sepa, o saltarse una protección. **Alta**: bloquea al operador o deja un dato
  del acta erróneo. **Media**: cuesta tiempo o induce a error. Y **Baja**. Prioridad: **P1**, antes de
  volver a calibrar; **P2**, antes de entregar a un operador de campo; **P3**, cuando convenga.

---

## 1. Qué hacer con estas dos versiones

| # | Aviso |
| :---: | :--- |
| 1 | **Medir el banco con la 3.6.11 o la 3.6.12**: el código del banco es el mismo en las dos (`BancoActivity.java` no cambia en `e159fdf`) |
| 2 | **No usar "Calibrar este equipo" de la 3.6.12 en un equipo de cliente.** Se escribe, pero el acta no se puede aceptar (QA-3612-01) |
| 3 | Si ya se ha escrito algo con la 3.6.12: **no pulsar "Calibrar" otra vez** con las casillas marcadas, porque reescribe (QA-3612-04). Rechazar el acta y restaurar a mano lo escrito (Avanzado, `#F,k#` si el anterior era el de fábrica) |
| 4 | **No usar Avanzado para escribir el 1 ni el 2**: la app lo permite (QA-3612-08) |
| 5 | Pasar las pruebas y **no seguir con NO APTO**: la pantalla lo muestra con "OK" delante (QA-3612-02) |

---

## 2. Pruebas JVM

### 2.1 Resultado

| Árbol | Compilación | Resultado |
| :--- | :--- | :--- |
| `fda734e` (3.6.11), exportado con `git archive` al scratchpad | `compileDebugUnitTestJavaWithJavac --offline` y JUnit a mano (`03_App_Movil/RetroV36/README.md:36-44`) | **OK, 108 tests** (11 clases), 0,46 s |
| `e159fdf` (3.6.12), exportado igual | Igual | **OK, 120 tests** (12 clases), 0,43 s |

- **Recuento:** 100 heredados de la 3.6.10, más `Version3611Test` (8) en los dos commits y `Version3612Test`
  (12) sólo en `e159fdf`.
- **Fixtures:** junto a la app se exportaron `01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c`
  (para `FabricaTest`) y `06_Calibracion/SLV-002/campanas/`.
- **Ajuste del export:** la ruta del scratchpad lleva `ñ` y el AGP 4.1.1 se niega a compilar. En la copia
  exportada, y sólo en ella, se añadió `android.overridePathCheck=true` a `gradle.properties`. No se compiló
  nada en el árbol de trabajo.

### 2.2 Los APK salen de esos commits

`assembleDebug --offline` en cada export, comparado con el APK entregado:

| APK | `classes.dex` | `classes2.dex` | Export |
| :--- | :--- | :--- | :--- |
| 3.6.11 | `9a5d877c…` | `5ff36f00…` | Idénticos a `fda734e` |
| 3.6.12 | `9a5d877c…` | `0585d056…` | Idénticos a `e159fdf` |

Las dos llevan la cola `9ddb7882…` y el catálogo `07ab9cd8…`.

### 2.3 Cobertura, y lo que no cubre

- `Version3612Test` prueba la lógica pura: D-20, el acta en disco, el corte durante `#S`, `#F`, `#SC`,
  T-A54, T-A63, la tabla y las anclas. **La prueba de T-A63 llama a mano a `verificacionFinal(true, …)`**
  (`Version3612Test.java:252`). Por eso no ve el bloqueante: en la app, esa llamada sólo existe dentro de
  la acción del botón que no se habilita.
- **Nada prueba `CalibrarActivity`**: ni el recorrido, ni el estado de los botones, ni los hilos.
- Reproducido aquí en la JVM, con las clases de `e159fdf` y un programa de 15 líneas fuera del repositorio:
  - acta con el 8 escrito y conforme, datos obligatorios y persistencia OK → `motivoNoAceptable(true)` =
    "falta la verificación final": **el botón Aceptar queda deshabilitado** (QA-3612-01);
  - reescribir el 8 después de la persistencia → `motivoNoAceptable(true)` = `null`: **la persistencia de
    la curva anterior vale para la nueva** (QA-3612-04).

---

## 3. ¿Se cerraron los defectos de la QA de la 3.6.10?

| ID | Estado | Evidencia |
| :--- | :--- | :--- |
| QA-3610-01 Filtro por `origen_x` | **Cerrado** | `Colocacion.java:31-55` (tolerancia y oscuro); `BancoCola.java:87`; `BancoActivity.java:349-352`; `Version3611Test` (`toleranciaSegunOrigenX`, `filtroPorOrigen`, `ningunPasoSeRechazaConSuPropiaX`) |
| **QA-3610-02** Rechazo sin salida | **Cerrado** | "Repetir colocación", "Medir igualmente" con nota obligatoria y "Dejar el paso": `BancoActivity.java:484-511`. La nota va a la serie (`:355`, `:533`) |
| **QA-3610-03** Sólo el primer saltado | **Cerrado** | `BancoCola.java:220-233` recorre todos en vuelta, a partir de `ultimoOfrecido` (`BancoActivity.java:96`, `:152`, `:507`). Test `alFinalSeOfrecenTodosLosSaltados`. `ultimoOfrecido` no se guarda: al reabrir la pantalla, la vuelta empieza por el primero (sin efecto en los datos) |
| **QA-3610-04** Giro y Atrás | **Cerrado** | `AndroidManifest.xml:40-41` (`configChanges`); vertical fija mientras se mide (`BancoActivity.java:282-286`); Atrás con confirmación (`:288-306`); serie en `enCurso` estática y retomada en `onResume` (`:256-272`); ningún diálogo sobre una pantalla destruida (`:400-410`; `Base.java:189-194`). **Sin prueba automática** |
| QA-3610-05 Ausencia cerca del oscuro | Mitigado | "No está en el oscuro" con el oscuro de la campaña + 40 (`Colocacion.java:31-55`) |
| QA-3610-08 Importación por diario no atómica | **Cerrado** | `ImportadorCampana.java:83-84`; test `importarPorDiarioEsAtomicoConUnReasignaDesconocido` |
| QA-3610-09 No trae `PASO` | **Cerrado** | `ImportadorCampana.java:144-145`; test `importarTraeLosPasoSinPisar` |
| QA-3610-10 `ELIGE` antiguas mandan | **Cerrado**, también sin importar: al aceptar una serie, el banco la elige | `BancoActivity.java:567`, `:611-617`, `:633`; test `laSerieDelBancoPasaAElegidaAunqueHayaUnEligeAntiguo` |
| QA-3610-11 / -12 / -13 | Cerrados | `BancoActivity.java:668` (alerta de copia fallida); `:102` (campaña cerrada); `Colocacion.java:33-34` (sin lectura no es ausencia) |
| QA-3610-14 `RTV-V3.6.apk` | **Cerrado** | Ya no está en `03_App_Movil/` |
| QA-3610-06, -07, -15 | Abiertos (P2, tiempo y trazabilidad del código) | Sin cambios en `Veredicto.java` |

---

## 4. Casos de aceptación del flujo "Calibrar este equipo"

Recorridos sobre el código de `e159fdf`. En la columna "Según el código", **PASA**, **FALLA** o **PARCIAL**
frente al resultado esperado. Precondición común: SLV-002 con firmware 3.6.2, banco completo con los tres
OSCURO y la A5, y la campaña en el teléfono.

| ID | Caso | Precondición | Pasos | Resultado esperado | Según el código |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **CA-01** | Camino feliz, códigos 8 y b | APTO; `#GN#` = campaña; s_rep medida; batería n ≥ 19 | "4. Calibrar" → casillas 8 y b → nombre y nota → "Calibrar" y PIN → P43 × 5 → P49 × 5 → "Persistencia" y PIN → apagar y encender → "Aceptar y grabar fecha" | Los dos conformes; persistencia OK; `#V#` y `#G` frescos; `#SC` y `#GC#`; acta ACEPTADA | **FALLA.** Hasta la persistencia coincide con lo esperado: previas (`CalibrarActivity.java:108-138`); por código, batería, `#G` anterior, `ESCRIBIENDO`, `#S`, `#G`, `#E` y `#V#` (`:457-503`); re-medida 5 × 4 (`:510-589`); persistencia (`:623-660`). **Aceptar no se habilita nunca** (QA-3612-01). Además, **el b previsiblemente no queda conforme** (QA-3612-06), y el acta del 8 lleva el oscuro de la sesión del b (QA-3612-10) |
| **CA-02** | Re-medida no conforme | Como CA-01; el patrón de re-medida da un R fuera del 10 % | Re-medida del código → NO CONFORME → repetición → NO CONFORME | Una repetición; después, restaurar, releer y parar. Los dos intentos en el acta | **PASA en la lógica.** `puedeRepetir()` admite dos válidas (`Acta.java:159-161`); aviso "Queda una repetición" (`CalibrarActivity.java:585-587`); restauración y parada (`:524-529`). Los NO_VALIDA se registran y no cuentan (`:563-571`; `Acta.java:105-107`). **Pero se da por restaurado aunque la relectura no coincida** (QA-3612-03) |
| **CA-03** | Corte de Bluetooth durante `#S` | Código 8 en curso | Cortar tras enviar `#S,8` → reconectar (Conexión, pruebas) → "Continuar la calibración a medias" | Resolver el 8 antes de nada: (a) entró → re-medida; (b) no entró → ofrecer repetirlo; (c) ninguna → restaurar | **PARCIAL.** Si el socket cae, `pedir` lanza (`Cliente.java:136-138`), `ESCRIBIENDO` queda en disco (`Acta.java:362-368`) y al volver se resuelve primero (`CalibrarActivity.java:396-398`, `:431-454`); `motivoNoEscribir` bloquea otro `#S` (`Acta.java:332-334`). **(b):** no ofrece repetir, y dice "Códigos escritos y verificados" (QA-3612-09). **Si el enlace sigue abierto pero el equipo no responde**, los timeouts no lanzan (`Cliente.java:130-171`), y `escribir()` borra el estado `ESCRIBIENDO` tras una restauración que tampoco ha respondido (QA-3612-03) |
| **CA-04** | App matada y reanudada | 8 escrito, re-medida a medias | Forzar el cierre → abrir → conectar → pruebas → "4. Calibrar" | "Calibración a medias de SLV-002"; re-medida del 8 desde el principio; ningún `#S` de otro código | **PASA.** Acta en disco, evento a evento (`Acta.java:177-183`); se recupera por MAC (`Campanas.java:235-256`); el botón dice "Continuar…" (`CalibrarActivity.java:152-154`); primero lo no resuelto (`:399-410`). No pide otra vez nombre ni nota (`:334`). Las colocaciones válidas a medias se pierden y se repiten (correcto: no hay intento parcial) |
| **CA-05** | Batería baja | Batería con n = 0, o `9` sin respuesta | "Calibrar"; después, cambiar la batería y volver a intentarlo | No escribe; tras el cambio, se puede seguir | **FALLA.** Bloquea bien (`CalibrarActivity.java:391-395`, `:466-470`; `Bateria.java:51-54`). Pero **la lectura bloqueante queda en la campaña y la previa deshabilita "Calibrar"** (`:131-133`, `:152`). La única otra lectura de `9` es un paso BATERIA pendiente del banco (`BancoActivity.java:167-169`, `:185-207`). **Con el banco completo, cambiar la batería no desbloquea nada** (QA-3612-05). Con n < 19 sólo hay un dato en el acta, sin aviso en pantalla (QA-3612-16) |
| **CA-06** | Otra serie | Equipo con `#GN#` = SLV-003, o campaña de otra MAC | Abrir "Calibrar" | Deshabilitado, con el motivo; ningún `#L`, `#S` ni `#SN` | **PASA.** Previa de identidad por `#GN#` (`CalibrarActivity.java:121-127`); con `NONE`, "dé de alta la serie" (`:124`); campaña por serie y MAC (`Campanas.java:59-73`); el acta, sólo de la misma MAC (`:248`). No se envía nada hasta "Calibrar" (`:338-347`) |
| **CA-07** | Campaña sin OSCURO | Sesión sin OSCURO final, o con deriva | Abrir "Calibrar" | 8 y b "no calculable: falta OSCURO"; los demás siguen | **PASA en lo principal.** `Anclas.oscuro` da NaN con el motivo (`Anclas.java:58-83`); tarjeta "Recta anclada no calculable" y sin casilla (`CalibrarActivity.java:289-293`, `:249-255`). **Matiz:** 3, 4 y 6 en grado 1 comprueban B13 con el `X_OSCURO` que dejó la tarjeta anterior o con 575 (`:269-271`; `Asistente.java:218`) (QA-3612-10) |
| **CA-08** | `#SC` que falla | Acta aceptable | "Aceptar"; `#SC` sin `#OK#` o `#GC#` con otra fecha | El acta no se cierra; se puede reintentar | **No alcanzable por QA-3612-01.** La lógica es correcta: sin `#OK#` y con `#GC#` distinto de hoy, no se cierra y dice "Reintente" (`CalibrarActivity.java:720-731`; `Acta.java:570-572`). Reintentar reenvía `#SC` con la misma fecha, sin daño. No se lee la batería antes de `#SC` (QA-3612-16) |
| **CA-09** | Reescribir un código ya escrito, el 1 y el 2 heredados | SLV-002 con 1 y 2 escritos el 19-sep (`SPEC-Calibracion-V3.6.md:663-664`) | En "Calibrar", buscar la casilla del 1 y del 2; en Avanzado, ajustar y escribir el 1 | Ni el 1 ni el 2 se reescriben (RF-CAL-37) | **FALLA.** En "Calibrar" no tienen casilla (`TablaCalibracion.java:75-76`; `CalibrarActivity.java:232-233`, `:251`). **En Avanzado sí se reescriben**, con grado a elegir, oscuro tecleado y sin batería (QA-3612-08) |
| **CA-10** | Reescribir el 8 ya conforme | CA-01 terminado en el 8, con la casilla todavía marcada | Pulsar "Calibrar" otra vez | No reescribir sin una decisión expresa; la persistencia hecha no vale para la curva nueva | **FALLA.** Reescribe (QA-3612-04) |
| CA-11 | Rechazar | Acta con el 8 conforme | "Rechazar" con motivo | Sin `#SC`; ofrece restaurar lo escrito (AT-17) | **PARCIAL.** Cierra sin `#SC`, pero **no restaura**: remite a Avanzado y a fábrica (`CalibrarActivity.java:740-755`), y el anterior no siempre es el de fábrica (QA-3612-17) |

**Correspondencia con AT-01..AT-22** (`QA-Flujo-Calibracion-V3.6.md:419-440`): AT-04 = CA-06 (pasa);
AT-05 pasa (`CalibrarActivity.java:124`); AT-06 pasa en lo esencial (doble entrada y casilla,
`AdminActivity.java:917-944`); AT-07 = CA-07 (pasa); **AT-08 = CA-01 (falla)**; AT-09 = CA-03 (parcial);
AT-11 = CA-04 (pasa); AT-12 pasa en la lógica (`CalibrarActivity.java:484`, `:499-502`); AT-13 = CA-02
(pasa); AT-15 = CA-05 (falla); AT-16 parcial (QA-3612-13); AT-17 = CA-11 (parcial); AT-18 no se comprueba
(las series elegidas del banco tienen K distintos por diseño, RF-CAL-35); AT-19 y AT-20 pasan (§6); AT-21
pasa (`CalibrarActivity.java:113-114`); AT-22 pasa, vía la previa de s_rep (`:128-130`).

---

## 5. Usabilidad: ¿"un botón y Coloque Pxx → OK"?

Se cuenta un toque por botón o casilla. Teclear un valor es una **entrada de texto**.

| Paso | Toques | Texto | Código |
| :--- | ---: | ---: | :--- |
| Tocar el equipo; se abren las pruebas (≈ 47 s); volver | 2 | | `ConexionActivity.java:80-96` |
| "4. Calibrar este equipo" | 1 | | `ConexionActivity.java:50` |
| Casillas de conformidad (8 y b) | 2 | | `CalibrarActivity.java:251-259` |
| Nombre y nota | | 2 | `:64-65`, `:332-337` |
| "Calibrar" y PIN | 2 | 1 | `:66`, `:338-347` |
| Por código: "Coloque Pxx… OK" y 4 "Levante y apoye" | 5 × 2 = 10 | | `:531-533`, `:574-577` |
| "Persistencia", PIN, "Seguir" y "Apague… OK" | 3 | 1 | `:69`, `:593-602`, `:624-625` |
| "Aceptar y grabar fecha" y PIN | 2 | 1 | `:70`, `:664-669` |
| Exportar el ZIP: volver a Campaña y exportar (el flujo no exporta) | ≥ 3 | | `:718-737` |
| **Total para 8 y b, sin repeticiones** | **≈ 25** | **5** | |

- **Para un solo código:** ≈ 20 toques y 5 entradas de texto. Una re-medida no conforme suma 6 toques.
- **Decisiones técnicas del operador: una por código**, la casilla. **Una de ellas no le corresponde:** la
  del b concede la dispensa de PA-24, que es de Diego (QA-3612-07).
- **Frente a lo pedido:** el núcleo por código sí es "Coloque P43 → OK" y cuatro "Levante y apoye", sin
  elegir grado, método ni patrón. **Pero no es un botón:** el PIN se teclea tres veces, más el nombre y la
  nota; la persistencia y la aceptación son botones aparte, y el ZIP se exporta en otra pantalla.
  RF-APP-34 pide 1 entrada de texto, y hay 5 (`SPEC-V3.6.md:857`) (QA-3612-14).
- Hoy, además, el recorrido **no termina** (QA-3612-01).

---

## 6. Versiones y actualización: 3.6.10 → 3.6.11 → 3.6.12

| Comprobación | Resultado |
| :--- | :--- |
| `aapt dump badging` | 3.6.10: `versionCode='3610'`; 3.6.11: `'3611'`; 3.6.12: `'3612'`, con `versionName` iguales al fichero. **Cada una es mayor que la anterior: Android acepta instalar encima** |
| Firma (`apksigner verify --print-certs`) | Las tres, `CN=Android Debug`, SHA-256 `c990adf69d888a41f5ba6d539a0176c80df1e5c3b5f46dca8dea9d0ca7b8075f`: **la misma** que la 3.6.8, la 3.6.9 y la 3.6.10 |
| Conserva la campaña y el paso del banco | **Sí.** El diario vive en `files/campanas/` (`Campanas.java:69-73`), y ni `fda734e` ni `e159fdf` tocan `Campana.java`: el formato (con `PASO` y `BATERIA`) no cambia. Al abrir, `siguiente()` vuelve al primer paso sin estado (`BancoCola.java:220-225`). **Una serie a medias** al actualizar queda PENDIENTE, no cuenta, y el paso se repite entero. El acta en curso es un fichero nuevo de la 3.6.12 (`Campanas.java:223-227`) |
| Nombres de entrega | `RTV-V3.6.11.apk` = `RTV-V3.6.11-3611.apk`, y `RTV-V3.6.12.apk` = `RTV-V3.6.12-3612.apk` (mismo md5). Ya no hay `RTV-V3.6.apk` |
| Riesgo que queda | El mismo de la 3.6.10: una 3.6.x firmada con otra clave de depuración obliga a desinstalar, y desinstalar borra la campaña. **Exportar antes de actualizar** |

---

## 7. Defectos nuevos

| ID | Defecto | Evidencia | Severidad | Prioridad |
| :--- | :--- | :--- | :---: | :---: |
| **QA-3612-01** | **BLOQUEANTE. "Aceptar y grabar fecha" nunca se habilita.** El botón exige `motivoNoAceptable(true) == null`, y eso pide `verificacionFinalOk`. Pero `verificacionFinal()` sólo se llama dentro de `aceptarInterno()`, a la que sólo se llega pulsando ese botón. Ningún acta se puede aceptar ni grabar `#SC`. Lo escrito queda en el equipo con el acta PENDIENTE, o RECHAZADA si se sale por ahí | `CalibrarActivity.java:158`, `:70`, `:691-713`; `Acta.java:547-552`; única llamada, en `CalibrarActivity.java:709`. El test la llama a mano (`Version3612Test.java:252`). Reproducido en la JVM (§2.3) | **Crítica** | **P1** |
| **QA-3612-02** | **Calibra con pruebas NO APTO.** La previa sólo exige que las pruebas estén hechas (`apto != null`) y pinta "OK Pruebas: NO APTO". El flujo del QA pide APTO, y en Avanzado NO APTO bloquea salvo override con casilla y PIN | `CalibrarActivity.java:115-116`; `QA-Flujo-Calibracion-V3.6.md:215`; `03_App_Movil/RetroV36/README.md:65` | **Crítica** | **P1** |
| **QA-3612-03** | **Una restauración que no se verifica se da por buena.** `restaurar()` siempre devuelve un texto, también con "NO igual a la anterior" o sin respuesta. Con ese texto, el código se marca RESTAURADO (resuelto: deja escribir el siguiente y aceptar) o se anota SIN_ESCRIBIR, que borra el estado `ESCRIBIENDO` que permitía resolver un corte. El texto de SIN_ESCRIBIR no sale en el acta. Caso típico: enlace abierto con el equipo mudo; los timeouts no lanzan excepción. En el intento siguiente, `#G` lee como "anterior" la curva que quedó sin verificar | `OpsEquipo.java:197-219`; `CalibrarActivity.java:441-442`, `:448-450`, `:499-502`, `:524-529`, `:475`; `Acta.java:241-243`, `:371-374`, `:588-651`; `Cliente.java:130-171` | **Crítica** | **P2** (poco probable; sin corregir no se entrega) |
| **QA-3612-04** | **Las casillas siguen marcadas después del flujo, y "Calibrar" reescribe los códigos ya conformes.** Tras reescribir, la persistencia hecha con la curva anterior sigue valiendo. Pulsar "Calibrar" para continuar una re-medida parada reescribe también los conformes | `CalibrarActivity.java:215-218`, `:255-256`, `:152-154`, `:406-417`; `Acta.java:391-399` (`escrito` no toca `persistenciaOk`), `:547-549`. Reproducido en la JVM (§2.3) | **Alta** (salta P9-B8 en la persistencia) | **P1** |
| **QA-3612-05** | **La batería lleva a un callejón sin salida.** Una lectura con n = 0, o sin respuesta (aunque sea un fallo puntual del enlace), queda en la campaña y deshabilita "Calibrar". La app no tiene otro sitio donde volver a leer `9` después del banco: cambiar la batería no desbloquea nada. Para el operador, sin salida salvo abrir otra campaña y perder el banco | `CalibrarActivity.java:131-133`, `:152`, `:391-395`; `Campana.java:343-353`; `OpsEquipo.java:181-191`; `BancoActivity.java:167-169` | **Alta** | **P1** |
| **QA-3612-06** | **Contradicción abierta: el b no puede pasar su propia re-medida.** La tabla re-mide el b con P49. La recta anclada da P49 a −10,2 % (SPEC) o −11,1 % (REFORM) del certificado, y por eso lleva la dispensa de RF-CAL-14. La comprobación 3 de la re-medida es, precisamente, RF-CAL-14 al 10 % sin dispensa. Previsible: dos NO CONFORME y restauración (≈ 90 disparos perdidos). **No se elige aquí:** se cierra decidiendo la regla y midiendo con la curva del banco | `SPEC-Calibracion-V3.6.md:786`, `:827`, `:830`; `06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md:166`, `:169`, `:251`; `Remedida3611.java:162-163`; `TablaCalibracion.java:86` | **Alta** | **P1** |
| **QA-3612-07** | **El operador concede la dispensa de PA-24, que es de Diego.** El b sale escribible con la dispensa "pendiente de Diego", y basta la casilla del operador. `exigeConformidad` se declara, pero nadie lo lee. Contradice "Sin ella, sólo verificar" y "Decide Diego" | `TablaCalibracion.java:35-36`, `:86` (ninguna otra referencia en `src/main`); `CalibrarActivity.java:251-255`, `:324-331`; `SPEC-Calibracion-V3.6.md:786`, `:1038` | **Alta** | **P1** |
| **QA-3612-08** | **Avanzado se salta la tabla RF-CAL-37.** Escribe cualquier código, también el 1 y el 2, con el grado a elegir, la x del oscuro tecleada, las medidas de sesión si la campaña no tiene elegidas (D-08) y sin leer la batería. El texto sigue diciendo que usa "la sesión de medida". Y si escribe un código sin patrón de re-medida en la tabla (7, a, c, d), su re-medida no existe: el acta queda sin resolver para siempre y bloquea escribir y aceptar | `AdminActivity.java:92-120`, `:127-128`, `:582`, `:608`, `:675-700`; `Sesion.java:241-250`; `CalibrarActivity.java:512-516`; `TablaCalibracion.java:83-88`; `Acta.java:335-338`, `:526-530`; `SPEC-V3.6.md:868` (RF-APP-45) | **Alta** | **P1** |
| QA-3612-09 | Reanudar tras un `#S` que no entró **no ofrece repetirlo** (RF-APP-37 b). Con la selección vacía, el flujo acaba diciendo "Códigos escritos y verificados. Ahora: persistencia". Si otro código está conforme, se puede aceptar sin el que falló, y el acta no lo menciona | `CalibrarActivity.java:396-398`, `:444-446`, `:399-427`; `Acta.java:241-243`; `SPEC-V3.6.md:860` | Media | P2 |
| QA-3612-10 | **El oscuro del acta de un código puede ser el de otro.** `X_OSCURO` y `ORIGEN_ANCLA` son estáticos y se fijan tarjeta a tarjeta; al escribir quedan con los de la última. El 8 es de la sesión 1 y el b de la 2 (cola, filas 54 y 98): el acta del 8 lleva el oscuro y el origen de la sesión 2. El dato "oscuro" es uno solo y se pisa. La curva sí sale con su ancla | `CalibrarActivity.java:268-272`, `:294-296`, `:489-493`; `Acta.java:346-350`; `assets/cola_banco_P1-P132.csv` | Media (dato del acta, RF-CAL-43) | P2 |
| QA-3612-11 | `preguntar()` abre el diálogo sin mirar si la pantalla sigue viva. Si Android destruye `CalibrarActivity` a mitad del flujo (Inicio y falta de memoria), el hilo único de `Cliente` se queda esperando para siempre: la app no atiende otra petición hasta reiniciarla | `Base.java:166-184` (frente a `:189-194`); `Cliente.java:38`; `CalibrarActivity.java:531`, `:565`, `:575`, `:624` | Media | P2 |
| QA-3612-12 | **RF-CAL-43 y RF-CAL-36, incompletos.** Los obligatorios son 4 datos más el oscuro. Faltan el SHA-256 del ZIP, la A5 del inicio y del final, la deriva D por sesión, la V mínima y máxima, y "PIN de fábrica". La deriva de la sesión por A5 no se calcula: una sesión con deriva entra en el ajuste | `Acta.java:53`, `:536-546`; `SPEC-Calibracion-V3.6.md:869-884` y `:765-773`; sólo la deriva del OSCURO (`Anclas.java:58-69`) | Media | P2 |
| QA-3612-13 | Sólo hay un `#L` al empezar el flujo. Una re-medida lenta (repeticiones, NO_VALIDA) no envía tramas `#`, y el modo administrador caduca a los 10 min. El `#S` siguiente falla y la app lo presenta como "escritura fallida" | `CalibrarActivity.java:373`; `PROTOCOLO-V3.6.md:95`; AT-16 | Media | P2 |
| QA-3612-14 | 5 entradas de texto donde RF-APP-34 pide 1: PIN tres veces, más nombre y nota | `CalibrarActivity.java:64-65`, `:338`, `:595`, `:665`; `SPEC-V3.6.md:857` | Media | P2 |
| QA-3612-15 | El README y el comentario dicen que tras la persistencia hay que volver a pasar las pruebas. El código no lo exige: la reconexión de la persistencia no llama a `Sesion.reiniciar()`, así que versión y APTO se conservan | `03_App_Movil/RetroV36/README.md:143-144`; `CalibrarActivity.java:156-157`, `:629-637`; `ConexionActivity.java:80-85` | Baja (documento) | P3 |
| QA-3612-16 | No se lee la batería antes de `#SC`, que RF-CAL-41 bloquea con n = 0. El aviso con n < 19 no se muestra. Al aceptar no se exporta el ZIP (QA §2.4 B.5) | `CalibrarActivity.java:391-395`, `:466-470`, `:718-737`; `SPEC-Calibracion-V3.6.md:840-860` | Baja | P2 |
| QA-3612-17 | "Rechazar" no ofrece restaurar lo escrito: remite a Avanzado y a fábrica, y el anterior no siempre es el de fábrica (AT-17) | `CalibrarActivity.java:740-755` | Baja | P2 |

**Observaciones que no son defectos:**

- **O-1.** Los códigos 1 y 2 heredados siguen **sin la comprobación de persistencia**
  (`SPEC-Calibracion-V3.6.md:669`). El flujo nuevo sólo la hace de los códigos de su acta
  (`CalibrarActivity.java:649`), pero un `#SC` nuevo volvería a fechar el equipo entero.
- **O-2.** La 3.6.12 se entrega sin T-S07 ni el simulador (R-SIM), igual que la 3.6.10 sin T-S01. Tres de
  los cinco defectos P1 se habrían visto en el primer recorrido de T-S07.
- **O-3.** El comentario de la clase dice "3.6.11, corte B" (`CalibrarActivity.java:25`). El corte B es
  la 3.6.12.

---

## 8. D-01..D-20 frente a la 3.6.12

| Defecto | Estado | Evidencia |
| :--- | :--- | :--- |
| D-01 Re-medida escondida | **Cerrado** en "Calibrar"; Avanzado remite allí | `CalibrarActivity.java:531-533`; `AdminActivity.java:140-143` |
| D-02 Acta sólo en memoria | **Cerrado** | `Acta.java:167-216`; `Campanas.java:235-273` |
| D-03 Corte durante `#S` | **Parcial** | CA-03; QA-3612-03 y -09 |
| D-04 Serie sin cotejo | **Cerrado** en lo esencial | `AdminActivity.java:917-944` |
| D-05 Identidad por el nombre BT | **Cerrado** para calibrar | `CalibrarActivity.java:121-127` |
| D-06 Administrador mezclado | **Abierto** en Avanzado | QA-3612-08 |
| D-08 Texto y caída a sesión | **Abierto** en Avanzado; cerrado en "Calibrar" (sólo series elegidas de la campaña, `CalibrarActivity.java:203-212`) | `AdminActivity.java:92-94`; `Sesion.java:241-250` |
| D-11 Oscuro y s_rep tecleados | s_rep cerrado; **oscuro tecleado aún en Avanzado** | `AdminActivity.java:127-128` |
| D-12 Grado a elegir | Cerrado en "Calibrar"; **abierto en Avanzado** | `AdminActivity.java:102-117` |
| D-13, D-19 Re-medida frente al certificado | **Cerrado** | `Remedida3611.java:151-169`. Contradicción con la dispensa del b: QA-3612-06 |
| D-14 Acta sin ZIP ni A5 | **Abierto** | QA-3612-12 |
| D-16 Persistencia | **Cerrado**, salvo al reescribir | `CalibrarActivity.java:623-660`; QA-3612-04 |
| D-17 Batería | Leída; **callejón sin salida** | QA-3612-05 |
| D-18 `#V#` fresco | **Cerrado** | `CalibrarActivity.java:485-486`, `:700`, `:734` |
| D-20 Intentos de re-medida | **Cerrado** | `Acta.java:406-419`; `Version3612Test` |
| D-07, D-09, D-10, D-15 | Cerrados en la 3.6.10 o la 3.6.11 | `QA-App-3.6.10.md` §7; §6 aquí |

---

## 9. Qué hace falta para APTO

1. **QA-3612-01:** que "Aceptar" dependa de la persistencia y no de la verificación final, que se hace
   dentro de la acción. Probar el estado del botón, no sólo `Acta`.
2. **QA-3612-02:** exigir APTO, o el override con casilla y PIN, igual que Avanzado.
3. **QA-3612-03, -04, -05, -07 y -08:** restauración verificada o estado sin resolver; desmarcar las
   casillas y anular la persistencia al reescribir; releer `9` desde "Calibrar"; el b sólo con la
   conformidad de Diego registrada; tabla RF-CAL-37 también en Avanzado.
4. **QA-3612-06:** Diego decide cómo se re-mide un código con dispensa de RF-CAL-14. Queda en
   `ARQUITECTURA.map` como contradicción abierta hasta que se decida y se mida.
5. **T-S07 en el simulador**, con el recorrido completo de la pantalla, antes de T-C43 en SLV-002.
