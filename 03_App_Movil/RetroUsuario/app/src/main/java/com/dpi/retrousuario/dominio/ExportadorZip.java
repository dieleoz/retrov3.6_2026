package com.dpi.retrousuario.dominio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP exportado (RF-USR-15 bis): `medidas.csv` + `tramas.log` + `inventario.csv` (sólo cabecera en
 * el incremento 1). Exportar es de sólo lectura sobre lo guardado: no borra nada (T-USR-27).
 */
final class ExportadorZip {

    private ExportadorZip() {
    }

    static void escribir(File destino, List<FilaMedida> filas, RegistroTramas log) {
        File carpeta = destino.getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            throw new IllegalStateException("No se pudo crear la carpeta de exportacion: " + carpeta);
        }
        try (FileOutputStream fos = new FileOutputStream(destino);
                ZipOutputStream zip = new ZipOutputStream(fos)) {
            escribirEntrada(zip, "medidas.csv", CsvMedidas.generar(filas));
            escribirEntrada(zip, "tramas.log", log.aBytes());
            escribirEntrada(zip, "inventario.csv", InventarioCsv.generar());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo exportar el ZIP: " + destino, e);
        }
    }

    private static void escribirEntrada(ZipOutputStream zip, String nombre, byte[] datos) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(datos);
        zip.closeEntry();
    }
}
