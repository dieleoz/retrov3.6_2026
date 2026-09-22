package com.dpi.retrov36;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Equipo simulado para las pruebas JVM: emula el firmware 3.6.2 del retrorreflectometro vertical
 * (PIC18F47K42, XC8 2.10), tal como esta en 01_Firmware/RetroVertical_V3.6.X/. Fidelidad comprobada
 * con T-S00 ({@link TS00Test}): reenvia las peticiones #...# del registro real T4 y compara byte a byte.
 *
 * Lo que emula, con su origen en el firmware:
 * <ul>
 * <li>Coeficientes en float32 (en XC8 2.10 double = float de 4 bytes: union FloatBytes,
 *     calibracion_v36.c:77-80). Fabrica: las constantes de calibracion_v36.c:34-47, redondeadas a float
 *     por el compilador. Lo escrito con #S pasa por el strtod de XC8 (calibracion_v36.c:354-364) y se
 *     imprime con el sprintf("%.8E") de XC8 (calibracion_v36.c:451-455), que se equivocan en 1-3 ulp. La
 *     emulacion de los dos es la de pruebas/T-A23_T-A30/emul.py:1-79 (validada alli 2267/2267 contra el
 *     simulador de MPLAB), portada linea a linea en {@link Xc8}.</li>
 * <li>RAM y EEPROM separadas. #S, #F, #FT y #ST escriben la RAM y luego vuelcan la RAM entera a la EEPROM
 *     (guardarEeprom, :128-154, via guardarYResponder, :595-605). Al encender, calibracionIniciar
 *     (:173-199) pone la RAM de fabrica, el PIN "2026", el modo administrador cerrado y 0 fallos de PIN, y
 *     carga de la EEPROM lo que tenga cabecera "V36" y CRC buena. Serie (#SN/#GN) y fecha (#SC/#GC) viven
 *     solo en EEPROM (:477-585): en blanco responden NONE.</li>
 * <li>Mascara de #V# por comparacion byte a byte con fabrica, no por contabilidad (mascaraAjustes,
 *     :202-223); bit 12 = temperatura.</li>
 * <li>#L: 5 fallos seguidos bloquean #L con #ERR,BLOQUEADO# hasta apagar (:644-657; PIN_MAX_FALLOS,
 *     :299). El modo administrador caduca tras 10 min sin tramas '#' (ADMIN_CADUCIDAD_MS, :298; adminTick,
 *     :337-341; tUltimaTrama se pone en cada trama '#', :620). Reloj simulado: {@link #avanzarMinutos}.</li>
 * <li>#S rechaza con #ERR,FORMATO# una curva que se sale de [0 ; 4000] en algun x de 600 a 4300
 *     (curvaValida, :404-422, llamada en :698), calculada en float como el firmware.</li>
 * <li>#E y la medida por codigo: aplicarEcuacion (:229-234) en float, izquierda a derecha, y
 *     arreglar_dato (ecuacionesCalibracion.c:49-54). Implementacion propia, no la de la app
 *     ({@code Ecuacion.respuestaFloat32}), para que el cotejo de la app contra el simulador no sea
 *     tautologico.</li>
 * <li>Trama '#' de mas de 96 bytes: se descarta sin responder (uart_module.c:61, :85-90).</li>
 * </ul>
 *
 * Fallos inyectables: corte del enlace antes o despues de aplicar una trama, #ERR en una trama,
 * equipo mudo (timeout) en una trama, una trama que responde #OK# sin hacer nada, y la bateria.
 * Toda trama recibida queda en {@link #recibidas}; una con '@' o un #P hace fallar la prueba.
 *
 * Lo que NO emula: el ruido y el tiempo de las medidas, el factor de temperatura sobre la x medida
 * (gui.c:301; x se toma tal cual), los tiempos de respuesta, y la conversion de un R negativo o de mas de
 * 65535 a unsigned int (comportamiento indefinido en C; se supone 0, como el comentario de :309-315).
 */
final class EquipoSimulado implements Canal {

    enum Falla { CORTE_ANTES, CORTE_DESPUES, ERR, ERR_FORMATO, MUDO, OK_SIN_HACER }

    private static final class Inyeccion {
        final String prefijo;
        final Falla falla;
        int veces;

        Inyeccion(String prefijo, Falla falla, int veces) {
            this.prefijo = prefijo;
            this.falla = falla;
            this.veces = veces;
        }
    }

    /** Fecha de compilacion que responde #V# (__DATE__ del .hex de la 3.6.2, calibracion_v36.c:457-475). */
    static final String FECHA_FIRMWARE = "2026-09-19";
    /** PIN que pone calibracionIniciar() si la EEPROM no trae otro (calibracion_v36.c:180). */
    static final String PIN_FABRICA = "2026";
    static final int PIN_MAX_FALLOS = 5;                   // calibracion_v36.c:299
    static final long ADMIN_CADUCIDAD_MS = 600000L;        // calibracion_v36.c:298
    static final int ADMIN_MAX = 96;                       // uart_module.c:61
    private static final int EC_NUM = 12;

    /** Factor de temperatura de fabrica, gui.c:42-44 (X_2, X_1, X_0), como float. */
    private static final float[] TEMP_FABRICA = {0.0f, Float.parseFloat("0.00043212"),
        Float.parseFloat("0.90148651")};

    /** Constantes de fabrica, calibracion_v36.c:34-47, en el orden c3, c2, c1, c0 (codigos 1-8, a-d). */
    private static final String[][] FABRICA_TXT = {
        {"0.0", "-0.000086541", "0.619668128", "-303"},          // :35 '1'
        {"-0.000000073", "0.000282508", "0.124075350", "-130"},  // :36 '2'
        {"0.000000163", "-0.000756695", "1.213142724", "-442"},  // :37 '3'
        {"0.000000147", "-0.000668624", "1.082394050", "-393"},  // :38 '4'
        {"0.000000026", "-0.000018402", "0.102152670", "-49"},   // :39 '5'
        {"0.0", "0.000090731", "0.001064329", "-21"},            // :40 '6'
        {"0.0", "0.000087404", "0.013617682", "-27"},            // :41 '7'
        {"0.000000086", "-0.000299026", "0.446572329", "-161"},  // :42 '8'
        {"0.000000163", "-0.000756695", "1.213142724", "-442"},  // :43 'a'
        {"0.0", "0.000113098", "-0.076130418", "10"},            // :44 'b'
        {"0.000000026", "-0.000018402", "0.102152670", "-49"},   // :45 'c'
        {"0.0", "0.000090731", "0.001064329", "-21"},            // :46 'd'
    };

    /**
     * Fabrica en float32. El compilador (en el PC, no la biblioteca del PIC) redondea las constantes al
     * float mas proximo: con eso la impresion de XC8 da los #G de fabrica de T4 (T-S00).
     */
    static final float[][] FABRICA = new float[EC_NUM][4];

    static {
        for (int k = 0; k < EC_NUM; k++) {
            for (int i = 0; i < 4; i++) {
                FABRICA[k][i] = Float.parseFloat(FABRICA_TXT[k][i]);
            }
        }
    }

    /** La EEPROM: cabecera "V36", 12 registros de coeficientes, el de temperatura y PIN. */
    private static final class Eeprom {
        boolean cabecera;
        final float[][] coef = new float[EC_NUM][4];
        final boolean[] crcMala = new boolean[EC_NUM + 1];
        final float[] temp = new float[3];
        String pin;
    }

    // ------------------------------------------------------------------ estado (API de las pruebas)

    /** La RAM de coeficientes (coefCal). Se usa como float32: (float) de cada coeficiente. */
    final Map<Character, Ecuacion> curvas = new HashMap<>();
    /** Ultima mascara calculada como el firmware (se recalcula en #V# y tras cada escritura). */
    int mascara;
    /** Fecha de calibracion en EEPROM (#SC/#GC); null = en blanco, #GC responde NONE. */
    String fecha;
    /** Serie en EEPROM (#SN/#GN); null = en blanco, #GN responde NONE. */
    String serie;
    /** PIN en RAM (pinAdmin). */
    String pin = PIN_FABRICA;
    boolean admin;
    boolean conectado = true;
    int bateriaN = 120;
    /** x que lee el equipo sobre la superficie colocada (oscuro si no hay nada). */
    double x = 565;
    final List<String> recibidas = new ArrayList<>();

    /** Factor de temperatura en RAM (X_2, X_1, X_0). */
    final float[] temp = TEMP_FABRICA.clone();
    /** Veces que se ha apagado y encendido. */
    int apagados;
    /** Fallos de PIN seguidos desde el ultimo #L bueno o el ultimo encendido. */
    int fallosPin;
    /** Reloj simulado (getMillis()), en ms. */
    long relojMs;

    private long tUltimaTrama;
    private final Eeprom ee = new Eeprom();
    private final List<Inyeccion> fallas = new ArrayList<>();

    /** Equipo recien grabado: EEPROM en blanco (0xFF), todo de fabrica, #GN y #GC NONE, PIN 2026. */
    EquipoSimulado() {
        calibracionIniciar();
    }

    /**
     * SLV-002 tras el acta de las 12:23 (CAL,0003): las #S del 1 y el 2 tal como las envio la app en T4
     * (campana_SLV-002_20260919_122727.zip, tramas/rtv36_20260919_114644.txt), pasadas por el strtod de
     * XC8; serie SLV-002 (#SN) y fecha 2026-09-19 (#SC). El PIN real no consta (el registro lo enmascara):
     * se supone "1234", guardado en EEPROM. Comprueba que #G da el texto del acta (TablaCalibracion).
     */
    static EquipoSimulado slv002() {
        EquipoSimulado s = new EquipoSimulado();
        s.escribirCurva('1', "0.00000000E+00", "0.00000000E+00", "2.98471545E-01", "-1.62263869E+02");
        s.escribirCurva('2', "0.00000000E+00", "0.00000000E+00", "3.65483818E-01", "-2.06628307E+02");
        s.serie = "SLV-002";
        s.fecha = "2026-09-19";
        s.pin = "1234";
        s.guardarEeprom();
        for (Map.Entry<Character, String> e : TablaCalibracion.heredados("SLV-002").entrySet()) {
            String g = s.tramaG(Fabrica.indice(e.getKey()));
            if (!g.equals(e.getValue())) {
                throw new IllegalStateException("slv002(): " + g + " no es el #G del acta " + e.getValue());
            }
        }
        return s;
    }

    private void escribirCurva(char k, String c3, String c2, String c1, String c0) {
        curvas.put(k, new Ecuacion(Xc8.strtof(c3), Xc8.strtof(c2), Xc8.strtof(c1), Xc8.strtof(c0)));
    }

    void inyectar(String prefijo, Falla f, int veces) {
        fallas.add(new Inyeccion(prefijo, f, veces));
    }

    /**
     * Apagar y encender: el enlace cae, la RAM se recarga desde la EEPROM (calibracionIniciar,
     * calibracion_v36.c:173-199), el modo administrador y los fallos de PIN vuelven a 0, y el enlace vuelve.
     */
    void apagarYEncender() {
        conectado = false;
        calibracionIniciar();
        apagados++;
        conectado = true;
    }

    /** Avanza el reloj del equipo (para la caducidad del modo administrador), sin esperar. */
    void avanzarMinutos(int min) {
        avanzarMs(min * 60000L);
    }

    void avanzarMs(long ms) {
        relojMs += ms;
        adminTick();
    }

    /** Estropea la CRC del registro del codigo k (o el de temperatura y PIN si k == 'T'). */
    void corromperEeprom(char k) {
        ee.crcMala[k == 'T' ? EC_NUM : Fabrica.indice(k)] = true;
    }

    /** Curva guardada en la EEPROM para k, o null si al encender se cargaria la de fabrica. */
    Ecuacion curvaEeprom(char k) {
        int r = Fabrica.indice(k);
        if (!ee.cabecera || ee.crcMala[r]) {
            return null;
        }
        float[] c = ee.coef[r];
        return new Ecuacion(c[0], c[1], c[2], c[3]);
    }

    int cuantas(String prefijo) {
        int n = 0;
        for (String t : recibidas) {
            if (t.startsWith(prefijo)) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ firmware: arranque y EEPROM

    private void calibracionIniciar() {
        for (int k = 0; k < EC_NUM; k++) {
            ponerFabrica(k);
        }
        System.arraycopy(TEMP_FABRICA, 0, temp, 0, 3);
        pin = PIN_FABRICA;
        admin = false;
        fallosPin = 0;
        tUltimaTrama = relojMs;
        if (ee.cabecera) {
            for (int k = 0; k < EC_NUM; k++) {
                if (!ee.crcMala[k]) {
                    float[] c = ee.coef[k];
                    curvas.put(Fabrica.CODIGOS[k], new Ecuacion(c[0], c[1], c[2], c[3]));
                }
            }
            if (!ee.crcMala[EC_NUM]) {
                System.arraycopy(ee.temp, 0, temp, 0, 3);
                pin = ee.pin;
            }
        }
        mascara = mascaraAjustes();
    }

    /** guardarEeprom (:128-154): cabecera y los 13 registros desde la RAM, con CRC buena. */
    private void guardarEeprom() {
        ee.cabecera = true;
        for (int k = 0; k < EC_NUM; k++) {
            ee.coef[k] = coefRam(k);
            ee.crcMala[k] = false;
        }
        System.arraycopy(temp, 0, ee.temp, 0, 3);
        ee.pin = pin;
        ee.crcMala[EC_NUM] = false;
        mascara = mascaraAjustes();
    }

    private float[] coefRam(int k) {
        Ecuacion e = curvas.get(Fabrica.CODIGOS[k]);
        return new float[]{(float) e.c3, (float) e.c2, (float) e.c1, (float) e.c0};
    }

    private int mascaraAjustes() {
        int m = 0;
        for (int k = 0; k < EC_NUM; k++) {
            float[] c = coefRam(k);
            for (int i = 0; i < 4; i++) {
                if (Float.floatToRawIntBits(c[i]) != Float.floatToRawIntBits(FABRICA[k][i])) {
                    m |= 1 << k;
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            if (Float.floatToRawIntBits(temp[i]) != Float.floatToRawIntBits(TEMP_FABRICA[i])) {
                m |= 1 << 12;
            }
        }
        return m;
    }

    private void adminTick() {
        if (admin && relojMs - tUltimaTrama > ADMIN_CADUCIDAD_MS) {
            admin = false;
        }
    }

    // ------------------------------------------------------------------ canal

    private static Cliente.Respuesta valida(String p, String t) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.VALIDA, t, t, 40);
    }

    private static Cliente.Respuesta timeout(String p) {
        return new Cliente.Respuesta(p, Receptor.Desenlace.TIMEOUT, null, "", -1);
    }

    @Override
    public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long timeoutMs) throws IOException {
        if (t.contains("@") || t.startsWith("#P,")) {
            throw new AssertionError("trama prohibida enviada: " + t);
        }
        if (!conectado) {
            throw new IOException("sin enlace (simulado)");
        }
        recibidas.add(t);
        adminTick();
        Inyeccion in = null;
        for (Inyeccion i : fallas) {
            if (i.veces > 0 && t.startsWith(i.prefijo)) {
                in = i;
                i.veces--;
                break;
            }
        }
        if (in != null) {
            switch (in.falla) {
                case CORTE_ANTES:
                    conectado = false;
                    throw new IOException("enlace perdido (simulado) antes de " + t);
                case CORTE_DESPUES:
                    responder(t);
                    conectado = false;
                    throw new IOException("enlace perdido (simulado) tras " + t);
                case ERR: marcarTrama(t); return valida(t, "#ERR,EEPROM#");
                case ERR_FORMATO: marcarTrama(t); return valida(t, "#ERR,FORMATO#");
                case MUDO:
                    return timeout(t);
                case OK_SIN_HACER:
                    marcarTrama(t);
                    return valida(t, "#OK#");
                default:
            }
        }
        String r = responder(t);
        return r == null ? timeout(t) : valida(t, r);
    }

    private void marcarTrama(String t) {
        if (t.startsWith("#")) {
            tUltimaTrama = relojMs;
        }
    }

    /** Respuesta del firmware a una trama, o null si no responde nada. */
    String responder(String t) {
        if (t.startsWith("#")) {
            if (t.length() > ADMIN_MAX || t.length() < 2 || !t.endsWith("#")) {
                return null;        // descartada (uart_module.c:85-90) o sin cerrar (se tira a los 2 s, :113-116)
            }
            tUltimaTrama = relojMs;
            return procesarTrama(t.substring(1, t.length() - 1));
        }
        if (t.length() == 1) {
            char c = t.charAt(0);
            if (c == 'e') {
                return "::" + Math.round(x);
            }
            if (c == '9') {
                return ":" + bateriaN + ":";
            }
            if (Fabrica.esCodigo(c)) {
                return "::" + evaluar(coefRam(Fabrica.indice(c)), (int) Math.round(x));
            }
        }
        return null;
    }

    /** adminProcesarTrama (calibracion_v36.c:611-808); 'cuerpo' es la trama sin los '#'. */
    private String procesarTrama(String cuerpo) {
        List<String> lista = new ArrayList<>();
        int ini = 0;
        for (int p = 0; p < cuerpo.length(); p++) {
            if (cuerpo.charAt(p) == ',') {
                lista.add(cuerpo.substring(ini, p));
                if (lista.size() >= 6) {
                    return "#ERR,FORMATO#";                  // MAX_CAMPOS, :629
                }
                ini = p + 1;
            }
        }
        lista.add(cuerpo.substring(ini));
        String[] c = lista.toArray(new String[0]);
        int nc = c.length;
        String o = c[0];

        if (o.equals("V") && nc == 1) {                      // :635-643
            mascara = mascaraAjustes();
            return String.format("#V,3.6,%s,%s,%04X#", FECHA_FIRMWARE, mascara != 0 ? "CAL" : "DEF", mascara);
        }
        if (o.equals("L") && nc == 2) {                      // :644-657
            if (fallosPin >= PIN_MAX_FALLOS) {
                admin = false;
                return "#ERR,BLOQUEADO#";
            }
            if (c[1].equals(pin)) {
                fallosPin = 0;
                admin = true;
                return "#OK#";
            }
            fallosPin++;
            admin = false;
            return "#ERR,PIN#";
        }
        if (o.equals("Q") && nc == 1) {                      // :658-661
            admin = false;
            return "#OK#";
        }
        if (o.equals("G") && nc == 2) {                      // :662-672
            int k = indiceCodigo(c[1]);
            return k < 0 ? "#ERR,FORMATO#" : tramaG(k);
        }
        if (o.equals("E") && nc == 3) {                      // :673-689
            int k = indiceCodigo(c[1]);
            int xv = leerEntero(c[2]);
            if (k < 0 || xv < 0) {
                return "#ERR,FORMATO#";
            }
            return "#E," + Fabrica.CODIGOS[k] + "," + evaluar(coefRam(k), xv) + "#";
        }
        if (o.equals("S") && nc == 6) {                      // :690-702
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            int k = indiceCodigo(c[1]);
            if (k < 0) {
                return "#ERR,FORMATO#";
            }
            float[] v = new float[4];
            for (int i = 0; i < 4; i++) {
                Float f = leerNumero(c[2 + i]);
                if (f == null) {
                    return "#ERR,FORMATO#";
                }
                v[i] = f;
            }
            if (!curvaValida(v)) {
                return "#ERR,FORMATO#";
            }
            curvas.put(Fabrica.CODIGOS[k], new Ecuacion(v[0], v[1], v[2], v[3]));
            guardarEeprom();
            return "#OK#";
        }
        if (o.equals("F") && nc == 2) {                      // :703-715
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            if (c[1].equals("*")) {
                for (int k = 0; k < EC_NUM; k++) {
                    ponerFabrica(k);
                }
            } else {
                int k = indiceCodigo(c[1]);
                if (k < 0) {
                    return "#ERR,FORMATO#";
                }
                ponerFabrica(k);
            }
            guardarEeprom();
            return "#OK#";
        }
        if (o.equals("FT") && nc == 1) {                     // :716-727
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            System.arraycopy(TEMP_FABRICA, 0, temp, 0, 3);
            guardarEeprom();
            return "#OK#";
        }
        if (o.equals("GC") && nc == 1) {                     // :728-730, responderFecha :554-569
            return "#GC," + (fecha != null && leerFecha(fecha) ? fecha : "NONE") + "#";
        }
        if (o.equals("GN") && nc == 1) {                     // :731-733, responderSerie :571-585
            return "#GN," + (serie != null && serieValida(serie) ? serie : "NONE") + "#";
        }
        if (o.equals("SC") && nc == 2) {                     // :734-744
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            if (c[1].equals("NONE")) {
                fecha = null;
                return "#OK#";
            }
            if (!leerFecha(c[1])) {
                return "#ERR,FORMATO#";
            }
            fecha = c[1];
            return "#OK#";
        }
        if (o.equals("SN") && nc == 2) {                     // :745-759
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            if (!serieValida(c[1]) || c[1].equals("NONE")) {
                return "#ERR,FORMATO#";
            }
            serie = c[1];
            return "#OK#";
        }
        if (o.equals("GT") && nc == 1) {                     // :760-768
            return "#GT," + Xc8.efmt(temp[0]) + "," + Xc8.efmt(temp[1]) + "," + Xc8.efmt(temp[2]) + "#";
        }
        if (o.equals("ST") && nc == 4) {                     // :769-781
            if (!admin) {
                return "#ERR,BLOQUEADO#";
            }
            float[] v = new float[3];
            for (int i = 0; i < 3; i++) {
                Float f = leerNumero(c[1 + i]);
                if (f == null) {
                    return "#ERR,FORMATO#";
                }
                v[i] = f;
            }
            if (!temperaturaValida(v)) {
                return "#ERR,FORMATO#";
            }
            System.arraycopy(v, 0, temp, 0, 3);
            guardarEeprom();
            return "#OK#";
        }
        if (o.equals("K") && nc == 1) {                      // :797-799: sin pantalla STONE, registro vacio
            return "#K,0,#";
        }
        if (o.equals("KC") && nc == 1) {                     // :800-804
            return "#OK#";
        }
        return "#ERR,FORMATO#";                              // :805-807
    }

    private void ponerFabrica(int k) {
        float[] f = FABRICA[k];
        curvas.put(Fabrica.CODIGOS[k], new Ecuacion(f[0], f[1], f[2], f[3]));
    }

    String tramaG(int k) {
        float[] c = coefRam(k);
        StringBuilder sb = new StringBuilder("#G,").append(Fabrica.CODIGOS[k]);
        for (float f : c) {
            sb.append(',').append(Xc8.efmt(f));
        }
        return sb.append('#').toString();
    }

    /** indiceCodigo (:344-351): un solo caracter de 1-8, a-d. */
    private static int indiceCodigo(String campo) {
        return campo.length() == 1 ? Fabrica.indice(campo.charAt(0)) : -1;
    }

    /** leerEntero (:425-436): 1 a 5 digitos, hasta 65535; -1 si no vale. */
    private static int leerEntero(String campo) {
        if (campo.isEmpty() || campo.length() > 5) {
            return -1;
        }
        long v = 0;
        for (int i = 0; i < campo.length(); i++) {
            char ch = campo.charAt(i);
            if (ch < '0' || ch > '9') {
                return -1;
            }
            v = v * 10 + (ch - '0');
        }
        return v > 65535 ? -1 : (int) v;
    }

    /** leerNumero (:354-364): strtod de XC8 sobre el campo entero, y finito. */
    private static Float leerNumero(String campo) {
        Float f = Xc8.strtofCampo(campo);
        return f == null || Float.isInfinite(f) || Float.isNaN(f) ? null : f;
    }

    private static boolean caracterSerieValido(char ch) {   // :522-524
        return ch >= 0x20 && ch <= 0x7E && ch != '#' && ch != ',';
    }

    private static boolean serieValida(String s) {          // :751-757 y :574-578
        if (s.length() < 1 || s.length() > 12) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!caracterSerieValido(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** leerFecha (:536-552) y fechaValida (:527-534). */
    private static boolean leerFecha(String f) {
        if (f.length() != 10 || f.charAt(4) != '-' || f.charAt(7) != '-') {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (i != 4 && i != 7 && (f.charAt(i) < '0' || f.charAt(i) > '9')) {
                return false;
            }
        }
        int a = Integer.parseInt(f.substring(0, 4));
        int m = Integer.parseInt(f.substring(5, 7));
        int d = Integer.parseInt(f.substring(8, 10));
        int[] dias = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (a < 2020 || a > 2099 || m < 1 || m > 12 || d < 1) {
            return false;
        }
        int dm = (m == 2 && a % 4 == 0) ? 29 : dias[m - 1];
        return d <= dm;
    }

    // ------------------------------------------------------------------ aritmetica del firmware (float32)

    /** aplicarEcuacion (:229-234) + arreglar_dato (ecuacionesCalibracion.c:49-54), todo en float. */
    static int evaluar(float[] c, int xEntero) {
        float xf = (float) xEntero;
        float r = c[0] * xf * xf * xf + c[1] * xf * xf + c[2] * xf + c[3];
        if (!(r >= 0)) {
            return 0;                  // negativo: indefinido en C; se supone que da la vuelta y sale 0
        }
        long v = (long) r;
        return v > Ecuacion.TECHO_FIRMWARE ? 0 : (int) v;
    }

    private static boolean finito(float v) {
        return !Float.isInfinite(v) && !Float.isNaN(v);
    }

    /** respuestaEnRango (:393-396). */
    private static boolean respuestaEnRango(float[] c, float x) {
        float r = c[0] * x * x * x + c[1] * x * x + c[2] * x + c[3];
        return finito(r) && r >= 0.0f && r <= 4000.0f;
    }

    /** criticoEnRango (:399-402). */
    private static boolean criticoEnRango(float[] c, float xc) {
        if (xc > 600.0f && xc < 4300.0f) {
            return respuestaEnRango(c, xc);
        }
        return true;
    }

    /** curvaValida (:407-422), en float como en XC8 (double de 32 bits). */
    static boolean curvaValida(float[] c) {
        if (!respuestaEnRango(c, 600.0f) || !respuestaEnRango(c, 4300.0f)) {
            return false;
        }
        if (c[0] == 0.0f) {
            return c[1] == 0.0f || criticoEnRango(c, -c[2] / (2.0f * c[1]));
        }
        float d = c[1] * c[1] - 3.0f * c[0] * c[2];
        if (!finito(d)) {
            return false;
        }
        if (d < 0.0f) {
            return true;
        }
        float q = (float) Math.sqrt(d);
        q = (c[1] < 0.0f) ? (q - c[1]) : -(q + c[1]);
        if (!criticoEnRango(c, q / (3.0f * c[0]))) {
            return false;
        }
        return q == 0.0f || criticoEnRango(c, c[2] / q);
    }

    private static boolean factorEnRango(float[] t3, float t) {   // :374-377
        float f = t3[0] * t * t + t3[1] * t + t3[2];
        return finito(f) && f >= 0.5f && f <= 1.5f;
    }

    private static boolean temperaturaValida(float[] t3) {       // :381-389
        if (!factorEnRango(t3, 0.0f) || !factorEnRango(t3, 831.0f)) {
            return false;
        }
        if (t3[0] != 0.0f) {
            float tv = -t3[1] / (2.0f * t3[0]);
            if (tv > 0.0f && tv < 831.0f && !factorEnRango(t3, tv)) {
                return false;
            }
        }
        return true;
    }

    /**
     * strtof (SMALLCODE) y efgtoa('E', precision 8) de XC8 2.10, portados linea a linea de
     * 01_Firmware/RetroVertical_V3.6.X/pruebas/T-A23_T-A30/emul.py:1-79 (que a su vez sigue
     * pic/sources/c99/common/strtof.c y doprnt.c de XC8). Operaciones en float32 al par (Java float),
     * salvo la conversion uint32 -> float, que en XC8 redondea el medio hacia arriba (emul.py:9-13).
     */
    static final class Xc8 {

        private Xc8() { }

        /** (float) de un uint32 en XC8: medio hacia arriba (emul.py:9-13). */
        static float u2f(long v) {
            int n = 64 - Long.numberOfLeadingZeros(v);
            if (n <= 24) {
                return (float) v;
            }
            int sh = n - 24;
            return (float) (((v + (1L << (sh - 1))) >> sh) << sh);
        }

        /** strtof de un texto que se sabe bien formado. */
        static float strtof(String s) {
            Float f = strtofCampo(s);
            if (f == null) {
                throw new IllegalArgumentException("no es un numero para strtod: " + s);
            }
            return f;
        }

        /**
         * strtod sobre un campo entero (emul.py:15-50). null si no consume nada o si sobra algo tras el
         * numero (leerNumero, calibracion_v36.c:358-359). Un exponente sin digitos ("1e") se rechaza: la
         * emulacion no lo cubre.
         */
        static Float strtofCampo(String s) {
            int i = 0;
            int len = s.length();
            boolean neg = false;
            if (i < len && s.charAt(i) == '-') {
                neg = true;
                i++;
            } else if (i < len && s.charAt(i) == '+') {
                i++;
            }
            long v = 0;
            int eexp = 0;
            int expon = 0;
            boolean dot = false;
            int digitos = 0;
            while (i < len) {
                char ch = s.charAt(i);
                if (!dot && ch == '.') {
                    dot = true;
                    i++;
                    continue;
                }
                if (ch < '0' || ch > '9') {
                    break;
                }
                digitos++;
                if (eexp != 9) {
                    if (dot) {
                        expon--;
                    }
                    eexp++;
                    v = (v * 10 + (ch - '0')) & 0xFFFFFFFFL;
                } else if (!dot) {
                    expon++;
                }
                i++;
            }
            if (digitos == 0) {
                return null;
            }
            int ue = 0;
            if (i < len && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                boolean en = false;
                if (i < len && s.charAt(i) == '-') {
                    en = true;
                    i++;
                } else if (i < len && s.charAt(i) == '+') {
                    i++;
                }
                int iniExp = i;
                while (i < len && s.charAt(i) == '0') {
                    i++;
                }
                StringBuilder digs = new StringBuilder();
                while (i < len && s.charAt(i) >= '0' && s.charAt(i) <= '9' && digs.length() < 3) {
                    digs.append(s.charAt(i));
                    i++;
                }
                if (i == iniExp) {
                    return null;
                }
                ue = digs.length() > 0 ? Integer.parseInt(digs.toString()) : 0;
                if (en) {
                    ue = -ue;
                }
            }
            if (i != len) {
                return null;
            }
            expon += ue;
            float l = u2f(v);
            if (l == 0) {
                return 0.0f;
            }
            if (expon < 0) {
                expon = -expon;
                while (expon >= 10) {
                    l = l / 1e10f;
                    expon -= 10;
                }
                while (expon != 0) {
                    l = l / 10.0f;
                    expon--;
                }
            } else if (expon > 0) {
                while (expon >= 10) {
                    l = l * 1e10f;
                    expon -= 10;
                }
                while (expon != 0) {
                    l = l * 10.0f;
                    expon--;
                }
            }
            return neg ? -l : l;
        }

        /** sprintf("%.8E") de XC8 (emul.py:52-79). */
        static String efmt(float f) {
            final int prec = 8;
            float g = f;
            boolean sign = g < 0;
            if (sign) {
                g = -g;
            }
            float u = 1.0f;
            int e = 0;
            if (!(g == 0)) {
                while (!(g < u * 10.0f)) {
                    u = u * 10.0f;
                    e++;
                }
                while (g < u) {
                    u = u / 10.0f;
                    e--;
                }
            }
            int m = prec + 1;
            float h = g;
            float ou = u;
            int d = 0;
            for (int i = 0; i < m; i++) {
                float l = (float) Math.floor(h / u);
                d = (int) l;
                h = h - l * u;
                u = u / 10.0f;
            }
            float l = u * 5.0f;
            if (h < l) {
                l = 0.0f;
            } else if (h == l && d % 2 == 0) {
                l = 0.0f;
            }
            h = g + l;
            u = ou;
            StringBuilder out = new StringBuilder();
            if (sign) {
                out.append('-');
            }
            for (int i = 0; i < m; i++) {
                float q = (float) Math.floor(h / u);
                d = (int) q;
                out.append((char) ('0' + d));
                if (i == 0) {
                    out.append('.');
                }
                h = h - q * u;
                u = u / 10.0f;
            }
            out.append('E').append(e < 0 ? '-' : '+');
            int ae = Math.abs(e);
            if (ae < 10) {
                out.append('0');
            }
            return out.append(ae).toString();
        }
    }
}
