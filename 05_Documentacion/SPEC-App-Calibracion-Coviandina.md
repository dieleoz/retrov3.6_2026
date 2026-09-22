# SPEC — "RTV Calibra": cargar el ZIP y calibrar. App corta, aparte, para las cuatro familias

**Sin medir. No se ha probado en un teléfono ni contra un equipo.** Revisión r2 (§8): cambios pedidos
por Diego y por las revisiones de `Cov_3.6.1_calibrar`; §8 manda sobre lo anterior donde choquen.

**Estado r1:** Sale de leer
`03_App_Movil/RetroV36/` en la rama `rtv-1.0` (base `d7d7b4d`, RTV 1.0.0-rc5) y de ejecutar la suite
JVM contra `EquipoSimulado`. El APK corto **compila y nada más**: no se ha instalado ni arrancado en
ningún teléfono, ni se ha visto conviviendo con la app de campo.

**Origen.** Diego, 19-sep-2026, noche: *"genera una versión de apk que sea cargar el .zip y darle
calibrar, poco más"*, y después *"dame un apk que funcione para todos, que sea cargar el .zip
reducido, darle calibrar, nombre, nota, serial, acta y ya"*.

**Documento hermano, que manda sobre lo suyo:** `SPEC-App-Unica-Familias-y-ZIP.md` (qué puede cada
familia y qué ZIP le corresponde). Los requisitos `RF-APP-U28` a `RF-APP-U38` son de allí; aquí sólo
se dice **dónde caen dentro del camino corto**.

---

## 1. Por qué existe

Para escribir **un solo código** en un equipo **ya medido** hicieron falta más de una hora y cuatro
pantallas. No falló el cálculo: **la app avisó de un desajuste y se quedó quieta**, y el operador tuvo
que adivinar que la salida estaba en otra pantalla.

La cadena, verificada línea a línea:

1. El ZIP venía de un banco `REPRESENTATIVO` y la campaña del teléfono nuevo estaba en `COMPLETO`.
2. `ImportadorCampana.java:119-135` (rc5): cuando los tipos no casan se traen las series y **no los
   `PASO`**, con el aviso *"lo ya medido cuenta al abrir el Banco"*.
3. Ese aviso **no era cierto del todo**: `BancoPrevio.aplicar` sólo reconstruye pasos `PATRON`
   (`BancoPrevio.java:139`). Los `OSCURO` y los `A5` **no los reconstruye nadie** — los únicos puntos
   que escriben pasos son `BancoActivity.java:608`, `BancoPrevio.java:158`,
   `ImportadorCampana.java:203`, `RehacerBanco.java:78` y `Campana.java:786`.
4. Sin pasos, `BancoCola.calibrable` (`:315`) devuelve `false` y `FlujoCalibracion.plan:584` corta con
   *"no calibrable: faltan patrones de AJUSTE o RE-MEDIDA"*. Detrás esperaba una segunda puerta:
   `Anclas.oscuro` sin `OSCURO` de la sesión (`Anclas.java:80`) y `Anclas.sRep` sin `A5`
   (`:102-116`), que además tumba la previa de `FlujoCalibracion.java:406` y bloquea **todos** los
   códigos a la vez.

**La app corta borra el caso por construcción: no hay campaña previa.** Su entrada es el ZIP y la
campaña nace vacía, así que `destinoConCola` es `false` (`ImportadorCampana.java:120`) y la cola del
ZIP se adopta sola (`:129-131`). El desajuste de bancos no puede darse aquí.

> El arreglo del caso general —que la campaña **de campo** adopte el banco del ZIP en vez de avisar—
> lo está haciendo otro agente en `ImportadorCampana` (D-1). **No es de este documento.**

---

## 2. El flujo, seis pasos y ni uno más

```
cargar el ZIP  →  calibrar  →  nombre  →  serie  →  acta  →  fin
```

Antes del paso 1: conectar el equipo. Al conectar, **las pruebas del equipo arrancan solas**
(`ConexionActivity.java:123-125`), porque sin ellas no hay `#GN#`, ni firmware detectado, ni previas
— y sin previas no se escribe. Eso no se toca.

**RF-COV-01 — Una sola pantalla.** La app abre en la lista de equipos emparejados. No hay menú, ni
"Avanzado", ni banco, ni medida de patrones, ni modo administrador, ni cierre de campaña.

**RF-COV-02 — Paso 1: "Cargar el ZIP".** Un botón, el selector del sistema (`ACTION_OPEN_DOCUMENT`,
sin permiso de almacenamiento). La app abre o crea la campaña de la serie y la MAC del equipo
conectado, importa el diario del ZIP adoptando su banco, aplica `BancoPrevio` y **deja el equipo
listo para calibrar**. Sin paso intermedio y sin que el operador sepa qué es una cola.

