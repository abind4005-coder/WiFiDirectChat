package com.btech.wifidirectchat;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.WpsInfo;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class WiFiDirectManager {

    private static final String TAG = "WiFiDirect";
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    public WiFiDirectManager(Context context) {
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager != null ? manager.initialize(context, context.getMainLooper(), null) : null;
        Log.d(TAG, "WiFi Direct Ready!");
    }

    // Broadcast Real User Profile Name to Nearby Devices
    public void startLocalProfileService(String userProfileName) {
        if (manager == null || channel == null) return;

        Map<String, String> record = new HashMap<>();
        record.put("user_profile_name", userProfileName);

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("MeshChatProfile", "_presence._tcp", record);

        try {
            manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Profile Service Broadcasted: " + userProfileName);
                        }
                        @Override
                        public void onFailure(int reason) {}
                    });
                }
                @Override
                public void onFailure(int reason) {}
            });
        } catch (SecurityException ignored) {}
    }

    public void discoverPeers() {
        if (manager == null || channel == null) return;
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

    public void disconnect() {
        if (manager == null || channel == null) return;
        manager.removeGroup(channel, null);
    }
}