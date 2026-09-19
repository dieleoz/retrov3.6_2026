# HISTORIA — cómo se llegó a la V3.6

## Las generaciones del Vertical (archivo `old\VERTICAL\`)

| Familia | Micro / compilador | Años | Calibración |
| :--- | :--- | :--- | :--- |
| V1-V3 "tarjeta 3" | PIC18F4550 / CCS C | 2012-2018 | Rectas por color en el código + potenciómetro; modo "CALIBRACION" al arrancar (2018) |
| **SAT-LUX/V3 K42** | **PIC18F47K42 / XC8 2.10** | **2020** | **Polinomios en `ecuacionesCalibracion.c`**; placa del Horizontal IoT ("SATLUX H-IoT"); STONE |
| v4.0 | PIC18F47K42 / XC8 2.31 | 2021 | Rectas en `Aplicacion.c`; protocolo `@LEERV` |
| V4.1 | PIC18F47K42 / XC8 3.10 | 2026 | `Ecuaciones.c`; repositorio `P_RetroReflectometro_Vertical` |

"V3" se usa en las dos primeras familias: **SAT-LUX/V3 es un nombre comercial**, no identifica la placa.

## 18-sep-2026 — SLV-002 con el equipo delante

- **Identificación:** PIC18F47K42 rev A001, STONE STA035WT-01, PCB "SATLUX H-IoT". Las pantallas
  coinciden con los diseños STONE de 2020.
- **Barrido de 255 bytes** con la app RTV Diag BT: responden `1`-`8` y `a`-`d` con `::n` y `9` con `:n:` (batería), pero **no
  `e`**. Las 12 respuestas, invertidas, dan la misma lectura interna: **las fórmulas del equipo son
  las del fuente de 2020**. Su firmware es una variante **sin el comando `e`**, que no está en el
  archivo: los dos `.hex` de 2020 sí lo tienen.
- **Lectura ICSP** (PICkit 3, tras recuperarlo del modo scripting): **chip protegido**, sin copia
  posible. La configuración leída coincide con la del fuente.
- **Blancos medidos:** XI entre +2 y +3 %, IV y IX entre +28 y +50 %. Techo de 806 en la fórmula del
  blanco. Detalle en `06_Calibracion/`.
- **Decisión:** crear la V3.6, con la calibración en EEPROM y ajustable desde la app.

## Errores que se cometieron y no deben repetirse

| Se concluyó | Era |
| :--- | :--- |
| "Es un 18F4550": sus textos de pantalla están en el fuente CCS | En STONE los textos son **imágenes**. Es K42 |
| "El firmware 2020 tiene modo de calibración negro/blanco" | `calibrate()` **no se llama nunca** |
| "El Bluetooth va a otra velocidad", porque `e` no contestaba | Va a 9600; **no existe el comando `e`** en ese equipo |

Lección común: **no concluir de un indicio sin cruzarlo con una segunda prueba.**
