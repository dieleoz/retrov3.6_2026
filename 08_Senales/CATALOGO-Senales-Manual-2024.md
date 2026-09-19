# Catálogo de señales verticales — Manual de Señalización Vial 2024

**Nada de este catálogo está medido.** Sale de leer el PDF del Manual y de cruzarlo con la
transcripción de la NTC 4739. Los colores, formas y láminas son lo que el Manual exige, no lo que
hay instalado en la vía, y ningún color de lámina se ha comprobado con el equipo.

Encargo: identificar qué señal se mide, para la futura app de producción del retrorreflectómetro.
Entregables de este directorio:

| Archivo | Qué es |
| :--- | :--- |
| `CATALOGO-Senales-Manual-2024.md` | Este documento |
| `senales.csv` | Una fila por señal: `codigo, nombre, familia, forma, color_fondo, color_orla, color_simbolo, lamina_minima, pagina, icono` |
| `iconos/<codigo>.svg` | 376 iconos propios, 64×64 de viewBox |
| `tools/generar_iconos.py` | Regenera los iconos y `indice.html` desde el CSV |
| `indice.html` | Rejilla estática con todos los iconos, código, nombre y página |

---

## 1. Fuente y método

| Campo | Valor |
| :--- | :--- |
| Documento | *Manual de Señalización Vial. Dispositivos uniformes en la infraestructura vial para la regulación del tránsito y la seguridad vial*, Ministerio de Transporte / ANSV |
| Edición | Segunda edición, 2024, versión ajustada con fe de erratas, Resolución 20253040037925, octubre de 2025 (p. 1 y p. 3) |
| Archivo leído | `D:\@Proyect\IT\P_RetroReflectometro_Vertical\04_Manuales\Manual_de_Senalizacion_Vial.pdf`, 1.426 páginas |
| Paginación | Se cita la página del PDF; coincide con la impresa |
| NTC 4739 | Solo a través de la transcripción propia `04_Manuales/NTC-4739-Requisitos.md` del repositorio V5 (NTC 4739:2011, segunda actualización, IDT de ASTM D4956-11a). Se cita la **página impresa de la norma** |