**RF-COV-03 — Paso 1 bis: lo que la app dice después de cargar.** En texto plano, bajo el botón:
cuántas series han entrado, de qué banco, **qué códigos quedan listos** y, uno por uno, **por qué no
lo están los demás**, con el motivo tomado literal de `BancoCola.calibrable` y de `Anclas`. Es
exactamente lo que esa noche no se veía en ninguna pantalla. Un código que la cola sólo verifica (7,
a, c, d en la completa) no dice "faltan patrones": dice **"esta cola sólo lo verifica"**.

**RF-COV-04 — Paso 2: "Calibrar".** *(Modificado en r2: RF-COV-11 a 13 y 17.)*
Abre `CalibrarActivity` con sus ramas `BuildConfig.CORTO`: sin tarjetas ni casillas por código, sin nota
de conformidad; el nombre de quien calibra se pide una vez y firma el acta (FIRMA-ACTA). Un solo botón
"Calibrar" hace el ciclo completo (PIN una vez por conexión, escritura, re-medida, persistencia, acta) en el
orden de la tabla, sin preguntar código a código (RF-COV-17). "Persistencia" y "Aceptar" quedan ocultos.

**RF-COV-05 — Paso 5: la serie, una vez y en un solo sitio.** Un botón que muestra la serie vigente y
permite teclearla cuando el equipo responde `#GN,NONE#`, cuando le cambiaron el módulo y se anuncia
`HC-06`, o cuando el nombre no la trae. Vía única: `Sesion.aceptarSerieTecleada`, el mismo diálogo de
`BancoActivity.java:471-491`. **No graba nada en el equipo**: el alta por `#SN` sigue en el modo
administrador de la app de campo. Se resuelve al cargar el ZIP y no se vuelve a preguntar.

**RF-COV-06 — La marca "serie declarada" llega al acta.** Si la serie la tecleó el operador y no la
leyó el equipo, **el acta lo dice en su primera línea**. El dato existe (`Sesion.marcaSerie()`,
`Sesion.java:278-280`) y hoy **no llega ni al ZIP ni al acta**: hoy su único consumidor es el correo
(`:299-300`). Es `RF-APP-U37` del documento hermano, y el camino corto es su sitio natural porque el
acta se arma aquí, al final. **Riesgo que cierra: R-U18**, un acta con serie declarada pasando por
acta con serie leída.

**RF-COV-07 — Al terminar: "Guardar / Compartir".** El ZIP ligero y el de soporte, con su copia en
`Download/RTV/`, en un solo selector (`CortoActivity.java:285-302`). El informe de calibración (texto;
el PDF está pendiente) se genera y se comparte solo al cerrar cualquier acción que deje actas aceptadas,
incluido el ciclo de un botón (`CalibrarActivity.java:544-609`, `ExportadorFinal.java`).

**RF-COV-08 — No hay nada más.** Lo que no está en esta lista, no está en esta app.

---

## 3. Qué pasa cuando algo no cuadra

| Situación | Qué hace la app |
| :--- | :--- |
| El ZIP es **de otra familia** que el equipo conectado | **No entra nada.** Un diálogo, con el motivo y qué hacer (`RF-APP-U32`) |
| El ZIP es **de otro equipo** (serie o MAC) | No entra nada, con las dos series y las dos MAC (`ImportadorCampana.java:93-96`) |
| El ZIP es el **ligero** (incremental) | Lo dice y pide el de soporte. Se detecta **por dentro**, no por el nombre (`ImportadorCampana.java:54-64`) |
| El ZIP no trae diario ni `campana.csv` | Lo dice y pide el de soporte |
| Patrón que no está en el catálogo | Lo dice y no entra nada (`ImportadorCampana.java:100-107`) |
| Falta el `OSCURO` o la `A5` | Se dice **al cargar**, código a código, con el motivo de `Anclas` |
| Equipo sin conectar, o sin serie | "Cargar" y "Calibrar" apagados, diciendo qué falta |
| Pruebas sin pasar, o NO APTO | Previa en rojo. No se calibra (`FlujoCalibracion.java:379`) |
| Familia que hoy no se calibra | Una línea que dice **qué le falta a ese equipo**, no sólo que no se puede |

