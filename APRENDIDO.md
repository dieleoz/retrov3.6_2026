# APRENDIDO — V3.6

Una entrada por lección. El proyecto siguiente la trae como `APRENDIDO-DE-V3.6.md`; las de la V3.6
anteriores al Orquestador están en `D:\IT\P_RetroVertical_V4.6\APRENDIDO-DE-V3.6.md` (L-01 a L-23).
Los errores concretos, con la errónea junto a la buena, en `HISTORIA.md`.

## A-01 — La app que lleva un teléfono se mira en su registro, no en la memoria
- **Pasó:** el procedimiento del 8 y la b decía "quédese con la 3.6.17" y el HONOR llevaba la rc3
  (`06_Calibracion/SLV-002/tramas/rtv36_20260919_191255_HONOR.txt:5`).
- **Lección:** antes de una instrucción de campo, la cabecera `# app:` del último registro de ese
  teléfono. Si una instalación falla, no se desinstala: se pierde la campaña.
- **Destino:** SUBE a `entregar` (instrucciones de campo con APK).

## A-02 — Un ZIP del que sale una curva se archiva el mismo día
- **Pasó:** el ZIP de soporte que sostiene el informe de ajuste estaba sólo en `07 pruebas/`, sin
  versionar (`06_Calibracion/SLV-002/campanas/HUELLAS.txt`, entrada del ZIP de soporte).
- **Lección:** lo que no está en el repositorio con su huella no existe como evidencia.
- **Destino:** SUBE a `entregar` §9.

## A-03 — Tomar datos para calibrar no es construir una curva
- **Pasó:** la toma creció a bancos de 80 y 180 pasos, series de 9 disparos y modo "preciso"; más de
  una hora por equipo y una app que un usuario no entiende (decisiones TOMA-50 y APP-SIMPLE).
- **Lección:** la curva (fórmula por color) se construye una vez con el banco completo; en cada
  equipo basta una toma corta y representativa. Tope: 50 tomas.
- **Destino:** SE QUEDA (y a la V4.6 por su `APRENDIDO-DE-V3.6.md`).

## A-04 — Una prueba que asevera no demuestra nada si el valor esperado salió del código
- **Pasó:** tres casos (`HISTORIA.md`, "Pruebas que no demostraban nada").
- **Lección:** cuenta el valor esperado que viene de fuera; al entregar, recuento separado y prueba
  nueva vista en rojo.
- **Destino:** SUBE a `verificar` §4 (hoy sólo en `CLAUDE.md` §7 de este repositorio).

## A-05 — Renumerar las secciones de `CLAUDE.md` rompe las citas `§n`
- **Pasó:** al reordenar `CLAUDE.md`, las skills y `HISTORIA.md` quedaron citando secciones que ya no
  eran las mismas; se corrigieron a mano.
- **Lección:** citar la regla por su título, o no renumerar.
- **Destino:** SUBE a `cerrar-sesion`.

## A-06 — Topes del método frente a los del proyecto
- **Pasó:** `medir_docs.sh` y el pre-commit ponen 1000 líneas a README y ROADMAP; Diego fijó 100 aquí
  (`.topes`). `medir_docs.sh` no lee `.topes`.
- **Lección:** el medidor y el hook deben leer los mismos topes.
- **Destino:** SUBE a `cerrar-sesion` (`medir_docs.sh`).

## A-07 — Contradicciones internas en las skills comunes
- **Pasó:** al pasar lo propio a `.claude/particularidades/` salieron: `entregar` §0 dice que en las
  particularidades va "el estado de hoy" y a la vez que la cifra del día no vive allí;
  `leer-planos-pcb` §0.3 y §6 llevan un estado fechado y cifras de otro proyecto;
  `verificar-pantalla-stone` no explica el formato del `.vt` de 2.ª generación.
- **Destino:** SUBE a `entregar`, `leer-planos-pcb` y `verificar-pantalla-stone`.
