package com.dpi.retrov36;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Medir el banco" (3.6.10, corte A; RF-APP-33, RF-CAL-35). Sigue la cola del APK paso a paso:
 * calentamiento, bateria (9), OSCURO, A5, patrones por color y exportar. Cada paso queda en el
 * diario de la campana (evento PASO): al volver, sigue en el primer paso pendiente. Los pasos de
 * control no se saltan; los patrones si, y quedan marcados.
 */
public class BancoActivity extends Base {

    private BancoCola cola;
    private Campana campana;
    private TextView txtAvance;
    private TextView txtPaso;
    private TextView txtResultado;
    private Button btnOk;
    private Button btnSaltar;
    private BancoCola.Paso paso;
    private volatile boolean ocupado;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        txtAvance = texto("");
        txtAvance.setTypeface(Typeface.DEFAULT_BOLD);
        txtPaso = texto("");
        txtPaso.setTextSize(24);
        txtPaso.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk = boton("OK", v -> ok());
        btnSaltar = boton("Saltar este patrón (queda marcado)", v -> saltar());
        fila(btnOk, btnSaltar);
        txtResultado = texto("");
        txtResultado.setTypeface(Typeface.MONOSPACE);
        boton("Exportar ahora (un solo ZIP + copia en Download/RTV/)", v -> exportar(null));
        cargar();
    }

    private void cargar() {
        try (InputStream in = getAssets().open(BancoCola.ASSET)) {
            cola = BancoCola.cargar(ImportadorCampana.leer(in));
        } catch (IOException | RuntimeException e) {
            txtPaso.setText("Cola no admitida: " + e.getMessage() + ". No se mide nada.");
            Registro.nota("banco: cola no admitida: " + e.getMessage());
            btnOk.setEnabled(false);
            btnSaltar.setEnabled(false);
            return;
        }
        Sesion s = Sesion.get();
        if (s.mac == null || s.mac.isEmpty() || !s.serieConocida()) {
            txtPaso.setText("Conecte con el equipo (y, si hace falta, dé su serie en Campaña) antes de medir el banco.");
            btnOk.setEnabled(false);
            btnSaltar.setEnabled(false);
            return;
        }
        try {
            campana = Campanas.abrir(this, s.serie(), s.mac);
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo abrir la campaña: " + e.getMessage());
            return;
        }
        Registro.nota("banco: cola md5 " + cola.md5 + ", " + cola.pasos.size() + " pasos");
        pintar();
    }

    private void pintar() {
        if (cola == null || campana == null) {
            return;
        }
        Map<Integer, String> est = campana.pasos();
        int hechos = 0;
        int saltados = 0;
        for (String e : est.values()) {
            if ("HECHO".equals(e)) {
                hechos++;
            } else {
                saltados++;
            }
        }
        paso = cola.siguiente(est);
        txtAvance.setText(String.format(Locale.US, "Equipo %s (%s). Cola md5 %s…\nPasos hechos %d de %d, saltados %d.%s",
                campana.equipo, campana.mac, cola.md5.substring(0, 8), hechos, cola.pasos.size(), saltados,
                campana.bateriaBloqueaEscrituras() ? "\nBATERÍA: escrituras bloqueadas (n = 0 o sin respuesta)." : ""));
        if (paso == null) {
            txtPaso.setText("Banco completo.");
        } else {
            String reintento = "SALTADO".equals(est.get(paso.orden)) ? " (saltado antes)" : "";
            txtPaso.setText("Sesión " + paso.sesion + " · paso " + paso.orden + reintento + "\n" + paso.instruccion()
                    + (paso.esMedida() ? String.format(Locale.US, "\n%d colocaciones × %d disparos + %d de asentamiento",
                    paso.k, paso.m, paso.asentamiento) : "")
                    + (paso.esMedida() && !paso.codigo.isEmpty() ? "\nCódigo " + paso.codigo + ", uso " + paso.uso : "")
                    + (paso.nota.isEmpty() ? "" : "\nNota: " + paso.nota)
                    + (paso.ajusteApp.isEmpty() ? "" : "\n" + paso.ajusteApp));
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        boolean abierta = !campana.cerrada();
        btnOk.setEnabled(abierta && paso != null && !ocupado
                && (con || "CALENTAMIENTO".equals(paso.tipo) || "EXPORTAR".equals(paso.tipo)));
        btnSaltar.setEnabled(abierta && paso != null && !ocupado && paso.saltable());
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnOk != null) {
            pintar();
        }
    }

    private void anotar(BancoCola.Paso p, String estado, String serieId, String nota) {
        try {
            campana.anotarPaso(p.orden, estado, serieId, Sesion.ahoraIso(), nota);
        } catch (IOException | RuntimeException e) {
            alerta("Campaña", "No se pudo guardar el paso: " + e.getMessage());
        }
    }

    private void saltar() {
        if (paso == null || !paso.saltable()) {
            return;
        }
        anotar(paso, "SALTADO", "", "saltado por el operador");
        Registro.nota("banco: paso " + paso.orden + " (" + paso.patron + ") saltado");
        pintar();
    }

    private void ok() {
        final BancoCola.Paso p = paso;
        if (p == null) {
            return;
        }
        switch (p.tipo) {
            case "CALENTAMIENTO":
                anotar(p, "HECHO", "", "calentamiento confirmado por el operador");
                pintar();
                break;
            case "BATERIA":
                bateria(p);
                break;
            case "EXPORTAR":
                exportar(p);
                break;
            default:
                if (p.esMedida()) {
                    medir(p);
                } else {
                    anotar(p, "HECHO", "", "paso sin acción en la app: " + p.tipo);
                    pintar();
                }
        }
    }

    // ------------------------------------------------------------------ bateria

    private void bateria(BancoCola.Paso p) {
        ocupado = true;
        pintar();
        Cliente.instancia().ejecutar(() -> {
            Integer n = null;
            String err = null;
            try {
                Cliente.Respuesta r = Cliente.instancia().pedir("9", Tramas.Tipo.BATERIA, Cliente.TIMEOUT_MEDIDA_MS);
                n = r.valida() ? Bateria.n(r.trama) : null;
            } catch (IOException | InterruptedException | RuntimeException e) {
                err = EnlaceSerie.descripcion(e);
            }
            final Bateria.Lectura l = Bateria.interpretar(n);
            final String ferr = err;
            enUi(() -> {
                ocupado = false;
                if (ferr != null) {
                    alerta("Batería", "Enlace perdido: " + ferr);
                    pintar();
                    return;
                }
                try {
                    campana.anotarBateria(Sesion.ahoraIso(), l.n, l.texto);
                } catch (IOException e) {
                    alerta("Campaña", "No se pudo guardar la batería: " + e.getMessage());
                }
                anotar(p, "HECHO", "", l.texto);
                txtResultado.setText(l.texto);
                if (l.aviso) {
                    alerta("Batería", l.texto);
                }
                pintar();
            });
        });
    }

    // ------------------------------------------------------------------ medida

    private void medir(BancoCola.Paso p) {
        Sesion s = Sesion.get();
        if (!s.versionMedible()) {
            alerta("No se puede medir", "Pase antes las pruebas del equipo (firmware: " + s.firmware() + ").");
            return;
        }
        ocupado = true;
        pantallaEncendida(true);
        pintar();
        final List<double[]> hechas = new ArrayList<>();
        final Campana.Serie[] serie = {null};
        Cliente.instancia().ejecutar(() -> colocacion(p, 1, serie, hechas));
    }

    /** Una colocacion en el hilo de trabajo: asentamiento (con el filtro de patron presente) + M disparos. */
    private void colocacion(BancoCola.Paso p, int k, Campana.Serie[] serie, List<double[]> hechas) {
        Sesion s = Sesion.get();
        String nombre = "OSCURO".equals(p.tipo) ? Campana.OSCURO.nombre : p.patron;
        String error = null;
        String ausente = null;
        try {
            double xa = Double.NaN;
            for (int i = 0; i < Math.max(1, p.asentamiento); i++) {
                final int kk = k;
                enUi(() -> txtResultado.setText("Colocación " + kk + " de " + p.k + ": asentamiento (se descarta)..."));
                LecturaX.Lectura l = LecturaX.leer(s);
                Registro.nota("banco: disparo de asentamiento, descartado: " + l.codigo + " -> "
                        + (l.respuesta.valida() ? l.respuesta.trama : l.respuesta.describir()));
                xa = l.x;
            }
            ausente = Colocacion.patronAusente(nombre, xa, p.xEsperada);
            if (ausente == null) {
                List<Double> xs = new ArrayList<>();
                for (int i = 0; i < p.m; i++) {
                    final int kk = k;
                    final int j = i + 1;
                    enUi(() -> txtResultado.setText("Midiendo " + nombre + ": colocación " + kk + " de " + p.k
                            + ", disparo " + j + " de " + p.m + "..."));
                    LecturaX.Lectura l = LecturaX.leer(s);
                    if (!l.valida()) {
                        Registro.nota("banco: disparo no válido: " + l.error);
                        continue;
                    }
                    if (serie[0] == null) {
                        serie[0] = campana.nuevaSerie(Sesion.ahoraIso(), s.serie(), s.mac, s.firmware(), nombre, 0,
                                l.codigo);
                        Registro.nota("banco: paso " + p.orden + " -> serie " + serie[0].id);
                    }
                    campana.agregarDisparo(serie[0], k, Sesion.ahoraIso(), l.respuesta.trama, l.x);
                    xs.add(l.x);
                }
                hechas.add(Estadistica.aVector(xs));
            } else {
                Registro.nota("banco: " + ausente);
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            error = EnlaceSerie.descripcion(e);
        }
        final String ferr = error;
        final String faus = ausente;
        enUi(() -> {
            if (ferr != null) {
                ocupado = false;
                pantallaEncendida(false);
                alerta("Medida interrumpida", ferr + Cliente.instancia().consejoSiMudo()
                        + "\nAl volver se repite el paso entero.");
                cerrarIncompleta(serie[0], "interrumpida: " + ferr);
                pintar();
                return;
            }
            if (faus != null) {
                new AlertDialog.Builder(this).setTitle("¿Está el patrón?")
                        .setMessage(faus).setCancelable(false)
                        .setPositiveButton("Ya está colocado", (d, w) -> Cliente.instancia().ejecutar(
                                () -> colocacion(p, k, serie, hechas)))
                        .setNegativeButton("Dejar el paso", (d, w) -> {
                            ocupado = false;
                            pantallaEncendida(false);
                            cerrarIncompleta(serie[0], "dejada: patrón no presente");
                            pintar();
                        }).show();
                return;
            }
            if (k < p.k) {
                new AlertDialog.Builder(this).setTitle("Levante y apoye (" + (k + 1) + " de " + p.k + ")")
                        .setMessage("Levante el equipo y vuelva a apoyarlo sobre " + nombre + ". Pulse OK cuando esté apoyado.")
                        .setCancelable(false)
                        .setPositiveButton("OK", (d, w) -> Cliente.instancia().ejecutar(
                                () -> colocacion(p, k + 1, serie, hechas)))
                        .show();
                return;
            }
            ocupado = false;
            pantallaEncendida(false);
            veredicto(p, serie[0], hechas);
        });
    }

    private void cerrarIncompleta(Campana.Serie s, String nota) {
        if (s != null) {
            try {
                campana.cerrar(s, "REPETIR", false, "banco: " + nota);
            } catch (IOException e) {
                Registro.nota("no se pudo cerrar la serie: " + e.getMessage());
            }
        }
    }

    private void veredicto(BancoCola.Paso p, Campana.Serie s, List<double[]> grupos) {
        if (s == null) {
            alerta("Sin lecturas", "Ningún disparo dio lectura. Se repite el paso.");
            pintar();
            return;
        }
        String cab = "Paso " + p.orden + ", " + s.id + " " + s.patron + ", código " + p.codigo + ", uso " + p.uso;
        if ("A5".equals(p.tipo)) {
            try {
                campana.cerrar(s, A5.VEREDICTO, false, "banco paso " + p.orden + " (" + p.bloque + ")");
            } catch (IOException e) {
                alerta("Campaña", e.getMessage());
            }
            anotar(p, "HECHO", s.id, "A5");
            txtResultado.setText(cab + "\n" + A5.evaluar(campana).texto);
            pintar();
            return;
        }
        Patron pat = campana.patron(s.patron);
        Veredicto.Resultado v = Veredicto.evaluarColocaciones(grupos, pat, campana.medias(pat.nombre),
                Veredicto.TOL_ORDEN, Veredicto.REPRO_MAX);
        try {
            int base = 0;
            int idx = 0;
            for (double[] g : grupos) {
                for (int i = 0; i < g.length; i++) {
                    if (v.descartar[base + i]) {
                        // indices de disparo de la serie: en orden de colocacion
                        campana.descartar(s, idx + 1, v.motivos[base + i]);
                    }
                    idx++;
                }
                base += g.length;
            }
        } catch (IOException | RuntimeException e) {
            Registro.nota("banco: no se pudo anotar un descarte: " + e.getMessage());
        }
        String nota = "banco paso " + p.orden + ", código " + p.codigo + ", uso " + p.uso;
        if ("OK".equals(v.veredicto)) {
            cerrarSerie(s, "OK", true, nota);
            anotar(p, "HECHO", s.id, "OK");
            txtResultado.setText(cab + "\n" + v.texto + "Aceptada. Siguiente paso.");
            pintar();
            return;
        }
        txtResultado.setText(cab + "\n" + v.texto);
        AlertDialog.Builder d = new AlertDialog.Builder(this).setTitle(s.patron + ": " + v.veredicto)
                .setMessage(cab + "\n" + v.texto).setCancelable(false)
                .setPositiveButton("Repetir", (x, w) -> {
                    cerrarSerie(s, v.veredicto, false, nota + "; repetida");
                    medir(p);
                })
                .setNeutralButton("Aceptar con nota", (x, w) -> aceptarConNota(p, s, v.veredicto, nota));
        if (p.saltable()) {
            d.setNegativeButton("Saltar", (x, w) -> {
                cerrarSerie(s, v.veredicto, false, nota + "; saltada");
                anotar(p, "SALTADO", s.id, v.veredicto);
                pintar();
            });
        }
        d.show();
    }

    private void aceptarConNota(BancoCola.Paso p, Campana.Serie s, String ver, String nota) {
        final EditText e = new EditText(this);
        e.setHint("Nota obligatoria");
        new AlertDialog.Builder(this).setTitle("Aceptar " + s.id).setView(e).setCancelable(false)
                .setPositiveButton("Aceptar", (d, w) -> {
                    String n = e.getText().toString().trim();
                    if (n.isEmpty()) {
                        aviso("La nota es obligatoria.");
                        aceptarConNota(p, s, ver, nota);
                        return;
                    }
                    cerrarSerie(s, ver, true, nota + "; " + n);
                    anotar(p, "HECHO", s.id, ver + " aceptada con nota");
                    pintar();
                }).show();
    }

    private void cerrarSerie(Campana.Serie s, String ver, boolean aceptada, String nota) {
        try {
            campana.cerrar(s, ver, aceptada, nota);
        } catch (IOException e) {
            alerta("Campaña", "No se pudo guardar el veredicto: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- exportar

    private void exportar(BancoCola.Paso p) {
        if (campana == null) {
            return;
        }
        if (p != null) {
            anotar(p, "HECHO", "", "exportación de la sesión " + p.sesion);
        }
        Campanas.Exportacion ex;
        try {
            ex = Campanas.exportarConHuellas(this, campana.equipo, campana.mac);
        } catch (IOException | RuntimeException e) {
            alerta("Exportar", "No se pudo preparar el ZIP: " + e.getMessage());
            pintar();
            return;
        }
        txtResultado.setText("ZIP: " + ex.zip.getName() + "\nmd5 " + ex.md5 + "\nsha256 " + ex.sha256 + "\nCopia: " + ex.copia);
        compartirZip(ex, "Banco de " + campana.equipo + " (" + campana.mac + "), " + Sesion.get().firmware());
        pintar();
    }
}
