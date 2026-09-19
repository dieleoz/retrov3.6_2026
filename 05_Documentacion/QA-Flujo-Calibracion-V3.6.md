# QA — Flujo de calibración de la app RTV 3.6.9 desde el puesto del operador

**Nada de lo que propone este documento está implementado ni probado.** Sale de leer el fuente de la app
3.6.9 (commit `8dfeb6e`, `versionName "3.6.9"`, `app/build.gradle:14-15`), los registros de tramas del
19-sep-2026 (`07 pruebas/19092026_1210/x/`) y las condiciones de `REVISION-Arquitectura-P9-V3.6.md`.
Los tiempos de campo son estimaciones hechas con los tiempos de disparo medidos ese día, no medidas de
un flujo que no existe. **El veredicto es que la 3.6.9 no está lista para un operador sin soporte**
(§6).

- **Enfoque:** ISTQB. Pruebas de usabilidad y de aceptación, basadas en riesgo. Severidad = impacto
  sobre el equipo del cliente y sobre la validez del acta. Prioridad = urgencia de corregirlo antes de la
  próxima calibración.
- **Escalas.** Severidad: *Crítica* (puede dejar el equipo del cliente mal calibrado sin que nadie lo
  sepa, o saltarse una protección), *Alta* (bloquea al operador o deja un dato de identidad o de acta
  erróneo), *Media* (cuesta tiempo o induce a error, sin dañar nada) y *Baja*. Prioridad: *P1* antes
  de volver a calibrar, *P2* antes de entregar a un operador de campo, *P3* cuando se pueda.
- **Rutas.** Las de la app son relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.
  Los registros, relativos a `07 pruebas/19092026_1210/x/`. En `tramas/rtv36_20260919_114644.txt`
  (en adelante **T4**) los tiempos son milisegundos desde las 11:46:44. La hora de reloj se ha
  calculado sumando y se ha comprobado con el acta que el propio registro abre a las 12:04:31 (T4:1480).
- **Requisito del usuario añadido durante la revisión** (Diego, 19-sep-2026): la serie se lee del
  equipo (`#GN#`) y no se teclea; el modo administrador se reduce a "tengo el ZIP" + "Calibrar"; y la
  campaña pide sólo los patrones mínimos por código. Se recoge en §2.2, §2.3 y §3.

---

## 0. Incidencia abierta: estado en que quedó SLV-002

Lo primero no es de usabilidad. Según el último registro, que termina a las 12:10:05 (T4:2058), el
equipo quedó así:

| Qué | Estado | Evidencia |
| :--- | :--- | :--- |
| Código 1 | **Escrito y sin verificar.** `#S,1,…,2.98471545E-01,-1.62263869E+02` → `#OK#` a las 12:04:34, relectura y `#E` conformes. **No consta ninguna re-medida** | T4:1660-1698. No hay ninguna línea "re-medida" en T4 |
| Acta | **PENDIENTE, sólo en memoria.** No se guardó ni va en el ZIP de las 12:10 | La app sólo guarda el acta al aceptarla o rechazarla (`AdminActivity.java:822-823`, `:840-841`). No hay `actas/` en el ZIP |
| Conformidad del superadministrador sobre el código 1 | Sólo en memoria. El registro de escritura no la incluye | `AdminActivity.java:593-594` la pasa al acta; T4:1698 no la lleva |
| Serie en EEPROM | **`SLV-02`** (se grabó `SLV-002` a las 11:48:52 y después `SLV-02` a las 12:09:02) | T4:593-602 y T4:2048-2057; `resumen.txt:4` |
| Fecha de calibración | `NONE` | `resumen.txt:4` |

**Qué hacer**, sin tocar el firmware: (1) con la app, re-medir el código 1 si el acta sigue viva en el
teléfono. Si el teléfono se ha reconectado o la app se ha cerrado, el acta se ha perdido
(`Sesion.java:130`, §1.3 D-02). En ese caso, **restaurar el código 1 con `#F,1#`** y repetir la
escritura en un flujo con acta. (2) Volver a grabar la serie `SLV-002`. (3) Anotar las dos cosas en
`06_Calibracion/SLV-002/`. Hasta entonces, SLV-002 mide el blanco intenso con una curva que nadie ha
verificado midiendo.

---

## 1. Recorrido real frente a esperado

### 1.1 Lo esperado (palabras de Diego)

> "Seguir un paso a paso para leer todos los patrones y, luego, con ese ZIP, darle a Calibrar."

Dos fases, sin decisiones técnicas del operador: **medir** (guiado) y **calibrar** (un botón).

### 1.2 Lo que hay que hacer hoy para calibrar los códigos 1, 8 y 2

Contado en el código de la 3.6.9. Se cuenta un toque por botón, casilla u opción de lista. Un
desplegable son dos toques (abrir y elegir). Teclear un valor se cuenta como una **entrada de texto**
aparte.

| # | Pantalla | Qué hace el operador | Toques | Texto | Decisión | Código |
| ---: | :--- | :--- | ---: | ---: | :--- | :--- |
| 1 | Conexión | Tocar el equipo en la lista | 1 | | | `ConexionActivity.java:153-158` |
| 2 | Pruebas | Se abren solas y tardan unos 47 s. Volver | 1 | | | `ConexionActivity.java:92-94`; T3 (`rtv36_20260919_113225.txt`):582 |
| 3 | Conexión | "3. Campaña de calibración" | 1 | | | `ConexionActivity.java:47-48` |
| 4 | Campaña | Bajar hasta la sección "Enviar" y tocar "Importar", que está en una fila doble junto a "Nueva campaña" | 1 | | ¿Dónde está? | `CampanaActivity.java:99-107` |
| 5 | Selector de ficheros | Elegir el ZIP | ≥ 2 | | ¿Cuál? | `CampanaActivity.java:683-696` |
| 6 | Campaña | "Preajuste A5", y por cada uno de los 3 patrones "OK, medir" más 4 diálogos "Colocación k de 5". Después, "Salir del preajuste A5" | 17 | | | `CampanaActivity.java:96`, `:225-235`, `:505-512` |
| 7 | Campaña | "Preajuste OSCURO", "OK, medir" y 4 diálogos de colocación. Sale solo | 6 | | | `CampanaActivity.java:97`, `:236-252` |
| 8 | Conexión | Volver y abrir "Modo administrador" | 2 | | | `ConexionActivity.java:50` |
| 9 | Administrador | "Entrar", PIN y "Entrar" | 2 | 1 | | `AdminActivity.java:294-341` |
| 10 | Administrador | "Leer coeficientes". Es obligatorio antes de ajustar, y nada lo dice hasta que falla | 1 | | | `AdminActivity.java:85`, `:502-505` |
| 11 | Administrador | Poner a mano la x de oscuro (por defecto 575, medido 565,4) y la s_rep (por defecto 3 %, medida 2,24 %) | 2 | 2 | Qué valores | `AdminActivity.java:127-136`; `Asistente.java:218`, `:234` |
| 12 | Administrador | Por código: desplegable, grado (3 opciones), "Ajustar", "Escribir…", confirmar y, si incumple, casilla, nota y "Escribir". **×3** | 3 × 9 = 27 | 3 | Grado, código y conformidad, ×3 | `AdminActivity.java:95-120`, `:517-597` |
| 13 | Administrador | Por código: desplegable de re-medida (elegir patrón), botón, "OK", K − 1 colocaciones y "OK" del resultado. **×3** | 3 × 5 = 15 con K = 1; 3 × 9 = 27 con K = 5 | | Qué patrón, ×3 | `AdminActivity.java:137-139`, `:736-811` |
| 14 | Administrador | "Aceptar acta" y confirmar | 2 | | | `AdminActivity.java:140`, `:813-827` |
| 15 | Campaña | Volver, entrar en Campaña, "Exportar" y el selector de la app para compartir | ≥ 5 | | ¿A quién? | `CampanaActivity.java:100`, `:644-681` |
| | | **Total** | **≈ 85** con el protocolo 1 × 9 del acta de hoy; **≈ 97** con 5 × 4 | **6** | **≥ 13** | |

