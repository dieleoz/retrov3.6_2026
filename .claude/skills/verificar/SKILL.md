---
name: verificar
description: Corre e interpreta los instrumentos de verificacion del repositorio V3.6 — arneses .c del firmware compilados con XC8 2.10 y ejecutados en MPLAB SIM con mdb, pruebas JUnit de la app RTV ejecutadas a mano (testDebugUnitTest no arranca en esta maquina), cruce de la pantalla STONE, contraste contra tramas y ZIP de un equipo — sin tragarse falsos verdes. Usar antes o despues de tocar el firmware, la app, el protocolo o una cola del banco; al escribir un arnes o una prueba; al revisar trabajo delegado a un subagente; o antes de decir que algo esta listo para grabar, para instalar en el telefono de campo o para una puerta.
---

# Verificar
Mapa: `ARQUITECTURA.map` §M4 (compilacion y pruebas).
Una constante mal copiada, dos campos de una trama invertidos o un codigo de color cambiado **no dan
error**: el firmware compila, la app arranca y el equipo mide otra cosa, con un acta firmada encima.
Verificar es la unica defensa contra eso. Y el instrumento tambien se equivoca, con numeros creibles:
casi todo lo que sigue son formas en que un verde convivio con el defecto.

## 0. Antes de empezar

**Lo que ya esta escrito aqui y manda**: `CLAUDE.md` §2 (toda afirmacion con `archivo:linea`
verificado), §7 (un recuento en verde no es cobertura), §8 (compilar la app) y §9 (subagentes).

**La cifra vigente no vive en esta skill**: recuentos de pruebas, md5, que version esta en cada
telefono y que firmware lleva cada equipo estan en `RETOMAR.md` y en la ultima acta o ZIP con su
`HUELLAS.txt`. Una skill se carga con autoridad y nadie va a la fuente: en otro proyecto una tabla de
cifras copiadas de un acta se leyo como vigente nueve dias. La cura no es actualizar la tabla: **es
no tenerla.**

### Los instrumentos de este repositorio

| Que se verifica | Instrumento | Donde esta la receta |
| :--- | :--- | :--- |
| Firmware V3.6 (PIC18F47K42) | Arnes `.c` **generado a partir del fuente real**, compilado con XC8 v2.10 (`C:\Program Files (x86)\Microchip\xc8\v2.10\bin\xc8-cc.exe`) y ejecutado en **MPLAB SIM** con `mdb.bat` de MPLAB X 5.50, `hwtool SIM` | `05_Documentacion/TDD-V3.6.md`, R-MDB (`:85-99`); ejemplo completo en `01_Firmware/RetroVertical_V3.6.X/pruebas/T-A20.md:16-19` y `T-A20_mdb_ejemplo.txt` |
| Compilacion reproducible | La base 2020 compilada con XC8 2.10 debe dar el `.hex` identico a `d089f962…` | `CAMBIOS-V3.6.md` §1 |
| App RTV | JUnit 4 en la JVM contra `EquipoSimulado`, **ejecutado a mano** | `03_App_Movil/RetroV36/README.md`, apartado «Tests JVM» |
| Pantalla STONE | `validar_stone.py` / `decodificar_k.py` | skill `verificar-pantalla-stone` |
| El equipo | Tramas y ZIP del telefono, con su huella | `06_Calibracion/<equipo>/tramas/`, `campanas/HUELLAS.txt` |

**Aqui no se usa mingw.** El compilador de referencia del firmware es XC8 2.10 y el ejecutor es MPLAB
SIM, porque el `double` de 32 bits y la aritmetica de la biblioteca de XC8 son lo que se mide
(`TDD-V3.6.md:94-95`). Un `gcc` de PC (`D:\toolchain\mingw64`) puede servir como **segunda fuente**
para una logica sin coma flotante, nunca como la medida; para la coma flotante la segunda fuente de
este repositorio es una emulacion IEEE float32 en Python (`T-A20.md`, ultima columna).

## 1. Contra que se compara una corrida

Se abre **la ultima acta o salida archivada** (en `pruebas/` para el firmware; la del ultimo QA para
la app) y se compara linea a linea. **Si tu corrida no da lo mismo, la diferencia es el hallazgo**,
sea mejor o peor. Si no coincide, alguien toco algo: averigua que **antes** de seguir.

**Apunta el total ANTES de tocar nada.** Sin ese numero no hay forma de saber despues si lo que
anadiste se mide.

## 2. Los tres estados, y ABORTADO no es PASS

| | significa |
|---|---|
| `PASS` | corrio y cumple |
| `FALLA` | corrio y **no** cumple |
| `ABORTADO` | **no pudo correr** — no dice *nada* del firmware ni de la app |

