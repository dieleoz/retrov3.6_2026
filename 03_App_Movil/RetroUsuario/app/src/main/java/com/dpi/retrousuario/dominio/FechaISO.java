package com.dpi.retrousuario.dominio;

/**
 * Fecha "AAAA-MM-DD" con la aritmética mínima que necesita RF-USR-02 (vencimiento = fecha + 1 año,
 * regla PA-02 del último día de febrero en año bisiesto), sin depender de {@code java.time} (API 26+; esta app tiene
 * {@code minSdkVersion 24} y no se activa desugaring de biblioteca para no depender de una
 * dependencia nueva no verificada en caché sin red, README.md "Sin INTERNET"). Aritmética de
 * calendario simple y propia: comparable por (año, mes, día), sin zona horaria ni huso.
 */
public final class FechaISO implements Comparable<FechaISO> {

    private final int anio;
    private final int mes;
    private final int dia;

    private FechaISO(int anio, int mes, int dia) {
        this.anio = anio;
        this.mes = mes;
        this.dia = dia;
    }

    /** @return la fecha, o null si "AAAA-MM-DD" no tiene ese formato exacto (10 caracteres, '-' en 4 y 7). */
    public static FechaISO deTexto(String texto) {
        if (texto == null || texto.length() != 10 || texto.charAt(4) != '-' || texto.charAt(7) != '-') {
            return null;
        }
        try {
            int a = Integer.parseInt(texto.substring(0, 4));
            int m = Integer.parseInt(texto.substring(5, 7));
            int d = Integer.parseInt(texto.substring(8, 10));
            return new FechaISO(a, m, d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static FechaISO de(int anio, int mes, int dia) {
        return new FechaISO(anio, mes, dia);
    }

    private static boolean esBisiesto(int anio) {
        return (anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0);
    }

    /**
     * Fecha + 1 año, con el último día de febrero de un año bisiesto venciendo en el último día de
     * febrero del año siguiente (PA-02, SPEC-Registro-Indicador-Interventoria.md:567,609): el resto
     * de fechas conserva mes y día.
     */
    public FechaISO masUnAnio() {
        if (mes == 2 && dia == 29) {
            return new FechaISO(anio + 1, 2, 28);
        }
        return new FechaISO(anio + 1, mes, dia);
    }

    private long clave() {
        return (long) anio * 10000 + mes * 100 + dia;
    }

    @Override
    public int compareTo(FechaISO otra) {
        return Long.compare(clave(), otra.clave());
    }

    public boolean esPosteriorA(FechaISO otra) {
        return compareTo(otra) > 0;
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d", anio, mes, dia);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FechaISO)) {
            return false;
        }
        return clave() == ((FechaISO) o).clave();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(clave());
    }

    /** Expuesto sólo para pruebas de esta clase (verificar la regla del bisiesto, PA-02). */
    static boolean esBisiestoParaPrueba(int anio) {
        return esBisiesto(anio);
    }
}
