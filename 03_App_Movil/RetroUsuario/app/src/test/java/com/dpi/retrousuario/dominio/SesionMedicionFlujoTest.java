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
        FilaMedida fila = sesion.medir("blanco", "2026-09-21T10:00:00-05:00", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
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
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
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
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
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
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
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
            sesion.medir("rojo", "x" + i, "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
        }
        File zip1 = sesion.exportar(carpeta, new Date(1000), TimeZone.getTimeZone("UTC"));
        assertEquals(2, sesion.filas().size()); // exportar no borra.

        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x2", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
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
        sesion.medir("rojo", "x1", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

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

    /** T-USR-01c(d) (SPEC r6, condición QA-2): con exigir_362 = false, un equipo que responde DEF a
     *  "#V#" y no contesta nada a #GN#/#GC# (mudo, no ERR,FORMATO) mide igual, con
     *  estadoCalibracion = "DEF" — el cuarto campo de #V# manda sobre "sin_fecha" aunque
     *  #GN#/#GC# no hayan contestado nada, no sólo cuando #GC# da una fecha (eso ya lo prueba
     *  defEsSinCalibracionAunqueGcTengaFecha, arriba, con gcRespondioAlgo = true).
     *
     *  <p>arq B-4 (condición sobre 0.3.1, corrige lo que hacía 766e6f2 aquí): el silencio de #GN#/#GC#
     *  ofrece reintentar SIEMPRE, con cualquier valor de exigir_362 (SPEC :115-122) — este flujo no es
     *  MainActivity, así que simula "reintentos agotados" con {@link Sonda362#vacio()} directamente,
     *  igual que haría MainActivity#mostrarSinRespuesta tras agotarlos con exigir_362 = false.</p> */
    @Test
    public void tUsr01cD_exigir362FalsoConDefYGnGcMudosMideConDef() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos()
                .responde("#V#", "#V,3.6,2026-09-19,DEF,0000#"); // #GN#/#GC# sin configurar: mudos.
        ParametrosRitmo params = new ParametrosRitmo();
        params.exigir362(false);

        DeteccionYSonda.Resultado deteccion = DeteccionYSonda.ejecutar(sim, params, ms -> { });
        assertEquals(DetectorEquipo.Resultado.COMPATIBLE, deteccion.deteccion().resultado());
        // arq B-4: silencio total ofrece reintentar primero, incluso con exigir_362 = false.
        assertEquals(Sonda362.Resultado.SIN_RESPUESTA_REINTENTAR, deteccion.sonda().resultado());
        Sonda362.ResultadoSonda sonda = Sonda362.vacio(); // reintentos agotados, exigir_362 = false: mide igual.
        assertFalse(sonda.serieLeida());
        assertFalse(sonda.fechaRegistrada());

        SesionMedicion sesion = new SesionMedicion(sim, params, new RegistroTramas(), new Diario(ficheroDiario));
        String fechaGC = sonda.fechaRegistrada() ? sonda.fechaCalibracion() : "NONE";
        sesion.registrarEquipo("MAC", deteccion.deteccion().respuestaV().crudo(), sonda.serie(), sonda.serieLeida(),
                deteccion.deteccion().respuestaV().estadoAjuste(), fechaGC, sonda.fechaRegistrada(),
                FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 105);
        sim.programarValor(10, 110);
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        assertEquals("DEF", fila.estadoCalibracion);
        assertEquals("", fila.fechaCalibracion);
        assertEquals("", fila.vencimiento);
        assertEquals("", fila.serieEquipo);
        assertEquals("ninguna", fila.serieOrigen);
    }

    /** QA-9: firmware_v con máscara "1A7F" viaja de extremo a extremo, sin reconstruir ni truncar, por
     *  DetectorEquipo → Sonda362 (vía DeteccionYSonda) → SesionMedicion → CsvMedidas (D-1/M1: el texto
     *  crudo de "#V#" tal cual, RespuestaV#crudo). */
    @Test
    public void mascara1A7FViajaSinCambiosDeExtremoAExtremo() {
        String crudoV = "#V,3.6,2026-09-19,CAL,1A7F#";
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos()
                .responde("#V#", crudoV)
                .responde("#GN#", "#GN,SLV-002#")
                .responde("#GC#", "#GC,2026-09-19#");
        ParametrosRitmo params = new ParametrosRitmo();

        DeteccionYSonda.Resultado deteccion = DeteccionYSonda.ejecutar(sim, params, ms -> { });
        assertEquals(DetectorEquipo.Resultado.COMPATIBLE, deteccion.deteccion().resultado());
        assertEquals("1A7F", deteccion.deteccion().respuestaV().mascaraHex());
        assertEquals(crudoV, deteccion.deteccion().respuestaV().crudo());
        Sonda362.ResultadoSonda sonda = deteccion.sonda();
        assertEquals(Sonda362.Resultado.OK, sonda.resultado());

        SesionMedicion sesion = new SesionMedicion(sim, params, new RegistroTramas(), new Diario(ficheroDiario));
        sesion.registrarEquipo("00:21:13:05:19:3B", deteccion.deteccion().respuestaV().crudo(), sonda.serie(),
                sonda.serieLeida(), deteccion.deteccion().respuestaV().estadoAjuste(), sonda.fechaCalibracion(),
                sonda.fechaRegistrada(), FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        assertEquals(crudoV, fila.firmwareV); // el mismo texto crudo, con la mascara 1A7F intacta.
        byte[] csv = CsvMedidas.generar(sesion.filas());
        String texto = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("firmware_v con 1A7F no aparece en medidas.csv:\n" + texto, texto.contains(crudoV));
    }

    /** QA-6 (M-2): sin haber conectado ningún equipo en la sesión (SesionMedicion recién construida,
     *  registrarEquipo nunca llamado), el segmento de nombre del ZIP es "VARIOS" (SPEC :281-283) —
     *  hoy sin prueba (segmentoSerieParaNombre nunca se ejercitaba con conectado = false). */
    @Test
    public void exportarSinHaberConectadoNingunEquipoUsaVarios() throws IOException {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim); // sin registrarEquipo: "conectado" sigue en false.

        File zip = sesion.exportar(carpeta, new Date(1000), TimeZone.getTimeZone("UTC"));
        assertTrue(zip.getName().startsWith("RTVU_VARIOS_"));
        assertEquals("RTVU_VARIOS_19700101-000001.zip", sesion.nombreZipSugerido(new Date(1000), TimeZone.getTimeZone("UTC")));
    }

    /** QA-6: con filas de más de un equipo (series distintas) en el mismo medidas.csv, también "VARIOS". */
    @Test
    public void exportarConFilasDeMasDeUnEquipoUsaVarios() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC1", "#V,3.6,2026-09-19,CAL,0000#", "SLV-001", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x1", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        // "cambia de equipo": otra sesion sobre el MISMO diario, otra serie (el diario es la fuente de verdad).
        SesionMedicion sesion2 = new SesionMedicion(sim, new ParametrosRitmo(), new RegistroTramas(), new Diario(ficheroDiario));
        sesion2.registrarEquipo("MAC2", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion2.medir("rojo", "x2", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        assertEquals("RTVU_VARIOS_19700101-000001.zip",
                sesion2.nombreZipSugerido(new Date(1000), TimeZone.getTimeZone("UTC")));
    }

    /** B-4 (condición QA-8): tras exportar, filasCortadasIgnoradas() refleja lo que el Diario descartó
     *  (B-1), para que el exportador pueda avisar de las filas cortadas en vez de callarlas. */
    @Test
    public void filasCortadasIgnoradasSeReflejanTrasExportar() throws IOException {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        assertTrue(sesion.filasCortadasIgnoradas().isEmpty()); // nada exportado/leido todavia.

        String lineaCortada = "FILA\u00012026-09-21T10:00:00-05:00\u00014,609712\u0001-74,081753"
                + "\u0001con_posicion\u0001azul\u00014\u00013\u0001100|110|120\u000111";
        try (java.io.FileWriter w = new java.io.FileWriter(ficheroDiario, true)) {
            w.write(lineaCortada);
            w.write('\n');
        }
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        sesion.exportar(carpeta, new Date(1000), TimeZone.getTimeZone("UTC"));
        assertEquals(1, sesion.filasCortadasIgnoradas().size());
        assertTrue(sesion.filasCortadasIgnoradas().get(0).startsWith("FILA"));
    }

    /** QA-9: tres exportaciones con el MISMO Date exacto dan base, "_2" y "_3", no tres nombres iguales
     *  ni el "(1)"/"(2)" que MediaStore añadiría solo (extiende D-4, exportarDosVecesConElMismoDateProduceElSufijoDeColision). */
    @Test
    public void tresExportacionesMismoInstanteDanBaseGuion2Guion3() {
        EquipoSimuladoDisparos sim = new EquipoSimuladoDisparos();
        SesionMedicion sesion = nuevaSesion(sim);
        sesion.registrarEquipo("MAC", "#V,3.6,2026-09-19,CAL,0000#", "SLV-002", true,
                RespuestaV.EstadoAjuste.CAL, "2026-09-19", true, FechaISO.de(2026, 9, 21));
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sim.programarValor(10, 100);
        sesion.medir("rojo", "x1", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));

        Date mismoInstante = new Date(1758470400000L);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        File zip1 = sesion.exportar(carpeta, mismoInstante, utc);
        File zip2 = sesion.exportar(carpeta, mismoInstante, utc);
        File zip3 = sesion.exportar(carpeta, mismoInstante, utc);

        assertTrue(zip1.exists());
        assertTrue(zip2.exists());
        assertTrue(zip3.exists());
        assertFalse(zip1.getName().endsWith("_2.zip"));
        assertFalse(zip1.getName().endsWith("_3.zip"));
        assertTrue(zip2.getName().endsWith("_2.zip"));
        assertTrue(zip3.getName().endsWith("_3.zip"));
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
        FilaMedida fila = sesion.medir("rojo", "x", "", "", "sin_posicion", PreguntaOperadorFalsa.siempre(PreguntaOperador.Decision.SALTAR));
        assertEquals(5, fila.n);
    }
}
