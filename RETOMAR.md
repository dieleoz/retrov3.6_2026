# RETOMAR — dónde se quedó el trabajo

**Cierre de sesión:** 18-sep-2026, 20:40. Repositorio `D:\IT\P_RetroVertical_V3.6` (remoto
`github.com/dieleoz/retrov3.6_2026`). Detalle en `ROADMAP.md` → "DÓNDE QUEDAMOS".

## Estado

- **SLV-002 (Concesionaria Vial Andina) GRABADO con la V3.6** a las 20:24: `.hex` md5 `680b6a7d…`,
  commit `f75ff88`. IPE dio *Program Succeeded*. El "Verify failed" posterior es normal, por la
  protección de código. **El firmware original se perdió** (autorizado).
- **G4 CERRADA el 19-sep-2026:** V3.6 detectada, coeficientes de fábrica, `#E` 60/60 exacto, 13 comandos y repetibilidad de 4 cuentas (acta en `06_Calibracion/SLV-002/`).
  diferencias frente a 2020, y el descarte del `#` suelto.
- App **RTV V3.6 3.6.2** (md5 `e5c6ffad…`) en `03_App_Movil\RTV-V3.6.apk`. No usar la 3.6.0.
- **Línea base "como llegó":** barrido de 255 bytes en oscuro y 8 blancos por pantalla
  (`06_Calibracion`, `ROADMAP.md`). **P4 por Bluetooth no se llegó a medir.**

## Lo primero mañana (G4: pruebas tras grabar)

1. **PICkit desconectado de la placa.** Apagar y encender el equipo. Comprobar que la pantalla arranca
   igual.
2. **No regresión en el equipo real:** OTROS PAPELES → BLANCO, 3 disparos. **P3 debe dar ~513 y P7
   ~790**, como antes de grabar.
3. **App 3.6.2 → Pruebas**, con el equipo sobre un patrón. Esperado: `#V#` → `V3.6 … DEF`; los 12
   códigos responden; la prueba 5 (`#E`) sin regresiones. Compartir el registro.
4. Opcional: **Botones de pantalla** (`#KC#`, pulsar un botón, `#K#`) para mapear la STONE de SLV-002.

## Después, para calibrar (P7-bis y P8)

- Cerrar en simulador **T-A23** (conversión `float` ↔ texto) y **T-A30** (límites de `#ST` sobre todo el
  rango de T). Hasta entonces **no ejecutar T-C32** ni escribir `#ST`.
- **Decisión pendiente de Diego:** contra qué tipo de lámina se ajusta la curva intensa del blanco y
  del amarillo (XI frente a IV/IX).
- Medir P1-P31 con `e` (ya existe en la V3.6) y ajustar en la app (grado+2 puntos); escribir con
  `#S`; volver a medir; acta.

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Lee D:\IT\P_RetroVertical_V3.6\RETOMAR.md y
ROADMAP.md ("DÓNDE QUEDAMOS"). SLV-002 quedó grabado ayer con la V3.6 (hex 680b6a7d) y falta
probarlo en el equipo (G4). Te paso los resultados de P3/P7 por pantalla y el registro de la app
3.6.2. Tú orquestas con subagentes; repos V3.6 y V4.6 son independientes del V5.
```
