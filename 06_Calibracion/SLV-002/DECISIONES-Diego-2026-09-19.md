# Decisiones de Diego sobre la calibración de SLV-002

Registro de decisiones del propietario del proceso. La app 3.6.13 exige que una dispensa esté aquí
registrada, y no en una casilla del operador (P11 §3, QA-3612).

| ID | Decisión | Fecha y hora | Palabras de Diego |
| :--- | :--- | :--- | :--- |
| D-C | Códigos 1 y 2 de intensas: compromiso C (XI frente a IV/IX) | 19-sep-2026, mañana | "C, compromiso entre XI e IV/IX" |
| D-B2 | Código 2: recta anclada en oscuro (opción b) | 19-sep-2026 11:20 | "b" |
| D-B1 | Código 1: grado 1 aceptado pese a incumplir RF-CAL-14/15/16 | 19-sep-2026 11:20 | "acepto el blanco" |
| D-A5 | Criterio de reproducibilidad: el que sale de la A5 | 19-sep-2026 11:20 | "y el criterio de la A5" |
| **PA-24** | **Código b (rojo tipo I): se escribe con recta anclada en oscuro pese a incumplir RF-CAL-14 (P39 +15 %, P49 −10 %)** | **19-sep-2026 ~14:10** | "Sí, escribirla: mucho mejor que fábrica" |
| **PA-14** | **Código 5 (azul intenso): se ajusta con recta anclada en oscuro, con la regla de cobertura relajada para este caso** | **19-sep-2026 ~14:10** | "Sí, relajar la regla y ajustarlo con una recta que pase por el negro, como el amarillo [...] es ajustarlo, claro que sí" (confirmado ~14:15) |
| D-FW | Grabar firmware aunque se pierda la calibración de la EEPROM | 19-sep-2026 ~12:45 | "graba, todo se puede perder" / "no pasa nada en el firmware" |
| D-VENC | Calibración vencida: avisa y marca el registro, no bloquea | 19-sep-2026 | respuesta a la pregunta |
| D-SFT | La fuente del indicador es `D:\onedrive\gdrive\sft` | 19-sep-2026 | respuesta a la pregunta |
| **REMEDIDA-b** | La re-medida del código b se juzga con RF-CAL-18 (frente a la curva escrita), no frente al certificado | 19-sep-2026 ~15:40 | "ok si de 1 a 4" (punto 1) |
| **PA-24 ampliada** | La dispensa del b cubre también RF-CAL-15: RMS de tipo I previsto en 11,5 % (> 6 %) | 19-sep-2026 ~15:40 | "ok si de 1 a 4" (punto 2) |
| **PA-24 margen** | Los límites de P39 (+15 %) y P49 (−10 %) son **techo**, con ±3 puntos de margen: un valor mejor que el aceptado siempre pasa | 19-sep-2026 ~15:40 | "ok si de 1 a 4" (punto 3) |
| **P81** | P81 sale del ajuste del código 5 y queda solo como patrón de verificación y re-medida | 19-sep-2026 ~15:40 | "ok si de 1 a 4" (punto 4) |
| **SERIE** | SLV-002 queda para Concesionaria Vial Andina (este equipo). El equipo de Autopistas del Nordeste (v4.0), que también mostraba SLV-002 y tiene el certificado V2023 39, pasa a SLV-003-2026 | 19-sep-2026 ~16:10 | "SLV-003-2026" → Autopistas del Nordeste |
| **SERIE-2** | Coviandina (Concesionaria Vial Andina, V3.6, MAC 00:21:13:05:19:3B) pasa a **SLV-002-2026**; Nordeste, a SLV-003-2026. Sustituye a la fila SERIE en lo que toca a Coviandina. Se graba con `#SN` **cuando la app (3.6.15) sepa renombrar sin perder la campaña, el acta ni las decisiones atadas a "SLV-002"** | 19-sep-2026 ~16:15 | "EL SLV-002-2026 DE COVIANDINA EL ANTERIOR QUE SI ES V3 Y VA CON V3.6" |
| **PROTOCOLO-MIN** | Valores por defecto de la app: **1 colocación × 4 disparos** (más el asentamiento) en campaña y banco. A5 y OSCURO conservan K = 5, porque de ellos salen la s_rep y el ancla. K × M sigue configurable | 19-sep-2026 ~16:40 | "DEJA ESE VALOR DE DEPURACION A 4 Y 1 O LO MÍNIMO COMO VALORES POR DEFECTO EN LA APP" |
| **PROTOCOLO-AJUSTE** | Los patrones de AJUSTE de los códigos que se escriben (8, b, 5), P81 y las re-medidas se miden en "preciso", 5 × 4; el resto, 1 × 4 | 19-sep-2026 ~17:10 | "ok si a todo" |
| **RF-CAL-15-b** | El límite del RMS de tipo I del código b es 11,5 % estricto, sin el ±3 | 19-sep-2026 ~17:10 | "ok si a todo" |
| **TIPO-I-REPETIR** | P34, P37, P43, P44, P38, P39 y P49 se repiten en "preciso"; los demás tipo I medidos a las 15:10 (3 × 3) valen | 19-sep-2026 ~17:10 | "ok si a todo" |
| **PRECISO-5x9** | Las series con K ≥ 5 y M ≥ 4 (por ejemplo, las de 5 × 9 de la mañana) cuentan como "preciso" (`Protocolo.java:102`; efecto en la media ≤ 0,19 %, REVISION-P15) | 19-sep-2026, tarde | "sí, cuentan como preciso" |
| **FIRMA-ACTA** | **Con el PIN de admin basta para firmar**: no se anade ninguna fila `FRASE-DIEGO` a `decisiones.csv` ni ninguna barrera adicional. Lo que cambia es el texto: el acta deja de decir "firmado por Diego" y pasa a decir **Firmado por "<nombre>", ITVIAL SAS, <fecha de ese dia>**. El nombre es el del operador que calibra, no un literal cableado. Afecta a `FlujoCalibracion.java:1588,1593,1611`. Se aplica en la siguiente APK, para no invalidar la revision de la rc2 en curso | 19-sep-2026, noche | "si tienen pass de admin firman, ya no lo compliques" / "Firmado por \" nombre\" ITVIAl SAS + fecha de ese dia" |
| **APP-SIMPLE** | **La app de calibracion se reduce a: cargar el ZIP, calibrar, nombre y nota, y como mucho cambiar la serie. Poco mas.** No es una mejora de lo que hay: es otra version, mas corta. Motivo medido: la noche del 19-sep hicieron falta cuatro pantallas y una hora para escribir un codigo, porque el ZIP venia de un banco REPRESENTATIVO y la campana se abrio en COMPLETO, y la app avisa del desajuste pero deja al operador adivinar que hay que ir a "Tomar muestras" a cambiar la cola. Lo que Diego pide es que importar un ZIP deje la campana lista para calibrar, sin pasos intermedios | 19-sep-2026, noche | "no mejora, cambia esa a una version que sea cargar, y calibrar, nombre, nota y si algo si desea cambiar el nombre, poco mas" |
| **APPS-DPI** | Apps de calibración sólo de DPI; USB por proyecto; cierra D-10 (nota 1) | 21-sep-2026 | nota 1 |
| **TOMA-50** | La toma de un equipo no pasa de 50 tomas (nota 2) | 21-sep-2026 | nota 2 |
| **VERIF-5-10** | Verificación final: 10 patrones re-medidos y su desviación (nota 3) | 21-sep-2026 | nota 3 |
| **CERT-TITULO** | "Certificado de calibración", firmado por ITVIAL SAS (nota 4) | 21-sep-2026 | nota 4 |
| **FECHA-EQUIPO** | Una sola fecha en el equipo: la del día en que se calibra (nota 5) | 21-sep-2026 | nota 5 |
| **USR-SIMPLE** | App de usuario simple: señal por icono y medir (nota 6) | 21-sep-2026 | nota 6 |
| **SERIE-USR** | App de usuario: si el equipo no da la serie, la escribe el operador (nota 7) | 21-sep-2026 | nota 7 |
| **SENAL-FILTRO** | App de usuario: filtro por familia y búsqueda al escribir (nota 7) | 21-sep-2026 | nota 7 |
| **USR-ALCANCE** | App de usuario: sólo firmware V3.6; informe por señal (nota 8) | 21-sep-2026 | nota 8 |

