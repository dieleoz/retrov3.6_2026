# Manual de Usuario — Aplicación Móvil de Calibración "RTV Calibra"

Manual técnico de la aplicación móvil de calibración para el Retrorreflectómetro Vertical V3.6.
Destinado al personal de metrología, técnicos de laboratorio y soporte técnico de DPI Ingeniería & Consultoría.

> [!NOTE]
> **Versión documentada y etiqueta en pantalla:**
> - **Etiqueta en pantalla:** `RTV Calibra` (nombre visible en el lanzador de aplicaciones de Android).
> - **Versión documentada:** `Cov_3.6.5_calibrar` (código de versión `10010`).
> - **Identificador de paquete:** `com.dpi.retrov36.calibra`.
> - **Archivo binario instalador:** `Cov_3.6.5_calibrar.apk`.
> - **Huella MD5:** `dc0beaf5e110a72b7cbb288e4011cb68`.
> - **Aviso de trazabilidad:** El renombrado formal a "Retro Coviandina Calibrar" corresponde a una tarea
>   programada de la línea de desarrollo; este manual describe con fidelidad la versión que el operador
>   visualiza y ejecuta hoy en el teléfono móvil.

---

## 1. Propósito Metrológico y Arquitectura del Sistema

La aplicación móvil de calibración es una herramienta técnica especializada de uso interno para DPI, diseñada para
ajustar los polinomios de calibración del retrorreflectómetro vertical sin requerir la reprogramación física del
microcontrolador PIC18F47K42 ni herramientas de compilación en campo.

### Fundamento Técnico de la Línea V3.6:
- **Almacenamiento en memoria EEPROM:** Las fórmulas de conversión residen en la memoria no volátil del instrumento
  a partir de la dirección `0x100`. Cada uno de los 12 canales ópticos dispone de un registro protegido de 18 bytes
  compuesto por 4 coeficientes en formato punto flotante IEEE 754 (little endian) y una suma de verificación
  cíclica CRC-16/CCITT.
- **Acceso mediante protocolo seguro:** Las modificaciones se ejecutan en modo administrador mediante tramas ASCII
  delimitadas por `#`, autenticadas por código PIN y validadas por hardware antes de su activación definitiva.
- **Identificación del binario entregado:**
  - **Nombre en lanzador de Android:** `RTV Calibra`
  - **Versión de la aplicación:** `Cov_3.6.5_calibrar` (código de versión `10010`)
  - **Archivo APK:** `Cov_3.6.5_calibrar.apk`
  - **Huella MD5 del APK:** `dc0beaf5e110a72b7cbb288e4011cb68`
  - **Identificador de paquete:** `com.dpi.retrov36.calibra`

---

## 2. Requisitos Previos para la Calibración

Antes de iniciar cualquier sesión metrológica en laboratorio o en banco de calibración:

1. **Preparación del instrumento:** Encienda el retrorreflectómetro vertical antes de iniciar la primera toma de
   datos para verificar la operatividad del sensor fotométrico y la fuente óptica.
2. **Ambiente controlado:** El laboratorio o recinto debe encontrarse libre de corrientes de polvo, a temperatura
   controlada y sin incidencia de luz solar directa sobre las bocas de medición.
3. **Patrones de calibración certificados:** Tenga a disposición los patrones planos de retrorreflexión certificados
   (patrones tipo I, II, III, IV, XI con valores de referencia vigentes trazables).
4. **Archivo ZIP de campaña preparado:** El archivo de banco de patrones (por ejemplo banco representativo) debe
   encontrarse cargado en el almacenamiento local del teléfono móvil.

---

## 3. Procedimiento de Calibración en Seis Pasos

La aplicación opera mediante un camino guiado simplificado estructurado en seis etapas:

```
Conexión  →  Cargar ZIP  →  Revisar listos  →  Calibrar  →  Re-medir  →  Acta y Cierre
```

### Paso 1: Conexión con el Retrorreflectómetro
1. Encienda el equipo y verifique que la pantalla STONE se encuentre activa.
2. Abra la aplicación `RTV Calibra` en el dispositivo móvil.
3. Seleccione el retrorreflectómetro en la lista de dispositivos Bluetooth emparejados.
4. La aplicación enviará la orden `#V#` para comprobar el firmware V3.6 y leerá el número de serie (`#GN#`).
5. Espere a que la aplicación confirme el enlace con indicador verde.

