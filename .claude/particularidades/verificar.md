# Particularidades de la V3.6 para `orquestador:verificar`

Lo que la común no cubre: los instrumentos de este repositorio, sus recetas y sus trampas. Sin cifras
vigentes: recuentos de pruebas, md5 y qué lleva cada equipo o teléfono están en `RETOMAR.md` y en la
última acta o ZIP con su `HUELLAS.txt`. Mapa: `ARQUITECTURA.map` §M4. Mandan además `CLAUDE.md` §2
(`archivo:línea`), §7 (pruebas), §8 (compilar) y §9 (subagentes).

## 0. Equivalencias con la común

- `ESTADO.md` es aquí `RETOMAR.md`. «La última acta» es la última salida archivada en `pruebas/` para el
  firmware y el último `05_Documentacion/QA-*.md` para la app.
- **No hay compuerta ni packs**: cada instrumento se corre a mano. Lo de la común sobre compuerta, packs,
  `MIDIERON`, `ROTA`/`NOTA`, batería de inyecciones y pack de manuales no tiene objeto aquí.
- Aquí el firmware **sí se graba** (`CLAUDE.md` §3): la frase de la común sobre «un firmware que no se
  toca y sólo se lee» no describe esta línea.

## 1. Los instrumentos

- **Firmware V3.6 (PIC18F47K42)**: arnés `.c` generado del fuente real, compilado con XC8 v2.10 por ruta
  absoluta (`C:\Program Files (x86)\Microchip\xc8\v2.10\bin\xc8-cc.exe`) y ejecutado en MPLAB SIM con
  `mdb.bat` de MPLAB X 5.50 (`hwtool SIM`). Receta R-MDB en `05_Documentacion/TDD-V3.6.md:85-99`; ejemplo
  completo en `01_Firmware/RetroVertical_V3.6.X/pruebas/T-A20.md:16-19` y `T-A20_mdb_ejemplo.txt`.
- **Compilación reproducible**: la base 2020 compilada con XC8 2.10 da el `.hex` idéntico a `d089f962…`
  (`01_Firmware/RetroVertical_V3.6.X/CAMBIOS-V3.6.md` §1). Es la prueba de una cadena nueva.
- **App RTV**: JUnit 4 en la JVM contra `EquipoSimulado`, ejecutado a mano
  (`03_App_Movil/RetroV36/README.md`, apartado «Tests JVM»).
- **Pantalla STONE**: `validar_stone.py` y `decodificar_k.py` (particularidad de `verificar-pantalla-stone`).
- **El equipo**: tramas y ZIP del teléfono con su huella (`06_Calibracion/<equipo>/tramas/`,
  `campanas/HUELLAS.txt`).

**Aquí no se mide con mingw.** Lo que se mide es el `double` de 32 bits y la aritmética de la biblioteca
de XC8 (`05_Documentacion/TDD-V3.6.md:94-95`). Un `gcc` de PC (`D:\toolchain\mingw64`) sólo sirve como
segunda fuente para lógica sin coma flotante; para la coma flotante la segunda fuente es la emulación
IEEE float32 en Python (`T-A20.md`, última columna). No aplican `-fcommon` ni CP1252 de la común.

## 2. Trampas propias del instrumento (además de la común §3)

- **`./gradlew testDebugUnitTest` no arranca en esta máquina**: el ejecutor de Gradle 6.5 no encuentra
  `GradleWorkerMain` porque la carpeta de usuario lleva `ñ`. Se compila con
  `compileDebugUnitTestJavaWithJavac --offline` y se ejecuta con `JUnitCore` a mano, con
  `junit-4.13.2.jar` y `hamcrest-core-1.3.jar` copiados a `libtest/` (fuera de `build/`: `clean` lo
  borra). Que no arranque por el camino normal no es excusa para no ejecutarla (`CLAUDE.md` §8).
- **La lista de clases de `JUnitCore` se escribe a mano: la clase que no se añade no se ejecuta**, y la
  corrida sale verde sin ella. Antes de dar una cifra, contar las clases de
  `app/src/test/java/com/dpi/retrov36/` contra las de la orden. Caso registrado en el README de la app
  (rama `rtv-1.0`): pedía añadir una clase a la lista y la orden no la llevaba; además la línea tenía
  un `\n` literal, que la shell convierte en un argumento más.
- XC8 y `mdb.bat` se llaman **por ruta absoluta**; si no están, ABORTADO, no se cae a otro compilador.
- **Las banderas del arnés reproducen las de `nbproject/Makefile-default.mk`**, en particular
  `-fno-short-double -fno-short-float` (`double` y `float` de 32 bits). Sin ellas se mide otro firmware.
- ABORTADO aquí: un arnés que no compila, un `mdb` que no llega al `break`, una clase de JUnit que no se
  encuentra. Se arregla antes de mirar nada más.
- `.hex`, `.md5`, colas y tramas se guardan byte a byte (`.gitattributes`): no se normalizan al medir.

## 3. Antes de creerte un verde, aquí

