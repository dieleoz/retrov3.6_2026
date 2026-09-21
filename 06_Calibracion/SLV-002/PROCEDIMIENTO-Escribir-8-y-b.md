# Procedimiento — escribir los códigos 8 y b en SLV-002
Mapa: `ARQUITECTURA.map` §M2 (flujo de calibración y evidencia de SLV-002).
**Nada de esto se ha probado en un teléfono ni contra el equipo.** Sale de leer el fuente de la app,
el fuente del firmware V3.6 y el ZIP del banco. Ningún texto de los que se citan se ha visto en una
pantalla. Si algo no aparece como aquí se dice, **pare y anote la hora**: manda lo que haga el equipo,
no este papel.

Lo que sí está verificado es de dónde sale cada cosa: cada afirmación lleva su `archivo:línea`.

- **Equipo:** SLV-002 (Coviandina), MAC `00:21:13:05:19:3B`, firmware **3.6.2**, máscara `CAL 0003`.
- **Lo que ya tiene escrito:** códigos **1 y 2**, acta ACEPTADA de las 12:23:26, vence 2027-09-19.
  Todo lo demás es de fábrica.
- **Lo que se va a escribir esta noche:** el **8** y el **b**. Nada más.
- **Fuente de los números:** `INFORME-Ajuste-SLV-002-20260919-1811.md`, sobre el ZIP
  `07 pruebas/190920261812/soporte_SLV-002_20260919_181120.zip`, md5 `79b23590c3a6b1877d55b5c4818b93ce`.

| Código | Color | `c1` | `c0` | Error medio hoy → después |
| :--- | :--- | :--- | :--- | :--- |
| **8** | amarillo tipo I | `2.11652640E-01` | `-1.20446226E+02` | 17,8 % → **4,5 %** |
| **b** | rojo tipo I | `3.20071157E-01` | `-1.82592593E+02` | 70,4 % → **4,8 %** |

`c3` y `c2` valen cero en los dos. No hay que teclearlos: los calcula la app.

---

## 0. Antes que nada: lo que va a pasar de verdad esta noche

Léalo ahora, no cuando pase.

1. **La re-medida del 8 va a fallar casi seguro, y no será culpa suya ni del ajuste.** El umbral que
   usa la app está mal dimensionado. Tiene **dos intentos por código**; al segundo fallo la app
   **deshace la escritura sola**. Está explicado en el §5, con lo que hay que hacer.
2. **No se pierde nada si eso ocurre.** El equipo vuelve a la curva de fábrica del 8, y la campaña,
   el banco y el acta de los códigos 1 y 2 siguen intactos.
3. **No cambie la serie del equipo esta noche.** Ver §1.4. Si graba `SLV-002-2026` antes de calibrar,
   la app deja de dejarle calibrar.
4. **No toque el selector de banco.** Ver §6.

---

## 1. Qué APK usar

### Recomendación: **quédese con la 3.6.17 que ya tiene instalada.** No instale la RTV 1.0.0-rc4.

Y hay una razón que lo cierra antes que las demás: **los números no dependen de la APK.**

Los cinco ficheros que calculan la curva, aplican los criterios y juzgan la re-medida son **byte a
byte idénticos** en las dos versiones. Comprobado por md5 entre los dos árboles de trabajo:

```
IGUAL  Asistente.java        (los criterios RF-CAL-14/15/16 y el criterio de #S)
IGUAL  Ajuste.java           (la recta anclada y los mínimos cuadrados)
IGUAL  Anclas.java           (el ancla de oscuro y la s_rep)
IGUAL  TablaCalibracion.java (el método de cada código y el orden 8 → b)
IGUAL  Remedida3611.java     (los criterios de la re-medida)
```

Y el camino del ZIP tampoco depende de la APK: `Importador.java` e `ImportadorCampana.java` son
también idénticos (`git diff f5145ed HEAD` sobre los dos ficheros: vacío).

Así que la pregunta no es "¿cuál calcula mejor?" —calculan lo mismo— sino "¿cuál arriesga menos?".

### Lo que se pierde con cada una

**Si se queda en la 3.6.17 (recomendado):**

- Siguen abiertos los diez defectos Altos de `REVISION-Arquitectura-P16-V3.6.md` y
  `QA-App-3.6.17.md`. Pero el informe de las 18:11 comprobó **por dos caminos independientes** que
  ninguno llegó a dispararse en estos datos: 0 medidas donadas en el estado final del diario, y 0
  discrepancias al cotejar los 74 pasos de medida contra el patrón real de cada serie.
- De los diez, sólo dos nombran al 8 o a la b, y los dos se rodean con una regla de campo:
  **A-02** (no tocar el selector de banco, §6) y **A-08** (no cambiar la serie, §1.4).
- **No tiene el candado del protocolo 5 × 4** (`ProtocoloDisparos.java` no existe en la 3.6.17:
  comprobado con `git ls-files`, `find`, `ls` y `git ls-tree f5145ed`, los cuatro en vacío). Ese
  candado impediría escribir un código con dato flojo. No importa esta noche: el informe ya verificó
  que los 12 patrones que miran el 8 y la b están los doce en 5 colocaciones × 4 disparos.
- El acta dirá `Cerrar sin restaurar (firma de Diego)` (`CalibrarActivity.java:89`) donde la rc4 dice
  "PIN de administrador". Es el mismo PIN; sólo cambia el rótulo.

**Si instala la rc4:**

- **Pierde la marcha atrás, y es una puerta de un solo sentido.** La rc4 tiene `versionCode 10003`
  (`build.gradle:16`) y la 3.6.17 tiene `3617` (`build.gradle:14`). Android no instala hacia atrás:
  para volver hay que desinstalar. Y la campaña vive en la memoria interna de la app
  (`Campanas.java:66`, `new File(ctx.getFilesDir(), "campanas")`), así que **desinstalar borra la
  campaña, el banco entero y el acta aceptada de los códigos 1 y 2.**
  Las dos APK están firmadas con el mismo certificado (SHA-256 `c990adf6…`, comprobado extrayendo el
  bloque de firma v2 de los dos ficheros) y tienen el mismo `applicationId`, así que la instalación
  *hacia delante* sí conservaría los datos. El problema es que no hay vuelta.
