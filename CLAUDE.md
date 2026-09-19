# CLAUDE.md — Reglas permanentes del repositorio V3.6

Retrorreflectómetro **Vertical**, línea **V3.6** (2026), de DPI Ingeniería & Consultoría.

**Nada de lo que hay aquí está validado contra un equipo físico salvo lo que un ZIP de tramas
respalde.** Las SPEC, las revisiones y las QA salen de leer fuente y de ejecutar en la JVM contra
`EquipoSimulado`. Un documento que cuadra en papel no es un documento medido.

Este fichero tiene las **reglas**. Las cifras del día van en `RETOMAR.md` y en `ROADMAP.md`; si una
cifra aparece aquí, está mal puesta.

---

## 1. Qué es esta línea, y qué no

- Equipos **V3 "SAT-LUX/V3 K42"**: PIC18F47K42, XC8 2.10, placa **"SATLUX H-IoT"**, pantalla STONE
  de 2.ª generación, Bluetooth **UART1 a 9600 8N1** (`05_Documentacion/PROTOCOLO-V3.6.md:16`).
  **No son 115200**: esa es la V4.1/V5, y confundirlas ya costó una sesión (`HISTORIA.md:34`).
- **Qué aporta la V3.6:** la calibración pasa del código a **EEPROM** y se ajusta **desde la app, en
  modo administrador, sin reprogramar**. Todo lo demás se comporta como el firmware de 2020, para que
  la pantalla y la app del cliente sigan funcionando. **La STONE no se toca.**
- **Proyecto independiente**, remoto privado propio `github.com/dieleoz/retrov3.6_2026`. El de la
  línea V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`) y el de la V4.6
  (`D:\IT\P_RetroVertical_V4.6`) son **otros proyectos**: se consultan, pero **nada de aquí se sube
  allí ni al revés**. Si la petición es de otro equipo, se dice en una línea a cuál pertenece y se
  sigue con lo de aquí. `D:\@Proyect\IT\P_RetroReflectometro_Vertical` es una **copia desfasada**:
  antes de editar, comprobar en qué árbol se está.

## 2. La regla que manda sobre todas

**Toda afirmación sobre el código va con `archivo:línea` verificado.** Nada de citar de memoria y
nada de deducir un valor "porque es lo normal". Las cifras que sostienen el resto del trabajo —un
md5, un commit, una hora, un veredicto— **se abren antes de citarlas**, con `git log`, `md5sum` o el
fichero delante.

Tres corolarios, todos pagados aquí:

- **Un resultado negativo se verifica dos veces, con herramientas distintas.** De un listado vacío no
  se concluye "no existe": se concluye "mi listado no lo ve".
- **No se concluye de un indicio sin cruzarlo con una segunda prueba** (`HISTORIA.md:36`).
- **Cuando dos fuentes se contradigan, no se elige por escrito.** Se marca como contradicción abierta
  en `ROADMAP.md` → "Contradicciones abiertas" y se cierra **midiendo o con registro**. Cuando se
  cierre, **se deja escrita la errónea junto a la buena**, como hace ya esa tabla.

## 3. Aquí el firmware SÍ se toca — y por eso hay puertas

Al contrario que en el repositorio V5, donde el equipo patrón no se reprograma, **esta línea existe
para grabar equipos**. Eso no la hace más permisiva: la hace más estricta.

**Nada se graba en un equipo sin las cuatro cosas** (`README.md`, "Reglas"):

1. compilación verificada contra la base de 2020 (`01_Firmware/base_2020_d089f962/`, md5
   `d089f962…`), con el `.hex` atado a un commit y su md5 declarado;
2. revisión del cambio;
3. prueba previa en otra placa, si la hay;
4. **autorización escrita del propietario del equipo** (puerta P6).

**Grabar es irreversible.** SLV-002 tenía `CP = ON`: la lectura ICSP no dio copia y **su firmware
original se perdió al grabar** (`HISTORIA.md:22-23`, `README.md:11`, `RUNBOOK.md:117`,
`05_Documentacion/SPEC-V3.6.md:1083`). La pérdida la autorizó Diego sabiéndolo. La regla que queda:
**antes de grabar, la línea base se levanta por Bluetooth y se archiva** (fases 1 y 2 del
`RUNBOOK.md`), porque puede ser lo único que quede del equipo anterior.

**Nada se da por escrito en un equipo sin su registro** —tramas o ZIP— en el repositorio, con su
huella en `HUELLAS.txt`.

## 4. Nunca se envía a un equipo un byte cuyo efecto no se conozca

El 18-sep-2026, en el barrido de 255 bytes a SLV-002, **después de `e` (0x65) no volvió a responder
ningún byte** (`05_Documentacion/SPEC-V3.6.md:459-465`). En el fuente de 2020 `e` devuelve la lectura
interna sin ecuación (`05_Documentacion/PROTOCOLO-V3.6.md:22`); la variante de SLV-002 no lo tiene
(`HISTORIA.md:20`). Que `e` deje el equipo sin Bluetooth hasta apagarlo sigue siendo **hipótesis sin
confirmar**, y se trata como cierta hasta que alguien mida.

Consecuencias vigentes:

- **Nunca se envía `e` a un equipo no identificado como V3.6.** La app 3.6.0 lo hacía en la
  detección: **no usar** (`03_App_Movil/RetroV36/README.md:7`).
- Orden de detección vigente (3.6.1 en adelante): `#V#` → `9` → `6` → `@LEERV,BLA,1@` sólo si nada
  respondió (`05_Documentacion/SPEC-V3.6.md:464`).
