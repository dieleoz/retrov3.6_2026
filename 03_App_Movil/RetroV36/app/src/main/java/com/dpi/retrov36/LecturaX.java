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
            Cliente.Respuesta r = Cliente.instancia().pedir(tx, p.tipoX(), Cliente.TIMEOUT_ADMIN_MS + 1000);
            Double x = r.valida() ? p.valorX(r.trama, clave) : null;
            String metodo = tx + " directa";
            if (x == null) {
                return new Lectura(clave, r, Double.NaN, Double.NaN, metodo, r.valida() ? "no cuadra: " + r.trama
                        : r.describir());
            }
            return new Lectura(clave, r, x, 0, metodo, "");
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