Codigos de salida: `0` PASS · `1` FALLA · `2` ABORTADO. **ABORTADO gana sobre FALLA.** Un arnes que no
compila, un `mdb` que no llega al `break`, una clase de JUnit que no se encuentra: todo eso es
ABORTADO, y se arregla antes de mirar nada mas. Mientras un instrumento esta abortado, lo que vigilaba
entra sin mirar.

Si cambias el comportamiento a proposito y el instrumento deja de saber leerlo, eso es ABORTADO y
esta bien — **actualizar el instrumento es parte del cambio, no un paso posterior.**

## 3. Sospecha del instrumento antes que del codigo

> Un «no aparece» no es un hallazgo hasta haber descartado al buscador. **Un negativo se verifica
> con dos herramientas** (`CLAUDE.md` §2). En este repositorio se concluyo que `CLAUDE.md` no
> existia y cuatro subagentes lo confirmaron con `git ls-files`, `find` y `ls` (`HISTORIA.md`, errores);
> en el Vertical se escribio que no estaban los fuentes y estaban los 45.

- **`./gradlew testDebugUnitTest` no arranca en esta maquina**: el ejecutor de Gradle 6.5 no
  encuentra `GradleWorkerMain` porque la carpeta de usuario lleva `ñ` (`C:\Users\Diego.Zuñiga`). Se
  compila con `compileDebugUnitTestJavaWithJavac --offline` y se ejecuta con `JUnitCore` a mano, con
  `junit-4.13.2.jar` y `hamcrest-core-1.3.jar` copiados a `libtest/` (fuera de `build/`: `clean` lo
  borra). **Que no arranque por el camino normal no es excusa para no ejecutarla** (`CLAUDE.md` §8).
- **La lista de clases de `JUnitCore` se escribe a mano: una clase de prueba que no se anade no se
  ejecuta**, y la corrida sale verde sin ella. Antes de dar una cifra, contar las clases de
  `app/src/test/java/com/dpi/retrov36/` y compararlas con las de la orden. Ejemplo (21-sep-2026, rama
  `rtv-1.0`, no vigente): el README pedia «anadir `Rc6DefectosTest` a la lista» y la orden no la
  tenia; ademas llevaba un `\n` literal en mitad de la linea, que la shell convierte en un argumento
  mas.
- **Un compilador que existe no es un compilador que mide.** XC8 y `mdb.bat` se llaman **por ruta
  absoluta**; si no estan, ABORTADO, no se cae a otro. Antes de fiarte de una cadena nueva, compila la
  base 2020 y compara con `d089f962…`.
- **Las banderas del arnes reproducen las del proyecto** (`nbproject/Makefile-default.mk`, en
  particular `double` y `float` de 32 bits, `-fno-short-double -fno-short-float`), no tapan defectos.
  Sin ellas se mide otro firmware.
- **Cmdlets que «no existen»** (`Get-FileHash`, `Compress-Archive`): el `PSModulePath` de una sesion
  de IDE rompe el autocargado. Usar Python o `md5sum`.
- **CRLF**: casi todo aqui esta en CRLF. Una busqueda o una mutacion multilinea con `\n` no casa y no
  avisa. Los `.hex`, `.md5`, colas y tramas se guardan byte a byte (`.gitattributes`): no los
  normalices al medir.
- **`build/`, `dist/`, `debug/` no sirven** para deducir que hace el equipo: se abre el `.c`
  (`CLAUDE.md` §10).

## 4. Antes de creerte un verde

**a) Una prueba que no asevera no cuenta** (`CLAUDE.md` §7). En este repositorio la mayoria de los
tests `Ruptura*` no contenian ni un `assert` ni un `fail`: pasan hagas lo que hagas. Contar las que
aseveran con `grep` sobre el cuerpo de cada metodo `@Test`, no por el nombre de la clase.

**b) Una prueba que asevera tampoco demuestra nada si el valor esperado salio del propio codigo.**
Cuenta como cobertura cuando el valor viene de una fuente **ajena al codigo** (el fuente del
firmware, `PROTOCOLO-V3.6.md`, una decision escrita de Diego, un certificado); si se leyo de la
salida o de codigo recien escrito, **fija comportamiento**: detecta regresiones, no demuestra que sea
correcto. Los tres casos reales de `HISTORIA.md`: el acta con el anuncio duplicado aseverado tal
cual; el simulador que respondia `#T,25.0,25.0#` **simetrico**, de modo que invertir los dos campos no
podia fallar; y la prueba de la firma que construia sus propios literales.

**c) El total no se movio al anadir algo.** Si escribes una prueba y el total sigue igual, no la estas
ejecutando (vease la lista de `JUnitCore`) o no la estas midiendo.

