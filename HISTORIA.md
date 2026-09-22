# HISTORIA — cómo se llegó a la V3.6
Mapa: `ARQUITECTURA.map` §M1 (equipo visto) y §M7 (contradicciones abiertas y errores corregidos).
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

Se deja la errónea junto a la buena, para que no vuelva a circular. Lección común: **no concluir de un
indicio sin cruzarlo con una segunda prueba.**

| Se concluyó | Era | Cómo se cerró |
| :--- | :--- | :--- |
| "Es un 18F4550": sus textos de pantalla están en el fuente CCS | En STONE los textos son **imágenes**. Es K42 | Equipo delante, 18-sep |
| "El firmware 2020 tiene modo de calibración negro/blanco" | `calibrate()` **no se llama nunca** | Lectura del fuente |
| "El Bluetooth va a otra velocidad", porque `e` no contestaba | Va a 9600; **no existe el comando `e`** en ese equipo | Barrido de 255 bytes |
| Serie `SLV-02` en EEPROM | `#GN` = **SLV-002** | ZIP de las 12:27 (`afdd700`) |
| Códigos 8 y 2 escritos | **1 y 2 escritos; el 8 no** | Mismo ZIP |
| `CAL,0003` "sale del fuente, no de una medida" | `#V#` = CAL 0003 **visto en campo** | ZIP de las 15:10 (`8c8c7de`) |
| El segundo equipo es un V3 y recibe la V3.6 | Es un **V4** (PPS de la V4.1): va a la V4.6 | `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md` |
| Serie `SLV-002` compartida con Nordeste | Coviandina `SLV-002-2026`; Nordeste `SLV-003-2026` | Decisiones SERIE y SERIE-2 |
| Café y lila "sin código en el firmware" | Se miden con el **código del rojo** | Decisión de Diego |
| B-01 de la P15 "cerrado" en la 3.6.17 | **Abierto por otros donantes**; ocho Altos más | `05_Documentacion/REVISION-Arquitectura-P16-V3.6.md:3` |
| "Los tests en verde avalan la entrega" | En `f5145ed`, **25 de 29 `Ruptura*` no aseveraban nada** | Recuento de `assert`/`fail` |
| Este repositorio "tiene `CLAUDE.md`" | No lo tenía hasta el 19-sep, noche | `git ls-files`, `find` y `ls` |
| El HONOR de Coviandina lleva la 3.6.17 | Lleva la **rc3** (10002) | Cabecera del registro de las 19:12 (`06_Calibracion/SLV-002/tramas/`), 21-sep |
| El banco de las 18:11 se midió en el HONOR | Se midió en el **Samsung** | Cabeceras de las tramas del ZIP de las 18:11 |
| Pruebas del equipo del HONOR "NO APTO" | Primera tanda NO APTO, **segunda APTO** | Mismo registro, l.586 y l.1170 |
| La recta anclada del 8 vista en el HONOR es la del flujo | Es la **vista previa de Avanzado** | `AdminActivity.java:522-535` en `fd37cc7` |
| La rc3 "cambió el formato" y no ve la campaña de la mañana | Misma ruta en 3.6.9, rc3 y rc6: **se borraron los datos** (hipótesis) | `Campanas.java:66,82` |
| "La 3.6.17 no tiene el candado 5 × 4" | **Sí lo tiene** | `FlujoCalibracion.java:699-721` en `f5145ed` |
| El ZIP que sostiene la curva "está en el repositorio" | Estaba sólo en `07 pruebas/` hasta el 21-sep | `git ls-files` |
| `#K#` "especificado, no implementado" (skill STONE) | **Implementado** | `calibracion_v36.c:797` |
| `targetSdk` "urgente" (encargo) o "sólo si se publica" | **No se publica**: no obliga | APPS-DPI |

## Pruebas que no demostraban nada (19-sep, noche)

Una prueba que asevera no demuestra nada si el valor esperado salió del propio código:

1. El acta decía `firmado por Firmado por "Ana Ruiz", …` y **la prueba aseveraba esa cadena**, copiada
   de la salida. No podía fallar nunca.
2. El simulador respondía `#T,25.0,25.0#`, **simétrico**, mientras el firmware manda el circuito
   primero. La app leía el campo equivocado y ninguna prueba podía verlo.
3. Una prueba de la firma construía los literales del acta y aseveraba sobre ellos: comprobaba la idea
   del autor, no el código.

## Contradicción abierta: modelo de pantalla

Este fichero (l.16) dice **STA035WT-01** (el `ARQUITECTURA.map` antiguo y el `README.md` lo decían hasta el 21-sep); `04_Pantalla_STONE/SOFTWARE-STONE.md:14`
y `01_Firmware/lecturas_equipos/V3-2/IDENTIFICACION.md:8` dicen **STVA035WT(-01)**. Se mira la etiqueta.

## 22-sep-2026: el 8 y la b, seis intentos en campo

El funcional de ITVIAL escribió los códigos 8 y b de SLV-002 con `Cov_3.6.5_calibrar` a la sexta
sesión. Los cinco primeros intentos no escribieron nada (`06_Calibracion/SLV-002/campanas/HUELLAS.txt`,
capa cruda en `07 pruebas/2209*`):

| Hora | Qué paró la escritura |
| :--- | :--- |
| 10:42 | Cuatro `#L` con `ERR,PIN`: el operador no tenía el PIN. El firmware se bloquea al quinto (`calibracion_v36.c:645-656`) |
| 10:49 y 10:56 | T-C41: pulsó OK antes de apagar. El equipo aún contestó `#Q#` → `#OK#` con el diálogo abierto |
| 11:41 y 13:35 | `#S,8` → `#ERR,FORMATO#` en 139 y 127 ms; la app restauró con `#F,8#` |
| 14:32 | **Entró**: la misma trama → `#OK#` en 333 ms. `#SC,2026-09-22#`, `#S,b` → `#OK#`; máscara `0283` |

**Lo que se concluyó mal, tres veces, antes de acertar:**

- *"Android tarda 8-12 s en ver caer el enlace y por eso falló T-C41."* **Falso:** a las 10:55 el equipo
  respondió `#Q#` → `#OK#` **después** del OK del operador (`rtv36_20260922_105411` l. 579-582), luego
  estaba encendido. Lo cierto: pulsó OK antes de apagar.
- *"El `ERR,FORMATO` es la notación `E+00`, el signo o la longitud."* **Falso:** los códigos 1 y 2 se
  escribieron el 19-sep con tramas de 66 bytes de la misma forma (`rtv36_20260919_114644` l. 1660-1663).
- *"Es el desbordamiento del anillo RX de 64 bytes, demostrado."* **No demostrado:** la misma trama de 66
  bytes entró a las 14:32 en una sola ráfaga. Es transitorio y **la causa sigue sin identificar**; el
  anillo (`uart_module.c:57`) es hipótesis. Quedó escrito como hecho en `RETOMAR.md` y se corrigió
  (`83217e1`).

**Y lo que sí quedó medido:** el equipo repite (9 patrones, ±2 cuentas entre el 19 y el 22-sep) y la
correlación entre certificado y lectura es +0,98 en amarillo y +0,96 en rojo, pero **negativa** en azul
y verde dentro del rango de trabajo (−0,68 y −0,42): P18 (XI, cert 84) lee 36 y P112 (XI, cert 101) lee
25. Contradicción C-08 del `ROADMAP.md`, abierta.
