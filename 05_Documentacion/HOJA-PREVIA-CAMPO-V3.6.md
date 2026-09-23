# Hoja Previa de Campo — Retrorreflectómetro Vertical V3.6

Lista de comprobación obligatoria para el operador o técnico antes de iniciar cualquier sesión de medición
o calibración con el instrumento en campo o laboratorio.

---

## Comprobaciones Previas Obligatorias

- [ ] **1. PIN de Administración en Mano y Advertencia de Bloqueo**
  - Tenga copiado el PIN de administración suministrado formalmente por DPI (no lo introduzca de memoria).
  - **Advertencia estricta:** Al acumular cinco (5) intentos consecutivos erróneos de PIN, el firmware del
    instrumento entra en bloqueo permanente de seguridad (`#ERR,BLOQUEADO#`), obligando a reiniciar el equipo.

- [ ] **2. Verificación de la Aplicación Instalada en el Teléfono**
  - Verifique qué aplicación lleva el teléfono consultando la cabecera `# app:` del último registro exportado:
    - **Operador de campo (Medición de señales):** Aplicación `Retro Coviandina` (versión 0.3.7, código 10).
    - **Técnico de laboratorio (Calibración metrológica):** Aplicación `RTV Calibra` (versión 3.6.5, código 10010).
  - Nunca intente calibrar con la aplicación de usuario ni realizar campañas con la de calibración.

- [ ] **3. Ubicación del Archivo ZIP en el Almacenamiento Local**
  - El archivo ZIP del banco de patrones o de campaña debe residir físicamente en la memoria interna del teléfono
    (carpeta `Descargas` o almacenamiento local).
  - **Prohibido:** Intentar abrir el archivo desde un chat de mensajería (WhatsApp, Telegram o correo web). La
    aplicación accede mediante el selector de documentos del sistema Android y no recibe archivos compartidos.

- [ ] **4. Orden Riguroso de Encendido y Secuencia de Apagado**
  - **Secuencia de encendido:** Encienda el retrorreflectómetro vertical -> Espere a que la pantalla táctil STONE
    muestre la interfaz principal lista -> Active el Bluetooth del teléfono -> Abra la aplicación -> Conecte.
  - **Secuencia ante apagado:** Si la aplicación solicita apagar el instrumento (por ejemplo durante la rutina de
    re-medición), apague físicamente el equipo en su interruptor, espere a que la app detecte la pérdida del enlace
    Bluetooth y sólo entonces pulse **Aceptar** en pantalla. La aplicación evalúa el estado del hardware, no la
    intención del operador.

- [ ] **5. Exportación Mandatoria antes de Cerrar la Aplicación**
  - Al concluir la jornada o serie de mediciones, pulse siempre el botón **Exportar** para compilar el paquete ZIP
    con los registros (`medidas.csv`, `inventario.csv` y `tramas.log`).
  - **Prohibido:** Forzar el cierre de la aplicación o expulsarla de memoria mientras hay una sesión abierta. Forzar
    el cierre destruye las actas técnicas y registros en curso.

- [ ] **6. Regla Estricta contra la Desinstalación de la Aplicación**
  - Bajo ninguna circunstancia desinstale la aplicación existente para instalar una versión nueva.
  - En Android, desinstalar borra de inmediato el almacenamiento privado de la app y destruye todo el historial.
  - Instale siempre el nuevo archivo APK directamente encima de la versión previa.
