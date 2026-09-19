/*
 * Prueba de ruptura del revisor P15 (05_Documentacion/REVISION-P15-QA-3.6.16.md, ef7ea10), llevada al arbol en la
 * 3.6.17. Original en el scratchpad de la revision: romper_banco/src/com/dpi/retrov36/RomperBanco.java. Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige
 * el defecto, lo comprueba con assert.
 */
package com.dpi.retrov36;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class RupturaBancoTest {
    static final String MAC = "00:21:13:05:19:3B";
    static final String ZIP = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    static List<Patron> cat;
    static Decisiones dec;
    static BancoPrevio.Grupos gr;
    static List<String> REP;

    static String asset(String n) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    static BancoCola cola(BancoCola.Tipo t) throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    static Campana telefono() throws Exception {
        String diario = ImportadorCampana.diarioDeZip(Files.readAllBytes(new File(ZIP).toPath()));
        Campana c = new Campana(cat, "SLV-002", MAC);
        int malas = c.leerDiario(new StringReader(diario));
        c.escribirEn(new StringWriter());
        if (malas > 0) {
            System.out.println("  lineas no aplicadas: " + malas);
        }
        return c;
    }

    /** = Base.aplicarBancoPrevio, llamado desde BancoActivity.cargar. */
    static String abrir(Campana c) throws Exception {
        BancoCola q = cola(BancoCola.Tipo.de(c.colaTipo()));
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, gr, dec, "SLV-002", "t");
        return c.colaTipo() + ": hechos " + r.hechos + ", repetir " + r.repetir + " " + r.texto + " | "
                + BancoPrevio.textoPendiente(q, c.pasos(), true, "PRECISO", REP);
    }

    static int n = 0;

    static Campana.Serie medir(Campana c, BancoCola.Paso p, int k, int m, double x) throws Exception {
        String nom = "OSCURO".equals(p.tipo) ? "OSCURO" : p.patron;
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:" + (n++) + "-0500", "SLV-002", MAC, "V3.6", nom, 0, 'e');
        Random rnd = new Random(n);
        for (int j = 1; j <= k; j++) {
            for (int i = 0; i < m; i++) {
                double v = Math.round(x + rnd.nextGaussian() * 3 + j);
                c.agregarDisparo(s, j, "t", "::" + (long) v, v);
            }
        }
        return s;
    }

    static Map<String, Integer> simular(Campana c, BancoCola q, boolean reabrir, Map<String, String> corte) throws Exception {
        Map<String, Integer> veces = new TreeMap<>();
        for (int guard = 0; guard < 400; guard++) {
            if (reabrir) {
                abrir(c);
            }
            Map<Integer, String> est = c.pasos();
            BancoCola.Paso p = q.siguiente(est, 0);
            if (p == null || "SALTADO".equals(est.get(p.orden))) {
                break;
            }
            if (!p.esMedida()) {
                c.anotarPaso(p.orden, "HECHO", "", "t", p.tipo);
                continue;
            }
            int[] km = ProtocoloDisparos.efectivo(p, true, "PRECISO", REP);
            if (corte != null && "PATRON".equals(p.tipo) && corte.containsKey(p.patron)) {
                String[] a = corte.remove(p.patron).split("x");
                km = new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1])};
            }
            double x = Double.isNaN(p.xEsperada) ? 800 : p.xEsperada;
            Campana.Serie s = medir(c, p, km[0], km[1], x);
            if ("A5".equals(p.tipo)) {
                c.cerrar(s, "A5", false, "banco paso " + p.orden);
            } else {
                c.cerrar(s, "OK", true, "banco paso " + p.orden);
                c.elegir(s);
            }
            c.anotarPaso(p.orden, "HECHO", s.id, "t", "OK");
            veces.merge(p.tipo + ":" + p.patron + "@" + p.orden, 1, Integer::sum);
        }
        return veces;
    }

    static void finales(Campana c, BancoCola q) {
        Map<Integer, String> est = c.pasos();
        for (char k : new char[]{'8', 'b', '5', '1'}) {
            StringBuilder sb = new StringBuilder();
            for (BancoCola.Paso p : q.pasos) {
                if ("PATRON".equals(p.tipo) && String.valueOf(k).equals(p.codigo)) {
                    Campana.Serie s = c.elegida(p.patron);
                    sb.append(p.patron).append('(').append(p.uso.charAt(0)).append(")=").append(est.get(p.orden)).append('/')
                            .append(c.seriePaso(p.orden));
                    if (s != null) {
                        int[] km = ProtocoloDisparos.deSerie(s);
                        sb.append(" el ").append(s.id).append(' ').append(km[0]).append('x').append(km[1]);
                    }
                    sb.append("; ");
                }
            }
            System.out.println("  codigo " + k + " calibrable=" + q.calibrable(String.valueOf(k), est) + " :: " + sb);
        }
        System.out.println("  " + Anclas.sRep(q, c, 1).texto);
        System.out.println("  " + Anclas.sRep(q, c, 2).texto);
        System.out.println("  " + Anclas.oscuro(q, c, '5').texto);
    }

    static BancoCola.Paso paso(BancoCola q, String pat) {
        for (BancoCola.Paso y : q.pasos) {
            if ("PATRON".equals(y.tipo) && pat.equals(y.patron)) {
                return y;
            }
        }
        return null;
    }

    static String repes(Map<String, Integer> v) {
        StringBuilder s = new StringBuilder();
        Map<String, Integer> porPatron = new TreeMap<>();
        for (Map.Entry<String, Integer> e : v.entrySet()) {
            if (e.getValue() > 1) {
                s.append(e).append(' ');
            }
        }
        return s.length() == 0 ? "ninguno" : s.toString();
    }

    /** El recorrido R1-R4 del revisor, como prueba JUnit (imprime lo observado). */
    @org.junit.Test
    public void recorrido() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] a) throws Exception {
        try (Reader r = new InputStreamReader(new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"),
                StandardCharsets.UTF_8)) {
            cat = Patron.leer(r);
        }
        dec = Decisiones.leer(asset("decisiones.csv"));
        gr = BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv"));
        REP = dec.decision("TIPO-I-REPETIR", "SLV-002").repetidos;
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);

        System.out.println("== R1 flujo real: 3.6.16 sobre el diario de las 15:10 (sin COLA)");
        Campana c = telefono();
        System.out.println("  colaElegida=" + c.colaElegida() + " colaTipo=" + c.colaTipo() + " pasos=" + c.pasos());
        System.out.println("  al abrir: " + abrir(c));
        System.out.println("  abrir otra vez: " + abrir(c));
        c.elegirCola("REPRESENTATIVO", rep.md5);
        System.out.println("  tras cambiar a representativo, pasos=" + c.pasos().size());
        System.out.println("  al abrir: " + abrir(c));
        for (String p : REP) {
            Campana.Serie s = c.elegida(p);
            System.out.println("   " + p + " elegida " + (s == null ? "-" : s.id + " " + Arrays.toString(ProtocoloDisparos.deSerie(s))
                    + " anulada=" + s.anulada) + " paso " + c.pasos().get(paso(rep, p).orden));
        }
        System.out.println("  anuladas: " + c.seriesAnuladas().size());

        System.out.println("== R2 terminar el representativo sin reabrir");
        Map<String, Integer> v = simular(c, rep, false, null);
        System.out.println("  pasos medidos: " + v.size() + "; repetidos: " + repes(v));
        System.out.println("  final: " + BancoPrevio.textoPendiente(rep, c.pasos(), true, "PRECISO", REP));
        finales(c, rep);
        System.out.println("  reabrir al final: " + abrir(c));

        System.out.println("== R3 terminar el representativo reabriendo el banco tras cada paso");
        Campana c3 = telefono();
        abrir(c3);
        c3.elegirCola("REPRESENTATIVO", rep.md5);
        abrir(c3);
        Map<String, Integer> v3 = simular(c3, rep, true, null);
        System.out.println("  pasos medidos: " + v3.size() + "; repetidos: " + repes(v3));
        for (String p : new String[]{"P127", "P19", "P81", "P18", "P28", "P4"}) {
            BancoCola.Paso x = paso(rep, p);
            String sid = c3.seriePaso(x.orden);
            System.out.println("   " + p + " paso " + x.orden + " " + c3.pasos().get(x.orden) + " serie " + sid + " de "
                    + (sid == null || sid.isEmpty() ? "-" : c3.serie(sid).patron));
        }
        finales(c3, rep);

        System.out.println("== N1 rehacer un patron que conto por equivalente (P7 por P1 S034)");
        Campana c4 = telefono();
        abrir(c4);
        c4.elegirCola("REPRESENTATIVO", rep.md5);
        abrir(c4);
        int o7 = paso(rep, "P7").orden;
        System.out.println("  P7 paso " + o7 + " " + c4.pasos().get(o7) + " serie " + c4.seriePaso(o7) + "; motivoNoRehacer="
                + RehacerBanco.motivoNoRehacer(c4, rep, o7));
        RehacerBanco.rehacer(c4, rep, o7, "P7 mal apoyado", "t");
        Campana.Serie s34 = c4.serie("S034");
        System.out.println("  tras rehacer: S034 (" + s34.patron + ") anulada=" + s34.anulada + "; elegida(P1)=" + c4.elegida("P1"));
        System.out.println("  reabrir: " + abrir(c4) + " ; P7 " + c4.pasos().get(o7));
        Campana c4b = telefono();
        abrir(c4b);
        System.out.println("  completo: P1(17) " + c4b.pasos().get(17) + " " + c4b.seriePaso(17) + ", P7(19) " + c4b.seriePaso(19)
                + ", P88(21) " + c4b.seriePaso(21));

        System.out.println("== N2 corte: P43 a 5x3 y P67 a 5x3");
        Campana c5 = telefono();
        abrir(c5);
        c5.elegirCola("REPRESENTATIVO", rep.md5);
        abrir(c5);
        Map<String, String> corte = new HashMap<>();
        corte.put("P43", "5x3");
        corte.put("P67", "5x3");
        simular(c5, rep, false, corte);
        System.out.println("  fin sin reabrir: " + BancoPrevio.textoPendiente(rep, c5.pasos(), true, "PRECISO", REP));
        System.out.println("  incumple P67: " + ProtocoloDisparos.incumple("P67", c5.elegida("P67"), ProtocoloDisparos.requerido("PRECISO"))
                + ", P43: " + ProtocoloDisparos.incumple("P43", c5.elegida("P43"), ProtocoloDisparos.requerido("PRECISO")));
        System.out.println("  calibrable 8=" + rep.calibrable("8", c5.pasos()) + " 5=" + rep.calibrable("5", c5.pasos()));
        System.out.println("  reabrir: " + abrir(c5));
        System.out.println("  reabrir otra: " + abrir(c5));
        Map<String, Integer> v5 = simular(c5, rep, false, null);
        System.out.println("  tras remedir: " + v5 + "; reabrir: " + abrir(c5));

        System.out.println("== N3 A5 de una sesion sin medir (anular A5-INICIO s2, luego s1) sobre R2");
        for (BancoCola.Paso y : rep.pasos) {
            if ("A5".equals(y.tipo) && "A5-INICIO".equals(y.bloque) && y.sesion == 2) {
                c.anular(c.serie(c.seriePaso(y.orden)), "prueba", "t");
            }
        }
        System.out.println("  s2: " + Anclas.sRep(rep, c, 2).texto);
        for (BancoCola.Paso y : rep.pasos) {
            if ("A5".equals(y.tipo) && "A5-INICIO".equals(y.bloque) && y.sesion == 1) {
                c.anular(c.serie(c.seriePaso(y.orden)), "prueba", "t");
            }
        }
        System.out.println("  s2 sin s1: " + Anclas.sRep(rep, c, 2).texto);
        Campana c6 = telefono();
        abrir(c6);
        c6.elegirCola("REPRESENTATIVO", rep.md5);
        abrir(c6);
        System.out.println("  v2 sin medir nada: " + Anclas.sRep(rep, c6, 2).texto);
        System.out.println("  completo 15:10: " + Anclas.sRep(comp, telefono(), 1).texto);
    }
}
