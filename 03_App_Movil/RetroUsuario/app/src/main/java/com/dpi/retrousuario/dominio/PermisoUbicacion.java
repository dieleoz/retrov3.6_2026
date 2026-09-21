package com.dpi.retrousuario.dominio;

/**
 * Condición C2 (arquitecto-iot sobre RetroUsuario 0.3.1): la app nunca pedía
 * {@code ACCESS_FINE_LOCATION} en tiempo de ejecución (sólo {@code WRITE_EXTERNAL_STORAGE}), pese a
 * declararlo en el manifiesto (AndroidManifest.xml) y a que la SPEC
 * (`05_Documentacion/SPEC-App-Usuario-V3.6.md:608`, §4 bis) y el README lo daban por
 * hecho. Se pide al entrar a medir ({@code MedirActivity.onCreate}, RF-USR-15: si se deniega, se mide
 * igual con {@code gps_estado = sin_posicion}, {@link com.dpi.retrousuario.UbicacionGps} ya lo trata
 * como "sin GPS" en cualquier caso).
 *
 * Esta clase sólo aísla la DECISIÓN de si toca pedirlo ahora, para probarla en la JVM sin Android: la
 * llamada real a {@code ActivityCompat.requestPermissions} vive en {@code MedirActivity} (capa
 * Android, sin arnés de pruebas en este árbol, igual que el resto de esa capa).
 */
public final class PermisoUbicacion {

    private PermisoUbicacion() {
    }

    /**
     * @param concedido true si el sistema ya tiene {@code ACCESS_FINE_LOCATION} concedido.
     * @param yaPedidoEnEsteProceso true si esta app ya mostró el diálogo del sistema en este proceso
     *                              (una vez por proceso, igual que el aviso previo de M-1: no se
     *                              repite en cada entrada a "Medir" ni en cada giro de pantalla).
     * @return true si toca pedirlo ahora.
     */
    public static boolean debePedirse(boolean concedido, boolean yaPedidoEnEsteProceso) {
        return !concedido && !yaPedidoEnEsteProceso;
    }
}
