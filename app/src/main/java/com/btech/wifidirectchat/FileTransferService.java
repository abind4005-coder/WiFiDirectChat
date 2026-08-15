package com.btech.wifidirectchat;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FileTransferService {

    private static final String TAG = "FileTransfer";
    public static final int FILE_PORT = 8899;

    public static void sendFile(Context context, String targetIP, Uri fileUri, FileSendListener listener) {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(targetIP, FILE_PORT), 5000);

                OutputStream outputStream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = cr.openInputStream(fileUri);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                if (inputStream != null) inputStream.close();
                socket.close();

                Log.d(TAG, "File Transfer Completed Successfully!");
                if (listener != null) listener.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "File Transfer Error: " + e.getMessage());
                if (listener != null) listener.onError(e.getMessage());
            }
        }).start();
    }

    public interface FileSendListener {
        void onSuccess();
        void onError(String error);
    }
}