# SPEC-V3.6 — Especificación del firmware y la app V3.6 del Retrorreflectómetro Vertical

**Estado: revisión 1.2 de la SPEC, 19-sep-2026, 10:30. Ninguna escritura de calibración se ha hecho
en un equipo, y C1 sigue sin cumplirse por la letra.** SLV-002 corrió la V3.6 (md5 `680b6a7d…`) desde
el 18-sep a las 20:24 y G4 pasó con ella el 19-sep a las 08:53; a las 09:36 del 19-sep se regrabó con
un `.hex` cuyo tamaño es el de la **V3.6.1** (commit `8860445`, md5 `8736c05d…`), **sin `#V#` leído
todavía** (C-36). La app es la RTV **3.6.4** (commit `090c84c`), con 56 pruebas JVM en verde
(ejecutadas en este trabajo). El estado de cada requisito frente al código, con `archivo:línea`, está
en [`MATRIZ-SPEC-codigo-V3.6.md`](MATRIZ-SPEC-codigo-V3.6.md), que sustituye a las tablas de §2.4 y
§3.8. El plan de pruebas ejecutable está en [`TDD-V3.6.md`](TDD-V3.6.md).

*Texto r1.1 del estado:* «revisión 1.1 de la SPEC, 18-sep-2026, 20:10. Nada se ha grabado ni medido en
un equipo con V3.6. El firmware está compilado (`.hex` md5 `680b6a7d…`, commit `319345f`) y probado
sólo en simulador; la app compila y pasa 45 pruebas JVM… Una prueba de simulador falla (T-A23…).»

**Revisión 1.3 (19-sep-2026, tarde) [NUEVO r1.3].** Añade RF-APP-33 a RF-APP-48 (§3.7 quater): el banco
guiado de los 133 patrones P1-P132, "Calibrar este equipo" con un solo botón, la serie leída de `#GN#`,
el acta persistente, la recuperación de P9-B12, la re-medida guiada, Importar visible, el aviso de no
desinstalar, el APK con la versión en el nombre, los códigos 3-6 ajustables, el aviso de batería con
la orden `9` y el `#V#` fresco tras escribir. **Nada de eso está implementado ni medido.** Estado real
de SLV-002 desde las 12:23 del 19-sep: firmware 3.6.2; códigos 1 y 2 escritos y re-medidos; acta
aceptada; `#SC` 2026-09-19; persistencia sin comprobar (`SPEC-Calibracion-V3.6.md` §12.0). La
especificación de calibración que acompaña a esta revisión es `SPEC-Calibracion-V3.6.md` §12.

**Revisión 1.2 (19-sep-2026).** Reconcilia la SPEC con el firmware 3.6.1, la app 3.6.4 y las medidas
de campo del 19-sep (`07 pruebas/19092026_0900/`). Lo que cambia lleva **[MOD r1.2]**, con el texto
anterior al lado; lo nuevo, **[NUEVO r1.2]**. Requisitos nuevos: RF-FW-31 y RF-APP-28 a RF-APP-32.
Requisitos de la campaña: RF-APP-32. Contradicciones nuevas: C-36 a C-47 (§9).

**Cómo leer la revisión 1.1.** No se ha borrado ningún requisito. Lo que cambia lleva la marca
**[MOD r1.1]** con el texto nuevo, y a continuación *Texto r1.0:* con el anterior. Lo nuevo lleva
**[NUEVO r1.1]**. El estado de cada requisito frente al código (implementado / parcial / no), con su
`archivo:línea`, está en las tablas de §2.4 y §3.8. Las condiciones de la revisión de arquitectura
(G1-G5 para grabar, C1-C5 para calibrar) están en §6 bis. *No confundir* las condiciones C1-C5 con las
contradicciones C-01…C-35 de §9, que llevan guion y dos cifras.

**Qué manda sobre qué.** **[MOD r1.1]** `PROTOCOLO-V3.6.md` es el contrato y **su revisión 1.1 (§4
bis) manda sobre su versión 1.0 y sobre esta SPEC**. Esta revisión de la SPEC integra la 1.1: las
observaciones O-01 a O-10 de §7 quedan resueltas por ella (salvo O-09, que la 1.1 no trata). Donde el
código y el contrato difieren, se anota en §9 y se propone decisión; si la decisión cambia el
contrato, la toma el propietario del protocolo.
*Texto r1.0:* «`PROTOCOLO-V3.6.md` es el contrato y **esta SPEC no lo cambia**: donde el protocolo
tiene un defecto, se anota como *observación al protocolo* (§7) y el requisito afectado queda marcado
como bloqueado hasta que el propietario del protocolo decida. Si esta SPEC y el protocolo se
contradicen, manda el protocolo.»

**Fuente citado.** **[MOD r1.1]** En §1, `archivo:línea` se refiere a la base
`01_Firmware/base_2020_d089f962/RetroVertical1.X/` (md5 del `.hex` en el árbol de trabajo, con CRLF:
`d089f9625090a1213291c090eae7ac01`; ver C-32). En §2.4, §3.8 y §6 bis se cita **el código actual**:
firmware `01_Firmware/RetroVertical_V3.6.X/` (sin prefijo) y app
`03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/` (sin prefijo), leídos entre las 19:50 y
las 20:05 del 18-sep-2026. **La app seguía cambiando mientras se escribía esto**: una línea citada
puede haberse movido; el nombre del método citado junto a ella es la referencia estable.
*Texto r1.0:* «Salvo que se diga otra cosa, `archivo:línea` se refiere a
`01_Firmware/base_2020_d089f962/RetroVertical1.X/` (fuente de 2020 sin tocar; su `.hex` tiene md5
`d089f9625090a1213291c090eae7ac01`, comprobado el 18-sep-2026). **No se ha leído
`RetroVertical_V3.6.X`**, que está en obras.»

Las referencias a la app de campo son de
`D:\@Proyect\IT\P_RetroReflectometro_Vertical\03_App_Movil\RetroVerticalP1\app\src\main\java\com\example\retrohorizontalp1\`,
y las de su especificación, de `...\05_Documentacion\SPEC-06-App-Movil-Funcional.md` (en adelante
SPEC-06).

**Repositorio.** **[NUEVO r1.1]** `D:\IT\P_RetroVertical_V3.6\`, con remoto privado propio
`github.com/dieleoz/retrov3.6_2026` (comprobado con `git remote -v` el 18-sep-2026). Ya no es "git
local sin remoto".

**Método.** Primero se especifica lo que hace 2020 (§1), después lo que añade la V3.6 (§2 y §3). Todo
lo que 2020 hace y la V3.6 conserva, **defectos incluidos**, está escrito como tal: la regla de la
V3.6 es que con la EEPROM en `DEF` el equipo se comporte igual que el de 2020.

---

## 1. Lo que hace el firmware de 2020 (línea base)

| Hecho | Dónde |
| :--- | :--- |
| UART1 a 9600 8N1 nominal (9592 reales): Fosc 16 MHz, `U1CON0 = 0xB0`, `U1BRG = 0x01A0` | `mcc.c:74`, `uart1.c:115,124,127` |
| **Cualquier byte recibido dispara una medida**, sea o no un código válido | `gui.c:295` |
| Buffer de recepción de 50 bytes **sin comprobación de límite**; el vaciado por tiempo existe pero está desactivado | `uart_module.c:19,51-57`, `:27-39`, `:109` |
| Medida: luz 15 encendida, 400 ms, 16 muestras (se descarta la primera), filtro IIR α = 0,995 sobre 599 muestras, apaga y **suma 200** | `measurement.c:222-247` |
| `x = (unsigned int)((ADC + 200) × (X_2·T² + X_1·T + X_0))`, con `X_2 = 0`, `X_1 = 0,00043212`, `X_0 = 0,90148651`. **La `x` es entera**: se trunca al asignarla a `reflectivityValue` | `gui.c:30,41-43,298-300` |
| `T` se lee del canal `INREFLECT30`, filtrada (α = 0,98, 200 muestras) y dividida por 4,928. Qué sensor es y en qué unidades, **sin verificar** | `measurement.c:64-73` |
| 12 polinomios de grado 2 o 3, uno por código, evaluados en `float` de 32 bits (el proyecto fija `double` = 32 bits) | `ecuacionesCalibracion.c:3-42`; `nbproject/configurations.xml:182-185` |
| Tres parejas idénticas: naranja (`:9-11` = `:31-33`), azul (`:15-17` = `:37-39`), verde (`:18-20` = `:40-42`) | ídem |
| El resultado se trunca a `unsigned int`; **si pasa de 4000 se pone a 0** | `gui.c:30`, `ecuacionesCalibracion.c:49-54` |
| Un resultado negativo se convierte a `unsigned int`: **comportamiento indefinido en C**. Lo previsible es un número enorme que `arreglar_dato()` pone a 0, sin comprobar sobre el código generado | `ecuacionesCalibracion.c:49-54` |
| Respuesta: `::<n>` con `%u`, **sin terminador**, seguida de un pitido de 80 ms | `ecuacionesCalibracion.c:56-67`, `gui.c:45-56` |
| Códigos por Bluetooth: `1`-`6` intensos, `7`, `8`, `a`-`d` opacos; `e` devuelve la `x` sin ecuación (pero sí con el corte a 4000) | `ecuacionesCalibracion.c:137-198` (`e` en `:191-193`) |
| `9`: responde `:<n>:` con el nivel de batería. **Roto**: parte de un valor ya reescalado, y `nivel[2]` desborda con dos cifras | `gui.c:267,328-340,349-350` |
| **[MOD r1.1]** Tras cada medida: `::<n>`, pitido de 80 ms (cede el turno), `clearBuffer()` y **después** 500 ms fijos. Se pierde lo recibido **antes** de `clearBuffer()` (durante la medida y durante el pitido); un byte que llega **en la pausa de 500 ms** queda en el buffer y **dispara otra medida** al volver al bucle. Es comportamiento de 2020 y se conserva. *Texto r1.0:* «Tras cada medida: se borra el buffer y se esperan 500 ms fijos. Lo recibido durante la medida **se pierde**» | `gui.c:342-346`; `ecuacionesCalibracion.c:63-64` |
| **[NUEVO r1.1]** El vaciado del buffer a los 2 s (`taskUartTimeout`) **está desactivado** y el perro guardián **apagado** (`WDTE = OFF`): nada recupera el equipo de un estado de recepción colgado | `uart_module.c:109`; `mcc_generated_files/device_config.c:77` |
| **[NUEVO r1.1]** **Gatillo pulsado + código Bluetooth:** el gatillo aplica primero la ecuación del color de pantalla; después el código aplica la suya sobre ese resultado. Con `e`, la respuesta **ya no es la `x`**, sino la ecuación de pantalla aplicada a la `x` (salvo que el color de pantalla sea 0x0E o no tenga rama). Es el equivalente en el V3 de la mejora M6 del V5 | `gui.c:311-327` (V3.6 `:312-328`) |
| Pantalla STONE por UART2: el código de color es `bufferPantalla[8]`, 0x01-0x08 y 0x0A-0x0E. **0x09 no tiene rama**. 0x0E = sin ecuación | `ecuacionesCalibracion.c:70-134` |
| Con el **gatillo** se aplica la ecuación del color de pantalla y el valor sale en pantalla (variables 199, 205, 210, 215), no por Bluetooth | `gui.c:284-293,311-315` |
| El color elegido en pantalla **no se borra nunca** (`borrarBuffer()` vacía). Si el gatillo está pulsado cuando llega un código Bluetooth, **la ecuación se aplica dos veces** (la de pantalla y la del código) | `uart_stone.c:32-35`, `gui.c:311-327` |
| Recepción UART2 sin límite, con `__delay_ms(20)` bloqueante | `uart_stone.c:87-107` |
| Batería a la pantalla (variable 207) cada 200 vueltas del bucle | `gui.c:348-357` |
| 2 s fijos tras el arranque antes de entrar en el bucle de medida | `gui.c:508-509` |
| La EEPROM guarda modelo, propietario, serie y geometría; las direcciones definidas llegan hasta 70 | `eeprom_manager.h:9-23` |
| Escribir un byte de EEPROM **deshabilita las interrupciones** hasta que acaba la escritura | `mcc_generated_files/memory.c:172-192` |
| Recepción UART1 por interrupción con anillo de 64 bytes | `mcc_generated_files/uart1.c:57,157` |
| Protección de código activa, también de la EEPROM de datos | `mcc_generated_files/device_config.c:97` |
| Conversión del ADC a 12 bits recompuesta a mano: **regenerar MCC la borra** | `mcc_generated_files/adcc.c:159` |

**Correspondencia código ↔ lámina** (de la pantalla STONE, `PROCEDIMIENTO-Calibracion-V3-K42.md`
§1.4 bis): "OTROS PAPELES" envía 0x01-0x06 (intensas) y "PAPEL TIPO I" envía 0x07-0x0D (opacas).

| Color | Intenso (otros papeles) | Opaco (papel tipo I) |
| :--- | :---: | :---: |
| Blanco | `1` / 0x01 | `7` / 0x07 |
| Amarillo | `2` / 0x02 | `8` / 0x08 |
| Verde | `3` / 0x03 | `a` / 0x0A |
| Rojo | `4` / 0x04 | `b` / 0x0B |
| Azul | `5` / 0x05 | `c` / 0x0C |
| Naranja | `6` / 0x06 | `d` / 0x0D |

**Forma de las 12 curvas de fábrica**, calculada en esta SPEC sobre los coeficientes de
`ecuacionesCalibracion.c:3-42` (aritmética, no medida):

- Todas dan **negativo por debajo de x ≈ 475-530**. Como `x ≥ 200` siempre (`measurement.c:247`),
  una lectura con poca señal entra en el comportamiento indefinido de la conversión.
- **Blanco intenso** (`:6-8`) tiene su máximo en x ≈ 3580 (el "techo de 806" de `HISTORIA.md`) y
  baja a partir de ahí.
- **Amarillo intenso** (`:3-5`) tiene su máximo en x ≈ 2784 y vuelve a ser negativo por encima de
  x ≈ 4175.
- **Naranja intenso** (`:9-11`) es creciente para todo x > 0 y no llega a 4000 en el rango del ADC:
  es la única de las intensas que se puede invertir sin ambigüedad en todo el rango útil. Por eso se
  usa `6` para reconstruir la `x` en un equipo sin `e`.

---

## 2. Requisitos del firmware V3.6 (RF-FW)

Cada requisito lleva su criterio de aceptación (CA) y la prueba que lo cierra (§5).

### 2.1 Compatibilidad con 2020

**RF-FW-01 — Enlace.** UART1 con la misma configuración que 2020 (`uart1.c:115,124,127`,
`mcc.c:74`). CA: un terminal a 9600 8N1 cableado a UART1 recibe `::<n>` legible. Pruebas: T-C02, T-C05.

**RF-FW-02 — Adquisición idéntica.** Luz, tiempos, número de muestras, filtro y `+200` idénticos a
`measurement.c:208-250`; factor de temperatura y truncado a entero idénticos a `gui.c:298-300`,
incluido el orden de las operaciones. **Los drivers MCC no se regeneran** (`adcc.c:159`). CA: el diff
de `measurement.c`, `adcc.c` y de las líneas 294-300 de `gui.c` contra la base sólo muestra cambios
justificados en `CAMBIOS-V3.6.md`; T-C08 dentro de tolerancia.

**RF-FW-03 — Códigos de medida.** `1`-`8` y `a`-`d`, un byte: mide, aplica la ecuación del código,
aplica el corte de `arreglar_dato()`, responde `::<n>` con `%u` sin terminador y pita 80 ms, como
`ecuacionesCalibracion.c:49-67,137-198`. CA: T-C05, T-C07.

**RF-FW-04 — Evaluación bit a bit igual a 2020.** Con los coeficientes de fábrica, la ecuación de
cada código da **el mismo `unsigned int`** que la función de 2020 para **toda** `x` entera de 0 a
65535. Para eso la V3.6 evalúa `((c3·x·x·x + c2·x·x) + c1·x) + c0` en `float` de 32 bits, en ese orden
(el de `ecuacionesCalibracion.c:3-42`), **no por Horner**. Un término que en 2020 no existe se evalúa
con coeficiente 0,0, lo que no altera el resultado (sumar +0,0 es exacto). CA: T-A20 en simulador,
cero diferencias en los 12 códigos.
**[MOD r1.1]** CA (condición **G2**): T-A20 en **MDB** (MPLAB SIM), 12 códigos × `x` = 0…65535, entre
las funciones de la base 2020 y la V3.6 **en `DEF`**: **0 diferencias**, con el informe archivado
(número de comparaciones y de diferencias por código, md5 del `calibracion_v36.c` usado y del arnés).
Si el `calibracion_v36.c` del commit de G1 no es el que se usó, T-A20 se repite.
*Texto r1.0:* «CA: T-A20 en simulador, cero diferencias en los 12 códigos.»

**RF-FW-05 — Valores de fábrica en ROM.** Los 12 × 4 coeficientes y los 3 del factor de temperatura
de fábrica se compilan **como literales con el mismo texto** que en `ecuacionesCalibracion.c:3-42` y
`gui.c:41-43` (así el compilador los redondea igual). `#F` los restaura desde ahí, nunca desde un
texto recibido. CA: revisión de código + T-A20 + T-C04.

**RF-FW-06 — Negativos y saturación.** El resultado negativo y el mayor de 4000 se comportan
**exactamente como en el código compilado de 2020**. Como el negativo es indefinido en C, el
comportamiento de referencia es el que midan T-A21 (simulador sobre el `.hex` de 2020) y T-B07 (SLV-002);
la V3.6 lo reproduce **de forma explícita** (sin depender de un comportamiento indefinido). CA: T-A21,
T-C07.
**[MOD r1.1]** La V3.6 **conserva la misma asignación `double` → `unsigned int` que 2020** (no la hace
explícita): con el mismo compilador (P3 cerrada) produce el mismo código, y T-A20 lo demuestra en todo
el rango, negativos incluidos (p. ej. `2` con x = 4300 da 65360 antes del corte, y 0 después;
`CAMBIOS-V3.6.md` §4.3). Hacerla explícita obligaría a adivinar qué hace el compilado de 2020 y abriría
la posibilidad de diferir. Modelo observado, a confirmar en T-A21: truncado hacia cero y módulo 2¹⁶.
CA: T-A20, T-A21, T-C07.
*Texto r1.0:* «…la V3.6 lo reproduce **de forma explícita** (sin depender de un comportamiento
indefinido).»

**RF-FW-07 — `e`.** Mide y responde `::<x>`: la `x` de RF-FW-02 tras `arreglar_dato()`, sin
ecuación (`ecuacionesCalibracion.c:191-193`). **Se conserva aunque SLV-002 no lo tenga.** CA: T-C06.

**RF-FW-08 — `9`.** Responde `:<n>:` con el mismo cálculo que `gui.c:328-340`. El desbordamiento de
`nivel[2]` (`gui.c:267`) se puede corregir **sólo si no cambia ningún byte emitido** para ninguno de
los valores posibles. Decisión pendiente (P-02). CA: T-C12.
**[MOD r1.1]** P-02 decidida en `PROTOCOLO-V3.6.md` §4 bis: `nivel[2]` pasa a `nivel[6]` (defecto de
memoria), sin cambiar ningún byte emitido. El formato `:<n>:` y el cálculo se conservan. CA: T-B09
(línea base), T-C12 (mismas respuestas).
*Texto r1.0:* «Decisión pendiente (P-02).»

**RF-FW-09 — Byte que no es código.** Un byte suelto que no sea `1`-`9`, `a`-`e` ni `#` dispara una
medida sin respuesta, como `gui.c:295`. CA: T-C13.

**RF-FW-10 — Pantalla STONE sin cambios.** Mismos códigos 0x01-0x0E en `bufferPantalla[8]`, mismas
ramas (0x09 sin rama, 0x0E sin ecuación), mismas variables (199, 205, 210, 215, 207) y mismos
tiempos que `ecuacionesCalibracion.c:70-134`, `gui.c:284-293,311-315,348-357`,
`uart_stone.c:87-138`. **El proyecto STONE no se toca.** CA: T-C14, T-C15.