**Ubicación del PDF.** El encargo lo situaba en `D:\IT\P_RetroReflectometro_Vertical\04_Manuales\`.
Ahí **no está**: comprobado con un listado del directorio y con una búsqueda por nombre (`Glob
**/Manual_de_Senalizacion*.pdf`), las dos vacías. Está en la carpeta hermana `D:\@Proyect\IT\...`,
que sí tiene las transcripciones y el PDF. Las transcripciones `.md` de las dos carpetas son las mismas
(mismo nombre y tamaño de `Manual-Senalizacion-Vial-Extracto.md`).

**Método.**

1. Texto extraído del PDF con PyMuPDF a un directorio temporal, fuera de cualquier repositorio.
2. Códigos y páginas: titulares en negrita de cada señal (13 pt en el capítulo 2) y rótulos de las
   figuras de clasificación. Las dos vías dan el mismo recuento para SR (61) y SP (87).
3. Formas y colores: texto de los apartados de características (p. 35-36, 59, 103, 146, 182, 202,
   600-610) y **lectura visual** de las figuras de clasificación (Figura 2-13, p. 55-59; Figura 2-26,
   p. 97-102; Figura 2-66, p. 182-184; Figura 2-73, p. 203-205; Figuras 6-31 a 6-34, p. 543-552;
   Figura 6-65, p. 587; Figuras 7-5 y 7-7, p. 610-624). Donde el texto y la figura no coinciden, se
   anota en la sección 7.
4. La transcripción previa `Manual-Senalizacion-Vial-Extracto.md` se usó como punto de partida y se
   cruzó con el PDF. **Tiene un hueco:** no recoge las familias del capítulo 6 (SIP, SRC, SPC, SIC,
   SRM, SIM), 35 códigos. Salieron al contar todos los prefijos del texto completo.

No se ha copiado ninguna página del Manual ni de la NTC a este repositorio; las citas literales son
frases cortas.

---

## 2. Familias y prefijos

El Manual clasifica las señales verticales en **tres categorías** —reglamentarias, preventivas e
informativas— que pueden adquirir condición **transitoria** (p. 32-33). **No define en ningún sitio
una regla de codificación**: los prefijos se usan sin enunciarse. La única regla escrita es la de obra:
a la codificación «se agrega la letra "O" o "PO"» (p. 600).

| Prefijo | Familia | Capítulo, numeral | Páginas | Señales |
| :--- | :--- | :--- | :--- | ---: |
| SR | Reglamentarias | Cap. 2, num. 2.2 | 54-95 | 61 |
| SP | Preventivas | Cap. 2, num. 2.3 | 96-143 | 87 |
| SI | Informativas (dirección, posición e indicación; servicios generales y especiales) | Cap. 2, num. 2.4 y 2.5 | 144-199 | 49 |
| ST | Informativas turísticas | Cap. 2, num. 2.6 | 199-216 | 33 |
| SIT | Informativas de túnel | Cap. 2, num. 2.8 | 252-261 | 9 |
| SIP | Informativas para peatones | Cap. 6, num. 6.1 | 512-522 | 4 |
| SRC | Reglamentarias para ciclo-infraestructura | Cap. 6, num. 6.2.4.4 | 542-548 | 9 |
| SPC | Preventivas para ciclo-infraestructura | Cap. 6, num. 6.2.4.5 | 549-551 | 4 |
| SIC | Informativas para ciclo-infraestructura | Cap. 6 | 551-558 | 11 |
| SRM | Reglamentaria para motociclistas | Cap. 6, num. 6.3.4.3 | 586 | 1 |
| SIM | Informativas para motociclistas | Cap. 6, num. 6.3.4.4 | 587-589 | 6 |
| SRO | Reglamentarias en obra | Cap. 7 | 609, 661-662 | 4 |
| SPO | Preventivas de obra (uso exclusivo de obra) | Cap. 7 | 618-621 | 6 |
| SPPO | Preventivas propias de obra (SP con fondo anaranjado) | Cap. 7 | 610-617 | 68 |
| SIO | Informativas de obra | Cap. 7 | 621-635 | 24 |
| **Total** | | | | **376** |

**Lo que el encargo mencionaba y el Manual no tiene como familia:**

- **Escolares.** No hay prefijo propio. Son tres preventivas de la serie SP: SP-47 ZONA ESCOLAR,
  SP-47A y SP-47B, con forma de pentágono y fondo amarillo verde fluorescente (p. 103). Buscado como
  prefijo y como sección en el texto completo: no aparece.
- **Eventos especiales** (capítulo 8, p. 783-806). **No tienen códigos propios.** Usan las señales de
  obra con fondo **rosado fluorescente**: preventivas (p. 790) e informativas (SIO, p. 794).
- **Señales de mensaje variable** (numeral 2.7, p. 221-251): son paneles emisores, no láminas. Fuera.

---

## 3. Características por familia

`lamina_minima` en el CSV resume la exigencia; la regla completa es la de la sección 4.

| Familia | Forma | Fondo | Orla | Símbolo / leyenda | Página |
| :--- | :--- | :--- | :--- | :--- | :--- |
| SR | Círculo; rectángulo si lleva leyenda. Excepciones: SR-01 octógono, SR-02 triángulo invertido, SR-38 y SR-39 rectángulo | Blanco; excepcionalmente rojo | Roja; excepcionalmente negra | Negro, blanco; excepcionalmente rojo o gris (el gris, en fin de prohibición) | 35, 59 |
| SR prohibición | Círculo con franja diagonal roja a 45°, descendente desde la izquierda | Blanco | Roja | Negro | 67 |
| SR autorización (SR-34, 40, 42) | Círculo | Blanco | Roja («color rojo del círculo») | Negro | 94 |
| SP | Rombo. Excepciones: SP-47, 47A, 47B (pentágono), SP-75 (rectángulo), SP-54 (cruz de San Andrés) | Amarillo; amarillo verde fluorescente en las de usuarios vulnerables | Negra | Negro | 35-36, 103 |
| SI dirección, posición, indicación | Rectangular o cuadrada; flecha y escudos de ruta como excepción | Verde; blanco en identificación vial (SI-01 a SI-03) | Blanca | Blanco; negro en identificación vial | 146 |
| SI servicios generales y especiales | Rectangular | Azul, con recuadro blanco | Blanca | Negro sobre el recuadro blanco. Excepción: SI-27 y SI-27A, fondo blanco, orla y texto negros | 146, 182 |
| ST turísticas | Rectangular (cuadrada en la Figura 2-72) | Marrón | Blanca | Blanco; flecha y distancia marrón sobre blanco | 146, 200, 202 |
| SIT túnel | Cuadrada (SIT-03 y 04, rectangulares) | SIT-01 a 04: verde, **fotoluminiscente**. SIT-05 a 09: azul con recuadro blanco (lectura de la figura) | Blanca | Blanco / negro y rojo | 258-261 |
| SIP peatones | Rectangular | Verde (SIP-01) o blanco (SIP-02 a 04) | — | — | 513, 522 |
| SRC ciclo-infraestructura | Círculo, como SR | Blanco | Roja | Negro | 543 (figura) |
| SPC ciclo-infraestructura | Rombo, como SP | Amarillo | Negra | Negro | 549 (figura) |
| SIC ciclo-infraestructura | Rectangular o cuadrada | Verde; recuadro azul para corredores principales (SIC-01) | Blanca | Blanco | 552, 554 |
| SRM / SIM motociclistas | SRM: círculo. SIM: cuadrada | SRM: blanco. SIM: verde; SIM-06 azul | SRM roja; SIM blanca | Negro / blanco | 586-587 |
| SRO obra | Círculo, como SR; SRO-04 es paleta PARE/SIGA | Blanco (SRO-04: rojo y verde) | Roja | Negro | 609, 662 |
| SPO / SPPO obra | Rombo (SPPO conserva la forma de la SP de origen) | **Anaranjado**; SPO-01 y SPO-03 **anaranjado fluorescente** | Negra | Negro | 600, 604, 610 |
| SIO obra | Rectangular | Anaranjado; «la primera señal de la zona de obra» anaranjado fluorescente | Negra | Negro | 604, 621 |
| Eventos especiales (sin código) | La de la señal de obra | **Rosado fluorescente** | Negra | Negro | 790, 794 |

Tabla 2-2 del Manual (p. 36), colores y aplicación: rojo → reglamentarias; amarillo y amarillo/verde
fluorescente → preventivas; verde → informativas; azul → servicios generales; marrón → turísticas;
anaranjado → obra; rosado fluorescente → eventos especiales; blanco → informativas. *El texto
extraído de esa tabla no conserva el emparejamiento fila a fila; el emparejamiento de arriba es el de
la tabla transcrita en el extracto del V5 y cuadra con los apartados de cada familia.*

---

## 4. Lámina retrorreflectiva exigida

| Caso | Lámina | Página |
| :--- | :--- | :--- |
| Señal vertical a la derecha de la vía (caso general) | **Tipo IV** (mínimos de la Tabla 2-5) | 39 |
| A la izquierda en calzada bidireccional de 2+ carriles por sentido o unidireccional de 3+; vías exclusivas de transporte público; **señales elevadas**; **SR-01 y SR-02** | **Tipo XI** (mínimos de la Tabla 2-6) | 39 |
| SR-01 PARE | «superiores a las Tipo IV» y medición periódica | 63 |
| Informativa sobre calzada o berma (pórtico o bandera) | Tipo IV «o superior (aumentado por un factor de 3)» | 160 |
| Informativa por encima de la altura recomendada | Tipo XI | 160 |
| Señales en la parte superior de un túnel | Se recomienda tipo XI o superior | 261 |
| Ciclo-infraestructura (SRC, SPC, SIC) | Los mismos criterios de 2.1.3.5, es decir IV / XI | 542 |
| Motociclistas (SRM, SIM) | Los del capítulo 2 | 586 |
| SIP-01 PASO PEATONAL | **No le aplica** el requisito de retrorreflectividad | 513 |
| SIP-02 a SIP-04 | El Manual no lo dice | — |
| SIT-01 a SIT-04 | **Fotoluminiscentes** (ASTM E2072, ISO 16069, UNE 23035-4, DIN 67510) | 258-259 |
| SIT-05 a SIT-09 | Se «recomiendan» fotoluminiscentes; sin tipo de lámina | 258 |
| Señales de obra (SRO, SPO, SPPO, SIO) | Tipo IV o superior | 602, 606 |
| Señales de obra enrollables | Tipo VI o superior | 51, 606 |
| Eventos especiales | Los materiales del capítulo 7 (obra) | 790 |

**Criterio de reemplazo:** «En el momento que la retrorreflectividad alcance los niveles mínimos
descritos anteriormente, se debe reemplazar la señal» (p. 39). Se exige medir antes y después de
limpiar (p. 54).

**Colores con mínimo de R<sub>A</sub> en el Manual** (Tablas 2-5 y 2-6, p. 39-40, verificadas
contra la imagen de la página): blanco, amarillo, anaranjado, verde, rojo, azul, púrpura, marrón/café,
amarillo verde fluorescente, amarillo fluorescente, anaranjado fluorescente. **Sin mínimo:** rosado
fluorescente, gris y negro (el negro está excluido expresamente, p. 39).

**El tipo I no aparece en ninguna exigencia del Manual.** Buscado en el texto completo: para señales
solo se citan los tipos IV, VI y XI; la única otra mención es «Tipo V» para las cintas de vehículos de
carga (p. 672, por NTC 5807). La tabla de tipo I solo está en la NTC (Tabla 1, pág. 7).

---

## 5. Tablas por señal

Todas las reglamentarias y preventivas del Manual, de todos los capítulos (SR, SP, SRC, SPC, SRM, SRO,
SPO, SPPO), y además todas las informativas con código (SI, ST, SIT, SIP, SIC, SIM, SIO): el Manual
las enumera en número finito y no costaba más incluirlas. Columna «Colores» = fondo / orla / símbolo.
Página = descripción de la señal; si no la tiene propia, la figura.

### SR — Reglamentarias (61)

Códigos no usados: SR-15, SR-27, SR-37, SR-57. SR-11 y SR-39 comparten nombre: la primera es circular (restricción), la segunda rectangular (obligación).

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SR-01 | PARE | octogono | rojo / blanco / blanco | superior a IV; XI | 63 |
| SR-02 | CEDA EL PASO | triangulo invertido | blanco / rojo / negro | XI | 66 |
| SR-49 | PRIORIDAD AL SENTIDO CONTRARIO | rectangulo vertical | blanco / negro / negro+rojo | IV | 66 |
| SR-04 | NO PASE | circulo | rojo / rojo / blanco | IV | 68 |
| SR-06 | PROHIBIDO GIRAR A LA IZQUIERDA | circulo | blanco / rojo / negro | IV | 69 |
| SR-08 | PROHIBIDO GIRAR A LA DERECHA | circulo | blanco / rojo / negro | IV | 70 |
| SR-10 | PROHIBIDO GIRAR EN “U” | circulo | blanco / rojo / negro | IV | 70 |
| SR-14 | PROHIBIDO CAMBIO DE CALZADA IZQUIERDA A DERECHA | circulo | blanco / rojo / negro | IV | 70 |
| SR-14A | PROHIBIDO CAMBIO DE CALZADA DERECHA A IZQUIERDA | circulo | blanco / rojo / negro | IV | 71 |
| SR-26 | NO ADELANTAR | circulo | blanco / rojo / negro | IV | 71 |
| SR-50 | PROHIBIDO GIRAR A LA DERECHA CON LUZ ROJA | rectangulo vertical | blanco / negro / negro+rojo | IV | 72 |
| SR-56 | ZONA PEATONAL (rotulada «Prioridad peatonal» en la Figura 2-13) | rectangulo vertical | blanco / negro / negro+rojo | IV | 73 |
| SR-16 | PROHIBIDO CIRCULACIÓN DE VEHÍCULOS AUTOMOTORES | circulo | blanco / rojo / negro | IV | 74 |
| SR-18 | PROHIBIDO CIRCULACIÓN DE VEHÍCULOS DE CARGA | circulo | blanco / rojo / negro | IV | 74 |
| SR-18A | PROHIBIDO CIRCULACIÓN DE VEHÍCULOS CON EJE ADICIONAL | circulo | blanco / rojo / negro | IV | 78 |
| SR-18B | PROHIBIDO CIRCULACIÓN DE VEHÍCULOS DESTINADOS AL TRANSPORTE DE MERCANCÍAS PELIGROSAS | circulo | blanco / rojo / negro | IV | 78 |
| SR-21 | PROHIBIDA CIRCULACIÓN DE CABALGADURAS | circulo | blanco / rojo / negro | IV | 75 |
| SR-22 | PROHIBIDA CIRCULACIÓN DE BICICLETAS Y MOTOCICLOS | circulo | blanco / rojo / negro | IV | 75 |
| SR-23 | PROHIBIDA CIRCULACIÓN DE MOTOCICLETAS | circulo | blanco / rojo / negro | IV | 75 |
| SR-24 | PROHIBIDA CIRCULACIÓN DE MAQUINARIA AGRÍCOLA | circulo | blanco / rojo / negro | IV | 76 |
| SR-25 | PROHIBIDA CIRCULACIÓN DE VEHÍCULOS DE TRACCIÓN ANIMAL | circulo | blanco / rojo / negro | IV | 76 |
| SR-51 | PROHIBIDA CIRCULACIÓN DE CARROS DE MANO | circulo | blanco / rojo / negro | IV | 76 |
| SR-52 | PROHIBIDA CIRCULACIÓN DE BUSES | circulo | blanco / rojo / negro | IV | 77 |
| SR-53 | PROHIBIDA CIRCULACIÓN DE MOTOCARROS | circulo | blanco / rojo / negro | IV | 77 |
| SR-54 | PROHIBIDA CIRCULACIÓN DE CUATRIMOTOS | circulo | blanco / rojo / negro | IV | 77 |
| SR-20 | PROHIBIDO CIRCULACIÓN DE PEATONES | circulo | blanco / rojo / negro | IV | 78 |
| SR-28 | PROHIBIDO PARQUEAR | circulo | blanco / rojo / negro | IV | 79 |
| SR-28A | PROHIBIDO PARQUEAR O DETENERSE | circulo | blanco / rojo / negro | IV | 79 |
| SR-29 | PROHIBIDO PITAR | circulo | blanco / rojo / negro | IV | 79 |
| SR-41 | PROHIBIDO EL ASCENSO Y DESCENSO DE PASAJEROS | circulo | blanco / rojo / negro | IV | 80 |
| SR-43 | PROHIBIDO EL CARGUE Y DESCARGUE | circulo | blanco / rojo / negro | IV | 80 |
| SR-47 | NO BLOQUEAR INTERSECCIÓN | circulo | blanco / rojo / negro | IV | 81 |
| SR-11 | CIRCULACIÓN EN AMBOS SENTIDOS | circulo | blanco / rojo / negro | IV | 81 |
| SR-12 | CIRCULACIÓN EN TRES CARRILES (UNO EN CONTRAFLUJO) | circulo | blanco / rojo / negro | IV | 82 |
| SR-13 | CIRCULACIÓN EN TRES CARRILES (DOS EN CONTRAFLUJO) | circulo | blanco / rojo / negro | IV | 82 |
| SR-30 | VELOCIDAD MÁXIMA PERMITIDA | circulo | blanco / rojo / negro | IV | 82 |
| SR-30A | VELOCIDAD MÍNIMA PERMITIDA | circulo | blanco / rojo / negro | IV | 84 |
| SR-30B | VELOCIDAD MÁXIMA PERMITIDA SALIDA | circulo | blanco / rojo / negro | IV | 85 |
| SR-31 | PESO MÁXIMO BRUTO PERMITIDO | circulo | blanco / rojo / negro | IV | 85 |
| SR-32 | ALTURA MÁXIMA PERMITIDA | circulo | blanco / rojo / negro | IV | 86 |
| SR-33 | ANCHO MÁXIMO PERMITIDO | circulo | blanco / rojo / negro | IV | 86 |
| SR-55 | LONGITUD MÁXIMA PERMITIDA | circulo | blanco / rojo / negro | IV | 87 |
| SR-48 | FIN PROHIBICIÓN | rectangulo vertical | blanco / negro / gris+negro | IV | 87 |
| SR-03 | DIRECCIÓN OBLIGADA O SIGA DE FRENTE | circulo | blanco / rojo / negro | IV | 88 |
| SR-05 | GIRO A LA IZQUIERDA SOLAMENTE | circulo | blanco / rojo / negro | IV | 88 |
| SR-07 | GIRO A LA DERECHA SOLAMENTE | circulo | blanco / rojo / negro | IV | 89 |
| SR-09 | GIRO EN “U” SOLAMENTE | circulo | blanco / rojo / negro | IV | 89 |
| SR-17 | VEHÍCULOS PESADOS A LA DERECHA | circulo | blanco / rojo / negro | IV | 89 |
| SR-19 | PEATONES A LA IZQUIERDA | circulo | blanco / rojo / negro | IV | 90 |
| SR-35 | CIRCULACIÓN CON LUCES BAJAS | circulo | blanco / rojo / negro | IV | 90 |
| SR-36 | RETÉN | circulo | blanco / rojo / negro | IV | 90 |
| SR-38 | SENTIDO ÚNICO DE CIRCULACIÓN | rectangulo horizontal | negro / blanco / blanco | IV | 91 |
| SR-39 | CIRCULACIÓN EN AMBOS SENTIDOS | rectangulo horizontal | negro / blanco / blanco | IV | 91 |
| SR-44 | CONSERVAR ESPACIAMIENTO | circulo | blanco / rojo / negro | IV | 92 |
| SR-45 | INDICACIÓN DE SEPARADOR DE TRÁNSITO A LA IZQUIERDA | circulo | blanco / rojo / negro | IV | 92 |
| SR-46 | INDICACIÓN DE SEPARADOR DE TRÁNSITO A LA DERECHA | circulo | blanco / rojo / negro | IV | 93 |
| SR-58 | CARRIL EXCLUSIVO | circulo | blanco / rojo / negro | IV | 93 |
| SR-59 | DISTANCIA LATERAL DE SEGURIDAD CON CICLISTAS | circulo | blanco / rojo / negro | IV | 94 |
| SR-34 | ZONA DE ESTACIONAMIENTO DE TAXI | circulo | blanco / rojo / negro | IV | 94 |
| SR-40 | ZONA EXCLUSIVA DE PARADERO | circulo | blanco / rojo / negro | IV | 95 |
| SR-42 | ZONA DE CARGUE Y DESCARGUE | circulo | blanco / rojo / negro | IV | 95 |

### SP — Preventivas (87)

Códigos no usados: SP-40, SP-58, SP-60 a SP-66, SP-68. Fluorescentes según p. 103, más SP-80 por la figura (ver M2).

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SP-01 | CURVA CERRADA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 109 |
| SP-02 | CURVA CERRADA A LA DERECHA | rombo | amarillo / negro / negro | IV | 109 |
| SP-03 | CURVA PRONUNCIADA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 109 |
| SP-04 | CURVA PRONUNCIADA A LA DERECHA | rombo | amarillo / negro / negro | IV | 110 |
| SP-05 | CURVA Y CONTRA-CURVA CERRADA PRIMERA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 110 |
| SP-06 | CURVA Y CONTRA-CURVA CERRADA PRIMERA A LA DERECHA | rombo | amarillo / negro / negro | IV | 111 |
| SP-07 | ZONA DE CURVAS SUCESIVAS LA PRIMERA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 111 |
| SP-08 | ZONA DE CURVAS SUCESIVAS LA PRIMERA A LA DERECHA | rombo | amarillo / negro / negro | IV | 112 |
| SP-09 | CURVA Y CONTRA-CURVA PRONUNCIADA PRIMERA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 112 |
| SP-10 | CURVA Y CONTRA-CURVA PRONUNCIADA PRIMERA A LA DERECHA | rombo | amarillo / negro / negro | IV | 113 |
| SP-11 | INTERSECCIÓN DE VÍAS | rombo | amarillo / negro / negro | IV | 123 |
| SP-12 | VÍA LATERAL IZQUIERDA | rombo | amarillo / negro / negro | IV | 123 |
| SP-13 | VÍA LATERAL DERECHA | rombo | amarillo / negro / negro | IV | 123 |
| SP-14 | INTERSECCIÓN EN “T” | rombo | amarillo / negro / negro | IV | 124 |
| SP-15 | BIFURCACIÓN EN “Y” | rombo | amarillo / negro / negro | IV | 124 |
| SP-16 | BIFURCACIÓN A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 124 |
| SP-17 | BIFURCACIÓN A LA DERECHA | rombo | amarillo / negro / negro | IV | 125 |
| SP-18 | INTERSECCIÓN ESCALONADA PRIMERA IZQUIERDA | rombo | amarillo / negro / negro | IV | 125 |
| SP-19 | INTERSECCIÓN ESCALONADA PRIMERA DERECHA | rombo | amarillo / negro / negro | IV | 125 |
| SP-20 | GLORIETA | rombo | amarillo / negro / negro | IV | 126 |
| SP-21 | INCORPORACIÓN DE TRÁNSITO DESDE LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 126 |
| SP-22 | INCORPORACIÓN DE TRÁNSITO DESDE LA DERECHA | rombo | amarillo / negro / negro | IV | 126 |
| SP-23 | PROXIMIDAD DE SEMÁFORO | rombo | amarillo / negro / negro+rojo+amarillo+verde | IV | 130 |
| SP-24 | SUPERFICIE RIZADA | rombo | amarillo / negro / negro | IV | 115 |
| SP-25 | PROXIMIDAD A RESALTO | rombo | amarillo / negro / negro | IV | 116 |
| SP-25A | UBICACIÓN DE RESALTO | rombo | amarillo / negro / negro | IV | 116 |
| SP-25B | PROXIMIDAD A REDUCTOR TRAPEZOIDAL/POMPEYANO | rombo | amarillo / negro / negro | IV | 117 |
| SP-25C | UBICACIÓN DE REDUCTOR TRAPEZOIDAL O POMPEYANO | rombo | amarillo / negro / negro | IV | 117 |
| SP-26 | DEPRESIÓN | rombo | amarillo / negro / negro | IV | 118 |
| SP-27 | PENDIENTE FUERTE DE DESCENSO | rombo | amarillo / negro / negro | IV | 114 |
| SP-27A | PENDIENTE FUERTE DE ASCENSO | rombo | amarillo / negro / negro | IV | 115 |
| SP-28 | REDUCCIÓN DE CALZADA A AMBOS LADOS | rombo | amarillo / negro / negro | IV | 119 |
| SP-29 | PROXIMIDAD A SEÑAL DE “PARE” | rombo | amarillo / negro / negro+rojo+blanco | IV | 130 |
| SP-30 | REDUCCIÓN DE LA CALZADA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 119 |
| SP-31 | REDUCCIÓN DE LA CALZADA A LA DERECHA | rombo | amarillo / negro / negro | IV | 119 |
| SP-32 | ENSANCHAMIENTO SIMÉTRICO DE LA CALZADA | rombo | amarillo / negro / negro | IV | 120 |
| SP-33 | PROXIMIDAD DE SEÑAL “CEDA EL PASO” | rombo | amarillo / negro / negro+rojo+blanco | IV | 131 |
| SP-34 | ENSANCHAMIENTO DE LA CALZADA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 120 |
| SP-35 | ENSANCHAMIENTO DE LA CALZADA A LA DERECHA | rombo | amarillo / negro / negro | IV | 120 |
| SP-36 | PUENTE ANGOSTO | rombo | amarillo / negro / negro | IV | 121 |
| SP-37 | TÚNEL | rombo | amarillo / negro / negro | IV | 140 |
| SP-38 | PESO MÁXIMO BRUTO VEHICULAR PERMITIDO | rombo | amarillo / negro / negro | IV | 121 |
| SP-39 | DOS SENTIDOS DE TRÁNSITO | rombo | amarillo / negro / negro | IV | 131 |
| SP-41 | TRES CARRILES DE TRÁNSITO (UNO EN CONTRAFLUJO) | rombo | amarillo / negro / negro | IV | 131 |
| SP-42 | ZONA DE DESPRENDIMIENTO DE ROCAS | rombo | amarillo / negro / negro | IV | 141 |
| SP-43 | TRES CARRILES DE TRÁNSITO (DOS EN CONTRAFLUJO) | rombo | amarillo / negro / negro | IV | 132 |
| SP-44 | SUPERFICIE DESLIZANTE | rombo | amarillo / negro / negro | IV | 141 |
| SP-45 | MAQUINARIA AGRÍCOLA EN LA VÍA | rombo | amarillo / negro / negro | IV | 132 |
| SP-46 | ZONA DE PEATONES | rombo | amarillo verde fluorescente / negro / negro | IV | 133 |
| SP-46A | PROXIMIDAD DE CRUCE PEATONAL | rombo | amarillo verde fluorescente / negro / negro | IV | 133 |
| SP-46B | UBICACIÓN DE CRUCE PEATONAL | rombo | amarillo verde fluorescente / negro / negro | IV | 134 |
| SP-46C | ZONA CON PRIORIDAD PEATONAL | rectangulo vertical | amarillo verde fluorescente / negro / negro | IV | 134 |
| SP-47 | ZONA ESCOLAR | pentagono | amarillo verde fluorescente / negro / negro | IV | 134 |
| SP-47A | PROXIMIDAD A CRUCE ESCOLAR | pentagono | amarillo verde fluorescente / negro / negro | IV | 135 |
| SP-47B | UBICACIÓN DE CRUCE ESCOLAR | pentagono | amarillo verde fluorescente / negro / negro | IV | 136 |
| SP-48 | NIÑOS JUGANDO | rombo | amarillo verde fluorescente / negro / negro | IV | 136 |
| SP-49 | PRESENCIA DE ANIMALES EN LA VÍA | rombo | amarillo / negro / negro | IV | 137 |
| SP-49A | CRUCE DE ANIMALES EN LA VÍA | rombo | amarillo verde fluorescente / negro / negro | IV | 137 |
| SP-50 | ALTURA LIBRE | rombo | amarillo / negro / negro | IV | 122 |
| SP-51 | ANCHO LIBRE | rombo | amarillo / negro / negro | IV | 122 |
| SP-52 | CRUCE FERROVIARIO A NIVEL SIN BARRERA | rombo | amarillo / negro / negro | IV | 127 |
| SP-52A | CRUCE FERROVIARIO A NIVEL CON BARRERAS | rombo | amarillo / negro / negro | IV | 127 |
| SP-53 | BARRERA | rombo | amarillo / negro / negro | IV | 127 |
| SP-54 | CRUZ DE SAN ANDRÉS | cruz san andres | blanco / negro / negro | IV | 128 |
| SP-55 | INICIACIÓN DE SEPARADOR (DOS SENTIDOS) | rombo | amarillo / negro / negro | IV | 138 |
| SP-55A | INICIACIÓN DE SEPARADOR (UN SENTIDO) | rombo | amarillo / negro / negro | IV | 138 |
| SP-56 | TERMINACIÓN DE VÍA CON SEPARADOR (DOS SENTIDOS) | rombo | amarillo / negro / negro | IV | 138 |
| SP-56A | TERMINACIÓN DE VÍA CON SEPARADOR (UN SENTIDO) | rombo | amarillo / negro / negro | IV | 139 |
| SP-57 | FINAL DEL PAVIMENTO | rombo | amarillo / negro / negro | IV | 118 |
| SP-57A | CAMBIO DE TEXTURA EN SUPERFICIE DE RODADURA | rombo | amarillo / negro / negro+rojo | IV | 118 |
| SP-59 | CICLISTAS EN LA VÍA | rombo | amarillo verde fluorescente / negro / negro | IV | 139 |
| SP-59A | PROXIMIDAD A CRUCE DE CICLISTAS | rombo | amarillo verde fluorescente / negro / negro | IV | 139 |
| SP-59B | UBICACIÓN DE CRUCE DE CICLISTAS | rombo | amarillo verde fluorescente / negro / negro | IV | 140 |
| SP-67 | RIESGO DE SINIESTRO | rombo | amarillo / negro / negro | IV | 141 |
| SP-69 | CURVA MUY CERRADA A LA IZQUIERDA | rombo | amarillo / negro / negro | IV | 113 |
| SP-70 | CURVA MUY CERRADA A LA DERECHA | rombo | amarillo / negro / negro | IV | 113 |
| SP-71 | PROYECCIÓN DE GRAVILLA | rombo | amarillo / negro / negro | IV | 142 |
| SP-72 | SALIDA DE VEHÍCULOS DE BOMBEROS | rombo | amarillo / negro / negro | IV | 142 |
| SP-73 | RÁFAGAS DE VIENTO LATERAL | rombo | amarillo / negro / negro | IV | 142 |
| SP-74 | DESNIVEL SEVERO | rombo | amarillo / negro / negro | IV | 143 |
| SP-75 | DELINEADOR DE CURVA HORIZONTAL | rectangulo vertical | amarillo o amarillo verde fluorescente / negro / negro | IV | 108 |
| SP-76 | LONGITUD MÁXIMA PERMITIDA | rombo | amarillo / negro / negro | IV | 122 |
| SP-77 | ZONA DE NIEBLA | rombo | amarillo / negro / negro | IV | 143 |
| SP-78 | PUENTE LEVADIZO | rombo | amarillo / negro / negro | IV | 143 |
| SP-79 | VEHÍCULOS DE CARGA O EXTRA-DIMENSIONADOS | rombo | amarillo / negro / negro | IV | 132 |
| SP-80 | TRICIMÓVILES EN LA VÍA | rombo | amarillo verde fluorescente / negro / negro | IV | 140 |
| SP-81 | CRUCE FERROVIARIO CON DESNIVEL DE RASANTES | rombo | amarillo / negro / negro | IV | 128 |

### SI — Informativas (dirección, posición e indicación; servicios generales y especiales) (49)

Códigos no usados: SI-12, SI-24, SI-28. SI-04 a SI-06 y SI-26 no tienen titular propio: la página es la de su figura.

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SI-01 | RUTA NACIONAL | escudo | blanco / negro / negro | IV | 175 |
| SI-01A | RUTA DEPARTAMENTAL | escudo | blanco / negro / negro | IV | 175 |
| SI-02 | RUTA PANAMERICANA | escudo | blanco / negro / negro | IV | 175 |
| SI-03 | RUTA MARGINAL DE LA SELVA | escudo | blanco / negro / negro | IV | 175 |
| SI-04 | POSTES DE REFERENCIA | rectangulo vertical | verde / blanco / blanco | IV | 177 |
| SI-05 | SEÑALES DE DIRECCIÓN | rectangulo horizontal | verde / blanco / blanco | IV | 171 |
| SI-05A | SEÑALES DE DIRECCIÓN / SALIDA INMEDIATA | rectangulo horizontal | verde / blanco / blanco | IV | 173 |
| SI-05B | DESTINOS EN GLORIETA | rectangulo horizontal | verde / blanco / blanco | IV | 157 |
| SI-05C | RUTA ALTERNATIVA | rectangulo horizontal | verde / blanco / blanco | IV | 170 |
| SI-05D | SEÑALES DE PRESEÑALIZACIÓN | rectangulo horizontal | verde / blanco / blanco | IV | 169 |
| SI-06 | SEÑALES DE CONFIRMACIÓN | rectangulo horizontal | verde / blanco / blanco | IV | 174 |
| SI-26 | NOMBRE DE CALLES Y NOMENCLATURA URBANA | rectangulo horizontal | verde / blanco / blanco | IV | 175 |
| SI-07 | SITIO DE PARQUEO | rectangulo vertical | azul / blanco / negro | IV | 186 |
| SI-07A | ZONAS ESPECIALES DE PARQUEO | rectangulo vertical | azul / blanco / negro | IV | 186 |
| SI-07B | ZONAS ESPECIALES DE PARQUEO DE BICICLETAS | rectangulo vertical | azul / blanco / negro | IV | 187 |
| SI-07C | ZONAS ESPECIALES DE PARQUEO DE VEHÍCULOS ELÉCTRICOS | rectangulo vertical | azul / blanco / negro | IV | 187 |
| SI-08 | PARADERO DE BUSES | rectangulo vertical | azul / blanco / negro | IV | 187 |
| SI-09 | ESTACIONAMIENTO DE TAXIS | rectangulo vertical | azul / blanco / negro | IV | 187 |
| SI-09A | ESTACIONAMIENTO DE TRICIMÓVILES | rectangulo vertical | azul / blanco / negro | IV | 188 |
| SI-10 | SERVICIO DE TRANSBORDADOR | rectangulo vertical | azul / blanco / negro | IV | 188 |
| SI-11 | VÍA PARA CICLISTAS | rectangulo vertical | azul / blanco / negro | IV | 189 |
| SI-11A | PRIORIDAD VÍA PARA CICLISTAS | rectangulo vertical | azul / blanco / negro | IV | 189 |
| SI-13 | ZONA MILITAR | rectangulo vertical | azul / blanco / negro | IV | 189 |
| SI-14 | AEROPUERTO | rectangulo vertical | azul / blanco / negro | IV | 190 |
| SI-15 | HOSPEDAJE | rectangulo vertical | azul / blanco / negro | IV | 190 |
| SI-16 | PRIMEROS AUXILIOS | rectangulo vertical | azul / blanco / negro | IV | 190 |
| SI-16A | HOSPITAL | rectangulo vertical | azul / blanco / negro | IV | 191 |
| SI-17 | SERVICIOS SANITARIOS | rectangulo vertical | azul / blanco / negro | IV | 191 |
| SI-18 | RESTAURANTE | rectangulo vertical | azul / blanco / negro | IV | 191 |
| SI-19 | TELÉFONO | rectangulo vertical | azul / blanco / negro | IV | 192 |
| SI-20 | IGLESIA | rectangulo vertical | azul / blanco / negro | IV | 192 |
| SI-21 | TALLER | rectangulo vertical | azul / blanco / negro | IV | 192 |
| SI-22 | ESTACIÓN DE SERVICIO | rectangulo vertical | azul / blanco / negro | IV | 193 |
| SI-22A | ESTACIÓN DE CARGA DE VEHÍCULOS ELÉCTRICOS | rectangulo vertical | azul / blanco / negro | IV | 193 |
| SI-23 | MONTALLANTAS | rectangulo vertical | azul / blanco / negro | IV | 193 |
| SI-25 | PASO O INSTALACIÓN ACCESIBLE | rectangulo vertical | azul / blanco / negro | IV | 194 |
| SI-25A | CRUCE DE PERSONAS CON Y/O EN SITUACIÓN DE DISCAPACIDAD VISUAL | rectangulo vertical | azul / blanco / negro | IV | 194 |
| SI-27 | SEGURIDAD VIAL | rectangulo vertical | blanco / negro / negro | IV | 194 |
| SI-27A | SEGURIDAD VIAL EN INTERSECCIONES SEMAFORIZADAS | rectangulo vertical | blanco / negro / negro | IV | 195 |
| SI-27B | RADAR PEDAGÓGICO | rectangulo vertical | azul / blanco / negro | IV | 195 |
| SI-29 | TRANSPORTE FERROVIARIO | rectangulo vertical | azul / blanco / negro | IV | 195 |
| SI-30 | TRANSPORTE MASIVO | rectangulo vertical | azul / blanco / negro | IV | 196 |
| SI-30A | TRANSPORTE POR CABLE | rectangulo vertical | azul / blanco / negro | IV | 196 |
| SI-31 | ZONA RECREATIVA | rectangulo vertical | azul / blanco / negro | IV | 196 |
| SI-32 | TSUNAMI RUTA DE EVACUACIÓN | rectangulo vertical | azul / blanco / negro | IV | 197 |
| SI-33 | ZONA DE RIESGO POR TSUNAMI | rectangulo vertical | azul / blanco / negro | IV | 197 |
| SI-34 | PUNTO DE ENCUENTRO POR TSUNAMI | rectangulo vertical | azul / blanco / negro | IV | 197 |
| SI-35 | SISTEMA PARA DETECCIÓN ELECTRÓNICA DE INFRACCIONES | rectangulo vertical | azul / blanco / negro | IV | 198 |
| SI-35A | ZONA DE CONTROL CON SISTEMA TECNOLÓGICO DE DETECCIÓN | rectangulo vertical | azul / blanco / negro | IV | 199 |

### ST — Informativas turísticas (33)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| ST-01 | ZONA DE CAMPING | cuadrado | marron / blanco / blanco | IV | 205 |
| ST-02 | PLAYA | cuadrado | marron / blanco / blanco | IV | 205 |
| ST-03 | MUSEO | cuadrado | marron / blanco / blanco | IV | 206 |
| ST-04 | MUELLE | cuadrado | marron / blanco / blanco | IV | 206 |
| ST-05 | ZOOLÓGICO | cuadrado | marron / blanco / blanco | IV | 206 |
| ST-06 | PUNTO DE INFORMACIÓN TURÍSTICA | cuadrado | marron / blanco / blanco | IV | 207 |
| ST-07 | ARTESANÍAS | cuadrado | marron / blanco / blanco | IV | 207 |
| ST-08 | BIENES ARQUEOLÓGICOS | cuadrado | marron / blanco / blanco | IV | 207 |
| ST-09 | CUERPO DE AGUA | cuadrado | marron / blanco / blanco | IV | 208 |
| ST-10 | POLIDEPORTIVO | cuadrado | marron / blanco / blanco | IV | 208 |
| ST-11 | MIRADOR | cuadrado | marron / blanco / blanco | IV | 208 |
| ST-12 | ALQUILER DE AUTOS | cuadrado | marron / blanco / blanco | IV | 209 |
| ST-12A | ALQUILER DE BICICLETAS O PATINETAS | cuadrado | marron / blanco / blanco | IV | 209 |
| ST-13 | ATRACTIVO NATURAL | cuadrado | marron / blanco / blanco | IV | 209 |
| ST-14 | VOLCÁN | cuadrado | marron / blanco / blanco | IV | 210 |
| ST-15 | NEVADO | cuadrado | marron / blanco / blanco | IV | 210 |
| ST-16 | TERMAL | cuadrado | marron / blanco / blanco | IV | 210 |
| ST-17 | CASCADA | cuadrado | marron / blanco / blanco | IV | 211 |
| ST-18 | PESCA | cuadrado | marron / blanco / blanco | IV | 211 |
| ST-19 | ARRECIFE CORALINO | cuadrado | marron / blanco / blanco | IV | 211 |
| ST-20 | CAVERNA | cuadrado | marron / blanco / blanco | IV | 212 |
| ST-21 | PÁRAMO | cuadrado | marron / blanco / blanco | IV | 212 |
| ST-22 | RÍO | cuadrado | marron / blanco / blanco | IV | 212 |
| ST-23 | PARQUE NACIONAL NATURAL | cuadrado | marron / blanco / blanco | IV | 213 |
| ST-24 | OBSERVATORIO DE FLORA Y FAUNA | cuadrado | marron / blanco / blanco | IV | 213 |
| ST-25 | SENDERO PARA EXCURSIONISTAS | cuadrado | marron / blanco / blanco | IV | 213 |
| ST-26 | PARAPENTE | cuadrado | marron / blanco / blanco | IV | 214 |
| ST-27 | ESCALADA | cuadrado | marron / blanco / blanco | IV | 214 |
| ST-28 | RAFTING | cuadrado | marron / blanco / blanco | IV | 214 |
| ST-29 | COMUNIDAD INDÍGENA | cuadrado | marron / blanco / blanco | IV | 215 |
| ST-30 | MONUMENTO NACIONAL | cuadrado | marron / blanco / blanco | IV | 215 |
| ST-31 | PATRIMONIO DE LA HUMANIDAD | cuadrado | marron / blanco / blanco | IV | 215 |
| ST-32 | CENTRO HISTÓRICO | cuadrado | marron / blanco / blanco | IV | 216 |

### SIT — Informativas de túnel (9)

SIT-01 a SIT-04 son fotoluminiscentes: no se miden con retrorreflectómetro.

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SIT-01 | SALIDA DE EMERGENCIA A LA IZQUIERDA | cuadrado | verde / blanco / blanco | no aplica: fotoluminiscente | 258 |
| SIT-02 | SALIDA DE EMERGENCIA A LA DERECHA | cuadrado | verde / blanco / blanco | no aplica: fotoluminiscente | 258 |
| SIT-03 | RUTA DE ESCAPE A SALIDA DE EMERGENCIA A LA IZQUIERDA | rectangulo horizontal | verde / blanco / blanco | no aplica: fotoluminiscente | 259 |
| SIT-04 | RUTA DE ESCAPE A SALIDA DE EMERGENCIA A LA DERECHA | rectangulo horizontal | verde / blanco / blanco | no aplica: fotoluminiscente | 259 |
| SIT-05 | TELÉFONO DE EMERGENCIA | cuadrado | azul / blanco / negro | no definida: fotoluminiscente recomendado | 260 |
| SIT-06 | EXTINTOR DE INCENDIOS | cuadrado | azul / blanco / negro+rojo | no definida: fotoluminiscente recomendado | 260 |
| SIT-07 | HIDRANTE Y MANGUERA PARA APAGAR INCENDIOS | cuadrado | azul / blanco / negro+rojo | no definida: fotoluminiscente recomendado | 260 |
| SIT-08 | BAHÍA DE ESTACIONAMIENTO PARA EMERGENCIAS | cuadrado | azul / blanco / negro | no definida: fotoluminiscente recomendado | 260 |
| SIT-09 | SISTEMA DE RADIO DEDICADO | cuadrado | azul / blanco / negro | no definida: fotoluminiscente recomendado | 261 |

### SIP — Informativas para peatones (4)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SIP-01 | PASO PEATONAL | rectangulo horizontal | verde / blanco / blanco | no aplica | 513 |
| SIP-02 | PASO DE VÍA FÉRREA | rectangulo vertical | blanco / negro / negro+rojo | no especificada | 522 |
| SIP-03 | OBSERVE ANTES DE CRUZAR | rectangulo horizontal | blanco / negro / negro | no especificada | 522 |
| SIP-04 | CALLE CÍVICA | rectangulo horizontal | blanco / negro / negro+rojo | no especificada | 513 |

### SRC — Reglamentarias para ciclo-infraestructura (9)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SRC-01 | CONSERVE LA DERECHA | circulo | blanco / rojo / negro | IV | 545 |
| SRC-02 | OBLIGATORIO DESCENDER DE LA BICICLETA | circulo | blanco / rojo / negro | IV | 545 |
| SRC-03 | CIRCULACIÓN NO COMPARTIDA | circulo | blanco / rojo / negro | IV | 546 |
| SRC-04 | CIRCULACIÓN PROHIBIDA DE MASCOTAS | circulo | blanco / rojo / negro | IV | 546 |
| SRC-05 | CIRCULACIÓN COMPARTIDA | circulo | blanco / rojo / negro | IV | 546 |
| SRC-06 | CICLO-INFRAESTRUCTURA | circulo | blanco / rojo / negro | IV | 547 |
| SRC-07 | CARRIL BUS-BICI | circulo | blanco / rojo / negro | IV | 547 |
| SRC-08 | CARRIL CICLOPREFERENTE | circulo | blanco / rojo / negro | IV | 548 |
| SRC-09 | BANDA CICLOPREFERENTE | circulo | blanco / rojo / negro | IV | 548 |

### SPC — Preventivas para ciclo-infraestructura (4)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SPC-01 | VEHÍCULOS EN LA CICLO-INFRAESTRUCTURA | rombo | amarillo / negro / negro | IV | 550 |
| SPC-02 | DESCENSO FUERTE | rombo | amarillo / negro / negro | IV | 550 |
| SPC-03 | ASCENSO FUERTE | rombo | amarillo / negro / negro | IV | 550 |
| SPC-04 | CIRCULACIÓN DE BICICLETAS A CONTRAFLUJO | rombo | amarillo / negro / negro | IV | 551 |

### SIC — Informativas para ciclo-infraestructura (11)

SIC-08 FIN DE CICLOBANDA se cita en p. 551 sin figura ni Banco: no va (M5).

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SIC-01 | NOMBRE O CÓDIGO DE LA CICLO-INFRAESTRUCTURA | rectangulo vertical | verde / blanco / blanco | IV | 554 |
| SIC-02 | DIRECCIÓN INMEDIATA DE LA CICLO-INFRAESTRUCTURA | rectangulo horizontal | verde / blanco / blanco | IV | 554 |
| SIC-02A | UBICACIÓN Y DIRECCIÓN ANTICIPADA | rectangulo horizontal | verde / blanco / blanco | IV | 555 |
| SIC-02B | DIRECCIÓN ESQUEMÁTICA | cuadrado | verde / blanco / blanco | IV | 555 |
| SIC-04 | FIN DE CICLORRUTA | cuadrado | verde / blanco / blanco+rojo | IV | 556 |
| SIC-05 | INICIO DE CICLORRUTA | cuadrado | verde / blanco / blanco | IV | 556 |
| SIC-06 | ZONA COMPARTIDA CICLISTAS – PEATONES | cuadrado | verde / blanco / blanco | IV | 556 |
| SIC-09 | VÍA EXCLUSIVA | cuadrado | verde / blanco / blanco+rojo | IV | 557 |
| SIC-11 | PARADERO EN LA CICLO-INFRAESTRUCTURA | rectangulo vertical | blanco / negro / negro+rojo | IV | 557 |
| SIC-12 | CONFIRMACIÓN DE DESTINO | rectangulo horizontal | verde / blanco / blanco | IV | 558 |
| SIC-12A | CONFIRMACIÓN DE UBICACIÓN | rectangulo horizontal | verde / blanco / blanco | IV | 558 |

### SRM — Reglamentaria para motociclistas (1)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SRM-01 | SOLO MOTOCICLETAS | circulo | blanco / rojo / negro | IV | 586 |

### SIM — Informativas para motociclistas (6)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SIM-01 | INICIO MOTOVÍA | cuadrado | verde / blanco / blanco | IV | 587 |
| SIM-02 | FIN MOTOVÍA | cuadrado | verde / blanco / blanco+rojo | IV | 588 |
| SIM-03 | ZONA DE ESPERA ADELANTADA DE MOTOS EN INTERSECCIÓN | cuadrado | verde / blanco / blanco | IV | 588 |
| SIM-04 | ZONA DE ESPERA ADELANTADA DE MOTOS EN INTERSECCIÓN – CON CARRIL EXCLUSIVO | cuadrado | verde / blanco / blanco | IV | 588 |
| SIM-05 | ZONA DE ESPERA ADELANTADA DE MOTOS EN INTERSECCIÓN – CON CARRIL EXCLUSIVO Y GIRO PERMITIDO | cuadrado | verde / blanco / blanco | IV | 589 |
| SIM-06 | PARQUEADERO PARA MOTOS | cuadrado | azul / blanco / blanco | IV | 587 |

### SRO — Reglamentarias en obra (4)

SRO-04 PARE/SIGA es la paleta del auxiliar de tránsito: octógono rojo por una cara, círculo verde por la otra (p. 662).

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SRO-01 | VÍA CERRADA | circulo | blanco / rojo / negro | IV (VI si enrollable) | 609 |
| SRO-02 | DESVÍO | circulo | blanco / rojo / negro | IV (VI si enrollable) | 609 |
| SRO-03 | UNO A UNO | circulo | blanco / rojo / negro | IV (VI si enrollable) | 609 |
| SRO-04 | PARE/SIGA (paleta) | octogono o circulo | rojo o verde / blanco / blanco | IV (VI si enrollable) | 662 |

### SPO — Preventivas de obra (uso exclusivo de obra) (6)

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SPO-01 | TRABAJO EN LA VÍA | rombo | anaranjado fluorescente / negro / negro | IV (VI si enrollable) | 618 |
| SPO-02 | MAQUINARIA EN LA VÍA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 619 |
| SPO-03 | AUXILIAR DE TRÁNSITO | rombo | anaranjado fluorescente / negro / negro | IV (VI si enrollable) | 620 |
| SPO-04 | ANGOSTAMIENTO A AMBOS LADOS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 620 |
| SPO-05 | ANGOSTAMIENTO A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 620 |
| SPO-06 | ANGOSTAMIENTO A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 621 |

### SPPO — Preventivas propias de obra (SP con fondo anaranjado) (68)

Nombre = el de la SP del mismo número (p. 610). Página = la de la Figura 7-5. SPPO-47 está en la figura y no en el Banco (M6).

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SPPO-01 | CURVA CERRADA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 610 |
| SPPO-02 | CURVA CERRADA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 610 |
| SPPO-03 | CURVA PRONUNCIADA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 610 |
| SPPO-04 | CURVA PRONUNCIADA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 610 |
| SPPO-05 | CURVA Y CONTRA-CURVA CERRADA PRIMERA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-06 | CURVA Y CONTRA-CURVA CERRADA PRIMERA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-09 | CURVA Y CONTRA-CURVA PRONUNCIADA PRIMERA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-10 | CURVA Y CONTRA-CURVA PRONUNCIADA PRIMERA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-11 | INTERSECCIÓN DE VÍAS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-12 | VÍA LATERAL IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-13 | VÍA LATERAL DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-14 | INTERSECCIÓN EN “T” | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-15 | BIFURCACIÓN EN “Y” | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-16 | BIFURCACIÓN A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-17 | BIFURCACIÓN A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-21 | INCORPORACIÓN DE TRÁNSITO DESDE LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-22 | INCORPORACIÓN DE TRÁNSITO DESDE LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-23 | PROXIMIDAD DE SEMÁFORO | rombo | anaranjado / negro / negro+rojo+amarillo+verde | IV (VI si enrollable) | 611 |
| SPPO-24 | SUPERFICIE RIZADA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-25 | PROXIMIDAD A RESALTO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 611 |
| SPPO-25A | UBICACIÓN DE RESALTO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-25B | PROXIMIDAD A REDUCTOR TRAPEZOIDAL/POMPEYANO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-25C | UBICACIÓN DE REDUCTOR TRAPEZOIDAL O POMPEYANO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-26 | DEPRESIÓN | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-27 | PENDIENTE FUERTE DE DESCENSO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-27A | PENDIENTE FUERTE DE ASCENSO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-29 | PROXIMIDAD A SEÑAL DE “PARE” | rombo | anaranjado / negro / negro+rojo+blanco | IV (VI si enrollable) | 612 |
| SPPO-32 | ENSANCHAMIENTO SIMÉTRICO DE LA CALZADA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-33 | PROXIMIDAD DE SEÑAL “CEDA EL PASO” | rombo | anaranjado / negro / negro+rojo+blanco | IV (VI si enrollable) | 612 |
| SPPO-34 | ENSANCHAMIENTO DE LA CALZADA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-35 | ENSANCHAMIENTO DE LA CALZADA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-36 | PUENTE ANGOSTO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-37 | TÚNEL | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-38 | PESO MÁXIMO BRUTO VEHICULAR PERMITIDO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-39 | DOS SENTIDOS DE TRÁNSITO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-41 | TRES CARRILES DE TRÁNSITO (UNO EN CONTRAFLUJO) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 612 |
| SPPO-42 | ZONA DE DESPRENDIMIENTO DE ROCAS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-43 | TRES CARRILES DE TRÁNSITO (DOS EN CONTRAFLUJO) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-44 | SUPERFICIE DESLIZANTE | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-45 | MAQUINARIA AGRÍCOLA EN LA VÍA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-46 | ZONA DE PEATONES | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-46A | PROXIMIDAD DE CRUCE PEATONAL | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-46B | UBICACIÓN DE CRUCE PEATONAL | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-46C | ZONA CON PRIORIDAD PEATONAL | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-47 | ZONA ESCOLAR | pentagono | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-47A | PROXIMIDAD A CRUCE ESCOLAR | pentagono | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-47B | UBICACIÓN DE CRUCE ESCOLAR | pentagono | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-50 | ALTURA LIBRE | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-51 | ANCHO LIBRE | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-52 | CRUCE FERROVIARIO A NIVEL SIN BARRERA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-52A | CRUCE FERROVIARIO A NIVEL CON BARRERAS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-53 | BARRERA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 613 |
| SPPO-55 | INICIACIÓN DE SEPARADOR (DOS SENTIDOS) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-55A | INICIACIÓN DE SEPARADOR (UN SENTIDO) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-56 | TERMINACIÓN DE VÍA CON SEPARADOR (DOS SENTIDOS) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-56A | TERMINACIÓN DE VÍA CON SEPARADOR (UN SENTIDO) | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-57 | FINAL DEL PAVIMENTO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-57A | CAMBIO DE TEXTURA EN SUPERFICIE DE RODADURA | rombo | anaranjado / negro / negro+rojo | IV (VI si enrollable) | 614 |
| SPPO-59 | CICLISTAS EN LA VÍA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-59A | PROXIMIDAD A CRUCE DE CICLISTAS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-59B | UBICACIÓN DE CRUCE DE CICLISTAS | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-67 | RIESGO DE SINIESTRO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-69 | CURVA MUY CERRADA A LA IZQUIERDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-70 | CURVA MUY CERRADA A LA DERECHA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-71 | PROYECCIÓN DE GRAVILLA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-74 | DESNIVEL SEVERO | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-75 | DELINEADOR DE CURVA HORIZONTAL | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 614 |
| SPPO-76 | LONGITUD MÁXIMA PERMITIDA | rombo | anaranjado / negro / negro | IV (VI si enrollable) | 614 |

### SIO — Informativas de obra (24)

No existen SIO-01, SIO-04 ni SIO-06 en figuras ni en el Banco.

| Código | Nombre oficial | Forma | Colores | Lámina | Pág. |
| :--- | :--- | :--- | :--- | :--- | ---: |
| SIO-02 | INICIO DE OBRA | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 626 |
| SIO-03 | FIN DE OBRA | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 627 |
| SIO-05 | DESVÍO A XXX m | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 627 |
| SIO-07 | DESVÍO | flecha | anaranjado / negro / negro | IV (VI si enrollable) | 627 |
| SIO-08 | FIN DE DESVÍO | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 628 |
| SIO-09 | FINAL O CIERRE DE CARRIL DERECHO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 629 |
| SIO-10 | FINAL O CIERRE DE CARRIL DERECHO EN UNA CALZADA UNIDIRECCIONAL DE TRES CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 629 |
| SIO-11 | FINAL O CIERRE DE CARRIL IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 629 |
| SIO-12 | FINAL O CIERRE DE CARRIL IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE TRES CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 630 |
| SIO-13 | GENERACIÓN DE CARRIL DERECHO EN UNA CALZADA UNIDIRECCIONAL DE UN CARRIL | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 630 |
| SIO-14 | GENERACIÓN DE CARRIL DERECHO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 630 |
| SIO-15 | GENERACIÓN DE CARRIL IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE UN CARRIL | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 631 |
| SIO-16 | GENERACIÓN DE CARRIL IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 631 |
| SIO-17 | CAMBIO DE ALINEAMIENTO AL LADO DERECHO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 631 |
| SIO-18 | CAMBIO DE ALINEAMIENTO AL LADO DERECHO EN UNA CALZADA UNIDIRECCIONAL DE TRES CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 632 |
| SIO-19 | CAMBIO DE ALINEAMIENTO AL LADO IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 632 |
| SIO-20 | CAMBIO DE ALINEAMIENTO AL LADO IZQUIERDO EN UNA CALZADA UNIDIRECCIONAL DE TRES CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 632 |
| SIO-21 | CAMBIO DE ALINEAMIENTO AL LADO DERECHO EN UNA CALZADA BIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 633 |
| SIO-22 | CAMBIO DE ALINEAMIENTO AL LADO IZQUIERDO EN UNA CALZADA BIDIRECCIONAL DE DOS CARRILES | rectangulo vertical | anaranjado / negro / negro | IV (VI si enrollable) | 633 |
| SIO-23 | PARADERO TEMPORAL DE BUSES | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 633 |
| SIO-24 | SENDERO PEATONAL | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 634 |
| SIO-25 | SEMÁFORO APAGADO | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 634 |
| SIO-26 | CRUCE PEATONAL NO HABILITADO | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 634 |
| SIO-27 | SEÑAL RELACIONADA CON SITUACIONES DE OBRA | rectangulo horizontal | anaranjado / negro / negro | IV (VI si enrollable) | 635 |


---

## 6. Iconos

- Dibujados por `tools/generar_iconos.py` a partir del CSV: forma, colores del CSV y un pictograma
  esquemático. **No se ha extraído ninguna imagen del PDF.**
- **Código siempre visible** en la franja inferior del icono.
- **Pictograma sustituido por texto corto** donde el real es complejo: animales, paisajes y
  edificios de las turísticas (ST), casi todos los servicios (hospedaje, iglesia, taller…), maquinaria
  agrícola, tracción animal, cabalgaduras, gravilla, viento lateral, desnivel, puente levadizo, tsunami,
  y las SIO con leyenda. Los de flechas, cruces, curvas, estrechamientos, resaltos, vehículos, bicicleta,
  moto, peatón, P, H, cruz roja, avión, surtidor y teléfono sí llevan pictograma propio simplificado.
- SPPO usa el pictograma de la SP del mismo número, con fondo anaranjado.
- **Colores de pantalla** de la Tabla I-1 del Manual (p. 894). Tres tonos son propios y están marcados
  en el script: verde oscuro (el de la tabla, 85-186-94, no da contraste con letra blanca a 64 px),
  gris de símbolo y anaranjado fluorescente. **Son colores de pantalla, no coordenadas cromáticas:
  no sirven para aceptar ni rechazar una lámina.**
- Regenerar: `python 08_Senales/tools/generar_iconos.py` (reescribe `iconos/` e `indice.html`).

---

## 7. Qué significa para el retrorreflectómetro

### 7.1 Colores que hay que medir por familia

| Familia | Colores retrorreflectivos a medir | Página |
| :--- | :--- | :--- |
| SR, SRC, SRM, SRO | Blanco y rojo (fondo y orla). El negro no se mide (p. 39). Gris en SR-48, sin mínimo | 39, 59 |
| SP, SPC | Amarillo; **amarillo verde fluorescente** en SP-46 a 46C, 47 a 47B, 48, 49A, 59 a 59B (y SP-80 según la figura) | 103 |
| SI (dirección) y SIC, SIM, SIP-01 | Verde y blanco | 146 |
| SI (servicios), SIM-06 | Azul y blanco | 146, 182 |
| SI-01 a 03 (escudos) | Blanco | 146 |
| ST | **Marrón** y blanco | 146, 202 |
| SPO, SPPO, SIO | **Anaranjado** y **anaranjado fluorescente** (SPO-01, SPO-03, primera SIO) | 600, 604 |
| Eventos especiales | **Rosado fluorescente** | 790, 794 |
| SIT-01 a 04 | Ninguno: fotoluminiscentes, no se miden con retrorreflectómetro | 258-259 |

«Se debe garantizar el mismo nivel de retrorreflexión tanto en el fondo como en el texto, flechas y
pictogramas, excepto para el color negro» (p. 39): la leyenda también es un punto de medida.

### 7.2 Cobertura del firmware V3.6

El firmware tiene **seis colores**, cada uno con dos ecuaciones:

| Color | Intenso: pantalla / Bluetooth | Opaco: pantalla / Bluetooth |
| :--- | :--- | :--- |
| Blanco | 0x01 / `1` | 0x07 / `7` |
| Amarillo | 0x02 / `2` | 0x08 / `8` |
| Verde | 0x03 / `3` | 0x0a / `a` |
| Rojo | 0x04 / `4` | 0x0b / `b` |
| Azul | 0x05 / `5` | 0x0c / `c` |
| Naranja | 0x06 / `6` | 0x0d / `d` |

Fuente: `01_Firmware/RetroVertical_V3.6.X/ecuacionesCalibracion.c:73-128` (pantalla) y `:141-190`
(Bluetooth); las funciones, en `:3-42`. **El código no dice «tipo I»: dice `Intenso` y `Opaco`**, y
el comentario de `:103` llama «BAJO» al opaco. La correspondencia con el menú de la pantalla
(«OTROS PAPELES» → intensas, «PAPEL TIPO I» → opacas) está en
`05_Documentacion/PROCEDIMIENTO-Calibracion-V3-K42.md:125-132`. Ese mismo documento deja abierta la
correspondencia entre intenso/opaco y los tipos de los testigos (`:232-236`, `:298`).

Además, el código `0x0e` / `e` devuelve la lectura **sin ecuación** (`:128-130`, `:191-193`).

Cobertura frente al Manual:

| Color del Manual | Código en el equipo | Cubierto |
| :--- | :--- | :--- |
| Blanco | 1 / 7 | Sí |
| Amarillo | 2 / 8 | Sí |
| Verde | 3 / a | Sí |
| Rojo | 4 / b | Sí |
| Azul | 5 / c | Sí |
| Anaranjado | 6 / d | Sí |
| **Marrón / café** | — | **No.** Toda la familia ST (33 señales) queda sin ecuación |
| **Amarillo verde fluorescente** | — | **No.** 13 preventivas de peatones, escolares y ciclistas, más sus SPPO |
| **Anaranjado fluorescente** | — | **No.** SPO-01 TRABAJO EN LA VÍA, SPO-03, primera SIO de cada obra |
| **Rosado fluorescente** | — | **No**, y además el Manual no le da mínimo |
| **Amarillo fluorescente** | — | No. El Manual le da mínimo (Tablas 2-5 y 2-6) pero no lo asigna a ninguna familia |
| **Púrpura** | — | No. Tiene mínimo en las Tablas 2-5 y 2-6 y **ninguna señal del Manual lo usa** |
| Gris | — | No, y el Manual no le da mínimo |
| Negro | — | No procede (p. 39) |

Dos consecuencias que la app tendrá que resolver, **no** resueltas aquí:

- **Intenso/opaco frente a IV/XI.** El Manual solo exige y solo da mínimos para IV y XI. Que la
  ecuación «intensa» mida bien una lámina IV y una XI con la misma recta es una hipótesis: no está
  medido.
- **Medir con la ecuación de otro color no vale.** Un marrón o un fluorescente medido con el código
  de otro color da un número que no es R<sub>A</sub>. El código `e` (sin ecuación) sirve para
  registrar la lectura cruda, no para dictaminar.

---

## 8. Contradicciones abiertas

No se elige ninguna: se dejan anotadas hasta que alguien mida o consulte la fuente primaria.

### 8.1 Manual frente a NTC 4739:2011

| # | Manual 2024 | NTC 4739:2011 (transcripción) | Estado |
| :--- | :--- | :--- | :--- |
| C1 | Tablas 2-3 y 2-4 (p. 37-38), rotuladas «NTC 4739», traen **púrpura**; la 2-3 trae además **rosado fluorescente** con cinco pares de coordenadas | Tablas 11 y 13 (pág. 13 y 15): diez colores, **ni púrpura ni rosado** | Abierta. El Manual copia de una revisión posterior de ASTM D4956, no de la NTC que cita |
| C2 | Tablas 2-5 y 2-6 (p. 39-40), «ángulos… definidos en la Norma NTC 4739», traen columna **púrpura** | Tablas 5 y 10 (pág. 9 y 12): sin púrpura | Abierta |
| C3 | Tabla 2-6, tipo XI, **amarillo a 0,2° / +30° = 165** (p. 40) | Tabla 10: **465** impreso (pág. 12) | Abierta. La transcripción de la NTC ya lo marca como anomalía del impreso |
| C4 | Tabla 2-6: filas de **1,00°** de observación (p. 40) | Tabla 10: las dos últimas filas rotuladas **«0,1°»** (pág. 12) | Abierta. Mismos valores, distinto rótulo |
| C5 | **Rosado fluorescente** exigido para eventos especiales (p. 36, 790, 794) | La NTC no tiene ese color | Abierta: **no existe mínimo de R<sub>A</sub> en ninguna de las dos fuentes** |

C3 y C4 son las dos «anomalías» que la tabla de errores del `CLAUDE.md` del V5 recuerda: el Manual no
las «corrige», es otra fuente.

### 8.2 Dentro del propio Manual

| # | Qué | Dónde |
| :--- | :--- | :--- |
| M1 | Excepciones de color de las SP: p. 36 cita SP-23, 29, 33, 54; p. 103 añade **SP-57A** | p. 36 vs p. 103 |
| M2 | **SP-80 TRICIMÓVILES** aparece con fondo amarillo verde fluorescente en la figura y no está en la lista de fluorescentes | p. 102 vs p. 103. En el CSV va como fluorescente, por la figura |
| M3 | La lista de fluorescentes dice «SP-59, SP59, SP-59B»: falta SP-59A, que la figura pinta fluorescente | p. 103 vs p. 102 |
| M4 | SP-54 se llama «PASO A NIVEL» en la lista de colores y «CRUZ DE SAN ANDRÉS» en su titular | p. 103 vs p. 128 |
| M5 | **SIC-08 FIN DE CICLOBANDA** se cita en el texto y no tiene figura ni lámina en el Banco | p. 551. No va al CSV |
| M6 | **SPPO-47** está en la Figura 7-5 y no en el Banco de señales (que salta de SPPO-46C a SPPO-47A) | p. 613 vs p. 1312-1313. Va al CSV |
| M7 | Tabla I-1 llama «Purpura» a un color RGB 238-58-130, que es un rosa | p. 894 |
| M8 | «SCR-05» en el texto por SRC-05 | p. 531 |
| M9 | «tipo IV o … superior (aumentado por un factor de 3)» sin decir sobre qué se multiplica | p. 160 |

---

## 9. Qué queda fuera

- **Variantes con leyenda libre** de las informativas de dirección (SI-05 a SI-06, SI-26, SIC-02,
  SIC-12): van una vez, con un texto de ejemplo; el contenido real cambia en cada señal.
- **Placas adosadas** (distancia, horario, «SALIDA», etc.) y lamas: no son señales con código.
- **Paneles de servicios** que agrupan varios pictogramas (p. 184-185) y panel turístico (p. 202).
- **Señales de mensaje variable** (2.7) y **semáforos** (capítulo 4): no son láminas.
- **Delineadores, hitos, elementos de canalización** (capítulos 5 y 7): son retrorreflectivos y el
  Manual les exige tipo IV (p. 465, 636), pero no son señales verticales con código. Tabla 7-5 de
  canalización, p. 636, fuera.
- **Demarcación horizontal** (capítulo 3): otro equipo.
- **SIC-08**: citada sin dibujo (M5).
- **Dimensiones** por velocidad y el Banco de señales (Anexo 2, p. 892-1421): no se transcriben.
