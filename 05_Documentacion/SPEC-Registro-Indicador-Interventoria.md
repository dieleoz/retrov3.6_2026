# SPEC — Registro de medidas periódicas e indicador de señalización vertical para la interventoría

**Nada de este documento está medido ni validado con una interventoría.** Sale de leer
especificaciones, normas transcritas y código. El umbral de cumplimiento **no está decidido**: las
fuentes dan cuatro criterios distintos (sección 1.4, C-01) y la geometría del equipo no está
documentada (C-06), así que hoy **ningún dictamen "cumple / no cumple" es defendible** sin que Diego
elija el criterio del contrato.

| Campo | Valor |
| :--- | :--- |
| Alcance | Sólo retrorreflectómetro **Vertical**. Lo horizontal aparece sólo para entender el indicador |
| Aplica a | V3.6 (este repositorio) y V4.6 (`D:\IT\P_RetroVertical_V4.6`), igual |
| Relacionados | [`SPEC-Calibracion-V3.6.md`](SPEC-Calibracion-V3.6.md) (en curso, otro agente), [`PROTOCOLO-V3.6.md`](PROTOCOLO-V3.6.md), `08_Senales/` (catálogo, aún no existe) |
| Fecha | 19-sep-2026 |
| Qué **no** decide | La tecnología de la app (nativa, híbrida o web): la estudia otro agente. Aquí se dice **qué** hace, no **con qué** |

---

## 0. Fuentes, y un aviso sobre SFT

### 0.1 El repositorio `github.com/dieleoz/SFT` está vacío

Comprobado dos veces, con dos métodos:

| Prueba | Resultado |
| :--- | :--- |
| `git clone https://github.com/dieleoz/SFT.git` | «You appear to have cloned an empty repository», código 0 |
| `git ls-remote https://github.com/dieleoz/SFT.git` | Ninguna referencia (ni rama ni etiqueta), código 0 |
| Control: `git ls-remote` a un repositorio inexistente de la misma cuenta | «Repository not found» |
| API pública sin autenticar (`api.github.com/repos/dieleoz/SFT`) | 404: es privado |

Conclusión: el repositorio **existe, hay acceso de lectura y no tiene ningún commit**. No hay nada que
citar de él. `gh` no está instalado en esta máquina y no se usó ningún token.

### 0.2 Lo que se usó en su lugar: la carpeta local `D:\onedrive\gdrive\sft`

36 ficheros `.md` fechados el 20-dic-2025 (proyecto «ControlTraffic 360 — SICC, Sistema Informático
de Contabilización y Control», cliente ANI; `RESUMEN_REQUERIMIENTOS_INDICADORES.md:2,17`).
**Decidido por Diego (19-sep-2026, PA-08): la fuente de SFT es esta carpeta.** El repositorio de
GitHub está vacío. En este documento, «SFT» significa esa carpeta.