**RF-FW-11 — Una sola tabla de ecuaciones.** El gatillo (pantalla) y el Bluetooth usan **la misma
tabla en RAM**, cargada de EEPROM o de fábrica. Un `#S` cambia las dos vías a la vez. CA: T-C19.

**RF-FW-12 — Defectos de 2020 que se conservan.** Por defecto se conservan: la doble ecuación con el
gatillo pulsado (`gui.c:311-327`), el color de pantalla que no se borra (`uart_stone.c:32-35`), la
pérdida de lo recibido durante la medida (`gui.c:342`) y la falta de rama 0x09. Cualquier corrección
necesita decisión escrita (P-02). CA: revisión de `CAMBIOS-V3.6.md`.
**[MOD r1.1]** P-02 decidida (`PROTOCOLO-V3.6.md` §4 bis): **se conservan** los defectos de
comportamiento (doble ecuación con el gatillo, color de pantalla que no se borra, pérdida de lo
recibido antes de `clearBuffer()`, medida disparada por un byte llegado en la pausa de 500 ms, 0x09 sin
rama, `9` con su cálculo); **se corrigen** sólo los de memoria (RF-FW-30). CA: revisión de
`CAMBIOS-V3.6.md` §2, T-C15.
*Texto r1.0:* «Cualquier corrección necesita decisión escrita (P-02).»

### 2.2 Órdenes nuevas (`PROTOCOLO-V3.6.md` §3)

**RF-FW-13 — Tramas `#...#`.** Un `#` abre trama. Mientras hay una trama abierta, **ningún byte
dispara medida** (tampoco los `1`-`8`, `a`-`e` que vayan dentro, como en `#G,1#`). Trama de más de 48
bytes: se descarta. Trama sin cierre: se descarta a los 2 s. CA: T-C16, T-C17.
**[MOD r1.1]** Límite de trama **96 bytes** con los dos `#` (O-01), en un buffer propio de 100
(`tramaAdmin`), separado de `bufferData`. Una trama de más de 96 se descarta **sin responder** y se
siguen descartando bytes hasta el siguiente `#`. El descarte de una trama sin cierre **no depende de
`taskUartTimeout`** (desactivada desde 2020, `uart_module.c:109` base) ni del perro guardián (apagado):
lo hace una comprobación de tiempo en cada vuelta del bucle principal, 2 s después del `#` que abrió
la trama. Consecuencia que se acepta y se documenta: **un `#` suelto se traga durante 2 s** todo byte
de medida que llegue detrás. CA: T-C16, T-C17 y **T-C36** (`#` suelto, 2,5 s, `1` mide y responde).
*Texto r1.0:* «Trama de más de 48 bytes: se descarta. Trama sin cierre: se descarta a los 2 s.»

**RF-FW-14 — Límite de recepción.** Ninguna secuencia de bytes escribe fuera del buffer de
recepción (hoy `uart_module.c:51-57` lo permite). CA: T-A22 (simulador) y T-C18.
**[MOD r1.1]** Se aplica a los dos buffers de UART1: `tramaAdmin[100]` (tramas `#`, hasta 96) y
`bufferData[50]` (bytes sueltos, hasta 49; el resto se descarta). **No se aplica a UART2**
(`bufferPantalla[50]`, sin límite desde 2020): se anota como observación no bloqueante O-11. El anillo
del driver UART1 es de 64 bytes (`mcc_generated_files/uart1.c:57`): una trama de 96 que llegue
mientras el bucle está bloqueado más de ~65 ms (bucle de 600 conversiones, escritura de EEPROM,
`__delay_ms(20)` de UART2) puede perder bytes; no bloqueante, se mide en T-C34. CA: T-A22, T-C18,
T-C34.

**RF-FW-15 — Trama recibida durante una medida.** El protocolo no lo dice (observación O-05). La
V3.6 **no mezcla** una trama con la medida en curso: o la atiende al acabar, o la descarta entera;
nunca la interpreta como bytes sueltos. CA: T-C20.
**[MOD r1.1]** Decisión O-05 del contrato 1.1: la trama `#` la analiza la tarea UART **en cuanto se
completa**, también con una medida en curso; **nunca dispara medida ni la borra `clearBuffer()`**. Por
tanto su respuesta **puede llegar antes** que el `::<n>` de la medida en curso. Los bytes sueltos
recibidos durante la medida se descartan, como en 2020. CA: T-C20 (esperado: `#V,...#` y `::<n>`, en
cualquier orden, y una sola medida).
*Texto r1.0:* «…o la atiende al acabar, o la descarta entera…»

**RF-FW-16 — `#V#`.** Responde `#V,3.6,<AAAA-MM-DD>,<CAL|DEF>#` sin medir. CA: T-C03.
**[MOD r1.1]** Responde `#V,3.6,<AAAA-MM-DD>,<CAL|DEF>,<máscara>#` sin medir. Fecha = `__DATE__` de la
compilación (O-07). Máscara: 4 cifras hex en mayúsculas; bit *i* = código *i* (orden `1`-`8`, `a`-`d`)
con algún coeficiente en RAM distinto, bit a bit, del de fábrica; bit 12 = factor de temperatura
distinto. **`CAL` si y sólo si la máscara no es 0**; esto difiere de la letra del contrato ("`CAL` si
hay calibración válida en EEPROM"): ver C-11, se propone adoptar la del código. CA: T-C03 (`DEF,0000`
en un chip recién grabado), T-C23 (`CAL,0001` tras escribir el código `1`), T-C25 (`DEF,0000` tras
`#F,*#`).
*Texto r1.0:* «Responde `#V,3.6,<AAAA-MM-DD>,<CAL|DEF>#` sin medir.»

**RF-FW-17 — Modo administrador.** `#L,<pin>#` con el PIN correcto abre la sesión; incorrecto
responde `#ERR,PIN#`. La sesión se cierra con `#Q#` o al apagar; **arranca siempre cerrada**. CA:
T-C21, T-C22.
**[MOD r1.1]** Además (O-06): **5 fallos seguidos** de PIN, en `#L` **o en `#P`**, bloquean `#L` y `#P`
hasta apagar (respuesta `#ERR,BLOQUEADO#`); un acierto pone el contador a 0. La sesión **caduca a los
10 min sin tramas `#`** (cualquier trama `#` la renueva, también `#V#`). Que `#P` cuente y quede
bloqueado va más allá de la letra del contrato (C-13); se adopta porque cierra la vía de probar PIN
por `#P`. CA: T-A31 (simulador), T-C21, T-C22, **T-C31** (bloqueo y caducidad en el equipo).
*Texto r1.0:* «La sesión se cierra con `#Q#` o al apagar; **arranca siempre cerrada**.»

**RF-FW-18 — Lectura.** `#G,<k>#` y `#GT#` responden con los valores en RAM, en el formato numérico
del protocolo, sin sesión de administrador. CA: T-C04.

**RF-FW-19 — Escritura.** `#S` y `#ST` sólo con sesión abierta (si no, `#ERR,BLOQUEADO#`). Rechazan
con `#ERR,FORMATO#` un número mal formado, NaN o infinito, y un `k` fuera de `1`-`8`, `a`-`d`.
Escriben RAM y EEPROM, **releen la EEPROM** y sólo entonces responden `#OK#`; si la relectura no
cuadra, `#ERR,EEPROM#` y la RAM queda con el valor anterior. CA: T-C23, T-C24. **`#S` está bloqueado
por O-01** (la trama no cabe en 48 bytes).
**[MOD r1.1]** O-01 resuelta (96 bytes): **`#S` ya no está bloqueado**. Condición **C4**: `#ST` rechaza
con `#ERR,FORMATO#` todo juego cuyo factor `F(T) = X_2·T² + X_1·T + X_0` salga de **[0,5 ; 1,5] para
algún `T` de 0 a 831**, que es todo el rango que puede tomar `T` (ADC de 12 bits, 0-4095, dividido por
4,928: `measurement.c:64-73`). Con el juego de fábrica, `F` va de 0,901 (T = 0) a 1,261 (T = 831).
Hoy el firmware sólo limita `X_0` (C4 parcial, ver §2.4). CA: T-A30, T-C24, **T-C32**.
*Texto r1.0:* «**`#S` está bloqueado por O-01** (la trama no cabe en 48 bytes).»
**[MOD r1.2]** C4 **cumplida en el firmware V3.6.1**, grabada en SLV-002 el 19-sep a las 09:36 y **sin verificar en equipo** (C-36, T-C32): `#ST` evalúa `F(T)` en `T` = 0, en `T` = 831 y en
el vértice si cae dentro, y rechaza con `#ERR,FORMATO#` todo valor no finito o fuera de [0,5 ; 1,5]
(`calibracion_v36.c:301-307,373-389,620-621`, commit `8860445`). T-A30 pasa en simulador (5/5 de la
TDD y 240 casos aleatorios frente a un barrido, `pruebas/T-A23_T-A30/resultado_T-A30.txt`). La frase
"Hoy el firmware sólo limita `X_0`" vale sólo para la V3.6 del 18-sep. Además, `#S` tiene límites
propios desde la 3.6.1: RF-FW-31.

**RF-FW-20 — Restaurar.** `#F,<k>#` y `#F,*#` restauran desde ROM (RF-FW-05). CA: T-C25.
Nota r1.1: `#F` no toca el factor de temperatura ni el PIN; no hay orden para devolver la temperatura
a fábrica salvo `#ST` con los valores de fábrica tecleados (O-13).
**[MOD r1.2]** Medido en simulador con la 3.6.1: `#ST` con el texto de fábrica **no** devuelve la
temperatura a `DEF`, porque `strtod` de XC8 lee `4,32120039E-04` y `9,01486659E-01`, que no son los
`float` de ROM, y la máscara queda en `CAL,1000` (`CAMBIOS-V3.6.md` §7.3; la máscara compara bit a bit,
`calibracion_v36.c:215-220`). Con la 3.6.1 **ninguna orden** devuelve la temperatura a `DEF`. Una orden
`#FT#` está en obra para una 3.6.2 (árbol de trabajo, sin commit a las 10:30): no cuenta hasta que haya
commit, `.hex` y prueba. C-30 sigue abierta.

**RF-FW-21 — PIN.** `#P,<actual>,<nuevo>#` con sesión abierta; el nuevo son 4 dígitos ASCII. PIN de
fábrica `2026`. CA: T-C26.

**RF-FW-22 — Formato numérico.** Emite notación científica con 7 cifras significativas y punto
decimal (`-8.654100E-05`); acepta además decimal simple (`0.6196681`). CA: T-A23 (simulador), T-C04.
**[MOD r1.1]** Emite **9 cifras significativas** (`%.8E`, p. ej. `-8.65410000E-05`) en `#G` y `#GT`;
acepta además decimal simple. Condición **C1**: la conversión **texto de 9 cifras → `float` → texto de
9 cifras** tiene que ser **exacta** (0 ulp de error en las dos direcciones) para los 51 valores de
fábrica y para 100 valores aleatorios de 1e-9 a 1e5 de los dos signos. **Hoy no se cumple**: la
biblioteca de XC8 2.10 se equivoca hasta en 2 ulp al imprimir y hasta en 3 al leer
(`CAMBIOS-V3.6.md` §3.3). Decisión propuesta: conversión exacta propia en la ruta de administración
(la de `CAMBIOS` §4.1), **antes del commit de G1** para no grabar dos veces. Mientras no se cumpla:
la calibración (P8) queda bloqueada y la app compara con la tolerancia de RF-APP-07. CA: T-A23, T-C04.
*Texto r1.0:* «Emite notación científica con 7 cifras significativas y punto decimal
(`-8.654100E-05`)…»
**[MOD r1.2]** Cifras corregidas con la medida de la 3.6.1 (`CAMBIOS-V3.6.md` §7.2): `%.8E` de XC8 se
equivoca **hasta en 3 ulp** (no 2) e ida y vuelta completa `#S` → `#G` llega a **5 ulp en simulador**
(2267 valores) y a **7 ulp con la emulación** de `strtof`/`efgtoa` de XC8, validada bit a bit contra el
simulador en esos 2267 valores y aplicada a 600 000 (6 ulp con `c2` de 1e-7 a 1e-6; 7 con `c3` de 1e-9
a 1e-6). `#G` de un valor de ROM, sin `strtod`: hasta 3 ulp en 600 000 casos. **C1 sigue sin
cumplirse.** La app lo rodea con `ULP_G = 4` y `ULP_S = 8` (`Ecuacion.java:92-93`, app 3.6.4) y con
`#E` en 5 puntos tras cada `#S` (`AdminActivity.java:498-512`), que compara enteros y no pasa por
texto. **Si ese rodeo sustituye a C1 no se decide aquí**: es la contradicción abierta C-47, para la
revisión de arquitectura.
*Cifras r1.1:* «hasta en 2 ulp al imprimir y hasta en 3 al leer».

**RF-FW-23 — Escritura de EEPROM y comunicaciones.** Como cada byte escrito deja las interrupciones
apagadas (`memory.c:181-188`), la respuesta `#OK#` sólo sale **después** de terminar todas las
escrituras. El tiempo total de un `#S` se mide y se anota (T-C23); la app no envía nada hasta recibir
la respuesta.
Nota r1.1: sólo se escriben los bytes que cambian, pero **el primer `#S` sobre un chip en `DEF` escribe
la cabecera y los 13 registros** (~238 bytes, del orden de 1 s con las interrupciones cortadas a
ratos), porque la EEPROM borrada está a 0xFF (`CAMBIOS-V3.6.md` §4.6). Se mide en **T-C35** (duración y
efecto sobre `getMillis()` y sobre la UART).

### 2.3 EEPROM, seguridad y construcción

**RF-FW-24 — Bloque en EEPROM.** Desde 0x100, con marca `V36`, versión, 12 × 4 `float`, 3 `float` de
temperatura, PIN y CRC-16. No escribe por debajo de 0x100 (las direcciones de 2020 llegan hasta 70,
`eeprom_manager.h:9-23`). Si la marca o el CRC no cuadran, carga fábrica y `#V#` da `DEF`. CA: T-A24
(simulador: bloque corrupto → `DEF`), T-C03.
**[MOD r1.1]** (O-04) Cabecera `V`,`3`,`6`,versión 1 en 0x100-0x103, sin CRC. **Un registro por
código** en `0x104 + 18·k` (k = 0-11, orden `1`-`8`, `a`-`d`): 4 `float` little endian + CRC-16, y un
registro 13 en 0x1DC con `X_2`, `X_1`, `X_0` y el PIN (4 ASCII) + CRC-16. **CRC-16/CCITT-FALSE** (poli
0x1021, inicio 0xFFFF, sin reflejar, sin XOR final; `"123456789"` → 0x29B1) sobre los 16 bytes de
datos, byte bajo primero. Cabecera mala: todo de fábrica (PIN `2026` incluido). CRC malo: **sólo ese
registro** vuelve a fábrica. CA: T-A24 (cabecera borrada → `DEF`; CRC de un registro alterado → sólo
ese código de fábrica; bloque válido → `CAL` con la máscara correcta), T-C03, T-C23.
*Texto r1.0:* «Desde 0x100, con marca `V36`, versión, 12 × 4 `float`, 3 `float` de temperatura, PIN y
CRC-16… Si la marca o el CRC no cuadran, carga fábrica…»

**RF-FW-25 — Nunca `@`.** Ninguna respuesta contiene `@`. CA: T-C27 (registro de todos los bytes
emitidos en la sesión de pruebas).

**RF-FW-26 — Configuración idéntica.** Bits de configuración iguales a `device_config.c` de 2020,
incluida `CP = ON` (`device_config.c:97`), y leídos en SLV-002 como `EC FF F7 FF 9F FF FF DF FE FF`.
CA: T-C01.

**RF-FW-27 — Cadena de compilación.** XC8 **2.10** y el proyecto MPLAB X de la base; `double` y
`float` de 32 bits (`configurations.xml:182-185`). Cada `.hex` entregado lleva su md5 y el del
fuente. CA: puerta P3 del `ROADMAP.md` y T-A25.
**[MOD r1.1]** P3 cerrada (la base compila idéntica, `CAMBIOS-V3.6.md` §1). Condición **G1**: el fuente
y el `.hex` que se graben están en **un commit** del repositorio; el `.hex` lleva al lado su md5
(`hex/RetroVertical_V3.6.hex.md5`) y el de cada fuente (`hex/fuente.md5`), y `CAMBIOS-V3.6.md` cita el
hash del commit. **El md5 se declara sobre los bytes que se entregan a IPE y se fija el fin de línea**:
con `core.autocrlf = true` y sin `.gitattributes`, git guarda el `.hex` con LF y el árbol de trabajo
lo tiene con CRLF, y **el md5 cambia** (C-32). Se exige `.gitattributes` con `*.hex -text` (o declarar
los dos md5). CA: T-A25, **T-A28**.

### 2.3 bis Requisitos de firmware nuevos en la revisión 1.1

**RF-FW-28 — `#E` [NUEVO r1.1].** (O-10) `#E,<k>,<x>#`, con `k` = `1`-`8`, `a`-`d` y `x` entero decimal
de 0 a 65535 (sólo dígitos, hasta 5), responde `#E,<k>,<R>#`: evalúa la ecuación `k` vigente en RAM en
esa `x`, **sin medir**, le aplica `arreglar_dato()` y devuelve el entero que saldría en `::<R>`. No
requiere sesión. No altera `reflectivityValue` de una medida en curso. `k` o `x` inválidos:
`#ERR,FORMATO#`. CA: T-A27 (simulador, contra las funciones de 2020), T-C05 (equipo, C5).

**RF-FW-29 — Registro de la pantalla `#K#` / `#KC#` [NUEVO r1.1].** Formato fijado aquí, tal como lo
emite el firmware (el contrato lo dejaba abierto): `#K,<n>,<h1>;<h2>;…;<h8>#`, con `n` = tramas UART2
leídas desde el arranque (decimal, `%lu`), y cada `<hi>` los bytes de una trama en **hex en mayúsculas,
dos cifras por byte, seguidos, sin espacios**, de la más antigua a la más reciente, separadas por `;`.
Una "trama" es lo que lee una llamada a `readUartStr2()` tras su espera de 20 ms: puede juntar dos
tramas STONE o partir una. Se guardan **hasta 16 bytes** por trama; el resto se descarta. Sin tramas:
`#K,<n>,#`. La respuesta **no tiene límite de longitud** (hasta ~280 bytes). `#KC#` vacía las ranuras y
responde `#OK#`; **`n` no se pone a 0**. El registro no altera `bufferPantalla` ni `bufferindice`.
CA: T-A33 (simulador), T-C33 (con la pantalla de SLV-002).

**RF-FW-30 — Defectos de memoria corregidos sin cambiar resultados [NUEVO r1.1].** (P-02, contrato
§4 bis) `tempSamples[15]` → `[16]` (la base escribe y lee el índice 15); `nivel[2]` → `[6]`; límite
comprobado en `bufferData`. Ninguno cambia un byte emitido ni la `x`: en la base, el índice 15 caía en
un bloque de 20 bytes sin uso (`CAMBIOS-V3.6.md` §2.2). CA: T-A29 (revisión del `.map` de la V3.6),
T-C08, T-C12.

**RF-FW-31 — Límites de `#S` [NUEVO r1.2].** Lo que hace la 3.6.1 y no pedía ninguna revisión: `#S`
rechaza con `#ERR,FORMATO#`, **antes de tocar RAM o EEPROM**, toda curva `R(x)` con algún valor no
finito, negativo o mayor que 4000 en algún `x` de **600 a 4300**. Se evalúa en los dos bordes y en las
raíces reales de `R'(x)` que caen dentro (`calibracion_v36.c:309-319,391-422,587-588`, commit
`8860445`). Origen de cada cifra: 4000, `arreglar_dato()` (`ecuacionesCalibracion.c:49-54`); 0, un
negativo sale 0 al pasar por `unsigned int`; 4300, ADC a fondo + 200 (`measurement.c:247`); **600 no
sale del código**: lo fijó el coordinador el 19-sep (`calibracion_v36.c:314-315`). Consecuencias
conocidas: (1) la curva de **fábrica del código `2` no cumple el límite**: es negativa desde x = 4175
(barrido en `float32` hecho en este trabajo: mínimo −176,9 en x = 4300; en simulador,
`#S,2,<fábrica>#` → `#ERR,FORMATO#`, `CAMBIOS` §7.3). No afecta a `#F,2#` ni a la ROM. (2) El rango de
`#S` no es el de C2 de la app (200-4400) ni el de la inversión (180-4400): C-42. CA: T-A30 (pasa en
simulador), **T-A41** (curva C de los códigos 1 y 2 frente a este límite), T-C37 (en el equipo, con la
3.6.1).

