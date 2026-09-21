# Particularidades de la V3.6 para `orquestador:entregar`

Lo que la skill común no cubre de este repositorio: rutas, recetas, puertas y trampas propias. La regla
vive en la común y en `CLAUDE.md`; si esta particularidad y `CLAUDE.md` chocan, manda `CLAUDE.md` y la
contradicción se anota. Sin cifras vigentes: el estado del día está en `RETOMAR.md`.
Mapa: `ARQUITECTURA.map` §M3 (entrega y puertas) y §M2 (ZIP y huellas).

## 0. Dónde vive cada cosa

Este repositorio **no tiene `ESTADO.md`**. Donde la común dice `ESTADO.md`, aquí es `RETOMAR.md` (estado
y cifras del día); el orden y las puertas P1-P12 están en `ROADMAP.md`; la evidencia es la última acta
o ZIP con su línea en `HUELLAS.txt`. Si dos discrepan, gana el registro (`CLAUDE.md` §10).

## 1. Qué es aquí «verificación en PC» y «validación en hardware»

- Verificación en PC: JUnit en la JVM contra `EquipoSimulado` y los arneses en MPLAB SIM.
- Validación: **el equipo con su registro** (tramas o ZIP con huella). El README de la app llegó a abrir
  con «compila y pasa sus tests JVM» sin haberse probado contra ningún equipo
  (`03_App_Movil/RetroV36/README.md:3-5`): las dos cosas eran ciertas a la vez y sólo la segunda decide.
- Una hipótesis se escribe como hipótesis aunque se trate como cierta: que `e` deje el equipo sin
  Bluetooth (`CLAUDE.md` §4).

## 2. Puertas propias antes de entregar

- **`.hex` para grabar**: las cuatro de `CLAUDE.md` §3 y la línea base levantada por Bluetooth y
  archivada antes (`RUNBOOK.md`, fases 1 y 2). El md5 del `.hex` va en `hex/RetroVertical_V3.6.hex.md5` y
  el de cada fuente en `hex/fuente.md5`. Un envío que diga «grabar» sin eso es un encargo mal redactado.
- **APK para Diego**: arquitecto y QA por escrito, los dos, con el recuento de pruebas separado
  (requisito / comportamiento) y cada prueba nueva vista en rojo (`CLAUDE.md` §6 y §7; particularidad
  de `verificar`).
- **Calibración**: por equipo (L-23), serie leída con `#GN#`, MAC, `#SC` sólo tras aceptar el acta y
  vencimiento al año en acta, informe y todo registro exportado; una dispensa que no esté en
  `06_Calibracion/<equipo>/DECISIONES-Diego-*.md` no existe (`CLAUDE.md` §5).
- Si el orden importa (línea base antes de grabar; `#SC` después del acta), el envío lo dice.

## 3. De dónde se copian las cifras aquí

El ZIP y su `HUELLAS.txt`, la trama, el `.hex.md5`, la salida de `aapt dump badging`, la salida de JUnit
o de MDB. Nunca de `RETOMAR.md` si existe el registro. Casos pagados (`HISTORIA.md`, tabla de errores):
serie `SLV-02` frente a `#GN` = `SLV-002`; códigos «8 y 2» frente a «1 y 2» del ZIP; una ruta de APK
corregida en tres commits seguidos, uno por un carácter de control dentro de la ruta (`git log`).

## 4. Cómo se escribe aquí

- Orden del documento (común §3): primero **qué corre en el equipo y en el teléfono**, y si se probó en
  el equipo. Modelo de cifra redonda con su alcance: «0 diferencias en 786 432 evaluaciones», sólo en
  simulador, en la misma frase (`01_Firmware/RetroVertical_V3.6.X/pruebas/T-A20.md:3-4`).
- Palabras de Diego, textuales: como la columna «Palabras de Diego» de `DECISIONES-Diego-*.md`.
- Manuales de campo: `RUNBOOK.md` y `06_Calibracion/<equipo>/PROCEDIMIENTO-*.md`. Un valor de ejemplo
  (serie, coeficiente, código) se marca **del equipo del ejemplo**: la calibración es por equipo.
- Antes de pedir que se envíe un byte al equipo, su efecto está escrito (`CLAUDE.md` §4): en un V3 de
  2020 `#V#`, `e` y `6` disparan una medida (luz y pitido) y el paso lo avisa; orden de detección en
  `05_Documentacion/SPEC-V3.6.md:464`. Identificar un equipo empieza por micro, placa y pantalla
  (`RUNBOOK.md`, fase 0), nunca por los textos de la pantalla.

## 5. Registros y ZIP: excepción a la común

- Los ZIP **de la app** son evidencia, no paquetes nuestros, y **sí se versionan** en
  `06_Calibracion/<equipo>/campanas/` (`.gitattributes`: `*.zip binary`) con su línea en `HUELLAS.txt`
  (md5, SHA-256 y qué contiene). Es la excepción al «los `.zip` no se versionan» de la común §9.
