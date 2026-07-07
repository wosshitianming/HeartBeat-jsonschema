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
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String text = value == null ? "" : value.trim();
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(text) || text.startsWith(PREFIX)) {
            // 返回已经完成封装的业务结果。
            return text;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            // 返回已经完成封装的业务结果。
            return PREFIX + Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("敏感密钥加密失败", ex);
        }
    }

    public String decryptIfCipher(String value) {
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        String text = value == null ? "" : value.trim();
        // 根据当前业务条件选择对应处理路径。
        if (!text.startsWith(PREFIX)) {
            // 返回已经完成封装的业务结果。
            return text;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            // 计算当前分支的中间结果，供后续判断或组装使用。
            byte[] bytes = Base64.getDecoder().decode(text.substring(PREFIX.length()));
            // 返回已经完成封装的业务结果。
            return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("敏感密钥解密失败", ex);
        }
    }

    public String mask(String value) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String plain = decryptIfCipher(value);
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (StringUtils.isEmpty(plain)) {
            // 返回已经完成封装的业务结果。
            return "";
        }
        // 根据当前业务条件选择对应处理路径。
        if (plain.length() <= 4) {
            // 返回已经完成封装的业务结果。
            return "****";
        }
        // 返回已经完成封装的业务结果。
        return plain.substring(0, 2) + "****" + plain.substring(plain.length() - 2);
    }

    private byte[] normalizeKey(String secretKey) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            byte[] hash = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            // 创建当前流程需要的临时对象，承载后续处理数据。
            byte[] key = new byte[16];
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            System.arraycopy(hash, 0, key, 0, key.length);
            // 返回已经完成封装的业务结果。
            return key;
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("初始化密钥失败", ex);
        }
    }
}