### 2.4 Estado del firmware frente a cada RF-FW [MOD r1.2]

**La tabla de la r1.1 se sustituye por [`MATRIZ-SPEC-codigo-V3.6.md`](MATRIZ-SPEC-codigo-V3.6.md)
§1**, leída contra el commit `8860445` (V3.6.1). Recuento: 27 cumplen, 2 parciales (RF-FW-14, UART2
sin límite; RF-FW-27, `CAMBIOS` no cita `8860445`), 1 no (RF-FW-22, C1). RF-FW-31 (nuevo) cumple.

Citas de la tabla r1.1 que ya no apuntan a lo que decían (se dejan escritas para que no vuelvan a
circular; la de la izquierda es la de r1.1, sobre `319345f`):

| Qué | r1.1 | r1.2 (`8860445`) |
| :--- | :--- | :--- |
| `#S` y deshacer | `calibracion_v36.c:503-513`, `:406-418` | `:580-592`, `:485-495` |
| C4 "sólo en `X_0`" | `:299-300,542` y `:536-548` | C4 completa: `:301-307,373-389,615-627` |
| `%.8E` / `strtod` | `:374-378` / `:335-345` | `:451-455` / `:354-364` |
| `#F` | `:514-526` | `:593-605` |
| `#G`, `#GT` | `:475-485,527-535` | `:552-562,606-614` |
| `#E`, `leerEntero` | `:486-502`, `:348-359` | `:563-579`, `:425-436` |
| `#P`, `pinValido` | `:549-563`, `:365-372` | `:628-642`, `:442-449` |
| `#V#` | `:201-222,380-398,448-456` | máscara `:202-223`, fecha `:458-475`, respuesta `:525-533` |
| Sesión y bloqueo | `:72-74,297-298,318-322,457-474,549-557` | `:73-75,181-182,298,337-341,534-547,628-636` |
| `#K#` | `:236-292,564-571` | `:236-292,643-650` |
| `aplicarEcuacion` | `:228-233` | `:229-234` |
| Literales de fábrica | `:33-46` | `:34-47` |

---

## 3. Requisitos de la app V3.6 (RF-APP)

### 3.1 Enlace y detección de versión

**RF-APP-01 — Enlace.** SPP (UUID `00001101-0000-1000-8000-00805F9B34FB`), como la app de campo
(`medicion.java:89,1661`). Una petición cada vez; la siguiente sólo tras la respuesta **y** al menos
1 s más (lo recibido durante la medida se pierde, `gui.c:342-346`). CA: T-A07.
**[MOD r1.1]** Pausa mínima antes de cada petición: **1500 ms desde el envío anterior y 600 ms desde el
último byte recibido**, lo que se cumpla más tarde; entre dos tramas `#` seguidas con un V3.6 ya
detectado, 150 ms (una trama `#` nunca mide, RF-FW-15). Motivo: el firmware borra lo recibido
80 ms después de emitir `::<n>` (pitido y `clearBuffer()`, §1) y **un byte que llega después, en la
pausa de 500 ms, sí dispara la medida siguiente**; 600 ms tras el último byte deja margen sobre esos
80 ms más la latencia del enlace. Valores provisionales hasta T-B06.
*Texto r1.0:* «…la siguiente sólo tras la respuesta **y** al menos 1 s más…»

**RF-APP-02 — Fin de respuesta.** `::<n>` y `:<n>:` no llevan terminador: la respuesta se da por
terminada tras un silencio (valor inicial 300 ms, a fijar en T-B06) o, en `#...#`, en el `#` de
cierre. Tiempo máximo de espera por medida: a fijar en T-B06 (valor inicial 5 s). **Nunca** se espera
`\r\n`. CA: T-A03.
**[MOD r1.1]** Valores iniciales: silencio **180 ms**, tiempo máximo **2500 ms** por medida y 2000 ms
por trama `#` (5000 ms para `#S` y `#F`). Se aceptan como provisionales porque la prueba 3 de la app
exige respuesta en < 2,5 s; se fijan con T-B06 (máximo observado de 50 medidas + 50 % de margen).
Una respuesta completa fuera de plazo cuenta como tiempo agotado. CA: T-A03, T-B06.
*Texto r1.0:* «…silencio (valor inicial 300 ms…)… (valor inicial 5 s).»

**RF-APP-03 — Detección de versión. MODIFICADO el 18-sep-2026 por dato de campo.** Con la app 3.6.0,
SLV-002 no respondió a `#V#`, `e`, `6` ni `@LEERV,BLA,1@` (registro de las 20:13). Sin embargo, en el
barrido de 255 bytes `6` respondía siempre, y **después de `e` (0x65) no volvió a responder ningún
byte**. Hipótesis, sin confirmar: en la variante de SLV-002, `e` deja el equipo sin Bluetooth hasta
apagarlo. **Nueva regla: nunca se envía `e` a un equipo no identificado como V3.6.** Orden vigente, el
de la app 3.6.1: `#V#` → `9` (`:n:` = V3 2020) → `6` → `@LEERV,BLA,1@` sólo si nada respondió.
"Con `e`" o "sin `e`" deja de detectarse por sonda: se registra como desconocido.

*Tabla anterior, retirada por ese motivo* (enviaba `e` en el paso 2). Al conectar, en este orden, con
la espera de RF-APP-01 entre pasos:

| Paso | Envía | Si responde | Conclusión |
| :--- | :--- | :--- | :--- |
| 1 | `#V#` | `#V,3.6,<fecha>,<CAL\|DEF>#` | **V3.6** (muestra fecha y estado) |
| 2 | `e` | `::<n>` | **V3 2020 con `e`** |
| 3 | `6` | `::<n>` | **V3 2020 sin `e`** (variante de SLV-002) |
| 4 | `@LEERV,BLA,1@` | `@LEERV,<n>@/n/r` | **V4**: se informa y no se opera (fuera de alcance, §6) |
| — | — | nada | Desconocido: no se opera |

En un V3 de 2020, `#V#`, `e` y `6` disparan una medida (`gui.c:295`): la luz se enciende y pita. Se
avisa al usuario antes de empezar. `@LEERV,BLA,1@` sólo se envía en el paso 4 y **ninguna otra trama
con `@`** (en el V4.1, una trama con `@` y sin `LEERV` deja el equipo sin Bluetooth). CA: T-A08, T-B01,
T-C03.
**[MOD r1.2] El párrafo siguiente queda RETIRADO (C-45).** Contradecía al propio RF-APP-03 [MOD
18-sep], que por dato de campo prohíbe `e` a un equipo no identificado. El código hace `#V#`, `9`, `6`,
`@LEERV,BLA,1@` (`Pruebas.java:247,281,289,297`, app 3.6.4), y eso es lo vigente. Se conserva como
texto retirado:
*Retirado:* **Decisión r1.1 sobre la desviación de la app: se mantiene este orden** (`#V#`, `e`, `6`,
`@LEERV,BLA,1@`), y el código actual ya lo sigue (`Pruebas.java:243,275,283,291`); lo que decía `9` era
el `README.md` de la app (C-09). Motivos para no usar `9` como segunda sonda: (1) `9` responde igual en
las dos variantes de 2020, así que **no distingue "con `e`" de "sin `e`"**, que es lo que decide cómo
se mide; (2) `9` también dispara una medida (`gui.c:296`), así que no ahorra nada; (3) su cálculo está
roto desde 2020 (`gui.c:332` parte de un valor ya reescalado en `:350-351`), así que una respuesta rara a `9` no
prueba nada. La detección cuesta como mucho tres medidas sin respuesta, con aviso previo.
Regla añadida: **ninguna petición de más de 49 bytes antes de que `#V#` haya respondido como V3.6**
(RF-APP-26).

**RF-APP-04 — Mapa color × tipo → código.** El de §1: tipo de lámina I → opaco (`7`, `8`, `a`-`d`);
tipos II a XI → intenso (`1`-`6`). CA: T-A09.

### 3.2 Medida de campo

**RF-APP-05 — Medir.** Envía el código de un byte de RF-APP-04, muestra el entero recibido. Un `0` se
muestra como "0 (saturado o negativo)", no como "sin señal" (`ecuacionesCalibracion.c:49-54`). CA:
T-B03, T-C05.
**[MOD r1.1] Aplazado a la fase de campo** (junto con PAR, §4): la app V3.6 de las puertas P5-P8 es
una herramienta técnica (pruebas, patrones, administración); en campo, SLV-002 sigue con la app del
cliente, que no cambia (`PROTOCOLO-V3.6.md` §1). Lo que las puertas sí necesitan, **enviar un código y
leer `::<n>`**, lo cubren la prueba 3 (`Pruebas.java:401-433`) y la línea base (RF-APP-25). El texto
"0 (saturado o negativo)" vale para los códigos `1`-`d`; **con `e` un `0` sólo puede ser saturación**
(RF-APP-06).
*Texto r1.0:* el requisito tal como está arriba, sin aplazar.

**RF-APP-06 — Registro de la `x`.** En V3.6 y en V3 2020 con `e`, la app puede registrar además la
`x` con `e` (modo técnico, no por defecto en campo, porque duplica el tiempo de medida). CA: T-C06.
**[MOD r1.1]** En la sesión de patrones la `x` es **lo único** que se registra (con `e`, o con `6`
invertido si no hay `e`), con su método; el emparejado "código de campo + `e`" se aplaza con RF-APP-05.
Reglas de lectura de `e`: (1) **`::0` con `e` sólo puede ser saturación** (`x` > 4000): `x` =
(ADC + 200) × `F(T)` ≥ 200 × 0,5 > 0 con el factor dentro de los límites de C4, así que nunca es
negativa; (2) **con el gatillo pulsado `e` no devuelve la `x`** (§1, `gui.c:312-328`): la app lo
advierte antes de cada serie y no tiene forma de detectarlo. CA: T-C06, T-C10.
*Texto r1.0:* el requisito tal como está arriba.

### 3.3 Modo de pruebas: "APTO / NO APTO"

Pantalla que **se ejecuta antes de dejar calibrar** y que también sirve en campo. Resultado único,
APTO o NO APTO, con el detalle de cada comprobación y un informe exportable (RF-APP-20).

**RF-APP-07 — Coherencia de las 12 fórmulas.**
- *V3.6:* lee `#G` de los 12 códigos y `#GT#`. En `DEF`, cada coeficiente debe coincidir con la tabla
  de fábrica de la app (RF-APP-08) con error relativo ≤ 1·10⁻⁶ (el formato de 7 cifras no permite
  exigir igualdad de bits, O-02). En `CAL`, deben coincidir con el último juego escrito desde la app,
  si lo hay en su registro; si no lo hay, se muestra y se marca "sin referencia".
- *V3 2020 (con o sin `e`):* sin lectura de coeficientes. Sobre un mismo patrón, pide los 12 códigos,
  invierte cada respuesta con la ecuación de fábrica (RF-APP-10) y comprueba que los 12 intervalos de
  `x` se solapan dentro de la repetibilidad medida. Es la prueba que en `HISTORIA.md` identificó las
  fórmulas de SLV-002.
- NO APTO si alguna no cuadra. CA: T-A10, T-B02, T-C09.
- **[MOD r1.1]** Tolerancia de comparación de coeficientes: **1 ulp de `float` de 32 bits** (contrato
  §4 bis, O-02) **sólo cuando T-A23 pase** (conversión exacta, C1). Mientras el firmware use la
  biblioteca de XC8 2.10 (±2 ulp al imprimir), la app **tiene que aceptar hasta 2 ulp** al comparar
  valores de ROM leídos con `#G`/`#GT`, o un chip recién grabado saldría NO APTO en la prueba 5 (C-12).
  Con la máscara a "fábrica", el resultado esperado de `#E` se calcula con **la tabla de fábrica de la
  app**, no con lo leído por `#G` (que puede estar a 2 ulp del valor que usa el equipo). Coherencia por
  inversión: tolerancia ≥ max(suelo de resolución, 2·s) (RF-APP-27).
  *Texto r1.0:* «…con error relativo ≤ 1·10⁻⁶ (el formato de 7 cifras no permite exigir igualdad de
  bits, O-02).»
- **[MOD r1.2]** Tolerancias vigentes, por medida y no por contrato: **`ULP_G = 4`** para `#G` frente a
  fábrica o frente a otra lectura `#G`, y **`ULP_S = 8`** para `#G` tras `#S` frente a lo enviado
  (`Ecuacion.java:92-93`, app 3.6.4). Motivo: `%.8E` de XC8 llega a 3 ulp y la ida y vuelta a 7
  (RF-FW-22 r1.2); con 2 ulp un chip recién grabado saldría NO APTO (G4 pasó el 19-sep con 4). La
  comprobación que decide es **por evaluación**: `#E` exacto contra la tabla de fábrica en los códigos
  a fábrica (`Pruebas.java:375-393`) y `#E` en 5 puntos contra la curva enviada tras `#S`
  (`AdminActivity.java:498-512`, ±1). La prueba 4 (coherencia) usa ahora una referencia con deriva entre
  la `e` inicial y la final, resolución local y un estado INVÁLIDA cuando el equipo se movió
  (`Coherencia.java:136-241`); la `e` inicial va precedida del disparo de asentamiento
  (`Pruebas.java:434-438`, RF-APP-28). Falta la referencia "último juego escrito" en `CAL`.
  *Texto r1.1:* «la app tiene que aceptar hasta 2 ulp».

**RF-APP-08 — Tabla de fábrica.** La app lleva los 12 × 4 coeficientes y los 3 de temperatura de
2020, copiados **del texto** de `ecuacionesCalibracion.c:3-42` y `gui.c:41-43`. Una prueba JVM los
compara contra ese archivo (T-A01) para que no se desvíen.

**RF-APP-09 — Repetibilidad.** 10 lecturas seguidas sobre un patrón sin mover el equipo, con `e` (o con
`6` e inversión si no hay `e`). Calcula media, desviación típica y coeficiente de variación. Umbral
de APTO: **a fijar con los datos de SLV-002** (T-B04); hasta entonces se muestra el valor sin
veredicto (P-04). CA: T-A11, T-B04.
**[MOD r1.1] Decisión sobre la desviación de la app (5 lecturas y umbral de 10 cuentas):** se hacen
**10 lecturas** (con 5, la desviación típica estimada tiene ±35 % de incertidumbre relativa; con 10,
±24 %; y `PROCEDIMIENTO` §4 pide 10). Se admite un **veredicto de avería**, no metrológico: NO APTO si
alguna lectura falla o si `s` > **10 cuentas de `x`**. Motivo para admitirlo antes de T-B04: el dato de
SLV-002 del 18-sep (8 blancos por pantalla, `x` de 1739 a 3271) da repetibilidad de ±1 cuenta, así que
10 cuentas es un fallo grosero, no un criterio de calidad. El umbral metrológico se fija tras T-B04 (P-04
sigue abierta para él). El estadístico es el de RF-APP-27: media de las lecturas válidas **de un mismo
método**, con `n` y `s` publicados. CA: T-A11, T-B04, T-C09.
*Texto r1.0:* «10 lecturas… hasta entonces se muestra el valor sin veredicto (P-04).»

**RF-APP-10 — Emulación de la aritmética del equipo.** La app evalúa las ecuaciones en `float` de 32
bits, en el orden de RF-FW-04, con truncado a entero y corte en 4000. La inversión de una respuesta
`n` devuelve **el conjunto de `x` enteras** con `f(x) = n`, no una raíz real. Con `6` (naranja
intenso, creciente) el conjunto es un intervalo de unas pocas cuentas; por debajo de x ≈ 475 no hay
inversión (resultado negativo). Riesgo: que la librería de coma flotante de XC8 no redondee como
IEEE-754 (R-05). CA: T-A04, T-A05, T-A26.
**[MOD r1.1]** La inversión **usa la misma emulación en `float` de 32 bits** que la comparación de `#E`
(`Ecuacion.respuestaFloat32`), no la evaluación en `double` por Horner: hoy `Inversion.intervalos` usa
`respuestaFirmware` (C-21), y las dos difieren en 1 unidad en algunas fronteras. El modelo de negativos
(truncado y módulo 2¹⁶, luego el corte a 4000) se ajusta a lo que mida T-A21; en el rango de uso
(`x` de 200 a 4400) todo negativo acaba en 0 por los dos caminos.
*Texto r1.0:* el requisito tal como está arriba.

**RF-APP-11 — Forma de las curvas.** Informa, por código, del máximo, de los ceros y del rango de `x`
en que la curva es creciente y no negativa. Para los valores de fábrica es **informativo**, no quita
el APTO (el techo del blanco y del amarillo intensos es un defecto conocido, §1). Para un juego que
vaya a escribir el asistente, es **bloqueante** (RF-APP-15). CA: T-A06.

**RF-APP-12 — Otras comprobaciones.** Respuesta a `#V#` y `#GT#`; lectura con la óptica tapada
(anotada como oscuro); que ninguna respuesta lleve `@`. CA: T-C09.

### 3.4 Medida de patrones P1-P31

**RF-APP-13 — Sesión de patrones.** Carga `06_Calibracion/patrones_certificados_P1-P31.csv` (patrón,
valor, tipo de lámina, color). Por patrón: 10 lecturas de repetibilidad y 3 recolocaciones de 5
lecturas (`PROCEDIMIENTO-Calibracion-V3-K42.md` §4), oscuro al principio y al final del bloque, hora
y temperatura ambiente anotada a mano. Con `e` en V3.6 y V3 2020 con `e`; **con `6` e inversión**
(RF-APP-10) en SLV-002 antes de grabar. Guarda cada lectura cruda (respuesta, código enviado, `x`
o intervalo de `x`). CA: T-B03, T-C10.

**RF-APP-14 — Avisos de cobertura.** Antes de ajustar, la app avisa de lo que el juego P1-P31 no
permite (`PROCEDIMIENTO` §3): verde, azul y rojo tienen un solo nivel (sólo verificar); naranja no
tiene patrones; **ninguno es de tipo I**, así que las ecuaciones opacas (`7`, `8`, `a`-`d`) no se pueden
ajustar con este juego (P-05). Y avisa si un mismo color mezcla tipos de lámina (IV, IX, XI) en una
sola curva (P-06). CA: T-A12.
**[MOD r1.1]** Rojo **no** tiene un solo nivel: tiene dos, muy próximos (P11 = 194 y P12 = 227, los
dos IV; `patrones_certificados_P1-P31.csv`). Con grado 1 exige 3 niveles (RF-APP-15), así que sigue
siendo "sólo verificar". La app lo dice bien (`Asistente.java:40`) y la SPEC r1.0 no (C-10). Resultado:
**sólo `1` (blanco intenso) y `2` (amarillo intenso) son ajustables con P1-P31.**
*Texto r1.0:* «…verde, azul y rojo tienen un solo nivel (sólo verificar)…»
**[MOD r1.2]** Desde el 19-sep hay **20 patrones tipo I** (P32a-P50, RF-APP-31), así que "ninguno es
de tipo I" ya no vale. La cobertura la calcula la app 3.6.4 **del catálogo**, no de una lista fija
(`Asistente.java:74-118`): cada código usa los patrones de su color y de su clase (tipo I → opacas;
II-XI → intensas, `:39-45`); grado máximo = niveles certificados distintos − 2, hasta 2; y si el rango
certificado es estrecho (< 20 unidades de R o < 30 % del mayor, `:33-36,93-95`), sólo se verifica.
Resultado con el catálogo actual: **ajustables `1` y `2` (grado 2), `8` (grado 2, cuatro niveles
64-122) y `b` (grado 1, tres niveles 46-81)**; `7` (un nivel), `a` (dos), `c` (7-10, estrecho) y `d`
(68-73, estrecho), sólo verificar. Esos umbrales de rango (20 y 30 %) **no salen de ninguna medida**.
El aviso de mezcla de tipos (P-06) sigue (`Asistente.java:306-312`).
*Texto r1.1:* «Resultado: sólo `1` (blanco intenso) y `2` (amarillo intenso) son ajustables con P1-P31.»

