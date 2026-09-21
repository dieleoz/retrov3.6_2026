package com.dpi.retrousuario;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.dpi.retrousuario.dominio.EstadoGps;

/**
 * RF-USR-15/RF-USR-06 (M-8): última posición conocida, sin pedir un fix nuevo ni bloquear la medida.
 * Sin permiso o sin proveedor con posición: {@link Resultado#gpsEstado} queda en
 * {@code "sin_posicion"} (la fila se guarda igual, T-USR-23). No declara ni pide permiso de
 * ubicación en tiempo de ejecución: si el sistema lo deniega, {@link SecurityException} se trata
 * igual que "no hay GPS".
 *
 * <p>QA-7: la decisión de si una posición sirve (edad, precisión) vive en {@link EstadoGps}
 * (dominio, sin Android, probada en la JVM); esta clase sólo lee el {@link Location} del sistema y
 * se lo pasa.</p>
 */
final class UbicacionGps {

    private UbicacionGps() {
    }

    /** {@code latitud}/{@code longitud} sólo tienen sentido cuando {@code gpsEstado} es "con_posicion". */
    static final class Resultado {
        final double latitud;
        final double longitud;
        final String gpsEstado;

        Resultado(double latitud, double longitud, String gpsEstado) {
            this.latitud = latitud;
            this.longitud = longitud;
            this.gpsEstado = gpsEstado;
        }

        private static Resultado sinPosicion() {
            return new Resultado(0, 0, EstadoGps.texto(EstadoGps.Resultado.SIN_POSICION));
        }
    }

    static Resultado ultimaConocida(Context contexto) {
        try {
            LocationManager lm = (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return Resultado.sinPosicion();
            }
            long ahora = System.currentTimeMillis();
            for (String proveedor : lm.getProviders(true)) {
                Location loc = lm.getLastKnownLocation(proveedor);
                if (loc == null) {
                    continue;
                }
                long edadMs = ahora - loc.getTime();
                float precisionM = loc.hasAccuracy() ? loc.getAccuracy() : Float.NaN;
                EstadoGps.Resultado estado = EstadoGps.evaluar(true, edadMs, precisionM);
                String texto = EstadoGps.texto(estado);
                if (estado == EstadoGps.Resultado.CON_POSICION) {
                    return new Resultado(loc.getLatitude(), loc.getLongitude(), texto);
                }
                if (estado == EstadoGps.Resultado.POSICION_ANTIGUA) {
                    return new Resultado(0, 0, texto); // QA-7: se descarta, pero se distingue de "nunca hubo".
                }
                // SIN_POSICION (imprecisa): sigue mirando otros proveedores antes de rendirse.
            }
        } catch (SecurityException sinPermiso) {
            return Resultado.sinPosicion(); // RF-USR-15: sin permiso, se mide igual.
        }
        return Resultado.sinPosicion();
    }
}
