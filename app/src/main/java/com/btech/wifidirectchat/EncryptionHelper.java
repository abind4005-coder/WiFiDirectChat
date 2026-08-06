package com.btech.wifidirectchat;

import android.util.Base64;
import android.util.Log;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionHelper {

    private static final String TAG = "Encryption";

    // Secret key — dono phones pe same hona chahiye
    private static final String SECRET = "WiFiDirectChat2024SecretKey!BTech";

    // ===== MESSAGE ENCRYPT KARO (Lock) =====
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = generateKey();

            // Random IV banao (har message ke liye alag)
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // AES Cipher
            Cipher cipher = Cipher.getInstance(
                    "AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // Encrypt karo
            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes("UTF-8"));

            // IV + encrypted combine karo
            byte[] combined = new byte[16 + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, 16);
            System.arraycopy(encrypted, 0,
                    combined, 16, encrypted.length);

            // Base64 me convert karo (text bhejne ke liye)
            return Base64.encodeToString(combined,
                    Base64.DEFAULT);

        } catch (Exception e) {
            Log.e(TAG, "Encrypt error: " + e.getMessage());
            return plainText; // Fallback — original bhejo
        }
    }

    // ===== MESSAGE DECRYPT KARO (Unlock) =====
    public static String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.decode(encryptedText,
                    Base64.DEFAULT);

            // IV nikalo (pehle 16 bytes)
            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, 16);

            // Encrypted data nikalo
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 16,
                    encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = generateKey();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Decrypt karo
            Cipher cipher = Cipher.getInstance(
                    "AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");

        } catch (Exception e) {
            Log.e(TAG, "Decrypt error: " + e.getMessage());
            return encryptedText; // Fallback
        }
    }

    // Key generate karo SECRET se
    private static SecretKeySpec generateKey() throws Exception {
        MessageDigest digest = MessageDigest
                .getInstance("SHA-256");
        byte[] keyBytes = digest.digest(
                SECRET.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, "AES");
    }
}