Además, **no está en la app**: apagar y encender el equipo al final y repetir `#V#`, `#G` y `#E` de lo
escrito, que P9-B8 exige para comprobar que la escritura persiste. Hoy se hace a mano.

**Pantallas:** 5 de la app (Conexión, Pruebas, Campaña, Administrador y los diálogos) más 2 del
sistema (selector de ficheros y selector para compartir). La de Administrador tiene **25 controles
activos a la vez** en un solo desplazamiento (`AdminActivity.java:84-170`): leer coeficientes, un
desplegable de código, 3 opciones de grado, Ajustar y Escribir, 4 campos de parámetros, el
desplegable y el botón de re-medida, Aceptar y Rechazar, el campo y el botón de serie, `#FT#`, el
desplegable de fábrica con Restaurar uno y Restaurar todos, los 2 campos y el botón de PIN, y Cerrar.
Diego contó unos 20; el código da 25. **Las acciones que calibran conviven en la misma pantalla con
las que borran (`#F,*#`) y con las que cambian la identidad del equipo (`#SN`).**

**Lo esperado**, con la propuesta de §2: 2 pantallas y unos 14 toques más uno por colocación, con una
sola decisión técnica (la conformidad) y una entrada de texto (la nota).

### 1.3 Defectos

| ID | Defecto | Severidad | Prioridad | Evidencia en código | Evidencia en el registro |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **D-01** | **La re-medida no se encuentra y bloquea sin decir dónde está.** El mensaje de bloqueo nombra la regla ("P9-B8"), pero no dice en qué botón se hace. La re-medida está en otra sección de la misma pantalla y exige elegir un patrón en un desplegable que sólo se rellena tras escribir | Crítica | P1 | `Acta.java:112-113`; `AdminActivity.java:137-139`, `:224-243` | Tres rechazos: 12:07:38 (código 8), 12:08:24 y 12:08:31 (código 2), en T4:1816, :2046 y :2047. **Ninguna re-medida** en todo T4. El equipo queda con el código 1 sin verificar (§0) |
| **D-02** | **El acta pendiente no se guarda, y reconectar la borra.** Con ella desaparece el bloqueo de P9-B8: tras reconectar, la app deja escribir otro código sin re-medir el anterior. También se pierde la conformidad del superadministrador | Crítica | P1 | `Sesion.java:130` (`acta = null` en `reiniciar()`, que se llama al conectar, `ConexionActivity.java:81`); `Acta.motivoNoEscribir()` sólo mira el acta en memoria (`Acta.java:106-117`); sólo se guarda al cerrarla (`AdminActivity.java:847-854`) | El ZIP de las 12:10 no lleva acta. Hubo 5 conexiones en 30 min (cabeceras de `tramas/`: 11:16, 11:32:00, 11:32:25, 11:46:21, 11:46:44) |
| **D-03** | **Un corte del enlace a mitad de `#S` no restaura nada.** La excepción sale de `escribir()` y `op()` la convierte en "Error: …", sin relectura, sin `#E` y sin restauración (P9-B12) | Crítica | P1 | `AdminActivity.java:260-268` y `:677` | No ocurrió hoy. Riesgo ya señalado en `REVISION-Arquitectura-P9-V3.6.md:438` |
| **D-04** | **La serie se sobrescribe con un solo toque y sin cotejo.** El diálogo sí dice "actual: SLV-002", pero en una frase, sin contraste y sin pedir que se teclee dos veces. No se compara con el nombre Bluetooth (`COVIANDINA_SLV-002`) ni con la serie de la campaña | Alta | P1 | `AdminActivity.java:887-909` (el texto "actual" está en `:895-896`); sólo se valida el formato (`Calibracion.java:65-80`) | `#SN,SLV-02#` a las 12:09:02 (T4:2048), 20 min después de `#SN,SLV-002#` (T4:593). `resumen.txt:2` y `:4` quedan contradictorios: "serie SLV-002" y "#GN#: SLV-02" |
| **D-05** | **La identidad del equipo sale del nombre Bluetooth, no de la EEPROM.** Campaña, acta y registros usan `Sesion.serie()`, que corta el nombre Bluetooth, o lo que se teclee si no trae serie. `#GN#` sólo se muestra. Contradice el requisito de Diego ("la serie se lee de `#GN#`") | Alta | P1 | `Sesion.java:148-164`; `CampanaActivity.java:131-149` (serie tecleada); `AdminActivity.java:729` (acta con `s.serie()`); `Pruebas.java:283-288` (`#GN#` sólo informa) | Acta con "Equipo: SLV-002" (T4:1478) mientras la EEPROM dice `SLV-02` |
| **D-06** | **La pantalla Administrador mezcla calibración, mantenimiento y operaciones destructivas.** Son 25 controles sin orden de uso | Alta | P1 | `AdminActivity.java:84-170` | "¿Qué le doy?" (Diego). El operador salió de la pantalla y volvió a entrar, lo que costó otro `#L`: `#Q#` a las 12:05:22 y `#L` a las 12:05:27 (T4:1728-1735; `AdminActivity.java:393-407`) |
| **D-07** | **"Importar" está escondido.** Es el tercer control de la sección "Enviar", al final, en fila con "Nueva campaña", que archiva la campaña: tocar el de al lado por error es caro | Media | P1 | `CampanaActivity.java:99-107` | 7 min 55 s entre "APTO" (11:33:12) y la importación (11:41:07): T3:582-583 |
| **D-08** | **Texto engañoso sobre el origen de los datos.** Dice que usa "la sesión de medida (pantalla Medida de patrones)". El código usa la campaña y, **si ésta no tiene series elegidas, cae sin avisar a las medidas sueltas de la sesión**, que es justo lo que prohíbe P9-B7 | Alta | P1 | Texto: `AdminActivity.java:92-94`. Código: `Sesion.java:241-250` | El informe muestra el origen ("Puntos: …", `AdminActivity.java:512`), pero después de ajustar |
| **D-09** | **La desinstalación borra la campaña sin aviso previo.** Los datos están en el almacenamiento privado y `allowBackup="false"` | Alta | P2 | `Campanas.java:64`; `AndroidManifest.xml:14` | A las 11:41, en la 3.6.9, se importaron **56 series y "0 ya estaban"** (T3:583): la campaña del teléfono estaba vacía. **Comprobado aquí:** `RTV-V3.6.8.apk` y `RTV-V3.6.9.apk` llevan el mismo certificado de depuración (SHA-256 `c990adf6…`, con `apksigner verify --print-certs`). Instalar encima no obligaba a desinstalar. La 3.6.6 no está en el árbol y no se ha podido comprobar |
| **D-10** | **Mismo nombre de APK para versiones distintas.** `RTV-V3.6.apk` se pisa en cada entrega. La versión sólo se ve dentro de una frase de la pantalla principal | Media | P2 | `03_App_Movil/RetroV36/README.md:24-25`; `ConexionActivity.java:43-44` | Hoy `RTV-V3.6.apk` y `RTV-V3.6.9.apk` tienen el mismo md5 (`3fbb68f3…`). Diego usó la 3.6.6 creyendo que era la 3.6.7 |
| **D-11** | **La x de oscuro y la s_rep se teclean** aunque la campaña ya las tiene. El acta anota el oscuro en x = 575 (el valor por defecto) y no en la x medida, 565,4 | Media | P2 | `AdminActivity.java:127-136`, `:698-699`; `Asistente.java:218`; la campaña tiene `xOscuro()` (`Campana.java:191-198`) | T4:1464: "Oscuro (x = 575)". `HUELLAS.txt`: OSCURO x = 565,4 y s_rep de la A5 del 2,24 % |
| **D-12** | **El operador elige grado y método en cada código**, cuando ya están decididos (P9-B6) | Media | P2 | `AdminActivity.java:102-117` | Tras escribir el código 1 en grado 1, se probaron grado 2 y la recta anclada **del mismo código ya escrito** (12:04:54, 12:05:49 y 12:05:53; T4:1699, :1737 y :1766). Nada indica que ese código ya está en el equipo |
| **D-13** | **La re-medida se hace con un solo patrón, lo elige el operador y no se compara con el certificado.** RF-CAL-18 pide "cada patrón del código" y, además, RF-CAL-14 frente al certificado | Media | P2 | `AdminActivity.java:743-748`; `Acta.java:141-162` (sólo compara con la curva, no con el certificado) | — |
| **D-14** | **El acta no lleva el SHA-256 del ZIP (P9-B2) ni el registro de la A5 y de G4 (P9-B1)** | Media | P2 | `Acta.java:205-235` | Acta de T4:1477-1486 |
| **D-15** | **Por defecto la campaña mide 3 × 3.** Con M = 3 el rechazo de descolgados no actúa (`N_MIN_DESCOLGADOS = 4`). La propuesta de SLV-002 recomienda 5 × 4 | Media | P2 | `CampanaActivity.java:73-77`; `PROPUESTA-Ajuste-SLV-002-2026-09-19.md` §6 | — |
| **D-16** | **La comprobación de persistencia del final de P9-B8 no está en la app**: apagar, encender, `#V#`, `#G` y `#E` | Media | P2 | No hay código que la haga. `Acta.motivoNoAceptable()` no la exige (`Acta.java:175-191`) | — |
| **D-17** | **La app no puede saber la carga de la batería.** La respuesta a `9` está rota en el firmware | Baja (no se puede corregir sin firmware) | P3 | `SPEC-V3.6.md:81`; `PROCEDIMIENTO-Calibracion-V3-K42.md:135` | `HUELLAS.txt`: la batería del equipo se cambió hacia las 11:40, a mitad de sesión |

