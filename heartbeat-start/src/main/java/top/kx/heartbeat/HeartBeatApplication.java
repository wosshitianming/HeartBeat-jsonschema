package top.kx.heartbeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 应用启动入口。
 *
 * <p>放在根包 {@code top.kx.heartbeat} 下，使组件扫描、JPA 仓储扫描、实体扫描默认覆盖到
 * domain / application / infrastructure / interfaces 各层，无需额外显式配置。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HeartBeatApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartBeatApplication.class, args);
    }
}
