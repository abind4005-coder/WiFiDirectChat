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

    public SocketServer(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setReuseAddress(true);
            Log.d(TAG, "Server Started on Port: " + PORT);

            while (running) {
                Socket client = serverSocket.accept();
                String senderIP = client.getInetAddress().getHostAddress();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream())
                );

                String message = reader.readLine();

                if (message != null && !message.isEmpty()) {
                    Log.d(TAG, "Received: " + message);
                    if (listener != null) {
                        listener.onMessageReceived(message, senderIP);
                    }
                }

                client.close();
            }

        } catch (IOException e) {
            if (running) {
                Log.e(TAG, "Server Error: " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Stop Error: " + e.getMessage());
        }
    }

    public interface MessageListener {
        void onMessageReceived(String message, String senderIP);
    }
}