package com.example.ecolim;

import android.app.Application;

public class EcolimApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Aplica modo oscuro / claro global al iniciar
        ThemeManager.get(this).inicializar();
    }
}