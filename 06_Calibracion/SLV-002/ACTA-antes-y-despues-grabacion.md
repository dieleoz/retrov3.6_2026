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
| P7 | XI | 768 | 790 / 790 / 790 → 790 | 789 / 790 / 790 → 789 | 3147 | ~3134 | −14 |

**Hallazgo.** Con las mismas fórmulas, la V3.6 lee unas **80-90 cuentas menos de `x` en la zona
x = 1700-2300** y **prácticamente lo mismo en x ≥ 3000**. No es un desplazamiento fijo ni un factor.
Recolocar el equipo varía ~7 puntos de pantalla en P3; la diferencia es ~25, repetida en 3 tandas y
dos días. **Causa sin cerrar (C-01):** la adquisición de la variante original (tiempo de LED, muestreo,
`+200`, factor de temperatura) no coincide con la del fuente 2020 en que se basa la V3.6, y no puede
comprobarse porque el original estaba protegido y se borró. Hipótesis: tiempo de exposición del LED
distinto; el historial de 2013 ya decía que el sensor "se comporta de modo incremental" con la
exposición. En la zona alta el sensor se acerca a su límite y la diferencia desaparece.

**Consecuencia.** La calibración de la V3.6 se hace sobre las lecturas de la V3.6, así que la
diferencia queda absorbida. Frente al certificado, la V3.6 queda **más cerca** en IV/IX que el original.
Los valores de "antes" son el acta del estado de llegada.
