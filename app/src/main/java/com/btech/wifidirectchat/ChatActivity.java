package com.btech.wifidirectchat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // UI Elements
    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvPeerName;

    // Data
    private List<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // Connection info
    private String groupOwnerIP;
    private boolean isGroupOwner;
    private String peerName;

    // Server (receive karne ke liye)
    private SocketServer socketServer;

    // UI update ke liye
    private Handler mainHandler = new Handler(
            Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Intent se data lo
        groupOwnerIP = getIntent()
                .getStringExtra("groupOwnerIP");
        isGroupOwner = getIntent()
                .getBooleanExtra("isGroupOwner",
                        false);
        peerName     = getIntent()
                .getStringExtra("peerName");

        // UI connect karo
        listMessages = findViewById(R.id.list_messages);
        etMessage    = findViewById(R.id.et_message);
        btnSend      = findViewById(R.id.btn_send);
        tvPeerName   = findViewById(R.id.tv_peer_name);

        // Peer ka naam dikhao
        tvPeerName.setText("📱 " + peerName);

        // Message list setup
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                messages);
        listMessages.setAdapter(adapter);

        // Agar Group Owner hoon → Server start karo
        if (isGroupOwner) {
            startServer();
        }

        // Send button click
        btnSend.setOnClickListener(v -> sendMessage());
    }

    // Message bhejna
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this,
                    "Kuch likho pehle!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Encrypt karo
        String encrypted = EncryptionHelper.encrypt(text);

        // Socket se bhejo
        new SocketClient(groupOwnerIP, encrypted,
                new SocketClient.SendListener() {
                    @Override
                    public void onSendSuccess() {
                        mainHandler.post(() -> {
                            // Apna message dikhao
                            messages.add("Tum: " + text);
                            adapter.notifyDataSetChanged();
                            listMessages.setSelection(
                                    messages.size() - 1);
                            etMessage.setText("");
                        });
                    }

                    @Override
                    public void onSendError(String error) {
                        mainHandler.post(() -> {
                            Toast.makeText(ChatActivity.this,
                                    "Send nahi hua: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        ).start();
    }

    // Server start karo (Group Owner)
    private void startServer() {
        socketServer = new SocketServer(message -> {
            // Decrypt karo
            String decrypted =
                    EncryptionHelper.decrypt(message);

            // UI pe dikhao
            mainHandler.post(() -> {
                messages.add("Dost: " + decrypted);
                adapter.notifyDataSetChanged();
                listMessages.setSelection(
                        messages.size() - 1);
            });
        });
        socketServer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketServer != null) {
            socketServer.stopServer();
        }
    }
}
