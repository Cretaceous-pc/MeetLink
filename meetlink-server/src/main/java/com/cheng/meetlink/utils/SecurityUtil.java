package com.cheng.meetlink.utils;

import com.cheng.meetlink.exception.MeetLinkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

@Slf4j
public final class SecurityUtil {

    private static final KeyPair keyPair;
    private static final BCryptPasswordEncoder passwordEncoder;
    private static final String AesKey = "MeetLinkServer2026";

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            keyPair = keyGen.generateKeyPair();
            passwordEncoder = new BCryptPasswordEncoder();
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String getPublicKey() {
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN PUBLIC KEY-----\n");
        pemBuilder.append(Base64.getEncoder().encodeToString(publicKeyBytes));
        pemBuilder.append("\n-----END PUBLIC KEY-----");
        return pemBuilder.toString();
    }

    public static String decryptPassword(String encryptedPassword) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new MeetLinkException("密码解析失败~");
        }
    }

    public static String encryptPassword(String password) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encryptedBytes = cipher.doFinal(password.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new MeetLinkException("密码加密失败~");
        }
    }

    public static boolean verifyPassword(String password, String passwordHash) {
        return passwordEncoder.matches(password, passwordHash);
    }

    public static String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private static SecretKeySpec getSecretAesKeySpec() {
        return new SecretKeySpec(AesKey.getBytes(), "AES");
    }

    public static String aesEncrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretAesKeySpec());
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES加密失败：", e);
            throw new MeetLinkException("生成失败~");
        }
    }

    public static String aesDecrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getSecretAesKeySpec());
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            log.error("AES解密失败：", e);
            throw new MeetLinkException("解析失败~");
        }
    }
}