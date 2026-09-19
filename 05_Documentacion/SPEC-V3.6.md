# SPEC-V3.6 — Especificación del firmware y la app V3.6 del Retrorreflectómetro Vertical

**Estado: especificación en papel, 18-sep-2026. Nada de lo que aquí se pide está implementado,
compilado, grabado ni medido.** Sale de leer el fuente de 2020, el protocolo y la app de campo. Cada
criterio de aceptación es una prueba pendiente, no un resultado.

**Qué manda sobre qué.** `PROTOCOLO-V3.6.md` es el contrato y **esta SPEC no lo cambia**: donde el
protocolo tiene un defecto, se anota como *observación al protocolo* (§7) y el requisito afectado
queda marcado como bloqueado hasta que el propietario del protocolo decida. Si esta SPEC y el
protocolo se contradicen, manda el protocolo.

**Fuente citado.** Salvo que se diga otra cosa, `archivo:línea` se refiere a
`01_Firmware/base_2020_d089f962/RetroVertical1.X/` (fuente de 2020 sin tocar; su `.hex` tiene md5
`d089f9625090a1213291c090eae7ac01`, comprobado el 18-sep-2026). **No se ha leído
`RetroVertical_V3.6.X`**, que está en obras. Las referencias a la app de campo son de
`D:\@Proyect\IT\P_RetroReflectometro_Vertical\03_App_Movil\RetroVerticalP1\app\src\main\java\com\example\retrohorizontalp1\`,
y las de su especificación, de `...\05_Documentacion\SPEC-06-App-Movil-Funcional.md` (en adelante
SPEC-06).

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
| Tras cada medida: se borra el buffer y se esperan 500 ms fijos. Lo recibido durante la medida **se pierde** | `gui.c:342-346` |
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

**RF-FW-05 — Valores de fábrica en ROM.** Los 12 × 4 coeficientes y los 3 del factor de temperatura
de fábrica se compilan **como literales con el mismo texto** que en `ecuacionesCalibracion.c:3-42` y
`gui.c:41-43` (así el compilador los redondea igual). `#F` los restaura desde ahí, nunca desde un
texto recibido. CA: revisión de código + T-A20 + T-C04.

**RF-FW-06 — Negativos y saturación.** El resultado negativo y el mayor de 4000 se comportan
**exactamente como en el código compilado de 2020**. Como el negativo es indefinido en C, el
comportamiento de referencia es el que midan T-A21 (simulador sobre el `.hex` de 2020) y T-B07 (SLV-002);
la V3.6 lo reproduce **de forma explícita** (sin depender de un comportamiento indefinido). CA: T-A21,
T-C07.

**RF-FW-07 — `e`.** Mide y responde `::<x>`: la `x` de RF-FW-02 tras `arreglar_dato()`, sin
ecuación (`ecuacionesCalibracion.c:191-193`). **Se conserva aunque SLV-002 no lo tenga.** CA: T-C06.

**RF-FW-08 — `9`.** Responde `:<n>:` con el mismo cálculo que `gui.c:328-340`. El desbordamiento de
`nivel[2]` (`gui.c:267`) se puede corregir **sólo si no cambia ningún byte emitido** para ninguno de
los valores posibles. Decisión pendiente (P-02). CA: T-C12.

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

### 2.2 Órdenes nuevas (`PROTOCOLO-V3.6.md` §3)

**RF-FW-13 — Tramas `#...#`.** Un `#` abre trama. Mientras hay una trama abierta, **ningún byte
dispara medida** (tampoco los `1`-`8`, `a`-`e` que vayan dentro, como en `#G,1#`). Trama de más de 48
bytes: se descarta. Trama sin cierre: se descarta a los 2 s. CA: T-C16, T-C17.

**RF-FW-14 — Límite de recepción.** Ninguna secuencia de bytes escribe fuera del buffer de
recepción (hoy `uart_module.c:51-57` lo permite). CA: T-A22 (simulador) y T-C18.

**RF-FW-15 — Trama recibida durante una medida.** El protocolo no lo dice (observación O-05). La
V3.6 **no mezcla** una trama con la medida en curso: o la atiende al acabar, o la descarta entera;
nunca la interpreta como bytes sueltos. CA: T-C20.

**RF-FW-16 — `#V#`.** Responde `#V,3.6,<AAAA-MM-DD>,<CAL|DEF>#` sin medir. CA: T-C03.

**RF-FW-17 — Modo administrador.** `#L,<pin>#` con el PIN correcto abre la sesión; incorrecto
responde `#ERR,PIN#`. La sesión se cierra con `#Q#` o al apagar; **arranca siempre cerrada**. CA:
T-C21, T-C22.

