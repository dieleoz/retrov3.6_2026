package com.dpi.retrov36;

import java.io.IOException;
import java.util.Locale;

/**
 * Operaciones con el equipo que comparten "Calibrar este equipo" y Avanzado. Llamar SOLO desde
 * el hilo de trabajo (Cliente.ejecutar). Cada una deja su trama en el registro (Cliente.pedir).
 */
public final class OpsEquipo {

    private OpsEquipo() { }

    static Cliente.Respuesta pedir(String t) throws IOException, InterruptedException {
        return Cliente.instancia().pedir(t, Tramas.Tipo.ADMIN, Cliente.TIMEOUT_ADMIN_MS);
    }

    /** #L,pin#: true si #OK#. */
    public static boolean login(String pin) throws IOException, InterruptedException {
        Cliente.Respuesta r = pedir("#L," + pin + "#");
        boolean ok = Tramas.esOk(r.trama);
        Sesion.get().adminDesbloqueado = ok;
        return ok;
    }

    /**
     * RF-APP-44: #V# fresco. Actualiza marca y mascara de la sesion. Devuelve la trama (o el
     * motivo si no respondio).
     */
    public static String leerV() throws IOException, InterruptedException {
        Cliente.Respuesta r = pedir("#V#");
        Tramas.InfoVersion iv = r.valida() ? Tramas.parsearVersion(r.trama) : null;
        if (iv == null) {
            return "sin respuesta válida a #V#: " + r.describir();
        }
        Sesion s = Sesion.get();
        s.marca = iv.marca;
        s.mascara = iv.mascara;
        return r.trama;
    }

    public static Ecuacion leerG(char k) throws IOException, InterruptedException {
        Cliente.Respuesta g = pedir("#G," + k + "#");
        Ecuacion e = g.valida() ? Tramas.parsearG(g.trama, k) : null;
        if (e != null) {
            Sesion.get().leidas[Fabrica.indice(k)] = e;
        }
        return e;
    }

    public static String tramaG(char k, Ecuacion e) {
        return "#G," + k + "," + Tramas.coeficiente(e.c3) + "," + Tramas.coeficiente(e.c2) + ","
                + Tramas.coeficiente(e.c1) + "," + Tramas.coeficiente(e.c0) + "#";
    }

    /** #E en 5 puntos contra una curva (+/-1). null si cuadra; si no, el detalle. */
    public static String comprobarPorE(char k, Ecuacion curva) throws IOException, InterruptedException {
        StringBuilder mal = new StringBuilder();
        for (int x : Pruebas.X_COMPROBACION) {
            Cliente.Respuesta r = pedir("#E," + k + "," + x + "#");
            Integer v = r.valida() ? Tramas.parsearE(r.trama, k) : null;
            int esperado = curva.respuestaFloat32(x);
            if (v == null) {
                mal.append(" x=").append(x).append(": ").append(r.describir()).append(';');
            } else if (Math.abs(v - esperado) > 1) {
                mal.append(" x=").append(x).append(": #E=").append(v).append(", esperado ").append(esperado).append(';');
            }
        }
        return mal.length() == 0 ? null : "#E no reproduce la curva:" + mal;
    }

    /** RF-CAL-41: orden 9 (nunca en mitad de una serie). Anota en la campana y en el acta. */
    public static Bateria.Lectura bateria(Campana c, Acta a) throws IOException, InterruptedException {
        Cliente.Respuesta r = Cliente.instancia().pedir("9", Tramas.Tipo.BATERIA, Cliente.TIMEOUT_MEDIDA_MS);
        Bateria.Lectura l = Bateria.interpretar(r.valida() ? Bateria.n(r.trama) : null);
        if (c != null && !c.cerrada()) {
            c.anotarBateria(Sesion.ahoraIso(), l.n, l.texto);
        }
        if (a != null && !a.cerrada()) {
            a.dato("batería", l.texto);
        }
        return l;
    }

    /**
     * Restaura el codigo k a 'anterior': #F,k# si es la de fabrica (RF-APP-18), #S con la copia si no.
     * Relee y compara. Devuelve el texto del resultado.
     */
    public static String restaurar(char k, Ecuacion anterior) throws IOException, InterruptedException {
        if (anterior == null) {
            return "no se conoce la curva anterior del código " + k + ": no se restaura";
        }
        boolean fab = anterior.igualFloat32(Fabrica.ecuacion(k));
        String trama;
        if (fab) {
            trama = "#F," + k + "#";
        } else {
            Tramas.TramaS ts = Tramas.tramaS(k, anterior);
            if (ts == null) {
                return "no se pudo formar la trama de restauración";
            }
            trama = ts.texto;
        }
        Cliente.Respuesta r = Cliente.instancia().pedir(trama, Tramas.Tipo.ADMIN, 5000);
        Ecuacion e = leerG(k);
        boolean ok = e != null && e.igualFloat32(anterior, fab ? Ecuacion.ULP_G : Ecuacion.ULP_S + Ecuacion.ULP_G);
        String v = leerV();
        return String.format(Locale.US, "restauración con %s -> %s; relectura %s; #V# %s",
                fab ? "#F," + k + "#" : "#S (copia anterior)", r.describir(),
                ok ? "igual a la anterior" : "NO igual a la anterior: " + e, v);
    }
}
