package com.example.ecolim.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ecolim.R;
import com.example.ecolim.database.EcolimDbHelper;
import com.example.ecolim.models.Usuario;

/**
 * SplashActivity — Pantalla de inicio de ECOLIM.
 *
 * Flujo:
 *  1. Muestra el logo animado 2 segundos
 *  2. Solicita permisos necesarios (cámara, notificaciones)
 *  3. Verifica si hay sesión guardada
 *     → Si hay sesión válida  → MainActivity (auto-login)
 *     → Si no hay sesión      → LoginActivity
 *
 * UBICACIÓN: app/src/main/java/com/example/ecolim/activities/SplashActivity.java
 */
public class SplashActivity extends BaseActivity {

    private static final String PREFS_NAME    = "ecolim_prefs";
    private static final String KEY_USER_ID   = "usuario_id";
    private static final int    SPLASH_DELAY  = 2000; // 2 segundos
    private static final int    REQ_PERMISOS  = 100;

    private TextView txtEstado;
    private boolean  permisosSolicitados = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        txtEstado = findViewById(R.id.txt_splash_estado);

        // Deshabilitar botón atrás en el splash
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        // No hacer nada — el splash no se puede cerrar con atrás
                    }
                });

        // Iniciar flujo después del delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            solicitarPermisos();
        }, SPLASH_DELAY);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PERMISOS
    // ══════════════════════════════════════════════════════════════════════════

    private void solicitarPermisos() {
        // Lista de permisos necesarios
        java.util.List<String> permisosFaltantes = new java.util.ArrayList<>();

        // Cámara (QR y foto de residuos)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permisosFaltantes.add(Manifest.permission.CAMERA);
        }

        // Notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Galería (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permisosFaltantes.isEmpty()) {
            // Hay permisos que pedir
            setEstado("Solicitando permisos...");
            ActivityCompat.requestPermissions(
                    this,
                    permisosFaltantes.toArray(new String[0]),
                    REQ_PERMISOS);
        } else {
            // Todos los permisos ya concedidos
            verificarSesion();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISOS) {
            // Independientemente del resultado → continuar
            // La app funciona sin algunos permisos (la cámara se pedirá al usarla)
            verificarSesion();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AUTO-LOGIN
    // ══════════════════════════════════════════════════════════════════════════

    private void verificarSesion() {
        setEstado("Verificando sesión...");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long usuarioId = prefs.getLong(KEY_USER_ID, -1);

        if (usuarioId != -1) {
            // Hay sesión guardada — verificar que el usuario existe en BD
            EcolimDbHelper db = EcolimDbHelper.getInstance(this);
            Usuario u = db.obtenerUsuarioPorId(usuarioId);

            if (u != null && u.isActivo()) {
                // ✅ Sesión válida → ir a MainActivity sin pasar por Login
                setEstado("Bienvenido, " + u.getNombre() + "...");
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        irA(MainActivity.class, false), 600);
                return;
            } else {
                // Usuario no existe o fue desactivado → limpiar sesión
                prefs.edit().remove(KEY_USER_ID).apply();
            }
        }

        // Sin sesión → ir al Login
        setEstado("Iniciando...");
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                irA(LoginActivity.class, true), 400);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void irA(Class<?> destino, boolean limpiarStack) {
        Intent intent = new Intent(this, destino);
        if (limpiarStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setEstado(String mensaje) {
        if (txtEstado != null) {
            runOnUiThread(() -> txtEstado.setText(mensaje));
        }
    }
}