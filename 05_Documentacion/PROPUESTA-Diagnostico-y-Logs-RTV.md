# Propuesta: diagnóstico y registros de la app RTV V3.6

**Estado: propuesta sin construir y sin medir.** Sale de leer el código fuente de las dos apps. No se ha
probado nada contra el equipo ni en el teléfono. Las cifras de tamaño de §2.3 salen de **un solo** registro
real, de la app 3.6.2. Todos los topes quedan abiertos hasta que se midan con la 3.6.12.

- **Alcance.** Todo lo que se propone es para la app del retrorreflectómetro **Vertical**, RTV V3.6. La app
  de semáforos sirve sólo de referencia: de ella no se copia código y no se construye nada para ella.
- **RTV revisada:** commit `e159fdf` (app 3.6.12, `versionCode 3612`, `app/build.gradle:14-15`). Salvo que
  se indique otra ruta, los ficheros `.java` están en
  `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.
- **Referencia revisada:** `D:\@Proyect\Controladora_Semaforos 2`, commit `ff921e0` (16-sep-2026), app
  `05_Funcional/App_Semaforo/`. Es JavaScript dentro de Capacitor (un WebView de Android). Las rutas que
  empiezan por `REF/` se refieren a esa carpeta. `app.js`, `www/app.js` y
  `android/app/src/main/assets/public/app.js` son hoy byte a byte iguales (md5 `cc2d3f93…`), y lo mismo
  pasa con `js/depuracion.js`. Se cita la copia de la raíz.
- **Sin cambios de código** en ninguno de los dos proyectos.
- **Datos privados.** La referencia lleva en el fuente un PIN por defecto (`REF/js/config.js:6`). Aquí no
  se reproduce su valor.

---

## 1. Tabla comparativa

| Aspecto | Referencia (semáforos) | RTV V3.6 | ¿Le falta algo a RTV? |
| :--- | :--- | :--- | :--- |
| **Tecnología** | JS en WebView. Exporta con `Blob` y un enlace de descarga (`REF/app.js:1310-1318`) | Java nativo. Comparte con `ACTION_SEND` y `FileProvider` (`Base.java:208-229`, `:232-283`) | — |
| **Registro de tramas** | Guarda las dos direcciones sin interpretar, en **memoria** y sin persistir (`REF/js/depuracion.js:83-150`). Tope de 300 tramas (`:92`) y 400 caracteres por línea (`:98`) | Un fichero por conexión en `files/registros/`, con TX y RX en hex y en ascii (`Registro.java:54-100`). Vuelca al disco línea a línea (`:112-119`) y usa un reloj monótono (`:33-34`) | No le falta nada: **RTV va por delante**, porque el registro sobrevive a un cierre de la app |
| **Diario orden → respuesta** | `DiarioOrdenes`: 120 entradas en memoria con la terna orden, respuesta y efecto (`REF/js/depuracion.js:446-540`) | Cada petición deja la nota `peticion X -> desenlace en ms` en el mismo fichero (`Cliente.java:165-166`). Tras 3 timeouts seguidos anota un aviso (`:168-171`) | No. El "efecto" de la referencia se deduce de un `$STATUS` periódico, y el V3.6 no emite ninguno (ver §3) |
| **Línea de tiempo del enlace, persistente** | `RegistroEnlace` en `localStorage`: 400 anotaciones. Registra caídas, regresos, rechazos y huecos, y no rellena con ceros lo que no se midió (`REF/js/registro_enlace.js:22-38`, `:58`, `:85`) | Anota la pérdida de conexión, pero sólo dentro del registro de esa conexión (`EnlaceSerie.java:231`). **Los intentos fallidos no llegan a ningún fichero**: el motivo sólo sale en pantalla (`EnlaceSerie.java:176-181`), y `Registro.abrir` se llama sólo cuando la conexión se logra (`:188`) | **Sí**: RF-DIAG-06 |
| **Niveles o clases** | Veredictos `ACEPTADA`, `RECHAZADA` y `ENVIADA` con su motivo (`REF/js/depuracion.js:120-144`). Clases `MUESTRA`, `CAIDA`, `REGRESO`, `EVENTO`, `ALARMA` y `RECHAZO` (`REF/js/registro_enlace.js:85`) | Separa TX y RX de las notas, que empiezan por `#` (`Registro.java:29-31`). Los desenlaces son `VALIDA`, `TIMEOUT` e `INESPERADA` (`Receptor.java:17`) | En parte: faltan contadores por desenlace (RF-DIAG-09) |
| **Rotación y tope** | Topes duros. Lo que se descarta se cuenta y se declara al exportar (`REF/js/depuracion.js:74-76`, `:352-355`; `REF/js/registro_enlace.js:35-38`, `:258-262`) | **No hay ninguno.** Ningún fichero de la app llama a `delete()`. En `files/registros/` se acumulan los registros, los CSV y también los ZIP de campaña (`Campanas.java:383-391`, `Sesion.java:266-276`) | **Sí**: RF-DIAG-04 |
| **Almacenamiento que falla** | Lo declara: `disponible` y `motivoNoDisponible` salen en pantalla (`REF/js/registro_enlace.js:88-93`, `:146-162`) | **Deja de escribir sin avisar**: si no se crea la carpeta (`Registro.java:58-59`), si falla la apertura (`:81-83`) o si falla una escritura (`:120-124`) | **Sí**: RF-DIAG-05 |
| **Registro de errores (crash log)** | No existe. No hay `onerror` ni `unhandledrejection`: se buscó con `grep` y con otra herramienta de búsqueda, sin resultados | No existe. No hay `UncaughtExceptionHandler`, con la misma doble búsqueda. Las excepciones capturadas acaban en un `Toast` que no deja rastro (p. ej. `Base.java:213`) | **Sí**: RF-DIAG-07. Aquí la referencia tampoco sirve de modelo |
| **Compartir** | Cuatro salidas separadas: tramas `.txt`, diario `.txt`, bitácora `.csv` y eventos `.csv`. Además copia al portapapeles con `navigator.share` (`REF/app.js:1320-1345`) y abre un enlace a `api.whatsapp.com` (`:6007`) | "Compartir registro y datos": envía con `ACTION_SEND_MULTIPLE` el registro **actual** y los CSV de la sesión (`Base.java:232-283`). Hay botón en `ConexionActivity.java:53`, `PruebasActivity.java:53`, `MedidaActivity.java:59` y `BotonesActivity.java:55`. El ZIP de campaña lleva md5 y sha256 (`Base.java:208-229`) y deja copia en `Download/RTV/` (`Campanas.java:160-188`). `Registro.intentCompartir` no lo llama nadie (`Registro.java:148`) | En parte: sólo se comparte el **último** registro, sin copia en Download y sin informe (RF-DIAG-01) |
| **Formato** | Texto plano elegido a propósito: una trama lleva comas dentro (`REF/js/depuracion.js:320-326`) | Texto con separador `;` y ascii saneado (`Registro.java:29`, `:79-80`), CSV y ZIP | — |
| **Datos del teléfono y de la app** | La cabecera del export describe el **equipo** (cruce, nodo, serie, modo, reloj, enlace), no el teléfono (`REF/app.js:1283-1296`). La versión de la app está escrita a mano (`:1284`) | La cabecera del registro lleva la fecha, el fabricante y el modelo del móvil, la versión de Android y el API, el dispositivo y la versión de la app tomada de `BuildConfig` (`Registro.java:71-78`). `resumen.txt` del ZIP añade el firmware y la calibración (`Campanas.java:394-400`) | En parte: faltan los permisos, el estado del Bluetooth y el espacio libre (RF-DIAG-02) |
| **Versión del firmware** | Botón "Consultar versión", que no toca nada (`REF/app.js:5705-5712`, `REF/index.html:714`) | `#V#`, `#GC#` y `#GN#` dentro de la prueba 2 (`Pruebas.java:256-286`). Se pintan en la cabecera (`Base.java:88`) y van a `Sesion` (`Sesion.java:42-51`) | No como dato. Falta dejarlo en un informe con su hora de lectura (RF-DIAG-02) |
| **Batería** | Va en el reporte de WhatsApp, o sale "no medida" (`REF/app.js:5998-5999`) | La orden `9` se anota en la campaña y en el acta (`OpsEquipo.java:73-83`). La última lectura queda en `Campana.ultimaBateriaN` (`Campana.java:155`, `:345`). **No va en "Compartir registro y datos"** | **Sí**: RF-DIAG-02 |
| **Última prueba** | No hay pruebas automáticas; sólo una prueba de focos manual (`REF/index.html:724-727`) | Seis pruebas con veredicto APTO o NO APTO (`Pruebas.java:53-58`) que arrancan solas al conectar (`ConexionActivity.java:96`). El detalle sólo se guarda **si hay campaña abierta** (`Campanas.java:139-146`). Al registro va una línea de resumen (`Pruebas.java:238`) y al envío, el resumen (`Base.java:275`) | **Sí**: RF-DIAG-03 |
| **Pantalla de diagnóstico** | Ventana del poste: contadores RX, OK y RUIDO con su variación y la ventana en segundos, más los 12 últimos movimientos (`REF/index.html:217-247`, `REF/app.js:2878-2960`). No hay umbrales: la decisión está razonada en `REF/js/diagnostico_enlace.js:49-55` | Franja de estado en cada pantalla: enlace, firmware y APTO/NO APTO (`Base.java:84-94`) | **Sí**: RF-DIAG-08 |
| **Informe de un toque** | Reporte de WhatsApp con el estado y los 5 últimos eventos (`REF/app.js:5987-6008`) | No hay un informe único | **Sí**: RF-DIAG-01 |
| **PIN en los registros** | Se tapa al anotar y se conserva la longitud (`REF/js/depuracion.js:181-206`) | `Hex.ocultarPin` en TX (`Hex.java:72-83`, llamado en `Registro.java:96`) y una expresión regular sobre las notas (`Registro.java:103`) | No |

