Orquestador: c5774c6

# CLAUDE.md — Reglas permanentes del repositorio V3.6

Retrorreflectómetro **Vertical**, línea **V3.6** (2026), de DPI Ingeniería & Consultoría. **Nada está
validado contra un equipo salvo lo que respalda un ZIP de tramas.** Las SPEC, las revisiones y las QA
salen de leer fuente y de simular; un documento que cuadra en papel no es un documento medido.

Aquí sólo hay **reglas**, y no se añade una sin quitar o fundir otra. Las cifras del día van en
`RETOMAR.md`; los errores cometidos, en `HISTORIA.md`; lo que pasó, en `git log`.

## 1. Qué es esta línea

- Equipos **V3 "SAT-LUX/V3 K42"**: PIC18F47K42, XC8 2.10, placa "SATLUX H-IoT", STONE de 2.ª
  generación, Bluetooth **UART1 a 9600 8N1** (`05_Documentacion/PROTOCOLO-V3.6.md:16`). No 115200:
  ésa es la V4.1/V5.
- La V3.6 pasa la calibración a **EEPROM**, ajustable **desde la app en modo administrador**. Todo lo
  demás se comporta como en 2020. **La STONE no se toca.**
- **Dos apps que no se mezclan:** la de empresa (DPI, por USB; muestras, calibrar, certificado) y la
  de usuario (va con el equipo y la usa el cliente).
- **Proyecto independiente** (`github.com/dieleoz/retrov3.6_2026`). Son otros proyectos, que se
  consultan y no se mezclan: V4.1/V5 (`D:\IT\P_RetroReflectometro_Vertical`), V4.6
  (`D:\IT\P_RetroVertical_V4.6`) y Ruta al Mar (`D:\IT\P_RetroVertical_RutaAlMar`).
  `D:\@Proyect\IT\P_RetroReflectometro_Vertical` es una copia desfasada.

## 2. La regla que manda

**Toda afirmación sobre el código va con `archivo:línea` verificado abriendo el fichero.** Un md5, un
commit o una versión se abren antes de citarlos. Un negativo se verifica dos veces con herramientas
distintas. Dos fuentes que se contradicen no se eligen por escrito: van a "Contradicciones abiertas"
del `ROADMAP.md` y se cierran midiendo; al cerrar, la errónea queda junto a la buena en `HISTORIA.md`.

## 3. Grabar firmware: cuatro puertas

Nada se graba sin: (1) `.hex` compilado contra la base 2020 (`01_Firmware/base_2020_d089f962/`), atado
a un commit y con md5; (2) revisión del cambio; (3) prueba previa en otra placa si la hay; (4)
**autorización escrita del propietario** (P6). **Grabar es irreversible**: SLV-002 tenía CP activo y
su firmware original se perdió (`RUNBOOK.md:117`). Antes de grabar, la línea base se levanta por
Bluetooth y se archiva. Nada se da por escrito en un equipo sin su registro en el repositorio.

## 4. Ningún byte de efecto desconocido

**Nunca se envía `e` a un equipo no identificado como V3.6** (`05_Documentacion/SPEC-V3.6.md:459-465`).
Detección: `#V#` → `9` → `6` → sonda `@LEERV,BLA,1@` (3.6.x; `,2@` en `rtv-1.0`, `Deteccion.java:12`).
En un V3 de 2020 `#V#`, `e` y `6` miden: se avisa antes. Se identifica por micro, placa y pantalla.

## 5. Calibración por equipo

L-23: campaña, ajuste, coeficientes, fecha y acta son de **cada equipo**, atados a serie y MAC
(`05_Documentacion/SPEC-Calibracion-V3.6.md:237`). La serie se lee con `#GN#` (en la app de usuario,
si falta, la teclea el operador: SERIE-USR). `#SC` sólo tras aceptar
el acta; vence al año. El contrato `05_Documentacion/PROTOCOLO-V3.6.md` manda sobre firmware y app
(`:7-8`). Una dispensa que no esté en `06_Calibracion/<equipo>/DECISIONES-Diego-*.md` no existe.

## 6. Entregar

