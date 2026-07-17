package top.kx.heartbeat.infrastructure.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SecretCryptoService {

    private static final String PREFIX = "{aes-gcm}";
    private static final String LEGACY_PREFIX = "{aes}";
    private static final String KEY_ALGORITHM = "AES";
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec keySpec;

    public SecretCryptoService(@Value("${heartbeat.security.secret-key:heartbeat-local-dev-secret}") String secretKey) {
        if (StringUtils.isBlank(secretKey)) {
            throw new IllegalArgumentException("Sensitive credential key must not be blank");
        }
        this.keySpec = new SecretKeySpec(normalizeKey(secretKey), KEY_ALGORITHM);
    }

    public String encryptIfPlain(String value) {
        String text = value == null ? "" : value.trim();
        if (StringUtils.isEmpty(text) || isEncrypted(text)) {
            return text;
        }
        try {
            return PREFIX + Base64.getEncoder().encodeToString(encryptGcm(text));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt sensitive credential", ex);
        }
    }

    public String decryptIfCipher(String value) {
        String text = value == null ? "" : value.trim();
        if (!isEncrypted(text)) {
            return text;
        }
        try {
            if (text.startsWith(PREFIX)) {
                byte[] payload = Base64.getDecoder().decode(text.substring(PREFIX.length()));
                return decryptGcm(payload);
            }
            byte[] payload = Base64.getDecoder().decode(text.substring(LEGACY_PREFIX.length()));
            return decryptLegacy(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt sensitive credential", ex);
        }
    }

    public String mask(String value) {
        String plain = decryptIfCipher(value);
        if (StringUtils.isEmpty(plain)) {
            return "";
        }
        if (plain.length() <= 4) {
            return "****";
        }
        return plain.substring(0, 2) + "****" + plain.substring(plain.length() - 2);
    }

    public boolean isEncrypted(String value) {
        String text = value == null ? "" : value.trim();
        return text.startsWith(PREFIX) || text.startsWith(LEGACY_PREFIX);
    }

    public String redact(String value) {
        return StringUtils.isBlank(value) ? "" : "******";
    }

    private byte[] encryptGcm(String value) throws Exception {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        SECURE_RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
        return payload;
    }

    private String decryptGcm(byte[] payload) throws Exception {
        if (payload.length < GCM_NONCE_LENGTH + GCM_TAG_LENGTH_BITS / 8) {
            throw new IllegalArgumentException("Invalid AES-GCM payload");
        }
        byte[] nonce = Arrays.copyOfRange(payload, 0, GCM_NONCE_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(payload, GCM_NONCE_LENGTH, payload.length);
        Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private String decryptLegacy(byte[] payload) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
    }

    private byte[] normalizeKey(String secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, 16);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize credential key", ex);
        }
    }
}
