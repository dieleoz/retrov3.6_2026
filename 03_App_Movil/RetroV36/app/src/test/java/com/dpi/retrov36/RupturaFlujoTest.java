/*
 * Prueba de ruptura del revisor P15 (05_Documentacion/REVISION-P15-QA-3.6.16.md, ef7ea10), llevada al arbol en la
 * 3.6.17. Original en el scratchpad de la revision: romper_flujo/src/RomperFlujoTest.java. Imprime lo observado (SALIDA) y, donde la 3.6.17 corrige
 * el defecto, lo comprueba con assert.
 */
package com.dpi.retrov36;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pruebas de ruptura de "Calibrar todo" (revisor adversario). Imprimen lo observado. */
public class RupturaFlujoTest {

    static final String MAC = "00:21:13:05:19:3B";

    /** Canal que corta el enlace en la n-esima trama que empieza por un prefijo. */
    static final class Cortador implements Canal {
        final EquipoSimulado sim;
        String prefijo;
        int n;
        int vistas;
        boolean mudo;   // true: en vez de cortar, no responde (timeout)

        Cortador(EquipoSimulado sim) {
            this.sim = sim;
        }

        void cortarEn(String p, int nesima) {
            prefijo = p;
            n = nesima;
            vistas = 0;
        }

        final List<String> todas = new ArrayList<>();

        int cuantas(String pre) {
            int c = 0;
            for (String t : todas) {
                if (t.startsWith(pre)) {
                    c++;
                }
            }
            return c;
        }

        @Override
        public Cliente.Respuesta pedir(String t, Tramas.Tipo tipo, long to) throws IOException {
            todas.add(t);
            if (prefijo != null && t.startsWith(prefijo) && ++vistas == n) {
                prefijo = null;
                if (mudo) {
                    sim.recibidas.add(t);
                    return new Cliente.Respuesta(t, Receptor.Desenlace.TIMEOUT, null, "", -1);
                }
                sim.conectado = false;
                throw new IOException("CORTE en " + t);
            }
            return sim.pedir(t, tipo, to);
        }
    }

    /** Operador con ganchos: factor por codigo en la re-medida, matar en el n-esimo apagado, bateria a 0 tras aceptar. */
    class Op implements FlujoCalibracion.Operador {
        final FlujoCalibracionTest.Operador in;
        final Map<Character, Double> factorRemedida = new HashMap<>();
        int matarEnApagado = -1;
        int apagados;
        char bateriaCeroTrasAceptar;
        String mensajeActa;
        final List<String> log = new ArrayList<>();

        Op(FlujoCalibracionTest.Operador in) {
            this.in = in;
        }

        @Override
        public int preguntar(String titulo, String mensaje, String... op) {
            if (titulo.startsWith("Re-medida del código")) {
                char k = titulo.charAt("Re-medida del código ".length());
                in.factor = factorRemedida.getOrDefault(k, 1.0);
            }
            if (titulo.startsWith("Acta del código")) {
                mensajeActa = mensaje;
            }
            log.add(titulo);
            return in.preguntar(titulo, mensaje, op);
        }

        @Override
        public void progreso(String texto) {
        }

        @Override
        public boolean apagarYEncender() {
            apagados++;
            if (apagados == matarEnApagado) {
                throw new FlujoCalibracionTest.AppMatada();
            }
            return in.apagarYEncender();
        }

        @Override
        public void actaAceptada(Acta a) {
            log.add("ACEPTADA " + a.codigos().get(0).k);
            if (bateriaCeroTrasAceptar != 0 && a.codigo(bateriaCeroTrasAceptar) != null) {
                sim.bateriaN = 0;
            }
        }
    }

    List<Patron> catalogo;
    BancoCola cola;
    EquipoSimulado sim;
    Cortador canal;
    Campana campana;
    FlujoCalibracionTest.Almacen almacen;
    FlujoCalibracionTest.Operador opIn;
    Op op;
    Decisiones decisiones;
    final Map<String, Double> factores = new HashMap<>();
    final Map<String, Double> xPatron = new HashMap<>();
    String hoy = "2026-09-20";
    final FlujoCalibracion.Reloj reloj = new FlujoCalibracion.Reloj() {
        public String ahoraIso() {
            return hoy + "T10:00:00-0500";
        }

        public String hoy() {
            return hoy;
        }
    };

