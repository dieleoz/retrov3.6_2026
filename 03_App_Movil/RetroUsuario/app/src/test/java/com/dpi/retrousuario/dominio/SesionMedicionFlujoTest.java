package com.dpi.retrousuario.dominio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * T-USR-07 (cero tecleo, "SIN SERIE"), T-USR-22 (estado de calibración de `#V#`), T-USR-23 (GPS
 * ausente, se mide igual), T-USR-27 (exportar no borra) y T-USR-28 (`lecturasPorColor` se fija en
 * Ajustes, nunca en la API de medir). TDD-V3.6.md §8.
 */
public class SesionMedicionFlujoTest {

    private File carpeta;
    private File ficheroDiario;

    @Before
    public void preparar() throws IOException {
        carpeta = new File(System.getProperty("java.io.tmpdir"), "usr-flujo-" + System.nanoTime());
        carpeta.mkdirs();
        ficheroDiario = File.createTempFile("diario-flujo", ".txt");
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

    private SesionMedicion nuevaSesion(EquipoSimuladoDisparos sim) {
        return new SesionMedicion(sim, new ParametrosRitmo(), new RegistroTramas(), new Diario(ficheroDiario));
    }

    /** T-USR-07(b): equipo 3.6.2 que responde #GN,NONE#: la fila lleva serie_equipo vacío, "ninguna", sin teclear nada. */
    @Test
    public void sinSerieGrabadaLaFilaQuedaVaciaSinTeclearNada() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("00:21:13:05:19:3B", "#V,3.6,2026-09-19,CAL,0000#", "", false,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(50, 100);
        sim.programarValor(50, 110);
        sim.programarValor(50, 120);
        FilaMedida fila = sesion.medir("blanco", "2026-09-21T10:00:00-05:00", "", "", "sin_posicion");
        assertEquals("", fila.serieEquipo);
        assertEquals("ninguna", fila.serieOrigen);
    }

    /** T-USR-22(a): DEF es "sin calibración" aunque #GC# tenga fecha: el cuarto campo de #V# manda. */
    @Test
    public void defEsSinCalibracionAunqueGcTengaFecha() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,DEF,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.DEF, "2025-01-01", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion");
        assertEquals("DEF", fila.estadoCalibracion);
        assertEquals("", fila.fechaCalibracion);
        assertEquals("", fila.vencimiento);
    }

    /** T-USR-22(b): máscara distinta de 0000 no cambia nada; CAL con fecha vigente. */
    @Test
    public void mascaraDistintaDeCeroNoCambiaElEstadoCalibrado() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0003#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion");
        assertEquals("CAL", fila.estadoCalibracion);
        assertEquals("2026-09-19", fila.fechaCalibracion);
    }

    /** T-USR-22(c): "#V#" sin el quinto campo (4 campos, PROTOCOLO-V3.6.md:42) también se acepta. */
    @Test
    public void respuestaVDeCuatroCamposSeAceptaIgual() {
        RespuestaV v = RespuestaV.analizar("#V,3.6,2026-09-19,CAL#");
        assertNotNull(v);
        assertEquals(RespuestaV.EstadoAjuste.CAL, v.estadoAjuste());
    }

    /** T-USR-23: sin posición GPS, se mide igual; latitud/longitud vacías, gps_estado = sin_posicion. */
    @Test
    public void sinPosicionGpsSeMideIgual() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion");
        assertNotNull(fila);
        assertEquals("", fila.latitud);
        assertEquals("", fila.longitud);
        assertEquals("sin_posicion", fila.gpsEstado);
    }

    /** T-USR-27: exportar no borra; se puede exportar dos veces, con nombres distintos y filas crecientes. */
    @Test
    public void exportarNoBorraYSePuedeExportarDosVeces() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        for (int i = 0; i < 2; i++) {
            sim.programarValor(10, 100);
            sim.programarValor(10, 100);
            sim.programarValor(10, 100);
            sesion.medir("rojo", "x" + i, "", "", "sin_posicion");
        }
        File zip1 = sesion.exportar(carpeta, new Date(1000), TimeZone.getTimeZone("UTC"));
        assertEquals(2, sesion.filas().size()); // exportar no borra.

        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x2", "", "", "sin_posicion");
        File zip2 = sesion.exportar(carpeta, new Date(2000), TimeZone.getTimeZone("UTC"));

        assertTrue(zip1.exists());
        assertTrue(zip2.exists());
        assertTrue(!zip1.getName().equals(zip2.getName()));
        assertEquals(3, sesion.filas().size());
    }

    /** D-4: exportar dos veces con el MISMO Date (mismo segundo exacto) no pisa el primer ZIP: el
     *  segundo nombre lleva el sufijo de colisión "_2" (NombreZip.resolverColision), y las dos
     *  exportaciones sobreviven en la carpeta con sus dos ficheros distintos. */
    @Test
    public void exportarDosVecesConElMismoDateProduceElSufijoDeColision() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x1", "", "", "sin_posicion");

        Date mismoInstante = new Date(1758470400000L);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        File zip1 = sesion.exportar(carpeta, mismoInstante, utc);
        File zip2 = sesion.exportar(carpeta, mismoInstante, utc); // MISMO Date exacto: colisión de nombre.

        assertTrue(zip1.exists());
        assertTrue(zip2.exists());
        assertFalse(zip1.getName().equals(zip2.getName()));
        assertTrue("el segundo nombre no lleva el sufijo de colision: " + zip2.getName(),
                zip2.getName().endsWith("_2.zip"));
        // el primer nombre NO lleva sufijo (es el que existia cuando se resolvio la colision del segundo).
        assertFalse(zip1.getName().endsWith("_2.zip"));
    }

    /** T-USR-28: la API de medir no recibe lecturasPorColor; sólo Ajustes (ParametrosRitmo) lo cambia. */
    @Test
    public void lecturasPorColorSoloSeFijaEnParametrosNuncaEnMedir() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        ParametrosRitmo params = new ParametrosRitmo();
        SesionMedicion sesion = new SesionMedicion(sim, params, new RegistroTramas(), new Diario(ficheroDiario));
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        params.lecturasPorColor(5); // "Ajustes", fuera del flujo de medir.
        for (int i = 0; i < 5; i++) {
            sim.programarValor(10, 100 + i);
        }
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion");
        assertEquals(5, fila.n);
    }
}
