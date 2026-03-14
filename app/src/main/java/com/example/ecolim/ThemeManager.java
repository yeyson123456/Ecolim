package com.example.ecolim;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREFS       = "ecolim_prefs";
    private static final String KEY_DARK    = "modo_oscuro";
    private static final String KEY_CONTRAST= "alto_contraste";
    private static final String KEY_COLOR   = "color_tema";
    private static final String KEY_FONT    = "font_size";

    private static ThemeManager instancia;
    private final SharedPreferences prefs;

    private ThemeManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager get(Context ctx) {
        if (instancia == null) instancia = new ThemeManager(ctx);
        return instancia;
    }

    public boolean isModoOscuro() {
        return prefs.getBoolean(KEY_DARK, false);
    }

    public void setModoOscuro(boolean on) {
        prefs.edit().putBoolean(KEY_DARK, on).apply();
        AppCompatDelegate.setDefaultNightMode(
                on ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Alto contraste
    // ═════════════════════════════════════════════════════════════════
    public boolean isAltoContraste() {
        return prefs.getBoolean(KEY_CONTRAST, false);
    }

    public void setAltoContraste(boolean on) {
        prefs.edit().putBoolean(KEY_CONTRAST, on).apply();
    }

    public int getColorTema() {
        return prefs.getInt(KEY_COLOR, 0);
    }

    public void setColorTema(int idx) {
        prefs.edit().putInt(KEY_COLOR, idx).apply();
    }

    public int getFontSize() {
        return prefs.getInt(KEY_FONT, 2);
    }

    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT, size).apply();
    }

    public void aplicarTema(androidx.appcompat.app.AppCompatActivity activity) {
        // Alto contraste tiene prioridad sobre todo
        if (isAltoContraste()) {
            activity.setTheme(R.style.Theme_Ecolim_AltoContraste);
            return;
        }
        // Modo oscuro
        if (isModoOscuro()) {
            activity.setTheme(R.style.Theme_Ecolim_Dark);
            return;
        }
        // Color de tema
        switch (getColorTema()) {
            case 1:  activity.setTheme(R.style.Theme_Ecolim_Azul);    break;
            case 2:  activity.setTheme(R.style.Theme_Ecolim_Naranja); break;
            case 3:  activity.setTheme(R.style.Theme_Ecolim_Gris);    break;
            default: activity.setTheme(R.style.Theme_Ecolim);         break;
        }
    }

    public Context wrapFontScale(Context base) {
        float[] scales = {0.75f, 0.87f, 1.0f, 1.15f, 1.30f};
        int idx = getFontSize();
        float scale = (idx >= 0 && idx < scales.length) ? scales[idx] : 1.0f;
        if (scale == 1.0f) return base;

        android.content.res.Configuration config =
                new android.content.res.Configuration(
                        base.getResources().getConfiguration());
        config.fontScale = scale;
        return base.createConfigurationContext(config);
    }

    public void inicializar() {
        AppCompatDelegate.setDefaultNightMode(
                isModoOscuro()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
