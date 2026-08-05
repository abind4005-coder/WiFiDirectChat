package com.btech.wifidirectchat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private Button btnDiscover;
    private ListView listPeers;
    private TextView tvStatus;

    // WiFi Direct
    private WiFiDirectManager wifiManager;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    // Devices list
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> deviceNames = new ArrayList<>();

    // Permission request code
    private static final int PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI connect karo
        btnDiscover = findViewById(R.id.btn_discover);
        listPeers   = findViewById(R.id.list_peers);
        tvStatus    = findViewById(R.id.tv_status);

        // WiFiDirectManager banao
        wifiManager = new WiFiDirectManager(this);

        // P2P Manager aur Channel lo
        p2pManager = (WifiP2pManager) getSystemService(
                Context.WIFI_P2P_SERVICE);
        channel = p2pManager.initialize(this, getMainLooper(), null);

        // ListView ke liye adapter
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceNames);
        listPeers.setAdapter(adapter);

        // IntentFilter — WiFi events sunne ke liye
        intentFilter = new IntentFilter();
        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Discover button click
        btnDiscover.setOnClickListener(v -> {
            tvStatus.setText("Dhoondh raha hoon...");
            checkPermissionAndDiscover();
        });

        // Device pe click karo → Connect
        listPeers.setOnItemClickListener((parent, view, position, id) -> {
            WifiP2pDevice device = peers.get(position);
            wifiManager.connect(device);
            tvStatus.setText("Connect ho raha hai: "
                    + device.deviceName);
        });
    }

    // Permission check karo pehle
    private void checkPermissionAndDiscover() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission maango user se
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                    },
                    PERMISSION_CODE);
        } else {
            // Permission hai — discover shuru karo
            wifiManager.discoverPeers();
            updatePeerList();
        }
    }

    // Permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                wifiManager.discoverPeers();
                updatePeerList();
            } else {
                Toast.makeText(this,
                        "Location permission chahiye WiFi Direct ke liye!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // Peer list update karo
    public void updatePeerList() {
        wifiManager.getPeers(peerList -> {
            peers.clear();
            deviceNames.clear();
            peers.addAll(peerList.getDeviceList());
            for (WifiP2pDevice device : peers) {
                deviceNames.add("📱 " + device.deviceName);
            }
            adapter.notifyDataSetChanged();
            tvStatus.setText(peers.size() + " device mila!");
        });
    }

    // WiFi Direct events ke liye
    public void onWifiDirectEnabled() {
        tvStatus.setText("WiFi Direct Ready! ✅");
    }

    public void onWifiDirectDisabled() {
        tvStatus.setText("WiFi Direct band hai! ❌");
    }

    public void onDeviceConnected() {
        tvStatus.setText("Connected! ✅");
        wifiManager.getInfo(info -> {
            if (info.groupFormed) {
                Toast.makeText(this,
                        "Connected! IP: " + info.groupOwnerAddress,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onDeviceDisconnected() {
        tvStatus.setText("Disconnected");
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(
                p2pManager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}