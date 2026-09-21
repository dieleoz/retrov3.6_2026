---
name: entregar
description: Prepara lo que sale del repositorio V3.6 hacia Diego, el cliente (Coviandina), la interventoria o el tecnico de campo — un .hex para grabar un V3 por ICSP, un APK de la app RTV, un procedimiento o runbook de campo, un acta, un informe de ajuste, un encargo de medida — y decide si lo que toca es un encargo o una entrega. Usar al redactar o revisar cualquier envio, antes de copiar un APK a 03_App_Movil/, antes de dar un .hex por grabable, antes de citar una cifra fuera del repositorio o antes de dar por cerrada una pregunta o una puerta.
---

# Entregar
Mapa: `ARQUITECTURA.map` §M3 (entrega y puertas) y §M2 (ZIP y huellas).
Un documento que sale del proyecto es una **autorizacion implicita**: quien lo recibe asume que
puede actuar sobre lo que dice. Aqui lo que hay al otro lado es un instrumento de medida cuya
calibracion firma un acta, y un micro que **se graba de forma irreversible** (`CLAUDE.md` §3): un
error en el papel acaba en un equipo que ya no se puede devolver a como estaba.

## 0. Antes de empezar: donde vive cada cosa en este repositorio

- **Reglas**: `CLAUDE.md`. Si esta skill y `CLAUDE.md` chocan en una regla, manda `CLAUDE.md` y la
  contradiccion se anota.
- **Estado del dia** (que firmware lleva cada equipo, que APK hay en cada telefono, que codigos estan
  escritos, commits): `RETOMAR.md`. **Orden de trabajo y puertas P1-P12**: `ROADMAP.md`. **Evidencia**:
  la ultima acta o ZIP, con su linea en `HUELLAS.txt`. Este repositorio **no tiene `ESTADO.md`**.
- Si `ROADMAP.md` y `RETOMAR.md` dan cifras distintas, gana el registro (`git log`, md5 del ZIP, la
  trama), no el mas reciente (`CLAUDE.md` §10).

**La cifra del dia nunca vive en esta skill.** Ni md5 vigentes, ni versiones de APK entregadas, ni
recuentos de pruebas: se abren en su fuente cada vez.

## 1. La distincion que decide que se manda

| paquete | cuando se manda | que es |
|---|---|---|
| **Encargo de medida** | siempre que falte una medida | Una PETICION: que alguien ponga el equipo delante y mida, lea `#V#`/`#GN#`, exporte un ZIP. **No entrega nada instalable** |
| **Entrega de version** | solo con la verificacion en PC en verde **y** la validacion en equipo hecha, **y** las puertas que pida (seccion 2) | Lo que alguien va a grabar en un PIC o instalar en el telefono de campo |

Las dos condiciones, no una. Aqui la verificacion en PC es JUnit en la JVM contra `EquipoSimulado`
y los arneses en MPLAB SIM; la validacion es **el equipo con su registro** (tramas o ZIP). En este
repositorio el README de la app llego a abrir con «compila y pasa sus tests JVM» con la advertencia
de que **no se habia probado contra ningun equipo** (`03_App_Movil/RetroV36/README.md:3-5`): las dos
cosas eran ciertas a la vez, y solo la segunda decide.

Si falta la medida, **no se bloquea todo**: se manda el encargo, y el encargo lo dice en su primera
linea. Confundir los dos paquetes es el error que mas cuesta.

> Una afirmacion sobre el equipo que depende de una medida que nadie ha tomado **no se escribe en
> ningun documento que salga**. Ejemplo de este repositorio: que `e` deje el equipo sin Bluetooth es
> **hipotesis sin confirmar** (`CLAUDE.md` §4) y asi se escribe, aunque se trate como cierta.

## 2. Puertas que este repositorio exige antes de entregar

**Un `.hex` para grabar** (`CLAUDE.md` §3; `README.md`, «Reglas»):

