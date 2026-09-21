package com.dpi.retrousuario;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.dpi.retrousuario.dominio.EstadoGps;

/**
 * RF-USR-15/RF-USR-06 (M-8): última posición conocida, sin pedir un fix nuevo ni bloquear la medida.
 * Sin permiso o sin proveedor con posición: {@link Resultado#gpsEstado} queda en
 * {@code "sin_posicion"} (la fila se guarda igual, T-USR-23). <b>Corrige Javadoc de 766e6f2 (arq C2):
 * esta clase NO pide el permiso</b> — {@link com.dpi.retrousuario.MedirActivity} lo pide en tiempo de
 * ejecución al entrar a medir (decisión probada en JVM en
 * {@link com.dpi.retrousuario.dominio.PermisoUbicacion}); se conceda o no, {@link SecurityException}
 * aquí se trata igual que "no hay GPS", así que la medida nunca se bloquea por esto.
 *
 * <p>QA-7: la decisión de si una posición sirve (edad, precisión) vive en {@link EstadoGps}
 * (dominio, sin Android, probada en la JVM); esta clase sólo lee el {@link Location} del sistema y
 * se lo pasa. B-3 (condición sobre 0.3.1): con varios proveedores activos, se elige la posición MÁS
 * RECIENTE de todas antes de clasificarla (no la del primer proveedor con posición, que podía ser más
 * vieja que la de otro proveedor consultado después).</p>
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
            Location masReciente = masRecienteDeTodosLosProveedores(lm);
            if (masReciente == null) {
                return Resultado.sinPosicion();
            }
            long edadMs = System.currentTimeMillis() - masReciente.getTime();
            float precisionM = masReciente.hasAccuracy() ? masReciente.getAccuracy() : Float.NaN;
            EstadoGps.Resultado estado = EstadoGps.evaluar(true, edadMs, precisionM);
            String texto = EstadoGps.texto(estado);
            if (estado == EstadoGps.Resultado.CON_POSICION) {
                return new Resultado(masReciente.getLatitude(), masReciente.getLongitude(), texto);
            }
            return new Resultado(0, 0, texto); // QA-7: se descarta, pero se distingue de "nunca hubo".
        } catch (SecurityException sinPermiso) {
            return Resultado.sinPosicion(); // RF-USR-15: sin permiso, se mide igual.
        }
    }

    /** B-3: la MÁS RECIENTE de todas las últimas posiciones conocidas de cada proveedor activo, antes
     *  de clasificarla por edad/precisión — no la del primer proveedor con alguna posición, que podía
     *  ser más vieja que la de otro proveedor consultado después. */
    private static Location masRecienteDeTodosLosProveedores(LocationManager lm) {
        Location masReciente = null;
        for (String proveedor : lm.getProviders(true)) {
            Location loc = lm.getLastKnownLocation(proveedor);
            if (loc != null && (masReciente == null || loc.getTime() > masReciente.getTime())) {
                masReciente = loc;
            }
        }
        return masReciente;
    }
}
