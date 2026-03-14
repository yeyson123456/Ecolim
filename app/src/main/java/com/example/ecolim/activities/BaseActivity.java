package com.example.ecolim.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecolim.ThemeManager;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        // wrapFontScale ajusta la escala de texto según la preferencia guardada
        Context wrapped = ThemeManager.get(base).wrapFontScale(base);
        super.attachBaseContext(wrapped);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // CRÍTICO: aplicarTema() ANTES de super.onCreate()
        ThemeManager.get(this).aplicarTema(this);
        super.onCreate(savedInstanceState);
    }
}