**d) Una comprobacion que nunca examina un candidato.** Un bucle con `break` sobre una condicion
siempre cierta sale verde sin evaluar nada. **Un numero redondo que coincide con «todas las
combinaciones posibles» es sospechoso**; por eso T-A20 lleva una segunda fuente que reproduce las
sumas (`T-A20.md`, tabla de resultados).

**e) El simulador del equipo es una fuente mas, no la verdad.** `EquipoSimulado` lo escribio quien
escribio la app: si responde lo que la app espera, las pruebas cuadran con la idea del autor. Sus
respuestas se cotejan con el fuente del firmware (`calibracion_v36.c`, `uart_module.c`) y con tramas
reales. Un simulador **simetrico** donde el equipo real no lo es esconde exactamente el defecto que
importa.

**f) Estado que se arrastra entre escenarios.** Un resultado que depende del orden de las pruebas no
es un resultado. En el arnes de MDB, cada `-DK_DESDE`/`-DK_HASTA` es una ejecucion aparte por algo.

**g) Medir antes de que el defecto tenga tiempo de aparecer.** Cuando una prueba comprueba que algo
**no** pasa (un plazo de `@LEERV`, un reintento), deja escrito **por que ese plazo basta**, con la
medida de campo al lado si la hay.

**h) Un fichero fuera del arnes es un punto ciego permanente.** Lista siempre que `.c` del firmware
quedan fuera del arnes y con que dobles se sustituyen (`UART1_Write`/`Read`, `DATAEE_*`, `getMillis`:
`TDD-V3.6.md:88-92`).

**i) El hueco que no grita.** No falla: falta. Se caza **censando y comparando** (`grep` de
declaraciones contra usos; `diff` de dos copias de una regla — p. ej. la app y `PROTOCOLO-V3.6.md`),
no leyendo.

## 5. Una prueba que nadie ha visto fallar es un adorno

**Toda prueba nueva se acompana de su salida en rojo** contra la version anterior (`CLAUDE.md` §7).
Si nunca se ha visto fallar, no se sabe que vigila.

- Si el instrumento parte de verde: verde -> FALLA al inyectar el defecto.
- Si parte de rojo: **el numero de fallos cambia** al inyectar y **vuelve exactamente al valor de
  partida** al restaurar.
- Restaura y confirma con **`git diff` vacio**, no con la impresion de haber restaurado.

`FabricaTest` es el modelo: compara las 12 ecuaciones de la app con el **texto** de
`01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c`, y se comprobo alterando
una cifra a proposito (`03_App_Movil/RetroV36/README.md`, tras «Tests JVM»).

**Un refactor puede apagar una prueba sin romperla**: si tocas la forma de un bloque que una prueba
lee por texto, repite el control negativo.

**Todo script de usar y tirar que sustituye texto lleva `assert viejo in texto` antes de escribir.**
Un `replace` que no casa (CRLF, escapes) imprime «ok» sin cambiar nada.

## 6. Pruebas que exigen el defecto: una por una, nunca en bloque

Al arreglar un defecto fallan las pruebas que lo exigian, y eso es correcto. Cada una acaba, anotada,
en: **se borra** (solo documentaba el defecto), **se invierte** (pasa a exigir lo nuevo), **se
conserva** (media otra cosa), **se reapunta** (solo cambio el texto que buscaba) o **se retira con el
motivo escrito** y quien cubre ahora esa propiedad. Ejemplo de este repositorio: una prueba de la
3.6.16 aseveraba el defecto D-1 y se corrigio su **valor esperado**, no el arreglo (rama `rtv-1.0`,
README de la app, apartado «Tests JVM»).

Reescribir en bloque hasta que todo pase es ajustar el instrumento hasta que de verde. **El mejor
termometro de un arreglo son los fallos que desaparecen solos.** Todo rojo esperado lleva **fecha y
dueno**; al arreglarlo, su marca se limpia en el mismo commit.

## 7. Escribir un arnes o una prueba

1. **El arnes se genera a partir del fuente real, nunca se copia a mano** (`TDD-V3.6.md:87-88`;
   `T-A20_gen_equiv.py`). Si no puedes leer el dato del artefacto real, aborta: un numero escrito a
   mano «que coincide» mide el valor viejo el dia que alguien lo cambie.
2. **Archivar en `pruebas/`**: arnes, generador, guion MDB, salida y un `T-xx.md` con el resultado y
   el md5 de cada fuente incluido, que debe coincidir con `hex/fuente.md5` del commit (`TDD-V3.6.md:98-99`).
3. **Suelo de cordura en todo censo**: si una lectura devuelve sospechosamente poco, aborta.
4. **Valor esperado de fuente ajena**: `archivo:linea` del firmware, del contrato o de la decision, en
   un comentario junto a la aseveracion. Si no la hay, la prueba se declara «fija comportamiento».
