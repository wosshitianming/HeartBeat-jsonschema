package top.kx.heartbeat.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "heartbeat.security.jwt")
public class JwtProperties {

    private String secret = "heartbeat-dev-secret-change-in-production-32bytes-min";
    private long accessTokenMinutes = 120;
    private long refreshTokenDays = 7;
}
