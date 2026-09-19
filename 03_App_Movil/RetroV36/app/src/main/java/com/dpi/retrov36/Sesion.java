package com.dpi.retrov36;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Estado de la conexion en curso: equipo, version, pruebas y medidas. */
public final class Sesion {

    public enum Version {
        SIN_DETECTAR("sin detectar"),
        V36("V3.6"),
        V3_2020("V3 2020"),
        V4("V4 (la app no aplica)"),
        DESCONOCIDA("desconocida");

        public final String texto;

        Version(String t) {
            texto = t;
        }
    }

    private static final Sesion INSTANCIA = new Sesion();

    public static Sesion get() {
        return INSTANCIA;
    }

    public volatile Version version = Version.SIN_DETECTAR;
    public volatile String fechaFirmware = "";
    /** "CAL" o "DEF" en V3.6; vacio en otro caso. */
    public volatile String marca = "";
    /** Mascara de #V# (revision 1.1); -1 si no la trae. */
    public volatile int mascara = -1;
    /** Ultima trama '#' respondida (elapsedRealtime), para la caducidad del modo admin. */
    public volatile long ultimaAlmohadillaMs;
    /** Fallos de PIN seguidos en esta conexion (el firmware bloquea #L a los 5). */
    public volatile int fallosPin;
    /** true si el equipo responde a 'e' (siempre en V3.6 segun contrato). */
    public volatile boolean eDisponible;
    public volatile String nombre = "";
    public volatile String mac = "";

    /** Coeficientes leidos con #G (V3.6); null si no leidos. */
    public final Ecuacion[] leidas = new Ecuacion[Fabrica.CODIGOS.length];
    public volatile double[] temperatura;

    /** null: pruebas no hechas; true/false: APTO / NO APTO. */
    public volatile Boolean apto;
    public volatile String resumenPruebas = "";
    public volatile boolean overrideAdmin;
    /** El operador acepto medir con NO APTO en esta conexion. */
    public volatile boolean medirNoAptoAceptado;
    public volatile boolean adminDesbloqueado;

    private final List<Medida> medidas = Collections.synchronizedList(new ArrayList<Medida>());
    private File csvMedidas;
    private File csvBotones;
    private File csvCoeficientes;
    private File csvLineaBase;
    private List<Patron> patrones;

    private Sesion() { }

    /** Se llama al conectar: todo lo del equipo anterior se olvida. */
    public synchronized void reiniciar(String nombre, String mac) {
        this.nombre = nombre;
        this.mac = mac;
        version = Version.SIN_DETECTAR;
        fechaFirmware = "";
        marca = "";
        mascara = -1;
        ultimaAlmohadillaMs = 0;
        fallosPin = 0;
        eDisponible = false;
        for (int i = 0; i < leidas.length; i++) {
            leidas[i] = null;
        }
        temperatura = null;
        apto = null;
        resumenPruebas = "";
        overrideAdmin = false;
        medirNoAptoAceptado = false;
        adminDesbloqueado = false;
        medidas.clear();
        csvMedidas = null;
        csvBotones = null;
        csvCoeficientes = null;
        csvLineaBase = null;
    }

    /** "SLV-002" de "COVIANDINA_SLV-002": lo que sigue al ultimo '_'. */
    public String serie() {
        String n = nombre == null ? "" : nombre;
        int i = n.lastIndexOf('_');
        return (i >= 0 && i < n.length() - 1) ? n.substring(i + 1) : n;
    }

    public String firmware() {
        switch (version) {
            case V36:
                return "V3.6 " + fechaFirmware + " " + marca
                        + (mascara >= 0 ? String.format(Locale.US, " mascara %04X", mascara) : "");
            case V3_2020:
                return "V3 2020 (sin e)";
            default:
                return version.texto;
        }
    }

    /** Identidad para cabeceras y exportaciones. */
    public String identidad() {
        return "equipo " + nombre + " (serie " + serie() + ", MAC " + mac + "), firmware " + firmware();
    }

    public boolean versionMedible() {
        return version == Version.V36 || version == Version.V3_2020;
    }

    /** Ecuacion con la que responde el equipo al codigo k. */
    public Ecuacion ecuacionVigente(char k) {
        int i = Fabrica.indice(k);
        if (version == Version.V36 && i >= 0 && leidas[i] != null) {
            return leidas[i];
        }
        return Fabrica.ecuacion(k);
    }

