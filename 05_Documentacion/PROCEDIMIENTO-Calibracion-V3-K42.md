# Procedimiento de calibración — Vertical "V3" con PIC18F47K42 (firmware de 2020)

**Nada de este procedimiento se ha ejecutado contra un equipo.** Sale de leer el fuente, línea por
línea, el 18-sep-2026. Antes de dar por buena cualquier cifra hay que verla con el equipo delante.

**Revisión adversaria hecha el mismo día.** Un segundo agente comprobó cada cita contra el fuente.
Confirmó las dos afirmaciones en las que se apoya el procedimiento (`calibrate()` no se ejecuta
nunca; `e` devuelve la `x`). Desmintió seis puntos, que están corregidos dejando la versión errónea
escrita al lado: una cita del naranja opaco, el caso del gatillo pulsado, el "negativo sale como 0",
el umbral de saturación, el número de muestras y la vía STONE.

**A qué equipo aplica.** Al Vertical con PIC18F47K42 que lleva el firmware `RetroVertical1.X`
(fuentes hasta el 20-ago-2020). **No** aplica al V2/V3 con 18F4550 (CCS, 2012-2018), que tiene su
propio procedimiento en `old\...\CALIBRACION RETROS VERTICALES V2 Y V3\...\Como calibrar.txt`, ni al
V4.0/V4.1 de este repositorio.

