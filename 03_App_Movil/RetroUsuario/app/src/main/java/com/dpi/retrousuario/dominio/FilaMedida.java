package com.dpi.retrousuario.dominio;

import java.util.List;

/**
 * Una fila de {@code medidas.csv} (RF-USR-06): una serie de disparos guardada, con todo lo que L-23
 * exige en cualquier registro exportado (serie, MAC, vencimiento) y lo que exige M-8 (GPS o su
 * ausencia declarada). Inmutable: se construye completa cuando la serie ya está decidida
 * (RF-USR-06, cada disparo se persiste antes, en {@link Diario}, pero la fila sólo existe cuando la
 * serie cierra).
 */
public final class FilaMedida {

    public final String fechaHoraIso;
    public final String latitud; // "" si sin posicion.
    public final String longitud;
    public final String gpsEstado; // "con_posicion" | "sin_posicion".
    public final String color;
    public final char codigoBt;
    public final int n;
    public final List<Integer> lecturas;
    public final int media;
    public final int minimo;
    public final boolean valido;
    public final String motivo;
    public final String serieEquipo;
    public final String serieOrigen; // "leida" | "ninguna".
    public final String mac;
    public final String firmwareV;
    public final String fechaCalibracion; // "" si DEF.
    public final String vencimiento; // "" si DEF.
    public final String estadoCalibracion; // "CAL" | "DEF" | "vencida" | "sin_fecha".

    public FilaMedida(String fechaHoraIso, String latitud, String longitud, String gpsEstado, String color,
            char codigoBt, int n, List<Integer> lecturas, int media, int minimo, boolean valido, String motivo,
            String serieEquipo, String serieOrigen, String mac, String firmwareV, String fechaCalibracion,
            String vencimiento, String estadoCalibracion) {
        this.fechaHoraIso = fechaHoraIso;
        this.latitud = latitud;
        this.longitud = longitud;
        this.gpsEstado = gpsEstado;
        this.color = color;
        this.codigoBt = codigoBt;
        this.n = n;
        this.lecturas = lecturas;
        this.media = media;
        this.minimo = minimo;
        this.valido = valido;
        this.motivo = motivo;
        this.serieEquipo = serieEquipo;
        this.serieOrigen = serieOrigen;
        this.mac = mac;
        this.firmwareV = firmwareV;
        this.fechaCalibracion = fechaCalibracion;
        this.vencimiento = vencimiento;
        this.estadoCalibracion = estadoCalibracion;
    }

    /** {@code lecturas} en el formato de `medidas.csv`: enteros separados por "|", sin el prefijo "::" (RF-USR-06). */
    public String lecturasTexto() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lecturas.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(lecturas.get(i));
        }
        return sb.toString();
    }
}
