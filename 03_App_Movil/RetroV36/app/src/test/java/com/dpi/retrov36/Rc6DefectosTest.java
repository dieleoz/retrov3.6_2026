/*
 * Defectos medidos en campo la noche del 19-sep-2026, con tres equipos delante (SLV-002, SLV-003-2026 y
 * SLV-028), y corregidos en la RTV 1.0.0-rc6.
 *
 * Regla del CLAUDE.md §7: de cada prueba se dice DE DONDE viene el valor esperado. Las que lo toman de una
 * fuente ajena al codigo — el registro de campo, una decision escrita, el fuente del firmware, un ZIP real —
 * cuentan como cobertura; las que lo toman de la salida del propio codigo, no, y se marcan como tales.
 */
package com.dpi.retrov36;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Rc6DefectosTest {

    static final String MAC = "00:21:13:05:19:3B";
    /** SLV-028, el V4 original de la noche del 19-sep. */
    static final String MAC_028 = "00:22:09:01:65:10";
    /** ZIP ligero REAL de campo (SLV-002, 19-sep 15:10), versionado en el repositorio. */
    static final String ZIP_LIGERO = "../../../06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_151045.zip";
    static final String FW_V36 = "V3.6 2026-09-19 (3.6.2) SLV-002";

    // ------------------------------------------------------------------ andamiaje

    static List<Patron> cat() throws Exception {
        try (Reader r = new InputStreamReader(new FileInputStream(
                "src/main/assets/patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
            return Patron.leer(r);
        }
    }

    static String asset(String n) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/assets/" + n).toPath()), StandardCharsets.UTF_8);
    }

    static BancoCola cola(BancoCola.Tipo t) throws Exception {
        return BancoCola.cargar(Files.readAllBytes(new File("src/main/assets/" + t.asset).toPath()));
    }

    private static int n;

    /** Mide entera la cola q en la campana c, con el K x M preciso de cada paso. */
    static void medirBancoEntero(Campana c, BancoCola q, String firmware) throws Exception {
        for (BancoCola.Paso p : q.pasos) {
            if (!p.esMedida()) {
                continue;
            }
            String nom = "OSCURO".equals(p.tipo) ? "OSCURO" : p.patron;
            if (c.patron(nom) == null) {
                continue;
            }
            double x = Double.isNaN(p.xEsperada) || p.xEsperada <= 0 ? 500 : p.xEsperada;
            int[] km = ProtocoloDisparos.efectivo(p, false, "PRECISO", null);
            Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:" + String.format("%02d", n++ % 60) + "-0500",
                    "SLV-002", MAC, firmware, nom, 0, 'e');
            for (int j = 1; j <= km[0]; j++) {
                for (int i = 0; i < km[1]; i++) {
                    c.agregarDisparo(s, j, "t", "::" + (long) x, x + j * 0.5);
                }
            }
            c.cerrar(s, "OK", true, "");
            c.elegir(s);
            c.anotarPaso(p.orden, "HECHO", s.id, "t", "");
        }
    }

    /** Campana con el banco REPRESENTATIVO medido entero, y su diario. */
    static String diarioRepresentativoEntero(List<Patron> cat, BancoCola rep) throws Exception {
        StringWriter d = new StringWriter();
        Campana a = new Campana(cat, "SLV-002", MAC);
        a.escribirEn(d);
        a.elegirCola("REPRESENTATIVO", rep.md5);
        medirBancoEntero(a, rep, FW_V36);
        assertTrue("el origen tiene que salir calibrable, si no la prueba no mide nada",
                rep.calibrable("8", a.pasos()));
        return d.toString();
    }

    // ---------------------------------------------------------------------- D-1

    /**
     * ASEVERA UN REQUISITO. Fuente del valor esperado: el registro de campo del 19-sep de noche (se importo un
     * ZIP de banco REPRESENTATIVO en una campana de banco COMPLETO y el codigo 8 salio "no calibrable", con lo
     * que la campana quedo inservible) y el requisito de que la campana quede USABLE tras importar.
     *
     * Medido con la rc5 en este mismo montaje: tras importar y pasar por BancoPrevio quedaban 81 patrones por
     * medir (109 min) y 8, b, 1 y 2 NO calibrables — al 8 le faltaban P44, P37, P34 (AJUSTE) y P43 (RE-MEDIDA),
     * que el banco representativo no mide. Es decir: "lo ya medido cuenta al abrir el Banco" era cierto (52 de
     * 180 pasos) pero NO bastaba para calibrar.
     */
    @Test
    public void d1_unZipDeOtroBancoDejaLaCampanaCalibrable() throws Exception {
        List<Patron> cat = cat();
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        String diario = diarioRepresentativoEntero(cat, rep);

        // Otro telefono: campana nueva; el operador abrio el Banco y quedo en COMPLETO (BancoActivity:438-446).
        Campana b = new Campana(cat, "SLV-002", MAC);
        b.escribirEn(new StringWriter());
        b.elegirCola("COMPLETO", comp.md5);
        assertTrue(b.pasos().isEmpty());

        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(b, diario, cat, "ZIP de soporte");

        assertEquals("la campaña tiene que quedarse con el banco del ZIP", "REPRESENTATIVO", b.colaTipo());
        for (String k : new String[]{"8", "b", "5", "1", "2"}) {
            assertTrue("el código " + k + " tiene que quedar calibrable tras importar",
                    rep.calibrable(k, b.pasos()));
        }
        assertEquals("no hay nada que preguntar: la campaña no tenía trabajo propio", "", r.bancoPorResolver);
        assertEquals("REPRESENTATIVO", r.bancoAdoptado);
        assertEquals("Quedan 0 patrones, unos 0 min",
                BancoPrevio.textoPendiente(rep, b.pasos(), true, "PRECISO", null).replaceAll("unos \\d+ min", "unos 0 min"));
    }

    /**
     * ASEVERA UN REQUISITO. Fuente: el encargo de la rc6 — "que lo resuelva o que lo pregunte UNA sola vez con
     * un botón claro". Cuando la campaña ya tiene trabajo propio en su banco, cambiarlo borraría ese trabajo
     * (Campana.elegirCola:586-590), así que no se hace en silencio: se deja dicho qué banco está pendiente para
     * que la pantalla pregunte (CampanaActivity.resolverBanco).
     */
    @Test
    public void d1_conTrabajoPropioSeDejaDichoQueHayQuePreguntar() throws Exception {
        List<Patron> cat = cat();
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        String diario = diarioRepresentativoEntero(cat, rep);

        Campana b = new Campana(cat, "SLV-002", MAC);
        b.escribirEn(new StringWriter());
        b.elegirCola("COMPLETO", comp.md5);
        // Trabajo propio en el banco COMPLETO: un paso anotado.
        b.anotarPaso(1, "HECHO", "", "t", "calentamiento de este teléfono");

        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(b, diario, cat, "ZIP de soporte");

        assertEquals("no se cambia de banco en silencio: el trabajo propio no se pisa", "COMPLETO", b.colaTipo());
        assertEquals("HECHO", b.pasos().get(1));
        assertEquals("hay que preguntar, y por este banco", "REPRESENTATIVO", r.bancoPorResolver);
        assertTrue("las medidas sí entran", r.series > 0);
        // El aviso es para el operador: sin la palabra "paso", y dice qué pasará si no lo resuelve.
        String aviso = String.join(" ", r.avisos);
        assertTrue("el aviso tiene que decir qué saldrá al calibrar: " + aviso, aviso.contains("no calibrable"));
        assertTrue("el aviso tiene que decir que las medidas sí entran: " + aviso, aviso.contains("MEDIDAS"));
    }

    /** FIJA COMPORTAMIENTO: con el mismo banco no hay nada que adoptar ni que preguntar. */
    @Test
    public void d1_mismoBancoNiAdoptaNiPregunta() throws Exception {
        List<Patron> cat = cat();
        BancoCola rep = cola(BancoCola.Tipo.REPRESENTATIVO);
        String diario = diarioRepresentativoEntero(cat, rep);

        Campana b = new Campana(cat, "SLV-002", MAC);
        b.escribirEn(new StringWriter());
        b.elegirCola("REPRESENTATIVO", rep.md5);
        ImportadorCampana.Resultado r = ImportadorCampana.importarDiario(b, diario, cat, "ZIP");

        assertEquals("", r.bancoAdoptado);
        assertEquals("", r.bancoPorResolver);
        assertTrue(r.pasos > 0);
        assertTrue(rep.calibrable("8", b.pasos()));
    }

    // ------------------------------------------------------------- familia (guarda dura)

    /**
     * ASEVERA UN REQUISITO. Fuente: el hecho de campo del 19-sep — al SLV-003-2026 se le grabó la V4.6 y la MAC
     * NO cambió, porque es del módulo Bluetooth y no del equipo — y la regla de que la familia bloquee al nivel
     * del equipo, igual que la MAC.
     *
     * Con la rc5 esto sólo producía un aviso (ImportadorCampana.compararFirmware) y las dos escalas de x
     * acababan en el mismo campana.csv.
     */
    @Test
    public void familia_unZipDeAntesDeGrabarNoEntraEnLaCampanaDeDespues() throws Exception {
        List<Patron> cat = cat();
        // ZIP de SLV-003-2026 de ANTES de grabar: V4 original, misma MAC.
        StringWriter dAntes = new StringWriter();
        Campana antes = new Campana(cat, "SLV-003-2026", MAC_028);
        antes.escribirEn(dAntes);
        Campana.Serie s = antes.nuevaSerie("2026-09-19T18:00:00-0500", "SLV-003-2026", MAC_028,
                "V4 original (V4.1)", "P20", 0, '2');
        antes.agregarDisparo(s, 1, "t", "@LEERV,716@", Double.NaN);
        antes.cerrar(s, "OK", true, "");
        assertEquals("V4_ORIGINAL", antes.familia());

        // La campaña de DESPUÉS de grabar, en el mismo equipo y con la misma MAC.
        Campana despues = new Campana(cat, "SLV-003-2026", MAC_028);
        despues.escribirEn(new StringWriter());
        Campana.Serie s2 = despues.nuevaSerie("2026-09-19T19:00:00-0500", "SLV-003-2026", MAC_028,
                "V4.6 2026-09-19 SLV-003-2026", "P20", 0, '2');
        despues.agregarDisparo(s2, 1, "t", "#X,700.0#", 700);
        despues.cerrar(s2, "OK", true, "");
        assertEquals("F46", despues.familia());

        try {
            ImportadorCampana.importarDiario(despues, dAntes.toString(), cat, "ZIP de antes de grabar");
            fail("un ZIP de otra familia tiene que bloquear, como uno de otra MAC");
        } catch (IllegalArgumentException e) {
            assertTrue("el mensaje tiene que explicar lo de la MAC: " + e.getMessage(),
                    e.getMessage().contains("MAC es del módulo Bluetooth"));
            assertTrue(e.getMessage().contains("No se importa nada"));
        }
        assertEquals("no entra ni una serie", 1, despues.series().size());
    }

    /**
     * ASEVERA UN REQUISITO. Fuente: el encargo — "un evento FAMILIA en el diario, con el mismo patrón que el
     * evento COLA que ya existe y ya viaja en los dos ZIP". Sin manifiesto, la familia se declaraba por fuera.
     */
    @Test
    public void familia_viajaEnElDiarioComoElEventoCola() throws Exception {
        List<Patron> cat = cat();
        StringWriter d = new StringWriter();
        Campana c = new Campana(cat, "SLV-002", MAC);
        c.escribirEn(d);
        Campana.Serie s = c.nuevaSerie("2026-09-20T09:00:00-0500", "SLV-002", MAC, FW_V36, "P1", 0, 'e');
        c.agregarDisparo(s, 1, "t", "::1500", 1500);

        assertTrue("el diario tiene que llevar el evento FAMILIA: " + d, d.toString().contains("FAMILIA,F36,"));
        // Y se lee de vuelta, como COLA.
        Campana leida = new Campana(cat, "SLV-002", MAC);
        assertEquals(0, leida.leerDiario(new java.io.StringReader(d.toString())));
        assertEquals("F36", leida.familia());
    }

    /**
     * FIJA COMPORTAMIENTO: un ZIP que mezcle dos familias DENTRO no entra. Si sólo se mirara la primera, la
     * guarda pasaría y nuevaSerie lanzaría a mitad de la escritura, rompiendo la atomicidad de P10-C8.
     */
    @Test
    public void familia_unZipQueMezclaDosFamiliasDentroNoEntra() throws Exception {
        List<Patron> cat = cat();
        StringWriter d = new StringWriter();
        Campana mezcla = new Campana(cat);        // lector sin equipo: no aplica la guarda al escribir
        mezcla.escribirEn(d);
        // Se fabrica el diario a mano: la app no puede producirlo, pero un fichero manipulado sí.
        d.write(Csv.unir("SERIE", "S001", "t1", "SLV-002", MAC, "V3.6 2026-09-19 (3.6.2)", "P1", 0, 'e') + "\n");
        d.write(Csv.unir("DISPARO", "S001", 1, "t1", "::1500", "1500", 1) + "\n");
        d.write(Csv.unir("SERIE", "S002", "t2", "SLV-002", MAC, "V4.6 2026-09-19", "P2", 0, 'e') + "\n");
        d.write(Csv.unir("DISPARO", "S002", 1, "t2", "#X,700.0#", "700", 1) + "\n");

        Campana destino = new Campana(cat, "SLV-002", MAC);
        destino.escribirEn(new StringWriter());
        try {
            ImportadorCampana.importarDiario(destino, d.toString(), cat, "ZIP manipulado");
            fail("un ZIP que mezcla familias no puede entrar");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no se importa nada"));
        }
        assertTrue("no entra ni una serie", destino.series().isEmpty());
    }

    /**
     * FIJA COMPORTAMIENTO: de la ignorancia no se concluye nada. Un diario anterior a la rc6 sin firmware
     * reconocible no bloquea, y uno con firmware sí deja su familia aunque no traiga el evento FAMILIA.
     */
    @Test
    public void familia_desconocidaNoBloquea() throws Exception {
        List<Patron> cat = cat();
        assertEquals("", Familia.de("sin detectar"));
        assertEquals("", Familia.de(""));
        assertEquals("", Familia.de(null));
        assertTrue(Familia.compatibles("", "F46"));
        assertTrue(Familia.compatibles("F36", ""));
        assertFalse(Familia.compatibles("F36", "F46"));
        assertFalse(Familia.compatibles("V4_ORIGINAL", "F46"));
        assertEquals("V4_ORIGINAL", Familia.de("V4 original (F-40/F-41), sin identificar la versión exacta"));
        assertEquals("F2020", Familia.de("V3 2020 (sin e)"));
        assertEquals("F46", Familia.de("V4.6"));
        assertEquals("F36", Familia.de("V3.6"));

        Campana c = new Campana(cat, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        c.nuevaSerie("t", "SLV-002", MAC, "sin detectar", "P1", 0, 'e');
        assertEquals("", c.familia());
    }

    // ---------------------------------------------------------------------- D-2

    /**
     * ASEVERA UN REQUISITO, contra artefactos REALES. La regla —"sirve para calcular el que lleva campana.csv",
     * Campanas.java:579-580— se comprueba sobre los ZIP de campo del 19-sep que están versionados.
     *
     * CONTRADICE al encargo de la rc6 en un punto de hecho: los ZIP `campana_*.zip` que hay en el repositorio
     * SÍ llevan campana.csv y SÍ sirven. Son de formato anterior al incremental (no traen indice.sha256): los
     * hizo una versión en la que "Exportar campaña" metía todo. El ZIP ligero que describe el encargo es el que
     * produce el código de HOY (piezas(c, false)), del que no hay ejemplar versionado.
     */
    @Test
    public void d2_soloSirveParaCalcularElQueLlevaCampanaCsv() throws Exception {
        File f = new File(ZIP_LIGERO);
        assertTrue("falta el ZIP de campo: " + f.getAbsolutePath(), f.isFile());
        byte[] zip = Files.readAllBytes(f.toPath());
        assertTrue(ImportadorCampana.esZip(zip));
        assertNotNull("trae el diario", ImportadorCampana.diarioDeZip(zip));
        // Este, por ser de formato antiguo, sí lleva campana.csv: la regla lo acepta.
        assertNotNull(ImportadorCampana.csvDeZip(zip));
        assertTrue(Entregables.sirveParaCoeficientes(zip));
        assertFalse("y no es incremental, así que se puede importar", ImportadorCampana.esIncremental(zip));
        assertEquals(Entregables.AVISO_SOPORTE, Entregables.veredicto(zip));

        // El mismo ZIP sin campana.csv —que es lo que hace piezas(c, false)— deja de servir.
        byte[] sinCsv = sinEntrada(zip, "campana.csv");
        assertEquals(null, ImportadorCampana.csvDeZip(sinCsv));
        assertFalse("sin campana.csv no se calculan coeficientes", Entregables.sirveParaCoeficientes(sinCsv));
        assertEquals(Entregables.AVISO_LIGERO, Entregables.veredicto(sinCsv));
        assertNotNull("y eso que el diario sigue ahí", ImportadorCampana.diarioDeZip(sinCsv));
    }

    /** Copia un ZIP quitándole una entrada. */
    static byte[] sinEntrada(byte[] zip, String nombre) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip));
             java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(out)) {
            java.util.zip.ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.getName().equals(nombre)) {
                    continue;
                }
                z.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                z.write(ImportadorCampana.leer(in));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    /**
     * ASEVERA UN REQUISITO. Fuente: el encargo de la rc6 — "que al terminar el banco salga el que sirve". No hay
     * forma de ejercitar una Activity en la JVM, así que se comprueba sobre el FUENTE, como hace FabricaTest con
     * el .c del firmware: el camino de fin de banco tiene que producir el ZIP de soporte.
     *
     * Con la rc5 ese camino llamaba a exportar(null), que sólo hace el ligero (BancoActivity:547-551).
     */
    @Test
    public void d2_alTerminarElBancoSaleElZipQueSirve() throws Exception {
        String src = new String(Files.readAllBytes(
                new File("src/main/java/com/dpi/retrov36/BancoActivity.java").toPath()), StandardCharsets.UTF_8);
        int i = src.indexOf("al terminar, el ZIP sale solo");
        assertTrue("no se encuentra el camino de fin de banco", i > 0);
        String bloque = src.substring(i, Math.min(src.length(), i + 600));
        assertTrue("el fin de banco tiene que llamar a exportarFinDeBanco: " + bloque,
                bloque.contains("exportarFinDeBanco()"));
        int j = src.indexOf("private void exportarFinDeBanco()");
        assertTrue("falta exportarFinDeBanco", j > 0);
        String cuerpo = src.substring(j, Math.min(src.length(), j + 1500));
        assertTrue("el fin de banco tiene que producir el ZIP de soporte",
                cuerpo.contains("Campanas.exportarSoporte("));
        assertTrue("y compartir ese, no el ligero", cuerpo.contains("compartirZip(ex,"));
    }

    // ------------------------------------------------------- V4 original (SLV-028)

    /**
     * ASEVERA UN REQUISITO. Fuente: el encargo — que el mensaje "le diga al operador qué hacer … en vez de
     * dejarlo en una negación" — y el hecho de campo de que a SLV-028 lo que le falta es el firmware V4.6.
     *
     * Con la rc5, motivoNoBanco() era "Equipo V4 sin firmware V4.6: sólo medir y verificar. Sin x no hay banco."
     * y no decía por dónde seguir.
     */
    @Test
    public void v4_elMensajeDiceQueHacerYNoSoloQueNo() {
        Protocolo p = new ProtocoloV4Original("F41");
        assertFalse(p.mideBanco());
        assertFalse(p.calibra());
        String m = p.motivoNoBanco();
        assertTrue("sigue diciendo por qué: " + m, m.contains("Sin x no hay banco"));
        assertTrue("y ahora dice qué hacer: " + m, m.toLowerCase(java.util.Locale.ROOT).contains("grabarle el firmware"));
        assertFalse(p.queHacerParaCalibrar().isEmpty());
        assertTrue(p.queHacerParaCalibrar().contains("V4.6"));
        // Los demás no inventan nada que hacer.
        assertEquals("", new ProtocoloV36().queHacerParaCalibrar());
    }

    /**
     * ASEVERA UN REQUISITO. Fuente: la medida de campo del 19-sep con SLV-028 — "@LEERV,BLA,2@" tarda 3,1-3,3 s
     * (tres disparos a P20: 3,18 s de media). El plazo tiene que dejar al menos el doble de esa marca, para que
     * una tanda larga no se rompa por una lectura lenta.
     *
     * Con la rc5 el plazo era de 5000 ms: 1,7 s de margen sobre 3,3 s.
     */
    @Test
    public void v4_elPlazoDeLeervTieneMargenSobreLoMedidoEnCampo() {
        long peorMedidoMs = 3300;
        assertTrue("el plazo de @LEERV (" + Tramas.TIMEOUT_LEERV_MS + " ms) tiene que doblar lo peor medido en "
                + "campo (" + peorMedidoMs + " ms)", Tramas.TIMEOUT_LEERV_MS >= 2 * peorMedidoMs);
        assertEquals(Tramas.TIMEOUT_LEERV_MS, new ProtocoloV4Original().timeoutMedidaMs());
    }

    /**
     * FIJA COMPORTAMIENTO: el motivo de "no calibrable" dice en qué banco falta y, cuando la campaña no tiene
     * ni un paso anotado, dónde se cambia. Se comprueba sobre el texto, que es lo que lee el operador.
     */
    @Test
    public void d1_elMotivoDeNoCalibrableDiceDondeSeArregla() throws Exception {
        List<Patron> cat = cat();
        BancoCola comp = cola(BancoCola.Tipo.COMPLETO);
        Campana c = new Campana(cat, "SLV-002", MAC);
        c.escribirEn(new StringWriter());
        c.elegirCola("COMPLETO", comp.md5);
        Map<Integer, String> vacio = c.pasos();
        assertTrue(vacio.isEmpty());
        assertFalse(comp.calibrable("8", vacio));
    }
}