- ZIP de soporte (completo; se importa en otro teléfono; el acta lo cita por SHA-256) frente a ZIP
  ligero (incremental, no se importa): `03_App_Movil/RetroV36/README.md`, apartado Banco (RF-APP-49 a
  53). Para calibrar o reconstruir una campaña hace falta el de soporte.
- Tramas en `06_Calibracion/<equipo>/tramas/*.txt` y colas `06_Calibracion/cola_banco_*.csv`, byte a
  byte. `07 pruebas/` es capa cruda, no se edita: lo que vale pasa a `06_Calibracion/<equipo>/` con su huella.
- Paquete propio (fuente para otro equipo, anexo para la interventoría): como la común §9, excluyendo
  además `debug/`, `nbproject/private/` y `.gradle/`.

## 6. El `.hex`

Quien recibe sólo graba (MPLAB IPE por ICSP; `05_Documentacion/TDD-V3.6.md`, R-IPE). Con el binario va:
**md5 recalculado** sobre el fichero que se envía (el hash declarado de este repositorio es md5; SHA-256
además si el documento lo usa), el commit y `hex/fuente.md5`, XC8 v2.10 por ruta absoluta
(`01_Firmware/RetroVertical_V3.6.X/CAMBIOS-V3.6.md:14`) con los flags de `nbproject/Makefile-default.mk`,
y con qué versión de la app se probó. `.hex` y `.md5` se guardan byte a byte (`.gitattributes:2-3`);
comprobar con `git ls-files --eol` o recalculando. Delante del equipo la versión sólo se ve con `#V#`,
que en un V3 de 2020 dispara una medida: el documento es la única trazabilidad del binario.

## 7. La APK

- Receta: `03_App_Movil/RetroV36/README.md`, apartado «Compilar» — JDK 11 (Temurin `jdk-11.0.24+8`),
  Gradle 6.5, AGP 4.1.1, `--offline`, `local.properties` con `sdk.dir=C:/android-sdk` (no se versiona).
  No es la skill `compilar-apk`: es de otro proyecto y otra cadena (`CLAUDE.md` §8). La app de
  desarrollo vive en la rama `rtv-1.0`; qué versión lleva cada teléfono lo dice `RETOMAR.md`.
- Rutas Windows con barras normales: `\a` de `7 sw apk\android-sdk` se convirtió en un carácter BEL en
  otro proyecto, y el JDK de aquí vive en esa misma carpeta (`D:/@Proyect/Baliza/7 sw apk/...`).
- Nombre: `03_App_Movil/RTV-V<versionName>.apk` y `RTV-V<versionName>-<versionCode>.apk` (RF-APP-41,
  `05_Documentacion/SPEC-V3.6.md:864`), con md5 declarado y `aapt dump badging` (paquete, `versionCode`,
  `versionName`). Los `.apk` no se versionan (`.gitignore`).
- **`versionCode` nunca baja**: con `android:allowBackup="false"` (`AndroidManifest.xml:19`) volver
  atrás exige desinstalar y borra la campaña (`COMPARATIVA-3.6.17-vs-rtv-1.0.md:86-92`; P16 A-07). Un
  par `versionName`/`versionCode` ya usado no se reutiliza; una APK de otra rama con el mismo paquete y
  `versionCode` mayor no se deja en la carpeta de entrega.

## 8. Lista propia antes de mandar

- [ ] `.hex`: cuatro puertas, línea base archivada, md5 recalculado, commit, compilador y pareja.
- [ ] APK: arquitecto **y** QA, recuento separado, dos copias con nombre, md5, `aapt`, `versionCode` mayor.
- [ ] Calibración: serie de EEPROM, MAC, vencimiento, dispensas en `DECISIONES`.
- [ ] Bytes al equipo: el efecto de cada uno está escrito; nada de `e` a ciegas.

## Diferencias con la común

- `ESTADO.md` no existe: estado en `RETOMAR.md`, orden en `ROADMAP.md`, evidencia en acta o ZIP con
  `HUELLAS.txt` (§0).
- Los ZIP de la app **se versionan** como evidencia (`CLAUDE.md` §3), contra la común §9 (§5).
- Hash declarado del `.hex` y de la APK: **md5**, regla del repositorio; SHA-256 como añadido (§6, §7).
- No aplican: `.docx` generados (común §8; no hay ninguno en `git ls-files`), gerbers y paquete de
  fabricación, copia del árbol a `assets/` (Cordova): la app RTV es Android nativa.
- Se añaden las puertas del repositorio (§2), los bytes de efecto desconocido (§4) y la cadena XC8 e
  IPE por ICSP (§6): existen porque aquí el firmware sí se graba, y grabar es irreversible.
