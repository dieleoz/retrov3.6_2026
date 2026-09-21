package com.dpi.retrousuario;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static final String PREFS = "retrousuario_ajustes";
    private static final String CLAVE_LECTURAS_POR_COLOR = "lecturasPorColor";

    private static final ParametrosRitmo PARAMETROS = new ParametrosRitmo();
    private static SesionMedicion sesion;
    private static EnlaceBluetooth enlace;
    /** M-1 (giro): el aviso previo (T-USR-02) se acepta una sola vez por proceso, no por Activity. */
    private static boolean avisoAceptado = false;

    private SesionHolder() {
    }

    /** M-1: true si el operador ya aceptó el aviso previo en este proceso (no vuelve a salir tras un giro). */
    static boolean avisoAceptado() {
        return avisoAceptado;
    }

    static void marcarAvisoAceptado() {
        avisoAceptado = true;
    }

    static ParametrosRitmo parametros() {
        return PARAMETROS;
    }

    /**
     * A2 (ALTO): invalida la sesión y el enlace anteriores (al cambiar de equipo, antes de conectar
     * al nuevo). No cierra el socket aquí (eso lo hace quien tenga la referencia real al enlace,
     * MainActivity, antes de llamar a esto): esto sólo quita la referencia compartida, para que
     * {@link MedirActivity}/{@link AjustesActivity} dejen de ver la sesión vieja de inmediato.
     */
    static void limpiar() {
        sesion = null;
        enlace = null;
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

    /** B3: `lecturasPorColor` sobrevive a la muerte del proceso (SharedPreferences), no sólo a que
     *  la Activity se recree. Se llama una vez, al arrancar (MainActivity.onCreate). */
    static void cargarAjustesPersistidos(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int valor = prefs.getInt(CLAVE_LECTURAS_POR_COLOR, PARAMETROS.lecturasPorColor());
        if (valor >= 1) {
            PARAMETROS.lecturasPorColor(valor);
        }
    }

    /** B3: guarda `lecturasPorColor` (AjustesActivity, tras validar el valor). */
    static void guardarLecturasPorColor(Context contexto, int valor) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(CLAVE_LECTURAS_POR_COLOR, valor)
                .apply();
    }
}
