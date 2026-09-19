/*
 * Prueba de ruptura del revisor P15 (05_Documentacion/REVISION-P15-QA-3.6.16.md, ef7ea10), llevada al arbol en la
 * 3.6.17. Original en el scratchpad de la revision: mio/src/MioE2ETest.java. Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige
 * el defecto, lo comprueba con assert.
 */
package com.dpi.retrov36;
import org.junit.Test;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.Files; import java.util.*;
public class RupturaE2ETest {
    static final String MAC = "00:21:13:05:19:3B";
    static final List<String> REP = Arrays.asList("P34","P37","P43","P44","P38","P39","P49");
    static String asset(String n) throws Exception { return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8); }
    List<Patron> cat; BancoCola q; Campana c; StringWriter w; Map<String, Double> xp = new HashMap<>();
    double factorP39 = 1.0;
    final FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
        public String ahoraIso() { return "2026-09-20T10:00:00-0500"; } public String hoy() { return "2026-09-20"; } };

    double xDe(BancoCola.Paso p) {
        String k = p.codigo; double cert = p.certificado;
        if ("8".equals(k)) return 565 + cert / 0.21;
        if ("b".equals(k)) return 565 + ("P39".equals(p.patron) ? factorP39 : 1.0) * cert / 0.36;
        if ("5".equals(k)) return 565 + cert / 0.327;
        return p.xEsperada > 0 ? p.xEsperada : 1500;
    }
    static String una(String s) { return s.replace("\n", " | "); }

    void preparar(boolean abrirDosVeces) throws Exception {
        try (InputStreamReader r = new InputStreamReader(new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) { cat = Patron.leer(r); }
        byte[] zip = Files.readAllBytes(new File("../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip").toPath());
        String diario = ImportadorCampana.diarioDeZip(zip);
        c = new Campana(cat, "SLV-002", MAC);
        int malas = c.leerDiario(new StringReader(diario));
        w = new StringWriter(); w.write(diario); c.escribirEn(w);
        System.out.println("1. lineasMalas=" + malas + " series=" + c.series().size() + " pasos=" + c.pasos().size()
            + " colaElegida=" + c.colaElegida() + " colaTipo=" + c.colaTipo());
        Decisiones d = Decisiones.leer(asset("decisiones.csv"));
        BancoPrevio.Grupos g = BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv"));
        BancoCola comp = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
        q = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_representativo_v2.csv").toPath()));
        Campana c0 = new Campana(cat, "SLV-002", MAC); c0.leerDiario(new StringReader(diario)); c0.escribirEn(new StringWriter());
        BancoPrevio.Resultado r0 = BancoPrevio.aplicar(c0, comp, g, d, "SLV-002", "t");
        System.out.println("3a. al abrir (COMPLETO): +" + r0.hechos + " rep=" + r0.repetir + " -> " + BancoPrevio.textoPendiente(comp, c0.pasos(), true, "PRECISO", REP));
        c.elegirCola("REPRESENTATIVO", q.md5);
        BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, g, d, "SLV-002", "t");
        System.out.println("3b. tras elegir REPRESENTATIVO: +" + r.hechos + " rep=" + r.repetir + " -> " + BancoPrevio.textoPendiente(q, c.pasos(), true, "PRECISO", REP));
        System.out.println("    " + r.texto);
        int n = 0, medidos = 0; Map<String,Integer> km = new TreeMap<>();
        BancoCola.Paso p;
        while ((p = q.siguiente(c.pasos())) != null && n++ < 500) {
            if (!"PATRON".equals(p.tipo) && !"OSCURO".equals(p.tipo) && !"A5".equals(p.tipo)) { c.anotarPaso(p.orden, "HECHO", "", "t", ""); continue; }
            int[] e = Protocolo.efectivo(p, true, "PRECISO", REP);
            String pat = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
            double x = "OSCURO".equals(p.tipo) ? 565 : xDe(p);
            if ("PATRON".equals(p.tipo)) xp.put(p.patron, x);
            Campana.Serie s = c.nuevaSerie("2026-09-20T08:00:00-0500", "SLV-002", MAC, "V3.6", pat, 0, 'e');
            for (int k = 1; k <= e[0]; k++) for (int i = 0; i < e[1]; i++) { double v = x * (1 + 0.004 * (k - 3)) + (i % 2 == 0 ? .5 : -.5); c.agregarDisparo(s, k, "f", "::" + Math.round(v), v); }
            boolean a5 = "A5".equals(p.tipo);
            c.cerrar(s, a5 ? A5.VEREDICTO : "OK", !a5, "banco paso " + p.orden);
            if (!a5) c.elegir(s);
            c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
            medidos++; km.merge(p.tipo + " " + e[0] + "x" + e[1], 1, Integer::sum);
            if (abrirDosVeces) BancoPrevio.aplicar(c, q, g, d, "SLV-002", "t");
        }
        System.out.println("4. banco: series nuevas=" + medidos + " " + km + " -> " + BancoPrevio.textoPendiente(q, c.pasos(), true, "PRECISO", REP)
            + " calibrable 8/b/5=" + q.calibrable("8", c.pasos()) + "/" + q.calibrable("b", c.pasos()) + "/" + q.calibrable("5", c.pasos()));
        for (String t : REP) {
            Campana.Serie s = c.elegida(t); System.out.print(t + "=" + s.id + " " + Protocolo.deSerie(s)[0] + "x" + Protocolo.deSerie(s)[1] + "; ");
        }
        System.out.println();
    }

    String calibrar(EquipoSimulado sim, FlujoCalibracionTest.Almacen alm, FlujoCalibracionTest.Operador op, String bt, Set<Character> sel) throws Exception {
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(sim, bt, MAC, Boolean.TRUE, "6/6", "RTV 3.6.16 (3616)");
        FlujoCalibracion f = new FlujoCalibracion(sim, op, alm, q, c, cat, Decisiones.leer(asset("decisiones.csv")), ctx, reloj);
        f.pin("1234");
        return f.calibrarTodo(sel, "Diego", "E2E P15");
    }
    static Set<Character> sel(char... ks) { Set<Character> s = new HashSet<>(); for (char k : ks) s.add(k); return s; }

    void volcar(FlujoCalibracionTest.Almacen alm) {
        for (String a : alm.cerradas) {
            for (String l : a.split("\n")) {
                if (l.contains("Equipo:") || l.contains("protocolo banco") || l.contains("patrones ") || l.contains("heredados")
                        || l.contains("pagado") || l.contains("RF-CAL-14") || l.contains("RF-CAL-15") || l.contains("s_rep")
                        || l.contains("Estado") || l.contains("SHA") || l.contains("sha256")) {
                    System.out.println("   | " + l.trim());
                }
            }
            System.out.println("   ----");
        }
    }

    @Test public void deIzquierdaADerecha() throws Exception {
        preparar(false);
        EquipoSimulado sim = EquipoSimulado.slv002();
        String t = FlujoCalibracion.renombrarSerie(sim, c, "1234", "SLV-002-2026", "SLV-002-2026", "Diego", "2026-09-20T09:00:00-0500");
        System.out.println("5. " + una(t) + " || sim.serie=" + sim.serie + " historial=" + c.historialSeries());
        FlujoCalibracionTest.Almacen alm = new FlujoCalibracionTest.Almacen();
        FlujoCalibracionTest.Operador op = new FlujoCalibracionTest.Operador(sim, xp);
        int ap = sim.apagados;
        String r = calibrar(sim, alm, op, "SLV-002", sel('8','b','5'));
        System.out.println("6. " + una(r));
        long nActa = 0; for (String x : op.titulos) if (x.startsWith("Acta")) nActa++;
        System.out.println("   actas=" + alm.cerradas.size() + " apagados=" + (sim.apagados - ap) + " #SC=" + sim.cuantas("#SC,") + " mascara=" + Integer.toHexString(sim.mascara) + " dialogos acta=" + nActa + " titulos=" + op.titulos);
        volcar(alm);
    }

    @Test public void conP39Mas14() throws Exception {
        factorP39 = 1.14;
        preparar(false);
        EquipoSimulado sim = EquipoSimulado.slv002();
        FlujoCalibracionTest.Almacen alm = new FlujoCalibracionTest.Almacen();
        FlujoCalibracionTest.Operador op = new FlujoCalibracionTest.Operador(sim, xp);
        String r = calibrar(sim, alm, op, "SLV-002", sel('8','b','5'));
        System.out.println("P39x1.14: " + una(r));
        volcar(alm);
    }

    @Test public void saliendoYEntrandoDelBanco() throws Exception {
        preparar(true);
        System.out.println("seriePaso 83 (P127)=" + c.seriePaso(83) + " -> " + c.serie(c.seriePaso(83)).patron + "; 85 (P19)=" + c.seriePaso(85) + " -> " + c.serie(c.seriePaso(85)).patron);
        EquipoSimulado sim = EquipoSimulado.slv002();
        FlujoCalibracionTest.Almacen alm = new FlujoCalibracionTest.Almacen();
        FlujoCalibracionTest.Operador op = new FlujoCalibracionTest.Operador(sim, xp);
        String r = calibrar(sim, alm, op, "SLV-002", sel('8','b','5'));
        System.out.println("salir/entrar: " + una(r));
        for (String a : alm.cerradas) for (String l : a.split("\n")) if (l.contains("P127") || l.contains("patrones 5") || l.contains("protocolo banco 5") || l.contains("series")) System.out.println("   | " + l.trim());
    }
}
