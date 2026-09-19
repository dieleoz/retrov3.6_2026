# RUNBOOK — calibrar un equipo V3 con la V3.6, paso a paso

**Estado: escrito el 19-sep-2026 a las 09:50, a partir de SLV-002. Las fases 10 y 11 (ajuste y
escritura) no se han ejecutado todavía en ningún equipo**, y la app 3.6.5 no se ha probado en el
equipo. Pensado para el **segundo equipo V3**. Adaptado de
`D:\IT\P_RetroVertical_V4.6\RUNBOOK-desde-V3.6.md` (fases 0-12), con los comandos, pantallas y
versiones reales de la V3.

**Se ejecuta completo una vez por equipo físico (L-23).** Cada equipo tiene su línea base, su campaña
(atada a su serie y su MAC, un solo ZIP), su ajuste y su acta. Lo único que se reutiliza entre
equipos es el catálogo de patrones, el método, el firmware y la app. **Los coeficientes de SLV-002 no
se copian a ningún otro equipo.**

## Cómo mantener este runbook

- Se actualiza **a medida que se avanza**, no al final: cada fase que se ejecuta en un equipo deja aquí
  su resultado (o su desviación) antes de pasar a la siguiente.
- Las versiones vigentes (firmware, app, `.hex` y APK con md5 y commit) se cambian en la tabla
  "Versiones" en cuanto cambian en el `ROADMAP.md`. Una versión sin commit no es vigente.
- Cada fase lleva siempre: objetivo, puerta, acciones, criterio de salida y qué salió mal en SLV-002.
- Toda cifra lleva su fuente (fichero y línea, o log y línea) y, si es de estado, la hora de lectura
  (L-12). Una cifra corregida se deja escrita al lado de la buena.
- Las lecciones nuevas van a `D:\IT\P_RetroVertical_V4.6\APRENDIDO-DE-V3.6.md` (L-xx) y aquí sólo se
  enlazan por su número.
- **Las SPEC son por versión de hardware** (regla de Diego, 19-sep-2026). Este runbook cita las SPEC de
  la V3.6; la V4.6 tiene las suyas y su propio runbook.

## Versiones — 19-sep-2026, 09:50

| Pieza | Vigente | Fuente |
| :--- | :--- | :--- |
| Firmware | **V3.6.1**, `.hex` md5 `8736c05d0273fdda66d5988f472d41e1`, commit `8860445` | `01_Firmware/RetroVertical_V3.6.X/hex/RetroVertical_V3.6.hex.md5`; `CAMBIOS-V3.6.md` §6 |
| Firmware en curso | 3.6.2: añade `#FT#` (factor de temperatura a fábrica). Sin commit | `05_Documentacion/PROTOCOLO-V3.6.md` §3 |
| App | **3.6.4**, commit `090c84c`, APK md5 `efb0386b…`. La **3.6.5** (`ff66f93`, campaña guiada) está sin probar en equipo | `03_App_Movil/RetroV36/README.md` |
| Compilador | XC8 2.10, sin `-mdfp` | `CAMBIOS-V3.6.md:13-15` |
| Programador | PICkit 3 (firmware 01.56.09), IPE de MPLAB X 5.50 | `grabacion_V3.6.1_2026-09-19.log:3-5` |
| Patrones | P1-P31 (IV, IX, XI) y tipo I P32a-P50 | `06_Calibracion/patrones_certificados_P1-P31.csv`; asset de la app |

## Resumen

| Fase | Qué | Estado en SLV-002 (19-sep, 09:50) |
| :--- | :--- | :--- |
| 0 | Identificar el equipo | Hecha, tras un error de partida (L-01) |
| 1 | Línea base antes de tocar | A medias: sin Bluetooth en P4 |
| 2 | Lectura ICSP | Hecha: chip protegido, sin copia |
| 3 | Autorización del propietario | Hecha, 18-sep |
| 4 | Grabación | V3.6 el 18-sep; V3.6.1 el 19-sep 09:36 |
| 5 | Verificación tras grabar (G4) | Cerrada para la V3.6; **pendiente para la V3.6.1** |
| 6 | Pruebas del equipo (APTO) | Hecha con la app 3.6.2 |
| 7 | Mapa de botones STONE | Sin hacer |
| 8 | Preparación de la campaña | Hecha a medias |
| 9 | Campaña de patrones | **En curso** |
| 10 | Ajuste | **Pendiente** |
| 11 | Escritura, `#E` y acta | **Pendiente** |
| 12 | App de producción, informe y registros | **Pendiente** (P9-P12 del `ROADMAP.md`) |

Las fases 3 a 7 de "desarrollo" del runbook de la V4.6 (SPEC, compilación reproducible, firmware,
app) **no se repiten** para el segundo V3: el firmware y la app ya existen. Sólo se repiten si hay que
cambiar el firmware.

