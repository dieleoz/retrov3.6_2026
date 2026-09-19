# SLV-002 — lecturas por pantalla antes y después de grabar la V3.6

Equipo SLV-002 (Concesionaria Vial Andina). Pantalla, OTROS PAPELES → BLANCO (código 1, ecuación
blanco intenso de fábrica, idéntica en los dos firmwares: T-A20 y barrido de 255 bytes). `x` = lectura
interna invertida con `-0,000086541·x² + 0,619668128·x − 303` (`ecuacionesCalibracion.c:6-8` de la base).

| Patrón | Tipo | Cert. | Antes (firmware original, 18-sep) | Después (V3.6, 18/19-sep) | x antes | x después | Δx |
| :--- | :---: | ---: | :--- | :--- | ---: | ---: | ---: |
| P3 | IV | 378 | 513 / 512 / 514 → 513 | 488/488/491 → 489 · 480/485/486 → 483 · 489/492/490 → 490 | 1739 | ~1660 | −80 |
| P2 | IV | 414 | 623 / 623 / 625 → 623 | 600 / 600 / 601 → 600 | 2125 | 2036 | −89 |
| P28 | IX | 484 | 658 / 659 / 658 → 658 | 635 / 639 / 638 → 637 | 2271 | ~2182 | −89 |
| P1 | XI | 762 | 780 / 780 / 780 → 780 | 769 / 780 / 781 → ~780 | 3029 | ~3029 | ~0 |
| P7 | XI | 768 | 790 / 790 / 790 → 790 | 789/790/790 → 789 (18-sep) · 798/798/798 → 798 (19-sep) | 3147 | 3134 / 3271 | −14 / +124 |

**Hallazgo.** Con las mismas fórmulas, la V3.6 lee unas **80-90 cuentas menos de `x` en la zona
x = 1700-2300** (sólido: P3, P2 y P28, varias tandas). **En la zona alta (x ≥ 3000) no es concluyente
por pantalla:** allí la ecuación del blanco está cerca de su techo y 1 punto de pantalla son 13-19
cuentas de `x`, así que P7 osciló entre 789 y 798 según la tanda. *Primera versión de esta acta:
"prácticamente lo mismo en x ≥ 3000"; retirado el 19-sep-2026 con la tanda de P7 de 798.* Para medir
la zona alta con precisión hay que usar `e` (ya existe en la V3.6), no la pantalla. No es un desplazamiento fijo ni un factor.
Recolocar el equipo varía ~7 puntos de pantalla en P3; la diferencia es ~25, repetida en 3 tandas y
dos días. **Causa sin cerrar (C-01):** la adquisición de la variante original (tiempo de LED, muestreo,
`+200`, factor de temperatura) no coincide con la del fuente 2020 en que se basa la V3.6, y no puede
comprobarse porque el original estaba protegido y se borró. Hipótesis: tiempo de exposición del LED
distinto; el historial de 2013 ya decía que el sensor "se comporta de modo incremental" con la
exposición. En la zona alta el sensor se acerca a su límite y la diferencia desaparece.

**Consecuencia.** La calibración de la V3.6 se hace sobre las lecturas de la V3.6, así que la
diferencia queda absorbida. Frente al certificado, la V3.6 queda **más cerca** en IV/IX que el original.
Los valores de "antes" son el acta del estado de llegada.

## G4 — pruebas por Bluetooth tras grabar (19-sep-2026 08:53, app 3.6.2)

- `#V#` → `#V,3.6,2026-09-18,DEF,0000#`. `#GT#` y los 12 `#G` iguales a fábrica.
- **`#E`: 60 coincidencias exactas de 60** (12 códigos × x = 500, 1000, 2000, 3000, 4000), en dos
  pasadas. El PIC real calcula como el firmware de 2020 (confirma T-A20 en hardware).
- Los 13 comandos responden en ~1 s. **`e` funciona** (V3.6).
- Repetibilidad con `e` sobre **P1** (XI, 762): 3016 / 3022 / 3027 / 3020 / 3023 → media 3021,6, s = 4,0 cuentas.
- Prueba 4 (coherencia) dio FALLO: en la primera pasada el equipo se apoyó a mitad de la prueba
  (códigos 1-8 en oscuro, x ≈ 575); en la segunda, 11/12 dentro y el código 1 a −22,5 cuentas, en la
  zona cercana al techo de su ecuación, con deriva de `x` de 3004 a 3027 durante la prueba. **Es del
  criterio de la app, no del equipo.**
- **Cerrado el mismo día:** la prueba se hizo sobre **P1**, no sobre P7 como se anotó primero. P1 por
  Bluetooth, código 1 = 776 (x ≈ 2993); `e` = 3016-3027 (x ≈ 3022); por pantalla con gatillo, ese día,
  780/781 (x ≈ 3029). **Gatillo y Bluetooth miden lo mismo**, dentro del ruido cerca del techo de la
  ecuación del blanco. *Versión anterior de esta línea: "P7 por Bluetooth = 776 frente a 798 por
  pantalla; sospecha de que el gatillo mueve el cabezal" — retirada: comparaba patrones distintos.*