- **Una prueba que no asevera no cuenta, y una que asevera con un valor esperado sacado del propio
  código sólo fija comportamiento** (`CLAUDE.md` §7, con sus tres casos). Contar las que aseveran con
  `grep` sobre el cuerpo de cada método `@Test`, no por el nombre de la clase. Fuentes ajenas válidas
  aquí: el fuente del firmware, `05_Documentacion/PROTOCOLO-V3.6.md`, una decisión escrita de Diego, un
  certificado.
- **`EquipoSimulado` es una fuente más, no la verdad**: lo escribió quien escribió la app. Sus respuestas
  se cotejan con `calibracion_v36.c`, `uart_module.c` y tramas reales. Un simulador simétrico donde el
  equipo no lo es (`#T,25.0,25.0#`) esconde justo la inversión de campos que importa (`HISTORIA.md`).
- Número redondo que coincide con «todas las combinaciones»: por eso T-A20 lleva una segunda fuente que
  reproduce las sumas (`T-A20.md`, tabla de resultados).
- Estado entre escenarios: en el arnés de MDB cada `-DK_DESDE`/`-DK_HASTA` es una ejecución aparte.
- Plazos (`@LEERV`, reintentos): la prueba deja escrito por qué ese plazo basta, con la medida de campo
  al lado si la hay.
- Fuera del arnés: los dobles `UART1_Write`/`Read`, `DATAEE_*` y `getMillis`
  (`05_Documentacion/TDD-V3.6.md:88-92`). Listar siempre qué `.c` del firmware quedan fuera.
- El hueco que no grita se caza comparando dos copias de una regla: la app contra `PROTOCOLO-V3.6.md`.

## 4. Vista en rojo: el modelo del repositorio

`FabricaTest` compara las 12 ecuaciones de la app con el **texto** de
`01_Firmware/base_2020_d089f962/RetroVertical1.X/ecuacionesCalibracion.c` y se comprobó alterando una
cifra a propósito (`03_App_Movil/RetroV36/README.md`, tras «Tests JVM»). El caso contrario, para
reconocerlo: una prueba de la 3.6.16 aseveraba el defecto D-1 y se corrigió su **valor esperado**, no el
arreglo (README de la app en la rama `rtv-1.0`, apartado «Tests JVM»).

## 5. Escribir un arnés o una prueba aquí

1. El arnés **se genera del fuente real, nunca se copia a mano** (`05_Documentacion/TDD-V3.6.md:87-88`;
   `T-A20_gen_equiv.py`).
2. Se archiva en `pruebas/`: arnés, generador, guion MDB, salida y un `T-xx.md` con el resultado y el md5
   de cada fuente incluido, que debe coincidir con `hex/fuente.md5` del commit (`TDD-V3.6.md:98-99`).
3. El valor esperado lleva `archivo:línea` del firmware, del contrato o de la decisión, en un comentario
   junto a la aseveración; si no la hay, la prueba se declara «fija comportamiento».
4. El contrato manda: `PROTOCOLO-V3.6.md` sobre firmware y app (`:7-8`); si uno lo contradice, se corrige
   ese, no el protocolo.

## 6. Revisar trabajo delegado, aquí

- Todo hallazgo Alto de un subagente se reabre en el código (`CLAUDE.md` §9): en la P16 uno no se
  reproducía como venía descrito.
- El «modelo» de la común es `EquipoSimulado`: si un cambio toca la app y el simulador a la vez, mirar si
  el simulador replica el firmware o relaja la comprobación.
- Una causa entra en `ROADMAP.md` o `RETOMAR.md` con su salida pegada; dos fuentes que se contradicen
  van a «Contradicciones abiertas» de `ROADMAP.md` (`CLAUDE.md` §2).

## 7. Lo que la JVM y MPLAB SIM NO verifican

Nada de esto sale de una corrida en verde:

- que el firmware grabado sea el `.hex` declarado (sólo `#V#` y el md5 del fichero grabado lo atan);
- que el Bluetooth del equipo responda a lo que la app manda, ni el efecto de un byte no documentado
  (`e`: `CLAUDE.md` §4);
- que la EEPROM conserve lo escrito tras apagar (se ve releyendo con `#G` en el equipo);
- que la pantalla STONE cargada sea el proyecto archivado;
- la óptica, la batería, la temperatura, el patrón físico y su certificado;
- que un coeficiente sirva para **otro** equipo: nunca (L-23).

La lista viaja pegada a toda cifra que salga de aquí. Verde no autoriza a grabar (`CLAUDE.md` §3);
mientras falte la prueba en el equipo, lo que se manda es un encargo de medida (`orquestador:entregar`).

## Diferencias con la común

- Compilador de referencia: XC8 2.10 y MPLAB SIM con `mdb.bat`, no mingw (§1).
- Sin compuerta, packs, batería de inyecciones, `MIDIERON`, `ROTA`/`NOTA`, sonda por UART ni pack de
  manuales; si llegan, se traen de la común (§0).
- El firmware sí se graba: verde no autoriza a grabar, y §7 sustituye a «Lo que el banco NO verifica».
- «Una prueba que no asevera no cuenta» y «valor esperado del propio código» son regla de `CLAUDE.md`
  §7 y no están en la común: candidatas a subir.
- JUnit a mano por la `ñ` y la lista de clases manual como trampa (§2): propias de esta máquina y esta app.
