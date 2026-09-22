# Revisiones de las apps V3.6 (arquitecto-iot y qa-istqb)

**Nada de esto está medido en un teléfono ni en un equipo: todo es JVM contra simulador y lectura de
código.** Registro de los veredictos y de las condiciones que las pruebas citan como fuente. Los informes
completos quedaron en el directorio temporal de la sesión; aquí se archiva lo que manda.

## App de calibrar (`03_App_Movil/RetroV36`, rama `rtv-1.0-cierre`)

- **Cov_3.6.1_calibrar** — arquitecto NO APTO, QA NO APTO. H-1 salida al Banco; H-2 re-medida por
  s_rep; H-3 fecha; M-1 texto de "listos".
- **Cov_3.6.2_calibrar** — NO APTO / NO APTO. A-1 la b seguía con s_rep; A-06 fecha al rechazar; resumen
  engañoso; MAX_NO_VALIDAS sin restaurar.
- **Cov_3.6.3_calibrar** — NO APTO / APTO con condiciones. H-1: un plazo vencido en `#GC#` borraba la
  fecha buena al rechazar (RF-COV-18/19); H-2 acta de la b.
- **Cov_3.6.4_calibrar** — APTO con condiciones / APTO con condiciones. Número de código en fallos;
  `calibrarInterno` de 110 líneas; `DATO,decisiones` de la b.
- **Cov_3.6.5_calibrar** — **APTO con condiciones / APTO. Entregable a Diego.** Pendiente: número de
  código en mensajes de fallo (`FlujoCalibracion.java:1190,1230,1452-1456,1499-1502`) antes de darla a
  un operador de Coviandina.
- **Cov_3.6.6_calibrar** (`4ea680c`) — arquitecto NO APTO; QA parada. Siguen llegando números de código al
  operador: restauración (`Ops.java:237`), `#S,k` de `escribir` (`FlujoCalibracion.java:1261`), heredados
  (`:1087`, `:1102-1104`), protocolo y re-medida (`:819`, `:1313`). Las pruebas sólo buscaban "código 8".
- **Cov_3.6.7_calibrar** (`e9d6e65`) — arquitecto NO APTO, **bucle declarado** (segunda corrección fallida
  sobre RF-COV-17/21); QA parada. C1-C4 de la 3.6.6 cerradas. Siguen: el acta en pantalla (`txtActa`,
  `CalibrarActivity.java:113-115,370-372`) sin guarda de corto, y `motivoNoEscribir` alcanzable en corto
  (`FlujoCalibracion.java:836-837`, filtro `"código " + k`). Propuesta del arquitecto para romper el bucle:
  ocultar el acta en corto y un único filtro en la frontera de la app corta, no más arreglos punto a punto.

## App de usuario (`03_App_Movil/RetroUsuario`)

- **0.2.0** — NO APTO / NO APTO. A1 dos series concurrentes; A2 MAC de otro equipo; A3 trama cortada
  por el plazo; A4 exportar en API 24-29; `exigir_362` sin usar; `firmware_v` sin máscara.
- **0.3.0** — APTO con condiciones / APTO con condiciones. Reintentos de sonda ofrecidos, no silenciosos;
  150 ms entre `#V#` y `#GN#`; `_2`/`_3` en MediaStore; giro de pantalla; exportar sin sesión; caso mixto
  de sonda; GPS 2 min / 50 m; reproducibilidad por contenido.
- **0.3.1** — APTO con condiciones / APTO con condiciones.
  - **C1:** tras NO_COMPATIBLE o reintentos agotados el socket queda abierto; reconexión fallida.
  - **C2:** el permiso de ubicación no se pide nunca en tiempo de ejecución.
  - B-1 estado de reintento en la Activity; B-3 GPS del primer proveedor; B-4 `exigir_362=false` sin
    reintentos; `fuente.md5` desde blobs de git.
- **0.3.2** — APTO con condiciones / APTO con condiciones.
  - **ALTO:** `vivo()` = `socket.isConnected()` no detecta un equipo caído; se reutiliza un socket muerto
    y un V3.6 sale "no compatible" en bucle.
  - M-1 detección doble tras un giro; M-2 el enlace no se libera al salir con Atrás; B-3 enlace huérfano;
    cita a una sección inexistente en `PermisoUbicacion.java`.
- **0.3.3** — APTO con condiciones / APTO con condiciones.
  - **C1 (arq):** salir con Atrás durante "Conectando" deja `detectando` en verdadero para siempre.
  - **C2 (arq):** tras un giro, el resultado de la detección se pinta en la Activity destruida.
  - **C3 (arq):** SPEC atrasada; cerrada en `SPEC-App-Usuario-V3.6.md` §4 bis.
  - **QA:** quitar `marcarCaido()` del catch de `EnlaceBluetooth.leerSinParar` no lo detecta ninguna prueba
    (126/126 en verde): la capa Android no tiene arnés. Pide prueba con un `InputStream` doble y, en
    campo, reconexión tras apagar el equipo. El rojo por aserción invertida no vale.
- **0.3.5** — APTO con condiciones / APTO con condiciones. QA: 69/69, 141/141, APK reproducible bit a bit;
  recuento real 98 requisito / 43 comportamiento (no 100/41: dos pruebas de `EstadoDeteccionTest` no citan
  requisito). La contradicción del "límite de 2 repeticiones" en la SPEC queda corregida. Arquitecto: C2 de
  la 0.3.4 cerrada; RF-USR-04 r7 cumple.
  **C1 (Alto):** girar con "Repetir o Saltar" abierto deja el hilo de medida colgado en
  `MedirActivity.java:73-87` con el cerrojo de `SesionMedicion.medir`; no se vuelve a medir. SPEC §4 bis
  lo recoge ya ("Girar a mitad de una medida o de una pregunta").
- **0.3.4** — APTO con condiciones / APTO con condiciones.
  - QA: 68/68 `fuente.md5`, 134/134; roturas de `LectorDeFlujo` y `EstadoDeteccion.salir()` vistas en rojo;
    ningún requisito roto. Sin arnés: capa Android (giro, Atrás, socket real, GPS, exportación).
  - C1 cerrada (`finally` en `MainActivity.hiloConectarYDetectar`).
  - **C2 sigue abierta (Alto):** `runOnUiThread(this::restaurarInterfaz)` (`MainActivity.java:259`) corre
    en la Activity destruida tras un giro y consume el resultado de un solo uso; la nueva queda en
    "Conectando". El error tampoco se publica. Además, esa llamada se encola antes del `finally` que
    baja `detectando`: si el hilo principal la ejecuta antes, pinta "Conectando" y no recoge el resultado.
  - Anotado, no bloquea: `reintentarSonda` pinta sobre `this`; ventana entre `limpiar()` y
    `marcarDetectando(true)`; dos detecciones al salir y volver a entrar enseguida.

## Regla que sale de estas vueltas

Una condición de arquitecto o QA es fuente de una prueba sólo si está escrita aquí; una prueba cuyo
esperado venga de un informe no archivado fija comportamiento, no requisito (`CLAUDE.md` §7).