---

## Fase 0 — Identificar el equipo

- **Objetivo.** Saber qué hay delante antes de enviarle nada.
- **Puerta.** Ninguna.
- **Acciones.**
  1. Abrir y fotografiar: micro (debe ser **PIC18F47K42**), placa (**"SATLUX H-IoT"**), pantalla
     (**STONE STA035WT-01**) (`HISTORIA.md:16-17`). Si el micro es un 18F4550, **este runbook no
     aplica**.
  2. Anotar serie, cliente, nombre Bluetooth (`COVIANDINA_<serie>` en SLV-002) y MAC.
  3. **No identificar por la pantalla**: sus textos son imágenes y no dicen qué firmware lleva.
  4. Crear `06_Calibracion/<serie>/` con el acta de identificación.
- **Criterio de salida.** Acta con micro, placa, pantalla, serie, cliente, nombre y MAC.
- **Qué salió mal en SLV-002.** Se dio por 18F4550 porque sus textos estaban en el fuente CCS, y la
  SPEC se empezó sobre esa base (L-01).

## Fase 1 — Línea base antes de tocar

- **Objetivo.** Registrar cómo mide el equipo **tal como llegó**, por pantalla y por Bluetooth.
- **Puerta.** Fase 0 cerrada. **Nada conectado al ICSP.**
- **Acciones.**
  1. Encender y esperar **10 min** (`PROCEDIMIENTO-Calibracion-V3-K42.md` §4; cifra del procedimiento,
     no medida).
  2. **Por pantalla:** OTROS PAPELES → tocar la barra de color hasta BLANCO (empieza en AMARILLO:
     amarillo → blanco → rojo → azul → verde → naranja; `PROCEDIMIENTO` §1.4 bis). 3 disparos con el
     gatillo sobre al menos cinco patrones de todo el rango. En SLV-002: P3, P2, P28, P1 y P7.
  3. **Por Bluetooth**, app RTV V3.6 → Pruebas → **"Línea base"**, forzando **"V3 2020 sin e"** si la
     detección falla. Envía sólo `1`-`8`, `a`-`d` y `9`: T-B03 (12 códigos sobre un patrón), T-B09
     (batería ×3), T-B07 (12 códigos en oscuro). Sale `lineabase_<serie>_<fecha>.csv`.
  4. **Nunca `e` a un equipo sin identificar** (L-14): en SLV-002 dejó el Bluetooth mudo hasta apagar.
     La app 3.6.x lo impide (`Tramas.peticionPermitida`). Tampoco tramas con `@` distintas de
     `@LEERV,BLA,1@`.
- **Criterio de salida.** CSV de línea base y registro de tramas guardados **antes** de conectar el
  PICkit; al menos cinco patrones de zonas distintas de `x`, 3 lecturas cada uno.
- **Qué salió mal en SLV-002.** P4 por Bluetooth no llegó a medirse; la zona alta no es concluyente por
  pantalla (1 punto = 13-19 cuentas de `x`); tras grabar, la zona x = 1700-2300 leyó 80-90 cuentas
  menos con las mismas fórmulas (`ACTA-antes-y-despues-grabacion.md`; L-15).

## Fase 2 — Lectura por ICSP

- **Objetivo.** Guardar lo que lleva el chip y saber si está protegido.
- **Puerta.** Fase 1 cerrada. Visto bueno del propietario a **conectar** el programador.
- **Acciones.**
  1. PICkit 3 al conector `PICKIT3` de 6 pines. Si da "Connection Failed" **incluso sin placa**, está
     en modo scripting: PICkit 3 Programmer v3.10 → Tools → *Revert to MPLAB mode* (L-03).
  2. Leer con IPE 5.50: `C:\Program Files\Microchip\MPLABX\v5.50\mplab_platform\mplab_ipe\ipecmd.exe
     -P18F47K42 -TPPK3 -GF<serie>_lectura_<fecha>.hex`. **Esta orden no quedó registrada con SLV-002**:
     anotar aquí la que se use.
  3. Guardar el `.hex` y su md5 en `01_Firmware/lecturas_equipos/<serie>/`. Comparar la configuración
     con la de referencia `EC FF F7 FF 9F FF FF DF FE FF` (`SPEC-V3.6.md`, T-C01).
- **Criterio de salida.** Volcado con md5. Programa en ceros = "protegido (CP)", no "vacío".
- **Qué salió mal en SLV-002.** CP activo: sólo configuración, sin copia. El original se perdió al
  grabar (autorizado).

## Fase 3 — Autorización del propietario

- **Objetivo.** Visto bueno escrito del propietario de **este** equipo, sabiendo qué se pierde.
- **Puerta.** Fase 2 hecha (se sabe si hay copia).
- **Acciones.** Explicar que la V3.6 borra el firmware original y que, con CP, **no se recupera**.
  Anotar la frase literal, quién y cuándo, en el `ROADMAP.md` (tramo "Segundo equipo V3").