    public synchronized List<Patron> patrones(Context ctx) throws IOException {
        if (patrones == null) {
            try (InputStreamReader r = new InputStreamReader(
                    ctx.getAssets().open("patrones_certificados_P1-P31.csv"), StandardCharsets.UTF_8)) {
                patrones = Patron.leer(r);
            }
        }
        return patrones;
    }

    public List<Medida> medidas() {
        synchronized (medidas) {
            return new ArrayList<>(medidas);
        }
    }

    public static String ahoraIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
    }

    private static String sello() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private static File nuevo(Context ctx, String prefijo, String ext) {
        File dir = new File(ctx.getFilesDir(), "registros");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        String s = sello();
        File f = new File(dir, prefijo + "_" + s + ext);
        for (int i = 2; f.exists(); i++) {
            f = new File(dir, prefijo + "_" + s + "_" + i + ext);
        }
        return f;
    }

    /** Guarda la medida en memoria y la anade al CSV de la sesion en el acto. */
    public synchronized void agregarMedida(Context ctx, Medida m) throws IOException {
        medidas.add(m);
        if (csvMedidas == null) {
            csvMedidas = nuevo(ctx, "medidas_" + limpio(serie()), ".csv");
            anexar(csvMedidas, Medida.cabecera());
        }
        anexar(csvMedidas, m.aCsv());
    }

    public synchronized File csvMedidas() {
        return csvMedidas;
    }

    /** Pareja boton -> trama STONE, en su propio CSV. */
    public synchronized void agregarBoton(Context ctx, String boton, String tramaHex, String codigo)
            throws IOException {
        if (csvBotones == null) {
            csvBotones = nuevo(ctx, "botones_" + limpio(serie()), ".csv");
            anexar(csvBotones, "fecha_hora,serie,mac,firmware,boton,trama_hex,codigo_byte8");
        }
        anexar(csvBotones, String.join(",", ahoraIso(), Medida.csv(serie()), Medida.csv(mac),
                Medida.csv(firmware()), Medida.csv(boton), Medida.csv(tramaHex), Medida.csv(codigo)));
    }

    public synchronized File csvBotones() {
        return csvBotones;
    }

    /**
     * Copia de los coeficientes leidos (RF-APP-22): un fichero nuevo en cada
     * copia, nunca se pisa el anterior.
     */
    public synchronized void guardarCoeficientes(Context ctx, String motivo) throws IOException {
        File f = nuevo(ctx, "coeficientes_" + limpio(serie()), ".csv");
        anexar(f, "fecha_hora,serie,mac,firmware,motivo,codigo,c3,c2,c1,c0");
        String pre = String.join(",", ahoraIso(), Medida.csv(serie()), Medida.csv(mac),
                Medida.csv(firmware()), Medida.csv(motivo));
        for (int i = 0; i < leidas.length; i++) {
            Ecuacion e = leidas[i];
            if (e != null) {
                anexar(f, pre + "," + Fabrica.CODIGOS[i] + "," + Tramas.coeficiente(e.c3) + ","
                        + Tramas.coeficiente(e.c2) + "," + Tramas.coeficiente(e.c1) + ","
                        + Tramas.coeficiente(e.c0));
            }
        }
        double[] t = temperatura;
        if (t != null) {
            anexar(f, pre + ",T,," + Tramas.coeficiente(t[0]) + "," + Tramas.coeficiente(t[1]) + ","
                    + Tramas.coeficiente(t[2]));
        }
        csvCoeficientes = f;
    }

    /** Fila de la linea base previa a grabar (G3), en su propio CSV. */
    public synchronized void agregarLineaBase(Context ctx, String fila) throws IOException {
        if (csvLineaBase == null) {
            csvLineaBase = nuevo(ctx, "lineabase_" + limpio(serie()), ".csv");
            anexar(csvLineaBase, LineaBase.cabecera());
        }
        anexar(csvLineaBase, fila);
    }

    public synchronized File csvLineaBase() {
        return csvLineaBase;
    }

    public synchronized File csvCoeficientes() {
        return csvCoeficientes;
    }

    private static String limpio(String s) {
        String r = s.replaceAll("[^A-Za-z0-9-]", "");
        return r.isEmpty() ? "equipo" : r;
    }

    private static void anexar(File f, String linea) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8)) {
            w.write(linea);
            w.write("\n");
        }
    }
}
