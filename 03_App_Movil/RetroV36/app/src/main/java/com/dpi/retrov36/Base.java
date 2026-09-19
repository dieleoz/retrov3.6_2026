package com.dpi.retrov36;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base de las pantallas: se construyen por codigo (sin layouts XML) con unos
 * pocos ayudantes, y todas vigilan el estado del enlace.
 */
public abstract class Base extends AppCompatActivity implements EnlaceSerie.OyenteEstado {

    public static final int VERDE = Color.rgb(0xC8, 0xE6, 0xC9);
    public static final int ROJO = Color.rgb(0xFF, 0xCD, 0xD2);
    public static final int AMARILLO = Color.rgb(0xFF, 0xF3, 0xC4);
    public static final int GRIS = Color.rgb(0xEE, 0xEE, 0xEE);
    public static final int AZUL = Color.rgb(0xBB, 0xDE, 0xFB);

    protected LinearLayout raiz;
    protected TextView txtEnlace;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        raiz.setPadding(p, p, p, p);
        sv.addView(raiz);
        setContentView(sv);
        // RF-APP-41: la version, en grande, en la cabecera de todas las pantallas.
        TextView ver = texto("RTV V" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        ver.setTextSize(20);
        ver.setTypeface(Typeface.DEFAULT_BOLD);
        txtEnlace = texto("");
        txtEnlace.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EnlaceSerie.instancia().agregarOyenteEstado(this);
        pintarEnlace(EnlaceSerie.instancia().estaConectado(), EnlaceSerie.instancia().getTextoEstado());
    }

    @Override
    protected void onPause() {
        super.onPause();
        EnlaceSerie.instancia().quitarOyenteEstado(this);
    }

    @Override
    public void estado(boolean conectado, String texto) {
        if (!conectado) {
            Sesion.get().adminDesbloqueado = false;
        }
        pintarEnlace(conectado, texto);
        refrescar();
    }

    private void pintarEnlace(boolean conectado, String texto) {
        Sesion s = Sesion.get();
        String t = texto;
        if (conectado) {
            t += "\nFirmware: " + s.firmware() + "\nPruebas: "
                    + (s.apto == null ? "sin hacer" : (s.apto ? "APTO" : "NO APTO"));
        }
        txtEnlace.setText(t);
        txtEnlace.setBackgroundColor(!conectado ? ROJO
                : (s.apto == null ? AMARILLO : (s.apto ? VERDE : ROJO)));
    }

    /** Vuelve a pintar la pantalla; se llama al cambiar el enlace. */
    protected void refrescar() {
        pintarEnlace(EnlaceSerie.instancia().estaConectado(), EnlaceSerie.instancia().getTextoEstado());
    }

    protected int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    protected TextView texto(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(15);
        v.setPadding(dp(6), dp(6), dp(6), dp(6));
        raiz.addView(v);
        return v;
    }

    protected TextView titulo(String t) {
        TextView v = texto(t);
        v.setTextSize(18);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(dp(6), dp(14), dp(6), dp(4));
        return v;
    }

    protected Button boton(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        raiz.addView(b);
        return b;
    }

    protected EditText campo(String pista, int tipo) {
        EditText e = new EditText(this);
        e.setHint(pista);
        e.setInputType(tipo);
        raiz.addView(e);
        return e;
    }

    protected EditText campoPin() {
        EditText e = new EditText(this);
        e.setHint("PIN (4 dígitos)");
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return e;
    }