- **Se mete 2 724 líneas nuevas en 35 ficheros entre usted y el equipo, sin revisión.** De la 3.6.17
  a la rc4 se reescriben enteras la capa de protocolo (`Protocolo`, `ProtocoloV36`, `ProtocoloV46`,
  `ProtocoloV4Original`, `ProtocoloV2020`), la detección (`Deteccion.java`, nuevo), los perfiles de
  equipo y la pantalla de medida. La rc2 tiene revisión —APTA CON CONDICIONES— pero **la rc3 y la
  rc4 no tienen ninguna**, y el APK rc2 que se revisó ya ni siquiera está en la carpeta. La regla de
  entrega del repositorio pide el visto bueno del arquitecto **y** de QA (`CLAUDE.md` §6): la rc4 no
  tiene ni uno.
- **Y lo que la rc4 arregla no le sirve esta noche.** Su arreglo de peso es la inversión de los
  campos de `#T` (la temperatura). En un V3.6 la app no pide temperatura en absoluto:
  `ProtocoloV36.tramaTemperatura()` devuelve `null` (`ProtocoloV36.java:85-88`) y `Ops.temperatura()`
  sale sin mandar nada por el cable cuando es `null` (`Ops.java:184-187`). El otro arreglo es el
  texto de la firma del acta, y sólo aparece en el camino de "Cerrar sin restaurar" y "Liberar", que
  es el camino del fracaso.
- El arquitecto ya escribió esta condición, sobre la rc1, y vale igual para la rc4:
  `REVISION-Arquitectura-P16-V3.6.md:48-49` — *"Instale **sólo** `RTV-V3.6.17.apk`. **No instale**
  `RTV-V1.0.0-rc1.apk` … si entra, la 3.6.17 ya no se puede poner encima sin desinstalar, y
  desinstalar borra la campaña."*

### Lo único que cambiaría esta recomendación

Si apareciera un defecto **en el cálculo o en la re-medida** que la rc4 arreglase, habría que
repensarlo. No puede ser: esos cinco ficheros son idénticos. Si lo que aparece es un defecto en la
3.6.17 **fuera** de esos cinco ficheros y dentro del camino de escribir, entonces sí — pero entonces
la salida no es la rc4 sin revisar, es parar y arreglarlo.

---

## 2. El ZIP: cuándo hace falta importarlo y cuándo no

Diego lo ha dicho así: *"debería poder cargar ese .zip en la apk, y darle calibrar"*. Es cierto que
el ZIP de soporte lleva la campaña entera y está pensado para eso. Pero hay un matiz que ahorra
trabajo y riesgo:

> **Si va a usar el mismo teléfono con el que midió el banco, NO importe nada. La campaña ya está
> dentro.** Vaya directo al §3.

El ZIP se importa sólo en dos casos: si va a usar **otro teléfono**, o si en éste se perdió la
campaña.

### 2.1 Cómo se importa

1. Encienda el equipo y déjelo **10 minutos** antes de medir (`GUIA-OPERADOR.md:7`).
2. Conéctese a `COVIANDINA_SLV-002…` en la lista.
3. Deje que terminen solas las pruebas. **Tiene que decir APTO.**
4. Entre en **Campaña**. La campaña se abre con la serie y la MAC del equipo al que está conectado
   (`CampanaActivity.java:165`), así que primero se conecta y después se abre.
5. Pulse **`Importar ZIP de campaña`** (`CampanaActivity.java:76`). Es el primer botón, al lado del
   del banco.
6. Se abre el selector de ficheros de Android. Elija
   `soporte_SLV-002_20260919_181120.zip`.

**El botón está gris si no hay campaña abierta o si hay una medida en curso**
(`CampanaActivity.java:292`). Si lo ve gris, es eso.

**Lo que ve cuando ha ido bien:** una línea con el nombre del fichero, su md5 y el recuento:

```
soporte_SLV-002_20260919_181120.zip (md5 79b23590…): campaña exportada (diario del ZIP): …
```

**Si se equivoca de ZIP** y coge el ligero (`campana_…zip` en vez de `soporte_…zip`), sale
(`CampanaActivity.java:754-755`):

```
es un ZIP ligero (incremental): no trae la campaña entera. Importe el ZIP de soporte (soporte_…zip)
```

### 2.2 Si importa en el teléfono que ya tiene esa campaña abierta

**Ni se duplica ni se pisa: se funde, y en la práctica no hace nada.** Es el caso benigno.

- Las series que ya están se reconocen por su clave y se cuentan como `yaEstaban`, **no se vuelven a
  crear** (`ImportadorCampana.java`, bucle sobre `todo.series()`: `if (vistas.contains(clave(s))) {
  r.yaEstaban++; … continue; }`).
- Si aquí ya hay una serie elegida viva para un patrón, **manda la suya** y el importador lo avisa:
  `"<patrón>: se mantiene la serie elegida por el operador (<id>)"`.
- Los pasos del banco no pisan un paso que ya tenga estado, salvo para mejorarlo (SALTADO o REHACER
  pasan a HECHO).

Como los 80 pasos de la cola ya están HECHO, importar aquí es prácticamente una operación en vacío.
**Por eso no merece la pena hacerlo: gana poco y toca la campaña que ya está bien.**

### 2.3 Si importa en otro teléfono limpio

Funciona, y el importador es estricto de la manera correcta: **o entra todo o no entra nada.**

Se planta y no importa nada si:

- **Es de otro equipo:** `"la campaña es de otro equipo (<equipo>, <MAC>); la abierta es de <equipo>
  (<MAC>): no se importa nada"`.
- **Hay un patrón que no está en el catálogo** del APK: `"la serie <id> es de un patrón que no está
  en el catálogo (<Pxx>): no se importa nada"`.
- **El diario trae líneas que no entiende:** `"<n> líneas del diario no se entienden: no se importa
  nada"`.

**Lo que gana:** un teléfono sin nada heredado. Los defectos A-03 y A-05 de la P16 viven en estados
de paso arrastrados de sesiones anteriores; en un teléfono limpio no hay de dónde arrastrarlos.

**Lo que pierde:** el histórico de actas de este equipo, que vive en la memoria interna del teléfono
viejo. El acta aceptada de los códigos 1 y 2 se queda allí. Y la comprobación T-C41 —que coteja lo
que el equipo lleva dentro contra el acta heredada— necesita esa acta. **Si no la tiene, el T-C41 le
va a fallar y no podrá calibrar.**

> **Conclusión: use el teléfono de siempre.** El otro teléfono sólo si en éste se perdió la campaña,
> y entonces hay que llevar también el acta de los códigos 1 y 2.

### 2.4 El evento de cola que viaja en el ZIP, y por qué importa

