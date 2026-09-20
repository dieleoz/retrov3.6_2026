# Comparativa funcional 3.6.17 → RTV 1.0.0 (rc3 / rc4)

**Nada de esto se ha probado en un teléfono ni contra un equipo.** Todo sale de leer el fuente de los
dos árboles y de comparar los dos commits. Es un **censo**, no un arreglo.

- **Origen:** `f5145ed` "App RTV 3.6.17…", rama `main` de `D:\IT\P_RetroVertical_V3.6`.
  APK `03_App_Movil/RTV-V3.6.17.apk`, md5 `fc016afab69142b347dc33e106f44606` — **comprobado con
  `md5sum` en este árbol**.
- **Destino:** `23c5fef` "RTV 1.0.0-rc4…", rama `rtv-1.0`, árbol `D:\IT\wt_rtv10`.
  APK `RTV-V1.0.0-rc4.apk`, md5 `a41fb09c74bd8f0b99500a1f29ea7b9d` — **comprobado**.
- **rc3** = `8b0fc9d`, `versionCode 10002`. Su APK **ya no está en el árbol** (`03_App_Movil/` sólo
  tiene los dos ficheros de la rc4), así que el md5 `40ffa4be…` **no se ha podido verificar aquí**.
- `rtv-1.0` sale de `main` por la mezcla `3dcaf41`, que trae `f5145ed` entera: la comparación
  `f5145ed..23c5fef` es limpia y no arrastra nada ajeno.
- El fuente de la app **no ha cambiado en `main` después de `f5145ed`**:
  `git diff --stat f5145ed..1247aae -- 03_App_Movil/` no devuelve ninguna línea.
- **El árbol `D:\IT\wt_rtv10` estaba en `23c5fef` y limpio** al empezar y al acabar
  (`git status --porcelain` vacío las dos veces). No he visto cambios bajo mis pies. **No he escrito
  nada allí.**

---

## 1. La respuesta corta: no se ha perdido nada

**Ninguna pantalla, ningún botón y ninguna opción de menú de la 3.6.17 desaparece en `rtv-1.0`.**
Verificado por dos caminos distintos, como pide `CLAUDE.md` §2 para un negativo:

1. `git diff --name-status f5145ed..23c5fef` no tiene **ni una sola línea `D`**: los 24 ficheros
   nuevos son todos `A`, el resto `M`.
2. Recuento de llamadas a `boton(` por pantalla, en el blob de cada commit:

   | Pantalla | 3.6.17 | rc4 |
   | :--- | :---: | :---: |
   | `ConexionActivity` (el menú) | 13 | 13 |
   | `BancoActivity` | 7 | 7 |
   | `CalibrarActivity` | 8 | 8 |
   | `AdminActivity` | 13 | 13 |
   | `CampanaActivity` | 11 | 11 |
   | `MedidaActivity` | 3 | 3 |
   | `PruebasActivity` | 5 | 5 |
   | `BotonesActivity` | 4 | 4 |

   El `onCreate` de `ConexionActivity` es **idéntico línea a línea** entre los dos commits salvo el
   rótulo de cabecera y una llamada añadida (`PerfilesApp.iniciar(this)`,
   `ConexionActivity.java:95`).

"Tomar muestras", "Guardar / Compartir", "Calibrar", el selector de banco con su cambio de cola,
"Rehacer patrón…", el acta con Aceptar/Rechazar, "Alta / Cambiar serie", el informe final y los ZIP
siguen todos donde estaban, con el mismo nombre.

**Lo único que cambia es en qué condiciones se habilitan.** Y ahí sí hay materia.

---

## 2. La advertencia que cambia el encuadre de la queja

> *"cambiamos y ésta no trae las cosas que se habían dejado en la .16 o .17"*

La comparación que hay detrás de esa frase es **3.6.17 delante de un V3** contra **rc3 delante de un
V4.6**. No son el mismo experimento.

**Con un V4.6 delante, la 3.6.17 no ofrece absolutamente nada.** No es una impresión: en el fuente de
la 3.6.17 la cadena `"4.6"` **no aparece ni una vez** en
`03_App_Movil/RetroV36/app/src/main/java/` (`git grep -c '4\.6' f5145ed -- …` no devuelve ninguna
línea). Su detección (`Pruebas.detectar`, blob de `f5145ed`) reconoce `#V#` sólo si la versión es
`"3.6"`; cualquier otra cae en la rama