### 3.5 Asistente de calibración (sólo V3.6, sólo administrador)

**RF-APP-15 — Ajuste.** Por código: mínimos cuadrados de `R = f(x)` sobre las medias por patrón, de
**grado 1 o 2** (el 3 no se ofrece). Exige al menos grado + 2 niveles distintos. Muestra
coeficientes, residuo por patrón (absoluto y %), error cuadrático medio y comparación con el error
puro de repetibilidad; ofrece el ajuste con y sin el punto de oscuro. Rechaza el juego si la curva no
es creciente y no negativa en el rango de `x` medido, o si pasa de 4000 en él. CA: T-A13, T-A14.
**[MOD r1.1]** Condición **C2**: la forma se valida en **todo el rango de uso de `x`, de 200 a 4400**
(no sólo en el medido): creciente en todo él, sin pasar de 4000 en todo él, y no negativa desde la `x`
mínima medida hasta 4400. Por debajo de la `x` mínima medida, un negativo **sólo se avisa**, porque
todas las curvas de fábrica son negativas hacia `x` = 200 (`f1(200)` = −182) y el equipo responde 0 en
oscuro por diseño; que esto cumpla o no la letra de C2 queda como contradicción abierta para el
propietario (C-27, P-09). Grado + 2 **niveles distintos** (no lecturas). Los puntos del ajuste son
**medias de lecturas válidas de un mismo método** (RF-APP-27). CA: T-A13, T-A14.
*Texto r1.0:* «Rechaza el juego si la curva no es creciente y no negativa en el rango de `x` medido, o si
pasa de 4000 en él.»
**[MOD r1.2]** Además de C2, la app aplica **el mismo criterio que `#S` del firmware 3.6.1**
(RF-FW-31: `R(x)` finito y en [0 ; 4000] para todo `x` entero de 600 a 4300, en `float32` y en el
orden de 2020; `Asistente.java:222-241`, aplicado en `:350-353`). Si una parábola no lo cumple, prueba
una recta con los mismos puntos y lo dice (`:354-366`). Esto evita mandar un `#S` que el firmware
rechazaría. Dato: con las medidas del 19-sep, el ajuste de grado 2 del código `2` (opción C) **no
cumple** el criterio (R(600) ≈ −60) y el de grado 1 sí (T-A41).

**RF-APP-16 — Estado "como llegó".** Evalúa las ecuaciones actuales (leídas con `#G`) sobre las `x`
medidas y anota la desviación de cada patrón. CA: T-A15.

**RF-APP-17 — Confirmación antes de escribir.** Pantalla con el código, los coeficientes actuales y
los nuevos, las `R` que darían los dos juegos sobre los patrones, y botón de confirmación explícita.
Después: `#S`, `#G` de relectura, comparación (≤ 1·10⁻⁶ relativo), y nueva medida de al menos un
patrón con el código. Guarda antes una copia de `#G` de los 12 códigos y de `#GT#`. **Bloqueado por
O-01** hasta que `#S` quepa en el límite de trama. CA: T-C23, T-C28.
**[MOD r1.1]** O-01 resuelta: **ya no está bloqueado**. `#S` con 9 cifras (`%.8E`), ≤ 96 bytes. Condición
**C3**: si la relectura con `#G` no coincide con lo enviado (con la tolerancia de RF-APP-07), **la app
restaura sola el estado anterior**: `#F,<k>#` si el anterior era el de fábrica (nunca `#S` con valores de
fábrica, RF-APP-18), o `#S` con los coeficientes anteriores; relee y lo anota. Lo mismo si `#S` responde
`#ERR` o no responde y la relectura no es el estado anterior. CA: T-A32, T-C23, T-C28.
*Texto r1.0:* «**Bloqueado por O-01** hasta que `#S` quepa en el límite de trama.»
**[MOD r1.2]** Tras `#S`, la app relee con `#G` a **`ULP_S = 8`** (no 1·10⁻⁶ relativo ni 1 ulp) y
comprueba **`#E` en 5 puntos** contra la curva enviada (±1); si una de las dos no cuadra, restaura
(`AdminActivity.java:468-556`, app 3.6.4). Antes de enviar, aplica el criterio de RF-FW-31
(`AdminActivity.java:421`).

**RF-APP-18 — Volver a fábrica.** Por código o todos, con `#F` (nunca reescribiendo los valores de
fábrica con `#S`, RF-FW-05). CA: T-C25.

### 3.6 Administrador

**RF-APP-19 — PIN.** El modo administrador se abre con el PIN del equipo (`#L`); la app no guarda el
PIN. Ofrece cambio de PIN (`#P`) y cierra la sesión (`#Q#`) al salir del modo, al desconectar y al
cerrar la app. Las funciones de escritura no se muestran fuera de ese modo. CA: T-C21, T-C26.
**[MOD r1.1]** Además: la app cuenta los fallos de PIN y avisa antes del quinto (el firmware bloquea
`#L` y `#P` hasta apagar, RF-FW-17); da la sesión por cerrada a los 9,5 min sin tramas `#` (el firmware
la cierra a los 10) y al recibir `#ERR,BLOQUEADO#`. El PIN sale como `****` en el registro. CA: T-A19,
T-C21, T-C26, T-C31.

### 3.7 Exportación y registro

**RF-APP-20 — Registro técnico.** Archivo aparte del CSV de proyecto: cada trama enviada y recibida
con marca de tiempo, versión detectada, sesión de patrones, ajustes, escrituras y el informe del modo
de pruebas. Nada se borra desde la app. CA: T-A16.

**RF-APP-21 — Acta de calibración.** Exporta el antes y el después por código: coeficientes, `x` y
`R` por patrón, residuos, versión de firmware, número de serie tecleado y declaración "ajuste contra
patrones, no calibración trazable" mientras falten geometría y certificado de origen de los patrones
(`patrones_certificados_P1-P31.csv`, líneas 3-4). CA: T-A17, T-C28.

**RF-APP-22 — Copia de coeficientes.** Exporta y guarda los 12 × 4 y los 3 de temperatura leídos del
equipo. Motivo: la EEPROM está protegida por `CP = ON` y un borrado para regrabar la vacía; esta copia
es lo único que queda. CA: T-C29.

### 3.7 bis Requisitos de app nuevos en la revisión 1.1

**RF-APP-23 — Temperatura: la app V3.6 no escribe `#ST` [NUEVO r1.1].** Decisión: el factor de
temperatura **no se ajusta desde la app en la V3.6**. Motivos: no se sabe qué sensor mide `T` ni en qué
unidades (CA3 de `ARQUITECTURA.map`), y P1-P31 no permiten ajustarlo (hace falta medir un mismo patrón
a varias temperaturas). La app lee `#GT#` y lo guarda en la copia (RF-APP-22). La trama `#ST` existe
en la app con los límites de C4 para cualquier uso futuro (`Tramas.tramaST`), pero no se ofrece. CA:
T-A37 (JVM de los límites), revisión de la interfaz.

**RF-APP-24 — Botones de pantalla [NUEVO r1.1].** Sólo V3.6: `#KC#`, el operador pulsa un botón de
la STONE, `#K#`; la app muestra las tramas, **resalta el byte 8** (`bufferPantalla[8]`,
`ecuacionesCalibracion.c:73-130`), lo traduce con la referencia archivada (OTROS PAPELES 0x01-0x06,
TIPO I 0x07-0x0D, 0x0E prueba ADC) y guarda la pareja botón → trama. Acepta el formato de RF-FW-29 y,
por tolerancia, hex con espacios. Una trama hex ilegible invalida la respuesta entera. CA: T-A36 (JVM
del analizador), T-C33.

**RF-APP-25 — Línea base previa a grabar (G3) [NUEVO r1.1].** Con el firmware original, la app
registra con la etiqueta `LINEA_BASE_PRE_GRABACION`, en un CSV propio y en el registro de tramas, cada
petición con su respuesta literal y su desenlace: los 12 códigos sobre un patrón (T-B02), los 12 en
oscuro (T-B07) y `9` **diez veces** (T-B09). La sesión P1-P31 de T-B03 se hace con RF-APP-13. CA:
T-B02, T-B03, T-B07, T-B09.

**RF-APP-26 — Longitud de petición según la versión [NUEVO r1.1].** La app **no envía ninguna petición
de más de 49 bytes mientras `#V#` no haya respondido como V3.6** en esa conexión: en el firmware 2020
`bufferData[50]` no tiene límite (`uart_module.c:51-57` base) y una trama larga escribiría fuera. Antes
de la detección sólo salen `#V#`, `e`, `6` y la sonda `@LEERV,BLA,1@` (13 bytes). CA: **T-A34** (JVM:
una trama de 50 bytes con la versión sin detectar o V3 2020 se rechaza antes de enviarse).

**RF-APP-27 — Estadístico, suelo y tolerancias [NUEVO r1.1].**
- **Estadístico de un patrón:** la media de las **lecturas válidas de `e`**, publicando `n` y la
  desviación típica `s`. Las lecturas de `6` invertido **no se mezclan** con las de `e`: si una sesión
  tiene de los dos métodos, se publican por separado y el ajuste (RF-APP-15) sólo usa las de `e` (las de
  `6` invertido sólo sirven antes de grabar, en SLV-002).
- **Suelo de resolución:** con `e`, **1 cuenta de `x`**. Con `6` invertido, la semianchura del
  intervalo de inversión (hasta ±3 cuentas hacia `x` de 1700-3000). **En la práctica el suelo lo marca
  la temperatura**: `dx/dT = (ADC + 200)·(2·X_2·T + X_1)`; con el juego de fábrica y `T` en décimas de
  grado (lo que sugiere el comentario de `gui.c:504`, **sin verificar**, CA3), son 10·0,00043212·(ADC +
  200) ≈ **4 a 13 cuentas de `x` por °C** para `x` de 1000 a 3000.
- **Tolerancias:** toda tolerancia de la app (coherencia, repetibilidad, comparación antes/después) es
  **≥ max(suelo, 2·s)**, con `s` la del patrón y método en cuestión.
CA: T-A11, **T-A35** (JVM: media sin mezclar métodos; tolerancia ≥ max(suelo, 2·s)), T-B04.

### 3.7 ter Requisitos nuevos en la revisión 1.2 (dato de campo del 19-sep-2026)

Fuente de las cifras de esta sección: `07 pruebas/19092026_0900/p29_p24_p23_p5/medidas_SLV-002_20260919_091213 (3).csv`
(153 lecturas con `e`, 17 series de 9 disparos, SLV-002 con la V3.6 del 18-sep) y
`medidas_SLV-002_20260919_085924 (1).csv` (9 series de 3). Análisis hecho en este trabajo; los CSV no
se han tocado.

**RF-APP-28 — Descarte de disparos de asentamiento [NUEVO r1.2].** Antes de cada serie de lecturas
(medida de patrones, `e` inicial de la prueba 3, repetibilidad) la app hace **N disparos que no
cuentan**: van al registro de tramas marcados como descartados y no entran en la media, en el CSV ni en
el ajuste. Motivo, medido: en **17 de 17 series** el primer disparo sale bajo, de 6 a 40 cuentas por
debajo de la mediana de los disparos 2-9 (17,3 de media). **No es sólo el primero**: media de
`x − mediana(2-9)` por posición, −17,4 · −5,1 · −3,5 · −0,8 · +1,9 · +3,4 · +1,5 · +2,4 · +3,2; el
segundo disparo sigue por debajo en 16 de 17 series. El efecto reaparece tras pausas de 8 s o más entre
series (P7, segunda serie). La causa **no está medida**: el filtro de la medida se reinicia en cada
disparo (`measurement.c:241`) y la luz se enciende 400 ms antes de muestrear (`:225-226`), así que no es
estado arrastrado por el IIR; la hipótesis es térmica (LED o sensor). **N se fija con T-C38**; hasta
entonces vale el de la app 3.6.4, **N = 1** por defecto, configurable de 0 a 5 (`Sesion.java:59`,
`MedidaActivity.java:134-144`, `LecturaX.java:43-48`, `Pruebas.java:434-438`), sabiendo que con N = 1
queda un sesgo del orden de 5 cuentas en el primer disparo que cuenta. CA: T-A38 (JVM), T-C38 (equipo).

**RF-APP-29 — Disparo descolgado [NUEVO r1.2].** Dentro de una serie, una lectura que se separa de la
mediana de la serie más de **max(30 cuentas, 5 · 1,4826 · MAD)** se marca "descolgada", **no entra en
la media** y se informa con su valor. Si hay más de una en la serie, la serie entera se marca para
repetir. Motivo, medido: en P7 (serie de las 09:19:26) el noveno disparo dio 3031 con una mediana de
3230 (−199); en las otras 16 series la mayor separación de la mediana fue de 13 cuentas y la MAD, de
1,5 a 7,5. El umbral de 30 no sale de ninguna norma: es ~2 veces la mayor separación normal observada.
La app 3.6.4 **no lo hace** (`Asistente.java:136-158` promedia todas las lecturas válidas); la 3.6.5 sí, en la campaña
(`Veredicto.descolgados`), con otra regla (C-46). CA: T-A39.
El suelo de 30 cuentas **importa**: la regla sin él, 5 · max(1,4826 · MAD ; 1,5), que es la de
`Veredicto` en la app 3.6.5 (commit `ff66f93`, `K_MAD = 5`, `SIGMA_MIN = 1.5`), marca como descolgado el 2004 de P2 (09:17:21: mediana 2016, MAD 1,5, umbral 11,1),
que es el primer disparo tras el de asentamiento y no un fallo del operador (RF-APP-28). C-46.

**RF-APP-30 — "¿Es este el patrón?" [NUEVO r1.2].** Si un patrón ya tiene lecturas en la sesión y la
mediana de la serie nueva se separa de la anterior más de **100 cuentas de `x`**, la app no la acepta
sin confirmar: "¿es P24?". La 3.6.5 (`Veredicto`) avisa por otros dos criterios: orden de los
certificados dentro del mismo color y tipo (3 %) y parecido (±1 %) con otro patrón ya medido; con los
XI desordenados (C-39), el primero avisará también en series buenas. Motivo, medido: P24 (593, IV) dio 2048-2076 y, 30 s después, 2438-2453
(Δ ≈ 370); recolocar el mismo patrón movió 6-58 cuentas en P20, P21, P23, P5 y P7. El umbral de 100 es
provisional. Qué serie es P24 queda abierto (C-40). La app 3.6.4 **no lo hace**. CA: T-A40.

**RF-APP-31 — Patrones tipo I [NUEVO r1.2].** El catálogo de la app incluye los 20 patrones tipo I
entregados por Diego el 19-sep (P32a-P50: 1 blanco, 4 amarillos, 2 verdes, 3 rojos, 6 azules, 4
naranjas), que calibran las ecuaciones **opacas** (`7`, `8`, `a`-`d`), y la cobertura por código sale
de él (RF-APP-14 r1.2). **P32 está duplicado** en la lista original (P32a azul 9 y P32b naranja 68):
pendiente de Diego (C-41). Ninguno se ha medido todavía. Con las curvas de fábrica, verde (6-7) y azul
(7-10) caerían hacia x ≈ 518-589, por debajo del 600 de `#S` (RF-FW-31) y en el oscuro de la V3.6
(x ≈ 575, acta de SLV-002, G4); es estimación, no medida. Evidencia en la app: 20 filas en
`assets/patrones_certificados_P1-P31.csv` (commit `090c84c`); `Asistente.java:39-45`. CA: T-A12,
T-C39.

**RF-APP-32 — Modo Campaña [NUEVO r1.2].** Pedido por Diego el 19-sep-2026. **Ningún documento lo
especificaba hasta esta revisión.** Implementado mientras se escribía, en la app **3.6.5** (commit
`ff66f93`, clases `Campana`, `Campanas`, `CampanaActivity`, `Cola`, `Veredicto`, `Importador`, `Csv`;
prueba `CampanaTest`; el commit declara 68 pruebas JVM, **no ejecutadas en este trabajo**). El código
sigue cambiando: se cita **por clase y método, no por línea**. Estado frente a los seis puntos, leído en
`ff66f93`: 32.1 hecho (`Campanas.abrir`, `Campana.esDeEsteEquipo`, `CampanaTest.dosEquiposNoCompartenSeries`);
32.2 hecho (diario de eventos de sólo añadir, cabecera de la clase `Campana`); 32.3 retoma, pero **no
hay cierre explícito de campaña** (el único `cerrar` es el de una serie); 32.4 ZIP hecho
(`Campanas.exportar`) **sin md5** (Grep de `md5` y `MessageDigest` en la app: sin resultados, dos
herramientas); 32.5 hecho (`ACTION_SEND` con un `EXTRA_STREAM` en `CampanaActivity`); 32.6 hecho
(`Importador`, `CampanaTest.elImportFiltraPorMac`, `importarCsvDeHoySinDuplicados`). La calibración que se hace con lo medido (asistente, modo
superadministrador, informe PDF) está en [`SPEC-Calibracion-V3.6.md`](SPEC-Calibracion-V3.6.md), en
redacción por otro agente: aquí no se duplica.

Antecedentes que motivan el requisito, medidos: (1) la carpeta de la campaña del 19-sep tiene 32
ficheros para 4 sesiones, porque cada vez que se compartía el móvil guardaba otra copia con `(1)`,
`(2)`…, prefijos crecientes del mismo fichero y con sufijos que no siguen el orden del tiempo (L-21 de
`D:\IT\P_RetroVertical_V4.6\APRENDIDO-DE-V3.6.md`; aquí se comprobó que
`medidas_SLV-002_20260919_091213 (5).csv` y `…091213.csv` son prefijos de
`p29_p24_p23_p5/…091213 (3).csv`). (2) La app de campo reescribe y trunca el fichero entero en cada
guardado (RF-08 de `ROADMAP-MEJORAS-App.md`, `archivos.java:16-28` de la app de campo). (3) Hoy la app
V3.6 comparte registro y CSV con `ACTION_SEND_MULTIPLE` (`Base`, método de compartir).

Flujo, común a los seis puntos: la app pide un patrón cada vez ("Coloque P27"), espera OK, hace el
asentamiento (RF-APP-28) y **9 disparos**, da el veredicto y pasa sola al siguiente. Un patrón se puede
saltar y queda como saltado, no como medido. Veredicto antes de guardar: descolgados (RF-APP-29), **s >
15 cuentas** sin descolgados → REPETIR, y "¿es este el patrón?" (RF-APP-30), con tres salidas:
**Repetir**, **Aceptar con nota** (nota obligatoria) y **Era otro patrón** (reasigna la serie y lo
anota).