- En un V3 de 2020, `#V#`, `e` y `6` **disparan una medida**: la luz se enciende y pita. Se avisa
  antes.
- **Identificar un equipo empieza por el micro, la placa y la pantalla**, nunca por los textos de la
  pantalla: en una STONE son imágenes.

## 5. La calibración es por equipo físico, nunca por modelo

Regla **L-23** (`README.md:20`, `ROADMAP.md:17`): campaña, ajuste, coeficientes, fecha y acta son de
**cada equipo**. Entre equipos se reutilizan el catálogo de patrones, el método, el firmware y la app
— **los coeficientes no, jamás**. La campaña va atada a **serie y MAC**
(`05_Documentacion/SPEC-Calibracion-V3.6.md:234-235`).

- La serie **se lee de la EEPROM** (`#GN#`), no se teclea.
- La calibración **vence al año** de `#SC` (`05_Documentacion/SPEC-Calibracion-V3.6.md:863`), y la
  fecha de vencimiento va en el acta, en el informe y en **todo registro exportado**, junto con la
  serie y la MAC (`:449`, `:546`).
- `#SC` se escribe **sólo después de aceptar el acta**, nunca antes.
- El contrato de órdenes `#...#` es `05_Documentacion/PROTOCOLO-V3.6.md`, y **manda sobre el firmware
  y sobre la app**: si uno de los dos lo contradice, se corrige ese, no el protocolo (`:7-8`).

## 6. Las puertas no se saltan

El proceso va por **puertas P1-P12** (`ROADMAP.md:108`). Ningún paso empieza sin cerrar el anterior, y
**ninguna puerta se declara cerrada sin la evidencia que pide**. Cuidado con la numeración: "P10-P14"
nombra a la vez puertas del proceso y **revisiones de arquitectura de la app**, que llevan su propia
serie; es contradicción abierta declarada en el `ROADMAP.md`.

**Regla de entrega: a Diego sólo se le entrega una APK con el visto bueno escrito del arquitecto y de
QA — los dos.** Uno solo no basta, y "pasa los tests" no es ninguno de los dos. Cada entrega se copia
como `03_App_Movil/RTV-V<versionName>.apk` y `RTV-V<versionName>-<versionCode>.apk`, con su md5
declarado y comprobada con `aapt dump badging`.

## 7. Un recuento de pruebas en verde no es cobertura

Medido en este árbol, HEAD `f5145ed`: de los **29 tests `Ruptura*`** de
`03_App_Movil/RetroV36/app/src/test/java/com/dpi/retrov36/`, **25 no contienen ni un `assert` ni un
`fail`**. `RupturaE2ETest` y `RupturaEquivTest` no tienen ninguno; `RupturaBancoTest` tiene uno fuera
de sus métodos de prueba. **Pasan hagas lo que hagas.** Este repositorio ya ha usado el recuento de
tests en verde como argumento de entrega.

Regla: **una prueba que no asevera no cuenta.** Un recuento de tests en verde dice, como mucho, que
nada reventó. Los defectos Altos de las últimas puertas salieron de reproducir a mano, no de la suite.

**Y una segunda regla, más fina, que costó tres tropiezos la noche del 19-sep-2026: una prueba que
sí asevera tampoco demuestra nada si el valor esperado salió del propio código que prueba.**

