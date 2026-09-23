# Manual de Operación y Mantenimiento — Retrorreflectómetro Vertical SAT-LUX/V3 K42

Manual de operación técnica y mantenimiento del instrumento de medición para la línea V3.6.
Destinado a inspectores de tránsito, técnicos viales, operadores de terreno y personal de laboratorio.

---

## 1. Principio Físico y Normativa de Medición

El Retrorreflectómetro Vertical SAT-LUX/V3 K42 es un instrumento optoelectrónico portátil de precisión diseñado para
medir en terreno y en laboratorio el coeficiente de retrorreflexión $R_A$ (o $R'$) sobre láminas y películas
retrorreflectivas aplicadas en señales de tránsito verticales.

### Especificaciones Técnicas y Metrológicas:
- **Magnitud medida:** Coeficiente de retrorreflexión $R_A$, expresado en unidades de candelas por lux por metro
  cuadrado ($cd \cdot lx^{-1} \cdot m^{-2}$).
- **Geometría óptica normalizada:**
  - Ángulo de observación ($\alpha$): **0,2°**.
  - Ángulo de entrada ($\beta$): **−4°** (o equivalente normal según diseño de banco).
  - Geometría estandarizada conforme a los requisitos de la norma técnica colombiana NTC 4739 y las especificaciones
    internacionales ASTM E1710.
- **Sistema de iluminación y captura:**
  - Fuente de luz: Emisor de estado sólido LED estabilizado espectralmente.
  - Sensor de detección: Fotodiodo de silicio de alta estabilidad con corrección de respuesta fotópica $V(\lambda)$
    para igualar la curva de sensibilidad del ojo humano estándar CIE.
- **Microcontrolador principal:** Microchip PIC18F47K42 en placa "SATLUX H-IoT".
- **Interfaz visual:** Pantalla gráfica táctil a color STONE STA035WT-01 de 2.ª generación.
- **Comunicación inalámbrica:** Enlace Bluetooth UART1 integrado a velocidad fija de 9600 baudios (8N1).

---

## 2. Componentes del Instrumento

El equipo se compone de los siguientes elementos principales:

1. **Cuerpo ergonómico principal:** Chasis de alta resistencia mecánica diseñado para trabajo pesado en carretera.
2. **Empuñadura con gatillo de disparo:** Permite sostener el instrumento con firmeza y accionar la medición de
   forma manual con un solo dedo.
3. **Ventana óptica de medición:** Ubicada en la base frontal inferior del instrumento. Dispone de un marco elástico
   de goma perimetral que actúa como sello de estanqueidad contra el ingreso de luz solar o luminarias parásitas.
4. **Pantalla táctil STONE (STA035WT-01):** Ubicada en la parte superior. Muestra el estado del sistema, el nivel
   de carga de la batería y los valores de retrorreflexión de cada disparo.
5. **Panel de control e interfaces:**
   - Interruptor general de alimentación (ON/OFF).
   - Toma para conector de carga de batería recargable interna.
   - Indicador luminoso LED de estado de carga eléctrica.
   - Módulo de comunicación serie inalámbrica Bluetooth interno.

---

## 3. Puesta en Marcha y Preparación para la Medición

### Procedimiento Inicial:
1. **Inspección visual:** Antes de cada jornada, retire la tapa protectora de la ventana óptica e inspeccione que la
   lente y el marco elástico de goma se encuentren completamente limpios y libres de partículas abrasivas.
2. **Encendido:** Accione el interruptor de encendido. La pantalla STONE se iluminará y desplegará la interfaz
   principal tras completar la inicialización del microcontrolador PIC18F47K42.
3. **Estabilización térmica (Requisito Metrológico):**
   - Mantenga el instrumento encendido durante al menos 10 a 15 minutos antes de registrar lecturas oficiales.
   - Este periodo garantiza que la fuente emisora LED y el circuito amplificador del fotodetector alcancen su
     temperatura de régimen estable, eliminando derivas térmicas en las lecturas.
4. **Comprobación de la batería:** Observe el indicador de carga en pantalla. Si el equipo reporta nivel bajo,
   recárguelo antes de salir a campo para evitar apagados repentinos durante una serie de medidas.

---

## 4. Procedimiento de Medición en Terreno

Para obtener datos confiables y reproducibles que cumplan los requisitos de interventoría vial:

1. **Acondicionamiento de la señal:** Si la cara de la señal presenta polvo denso o salpicaduras de lodo superficial,
   límpiela suavemente con un paño seco o ligeramente humedecido con agua limpia. No use detergentes abrasivos.
2. **Asentamiento perpendicular:**
   - Apoye la boca frontal del retrorreflectómetro contra la superficie de la lámina retrorreflectiva.
   - Mantenga el instrumento completamente perpendicular a la superficie de la señal.
   - Aplique una presión suave y uniforme para asegurar que el marco de goma selle todo el perímetro, bloqueando la
     luz ambiental del exterior.
3. **Disparo de medición:**
   - Oprima el gatillo de la empuñadura del instrumento o toque el botón **Medir** en la aplicación móvil enlazada.
   - El equipo activará un pulso de luz interno, procesará las muestras analógicas y emitirá un pitido de
     confirmación.
   - El valor numérico de retrorreflexión aparecerá en la pantalla STONE y se transmitirá simultáneamente al teléfono.
4. **Muestreo por señal (Toma Múltiple):**
   - Según las directrices técnicas del Manual de Señalización Vial y la NTC 4739, nunca se dictamina una señal con
     un solo disparo.
   - Realice al menos tres tomas en distintos puntos representativos del mismo color (por ejemplo en el fondo y en
     la orla) para calcular el promedio aritmético.

---

## 5. Limpieza, Cuidados y Mantenimiento Preventivo

El retrorreflectómetro es un equipo óptico de alta precisión y debe ser tratado con el cuidado correspondiente:

- **Limpieza de la ventana óptica:**
  - Retire partículas de polvo con una perilla de aire soplador antes de tocar la superficie de vidrio.
  - Limpie la ventana de medición únicamente con toallitas de microfibra de uso óptico.
  - **PROHIBIDO:** Usar solventes agresivos, alcoholes industriales, acetona, thinner o paños de taller que puedan
    rayar el recubrimiento óptico o deteriorar el marco de goma.
- **Protección contra factores ambientales:**
  - No utilice el equipo bajo lluvia intensa ni en presencia de condensación visible sobre las lentes.
  - Si el equipo pasa de un vehículo con aire acondicionado a un ambiente cálido y húmedo, permita que se aclimate
    durante 15 minutos para evaporar cualquier rocío antes de encenderlo.
- **Transporte y almacenamiento:**
  - Transporte siempre el instrumento dentro de su maletín rígido acolchado de protección suministrado por DPI.
  - Nunca deje el instrumento expuesto al sol directo dentro de la cabina de un vehículo cerrado.

---

## 6. Mantenimiento del Sistema de Batería

- Utilice exclusivamente el cargador de pared suministrado por DPI Ingeniería & Consultoría.
- Conecte el cargador a una toma de corriente estable. El indicador LED confirmará el proceso de carga.
- Un ciclo completo de carga toma aproximadamente entre 3 y 5 horas.
- Si el instrumento va a permanecer almacenado sin uso por más de dos meses, realice una recarga completa para
  proteger la vida útil del acumulador interno.

---

## 7. Trazabilidad Metrológica y Calibración Periódica

- **Vigencia del ajuste:** La calibración técnica del retrorreflectómetro vertical tiene una vigencia recomendada de
  un año calendario. La fecha del último ajuste se encuentra registrada de forma indeleble en la memoria EEPROM del
  instrumento y se transmite en cada verificación inicial mediante la orden `#V#`.
- **Patrones de control:** Se aconseja verificar periódicamente la estabilidad del equipo midiendo los patrones de
  referencia suministrados con el equipo.
- **Recalibración anual:** Cumplido el periodo de vigencia o en caso de detectarse desviaciones superiores a las
  tolerancias admisibles por la norma técnica, el instrumento debe remitirse al laboratorio de DPI Ingeniería &
  Consultoría para su ajuste metrológico y emisión de un nuevo certificado de calibración.