Al importar, si la campaña de destino todavía no tiene cola elegida, el importador **le pone la del
ZIP** (`c.elegirCola(tipoOrigen, todo.colaMd5())`). En este ZIP la cola es la REPRESENTATIVA, md5
`70ef3b868db85ef75743936a6218935e` (informe, fila "Cola del banco"). Eso es lo que hace que al abrir
el Banco aparezca directamente como REPRESENTATIVO y con los 80 pasos HECHO.

Si la campaña de destino **ya tiene otra cola**, el importador trae las series pero **no los pasos**,
y lo dice:

```
el ZIP es de un banco REPRESENTATIVO y esta campaña de un banco COMPLETO: se traen las series, no los
pasos; lo ya medido cuenta al abrir el Banco
```

Eso no es un error, pero deja los 80 pasos sin estado, y entonces `BancoPrevio` los vuelve a rellenar
desde cero — que es exactamente el sitio donde vive el defecto de las donaciones
(`BancoPrevio.java:145`). **Mientras los pasos conserven su estado, `BancoPrevio` los salta y el
defecto no puede dispararse** (`BancoPrevio.java:139`: `est.containsKey(p.orden) → continue`).

De ahí las dos reglas del §6.

---

## 3. ¿Hace falta el equipo conectado? **Sí. Sin discusión.**

El ZIP trae los **datos**. La calibración **escribe en el equipo**. Son dos cosas distintas.

Escribir un código manda al equipo `#S,8,…#`, lo relee con `#G`, lo comprueba con `#E` en cinco
puntos, y al aceptar graba la fecha con `#SC` y la relee con `#GC#`. Nada de eso se puede hacer desde
un fichero.

Y la app no le va a dejar ni empezar: la pantalla **Calibrar este equipo** abre con
`Comprobaciones previas` (`CalibrarActivity.java:66`), y la lista sale de
`FlujoCalibracion.previas()` (`FlujoCalibracion.java:364-418`). Las primeras son, literalmente:

| Lo que comprueba | Qué dice si falla |
| :--- | :--- |
| Conexión | `Sin conexión: conecte con el equipo` |
| Firmware 3.6.2 | `El firmware no está detectado como 3.6.2 (<x>): pase las pruebas del equipo` |
| Pruebas APTO | `Pruebas: NO APTO (<resumen>): con NO APTO no se calibra (QA-3612-02)` |
| Cola del banco | `Cola del banco no admitida` |
| Campaña | `Sin campaña de este equipo` |
| Serie `#GN#` | ver §1.4 y §3.1 |
| s_rep | `s_rep: … — mida la A5 del inicio del banco` |
| Batería | `Batería: la última lectura bloquea las escrituras (n = 0 o sin respuesta)…` |

Con cualquiera en rojo, el botón de calibrar está deshabilitado.

**Lo que tiene que tener delante, entonces:** el equipo encendido y emparejado, los patrones **P43**
(amarillo tipo I) y **P49** (rojo tipo I) a mano para las re-medidas, la lámina A5 y el oscuro por si
hay que rehacer algo, el PIN, y batería.

### 3.1 La comprobación que más gente sorprende: serie y MAC

La app exige que la serie que el equipo devuelve con `#GN#` sea **exactamente la serie actual de la
campaña**, y que la MAC coincida. Si no:

```
El equipo dice SLV-002-2026 (#GN#) y la campaña SLV-002: repita "Cambiar serie" en Avanzado hasta que
coincidan (el acta lleva la serie de la campaña). No se calibra
```

o, si no está ni en el historial:

```
La serie #GN# (<x>) no coincide con la campaña (SLV-002): no se calibra
```

### 3.2 Por eso: **no grabe la serie nueva esta noche**

La decisión **SERIE-2** deja escrito que SLV-002 pasa a `SLV-002-2026`, pero está **sin grabar**.
Si esta noche pulsa "Cambiar serie" antes de calibrar, `#GN#` deja de casar con la campaña y la app
no le deja escribir nada.

**Orden correcto: primero el 8 y la b, con sus actas aceptadas. La serie, otro día.** Y cuando toque,
con la condición del arquitecto delante (`REVISION-Arquitectura-P16-V3.6.md:50-51`): Diego da el
visto bueno con el equipo delante, y si la app dice "no se pudo verificar", **se para**.

---

## 4. El procedimiento, paso a paso

### 4.0 El PIN

- **PIN de fábrica: `2026`.** Dos fuentes independientes: `PROTOCOLO-V3.6.md:74` y el propio
  firmware, `01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c:180`
  (`pinAdmin[0]='2'; pinAdmin[1]='0'; pinAdmin[2]='2'; pinAdmin[3]='6';`). Si alguien lo cambió con
  `#P`, es el que se puso: el firmware lo lee de la EEPROM (`calibracion_v36.c:197`).
- **Son 4 dígitos.** La app no admite otra cosa.
- **Cinco fallos seguidos bloquean el PIN hasta apagar el equipo**
  (`calibracion_v36.c:299` `PIN_MAX_FALLOS 5`, y `:645-655`). Si se bloquea: apague el equipo,
  espere, enciéndalo y vuelva a conectar.
- La app lo pide **una vez por conexión**, en un diálogo titulado
  `PIN del equipo (una vez por conexión)` con el botón `Seguir` (`CalibrarActivity.java:393-395`).
- El modo administrador del firmware caduca **a los 10 minutos sin tramas `#`**
  (`calibracion_v36.c:298,338`). **No se preocupe por eso:** la app reenvía `#L` al principio de cada
  fase (`FlujoCalibracion.entrar()`, que llama siempre a `ops.entrar(pin)`). No hay que hacer nada.

### 4.1 Preparar

1. Equipo encendido **10 minutos** antes de medir.
2. Conectar. Dejar que pasen las pruebas solas. **APTO.**
3. Entrar en **Calibrar este equipo** (botón `Calibrar` en la pantalla principal, o
   `Calibrar este equipo` desde Campaña).
4. Mirar `Comprobaciones previas`. **Todas en verde.** Si alguna está en rojo, se resuelve antes; no
   se sigue.

### 4.2 Escribir el 8

El 8 **va solo y primero**. Lo impone la tabla (`TablaCalibracion.java:189-190`), con este aviso:
*"El 8 va solo y primero; el b y el 5, en un acta posterior (P12 §6.3)"*.

