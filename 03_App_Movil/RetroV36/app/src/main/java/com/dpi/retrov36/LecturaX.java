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

    public static Lectura leer(Sesion s) throws IOException, InterruptedException {
        // 'e' SOLO en V3.6. En SLV-002 (V3 2020) 'e' deja el equipo sin
        // responder por Bluetooth (hipotesis del 18-sep-2026, sin confirmar).
        char k = s.version == Sesion.Version.V36 && s.eDisponible ? 'e' : '6';
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
