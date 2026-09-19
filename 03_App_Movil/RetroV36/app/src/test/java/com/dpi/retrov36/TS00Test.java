package com.dpi.retrov36;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Test;

/**
 * T-S00 (05_Documentacion/TDD-V3.6.md:1201-1206): el simulador reproduce un registro real.
 *
 * Se reenvia a un {@link EquipoSimulado} recien grabado (EEPROM en blanco: #V,...,DEF,0000#, fabrica,
 * #GN,NONE#, #GC,NONE#, como estaba SLV-002 al empezar T4) cada peticion #...# del registro T4, en orden,
 * avanzando su reloj segun los t_ms del registro, y se compara su respuesta con la RX real byte a byte.
 * Las peticiones de medida (e, 9, codigos) no se comparan ni se reenvian: dependen del patron.
 *
 * Fuente: tramas/rtv36_20260919_114644.txt dentro de
 * 06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip (md5 ce1f35fc..., el mismo que cita
 * TablaCalibracion). La copia de "07 pruebas/19092026_1210/x/tramas/" no sirve: esta cortada en la linea
 * 2061 (87 255 bytes de 125 681), y es la que dio las 106 peticiones de REVISION-Arquitectura-P12 §3.
 *
 * El PIN va enmascarado en el registro (#L,****#): se sustituye por el del simulador.
 */
public class TS00Test {

    static final String ZIP = "06_Calibracion/SLV-002/campanas/campana_SLV-002_20260919_122727.zip";
    static final String MD5 = "ce1f35fc64439cbb602d014b725fadfb";
    static final String ENTRADA = "tramas/rtv36_20260919_114644.txt";

    /** Una peticion #...# del registro con lo que el equipo respondio. */
    static final class Peticion {
        final long t;
        final String tx;
        final StringBuilder rx = new StringBuilder();

        Peticion(long t, String tx) {
            this.t = t;
            this.tx = tx;
        }
    }