1. En **`Códigos (tabla RF-CAL-37 del APK)`** (`CalibrarActivity.java:68`), marque **sólo** la
   casilla del código 8. Dice `Conforme: escribir el código 8`.
   **Asegúrese de que la del b y la del 5 están sin marcar.** Si marca el 8 con otro, sale:
   `El código 8 va solo, en un acta propia (P12 §6.3): desmarque los demás o acabe el acta en curso.`
2. Rellene **`Nombre del superadministrador (se recuerda)`** (`:72`) y
   **`Nota de la conformidad`** (`:74`). **Los dos son obligatorios**: sin ellos sale
   `Escriba el nombre y la nota de la conformidad.` Lo que ponga en la nota va al acta; use el §7.
3. Pulse **`Calibrar todo (8 → b → 5 …)`** (`:79`) con sólo el 8 marcado.
   *(La alternativa es `Continuar / un código` (`:80`) y llevar a mano la persistencia y la
   aceptación. Recomiendo `Calibrar todo`: encadena los pasos en el orden correcto y **aun así le
   enseña el acta antes de aceptarla**, ver el paso 7. De noche, menos cosas que recordar.)*
4. Teclee el PIN cuando lo pida.
5. **T-C41 — apagar y encender.** Sale el diálogo `Apagar y encender` con el texto
   `Apague el equipo (la app verá caer la conexión), espere 5 s y enciéndalo. Pulse OK cuando esté
   encendido: la app vuelve a conectar sola.` (`CalibrarActivity.java:255-257`). Hágalo de verdad:
   la app comprueba que el enlace cae.
   **Si falla:** `T-C41 FALLA: <motivo>. No se calibra: el equipo no tiene lo que dicen <origen>;
   avise a Diego.` Esto significa que lo que el equipo lleva dentro no cuadra con el acta de los
   códigos 1 y 2. **Pare.**
6. **La escritura** (`FlujoCalibracion.java:1202-1258`). La app, en este orden:
   lee la batería → `#L` → comprueba que la curva pasa el criterio de `#S` → **lee con `#G` la curva
   anterior** (para poder deshacer) → anota `ESCRIBIENDO` en el acta **antes** de enviar → manda
   `#S,8,…` → **la relee con `#G`** y compara bit a bit → **la comprueba con `#E` en cinco puntos**
   (x = 500, 1000, 2000, 3000, 4000).
   Verá en el progreso: `Código 8: #S...`
   Al acabar bien: `Códigos escritos y verificados. Ahora: persistencia (apagar y encender) y después
   aceptar.`
   **Si la escritura falla**, la app restaura sola:
   `Escritura del código 8 fallida: #S,8 -> <…>. Restaurado.`
7. **La re-medida de P43.** Lo importante está en el §5. Léalo antes de llegar aquí.
8. **Persistencia.** Otra vez apagar y encender. La app relee `#G` y `#E` y comprueba que la curva
   sigue dentro. `Persistencia OK: …` o `Persistencia FALLA (<motivo>): …`.
   Si dice `Persistencia NO válida: no se vio caer y volver el enlace (P12 §5.1). Apague de verdad el
   equipo y repítala.` — es que no lo apagó del todo.
9. **El acta, para leerla.** Diálogo titulado `Acta del código 8`, con el acta entera y la pregunta
   `¿Acepta esta acta? Se graba la fecha de calibración (una vez al día).`, y tres botones:
   **`Aceptar`** / **`Rechazar`** / **`Parar aquí`** (`FlujoCalibracion.java:1738-1745`).
   **Léala aquí.** Lo que hay que mirar está en el §7. `Parar aquí` deja el acta a medias y se puede
   retomar.
10. **Aceptar.** Al pulsar `Aceptar`, la app (`FlujoCalibracion.java:1432-1496`):
    `#L` → batería → `#V#` → relee `#G` de cada código certificado y de los heredados → si todo casa,
    lee `#GC#`, y **sólo si la fecha no es ya la de hoy** manda `#SC,2026-09-19#` y la **relee con
    `#GC#`** → exporta el ZIP de soporte y mete su **SHA-256** en el acta → cierra el acta.
    **Confirmado que `#SC` va después de aceptar, nunca antes:** es la única llamada a `#SC` del
    árbol y está dentro de `aceptar()` (`FlujoCalibracion.java:1474`).
    Al final verá:
    `Acta ACEPTADA; fecha 2026-09-19 (vence 2027-09-19). SLV-002`
    **Si algo no casa antes de grabar la fecha:**
    `Verificación final FALLA: <motivo>. El acta no se acepta y no se graba la fecha.`
    **Si la fecha no quedó:**
    `#SC no quedó grabada (#SC -> …, #GC# -> …): el acta NO se cierra. Reintente.`

### 4.3 Escribir el b

Igual que el 8, con dos diferencias:

- **Exige el acta del 8 ACEPTADA.** Está en la tabla (`TablaCalibracion.java:196`,
  `requiereAceptado = '8'`). Si no lo está: `va después de un acta ACEPTADA (y vigente) del código 8:
  el 8 solo y primero`.
- **El patrón de re-medida es P49**, no P43.
- Su casilla lleva un texto más largo, porque arrastra la dispensa PA-24. Léalo y vaya al §7.

Desmarque el 8 (ya está aceptado; si lo deja marcado la app lo salta y lo dice), marque **sólo la b**,
y repita del paso 3 al 10.

### 4.4 El ZIP de soporte y su huella

**Se exporta solo al aceptar cada acta**, y **su SHA-256 queda escrito dentro del acta** como el dato
`ZIP de soporte (SHA-256)` (`FlujoCalibracion.java:1490-1493`). No hay que hacer nada para eso.

Al terminar la acción se abre **un único selector para compartir** el informe y los ZIP, con el texto
`Calibración de SLV-002: informe y ZIP (copia en Download/RTV/)` (`CalibrarActivity.java:545`).
**Envíeselos.** Hay copia en `Download/RTV/` del teléfono.

**Si el ZIP no se pudo exportar**, al final del mensaje aparece:
`ATENCIÓN: el ZIP no se pudo exportar (<motivo>). Pulse ZIP de soporte en el Banco o en Campaña.`

El botón **`ZIP de soporte`** del Banco es el único sitio que **enseña las huellas en pantalla**:

```
ZIP de soporte: soporte_SLV-002_<fecha>_<hora>.zip
md5 <32 hex>
sha256 <64 hex>
Copia: <ruta en Download/RTV/>
```

