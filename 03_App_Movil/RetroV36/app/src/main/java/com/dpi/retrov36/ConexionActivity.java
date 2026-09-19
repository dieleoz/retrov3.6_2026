package com.dpi.retrov36;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;

/**
 * Pantalla principal: dispositivos emparejados, conexion SPP y acceso al resto.
 * "Conectado" solo aparece cuando connect() ha terminado sin excepcion
 * (EnlaceSerie). Al conectar se abren solas las pruebas del equipo.
 */
public class ConexionActivity extends Base {

    private static final int PIDE_ACTIVAR_BT = 101;
    private static final int PIDE_PERMISOS = 102;

    private LinearLayout listaDispositivos;
    private TextView txtLista;
    private Button btnPruebas;
    private Button btnMedir;
    private Button btnBotones;
    private Button btnCampana;
    private Button btnAdmin;
    /** true entre el toque del operador y el final de connect(). */
    private boolean esperandoConexion;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        texto("RTV V3.6 " + BuildConfig.VERSION_NAME + " - retrorreflectómetro vertical V3 / V3.6 (PIC18F47K42). "
                + "Mide patrones y, sólo con firmware V3.6, calibra.");
        btnPruebas = boton("1. Pruebas del equipo", v -> startActivity(new Intent(this, PruebasActivity.class)));
        btnMedir = boton("2. Medida de patrones", v -> startActivity(new Intent(this, MedidaActivity.class)));
        btnCampana = boton("3. Campaña de calibración (guiada, un solo envío)",
                v -> startActivity(new Intent(this, CampanaActivity.class)));
        btnBotones = boton("Botones de pantalla (sólo V3.6)", v -> startActivity(new Intent(this, BotonesActivity.class)));
        btnAdmin = boton("Modo administrador (sólo V3.6)", v -> startActivity(new Intent(this, AdminActivity.class)));
        boton("Compartir registro y datos", v -> compartirTodo());
        Button act = boton("Actualizar lista", v -> cargarEmparejados());
        Button des = boton("Desconectar", v -> {
            if (Pruebas.get().enCurso()) {
                Pruebas.get().cancelar();
            }
            EnlaceSerie.instancia().desconectar();
        });
        fila(act, des);
        titulo("Dispositivos emparejados (toque uno para conectar)");
        txtLista = texto("");
        listaDispositivos = new LinearLayout(this);
        listaDispositivos.setOrientation(LinearLayout.VERTICAL);
        raiz.addView(listaDispositivos);
        Campanas.iniciar(this);
        pedirPermisos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEmparejados();
        refrescar();
    }

    @Override
    public void estado(boolean conectado, String texto) {
        if (conectado && esperandoConexion) {
            esperandoConexion = false;
            EnlaceSerie en = EnlaceSerie.instancia();
            Sesion.get().reiniciar(en.getNombre(), en.getMac());
            Cliente.instancia().reiniciarCuenta();
            Registro.nota("sesion nueva: " + Sesion.get().identidad());
            // La campana de ESTE equipo se abre al conectar, para que las pruebas queden en ella.
            if (Sesion.get().serieConocida()) {
                try {
                    Campanas.abrir(this, Sesion.get().serie(), en.getMac());
                } catch (java.io.IOException | RuntimeException e) {
                    Registro.nota("no se pudo abrir la campaña: " + e.getMessage());
                }
            }
            Intent i = new Intent(this, PruebasActivity.class);
            i.putExtra(PruebasActivity.EXTRA_AUTO, true);
            startActivity(i);
        } else if (!conectado && !EnlaceSerie.instancia().estaConectando()) {
            esperandoConexion = false;
        }
        super.estado(conectado, texto);
    }

    @Override
    protected void refrescar() {
        super.refrescar();
        if (btnPruebas == null) {
            return;
        }
        boolean con = EnlaceSerie.instancia().estaConectado();
        Sesion s = Sesion.get();
        btnPruebas.setEnabled(con);
        btnMedir.setEnabled(con);
        btnBotones.setEnabled(con && s.version == Sesion.Version.V36);
        btnAdmin.setEnabled(con);
        listaDispositivos.setEnabled(!EnlaceSerie.instancia().estaConectando());
    }

    private void pedirPermisos() {
        // targetSdk 30: BLUETOOTH y BLUETOOTH_ADMIN se conceden al instalar;
        // solo la ubicacion se pide en ejecucion (mismo modelo que RetroDiagBT).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PIDE_PERMISOS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, @NonNull String[] permisos, @NonNull int[] res) {
        super.onRequestPermissionsResult(codigo, permisos, res);
        cargarEmparejados();
    }

    private void cargarEmparejados() {
        listaDispositivos.removeAllViews();
        BluetoothAdapter bt = EnlaceSerie.adaptador();
        if (bt == null) {
            txtLista.setText("Este teléfono no tiene Bluetooth.");
            return;
        }
        if (!bt.isEnabled()) {
            txtLista.setText("El Bluetooth está apagado. Actívelo y pulse Actualizar lista.");
            try {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), PIDE_ACTIVAR_BT);
            } catch (SecurityException | ActivityNotFoundException e) {
                aviso("No se pudo pedir que se active el Bluetooth: " + e.getClass().getSimpleName());
            }
            return;
        }
        try {
            Set<BluetoothDevice> emparejados = bt.getBondedDevices();
            txtLista.setText(emparejados.isEmpty()
                    ? "No hay dispositivos emparejados. Empareje el equipo desde los ajustes del teléfono."
                    : "El equipo aparece como COVIANDINA_<serie> (p. ej. COVIANDINA_SLV-002).");
            for (final BluetoothDevice d : emparejados) {
                Button b = new Button(this);
                b.setAllCaps(false);
                b.setText(EnlaceSerie.nombreDe(d) + "\n" + d.getAddress());
                b.setOnClickListener(v -> conectarCon(d));
                listaDispositivos.addView(b);
            }
        } catch (SecurityException e) {
            txtLista.setText("Falta el permiso de Bluetooth: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent datos) {
        super.onActivityResult(codigo, resultado, datos);
        if (codigo == PIDE_ACTIVAR_BT) {
            cargarEmparejados();
        }
    }

    private void conectarCon(BluetoothDevice d) {
        if (Pruebas.get().enCurso()) {
            aviso("Hay pruebas en curso. Cancélelas antes de reconectar.");
            return;
        }
        if (EnlaceSerie.instancia().estaConectando()) {
            return;
        }
        esperandoConexion = true;
        EnlaceSerie.instancia().conectar(this, d);
        refrescar();
    }
}