**En resumen:** RTV ya cubre la mitad dura. Registra las dos direcciones, en disco y sin perder líneas.
Protege el PIN. Genera el ZIP de campaña con huellas y copia fuera de la app. Pone la versión de la app en
todas partes. Le faltan cinco cosas: un **informe de soporte único**, **topes y rotación**, **avisar cuando
el registro deja de escribirse**, un **registro de errores** y **dejar rastro de los fallos de conexión**.

---

## 2. Propuesta: "Diagnóstico y soporte" en RTV

Clasificación: **núcleo** es lo que hace falta para diagnosticar a distancia un equipo de campo. **Mejora**
es lo que ayuda, pero no bloquea.

### 2.1 Reglas que valen para todos los requisitos

1. **Por defecto no se envía ninguna trama al equipo.** El informe usa los valores ya leídos, cada uno con
   su hora de lectura. Motivos:
   - La orden `9` dispara la lámpara y no puede ir en mitad de una serie (`Bateria.java:13`, RF-APP-43).
   - En un V3 2020, `#V#`, `9` y `6` disparan una medida (`Pruebas.java:303-304`).
   - A un equipo sin identificar nunca se le manda `e` (`Pruebas.java:301-303`).
2. **Un dato que no se leyó no se rellena.** Se escribe "no leído en esta conexión", no un cero ni la
   palabra `null`. Es la regla de `REF/js/registro_enlace.js:29-33`, y en RTV ya la sigue
   `Sesion.datosCalibracion()` con su `?` (`Sesion.java:193-194`).
