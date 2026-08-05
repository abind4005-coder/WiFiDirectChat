package com.btech.wifidirectchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final MainActivity activity;

    public WiFiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            MainActivity activity) {
        this.manager  = manager;
        this.channel  = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
                .equals(action)) {
            int state = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.onWifiDirectEnabled();
            } else {
                activity.onWifiDirectDisabled();
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                .equals(action)) {
            activity.updatePeerList();

        } else if (WifiP2pManager
                .WIFI_P2P_CONNECTION_CHANGED_ACTION
                .equals(action)) {
            if (intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_NETWORK_INFO) != null) {
                activity.onDeviceConnected();
            } else {
                activity.onDeviceDisconnected();
            }
        }
    }
}