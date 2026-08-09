# 📡 MeshNet P2P - Offline WiFi Direct Encrypted Messaging & Mesh Network

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android Min SDK: 23](https://img.shields.io/badge/Min%20SDK-23%20(Android%206.0)-brightgreen.svg)](https://developer.android.com/)
[![Security: AES-256](https://img.shields.io/badge/Encryption-AES--256--CBC-blue.svg)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)

**MeshNet P2P** is an advanced, decentralized Android application designed for secure, offline communication without requiring Internet, Cellular Data, or Centralized Servers. It leverages native **Android WiFi Direct (P2P)**, **AES-256 Bit Encryption**, and a **Multi-Hop Mesh Relay Engine** to extend communication coverage beyond physical hardware boundaries.

---

## 🌟 Key Features

- **🌐 100% Offline Communication:** Works completely without Internet, SIM card, or external routers.
- **🔐 Military-Grade Security:** All messages are encrypted locally using **AES-256-CBC Cryptography**.
- **📡 Multi-Hop Mesh Relay:** Extends communication range (up to 50m–70m) by automatically forwarding packets through intermediate nodes.
- **🌐 Background Router Mode:** Dedicated Foreground Service allows intermediate devices to act as silent range-extending bridges without disturbing the user.
- **📁 Encrypted File Sharing:** High-speed offline sharing for Photos, PDFs, and Documents (up to 10-20 MB/s).
- **📲 Explicit Pairing Confirmation:** Custom connection request popups (Accept/Decline) to prevent unauthorized access.
- **🎨 Modern Cyberpunk UI:** High-contrast Dark Theme with glowing pulse animations and clear visibility.

---

## 🏗️ System Architecture



---

## 🛠️ Tech Stack & Dependencies

- **Language:** Java (JDK 17)
- **Framework:** Android Native SDK (API Level 23 to 34)
- **Networking:** Native `WifiP2pManager`, Java Sockets (`ServerSocket`, `Socket`)
- **Security:** `javax.crypto.Cipher` (AES-256-CBC with PKCS5Padding)
- **Background Engine:** Android Foreground Service (`BackgroundRouterService`)

---

## 🚀 How to Run & Build Project

### Prerequisites
- **Android Studio** (Jellyfish / Igneous or newer)
- **Android Device(s)** running Android 6.0 (API 23) or higher (Physical devices recommended for WiFi Direct testing).

### Step-by-Step Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/abind4005-coder/WiFiDirectChat.git
   Open in Android Studio:

Open Android Studio -> Click Open -> Select the cloned project folder.
Build APK:

Go to Top Menu: Build -> Build Bundle(s) / APK(s) -> Build APK(s).
Install the generated app-debug.apk on 2 or more Android devices.
Permissions Required:

Location Services (GPS): Required by Android OS for WiFi P2P scanning.
Nearby Devices / WiFi State: Allowed on first launch.

Direct 1-on-1 Chat:

Open the app on two nearby phones.
Click 🔍 SEARCH PEERS.
Tap on the target device name from the list.
Click ACCEPT on the receiving device to start chatting!
File Transfer:

Inside the Chat Screen, tap 📁 FILE.
Select any image/PDF/document. The file will be transferred and saved in Downloads.
Background Mesh Router Mode (Extended Range):

On the middle device (Phone B), toggle Background Relay Router to ON.
Phone B can now be minimized or locked. It will silently route messages between Phone A and Phone C over long distances!
📜 License
Distributed under the MIT License. See LICENSE for more information.

👨‍💻 Author & Contributions
Developed with ❤️ for B.Tech Final Year Capstone Project.
