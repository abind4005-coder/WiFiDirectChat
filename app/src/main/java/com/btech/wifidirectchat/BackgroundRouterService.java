package com.btech.wifidirectchat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class BackgroundRouterService extends Service {

    private static final String TAG = "BackgroundRouter";
    private static final String CHANNEL_ID = "MeshRouterChannel";
    private SocketServer socketServer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📡 Mesh Router Active")
                .setContentText("Phone is acting as an Offline Relay Bridge")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
        startSilentRelayServer();
    }

    private void startSilentRelayServer() {
        socketServer = new SocketServer((rawPayload, senderIP) -> {
            String[] parts = rawPayload.split("\\|", 3);
            if (parts.length == 3) {
                String senderNode = parts[0];
                int hopCount = Integer.parseInt(parts[1]);
                String encryptedMsg = parts[2];

                Log.d(TAG, "Silent Router Relaying Packet from " + senderNode + " Hop: " + hopCount);

                if (hopCount < 4) {
                    relayPacketToMesh(senderNode, hopCount + 1, encryptedMsg);
                }
            }
        });
        socketServer.start();
    }

    private void relayPacketToMesh(String originalSender, int nextHop, String encryptedMsg) {
        String newPacket = originalSender + "|" + nextHop + "|" + encryptedMsg;

        new SocketClient("192.168.49.255", newPacket, new SocketClient.SendListener() {
            @Override
            public void onSendSuccess() {
                Log.d(TAG, "Silently Relayed Packet from " + originalSender);
            }
            @Override
            public void onSendError(String error) {}
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socketServer != null) {
            socketServer.stopServer();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mesh Router Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}