Notas de las filas del 21-sep-2026:

1. **APPS-DPI.** Las apps de calibración son sólo de DPI: no se publican en ningún sitio y el APK
   se pasa por USB, uno por proyecto. El cliente recibe el equipo calibrado y el PDF. Diego: "calibrar
   es fecha, operador o nombre y exportar el pdf de calibración"; "como son apps propias de
   calibración, sólo las tiene la empresa"; "la app no se publica en ningún lugar"; "se pasa en una
   usb por proyecto".
2. **TOMA-50.** Diego: "no es necesario el banco completo, muchos colores están en el mismo margen y
   no aportan, te sirven para la curva o fórmula"; "nuestro firmware puede mejorar esa fórmula, desde
   que te cuadre en las medidas"; y "el .zip construir no más de 50 tomas,
   112 es exagerado, demora más de 1 hora tomando medidas".
   ▸ Propuesta, sin confirmar: 1 colocación por referencia. Si anula PROTOCOLO-AJUSTE y TIPO-I-REPETIR
   está pendiente (D-16).
3. **VERIF-5-10.** Tras calibrar se re-miden 10 patrones ("10 ok, es para validar lo que llevas");
   el certificado da el valor medido y el error % de cada uno. Margen: Diego acepta los dos de 2018
   (±5 % y ±10 %): "pues ambos, al final
   da igual, es sacar el valor". Cierra D-14.
   ▸ Propuesta, sin confirmar: se imprime el error; conforme hasta ±10 %, con marca por encima de ±5 %.
