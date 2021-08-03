package org.jfrog.bamboo.security;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.*;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Created by diman on 07/03/2017.
 */
@ThreadSafe
public class EncryptionHelper {

    private static final Logger log = Logger.getLogger(EncryptionHelper.class);

    private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";

    private static final String KEY_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 24;
    private static final String uiKey = generateRandomKey();
    private static final String dbKey = "Beetlejuice version $version (c) Copyright 2003-2005 Pols Consulting Limited";

    /***
     * Encrypts data with the constant DB key. Use this method to encrypt configuration data. For example, sensitive data which is meant
     * to be saved to the DB.
     * Use the 'decrypt' method for the opposite operation.
     * @param stringToEncrypt - Nullable string to encrypt.
     * @return - Encrypted data.
     */
    @NotNull
    public static String encryptForConfig(@Nullable String stringToEncrypt) {
        if (StringUtils.isEmpty(stringToEncrypt)) {
            return "";
        }

        try {
            final byte[] encrypted = getEncrypter(dbKey).doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getMimeEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt.", e);
        }
    }

    /***
     * Encrypts data with the changing generated key. Use this method to encrypt sensitive data before it is presented in the UI.
     * Use the 'decrypt' method for the opposite operation.
     * @param stringToEncrypt - Nullable string to encrypt.
     * @return - Encrypted data.
     */
    @NotNull
    public static String encryptForUi(@Nullable String stringToEncrypt) {
        if (StringUtils.isEmpty(stringToEncrypt)) {
            return "";
        }

        try {
            final byte[] encrypted = getEncrypter(uiKey).doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
            return new Base32().encodeAsString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt.", e);
        }
    }

    @NotNull
    public static String decrypt(@Nullable String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }

        try {
            final byte[] encrypted = Base64.getMimeDecoder().decode(data);
            return decryptWithKey(dbKey, encrypted);
        } catch (Exception e) {
            try {
                final byte[] encrypted = new Base32().decode(data);
                return decryptWithKey(uiKey, encrypted);
            } catch (Exception ee) {
                throw new RuntimeException("Failed to decrypt.", ee);
            }
        }
    }

    public static String decryptIfNeeded(String s) {
        try {
            s = decrypt(s);
        } catch (RuntimeException e) {
            // Ignore. The field may not be encrypted.
        }
        return s;
    }

    private static String decryptWithKey(String key, byte[] encrypted) throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        return new String(getDecrypter(key).doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static String generateRandomKey() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++)
            sb.append(KEY_CHARSET.charAt(rnd.nextInt(KEY_CHARSET.length())));
        return sb.toString();
    }

    private static SecretKey generateSecret(String key)
            throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        DESedeKeySpec myKeySpec = new DESedeKeySpec(key.getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory myKeyFactory = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
        return myKeyFactory.generateSecret(myKeySpec);
    }

    private static Cipher getDecrypter(String key) throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        SecretKey secretKey = generateSecret(key);
        final Cipher decrypter = threadLocalDecrypter.get();
        decrypter.init(Cipher.DECRYPT_MODE, secretKey);
        return decrypter;
    }

    private static Cipher getEncrypter(String key) throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        SecretKey secretKey = generateSecret(key);
        final Cipher encrypter = threadLocalEncrypter.get();
        encrypter.init(Cipher.ENCRYPT_MODE, secretKey);
        return encrypter;
    }

    private static ThreadLocal<Cipher> threadLocalEncrypter = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                log.error("Cannot create encrypter", e);
            }
            return null;
        }
    };

    private static ThreadLocal<Cipher> threadLocalDecrypter = new ThreadLocal<Cipher>() {
        @Override
        protected Cipher initialValue() {
            try {
                return Cipher.getInstance(DESEDE_ENCRYPTION_SCHEME);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                log.error("Cannot create decrypter", e);
            }
            return null;
        }
    };
}