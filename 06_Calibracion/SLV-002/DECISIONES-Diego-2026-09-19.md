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