**RF-COV-09 — El rechazo por familia detiene el paso 1, y está bien que lo detenga.** El documento
hermano lo deja como decisión de flujo, no suya (`SPEC-App-Unica-Familias-y-ZIP.md`, §5). **Aquí se
decide: detiene.** Mezclar `x` de escalas distintas produce coeficientes que *parecen* buenos, y eso
es peor que un diálogo. La diferencia con la noche del 19-sep no es que haya fricción: es que la
fricción **dice qué hacer y se resuelve en el sitio**, en vez de avisar y quedarse quieta. El texto
lleva siempre la salida: *"abra una campaña aparte para la etapa anterior"*.

**RF-COV-10 — Ningún botón apagado sin decir qué hacer.** Cuatro familias, y tres de ellas hoy no se
calibran. Eso **no es un fallo de esta app**, es el estado del proyecto, y se dice así:

| Familia | Qué se puede | Qué se le dice al operador |
| :--- | :--- | :--- |
| **V3.6** | Todo: calibrar y escribir | — |
| **V4.6** | Medir y banco; calibrar **no** | La calibración está candada a propósito hasta que exista una V4.6 grabada |
| **V4 original** | Ni banco ni calibrar | Necesita que le graben el firmware **V4.6**: sin `#X` no hay `x` |
| **V3 2020** | Ni banco ni calibrar | Necesita que le graben el firmware **V3.6**: no tiene órdenes `#…#` |

La frase sale de `Protocolo.queHacerParaCalibrar()`, **que es del otro agente y no se duplica aquí**
(ver §6).

---

## 4. Lo que no se toca

- **El cálculo, los criterios y las dispensas son los mismos ficheros:** `Asistente`, `Ajuste`,
  `Anclas`, `TablaCalibracion`, `Remedida3611`, sin una línea de diferencia. **Si un número sale
  distinto del de la app de campo con el mismo ZIP, es un fallo de esta app, no una variante.**
- **Escribir sigue exigiendo el equipo conectado**, con previas, PIN, relectura, persistencia y acta.
  Esto simplifica la navegación, no las garantías.
- **La lista blanca de tramas por perfil no se afloja** (`Protocolo.permitida`): es lo que impide
  colgar un equipo.
- Las pruebas heredadas, en verde, sin excepción.

---

## 5. Cómo conviven las dos apps: `applicationId` propio

**Decisión: aplicación aparte.** Hasta la rc5 son la misma aplicación, así que **no caben las dos en
un teléfono**: instalar una borra la otra, y volver atrás exige desinstalar, que se lleva la campaña
por delante (`Campanas` guarda en `ctx.getFilesDir()`, `Campanas.java:66`). Esa noche eso obligó a
repartir el trabajo entre dos móviles.

Comprobado **abriendo el código**, no razonando:

- `applicationId "com.dpi.retrov36"` (`app/build.gradle:11`); se le añade el sufijo `.calibra`.
- El `FileProvider` declara `android:authorities="${applicationId}.ficheros"`
  (`AndroidManifest.xml:52`): **la autoridad se separa sola**. Es lo que hace posible la convivencia —
  dos apps con la misma autoridad no pueden instalarse a la vez.
- **Ningún fuente Java escribe el nombre del paquete como literal** fuera de su línea `package`
  (comprobado sobre los 67 ficheros de `main`). Nada da por supuesto un paquete concreto.
- Almacenamiento: `ctx.getFilesDir()` (`Campanas.java:66,148,210`). **Cada app tiene el suyo**, y el
  de la corta nace vacío — que es justo lo que se quiere, porque su entrada es el ZIP.
- El ZIP se lee por `ACTION_OPEN_DOCUMENT` + `openInputStream` (`CampanaActivity.java:715,749`):
  permiso puntual del sistema, **sin permiso de almacenamiento**.
- Firma: las dos salen del almacén de depuración de Gradle. Son **paquetes distintos**: no hay
  conflicto de firma y ninguna instalación toca a la otra.
- Emparejamiento Bluetooth: es del sistema. El equipo **sigue emparejado**; la app nueva sí vuelve a
  pedir el permiso de ubicación, porque es otro paquete.

**Verificado en los dos binarios** (`aapt dump badging`):

| APK | Paquete | Etiqueta | versionName |
| :--- | :--- | :--- | :--- |
| `app-debug.apk` | `com.dpi.retrov36` | RTV | `1.0.0-rc6` |
| `app-coviandina.apk` | `com.dpi.retrov36.calibra` | RTV Calibra | `Cov_<versión>_calibrar` (RF-COV-16) |

**Lo que hay que vigilar, y queda dicho:** las dos escriben en el **mismo** `Download/RTV/`
(`Campanas.java:245`). Los nombres llevan serie y fecha, así que no se pisan, pero la carpeta mezcla
lo de las dos.

