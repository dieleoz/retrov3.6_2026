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

    protected void enUi(Runnable r) {
        runOnUiThread(r);
    }

    protected void aviso(String t) {
        Toast.makeText(this, t, Toast.LENGTH_LONG).show();
    }

    protected void alerta(String titulo, String mensaje) {
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
            startActivity(Intent.createChooser(i, "Enviar la campaña (un solo ZIP)"));
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
        i.putExtra(Intent.EXTRA_SUBJECT, "RTV V3.6 - " + s.identidad());
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