5. **Pregunta si mides un requisito real o un deseo tuyo.** El contrato manda:
   `PROTOCOLO-V3.6.md` manda sobre firmware y app (`:7-8`); si uno lo contradice, se corrige ese.
6. **Una prueba que declara que no mide es correcta; una que da verde sin ejercer nada, no.**

## 8. Revisar trabajo delegado: por el diff, no por el informe

Un informe — propio o de un subagente — **no es una medida** (`CLAUDE.md` §9).

- **Todo hallazgo Alto de un subagente se reabre en el codigo** antes de darlo por bueno; en la P16
  uno no se reproducia como venia descrito.
- Si el cambio toca **la app y `EquipoSimulado` a la vez**, mira si el simulador replica el
  comportamiento del firmware o relaja la comprobacion. Las dos copias pueden mentir juntas.
- Antes de escribir una causa en `ROADMAP.md` o `RETOMAR.md`, **reproduce y pega la salida**.
- Una causa que se cae **se marca refutada, no se borra**; la erronea queda escrita junto a la buena.
- **Cuando dos fuentes se contradicen no se elige por escrito**: va a «Contradicciones abiertas» de
  `ROADMAP.md` y se cierra midiendo.
- **Un hallazgo del banco es material a validar, no un veredicto**: prueba que dos artefactos se
  contradicen, no cual tiene razon ni que hace el equipo.

## 9. Lo que la JVM y el simulador NO verifican

Todo lo de aqui **se ejecuta en la JVM o en MPLAB SIM: no mide un equipo** (`CLAUDE.md`, primera
linea). Nada de esto sale de una corrida en verde:

- que el firmware grabado sea el `.hex` declarado (solo `#V#` y el md5 del fichero grabado lo atan);
- que el Bluetooth del equipo responda a lo que la app manda, ni el efecto de un byte no documentado
  (`e`: `CLAUDE.md` §4);
- que la EEPROM del equipo conserve lo escrito tras apagar (se ve releyendo con `#G` en el equipo);
- que la pantalla STONE cargada sea el proyecto archivado;
- la optica, la bateria, la temperatura, el patron fisico y su certificado;
- que un coeficiente sirva para **otro** equipo: nunca (L-23).

La lista **viaja pegada a toda cifra que salga de aqui**. **Verde no es entregable, y no autoriza a
grabar** (puertas de `CLAUDE.md` §3). Mientras falte la prueba en el equipo, lo que se manda es un
**encargo de medida** — skill `entregar`.

## Diferencias con la comun

- Quitado: §0 `.claude/particularidades/verificar.md` — no existe aqui; los instrumentos del repo van en la tabla de §0.
- Cambiado: `ESTADO.md` -> `RETOMAR.md` y ultima acta/ZIP con `HUELLAS.txt` (C1).
- Cambiado: compilador de referencia `D:\toolchain\mingw64` -> XC8 2.10 + MPLAB SIM con `mdb.bat` (MPLAB X 5.50); mingw solo como segunda fuente sin coma flotante (C9 no aplica).
- Anadido: `testDebugUnitTest` no arranca por la `ñ`; JUnitCore a mano; la lista de clases a mano como trampa — `CLAUDE.md` §8 y README de la app.
- Anadido: §4 a/b «prueba que no asevera» y «valor esperado sacado del propio codigo», con los tres casos de `CLAUDE.md` §7.
- Anadido: §4 e, `EquipoSimulado` como fuente del autor (caso `#T` simetrico).
- Anadido: §7 arnes generado del fuente, archivo en `pruebas/` con `fuente.md5` — `TDD-V3.6.md` R-MDB.
- Anadido: §8 reabrir hallazgos Altos, contradicciones a `ROADMAP.md` — `CLAUDE.md` §2 y §9.
- Quitado: compuerta con acta no idempotente y `--rapido`, packs por texto (`fuente.py`, numeracion `_01_`), bateria de inyecciones, `MIDIERON`, `ROTA`/`NOTA`, sonda por UART, pack de manuales — este repo no tiene compuerta ni packs; si llegan, traerlos de la comun.
- Cambiado: §10 «Lo que el banco NO verifica» -> §9 con lo propio (EEPROM, Bluetooth, optica, L-23); se quita cableado de campo, reles y radios.
- Cambiado: la frase «si el firmware no se toca, aplican sin banco» de la comun no aplica: aqui el firmware SI se graba (`CLAUDE.md` §3).
- Quitado: ejemplos de Semaforos/Baliza/Camion salvo los que ensenan algo sin equivalente aqui; anadidos T-A20, `FabricaTest` y D-1.
