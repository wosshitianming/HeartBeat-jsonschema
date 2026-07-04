package top.kx.heartbeat.infrastructure.security;


import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class SecretCryptoService {

    private static final String PREFIX = "{aes}";
    private static final String ALGORITHM = "AES";

    private final SecretKeySpec keySpec;

    public SecretCryptoService(@Value("${heartbeat.security.secret-key:heartbeat-local-dev-secret}") String secretKey) {
        this.keySpec = new SecretKeySpec(normalizeKey(secretKey), ALGORITHM);
    }

    public String encryptIfPlain(String value) {
        String text = value == null ? "" : value.trim();
        if (StringUtils.isEmpty(text) || text.startsWith(PREFIX)) {
            return text;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return PREFIX + Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("敏感密钥加密失败", ex);
        }
    }

    public String decryptIfCipher(String value) {
        String text = value == null ? "" : value.trim();
        if (!text.startsWith(PREFIX)) {
            return text;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] bytes = Base64.getDecoder().decode(text.substring(PREFIX.length()));
            return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("敏感密钥解密失败", ex);
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

    private byte[] normalizeKey(String secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[16];
            System.arraycopy(hash, 0, key, 0, key.length);
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("初始化密钥失败", ex);
        }
    }
}
