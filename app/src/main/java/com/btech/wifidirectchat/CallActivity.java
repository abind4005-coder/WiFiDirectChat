package com.btech.wifidirectchat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class CallActivity extends AppCompatActivity
        implements VoiceCallManager.CallListener {

    private static final int PERM_RECORD_AUDIO = 501;

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

        isCaller = getIntent().getBooleanExtra("isCaller", true);
        peerName = getIntent().getStringExtra("peerName");
        peerIP   = getIntent().getStringExtra("peerIP");

        tvCallerName   = findViewById(R.id.tv_caller_name);
        tvStatus       = findViewById(R.id.tv_call_status);
        tvDuration     = findViewById(R.id.tv_duration);
        tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        tvMuteIcon     = findViewById(R.id.iv_mute);
        tvSpeakerIcon  = findViewById(R.id.iv_speaker);
        btnMute        = findViewById(R.id.btn_mute);
        btnEndCall     = findViewById(R.id.btn_end_call);
        btnSpeaker     = findViewById(R.id.btn_speaker);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }

        tvCallerName.setText(peerName != null ? peerName : "Peer Device");
        if (peerName != null && !peerName.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(peerName.charAt(0)).toUpperCase());
        }

        voiceCallManager = new VoiceCallManager(this);

        btnMute.setOnClickListener(v -> {
            boolean muted = !voiceCallManager.isMuted();
            voiceCallManager.setMuted(muted);
            tvMuteIcon.setText(muted ? "🔇" : "🎤");
            btnMute.setAlpha(muted ? 0.5f : 1.0f);
            Toast.makeText(this, muted ? "Mic Muted 🔇" : "Mic ON 🎤", Toast.LENGTH_SHORT).show();
        });

        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOn = !isSpeakerOn;
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(isSpeakerOn);
            }
            tvSpeakerIcon.setText(isSpeakerOn ? "🔈" : "🔊");
            btnSpeaker.setAlpha(isSpeakerOn ? 1.0f : 0.6f);
            Toast.makeText(this, isSpeakerOn ? "Speaker ON 🔊" : "Speaker OFF", Toast.LENGTH_SHORT).show();
        });

        btnEndCall.setOnClickListener(v -> endCall());

        // Check mic permission first
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERM_RECORD_AUDIO);
        } else {
            initiateCallEngine();
        }
    }

    private void initiateCallEngine() {
        if (isCaller) {
            tvStatus.setText("📞 Calling " + (peerName != null ? peerName : "") + "...");
            voiceCallManager.startCall(peerIP);
        } else {
            tvStatus.setText("📲 Answering Call...");
            voiceCallManager.receiveCall();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateCallEngine();
            } else {
                Toast.makeText(this, "Microphone permission required for voice calls!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

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
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
        if (voiceCallManager != null) {
            voiceCallManager.endCall();
        }
        finish();
    }

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
            Toast.makeText(CallActivity.this, "Call Ended", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onCallError(String message) {
        runOnUiThread(() -> {
            tvStatus.setText("❌ " + message);
            Toast.makeText(CallActivity.this, message, Toast.LENGTH_LONG).show();
            timerHandler.postDelayed(this::finish, 2000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
        if (voiceCallManager != null) {
            voiceCallManager.endCall();
        }
    }

    @Override
    public void onBackPressed() {
        endCall();
    }
}