**Dos afirmaciones del encargo que el código matiza:**

- "La app no mostró la serie anterior": **la mostraba**. El diálogo dice `actual: <serieEquipo>`
  (`AdminActivity.java:895-896`). En la segunda grabación era `SLV-002`, porque la primera lo había
  actualizado (`:902-904`). El defecto real es otro: la muestra dentro de una frase, sin contraste, sin
  doble entrada y sin cotejo (D-04).
- "La desinstalación se debió a las versiones": entre la 3.6.8 y la 3.6.9 **no**. La firma es la misma
  (D-09), así que instalar encima conservaba los datos.

---

## 2. Flujo propuesto: "Calibrar este equipo"

### 2.1 Principio

Hay dos modos. **Operación** es lo que ve el operador: medir la campaña y calibrar. **Avanzado** es
para el superadministrador y no se ve por defecto. El flujo de calibración **no ofrece decisiones
técnicas**: el método por código, el protocolo, los patrones y los umbrales son configuración
decidida y firmada (P9-B5, B6), que la app lee y enseña, no que el operador elija.

Método vigente por código (decisiones de Diego, `PROPUESTA-Ajuste-SLV-002-2026-09-19.md:531-541`):

| Código | Método | Condición |
| :---: | :--- | :--- |
| 1 blanco intenso | Grado 1 | Dispensa de RF-CAL-14/15/16 ya dada por Diego. Queda escrita en el acta |
| 8 amarillo tipo I | Grado 1 | C-CAL-15: certificado de los tipo I o aprobación expresa de Diego (`REVISION-Arquitectura-P9-V3.6.md:475-476`) |
| 2 amarillo intenso | Recta anclada en el oscuro de la campaña | Serie OSCURO elegida y aceptada |
| 3, 4, 5, 6, 7, a, b, c, d | Sólo verificar, con la curva de fábrica | `b`: dejar fábrica y marcarlo "fuera" (propuesta §5) |

