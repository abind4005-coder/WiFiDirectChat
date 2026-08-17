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
            // Save in app's internal cache dir (No permission needed!)
            File cacheDir = context.getCacheDir();
            audioFile = new File(cacheDir, "VoiceNote_" + System.currentTimeMillis() + ".m4a");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Recording started: " + audioFile.getAbsolutePath());
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
                Log.d(TAG, "Recording stopped. File: " + audioFile.getAbsolutePath());
            }
            return audioFile;
        } catch (Exception e) {
            Log.e(TAG, "Stop Error: " + e.getMessage());
            return null;
        }
    }

    public void playAudio(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "Playing audio: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Playback Error: " + e.getMessage());
        }
    }

    public void release() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}