3. **Lo que se recorta se cuenta y se dice**, como hace `REF/js/depuracion.js:74-76`.
4. **No se inventan umbrales.** La pantalla y el informe muestran cifras y edades, no un semáforo de "bien"
   o "mal". Los únicos juicios son los que ya existen en el código: `Bateria.AVISO_N = 19`
   (`Bateria.java:17`) y `TIMEOUTS_PARA_CONSEJO = 3` (`Cliente.java:47`).
5. **Nada sale sin el PIN tapado.** Se reutiliza `Hex.ocultarPin` y la expresión regular de
   `Registro.java:103`.
6. **Nada en la app pide INTERNET** (`AndroidManifest.xml:5-8`). Los ficheros los envía otra aplicación.

### 2.2 El ZIP de soporte

**RF-DIAG-01 (núcleo). Botón "Diagnóstico y soporte" que genera un ZIP.**
Va en la pantalla de conexión, junto a "Compartir registro y datos" (`ConexionActivity.java:53`), y
funciona **también sin conexión**. Hace cuatro cosas:
- Escribe `soporte_<serie|sin-serie>_<aaaammdd_hhmmss>_app<versionName>.zip` en `files/registros/`, la
  única ruta que publica el `FileProvider` (`res/xml/ficheros.xml`).
- Calcula su md5 y su sha256 con `Resumen.hex`, como hace `Campanas.java:366-367`.
- Deja una copia en `Download/RTV/` reutilizando `Campanas.copiarADescargas` (`Campanas.java:160-188`).
- Lo comparte con `ACTION_SEND`, igual que `compartirZip` (`Base.java:208-229`).

