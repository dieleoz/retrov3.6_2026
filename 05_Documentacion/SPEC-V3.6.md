# SPEC-V3.6 — Especificación del firmware y la app V3.6 del Retrorreflectómetro Vertical

**Estado: revisión 1.1 de la SPEC, 18-sep-2026, 20:10. Nada se ha grabado ni medido en un equipo con
V3.6.** El firmware está compilado (`.hex` md5 `680b6a7d…`, commit `319345f`) y probado sólo en
simulador; la app compila y pasa 45 pruebas JVM (ejecutadas en este trabajo a las 19:59). Una prueba
de simulador **falla** (T-A23: conversión de números, condición C1). El plan de pruebas ejecutable
está en [`TDD-V3.6.md`](TDD-V3.6.md).

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

**RF-FW-20 — Restaurar.** `#F,<k>#` y `#F,*#` restauran desde ROM (RF-FW-05). CA: T-C25.
Nota r1.1: `#F` no toca el factor de temperatura ni el PIN; no hay orden para devolver la temperatura
a fábrica salvo `#ST` con los valores de fábrica tecleados (O-13).

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

### 2.4 Estado del firmware frente a cada RF-FW [NUEVO r1.1]

Leído el 18-sep-2026 entre 19:50 y 20:05 (commit `319345f`, `.hex` md5 `680b6a7d…` con CRLF). Rutas
relativas a `01_Firmware/RetroVertical_V3.6.X/`. "Implementado" es **en el código**, no probado en
equipo.