**Fuente citado.** Todas las referencias `archivo:línea` son de
`D:\@Proyect\IT\old\VERTICAL\2_V3.3_18F47K42_2020\RETRO VERTICAL__E_Electronica_proyectos\1_RetroVertical_2020(modificada)\RetroVertical1.X\`.
La otra copia (`RETRO VERTICAL\...`) tiene `gui.c`, `measurement.c`, `ecuacionesCalibracion.c`,
`main.c` y `uart_module.c` idénticos byte a byte. **El fuente no está en este repositorio.**

---

## 1. Lo que hay que saber antes de empezar

### 1.1 La calibración está en el código, y sólo en el código

- **Doce ecuaciones, una por color y tipo, escritas como polinomios** de grado 2 y 3 en
  `ecuacionesCalibracion.c:3-42`. Tres parejas son idénticas: el naranja (`:9-11` y `:34-36`), el
  azul (`:15-17` y `:37-39`) y el verde (`:18-20` y `:40-42`) usan la misma ecuación para intenso y
  para opaco.
  (Corrección de la verificación: el naranja opaco está en `:31-33`, no en `:34-36`, que es el
  rojo opaco.)
- **Un factor de temperatura** multiplica la lectura antes de la ecuación:
  `valor = ADC × (X_2·T² + X_1·T + X_0)` (`gui.c:300`), con `X_2 = 0`, `X_1 = 0,00043212` y
  `X_0 = 0,90148651` (`gui.c:41-43`).
- **La EEPROM no guarda calibración.** Guarda modelo, propietario, número de serie y geometría
  (`eeprom_manager.h:9-12`). **Tiene direcciones reservadas** para patrón negro/blanco, offsets y
  factor de batería (`:13-19`), pero nada las escribe: `writeFunction15Params` y
  `writeFunction30Params` (`eeprom_manager.c:150-168`) no se llaman desde ningún sitio.
- **Consecuencia: dejar el equipo calibrado exige editar `ecuacionesCalibracion.c`, recompilar con
  XC8 y grabar por ICSP.** No hay otra vía.

### 1.2 No hay modo de calibración en el equipo — corrección

Se afirmó el 18-sep-2026, en esta misma sesión y también en el informe del subagente, que el equipo
tiene un modo que captura un patrón negro y uno blanco desde el menú. **Es falso en la práctica:**

| Lo que se dijo | Lo que dice el código |
| :--- | :--- |
| Hay un modo de calibración negro/blanco desde el menú | `calibrate()` (`gui.c:94-148`) **no se llama desde ningún sitio**. Tampoco `guiWelcome`, `guiOwner`, `guiSerial`, `chooseUserGeometry` ni `checkTemperatureDiff`. El bucle real es `main.c:30-36` → `taskGui` (`gui.c:493-523`) → `measure` (`gui.c:260-363`) |
| Aunque volátil, la normalización negro/blanco se aplica | `acquireReflectivity15Value` **se ejecuta en cada medida** (`gui.c:297`) y calcula el valor normalizado en `reflectivity15Value` (`measurement.c:275-286`), pero **nadie lo usa**: `gui.c:298` toma `getReflectivity15Adc()`, el ADC sin normalizar. Con `adcBlack = adcWhite = 0`, la normalización divide por cero en coma flotante en cada medida (`:275-276`), sin efecto sobre la `x` |
| La pantalla "HORIZONTAL V3.3" identifica este firmware | Ese texto sólo está en `guiWelcome` (`gui.c:158,160`), que nunca se ejecuta. **No sirve para identificar el firmware** |

### 1.3 Qué es exactamente la lectura

`measurement.c:208-250`:

1. Enciende la luz (`SALIDA15`) y espera **400 ms** (`:222-226`).
2. Toma 16 muestras rápidas, descarta la primera y promedia las otras 15 (`:228-237`). Después
   aplica un filtro IIR con α = 0,995 sobre **599 muestras**, arrancando de ese promedio (`:239-245`).
   El promedio inicial sigue pesando un ~5 % en el resultado (0,995^599).
3. Apaga la luz y **suma 200** (`:246-247`). El propio comentario dice que es para evitar valores
   negativos y que "en la calibración se arregla".

Después, en `gui.c:298-300`, se multiplica por el factor de temperatura. **Ese número es la `x` de
los doce polinomios.**

El ADC es de 12 bits, pero **la conversión a 12 bits está hecha a mano**: MCC configura el resultado
justificado a la izquierda (`adcc.c:107-108`, `ADCON0 = 0x80`), y `adcc.c:159` lo recompone con
`(ADRESH << 4) + (ADRESL >> 4)`. **Si alguien regenera los drivers con MCC, esa línea se pierde**:
las lecturas salen multiplicadas por 16 y la suma de `measurement.c:235` desborda.

El reloj del ADC es FOSC/2 = 8 MHz (`adcc.c:105-106`, `ADCLK = 0x00`). **Sin cotejar con la hoja de
datos del K42:** podría estar por debajo del TAD mínimo, y eso afectaría a la exactitud de la `x`.

### 1.4 La vía para leer la `x` sin reprogramar: el carácter `e`

Por Bluetooth, el primer carácter recibido elige la ecuación (`ecuacionesCalibracion.c:137-198`):

| Carácter | Ecuación | Carácter | Ecuación |
| :---: | :--- | :---: | :--- |
| `1` | blanco intenso | `7` | blanco opaco |
| `2` | amarillo intenso | `8` | amarillo opaco |
| `3` | verde intenso | `a` | verde opaco |
| `4` | rojo intenso | `b` | rojo opaco |
| `5` | azul intenso | `c` | azul opaco |
| `6` | naranja intenso | `d` | naranja opaco |
| **`e`** | **ninguna: devuelve la `x`** (`:191-192`) | `9` | nivel de batería, `:<n>:` (`gui.c:328-340`) |

**Con `e` el equipo mide y devuelve la lectura compensada por temperatura, sin ecuación.** Es el
equivalente del "tipo 1" del V4.1 y es la base de todo este procedimiento.

**Siempre que el gatillo esté suelto.** Si está pulsado cuando llega la petición (`gui.c:294`),
primero se ejecuta `ecuacionesColoresGatillo` (`gui.c:311-315`), que aplica la ecuación del color
que quedó seleccionado en la pantalla. Ese color **no se borra nunca**, porque `borrarBuffer()` está
vacía (`uart_stone.c:32-35`). Después, la rama `e` envía ese valor **ya transformado**, no la `x`.
**Durante la sesión nadie toca el gatillo.**

En la pantalla STONE, el código `0x0e` sólo **selecciona** "sin ecuación"
(`ecuacionesCalibracion.c:128-130`); la medida la dispara el gatillo (`gui.c:311-313`) y el valor
sale en pantalla, no por Bluetooth.

### 1.4 bis Modo oculto "PRUEBA ADC" en la pantalla — la vía sin Bluetooth

Hallado el 18-sep-2026 en el proyecto STONE
`...\1_RetroVertical_2020(modificada)\lcd_RetroVertical\lcd_RetroVertical.vt`, cruzando cada botón
con su página, su destino y su código (el byte bajo del valor del botón es el `bufferPantalla[8]` de
`ecuacionesCalibracion.c:73-130`):

- **Cómo se entra:** menú OTROS PAPELES / PAPEL TIPO I (pág. 3) → **engranaje** arriba a la izquierda,
  zona (4,5)-(43,39) → pág. 17 (brillo) → **esquina superior derecha**, zona (290,0)-(320,14) → pág. 19
  (vacía) → **esquina superior izquierda**, zona (0,0)-(26,20): envía **0x0E** y abre la pág. 18.
  También se llega a la pág. 19 desde la pág. 1 (logo), zona (143,0)-(188,14).
- **Qué muestra:** la pág. 18 (`IMAGE\18_calibracion.jpg`) se titula **"PRUEBA ADC"** y dice: *"Este
  modo es exclusivamente para realizar Pruebas y calibrar el instrumento. El resultado obtenido será
  ADC/mV + 200 + ajuste offset/mV"*. Tiene las variables 199, 205, 210 y 215: tres medidas y su
  promedio. **Con el gatillo da la `x` de §1.3 sin ecuación**, la misma que `e` por Bluetooth.
- **Salir:** botón "regresar", zona (176,102)-(283,141) → pág. 3.
- La luz no depende del color elegido: sólo se enciende `SALIDA15` (`measurement.c:222`). La `x` de
  un patrón no cambia con el color seleccionado.

**A qué ecuación va cada botón en uso normal** (misma extracción):

| Menú | Código enviado | Ecuaciones |
| :--- | :--- | :--- |
| OTROS PAPELES | 0x01 a 0x06 | **intensas** (`ecuacionesCalibracion.c:3-20`) |
| PAPEL TIPO I | 0x07 a 0x0D | **opacas** (`:22-42`) |

Al pulsar "OTROS PAPELES" se envía 0x02 y se entra en AMARILLO. Cada toque en la barra de color envía
el código del color siguiente: amarillo → blanco (0x01) → rojo (0x04) → azul (0x05) → verde (0x03) →
naranja (0x06) → amarillo. **Resuelve la contradicción de §3** sobre intenso/opaco: los patrones IV,
IX y XI se miden en OTROS PAPELES y se ajustan contra las ecuaciones intensas.

**La respuesta a `9` (batería) está rota.** Parte de un `batteryVoltage` ya reescalado en la vuelta
anterior (`gui.c:349-350`), y `nivel[2]` desborda con valores de dos cifras (`gui.c:267,335`). No se
usa en este procedimiento.

### 1.5 El enlace

- **9600 baudios.** Fosc 16 MHz (`mcc.c:74`, `OSCFRQ = 0x05`; `device_config.h:50`),
  `U1CON0 = 0xB0` (BRGS = 1) y `U1BRG = 0x01A0` = 416 (`uart1.c:115,124,127`):
  16 000 000 / (4 × 417) = 9592 baudios, un 0,08 % por debajo de 9600.
- **Respuesta: `::<n>`, sin terminador** (`ecuacionesCalibracion.c:56-67`). El cliente no puede
  esperar un fin de línea: tiene que dar la respuesta por terminada tras un silencio.
- **Cualquier byte recibido dispara una medición** (`gui.c:295`, `bufferIndex > 0`), aunque no
  corresponda a ningún color. En ese caso mide y no contesta.
- **Enviar un solo carácter por petición, sin `\r` ni `\n`.** El buffer de recepción es de 50 bytes
  y `readUartStr()` escribe sin comprobar el límite (`uart_module.c:51-57`). El vaciado por tiempo
  está desactivado (`uart_module.c:109`, comentado). El buffer sólo se vacía al terminar cada
  medición (`gui.c:342-343`).
- **Esperar la respuesta, y después al menos 1 s más, antes de enviar la siguiente petición.** Lo
  que llegue durante la medida, o durante el pitido de 80 ms que sigue a la respuesta
  (`ecuacionesCalibracion.c:64`), pasa al buffer y lo borra `clearBuffer()` (`gui.c:342`): **se pierde
  sin respuesta.** Después vienen 500 ms de espera fija (`gui.c:345-346`).
- **Al encender, esperar unos segundos antes de la primera petición.** Hay 2 s fijos antes de entrar
  en el bucle de medida (`gui.c:508-509`).

### 1.6 Tres trampas del valor devuelto

- **Un 0 puede ser saturación.** `arreglar_dato()` pone a 0 todo valor mayor que 4000
  (`ecuacionesCalibracion.c:49-54`). La condición real es `(ADC + 200) × F > 4000`, donde F es el
  factor de temperatura. Si T va en décimas de grado, como sugiere el comentario de `gui.c:503`
  ("20 grados serán 200 unidades"), a 25 °C F ≈ 1,01 y la saturación empieza hacia ADC ≈ 3760.
  **Esa escala está sin verificar**: si F < 0,931, la saturación es imposible. En la hoja de datos,
  un 0 se anota como "saturado o negativo", no como "sin señal".
- **Un resultado negativo previsiblemente sale como 0, pero no está garantizado.**
  `reflectivityValue` es `unsigned int` (`gui.c:30`). Convertir un double negativo a `unsigned int`
  es **comportamiento indefinido** en C (C99 6.3.1.4). Con la librería de XC8 lo previsible es que se
  convierta en un número enorme y que `arreglar_dato()` lo mande a 0, pero no se ha comprobado sobre
  el código generado. **Las curvas nuevas no deben dar negativo dentro del rango de uso.**
- **La temperatura "T" no se sabe qué es.** Se lee del canal `INREFLECT30`, filtrada y dividida por
  4,928 (`measurement.c:64-73`). El comentario dice que la señal de temperatura se amplifica y "se
  manda por inreflect". Qué sensor hay en ese canal en la placa, y en qué unidades sale, está **sin
  verificar**.

---

## 2. Antes de calibrar

1. **Confirmar que el equipo lleva este firmware.** Terminal Bluetooth desde el móvil, **sin fin de
   línea** (en la app, "newline: none"). **Los baudios de la app no influyen**: por Bluetooth SPP la
   velocidad sólo existe entre el PIC y el módulo, y la fija el módulo. *(Corrección del 18-sep-2026:
   la primera versión de este paso decía "probar 9600 y luego 115200", lo que sólo tiene sentido
   con un adaptador serie cableado a `X4`, no por Bluetooth.)*
   - Enviar **sólo** `e`. Si responde `::<número>`, es este firmware.
   - Si no responde en 3 s, enviar `@LEERV,BLA,1@`. Si responde `@LEERV,<n>@/n/r`, es v4.0 o V4.1:
     seguir `PROCEDIMIENTO-Identificar-Firmware.md`. Este documento no aplica.
   - Las dos tramas son inocuas en las tres versiones. `e` no lleva `@`. `@LEERV,...@` es válida en
     v4.x, y en el firmware de 2020 sólo dispara una medida sin respuesta, porque ningún código
     empieza por `@` (`ecuacionesCalibracion.c:141-193`). **No** enviar ninguna otra trama con `@`:
     en el V4.1, una trama con `@` y sin `LEERV` deja el equipo sin Bluetooth hasta apagarlo
     (`Serial.c:87-104` del repositorio).
2. **Leer y guardar lo que tiene grabado**, por ICSP, antes de tocar nada. Anotar el número de serie,
   la fecha y el hash del volcado. Compararlo con los dos `.hex` conocidos de este firmware (md5
   `d089f962…` y `43f9f74b…`, según el `INDICE.md` de `old\`); la diferencia entre ellos no está
   explicada.
3. **Confirmar que el equipo no es el patrón.** `CLAUDE.md` §3 prohíbe reprogramar el equipo patrón.
   Este procedimiento termina grabando firmware.
4. **Tener la cadena de compilación:** MPLAB X, XC8 (el proyecto se hizo con la 2.10) y un PICkit.

---

## 3. Patrones

- Hace falta, **por cada color y tipo**, al menos **5 niveles** que cubran el rango, más una lectura
  con la óptica tapada (oscuro).
- **Patrones con valor certificado, P1 a P31**, entregados el 18-sep-2026: 17 de tipo XI, 9 de
  tipo IV y 5 de tipo IX, en `06_Calibracion/patrones_certificados_P1-P31.csv`. La "t" del listado
  original es la etiqueta del tipo de lámina (I a XI, en romano), no un dato aparte. **Falta saber
  el color de cada uno y la geometría del certificado.** No son los
  testigos P2-P40 de 2018: tienen los mismos nombres y otros valores (P19 = 85 frente a 746,22).
  **Colores aportados por inspección el mismo día** (no certificados). Cobertura:

  | Color | Patrones | Rango | Sirve para |
  | :--- | ---: | :--- | :--- |
  | Amarillo | 14 (IV, IX, XI) | 334-782 | **Ajustar la curva** |
  | Blanco | 8 (IV, IX, XI) | 378-828 | **Ajustar la curva** |
  | Verde | 4 (XI) | 164-173 | Sólo verificar: un único nivel |
  | Azul | 3 (XI) | 83-85 | Sólo verificar: un único nivel |
  | Rojo | 2 (IV) | 194-227 | Sólo verificar: dos niveles muy próximos |
  | Naranja | 0 | — | **Nada** |

  Con un único nivel se mide el desvío en un punto, pero no la pendiente: **verde, azul y rojo no se
  pueden recalibrar con este juego**; sólo comprobar. Los valores caben todos por encima de los
  mínimos NTC 4739 a 0,2°/−4° (tablas 5, 9 y 10), lo que es coherente con esa geometría, pero **no la
  demuestra**.
- Existen también los testigos **P2 a P40** de 2018, con su valor esperado por color y tipo, en
  `old\VERTICAL\1_V2-V3_18F4550_CCS\CALIBRACION RETROS VERTICALES V2 Y V3\lista de calibracion retros.xlsx`.
  **Sus valores sirven; sus lecturas en cuentas no.** Las cuentas de ese archivo son del 18F4550,
  con ADC de 10 bits y otra placa.
- **Contradicción abierta:** los testigos están clasificados como "I." y "O." (ingeniería y otros
  papeles), y este firmware usa "intenso" y "opaco" (el código también llama "BAJO" al opaco,
  `ecuacionesCalibracion.c:103`). **No consta que las dos clasificaciones coincidan.** Hay que
  aclararlo antes de asignar un testigo a una ecuación.
- Sin certificado ni trazabilidad de los testigos, el resultado es un ajuste contra testigos, **no
  una calibración trazable**. Hay que declararlo así en el acta.

---

## 4. Captura de las lecturas, sin reprogramar

1. **Encender, esperar 10 minutos** a que se estabilice la temperatura de la óptica, y anotar la
   temperatura ambiente con un termómetro externo. **No tocar el gatillo en toda la sesión** (§1.4). **Toda la sesión en el mismo régimen térmico**:
   el factor de temperatura (§1.1) forma parte de la `x`.
2. **Oscuro:** dos lecturas con `e` y la óptica tapada, al principio y al final de cada bloque.
   Sirven para medir la deriva.
3. **Por cada testigo:**
   - 10 lecturas con `e` seguidas, sin mover el equipo. Miden la **repetibilidad**.
   - 3 recolocaciones del equipo, con 5 lecturas cada una. Miden el **efecto de colocar** el
     equipo.
4. **Repetir todo otro día**, arrancando el equipo en frío.
5. **Anotar cada lectura** con la hora, el testigo, la temperatura ambiente y el número de serie del
   equipo. Un 0 se anota como saturado o negativo (§1.6).

---

## 5. Ajuste de las ecuaciones

1. **Media y desviación típica** por testigo, en la `x` devuelta por `e`.
2. **Ajustar por color y tipo** `R = f(x)` con los valores esperados de los testigos:
   - Primero una recta. Sólo se pasa a grado 2 o 3 si los residuos lo justifican **y** hay al menos
     un punto más que coeficientes. El firmware actual usa grado 3 en varios colores
     (`ecuacionesCalibracion.c:3-42`); con 5 testigos, un grado 3 deja un único grado de libertad.
   - Ajustar con y sin el punto de oscuro. Si la pendiente cambia mucho, el oscuro no pertenece a
     la curva.
3. **Separar repetibilidad de no linealidad:** comparar el error puro (paso 3 de §4) con el residuo
   del ajuste. Si el residuo no supera claramente el error puro, **subir de grado no está
   justificado.**
4. **Comprobar el rango:** la curva no puede dar valores negativos dentro del rango de uso, porque
   saldrían como 0 (§1.6).
5. **Contrastar con las ecuaciones de hoy:** evaluar las doce actuales sobre las `x` medidas y
   anotar la desviación de cada testigo. Ese es el estado "como llegó" del equipo.

---

## 6. Llevar las ecuaciones al equipo (reprograma)

1. Copiar el proyecto `RetroVertical1.X` a una carpeta con el **número de serie del equipo y la
   fecha**. Nunca se edita el original de `old\`.
2. Sustituir los coeficientes en `ecuacionesCalibracion.c:3-42`, **uno a uno y sin tocar la
   estructura**. Si se decide cambiar el factor de temperatura, en `gui.c:41-43`. **No regenerar los
   drivers con MCC**: se perdería la conversión a mano de `adcc.c:159` (§1.3).
3. Compilar con XC8. Guardar el `.hex` resultante junto con su md5.
4. Grabar por ICSP.
5. **Verificar:** repetir el paso 3 de §4 con el código de cada color y tipo (no con `e`), y anotar
   la desviación frente al valor esperado de cada testigo. Ese es el estado "como sale" del equipo.
6. Archivar en la carpeta del equipo: el volcado original, el fuente modificado, el `.hex` nuevo, la
   hoja de lecturas y el acta con el antes y el después.

---

## 7. Lo que falta y bloquea

| Qué falta | Por qué bloquea |
| :--- | :--- |
| Confirmar que el equipo lleva este firmware (§2, paso 1) | Si lleva otro, nada de este documento aplica |
| La correspondencia entre "intenso/opaco" y los tipos de lámina de los testigos | Sin ella no se sabe contra qué testigo se ajusta cada ecuación |
| Qué sensor mide `T` y en qué unidades (§1.6) | Sin eso no se puede validar ni ajustar el factor de temperatura |
| Certificado y trazabilidad de los testigos P2-P40 | Sin ellos el resultado es un ajuste, no una calibración |
| Si la placa tiene ajustes de ganancia o de intensidad de LED | No verificado en el diseño `PCB (Proteus) 3.3`. Si los tiene, pueden resolver la saturación sin cambiar las ecuaciones |
| Por qué hay dos `.hex` distintos del mismo fuente | Podría haber una variante del firmware que no es la que aquí se describe |
| Si el reloj del ADC a 8 MHz cumple el TAD mínimo del K42 (§1.3) | Si no lo cumple, la `x` pierde exactitud y ninguna curva lo arregla |
| La escala de T: décimas de grado, según un comentario (§1.6) | Decide si el equipo puede saturar |
