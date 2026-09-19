package com.dpi.retrov36;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Huellas de un fichero (md5 y SHA-256) en hexadecimal. Java puro. */
public final class Resumen {

    private Resumen() { }

    public static String hex(File f, String algoritmo) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algoritmo);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("algoritmo no disponible: " + algoritmo, e);
        }
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) {
                md.update(b, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte x : md.digest()) {
            sb.append(String.format("%02x", x & 0xFF));
        }
        return sb.toString();
    }
}