| # | Requisito | Criterio de aceptación (medible) | Prueba |
| :---: | :--- | :--- | :--- |
| 32.1 | **Identificación.** Una campaña es de **un equipo**: clave = **MAC** del enlace + **serie tecleada**. La MAC manda (es lo único que no teclea nadie). Casos: (a) MAC y serie coinciden con una campaña abierta → se retoma; (b) **misma serie y otra MAC** → no se añade nada a la campaña de esa serie; la app avisa "serie X ya tiene campaña con la MAC Y" y sólo ofrece campaña nueva; (c) **misma MAC y otra serie** → aviso y confirmación explícita; si se confirma, campaña nueva con la serie nueva y una nota que enlaza con la anterior; nunca se fusionan. En código: `Campanas.abrir(ctx, serie, mac)`, `Campana.esDeEsteEquipo(mac)` | (a) retoma 1 de 1; (b) 0 eventos añadidos al diario de la campaña existente (tamaño en bytes igual antes y después) y 1 aviso; (c) 0 eventos añadidos sin confirmación; tras confirmar, 2 diarios distintos | T-A44 (JVM) |
| 32.2 | **Diario de sólo añadir.** Una línea CSV por evento (`SERIE`, `DISPARO`, `DESCARTE`, `VEREDICTO`, `REASIGNA`, `ELIGE`; lista de la clase `Campana`), **escrita y volcada a disco en cada disparo**, nunca reescrita ni truncada (lo contrario de RF-08). Nada se borra: reasignar o descartar son eventos nuevos. Al leer, una línea incompleta (corte a mitad) se cuenta y se salta, y no invalida las demás (`Campanas.lineasMalas()`) | Tras cada disparo, el diario crece y el anterior es **prefijo byte a byte** del nuevo (100 disparos simulados, 100 de 100). Cortando la escritura en un byte al azar: se pierde como mucho el último evento y `lineasMalas()` = 1. Ninguna operación de la campaña abre el diario en modo escritura sin añadir | T-A47 (JVM) |
| 32.3 | **Retomar y cerrar.** Al conectar, la app abre la campaña **abierta** de esa MAC y serie (32.1); si hay más de una abierta de la misma clave, la más reciente, y lo dice. Se reanuda en el primer patrón sin serie aceptada ni saltada. Se **cierra** sólo con una acción explícita "Cerrar campaña", que escribe un evento de cierre en el diario; una campaña cerrada no admite más disparos (se abre otra). **Hueco:** la lista de eventos de `Campana` no tiene evento de cierre a las 10:30 | Matar la app tras la serie 3 de 5 y reabrir: reanuda en el patrón 4 con las 3 series (diario idéntico byte a byte). Cerrar y reconectar: campaña nueva, 0 eventos añadidos a la cerrada | T-A48 (JVM); T-C40 (equipo) |
| 32.4 | **ZIP.** Un solo fichero `campana_<serie>_<AAAAMMDD-HHMMSS>.zip` (si existe, sufijo `_2`, `_3`…, como `Campanas.exportar`), con exactamente: `campana.csv` (series con veredicto y nota), el **diario**, `pruebas_<serie>_<mac>.txt` (resultados de las Pruebas), el **registro de tramas de todas las conexiones** de la campaña y `resumen.txt`. `resumen.txt` lleva el md5 de cada una de las otras piezas; el **md5 del ZIP** se anota en el registro técnico y se muestra al operador | Un ZIP de una campaña simulada con 2 conexiones: 5 entradas con esos nombres; los md5 de `resumen.txt` coinciden con los de las piezas extraídas; el registro incluye cada TX/RX de las 2 conexiones; el md5 mostrado = md5 del fichero | T-A45 (JVM) |
| 32.5 | **Compartir una vez.** Un único botón "Exportar campaña" genera el ZIP y lo entrega por el **menú Compartir de Android** con `ACTION_SEND` y **un solo** adjunto; no hay compartir por patrón ni por sesión en el flujo de campaña. **Esto cierra P-03** (el medio de entrega es el menú del sistema, sin credenciales en la app) y evita L-21 | En la interfaz de campaña hay 1 acción de compartir; el `Intent` lleva 1 URI con extensión `.zip`; exportar dos veces seguidas da dos ZIP con nombres distintos (sufijo), nunca copias `(1)` | T-A46 (JVM del `Intent` y del nombre); revisión de la interfaz |
| 32.6 | **Importar lo previo.** Importa CSV de medidas anteriores (14 columnas de `Medida`) **filtrados por MAC**, y **sin duplicar** las copias-prefijo de L-21 (se queda con la más larga y comprueba que las demás son prefijos) | Importar los 16 CSV de `07 pruebas/19092026_0900/` en una campaña de MAC `00:21:13:05:19:3B`: entran **180 lecturas** (153 de la sesión de las 09:12 y 27 de la de las 08:59), no la suma de los ficheros; con otra MAC, 0 | T-A49 (JVM) |

Casos de aceptación de extremo a extremo (T-C40, equipo): una campaña de 5 patrones en SLV-002 con un
cierre forzado de la app a mitad, un patrón saltado y una serie reasignada; exportar; el ZIP contiene
lo descrito y su md5 coincide con el anotado.

### 3.7 quater Requisitos nuevos en la revisión 1.3 (banco y "Calibrar este equipo") [NUEVO r1.3]

**Nada de esta sección está implementado** en la app 3.6.9 (commit `f52eeb1`). El estado de cada
requisito frente al código está en `MATRIZ-SPEC-codigo-V3.6.md`. Salen del QA del flujo
(`QA-Flujo-Calibracion-V3.6.md`, D-01 a D-17), de los defectos D-18 a D-20 que deja ver el ZIP de las
12:27 (`SPEC-Calibracion-V3.6.md` §12.0) y de lo que pidió Diego el 19-sep:

- un banco de medida guiado de los 133 patrones;
- con el ZIP, "Calibrar" y poco más;
- la serie se lee de `#GN#`;
- la calibración vencida avisa y no bloquea.

La parte metrológica (protocolo, criterios y métodos) está en `SPEC-Calibracion-V3.6.md` §12
(RF-CAL-35 a RF-CAL-43), y la cola del banco en `06_Calibracion/PLAN-Captura-Banco-P1-P132.md`. Las
pruebas están en `TDD-V3.6.md` §3 ter. **Ninguno de estos requisitos exige cambiar el firmware 3.6.2**
(`SPEC-Calibracion-V3.6.md` §12.9).

**Dos modos.** *Operación* es lo que ve el operador: medir el banco y calibrar. *Avanzado* es lo de hoy
(ajuste manual, fábrica, PIN, serie, `#FT#`, botones), detrás de un aviso de riesgo, hasta que exista el
superadministrador (RF-CAL-27).

| RF | Requisito | Criterio de aceptación (medible) | Prueba |
| :--- | :--- | :--- | :--- |
| **RF-APP-33** | **Modo banco.** La app carga la cola `cola_banco_P1-P132.csv` desde los assets, **sólo si su md5 está en la lista del APK**. La sigue paso a paso: "Coloque P28 (blanco IX, cert. 484) y pulse OK", y después "Levante y apoye (2 de 3)". Cada colocación son 1 + M disparos con `e`. OSCURO, A5 y batería son pasos de la cola y **no se pueden saltar**; los patrones sí, y quedan marcados. Al terminar cada sesión, exporta el ZIP sin preguntar (RF-APP-40). Retoma en el primer paso pendiente | Con la cola de 180 filas: 133 pasos de patrón y 47 de control, en el orden del CSV. Nº de `e` = Σ K·(M + 1) de los pasos medidos. Un md5 desconocido → "Cola no admitida", sin ningún `e`. OSCURO y A5 sin botón "Saltar". Matar la app en el paso 40 y reabrir → sigue en el 40 con el diario idéntico byte a byte | T-A50, T-S01, T-C42 |
| **RF-APP-34** | **"Calibrar este equipo".** Un botón en la pantalla Campaña que hace la Fase B de `SPEC-Calibracion-V3.6.md` §12.3: (1) comprobaciones previas automáticas; (2) una pantalla de resumen con una tarjeta por código de la tabla RF-CAL-37; (3) una casilla de conformidad por código, el nombre y una nota; (4) un botón "Calibrar"; (5) la secuencia código a código; (6) la persistencia; (7) el acta con "Aceptar y grabar fecha" o "Rechazar". El operador no elige código, grado, método ni patrón de re-medida | Toques del operador en la Fase B de 3 códigos: ≤ 24, más uno por colocación. Entradas de texto: 1 (la nota). La pantalla de calibración tiene **0** controles de grado, método o patrón. Con una comprobación previa que falla, el botón sigue deshabilitado y dice cuál falla y cómo se resuelve (AT-04, AT-05, AT-07, AT-18, AT-21, AT-22) | T-S07, T-C43 |
| **RF-APP-35** | **Identidad por `#GN#`.** La serie de la campaña, del acta y de los registros es la de `#GN#` más la MAC. El nombre Bluetooth sólo se coteja. Si `#GN#`, MAC y campaña no coinciden, no se mide ni se calibra, y se dice por qué. **Alta de serie** sólo si `#GN#` = `NONE` o desde Avanzado: se muestra "Serie actual: X → Nueva: ___" en grande; la nueva se teclea dos veces; si difiere del nombre Bluetooth, hay que marcar una casilla; si la actual no es `NONE`, se pide una nota. Después, `#SN` y relectura con `#GN#`. Sustituye a `Sesion.serie()` (`Sesion.java:157-164`) | `#GN,SLV-003#` con una campaña de SLV-002 → 0 tramas `#L`, `#S`, `#SN`. Teclear `SLV-02` y `SLV-002` → 0 `#SN` ("no coinciden"). El caso real del 19-sep (`#SN,SLV-02#` a las 12:09:02, T4:2048) no se puede repetir sin doble entrada, casilla y nota | T-A59, T-S03, T-S04, T-S05, T-C48 |
| **RF-APP-36** | **Acta y calibración en curso, en disco y reanudables.** El estado de la Fase B vive en un fichero de sólo añadir de la campaña: acta, conformidades, código en curso, fase (escribiendo, escrito, re-medida, verificado) y copia previa. **Reconectar no borra nada.** Hoy `Sesion.reiniciar()` pone `acta = null` (`Sesion.java:130`) y se llama al conectar. Al volver, la app ofrece "Hay una calibración a medias de SLV-002: código 8, escrito, falta la re-medida. Continuar". El bloqueo de P9-B8 se evalúa sobre ese estado, no sobre la memoria | Matar la app con el código 1 escrito y sin re-medida, y reabrir → ofrece la re-medida del 1, y `#S` de otro código queda bloqueado. Desconectar y reconectar 5 veces → el acta y las conformidades siguen idénticas (diff vacío). El ZIP lleva el acta PENDIENTE si se exporta a medias | T-A60, T-S09, T-S10 |
| **RF-APP-37** | **Corte durante `#S` (P9-B12), automático.** Si el enlace cae entre el envío de `#S,k` y la relectura, al reconectar la app hace, antes que nada, `#G,k#`: (a) si coincide con lo enviado (8 ulp), `#E` en 5 puntos y sigue con la re-medida; (b) si coincide con lo anterior, el `#S` no entró, y ofrece repetirlo; (c) si no coincide con ninguno, restaura lo anterior y relee. Nunca hay un `#S` de otro código antes de resolver k | Simulador con corte tras `#S,8`, en las tres variantes: (a) sin restauración y con la re-medida pendiente; (b) sin restauración y con `#S,8` ofrecido; (c) `#F,8#` o `#S` con la copia, y relectura igual a la copia. En las tres, 0 `#S` de otro código entre el corte y la resolución | T-A61, T-S08, T-C46 |
| **RF-APP-38** | **Re-medida guiada** según RF-CAL-39: el patrón lo fija la tabla del APK (RF-CAL-37); K = 5 y M = 4, con `e` y código alternados; tres comprobaciones; las colocaciones inválidas se repiten; una sola repetición si no es conforme; y si vuelve a fallar, restauración. El acta guarda **todos** los intentos. Sustituye a `Acta.evaluarRemedida` con s_rep supuesta (`Acta.java:141-163`) y a la sobrescritura de `Acta.remedida()` (`Acta.java:133`) | Con los datos del 19-sep: el intento de 12:17:02 (d̄ = +50,7, T4:2156) sale como "colocación no válida" y no como NO CONFORME. P28 (497,8 frente a cert. 484) y P5 (693,2 frente a 740) salen conformes. Un patrón a +12 % del certificado → NO CONFORME, una repetición y, si falla, restauración. El acta no contiene "supuesta" | T-A53, T-A54, T-A55, T-S12, T-S23 |
| **RF-APP-39** | **Importar, visible.** "Importar ZIP de campaña" es el primer botón de la pantalla Campaña, al nivel de "Medir el banco". "Nueva campaña" y "Cerrar campaña" pasan a Avanzado. Hoy "Importar" es el tercer control de la sección "Enviar" (`CampanaActivity.java:99-107`) | Con la pantalla recién abierta en un teléfono de 6", "Importar" se ve sin desplazarse. Ningún control que archive o cierre la campaña está a menos de 1 fila de "Importar". Reimportar el mismo ZIP → "0 importadas, N ya estaban" | T-S02, T-A68 |
| **RF-APP-40** | **Aviso de no desinstalar y copia fuera de la app.** Al arrancar, si hay series sin exportar: "Hay N series sin exportar. No desinstale la app: se borrarían. Para actualizar, instale la versión nueva encima". Cada exportación deja una copia en `Download/RTV/`. `allowBackup` sigue en `false` (`AndroidManifest.xml:14`) | Con 3 series sin exportar, el aviso aparece al arrancar (1 de 1). Tras exportar, `Download/RTV/campana_<serie>_<fecha>.zip` existe y su SHA-256 es el del ZIP compartido. Desinstalar y reinstalar → la copia de `Download/RTV/` sigue | T-A69, T-A71 |
| **RF-APP-41** | **Versión en el nombre del APK y en pantalla.** Sólo se entrega `RTV-V<versionName>-<versionCode>.apk`, y nunca `RTV-V3.6.apk` (`03_App_Movil/RetroV36/README.md:24-25`). La versión va en grande en la cabecera de todas las pantallas, y en el nombre de cada ZIP y de cada acta | La carpeta de entrega no contiene `RTV-V3.6.apk`. El APK 3.6.10 se llama `RTV-V3.6.10-3610.apk`. Captura de las 5 pantallas con la versión visible. `resumen.txt` y el acta llevan `3.6.10` | T-A62, T-A70 |
| **RF-APP-42** | **Códigos 3, 4 y 6 ajustables, y el 5 si PA-14.** El asset de la app pasa a ser el catálogo P1-P132 (md5 `a74222c0…`, PA-21). La regla de cobertura (`Asistente.java:75-98`) se mantiene para mínimos cuadrados. Para la **recta anclada** cuenta el ancla como un nivel más (certificado 0 en la `x` del OSCURO): así el 5 (83-102) pasa a tener un rango de 0-102. Hoy `proponer` bloquea la anclada con la cobertura de grado 1 (`Asistente.java:406-422`) | Con P1-P132: 1, 2, 3, 4, 6 y 8 hasta grado 2; b hasta grado 1; 5, 7, a, c y d "sólo verificar" con mínimos cuadrados. 5 **ajustable con la recta anclada** sólo si la tabla RF-CAL-37 lo permite. Café y lila no cuentan en la cobertura del 4 | T-A56 |
| **RF-APP-43** | **Aviso de batería** según RF-CAL-41: `9` al empezar cada sesión, al cambiar de color, antes de la Fase B y antes de cada `#S`, nunca en mitad de una serie. Muestra V y el aviso. **Bloquea las escrituras** con n = 0 o sin respuesta. La V queda en cada serie del diario y en el acta | n = 40 → "Batería 10,77 V", sin aviso. n = 15 → aviso y medida permitida. n = 0 o sin respuesta → `#S`, `#F`, `#SC`, `#SN`, `#FT` no se envían (0 tramas) y la medida sigue. Ningún `9` entre dos disparos de una misma serie | T-A57, T-S14, T-C44, T-C49 |
| **RF-APP-44** | **`#V#` fresco.** La app vuelve a leer `#V#` después de cada `#S`, `#F`, `#FT` y `#SC`, y antes de generar el resumen, la cabecera del ZIP y el acta. La marca y la máscara del acta son las de esa lectura. "No calibrado" sólo sale con un `#V#` leído después de la última escritura | Tras `#S,1` y `#S,2` en el simulador: `#V#` enviado ≥ 2 veces después del primer `#S`. El acta dice `CAL mascara 0003`. El caso del 19-sep (acta con "DEF mascara 0000" tras escribir, línea 3) no se reproduce | T-A66, T-S20, T-C41 |
| **RF-APP-45** | **Avanzado, aparte.** La pantalla de operación no tiene `#F,*#`, `#FT#`, `#P`, ajuste manual, parámetros del oscuro ni grado. Todo eso va a Avanzado, que se abre con un aviso de riesgo (y con la frase del superadministrador cuando exista, RF-CAL-27). Cada acción destructiva de Avanzado pide confirmación tecleada | Nº de controles activos en la pantalla de calibración de Operación ≤ 8 (hoy, 25 en `AdminActivity.java:84-170`). 0 acciones destructivas en Operación | Revisión de interfaz en T-S07 |
| **RF-APP-46** | **Persistencia dentro del flujo** (final de P9-B8): "Apague el equipo, espere 5 s y enciéndalo". La app reconecta y repite `#V#`, `#G` de lo escrito y `#E` en 5 puntos. Si algo no coincide, el acta no se puede aceptar | Sin el ciclo de apagado, "Aceptar" está deshabilitado. Con él, el registro de tramas contiene, después de la reconexión, `#V#`, un `#G,k#` por código escrito y 5 `#E,k,…#` por código | T-S07, T-C41 |
| **RF-APP-47** | **Código por fila de la cola.** El código de cada patrón sale de la columna `codigo_equipo` de la cola, no sólo de `Fabrica.color()` (`Fabrica.java:32-42`). Café y lila van con el 4 y con uso VERIFICACIÓN (RF-CAL-40) | Los 10 café y lila se evalúan con la curva del 4 en el resumen, y ninguno aparece en los puntos del ajuste del 4 | T-A64 |
| **RF-APP-48** | **Oscuro y s_rep sacados de la campaña.** La `x` del OSCURO es la media de la serie OSCURO de la sesión. La s_rep es la de la A5 del inicio (RF-CAL-38). En Operación no hay campos para teclearlas. Hoy se teclean (`AdminActivity.java:127-136`, `:468-471`) y el acta usa 575 (`Asistente.java:218`) | Con la campaña del 19-sep, el acta dice "Oscuro (x = 565,4)" y "s_rep 2,24 % (A5)". Sin serie OSCURO, la recta anclada queda "no calculable: falta OSCURO" (AT-07) | T-A65, T-S06 |

**Defectos del QA que cubre cada requisito:** D-01 → 34, 38. D-02 → 36. D-03 → 37. D-04 y D-05 → 35.
D-06 → 34, 45. D-07 → 39. D-08 → 34 (sin la caída a las medidas de sesión, `Sesion.java:241-250`).
D-09 → 40. D-10 → 41. D-11 → 48. D-12 → 34 y RF-CAL-37. D-13 → 38. D-14 → RF-CAL-43. D-15 → 33.
D-16 → 46. D-17 → 43. D-18 → 44. D-19 y D-20 → 38.

**Lo que esta revisión corrige de la r1.2**, según la matriz al día (`MATRIZ-SPEC-codigo-V3.6.md`):

- RF-APP-32.3 y 32.4 ya tienen cierre de campaña y huellas md5 y SHA-256 en la 3.6.9. El "hueco" de
  §3.7 ter queda superado.
- `#FT#` ya no está "en obra" (§2.2, línea 298): está en la 3.6.2 (`calibracion_v36.c:716-727`), grabada en SLV-002.
- El descolgado de RF-APP-29 pide un suelo de 30 cuentas, y el código usa 50 (C-46, sigue abierta).

### 3.8 Estado de la app frente a cada RF-APP [MOD r1.2]

**La tabla de la r1.1 se sustituye por [`MATRIZ-SPEC-codigo-V3.6.md`](MATRIZ-SPEC-codigo-V3.6.md)
§2**, leída contra el commit `090c84c` (RTV 3.6.4), con 56 pruebas JVM en verde ejecutadas en este
trabajo. Recuento de RF-APP-01…27: 11 cumplen, 12 parciales, 3 no (RF-APP-05 aplazado, RF-APP-09 con 5
lecturas, RF-APP-21 sin acta) y 1 contradice (RF-APP-06: `::0` con `e` rotulado "saturado o negativo",
`LecturaX.java:64`). De los nuevos (28-32), los cinco parciales: 28-31 frente a `090c84c` y a `ff66f93` (3.6.5), y 32 frente a `ff66f93`.

Citas de la tabla r1.1 que se han movido (izquierda, app de las 20:05 del 18-sep; derecha, `090c84c`):
`Ecuacion.java:275-287` → `:56-68` (emulación `float32`); `Ecuacion.java:290-300` → `:92-107`
(tolerancias e igualdad); `Ecuacion.java:245-267` → `:26-28,41-48` (`double`, Horner);
`Inversion.java:52` → `:58`; `Pruebas.java:243,275,283,291` → `:247,281,289,297`;
`Pruebas.java:307-398` → `:309-419`; `Pruebas.java:401-433` → `:421-475`;
`AdminActivity.java:395-503` → `:391-556`; `AdminActivity.java:442-495` → `:468-556`;
`Asistente.java:26-56` → `:74-118`; `Asistente.java:99-147` → `:160-241`; `Tramas.java:228-244` →
`:238-254`.

