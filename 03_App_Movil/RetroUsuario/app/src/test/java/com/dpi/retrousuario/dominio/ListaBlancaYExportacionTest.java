package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-19a — lista blanca de bytes transmitidos, aseverada sobre lo que RECIBE el simulador, no
 * sobre `tramas.log` (M-6, corrige r3). T-USR-20a — ciclo completo sin red. Ambas sobre el mismo
 * ciclo: detección (RF-USR-01) + sonda (RF-USR-02) + una serie de "Medir y exportar" (RF-USR-04) +
 * exportar (RF-USR-15 bis). TDD-V3.6.md §8.
 */
public class ListaBlancaYExportacionTest {

    private File carpeta;
    private File ficheroDiario;

    @Before
    public void preparar() throws IOException {
        carpeta = new File(System.getProperty("java.io.tmpdir"), "usr19a-" + System.nanoTime());
        carpeta.mkdirs();
        ficheroDiario = File.createTempFile("diario-usr19a", ".txt");
        ficheroDiario.deleteOnExit();
    }

    @After
    public void limpiar() {
        ficheroDiario.delete();
        for (File f : carpeta.listFiles()) {
            f.delete();
        }
        carpeta.delete();
    }

    /** (b) y (c): ciclo completo contra un único simulador; lista blanca y proyección TX del `tramas.log` exportado. */
    @Test
    public void cicloCompletoSoloEnviaLaListaBlancaYElRegistroCoincideConElSimulador() throws IOException {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos()
                .responde("#V#", "#V,3.6,2026-09-19,CAL,0003#")
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");
        RegistroTramas log = new RegistroTramas();
        Canal canal = new CanalRegistrado(sim, log, sim);

        DetectorEquipo.ResultadoDeteccion deteccion = DetectorEquipo.detectar(canal);
        assertEquals(DetectorEquipo.Resultado.COMPATIBLE, deteccion.resultado());
        Sonda362.ResultadoSonda sonda = Sonda362.sondear(canal);
        assertEquals(Sonda362.Resultado.OK, sonda.resultado());

        Diario diario = new Diario(ficheroDiario);
        SesionMedicion sesion = new SesionMedicion(sim, new ParametrosRitmo(), log, diario);
        String fechaGCTexto = sonda.fechaRegistrada() ? sonda.fechaCalibracion() : "NONE";
        sesion.registrarEquipo("00:21:13:05:19:3B", "#V,3.6,2026-09-19,CAL,0003#", sonda.serie(), sonda.serieLeida(),
                deteccion.respuestaV().estadoAjuste(), fechaGCTexto, true, FechaISO.de(2026, 9, 21));

        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        FilaMedida fila = sesion.medir("rojo", "2026-09-21T14:32:07-05:00", "", "", "sin_posicion");
        assertTrue(fila.valido);

        // (b) lista blanca aseverada sobre lo que RECIBIO el simulador (M-6, corrige r3).
        Set<String> permitidas = new HashSet<>(java.util.Arrays.asList("#V#", "#GC#", "#GN#", "1", "2", "3", "4", "5", "6"));
        for (String recibida : sim.tramasRecibidas()) {
            assertTrue("byte fuera de la lista blanca: " + recibida, permitidas.contains(recibida));
        }
        assertFalse(sim.tramasRecibidas().contains("e"));

        File zip = sesion.exportar(carpeta, new Date(1758470400000L), TimeZone.getTimeZone("America/Bogota"));

        // (c) proyeccion TX del tramas.log exportado == lo que recibio el simulador, en hex y en orden.
        List<String> txDelZip = leerColumnaTxHex(zip);
        List<String> esperadoHex = new ArrayList<>();
        for (String t : sim.tramasRecibidas()) {
            esperadoHex.add(RegistroTramas.hexDe(t));
        }
        assertEquals(esperadoHex, txDelZip);

        // (d) cabecera fija de inventario.csv, cero filas de datos.
        byte[] inventario = leerEntrada(zip, "inventario.csv");
        assertEquals(InventarioCsv.CABECERA + "\r\n", new String(inventario, StandardCharsets.UTF_8));
    }

    private static List<String> leerColumnaTxHex(File zip) throws IOException {
        byte[] datos = leerEntrada(zip, "tramas.log");
        List<String> out = new ArrayList<>();
        for (String linea : new String(datos, StandardCharsets.UTF_8).split("\n")) {
            String[] campos = linea.split(";", -1);
            if (campos.length >= 3 && "TX".equals(campos[1])) {
                out.add(campos[2]);
            }
        }
        return out;
    }

    private static byte[] leerEntrada(File zip, String nombre) throws IOException {
        try (ZipFile zf = new ZipFile(zip)) {
            ZipEntry entrada = zf.getEntry(nombre);
            try (InputStream in = zf.getInputStream(entrada)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            }
        }
    }
}
