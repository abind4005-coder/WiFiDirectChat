# 📡 DMS — Decentralized Mesh System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android Min SDK: 26](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)](https://developer.android.com/)
[![Security: AES-256](https://img.shields.io/badge/Encryption-AES--256--CBC-blue.svg)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
[![Blockchain: SHA-256](https://img.shields.io/badge/Blockchain-SHA--256-orange.svg)](https://en.wikipedia.org/wiki/SHA-2)
[![Firebase](https://img.shields.io/badge/Firebase-Realtime%20Database-yellow.svg)](https://firebase.google.com/)

**DMS (Decentralized Mesh System)** is a fully offline, encrypted, peer-to-peer Android communication app. It works completely without Internet, SIM card, or central servers — using native **WiFi Direct**, **AES-256 Encryption**, **SHA-256 Blockchain Ledger**, and a **Multi-Hop Mesh Relay Engine**.

> 🎓 **B.Tech Final Year Capstone Project**
> **Buddha Institute of Technology**
> **Team:** Aman Kumar Bind (Leader) | Aryan Gond | Khushi Nishad | Muskan Yadav

---

## 🌟 Key Features

| Feature | Description |
|:---|:---|
| 🌐 **100% Offline** | Works without Internet, SIM, or Router |
| 🔐 **AES-256-CBC Encryption** | Military-grade message encryption |
| ⛓️ **SHA-256 Blockchain Ledger** | Tamper-proof message logging |
| 🎙️ **Offline Voice Notes** | Record, send & play audio messages |
| 📁 **Encrypted File Transfer** | Photos, PDFs, Docs up to 1GB |
| 🌐 **Multi-Hop Mesh Relay** | Chain phones for 100m+ range |
| ⚡ **Background Router Service** | Silent mesh relay foreground service |
| 👤 **Unique Profile Identity** | One-time locked profile (e.g. Rahul#4891) |
| 🔥 **Firebase User Tracking** | Track who uses the app via Firebase Console |
| 📡 **LoRa Phase 2 Ready** | Arduino LoRa code for 15km future range |
| 🎨 **Cyberpunk Dark UI** | Neon cyan/green theme with pulse animations |

---

## 🏗️ System Architecture

```
DIRECT MODE (30-100m):
📱 Phone A ←——— WiFi Direct P2P ———→ 📱 Phone B
              AES-256 Encrypted
              SHA-256 Blockchain

MESH MODE (100m-500m+):
📱 Phone A ——→ 📱 Relay B ——→ 📱 Phone C
         Background Router Service

FIREBASE (Online Sync When Available):
📱 Phone A ——→ 🔥 Firebase ——→ 📊 Dashboard
         User Profile + Analytics
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|:---|:---|
| **Language** | Java (Android SDK) |
| **Networking** | WiFi Direct (WifiP2pManager API) |
| **Sockets** | TCP — Port 8888 (Chat) / Port 8899 (Files) |
| **Encryption** | AES-256-CBC + PKCS5Padding + Dynamic IV |
| **Blockchain** | SHA-256 Custom Hash Chain |
| **Audio** | MediaRecorder (AAC/M4A format) |
| **Storage** | Android SAF (Storage Access Framework) |
| **Backend** | Firebase Realtime Database + Analytics |
| **Background** | Android Foreground Service |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 14 (API 35) |

---

## 🔥 Firebase Integration

DMS uses Firebase for **optional user tracking** when internet is available:

- **Realtime Database:** Stores user profile name, device info, first seen & last seen
- **Analytics:** Tracks app opens and new user registrations
- **Offline Support:** App works 100% offline — Firebase syncs automatically when internet becomes available

**Firebase Console Dashboard:**
```
console.firebase.google.com → DMS-MeshNet → Realtime Database

DMS_Users/
├── Rahul_4891/
│   ├── profileName: "Rahul#4891"
│   ├── deviceModel: "Redmi Note 12"
│   ├── firstSeen:   "21-08-2026 18:52"
│   ├── lastSeen:    "21-08-2026 19:00"
│   └── status:      "Active"
```

---

## 📡 LoRa Phase 2 Integration (Future Scope)

See [`LoRa_Arduino_Integration/`](./LoRa_Arduino_Integration/) folder for complete Arduino code.

| Feature | WiFi Direct (Current) | LoRa (Future) |
|:---|:---:|:---:|
| Range | 100m | 15km |
| Speed | 250 Mbps | 5-50 Kbps |
| File Transfer | ✅ | ❌ Text Only |
| Hardware Cost | Free | ₹1200/device |

---

## 📊 Network Capacity

| Mode | Max Users | Range |
|:---|:---:|:---|
| Direct 1-to-1 | 2 | ~30m |
| Single P2P Group | 8 | ~50m |
| Multi-Hop Mesh | 50+ | 100m-500m+ |
| LoRa Future | Unlimited | 2km-15km |

---

## 🚀 How to Build & Run

### Prerequisites
- Android Studio (Latest)
- Android Device(s) — Android 8.0+ (API 26+)
- 2 Physical devices for testing WiFi Direct

### Installation Steps

```bash
# 1. Clone Repository
git clone https://github.com/abind4005-coder/WiFiDirectChat.git

# 2. Open in Android Studio
File → Open → Select cloned folder

# 3. Add Firebase (Optional)
# Place your google-services.json in app/ folder
# Get it from: console.firebase.google.com

# 4. Build APK
Build → Build Bundle(s)/APK(s) → Build APK(s)

# 5. Install on devices
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Required Permissions
- `ACCESS_FINE_LOCATION` — Required by Android for WiFi Direct scanning
- `NEARBY_WIFI_DEVICES` — Android 12+ WiFi Direct
- `RECORD_AUDIO` — Voice Notes feature
- `FOREGROUND_SERVICE` — Background Mesh Router

---

## 📱 How to Use

### Text Chat
```
1. Open app on 2 phones
2. Set unique Profile Name (one-time)
3. Click 🔍 SEARCH PEERS
4. Tap device from list → Connect
5. Accept on receiving device
6. Start chatting! 💬
```

### Voice Notes
```
1. In Chat Screen → tap 🎙️ REC
2. Speak your message
3. Tap ⏹️ STOP → Auto sent!
4. Receiver taps message → Play 🔊
```

### File Transfer
```
1. In Chat Screen → tap 📁 FILE
2. Select any file (Photo/PDF/Doc)
3. File sent encrypted!
4. Receiver taps → Save to custom folder 💾
```

### Mesh Relay Mode
```
1. On middle phone → Toggle "Background Relay Router" ON
2. Phone can be minimized/locked
3. Messages route through it silently!
```

---

## 🔐 Security Architecture

```
Layer 1: AES-256-CBC Encryption
  Plain Text → AES-256 + Dynamic IV → Base64 Cipher

Layer 2: SHA-256 Blockchain Ledger  
  Each message = 1 Block
  Block = {Index + Timestamp + Data + PreviousHash + Hash}
  Tamper = Chain Break → Detected!

Layer 3: Unique Identity Lock
  Name + Random Tag = Rahul#4891 (Permanent)
  Prevents impersonation attacks
```

---

## 📂 Project Structure

```
WiFiDirectChat/
├── app/src/main/java/com/btech/wifidirectchat/
│   ├── MainActivity.java          # Main screen + Firebase
│   ├── ChatActivity.java          # Chat + Voice + Files
│   ├── WiFiDirectManager.java     # P2P connection manager
│   ├── WiFiDirectBroadcastReceiver.java
│   ├── SocketServer.java          # TCP Port 8888 receiver
│   ├── SocketClient.java          # TCP message sender
│   ├── EncryptionHelper.java      # AES-256 cipher
│   ├── BlockchainManager.java     # SHA-256 ledger
│   ├── BackgroundRouterService.java # Mesh relay service
│   ├── FileTransferService.java   # Port 8899 file engine
│   └── AudioRecorderHelper.java   # Voice note recorder
├── app/src/main/res/
│   ├── layout/activity_main.xml
│   ├── layout/activity_chat.xml
│   └── anim/pulse.xml
├── LoRa_Arduino_Integration/      # Phase 2 Arduino Code
│   ├── DMS_LoRa_Sender.ino
│   ├── DMS_LoRa_Receiver.ino
│   └── README.md
└── README.md
```

---

## 📜 License

Distributed under the **MIT License**.

---

## 👨‍💻 Team

| Name | Role |
|:---|:---|
| **Aman Kumar Bind** | Team Leader & Lead Developer |
| **Aryan Gond** | Developer |
| **Khushi Nishad** | Developer |
| **Muskan Yadav** | Developer |

> 🎓 Developed for **B.Tech Final Year Capstone Project**
> **Buddha Institute of Technology | CSE Department | 2025-26**
