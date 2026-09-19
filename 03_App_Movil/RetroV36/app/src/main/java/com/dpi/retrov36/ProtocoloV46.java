package com.dpi.retrov36;

/**
 * Firmware V4.6 (F-46), segun PROTOCOLO-V4.6-BORRADOR.md §4 (tabla unica, revision P2): la medida es la del
 * V4 ("@LEERV", sin cambios, 0x00 final incluido), y la administracion es el contrato "#...#" de la V3.6 mas
 * "#X,k#" (x en bruto, sustituye a 'e'), "#T#" y "#GB#" (bateria). Java puro.
 *
 * Solo en simulador hasta que exista una V4.6 grabada (revision P2, A-5): calibrar y el banco dependen de
 * que firmwares.csv traiga la cola de la V4.6, que hoy no existe.
 */
public final class ProtocoloV46 implements Protocolo {

    private final PerfilFirmware perfil;

    public ProtocoloV46() {
        this(PerfilFirmware.de(Firmware.F46));
    }

    public ProtocoloV46(PerfilFirmware perfil) {
        this.perfil = perfil;
    }

    @Override
    public Firmware firmware() {
        return Firmware.F46;
    }

    @Override
    public String nombre() {
        return "V4.6";
    }

    /** Las 12 @LEERV y "#...#" sin '@' de hasta 96 bytes. Ningun byte suelto (la V4.6 no los adopta). */
    @Override
    public boolean permitida(String t) {
        if (Tramas.esLeervValida(t)) {
            return true;
        }
        return t != null && t.indexOf('@') < 0 && t.startsWith("#") && t.endsWith("#") && t.length() >= 3
                && t.length() <= Tramas.MAX_TRAMA;
    }

    @Override
    public String tramaMedida(char k) {
        return Tramas.tramaLeerv(k);
    }

    @Override
    public Tramas.Tipo tipoMedida() {
        return Tramas.Tipo.LEERV;
    }

    @Override
    public long timeoutMedidaMs() {
        return Tramas.TIMEOUT_LEERV_MS;
    }

    @Override
    public Integer valorMedida(String trama) {
        return Tramas.valorLeerv(trama);
    }

    @Override
    public boolean daX() {
        return true;
    }

    @Override
    public String tramaX(char k) {
        return Fabrica.esCodigo(k) ? "#X," + k + "#" : null;
    }

    @Override
    public Tramas.Tipo tipoX() {
        return Tramas.Tipo.ADMIN;
    }

    @Override
    public Double valorX(String trama, char k) {
        return Tramas.parsearX(trama, k);
    }

    @Override
    public String tramaBateria() {
        return "#GB#";
    }

    @Override
    public Tramas.Tipo tipoBateria() {
        return Tramas.Tipo.ADMIN;
    }

    @Override
    public Integer valorBateria(String trama) {
        return Tramas.parsearGB(trama);
    }

    @Override
    public boolean administra() {
        return true;
    }

    @Override
    public boolean calibra() {
        return !perfil.colaBanco.isEmpty();
    }

    @Override
    public String motivoNoCalibra() {
        return calibra() ? "" : "V4.6: falta la cola del banco de la V4.6 en firmwares.csv (RF-APP-U09); "
                + "hasta entonces sólo medir.";
    }

    @Override
    public boolean mideBanco() {
        return calibra();
    }

    @Override
    public String motivoNoBanco() {
        return motivoNoCalibra();
    }

    @Override
    public String etiquetaMedida(char k) {
        return "R entera";
    }

    @Override
    public PerfilFirmware perfil() {
        return perfil;
    }
}
