package com.example.ecolim.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.fragments.ConfiguracionFragment;
import com.example.ecolim.fragments.HistorialFragment;
import com.example.ecolim.fragments.HomeFragment;
import com.example.ecolim.fragments.RegistroFragment;
import com.example.ecolim.fragments.ReportesFragment;
import com.example.ecolim.models.Usuario;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {

    private static final String PREFS_NAME  = "ecolim_prefs";
    private static final String KEY_USER_ID = "usuario_id";

    private BottomNavigationView bottomNav;
    private long                 usuarioId;
    private Usuario              usuarioActual;
    private EcolimDbHelper       db;

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int nivel = prefs.getInt("font_size", 2); // 0=muy pequeño, 2=normal, 4=muy grande

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

        // Obtener usuario de Intent o SharedPreferences
        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        if (usuarioId == -1) {
            usuarioId = prefs.getLong(KEY_USER_ID, -1);
        }

        if (usuarioId == -1) {
            irALogin();
            return;
        }

        usuarioActual = db.obtenerUsuarioPorId(usuarioId);
        if (usuarioActual == null) {
            irALogin();
            return;
        }

        // Mostrar HomeFragment por defecto
        if (savedInstanceState == null) {
            cargarFragmento(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Listener de navegación
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragmento = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                fragmento = new HomeFragment();
            } else if (id == R.id.nav_registro) {
                fragmento = new RegistroFragment();
            } else if (id == R.id.nav_historial) {
                fragmento = new HistorialFragment();
            } else if (id == R.id.nav_reportes) {
                fragmento = new ReportesFragment();
            } else if (id == R.id.nav_configuracion) {
                fragmento = new ConfiguracionFragment();
            }

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

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    // Cerrar sesión
    public void cerrarSesion() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_USER_ID)
                .apply();
        irALogin();
    }

    private void irALogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
        finish();
    }

}