Apunte el md5. Al archivarlo en `06_Calibracion/SLV-002/` va con su huella en `HUELLAS.txt`
(`CLAUDE.md` §3).

---

## 5. La trampa de la re-medida

**Esto es lo que más probablemente le va a pasar, y lo que peor documentado está.** Va entero.

### 5.1 Qué es, y por qué el umbral está mal

La app juzga la re-medida con dos criterios (`Remedida3611.java:55-74`):

- **Criterio 2 — reproducir la campaña:** `|x_remedida − x_banco| ≤ 2 · s_rep · x_banco · √(1/K+1/K)`
- **Criterio 3 — frente al certificado:** `|R − R_cert| ≤ máx(10 % ; 2 unidades)`
  *(en la b, la decisión REMEDIDA-b cambia el 3 por RF-CAL-18, frente a la curva escrita)*

El problema está en el criterio 2. La `s_rep` que usa sale de la lámina A5 medida **en una sola
tirada, al principio de la sesión**: 0,149 % en la sesión del 8 y 0,314 % en la del b
(informe §2.1). Pero la re-medida compara esa tirada con una serie medida **horas antes, sobre otro
patrón, levantando y volviendo a apoyar el equipo**. Eso se mueve mucho más. La mediana del día fue
**1,25 %** (informe §6).

Los números, calculados con la fórmula exacta del código:

| Código | Patrón | x del banco | s_rep | **Límite del criterio 2** | La dispersión real del día |
| :--- | :--- | ---: | ---: | ---: | :--- |
| **8** | P43 | 1133,90 | 0,149 % | **±2,1 cuentas (±0,19 %)** | mediana 1,25 % = 14,2 cuentas → **6,6 veces el límite** |
| **b** | P49 | 813,30 | 0,314 % | **±3,2 cuentas (±0,40 %)** | mediana 1,25 % = 10,2 cuentas → **3,1 veces el límite** |

Puesto de otra manera, con la tabla de repetibilidad del informe (17 patrones repetidos ese día):
**sólo 2 de los 17 habrían pasado el límite del 8, y 4 de los 17 el de la b.** Y eso incluye los
patrones puente, que son el mejor caso.

> **Traducción: es más probable que la re-medida falle a que pase, aunque todo esté bien.**

### 5.2 Qué va a ver exactamente

**Primero, la petición** (`FlujoCalibracion.java:1308-1310`):

- Título: `Re-medida del código 8`
- Mensaje: `Coloque P43 (…) y pulse OK. 5 colocaciones × 4 pares ('e' y código 8).`
- Botones: `OK` / `Parar aquí`

Entre colocación y colocación: `Levante y apoye (2 de 5)` — `Levante el equipo y vuelva a apoyarlo
sobre P43.`

**Si una colocación concreta no vale** (el patrón no está, o el equipo se movió entre los dos
disparos del par), sale `Colocación no válida` con el motivo y `Vuelva a colocar P43 y pulse OK.`
**Esa colocación no cuenta y no gasta ninguna de las cinco.** Repítala tranquilo. El tope son 10
seguidas, y entonces: `Demasiadas colocaciones no válidas seguidas (10): se para la re-medida del
código 8. Revise el patrón y el apoyo, y vuelva a pulsar Calibrar.`

**Si las cinco colocaciones entran pero el conjunto no pasa**, sale el diálogo que importa
(`FlujoCalibracion.java:1379-1381`):

- Título: **`Re-medida NO CONFORME`**
- Mensaje: el resultado, con **las dos mitades**, y debajo `Queda una repetición.`
- **Un solo botón: `OK`.** No hay "aceptar igualmente". No existe.

El texto tiene esta forma exacta (`Remedida3611.java:69-72`):

```
P43: x 1148.3 frente a 1133.9 del banco (+1.3 %, límite ±2.1; s_rep 0.15 % A5 del inicio de la
sesión 1) FALLA; R 119.8 frente a cert. 122 (-1.8 %, límite ±12.2) OK
```

### 5.3 Cómo distinguir el fallo falso del de verdad

**Lea las dos mitades del texto. Están separadas por un punto y coma.**

| Lo que ve | Qué es | Qué hacer |
| :--- | :--- | :--- |
| La **x** dice `FALLA`, la **R** dice `OK`, y la desviación de x es de un 0,5 a un 3 % | **Fallo falso.** Es el umbral, no el equipo. La curva es buena: lo dice la mitad que compara contra el certificado | §5.4 |
| La **x** dice `FALLA` con una desviación **grande** (más del 5 %, decenas de cuentas) | **Fallo de verdad, y es de colocación.** Lámina mal apoyada, sucia, movida, o no es el patrón que cree | Repita, cuidando el apoyo. Es lo que el informe vio en S006/P34: los tres primeros disparos 700 cuentas altos |
| La **R** dice `FALLA` | **Pare.** Eso sí es la curva, o el patrón equivocado | No insista. Anote la hora y avise |
| `NO_EVALUABLE` | Falta la s_rep o falta la serie del banco de ese patrón | No es un fallo de la medida. Avise: hay un agujero en los datos |

El texto literal de `NO_EVALUABLE` es:
`P43: no hay s_rep medida (A5 del inicio o K >= 3 en la serie); el criterio no se evalúa (RF-CAL-38)`

### 5.4 Qué hacer cuando es un fallo falso — y el presupuesto de dos intentos

**Tiene exactamente dos re-medidas válidas por código.** No es negociable y está en el código:
`puedeRepetir()` exige `validos() < 2` (`Acta.java:169-170`) y `debeRestaurarse()` se cumple con
`validos() >= 2` (`:174-175`).

**Al segundo fallo la app deshace la escritura ella sola, sin preguntar**
(`FlujoCalibracion.java:1305-1306` → `restaurarCodigo(k)`). Verá:

```
Código 8: restaurando la curva anterior...
```

y después:

```
Código 8 restaurado tras dos re-medidas no conformes: <detalle>. La secuencia se detiene.
El acta no tiene ningún código conforme: pulse Rechazar.
```

**Eso no es una avería.** El equipo vuelve a la curva de fábrica del 8, verificado con `#G`. No se
pierde nada más. Pulse `Rechazar` como dice.

**Por eso el primer intento hay que gastarlo bien.** Lo único que está en su mano es colocar la
lámina lo más parecido posible a como estuvo en el banco:

- **Misma lámina, mismo sitio, misma orientación, mismo apoyo.** El informe midió P5 a 0° y a 90° con
  dos minutos de diferencia: +0,36 %. La orientación no parece el problema, pero no lo agrave.
