# Manual Interno de Know-How — Retrorreflectómetro Vertical V3.6

Manual técnico de ingeniería para preservación de conocimiento y mantenimiento del sistema V3.6.
Destinado a ingenieros de desarrollo, soporte técnico de firmware y metrología de DPI Ingeniería & Consultoría.

---

## 1. Objeto y Arquitectura General del Sistema

Este documento recopila la totalidad del conocimiento técnico obtenido durante la ingeniería inversa, diseño y
desarrollo de la línea V3.6 (PIC18F47K42), evitando tener que repetir el proceso de ingeniería inversa en el futuro.

### 1.1 Identificación del Hardware Físico
- **Instrumento:** Retrorreflectómetro Vertical SAT-LUX/V3 K42.
- **Microcontrolador:** Microchip PIC18F47K42 (revisión de silicio A001).
- **Tarjeta electrónica principal:** Placa rotulada "SATLUX H-IoT" (originalmente concebida para el proyecto
  Horizontal IoT, adaptada en producción 2020 para el Vertical).
- **Pantalla gráfica táctil:** Módulo inteligente STONE de segunda generación, conectado a la UART secundaria del
  microcontrolador configurada a 9600 baudios (8N1) en el binario grabado
  (`dist/default/production/RetroVertical_V3.6.X.production.lst:35500-35507`, BRG 416 con reloj de 16 MHz). La
  velocidad del módulo es configurable en su firmware y queda pendiente de validar contra el equipo físico.
- **Canal de comunicación Bluetooth:** Módulo serie conectado a la UART1 del PIC configurada a 9600 baudios (8N1),
  según se especifica en `05_Documentacion/PROTOCOLO-V3.6.md:16`.
- **Sensor fotométrico:** Fotodiodo de silicio conectado a canal analógico ADC (`measurement.c:208-250`). El
  promedio suma los índices 1 a 15 (`measurement.c:234-237`) y el desplazamiento de +200 cuentas (`línea 247`)
  se aplica sobre un filtro EMA de software de 600 muestras (`líneas 239-245`).

### 1.2 Binarios de Entrega Autorizados y Trazabilidad

| Componente | Archivo binario | Versión / Código | Huella MD5 | Audiencia / Manual |
| :--- | :--- | :---: | :--- | :--- |
| **Firmware PIC** | `RetroVertical_V3.6.hex` | V3.6.2 (XC8 v2.10) | `9d5d5e3951c8aa83d1465f16f3733d27` | Operación de Instrumento |
| **App Cliente** | `RETRO-COVIANDINA-usuario-0.3.7-10.apk` | 0.3.7 (código 10) | `6aecf3ac2251db29f78b74d324b920cc` | Operador de Campo ("Retro Coviandina") |
| **App Calibración** | `Cov_3.6.5_calibrar.apk` | 3.6.5 (código 10010) | `dc0beaf5e110a72b7cbb288e4011cb68` | Metrología y Laboratorio ("RTV Calibra") |

---

## 2. Firmware V3.6 frente a la Base de 2020

### 2.1 Limitación de la Base Histórica (2020)
En el firmware de 2020 (`01_Firmware/base_2020_d089f962/`, compilado con Microchip XC8 v2.10 con MD5 del archivo
hexadecimal `d089f9625090a1213291c090eae7ac01`), las 12 ecuaciones de calibración estaban grabadas en la memoria de
programa Flash (`ecuacionesCalibracion.c:3-42`). Modificar un coeficiente obligaba a reprogramar físicamente el chip
mediante programador PICkit.

### 2.2 Solución Implementada en la Línea V3.6
La versión V3.6 traslada las fórmulas polinómicas a la memoria EEPROM interna del microcontrolador PIC18F47K42
(`01_Firmware/RetroVertical_V3.6.X/calibracion_v36.c`). Los bits de configuración permanecen estrictamente idénticos a
la base de 2020: `EC FF F7 FF 9F FF FF DF FE FF` en el rango de direcciones `0x300000-0x300009`.

---

## 3. Mapa de Memoria EEPROM (PIC18F47K42)

La memoria EEPROM organiza los parámetros de calibración y metrología a partir de la dirección base `0x100`:

| Dirección base | Tamaño | Descripción del registro | Formato interno |
| :--- | :---: | :--- | :--- |
| `0x100 - 0x103` | 4 B | Cabecera de integridad V3.6 | ASCII `V36` seguido de versión de formato `0x01` |
| `0x104 + 18·k` | 18 B | Canales ópticos (k = 0 a 11) | 4 floats IEEE 754 little-endian + CRC16 CCITT |
| `0x1DC - 0x1ED` | 18 B | Factor de temperatura y PIN | Floats X_2, X_1, X_0, PIN (4 B ASCII) + CRC16 |
| `0x1EE - 0x1FF` | 18 B | Número de serie del equipo | Longitud (1 B), 12 B ASCII, 3 ceros + CRC16 |
| `0x200 - 0x211` | 18 B | Fecha de calibración técnica | Año (2 B low-first), mes, día, 12 ceros + CRC16 |

### 3.1 Canales Ópticos (Códigos 1 a 8 y a a d)
Los índices corresponden a las 12 funciones de calibración históricas:
- Códigos 1 a 8: Blanco tipo I, Amarillo tipo I, Blanco microprismático, etc.
- Códigos a a d: Canales extendidos de color según `calibracion_v36.h`.
- Los códigos 1 y 2 corresponden a patrones base; la decisión de no sobreescribirlos corresponde a esta campaña
  específica de calibración y no a una restricción física indeleble del firmware.

