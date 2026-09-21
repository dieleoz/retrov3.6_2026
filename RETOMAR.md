# RETOMAR — dónde se quedó el trabajo

**Los códigos 8 y b de SLV-002 siguen sin escribir.** Estado vigente; se reescribe en cada sesión.
Reglas en `CLAUDE.md`; orden en `ROADMAP.md`; decisiones en
`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`. Mapa: `ARQUITECTURA.map` §M2 y §M1.

## Estado (21-sep-2026)

- **Orquestador acoplado** (`a13dd24`): pre-commit con `.topes`, particularidades en
  `.claude/particularidades/`, `APRENDIDO.md`, `fuentes/INDICE.md`. Revisión `arquitecto-iot`: APTO
  CON CONDICIONES, cerradas en el mismo commit.
- **SLV-002 (Coviandina):** firmware 3.6.2; códigos 1 y 2 con acta aceptada (vence 2027-09-19);
  serie en EEPROM `SLV-002`, `SLV-002-2026` decidida y sin grabar.
- **Banco:** el ZIP de soporte de las 18:11 del 19-sep (md5 `79b23590…`) está archivado en
  `06_Calibracion/SLV-002/campanas/` y basta para calibrar el 8 y la b. **No hay que volver a medir.**
- **Rumbo nuevo (decisiones del 21-sep):** las apps de calibración son de DPI y van por USB
  (APPS-DPI). Tres piezas: **Toma** (hasta 50 tomas, TOMA-50), **Calibra** (cargar ZIP, calibrar con
  fecha y operador, verificar 5-10 referencias, VERIF-5-10) y el **"Certificado de calibración"** que
  firma ITVIAL SAS (CERT-TITULO). Sustituye al camino "rc3/rc4 + procedimiento largo" del 19-21 sep:
  ese procedimiento y sus once cambios quedan sin aplicar.
- **Apps:** la app corta RTV Calibra está en la rama `rtv-1.0-cierre` (`7faaf38`, en GitHub): carga
  el ZIP y calibra, pero no emite PDF ni tiene arquitecto ni QA. Base para la tarea 3 del ROADMAP.
- **Teléfono de Coviandina:** HONOR con la rc3 (10002). Calibra se instala al lado, con su propio
  paquete, sin tocar esa campaña.

## Datos que ya están para la SPEC

- Formato del certificado: la hoja `lista de calibracion retros.xlsx` de 2018 (VALOR ESPERADO /
  LLEGADA / SALIDA / Error %), en `D:\@Proyect\IT\old\VERTICAL\1_V2-V3_18F4550_CCS\`; contenido
  mínimo en `SPEC-Calibracion-V3.6.md` RF-CAL-32 (el título lo cambia CERT-TITULO).
- Toma: propuesta de 36 tomas + 14 de reserva, por código, del cruce de los Excel de 2018 con el
  firmware (2018 usaba ~45 patrones × 3 disparos × 1 colocación, rectas por tramos). Los códigos a, c
  y d no tienen curva propia en 2020 (`ecuacionesCalibracion.c:32=10, 38=16, 41=19`).

## Pendiente de Diego

D-16 (¿TOMA-50 anula PROTOCOLO-AJUSTE y TIPO-I-REPETIR? ¿curva propia para a, c y d?) y las de la
tabla del ROADMAP.

## Prompt para retomar

```
Retomamos la V3.6 (D:\IT\P_RetroVertical_V3.6, github.com/dieleoz/retrov3.6_2026). Lee CLAUDE.md,
RETOMAR.md y ROADMAP.md; comprueba con git log. Objetivo: certificar SLV-002. Siguiente: SPEC de
Calibra con el "Certificado de calibración" y sus pruebas (tarea 1), arquitecto-iot sobre ella, y
desarrollo sobre la rama rtv-1.0-cierre. El ZIP del banco ya existe; no se vuelve a medir.
```