- **Lámina limpia y plana.** Las desviaciones grandes del día fueron de apoyo, no de ruido.
- **Equipo ya caliente**, encendido de hace rato.
- **Antes de empezar la re-medida de verdad, apoye y levante una vez** sin prisa, para que el equipo
  asiente. El informe encontró un patrón repetido de *colocaciones iniciales desviadas en bloque*
  (S006/P34, S036/P27, S038/P28, S068/P37).

**Si se le van los dos intentos con la x fallando y la R bien:** el código no se escribe esta noche.
Y la conclusión honesta es que **no es suyo el problema, es del umbral**: hay que hacer que la
`s_rep` del criterio 2 se mida entre re-colocaciones y no dentro de una tirada. Eso es un cambio de
la app, con su revisión. **No lo fuerce esta noche y no baje ningún umbral a mano.**

Anótelo y páselo: es el hallazgo más accionable de la sesión.

### 5.5 Un detalle que confunde

Tras una re-medida válida no conforme, **no se puede volver a escribir el código: sólo repetir la
re-medida** (`Acta.java:406-408`): *"el código 8 tiene una re-medida válida NO CONFORME: se repite la
re-medida, no la escritura (D-20)"*. Es correcto. La curva escrita es la misma; lo que se repite es
la comprobación.

---

## 6. Las condiciones de campo que aplican esta noche

De las 21 de la revisión de la rc2 y las 7 de la P16, éstas son las que tocan a esta sesión. En
lenguaje llano.

### 6.1 No toque el selector de banco. Punto.

Está en la pantalla del Banco, como `Banco: REPRESENTATIVO (cambiar)`. **No lo pulse.** Ni para
mirar.

- Cambiar de tipo de cola **borra el estado de los 80 pasos** (`Campana.java:536-540`:
  `pasos.clear(); seriePaso.clear(); historialPasos.clear();`).
- Y con los pasos vacíos, `BancoPrevio` los vuelve a rellenar — que es justo donde vive el defecto de
  las donaciones. Hoy no está disparado. Tocar el selector es la forma más fácil de dispararlo.
- Además, el botón **no se bloquea ni con una medida en curso** (A-02), y entonces puede dar por
  medido un patrón con la lectura de otro.

Si la app le ofrece cambiar de cola sola al entrar al Banco, diga que **no** y quédese donde está.

### 6.2 Si el enlace cae al grabar la fecha: **NO pulse "Rechazar"**

Es el caso A-06 / QA-3617-02, y está reproducido **sobre el código 8** en la QA
(`QA-App-3.6.17.md:259`).

**Qué pasa:** `#SC` entra en el equipo, el enlace cae antes de que la app la relea con `#GC#`. El
equipo se queda con la fecha de hoy y el acta sin cerrar.

**Qué ve:** `Aceptar interrumpido: IOException: enlace perdido…` o
`#SC no quedó grabada (#SC -> …, #GC# -> …): el acta NO se cierra. Reintente.`

**Qué hacer:** **reconecte, pase las pruebas otra vez y vuelva a pulsar `Aceptar y grabar fecha`.**
La app ve que la fecha ya está puesta y no la reescribe (anota en el acta
`no se reescribe: el equipo ya tiene la fecha de hoy`), y cierra bien.

**Qué NO hacer:** pulsar `Rechazar`. `rechazar()` devuelve las curvas a su estado anterior pero **no
toca `#SC` ni `#GC#`** — verificado leyendo el método entero (`FlujoCalibracion.java:1501-1554`) y
con `grep '#SC'`, cuyas únicas apariciones ejecutables están en `aceptar()`. El equipo se quedaría
**con las curvas viejas y la fecha de hoy**: presentándose como calibrado hoy sin estarlo.
**Si ya ocurrió, no entregue el equipo: avise.**

### 6.3 No pulse `Rehacer` y luego cambie de cola

Si rehace un patrón, mídalo antes de tocar nada más. Un cambio de cola después de un Rehacer puede
resucitar la medida vieja sin avisar (A-05).
*(Ojo: la QA-3.6.17 dice que no consigue reproducirlo y contradice a la P16. Está sin cerrar. Se
rodea igual: no cuesta nada.)*

### 6.4 Cuente los disparos

Si un disparo no da lectura, la app lo descarta **sin decírselo en pantalla**. Si una colocación
termina antes de lo que esperaba, repítala.

### 6.5 No se fíe del número de batería

La app no comprueba que esté entre 0 y 100 %. Si ve un valor absurdo, tómelo como "no sé". Sólo
bloquea escrituras con 0 % o sin respuesta. Tenga batería de repuesto: una batería que bloquea a
mitad de un acta es una noche perdida.

### 6.6 Si sale "RESTAURACIÓN NO VERIFICADA" o "rechazo pendiente": pare

No toque nada más y avise. Tenga el PIN a mano: sin PIN y sin enlace no se puede cerrar ni liberar
(A-04). El texto es:

```
NO se rechaza: RESTAURACIÓN NO VERIFICADA del código 8 (…). El acta queda con un rechazo pendiente:
vuelva a pulsar Rechazar para reintentarlo; si no se resuelve, avise a Diego.
```

En ese estado la app deshabilita `Calibrar todo`, `Continuar / un código`, `Persistencia` y
`Aceptar`, y hace visible `Cerrar sin restaurar (firma de Diego)`. **Ese botón es el último recurso
y cierra el acta dejando los códigos en estado desconocido.** No lo pulse sin hablar antes.

### 6.7 No cambie la serie

Ver §3.2. Esta noche, no.

---

## 7. Qué tiene que decir el acta

El acta la genera la app (`Acta.java:744-819`). **Léala en el diálogo `Acta del código 8` antes de
pulsar `Aceptar`.** Esto es lo que hay que mirar, y lo que hay que añadir a mano.

### 7.1 Lo que tiene que estar, y hay que comprobar

