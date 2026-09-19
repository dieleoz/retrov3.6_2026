# ROADMAP — V3.6: qué se hace y en qué orden

**Actualizado:** 18-sep-2026. Lo que no está aquí no está en ejecución. **Nada se ha grabado ni
probado en un equipo con V3.6.**

## Objetivo

Dejar los **dos equipos V3** (SLV-002 y el segundo) con firmware **V3.6**: la calibración pasa a
EEPROM y se ajusta **desde la app, en modo administrador**, sin volver a reprogramar. La pantalla
STONE y la app del cliente siguen funcionando igual. Después, los dos V4, en su propio repositorio.

## Puertas: ninguna se salta

| Puerta | Qué tiene que cumplirse | Estado |
| :--- | :--- | :--- |
| **P1 — Especificación** | `SPEC-V3.6` escrita: requisitos de firmware y app, paridad con la app de campo, modo de pruebas y criterios de aceptación | Subagente en curso |
| **P2 — Arquitectura** | Revisión adversaria de SPEC, PROTOCOLO y código: **aprobado**, o aprobado con condiciones ya resueltas | Tras P1 |
| **P3 — Compilación reproducible** | XC8 2.10 compila la base 2020 **idéntica** a `d089f962`. Si no, se decide por escrito si se acepta la diferencia | **Cerrada, 18-sep-2026.** Sale `d089f9625090a1213291c090eae7ac01` idéntico (revisor de arquitectura; flags en su informe) |
| **P4 — Firmware V3.6** | `.hex` compilado. Los coeficientes de fábrica reproducen las 12 ecuaciones de 2020. `CAMBIOS-V3.6.md` revisado | Subagente firmware |
| **P5 — App V3.6** | APK compilado; tests de inversión y ajuste en verde; **modo de pruebas** que comprueba el equipo antes de dejar calibrar | Subagente app |
| **P6 — Autorización** | Visto bueno del propietario del equipo a reprogramarlo, sabiendo que el original **no se puede recuperar** (chip protegido) | **Cerrada el 18-sep-2026 por Diego**: *"daña el firmware y descarga para ver cómo responde este PIC de la versión perdida de 3.3"*. Acepta perder el original |
| **P7 — Grabación** | Grabar con el PICkit 3 (IPE 5.50), leer para verificar y pasar el modo de pruebas de la app. **P2 ya dio su veredicto (APROBADO CON CONDICIONES, 18-sep-2026 19:48), así que ya no se graba "sin esperar a P2"**: se graba cuando se cumplan **G1-G5**: G1 commit del fuente y `.hex` atado a ese commit con md5; G2 T-A20 con 0 diferencias en los 12 códigos; G3 línea base de SLV-002 tomada antes de grabar; G4 verificación con IPE y T-C03/T-C04; G5 documentos reconciliados. Así se reconcilia con SPEC §6 y con el README. P6: **la placa es de un cliente** (Concesionaria Vial Andina) y Diego lo asume | Espera G1-G3 |
| **P8 — Calibración** | Medir P1-P31 con `e`, ajustar por color en la app, escribir los coeficientes y volver a medir. Acta de antes y después | Tras P7 |

## AHORA

1. SPEC y revisión de arquitectura (P1, P2).
2. Firmware (P3, P4) y app (P5), en paralelo.
3. Mapa del repositorio: `ARQUITECTURA.map`, subagente en curso.
4. Mejoras de la app: se recogen del repositorio V5 (`MEJORAS.md`, SPEC-05/06/07, TDD-01) en
   `ROADMAP-MEJORAS-App.md`. **No bloquean la V3.6**: van después.

## Datos de SLV-002 ya tomados (sirven antes y después de grabar)

8 blancos por pantalla (x de 1739 a 3271; P4 en el techo). **La `x` depende del sensor, no de las
fórmulas.** Seguirá valiendo tras grabar **sólo si la adquisición, el `+200` y el factor de temperatura
de la variante grabada son los del fuente 2020**. No se puede verificar antes de grabar (C-01 de la
SPEC); se comprueba después con T-C08. *Antes decía "sigue valiendo" sin esa condición.* Con la `x` y las fórmulas de 2020 se reconstruye además el estado "como llegó" del equipo.

## Después de la V3.6

- **Segundo V3:** barrido, medida y grabación, con el mismo método.
- **Los dos V4:** identificar primero si llevan v4.0 o V4.1. Repositorio `P_RetroReflectometro_Vertical`.
- Mejoras de la app según `ROADMAP-MEJORAS-App.md`.