El ZIP contiene:

| Entrada | Origen |
| :--- | :--- |
| `informe.txt` | RF-DIAG-02 |
| `tramas/rtv36_*.txt` | **Todos** los registros que queden tras la rotación (RF-DIAG-04), no sólo el actual |
| `datos/*.csv` | Los CSV de la sesión: medidas, botones, coeficientes y línea base (`Base.java:236-250`) |
| `campana/` | El diario, `pruebas_*.txt` y las actas de la campaña abierta, **copiados en modo lectura** |
| `enlace.log` | RF-DIAG-06 |
| `errores/` | RF-DIAG-07 |

La restricción clave es que **el ZIP de soporte no es una exportación de campaña**. No llama a
`Campanas.exportarConHuellas`, porque esa función escribe la línea `EXPORTA` en el diario
(`Campanas.java:370`) y cambia la cuenta de series sin exportar, que es la que decide el aviso de "no
desinstale" (`ConexionActivity.java:121-128`).

*Aceptación:*
- Con el Bluetooth apagado y sin equipo, el botón genera el ZIP y ofrece compartirlo.
- El registro de tramas no gana **ninguna** línea `TX` al pulsarlo.
- El diario de la campaña queda idéntico byte a byte antes y después. El número de series sin exportar no
  cambia.
- `grep -E "#[LP],[0-9]"` sobre el ZIP descomprimido no devuelve nada.
- El md5 que muestra la app coincide con el de la copia de `Download/RTV/`.

**RF-DIAG-02 (núcleo). `informe.txt`: lo que hace falta para leer el ZIP sin llamar a nadie.**
Texto plano con una sección por tema. Cada dato lleva su hora de lectura, o "no leído en esta conexión":

- **App:** `APPLICATION_ID`, `versionName` y `versionCode` de `BuildConfig`, como en `Registro.java:71`.
- **Firmware:**
  - `Sesion.firmware()` (`Sesion.java:166-177`): versión, fecha, `CAL` o `DEF` y máscara de `#V#`.
  - `#GC#`: fecha de calibración, vencimiento y estado.
  - `#GN#`: serie grabada en el equipo.
  - Todo ello con `Sesion.datosCalibracion()` (`Sesion.java:184-198`).
  - Si el firmware no tiene `#GN#` ni `#GC#`, se copia literal la frase de `Sesion.java:189`.
- **Batería:** la última `n` y su texto de `Bateria.interpretar`, con la fecha. Sale de la campaña
  (`Campana.java:155`, `:343-345`). Si no hay lectura, "sin lectura de 9 en esta campaña".
