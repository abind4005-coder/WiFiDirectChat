package com.btech.wifidirectchat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button   btnDiscover;
    private ListView listPeers;
    private TextView tvStatus, tvRouterStatus, tvUserProfile, tvPeerCount;
    private View     radarRingOuter, radarRingInner;
    private Switch   switchRouter;

    private WiFiDirectManager    wifiManager;
    private WifiP2pManager       p2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver    receiver;
    private IntentFilter         intentFilter;

    private final List<WifiP2pDevice> peers       = new ArrayList<>();
    private       ArrayAdapter<String> adapter;
    private final List<String>        deviceNames = new ArrayList<>();

    private static final int PERMISSION_CODE = 100;
    private boolean  isDialogShowing = false;
    private Animation pulseAnim;
    private String   userProfileName = "User";

    // Firebase
    private FirebaseAnalytics  firebaseAnalytics;
    private DatabaseReference  dbReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Normal screen layout (Fix: ensures status bar, clock, notch and battery never cut into content)
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
            );
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase Initialize
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        dbReference = FirebaseDatabase.getInstance().getReference("DMS_Users");

        btnDiscover    = findViewById(R.id.btn_discover);
        listPeers      = findViewById(R.id.list_peers);
        tvStatus       = findViewById(R.id.tv_status);
        tvRouterStatus = findViewById(R.id.tv_router_status);
        switchRouter   = findViewById(R.id.switch_router);
        tvUserProfile  = findViewById(R.id.tv_user_profile);
        tvPeerCount    = findViewById(R.id.tv_peer_count);
        radarRingOuter = findViewById(R.id.radar_ring_outer);
        radarRingInner = findViewById(R.id.radar_ring_inner);

        pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);

        Animation glowAnim = AnimationUtils.loadAnimation(this, R.anim.glow_pulse);
        if (radarRingOuter != null) radarRingOuter.startAnimation(glowAnim);
        if (radarRingInner != null) radarRingInner.startAnimation(glowAnim);

        checkAndPromptUniqueUserProfile();

        wifiManager = new WiFiDirectManager(this);
        p2pManager  = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (p2pManager != null) {
            channel = p2pManager.initialize(this, getMainLooper(), null);
        }

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, deviceNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.parseColor("#00F2FE"));
                tv.setTextSize(15);
                tv.setPadding(12, 12, 12, 12);
                return view;
            }
        };
        listPeers.setAdapter(adapter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        switchRouter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent serviceIntent = new Intent(this, BackgroundRouterService.class);
            if (isChecked) {
                tvRouterStatus.setText("Router Mode: 🟢 ACTIVE");
                tvRouterStatus.setTextColor(0xFF00FF87);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                tvRouterStatus.setText("Router Mode: OFF");
                tvRouterStatus.setTextColor(0xFF8B949E);
                stopService(serviceIntent);
            }
        });

        btnDiscover.setOnClickListener(v -> {
            WifiManager wm = (WifiManager)
                    getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null && !wm.isWifiEnabled()) {
                Toast.makeText(this, "WiFi ON karo pehle!", Toast.LENGTH_LONG).show();
                return;
            }
            if (!isLocationEnabled()) {
                Toast.makeText(this, "Location ON karo!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }
            btnDiscover.setText("📡\nSCANNING");
            btnDiscover.startAnimation(pulseAnim);
            tvStatus.setText("🔍 Scanning for nearby peers...");
            checkPermissionAndDiscover();
        });

        listPeers.setOnItemClickListener((parent, view, position, id) -> {
            if (position < peers.size()) {
                WifiP2pDevice device      = peers.get(position);
                String        displayName = deviceNames.get(position);
                new AlertDialog.Builder(this)
                        .setTitle("Connect Confirmation")
                        .setMessage("Kya aap " + displayName + " se connect hona chahte hain?")
                        .setPositiveButton("Connect", (dialog, which) -> {
                            wifiManager.connect(device);
                            tvStatus.setText("Connecting to: " + displayName);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    // ─── Unique Profile Setup ──────────────────────────────────────
    private void checkAndPromptUniqueUserProfile() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        userProfileName = prefs.getString("username", null);

        if (userProfileName == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("👤 Create Unique Profile");
            builder.setMessage("Apna naam set karein (ek baar set hone ke baad change nahi hoga):");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Aapka Naam");
            builder.setView(input);

            builder.setPositiveButton("Save & Lock", (dialog, which) -> {
                String raw = input.getText().toString().trim();
                if (!raw.isEmpty()) {
                    int randomTag   = 1000 + new Random().nextInt(9000);
                    userProfileName = raw + "#" + randomTag;
                } else {
                    userProfileName = "User_" + Build.MODEL + "#"
                            + (1000 + new Random().nextInt(9000));
                }
                prefs.edit().putString("username", userProfileName).apply();
                updateProfileBadgeUI();
                Toast.makeText(this, "Profile Locked: " + userProfileName,
                        Toast.LENGTH_LONG).show();

                // ✅ Save to Firebase
                saveUserToFirebase(userProfileName);
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            // Already existing user — update last seen
            updateProfileBadgeUI();
            updateLastSeen(userProfileName);
        }
    }

    private void updateProfileBadgeUI() {
        if (tvUserProfile != null && userProfileName != null) {
            tvUserProfile.setText("👤 " + userProfileName + " 🟢");
        }
    }

    // ─── Firebase: Save New User ───────────────────────────────────
    private void saveUserToFirebase(String profileName) {
        try {
            String userId  = profileName.replace("#", "_").replace(" ", "_");
            String timeNow = new SimpleDateFormat(
                    "dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            Map<String, Object> userData = new HashMap<>();
            userData.put("profileName",  profileName);
            userData.put("deviceModel",  Build.MODEL);
            userData.put("deviceBrand",  Build.BRAND);
            userData.put("androidVersion", Build.VERSION.RELEASE);
            userData.put("firstSeen",    timeNow);
            userData.put("lastSeen",     timeNow);
            userData.put("appVersion",   "1.0");
            userData.put("status",       "Active");

            dbReference.child(userId).setValue(userData)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Profile Registered! ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        // Offline hai to ignore karein
                    });

            // Firebase Analytics Event
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putString("profile_name", profileName);
            bundle.putString("device_model", Build.MODEL);
            firebaseAnalytics.logEvent("new_user_registered", bundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Firebase: Update Last Seen ────────────────────────────────
    private void updateLastSeen(String profileName) {
        try {
            String userId  = profileName.replace("#", "_").replace(" ", "_");
            String timeNow = new SimpleDateFormat(
                    "dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            dbReference.child(userId).child("lastSeen").setValue(timeNow);
            dbReference.child(userId).child("status").setValue("Active");

            // Analytics: App Opened Event
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putString("profile_name", profileName);
            firebaseAnalytics.logEvent("app_opened", bundle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void checkPermissionAndDiscover() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.NEARBY_WIFI_DEVICES,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_CODE);
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_CODE);
                return;
            }
        }
        wifiManager.discoverPeers();
        new Handler(Looper.getMainLooper()).postDelayed(this::updatePeerList, 3000);
    }

    public void updatePeerList() {
        wifiManager.getPeers(peerList -> {
            btnDiscover.clearAnimation();
            btnDiscover.setText("📡\nSCAN");
            if (peerList != null && peerList.getDeviceList() != null) {
                peers.clear();
                deviceNames.clear();
                peers.addAll(peerList.getDeviceList());
                for (WifiP2pDevice device : peers) {
                    String name = (device.deviceName != null
                            && !device.deviceName.isEmpty())
                            ? device.deviceName : "Nearby User";
                    deviceNames.add("👤 " + name + " 🟢 [Strong]");
                }
                adapter.notifyDataSetChanged();
                if (tvPeerCount != null) {
                    tvPeerCount.setText(peers.size() + " Found");
                }
                tvStatus.setText(peers.isEmpty()
                        ? "No devices found. Tap radar to retry."
                        : peers.size() + " device(s) found! Tap to connect");
            }
        });
    }

    public void onWifiDirectEnabled()  { tvStatus.setText("WiFi Direct Ready! ✅"); }
    public void onWifiDirectDisabled() { tvStatus.setText("WiFi Direct OFF! ❌"); }

    public void onDeviceConnected() {
        if (isDialogShowing) return;
        wifiManager.getInfo(info -> {
            if (info != null && info.groupFormed) {
                isDialogShowing = true;
                String ownerIP = (info.groupOwnerAddress != null)
                        ? info.groupOwnerAddress.getHostAddress() : "192.168.49.1";

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("📲 Connection Request")
                        .setMessage("Ek device connect hona chahta hai. Accept karein?")
                        .setCancelable(false)
                        .setPositiveButton("ACCEPT", (dialog, which) -> {
                            isDialogShowing = false;
                            tvStatus.setText("Connected! ✅");
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("groupOwnerIP",  ownerIP);
                            intent.putExtra("isGroupOwner",  info.isGroupOwner);
                            intent.putExtra("peerName",      userProfileName);
                            startActivity(intent);
                        })
                        .setNegativeButton("DECLINE", (dialog, which) -> {
                            isDialogShowing = false;
                            wifiManager.disconnect();
                            tvStatus.setText("Declined ❌");
                        })
                        .show();
            }
        });
    }

    public void onDeviceDisconnected() { tvStatus.setText("Disconnected"); }

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
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }
}