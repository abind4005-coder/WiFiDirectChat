package com.btech.wifidirectchat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnDiscover;
    private ListView listPeers;
    private TextView tvStatus, tvRouterStatus;
    private Switch switchRouter;

    private WiFiDirectManager wifiManager;
    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> deviceNames = new ArrayList<>();

    private static final int PERMISSION_CODE = 100;
    private boolean isDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDiscover     = findViewById(R.id.btn_discover);
        listPeers       = findViewById(R.id.list_peers);
        tvStatus        = findViewById(R.id.tv_status);
        tvRouterStatus  = findViewById(R.id.tv_router_status);
        switchRouter    = findViewById(R.id.switch_router);

        wifiManager = new WiFiDirectManager(this);

        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2pManager != null) {
            channel = p2pManager.initialize(this, getMainLooper(), null);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listPeers.setAdapter(adapter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        switchRouter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent serviceIntent = new Intent(this, BackgroundRouterService.class);
            if (isChecked) {
                tvRouterStatus.setText("Router Mode: 🟢 ACTIVE (Relay Bridge)");
                tvRouterStatus.setTextColor(0xFF3FB950);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "Router Mode Enabled! Background Relay Active.", Toast.LENGTH_SHORT).show();
            } else {
                tvRouterStatus.setText("Router Mode: OFF");
                tvRouterStatus.setTextColor(0xFF8B949E);
                stopService(serviceIntent);
                Toast.makeText(this, "Router Mode Disabled.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDiscover.setOnClickListener(v -> {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && !wm.isWifiEnabled()) {
                Toast.makeText(this, "WiFi ON karo pehle!", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isLocationEnabled()) {
                Toast.makeText(this, "Location ON karo Settings me!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }

            tvStatus.setText("Dhoondh raha hoon... / Searching...");
            checkPermissionAndDiscover();
        });

        listPeers.setOnItemClickListener((parent, view, position, id) -> {
            if (position < peers.size()) {
                WifiP2pDevice device = peers.get(position);

                new AlertDialog.Builder(this)
                        .setTitle("Connect Confirmation")
                        .setMessage("Kya aap " + device.deviceName + " se connect hona chahte hain?")
                        .setPositiveButton("Connect", (dialog, which) -> {
                            wifiManager.connect(device);
                            tvStatus.setText("Connection request sent to: " + device.deviceName);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void checkPermissionAndDiscover() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                return;
            }
        }

        wifiManager.discoverPeers();
        new Handler(Looper.getMainLooper()).postDelayed(this::updatePeerList, 3000);
    }

    public void updatePeerList() {
        wifiManager.getPeers(peerList -> {
            if (peerList != null && peerList.getDeviceList() != null) {
                peers.clear();
                deviceNames.clear();
                peers.addAll(peerList.getDeviceList());
                for (WifiP2pDevice device : peers) {
                    deviceNames.add("📱 " + device.deviceName);
                }
                adapter.notifyDataSetChanged();
                if (peers.isEmpty()) {
                    tvStatus.setText("No devices found");
                } else {
                    tvStatus.setText(peers.size() + " device(s) found!");
                }
            }
        });
    }

    public void onWifiDirectEnabled() {
        tvStatus.setText("WiFi Direct Ready! ✅");
    }

    public void onWifiDirectDisabled() {
        tvStatus.setText("WiFi Direct OFF! ❌");
    }

    public void onDeviceConnected() {
        if (isDialogShowing) return;

        wifiManager.getInfo(info -> {
            if (info != null && info.groupFormed) {
                isDialogShowing = true;

                String ownerIP = (info.groupOwnerAddress != null) ? info.groupOwnerAddress.getHostAddress() : "192.168.49.1";

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("📲 Connection Request")
                        .setMessage("Dusra device aapke sath connect hona chahta hai. Kya aap Accept karte hain?")
                        .setCancelable(false)
                        .setPositiveButton("ACCEPT", (dialog, which) -> {
                            isDialogShowing = false;
                            tvStatus.setText("Connected! ✅");

                            try {
                                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                                intent.putExtra("groupOwnerIP", ownerIP);
                                intent.putExtra("isGroupOwner", info.isGroupOwner);
                                intent.putExtra("peerName", "Peer Device");
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Error starting chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("DECLINE", (dialog, which) -> {
                            isDialogShowing = false;
                            wifiManager.disconnect();
                            tvStatus.setText("Connection Declined ❌");
                        })
                        .show();
            }
        });
    }

    public void onDeviceDisconnected() {
        tvStatus.setText("Disconnected");
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(p2pManager, channel, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {}
        }
    }
}