```java
if (iv != null) {
    s.version = Sesion.Version.DESCONOCIDA;
    d.append("Responde a #V# con versión ").append(iv.version).append(": no es la del contrato.");
```

y a partir de ahí `versionMedible()` (`Sesion.java` de `f5145ed`) es `false`, las pruebas 3-6 salen
**FALLO**, el resultado es **NO APTO** y el banco no mide.

De modo que, delante de un V4.6, **la rc3 no quita nada de la 3.6.17: añade todo lo que hay.** Lo que
falta, falta respecto de *lo que la 3.6.17 hacía con un V3.6*, que es otro equipo.

---

## 3. Tipo B — pérdidas reales

**Ninguna en el sentido estricto** (una opción que existía y con cualquier equipo ya no se alcanza).
Lo más parecido a una pérdida son dos cosas, y ninguna es una función de medida:

### B-1. No se puede volver atrás a la 3.6.17 sin desinstalar, y desinstalar borra la campaña

`applicationId` sigue siendo `com.dpi.retrov36` (`app/build.gradle:11`, sin cambio), pero el
`versionCode` pasa de `3617` a `10003` (`app/build.gradle:16-17`). Android **no instala un
`versionCode` menor encima de otro mayor**: con la rc3 (10002) o la rc4 (10003) ya puestas, volver a
la 3.6.17 (3617) exige **desinstalar**, y con `android:allowBackup="false"`
(`AndroidManifest.xml:19`) eso se lleva por delante la campaña del teléfono.

Es exactamente el aviso A-07 que la revisión P16 dio para la rc1
(`REVISION-Arquitectura-P16-V3.6.md`, §1 punto 6), y sigue vigente. **No es reversible sin perder
datos: si hay que volver, primero "Guardar / Compartir".**

### B-2. La "frase de Diego" para firmar ya no existe

La 3.6.17 ofrecía firmar "Cerrar sin restaurar" y "Liberar" con el PIN **o** con una frase registrada
(`FRASE-DIEGO` de `decisiones.csv`). La rc3 la quita entera y deja sólo el PIN de administrador, y
además pide el **nombre** de quien firma
(`FlujoCalibracion.java`, `firma(String,String)`, sustituye a `firmaDeDiego(String)`;
`CalibrarActivity.java`, `campoNombre()` y `cerrarSinRestaurar()`).

**Se pierde una salida sobre el papel, no en la práctica**: la fila `FRASE-DIEGO` **nunca ha viajado
en el asset** — es el defecto A-04 de la P16 — y el propio comentario del código nuevo lo dice. El
camino del PIN, que era el único que funcionaba, sigue ahí. **Lo llamo B porque el texto de los
botones y la `GUIA-OPERADOR.md` cambian de significado**, no porque se pierda capacidad.

### B-3 (no es pérdida, es desajuste de documentación)

`03_App_Movil/GUIA-OPERADOR.md` **no se ha tocado** en `rtv-1.0`
(`git diff --stat f5145ed..23c5fef -- 03_App_Movil/GUIA-OPERADOR.md` no devuelve nada). Sigue
titulada "app RTV 3.6.17", sigue diciendo *"La app pide una «firma de Diego»"* —que ya no ocurre— y
**no menciona ninguna de las puertas por familia de firmware**. El operador que lea esa guía con la
rc3 delante verá justamente lo que Diego describe: cosas que la guía promete y la app no ofrece.

---

## 4. Tipo A — cerrado por familia de firmware

Todas dependen de tres preguntas nuevas al protocolo detectado: `administra()`, `calibra()` y
`mideBanco()` (`Protocolo.java:85,88,94`). Con un **V3.6** las tres son `true`
(`ProtocoloV36.java:121-143`): **delante de un V3.6 no se cierra absolutamente nada.**

