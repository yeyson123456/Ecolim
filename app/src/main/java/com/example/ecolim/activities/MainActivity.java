package com.example.ecolim.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.fragments.ConfiguracionFragment;
import com.example.ecolim.fragments.HistorialFragment;
import com.example.ecolim.fragments.HomeFragment;
import com.example.ecolim.fragments.RegistroFragment;
import com.example.ecolim.fragments.ReportesFragment;
import com.example.ecolim.models.Usuario;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executor;

public class MainActivity extends BaseActivity {

    private static final String PREFS_NAME  = "ecolim_prefs";
    private static final String KEY_USER_ID = "usuario_id";

    private BottomNavigationView bottomNav;
    private MaterialToolbar      toolbar;
    private long                 usuarioId;
    private Usuario              usuarioActual;
    private EcolimDbHelper       db;

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int nivel = prefs.getInt("font_size", 2);
        float[] escalas = {0.75f, 0.875f, 1.0f, 1.15f, 1.3f};
        float escala = (nivel >= 0 && nivel < escalas.length) ? escalas[nivel] : 1.0f;
        Configuration config = base.getResources().getConfiguration();
        config.fontScale = escala;
        Context ctx = base.createConfigurationContext(config);
        super.attachBaseContext(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(
                prefs.getBoolean("modo_oscuro", false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db        = EcolimDbHelper.getInstance(this);
        bottomNav = findViewById(R.id.bottom_navigation);
        toolbar   = findViewById(R.id.toolbar_main);

        // ✅ Registrar toolbar como ActionBar para que onCreateOptionsMenu funcione
        if (toolbar != null) setSupportActionBar(toolbar);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        boolean vieneDeSesionGuardada = usuarioId == -1;

        if (vieneDeSesionGuardada) {
            usuarioId = prefs.getLong(KEY_USER_ID, -1);
        }

        if (usuarioId == -1) { irALogin(); return; }

        usuarioActual = db.obtenerUsuarioPorId(usuarioId);
        if (usuarioActual == null) { irALogin(); return; }

        configurarNavegacion();

        if (vieneDeSesionGuardada) {
            pedirHuella();
        } else {
            if (savedInstanceState == null) {
                cargarFragmento(new HomeFragment());
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ✅ MENÚ DEL TOOLBAR — ícono QR
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Ícono QR en el toolbar (esquina superior derecha)
        menu.add(0, R.id.menu_qr, 0, "Códigos QR")
                .setIcon(android.R.drawable.ic_menu_camera)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_qr) {
            startActivity(new Intent(this, QRGeneratorActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BIOMETRÍA
    // ══════════════════════════════════════════════════════════════════════════

    private void pedirHuella() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            cargarFragmento(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                        cargarFragmento(new HomeFragment());
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        cerrarSesion();
                    }
                    @Override
                    public void onAuthenticationFailed() {}
                });

        biometricPrompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Ecolim")
                .setSubtitle("Verifica tu identidad para continuar")
                .setNegativeButtonText("Usar contraseña")
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVEGACIÓN
    // ══════════════════════════════════════════════════════════════════════════

    private void configurarNavegacion() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmento = null;
            int id = item.getItemId();

            if      (id == R.id.nav_home)          fragmento = new HomeFragment();
            else if (id == R.id.nav_registro)       fragmento = new RegistroFragment();
            else if (id == R.id.nav_historial)      fragmento = new HistorialFragment();
            else if (id == R.id.nav_reportes)       fragmento = new ReportesFragment();
            else if (id == R.id.nav_configuracion)  fragmento = new ConfiguracionFragment();

            if (fragmento != null) {
                Bundle args = new Bundle();
                args.putLong("usuario_id", usuarioId);
                fragmento.setArguments(args);
                cargarFragmento(fragmento);
                return true;
            }
            return false;
        });
    }

    private void cargarFragmento(Fragment fragmento) {
        if (fragmento.getArguments() == null) {
            Bundle args = new Bundle();
            args.putLong("usuario_id", usuarioId);
            fragmento.setArguments(args);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragmento)
                .commit();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SESIÓN
    // ══════════════════════════════════════════════════════════════════════════

    public Usuario getUsuarioActual() { return usuarioActual; }
    public long    getUsuarioId()     { return usuarioId; }

    public void cerrarSesion() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().remove(KEY_USER_ID).apply();
        irALogin();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}