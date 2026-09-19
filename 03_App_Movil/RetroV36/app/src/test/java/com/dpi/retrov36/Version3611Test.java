package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * App 3.6.11, arreglo de la QA de la 3.6.10 (QA-App-3.6.10.md, 0dae2ec): QA-3610-01 (filtro por
 * origen_x), -02 (salida ante el rechazo: la parte JVM), -03 (todos los saltados), -08/-09/-10
 * (importacion) y la parte JVM de T-S01. El ciclo de vida de BancoActivity (QA-3610-04) no se
 * puede probar en la JVM.
 */
public class Version3611Test {

    private static final String MAC = "00:21:13:05:19:3B";

    private static BancoCola cola() throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/cola_banco_P1-P132.csv").toPath()));
    }

    private static List<Patron> catalogo132() throws Exception {
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream("src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    private static BancoCola.Paso patron(BancoCola c, String nombre) {
        for (BancoCola.Paso p : c.pasos) {
            if ("PATRON".equals(p.tipo) && nombre.equals(p.patron)) {
                return p;
            }
        }
        throw new AssertionError(nombre + " no está en la cola");
    }

    // ------------------------------------------------------------ QA-3610-01

    @Test
    public void toleranciaSegunOrigenX() throws Exception {
        BancoCola c = cola();
        int medida = 0;
        int recta = 0;
        int sinRango = 0;
        for (BancoCola.Paso p : c.pasos) {
            if (!"PATRON".equals(p.tipo)) {
                continue;
            }
            double t = p.toleranciaX();
            String col = p.color;
            if (col.startsWith("caf") || col.startsWith("lila") || p.origenX.contains("fabrica")) {
                assertTrue(p.patron, Double.isNaN(t));
                sinRango++;
            } else if ("medida".equals(p.origenX)) {
                assertEquals(p.patron, 0.20, t, 0);
                medida++;
            } else {
                assertEquals(p.patron + " " + p.origenX, "estimada: recta por el oscuro", p.origenX);
                assertEquals(p.patron, 0.30, t, 0);
                recta++;
            }
        }
        // 133 pasos de patron: 51 con x medida, 57 con la recta, 10 cafe y lila y 15 naranja intenso.
        assertEquals(51, medida);
        assertEquals(57, recta);
        assertEquals(25, sinRango);
        assertTrue(Double.isNaN(patron(c, "P68").toleranciaX()));    // cafe
        assertTrue(Double.isNaN(patron(c, "P69").toleranciaX()));    // lila
        assertTrue(Double.isNaN(patron(c, "P70").toleranciaX()));    // naranja, fabrica invertida
    }

    @Test
    public void filtroPorOrigen() {
        // +/-30 % en las estimadas con la recta; +/-20 % en las medidas.
        assertNull(Colocacion.patronAusente("Px", 750, 1000, 0.30, 565));
        assertNotNull(Colocacion.patronAusente("Px", 690, 1000, 0.30, 565));
        assertNotNull(Colocacion.patronAusente("Px", 750, 1000, 0.20, 565));
        // Sin rango (cafe, lila, fabrica invertida): solo "no esta en oscuro". Lila a 0,53 de lo esperado pasa.
        double lila = 565 + 0.53 * (1342 - 565);
        assertNull(Colocacion.patronAusente("P69", lila, 1342, Double.NaN, 565));
        // El caso del 19-sep: 592, a 27 cuentas del oscuro.
        assertNotNull(Colocacion.patronAusente("P69", 592, 1342, Double.NaN, 565));
        // QA-3610-13: sin lectura no es "patron ausente".
        String mudo = Colocacion.patronAusente("P69", Double.NaN, 1342, Double.NaN, 565);
        assertTrue(mudo, mudo.contains("no dio lectura"));
        assertFalse(mudo.contains("NaN"));
    }

    /** Con la x esperada como valor real, ningun paso de la cola se rechaza (sin falsos rechazos por construccion). */
    @Test
    public void ningunPasoSeRechazaConSuPropiaX() throws Exception {
        for (BancoCola.Paso p : cola().pasos) {
            if ("PATRON".equals(p.tipo) && !Double.isNaN(p.xEsperada)) {
                assertNull(p.patron, Colocacion.patronAusente(p.patron, p.xEsperada, p.xEsperada, p.toleranciaX(), 565));
            }
        }
    }

    // ------------------------------------------------------------ QA-3610-03

    @Test
    public void alFinalSeOfrecenTodosLosSaltados() throws Exception {
        BancoCola c = cola();
        Map<Integer, String> est = new LinkedHashMap<>();
        for (BancoCola.Paso p : c.pasos) {
            est.put(p.orden, "HECHO");
        }
        int a = patron(c, "P56").orden;
        int b = patron(c, "P69").orden;
        int d = patron(c, "P70").orden;
        for (int o : new int[]{a, b, d}) {
            est.put(o, "SALTADO");
        }
        assertEquals(3, c.saltados(est).size());
        assertEquals(a, c.siguiente(est, 0).orden);
        // Se vuelve a dejar el primero: sale el segundo, luego el tercero, y la vuelta empieza otra vez.
        assertEquals(b, c.siguiente(est, a).orden);
        assertEquals(d, c.siguiente(est, b).orden);
        assertEquals(a, c.siguiente(est, d).orden);
        est.put(b, "HECHO");
        assertEquals(d, c.siguiente(est, a).orden);
        est.put(a, "HECHO");
        est.put(d, "HECHO");
        assertNull(c.siguiente(est, 0));
    }

    // ------------------------------------------------------ T-S01 (parte JVM)

    @Test
    public void tS01RecorridoDeLaCola() throws Exception {
        BancoCola c = cola();
        Map<Integer, String> est = new LinkedHashMap<>();
        int e = 0;
        int bateria = 0;
        int patrones = 0;
        int pasos = 0;
        BancoCola.Paso p;
        int anterior = 0;
        while ((p = c.siguiente(est, 0)) != null) {
            assertTrue("en el orden del CSV", p.orden > anterior);
            anterior = p.orden;
            e += p.esMedida() ? p.k * (p.m + Math.max(1, p.asentamiento)) : 0;
            bateria += "BATERIA".equals(p.tipo) ? 1 : 0;
            patrones += "PATRON".equals(p.tipo) ? 1 : 0;
            est.put(p.orden, "HECHO");
            pasos++;
        }
        assertEquals(180, pasos);
        assertEquals(133, patrones);
        assertEquals("Σ K·(M + 1), QA-App-3.6.10 §3", 2575, e);
        assertTrue(bateria >= 3);
    }

    // ------------------------------------------------------- QA-3610-08/09/10

    private static Campana origen(StringWriter w) throws Exception {
        Campana a = new Campana(catalogo132(), "SLV-002", MAC);
        a.escribirEn(w);
        Campana.Serie s = a.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", "P28", 0, 'e');
        a.agregarDisparo(s, 1, "2026-09-20T09:00:01-0500", "::2277", 2277);
        a.agregarDisparo(s, 1, "2026-09-20T09:00:03-0500", "::2279", 2279);
        a.cerrar(s, "OK", true, "banco");
        a.anotarPaso(20, "HECHO", s.id, "f", "");
        a.anotarPaso(21, "SALTADO", "", "f", "");
        return a;
    }

    @Test
    public void importarPorDiarioEsAtomicoConUnReasignaDesconocido() throws Exception {
        StringWriter w = new StringWriter();
        Campana a = origen(w);
        a.reasignar(a.series().get(0), "P27", "prueba");
        String malo = w.toString().replace("REASIGNA,S001,P27", "REASIGNA,S001,P999");
        assertTrue(malo.contains("P999"));
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        try {
            ImportadorCampana.importarDiario(c, malo, catalogo132(), "zip alterado");
            fail("un patrón desconocido no se importa");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no se importa nada"));
        }
        assertTrue(c.series().isEmpty());
        assertTrue(c.pasos().isEmpty());
    }

    @Test
    public void importarTraeLosPasoSinPisar() throws Exception {
        StringWriter w = new StringWriter();
        origen(w);
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        Campana.Serie mia = c.nuevaSerie("2026-09-19T09:00:00-0500", "SLV-002", MAC, "V3.6", "P11", 0, 'e');
        c.agregarDisparo(mia, 1, "2026-09-19T09:00:01-0500", "::1500", 1500);
        c.cerrar(mia, "OK", true, "");
        c.anotarPaso(20, "SALTADO", "", "f", "");       // aqui saltado, alli hecho: se trae
        c.anotarPaso(21, "HECHO", mia.id, "f", "");     // aqui hecho: no se pisa con el SALTADO de alli
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(c, w.toString(), catalogo132(), "zip");
        assertEquals(1, r.series);
        assertEquals(1, r.pasos);
        assertEquals("HECHO", c.pasos().get(20));
        String sid = c.seriePaso(20);
        assertEquals("P28", c.serie(sid).patron);         // renumerada a la serie importada
        assertEquals("HECHO", c.pasos().get(21));
        assertEquals(mia.id, c.seriePaso(21));
        // Reimportar no duplica series ni pasos.
        ImportadorCampana.Resultado r2 = ImportadorCampana.importarDiario(c, w.toString(), catalogo132(), "otra vez");
        assertEquals(0, r2.series);
        assertEquals(0, r2.pasos);
    }

    @Test
    public void laSerieDelBancoPasaAElegidaAunqueHayaUnEligeAntiguo() throws Exception {
        Campana c = new Campana(catalogo132(), "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        Campana.Serie vieja = c.nuevaSerie("2026-09-19T09:00:00-0500", "SLV-002", MAC, "V3.6", "P28", 0, 'e');
        c.agregarDisparo(vieja, 1, "f", "::2270", 2270);
        c.cerrar(vieja, "OK", true, "19-sep, 1 x 9");
        c.elegir(vieja);
        Campana.Serie banco = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, "V3.6", "P28", 0, 'e');
        c.agregarDisparo(banco, 1, "f2", "::2280", 2280);
        c.cerrar(banco, "OK", true, "banco");
        assertEquals(vieja.id, c.elegida("P28").id);        // lo que QA-3610-10 describe
        c.elegir(banco);                                    // lo que hace ahora BancoActivity al aceptar
        assertEquals(banco.id, c.elegida("P28").id);
    }
}
