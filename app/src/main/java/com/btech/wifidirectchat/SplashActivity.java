package com.btech.wifidirectchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int progress = 0;

    // Loading messages jo ek ek karke dikhenge
    private String[] statusMessages = {
        "Initializing secure channel...",
        "Loading encryption keys...",
        "Activating mesh protocol...",
        "Scanning for peers...",
        "DMS Ready! 🚀"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen karo
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progress_bar);
        tvStatus    = findViewById(R.id.tv_status);

        View logoContainer = findViewById(R.id.logo_container);
        View glowOuter     = findViewById(R.id.glow_outer);
        View glowInner     = findViewById(R.id.glow_inner);
        TextView tvName    = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);

        // ── Animations Load Karo ──────────────────────────
        Animation logoAnim  = AnimationUtils.loadAnimation(this, R.anim.splash_logo);
        Animation textAnim  = AnimationUtils.loadAnimation(this, R.anim.splash_text);
        Animation glowAnim  = AnimationUtils.loadAnimation(this, R.anim.glow_pulse);

        // ── Animations Start Karo ─────────────────────────
        glowOuter.startAnimation(glowAnim);
        glowInner.startAnimation(glowAnim);
        logoContainer.startAnimation(logoAnim);

        // Text animation thodi der baad
        handler.postDelayed(() -> {
            tvName.startAnimation(textAnim);
            tvTagline.startAnimation(textAnim);
        }, 400);

        // ── Progress Bar Animation ────────────────────────
        startProgressAnimation();
    }

    private void startProgressAnimation() {
        // Har 60ms pe progress update hoga (3 seconds total)
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress <= 100) {
                    progressBar.setProgress(progress);

                    // Status message update karo
                    int msgIndex = (progress / 20);
                    if (msgIndex < statusMessages.length) {
                        tvStatus.setText(statusMessages[msgIndex]);
                    }

                    progress += 2;
                    handler.postDelayed(this, 50);
                } else {
                    // Progress complete — MainActivity pe jao!
                    goToMain();
                }
            }
        };

        // 500ms baad start karo (logo animation ke baad)
        handler.postDelayed(progressRunnable, 500);
    }

    private void goToMain() {
        // Fade out animation
        View rootView = findViewById(android.R.id.content);
        Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeOut.setDuration(400);
        rootView.startAnimation(fadeOut);

        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            // Smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 400);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