---

## 4. Paridad funcional con la app de campo (PAR)

La app de campo habla `@LEERV` (`medicion.java:1590`), no el protocolo V3. Las funciones se replican;
el protocolo, no. Decisión: **R** = replica, **A** = aplaza, **N** = no aplica.

| ID | Función de la app de campo | Referencia | Dec. | Motivo |
| :--- | :--- | :--- | :---: | :--- |
| PAR-01 | Inicio con Verificar / Crear / Abrir | `MainActivity.java:22-30`; SPEC-06 §B | R | Flujo de campo conocido por los operarios |
| PAR-02 | Crear proyecto: 5 campos obligatorios, CSV con 4 líneas de cabecera y `;` | `crearproyecto.java:49-57`; SPEC-06 §C | R | **Formato idéntico**, para que quien recibe los CSV no note el cambio |
| PAR-03 | Interruptor "Distancia" | SPEC-06 §B, §L.1 | N | No tiene función en la app de campo |
| PAR-04 | Abrir proyecto: lista, seguir midiendo, ver datos, enviar | `abrirproyecto.java:34-36,50,65,80` | R | Flujo de campo |
| PAR-05 | Tipo de señal: reglamentaria, preventiva, informativa | `medicion.java:143` | R | Dato del registro |
| PAR-06 | Catálogo de señales con imagen, botones Más y Menos | `medicion.java:176,833`; SPEC-06 §J | R | Dato del registro; se reutilizan los *drawables* |
| PAR-07 | Color de lámina (6), con código `BLA` … `NAR` | `medicion.java:85,1490` | R | Entra en el mapa de RF-APP-04 |
| PAR-08 | Tipo de lámina: *spinner* de **11 tipos**, que al equipo sólo llega como "1" o "2" | `medicion.java:1548,1584-1589` | R | En V3 decide intenso u opaco (RF-APP-04). SPEC-06 dice "tipo 1 o tipo 2": ver contradicción C-08 |
| PAR-09 | Medir: trama, espera fija de 3250 ms, una sola lectura | `medicion.java:1590-1594` | R (función) | Se replica "medir y mostrar"; el mecanismo cambia a un byte y fin por silencio (RF-APP-02) |
| PAR-10 | Análisis de `@LEERV,<n>@` | `medicion.java:1596-1612` | N | Otro protocolo |
| PAR-11 | Guardar medida: 9 campos con `;`, fecha con `Date.toString()` | `medicion.java:1562-1563` | R | **Mismas columnas y mismo orden**. Lo técnico (`x`, código) va al registro de RF-APP-20, no al CSV |
| PAR-12 | Aviso "No ha recibido medida" | `medicion.java:1557-1558` | R | Tras guardar, la app de campo pone el valor a "0" (`:1565`), con lo que un segundo guardado sin medir graba 0. Se replica el aviso; si se cierra ese hueco, es mejora de `ROADMAP-MEJORAS-App.md` |
| PAR-13 | Geolocalización por GPS y red | `medicion.java:1704-1705` | R | Dato del registro |
| PAR-14 | Dirección por *geocoder* inverso, editable | `medicion.java:1722`; SPEC-06 §E | R | Dato del registro |
| PAR-15 | Ver datos del proyecto | `verdatos.java:32-36` | R | Consulta en campo |
| PAR-16 | Enviar el CSV por correo SMTP con credenciales en el código | `Enviarcorreo.java:70-73,90-114` | R (función) | Se replica "entregar el CSV por correo"; el medio (credenciales embebidas) se decide en P-03 |
| PAR-17 | Finalizar → pantalla de correo | `medicion.java:1536-1542` | R | Flujo de campo |
| PAR-18 | Pérdida de conexión: aviso y vuelta a abrir proyecto | `medicion.java:1616-1621` | R | Comportamiento conocido |
| PAR-19 | Lista de emparejados y conexión SPP insegura | `medicion.java:89,1661` | R | Mismo módulo Bluetooth |
| PAR-20 | Verificar Bluetooth enviando "Prueba de conexion 122357" | `dispositivos.java:95` | N | En un V3 esa cadena dispara una medida y no obtiene respuesta. La sustituye la detección de versión (RF-APP-03) |
| PAR-21 | Verificar GPS | `verificargps.java`; SPEC-06 §H | R | Diagnóstico de campo |
| PAR-22 | Almacenamiento privado de la app | `archivos.java:19` | R | Con salida por el sistema de compartir de Android para exportar |
| PAR-23 | Mejoras de SPEC-06 §L (validaciones, cierre de socket, etc.) | SPEC-06 §L | A | `ROADMAP.md`: las mejoras van después de la V3.6 |

**[MOD r1.1] Decisión: todas las PAR marcadas R pasan a A (aplazadas a una fase de campo, después de
P8)**, salvo PAR-19 y PAR-22, que la app ya cumple. *Decisión r1.0:* R (replica) en las indicadas en la
tabla. Motivo: ninguna puerta P5-P8 necesita el flujo de campo; la app V3.6 que se ha construido es la
herramienta técnica de las puertas (pruebas, patrones, línea base, administración, botones), y en campo
SLV-002 sigue con la app de su cliente. La paridad no se abandona: se mide contra ella cuando se aborde
la fase de campo (T-B05). Estado a 18-sep-2026, 20:05:

| PAR | Estado en la app V3.6 | Evidencia |
| :--- | :--- | :--- |
| PAR-19 | Implementado | Lista de emparejados en `ConexionActivity.java`; socket SPP inseguro: `EnlaceSerie.java:157` |
| PAR-22 | Implementado | Ficheros en `files/registros/`, salida por "Compartir" (`README.md` de la app; `Sesion.java`) |
| PAR-01, 02, 04-09, 11-18, 21 | No (aplazado) | La app no tiene proyectos, catálogo de señales, GPS ni correo; sus actividades son `ConexionActivity`, `PruebasActivity`, `MedidaActivity`, `AdminActivity` y `BotonesActivity` (`AndroidManifest.xml:21-33`) |
| PAR-03, 10, 20 | N (no aplica) | Sin cambio |
| PAR-23 | A | Sin cambio |

Nota r1.1: PAR-08 remite a "C-08", que en la r1.0 no existía; la contradicción del *spinner* es C-07.
Queda anotado como C-08 en §9.

**Pregunta abierta, sin respuesta inventada.** El cliente de SLV-002 usa **una app propia que no
tenemos**. Por el firmware se deduce que hablará un byte por petición y leerá `::<n>`, pero no
sabemos qué códigos envía, qué espera tras la respuesta, si usa `9`, ni si envía algo que empiece por
`#`. Es la app que "tiene que seguir funcionando sin cambios" (`PROTOCOLO-V3.6.md` §1) y no hay forma
de probarla sin ella (T-C30).

---

## 5. Plan de pruebas

Toda prueba deja registro: fecha, equipo, versión detectada, tramas en crudo y resultado.

**[MOD r1.1]** El plan **ejecutable** (precondiciones, tramas literales, resultado esperado, criterio
de pasa o falla, quién lo ejecuta y cómo, orden real de la campaña y estado de cada prueba) está en
[`TDD-V3.6.md`](TDD-V3.6.md), que **manda sobre las tablas de esta sección**. Estas tablas se conservan
como texto r1.0. Cambios de la r1.1 respecto a ellas:

- **[r1.2]** Pruebas nuevas de la revisión 1.2: T-A38 (asentamiento), T-A39 (descolgado), T-A40 (¿es
  este el patrón?), T-A41 (curva C frente al límite de `#S`), T-A42 a T-A49 (modo Campaña), T-C37
  (regrabación 3.6.0 → 3.6.1), T-C38 (N de asentamiento), T-C39 (opacas con tipo I) y T-C40 (campaña de
  extremo a extremo). Detalle en `TDD-V3.6.md` §3 bis.
- **Pruebas nuevas:** T-A27 (`#E` en simulador), T-A28 (G1: commit y md5), T-A29 (memoria en el
  `.map`), T-A30 (límites de `#ST`, C4), T-A31 (órdenes de administración en simulador), T-A32
  (restauración automática, C3), T-A33 (`#K#` en simulador), T-A34 (longitud de petición, RF-APP-26),
  T-A35 (estadístico y tolerancias, RF-APP-27), T-A36 (analizador de `#K#`), T-A37 (límites de `#ST` en
  la app); T-B11 (barrido de 255 bytes) y T-B12 (lectura ICSP), **ya hechas**; T-C31 (bloqueo de PIN y
  caducidad), T-C32 (`#ST` en el equipo), T-C33 (`#K#` con la pantalla), T-C34 (trama de 96 bytes con
  medida en curso), T-C35 (duración de la escritura de EEPROM), T-C36 (`#` suelto, 2,5 s, `1` mide).
- **T-A18:** 9 cifras y límite de 96 bytes (no 7 y 48). **T-A23:** 9 cifras, criterio 0 ulp (C1).
  **T-A20:** en MDB, con informe (G2). **T-C04:** 1 ulp si T-A23 pasa; si no, ≤ 2 ulp y `#E` exacto
  contra la tabla de fábrica. **T-C05 (C5):** con `#E` (exacto) además de la comparación estadística.
  **T-B09:** diez veces, como decía; la app hoy lo hace tres (C-18). **T-C19:** ya no depende de O-01.

### 5.1 (a) Pruebas sin equipo

**Unitarias JVM (app).**

| ID | Qué | Pasos | Esperado |
| :--- | :--- | :--- | :--- |
| T-A01 | Tabla de fábrica | Leer `ecuacionesCalibracion.c` y `gui.c:41-43` del repositorio, extraer los literales y comparar con la tabla de la app | Los 51 valores coinciden como texto |
| T-A02 | Mapa código ↔ ecuación | Para cada código, comprobar que la tabla de la app apunta a la línea de `ecuacionesCalibracion.c:137-198` correcta | 12 de 12, incluidas las 3 parejas idénticas |
| T-A03 | Análisis de respuestas | Alimentar `::512`, `::5` + `12` en dos lecturas, `:3:`, `#G,1,...#` partida en tres, basura antes del `#`, silencio sin respuesta | Valores correctos; fin por silencio; tiempo agotado notificado; ningún `\r\n` esperado |
| T-A04 | Emulación `float` | Para `x` = 0..65535 y los 12 códigos, evaluar en `float` con el orden de RF-FW-04 y truncar | Coincide con la tabla de referencia generada por T-A20 (cuando exista) |
| T-A05 | Inversión | Para cada `x` entera de 475 a 4400, `n = f6(x)`; invertir `n` | El conjunto devuelto contiene `x`; anchura del intervalo registrada |
| T-A06 | Forma de las curvas | Calcular máximos, ceros y tramo creciente de los 12 códigos de fábrica | Blanco intenso máximo en x ≈ 3580; amarillo intenso ≈ 2784; naranja intenso creciente |
| T-A07 | Ritmo de peticiones | Simular respuestas; comprobar que no se envía nada antes de respuesta + 1 s | Ninguna petición solapada |
| T-A08 | Detección de versión | Simular los cinco casos de RF-APP-03 | Clasificación correcta; `@LEERV` sólo en el paso 4; ninguna otra trama con `@` |
| T-A09 | Mapa color × tipo | 6 colores × 11 tipos | Tipo I → opaco; II-XI → intenso |
| T-A10 | Coherencia | Coeficientes de fábrica formateados a 7 cifras y releídos; un juego con un coeficiente alterado | El primero APTO, el segundo NO APTO |
| T-A11 | Repetibilidad | Series sintéticas con desviación conocida | Media, desviación y CV correctos |
| T-A12 | Cobertura | Cargar el CSV P1-P31 | Avisos de verde, azul, rojo, naranja y de "ningún tipo I" |
| T-A13 | Ajuste | Puntos exactos de una recta y de una parábola; puntos con ruido conocido | Recupera los coeficientes; residuos y ECM correctos; rechaza con menos de grado + 2 niveles |
| T-A14 | Validación del juego | Curva decreciente, negativa o > 4000 en el rango | Rechazada |
| T-A15 | "Como llegó" | `x` sintéticas y ecuaciones de fábrica | Desviaciones correctas |
| T-A16 | Registro | Sesión simulada | Todas las tramas con hora, en orden |
| T-A17 | Acta y CSV | Generar acta; generar CSV de proyecto | CSV **byte a byte** igual al que produce `crearproyecto.java:50-53` + `medicion.java:1562` para los mismos datos |
| T-A18 | Formato numérico | Formatear los 51 valores de fábrica; medir la longitud de cada `#S` | Formato de `PROTOCOLO` §3; **longitud de `#S` > 48 detectada** (O-01) y la app se niega a enviarla |
| T-A19 | PIN | Entradas de 3, 4, 5 dígitos y con letras | Sólo 4 dígitos aceptados |

**Firmware en simulador (MPLAB X SIM).** No son JVM, pero sin ellas la no regresión no se puede
afirmar bit a bit.

| ID | Qué | Pasos | Esperado |
| :--- | :--- | :--- | :--- |
| T-A20 | No regresión de ecuaciones | Ejecutar las 12 funciones de 2020 y la evaluación V3.6 con coeficientes de fábrica para `x` = 0..65535 | **Cero diferencias**. Se guarda la tabla como referencia de T-A04 |
| T-A21 | Negativos en 2020 | Con el código de 2020 compilado por XC8 2.10, `x` que den resultado negativo en cada código | Valor emitido anotado: es la referencia de RF-FW-06 |
| T-A22 | Desbordamiento de recepción | Inyectar 200 bytes seguidos y tramas de 49, 60 y 255 bytes | Ninguna escritura fuera del buffer; el equipo responde después |
| T-A23 | Números | `#S` (con límite ampliado, sólo en simulador) con `-8.654100E-05`, `0.6196681`, `1e5`, `abc`, `nan` | Aceptados los dos primeros; `#ERR,FORMATO#` para los inválidos |
| T-A24 | EEPROM | Marca borrada; CRC alterado; bloque válido | `DEF`, `DEF`, `CAL` |
| T-A25 | Compilación | Compilar la base con XC8 2.10 | `.hex` idéntico a `d089f962…`, o diferencia documentada (puerta P3) |
| T-A26 | Redondeo de XC8 | Comparar la emulación de T-A04 con T-A20 | Iguales; si no, R-05 se materializa y RF-APP-10 se revisa |

### 5.2 (b) Contra equipos sin grabar (V3 2020 y SLV-002)

Ninguna de estas pruebas escribe nada en el equipo. En SLV-002 no hay `e`: se usa `6` e inversión.

| ID | Qué | Pasos | Esperado |
| :--- | :--- | :--- | :--- |
| T-B01 | Detección | Conectar la app | SLV-002 → "V3 2020 sin `e`"; un V3 2020 con `e`, si aparece, → "con `e`". El equipo sigue respondiendo a `1` después |
| T-B02 | Coherencia por inversión | Sobre un patrón, los 12 códigos | Los 12 intervalos de `x` se solapan (repite `HISTORIA.md`) → APTO en esa parte |
| T-B03 | Patrones antes de grabar | RF-APP-13 con `6` sobre P1-P31 | `x` (intervalo) por patrón y sesión. **Es la línea base "como llegó"** |
| T-B04 | Repetibilidad | 10 lecturas con `6` sobre 3 patrones de distinto nivel | CV por nivel. Con esto se fija el umbral de RF-APP-09 |
| T-B05 | Flujo de campo | Crear proyecto, medir con cada color y tipo, guardar, ver, exportar | CSV con el formato de PAR-02/PAR-11 |
| T-B06 | Tiempos | Medir el tiempo de respuesta de 50 peticiones y el silencio entre bytes de una respuesta | Fija el tiempo máximo y el silencio de RF-APP-02 |
| T-B07 | Negativos y saturación | Óptica tapada con `1`, `2`, `3`, `4`, `5`, `7`; patrón más alto con `1` y `2` | Respuestas anotadas: referencia de RF-FW-06 junto con T-A21 |
| T-B08 | `#V#` inocuo | Enviar `#V#`, esperar, enviar `6` | Mide sin responder; después `6` responde normal |
| T-B09 | Batería | `9` diez veces | Respuestas anotadas (referencia de RF-FW-08) |
| T-B10 | Pantalla STONE, línea base | Fotografiar cada pantalla; gatillo sobre un patrón con cada color de OTROS PAPELES y PAPEL TIPO I; página oculta "PRUEBA ADC" | Registro para comparar con T-C14 |

### 5.3 (c) Tras grabar la V3.6

| ID | Qué | Pasos | Esperado |
| :--- | :--- | :--- | :--- |
| T-C01 | Grabación | IPE: grabar, verificar, leer configuración | Verificación correcta; configuración `EC FF F7 FF 9F FF FF DF FE FF` |
| T-C02 | Arranque | Encender y esperar 3 s | Pantalla STONE igual que en T-B10 |
| T-C03 | Versión | `#V#` en el primer arranque | `#V,3.6,<fecha>,DEF#`; no mide (no se enciende la luz ni pita) |
| T-C04 | Lectura de fábrica | `#G` de los 12 códigos y `#GT#` | Iguales a la tabla de fábrica (≤ 1·10⁻⁶ relativo) |
| T-C05 | **No regresión, Bluetooth** | En `DEF`, sobre 3 patrones: alternar `e` y el código `k` 10 veces, para los 12 códigos | Para cada par, `resp_k` = `f_k(x)` de la `e` vecina dentro de la repetibilidad de T-B04 propagada por la pendiente de `f_k`. La igualdad exacta la garantiza T-A20 |
| T-C06 | `e` | 10 lecturas | Responde `::<x>` y concuerda con la inversión de `6` |
| T-C07 | Negativos y saturación | Repetir T-B07 | Mismas respuestas que T-B07 |
| T-C08 | **Continuidad de la `x`** | Repetir T-B03 con `e` sobre los mismos patrones | `x` dentro del intervalo de T-B03 más la deriva y la repetibilidad. **Si no**, la variante de SLV-002 tenía otra adquisición u otro factor de temperatura, y el "como llegó" se invalida (C-01) |
| T-C09 | Modo de pruebas | Pasarlo completo | APTO |
| T-C10 | Patrones | RF-APP-13 con `e` | Registro completo por patrón |
| T-C11 | Ritmo | Peticiones cada respuesta + 1 s durante 30 min | Ninguna perdida |
| T-C12 | Batería | `9` diez veces | Mismo formato y valores coherentes con T-B09 |
| T-C13 | Byte no código | Enviar `z` | Mide sin responder, como 2020 |
| T-C14 | **No regresión, STONE** | Repetir T-B10 | Mismas pantallas; mismos valores dentro de repetibilidad; "PRUEBA ADC" da la `x` |
| T-C15 | Gatillo con Bluetooth | Gatillo pulsado + `1` | Mismo comportamiento que 2020 (doble ecuación), salvo decisión P-02 |
| T-C16 | Trama sin medida | `#G,1#` | Responde `#G,1,...#`; ni luz ni pitido |
| T-C17 | Trama incompleta | `#G,1`, esperar 2,5 s, `e` | `e` responde normal; la trama no deja rastro |
| T-C18 | Desbordamiento | 200 bytes de basura; después `#V#` | El equipo sigue vivo y responde |
| T-C19 | Tabla única | `#S` de prueba en el código 1 (si O-01 se resuelve); gatillo con blanco de OTROS PAPELES | La pantalla usa la curva nueva |
| T-C20 | Trama durante medida | `6` y, a los 200 ms, `#V#` | `::<n>` y, según RF-FW-15, `#V,...#` después o nada; nunca otra medida |
| T-C21 | PIN | `#L,0000#`; `#L,2026#` | `#ERR,PIN#`; `#OK#` |
| T-C22 | Cierre de sesión | `#Q#` y `#S...#`; abrir sesión, apagar, encender, `#S...#` | `#ERR,BLOQUEADO#` las dos veces |
| T-C23 | Escritura | `#S` de un código, `#G`, apagar, encender, `#G`, `#V#` | Coincide; persiste; `CAL`. Tiempo de respuesta anotado |
| T-C24 | Formato | `#S` con `abc` | `#ERR,FORMATO#`; RAM sin cambios |
| T-C25 | Fábrica | `#F,1#`, `#F,*#` | Coeficientes de fábrica; T-C05 repetido en el código 1 da lo mismo que antes |
| T-C26 | Cambio de PIN | `#P,2026,1234#`, `#Q#`, `#L,2026#`, `#L,1234#`, apagar y repetir | `#OK#`; `#ERR,PIN#`; `#OK#`; persiste |
| T-C27 | Sin `@` | Registro de todos los bytes de T-C03 a T-C26 | Ningún `@` |
| T-C28 | Calibración de extremo a extremo | Asistente sobre blanco y amarillo intensos; escribir; volver a medir los patrones | Acta de antes y después; residuos dentro de lo mostrado por el asistente |
| T-C29 | Copia de coeficientes | Exportar | Archivo con los 51 valores, igual a `#G`/`#GT#` |
| T-C30 | App del cliente | Con la app propia del cliente de SLV-002, si se consigue | Funciona sin cambios. **Sin la app, esta prueba no se puede hacer** |

