package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** App 3.6.10, corte A (medir el banco): T-A50, T-A56, T-A57, T-A64, T-A68, P10-C1, P10-C8. */
public class Version3610Test {

    private static final String MAC = "00:21:13:05:19:3B";

    private static byte[] colaBytes() throws Exception {
        return Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath());
    }

    private static List<Patron> catalogo132() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    // ----------------------------------------------------------------- T-A50

    @Test
    public void tA50ColaDelBanco() throws Exception {
        byte[] b = colaBytes();
        assertEquals("9ddb7882fa6c32c50c90fcdd72ba8960", Resumen.hex(b, "MD5"));
        BancoCola c = BancoCola.cargar(b);
        assertEquals(180, c.pasos.size());
        Set<String> ids = new HashSet<>();
        int k5 = 0;
        int k3 = 0;
        for (BancoCola.Paso p : c.pasos) {
            if ("PATRON".equals(p.tipo)) {
                ids.add(p.patron);
                if (p.k == 5) {
                    k5++;
                } else if (p.k == 3) {
                    k3++;
                }
            }
            if (p.esMedida()) {
                assertEquals(4, p.m);
                assertEquals(1, p.asentamiento);
            }
            if ("OSCURO".equals(p.tipo) || "P81".equals(p.patron)) {
                assertEquals("P10-C3: " + p.orden, 5, p.k);
                assertEquals("la cola 9ddb7882 ya trae K = 5", "", p.ajusteApp);
            }
        }
        // 133 patrones, los 133 del catalogo.
        assertEquals(133, ids.size());
        Set<String> cat = new HashSet<>();
        for (Patron p : catalogo132()) {
            cat.add(p.nombre);
        }
        assertEquals(cat, ids);
        // Con la cola 9ddb7882: 13 patrones a K = 5 (los 9 de antes, P81 y los tres del b) y 120 a K = 3.
        assertEquals(13, k5);
        assertEquals(120, k3);
    }

    @Test
    public void tA50ColaConUnByteCambiadoNoSeAdmite() throws Exception {
        byte[] b = colaBytes().clone();
        b[200] = (byte) (b[200] == '3' ? '4' : '3');
        try {
            BancoCola.cargar(b);
            org.junit.Assert.fail("admitió una cola alterada");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Cola no admitida"));
        }
    }

    @Test
    public void tA50RecorridoSaltosYCalibrable() throws Exception {
        BancoCola c = BancoCola.cargar(colaBytes());
        Map<Integer, String> est = new LinkedHashMap<>();
        int disparos = 0;
        for (BancoCola.Paso p : c.pasos) {
            if (!p.saltable()) {
                // OSCURO, A5, BATERIA, ... no se pueden saltar.
                assertFalse(p.tipo, p.saltable());
            }
            if ("P56".equals(p.patron) || "P35".equals(p.patron)) {
                est.put(p.orden, "SALTADO");
                continue;
            }
            disparos += p.disparos();
            est.put(p.orden, "HECHO");
        }
        int esperado = 0;
        for (BancoCola.Paso p : c.pasos) {
            if (p.esMedida() && !"P56".equals(p.patron) && !"P35".equals(p.patron)) {
                esperado += p.k * (p.m + p.asentamiento);
            }
        }
        assertEquals(esperado, disparos);
        // Todo hecho salvo los saltados: siguiente() ofrece el primer saltado.
        BancoCola.Paso sig = c.siguiente(est);
        assertNotNull(sig);
        assertEquals("P56", sig.patron);
        // P56 (AJUSTE del 1) saltado: el 1 no es calibrable; el 2 si.
        assertFalse(c.calibrable("1", est));
        assertTrue(c.calibrable("2", est));
    }

    @Test
    public void reanudacionDesdeElDiario() throws Exception {
        BancoCola c = BancoCola.cargar(colaBytes());
        StringWriter w = new StringWriter();
        Campana a = new Campana(catalogo132(), "SLV-002", MAC);
        a.escribirEn(w);
        for (int i = 0; i < 39; i++) {
            a.anotarPaso(c.pasos.get(i).orden, "HECHO", "", "f", "");
        }
        a.anotarBateria("f", 40, Bateria.interpretar(40).texto);
        Campana b = new Campana(catalogo132(), "SLV-002", MAC);
        assertEquals(0, b.leerDiario(new StringReader(w.toString())));
        assertEquals(40, c.siguiente(b.pasos()).orden);           // sigue en el 40
        assertFalse(b.bateriaBloqueaEscrituras());
        b.escribirEn(new StringWriter());
        b.anotarBateria("f", null, Bateria.interpretar(null).texto);
        assertTrue(b.bateriaBloqueaEscrituras());
    }

    // ----------------------------------------------------------------- T-A56

    @Test
    public void tA56CoberturaConP1P132() throws Exception {
        List<Patron> cat = catalogo132();
        assertEquals(133, cat.size());
        StringBuilder t = new StringBuilder();
        for (char k : Fabrica.CODIGOS) {
            t.append(Asistente.cobertura(k, cat).texto()).append('\n');
        }
        for (char k : new char[]{'1', '2', '3', '4', '6', '8'}) {
            assertEquals(t.toString(), 2, Asistente.cobertura(k, cat).gradoMaximo);
        }
        assertEquals(t.toString(), 1, Asistente.cobertura('b', cat).gradoMaximo);
        for (char k : new char[]{'5', '7', 'a', 'c', 'd'}) {
            assertEquals(t.toString(), 0, Asistente.cobertura(k, cat).gradoMaximo);
        }
        // Cafe y lila no cuentan en la cobertura del 4: 16 patrones, no 26.
        assertEquals(t.toString(), 16, Asistente.cobertura('4', cat).patrones);
    }

    // ----------------------------------------------------------------- T-A57

    @Test
    public void tA57Bateria() {
        assertEquals("Batería 10,77 V (n = 40)", Bateria.interpretar(40).texto);
        assertFalse(Bateria.interpretar(40).aviso);
        assertTrue(Bateria.interpretar(19).texto.contains("10,54 V"));
        assertFalse(Bateria.interpretar(19).aviso);
        assertTrue(Bateria.interpretar(15).texto.contains("10,50 V"));
        assertTrue(Bateria.interpretar(15).aviso);
        assertFalse(Bateria.interpretar(15).bloqueaEscrituras);
        assertTrue(Bateria.interpretar(5).texto.contains("10,39-10,44 V"));
        assertTrue(Bateria.interpretar(5).aviso);
        assertTrue(Bateria.interpretar(0).texto.contains("< 10,34 V"));
        assertTrue(Bateria.interpretar(0).bloqueaEscrituras);
        assertTrue(Bateria.interpretar(177).texto.contains("> 12,28 V"));
        assertTrue(Bateria.interpretar(242).texto.contains("12,99 V"));   // sin recorte, n > 177 (13 V da 242)
        assertTrue(Bateria.interpretar(null).bloqueaEscrituras);
        assertEquals(Integer.valueOf(40), Bateria.n(":40:"));
        assertNull(Bateria.n("::40"));
    }

    // ----------------------------------------------------------------- T-A64

    @Test
    public void tA64CafeYLilaConElCodigo4() throws Exception {
        BancoCola c = BancoCola.cargar(colaBytes());
        List<String> cafeLila = new ArrayList<>();
        for (BancoCola.Paso p : c.pasos) {
            if ("cafe".equals(p.color) || "lila".equals(p.color)) {
                cafeLila.add(p.patron);
                assertEquals(p.patron, "4", p.codigo);
                assertEquals(p.patron, "VERIFICACION", p.uso);
            }
        }
        assertEquals(10, cafeLila.size());
        // En una campana que mide todo, el ajuste del 4 solo lleva rojos.
        Campana camp = new Campana(catalogo132(), "SLV-002", MAC);
        for (Patron p : catalogo132()) {
            Campana.Serie s = camp.nuevaSerie("f", "SLV-002", MAC, "V3.6", p.nombre, 0, 'e');
            camp.agregarDisparo(s, 1, "f", "::1000", 1000 + p.valor);
            camp.cerrar(s, "OK", true, "");
        }
        List<Asistente.Punto> pts = Asistente.puntos(camp.medidasElegidas(), '4');
        assertEquals(16, pts.size());
        for (Asistente.Punto q : pts) {
            assertEquals("rojo", q.patron.color);
        }
    }

    // ----------------------------------------------------------------- P10-C1

    @Test
    public void p10C1PatronPresenteYCoherenciaPorPar() {
        // 12:16:32: asentamiento 592 con P28 esperado en 2277,9: no hay patron.
        assertNotNull(Colocacion.patronAusente("P28", 592, 2277.9));
        assertNull(Colocacion.patronAusente("P28", 2213, 2277.9));
        assertNull(Colocacion.patronAusente("P28", 592, Double.NaN));      // sin x esperada no se filtra
        // Pares del intento de 12:17:02 (d de REVISION-P10 §2.1; R_#G ~ 16 sin patron): no valida.
        double[] d = {-3.1, 0.8, 0.8, 1.5, -3.5, 462.3, -1.8, -3.3, -1.3};
        double[] rg = new double[9];
        double[] r = new double[9];
        for (int i = 0; i < 9; i++) {
            rg[i] = 16.2;
            r[i] = rg[i] + d[i];
        }
        assertNotNull(Colocacion.paresIncoherentes(r, rg));
        // Segundo intento: mayor |d| 4,5 con R ~ 490 (umbral ~ 9,8): valida.
        double[] rg2 = {490, 491, 492, 493, 494, 495, 496, 497, 498};
        double[] r2 = {494.5, 489, 493, 492, 495, 493, 497, 496, 497};
        assertNull(Colocacion.paresIncoherentes(r2, rg2));
        assertEquals(9.8, Colocacion.umbralPar(490), 1e-9);
        assertEquals(3, Colocacion.umbralPar(16.2), 1e-9);
    }

    // ----------------------------------------------------------------- T-A68 y P10-C8

    private static byte[] zip1227() throws Exception {
        File z = new File("../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip");
        assertTrue("falta " + z.getAbsolutePath(), z.exists());
        return Files.readAllBytes(z.toPath());
    }

    @Test
    public void tA68ImportadorConElZipDeLas1227() throws Exception {
        byte[] z = zip1227();
        String diario = ImportadorCampana.diarioDeZip(z);
        assertNotNull(diario);
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, catalogo132(), "zip 12:27");
        assertTrue(r.texto(), r.series > 0);
        int n = r.series;
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(c, diario, catalogo132(), "otra vez");
        assertEquals(0, r2.series);
        assertEquals(n, r2.yaEstaban);
        Campana otra = new Campana(catalogo132(), "SLV-003", "00:21:13:AA:BB:CC");
        try {
            ImportadorCampana.importarDiario(otra, diario, catalogo132(), "x");
            org.junit.Assert.fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("otro equipo"));
        }
        assertTrue(otra.series().isEmpty());
    }

    @Test
    public void p10C8ImportAtomicoConPatronDesconocidoEnLaFila40() throws Exception {
        List<String> csv = ImportadorCampana.csvDeZip(zip1227());
        assertNotNull(csv);
        assertTrue(csv.size() > 41);
        List<String> mala = new ArrayList<>(csv);
        List<String> f = Csv.partir(mala.get(40));
        String linea = mala.get(40).replace("," + f.get(6) + ",", ",P999,");   // patron_original desconocido
        assertFalse(linea.equals(mala.get(40)));
        mala.set(40, linea);
        StringWriter w = new StringWriter();
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        c.escribirEn(w);
        int antes = w.toString().length();
        try {
            ImportadorCampana.importar(c, mala, "zip alterado");
            org.junit.Assert.fail("importó con un patrón desconocido");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no se importa nada"));
        }
        assertEquals("el diario queda igual", antes, w.toString().length());
        assertTrue(c.series().isEmpty());
    }

    @Test
    public void p10C8NoPisaLoElegidoPorElOperador() throws Exception {
        String diario = ImportadorCampana.diarioDeZip(zip1227());
        Campana tmp = new Campana(catalogo132(), "SLV-002", MAC);
        tmp.leerDiario(new StringReader(diario));
        // El operador ya eligio una serie propia para un patron que el ZIP tambien trae.
        String patron = null;
        for (Campana.Serie s : tmp.series()) {
            if (tmp.elegida(s.patron) == s && !Campana.OSCURO.nombre.equals(s.patron)) {
                patron = s.patron;
                break;
            }
        }
        assertNotNull(patron);
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        Campana.Serie mia = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", patron, 0, 'e');
        c.agregarDisparo(mia, 1, "2026-09-20T09:00:01-0500", "::1", 1);
        c.cerrar(mia, "OK", true, "");
        c.elegir(mia);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, diario, catalogo132(), "zip");
        assertEquals(mia.id, c.elegida(patron).id);
        assertTrue(r.texto(), r.texto().contains(patron + ": se mantiene la serie elegida por el operador"));
    }
}
