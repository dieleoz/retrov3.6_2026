package com.dpi.retrov36;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Campana de calibracion (3.6.5): modo guiado patron a patron y lista.
 * Todo se guarda al medir (diario de la campana del equipo conectado) y sale
 * en UN solo ZIP con "Exportar campana". Nada se comparte por patron.
 */
public class CampanaActivity extends Base {

    private static final int PIDE_CSV = 301;
    /** Patrones que el operador salto en esta sesion (van al final de la cola). */
    private static final List<String> SALTADOS = new ArrayList<>();

    private Campana campana;
    private TextView txtAvance;
    private TextView txtColoque;
    private TextView txtResultado;
    private EditText edN;
    private EditText edTol;
    private Button btnOk;
    private Button btnSaltar;
    private Spinner spColor;
    private Spinner spTipo;
    private LinearLayout lista;
    private Cola.Paso paso;
    private volatile boolean midiendo;
    private Button btnCerrar;
    private Button btnImportar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        txtAvance = texto("");
        txtAvance.setTypeface(Typeface.DEFAULT_BOLD);
        edN = campo("Disparos por patrón (N)", InputType.TYPE_CLASS_NUMBER);
        edN.setText("9");
        edTol = campo("Tolerancia de orden por certificado (%)", InputType.TYPE_CLASS_NUMBER);
        edTol.setText("3");
        fila(edN, edTol);

        titulo("Modo guiado");
        txtColoque = texto("");
        txtColoque.setTextSize(24);
        txtColoque.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk = boton("OK, medir", v -> medirPaso());
        btnSaltar = boton("Saltar (lo dejo para el final)", v -> saltar());
        fila(btnOk, btnSaltar);
        txtResultado = texto("");
        txtResultado.setTypeface(Typeface.MONOSPACE);

        titulo("Enviar");
        boton("Exportar campaña (un solo ZIP con todo)", v -> exportar());
        texto("El ZIP lleva las series, las pruebas del equipo, todos los registros de tramas de la campaña y "
                + "el resumen. Después de exportar no hace falta compartir nada más.");
        btnCerrar = boton("Cerrar campaña (queda de solo lectura)", v -> cerrarCampana());
        Button imp = boton("Importar CSV de medidas antiguas", v -> importar());
        Button nueva = boton("Nueva campaña (archiva la actual)", v -> nueva());
        fila(imp, nueva);
        btnImportar = imp;