### Paso 2: Carga del Archivo ZIP de Calibración
1. En la pantalla principal, presione el botón **Cargar el ZIP**.
2. El sistema abrirá el selector de documentos de Android (`ACTION_OPEN_DOCUMENT`).
3. Seleccione el archivo `.zip` que contiene los datos crudos y las lecturas previas del banco de patrones.
4. La aplicación descomprime e importa las series, asume la estructura del banco y prepara la cola de ajuste.

### Paso 3: Revisión de Códigos "Listos para Calibrar"
Bajo el botón de carga, la aplicación muestra de forma transparente el diagnóstico del instrumento:
- Número de series importadas y banco de procedencia.
- **Códigos listos para calibrar:** Enumera los canales habilitados para escritura (por ejemplo, canal 8 blanco y
  canal b amarillo).
- **Códigos bloqueados con su motivo:** Indica si algún código no es calibrable (por ejemplo, canales reservados de
  fábrica 1 y 2 que no se sobreescriben, o códigos que requieren verificación previa).

### Paso 4: Ejecución del Botón "Calibrar"
1. Pulse el botón principal **Calibrar**.
2. Ingrese el nombre completo del técnico responsable de la calibración (este dato se plasmará en el acta técnica).
3. La aplicación realiza de forma automática:
   - Apertura de sesión de administrador enviando el PIN (`#L,<pin>#`; el PIN lo entrega DPI).
     Por seguridad y normativa interna, es obligatorio cambiar el PIN de fábrica antes de la primera escritura.
   - Envío de los coeficientes calculados mediante tramas `#S,<k>,<c3>,<c2>,<c1>,<c0>#`.
   - Grabación y validación de integridad en la memoria EEPROM.

### Paso 5: Re-medida y Verificación de Conformidad
Una vez escritos los coeficientes en el instrumento, la aplicación guía al operador para colocar los patrones de
control y ejecutar las lecturas de verificación:
- **Criterio de conformidad metrológica estricta:**
  - **Canal 8 (blanco):** La desviación relativa entre el valor medido por el equipo y el valor certificado del
    patrón debe situarse obligatoriamente dentro de un margen máximo de **±5 %**.
  - **Canal b (amarillo):** La desviación relativa debe situarse dentro de un margen máximo de **±10 %**.
- Si una re-medida no cumple la tolerancia exigida, la aplicación rechaza automáticamente el canal y ofrece
  restaurar los parámetros anteriores.

### Paso 6: Grabación de Fecha, Acta y Cierre Seguro
1. Si los canales verificados cumplen los límites de tolerancia, la aplicación graba la fecha del día en la EEPROM
   del equipo mediante la orden `#SC,<AAAA-MM-DD>#`.
2. La aplicación relee inmediatamente la fecha mediante `#GC#` para comprobar su persistencia física en hardware.
3. Se genera el acta técnica de calibración en formato de texto plano (`acta_*.txt`) detallando las mediciones,
   desviaciones y veredictos de conformidad.
4. La aplicación envía la orden `#Q#` para cerrar la sesión de administración y bloquear el firmware del equipo,
   impidiendo alteraciones accidentales.

---

## 4. Controles de Salvamento y Seguridad (Modo Técnico)

Para responder ante contingencias durante la prueba de laboratorio, la aplicación cuenta con salvavidas técnicos:

- **Leer batería (9):** Solicita la tensión del acumulador interno mediante el comando de diagnóstico 9.
- **Rechazar:** Revierte de inmediato la memoria del equipo a la fecha y coeficientes existentes antes de la sesión.
- **Cerrar sin restaurar:** Cierra la comunicación dejando los valores en el estado actual (requiere PIN de
  administrador).
- **Liberar:** Cierra el socket de comunicación Bluetooth y desbloquea el microcontrolador en caso de bloqueo en
  el enlace serie.

---

## 5. Salidas Documentales y Soporte Técnico

Al completar la jornada de calibración, la aplicación genera y almacena en el dispositivo móvil:

1. **Acta Técnica de Calibración (`.txt`):** Archivo formal con el historial de disparos, lecturas de referencia,
   coeficientes polinómicos ajustados y declaración de conformidad.
2. **Paquete ZIP de Soporte (`soporte_*.zip`):** Contiene el acta técnica, el diario de eventos y el archivo
   `tramas.log` completo con todas las tramas de comunicación serie intercambiadas con el equipo.
3. **Certificado de Calibración:** Los datos del acta técnica son la fuente fidedigna para emitir el certificado
   técnico para el cliente conforme al formato oficial bajo la declaración de alcance correspondiente.
