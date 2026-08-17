package com.btech.wifidirectchat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileTransferService {

    public static final int FILE_PORT = 8899;
    private static final String TAG = "FileTransfer";

    public interface FileSendListener {
        void onSuccess();
        void onError(String error);
    }

    public static void sendFile(Context context, String destIP, Uri fileUri, FileSendListener listener) {
        new Thread(() -> {
            try {
                // Get filename from URI
                String fileName = getFileName(context, fileUri);

                // Open InputStream from URI (works for ALL URI types including content://)
                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    listener.onError("Cannot open file!");
                    return;
                }

                Socket socket = new Socket(destIP, FILE_PORT);
                OutputStream outputStream = socket.getOutputStream();

                // Send filename length + filename first
                byte[] fileNameBytes = fileName.getBytes("UTF-8");
                outputStream.write(fileNameBytes.length);
                outputStream.write(fileNameBytes);

                // Send file bytes
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();
                socket.close();

                Log.d(TAG, "File sent successfully: " + fileName);
                listener.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "Send Error: " + e.getMessage());
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return (result != null) ? result : "Received_File_" + System.currentTimeMillis();
    }
}