        titulo("Lista de patrones");
        spColor = new Spinner(this);
        spTipo = new Spinner(this);
        raiz.addView(spColor);
        raiz.addView(spTipo);
        fila(spColor, spTipo);
        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(lista);
        abrir();
    }

    // ------------------------------------------------------------- apertura

    private void abrir() {
        Sesion s = Sesion.get();
        if (s.mac == null || s.mac.isEmpty()) {
            txtColoque.setText("Conecte primero con el equipo: la campaña es de un equipo concreto (serie y MAC).");
            btnOk.setEnabled(false);
            btnSaltar.setEnabled(false);
            return;
        }
        if (!s.serieConocida()) {
            final EditText e = new EditText(this);
            e.setHint("Serie del equipo, p. ej. SLV-003");
            new AlertDialog.Builder(this).setTitle("Serie del equipo")
                    .setMessage("El nombre Bluetooth (" + s.nombre + ") no trae la serie. Escríbala: la campaña y "
                            + "el ajuste son de este equipo y no se mezclan con otros.")
                    .setView(e).setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> {
                        String t = e.getText().toString().trim();
                        if (t.isEmpty()) {
                            finish();
                            return;
                        }
                        s.serieManual = t;
                        Registro.nota("serie tecleada por el operador: " + t);
                        abrir();
                    })
                    .setNegativeButton("Cancelar", (d, w) -> finish()).show();
            return;
        }
        try {
            campana = Campanas.abrir(this, s.serie(), s.mac);
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo abrir la campaña: " + e.getMessage());
            return;
        }
        if (Campanas.lineasMalas() > 0) {
            aviso(Campanas.lineasMalas() + " líneas del diario no se entendieron (quedan en el fichero).");
        }
        prepararFiltros();
        pintar();
    }

    private void prepararFiltros() {
        List<String> colores = new ArrayList<>();
        colores.add("todos los colores");
        List<String> tipos = new ArrayList<>();
        tipos.add("todos los tipos");
        for (Patron p : campana.catalogo()) {
            if (!colores.contains(p.color)) {
                colores.add(p.color);
            }
            if (!tipos.contains(p.tipo)) {
                tipos.add(p.tipo);
            }
        }
        spColor.setAdapter(adaptador(colores));
        spTipo.setAdapter(adaptador(tipos));
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, android.view.View v, int i, long id) {
                pintarLista();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) { }
        };
        spColor.setOnItemSelectedListener(l);
        spTipo.setOnItemSelectedListener(l);
    }

    private ArrayAdapter<String> adaptador(List<String> l) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, l);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private double tol() {
        try {
            double t = Double.parseDouble(edTol.getText().toString().trim()) / 100.0;
            return (t > 0 && t < 0.5) ? t : Veredicto.TOL_ORDEN;
        } catch (NumberFormatException e) {
            return Veredicto.TOL_ORDEN;
        }
    }

    // --------------------------------------------------------------- pintar

    private void pintar() {
        if (campana == null) {
            return;
        }
        txtAvance.setText("Equipo " + campana.equipo + " (" + campana.mac + ")\nAvance: " + campana.avance());
        List<Cola.Paso> cola = Cola.construir(campana, tol(), SALTADOS);
        paso = cola.isEmpty() ? null : cola.get(0);
        if (paso == null) {
            txtColoque.setText("Campaña completa: no queda nada en la cola. Pulse \"Exportar campaña\".");
        } else {
            Patron p = campana.patron(paso.patron);
            txtColoque.setText(String.format(Locale.US, "Coloque %s — %s %s — cert. %s — orientación %d°",
                    p.nombre, p.color, p.tipo, Campana.fmt(p.valor), paso.orientacion)
                    + "\n(" + paso.motivo + "; quedan " + cola.size() + ")");
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        boolean abiertaC = !campana.cerrada();
        btnOk.setEnabled(abiertaC && paso != null && !midiendo && con);
        btnSaltar.setEnabled(abiertaC && paso != null && !midiendo);
        btnCerrar.setEnabled(abiertaC && !midiendo);
        btnImportar.setEnabled(abiertaC && !midiendo);
        if (!abiertaC) {
            txtColoque.setText("Campaña CERRADA: de solo lectura. Puede exportarla; para medir, \"Nueva campaña\".");
        }
        pintarLista();
    }

    private void pintarLista() {
        if (campana == null || lista == null) {
            return;
        }
        lista.removeAllViews();
        String fc = spColor.getSelectedItemPosition() > 0 ? (String) spColor.getSelectedItem() : null;
        String ft = spTipo.getSelectedItemPosition() > 0 ? (String) spTipo.getSelectedItem() : null;
        Map<String, String> prio = campana.prioritarios(tol());
        for (final Patron p : campana.catalogo()) {
            if ((fc != null && !fc.equals(p.color)) || (ft != null && !ft.equals(p.tipo))) {
                continue;
            }
            Campana.Estado e = campana.estado(p.nombre);
            Campana.Serie el = campana.elegida(p.nombre);
            String t = p.nombre + "  " + Campana.fmt(p.valor) + "  " + p.tipo + "  " + p.color + " — "
                    + (e == Campana.Estado.MEDIDO ? String.format(Locale.US, "medido: %s, x %.0f, s %.1f, n %d",
                    el.id, el.media(), el.desviacion(), el.validos().length)
                    : (e == Campana.Estado.REPETIR ? "repetir" : "por hacer"))
                    + " (" + campana.seriesDe(p.nombre).size() + " series)"
                    + (prio.containsKey(p.nombre) ? "\nIMPRESCINDIBLE: " + prio.get(p.nombre) : "");
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(t);
            b.setBackgroundColor(prio.containsKey(p.nombre) ? ROJO
                    : (e == Campana.Estado.MEDIDO ? VERDE : (e == Campana.Estado.REPETIR ? AMARILLO : GRIS)));
            b.setOnClickListener(v -> opcionesPatron(p));
            lista.addView(b);
        }
    }

    private void opcionesPatron(Patron p) {
        String[] op = {"Medir a 0°", "Medir a 90°", "Series (elegir la que entra en el ajuste)"};
        new AlertDialog.Builder(this).setTitle(p.toString())
                .setItems(op, (d, w) -> {
                    if (w == 0 || w == 1) {
                        medir(p, w == 0 ? 0 : 90);
                    } else {
                        series(p);
                    }
                }).show();
    }

    private void series(Patron p) {
        final List<Campana.Serie> ls = campana.seriesDe(p.nombre);
        if (ls.isEmpty()) {
            aviso("Sin series de " + p.nombre + ".");
            return;
        }
        Campana.Serie el = campana.elegida(p.nombre);
        String[] t = new String[ls.size()];
        for (int i = 0; i < ls.size(); i++) {
            Campana.Serie s = ls.get(i);
            t[i] = String.format(Locale.US, "%s%s %s: x %.0f s %.1f n %d, %s%s%s", s == el ? "[ELEGIDA] " : "", s.id,
                    s.orientacion < 0 ? "?" : s.orientacion + "°", s.media(), s.desviacion(), s.validos().length,
                    s.veredicto, s.aceptada ? ", aceptada" : ", no aceptada",
                    s.patronOriginal.equals(s.patron) ? "" : " (medida como " + s.patronOriginal + ")");
        }
        new AlertDialog.Builder(this).setTitle("Series de " + p.nombre + " (toque una aceptada para elegirla)")
                .setItems(t, (d, w) -> {
                    Campana.Serie s = ls.get(w);
                    if (campana.cerrada()) {
                        aviso("La campaña está cerrada (solo lectura).");
                        return;
                    }
                    if (!s.aceptada) {
                        aviso("Sólo se puede elegir una serie aceptada.");
                        return;
                    }
                    try {
                        campana.elegir(s);
                        Registro.nota("campana: " + s.id + " elegida para " + p.nombre);
                    } catch (IOException e) {
                        alerta("Campaña", "No se pudo guardar: " + e.getMessage());
                    }
                    pintar();
                }).show();
    }

    // ---------------------------------------------------------------- guiado

    private void saltar() {
        if (paso != null) {
            SALTADOS.remove(paso.patron + "@" + paso.orientacion);
            SALTADOS.add(paso.patron + "@" + paso.orientacion);
            Registro.nota("campana: " + paso.patron + " saltado por el operador");
            pintar();
        }
    }

    private void medirPaso() {
        if (paso != null) {
            medir(campana.patron(paso.patron), paso.orientacion);
        }
    }

    private void medir(final Patron p, final int orientacion) {
        final Sesion s = Sesion.get();
        if (campana != null && campana.cerrada()) {
            alerta("Campaña cerrada", "La campaña está cerrada (solo lectura).");
            return;
        }
        if (!EnlaceSerie.instancia().estaConectado() || !s.versionMedible()) {
            alerta("No se puede medir", "Conecte con el equipo y pase las pruebas (firmware: " + s.firmware() + ").");
            return;
        }
        if (campana == null || !campana.esDeEsteEquipo(s.mac)) {
            alerta("Otro equipo", "La campaña abierta es de otro equipo. Vuelva a abrir esta pantalla.");
            return;
        }
        if (s.apto == null) {
            alerta("Pruebas pendientes", "Antes de medir hay que pasar las pruebas del equipo.");
            return;
        }
        if (!s.apto && !s.medirNoAptoAceptado) {
            new AlertDialog.Builder(this).setTitle("Equipo NO APTO")
                    .setMessage(s.resumenPruebas + "\n\n¿Medir igualmente?")
                    .setPositiveButton("Medir igualmente", (d, w) -> {
                        s.medirNoAptoAceptado = true;
                        Registro.nota("el operador acepta medir la campaña con el equipo NO APTO");
                        medir(p, orientacion);
                    })
                    .setNegativeButton("Cancelar", null).show();
            return;
        }
        final int n;
        try {
            n = Integer.parseInt(edN.getText().toString().trim());
            if (n < 3 || n > 30) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            alerta("N", "Escriba entre 3 y 30 disparos por patrón.");
            return;
        }
        midiendo = true;
        pantallaEncendida(true);
        pintar();
        txtResultado.setText("Midiendo " + p.nombre + "...");
        final double tolOrden = tol();
        Cliente.instancia().ejecutar(() -> {
            Campana.Serie serie = null;
            String error = null;
            try {
                char codigo = s.version == Sesion.Version.V36 ? 'e' : '6';
                serie = campana.nuevaSerie(Sesion.ahoraIso(), s.serie(), s.mac, s.firmware(), p.nombre, orientacion, codigo);
                Registro.nota("campana: " + serie.id + " " + p.nombre + " a " + orientacion + "°, N=" + n);
                if (s.disparosAsentamiento > 0) {
                    enUi(() -> txtResultado.setText("Disparo de asentamiento (se descarta)..."));
                    LecturaX.asentar(s);
                }
                for (int i = 0; i < n; i++) {
                    final int k = i + 1;
                    enUi(() -> txtResultado.setText("Midiendo " + p.nombre + ": disparo " + k + " de " + n + "..."));
                    LecturaX.Lectura l = LecturaX.leer(s);
                    if (l.valida()) {
                        campana.agregarDisparo(serie, Sesion.ahoraIso(), l.respuesta.trama, l.x);
                    } else {
                        Registro.nota("campana: disparo " + k + " no valido: " + l.error);
                    }
                }
            } catch (IOException | InterruptedException | RuntimeException e) {
                error = EnlaceSerie.descripcion(e);
            }
            final Campana.Serie fs = serie;
            final String ferr = error;
            enUi(() -> {
                midiendo = false;
                pantallaEncendida(false);
                if (fs == null) {
                    alerta("Medida", "No se pudo empezar: " + ferr);
                    pintar();
                    return;
                }
                if (ferr != null) {
                    txtResultado.setText("Medida interrumpida: " + ferr + Cliente.instancia().consejoSiMudo());
                }
                veredicto(fs, tolOrden);
            });
        });
    }

    private void veredicto(Campana.Serie serie, double tolOrden) {
        double[] x = new double[serie.disparos.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = serie.disparos.get(i).x;
        }
        Patron p = campana.patron(serie.patron);
        if (x.length == 0) {
            cerrar(serie, "SIN_DATOS", false, "ninguna lectura válida");
            new AlertDialog.Builder(this).setTitle(p.nombre + ": sin lecturas válidas")
                    .setMessage("Ningún disparo dio lectura." + Cliente.instancia().consejoSiMudo())
                    .setPositiveButton("Repetir", (d, w) -> medir(p, serie.orientacion))
                    .setNegativeButton("Cerrar", (d, w) -> pintar()).setCancelable(false).show();
            return;
        }
        Veredicto.Resultado v = Veredicto.evaluar(x, p, campana.medias(p.nombre), tolOrden);
        try {
            for (int i = 0; i < x.length; i++) {
                if (v.descartar[i] && !serie.disparos.get(i).descartado) {
                    campana.descartar(serie, i + 1, v.motivos[i]);
                }
            }
        } catch (IOException e) {
            alerta("Campaña", "No se pudo guardar: " + e.getMessage());
        }
        String cab = serie.id + " " + p.nombre + " (" + p.color + " " + p.tipo + ", cert " + Campana.fmt(p.valor)
                + ", " + serie.orientacion + "°)\n";
        Registro.nota("campana: veredicto " + serie.id + " " + v.veredicto + "\n" + v.texto);
        if ("OK".equals(v.veredicto)) {
            cerrar(serie, "OK", true, "");
            txtResultado.setText(cab + v.texto + "Aceptada. Siguiente patrón.");
            pintar();
            return;
        }
        txtResultado.setText(cab + v.texto);
        new AlertDialog.Builder(this).setTitle(p.nombre + ": " + v.veredicto)
                .setMessage(cab + v.texto)
                .setCancelable(false)
                .setPositiveButton("Repetir", (d, w) -> {
                    cerrar(serie, v.veredicto, false, "repetida");
                    medir(p, serie.orientacion);
                })
                .setNeutralButton("Aceptar con nota", (d, w) -> aceptarConNota(serie, v.veredicto))
                .setNegativeButton("Era otro patrón", (d, w) -> eraOtro(serie, tolOrden))
                .show();
    }

    private void aceptarConNota(Campana.Serie serie, String veredicto) {
        final EditText e = new EditText(this);
        e.setHint("Nota obligatoria: por qué se acepta");
        new AlertDialog.Builder(this).setTitle("Aceptar " + serie.id + " igualmente").setView(e)
                .setCancelable(false)
                .setPositiveButton("Aceptar", (d, w) -> {
                    String nota = e.getText().toString().trim();
                    if (nota.isEmpty()) {
                        aviso("La nota es obligatoria.");
                        aceptarConNota(serie, veredicto);
                        return;
                    }
                    cerrar(serie, veredicto, true, nota);
                    pintar();
                })
                .setNegativeButton("Volver", (d, w) -> veredicto(serie, tol())).show();
    }

    private void eraOtro(Campana.Serie serie, double tolOrden) {
        final List<Patron> cat = campana.catalogo();
        String[] t = new String[cat.size()];
        for (int i = 0; i < t.length; i++) {
            t[i] = cat.get(i).toString();
        }
        new AlertDialog.Builder(this).setTitle("¿Qué patrón era? (" + serie.id + ", medida como " + serie.patronOriginal + ")")
                .setItems(t, (d, w) -> {
                    try {
                        campana.reasignar(serie, cat.get(w).nombre, "el operador indica que era " + cat.get(w).nombre);
                        Registro.nota("campana: " + serie.id + " reasignada de " + serie.patronOriginal + " a "
                                + cat.get(w).nombre + " (el registro original se conserva)");
                    } catch (IOException | RuntimeException e) {
                        alerta("Campaña", "No se pudo reasignar: " + e.getMessage());
                        return;
                    }
                    veredicto(serie, tolOrden);
                })
                .setNegativeButton("Volver", (d, w) -> veredicto(serie, tolOrden)).show();
    }

    private void cerrar(Campana.Serie serie, String veredicto, boolean aceptada, String nota) {
        try {
            campana.cerrar(serie, veredicto, aceptada, nota);
        } catch (IOException e) {
            alerta("Campaña", "No se pudo guardar el veredicto: " + e.getMessage());
        }
    }

    // ------------------------------------------------------ exportar e importar

    private void exportar() {
        Sesion s = Sesion.get();
        if (campana == null) {
            return;
        }
        File zip;
        Campanas.Exportacion ex;
        try {
            ex = Campanas.exportarConHuellas(this, campana.equipo, campana.mac);
            zip = ex.zip;
        } catch (IOException | RuntimeException e) {
            alerta("Exportar", "No se pudo preparar el ZIP: " + e.getMessage());
            return;
        }
        String huellas = zip.getName() + "\nmd5 " + ex.md5 + "\nsha256 " + ex.sha256;
        txtResultado.setText("ZIP preparado:\n" + huellas);
        Uri u;
        try {
            u = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".ficheros", zip);
        } catch (IllegalArgumentException e) {
            alerta("Exportar", "No se pudo compartir el ZIP: " + e.getMessage());
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_STREAM, u);
        i.putExtra(Intent.EXTRA_SUBJECT, "Campaña " + campana.equipo + " - " + zip.getName());
        i.putExtra(Intent.EXTRA_TEXT, "Campaña de calibración de " + campana.equipo + " (" + campana.mac + "), "
                + s.firmware() + ". Avance: " + campana.avance()
                + (campana.cerrada() ? ". Campaña CERRADA." : "") + "\n" + huellas);
        i.setClipData(ClipData.newRawUri(zip.getName(), u));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Enviar la campaña (un solo ZIP)"));
        } catch (ActivityNotFoundException e) {
            aviso("No hay ninguna aplicación para compartir el ZIP.");
        }
    }

    private void importar() {
        if (campana == null) {
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(i, PIDE_CSV);
        } catch (ActivityNotFoundException e) {
            aviso("No hay selector de ficheros en este teléfono.");
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo != PIDE_CSV || resultado != RESULT_OK || datos == null || campana == null) {
            return;
        }
        final List<Uri> uris = new ArrayList<>();
        if (datos.getClipData() != null) {
            for (int k = 0; k < datos.getClipData().getItemCount(); k++) {
                uris.add(datos.getClipData().getItemAt(k).getUri());
            }
        } else if (datos.getData() != null) {
            uris.add(datos.getData());
        }
        final double tolOrden = tol();
        Cliente.instancia().ejecutar(() -> {
            String res;
            try {
                List<String> lineas = new ArrayList<>();
                for (Uri u : uris) {
                    try (InputStream in = getContentResolver().openInputStream(u);
                         BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String l;
                        while ((l = r.readLine()) != null) {
                            lineas.add(l);
                        }
                    }
                }
                Importador.Resultado ir = Importador.importar(campana, lineas, tolOrden);
                res = uris.size() + " ficheros: " + ir.texto();
                Registro.nota("campana: importacion de CSV: " + res);
            } catch (IOException | RuntimeException e) {
                res = "No se pudo importar: " + EnlaceSerie.descripcion(e);
            }
            final String fr = res;
            enUi(() -> {
                txtResultado.setText(fr);
                pintar();
            });
        });
    }

    private void cerrarCampana() {
        if (campana == null || campana.cerrada()) {
            return;
        }
        new AlertDialog.Builder(this).setTitle("Cerrar campaña")
                .setMessage("La campaña de " + campana.equipo + " queda de solo lectura: no se podrá medir, importar ni "
                        + "elegir series. Se puede seguir exportando. ¿Cerrar?")
                .setPositiveButton("Cerrar campaña", (d, w) -> {
                    try {
                        Campanas.cerrarCampana(this, campana.equipo, campana.mac);
                    } catch (IOException | RuntimeException e) {
                        alerta("Campaña", "No se pudo cerrar: " + e.getMessage());
                    }
                    pintar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void nueva() {
        if (campana == null) {
            return;
        }
        new AlertDialog.Builder(this).setTitle("Nueva campaña")
                .setMessage("La campaña actual de " + campana.equipo + " se archiva (no se borra) y empieza otra vacía. "
                        + "Exporte antes si no lo ha hecho.")
                .setPositiveButton("Archivar y empezar", (d, w) -> {
                    try {
                        Campanas.nueva(this, campana.equipo, campana.mac);
                        campana = Campanas.abierta();
                        SALTADOS.clear();
                    } catch (IOException | RuntimeException e) {
                        alerta("Campaña", "No se pudo: " + e.getMessage());
                    }
                    pintar();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnOk != null && campana != null) {
            pintar();
        }
    }
}
