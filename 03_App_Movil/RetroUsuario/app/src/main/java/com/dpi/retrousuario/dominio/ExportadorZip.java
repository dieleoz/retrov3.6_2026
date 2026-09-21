package com.dpi.retrousuario.dominio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP exportado (RF-USR-15 bis): `medidas.csv` + `tramas.log` + `inventario.csv` (sólo cabecera en
 * el incremento 1). Exportar es de sólo lectura sobre lo guardado: no borra nada (T-USR-27).
 *
 * <p>A4: además del fichero directo (API 24-28), la vía {@code MediaStore.Downloads} (API 29+, fuera
 * de este módulo Java puro) necesita escribir en el {@link OutputStream} que da
 * {@code ContentResolver.openOutputStream}, no en un {@link File}: {@link #escribir(OutputStream,
 * List, RegistroTramas)} y {@link #generarBytes(List, RegistroTramas)} sirven a las dos vías con el
 * mismo contenido.</p>
 */
public final class ExportadorZip {

    private ExportadorZip() {
    }

    public static void escribir(File destino, List<FilaMedida> filas, RegistroTramas log) {
        File carpeta = destino.getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new IllegalStateException("No se pudo crear la carpeta de exportacion: " + carpeta);
        }
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            escribir(fos, filas, log);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo exportar el ZIP: " + destino, e);
        }
    }

    /** A4: escribe el ZIP en {@code out} (usado también por la vía MediaStore.Downloads, API 29+). */
    public static void escribir(OutputStream out, List<FilaMedida> filas, RegistroTramas log) {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            escribirEntrada(zip, "medidas.csv", CsvMedidas.generar(filas));
            escribirEntrada(zip, "tramas.log", log.aBytes());
            escribirEntrada(zip, "inventario.csv", InventarioCsv.generar());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el ZIP", e);
        }
    }

    /** A4: el ZIP entero en memoria, para la vía MediaStore.Downloads (el nombre lo resuelve el sistema). */
    public static byte[] generarBytes(List<FilaMedida> filas, RegistroTramas log) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        escribir(out, filas, log);
        return out.toByteArray();
    }

    private static void escribirEntrada(ZipOutputStream zip, String nombre, byte[] datos) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(datos);
        zip.closeEntry();
    }
}
