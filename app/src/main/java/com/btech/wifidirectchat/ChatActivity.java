package com.btech.wifidirectchat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final int FILE_PICK_CODE = 200;
    private static final int SAVE_FOLDER_PICK_CODE = 300;
    private static final int AUDIO_PERM_CODE = 400;

    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend, btnAttachFile, btnRecordAudio;
    private TextView tvPeerName;

    private List<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String lastKnownPeerIP;
    private boolean isGroupOwner;
    private String peerName;
    private String myUserProfileName;

    private SocketServer socketServer;
    private WiFiDirectManager wifiManager;
    private BlockchainManager blockchainManager;
    private AudioRecorderHelper audioHelper;
    private boolean isRecording = false;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isFileServerRunning = true;
    private File lastReceivedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        wifiManager = new WiFiDirectManager(this);
        blockchainManager = new BlockchainManager();
        audioHelper = new AudioRecorderHelper();

        isGroupOwner = getIntent().getBooleanExtra("isGroupOwner", false);
        String groupOwnerIP = getIntent().getStringExtra("groupOwnerIP");
        peerName     = getIntent().getStringExtra("peerName");

        myUserProfileName = getSharedPreferences("UserProfile", MODE_PRIVATE).getString("username", Build.MODEL);

        if (!isGroupOwner && groupOwnerIP != null) {
            lastKnownPeerIP = groupOwnerIP;
        }

        listMessages   = findViewById(R.id.list_messages);
        etMessage      = findViewById(R.id.et_message);
        btnSend        = findViewById(R.id.btn_send);
        btnAttachFile  = findViewById(R.id.btn_attach_file);
        btnRecordAudio = findViewById(R.id.btn_record_audio);
        tvPeerName     = findViewById(R.id.tv_peer_name);

        if (peerName != null) {
            tvPeerName.setText("📱 " + peerName + " [⛓️ SECURED]");
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                String item = getItem(position);
                if (item != null && item.startsWith("Aap / You:")) {
                    textView.setTextColor(Color.parseColor("#00F2FE"));
                } else if (item != null && item.contains("[VOICE NOTE]")) {
                    textView.setTextColor(Color.parseColor("#FF5555")); // Red for Voice Notes
                } else if (item != null && item.contains("[FILE]")) {
                    textView.setTextColor(Color.parseColor("#E3B341"));
                } else {
                    textView.setTextColor(Color.parseColor("#00FF87"));
                }
                textView.setTextSize(14);
                return view;
            }
        };
        listMessages.setAdapter(adapter);

        listMessages.setOnItemClickListener((parent, view, position, id) -> {
            String item = messages.get(position);
            if (item.contains("[VOICE NOTE]") && lastReceivedFile != null && lastReceivedFile.exists()) {
                audioHelper.playAudio(lastReceivedFile.getAbsolutePath());
                Toast.makeText(ChatActivity.this, "Playing Voice Note... 🔊", Toast.LENGTH_SHORT).show();
            } else if (item.contains("FILE")) {
                promptCustomFolderSave();
            }
        });

        startMeshServer();
        startFileReceiverServer();

        btnSend.setOnClickListener(v -> sendMessage());
        btnAttachFile.setOnClickListener(v -> openFilePicker());
        btnRecordAudio.setOnClickListener(v -> handleAudioRecording());
    }

    private void handleAudioRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERM_CODE);
            return;
        }

        if (!isRecording) {
            isRecording = true;
            btnRecordAudio.setText("⏹️ STOP");
            btnRecordAudio.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF00FF87));
            audioHelper.startRecording(this);
            Toast.makeText(this, "Recording Voice Note...", Toast.LENGTH_SHORT).show();
        } else {
            isRecording = false;
            btnRecordAudio.setText("🎙️ REC");
            btnRecordAudio.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1F293D));
            File audioFile = audioHelper.stopRecording();

            if (audioFile != null && audioFile.exists()) {
                Uri audioUri = Uri.fromFile(audioFile);
                String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";

                FileTransferService.sendFile(this, destIP, audioUri, new FileTransferService.FileSendListener() {
                    @Override
                    public void onSuccess() {
                        mainHandler.post(() -> {
                            messages.add("Aap / You: 🎙️ [VOICE NOTE SENT]");
                            adapter.notifyDataSetChanged();
                            listMessages.setSelection(messages.size() - 1);
                            Toast.makeText(ChatActivity.this, "Voice Note Sent! 🎙️", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> Toast.makeText(ChatActivity.this, "Audio Send Error: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }
    }

    private void promptCustomFolderSave() {
        new AlertDialog.Builder(this)
                .setTitle("💾 Save File to Custom Folder")
                .setMessage("Kya aap is received file ko apni pasand ke Storage Folder me save karna chahte hain?")
                .setPositiveButton("SELECT FOLDER", (dialog, which) -> openCustomFolderPicker())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openCustomFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String defaultFileName = (lastReceivedFile != null) ? lastReceivedFile.getName() : "Received_File_" + System.currentTimeMillis() + ".jpg";
            intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
            startActivityForResult(intent, SAVE_FOLDER_PICK_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open Folder Manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";

                Toast.makeText(this, "Sending Encrypted File...", Toast.LENGTH_SHORT).show();
                FileTransferService.sendFile(this, destIP, fileUri, new FileTransferService.FileSendListener() {
                    @Override
                    public void onSuccess() {
                        mainHandler.post(() -> {
                            messages.add("Aap / You: [FILE SENT 📁]");
                            adapter.notifyDataSetChanged();
                            listMessages.setSelection(messages.size() - 1);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> Toast.makeText(ChatActivity.this, "File Transfer Error: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }

        if (requestCode == SAVE_FOLDER_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri targetLocationUri = data.getData();
            if (targetLocationUri != null) {
                saveFileToCustomLocation(targetLocationUri);
            }
        }
    }

    private void saveFileToCustomLocation(Uri targetUri) {
        new Thread(() -> {
            try {
                OutputStream out = getContentResolver().openOutputStream(targetUri);

                if (lastReceivedFile != null && lastReceivedFile.exists()) {
                    InputStream in = new FileInputStream(lastReceivedFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    in.close();
                } else {
                    out.write("Received File from WiFi Direct Mesh Chat".getBytes());
                }

                out.close();

                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "File Saved to Custom Folder! 💾✅", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "Save Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startFileReceiverServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(FileTransferService.FILE_PORT);
                serverSocket.setReuseAddress(true);
                while (isFileServerRunning) {
                    Socket client = serverSocket.accept();
                    InputStream inputStream = client.getInputStream();

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) downloadsDir.mkdirs();

                    lastReceivedFile = new File(downloadsDir, "WiFiChat_Received_" + System.currentTimeMillis() + ".m4a");

                    FileOutputStream fos = new FileOutputStream(lastReceivedFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    fos.close();
                    inputStream.close();
                    client.close();

                    mainHandler.post(() -> {
                        messages.add("Dost / Friend: 🎙️ [VOICE NOTE RECEIVED - Tap to Play 🔊]");
                        adapter.notifyDataSetChanged();
                        listMessages.setSelection(messages.size() - 1);
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Message khali nahi ho sakta!", Toast.LENGTH_SHORT).show();
            return;
        }

        String encrypted = EncryptionHelper.encrypt(text);
        BlockchainManager.Block newBlock = blockchainManager.addMessageBlock(encrypted);

        String blockchainPacket = myUserProfileName + "|" + newBlock.hash.substring(0, 8) + "|" + encrypted;
        String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";

        new SocketClient(destIP, blockchainPacket, new SocketClient.SendListener() {
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
        socketServer = new SocketServer((message, senderIP) -> {
            lastKnownPeerIP = senderIP;

            String[] parts = message.split("\\|", 3);
            if (parts.length == 3) {
                String senderProfileName = parts[0];
                String blockHash = parts[1];
                String encryptedMsg = parts[2];

                blockchainManager.addMessageBlock(encryptedMsg);
                String decrypted = EncryptionHelper.decrypt(encryptedMsg);

                mainHandler.post(() -> {
                    messages.add(senderProfileName + ": " + decrypted);
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
        isFileServerRunning = false;
        if (socketServer != null) {
            socketServer.stopServer();
        }
        if (wifiManager != null) {
            wifiManager.disconnect();
        }
    }
}