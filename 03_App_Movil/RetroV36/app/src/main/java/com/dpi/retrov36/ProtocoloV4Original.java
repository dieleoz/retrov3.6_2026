package com.dpi.retrov36;

/**
 * V4 original (F-40 v4.0 o F-41 V4.1, sin grabar con la V4.6): solo "@LEERV,<COLOR>,<1|2>@" ->
 * "@LEERV,<entero>@/n/r" + 0x00 (V4.1:Serial.c:31-40, Aplicacion.c:312). Solo medir; no hay x, bateria ni
 * administracion, asi que no se calibra ni se mide el banco (RF-APP-U07). Java puro.
 *
 * Lista cerrada: solo las 12 @LEERV validas, byte a byte. En el V4.1 una '@' sin LEERV deja el equipo sin
 * Bluetooth hasta apagarlo (V4.1:Serial.c:86-104).
 */
public final class ProtocoloV4Original implements Protocolo {

    public static final String MOTIVO = "Equipo V4 sin firmware V4.6: sólo medir y verificar";
    public static final String ETIQUETA_TIPO1_V41 = "señal tipo 1, cuentas/10, no es retrorreflexión";
    public static final String ETIQUETA_TIPO1_SIN = "tipo 1 sin interpretar";

    /** Variante conocida por el perfil del equipo: "F41" (fuente V4.1), "F40" o "" (sin identificar). */
    private final String variante;

    public ProtocoloV4Original() {
        this("");
    }

    public ProtocoloV4Original(String variante) {
        this.variante = variante == null ? "" : variante;
    }

    public String variante() {
        return variante;
    }

    @Override
    public Firmware firmware() {
        return Firmware.V4_ORIGINAL;
    }

    @Override
    public String nombre() {
        return "F41".equals(variante) ? "V4 original (V4.1)" : "F40".equals(variante) ? "V4 original (v4.0)"
                : Firmware.V4_ORIGINAL.texto;
    }

    @Override
    public boolean permitida(String t) {
        return Tramas.esLeervValida(t);
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
        return false;
    }

    @Override
    public String tramaX(char k) {
        return null;
    }

    @Override
    public Tramas.Tipo tipoX() {
        return null;
    }

    @Override
    public Double valorX(String trama, char k) {
        return null;
    }

    @Override
    public String tramaBateria() {
        return null;
    }

    @Override
    public Tramas.Tipo tipoBateria() {
        return null;
    }

    @Override
    public Integer valorBateria(String trama) {
        return null;
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
        return MOTIVO;
    }

    @Override
    public boolean mideBanco() {
        return false;
    }

    @Override
    public String motivoNoBanco() {
        return MOTIVO + ". Sin x no hay banco.";
    }

    /** RF-APP-U07: el tipo 1 del V4.1 es la señal en cuentas/10; en un firmware sin identificar, sin interpretar. */
    @Override
    public String etiquetaMedida(char k) {
        if (!Tramas.esTipo1(k)) {
            return "R entera";
        }
        return "F41".equals(variante) ? ETIQUETA_TIPO1_V41 : ETIQUETA_TIPO1_SIN;
    }

    @Override
    public PerfilFirmware perfil() {
        return PerfilFirmware.de(Firmware.V4_ORIGINAL);
    }
}