Esta tabla va en un fichero de configuración versionado, dentro del APK de calibración. **Que cambie
exige compilar**: el operador no la toca.

### 2.2 Fase A — Medir

Pantalla **Campaña**, con este orden de arriba abajo:

1. **Cabecera de identidad**, leída del equipo: serie `#GN#`, MAC, firmware (`#V#` y `#GC#`), estado
   APTO. Si `#GN#` = `NONE`, la app ofrece **"Dar de alta la serie"** (§3.1). Si la serie de la EEPROM
   no coincide con la de la campaña abierta o con la del nombre Bluetooth, **no se mide** y se dice
   por qué.
2. **Dos botones grandes:** "Tengo el ZIP de la campaña: importar" y "Medir la campaña".
3. **Selector de modo:** *Calibración* (sólo los patrones mínimos, §2.3) o *Verificación completa*
   (los 50 y el oscuro).
4. **Cola única guiada**, con K × M fijo (5 × 4, no editable en Operación): calentamiento de 10 min
   (RF-CAL-01), **OSCURO** al principio, **A5** (P22, P28, P4 a 5 × 9, `A5.java:20-24`) y después
   los patrones del modo. Cada paso dice "Coloque P28, blanco IX, cert. 484", y cada colocación
   "Levante y apoye (3 de 5)", con un botón que ocupa media pantalla.
5. Al terminar la cola: **"Campaña completa. Exportar ZIP"**. La exportación es automática y el ZIP
   queda en la carpeta pública de descargas, además del envío (§3.3).

Hoy la A5 y el OSCURO son preajustes que hay que activar y desactivar (`CampanaActivity.java:366-394`).
En el flujo propuesto **están en la cola** y no se pueden saltar en modo Calibración.

### 2.3 Patrones mínimos por código

**Regla, fijada a priori (propuesta):** para cada código ajustable, el patrón de certificado más bajo
y el más alto **de cada tipo de lámina presente**, más el OSCURO si el método es la recta anclada. Si
con eso no se llega a grado + 2 niveles con el rango mínimo (`Asistente.java:75-99`), se añade el
patrón intermedio de mejor repetibilidad. La regla se fija antes de medir y **no se elige mirando el
ajuste**: escoger el subconjunto que mejor reproduce la curva completa es seleccionar a la carta.

Con el catálogo de SLV-002 (`assets/patrones_certificados_P1-P31.csv`) y las medias de la campaña
(`resumen.txt`):

| Código | Patrones de Calibración | Cuántos | Efecto frente a la curva con todos (calculado aquí) | Re-medida (Fase B) |
| :--- | :--- | ---: | :--- | :--- |
| **1** blanco | IV: **P3** (378), **P2** (414). IX: **P27** (471), **P28** (484). XI: **P1** (762), **P4** (828) | 6 de 8 | Pendiente 0,3038 frente a 0,2985. Diferencia en R: −1,6 en x = 1674, +2,7 en 2500 y +7,0 en 3316. R en el oscuro: 1,9 frente a 9,4 | **P28** (IX, x ≈ 2278, medio del rango, con A5: −2,1 %) |
| **8** amarillo tipo I | **P44** (64), **P34** (86), **P43** (122). P37 (82) opcional: la campaña anota "papel de baja calidad" | 3 de 4 | Pendiente 0,2190 frente a 0,2204. Error máximo en los 4: P37 +7,8 % | **P43** (el que más cuentas da sobre el oscuro: ~586) |
| **2** amarillo, recta anclada | **OSCURO**. IV: **P22** (334), **P24** (593). IX: **P31** (573), **P26** (583). XI: **P10** (680), **P20** (782) | 6 de 14, más OSCURO | k = 0,3640 frente a 0,3655 (−0,42 %): −3,4 en R en x = 2800. **Si P24 no se confirma (C-40)** y se usa P29: −1,6 %, −13 en R | **P25** (IX, 576, x ≈ 2157, medio del rango) |

**Por qué no bastan 3 patrones en el código 1.** Con P3, P28 y P4, la recta da **R = 45 en el oscuro**
(x = 575). El límite de P9-B13 que aplica la app es máx(24,7 + 10 ; 25) = 34,7
(`Asistente.java:228-229`, `:237`), así que **la app no la dejaría escribir**. Con P2, P28 y P4 la
pendiente se va a 0,330 y P3 queda a −24 %. P2 y P3 se contradicen (`PROPUESTA…:402`): el que falte
mueve la curva entera. Por eso la regla toma el más bajo y el más alto de cada tipo, y no sólo los
extremos del código.

**Tiempo de campo, estimado** con lo medido el 19-sep. En la A5, 9 disparos por colocación llevaron
12-13 s, unos 1,5 s por disparo, y el cambio de colocación unos 11 s (`campana.csv`, series
S057-S060: 108-110 s por serie de 5 × 9). Se suman 20 s para colocar el patrón y leer la pantalla.
Por patrón a 5 × (1 + 4): 5 × 5 × 1,5 + 4 × 11 + 20 ≈ **1,7 min**.

| Modo | Qué se mide | Estimación |
| :--- | :--- | ---: |
| **Calibración** | Calentamiento 10 min, OSCURO, A5 (3 × 2,2 min), 15 patrones de §2.3 | 10 + 1,7 + 6,6 + 15 × 1,7 ≈ **44 min** |
| Calibración con todos los patrones de 1, 2 y 8 | Igual, con 26 patrones | ≈ **63 min** |
| **Verificación completa** | Los 50 patrones y el OSCURO, más la A5 | ≈ **105 min** |
| Fase B | 3 re-medidas × 1,7 min, escrituras (~5 s por código, T4:1660-1698) y persistencia | ≈ **8 min** |

### 2.4 Fase B — "Calibrar este equipo"

Un botón en la pantalla Campaña. Se activa cuando pasan todas las comprobaciones previas.

**B.0 Comprobaciones previas.** Son automáticas y no piden ningún toque. Si alguna falla, se dice
cuál y cómo resolverla:

- Enlace abierto, pruebas APTO y firmware 3.6.2 (`#GC#` responde).
- `#GN#` y MAC del equipo = serie y MAC de la campaña. Si no coinciden: "Este equipo es SLV-02; la
  campaña es de SLV-002". No se sigue.
