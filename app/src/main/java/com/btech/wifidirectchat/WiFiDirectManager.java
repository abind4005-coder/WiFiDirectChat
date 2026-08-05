package com.btech.wifidirectchat;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.WpsInfo;
import android.util.Log;

public class WiFiDirectManager {

    private static final String TAG = "WiFiDirect";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    public WiFiDirectManager(Context context) {
        manager = (WifiP2pManager) context
                .getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(
                context,
                context.getMainLooper(),
                null
        );
        Log.d(TAG, "WiFi Direct Ready!");
    }

    // Nearby phones dhundho
    public void discoverPeers() {
        try {
            manager.discoverPeers(channel,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Discovery shuru!");
                        }
                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Discovery fail: " + reason);
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission nahi hai: " + e.getMessage());
        }
    }

    // Mili hui devices ki list lo
    public void getPeers(WifiP2pManager.PeerListListener listener) {
        try {
            manager.requestPeers(channel, listener);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission nahi hai: " + e.getMessage());
        }
    }

    // Kisi device se connect karo
    public void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        try {
            manager.connect(channel, config,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Connection request bheja!");
                        }
                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Connect fail: " + reason);
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission nahi hai: " + e.getMessage());
        }
    }

    // Connection info lo
    public void getInfo(WifiP2pManager.ConnectionInfoListener listener) {
        manager.requestConnectionInfo(channel, listener);
    }

    // Disconnect karo
    public void disconnect() {
        manager.removeGroup(channel, null);
        Log.d(TAG, "Disconnected!");
    }
}