package org.jfrog.bamboo.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Created by diman on 07/03/2017.
 */
@ThreadSafe
public class EncryptionHelper {

    private static final Logger log = Logger.getLogger(EncryptionHelper.class);

    private static String DESEDE_ENCRYPTION_SCHEME = "DESede";

    @NotNull
    public static String encrypt(@Nullable String stringToEncrypt) {
        if (StringUtils.isEmpty(stringToEncrypt)) {
            return StringUtils.EMPTY;
        }
        try {
            final byte[] encrypted = getEncrypter().doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getMimeEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt.", e);
        }
    }

    @NotNull
    public static String decrypt(@Nullable String data) {
        if (StringUtils.isEmpty(data)) {
            return StringUtils.EMPTY;
        }
        try {
            final byte[] encrypted = Base64.getMimeDecoder().decode(data);
            return new String(getDecrypter().doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt.", e);
        }
    }

    private static SecretKey generateSecret()
            throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        DESedeKeySpec myKeySpec = new DESedeKeySpec("Beetlejuice version $version (c) Copyright 2003-2005 Pols Consulting Limited"
                .getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory myKeyFactory = SecretKeyFactory.getInstance(DESEDE_ENCRYPTION_SCHEME);
        return myKeyFactory.generateSecret(myKeySpec);
    }

    private static Cipher getDecrypter() throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        SecretKey secretKey = generateSecret();
        final Cipher decrypter = threadLocalDecrypter.get();
        decrypter.init(Cipher.DECRYPT_MODE, secretKey);
        return decrypter;
    }

    private static Cipher getEncrypter() throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException {
        SecretKey secretKey = generateSecret();
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