- La campaña tiene elegida y aceptada cada serie de los patrones mínimos, más la serie OSCURO si hay
  un código con recta anclada. Si falta el OSCURO, el código 2 aparece como "no calculable: falta
  OSCURO" y los demás siguen (§2.5, conflicto 5).
- Protocolo homogéneo: todas las series elegidas con el mismo K × M. Si no, se avisa y no se sigue
  (P9-B3; hoy `Campana.protocolo()` toma el más frecuente sin avisar, `Campana.java:233-266`).
- A5 medida y su veredicto a la vista. Si cumple el criterio de reversión de P9-A5, no se sigue.
- No hay ninguna calibración a medias pendiente (B.6). Si la hay, se ofrece reanudarla.

**B.1 Cálculo.** Por cada código de la tabla de §2.1, con las series elegidas **de la campaña, nunca
de la sesión** (se elimina la caída de `Sesion.java:241-250`): la curva con el método fijado; los
criterios de SPEC-Calibracion §5 con la s_rep **de la A5**, no la de un campo tecleado; el valor en
el oscuro **en la x medida**; el criterio de `#S` del firmware (`Asistente.criterioFirmwareS`); y la
trama `#S`.

**B.2 Una sola pantalla de resumen.** Una tarjeta por código:

```
Código 1 — blanco intenso — grado 1 — patrones P1 P2 P3 P4 P27 P28 (n = 5 × 4 cada uno)
  Se escribirá:  c1 = 2.98471545E-01   c0 = -1.62263869E+02      (hoy: fábrica)
  R en oscuro (x = 565,4): 6,5   fábrica 24,7   límite 34,7        OK
  R en el patrón más bajo (P3, x = 1674): 337   cert. 378
  Por debajo de x = 1674 la lectura NO está calibrada.
  Avisos: RF-CAL-14 P3 −10,7 %; RF-CAL-15 IV 10,0 %; RF-CAL-16 XI 2,8 > 2,6 %
          (dispensa de Diego, 19-sep-2026 11:20)
  Re-medida: P28
Código 8 — …      Código 2 — …
Se verificarán sin ajustar: 3 4 5 7 a b c d (curva de fábrica)
```

Debajo: el nombre del ZIP y su SHA-256, el protocolo 5 × 4 y la frase "Ajuste contra patrones, no
calibración trazable".

**B.3 Una sola conformidad.** Un bloque con **una casilla por código** ("Acepto la curva del código 1,
con R = 6,5 en oscuro"), el nombre del superadministrador y una nota obligatoria. Un solo botón,
"Calibrar". La conformidad de cada código queda en el acta con su texto (P9-B13 pide aceptación
**por curva**; por eso hay una casilla por código y no una sola casilla).

**B.4 Escritura y verificación en secuencia, código a código.** Para cada código con casilla:

1. Se guarda en disco el estado "escribiendo k", con la copia de los 12 juegos y `#GT#` (RF-CAL-19;
   hoy `leerTodo()`, `AdminActivity.java:412-444`).
2. `#S` → `#G,k#` (relectura, 8 ulp) → `#E` en 5 puntos (±1). Es la lógica de hoy
   (`AdminActivity.java:660-717`), sin cambios.
3. Si algo falla, **se restaura el código a su estado anterior** (`AdminActivity.java:614-636`), se
   relee, se registra y **se detiene** la secuencia.
4. Pantalla completa: **"Coloque P28 (blanco IX, 484) y pulse OK"**. La app elige el patrón (§2.3).
   Después, "Levante y apoye (2 de 5)" … "(5 de 5)". Protocolo del acta, igual al de la campaña
   (P9-B3).
5. Se evalúa la re-medida (`Acta.evaluarRemedida()`, `Acta.java:141-162`) y además RF-CAL-14 frente
   al certificado. **Conforme:** se guarda en disco y se pasa al siguiente código. **No conforme:**
   se ofrece **una** repetición, que queda registrada. Si la repetición tampoco es conforme, se
   restaura el código y se detiene (§2.5, conflicto 3).

**B.5 Persistencia y acta.** "Apague el equipo, espere 5 s y enciéndalo". La app reconecta y repite
`#V#`, `#G` de lo escrito y `#E` en 5 puntos (final de P9-B8). Después verifica los códigos
no ajustados con sus patrones de la campaña (RF-CAL-17, sin volver a medir: se cuenta la desviación
con fábrica) y muestra **el acta completa** con dos botones: **"Aceptar y grabar fecha"** (`#SC` y
`#GC#`, como hoy en `AdminActivity.java:864-885`) y "Rechazar", que ofrece restaurar lo escrito. Al
aceptar, el acta se guarda y **se exporta el ZIP** con el acta dentro.

**B.6 Reanudación.** El estado de la calibración (el acta, la conformidad, qué código va por dónde y
la copia previa) vive **en disco**, no en `Sesion`. Si se corta el enlace, se cierra la app o el
equipo se apaga a mitad, al volver la app ofrece "Hay una calibración a medias de SLV-002 (código 8,
escrito, falta re-medida). Continuar". Si el corte fue durante `#S`, ejecuta P9-B12 antes de nada.

**Toques del operador en la Fase B:** "Calibrar este equipo" (1), las casillas (3), la nota (1 texto),
"Calibrar" (1), "Coloque…" (1 × 3), colocaciones (4 × 3), "Apague y encienda" (1), "Aceptar" (1) y
compartir el ZIP (≥ 2). **Unos 24 toques y una entrada de texto, y ninguna decisión técnica** fuera
de la conformidad. Con la Fase A, unos 14 toques y uno por colocación.

### 2.5 Cómo se cumple cada protección

