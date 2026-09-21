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

## Regla que sale de estas vueltas

Una condición de arquitecto o QA es fuente de una prueba sólo si está escrita aquí; una prueba cuyo
esperado venga de un informe no archivado fija comportamiento, no requisito (`CLAUDE.md` §7).
