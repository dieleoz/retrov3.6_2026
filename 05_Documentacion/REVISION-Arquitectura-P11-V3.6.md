# Revisión de arquitectura P11 — V3.6 (app 3.6.12, "Calibrar este equipo"; firmware 3.6.2 sin cambios)

**Nada de lo que revisa este documento se ha ejecutado nunca contra un equipo ni contra un simulador.**
El flujo "Calibrar este equipo" escribe en la EEPROM y no ha corrido ni una vez: T-S07, T-S09, T-S10,
T-S12 y T-S16 están PENDIENTES (`TDD-V3.6.md:1270-1318`), el simulador tampoco existe (T-S00 PENDIENTE,
`TDD-V3.6.md:1201`) y T-C43 está PENDIENTE (`TDD-V3.6.md:1376`). Todo lo que sigue sale de leer el
fuente, de ejecutar las pruebas JVM y de cotejar el APK. Los cálculos propios se marcan como tales.

- **Fecha:** 19-sep-2026, tarde. Revisor adversario. Es el visto bueno final (P11) antes de escribir
  calibraciones en SLV-002, equipo de cliente con firmware 3.6.2.
- **Repositorio:** `D:\IT\P_RetroVertical_V3.6`, HEAD `e159fdf`. El árbol de la app está limpio respecto
  a ese commit.
- **App revisada:** RTV 3.6.12. Delta `fda734e..e159fdf`: 15 ficheros y +2160 líneas.
- **APK:** `03_App_Movil/RTV-V3.6.12.apk`, md5 `6a7000e7c51b6709926211cb7a95a63e`, recalculado aquí.
  - `aapt`: `versionCode='3612' versionName='3.6.12'`, igual que `app/build.gradle`.
  - Las 47 clases de `com.dpi.retrov36` del dex son las 47 de `src/main/java`.
  - Están las cadenas nuevas de la 3.6.12 y faltan las que el commit borró de `AdminActivity`.
  - Los assets son iguales al fuente: cola `9ddb7882…` y catálogo `07ab9cd8…`.
  - **El binario corresponde a `e159fdf` por contenido**, no byte a byte: no se ha recompilado.
- **Pruebas JVM:** **120 de 120 en verde** con JDK 11.0.24.
  - Con el método de P9 y P10 (`javac -sourcepath src/main/java`), 108 pruebas de 11 clases.
  - `Version3612Test` (12 pruebas) **no compila con ese método**: `OpsEquipo.tramaG`
    (`Version3612Test.java:60`, `:204`) arrastra `Cliente` y `Sesion`, que importan `android.*`. Pasa
    compilando con `android.jar`, `androidx.core` y un `BuildConfig` a mano.
- **Contra:**
  - P9 (B1-B13) y P10 (C1-C12, corte B de §6, hallazgos de §5);
  - `SPEC-V3.6.md` r1.3 y `SPEC-Calibracion-V3.6.md` §12, con la tabla RF-CAL-37
    (`SPEC-Calibracion-V3.6.md:779-787`);
  - REFORM (`06_Calibracion/SLV-002/REFORMULACION-y-Simulacion-2026-09-19.md`);
  - `TDD-V3.6.md` §3 ter;
  - `QA-App-3.6.10.md` y el arreglo 3.6.11 (`fda734e`).

Las rutas de la app son relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`, y las de
firmware, a `01_Firmware/RetroVertical_V3.6.X/`.

---

## 1. Veredicto

**(c) NO APTO hoy, con una lista mínima de siete arreglos (§5.1) y cuatro condiciones de procedimiento
(§5.2).** Con eso cumplido pasa a **(a): APTO para calibrar el 8 después del banco**. **El b, además,
sólo con PA-24 decidida por escrito.**

**(b) no es posible, y eso está bien.** Con los datos de hoy la 3.6.12 no escribe nada en SLV-002:

- El 8 y el b sólo son calibrables si todos sus pasos AJUSTE y RE-MEDIDA de la cola están en `HECHO`
  (`BancoCola.java:250-262`). La campaña del 19-sep no tiene pasos de banco, así que la tarjeta dice "No
  calibrable" (`CalibrarActivity.java:234-235`).
- El 1 y el 2 están en `NO_REESCRIBIR` (`TablaCalibracion.java:75-76`), y el 5 en `SOLO_VERIFICAR`
  (`:79-81`).

**Por qué no es (a) tal como está.** Hay tres secuencias sin error del operador, más una que pasa por
Avanzado, que acaban en una de las cuatro situaciones del encargo: un acta aceptada que no corresponde
al equipo, o una curva escrita sin acta válida (§3):

- una restauración fallida cuenta como resuelta;
- la persistencia no se rehace cuando se escribe otro código después de hacerla;
- el acta cita el oscuro de otro código;
- Avanzado puede escribir dentro del acta del flujo, saltándose la tabla y D-20.

**Además, el flujo no ha corrido nunca, ni en simulador.** La primera ejecución de un flujo que escribe en
EEPROM no puede ser en el equipo del cliente.

Lo que sí está bien resuelto, y hay que decirlo:

- el diario del acta en disco y su reanudación (D-02);
- el corte durante `#S` (RF-APP-37);
- D-20 dentro del flujo;
- los filtros de P10-C1;
- `#SC` que no cierra el acta si falla (P10-C5);
- s_rep que ya no se teclea (P10-C6);
- la tabla RF-CAL-37 cableada.

