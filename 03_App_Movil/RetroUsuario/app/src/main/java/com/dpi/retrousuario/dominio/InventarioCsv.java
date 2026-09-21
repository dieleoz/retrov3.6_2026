package com.dpi.retrousuario.dominio;

import java.nio.charset.StandardCharsets;

/**
 * `inventario.csv` del ZIP (RF-USR-15 bis, nuevo r6, C4): en el incremento 1 sólo lleva la cabecera
 * fija (columnas de RF-USR-13, con {@code valor_instalacion_<color>} expandida por cada color
 * medible de RF-USR-03), **cero filas**; RF-USR-13 trae las filas en el incremento 2.
 */
final class InventarioCsv {

    static final String CABECERA = "identificador;codigo;latitud;longitud;lamina;valor_instalacion_blanco;"
            + "valor_instalacion_amarillo;valor_instalacion_verde;valor_instalacion_rojo;"
            + "valor_instalacion_azul;valor_instalacion_anaranjado;valor_instalacion_marron;"
            + "anio_instalacion;serie_equipo_instalacion;estado;sustituida_por";

    private InventarioCsv() {
    }

    static byte[] generar() {
        return (CABECERA + "\r\n").getBytes(StandardCharsets.UTF_8);
    }
}