**RF-FW-18 — Lectura.** `#G,<k>#` y `#GT#` responden con los valores en RAM, en el formato numérico
del protocolo, sin sesión de administrador. CA: T-C04.

**RF-FW-19 — Escritura.** `#S` y `#ST` sólo con sesión abierta (si no, `#ERR,BLOQUEADO#`). Rechazan
con `#ERR,FORMATO#` un número mal formado, NaN o infinito, y un `k` fuera de `1`-`8`, `a`-`d`.
Escriben RAM y EEPROM, **releen la EEPROM** y sólo entonces responden `#OK#`; si la relectura no
cuadra, `#ERR,EEPROM#` y la RAM queda con el valor anterior. CA: T-C23, T-C24. **`#S` está bloqueado
por O-01** (la trama no cabe en 48 bytes).

**RF-FW-20 — Restaurar.** `#F,<k>#` y `#F,*#` restauran desde ROM (RF-FW-05). CA: T-C25.

**RF-FW-21 — PIN.** `#P,<actual>,<nuevo>#` con sesión abierta; el nuevo son 4 dígitos ASCII. PIN de
fábrica `2026`. CA: T-C26.

**RF-FW-22 — Formato numérico.** Emite notación científica con 7 cifras significativas y punto
decimal (`-8.654100E-05`); acepta además decimal simple (`0.6196681`). CA: T-A23 (simulador), T-C04.

**RF-FW-23 — Escritura de EEPROM y comunicaciones.** Como cada byte escrito deja las interrupciones
apagadas (`memory.c:181-188`), la respuesta `#OK#` sólo sale **después** de terminar todas las
escrituras. El tiempo total de un `#S` se mide y se anota (T-C23); la app no envía nada hasta recibir
la respuesta.

### 2.3 EEPROM, seguridad y construcción

**RF-FW-24 — Bloque en EEPROM.** Desde 0x100, con marca `V36`, versión, 12 × 4 `float`, 3 `float` de
temperatura, PIN y CRC-16. No escribe por debajo de 0x100 (las direcciones de 2020 llegan hasta 70,
`eeprom_manager.h:9-23`). Si la marca o el CRC no cuadran, carga fábrica y `#V#` da `DEF`. CA: T-A24
(simulador: bloque corrupto → `DEF`), T-C03.

**RF-FW-25 — Nunca `@`.** Ninguna respuesta contiene `@`. CA: T-C27 (registro de todos los bytes
emitidos en la sesión de pruebas).

**RF-FW-26 — Configuración idéntica.** Bits de configuración iguales a `device_config.c` de 2020,
incluida `CP = ON` (`device_config.c:97`), y leídos en SLV-002 como `EC FF F7 FF 9F FF FF DF FE FF`.
CA: T-C01.

**RF-FW-27 — Cadena de compilación.** XC8 **2.10** y el proyecto MPLAB X de la base; `double` y
`float` de 32 bits (`configurations.xml:182-185`). Cada `.hex` entregado lleva su md5 y el del
fuente. CA: puerta P3 del `ROADMAP.md` y T-A25.

---

## 3. Requisitos de la app V3.6 (RF-APP)

### 3.1 Enlace y detección de versión

**RF-APP-01 — Enlace.** SPP (UUID `00001101-0000-1000-8000-00805F9B34FB`), como la app de campo
(`medicion.java:89,1661`). Una petición cada vez; la siguiente sólo tras la respuesta **y** al menos
1 s más (lo recibido durante la medida se pierde, `gui.c:342-346`). CA: T-A07.

**RF-APP-02 — Fin de respuesta.** `::<n>` y `:<n>:` no llevan terminador: la respuesta se da por
terminada tras un silencio (valor inicial 300 ms, a fijar en T-B06) o, en `#...#`, en el `#` de
cierre. Tiempo máximo de espera por medida: a fijar en T-B06 (valor inicial 5 s). **Nunca** se espera
`\r\n`. CA: T-A03.

**RF-APP-03 — Detección de versión.** Al conectar, en este orden, con la espera de RF-APP-01 entre
pasos:

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

**RF-APP-04 — Mapa color × tipo → código.** El de §1: tipo de lámina I → opaco (`7`, `8`, `a`-`d`);
tipos II a XI → intenso (`1`-`6`). CA: T-A09.

### 3.2 Medida de campo

**RF-APP-05 — Medir.** Envía el código de un byte de RF-APP-04, muestra el entero recibido. Un `0` se
muestra como "0 (saturado o negativo)", no como "sin señal" (`ecuacionesCalibracion.c:49-54`). CA:
T-B03, T-C05.