    /** Mueve las vistas dadas a una fila horizontal de igual anchura. */
    protected LinearLayout fila(View... vistas) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : vistas) {
            ((ViewGroup) v.getParent()).removeView(v);
            f.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        raiz.addView(f);
        return f;
    }

    /** Texto de un asset del APK (UTF-8), o null. */
    protected String asset(String n) {
        try (java.io.InputStream in = getAssets().open(n)) {
            return new String(ImportadorCampana.leer(in), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    protected Decisiones decisionesApk() {
        return Decisiones.leer(asset(Decisiones.ASSET));
    }

    /** Patrones de TIPO-I-REPETIR para el equipo (vacio sin decision). */
    protected static List<String> repetirApk(Decisiones d, String equipo) {
        Decisiones.Decision r = d.decision("TIPO-I-REPETIR", equipo);
        return r == null ? new ArrayList<String>() : r.repetidos;
    }

    protected BancoPrevio.Grupos gruposApk() {
        return BancoPrevio.Grupos.leer(asset("grupos_patrones_equivalentes.csv"));
    }

    /** La cola de un tipo de banco, o null si no viaja en este APK o su md5 no esta admitido. */
    protected BancoCola colaApk(BancoCola.Tipo t) {
        try (java.io.InputStream in = getAssets().open(t.asset)) {
            return BancoCola.cargar(ImportadorCampana.leer(in));
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    /** Banco mas corto (3.6.16): lo ya medido cuenta y lo que Diego manda repetir vuelve a la cola. */
    protected String aplicarBancoPrevio(Campana c) {
        if (c == null) {
            return "";
        }
        BancoCola q = colaApk(BancoCola.Tipo.de(c.colaTipo()));
        if (q == null) {
            return "";
        }
        try {
            String eq = TablaCalibracion.canonico(c.historialSeries(), c.mac);
            Decisiones d = decisionesApk();
            BancoPrevio.Resultado r = BancoPrevio.aplicar(c, q, gruposApk(), d, eq, Sesion.ahoraIso());
            return (r.hechos + r.repetir == 0 ? "" : r.hechos + " pasos ya medidos cuentan como hechos; " + r.repetir
                    + " a repetir en preciso. ") + BancoPrevio.textoPendiente(q, c.pasos(), true,
                    d.valor("PROTOCOLO-AJUSTE", eq), repetirApk(d, eq)) + ".";
        } catch (java.io.IOException | RuntimeException e) {
            return "No se pudo revisar lo ya medido: " + e.getMessage();
        }
    }

    protected void enUi(Runnable r) {
        runOnUiThread(r);
    }

    /**
     * Desde el hilo de trabajo: muestra un dialogo y espera la respuesta. Devuelve 0 (positivo),
     * 1 (negativo) o 2 (neutro). negativo y neutro pueden ser null.
     */
    protected int preguntar(String titulo, String mensaje, String positivo, String negativo, String neutro)
            throws InterruptedException {
        final int[] r = {-1};
        if (isFinishing() || isDestroyed()) {
            return -1;   // QA-3612-11: sin pantalla no se espera una respuesta que no llegara
        }
        final java.util.concurrent.CountDownLatch l = new java.util.concurrent.CountDownLatch(1);
        synchronized (esperas) {
            esperas.add(l);
        }
        enUi(() -> {
            if (isFinishing() || isDestroyed()) {
                l.countDown();
                return;
            }
            AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle(titulo).setMessage(mensaje).setCancelable(false)
                    .setPositiveButton(positivo, (d, w) -> { r[0] = 0; l.countDown(); });
            if (negativo != null) {
                b.setNegativeButton(negativo, (d, w) -> { r[0] = 1; l.countDown(); });
            }
            if (neutro != null) {
                b.setNeutralButton(neutro, (d, w) -> { r[0] = 2; l.countDown(); });
            }
            b.show();
        });
        l.await();
        synchronized (esperas) {
            esperas.remove(l);
        }
        return r[0];
    }

    private final java.util.List<java.util.concurrent.CountDownLatch> esperas = new java.util.ArrayList<>();

    @Override
    protected void onDestroy() {
        // QA-3612-11: si la pantalla muere con un dialogo abierto, el hilo de trabajo recibe -1 y para.
        synchronized (esperas) {
            for (java.util.concurrent.CountDownLatch l : esperas) {
                l.countDown();
            }
            esperas.clear();
        }
        super.onDestroy();
    }

    protected void aviso(String t) {
        Toast.makeText(this, t, Toast.LENGTH_LONG).show();
    }

    protected void alerta(String titulo, String mensaje) {
        if (isFinishing() || isDestroyed()) {
            // QA-3610-04: nunca un dialogo sobre una actividad destruida.
            Registro.nota("aviso sin pantalla (" + titulo + "): " + mensaje);
            return;
        }
        new AlertDialog.Builder(this).setTitle(titulo).setMessage(mensaje)
                .setPositiveButton("Entendido", null).show();
    }

    protected void pantallaEncendida(boolean si) {
        if (si) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /** Comparte el ZIP de una campana exportada (un solo envio) con sus huellas. */
    protected void compartirZip(Campanas.Exportacion ex, String texto) {
        Uri u;
        try {
            u = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".ficheros", ex.zip);
        } catch (IllegalArgumentException e) {
            aviso("No se pudo compartir el ZIP: " + e.getMessage());
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_STREAM, u);
        i.putExtra(Intent.EXTRA_SUBJECT, ex.zip.getName());
        i.putExtra(Intent.EXTRA_TEXT, texto + "\n" + ex.zip.getName() + "\nmd5 " + ex.md5 + "\nsha256 " + ex.sha256
                + "\nCopia: " + ex.copia);
        i.setClipData(ClipData.newRawUri(ex.zip.getName(), u));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Enviar el ZIP"));
        } catch (ActivityNotFoundException e) {
            aviso("No hay ninguna aplicación para compartir el ZIP.");
        }
    }

    /** Los dos ZIP (ligero y de soporte) en un solo selector (ACTION_SEND_MULTIPLE). */
    /** 3.6.17: los ZIP y el informe de calibracion en un solo selector. */
    protected void compartirFicheros(List<File> fs, String texto) {
        if (fs.isEmpty()) {
            return;
        }
        ArrayList<Uri> us = new ArrayList<>();
        try {
            for (File f : fs) {
                us.add(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".ficheros", f));
            }
        } catch (IllegalArgumentException e) {
            aviso("No se pudo compartir: " + e.getMessage());
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("*/*");
        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, us);
        i.putExtra(Intent.EXTRA_SUBJECT, fs.get(0).getName());
        i.putExtra(Intent.EXTRA_TEXT, texto);
        ClipData cd = ClipData.newRawUri(fs.get(0).getName(), us.get(0));
        for (int k = 1; k < us.size(); k++) {
            cd.addItem(new ClipData.Item(us.get(k)));
        }
        i.setClipData(cd);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Guardar / compartir"));
        } catch (ActivityNotFoundException e) {
            aviso("No hay ninguna aplicación para compartir.");
        }
    }

    protected void compartirZips(List<Campanas.Exportacion> todos, String texto) {
        if (todos.isEmpty()) {
            return;
        }
        ArrayList<Uri> us = new ArrayList<>();
        StringBuilder t = new StringBuilder(texto);
        try {
            for (Campanas.Exportacion ex : todos) {
                us.add(FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".ficheros", ex.zip));
                t.append("\n").append(ex.zip.getName()).append("\nmd5 ").append(ex.md5).append("\nsha256 ")
                        .append(ex.sha256).append("\nCopia: ").append(ex.copia);
            }
        } catch (IllegalArgumentException e) {
            aviso("No se pudo compartir el ZIP: " + e.getMessage());
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("application/zip");
        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, us);
        i.putExtra(Intent.EXTRA_SUBJECT, todos.get(0).zip.getName());
        i.putExtra(Intent.EXTRA_TEXT, t.toString());
        ClipData cd = ClipData.newRawUri(todos.get(0).zip.getName(), us.get(0));
        for (int k = 1; k < us.size(); k++) {
            cd.addItem(new ClipData.Item(us.get(k)));
        }
        i.setClipData(cd);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Enviar la campaña (ZIP ligero y de soporte)"));
        } catch (ActivityNotFoundException e) {
            aviso("No hay ninguna aplicación para compartir el ZIP.");
        }
    }

    /** Comparte registro de tramas y CSV de la sesion por ACTION_SEND_MULTIPLE. */
    protected void compartirTodo() {
        List<File> f = new ArrayList<>();
        File r = Registro.ficheroActual();
        if (r != null && r.exists()) {
            f.add(r);
        }
        Sesion s = Sesion.get();
        if (s.csvMedidas() != null) {
            f.add(s.csvMedidas());
        }
        if (s.csvBotones() != null) {
            f.add(s.csvBotones());
        }
        if (s.csvCoeficientes() != null) {
            f.add(s.csvCoeficientes());
        }
        if (s.csvLineaBase() != null) {
            f.add(s.csvLineaBase());
        }
        if (f.isEmpty()) {
            aviso("Aún no hay nada que compartir: el registro se crea al conectar.");
            return;
        }
        ArrayList<Uri> uris = new ArrayList<>();
        ClipData clip = null;
        try {
            for (File x : f) {
                Uri u = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".ficheros", x);
                uris.add(u);
                if (clip == null) {
                    clip = ClipData.newRawUri(x.getName(), u);
                } else {
                    clip.addItem(new ClipData.Item(u));
                }
            }
        } catch (IllegalArgumentException e) {
            aviso("No se pudieron preparar los ficheros: " + e.getMessage());
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("text/*");
        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        i.putExtra(Intent.EXTRA_SUBJECT, "RTV " + BuildConfig.VERSION_NAME + " - " + s.identidad());
        i.putExtra(Intent.EXTRA_TEXT, s.identidad() + "\nPruebas: " + s.resumenPruebas);
        i.setClipData(clip);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Compartir registro y datos"));
        } catch (ActivityNotFoundException e) {
            aviso("No hay ninguna aplicación para compartir ficheros.");
        }
    }
}