    @Before
    public void preparar() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            catalogo = Patron.leer(r);
        }
        cola = BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
        sim = EquipoSimulado.slv002();
        canal = new Cortador(sim);
        almacen = new FlujoCalibracionTest.Almacen();
        opIn = new FlujoCalibracionTest.Operador(sim, xPatron);
        op = new Op(opIn);
        decisiones = Decisiones.leer(new String(Files.readAllBytes(new File("src/main/assets/decisiones.csv").toPath()),
                StandardCharsets.UTF_8));
        banco();
    }

    void banco() throws IOException {
        campana = new Campana(catalogo, "SLV-002", MAC);
        campana.escribirEn(new StringWriter());
        xPatron.clear();
        for (BancoCola.Paso p : cola.pasos) {
            if ("OSCURO".equals(p.tipo)) {
                double o = p.sesion == 2 ? 571 : 565;
                serie(p, Campana.OSCURO.nombre, new double[]{o, o + 1, o - 1, o, o});
            } else if ("A5".equals(p.tipo) && "A5-INICIO".equals(p.bloque) && p.sesion == 1) {
                double x = p.xEsperada;
                Campana.Serie s = serieSin(p, p.patron, new double[]{x * 0.98, x * 0.99, x, x * 1.01, x * 1.02});
                campana.cerrar(s, A5.VEREDICTO, false, "banco paso " + p.orden);
                campana.anotarPaso(p.orden, "HECHO", s.id, "f", "");
            } else if ("PATRON".equals(p.tipo) && ("8".equals(p.codigo) || "b".equals(p.codigo) || "5".equals(p.codigo))) {
                double x0 = "b".equals(p.codigo) ? 571 : 565;
                double a = "8".equals(p.codigo) ? 0.21 : "b".equals(p.codigo) ? 0.36 : 0.327;
                double f = factores.getOrDefault(p.patron, 1.0);
                double x = x0 + f * p.certificado / a;
                xPatron.put(p.patron, x);
                serie(p, p.patron, new double[]{x, x, x, x, x});
            }
        }
    }

    Campana.Serie serieSin(BancoCola.Paso p, String patron, double[] medias) throws IOException {
        Campana.Serie s = campana.nuevaSerie("2026-09-20T08:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        for (int k = 0; k < medias.length; k++) {
            for (int i = 0; i < 4; i++) {
                double v = medias[k] + (i % 2 == 0 ? 0.5 : -0.5);
                campana.agregarDisparo(s, k + 1, "f", "::" + Math.round(v), v);
            }
        }
        return s;
    }

    void serie(BancoCola.Paso p, String patron, double[] medias) throws IOException {
        Campana.Serie s = serieSin(p, patron, medias);
        campana.cerrar(s, "OK", true, "banco paso " + p.orden);
        campana.elegir(s);
        campana.anotarPaso(p.orden, "HECHO", s.id, "f", "");
    }

    FlujoCalibracion flujo() throws Exception {
        sim.conectado = true;
        FlujoCalibracion.Contexto ctx = FlujoCalibracion.identificar(canal, "SLV-002", MAC, Boolean.TRUE, "6/6", "RTV 3.6.16");
        sim.recibidas.clear();
        FlujoCalibracion f = new FlujoCalibracion(canal, op, almacen, cola, campana, catalogo, decisiones, ctx, reloj);
        f.pin("1234");
        return f;
    }

    static Set<Character> sel(char... ks) {
        Set<Character> s = new HashSet<>();
        for (char k : ks) {
            s.add(k);
        }
        return s;
    }

    String todo(FlujoCalibracion f, char... ks) throws Exception {
        try {
            return f.calibrarTodo(sel(ks), "Diego", "nota");
        } catch (IOException e) {
            return "IOException: " + e.getMessage();
        } catch (FlujoCalibracionTest.AppMatada e) {
            return "APP MATADA";
        }
    }

    void p(String s) {
        System.out.println(s);
    }

    String estado(FlujoCalibracion f) {
        return "cerradas=" + almacen.cerradas.size() + " #S8=" + canal.cuantas("#S,8,") + " #Sb=" + canal.cuantas("#S,b,")
                + " #S5=" + canal.cuantas("#S,5,") + " #SC=" + canal.cuantas("#SC,") + " apagados=" + sim.apagados
                + " acta=" + (f.acta() == null ? "-" : f.acta().codigos().isEmpty() ? "ABIERTA VACIA" : "abierta "
                + f.acta().codigos().get(0).k) + " fechaEquipo=" + sim.fecha;
    }

    // --------------------------------------------------------------------------------

    @Test
    public void r01Feliz_yLoQueSeEnsena() throws Exception {
        p("\n=== R01 feliz 8 -> b -> 5");
        FlujoCalibracion f = flujo();
        String r = todo(f, '8', 'b', '5');
        p("resultado: " + r);
        p(estado(f));
        p("dialogo del acta del 5 (mensaje) empieza: " + op.mensajeActa.substring(0, 40).replace('\n', '|'));
        p("dialogo contiene 'Estado: PENDIENTE': " + op.mensajeActa.contains("Estado: PENDIENTE")
                + "; contiene 'Verificación final': " + op.mensajeActa.contains("Verificación final")
                + "; contiene 'Fecha de calibración': " + op.mensajeActa.contains("Fecha de calibración"));
        for (int i = 0; i < 3; i++) {
            String a = almacen.cerradas.get(i);
            for (String l : a.split("\n")) {
                if (l.startsWith("T-C41 apagado") || l.startsWith("Conformidad:") || l.startsWith("heredados (T-C41):")) {
                    p("  acta " + i + ": " + (l.length() > 170 ? l.substring(0, 170) + "..." : l));
                }
            }
        }
        // orden de tramas: donde cae el #SC respecto al apagado usado por el T-C41 del b
        StringBuilder sb = new StringBuilder();
        boolean en = false;
        for (String t : canal.todas) {
            if (t.startsWith("#S,8,")) en = true;
            if (en) sb.append(t.length() > 14 ? t.substring(0, 14) : t).append(' ');
            if (t.startsWith("#S,b,")) break;
        }
        p("tramas desde #S,8 hasta #S,b: " + sb);
    }

    @Test
    public void r02CortesEnCadaFase() throws Exception {
        String[][] casos = {{"#L,", "1"}, {"#S,8,", "1"}, {"#SC,", "1"}, {"#GC#", "1"}, {"#GC#", "2"},
                {"#G,8", "4"}, {"#S,b,", "1"}, {"#S,5,", "1"}, {"#V#", "3"}};
        for (String[] c : casos) {
            preparar();
            FlujoCalibracion f = flujo();
            canal.cortarEn(c[0], Integer.parseInt(c[1]));
            String r = todo(f, '8', 'b', '5');
            p("\n=== R02 corte en " + c[0] + " (vez " + c[1] + ")\n 1a pulsacion: " + r + "\n  " + estado(f));
            sim.conectado = true;
            String r2 = todo(f, '8', 'b', '5');      // misma pantalla, se vuelve a pulsar
            p(" 2a pulsacion: " + cortar(r2) + "\n  " + estado(f));
            if (f.acta() != null) {
                String r3 = todo(f, '8', 'b', '5');
                p(" 3a pulsacion: " + cortar(r3) + "\n  " + estado(f));
            }
        }
    }

    @Test
    public void r02bGcMudoAntesDelSC() throws Exception {
        p("\n=== R02b #GC# sin respuesta antes del #SC; fecha del equipo NONE; fecha del equipo futura");
        FlujoCalibracion f = flujo();
        canal.mudo = true;
        canal.cortarEn("#GC#", 1);
        p(todo(f, '8') + " | " + estado(f));
        preparar();
        sim.fecha = "2027-01-01";
        FlujoCalibracion g = flujo();
        p("equipo con fecha 2027-01-01: " + todo(g, '8') + " | " + estado(g));
    }

    @Test
    public void r03AppMatada() throws Exception {
        String[] donde = {"Re-medida del código b", "Acta del código 8", "Acta del código b"};
        for (String d : donde) {
            preparar();
            FlujoCalibracion f = flujo();
            opIn.matarEn = d;
            p("\n=== R03 app matada en '" + d + "': " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
            almacen = almacen.reabrir();
            FlujoCalibracion g = flujo();
            p(" tras reabrir: " + cortar(todo(g, '8', 'b', '5')) + "\n  " + estado(g));
        }
        // en la persistencia (2o apagado = persistencia del 8; el 1o es el T-C41)
        preparar();
        op.matarEnApagado = 2;
        FlujoCalibracion f = flujo();
        p("\n=== R03 app matada en la persistencia del 8: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        almacen = almacen.reabrir();
        FlujoCalibracion g = flujo();
        p(" tras reabrir: " + cortar(todo(g, '8', 'b', '5')) + "\n  " + estado(g));
        // en el apagado del T-C41 del b? (no hay: atajo). En la persistencia del b (3er apagado)
        preparar();
        op.matarEnApagado = 3;
        FlujoCalibracion h = flujo();
        p("\n=== R03 app matada en la persistencia del b: " + todo(h, '8', 'b', '5') + "\n  " + estado(h));
        almacen = almacen.reabrir();
        FlujoCalibracion i = flujo();
        p(" tras reabrir: " + cortar(todo(i, '8', 'b', '5')) + "\n  " + estado(i));
        p(" T-C41 del 5 en el acta: " + linea(almacen.cerradas.get(almacen.cerradas.size() - 1), "T-C41 apagado"));
    }

    @Test
    public void r04RemedidaNoConformeX2EnElBYRestauracionQueFalla() throws Exception {
        p("\n=== R04 b no conforme x2; #F,b# falla 3 veces");
        op.factorRemedida.put('b', 0.85);
        sim.inyectar("#F,b#", EquipoSimulado.Falla.OK_SIN_HACER, 10);
        FlujoCalibracion f = flujo();
        p("1 Calibrar todo: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("2 Calibrar todo: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("3 Rechazar: " + f.rechazar("b no conforme"));
        p("  rechazoPendiente=" + f.rechazoPendiente() + " puedeRechazar=" + f.puedeRechazar());
        p("4 Rechazar otra vez (#F aun falla): " + f.rechazar("otra"));
        p("4b Rechazar (3a, #F aun falla): " + cortar(f.rechazar("otra")));
        // RTV 1.0.0-rc3 (FIRMA-ACTA): un texto no firma; autoriza el PIN de admin, y el nombre va aparte.
        String c5 = f.cerrarSinRestaurar("Operador", "x", "Ana Ruiz");
        String c6 = f.cerrarSinRestaurar("no soy diego", "x", "Ana Ruiz");
        p("5 cerrar 'Operador': " + c5);
        p("6 cerrar 'no soy diego': " + c6);
        org.junit.Assert.assertTrue(c6, c6.contains("PIN de administrador"));
        org.junit.Assert.assertTrue(f.rechazoPendiente());
        String c7 = f.cerrarSinRestaurar("1234", "el #F,b no entra; se revisa en taller", "Ana Ruiz");
        p("6b cerrar con el PIN: " + c7);
        org.junit.Assert.assertTrue(c7, c7.startsWith("Acta cerrada SIN RESTAURAR"));
        p("  " + estado(f) + " curva b en equipo=" + sim.curvas.get('b'));
        p("  ultima acta cerrada, lineas RECHAZ/Estado: " + linea(almacen.cerradas.get(almacen.cerradas.size() - 1), "Estado"));
        // 3.6.17 (F-03): ya no se vuelve a calibrar enseguida con la curva desconocida como 'anterior'
        op.factorRemedida.clear();
        FlujoCalibracion g = flujo();
        int sb0 = canal.cuantas("#S,b,");
        String r7 = todo(g, 'b', '5');
        p("7 Calibrar todo b,5 tras cerrar sin restaurar: " + cortar(r7) + "\n  " + estado(g));
        org.junit.Assert.assertEquals(sb0, canal.cuantas("#S,b,"));
        org.junit.Assert.assertNotNull(g.bloqueoSinRestaurar());
        p("  #F,b# enviados en total: " + canal.cuantas("#F,b#"));
    }

    @Test
    public void r05PararAquiEnElActaDel8YReanudar() throws Exception {
        p("\n=== R05 Parar aqui en el acta del 8");
        opIn.respuestas.put("Acta del código 8", 2);
        FlujoCalibracion f = flujo();
        p("1: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("  puedeAceptar=" + f.puedeAceptar() + " motivo=" + f.motivoNoAceptar());
        p("2 (reanudar): " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("  T-C41 acta b: " + linea(almacen.cerradas.get(1), "T-C41 apagado"));
        // Parar aqui y luego pulsar el boton Aceptar a mano, fuera de Calibrar todo
        preparar();
        opIn.respuestas.put("Acta del código 8", 2);
        FlujoCalibracion g = flujo();
        todo(g, '8', 'b');
        p("Parar + boton Aceptar: " + g.aceptar());
        p("  y luego Calibrar todo b: " + todo(g, 'b') + " | " + estado(g));
        p("  T-C41 acta b: " + linea(almacen.cerradas.get(1), "T-C41 apagado"));
        // dialogo del acta cerrado por la pantalla (-1)
        preparar();
        opIn.respuestas.put("Acta del código 8", -1);
        FlujoCalibracion h = flujo();
        p("dialogo destruido (-1): " + todo(h, '8', 'b') + " | " + estado(h));
    }

    @Test
    public void r06Bateria() throws Exception {
        p("\n=== R06 bateria a 0 antes");
        sim.bateriaN = 0;
        FlujoCalibracion f = flujo();
        p("1: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        sim.bateriaN = 120;
        p("  Leer bateria: " + f.leerBateria());
        p("2: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("3: " + todo(f, '8', 'b', '5') + "\n  " + estado(f));
        p("  Continuar(8): " + f.calibrar(sel('8'), "Diego", "n"));
        p("  " + estado(f));

        p("\n=== R06b bateria a 0 tras aceptar el 8 (antes del b)");
        preparar();
        op.bateriaCeroTrasAceptar = '8';
        FlujoCalibracion g = flujo();
        p("1: " + todo(g, '8', 'b', '5') + "\n  " + estado(g));
        sim.bateriaN = 120;
        op.bateriaCeroTrasAceptar = 0;
        p("  Leer bateria: " + g.leerBateria());
        p("2: " + todo(g, '8', 'b', '5') + "\n  " + estado(g));
        p("3: " + todo(g, 'b', '5') + "\n  " + estado(g));

        p("\n=== R06c bateria a 0 justo antes de aceptar el 8");
        preparar();
        FlujoCalibracion h = flujo();
        opIn.respuestas.put("Acta del código 8", 0);

        op.log.clear();
        // bateria 0 cuando se muestra el acta del 8
        FlujoCalibracionTest.Operador in = opIn;
        Op op2 = new Op(in) {
            boolean hecho;
            @Override
            public int preguntar(String t, String m, String... o) {
                if (t.startsWith("Acta del código 8") && !hecho) {
                    hecho = true;
                    sim.bateriaN = 0;
                }
                return super.preguntar(t, m, o);
            }
        };
        op = op2;
        h = flujo();
        p("1: " + todo(h, '8', 'b', '5') + "\n  " + estado(h));
        sim.bateriaN = 120;
        p("  Leer bateria: " + h.leerBateria());
        p("2: " + todo(h, '8', 'b', '5') + "\n  " + estado(h));
    }

    @Test
    public void r07UnDiaDespues() throws Exception {
        p("\n=== R07 8 aceptado el 20; b y 5 el 21");
        FlujoCalibracion f = flujo();
        p("dia 20: " + todo(f, '8') + " | " + estado(f));
        hoy = "2026-09-21";
        FlujoCalibracion g = flujo();
        p("dia 21: " + todo(g, 'b', '5') + " | " + estado(g));
        p("  T-C41 acta b: " + linea(almacen.cerradas.get(1), "T-C41 apagado"));
    }

    @Test
    public void r08P39Mas15ConDecisionesReales() throws Exception {
        for (double fac : new double[]{1.0, 1.15, 1.20, 1.23, 1.26}) {
            factores.clear();
            factores.put("P39", fac);
            preparar();
            FlujoCalibracion f = flujo();
            for (FlujoCalibracion.Tarjeta t : f.tarjetas(null)) {
                if (t.k == 'b') {
                    StringBuilder sb = new StringBuilder();
                    for (String l : t.texto.split("\n")) {
                        if (l.contains("Incumple") || l.contains("NO SE ESCRIBE") || l.contains("c1 ")) {
                            sb.append("\n    ").append(l.trim());
                        }
                    }
                    p("\n=== R08 P39 x" + fac + " casilla b=" + t.casilla + sb);
                }
            }
            if (fac >= 1.15) {
                p("  Calibrar todo 8,b: " + cortar(todo(f, '8', 'b')) + " | " + estado(f));
            }
        }
    }

    static String linea(String texto, String empieza) {
        for (String l : texto.split("\n")) {
            if (l.startsWith(empieza)) {
                return l.length() > 200 ? l.substring(0, 200) + "..." : l;
            }
        }
        return "(sin linea " + empieza + ")";
    }

    static String cortar(String s) {
        return s.length() > 400 ? s.substring(0, 400) + "..." : s;
    }
}
