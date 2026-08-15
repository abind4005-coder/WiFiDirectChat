package com.btech.wifidirectchat;

import android.util.Log;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class BlockchainManager {

    private static final String TAG = "BlockchainEngine";
    private List<Block> chain = new ArrayList<>();

    public BlockchainManager() {
        // Genesis Block (Pehla Block)
        chain.add(new Block("0", "Genesis Block: MeshNet Chain Initialized"));
    }

    // Naya Message Block Ledger me Add Karein
    public Block addMessageBlock(String messageData) {
        String prevHash = chain.get(chain.size() - 1).hash;
        Block newBlock = new Block(prevHash, messageData);
        chain.add(newBlock);
        Log.d(TAG, "New Block Mined & Added to Ledger! Hash: " + newBlock.hash);
        return newBlock;
    }

    // Verify karein ki Packet kisi Hacker dwara Tamper / Change toh nahi hua
    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Hash Verify
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                Log.e(TAG, "SECURITY ALERT: Message Tampering Detected!");
                return false;
            }

            // Previous Hash Chain Verify
            if (!currentBlock.previousHash.equals(previousBlock.hash)) {
                Log.e(TAG, "SECURITY ALERT: Blockchain Broken / Invalid Chain!");
                return false;
            }
        }
        return true;
    }

    // Block Class Structure
    public static class Block {
        public String hash;
        public String previousHash;
        public String data;
        public long timeStamp;

        public Block(String previousHash, String data) {
            this.previousHash = previousHash;
            this.data = data;
            this.timeStamp = System.currentTimeMillis();
            this.hash = calculateHash();
        }

        public String calculateHash() {
            try {
                String input = previousHash + Long.toString(timeStamp) + data;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}