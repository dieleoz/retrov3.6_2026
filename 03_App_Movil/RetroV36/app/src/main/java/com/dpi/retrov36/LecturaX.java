package com.dpi.retrov36;

import java.io.IOException;

/**
 * Obtiene la lectura interna x del equipo.
 * - Con 'e' (V3.6, o V3 2020 si respondio a 'e' en las pruebas): x directa.
 * - Sin 'e' (SLV-002): envia '6' (naranja intenso, monotono y sin techo de
 *   x = 500 a 4300) e invierte la ecuacion de forma exacta (Inversion).
 * Llamar solo desde Cliente.ejecutar().
 */
public final class LecturaX {

    private LecturaX() { }

    public static final class Lectura {
        public final char codigo;
        public final Cliente.Respuesta respuesta;
        public final double x;
        public final double u;
        public final String metodo;
        /** Vacio si valida; motivo si no. */
        public final String error;
        /**
         * RTV 1.0.0-rc3: temperatura optica que venia DENTRO de la respuesta de "#X" (RF-FW-B13,
         * "#X,k,x,TO,a#"); null con el candidato de hoy, que solo manda tres campos. Cuando viene, es la del
         * propio disparo y no hace falta pedir "#T#" detras: ese es el camino barato, y el unico exacto.
         */
        public Double temperaturaOptica;
        /** Ajuste por temperatura ya aplicado a x, en cuentas; null si el firmware no lo manda. */
        public Double ajusteTemperatura;

        Lectura(char codigo, Cliente.Respuesta respuesta, double x, double u, String metodo, String error) {
            this.codigo = codigo;
            this.respuesta = respuesta;
            this.x = x;
            this.u = u;
            this.metodo = metodo;
            this.error = error;
        }

        public boolean valida() {
            return error.isEmpty();
        }

        /** true si la propia respuesta de medida ya trajo la temperatura de este disparo. */
        public boolean traeTemperatura() {
            return temperaturaOptica != null;
        }
    }

    /**
     * Hace s.disparosAsentamiento lecturas y las descarta: van al registro de
     * tramas como "disparo de asentamiento, descartado", no a la media ni al CSV.
     */
    public static void asentar(Sesion s) throws IOException, InterruptedException {
        for (int i = 0; i < s.disparosAsentamiento; i++) {
            Lectura l = leer(s);
            Registro.nota("disparo de asentamiento, descartado: " + l.codigo + " -> "
                    + (l.respuesta.valida() ? l.respuesta.trama : l.respuesta.describir()));
        }
    }

    public static Lectura leer(Sesion s) throws IOException, InterruptedException {
        return leer(s, '1');
    }

    /**
     * RTV 1.0: la trama sale del protocolo. 'e' en la V3.6; "#X,k#" en la V4.6 (x con la luz y la rama del
     * codigo k: la cola de la V4.6 dira la clave por patron); '6' invertido en el V3 de 2020, a quien nunca se
     * envia 'e' (en SLV-002 lo dejo sin responder, 18-sep-2026, hipotesis). Sin x (V4 original), no se lee.
     */
    public static Lectura leer(Sesion s, char clave) throws IOException, InterruptedException {
        Protocolo p = s.protocolo;
        if (p == null || !p.daX()) {
            throw new IllegalStateException("el firmware " + s.versionTexto() + " no da x");
        }
        String tx = p.tramaX(clave);
        if (tx != null && tx.length() > 1) {
            // #X mide (1000 muestras con la luz del codigo): la misma espera que @LEERV.
            Cliente.Respuesta r = Cliente.instancia().pedir(tx, p.tipoX(), Tramas.TIMEOUT_LEERV_MS);
            Double x = r.valida() ? p.valorX(r.trama, clave) : null;
            String metodo = tx + " directa";
            if (x == null) {
                // RTV 1.0.0-rc3: si el equipo dijo #ERR,OCUPADO# se dice asi, no como "trama rara". La V4.6
                // lo responde a un #X que llega con una medida en curso (V4.6:Calibracion.c:980).
                String err = r.valida() ? Tramas.motivoError(r.trama) : null;
                return new Lectura(clave, r, Double.NaN, Double.NaN, metodo,
                        err != null ? "el equipo respondió #ERR," + err + "#: no midió"
                                : r.valida() ? "no cuadra: " + r.trama : r.describir());
            }
            Lectura l = new Lectura(clave, r, x, 0, metodo, "");
            // RF-FW-B13: si la respuesta trae la TO y el ajuste, son los del propio disparo.
            Tramas.XCompleta xc = Tramas.parsearXCompleta(r.trama, clave);
            if (xc != null && xc.traeTemperatura()) {
                l.temperaturaOptica = xc.temperaturaOptica;
                l.ajusteTemperatura = xc.ajuste;
            }
            return l;
        }
        char k = tx != null ? tx.charAt(0) : '6';
        Cliente.Respuesta r = Cliente.instancia().pedir(String.valueOf(k), Tramas.Tipo.MEDIDA,
                Cliente.TIMEOUT_MEDIDA_MS);
        String metodo = k == 'e' ? "e directa" : "6 invertido";
        if (!r.valida()) {
            return new Lectura(k, r, Double.NaN, Double.NaN, metodo, r.describir());
        }
        int v = Tramas.valorMedida(r.trama);
        if (v == 0) {
            return new Lectura(k, r, Double.NaN, Double.NaN, metodo,
                    "respuesta 0: saturado o negativo, no es una lectura");
        }
        if (k == 'e') {
            return new Lectura(k, r, v, 0, metodo, "");
        }
        Inversion.Resultado inv = Inversion.invertir(s.ecuacionVigente('6'), v, Double.NaN);
        if (!inv.valido) {
            return new Lectura(k, r, Double.NaN, Double.NaN, metodo, inv.motivo);
        }
        return new Lectura(k, r, inv.x, inv.u, metodo, "");
    }
}
