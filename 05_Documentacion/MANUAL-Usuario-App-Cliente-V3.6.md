# Manual de Usuario — Aplicación Móvil de Campo "Retro Coviandina"

Manual de operación de la aplicación móvil de usuario para el Retrorreflectómetro Vertical V3.6.
Destinado a operadores de campo, técnicos viales e interventoría.

---

## 1. Identificación y Alcance del Sistema

La aplicación móvil de campo "Retro Coviandina" permite conectar el retrorreflectómetro vertical mediante
Bluetooth, registrar las lecturas de retrorreflexión de señales de tránsito y exportar los registros técnicos.

- **Instrumento compatible:** Retrorreflectómetro Vertical V3.6 (firmware de la línea V3.6).
- **Nombre en pantalla:** `Retro Coviandina`
- **Identificador de paquete:** `com.dpi.retrousuario.coviandina`
- **Versión de la aplicación:** 0.3.7 (código de versión `10`)
- **Archivo instalador:** `RETRO-COVIANDINA-usuario-0.3.7-10.apk`
- **Huella MD5 del instalador:** `6aecf3ac2251db29f78b74d324b920cc`
- **Sistema operativo requerido:** Android 7.0 (API 24) o superior.
- **Permisos requeridos por el sistema:**
  - **Ubicación precisa (`ACCESS_FINE_LOCATION`):** requerida por el sistema Android para escanear y conectar por
    Bluetooth, y para georreferenciar las coordenadas geográficas de cada señal inspeccionada.
  - **Bluetooth:** requerido para enlazar con el instrumento a 9600 baudios (8N1).

> [!NOTE]
> **Alcance operativo y estado de medición:**
> - **Qué hace:** Registra disparos de medición, calcula promedios por color, georreferencia con GPS y exporta el
>   paquete ZIP auditado con los archivos de medidas, inventario y tramas.
> - **Qué NO hace:** No calibra ni modifica los coeficientes ni la memoria EEPROM del equipo (labor reservada a la
>   aplicación técnica de laboratorio de DPI). Tampoco sustituye la inspección visual del estado físico de la señal.
> - **Estado de los datos:** Las lecturas fotométricas provienen directamente del sensor del instrumento físico. La
>   precisión y disponibilidad de las coordenadas dependen de la antena GPS del teléfono del operador.

---

## 2. Instalación y Regla Estricta contra la Desinstalación

La instalación se realiza copiando el archivo APK en el almacenamiento interno del teléfono móvil (por ejemplo en
la carpeta `Descargas`).

> [!CAUTION]
> **REGLA CRÍTICA: NUNCA DESINSTALAR LA APLICACIÓN PARA ACTUALIZARLA**
>
> Bajo ninguna circunstancia desinstale la aplicación existente para instalar una versión más reciente. En el
> sistema Android, desinstalar una aplicación borra de inmediato su almacenamiento privado, destruyendo todas las
> campañas de medición, registros históricos y diarios de campo acumulados en el dispositivo.
>
> Instale la nueva versión directamente tocando el archivo APK desde el explorador de archivos del teléfono. El
> sistema actualizará la aplicación existente preservando intactos todos los datos.

### Pasos de Instalación / Actualización:
1. Verifique que el archivo `RETRO-COVIANDINA-usuario-0.3.7-10.apk` se encuentre en la memoria interna del teléfono.
2. Abra el explorador de archivos del dispositivo, localice el archivo APK y pulse sobre él.
3. Si el sistema solicita confirmación para instalar aplicaciones desconocidas, conceda la autorización para ese
   explorador de archivos.
4. Seleccione **Actualizar** (o **Instalar** si es la primera vez en un teléfono nuevo).
5. Al terminar, pulse **Listo** o **Abrir**.

---

## 3. Conexión con el Retrorreflectómetro Vertical

1. **Encendido del equipo:** Encienda el retrorreflectómetro con el interruptor principal. El indicador de la
   pantalla táctil STONE iniciará y mostrará la interfaz principal.
2. **Emparejamiento previo en Android:** Si es la primera vez que conecta el teléfono al instrumento, vaya a los
   Ajustes de Bluetooth de Android, busque dispositivos disponibles y vincule el retrorreflectómetro vertical
   (ingrese el PIN de enlace Bluetooth del módulo si el sistema lo solicita).
3. **Inicio de la aplicación:** Abra la aplicación `Retro Coviandina`.
4. **Aviso de seguridad:** Al ingresar a la pantalla de conexión, la aplicación mostrará una advertencia indicando que
   el equipo puede encender su haz de luz y emitir señales acústicas durante la sincronización inicial.
5. **Selección del equipo:** Toque el nombre del equipo en la lista de dispositivos emparejados.
6. **Validación automática:** La aplicación enviará la trama de versión del protocolo (`#V#`). Al confirmar que el
   equipo responde con firmware V3.6, consultará el número de serie (`#GN#`) y la fecha de calibración (`#GC#`).
   Una vez verificada la compatibilidad, el botón de ingreso a medición quedará habilitado.

---

## 4. Modo de Operación: "Medir y Exportar"