- Proceso por puertas P1-P12 (`ROADMAP.md`); ninguna se cierra sin su evidencia.
- **A Diego sólo va una APK con visto bueno escrito de arquitecto y de QA.** Se copia como
  `03_App_Movil/RTV-V<versionName>.apk` y `-<versionCode>.apk`, con md5 y `aapt dump badging`. Dos
  binarios distintos con la misma versión son un defecto. El md5 del contenedor APK no se reproduce entre compilaciones
  (el empaquetador reordena el ZIP): la equivalencia se comprueba por contenido (dex, recursos, firma).
- **Antes de una instrucción de campo, se mira qué app lleva ese teléfono** en la cabecera `# app:` de
  su último registro, no en la memoria. Android no instala un `versionCode` menor, la app no admite
  copia y desinstalar borra la campaña: **si una instalación falla, no se desinstala**.
- Un ZIP o registro del que sale una cifra o una conclusión **se archiva el mismo día** en
  `06_Calibracion/<equipo>/` con su huella. En `07 pruebas/` o en Descargas no cuenta.

## 7. Pruebas

Una prueba que no asevera no cuenta, y una que asevera **tampoco demuestra nada si el valor esperado
salió del código que prueba**: cuenta si viene de fuera (firmware, contrato, decisión escrita,
certificado). Al entregar, el recuento va separado (requisito / comportamiento) y cada prueba nueva
se entrega **vista en rojo**. Los casos que lo enseñaron están en `HISTORIA.md`.

## 8. Compilar y ejecutar

- App: la de desarrollo en la rama **`rtv-1.0`**. JDK 11, Gradle 6.5, AGP 4.1.1; receta en
  `03_App_Movil/RetroV36/README.md` y en `.claude/particularidades/entregar.md` §7 (no en `compilar-apk`, del V5).
- `testDebugUnitTest` no arranca por la `ñ` de `C:\Users\Diego.Zuñiga`: se compila y se ejecuta con
  **JUnitCore a mano**. La lista de clases es manual: la que no se añade no se ejecuta.
- Firmware: XC8 2.10 y MPLAB SIM (`05_Documentacion/TDD-V3.6.md`).

## 9. Subagentes

El principal orquesta; los subagentes ejecutan con encargos acotados y datos para confirmar **o
desmentir**. Arquitecto y QA, adversarios y con alcances disjuntos. Todo hallazgo Alto se reabre en el
código. **Dos subagentes nunca escriben en el mismo árbol a la vez** (en paralelo, cada uno en su
worktree y con rama propia). El principal también se equivoca: gana el código, no el contexto.
Modelos: opus sólo para `arquitecto-iot`; el resto sonnet o haiku; Fable, sólo si Diego lo autoriza.

## 10. Documentos

| Documento | Para qué | Límite |
| :--- | :--- | :--- |
| `README.md` | Qué es y dónde está cada cosa | 100 líneas; sin estado del día |
| `ROADMAP.md` | Qué falta, en qué orden, sesión en curso y siguiente; puertas, decisiones, contradicciones | 100 líneas; lo hecho se borra |
| `RETOMAR.md` | Estado vigente y prompt para retomar. Manda sobre las cifras | Se reescribe, no se acumula |
| `CLAUDE.md` | Estas reglas; la primera línea, el commit del Orquestador | 200 líneas |
| `HISTORIA.md` | Cómo se llegó aquí y **qué se concluyó mal**, con la errónea junto a la buena | — |
| `ARQUITECTURA.map` | Mapa del sistema (§M1-§M4, §M6) e índice de referencias cruzadas (§M5) | 1000 líneas |
| `.claude/particularidades/` | Lo propio de cada skill `orquestador:*`, con su mismo nombre | Sin cifras vigentes |

Ninguno es una bitácora. Si `ROADMAP.md` y `RETOMAR.md` discrepan, gana el registro (`git log`, md5,
trama). Español técnico y sobrio, sin emojis; si algo está sin medir, se dice en la primera línea.
`build/`, `dist/`, `debug/` no sirven para deducir qué hace el equipo: se abre el `.c`.
`.apk` no se versiona; `07 pruebas/` es capa cruda y no se edita; `.hex`, `.md5`, colas y tramas, byte a byte
(`.gitattributes`). Lo aprendido pasa a la V4.6 en `APRENDIDO-DE-V3.6.md`.