4. **CERT-TITULO.** "Certificado de calibración", tal cual; firma ITVIAL SAS. Diego: "Certificado de
   calibración tal cual sí", "ITVIAL SAS firma". Sustituye la propuesta de RF-CAL-30 (P-CAL-02) y
   cierra D-15; la SPEC se corrige al escribir la de Calibra.
5. **FECHA-EQUIPO.** `#SC` graba la fecha de la última calibración en la EEPROM (`PROTOCOLO-V3.6.md:51-52`);
   el historial de año en año son los ZIP de cada calibración, que se archivan, y cargarlos es opcional.
   Diego: "la fecha se guarda en el firmware, no? el equipo la captura así como el nombre"; "sería
   sacar un .zip de calibración [...] y la cargo o no, optativo"; "a la fecha de hoy o de cuando se le
   dé al botón calibrar, ese día se genera el acta y sale". Opción (b) de la revisión app-firmware.
   ▸ Propuesta, sin confirmar: la re-medida tras escribir cada código se juzga como la verificación
   (VERIF-5-10): error frente al certificado dentro de ±10 %, en lugar del criterio de s_rep.
6. **USR-SIMPLE.** La app de usuario se queda en lo mínimo: conectar, elegir la señal por su icono y
   código del catálogo, medir, guardar y exportar. Diego: "por favor simple, esa app tiene múltiples
   funcionalidades que sólo las entiende el que hizo el firmware, no un funcional". Cierra C-USR-01.
7. **SERIE-USR y SENAL-FILTRO.** Diego: "la escribe el funcional"; "deben existir según el manual de
   señalización un filtro, verticales, preventivas, reglamentarias, luego al escribir SI... el filtro ya
   me recoge las que queden, hacerle la vida fácil al funcional". La serie tecleada va marcada
   "declarada, no leída del equipo".
8. **USR-ALCANCE.** Diego: "3.6 trabaja sólo para este firmware, encontramos que cada firmware varía en
   códigos [...] es apk por cliente en calibración y en apk de usuario final"; "sólo lo que indique el
   manual de señalización vial, los tipo I es un tema interno"; la curva debe "permitir en lo posible
   sacar valores bajos [...] no decir null a todo sino indicar que no cumple con una medida estándar";
   "se miden sólo el papel retrorreflectivo, lo otro es parte de mantenimiento"; el informe lleva
   "señal, ubicación, estado de la señal [...] si está limpia, en buen estado", y "algunas señales
   tienen un serial detrás para identificar la medida con el identificador de esa señal en campo".
   La V4.6 es otra app y otro cliente; lo que se salve de aquí pasa por el Orquestador.