1. compilado con XC8 2.10 y verificado contra la base 2020 (`01_Firmware/base_2020_d089f962/`),
   con el `.hex` **atado a un commit** y su md5 declarado (`hex/RetroVertical_V3.6.hex.md5`, y
   `hex/fuente.md5` con el md5 de cada fuente incluido);
2. revision del cambio;
3. prueba previa en otra placa, si la hay;
4. **autorizacion escrita del propietario del equipo** (puerta P6).

Y antes de grabar, **la linea base del equipo se levanta por Bluetooth y se archiva**
(`RUNBOOK.md`, fases 1 y 2): SLV-002 tenia `CP = ON`, la lectura ICSP no dio copia y su firmware
original se perdio al grabar (`HISTORIA.md:22-23`). Un envio que diga «grabar» sin esas cuatro cosas
y sin la linea base archivada es un encargo mal redactado, no una entrega.

**Una APK para Diego** (`CLAUDE.md` §6): **visto bueno escrito del arquitecto y de QA, los dos**.
Uno solo no basta y «pasa los tests» no es ninguno de los dos. El recuento de pruebas que la
acompane va **separado**: cuantas aseveran un requisito (valor esperado de una fuente ajena al
codigo) y cuantas fijan comportamiento; y cada prueba nueva, con su salida en rojo contra la version
anterior (`CLAUDE.md` §7, skill `verificar`).

**Una calibracion** (`CLAUDE.md` §5): por equipo fisico, nunca por modelo (L-23); atada a serie
(leida con `#GN#`, no tecleada) y MAC; `#SC` solo despues de aceptar el acta; la fecha de
vencimiento (un ano desde `#SC`) va en el acta, en el informe y en todo registro exportado, junto con
serie y MAC. **Una dispensa que no esta en `06_Calibracion/<equipo>/DECISIONES-Diego-*.md` no
existe** (`CLAUDE.md` §5).

## 3. Las cifras se copian de su fuente, nunca a mano

Antes de poner un numero, un codigo, una hora, un md5 o un «funciona» en algo que sale, **vuelve a
abrir el artefacto que lo contiene**: el ZIP y su `HUELLAS.txt`, la trama, el `.hex.md5`, la salida
de `aapt dump badging`, la salida de JUnit o de MDB. No del mensaje anterior, ni de `RETOMAR.md` si
existe el registro, ni de un borrador.

Casos de este repositorio (`HISTORIA.md`, errores): la serie se escribio `SLV-02` y `#GN` decia
**SLV-002**; se dieron por escritos los codigos 8 y 2 y el ZIP decia **1 y 2**. Y en `RETOMAR.md`
del 21-sep-2026 la ruta de una APK hubo que corregirla tres commits seguidos, uno de ellos por un
caracter de control dentro de la ruta (`git log`, `2f775a3`, `b6b4d46`, `e086c80`; ejemplo, no
vigente).

**Las cifras caducan mientras se prepara el envio.** Con agentes comiteando a la vez, se copian
**despues** de la ultima corrida completa e inmediatamente antes de mandar; si entre medias corre
otra, se vuelven a copiar. Y la pasada que da las cifras se corre con el **arbol quieto** y se
confirma con una segunda.

## 4. Un documento no abre con la cifra en verde

Orden obligatorio de todo documento que reporte una verificacion (`CLAUDE.md` §10):

1. **Que corre hoy en el equipo o en el telefono**, y que esto no es eso.
2. **Si se ha probado en el equipo o no**, con esas palabras — no con una cifra que lo insinue.
3. **Que esta roto o sin causa**, en un bloque que no se pueda saltar.
4. Solo despues, las novedades.

**Una cifra perfecta engana mas que una mala**: «0 diferencias en 786 432 evaluaciones» es cierto y
es **solo en simulador** (`01_Firmware/RetroVertical_V3.6.X/pruebas/T-A20.md:3-4` lo dice en la
misma frase; asi se escribe). Cuando el resultado sea redondo, la frase que lo acompana es de
**alcance** — que quedo sin medir —, no de celebracion.