El criterio no es el cuidado que se puso, es **de dónde vino el valor esperado**:

- **Cuenta como cobertura** cuando viene de una fuente **ajena al código**: el fuente del firmware,
  el contrato de tramas, una decisión escrita de Diego, un certificado.
- **No cuenta** cuando se leyó de la salida, o de código recién escrito. Detecta regresiones, que no
  es poco, pero no demuestra que el comportamiento sea el correcto.

Los tres casos reales, para que se reconozcan:

1. El acta salía diciendo `firmado por Firmado por "Ana Ruiz", ITVIAL SAS, …` —el anuncio
   duplicado— y **la prueba aseveraba esa cadena**, copiada de la salida. No podía fallar nunca.
2. El simulador respondía `#T,25.0,25.0#`, **simétrico**, mientras el firmware manda el circuito
   primero. La app leía la óptica del campo equivocado y **ninguna prueba podía verlo**, porque
   invertir dos campos iguales no falla. Habría metido un 0 estructural en la única columna que se
   ajusta.
3. Una prueba de la firma construía los literales del acta y aseveraba sobre ellos: comprueba la
   idea que tiene el autor del código, no el código.

Consecuencia práctica: **al entregar, el recuento va separado** —cuántas aseveran un requisito y
cuántas fijan comportamiento— y una prueba nueva se acompaña de **su salida en rojo** contra la
versión anterior. Si nunca se ha visto fallar, no se sabe qué vigila.

## 8. Compilar la app

La receta está en **`03_App_Movil/RetroV36/README.md:13-22`**, y **no** en la skill `compilar-apk`,
que es de otro proyecto y de otra cadena:

- **JDK 11** (Temurin `jdk-11.0.24+8`), **Gradle 6.5**, **AGP 4.1.1** (`:15,18`).
- `compileSdkVersion 30`, `minSdkVersion 24`, `targetSdkVersion 30`
  (`03_App_Movil/RetroV36/app/build.gradle:7,12,13`).
- **`./gradlew testDebugUnitTest` no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra
  `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se compila con
  `compileDebugUnitTestJavaWithJavac` y se ejecuta con **JUnit a mano**
  (`03_App_Movil/RetroV36/README.md:34-45`). Que la suite no arranque por el camino normal **no es
  excusa para no ejecutarla**.

## 9. Subagentes, y que contradigan

**El agente principal orquesta; los subagentes ejecutan.** Los encargos van **acotados**: qué ficheros
entran, qué se ignora expresamente, y los datos ya verificados para que los confirmen **o los
desmientan con evidencia**. La contradicción explícita es un entregable, no un fallo.

- Revisiones de arquitectura y QA, **adversarias y con alcances disjuntos**; **todo hallazgo Alto de
  un subagente se reabre en el código** antes de darlo por bueno (en la P16, uno no se reproducía
  como venía descrito).
- Para un **juicio** (aprobar una APK, decidir un rumbo) se usa un modelo capaz. Para un **volcado**
  de datos verificables, al revés.
- **El agente principal también se equivoca, y con la misma seguridad.** Cuando un subagente le lleve
  la contraria, se abre el código y se comprueba: no gana quien tenga más contexto.

## 10. Lo que ya salió mal aquí

Se deja escrita la equivocada junto a la buena, para que no vuelva a circular.

| Se concluyó | Era | Cómo se cerró |
| :--- | :--- | :--- |
| "Es un 18F4550": sus textos de pantalla están en el fuente CCS | En STONE los textos son **imágenes**. Es **K42** | Equipo delante, 18-sep (`HISTORIA.md:30-34`) |
| "El firmware 2020 tiene modo de calibración negro/blanco" | `calibrate()` **no se llama nunca** | Lectura del fuente |
| "El Bluetooth va a otra velocidad", porque `e` no contestaba | Va a **9600**; **no existe el comando `e`** en ese equipo | Barrido de 255 bytes |
| Serie `SLV-02` en EEPROM | `#GN` = **SLV-002** | ZIP de las 12:27 (`afdd700`) |
| Códigos 8 y 2 escritos | **1 y 2 escritos; el 8 no** | Mismo ZIP |
| `CAL,0003` "sale del fuente, no de una medida" | `#V#` = CAL 0003 **visto en campo** | ZIP de las 15:10 (`8c8c7de`) |
| El segundo equipo es un V3 y recibe la V3.6 | Es un **V4** (PPS de la V4.1): va a la V4.6 | `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md` |
| Café y lila "sin código en el firmware" | Se miden con el **código del rojo** | Decisión de Diego |
| B-01 de la P15 "cerrado" en la 3.6.17 | **Abierto por otros donantes**; ocho Altos más | `05_Documentacion/REVISION-Arquitectura-P16-V3.6.md:3` |
| "Los tests en verde avalan la entrega" | **25 de 29 `Ruptura*` no aseveran nada** | §7 de este fichero |
| Este repositorio "tiene `CLAUDE.md`" | **No lo tenía** hasta el 19-sep-2026, noche | `git ls-files`, `find` y `ls`, por cuatro subagentes |