| Lo que el operador no ve | Dónde se decide | V3.6 | V4.6 | V3 2020 | V4 original |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **"Calibrar"** (botón grande) | `ConexionActivity.java`, `btnGrabar.setEnabled(con && (pr == null \|\| pr.calibra()))` | sí | **no** | **no** | **no** |
| **"4. Calibrar este equipo"** | mismo, `btnCalibrar` | sí | **no** | **no** | **no** |
| **"Modo administrador"** | mismo, `btnAdmin` | sí | **no** | **no** | **no** |
| **"Botones de pantalla"** | mismo, `btnBotones.setEnabled(con && s.administra())` | sí | **sí (gana)** | no | no |
| **"Tomar muestras"** | mismo, `btnMuestras`, condición `pr.mideBanco()` | sí | **sí** | sí | **no** |
| **"3. Campaña de calibración"** | mismo, `btnCampana.setEnabled(banco)` | sí | sí | sí | **no** |
| **Banco completo P1-P132 y Representativo v2** | `BancoActivity.elegirBanco()`, filtro `t.esV46() == v46` | sí | **no** | sí | — |
| **Banco representativo V4.6** | mismo filtro | **no** | sí | **no** | — |
| **Pruebas 3-6** | `Pruebas.java`, `if (!proto.daX() \|\| !proto.permitida("1"))` → `NO_APLICA` | sí | **NO_APLICA** | sí | **NO_APLICA** |
| **"Línea base" (T-B03/T-B09)** | `PruebasActivity.java`, `!s.protocolo.permitida("1")` | sí | **no** | sí | **no** |

Dos matices que importan:

- **Los botones deshabilitados dicen por qué.** El rótulo se reescribe con el motivo:
  `btnCalibrar.setText("4. Calibrar este equipo" + (calibra ? "" : "\n(" + p.motivoNoCalibra() + ")"))`.
  Con un V4.6 el motivo es `"Calibración V4.6: próxima versión"` (`ProtocoloV46.java:13`). Eso está
  bien hecho, pero **no aparece en ninguna guía**, y un botón gris con letra pequeña debajo se lee
  como "esto ya no está".
- **Con un V3 2020 (SLV-002) tres botones pasan de habilitados a grises.** En la 3.6.17
  `btnGrabar`, `btnCalibrar` y `btnAdmin` sólo pedían `con` (estar conectado), y el rechazo llegaba
  al entrar. En `rtv-1.0` se deshabilitan de entrada con el motivo
  `"Firmware V3 de 2020: no tiene órdenes de calibración."` (`ProtocoloV2020.java:59`). **Es más
  honesto y no se pierde capacidad**, pero si la comparación de Diego fue sobre SLV-002, esto es
  literalmente "cosas que ya no están".

---

## 5. El nudo: la puerta de calibración se lleva por delante la administración

Esto no es ni A ni B limpio, y es lo que más conviene mirar.

`ProtocoloV46.calibra()` es `calibracion && mideBanco()` (`ProtocoloV46.java:135-137`), y
`calibracion` llega `false` por el constructor de un argumento (`ProtocoloV46.java:23-25`).
**Los dos únicos sitios de producción que construyen el protocolo usan ese constructor**:
`Deteccion.java:112` y `FlujoCalibracion.java:269`. El constructor con `calibracion = true` sólo
aparece en tests (`FlujoCalibracionTest.java:1407`, `RupturaRtv10Test.java:363`). Verificado con
`grep -rn "new ProtocoloV46("` sobre todo el árbol: **con un V4.6 real, `calibra()` es siempre
`false`, y ningún dato de `firmwares.csv` puede cambiarlo.**

La consecuencia pasa de la calibración a la administración:

- `ConexionActivity`: `btnAdmin.setEnabled(con && calibra)` → **la pantalla "Modo administrador" es
  inalcanzable** con un V4.6.
- `AdminActivity.refrescar()`: `boolean v36 = s.administra() && s.protocolo.calibra();` y
  `btnEntrar.setEnabled(v36 && …)` → **aunque se llegara, no se puede entrar.**

Y dentro de esa pantalla, tras `btnEntrar`, viven cosas que **no son calibración**:

- `btnSerie = boton("Alta / Cambiar serie (#SN, verificada con #GN)", …)` (`AdminActivity.java:156`)
- `btnFT`, "Restaurar temperatura de fábrica (#FT#)"
- "Volver a fábrica" (`#F,k#`, `#F,*#`)
- "Cambiar PIN" (`#P#`)

