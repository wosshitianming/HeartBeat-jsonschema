package top.kx.heartbeat.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全加密配置。
 *
 * <p>统一注册密码编码器，避免业务类和仓储类自行创建加密实现。</p>
 */
@Configuration
public class SecurityCryptoConfig {

    /**
     * 注册密码编码器。
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 创建 BCrypt 密码编码器。
        return new BCryptPasswordEncoder();
    }
}