- **Última prueba:** RF-DIAG-03.
- **Campaña y acta:**
  - Fichero de la campaña.
  - Series totales y series sin exportar.
  - Última exportación, con sus huellas (`Campana.java:314-317`).
  - Si hay acta en curso (`Sesion.calibrando()`, `Sesion.java:80-83`) y cuál es su último paso.
- **Teléfono:** `Build.MANUFACTURER`, `MODEL`, `VERSION.RELEASE` y `SDK_INT`. También el espacio libre de
  `getFilesDir()`.
- **Permisos:** `ACCESS_FINE_LOCATION` y, con API < 29, `WRITE_EXTERNAL_STORAGE`, cada uno concedido o
  denegado. Son los mismos que pide `ConexionActivity.java:130-146`.
- **Bluetooth:** si existe adaptador y si está encendido (los mismos casos que `ConexionActivity.java:155-165`),
  el número de dispositivos emparejados, y si hay conexión, con nombre y MAC.
- **Enlace:** `getTextoEstado()`, `msDesdeUltimoEnvio()` (`EnlaceSerie.java:94`), el último RX
  (`Cliente.java:41`) y los timeouts seguidos (`Cliente.java:180`).
- **Registros:** número de ficheros, bytes totales y lo recortado por RF-DIAG-04. Si el registro está
  deshabilitado, el motivo que da RF-DIAG-05.

*Aceptación:*
- En el simulador, con un V3.6 identificado: `informe.txt` trae el `#V#`, el `#GC#` y el `#GN#` que están en
  el registro de tramas de esa conexión, con la hora de la línea `RX` correspondiente.
- Con un V3 2020: dice "no disponibles en V3 2020" y no inventa ninguna serie.
- Si se niega el permiso de ubicación, sale "denegado".

**RF-DIAG-03 (núcleo). La última prueba se guarda siempre.**
Hoy `Campanas.anotarPruebas` no hace nada si no hay campaña abierta (`Campanas.java:140-142`), así que el
detalle de las seis pruebas se pierde al cerrar la app. La propuesta: `Pruebas.terminar`
(`Pruebas.java:234-250`) escribe además el texto completo en `files/registros/pruebas_ultima.txt`
(sobrescrito) y en el registro de tramas, como nota `#`. El ZIP de soporte lo incluye.

*Aceptación:*
- Sin campaña abierta, pasar las pruebas, matar la app y generar el ZIP de soporte: `pruebas_ultima.txt`
  trae las 6 pruebas con su detalle y el mismo veredicto que se vio en pantalla.

**Refresco opcional, dentro de RF-DIAG-02.** Un botón aparte, "Releer del equipo", manda `#V#`, `#GC#` y
`#GN#` sólo si `Sesion.version == V36`. Manda `9` sólo si no hay ninguna serie en curso. Antes pide
confirmación con el texto "La orden 9 enciende la lámpara". Sin conexión no aparece.
*Aceptación:*
- Con un V3 2020 o un equipo sin identificar, el botón está deshabilitado y el registro gana 0 `TX`.
- Con un V3.6, el registro gana exactamente 3 o 4 `TX`, y ninguno es `e`.

### 2.3 Rotación y tamaño máximo

**Dato de partida (medido, una sola muestra).** Un registro real de la app 3.6.2, con el equipo SLV-002 y
un teléfono HONOR con Android 15, ocupa **53 838 bytes en 961 s** de medida continua, unos 3,4 kB por
minuto. El fichero está en `07 pruebas/19092026_0900/p29_p24_p23_p5/rtv36_20260919_090703.txt` y no está
versionado. Un ZIP de campaña ocupa entre 50 y 81 kB (`06_Calibracion/SLV-002/campanas/`). **Hay que
repetir la medida con la 3.6.12** antes de fijar los topes.

**RF-DIAG-04 (núcleo). Tope por fichero y tope total, con recorte declarado.**
- **Por fichero: 5 MB**, unas 24 h de medida continua al ritmo medido. Al superarlo, `Registro` abre
  `rtv36_<sello>_<n>.txt`, con la misma cabecera y la nota `# continuación de <anterior>`. Hoy no hay
  límite.