Modelo de pantalla, **contradicción abierta y sin cerrar**: `README.md:4`, `HISTORIA.md:16` y
`ARQUITECTURA.map:120` dicen **STA035WT-01**; `04_Pantalla_STONE/SOFTWARE-STONE.md:14` y
`01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md:8` dicen **STVA035WT(-01)**. No se elige: se
mira la etiqueta del equipo.

## 11. Convenciones

- Documentación en **español**, tono técnico y sobrio, **sin emojis**.
- **Ignorar** `build/`, `dist/`, `debug/`, `nbproject/private/`, `.gradle/`, `.idea/`. Los artefactos
  de `build/` no sirven para deducir qué hace el equipo: se abre el `.c`.
- **Los `.apk` no se versionan** (`.gitignore:7`). Tampoco `07 pruebas/`, que es material en bruto del
  teléfono: lo que vale pasa a `06_Calibracion/<equipo>/` con su huella.
- **Los `.hex`, `.md5` y las colas del banco se guardan byte a byte** (`.gitattributes:2-5`): con
  conversión de fin de línea el md5 declarado deja de coincidir con lo versionado.
- **Las SPEC son por versión de hardware:** la V3.6 lleva las suyas y la V4.6 las duplica y adapta.
  La excepción es la app, que es **una sola** para las dos líneas
  (`05_Documentacion/SPEC-App-Unica-V36-V46.md`).
- **Lo aprendido aquí pasa a la V4.6** como lecciones `L-xx` en
  `D:\IT\P_RetroVertical_V4.6\APRENDIDO-DE-V3.6.md`. Escribirlo allí es parte de cerrar el trabajo
  aquí, no un extra.
- Un documento no abre con la cifra en verde: si algo está sin medir, se dice en la primera línea.
- Las decisiones de Diego se registran en `06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`
  (una fila por decisión, con su ID). La app exige que las dispensas estén ahí; una dispensa que no
  esté escrita **no existe**.

## 12. Dónde está cada cosa

| Documento | Para qué |
| :--- | :--- |
| [`ROADMAP.md`](ROADMAP.md) | **Qué se hace y en qué orden.** Puertas, revisiones, el paso en curso y las contradicciones abiertas. **Manda sobre la ejecución**: lo que no está aquí, no está en ejecución |
| [`RETOMAR.md`](RETOMAR.md) | Dónde se quedó el trabajo y el prompt para retomarlo. **Manda sobre el estado**: si dos documentos dan cifras distintas del día, se corrige el otro |
| [`README.md`](README.md) | Qué es esto, qué versiones hay vigentes y dónde está cada carpeta |
| [`RUNBOOK.md`](RUNBOOK.md) | Calibrar un equipo V3 con la V3.6, fase a fase. Es el procedimiento, no el plan |
| [`HISTORIA.md`](HISTORIA.md) | Cómo se llegó aquí y qué se concluyó mal por el camino |
| [`ARQUITECTURA.map`](ARQUITECTURA.map) | Grafo del sistema (Mermaid), con lo visto en equipo marcado como tal |
| [`05_Documentacion/`](05_Documentacion/) | `SPEC-V3.6`, `PROTOCOLO-V3.6`, `SPEC-Calibracion-V3.6`, `TDD-V3.6`, la matriz SPEC↔código, las revisiones de arquitectura y las QA |
| [`06_Calibracion/`](06_Calibracion/) | Catálogo y colas de patrones, y una carpeta por equipo con campañas, actas y `HUELLAS.txt` |
| [`ROADMAP-MEJORAS-App.md`](ROADMAP-MEJORAS-App.md) | Las mejoras de la app evaluadas para esta línea |

Si `ROADMAP.md` y `RETOMAR.md` se contradicen en una hora o un commit, gana **el registro**: `git
log`, el md5 del ZIP o la trama. Ninguno de los dos gana por ser más reciente.