El modo principal está diseñado para trabajo intensivo en carretera con cero tecleo en pantalla.

### Flujo de Trabajo en Campo:
1. **Posicionamiento del instrumento:** Asiente la base del retrorreflectómetro vertical de manera firme y
   completamente perpendicular contra la cara de la señal de tránsito. Asegúrese de que el marco de goma selle
   cualquier entrada de luz solar exterior hacia la ventana óptica.
2. **Selección de color:** En la interfaz de la aplicación, seleccione el color correspondiente a la lámina
   retrorreflectiva que va a medir:
   - Blanco
   - Amarillo
   - Rojo
   - Naranja
   - Azul
   - Verde
   - Café
3. **Ejecución del disparo:**
   - Puede presionar el botón **Medir** en la pantalla del teléfono, o bien accionar directamente el gatillo
     físico del instrumento.
   - El equipo encenderá el haz luminoso interno, capturará las muestras del sensor fotométrico y transmitirá las
     lecturas a la aplicación móvil.
4. **Promediado de lecturas:**
   - Por defecto, la norma técnica requiere 3 disparos por cada color para consolidar el valor representativo.
   - La aplicación acumula los disparos y calcula la media aritmética de manera automática.

---

## 5. Diálogo del Operador: "Repetir o Saltar"

La aplicación incorpora una regla de diseño estricta: **ninguna repetición es automática ni silenciosa**. El sistema
nunca tomará decisiones a espaldas del operador de campo.

### Cuándo se Presenta el Diálogo:
Si una medición presenta anomalías técnicas tales como:
- Lectura en cero o valor negativo (sensor desalineado o señal totalmente degradada).
- Saturación del convertidor analógico-digital (luz solar directa filtrada por mal asentamiento).
- Disparo anulado por interferencia o tiempo de respuesta excedido.

La aplicación detendrá la secuencia y desplegará un cuadro de diálogo con dos alternativas:

1. **Repetir:** El operador reacomoda el equipo contra la señal y ejecuta nuevamente el disparo para reemplazar la
   muestra defectuosa.
2. **Saltar:** Si la señal está destruida o no es posible medirla físicamente, el operador selecciona saltar y el
   sistema anula esa serie sin registrar números artificiales.

Si el operador pulsa el botón Atrás o apaga el equipo con la pregunta abierta en pantalla, la aplicación descarta la
serie en curso de forma limpia, impidiendo que se guarden filas incompletas o erróneas en el reporte final.

---

## 6. Georreferenciación y Registro GPS

Cada serie de medición registrada en campo se vincula automáticamente con las coordenadas geográficas del teléfono:

- **Criterio de validez:** La coordenada se registra como válida (`con_posicion`) únicamente si el receptor GPS del
  teléfono reporta una incertidumbre menor a 50 metros y la lectura tiene menos de 2 minutos de antigüedad.
- **Zonas sin cobertura satelital:** En túneles, pasos bajo nivel o cañones donde la señal GPS se degrade, la
  aplicación continúa operando normalmente y marca el registro como `posicion_antigua` o `sin_posicion`. Bajo ninguna
  circunstancia se interrumpe la jornada de medición por falta de señal satelital.

---

## 7. Exportación y Envío de la Campaña

Al finalizar el recorrido de inspección o al término del turno de trabajo:

1. En la pantalla principal o de medición, pulse el botón **Exportar**.
2. La aplicación compila la base de datos de la sesión y genera un archivo comprimido en formato ZIP en la carpeta
   pública de descargas del teléfono (`Descargas/RTV` o `Downloads`).
3. El archivo ZIP generado contiene tres elementos técnicos auditables:
   - `medidas.csv`: Tabla con todas las lecturas de retrorreflexión, fecha, hora, color, coordenadas GPS y serie.
   - `inventario.csv`: Listado estructurado para interventoría y mantenimiento vial.
   - `tramas.log`: Registro completo de todas las comunicaciones Bluetooth transmitidas para respaldo pericial.
4. El teléfono abrirá automáticamente el menú del sistema Android para compartir el archivo ZIP mediante correo
   electrónico, mensajería instantánea o transferencia directa a un computador mediante cable USB.

---

## 8. Diagnóstico y Solución de Problemas

| Síntoma observado | Causa probable | Procedimiento de solución |
| :--- | :--- | :--- |
| El equipo no aparece en la lista Bluetooth | Bluetooth apagado o equipo sin energía | Encienda el retrorreflectómetro y verifique que el Bluetooth del teléfono esté activo |
| Mensaje "Equipo no compatible" | Firmware distinto a la línea V3.6 | Verifique con soporte técnico de DPI la versión de firmware del instrumento |
| Lecturas erráticas o advertencia de saturación | Luz solar ingresando por los bordes | Asiente firmemente la cara de apoyo del equipo contra la superficie plana de la señal |
| Mensaje de batería baja en pantalla STONE | Batería del instrumento descargada | Conecte el cargador suministrado por DPI hasta completar la carga del equipo |
| Error al intentar instalar la actualización | Intento de desinstalación previa o APK dañado | Asegúrese de no desinstalar la app previa y verifique la integridad del archivo APK |