    static File buscarZip() {
        File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (d != null) {
            File z = new File(d, ZIP);
            if (z.isFile()) {
                return z;
            }
            d = d.getParentFile();
        }
        throw new AssertionError("no se encuentra " + ZIP + " subiendo desde " + System.getProperty("user.dir"));
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x & 0xFF));
        }
        return sb.toString();
    }

    static String deHex(String h) {
        StringBuilder sb = new StringBuilder();
        for (String p : h.trim().split(" +")) {
            if (!p.isEmpty()) {
                sb.append((char) Integer.parseInt(p, 16));
            }
        }
        return sb.toString();
    }

    /** Peticiones #...# del registro T4, con la RX recibida hasta la siguiente TX. */
    static List<Peticion> leerT4() throws Exception {
        File z = buscarZip();
        assertEquals("el ZIP de la campana no es el del acta", MD5,
                hex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(z.toPath()))));
        String texto;
        try (ZipFile zf = new ZipFile(z)) {
            ZipEntry e = zf.getEntry(ENTRADA);
            assertNotNull(ENTRADA, e);
            try (InputStream in = zf.getInputStream(e)) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bo.write(buf, 0, n);
                }
                texto = new String(bo.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        List<Peticion> ps = new ArrayList<>();
        Peticion actual = null;
        for (String linea : texto.split("\r?\n")) {
            if (linea.startsWith("#") || linea.isEmpty()) {
                continue;                                    // comentarios del registro
            }
            String[] c = linea.split(";", 4);
            if (c.length < 3) {
                continue;
            }
            String bytes = deHex(c[2]);
            if ("TX".equals(c[1])) {
                actual = bytes.startsWith("#") ? new Peticion(Long.parseLong(c[0]), bytes) : null;
                if (actual != null) {
                    ps.add(actual);
                }
            } else if ("RX".equals(c[1]) && actual != null) {
                actual.rx.append(bytes);
            }
        }
        return ps;
    }

    static String orden(String tx) {
        String cuerpo = tx.substring(1, tx.length() - 1);
        int coma = cuerpo.indexOf(',');
        return "#" + (coma < 0 ? cuerpo + "#" : cuerpo.substring(0, coma));
    }

    @Test
    public void tS00ReproduceElRegistroT4() throws Exception {
        List<Peticion> ps = leerT4();
        assertTrue("el registro T4 trae " + ps.size() + " peticiones #", ps.size() >= 100);

        EquipoSimulado sim = new EquipoSimulado();
        Map<String, int[]> cuenta = new LinkedHashMap<>();
        List<String> diferencias = new ArrayList<>();
        long t0 = ps.get(0).t;
        for (Peticion p : ps) {
            sim.avanzarMs(p.t - t0);
            t0 = p.t;
            String tx = p.tx.equals("#L,****#") ? "#L," + sim.pin + "#" : p.tx;
            Cliente.Respuesta r = sim.pedir(tx, null, 5000);
            String simulada = r.valida() ? r.trama : "(sin respuesta)";
            String real = p.rx.toString();
            int[] c = cuenta.computeIfAbsent(orden(p.tx), k -> new int[2]);
            c[1]++;
            if (simulada.equals(real)) {
                c[0]++;
            } else {
                diferencias.add(p.t + " " + p.tx + "\n    real:     " + real + "\n    simulada: " + simulada);
            }
        }

        StringBuilder informe = new StringBuilder("T-S00 contra T4 (" + ps.size() + " peticiones #):\n");
        for (Map.Entry<String, int[]> e : cuenta.entrySet()) {
            informe.append(String.format("  %-6s %3d/%-3d%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        System.out.print(informe);
        if (!diferencias.isEmpty()) {
            fail(informe + "Difieren " + diferencias.size() + ":\n" + String.join("\n", diferencias));
        }
        assertEquals(0, sim.apagados);
    }

    // ---------------------------------------------------------------- lo nuevo del simulador, contra el firmware

    @Test
    public void laPersistenciaEsLaDeLaEeprom() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        String g1 = sim.pedir("#G,1#", null, 0).trama;
        sim.curvas.put('1', Fabrica.ecuacion('1'));          // cambio solo en RAM
        sim.apagarYEncender();
        assertEquals(1, sim.apagados);
        assertTrue(sim.conectado);
        assertEquals(g1, sim.pedir("#G,1#", null, 0).trama);
        assertEquals("#V,3.6,2026-09-19,CAL,0003#", sim.pedir("#V#", null, 0).trama);
        assertEquals("#GN,SLV-002#", sim.pedir("#GN#", null, 0).trama);
        assertEquals("#GC,2026-09-19#", sim.pedir("#GC#", null, 0).trama);

        sim.corromperEeprom('2');                             // CRC mala: ese codigo, de fabrica (:188)
        sim.apagarYEncender();
        assertNull(sim.curvaEeprom('2'));
        assertEquals("#V,3.6,2026-09-19,CAL,0001#", sim.pedir("#V#", null, 0).trama);
    }

    @Test
    public void unEquipoEnBlancoRespondeNone() throws Exception {
        EquipoSimulado sim = new EquipoSimulado();
        assertEquals("#GC,NONE#", sim.pedir("#GC#", null, 0).trama);
        assertEquals("#GN,NONE#", sim.pedir("#GN#", null, 0).trama);
        assertEquals("#V,3.6,2026-09-19,DEF,0000#", sim.pedir("#V#", null, 0).trama);
        assertEquals("#ERR,PIN#", sim.pedir("#L,1234#", null, 0).trama);
        assertEquals("#OK#", sim.pedir("#L,2026#", null, 0).trama);
        assertEquals("#ERR,FORMATO#", sim.pedir("#SN,NONE#", null, 0).trama);
        assertEquals("#ERR,FORMATO#", sim.pedir("#SC,2026-02-30#", null, 0).trama);
    }

    @Test
    public void cincoPinMalosBloqueanHastaApagar() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        for (int i = 0; i < 5; i++) {
            assertEquals("#ERR,PIN#", sim.pedir("#L,0000#", null, 0).trama);
        }
        assertEquals("#ERR,BLOQUEADO#", sim.pedir("#L,1234#", null, 0).trama);   // :645-647
        assertFalse(sim.admin);
        sim.apagarYEncender();
        assertEquals("#OK#", sim.pedir("#L,1234#", null, 0).trama);
    }

    @Test
    public void elModoAdministradorCaducaA10MinutosSinTramas() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        assertEquals("#OK#", sim.pedir("#L,1234#", null, 0).trama);
        sim.avanzarMinutos(9);
        assertEquals("#OK#", sim.pedir("#SC,2026-09-20#", null, 0).trama);    // la trama renueva el plazo
        sim.avanzarMinutos(10);
        assertTrue(sim.admin);                                // 600 000 ms justos: aun no (> estricto, :338)
        sim.avanzarMs(1);
        assertFalse(sim.admin);
        assertEquals("#ERR,BLOQUEADO#", sim.pedir("#SC,2026-09-21#", null, 0).trama);
    }

    @Test
    public void sFueraDeRangoEsFormato() throws Exception {
        EquipoSimulado sim = EquipoSimulado.slv002();
        sim.pedir("#L,1234#", null, 0);
        // negativa en x = 600 (el caso del registro T4 a las 1257263 ms)
        assertEquals("#ERR,FORMATO#",
                sim.pedir("#S,8,0.00000000E+00,0.00000000E+00,3.00000000E-01,-2.00000000E+02#", null, 0).trama);
        // mas de 4000 en x = 4300
        assertEquals("#ERR,FORMATO#",
                sim.pedir("#S,8,0.00000000E+00,0.00000000E+00,1.00000000E+00,0.00000000E+00#", null, 0).trama);
        assertEquals("#ERR,FORMATO#", sim.pedir("#S,8,0,0,1x,0#", null, 0).trama);
        assertEquals(0, sim.mascara & (1 << Fabrica.indice('8')));
    }
}
