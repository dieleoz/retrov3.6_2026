# APRENDIDO — V3.6

Una entrada por lección. El proyecto siguiente la trae como `APRENDIDO-DE-V3.6.md`; las de la V3.6
anteriores al Orquestador están en `D:\IT\P_RetroVertical_V4.6\APRENDIDO-DE-V3.6.md` (L-01 a L-23).
Los errores concretos, con la errónea junto a la buena, en `HISTORIA.md`.

## A-01 — La app que lleva un teléfono se mira en su registro, no en la memoria
- **Pasó:** el procedimiento del 8 y la b decía "quédese con la 3.6.17" y el HONOR llevaba la rc3
  (`06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt:5`).
- **Lección:** antes de una instrucción de campo, la cabecera `# app:` del último registro de ese
  teléfono. Si una instalación falla, no se desinstala: se pierde la campaña.
- **Destino:** SUBE a `entregar` (instrucciones de campo con APK).

## A-02 — Un ZIP del que sale una curva se archiva el mismo día
- **Pasó:** el ZIP de soporte que sostiene el informe de ajuste estaba sólo en `07 pruebas/`, sin
  versionar (`06_Calibracion/SLV-002/campanas/HUELLAS.txt`, entrada del ZIP de soporte).
- **Lección:** lo que no está en el repositorio con su huella no existe como evidencia.
- **Destino:** SUBE a `entregar` §9.

## A-03 — Tomar datos para calibrar no es construir una curva
- **Pasó:** la toma creció a bancos de 80 y 180 pasos, series de 9 disparos y modo "preciso"; más de
  una hora por equipo y una app que un usuario no entiende (decisiones TOMA-50 y APP-SIMPLE).
- **Lección:** la curva (fórmula por color) se construye una vez con el banco completo; en cada
  equipo basta una toma corta y representativa. Tope: 50 tomas.
- **Destino:** SE QUEDA (y a la V4.6 por su `APRENDIDO-DE-V3.6.md`).

## A-04 — Una prueba que asevera no demuestra nada si el valor esperado salió del código
- **Pasó:** tres casos (`HISTORIA.md`, "Pruebas que no demostraban nada").
- **Lección:** cuenta el valor esperado que viene de fuera; al entregar, recuento separado y prueba
  nueva vista en rojo.
- **Destino:** SUBE a `verificar` §4 (hoy sólo en `CLAUDE.md` §7 de este repositorio).

## A-05 — Renumerar las secciones de `CLAUDE.md` rompe las citas `§n`
- **Pasó:** al reordenar `CLAUDE.md`, las skills y `HISTORIA.md` quedaron citando secciones que ya no
  eran las mismas; se corrigieron a mano.
- **Lección:** citar la regla por su título, o no renumerar.
- **Destino:** SUBE a `cerrar-sesion`.

## A-06 — Topes del método frente a los del proyecto
- **Pasó:** `medir_docs.sh` y el pre-commit ponen 1000 líneas a README y ROADMAP; Diego fijó 100 aquí
  (`.topes`). `medir_docs.sh` no lee `.topes`.
- **Lección:** el medidor y el hook deben leer los mismos topes.
- **Destino:** SUBE a `cerrar-sesion` (`medir_docs.sh`).

## A-07 — Contradicciones internas en las skills comunes
- **Pasó:** al pasar lo propio a `.claude/particularidades/` salieron: `entregar` §0 dice que en las
  particularidades va "el estado de hoy" y a la vez que la cifra del día no vive allí;
  `leer-planos-pcb` §0.3 y §6 llevan un estado fechado y cifras de otro proyecto;
  `verificar-pantalla-stone` no explica el formato del `.vt` de 2.ª generación.
- **Destino:** SUBE a `entregar`, `leer-planos-pcb` y `verificar-pantalla-stone`.

