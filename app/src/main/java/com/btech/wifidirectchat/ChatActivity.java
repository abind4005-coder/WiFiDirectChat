package com.btech.wifidirectchat;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvPeerName;

    private List<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String lastKnownPeerIP;
    private boolean isGroupOwner;
    private String peerName;
    private String myDeviceID;

    private SocketServer socketServer;
    private WiFiDirectManager wifiManager;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        wifiManager = new WiFiDirectManager(this);

        isGroupOwner = getIntent().getBooleanExtra("isGroupOwner", false);
        String groupOwnerIP = getIntent().getStringExtra("groupOwnerIP");
        peerName     = getIntent().getStringExtra("peerName");

        myDeviceID = android.os.Build.MODEL;

        if (!isGroupOwner && groupOwnerIP != null) {
            lastKnownPeerIP = groupOwnerIP;
        }

        listMessages = findViewById(R.id.list_messages);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        tvPeerName   = findViewById(R.id.tv_peer_name);

        if (peerName != null) {
            tvPeerName.setText("📱 " + peerName);
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                String item = getItem(position);
                if (item != null && item.startsWith("Aap / You:")) {
                    textView.setTextColor(Color.parseColor("#58A6FF"));
                } else if (item != null && item.startsWith("[RELAY]")) {
                    textView.setTextColor(Color.parseColor("#E3B341"));
                } else {
                    textView.setTextColor(Color.parseColor("#3FB950"));
                }
                textView.setTextSize(15);
                return view;
            }
        };
        listMessages.setAdapter(adapter);

        startMeshServer();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Message khali nahi ho sakta!", Toast.LENGTH_SHORT).show();
            return;
        }

        String encrypted = EncryptionHelper.encrypt(text);
        String meshPacket = myDeviceID + "|1|" + encrypted;
        String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";

        new SocketClient(destIP, meshPacket, new SocketClient.SendListener() {
            @Override
            public void onSendSuccess() {
                mainHandler.post(() -> {
                    messages.add("Aap / You: " + text);
                    adapter.notifyDataSetChanged();
                    listMessages.setSelection(messages.size() - 1);
                    etMessage.setText("");
                });
            }

            @Override
            public void onSendError(String error) {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "Send Error: WiFi Disconnected", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startMeshServer() {
        socketServer = new SocketServer((rawPayload, senderIP) -> {
            lastKnownPeerIP = senderIP;

            String[] parts = rawPayload.split("\\|", 3);
            if (parts.length == 3) {
                String senderNode = parts[0];
                int hopCount = Integer.parseInt(parts[1]);
                String encryptedMsg = parts[2];

                String decrypted = EncryptionHelper.decrypt(encryptedMsg);

                mainHandler.post(() -> {
                    if (hopCount > 1) {
                        messages.add("Dost / Friend (" + senderNode + " via Relay): " + decrypted);
                    } else {
                        messages.add("Dost / Friend: " + decrypted);
                    }
                    adapter.notifyDataSetChanged();
                    listMessages.setSelection(messages.size() - 1);
                });
            } else {
                // Fallback for plain messages
                String decrypted = EncryptionHelper.decrypt(rawPayload);
                mainHandler.post(() -> {
                    messages.add("Dost / Friend: " + decrypted);
                    adapter.notifyDataSetChanged();
                    listMessages.setSelection(messages.size() - 1);
                });
            }
        });
        socketServer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketServer != null) {
            socketServer.stopServer();
        }
        if (wifiManager != null) {
            wifiManager.disconnect();
        }
    }
}