### 3.2 Algoritmo de Evaluación Polinómica
La evaluación se ejecuta en tipo `double` (64 bits) sin esquema Horner para preservar estricta identidad numérica
con la base de 2020 (`calibracion_v36.c:229-234`):
```c
resultado = ((c3 * x * x * x + c2 * x * x) + c1 * x) + c0;
```
En pruebas de simulación bajo MPLAB SIM (arnés T-A20), este cálculo produjo cero diferencias sobre 786.432 puntos de
comparación frente al cálculo de 2020.

---

## 4. Protocolo de Comandos Serie UART1 (9600 8N1)

Las tramas de administración están delimitadas por `#` al inicio y al final (`uart_module.c:52-121`), con un tamaño
máximo de 48 bytes y caducidad automática de 2 segundos ante tramas incompletas.

### 4.1 Comandos Públicos (Sin Autenticación)
- `#V#`: Retorna 5 campos: `#V,3.6,<fecha>,<CAL|DEF>,<mascaraHex>#` (`calibracion_v36.c:635-643`). Indica versión,
  fecha en EEPROM, estado (`CAL` si hay calibración o `DEF` de fábrica) y la máscara de 4 dígitos hexadecimales.
- `#G,<k>#`: Consulta los 4 coeficientes del canal k (`#G,<k>,<c3>,<c2>,<c1>,<c0>#`).
- `#GT#`: Consulta los factores de compensación térmica X_2, X_1, X_0 (`gui.c:41-43`).
- `#GN#`: Consulta la serie grabada en EEPROM (`#GN,<serie>#` o `#GN,NONE#`).
- `#GC#`: Consulta la fecha de calibración (`#GC,<AAAA-MM-DD>#` o `#GC,NONE#`).

### 4.2 Comandos de Administración (Requieren `#L,<pin>#`)
- `#L,<pin>#`: Autentica sesión de administración (`#L,<pin>#`; el PIN lo entrega DPI). Al acumular 5 fallos
  consecutivos, el firmware entra en estado de bloqueo permanente (`#ERR,BLOQUEADO#`) hasta reiniciar el equipo.
- `#P,<pin_actual>,<pin_nuevo>#`: Modifica el PIN de acceso. La exigencia de cambiar el PIN de fábrica queda en
  suspenso hasta que se implemente el rol de superadministrador (`SPEC-Calibracion-V3.6.md:911,1030`, PA-13); hoy
  no existe un control activo en el firmware que impida operar con el PIN inicial si no se ha modificado.
- `#S,<k>,<c3>,<c2>,<c1>,<c0>#`: Escribe coeficientes en memoria RAM y EEPROM (`calibracion_v36.c:658`).
- `#SC,<AAAA-MM-DD>#`: Escribe la fecha de calibración en la dirección `0x200`.
- `#SN,<serie>#`: Escribe el número de serie en la dirección `0x1EE`.
- `#Q#`: Cierra la sesión de administración y retorna el microcontrolador a modo operativo seguro.

---

## 5. Ecosistema de Aplicaciones Móviles

El sistema se compone de dos aplicaciones con alcances estrictamente separados:

### 5.1 Aplicación de Usuario (`RetroUsuario`)
- **Directorio de código:** `03_App_Movil/RetroUsuario/`.
- **Paquete:** `com.dpi.retrousuario.coviandina`, versión 0.3.7 (código 10).
- **Entregable:** `RETRO-COVIANDINA-usuario-0.3.7-10.apk`.
- **Arquitectura FABLE-USR:** La máquina de estados de medición reside en `dominio.EstadoMedida`, sobreviviendo a
  rotaciones de pantalla y pausas de ciclo de vida de Android.
- **Diálogo de Operador:** Implementa `PreguntaOperador.Decision.ABANDONAR` para descartar de forma limpia medidas
  interrumpidas, garantizando que ninguna lectura nula o corrupta genere filas espurias en `medidas.csv`.
- **Suite de pruebas:** 152 pruebas unitarias JVM puras (107 de requisito, 45 de comportamiento) sin dependencias
  de emulador, ejecutables directamente con JUnitCore.

### 5.2 Aplicación de Calibración (`RetroV36`)
- **Directorio de código:** `03_App_Movil/RetroV36/`.
- **Paquete:** `com.dpi.retrov36.calibra`.
- **Etiqueta en pantalla:** `RTV Calibra`.
- **Versión de campo validada:** `Cov_3.6.5_calibrar.apk` (código 10010, MD5 `dc0beaf5e110a72b7cbb288e4011cb68`).
- **Flujo guiado:** Importa el paquete ZIP de campaña, aplica banco previo, escribe en EEPROM y verifica tolerancias
  según regla `RF-COV-12` (±5 % para código 8 blanco y ±10 % para código b amarillo).
- **Cierre formal:** Emite actas técnicas y paquete de soporte para respaldo del certificado de calibración emitido
  bajo `CERT-TITULO-2`.

---

## 6. Lecciones Técnicas Aprendidas y Trampas Evitadas

1. **Transitorios `#ERR,FORMATO#`:** Ocurren por sobrecarga en la recepción UART cuando la aplicación móvil envía
   bloques largos a ráfaga. Se resuelven implementando pausas entre bloques (pacing de 24 bytes cada 30 ms).
2. **Catálogo de Patrones Azules y Verdes (C-08):** En patrones con certificado alto (≥ 40), se observó correlación
   negativa en lecturas crudas. No forzar coeficientes en canales 3, 4, 5 y 6 hasta que se verifiquen físicamente
   las etiquetas de los patrones patrón P112 y P124 (decisión D-14).
3. **Prohibición de desinstalación de aplicaciones:** En Android, desinstalar una app elimina su almacenamiento
   privado (`/data/data/<paquete>`), destruyendo las bases de datos de campañas. Toda actualización debe ser directa.