## 5. Textual lo que dijeron, aparte lo que proponemos

Al citar a Diego, al cliente o a la interventoria: **comillas y palabras exactas**, como hace la
columna «Palabras de Diego» de `DECISIONES-Diego-*.md`. Lo que el proyecto propone por defecto va
aparte, marcado (`▸ Si no hay respuesta:`), nunca en el mismo parrafo como si fuera lo acordado.

**Y una hipotesis nunca se redacta como una reparacion.** Primero la lectura, despues la pieza; si
es hardware, se pide la medida, no se dictamina.

## 6. Cada envio es nuevo, con solo lo que falta

No se reedita el documento anterior: se crea uno nuevo con unicamente lo que sigue abierto, y el
anterior queda como **historico**, con una cabecera que diga por que se cae. Nunca se borra: una
causa que desaparece en silencio vuelve a proponerse como nueva. Una conclusion equivocada se deja
escrita **junto a la buena** (la tabla de errores de `HISTORIA.md` es el ejemplo). **Un solo encargo vigente.**

## 7. Los documentos con aviso salen con el aviso, o no salen

Un documento entra en un envio **entero y con su cabecera de estado**. Extraer una tabla de un
procedimiento marcado «sin medir» la convierte en una orden de campo. Si el orden importa (linea base
archivada antes de grabar; `#SC` solo tras aceptar el acta), el envio lo dice.

## 8. Procedimientos y runbooks de campo

`RUNBOOK.md` y los `PROCEDIMIENTO-*.md` de `06_Calibracion/<equipo>/` son manuales de campo:

- **Que hacer cuando el resultado no es el esperado.** Cada paso: que se hace, que se tiene que ver
  y **que significa cada forma de fallar**.
- **Una casilla** en cada comprobacion, y sitio para **firma y fecha**.
- **Pasos numerados**, para poder decir por telefono «vas por el 4.3».
- **Preguntas abiertas al final**, dirigidas a quien pueda contestarlas. Nunca como instrucciones.
- Un valor de ejemplo (serie, coeficiente, codigo) se marca como **del equipo del ejemplo**: en
  cuanto un ejemplo se lee como defecto, alguien lo teclea, y la calibracion es por equipo (L-23).
- **Antes de pedir que se envie un byte al equipo, su efecto tiene que estar escrito** (`CLAUDE.md`
  §4): nunca `e` a un equipo no identificado como V3.6; en un V3 de 2020, `#V#`, `e` y `6`
  **disparan una medida** (luz y pitido): el paso lo avisa antes. Orden de deteccion vigente en
  `05_Documentacion/SPEC-V3.6.md:464`.
- Identificar un equipo empieza por **micro, placa y pantalla** (`RUNBOOK.md`, fase 0), nunca por
  los textos de la pantalla: en una STONE son imagenes.

## 9. Registros, ZIP y huellas

En este repositorio los ZIP **de la app** son evidencia, no paquetes nuestros:

- Los ZIP de campana y de soporte que exporta el telefono **se versionan** en
  `06_Calibracion/<equipo>/campanas/`, como binario (`.gitattributes`: `*.zip binary`), con su
  linea en `HUELLAS.txt` (md5, SHA-256 y una linea de que contiene). **Nada se da por escrito en un
  equipo sin su registro** (`CLAUDE.md` §3).
- **ZIP de soporte** (con todo; es el que se importa en otro telefono y el que cita el acta por
  SHA-256) frente a **ZIP ligero** (incremental, no se importa):
  `03_App_Movil/RetroV36/README.md`, apartado Banco (RF-APP-49 a 53). Para calibrar o reconstruir
  una campana hace falta el de soporte.
- Las tramas van a `06_Calibracion/<equipo>/tramas/*.txt` **sin conversion de fin de linea**, y las
  colas `06_Calibracion/cola_banco_*.csv` igual (`.gitattributes`).
