package com.btech.wifidirectchat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * VoiceCallManager — 2-Way Full-Duplex Real-Time Voice Engine
 * Single bidirectional TCP socket on port 9050.
 * AudioRecord (Mic) -> Socket OutputStream
 * Socket InputStream -> AudioTrack (Speaker)
 */
public class VoiceCallManager {

    private static final String TAG = "VoiceCall";
    public static final int CALL_PORT = 9050;

    // Audio format settings (16kHz PCM 16-bit Mono: clean, low latency over WiFi)
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_IN  = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING    = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private AudioTrack  audioTrack;

    private Socket       activeSocket;
    private ServerSocket serverSocket;

    private Thread workerThread;
    private Thread sendThread;
    private Thread receiveThread;

    private volatile boolean isRunning = false;
    private volatile boolean isMuted   = false;

    private CallListener listener;

    public interface CallListener {
        void onCallConnected();
        void onCallEnded();
        void onCallError(String message);
    }

    public VoiceCallManager(CallListener listener) {
        this.listener = listener;
    }

    // ─── START CALL (Caller Side) ──────────────────────────────────
    public void startCall(String peerIP) {
        isRunning = true;
        workerThread = new Thread(() -> {
            Socket socket = null;
            int attempts = 0;
            // Retry connecting while receiver initializes
            while (isRunning && attempts < 15) {
                try {
                    attempts++;
                    Log.d(TAG, "Connecting to " + peerIP + ":" + CALL_PORT + " (Attempt " + attempts + ")");
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(peerIP, CALL_PORT), 1500);
                    if (socket.isConnected()) {
                        Log.d(TAG, "Connected to receiver successfully!");
                        break;
                    }
                } catch (Exception e) {
                    try { if (socket != null) socket.close(); } catch (Exception ignored) {}
                    socket = null;
                    try { Thread.sleep(600); } catch (InterruptedException ie) { break; }
                }
            }

            if (socket != null && socket.isConnected() && isRunning) {
                activeSocket = socket;
                setupDuplexAudio(socket);
            } else if (isRunning) {
                Log.e(TAG, "Failed to connect after attempts");
                if (listener != null) listener.onCallError("Peer not responding / connection failed");
            }
        });
        workerThread.start();
    }

    // ─── RECEIVE CALL (Receiver Side) ──────────────────────────────
    public void receiveCall() {
        isRunning = true;
        workerThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(CALL_PORT);
                serverSocket.setReuseAddress(true);
                Log.d(TAG, "Receiver listening on port " + CALL_PORT);

                Socket socket = serverSocket.accept();
                Log.d(TAG, "Caller accepted from " + socket.getInetAddress());

                if (isRunning) {
                    activeSocket = socket;
                    setupDuplexAudio(socket);
                }
            } catch (Exception e) {
                if (isRunning) {
                    Log.e(TAG, "Receiver server error: " + e.getMessage());
                    if (listener != null) listener.onCallError("Incoming call error: " + e.getMessage());
                }
            }
        });
        workerThread.start();
    }

    // ─── SETUP DUPLEX AUDIO (Both Sides) ───────────────────────────
    private void setupDuplexAudio(Socket socket) {
        try {
            int minRecordBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING);
            int recordBufSize = Math.max(minRecordBuf, 2048);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE, CHANNEL_IN, ENCODING, recordBufSize);

            int minTrackBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING);
            int trackBufSize = Math.max(minTrackBuf, 2048);

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE, CHANNEL_OUT, ENCODING,
                    trackBufSize, AudioTrack.MODE_STREAM);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord init failed!");
                if (listener != null) listener.onCallError("Mic initialization failed");
                return;
            }

            audioTrack.play();
            audioRecord.startRecording();

            if (listener != null) listener.onCallConnected();

            // 1. Thread: Mic -> OutputStream
            final OutputStream out = socket.getOutputStream();
            sendThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (isRunning && !socket.isClosed()) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            if (isMuted) {
                                byte[] silence = new byte[read];
                                out.write(silence, 0, read);
                            } else {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    if (isRunning) Log.e(TAG, "Send audio loop error: " + e.getMessage());
                }
            });
            sendThread.start();

            // 2. Thread: InputStream -> AudioTrack
            final InputStream in = socket.getInputStream();
            receiveThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (isRunning && !socket.isClosed()) {
                        int read = in.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            audioTrack.write(buffer, 0, read);
                        } else if (read == -1) {
                            // Peer disconnected
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (isRunning) Log.e(TAG, "Receive audio loop error: " + e.getMessage());
                }
                if (isRunning) {
                    endCall();
                }
            });
            receiveThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Setup audio error: " + e.getMessage());
            if (listener != null) listener.onCallError("Audio error: " + e.getMessage());
        }
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void endCall() {
        if (!isRunning) return;
        isRunning = false;

        try {
            if (audioRecord != null) {
                try { audioRecord.stop(); } catch (Exception ignored) {}
                audioRecord.release();
                audioRecord = null;
            }
            if (audioTrack != null) {
                try { audioTrack.stop(); } catch (Exception ignored) {}
                audioTrack.release();
                audioTrack = null;
            }
            if (activeSocket != null) {
                try { activeSocket.close(); } catch (Exception ignored) {}
                activeSocket = null;
            }
            if (serverSocket != null) {
                try { serverSocket.close(); } catch (Exception ignored) {}
                serverSocket = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up call: " + e.getMessage());
        }

        if (sendThread != null) sendThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();
        if (workerThread != null) workerThread.interrupt();

        if (listener != null) listener.onCallEnded();
    }
}