### La pregunta que esta SPEC no cierra por su cuenta

**¿La corta sustituye a la larga para el operador de campo?** **No, y no debería.** La corta calibra a
partir de un ZIP **ya medido**; para producir ese ZIP hacen falta el banco, las pruebas y la línea
base, que sólo están en la larga. Lo que se simplifica es **calibrar**, no **medir**. Se instalan
juntas y cada una hace lo suyo. La decisión es de Diego; aquí queda el argumento.

---

## 6. Dos correcciones, una a otro documento y otra a mí mismo

**A mí mismo.** Escribí `AppCorta.queLeFalta(Firmware)` para decir qué le falta a cada familia. Otro
agente ha añadido, en paralelo, `Protocolo.queHacerParaCalibrar()` (`Protocolo.java:98`,
`ProtocoloV4Original.java:156`). **Su sitio es mejor que el mío**: la frase pertenece al protocolo,
que es quien sabe qué puede cada firmware, y así la usan las dos apps. **Mi función se retira y se
llama a la suya.** Lo dejo escrito para que no vuelva a aparecer duplicada.

**Al documento hermano.** `SPEC-App-Unica-Familias-y-ZIP.md` dice que el ZIP de soporte se distingue
del ligero *"únicamente por el prefijo del nombre del fichero: nada dentro dice qué es"*. **No es
exacto.** `ImportadorCampana.esIncremental` (`:54-64`) lo decide **mirando dentro**: el ligero trae
`indice.sha256` o una entrada con `.desde_`. La app corta lo usa y rechaza el ligero por contenido,
no por nombre. Lo que **sí** es cierto es lo de fondo —y sigue abierto—: **nada dentro del ZIP declara
su familia**, que es `RF-APP-U29`, y ahí el documento hermano tiene toda la razón.

---

## 8. Revisión r2: lo que cambia

Fuentes: decisiones APPS-DPI, VERIF-5-10, CERT-TITULO y FECHA-EQUIPO
(`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`); revisión `arquitecto-iot` de
`Cov_3.6.1_calibrar` (NO APTO: H-1, H-2, H-3, M-1, B-2); revisión de la cadena app-firmware en MPLAB SIM
(APTO CON CONDICIONES, C-1). Las revisiones están en el scratchpad de la sesión, sin archivar.

**RF-COV-11 — Ninguna salida al Banco.** Desde esta app no se llega al Banco ni a "Tomar muestras" por
ningún camino (cierra H-1, `CalibrarActivity.java:441-446`). Criterio: ninguna pantalla abre
`BancoActivity`.

**RF-COV-17 — Un solo botón, sin códigos a la vista.** Diego: "es cargar un .zip y calibrar, ni idea
el funcional qué es un 8". El operador **no elige códigos, ni los ve por su número**: pulsa "Calibrar",
teclea su nombre una vez y la app calibra sola **todo lo que el ZIP permite, en el orden que exige la
tabla** (el 8 antes que la b, `TablaCalibracion.java:196`), dando por aceptada cada acta cuya re-medida
sea conforme (RF-COV-12) para poder seguir con la siguiente. Si alguna no es conforme, restaura ese
código, sigue con los demás que no dependan de él y lo dice al final. En pantalla, colores por su nombre
("amarillo, lámina tipo I"), nunca el código. Al terminar, un resumen: qué quedó calibrado, qué no y por
qué, y "Guardar". Sustituye a la casilla por código y a la aceptación código a código de RF-COV-04.

**RF-COV-12 — Re-medida tras escribir: error frente al certificado, para todos los códigos.**
Sustituye al criterio de reproducción por s_rep (`Remedida3611.java:65-66`, H-2) y a REMEDIDA-b
(RF-CAL-18) en esta app: la b se juzga igual que el 8. **Conforme si el valor medido cae dentro de ±10 %
del certificado**; el acta y el certificado dan, por patrón, el valor certificado, el rango a ±5 % y a
±10 %, lo medido y si queda dentro de ±5 % (VERIF-5-10, confirmado por Diego). Se mantienen los dos
intentos y restaurar el estado anterior si no es conforme.

**RF-COV-13 — La fecha, escrita y releída.** `#SC` graba la fecha **del día en que se calibra**
(FECHA-EQUIPO); vence un año después. Tras `#SC` la app relee `#GC#` y, si no coincide, el acta lo dice y
no queda aceptada (cierra H-3). El equipo guarda una sola fecha; el histórico son los ZIP de cada
calibración. Cierra C-1 de la revisión app-firmware.

