package com.dpi.retrov36;

/**
 * V3 de 2020 (F-2020), y la variante sin 'e' de SLV-002: medida por un byte -> "::n", bateria con '9'. Sin 'e'
 * (tras 'e' SLV-002 dejo de responder, Tramas) y sin "#...#": x se obtiene invirtiendo el codigo 6
 * (LecturaX, Inversion). No se calibra. Java puro.
 */
public final class ProtocoloV2020 extends ProtocoloV36 {

    public ProtocoloV2020() {
        super(PerfilFirmware.de(Firmware.F2020));
    }

    @Override
    public Firmware firmware() {
        return Firmware.F2020;
    }

    @Override
    public String nombre() {
        return "V3 2020 (sin e)";
    }

    /** Bytes 1-8, a-d y 9. Nunca 'e' ni "#...#". */
    @Override
    public boolean permitida(String t) {
        if (t == null || t.length() != 1) {
            return false;
        }
        char c = t.charAt(0);
        return Fabrica.esCodigo(c) || c == '9';
    }

    /** Sin x directa: LecturaX la saca invirtiendo el 6. */
    @Override
    public String tramaX(char k) {
        return null;
    }

    /** RTV 1.0.0-rc3: el V3 de 2020 no habla "#...#", asi que ni siquiera tiene donde pedir la temperatura. */
    @Override
    public String motivoSinTemperatura() {
        return "El firmware V3 de 2020 no tiene órdenes \"#...#\": no hay ninguna trama con la que pedirle la "
                + "temperatura.";
    }

    @Override
    public boolean administra() {
        return false;
    }

    @Override
    public boolean calibra() {
        return false;
    }

    @Override
    public String motivoNoCalibra() {
        return "Firmware V3 de 2020: no tiene órdenes de calibración.";
    }
}
