package com.dpi.retrov36;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Importa a la campana los CSV de medidas de la 3.6.2-3.6.4
 * (medidas_<serie>_<fecha>.csv, cabecera Medida.cabecera()). Java puro.
 *
 * Cada "Compartir" de aquellas versiones sacaba una copia creciente del mismo
 * CSV: las filas repetidas se quitan (clave fecha + patron + respuesta), y
 * tambien las que ya estan en la campana. Las filas se agrupan en series:
 * mismo patron y menos de SEPARACION_S segundos entre disparos seguidos (en
 * una serie van a ~1,6 s). Cada serie importada pasa el mismo veredicto que una
 * medida nueva; sin operador delante, solo se acepta si el veredicto es OK: una
 * serie ruidosa o dudosa queda sin aceptar (el patron pasa a "repetir") y las
 * dudas quedan en la nota. La orientacion queda desconocida.
 */
public final class Importador {

    public static final long SEPARACION_S = 5;

    private Importador() { }

    public static final class Resultado {
        public int filas;
        public int duplicadas;
        public int yaEnCampana;
        public int patronDesconocido;
        /** Filas de otro equipo (MAC distinta): no se importan. */
        public int otroEquipo;
        public int series;

        public String texto() {
            return filas + " filas leídas, " + duplicadas + " duplicadas, " + yaEnCampana + " ya en la campaña, "
                    + patronDesconocido + " de patrón desconocido, " + otroEquipo + " de otro equipo; "
                    + series + " series nuevas.";
        }
    }

    private static final class Fila implements Comparable<Fila> {
        long t;
        String fecha;
        String equipo;
        String mac;
        String firmware;
        String patron;
        char codigo;
        String bruta;
        double x;

        @Override
        public int compareTo(Fila o) {
            return Long.compare(t, o.t);
        }
    }

    static long segundos(String fecha) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(fecha).getTime() / 1000;
    }

    public static Resultado importar(Campana c, List<String> lineas, double tolOrden) throws IOException {
        Resultado r = new Resultado();
        Set<String> vistas = new HashSet<>();
        for (Campana.Serie s : c.series()) {
            for (Campana.Disparo d : s.disparos) {
                vistas.add(d.fecha + "|" + s.patronOriginal + "|" + d.bruta);
            }
        }
        Set<String> nuevas = new HashSet<>();
        List<Fila> filas = new ArrayList<>();
        for (String l : lineas) {
            if (l.trim().isEmpty() || l.startsWith("fecha_hora,") || l.startsWith("#")) {
                continue;
            }
            List<String> f = Csv.partir(l);
            if (f.size() < 11) {
                continue;
            }
            r.filas++;
            if (!c.esDeEsteEquipo(f.get(2))) {
                r.otroEquipo++;
                continue;
            }
            String clave = f.get(0) + "|" + f.get(4) + "|" + f.get(9);
            if (vistas.contains(clave)) {
                r.yaEnCampana++;
                continue;
            }
            if (!nuevas.add(clave)) {
                r.duplicadas++;
                continue;
            }
            if (c.patron(f.get(4)) == null) {
                r.patronDesconocido++;
                continue;
            }
            Fila x = new Fila();
            try {
                x.t = segundos(f.get(0));
                x.x = f.get(10).isEmpty() ? Double.NaN : Double.parseDouble(f.get(10));
            } catch (ParseException | NumberFormatException e) {
                continue;
            }
            x.fecha = f.get(0);
            x.equipo = f.get(1);
            x.mac = f.get(2);
            x.firmware = f.get(3);
            x.patron = f.get(4);
            x.codigo = f.get(8).isEmpty() ? 'e' : f.get(8).charAt(0);
            x.bruta = f.get(9);
            filas.add(x);
        }
        Collections.sort(filas);
        List<List<Fila>> grupos = new ArrayList<>();
        for (Fila f : filas) {
            List<Fila> ult = grupos.isEmpty() ? null : grupos.get(grupos.size() - 1);
            if (ult != null && ult.get(ult.size() - 1).patron.equals(f.patron)
                    && f.t - ult.get(ult.size() - 1).t <= SEPARACION_S) {
                ult.add(f);
            } else {
                List<Fila> g = new ArrayList<>();
                g.add(f);
                grupos.add(g);
            }
        }
        for (List<Fila> g : grupos) {
            Fila a = g.get(0);
            Campana.Serie s = c.nuevaSerie(a.fecha, a.equipo, a.mac, a.firmware, a.patron, -1, a.codigo);
            double[] xs = new double[g.size()];
            for (int i = 0; i < g.size(); i++) {
                c.agregarDisparo(s, g.get(i).fecha, g.get(i).bruta, g.get(i).x);
                xs[i] = g.get(i).x;
            }
            Veredicto.Resultado v = Veredicto.evaluar(xs, c.patron(a.patron), c.medias(a.patron), tolOrden);
            for (int i = 0; i < xs.length; i++) {
                if (v.descartar[i]) {
                    c.descartar(s, i + 1, v.motivos[i]);
                }
            }
            String nota = "importada de CSV" + (v.dudas.isEmpty() ? "" : "; dudas: " + String.join(" | ", v.dudas));
            c.cerrar(s, v.veredicto, "OK".equals(v.veredicto), nota);
            r.series++;
        }
        return r;
    }
}
