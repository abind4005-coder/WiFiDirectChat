package com.btech.wifidirectchat;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;

public class AudioRecorderHelper {

    private static final String TAG = "AudioHelper";
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File audioFile;

    public void startRecording(Context context) {
        try {
            audioFile = new File(context.getCacheDir(), "VoiceNote_" + System.currentTimeMillis() + ".m4a");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Audio Recording Started...");
        } catch (Exception e) {
            Log.e(TAG, "Recording Error: " + e.getMessage());
        }
    }

    public File stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            return audioFile;
        } catch (Exception e) {
            Log.e(TAG, "Stop Record Error: " + e.getMessage());
            return null;
        }
    }

    public void playAudio(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Playback Error: " + e.getMessage());
        }
    }
}