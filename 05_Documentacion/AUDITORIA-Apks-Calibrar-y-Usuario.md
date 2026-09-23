# Auditoría técnica — Apps V3.6: Calibrar (Cov_3.6.5_calibrar) y Usuario (RetroUsuario 0.3.7)

**Fecha:** 22-sep-2026  
**Alcance:** Línea Retrorreflectómetro Vertical V3.6 (PIC18F47K42, placa "SATLUX H-IoT", Bluetooth 9600 8N1).  
**Método:** Inspección estática de código fuente línea a línea, cotejo contra contratos de protocolo
(`PROTOCOLO-V3.6.md`), especificaciones técnicas (`SPEC-App-Calibracion-Coviandina.md`,
`SPEC-App-Usuario-V3.6.md`), árbol git y suites de pruebas unitarias JVM.

---

## 1. Identificación de los binarios y fuentes auditados

| Parámetro | App de Calibrar (Empresa / Corta) | App de Usuario Final (Campo) |
| :--- | :--- | :--- |
| **Nombre comercial / APK** | `Cov_3.6.5_calibrar` | `RetroUsuario` |
| **Fichero binario** | `Cov_3.6.5_calibrar.apk` | `RetroUsuario-0.3.7.apk` |
| **Paquete (`applicationId`)** | `com.dpi.retrov36.calibra` | `com.dpi.retrousuario.coviandina` |
| **Versión (`versionCode`/`Name`)** | `10010` / `Cov_3.6.5_calibrar` | `10` / `0.3.7` |
| **Rama / Commit base** | `rtv-1.0-cierre` (`316a6bc`) | `retro-usuario` (`b8a299d`) |
| **Árbol de código fuente** | `03_App_Movil/RetroV36/` | `03_App_Movil/RetroUsuario/` |
| **Propósito principal** | Calibración campo 8 y b con ZIP | Medición rutinaria y CSV |
| **Perfil de usuario** | Técnico autorizado DPI / Coviandina | Operador de la concesión vial |
| **Acceso a `#S` / `#SC`** | **Sí** (modo administrador bajo PIN) | **No** (solo lectura `#V#`, etc.) |
| **Veredicto previo** | APTO con cond. (Arq) / APTO (QA) | APTO (Arq) / QA 152/152 JVM |

---

## 2. Auditoría: App de Calibrar (`Cov_3.6.5_calibrar`)

### 2.1 Flujo y mecanismo de apertura del ZIP
- **Implementación (`CortoActivity.java:158-161`):**  
  El botón *"Cargar ZIP"* dispara `Intent(Intent.ACTION_OPEN_DOCUMENT)` con categoría `CATEGORY_OPENABLE`
  y tipo MIME `*/*`. Invoca el Storage Access Framework (SAF) de Android. Permite seleccionar el archivo
  desde Descargas, almacenamiento interno o Google Drive mediante un permiso puntual (`openInputStream`),
  sin solicitar el permiso peligroso de almacenamiento en tiempo de ejecución.
- **Configuración del manifiesto (`AndroidManifest.xml:29-32`):**  
  La aplicación únicamente declara un `<intent-filter>` con acción `MAIN` y categoría `LAUNCHER`.
  **No declara ningún `<intent-filter>` para `ACTION_VIEW` ni `ACTION_SEND` asociado a extensiones `.zip`
  o tipo MIME `application/zip`**.
- **Diagnóstico del incidente operativo (WhatsApp / Stickers):**  
  Al pulsar el archivo `.zip` directamente en WhatsApp, Android busca aplicaciones registradas para `.zip`.
  Al no tener filtro de recepción en el manifest, Android delega la apertura a la app asociada por defecto
  (en el caso de campo, una app de stickers de WhatsApp que procesa paquetes zip).  
  **Veredicto:** El diseño del software se apega a `RF-COV-02`, pero exige operativamente guardar primero
  el ZIP en el almacenamiento del dispositivo antes de cargarlo desde el botón de la app.

### 2.2 Integridad, trazabilidad y rechazos atómicos (Regla L-23)
- **Validación del contenedor (`CortoActivity.java:207-216`):**  
  Comprueba firma de datos (`esZip`), rechaza ZIPs ligeros (`esIncremental`) exigiendo el de soporte
  (`soporte_…zip`), y verifica la presencia obligatoria del diario (`diarioDeZip`).
- **Comprobación de identidad de hardware (`ImportadorCampana.java:84-125`):**  
  Garantiza de forma estricta la regla **L-23** (*calibración por equipo físico*):
  - Verifica que cada serie del diario coincida con la serie y MAC del equipo conectado
    (`mismaSerie(c, todo, s.equipo)` y `c.esDeEsteEquipo(s.mac)`).
  - Si el ZIP es de otro equipo, aborta atómicamente y no escribe nada en la campaña local (`:93-96`).
- **Compatibilidad de familia de firmware (`CortoActivity.java:218-220`, `AppCorta.java`):**  
  Compara la familia del diario con la del equipo conectado (`p.firmware().name()`). Impide mezclar
  escalas incompatibles (p. ej. cargar medidas de un V4 original en un V3.6).
- **Adopción de cola y banco:**  
  La base de datos local nace vacía (`ctx.getFilesDir()`). Al importar, adopta la cola del ZIP de
  soporte, erradicando desajustes de banco (`RF-APP-55` a `RF-APP-58`).

### 2.3 Seguridad metrológica en la escritura de hardware
- **Calibración automática de un botón (`CalibracionAutomatica.java` / `RF-COV-17`):**  
  El operador no manipula polinomios ni códigos. La app ejecuta la secuencia fija (códigos 8 y b).
- **Re-medida contra certificado (`RF-COV-12`):**  
  Aplica el criterio de aceptación a ±10 % contra el valor certificado del patrón (`VERIF-5-10`).