- `07 pruebas/` es material en bruto del telefono y **no se versiona**: lo que vale pasa a
  `06_Calibracion/<equipo>/` con su huella (`CLAUDE.md` §6 y §10).
- Un ZIP que ya viajo **no se reescribe ni se manipula** con el mismo nombre.

Si alguna vez hay que armar un paquete propio (fuente para otro equipo, anexo para la
interventoria): **con un script y `zipfile` de Python**, no con `Compress-Archive` (muere por el
`PSModulePath` heredado del IDE); **contenido de `HEAD`** (`git show HEAD:<ruta>` o `git archive`), no
del disco; **sin `build/`, `dist/`, `debug/`, `nbproject/private/`, `__pycache__`, `.gradle/`** y
contarlo despues (= 0); **extraido en limpio y compilado desde la extraccion**; nombre con fecha y
commit.

## 10. El .hex: la trazabilidad la pone el documento

Aqui quien recibe **solo graba** (MPLAB IPE por ICSP, `TDD-V3.6.md`, R-IPE), asi que se entrega
binario, sin excepcion con:

- el **md5 recalculado sobre el fichero que se envia** (regla del repositorio), no copiado; SHA-256
  ademas si el documento que lo acompana lo usa;
- el **commit** del que sale y el md5 de cada fuente (`hex/fuente.md5`);
- **compilador y banderas**: XC8 v2.10 por ruta absoluta
  (`01_Firmware/RetroVertical_V3.6.X/CAMBIOS-V3.6.md:14`) con los flags de
  `nbproject/Makefile-default.mk`;
- con que **pareja** se probo (firmware y version de la app).

Los `.hex` y `.md5` se guardan **byte a byte** (`.gitattributes:2-3`): con conversion de fin de
linea el md5 declarado deja de coincidir con lo versionado. Comprobar con `git ls-files --eol` o
recalculando.

Delante del equipo, la version solo se ve por `#V#` (y en un V3 de 2020 `#V#` dispara una medida):
el documento es la unica trazabilidad del binario.

## 11. APK de la app RTV

La receta de compilar **no** es la skill `compilar-apk` (es de otro proyecto y otra cadena,
`CLAUDE.md` §8): es `03_App_Movil/RetroV36/README.md`, apartado «Compilar» — JDK 11 (Temurin
`jdk-11.0.24+8`), Gradle 6.5, AGP 4.1.1, `--offline`, `local.properties` con `sdk.dir=C:/android-sdk`
(barras normales, no se versiona). La app vigente de desarrollo (app unica V3.6 / V4 original / V4.6)
vive en la rama `rtv-1.0`; que version esta en cada telefono lo dice `RETOMAR.md`.

- **Escribir las rutas Windows con barras normales.** En otro proyecto, `\a` de «`7 sw apk\android-sdk`»
  se convirtio en un caracter BEL y la receta quedo inservible; el JDK de aqui vive en esa misma
  carpeta (`D:/@Proyect/Baliza/7 sw apk/...`).
- **Nombre**: se copia como `03_App_Movil/RTV-V<versionName>.apk` y
  `RTV-V<versionName>-<versionCode>.apk` (RF-APP-41, `SPEC-V3.6.md:864`), con su md5 declarado y
  comprobada con `aapt dump badging` (paquete, `versionCode`, `versionName`). Los `.apk` no se
  versionan (`.gitignore`).
- **`versionCode` nunca baja.** Android no instala un `versionCode` menor encima de uno mayor, y con
  `android:allowBackup="false"` (`AndroidManifest.xml:19`) volver atras exige desinstalar, lo que
  borra la campana del telefono (`COMPARATIVA-3.6.17-vs-rtv-1.0.md:86-92`,
  `REVISION-Arquitectura-P16-V3.6.md`, A-07).
- **Dos APK distintas con el mismo `versionName`/`versionCode` es un defecto**: en el telefono no se
  distinguen. Un par ya usado por una compilacion anterior no se reutiliza. Un APK de otra rama con el
  mismo paquete y `versionCode` mayor **no se deja en la carpeta de entrega** (P16 A-07, Alta).