Ficheros usados (rutas relativas a `D:\onedrive\gdrive\sft\`):

- `REQ_IND_E11_SenalizacionVertical.md` — indicador E11 (vertical).
- `REQ_IND_E12_SenalizacionHorizontal.md` — indicador E12 (horizontal), sólo como contraste.
- `RESUMEN_REQUERIMIENTOS_INDICADORES.md` — modelo general de inspección e interventoría.
- `MAPA_INDICADORES_COMPLETO.md` — frecuencias.
- `REQ_IND_E1_IndiceRugosidad.md` — único que pide equipo y certificado de calibración en el formulario.

### 0.3 Fuentes normativas, todas ya transcritas en el repositorio del V5

Rutas relativas a `D:\@Proyect\IT\P_RetroReflectometro_Vertical\04_Manuales\`:

- `Manual-Senalizacion-Vial-Extracto.md` — Manual de Señalización Vial 2024 (ANSV).
- `NTC-4739-Requisitos.md` — NTC 4739:2011, tablas de lámina nueva.
- `NTC-4739-Estado-Actual.md` — vigencia, apéndices técnicos de la ANI, ASTM E1709.
- `Instrumentos-y-Metodo-Campo.md` — IMT, Publicación Técnica 853 (México, 2024), método de campo.

Las citas a normas que no se han leído en su original llevan la ruta de la transcripción, no la
página del original, salvo cuando la transcripción da la página.

---

## 1. El indicador

### 1.1 Lo que define SFT (indicador E11, «Estado de Señalización Vertical»)

| Dato | Valor en SFT | Cita |
| :--- | :--- | :--- |
| Código y nombre | REQ-IND-E11, «Estado de Señalización Vertical» | `REQ_IND_E11_SenalizacionVertical.md:5-6` |
| Fuente legal que declara | «Apéndice Técnico 4, Manual de Señalización Vial (Resolución 1050/2004 MinTransporte)» | `:9` |
| Objetivo | 100 % de señales visibles, legibles y en posición correcta | `:17` |
| Visibilidad nocturna | Retrorreflectividad (cd/lx/m²) **> 70 % del valor original** | `:23` |
| Legibilidad | Sin decoloración > 30 % | `:24` |
| Posición | Desviación < 10° | `:25` |
| Daños | Sin perforaciones ni dobleces | `:26` |
| Frecuencia | **Mensual** | `:29`; también `MAPA_INDICADORES_COMPLETO.md:130` |
| Aceptación | **≥ 95 % de señales funcionales**; **0 tolerancia en zonas críticas** (curvas peligrosas, intersecciones) | `:32-33` |
| Tiempo de corrección | Caída o ilegible: 48 h; reemplazo por desgaste: 15 días | `:36-37` |
| Clasificación por señal | EXCELENTE / BUENA / REGULAR / MALA / CRÍTICA, en función del % de retrorreflectividad, la legibilidad (1-10) y el daño | `:64-72` |
| Cumplimiento por señal | `is_compliant` = condición EXCELENTE o BUENA (≥ 70 % y legibilidad ≥ 7) | `:74-76` |
| Requisito de app | «App Móvil con Retrorreflectómetro»: captura por Bluetooth, comparación automática con valor de diseño, alerta si < 70 % | `:90-93` |
| Inventario | Con fechas de instalación, predicción de vida útil | `:95-97` |

**Unidad de agregación.** SFT no define muestreo por tramo: el objetivo es el 100 % de las señales
(`:17`) y el indicador es el porcentaje de señales funcionales (`:32`). Cada medida lleva `pr_km` y
`inspection_id` (`:49,52`); la inspección lleva el tramo (`RESUMEN_REQUERIMIENTOS_INDICADORES.md:125,387`).
La agregación natural es, por tanto, **por inspección = tramo y fecha**, y dentro de ella por señal.

**Lecturas.** SFT guarda **un solo valor** por señal (`retroreflectivity_measurement numeric(5,1)`,
`:58`) y un porcentaje (`:59`). No distingue colores ni número de lecturas.

**Equipo.** La tabla de E11 **no tiene campo de equipo ni de calibración** (`:47-83`). La de E12 sí
(`measurement_equipment varchar(50)`, `REQ_IND_E12_SenalizacionHorizontal.md:103`), y el formulario
de E1 pide «Equipo utilizado» y «Certificado de calibración (file upload + text reference)»
(`REQ_IND_E1_IndiceRugosidad.md:113-114`).

### 1.2 Lo que SFT exige para la interventoría (común a todos los indicadores)

| Dato | Cita (`RESUMEN_REQUERIMIENTOS_INDICADORES.md`) |
| :--- | :--- |
| La evaluación de interventoría es **obligatoria** para cada indicador y **prevalece** sobre la del concesionario si discrepan | `:115,117` |
| Tipos de inspección: Periódica, Auto-evaluación, Evaluación (interventoría) | `:128,390` |
| Datos de la inspección: tramo, fecha de inspección, fecha de verificación, tipo | `:125-128` |
| Datos por punto: PR-KM, calzada (Derecha, Izquierda, Separador central, Bidireccional, Par vial), carril, observaciones | `:137-141,398-403` |
| PR-KM debe estar dentro del tramo | `:143-145` |
| Evidencias: PDF, JPG, fotografía o vídeo, **evidencia de equipos de medición y calibración** | `:148-152` |
| **Criterios obligatorios de la evaluación de interventoría:** fecha de aplicación del método, **equipos utilizados y calibración**, **personal encargado**, **número de pruebas realizadas**, sectores de la Unidad Funcional con abscisado, registro fotográfico o vídeo, resultado (Finalizado / En corrección) | `:155-162` |
| Estados de la inspección: EN_INSPECCION, EN_REVISION, FINALIZADO, EN_CORRECCION, ANULADO | `:391` |
| Anular en lugar de borrar (HU003) | `:290-292` |

**SFT no trae ninguna plantilla de formato** (ni XLSX ni PDF) para E11. Lo más parecido a una
plantilla es el esquema de la tabla `e11_vertical_signs_measurements` (`REQ_IND_E11:47-83`) y las
entidades `Inspección` y `Estado_Inspeccion` (`RESUMEN:385-404`). Se reproducen campo a campo en la
sección 4.4.

### 1.3 Lo horizontal, sólo para entender el indicador (E12)

- Umbrales **absolutos** en mcd/lx/m²: borde 150, carril 100, cebra 200; tachas 95 % operativas
  (`REQ_IND_E12:38-41`). Criterio agregado: ≥ 85 % de las demarcaciones (`:43`).
- Frecuencia: mensual visual y **semestral con equipo** (`:32`).
- Guarda el equipo de medida (`:103`) y calcula el cumplimiento en la base (`:84-96`).

Lo que enseña: en SFT el horizontal usa **mínimos absolutos por tipo** y el vertical un **porcentaje
del valor original**. Son dos lógicas distintas para el mismo tipo de medida; el vertical no puede
calcularse sin guardar el valor de referencia de cada señal (RF-REG-09).

### 1.4 Lo que dice la norma, y dónde choca con SFT

| Fuente | Criterio de aceptación | Cita |
| :--- | :--- | :--- |
| **Manual de Señalización Vial 2024** (Res. 20243040045005) | Reemplazar la señal «en el momento que la retrorreflectividad alcance los niveles mínimos» de la **Tabla 2-5 (lámina tipo IV)** o **2-6 (tipo XI)**: valores absolutos por color y geometría | `Manual-Senalizacion-Vial-Extracto.md:223-249,427` (Manual p. 39-40) |
| Manual 2024 — qué lámina exige | Tipo IV caso general; tipo XI a la izquierda en calzadas de 2+ carriles por sentido, unidireccionales de 3+, transporte público, elevadas, SR-01 y SR-02 | `Manual-Senalizacion-Vial-Extracto.md:284-289` (p. 39) |
| Manual 2024 — SR-01 PARE | Medir periódicamente para programar el reemplazo | `:270-274,431` (p. 63) |
| Manual 2024 — mantenimiento | Medir durante limpieza, para decidir entre limpieza y reposición | `:412,428` (p. 54) |
| Manual 2024 — registro | Inventario, base consultable, etiqueta en el dorso con fecha de instalación y **material retrorreflectivo** | `:433,444-448` (p. 24, 47) |
| **ANI, Apéndice Técnico 4**, E11 | NTC 4739; frecuencia **mensual/semestral**; unidad: cada señal; «Retroflectividad **80 % del valor tomado de la instalación**»; corrección **1 semana** (retro) y 48 h (ilegible) | `NTC-4739-Estado-Actual.md:254-261` |
| **ANI, Apéndice Técnico 2**, §6.3.2.1 | **80 % de la reflectividad inicial exigida por INVIAS 2004** y NTC 4739; reposición inmediata | `NTC-4739-Estado-Actual.md:263-268` |
| Otros apéndices ANI | «≥ 80 % de los valores requeridos en la norma» (no descargado) | `NTC-4739-Estado-Actual.md:270-272` |
| NTC 4739 | Norma de **producto** (lámina nueva); ASTM D4956 §1.2 excluye los mínimos en servicio | `NTC-4739-Estado-Actual.md:16-18,213-215` |
| ASTM E1709 | Portátil a **0,2° de observación**; entrada −4° según fuentes secundarias; no aplica si la geometría del instrumento difiere | `NTC-4739-Estado-Actual.md:307-334` |
| IMT PT-853 (México, referencial, sin fuerza en Colombia) | **Cuatro lecturas por color**, promedio; nunca el negro; **certificado de calibración con fecha inferior a un año**; modelo y número de serie del equipo | `Instrumentos-y-Metodo-Campo.md:256-264,283-286,304-313` |

### 1.5 Contradicciones abiertas (no se elige; se cierran con Diego o midiendo)

| ID | Contradicción | Fuentes |
| :--- | :--- | :--- |
| **C-01** | **Umbral.** SFT: > 70 % del valor original. AT4: 80 % del valor de instalación. AT2: 80 % de lo exigido por INVIAS 2004. Manual 2024: 100 % de la tabla absoluta. Sobre la misma señal dan umbrales distintos | `REQ_IND_E11:23`; `NTC-4739-Estado-Actual.md:254-272`; `Manual-…-Extracto.md:427` |
| **C-02** | **Manual citado.** SFT cita el Manual de 2004 (Res. 1050); el vigente es el de 2024 (Res. 20243040045005) | `REQ_IND_E11:9`; `NTC-4739-Estado-Actual.md:19-20` |
| **C-03** | **Frecuencia.** SFT: mensual. AT4: «mensual/semestral» | `REQ_IND_E11:29`; `NTC-4739-Estado-Actual.md:255` |
| **C-04** | **Corrección por desgaste.** SFT: 15 días. AT4: 1 semana | `REQ_IND_E11:37`; `NTC-4739-Estado-Actual.md:258-259` |
| **C-05** | **Lecturas.** SFT guarda un valor por señal. Manual (tablas por color) y PT-853 (cuatro por color) exigen valor por color | `REQ_IND_E11:58`; `Instrumentos-y-Metodo-Campo.md:311-313` |
| **C-06** | **Geometría del equipo no documentada.** Sin ella no se sabe con qué fila de la tabla comparar ni si aplica E1709 | `P_RetroReflectometro_Vertical\ESTADO.md:55-56` |
| **C-07** | **Manual 2024 y NTC 4739:2011 no coinciden** (una celda: amarillo XI 0,2°/+30°, 165 contra 465); el Manual copia de una revisión posterior de D4956 | `P_RetroReflectometro_Vertical\ESTADO.md:50-52` |
| **C-08** | **Rango de PR en SFT.** `pr_km numeric(5,3)` admite hasta 99,999 km, pero el ejemplo del propio SFT es un tramo K106+631–K122+380. E1, E10 y E16 usan `numeric(8,3)` o `(8,5)` | `REQ_IND_E11:52`; `RESUMEN:145`; `REQ_IND_E10:64`, `REQ_IND_E16:68`, `REQ_IND_E1:60` |
| **C-09** | **Condición interna de SFT.** `:23` pone el límite en «> 70 %»; la fórmula de `:67` acepta «≥ 70». Y una señal con 90 % y legibilidad 5 queda REGULAR (no cumple) aunque su retrorreflexión sea buena | `REQ_IND_E11:23,66-68` |
| **C-10** | **Colores.** Las tablas del Manual incluyen púrpura, marrón y fluorescentes; el protocolo del equipo admite seis colores | `Manual-…-Extracto.md:227`; CLAUDE.md del V5 §5 |

**Consecuencia de diseño:** el criterio de umbral **no se codifica como constante**. Es un parámetro
del proyecto, con su fuente escrita en cada exportación (RF-REG-09). Esta es también la conclusión de
`NTC-4739-Estado-Actual.md:278-280`.

---

## 2. Modelo de datos de las medidas periódicas

Leyenda. **Oblig.:** O = obligatorio; C = condicional; Op = opcional. **Origen:** U = usuario;
G = GPS del teléfono; E = equipo por Bluetooth; T = teléfono (reloj, identificador); K = catálogo
(`08_Senales/`); P = parámetro del proyecto; X = calculado por la app. **Clase:** N = núcleo
(imprescindible para la interventoría); M = mejora (comodidad).

Los tipos son lógicos (texto, entero, decimal, fecha-hora ISO 8601 con zona, booleano, UUID). No se
fija motor de base de datos.

### 2.1 Entidades y relaciones

```
Proyecto (contrato) 1─* Vía 1─* Tramo
Vía 1─* Señal (inventario, con historial)       Equipo 1─* Calibración
Proyecto 1─* Campaña (periódica) 1─* Visita ─* Lectura
                                  Visita *─1 Señal
                                  Visita *─1 Equipo, *─1 Calibración vigente ese día
Diario (append-only, cadena de hashes) ← todo lo anterior
```

### 2.2 Proyecto

| Campo | Tipo | Oblig. | Origen | Clase | Nota |
| :--- | :--- | :---: | :---: | :---: | :--- |
| proyecto_id | UUID | O | X | N | |
| nombre, contrato, entidad contratante | texto | O | U | N | Manual p. 47 pide contrato en la etiqueta |
| criterio_umbral | enum `TABLA_MANUAL_2024`, `PCT_VALOR_INSTALACION`, `PCT_VALOR_NORMA`, `PCT_VALOR_ORIGINAL_SFT` | O | P | N | C-01 |
| porcentaje_umbral | decimal | C | P | N | Obligatorio si el criterio es un porcentaje (80 AT4/AT2, 70 SFT) |
| fuente_umbral | texto | O | P | N | Literal que va al informe: «AT4 E11, 80 % del valor de instalación» |
| geometria_comparada | texto | O | P | N | Fila de la tabla contra la que se compara (C-06). Mientras no se mida, «no declarada» y todo dictamen sale como **no dictaminable** |
| lecturas_por_color | entero ≥ 1 | O | P | N | Por defecto 4 (PT-853) |
| criterio_agregado | decimal | O | P | N | 95 % en SFT (`REQ_IND_E11:32`) |
| responsable | texto | O | U | N | Ya existe en la app actual |

### 2.3 Vía y tramo

| Campo | Tipo | Oblig. | Origen | Clase |
| :--- | :--- | :---: | :---: | :---: |
| via_id | UUID | O | X | N |
| codigo_via, nombre_via | texto | O | U | N |
| unidad_funcional | texto | Op | U | N (SFT la usa como sector, `RESUMEN:160`) |
| tramo_id | UUID | O | X | N |
| pr_inicio, pr_fin | texto `K106+631` y decimal en km | O | U | N |

La PR se guarda **como texto tal como se escribe en la vía y como decimal en km**, sin límite de
99,999 (C-08).

### 2.4 Señal (inventario)

| Campo | Tipo | Oblig. | Origen | Clase | Nota |
| :--- | :--- | :---: | :---: | :---: | :--- |
| senal_id | UUID | O | X | N | Identidad propia; nunca se reutiliza |
| etiqueta_campo | texto | Op | U | N | Código pintado o de activo, si existe (Manual p. 47) |
| codigo_manual | texto | O | K/U | N | `SR-01`, `SP-26`… del Manual 2024. Si `08_Senales/` no existe aún, texto libre validado por patrón |
| familia | enum `SR`, `SP`, `SI`, `ST`, otra | O | K | N | Agrupa la exportación. SFT usa REGLAMENTARIA/PREVENTIVA/INFORMATIVA/TRANSITORIA (`REQ_IND_E11:44`) |
| color_fondo, colores_leyenda | lista de colores | O | K/U | N | El negro se marca **no retrorreflectivo** y no se mide |
| tipo_lamina | enum I-XI | O | U | N | Decide la tabla (Manual p. 39). Sin él, «no dictaminable» |
| lamina_exigida | enum IV, XI | C | X/U | N | Por ubicación (izquierda, elevada, SR-01/02…) |
| ubicacion_critica | booleano | O | U | N | 0 tolerancia (`REQ_IND_E11:33,79`) |
| lat, lon | decimal | O | G | N | |
| precision_gps_m | decimal | O | G | N | La que declare el teléfono |
| pr | texto + decimal | O | U | N | |
| sentido | texto | O | U | N | Hacia qué población o abscisa creciente/decreciente |
| lado | enum derecha, izquierda, elevada, separador | O | U | N | |
| calzada | enum de SFT | O | U | N | `RESUMEN:138` |
| fecha_instalacion | fecha | C | U | N | Obligatoria con `PCT_VALOR_INSTALACION` |
| valor_referencia_por_color | decimal por color | C | U/X | N | Base del % (SFT y AT4). Si no se conoce, «no dictaminable» con criterios por porcentaje |
| foto | fichero + SHA-256 | Op | U | N | |
| estado | enum ACTIVA, RETIRADA, REEMPLAZADA | O | X | N | Nunca se borra |
| reemplaza_a | UUID | C | U | N | Historial al reponer una señal |
| alta_fecha, alta_operador, baja_fecha, baja_motivo | | C | T/U | N | |

### 2.5 Campaña (inspección periódica)

| Campo | Tipo | Oblig. | Origen | Clase |
| :--- | :--- | :---: | :---: | :---: |
| campana_id | UUID | O | X | N |
| proyecto, vía, tramo | UUID | O | U | N |
| tipo_inspeccion | enum PERIODICA, AUTOEVALUACION, EVALUACION | O | U | N (`RESUMEN:390`) |
| periodo | texto `2026-09` | O | U | N |
| fecha_inicio, fecha_cierre | fecha-hora | O | T | N |
| operadores | lista de texto | O | U | N («personal encargado», `RESUMEN:158`) |
| estado | enum ABIERTA, CERRADA, ANULADA | O | X | N |

### 2.6 Visita (una señal medida en una campaña)

| Campo | Tipo | Oblig. | Origen | Clase | Nota |
| :--- | :--- | :---: | :---: | :---: | :--- |
| visita_id | UUID | O | X | N | |
| campana_id, senal_id | UUID | O | X/U | N | |
| fecha_hora | ISO 8601 con zona | O | T | N | La app actual usa `Date.toString()` (`medicion.java:1562`) |
| operador | texto | O | U | N | |
| lat, lon, precision_gps_m | decimal | O | G | N | Posición al medir, además de la del inventario |
| distancia_a_inventario_m | decimal | O | X | N | Si supera el radio, aviso |
| estado_limpieza | enum SIN_LIMPIAR, LIMPIA, AMBAS | O | U | N | Manual p. 54 |
| legibilidad | entero 1-10 | Op | U | M | SFT `:60` |
| dano_fisico | booleano | Op | U | M | SFT `:61` |
| desviacion_vertical_grados | entero | Op | U | M | SFT `:62` |
| temperatura, humedad | decimal | Op | E/U | M | PT-853 p. 45 |
| equipo_id, calibracion_id | UUID | O | X | N | Sección 3 |
| calibracion_vencida | booleano | O | X | N | RF-REG-03 |
| observaciones, fotos | texto, ficheros + SHA-256 | Op | U | N | |

### 2.7 Lectura (cada disparo)

| Campo | Tipo | Oblig. | Origen | Clase | Nota |
| :--- | :--- | :---: | :---: | :---: | :--- |
| lectura_id | UUID | O | X | N | |
| visita_id | UUID | O | X | N | |
| color | enum del protocolo | O | U | N | |
| indice | entero 1..n | O | X | N | |
| valor | decimal | O | E | N | cd·lx⁻¹·m⁻² tal como lo entrega el equipo |
| trama_cruda | texto | O | E | N | Petición y respuesta literales, para auditar |
| fecha_hora | ISO 8601 | O | T | N | |
| descartada, motivo | booleano, texto | C | U | N | Zona dañada (PT-853 p. 53-54). No se borra: se marca |

### 2.8 Resultado por color y por señal (calculado, nunca editable)

| Campo | Cálculo | Clase |
| :--- | :--- | :---: |
| n_lecturas | Lecturas no descartadas del color | N |
| media, minimo, maximo | Sobre las no descartadas | N |
| umbral_aplicado | Según `criterio_umbral`, tabla (IV o XI), color y `geometria_comparada` | N |
| fuente_umbral | Texto del proyecto + fila de tabla | N |
| cumple_color | `media ≥ umbral` (PT-853: se evalúa el promedio, `Instrumentos-y-Metodo-Campo.md:312-313`). Si `n < lecturas_por_color`, falta de lecturas | N |
| dictamen_color | CUMPLE, NO_CUMPLE, NO_DICTAMINABLE (sin lámina, sin geometría, sin referencia, color sin fila en la tabla o no medible por el equipo), INCOMPLETO | N |
| dictamen_senal | NO_CUMPLE si algún color retrorreflectivo no cumple; CUMPLE si todos cumplen; NO_DICTAMINABLE en otro caso | N |
| condicion_sft | Fórmula de `REQ_IND_E11:64-72`, sólo si hay legibilidad y daño | M |

---

## 3. Identificación y calibración del equipo

### 3.1 Qué lleva todo registro exportado

| Campo | Origen | Estado hoy |
| :--- | :--- | :--- |
| **MAC Bluetooth** | El teléfono la conoce al emparejar. La app V3.6 ya la escribe en la cabecera del registro de sesión (`03_App_Movil/RetroV36/.../Registro.java:26-27`) | Disponible |
| **Nombre Bluetooth** | Idem | Disponible |
| **Número de serie** | Ver 3.2: el firmware **no lo transmite** | **Manual**: se asocia a la MAC en el alta del equipo, desde el acta de `06_Calibracion/<serie>/` |
| **Firmware** | `#V#` → `#V,3.6,<fecha de compilación>,<CAL\|DEF>,<máscara>#` (`calibracion_v36.c:525-532`) | Disponible. La 3.6 y la 3.6.1 responden las dos `3.6`; sólo las separa la fecha de compilación (`PROTOCOLO-V3.6.md`, tabla de trazabilidad de `#V#`). Se guarda la respuesta **entera** |
| **Fecha de calibración** | Certificado que emite la app en modo superadministrador ([`SPEC-Calibracion-V3.6.md`](SPEC-Calibracion-V3.6.md)) | No existe aún |
| **Fecha de vencimiento** | **Fecha de calibración + 1 año** (fecha de emisión del certificado) | Calculada |
| **Identificador y SHA-256 del certificado** | Del PDF o del paquete del certificado | No existe aún |
| **Huella de los coeficientes** | SHA-256 de las respuestas de `#G` de los 12 códigos y `#GT#` leídas al conectar | Disponible por protocolo |

### 3.2 Dónde vive hoy cada dato en el equipo (V3.6)

- **La fecha de `#V#` es la de compilación, no la de calibración.** Sale de `__DATE__`
  (`calibracion_v36.c:457-475`); lo dice el propio protocolo (`PROTOCOLO-V3.6.md`, decisión O-07).
- **Mapa de EEPROM** (1024 bytes: el driver enmascara la dirección a 10 bits,
  `mcc_generated_files/memory.c:176-177`):

  | Rango | Contenido | Cita |
  | :--- | :--- | :--- |
  | 0x000-0x049 (0-73) | Bloque heredado: modelo, propietario (16), **serie (16, en 18)**, geometría, patrones, offsets, batería | `eeprom_manager.h:9-23` |
  | 0x04A-0x0FF | **Libre** | — |
  | 0x100-0x103 | Cabecera `V36` + versión | `calibracion_v36.c:56,64-65` |
  | 0x104-0x1ED | 13 registros de 18 bytes (12 códigos + temperatura/PIN), CRC-16 cada uno | `calibracion_v36.c:57-58,66-68` |
  | 0x1EE-0x3FF | **Libre** (530 bytes) | — |

  Nota: `PROTOCOLO-V3.6.md` §4 dice que el bloque heredado ocupa 0-52 citando `eeprom_manager.h:9-19`;
  las líneas 20-23 lo extienden hasta el byte 73. No hay solape con 0x100, pero la cifra está corta.
- **Número de serie.** Hay 16 bytes en EEPROM (`eeprom_manager.h:11`) y funciones `readSerial` y
  `writeSerial` (`eeprom_manager.c:98,118`), pero **`writeSerial` no tiene ninguna llamada** en el
  fuente y la pantalla de serie **imprime una constante `SLH-046`**, no lo leído
  (`gui.c:189,193,198`). Ningún comando Bluetooth devuelve la serie. Hoy la serie **no se puede
  obtener del equipo**.
- **No hay campo de fecha de calibración ni de identificador de certificado** en ninguno de los dos
  bloques.

### 3.3 Propuesta

**Opción A — sin tocar el firmware (núcleo).** La fecha vive en el **certificado** que genera la app
y en la base local de la app, atada al equipo por tres cosas: MAC, serie (del acta) y **huella de los
coeficientes**. Al conectar, la app lee `#V#`, `#G` ×12 y `#GT#`, calcula la huella y la compara con
la del certificado:

- coincide → calibración identificada, con su fecha y su vencimiento;
- no coincide → «**coeficientes del equipo distintos del certificado**»: el equipo se recalibró fuera
  de este certificado o se le repusieron los de fábrica (`#F`, `#FT#`). La medida queda marcada igual
  que con calibración vencida;
- `#V#` responde `DEF` → «equipo con coeficientes de fábrica, sin calibración».

Esto funciona hoy con el protocolo 1.1 y da algo que una fecha en EEPROM no da: **prueba de que los
coeficientes con que se midió son los certificados**. Su límite: si se reinstala la app o se cambia de
teléfono, el certificado hay que importarlo (va en el paquete de exportación, RF-REG-14).

**Opción B — fecha y serie en la EEPROM. Decidida por Diego el 19-sep-2026 (PA-07): entra en el
firmware 3.6.2, en preparación.** Comandos previstos: `#SC`/`#GC` para la fecha de calibración y
`#SN`/`#GN` para la serie; los nombres y formatos definitivos los fija `PROTOCOLO-V3.6.md` al
entregarse el firmware, y mandan sobre lo que sigue. La app calcula el vencimiento (+1 año) a partir
de la fecha leída del equipo y lo contrasta con el certificado; la huella de `#G`×12 + `#GT#` de la
opción A **sigue valiendo como control cruzado**. Propuesta de formato original, a título indicativo: Un registro nuevo en **0x1EE**
con el mismo formato que los demás (16 bytes + CRC-16/CCITT-FALSE): fecha de calibración `AAAAMMDD`
(4 bytes BCD), primeros 8 bytes del SHA-256 del certificado, 4 reservados. Dos comandos nuevos:
`#SC,<AAAAMMDD>,<id16hex>#` (modo administrador, como `#S`) y `#GC#` (libre). **No se cambia el
formato de `#V#`**: los clientes ya lo parsean. Ventaja: la fecha viaja con el equipo a cualquier
teléfono. Exige grabar el 3.6.2 por ICSP en cada equipo. Conviene además que la pantalla de serie
muestre lo leído de EEPROM y no la constante `SLH-046` (`gui.c:198`).

Resultado: **B es el mecanismo principal y A el control cruzado.** Mientras un equipo no lleve el
3.6.2, sólo hay A y la serie se toma del acta.

### 3.4 Regla de calibración vencida

- **Requisito:** si el día de la medida es posterior a la fecha de vencimiento, o no hay certificado,
  o la huella no coincide, **cada visita, cada fila del CSV y la cabecera del PDF** llevan de forma
  visible la leyenda «**EQUIPO CON CALIBRACIÓN VENCIDA**» (o la que corresponda: «SIN CALIBRACIÓN»,
  «COEFICIENTES DISTINTOS DEL CERTIFICADO»).
- **¿Bloquea o avisa?** Ni SFT ni el Manual 2024 lo fijan: SFT sólo pide «evidencia de equipos de
  medición y calibración» (`RESUMEN:152,157`); el año de validez viene de PT-853, fuente mexicana y
  referencial (`Instrumentos-y-Metodo-Campo.md:283-286`). **Decidido por Diego el 19-sep-2026
  (PA-01): avisa y marca el registro; no bloquea.** El texto visible en cada registro exportado es
  exactamente «EQUIPO CON CALIBRACIÓN VENCIDA». Se puede medir, el aviso exige una
  confirmación explícita, la visita queda marcada y el resumen de cumplimiento **cuenta aparte** las
  señales medidas con equipo no vigente.

---

## 4. Exportación para la interventoría

### 4.1 Paquete

Un único ZIP por campaña (o por proyecto), que se entrega por el mecanismo de compartir del teléfono.
**Nunca por SMTP con credenciales dentro de la app** (ver 5.3).

```
<proyecto>_<via>_<periodo>_<equipo>.zip
  informe.pdf            informe para la interventoría
  lecturas.csv           una fila por lectura (dato crudo)
  resultados.csv         una fila por señal y color (calculado)
  inventario.csv         señales de la vía, con su historial
  equipo.csv             equipo, firmware, calibración, huella
  campanas.csv           campañas incluidas
  certificado/<id>.pdf   certificado de calibración vigente ese día
  fotos/<sha256>.jpg     fotos, nombradas por su hash
  diario.jsonl           diario append-only con cadena de hashes
  manifiesto.json        SHA-256 de cada fichero anterior
```

- CSV en UTF-8, separador `;` (como la app actual), decimal con punto, fechas ISO 8601 con zona.
- XLSX con las mismas hojas es **mejora** (RF-REG-15).

### 4.2 Estructura del informe PDF

1. **Cabecera en todas las páginas:** proyecto, contrato, vía, tramo, periodo; equipo (serie, MAC,
   nombre Bluetooth, respuesta de `#V#`); certificado (id, fecha de calibración, **vencimiento**,
   SHA-256); leyenda de calibración vencida si aplica; SHA-256 del manifiesto.
2. **Criterio aplicado:** `criterio_umbral`, porcentaje, fuente literal, geometría comparada y, si no
   está declarada, la frase «geometría del equipo no declarada: los dictámenes son orientativos».
3. **Resumen del indicador:** por vía y tramo, número de señales medidas, que cumplen, que no, no
   dictaminables; **% de cumplimiento** frente a `criterio_agregado`; señales en ubicación crítica que
   no cumplen (0 tolerancia).
4. **Por familia de señal** (SR, SP, SI, ST…) y dentro de ella por PR: una fila por señal con código,
   PR, sentido, lado, lámina, y por color n, media, mínimo, umbral y dictamen.
5. **Evolución** frente a la campaña anterior de la misma señal (mejora, RF-REG-21).
6. **Anexos:** lista de fotos con hash; incidencias (lecturas descartadas, señales dadas de baja,
   correcciones del diario).

### 4.3 Trazabilidad

- **Diario append-only.** Toda alta, lectura, descarte, corrección y baja es un evento nuevo. Nada se
  sobrescribe ni se borra; una corrección referencia el evento corregido y lleva motivo y operador
  (SFT pide anular, no borrar: `RESUMEN:290-292`).
- **Cadena de hashes.** Cada evento lleva el SHA-256 del anterior. Alterar uno rompe todos los
  siguientes.
- **Manifiesto** con el SHA-256 de cada fichero; su propio hash va impreso en el PDF.
- **Límite honesto:** la cadena demuestra que nadie editó **después** de que un tercero guardara el
  hash. Por eso el hash del manifiesto tiene que salir del teléfono en el mismo acto de la entrega
  (impreso en el PDF que recibe la interventoría). Sin un tercero que lo conserve, quien tenga el
  teléfono puede regenerar la cadena entera. Firmar el manifiesto con una clave del equipo o de un
  servidor queda como **decisión abierta** (PA-05).

### 4.4 Correspondencia campo a campo con el esquema de SFT

SFT no trae plantilla; ésta es la correspondencia con su tabla `e11_vertical_signs_measurements`
(`REQ_IND_E11_SenalizacionVertical.md:47-83`). La exportación `sft_e11.csv` es **mejora** (RF-REG-17).

| Campo SFT | Línea | De dónde sale en este modelo |
| :--- | :---: | :--- |
| `id` | 48 | `visita_id` |
| `inspection_id` | 49 | `campana_id` |
| `sign_inventory_id` | 51 | `senal_id` (o `etiqueta_campo` si la interventoría tiene su propio inventario) |
| `pr_km` | 52 | `pr` en km decimal (ver C-08) |
| `sign_type` | 54 | `familia`: SR→REGLAMENTARIA, SP→PREVENTIVA, SI→INFORMATIVA, ST→TRANSITORIA |
| `sign_code` | 55 | `codigo_manual` |
| `retroreflectivity_measurement` | 58 | **Media del color de fondo** (C-05: SFT no tiene sitio para más colores; el resto va en `resultados.csv`) |
| `retroreflectivity_percentage` | 59 | Media de fondo / `valor_referencia` del fondo × 100; vacío si no hay referencia |
| `legibility_score` | 60 | `legibilidad` |
| `physical_damage` | 61 | `dano_fisico` |
| `vertical_deviation_degrees` | 62 | `desviacion_vertical_grados` |
| `condition`, `is_compliant` | 64-76 | Los calcula SFT; la app no los envía |
| `is_critical_location` | 79 | `ubicacion_critica` |
| `photo_urls` | 81 | Nombres de fichero en `fotos/` |
| `created_at` | 82 | `fecha_hora` de la visita |
| *(no existe en E11)* | — | Equipo y calibración: SFT los pide como evidencia de inspección (`RESUMEN:152,157`); van en `equipo.csv` y en `certificado/` |

Y con las entidades generales (`RESUMEN:385-404`): `tramo_id`, `fecha_inspeccion` (= `fecha_inicio`),
`fecha_verificacion` (la pone la interventoría), `tipo_inspeccion`, `calzada`, `carril` (no aplica a
señal vertical: vacío), `cumplimiento` (= `dictamen_senal`), `observaciones`, `valores_medicion` (JSON
con las lecturas por color).

---

## 5. Contraste con la app de campo actual (`RetroVerticalP1`)

Fuente: código en `D:\@Proyect\IT\P_RetroReflectometro_Vertical\03_App_Movil\RetroVerticalP1\app\src\main\java\com\example\retrohorizontalp1\`
y `05_Documentacion\SPEC-06-App-Movil-Funcional.md` §C, §D, §F, §G.

### 5.1 Qué guarda hoy

Un CSV por proyecto con cabecera de proyecto (nombre, descripción, responsable, nombre y código de la
vía; `crearproyecto.java:50-53`) y una línea por medida guardada (`medicion.java:1562`):
retrorreflectividad, latitud, longitud, dirección postal, color, tipo, tipo de señal, código de
señal, fecha (`Date.toString()`).

### 5.2 Qué le falta

| Requisito | Hoy | Cita |
| :--- | :--- | :--- |
| Identidad del equipo | **No se guarda.** La MAC se lee al listar emparejados y se descarta | `medicion.java:120` |
| Firmware y calibración | No existen | — |
| Inventario de señales con ID | No: cada medida es una línea suelta; la misma señal medida dos veces no se relaciona | `medicion.java:1562` |
| PR, sentido, lado, calzada | No; sólo dirección postal por geocodificación inversa | SPEC-06 §E |
| Precisión del GPS | No | `medicion.java:1562` |
| Varias lecturas por color, media y mínimo | No: una lectura por línea, sin agrupar | `medicion.java:1562` |
| Tipo de lámina | Mal resuelto: el selector ofrece «Tipo 1» a «Tipo 11», pero **todo lo que no es Tipo 1 se envía como `2`** al equipo; el CSV guarda el texto elegido, no lo que se midió | `medicion.java:1548,1584-1588` |
| Color | Se guarda el **nombre visible** («Blanco»), no el código de 3 letras que se envió | `medicion.java:1495-1525,1562` |
| Umbral y cumplimiento | No existen | — |
| Fecha con zona y formato estable | `Date.toString()`, dependiente de configuración regional | `medicion.java:1562` |
| Inmutabilidad | No: el fichero se reescribe entero; tras guardar, el campo queda en `0` y un segundo toque guarda un cero | `medicion.java:1563,1565`; SPEC-06 §F |
| Exportación | Correo SMTP con el CSV adjunto y **credencial embebida** (no se reproduce aquí) | SPEC-06 §G «Credenciales» |
| Fotos | No | — |

### 5.3 Discrepancias con SPEC-06 que conviene corregir allí (no se tocan desde aquí)

- SPEC-06 §F dice que el color se guarda como código (`BLA`…); el código guarda el nombre visible
  (`medicion.java:1562` con `Bcolor.getText()`, textos en `:1495-1525`).
- SPEC-06 §D habla de dos tipos; el selector tiene once (`medicion.java:1548`).

### 5.4 Qué se conserva

El flujo proyecto → vía → medida con selección de señal por imagen y geolocalización automática es
correcto y está en uso (regla 6 del CLAUDE.md del V5). Este documento **amplía** ese flujo; no lo
sustituye.

---

## 6. Inventario georreferenciado de señales y medidas periódicas

### 6.1 Levantamiento, una sola vez por vía

- Cada señal recibe un `senal_id` propio y los campos de 2.4: código del Manual 2024, familia,
  colores, lámina, coordenadas con la **precisión que declare el teléfono**, PR, sentido, lado y foto
  opcional.
- Dos vías de entrada: **en campo** (alta durante la primera campaña) o **importado** de un fichero
  (inventario del concesionario o de otro teléfono).

### 6.2 Visitas siguientes

- La app **propone las señales cercanas** a la posición actual: radio configurable, ordenadas por
  distancia y, a igual distancia, primero las que están de cara al sentido de marcha (rumbo del
  teléfono frente al `sentido` de la señal). El radio por defecto es un parámetro sin fijar
  (**PA-03**); la precisión declarada del GPS se muestra junto a la lista.
- El operador elige una y mide. La visita queda enlazada a esa señal y a la campaña abierta.
- **GPS impreciso** o señal que no aparece: búsqueda manual por PR (rango) o por código del Manual.
  Esto es núcleo; la propuesta por GPS es comodidad.
- **Señal nueva:** alta con estado ACTIVA y fecha. **Señal retirada:** baja con motivo; sigue en el
  inventario y en el historial. **Reposición:** señal nueva con `reemplaza_a`. Nada se borra.
- Si la posición al medir dista del inventario más que el radio, la app avisa y guarda la distancia.

### 6.3 Cruce e indicador

- Con las visitas enlazadas, el indicador se calcula como en 2.8 y 4.2: umbral por color y lámina
  según `criterio_umbral`, agregación por vía y tramo, % de cumplimiento frente a
  `criterio_agregado`, y señales críticas aparte.
- **Evolución por señal:** serie de medias por color a lo largo de las campañas (mejora).

### 6.4 Datos sin conexión

- **Base local en el teléfono** con todas las entidades de la sección 2. La app mide, guarda, calcula y
  exporta **sin red**.
- **Inventario entre teléfonos:** exportación de `inventario.csv` + fotos + manifiesto; importación con
  fusión por `senal_id`. Un `senal_id` desconocido se añade; uno conocido con cambios genera un evento
  de corrección en el diario, nunca una sobrescritura. Dos altas distintas de la misma señal física
  (dos UUID a pocos metros con el mismo código) se señalan como **posible duplicado** para que el
  operador las fusione con un evento explícito.
- **Servidor:** no se propone. Si se quiere sincronizar teléfonos en línea o firmar manifiestos, es
  **decisión abierta** (PA-05).

---

## 7. Requisitos

Clase: **N** = núcleo (imprescindible para la interventoría), **M** = mejora (comodidad; entra si no
cabe en la primera app de producción).

| ID | Clase | Requisito | Criterio de aceptación | Prueba |
| :--- | :---: | :--- | :--- | :--- |
| **RF-REG-01** | N | Cada visita guarda MAC, nombre Bluetooth, serie y respuesta completa de `#V#` del equipo con que se midió | Ninguna visita exportada tiene estos campos vacíos; la serie vacía impide medir hasta darla de alta | Unitaria sobre el exportador con un equipo sin serie: la medida se rechaza. Con simulador: `#V#` aparece literal en `equipo.csv` |
| **RF-REG-02** | N | Todo registro exportado lleva fecha de calibración, **vencimiento = calibración + 1 año**, id y SHA-256 del certificado | Vencimiento correcto en años bisiestos (29-feb → 28-feb del año siguiente, **PA-02**) | Unitaria: tabla de fechas, incluido el 29-feb |
| **RF-REG-03** | N | Si el día de la medida es posterior al vencimiento, no hay certificado o la huella no coincide, la visita, su fila CSV y la cabecera del PDF llevan la leyenda visible «EQUIPO CON CALIBRACIÓN VENCIDA». **Avisa, no bloquea** (PA-01, decidido 19-sep-2026) | La leyenda aparece en las tres salidas; el resumen cuenta esas señales aparte | Instrumentada: reloj del teléfono adelantado un año y un día; exportar y buscar la leyenda |
| **RF-REG-04** | N | Al conectar, la app calcula la huella de `#G`×12 + `#GT#` y la compara con la del certificado | Coincide → vigente; no coincide → «coeficientes distintos del certificado»; `DEF` → «sin calibración» | Simulador: cambiar un coeficiente con `#S` y reconectar |
| **RF-REG-05** | N | Inventario de señales por vía con `senal_id` propio y los campos obligatorios de 2.4 | Una señal no se puede medir sin estar en el inventario (se da de alta en el momento si hace falta) | Unitaria sobre el modelo: campos obligatorios; UUID único |
| **RF-REG-06** | N | Ubicación con coordenadas, precisión declarada, PR (texto y km, sin tope de 99,999), sentido, lado y calzada | `K122+380` se guarda y se exporta como `122.380` | Unitaria del conversor de PR |
| **RF-REG-07** | N | Campaña con vía, tramo, periodo, tipo de inspección (Periódica, Autoevaluación, Evaluación) y operadores | Toda visita pertenece a una campaña abierta; una campaña cerrada no admite visitas | Unitaria del ciclo de vida de la campaña |
| **RF-REG-08** | N | Lecturas por color: `lecturas_por_color` disparos (4 por defecto), cada uno con su trama cruda; media, mínimo, máximo y n; lecturas descartables con motivo, nunca borradas | Con 4 lecturas 100, 110, 120, 130 → media 115, mínimo 100; al descartar una, n = 3 y sigue en `lecturas.csv` | Unitaria de estadística; instrumentada de descarte |
| **RF-REG-09** | N | Umbral según `criterio_umbral` del proyecto, con fuente literal; dictamen por color y por señal; NO_DICTAMINABLE cuando falte lámina, geometría, valor de referencia o fila de tabla | Con geometría «no declarada», ningún dictamen sale como CUMPLE / NO_CUMPLE | Unitaria con los cuatro criterios de C-01 sobre la misma señal |
| **RF-REG-10** | N | El negro no se mide; un color sin soporte en el equipo (púrpura, marrón, fluorescentes) sale como «no medible con este equipo» | Una señal con leyenda negra no pide lecturas del negro | Unitaria |
| **RF-REG-11** | N | Ubicación crítica por señal; el resumen lista aparte las críticas que no cumplen | Una sola crítica que no cumple aparece aunque el % global supere el 95 % | Unitaria del agregado |
| **RF-REG-12** | M | Legibilidad, daño físico y desviación vertical, para calcular la condición de SFT | La condición coincide con la fórmula de `REQ_IND_E11:64-72` | Unitaria con los casos QA-E11-01 y QA-E11-02 de SFT (`:105-106`) |
| **RF-REG-13** | N | Diario append-only con cadena SHA-256; correcciones como eventos nuevos | Alterar un byte de un evento hace fallar la verificación de todos los siguientes | Unitaria: diario de 100 eventos, modificar el 50 |
| **RF-REG-14** | N | Paquete ZIP de 4.1 con manifiesto; el SHA-256 del manifiesto va impreso en el PDF | Un verificador externo recalcula todos los hashes y coinciden | Script de verificación fuera de la app sobre un paquete real |
| **RF-REG-15** | M | Las mismas tablas en XLSX | Mismo contenido que los CSV | Comparación automática CSV ↔ XLSX |
| **RF-REG-16** | N | PDF con cabecera de equipo y calibración en todas las páginas, criterio aplicado, resumen por vía, tramo y familia, y detalle por señal | Revisión con una interventoría real (**PA-04**) | Manual, con lista de comprobación de 4.2 |
| **RF-REG-17** | M | Exportación `sft_e11.csv` según la tabla de 4.4 | Carga sin errores en el esquema de `REQ_IND_E11:47-83` | Carga en una base de prueba con ese esquema |
| **RF-REG-18** | M | Propuesta de señales cercanas por GPS, radio configurable, orden por distancia y sentido | Con precisión de 5 m y radio de 30 m, la señal correcta aparece entre las tres primeras (valores de prueba, no de diseño) | Instrumentada con posiciones simuladas |
| **RF-REG-19** | N | Búsqueda manual de señal por PR o por código | Encuentra la señal sin GPS | Instrumentada en modo avión |
| **RF-REG-20** | N | Alta, baja y reposición con historial; nada se borra | Una señal dada de baja sigue en `inventario.csv` con estado y motivo | Unitaria |
| **RF-REG-21** | M | Evolución por señal entre campañas | Serie de medias por color, ordenada por periodo | Unitaria |
| **RF-REG-22** | N | Indicador agregado por vía y tramo: % de cumplimiento frente a `criterio_agregado` | 19 de 20 señales dictaminables que cumplen → 95 %, cumple con criterio 95 %; las no dictaminables se informan aparte y no entran en el denominador (**PA-06**) | Unitaria |
| **RF-REG-23** | N | Todo funciona sin red: medir, guardar, calcular, exportar | Ciclo completo en modo avión | Instrumentada |
| **RF-REG-24** | N | Importar un inventario inicial desde fichero | Un `inventario.csv` exportado por otra instalación se importa sin pérdidas | Ida y vuelta entre dos instalaciones |
| **RF-REG-25** | M | Fusión de inventarios entre teléfonos con detección de duplicados | Dos altas de la misma señal a menos de 5 m con igual código se marcan como posible duplicado | Unitaria |
| **RF-REG-26** | N | Fecha de calibración y serie en EEPROM, firmware 3.6.2 (opción B de 3.3, decidida el 19-sep-2026). La app lee fecha y serie del equipo, calcula el vencimiento (+1 año) y lo contrasta con el certificado y con la huella | `#GC` devuelve lo escrito con `#SC`, `#GN` lo escrito con `#SN` (nombres definitivos en PROTOCOLO-V3.6); `#V#` no cambia; fecha del equipo distinta de la del certificado → registro marcado | TDD de firmware en simulador y en equipo; prueba de app con fecha discrepante |
| **RF-REG-27** | N | La exportación no contiene credenciales y no las necesita: se entrega por el mecanismo de compartir del teléfono | Ninguna cadena de credencial en el APK ni en el paquete | Búsqueda automática en el APK y en el ZIP |
| **RF-REG-28** | N | Fechas en ISO 8601 con zona; el estado de limpieza (sin limpiar, limpia, ambas) se registra en cada visita | Fechas legibles igual en cualquier configuración regional | Unitaria con dos configuraciones regionales |

**Núcleo, en una línea:** equipo identificado y calibración con vencimiento (01-04), inventario con ID
y ubicación (05-06, 19-20, 24), campaña y lecturas por color con trama cruda (07-08), umbral
parametrizado con «no dictaminable» (09-11, 22), diario inmutable y paquete verificable (13-14, 16),
sin red y sin credenciales (23, 27-28). **Mejora:** propuesta por GPS, evolución, XLSX, formato SFT,
condición visual de SFT y fusión entre teléfonos. La fecha y la serie en EEPROM (26) pasan a núcleo
por decisión de Diego del 19-sep-2026.

---

## 8. Puntos abiertos y decisiones de Diego

| ID | Pregunta | Por qué no se decide aquí |
| :--- | :--- | :--- |
| **PA-01** | ¿La calibración vencida **bloquea** la medida o sólo **avisa**? | **Decidido (19-sep-2026):** avisa y marca; texto «EQUIPO CON CALIBRACIÓN VENCIDA» |
| **PA-02** | Vencimiento de una calibración del 29-feb | Convención: 28-feb del año siguiente |
| **PA-03** | Radio por defecto de la propuesta de señales cercanas | Depende de la precisión real del GPS en campo: se mide |
| **PA-04** | ¿Qué criterio de umbral usa el contrato concreto (C-01)? ¿Hay una interventoría con quien validar el PDF? | Es una decisión contractual, no técnica |
| **PA-05** | Servidor para sincronizar o firmar manifiestos | Fuera de alcance salvo decisión expresa |
| **PA-06** | ¿Las señales no dictaminables entran en el denominador del %? | SFT no lo dice. Propuesta: no entran y se informan aparte |
| **PA-07** | ¿Se autoriza la opción B (fecha en EEPROM, firmware 3.6.x)? | **Decidido (19-sep-2026):** opción B en el firmware 3.6.2, fecha (`#SC`/`#GC`) y serie (`#SN`/`#GN`); nombres definitivos en PROTOCOLO-V3.6; la huella sigue como control cruzado. En la V4.6: por verificar en V4 |
| **PA-08** | ¿`D:\onedrive\gdrive\sft` es el contenido de SFT? | **Decidido (19-sep-2026):** sí, es la fuente; el repositorio de GitHub está vacío (0.1) |
| **PA-09** | Geometría del equipo (C-06) | Se cierra midiendo, no leyendo |