- **Gestión segura de fecha (`#SC` / `#GC#` — `RF-COV-18` y `RF-COV-19`):**  
  Lee obligatoriamente `#GC#` antes de `#SC`. Si la comunicación falla, aborta sin escribir. Si el
  operador rechaza o la re-medida no cumple, restaura únicamente la fecha previamente leída.

### 2.4 Hallazgos de auditoría (App Calibrar)
1. **[H-CAL-01] [Usabilidad] Ausencia de `intent-filter` para compartir ZIP:**  
   *Severidad:* Baja / Mejora de usabilidad.  
   *Detalle:* Exige al operador guardar el archivo en Descargas/Drive antes de abrir la app.
2. **[H-CAL-02] [Presentación] Fuga residual de identificadores numéricos:**  
   *Severidad:* Muy baja (condición documentada en `REVISIONES-Apps-V3.6.md:17-19`).  
   *Detalle:* En errores de comunicación (`FlujoCalibracion.java:1190,1230`), algunos mensajes
   pueden exponer el identificador del código al operador.

---

## 3. Auditoría: App de Usuario Final (`RetroUsuario 0.3.7`)

### 3.1 Arquitectura de ciclo de vida y concurrencia (FABLE-USR)
- **Desacoplamiento UI / Dominio (`RetroUsuario/README.md:21-35`):**  
  La versión 0.3.7 consolida la máquina de estados de medición en `dominio.EstadoMedida` y de detección
  en `dominio.EstadoDeteccion`.
- **Inmunidad ante rotación (`MedirActivity.java:70`, `MainActivity.java:79`):**  
  Los hilos de fondo no retienen referencias a elementos visuales de la Activity. Las publicaciones a la
  interfaz validan `!isFinishing() && !isDestroyed()`, erradicando caídas por `BadTokenException`.
- **Protección contra registros espurios de media 0 (`SerieDisparos.java:98-100`, `b8a299d`):**  
  Cierra la condición **C1** de la versión 0.3.6: al salir con el diálogo de cero abierto, emite
  `PreguntaOperador.Decision.ABANDONAR` y descarta la serie sin escribir filas inválidas (`SPEC §4 bis`).

### 3.2 Protección del hardware y enlace serie Bluetooth
- **Filtro estricto de conexión (`RF-USR-01`):**  
  Envía exclusivamente `#V#`. Si no inicia por `#V,3.6,`, clasifica el equipo como no compatible y corta.
  No envía `9`, `6`, `e` ni tramas `@LEERV...`, protegiendo firmwares de 2020 y V4.
- **Detección de enlace caído (`LectorDeFlujo.java`, `EstadoEnlace.java`):**  
  Monitorea excepciones y eventos EOF con `marcarCaido()`, evitando bucles de "no compatible".
- **Temporización defensiva:**  
  Pausa mínima de 150 ms entre tramas `#` consecutivas, respetando la UART del microcontrolador.

### 3.3 Metrología, geolocalización y formato de datos
- **Política de repetición (`RF-USR-04 r7`):**  
  Ningún reintento es automático ni silencioso; consulta siempre mediante `PreguntaOperador`.
- **Captura de coordenadas GPS (`PermisoUbicacion.java`, `RF-USR-15`):**  
  Permiso pedido en ejecución. Si no hay posición, continúa con `gps_estado = sin_posicion`.
  Coordenadas válidas con 6 decimales fijos.
- **Formato de exportación (`RF-USR-15 bis`):**  
  Genera `medidas.csv` según RFC 4180 (BOM UTF-8 `EF BB BF`, CRLF, comillas dobles). Exporta ZIP en
  `Download/RTV/` por MediaStore en API ≥ 29.

### 3.4 Hallazgos de auditoría (App Usuario)
1. **[H-USR-01] [Validación] Pendiente de prueba física en hardware real:**  
   *Severidad:* Informativa / Tarea de Roadmap (B7).  
   *Detalle:* Posee 152 pruebas unitarias JVM en verde (107 requisito / 45 comportamiento). Resta
   validación manual en dos teléfonos físicos (Android ≤9 y ≥10).

---

## 4. Cuadro comparativo de garantías y seguridad

| Aspecto de Seguridad | App Calibrar (3.6.5) | App Usuario (0.3.7) |
| :--- | :--- | :--- |
| **Daño a EEPROM** | Controlado con `#F,k#` | Nulo: no emite `#S` ni `#SC` |
| **Separación equipos** | Absoluta: MAC, serie, familia | Garantizada por serie y MAC |
| **Rotación pantalla** | Bloqueada / ciclo corto | Total: desacople `EstadoMedida` |
| **Permisos Android** | SAF (`*/*`) y Bluetooth | Ubicación y Bluetooth |
| **Archivos externos** | SAF (`ACTION_OPEN_DOCUMENT`) | Solo exporta |

---

## 5. Conclusiones y recomendaciones

1. **Sobre la APK de calibrar (`Cov_3.6.5_calibrar`):**  
   - Importar el `.zip` en software es **correcto y mandatorio** para la trazabilidad metrológica.  
   - Para evitar confusión con WhatsApp/stickers, el operador debe guardar el ZIP en Descargas/Drive
     antes de pulsar *Cargar ZIP* en la app.  
   - Se recomienda incorporar en versiones futuras un `<intent-filter>` para tipo MIME `application/zip`.
2. **Sobre la APK de usuario final (`RetroUsuario 0.3.7`):**  
   - Arquitectura desacoplada en `b8a299d` resuelve las condiciones críticas de ciclo de vida.  
   - Estado: **APTO DE CÓDIGO Y ARQUITECTURA**, lista para validación en dispositivo físico (B7).
