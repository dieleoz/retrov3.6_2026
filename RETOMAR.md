# RETOMAR — dónde se quedó el trabajo

**Los códigos 8 y b de SLV-002 siguen sin escribir: la app para hacerlo está entregada y falta usarla.**
Estado vigente; se reescribe en cada sesión. Reglas en `CLAUDE.md`; orden en `ROADMAP.md`; decisiones en
`06_Calibracion/SLV-002/DECISIONES-Diego-2026-09-19.md`. Mapa: `ARQUITECTURA.map` §M2 y §M1.

## Estado (21-sep-2026, tarde)

- **SLV-002 (Coviandina):** firmware 3.6.2; códigos 1 y 2 con acta aceptada (vence 2027-09-19); serie
  `SLV-002`, `SLV-002-2026` decidida y sin grabar. ZIP del banco (18:11, md5 `79b23590…`) archivado.
- **App de calibrar (empresa): `Cov_3.6.5_calibrar`**, `com.dpi.retrov36.calibra`, versionCode 10010,
  md5 `dc0beaf5e110a72b7cbb288e4011cb68`, SHA-256 `770bd7c3…2a55147`. Rama `rtv-1.0-cierre`, `316a6bc`.
  QA APTO; arquitecto APTO CON CONDICIONES (mensajes de fallo con número de código, antes de darla a un
  operador de Coviandina). **Entregable a Diego** en `03_App_Movil/Cov_3.6.5_calibrar.apk`. Cargar el ZIP →
  un botón "Calibrar" → re-medida ±10 % frente al certificado → fecha del día (se lee antes y se relee).
- **App de usuario: `RetroUsuario` 0.3.3** (`com.dpi.retrousuario.coviandina`, versionCode 6), rama
  `worktree-agent-a0b5ff37e1945291c` (`09d6130`, se sube como `retro-usuario`). Incremento 1 "Medir y
  exportar". Arquitecto y QA: APTO CON CONDICIONES (`05_Documentacion/REVISIONES-Apps-V3.6.md`).
  **0.3.4 en curso:** `detectando` tras Atrás, resultado tras giro y prueba del lector con equipo caído.
  Incremento 2 ("señal a señal", indicador por norma, inventario) sin programar.
- **SPEC al día con el código:** calibrar, `SPEC-App-Calibracion-Coviandina.md` §8 (RF-COV-11 a 23);
  usuario, `SPEC-App-Usuario-V3.6.md` r6 + §4 bis.

## Pendiente de Diego

1. **Calibrar el 8 y la b** con `Cov_3.6.5_calibrar` y el ZIP de las 18:11; traer el ZIP que salga.
2. **Subir las ramas** (el sistema bloquea el push a los subagentes):
   `git -C .claude/worktrees/agent-abbfb74fdaea81269 push origin rtv-1.0-cierre` y
   `git -C .claude/worktrees/agent-a0b5ff37e1945291c push origin worktree-agent-a0b5ff37e1945291c:retro-usuario`.
3. Probar la 0.3.4 en dos teléfonos (Android ≤9 y ≥10): exportar, girar, cambiar de equipo, salir con
   Atrás y apagar el equipo a mitad de sesión.
4. Decisiones abiertas del `ROADMAP.md` (D-13, D-16, D-3, D-4, D-5, D-8) y las propuestas ▸ de la SPEC
   de usuario (geometría del equipo, umbral doble, cero, 3.6.2 obligatoria).

## Prompt para retomar

```
Retomamos la V3.6 (D:\IT\P_RetroVertical_V3.6, github.com/dieleoz/retrov3.6_2026). Lee CLAUDE.md,
RETOMAR.md y ROADMAP.md; comprueba con git log. Dos apps: calibrar (Cov_3.6.5_calibrar, entregada) y
usuario (RetroUsuario 0.3.x). Siguiente: el resultado de Diego al calibrar el 8 y la b; revisión de la 0.3.4.
```
