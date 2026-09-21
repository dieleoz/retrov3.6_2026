package com.dpi.retrousuario;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

/**
 * RF-USR-15/RF-USR-06 (M-8): última posición conocida, sin pedir un fix nuevo ni bloquear la medida.
 * Sin permiso o sin proveedor con posición: {@code null} (la fila se guarda igual, con
 * {@code gps_estado = sin_posicion}, T-USR-23). No declara ni pide permiso de ubicación en tiempo de
 * ejecución: si el sistema lo deniega, {@link SecurityException} se trata igual que "no hay GPS".
 */
final class UbicacionGps {

    private UbicacionGps() {
    }

    /** {@code {latitud, longitud}}, o {@code null} si no hay ninguna posición conocida. */
    static double[] ultimaConocida(Context contexto) {
        try {
            LocationManager lm = (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return null;
            }
            for (String proveedor : lm.getProviders(true)) {
                Location loc = lm.getLastKnownLocation(proveedor);
                if (loc != null) {
                    return new double[] { loc.getLatitude(), loc.getLongitude() };
                }
            }
        } catch (SecurityException sinPermiso) {
            return null; // RF-USR-15: sin permiso, se mide igual.
        }
        return null;
    }
}