- **Criterio de salida.** Cita escrita con fecha.
- **En SLV-002.** Diego, 18-sep-2026, aceptando perder el original (`ROADMAP.md`, P6).

## Fase 4 — Grabación

- **Objetivo.** Grabar el `.hex` vigente, atado a su commit.
- **Puerta.** Fases 1-3 cerradas. `.hex` con md5 comprobado contra su `.md5` y su commit (G1).
- **Acciones.**
  1. `md5sum 01_Firmware/RetroVertical_V3.6.X/hex/RetroVertical_V3.6.hex` debe dar el md5 de la tabla
     "Versiones". Si no, no se graba.
  2. `ipecmd -P18F47K42 -TPPK3 -F<ruta del .hex> -M -Y` (L-14). Guardar la salida en
     `01_Firmware/lecturas_equipos/<serie>/grabacion_<version>_<fecha>.log`.
  3. Esperado: *Program Succeeded*. El "Verify failed" posterior leyendo ceros **es normal**: el `.hex`
     activa CP (`grabacion_V3.6.1_2026-09-19.log:20,34`).
  4. Commit del log con la versión, el md5 y la hora en el mensaje.
- **Criterio de salida.** Log con *Program Succeeded*, en un commit.

## Fase 5 — Verificación tras grabar (G4)

- **Objetivo.** Demostrar que el equipo mide como el firmware de 2020 con coeficientes de fábrica.
- **Puerta.** Fase 4 cerrada. **PICkit desconectado, equipo apagado y encendido.**
- **Acciones.**
  1. Por pantalla: los mismos patrones y el mismo menú de la fase 1, 3 disparos cada uno.
  2. App → Pruebas del equipo. Esperado: `#V#` → `#V,3.6,<fecha de compilación>,DEF,0000#` (V3.6.1:
     `2026-09-19`); `#GT#` y los 12 `#G` a fábrica a 4 ulp; **`#E` 60 de 60 exactos** (12 códigos ×
     x = 500, 1000, 2000, 3000, 4000); los 13 comandos responden; repetibilidad con `e` ≤ 10 cuentas.
  3. Anotar en el acta del equipo, con el registro de tramas.
- **Criterio de salida.** Como en SLV-002 el 19-sep (`ACTA-antes-y-despues-grabacion.md`, G4): `#E`
  60/60, `DEF`, repetibilidad s = 4,0.
- **Qué salió mal en SLV-002.** Se anotó P7 donde era P1 (L-18). La coherencia dio FALLO por criterio
  de la app (equipo movido a mitad de prueba); corregido en la 3.6.3 con el estado INVÁLIDA.

## Fase 6 — Pruebas del equipo: APTO

- **Objetivo.** Que el modo administrador quede desbloqueado sólo con el equipo en buen estado.
- **Puerta.** Fase 5 cerrada.
- **Acciones.** App → Pruebas, con el equipo **apoyado** sobre un patrón de nivel medio o alto, sin
  tocarlo. Orden 1, 2, 5, 3, 4, 6. Si sale INVÁLIDA, se repite; no es un fallo del equipo.
- **Criterio de salida.** **APTO** en el registro. NO APTO bloquea el modo administrador.

## Fase 7 — Mapa de botones de la pantalla (opcional)

- **Objetivo.** Saber qué código envía cada botón de **esta** pantalla STONE.
- **Acciones.** App → Botones de pantalla: `#KC#`, pulsar un botón, `#K#`; anotar la pareja. La app
  resalta el byte 8 (`bufferPantalla[8]`): OTROS PAPELES 0x01-0x06, TIPO I 0x07-0x0D, 0x0E prueba ADC.
- **Criterio de salida.** `botones_<serie>_<fecha>.csv`. **En SLV-002 no se ha hecho.**

## Fase 8 — Preparar la campaña

- **Acciones.**
  1. Marcar en cada patrón XI una flecha de orientación y medir siempre igual respecto al cabezal (L-22).
  2. Comprobar el catálogo de la app: tipo I P32a-P50; **P32 está duplicado** (P32a azul, P32b naranja),
     pendiente de confirmar con Diego antes de medirlo.
  3. App: disparos de asentamiento = 1 (por defecto); N = 9 disparos por serie.
  4. Equipo encendido 10 min antes de la primera serie; toda la campaña en el mismo régimen térmico.

## Fase 9 — Campaña de patrones

- **Objetivo.** Una `x` limpia por patrón, atribuible a **este** equipo.
- **Puerta.** Fases 6 y 8 cerradas. La `x` se lee con `e` (V3.6); sin Bluetooth, con el modo oculto
  "PRUEBA ADC" de la pantalla (`PROCEDIMIENTO` §1.4 bis).