| Campo | Qué tiene que decir |
| :--- | :--- |
| Equipo y MAC | `SLV-002` y `00:21:13:05:19:3B` |
| Firmware | `V3.6 2026-09-19 (3.6.2) CAL mascara 0003` |
| `banco` | `REPRESENTATIVO` |
| `md5 de la cola` | `70ef3b868db85ef75743936a6218935e` |
| `patrones 8` | **`P44, P37, P34, P43`** — los cuatro, y ninguno más |
| `patrones b` | **`P39, P38, P49`** — los tres, y ninguno más |
| Curva certificada | la trama `#G,8,0.00000000E+00,0.00000000E+00,2.11652640E-01,-1.20446226E+02#` (y la equivalente del b). **Compruebe los dos coeficientes contra la tabla de arriba.** |
| `#E en 5 puntos` | `coincide con la curva enviada (±1)` |
| `protocolo banco` | `5×4` en todos los patrones |
| Persistencia | `OK` |
| Verificación final | `OK` |
| Estado | `ACEPTADA <fecha>` |
| `ZIP de soporte (SHA-256)` | tiene que estar. Es la trazabilidad del acta |

> **Si en `patrones 8` o `patrones b` aparece un nombre que no está en esas listas, pulse `Rechazar`
> y avise.** Sería la donación de un equivalente, que es el defecto A-01. Hoy no debería pasar; ésta
> es la comprobación que lo caza si pasa.

### 7.2 Lo que el acta **no** trae, y hay que poner en la nota

Tres carencias reales, comprobadas leyendo `Acta.texto()` entero:

1. **No lleva la fecha de vencimiento.** Verificado dos veces: `grep "vence\|vencimiento" Acta.java`
   no devuelve nada, y en todo el árbol `Calibracion.vencimiento(...)` sólo se usa en
   `ExportadorFinal.java:36` (el informe), en el mensaje de retorno `FlujoCalibracion.java:1494` y en
   `Sesion.java:195`. **Contradice el `CLAUDE.md` §5**, que exige la fecha de vencimiento en el acta,
   en el informe y en todo registro exportado. Se rodea: está en el informe que se exporta al lado, y
   se escribe en la nota. **Corregirlo es trabajo de la app, no de esta noche.**
2. **No lleva los residuos si no hay incumplimiento.** Y el 8 y la b no incumplen nada, así que el
   acta saldrá **sin ningún residuo**. Los residuos están en el informe y en la tarjeta de la
   pantalla. Póngalos en la nota.
3. **No dice qué códigos quedan de fábrica.** Ver §7.4.

### 7.3 La nota de la conformidad: lo que hay que escribir

**Para el 8:**

```
Código 8 (amarillo tipo I), recta anclada en oscuro x0=569,08. Sin incumplimientos y sin dispensa.
Residuos frente al certificado: P44 +5,3 %, P34 -5,0 %, P37 +5,9 %, P43 -2,0 %. RMS de tipo I 4,8 %
(fábrica 19,7 %). Error medio 17,8 % -> 4,5 %. Ajuste validado en x = 887 a 1134; por encima y por
debajo, la curva extrapola y no hay patrón que la respalde. Vence 2027-09-19.
```

**Para el b — y aquí está lo que no se puede dejar pasar:**

```
Código b (rojo tipo I), recta anclada en oscuro x0=570,48. Sin incumplimientos.
LA DISPENSA PA-24 NO SE INVOCA Y NO HACE FALTA: con el banco de las 18:11 los residuos son
P39 +6,1 %, P38 +4,2 %, P49 -4,0 %, los tres dentro del ±10 % de RF-CAL-14, y el RMS de tipo I es
4,9 %, dentro del 6 % de RF-CAL-15. El techo de PA-24 (P39 +15 %, P49 -10 %) y el límite ampliado
RF-CAL-15-b de 11,5 % no llegan a aplicarse. El código b entra por derecho propio.
Error medio 70,4 % -> 4,8 %. Validado en x = 723 a 813. Vence 2027-09-19.
```

**Por qué hay que escribirlo así:** la app pone la dispensa en la ficha del código de oficio, porque
la fila del b sólo existe como escribible si PA-24 y REMEDIDA-b están en `decisiones.csv`
(`TablaCalibracion.java:192-197`; las dos están, líneas 10 y 12 del
`assets/decisiones.csv`). El acta dirá algo como `dispensa RF-CAL-14/15 (PA-24)`. **Eso hoy es
falso en el fondo aunque sea cierto en la forma:** no hay nada que dispensar. Si el acta sale
invocando una dispensa que no hace falta, un lector futuro creerá que el b se escribió forzado.
**La nota es el sitio donde se corrige.**

### 7.4 Qué queda calibrado y qué queda de fábrica

Al acabar la noche, el acta y el informe tienen que dejar claro que **este equipo no es uniforme**:

| Código | Color | Estado tras esta noche |
| :--- | :--- | :--- |
| 1 | blanco intenso | **calibrado** (acta 12:23 del 19-sep-2026) |
| 2 | amarillo intenso | **calibrado** (acta 12:23) |
| **8** | **amarillo tipo I** | **calibrado esta noche** |
| **b** | **rojo tipo I** | **calibrado esta noche** |
| 3 | verde intenso | **de fábrica** — bloqueado, dato invertido (§8) |
| 4 | rojo intenso | **de fábrica** — mezcla tres tipos de lámina en una curva |
| 5 | azul intenso | **de fábrica** — bloqueado, dato invertido (§8) |
| 6 | naranja intenso | **de fábrica** — a un patrón de conseguirlo |
| 7, a, c, d | tipo I blanco/verde/azul/naranja | **de fábrica** — sólo verificación |
| temperatura | — | **de fábrica** (máscara 0003) |

**Esto hay que decirlo en el informe que se entregue.** Un equipo con cuatro códigos ajustados y ocho
de fábrica **no es el mismo instrumento según qué lámina se mida**. La máscara `#V#` debería pasar de
`0003` a incluir los bits del 8 y del b; compruébelo al final con `#V#`.

---

## 8. Los códigos 3 y 5: lo que de verdad los desbloquea

**No se escriben, y no es cuestión de aflojar umbrales.** El dato está invertido.

**Código 3 (verde intenso).** En los tres tipos de lámina, el patrón de certificado más alto da
menos cuentas que el de certificado más bajo:

| Patrón | tipo | cert | x medida |
| :--- | :--- | ---: | ---: |
| P64 | IV | 54 | 828,25 |
| P63 | IV | 121 | **669,75** |
| P124 | IX | 51 | 823,50 |
| P123 | IX | 115 | **685,50** |
| P109 | XI | 51 | 946,00 |
| P16 | XI | 170 | **842,25** |

**Código 5 (azul intenso).** Lo mismo, y peor. Dentro del mismo tipo XI: P18 (cert. 84) da 811
cuentas y P112 (cert. 101) da 733 — 78 cuentas de inversión, donde P112 repitió entre sus dos series
con 7,4 cuentas de diferencia.