| Condición | Cómo la cumple el flujo | ¿Choca? |
| :--- | :--- | :--- |
| **P9-B1** A1-A5 cumplidas | B.0 exige la A5 medida en la campaña y firmware 3.6.2 por `#GC#`. El acta lleva el veredicto de la A5 y la G4 de pruebas | No |
| **P9-B2** ZIP citado por SHA-256 | El acta lleva nombre y SHA-256 del ZIP de entrada. Hoy no los lleva (D-14) | No |
| **P9-B3** Protocolo fijo | K × M fijo en Operación. B.0 rechaza campañas con protocolos mezclados. La re-medida usa el del acta | **Sí, con el deseo de "sólo OK"**: conflicto 1 |
| **P9-B4** C-46 | Cumplida | No |
| **P9-B5** Criterios antes de escribir, con s_rep de la A5 | B.1 los calcula antes de cualquier `#S`, con la s_rep de la A5 y no de un campo | No |
| **P9-B6** Método decidido | Tabla de §2.1, fija en el APK. El operador no elige grado | No |
| **P9-B7** Ajuste con la campaña | Se suprime la caída a las medidas de sesión. El resumen lista patrones y n | No |
| **P9-B8** Un código cada vez, re-medida, persistencia | B.4 es estrictamente secuencial. B.5 hace la persistencia. B.6 impide que un corte se salte el bloqueo (hoy sí puede, D-02) | No. Lo refuerza |
| **P9-B9** Curva certificada = `#G` | El acta guarda el texto de `#G` tras `#S` y la predicción en float32, como hoy (`AdminActivity.java:695-700`) | Matiz: la conformidad se da sobre la curva **propuesta**, antes de `#S`. La leída se diferencia en ≤ 8 ulp; si se sale de ahí, ya se restaura. No hace falta otra conformidad |
| **P9-B10** Sin `#P` | El PIN no aparece en Operación. Sólo en Avanzado | **Sí, con RF-CAL-29**: conflicto 4 |
| **P9-B11** Firmware en el acta | La app ya distingue la 3.6.2 por `#GC#` (`Pruebas.java:279-283`). El md5 del `.hex` sigue sin poder leerse del equipo: campo del acta que rellena el superadministrador | No |
| **P9-B12** Corte durante la escritura | Automático en B.6: reconectar, `#G,k#`, `#E` y restaurar si no coincide ni con lo enviado ni con lo anterior. Nunca se sigue con otro código antes | No. Hoy es manual (D-03) |
| **P9-B13** Oscuro y parte baja | Tarjeta de B.2 con R en el oscuro medido y en el patrón más bajo, frente a fábrica, y la frase "no calibrada por debajo". Una casilla de aceptación por curva. El bloqueo de `Asistente.comprobarOscuro` se mantiene | No |
| **`#SC` sólo al aceptar el acta** | Sólo en B.5 al pulsar "Aceptar" | No |
| RF-CAL-22 (C3) Restaurar | B.4.3 restaura al **estado anterior** | **Sí, con "restaura a fábrica"**: conflicto 2 |

**Conflictos. No se elimina ninguna protección; se proponen salidas para que decida Diego:**

1. **"Sólo Coloque P28 → OK" frente a P9-B3/B5.** La re-medida tiene que repetir el protocolo de la
   campaña, con K colocaciones. Con K = 5 son un "Coloque" y cuatro "Levante y apoye". Con K = 1 no
   hay s entre colocaciones y la tolerancia se supone (`Acta.java:153`, `:159`, "(supuesta)"). **Salida:** las
   colocaciones se quedan, pero sin decisiones: botón a media pantalla, contador "3 de 5" y aviso
   sonoro. No se quitan.
2. **"Si falla, restaura a fábrica" frente a RF-CAL-22.** Restaurar a fábrica un equipo que ya tenía
   una calibración buena la destruiría. **Salida:** restaurar al **estado anterior**, que en una
   primera calibración es fábrica. Es lo que hace hoy `restaurar()`.
3. **Re-medida no conforme.** Ni B8 ni RF-CAL-22 dicen que se restaure. Hoy el código queda escrito y
   el acta no se puede aceptar (`Acta.java:186-188`). Con la tolerancia de máx(2 ; 2·s_rep), una
   colocación desafortunada puede dar un falso no conforme. **Propuesta:** una sola repetición
   registrada. Si también falla, restaurar al estado anterior y detener. **Decide Diego**; repetir
   hasta que pase no es aceptable.
4. **RF-CAL-29 frente a P9-B10.** La SPEC pide cambiar el PIN de fábrica antes de la primera escritura
   (`SPEC-Calibracion-V3.6.md:514-516`), y la revisión prohíbe cambiarlo en P8. Es una contradicción
   **entre documentos**, anterior a este flujo. **Salida:** el flujo no cambia el PIN; el acta anota
   "PIN de fábrica" si lo es. Queda abierta.
5. **Patrones mínimos (§2.3) frente a la decisión C y RF-CAL-17.** La decisión C ajusta con **todos**
   los patrones de los códigos 1 y 2 (`SPEC-Calibracion-V3.6.md:374-377`). Con el subconjunto, la
   curva cambia hasta 7 unidades de R en el código 1 y 3,4 en el 2 con los datos de hoy, por debajo
   de la colocación (~14 R, `PROPUESTA…:94`), y la validación dejando uno fuera se hace con menos
   puntos. **Diego tiene que aprobar la regla de §2.3.** Mientras no lo haga, el modo Calibración
   mide los 26 patrones de 1, 2 y 8 (unos 63 min).
6. **Re-medida con un patrón frente a RF-CAL-18/23**, que piden "cada patrón del código". La app ya
   hace hoy uno solo (D-13). **Salida:** un patrón por código en la Fase B. Los demás, en modo
   Verificación completa después de aceptar. El acta declara la desviación. Queda abierta.
7. **Superadministrador.** RF-CAL-26/27 no está implementado. La "conformidad del superadministrador"
   es una casilla (`AdminActivity.java:580-585`). El flujo no lo empeora, pero la conformidad única
   pesa más: **se presenta como control de procedimiento, no como identidad**.

### 2.6 Qué se hace con las pantallas actuales

| Pantalla o control | Destino |
| :--- | :--- |
| Conexión | Se queda. Tres entradas: "Campaña y calibración", "Pruebas" y "Avanzado". La versión en grande (§3.5) |
| Pruebas | Se queda, automática al conectar |
| Medida de patrones (`MedidaActivity`) | A **Avanzado**. Su existencia provoca el texto engañoso (D-08) |
| Botones de pantalla | A Avanzado |
| Campaña: lista de patrones, series, "Era otro patrón" | Se quedan debajo de la cola, plegados |
| Campaña: K, M, tolerancias y preajustes A5 y OSCURO | A Avanzado. En Operación, fijos y dentro de la cola |
| Campaña: "Nueva campaña" y "Cerrar campaña" | A Avanzado, lejos de "Importar" |
| Administrador: leer coeficientes, ajuste manual por código y grado, re-medida manual, parámetros del oscuro | A **Avanzado**, con un rótulo de uso para diagnóstico. El flujo normal no pasa por ahí |
| Administrador: serie | Alta de serie (§3.1), sólo si `#GN#` = `NONE` o desde Avanzado |
| Administrador: `#FT#`, fábrica (uno y todos) y PIN | A Avanzado, cada uno con confirmación tecleada |
| El PIN del equipo (`#L`) | Se pide una vez, al pulsar "Calibrar este equipo", y la app lo renueva antes de que caduque (`AdminActivity.java:33`) |

