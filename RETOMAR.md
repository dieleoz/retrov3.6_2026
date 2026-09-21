# RETOMAR — dónde se quedó el trabajo

**Puesto al día:** 21-sep-2026, por la mañana. Entre la noche del 19-sep y hoy **no se hizo nada**
(Diego): no se midió, no se grabó y no hay ZIP fuera del repositorio. Repositorio
`D:\IT\P_RetroVertical_V3.6` (remoto `github.com/dieleoz/retrov3.6_2026`). Reglas en `CLAUDE.md`;
orden de trabajo en `ROADMAP.md`; procedimiento del 8 y la b en
`06_Calibracion/SLV-002/PROCEDIMIENTO-Escribir-8-y-b.md`.

**Alcance de este repositorio:** sólo Coviandina (SLV-002) y la app. Nordeste es de la V4.6
(`D:\IT\P_RetroVertical_V4.6`); Ruta al Mar (SLV-028) tiene proyecto propio
(`D:\IT\P_RetroVertical_RutaAlMar`). No se tocan desde aquí.

## Estado (verificado con `git log` y los registros, 21-sep)

- `main` en `fbfa43e`; rama `rtv-1.0` en `72d00cd`. Local y remoto coinciden.
- **SLV-002** lleva el firmware **3.6.2**. **Códigos 1 y 2 calibrados**, acta ACEPTADA 12:23:26 del
  19-sep (ZIP de las 12:27, hecha en el HONOR con la 3.6.9), vence 2027-09-19. Serie en EEPROM
  `SLV-002`; `SLV-002-2026` decidida y **sin grabar** (se graba **después** de calibrar el 8 y la b).
- **Banco representativo completo** (80 pasos HECHO), ZIP de soporte de las 18:11, md5 `79b23590…`,
  **medido en el Samsung SM-S918B** (3.6.11 → 3.6.16 → 3.6.17). Desde hoy está en
  `06_Calibracion/SLV-002/campanas/` con su huella. De él sale la curva:
  `INFORME-Ajuste-SLV-002-20260919-1811.md`. La b ya no necesita la dispensa PA-24.
- **Códigos 8 y b: sin escribir.** Nada se escribió en SLV-002 después del acta de las 12:23.

### Dato que cambia el procedimiento: qué app lleva cada teléfono

| Teléfono | Última app vista | Evidencia |
| :--- | :--- | :--- |
| **HONOR ABR-LX3** (el de Coviandina) | **`1.0.0-rc3` (10002)**, **no** la 3.6.17 | `06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt`, md5 `fce014de…` |
| Samsung SM-S918B | 3.6.17 a las 16:40; `1.0.0-rc3` a las 18:32 (con otro equipo); rc5 después, según Diego | ZIP de las 18:11; `Descargas/rtv36_20260919_183221.txt` |

Lo que dice el registro del HONOR de las 19:12, con la rc3:

- abrió la campaña de SLV-002 **con 0 series**: la campaña de la mañana, la del acta, **no la ve**;
- importó el ZIP de las 18:11 en una campaña de cola **COMPLETO** (129 series, 69 pasos hechos,
  "quedan 64 patrones"): la trampa del ZIP representativo dentro de una campaña completa;
- pruebas del equipo **NO APTO** (coherencia de fórmulas y repetibilidad de `e`), sin explicar aún;
- batería con n = 0: escrituras bloqueadas. Se envió `#L`, se previsualizó el 8 con recta anclada y
  se cerró con `#Q`. **No se envió `#S`.**

Consecuencias: el HONOR ya no puede volver a la 3.6.17 sin desinstalar, y desinstalar borra la
campaña. La rc3 escribe en el acta `firmado por Firmado por "…"` (arreglado en la rc4, `23c5fef`).
**De la rc3 a la rc6 ninguna tiene revisión de arquitecto ni de QA**; la última revisada es la rc2
(`REVISION-QA-RTV-1.0.0-rc2.md`, en la rama). La recomendación del procedimiento, "quédese con la
3.6.17", **ya no aplica a ese teléfono**: está en revisión qué hacer.

### Apps

- **RTV 1.0.0-rc6** (`fd37cc7`, 10005, 345 pruebas): ZIP de otro banco, guarda por familia y ZIP de
  soporte al terminar el banco. Sin arquitecto ni QA.
- **App corta "RTV Calibra"** (`72d00cd`), interrumpida al apagar: 358 pruebas, 9 aseveran un
  requisito. APK aparte (`applicationId` propio). **No entregable**: sus APK se compilaron sobre
  código sin guardar, dos pruebas nunca se han visto en rojo, le faltan arquitecto y QA.
- La 3.6.17 fue rechazada por arquitecto (P16) y QA; no se entrega.

## En curso (21-sep)

1. **Análisis rc6 sobre rc3 en el HONOR** (subagente, sólo lectura): si la rc6 conserva la campaña
   al instalarse encima, qué cambia en el camino del 8 y la b, qué código sin revisar lo toca, por
   qué la rc3 no ve la campaña de la mañana y qué hacer con la cola COMPLETO. **Diego no calibra
   hasta tener esto.**
2. **Cerrar la app corta** (subagente, worktree sobre `rtv-1.0`): recompilar sobre árbol limpio, ver
   en rojo las dos pruebas pendientes, prueba propia de RF-COV-09.

## Lo siguiente

1. Con el análisis: decidir teléfono y versión (rc4, rc6 o app corta) y corregir el procedimiento.
2. Arquitecto y QA de la versión elegida. Sin los dos vistos buenos no se entrega.
3. Diego calibra: **el 8 y su acta; la b después**, con recta anclada en oscuro. Cambiar batería si
   la orden 9 da n = 0. Si la re-medida falla con x a 0,5-3 % y la R sale bien, es el umbral
   (procedimiento §5.1), no la curva.
4. Grabar la serie `SLV-002-2026`, después de calibrar.
5. ZIP de soporte al repositorio con su huella.

Los códigos 3 y 5 no se arreglan midiendo: su dato está invertido (ver informe de las 18:11).

## Prompt para retomar

```
Retomamos la V3.6 del Retrorreflectómetro Vertical. Repositorio D:\IT\P_RetroVertical_V3.6
(github.com/dieleoz/retrov3.6_2026). Lee CLAUDE.md, RETOMAR.md y ROADMAP.md, y comprueba con git log
antes de citar. Alcance: sólo Coviandina (SLV-002) y la app. SLV-002: firmware 3.6.2, códigos 1 y 2
con acta aceptada (vence 2027-09-19); 8 y b sin escribir; banco completo en el ZIP de las 18:11
(md5 79b23590…). El HONOR de Coviandina lleva la rc3 (10002), no la 3.6.17: ver la tabla de
teléfonos de RETOMAR. A Diego sólo se le entrega una APK con arquitecto y QA escritos.
```