Los arreglos que faltan son pequeños y locales.

---

## 2. Condiciones de P9 y P10 que aplican al corte B

Leyenda: **C** cumplida, **P** parcial, **N** no cumplida. "Test" es la prueba JVM que lo cubre. Las
12 de `Version3612Test` ejercitan sólo clases puras (`Acta`, `TablaCalibracion`, `Remedida3611`,
`Anclas`, `Campana`, `BancoCola`). **Ninguna ejercita `CalibrarActivity` ni `AdminActivity`.**

### 2.1 P10

| N.º | Estado | Evidencia en el código | Test |
| :--- | :---: | :--- | :--- |
| P10-C1 (filtros de colocación) | **C** | Patrón presente en [0,8 ; 1,2]·x̄_banco (`Colocacion.java:58-69`). Coherencia por par, máx(3 ; 2 %·R_#G) (`:72-89`). Se usan antes de contar la colocación (`CalibrarActivity.java:560-571`) | `colocacionConPatronAusenteNoCuenta`. El caso real de las 12:17:02 sólo está en `Version3610Test` y `Version3611Test`, y no pasa por `Remedida3611.colocacion` |
| P10-C2 (tabla única) | **P** | La tabla Java coincide con la SPEC en métodos y patrones (`TablaCalibracion.java:75-88` frente a `SPEC-Calibracion-V3.6.md:781-787`). Pero **el b se puede escribir sin PA-24**: `exigeConformidad` (`:36`, `:86`) no se lee en ningún fichero, y el b recibe la misma casilla que los demás (`CalibrarActivity.java:251-259`). La SPEC dice "Sin ella, sólo verificar" (`:786`) | `tablaRfCal37`, que no comprueba el b sin conformidad |
| P10-C3 (cola separada del método) | **C** | La cola sólo aporta los patrones y el estado (`CalibrarActivity.java:190-201`). El método sale de la tabla | — |
| P10-C4 (D-20) | **P** | Dentro del flujo: todos los intentos al diario, incluidos NO_VALIDA (`Acta.java:406-419`). Máximo una repetición (`:159-161`) y restauración tras la segunda (`CalibrarActivity.java:524-529`). **Fugas:** (1) `motivoNoEscribir(k)` no bloquea el mismo código con una re-medida válida pendiente (`Acta.java:335-339`), así que un `#S` desde Avanzado (`AdminActivity.java:685`) lo reescribe y `anularAnterior` reinicia el contador (`Acta.java:376-384`); en el texto sólo queda "anulado (n intentos)" (`:632-634`). (2) Un intento cortado a mitad no deja rastro en el acta, aunque T-S09 lo pide (`TDD-V3.6.md:1270-1274`) | `d20UnaRepeticion…` y `reescribirElMismoCodigoNoBloquea`, las dos sobre `Acta`. **No hay prueba de reescribir tras un NO CONFORME** |
| P10-C5 (lo certificado está en el equipo) | **P** | `#V#` y `#G` frescos antes de aceptar (`CalibrarActivity.java:700-713`). `#SC` fallida no cierra (`:720-731`; `Acta.java:570-572`). `#F` desde Avanzado invalida (`AdminActivity.java:996-999`). **Pero:** (1) de `#V#` sólo se comprueba que responda (`CalibrarActivity.java:702`), no que diga `CAL` ni la máscara; (2) la invalidación sólo actúa si el acta está en memoria (`Sesion.acta`, que `Sesion.java:130` borra al reconectar); (3) no se comprueban los códigos 1 y 2 (§4) | `fTrasLaConformidad…` y `siSCFalla…`, sobre `Acta`. La vía de `AdminActivity` no tiene prueba |
| P10-C6 (s_rep no se teclea) | **C** | `Anclas.sRep` (`Anclas.java:85-107`) y la comprobación previa (`CalibrarActivity.java:128-130`). En Avanzado, `TextView` en lugar de `EditText` (`AdminActivity.java:135`, `:474-485`) | Sin prueba. Lo garantiza la interfaz |
| P10-C10 (anexo y T-C41) | **P** | Anexo hecho (`06_Calibracion/SLV-002/ANEXO-Acta-20260919-122326.md`). **T-C41 sigue PENDIENTE** (`TDD-V3.6.md:1361`), y la 3.6.12 no la hace | — |
| P10-C11 (dos cortes) | **C** | 3.6.10 y 3.6.11 (corte A) y 3.6.12 (corte B), con nombre versionado | — |
| P10-C12 (1 + 4) | **C** | El acta declara 5 × 4 con 1 de asentamiento (`CalibrarActivity.java:377`; `Acta.java:596-597`) | — |

### 2.2 Corte B de P10 §6

| Orden | Estado | Evidencia |
| :--- | :---: | :--- |
| B1 (acta en disco) | **C** | Diario de sólo añadir, con `flush` por evento (`Acta.java:177-183`). Lectura y reanudación en `Campanas.java` (`actaEnCurso`), filtrando por MAC. Sin prueba de disco: `actaEnDiscoSeRetomaIgual` usa un `StringWriter` |
| B2 (re-medida) | **C/P** | Como P10-C1 y C4. La s_rep de la propia serie con K ≥ 3 (RF-CAL-38, T-A53 c) no está: `Remedida3611.java:60` la menciona y el código no la calcula |
| B3 (verificaciones frescas) | **P** | Como P10-C5 |
| B4 (corte durante `#S`) | **C/P** | `resolverCorte` (`CalibrarActivity.java:431-454`) compara `#G` con lo enviado y lo anterior, y hace `#E`. Pero el código reanudado entra en el acta **sin método, sin oscuro y sin dispensa** (`:438`), y así esquiva el "falta oscuro" de `Acta.java:542-546`. Sin prueba de `resolverCorte` |
| B5 (tabla, oscuro, s_rep, deriva) | **P** | Deriva y ancla por sesión (`Anclas.java:46-76`). Pero: (1) sin OSCURO en la sesión se cae al OSCURO elegido de toda la campaña (`:78-82`), en contra de `SPEC-Calibracion-V3.6.md:750-751`; (2) `Asistente.X_OSCURO` y `ORIGEN_ANCLA` son estáticos y los pisa cada tarjeta (§3, caso 4) |
| B6 (persistencia) | **P** | Existe (`CalibrarActivity.java:623-660`), pero no se anula al escribir otro código después (§3, caso 2) |
| B7 (identidad, alta de serie) | **C** | `#GN#` igual a la campaña (`CalibrarActivity.java:121-127`). Doble entrada y aviso en el alta (`AdminActivity.java`, diálogo "Alta de la serie") |
| B8 (botón y bloqueo por batería) | **C/P** | Batería antes del flujo y antes de cada `#S` (`CalibrarActivity.java:391-395`, `:466-470`). El bloqueo previo (`:131-133`) sólo se levanta con otra lectura del banco (`BancoActivity.java:207`): desde "Calibrar" no hay salida. Molesto, pero seguro |
| B9 (campos de RF-CAL-43) | **P** | Obligatorios sólo 4 campos (`Acta.java:53`). Faltan del criterio de `SPEC-Calibracion-V3.6.md:869-884`: SHA-256 del ZIP, A5 de inicio y fin con deriva, V al inicio y V mínima (el dato "batería" se sobrescribe, `OpsEquipo.java:79-81`) y "PIN de fábrica sí/no" |

### 2.3 P9 (condiciones B)

| N.º | Estado | Nota |
| :--- | :---: | :--- |
| B1 (A1-A5) | **P** | La A5 de P9-A5 se sustituye por la A5 del banco (C-CAL-22). La app la usa para s_rep, pero no evalúa el criterio de reapertura del 1 y el 2 |
| B3 (protocolo fijo) | **C** | Pasa a 1 + 4 (P10-C12) |
| B5 (criterios antes de escribir, con s_rep) | **C** para el 8, **P** para el b | Un incumplimiento sin dispensa bloquea (`CalibrarActivity.java:324-331`). En el b la dispensa está "pendiente" y aun así deja pasar |
| B6 (método decidido) | **C** en el código | La tabla lo fija. La decisión de Diego sobre C-P10-2 (8 anclado) no consta firmada. C-CAL-15 (certificado de los tipo I) sigue abierta (`SPEC-Calibracion-V3.6.md:785`) |
| B7 (ajuste con la campaña) | **P** | Puntos: patrones AJUSTE y RE-MEDIDA de la cola ∩ series elegidas (`CalibrarActivity.java:203-212`). La serie del banco pasa a elegida (`BancoActivity.java:608-613`, QA-3610-10). **El acta no lista los patrones ni n** |
| B8 (un código cada vez y persistencia) | **P** | Un código cada vez: C (`Acta.java:335-339`). Persistencia: P (§3, caso 2) |
| B9 (la curva leída con `#G`) | **C** | `CalibrarActivity.java:482`, `:494`. La re-medida evalúa en float32 con la curva leída (`Remedida3611.java:48-51`) |
| B10 (sin `#P`) | **C** | "Calibrar" no envía `#P` |
| B11 (firmware en el acta) | **C** | `Sesion.firmware()` con la variante 3.6.2 por `#GC#`. No lleva el md5 del `.hex` |
| B12 (corte de enlace) | **C/P** | Como B4 |
| B13 (oscuro y parte baja) | **P** | La comprobación del oscuro en la propuesta usa el ancla correcta. **La línea del acta no** (§3, caso 4) |

### 2.4 Hallazgos de P10 §5 que tocan al corte B

| # | Estado |
| :---: | :--- |
| 6 (`#SC` fallida) | Cerrado (`CalibrarActivity.java:720-731`) |
| 7 (`#F` sin consultar el acta) | Cerrado en lo sustancial: la verificación final lo detecta siempre para lo certificado (`:703-713`) |
| 8 (acta sólo en memoria) | Cerrado |
| **9 (restauración fallida)** | **Sigue abierto** (§3, caso 1) |
| 10 (D-18) | Cerrado: `leerV` actualiza marca y máscara (`OpsEquipo.java:30-40`) y el acta guarda el `#V#` posterior |
| 11 (575) | Cerrado en "Calibrar". **Sigue en Avanzado**, que teclea el oscuro (`AdminActivity.java:127-128`, `:471`) |

---

## 3. Recorrido de riesgo sobre SLV-002

Hecho de firmware que acota el daño, comprobado aquí:

- `guardarEeprom` sólo escribe los bytes que cambian (`eeEscribirSiDistinto`, `calibracion_v36.c:118`,
  `:132-138`).
- Por tanto, un corte de alimentación durante `#S,8` sólo puede dañar el registro del 8. El registro
  dañado vuelve a fábrica al arrancar (`:188`).
- **Los registros del 1 y del 2, y el del PIN, no se reescriben.**
- **Ninguna secuencia deja el equipo inservible.** "Calibrar" no envía `#P`, `#FT` ni `#F,*`. Cinco PIN
  fallidos se resuelven apagando y encendiendo.

### Caso 1. Re-medida no conforme dos veces, con una restauración que no queda bien — acta aceptada que no corresponde al equipo

Tras la segunda NO CONFORME, `CalibrarActivity.java:526-527` restaura y **marca el código como
`restaurado` pase lo que pase**. `OpsEquipo.restaurar` devuelve un texto con "NO igual a la anterior"
(`OpsEquipo.java:106-110`), pero no hay ninguna rama que lo trate como fallo. El código pasa a resuelto
(`Acta.java:154-156`) y deja de figurar entre los certificados (`:460-468`).

Si otro código del acta está conforme, el acta se acepta, porque la verificación final sólo relee los
certificados (`CalibrarActivity.java:703`), y se graba una fecha `#SC` nueva. El equipo queda con un
código en un estado desconocido, y el acta dice "RESTAURADO".

Es poco probable: con el 8 y el b la curva anterior es la de fábrica y se restaura con `#F,k#`. Pero es
justo el caso por el que se pregunta. Si en cambio el enlace se cae durante la restauración, el caso es
seguro: salta una excepción, no se anota `restaurado` y al volver se reintenta (`:524-529`).

### Caso 2. Escribir el 8, persistencia y después escribir el b — acta aceptada sin persistencia del b

`Acta.escrito` (`Acta.java:391-399`) no anula ni `persistenciaOk` ni `verificacionFinalOk`. El botón de
persistencia se habilita en cuanto todos los códigos están resueltos (`CalibrarActivity.java:155`), y el
mensaje final del flujo invita a hacerla después de cada tanda (`:427`).

Si Diego calibra el 8, hace la persistencia y **después** selecciona el b, el acta se acepta con el b sin
haber apagado y encendido. Incumple P9-B8 y RF-APP-46. La verificación final sí relee el `#G` del b, pero
no tras un arranque.

### Caso 3. `#F` desde Avanzado con el acta abierta

- **Si el acta está en memoria**, porque Diego viene de "Calibrar" en la misma conexión: `#F,k#` o
  `#F,*#` la invalidan (`AdminActivity.java:996-999`), incluso sin respuesta `OK`. La comprobación previa
  bloquea y pide rechazar (`CalibrarActivity.java:134-136`). Correcto.
- **Si Diego ha reconectado desde la pantalla inicial:** `Sesion.reiniciar` borra `acta`
  (`Sesion.java:130`) y Avanzado no ve el acta, así que no la invalida.
  - Aun así, el acta no se puede aceptar: `aceptarInterno` relee `#G` y da "DISTINTO de lo certificado"
    (`CalibrarActivity.java:703-713`). **Queda cubierto por la verificación final, no por la
    invalidación.**
- **`#F,1#` o `#F,2#`**, los códigos del acta de las 12:23:
  - `acta.codigo(k)` es `null` y no invalida nada.
  - La verificación final no mira el 1 ni el 2.
  - `#V#` no se compara con la máscara esperada (`:702`).
  - Resultado: **se acepta un acta nueva y se renueva la fecha `#SC` en un equipo con el 1 en fábrica.**
    El acta de las 12:23 deja de corresponder al equipo y nada lo registra.

### Caso 4. El 8 y el b en sesiones distintas — el acta cita el oscuro de otro código

Según la cola, el 8 se mide en la sesión 1 (P43, fila 54) y el b en la sesión 2 (P39, P38 y P49, filas
96-98).

- `pintarTarjetas` recorre los 12 códigos (orden 1…8, a, b…) y, para cada tarjeta calibrable,
  `propuesta()` pisa los estáticos `Asistente.X_OSCURO` y `ORIGEN_ANCLA` (`CalibrarActivity.java:268-271`,
  `:294-296`).
- Cuando se escribe el 8, `escribir()` redacta la línea B13 del acta con `X_OSCURO` y el dato "oscuro" con
  `ORIGEN_ANCLA` (`:489-493`). **Los dos son los de la sesión 2, la del b.**
- La curva sí es correcta: se ajustó con el ancla propia al construir la propuesta (`Asistente.java:450`),
  y su método lo cita (`:527-528`).
- Resultado: dentro del acta del 8, el método y la línea del oscuro dan dos x distintas. Además "oscuro"
  es una sola clave para todos los códigos (`Acta.java:543`).
- Si una tarjeta no tiene ancla (NaN), hereda el valor anterior (`:269`) o el 575 por defecto
  (`Asistente.java:218`).

### Caso 5. Bluetooth caído, punto a punto

| Punto | Qué pasa | Evidencia | ¿Seguro? |
| :--- | :--- | :--- | :---: |
| Login o batería | La excepción corta el flujo y no se escribe nada | `CalibrarActivity.java:355-361`, `:373-395` | Sí |
| Tras `ESCRIBIENDO` y antes o después de `#S`, o durante `#G`/`#E` | Al volver, `resolverCorte`: "entró", "no entró" o "ni una ni otra", y restaura | `:396-398`, `:431-454`; `Acta.java:362-368` | Sí. **Pero en "entró" el acta pierde método, oscuro y dispensa** (`:438`) |
| En la re-medida | La colocación en curso se pierde sin anotarse. Al volver, la re-medida empieza de cero y el contador de válidas no cambia | `:537-578` | Sí para el equipo. Falta anotarlo (T-S09) |
| En la restauración tras dos NO CONFORME | No se anota `restaurado` y al volver se reintenta | `:524-529` | Sí |
| En la persistencia | "Repita la persistencia" | `:638-644` | Sí |
| Entre `#SC` con `OK` y el evento `ACEPTADA` | La fecha queda en el equipo con el acta abierta. Al volver, "Aceptar" repite `#SC` y cierra. **Si Diego rechaza en vez de reintentar, la fecha se queda** | `:720-735`, `:740-755` | Casi. Se cierra reintentando |

### Caso 6. Batería a 0

- `9` antes del flujo y antes de cada `#S`. Si no responde o da n = 0, bloquea (`CalibrarActivity.java:391-395`,
  `:466-470`; `Campana.java:351-353`).
- Si la batería muere durante `#S`, el registro del código queda a medias y vuelve a fábrica al arrancar.
  Al volver, `resolverCorte` lee fábrica: si lo anterior era fábrica, "no entró"; si no, "ni una ni otra"
  y restaura.
- Los umbrales de PA-17 siguen sin T-C44.
- **Seguro.**

### Caso 7. App matada a mitad

El diario se vuelca evento a evento (`Acta.java:177-183`) y se reanuda desde disco (`Campanas.actaEnCurso`).
Los puntos de corte son los del caso 5. **Seguro, con dos salvedades:**

- **Si Diego no vuelve o rechaza el acta,** lo escrito se queda en el equipo. "Rechazar" no restaura:
  sólo lo avisa (`CalibrarActivity.java:744`). SLV-002 **ya tiene fecha `#SC` del 19-sep que vence el
  19-sep-2027** (commit `afdd700`). Así, una curva del 8 sin verificar, o verificada en un acta
  rechazada, quedaría bajo una fecha válida.
  - No hace falta ninguna `#SC` nueva para llegar a "fecha sin calibración verificada": basta con la
    vieja.
- **Rechazar y abrir un acta nueva reinicia D-20.** El acta rechazada queda archivada con su diario
  (`Campanas.java`, `cerrarActaEnDisco`), pero la nueva no la cita.

### Resumen de los cuatro riesgos del encargo

| Riesgo | ¿Hay secuencia? | Casos |
| :--- | :--- | :--- |
| Curva escrita sin acta | **Sí**, por diseño: "Parar aquí", rechazar o no volver. Sin restauración automática | 7 |
| Acta aceptada que no corresponde al equipo | **Sí** | 1, 3 (`#F,1`/`#F,2`), 4 (en el dato del oscuro) y la vía de Avanzado de P10-C4 |
| Fecha `#SC` sin calibración verificada | **No por una `#SC` nueva**: sólo se graba tras la verificación final. **Sí de hecho**: la fecha del 19-sep ya cubre cualquier curva que se deje escrita | 3 y 7 |
| Equipo inservible | **No** | §3, cabecera |

---

## 4. Estado heredado de SLV-002

**Códigos 1 y 2 (acta de las 12:23).** La 3.6.12 los **respeta, pero no los verifica ni los protege**:

- **No los reescribe desde "Calibrar".** Están en `NO_REESCRIBIR`
  (`TablaCalibracion.java:75-76`): su tarjeta dice "No se escribe." y no tiene casilla
  (`CalibrarActivity.java:232-233`, `:251`).
- **No los verifica.**
  - La tabla les asigna P28 y P25 como re-medida, pero el flujo sólo re-mide lo que escribe
    (`:399-426`).
  - La persistencia y la verificación final recorren sólo los certificados del acta en curso
    (`:649`, `:703`).
  - **T-C41 sigue sin hacer** (`TDD-V3.6.md:1361-1367`), y la 3.6.12 no la sustituye.
- **No los protege.**
  - Avanzado no consulta la tabla: no hay ninguna referencia a `TablaCalibracion` en `AdminActivity`.
  - Puede reescribir el 1 o el 2 dentro del acta del flujo (`AdminActivity.java:678-695`), con el
    oscuro tecleado (`:471`).
  - Puede devolverlos a fábrica sin que nada lo note (caso 3).
- **Si la A5 del banco reabre el 1 y el 2** (C-CAL-22; anexo, §5), "Calibrar" no puede
  recalibrarlos: `NO_REESCRIBIR` está compilado. Haría falta un APK nuevo o pasar por Avanzado, que es la
  vía sin control.

**Códigos 8 y b.**

- **Cómo se calculan.** Los dos van con recta anclada en la media de los OSCURO de inicio y fin de su
  sesión (`Anclas.java:46-72`), ajustada en la app con los puntos del banco.
  - **No se escriben los coeficientes de REFORM** (`SPEC-Calibracion-V3.6.md:785-786`). REFORM ancló
    en el S060 (565,36) con K = 1 de las 10:33. Con K = 5 y el ancla de la sesión no saldrán iguales,
    y está bien que no salgan.
- **El 8:**
  - re-medida con P43;
  - sin dispensa: si incumple, no se escribe;
  - C-CAL-15 (certificado de los tipo I) sigue abierta.
- **El b:**
  - re-medida con P49;
  - **se puede escribir marcando la casilla aunque PA-24 no esté decidida** (§2.1, P10-C2);
  - con la conformidad genérica (nombre y nota), la dispensa entra en el acta como "aceptada en la
    conformidad" (`CalibrarActivity.java:494-496`);
  - eso contradice "Sin ella, sólo verificar" (`SPEC-Calibracion-V3.6.md:786`).

**Código 5 con PA-14 sin decidir.**

- `PA14_ANCLADA_5 = false` (`TablaCalibracion.java:25`) y nada lo cambia, así que el 5 queda en
  `SOLO_VERIFICAR`.
- **"Sólo verificar" no hace nada:** no hay casilla ni re-medida para los códigos no escribibles
  (`CalibrarActivity.java:232-233`, `:251`), y el acta no nombra el 5.
- **Si Diego no decide,** el 5 sigue de fábrica, que lee un −55 % (`SPEC-Calibracion-V3.6.md:1028`),
  y la fecha `#SC` nueva lo cubre sin decir nada.
- **Si decide que sí,** no basta con cambiar el indicador:
  - hace falta otro APK;
  - la regla de cobertura de la anclada de RF-APP-42 no está programada: la anclada se evalúa como grado
    1 con los valores de los patrones (`Asistente.java:406-432`), y T-A56 fallaría.
- **No es un riesgo de escritura:** es el valor por defecto seguro. **Es un riesgo de documento:** el
  acta tiene que declarar el 5 como no calibrado.

---

## 5. Lo que hace falta

### 5.1 Lista mínima de arreglos (3.6.13), sin la cual es NO APTO

| N.º | Arreglo | Dónde | Prueba que lo cierra |
| :---: | :--- | :--- | :--- |
| **P11-M1** | Una restauración cuya relectura no coincide **no** resuelve el código. El acta queda no aceptable y lo dice en rojo | `CalibrarActivity.java:526-527`; `OpsEquipo.java:106-110` (devolver el resultado, no sólo el texto) | JVM: `restaurado` con relectura distinta ⇒ `motivoNoAceptable` ≠ null |
| **P11-M2** | Cada `escrito()` y cada `sinEscribir()` posterior anulan `persistenciaOk` y `verificacionFinalOk` | `Acta.java:391-399` | JVM: escribir 8, persistencia OK, escribir b ⇒ "falta la persistencia" |
| **P11-M3** | Oscuro por código. La propuesta guarda su x de ancla y su origen, y el acta los anota por código ("oscuro 8", "oscuro b"). El corte reanudado conserva método, oscuro y dispensa, que se anotan en `ESCRIBIENDO` | `CalibrarActivity.java:268-297`, `:438`, `:489-493`; `Acta.java:543` | JVM: 8 en la sesión 1 y b en la sesión 2 ⇒ cada acta con su oscuro. `resolverCorte` en "entró" ⇒ método presente |
| **P11-M4** | Avanzado no escribe mientras haya abierta un acta de la tabla (`tabla` no vacía). `motivoNoEscribir(k)` rechaza el mismo k si tiene una re-medida válida sin resolver | `AdminActivity.java:678-695`; `Acta.java:325-341` | JVM: NO CONFORME en el 8 ⇒ `motivoNoEscribir('8')` ≠ null |
| **P11-M5** | El b en `SOLO_VERIFICAR` hasta que PA-24 esté decidida, igual que el 5 con PA-14 (usar `exigeConformidad`) | `TablaCalibracion.java:86`; `CalibrarActivity.java:251-259` | `tablaRfCal37` ampliada |
| **P11-M6** | La verificación final exige `#V#` en `CAL`, con máscara igual a {1, 2} ∪ certificados, y el `#G` del 1 y del 2 igual al texto del acta de las 12:23 (T-C41, P9-P3). Si no, el acta no se acepta | `CalibrarActivity.java:700-713` | JVM sobre el comparador de máscara. T-C41 en el equipo |
| **P11-M7** | **El flujo corre una vez fuera del equipo del cliente:** T-S07, T-S10 y T-S12 en simulador. El simulador no existe (T-S00), así que la alternativa es un equipo propio con la 3.6.2. Se archiva el registro | `TDD-V3.6.md:1201`, `:1276`, `:1287` | Registro y acta archivados |

### 5.2 Condiciones de procedimiento, antes del primer `#S` en SLV-002

1. **T-C41 en SLV-002** antes de escribir nada. Si falla, no se calibra y se reabre el acta de las 12:23.
2. **A5 del inicio del banco** con el criterio de C-CAL-22. Si reabre el 1 y el 2, "Calibrar" no sirve
   para ellos (§4).
3. **PA-24 por escrito** para el b, y la nota de C-CAL-15 (certificado de los tipo I P43, P38, P39 y P49)
   para el 8, las dos en el texto de la conformidad.
4. **Si se rechaza un acta, se restaura a mano** en Avanzado lo que se haya escrito, con `#F,k#`, y se
   anota. La fecha del 19-sep ya cubre cualquier curva que se quede escrita (§3, caso 7).

### 5.3 Deberían ir en la 3.6.13, pero no bloquean

- "Rechazar" ofrece restaurar lo escrito a su `anterior`, que la app conoce.
- La comprobación previa exige APTO, no sólo que las pruebas estén hechas (`CalibrarActivity.java:115`).
- Completar RF-CAL-43:
  - SHA-256 del ZIP;
  - A5 y deriva;
  - V al inicio, V mínima y V antes de cada `#S`;
  - PIN de fábrica sí/no;
  - lista de patrones y n (P9-B7);
  - estado de los 12 códigos, con "5: no calibrado, fábrica".
- Quitar la caída al OSCURO de toda la campaña cuando hay cola (`Anclas.java:78-82`).
- Anotar la colocación interrumpida (T-S09). Que un acta nueva cite las rechazadas del mismo equipo y
  día.
- Pruebas de actividad o, al menos, sacar la lógica de `resolverCorte`, `persistenciaInterna` y
  `aceptarInterno` a clases puras con prueba JVM. Hoy la afirmación del commit sobre esas tres cosas no
  tiene ninguna prueba que la respalde.
- `Version3612Test` compilable como prueba JVM pura: sacar `tramaG` de `OpsEquipo`.
- Salida del bloqueo de batería desde "Calibrar" (una lectura `9` a petición).

---

## 6. Contradicciones abiertas

| ID | Contradicción | Fuentes | Cierra |
| :--- | :--- | :--- | :--- |
| C-P11-1 | El b "sin PA-24, sólo verificar", frente al b escribible en el APK | `SPEC-Calibracion-V3.6.md:786`; `TablaCalibracion.java:86`; `CalibrarActivity.java:251-259` | P11-M5 |
| C-P11-2 | El commit dice "corte durante `#S`", "persistencia", "`#F` invalida" y "s_rep no se teclea", y ninguna prueba ejercita el código que lo hace | `e159fdf`; `Version3612Test.java:177-254` | P11-M7 y §5.3 |
| C-P11-3 | La SPEC y el TDD dicen "sin OSCURO sólo el código 1 se puede calibrar", y el 1 ya no es escribible. Tampoco nombran el 3, el 4 y el 6 en grado 1 | `SPEC-Calibracion-V3.6.md:790-791`; `TDD-V3.6.md:1245` (T-S06) | Agente de SPEC |
| C-P11-4 | T-S12 parte de "tras escribir el código 1", que la tabla ya no permite | `TDD-V3.6.md:1288` | Agente de SPEC: pasarla al 8 |
| C-P11-5 | Re-medida del 8: REFORM da "P43 o P34"; la SPEC y el APK, sólo P43 | REFORM:452; `SPEC-Calibracion-V3.6.md:785` | Diego |
| C-P11-6 | 1 y 2 "no se tocan hasta el banco" (REFORM) frente a "no se reescriben" (SPEC y APK) | REFORM:452; `SPEC-Calibracion-V3.6.md:781-782` | Diego, con la A5 del banco |
| C-P11-7 | La s_rep de la propia serie con K ≥ 3 está permitida (RF-CAL-38) pero no programada | `SPEC-Calibracion-V3.6.md:800`; `Remedida3611.java:60`; `Anclas.java:85-107` | Agente de app |

---

## 7. Cómo se ha comprobado

**Leído aquí, línea a línea:**

- `CalibrarActivity`, `Acta`, `OpsEquipo`, `TablaCalibracion`, `Remedida3611` y `Anclas`, completos;
- el diff de `e159fdf` en `AdminActivity`, `Campanas`, `ConexionActivity`, `Base` y `CampanaActivity`;
- `Sesion.reiniciar`, `ConexionActivity.estado`, `BancoCola.calibrable` y `Colocacion`;
- `guardarEeprom` y `calibracionIniciar` del firmware;
- la cola (filas del OSCURO, del 8 y del b);
- el anexo al acta de las 12:23.

**Dos subagentes, con encargos acotados.**

- **Pruebas y APK.**
  - Compiló y ejecutó las 120 pruebas con JDK 11.
  - Comprobó que `Version3612Test` no compila como prueba JVM pura.
  - Describió qué afirma cada una de las 12, e informó de que ninguna toca la capa de actividad.
  - Cotejó el APK con `aapt`, `dexdump`, las cadenas y el md5 de los assets.
- **Tabla y documentos.**
  - Cotejó RF-CAL-37 de la SPEC con `TablaCalibracion` y con REFORM.
  - Revisó PA-14 y PA-24, el ancla y los criterios de `Remedida3611`.

**Contraste con los subagentes.**

- Se han vuelto a abrir aquí, antes de usarlos:
  - `exigeConformidad` sin uso (Grep en todo el paquete);
  - la caída del ancla (`Anclas.java:78-82`);
  - los estáticos del oscuro (`CalibrarActivity.java:268-297`, `:489-493`);
  - la ausencia de `TablaCalibracion` en `AdminActivity`.
- **Un matiz de esta revisión sobre el subagente de pruebas.** Dice que reescribir un código CONFORME
  reinicia D-20. Es cierto, pero **no sirve para escapar de un NO CONFORME** dentro de "Calibrar": un
  código sin resolver va siempre a la re-medida (`CalibrarActivity.java:411-425`), y uno restaurado se
  salta (`:414-415`). La fuga real es la de Avanzado (P11-M4).
- **Un matiz sobre el caso 4, de esta revisión.** La curva escrita **sí** usa su ancla. Lo erróneo es la
  línea del acta, no el equipo.

**Lo que no se ha hecho:**

- no se ha medido nada;
- no se ha recompilado el APK;
- no se ha ejecutado ningún flujo de actividad;
- no se ha cotejado el certificado de los tipo I.