Avanzado se abre con la frase del superadministrador (RF-CAL-27) cuando exista. Hasta entonces, con un
aviso explícito de riesgo.

---

## 3. Correcciones pequeñas

Son independientes del flujo nuevo y se pueden hacer en la 3.6.10.

### 3.1 Serie: se lee, no se teclea

- **En operación normal, la serie sale de `#GN#`.** La campaña, el acta y los registros usan la serie
  de la EEPROM más la MAC, y el nombre Bluetooth sólo se coteja. Si los tres no coinciden, se para.
  Cambia `Sesion.serie()` (`Sesion.java:157-164`) y elimina la serie tecleada de
  `CampanaActivity.java:131-149` salvo cuando `#GN#` = `NONE`.
- **Alta de serie**, sólo si `#GN#` = `NONE` o desde Avanzado. El diálogo muestra en grande
  **"Serie actual en el equipo: SLV-002 → Nueva: _____"**. La nueva se teclea **dos veces**. Si
  difiere del nombre Bluetooth, hay que marcar una casilla que lo reconozca. Si la actual no es `NONE`,
  se exige además una nota. `#SN` sólo se envía si las dos entradas son iguales, y después se relee
  `#GN#` como hoy (`AdminActivity.java:898-906`).
- El `#SN` y la serie anterior van al diario de la campaña, no sólo al registro.

### 3.2 Importar visible arriba

"Importar ZIP de campaña" es el primer botón de la pantalla Campaña, al mismo nivel que "Medir". Hoy
es el tercero de la sección "Enviar" (`CampanaActivity.java:99-107`). "Nueva campaña" se va a
Avanzado.

### 3.3 Aviso de "no desinstale" y copia fuera de la app

- Al arrancar, si hay una campaña con series sin exportar: **"Hay N series sin exportar. No
  desinstale la app: se borrarían. Para actualizar, instale la nueva versión encima"**.
- Cada exportación deja además una copia en `Download/RTV/` (almacenamiento compartido), para que
  una desinstalación no la borre. `allowBackup` sigue en `false` (`AndroidManifest.xml:14`, decisión
  de seguridad).
- Instrucción de entrega: **instalar encima**. Se ha comprobado que la 3.6.8 y la 3.6.9 llevan la misma
  firma (D-09). Mientras se firme con la clave de depuración de una sola máquina, compilar en otra
  cambiaría la firma y obligaría a desinstalar. Hace falta una clave de firma propia del proyecto
  (P9-P8).

### 3.4 Texto engañoso

`AdminActivity.java:92-94`. Sustituir por: "Usa las series elegidas de la **campaña de este equipo**
(serie y MAC). Sin campaña, no calcula". Y quitar la caída a las medidas de sesión
(`Sesion.java:241-250`), que contradice P9-B7.

### 3.5 Nombre versionado

- Dejar de producir `RTV-V3.6.apk`. Sólo `RTV-V3.6.9-369.apk`, con `versionName` y `versionCode`.
  Cambia `03_App_Movil/RetroV36/README.md:24-25`.
- `versionName` en grande, en la cabecera de todas las pantallas, y en el nombre de cada ZIP y de
  cada acta.
- Con la firma propia (§3.3), `applicationId` distinto para el APK de calibración (RF-CAL-26).

---

## 4. Casos de prueba de aceptación

Formato ISTQB. "Equipo" es SLV-002 con firmware 3.6.2, salvo que se diga otra cosa. Todos exigen
el registro de tramas de la ejecución como evidencia.