Las inversiones son del **15 al 20 %**. La repetibilidad del día es del **1 al 5 %**. Medir otra vez
no lo arregla: **no es la medida, es el certificado o la lámina.**

Y lo confirma el control que el propio Diego apartó para esto: si se escribiera el 5, **P81 (cert.
92) pasaría a leer 44,7**, un −51,5 %.

### El encargo que hay que sacar

> **Verificar contra el otro retrorreflectómetro patrón los certificados de:**
> - **los verdes de 51:** `P109`, `P124` (y `P64`, cert. 54)
> - **los azules de 84-101:** `P18`, `P112`, `P67`, `P127`

El operador ya empezó: en la serie **S129 (P109)** anotó *"da 51 en el otro retro patrón; El retro
patrón da 51"*. Y la propia cola lo sospechaba: los pasos 73 y 76 llevan la nota *"uno de los 9
verdes certificados en 51 exacto: confirmar certificado"*.

**Eso es lo que desbloquea el 3 y el 5. No más medidas con este equipo.**

Los otros dos, para cerrar el cuadro:

- **Código 6 (naranja):** está a un patrón. Falla por P87 (+11,1 %) y por el RMS del tipo IX (7,5 %
  frente al 6 %). Medir **P77, P76 o P122** y repetir **P87 en 5 × 4** podría cambiarlo.
- **Código 4 (rojo intenso):** mezcla los tipos IV, IX y XI en una sola curva y no se pueden
  conciliar. Antes de volver a intentarlo hay que decidir si el rojo intenso admite **una sola curva
  para los tres tipos**. Es una decisión, no una medida.

---

## 9. Lo que contradigo de lo que se me pasó

Regla del repositorio: la contradicción es un entregable.

| Lo que se me dijo | Lo que verifiqué |
| :--- | :--- |
| «las **15** condiciones de campo» de la revisión de la rc2 | Son **21**, numeradas, en `REVISION-QA-RTV-1.0.0-rc2.md:487-560` |
| «las **siete** condiciones de la P16» | Son siete ítems, pero su propio encabezado dice «6 líneas» (`REVISION-Arquitectura-P16-V3.6.md:36` frente a `:38-51`). El documento no concuerda consigo mismo |
| «sólo se escriben el 8 y la b» | Los dos documentos de revisión analizan el escenario **«8, b y 5»** (`REVISION-Arquitectura-P16-V3.6.md:224`). La condición 4 de la P16 —leer la lista `patrones 5` del acta— era **la única salvaguarda contra A-01**, y al no escribirse el 5 se queda sin objeto. Por eso el §7.1 de aquí la sustituye por la comprobación de `patrones 8` y `patrones b` |
| «instalar la rc4 conserva la campaña» | **Cierto, y verificado**: mismo `applicationId`, mismo certificado de firma (SHA-256 `c990adf6…` en las dos APK) y `versionCode` mayor. Y también es cierto lo otro: volver atrás exige desinstalar, y desinstalar borra `getFilesDir()/campanas` (`Campanas.java:66`) |
| «cargar el ZIP y darle calibrar» | **Sólo si cambia de teléfono.** En el teléfono que midió el banco la campaña ya está dentro e importar no aporta nada (§2.2). Y en todo caso **el equipo tiene que estar encendido y conectado**: el ZIP trae los datos, no escribe curvas (§3) |
| que la re-medida es un trámite | Es lo más probable que falle esta noche, con **6,6 veces** el margen en el 8. Y al segundo fallo la app deshace la escritura sola, sin preguntar (§5) |

Y una contradicción **entre los documentos del repositorio**, que dejo abierta sin elegir:

- **A-05** («cambiar de cola resucita una serie que se mandó Rehacer»): la P16 lo da por
  **CONFIRMADO ALTO** (`:289-320`), la QA de la 3.6.17 dice **NO REPRODUCIDO** y la contradice
  expresamente (`QA-App-3.6.17.md:272-287`), y la revisión de la rc2 se pone del lado de la P16
  (`:453`). **Se cierra midiendo.** Mientras tanto se rodea: no cambiar de cola (§6.1 y §6.3).
- **A-08** (el truncamiento de `SLV-002-2026` al cambiar la serie): la P16 lo da por riesgo abierto
  y dice «esto no está medido contra el equipo» (`:370-374`); la revisión de la rc2 lo declara
  **FALSO** (`:455`, `:471`). **Se cierra leyendo `#GN#` con el equipo delante.** Esta noche da
  igual: la serie no se cambia (§3.2).

---

## 10. Resumen en una tarjeta

1. **APK: la 3.6.17 que ya tiene.** No instale la rc4. No hay vuelta atrás y la campaña se borraría.
2. **No importe el ZIP** si usa el mismo teléfono. Ya está dentro.
3. **Equipo encendido 10 min, conectado, pruebas APTO.** El ZIP no escribe curvas.
4. **No cambie la serie. No toque el selector de banco.**
5. **PIN `2026`** (si nadie lo cambió). Cinco fallos lo bloquean hasta apagar.
6. **Marque sólo el 8** → nombre y nota → `Calibrar todo` → apagar/encender → re-medida de P43 →
   apagar/encender → **lea el acta** → `Aceptar`.
7. **Después, sólo la b** → re-medida de **P49** → lea el acta → `Aceptar`.
8. **La re-medida va a fallar el criterio de la x.** Mire la mitad de la R: si dice `OK`, es el
   umbral. Tiene **dos intentos**; al segundo la app deshace la escritura sola y no se pierde nada
   más.
9. **En el acta de la b, escriba que PA-24 no se invoca.**
10. **Si el enlace cae al grabar la fecha: reconecte y `Aceptar` otra vez. NUNCA `Rechazar`.**
11. **Envíe el informe y los ZIP.** Apunte el md5.

---

*Escrito el 19-sep-2026 por la noche a partir del informe de las 18:11, del fuente de la app en
`D:\IT\P_RetroVertical_V3.6\03_App_Movil\RetroV36\` (main, HEAD `1247aae`; el fuente no ha cambiado
desde `f5145ed`, que es el de la 3.6.17), del fuente del firmware en
`01_Firmware/RetroVertical_V3.6.X/`, y de la rama `rtv-1.0` en `D:\IT\wt_rtv10` **en sólo lectura**.
No se tocó ningún fichero de esos árboles y no se hizo commit. **Nada está validado contra el equipo
físico.***