- **Total de `files/registros/`: 50 MB.** Se borra primero lo más antiguo, con dos excepciones que no se
  borran nunca:
  1. los registros que caen dentro de una campaña con series sin exportar, es decir, el mismo criterio de
     inclusión que usa el ZIP: `lastModified >= inicioMs - 60 000` (`Campanas.java:419-420`);
  2. el último ZIP de campaña y el último ZIP de soporte.

  Si lo protegido ya supera el tope, **no se borra nada** y se avisa: "Registros: 62 MB, exporte la campaña".
- **Recuento.** Cada borrado se suma a un contador persistente (`registros/rotacion.txt`: fecha, fichero y
  bytes). El contador sale en `informe.txt`. Es la regla de `REF/js/registro_enlace.js:35-38`.
- La rotación corre al arrancar y al abrir cada registro. No corre nunca durante una serie.

*Aceptación:*
- Con 60 MB de registros antiguos sin campaña: al arrancar se baja a ≤ 50 MB, y `rotacion.txt` lista cada
  fichero borrado.
- Con 60 MB que pertenecen a una campaña con series sin exportar: no se borra ninguno y sale el aviso.
- Un registro de más de 5 MB se parte en dos sin perder ninguna línea: el número de líneas `TX`/`RX` suma
  lo mismo.

**RF-DIAG-05 (núcleo). Un registro que no se escribe se dice.**
`Registro` pasa a tener `disponible` y `motivo`. Se ponen a falso en `Registro.java:58-59`, `:81-83` y
`:120-124`. La franja de estado (`Base.java:84-94`) muestra en rojo "REGISTRO DETENIDO: <motivo>", y el
informe lo recoge.

*Aceptación:*
- Con el almacenamiento lleno (simulado con un `Writer` que lanza una `IOException`), la franja muestra
  "REGISTRO DETENIDO" antes de la siguiente petición.
- Hoy la app sigue midiendo sin registro y sin decirlo.

**RF-DIAG-06 (núcleo). `enlace.log`: los intentos de conexión, persistentes y fuera de cada registro.**
Es un fichero único que se abre al arrancar y no depende de que haya conexión. Una línea por suceso, con la
hora de pared:
- intento de conexión;
- fallo, con los dos motivos, el del socket inseguro y el del seguro (`EnlaceSerie.java:176-181`), que hoy
  sólo se ven en pantalla;
- conexión lograda, con el tipo de socket (`:189-190`);
- conexión perdida (`:231`);
- desconexión pedida (`:272`);
- el aviso de 3 timeouts (`Cliente.java:171`).

Tope de 2000 líneas. Al superarlo se descartan las más antiguas y se cuentan. Es la versión RTV de la
bitácora de la referencia (`REF/js/registro_enlace.js`), sin muestras periódicas, porque el V3.6 no emite
telemetría.

*Aceptación:*
- Tres intentos fallidos contra un equipo apagado y un cuarto que conecta dejan 4 líneas de intento, 3 de
  fallo con motivos y 1 de conexión.
- Hoy no queda ninguna de las tres primeras.

### 2.4 Registro de errores y excepciones

**RF-DIAG-07 (núcleo). Crash log y errores capturados.**
- **Errores no capturados.** Una clase `Application` registra `Thread.setDefaultUncaughtExceptionHandler`.
  Escribe `errores/crash_<sello>.txt` con: versión de la app, hilo, traza completa, la actividad en
  primer plano, `Sesion.identidad()` y las **últimas 50 líneas** del registro de tramas actual. Después
  **encadena** el manejador anterior, para que Android cierre la app como siempre.
- **Errores capturados.** Los `catch` que hoy acaban en un `aviso()` o en un `alerta()` añaden una línea a
  `errores/errores.log` (fecha, clase, mensaje). Por ejemplo, `Base.java:212-214`, `:268-270` y
  `ConexionActivity.java:165-166`. `alerta()` ya hace algo parecido con la actividad destruida
  (`Base.java:190-193`).