- **Una funcion no esta hecha hasta que esta en el APK** instalado en el telefono.
- **Un control que se ve no es un control que funciona**: cruzar el layout contra el codigo antes
  de anunciar la funcion.
- **Que el APK lleva dentro el fuente que dice** se comprueba abriendolo como zip, entrada por
  entrada y por CRC, y **buscando las cadenas del cambio**. Un build en verde no demuestra que el
  cambio este dentro. Cada cambio del fuente deja obsoleto el APK del disco.

## 12. Antes de mandar algo

- [ ] Si es un encargo de medida: dice en su primera linea que es una **peticion**.
- [ ] Cada cifra, codigo, md5 u hora se volvio a mirar **hoy** en su fuente (registro, no resumen).
- [ ] El documento dice si se probo en el equipo, en la primera pantalla.
- [ ] Si es un `.hex`: las cuatro puertas de §2, linea base archivada, md5 recalculado, commit,
      compilador y pareja.
- [ ] Si es una APK: arquitecto **y** QA por escrito, recuento separado, las dos copias con nombre
      versionado, md5, `aapt dump badging`, `versionCode` mayor que el ultimo instalado.
- [ ] Si es una calibracion: serie leida de EEPROM, MAC, vencimiento, dispensas en `DECISIONES`.
- [ ] Si pide enviar bytes al equipo: el efecto de cada uno esta escrito, y nada de `e` a ciegas.
- [ ] Si es un envio nuevo: documento nuevo con solo lo abierto; el anterior, a historico.

Para interpretar si algo esta de verdad verificado, la skill `verificar`. Para la pantalla STONE,
`verificar-pantalla-stone`. Para una placa, `leer-planos-pcb`.

## Diferencias con la comun

- Quitado: §0 de `.claude/particularidades/entregar.md` — aqui no existe; lo propio va dentro de esta skill (copia de proyecto, una sola version).
- Cambiado: `ESTADO.md` -> `RETOMAR.md` (estado), `ROADMAP.md` (orden) y acta/ZIP con `HUELLAS.txt` (evidencia) — este repo no tiene `ESTADO.md` (C1).
- Anadido: §2 puertas del repo (cuatro para grabar, linea base, arquitecto Y QA, recuento separado, L-23, `#SC`, dispensas) — `CLAUDE.md` §3, §5, §6, §7, §11.
- Quitado: §8 de la comun («Los .docx se generan») — este repo no genera `.docx` (`git ls-files` sin ninguno); si llega a hacerlo, traerla de la comun.
- Quitado: gerbers y paquete de fabricacion — aqui no se fabrica placa.
- Cambiado: §9 paquete .zip -> «Registros, ZIP y huellas»: los ZIP de la app SI se versionan como evidencia (`CLAUDE.md` §3), contra el «los .zip no se versionan» de la comun; el armado por script queda reducido para paquetes propios.
- Cambiado: SHA-256 -> md5 como hash declarado del `.hex`/APK (regla del repo), SHA-256 como anadido (C2 aplicada con el hash del repo).
- Anadido: §10 cadena XC8 2.10, `fuente.md5`, byte a byte (`.gitattributes`), IPE por ICSP.
- Cambiado: §11 APK — la receta si esta aqui (README de la app), no en `compilar-apk`; anadidos nombre RF-APP-41, `aapt`, `versionCode` que no baja, par version/codigo no reutilizable, barras normales por el BEL.
- Quitado: ejemplos de Baliza/Semaforos/Camion/Pasos donde habia uno de este repo que ensena lo mismo; sustituidos por casos de `HISTORIA.md`, errores y T-A20.
- Anadido: §8 bytes de efecto desconocido (`e`, `#V#`, `6`) e identificacion por micro/placa/pantalla — `CLAUDE.md` §4.
- Quitado: el parrafo de la comun sobre copiar el arbol a `assets/` (Cordova) — la app RTV es Android nativa.