**RF-COV-14 — "Listos para calibrar" dice la verdad.** Tras cargar el ZIP sólo aparecen los códigos que
esta app puede escribir de verdad, filtrados con `TablaCalibracion` (cierra M-1): 1 y 2 no se reescriben;
los bloqueados, con su motivo; la b, "después del 8".

**RF-COV-15 — Funciona sola.** No supone la app de campo instalada ni remite a ella en sus textos (B-2).
Todo lo que necesita (catálogo, colas, decisiones) va en sus assets.

**RF-COV-16 — Nombre de versión.** `Cov_<versión>_calibrar` (petición de Diego); `versionCode` propio,
distinto del de cualquier APK anterior del mismo paquete.

**RF-COV-18 — La fecha anterior se lee antes de grabar la nueva.** Antes de `#SC` la app lee `#GC#`. Si no
responde (plazo vencido) o responde algo que no es una fecha ni `NONE`, **no se envía `#SC`**: el acta dice
"no se pudo leer la fecha del equipo; reintente" y el código queda como "escrito sin aceptar". La fecha
anterior leída (una fecha o `NONE`) se anota en el acta antes de enviar `#SC`.

**RF-COV-19 — Rechazar devuelve sólo la fecha que se leyó.** Si el acta emitió `#SC`, al rechazar se envía
`#SC,<fecha anterior leída>#` (o `#SC,NONE#` sólo si lo leído fue `NONE`) y se relee `#GC#`. Si no hay fecha
anterior leída, no se envía nada y el acta lo dice. Nunca se borra una fecha que no se leyó.

**RF-COV-20 — El acta dice con qué regla se juzgó cada código.** En esta app, todos con RF-COV-12
(VERIF-5-10); ninguna línea del acta cita RF-CAL-18 ni s_rep para la b.

**RF-COV-21 — Cierre automático y mensajes coherentes.** Si tras la secuencia un acta no tiene ningún código
conforme, se rechaza sola y el resumen no pide pulsar Rechazar. El motivo de cada restauración es el real
("colocaciones no válidas" o "re-medida fuera de ±10 %"). El contador de colocaciones no válidas es de
**seguidas**: una válida lo pone a cero. Al terminar, `#Q#` cierra el modo administrador (PROTOCOLO:44).
Ningún texto que vea el operador lleva el número de código (RF-COV-17). El acta guardada y el ZIP de
soporte son documentos técnicos: llevan código y tramas (P11-M1) y **en esta app no se muestran en
pantalla** (el acta queda oculta). Todo texto que llega a la pantalla pasa por **un único filtro** en la
frontera de la app, que sustituye cualquier forma del número de código (`código k`, `del k`, `#S,k`, `#G,k`,
`#F,k#`) por el nombre del color; no se arregla texto a texto (bucle declarado en la 3.6.7, decisión
A4B-FILTRO de Diego).

**RF-COV-22 — Estructura.** La secuencia automática vive en `CalibracionAutomatica`; la lectura, escritura y
devolución de la fecha, en una clase propia. Ningún arreglo hace crecer `FlujoCalibracion.java` ni
`CalibrarActivity.java` (rules/modularidad.md); ningún método pasa de 100 líneas.

**RF-COV-23 — Salvavidas visibles.** En esta app quedan visibles "Leer batería (9)", "Rechazar",
"Cerrar sin restaurar" y "Liberar" (estos dos con PIN de administrador), para salir de una sesión a medias
(`CalibrarActivity.java:86,104-107`). Es la excepción escrita a RF-COV-08.

**Nota:** RF-COV-18, 19 y 21 (restauración y contador) corrigen código compartido con la app de campo, que
hereda el cambio y necesita su propia revisión antes de entregarse.

**Lo que sigue pendiente, fuera de esta revisión:** la verificación final de 10 patrones sobre todos los
códigos y el "Certificado de calibración" firmado por ITVIAL SAS (VERIF-5-10, CERT-TITULO); la toma de
muestras (TOMA-50). Van en la app de empresa completa (`ROADMAP.md`, tareas 1 a 4 y 9).

## 7. Lo que esta SPEC NO ha comprobado

- Nada se ha probado **en un teléfono** ni **contra un equipo**.
- Las dos apps **no se han visto instaladas a la vez**: que convivan sale de leer el manifiesto y el
  `build.gradle` y de mirar los dos APK con `aapt`, no de haberlas instalado.
- `RF-COV-06` (la marca de serie declarada en el acta) y `RF-COV-09` (el rechazo por familia) están
  implementados y probados en la JVM (`AppCortaTest`); como el resto, sin probar en un teléfono.
- El camino corto se ha ejercitado **sólo en la JVM**, con el ZIP archivado de las 15:10.
