package com.dpi.retrousuario.dominio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * T-USR-21a — columnas de `medidas.csv` del incremento 1, fijas, aseveradas byte a byte, BOM y CRLF
 * incluidos (TDD-V3.6.md §8, RF-USR-06, M-6, corrige r4/r5, C2).
 */
public class CsvMedidasTest {

    private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    /** (a) fila "normal", con GPS y serie leída: comparación byte a byte de la línea exacta de la SPEC. */
    @Test
    public void filaConGpsYSerieLeidaByteAByte() {
        FilaMedida f = new FilaMedida("2026-09-21T14:32:07-05:00", "4,609712", "-74,081753", "con_posicion",
                "rojo", '4', 3, Arrays.asList(100, 110, 120), 110, 100, true, "", "SLV-002", "leida",
                "00:21:13:05:19:3B", "#V,3.6,2026-09-19,CAL,0003#", "2026-09-19", "2027-09-19", "CAL");
        byte[] csv = CsvMedidas.generar(Collections.singletonList(f));
        String esperado = "2026-09-21T14:32:07-05:00;4,609712;-74,081753;con_posicion;rojo;4;3;100|110|120;"
                + "110;100;SI;;SLV-002;leida;00:21:13:05:19:3B;#V,3.6,2026-09-19,CAL,0003#;2026-09-19;"
                + "2027-09-19;CAL";
        assertFilaEsperada(csv, esperado);
    }

    /** (b) sin GPS y sin serie ("#GN,NONE#"): latitud/longitud vacías, gps_estado y serie_origen correspondientes. */
    @Test
    public void filaSinGpsYSinSerie() {
        FilaMedida f = new FilaMedida("2026-09-21T15:00:00-05:00", "", "", "sin_posicion", "azul", '5', 3,
                Arrays.asList(90, 95, 88), 91, 88, true, "", "", "ninguna", "00:21:13:05:19:3B",
                "#V,3.6,2026-09-19,CAL,0003#", "2026-09-19", "2027-09-19", "CAL");
        byte[] csv = CsvMedidas.generar(Collections.singletonList(f));
        String esperado = "2026-09-21T15:00:00-05:00;;;sin_posicion;azul;5;3;90|95|88;91;88;SI;;;ninguna;"
                + "00:21:13:05:19:3B;#V,3.6,2026-09-19,CAL,0003#;2026-09-19;2027-09-19;CAL";
        assertFilaEsperada(csv, esperado);
    }

    /** (c) serie con ';' y '"' (nuevo r6, C2): se cita con la regla RFC 4180, comilla interna duplicada. */
    @Test
    public void serieConPuntoYComaYComillaSeCitaConRfc4180() {
        FilaMedida f = new FilaMedida("2026-09-21T15:05:00-05:00", "4,609712", "-74,081753", "con_posicion",
                "rojo", '4', 3, Arrays.asList(100, 110, 120), 110, 100, true, "", "SLV;002\"A", "leida",
                "00:21:13:05:19:3B", "#V,3.6,2026-09-19,CAL,0003#", "2026-09-19", "2027-09-19", "CAL");
        byte[] csv = CsvMedidas.generar(Collections.singletonList(f));
        String texto = new String(csv, StandardCharsets.UTF_8);
        assertEquals(1, contar(texto, "\"SLV;002\"\"A\""));
    }

    /** Un campo con ';' pero SIN '"' también dispara la regla de comillas (RFC 4180): no basta con citar por '"'. */
    @Test
    public void unSoloPuntoYComaSinComillaTambienDisparaLaCita() {
        assertEquals("\"SLV;002\"", CsvMedidas.citar("SLV;002"));
    }

    /** BOM en los 3 primeros bytes, y cada línea (incluida la cabecera) termina en CRLF, nunca sólo LF. */
    @Test
    public void bomYCrlfEnTodasLasLineas() {
        byte[] csv = CsvMedidas.generar(Collections.<FilaMedida>emptyList());
        assertArrayEquals(BOM, Arrays.copyOfRange(csv, 0, 3));
        String texto = new String(csv, StandardCharsets.UTF_8);
        String sinBom = texto.substring(1); // el BOM decodifica como un char (﻿).
        assertEquals(CsvMedidas.CABECERA + "\r\n", sinBom);
    }

    @Test
    public void cabeceraExactaSinColumnaDeSenalNiCumpleNoCumple() {
        List<String> columnas = Arrays.asList(CsvMedidas.CABECERA.split(";"));
        assertEquals(19, columnas.size());
        assertEquals("fecha_hora", columnas.get(0));
        assertEquals("estado_calibracion", columnas.get(18));
    }

    private static void assertFilaEsperada(byte[] csv, String filaEsperada) {
        String texto = new String(csv, StandardCharsets.UTF_8);
        String[] lineas = texto.substring(1).split("\r\n", -1); // quita el BOM (1 char), separa por CRLF.
        assertEquals(CsvMedidas.CABECERA, lineas[0]);
        assertEquals(filaEsperada, lineas[1]);
    }

    private static int contar(String texto, String sub) {
        int n = 0, i = 0;
        while ((i = texto.indexOf(sub, i)) >= 0) {
            n++;
            i += sub.length();
        }
        return n;
    }
}