| ID | Precondición | Pasos | Resultado esperado |
| :--- | :--- | :--- | :--- |
| **AT-01** Fase A completa | App instalada; `#GN#` = serie del nombre BT; sin campaña | Conectar → "Medir la campaña" en modo Calibración → seguir la cola hasta el final | La cola pide calentamiento, OSCURO, A5 y los 15 patrones de §2.3, todos a 5 × 4 (A5 a 5 × 9). Toques = pasos de cola + colocaciones. Al final, ZIP exportado con SHA-256 y copia en `Download/RTV/` |
| **AT-02** Importar | Campaña vacía; ZIP del mismo equipo | Conectar → "Importar ZIP" (primer botón) | Importa sin desplazarse. Informa "N series importadas". Reimportar da "0 importadas, N ya estaban" (`ImportadorCampana.java:74-81`) |
| **AT-03** ZIP de otro equipo | Campaña de SLV-002 abierta | Importar un ZIP de SLV-003 | "La campaña es de otro equipo… no se importa nada". La campaña no cambia (`ImportadorCampana.java:68-73`) |
| **AT-04** Equipo de otra serie | Campaña de SLV-002; conectado un equipo con `#GN#` = `SLV-003` o con otra MAC | Pulsar "Calibrar este equipo" | Botón deshabilitado con "Este equipo es SLV-003; la campaña es de SLV-002". Ninguna trama `#L`, `#S` ni `#SN` en el registro |
| **AT-05** Serie sin grabar | `#GN#` = `NONE` | Conectar | Ofrece "Dar de alta la serie". Hasta darla, "Calibrar" deshabilitado |
| **AT-06** Error de tecleo en la serie | `#GN#` = `SLV-002` | Avanzado → Serie → teclear `SLV-02` y `SLV-002` | No envía `#SN`: "Las dos entradas no coinciden". Con `SLV-02` dos veces: pide casilla (difiere del nombre BT) y nota; muestra "SLV-002 → SLV-02" en grande |
| **AT-07** Campaña sin OSCURO | Campaña completa salvo la serie OSCURO | "Calibrar este equipo" | Resumen con el código 2 "no calculable: falta OSCURO". Los códigos 1 y 8 se pueden calibrar. No se envía ningún `#S,2` |
| **AT-08** Fase B completa | Campaña completa con OSCURO y A5; APTO | "Calibrar" → casillas y nota → secuencia → apagar y encender → Aceptar | En orden: `#L`; por código `#S`, `#G`, 5 `#E` y re-medida conforme antes del siguiente `#S`; después, reconexión con `#V#`, `#G` y `#E`; `#SC` y `#GC#` **después** de Aceptar y una sola vez. Acta con curva `#G`, oscuro en x medida, conformidad por código, SHA-256 del ZIP y A5. ZIP exportado con el acta |
| **AT-09** Corte de BT durante `#S` | Fase B en el código 8, con el código 1 ya verificado | Cortar el BT del equipo tras enviar `#S,8`, antes de `#OK#` → reconectar | Al reconectar: "Calibración a medias: código 8". Hace `#G,8#` y `#E` en 5 puntos. Si no coincide con lo enviado ni con lo anterior, restaura el estado anterior. No hay ningún `#S` de otro código antes de resolver el 8. El acta conserva el código 1 y su re-medida |
| **AT-10** Corte de BT durante la re-medida | Código 1 escrito y verificado por `#E` | Cortar el BT en la colocación 3 de 5 → reconectar | La re-medida no cuenta. Se ofrece repetirla completa. No se puede escribir el código 8 entre medias (hoy sí se puede: D-02) |
| **AT-11** Cierre de la app a mitad | Código 1 escrito, sin re-medida | Forzar el cierre de la app → abrir → conectar | Reanuda la re-medida del código 1. El acta y la conformidad siguen intactas |
| **AT-12** `#E` no reproduce | Simulador o equipo con respuesta `#E` alterada | Fase B | "`#E` no reproduce la curva". Se restaura el estado anterior con relectura. La secuencia se detiene. El código no entra en el acta |
| **AT-13** Re-medida no conforme | Patrón de re-medida cambiado por otro de distinto valor, o equipo desplazado | Fase B hasta la re-medida del código 1 | "NO CONFORME". Se ofrece una repetición. Si falla la segunda, se restaura el estado anterior del código 1 y se detiene. Las dos re-medidas quedan en el acta. "Aceptar" no admite el código 1 |
| **AT-14** Oscuro fuera de límite | Campaña en la que el código 1 da R en el oscuro > límite (p. ej. sólo P3, P28 y P4, que dan 45 > 34,7) | "Calibrar" | La tarjeta del código 1 dice "no escribible: oscuro 45 > 34,7 (P9-B13)", sin casilla. No hay `#S,1` |
| **AT-15** Batería baja | Batería del equipo por debajo del umbral de uso (medida con polímetro: la app no puede leerla, D-17) | Fase A y Fase B | La app no detecta la batería. **Lo esperado es que la calidad lo detecte**: s entre colocaciones > `REPRO_MAX` → serie repetir; equipo mudo → tratado como corte (AT-09 y AT-10). Checklist inicial "batería cargada o cambiada" anotada en el acta |
| **AT-16** Caducidad del modo admin | Fase B en curso, 9 min sin tramas `#` | Dejar el teléfono parado en "Coloque P28" 10 min → OK | La app renueva `#L` antes de medir o pide el PIN. No se repite ningún `#S` ya verificado |
| **AT-17** Rechazo del acta | Fase B terminada | "Rechazar" con motivo | Sin `#SC`. Se ofrece restaurar los códigos escritos al estado anterior. Acta RECHAZADA guardada y en el ZIP |
| **AT-18** Protocolo mezclado | Campaña importada 1 × 9 más series nuevas 5 × 4 | "Calibrar" | "Protocolos distintos en la campaña: 1 × 9 y 5 × 4". No se sigue (P9-B3) |
| **AT-19** Actualizar la app | 3.6.9 con campaña; APK 3.6.10 de la misma firma | Instalar encima | La campaña sigue. Si se intenta desinstalar con series sin exportar, el aviso ya se mostró al arrancar |
| **AT-20** Versión visible | APK entregado | Mirar el fichero y la cabecera de la app | Fichero `RTV-V3.6.10-3610.apk`, sin `RTV-V3.6.apk`. La cabecera de todas las pantallas muestra la versión. El ZIP y el acta la llevan |
| **AT-21** Firmware 3.6.1 | Equipo con 3.6.1 (`#GC#` sin respuesta válida) | "Calibrar" | Deshabilitado: "Hace falta la 3.6.2 (serie y fecha de calibración)" |
| **AT-22** Falta la A5 | Campaña sin series A5 | "Calibrar" | Deshabilitado: "Falta la medida puente A5 (P9-A5)" |

---

## 5. Riesgos que este documento no resuelve

- **Nada de §2 está validado en hardware.** Los tiempos de §2.3 son estimaciones con tiempos de
  disparo reales, no los de un flujo que no existe.
- La regla de patrones mínimos se ha comprobado **con una sola campaña**, la del 19-sep. Con otra,
  la diferencia frente a la curva completa puede ser mayor.
- P24 sigue con la identidad sin confirmar (C-40), y el código 2 la usa como extremo alto de los IV.
- La batería no se puede vigilar sin cambiar el firmware, y el firmware no se toca.

---

## 6. Veredicto

**La 3.6.9 no está lista para un operador de campo sin soporte.** La prueba es el propio 19-sep: con el
dueño del proceso a los mandos, el equipo quedó con un código escrito sin verificar, el acta en el aire
y la serie mal grabada (§0). Las protecciones del acta funcionaron: P9-B8 impidió escribir los
códigos 8 y 2. Pero la app no llevó al operador hasta la salida, y un corte o una reconexión las
habría desactivado (D-02).

**Lista mínima para que sí** (en orden):

1. **Acta y calibración en curso persistidas en disco, con reanudación**, y `reiniciar()` sin borrar el
   acta (D-02, P9-B8).
2. **Recuperación automática de P9-B12** ante un corte durante `#S` (D-03).
3. **Re-medida guiada dentro de la secuencia**, con el patrón elegido por la app (D-01, D-13).
4. **Serie de `#GN#` como identidad**, con el cotejo con MAC y campaña y el alta con doble entrada
   (D-04, D-05).
5. **Botón "Calibrar este equipo"** con resumen, conformidad por código y método fijo, y lo demás
   en Avanzado (D-06, D-11, D-12).
6. **Importar arriba, aviso de no desinstalar, copia del ZIP fuera de la app y APK versionado**
   (D-07, D-09, D-10).
7. **Texto corregido y sin caída a las medidas de sesión** (D-08).
8. **Persistencia (apagar y encender) dentro del flujo** (D-16).
9. **Decisiones de Diego** sobre los conflictos 3, 5 y 6 de §2.5, y sobre la regla de patrones
   mínimos.
10. **AT-01 a AT-22 superados** en SLV-002 o en el segundo equipo, con los registros archivados.

Los puntos 1 a 3 son P1 **aunque no se haga el flujo nuevo**: sin ellos, la 3.6.9 puede dejar un
código escrito sin verificar sin que nada lo impida.
