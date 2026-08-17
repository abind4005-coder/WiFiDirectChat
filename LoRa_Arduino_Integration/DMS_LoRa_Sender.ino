/*
 * ============================================
 * DMS - Decentralized Mesh System
 * PHASE 2: LoRa Long Range Integration
 * FILE: DMS_LoRa_Sender.ino (Phone A Side)
 * ============================================
 *
 * FLOW:
 * Android Phone A
 *     → Bluetooth (HC-05)
 *         → Arduino Nano
 *             → LoRa SX1278 RF Signal (15km)
 *
 * HARDWARE NEEDED:
 * 1. Arduino Nano       = Rs.350
 * 2. LoRa SX1278 Ra-02  = Rs.500
 * 3. HC-05 Bluetooth    = Rs.150
 * 4. Battery + Holder   = Rs.200
 *
 * LIBRARIES: (Install from Arduino IDE Library Manager)
 * 1. LoRa by Sandeep Mistry
 * 2. SoftwareSerial (Built-in)
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
#define LORA_SS_PIN    10   // Chip Select
#define LORA_RST_PIN    9   // Reset
#define LORA_DIO0_PIN   2   // Interrupt
#define LORA_FREQUENCY  433E6  // 433 MHz

// ------ HC-05 Bluetooth Pins ------
#define BT_RX_PIN  4   // Arduino RX ← HC05 TX
#define BT_TX_PIN  3   // Arduino TX → HC05 RX

SoftwareSerial bluetoothSerial(BT_RX_PIN, BT_TX_PIN);

String receivedFromPhone = "";
int    packetCount       = 0;

// ==========================================
void setup() {
    Serial.begin(9600);
    bluetoothSerial.begin(9600);  // HC-05 default speed

    Serial.println("==============================");
    Serial.println(" DMS LoRa SENDER Starting...");
    Serial.println("==============================");

    // Initialize LoRa Radio Module
    LoRa.setPins(LORA_SS_PIN, LORA_RST_PIN, LORA_DIO0_PIN);

    if (!LoRa.begin(LORA_FREQUENCY)) {
        Serial.println("[ERROR] LoRa Module NOT Found!");
        Serial.println("Check SX1278 wiring connections.");
        bluetoothSerial.println("ERROR:LORA_NOT_FOUND");
        while (true);  // Stop execution
    }

    // LoRa Settings for Maximum Range (15km)
    LoRa.setSpreadingFactor(12);      // SF12 = Longest range
    LoRa.setSignalBandwidth(125E3);   // 125 kHz bandwidth
    LoRa.setCodingRate4(8);           // Best error correction
    LoRa.setTxPower(20);              // 20 dBm = Maximum power

    Serial.println("[OK] LoRa Module Ready!");
    Serial.println("[OK] Frequency: 433 MHz");
    Serial.println("[OK] Max Range: ~15 km");
    Serial.println("[OK] Waiting for Android message...");

    // Notify Android App that LoRa is ready
    bluetoothSerial.println("STATUS:LORA_READY");
}

// ==========================================
void loop() {
    // Read message byte by byte from Android via Bluetooth
    while (bluetoothSerial.available()) {
        char inChar = bluetoothSerial.read();

        if (inChar == '\n') {
            // Complete message received - now send via LoRa
            receivedFromPhone.trim();

            if (receivedFromPhone.length() > 0) {
                sendMessageViaLoRa(receivedFromPhone);
                receivedFromPhone = "";
            }
        } else {
            receivedFromPhone += inChar;
        }
    }
}

// ==========================================
void sendMessageViaLoRa(String message) {
    packetCount++;

    // Build DMS LoRa Packet with header
    // Format: DMS|PacketNo|Message
    String loraPacket = "DMS|" + String(packetCount) + "|" + message;

    Serial.println("---------------------------");
    Serial.println("Sending Packet #" + String(packetCount));
    Serial.println("Message: " + message);
    Serial.println("Packet:  " + loraPacket);

    // Transmit LoRa Packet
    LoRa.beginPacket();
    LoRa.print(loraPacket);
    LoRa.endPacket();

    Serial.println("Packet Sent! Size: "
                   + String(loraPacket.length()) + " bytes");
    Serial.println("---------------------------");

    // Notify Android App - message sent successfully
    bluetoothSerial.println("STATUS:SENT_OK|" + String(packetCount));
}