package com.btech.wifidirectchat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * VoiceCallManager — Real-time 2-way voice call engine
 * Uses AudioRecord (mic capture) + AudioTrack (speaker play)
 * over TCP sockets for full-duplex communication.
 */
public class VoiceCallManager {

    private static final String TAG = "VoiceCall";

    // Ports for sending/receiving audio
    public static final int SEND_PORT    = 9000; // Caller → Receiver
    public static final int RECEIVE_PORT = 9001; // Receiver → Caller

    // Audio settings (CD quality, mono)
    private static final int SAMPLE_RATE   = 44100;
    private static final int CHANNEL_IN    = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_OUT   = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING      = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private AudioTrack  audioTrack;

    private Socket       sendSocket;
    private Socket       receiveSocket;
    private ServerSocket serverSocket;

    private Thread sendThread;
    private Thread receiveThread;
    private Thread serverThread;

    private boolean isRunning = false;
    private boolean isMuted   = false;

    private CallListener listener;

    public interface CallListener {
        void onCallConnected();
        void onCallEnded();
        void onCallError(String message);
    }

    public VoiceCallManager(CallListener listener) {
        this.listener = listener;
    }

    // ─── START CALL (Caller side) ──────────────────────────────────
    public void startCall(String receiverIP) {
        isRunning = true;

        new Thread(() -> {
            try {
                // Connect to receiver's SEND_PORT (to send audio)
                Thread.sleep(1000); // Give receiver time to start listening
                sendSocket = new Socket(receiverIP, SEND_PORT);
                Log.d(TAG, "Send socket connected to " + receiverIP);

                // Start sending our mic audio
                startSendingAudio(sendSocket.getOutputStream());

                if (listener != null) listener.onCallConnected();

            } catch (Exception e) {
                Log.e(TAG, "Send error: " + e.getMessage());
                if (listener != null) listener.onCallError("Could not connect to peer");
            }
        }).start();

        // Also listen for incoming audio from receiver
        startReceivingAudio(RECEIVE_PORT);
    }

    // ─── RECEIVE CALL (Receiver side) ─────────────────────────────
    public void receiveCall() {
        isRunning = true;

        // Listen for caller's audio on SEND_PORT
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SEND_PORT);
                Log.d(TAG, "Waiting for caller audio on port " + SEND_PORT);
                Socket incomingSocket = serverSocket.accept();
                Log.d(TAG, "Caller connected!");

                // Play incoming audio
                playIncomingAudio(incomingSocket.getInputStream());

                if (listener != null) listener.onCallConnected();

            } catch (Exception e) {
                if (isRunning) {
                    Log.e(TAG, "Receive server error: " + e.getMessage());
                }
            }
        });
        serverThread.start();

        // Also send our own mic audio back to caller on RECEIVE_PORT
        // (Caller's IP is needed — passed from ChatActivity)
    }

    // ─── Send mic audio to peer ───────────────────────────────────
    private void startSendingAudio(OutputStream out) {
        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_IN, ENCODING);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_IN, ENCODING, bufferSize);

        audioRecord.startRecording();

        sendThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            try {
                while (isRunning) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0 && !isMuted) {
                        out.write(buffer, 0, read);
                        out.flush();
                    } else if (isMuted) {
                        // Send silence when muted
                        byte[] silence = new byte[read];
                        out.write(silence, 0, read);
                        out.flush();
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Send thread error: " + e.getMessage());
            }
        });
        sendThread.start();
        Log.d(TAG, "Audio sending started");
    }

    // ─── Play incoming audio from peer ────────────────────────────
    private void playIncomingAudio(InputStream in) {
        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_OUT, ENCODING);

        audioTrack = new AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                SAMPLE_RATE, CHANNEL_OUT, ENCODING,
                bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();

        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            try {
                while (isRunning) {
                    int read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        audioTrack.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Play thread error: " + e.getMessage());
            }
        });
        receiveThread.start();
        Log.d(TAG, "Audio playback started");
    }

    // ─── Start receiving audio on given port ──────────────────────
    private void startReceivingAudio(int port) {
        new Thread(() -> {
            try {
                ServerSocket srv = new ServerSocket(port);
                Socket sock = srv.accept();
                playIncomingAudio(sock.getInputStream());
            } catch (Exception e) {
                if (isRunning) Log.e(TAG, "Receive error: " + e.getMessage());
            }
        }).start();
    }

    // ─── Mute / Unmute ────────────────────────────────────────────
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        Log.d(TAG, "Muted: " + muted);
    }

    public boolean isMuted() {
        return isMuted;
    }

    // ─── End Call ─────────────────────────────────────────────────
    public void endCall() {
        isRunning = false;
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            if (sendSocket    != null) sendSocket.close();
            if (receiveSocket != null) receiveSocket.close();
            if (serverSocket  != null) serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "End call error: " + e.getMessage());
        }

        if (sendThread    != null) sendThread.interrupt();
        if (receiveThread != null) receiveThread.interrupt();
        if (serverThread  != null) serverThread.interrupt();

        if (listener != null) listener.onCallEnded();
        Log.d(TAG, "Call ended");
    }
}
