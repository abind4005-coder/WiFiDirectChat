package com.btech.wifidirectchat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final int FILE_PICK_CODE  = 200;
    private static final int SAVE_FILE_CODE  = 300;
    private static final int AUDIO_PERM_CODE = 400;

    private ListView listMessages;
    private EditText etMessage;
    private Button   btnSend, btnAttachFile, btnRecordAudio;
    private TextView tvPeerName;

    private final List<String>          messages     = new ArrayList<>();
    private final Map<Integer, String>  filePathMap  = new HashMap<>();
    // filePathMap: message index → absolute file path (for later save/play)

    private ArrayAdapter<String> adapter;

    private String  lastKnownPeerIP;
    private boolean isGroupOwner;
    private String  peerName;
    private String  myUserProfileName;

    private SocketServer        socketServer;
    private WiFiDirectManager   wifiManager;
    private BlockchainManager   blockchainManager;
    private AudioRecorderHelper audioHelper;
    private boolean             isRecording = false;

    private final Handler mainHandler        = new Handler(Looper.getMainLooper());
    private       boolean isFileServerRunning = true;

    // Currently selected file for saving
    private String selectedFilePathForSave = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        wifiManager       = new WiFiDirectManager(this);
        blockchainManager = new BlockchainManager();
        audioHelper       = new AudioRecorderHelper();

        isGroupOwner = getIntent().getBooleanExtra("isGroupOwner", false);
        String groupOwnerIP = getIntent().getStringExtra("groupOwnerIP");
        peerName = getIntent().getStringExtra("peerName");

        myUserProfileName = getSharedPreferences("UserProfile", MODE_PRIVATE)
                .getString("username", Build.MODEL);

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

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, messages) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                String item = getItem(position);
                if (item == null) return view;

                if (item.startsWith("Aap / You:")) {
                    tv.setTextColor(Color.parseColor("#00F2FE"));
                } else if (item.contains("[VOICE NOTE]") || item.contains("VOICE NOTE")) {
                    tv.setTextColor(Color.parseColor("#FF5555"));
                } else if (item.contains("[FILE]") || item.contains("FILE RECEIVED")) {
                    tv.setTextColor(Color.parseColor("#E3B341"));
                } else {
                    tv.setTextColor(Color.parseColor("#00FF87"));
                }
                tv.setTextSize(14);
                tv.setPadding(12, 10, 12, 10);
                return view;
            }
        };
        listMessages.setAdapter(adapter);

        // ✅ TAP ANY TIME: Tap on received message → Save / Play dialog
        listMessages.setOnItemClickListener((parent, view, position, id) -> {
            String item = messages.get(position);

            // Check if this message has a stored file path
            if (filePathMap.containsKey(position)) {
                String filePath = filePathMap.get(position);
                File   file     = new File(filePath);

                if (!file.exists()) {
                    Toast.makeText(this, "File no longer available!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isAudio = filePath.endsWith(".m4a") || filePath.endsWith(".mp3");

                if (isAudio) {
                    // Voice Note options: Play or Save
                    new AlertDialog.Builder(this)
                            .setTitle("🎙️ Voice Note Options")
                            .setMessage("Kya karna chahte hain?")
                            .setPositiveButton("▶️ Play Now", (d, w) -> {
                                audioHelper.playAudio(filePath);
                                Toast.makeText(this, "Playing Voice Note 🔊", Toast.LENGTH_SHORT).show();
                            })
                            .setNeutralButton("💾 Save to Device", (d, w) -> {
                                selectedFilePathForSave = filePath;
                                openSavePicker(filePath);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // File / Image options: Save to custom folder
                    String fileName = file.getName();
                    new AlertDialog.Builder(this)
                            .setTitle("📁 File Options")
                            .setMessage("File: " + fileName + "\n\nKya karna chahte hain?")
                            .setPositiveButton("💾 Save to Custom Folder", (d, w) -> {
                                selectedFilePathForSave = filePath;
                                openSavePicker(filePath);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        });

        startMeshServer();
        startFileReceiverServer();

        btnSend.setOnClickListener(v -> sendMessage());
        btnAttachFile.setOnClickListener(v -> openFilePicker());
        btnRecordAudio.setOnClickListener(v -> handleAudioRecording());
    }

    // ─── Open File Picker (to SEND) ────────────────────────────────
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_PICK_CODE);
    }

    // ─── Open Save Picker (custom folder to SAVE received file) ───
    private void openSavePicker(String sourceFilePath) {
        String fileName = new File(sourceFilePath).getName();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, SAVE_FILE_CODE);
    }

    // ─── Audio Recording ───────────────────────────────────────────
    private void handleAudioRecording() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERM_CODE);
            return;
        }

        if (!isRecording) {
            isRecording = true;
            btnRecordAudio.setText("⏹️ STOP");
            btnRecordAudio.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF00FF87));
            audioHelper.startRecording(this);
            Toast.makeText(this, "🎙️ Recording... Dobara dabao to send!", Toast.LENGTH_SHORT).show();
        } else {
            isRecording = false;
            btnRecordAudio.setText("🎙️ REC");
            btnRecordAudio.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF1F293D));

            File audioFile = audioHelper.stopRecording();

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                String destIP  = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";
                Uri    audioUri = Uri.fromFile(audioFile);

                Toast.makeText(this, "Sending Voice Note...", Toast.LENGTH_SHORT).show();
                FileTransferService.sendFile(this, destIP, audioUri,
                        new FileTransferService.FileSendListener() {
                            @Override public void onSuccess() {
                                mainHandler.post(() -> {
                                    addMessage("Aap / You: 🎙️ [VOICE NOTE SENT]");
                                    Toast.makeText(ChatActivity.this,
                                            "Voice Note Sent! 🎙️✅", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String error) {
                                mainHandler.post(() ->
                                        Toast.makeText(ChatActivity.this,
                                                "Audio Send Error: " + error, Toast.LENGTH_LONG).show());
                            }
                        });
            } else {
                Toast.makeText(this, "Recording too short! Try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─── Activity Results ──────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // File selected to SEND
        if (requestCode == FILE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";
                Toast.makeText(this, "Sending File...", Toast.LENGTH_SHORT).show();
                FileTransferService.sendFile(this, destIP, fileUri,
                        new FileTransferService.FileSendListener() {
                            @Override public void onSuccess() {
                                mainHandler.post(() -> addMessage("Aap / You: [FILE SENT 📁]"));
                            }
                            @Override public void onError(String error) {
                                mainHandler.post(() ->
                                        Toast.makeText(ChatActivity.this,
                                                "Send Error: " + error, Toast.LENGTH_LONG).show());
                            }
                        });
            }
        }

        // Custom save location selected
        if (requestCode == SAVE_FILE_CODE && resultCode == RESULT_OK && data != null) {
            Uri saveUri = data.getData();
            if (saveUri != null && selectedFilePathForSave != null) {
                saveFileToUri(selectedFilePathForSave, saveUri);
                selectedFilePathForSave = null;
            }
        }
    }

    // ─── Write received file to user-selected Uri ──────────────────
    private void saveFileToUri(String sourcePath, Uri targetUri) {
        new Thread(() -> {
            try {
                OutputStream out = getContentResolver().openOutputStream(targetUri);
                InputStream  in  = new FileInputStream(sourcePath);
                byte[] buf = new byte[8192];
                int    len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                in.close();
                out.close();
                mainHandler.post(() ->
                        Toast.makeText(this, "File Saved Successfully! 💾✅",
                                Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Save Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── File Receiver Server ──────────────────────────────────────
    private void startFileReceiverServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(FileTransferService.FILE_PORT);
                serverSocket.setReuseAddress(true);

                while (isFileServerRunning) {
                    Socket      client      = serverSocket.accept();
                    InputStream inputStream = client.getInputStream();

                    // Read filename
                    int    nameLen   = inputStream.read();
                    byte[] nameBytes = new byte[nameLen];
                    int    totalRead = 0;
                    while (totalRead < nameLen) {
                        int r = inputStream.read(nameBytes, totalRead, nameLen - totalRead);
                        if (r == -1) break;
                        totalRead += r;
                    }
                    String receivedFileName = new String(nameBytes, "UTF-8");

                    // Save permanently in app's FILES directory (persists across sessions!)
                    File saveDir = new File(getFilesDir(), "ReceivedFiles");
                    if (!saveDir.exists()) saveDir.mkdirs();

                    File savedFile = new File(saveDir, receivedFileName);
                    FileOutputStream fos = new FileOutputStream(savedFile);
                    byte[] buf = new byte[8192];
                    int    bytesRead;
                    while ((bytesRead = inputStream.read(buf)) != -1) {
                        fos.write(buf, 0, bytesRead);
                    }
                    fos.close();
                    inputStream.close();
                    client.close();

                    boolean isAudio = receivedFileName.endsWith(".m4a")
                            || receivedFileName.endsWith(".mp3");

                    final String savedPath = savedFile.getAbsolutePath();

                    mainHandler.post(() -> {
                        int msgIndex = messages.size(); // Index of new message

                        String label = isAudio
                                ? "👤 Dost: 🎙️ [VOICE NOTE — Tap to Play or Save]"
                                : "👤 Dost: 📁 [FILE: " + receivedFileName + " — Tap to Save]";

                        // ✅ Store file path mapped to this message index
                        filePathMap.put(msgIndex, savedPath);
                        addMessage(label);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ─── Text Message Sender ───────────────────────────────────────
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Message likhein pehle!", Toast.LENGTH_SHORT).show();
            return;
        }

        String encrypted = EncryptionHelper.encrypt(text);
        BlockchainManager.Block block = blockchainManager.addMessageBlock(encrypted);
        String packet = myUserProfileName + "|" + block.hash.substring(0, 8) + "|" + encrypted;
        String destIP = (lastKnownPeerIP != null) ? lastKnownPeerIP : "192.168.49.1";

        new SocketClient(destIP, packet, new SocketClient.SendListener() {
            @Override public void onSendSuccess() {
                mainHandler.post(() -> {
                    addMessage("Aap / You: " + text);
                    etMessage.setText("");
                });
            }
            @Override public void onSendError(String error) {
                mainHandler.post(() ->
                        Toast.makeText(ChatActivity.this,
                                "Send Error: Connection lost!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── Mesh Message Receiver ─────────────────────────────────────
    private void startMeshServer() {
        socketServer = new SocketServer((message, senderIP) -> {
            lastKnownPeerIP = senderIP;
            String[] parts = message.split("\\|", 3);
            if (parts.length == 3) {
                String sender       = parts[0];
                String encryptedMsg = parts[2];
                blockchainManager.addMessageBlock(encryptedMsg);
                String decrypted = EncryptionHelper.decrypt(encryptedMsg);
                mainHandler.post(() -> addMessage(sender + ": " + decrypted));
            }
        });
        socketServer.start();
    }

    // ─── Helper: Add message to list ──────────────────────────────
    private void addMessage(String msg) {
        messages.add(msg);
        adapter.notifyDataSetChanged();
        listMessages.setSelection(messages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isFileServerRunning = false;
        audioHelper.release();
        if (socketServer != null) socketServer.stopServer();
        if (wifiManager  != null) wifiManager.disconnect();
    }
}