- **Al arrancar**, si hay un `crash_*.txt` que no se ha compartido, `ConexionActivity` avisa: "La app se
  cerró por un error el <fecha>. Use Diagnóstico y soporte para enviarlo".
- **Tope:** los 20 `crash_*.txt` más recientes y 500 líneas de `errores.log`. Lo descartado se cuenta.
- **Privacidad:** el PIN va tapado, porque las 50 líneas pasan por el mismo filtro que el registro.

*Aceptación:*
- En la compilación de depuración, un botón oculto lanza una `RuntimeException`. Tras reabrir la app,
  existe `crash_*.txt` con la traza, aparece el aviso y el ZIP de soporte lo incluye.
- En la compilación de entrega, ese botón no existe.

### 2.5 Pantalla de estado en vivo

**RF-DIAG-08 (mejora). Pantalla "Estado".**
Es de sólo lectura, se refresca cada segundo y **no envía nada**. Muestra:
- **Enlace:** conectado o no, nombre y MAC, desde cuándo, ms desde el último envío y desde el último RX.
- **Cliente:** timeouts seguidos y el consejo de `CONSEJO_MUDO` si procede (`Cliente.java:47-49`, `:185`).
- **Equipo:** `Sesion.firmware()`, `datosCalibracion()` y la última batería con su edad.
- **Pruebas:** APTO / NO APTO / sin hacer, con la hora.
- **Campaña:** series totales y sin exportar, y acta en curso.
- **Registro:** fichero, bytes, espacio libre y estado de RF-DIAG-05.
- **Permisos y Bluetooth:** como en RF-DIAG-02.

Sin colores por umbral, salvo los rojos que ya existen: sin conexión, NO APTO (`Base.java:92-93`) y
registro detenido. El motivo es el mismo que da `REF/js/diagnostico_enlace.js:49-55`.

*Aceptación:*
- Con la pantalla abierta 5 minutos y conectada, el registro de tramas no gana ninguna línea `TX` atribuible
  a ella.
- Al cortar el Bluetooth, la pantalla pasa a "sin conexión" en ≤ 2 s.

**RF-DIAG-09 (mejora). Contadores de la conexión.**
El `Cliente` cuenta las peticiones por desenlace (`VALIDA`, `TIMEOUT`, `INESPERADA`; `Receptor.java:17`) y
los desbordes (`Cliente.java:166`). Cuenta además por código de orden y guarda la latencia mínima, media y
máxima. Es la PIF-2 de `ROADMAP-MEJORAS-App.md:352`. Los contadores se **recalculan** a partir de una lista
acotada, en lugar de llevarse aparte. Es la lección de `REF/js/depuracion.js:271-276`: un contador
independiente acaba discrepando de la lista. Salen en la pantalla de estado y en el informe.

*Aceptación:*
- Tras las 6 pruebas en el simulador, la suma de los tres desenlaces es igual al número de líneas
  `# ... peticion` del registro.

**RF-DIAG-10 (mejora). Salida de respaldo: copiar el informe.**
Si no hay ninguna aplicación que acepte el ZIP (`Base.java:226-228`, `:280-282`), el texto de
`informe.txt` se copia al portapapeles y se muestra en pantalla para seleccionarlo. Es la salida "que no
puede fallar" de la referencia (`REF/app.js:1320-1345`).

*Aceptación:*
- En un teléfono sin apps de correo ni de mensajería, el informe queda en el portapapeles y se ve en
  pantalla.

**RF-DIAG-11 (mejora). Limpieza de código muerto.**
`Registro.intentCompartir` (`Registro.java:148-161`) no tiene llamadas. Se retira, o se reutiliza dentro de
RF-DIAG-01. Así no quedan dos caminos para compartir que puedan divergir.

*Aceptación:*
- `grep intentCompartir` devuelve 0 definiciones sin uso.