## A-08 — Tras compactar el contexto, las respuestas pasaron al inglés
- **Pasó:** después de la compactación, todas las respuestas a Diego salieron en inglés. Ninguna skill ni
  particularidad lo pide (búsqueda de "english"/"inglés" en `.claude/particularidades/` y en las skills
  del Orquestador: sin resultados). El resumen de la compactación, las instrucciones del arnés, las
  notificaciones de tareas y las salidas de herramientas están en inglés, y arrastraron el idioma.
- **Lección:** el idioma de respuesta es el de Diego (español), no el del contexto. Al retomar tras una
  compactación, comprobarlo en la primera respuesta; los encargos a subagentes, también en español.
- **Destino:** SUBE al método global ("Tono") y a la skill que retoma sesión.

## A-09 — En Android, una operación con hilo de fondo no puede ser de la Activity
- **Pasó:** de la 0.3.0 a la 0.3.5 de la app de usuario, cada revisión encontró un defecto de giro nuevo
  (pintar sobre una Activity destruida, "detectando" colgado, pregunta que muere con la pantalla). Cada
  arreglo sacaba un campo de la Activity, nunca la regla; el revisor Fable lo diagnosticó (FABLE-USR).
- **Lección:** ningún hilo lanzado por una Activity retiene `this`; el estado y los canales de una
  operación (medir, preguntar, detectar) viven en un objeto de proceso probado en JVM, y la pantalla sólo
  pinta lo que ese objeto sabe al quedar visible.
- **Destino:** SUBE a `verificar` (apps Android) y a la V4.6 por su `APRENDIDO-DE-V3.6.md`.

## A-10 — Antes de que el funcional toque el equipo, cinco comprobaciones
- **Pasó:** el 22-sep hicieron falta seis sesiones para escribir el 8 y la b. Las cinco primeras no
  escribieron nada y ninguna falló por el código: PIN que el operador no tenía (cuatro `ERR,PIN`, y el
  firmware se bloquea al quinto, `calibracion_v36.c:645-656`); dos veces T-C41 porque pulsó OK antes de
  apagar; y dos `#S,8` → `#ERR,FORMATO#` transitorios. Detalle en `HISTORIA.md`.
- **Lección:** antes de mandar a alguien a medir, por escrito y comprobado: **(1)** que tiene el PIN del
  equipo y sabe que el firmware se bloquea a los cinco fallos; **(2)** qué APK lleva su teléfono, de la
  cabecera `# app:` de su último registro (A-01); **(3)** que el ZIP está guardado en el almacenamiento
  del teléfono, no en el chat: la app lo abre con el selector del sistema y no recibe ficheros
  compartidos; **(4)** el orden exacto de los pasos que dependen de apagar el equipo, porque la app
  decide por lo que ve en el enlace, no por lo que el operador cree que hizo; **(5)** que exporte los
  ZIP **antes** de cerrar la app, y que si algo se atasca no la fuerce: se pierde el acta a medias.
- **Y una del diagnóstico:** un fallo que no se repite no tiene causa hasta que se mida. Ese día se dio
  por buena tres veces una causa distinta y ninguna resistió el registro.
- **Destino:** SUBE a `entregar` (instrucciones de campo) y a la V4.6 por `APRENDIDO-DE-V3.6.md`.

## A-11 — Un validador automático en verde no es un visto bueno
- **Pasó:** `coherencia_manual.py` dio "0 bloqueantes" tres veces sobre manuales que afirmaban 115200
  baudios donde el binario grabado dice 9600, 16 muestras donde el código promedia 15, 32 bits donde hay
  `double`, y que publicaban el PIN de fábrica. El script mira jerga y sellos, no cifras ni credenciales.
- **Lección:** el verde del script es una condición, no el veredicto. Quien escribe no valida, y el que
  valida abre el fichero citado y comprueba la línea. Un dato vive en la SPEC; el manual lo cita.
- **Destino:** SUBE a `manual` y a la V4.6 por `APRENDIDO-DE-V3.6.md`.
