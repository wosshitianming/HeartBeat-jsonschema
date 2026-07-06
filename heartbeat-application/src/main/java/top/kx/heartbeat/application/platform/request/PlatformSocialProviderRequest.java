package top.kx.heartbeat.application.platform.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class PlatformSocialProviderRequest {

    @JsonAlias("provider")
    private String providerCode;
    @JsonAlias("name")
    private String providerName;
    private String providerType;
    private String clientId;
    private String appKey;
    private String appSecretCipher;
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String scopes;
    private Boolean enabled;
    private String status;
}