---

## 6. Riesgos y condiciones de grabación

| ID | Riesgo | Consecuencia | Condición antes de grabar |
| :--- | :--- | :--- | :--- |
| R-01 | **Chip protegido, sin copia.** SLV-002 tiene `CP = ON`; su firmware no se pudo leer (`HISTORIA.md`) | Grabar borra el original **sin vuelta atrás**, EEPROM incluida | T-B01 a T-B10 hechos y archivados; autorización P6 firmada sabiendo esto |
| R-02 | **El fuente no es el grabado.** SLV-002 no responde a `e`; los `.hex` de 2020 sí lo tienen. Sólo está probado que sus 12 fórmulas son las de 2020, **no** su adquisición, su `+200` ni su factor de temperatura | La V3.6 puede medir distinto aunque esté en `DEF`, y el "como llegó" de T-B03 dejaría de ser comparable | Aceptarlo por escrito; T-C08 lo detecta después |
| R-03 | **Reproducibilidad de XC8 2.10.** No se sabe si la base compila idéntica a `d089f962…` | Sin eso, cualquier diferencia de comportamiento no se puede atribuir al cambio | Puerta P3 (T-A25) cerrada o diferencia aceptada por escrito |
| R-04 | **Autorización del propietario** (Concesionaria Vial Andina para SLV-002) | Grabar sin ella no es una opción | Puerta P6 |
| R-05 | La coma flotante de XC8 no redondea igual que la JVM | La inversión de la app y la comparación de coeficientes fallan en el último bit | T-A26 |
| R-06 | Un corte de alimentación durante `#S` deja el CRC mal | **Todo** el bloque vuelve a fábrica, no sólo el código escrito (O-04) | La app guarda la copia de RF-APP-22 antes de cada escritura |
| R-07 | Regenerar drivers MCC | Se pierde `adcc.c:159` y las lecturas salen × 16 | Revisión del diff (RF-FW-02) |
| R-08 | No hay placa de pruebas confirmada | La primera grabación real sería en un equipo del cliente | `README.md`, reglas: "prueba previa en otra placa si la hay". Si no la hay, se dice en la autorización |
| R-09 | La app del cliente no está disponible | No se puede demostrar "sigue funcionando sin cambios" | Aceptarlo por escrito o conseguir la app (T-C30) |

**Orden de grabación.** Sólo con P2 a P6 cerradas (`ROADMAP.md`): PICkit 3 con IPE 5.50, grabar,
verificar, leer configuración (T-C01), y a continuación T-C02 a T-C09 **antes** de escribir ningún
coeficiente.
**[MOD r1.1]** Se graba cuando se cumplen **G1-G5** (§6 bis), que es la condición del veredicto de P2
(aprobado con condiciones, 18-sep-2026 19:48) y la del `ROADMAP.md` P7. Después: T-C01 a T-C09 y
T-C12 a T-C18, T-C20, T-C27, T-C36 **antes de cualquier escritura**; las escrituras (T-C19, T-C21 a
T-C26, T-C31, T-C32, T-C35) sólo con C1-C5 cumplidas. Orden detallado en `TDD-V3.6.md`.
*Texto r1.0:* «Sólo con P2 a P6 cerradas…»

## 6 bis. Condiciones de la revisión de arquitectura (P2) [NUEVO r1.1]

Veredicto de P2, 18-sep-2026 19:48: **APROBADO CON CONDICIONES.** Estado de cada condición a las
20:05 del mismo día. En `ROADMAP.md`, G1-G5 están en la puerta P7 y C1-C5 en la puerta **P7-bis**
("Condiciones para calibrar"); esta sección les da criterio de aceptación y prueba.

**[MOD r1.2] Estado a las 10:30 del 19-sep-2026.** Las tablas de abajo son las de la r1.1 (20:05 del
18-sep) y se conservan como texto anterior.

| Cond. | Estado r1.2 | Evidencia |
| :--- | :--- | :--- |
| G1 | **Cumplida** para la 3.6 (`f75ff88`) y la 3.6.1 (`8860445`), salvo que `CAMBIOS` no cita `8860445` | `.gitattributes` con `*.hex -text` (`1a57249`); md5 del blob de `8860445` = `8736c05d…` = declarado |
| G2 | **Cumplida** | `pruebas/T-A20.md` (C-19 cerrada): 786 432 comparaciones, 0 diferencias; repetida con la 3.6.1 (`CAMBIOS` §7.4); y `#E` 60/60 exacto en SLV-002 |
| G3 | **Ya no realizable** tal como estaba escrita | El firmware original se borró el 18-sep a las 20:24. Quedó: T-B11 (barrido), T-B12 (ICSP) y 8 blancos por pantalla (acta) |
| G4 | **Cumplida con la V3.6 del 18-sep** (19-sep, 08:53, app 3.6.2) | Acta: `#V,3.6,2026-09-18,DEF,0000#`; 12 `#G` y `#GT#` iguales a fábrica a 4 ulp; `#E` 60/60. **Con la 3.6.1 grabada a las 09:36, pendiente** (T-C37) |
| G5 | Parcial | Esta revisión; quedan C-36 a C-47 |
| C1 | **No cumplida por la letra; contradicción abierta C-47** | T-A23 medida (ida y vuelta hasta 7 ulp); la app tolera 8 y verifica con `#E` tras `#S` (RF-APP-07 r1.2). Si eso sustituye a C1 lo decide la revisión de arquitectura |
| C2 | Cumplida en la app, más el criterio de `#S` | `Asistente.java:179-241,347-353` (3.6.4); C-27 y C-42 abiertas |
| C3 | Implementada | `AdminActivity.java:468-556` (3.6.4); T-A32 sin prueba automática |
| C4 | **Cumplida en el firmware 3.6.1** (`calibracion_v36.c:621`), grabada en SLV-002 el 19-sep a las 09:36; **sin verificar en equipo** (ni `#V#` leído tras la grabación, C-36, ni T-C32) | T-A30 pasa en simulador. La app no ofrece `#ST` y su `tramaST` sólo limita `X_0` (`Tramas.java:240-254`) |
| C5 | Parcial | `#E` exacto en SLV-002 (prueba 5, 60/60). T-C05 completa y T-C23 pendientes |

### Para grabar (G1-G5)

| Cond. | Requisito y criterio de aceptación | RF | Prueba | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **G1** | Fuente y `.hex` en un commit; `.hex` con su md5 al lado, md5 de cada fuente, hash del commit en `CAMBIOS-V3.6.md`; fin de línea del `.hex` fijado (`.gitattributes`: `*.hex -text`) | RF-FW-27 | T-A28 | **Parcial.** Commit `319345f` (20:01) contiene fuente, `.hex`, `hex/RetroVertical_V3.6.hex.md5` y `hex/fuente.md5`. **Pero** el md5 declarado (`680b6a7d…`) es el del `.hex` con CRLF del árbol de trabajo; el blob del commit está en LF y da `32d636f8…` (C-32). Falta el hash en `CAMBIOS` |
| **G2** | T-A20 en MDB: 0 diferencias en los 12 códigos, `x` de 0 a 65535, entre la base 2020 y la V3.6 en `DEF` | RF-FW-04, 05, 06 | T-A20 | **Declarada cumplida** (`CAMBIOS-V3.6.md` §3.1: 0 diferencias). Arnés, generador y guion MDB en `pruebas/`, que coinciden con el `calibracion_v36.c` actual. **Falta el informe** `pruebas/T-A20.md` que `CAMBIOS` cita y no existe (C-19): recuentos por código y md5 |
| **G3** | Línea base de SLV-002 antes de grabar: T-B03, T-B07, T-B09, T-B10 | RF-APP-13, 25; RF-FW-06, 08, 10 | T-B03, T-B07, T-B09, T-B10 | **Pendiente.** Hechas antes: T-B11 (barrido) y T-B12 (ICSP); T-B10 parcial (8 blancos por pantalla). La app cubre T-B07 y T-B09 (esta con 3 repeticiones, no 10) con `LineaBase` |
| **G4** | Tras grabar: verificación con IPE (T-C01), `#V#` (T-C03) y `#G`/`#GT#` de fábrica (T-C04) | RF-FW-16, 18, 26 | T-C01, T-C03, T-C04 | **Pendiente** (tras grabar). Riesgo: con la tolerancia de 1 ulp de la app, T-C04 **puede fallar** en un chip recién grabado (C-12) |
| **G5** | Documentos reconciliados | — | Revisión | **Parcial.** Esta SPEC r1.1 y `TDD-V3.6.md` integran P2; quedan abiertas C-09, C-15, C-16, C-18, C-19, C-32 y C-34, que piden cambios en otros documentos o en el código |

### Para calibrar (C1-C5)

| Cond. | Requisito y criterio de aceptación | RF | Prueba | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **C1** | T-A23: 9 cifras en `printf` y `strtod` de XC8, ida y vuelta **exacta** (0 ulp); T-A24: EEPROM (cabecera, CRC por registro) | RF-FW-22, 24 | T-A23, T-A24 | **T-A23 falla** (±2 ulp al imprimir, ±3 al leer; `CAMBIOS` §3.3). T-A24 hecha en simulador (`CAMBIOS` §3.2) |
| **C2** | La curva se valida en todo el rango de `x` (200-4400) y con grado + 2 niveles distintos | RF-APP-15 | T-A13, T-A14 | **Implementada** en la app (`Asistente.java:99-147,186-189`; `Ajuste.java:48-52`); pruebas JVM en verde. Queda C-27 (negativos bajo la `x` mínima) |
| **C3** | Si `#G` no coincide tras `#S`, restauración automática del estado anterior | RF-APP-17 | T-A32, T-C23 | **Implementada** (`AdminActivity.java:442-495`). Sin prueba automática: la lógica vive en la actividad (T-A32 pendiente) |
| **C4** | Límites de `#ST`: `F(T)` en [0,5 ; 1,5] para `T` de 0 a 831 | RF-FW-19, RF-APP-23 | T-A30, T-A37, T-C32 | **Parcial**: firmware y app sólo limitan `X_0` (`calibracion_v36.c:299-300,542`; `Tramas.java:228-240`). La app no ofrece `#ST`. `ROADMAP.md` P7-bis la da por cumplida en la app (C-34) |
| **C5** | T-C05 con `#E` y T-C23 | RF-FW-04, 28, 19 | T-C05, T-C23 | **Pendiente** (tras grabar) |

### No bloqueantes (anotados en la revisión)

| Tema | Dónde | Tratamiento |
| :--- | :--- | :--- |
| Anillo UART1 de 64 bytes frente a tramas de 96 | `mcc_generated_files/uart1.c:57` | O-11 bis; se mide en T-C34. La app no envía tramas `#` mientras espera una medida |
| Un `#` se traga los bytes que lleguen durante 2 s | `uart_module.c:94-99,113-116` | Documentado en RF-FW-13; T-C17, T-C36 |
| Fallos de PIN en `#P` | `calibracion_v36.c:549-557` | Adoptado en RF-FW-17 (cuentan y bloquean); C-13 |
| UART2 sin límite | `uart_stone.c:92-99` | O-11; defecto de 2020 que se conserva (la STONE no se toca) |

---

## 7. Observaciones al protocolo

No se cambia `PROTOCOLO-V3.6.md`. Se anotan para quien lo mantiene.

- **O-01 — `#S` no cabe en 48 bytes.** Con el formato obligatorio, `#S,d,-8.654100E-05,…#` con cuatro
  coeficientes negativos ocupa 61 bytes; con los cuatro positivos, 57. El límite de §3 es 48. **`#S`
  es inutilizable tal como está**, y con él el asistente. `#ST` (46 bytes como máximo) sí cabe, y las
  respuestas `#G` (hasta 61 bytes) no están limitadas por el buffer de recepción.
- **O-02 — 7 cifras significativas no bastan para un `float` de 32 bits.** Hacen falta 9 para que
  texto → `float` → texto → `float` devuelva el mismo valor. Consecuencias: un `#G` releído y reenviado
  con `#S` puede cambiar el último bit, y la comparación de coeficientes tiene que ser con tolerancia
  (RF-APP-07). No afecta a la no regresión mientras fábrica se restaure con `#F` (RF-FW-05).
- **O-03 — `CAL`/`DEF` es global.** Tras un `#F,k#` con el resto calibrado, `#V#` dice `CAL` aunque
  ese código esté en fábrica. No hay forma de saber qué códigos están ajustados sin comparar `#G`.
- **O-04 — Un solo CRC para todo el bloque.** Un corte durante una escritura devuelve los 12 códigos a
  fábrica. Tampoco se dice qué CRC-16 (polinomio, valor inicial), lo que hace falta para T-A24.
- **O-05 — Trama durante una medida.** No se dice qué pasa si una trama `#...#` llega mientras mide
  (en 2020, `gui.c:342` lo borra todo). RF-FW-15 fija lo mínimo.
- **O-06 — PIN.** 4 dígitos en claro por SPP, sin límite de intentos: 10 000 pruebas lo abren. La
  sesión no caduca si se corta el enlace sin `#Q#`.
- **O-07 — Fecha de `#V#`.** No se dice si `<AAAA-MM-DD>` es la fecha de compilación o de versión.
- **O-08 — `e` con saturación.** `e` pasa por `arreglar_dato()` (`ecuacionesCalibracion.c:191-193`
  → `:56-60`): una `x` > 4000 sale como 0. "Sin ecuación" no es "sin tratamiento".
- **O-09 — Cita de EEPROM.** §4 dice que 2020 ocupa `eeprom_manager.h:9-19` (direcciones 0-52). Las
  definiciones siguen hasta la línea 23 y la dirección 70. No choca con 0x100, pero la cita es inexacta.
- **O-10 — No hay forma de evaluar una ecuación sin medir.** Por eso la no regresión exacta sólo se
  puede demostrar en simulador (T-A20); en el equipo es estadística (T-C05).

**[NUEVO r1.1] Resolución.** La revisión 1.1 del contrato (§4 bis) resolvió O-01 (96 bytes), O-02
(9 cifras; **pero XC8 no las convierte exactas**, C1), O-03 (máscara), O-04 (registro por código,
CRC-16/CCITT-FALSE), O-05, O-06, O-07 (`__DATE__`), O-08 (se mantiene) y O-10 (`#E`). **O-09 sigue
abierta**: §4 del contrato aún cita `eeprom_manager.h:9-19` (C-23). Observaciones nuevas:

- **O-11 — UART2 sin límite y anillo UART1 de 64 bytes.** `readUartStr2()` escribe
  `bufferPantalla[bufferindice++]` sin comprobar (`uart_stone.c:92-99`); `bufferindice` sólo vuelve a 0
  en `gui.c:293`. Y una trama `#` de 96 bytes no cabe en el anillo de 64 si el bucle está bloqueado
  (`CAMBIOS-V3.6.md` §4.5). No bloqueantes; T-C34.
- **O-12 — Semántica de `CAL`.** El contrato dice "`CAL` si hay calibración válida en EEPROM"; el
  firmware da `CAL` si la máscara no es 0 (algún valor en RAM distinto de fábrica). Tras `#F,*#`, o tras
  cambiar sólo el PIN, la EEPROM es válida y `#V#` dice `DEF`. Se propone cambiar el contrato a la
  semántica del código (C-11).
- **O-13 — No hay "fábrica" para la temperatura.** `#F` no toca `X_2`, `X_1`, `X_0` ni el PIN; volver
  a fábrica la temperatura exige `#ST` con los valores tecleados, que pasan por `strtod` (C1). Mientras
  la app no escriba `#ST` (RF-APP-23) no hay forma de cambiarla, así que no hay que volver.

---

## 8. Fuera de alcance

- **Equipos V4** (v4.0, V4.1): **[MOD r1.1]** irán a la **V4.6, que es un proyecto propio**:
  `D:\IT\P_RetroVertical_V4.6\`, con remoto privado `github.com/dieleoz/retrov4.6_2026`, y sus
  borradores `05_Documentacion/SPEC-V4.6-BORRADOR.md`, `TDD-V4.6-BORRADOR.md` y
  `PROTOCOLO-V4.6-BORRADOR.md` (comprobado el 18-sep-2026). Lo aprendido aquí que le aplica va a
  `APRENDIDO-DE-V3.6.md` de ese proyecto. La app V3.6 los detecta (RF-APP-03) y no opera con ellos.
  *Texto r1.0:* «irán a una V4.6 en el repositorio `P_RetroReflectometro_Vertical`.» Era un error: ese
  repositorio es el de la línea V4.1/V5.
- **Cambios de la pantalla STONE** (`04_Pantalla_STONE/`): no se tocan.
- **Mejoras de la app** más allá de la paridad (`ROADMAP-MEJORAS-App.md`): después de la V3.6.
- La placa y su hardware (ganancia, LED): no se cambian.

---

## 9. Contradicciones abiertas

Se dejan sin elegir hasta que alguien mida o decida.

- **C-01 — `ROADMAP.md` vs `HISTORIA.md`.** El ROADMAP afirma que la `x` de SLV-002 "sigue valiendo
  tras grabar la V3.6". `HISTORIA.md` sólo demuestra que las 12 fórmulas son las de 2020; la
  adquisición y el factor de temperatura de esa variante no se pueden leer (chip protegido). Se cierra
  con T-C08.
- **C-02 — `ROADMAP.md` P8 vs `PROCEDIMIENTO` §3.** P8 dice "medir P1-P31 con `e`, ajustar por
  color". Con P1-P31 sólo se pueden ajustar blanco y amarillo, y **sólo las curvas intensas**: no hay
  ningún patrón de tipo I.
- **C-03 — `PROCEDIMIENTO` desfasado.** Dice que "el fuente no está en este repositorio" y cita la
  ruta de `old\`; ahora está en `01_Firmware/base_2020_d089f962/`. Remite a `CLAUDE.md` §3 ("equipo
  patrón"), que es del otro repositorio. Su §3 deja abierta la correspondencia intenso/opaco que su
  §1.4 bis da por resuelta.
- **C-04 — `README.md`.** Enlaza `ARQUITECTURA.map`, que no existe todavía (en curso según
  `ROADMAP.md`), y describe `03_App_Movil/`, que está vacío.
- **C-05 — `HISTORIA.md`.** Habla de "los dos `.hex` de 2020"; en el repositorio sólo está uno
  (`d089f962…`). El otro (`43f9f74b…`) está en `old\`.
- **C-06 — `PROTOCOLO` §3.** Límite de 48 bytes frente a la longitud de `#S` (O-01).
- **C-07 — SPEC-06 frente al código de la app de campo:** el *spinner* tiene 11 tipos, no 2
  (`medicion.java:1548`); Menos sí tiene implementación (`medicion.java:833`); Finalizar lleva a la
  pantalla de correo, no a abrir proyecto (`medicion.java:1540`); en el árbol de trabajo actual el
  correo sale por `mail.itvial.com:26` y el bloque de Gmail está comentado (`Enviarcorreo.java:70-73,
  77-87`); y el ejemplo `@LEERV,255.3@` no puede darse, porque el equipo envía un entero.

**[NUEVO r1.1] Contradicciones encontradas al validar contra el código (18-sep-2026, 19:50-20:10).**
Las que tienen decisión la llevan; las demás quedan abiertas.