### 2.6 Orden propuesto

1. RF-DIAG-05 y RF-DIAG-06: son pequeños y cierran dos puntos ciegos que ya existen.
2. RF-DIAG-03.
3. RF-DIAG-07.
4. RF-DIAG-04: antes, medir el tamaño del registro con la 3.6.12.
5. RF-DIAG-01 y RF-DIAG-02.
6. Las mejoras: RF-DIAG-08 a RF-DIAG-11.

Cada requisito necesita su prueba JVM en `app/src/test/`, como `Version3612Test.java`. El refresco, además,
necesita una prueba en el simulador que cuente las líneas `TX`.

---

## 3. Lo que no conviene copiar de la referencia

| De la referencia | Por qué no |
| :--- | :--- |
| **Registro de tramas sólo en memoria** (`REF/js/depuracion.js:150`, tope de 300 en `:92`) | En RTV sería un retroceso. `Registro` ya persiste línea a línea (`Registro.java:33-36`, `:119`). En la referencia tiene sentido por la cadencia de 1 `$STATUS` por segundo (`REF/js/depuracion.js:39-44`), y el V3.6 sólo habla cuando se le pregunta |
| **"Efecto" de una orden deducido del `$STATUS` siguiente** (`REF/js/depuracion.js:462-506`) | El V3.6 no emite telemetría periódica. El efecto de una escritura ya se verifica releyendo (`#G`, `#E` y `#V#` frescos: RF-APP-44 y RF-APP-46, `OpsEquipo.java:60-69`). Una ventana de efecto sería inventar la correlación |
| **Reporte por enlace a `api.whatsapp.com`** (`REF/app.js:6007`) | Necesita internet, y RTV no tiene ese permiso a propósito (`AndroidManifest.xml:5-8`). El reporte además lleva emojis y sólo 5 eventos (`REF/app.js:6004`). `ACTION_SEND` deja elegir WhatsApp o correo sin depender de la red |
| **CSV de eventos sin escapar las comillas** (`REF/app.js:6015`) | Un mensaje con `"` rompe la fila. RTV ya escapa con `Medida.csv` (`Sesion.java:300-301`) |
| **Versión de la app escrita a mano** (`REF/app.js:1284`, `:1524`: "V9.0"), distinta de la de `REF/js/config.js:5` ("8.9.0") | Dos cifras para una misma cosa. RTV la toma siempre de `BuildConfig` (`Base.java:55`, `Registro.java:71`, `Campanas.java:394`), y así debe seguir |
| **PIN por defecto en el fuente** (`REF/js/config.js:6`) | Es una credencial. RTV no debe llevar ninguna (M-12, `ROADMAP-MEJORAS-App.md:80`) |
| **Tres copias del mismo fuente** (`app.js`, `www/app.js` y `android/.../assets/public/app.js`) | Hoy coinciden, pero nada obliga a que sigan coincidiendo. RTV tiene una sola fuente y ningún paso de copia |
| **Lista de eventos recortada a 30, de la que la ventana de diagnóstico enseña 12** (`REF/app.js:622`, `:2881`) | La propia referencia documenta que un evento periódico se comía la lista en 6 minutos (`REF/js/diagnostico_enlace.js:15-25`). Una lista corta compartida entre pantallas no sirve como registro |
| **Descarga con `Blob` y un aviso de "si no aparece, copie"** (`REF/app.js:1310-1318`) | Es un rodeo propio del WebView. En RTV el fichero existe y `FileProvider` lo publica. Sólo se adopta la copia al portapapeles como respaldo (RF-DIAG-10) |

**Sí se adoptan de la referencia**, y ya van dentro de los requisitos:
- el recorte contado y declarado;
- lo no medido no se rellena;
- se avisa cuando el almacenamiento falla;
- los contadores se recalculan a partir de la lista;
- no hay umbrales sin decidir;
- el horizonte se calcula a partir de los topes, no se escribe a mano (`REF/js/registro_enlace.js:277-282`).
