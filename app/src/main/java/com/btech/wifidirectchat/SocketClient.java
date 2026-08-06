package com.btech.wifidirectchat;

import android.util.Log;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient extends Thread {

    private static final String TAG = "SocketClient";

    private String serverIP;
    private String message;
    private SendListener listener;

    // Constructor
    public SocketClient(String serverIP,
                        String message,
                        SendListener listener) {
        this.serverIP = serverIP;
        this.message  = message;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            // Server se connect karo
            Socket socket = new Socket();
            socket.connect(
                    new InetSocketAddress(serverIP,
                            SocketServer.PORT),
                    3000  // 3 second timeout
            );

            // Message bhejo
            PrintWriter writer = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream()
                            )
                    ), true
            );

            writer.println(message);
            socket.close();

            Log.d(TAG, "Message bheja: " + message);
            listener.onSendSuccess();

        } catch (IOException e) {
            Log.e(TAG, "Send error: " + e.getMessage());
            listener.onSendError(e.getMessage());
        }
    }

    // Result callbacks
    public interface SendListener {
        void onSendSuccess();
        void onSendError(String error);
    }
}