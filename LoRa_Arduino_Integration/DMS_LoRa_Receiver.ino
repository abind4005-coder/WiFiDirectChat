/*
 * ============================================
 * DMS - Decentralized Mesh System
 * PHASE 2: LoRa Long Range Integration
 * FILE: DMS_LoRa_Receiver.ino (Phone B Side)
 * ============================================
 *
 * FLOW:
 * LoRa SX1278 RF Signal (15km)
 *     → Arduino Nano
 *         → Bluetooth (HC-05)
 *             → Android Phone B (DMS App)
 *
 * COLLEGE: Buddha Institute of Technology
 * TEAM: Aman Kumar Bind (Leader),
 *       Aryan Gond, Khushi Nishad, Muskan Yadav
 * ============================================
 */

#include <SPI.h>
#include <LoRa.h>
#include <SoftwareSerial.h>

// ------ LoRa SX1278 Pin Definitions ------
#define LORA_SS_PIN    10
#define LORA_RST_PIN    9
#define LORA_DIO0_PIN   2
#define LORA_FREQUENCY  433E6  // 433 MHz

// ------ HC-05 Bluetooth Pins ------
#define BT_RX_PIN  4
#define BT_TX_PIN  3

SoftwareSerial bluetoothSerial(BT_RX_PIN, BT_TX_PIN);

int totalReceived = 0;

// ==========================================
void setup() {
    Serial.begin(9600);
    bluetoothSerial.begin(9600);

    Serial.println("================================");
    Serial.println(" DMS LoRa RECEIVER Starting...");
    Serial.println("================================");

    // Initialize LoRa Radio Module
    LoRa.setPins(LORA_SS_PIN, LORA_RST_PIN, LORA_DIO0_PIN);

    if (!LoRa.begin(LORA_FREQUENCY)) {
        Serial.println("[ERROR] LoRa Module NOT Found!");
        bluetoothSerial.println("ERROR:LORA_NOT_FOUND");
        while (true);
    }

    // Settings MUST exactly match Sender
    LoRa.setSpreadingFactor(12);
    LoRa.setSignalBandwidth(125E3);
    LoRa.setCodingRate4(8);

    Serial.println("[OK] LoRa Receiver Ready!");
    Serial.println("[OK] Frequency: 433 MHz");
    Serial.println("[OK] Listening for incoming packets...");

    bluetoothSerial.println("STATUS:LORA_READY");
}

// ==========================================
void loop() {
    // Continuously listen for LoRa packets
    int packetSize = LoRa.parsePacket();

    if (packetSize) {
        String receivedPacket = "";

        // Read all bytes of the packet
        while (LoRa.available()) {
            receivedPacket += (char)LoRa.read();
        }

        // Get signal quality information
        int  rssi = LoRa.packetRssi();    // Signal Strength
        float snr = LoRa.packetSnr();     // Signal-to-Noise Ratio

        totalReceived++;

        Serial.println("============================");
        Serial.println("Packet #" + String(totalReceived) + " Received!");
        Serial.println("Raw Packet: " + receivedPacket);
        Serial.println("RSSI: " + String(rssi) + " dBm");
        Serial.println("SNR:  " + String(snr) + " dB");
        Serial.println("Size: " + String(packetSize) + " bytes");

        // Validate DMS packet format: "DMS|PacketNo|Message"
        if (receivedPacket.startsWith("DMS|")) {
            // Extract message after second pipe
            int firstPipe  = receivedPacket.indexOf('|');
            int secondPipe = receivedPacket.indexOf('|', firstPipe + 1);

            if (secondPipe != -1) {
                String packetNo = receivedPacket.substring(
                        firstPipe + 1, secondPipe);
                String message  = receivedPacket.substring(secondPipe + 1);

                Serial.println("Packet No: " + packetNo);
                Serial.println("Message:   " + message);

                // Forward to Android App via Bluetooth
                // Format: MSG:message|RSSI:-75
                bluetoothSerial.println(
                        "MSG:" + message + "|RSSI:" + String(rssi)
                );

                Serial.println("Forwarded to Android App!");
            }
        } else {
            Serial.println("[WARN] Invalid DMS packet - Ignored");
        }

        Serial.println("============================");
    }
}