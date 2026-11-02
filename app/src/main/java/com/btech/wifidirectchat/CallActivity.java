package com.btech.wifidirectchat;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity
        implements VoiceCallManager.CallListener {

    private TextView     tvCallerName, tvStatus, tvDuration, tvAvatarLetter, tvMuteIcon, tvSpeakerIcon;
    private LinearLayout btnMute, btnEndCall, btnSpeaker;

    private VoiceCallManager voiceCallManager;
    private Handler  timerHandler = new Handler(Looper.getMainLooper());
    private int      seconds      = 0;
    private boolean  isCaller;
    private boolean  isSpeakerOn  = false;
    private String   peerName;
    private String   peerIP;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        // Get data from intent
        isCaller = getIntent().getBooleanExtra("isCaller", true);
        peerName = getIntent().getStringExtra("peerName");
        peerIP   = getIntent().getStringExtra("peerIP");

        // Views
        tvCallerName   = findViewById(R.id.tv_caller_name);
        tvStatus       = findViewById(R.id.tv_call_status);
        tvDuration     = findViewById(R.id.tv_duration);
        tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        tvMuteIcon     = findViewById(R.id.iv_mute);
        tvSpeakerIcon  = findViewById(R.id.iv_speaker);
        btnMute        = findViewById(R.id.btn_mute);
        btnEndCall     = findViewById(R.id.btn_end_call);
        btnSpeaker     = findViewById(R.id.btn_speaker);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Set peer name and avatar
        tvCallerName.setText(peerName != null ? peerName : "Unknown");
        if (peerName != null && !peerName.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(peerName.charAt(0)).toUpperCase());
        }

        // Init voice call
        voiceCallManager = new VoiceCallManager(this);

        if (isCaller) {
            tvStatus.setText("📞 Calling " + peerName + "...");
            voiceCallManager.startCall(peerIP);
        } else {
            tvStatus.setText("📲 Incoming call from " + peerName);
            voiceCallManager.receiveCall();
        }

        // Mute Button
        btnMute.setOnClickListener(v -> {
            boolean muted = !voiceCallManager.isMuted();
            voiceCallManager.setMuted(muted);
            tvMuteIcon.setText(muted ? "🔇" : "🎤");
            btnMute.setAlpha(muted ? 0.5f : 1.0f);
            Toast.makeText(this, muted ? "Muted 🔇" : "Unmuted 🎤",
                    Toast.LENGTH_SHORT).show();
        });

        // Speaker Button
        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            audioManager.setSpeakerphoneOn(isSpeakerOn);
            tvSpeakerIcon.setText(isSpeakerOn ? "🔈" : "🔊");
            btnSpeaker.setAlpha(isSpeakerOn ? 1.0f : 0.6f);
            Toast.makeText(this, isSpeakerOn ? "Speaker ON 🔊" : "Speaker OFF",
                    Toast.LENGTH_SHORT).show();
        });

        // End Call Button
        btnEndCall.setOnClickListener(v -> {
            endCall();
        });
    }

    // ─── Timer: Count call duration ───────────────────────────────
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            seconds++;
            int mins = seconds / 60;
            int secs = seconds % 60;
            tvDuration.setText(String.format("%02d:%02d", mins, secs));
            timerHandler.postDelayed(this, 1000);
        }
    };

    private void startTimer() {
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void endCall() {
        stopTimer();
        if (voiceCallManager != null) voiceCallManager.endCall();
        finish();
    }

    // ─── CallListener Callbacks ───────────────────────────────────
    @Override
    public void onCallConnected() {
        runOnUiThread(() -> {
            tvStatus.setText("🟢 Connected");
            startTimer();
        });
    }

    @Override
    public void onCallEnded() {
        runOnUiThread(() -> {
            stopTimer();
            finish();
        });
    }

    @Override
    public void onCallError(String message) {
        runOnUiThread(() -> {
            tvStatus.setText("❌ " + message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            timerHandler.postDelayed(this::finish, 2000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (voiceCallManager != null) voiceCallManager.endCall();
    }

    @Override
    public void onBackPressed() {
        endCall();
    }
}
