package com.example.ecolim.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecolim.R;

public class SplashActivity extends BaseActivity  {

    private static final int    SPLASH_DELAY = 2500;
    private static final String PREFS_NAME   = "ecolim_prefs";
    private static final String KEY_USER_ID  = "usuario_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo   = findViewById(R.id.img_logo);
        TextView  txtSlogan = findViewById(R.id.txt_slogan);

        // Animación logo
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(800);
        scaleAnim.setFillAfter(true);
        imgLogo.startAnimation(scaleAnim);

        // Animación slogan
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1000);
        fadeIn.setStartOffset(600);
        fadeIn.setFillAfter(true);
        txtSlogan.startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            long usuarioId = prefs.getLong(KEY_USER_ID, -1);

            Intent intent = usuarioId != -1
                    ? new Intent(this, MainActivity.class).putExtra("usuario_id", usuarioId)
                    : new Intent(this, LoginActivity.class);

            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DELAY);
    }
}