- **Acciones.** App 3.6.5 → **"3. Campaña de calibración"**, modo guiado:
  1. La campaña se abre por serie + MAC. **Nunca** mezclar series de dos equipos.
  2. "Coloque P27 …" → colocar → OK → asentamiento + 9 disparos → veredicto. **Levantar la pistola sólo
     cuando la app dé la serie por terminada** (L-17).
  3. Si el veredicto no es OK: "Repetir", "Aceptar con nota" o "Era otro patrón". No corregir a mano (L-18).
  4. Orden de la cola: imprescindibles (huecos de `x`, series en conflicto), tipo I que ajustan
     (amarillo P34, P37, P43, P44; rojo P38, P39, P49), resto de tipo I, giro de P5 a 0° y 90°, lo demás.
  5. **"Exportar campaña"** → un solo `campana_<serie>_<fecha>.zip` (L-21). Copiarlo a
     `07 pruebas/<fecha>/` y hacer commit con su md5.
- **Criterio de salida.** Todos los patrones que entran en el ajuste con serie OK; ninguno imprescindible
  pendiente. Criterio provisional: s ≤ 10-15 cuentas por serie y dos series por patrón a menos de 3 s.
- **Qué salió mal en SLV-002.** Primer disparo bajo en 17/17 series (L-16); un descolgado en P7
  (3031 frente a ~3230, L-17); P24 con dos series contradictorias (2065 y 2443); 32 ficheros por
  compartir varias veces (L-21); XI desordenados respecto a su certificado (L-22). A las 09:50 faltan
  P27, P28, P24, P30 ×9, los tipo I y el giro de los XI.

## Fase 10 — Ajuste

- **Objetivo.** Curvas nuevas por código **para este equipo**, con el residuo por tipo declarado.
- **Puerta.** Fase 9 cerrada. C1-C5 (`SPEC-V3.6.md` §6 bis). Firmware ≥ 3.6.1 (límites de `#S`).
- **Acciones.**
  1. App → Modo administrador (`#L,<pin>#`; PIN de fábrica `2026`, `PROTOCOLO-V3.6.md` §3) → Asistente.
  2. **Opción C** (Diego, 19-sep-2026): códigos 1 y 2 por mínimos cuadrados con todos los patrones, sin
     ponderar por tipo. Verde, azul y rojo intensos sólo se comprueban. 8 y b si hay tipo I suficientes.
  3. Grado máximo = niveles certificados distintos − 2 (hasta 2); sin ajuste con rango certificado
     estrecho (`Asistente.cobertura`).
  4. La curva tiene que pasar el criterio de `#S` del firmware: `R(x)` en [0; 4000] para x = 600-4300
     (`CAMBIOS-V3.6.md` §7.1). La fábrica del código 2 **no lo pasa** (negativa desde x = 4175, L-19).
  5. Enviar la propuesta a Diego: coeficientes, residuo por patrón y por tipo, "como llegó" frente a
     "como sale".
- **Criterio de salida.** Propuesta aceptada por Diego por escrito.

## Fase 11 — Escritura, `#E` y acta

- **Puerta.** Fase 10 aceptada. Pruebas del equipo en APTO en la misma sesión.
- **Acciones.**
  1. Copia de los 12 juegos y la temperatura (`coeficientes_<serie>_<fecha>.csv`).
  2. `#S` por código; relectura `#G` a `ULP_S` = 8 (la ida y vuelta llega a 7 ulp, L-20). Si no
     coincide, la app restaura sola (C3).
  3. **`#E` en 5 puntos contra la curva enviada**: es la comprobación que manda.
  4. No escribir `#ST` con el texto de fábrica: deja el bit 12 en `CAL` (`CAMBIOS-V3.6.md` §7.3). Para
     volver a fábrica en temperatura, `#FT#` (firmware 3.6.2, en curso).
  5. `#Q#`. Volver a medir los patrones **con el código de cada color** (no con `e`), por Bluetooth y por
     pantalla. Acta de antes y después en `06_Calibracion/<serie>/`.
- **Criterio de salida.** `#E` idéntico a la curva enviada; `#V#` en `CAL` con la máscara esperada;
  acta con residuo por tipo, commit.

## Fase 12 — App de producción, informe y registros

Pendiente. Puertas P9-P12 del [`ROADMAP.md`](ROADMAP.md): validación del arquitecto, propuesta de app
de producción (revisando `RetroVerticalP1` del repositorio V5), APK de producción, PDF de "informe de
ajuste y verificación" y registros para la interventoría (vencimiento = calibración + 1 año). SPEC en
curso: `05_Documentacion/SPEC-Calibracion-V3.6.md` y
`05_Documentacion/SPEC-Registro-Indicador-Interventoria.md`.