- **C-05 y CA7 — cerradas por medida.** Los "dos `.hex` de 2020" son **el mismo fichero con distinto
  fin de línea**: `d089f962…` es la versión con CRLF y `43f9f74b…` la misma con LF. Comprobado con
  `tr -d '\r' | md5sum` sobre las 9 copias de `old\VERTICAL\` (las 7 con CRLF dan `43f9f74b…` al
  quitar los CR; las 2 restantes ya estaban en LF) y sobre el blob que git guarda de
  `base_2020_d089f962/hex/` (`43f9f74b…`). Ver C-32.
- **C-08 — Referencia rota.** PAR-08 remite a "C-08", que no existía; se refiere a C-07.
- **C-09 — Orden de detección.** El encargo de esta revisión y el `README.md` de la app (tabla de
  pruebas, fila 2) dicen `#V#`, `9`, `@LEERV`; el código hace `#V#`, `e`, `6`, `@LEERV`
  (`Pruebas.java:243,275,283,291`), como RF-APP-03. **Decidido:** manda RF-APP-03; se corrige el README.
- **C-10 — Rojo.** RF-APP-14 r1.0 dice "un solo nivel"; el CSV tiene dos (P11 = 194, P12 = 227).
  **Decidido:** manda el CSV (RF-APP-14 r1.1).
- **C-11 — `CAL`.** Contrato §3 frente a `calibracion_v36.c:452`. Propuesta en O-12.
- **C-12 — Tolerancia de coeficientes.** Contrato §4 bis (9 cifras, 1 ulp) frente a `CAMBIOS` §3.3
  (XC8: ±2 ulp al imprimir, ±3 al leer) frente a la app (1 ulp: `Ecuacion.java:290-300`) frente a
  T-C04 r1.0 (10⁻⁶ relativo, que sí pasaría). **Consecuencia:** con el `.hex` y la app de hoy, la
  prueba 5 puede dar NO APTO en un chip recién grabado y bloquear el modo administrador. **Decidido:**
  conversión exacta en el firmware antes de G1 (RF-FW-22); si no se hace, la app acepta 2 ulp en
  valores de ROM (RF-APP-07).
- **C-13 — `#P` y el bloqueo de PIN.** El contrato sólo cuenta `#L`; el firmware cuenta también `#P`
  (`calibracion_v36.c:549-557`). **Decidido:** se adopta el código (RF-FW-17).
- **C-14 — Longitud de `#K#`.** El contrato pide limitarla o documentarla; el firmware no la limita
  (~280 bytes). **Decidido:** se documenta (RF-FW-29); la app admite hasta 512 bytes
  (`Receptor.java:15`).
- **C-15 — Estado de las puertas.** `README.md` dice "Revisión de arquitectura (P2): En curso" y
  "Firmware… Pendiente: reproducibilidad"; `ROADMAP.md` P1 "Subagente en curso" y P2 "Tras P1"; el mapa
  (`ARQUITECTURA.map`, CA4) da la reproducibilidad por pendiente. P2 dio veredicto a las 19:48 y P3 está
  cerrada (`CAMBIOS` §1). Abierta: la corrige quien mantiene esos documentos.
- **C-16 — Estado de la app.** El encargo habla de 42 pruebas JVM y APK md5 `6f8086f8…`; a las 19:59
  hay **45** (en verde) y el APK del árbol es `6cc455d3…` (19:56). Los APK no se versionan
  (`.gitignore`), así que **ningún APK está atado a un commit**. Abierta.
- **C-17 — Qué se pierde tras una medida.** §1 r1.0 decía "lo recibido durante la medida se pierde";
  lo exacto es "lo recibido antes de `clearBuffer()`", y lo que llega en la pausa de 500 ms dispara otra
  medida. **Decidido:** §1 r1.1 y RF-APP-01.
- **C-18 — Etiquetas de `LineaBase`.** `LineaBase.java:9-13` llama "T-B03" a los 12 códigos sobre un
  patrón, que es T-B02; T-B03 es la sesión P1-P31. Y hace `9` tres veces (`:27`), no diez (T-B09).
  Abierta: corrige el agente de la app.
- **C-19 — Informe de T-A20.** `CAMBIOS-V3.6.md` §3.1 remite a `pruebas/T-A20.md`, que no existe a las
  20:05 (sólo arnés, generador y guion MDB). Abierta: la cierra el agente de firmware.
- **C-20 — Negativos "explícitos".** RF-FW-06 r1.0 pedía una conversión explícita; el firmware
  conserva la de 2020 (`CAMBIOS` §4.3). **Decidido:** se acepta (RF-FW-06 r1.1).
- **C-21 — Aritmética de la inversión.** RF-APP-10 pide `float` de 32 bits; `Inversion.java:52` usa
  `respuestaFirmware` (`double`, Horner: `Ecuacion.java:245-267`). **Decidido:** manda RF-APP-10.
- **C-22 — "Sin remoto".** `README.md:52` de la V3.6, `README.md:7` de la V4.6 y
  `APRENDIDO-DE-V3.6.md:8` dicen "git local sin remoto"; los dos repositorios tienen remoto privado
  (`dieleoz/retrov3.6_2026`, `dieleoz/retrov4.6_2026`, `git remote -v`, 18-sep-2026). Corregido en esta
  SPEC, en `APRENDIDO` y en los dos `README.md`, con el texto anterior al lado.
- **C-23 — Cita de EEPROM en el contrato.** O-09 sigue sin resolver en §4 del contrato.
- **C-24 — Pausa entre peticiones.** RF-APP-01 r1.0 (respuesta + 1 s) frente a la app (1500 ms desde
  el envío, 600 ms desde el último byte). **Decidido:** RF-APP-01 r1.1.
- **C-25 — Silencio y plazo.** RF-APP-02 r1.0 (300 ms, 5 s) frente a la app (180 ms, 2500 ms).
  **Decidido:** provisionales de la app hasta T-B06.
- **C-26 — Repetibilidad.** RF-APP-09 r1.0 (10 lecturas, sin veredicto) frente a la app (5 lecturas,
  SD ≤ 10). **Decidido:** RF-APP-09 r1.1 (10 lecturas, veredicto sólo de avería).
- **C-27 — Letra de C2.** "Validar la curva en todo el rango de `x`" frente a que todas las curvas de
  fábrica son negativas hacia `x` = 200. La app bloquea desde la `x` mínima medida y sólo avisa por
  debajo (`Asistente.java:108-147`). Abierta: P-09.
- **C-28 — Orden de grabación.** §6 r1.0 ("P2 a P6 cerradas") frente a `ROADMAP.md` P7 ("G1-G5").
  **Decidido:** G1-G5 (§6 r1.1).
- **C-29 — `#KC#`.** El contrato dice "vacía el registro"; el firmware vacía las ranuras y no el
  contador `n` (`calibracion_v36.c:567-571`). **Decidido:** se documenta (RF-FW-29).
- **C-30 — `#F` y la temperatura.** Ver O-13. Abierta, sin efecto mientras RF-APP-23 siga vigente.
- **C-31 — `@` en lo recibido.** RF-APP-12 pide comprobar que ninguna respuesta lleva `@`; la app sólo
  lo comprueba en lo que envía (`Tramas.java:43-45`). **Decidido:** se cubre revisando el registro
  (T-C27) y se pide la comprobación en la app.
- **C-32 — El md5 de un `.hex` depende del fin de línea.** Con `core.autocrlf = true` y sin
  `.gitattributes`, el `.hex` V3.6 del árbol (CRLF) da `680b6a7d…` y su blob en el commit `319345f` (LF)
  da `32d636f8…`; la base, `d089f962…` y `43f9f74b…`. Un clon con otra configuración "no cuadra" con el
  md5 declarado aunque la imagen de memoria sea idéntica. **Decidido:** G1 exige fijar el fin de línea
  (RF-FW-27 r1.1). Esto cierra C-05 y CA7.
- **C-33 — `::0` con `e`.** RF-APP-05 y `LecturaX.java:52-55` dicen "saturado o negativo"; con `e` sólo
  puede ser saturación (RF-APP-06 r1.1). **Decidido:** manda RF-APP-06 r1.1.
- **C-34 — C4 "cumplida".** `ROADMAP.md` P7-bis dice que la app ya cumple C2-C4. En la app, C4 se
  cumple **sólo porque no ofrece `#ST`** (RF-APP-23), y su `tramaST` limita sólo `X_0`
  (`Tramas.java:237-240`). En el firmware, `#ST` acepta cualquier `X_1` y `X_2` finitos
  (`calibracion_v36.c:536-548`; `CAMBIOS-V3.6.md` §4.4 lo reconoce). Con el criterio de RF-FW-19 r1.1,
  **C4 está parcial**. Abierta hasta T-A30.
- **C-35 — Destino de la V4.6.** §8 r1.0 mandaba la V4.6 al repositorio `P_RetroReflectometro_Vertical`;
  es un proyecto propio (`D:\IT\P_RetroVertical_V4.6\`). **Decidido:** §8 r1.1.

**[NUEVO r1.2] Estado de las anteriores y contradicciones nuevas (19-sep-2026, 09:30-10:30).**

Cambian de estado:
- **C-01 — con evidencia, sigue abierta.** El acta de SLV-002 muestra, por pantalla y con las mismas
  fórmulas, 80-90 cuentas de `x` **menos** con la V3.6 en x = 1700-2300 (P3, P2, P28, varias tandas);
  en la zona alta no es concluyente. Apunta a que la adquisición del original no era la del fuente
  2020, pero ya no se puede comprobar con `e` (original borrado). T-C08 no se puede hacer como estaba
  escrita.
- **C-12 — cerrada por medida.** Tolerancias vigentes `ULP_G = 4`, `ULP_S = 8` (RF-APP-07 r1.2). El
  contrato (O-02, 1 ulp) no es alcanzable con XC8 2.10: se anota en `PROTOCOLO-V3.6.md` §4 ter.
- **C-19 — cerrada.** `pruebas/T-A20.md` existe.
- **C-21 — sigue abierta.** `Inversion.java:58` usa `respuestaFirmware` (`double`, Horner) en la 3.6.4.
- **C-30 — con evidencia, sigue abierta.** Ver RF-FW-20 r1.2.
- **C-34 — cerrada en el firmware.** La 3.6.1 aplica C4 completa (`calibracion_v36.c:620-621`). Sin verificar en equipo.

Nuevas:
- **C-36 — Qué firmware lleva SLV-002.** El encargo de esta revisión, `ROADMAP.md` y `CAMBIOS-V3.6.md`
  (cabecera) dicen la V3.6 del 18-sep. El commit `869d3c6` y
  `01_Firmware/lecturas_equipos/SLV-002/grabacion_V3.6.1_2026-09-19.log` registran otra grabación a
  las 09:36:15 con memoria de programa hasta `0x1d07f`, que es el tamaño del `.hex` de la 3.6.1 (último
  byte de datos `0x1d009`) y no el de la 3.6 (`0x1bcad`). El registro no guarda el md5 del fichero
  cargado. **Abierta hasta que `#V#` responda** `…,2026-09-19,…` (T-C37).
- **C-37 — La versión no distingue 3.6 de 3.6.1.** `#V#` responde `3.6` en las dos
  (`calibracion_v36.h:16`); la 3.6.1 sólo se distingue por la fecha de compilación (`CAMBIOS` §6) y la
  app decide "3.6.1" si la fecha es ≥ 2026-09-19 (`Sesion.java`, `limitesS()`). Una recompilación de la
  3.6 posterior a esa fecha pasaría por 3.6.1. Cambiar la cadena exige tocar el firmware: decide el
  propietario. Abierta.
- **C-38 — Primer disparo: ¿deriva o asentamiento?** La prueba 4 de la 3.6.3 interpretaba la
  diferencia entre la `e` inicial y la final como deriva (`Coherencia.java:16-18,140,217`; acta:
  3004 → 3021 → 3023). Con los datos de RF-APP-28, la `e` inicial es un primer disparo y sale baja por
  sí sola. La 3.6.4 antepone un disparo de asentamiento (`Pruebas.java:434-438`), pero la causa física
  no está medida y un disparo no basta del todo. Abierta hasta T-C38.
- **C-39 — Los XI no se ordenan por su certificado.** Blanco XI: P4 (828) → x 3361, P7 (768) →
  3231-3237, P6 (772) → 3115, P1 (762) → 3071. Amarillo XI: P20 (782) → 2798-2804, P23 (714) →
  2723-2757, P5 (740) → 2526-2584, P9 (705) → 2556, P8 (721) → 2518, P10 (680) → 2462. Medias de los
  disparos 2-9 (o 2-3), sin el descolgado de P7. Hipótesis de orientación del patrón, **sin medir**.
  Con la opción C, el residuo XI calculado en este trabajo es de −7 a +5 % en amarillo y de −3 a +3 %
  en blanco. Abierta.
- **C-40 — P24 tiene tres candidatas** (RF-APP-30). (a) Sesión de las 08:59: dos series etiquetadas
  P24, 2048-2076 y 2438-2453 (Δ ≈ 370) en 30 s. (b) La carpeta `07 pruebas/19092026_0900/p29_p24_p23_p5/`
  se llama "P29, P24, P23, P5", pero su CSV etiqueta la serie de las 09:23:40 como **P20** (mediana 2805,
  igual que el P20 de las 09:15, 2798). Otros amarillos IV dan 1470-1724 y los IX (573-583) 2096-2202.
  Abierta: no se elige cuál es P24 ni si la serie de las 09:23:40 es P20 o P24.
- **C-41 — P32 duplicado** (RF-APP-31). Abierta: la decide Diego.
- **C-42 — Tres rangos de `x` "de uso".** C2 de la app: 200-4400 (`Asistente.java:161-162`); `#S` del
  firmware: 600-4300 (`calibracion_v36.c:316-317`; el 600 "no sale del código", `:314-315`); inversión
  de la app: 180-4400 (`Inversion.java:20,26`). Y el oscuro de la V3.6 en SLV-002 está en x ≈ 575
  (acta, G4). La app aplica hoy las dos primeras a la vez, así que no escribe nada que el firmware
  rechace; pero qué rango es el de uso, y si el 600 deja fuera a los tipo I oscuros (RF-APP-31), no
  está decidido. Abierta (P-12).
- **C-43 — Umbrales de cobertura sin medida.** RF-APP-14 r1.2: "rango estrecho" = < 20 unidades de R
  o < 30 % (`Asistente.java:33-36`). Deciden que `c` y `d` no se ajusten. No salen de ningún dato.
  Abierta.
- **C-44 — T-A23 "cerrada".** El encargo de esta revisión la da por cerrada con la 3.6.1. Como
  **medida** lo está (`CAMBIOS` §7.2); como **criterio** (0 ulp, C1) falla. Se escribe así en la TDD.
- **C-46 — Regla del disparo descolgado.** RF-APP-29 pide max(30 cuentas, 5 · 1,4826 · MAD); la de
  `Veredicto` de la 3.6.5 (`ff66f93`) es 5 · max(1,4826 · MAD ; 1,5) sin suelo de 30, y con
  los datos del 19-sep marca el 2004 de P2 (umbral 11,1), que es asentamiento, no un descolgado. No se
  elige por escrito: se cierra con T-A39 sobre las 17 series literales.
- **C-47 — C1 frente al rodeo de la app. Para la revisión de arquitectura; no se decide aquí.**
  *Lado C1* (condición de P2, 18-sep 19:48): ida y vuelta **exacta** texto → `float` → texto (0 ulp),
  T-A23. Medido con la 3.6.1: hasta 5 ulp en simulador y 7 con la emulación validada (`CAMBIOS` §7.2);
  `%.8E` solo, hasta 3. Motivo de C1: que `#G` tras `#S` pruebe que la EEPROM tiene lo enviado.
  *Lado de la app* (3.6.4 y 3.6.5): `ULP_S = 8` (`Ecuacion.java:93`, máximo medido + 1) y, tras cada
  `#S`, `#E` en 5 puntos contra la curva enviada con ±1 cuenta, que compara enteros sin pasar por texto,
  y restauración si algo falla (`AdminActivity.java:498-556`). Lo que el rodeo **no** cubre: un error de
  hasta 7 ulp en un coeficiente entre los 5 puntos de `#E` (se estima despreciable frente a 1 cuenta,
  **sin medir**) y la dependencia de que la emulación de XC8 siga siendo la cota. Alternativa: la
  conversión exacta propia en el firmware (P-10), que obliga a regrabar.
- **C-45 — RF-APP-03 contra sí mismo.** El texto [MOD 18-sep] prohíbe `e` y pone `9` como segunda
  sonda; el párrafo "Decisión r1.1" mantenía `e`. **Decidido:** manda el [MOD 18-sep] y el código
  (`Pruebas.java:247-297`); el párrafo queda retirado.

---

## 10. Preguntas abiertas para el propietario

- **P-01 — O-01:** cómo se resuelve `#S` (subir el límite, acortar el formato o partir la trama).
  Hasta entonces el asistente de calibración no puede escribir.
- **P-02 — Defectos de 2020:** ¿se conservan (doble ecuación con gatillo, `nivel[2]`, 0x09 sin rama) o
  se corrige alguno aceptando que cambia el comportamiento?
- **P-03 — Correo:** ¿se replica el envío SMTP con credenciales en el código, o se entrega el CSV por
  el menú de compartir de Android?
  **[MOD r1.2] Cerrada por RF-APP-32.5:** en la V3.6 se entrega **un ZIP por campaña por el menú
  Compartir de Android**, sin credenciales en la app. El envío SMTP de la app de campo (PAR-16) no se
  replica en la V3.6.
- **P-04 — Umbrales de APTO:** repetibilidad admisible; se proponen tras T-B04.
- **P-05 — Tipo I:** sin patrones de tipo I, ¿se dejan las 6 curvas opacas en fábrica?
- **P-06 — Una curva intensa para IV, IX y XI:** en SLV-002 el blanco XI sale +2-3 % y el IV y IX
  +28-50 % (`HISTORIA.md`). Una sola curva por color no puede corregir las dos cosas. ¿Contra qué tipo
  se ajusta?
- **P-07 — App del cliente de SLV-002:** ¿se puede conseguir para T-C30?
- **P-08 — Placa de pruebas:** ¿hay una placa H-IoT que no sea de un cliente para la primera grabación?

**[NUEVO r1.1] Estado y preguntas nuevas.** P-01 cerrada (96 bytes, contrato 1.1). P-02 cerrada
(contrato §4 bis). P-06 contestada en `ROADMAP.md` y L-07 de la V4.6 sólo en parte: sigue sin decir
contra qué tipo se ajusta. P-08: `ROADMAP.md` P7 dice que no hay placa de repuesto y que se graba
SLV-002.

- **P-09 — Letra de C2 (C-27):** ¿un negativo por debajo de la `x` mínima medida bloquea o sólo avisa?
- **P-10 — Conversión exacta (C1):** **[r1.2]** ahora ligada a C-47, que decide la revisión de arquitectura. ¿Se añade al firmware antes del commit de G1 (una sola grabación)
  o después (dos grabaciones y T-A20 dos veces)?
- **P-11 — Semántica de `CAL` (O-12):** ¿se cambia el contrato a la del código?

**[NUEVO r1.2]** **P-06 contestada** (Diego, 19-sep-2026, `ROADMAP.md`): **opción C**, compromiso XI e
IV/IX en los códigos `1` y `2`, por mínimos cuadrados con todos los patrones y sin ponderar por tipo;
el acta declara el error residual de cada tipo. Preguntas nuevas:

- **P-12 — Rango de uso de `x` (C-42):** ¿200-4400, 600-4300 u otro? ¿Se acepta que los tipo I oscuros
  (verde, azul) queden por debajo del límite de `#S`?
- **P-13 — Asentamiento (C-38):** ¿cuántos disparos se descartan? Propuesta: el N que dé T-C38.
- **P-14 — P24 y P32 (C-40, C-41):** ¿cuál de las dos series es P24 y qué patrones son P32a y P32b?
- **P-15 — Cadena de versión (C-37):** ¿`#V#` debe distinguir 3.6 de 3.6.1 (y de la 3.6.2 en obra)?
- **P-16 — Grado de la curva C del código `2`:** con los datos del 19-sep, grado 2 no cumple el
  límite de `#S` y grado 1 sí (T-A41). ¿Se acepta grado 1?
