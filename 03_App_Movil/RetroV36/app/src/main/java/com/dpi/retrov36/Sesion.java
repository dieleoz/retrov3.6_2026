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

    private static final Sesion INSTANCIA = new Sesion();

    public static Sesion get() {
        return INSTANCIA;
    }

    /**
     * Protocolo del firmware detectado (RTV 1.0, RF-APP-U06); null si no se ha detectado o no se reconocio.
     * Las pantallas preguntan al protocolo que puede hacer, no por la version.
     */
    public volatile Protocolo protocolo;
    /** true tras una deteccion, aunque no se reconociera el firmware. */
    public volatile boolean detectado;
    /** Diario del estado oculto del V4 original (RF-APP-U08). */
    public final DiarioEstadoOculto diario = new DiarioEstadoOculto();
    /** Serie declarada por el perfil de equipos.csv ("" si no hay). */
    public volatile String serieDeclaradaPerfil = "";
    public volatile String fechaFirmware = "";
    /** "CAL" o "DEF" en V3.6; vacio en otro caso. */
    public volatile String marca = "";
    /** Variante de la V3.6 segun #GC# (solo se pregunta tras identificar una V3.6). */
    public volatile Calibracion.Variante variante = Calibracion.Variante.DESCONOCIDA;
    /** Serie grabada en el equipo (#GN#): "NONE" si no hay; null si no se leyo o el firmware no la tiene. */
    public volatile String serieEquipo;
    /** Fecha de calibracion (#GC#): AAAA-MM-DD o "NONE"; null si no se leyo o el firmware no la tiene. */
    public volatile String fechaCalibracion;
    /** Mascara de #V# (revision 1.1); -1 si no la trae. */
    public volatile int mascara = -1;
    /** Ultima trama '#' respondida (elapsedRealtime), para la caducidad del modo admin. */
    public volatile long ultimaAlmohadillaMs;
    /** Fallos de PIN seguidos en esta conexion (el firmware bloquea #L a los 5). */
    public volatile int fallosPin;
    /**
     * Disparos de asentamiento que se hacen y se DESCARTAN antes de cada serie
     * (Medir xN y la 'e' inicial de la prueba 3). Medido en SLV-002 el
     * 19-sep-2026: en 17 de 17 series de 9 disparos con 'e' el primero sale
     * bajo, 6-39 cuentas (17 de media); sin el, s ~ 5. 1 por defecto, 0 lo
     * desactiva. No se reinicia al reconectar.
     */
    public volatile int disparosAsentamiento = 1;
    public volatile String nombre = "";
    public volatile String mac = "";

    /** Coeficientes leidos con #G (V3.6); null si no leidos. */
    public final Ecuacion[] leidas = new Ecuacion[Fabrica.CODIGOS.length];
    public volatile double[] temperatura;
    /**
     * Acta de la calibracion en curso (3.6.8); null si no se ha escrito nada. Mientras
     * exista y no este cerrada, el protocolo de disparos esta fijo (P9-B3).
     */
    public volatile Acta acta;

    public boolean calibrando() {
        Acta a = acta;
        return a != null && !a.cerrada();
    }

    /** De donde salen los puntos del ajuste (P9-B7). */
    public String origenAjuste() {
        Campana c = Campanas.abierta();
        if (c != null && c.esDeEsteEquipo(mac) && !c.medidasElegidas().isEmpty()) {
            return "series elegidas de la campaña de " + c.equipo + " (" + c.mac + ")";
        }
        return "ATENCIÓN: medidas sueltas de esta sesión, no de la campaña (P9-B7)";
    }

    /** null: pruebas no hechas; true/false: APTO / NO APTO. */
    public volatile Boolean apto;
    public volatile String resumenPruebas = "";
    public volatile boolean overrideAdmin;
    /** El operador acepto medir con NO APTO en esta conexion. */
    public volatile boolean medirNoAptoAceptado;
    public volatile boolean adminDesbloqueado;
    /** PIN del equipo, una vez por conexion (QA-3612-14). Solo en memoria. */
    public volatile String pinAdmin;

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
        serieManual = "";
        protocolo = null;
        detectado = false;
        diario.reiniciar();
        serieDeclaradaPerfil = "";
        fechaFirmware = "";
        marca = "";
        mascara = -1;
        variante = Calibracion.Variante.DESCONOCIDA;
        serieEquipo = null;
        fechaCalibracion = null;
        ultimaAlmohadillaMs = 0;
        fallosPin = 0;
        for (int i = 0; i < leidas.length; i++) {
            leidas[i] = null;
        }
        temperatura = null;
        acta = null;
        apto = null;
        resumenPruebas = "";
        overrideAdmin = false;
        medirNoAptoAceptado = false;
        adminDesbloqueado = false;
        pinAdmin = null;
        medidas.clear();
        csvMedidas = null;
        csvBotones = null;
        csvCoeficientes = null;
        csvLineaBase = null;
    }

    /** "SLV-002" de "COVIANDINA_SLV-002": lo que sigue al ultimo '_'. */
    /** Serie tecleada por el operador cuando el nombre SPP no la trae; vacia si no. */
    public volatile String serieManual = "";

    /** RTV 1.0 (RF-APP-U11, U13): con la V4.6 la serie es la de #GN#, nunca tecleada; en blanco, no hay serie. */
    private boolean serieDeV46() {
        Protocolo p = protocolo;
        return p != null && p.firmware() == Protocolo.Firmware.F46;
    }

    /** true si se sabe la serie (#GN# de la V4.6; si no, del nombre "..._<serie>", tecleada o de equipos.csv). */
    public boolean serieConocida() {
        if (serieDeV46()) {
            return serieEquipo != null && !Calibracion.NONE.equals(serieEquipo);
        }
        if (!serieManual.isEmpty() || !serieDeclaradaPerfil.isEmpty()) {
            return true;
        }
        String n = nombre == null ? "" : nombre;
        int i = n.lastIndexOf('_');
        return i >= 0 && i < n.length() - 1;
    }

    public String serie() {
        if (serieDeV46()) {
            return serieConocida() ? serieEquipo : "";
        }
        if (!serieManual.isEmpty()) {
            return serieManual;
        }
        if (!serieDeclaradaPerfil.isEmpty()) {
            return serieDeclaradaPerfil;
        }
        String n = nombre == null ? "" : nombre;
        int i = n.lastIndexOf('_');
        return (i >= 0 && i < n.length() - 1) ? n.substring(i + 1) : n;
    }

    public boolean sinDetectar() {
        return protocolo == null && !detectado;
    }

    /** Nombre del firmware, o "sin detectar" / "desconocido". */
    public String versionTexto() {
        Protocolo p = protocolo;
        return p != null ? p.nombre() : detectado ? "desconocido" : "sin detectar";
    }

    public String firmware() {
        Protocolo p = protocolo;
        if (p == null) {
            return versionTexto();
        }
        switch (p.firmware()) {
            case F36:
                return "V3.6 " + fechaFirmware + (variante == Calibracion.Variante.V362 ? " (3.6.2)"
                        : (limitesS() ? " (3.6.1)" : " (3.6.0, sin límites de #S)")) + " " + marca
                        + (mascara >= 0 ? String.format(Locale.US, " mascara %04X", mascara) : "");
            case F46:
                return "V4.6 " + fechaFirmware + " " + marca
                        + (mascara >= 0 ? String.format(Locale.US, " mascara %04X", mascara) : "");
            default:
                return p.nombre();
        }
    }

    /** true si habla el contrato "#...#" (V3.6 o V4.6). */
    public boolean administra() {
        Protocolo p = protocolo;
        return p != null && p.administra();
    }

    /** true si el equipo tiene serie y fecha en EEPROM (#GN#, #GC#): V3.6.2 y V4.6. */
    public boolean es362() {
        Protocolo p = protocolo;
        return p != null && p.administra()
                && (p.firmware() == Protocolo.Firmware.F46 || variante == Calibracion.Variante.V362);
    }

    /** RF-APP-U11: marca de la serie cuando no sale de #GN#. */
    public String marcaSerie() {
        return Deteccion.marcaSerie(protocolo, variante, serieEquipo);
    }

    /** Serie del equipo, fecha de calibracion, vencimiento y estado (3.6.2). */
    public String datosCalibracion() {
        if (!administra()) {
            return "Serie y fecha de calibración: no disponibles en " + versionTexto();
        }
        if (!es362()) {
            return "Serie y fecha de calibración: el firmware no es la 3.6.2 (no tiene #GN#/#GC#)";
        }
        String f = fechaCalibracion;
        String venc = (f != null && Calibracion.fechaValida(f)) ? Calibracion.vencimiento(f) : "-";
        return "Serie en el equipo (#GN#): " + (serieEquipo == null ? "?" : serieEquipo)
                + "; fecha de calibración (#GC#): " + (f == null ? "?" : f)
                + "; vencimiento: " + venc
                + "; estado: " + Calibracion.estado(f, marca, Calibracion.hoy());
    }

    /** Identidad para cabeceras y exportaciones. */
    public String identidad() {
        return "equipo " + nombre + " (serie " + serie() + marcaSerie() + ", MAC " + mac + "), firmware " + firmware();
    }

    /**
     * true si el firmware es la V3.6.1 o posterior (fecha de compilacion
     * 2026-09-19 o mayor), que rechaza en #S curvas fuera de [0; 4000] en
     * x = 600-4300. La del 2026-09-18 es la V3.6 sin esos limites.
     */
    public boolean limitesS() {
        return fechaFirmware != null && fechaFirmware.compareTo("2026-09-19") >= 0;
    }

    /** true si se puede medir la x de los patrones (hay x, directa o por inversion). */
    public boolean versionMedible() {
        Protocolo p = protocolo;
        return p != null && p.daX();
    }

    /** Ecuacion con la que responde el equipo al codigo k. */
    public Ecuacion ecuacionVigente(char k) {
        int i = Fabrica.indice(k);
        if (administra() && i >= 0 && leidas[i] != null) {
            return leidas[i];
        }
        return Fabrica.ecuacion(k);
    }

    public synchronized List<Patron> patrones(Context ctx) throws IOException {
        if (patrones == null) {
            try (InputStreamReader r = new InputStreamReader(
                    ctx.getAssets().open("patrones_certificados_P1-P132.csv"), StandardCharsets.UTF_8)) {
                patrones = Patron.leer(r);
            }
        }
        return patrones;
    }

    /**
     * Medidas para el asistente de ajuste: las series elegidas de la campana
     * abierta si es de ESTE equipo y tiene alguna; si no, las de la sesion.
     * Nunca las de otro equipo.
     */
    public List<Medida> medidasParaAjuste() {
        Campana c = Campanas.abierta();
        if (c != null && c.esDeEsteEquipo(mac)) {
            List<Medida> l = c.medidasElegidas();
            if (!l.isEmpty()) {
                return l;
            }
        }
        return medidas();
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
