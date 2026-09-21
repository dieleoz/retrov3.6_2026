package com.dpi.retrousuario;

import com.dpi.retrousuario.dominio.ParametrosRitmo;
import com.dpi.retrousuario.dominio.SesionMedicion;

/**
 * Punto único de acceso a la sesión de medición en curso (RF-USR-04, RF-USR-05, RF-USR-06): {@link MainActivity} la
 * crea tras la sonda (RF-USR-01/02); {@link MedirActivity} y {@link AjustesActivity} la leen. Vive
 * mientras el proceso vive (un solo equipo conectado a la vez, sin hilos concurrentes que la
 * comparta); lo que sobrevive a la muerte del proceso es el diario en disco, no este objeto
 * (RF-USR-06, C5, T-USR-25).
 *
 * {@link #parametros()} es el **mismo** objeto para toda la app: cambiar `lecturasPorColor` en
 * {@link AjustesActivity} se ve de inmediato en la siguiente serie que mida {@link MedirActivity}
 * (T-USR-28), sin pasar por la pantalla de medir.
 */
final class SesionHolder {

    private static final ParametrosRitmo PARAMETROS = new ParametrosRitmo();
    private static SesionMedicion sesion;
    private static EnlaceBluetooth enlace;

    private SesionHolder() {
    }

    static ParametrosRitmo parametros() {
        return PARAMETROS;
    }

    static void establecer(SesionMedicion s, EnlaceBluetooth e) {
        sesion = s;
        enlace = e;
    }

    static SesionMedicion sesion() {
        return sesion;
    }

    static EnlaceBluetooth enlace() {
        return enlace;
    }
}