| RF | Estado | Evidencia en el código | Pendiente |
| :--- | :--- | :--- | :--- |
| RF-FW-01 | Implementado | `mcc_generated_files/` idéntico a la base (`diff -rq`, 18-sep); `uart1.c:115,124,127`, `mcc.c:74` | T-C02, T-C05 |
| RF-FW-02 | Implementado | `measurement.c` sólo cambia en `:212`; `gui.c:299-301` igual a la base `:298-300`; `adcc.c` sin tocar | T-C08 |
| RF-FW-03 | Implementado | `ecuacionesCalibracion.c:49-67,137-198` iguales a la base salvo los cuerpos `:3-42` | T-C05, T-C07 |
| RF-FW-04 | Implementado | `calibracion_v36.c:228-233` (`aplicarEcuacion`, orden de 2020, no Horner) | Informe de T-A20 (G2) |
| RF-FW-05 | Implementado | `calibracion_v36.c:33-46` (literales), `gui.c:42-44`; `#F` copia de `coefFabrica`: `calibracion_v36.c:514-526` | T-C04, T-C25 |
| RF-FW-06 | Implementado (r1.1) | Misma asignación que 2020: `calibracion_v36.c:229` | T-A21, T-B07, T-C07 |
| RF-FW-07 | Implementado | `ecuacionesCalibracion.c:191-193` | T-C06 |
| RF-FW-08 | Implementado (r1.1) | `gui.c:268` (`nivel[6]`), cálculo `gui.c:329-341` igual | T-B09, T-C12 |
| RF-FW-09 | Implementado | `gui.c:296`; `#` excluido en `uart_module.c:94-99` | T-C13 |
| RF-FW-10 | Implementado | `ecuacionesCalibracion.c:70-134` igual; `uart_stone.c:89-101` sólo añade el registro | T-B10, T-C14 |
| RF-FW-11 | Implementado | Las 12 funciones llaman a `aplicarEcuacion()`: `ecuacionesCalibracion.c:3-42` | T-C19 |
| RF-FW-12 | Implementado (r1.1) | `gui.c:312-328`, `uart_stone.c:32-35`, `gui.c:343-347` sin cambios | T-C15 |
| RF-FW-13 | Implementado (r1.1) | `uart_module.c:61-117` (96 bytes, descarte, 2 s en `atenderAdmin`, `:113-116`) | T-C16, T-C17, T-C36 |
| RF-FW-14 | Parcial | UART1: `uart_module.c:87-91,100`. UART2 sin límite: `uart_stone.c:92-99` (O-11) | T-A22, T-C18, T-C34 |
| RF-FW-15 | Implementado (r1.1) | `uart_module.c:94-99,108-117`; la trama nunca pasa por `bufferData` | T-C20 |
| RF-FW-16 | Implementado (r1.1) | `calibracion_v36.c:201-222,380-398,448-456` | T-C03 |
| RF-FW-17 | Implementado (r1.1) | `calibracion_v36.c:72-74,297-298,318-322,457-474,549-557` | T-C21, T-C22, T-C31 |
| RF-FW-18 | Implementado | `calibracion_v36.c:475-485,527-535` | T-C04 |
| RF-FW-19 | **Parcial** | `#S`: `:503-513`; deshacer si la relectura falla: `:406-418`. **C4 sólo en `X_0`**: `:299-300,542` | T-A30, T-C23, T-C24, T-C32 |
| RF-FW-20 | Implementado | `calibracion_v36.c:514-526` | T-C25 |
| RF-FW-21 | Implementado | `calibracion_v36.c:549-563`, `pinValido` `:365-372` | T-C26 |
| RF-FW-22 | **No cumple C1** | `%.8E`: `calibracion_v36.c:374-378`; `strtod`: `:335-345`. Error de XC8: ±2 ulp / ±3 ulp | Conversión exacta; T-A23 |
| RF-FW-23 | Implementado | `#OK#` tras `guardarEeprom()` y relectura: `calibracion_v36.c:127-153,408-418`; escritura con `GIE = 0`: `mcc_generated_files/memory.c:172-192` | T-C23, T-C35 |
| RF-FW-24 | Implementado (r1.1) | `calibracion_v36.c:52-198` | T-A24 hecha en simulador; T-C23 |
| RF-FW-25 | Implementado | Ninguna cadena emitida contiene `@` (búsqueda en `*.c`, 18-sep) | T-C27 |
| RF-FW-26 | Implementado | `mcc_generated_files/device_config.c` idéntico; `CP = ON` en `:97`; configuración del `.hex` igual (`CAMBIOS` §2) | T-C01 |
| RF-FW-27 | **Parcial (G1)** | Commit `319345f` (20:01) con fuente, `.hex` y md5; `md5sum -c hex/fuente.md5` sin fallos a las 19:58. Falta `.gitattributes` y citar el commit en `CAMBIOS` | T-A28 |
| RF-FW-28 | Implementado | `calibracion_v36.c:486-502`, `leerEntero` `:348-359` | T-A27, T-C05 |
| RF-FW-29 | Implementado | `calibracion_v36.c:236-292,564-571`; `uart_stone.c:89-101` | T-A33, T-C33 |
| RF-FW-30 | Implementado | `measurement.c:212`, `gui.c:268`, `uart_module.c:100` | T-A29, T-C08 |

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
**Decisión r1.1 sobre la desviación de la app: se mantiene este orden** (`#V#`, `e`, `6`,
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

### 3.8 Estado de la app frente a cada RF-APP [NUEVO r1.1]

Leído entre las 19:52 y las 20:05 del 18-sep-2026 (app en cambio; 45 pruebas JVM en verde a las 19:59
sobre las clases compiladas a las 19:55; APK en el árbol md5 `6cc455d3…`, 19:56, no versionado).
Rutas relativas a `03_App_Movil/RetroV36/app/src/main/java/com/dpi/retrov36/`.

| RF | Estado | Evidencia en el código | Falta |
| :--- | :--- | :--- | :--- |
| RF-APP-01 | Implementado (r1.1) | `EnlaceSerie.java:33,157` (UUID, socket inseguro); `Cliente.java:65-74,202-218` | T-A07 (sin prueba JVM), T-B06 |
| RF-APP-02 | Implementado (r1.1) | `Receptor.java:14-15,59`; `Cliente.java:68-69,147-200` | T-B06 |
| RF-APP-03 | Implementado | `Pruebas.java:240-301` (`detectar`) | README de la app (C-09); T-A08 completa |
| RF-APP-04 | Parcial | `Fabrica.java:32-60` (color, intensa/opaca); sin mapa tipo I-XI → código | Aplazado con PAR |
| RF-APP-05 | No (aplazado r1.1) | Sólo prueba 3: `Pruebas.java:401-433` | Fase de campo |
| RF-APP-06 | Parcial (r1.1) | `LecturaX.java:39-60`; `Medida` guarda la `x` y el método. Texto de `::0`: "saturado o negativo" (`LecturaX.java:52-55`) | Texto "saturación" con `e`; aviso de gatillo |
| RF-APP-07 | **Parcial** | Pruebas 4 y 5: `Coherencia.java`, `Pruebas.java:307-398`. Tolerancia 1 ulp: `Ecuacion.java:290-300`, `Pruebas.java:333`; `#E` esperado con lo leído: `Pruebas.java:379` | 2 ulp mientras no pase T-A23; `#E` contra fábrica |
| RF-APP-08 | Parcial | 48 coeficientes: `Fabrica.java:62-95` + `FabricaTest`. Los 3 de temperatura no están en `Fabrica` | Añadir los 3 y compararlos (T-A01) |
| RF-APP-09 | **Parcial** | `Repetibilidad.java:13-14`: 5 lecturas, SD ≤ 10 | Pasar a 10 lecturas |
| RF-APP-10 | **Parcial** | `Ecuacion.java:275-287` (float32, orden 2020); la inversión usa `double`/Horner: `Inversion.java:52`, `Ecuacion.java:245-267` | Invertir con float32 (C-21) |
| RF-APP-11 | Parcial | Bloqueante en `Asistente.validarForma` (`Asistente.java:118-147`); sin informe de las 12 curvas de fábrica | Informe informativo |
| RF-APP-12 | Parcial | `#V#` y `#GT#`: pruebas 2 y 5. Sin lectura de oscuro guiada; no se comprueba `@` en lo recibido (sólo en lo enviado, `Tramas.java:43-45`) | Oscuro; `@` en recepción (C-31) |
| RF-APP-13 | Parcial | `MedidaActivity.java:41-50` (N lecturas por patrón, 3 por defecto) | 10 + 3×5 recolocaciones, oscuro, hora y temperatura |
| RF-APP-14 | Implementado | `Asistente.java:26-56` (cobertura), `:190-196` (mezcla de tipos, P-06) | — |
| RF-APP-15 | Parcial | `Ajuste.java:41-60` (grado 1-2, grado + 2 distintas); `Asistente.java:99-147,186-189` (C2) | Con y sin oscuro; comparación con repetibilidad; no mezclar métodos |
| RF-APP-16 | Implementado | `Asistente.java:216-228` (R actual y desvío por patrón) | — |
| RF-APP-17 | Implementado (r1.1) | `AdminActivity.java:395-503` (confirmación, copia, `#S`, relectura, C3) | Tolerancia de C-12; T-A32 |
| RF-APP-18 | Implementado | `AdminActivity.java:505-520` | — |
| RF-APP-19 | Implementado (r1.1) | `AdminActivity.java:242-330` | T-A19 (sin prueba JVM) |
| RF-APP-20 | Implementado | `Registro.java`; PIN oculto: `Hex.ocultarPin` (prueba `elPinNoLlegaAlRegistro`) | — |
| RF-APP-21 | **No** | Sin acta | Antes de P8 |
| RF-APP-22 | Implementado | `Sesion.java:209-230`; copia antes de escribir: `AdminActivity.java:470-471,515` | — |
| RF-APP-23 | Implementado | `Tramas.java:228-244` sin uso en la interfaz | — |
| RF-APP-24 | Implementado | `BotonesActivity.java:81-164`; `Tramas.java:246-297` | T-C33 |
| RF-APP-25 | Parcial | `LineaBase.java` (12 códigos, oscuro, `9`) | `9` sólo 3 veces (`LineaBase.java:27`); etiqueta "T-B03" donde es T-B02 (C-18) |
| RF-APP-26 | Parcial | Por construcción: las tramas largas sólo se envían en V3.6; `Cliente.pedir` no comprueba longitud (`Cliente.java:147-160`) | Comprobación explícita; T-A34 |
| RF-APP-27 | Parcial | `Asistente.puntos` (`Asistente.java:74-97`) no filtra por método; tolerancias fijas: `Coherencia.java:22`, `Repetibilidad.java:14` | Filtro por método; tolerancias ≥ max(suelo, 2·s) |

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

---

## 10. Preguntas abiertas para el propietario

- **P-01 — O-01:** cómo se resuelve `#S` (subir el límite, acortar el formato o partir la trama).
  Hasta entonces el asistente de calibración no puede escribir.
- **P-02 — Defectos de 2020:** ¿se conservan (doble ecuación con gatillo, `nivel[2]`, 0x09 sin rama) o
  se corrige alguno aceptando que cambia el comportamiento?
- **P-03 — Correo:** ¿se replica el envío SMTP con credenciales en el código, o se entrega el CSV por
  el menú de compartir de Android?
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
- **P-10 — Conversión exacta (C1):** ¿se añade al firmware antes del commit de G1 (una sola grabación)
  o después (dos grabaciones y T-A20 dos veces)?
- **P-11 — Semántica de `CAL` (O-12):** ¿se cambia el contrato a la del código?