`#SN` es una orden de **administración**, no de calibración: `ProtocoloV46.administra()` devuelve
`true` (`ProtocoloV46.java:130-132`). La puerta que se quiso cerrar era `calibra()`, y ha cerrado
también la única vía de la app para **darle serie a un equipo que responde `#GN,NONE#`**.

**Sobre la sospecha de Diego, y aquí le contradigo:** `#GN,NONE#` **no impide abrir campaña ni ver
el banco**. `Sesion.serie()` descarta explícitamente el `NONE` y cae a la serie declarada
(nombre Bluetooth o tecleada) — el comentario del código lo dice con todas las letras: *"recien
grabada (#GN,NONE#) vale la declarada … para poder medir el banco antes de dar de alta la serie"*
(`Sesion.java`, `serie()`). `Calibracion.NONE` es `"NONE"` (`Calibracion.java:18`) y
`serieConocida()` lo salta igual. El diálogo de "escriba la serie" de `CampanaActivity.abrir()` es
**idéntico byte a byte** al de la 3.6.17 (comparados los dos blobs). Lo que sí impide `#GN,NONE#` es
**grabar la serie de verdad**, porque para eso hace falta `#SN` y `#SN` está detrás de la puerta de
calibración.

---

## 6. ¿Cambia algo la rc4?

**No, en nada que el operador vea como una función perdida o recuperada.** El diff `8b0fc9d..23c5fef`
toca cinco ficheros: `README.md`, `build.gradle` (10002→10003, `1.0.0-rc3`→`1.0.0-rc4`),
`FlujoCalibracion.java`, `FlujoCalibracionTest.java` y `FirmaActaTest.java` (nuevo).

El único cambio de comportamiento es **el texto del acta**: en la rc3 los cuatro puntos que
incrustaban la firma le anteponían "firmado por" y el acta decía
`firmado por Firmado por "Ana Ruiz", ITVIAL SAS, …`. La rc4 lo lee una sola vez
(`FlujoCalibracion.firmaDe(String,String)`, pública y en un solo sitio) y la fecha pasa de recortar
`ahoraIso()` a `reloj.hoy()`, el mismo del que sale la que se graba con `#SC`.

**Ninguna puerta de las de §4 se mueve en la rc4.** Verificado: el diff rc3→rc4 no toca
`ConexionActivity`, `BancoActivity`, `AdminActivity`, `Pruebas`, `Protocolo*` ni `firmwares.csv`.

---

## 7. Lo que hay que medir, no razonar

1. Si el V4.6 de campo responde `#V#` con **`4.6` exacto**. Si responde `4.6A`, `4.6B` o cualquier
   otra cosa, cae en `C-U03` (`Deteccion.java:117-120`), `r.protocolo` queda `null` y **entonces sí
   no hay nada**: ni banco, ni pruebas, ni medida. Es el único camino que reproduce de verdad "no
   trae nada".
2. Si el nombre Bluetooth del equipo trae la serie tras un `_`. Si no la trae y el operador cancela
   el diálogo de "Serie del equipo", `BancoActivity.cargar()` se queda en *"Conecte con el equipo (y,
   si hace falta, dé su serie en Campaña) antes de medir el banco"* y el banco **parece** no existir.
   Es igual en la 3.6.17, pero con un V4.6 es la primera vez que alguien llega hasta ahí.
3. Si el md5 de la rc3 que Diego tiene instalada es `40ffa4be…`. **No se ha podido comprobar**: ese
   APK ya no está en el árbol.

---

## 8. Resumen para quien decida

- **Perdido de verdad: nada de medida.** Lo único irreversible es que **no se puede volver a la
  3.6.17 sin desinstalar** (B-1), y desinstalar borra la campaña.
- **Cerrado por familia: la calibración entera con un V4.6**, más el banco completo y el
  representativo v2, más las pruebas 3-6 y la línea base. Todo decidido y todo con su motivo escrito
  en el botón.
- **El agujero que conviene mirar** es que la puerta de `calibra()` cierra también `#SN`, `#FT`,
  `#F,k#` y `#P#`, que son administración y no calibración (§5).
- **La `GUIA-OPERADOR.md` sigue siendo la de la 3.6.17** y ya no describe la app que Diego tiene en
  la mano (B-3). Corregirla cuesta poco y explica la mitad de la queja.
- **La rc4 no cambia ninguna de estas respuestas.**
