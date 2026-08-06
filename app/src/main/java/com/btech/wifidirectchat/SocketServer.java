package com.btech.wifidirectchat;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    private static final String TAG = "SocketServer";
    public static final int PORT = 8888;

    private ServerSocket serverSocket;
    private boolean running = true;
    private MessageListener listener;

    // Constructor
    public SocketServer(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            // Port 8888 pe "sunna" shuru karo
            serverSocket = new ServerSocket(PORT);
            Log.d(TAG, "Server start hua! Port: " + PORT);

            while (running) {
                // Client ka wait karo
                Socket client = serverSocket.accept();
                Log.d(TAG, "Client connected: "
                        + client.getInetAddress());

                // Message padho
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                client.getInputStream()
                        )
                );

                String message = reader.readLine();

                if (message != null && !message.isEmpty()) {
                    Log.d(TAG, "Message aaya: " + message);
                    // UI thread pe message dikhao
                    listener.onMessageReceived(message);
                }

                client.close();
            }

        } catch (IOException e) {
            if (running) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }
    }

    // Server band karo
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Stop error: " + e.getMessage());
        }
    }

    // Message aane pe kya karo
    public interface MessageListener {
        void onMessageReceived(String message);
    }
}