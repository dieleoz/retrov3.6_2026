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

- Al empezar el día: `main` en `fbfa43e`, rama `rtv-1.0` en `72d00cd`, local y remoto iguales. Rama local nueva `rtv-1.0-cierre` (sin subir).
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

- abrió la campaña de SLV-002 **con 0 series**. La ruta del fichero es la misma en la 3.6.9, la rc3 y
  la rc6 (`Campanas.java:66,82`), así que **no es una migración**: los datos de la app se borraron
  entre las 12:27 y las 19:12 (desinstalación o "borrar datos"). Es hipótesis, sin registro del
  borrado. El acta de los códigos 1 y 2 sigue en el ZIP de las 12:27; la app no la necesita, porque
  las curvas del 1 y del 2 van fijas en el APK (`TablaCalibracion.java:234-246`);
- importó el ZIP de las 18:11 en una campaña de cola **COMPLETO** (129 series, 69 pasos hechos,
  "quedan 64 patrones"): la trampa del ZIP representativo dentro de una campaña completa. Así, los
  OSCURO de sesión no tienen paso y el 8 no se podría anclar;
- pruebas del equipo: **la primera tanda NO APTO, la segunda APTO** (l.586 y l.1170);
- batería con n = 0: escrituras bloqueadas. Se envió `#L`; la recta anclada del 8 que se vio es la
  **vista previa de Avanzado** (ancla en la serie S123, x = 571), **no** la del flujo "Calibrar", que
  ancla en la media de los OSCURO de sesión (569,08 según el informe). Cerró con `#Q`. **No se envió
  `#S`.**

Corregido el mismo 21-sep (queda la errónea junto a la buena): en `a6174dc` este fichero decía
"pruebas NO APTO, sin explicar" y daba la recta anclada de Avanzado como "la del 8". Lo cerró el
análisis de la rc6 releyendo el registro y `AdminActivity.java:522-535`.

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

## Hecho el 21-sep por la mañana

1. **Análisis rc6 sobre rc3 en el HONOR** (sólo lectura). Veredicto: la firma de 3.6.8 a rc6 es la
   misma (`~/.android/debug.keystore`); la de la rc3 no se puede comprobar porque su APK no está en el
   disco. Si Android rechaza la instalación, **no desinstalar**. Camino limpio de la campaña, igual en
   rc4 y rc6: "ZIP de soporte (todo)" → "Nueva campaña (archiva la actual)" → **sin abrir el Banco**,
   importar el ZIP de las 18:11 → comprobar REPRESENTATIVO con 80 pasos hechos. **Recomienda la rc4**
   (`RTV-V1.0.0-rc4.apk`, md5 `a41fb09c…`, en `D:\IT\wt_rtv10wt_rtv10_App_Movil3_App_Movil\`) tras arquitecto y
   QA del salto rc2 → rc4. Trae una lista de once cambios al procedimiento, sin aplicar.
2. **App corta cerrada en la JVM**, rama local `rtv-1.0-cierre` (`0a849fe`, `7faaf38`), 360 pruebas.
   Arregla RF-COV-09: `Familia.compatibles` deja pasar una familia desconocida (`Familia.java:88`),
   así que una campaña vacía admite un ZIP de otra familia. **Esto sigue igual en la app de campo.**
   Sus APK repiten `1.0.0-rc6`/10005 con otro binario: hay que subir la versión.

## Lo siguiente

1. Diego decide versión (recomendado rc4) y teléfono (HONOR). Se aplican los cambios al procedimiento.
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
