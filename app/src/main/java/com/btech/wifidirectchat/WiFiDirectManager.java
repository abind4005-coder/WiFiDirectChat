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
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager != null ? manager.initialize(context, context.getMainLooper(), null) : null;
        Log.d(TAG, "WiFi Direct Ready!");
    }

    public void discoverPeers() {
        if (manager == null || channel == null) return;

        // Pehle purani memory clear karo taaki re-connect ho sake
        disconnect();

        try {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discovery started!");
                }
                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Discovery failed: " + reason);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing: " + e.getMessage());
        }
    }

    public void getPeers(WifiP2pManager.PeerListListener listener) {
        if (manager == null || channel == null) return;
        try {
            manager.requestPeers(channel, listener);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing: " + e.getMessage());
        }
    }

    public void connect(WifiP2pDevice device) {
        if (manager == null || channel == null) return;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        try {
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection request sent!");
                }
                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Connect failed: " + reason);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing: " + e.getMessage());
        }
    }

    public void getInfo(WifiP2pManager.ConnectionInfoListener listener) {
        if (manager == null || channel == null) return;
        manager.requestConnectionInfo(channel, listener);
    }

    // Forcefully Purane Connections Remove Karein
    public void disconnect() {
        if (manager == null || channel == null) return;

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Group removed / Memory cleared!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "No active group to remove.");
            }
        });
    }
}