**RF-APP-06 — Registro de la `x`.** En V3.6 y en V3 2020 con `e`, la app puede registrar además la
`x` con `e` (modo técnico, no por defecto en campo, porque duplica el tiempo de medida). CA: T-C06.

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

**RF-APP-08 — Tabla de fábrica.** La app lleva los 12 × 4 coeficientes y los 3 de temperatura de
2020, copiados **del texto** de `ecuacionesCalibracion.c:3-42` y `gui.c:41-43`. Una prueba JVM los
compara contra ese archivo (T-A01) para que no se desvíen.

**RF-APP-09 — Repetibilidad.** 10 lecturas seguidas sobre un patrón sin mover el equipo, con `e` (o con
`6` e inversión si no hay `e`). Calcula media, desviación típica y coeficiente de variación. Umbral
de APTO: **a fijar con los datos de SLV-002** (T-B04); hasta entonces se muestra el valor sin
veredicto (P-04). CA: T-A11, T-B04.

**RF-APP-10 — Emulación de la aritmética del equipo.** La app evalúa las ecuaciones en `float` de 32
bits, en el orden de RF-FW-04, con truncado a entero y corte en 4000. La inversión de una respuesta
`n` devuelve **el conjunto de `x` enteras** con `f(x) = n`, no una raíz real. Con `6` (naranja
intenso, creciente) el conjunto es un intervalo de unas pocas cuentas; por debajo de x ≈ 475 no hay
inversión (resultado negativo). Riesgo: que la librería de coma flotante de XC8 no redondee como
IEEE-754 (R-05). CA: T-A04, T-A05, T-A26.

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

### 3.5 Asistente de calibración (sólo V3.6, sólo administrador)

**RF-APP-15 — Ajuste.** Por código: mínimos cuadrados de `R = f(x)` sobre las medias por patrón, de
**grado 1 o 2** (el 3 no se ofrece). Exige al menos grado + 2 niveles distintos. Muestra
coeficientes, residuo por patrón (absoluto y %), error cuadrático medio y comparación con el error
puro de repetibilidad; ofrece el ajuste con y sin el punto de oscuro. Rechaza el juego si la curva no
es creciente y no negativa en el rango de `x` medido, o si pasa de 4000 en él. CA: T-A13, T-A14.

**RF-APP-16 — Estado "como llegó".** Evalúa las ecuaciones actuales (leídas con `#G`) sobre las `x`
medidas y anota la desviación de cada patrón. CA: T-A15.

**RF-APP-17 — Confirmación antes de escribir.** Pantalla con el código, los coeficientes actuales y
los nuevos, las `R` que darían los dos juegos sobre los patrones, y botón de confirmación explícita.
Después: `#S`, `#G` de relectura, comparación (≤ 1·10⁻⁶ relativo), y nueva medida de al menos un
patrón con el código. Guarda antes una copia de `#G` de los 12 códigos y de `#GT#`. **Bloqueado por
O-01** hasta que `#S` quepa en el límite de trama. CA: T-C23, T-C28.

**RF-APP-18 — Volver a fábrica.** Por código o todos, con `#F` (nunca reescribiendo los valores de
fábrica con `#S`, RF-FW-05). CA: T-C25.

### 3.6 Administrador

**RF-APP-19 — PIN.** El modo administrador se abre con el PIN del equipo (`#L`); la app no guarda el
PIN. Ofrece cambio de PIN (`#P`) y cierra la sesión (`#Q#`) al salir del modo, al desconectar y al
cerrar la app. Las funciones de escritura no se muestran fuera de ese modo. CA: T-C21, T-C26.

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

**Pregunta abierta, sin respuesta inventada.** El cliente de SLV-002 usa **una app propia que no
tenemos**. Por el firmware se deduce que hablará un byte por petición y leerá `::<n>`, pero no
sabemos qué códigos envía, qué espera tras la respuesta, si usa `9`, ni si envía algo que empiece por
`#`. Es la app que "tiene que seguir funcionando sin cambios" (`PROTOCOLO-V3.6.md` §1) y no hay forma
de probarla sin ella (T-C30).

---

## 5. Plan de pruebas

Toda prueba deja registro: fecha, equipo, versión detectada, tramas en crudo y resultado.

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

---

## 8. Fuera de alcance

- **Equipos V4** (v4.0, V4.1): irán a una V4.6 en el repositorio `P_RetroReflectometro_Vertical`.
  La app V3.6 los detecta (RF-APP-03) y no opera con ellos.
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
