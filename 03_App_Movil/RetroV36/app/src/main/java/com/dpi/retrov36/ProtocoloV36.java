package com.dpi.retrov36;

/**
 * Firmware V3.6 (F-36): medida por un byte de clave -> "::n", x con 'e', bateria con '9' -> ":n:", y el
 * contrato de administracion "#...#" de PROTOCOLO-V3.6.md rev. 1.1. Java puro.
 */
public class ProtocoloV36 implements Protocolo {

    private final PerfilFirmware perfil;

    public ProtocoloV36() {
        this(PerfilFirmware.de(Firmware.F36));
    }

    public ProtocoloV36(PerfilFirmware perfil) {
        this.perfil = perfil;
    }

    @Override
    public Firmware firmware() {
        return Firmware.F36;
    }

    @Override
    public String nombre() {
        return "V3.6";
    }

    /** Bytes 1-8, a-d, 9, 6 y e; y tramas "#...#" sin '@' de hasta 96 bytes. */
    @Override
    public boolean permitida(String t) {
        if (t == null || t.isEmpty() || t.indexOf('@') >= 0) {
            return false;
        }
        if (t.length() == 1) {
            char c = t.charAt(0);
            return Fabrica.esCodigo(c) || c == '9' || c == 'e';
        }
        return t.startsWith("#") && t.endsWith("#") && t.length() >= 3 && t.length() <= Tramas.MAX_TRAMA;
    }

    @Override
    public String tramaMedida(char k) {
        return Fabrica.esCodigo(k) ? String.valueOf(k) : null;
    }

    @Override
    public Tramas.Tipo tipoMedida() {
        return Tramas.Tipo.MEDIDA;
    }

    @Override
    public long timeoutMedidaMs() {
        return Ops.TIMEOUT_MEDIDA_MS;
    }

    @Override
    public Integer valorMedida(String trama) {
        return Tramas.valorMedida(trama);
    }

    @Override
    public boolean daX() {
        return true;
    }

    @Override
    public String tramaX(char k) {
        return "e";
    }

    @Override
    public Tramas.Tipo tipoX() {
        return Tramas.Tipo.MEDIDA;
    }

    @Override
    public Double valorX(String trama, char k) {
        Integer v = Tramas.valorMedida(trama);
        return v == null ? null : (double) v;
    }

    @Override
    public String tramaBateria() {
        return "9";
    }

    @Override
    public Tramas.Tipo tipoBateria() {
        return Tramas.Tipo.BATERIA;
    }

    @Override
    public Integer valorBateria(String trama) {
        return Bateria.n(trama);
    }

    @Override
    public boolean administra() {
        return true;
    }

    @Override
    public boolean calibra() {
        return true;
    }

    @Override
    public String motivoNoCalibra() {
        return "";
    }

    @Override
    public boolean mideBanco() {
        return true;
    }

    @Override
    public String motivoNoBanco() {
        return "";
    }

    @Override
    public String etiquetaMedida(char k) {
        return "R";
    }

    @Override
    public PerfilFirmware perfil() {
        return perfil;
    }
}
